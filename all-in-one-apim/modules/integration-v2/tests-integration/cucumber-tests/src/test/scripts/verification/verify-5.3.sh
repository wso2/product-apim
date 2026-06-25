#!/usr/bin/env bash
#
# DEPRECATED 2026-06-30: superseded — its design assumptions changed (the actor model / basic-as-overlay /
# legacy-lane removal). Intentionally NOT run so fv sweeps stay green and nobody re-diagnoses an expected
# failure. Rationale + replacement are in docs/devs/parallel-framework-implementation-plan.md (the [-] note);
# the equivalent property is verified by Phase 7 against the current model.
echo "VERIFY 5.3: DEPRECATED (superseded) - not run; see parallel-framework-implementation-plan.md"
exit 0
#
# Phase 5.3 verification — lifecycle provisioning lands in the freshly booted container.
#
# testng-fv-5.3.xml runs ONE REAL APIM block (block=fv-5.3) with initTenantUsers=true and no tenantSet, so
# BlockLifecycleListener boots its DynamicApimContainer, gates on readiness, then provisions the DEFAULT
# tenant set (super tenant + tenant1.com + users testUser1/testUser11) into THIS block's own container
# during onStart. BlockProvisioningProbeRunner asserts three things in one scenario:
#   - the provisioned tenant/user beans are readable from the block's shared scope under the tenant-domain
#     key (carbon.super + tenant1.com, with admin + keyed users);
#   - CURRENT_TENANT resolves off those shared beans exactly as the publisher runners do;
#   - the tenants/users ACTUALLY exist in the live server - queried back via the same SOAP the legacy init
#     steps use (retrieve tenant details / retrieve users), proving provisioning reached the booted server.
#
# Asserts, after the run: Maven build SUCCEEDS (all probe assertions held); exactly 1 observation across
# exactly 1 distinct real container id (the block booted its own container and the probe ran on it); and no
# fv-5.3 container leaked. Re-runnable / idempotent. Prints a single PASS/FAIL line, non-zero on fail.
#
# Usage:  ./verify-5.3.sh
set -euo pipefail

STEP="5.3"
BLOCK_LABEL="fv-5.3"
LABEL_FILTER="label=block=${BLOCK_LABEL}"
EXPECTED_BLOCKS=1
EXPECTED_OBS=1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
MVN_LOG="${MODULE_DIR}/target/verify-5.3-maven.log"

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

echo "== Phase ${STEP} verification: lifecycle provisioning lands in the freshly booted container =="
rm -f "${OBS_FILE}" "${MVN_LOG}"
cleanup_containers

echo "Running verification suite via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-5.3.xml test ) > "${MVN_LOG}" 2>&1; then
    tail -25 "${MVN_LOG}"
    fail "Maven build failed - a provisioning probe assertion failed (see ${MVN_LOG})"
fi

# Assertion 1: the block ran against its own real container.
[ -f "${OBS_FILE}" ] || fail "expected observation file not produced: ${OBS_FILE}"
OBS_COUNT="$(grep -c . "${OBS_FILE}" || true)"
[ "${OBS_COUNT}" = "${EXPECTED_OBS}" ] || fail "expected ${EXPECTED_OBS} observation, got ${OBS_COUNT}"
DISTINCT_IDS="$(awk -F'|' '{print $3}' "${OBS_FILE}" | sort -u)"
ID_COUNT="$(printf '%s\n' "${DISTINCT_IDS}" | grep -c . || true)"
[ "${ID_COUNT}" = "${EXPECTED_BLOCKS}" ] \
    || fail "expected ${EXPECTED_BLOCKS} distinct container id, got ${ID_COUNT}: ${DISTINCT_IDS}"
if printf '%s\n' "${DISTINCT_IDS}" | grep -Eq '^(none|null)$'; then
    fail "the probe recorded a missing container id: ${DISTINCT_IDS}"
fi

# Assertion 2: no fv-5.3 container leaked (onFinish stopped it).
LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
[ -z "${LEFTOVER}" ] || fail "fv-5.3 containers leaked after run: ${LEFTOVER}"

echo "VERIFY ${STEP}: PASS - provisioning landed in the freshly booted container: shared-scope beans + CURRENT_TENANT readable and tenants/users confirmed on the live server via SOAP, ${EXPECTED_BLOCKS} container, no leaks"
