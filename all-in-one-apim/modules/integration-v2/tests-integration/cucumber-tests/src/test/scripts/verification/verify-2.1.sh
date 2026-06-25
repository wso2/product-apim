#!/usr/bin/env bash
#
# DEPRECATED 2026-06-30: superseded — its design assumptions changed (the actor model / basic-as-overlay /
# legacy-lane removal). Intentionally NOT run so fv sweeps stay green and nobody re-diagnoses an expected
# failure. Rationale + replacement are in docs/devs/parallel-framework-implementation-plan.md (the [-] note);
# the equivalent property is verified by Phase 7 against the current model.
echo "VERIFY 2.1: DEPRECATED (superseded) - not run; see parallel-framework-implementation-plan.md"
exit 0
#
# Phase 2.1 verification — namespaced shared-scope key.
#
# Type-A (no Docker): runs the isolated Phase 2.1 suite, which orchestrates two nested TestNG suites
# with two same-named <test> blocks (one per suite) wired to BlockScopeListener, and asserts each
# block reads back its own shared sentinel (no cross-block merge) while the legacy bare-testName key
# would have collided. Also asserts the legacy testng.xml never references BlockScopeListener and the
# legacy TestNameMdcListener is untouched. Prints a single PASS/FAIL line; exits non-zero on failure.
#
# Usage:  ./verify-2.1.sh
set -euo pipefail

STEP="2.1"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
LEGACY_SUITE="${MODULE_DIR}/src/test/resources/testng.xml"

fail() { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

echo "== Phase ${STEP} verification: namespaced shared-scope key (no Docker) =="

# Regression guard 1: the legacy suite must NOT load the new parallel-lane listener.
if grep -q "BlockScopeListener" "${LEGACY_SUITE}"; then
    fail "legacy testng.xml references BlockScopeListener — it must stay legacy-only"
fi
# Regression guard 2: the legacy suite still wires TestNameMdcListener (unchanged behavior).
if ! grep -q "TestNameMdcListener" "${LEGACY_SUITE}"; then
    fail "legacy testng.xml no longer wires TestNameMdcListener — legacy scope behavior changed"
fi

echo "Running verification suite via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-2.1.xml test ); then
    fail "verification suite reported test failures (see Maven output above)"
fi

echo "VERIFY ${STEP}: PASS - same-named blocks across suites isolated; bare-name key collides; legacy untouched"
