#!/usr/bin/env bash
# Allows sudo to preserve Playwright's socketpair descriptors 3/4 when unshare starts a private browser network
# namespace. The close-from override is scoped to unshare; the browser wrapper drops back to its caller's uid.
# Usage: sudo bash setup-browser-network.sh
set -euo pipefail

for tool in socat ip unshare setpriv visudo; do
    if ! command -v "$tool" > /dev/null; then
        echo "setup-browser-network: $tool is required (install socat, iproute2, util-linux and sudo)" >&2
        exit 1
    fi
done
config=/etc/sudoers.d/pw-browser-network
printf 'Defaults!%s closefrom_override\n' "$(command -v unshare)" > "$config"
chmod 0440 "$config"
visudo -cf "$config"
# World-readable marker the test JVM checks before launching the browser through the namespace wrapper
# (/etc/sudoers.d itself is not readable by the runner user).
echo "browser network namespace configured by setup-browser-network.sh" > /etc/pw-browser-network
chmod 0644 /etc/pw-browser-network
