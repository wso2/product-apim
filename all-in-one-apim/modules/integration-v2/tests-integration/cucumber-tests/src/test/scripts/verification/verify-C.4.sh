#!/usr/bin/env bash
#
# Phase C.4 verification — host-capacity sanity.
#
# Goal (from the plan): confirm the chosen K (suite parallel="tests" thread-count) fits the CI/dev Docker
# host (CPU/RAM/file-descriptors) without OOM or boot-timeout cascades; record the safe K and the observed
# per-container footprint.
#
# C.1 estimated the APIM footprint (~1.5-2 GB/container) from soak failures on an undersized VM. C.4 turns
# that estimate into a MEASURED, asserted check: it runs the real capstone suite at the chosen K=2
# (testng-framework-verification.xml - the same suite the soak uses) and, while it runs, samples each live
# block container's actual memory via `docker stats`, tracking the peak per-container and peak aggregate
# footprint. Then it asserts the host genuinely has headroom for the chosen K and records the data.
#
# Assertions:
#   1. The capstone produces all expected observations - i.e. at K=2 on THIS host every good block became
#      ready in time (no boot-timeout cascade) and nothing was OOM-killed mid-boot. NOTE: the capstone's
#      BrokenBlock is now EXPECTED to redden the build (its boot failure is a hard config FAILURE, not a
#      skip), so the Maven exit code is non-zero by design and is NOT the capacity signal; capacity is
#      certified by the good blocks' observations instead.
#   2. Peak live containers reached the chosen K (K=2 genuinely ran at once - capacity was actually exercised).
#   3. A real per-container footprint was measured (> 0 MiB) and recorded.
#   4. Headroom: the measured peak AGGREGATE footprint stayed at/under HEADROOM_PCT of the Docker host's
#      total memory - the host did not run to the edge, so there is margin against OOM.
#   5. Safe-K: from the measured per-container footprint and host memory, the host can support at least the
#      chosen K (computed safeMaxK >= K). safeMaxK is recorded as the headroom indicator.
#   6. File descriptors: the soft nofile limit is unlimited or comfortably above a per-container floor.
#
# Records everything to target/fv-c4-capacity.txt. Re-runnable / idempotent. Prints a single PASS/FAIL line;
# exits non-zero on failure.
#
# Usage:  ./verify-C.4.sh
set -euo pipefail

STEP="C.4"
BLOCK_LABEL="fv-6.1"                 # the capstone suite labels its containers block=fv-6.1
LABEL_FILTER="label=block=${BLOCK_LABEL}"
SUITE_XML="testng-framework-verification.xml"
K=2                                  # the chosen suite-level cap (max concurrent blocks/containers)
EXPECTED_OBS=4                       # capstone: 2 classes x 2 good blocks
HEADROOM_PCT=80                      # peak aggregate footprint must stay at/under this % of host memory
FD_FLOOR=1024                        # minimum acceptable soft nofile limit if it is not "unlimited"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
MVN_LOG="${MODULE_DIR}/target/verify-C.4-maven.log"
MARKER="${MODULE_DIR}/target/fv-c4-capacity.txt"

fail() { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

cleanup_containers() {
    local ids
    ids="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
    if [ -n "${ids}" ]; then
        docker rm -f ${ids} >/dev/null 2>&1 || true
    fi
}
trap cleanup_containers EXIT

# Convert a docker-stats memory token (e.g. "1.523GiB", "523.4MiB", "12KiB", "8B") to whole MiB.
to_mib() {
    awk -v s="$1" 'BEGIN{
        if (match(s,/[0-9.]+/)) { val=substr(s,RSTART,RLENGTH); unit=substr(s,RSTART+RLENGTH); }
        else { print 0; exit; }
        f=0;
        if (unit=="GiB"||unit=="GB") f=1024;
        else if (unit=="MiB"||unit=="MB") f=1;
        else if (unit=="KiB"||unit=="KB") f=1/1024;
        else if (unit=="B") f=1/1048576;
        printf "%.0f", val*f;
    }'
}

echo "== Phase ${STEP} verification: host-capacity sanity at the chosen K=${K} =="
rm -f "${OBS_FILE}" "${MVN_LOG}" "${MARKER}"
cleanup_containers

# Host/Docker capacity snapshot.
HOST_INFO="$(docker info --format '{{.MemTotal}}|{{.NCPU}}' 2>/dev/null || echo '0|0')"
HOST_MEM_BYTES="${HOST_INFO%%|*}"
HOST_CPUS="${HOST_INFO##*|}"
[ "${HOST_MEM_BYTES}" -gt 0 ] 2>/dev/null || fail "could not read Docker host total memory"
HOST_MEM_MIB=$(( HOST_MEM_BYTES / 1048576 ))
MEM_BUDGET_MIB=$(( HOST_MEM_MIB * HEADROOM_PCT / 100 ))
FD_LIMIT="$(ulimit -n)"
echo "Docker host: ${HOST_MEM_MIB} MiB RAM, ${HOST_CPUS} CPU(s); fd soft limit: ${FD_LIMIT}; budget (${HEADROOM_PCT}%): ${MEM_BUDGET_MIB} MiB"

echo "Running capstone suite at K=${K} via Maven (sampling per-container memory)..."
( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml="${SUITE_XML}" test ) > "${MVN_LOG}" 2>&1 &
MVN_PID=$!

