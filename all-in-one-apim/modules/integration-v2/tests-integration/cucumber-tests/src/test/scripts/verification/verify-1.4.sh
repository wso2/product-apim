#!/usr/bin/env bash
#
# Phase 1.4 verification — misuse + abnormal-termination behavior of DynamicApimContainer.
#
# Asserts (in-JVM) that getMappedPort() before start() fails fast, and that a hard `docker kill`
# releases the host port (no host-port leak) while leaving an exited container record that must be
# pruned explicitly (the test prunes it itself). Re-runnable / idempotent: pre-cleans any leftover
# verify-1.4 containers, runs the isolated Phase 1.4 suite, then asserts no Docker container leaks.
# Prints a single PASS/FAIL line and exits non-zero on failure.
#
# Usage:  ./verify-1.4.sh
set -euo pipefail

STEP="1.4"
LABEL_FILTER="label=verify-step=${STEP}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root

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

echo "== Phase ${STEP} verification: misuse + abnormal-termination behavior =="
cleanup_containers

echo "Running verification suite via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-1.4.xml test ); then
    fail "verification suite reported test failures (see Maven output above)"
fi

# Post-exit assertion: no containers with our label leaked (the test self-prunes the killed record).
LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
if [ -n "${LEFTOVER}" ]; then
    fail "containers leaked after run: ${LEFTOVER}"
fi

echo "VERIFY ${STEP}: PASS - getMappedPort fails fast before start; abnormal kill releases host port, leaves prunable record"
