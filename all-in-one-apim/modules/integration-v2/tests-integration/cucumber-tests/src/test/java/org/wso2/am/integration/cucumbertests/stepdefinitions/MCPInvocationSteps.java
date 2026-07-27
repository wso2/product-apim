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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gateway MCP-server invocation glue (ports the invoke half of MCPServerTestCase). Drives the MCP JSON-RPC
 * handshake through the gateway's {@code /mcp} path: initialize → tools/list → tools/call, PROPAGATING the
 * {@code Mcp-Session-Id} the backend issues on initialize. This deliberately exercises a REAL session-stateful
 * MCP server (the official-SDK node mock), closing the gap left by the legacy's stateless WireMock stub — it
 * verifies the APIM gateway correctly proxies MCP session state and SSE-framed responses.
 *
 * <p>Uses the JDK {@link java.net.http.HttpClient} (with an explicit trust-all SSLContext for the https gateway)
 * so it can read the {@code Mcp-Session-Id} response header and handle either an SSE ({@code data:} framed) or a
 * plain-JSON response body.</p>
 *
 * <p>Every step here retries its whole handshake through {@link Utils#retryUntil}, so none of them hand-rolls a
 * deadline, the poll cadence or the retry-only-on-IOException policy again.</p>
 */
public class MCPInvocationSteps {

    private static final String INIT = "{\"jsonrpc\":\"2.0\",\"id\":0,\"method\":\"initialize\",\"params\":{"
            + "\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},"
            + "\"clientInfo\":{\"name\":\"apim-it\",\"version\":\"1.0\"}}}";
    private static final String INITIALIZED = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}";
    private static final String TOOLS_LIST = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}";

    /**
     * Full MCP round-trip through the gateway: initialize (capture Mcp-Session-Id) → tools/list (must contain
     * {@code toolName}) → tools/call {@code toolName}(args) (result must contain {@code expected}). Retries the
     * whole flow until it succeeds or the deadline elapses (a freshly published MCP server takes a moment to
     * become routable). The context already carries any {@code /t/<tenant>} prefix.
     */
    @When("I invoke the MCP tool {string} with arguments {string} at gateway context {string} version {string} using access token {string} expecting result containing {string} within {int} seconds")
    public void invokeMcpTool(String toolName, String argsJson, String context, String version, String accessToken,
                              String expected, int timeoutSeconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String token = TestContext.resolve(accessToken).toString();
        String base = Utils.getBaseGatewayUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String mcpUrl = base + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext + "/" + version + "/mcp";

        HttpClient client = newClient();
        // Cross-attempt state must be assignable from inside the attempt lambda, hence the atomics.
        AtomicReference<String> lastError = new AtomicReference<>();
        AtomicInteger lastStatus = new AtomicInteger(-1);
        long startedMillis = System.currentTimeMillis();
        // The envelope floors the wait at the shared Constants.RUNTIME_PROPAGATION_TIMEOUT rather than this
        // loop's former local 30s floor — one module-wide ceiling, so no MCP step can silently drift below it.
        String callResult = Utils.retryUntil(timeoutSeconds * 1000L, () -> {
            try {
                // 1) initialize — capture the session id the backend issues
                HttpResponse<String> initResp = post(client, mcpUrl, token, null, INIT);
                lastStatus.set(initResp.statusCode());
                String sessionId = initResp.headers().firstValue("mcp-session-id").orElse(null);
                if (initResp.statusCode() != 200 || !sseOrJson(initResp.body()).contains("serverInfo")) {
                    lastError.set("init status=" + initResp.statusCode() + " body=" + initResp.body());
                    return null;
                }
                // 2) notifications/initialized (best-effort, same session)
                post(client, mcpUrl, token, sessionId, INITIALIZED);
                // 3) tools/list — must advertise the tool (proves the session carried past initialize)
                HttpResponse<String> listResp = post(client, mcpUrl, token, sessionId, TOOLS_LIST);
                lastStatus.set(listResp.statusCode());
                String listBody = sseOrJson(listResp.body());
                if (!listBody.contains(toolName)) {
                    lastError.set("tools/list did not contain '" + toolName + "': " + listBody);
                    return null;
                }
                // 4) tools/call — the actual stateful round-trip to the real MCP server
                String callPayload = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{"
                        + "\"name\":\"" + toolName + "\",\"arguments\":" + argsJson + "}}";
                HttpResponse<String> callResp = post(client, mcpUrl, token, sessionId, callPayload);
                lastStatus.set(callResp.statusCode());
                String body = sseOrJson(callResp.body());
                if (callResp.statusCode() == 200 && body.contains(expected)) {
                    return body;
                }
                lastError.set("tools/call status=" + callResp.statusCode() + " body=" + body);
                return null;
            } catch (IOException transientDuringWarmup) {
                lastError.set(transientDuringWarmup.getMessage());
                return null;
            }
        }, result -> true);
        if (callResult == null) {
            Assert.fail("MCP tool call did not return a result containing '" + expected + "' within the deadline; "
                    + "last: " + lastError.get()
                    + authRejectionDetail(mcpUrl, accessToken, token, lastStatus.get(), lastError.get(),
                            startedMillis));
        }
    }

    /**
     * Multi-call in ONE MCP session: initialize once (capture the session), send notifications/initialized, then
     * run several tools/call on the SAME {@code Mcp-Session-Id}, asserting each result contains its expected
     * marker. Proves the gateway PERSISTS MCP session state across calls — a check a stateless mock cannot do.
     * {@code calls} format: {@code tool|argsJson|expected ; tool|argsJson|expected} (semicolon-separated).
     */
    @When("I invoke MCP tools in one session at gateway context {string} version {string} using access token {string} with calls {string} within {int} seconds")
    public void invokeMcpMultiCall(String context, String version, String accessToken, String calls,
                                   int timeoutSeconds) throws Exception {
        String mcpUrl = buildMcpUrl(context, version);
        String token = TestContext.resolve(accessToken).toString();
        String[] specs = calls.split(";");

        HttpClient client = newClient();
        AtomicReference<String> lastError = new AtomicReference<>();
        AtomicInteger lastStatus = new AtomicInteger(-1);
        long startedMillis = System.currentTimeMillis();
        Boolean allCallsOk = Utils.retryUntil(timeoutSeconds * 1000L, () -> {
            try {
                HttpResponse<String> initResp = post(client, mcpUrl, token, null, INIT);
                lastStatus.set(initResp.statusCode());
                String sessionId = initResp.headers().firstValue("mcp-session-id").orElse(null);
                if (initResp.statusCode() != 200 || !sseOrJson(initResp.body()).contains("serverInfo")) {
                    lastError.set("init status=" + initResp.statusCode());
                    return null;
                }
                post(client, mcpUrl, token, sessionId, INITIALIZED);
                boolean allOk = true;
                String detail = "";
                for (String spec : specs) {
                    String[] p = spec.trim().split("\\|", 3);
                    String payload = "{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"tools/call\",\"params\":{"
                            + "\"name\":\"" + p[0].trim() + "\",\"arguments\":" + p[1].trim() + "}}";
                    HttpResponse<String> callResp = post(client, mcpUrl, token, sessionId, payload);
                    lastStatus.set(callResp.statusCode());
                    String body = sseOrJson(callResp.body());
                    if (!body.contains(p[2].trim())) {
                        allOk = false;
                        detail = "call " + p[0].trim() + " missing '" + p[2].trim() + "': " + body;
                        break;
                    }
                }
                if (allOk) {
                    return Boolean.TRUE;
                }
                lastError.set(detail);
                return null;
            } catch (IOException transientDuringWarmup) {
                lastError.set(transientDuringWarmup.getMessage());
                return null;
            }
        }, result -> true);
        if (allCallsOk == null) {
            Assert.fail("MCP multi-call session did not satisfy all calls within the deadline; last: "
                    + lastError.get()
                    + authRejectionDetail(mcpUrl, accessToken, token, lastStatus.get(), lastError.get(),
                            startedMillis));
        }
    }

    /**
     * Asserts the gateway's {@code tools/list} — via the full handshake (initialize → notifications/initialized
     * → tools/list) — advertises every tool in {@code expectedCsv} and NONE of the tools in {@code absentCsv}.
     * The advertised names are PARSED from {@code result.tools[].name} and compared as a set (order-independent;
     * mirrors upstream PR #14237's hardening of the legacy exact-JSON tool-list compare, which flaked because
     * tool order is not guaranteed). Retries the whole flow to ride out publish/redeploy propagation.
     */
    @When("I list MCP tools at gateway context {string} version {string} using access token {string} expecting tools {string} and not {string} within {int} seconds")
    public void listMcpToolsExpecting(String context, String version, String accessToken, String expectedCsv,
                                      String absentCsv, int timeoutSeconds) throws Exception {
        String mcpUrl = buildMcpUrl(context, version);
        String token = TestContext.resolve(accessToken).toString();
        java.util.List<String> expected = java.util.Arrays.asList(expectedCsv.split("\\s*,\\s*"));
        java.util.List<String> absent = java.util.Arrays.asList(absentCsv.split("\\s*,\\s*"));

        HttpClient client = newClient();
        AtomicReference<String> lastError = new AtomicReference<>();
        AtomicInteger lastStatus = new AtomicInteger(-1);
        long startedMillis = System.currentTimeMillis();
        java.util.Set<String> converged = Utils.retryUntil(timeoutSeconds * 1000L, () -> {
            try {
                HttpResponse<String> initResp = post(client, mcpUrl, token, null, INIT);
                lastStatus.set(initResp.statusCode());
                String sessionId = initResp.headers().firstValue("mcp-session-id").orElse(null);
                if (initResp.statusCode() != 200 || !sseOrJson(initResp.body()).contains("serverInfo")) {
                    lastError.set("init status=" + initResp.statusCode());
                    return null;
                }
                post(client, mcpUrl, token, sessionId, INITIALIZED);
                HttpResponse<String> listResp = post(client, mcpUrl, token, sessionId, TOOLS_LIST);
                lastStatus.set(listResp.statusCode());
                String listBody = sseOrJson(listResp.body());
                // Guard before parsing: a non-200 / empty tools/list must surface its status+body through the
                // retry diagnostics, not as an opaque JSONException message in lastError.
                if (listResp.statusCode() != 200 || listBody == null || listBody.isBlank()) {
                    lastError.set("tools/list status=" + listResp.statusCode() + " body=" + listBody);
                    return null;
                }
                // Parse the advertised names from result.tools[].name — never substring-match the raw body
                // (a tool name appearing inside another tool's description would false-positive).
                java.util.Set<String> names = new java.util.HashSet<>();
                JSONArray tools = new JSONObject(listBody).getJSONObject("result").getJSONArray("tools");
                for (int i = 0; i < tools.length(); i++) {
                    names.add(tools.getJSONObject(i).getString("name"));
                }
                boolean ok = names.containsAll(expected);
                for (String a : absent) {
                    ok = ok && !names.contains(a);
                }
                if (ok) {
                    return names;
                }
                lastError.set("advertised tools " + names + " (expected all of " + expected
                        + ", none of " + absent + ")");
                return null;
            } catch (IOException | JSONException transientDuringWarmup) {
                lastError.set(transientDuringWarmup.getMessage());
                return null;
            }
        }, result -> true);
        if (converged == null) {
            Assert.fail("Gateway tools/list did not converge to the expected tool set within the deadline; last: "
                    + lastError.get()
                    + authRejectionDetail(mcpUrl, accessToken, token, lastStatus.get(), lastError.get(),
                            startedMillis));
        }
    }

    /**
     * Invokes an MCP tool expecting a JSON-RPC ERROR (a method/tool error, not a result) — e.g. calling a tool
     * that is not exposed. Asserts the response carries an error indicator (`error`/`isError`). Validates the
     * gateway relays MCP error semantics end-to-end.
     */
    @When("I invoke the MCP tool {string} with arguments {string} at gateway context {string} version {string} using access token {string} expecting an error within {int} seconds")
    public void invokeMcpToolExpectError(String toolName, String argsJson, String context, String version,
                                         String accessToken, int timeoutSeconds) throws Exception {
        String mcpUrl = buildMcpUrl(context, version);
        String token = TestContext.resolve(accessToken).toString();

        HttpClient client = newClient();
        AtomicReference<String> lastError = new AtomicReference<>();
        AtomicInteger lastStatus = new AtomicInteger(-1);
        long startedMillis = System.currentTimeMillis();
        Boolean errored = Utils.retryUntil(timeoutSeconds * 1000L, () -> {
            try {
                HttpResponse<String> initResp = post(client, mcpUrl, token, null, INIT);
                lastStatus.set(initResp.statusCode());
                String sessionId = initResp.headers().firstValue("mcp-session-id").orElse(null);
                if (initResp.statusCode() != 200 || !sseOrJson(initResp.body()).contains("serverInfo")) {
                    lastError.set("init status=" + initResp.statusCode());
                    return null;
                }
                post(client, mcpUrl, token, sessionId, INITIALIZED);
                String callPayload = "{\"jsonrpc\":\"2.0\",\"id\":8,\"method\":\"tools/call\",\"params\":{"
                        + "\"name\":\"" + toolName + "\",\"arguments\":" + argsJson + "}}";
                HttpResponse<String> callResp = post(client, mcpUrl, token, sessionId, callPayload);
                lastStatus.set(callResp.statusCode());
                String body = sseOrJson(callResp.body());
                if (body.toLowerCase().contains("error") || callResp.statusCode() >= 400) {
                    return Boolean.TRUE;
                }
                lastError.set("expected an error but got status=" + callResp.statusCode() + " body=" + body);
                return null;
            } catch (IOException transientDuringWarmup) {
                lastError.set(transientDuringWarmup.getMessage());
                return null;
            }
        }, result -> true);
        if (errored == null) {
            // A 401 can only be the LAST status here if the HANDSHAKE was rejected on every attempt: any 4xx on
            // the tools/call itself already satisfies this step. So the marker still means a broken credential,
            // never this step's own expected (JSON-RPC level) error.
            Assert.fail("MCP tool call did not return an error within the deadline; last: " + lastError.get()
                    + authRejectionDetail(mcpUrl, accessToken, token, lastStatus.get(), lastError.get(),
                            startedMillis));
        }
    }

    /**
     * Negative auth: attempts a tool INVOCATION with an INVALID bearer token and asserts the gateway rejects it
     * with the EXACT {@code expectedStatus} (strict — so a future change of code is caught as a regression).
     * FINDINGS (verify-first): (a) the gateway does NOT authenticate the MCP {@code initialize} handshake (200
     * even with a bad token) — auth is enforced at {@code tools/call}; (b) the rejection code differs by
     * subtype — the proxy subtype returns 401, the DirectBackend (OpenAPI) subtype returns 403 — so each
     * feature asserts its own exact code. Retries only to ride out warm-up.
     */
    @When("I invoke the MCP server at gateway context {string} version {string} with an invalid token expecting status {int} within {int} seconds")
    public void invokeMcpInvalidToken(String context, String version, int expectedStatus, int timeoutSeconds)
            throws Exception {
        String mcpUrl = buildMcpUrl(context, version);
        String badToken = "invalid-mcp-token-xyz";
        String callPayload = "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\",\"params\":{"
                + "\"name\":\"echo\",\"arguments\":{\"message\":\"x\"}}}";
        HttpClient client = newClient();
        AtomicInteger last = new AtomicInteger(-1);
        // No auth-rejection marker in this step: the rejection IS what it asserts (a deliberately invalid
        // token), so logging one would fire on expected behaviour.
        Integer matched = Utils.retryUntil(timeoutSeconds * 1000L, () -> {
            try {
                // A tool call with a bad token — the handshake may 200, but the invocation must be rejected.
                HttpResponse<String> initResp = post(client, mcpUrl, badToken, null, INIT);
                if (initResp.statusCode() == expectedStatus) {
                    return initResp.statusCode(); // rejected already at the handshake with the exact code
                }
                String sessionId = initResp.headers().firstValue("mcp-session-id").orElse(null);
                post(client, mcpUrl, badToken, sessionId, INITIALIZED);
                HttpResponse<String> callResp = post(client, mcpUrl, badToken, sessionId, callPayload);
                last.set(callResp.statusCode());
                if (last.get() == expectedStatus) {
                    return callResp.statusCode();
                }
                return null;
            } catch (IOException transientDuringWarmup) {
                // retry
                return null;
            }
        }, result -> true);
        if (matched == null) {
            Assert.fail("MCP invalid-token tool call expected status " + expectedStatus + " but last was "
                    + last.get());
        }
    }

    /**
     * Invokes an MCP tool and asserts the gateway returns the expected HTTP status on the {@code tools/call}
     * (e.g. 200 with the required scope, 403 without). Retries until the status matches or the deadline elapses
     * (rides out warm-up, where a not-yet-routable server returns 404). Used by scope-enforcement.
     */
    @When("I invoke the MCP tool {string} with arguments {string} at gateway context {string} version {string} using access token {string} expecting status {int} within {int} seconds")
    public void invokeMcpToolExpectStatus(String toolName, String argsJson, String context, String version,
                                          String accessToken, int expectedStatus, int timeoutSeconds)
            throws Exception {
        String mcpUrl = buildMcpUrl(context, version);
        String token = TestContext.resolve(accessToken).toString();
        String callPayload = "{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"tools/call\",\"params\":{"
                + "\"name\":\"" + toolName + "\",\"arguments\":" + argsJson + "}}";

        HttpClient client = newClient();
        AtomicInteger last = new AtomicInteger(-1);
        AtomicReference<String> lastBody = new AtomicReference<>();
        long startedMillis = System.currentTimeMillis();
        Integer matched = Utils.retryUntil(timeoutSeconds * 1000L, () -> {
            try {
                HttpResponse<String> initResp = post(client, mcpUrl, token, null, INIT);
                // Throttling (429) can trip on ANY /mcp request (the handshake counts too), so when a 429 is
                // expected, a 429 at initialize is a valid "throttled" signal — don't require it on tools/call.
                if (expectedStatus == 429 && initResp.statusCode() == 429) {
                    return initResp.statusCode();
                }
                String sessionId = initResp.headers().firstValue("mcp-session-id").orElse(null);
                post(client, mcpUrl, token, sessionId, INITIALIZED);
                HttpResponse<String> callResp = post(client, mcpUrl, token, sessionId, callPayload);
                last.set(callResp.statusCode());
                lastBody.set(sseOrJson(callResp.body()));
                if (last.get() == expectedStatus) {
                    return callResp.statusCode();
                }
                return null;
            } catch (IOException transientDuringWarmup) {
                // retry
                return null;
            }
        }, result -> true);
        if (matched == null) {
            // Only when 401 was NOT the asserted outcome: the proxy subtype's invalid-token scenarios assert 401
            // on purpose, and a marker on an expected rejection is noise.
            String authDetail = expectedStatus == 401 ? ""
                    : authRejectionDetail(mcpUrl, accessToken, token, last.get(), lastBody.get(), startedMillis);
            Assert.fail("MCP tool call expected status " + expectedStatus + " but last was " + last.get()
                    + authDetail);
        }
    }

    /**
     * The auth-rejection marker for a FAILED MCP step: emitted only when the LAST response the retry envelope
     * saw was a 401, i.e. the gateway rejected the credential on every attempt over the whole window — which is
     * a revoked/invalidated credential, not propagation (see {@link Utils#logAuthRejection}). Returns "" for
     * anything else so callers can append it unconditionally to their failure message, and is never reached on a
     * passing path.
     */
    private static String authRejectionDetail(String what, String credentialContextKey, String credential,
                                              int lastStatus, String lastError, long startedMillis) {
        if (lastStatus != 401) {
            return "";
        }
        return " " + Utils.logAuthRejection(what, credentialContextKey, credential, 401, lastError,
                System.currentTimeMillis() - startedMillis);
    }

    /** Builds the gateway MCP endpoint URL: {@code <gatewayWs-less base>/<context>/<version>/mcp}. */
    private String buildMcpUrl(String context, String version) {
        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String base = Utils.getBaseGatewayUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext + "/" + version + "/mcp";
    }

    /**
     * Builds the trust-all client ONCE per step, OUTSIDE the retry envelope: MCP session state rides the
     * {@code Mcp-Session-Id} header rather than the connection, so one client serves every attempt, and the SSL
     * setup's checked exception (a genuine test-harness fault) stays out of the envelope's transient-IOException
     * policy instead of being retried as warm-up.
     */
    private HttpClient newClient() throws Exception {
        return HttpClient.newBuilder().sslContext(trustAll()).connectTimeout(Duration.ofSeconds(15)).build();
    }

    private HttpResponse<String> post(HttpClient client, String url, String token, String sessionId, String body)
            throws IOException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (sessionId != null) {
            b.header("Mcp-Session-Id", sessionId);
        }
        try {
            return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException interrupted) {
            // A retry attempt may only throw IOException, so restore the interrupt flag and hand the envelope an
            // IOException: its next poll pause rethrows the InterruptedException, keeping cancellation prompt
            // instead of letting it be mistaken for a completed attempt.
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while posting to " + url, interrupted);
        }
    }

    /** Extracts the JSON payload from an SSE ({@code data:} framed) body, or returns the body unchanged. */
    private String sseOrJson(String body) {
        if (body == null) {
            return "";
        }
        int idx = body.lastIndexOf("data:");
        if (idx < 0) {
            return body;
        }
        String rest = body.substring(idx + "data:".length());
        int nl = rest.indexOf('\n');
        return (nl >= 0 ? rest.substring(0, nl) : rest).trim();
    }

    private SSLContext trustAll() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) { }
            public void checkServerTrusted(X509Certificate[] chain, String authType) { }
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }}, new SecureRandom());
        return sslContext;
    }
}
