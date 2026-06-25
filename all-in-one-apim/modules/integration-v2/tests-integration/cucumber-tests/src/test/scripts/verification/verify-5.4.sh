#!/usr/bin/env bash
#
# DEPRECATED 2026-06-30: superseded — its design assumptions changed (the actor model / basic-as-overlay /
# legacy-lane removal). Intentionally NOT run so fv sweeps stay green and nobody re-diagnoses an expected
# failure. Rationale + replacement are in docs/devs/parallel-framework-implementation-plan.md (the [-] note);
# the equivalent property is verified by Phase 7 against the current model.
echo "VERIFY 5.4: DEPRECATED (superseded) - not run; see parallel-framework-implementation-plan.md"
exit 0
#
# Phase 5.4 verification — legacy provisioning parity (regression).
#
# testng-fv-5.4.xml runs the UNCHANGED legacy trio in order against a REAL legacy fixed-port APIM container:
#   - SystemInitializationRunner  : boots the legacy APIMContainer (system_initialization.feature)
#   - TenantUserInitializationRunner: provisions + asserts the default tenant set
#                                     (tenant_users_initialisation.feature)
#   - SystemShutdown              : stops the legacy container (system_shutdown.feature)
# No blockLabel is declared, so BlockLifecycleListener OPTS OUT - the legacy lane drives its own lifecycle.
#
# This is a pure regression: it proves the 5.1/5.2 work (extracting TenantUserProvisioner and driving
# provisioning from BlockLifecycleListener) left the legacy provisioning path untouched and still green.
#
# Asserts, after the run: Maven build SUCCEEDS; testng-results report has passed>=1, failed=0, skipped=0
# (the legacy provisioning feature's own assertions held); and the new-lane observation file is NOT produced
# (the legacy lane is not a probe lane and must record nothing). Re-runnable / idempotent. Single PASS/FAIL
# line, non-zero on fail.
#
# Usage:  ./verify-5.4.sh
set -euo pipefail

STEP="5.4"
SUITE_XML="testng-fv-5.4.xml"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
RESULTS_XML="${MODULE_DIR}/target/surefire-reports/testng-results.xml"
MVN_LOG="${MODULE_DIR}/target/verify-5.4-maven.log"

fail() { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

echo "== Phase ${STEP} verification: legacy provisioning parity (regression) =="
rm -f "${OBS_FILE}" "${MVN_LOG}"

echo "Running legacy trio via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml="${SUITE_XML}" test ) > "${MVN_LOG}" 2>&1; then
    tail -25 "${MVN_LOG}"
    fail "Maven build failed - legacy provisioning regressed (see ${MVN_LOG})"
fi

# Assertion 1: the legacy runners passed - report shows passes, zero failures, zero skips.
[ -f "${RESULTS_XML}" ] || fail "testng-results.xml not produced: ${RESULTS_XML}"
HEADER="$(grep -o '<testng-results[^>]*>' "${RESULTS_XML}" | head -1)"
get_attr() { printf '%s' "${HEADER}" | sed -n "s/.* $1=\"\\([0-9]*\\)\".*/\\1/p"; }
PASSED="$(get_attr passed)"; FAILED="$(get_attr failed)"; SKIPPED="$(get_attr skipped)"
[ -n "${PASSED}" ] && [ "${PASSED}" -ge 1 ] || fail "expected passed>=1, got '${PASSED}' (${HEADER})"
[ "${FAILED}" = "0" ] || fail "expected failed=0, got '${FAILED}' (${HEADER})"
[ "${SKIPPED}" = "0" ] || fail "expected skipped=0, got '${SKIPPED}' (${HEADER})"

# Assertion 2: the legacy lane is not a probe lane - it must record no block observation.
[ ! -f "${OBS_FILE}" ] \
    || fail "legacy lane unexpectedly produced a block observation file (listener did not opt out): ${OBS_FILE}"

echo "VERIFY ${STEP}: PASS - legacy trio ran green (passed=${PASSED}, failed=0, skipped=0); listener opted out (no probe observation), legacy provisioning parity holds"
