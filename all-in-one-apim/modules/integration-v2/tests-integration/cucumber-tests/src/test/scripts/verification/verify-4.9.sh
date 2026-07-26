#!/usr/bin/env bash
#
# Phase 4.9 verification — boot-failure isolation across parallel blocks.
#
# The suite runs three blocks parallel="tests" thread-count="2". Phase4.9-BlockBad points tomlOverlayPath
# at a nonexistent file so its boot fails and its class FAILS (build red); Phase4.9-BlockGoodA/-BlockGoodB
# boot real containers and pass. A bad boot must NOT fail the good siblings or abort the suite.
#
# Asserts, after the run: Maven build FAILS (the bad block reddens the build, but the good blocks still
# pass in isolation); the bad block's abortIfBlockBootFailed is recorded as a @BeforeClass config FAILURE
# (CONFIG_FAILS>=1), while passed>=2 (both good blocks' @Test methods); the failure carries the boot cause
# ("APIM block boot failed" + NoSuchFileException) and there is no NPE cascade; exactly two observations
# across two distinct real container ids (only the good blocks ran probes); and no container with the block
# label leaked. Re-runnable / idempotent. Prints a single PASS/FAIL line, non-zero on fail.
#
# Usage:  ./verify-4.9.sh
set -euo pipefail

STEP="4.9"
BLOCK_LABEL="fv-4.9"
LABEL_FILTER="label=block=${BLOCK_LABEL}"
EXPECTED_OBS=2

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
RESULTS_XML="${MODULE_DIR}/target/surefire-reports/testng-results.xml"
MVN_LOG="${MODULE_DIR}/target/verify-4.9-maven.log"

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

echo "== Phase ${STEP} verification: boot-failure isolation across parallel blocks =="
rm -f "${OBS_FILE}" "${RESULTS_XML}" "${MVN_LOG}"
cleanup_containers

# A bad boot must FAIL the build (regression guard against silent skip-to-green) WITHOUT taking its healthy
# siblings down: Maven must exit non-zero. We run inside `if` so set -e does not abort on the expected
# failure; the redirect to the log is preserved for diagnostics.
echo "Running verification suite via Maven..."
if ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-4.9.xml test ) > "${MVN_LOG}" 2>&1; then
    tail -25 "${MVN_LOG}"
    fail "Maven build SUCCEEDED - the bad block must FAIL the build, not skip-to-green"
fi

# Assertion 1: the bad block's guard rethrew the bootError as a @BeforeClass config FAILURE, while the two
# good blocks still passed in isolation. (TestNG marks the bad class's @Test methods SKIPPED, but the failed
# config method is what reddens the build, so we assert on the config-method FAIL plus the good blocks'
# passed @Test methods - not the root 'failed' attribute, which counts only @Test methods and stays 0.)
[ -f "${RESULTS_XML}" ] || fail "expected testng results not produced: ${RESULTS_XML}"
ROOT_ATTRS="$(grep -o '<testng-results[^>]*>' "${RESULTS_XML}" | head -1)"
get_attr() { printf '%s' "${ROOT_ATTRS}" | sed -n "s/.* $1=\"\([0-9]*\)\".*/\1/p"; }
PASSED="$(get_attr passed)"
CONFIG_FAILS="$(grep 'abortIfBlockBootFailed' "${RESULTS_XML}" | grep -c 'status="FAIL"' || true)"
[ "${CONFIG_FAILS}" -ge 1 ] \
    || fail "expected >=1 abortIfBlockBootFailed config FAILURE (the bad block), got ${CONFIG_FAILS}"
[ "${PASSED:-0}" -ge 2 ] || fail "expected >=2 passed (both good blocks), got '${PASSED}': ${ROOT_ATTRS}"

# Assertion 2: the failure is diagnosable - carries the guard message and the real boot cause.
grep -q "APIM block boot failed" "${RESULTS_XML}" \
    || fail "failure reason missing the 'APIM block boot failed' guard message (blank failure?)"
grep -q "NoSuchFileException" "${RESULTS_XML}" \
    || fail "failure reason missing the boot root cause (NoSuchFileException) from the bad toml overlay"

# Assertion 3: no NPE cascade from the absent container in the bad block.
if grep -q "NullPointerException" "${RESULTS_XML}"; then
    fail "NullPointerException present - the bad block's absent container caused an NPE cascade"
fi

# Assertion 4: only the good blocks ran probes - two observations across two distinct real containers.
[ -f "${OBS_FILE}" ] || fail "expected observation file not produced: ${OBS_FILE}"
OBS_COUNT="$(grep -c . "${OBS_FILE}" || true)"
[ "${OBS_COUNT}" = "${EXPECTED_OBS}" ] \
    || fail "expected ${EXPECTED_OBS} observations (good blocks only), got ${OBS_COUNT}"
DISTINCT_IDS="$(awk -F'|' '{print $3}' "${OBS_FILE}" | sort -u)"
ID_COUNT="$(printf '%s\n' "${DISTINCT_IDS}" | grep -c . || true)"
[ "${ID_COUNT}" = "${EXPECTED_OBS}" ] \
    || fail "expected ${EXPECTED_OBS} distinct container ids (one per good block), got ${ID_COUNT}: ${DISTINCT_IDS}"
if printf '%s\n' "${DISTINCT_IDS}" | grep -Eq '^(none|null)$'; then
    fail "a good-block probe recorded a missing container id: ${DISTINCT_IDS}"
fi

# Assertion 5: nothing leaked - good blocks tore down, the bad block created nothing.
LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
[ -z "${LEFTOVER}" ] || fail "containers leaked after run: ${LEFTOVER}"

echo "VERIFY ${STEP}: PASS - bad block FAILED (build red) in isolation (${CONFIG_FAILS} config FAILUREs), ${PASSED} good blocks still passed, ${EXPECTED_OBS} containers / 2 ids, no NPE, no leaks"
