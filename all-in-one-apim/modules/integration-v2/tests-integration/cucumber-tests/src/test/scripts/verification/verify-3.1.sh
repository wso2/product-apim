#!/usr/bin/env bash
#
# DEPRECATED 2026-06-30: superseded — its design assumptions changed (the actor model / basic-as-overlay /
# legacy-lane removal). Intentionally NOT run so fv sweeps stay green and nobody re-diagnoses an expected
# failure. Rationale + replacement are in docs/devs/parallel-framework-implementation-plan.md (the [-] note);
# the equivalent property is verified by Phase 7 against the current model.
echo "VERIFY 3.1: DEPRECATED (superseded) - not run; see parallel-framework-implementation-plan.md"
exit 0
#
# Phase 3.1 verification — suite/test-name uniqueness lint.
#
# Type-A (no Docker): runs the isolated Phase 3.1 suite, which drives the new BlockUniquenessLintListener
# through in-memory XmlSuites: duplicate composite suiteName::testName (across suites and across a child
# suite) is rejected, an unnamed/default-named suite is rejected, valid distinct composites pass, and a
# real nested TestNG run with a duplicate fails fast at load (the probe never executes). Also asserts the
# legacy testng.xml never references the lint listener and still wires the legacy alter-suite listener.
# Prints a single PASS/FAIL line; exits non-zero on failure.
#
# Usage:  ./verify-3.1.sh
set -euo pipefail

STEP="3.1"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
LEGACY_SUITE="${MODULE_DIR}/src/test/resources/testng.xml"

fail() { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

echo "== Phase ${STEP} verification: suite/test-name uniqueness lint (no Docker) =="

# Regression guard 1: the legacy suite must NOT load the new lint listener.
if grep -q "BlockUniquenessLintListener" "${LEGACY_SUITE}"; then
    fail "legacy testng.xml references BlockUniquenessLintListener — it must stay new-lane-only"
fi
# Regression guard 2: the legacy alter-suite listener is left untouched (still wired).
if ! grep -q "ParallelToggleAlterSuiteListener" "${LEGACY_SUITE}"; then
    fail "legacy testng.xml no longer wires ParallelToggleAlterSuiteListener — legacy behavior changed"
fi

echo "Running verification suite via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-3.1.xml test ); then
    fail "verification suite reported test failures (see Maven output above)"
fi

echo "VERIFY ${STEP}: PASS - duplicate/unnamed composites rejected at load (fail-fast, no block run); distinct composites pass; legacy untouched"
