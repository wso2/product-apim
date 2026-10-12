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
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.JwtTestUtils;
import org.wso2.am.integration.cucumbertests.utils.Names;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * API-security glue ported from APISecurityTestCase: JWT-format (locally signed) API keys with a key type and
 * restriction claims, revocation of such a key by value, api-key listing by name, role-less principals and the
 * empty Authorization header.
 */
public class ApiSecurityParitySteps {

    private static final Log log = LogFactory.getLog(ApiSecurityParitySteps.class);

    /**
     * Locally signs a JWT-format API key for an application with an explicit key type and optional
     * {@code permittedIP} / {@code permittedReferer} claims. An empty restriction string omits that claim.
     * The claims mirror legacy {@code JWTGenerator}: the application's name, tier, owner, UUID and the numeric
     * application row id read from APIM's internal data API (tenant-scoped through {@code xWSO2Tenant}).
     */
    @When("I generate locally signed JWT API key of type {string} for application id {string} with permitted IP {string} and permitted referer {string} as {string}")
    public void iGenerateTypedLocallySignedJwtApiKey(String keyType, String appIdKey, String permittedIp,
                                                     String permittedReferer, String keyContext) throws IOException {

        String appUuid = TestContext.resolve(appIdKey).toString();
        HttpResponse appResponse = SimpleHTTPClient.getInstance().doGet(
                Utils.getApplicationEndpointURL(Utils.getBaseUrl(), appUuid), Identity.devportalHeaders());
        Assert.assertNotNull(appResponse, "DevPortal returned no response while reading application " + appUuid);
        Assert.assertEquals(appResponse.getResponseCode(), 200,
                "Failed to read application metadata needed for the JWT API key: " + appResponse.getData());
        JSONObject app = new JSONObject(appResponse.getData());
        String applicationName = app.optString("name");
        String applicationTier = app.optString("throttlingPolicy");
        String applicationOwner = app.optString("owner");
        Assert.assertFalse(applicationName.isBlank() || applicationTier.isBlank() || applicationOwner.isBlank(),
                "DevPortal application response omitted claims required by JWT API-key validation: " + app);

        User systemAdmin = Identity.gatewayManagementAdmin();
        Map<String, String> internalHeaders = Identity.basicAuthHeaders(systemAdmin.getUserName(),
                systemAdmin.getPassword());
        internalHeaders.put("xWSO2Tenant", Identity.actingTenantDomain());
        HttpResponse internalResponse = SimpleHTTPClient.getInstance().doGet(
                Utils.getBaseUrl() + "internal/data/v1/applications", internalHeaders);
        Assert.assertNotNull(internalResponse, "APIM internal application lookup returned no response");
        Assert.assertEquals(internalResponse.getResponseCode(), 200,
                "APIM internal application lookup failed: " + internalResponse.getData());
        JSONArray internalApplications = new JSONObject(internalResponse.getData()).getJSONArray("list");
        int matchedId = -1;
        int matches = 0;
        for (int i = 0; i < internalApplications.length(); i++) {
            JSONObject internalApp = internalApplications.getJSONObject(i);
            if (appUuid.equals(internalApp.optString("uuid"))) {
                matchedId = internalApp.optInt("id", -1);
                matches++;
            }
        }
        Assert.assertEquals(matches, 1,
                "Expected exactly one internal application row for UUID " + appUuid + ", found " + matches);
        Assert.assertTrue(matchedId > 0, "Internal application row has invalid numeric id " + matchedId);

        String signedKey = JwtTestUtils.buildLegacyJwtApiKey(Identity.actingActor().getUserName(),
                Utils.getAPIMTokenEndpointURL(Utils.getBaseUrl()), applicationName, applicationTier, matchedId,
                appUuid, applicationOwner, keyType, permittedIp, permittedReferer);
        TestContext.set(Utils.normalizeContextKey(keyContext), signedKey);
        log.info("Generated " + keyType + " JWT-format API key for application " + appUuid + " (tenant="
                + Identity.actingTenantDomain() + ", numericApplicationId=" + matchedId + ")");
    }

    /**
     * Revokes an API key BY VALUE through the DevPortal revoke endpoint
     * ({@code applications/{id}/api-keys/{keyType}/revoke}, body {@code {"apikey":"<key>"}}). This is the
     * revocation path for a JWT-format key: the product verifies the key's signature, matches its application
     * claim against the path's application and revokes its {@code jti}. The response is published for assertion.
     */
    @When("I revoke the api key value {string} of type {string} for application id {string}")
    public void iRevokeApiKeyByValue(String keyRef, String keyType, String appId) throws IOException {

        String actualAppId = TestContext.resolve(appId).toString();
        String apiKey = TestContext.resolve(keyRef).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        String url = Utils.getBaseUrl() + Constants.DEFAULT_DEVPORTAL + "applications/" + actualAppId
                + "/api-keys/" + keyType + "/revoke";
        Requests.post(url, headers, new JSONObject().put("apikey", apiKey).toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Asserts the api-key listing published by the preceding step carries an entry whose {@code keyName} is the
     * given name, with a non-blank {@code keyUUID}. Handles both listing shapes (bare array or count/list wrapper).
     */
    @Then("The api key list should contain a key named {string}")
    public void theApiKeyListShouldContainKeyNamed(String keyName) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() == 200 && response.getData() != null
                        && !response.getData().isBlank(),
                "No api-key list response to search: got " + (response == null ? "no response"
                        : response.getResponseCode() + " / body=" + response.getData()));
        String data = response.getData().trim();
        JSONArray list = data.startsWith("[") ? new JSONArray(data) : new JSONObject(data).getJSONArray("list");
        for (int i = 0; i < list.length(); i++) {
            JSONObject entry = list.getJSONObject(i);
            if (keyName.equals(entry.optString("keyName"))) {
                Assert.assertFalse(entry.optString("keyUUID").isBlank(),
                        "Api key '" + keyName + "' is listed without a keyUUID: " + entry);
                return;
            }
        }
        Assert.fail("Api key named '" + keyName + "' is not in the listing: " + response.getData());
    }

