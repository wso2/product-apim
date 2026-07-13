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

import io.cucumber.java.en.When;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Gateway WebSocket-API invocation glue (ports WebSocketAPITestCase). Connects a WebSocket client through the
 * gateway's WS inbound (apim.ws.port=9099, exposed by DynamicApimContainer as {@code baseGatewayWsUrl}), sends
 * a text frame, and asserts the echo. The backend is the raw-WS echo handler on {@code node-customer-service}
 * which returns each message UPPERCASED — the same contract the legacy Jetty WebSocketServerImpl used.
 *
 * <p>Uses the JDK's built-in {@link java.net.http.WebSocket} (no new dependency). The token is presented in the
 * {@code Authorization} header on the upgrade request (the primary auth mode legacy exercised).</p>
 */
public class WebSocketInvocationSteps {

    private static final String BASE_GATEWAY_WS_URL_KEY = "baseGatewayWsUrl";
    private static final String BASE_GATEWAY_WSS_URL_KEY = "baseGatewayWssUrl";

    private String getBaseGatewayWsUrl() {
        Object url = TestContext.get(BASE_GATEWAY_WS_URL_KEY);
        if (url == null) {
            throw new IllegalStateException("baseGatewayWsUrl is not available in the test context yet");
        }
        return url.toString();
    }

    private String getBaseGatewayWssUrl() {
        Object url = TestContext.get(BASE_GATEWAY_WSS_URL_KEY);
        if (url == null) {
            throw new IllegalStateException("baseGatewayWssUrl is not available in the test context yet");
        }
        return url.toString();
    }

    /**
     * Connects to a deployed WebSocket API at its full gateway WS context (the context already carries the
     * {@code /t/<tenant>} prefix for tenant APIs), sends {@code message}, and asserts the echoed response equals
     * {@code expectedEcho}. Retries the whole connect+echo until it succeeds or the deadline elapses (a freshly
     * published WS API takes a moment to become routable at the gateway).
     */
    @When("I invoke the WebSocket API at gateway ws context {string} with message {string} using access token {string} expecting echo {string} within {int} seconds")
    public void invokeWebSocket(String context, String message, String accessToken, String expectedEcho,
                                int timeoutSeconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String token = TestContext.resolve(accessToken).toString();
        String base = getBaseGatewayWsUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String wsUrl = base + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;

        long endTime = System.currentTimeMillis() + Math.max(timeoutSeconds * 1000L, 30000L);
        String lastError = null;
        String received = null;
        while (System.currentTimeMillis() < endTime) {
            try {
                received = connectSendReceive(wsUrl, token, message);
                if (received != null) {
                    break;
                }
            } catch (Exception connectingWhileWarmup) {
                // The WS route may not be reachable immediately after publish (handshake 404/403 during
                // warm-up). Retry until the deadline.
                lastError = connectingWhileWarmup.getMessage();
            }
            Thread.sleep(2000);
        }
        Assert.assertNotNull(received, "WebSocket API did not echo a response within the deadline; last error: "
                + lastError);
        Assert.assertEquals(received, expectedEcho,
                "WebSocket echo mismatch — expected the backend to return the message uppercased.");
    }

    /**
     * As {@link #invokeWebSocket} but authenticates with an API KEY in the {@code apikey} header (the WS api-key
     * auth mode — {@code wscat -H "apikey: <key>"} in the docs) instead of an OAuth token.
     */
    @When("I invoke the WebSocket API at gateway ws context {string} with message {string} using api key {string} expecting echo {string} within {int} seconds")
    public void invokeWebSocketWithApiKey(String context, String message, String apiKey, String expectedEcho,
                                          int timeoutSeconds) throws Exception {
        String wsUrl = buildWsUrl(context);
        String key = TestContext.resolve(apiKey).toString();
        long endTime = System.currentTimeMillis() + Math.max(timeoutSeconds * 1000L, 30000L);
        String lastError = null;
        String received = null;
        while (System.currentTimeMillis() < endTime) {
            try {
                received = connectSendReceive(wsUrl, "apikey", key, message);
                if (received != null) {
                    break;
                }
            } catch (Exception connectingWhileWarmup) {
                lastError = connectingWhileWarmup.getMessage();
            }
            Thread.sleep(2000);
        }
        Assert.assertNotNull(received, "WebSocket API (api-key auth) did not echo within the deadline; last error: "
                + lastError);
        Assert.assertEquals(received, expectedEcho, "WebSocket echo mismatch (api-key auth).");
    }

