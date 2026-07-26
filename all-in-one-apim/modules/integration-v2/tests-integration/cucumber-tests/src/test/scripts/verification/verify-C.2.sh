#!/usr/bin/env bash
#
# Phase C.2 verification — concurrent shared-scope write stress.
#
# Type-A (no Docker): runs the isolated C.2 suite, whose TestContextConcurrencyStressTest spins up many
# barrier-synchronized worker threads across many simulated block scopes (SCOPES * THREADS_PER_SCOPE) that
# all begin writing at the same instant and hammer TestContext.setShared/get/addToList. The test's own
# assertions enforce the three C.2 guarantees - no lost updates (every key readable with its exact value,
# both per-thread and in a final cross-thread sweep), no ConcurrentModification/ClassCast (interleaved
# reads + getList cast never throw; any worker throwable fails the test), and strict per-scope isolation
# (the scopeOwner sentinel never bleeds across scopes; per-thread local lists hold exactly their own
# appends) - so a green build already means the stress held.
#
# This script adds an independent check that the stress actually RAN at the expected scale (not silently
# skipped): it reads the marker file the test writes and asserts errors=0 and the expected total write
# count. Re-runnable / idempotent. Prints a single PASS/FAIL line; exits non-zero on failure.
#
# Usage:  ./verify-C.2.sh
set -euo pipefail

STEP="C.2"
SUITE_XML="testng-fv-C.2.xml"

# Must match the constants in TestContextConcurrencyStressTest.
SCOPES=8
THREADS_PER_SCOPE=4
KEYS_PER_THREAD=500
EXPECTED_WRITES=$(( SCOPES * THREADS_PER_SCOPE * KEYS_PER_THREAD ))

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
MARKER="${MODULE_DIR}/target/fv-c2-stress.txt"
MVN_LOG="${MODULE_DIR}/target/verify-C.2-maven.log"

fail() { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

echo "== Phase ${STEP} verification: concurrent shared-scope write stress (no Docker) =="
rm -f "${MARKER}" "${MVN_LOG}"

echo "Running verification suite via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml="${SUITE_XML}" test ) > "${MVN_LOG}" 2>&1; then
    tail -30 "${MVN_LOG}"
    fail "stress suite reported failures - a lost update, CME/CCE, or isolation breach under contention (see ${MVN_LOG})"
fi

# Assertion 1: the stress actually executed and wrote its marker.
[ -f "${MARKER}" ] || fail "stress marker not produced: ${MARKER} (test may have been skipped)"

# Assertion 2: the test reported zero worker errors.
grep -q '^errors=0$' "${MARKER}" \
    || fail "stress marker does not report errors=0 (see ${MARKER}): $(grep -E '^errors=' "${MARKER}" || echo 'no errors line')"

# Assertion 3: the stress ran at the expected scale (no silent shrink to a trivial run).
ACTUAL_WRITES="$(sed -n 's/^totalWrites=//p' "${MARKER}")"
[ "${ACTUAL_WRITES}" = "${EXPECTED_WRITES}" ] \
    || fail "expected ${EXPECTED_WRITES} total writes, marker reports '${ACTUAL_WRITES}' (${MARKER})"

# Assertion 4: every simulated block kept its own retained shared scope.
SCOPE_COUNT="$(sed -n 's/^sharedScopeCount=//p' "${MARKER}")"
[ -n "${SCOPE_COUNT}" ] && [ "${SCOPE_COUNT}" -ge "${SCOPES}" ] \
    || fail "expected >=${SCOPES} retained shared scopes, marker reports '${SCOPE_COUNT}' (${MARKER})"

echo "VERIFY ${STEP}: PASS - ${EXPECTED_WRITES} concurrent shared writes across ${SCOPES} scopes (${THREADS_PER_SCOPE} threads each), zero lost updates / CME / CCE / isolation breaches, ${SCOPE_COUNT} scopes retained"
