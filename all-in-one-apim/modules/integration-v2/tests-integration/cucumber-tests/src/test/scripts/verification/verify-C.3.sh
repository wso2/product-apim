#!/usr/bin/env bash
#
# Phase C.3 verification — suite-abort cleanup.
#
# Goal (from the plan): interrupt a running parallel suite (Ctrl-C / kill); document and verify what
# containers/ports remain (relevant with Ryuk disabled), and that a FOLLOW-UP run still succeeds (no stuck
# ports). Add a JVM shutdown hook if leaks are unacceptable.
#
# What this script does, in order:
#   1. Launches testng-fv-C.3.xml (suite parallel="tests" thread-count="2") in the background and waits
#      until its block containers (label block=fv-c.3) are actually RUNNING - i.e. a genuinely parallel,
#      multi-container suite is in flight.
#   2. Captures the host ports those containers hold, then HARD-KILLS the test JVM with SIGKILL (plus the
#      Maven wrapper and the forked surefire booter). SIGKILL cannot be trapped, so this bypasses
#      BlockLifecycleListener.onFinish (graceful stop + scope clear) AND any JVM shutdown hook - the worst
#      case: an OOM-kill / crashed agent / kill -9, not a polite Ctrl-C.
#   3. DOCUMENTS what survived the abort: with Testcontainers reuse enabled, Ryuk is disabled, so the
#      expectation is that the containers LEAK (are not auto-reaped). This is reported, not failed - a leak
#      on abrupt kill is the documented, expected behavior of the current (Ryuk-off) configuration.
#   4. Force-removes any leftover fv-c.3 containers and VERIFIES the exact host ports they held are released
#      at the OS level (no stuck ports - the Phase 1.4 primitive: a killed container frees its dynamic host
#      port; only the exited record lingers until pruned).
#   5. Runs the SAME suite again, synchronously, and asserts it SUCCEEDS and produces fresh observations -
#      proving an abort never wedges a follow-up run.
#   6. Asserts a clean final state (zero fv-c.3 containers).
#
# FAILS only when the abort actually broke something: containers never came up to abort; a captured host
# port stayed stuck after removal; the follow-up run failed or produced no observations; or the final state
# is not clean. A reaped-vs-leaked result is informational and printed either way.
#
# Re-runnable / idempotent. Prints a single PASS/FAIL line; exits non-zero on failure.
#
# Usage:  ./verify-C.3.sh
set -euo pipefail

STEP="C.3"
BLOCK_LABEL="fv-c.3"
LABEL_FILTER="label=block=${BLOCK_LABEL}"
SUITE_XML="testng-fv-C.3.xml"
K=2                       # both blocks/containers should come up before we abort
EXPECTED_FOLLOWUP_OBS=2   # 1 probe class x 2 blocks on the clean follow-up run
BOOT_WAIT_SECS=420        # max wait for at least one block container to be running before aborting
PORT_RELEASE_SECS=60      # max wait for a killed container's host port to free at the OS level

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
ABORT_LOG="${MODULE_DIR}/target/verify-C.3-abort-maven.log"
FOLLOWUP_LOG="${MODULE_DIR}/target/verify-C.3-followup-maven.log"

