#!/usr/bin/env bash
#
# Phase C.1 verification — repeatability / flake soak.
#
# Runs the Phase 6.1 capstone suite (verify-6.1.sh) N times back-to-back. Each run must be green AND leave
# zero fv-6.1 containers behind, so the next run starts from a clean host. A single capstone run can hide
# port races, shared-scope races, and readiness-timing flakes that only surface across repeated boots/teardowns
# of the same docker label and the same dynamic-port pool; running it many times in a row flushes them out.
#
# Each iteration delegates ALL functional assertions to verify-6.1.sh (build success, per-block isolation,
# K/M caps, clean skip of the broken block, no in-run leak) - this soak adds only the cross-run guarantees:
#   1. Pre-flight: the host has zero fv-6.1 containers before the soak begins.
#   2. Every one of the N capstone runs exits 0 (green).
#   3. Between every run, zero fv-6.1 containers remain (no leak that the next run would inherit).
# Prints a single PASS/FAIL line, non-zero on the first failing run.
#
# Usage:  ./verify-C.1.sh [N]      (N = number of consecutive capstone runs; default 10)
set -euo pipefail

STEP="C.1"
BLOCK_LABEL="fv-6.1"
LABEL_FILTER="label=block=${BLOCK_LABEL}"
N="${1:-10}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERIFY_CAPSTONE="${SCRIPT_DIR}/verify-6.1.sh"

fail() { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

leftover_count() { docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null | grep -c . || true; }

case "${N}" in
    ''|*[!0-9]*) fail "N must be a positive integer, got '${N}'" ;;
esac
[ "${N}" -ge 1 ] || fail "N must be >= 1, got '${N}'"
[ -x "${VERIFY_CAPSTONE}" ] || fail "capstone verifier not found or not executable: ${VERIFY_CAPSTONE}"

echo "== Phase ${STEP} verification: repeatability / flake soak (${N} back-to-back capstone runs) =="

# Guarantee 1: start from a clean host so a pre-existing leak can't masquerade as a pass.
PRE="$(leftover_count)"
[ "${PRE}" = "0" ] || fail "found ${PRE} leftover fv-6.1 container(s) before the soak - clean up first"

PASSES=0
for i in $(seq 1 "${N}"); do
    echo "--- ${STEP} capstone run ${i}/${N} ---"
    if ! "${VERIFY_CAPSTONE}"; then
        fail "capstone run ${i}/${N} was NOT green - suite is not repeatable (see its maven log)"
    fi
    # Guarantee 3: no container may survive a run for the next one to inherit.
    BETWEEN="$(leftover_count)"
    [ "${BETWEEN}" = "0" ] \
        || fail "run ${i}/${N} left ${BETWEEN} fv-6.1 container(s) behind - cross-run leak"
    PASSES=$((PASSES + 1))
done

echo "VERIFY ${STEP}: PASS - capstone suite green ${PASSES}/${N} consecutive runs, zero leftover containers between runs"
