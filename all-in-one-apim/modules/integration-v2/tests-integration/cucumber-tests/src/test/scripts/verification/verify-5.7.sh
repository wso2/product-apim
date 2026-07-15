#!/usr/bin/env bash
#
# DEPRECATED 2026-06-30: superseded — its design assumptions changed (the actor model / basic-as-overlay /
# legacy-lane removal). Intentionally NOT run so fv sweeps stay green and nobody re-diagnoses an expected
# failure. Rationale + replacement are in docs/devs/parallel-framework-implementation-plan.md (the [-] note);
# the equivalent property is verified by Phase 7 against the current model.
echo "VERIFY 5.7: DEPRECATED (superseded) - not run; see parallel-framework-implementation-plan.md"
exit 0
#
# Phase 5.7 verification — provisioning idempotency (skip-if-exists no-ops on re-run).
#
# testng-fv-5.7.xml boots ONE REAL APIM block (block=fv-5.7) with initTenantUsers=true, so
# BlockLifecycleListener provisions the default tenant set once during onStart. BlockIdempotencyProbeRunner
# then provisions the SAME set a second time against the same container ("I provision the default tenant set
# again"): every create must hit TenantUserProvisioner's skip-if-exists branch and no-op. A broken
# skip-if-exists would attempt a re-create and the server would answer non-200, making that step throw - so a
# clean re-run IS the idempotency proof. The probe then asserts the tenant exists and the user exists EXACTLY
# ONCE on the live server (no duplicates), and records an observation.
#
# Asserts, after the run: Maven build SUCCEEDS (re-provision no-opped, exactly-once held); exactly 1
# observation across exactly 1 distinct real container id; and no fv-5.7 container leaked. Re-runnable /
# idempotent. Single PASS/FAIL line, non-zero on fail.
#
# Usage:  ./verify-5.7.sh
set -euo pipefail

STEP="5.7"
BLOCK_LABEL="fv-5.7"
LABEL_FILTER="label=block=${BLOCK_LABEL}"
SUITE_XML="testng-fv-5.7.xml"
EXPECTED_BLOCKS=1
EXPECTED_OBS=1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
MVN_LOG="${MODULE_DIR}/target/verify-5.7-maven.log"

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

echo "== Phase ${STEP} verification: provisioning idempotency (skip-if-exists no-ops on re-run) =="
rm -f "${OBS_FILE}" "${MVN_LOG}"
cleanup_containers

echo "Running verification suite via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml="${SUITE_XML}" test ) > "${MVN_LOG}" 2>&1; then
    tail -25 "${MVN_LOG}"
    fail "Maven build failed - re-provisioning was not idempotent (threw or created a duplicate; see ${MVN_LOG})"
fi

# Assertion 1: the probe ran against its own real container (re-provision no-op + exactly-once held).
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

# Assertion 2: no fv-5.7 container leaked.
LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
[ -z "${LEFTOVER}" ] || fail "fv-5.7 containers leaked after run: ${LEFTOVER}"

echo "VERIFY ${STEP}: PASS - re-provisioning the same set no-opped (skip-if-exists), tenant/user exist exactly once, ${EXPECTED_BLOCKS} container, no leaks"
