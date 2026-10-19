#!/usr/bin/env bash
set -euo pipefail
umask 077

ACTION="${1:?Usage: setup-distributed-memory-budget.sh setup|cleanup state-directory}"
STATE_DIR="${2:?Usage: setup-distributed-memory-budget.sh setup|cleanup state-directory}"
# A dash in a slice name creates an implicit parent slice and changes its cgroup path.
SLICE_NAME="apimdistributed.slice"
SLICE_FILE="/etc/systemd/system/$SLICE_NAME"
CGROUP_PATH="/sys/fs/cgroup/$SLICE_NAME"
DOCKER_CONFIG="/etc/docker/daemon.json"
SWAP_FILE="/swapfile-apim-distributed"
MEMORY_HIGH_BYTES=$((14 * 1024 * 1024 * 1024))
MEMORY_MAX_BYTES=$((15 * 1024 * 1024 * 1024))
SWAP_BUDGET_BYTES=$((5 * 1024 * 1024 * 1024))

backup_file() {
    local target="$1" backup="$2"
    if [ -e "$target" ]; then
        cp -p "$target" "$backup"
        touch "$backup.present"
    else
        touch "$backup.absent"
    fi
}

restore_file() {
    local target="$1" backup="$2"
    if [ -f "$backup.present" ]; then
        cp -p "$backup" "$target"
    elif [ -f "$backup.absent" ]; then
        rm -f "$target"
    fi
}

cleanup() {
    [ -d "$STATE_DIR" ] || return 0
    local result=0
    # Docker's PID 1 may ignore SIGTERM. Kill this job's whole cgroup before stopping its units
    # so cancellation cleanup does not wait for each scope's 90-second stop timeout.
    if [ -f "$STATE_DIR/slice-modified" ] && [ -f "$CGROUP_PATH/cgroup.kill" ]; then
        if ! echo 1 > "$CGROUP_PATH/cgroup.kill"; then result=1; fi
    fi
    if [ -f "$STATE_DIR/slice-modified" ]; then
        local slice_state
        if slice_state="$(systemctl show "$SLICE_NAME" --property=ActiveState --value)"; then
            case "$slice_state" in
                inactive|failed) ;;
                *) if ! systemctl stop "$SLICE_NAME"; then result=1; fi ;;
            esac
        else
            result=1
        fi
        if ! restore_file "$SLICE_FILE" "$STATE_DIR/slice"; then result=1; fi
        if ! systemctl daemon-reload; then result=1; fi
    fi
    if [ -f "$STATE_DIR/docker-modified" ]; then
        if restore_file "$DOCKER_CONFIG" "$STATE_DIR/daemon.json"; then
            if ! systemctl reset-failed docker || ! systemctl restart docker; then result=1; fi
        else
            result=1
        fi
    fi
    if [ -f "$STATE_DIR/swapfile-created" ] && [ -e "$SWAP_FILE" ]; then
        if ! awk -v path="$SWAP_FILE" '$1 == path {found=1} END {exit !found}' /proc/swaps \
                || swapoff "$SWAP_FILE"; then
            if ! rm -f "$SWAP_FILE"; then result=1; fi
        else
            echo "Could not disable $SWAP_FILE during cleanup; leaving the active swap file in place" >&2
            result=1
        fi
    fi
    if [ "$result" -eq 0 ]; then
        rm -rf "$STATE_DIR"
    else
        echo "Cleanup incomplete; retained backups in $STATE_DIR for retry" >&2
    fi
    return "$result"
}

if [ "$EUID" -ne 0 ]; then
    echo "Run this script as root" >&2
    exit 1
fi
if [ "$ACTION" = cleanup ]; then
    cleanup
    exit $?
fi
if [ "$ACTION" != setup ]; then
    echo "Unknown action: $ACTION" >&2
    exit 2
fi
if [ -e "$STATE_DIR" ]; then
    echo "State directory already exists; refusing to overwrite its backups: $STATE_DIR" >&2
    exit 1
fi
if [ ! -f /sys/fs/cgroup/cgroup.controllers ]; then
    echo "Distributed aggregate memory budget requires cgroup v2" >&2
    exit 1
fi
CGROUP_DRIVER="$(docker info --format '{{.CgroupDriver}}')"
if [ "$CGROUP_DRIVER" != systemd ]; then
    echo "Distributed aggregate memory budget requires Docker's systemd cgroup driver; found '$CGROUP_DRIVER'" >&2
    exit 1
fi
if [ -f "$DOCKER_CONFIG" ]; then
    dockerd --validate --config-file="$DOCKER_CONFIG"
fi

mkdir -p "$STATE_DIR" /etc/docker
rollback_on_failure() {
    local status=$?
    if [ "$status" -ne 0 ]; then
        echo "Distributed memory-budget setup failed; restoring host configuration" >&2
        cleanup || echo "Rollback incomplete; the workflow cleanup step will retry" >&2
    fi
}
trap rollback_on_failure EXIT
backup_file "$DOCKER_CONFIG" "$STATE_DIR/daemon.json"
backup_file "$SLICE_FILE" "$STATE_DIR/slice"

