#!/usr/bin/env bash
#
# Phase 7.2/7.3/7.4/7.7/7.8 verification (consolidated, Type-B) — test-authoring framework features.
#
# One block boot exercises: tenancy provisioning+routing (7.2), the actor/Identity model + auth keys (7.3),
# multi-feature runner setup-handoff (7.4), gateway invocation wiring (7.7), and the extra-overlay merge (7.8).
# Re-runnable: pre-cleans any leftover framework-features containers, runs the suite, asserts no leaks.
# Prints a single PASS/FAIL line; exits non-zero on failure.
#
# Usage:  ./verify-7.2.sh
set -euo pipefail

STEP="7.2"
LABEL_FILTER="label=block=framework-features"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"

fail() { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

cleanup_containers() {
    local ids
    ids="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
    if [ -n "${ids}" ]; then docker rm -f ${ids} >/dev/null 2>&1 || true; fi
}
trap cleanup_containers EXIT

echo "== Phase ${STEP} verification: test-authoring framework features (tenancy/actor/handoff/gateway/overlay) =="
cleanup_containers

if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-7.2.xml test ); then
    fail "verification suite reported test failures (see Maven output above)"
fi

LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
if [ -n "${LEFTOVER}" ]; then fail "containers leaked after run: ${LEFTOVER}"; fi

echo "VERIFY ${STEP}: PASS - tenancy routing (7.2), actor model+auth keys (7.3), setup-handoff (7.4), gateway invocation (7.7), extra-overlay merge (7.8); no leaks"
