#!/bin/sh
set -e

# Initialise the UFS cache directory structures required by both instances.
# Must run before the daemon starts; -z writes the swap.state files.
squid -f /etc/squid/squid-anon.conf -z --foreground
squid -f /etc/squid/squid-auth.conf -z --foreground

# Start both Squid instances in the background.
squid -N -f /etc/squid/squid-anon.conf &
ANON_PID=$!

squid -N -f /etc/squid/squid-auth.conf &
AUTH_PID=$!

echo "squid-anon  started on port 3128 (PID ${ANON_PID})"
echo "squid-auth  started on port 3129 (PID ${AUTH_PID})"

# Monitor both PIDs. When either process exits, kill the survivor and exit nonzero so the
# container stops immediately. A bare `wait` without operands keeps the container alive even
# after one Squid dies, hiding a failed proxy instance until a test times out.
while true; do
    if ! kill -0 "${ANON_PID}" 2>/dev/null; then
        echo "squid-anon (PID ${ANON_PID}) exited unexpectedly" >&2
        kill "${AUTH_PID}" 2>/dev/null || true
        exit 1
    fi
    if ! kill -0 "${AUTH_PID}" 2>/dev/null; then
        echo "squid-auth (PID ${AUTH_PID}) exited unexpectedly" >&2
        kill "${ANON_PID}" 2>/dev/null || true
        exit 1
    fi
    sleep 1
done
