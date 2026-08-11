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
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.testcontainers.DynamicPlatformGatewayContainer;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Steps for the API Platform Gateway journey (@cap:gateway @feat:platform-gateway). Registration and connection
 * are ADMIN PRODUCT BEHAVIOUR performed here as steps (§14) — the block only boots the gateway infrastructure
 * (see {@code BlockLifecycleListener.bootPlatformGateway}); these steps register it via the admin REST API,
 * connect the running gateway with the minted registration token, and assert it reaches the connected state.
 *
 * <p>Shared-scope keys the block publishes: {@code blockPlatformGateway} (the container),
 * {@code platformGatewayControlPlaneHost} (the {@code host.docker.internal:<port>} the gateway dials).
 */
public class PlatformGatewaySteps {

    private static final String GATEWAY_ID_KEY = "platformGatewayId";
    private static final String GATEWAY_NAME_KEY = "platformGatewayName";
    private static final String GATEWAY_TOKEN_KEY = "platformGatewayRegistrationToken";
    private static final String CONTAINER_KEY = "blockPlatformGateway";
    private static final String CONTROLPLANE_HOST_KEY = "platformGatewayControlPlaneHost";
    private static final String DATA_PLANE_URL_KEY = "platformGatewayDataPlaneUrl";
    private static final String API_CONTEXT_KEY = "pgApiContext";

    @When("I register a platform gateway {string}")
    public void registerPlatformGateway(String name) throws IOException {
        String payload = new JSONObject()
                .put("name", name)
                .put("displayName", name)
                .put("vhost", "https://localhost:8443")
                .put("description", "integration-v2 platform gateway")
                .toString();
        HttpResponse resp = Requests.post(Utils.getPlatformGatewaysURL(Utils.getBaseUrl()),
                Identity.adminHeaders(), payload, "application/json");
        // On a successful create, capture the id (for failure-safe cleanup) + the one-time registrationToken and
        // the assigned name (for the connect step). The response is published as httpResponse for the status check.
        if (resp != null && resp.getResponseCode() == 201 && resp.getData() != null) {
            JSONObject body = new JSONObject(resp.getData());
            String id = body.getString("id");
            ResourceCleanup.register(ResourceCleanup.CREATED_PLATFORM_GATEWAY_IDS, id);
            TestContext.set(GATEWAY_ID_KEY, id);
            TestContext.set(GATEWAY_NAME_KEY, body.getString("name"));
            TestContext.set(GATEWAY_TOKEN_KEY, body.getString("registrationToken"));
        }
    }

    @When("I connect the platform gateway with the issued registration token")
    public void connectPlatformGateway() {
        DynamicPlatformGatewayContainer gateway = platformGateway();
        String controlPlaneHost = (String) TestContext.get(CONTROLPLANE_HOST_KEY);
        String token = (String) TestContext.resolve(GATEWAY_TOKEN_KEY);
        String name = (String) TestContext.resolve(GATEWAY_NAME_KEY);
        gateway.connect(controlPlaneHost, token, name);
        // connect() restarts the gateway compose, so the data-plane host port changes — re-publish the current
        // URL (the listener's pre-connect value is now stale) for the invocation step.
        TestContext.setShared(DATA_PLANE_URL_KEY, gateway.getDataPlaneHttpsUrl());
    }

