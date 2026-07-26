#!/usr/bin/env bash
#
# Phase 1.1 verification — DynamicApimContainer dynamic-port lifecycle.
#
# Re-runnable / idempotent: pre-cleans any leftover verify-1.1 containers, runs the isolated
# verification suite (which inherits the real surefire env), then asserts no Docker container
# leaks and that the recorded host port is released. Prints a single PASS/FAIL line and exits
# non-zero on failure.
#
# Usage:  ./verify-1.1.sh
set -euo pipefail

STEP="1.1"
LABEL_FILTER="label=verify-step=${STEP}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
PORT_FILE="${MODULE_DIR}/target/verify-1.1-servlet-https-port.txt"

fail()  { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

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

echo "== Phase ${STEP} verification: DynamicApimContainer =="
rm -f "${PORT_FILE}"
cleanup_containers

# Run the isolated verification suite. -am rebuilds the testcontainers/util deps so the latest
# DynamicApimContainer code is exercised.
echo "Running verification suite via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-1.1.xml test ); then
    fail "verification suite reported test failures (see Maven output above)"
fi

# Post-exit assertion 1: no containers with our label leaked.
LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
if [ -n "${LEFTOVER}" ]; then
    fail "containers leaked after run: ${LEFTOVER}"
fi

# Post-exit assertion 2: the recorded mapped host port is released (connection refused).
if [ -f "${PORT_FILE}" ]; then
    HOSTPORT="$(cat "${PORT_FILE}")"
    HOST="${HOSTPORT%%:*}"
    PORT="${HOSTPORT##*:}"
    if [ "${HOST}" = "localhost" ] || [ "${HOST}" = "127.0.0.1" ]; then
        if (exec 3<>"/dev/tcp/${HOST}/${PORT}") 2>/dev/null; then
            exec 3>&- 3<&- 2>/dev/null || true
            fail "host port ${HOST}:${PORT} still accepts connections after stop"
        fi
    fi
else
    fail "expected port-record file not produced: ${PORT_FILE}"
fi

echo "VERIFY ${STEP}: PASS - dynamic ports mapped, server became ready, ports released, no leaks"
