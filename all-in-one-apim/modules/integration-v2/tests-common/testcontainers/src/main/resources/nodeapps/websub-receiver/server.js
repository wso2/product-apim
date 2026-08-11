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

// Subscriber-side callback receiver for gateway WebSub-API tests (legacy CallbackServerServlet /
// CallbackServerServletWithSubVerification / CallbackServerServlet2).
//
// WHY IT LIVES IN THIS IMAGE AND NOT IN THE TEST JVM: APIM's WebSub hub delivers content SERVER-TO-SERVER to the
// hub.callback URL, so the callback has to be reachable from inside the APIM container — i.e. on the shared docker
// network as http://nodebackend:3022/receiver/<name>. For the same reason the ASSERTIONS cannot live here: they
// run in the test JVM, so the recorded deliveries are read back over the HOST-published introspection endpoint.
//
// Receivers are NAMED (a path segment) rather than global. Tests run in parallel against this one shared backend,
// so each scenario must pass its own unique name (utils/Names.unique) as the callback path segment; a global
// counter would make two concurrent WebSub scenarios overwrite each other's delivery count.
//
// TWO CALLBACK FLAVOURS, one per path family — both record identically, they differ ONLY in what the verification
// GET returns:
//   /receiver/:name  echoes hub.challenge (200 text/plain). Required when the API has
//                    enableSubscriberVerification=true, and correct WebSub either way.
//   /silent/:name    records the handshake but answers 200 with an EMPTY body — the legacy CallbackServerServlet
//                    behaviour, i.e. a subscriber that never confirms. Use it to prove a subscription still
//                    succeeds when subscriber verification is off.
const express = require('express');

const app = express();
const port = process.env.PORT || 3022;

// Raw bytes, not parsed JSON: the x-hub-signature HMAC is computed over the exact delivered body, so
// re-serialising it through a JSON parser would break signature verification in the test.
app.use(express.raw({ type: '*/*', limit: '5mb' }));

// name -> { verifications, events, verificationCount, count }.
//
// WRITES create, READS DO NOT. The hub-driven paths (verification handshake, delivery) create on first touch
// because the name arrives from the product and its absence is not a test error. The introspection GET must NOT:
// allocating there made every read answer 200, so a typo'd receiver key reported count 0 and an "expected 0
// events" assertion passed vacuously against a receiver that never existed. A test therefore REGISTERS its
// receiver name up front (POST /register/:name) so an empty-but-real receiver is distinguishable from a wrong one.
//
// RETENTION IS BOUNDED. This container lives for the whole suite run and the WebSub steps deliberately never
// reset, so retaining every body unboundedly is a leak (express.raw accepts up to 5mb each). The COUNTS are exact
// and authoritative; the arrays keep only the most recent MAX_RETAINED records, and each stored body is truncated.
// Assertions read `count`/`verificationCount`; anything walking `events` sees a TAIL, not the whole history.
const MAX_RETAINED = 200;
const MAX_BODY_CHARS = 4096;
const receivers = new Map();

function newReceiver() {
    return { verifications: [], events: [], verificationCount: 0, count: 0 };
}

/** Hub-driven paths only: creates on first touch. */
function receiverFor(name) {
    if (!receivers.has(name)) {
        receivers.set(name, newReceiver());
    }
    return receivers.get(name);
}

function retain(list, record) {
    list.push(record);
    while (list.length > MAX_RETAINED) {
        list.shift();
    }
}

function recordVerification(req) {
    const record = {
        mode: req.query['hub.mode'] || null,
        topic: req.query['hub.topic'] || null,
        challenge: req.query['hub.challenge'] || null,
        leaseSeconds: req.query['hub.lease_seconds'] || null,
        headers: req.headers,
        receivedAt: new Date().toISOString()
    };
    const receiver = receiverFor(req.params.name);
    receiver.verificationCount += 1;
    retain(receiver.verifications, record);
    console.log(`----WebSub verification name=${req.params.name} mode=${record.mode} topic=${record.topic}`
        + ` challenge=${record.challenge}`);
    return record;
}

function recordDelivery(req) {
    const record = {
        headers: req.headers,
        body: Buffer.isBuffer(req.body) ? req.body.toString('utf8').slice(0, MAX_BODY_CHARS) : '',
        signature: req.headers['x-hub-signature'] || null,
        linkHeader: req.headers.link || null,
        receivedAt: new Date().toISOString()
    };
    const receiver = receiverFor(req.params.name);
    receiver.count += 1;
    retain(receiver.events, record);
    console.log(`----WebSub delivery name=${req.params.name} signature=${record.signature} body=${record.body}`);
    return record;
}

app.get('/health', (req, res) => res.json({ status: 'ok' }));

// Verifying callback: echo the challenge back so the hub confirms the (un)subscription. Echoed for unsubscribe as
// well as subscribe (the legacy servlet echoed on subscribe only); WebSub verifies both intents.
app.get('/receiver/:name', (req, res) => {
    const record = recordVerification(req);
    if (record.challenge === null) {
        res.status(400).type('text/plain').send('hub.challenge is missing');
        return;
    }
    res.status(200).type('text/plain').send(record.challenge);
});

// Non-verifying callback: records the handshake, answers 200 with an empty body.
app.get('/silent/:name', (req, res) => {
    recordVerification(req);
    res.status(200).type('text/plain').send('');
});

function handleDelivery(req, res) {
    recordDelivery(req);
    res.status(200).type('text/plain').send('');
}

app.post('/receiver/:name', handleDelivery);
app.post('/silent/:name', handleDelivery);

// Declares a receiver name BEFORE the hub knows about it, so the introspection read below can answer 404 for a
// name nothing ever registered instead of inventing an empty one. Idempotent.
app.post('/register/:name', (req, res) => {
    if (!receivers.has(req.params.name)) {
        receivers.set(req.params.name, newReceiver());
    }
    res.status(200).json({ name: req.params.name, registered: true });
});

// Introspection read by the test JVM over the HOST-published port. 404s for an unregistered name — never creates.
app.get('/events/:name', (req, res) => {
    const receiver = receivers.get(req.params.name);
    if (!receiver) {
        return res.status(404).json({
            name: req.params.name,
            message: 'no such receiver — it was never registered and the hub never touched it; check the key'
        });
    }
    res.json({
        name: req.params.name,
        count: receiver.count,
        verificationCount: receiver.verificationCount,
        verifications: receiver.verifications,
        events: receiver.events,
        retained: { events: receiver.events.length, max: MAX_RETAINED }
    });
});

function resetReceiver(req, res) {
    receivers.delete(req.params.name);
    res.json({ name: req.params.name, count: 0, verificationCount: 0, verifications: [], events: [] });
}

app.post('/events/:name/reset', resetReceiver);
app.delete('/events/:name', resetReceiver);

app.listen(port, () => {
    console.log(`WebSub callback receiver running at http://nodebackend:${port}`
        + ' (callbacks: /receiver/:name, /silent/:name; introspection: /events/:name)');
});
