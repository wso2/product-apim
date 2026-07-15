#!/usr/bin/env bash
#
# DEPRECATED 2026-06-30: superseded — its design assumptions changed (the actor model / basic-as-overlay /
# legacy-lane removal). Intentionally NOT run so fv sweeps stay green and nobody re-diagnoses an expected
# failure. Rationale + replacement are in docs/devs/parallel-framework-implementation-plan.md (the [-] note);
# the equivalent property is verified by Phase 7 against the current model.
echo "VERIFY 5.6: DEPRECATED (superseded) - not run; see parallel-framework-implementation-plan.md"
exit 0
#
# Phase 5.6 verification — provisioning targets the block's OWN container under parallelism.
#
# testng-fv-5.6.xml runs TWO REAL APIM blocks in parallel (suite parallel="tests" thread-count="2"), both
# block=fv-5.6, both initTenantUsers=true with the DEFAULT tenant set - so both provision the SAME tenant
# domains (carbon.super + tenant1.com + testUser1/testUser11) at the same time. Each runs
# BlockProvisioningProbeRunner, whose scenario SOAP-retrieves tenants/users from ITS OWN block's baseUrl and
# asserts they exist there, then records an observation (millis|thread|containerId|baseUrl|baseGatewayUrl).
# A wrong-URL bug (a provisioner writing to the other block's container, or shared-scope crosstalk) would
# either fail a probe's in-container SOAP assertion or collapse the two blocks onto one container id / URL.
#
# Asserts, after the run: Maven build SUCCEEDS (each block's tenants existed in its OWN container); exactly 2
# observations across exactly 2 DISTINCT real container ids; and - the heart of this gate - exactly 2
# DISTINCT baseUrls (distinct mapped ports), proving each provisioner targeted its own container, not a
# shared one. Finally, no fv-5.6 container leaked. Re-runnable / idempotent. Single PASS/FAIL line.
#
# Usage:  ./verify-5.6.sh
set -euo pipefail

STEP="5.6"
BLOCK_LABEL="fv-5.6"
LABEL_FILTER="label=block=${BLOCK_LABEL}"
SUITE_XML="testng-fv-5.6.xml"
EXPECTED_BLOCKS=2
EXPECTED_OBS=2

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
MVN_LOG="${MODULE_DIR}/target/verify-5.6-maven.log"

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

echo "== Phase ${STEP} verification: provisioning targets the block's own container under parallelism =="
rm -f "${OBS_FILE}" "${MVN_LOG}"
cleanup_containers

echo "Running verification suite via Maven (two parallel blocks)..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml="${SUITE_XML}" test ) > "${MVN_LOG}" 2>&1; then
    tail -25 "${MVN_LOG}"
    fail "Maven build failed - a block's tenants were not in its own container (see ${MVN_LOG})"
fi

# Assertion 1: both blocks ran their probe - exactly two observations.
[ -f "${OBS_FILE}" ] || fail "expected observation file not produced: ${OBS_FILE}"
OBS_COUNT="$(grep -c . "${OBS_FILE}" || true)"
[ "${OBS_COUNT}" = "${EXPECTED_OBS}" ] || fail "expected ${EXPECTED_OBS} observations, got ${OBS_COUNT}"

# Assertion 2: two distinct real container ids - each block booted its own container.
DISTINCT_IDS="$(awk -F'|' '{print $3}' "${OBS_FILE}" | sort -u)"
ID_COUNT="$(printf '%s\n' "${DISTINCT_IDS}" | grep -c . || true)"
[ "${ID_COUNT}" = "${EXPECTED_BLOCKS}" ] \
    || fail "expected ${EXPECTED_BLOCKS} distinct container ids (one per block), got ${ID_COUNT}: ${DISTINCT_IDS}"
if printf '%s\n' "${DISTINCT_IDS}" | grep -Eq '^(none|null)$'; then
    fail "a probe recorded a missing container id: ${DISTINCT_IDS}"
fi

# Assertion 3 (the heart of the gate): two distinct baseUrls (distinct mapped ports) - each provisioner
# targeted its OWN container, not a shared URL.
DISTINCT_URLS="$(awk -F'|' '{print $4}' "${OBS_FILE}" | sort -u)"
URL_COUNT="$(printf '%s\n' "${DISTINCT_URLS}" | grep -c . || true)"
[ "${URL_COUNT}" = "${EXPECTED_BLOCKS}" ] \
    || fail "expected ${EXPECTED_BLOCKS} distinct baseUrls (per-container mapped ports), got ${URL_COUNT}: ${DISTINCT_URLS}"

# Assertion 4: no fv-5.6 container leaked.
LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
[ -z "${LEFTOVER}" ] || fail "fv-5.6 containers leaked after run: ${LEFTOVER}"

echo "VERIFY ${STEP}: PASS - two parallel blocks each provisioned and verified the SAME tenant set in their OWN container (${EXPECTED_BLOCKS} distinct container ids + ${EXPECTED_BLOCKS} distinct baseUrls), no leaks"
