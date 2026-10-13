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

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Names;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Steps for the Moesif analytics publisher: what the gateway actually publishes to Moesif for a gateway
 * invocation (captured request/response bodies, redacted and masked headers, the sampling weight), and the
 * publisher's own reliability behaviour (dynamic sampling, the retry queue).
 *
 * <p>Covers apim-analytics-publisher #165 (dynamic sampling + retry queue) and #168/#169 (body support), and
 * carbon-apimgt #13927/#13951/#13956 (gateway-side capture, the unbuildable-content-type guard, and the
 * credential strip).
 *
 * <p>ASSERTIONS READ A MOCK COLLECTOR, NOT MOESIF. The block points
 * {@code [apim.analytics.properties] moesif_base_url} at the {@code moesif-collector} node app, which serves the
 * three routes the Moesif SDK calls and records what it receives. That app's header comment carries the design
 * rationale; the two parts that shape every step here are:
 * <ul>
 *   <li><b>Events are bucketed by a request-header value.</b> {@code [apim.analytics] enable = true} is
 *       container-global, so the collector sees an event for EVERY invocation in the block. The published event's
 *       {@code request.uri} is built from apiContext + resourceTemplate, so neither a unique path nor a query
 *       parameter survives into it — a request HEADER does (the block runs {@code send_headers = true}). Each
 *       scenario therefore takes its own trace id and stamps it into {@code X-Trace-Id}.</li>
 *   <li><b>Counts must be asserted SETTLED.</b> Publishing is asynchronous through a queue, and the retry queue's
 *       recovery drain is exactly where a duplicate delivery would appear, so every count goes through
 *       {@link Utils#awaitSettledCount} — an accept-on-reaching-N loop cannot see an over-count.</li>
 * </ul>
 *
 * <p>Collector control and introspection calls are INFRA-side reads consumed inside the step, so they go through
 * {@link SimpleHTTPClient} directly and deliberately do NOT publish {@code httpResponse} (CLAUDE.md §7). Gateway
 * invocations are not made here at all — they funnel through {@link APIInvocationSteps} like every other
 * invocation in the suite.
 */
public class MoesifAnalyticsSteps {

    private static final Log log = LogFactory.getLog(MoesifAnalyticsSteps.class);

    /** Container port of the node {@code moesif-collector} backend (published to the host — see NodeAppServer). */
    private static final int MOESIF_COLLECTOR_PORT = 3029;

    /** The request header whose value buckets an event at the collector. Must match TRACE_HEADER in server.js. */
    private static final String TRACE_HEADER = "X-Trace-Id";

    /** Ingest status meaning "accepted" — what the collector answers unless a scenario injects something else. */
    private static final int INGEST_OK_STATUS = 201;

    /** The sample rate every scenario must find at the collector on entry: publish everything. */
    private static final int DEFAULT_SAMPLE_RATE = 100;

    /**
     * How long the cleanup hook waits for the publisher to re-read a restored sample rate. Comfortably over the
     * block's 5000ms refresh interval, since the wait must span a whole schedule tick plus the fetch itself.
     */
    private static final long RESTORE_REFRESH_TIMEOUT_MILLIS = 30_000L;

    /** Base poll interval while watching for that refresh. */
    private static final long CONFIG_REFRESH_POLL_MILLIS = 500L;

    /**
     * How long an event count must stay UNCHANGED before it is taken as final (see
     * {@link Utils#awaitSettledCount}). Must exceed the gap between two consecutive arrivals, not the total
     * publish time. Two things set the floor here: the publisher's queue worker batches for up to 2000ms before
     * flushing, and the block's {@code retry_interval_seconds = 2} paces the retry-queue drain one batch per
     * probe tick. 15s leaves several drain ticks of headroom. Raise it if a count ever settles LOW.
     */
    private static final long EVENT_SETTLE_QUIET_MILLIS = 15_000L;

    /**
     * Whether this scenario touched collector state that outlives it. Both control knobs are collector-GLOBAL
     * (there is one Moesif key, hence one app config and one reachability state), so the hook below has to put
     * them back even though scenarios in this block run sequentially — the next scenario's baseline is otherwise
     * whatever the last one left.
     */
    private boolean collectorStateMutated;

    /**
     * The collector's app-config fetch count at the moment this scenario last changed the sample rate. The
     * observed-refresh wait polls for a count STRICTLY GREATER than this, which is what makes "the gateway has
     * read the new rate" a fact rather than an elapsed-time assumption.
     */
    private int fetchesWhenRateSet;

    // ---------------------------------------------------------------------------------------------
    // Trace key (the per-scenario event bucket)
    // ---------------------------------------------------------------------------------------------

    /**
     * Takes a unique trace id for this scenario, stores it under {@code contextKey}, and DECLARES it to the
     * collector.
     *
     * <p>Declaring up front is what makes a zero-count assertion meaningful: the collector's introspection route
     * 404s for a key it has never seen, so a typo'd context key fails loudly instead of reading back an invented
     * empty bucket and vacuously satisfying "exactly 0 events were published". Registering here is also what
     * keeps a legitimate not-yet-published bucket answering 200 with a zero count.
     *
     * <p>The scenario then sends the id in {@code X-Trace-Id} on every invocation it wants attributed to itself.
     */
    @Given("I have a Moesif analytics trace key stored as {string}")
    public void iHaveAMoesifTraceKey(String contextKey) throws IOException {

        String key = Utils.normalizeContextKey(contextKey);
        String traceId = Names.unique(key).replaceAll("[^A-Za-z0-9]", "");
        TestContext.set(key, traceId);

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(
                collectorUrl("/__control/register/" + traceId), new HashMap<>(), "",
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertTrue(response != null && response.getResponseCode() == 200,
                "Could not register Moesif trace key '" + traceId + "' at the mock collector; got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        log.info("Moesif analytics trace key '" + traceId + "' registered for context key '" + key + "'.");
    }

    // ---------------------------------------------------------------------------------------------
    // Collector control (outage / sample rate)
    // ---------------------------------------------------------------------------------------------

    /**
     * Makes the collector answer every subsequent event ingest with {@code status} instead of accepting it.
     *
     * <p>This is how a Moesif outage is simulated without touching the network: 503 is retryable
     * ({@code HttpStatusHelper.shouldRetry} = 5xx plus 408/429) so the retry queue engages, while 401 is a
     * permanent rejection so the events are dropped without retry. The two cases are the whole point of the
     * retry-queue pair of scenarios, so the status is stated per scenario rather than hidden behind an
     * "outage"/"recovery" wording.
     */
    @When("the Moesif collector starts rejecting events with status {int}")
    public void theCollectorRejectsEventsWith(int status) throws IOException {

        setIngestStatus(status);
    }

    /** Puts the collector back to accepting events, so a queued backlog can drain. */
    @When("the Moesif collector starts accepting events again")
    public void theCollectorAcceptsEventsAgain() throws IOException {

        setIngestStatus(INGEST_OK_STATUS);
    }

    /**
     * Sets the {@code sample_rate} the collector's {@code GET /v1/config} answers with, and remembers how many
     * times the config had been fetched at that moment.
     *
     * <p>Sample rates are deliberately NOT part of the gateway's configuration: they live in the Moesif
     * application config, which the publisher re-fetches on a fixed schedule. That is what makes a rate change
     * observable with NO server restart, and it is why one container can cover the whole sampling arc.
     *
     * <p>The fetch count is the baseline for {@link #iWaitForTheSamplingConfigRefresh}: any fetch after it
     * necessarily returned the new rate.
     */
    @When("the Moesif app config reports a sample rate of {int}")
    public void theMoesifAppConfigReportsSampleRate(int sampleRate) throws IOException {

        fetchesWhenRateSet = setSampleRate(sampleRate);
    }

    /** POSTs a sample rate and returns the collector's fetch count at that instant (the wait baseline). */
    private int setSampleRate(int sampleRate) throws IOException {

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(collectorUrl("/__control/sample-rate"),
                new HashMap<>(), "{\"sample_rate\": " + sampleRate + "}",
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertTrue(response != null && response.getResponseCode() == 200,
                "Could not set the mock Moesif app config sample rate to " + sampleRate + "; got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        collectorStateMutated = true;
        int fetches = new JSONObject(response.getData()).getInt("configFetches");
        log.info("Mock Moesif app config sample_rate set to " + sampleRate + " (config fetched " + fetches
                + " time(s) so far).");
        return fetches;
    }

    /**
     * Waits until the publisher has re-fetched the app config SINCE the rate was last set, so the following
     * assertion is about the new rate rather than a race with the refresh schedule.
     *
     * <p>Polled, not slept (CLAUDE.md §4). The refresh is a real, observable event — the collector counts every
     * {@code GET /v1/config} it serves — so waiting for the count to move past the baseline captured when the
     * rate was set proves the gateway has actually READ the new value. Sleeping for one nominal interval instead
     * would be a timing guess that is simultaneously flaky (a refresh landing microseconds late) and slower than
     * necessary (the common case returns as soon as the fetch happens).
     */
    @When("I wait for the Moesif sampling config to be re-fetched within {int} seconds")
    public void iWaitForTheSamplingConfigRefresh(int timeoutSeconds) throws Exception {

        int baseline = fetchesWhenRateSet;
        Integer observed = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> new JSONObject(readJson(collectorUrl("/__config-fetches"))).getInt("configFetches"),
                fetches -> fetches > baseline);
        Assert.assertNotNull(observed, "The mock Moesif collector never reported its app-config fetch count.");
        Assert.assertTrue(observed > baseline, "The publisher did not re-fetch the Moesif app config within "
                + timeoutSeconds + "s of the rate change (still " + observed + " fetch(es), baseline " + baseline
                + "), so a sampling assertion now would race the refresh schedule.");
        log.info("Moesif app config re-fetched (" + baseline + " -> " + observed + "); the new sample rate is live.");
    }

    // ---------------------------------------------------------------------------------------------
    // Event count assertions
    // ---------------------------------------------------------------------------------------------

    /**
     * Asserts the collector accepted EXACTLY {@code expected} events for this scenario's trace key, and that the
     * count had stopped changing when it was read.
     *
     * <p>Both halves matter. Publishing is asynchronous, so a count read too early is low; and the retry queue's
     * recovery drain is precisely where a duplicate delivery would show up, so a count read the moment it reaches
     * the expected value cannot see an over-count. {@link Utils#awaitSettledCount} is the envelope that catches
     * both (see its javadoc).
     */
    @Then("Moesif should have received exactly {int} events for trace key {string} within {int} seconds")
    public void moesifShouldHaveReceivedExactlyEvents(int expected, String traceKey, int timeoutSeconds)
            throws InterruptedException {

        String traceId = TestContext.resolve(traceKey).toString();
        Utils.SettledCount settled = Utils.awaitSettledCount(EVENT_SETTLE_QUIET_MILLIS, timeoutSeconds * 1000L,
                () -> readBucket(traceId).getInt("count"));
        Assert.assertTrue(settled.settled(), "The Moesif event count for trace key '" + traceId + "' never stopped "
                + "changing (last seen " + settled.value() + " over " + settled.samples() + " sample(s), quiet window "
                + EVENT_SETTLE_QUIET_MILLIS + "ms), so no exact count can be claimed.");
        Assert.assertEquals(settled.value(), expected, "Moesif received " + settled.value() + " event(s) for trace "
                + "key '" + traceId + "' but exactly " + expected + " were expected.");
    }

    /**
     * Asserts the collector was ASKED to accept at least {@code expected} events for this trace key, whatever it
     * answered.
     *
     * <p>The counterpart to the accepted count, and the reason the collector counts attempts separately: during a
     * simulated outage "0 accepted" is on its own ambiguous — it is equally consistent with the gateway never
     * having published at all, which would make the retry-queue scenario pass for the wrong reason. At LEAST,
     * not exactly, because a retried batch is legitimately re-attempted on every probe tick.
     */
    @Then("Moesif should have been offered at least {int} events for trace key {string} within {int} seconds")
    public void moesifShouldHaveBeenOfferedAtLeastEvents(int expected, String traceKey, int timeoutSeconds)
            throws Exception {

        String traceId = TestContext.resolve(traceKey).toString();
        Integer attempts = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> readBucket(traceId).getInt("attempts"), seen -> seen >= expected);
        Assert.assertNotNull(attempts, "No attempt count could be read for trace key '" + traceId + "'.");
        Assert.assertTrue(attempts >= expected, "Moesif was offered only " + attempts + " event(s) for trace key '"
                + traceId + "' but at least " + expected + " were expected — the gateway never attempted to "
                + "publish, so nothing can be concluded about how the failure was handled.");
    }

    // ---------------------------------------------------------------------------------------------
    // Event shape assertions
    // ---------------------------------------------------------------------------------------------

    /**
     * Reads the single event recorded for this trace key and stores it for the assertions that follow, failing if
     * the count is not exactly one.
     *
     * <p>Pinning the cardinality here is what makes every following assertion sound: they read "the" event, and
     * indexing into a list that unexpectedly holds two would silently assert against whichever arrived first.
     */
    @Then("the Moesif event for trace key {string} should be the only one and stored as {string}")
    public void theMoesifEventShouldBeTheOnlyOne(String traceKey, String contextKey)
            throws InterruptedException, IOException {

        String traceId = TestContext.resolve(traceKey).toString();
        Utils.SettledCount settled = Utils.awaitSettledCount(EVENT_SETTLE_QUIET_MILLIS, 0L,
                () -> readBucket(traceId).getInt("count"));
        Assert.assertTrue(settled.settled(), "The Moesif event count for trace key '" + traceId + "' never settled "
                + "(last seen " + settled.value() + "), so the event under assertion cannot be identified.");
        Assert.assertEquals(settled.value(), 1, "Expected exactly one Moesif event for trace key '" + traceId
                + "' but " + settled.value() + " were recorded; the assertions that follow read 'the' event.");

        // One read AFTER settling: the count is now final, so the retained list cannot grow under the assertion.
        JSONArray events = readBucket(traceId).getJSONArray("events");
        Assert.assertEquals(events.length(), 1, "The collector reported a settled count of 1 for trace key '"
                + traceId + "' but retained " + events.length() + " event(s).");
        TestContext.set(Utils.normalizeContextKey(contextKey), events.getJSONObject(0).toString());
        log.info("Moesif event for trace key '" + traceId + "': " + events.getJSONObject(0));
    }

    /**
     * Asserts a captured body arrived as a STRUCTURED object whose field equals an exact value.
     *
     * <p>"Structured" is half the assertion. A JSON body is meant to reach Moesif as a deserialized object (the
     * SDK's {@code BodyParser} does that when the Content-Type says json or the body starts with {@code {}/[}),
     * which is what makes it searchable there. A body that arrived as an opaque string — or Base64 — would still
     * "contain" the value, so a substring check would pass on exactly the regression this is guarding.
     *
     * @param direction {@code request} or {@code response}
     */
    @Then("the Moesif event {string} should have {string} body field {string} equal to {string}")
    public void theMoesifEventBodyFieldShouldEqual(String eventKey, String direction, String field,
                                                   String expected) {

        JSONObject side = eventSide(eventKey, direction);
        Assert.assertTrue(side.has("body") && !side.isNull("body"),
                "The " + direction + " carried no body at all; expected field '" + field + "'.");
        Object body = side.get("body");
        Assert.assertTrue(body instanceof JSONObject, "The " + direction + " body did not reach Moesif as a "
                + "structured object (it arrived as " + body.getClass().getSimpleName() + ": " + body + "), so it "
                + "is not searchable there even though it may contain the right text.");
        JSONObject bodyObject = (JSONObject) body;
        Assert.assertTrue(bodyObject.has(field), "The " + direction + " body has no field '" + field + "'; body="
                + bodyObject);
        Assert.assertEquals(bodyObject.get(field).toString(), Utils.resolveContextPlaceholders(expected),
                "The " + direction + " body field '" + field + "' did not match; body=" + bodyObject);
    }

    /**
     * Asserts a captured body arrived Base64-encoded and decodes to exactly {@code expected}.
     *
     * <p>Decoded with a MIME decoder, not the strict one: the Moesif SDK encodes with {@code Base64.DEFAULT},
     * which line-wraps at 76 characters and appends a newline, so a strict decoder rejects any body over that
     * length. Getting this wrong produces a failure that looks like a product bug and is not one — the publisher's
     * own unit tests document the same trap.
     */
    @Then("the Moesif event {string} should have a base64 {string} body decoding to {string}")
    public void theMoesifEventBodyShouldDecodeTo(String eventKey, String direction, String expected) {

        JSONObject side = eventSide(eventKey, direction);
        Assert.assertEquals(side.optString("transfer_encoding", null), "base64",
                "The " + direction + " body was not flagged as base64; event side=" + side);
        String encoded = side.optString("body", null);
        Assert.assertNotNull(encoded, "The " + direction + " carried no body to decode.");
        String decoded = new String(Base64.getMimeDecoder().decode(encoded), StandardCharsets.UTF_8);
        Assert.assertEquals(decoded, Utils.resolveContextPlaceholders(expected),
                "The decoded " + direction + " body did not match what was sent.");
    }

    /**
     * Asserts NO body was published for one direction — the shape every drop and skip produces.
     *
     * <p>The gateway never truncates: an oversized body, an excluded content type, a content type with no
     * registered Axis2 message builder, and a body-less method all produce the SAME published shape, namely the
     * absence of the key. A test that accepted a shortened body would pass while the product silently started
     * sending partial payloads to a third party.
     */
    @Then("the Moesif event {string} should have no {string} body")
    public void theMoesifEventShouldHaveNoBody(String eventKey, String direction) {

        JSONObject side = eventSide(eventKey, direction);
        Assert.assertTrue(!side.has("body") || side.isNull("body"),
                "A " + direction + " body was published when none was expected: " + side.opt("body"));
    }

    /**
     * Asserts a header is ABSENT from a published event's header map, in ANY casing.
     *
     * <p>Case-insensitively, because that is the guarantee: the gateway copies transport headers through a
     * {@code TreeMap(String.CASE_INSENSITIVE_ORDER)} and strips by a case-insensitive predicate, so an HTTP/2
     * client sending {@code authorization} in lower case must be stripped just as one sending
     * {@code Authorization} is. Asserting only the exact spelling would pass while the lower-case form leaked.
     */
    @Then("the Moesif event {string} should have no {string} header {string}")
    public void theMoesifEventShouldHaveNoHeader(String eventKey, String direction, String headerName) {

        JSONObject headers = eventHeaders(eventKey, direction);
        for (String name : headers.keySet()) {
            Assert.assertFalse(name.equalsIgnoreCase(headerName), "The " + direction + " header '" + headerName
                    + "' was published to Moesif as '" + name + "' — a credential or session header must never "
                    + "leave the gateway. Headers=" + headers.keySet());
        }
    }

    /**
     * Asserts a header IS present in a published event's header map with an exact value (case-insensitive on the
     * NAME, exact on the VALUE).
     *
     * <p>Carries two distinct jobs. It pins a masked header positively — {@code == "*****"}, not "the real value
     * is absent" — because mask parsing fails SAFE: a mask configuration the gateway cannot parse masks nothing
     * and logs a warning, which an absence-based assertion would report as a pass. And it is the control for the
     * redaction assertions: a benign header surviving with its exact value is what proves header publishing was
     * switched on at all, so the "no Authorization header" assertions are not vacuously true.
     */
    @Then("the Moesif event {string} should have {string} header {string} equal to {string}")
    public void theMoesifEventShouldHaveHeaderEqualTo(String eventKey, String direction, String headerName,
                                                      String expected) {

        JSONObject headers = eventHeaders(eventKey, direction);
        String matched = null;
        for (String name : headers.keySet()) {
            if (name.equalsIgnoreCase(headerName)) {
                matched = name;
                break;
            }
        }
        Assert.assertNotNull(matched, "The " + direction + " header '" + headerName + "' was not published at all; "
                + "headers=" + headers.keySet());
        Assert.assertEquals(headers.get(matched).toString(), Utils.resolveContextPlaceholders(expected),
                "The published " + direction + " header '" + matched + "' did not have the expected value.");
    }

    /** Asserts a top-level event field equals an exact value — the sampling {@code weight}, the HTTP verb. */
    @Then("the Moesif event {string} should have field {string} equal to {string}")
    public void theMoesifEventFieldShouldEqual(String eventKey, String field, String expected) {

        JSONObject event = new JSONObject(TestContext.resolve(eventKey).toString());
        Assert.assertTrue(event.has(field), "The Moesif event has no field '" + field + "'; event=" + event);
        Assert.assertEquals(event.get(field).toString(), Utils.resolveContextPlaceholders(expected),
                "The Moesif event field '" + field + "' did not match; event=" + event);
    }

    /** Asserts an event metadata entry equals an exact value — where the captured content types deliberately ride. */
    @Then("the Moesif event {string} should have metadata {string} equal to {string}")
    public void theMoesifEventMetadataShouldEqual(String eventKey, String field, String expected) {

        JSONObject event = new JSONObject(TestContext.resolve(eventKey).toString());
        JSONObject metadata = event.optJSONObject("metadata");
        Assert.assertNotNull(metadata, "The Moesif event carried no metadata; event=" + event);
        Assert.assertTrue(metadata.has(field), "The Moesif event metadata has no '" + field + "'; metadata="
                + metadata);
        Assert.assertEquals(metadata.get(field).toString(), Utils.resolveContextPlaceholders(expected),
                "The Moesif event metadata '" + field + "' did not match; metadata=" + metadata);
    }

    /**
     * Asserts EVERY event recorded for this trace key carries the same exact {@code weight}.
     *
     * <p>Every, rather than a count, because at a partial sample rate the number of events that arrive is decided
     * per event by {@code ThreadLocalRandom} — asserting a count there would be flaky by construction. The weight
     * is not random: it is {@code floor(100 / rate)} for whatever rate the decision used, so it is exactly
     * assertable on whichever events did arrive. Requires at least one event, or the assertion is vacuous.
     */
    @Then("every Moesif event for trace key {string} should have weight {int}")
    public void everyMoesifEventShouldHaveWeight(String traceKey, int expectedWeight) throws IOException {

        String traceId = TestContext.resolve(traceKey).toString();
        JSONArray events = readBucket(traceId).getJSONArray("events");
        Assert.assertTrue(events.length() > 0, "No Moesif event was recorded for trace key '" + traceId + "', so "
                + "asserting a weight of " + expectedWeight + " on 'every' event would be vacuous.");
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.getJSONObject(i);
            Assert.assertEquals(event.optInt("weight", Integer.MIN_VALUE), expectedWeight,
                    "Event " + i + " of " + events.length() + " for trace key '" + traceId + "' carried weight "
                            + event.opt("weight") + " rather than " + expectedWeight + "; event=" + event);
        }
        log.info("All " + events.length() + " Moesif event(s) for trace key '" + traceId + "' carried weight "
                + expectedWeight + ".");
    }

    /**
     * Asserts the BACKEND received exactly the bytes the scenario sent, read from the digest headers the
     * reflect-body-sensitive route sets rather than from the echoed response body.
     *
     * <p>This is the "capture did not damage the proxied message" assertion, and it has to read headers. Capture
     * can rebuild and re-serialize the message on the response leg too, so the echoed BODY cannot isolate the
     * request leg: comparing it against the request cannot separate "the request was altered" from "the
     * response was". Headers are not touched by the message builder, so the digest is a clean observation of
     * the request leg alone.
     *
     * <p>Asserts the byte length as well as the digest: a length mismatch names the failure ("truncated at N")
     * where a digest mismatch alone only says "different".
     */
    @Then("the backend should have received exactly the body {string}")
    public void theBackendShouldHaveReceivedExactly(String bodyKey) {

        String expected = TestContext.resolve(bodyKey).toString();
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertNotNull(response, "No response was captured, so the backend's view cannot be read.");

        String receivedBytes = headerIgnoringCase(response, "X-Received-Bytes");
        String receivedSha = headerIgnoringCase(response, "X-Received-Sha256");
        Assert.assertNotNull(receivedBytes, "The backend did not report X-Received-Bytes — the invocation did not "
                + "reach the reflect-body-sensitive route, so nothing can be concluded about forwarding.");

        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        Assert.assertEquals(Integer.parseInt(receivedBytes), expectedBytes.length,
                "The backend received " + receivedBytes + " bytes but " + expectedBytes.length + " were sent, so "
                        + "capture altered the length of the forwarded body.");
        Assert.assertEquals(receivedSha, sha256Hex(expectedBytes),
                "The backend received " + receivedBytes + " bytes — the right LENGTH — but a different digest, so "
                        + "capture altered the content of the forwarded body.");
    }

    // ---------------------------------------------------------------------------------------------
    // Cleanup
    // ---------------------------------------------------------------------------------------------

    /**
     * Restores the collector's global knobs after any scenario that moved them.
     *
     * <p>Runs as a hook rather than as feature steps so it also runs when the scenario FAILS: a scenario that
     * died mid-outage would otherwise leave the collector rejecting every event, and every later scenario in the
     * block would fail for a reason that has nothing to do with what it asserts. Idempotent, and only acts when
     * something was actually changed.
     */
    @After("@moesif-analytics")
    public void restoreCollectorState() {

        if (!collectorStateMutated) {
            return;
        }
        try {
            setIngestStatus(INGEST_OK_STATUS);
            int baseline = setSampleRate(DEFAULT_SAMPLE_RATE);
            // WAIT FOR THE PUBLISHER TO OBSERVE THE RESTORED RATE, don't just set it. Restoring the collector's
            // config is instant; the gateway only re-reads it on its refresh schedule, so a scenario starting
            // inside that window still samples at the PREVIOUS rate. Measured: a scenario that ran straight after
            // the rate-50 scenario published 2 of its 3 events and its exact-count assertion failed — the classic
            // state-leak-across-scenarios failure, and invisible because the leaked state is inside the server.
            boolean observed = awaitConfigRefreshPast(baseline);
            log.info("Mock Moesif collector restored: ingest " + INGEST_OK_STATUS + ", sample_rate "
                    + DEFAULT_SAMPLE_RATE + (observed ? " (publisher has re-read it)."
                    : " (publisher had NOT re-read it before the wait expired)."));
        } catch (IOException couldNotRestore) {
            // WARN, not a failure: this hook must not convert a passing scenario into a failing one, but a leaked
            // outage state is the most likely cause of a cascade in the rest of the block, so it must be visible.
            log.warn("Could not restore the mock Moesif collector state; later scenarios in this block may fail "
                    + "for an unrelated reason.", couldNotRestore);
        } finally {
            collectorStateMutated = false;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** Host-reachable URL of a collector route. */
    private static String collectorUrl(String path) {
        return Utils.getNodeBackendUrl(MOESIF_COLLECTOR_PORT) + path;
    }

    private void setIngestStatus(int status) throws IOException {

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(collectorUrl("/__control/ingest-status"),
                new HashMap<>(), "{\"status\": " + status + "}", Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertTrue(response != null && response.getResponseCode() == 200,
                "Could not set the mock Moesif collector ingest status to " + status + "; got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        collectorStateMutated = true;
        log.info("Mock Moesif collector ingest status set to " + status + ".");
    }

    /** Case-insensitive response-header lookup (null when absent) — HTTP header names are not case-sensitive. */
    private static String headerIgnoringCase(HttpResponse response, String name) {

        if (response.getHeaders() == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String sha256Hex(byte[] data) {

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException everyJvmHasSha256) {
            throw new IllegalStateException("SHA-256 unavailable", everyJvmHasSha256);
        }
    }

    /**
     * Polls until the collector has served an app-config fetch beyond {@code baseline}, returning whether it did.
     *
     * <p>Returns a boolean instead of asserting because its caller is a cleanup hook: a hook that fails turns a
     * PASSING scenario red and points at the wrong scenario. A timeout here is reported as a WARN and the next
     * scenario's own assertions still catch a genuinely stale rate.
     */
    private static boolean awaitConfigRefreshPast(int baseline) {

        long deadline = System.currentTimeMillis() + RESTORE_REFRESH_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (new JSONObject(readJson(collectorUrl("/__config-fetches"))).getInt("configFetches") > baseline) {
                    return true;
                }
                Utils.pollPause(System.currentTimeMillis(), CONFIG_REFRESH_POLL_MILLIS);
            } catch (IOException transientRead) {
                return false;
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /** GETs a collector route and returns its body, asserting a 200 so a mis-typed route fails loudly. */
    private static String readJson(String url) throws IOException {

        HttpResponse response = SimpleHTTPClient.getInstance().doGet(url, new HashMap<>());
        Assert.assertNotNull(response, "No response from the mock Moesif collector at " + url + ".");
        Assert.assertEquals(response.getResponseCode(), 200, "The mock Moesif collector answered "
                + response.getResponseCode() + " for " + url + ": " + response.getData());
        return response.getData();
    }

    /**
     * Reads one trace key's bucket from the collector. A 404 means the key was never registered — a test bug, not
     * a product one, so it is reported as such rather than treated as an empty bucket.
     */
    private static JSONObject readBucket(String traceId) throws IOException {

        HttpResponse response = SimpleHTTPClient.getInstance().doGet(collectorUrl("/__events/" + traceId),
                new HashMap<>());
        Assert.assertNotNull(response, "No response from the mock Moesif collector for trace key '" + traceId + "'.");
        Assert.assertEquals(response.getResponseCode(), 200, "The mock Moesif collector answered "
                + response.getResponseCode() + " for trace key '" + traceId + "' (404 means the key was never "
                + "registered — the scenario is missing its trace-key Given): " + response.getData());
        return new JSONObject(response.getData());
    }

    /** {@code request} or {@code response} object of a stored event. */
    private static JSONObject eventSide(String eventKey, String direction) {

        JSONObject event = new JSONObject(TestContext.resolve(eventKey).toString());
        if (!"request".equals(direction) && !"response".equals(direction)) {
            throw new IllegalArgumentException("Unknown event direction '" + direction
                    + "' — expected 'request' or 'response'");
        }
        JSONObject side = event.optJSONObject(direction);
        Assert.assertNotNull(side, "The Moesif event carried no '" + direction + "' object; event=" + event);
        return side;
    }

    /** Header map of one side of a stored event; an absent map reads as empty so a negative can still be stated. */
    private static JSONObject eventHeaders(String eventKey, String direction) {

        JSONObject headers = eventSide(eventKey, direction).optJSONObject("headers");
        return headers == null ? new JSONObject() : headers;
    }
}