    /**
     * Asserts the gateway REJECTS a WS invocation carrying an invalid/insufficient OAuth token — the WS upgrade
     * is refused (handshake exception) or the connection closes without echoing. Precede with a valid-token
     * invoke so the API is already routable, so this rejection is genuine (not warm-up).
     */
    @When("I invoke the WebSocket API at gateway ws context {string} using access token {string} expecting rejection within {int} seconds")
    public void invokeWsExpectRejectionToken(String context, String accessToken, int timeoutSeconds) throws Exception {
        String token = TestContext.resolve(accessToken).toString();
        expectRejection(buildWsUrl(context), "Authorization", "Bearer " + token, timeoutSeconds);
    }

    /** As above but the rejected credential is an API KEY in the {@code apikey} header (e.g. expired / not enabled). */
    @When("I invoke the WebSocket API at gateway ws context {string} using api key {string} expecting rejection within {int} seconds")
    public void invokeWsExpectRejectionApiKey(String context, String apiKey, int timeoutSeconds) throws Exception {
        String key = TestContext.resolve(apiKey).toString();
        expectRejection(buildWsUrl(context), "apikey", key, timeoutSeconds);
    }

    /**
     * Sends {@code messageCount} messages over ONE WebSocket connection and asserts the gateway THROTTLES before
     * all of them echo — the API-level frame-count policy counts each WS frame (both directions) as a request, so
     * once the limit is hit the gateway stops echoing / closes the connection. Passes if fewer than
     * {@code messageCount} echoes come back. Ports the throttling check of WebSocketAPITestCase.
     */
    @When("I invoke the WebSocket API at gateway ws context {string} sending {int} messages using access token {string} expecting throttling within {int} seconds")
    public void invokeWsExpectThrottling(String context, int messageCount, String accessToken, int timeoutSeconds)
            throws Exception {
        String wsUrl = buildWsUrl(context);
        String token = TestContext.resolve(accessToken).toString();
        java.util.concurrent.BlockingQueue<String> echoes = new java.util.concurrent.LinkedBlockingQueue<>();
        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean(false);

        // Establish the connection, retrying only the CONNECT (data frames, not the handshake, count toward the
        // limit) until the freshly-deployed API is routable.
        long connectDeadline = System.currentTimeMillis() + Math.max(timeoutSeconds * 1000L, 30000L);
        WebSocket ws = null;
        while (ws == null && System.currentTimeMillis() < connectDeadline) {
            try {
                ws = HttpClient.newBuilder().sslContext(trustAllSslContext()).build()
                        .newWebSocketBuilder()
                        .header("Authorization", "Bearer " + token)
                        .connectTimeout(java.time.Duration.ofSeconds(15))
                        .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                            private final StringBuilder parts = new StringBuilder();

                            @Override
                            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                                parts.append(data);
                                if (last) {
                                    echoes.add(parts.toString());
                                    parts.setLength(0);
                                }
                                webSocket.request(1);
                                return null;
                            }

                            @Override
                            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                                closed.set(true);
                                return null;
                            }

                            @Override
                            public void onError(WebSocket webSocket, Throwable error) {
                                closed.set(true);
                            }
                        }).get(20, TimeUnit.SECONDS);
            } catch (Exception warmup) {
                Thread.sleep(2000);
            }
        }
        Assert.assertNotNull(ws, "Could not establish the WebSocket connection for the throttling test");

        int received = 0;
        try {
            for (int i = 0; i < messageCount; i++) {
                if (closed.get()) {
                    break;
                }
                try {
                    ws.sendText("throttle-msg-" + i, true).get(5, TimeUnit.SECONDS);
                } catch (Exception sendFailed) {
                    break;   // connection dropped by the throttle handler
                }
                String echo = echoes.poll(3, TimeUnit.SECONDS);
                if (echo == null) {
                    break;   // no echo → throttled
                }
                received++;
                // Space the frames so the traffic manager's async throttle decision (per-minute event count) has
                // time to be computed and pushed back to the gateway — a rapid burst outruns it and never trips.
                Thread.sleep(1500);
            }
        } finally {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            } catch (Exception ignore) {
                // already closed
            }
        }
        Assert.assertTrue(received < messageCount,
                "Expected WS throttling to stop echoes before all " + messageCount + " messages, but received "
                        + received + " (no throttling observed).");
    }

    /** Builds the full gateway WS URL from an API context (context already carries {@code /t/<tenant>}). */
    private String buildWsUrl(String context) {
        return joinBase(getBaseGatewayWsUrl(), context);
    }

    /** Builds the full gateway SECURE (wss://) URL from an API context. */
    private String buildWssUrl(String context) {
        return joinBase(getBaseGatewayWssUrl(), context);
    }

    private String joinBase(String base, String context) {
        String resolvedContext = Utils.resolveContextPlaceholders(context);
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
    }

    /**
     * SECURE (wss://) counterpart of {@link #invokeWebSocket} — connects over the gateway's secure WS inbound
     * (apim.wss.port=8099) and asserts the echo. The trust-all SSLContext handles the TLS handshake.
     */
    @When("I invoke the WebSocket API at gateway wss context {string} with message {string} using access token {string} expecting echo {string} within {int} seconds")
    public void invokeSecureWebSocket(String context, String message, String accessToken, String expectedEcho,
                                      int timeoutSeconds) throws Exception {
        String wsUrl = buildWssUrl(context);
        String token = TestContext.resolve(accessToken).toString();
        Assert.assertEquals(echoOverWs(wsUrl, "Authorization", "Bearer " + token, message, timeoutSeconds),
                expectedEcho, "Secure WebSocket (wss) echo mismatch.");
    }

    /** SECURE (wss://) WS invocation authenticated with an API key. */
    @When("I invoke the WebSocket API at gateway wss context {string} with message {string} using api key {string} expecting echo {string} within {int} seconds")
    public void invokeSecureWebSocketWithApiKey(String context, String message, String apiKey, String expectedEcho,
                                                int timeoutSeconds) throws Exception {
        String wsUrl = buildWssUrl(context);
        String key = TestContext.resolve(apiKey).toString();
        Assert.assertEquals(echoOverWs(wsUrl, "apikey", key, message, timeoutSeconds), expectedEcho,
                "Secure WebSocket (wss, api-key) echo mismatch.");
    }

    /**
     * CORS: invoke a WS API presenting an {@code Origin} header (plus the token). With WS CORS validation enabled
     * (gateway-wide {@code [apim.cors] enable_validation_for_ws=true}), an ALLOWED origin connects and echoes.
     */
    @When("I invoke the WebSocket API at gateway ws context {string} with message {string} using access token {string} and origin {string} expecting echo {string} within {int} seconds")
    public void invokeWsWithOrigin(String context, String message, String accessToken, String origin,
                                   String expectedEcho, int timeoutSeconds) throws Exception {
        Assert.assertEquals(echoOverWs(buildWsUrl(context), authAndOrigin(accessToken, origin), message,
                timeoutSeconds), expectedEcho, "WS (with Origin) echo mismatch.");
    }

    /** CORS negative: a DISALLOWED {@code Origin} is rejected at the WS handshake. */
    @When("I invoke the WebSocket API at gateway ws context {string} using access token {string} and origin {string} expecting rejection within {int} seconds")
    public void invokeWsWithOriginExpectRejection(String context, String accessToken, String origin,
                                                  int timeoutSeconds) throws Exception {
        expectRejection(buildWsUrl(context), authAndOrigin(accessToken, origin), timeoutSeconds);
    }

    private java.util.Map<String, String> authAndOrigin(String accessToken, String origin) {
        java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + TestContext.resolve(accessToken).toString());
        headers.put("Origin", Utils.resolveContextPlaceholders(origin));
        return headers;
    }

    /**
     * API-key IP restriction (WS negative): invoke with an IP-restricted API key AND an {@code X-Forwarded-For}
     * header, expecting REJECTION. The WS inbound enforces the {@code permittedIP} on the real socket client IP and
     * IGNORES X-Forwarded-For (verified via a standalone probe — unlike the REST passthrough, which honours XFF),
     * so the in-container client is rejected even when XFF claims the permitted IP.
     */
    @When("I invoke the WebSocket API at gateway ws context {string} using api key {string} and forwarded-for {string} expecting rejection within {int} seconds")
    public void invokeWsApiKeyXffReject(String context, String apiKey, String xff, int timeoutSeconds)
            throws Exception {
        expectRejection(buildWsUrl(context), apiKeyAndXff(apiKey, xff), timeoutSeconds);
    }

    private java.util.Map<String, String> apiKeyAndXff(String apiKey, String xff) {
        java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();
        headers.put("apikey", TestContext.resolve(apiKey).toString());
        headers.put("X-Forwarded-For", Utils.resolveContextPlaceholders(xff));
        return headers;
    }

    /**
     * API-key Referer restriction (WS positive): invoke with a Referer-restricted API key AND a MATCHING
     * {@code Referer} handshake header, expecting the backend echo. Unlike {@code permittedIP} (matched on the
     * socket IP, so XFF-immune), {@code permittedReferer} is matched against the client-sent {@code Referer}
     * header on the WS upgrade — so the positive is directly assertable by presenting a matching Referer.
     */
    @When("I invoke the WebSocket API at gateway ws context {string} with message {string} using api key {string} and referer {string} expecting echo {string} within {int} seconds")
    public void invokeWsApiKeyWithReferer(String context, String message, String apiKey, String referer,
                                          String expectedEcho, int timeoutSeconds) throws Exception {
        Assert.assertEquals(echoOverWs(buildWsUrl(context), apiKeyAndReferer(apiKey, referer), message,
                timeoutSeconds), expectedEcho, "WS (api-key + matching Referer) echo mismatch.");
    }

    /** API-key Referer restriction (WS negative): a NON-matching {@code Referer} is rejected at the handshake. */
    @When("I invoke the WebSocket API at gateway ws context {string} using api key {string} and referer {string} expecting rejection within {int} seconds")
    public void invokeWsApiKeyRefererReject(String context, String apiKey, String referer, int timeoutSeconds)
            throws Exception {
        expectRejection(buildWsUrl(context), apiKeyAndReferer(apiKey, referer), timeoutSeconds);
    }

    private java.util.Map<String, String> apiKeyAndReferer(String apiKey, String referer) {
        java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();
        headers.put("apikey", TestContext.resolve(apiKey).toString());
        headers.put("Referer", Utils.resolveContextPlaceholders(referer));
        return headers;
    }

    /**
     * Query-parameter auth (WS positive, api-key): the API key is presented as a {@code ?apikey=<key>} query
     * parameter on the WS URL instead of a header (the {@code AUTH_IN.APIKEY_QUERY} mode legacy exercised). No
     * auth header is sent — auth rides entirely on the query string.
     */
    @When("I invoke the WebSocket API at gateway ws context {string} with message {string} using api key query param {string} expecting echo {string} within {int} seconds")
    public void invokeWsApiKeyQueryParam(String context, String message, String apiKey, String expectedEcho,
                                         int timeoutSeconds) throws Exception {
        String key = TestContext.resolve(apiKey).toString();
        String wsUrl = appendQuery(buildWsUrl(context), "apikey", key);
        Assert.assertEquals(echoOverWs(wsUrl, new java.util.LinkedHashMap<>(), message, timeoutSeconds),
                expectedEcho, "WS (api-key query param) echo mismatch.");
    }

    /** Query-parameter auth (WS positive, token): the token is presented as {@code ?access_token=<token>}. */
    @When("I invoke the WebSocket API at gateway ws context {string} with message {string} using access token query param {string} expecting echo {string} within {int} seconds")
    public void invokeWsTokenQueryParam(String context, String message, String accessToken, String expectedEcho,
                                        int timeoutSeconds) throws Exception {
        String token = TestContext.resolve(accessToken).toString();
        String wsUrl = appendQuery(buildWsUrl(context), "access_token", token);
        Assert.assertEquals(echoOverWs(wsUrl, new java.util.LinkedHashMap<>(), message, timeoutSeconds),
                expectedEcho, "WS (token query param) echo mismatch.");
    }

    /** Appends a single URL-encoded query parameter to a WS URL. */
    private String appendQuery(String wsUrl, String name, String value) {
        String sep = wsUrl.contains("?") ? "&" : "?";
        return wsUrl + sep + name + "="
                + java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Connect+send+echo retry loop (single auth header); delegates to the multi-header form. */
    private String echoOverWs(String wsUrl, String headerName, String headerValue, String message,
                              int timeoutSeconds) throws Exception {
        java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();
        headers.put(headerName, headerValue);
        return echoOverWs(wsUrl, headers, message, timeoutSeconds);
    }

    /** Connect+send+echo retry loop with arbitrary headers; returns the echo or fails after the deadline. */
    private String echoOverWs(String wsUrl, java.util.Map<String, String> headers, String message,
                              int timeoutSeconds) throws Exception {
        long endTime = System.currentTimeMillis() + Math.max(timeoutSeconds * 1000L, 30000L);
        String lastError = null;
        String received = null;
        while (System.currentTimeMillis() < endTime) {
            try {
                received = connectSendReceive(wsUrl, headers, message);
                if (received != null) {
                    break;
                }
            } catch (Exception connectingWhileWarmup) {
                lastError = connectingWhileWarmup.getMessage();
            }
            Thread.sleep(2000);
        }
        Assert.assertNotNull(received, "WebSocket did not echo within the deadline; last error: " + lastError);
        return received;
    }

    /**
     * Confirms a WS invocation is rejected, retrying until a rejection is observed within the deadline. A
     * handshake exception (upgrade refused) or a no-echo close is the rejection. An echo means "not rejected
     * (yet)" — because an enforcement control (e.g. a scope gate freshly attached to the operation) can take a
     * moment to propagate to the gateway after a redeploy, an early echo is tolerated and retried; the step only
     * FAILS if the invocation keeps echoing for the WHOLE deadline (enforcement never applied — a genuine leak).
     * A garbage/invalid credential rejects on the first attempt, so this stays fast for immediate negatives.
     */
    private void expectRejection(String wsUrl, String headerName, String headerValue, int timeoutSeconds)
            throws Exception {
        java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();
        headers.put(headerName, headerValue);
        expectRejection(wsUrl, headers, timeoutSeconds);
    }

    /** Multi-header rejection check (e.g. Authorization + a disallowed Origin for CORS). */
    private void expectRejection(String wsUrl, java.util.Map<String, String> headers, int timeoutSeconds)
            throws Exception {
        long endTime = System.currentTimeMillis() + Math.max(timeoutSeconds * 1000L, 10000L);
        boolean everEchoed = false;
        while (System.currentTimeMillis() < endTime) {
            try {
                String echo = connectSendReceive(wsUrl, headers, "probe");
                if (echo == null) {
                    return;   // no echo → rejected
                }
                everEchoed = true;   // accepted this round — enforcement may still be propagating; retry
            } catch (Exception rejected) {
                return;   // handshake/upgrade refused → rejection confirmed
            }
            Thread.sleep(2000);
        }
        Assert.assertFalse(everEchoed, "Expected the WS invocation to be rejected within " + timeoutSeconds
                + "s, but it kept echoing — the enforcement control never applied.");
    }

    /**
     * Connects to a deployed GraphQL API's subscription endpoint through the gateway WS inbound (subprotocol
     * {@code graphql-ws}), performs the graphql-ws handshake (connection_init → connection_ack), sends a
     * {@code start} message carrying the subscription query, and asserts the first {@code data} message contains
     * {@code expectedData}. Retries the whole flow until it succeeds or the deadline elapses. Ports the core
     * subscription invocation of GraphqlSubscriptionTestCase (liftStatusChange → {"name":"Astra Express"}).
     */
    @When("I invoke the GraphQL subscription at gateway ws context {string} with query {string} using access token {string} expecting data containing {string} within {int} seconds")
    public void invokeGraphqlSubscription(String context, String query, String accessToken, String expectedData,
                                          int timeoutSeconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String token = TestContext.resolve(accessToken).toString();
        String base = getBaseGatewayWsUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String wsUrl = base + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;

        long endTime = System.currentTimeMillis() + Math.max(timeoutSeconds * 1000L, 30000L);
        String lastError = null;
        String data = null;
        while (System.currentTimeMillis() < endTime) {
            try {
                data = subscribeAndReceive(wsUrl, token, query);
                if (data != null && data.contains(expectedData)) {
                    break;
                }
            } catch (Exception connectingWhileWarmup) {
                lastError = connectingWhileWarmup.getMessage();
            }
            Thread.sleep(2000);
        }
        Assert.assertNotNull(data, "GraphQL subscription returned no data message within the deadline; last error: "
                + lastError);
        Assert.assertTrue(data.contains(expectedData),
                "GraphQL subscription data did not contain '" + expectedData + "'; last message: " + data);
    }

    /**
     * Opens a graphql-ws subscription and sends {@code frames} start-frames; asserts the gateway THROTTLES a frame
     * (code 4003 "Websocket frame throttled out") once the API-level request-count limit is exceeded. Ports the
     * subscription-throttling case of GraphqlSubscriptionTestCase. NOTE: this enforces via an API-level advanced
     * REQUEST-COUNT policy (local frame counting at the WS inbound) — the mechanism that actually works, unlike a
     * subscription EVENT-COUNT plan (async TM loop) which does not enforce on the all-in-one profile. Frame counts
     * are logged for diagnostics.
     */
    @When("I invoke the GraphQL subscription at gateway ws context {string} with query {string} using access token {string} sending {int} frames expecting frame throttling")
    public void invokeGraphqlSubscriptionExpectThrottling(String context, String query, String accessToken,
                                                          int frames) throws Exception {
        String wsUrl = joinBase(getBaseGatewayWsUrl(), context);
        String token = TestContext.resolve(accessToken).toString();
        long deadline = System.currentTimeMillis() + 150000L;
        int[] result = null;
        String throttleMsg = null;
        while (System.currentTimeMillis() < deadline) {
            java.util.concurrent.atomic.AtomicInteger dataCount = new java.util.concurrent.atomic.AtomicInteger();
            java.util.concurrent.atomic.AtomicReference<String> tMsg = new java.util.concurrent.atomic.AtomicReference<>();
            boolean established = subscribeAndProbe(wsUrl, token, query, frames, dataCount, tMsg);
            if (established) {
                result = new int[]{dataCount.get(), tMsg.get() != null ? 1 : 0};
                throttleMsg = tMsg.get();
                break;
            }
            Thread.sleep(3000);
        }
        Assert.assertNotNull(result, "Could not establish the graphql-ws subscription for the throttling test");
        System.out.println("[GQL-SUB-THROTTLE] framesSent=" + frames + " dataMessages=" + result[0]
                + " throttled=" + (result[1] == 1) + " throttleMsg=" + throttleMsg);
        Assert.assertEquals(result[1], 1, "Expected GraphQL subscription frame throttling (4003 'Websocket frame "
                + "throttled out') within " + frames + " frames, but none occurred (data messages=" + result[0] + ").");
    }

    /**
     * Opens a graphql-ws subscription, sends ONE start(query), and asserts the gateway returns a graphql-ws
     * {@code error} message carrying {@code expectedCode} (e.g. 4021 QUERY TOO COMPLEX, 4020 QUERY TOO DEEP).
     * Retries the whole flow for warm-up. Ports the complexity/depth rejection cases of GraphqlSubscriptionTestCase.
     */
    @When("I invoke the GraphQL subscription at gateway ws context {string} with query {string} using access token {string} expecting error code {int} within {int} seconds")
    public void invokeGraphqlSubscriptionExpectErrorCode(String context, String query, String accessToken,
                                                         int expectedCode, int timeoutSeconds) throws Exception {
        String wsUrl = joinBase(getBaseGatewayWsUrl(), context);
        String token = TestContext.resolve(accessToken).toString();
        long deadline = System.currentTimeMillis() + Math.max(timeoutSeconds * 1000L, 60000L);
        String lastMsg = null;
        while (System.currentTimeMillis() < deadline) {
            String errorMsg = subscribeExpectError(wsUrl, token, query);
            if (errorMsg != null) {
                lastMsg = errorMsg;
                if (errorMsg.contains("\"code\":" + expectedCode) || errorMsg.contains("\"code\": " + expectedCode)) {
                    System.out.println("[GQL-SUB-ERR] expected=" + expectedCode + " msg=" + errorMsg);
                    return;
                }
            }
            Thread.sleep(3000);
        }
        Assert.fail("Expected graphql-ws error code " + expectedCode + " for query [" + query
                + "] but did not observe it; last error message: " + lastMsg);
    }

    /** graphql-ws: init/ack, send one start(query), return the first {@code type:error} message (or null). */
    private String subscribeExpectError(String wsUrl, String token, String query) throws Exception {
        HttpClient client = HttpClient.newBuilder().sslContext(trustAllSslContext()).build();
        CompletableFuture<Void> ack = new CompletableFuture<>();
        CompletableFuture<String> error = new CompletableFuture<>();
        WebSocket ws;
        try {
            ws = client.newWebSocketBuilder()
                    .header("Authorization", "Bearer " + token)
                    .subprotocols("graphql-ws")
                    .connectTimeout(java.time.Duration.ofSeconds(15))
                    .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                        private final StringBuilder parts = new StringBuilder();

                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence part, boolean last) {
                            parts.append(part);
                            if (last) {
                                String msg = parts.toString();
                                parts.setLength(0);
                                if (msg.contains("connection_ack")) {
                                    ack.complete(null);
                                } else if (msg.contains("\"error\"") || msg.contains("\"code\"")) {
                                    error.complete(msg);
                                }
                            }
                            webSocket.request(1);
                            return null;
                        }
                    }).get(20, TimeUnit.SECONDS);
        } catch (Exception connectFailed) {
            return null;
        }
        try {
            Thread.sleep(15000);
            ws.sendText("{\"type\":\"connection_init\",\"payload\":{}}", true);
            ack.get(30, TimeUnit.SECONDS);
            String start = "{\"id\":\"1\",\"type\":\"start\",\"payload\":{\"variables\":{},\"extensions\":{},"
                    + "\"operationName\":null,\"query\":\"" + query.replace("\"", "\\\"") + "\"}}";
            ws.sendText(start, true);
            try {
                return error.get(30, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException noError) {
                return null;
            }
        } finally {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");
        }
    }

    /** graphql-ws multi-frame probe: init/ack, send N start-frames, capture data-count + any throttle message. */
    private boolean subscribeAndProbe(String wsUrl, String token, String query, int frames,
                                      java.util.concurrent.atomic.AtomicInteger dataCount,
                                      java.util.concurrent.atomic.AtomicReference<String> throttleMsg)
            throws Exception {
        HttpClient client = HttpClient.newBuilder().sslContext(trustAllSslContext()).build();
        CompletableFuture<Void> ack = new CompletableFuture<>();
        WebSocket ws;
        try {
            ws = client.newWebSocketBuilder()
                    .header("Authorization", "Bearer " + token)
                    .subprotocols("graphql-ws")
                    .connectTimeout(java.time.Duration.ofSeconds(15))
                    .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                        private final StringBuilder parts = new StringBuilder();

                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence part, boolean last) {
                            parts.append(part);
                            if (last) {
                                String msg = parts.toString();
                                parts.setLength(0);
                                if (msg.contains("connection_ack")) {
                                    ack.complete(null);
                                } else if (msg.contains("\"data\"")) {
                                    dataCount.incrementAndGet();
                                }
                                if (msg.contains("throttled") || msg.contains("4003")) {
                                    throttleMsg.compareAndSet(null, msg);
                                }
                            }
                            webSocket.request(1);
                            return null;
                        }
                    }).get(20, TimeUnit.SECONDS);
        } catch (Exception connectFailed) {
            return false;
        }
        try {
            Thread.sleep(15000);   // let the gateway bring up the backend WS leg
            ws.sendText("{\"type\":\"connection_init\",\"payload\":{}}", true);
            ack.get(30, TimeUnit.SECONDS);
            String q = query.replace("\"", "\\\"");
            for (int i = 1; i <= frames; i++) {
                String start = "{\"id\":\"" + i + "\",\"type\":\"start\",\"payload\":{\"variables\":{},"
                        + "\"extensions\":{},\"operationName\":null,\"query\":\"" + q + "\"}}";
                ws.sendText(start, true);
                Thread.sleep(500);
                if (throttleMsg.get() != null) {
                    break;
                }
            }
            Thread.sleep(5000);   // drain any late throttle frame
            return true;
        } finally {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");
        }
    }

    /** graphql-ws flow: connection_init → connection_ack → start(query) → first data message. */
    private String subscribeAndReceive(String wsUrl, String token, String query) throws Exception {
        HttpClient client = HttpClient.newBuilder().sslContext(trustAllSslContext()).build();
        CompletableFuture<Void> ack = new CompletableFuture<>();
        CompletableFuture<String> data = new CompletableFuture<>();
        WebSocket ws = client.newWebSocketBuilder()
                .header("Authorization", "Bearer " + token)
                .subprotocols("graphql-ws")
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    private final StringBuilder parts = new StringBuilder();

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence part, boolean last) {
                        parts.append(part);
                        if (last) {
                            String msg = parts.toString();
                            parts.setLength(0);
                            if (msg.contains("connection_ack")) {
                                ack.complete(null);
                            } else if (msg.contains("\"data\"")) {
                                data.complete(msg);
                            }
                        }
                        webSocket.request(1);
                        return null;
                    }
                })
                .get(20, TimeUnit.SECONDS);
        try {
            // The gateway establishes the backend WS leg asynchronously after the client handshake; sending
            // connection_init before it is up means the ack is never relayed back. The legacy client sleeps 20s
            // here for the same reason — give the backend leg time to come up before the graphql-ws handshake.
            Thread.sleep(15000);
            ws.sendText("{\"type\":\"connection_init\",\"payload\":{}}", true);
            ack.get(30, TimeUnit.SECONDS);
            String start = "{\"id\":\"1\",\"type\":\"start\",\"payload\":{\"variables\":{},\"extensions\":{},"
                    + "\"operationName\":null,\"query\":\"" + query.replace("\"", "\\\"") + "\"}}";
            ws.sendText(start, true);
            return data.get(30, TimeUnit.SECONDS);
        } finally {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");
        }
    }

    /**
     * Builds a trust-all {@link SSLContext}. {@link HttpClient} eagerly resolves {@link SSLContext#getDefault()},
     * which fails to construct in this test JVM ("DefaultSSLContext") even though ws:// needs no TLS; supplying
     * an explicit context sidesteps that (the suite already trusts APIM's self-signed cert for https).
     */
    private SSLContext trustAllSslContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) { }
            public void checkServerTrusted(X509Certificate[] chain, String authType) { }
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }}, new SecureRandom());
        return sslContext;
    }

    /** Token (Authorization: Bearer) convenience overload. */
    private String connectSendReceive(String wsUrl, String token, String message) throws Exception {
        return connectSendReceive(wsUrl, "Authorization", "Bearer " + token, message);
    }

    /**
     * One connect → sendText → await first text response → close cycle, presenting the given auth header
     * ({@code Authorization: Bearer <token>} or {@code apikey: <key>}). Returns the response, or throws if the
     * handshake/upgrade is refused (e.g. an invalid credential → the gateway rejects the WS upgrade).
     */
    private String connectSendReceive(String wsUrl, String headerName, String headerValue, String message)
            throws Exception {
        java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();
        headers.put(headerName, headerValue);
        return connectSendReceive(wsUrl, headers, message);
    }

    /** Multi-header variant (e.g. Authorization + Origin for CORS). Sets each header on the WS upgrade request. */
    private String connectSendReceive(String wsUrl, java.util.Map<String, String> headers, String message)
            throws Exception {
        HttpClient client = HttpClient.newBuilder().sslContext(trustAllSslContext()).build();
        CompletableFuture<String> response = new CompletableFuture<>();
        WebSocket.Builder builder = client.newWebSocketBuilder();
        for (java.util.Map.Entry<String, String> e : headers.entrySet()) {
            builder.header(e.getKey(), e.getValue());
        }
        WebSocket ws = builder
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    private final StringBuilder parts = new StringBuilder();

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        parts.append(data);
                        if (last) {
                            response.complete(parts.toString());
                        }
                        webSocket.request(1);
                        return null;
                    }
                })
                .get(20, TimeUnit.SECONDS);
        try {
            ws.sendText(message, true);
            return response.get(20, TimeUnit.SECONDS);
        } finally {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");
        }
    }
}