    /**
     * Provisions a scenario-owned user with NO role at all (the addUser call carries no roleList element) in the
     * acting tenant, registering it for the teardown sweep. Publishes the store name under {@code usernameKey}
     * and the tenant-qualified credential name under {@code <usernameKey>LoginName}.
     */
    @When("I provision a user with no roles with name prefix {string} password {string} storing the username as {string}")
    public void iProvisionRolelessUser(String namePrefix, String password, String usernameKey) throws IOException {

        provisionRolelessUser(Names.unique(namePrefix), password, usernameKey);
    }

    /**
     * As the step above, for a store name that is itself an email address ({@code <unique>@<emailDomain>}).
     */
    @When("I provision a user with no roles with name prefix {string} and email domain {string} password {string} storing the username as {string}")
    public void iProvisionRolelessEmailUser(String namePrefix, String emailDomain, String password,
                                            String usernameKey) throws IOException {

        provisionRolelessUser(Names.unique(namePrefix) + Constants.CHAR_AT + emailDomain, password, usernameKey);
    }

    private void provisionRolelessUser(String username, String password, String usernameKey) throws IOException {

        String tenantDomain = Identity.actingTenantDomain();
        User tenantAdmin = Utils.getTenantFromContext(tenantDomain).getTenantAdmin();
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"http://service.ws.um.carbon.wso2.org\"><soapenv:Header/><soapenv:Body>"
                + "<ser:addUser><ser:userName>" + Utils.escapeXml(username) + "</ser:userName>"
                + "<ser:credential>" + Utils.escapeXml(password) + "</ser:credential>"
                + "<ser:profileName>default</ser:profileName>"
                + "<ser:requirePasswordChange>false</ser:requirePasswordChange>"
                + "</ser:addUser></soapenv:Body></soapenv:Envelope>";
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(
                Utils.getRemoteUserStoreManagerServiceURL(Utils.getBaseUrl()), payload, "urn:addUser",
                tenantAdmin.getUserName(), tenantAdmin.getPassword());
        Assert.assertTrue(response.getResponseCode() >= 200 && response.getResponseCode() < 300,
                "Adding role-less user '" + username + "' in tenant '" + tenantDomain + "' failed with "
                        + response.getResponseCode() + ": " + response.getData());
        ResourceCleanup.register(ResourceCleanup.CREATED_USER_NAMES, username);
        String key = Utils.normalizeContextKey(usernameKey);
        TestContext.set(key, username);
        TestContext.set(key + "LoginName", username + Constants.CHAR_AT + tenantDomain);
    }

    /**
     * Invokes a deployed API carrying an {@code Authorization} header whose value is EMPTY, retrying until the
     * expected status. Before the gateway call, the same client sends the same header map to a loopback socket
     * and the raw request bytes are checked for an {@code Authorization:} line with an empty value, so the step
     * fails rather than silently testing a request with the header dropped.
     */
    @When("I invoke the API at gateway context {string} with method GET and an empty Authorization header until response status code becomes {int} within {int} seconds")
    public void invokeWithEmptyAuthorizationHeader(String context, int expectedStatus, int timeoutSeconds)
            throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("Authorization", "");
        assertEmptyAuthorizationOnTheWire(headers);

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String endpointUrl = Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/")
                + resolvedContext;
        HttpResponse last = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> Requests.get(endpointUrl, headers),
                response -> response.getResponseCode() == expectedStatus);
        Requests.publishPollResult(last);
        Assert.assertNotNull(last, "No response captured while waiting for status " + expectedStatus);
        Assert.assertEquals(last.getResponseCode(), expectedStatus,
                "API did not return " + expectedStatus + " within the deadline; last response: " + last.getData());
    }

    private static void assertEmptyAuthorizationOnTheWire(Map<String, String> headers) throws Exception {

        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(15000);
            CompletableFuture<String> captured = CompletableFuture.supplyAsync(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(15000);
                    InputStream in = socket.getInputStream();
                    ByteArrayOutputStream raw = new ByteArrayOutputStream();
                    int b;
                    while ((b = in.read()) != -1) {
                        raw.write(b);
                        if (raw.toString(StandardCharsets.ISO_8859_1).endsWith("\r\n\r\n")) {
                            break;
                        }
                    }
                    OutputStream out = socket.getOutputStream();
                    out.write("HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                            .getBytes(StandardCharsets.ISO_8859_1));
                    out.flush();
                    return raw.toString(StandardCharsets.ISO_8859_1);
                } catch (IOException e) {
                    throw new IllegalStateException("Loopback capture of the probe request failed", e);
                }
            });
            SimpleHTTPClient.getInstance().doGet("http://127.0.0.1:" + server.getLocalPort() + "/probe", headers);
            String request = captured.get(20, TimeUnit.SECONDS);
            boolean emptyHeaderSent = false;
            for (String line : request.split("\r\n")) {
                int colon = line.indexOf(':');
                if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase("Authorization")) {
                    Assert.assertTrue(line.substring(colon + 1).trim().isEmpty(),
                            "The probe request carried a non-empty Authorization value: " + line);
                    emptyHeaderSent = true;
                }
            }
            Assert.assertTrue(emptyHeaderSent,
                    "The HTTP client did not put an empty Authorization header on the wire. Request:\n" + request);
        }
    }
}
