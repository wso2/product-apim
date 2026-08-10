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

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

    private static final Log log = LogFactory.getLog(WebSocketInvocationSteps.class);

    // Base gap between outbound frames in the frame-quota arc — the traffic manager's throttle decision is
    // asynchronous, so frames must be spaced for it to land in the gateway's ThrottleDataHolder before the next send.
    private static final long FRAME_PACING_MILLIS = 1500L;

    // The EXACT text frame the gateway writes back when a raw-WS frame is throttled out. Assembled by
    // InboundProcessorResponseDTO.getErrorResponseString() from FrameErrorConstants.THROTTLED_OUT_ERROR (4003) and
    // THROTTLED_OUT_ERROR_MESSAGE, so it is the product's own literal and not a paraphrase.
    private static final String WS_THROTTLED_OUT_FRAME = "Error code: 4003 reason: Websocket frame throttled out";

    // Handle to a background, continuous tenant-handshake flood (see the sustained-flood steps). A sustained flood
    // keeps every event-loop thread poisoned for the WHOLE subscription arc (handshake -> connection_init -> the
    // subscribe frame's scope check -> data), so wrapping an existing subscription assertion (data delivery,
    // throttling) with it turns that assertion into a tenant-isolation guard for every tenant-scoped frame path.
    private volatile java.util.List<Thread> sustainedFloodThreads;
    private volatile java.util.concurrent.atomic.AtomicBoolean sustainedFloodRunning;
    private volatile java.util.concurrent.atomic.AtomicInteger sustainedFloodReached;


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
        String base = Utils.getBaseGatewayWsUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String wsUrl = base + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;

        long startedMillis = System.currentTimeMillis();
        AtomicReference<Throwable> lastFailure = new AtomicReference<>();
        String received = Utils.retryUntil(timeoutSeconds * 1000L, () -> {
            try {
                return connectSendReceive(wsUrl, token, message);
            } catch (InterruptedException interrupted) {
                // Cancellation is not a transient/expected outcome — restore the flag so the envelope's next
                // pause rethrows it instead of reading this as a retryable attempt.
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception connectingWhileWarmup) {
                // The WS route may not be reachable immediately after publish (handshake 404/403 during
                // warm-up). Retry until the deadline.
                lastFailure.set(connectingWhileWarmup);
                return null;
            }
        }, echo -> echo != null);
        // Computed on the failure path ONLY: the marker must never fire for a step that passed.
        String authDetail = received != null ? ""
                : authRejectionDetail(wsUrl, accessToken, token, lastFailure, startedMillis);
        Assert.assertNotNull(received, "WebSocket API did not echo a response within the deadline; last error: "
                + lastErrorOf(lastFailure) + authDetail);
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
        long startedMillis = System.currentTimeMillis();
        AtomicReference<Throwable> lastFailure = new AtomicReference<>();
        String received = Utils.retryUntil(timeoutSeconds * 1000L, () -> {
            try {
                return connectSendReceive(wsUrl, "apikey", key, message);
            } catch (InterruptedException interrupted) {
                // Cancellation is not a transient/expected outcome — restore the flag so the envelope's next
                // pause rethrows it instead of reading this as a retryable attempt.
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception connectingWhileWarmup) {
                lastFailure.set(connectingWhileWarmup);
                return null;
            }
        }, echo -> echo != null);
        String authDetail = received != null ? ""
                : authRejectionDetail(wsUrl, apiKey, key, lastFailure, startedMillis);
        Assert.assertNotNull(received, "WebSocket API (api-key auth) did not echo within the deadline; last error: "
                + lastErrorOf(lastFailure) + authDetail);
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
     * Sends up to {@code messageCount} messages over ONE WebSocket connection and asserts the gateway answered with
     * a THROTTLED-OUT frame — having first echoed the opening message, so a never-routed API cannot satisfy it.
     *
     * <p>WHAT THE OBSERVABLE ACTUALLY IS, and why the previous shape could not see it. On a raw-WS frame throttle
     * the gateway does NOT go quiet and does NOT close: {@code doThrottle} sets only {@code error=true} with
     * {@code errorCode=4003} and leaves {@code closeConnection} false, so {@code WebsocketInboundHandler}/
     * {@code WebsocketHandler} take the non-close branch and {@code writeAndFlush} a TEXT frame whose whole body is
     * {@code InboundProcessorResponseDTO.getErrorResponseString()} —
     * {@code "Error code: 4003 reason: Websocket frame throttled out"}. This step previously counted EVERY inbound
     * text frame as an echo and asserted {@code received < messageCount}, so a throttled frame was tallied as an
     * echo and a fully throttled run reported "all messages echoed, no throttling observed". Classifying the frame
     * is therefore not a refinement — without it the measurement cannot distinguish enforcement from its absence,
     * and the earlier "10 frames on a 4/min limit ALL echoed (0 throttled)" reading is not evidence of a product
     * gap.
     *
     * <p>WHY THE TRIP INDEX IS NOT ASSERTED. The gateway counts each frame in BOTH DIRECTIONS against one
     * aggregate — {@code InboundWebSocketProcessor} routes a client→server frame to
     * {@code RequestProcessor.handleRequest} and a server→client frame to {@code ResponseProcessor.handleResponse},
     * and BOTH call the same {@code doThrottle}, which publishes one event to
     * {@code org.wso2.throttle.request.stream:1.0.0} per frame. Against an ECHO backend a send therefore costs TWO
     * units, so a limit of N is consumed after N/2 sends. But {@code doThrottle} CHECKS the local
     * {@code ThrottleDataHolder} before it PUBLISHES, and the holder is filled asynchronously by the traffic
     * manager, so whether the halfway send or the one after it is refused depends on when that decision lands. The
     * exact index is consequently not a property of the product, and pinning it would be a guess dressed as an
     * assertion. What IS exact and unambiguous is asserted instead, in three parts: the first message echoed, a
     * 4003 frame arrived, and no frame carried a DIFFERENT error code (so an auth/blocked refusal can never be read
     * as a throttle). This is the same observable the live GraphQL subscription-throttling feature asserts.
     *
     * <p>Frames are SPACED through {@link Utils#pollPause} — the shared pacing primitive (§15), never a bare
     * {@code Thread.sleep} — because a back-to-back burst outruns the asynchronous decision and never trips.
     */
    @When("I invoke the WebSocket API at gateway ws context {string} sending {int} messages using access token {string} expecting a throttled-out frame within {int} seconds")
    public void invokeWsExpectThrottling(String context, int messageCount, String accessToken, int timeoutSeconds)
            throws Exception {
        String wsUrl = buildWsUrl(context);
        String token = TestContext.resolve(accessToken).toString();
        java.util.concurrent.BlockingQueue<String> echoes = new java.util.concurrent.LinkedBlockingQueue<>();
        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicInteger closeCode = new java.util.concurrent.atomic.AtomicInteger(0);

        // Establish the connection, retrying only the CONNECT (data frames, not the handshake, count toward the
        // limit) until the freshly-deployed API is routable.
        long connectDeadlineStart = System.currentTimeMillis();
        long connectDeadline = connectDeadlineStart + Math.max(timeoutSeconds * 1000L, 30000L);
        WebSocket ws = null;
        HttpClient client = null;
        while (ws == null && System.currentTimeMillis() < connectDeadline) {
            HttpClient attemptClient = HttpClient.newBuilder().sslContext(trustAllSslContext()).build();
            try {
                ws = attemptClient.newWebSocketBuilder()
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
                                closeCode.set(statusCode);
                                return null;
                            }

                            @Override
                            public void onError(WebSocket webSocket, Throwable error) {
                                closed.set(true);
                            }
                        }).get(20, TimeUnit.SECONDS);
                client = attemptClient;
            } catch (InterruptedException interrupted) {
                // Cancellation is not a transient/expected outcome — restore the flag and stop.
                closeQuietly(attemptClient);
                Thread.currentThread().interrupt();
                throw interrupted;
            } catch (Exception warmup) {
                closeQuietly(attemptClient);
                Utils.pollPause(connectDeadlineStart, 2000);
            }
        }
        Assert.assertNotNull(ws, "Could not establish the WebSocket connection for the throttling test");

        java.util.List<String> frames = new java.util.ArrayList<>();
        long framePacingStart = System.currentTimeMillis();
        try {
            for (int i = 0; i < messageCount; i++) {
                if (closed.get()) {
                    break;
                }
                try {
                    ws.sendText("throttle-msg-" + i, true).get(5, TimeUnit.SECONDS);
                } catch (InterruptedException interrupted) {
                    // Cancellation is not a transient/expected outcome — restore the flag and stop.
                    Thread.currentThread().interrupt();
                    throw interrupted;
                } catch (Exception sendFailed) {
                    break;   // connection dropped by the throttle handler
                }
                String frame = echoes.poll(3, TimeUnit.SECONDS);
                if (frame == null) {
                    break;   // nothing came back at all
                }
                frames.add(frame);
                if (isFrameError(frame)) {
                    break;   // the gateway answered with an error frame instead of the echo — stop sending
                }
                // Space the frames so the traffic manager's async throttle decision has time to be computed and
                // pushed back into the gateway's ThrottleDataHolder — a rapid burst outruns it and never trips.
                // The shared pacing primitive (§15), never a bare Thread.sleep.
                Utils.pollPause(framePacingStart, FRAME_PACING_MILLIS);
            }
        } finally {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            } catch (Exception ignore) {
                // already closed
            }
            closeQuietly(client);
        }

        String observed = "frames=" + frames + ", closed=" + closed.get() + ", closeCode=" + closeCode.get();
        // 1. The API routed at all. Without this the throttle assertion below would be satisfiable by an API that
        //    never worked (the vacuity that made the legacy `received < 10` pass at 0).
        Assert.assertFalse(frames.isEmpty(), "The WS API returned no frame at all, so nothing about its frame quota "
                + "can be concluded; " + observed);
        Assert.assertEquals(frames.get(0), "THROTTLE-MSG-0", "The first WS message was not echoed, so the API was "
                + "not routable and the throttle observation below would be meaningless; " + observed);
        // 2. No OTHER error code masquerading as a throttle (auth 4001/4002, blocked 4004, ...). Compared with
        //    equals, not contains: the frame body IS the whole getErrorResponseString(), so a substring match would
        //    also accept a longer frame that merely embedded the throttle text.
        for (String frame : frames) {
            Assert.assertTrue(!isFrameError(frame) || WS_THROTTLED_OUT_FRAME.equals(frame),
                    "The gateway returned a WS error frame that is NOT a throttle-out, so this run measured a "
                            + "different refusal: " + frame + "; " + observed);
        }
        // 3. The throttle itself, on the exact frame body the gateway emits for 4003.
        boolean throttled = frames.stream().anyMatch(WS_THROTTLED_OUT_FRAME::equals);
        Assert.assertTrue(throttled, "The gateway never returned the WS throttled-out frame (\""
                + WS_THROTTLED_OUT_FRAME + "\") within " + messageCount + " messages, so the API's frame quota was "
                + "not enforced; " + observed);
        // The measured trajectory, not just the verdict: WHICH send tripped is what confirms the both-direction
        // arithmetic (a send plus its echo costs two events), and a green run records nothing about it otherwise.
        long echoed = frames.stream().filter(frame -> !isFrameError(frame)).count();
        log.info("WS frame quota measured: " + echoed + " of " + messageCount + " offered sends were echoed before "
                + "the throttled-out frame arrived; " + observed);
    }

    /**
     * Whether a raw-WS inbound text frame is the gateway's own error frame rather than a backend echo. The gateway
     * writes {@code "Error code: <n> reason: <message>"} ({@code InboundProcessorResponseDTO#getErrorResponseString})
     * on the non-closing error branch, and the echo backend uppercases whatever it is sent, so the two are
     * distinguishable by that prefix.
     */
    private static boolean isFrameError(String frame) {
        return frame != null && frame.startsWith("Error code: ");
    }

    /** Builds the full gateway WS URL from an API context (context already carries {@code /t/<tenant>}). */
    private String buildWsUrl(String context) {
        return joinBase(Utils.getBaseGatewayWsUrl(), context);
    }

    /** Builds the full gateway SECURE (wss://) URL from an API context. */
    private String buildWssUrl(String context) {
        return joinBase(Utils.getBaseGatewayWssUrl(), context);
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
        Assert.assertEquals(echoOverWs(wsUrl, "Authorization", "Bearer " + token, message, timeoutSeconds,
                accessToken, token), expectedEcho, "Secure WebSocket (wss) echo mismatch.");
    }

    /** SECURE (wss://) WS invocation authenticated with an API key. */
    @When("I invoke the WebSocket API at gateway wss context {string} with message {string} using api key {string} expecting echo {string} within {int} seconds")
    public void invokeSecureWebSocketWithApiKey(String context, String message, String apiKey, String expectedEcho,
                                                int timeoutSeconds) throws Exception {
        String wsUrl = buildWssUrl(context);
        String key = TestContext.resolve(apiKey).toString();
        Assert.assertEquals(echoOverWs(wsUrl, "apikey", key, message, timeoutSeconds, apiKey, key), expectedEcho,
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
                timeoutSeconds, accessToken, TestContext.resolve(accessToken).toString()), expectedEcho,
                "WS (with Origin) echo mismatch.");
    }

    /** CORS negative: a DISALLOWED {@code Origin} is rejected at the WS handshake. */
    @When("I invoke the WebSocket API at gateway ws context {string} using access token {string} and origin {string} expecting rejection within {int} seconds")
    public void invokeWsWithOriginExpectRejection(String context, String accessToken, String origin,
                                                  int timeoutSeconds) throws Exception {
        expectRejection(buildWsUrl(context), authAndOrigin(accessToken, origin), timeoutSeconds);
    }

    private Map<String, String> authAndOrigin(String accessToken, String origin) {
        Map<String, String> headers = new LinkedHashMap<>();
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

    private Map<String, String> apiKeyAndXff(String apiKey, String xff) {
        Map<String, String> headers = new LinkedHashMap<>();
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
                timeoutSeconds, apiKey, TestContext.resolve(apiKey).toString()), expectedEcho,
                "WS (api-key + matching Referer) echo mismatch.");
    }

    /** API-key Referer restriction (WS negative): a NON-matching {@code Referer} is rejected at the handshake. */
    @When("I invoke the WebSocket API at gateway ws context {string} using api key {string} and referer {string} expecting rejection within {int} seconds")
    public void invokeWsApiKeyRefererReject(String context, String apiKey, String referer, int timeoutSeconds)
            throws Exception {
        expectRejection(buildWsUrl(context), apiKeyAndReferer(apiKey, referer), timeoutSeconds);
    }

    private Map<String, String> apiKeyAndReferer(String apiKey, String referer) {
        Map<String, String> headers = new LinkedHashMap<>();
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
        Assert.assertEquals(echoOverWs(wsUrl, new LinkedHashMap<>(), message, timeoutSeconds, apiKey, key),
                expectedEcho, "WS (api-key query param) echo mismatch.");
    }

    /** Query-parameter auth (WS positive, token): the token is presented as {@code ?access_token=<token>}. */
    @When("I invoke the WebSocket API at gateway ws context {string} with message {string} using access token query param {string} expecting echo {string} within {int} seconds")
    public void invokeWsTokenQueryParam(String context, String message, String accessToken, String expectedEcho,
                                        int timeoutSeconds) throws Exception {
        String token = TestContext.resolve(accessToken).toString();
        String wsUrl = appendQuery(buildWsUrl(context), "access_token", token);
        Assert.assertEquals(echoOverWs(wsUrl, new LinkedHashMap<>(), message, timeoutSeconds, accessToken, token),
                expectedEcho, "WS (token query param) echo mismatch.");
    }

    /** Appends a single URL-encoded query parameter to a WS URL. */
    private String appendQuery(String wsUrl, String name, String value) {
        String sep = wsUrl.contains("?") ? "&" : "?";
        return wsUrl + sep + name + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Connect+send+echo retry loop (single auth header); delegates to the multi-header form. */
    private String echoOverWs(String wsUrl, String headerName, String headerValue, String message,
                              int timeoutSeconds, String credentialContextKey, String credential) throws Exception {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(headerName, headerValue);
        return echoOverWs(wsUrl, headers, message, timeoutSeconds, credentialContextKey, credential);
    }

    /**
     * Connect+send+echo retry loop with arbitrary headers; returns the echo or fails after the deadline. The
     * credential is carried in only so a FAILING invocation can NAME it (its context key and jti) when the
     * gateway refused the handshake — the headers map alone cannot say which context key it came from.
     */
    private String echoOverWs(String wsUrl, Map<String, String> headers, String message, int timeoutSeconds,
                              String credentialContextKey, String credential) throws Exception {
        long startedMillis = System.currentTimeMillis();
        AtomicReference<Throwable> lastFailure = new AtomicReference<>();
        String received = Utils.retryUntil(timeoutSeconds * 1000L, () -> {
            try {
                return connectSendReceive(wsUrl, headers, message);
            } catch (InterruptedException interrupted) {
                // Cancellation is not a transient/expected outcome — restore the flag so the envelope's next
                // pause rethrows it instead of reading this as a retryable attempt.
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception connectingWhileWarmup) {
                lastFailure.set(connectingWhileWarmup);
                return null;
            }
        }, echo -> echo != null);
        String authDetail = received != null ? ""
                : authRejectionDetail(wsUrl, credentialContextKey, credential, lastFailure, startedMillis);
        Assert.assertNotNull(received, "WebSocket did not echo within the deadline; last error: "
                + lastErrorOf(lastFailure) + authDetail);
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
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(headerName, headerValue);
        expectRejection(wsUrl, headers, timeoutSeconds);
    }

    /**
     * Multi-header rejection check (e.g. Authorization + a disallowed Origin for CORS). Deliberately emits NO
     * auth-rejection diagnostic: a refused handshake is this step's EXPECTED outcome, and a marker that fires on
     * expected behaviour is noise.
     */
    private void expectRejection(String wsUrl, Map<String, String> headers, int timeoutSeconds)
            throws Exception {
        AtomicBoolean everEchoed = new AtomicBoolean(false);
        Boolean rejectionObserved = Utils.retryUntil(timeoutSeconds * 1000L, () -> {
            try {
                String echo = connectSendReceive(wsUrl, headers, "probe");
                if (echo == null) {
                    return Boolean.TRUE;   // no echo → rejected
                }
                everEchoed.set(true);   // accepted this round — enforcement may still be propagating; retry
                return null;
            } catch (InterruptedException interrupted) {
                // Cancellation is not a transient/expected outcome — restore the flag so the envelope's next
                // pause rethrows it instead of reading this as a retryable attempt.
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception rejected) {
                return Boolean.TRUE;   // handshake/upgrade refused → rejection confirmed
            }
        }, outcome -> outcome != null);
        if (Boolean.TRUE.equals(rejectionObserved)) {
            return;   // a rejection was observed — an earlier echo was only enforcement still propagating
        }
        Assert.assertFalse(everEchoed.get(), "Expected the WS invocation to be rejected within " + timeoutSeconds
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
        String base = Utils.getBaseGatewayWsUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String wsUrl = base + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;

        long startedMillis = System.currentTimeMillis();
        AtomicReference<Throwable> lastFailure = new AtomicReference<>();
        String data = Utils.retryUntil(timeoutSeconds * 1000L, () -> {
            try {
                return subscribeAndReceive(wsUrl, token, query, lastFailure);
            } catch (InterruptedException interrupted) {
                // Cancellation is not a transient/expected outcome — restore the flag so the envelope's next
                // pause rethrows it instead of reading this as a retryable attempt.
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception connectingWhileWarmup) {
                lastFailure.set(connectingWhileWarmup);
                return null;
            }
        }, message -> message != null && message.contains(expectedData));
        String authDetail = data != null ? ""
                : authRejectionDetail(wsUrl, accessToken, token, lastFailure, startedMillis);
        Assert.assertNotNull(data, "GraphQL subscription returned no data message within the deadline; last error: "
                + lastErrorOf(lastFailure) + authDetail);
        Assert.assertTrue(data.contains(expectedData),
                "GraphQL subscription data did not contain '" + expectedData + "'; last message: " + data);
    }

    /**
     * Regression assertion for the gateway WS tenant-flow leak (carbon-apimgt
     * {@code InboundWebSocketProcessor.handleHandshake} calls {@code startTenantFlow()}/{@code setTenantDomain()}
     * and never {@code endTenantFlow()}, leaking the handshake's tenant onto the shared netty event-loop thread).
     *
     * <p>Forces the interleave deterministically: opens the carbon.super victim graphql-ws connection, then during
     * the pre-{@code connection_init} idle window floods {@code floodCount} concurrent handshakes to the tenant WS
     * context — each reaches the gateway's {@code startTenantFlow} (poisoning the event-loop thread) before failing
     * auth — and only THEN sends {@code connection_init}. On an unfixed gateway the victim's frame-auth inherits the
     * leaked tenant, resolves the wrong key manager, and the gateway closes the connection with WS close 4001
     * "Invalid JWT token". This step ASSERTS THE FIXED behaviour: the subscription authenticates (receives
     * {@code connection_ack}, is NOT closed with 4001) despite the flood. It therefore FAILS on the current
     * (unfixed) image with an {@link AssertionError} carrying the 4001 detail — that failure is the reproduction —
     * and PASSES once the {@code endTenantFlow} fix ships in the gateway jar.
     */
    @When("I open a GraphQL subscription at gateway ws context {string} with query {string} using access token {string}, the subscription authenticates despite {int} concurrent handshakes to tenant ws context {string}")
    public void graphqlSubscriptionSurvivesTenantHandshakeFlood(String context, String query, String accessToken,
                                                                int floodCount, String tenantContext)
            throws Exception {
        String victimUrl = joinBase(Utils.getBaseGatewayWsUrl(), context);
        String tenantUrl = joinBase(Utils.getBaseGatewayWsUrl(), tenantContext);
        String token = TestContext.resolve(accessToken).toString();

        HttpClient client = HttpClient.newBuilder().sslContext(trustAllSslContext()).build();
        CompletableFuture<Void> ack = new CompletableFuture<>();
        CompletableFuture<String> closed = new CompletableFuture<>();
        WebSocket ws;
        try {
            ws = client.newWebSocketBuilder()
                    .header("Authorization", "Bearer " + token)
                    .subprotocols("graphql-ws")
                    .connectTimeout(java.time.Duration.ofSeconds(15))
                    .buildAsync(URI.create(victimUrl), new WebSocket.Listener() {
                        private final StringBuilder parts = new StringBuilder();

                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence part, boolean last) {
                            parts.append(part);
                            if (last) {
                                String msg = parts.toString();
                                parts.setLength(0);
                                if (msg.contains("connection_ack")) {
                                    ack.complete(null);
                                } else if (msg.contains("Invalid JWT") || msg.contains("\"type\":\"error\"")) {
                                    closed.complete("error-frame: " + msg);
                                }
                            }
                            webSocket.request(1);
                            return null;
                        }

                        @Override
                        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                            closed.complete(statusCode + ":" + reason);
                            return null;
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            closed.complete("onError:" + error);
                        }
                    }).get(20, TimeUnit.SECONDS);
        } catch (Exception connectFailed) {
            closeQuietly(client);
            Assert.fail("Victim graphql-ws handshake to " + victimUrl + " was refused before the flood could run "
                    + "(the victim carbon.super token must upgrade cleanly): " + connectFailed);
            return;   // unreachable; keeps ws definitely-assigned
        }
        try {
            // The gateway brings up the backend WS leg asynchronously after the client handshake (the plain
            // subscription flow sleeps here for the same reason). Use that idle window to flood the tenant
            // handshakes so every event-loop thread is poisoned BEFORE connection_init drives the victim's
            // frame-auth. No extra sleep beyond this backend-warmup wait.
            int reached = floodTenantHandshakes(tenantUrl, token, floodCount);
            log.info("[WS-LEAK] flooded " + reached + "/" + floodCount + " tenant handshakes during the idle window");

            ws.sendText("{\"type\":\"connection_init\",\"payload\":{}}", true);

            // Wait for connection_ack OR a close/error (the 4001 rejection). Whichever completes first.
            CompletableFuture.anyOf(ack, closed).get(30, TimeUnit.SECONDS);
            if (ack.isDone()) {
                return;   // authenticated despite the flood — the FIXED behaviour
            }
            String detail = closed.isDone() ? closed.get() : "no connection_ack and no close within the deadline";
            Assert.fail("GraphQL subscription was REJECTED while " + floodCount + " tenant handshakes flooded "
                    + tenantUrl + " — the gateway refused a VALID carbon.super token (close/error: " + detail
                    + "). This is the WS tenant-flow leak (InboundWebSocketProcessor.handleHandshake never calls "
                    + "endTenantFlow); a 4001 'Invalid JWT token' here reproduces the bug and is EXPECTED to fail on "
                    + "the current image, passing once the endTenantFlow fix is in the gateway jar.");
        } catch (java.util.concurrent.TimeoutException noVerdict) {
            // Neither ack nor a rejection arrived. Treat as inconclusive-but-failing: on the fixed gateway the ack
            // is prompt, so a timeout here means the victim never authenticated under the flood.
            Assert.fail("GraphQL subscription neither acked nor was rejected within the deadline while "
                    + floodCount + " tenant handshakes flooded " + tenantUrl + " (no connection_ack observed).");
        } finally {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            } catch (Exception closeFailure) {
                log.warn("Ignoring failure to send graphql-ws close frame: " + closeFailure.getMessage());
            }
            closeQuietly(client);
        }
    }

    /**
     * Fires {@code count} concurrent WS handshakes at {@code tenantUrl} carrying the victim's (carbon.super) token —
     * valid for the super tenant but INVALID for tenant1.com, so each fails auth AFTER the gateway has already run
     * {@code startTenantFlow()}/{@code setTenantDomain(tenant1.com)} on its event-loop thread (the leak). A
     * handshake that upgrades and one that is rejected both reach that leak, so both count as "reached". Returns how
     * many handshakes reached the server path. Ports {@code WsLeakRepro.floodTenant}.
     */
    private int floodTenantHandshakes(String tenantUrl, String token, int count) {
        AtomicInteger reached = new AtomicInteger(0);
        java.util.List<Thread> threads = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            // Dedicated thread per handshake (not CompletableFuture's common ForkJoinPool): these tasks BLOCK on the
            // WS upgrade, and the pool's bounded parallelism (cores-1) would serialise them and defeat the concurrent
            // flood. Matches floodLoop's dedicated-thread approach.
            Thread t = new Thread(() -> {
                HttpClient c = null;
                try {
                    c = HttpClient.newBuilder().sslContext(trustAllSslContext()).build();
                    WebSocket w = c.newWebSocketBuilder()
                            .header("Authorization", "Bearer " + token)
                            .connectTimeout(java.time.Duration.ofSeconds(4))
                            .buildAsync(URI.create(tenantUrl), new WebSocket.Listener() { })
                            .get(4, TimeUnit.SECONDS);
                    try {
                        w.sendClose(WebSocket.NORMAL_CLOSURE, "x");
                    } catch (Exception ignore) {
                        // already closing
                    }
                    reached.incrementAndGet();   // upgraded (also leaked)
                } catch (Exception rejectedOrTimedOut) {
                    // A 401/handshake failure STILL means the gateway reached startTenantFlow before auth → leaked.
                    reached.incrementAndGet();
                } finally {
                    if (c != null) {
                        closeQuietly(c);
                    }
                }
            }, "tenant-flood-" + i);
            t.setDaemon(true);
            t.start();
            threads.add(t);
        }
        for (Thread t : threads) {
            try {
                t.join(8000);   // a handshake that never returned within the budget is not counted
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }
        return reached.get();
    }

    /**
     * Starts a background, CONTINUOUS tenant-handshake flood: {@code concurrency} daemon threads each open→close WS
     * handshakes at {@code tenantContext} back-to-back (carrying the victim's carbon.super token, which is INVALID
     * for the tenant, so each reaches {@code startTenantFlow()}/{@code setTenantDomain(tenant)} before failing auth —
     * poisoning event-loop threads). Unlike the one-shot {@code floodTenantHandshakes} used by the auth canary, this
     * keeps threads poisoned for the WHOLE subscription arc that follows, so the subscribe frame's tenant-scoped
     * scope check (and throttle lookup) runs on a poisoned thread. Pair with {@code I stop the sustained flood}.
     * Each flood thread runs handshakes sequentially (its own thread) rather than via the common ForkJoinPool, so it
     * never starves the victim subscription's own async WS client.
     */
    @When("I start a sustained flood of {int} concurrent tenant handshakes to tenant ws context {string} using access token {string}")
    public void startSustainedTenantFlood(int concurrency, String tenantContext, String accessToken) {
        String tenantUrl = joinBase(Utils.getBaseGatewayWsUrl(), tenantContext);
        String token = TestContext.resolve(accessToken).toString();
        java.util.concurrent.atomic.AtomicBoolean running = new java.util.concurrent.atomic.AtomicBoolean(true);
        java.util.concurrent.atomic.AtomicInteger reached = new java.util.concurrent.atomic.AtomicInteger();
        java.util.List<Thread> threads = new java.util.ArrayList<>();
        for (int i = 0; i < concurrency; i++) {
            Thread t = new Thread(floodLoop(tenantUrl, token, running, reached), "sustained-tenant-flood-" + i);
            t.setDaemon(true);
            t.start();
            threads.add(t);
        }
        this.sustainedFloodRunning = running;
        this.sustainedFloodReached = reached;
        this.sustainedFloodThreads = threads;
        log.info("[WS-LEAK] sustained tenant-handshake flood started: " + concurrency + " threads -> " + tenantUrl);
    }

    /**
     * Stops the sustained flood started above, joins its threads, and asserts it actually reached the gateway at
     * least once — a zero-reach flood is a no-op that could not have exercised the tenant-leak interleave, so it must
     * fail loudly rather than give false confidence.
     */
    @Then("I stop the sustained flood")
    public void stopSustainedTenantFlood() throws InterruptedException {
        if (sustainedFloodRunning != null) {
            sustainedFloodRunning.set(false);
        }
        if (sustainedFloodThreads != null) {
            for (Thread t : sustainedFloodThreads) {
                t.join(8000);
            }
        }
        int total = sustainedFloodReached != null ? sustainedFloodReached.get() : 0;
        sustainedFloodThreads = null;
        sustainedFloodRunning = null;
        sustainedFloodReached = null;
        log.info("[WS-LEAK] sustained tenant-handshake flood stopped: " + total + " handshakes reached the gateway");
        Assert.assertTrue(total > 0, "The sustained tenant-handshake flood reached the gateway 0 times — it was a "
                + "no-op and could not have exercised the tenant-leak interleave (check the tenant WS context/token).");
    }

    /**
     * One flood thread's loop: open→close a tenant WS handshake back-to-back until stopped. Both an upgrade and a
     * rejection count as "reached" — either way the gateway ran startTenantFlow before auth (the poisoning point).
     */
    private Runnable floodLoop(String tenantUrl, String token,
                               java.util.concurrent.atomic.AtomicBoolean running,
                               java.util.concurrent.atomic.AtomicInteger reached) {
        return () -> {
            while (running.get()) {
                HttpClient c = null;
                try {
                    c = HttpClient.newBuilder().sslContext(trustAllSslContext()).build();
                    WebSocket w = c.newWebSocketBuilder()
                            .header("Authorization", "Bearer " + token)
                            .connectTimeout(java.time.Duration.ofSeconds(4))
                            .buildAsync(URI.create(tenantUrl), new WebSocket.Listener() { })
                            .get(4, TimeUnit.SECONDS);
                    reached.incrementAndGet();
                    try {
                        w.sendClose(WebSocket.NORMAL_CLOSURE, "x");
                    } catch (Exception ignore) {
                        // already closing
                    }
                } catch (Exception rejectedOrTimedOut) {
                    // A 401/handshake failure STILL means the gateway reached startTenantFlow before auth → poisoned.
                    reached.incrementAndGet();
                } finally {
                    if (c != null) {
                        closeQuietly(c);
                    }
                }
            }
        };
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
        String wsUrl = joinBase(Utils.getBaseGatewayWsUrl(), context);
        String token = TestContext.resolve(accessToken).toString();
        long startedMillis = System.currentTimeMillis();
        AtomicReference<Throwable> lastFailure = new AtomicReference<>();
        // The verdict below reports the LAST ESTABLISHED probe, so the counts are carried here rather than taken
        // from the envelope's return — a closing attempt that could not connect returns null.
        AtomicReference<int[]> lastProbe = new AtomicReference<>();
        AtomicReference<String> lastThrottleMsg = new AtomicReference<>();
        // Only accept once a frame was ACTUALLY throttled. The bandwidth quota is per-minute and accumulates
        // across probes, so under load — where a single probe can deliver too few frames to exceed the quota
        // inside its window — we re-subscribe and keep sending until the gateway throttles (4003) or the deadline
        // elapses, instead of giving up on the first established-but-not-yet-throttled probe (the cause of an
        // intermittent full-suite flake).
        Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT, () -> {
            AtomicInteger dataCount = new AtomicInteger();
            AtomicReference<String> tMsg = new AtomicReference<>();
            boolean established;
            try {
                established = subscribeAndProbe(wsUrl, token, query, frames, dataCount, tMsg, lastFailure);
            } catch (InterruptedException interrupted) {
                // Cancellation is not a transient failure — restore the flag so the envelope's next pause
                // rethrows it instead of reading this as a retryable probe.
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception transientFailure) {
                // transient ws failure — counts as not-established; re-probe within the deadline
                // (same tolerance as this file's other invocation loops)
                established = false;
            }
            if (!established) {
                return null;
            }
            lastProbe.set(new int[]{dataCount.get(), tMsg.get() != null ? 1 : 0});
            lastThrottleMsg.set(tMsg.get());
            return lastProbe.get();
        }, probe -> probe != null && probe[1] == 1);
        int[] result = lastProbe.get();
        String throttleMsg = lastThrottleMsg.get();
        String authDetail = result != null ? ""
                : authRejectionDetail(wsUrl, accessToken, token, lastFailure, startedMillis);
        Assert.assertNotNull(result, "Could not establish the graphql-ws subscription for the throttling test"
                + authDetail);
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
        String wsUrl = joinBase(Utils.getBaseGatewayWsUrl(), context);
        String token = TestContext.resolve(accessToken).toString();
        long startedMillis = System.currentTimeMillis();
        AtomicReference<Throwable> lastFailure = new AtomicReference<>();
        AtomicReference<String> lastMsg = new AtomicReference<>();
        String matched = Utils.retryUntil(timeoutSeconds * 1000L, () -> {
            String errorMsg;
            try {
                errorMsg = subscribeExpectError(wsUrl, token, query, lastFailure);
            } catch (InterruptedException interrupted) {
                // Cancellation is not a transient failure — restore the flag so the envelope's next pause
                // rethrows it instead of reading this as a retryable attempt.
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception transientFailure) {
                // transient ws failure — retry within the deadline (same tolerance as the other loops here)
                errorMsg = null;
            }
            if (errorMsg == null) {
                return null;
            }
            lastMsg.set(errorMsg);
            return errorMsg;
        }, errorMsg -> carriesErrorCode(errorMsg, expectedCode));
        if (carriesErrorCode(matched, expectedCode)) {
            System.out.println("[GQL-SUB-ERR] expected=" + expectedCode + " msg=" + matched);
            return;
        }
        // A refused handshake is NOT the expected outcome here: the expected error arrives as a graphql-ws frame
        // over an ESTABLISHED connection, so a 401 upgrade rejection is a genuine credential problem to report.
        Assert.fail("Expected graphql-ws error code " + expectedCode + " for query [" + query
                + "] but did not observe it; last error message: " + lastMsg.get()
                + authRejectionDetail(wsUrl, accessToken, token, lastFailure, startedMillis));
    }

    /** True when a graphql-ws message carries the given error code (the gateway emits it with and without a space). */
    private static boolean carriesErrorCode(String message, int expectedCode) {
        return message != null && (message.contains("\"code\":" + expectedCode)
                || message.contains("\"code\": " + expectedCode));
    }

    /** The last failed attempt's message — the {@code lastError} string the hand-rolled loops used to carry. */
    private static String lastErrorOf(AtomicReference<Throwable> lastFailure) {
        Throwable failure = lastFailure.get();
        return failure == null ? null : failure.getMessage();
    }

    /**
     * The HTTP status of a REFUSED WS upgrade, or 0 when the failure was not a handshake rejection. The whole cause
     * chain is walked: the authoritative status lives on the handshake exception's response, but that exception
     * carries no message of its own and the wording naming the status ("Unexpected HTTP response status code 401")
     * sits on a nested cause — so the text is a fallback for a JDK that shapes the chain differently. The fallback
     * matches the status WORDING rather than a bare "401", so an unrelated message never fakes a rejection.
     */
    private static int handshakeStatus(Throwable failure) {
        for (Throwable cause = failure; cause != null && cause != cause.getCause(); cause = cause.getCause()) {
            if (cause instanceof WebSocketHandshakeException handshake && handshake.getResponse() != null) {
                return handshake.getResponse().statusCode();
            }
            String message = cause.getMessage();
            if (message != null && (message.contains("status code 401") || message.contains("401 Unauthorized"))) {
                return 401;
            }
        }
        return 0;
    }

    /**
     * The auth-rejection diagnostic to append to a FAILING invocation step's message, or "" when nothing observed
     * pointed at a refused credential. Call ONLY from a failure path of a step that expects the handshake to
     * SUCCEED: a 401 on a step that passed, or on the rejection negatives that exist to provoke one, is not a
     * finding, and the one grep-able marker loses its meaning if it fires on expected behaviour.
     *
     * <p>The LAST failure is the one inspected, because a credential still refused when the window closed is
     * exactly the persistent rejection {@link Utils#logAuthRejection} describes (a warm-up 404/403 that later
     * cleared is not).
     */
    private static String authRejectionDetail(String what, String credentialContextKey, String credential,
                                              AtomicReference<Throwable> lastFailure, long startedMillis) {
        Throwable failure = lastFailure.get();
        if (failure == null || handshakeStatus(failure) != 401) {
            return "";
        }
        return " " + Utils.logAuthRejection(what, credentialContextKey, credential, 401, failure.getMessage(),
                System.currentTimeMillis() - startedMillis);
    }

    /** Best-effort close of a per-attempt WS HttpClient; a close failure must never mask the attempt's result. */
    private static void closeQuietly(HttpClient client) {
        try {
            client.close();
        } catch (Exception closeFailure) {
            // the client is being discarded anyway; log but never let cleanup override the return/throw
            log.warn("Ignoring failure to close WS HttpClient (possible resource leak): " + closeFailure.getMessage());
        }
    }

    /**
     * graphql-ws: init/ack, send one start(query), return the first {@code type:error} message (or null). A
     * refused upgrade is reported into {@code lastFailure} as well as logged: it is absorbed here as an
     * inconclusive attempt, so the caller could otherwise never say WHY its window closed empty.
     */
    private String subscribeExpectError(String wsUrl, String token, String query,
                                        AtomicReference<Throwable> lastFailure) throws Exception {
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
        } catch (InterruptedException interrupted) {
            // Cancellation must reach the caller's poll, not read as an inconclusive attempt.
            Thread.currentThread().interrupt();
            closeQuietly(client);
            throw interrupted;
        } catch (Exception connectFailed) {
            log.warn("graphql-ws connect failed (inconclusive attempt, caller will retry): " + connectFailed.getMessage());
            lastFailure.set(connectFailed);
            closeQuietly(client);
            return null;
        }
        try {
            // A complexity/depth rejection is produced by the gateway QueryAnalyzer at the START frame and needs no
            // backend WS leg, so no long pre-init wait is required here — a short settle just avoids racing the
            // handshake on a freshly-opened connection. Anything slower (cold route/warm-up) is absorbed by the
            // caller RETRYING (this method returns null below), NOT by a bigger blind sleep.
            Thread.sleep(2000);
            ws.sendText("{\"type\":\"connection_init\",\"payload\":{}}", true);
            ack.get(15, TimeUnit.SECONDS);
            String start = "{\"id\":\"1\",\"type\":\"start\",\"payload\":{\"variables\":{},\"extensions\":{},"
                    + "\"operationName\":null,\"query\":\"" + query.replace("\"", "\\\"") + "\"}}";
            ws.sendText(start, true);
            return error.get(20, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException e) {
            // init/ack/start/error did not complete in time — an INCONCLUSIVE attempt, not a test failure. Return
            // null so the caller's retry-until-deadline loop tries again; the loop asserts a clear failure if no
            // attempt ever observes the expected code. (Previously the ack.get timeout was unguarded and escaped
            // the retry loop as a bare TimeoutException — the recurring CI flake this fixes.)
            log.warn("graphql-ws init/ack/error wait did not complete (inconclusive attempt, caller will retry): "
                    + e.getMessage());
            return null;
        } finally {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            } catch (Exception closeFailure) {
                // best-effort close of an already-closing/failed WS — logged, never allowed to skip closeQuietly below
                log.warn("Ignoring failure to send graphql-ws close frame: " + closeFailure.getMessage());
            }
            closeQuietly(client);
        }
    }

    /**
     * graphql-ws multi-frame probe: init/ack, send N start-frames, capture data-count + any throttle message. A
     * refused upgrade is reported into {@code lastFailure} (see {@link #subscribeExpectError}).
     */
    private boolean subscribeAndProbe(String wsUrl, String token, String query, int frames,
                                      AtomicInteger dataCount, AtomicReference<String> throttleMsg,
                                      AtomicReference<Throwable> lastFailure)
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
        } catch (InterruptedException interrupted) {
            // Cancellation must reach the caller's poll, not read as an inconclusive attempt.
            Thread.currentThread().interrupt();
            closeQuietly(client);
            throw interrupted;
        } catch (Exception connectFailed) {
            log.warn("graphql-ws connect failed (inconclusive attempt, caller will retry): " + connectFailed.getMessage());
            lastFailure.set(connectFailed);
            closeQuietly(client);
            return false;
        }
        try {
            Thread.sleep(15000);   // let the gateway bring up the backend WS leg (frames need the backend relay)
            ws.sendText("{\"type\":\"connection_init\",\"payload\":{}}", true);
            ack.get(15, TimeUnit.SECONDS);
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
        } catch (java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException e) {
            // ack/send did not complete in time — inconclusive attempt. Return false so the caller retries;
            // never let a slow handshake escape as a bare TimeoutException that aborts the test.
            log.warn("graphql-ws ack/send did not complete (inconclusive attempt, caller will retry): "
                    + e.getMessage());
            return false;
        } finally {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            } catch (Exception closeFailure) {
                // best-effort close of an already-closing/failed WS — logged, never allowed to skip closeQuietly below
                log.warn("Ignoring failure to send graphql-ws close frame: " + closeFailure.getMessage());
            }
            closeQuietly(client);
        }
    }

    /**
     * graphql-ws flow: connection_init → connection_ack → start(query) → first data message. A refused upgrade is
     * reported into {@code lastFailure} (see {@link #subscribeExpectError}).
     */
    private String subscribeAndReceive(String wsUrl, String token, String query,
                                       AtomicReference<Throwable> lastFailure) throws Exception {
        HttpClient client = HttpClient.newBuilder().sslContext(trustAllSslContext()).build();
        CompletableFuture<Void> ack = new CompletableFuture<>();
        CompletableFuture<String> data = new CompletableFuture<>();
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
                                data.complete(msg);
                            }
                        }
                        webSocket.request(1);
                        return null;
                    }
                })
                .get(20, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            // Cancellation must reach the caller's poll, not read as an inconclusive attempt.
            Thread.currentThread().interrupt();
            closeQuietly(client);
            throw interrupted;
        } catch (Exception connectFailed) {
            log.warn("graphql-ws connect failed (inconclusive attempt, caller will retry): " + connectFailed.getMessage());
            lastFailure.set(connectFailed);
            closeQuietly(client);
            return null;
        }
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
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            } catch (Exception closeFailure) {
                // best-effort close of an already-closing/failed WS — logged, never allowed to skip closeQuietly below
                log.warn("Ignoring failure to send graphql-ws close frame: " + closeFailure.getMessage());
            }
            closeQuietly(client);
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
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(headerName, headerValue);
        return connectSendReceive(wsUrl, headers, message);
    }

    /** Multi-header variant (e.g. Authorization + Origin for CORS). Sets each header on the WS upgrade request. */
    private String connectSendReceive(String wsUrl, Map<String, String> headers, String message)
            throws Exception {
        HttpClient client = HttpClient.newBuilder().sslContext(trustAllSslContext()).build();
        CompletableFuture<String> response = new CompletableFuture<>();
        WebSocket.Builder builder = client.newWebSocketBuilder();
        for (Map.Entry<String, String> e : headers.entrySet()) {
            builder.header(e.getKey(), e.getValue());
        }
        WebSocket ws = null;
        try {
            ws = builder
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
            ws.sendText(message, true);
            return response.get(20, TimeUnit.SECONDS);
        } finally {
            if (ws != null) {
                try {
                    ws.sendClose(WebSocket.NORMAL_CLOSURE, "done");
                } catch (Exception closeFailure) {
                    log.warn("Ignoring failure to send WebSocket close frame: " + closeFailure.getMessage());
                }
            }
            closeQuietly(client);
        }
    }
}
