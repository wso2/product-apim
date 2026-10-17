// Observe the Docker host's real clock. This process never adjusts time.
'use strict';
const fs = require('fs');
const bootId = fs.readFileSync('/proc/sys/kernel/random/boot_id', 'utf8').trim();
function sample() {
    const monotonicNanos = process.hrtime.bigint().toString();
    const wallTimeMillis = Date.now();
    const monotonicAfterNanos = process.hrtime.bigint().toString();
    process.stdout.write(JSON.stringify({type: 'clock-sample', bootId, wallTimeMillis, monotonicNanos,
        monotonicAfterNanos}) + '\n');
}
sample();
setInterval(sample, 200);
