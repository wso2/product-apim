#!/usr/bin/env bash
#
# Phase 4.14 verification — BlockLifecycleListener parameter handling (defaults + overlay).
#
# testng-fv-4.14.xml runs two REAL APIM blocks serially, both labelled block=fv-4.14:
#   - Phase4.14-Defaults sets NO tomlOverlayPath: the listener must fall back to the base deployment.toml
#     and still boot ready (no NPE on the absent overlay). Its probe cat's the in-container toml and
#     asserts the overlay marker is ABSENT (it is the unmodified base toml).
#   - Phase4.14-Overlay sets tomlOverlayPath to the toml this script generates (base toml + a distinctive
#     marker comment). Its probe cat's the in-container toml and asserts the marker is PRESENT - proving
#     the parameter actually reached the running container, not merely that the file exists on disk.
#
# This script generates the overlay toml from the base toml, then asserts after the run: Maven build
# SUCCEEDS (both probes' in-container assertions held); exactly 2 observations across 2 distinct real
# container ids (both blocks booted their own container); and no fv-4.14 container leaked. Re-runnable /
# idempotent. Prints a single PASS/FAIL line, non-zero on fail.
#
# Usage:  ./verify-4.14.sh
set -euo pipefail

STEP="4.14"
BLOCK_LABEL="fv-4.14"
LABEL_FILTER="label=block=${BLOCK_LABEL}"
MARKER="FV-4.14-OVERLAY-MARKER"
EXPECTED_BLOCKS=2
EXPECTED_OBS=2

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# .../cucumber-tests/src/test/scripts/verification -> module root is 4 levels up
MODULE_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
REACTOR_DIR="$(cd "${MODULE_DIR}/../.." && pwd)"   # integration-v2 root
# The full-file tomlOverlayPath replaces the whole config verbatim, so the base must be a COMPLETE bootable
# toml. Use the product distribution deployment.toml (the image's built-in config) — NOT basic/deployment.toml,
# which is now an overlay merged onto the distribution and is not bootable on its own.
BASE_TOML="${MODULE_DIR}/../../../distribution/product/src/main/conf/deployment.toml"
OVERLAY_TOML="${MODULE_DIR}/target/fv-4.14-deployment.toml"
OBS_FILE="${MODULE_DIR}/target/fv-block-observations.txt"
MVN_LOG="${MODULE_DIR}/target/verify-4.14-maven.log"

fail() { echo "VERIFY ${STEP}: FAIL - $1"; exit 1; }

cleanup_containers() {
    local ids
    ids="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
    if [ -n "${ids}" ]; then
        echo "Cleaning up verify-${STEP} containers: ${ids}"
        docker rm -f ${ids} >/dev/null 2>&1 || true
    fi
}

# Always clean up on exit so a crashed run never poisons the next.
trap cleanup_containers EXIT

echo "== Phase ${STEP} verification: listener parameter handling (defaults + overlay) =="
rm -f "${OBS_FILE}" "${MVN_LOG}" "${OVERLAY_TOML}"
cleanup_containers

# Generate the overlay toml = full base toml + a distinctive marker comment. The toml is a FULL
# replacement (DB config is supplied via env vars), so the overlay must itself be a complete bootable toml.
[ -f "${BASE_TOML}" ] || fail "base toml not found: ${BASE_TOML}"
mkdir -p "$(dirname "${OVERLAY_TOML}")"
cp "${BASE_TOML}" "${OVERLAY_TOML}"
printf '\n# %s\n' "${MARKER}" >> "${OVERLAY_TOML}"
# Sanity: the base toml must NOT already carry the marker, or the negative assertion would be vacuous.
if grep -q "${MARKER}" "${BASE_TOML}"; then
    fail "base toml unexpectedly already contains the marker '${MARKER}' - choose a more distinctive marker"
fi

echo "Running verification suite via Maven..."
if ! ( cd "${REACTOR_DIR}" && mvn -q -pl tests-integration/cucumber-tests -am \
        -Dsurefire.suite.xml=testng-fv-4.14.xml test ) > "${MVN_LOG}" 2>&1; then
    tail -25 "${MVN_LOG}"
    fail "Maven build failed - a parameter-handling probe assertion failed (see ${MVN_LOG})"
fi

# Assertion 1: both blocks ran against their own real container.
[ -f "${OBS_FILE}" ] || fail "expected observation file not produced: ${OBS_FILE}"
OBS_COUNT="$(grep -c . "${OBS_FILE}" || true)"
[ "${OBS_COUNT}" = "${EXPECTED_OBS}" ] || fail "expected ${EXPECTED_OBS} observations, got ${OBS_COUNT}"
DISTINCT_IDS="$(awk -F'|' '{print $3}' "${OBS_FILE}" | sort -u)"
ID_COUNT="$(printf '%s\n' "${DISTINCT_IDS}" | grep -c . || true)"
[ "${ID_COUNT}" = "${EXPECTED_BLOCKS}" ] \
    || fail "expected ${EXPECTED_BLOCKS} distinct container ids (one per block), got ${ID_COUNT}: ${DISTINCT_IDS}"
if printf '%s\n' "${DISTINCT_IDS}" | grep -Eq '^(none|null)$'; then
    fail "a probe recorded a missing container id: ${DISTINCT_IDS}"
fi

# Assertion 2: no fv-4.14 container leaked.
LEFTOVER="$(docker ps -aq --filter "${LABEL_FILTER}" 2>/dev/null || true)"
[ -z "${LEFTOVER}" ] || fail "fv-4.14 containers leaked after run: ${LEFTOVER}"

echo "VERIFY ${STEP}: PASS - defaults block booted on base toml (marker absent), overlay block shipped the marker into its container, ${EXPECTED_BLOCKS} containers, no leaks"
