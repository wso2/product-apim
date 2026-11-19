#!/usr/bin/env python3
"""Normalised ERROR/WARN inventory of one block's logs.

usage: digest_block_log.py <server log> [<label>.test.log] [--all]

<server log> is <label>.carbon.log, or for a distributed block one container log (<label>.apim-cp.log,
<label>.apim-gw.log, <label>.apim-tm.log, <label>.mysql.log), or a block's external Identity Server log
(<label>.is.log, logger in braces: its short class name prefixes the message): run it once per log. Test lines may keep the CI runner's ISO timestamp
prefix.

Prints, for the server log and the test log separately, every distinct normalised ERROR/WARN message with its count
and the first/last timestamps. Server WARNs logged before startup completed are summarised separately as startup
noise (use --all to list them too): every WARN before the first "WSO2 Carbon started in" line (MySQL: "ready for
connections"), plus the catalogued
startup/restart WARN signatures (OSGi waits, JMS listeners not yet polling, access-log.properties, GA config, Digester
maxDays, DefaultSchemaGenerator overloading, tenant sequence copy) wherever they appear, since restart blocks repeat
startup mid-run and tenants load lazily. Each signature matches its full catalogued message; an ERROR (before startup
too), or any other text from those loggers, is counted as a finding.
Stack-trace continuation lines are not counted as messages. MySQL error-log lines are counted by their own bracketed
severity ([Warning]/[ERROR]; [System]/[Note] are informational). A server log with no recognised line is reported as
unparsed rather than as zero findings, and one without a startup line (a server that never finished starting, or a
log cut before it) lists every WARN as unverified rather than summarising it as startup noise.

Normalisation replaces UUIDs, the suite's ${UNIQUE:...} suffixes (_<hex8>_<n>), long hex ids, IP addresses, numbers
and quoted values, so the same message from different scenarios groups together. Always read the original lines
(grep the normalised text's stable words) before classifying a group.
"""
import re
import sys
from collections import OrderedDict

# APIM writes "ERROR - Logger message"; Identity Server writes "ERROR {fully.qualified.Logger} - message".
SERVER = re.compile(r'^\[(\d{4}-\d\d-\d\d \d\d:\d\d:\d\d,\d{3})\]\s+(?:\[[^\]]*\]\s+)?(ERROR|WARN)\s+'
                    r'(?:\{([^}]*)\}\s+)?-\s+(.*)$')
MYSQL = re.compile(r'^(\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d\.\d+Z)\s+\d+\s+\[(System|Note|Warning|ERROR)\]\s+(.*)$')
MYSQL_LEVELS = {'Warning': 'WARN', 'ERROR': 'ERROR'}
SERVER_LINE = re.compile(r'^\[\d{4}-\d\d-\d\d \d\d:\d\d:\d\d,\d{3}\]\s')
TEST = re.compile(r'^(?:\S+Z )?(\d\d:\d\d:\d\d\.\d{3}) \[[^\]]+\] (ERROR|WARN)\s+(\S+)\s+\[[^\]]*\] - (.*)$')
STARTED = re.compile(r'WSO2 Carbon started in|ready for connections')
# The catalogued startup/restart WARN messages (references/benign-catalogue.md), matched from the logger name onward.
STARTUP_NOISE = re.compile(
    r'\w+ServiceComponent Waiting for required OSGi services: '
    r'|JMSListener Polling tasks on destination : \w+ of type topic for listener Siddhi-JMS-Consumer#\w+ have not yet '
    r'started after \d+ seconds'
    r'|MiscellaneousUtil Error loading properties from a file at from the System defined location: access-log\.properties$'
    r'|AccessConfiguration Error loading properties from file: access-log\.properties$'
    r'|APIMgtGoogleAnalyticsTrackingHandler Unable to load Google Analytics configuration using key: ga-config-key$'
    r'|Digester Match \[Server/Service/Engine/Host/Valve\] failed to set property \[maxDays\] to \[\]$'
    r"|DefaultSchemaGenerator We don't support method overloading\. Ignoring \[\w+\]$"
    r'|TenantServiceCreator Error while copying API manager specific synapse sequences')

