#!/usr/bin/env bash
#
# Phase 7.9 verification — capability taxonomy lint (Type-A, no Docker).
#
# render_coverage_tree.py is the framework guardrail that enforces the closed @cap/@feat vocabulary, the
# @setup <-> _setup_* filename rule, and valid @type values. This verifies the linter actually distinguishes
# good from bad: a valid fixture lints clean (invalid: 0, exit 0); an unknown @cap and a _setup_-without-@setup
# fixture each land in the invalid bucket and make it exit non-zero. Runs against TEMP fixtures via --features,
# so the real feature tree is never touched. Prints a single PASS/FAIL line.
#
# Usage:  ./verify-7.9.sh
set -euo pipefail

STEP="7.9"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
DEVS_DIR="$(cd "${MODULE_DIR}/../../docs/devs" && pwd)"
LINT="${DEVS_DIR}/render_coverage_tree.py"
MAP="${DEVS_DIR}/capability-map.yml"

TMP="$(mktemp -d)"
trap 'rm -rf "${TMP}"' EXIT
fail() { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

echo "== Phase ${STEP} verification: capability taxonomy lint (no Docker) =="
[ -f "${LINT}" ] || fail "linter not found: ${LINT}"

# 1) Valid fixture -> lints clean (exit 0, invalid: 0).
mkdir -p "${TMP}/good"
cat > "${TMP}/good/ok.feature" <<'EOF'
Feature: FV 7.9 valid
  @cap:publisher @feat:api-lifecycle @type:smoke
  Scenario: valid taxonomy
    Given a step
EOF
if ! out="$(python3 "${LINT}" --map "${MAP}" --features "${TMP}/good" --out "${TMP}/good.md" 2>&1)"; then
    fail "linter exited non-zero on a VALID fixture: ${out}"
fi
echo "${out}" | grep -qE "invalid: 0" || fail "linter did not report 'invalid: 0' for a valid fixture: ${out}"

# 2) Unknown @cap/@feat -> invalid bucket, non-zero exit.
mkdir -p "${TMP}/bad_cap"
cat > "${TMP}/bad_cap/bad.feature" <<'EOF'
Feature: FV 7.9 unknown cap
  @cap:bogus @feat:nope @type:smoke
  Scenario: unknown taxonomy
    Given a step
EOF
if python3 "${LINT}" --map "${MAP}" --features "${TMP}/bad_cap" --out "${TMP}/bad_cap.md" >/dev/null 2>&1; then
    fail "linter exited 0 for an UNKNOWN @cap/@feat fixture (should reject)"
fi

# 3) _setup_* filename without @setup tag -> invalid bucket, non-zero exit (bidirectional rule).
mkdir -p "${TMP}/bad_setup"
cat > "${TMP}/bad_setup/_setup_orphan.feature" <<'EOF'
Feature: FV 7.9 setup filename without setup tag
  @cap:publisher @feat:api-lifecycle @type:smoke
  Scenario: not tagged setup
    Given a step
EOF
if python3 "${LINT}" --map "${MAP}" --features "${TMP}/bad_setup" --out "${TMP}/bad_setup.md" >/dev/null 2>&1; then
    fail "linter exited 0 for a _setup_* file not tagged @setup (should reject)"
fi

echo "VERIFY ${STEP}: PASS - lint accepts the closed vocab (invalid: 0), rejects unknown @cap/@feat and _setup_-without-@setup (non-zero exit)"
