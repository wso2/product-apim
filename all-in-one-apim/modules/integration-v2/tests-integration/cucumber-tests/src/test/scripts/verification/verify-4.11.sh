#!/usr/bin/env bash
#
# Phase 4.11 verification — the FAILURE reason is diagnosable (boot exception preserved as cause).
#
# The block's tomlOverlayPath points at a nonexistent file carrying a distinctive marker
# (fv-4.11-diagnostic-marker). onStart fails reading it, records the cause, and BaseBlockRunner's guard
# rethrows it as IllegalStateException("APIM block boot failed", cause) - a @BeforeClass config FAILURE.
# This is a fast, no-Docker gate: it does not boot a container - its sole job is to prove the FAILURE is
# debuggable rather than blank.
#
# Stronger than 4.5's text-presence checks: this asserts the failure carries, in the TestNG report, the
# IllegalStateException CLASS, its guard message, AND the chained root cause (Caused by: NoSuchFileException)
# naming the EXACT failing file (the marker path). It also asserts the same causal chain reached the
# console (Maven log) so a CI tail is debuggable too. Asserts build FAILS (a boot failure is not a skip),
# the guard's abortIfBlockBootFailed config method is recorded FAILED (CONFIG_FAILS>=1), and no NPE.
# Re-runnable / idempotent. Single PASS/FAIL line.
#
# Usage:  ./verify-4.11.sh
set -euo pipefail

STEP="4.11"
BLOCK_LABEL="fv-4.11"
LABEL_FILTER="label=block=${BLOCK_LABEL}"
PATH_MARKER="fv-4.11-diagnostic-marker"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
RESULTS_XML="${MODULE_DIR}/target/surefire-reports/testng-results.xml"
MVN_LOG="${MODULE_DIR}/target/verify-4.11-maven.log"

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

echo "== Phase ${STEP} verification: failure reason is diagnosable =="
rm -f "${OBS_FILE}" "${RESULTS_XML}" "${MVN_LOG}"
cleanup_containers

# A boot failure must FAIL the build (regression guard against silent skip-to-green): Maven must exit
# non-zero. We run inside `if` so set -e does not abort on the expected failure; the redirect to the log is
# preserved for diagnostics.
echo "Running verification suite via Maven..."
if ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-4.11.xml test ) > "${MVN_LOG}" 2>&1; then
    tail -25 "${MVN_LOG}"
    fail "Maven build SUCCEEDED - a boot failure must FAIL the build, not skip-to-green"
fi

# Assertion 1: the guard rethrew the bootError as a @BeforeClass config FAILURE. (TestNG marks the class's
# @Test methods SKIPPED, but the failed config method is what reddens the build, so we assert on the
# config-method FAIL - not the root 'failed' attribute, which counts only @Test methods and stays 0.)
[ -f "${RESULTS_XML}" ] || fail "expected testng results not produced: ${RESULTS_XML}"
CONFIG_FAILS="$(grep 'abortIfBlockBootFailed' "${RESULTS_XML}" | grep -c 'status="FAIL"' || true)"
[ "${CONFIG_FAILS}" -ge 1 ] \
    || fail "expected >=1 abortIfBlockBootFailed config FAILURE, got ${CONFIG_FAILS}"

# Assertion 2: the report records the IllegalStateException CLASS with its guard message (not a blank failure).
grep -q 'class="java.lang.IllegalStateException"' "${RESULTS_XML}" \
    || fail "report does not record the failure as a java.lang.IllegalStateException (blank/untyped failure?)"
grep -q "APIM block boot failed" "${RESULTS_XML}" \
    || fail "report missing the 'APIM block boot failed' guard message"

# Assertion 3: the CHAINED root cause is preserved and names the EXACT failing file - this is what makes
# the failure debuggable rather than just labelled.
grep -q "Caused by:" "${RESULTS_XML}" \
    || fail "report has no 'Caused by:' chain - the boot exception was not preserved as the failure cause"
grep -q "NoSuchFileException" "${RESULTS_XML}" \
    || fail "report's cause chain is missing the boot root cause (NoSuchFileException)"
grep -q "${PATH_MARKER}" "${RESULTS_XML}" \
    || fail "report does not name the failing overlay file (marker '${PATH_MARKER}') - cause not diagnosable"

# Assertion 4: no NPE cascade masking the real cause.
if grep -q "NullPointerException" "${RESULTS_XML}"; then
    fail "NullPointerException present - an NPE cascade is masking the real boot cause"
fi

# Assertion 5: the same causal chain reached the console (Maven log), so a CI tail is debuggable too.
grep -q "NoSuchFileException" "${MVN_LOG}" \
    || fail "Maven console log does not surface the boot cause (NoSuchFileException)"
grep -q "${PATH_MARKER}" "${MVN_LOG}" \
    || fail "Maven console log does not name the failing overlay file (marker '${PATH_MARKER}')"

echo "VERIFY ${STEP}: PASS - failure recorded as IllegalStateException @BeforeClass config FAIL with chained NoSuchFileException naming '${PATH_MARKER}', surfaced in report and console, no NPE"
