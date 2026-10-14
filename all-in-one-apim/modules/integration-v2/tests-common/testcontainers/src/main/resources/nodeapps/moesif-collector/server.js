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

// Mock Moesif collector for the Moesif analytics-publisher tests (apim-analytics-publisher #165/#168/#169,
// carbon-apimgt #13927/#13951/#13956).
//
// WHY IT LIVES IN THIS IMAGE AND NOT IN THE TEST JVM: the gateway's analytics publisher talks to Moesif
// SERVER-TO-SERVER from inside the APIM container, so the collector has to be reachable on the shared docker
// network as http://nodebackend:3029 (that value goes into [apim.analytics.properties] moesif_base_url, which
// the Moesif SDK uses as its per-client Configuration.baseUri). For the same reason the ASSERTIONS cannot live
// here: they run in the test JVM, so recorded events are read back over the HOST-published introspection route.
//
// THE THREE ROUTES THE SDK ACTUALLY CALLS (extracted from moesifapi-1.8.5 APIController):
//   POST /v1/events        single event  — SimpleMoesifClient.publish
//   POST /v1/events/batch  event batch   — publishBatch, and EVERY retry-buffer probe/drain (the buffer always
//                                          uses the batch endpoint, even for a one-event batch)
//   GET  /v1/config        app config    — MoesifSamplingManager's periodic sample-rate fetch
// Nothing else is served on a /v1 path, so an SDK call we have not modelled 404s loudly instead of being
// silently absorbed.
//
// EVENTS ARE BUCKETED BY A REQUEST-HEADER VALUE, NOT GLOBALLY. [apim.analytics] enable=true is a
// container-global switch, so this collector receives an event for EVERY gateway invocation in the block, not
// just the one a scenario is asserting on. A global counter would therefore make every count assertion race
// with its neighbours. The publisher builds the event's `request.uri` from apiContext + apiResourceTemplate
// (SimpleMoesifClient.buildEventResponse) — the RESOURCE TEMPLATE, not the real request path — so neither a
// unique path segment nor a query parameter survives into the event and cannot be used as the key. What does
// survive is request headers (the block runs send_headers=true), so each scenario stamps a unique
// utils/Names.unique value into TRACE_HEADER and reads its own bucket back by that value. The lookup is
// case-insensitive because the gateway copies transport headers through a
// TreeMap(String.CASE_INSENSITIVE_ORDER) that preserves whatever casing the client sent.
//
// WRITES CREATE, READS DO NOT. Ingest creates a bucket on first touch, because the key arrives from the product
// and its absence there is not a test error. The introspection GET must NOT: allocating on read made every read
// answer 200, so a typo'd trace key reported count 0 and an "expected 0 events" assertion passed vacuously
// against a bucket that never existed. A scenario therefore REGISTERS its key up front
// (POST /__control/register/:key) so an empty-but-real bucket is distinguishable from a wrong one.
//
// ACCEPTED IS COUNTED SEPARATELY FROM ATTEMPTED. During a simulated outage the interesting assertion is "the
// gateway tried and we refused", which is indistinguishable from "the gateway never called" if only accepted
// events are counted. `attempts` counts every ingest request reaching this app whatever the injected status;
// `count` counts only events accepted with a 2xx.
//
// RETENTION IS BOUNDED. This container lives for the whole suite run and nothing resets it between blocks, so
// retaining every event unboundedly is a leak — captured bodies alone can approach payload_size_limit each. The
// COUNTS are exact and authoritative; `events` keeps only the most recent MAX_RETAINED, each with its bodies
// truncated. Assertions that need a count read `count`; assertions that need a shape read the tail.
const express = require('express');

const app = express();
const port = process.env.PORT || 3029;

// Request header whose value buckets an event. Not a Moesif concept — purely this fixture's correlation key.
const TRACE_HEADER = 'x-trace-id';

const MAX_RETAINED = 50;
const MAX_BODY_CHARS = 8192;

// Moesif ingest answers 201 Created on success. The SDK treats any 2xx as success
// (HttpStatusHelper.isSuccess), 5xx/408/429 as retryable (shouldRetry) and every other 4xx as a permanent
// rejection — which is exactly the three-way split the outage-control route drives.
const INGEST_OK_STATUS = 201;

// Bodies can be large and JSON-parsing them is the point (a captured JSON body must arrive as a structured
// object), so raise the limit well above the block's payload_size_limit.
app.use(express.json({ limit: '10mb', type: () => true }));

// key -> { count, attempts, events: [] }
const buckets = new Map();

// How many times GET /v1/config has been served. Exposed so a test can WAIT FOR an observed refresh rather
// than sleeping for one: after changing the sample rate, the point at which the gateway has definitely seen
// the new value is the next fetch AFTER the change, which is a poll-able fact rather than a timing guess.
let configFetches = 0;

// Injected ingest status. 2xx accepts; anything else is returned instead, which is how a scenario simulates a
// Moesif outage (503 → retryable → the retry queue engages) or a permanent rejection (401 → dropped, no retry).
let ingestStatus = INGEST_OK_STATUS;

// What GET /v1/config answers with. sample_rate 100 = publish everything, which is the state every scenario
// must find on entry; the sampling scenario moves it and its hook puts it back.
let appConfig = {
    org_id: 'mock-org',
    app_id: 'mock-app',
    sample_rate: 100,
    user_sample_rate: {},
    company_sample_rate: {},
    user_rules: {},
    company_rules: {},
    regex_config: []
};

function newBucket() {
    return { count: 0, attempts: 0, events: [] };
}

