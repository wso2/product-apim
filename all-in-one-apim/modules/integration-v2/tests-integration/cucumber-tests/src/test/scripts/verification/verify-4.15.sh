#!/usr/bin/env bash
#
# DEPRECATED 2026-06-30: superseded — its design assumptions changed (the actor model / basic-as-overlay /
# legacy-lane removal). Intentionally NOT run so fv sweeps stay green and nobody re-diagnoses an expected
# failure. Rationale + replacement are in docs/devs/parallel-framework-implementation-plan.md (the [-] note);
# the equivalent property is verified by Phase 7 against the current model.
echo "VERIFY 4.15: DEPRECATED (superseded) - not run; see parallel-framework-implementation-plan.md"
exit 0
#
# Phase 4.15 verification — mixed-lane co-existence (legacy block + new lifecycle block in one suite).
#
# testng-fv-4.15.xml mixes BOTH execution models in two serial blocks:
#   - Phase4.15-Legacy sets NO blockLabel: it self-boots a legacy fixed-port APIMContainer
#     (SystemInitializationRunner) and stops it (SystemShutdown). BlockLifecycleListener must OPT OUT.
#   - Phase4.15-Lifecycle sets blockLabel=fv-4.15: BlockLifecycleListener boots its DynamicApimContainer
#     and the probe records one observation.
#
# Asserts, after the run: Maven build SUCCEEDS (both lanes pass); exactly 1 observation (only the lifecycle
# probe records) across exactly 1 distinct real container id; and - the heart of this gate - NO container
# (running or exited) carries the docker label block=Phase4.15-Legacy. That label is the fallback the
# listener WOULD have stamped on a container booted for the legacy block before the opt-in; its absence
# proves the listener no-opped for the un-opted-in legacy block rather than booting a stray container.
# Finally, no fv-4.15 lifecycle container may leak. Re-runnable / idempotent. Single PASS/FAIL line.
#
# Usage:  ./verify-4.15.sh
set -euo pipefail

STEP="4.15"
LIFECYCLE_LABEL="fv-4.15"
LIFECYCLE_FILTER="label=block=${LIFECYCLE_LABEL}"
# The label the listener would have used as its fallback for the legacy <test> if it had NOT opted out.
LEGACY_FALLBACK_FILTER="label=block=Phase4.15-Legacy"
EXPECTED_OBS=1
EXPECTED_BLOCKS=1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
MVN_LOG="${MODULE_DIR}/target/verify-4.15-maven.log"

fail() { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

cleanup_containers() {
    local ids
    # Gather both labels (lifecycle + the legacy fallback the listener must NOT have used) and dedupe.
    ids="$( { docker ps -aq --filter "${LIFECYCLE_FILTER}" 2>/dev/null || true; \
              docker ps -aq --filter "${LEGACY_FALLBACK_FILTER}" 2>/dev/null || true; } | sort -u )"
    if [ -n "${ids}" ]; then
        echo "Cleaning up verify-${STEP} containers: ${ids}"
        docker rm -f ${ids} >/dev/null 2>&1 || true
    fi
}

# Always clean up on exit so a crashed run never poisons the next.
trap cleanup_containers EXIT

echo "== Phase ${STEP} verification: mixed-lane co-existence (legacy block + lifecycle block) =="
rm -f "${OBS_FILE}" "${MVN_LOG}"
cleanup_containers

echo "Running verification suite via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-4.15.xml test ) > "${MVN_LOG}" 2>&1; then
    tail -25 "${MVN_LOG}"
    fail "Maven build failed - a lane failed to co-exist (see ${MVN_LOG})"
fi

# Assertion 1 (the heart of the gate): the listener no-opped for the un-opted-in legacy block - it did NOT
# boot a fallback-labelled container for it.
STRAY="$(docker ps -aq --filter "${LEGACY_FALLBACK_FILTER}" 2>/dev/null || true)"
[ -z "${STRAY}" ] \
    || fail "listener booted a stray container for the legacy block (label block=Phase4.15-Legacy): ${STRAY}"

# Assertion 2: only the lifecycle probe recorded - exactly one observation, one real container id.
[ -f "${OBS_FILE}" ] || fail "expected observation file not produced: ${OBS_FILE}"
OBS_COUNT="$(grep -c . "${OBS_FILE}" || true)"
[ "${OBS_COUNT}" = "${EXPECTED_OBS}" ] \
    || fail "expected ${EXPECTED_OBS} observation (only the lifecycle probe records), got ${OBS_COUNT}"
DISTINCT_IDS="$(awk -F'|' '{print $3}' "${OBS_FILE}" | sort -u)"
ID_COUNT="$(printf '%s\n' "${DISTINCT_IDS}" | grep -c . || true)"
[ "${ID_COUNT}" = "${EXPECTED_BLOCKS}" ] \
    || fail "expected ${EXPECTED_BLOCKS} distinct lifecycle container id, got ${ID_COUNT}: ${DISTINCT_IDS}"
if printf '%s\n' "${DISTINCT_IDS}" | grep -Eq '^(none|null)$'; then
    fail "the lifecycle probe recorded a missing container id: ${DISTINCT_IDS}"
fi

# Assertion 3: no lifecycle container leaked (onFinish stopped it).
LEFTOVER="$(docker ps -aq --filter "${LIFECYCLE_FILTER}" 2>/dev/null || true)"
[ -z "${LEFTOVER}" ] || fail "fv-4.15 lifecycle containers leaked after run: ${LEFTOVER}"

echo "VERIFY ${STEP}: PASS - legacy + lifecycle blocks co-existed; listener no-opped for the un-opted-in legacy block (no stray container), lifecycle probe ran on its own container, no leaks"
