#!/usr/bin/env bash
#
# Phase 2.3 verification — no scope-map leak across many blocks.
#
# Type-A (no Docker): runs the isolated Phase 2.3 suite, which drives 20 short blocks through
# BlockScopeListener and asserts the TestContext shared/local scope maps return to baseline when each
# block tears down via clear() (no per-block buildup), while a control run without teardown leaves
# exactly one entry per block. Prints a single PASS/FAIL line; exits non-zero on failure.
#
# Usage:  ./verify-2.3.sh
set -euo pipefail

STEP="2.3"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
LEGACY_SUITE="${MODULE_DIR}/src/test/resources/testng.xml"

fail() { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

echo "== Phase ${STEP} verification: no scope-map leak across many blocks (no Docker) =="

# Regression guard: the legacy suite must NOT load the new parallel-lane listener.
if grep -q "BlockScopeListener" "${LEGACY_SUITE}"; then
    fail "legacy testng.xml references BlockScopeListener — it must stay legacy-only"
fi

echo "Running verification suite via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-2.3.xml test ); then
    fail "verification suite reported test failures (see Maven output above)"
fi

echo "VERIFY ${STEP}: PASS - cleared blocks reclaim their scope maps (no per-block buildup); clear() confirmed as the reclaiming mechanism"
