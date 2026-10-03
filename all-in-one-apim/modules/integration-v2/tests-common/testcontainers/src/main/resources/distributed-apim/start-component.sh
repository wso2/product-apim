#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${WSO2_START_SCRIPT:-}" ]]; then
    echo "WSO2_START_SCRIPT is not configured" >&2
    exit 2
fi

exec "${WSO2_HOME}/bin/${WSO2_START_SCRIPT}" "$@"
