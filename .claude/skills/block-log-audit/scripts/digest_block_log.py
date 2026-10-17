#!/usr/bin/env python3
"""Normalised ERROR/WARN inventory of one block's logs.

usage: digest_block_log.py <label>.carbon.log [<label>.test.log] [--all]

Prints, for the server log and the test log separately, every distinct normalised ERROR/WARN message with its count
and the first/last timestamps. Server messages logged before startup completed are summarised separately as startup
noise (use --all to list them too): everything before the first "WSO2 Carbon started in" line, plus the catalogued
startup/restart signatures (OSGi waits, JMS listeners not yet polling, access-log.properties, GA config, Digester,
DefaultSchemaGenerator, tenant sequence copy) wherever they appear, since restart blocks repeat startup mid-run.
Stack-trace continuation lines are not counted as messages.

Normalisation replaces UUIDs, the suite's ${UNIQUE:...} suffixes (_<hex8>_<n>), long hex ids, IP addresses, numbers
and quoted values, so the same message from different scenarios groups together. Always read the original lines
(grep the normalised text's stable words) before classifying a group.
"""
import re
import sys
from collections import OrderedDict

SERVER = re.compile(r'^\[(\d{4}-\d\d-\d\d \d\d:\d\d:\d\d,\d{3})\]\s+(?:\[[^\]]*\]\s+)?(ERROR|WARN)\s+-\s+(.*)$')
TEST = re.compile(r'^(\d\d:\d\d:\d\d\.\d{3}) \[[^\]]+\] (ERROR|WARN)\s+(\S+)\s+\[[^\]]*\] - (.*)$')
STARTED = re.compile(r'WSO2 Carbon started in')
STARTUP_NOISE = re.compile(r'Waiting for required OSGi services|JMSListener Polling tasks on destination .* have not yet '
                           r'started|access-log\.properties|Unable to load Google Analytics configuration|Digester|'
                           r'DefaultSchemaGenerator|TenantServiceCreator Error while copying')

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
    groups, startup = OrderedDict(), OrderedDict()
    started = not server
    with open(path, encoding='utf-8', errors='replace') as f:
        for line in f:
            if server and not started and STARTED.search(line):
                started = True
            m = pattern.match(line.rstrip('\n'))
            if not m:
                continue
            if server:
                ts, level, msg = m.group(1), m.group(2), m.group(3)
            else:
                ts, level, msg = m.group(1), m.group(2), m.group(3) + ' ' + m.group(4)
            key = (level, normalise(msg))
            target = groups if started and not (server and STARTUP_NOISE.search(msg)) else startup
            entry = target.setdefault(key, [0, ts, ts])
            entry[0] += 1
            entry[2] = ts
    return groups, startup


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
    groups, startup = inventory(paths[0], SERVER, server=True)
    table(f'Server ERROR/WARN after startup ({paths[0]})', groups)
    if show_all:
        table('Server ERROR/WARN during startup', startup)
    else:
        print(f'\nStartup noise: {len(startup)} distinct, {sum(v[0] for v in startup.values())} lines (--all lists them)')
    if len(paths) > 1:
        test_groups, _ = inventory(paths[1], TEST, server=False)
        table(f'Test-side ERROR/WARN ({paths[1]})', test_groups)


if __name__ == '__main__':
    main(sys.argv[1:])
