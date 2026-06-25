#!/usr/bin/env bash
#
# Phase 4.10 verification — teardown idempotency / double-stop.
#
# Two blocks run serially. Phase4.10-DoubleStop boots a real container, records its observation, then the
# probe stops the container itself — so BlockLifecycleListener.onFinish's later stop() is a DOUBLE-STOP
# that must no-op (the scenario still PASSES, nothing leaks). Phase4.10-NoContainer points tomlOverlayPath
# at a nonexistent file so boot produces NO container; the guard rethrows the bootError as a @BeforeClass
# config FAILURE (build red), and onFinish must still hit its null-guard and no-op (nothing to stop).
#
# Asserts, after the run: Maven build FAILS (the no-container block's boot failure reddens the build, but
# the double-stop block still passes); the no-container block's abortIfBlockBootFailed is recorded as a
# @BeforeClass config FAILURE (CONFIG_FAILS>=1) while passed>=1 (the double-stop block); no
# NullPointerException anywhere (the null-guard held); exactly one observation with a real container id
# (only the double-stop block ran a probe); and no container with the block label leaked (the double-stopped
# container is gone). Re-runnable / idempotent. Prints a single PASS/FAIL line, non-zero on fail.
#
# Usage:  ./verify-4.10.sh
set -euo pipefail

STEP="4.10"
BLOCK_LABEL="fv-4.10"
LABEL_FILTER="label=block=${BLOCK_LABEL}"
EXPECTED_OBS=1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
RESULTS_XML="${MODULE_DIR}/target/surefire-reports/testng-results.xml"
MVN_LOG="${MODULE_DIR}/target/verify-4.10-maven.log"

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

echo "== Phase ${STEP} verification: teardown idempotency / double-stop =="
rm -f "${OBS_FILE}" "${RESULTS_XML}" "${MVN_LOG}"
cleanup_containers

# The no-container block's boot failure must FAIL the build, while the double-stop block still passes and
# the null-guard/double-stop teardowns stay no-ops. Maven must exit non-zero. We run inside `if` so set -e
# does not abort on the expected failure; the redirect to the log is preserved for diagnostics.
echo "Running verification suite via Maven..."
if ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-4.10.xml test ) > "${MVN_LOG}" 2>&1; then
    tail -25 "${MVN_LOG}"
    fail "Maven build SUCCEEDED - the no-container block's boot failure must FAIL the build, not skip-to-green"
fi

# Assertion 1: the no-container block's guard rethrew the bootError as a @BeforeClass config FAILURE, while
# the double-stop block still passed. (TestNG marks the no-container class's @Test methods SKIPPED, but the
# failed config method is what reddens the build, so we assert on the config-method FAIL plus the
# double-stop block's passed @Test method - not the root 'failed' attribute, which counts only @Test methods.)
[ -f "${RESULTS_XML}" ] || fail "expected testng results not produced: ${RESULTS_XML}"
ROOT_ATTRS="$(grep -o '<testng-results[^>]*>' "${RESULTS_XML}" | head -1)"
get_attr() { printf '%s' "${ROOT_ATTRS}" | sed -n "s/.* $1=\"\([0-9]*\)\".*/\1/p"; }
PASSED="$(get_attr passed)"
CONFIG_FAILS="$(grep 'abortIfBlockBootFailed' "${RESULTS_XML}" | grep -c 'status="FAIL"' || true)"
[ "${CONFIG_FAILS}" -ge 1 ] \
    || fail "expected >=1 abortIfBlockBootFailed config FAILURE (the no-container block), got ${CONFIG_FAILS}"
[ "${PASSED:-0}" -ge 1 ] || fail "expected >=1 passed (the double-stop block), got '${PASSED}': ${ROOT_ATTRS}"

# Assertion 2: the null-guard held - no NPE from onFinish on the block that booted no container.
if grep -q "NullPointerException" "${RESULTS_XML}"; then
    fail "NullPointerException present - onFinish null-guard did not hold for the no-container block"
fi

# Assertion 3: only the double-stop block ran a probe - one observation with a real container id.
[ -f "${OBS_FILE}" ] || fail "expected observation file not produced: ${OBS_FILE}"
OBS_COUNT="$(grep -c . "${OBS_FILE}" || true)"
[ "${OBS_COUNT}" = "${EXPECTED_OBS}" ] \
    || fail "expected ${EXPECTED_OBS} observation (double-stop block only), got ${OBS_COUNT}"
CONTAINER_ID="$(awk -F'|' 'NR==1{print $3}' "${OBS_FILE}")"
case "${CONTAINER_ID}" in none|null|"") fail "probe recorded a missing container id '${CONTAINER_ID}'" ;; esac

# Assertion 4: nothing leaked - the double-stopped container is gone, the skipped block created nothing.
LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
[ -z "${LEFTOVER}" ] || fail "containers leaked after run: ${LEFTOVER}"

echo "VERIFY ${STEP}: PASS - double-stop passed and no-container block FAILED the build (${PASSED} passed/${CONFIG_FAILS} config FAILUREs) while onFinish's null-guard held, no NPE, no leaks"