    @Then("the platform gateway {string} becomes active within {int} seconds")
    public void gatewayBecomesActive(String name, int timeoutSeconds) throws InterruptedException {
        String url = Utils.getPlatformGatewaysURL(Utils.getBaseUrl());
        HttpResponse resp = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> Requests.get(url, Identity.adminHeaders()),
                r -> isGatewayActive(r, name));
        Assert.assertTrue(isGatewayActive(resp, name),
                "Platform gateway '" + name + "' did not report isActive=true within " + timeoutSeconds
                        + "s; last response=" + (resp == null ? "null" : resp.getData()));
    }

    @Then("the platform gateway {string} is inactive")
    public void gatewayIsInactive(String name) throws IOException {
        HttpResponse resp = Requests.get(Utils.getPlatformGatewaysURL(Utils.getBaseUrl()), Identity.adminHeaders());
        Assert.assertFalse(isGatewayActive(resp, name),
                "Platform gateway '" + name + "' should be inactive (registered but never connected) but reports "
                        + "isActive=true; " + (resp == null ? "null" : resp.getData()));
    }

    @When("I create and deploy a REST API from {string} to the platform gateway as {string}")
    public void createAndDeployToPlatformGateway(String payloadPath, String apiIdKey)
            throws IOException, InterruptedException {
        BaseSteps baseSteps = new BaseSteps();
        PublisherBaseSteps publisher = new PublisherBaseSteps();
        // Reuse the publisher glue: create the API (asserts 201, registers it for cleanup), create a revision,
        // then deploy that revision to THIS platform gateway's environment — registration auto-creates a
        // deployable environment named after the gateway. The deploy `vhost` is the vhost HOST (parsed from the
        // registration vhost URL "https://localhost:8443" -> "localhost"), not the vhost name.
        baseSteps.putJsonPayloadFromFile(payloadPath, "<createApiPayload>");
        publisher.iCreateAnAPIWithPayloadAs("apis", "<createApiPayload>", apiIdKey);
        // Capture the API context (to build the gateway invocation URL); the create response is the httpResponse.
        Object createResp = TestContext.get("httpResponse");
        if (createResp instanceof HttpResponse hr) {
            TestContext.set(API_CONTEXT_KEY, Utils.extractValueFromPayload(hr.getData(), "context"));
        }
        baseSteps.putJsonPayloadInContext("<createRevisionPayload>", "{\"description\":\"Initial Revision\"}");
        publisher.iCreateResourceRevision("apis", apiIdKey, "<createRevisionPayload>");
        String gatewayName = (String) TestContext.resolve(GATEWAY_NAME_KEY);
        baseSteps.putJsonPayloadInContext("<deployRevisionPayload>",
                "[{\"name\":\"" + gatewayName + "\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        publisher.iDeployApiRevisionGivenPayload("<revisionId>", "apis", apiIdKey, "<deployRevisionPayload>");
    }

    @When("I invoke the deployed API on the platform gateway with access token {string} "
            + "until response status code becomes {int} within {int} seconds")
    public void invokeThroughPlatformGateway(String tokenKey, int expectedStatus, int timeoutSeconds)
            throws InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + (String) TestContext.resolve(tokenKey));
        invokeAndAssert(headers, expectedStatus, timeoutSeconds);
    }

    @When("I invoke the deployed API on the platform gateway without authentication "
            + "until response status code becomes {int} within {int} seconds")
    public void invokeThroughPlatformGatewayNoAuth(int expectedStatus, int timeoutSeconds)
            throws InterruptedException {
        invokeAndAssert(new HashMap<>(), expectedStatus, timeoutSeconds);
    }

    @When("I invoke the deployed API on the platform gateway with an invalid access token "
            + "until response status code becomes {int} within {int} seconds")
    public void invokeThroughPlatformGatewayInvalidToken(int expectedStatus, int timeoutSeconds)
            throws InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer invalid-token-deadbeef00000000");
        invokeAndAssert(headers, expectedStatus, timeoutSeconds);
    }

    @When("I invoke the deployed API on the platform gateway with header {string} set to {string} "
            + "until response status code becomes {int} within {int} seconds")
    public void invokeThroughPlatformGatewayWithHeader(String header, String value, int expectedStatus,
            int timeoutSeconds) throws InterruptedException {
        // If value is a context key (e.g. a generated api-key), resolve it; otherwise use the literal (a wrong key).
        Map<String, String> headers = new HashMap<>();
        headers.put(header, TestContext.contains(value) ? String.valueOf(TestContext.get(value)) : value);
        invokeAndAssert(headers, expectedStatus, timeoutSeconds);
    }

    private void invokeAndAssert(Map<String, String> headers, int expectedStatus, int timeoutSeconds)
            throws InterruptedException {
        String dataPlaneUrl = (String) TestContext.get(DATA_PLANE_URL_KEY);
        String context = ((String) TestContext.resolve(API_CONTEXT_KEY)).trim();
        String base = dataPlaneUrl.endsWith("/") ? dataPlaneUrl.substring(0, dataPlaneUrl.length() - 1) : dataPlaneUrl;
        String path = context.startsWith("/") ? context : "/" + context;
        String url = base + path + "/1.0.0/customers/123";
        // Data plane is HTTPS with a self-signed listener cert (CN=localhost); SimpleHTTPClient trusts all in the
        // test lane. Invocation is async (the deploy propagates to the gateway over the WS), so retry until ready.
        HttpResponse resp = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> SimpleHTTPClient.getInstance().doGet(url, headers),
                r -> r != null && r.getResponseCode() == expectedStatus);
        if (resp != null) {
            TestContext.set("httpResponse", resp);
        }
        Assert.assertEquals(resp == null ? -1 : resp.getResponseCode(), expectedStatus,
                "Invoke via platform gateway " + url + " -> "
                        + (resp == null ? "null" : resp.getResponseCode() + " " + resp.getData()));
    }

    private static boolean isGatewayActive(HttpResponse resp, String name) {
        if (resp == null || resp.getResponseCode() != 200 || resp.getData() == null) {
            return false;
        }
        JSONArray list = new JSONObject(resp.getData()).optJSONArray("list");
        if (list == null) {
            return false;
        }
        for (int i = 0; i < list.length(); i++) {
            JSONObject gateway = list.getJSONObject(i);
            if (name.equals(gateway.optString("name")) && gateway.optBoolean("isActive")) {
                return true;
            }
        }
        return false;
    }

    private static DynamicPlatformGatewayContainer platformGateway() {
        Object gateway = TestContext.get(CONTAINER_KEY);
        if (!(gateway instanceof DynamicPlatformGatewayContainer container)) {
            throw new IllegalStateException("No platform gateway booted for this block — set "
                    + "bootPlatformGateway=\"true\" on the <test> block");
        }
        return container;
    }
}
