#!/usr/bin/env bash
#
# Phase 4.1 verification — BaseBlockRunner boot-failure guard (fail, not skip).
#
# Type-A (no Docker): runs the isolated Phase 4.1 suite, which drives the package-private guard
# BaseBlockRunner.abortIfBlockBootFailed directly with a stub ITestContext. Asserts the guard rethrows the
# recorded bootError as a hard IllegalStateException ("APIM block boot failed") - a @BeforeClass
# configuration FAILURE - when boot failed (so block classes FAIL and the build goes RED, with no NPE
# cascade), and is a no-op when no bootError was recorded.
# Prints a single PASS/FAIL line; exits non-zero on failure.
#
# Usage:  ./verify-4.1.sh
set -euo pipefail

STEP="4.1"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root

fail() { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

echo "== Phase ${STEP} verification: BaseBlockRunner boot-failure guard - fail, not skip (no Docker) =="

echo "Running verification suite via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-4.1.xml test ); then
    fail "verification suite reported test failures (see Maven output above)"
fi

echo "VERIFY ${STEP}: PASS - recorded bootError is rethrown as a hard IllegalStateException @BeforeClass config FAILURE (build red, cause preserved); no-op when boot succeeded"
