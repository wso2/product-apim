#!/usr/bin/env bash
#
# Phase 4.12 verification — suite cap negative control (mirrors the pure-TestNG SmokeTest2).
#
# testng-fv-4.12.xml runs N=3 single-class blocks under suite parallel="tests" thread-count="2" (K=2),
# all sharing the docker label block=fv-4.12. Because each block holds ONE class, this isolates the
# SUITE-level container cap: with three blocks but only two block slots, the 3rd block must wait, so the
# live APIM container count must NEVER reach 3.
#
# While Maven runs, this polls `docker ps` for the live count of block containers and tracks the peak.
# Asserts, after the run: the suite passed; 3 observations across exactly 3 distinct real container ids
# (all three blocks eventually booted their own container); the peak live container count NEVER reached 3
# (the K=2 container cap is a real bound, not just a thread bound) AND reached 2 (the cap was genuinely
# exercised - blocks overlapped rather than serializing, which would make the "never 3" claim vacuous).
# Re-runnable / idempotent. Prints a single PASS/FAIL line, non-zero on fail.
#
# Usage:  ./verify-4.12.sh
set -euo pipefail

STEP="4.12"
BLOCK_LABEL="fv-4.12"
LABEL_FILTER="label=block=${BLOCK_LABEL}"
K=2   # suite-level cap: max concurrent blocks/containers
EXPECTED_BLOCKS=3
EXPECTED_OBS=3

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
MVN_LOG="${MODULE_DIR}/target/verify-4.12-maven.log"

fail() { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

cleanup_containers() {
    local ids
    ids="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
    if [ -n "${ids}" ]; then
        echo "Cleaning up verify-${STEP} containers: ${ids}"
        docker rm -f ${ids} >/dev/null 2>&1 || true
    fi
}

# Always clean up on exit so a crashed run never poisons the next.
trap cleanup_containers EXIT

echo "== Phase ${STEP} verification: suite cap negative control =="
rm -f "${OBS_FILE}" "${MVN_LOG}"
cleanup_containers

echo "Running verification suite via Maven (polling live container count)..."
( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-4.12.xml test ) > "${MVN_LOG}" 2>&1 &
MVN_PID=$!

MAX_LIVE=0
while kill -0 "${MVN_PID}" 2>/dev/null; do
    LIVE="$(docker ps -q --filter "${LABEL_FILTER}" 2>/dev/null | grep -c . || true)"
    if [ "${LIVE}" -gt "${MAX_LIVE}" ]; then MAX_LIVE="${LIVE}"; fi
    sleep 1
done
wait "${MVN_PID}" && MVN_RC=0 || MVN_RC=$?

[ "${MVN_RC}" = "0" ] || { tail -25 "${MVN_LOG}"; fail "verification suite reported test failures (see ${MVN_LOG})"; }

# Assertion 1: all three blocks eventually ran against their own real container.
[ -f "${OBS_FILE}" ] || fail "expected observation file not produced: ${OBS_FILE}"
OBS_COUNT="$(grep -c . "${OBS_FILE}" || true)"
[ "${OBS_COUNT}" = "${EXPECTED_OBS}" ] || fail "expected ${EXPECTED_OBS} observations, got ${OBS_COUNT}"
DISTINCT_IDS="$(awk -F'|' '{print $3}' "${OBS_FILE}" | sort -u)"
ID_COUNT="$(printf '%s\n' "${DISTINCT_IDS}" | grep -c . || true)"
[ "${ID_COUNT}" = "${EXPECTED_BLOCKS}" ] \
    || fail "expected ${EXPECTED_BLOCKS} distinct container ids (one per block), got ${ID_COUNT}: ${DISTINCT_IDS}"
if printf '%s\n' "${DISTINCT_IDS}" | grep -Eq '^(none|null)$'; then
    fail "a probe recorded a missing container id: ${DISTINCT_IDS}"
fi

# Assertion 2 (the negative control): peak live containers NEVER reached 3 - the K=2 cap is a real
# container bound, so the 3rd block waited for a slot.
[ "${MAX_LIVE}" -le "${K}" ] \
    || fail "peak ${MAX_LIVE} live containers exceeded the suite cap K=${K} - the 3rd block did not wait"

# Assertion 3: the cap was genuinely exercised - the peak reached K (blocks overlapped). Without this a
# fully serial run (peak 1) would satisfy "never 3" vacuously and prove nothing.
[ "${MAX_LIVE}" -ge "${K}" ] \
    || fail "peak was only ${MAX_LIVE} live container(s) - blocks never overlapped, so the cap is unproven"

echo "VERIFY ${STEP}: PASS - ${EXPECTED_BLOCKS} blocks/containers, peak ${MAX_LIVE} live (never reached 3; cap K=${K} held and was exercised), all passed"
