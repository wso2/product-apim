#!/usr/bin/env bash
#
# Phase 4.6 verification — teardown + release for the parallel-on-shared-container model.
#
# A single successful block boots one DynamicApimContainer; once the probe passes, onFinish stops it so
# Docker releases its dynamic host ports. Asserts, after the run: the suite passed (1 observation with a
# real container id); the mapped servlet-https host port (from the probe's recorded baseUrl) becomes
# refused; and no container with the block label leaked.
#
# The host-port release is POLLED with a grace period rather than probed once: a stopped container's
# port forward can linger a beat on some Docker backends (e.g. colima), so a single immediate socket
# probe is a timing artifact - but a genuine leak never releases and is still caught. Re-runnable /
# idempotent. Prints a single PASS/FAIL line and exits non-zero on failure.
#
# Usage:  ./verify-4.6.sh
set -euo pipefail

STEP="4.6"
BLOCK_LABEL="fv-4.6"
LABEL_FILTER="label=block=${BLOCK_LABEL}"
RELEASE_GRACE_SECONDS=30

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

# Returns 0 if the host:port is refused (released), 1 if it still accepts a connection.
port_refused() {
    if (exec 3<>"/dev/tcp/$1/$2") 2>/dev/null; then
        exec 3>&- 3<&- 2>/dev/null || true
        return 1
    fi
    return 0
}

# Always clean up on exit so a crashed run never poisons the next.
trap cleanup_containers EXIT

echo "== Phase ${STEP} verification: teardown + release =="
rm -f "${OBS_FILE}"
cleanup_containers

echo "Running verification suite via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-4.6.xml test ); then
    fail "verification suite reported test failures (see Maven output above)"
fi

# Assertion 1: the block ran successfully against a real booted container.
[ -f "${OBS_FILE}" ] || fail "expected observation file not produced: ${OBS_FILE}"
OBS_COUNT="$(grep -c . "${OBS_FILE}" || true)"
[ "${OBS_COUNT}" -ge 1 ] || fail "expected >=1 probe observation, got ${OBS_COUNT}"
CONTAINER_ID="$(awk -F'|' 'NR==1{print $3}' "${OBS_FILE}")"
case "${CONTAINER_ID}" in none|null|"") fail "probe recorded a missing container id '${CONTAINER_ID}'" ;; esac
BASE_URL="$(awk -F'|' 'NR==1{print $4}' "${OBS_FILE}")"

# Assertion 2: the container was torn down - no container with the block label remains.
LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
[ -z "${LEFTOVER}" ] || fail "containers leaked after run: ${LEFTOVER}"

# Assertion 3: the mapped servlet-https host port is released (refused) within the grace window.
HOSTPORT="${BASE_URL#*://}"; HOSTPORT="${HOSTPORT%%/*}"
HOST="${HOSTPORT%%:*}"; PORT="${HOSTPORT##*:}"
if [ "${HOST}" = "localhost" ] || [ "${HOST}" = "127.0.0.1" ]; then
    RELEASED=0
    DEADLINE=$((SECONDS + RELEASE_GRACE_SECONDS))
    while [ "${SECONDS}" -lt "${DEADLINE}" ]; do
        if port_refused "${HOST}" "${PORT}"; then RELEASED=1; break; fi
        sleep 1
    done
    [ "${RELEASED}" = "1" ] \
        || fail "host port ${HOST}:${PORT} still accepts connections ${RELEASE_GRACE_SECONDS}s after stop"
fi

echo "VERIFY ${STEP}: PASS - block container stopped, mapped host port released, no leaks"
