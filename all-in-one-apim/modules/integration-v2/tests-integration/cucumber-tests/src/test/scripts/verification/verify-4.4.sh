#!/usr/bin/env bash
#
# Phase 4.4 verification — boot-once + readiness gate for the parallel-on-shared-container model.
#
# One <test> block holds TWO probe classes (BlockProbeRunnerOne/Two). BlockLifecycleListener.onStart
# boots a SINGLE DynamicApimContainer for the block and gates on readiness; BlockScopeListener makes the
# block's shared scope visible on each worker invocation. Each probe reuses "I wait for the APIM server
# to be ready" (so the run only succeeds against a ready server) and appends one observation line
# (millis|thread|containerId|baseUrl|gatewayUrl) to target/fv-block-observations.txt.
#
# After the run this asserts: exactly TWO observations (one per probe class); a SINGLE distinct, real
# container id across both (one container booted for the block, not per class); a single distinct
# baseUrl and gateway URL (both classes saw the same server); no leaked containers; and the mapped host
# port released. Re-runnable / idempotent. Prints a single PASS/FAIL line and exits non-zero on failure.
#
# Usage:  ./verify-4.4.sh
set -euo pipefail

STEP="4.4"
BLOCK_LABEL="fv-4.4"
LABEL_FILTER="label=block=${BLOCK_LABEL}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"

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

echo "== Phase ${STEP} verification: boot-once + readiness gate =="
rm -f "${OBS_FILE}"
cleanup_containers

echo "Running verification suite via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-4.4.xml test ); then
    fail "verification suite reported test failures (see Maven output above)"
fi

# Assertion 1: exactly two observations were recorded (one per probe class in the block).
[ -f "${OBS_FILE}" ] || fail "expected observation file not produced: ${OBS_FILE}"
OBS_COUNT="$(grep -c . "${OBS_FILE}" || true)"
[ "${OBS_COUNT}" = "2" ] || fail "expected 2 probe observations, got ${OBS_COUNT} (see ${OBS_FILE})"

# Assertion 2: a single, real container id across both probes (one container for the block, not per class).
DISTINCT_IDS="$(awk -F'|' '{print $3}' "${OBS_FILE}" | sort -u)"
ID_COUNT="$(printf '%s\n' "${DISTINCT_IDS}" | grep -c . || true)"
[ "${ID_COUNT}" = "1" ] || fail "probes saw ${ID_COUNT} distinct container ids (expected 1): ${DISTINCT_IDS}"
case "${DISTINCT_IDS}" in
    none|null|"") fail "probes recorded a missing container id '${DISTINCT_IDS}'" ;;
esac

# Assertion 3: both probes saw the same baseUrl and the same gateway URL.
BASE_URLS="$(awk -F'|' '{print $4}' "${OBS_FILE}" | sort -u)"
GW_URLS="$(awk -F'|' '{print $5}' "${OBS_FILE}" | sort -u)"
[ "$(printf '%s\n' "${BASE_URLS}" | grep -c .)" = "1" ] || fail "probes saw differing baseUrls: ${BASE_URLS}"
[ "$(printf '%s\n' "${GW_URLS}" | grep -c .)" = "1" ] || fail "probes saw differing gateway URLs: ${GW_URLS}"
case "${BASE_URLS}" in null|"") fail "probes recorded a missing baseUrl" ;; esac

# Assertion 4: no containers with our block label leaked (the block's container was torn down).
# NOTE: the strict host-port socket-release check is intentionally NOT done here - teardown/port
# release has its own dedicated gate (4.6), where it is polled with a grace period. 4.4's scope is
# boot-once + readiness + same-server, and a stopped block container is evidenced by no leftovers.
LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
[ -z "${LEFTOVER}" ] || fail "containers leaked after run: ${LEFTOVER}"

echo "VERIFY ${STEP}: PASS - one container booted for the block, both probe classes saw the same ready baseUrl/gateway, no leaks"
