#!/usr/bin/env bash
#
# Creates a cgroup-v2 group that caps the Playwright browser on Linux hosts (CI): 1 CPU and 2.5 GiB by default.
# Run once per job as root, before the suite. PlaywrightSsoClient launches Chromium through
# chromium-cgroup-wrapper.sh whenever this group exists, so every browser process shares this one budget.
#
# Usage: sudo setup-browser-cgroup.sh [cgroup-path] [memory-limit-bytes] [swap-limit-bytes]
# The default 2.5 GiB limit and no swap are used for all-in-one; distributed CI passes explicit limits.
set -euo pipefail

CGROUP="${1:-/sys/fs/cgroup/pw-browser}"
CGROUP_ROOT="$(dirname "$CGROUP")"
CPU_MAX="100000 100000"        # 100 ms of CPU per 100 ms period = 1 CPU
MEMORY_MAX="${2:-2684354560}"  # default 2.5 GiB
MEMORY_SWAP_MAX="${3:-0}"      # swap is disabled by default

if [ ! -f "$CGROUP_ROOT/cgroup.controllers" ]; then
    echo "setup-browser-cgroup: $CGROUP_ROOT is not a cgroup v2 hierarchy" >&2
    exit 1
fi
if [ -f "$CGROUP/cgroup.procs" ] && [ -n "$(cat "$CGROUP/cgroup.procs")" ]; then
    echo "setup-browser-cgroup: refusing to reuse non-empty cgroup $CGROUP" >&2
    exit 1
fi
if [[ "$CGROUP" == *.service ]]; then
    # systemd must own the CPU/memory properties of a subgroup beneath a slice; otherwise it can
    # disable controllers for the manually created group when other managed scopes start.
    # The idle process keeps this transient service and its browser cgroup alive until cleanup.
    systemd-run --quiet --collect --service-type=exec --unit="$(basename "$CGROUP")" \
        --slice="$(basename "$CGROUP_ROOT")" --property=CPUQuota=100% --property=CPUQuotaPeriodSec=100ms \
        --property="MemoryMax=$MEMORY_MAX" --property="MemorySwapMax=$MEMORY_SWAP_MAX" \
        --property=TasksMax=infinity --property=RuntimeMaxSec=7h /usr/bin/sleep infinity
    ACTUAL_CGROUP="$(systemctl show "$(basename "$CGROUP")" --property=ControlGroup --value)"
    if [ "/sys/fs/cgroup$ACTUAL_CGROUP" != "$CGROUP" ]; then
        echo "setup-browser-cgroup: systemd created an unexpected cgroup $ACTUAL_CGROUP" >&2
        exit 1
    fi
else
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
    elif [ "$MEMORY_SWAP_MAX" -gt 0 ]; then
        echo "setup-browser-cgroup: cgroup v2 swap controller is unavailable" >&2
        exit 1
    fi
fi

if [ "$(cat "$CGROUP/cpu.max")" != "$CPU_MAX" ] || [ "$(cat "$CGROUP/memory.max")" != "$MEMORY_MAX" ]; then
    echo "setup-browser-cgroup: CPU or memory cap was not applied" >&2
    exit 1
fi
if [ -f "$CGROUP/memory.swap.max" ] && [ "$(cat "$CGROUP/memory.swap.max")" != "$MEMORY_SWAP_MAX" ]; then
    echo "setup-browser-cgroup: swap cap was not applied" >&2
    exit 1
fi

echo "setup-browser-cgroup: $CGROUP cpu.max=[$(cat "$CGROUP/cpu.max")] memory.max=[$(cat "$CGROUP/memory.max")]" \
     "memory.swap.max=[$(cat "$CGROUP/memory.swap.max" 2>/dev/null || echo n/a)]"
