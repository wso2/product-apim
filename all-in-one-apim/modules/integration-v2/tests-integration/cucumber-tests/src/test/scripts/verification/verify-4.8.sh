#!/usr/bin/env bash
#
# Phase 4.8 verification — onFinish always releases, including on test FAILURE/ERROR.
#
# Two blocks run serially. Phase4.8-BlockFail boots a real container, records its observation, then the
# probe deliberately FAILS; Phase4.8-BlockError boots a real container, records, then THROWS (ERROR). In
# both cases BlockLifecycleListener.onFinish must still stop the block container and release its dynamic
# host ports — teardown is not conditional on success. The Maven build is therefore EXPECTED to report
# test failures (non-zero); this gate asserts the failures are the deliberate ones AND that teardown still
# happened.
#
# Asserts, after the run: Maven exited non-zero (the deliberate probes did fail); testng-results shows
# failed>=2 and passed=0; two observations across two distinct real container ids; no container with the
# block label leaked; and each recorded servlet-https host port becomes refused within a grace window
# (ports released). Re-runnable / idempotent. Prints a single PASS/FAIL line, non-zero on fail.
#
# Usage:  ./verify-4.8.sh
set -euo pipefail

STEP="4.8"
BLOCK_LABEL="fv-4.8"
LABEL_FILTER="label=block=${BLOCK_LABEL}"
RELEASE_GRACE_SECONDS=30
EXPECTED_OBS=2

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
RESULTS_XML="${MODULE_DIR}/target/surefire-reports/testng-results.xml"
MVN_LOG="${MODULE_DIR}/target/verify-4.8-maven.log"

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

# Polls a host:port until refused or the grace window elapses. 0 if released, 1 otherwise.
await_port_release() {
    local host="$1" port="$2" deadline=$((SECONDS + RELEASE_GRACE_SECONDS))
    while [ "${SECONDS}" -lt "${deadline}" ]; do
        if port_refused "${host}" "${port}"; then return 0; fi
        sleep 1
    done
    return 1
}

# Always clean up on exit so a crashed run never poisons the next.
trap cleanup_containers EXIT

echo "== Phase ${STEP} verification: onFinish always releases (incl. on FAILURE/ERROR) =="
rm -f "${OBS_FILE}" "${RESULTS_XML}" "${MVN_LOG}"
cleanup_containers

echo "Running verification suite via Maven (test failures are EXPECTED)..."
MVN_RC=0
( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-4.8.xml test ) > "${MVN_LOG}" 2>&1 || MVN_RC=$?

# Assertion 0: the deliberate probes really did fail the build (guards against a no-op/compile-skip pass).
[ "${MVN_RC}" != "0" ] || fail "Maven passed, but the deliberate fail/error probes should have failed the build"

# Assertion 1: results show exactly the deliberate failures (>=2 failed) and no spurious pass.
[ -f "${RESULTS_XML}" ] || { tail -25 "${MVN_LOG}"; fail "expected testng results not produced: ${RESULTS_XML}"; }
ROOT_ATTRS="$(grep -o '<testng-results[^>]*>' "${RESULTS_XML}" | head -1)"
get_attr() { printf '%s' "${ROOT_ATTRS}" | sed -n "s/.* $1=\"\([0-9]*\)\".*/\1/p"; }
FAILED="$(get_attr failed)"; PASSED="$(get_attr passed)"
[ "${FAILED:-0}" -ge 2 ] || fail "expected >=2 failed (the deliberate fail+error), got '${FAILED}': ${ROOT_ATTRS}"
[ "${PASSED:-x}" = "0" ] || fail "expected 0 passed, got '${PASSED}': ${ROOT_ATTRS}"

# Assertion 2: both blocks booted a real container and recorded an observation before failing.
[ -f "${OBS_FILE}" ] || fail "expected observation file not produced: ${OBS_FILE}"
OBS_COUNT="$(grep -c . "${OBS_FILE}" || true)"
[ "${OBS_COUNT}" = "${EXPECTED_OBS}" ] || fail "expected ${EXPECTED_OBS} observations, got ${OBS_COUNT}"
DISTINCT_IDS="$(awk -F'|' '{print $3}' "${OBS_FILE}" | sort -u)"
ID_COUNT="$(printf '%s\n' "${DISTINCT_IDS}" | grep -c . || true)"
[ "${ID_COUNT}" = "${EXPECTED_OBS}" ] \
    || fail "expected ${EXPECTED_OBS} distinct container ids (one per block), got ${ID_COUNT}: ${DISTINCT_IDS}"
if printf '%s\n' "${DISTINCT_IDS}" | grep -Eq '^(none|null)$'; then
    fail "a probe recorded a missing container id: ${DISTINCT_IDS}"
fi

# Assertion 3: teardown happened despite the failures - no container with the block label remains.
LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
[ -z "${LEFTOVER}" ] || fail "containers leaked after failed/errored blocks: ${LEFTOVER}"

# Assertion 4: each recorded servlet-https host port is released (refused) within the grace window.
while IFS= read -r base_url; do
    [ -n "${base_url}" ] || continue
    hostport="${base_url#*://}"; hostport="${hostport%%/*}"
    host="${hostport%%:*}"; port="${hostport##*:}"
    case "${host}" in localhost|127.0.0.1) ;; *) continue ;; esac
    await_port_release "${host}" "${port}" \
        || fail "host port ${host}:${port} still accepts connections ${RELEASE_GRACE_SECONDS}s after a failed block"
done < <(awk -F'|' '{print $4}' "${OBS_FILE}")

echo "VERIFY ${STEP}: PASS - both blocks failed as designed yet onFinish stopped ${EXPECTED_OBS} containers and released their ports, no leaks"
