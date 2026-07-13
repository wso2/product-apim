#!/usr/bin/env bash
#
# Phase 5.5 verification — a provisioning failure in onStart FAILS the block (build red).
#
# testng-fv-5.5.xml boots ONE REAL APIM block (block=fv-5.5) with initTenantUsers=true and
# tenantSet=adpsample. adpsample is the pre-migrated profile: addAdpsampleTenant only builds a context bean
# (no SOAP create), so on this FRESH (non-migrated) container adpsample.com does not exist server-side. The
# follow-up addUser SOAP authenticates as admin@adpsample.com and gets a non-200, so TenantUserProvisioner
# throws. Because provisioning runs inside BlockLifecycleListener.onStart's try, the throw is recorded as the
# bootError attribute (NOT surfaced mid-scenario), and BaseBlockRunner's @BeforeClass rethrows it as a hard
# IllegalStateException - the probe class FAILS (a @BeforeClass config FAILURE) with the provisioning failure
# as root cause.
#
# Asserts, after the run: Maven build FAILS (a boot failure must redden the build, not skip-to-green); the
# guard's abortIfBlockBootFailed config method is recorded FAILED (CONFIG_FAILS>=1), no NPE cascade; the
# Maven log carries the listener's boot-failure marker (the failure really came from the provisioning
# failure, not something else); the probe never ran, so NO block observation was recorded; and the container
# did not leak - onFinish stopped it even though provisioning failed after boot. Re-runnable / idempotent.
# Single PASS/FAIL line, non-zero on fail.
#
# Usage:  ./verify-5.5.sh
set -euo pipefail

STEP="5.5"
BLOCK_LABEL="fv-5.5"
LABEL_FILTER="label=block=${BLOCK_LABEL}"
SUITE_XML="testng-fv-5.5.xml"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
RESULTS_XML="${MODULE_DIR}/target/surefire-reports/testng-results.xml"
MVN_LOG="${MODULE_DIR}/target/verify-5.5-maven.log"

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

echo "== Phase ${STEP} verification: provisioning failure FAILS the block (build red) =="
rm -f "${OBS_FILE}" "${MVN_LOG}"
cleanup_containers

# A provisioning failure must FAIL the build (regression guard against silent skip-to-green): Maven must exit
# non-zero. We run inside `if` so set -e does not abort on the expected failure; the redirect to the log is
# preserved for diagnostics.
echo "Running verification suite via Maven..."
if ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml="${SUITE_XML}" test ) > "${MVN_LOG}" 2>&1; then
    tail -25 "${MVN_LOG}"
    fail "Maven build SUCCEEDED - a provisioning failure must FAIL the build, not skip-to-green (see ${MVN_LOG})"
fi

# Assertion 1: the guard rethrew the bootError as a @BeforeClass config FAILURE (no NPE cascade). (TestNG
# marks the class's @Test methods SKIPPED, but the failed config method is what reddens the build, so we
# assert on the config-method FAIL - not the root 'failed' attribute, which counts only @Test methods.)
[ -f "${RESULTS_XML}" ] || fail "testng-results.xml not produced: ${RESULTS_XML}"
CONFIG_FAILS="$(grep 'abortIfBlockBootFailed' "${RESULTS_XML}" | grep -c 'status="FAIL"' || true)"
[ "${CONFIG_FAILS}" -ge 1 ] \
    || fail "expected >=1 abortIfBlockBootFailed config FAILURE (the block must fail on provisioning failure), got ${CONFIG_FAILS}"

# Assertion 2: the failure really came from the provisioning failure recorded by the listener.
grep -q "boot/readiness failed" "${MVN_LOG}" \
    || fail "Maven log lacks the listener's boot-failure marker - failure may not be provisioning-driven (see ${MVN_LOG})"

# Assertion 3: the probe never ran (failed before its scenario), so it recorded no observation.
[ ! -f "${OBS_FILE}" ] \
    || fail "an observation was recorded - the failed probe unexpectedly ran its scenario: ${OBS_FILE}"

# Assertion 4: the container did not leak - onFinish stopped it despite the post-boot provisioning failure.
LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
[ -z "${LEFTOVER}" ] || fail "fv-5.5 container leaked after a provisioning-failure FAIL: ${LEFTOVER}"

echo "VERIFY ${STEP}: PASS - provisioning failure recorded as bootError; block FAILED (build red) via ${CONFIG_FAILS} config FAILURE(s), probe never ran, container released by onFinish, no leak"
