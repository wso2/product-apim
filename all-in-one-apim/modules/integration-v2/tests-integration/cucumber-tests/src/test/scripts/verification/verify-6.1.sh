#!/usr/bin/env bash
#
# Phase 6.1 verification — the capstone: the whole parallel-on-shared-container model end to end.
#
# testng-framework-verification.xml runs the suite parallel="tests" thread-count="2" (=> at most K=2 blocks,
# and so K=2 APIM containers, alive at once) over THREE blocks (all docker label block=fv-6.1, distinct
# <test> names => distinct shared scopes), each parallel="classes" thread-count="2" (=> at most M=2 probe
# classes at once on the block's single shared container):
#   * Phase6.1-BlockA      - default tenant set provisioned in onStart; TWO probe classes share its container.
#   * Phase6.1-BlockB      - default tenant set provisioned in onStart; TWO probe classes share its container.
#   * Phase6.1-BrokenBlock - tenantSet=adpsample on a FRESH container; provisioning throws in onStart and is
#                            recorded as bootError, so BaseBlockRunner FAILS the class (build red).
# Each non-failed probe records an observation (millis|thread|containerId|baseUrl|baseGatewayUrl).
#
# While Maven runs, this polls `docker ps` for the live count of block containers and tracks the peak.
# Asserts, after the run (the full 6.1 checklist):
#   1. Build FAILS - the broken block reddens the build (a boot failure is never a silent skip-to-green),
#      while the two good blocks still pass in isolation.
#   2. Readiness + observations - exactly 4 observations (2 classes x 2 good blocks); the broken block's
#      class never ran, so it recorded none. No 'none'/'null' container id.
#   3. Isolation - exactly 2 DISTINCT container ids AND 2 DISTINCT baseUrls (distinct mapped ports), and each
#      container id appears EXACTLY twice => each good block's two classes saw their OWN block's container/URL
#      with no cross-block override under the suiteName::testName namespace.
#   4. Cap K - peak live containers never exceeded K=2 but reached 2 (blocks truly overlapped, K is a real bound).
#   5. Per-block cap M - within each good block at most M=2 distinct worker threads were used.
#   6. Build red in isolation - the broken block's abortIfBlockBootFailed is recorded as a @BeforeClass config
#      FAILURE (CONFIG_FAILS>=1), and the Maven log carries the listener's boot-failure marker (the failure
#      really came from the broken block's provisioning failure).
#   7. Release - zero fv-6.1 containers leaked (every block, including the broken one, released its container).
# Re-runnable / idempotent. Prints a single PASS/FAIL line, non-zero on fail.
#
# Usage:  ./verify-6.1.sh
set -euo pipefail

STEP="6.1"
BLOCK_LABEL="fv-6.1"
LABEL_FILTER="label=block=${BLOCK_LABEL}"
SUITE_XML="testng-framework-verification.xml"
K=2                 # suite-level cap: max concurrent blocks/containers
M=2                 # per-block cap: max concurrent classes
EXPECTED_GOOD_BLOCKS=2
EXPECTED_OBS=4      # 2 classes x 2 good blocks (broken block records none)
OBS_PER_BLOCK=2

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
RESULTS_XML="${MODULE_DIR}/target/surefire-reports/testng-results.xml"
MVN_LOG="${MODULE_DIR}/target/verify-6.1-maven.log"

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

echo "== Phase ${STEP} verification: capstone - full parallel-on-shared-container model =="
rm -f "${OBS_FILE}" "${MVN_LOG}"
cleanup_containers

echo "Running capstone verification suite via Maven (polling live container count)..."
( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml="${SUITE_XML}" test ) > "${MVN_LOG}" 2>&1 &
MVN_PID=$!

MAX_LIVE=0
while kill -0 "${MVN_PID}" 2>/dev/null; do
    LIVE="$(docker ps -q --filter "${LABEL_FILTER}" 2>/dev/null | grep -c . || true)"
    if [ "${LIVE}" -gt "${MAX_LIVE}" ]; then MAX_LIVE="${LIVE}"; fi
    sleep 1
done
wait "${MVN_PID}" && MVN_RC=0 || MVN_RC=$?

# Assertion 1: build FAILED - the broken block reddens the build (a boot failure is never a silent
# skip-to-green); the good blocks still passing in isolation is proven by assertions 2-5 below.
[ "${MVN_RC}" != "0" ] || { tail -25 "${MVN_LOG}"; fail "Maven build SUCCEEDED - the broken block must FAIL the build, not skip-to-green"; }

# Assertion 2: readiness + observations - the two good blocks each ran both probe classes.
[ -f "${OBS_FILE}" ] || fail "expected observation file not produced: ${OBS_FILE}"
OBS_COUNT="$(grep -c . "${OBS_FILE}" || true)"
[ "${OBS_COUNT}" = "${EXPECTED_OBS}" ] || fail "expected ${EXPECTED_OBS} observations (2 classes x 2 good blocks), got ${OBS_COUNT}"
if awk -F'|' '{print $3}' "${OBS_FILE}" | grep -Eq '^(none|null)$'; then
    fail "a probe recorded a missing container id"
