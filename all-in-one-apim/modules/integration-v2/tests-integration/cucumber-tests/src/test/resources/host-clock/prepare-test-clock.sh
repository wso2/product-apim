#!/usr/bin/env bash
# Prepare an Ubuntu Docker host before any product container starts.
# Usage: sudo bash prepare-test-clock.sh prepare|restore <diagnostic-directory>
# Ubuntu dependencies: chrony, python3, util-linux. Diagnostic output is archived with the suite logs.
set -euo pipefail
export LC_ALL=C

ACTION="${1:-prepare}"
OUTPUT="${2:?Usage: prepare-test-clock.sh prepare|restore <diagnostic-directory>}"
CHRONY_CONFIG=/etc/chrony/chrony.conf
LIMA_OVERRIDE=/run/systemd/system/lima-guestagent.service.d/apim-test-clock.conf

if [[ "$EUID" != 0 || "$(uname -s)" != Linux ]]; then
    echo 'prepare-test-clock: run as root on the Ubuntu Docker host' >&2
    exit 1
fi
if ! . /etc/os-release || [[ "${ID:-}" != ubuntu ]]; then
    echo 'prepare-test-clock: this preparation script supports Ubuntu hosts' >&2
    exit 1
fi
mkdir -p "$OUTPUT"
OUTPUT="$(realpath "$OUTPUT")"
exec 9>/run/lock/apim-test-clock.lock
flock -n 9 || { echo 'prepare-test-clock: another clock preparation is running' >&2; exit 1; }

snapshot() {
    local phase="$1"
    {
        date --iso-8601=ns
        cat /etc/os-release
        timedatectl status || true
        for service in chrony systemd-timesyncd ntp ntpsec openntpd lima-guestagent; do
            systemctl show "$service.service" -p ActiveState -p MainPID -p ExecStart || true
        done
        chronyc tracking || true
        chronyc sources -v || true
        chronyd -p -f "$CHRONY_CONFIG" || true
    } > "$OUTPUT/$phase.txt" 2>&1
    chmod a+r "$OUTPUT/$phase.txt"
}

run_clock_command() {
    local diagnostic="$1"
    shift
    local status=0
    if "$@" > "$OUTPUT/$diagnostic" 2>&1; then
        return 0
    else
        status=$?
    fi
    echo "prepare-test-clock: $diagnostic failed (exit $status): $*" >&2
    cat "$OUTPUT/$diagnostic" >&2
    return "$status"
}

if [[ "$ACTION" == restore ]]; then
    [[ -f "$OUTPUT/state.json" ]] || { echo 'prepare-test-clock: no saved state to restore' >&2; exit 1; }
    cp -p "$OUTPUT/chrony.conf.before" "$CHRONY_CONFIG"
    if [[ -f "$OUTPUT/lima-override.before" ]]; then
        cp -p "$OUTPUT/lima-override.before" "$LIMA_OVERRIDE"
    elif [[ -f "$OUTPUT/lima-override-created" ]]; then
        rm -f "$LIMA_OVERRIDE"
    fi
    systemctl daemon-reload
    if [[ -f "$OUTPUT/lima-override-created" ]]; then
        systemctl restart lima-guestagent.service
    fi
    if python3 -c 'import json,sys;sys.exit(not json.load(open(sys.argv[1]))["chronyWasActive"])' "$OUTPUT/state.json"; then
        systemctl restart chrony.service
    else
        systemctl stop chrony.service
    fi
    while IFS= read -r service; do
        systemctl start "$service.service"
    done < "$OUTPUT/competing-services.before"
    rm -f "$OUTPUT/prepared.json"
    snapshot restored
    echo "prepare-test-clock: restored clock services; diagnostics=$OUTPUT"
    exit 0
fi

[[ "$ACTION" == prepare ]] || { echo "Unknown action: $ACTION" >&2; exit 1; }
[[ ! -f "$OUTPUT/state.json" ]] || { echo 'prepare-test-clock: use a fresh diagnostic directory' >&2; exit 1; }
for command in chronyc chronyd python3 systemctl docker; do
    command -v "$command" >/dev/null || { echo "prepare-test-clock: missing dependency: $command" >&2; exit 1; }
done
[[ -f "$CHRONY_CONFIG" ]] || { echo "prepare-test-clock: missing $CHRONY_CONFIG" >&2; exit 1; }
if [[ -n "$(docker ps -q)" ]]; then
    echo 'prepare-test-clock: prepare time before starting containers on this host' >&2
    exit 1
