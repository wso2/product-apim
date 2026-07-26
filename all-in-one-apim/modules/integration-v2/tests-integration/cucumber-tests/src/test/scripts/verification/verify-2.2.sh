#!/usr/bin/env bash
#
# Phase 2.2 verification — shared-scope visibility on the executing worker thread.
#
# Type-A (no Docker): runs the isolated Phase 2.2 suite, which orchestrates a nested parallel TestNG
# suite (parallel="tests" + data-provider-thread-count=2) wired to BlockScopeListener. The global
# scope is pre-seeded with a POISON sentinel; every concurrent invocation must read back its own block
# id — never the global POISON (scope not visible on the worker thread) nor a sibling block's value
# (cross-block leak). Prints a single PASS/FAIL line; exits non-zero on failure.
#
# Usage:  ./verify-2.2.sh
set -euo pipefail

STEP="2.2"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
LEGACY_SUITE="${MODULE_DIR}/src/test/resources/testng.xml"

fail() { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

echo "== Phase ${STEP} verification: scope visibility on worker threads (no Docker) =="

# Regression guard: the legacy suite must NOT load the new parallel-lane listener.
if grep -q "BlockScopeListener" "${LEGACY_SUITE}"; then
    fail "legacy testng.xml references BlockScopeListener — it must stay legacy-only"
fi

echo "Running verification suite via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-2.2.xml test ); then
    fail "verification suite reported test failures (see Maven output above)"
fi

echo "VERIFY ${STEP}: PASS - per-invocation scope visible on worker threads; no global POISON, no sibling leak"
