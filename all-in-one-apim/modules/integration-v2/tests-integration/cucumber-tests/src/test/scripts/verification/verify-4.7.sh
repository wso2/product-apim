#!/usr/bin/env bash
#
# Phase 4.7 verification — two-level concurrency on real containers (scaled).
#
# testng-fv-4.7.xml runs the suite parallel="tests" thread-count="2" (=> at most K=2 blocks, and so K=2
# APIM containers, alive at once) over N=3 blocks, each parallel="classes" thread-count="2" over THREE
# probe classes (=> at most M=2 classes at once on the block's single shared container). All three blocks
# share the docker label block=fv-4.7 (distinct <test> names => distinct shared scopes).
#
# While Maven runs, this polls `docker ps` for the live count of block containers and tracks the peak.
# Asserts, after the run: the suite passed; 9 observations across exactly 3 distinct real container ids
# (every block booted its own container, every class observed its block's container); the peak live
# container count never exceeded K=2 (the container cap is a real bound) AND reached 2 (blocks truly ran
# in parallel, not serialized); and within each block at most M=2 distinct worker threads were used (at
# most M classes ran at once). Re-runnable / idempotent. Prints a single PASS/FAIL line, non-zero on fail.
#
# Usage:  ./verify-4.7.sh
set -euo pipefail

STEP="4.7"
BLOCK_LABEL="fv-4.7"
LABEL_FILTER="label=block=${BLOCK_LABEL}"
K=2   # suite-level cap: max concurrent blocks/containers
M=2   # per-block cap: max concurrent classes
EXPECTED_BLOCKS=3
EXPECTED_OBS=9   # 3 classes x 3 blocks

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
MVN_LOG="${MODULE_DIR}/target/verify-4.7-maven.log"

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

echo "== Phase ${STEP} verification: two-level concurrency on real containers =="
rm -f "${OBS_FILE}" "${MVN_LOG}"
cleanup_containers

echo "Running verification suite via Maven (polling live container count)..."
( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-4.7.xml test ) > "${MVN_LOG}" 2>&1 &
MVN_PID=$!

MAX_LIVE=0
while kill -0 "${MVN_PID}" 2>/dev/null; do
    LIVE="$(docker ps -q --filter "${LABEL_FILTER}" 2>/dev/null | grep -c . || true)"
    if [ "${LIVE}" -gt "${MAX_LIVE}" ]; then MAX_LIVE="${LIVE}"; fi
    sleep 1
done
wait "${MVN_PID}" && MVN_RC=0 || MVN_RC=$?

[ "${MVN_RC}" = "0" ] || { tail -25 "${MVN_LOG}"; fail "verification suite reported test failures (see ${MVN_LOG})"; }

# Assertion 1: every block ran every probe class against a real container.
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

# Assertion 2: the container cap is real - peak live containers never exceeded K, but reached K
# (so blocks genuinely overlapped rather than running one at a time).
[ "${MAX_LIVE}" -le "${K}" ] || fail "peak ${MAX_LIVE} live containers exceeded the suite cap K=${K}"
[ "${MAX_LIVE}" -ge 2 ] \
    || fail "peak was only ${MAX_LIVE} live container(s) - blocks did not run in parallel, so K is unproven"

# Assertion 3: within each block, at most M classes ran at once (=> at most M distinct worker threads).
while IFS= read -r cid; do
    [ -n "${cid}" ] || continue
    THREADS="$(awk -F'|' -v c="${cid}" '$3==c{print $2}' "${OBS_FILE}" | sort -u | grep -c . || true)"
    [ "${THREADS}" -le "${M}" ] \
        || fail "block container ${cid} used ${THREADS} distinct worker threads (> per-block cap M=${M})"
done <<< "${DISTINCT_IDS}"

echo "VERIFY ${STEP}: PASS - ${EXPECTED_BLOCKS} blocks/containers, peak ${MAX_LIVE} live (cap K=${K}, parallelism observed), <=${M} classes per block at once, all passed"