touch "$STATE_DIR/slice-modified"
cat > "$SLICE_FILE" <<EOF
[Unit]
Description=Aggregate resource budget for distributed APIM integration tests

[Slice]
CPUAccounting=yes
MemoryAccounting=yes
MemoryHigh=$MEMORY_HIGH_BYTES
MemoryMax=$MEMORY_MAX_BYTES
MemorySwapMax=$SWAP_BUDGET_BYTES
EOF
chmod 644 "$SLICE_FILE"

touch "$STATE_DIR/docker-modified"
python3 - "$DOCKER_CONFIG" "$SLICE_NAME" <<'PYTHON'
import json
import os
import stat
import sys

path, parent = sys.argv[1:]
configuration = {}
mode = 0o644
if os.path.exists(path):
    mode = stat.S_IMODE(os.stat(path).st_mode)
    with open(path, encoding="utf-8") as stream:
        configuration = json.load(stream)
configuration["cgroup-parent"] = parent
temporary = path + ".apim-tmp"
try:
    with open(temporary, "w", encoding="utf-8") as stream:
        json.dump(configuration, stream, indent=2)
        stream.write("\n")
    os.chmod(temporary, mode)
    os.replace(temporary, path)
finally:
    if os.path.exists(temporary):
        os.unlink(temporary)
PYTHON

dockerd --validate --config-file="$DOCKER_CONFIG"
systemctl daemon-reload
systemctl start "$SLICE_NAME"
systemctl reset-failed docker
systemctl restart docker

if [ "$(docker info --format '{{.CgroupDriver}}')" != systemd ]; then
    echo "Docker did not restart with the systemd cgroup driver" >&2
    exit 1
fi
for control in memory.high memory.max memory.swap.max cgroup.kill; do
    if [ ! -f "$CGROUP_PATH/$control" ]; then
        echo "Aggregate budget cgroup is missing $control" >&2
        exit 1
    fi
done
if [ "$(cat "$CGROUP_PATH/memory.high")" != "$MEMORY_HIGH_BYTES" ] \
        || [ "$(cat "$CGROUP_PATH/memory.max")" != "$MEMORY_MAX_BYTES" ] \
        || [ "$(cat "$CGROUP_PATH/memory.swap.max")" != "$SWAP_BUDGET_BYTES" ]; then
    echo "Systemd did not apply the requested aggregate memory and swap limits" >&2
    exit 1
fi

SWAP_TOTAL_KIB="$(awk '$1 == "SwapTotal:" {print $2}' /proc/meminfo)"
if [ "$((SWAP_TOTAL_KIB * 1024))" -lt "$SWAP_BUDGET_BYTES" ]; then
    if [ -e "$SWAP_FILE" ]; then
        echo "Host swap is below 5 GiB and $SWAP_FILE already exists; cannot provision the requested swap budget" >&2
        exit 1
    fi
    MISSING_SWAP_BYTES=$((SWAP_BUDGET_BYTES - SWAP_TOTAL_KIB * 1024))
    # A swap header consumes one page; reserve it so usable SwapTotal reaches the requested budget.
    PAGE_SIZE="$(getconf PAGESIZE)"
    SWAP_FILE_MIB=$(((MISSING_SWAP_BYTES + PAGE_SIZE + 1024 * 1024 - 1) / (1024 * 1024)))
    REQUIRED_DISK_KIB=$((SWAP_FILE_MIB * 1024 + 256 * 1024))
    AVAILABLE_DISK_KIB="$(df -Pk / | awk 'NR == 2 {print $4}')"
    if [ "$AVAILABLE_DISK_KIB" -lt "$REQUIRED_DISK_KIB" ]; then
        echo "Insufficient disk space to provision ${SWAP_FILE_MIB} MiB of swap while retaining a 256 MiB reserve" >&2
        exit 1
    fi
    touch "$STATE_DIR/swapfile-created"
    dd if=/dev/zero of="$SWAP_FILE" bs=1M count="$SWAP_FILE_MIB" status=none
    mkswap "$SWAP_FILE" >/dev/null
    swapon "$SWAP_FILE"
fi
SWAP_TOTAL_KIB="$(awk '$1 == "SwapTotal:" {print $2}' /proc/meminfo)"
if [ "$((SWAP_TOTAL_KIB * 1024))" -lt "$SWAP_BUDGET_BYTES" ]; then
    echo "Active host swap is below the requested 5 GiB budget" >&2
    exit 1
fi

echo "Distributed resource budget: cgroup=$CGROUP_PATH memory.high=$(cat "$CGROUP_PATH/memory.high") memory.max=$(cat "$CGROUP_PATH/memory.max") memory.swap.max=$(cat "$CGROUP_PATH/memory.swap.max")"
echo "Host swap: $(awk '/SwapTotal:|SwapFree:/ {printf "%s=%sKiB ", $1, $2}' /proc/meminfo)"
echo "Docker default cgroup parent: $SLICE_NAME"
