#!/usr/bin/env bash
#
# Phase 4.5 verification — fail-on-boot-failure for the parallel-on-shared-container model.
#
# The block's tomlOverlayPath <parameter> points at a nonexistent file, so BlockLifecycleListener.onStart
# fails reading the overlay, records the cause as the bootError attribute (without throwing), and
# BaseBlockRunner's guard rethrows that as a @BeforeClass configuration FAILURE per class. A boot failure
# must turn the build RED - never a silent skip-to-green - while still avoiding an NPE cascade from the
# absent container; onFinish must no-op (no container was ever created -> nothing to stop, nothing to leak).
#
# Asserts, after the run: Maven build FAILS (a boot failure is not a skip); the guard's
# abortIfBlockBootFailed config method is recorded FAILED once per probe class (TestNG marks the @Test
# methods SKIPPED, but the failed config method is what reddens the build); the failure carries the boot
# cause (NoSuchFileException + the "APIM block boot failed" message) so it is diagnosable; there is NO
# NullPointerException; the probe observation file was NOT produced (no step ran); and no containers
# leaked. Re-runnable / idempotent. Prints a single PASS/FAIL line and exits non-zero on failure.
#
# Usage:  ./verify-4.5.sh
set -euo pipefail

STEP="4.5"
BLOCK_LABEL="fv-4.5"
LABEL_FILTER="label=block=${BLOCK_LABEL}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
RESULTS_XML="${MODULE_DIR}/target/surefire-reports/testng-results.xml"

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

echo "== Phase ${STEP} verification: fail-on-boot-failure =="
rm -f "${OBS_FILE}" "${RESULTS_XML}"
cleanup_containers

# A boot failure must FAIL the build (regression guard against silent skip-to-green): Maven must exit
# non-zero. We run inside `if` so set -e does not abort the script on the expected failure.
echo "Running verification suite via Maven..."
if ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-4.5.xml test ); then
    fail "Maven build SUCCEEDED - a boot failure must FAIL the build, not skip-to-green"
fi

# Assertion 1: the guard rethrew the bootError as a @BeforeClass config FAILURE once per probe class.
# (TestNG marks the class's @Test methods SKIPPED, but the failed config method is what reddens the build,
# so we assert on the config-method FAIL - not the root 'failed' attribute, which counts only @Test methods.)
[ -f "${RESULTS_XML}" ] || fail "expected testng results not produced: ${RESULTS_XML}"
CONFIG_FAILS="$(grep 'abortIfBlockBootFailed' "${RESULTS_XML}" | grep -c 'status="FAIL"' || true)"
[ "${CONFIG_FAILS}" -ge 2 ] \
    || fail "expected >=2 abortIfBlockBootFailed config FAILUREs (one per probe class), got ${CONFIG_FAILS}"

# Assertion 2: the failure is diagnosable - carries the guard message and the real boot cause as root.
grep -q "APIM block boot failed" "${RESULTS_XML}" \
    || fail "failure reason missing the 'APIM block boot failed' guard message (blank failure?)"
grep -q "NoSuchFileException" "${RESULTS_XML}" \
    || fail "failure reason missing the boot root cause (NoSuchFileException) from the bad toml overlay"

# Assertion 3: no NPE cascade from the absent container.
if grep -q "NullPointerException" "${RESULTS_XML}"; then
    fail "NullPointerException present - the absent container caused an NPE cascade"
fi

# Assertion 4: onFinish no-op - no probe step ran, so no observation file was produced.
[ ! -s "${OBS_FILE}" ] || fail "observation file was produced - a probe step ran despite the boot failure"

# Assertion 5: nothing leaked - no container was ever created for this block.
LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
[ -z "${LEFTOVER}" ] || fail "containers leaked after run: ${LEFTOVER}"

echo "VERIFY ${STEP}: PASS - boot failure FAILED the build via ${CONFIG_FAILS} config FAILUREs (no silent skip), boot cause preserved as root, no NPE cascade, onFinish no-op, no leaks"
