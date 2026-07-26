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
import io.cucumber.java.en.When;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway runtime-invocation steps. Invokes deployed APIs through the block's gateway (REST/SOAP, by access
 * token or API key, with optional retry-until-status for eventual consistency), plus the OpenID userinfo
 * endpoint. The tenant context segment of the invocation URL is derived from the scenario's acting actor
 * ({@link Identity#actingActor()}) rather than the retired {@code currentTenant} pointer.
 */
public class APIInvocationSteps {

    /** Context key under which every invocation publishes its response for the following assertion step. */
    private static final String HTTP_RESPONSE_KEY = "httpResponse";

    private String getBaseGatewayUrl() {

        Object baseGatewayUrl = TestContext.get("baseGatewayUrl");
        if (baseGatewayUrl == null) {
            throw new IllegalStateException("baseGatewayUrl is not available in the test context yet");
        }
        return baseGatewayUrl.toString();
    }

    /** Tenant domain for the invocation URL — taken from the scenario's acting actor. */
    private String actingTenantDomain() {
        return Identity.actingActor().getUserDomain();
    }

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
        String endpointUrl = getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
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
            long warmupDeadline = System.currentTimeMillis() + Constants.DEPLOYMENT_WAIT_TIME;
            HttpResponse response = null;
            while (System.currentTimeMillis() < warmupDeadline) {
                try {
                    response = invokeApiByContext(resolvedContext, httpMethod, accessToken, payload);
                    break;
                } catch (IOException transientDuringWarmup) {
                    Thread.sleep(2000);
                }
            }
            Assert.assertNotNull(response, "Invocation " + i + " of " + times + " never completed (gateway "
                    + "unreachable within the warmup window).");
            Assert.assertEquals(response.getResponseCode(), 200, "Invocation " + i + " of " + times
                    + " was not 200 (throttle tier change did not raise the limit as expected); last response: "
                    + response.getData());
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
        // Never wait less than the global deployment window: a freshly published API's gateway route can take
        // longer than a short per-step value to become routable, especially under load. The feature's value
        // can still request MORE than the floor.
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                last = invokeApiByContext(resolvedContext, httpMethod, accessToken, payload);
                if (last.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Only transient connectivity errors (e.g. an SSL handshake against a gateway listener still
                // coming up after a restart or fresh deploy) are retried. A bad token/key context key throws
                // IllegalArgumentException, which is NOT caught here so it fails fast rather than as a timeout.
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        assertReachedExpectedStatus(last, expectedStatus);
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
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                last = invokeApiByRawContext(resolvedContext, accessToken);
                if (last.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Only transient connectivity errors are retried (see invokeApiByContextUntilStatus).
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        assertReachedExpectedStatus(last, expectedStatus);
    }

    /** GET a full gateway context with the raw (un-normalized) path; publishes the response to {@code httpResponse}. */
    private HttpResponse invokeApiByRawContext(String resolvedContext, String accessToken) throws IOException {

        String actualAccessToken = TestContext.resolve(accessToken).toString();
        // Join base + context with exactly one slash. The default invoke relies on the client's URI
        // normalization to collapse a "//", but doGetRaw disables normalization, so a double slash would reach
        // the gateway verbatim and be rejected as "Invalid URL".
        String base = getBaseGatewayUrl();
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
        String endpointUrl = getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                last = execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl,
                        new HashMap<>(), "");
                if (last.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Retry transient connectivity only (see invokeApiByContextUntilStatus).
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        assertReachedExpectedStatus(last, expectedStatus);
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
        String endpointUrl = getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        Map<String, String> headers = new HashMap<>();
        headers.put("Origin", origin);
        headers.put("Access-Control-Request-Method", requestMethod);
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                last = execute(CurlOption.HttpMethod.OPTIONS, endpointUrl, new HashMap<>(headers), "");
                if (last.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Retry transient connectivity only (see invokeApiByContextUntilStatus).
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        assertReachedExpectedStatus(last, expectedStatus);
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
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                last = invokeApiByContext(resolvedContext, httpMethod, accessToken, payload, headerName);
                if (last.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Retry transient connectivity only (see invokeApiByContextUntilStatus).
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        assertReachedExpectedStatus(last, expectedStatus);
    }

    /**
     * Invokes a deployed API at its full gateway context with an extra CUSTOM request header (name + value)
     * alongside the bearer token, retrying until the expected status. Needed by the gateway schema-validation
     * test, where a resource declares a REQUIRED request header (X-Request-ID): omitting it is rejected by the
     * gateway's request schema validation (400), while sending it lets the call through (200). Distinct from the
     * {@code in header} variant, which places the TOKEN in a custom header rather than adding an arbitrary one.
     */
    @When("I invoke the API at gateway context {string} with method {string} using access token {string} and payload {string} with request header {string} set to {string} until response status code becomes {int} within {int} seconds")
    public void invokeApiByContextWithHeaderUntilStatus(String context, String httpMethod, String accessToken,
                                                        String payload, String headerName, String headerValue,
                                                        int expectedStatus, int timeoutSeconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                String actualAccessToken = TestContext.resolve(accessToken).toString();
                String actualPayload = (payload == null || payload.isEmpty())
                        ? null : TestContext.resolve(payload).toString();
                String endpointUrl = getBaseGatewayUrl()
                        + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + actualAccessToken);
                headers.put(headerName, headerValue);
                last = execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers,
                        actualPayload);
                if (last.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Retry transient connectivity only (see invokeApiByContextUntilStatus).
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        assertReachedExpectedStatus(last, expectedStatus);
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
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                String actualAccessToken = TestContext.resolve(accessToken).toString();
                String actualPayload = (payload == null || payload.isEmpty())
                        ? "" : TestContext.resolve(payload).toString();
                String endpointUrl = getBaseGatewayUrl()
                        + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + actualAccessToken);
                last = execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers,
                        actualPayload, contentType);
                if (last.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Retry transient connectivity only (see invokeApiByContextUntilStatus).
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        assertReachedExpectedStatus(last, expectedStatus);
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

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                String actualKey = TestContext.resolve(apiKey).toString();
                String endpointUrl = getBaseGatewayUrl()
                        + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
                Map<String, String> headers = new HashMap<>();
                headers.put("accept", "application/json");
                headers.put(headerName, actualKey);
                last = execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers, "");
                if (last.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Retry transient connectivity only.
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        assertReachedExpectedStatus(last, expectedStatus);
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
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                last = invokeApiByContext(resolvedContext, httpMethod, accessToken, payload);
                if (last.getResponseCode() == 200 && last.getData() != null && last.getData().contains(marker)) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Retry transient connectivity only (see invokeApiByContextUntilStatus).
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
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
        String actualPayload = (payload == null || payload.isEmpty()) ? "" : TestContext.resolve(payload).toString();
        // The context already carries any /t/<tenant> prefix, so append it directly to the gateway base URL.
        String endpointUrl = getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;

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
        String actualPayload = (payload == null || payload.isEmpty()) ? "" : TestContext.resolve(payload).toString();
        // Resolve {{contextKey}} placeholders in the path so the invocation can target a uniquely-generated
        // API context (names/contexts are randomized by ${UNIQUE:...}), e.g. "{{apiContext}}/1.0.0/...".
        String resolvedPath = Utils.resolveContextPlaceholders(path);
        String endpointUrl = Utils.getAPIInvocationURL(getBaseGatewayUrl(), resolvedPath, actingTenantDomain());

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

        String endpointUrl = Utils.getAPIInvocationURL(getBaseGatewayUrl(),
                Utils.resolveContextPlaceholders(path), actingTenantDomain());
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
        String endpointUrl = getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                last = invokeWithApiKey(endpointUrl, httpMethod, apikey);
                if (last.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Retry transient connectivity only (see invokeApiByContextUntilStatus).
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        assertReachedExpectedStatus(last, expectedStatus);
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
        String endpointUrl = getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                String actualKey = TestContext.resolve(apikey).toString();
                String actualPayload = (payload == null || payload.isEmpty())
                        ? "" : TestContext.resolve(payload).toString();
                Map<String, String> headers = new HashMap<>();
                headers.put("accept", "application/json");
                headers.put("ApiKey", actualKey);
                last = execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers,
                        actualPayload);
                if (last.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Retry transient connectivity only.
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        assertReachedExpectedStatus(last, expectedStatus);
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
        String endpointUrl = getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                Map<String, String> headers = new HashMap<>();
                headers.put("accept", "application/json");
                headers.put("ApiKey", TestContext.resolve(apikey).toString());
                headers.put("X-Forwarded-For", Utils.resolveContextPlaceholders(forwardedFor));
                last = execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers, "");
                if (last.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Retry transient connectivity only.
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        assertReachedExpectedStatus(last, expectedStatus);
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
        String endpointUrl = getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                Map<String, String> headers = new HashMap<>();
                headers.put("accept", "application/json");
                headers.put("ApiKey", TestContext.resolve(apikey).toString());
                headers.put("Referer", Utils.resolveContextPlaceholders(referer));
                last = execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers, "");
                if (last.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Retry transient connectivity only.
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        assertReachedExpectedStatus(last, expectedStatus);
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
        String endpointUrl = getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                Map<String, String> headers = new HashMap<>();
                headers.put("accept", "application/json");
                headers.put("Authorization", authHeader);
                last = execute(CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase()), endpointUrl, headers, "");
                if (last.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Retry transient connectivity only.
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        assertReachedExpectedStatus(last, expectedStatus);
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
        String endpointUrl = getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                last = invokeWithInternalKey(endpointUrl, httpMethod, internalKey);
                if (last.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Retry transient connectivity only (see invokeApiByContextUntilStatus).
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        assertReachedExpectedStatus(last, expectedStatus);
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

        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                last = invokeApiUsingKey(path, httpMethod, apikey);
                if (last.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Retry transient connectivity only (see invokeApiByContextUntilStatus).
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        assertReachedExpectedStatus(last, expectedStatus);
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

        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        HttpResponse last = null;
        do {
            try {
                last = invokeApiUsingAccessToken(path, httpMethod, accessToken, payload);
                if (last.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // Retry transient connectivity only (see invokeApiByContextUntilStatus).
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        assertReachedExpectedStatus(last, expectedStatus);
    }

    /**
     * Invokes the OpenID Connect userinfo endpoint with the given access token and stores the response.
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

        execute(CurlOption.HttpMethod.GET, Utils.getUserInfoEndpointURL(baseUrl), headers, null);
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
        String endpointUrl = Utils.getAPIInvocationURL(getBaseGatewayUrl(),
                Utils.resolveContextPlaceholders(path), actingTenantDomain());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualAccessToken);
        headers.put("Content-Type", Constants.CONTENT_TYPES.TEXT_XML);
        if (soapAction != null && !soapAction.isEmpty()) {
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
        String endpointUrl = getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualAccessToken);
        headers.put("Content-Type", Constants.CONTENT_TYPES.TEXT_XML);
        if (soapAction != null && !soapAction.isEmpty()) {
            headers.put("SOAPAction", soapAction);
        }

        execute(CurlOption.HttpMethod.POST, endpointUrl, headers, actualPayload, Constants.CONTENT_TYPES.TEXT_XML);
    }
}
