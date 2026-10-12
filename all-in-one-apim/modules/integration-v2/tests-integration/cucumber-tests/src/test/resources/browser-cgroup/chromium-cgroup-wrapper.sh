#!/usr/bin/env bash
#
# Playwright's Chromium executable on Linux hosts that have the browser cgroup (see setup-browser-cgroup.sh).
# Moves this process into the cgroup, then execs the real browser in place, so the browser and every process it
# spawns inherit the cap. exec keeps the inherited file descriptors 3/4 that Playwright's --remote-debugging-pipe
# uses; sudo is only used for the one write into cgroup.procs, never around the browser itself.
#
# Environment (set by PlaywrightSsoClient): PW_BROWSER_CGROUP (cgroup directory), PW_REAL_BROWSER (executable).
set -euo pipefail

: "${PW_BROWSER_CGROUP:?PW_BROWSER_CGROUP is not set}"
: "${PW_REAL_BROWSER:?PW_REAL_BROWSER is not set}"

if ! echo "$$" | sudo -n tee "$PW_BROWSER_CGROUP/cgroup.procs" > /dev/null; then
    echo "chromium-cgroup-wrapper: could not move pid $$ into $PW_BROWSER_CGROUP; refusing to run the browser uncapped" >&2
    exit 1
fi
exec "$PW_REAL_BROWSER" "$@"
