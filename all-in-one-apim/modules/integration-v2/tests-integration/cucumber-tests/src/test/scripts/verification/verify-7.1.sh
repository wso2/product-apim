#!/usr/bin/env bash
#
# Phase 7.1 verification — test-authoring context features (Type-A, no Docker).
#
# Exercises the context mechanisms the ported suite relies on but the original Phase 1-6 lane never did:
# ${UNIQUE:<base>} collision-free naming, {{key}} placeholder resolution (incl. fail-on-missing), and the
# local(runner) vs shared(block) TestContext scope semantics. Prints a single PASS/FAIL line; exits non-zero
# on failure.
#
# Usage:  ./verify-7.1.sh
set -euo pipefail

STEP="7.1"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root

fail() { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

echo "== Phase ${STEP} verification: test-authoring context features (no Docker) =="

echo "Running verification suite via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-7.1.xml test ); then
    fail "verification suite reported test failures (see Maven output above)"
fi

echo "VERIFY ${STEP}: PASS - \${UNIQUE} names collision-free, {{}} placeholders resolve (fail on missing), local=runner-scoped & shared=block-scoped"