fi

snapshot before
cp -p "$CHRONY_CONFIG" "$OUTPUT/chrony.conf.before"
chrony_active=false
systemctl is-active --quiet chrony.service && chrony_active=true
python3 - "$OUTPUT/state.json" "$chrony_active" <<'PY'
import json,sys
json.dump({'chronyWasActive':sys.argv[2]=='true'},open(sys.argv[1],'w'),indent=2)
PY
: > "$OUTPUT/competing-services.before"
trap 'snapshot failed; echo "prepare-test-clock: failed; inspect $OUTPUT and restore using this script" >&2' ERR
for service in systemd-timesyncd ntp ntpsec openntpd; do
    if systemctl is-active --quiet "$service.service"; then
        echo "$service" >> "$OUTPUT/competing-services.before"
        systemctl stop "$service.service"
    fi
done

# Lima's SyncTime RPC calls settimeofday independently of NTP. Keep its other functions, but prevent it
# from setting the clock. This runtime-only override is relevant to local Ubuntu VMs, not GitHub runners.
if systemctl is-active --quiet lima-guestagent.service; then
    mkdir -p "$(dirname "$LIMA_OVERRIDE")"
    if [[ -f "$LIMA_OVERRIDE" ]]; then
        cp -p "$LIMA_OVERRIDE" "$OUTPUT/lima-override.before"
    fi
    printf '[Service]\nCapabilityBoundingSet=~CAP_SYS_TIME\n' > "$LIMA_OVERRIDE"
    touch "$OUTPUT/lima-override-created"
    systemctl daemon-reload
    systemctl restart lima-guestagent.service
    lima_pid="$(systemctl show lima-guestagent.service -p MainPID --value)"
    python3 - "$lima_pid" <<'PY'
import sys
status=dict(line.split(':',1) for line in open('/proc/'+sys.argv[1]+'/status') if ':' in line)
if int(status['CapEff'].strip(),16) & (1<<25):
    raise SystemExit('lima-guestagent still has CAP_SYS_TIME')
PY
fi

systemctl start chrony.service
# Clock steps are permitted only here, before APIM exists. Bound every wait so missing time sources cannot hang CI.
run_clock_command initial-step-policy.txt timeout 15 chronyc makestep 0.1 1
run_clock_command initial-sources.txt timeout 15 chronyc -n sources
# Match a source mode AND state, followed by whitespace: the table separator also starts with =.
# Burst applies to NTP servers (^) and peers (=), not reference clocks (#). Azure runners can use only PHC0.
if grep -Eq '^[=^][*+?x~-][[:space:]]' "$OUTPUT/initial-sources.txt"; then
    run_clock_command initial-burst.txt timeout 15 chronyc burst 4/4
else
    echo 'Skipped burst: no network NTP sources; Chrony synchronization is still required.' \
        > "$OUTPUT/initial-burst.txt"
    cat "$OUTPUT/initial-burst.txt"
fi
run_clock_command initial-sync.txt timeout 75 chronyc waitsync 60 0.05 0 1

# Last directives override distribution defaults. Persist the policy so a daemon restart during the suite
# also uses slew correction; cap slew rate at 1000 ppm to preserve meaningful wall-clock throttle windows.
cat >> "$CHRONY_CONFIG" <<'CONF'

# APIM integration-v2 runtime clock policy (restored by prepare-test-clock.sh restore).
makestep 0.1 0
maxslewrate 1000
leapsecmode slew
CONF
run_clock_command effective-chrony.conf chronyd -p -f "$CHRONY_CONFIG"
systemctl restart chrony.service
run_clock_command runtime-sync.txt timeout 75 chronyc waitsync 60 0.05 0 1
run_clock_command runtime-step-policy.txt timeout 15 chronyc makestep 0.1 0
snapshot prepared
python3 - "$OUTPUT/prepared.json" <<'PY'
import datetime,json,subprocess,sys
pid=subprocess.check_output(['systemctl','show','chrony.service','-p','MainPID','--value'],text=True).strip()
json.dump({'prepared':True,'mode':'slew','bootId':open('/proc/sys/kernel/random/boot_id').read().strip(),
           'preparedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'chronyPid':int(pid),
           'maxSlewRatePpm':1000,'automaticSteps':False},open(sys.argv[1],'w'),indent=2)
PY
chmod a+r "$OUTPUT"/*
trap - ERR
echo "prepare-test-clock: synchronized, slew-only; marker=$OUTPUT/prepared.json"
