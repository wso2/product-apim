#!/usr/bin/env bash
#
# Playwright's Linux Chromium executable. The browser has a private network namespace whose stable interfaces
# cannot receive Docker's host address changes. A loopback TCP relay reaches the host CONNECT proxy through a
# filesystem Unix socket. The browser runs as its caller; its helper processes share the optional cgroup cap.
#
# sudo preserves Playwright's socketpair descriptors 3/4 with -C 5 (see setup-browser-network.sh).
# Environment: PW_REAL_BROWSER, PW_BROWSER_PROXY_SOCKET, PW_BROWSER_PROXY_PORT, PW_BROWSER_HOME,
# and optional PW_BROWSER_CGROUP.
set -euo pipefail

: "${PW_REAL_BROWSER:?PW_REAL_BROWSER is not set}"
: "${PW_BROWSER_PROXY_SOCKET:?PW_BROWSER_PROXY_SOCKET is not set}"
: "${PW_BROWSER_PROXY_PORT:?PW_BROWSER_PROXY_PORT is not set}"

if [[ "${1:-}" != --network-child ]]; then
    if [[ -n "${PW_BROWSER_CGROUP:-}" ]]; then
        if ! echo "$$" | sudo -n tee "$PW_BROWSER_CGROUP/cgroup.procs" > /dev/null; then
            echo "chromium-cgroup-wrapper: could not move pid $$ into $PW_BROWSER_CGROUP" >&2
            exit 1
        fi
    fi
    sudo -n -E -C 5 unshare --net -- setpriv --pdeathsig TERM -- bash "$0" \
        --network-child "$(id -u)" "$(id -g)" "$@" &
    supervisor_pid=$!
    trap 'kill "$supervisor_pid" 2>/dev/null || true' TERM INT
    wait "$supervisor_pid"
    exit $?
fi

browser_uid="$2"
browser_gid="$3"
shift 3
ip link set lo up
# A stable interface keeps navigator.onLine true. Address generation is disabled before link-up so IPv6 DAD
# cannot introduce a delayed address change after Chromium starts. This address exists only in this namespace.
ip link add pw-link type dummy
ip link set pw-link addrgenmode none
ip addr add 192.0.2.1/32 dev pw-link
ip link set pw-link up

coproc PW_RELAY {
    exec setpriv --pdeathsig TERM -- socat -d -d \
        "TCP4-LISTEN:$PW_BROWSER_PROXY_PORT,bind=127.0.0.1,reuseaddr,fork" \
        "UNIX-CONNECT:$PW_BROWSER_PROXY_SOCKET" 3<&- 4>&- 2>&1
}
relay_pid="$PW_RELAY_PID"
exec 5<&"${PW_RELAY[0]}"
ready=false
while IFS= read -r line <&5; do
    if [[ "$line" == *"listening on"* ]]; then ready=true; break; fi
    echo "$line" >&2
done
if [[ "$ready" != true ]]; then echo 'browser proxy relay failed to listen' >&2; exit 1; fi
cat <&5 > /dev/null 3<&- 4>&- &
drain_pid=$!
browser_pid=''
trap '[[ -z "$browser_pid" ]] || kill "$browser_pid" 2>/dev/null || true; kill "$relay_pid" "$drain_pid" 2>/dev/null || true' EXIT
trap 'exit 143' TERM INT
if [[ -n "${PW_BROWSER_HOME:-}" ]]; then export HOME="$PW_BROWSER_HOME"; fi
setpriv --reuid "$browser_uid" --regid "$browser_gid" --init-groups --pdeathsig TERM -- \
    "$PW_REAL_BROWSER" "$@" &
browser_pid=$!
wait "$browser_pid"
