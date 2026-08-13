/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

// Upstream for gateway SSE-API invocation tests: a test publishes a WSO2 SSE-type API pointing here, invokes it
// through the gateway and asserts "events sent == events received" (the legacy ServerSentEventsAPITestCase
// assertion). The legacy Jetty EventSourceServlet emitted an UNBOUNDED stream and the test counted what the
// servlet happened to push before the receiver was torn down — inherently racy. Here the count is a CALLER-SUPPLIED
// query param, so "sent" is known up front and the assertion needs no cross-process counter.
//
// The default count is deterministic too (DEFAULT_COUNT), so a test can still assert an exact number even if the
// gateway does not forward the query string to the upstream.
//
// Every path except the reserved introspection paths below serves the stream, so the API's topic path (whatever
// the AsyncAPI definition declares) reaches it without a backend change.
//   RESERVED (not usable as a topic path): /health, /streams
const express = require('express');

const app = express();
const port = process.env.PORT || 3021;

// Small enough that a stream of a few dozen events finishes in well under a second, large enough that the
// events arrive as separate chunks rather than one flush (which is what makes this an SSE test and not a
// plain-body test).
const DEFAULT_DELAY_MS = 50;
const DEFAULT_COUNT = 10;
const DEFAULT_EVENT_NAME = 'message';

// Diagnostic record of every stream served, newest last. Reset via POST /streams/reset. This is a debugging aid
// and NOT a parallel-safe assertion source unless the caller scopes it with ?tag=<unique> on both the invocation
// and the introspection read.
// Bounded: NodeAppServer is a singleton that is never stopped, so this records every stream served for the whole
// suite run. Each record is small fixed metadata (no event bodies), so the exposure is modest — but unbounded is
// unbounded, and a future scenario that loops over streams would grow it without limit. Keeps the most recent
// MAX_RETAINED_STREAMS; eviction drops the OLDEST, and the diagnostic reader matches by TAG on the stream it just
// opened, so the cap stays well above what one scenario can open before asserting (the quota arc retries ~12x).
const MAX_RETAINED_STREAMS = 200;
const streams = [];

function retainStream(stream) {
    streams.push(stream);
    while (streams.length > MAX_RETAINED_STREAMS) {
        streams.shift();
    }
}

function intParam(raw, fallback, min, max) {
    const parsed = Number.parseInt(raw, 10);
    if (Number.isNaN(parsed) || parsed < min || parsed > max) {
        return fallback;
    }
    return parsed;
}

app.get('/health', (req, res) => res.json({ status: 'ok' }));

app.get('/streams', (req, res) => {
    const matches = streams.filter((s) => (!req.query.tag || s.tag === req.query.tag)
        && (!req.query.path || s.path === req.query.path));
    res.json({ count: matches.length, streams: matches });
});

function resetStreams(req, res) {
    streams.length = 0;
    res.json({ count: 0, streams: [] });
}

app.post('/streams/reset', resetStreams);
app.delete('/streams', resetStreams);

// text/event-stream, one well-formed frame (id/event/data + blank-line terminator) per event, then a clean end.
// Only ONE `data:` line per frame, so a receiver that counts lines starting with `data:` counts exactly `count`.
app.get('/*', (req, res) => {
    const count = intParam(req.query.count, DEFAULT_COUNT, 0, 100000);
    const delayMs = intParam(req.query.delayMs, DEFAULT_DELAY_MS, 0, 60000);
    const eventName = req.query.event ? String(req.query.event) : DEFAULT_EVENT_NAME;

    const stream = {
        path: req.path,
        tag: req.query.tag ? String(req.query.tag) : null,
        requested: count,
        sent: 0,
        completed: false,
        startedAt: new Date().toISOString()
    };
    retainStream(stream);
    console.log(`----SSE stream opened path=${req.path} count=${count} delayMs=${delayMs} tag=${stream.tag}`);

    res.status(200).set({
        'Content-Type': 'text/event-stream; charset=utf-8',
        // no-transform stops any intermediary from buffering/recompressing the stream into a single response.
        'Cache-Control': 'no-cache, no-transform',
        Connection: 'keep-alive'
    });
    res.flushHeaders();

    let timer = null;
    let clientGone = false;
    // The gateway closes the connection when the client disconnects (or on throttle-out). Stop emitting then, so a
    // half-finished stream is recorded as completed=false with its real `sent` count instead of leaking a timer.
    req.on('close', () => {
        clientGone = true;
        if (timer) {
            clearTimeout(timer);
        }
        console.log(`----SSE stream closed path=${req.path} sent=${stream.sent}/${count}`);
    });

    const emit = () => {
        if (clientGone) {
            return;
        }
        const seq = stream.sent + 1;
        res.write(`id: ${seq}\nevent: ${eventName}\ndata: {"seq":${seq},"message":"sse event ${seq}"}\n\n`);
        stream.sent = seq;
        if (seq >= count) {
            stream.completed = true;
            res.end();
            return;
        }
        timer = setTimeout(emit, delayMs);
    };

    if (count === 0) {
        stream.completed = true;
        res.end();
    } else {
        emit();
    }
});

app.listen(port, () => {
    console.log(`SSE emitter backend running at http://nodebackend:${port}/ (?count=N&delayMs=M&event=NAME&tag=T)`);
});
