#!/usr/bin/env bash
#
# Creates a cgroup-v2 group that caps the Playwright browser on Linux hosts (CI): 1 CPU, 2.5 GiB by default, no swap.
# Run once per job as root, before the suite. PlaywrightSsoClient launches Chromium through
# chromium-cgroup-wrapper.sh whenever this group exists, so every browser process shares this one budget.
#
# Usage: sudo setup-browser-cgroup.sh [cgroup-path] [memory-limit-bytes]
# The default 2.5 GiB limit is used for all-in-one; distributed CI passes 512 MiB explicitly.
set -euo pipefail

CGROUP="${1:-/sys/fs/cgroup/pw-browser}"
CGROUP_ROOT="$(dirname "$CGROUP")"
CPU_MAX="100000 100000"        # 100 ms of CPU per 100 ms period = 1 CPU
MEMORY_MAX="${2:-2684354560}"  # default 2.5 GiB
MEMORY_SWAP_MAX="0"

if [ ! -f "$CGROUP_ROOT/cgroup.controllers" ]; then
    echo "setup-browser-cgroup: $CGROUP_ROOT is not a cgroup v2 hierarchy" >&2
    exit 1
fi
for controller in cpu memory; do
    if ! grep -qw "$controller" "$CGROUP_ROOT/cgroup.subtree_control"; then
        echo "+$controller" > "$CGROUP_ROOT/cgroup.subtree_control"
    fi
done

mkdir -p "$CGROUP"
echo "$CPU_MAX" > "$CGROUP/cpu.max"
echo "$MEMORY_MAX" > "$CGROUP/memory.max"
if [ -f "$CGROUP/memory.swap.max" ]; then
    echo "$MEMORY_SWAP_MAX" > "$CGROUP/memory.swap.max"
fi

echo "setup-browser-cgroup: $CGROUP cpu.max=[$(cat "$CGROUP/cpu.max")] memory.max=[$(cat "$CGROUP/memory.max")]" \
     "memory.swap.max=[$(cat "$CGROUP/memory.swap.max" 2>/dev/null || echo n/a)]"