MAX_LIVE=0
MAX_PER_MIB=0
MAX_AGG_MIB=0
while kill -0 "${MVN_PID}" 2>/dev/null; do
    IDS="$(docker ps -q --filter "${LABEL_FILTER}" 2>/dev/null || true)"
    LIVE="$(printf '%s\n' "${IDS}" | grep -c . || true)"
    [ "${LIVE}" -gt "${MAX_LIVE}" ] && MAX_LIVE="${LIVE}"
    if [ -n "${IDS}" ]; then
        AGG=0
        # docker stats MemUsage looks like "523.4MiB / 11.66GiB"; take the first token.
        while IFS= read -r usage; do
            [ -n "${usage}" ] || continue
            mib="$(to_mib "${usage%% /*}")"
            AGG=$(( AGG + mib ))
            [ "${mib}" -gt "${MAX_PER_MIB}" ] && MAX_PER_MIB="${mib}"
        done < <(docker stats --no-stream --format '{{.MemUsage}}' ${IDS} 2>/dev/null || true)
        [ "${AGG}" -gt "${MAX_AGG_MIB}" ] && MAX_AGG_MIB="${AGG}"
    fi
    sleep 2
done
wait "${MVN_PID}" && MVN_RC=0 || MVN_RC=$?

# Assertion 1: capacity is certified by the good blocks' OBSERVATIONS, not the Maven exit code. The
# capstone's BrokenBlock now reddens the build by design (its boot failure is a hard config FAILURE, not a
# skip), so MVN_RC is expected non-zero and is NOT a capacity signal. The real signal is that all good-block
# observations landed: if a good block had OOM'd or hit a boot-timeout under load at this K, it would be
# missing its observations. (MVN_RC is only used in a diagnostic tail below.)
[ -f "${OBS_FILE}" ] || { [ "${MVN_RC}" = "0" ] || tail -25 "${MVN_LOG}"; fail "no observation file produced: ${OBS_FILE} (a block likely failed to become ready - capacity/boot-timeout)"; }
OBS_COUNT="$(grep -c . "${OBS_FILE}" || true)"
[ "${OBS_COUNT}" = "${EXPECTED_OBS}" ] \
    || { [ "${MVN_RC}" = "0" ] || tail -25 "${MVN_LOG}"; fail "expected ${EXPECTED_OBS} observations at K=${K}, got ${OBS_COUNT} - a block may have hit a boot-timeout under load (see ${MVN_LOG})"; }

# Assertion 2: the chosen K was actually exercised (K containers ran at once).
[ "${MAX_LIVE}" -ge "${K}" ] \
    || fail "peak ${MAX_LIVE} live container(s) never reached the chosen K=${K} - capacity at K was not actually exercised"

# Assertion 3: a real per-container footprint was measured.
[ "${MAX_PER_MIB}" -gt 0 ] \
    || fail "measured a 0 MiB per-container footprint - docker stats sampling failed (cannot certify capacity)"

# Assertion 4: peak aggregate footprint stayed within the headroom budget (margin against OOM).
[ "${MAX_AGG_MIB}" -le "${MEM_BUDGET_MIB}" ] \
    || fail "peak aggregate footprint ${MAX_AGG_MIB} MiB exceeded the ${HEADROOM_PCT}% budget ${MEM_BUDGET_MIB} MiB of ${HOST_MEM_MIB} MiB - too close to OOM at K=${K}"

# Assertion 5: the host can support at least the chosen K (safeMaxK >= K).
SAFE_MAX_K=$(( MEM_BUDGET_MIB / MAX_PER_MIB ))
[ "${SAFE_MAX_K}" -ge "${K}" ] \
    || fail "host supports only K=${SAFE_MAX_K} at ${MAX_PER_MIB} MiB/container within ${MEM_BUDGET_MIB} MiB budget, but chosen K=${K}"

# Assertion 6: file-descriptor headroom (unlimited, or comfortably above a per-container floor).
if [ "${FD_LIMIT}" != "unlimited" ]; then
    [ "${FD_LIMIT}" -ge "${FD_FLOOR}" ] 2>/dev/null \
        || fail "fd soft limit ${FD_LIMIT} is below the floor ${FD_FLOOR} - risk of fd exhaustion under parallel containers"
fi

HEADROOM_USED_PCT=$(( MAX_AGG_MIB * 100 / HOST_MEM_MIB ))
{
    echo "chosenK=${K}"
    echo "peakLiveContainers=${MAX_LIVE}"
    echo "perContainerFootprintMiB=${MAX_PER_MIB}"
    echo "aggregatePeakMiB=${MAX_AGG_MIB}"
    echo "dockerHostMemMiB=${HOST_MEM_MIB}"
    echo "dockerHostCPUs=${HOST_CPUS}"
    echo "headroomBudgetPct=${HEADROOM_PCT}"
    echo "hostMemUsedAtPeakPct=${HEADROOM_USED_PCT}"
    echo "safeMaxK=${SAFE_MAX_K}"
    echo "fdSoftLimit=${FD_LIMIT}"
    echo "observations=${OBS_COUNT}"
    echo "result=PASS"
} > "${MARKER}"

echo "VERIFY ${STEP}: PASS - chosen K=${K} fits this host: peak ${MAX_LIVE} containers, ~${MAX_PER_MIB} MiB/container, peak aggregate ${MAX_AGG_MIB} MiB = ${HEADROOM_USED_PCT}% of ${HOST_MEM_MIB} MiB (budget ${MEM_BUDGET_MIB} MiB), safeMaxK=${SAFE_MAX_K}, fd=${FD_LIMIT}; no OOM/boot-timeout, ${OBS_COUNT} observations (marker: ${MARKER})"