SUBS = [
    (re.compile(r'[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}'), '<UUID>'),
    (re.compile(r'_[0-9a-f]{8}_\d+'), '_<U>'),
    (re.compile(r'\b[0-9a-f]{16,}\b'), '<HEX>'),
    (re.compile(r'\b\d{1,3}(?:\.\d{1,3}){3}(?::\d+)?\b'), '<IP>'),
    (re.compile(r"'[^']{1,80}'"), "'<V>'"),
    (re.compile(r'\b\d+\b'), '<N>'),
]


def normalise(message):
    message = message.strip()
    for pattern, replacement in SUBS:
        message = pattern.sub(replacement, message)
    return message[:240]


def inventory(path, pattern, server):
    """Returns (groups, startup, recognised, started): recognised counts lines in a known log format, of any
    severity; started is whether the startup line was found (always True for a test log)."""
    groups, startup = OrderedDict(), OrderedDict()
    started = not server
    recognised = 0
    with open(path, encoding='utf-8', errors='replace') as f:
        for line in f:
            if server and not started and STARTED.search(line):
                started = True
            line = line.rstrip('\n')
            m = pattern.match(line)
            if server and not m:
                mysql = MYSQL.match(line)
                if mysql:
                    recognised += 1
                    if mysql.group(2) not in MYSQL_LEVELS:
                        continue
                    ts, level, msg = mysql.group(1), MYSQL_LEVELS[mysql.group(2)], mysql.group(3)
                elif SERVER_LINE.match(line):
                    recognised += 1
                    continue
                else:
                    continue
            elif not m:
                continue
            elif server:
                recognised += 1
                ts, level, msg = m.group(1), m.group(2), m.group(4)
                if m.group(3):
                    msg = m.group(3).rsplit('.', 1)[-1] + ' ' + msg
            else:
                recognised += 1
                ts, level, msg = m.group(1), m.group(2), m.group(3) + ' ' + m.group(4)
            key = (level, normalise(msg))
            noise = server and level == 'WARN' and STARTUP_NOISE.match(msg.strip())
            target = groups if (started or level == 'ERROR') and not noise else startup
            entry = target.setdefault(key, [0, ts, ts])
            entry[0] += 1
            entry[2] = ts
    return groups, startup, recognised, started


def table(title, groups):
    print(f'\n### {title}: {len(groups)} distinct, {sum(v[0] for v in groups.values())} lines')
    if not groups:
        return
    print('| Count | Level | First | Last | Normalised message |')
    print('|---:|---|---|---|---|')
    for (level, msg), (count, first, last) in sorted(groups.items(), key=lambda kv: -kv[1][0]):
        print(f'| {count} | {level} | {first} | {last} | `{msg.replace("|", "/")}` |')


def main(argv):
    show_all = '--all' in argv
    paths = [a for a in argv if a != '--all']
    if not paths:
        sys.exit(__doc__)
    groups, startup, recognised, started = inventory(paths[0], SERVER, server=True)
    if not recognised:
        print(f'\n### Server log unparsed ({paths[0]}): no Carbon or MySQL log line recognised; inventory it by hand')
    table(f'Server ERROR (any time) and WARN after startup ({paths[0]})', groups)
    if recognised and not started:
        table('Startup boundary not found (no "WSO2 Carbon started in" or "ready for connections" line): '
              'server WARN unverified, classify each', startup)
    elif show_all:
        table('Server WARN during startup', startup)
    else:
        print(f'\nStartup noise: {len(startup)} distinct, {sum(v[0] for v in startup.values())} lines (--all lists them)')
    if len(paths) > 1:
        test_groups, _, _, _ = inventory(paths[1], TEST, server=False)
        table(f'Test-side ERROR/WARN ({paths[1]})', test_groups)


if __name__ == '__main__':
    main(sys.argv[1:])
