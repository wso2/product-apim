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

import io.cucumber.core.options.CurlOption;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Gateway runtime-invocation steps. Invokes deployed APIs through the block's gateway (REST/SOAP/SSE, by access
 * token or API key, with optional retry-until-status for eventual consistency), plus the OpenID userinfo
 * endpoint. The tenant context segment of the invocation URL is derived from the scenario's acting actor
 * ({@link Identity#actingActor()}) rather than the retired {@code currentTenant} pointer.
 */
public class APIInvocationSteps {

    private static final Log log = LogFactory.getLog(APIInvocationSteps.class);

    /** Context key under which every invocation publishes its response for the following assertion step. */
    private static final String HTTP_RESPONSE_KEY = "httpResponse";
    /** Container port of the node {@code sse-emitter} backend (published to the host — see {@code NodeAppServer}). */
    private static final int SSE_EMITTER_PORT = 3021;
    /** The SSE media type — what an SSE client asks for and what the emitter streams back. */
    private static final String SSE_CONTENT_TYPE = "text/event-stream";
    /**
     * Sub-path appended to an SSE API's gateway context. The API's single operation is {@code /*}, and synapse
     * renders that as {@code url-mapping="/*"} — which needs a non-empty sub-path to match, so the bare context
     * would 404. The emitter serves the stream on every non-reserved path, so the segment itself is arbitrary
     * (legacy used its servlet's {@code /memory} mapping).
     */
    private static final String SSE_TOPIC_PATH = "/events";
    /** Context key holding the number of SSE events the last SSE invocation received through the gateway. */
    private static final String SSE_EVENTS_RECEIVED_KEY = "sseEventsReceived";

    /**
     * Inter-frame cadence the emitter is asked for on a QUOTA arc ({@code delayMs}). One second per event, so a
     * handful of events already spans several seconds — long enough for the gateway's event-quota decision to take
     * effect mid-stream. The emitter's own default (50ms) would finish the whole stream inside the decision's
     * propagation window, which would read as "not throttled" for a timing reason rather than a product one.
     */
    private static final int THROTTLED_SSE_DELAY_MILLIS = 1000;

    /**
     * The single low-level invocation primitive every step funnels through. It CLEARS any prior
     * {@code httpResponse} first, so that if the call throws (a transient connectivity error during gateway
     * warm-up, or a bad key/token) the context is left WITHOUT a stale response — a later
     * "response status code should be N" assertion then cannot be satisfied by a leftover value from an
     * earlier step. On a real response it publishes it as {@code httpResponse} and also returns it, so
     * retry loops can hold the last real response locally without re-reading shared context.
     */
    private HttpResponse execute(CurlOption.HttpMethod method, String endpointUrl, Map<String, String> headers,
                                 String payload, String contentType, boolean rawGet) throws IOException {

        TestContext.remove(HTTP_RESPONSE_KEY);
        SimpleHTTPClient client = SimpleHTTPClient.getInstance();
        HttpResponse response;
        if (rawGet) {
            // GET with the client's URI normalization DISABLED, so a percent-encoded path segment reaches the
            // gateway verbatim; method/payload/contentType are unused on this path.
            response = client.doGetRaw(endpointUrl, headers);
        } else {
            switch (method) {
                case GET:
                    response = client.doGet(endpointUrl, headers);
                    break;
                case DELETE:
                    response = client.doDelete(endpointUrl, headers);
                    break;
                case POST:
                    response = client.doPost(endpointUrl, headers, payload, contentType);
                    break;
                case PUT:
                    response = client.doPut(endpointUrl, headers, payload, contentType);
                    break;
                case PATCH:
                    response = client.doPatch(endpointUrl, headers, payload, contentType);
                    break;
                case HEAD:
                    response = client.doHead(endpointUrl, headers);
                    break;
                case OPTIONS:
                    response = client.doOptions(endpointUrl, headers);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported HTTP method for invocation: " + method);
            }
        }
        TestContext.set(HTTP_RESPONSE_KEY, response);
        return response;
    }

    /** {@link #execute(CurlOption.HttpMethod, String, Map, String, String, boolean)} for a normalized request. */
    private HttpResponse execute(CurlOption.HttpMethod method, String endpointUrl, Map<String, String> headers,
                                 String payload, String contentType) throws IOException {
        return execute(method, endpointUrl, headers, payload, contentType, false);
    }

    /** {@link #execute(CurlOption.HttpMethod, String, Map, String, String)} defaulting to a JSON content type. */
    private HttpResponse execute(CurlOption.HttpMethod method, String endpointUrl, Map<String, String> headers,
                                 String payload) throws IOException {
        return execute(method, endpointUrl, headers, payload, Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * A large request body must NOT bypass gateway authentication: a POST of a {@code sizeKb}-KB body with an invalid
     * bearer token is rejected, regardless of size. Ports InvalidAuthTokenLargePayloadTestCase — which uploads 1KB /
     * 100KB / 1MB files with a bad token and asserts the upload is rejected. Two outcomes both satisfy the security
     * property (the request never reaches the backend): the client captures a 401 response, OR the gateway rejects
     * auth and closes the connection before consuming the body (a reset/broken pipe, exactly what the legacy catches).
     * Only "connection refused"/"no route" (gateway down) is a genuine failure. A single shot (no retry) so a
     * connection reset is not masked as a timeout.
     */
    @When("I invoke the API at gateway context {string} with method {string} using access token {string} and a {int} KB payload expecting authentication rejection")
    public void invokeLargePayloadExpectAuthRejection(String context, String httpMethod, String token, int sizeKb)
            throws Exception {
        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String actualToken = TestContext.resolve(token).toString();
        String endpointUrl = Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualToken);
        char[] buf = new char[sizeKb * 1024];
        java.util.Arrays.fill(buf, 'A');
        try {
            HttpResponse response = execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl,
                    headers, new String(buf), Constants.CONTENT_TYPES.APPLICATION_JSON);
            Assert.assertEquals(response.getResponseCode(), 401,
                    "A " + sizeKb + "KB payload with an invalid token must be rejected with 401, got "
                            + response.getResponseCode() + ": " + response.getData());
        } catch (IOException e) {
            String msg = String.valueOf(e.getMessage()).toLowerCase();
            Assert.assertFalse(msg.contains("connection refused") || msg.contains("no route to host"),
                    "Gateway unreachable — not an auth rejection: " + e.getMessage());
        }
    }

    /**
     * Fails with a clear message if a retried invocation never reached the expected status before the deadline.
     * Asserts on the last response the loop actually captured (not the shared context key), so a persistent
     * throw ({@code last == null}) is reported distinctly from a persistent wrong status.
     */
    private void assertReachedExpectedStatus(HttpResponse last, int expectedStatus) {
        Assert.assertNotNull(last, "No response was captured while waiting for status " + expectedStatus
                + " within the deadline — every attempt threw (gateway unreachable, or a bad token/key context key).");
        Assert.assertEquals(last.getResponseCode(), expectedStatus,
                "API did not return " + expectedStatus + " within the deadline; last response: " + last.getData());
    }

    /**
     * THE retry envelope for the invocation steps here: attempts until the expected status or the shared ceiling,
     * then asserts. Delegates the loop to {@link Utils#retryUntil} so no step hand-rolls the deadline, pacing and
     * exception policy again — 18 near-identical loops used to, and one had already drifted off the shared ceiling
     * onto a hardcoded window.
     *
     * <p>Its second job is to be this family's ONE observable failure point. An unexpected 401 is reported once
     * through {@link Utils#logAuthRejection} — naming the credential and its {@code jti} — instead of surfacing as
     * an opaque status mismatch after a whole window has burned. A credential rejected on every attempt cannot
     * recover by being replayed (see that method), so this diagnostic is the difference between pinpointing the
     * cause from CI logs and re-running the investigation.
     *
     * @param what                 what is being invoked, for the diagnostic (typically the resolved context)
     * @param credentialContextKey context key of the token/API key presented, or {@code null} when unauthenticated
     */
    private void invokeUntilStatus(String what, String credentialContextKey, int expectedStatus, int timeoutSeconds,
                                   Utils.RetryAttempt<HttpResponse> attempt) throws Exception {

        long started = System.currentTimeMillis();
        HttpResponse last = Utils.retryUntil(timeoutSeconds * 1000L, attempt,
                response -> response.getResponseCode() == expectedStatus);
        if (last != null && last.getResponseCode() == 401 && expectedStatus != 401) {
            Utils.logAuthRejection(what, credentialContextKey, credentialForDiagnostic(credentialContextKey),
                    last.getResponseCode(), last.getData(), System.currentTimeMillis() - started);
        }
        assertReachedExpectedStatus(last, expectedStatus);
    }

    /** Resolves a credential for the diagnostic ONLY — never throws, because a diagnostic must not fail a test. */
    private static String credentialForDiagnostic(String contextKey) {
        if (contextKey == null) {
            return null;
        }
        try {
            return TestContext.resolve(contextKey).toString();
        } catch (RuntimeException unresolvable) {
            return null;
        }
    }

    /**
     * Invokes a deployed API a FIXED number of times back-to-back and asserts every response is 200 — used to
     * prove a throttle limit is at least {@code times} per minute (i.e. a burst of {@code times} calls does NOT
     * trip 429). Distinguishes one application-throttle tier from another: pick {@code times} above the LOW
     * tier's limit but at/below the HIGH tier's, so the burst succeeds only when the higher tier is in effect.
     * Each individual call is retried briefly on a transient warmup IOException (the same connectivity guard as
     * the until-status variants), so a still-warming gateway route does not falsely fail the burst.
     */
    @When("I invoke the API at gateway context {string} with method {string} using access token {string} and payload {string} {int} times expecting status 200")
    public void invokeApiByContextNTimesExpecting200(String context, String httpMethod, String accessToken,
                                                     String payload, int times) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        for (int i = 1; i <= times; i++) {
            // Each call is retried only until it COMPLETES (any status); the burst's assertion is 200 below.
            HttpResponse response = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                    () -> invokeApiByContext(resolvedContext, httpMethod, accessToken, payload),
                    completed -> true);
            Requests.publishPollResult(response);
            Assert.assertNotNull(response, "Invocation " + i + " of " + times + " never completed (gateway "
                    + "unreachable within the warmup window).");
            Assert.assertEquals(response.getResponseCode(), 200, "Invocation " + i + " of " + times
                    + " was not 200 (throttle tier change did not raise the limit as expected); last response: "
                    + response.getData());
        }
    }

    /**
     * Invokes a load-balanced API {@code times} times back-to-back and asserts the responses ROUND-ROBIN across
     * the given comma-separated backend markers (a distinguishing substring of each member's body, e.g.
     * {@code File 1,File 2,File 3}). Asserts three things, in increasing strength:
     * <ol>
     *   <li>every call is 200 and its body matches EXACTLY ONE marker (an ambiguous marker set is a test bug,
     *       not a product result, so it fails loudly);</li>
     *   <li>every marker is reached at least once — the load really is distributed across all members;</li>
     *   <li>every window of N CONSECUTIVE responses (N = marker count) contains all N distinct markers.</li>
     * </ol>
     * (3) is the order property, and it is deliberately phrased to be START-OFFSET INDEPENDENT: the gateway's
     * round-robin cursor position when the counted calls begin is not knowable (a preceding readiness poll has
     * already advanced it), so asserting a fixed sequence would be flaky. The sliding-window property holds for
     * ANY rotation of a round-robin sequence, yet fails immediately if the distribution degenerates to one or two
     * endpoints or repeats a member within a cycle — which "all members are reachable" alone does not catch.
     * Each call is retried only until it COMPLETES (transient warm-up IOException), never until it matches, so a
     * wrong body is never polled away.
     */
    @When("I invoke the API at gateway context {string} with method {string} using access token {string} {int} times round-robin across backend markers {string}")
    public void invokeApiByContextRoundRobin(String context, String httpMethod, String accessToken, int times,
                                             String markersCsv) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String[] markers = Utils.resolveContextPlaceholders(markersCsv).split("\\s*,\\s*");
        Assert.assertTrue(markers.length >= 2, "A round-robin assertion needs at least two backend markers, got: "
                + markersCsv);
        Assert.assertTrue(times >= markers.length, "Need at least as many invocations (" + times + ") as markers ("
                + markers.length + ") to observe a full round-robin cycle.");

        String[] observed = new String[times];
        for (int i = 0; i < times; i++) {
            final int call = i + 1;
            HttpResponse response = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                    () -> invokeApiByContext(resolvedContext, httpMethod, accessToken, ""),
                    completed -> true);
            Assert.assertNotNull(response, "Invocation " + call + " of " + times + " never completed (gateway "
                    + "unreachable within the warmup window).");
            Assert.assertEquals(response.getResponseCode(), 200, "Invocation " + call + " of " + times
                    + " was not 200; body: " + response.getData());
            String body = response.getData() == null ? "" : response.getData();
            String matched = null;
            for (String marker : markers) {
                if (body.contains(marker)) {
                    Assert.assertNull(matched, "Invocation " + call + " body matches MORE THAN ONE backend marker ('"
                            + matched + "' and '" + marker + "') — the markers are not mutually exclusive: " + body);
                    matched = marker;
                }
            }
            Assert.assertNotNull(matched, "Invocation " + call + " of " + times + " reached no known backend; "
                    + "expected the body to carry one of " + String.join(", ", markers) + " but got: " + body);
            observed[i] = matched;
        }

        List<String> trail = Arrays.asList(observed);
        String trace = String.join(" -> ", observed);
        for (String marker : markers) {
            Assert.assertTrue(trail.contains(marker),
                    "Backend '" + marker + "' was never reached over " + times + " calls — the load is NOT "
                            + "distributed across every member. Observed: " + trace);
        }
        int window = markers.length;
        for (int start = 0; start + window <= times; start++) {
            Set<String> inWindow = new HashSet<>(trail.subList(start, start + window));
            Assert.assertEquals(inWindow.size(), window, "Round-robin broke down: calls " + (start + 1) + ".."
                    + (start + window) + " did not reach " + window + " DISTINCT backends. Observed: " + trace);
        }
    }

    /**
     * Invokes a deployed API using an access token, addressing it by its full gateway context path (the
     * {@code context} field returned by the Publisher API, which already carries the {@code /t/<tenant>}
     * prefix for tenant APIs) — so no tenant prefix is added here. Use this when the path was captured from
     * an API's {@code context}; use the {@code at path} variant when supplying a bare super-tenant path.
     */
    @When("I invoke the API at gateway context {string} with method {string} using access token {string} and payload {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiByContextUntilStatus(String context, String httpMethod, String accessToken, String payload,
                                              int expectedStatus, int timeoutSeconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        // The envelope floors the wait at the global propagation window: a freshly published API's gateway route
        // can take longer than a short per-step value to become routable, especially under load. The feature's
        // value can still request MORE than the floor.
        invokeUntilStatus(resolvedContext, accessToken, expectedStatus, timeoutSeconds,
                () -> invokeApiByContext(resolvedContext, httpMethod, accessToken, payload));
    }

    /**
     * SETTLE-WINDOW counterpart of the until-status invoke: re-invokes throughout the window and asserts EVERY
     * response is {@code expectedStatus}, failing on the first deviation. An until-status poll cannot express this
     * — it stops at the first matching response, so a status that is merely *transiently* right (a backend still
     * quiescing, an endpoint suspension about to lapse) passes it. Where the claim is "this status is now the
     * ENFORCED steady state", the assertion has to survive time.
     *
     * <p>Used by the endpoint-certificate arc after the certificate is deleted: legacy re-probed 3 times, 2s apart,
     * to be sure the post-delete 500 was enforcement and not a transient. Paces with {@link Utils#pollPause} (never
     * a bare {@code Thread.sleep} — §4/§15) and publishes each response, so the feature may also assert on the last.
     * Probes at both ENDS of the window inclusive (t=0 and t={@code seconds}), so the asserted span is the full
     * window the step name promises rather than one pause-interval short of it.
     */
    @When("I invoke the API at gateway context {string} with method {string} using access token {string} and the response status code should remain {int} for {int} seconds")
    public void invokeApiByContextStatusShouldRemain(String context, String httpMethod, String accessToken,
                                                     int expectedStatus, int seconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        long pollStart = System.currentTimeMillis();
        long end = pollStart + seconds * 1000L;
        int probe = 0;
        while (true) {
            probe++;
            // A transient connectivity blip during the window is not a status deviation, so each probe is retried
            // only until it COMPLETES (any status) — the same warm-up guard the burst steps use.
            HttpResponse response = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                    () -> invokeApiByContext(resolvedContext, httpMethod, accessToken, ""),
                    completed -> true);
            Assert.assertNotNull(response, "Probe " + probe + " of the settle window never completed (gateway "
                    + "unreachable).");
            Assert.assertEquals(response.getResponseCode(), expectedStatus, "Probe " + probe + " of the "
                    + seconds + "s settle window returned " + response.getResponseCode() + " instead of "
                    + expectedStatus + " — the status is NOT the enforced steady state; body: "
                    + response.getData());
            // Deadline checked BEFORE pausing, so the pause only ever precedes a probe that will actually run.
            // Pausing first (a do/while on the same deadline) both idled out the tail of the window and left the
            // LAST probe one interval short of it — a "remains for 10s" claim verified only to t=8s.
            if (System.currentTimeMillis() >= end) {
                break;
            }
            Utils.pollPause(pollStart, Constants.RETRY_INTERVAL_TIME);
        }
    }

    /**
     * Invokes a deployed API at its full gateway context using a RAW (un-normalized) path, so a percent-encoded
     * segment (e.g. {@code %28}/{@code %29}) is sent to the gateway verbatim rather than being decoded by the
     * HTTP client. GET only. Needed to test the gateway's routing of an encoded URI path segment — the default
     * invoke lets Apache HttpClient normalize/decode the path before it reaches the gateway. Retries until the
     * expected status (transient IOExceptions only), then asserts.
     */
    @When("I invoke the API at raw gateway context {string} using access token {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiByRawContextUntilStatus(String context, String accessToken, int expectedStatus,
                                                 int timeoutSeconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        invokeUntilStatus(resolvedContext, accessToken, expectedStatus, timeoutSeconds,
                () -> invokeApiByRawContext(resolvedContext, accessToken));
    }

    /** GET a full gateway context with the raw (un-normalized) path; publishes the response to {@code httpResponse}. */
    private HttpResponse invokeApiByRawContext(String resolvedContext, String accessToken) throws IOException {

        String actualAccessToken = TestContext.resolve(accessToken).toString();
        // Join base + context with exactly one slash. The default invoke relies on the client's URI
        // normalization to collapse a "//", but doGetRaw disables normalization, so a double slash would reach
        // the gateway verbatim and be rejected as "Invalid URL".
        String base = Utils.getBaseGatewayUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String endpointUrl = base + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualAccessToken);
        return execute(CurlOption.HttpMethod.GET, endpointUrl, headers, "",
                Constants.CONTENT_TYPES.APPLICATION_JSON, true);
    }

    /**
     * Invokes a deployed API at its full gateway context with NO Authorization header, retrying until the
     * expected status. Used for prototyped APIs (invocable without a subscription/token → 200) and as the
     * negative for a normal secured API (no token → 401).
     */
    @When("I invoke the API at gateway context {string} with method {string} without authentication until response status code becomes {int} within {int} seconds")
    public void invokeApiByContextNoAuthUntilStatus(String context, String httpMethod, int expectedStatus,
                                                     int timeoutSeconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String endpointUrl = Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        invokeUntilStatus(resolvedContext, null, expectedStatus, timeoutSeconds,
                () -> execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl,
                        new HashMap<>(), ""));
    }

    /**
     * Sends a CORS pre-flight: an OPTIONS request carrying the {@code Origin} and {@code Access-Control-Request-Method}
     * headers (the two headers a browser sends before a cross-origin call), with NO Authorization header, retrying
     * until the expected status. The gateway's CORS handler matches the resource by the requested method and either
     * answers the pre-flight itself (CORS enabled → 200 with the Access-Control-Allow-* headers) or, if the resource
     * declares an explicit OPTIONS method, routes it to the backend. Without the Access-Control-Request-Method header
     * the handler finds no acceptable resource and returns 405, so a real pre-flight MUST send it.
     */
    @When("I send a CORS preflight to gateway context {string} with origin {string} and request method {string} until response status code becomes {int} within {int} seconds")
    public void sendCorsPreflightUntilStatus(String context, String origin, String requestMethod, int expectedStatus,
                                             int timeoutSeconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String endpointUrl = Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        Map<String, String> headers = new HashMap<>();
        headers.put("Origin", origin);
        headers.put("Access-Control-Request-Method", requestMethod);
        invokeUntilStatus(resolvedContext, null, expectedStatus, timeoutSeconds,
                () -> execute(CurlOption.HttpMethod.OPTIONS, endpointUrl, new HashMap<>(headers), ""));
    }

    /**
     * Pre-flight variant of {@link #sendCorsPreflightUntilStatus} that retries until a given RESPONSE HEADER
     * carries an expected value, not merely until a status. Needed when a CORS config CHANGE is under test: the
     * pre-flight answers 200 both before and after the change, so an until-status poll would return the STALE
     * response the instant it fired and the following assertions would race the gateway's artifact propagation.
     * Accepting on the header value is the only condition that actually observes the new config arriving. The
     * header lookup is case-insensitive and the last response is published, so the feature re-asserts it (and every
     * other header of the same response) itself.
     */
    @When("I send a CORS preflight to gateway context {string} with origin {string} and request method {string} until response header {string} becomes {string} within {int} seconds")
    public void sendCorsPreflightUntilHeader(String context, String origin, String requestMethod, String headerName,
                                             String expectedValue, int timeoutSeconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String expected = Utils.resolveContextPlaceholders(expectedValue);
        String endpointUrl = Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        Map<String, String> headers = new HashMap<>();
        headers.put("Origin", origin);
        headers.put("Access-Control-Request-Method", requestMethod);
        HttpResponse last = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> execute(CurlOption.HttpMethod.OPTIONS, endpointUrl, new HashMap<>(headers), ""),
                response -> response.getResponseCode() == 200
                        && expected.equals(headerValueIgnoringCase(response, headerName)));
        Requests.publishPollResult(last);
        assertReachedExpectedStatus(last, 200);
        Assert.assertEquals(headerValueIgnoringCase(last, headerName), expected,
                "Pre-flight response header '" + headerName + "' never became '" + expected
                        + "' within the deadline (the CORS config change did not reach the gateway).");
    }

    /** Case-insensitive response-header lookup (null when absent) — HTTP header names are not case-sensitive. */
    private static String headerValueIgnoringCase(HttpResponse response, String headerName) {
        if (response == null || response.getHeaders() == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(headerName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Invokes a deployed API at its full gateway context, sending the bearer token in a CUSTOM authorization
     * header instead of the standard {@code Authorization} one, retrying until the expected status. Used by the
     * custom-auth-header feature, whose container sets {@code [apim.oauth_config] auth_header} so the gateway
     * reads the token from that header.
     */
    @When("I invoke the API at gateway context {string} with method {string} using access token {string} in header {string} and payload {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiByContextInHeaderUntilStatus(String context, String httpMethod, String accessToken,
                                                      String headerName, String payload, int expectedStatus,
                                                      int timeoutSeconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        invokeUntilStatus(resolvedContext, accessToken, expectedStatus, timeoutSeconds,
                () -> invokeApiByContext(resolvedContext, httpMethod, accessToken, payload, headerName));
    }

    /**
     * Invokes a deployed API at its full gateway context with an extra CUSTOM request header (name + value)
     * alongside the bearer token, retrying until the expected status. Needed by the gateway schema-validation
     * test, where a resource declares a REQUIRED request header (X-Request-ID): omitting it is rejected by the
     * gateway's request schema validation (400), while sending it lets the call through (200). Distinct from the
     * {@code in header} variant, which places the TOKEN in a custom header rather than adding an arbitrary one.
     *
     * <p>{@code {{contextKey}}} placeholders in the header VALUE are resolved, so the value can be something a
     * previous step computed — e.g. the base64 certificate sent in {@code X-WSO2-CLIENT-CERTIFICATE} to prove that
     * header cannot substitute for a real mutual-SSL handshake. A literal without {@code {{}}} passes through
     * unchanged.
     */
    @When("I invoke the API at gateway context {string} with method {string} using access token {string} and payload {string} with request header {string} set to {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiByContextWithHeaderUntilStatus(String context, String httpMethod, String accessToken,
                                                        String payload, String headerName, String headerValue,
                                                        int expectedStatus, int timeoutSeconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String resolvedHeaderValue = Utils.resolveContextPlaceholders(headerValue);
        invokeUntilStatus(resolvedContext, accessToken, expectedStatus, timeoutSeconds, () -> {
            String actualAccessToken = TestContext.resolve(accessToken).toString();
            String actualPayload = (payload == null || payload.isEmpty())
                    ? null : TestContext.resolve(payload).toString();
            String endpointUrl = Utils.getBaseGatewayUrl()
                    + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + actualAccessToken);
            headers.put(headerName, resolvedHeaderValue);
            return execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers,
                    actualPayload);
        });
    }

    /**
     * Invokes a deployed API at its full gateway context sending the body with an EXPLICIT Content-Type (rather
     * than the default application/json), retrying until the expected status. Needed to drive the gateway's
     * message builder for a given content type — e.g. POSTing a malformed XML body as {@code application/xml}
     * so the gateway attempts to build the message and returns a clean error rather than crashing
     * (MalformedRequestTest). The content type is applied to the request entity, so the backend/gateway sees the
     * declared type.
     */
    @When("I invoke the API at gateway context {string} with method {string} using access token {string} and payload {string} with content type {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiByContextWithContentTypeUntilStatus(String context, String httpMethod, String accessToken,
                                                             String payload, String contentType, int expectedStatus,
                                                             int timeoutSeconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        invokeUntilStatus(resolvedContext, accessToken, expectedStatus, timeoutSeconds, () -> {
            String actualAccessToken = TestContext.resolve(accessToken).toString();
            String actualPayload = (payload == null || payload.isEmpty())
                    ? "" : TestContext.resolve(payload).toString();
            String endpointUrl = Utils.getBaseGatewayUrl()
                    + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + actualAccessToken);
            return execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers,
                    actualPayload, contentType);
        });
    }

    /**
     * Invokes a deployed API presenting an API key in a NAMED request header (rather than a bearer token),
     * retrying until the expected status. Used to verify api_key security accepts the key only in the API's
     * configured header (e.g. a custom {@code Custom-ApiKey-Header}) and rejects it in the default {@code ApiKey}.
     */
    @When("I invoke the API at gateway context {string} with method {string} using api key {string} in header {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiByContextUsingKeyInHeaderUntilStatus(String context, String httpMethod, String apiKey,
                                                              String headerName, int expectedStatus,
                                                              int timeoutSeconds) throws Exception {

        invokeCredentialInHeaderUntilStatus(context, httpMethod, apiKey, headerName, expectedStatus, timeoutSeconds);
    }

    /**
     * Invokes a deployed API presenting a credential VERBATIM (no {@code Bearer } prefix, no scheme) in a named
     * request header, retrying until the expected status. This is the CROSS-CREDENTIAL step: it presents a
     * credential of one kind in the header belonging to another kind — an OAuth2 access token in {@code apikey},
     * an internal key in {@code apikey}, an api key in {@code Internal-Key}, and so on — which is exactly the
     * confusion the gateway must refuse. It shares the api-key-in-header implementation (same wire shape: an
     * opaque credential string in an arbitrary header); the separate phrasing exists so a feature line states
     * WHICH credential kind is being misused rather than calling an OAuth token an "api key".
     *
     * <p>The expected status is stated per case by the caller and is NOT assumed uniform: these negatives differ
     * by which authenticator claims the request (see CLAUDE.md §12) and each scenario pins its own exact value.
     */
    @When("I invoke the API at gateway context {string} with method {string} presenting credential {string} verbatim in header {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiByContextWithVerbatimCredentialUntilStatus(String context, String httpMethod,
                                                                    String credential, String headerName,
                                                                    int expectedStatus, int timeoutSeconds)
            throws Exception {

        invokeCredentialInHeaderUntilStatus(context, httpMethod, credential, headerName, expectedStatus,
                timeoutSeconds);
    }

    /** Shared retry-until-status loop placing a credential verbatim in a named request header. */
    private void invokeCredentialInHeaderUntilStatus(String context, String httpMethod, String credentialKey,
                                                     String headerName, int expectedStatus, int timeoutSeconds)
            throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        invokeUntilStatus(resolvedContext, credentialKey, expectedStatus, timeoutSeconds, () -> {
            String actualCredential = TestContext.resolve(credentialKey).toString();
            String endpointUrl = Utils.getBaseGatewayUrl()
                    + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");
            headers.put(headerName, actualCredential);
            return execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers, "");
        });
    }

    /**
     * Invokes a deployed API by its full gateway context, retrying until the response is a 200 whose body
     * CONTAINS the given marker (or the deadline elapses). Used where two responses share a status code and
     * only the body distinguishes them — e.g. default-version routing, where a versionless context returns 200
     * from whichever version is default and the body reveals which backend served it. The last response is left
     * in context, so a following {@code The response should contain} can re-assert it.
     */
    @When("I invoke the API at gateway context {string} with method {string} using access token {string} and payload {string} until response body contains {string} within {int} seconds")
    public void invokeApiByContextUntilBodyContains(String context, String httpMethod, String accessToken,
                                                    String payload, String expectedBody, int timeoutSeconds)
            throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String marker = Utils.resolveContextPlaceholders(expectedBody);
        long started = System.currentTimeMillis();
        // Accept on body content, not just status — so this one keeps its own accept condition and assertions
        // rather than going through invokeUntilStatus.
        HttpResponse last = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> invokeApiByContext(resolvedContext, httpMethod, accessToken, payload),
                response -> response.getResponseCode() == 200 && response.getData() != null
                        && response.getData().contains(marker));
        Requests.publishPollResult(last);
        if (last != null && last.getResponseCode() == 401) {
            Utils.logAuthRejection(resolvedContext, accessToken, credentialForDiagnostic(accessToken),
                    last.getResponseCode(), last.getData(), System.currentTimeMillis() - started);
        }
        assertReachedExpectedStatus(last, 200);
        String body = last.getData();
        Assert.assertTrue(body != null && body.contains(marker),
                "Response body was missing/null or never contained '" + marker + "' within the deadline; last response: "
                        + body);
    }

    /**
     * The negative counterpart of the variant above: retries until the response is a 200 whose body NO LONGER
     * contains the marker. Needed wherever a policy/config is REMOVED and the effect must disappear — there the
     * status is 200 both before and after, so an until-status-200 poll is satisfied by the still-live old
     * sequence and the absence assertion reads the pre-change response (a real CI failure at
     * mediation_policies.feature:374). Management-plane deployment status cannot substitute: it flips when the
     * revision row is written, which is not when synapse has hot-swapped. Polling the data plane for the absence
     * is the only gate that distinguishes the two states.
     */
    @When("I invoke the API at gateway context {string} with method {string} using access token {string} and payload {string} until response body no longer contains {string} within {int} seconds")
    public void invokeApiByContextUntilBodyNoLongerContains(String context, String httpMethod, String accessToken,
                                                            String payload, String absentBody, int timeoutSeconds)
            throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String marker = Utils.resolveContextPlaceholders(absentBody);
        long started = System.currentTimeMillis();
        HttpResponse last = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> invokeApiByContext(resolvedContext, httpMethod, accessToken, payload),
                response -> response.getResponseCode() == 200 && response.getData() != null
                        && !response.getData().contains(marker));
        Requests.publishPollResult(last);
        if (last != null && last.getResponseCode() == 401) {
            Utils.logAuthRejection(resolvedContext, accessToken, credentialForDiagnostic(accessToken),
                    last.getResponseCode(), last.getData(), System.currentTimeMillis() - started);
        }
        assertReachedExpectedStatus(last, 200);
        String body = last.getData();
        Assert.assertFalse(body == null || body.contains(marker),
                "Response body was null or still contained '" + marker + "' when the deadline elapsed, so the "
                        + "change never reached the gateway; last response: " + body);
    }

    /**
     * Invokes a deployed API at its full gateway context with an EXPLICIT Content-Type, retrying until the
     * response is a 200 whose body CONTAINS the given marker. The combination the two single-axis variants
     * cannot express: a RESPONSE-flow transformation policy is only exercised when the request carries the
     * right content type, and its effect shows up in the BODY while the status stays 200 throughout — so a
     * status-based retry cannot wait for a redeployed policy to take effect.
     */
    @When("I invoke the API at gateway context {string} with method {string} using access token {string} and payload {string} with content type {string} until response body contains {string} within {int} seconds")
    public void invokeApiByContextWithContentTypeUntilBodyContains(String context, String httpMethod,
                                                                   String accessToken, String payload,
                                                                   String contentType, String expectedBody,
                                                                   int timeoutSeconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String marker = Utils.resolveContextPlaceholders(expectedBody);
        long started = System.currentTimeMillis();
        HttpResponse last = Utils.retryUntil(timeoutSeconds * 1000L, () -> {
            String actualAccessToken = TestContext.resolve(accessToken).toString();
            String actualPayload = (payload == null || payload.isEmpty())
                    ? "" : TestContext.resolve(payload).toString();
            String endpointUrl = Utils.getBaseGatewayUrl()
                    + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + actualAccessToken);
            return execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers,
                    actualPayload, contentType);
        }, response -> response.getResponseCode() == 200 && response.getData() != null
                && response.getData().contains(marker));
        Requests.publishPollResult(last);
        if (last != null && last.getResponseCode() == 401) {
            Utils.logAuthRejection(resolvedContext, accessToken, credentialForDiagnostic(accessToken),
                    last.getResponseCode(), last.getData(), System.currentTimeMillis() - started);
        }
        assertReachedExpectedStatus(last, 200);
        String body = last.getData();
        Assert.assertTrue(body != null && body.contains(marker),
                "Response body was missing/null or never contained '" + marker + "' within the deadline; last response: "
                        + body);
    }

    /** Single invocation addressing the API by its full gateway context path (no tenant prefixing). */
    private HttpResponse invokeApiByContext(String resolvedContext, String httpMethod, String accessToken,
                                            String payload) throws IOException {
        return invokeApiByContext(resolvedContext, httpMethod, accessToken, payload, "Authorization");
    }

    /** As above, but places the bearer token in the given authorization header name. */
    private HttpResponse invokeApiByContext(String resolvedContext, String httpMethod, String accessToken,
                                            String payload, String authHeaderName) throws IOException {

        String actualAccessToken = TestContext.resolve(accessToken).toString();
        String actualPayload = (payload == null || payload.isBlank()) ? "" : TestContext.resolve(payload).toString();
        // The context already carries any /t/<tenant> prefix, so append it directly to the gateway base URL.
        String endpointUrl = Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;

        Map<String, String> headers = new HashMap<>();
        headers.put(authHeaderName, "Bearer " + actualAccessToken);

        return execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers, actualPayload);
    }

    /**
     * Invokes an API endpoint using an OAuth2 access token for authentication.
     * Supports GET, POST, PUT, and DELETE HTTP methods. The access token is resolved from the test context
     * and included in the Authorization header as a Bearer token.
     *
     * @param path The API resource path to invoke
     * @param httpMethod The HTTP method to use (GET, POST, PUT, or DELETE)
     * @param accessToken Context key containing the access token to use for authentication
     * @param payload Context key containing the request payload
     */
    @When("I invoke the API resource at path {string} with method {string} using access token {string} and payload {string}")
    public HttpResponse invokeApiUsingAccessToken(String path, String httpMethod, String accessToken, String payload)
            throws IOException {

        String actualAccessToken = TestContext.resolve(accessToken).toString();
        String actualPayload = (payload == null || payload.isBlank()) ? "" : TestContext.resolve(payload).toString();
        // Resolve {{contextKey}} placeholders in the path so the invocation can target a uniquely-generated
        // API context (names/contexts are randomized by ${UNIQUE:...}), e.g. "{{apiContext}}/1.0.0/...".
        String resolvedPath = Utils.resolveContextPlaceholders(path);
        String endpointUrl = Utils.getAPIInvocationURL(Utils.getBaseGatewayUrl(), resolvedPath, Identity.actingTenantDomain());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualAccessToken);

        return execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers, actualPayload);
    }

    /**
     * Invokes an API endpoint using an API Key for authentication.
     * Supports GET, POST, PUT, and DELETE HTTP methods. The API key is resolved from the test context
     * and included in the ApiKey header.
     *
     * @param path The API resource path to invoke
     * @param httpMethod The HTTP method to use (GET, POST, PUT, or DELETE)
     * @param apikey Context key containing the API key to use for authentication
     */
    @When("I invoke the API resource at path {string} with method {string} using api key {string}")
    public HttpResponse invokeApiUsingKey(String path, String httpMethod, String apikey) throws IOException {

        String endpointUrl = Utils.getAPIInvocationURL(Utils.getBaseGatewayUrl(),
                Utils.resolveContextPlaceholders(path), Identity.actingTenantDomain());
        return invokeWithApiKey(endpointUrl, httpMethod, apikey);
    }

    /**
     * Invokes an API using an API key, addressing it by its full gateway context path (the {@code context}
     * field returned by the Publisher API, which already carries the {@code /t/<tenant>} prefix for tenant
     * APIs) — so no tenant prefix is added here. Use this for the tenant rows of a Scenario Outline; the
     * {@code at path} variant double-prefixes a tenant context.
     */
    @When("I invoke the API at gateway context {string} with method {string} using api key {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiByContextUsingKeyUntilStatus(String context, String httpMethod, String apikey,
                                                      int expectedStatus, int timeoutSeconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String endpointUrl = Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        invokeUntilStatus(resolvedContext, apikey, expectedStatus, timeoutSeconds,
                () -> invokeWithApiKey(endpointUrl, httpMethod, apikey));
    }

    /**
     * API-key invocation with a request PAYLOAD (e.g. an AI API's {@code POST /chat/completions}), retrying
     * until the expected status. The api-key {@code ApiKey} header carries auth; the payload is sent as the
     * body. Needed because the no-payload api-key variant can't drive POST bodies.
     */
    @When("I invoke the API at gateway context {string} with method {string} using api key {string} and payload {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiByContextUsingKeyAndPayloadUntilStatus(String context, String httpMethod, String apikey,
                                                                String payload, int expectedStatus,
                                                                int timeoutSeconds) throws Exception {
        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String endpointUrl = Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        invokeUntilStatus(resolvedContext, apikey, expectedStatus, timeoutSeconds, () -> {
            String actualKey = TestContext.resolve(apikey).toString();
            String actualPayload = (payload == null || payload.isEmpty())
                    ? "" : TestContext.resolve(payload).toString();
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");
            headers.put("ApiKey", actualKey);
            return execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers,
                    actualPayload);
        });
    }

    /**
     * API-key invocation presenting an {@code X-Forwarded-For} header, retrying until the expected status. For an
     * API key generated with a {@code permittedIP} restriction, the REST passthrough derives the client IP from
     * {@code X-Forwarded-For}, so a matching XFF is authorised (200) and a non-matching one is rejected (403).
     * (Verified on a standalone server.)
     */
    @When("I invoke the API at gateway context {string} with method {string} using api key {string} and forwarded-for {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiByContextUsingKeyAndXffUntilStatus(String context, String httpMethod, String apikey,
                                                            String forwardedFor, int expectedStatus,
                                                            int timeoutSeconds) throws Exception {
        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String endpointUrl = Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        invokeUntilStatus(resolvedContext, apikey, expectedStatus, timeoutSeconds, () -> {
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");
            headers.put("ApiKey", TestContext.resolve(apikey).toString());
            headers.put("X-Forwarded-For", Utils.resolveContextPlaceholders(forwardedFor));
            return execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers, "");
        });
    }

    /**
     * API-key invocation presenting a {@code Referer} header, retrying until the expected status. For a key
     * generated with a {@code permittedReferer} restriction, the gateway matches the request's {@code Referer}
     * against the permitted patterns — a matching Referer is authorised (200), a non-matching one is forbidden
     * (403). Ports the REST api-key Referer-restriction case of APISecurityTestCase.
     */
    @When("I invoke the API at gateway context {string} with method {string} using api key {string} and referer {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiByContextUsingKeyAndRefererUntilStatus(String context, String httpMethod, String apikey,
                                                                String referer, int expectedStatus,
                                                                int timeoutSeconds) throws Exception {
        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String endpointUrl = Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        invokeUntilStatus(resolvedContext, apikey, expectedStatus, timeoutSeconds, () -> {
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");
            headers.put("ApiKey", TestContext.resolve(apikey).toString());
            headers.put("Referer", Utils.resolveContextPlaceholders(referer));
            return execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers, "");
        });
    }

    /**
     * Basic-auth invocation using an ACTOR's carbon credentials (username:password → {@code Authorization: Basic}),
     * retrying until the expected status. For an API whose securityScheme includes {@code basic_auth}, valid user
     * credentials are authorised (200). Ports the basic-auth positive of APISecurityTestCase.
     */
    @When("I invoke the API at gateway context {string} with method {string} using basic auth for actor {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiByContextUsingBasicAuthActor(String context, String httpMethod, String actorRef,
                                                      int expectedStatus, int timeoutSeconds) throws Exception {
        User actor = Identity.resolveActor(actorRef);
        String creds = Base64.getEncoder().encodeToString(
                (actor.getUserName() + ":" + actor.getPassword()).getBytes(StandardCharsets.UTF_8));
        invokeWithBasicAuthUntilStatus(context, httpMethod, "Basic " + creds, expectedStatus, timeoutSeconds);
    }

    /**
     * Basic-auth invocation with EXPLICIT username/password (for the wrong-credentials negative), retrying until
     * the expected status. Wrong credentials are rejected (401).
     */
    @When("I invoke the API at gateway context {string} with method {string} using basic auth username {string} password {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiByContextUsingBasicAuthCreds(String context, String httpMethod, String username,
                                                      String password, int expectedStatus, int timeoutSeconds)
            throws Exception {
        String creds = Base64.getEncoder().encodeToString(
                (Utils.resolveContextPlaceholders(username) + ":" + Utils.resolveContextPlaceholders(password))
                        .getBytes(StandardCharsets.UTF_8));
        invokeWithBasicAuthUntilStatus(context, httpMethod, "Basic " + creds, expectedStatus, timeoutSeconds);
    }

    /**
     * Basic-auth invocation using a VALID actor's username with an OVERRIDDEN (wrong) password — the faithful
     * wrong-credentials negative (a valid user + bad password → 401, consistently across tenants; a made-up
     * domainless username instead resolves against the super tenant and yields 403 on a tenant API).
     */
    @When("I invoke the API at gateway context {string} with method {string} using basic auth for actor {string} with password {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiByContextUsingBasicAuthActorPassword(String context, String httpMethod, String actorRef,
                                                              String password, int expectedStatus, int timeoutSeconds)
            throws Exception {
        User actor = Identity.resolveActor(actorRef);
        String creds = Base64.getEncoder().encodeToString(
                (actor.getUserName() + ":" + Utils.resolveContextPlaceholders(password)).getBytes(StandardCharsets.UTF_8));
        invokeWithBasicAuthUntilStatus(context, httpMethod, "Basic " + creds, expectedStatus, timeoutSeconds);
    }

    /** Shared retry-until-status loop for a fixed {@code Authorization} header (Basic-auth invocations). */
    private void invokeWithBasicAuthUntilStatus(String context, String httpMethod, String authHeader,
                                                int expectedStatus, int timeoutSeconds) throws Exception {
        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String endpointUrl = Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        // No credential context key: basic-auth credentials are built inline, and are not a JWT whose jti could
        // be correlated against the gateway's revocation/invalid-token caches.
        invokeUntilStatus(resolvedContext, null, expectedStatus, timeoutSeconds, () -> {
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json");
            headers.put("Authorization", authHeader);
            return execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers, "");
        });
    }

    /** Single API-key invocation against a fully-built gateway URL. */
    private HttpResponse invokeWithApiKey(String endpointUrl, String httpMethod, String apikey) throws IOException {

        String actualKey = TestContext.resolve(apikey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("ApiKey", actualKey);

        return execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers, "");
    }

    /**
     * Invokes a deployed API by its full gateway context using a publisher **internal API key** (the
     * {@code Internal-Key} header), retrying until the expected status. Unlike a devportal application API key
     * or a subscription token, the internal key can invoke a deployed-but-unpublished (CREATED-stage) API — the
     * publisher try-out path. Addresses the API by context verbatim (no tenant prefixing).
     */
    @When("I invoke the API at gateway context {string} with method {string} using internal key {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiByContextUsingInternalKeyUntilStatus(String context, String httpMethod, String internalKey,
                                                              int expectedStatus, int timeoutSeconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String endpointUrl = Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        invokeUntilStatus(resolvedContext, internalKey, expectedStatus, timeoutSeconds,
                () -> invokeWithInternalKey(endpointUrl, httpMethod, internalKey));
    }

    /** Single internal-key invocation against a fully-built gateway URL (token in the {@code Internal-Key} header). */
    private HttpResponse invokeWithInternalKey(String endpointUrl, String httpMethod, String internalKey) throws IOException {

        String actualKey = TestContext.resolve(internalKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "*/*");
        headers.put("Internal-Key", actualKey);

        return execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers, "");
    }

    /**
     * Invokes an API using an API Key, retrying until the response reaches the expected status code or
     * the timeout elapses. Useful right after deployment/publish while the gateway eventually becomes
     * consistent. The last response is left in context for the following assertion step.
     *
     * @param path           The API resource path to invoke
     * @param httpMethod     The HTTP method to use
     * @param apikey         Context key containing the API key
     * @param expectedStatus The response status code to wait for
     * @param timeoutSeconds Maximum time to keep retrying, in seconds
     */
    @When("I invoke the API resource at path {string} with method {string} using api key {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiUsingKeyUntilStatus(String path, String httpMethod, String apikey, int expectedStatus,
                                             int timeoutSeconds) throws Exception {

        invokeUntilStatus(Utils.resolveContextPlaceholders(path), apikey, expectedStatus, timeoutSeconds,
                () -> invokeApiUsingKey(path, httpMethod, apikey));
    }

    /**
     * Invokes an API using an access token, retrying until the response reaches the expected status
     * code or the timeout elapses. Useful for eventual-consistency waits (e.g. a token becoming valid
     * after subscription, or invalid after revocation). The last response is left in context.
     *
     * @param path           The API resource path to invoke
     * @param httpMethod     The HTTP method to use
     * @param accessToken    Context key containing the access token
     * @param payload        Context key containing the request payload
     * @param expectedStatus The response status code to wait for
     * @param timeoutSeconds Maximum time to keep retrying, in seconds
     */
    @When("I invoke the API resource at path {string} with method {string} using access token {string} and payload {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiUsingAccessTokenUntilStatus(String path, String httpMethod, String accessToken, String payload,
                                                     int expectedStatus, int timeoutSeconds) throws Exception {

        invokeUntilStatus(Utils.resolveContextPlaceholders(path), accessToken, expectedStatus, timeoutSeconds,
                () -> invokeApiUsingAccessToken(path, httpMethod, accessToken, payload));
    }

    /**
     * Invokes the OpenID Connect userinfo endpoint with the given access token and stores the response.
     *
     * <p>The endpoint is resolved TENANT-AWARE from the acting actor's tenant: a tenant user's token must be
     * presented at {@code t/<tenant>/oauth2/userinfo} (see
     * {@link Utils#getUserInfoEndpointURL(String, String)}). This step previously always built the unprefixed
     * super-tenant path, so the tenant row of the OpenID scenario never actually exercised the tenant path its
     * comment claimed — and presenting a tenant token at the super path intermittently answered 400
     * {@code "Error in getting AccessTokenDO"}, a real flake this resolves.
     *
     * @param accessToken Context key containing the access token
     */
    @When("I invoke the OpenID userinfo endpoint using access token {string}")
    public void invokeUserInfoEndpoint(String accessToken) throws Exception {

        String actualAccessToken = TestContext.resolve(accessToken).toString();
        String baseUrl = TestContext.get("baseUrl").toString();

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualAccessToken);
        headers.put("accept", "application/json");

        // Tenant-aware by design: a tenant token must be presented at t/<tenant>/oauth2/userinfo. The acting
        // actor's @domain does NOT re-route the un-prefixed path, so resolving the tenant here is what makes the
        // tenant row of an actor Scenario Outline genuinely exercise the tenant route rather than repeat the
        // super-tenant one (ports the legacy OpenIDTokenAPITestCase tenant branch).
        execute(CurlOption.HttpMethod.GET,
                Utils.getUserInfoEndpointURL(baseUrl, Identity.actingTenantDomain()), headers, null);
    }

    /**
     * Invokes a SOAP API endpoint using an OAuth2 access token for authentication.
     * This step sets the appropriate Content-Type header for SOAP (text/xml) and includes the SOAPAction header
     * if provided. The payload should be a SOAP envelope in XML format.
     *
     * @param path The SOAP API resource path to invoke
     * @param accessToken Context key containing the access token to use for authentication
     * @param payload Context key containing the SOAP request payload (XML format)
     * @param soapAction The SOAPAction header value
     */
    @When("I invoke the SOAP API at path {string} using access token {string} and payload {string} and soap action {string}")
    public void iInvokeTheSOAPAPIAtPathUsingAccessTokenAndPayloadAndSoapAction(String path, String accessToken, String payload, String soapAction) throws IOException {

        String actualAccessToken = TestContext.resolve(accessToken).toString();
        String actualPayload = TestContext.resolve(payload).toString();
        String endpointUrl = Utils.getAPIInvocationURL(Utils.getBaseGatewayUrl(),
                Utils.resolveContextPlaceholders(path), Identity.actingTenantDomain());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualAccessToken);
        headers.put("Content-Type", Constants.CONTENT_TYPES.TEXT_XML);
        if (soapAction != null && !soapAction.isBlank()) {
            headers.put("SOAPAction", soapAction);
        }

        execute(CurlOption.HttpMethod.POST, endpointUrl, headers, actualPayload, Constants.CONTENT_TYPES.TEXT_XML);
    }

    /**
     * Invokes a SOAP API addressing it by its full gateway context path (the {@code context} field returned by
     * the Publisher API, which already carries the {@code /t/<tenant>} prefix for tenant APIs) — so no tenant
     * prefix is added here. Use this when the path was captured from an API's {@code context} (needed for the
     * tenant rows of a Scenario Outline); the {@code at path} variant double-prefixes a tenant context.
     */
    @When("I invoke the SOAP API at gateway context {string} using access token {string} and payload {string} and soap action {string}")
    public void invokeSoapByGatewayContext(String context, String accessToken, String payload, String soapAction) throws IOException {

        String actualAccessToken = TestContext.resolve(accessToken).toString();
        String actualPayload = TestContext.resolve(payload).toString();
        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String endpointUrl = Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualAccessToken);
        headers.put("Content-Type", Constants.CONTENT_TYPES.TEXT_XML);
        if (soapAction != null && !soapAction.isBlank()) {
            headers.put("SOAPAction", soapAction);
        }

        execute(CurlOption.HttpMethod.POST, endpointUrl, headers, actualPayload, Constants.CONTENT_TYPES.TEXT_XML);
    }

    /**
     * Invokes a published SSE API through the gateway and counts the events that arrive, addressing it by its full
     * gateway context (which already carries {@code /t/<tenant>} for a tenant API — see the REST context variant).
     *
     * <p>SSE is NOT a WebSocket protocol: {@code SseApiHandler} rewrites the verb to SUBSCRIBE for authentication
     * and the API is otherwise an ordinary synapse resource bound to the default (HTTP passthrough) transport, so
     * this is a plain authenticated GET on the gateway and funnels through {@link #execute} like every other
     * invocation. The stream is BOUNDED by construction — the emitter sends exactly {@code events} frames (the
     * {@code count} query parameter) and then closes — so the client reads it to completion as a normal response
     * body instead of needing a streaming receiver plus a race-prone "how long shall I listen" window. That is
     * what makes the received count exact rather than "whatever arrived before teardown", which the legacy
     * unbounded Jetty {@code EventSourceServlet} could not offer.
     *
     * <p>Events are counted as {@code data:} lines, one per frame (the emitter writes a single data line per
     * event). {@code tag} scopes the emitter's own diagnostic record of this stream so the following backend
     * cross-check reads THIS stream and not a sibling scenario's — pass a unique value.
     */
    @When("I invoke the SSE API at gateway context {string} using access token {string} requesting {int} events tagged {string} within {int} seconds")
    public void invokeSseApiByGatewayContext(String context, String accessToken, int events, String tag,
                                             int timeoutSeconds) throws Exception {
        invokeSse(context, accessToken, events, tag, timeoutSeconds, events, null);
    }

    /**
     * The event-quota arc: opens the stream repeatedly until the plan's quota has been consumed and the gateway
     * delivers ZERO events, which is the one exactly-determined state a streaming quota produces. Ports the
     * event-quota half of ServerSentEventsAPITestCase#testSseApiThrottling.
     *
     * <p>WHY ZERO AND NOT "EXACTLY N". The obvious shape — request 20 events against a 2-events/min plan and assert
     * exactly 2 arrive — is NOT a property of the product, and asserting it would be a guess wearing an exact
     * assertion. {@code SseResponseStreamInterceptor.targetResponse} counts the {@code "\n\n"}-delimited events in
     * each buffer and then does two INDEPENDENT things: it READS the throttle verdict out of the shared throttle-data
     * holder ({@code SseUtils.isThrottled} over the resource-, subscription- and application-level keys) and it
     * PUBLISHES the event count to the traffic manager on a separate executor
     * ({@code SseUtils.publishNonThrottledEvent}). The verdict therefore lags the traffic that caused it by one
     * decision round-trip, so a handful of events past the quota are still forwarded before the holder flips.
     * MEASURED: with a 2-events/min plan and a 1s emitter cadence the gateway delivered 4 of 20 — genuinely
     * truncated, but not at the quota. "Fewer than requested" would be the §12-forbidden widened form (and vacuous
     * at zero, the defect that let legacy ThrottlingTestCase pass with a dead fan-out), so neither shape is used.
     * Once the verdict HAS landed the quota is absolute for the rest of the window, and that end state is exact: a
     * fresh stream yields exactly 0 events. This is also the legacy's own second observable — it reconnected after
     * the throttle and expected the reconnect to be refused.
     *
     * <p>VACUITY IS KILLED BY THE FIRST ATTEMPT, which must deliver at least one event: an API that never routed
     * would answer 0 immediately and otherwise satisfy this step. So the assertion is a pair — the stream worked,
     * then the quota silenced it — and the step fails naming which half did not hold.
     *
     * <p>{@link Utils#retryUntil} and not {@link Utils#awaitSettledCount} (§15): the target is not a monotonic
     * counter converging on a total, it is a per-stream outcome that must reach one specific value. The accept
     * condition here is {@code == 0}, not {@code >= 0}, so the "accept on reached, assert exact" hazard that makes
     * {@code retryUntil} unsound for a final COUNT does not arise — there is no over-count below zero to miss.
     *
     * <p>Per the docs an SSE event is counted server→client ONLY (unlike WebSocket's both-direction aggregate), so
     * no halving applies. The caller attaches the quota as the API's SUBSCRIPTION TIER; an earlier revision used an
     * API-level {@code apiThrottlingPolicy}, which is not a streaming lever at all, so its "20/20 delivered"
     * measurement was uninterpretable.
     *
     * <p>REQUIRES A QUOTA WINDOW LONGER THAN THIS STEP'S DEADLINE, and the caller must supply one (the scenarios
     * use an hour). "A fresh stream delivers exactly 0" is only sound while the quota STAYS exhausted: on a
     * per-minute plan the window can replenish inside the retry loop, the next stream then legitimately delivers
     * events again, and the step times out reporting an unenforced quota when nothing is wrong. Do not "simplify"
     * the plan back to per-minute.
     */
    @When("I invoke the SSE API at gateway context {string} using access token {string} requesting {int} events tagged {string} until its event quota throttles the stream to zero within {int} seconds")
    public void invokeSseUntilThrottledToZero(String context, String accessToken, int events, String tag,
                                              int timeoutSeconds) throws Exception {

        // The FIRST stream doubles as the routability gate: it retries the status while the freshly published API
        // becomes routable, so a non-zero count here means the arc really worked before the quota bit.
        int first = invokeSse(context, accessToken, events, tag, timeoutSeconds, THROTTLED_SSE_DELAY_MILLIS);
        Assert.assertTrue(first > 0, "The FIRST SSE stream on the quota-limited API delivered " + first + " events, "
                + "so the API never routed and the zero-delivery assertion below would be satisfied for the wrong "
                + "reason.");

        // Then re-open the stream until the traffic manager's verdict has landed and silences it. A plain execute
        // here, NOT the status-retry envelope: the route is already proven above, so retrying a non-200 would only
        // mask a regression.
        Integer settled = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> openSseStream(context, accessToken, events, tag, THROTTLED_SSE_DELAY_MILLIS),
                received -> received == 0);

        Assert.assertNotNull(settled, "The SSE quota arc never completed a follow-up invocation within the deadline "
                + "(the first stream delivered " + first + " events).");
        Assert.assertEquals(settled.intValue(), 0, "The event quota never silenced the SSE stream: the last "
                + "invocation still delivered " + settled + " of " + events + " requested events (the first "
                + "delivered " + first + "). Once the traffic manager's verdict has landed a fresh stream on an "
                + "exhausted quota must deliver exactly 0.");
        // The measured trajectory, not just the verdict: a green run otherwise records nothing about WHERE the quota
        // bit, and that number is what tells an enforced quota apart from a stream that happened to be short.
        log.info("SSE event quota measured: first stream delivered " + first + " of " + events + " requested events, "
                + "a fresh stream after the verdict landed delivered " + settled);
    }

    /**
     * Opens ONE SSE stream through the gateway, publishes the response and returns the number of events that
     * arrived. {@code delayMillis} is the emitter's inter-frame cadence ({@code null} leaves the emitter's own fast
     * default): the smoke arc wants the whole stream to finish in well under a second, while a quota arc MUST
     * outlive the throttle decision's propagation — the quota is evaluated against Traffic-Manager state, so a
     * stream that completes in 50ms×N would finish before any decision could take effect and would look unthrottled
     * for a purely timing reason.
     */
    private int openSseStream(String context, String accessToken, int events, String tag, Integer delayMillis)
            throws IOException {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + TestContext.resolve(accessToken).toString());
        headers.put("Accept", SSE_CONTENT_TYPE);

        HttpResponse streamed = execute(CurlOption.HttpMethod.GET,
                sseStreamUrl(resolvedContext, events, tag, delayMillis), headers, null);
        int received = countSseEvents(streamed.getData());
        TestContext.set(SSE_EVENTS_RECEIVED_KEY, received);
        return received;
    }

    /** {@code <gateway><context><SSE_TOPIC_PATH>?count=&tag=[&delayMs=]} — the emitter-bounded stream URL. */
    private String sseStreamUrl(String resolvedContext, int events, String tag, Integer delayMillis) {
        return Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext
                + SSE_TOPIC_PATH + "?count=" + events + "&tag="
                + Utils.urlEncode(Utils.resolveContextPlaceholders(tag))
                + (delayMillis == null ? "" : "&delayMs=" + delayMillis);
    }

    /**
     * Opens one SSE stream RETRYING THE STATUS while the freshly published API becomes routable (§11), and returns
     * the number of events that arrived.
     */
    private int invokeSse(String context, String accessToken, int events, String tag, int timeoutSeconds,
                          Integer delayMillis) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String endpointUrl = sseStreamUrl(resolvedContext, events, tag, delayMillis);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + TestContext.resolve(accessToken).toString());
        headers.put("Accept", SSE_CONTENT_TYPE);

        invokeUntilStatus(resolvedContext + SSE_TOPIC_PATH, accessToken, 200, timeoutSeconds,
                () -> execute(CurlOption.HttpMethod.GET, endpointUrl, headers, null));

        // The funnel published the accepted response; read it back rather than re-invoking (a second call would
        // open a second stream and desynchronise the backend cross-check).
        HttpResponse streamed = (HttpResponse) TestContext.get(HTTP_RESPONSE_KEY);
        int received = countSseEvents(streamed.getData());
        TestContext.set(SSE_EVENTS_RECEIVED_KEY, received);
        return received;
    }

    /** The smoke arc's exact claim: every event the emitter was asked for arrived through the gateway. */
    private void invokeSse(String context, String accessToken, int events, String tag, int timeoutSeconds,
                           int expectedDelivered, Integer delayMillis) throws Exception {

        int received = invokeSse(context, accessToken, events, tag, timeoutSeconds, delayMillis);
        HttpResponse streamed = (HttpResponse) TestContext.get(HTTP_RESPONSE_KEY);
        Assert.assertEquals(received, expectedDelivered, "The gateway delivered " + received + " SSE events but "
                + expectedDelivered + " were expected (" + events + " requested from the backend); stream body: "
                + streamed.getData());
    }

    /**
     * The legacy {@code eventsSent == eventsReceived} assertion, sourced from BOTH ends: the emitter's own record
     * of the stream it served (its {@code /streams} diagnostic, scoped by {@code tag}) versus what the preceding
     * step counted arriving through the gateway. Without this the received count alone could not distinguish "the
     * gateway forwarded every event" from "the backend only ever sent that many" — which is the whole point of
     * the legacy pairing. Also asserts the backend ran the stream to completion, so a stream cut short mid-flight
     * (a dropped connection that happened to leave a matching count) is not read as success.
     */
    @Then("The SSE backend should have sent every event the gateway delivered for tag {string}")
    public void sseBackendSentEveryDeliveredEvent(String tag) throws IOException {

        String resolvedTag = Utils.resolveContextPlaceholders(tag);
        int received = (int) TestContext.resolve(SSE_EVENTS_RECEIVED_KEY);
        String streamsUrl = Utils.getNodeBackendUrl(SSE_EMITTER_PORT) + "/streams?tag="
                + Utils.urlEncode(resolvedTag);
        // Intermediate read: consumed locally, so it must NOT overwrite the invocation's httpResponse (§7).
        HttpResponse diagnostic = SimpleHTTPClient.getInstance().doGet(streamsUrl, new HashMap<>());
        Assert.assertTrue(diagnostic != null && diagnostic.getResponseCode() == 200
                        && diagnostic.getData() != null && !diagnostic.getData().isBlank(),
                "Could not read the sse-emitter's stream diagnostic at " + streamsUrl + "; got="
                        + (diagnostic == null ? "null" : diagnostic.getResponseCode() + "/" + diagnostic.getData()));

        JSONObject diagnosticJson = new JSONObject(diagnostic.getData());
        JSONArray streams = diagnosticJson.getJSONArray("streams");
        Assert.assertEquals(streams.length(), 1, "Expected exactly one sse-emitter stream tagged '" + resolvedTag
                + "' (the tag must be unique per invocation); got: " + diagnostic.getData());
        JSONObject stream = streams.getJSONObject(0);
        int sent = stream.getInt("sent");
        Assert.assertNotEquals(sent, 0, "The sse-emitter backend served the stream but sent no events: "
                + diagnostic.getData());
        Assert.assertTrue(stream.getBoolean("completed"), "The sse-emitter did not run the stream to completion "
                + "(the connection was cut mid-stream): " + diagnostic.getData());
        Assert.assertEquals(received, sent, "The gateway delivered " + received + " of the " + sent
                + " SSE events the backend sent: " + diagnostic.getData());
    }

    /**
     * Events in a {@code text/event-stream} body: one per {@code data:} line. Counts lines rather than the
     * {@code \n\n} frame delimiter the gateway's own interceptor counts, so a trailing/duplicated blank line
     * cannot inflate the total.
     */
    private static int countSseEvents(String body) {

        if (body == null) {
            return 0;
        }
        int events = 0;
        for (String line : body.split("\n")) {
            if (line.startsWith("data:")) {
                events++;
            }
        }
        return events;
    }

    /**
     * Invokes a deployed API through the gateway with {@link SimpleHTTPClient#doRequestCollectingHeader} — NOT the
     * shared {@link #execute} funnel — so that MULTI-VALUED response headers are preserved and can be asserted
     * exactly. The shared {@code HttpResponse.getHeaders()} returns a {@code Map<String,String>}, which cannot hold
     * two values for one header name, so the generic header-assertion steps ({@code The response header … should …})
     * are structurally blind to a backend that emits the same header twice (e.g. two {@code Set-Cookie}s). This step
     * reads every value of the named header instead and asserts BOTH the exact count AND the exact (sorted) set of
     * values, so a gateway that COLLAPSED the duplicates (one value), duplicated one of them (two identical values)
     * or added a third would each fail. The shared {@code HttpResponse} type is deliberately left unchanged:
     * widening it to hold multi-valued headers is a cross-cutting change to a shared module and out of scope for
     * this assertion, which is why the raw-client primitive is used here rather than the usual funnel.
     *
     * <p>It goes through {@code SimpleHTTPClient} (Apache, trust-all + {@code NoopHostnameVerifier}) rather than
     * the JDK {@code java.net.http.HttpClient} this step first used: the container certificate is
     * {@code CN=localhost} / SAN {@code DNSName: localhost}, and the JDK client always forces
     * {@code endpointIdentificationAlgorithm=HTTPS} with no API to disable it — so on any host where
     * testcontainers publishes the gateway on an IP (colima/remote docker → {@code https://<ip>:<port>}) EVERY
     * attempt died with an SSLHandshakeException and the step timed out with "last status -1".
     *
     * <p>Does NOT publish {@code httpResponse} (the raw response is not the shared type the generic assertion
     * steps read), so it self-asserts the status and headers; it still clears any prior {@code httpResponse}
     * first, so a throw cannot leave a stale response for a following assertion (§7). Retries the whole invocation
     * until it returns 200 (a freshly published API takes a moment to become routable) — but a 200 whose header
     * count/values are wrong is NOT retried, so a genuine collapse surfaces as an assertion failure rather than a
     * timeout.</p>
     *
     * <p>LIMITATION: {@code expectedValuesCsv} is comma-split, so an expected header value that itself CONTAINS a
     * comma cannot be expressed — the common case being a dated {@code Set-Cookie}
     * ({@code …; Expires=Wed, 21 Oct 2026 …}), which would be read as two values and fail the count assertion.
     * Neither scenario using this step needs one today. If one ever does, take the markers as a DataTable instead
     * (see {@code LoggingSteps#logFileShouldGainLineContainingAll}, which uses that form for the same reason)
     * rather than inventing a second delimiter.</p>
     */
    @When("I invoke the API at gateway context {string} with method {string} using access token {string} and assert the response carries exactly {int} {string} headers with values {string} within {int} seconds")
    public void invokeAndAssertMultiValuedHeader(String context, String httpMethod, String accessToken,
                                                 int expectedCount, String headerName, String expectedValuesCsv,
                                                 int timeoutSeconds) throws Exception {

        TestContext.remove(HTTP_RESPONSE_KEY);
        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String token = TestContext.resolve(accessToken).toString();
        String base = Utils.getBaseGatewayUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String endpointUrl = base + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        List<String> expectedValues = Arrays.stream(expectedValuesCsv.split(","))
                .map(String::trim).sorted().collect(Collectors.toList());

        Map<String, String> requestHeaders = Identity.bearerHeaders(token);
        AtomicInteger lastStatus = new AtomicInteger(-1);
        // Retry only ROUTABILITY (a non-200 while the gateway warms up); once a 200 arrives its headers ARE the
        // answer and are asserted below — a collapsed header set must fail, not be polled away.
        List<String> observed = Utils.retryUntil(timeoutSeconds * 1000L, () -> {
            SimpleHTTPClient.MultiValuedHeaderResult result = SimpleHTTPClient.getInstance()
                    .doRequestCollectingHeader(httpMethod, endpointUrl, requestHeaders, headerName);
            lastStatus.set(result.getStatusCode());
            if (result.getStatusCode() != 200) {
                return null;
            }
            return result.getHeaderValues();
        }, result -> true);

        Assert.assertNotNull(observed, "The API at " + resolvedContext + " never returned 200 within the deadline "
                + "(last status " + lastStatus.get() + "), so the '" + headerName + "' headers could not be asserted.");
        List<String> observedSorted = new ArrayList<>(observed);
        Collections.sort(observedSorted);
        Assert.assertEquals(observed.size(), expectedCount, "The gateway returned " + observed.size() + " '"
                + headerName + "' header(s) but exactly " + expectedCount + " were expected — the duplicate headers "
                + "were collapsed or multiplied. Observed values: " + observed);
        Assert.assertEquals(observedSorted, expectedValues, "The '" + headerName + "' header values did not match "
                + "exactly. Observed (sorted): " + observedSorted + "; expected (sorted): " + expectedValues);
    }
}