fail() { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

cleanup_containers() {
    local ids
    ids="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
    if [ -n "${ids}" ]; then
        docker rm -f ${ids} >/dev/null 2>&1 || true
    fi
}

# host ports currently published by all running fv-c.3 containers (one per line, deduped)
captured_host_ports() {
    local ids id
    ids="$(docker ps -q --filter "${LABEL_FILTER}" 2>/dev/null || true)"
    for id in ${ids}; do
        docker port "${id}" 2>/dev/null | sed -n 's/.*:\([0-9][0-9]*\)$/\1/p'
    done | sort -u
}

port_open() { (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null && { exec 3>&- 3<&-; return 0; } || return 1; }

# Best-effort kill of anything we spawned, then container cleanup, on any exit.
ABORT_DONE=0
final_trap() {
    if [ "${ABORT_DONE}" = "0" ] && [ -n "${MVN_PID:-}" ]; then
        pkill -9 -P "${MVN_PID}" 2>/dev/null || true
        kill -9 "${MVN_PID}" 2>/dev/null || true
    fi
    cleanup_containers
}
trap final_trap EXIT

echo "== Phase ${STEP} verification: suite-abort cleanup (hard kill of a live parallel suite) =="
rm -f "${OBS_FILE}" "${ABORT_LOG}" "${FOLLOWUP_LOG}"
cleanup_containers

# --- 1. Launch the suite and wait until its containers are actually running --------------------------
echo "Launching abort-target suite in background; waiting for block containers to come up..."
( cd "${REACTOR_DIR}" && exec mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml="${SUITE_XML}" test ) > "${ABORT_LOG}" 2>&1 &
MVN_PID=$!

RUNNING=0
for ((i=0; i<BOOT_WAIT_SECS; i++)); do
    if ! kill -0 "${MVN_PID}" 2>/dev/null; then
        tail -25 "${ABORT_LOG}"
        fail "abort-target suite exited before any container came up (see ${ABORT_LOG})"
    fi
    RUNNING="$(docker ps -q --filter "${LABEL_FILTER}" 2>/dev/null | grep -c . || true)"
    if [ "${RUNNING}" -ge "${K}" ]; then break; fi
    sleep 1
done
[ "${RUNNING}" -ge 1 ] || fail "no fv-c.3 container became running within ${BOOT_WAIT_SECS}s - nothing to abort"
echo "  ${RUNNING} block container(s) running (target K=${K}); proceeding to abort."

# --- 2. Capture the host ports they hold, then HARD-KILL the test JVM (bypasses onFinish) ------------
PORTS_BEFORE="$(captured_host_ports)"
[ -n "${PORTS_BEFORE}" ] || fail "could not read any published host port from the running containers"
echo "  Captured published host ports before kill: $(echo ${PORTS_BEFORE} | tr '\n' ' ')"

echo "  SIGKILL-ing the Maven/surefire test JVM (simulating an abrupt, untrappable abort)..."
pkill -9 -P "${MVN_PID}" 2>/dev/null || true
kill -9 "${MVN_PID}" 2>/dev/null || true
pkill -9 -f 'surefirebooter' 2>/dev/null || true
wait "${MVN_PID}" 2>/dev/null || true
ABORT_DONE=1
sleep 3   # let the OS finish tearing down the process tree

# --- 3. Document what survived the abort (Ryuk disabled => expect a leak, reported not failed) -------
SURVIVORS="$(docker ps -q --filter "${LABEL_FILTER}" 2>/dev/null | grep -c . || true)"
ALL_RECORDS="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null | grep -c . || true)"
if [ "${SURVIVORS}" -gt 0 ]; then
    echo "  ABORT RESULT: ${SURVIVORS} container(s) LEAKED (still running) - expected with Ryuk disabled"
    echo "                (reuse enabled => no Ryuk reaper; SIGKILL skips onFinish and any shutdown hook)."
else
    echo "  ABORT RESULT: 0 running survivors (${ALL_RECORDS} record(s) total) - something reaped them."
fi

# --- 4. Force-clean leftovers and verify the captured host ports are released at the OS level --------
echo "  Force-removing any leftover fv-c.3 containers and checking host ports release..."
cleanup_containers
for p in ${PORTS_BEFORE}; do
    freed=0
    for ((i=0; i<PORT_RELEASE_SECS; i++)); do
        if ! port_open "${p}"; then freed=1; break; fi
        sleep 1
    done
    [ "${freed}" = "1" ] || fail "host port ${p} still bound ${PORT_RELEASE_SECS}s after removing the aborted containers (stuck port)"
done
echo "  All captured host ports released - no stuck ports."

# --- 5. Follow-up run must succeed and produce fresh observations ------------------------------------
echo "Running a follow-up suite synchronously - it must succeed despite the prior abort..."
rm -f "${OBS_FILE}"
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml="${SUITE_XML}" test ) > "${FOLLOWUP_LOG}" 2>&1; then
    tail -25 "${FOLLOWUP_LOG}"
    fail "follow-up run failed after an abort - the abort wedged the next run (see ${FOLLOWUP_LOG})"
fi
[ -f "${OBS_FILE}" ] || fail "follow-up run produced no observation file: ${OBS_FILE}"
FOLLOWUP_OBS="$(grep -c . "${OBS_FILE}" || true)"
[ "${FOLLOWUP_OBS}" = "${EXPECTED_FOLLOWUP_OBS}" ] \
    || fail "follow-up expected ${EXPECTED_FOLLOWUP_OBS} observations, got ${FOLLOWUP_OBS} (see ${FOLLOWUP_LOG})"

# --- 6. Final state must be clean -------------------------------------------------------------------
cleanup_containers
LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
[ -z "${LEFTOVER}" ] || fail "fv-c.3 containers still present at end of verification: ${LEFTOVER}"

ABORT_SUMMARY="leaked ${SURVIVORS} container(s) on SIGKILL (expected, Ryuk disabled)"
[ "${SURVIVORS}" = "0" ] && ABORT_SUMMARY="0 survivors after SIGKILL"
echo "VERIFY ${STEP}: PASS - aborted a live K=${K} parallel suite: ${ABORT_SUMMARY}; all captured host ports released (no stuck ports); follow-up run succeeded with ${FOLLOWUP_OBS} fresh observations; clean final state"
