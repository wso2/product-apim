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
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway runtime-invocation steps. Invokes deployed APIs through the block's gateway (REST/SOAP, by access
 * token or API key, with optional retry-until-status for eventual consistency), plus the OpenID userinfo
 * endpoint. The tenant context segment of the invocation URL is derived from the scenario's acting actor
 * ({@link Identity#defaultActor()}) rather than the retired {@code currentTenant} pointer.
 */
public class APIInvocationSteps {

    private String getBaseGatewayUrl() {

        Object baseGatewayUrl = TestContext.get("baseGatewayUrl");
        if (baseGatewayUrl == null) {
            throw new IllegalStateException("baseGatewayUrl is not available in the test context yet");
        }
        return baseGatewayUrl.toString();
    }

    /** Tenant domain for the invocation URL — taken from the scenario's acting actor. */
    private String actingTenantDomain() {
        return Identity.defaultActor().getUserDomain();
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
        do {
            try {
                invokeApiByContext(resolvedContext, httpMethod, accessToken, payload);
                HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
                if (response != null && response.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (Exception transientDuringWarmup) {
                // A transient error (e.g. an SSL handshake against a gateway listener still coming up after a
                // server restart or a fresh deploy) must be retried, not fatal — keep polling until the deadline.
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
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
        do {
            try {
                invokeApiByContext(resolvedContext, httpMethod, accessToken, payload, headerName);
                HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
                if (response != null && response.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (Exception transientDuringWarmup) {
                // A transient error (e.g. an SSL handshake against a gateway listener still coming up after a
                // server restart or a fresh deploy) must be retried, not fatal — keep polling until the deadline.
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
    }

    /** Single invocation addressing the API by its full gateway context path (no tenant prefixing). */
    private void invokeApiByContext(String resolvedContext, String httpMethod, String accessToken, String payload)
            throws Exception {
        invokeApiByContext(resolvedContext, httpMethod, accessToken, payload, "Authorization");
    }

    /** As above, but places the bearer token in the given authorization header name. */
    private void invokeApiByContext(String resolvedContext, String httpMethod, String accessToken, String payload,
                                    String authHeaderName)
            throws Exception {

        String actualAccessToken = Utils.resolveFromContext(accessToken).toString();
        String actualPayload = (payload == null || payload.isEmpty()) ? "" : Utils.resolveFromContext(payload).toString();
        // The context already carries any /t/<tenant> prefix, so append it directly to the gateway base URL.
        String endpointUrl = getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;

        Map<String, String> headers = new HashMap<>();
        headers.put(authHeaderName, "Bearer " + actualAccessToken);

        CurlOption.HttpMethod method = CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase());
        switch (method) {
            case GET:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doGet(endpointUrl, headers));
                break;
            case DELETE:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doDelete(endpointUrl, headers));
                break;
            case POST:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doPost(endpointUrl, headers, actualPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON));
                break;
            case PUT:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doPut(endpointUrl, headers, actualPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON));
                break;
        }
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
    public void invokeApiUsingAccessToken(String path, String httpMethod, String accessToken, String payload) throws Exception {

        String actualAccessToken = Utils.resolveFromContext(accessToken).toString();
        String actualPayload = (payload == null || payload.isEmpty()) ? "" : Utils.resolveFromContext(payload).toString();
        // Resolve {{contextKey}} placeholders in the path so the invocation can target a uniquely-generated
        // API context (names/contexts are randomized by ${UNIQUE:...}), e.g. "{{apiContext}}/1.0.0/...".
        String resolvedPath = Utils.resolveContextPlaceholders(path);
        String endpointUrl = Utils.getAPIInvocationURL(getBaseGatewayUrl(), resolvedPath, actingTenantDomain());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualAccessToken);

        CurlOption.HttpMethod method = CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase());
        switch (method) {
            case GET:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doGet(endpointUrl, headers));
                break;
            case DELETE:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doDelete(endpointUrl, headers));
                break;
            case POST:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doPost(endpointUrl, headers, actualPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON));
                break;
            case PUT:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doPut(endpointUrl, headers, actualPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON));
                break;
        }
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
    public void invokeApiUsingKey(String path, String httpMethod, String apikey) throws Exception {

        String endpointUrl = Utils.getAPIInvocationURL(getBaseGatewayUrl(),
                Utils.resolveContextPlaceholders(path), actingTenantDomain());
        invokeWithApiKey(endpointUrl, httpMethod, apikey);
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
        // Never wait less than the global deployment window: a freshly published API's gateway route can take
        // longer than a short per-step value to become routable, especially under load. The feature's value
        // can still request MORE than the floor.
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        do {
            try {
                invokeWithApiKey(endpointUrl, httpMethod, apikey);
                HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
                if (response != null && response.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (Exception transientDuringWarmup) {
                // A transient error (e.g. an SSL handshake against a gateway listener still coming up after a
                // server restart or a fresh deploy) must be retried, not fatal — keep polling until the deadline.
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
    }

    /** Single API-key invocation against a fully-built gateway URL. */
    private void invokeWithApiKey(String endpointUrl, String httpMethod, String apikey) throws Exception {

        String actualKey = Utils.resolveFromContext(apikey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("ApiKey", actualKey);

        CurlOption.HttpMethod method = CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase());
        switch (method) {
            case GET:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doGet(endpointUrl, headers));
                break;
            case DELETE:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doDelete(endpointUrl, headers));
                break;
            case POST:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doPost(endpointUrl, headers, "", Constants.CONTENT_TYPES.APPLICATION_JSON));
                break;
            case PUT:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doPut(endpointUrl, headers, "", Constants.CONTENT_TYPES.APPLICATION_JSON));
                break;
        }
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

        // Never wait less than the global deployment window: a freshly published API's gateway route can take
        // longer than a short per-step value to become routable, especially under load. The feature's value
        // can still request MORE than the floor.
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        do {
            try {
                invokeApiUsingKey(path, httpMethod, apikey);
                HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
                if (response != null && response.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (Exception transientDuringWarmup) {
                // A transient error (e.g. an SSL handshake against a gateway listener still coming up after a
                // server restart or a fresh deploy) must be retried, not fatal — keep polling until the deadline.
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
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

        // Never wait less than the global deployment window: a freshly published API's gateway route can take
        // longer than a short per-step value to become routable, especially under load. The feature's value
        // can still request MORE than the floor.
        long deadlineMillis = Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        long endTime = System.currentTimeMillis() + deadlineMillis;
        do {
            try {
                invokeApiUsingAccessToken(path, httpMethod, accessToken, payload);
                HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
                if (response != null && response.getResponseCode() == expectedStatus) {
                    return;
                }
            } catch (Exception transientDuringWarmup) {
                // A transient error (e.g. an SSL handshake against a gateway listener still coming up after a
                // server restart or a fresh deploy) must be retried, not fatal — keep polling until the deadline.
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
    }

    /**
     * Invokes the OpenID Connect userinfo endpoint with the given access token and stores the response.
     *
     * @param accessToken Context key containing the access token
     */
    @When("I invoke the OpenID userinfo endpoint using access token {string}")
    public void invokeUserInfoEndpoint(String accessToken) throws Exception {

        String actualAccessToken = Utils.resolveFromContext(accessToken).toString();
        String baseUrl = TestContext.get("baseUrl").toString();

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualAccessToken);
        headers.put("accept", "application/json");

        TestContext.set("httpResponse",
                SimpleHTTPClient.getInstance().doGet(Utils.getUserInfoEndpointURL(baseUrl), headers));
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

        String actualAccessToken = Utils.resolveFromContext(accessToken).toString();
        String actualPayload = Utils.resolveFromContext(payload).toString();
        String endpointUrl = Utils.getAPIInvocationURL(getBaseGatewayUrl(),
                Utils.resolveContextPlaceholders(path), actingTenantDomain());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualAccessToken);
        headers.put("Content-Type", Constants.CONTENT_TYPES.TEXT_XML);
        if (soapAction != null && !soapAction.isEmpty()) {
            headers.put("SOAPAction", soapAction);
        }

        TestContext.set("httpResponse", SimpleHTTPClient.getInstance()
                .doPost(endpointUrl, headers, actualPayload, Constants.CONTENT_TYPES.TEXT_XML));
    }

    /**
     * Invokes a SOAP API addressing it by its full gateway context path (the {@code context} field returned by
     * the Publisher API, which already carries the {@code /t/<tenant>} prefix for tenant APIs) — so no tenant
     * prefix is added here. Use this when the path was captured from an API's {@code context} (needed for the
     * tenant rows of a Scenario Outline); the {@code at path} variant double-prefixes a tenant context.
     */
    @When("I invoke the SOAP API at gateway context {string} using access token {string} and payload {string} and soap action {string}")
    public void invokeSoapByGatewayContext(String context, String accessToken, String payload, String soapAction) throws IOException {

        String actualAccessToken = Utils.resolveFromContext(accessToken).toString();
        String actualPayload = Utils.resolveFromContext(payload).toString();
        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String endpointUrl = getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualAccessToken);
        headers.put("Content-Type", Constants.CONTENT_TYPES.TEXT_XML);
        if (soapAction != null && !soapAction.isEmpty()) {
            headers.put("SOAPAction", soapAction);
        }

        TestContext.set("httpResponse", SimpleHTTPClient.getInstance()
                .doPost(endpointUrl, headers, actualPayload, Constants.CONTENT_TYPES.TEXT_XML));
    }
}