/** Ingest path only: creates on first touch. */
function bucketFor(key) {
    if (!buckets.has(key)) {
        buckets.set(key, newBucket());
    }
    return buckets.get(key);
}

/** Case-insensitive lookup of the trace header inside an event's request.headers map. */
function traceKeyOf(event) {
    const headers = event && event.request && event.request.headers;
    if (!headers || typeof headers !== 'object') {
        return null;
    }
    for (const name of Object.keys(headers)) {
        if (name.toLowerCase() === TRACE_HEADER) {
            return String(headers[name]);
        }
    }
    return null;
}

function truncate(value) {
    if (typeof value !== 'string' || value.length <= MAX_BODY_CHARS) {
        return value;
    }
    return value.slice(0, MAX_BODY_CHARS);
}

/**
 * Retains the event with its bodies bounded. A structured (already-deserialized) JSON body is kept as-is —
 * that IS the assertion target for the JSON capture scenario, and re-stringifying it would lose the
 * distinction between "arrived as an object" and "arrived as a string".
 */
function retain(bucket, event) {
    const trimmed = JSON.parse(JSON.stringify(event));
    if (trimmed.request && typeof trimmed.request.body === 'string') {
        trimmed.request.body = truncate(trimmed.request.body);
    }
    if (trimmed.response && typeof trimmed.response.body === 'string') {
        trimmed.response.body = truncate(trimmed.response.body);
    }
    bucket.events.push(trimmed);
    while (bucket.events.length > MAX_RETAINED) {
        bucket.events.shift();
    }
}

/**
 * Shared ingest handler for both event routes. Counts the attempt against every bucket the payload names
 * BEFORE the injected status is considered, so a refused batch is still visible as an attempt; accepts (and
 * retains) only on a 2xx.
 */
function ingest(req, res, label) {
    const payload = req.body;
    const events = Array.isArray(payload) ? payload : [payload];
    const accepted = ingestStatus >= 200 && ingestStatus < 300;

    for (const event of events) {
        const key = traceKeyOf(event);
        if (key === null) {
            // An event from some other scenario's traffic, or one with headers suppressed. Not an error —
            // this collector sees the whole block's traffic — but there is no bucket to attribute it to.
            continue;
        }
        const bucket = bucketFor(key);
        bucket.attempts++;
        if (accepted) {
            bucket.count++;
            retain(bucket, event);
        }
    }
    console.log(`----moesif-collector ${label}: ${events.length} event(s), answering ${ingestStatus}`);
    res.status(ingestStatus).json(accepted ? {} : { error: `injected status ${ingestStatus}` });
}

app.post('/v1/events', (req, res) => ingest(req, res, 'POST /v1/events'));
app.post('/v1/events/batch', (req, res) => ingest(req, res, 'POST /v1/events/batch'));

app.get('/v1/config', (req, res) => {
    configFetches++;
    console.log(`----moesif-collector GET /v1/config #${configFetches}, sample_rate: ${appConfig.sample_rate}`);
    // The SDK reads the app config's ETag off this header; sending a stable one is harmless because
    // the publisher's sampling manager does its own unconditional fetch and ignores ETags entirely.
    res.set('X-Moesif-Config-ETag', 'mock-etag');
    res.status(200).json(appConfig);
});

// --- test-only control and introspection (read from the test JVM over the host-published port) ---

/** Registers a trace key so a later read distinguishes "no events yet" from "wrong key". */
app.post('/__control/register/:key', (req, res) => {
    bucketFor(req.params.key);
    res.status(200).json({ registered: req.params.key });
});

/** Reads one bucket. Deliberately does NOT create: an unregistered key is a test bug, so it 404s. */
app.get('/__events/:key', (req, res) => {
    const bucket = buckets.get(req.params.key);
    if (!bucket) {
        res.status(404).json({ error: `no bucket registered for '${req.params.key}'` });
        return;
    }
    res.status(200).json({
        key: req.params.key,
        count: bucket.count,
        attempts: bucket.attempts,
        events: bucket.events
    });
});

/** How many times the app config has been fetched, for the observed-refresh wait. */
app.get('/__config-fetches', (req, res) => {
    res.status(200).json({ configFetches: configFetches });
});

/** Sets the status every subsequent ingest answers with. Body: {"status": 503}. */
app.post('/__control/ingest-status', (req, res) => {
    const requested = Number(req.body && req.body.status);
    if (!Number.isInteger(requested) || requested < 100 || requested > 599) {
        res.status(400).json({ error: 'status must be an integer HTTP status code' });
        return;
    }
    ingestStatus = requested;
    console.log(`----moesif-collector ingest status set to ${ingestStatus}`);
    res.status(200).json({ status: ingestStatus });
});

/** Sets what GET /v1/config answers. Body: {"sample_rate": 0}. */
app.post('/__control/sample-rate', (req, res) => {
    const requested = Number(req.body && req.body.sample_rate);
    if (!Number.isInteger(requested)) {
        res.status(400).json({ error: 'sample_rate must be an integer' });
        return;
    }
    appConfig = Object.assign({}, appConfig, { sample_rate: requested });
    console.log(`----moesif-collector sample_rate set to ${requested}`);
    // The fetch count AT THE MOMENT OF THE CHANGE is the baseline a caller waits past: any fetch after this
    // number necessarily returned the new rate.
    res.status(200).json({ sample_rate: requested, configFetches: configFetches });
});

/** Drops one bucket's recorded state; the key stays registered so reads still 200 with count 0. */
app.post('/__control/reset/:key', (req, res) => {
    buckets.set(req.params.key, newBucket());
    res.status(200).json({ reset: req.params.key });
});

app.listen(port, () => {
    console.log(`moesif-collector listening on ${port}`);
});