fi

# Assertion 3 (isolation): exactly 2 distinct container ids AND 2 distinct baseUrls, each id appearing
# exactly OBS_PER_BLOCK times => each good block's classes saw their OWN container/URL, no cross-block override.
DISTINCT_IDS="$(awk -F'|' '{print $3}' "${OBS_FILE}" | sort -u)"
ID_COUNT="$(printf '%s\n' "${DISTINCT_IDS}" | grep -c . || true)"
[ "${ID_COUNT}" = "${EXPECTED_GOOD_BLOCKS}" ] \
    || fail "expected ${EXPECTED_GOOD_BLOCKS} distinct container ids (one per good block), got ${ID_COUNT}: ${DISTINCT_IDS}"
DISTINCT_URLS="$(awk -F'|' '{print $4}' "${OBS_FILE}" | sort -u)"
URL_COUNT="$(printf '%s\n' "${DISTINCT_URLS}" | grep -c . || true)"
[ "${URL_COUNT}" = "${EXPECTED_GOOD_BLOCKS}" ] \
    || fail "expected ${EXPECTED_GOOD_BLOCKS} distinct baseUrls (per-container mapped ports), got ${URL_COUNT}: ${DISTINCT_URLS}"
while IFS= read -r cid; do
    [ -n "${cid}" ] || continue
    N="$(awk -F'|' -v c="${cid}" '$3==c' "${OBS_FILE}" | grep -c . || true)"
    [ "${N}" = "${OBS_PER_BLOCK}" ] \
        || fail "container ${cid} has ${N} observations (expected ${OBS_PER_BLOCK} - its block's two classes); isolation/grouping broken"
done <<< "${DISTINCT_IDS}"

# Assertion 4 (cap K): peak live containers never exceeded K, but reached 2 (blocks genuinely overlapped).
[ "${MAX_LIVE}" -le "${K}" ] || fail "peak ${MAX_LIVE} live containers exceeded the suite cap K=${K}"
[ "${MAX_LIVE}" -ge 2 ] \
    || fail "peak was only ${MAX_LIVE} live container(s) - blocks did not run in parallel, so K is unproven"

# Assertion 5 (per-block cap M): within each good block at most M classes ran at once (<=M distinct threads).
while IFS= read -r cid; do
    [ -n "${cid}" ] || continue
    THREADS="$(awk -F'|' -v c="${cid}" '$3==c{print $2}' "${OBS_FILE}" | sort -u | grep -c . || true)"
    [ "${THREADS}" -le "${M}" ] \
        || fail "block container ${cid} used ${THREADS} distinct worker threads (> per-block cap M=${M})"
done <<< "${DISTINCT_IDS}"

# Assertion 6 (build red in isolation): the broken block's guard rethrew the bootError as a @BeforeClass
# config FAILURE (CONFIG_FAILS>=1) and the failure was provisioning-driven (the listener recorded a
# boot/readiness failure). (TestNG marks the broken class's @Test methods SKIPPED, but the failed config
# method is what reddens the build, so we assert on the config-method FAIL - not the root 'failed' attribute,
# which counts only @Test methods and stays 0.)
[ -f "${RESULTS_XML}" ] || fail "testng-results.xml not produced: ${RESULTS_XML}"
CONFIG_FAILS="$(grep 'abortIfBlockBootFailed' "${RESULTS_XML}" | grep -c 'status="FAIL"' || true)"
[ "${CONFIG_FAILS}" -ge 1 ] \
    || fail "expected >=1 abortIfBlockBootFailed config FAILURE (the broken block), got ${CONFIG_FAILS}"
grep -q "boot/readiness failed" "${MVN_LOG}" \
    || fail "Maven log lacks the listener's boot-failure marker - the failure may not be provisioning-driven (see ${MVN_LOG})"

# Assertion 7 (release): zero containers leaked - every block, including the broken one, released its container.
LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
[ -z "${LEFTOVER}" ] || fail "fv-6.1 containers leaked after run: ${LEFTOVER}"

echo "VERIFY ${STEP}: PASS - ${EXPECTED_GOOD_BLOCKS} good blocks each provisioned+observed their OWN container (${EXPECTED_GOOD_BLOCKS} distinct ids + ${EXPECTED_GOOD_BLOCKS} distinct baseUrls, ${OBS_PER_BLOCK} obs each), peak ${MAX_LIVE} live (cap K=${K}, parallelism observed), <=${M} classes/block at once, broken block FAILED the build in isolation (${CONFIG_FAILS} config FAILURE(s)), no leaks"
