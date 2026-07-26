#!/usr/bin/env bash
#
# Phase 4.13 verification — the readiness gate rejects a partial boot.
#
# PartialBootReadinessVerificationTest boots a real Testcontainers nginx that LISTENS but 404s on the
# gateway health-check path (negative control): the mapped port is provably open, yet
# ServerReadiness.awaitReady must return false because the health-check never returns 200. A second nginx,
# configured to answer 200 on every path, must make the same gate return true (positive control). Together
# these prove readiness gates on the HTTP 200 health-check, not merely on Wait.forListeningPort - so a
# container that opens its port but never serves the app can never be falsely reported "ready".
#
# Asserts, after the run: Maven build SUCCEEDS; testng-results shows passed>=1, failed=0, skipped=0 (both
# controls are assertions inside one @Test); and no nginx stub with the block label leaked. Re-runnable /
# idempotent. Prints a single PASS/FAIL line, non-zero on fail.
#
# Usage:  ./verify-4.13.sh
set -euo pipefail

STEP="4.13"
BLOCK_LABEL="fv-4.13"
LABEL_FILTER="label=block=${BLOCK_LABEL}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
RESULTS_XML="${MODULE_DIR}/target/surefire-reports/testng-results.xml"
MVN_LOG="${MODULE_DIR}/target/verify-4.13-maven.log"

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

echo "== Phase ${STEP} verification: readiness gate rejects partial boot =="
rm -f "${RESULTS_XML}" "${MVN_LOG}"
cleanup_containers

echo "Running verification suite via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-4.13.xml test ) > "${MVN_LOG}" 2>&1; then
    tail -25 "${MVN_LOG}"
    fail "Maven build failed - the readiness gate mishandled a partial boot (false ready?) or a control failed"
fi

# Assertion 1: the test passed (both negative and positive controls held).
[ -f "${RESULTS_XML}" ] || fail "expected testng results not produced: ${RESULTS_XML}"
ROOT_ATTRS="$(grep -o '<testng-results[^>]*>' "${RESULTS_XML}" | head -1)"
get_attr() { printf '%s' "${ROOT_ATTRS}" | sed -n "s/.* $1=\"\([0-9]*\)\".*/\1/p"; }
PASSED="$(get_attr passed)"; FAILED="$(get_attr failed)"; SKIPPED="$(get_attr skipped)"
[ "${FAILED:-x}" = "0" ] || fail "expected 0 failed, got '${FAILED}': ${ROOT_ATTRS}"
[ "${SKIPPED:-x}" = "0" ] || fail "expected 0 skipped, got '${SKIPPED}': ${ROOT_ATTRS}"
[ "${PASSED:-0}" -ge 1 ] || fail "expected >=1 passed, got '${PASSED}': ${ROOT_ATTRS}"

# Assertion 2: no stub container leaked.
LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
[ -z "${LEFTOVER}" ] || fail "nginx stub containers leaked after run: ${LEFTOVER}"

echo "VERIFY ${STEP}: PASS - listening-but-non-200 stub rejected, 200-serving stub accepted (gate keys on HTTP 200), no leaks"
