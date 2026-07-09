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

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Names;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class ApplicationBaseSteps {

    BaseSteps baseSteps = new BaseSteps();

    private String getBaseUrl() {

        return baseSteps.getBaseUrl();
    }

    /**
     * Creates a new application in the Developer Portal using a JSON payload.
     * The created application ID is stored as "createdAppId" in the test context for use in subsequent steps.
     * 
     * @param payload Context key containing the application creation JSON payload
     */
    @When("I create an application with payload {string}")
    public void iCreateAnApplicationWithJsonPayload(String payload) throws IOException {

        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse applicationCreateResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getApplicationCreateURL(getBaseUrl()), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", applicationCreateResponse);
        Assert.assertEquals(applicationCreateResponse.getResponseCode(), 201, applicationCreateResponse.getData());
        Object createdAppId = Utils.extractValueFromPayload(applicationCreateResponse.getData(), "applicationId");
        TestContext.set("createdAppId", createdAppId);
        // Register for scenario teardown so a shared-server suite does not accumulate applications across scenarios.
        ResourceCleanup.register(Constants.CREATED_APPLICATION_IDS, createdAppId);
    }

    /**
     * Creates a custom application-level throttling policy (admin API) with a low request-count limit, so a
     * subsequent invocation can be driven past it to prove throttling enforcement (429). Built-in tiers are
     * far too high (thousands/min) to trip in a test, so a bespoke low policy is required. The resolved
     * (uniquified) policy name is published as {@code appThrottlePolicyName} for the application payload to
     * reference, and the created policy is registered for admin-token teardown.
     *
     * @param policyBaseName     policy name, may contain a {@code ${UNIQUE:...}} token for parallel safety
     * @param requestsPerMinute  the request-count limit per minute
     */
    @When("I create an application throttling policy {string} allowing {int} requests per minute")
    public void iCreateApplicationThrottlingPolicy(String policyBaseName, int requestsPerMinute) throws IOException {

        String policyName = Utils.resolveContextPlaceholders(Utils.resolvePayloadPlaceholders(policyBaseName));
        String payload = String.format(
                "{\"policyName\":\"%s\",\"displayName\":\"%s\",\"description\":\"Throttle-enforcement test: %d req/min\","
                        + "\"type\":\"ApplicationThrottlePolicy\",\"defaultLimit\":{\"type\":\"REQUESTCOUNTLIMIT\","
                        + "\"requestCount\":{\"timeUnit\":\"min\",\"unitTime\":1,\"requestCount\":%d}}}",
                policyName, policyName, requestsPerMinute, requestsPerMinute);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getApplicationThrottlingPoliciesURL(getBaseUrl()), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
        TestContext.set("appThrottlePolicyName", policyName);

        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object policyId = Utils.extractValueFromPayload(response.getData(), "policyId");
            if (policyId != null) {
                TestContext.set("appThrottlePolicyId", policyId);
                ResourceCleanup.register(Constants.CREATED_APPLICATION_POLICY_IDS, policyId);
            }
        }
    }

    /**
     * Creates a subscription-tier throttling policy with a per-minute request-count limit (admin API), for
     * subscription-level throttling enforcement. Stores {@code subThrottlePolicyName}/{@code subThrottlePolicyId}
     * and registers the id for owner-aware teardown. {@code rateLimitCount == 0} disables burst control.
     */
    @When("I create a subscription throttling policy {string} allowing {int} requests per minute")
    public void iCreateSubscriptionThrottlingPolicy(String policyBaseName, int requestsPerMinute) throws IOException {
        createSubscriptionThrottlingPolicy(policyBaseName, requestsPerMinute, 0);
    }

    /**
     * As above, but with **burst control**: a low {@code rateLimitCount} per minute on top of a (typically high)
     * request-count quota, so a 429 within the first few calls is unambiguously the burst limit, not the quota.
     * Burst is set at MINUTE granularity so it trips deterministically via the cumulative until-429 retry (a
     * sub-second burst window would reset between the retry's spaced attempts).
     */
    @When("I create a subscription throttling policy {string} allowing {int} requests per minute with burst limit {int} per minute")
    public void iCreateSubscriptionThrottlingPolicyWithBurst(String policyBaseName, int requestsPerMinute,
                                                             int burstPerMinute) throws IOException {
        createSubscriptionThrottlingPolicy(policyBaseName, requestsPerMinute, burstPerMinute, "Internal/everyone");
    }

    /**
     * As above but ALLOW-restricted to a single role, so the tier is usable only by users in that role (a user
     * outside it is refused at subscribe time with 403 "Tier … is not allowed"). Ports the role-restricted-tier
     * check of SubscriptionThrottlingPolicyTestCase.
     */
    @When("I create a subscription throttling policy {string} allowing {int} requests per minute restricted to role {string}")
    public void iCreateRestrictedSubscriptionThrottlingPolicy(String policyBaseName, int requestsPerMinute,
                                                              String role) throws IOException {
        createSubscriptionThrottlingPolicy(policyBaseName, requestsPerMinute, 0, role);
    }

    private void createSubscriptionThrottlingPolicy(String policyBaseName, int requestsPerMinute, int burstPerMinute)
            throws IOException {
        createSubscriptionThrottlingPolicy(policyBaseName, requestsPerMinute, burstPerMinute, "Internal/everyone");
    }

    private void createSubscriptionThrottlingPolicy(String policyBaseName, int requestsPerMinute, int burstPerMinute,
                                                    String allowRole) throws IOException {

        String policyName = Utils.resolveContextPlaceholders(Utils.resolvePayloadPlaceholders(policyBaseName));
        String payload = String.format(
                "{\"policyName\":\"%s\",\"displayName\":\"%s\",\"description\":\"Throttle-enforcement test: %d req/min,"
                        + " burst %d/min\",\"type\":\"SubscriptionThrottlePolicy\",\"defaultLimit\":{\"type\":"
                        + "\"REQUESTCOUNTLIMIT\",\"requestCount\":{\"timeUnit\":\"min\",\"unitTime\":1,"
                        + "\"requestCount\":%d}},\"rateLimitCount\":%d,\"rateLimitTimeUnit\":\"min\","
                        + "\"stopOnQuotaReach\":true,\"billingPlan\":\"FREE\",\"customAttributes\":[],"
                        // Subscription tiers are role-gated: a user lacking an ALLOW-listed role is refused the tier
                        // at subscribe time (403 "not allowed"). Default Internal/everyone → usable by any actor.
                        + "\"permissions\":{\"permissionType\":\"ALLOW\",\"roles\":[\"%s\"]}}",
                policyName, policyName, requestsPerMinute, burstPerMinute, requestsPerMinute, burstPerMinute, allowRole);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getSubscriptionThrottlingPoliciesURL(getBaseUrl()), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
        TestContext.set("subThrottlePolicyName", policyName);

        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object policyId = Utils.extractValueFromPayload(response.getData(), "policyId");
            if (policyId != null) {
                TestContext.set("subThrottlePolicyId", policyId);
                ResourceCleanup.register(Constants.CREATED_SUBSCRIPTION_POLICY_IDS, policyId);
            }
        }
    }

    /**
     * Creates an advanced (API-level) throttling policy with a per-minute request-count limit (admin API). An
     * advanced policy is set as an API's {@code apiThrottlingPolicy} and enforced across ALL subscriptions to
     * that API. Stores {@code advThrottlePolicyName}/{@code advThrottlePolicyId} and registers the id for
     * owner-aware teardown.
     */
    @When("I create an advanced throttling policy {string} allowing {int} requests per minute")
    public void iCreateAdvancedThrottlingPolicy(String policyBaseName, int requestsPerMinute) throws IOException {

        String policyName = Utils.resolveContextPlaceholders(Utils.resolvePayloadPlaceholders(policyBaseName));
        String payload = String.format(
                "{\"policyName\":\"%s\",\"displayName\":\"%s\",\"description\":\"Throttle-enforcement test: %d req/min\","
                        + "\"type\":\"AdvancedThrottlePolicy\",\"defaultLimit\":{\"type\":\"REQUESTCOUNTLIMIT\","
                        + "\"requestCount\":{\"timeUnit\":\"min\",\"unitTime\":1,\"requestCount\":%d}},"
                        + "\"conditionalGroups\":[]}",
                policyName, policyName, requestsPerMinute, requestsPerMinute);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAdvancedThrottlingPoliciesURL(getBaseUrl()), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
        TestContext.set("advThrottlePolicyName", policyName);

        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object policyId = Utils.extractValueFromPayload(response.getData(), "policyId");
            if (policyId != null) {
                TestContext.set("advThrottlePolicyId", policyId);
                ResourceCleanup.register(Constants.CREATED_ADVANCED_POLICY_IDS, policyId);
            }
        }
    }

    // ---- Admin throttling-policy CRUD breadth (application/subscription/advanced/custom) ----

    private void postAdminPolicy(String listUrl, String payload, String nameKey, String policyName, String idKey,
                                 String cleanupListKey) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(listUrl, headers, payload, Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
        TestContext.set(nameKey, policyName);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object policyId = Utils.extractValueFromPayload(response.getData(), "policyId");
            if (policyId != null) {
                TestContext.set(idKey, policyId);
                ResourceCleanup.register(cleanupListKey, policyId);
            }
        }
    }

    /** Subscription throttling policy with a BANDWIDTH limit (KB/min). */
    @When("I create a subscription throttling policy {string} allowing {int} KB per minute")
    public void iCreateSubscriptionBandwidthPolicy(String policyBaseName, int kbPerMinute) throws IOException {
        String policyName = Utils.resolveContextPlaceholders(Utils.resolvePayloadPlaceholders(policyBaseName));
        String payload = String.format(
                "{\"policyName\":\"%s\",\"displayName\":\"%s\",\"description\":\"Subscription bandwidth %d KB/min\","
                        + "\"type\":\"SubscriptionThrottlePolicy\",\"defaultLimit\":{\"type\":\"BANDWIDTHLIMIT\","
                        + "\"bandwidth\":{\"timeUnit\":\"min\",\"unitTime\":1,\"dataAmount\":%d,\"dataUnit\":\"KB\"}},"
                        + "\"rateLimitCount\":0,\"rateLimitTimeUnit\":\"min\",\"stopOnQuotaReach\":true,"
                        + "\"billingPlan\":\"FREE\",\"customAttributes\":[],"
                        + "\"permissions\":{\"permissionType\":\"ALLOW\",\"roles\":[\"Internal/everyone\"]}}",
                policyName, policyName, kbPerMinute, kbPerMinute);
        postAdminPolicy(Utils.getSubscriptionThrottlingPoliciesURL(getBaseUrl()), payload, "subThrottlePolicyName",
                policyName, "subThrottlePolicyId", Constants.CREATED_SUBSCRIPTION_POLICY_IDS);
    }

    /**
     * Subscription throttling policy with an AI TOKEN QUOTA limit (total tokens/min) — the quota enforced on
     * AIAPI-subtype APIs. The gateway accumulates the token counts each backend LLM response reports (usage.*)
     * against this quota and returns 429 once it is exceeded. requestCount and the prompt/completion sub-limits
     * are set high so the TOTAL-token quota is unambiguously what trips. Ports the AI token-throttling aspect of
     * the AI-API suite (the Gemini restart variant is parked). Stores {@code subThrottlePolicyName}/
     * {@code subThrottlePolicyId} and registers the id for owner-aware teardown.
     */
    @When("I create a subscription throttling policy {string} allowing {int} total tokens per minute")
    public void iCreateSubscriptionAiTokenQuotaPolicy(String policyBaseName, int totalTokensPerMinute)
            throws IOException {
        String policyName = Utils.resolveContextPlaceholders(Utils.resolvePayloadPlaceholders(policyBaseName));
        String payload = String.format(
                "{\"policyName\":\"%s\",\"displayName\":\"%s\",\"description\":\"AI token quota %d tokens/min\","
                        + "\"type\":\"SubscriptionThrottlePolicy\",\"defaultLimit\":{\"type\":\"AIAPIQUOTALIMIT\","
                        + "\"aiApiQuota\":{\"timeUnit\":\"min\",\"unitTime\":1,\"requestCount\":1000000,"
                        + "\"totalTokenCount\":%d,\"promptTokenCount\":1000000,\"completionTokenCount\":1000000}},"
                        + "\"rateLimitCount\":0,\"rateLimitTimeUnit\":\"min\",\"stopOnQuotaReach\":true,"
                        + "\"billingPlan\":\"FREE\",\"customAttributes\":[],"
                        + "\"permissions\":{\"permissionType\":\"ALLOW\",\"roles\":[\"Internal/everyone\"]}}",
                policyName, policyName, totalTokensPerMinute, totalTokensPerMinute);
        postAdminPolicy(Utils.getSubscriptionThrottlingPoliciesURL(getBaseUrl()), payload, "subThrottlePolicyName",
                policyName, "subThrottlePolicyId", Constants.CREATED_SUBSCRIPTION_POLICY_IDS);
    }

    /**
     * Subscription throttling policy with an EVENT-COUNT limit (events/min) — the ASYNC (streaming) quota enforced
     * on WebSocket / WebSub / SSE APIs. Per the streaming rate-limiting docs the gateway counts each WS frame (both
     * directions) as an event and throttles the connection once the count is exceeded. An EVENTCOUNTLIMIT
     * subscription policy is an async plan, so a streaming API can offer it. Stores {@code subThrottlePolicyName}/
     * {@code subThrottlePolicyId} and registers the id for owner-aware teardown.
     */
    @When("I create a subscription throttling policy {string} allowing {int} events per minute")
    public void iCreateSubscriptionEventCountPolicy(String policyBaseName, int eventsPerMinute) throws IOException {
        String policyName = Utils.resolveContextPlaceholders(Utils.resolvePayloadPlaceholders(policyBaseName));
        String payload = String.format(
                "{\"policyName\":\"%s\",\"displayName\":\"%s\",\"description\":\"Streaming event quota %d events/min\","
                        + "\"type\":\"SubscriptionThrottlePolicy\",\"defaultLimit\":{\"type\":\"EVENTCOUNTLIMIT\","
                        + "\"eventCount\":{\"timeUnit\":\"min\",\"unitTime\":1,\"eventCount\":%d}},"
                        + "\"rateLimitCount\":0,\"rateLimitTimeUnit\":\"min\",\"stopOnQuotaReach\":true,"
                        + "\"billingPlan\":\"FREE\",\"customAttributes\":[],"
                        + "\"permissions\":{\"permissionType\":\"ALLOW\",\"roles\":[\"Internal/everyone\"]}}",
                policyName, policyName, eventsPerMinute, eventsPerMinute);
        postAdminPolicy(Utils.getSubscriptionThrottlingPoliciesURL(getBaseUrl()), payload, "subThrottlePolicyName",
                policyName, "subThrottlePolicyId", Constants.CREATED_SUBSCRIPTION_POLICY_IDS);
    }

    /**
     * Creates a SubscriptionThrottlePolicy carrying GraphQL query-analysis limits ({@code graphQLMaxComplexity} /
     * {@code graphQLMaxDepth}) with a high request-count default (so only complexity/depth trips). Ports the
     * QueryComplexPolicy / QueryDepthPolicy of GraphqlSubscriptionTestCase.
     */
    @When("I create a subscription throttling policy {string} with max complexity {int} and max depth {int}")
    public void iCreateSubscriptionGraphQLPolicy(String policyBaseName, int maxComplexity, int maxDepth)
            throws IOException {
        String policyName = Utils.resolveContextPlaceholders(Utils.resolvePayloadPlaceholders(policyBaseName));
        String payload = String.format(
                "{\"policyName\":\"%s\",\"displayName\":\"%s\",\"description\":\"GraphQL query limits: complexity %d,"
                        + " depth %d\",\"type\":\"SubscriptionThrottlePolicy\",\"defaultLimit\":{"
                        + "\"type\":\"REQUESTCOUNTLIMIT\",\"requestCount\":{\"timeUnit\":\"min\",\"unitTime\":1,"
                        + "\"requestCount\":1000}},\"graphQLMaxComplexity\":%d,\"graphQLMaxDepth\":%d,"
                        + "\"rateLimitCount\":0,\"rateLimitTimeUnit\":\"min\",\"stopOnQuotaReach\":true,"
                        + "\"billingPlan\":\"FREE\",\"customAttributes\":[],"
                        + "\"permissions\":{\"permissionType\":\"ALLOW\",\"roles\":[\"Internal/everyone\"]}}",
                policyName, policyName, maxComplexity, maxDepth, maxComplexity, maxDepth);
        postAdminPolicy(Utils.getSubscriptionThrottlingPoliciesURL(getBaseUrl()), payload, "subThrottlePolicyName",
                policyName, "subThrottlePolicyId", Constants.CREATED_SUBSCRIPTION_POLICY_IDS);
    }

    /**
     * Sets an API's per-field GraphQL complexity values (PUT /apis/{id}/graphql-policies/complexity). Ports
     * addGraphQLComplexityDetails — the per-field weights the gateway sums to compute a query's complexity.
     */
    @When("I set the GraphQL complexity for API {string} from payload {string}")
    public void iSetGraphqlComplexity(String apiId, String payloadKey) throws IOException {
        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String payload = Utils.resolveFromContext(payloadKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPut(Utils.getGraphQLComplexityURL(getBaseUrl(), actualApiId), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /** Advanced (API-level) throttling policy with a BANDWIDTH limit (KB/min). */
    @When("I create an advanced throttling policy {string} allowing {int} KB per minute")
    public void iCreateAdvancedBandwidthPolicy(String policyBaseName, int kbPerMinute) throws IOException {
        String policyName = Utils.resolveContextPlaceholders(Utils.resolvePayloadPlaceholders(policyBaseName));
        String payload = String.format(
                "{\"policyName\":\"%s\",\"displayName\":\"%s\",\"description\":\"Advanced bandwidth %d KB/min\","
                        + "\"type\":\"AdvancedThrottlePolicy\",\"defaultLimit\":{\"type\":\"BANDWIDTHLIMIT\","
                        + "\"bandwidth\":{\"timeUnit\":\"min\",\"unitTime\":1,\"dataAmount\":%d,\"dataUnit\":\"KB\"}},"
                        + "\"conditionalGroups\":[]}",
                policyName, policyName, kbPerMinute, kbPerMinute);
        postAdminPolicy(Utils.getAdvancedThrottlingPoliciesURL(getBaseUrl()), payload, "advThrottlePolicyName",
                policyName, "advThrottlePolicyId", Constants.CREATED_ADVANCED_POLICY_IDS);
    }

    /** Advanced throttling policy with a conditional group (a header-condition-gated sub-limit). */
    @When("I create an advanced throttling policy {string} allowing {int} requests per minute with a header conditional group")
    public void iCreateAdvancedConditionalPolicy(String policyBaseName, int requestsPerMinute) throws IOException {
        String policyName = Utils.resolveContextPlaceholders(Utils.resolvePayloadPlaceholders(policyBaseName));
        String payload = String.format(
                "{\"policyName\":\"%s\",\"displayName\":\"%s\",\"description\":\"Advanced with conditional group\","
                        + "\"type\":\"AdvancedThrottlePolicy\",\"defaultLimit\":{\"type\":\"REQUESTCOUNTLIMIT\","
                        + "\"requestCount\":{\"timeUnit\":\"min\",\"unitTime\":1,\"requestCount\":%d}},"
                        + "\"conditionalGroups\":[{\"description\":\"gold-header group\",\"conditions\":[{\"type\":"
                        + "\"HEADERCONDITION\",\"invertCondition\":false,\"headerCondition\":{\"headerName\":"
                        + "\"X-Tier\",\"headerValue\":\"gold\"}}],\"limit\":{\"type\":\"REQUESTCOUNTLIMIT\","
                        + "\"requestCount\":{\"timeUnit\":\"min\",\"unitTime\":1,\"requestCount\":%d}}}]}",
                policyName, policyName, requestsPerMinute, requestsPerMinute);
        postAdminPolicy(Utils.getAdvancedThrottlingPoliciesURL(getBaseUrl()), payload, "advThrottlePolicyName",
                policyName, "advThrottlePolicyId", Constants.CREATED_ADVANCED_POLICY_IDS);
    }

    /** Generic retrieve of a throttling policy by type + id (admin API). Non-asserting. */
    @When("I retrieve the {string} throttling policy with id {string}")
    public void iRetrieveThrottlingPolicyByType(String policyType, String idKey) throws IOException {
        String policyId = Utils.resolveFromContext(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getThrottlingPolicyByTypeURL(getBaseUrl(), policyType, policyId), headers);
        TestContext.set("httpResponse", response);
    }

    /** Generic delete of a throttling policy by type + id (admin API). Non-asserting (also used for 404 checks). */
    @When("I delete the {string} throttling policy with id {string}")
    public void iDeleteThrottlingPolicyByType(String policyType, String idKey) throws IOException {
        String policyId = Utils.resolveFromContext(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getThrottlingPolicyByTypeURL(getBaseUrl(), policyType, policyId), headers);
        TestContext.set("httpResponse", response);
    }

    /** Generic update: retrieve the policy, set a new description, and PUT it back. */
    @When("I update the {string} throttling policy {string} setting its description to {string}")
    public void iUpdateThrottlingPolicyDescription(String policyType, String idKey, String description)
            throws IOException {
        String policyId = Utils.resolveFromContext(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        String url = Utils.getThrottlingPolicyByTypeURL(getBaseUrl(), policyType, policyId);
        HttpResponse getResp = SimpleHTTPClient.getInstance().doGet(url, headers);
        JSONObject policy = new JSONObject(getResp.getData());
        policy.put("description", description);
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPut(url, headers, policy.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /** Retrieve all throttling policies of a type (admin API). Non-asserting. */
    @When("I retrieve all {string} throttling policies")
    public void iRetrieveAllThrottlingPolicies(String policyType) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getThrottlingPoliciesByTypeURL(getBaseUrl(), policyType), headers);
        TestContext.set("httpResponse", response);
    }

    // ---- Admin gateway environment CRUD ----

    private void createGatewayEnvironment(String name, String displayName, String host, String gatewayType)
            throws IOException {
        createGatewayEnvironment(name, displayName, host, gatewayType, null);
    }

    private void createGatewayEnvironment(String name, String displayName, String host, String gatewayType,
                                          String allowRole) throws IOException {
        JSONObject env = new JSONObject()
                .put("name", name)
                .put("displayName", displayName)
                .put("description", "Gateway environment CRUD test")
                .put("provider", "wso2")
                .put("isReadOnly", false);
        if (allowRole != null && !allowRole.isBlank()) {
            env.put("permissions", new JSONObject()
                    .put("permissionType", "ALLOW")
                    .put("roles", new JSONArray().put(allowRole)));
        }
        JSONArray vhosts = new JSONArray();
        boolean typedGateway = gatewayType != null && !gatewayType.isBlank();
        if (host != null && !host.isBlank()) {
            JSONObject vhost = new JSONObject().put("host", host).put("httpPort", 8280).put("httpsPort", 8243);
            if (typedGateway) {
                // Non-Regular gateways (e.g. APK) reject ws/wss ports and expect an httpContext.
                vhost.put("httpContext", "gwctx");
            } else {
                vhost.put("httpContext", "").put("wsPort", 9099).put("wssPort", 8099);
            }
            vhosts.put(vhost);
        }
        env.put("vhosts", vhosts);
        if (typedGateway) {
            env.put("gatewayType", gatewayType);
        }
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getEnvironmentsURL(getBaseUrl()), headers, env.toString(),
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object id = Utils.extractValueFromPayload(response.getData(), "id");
            if (id != null) {
                TestContext.set("environmentId", id);
                ResourceCleanup.register(Constants.CREATED_ENVIRONMENT_IDS, id);
            }
        }
    }

    /** Create a gateway environment (single vhost). Non-asserting — feature confirms 201 or the negative code. */
    @When("I create a gateway environment with name {string} display name {string} and vhost host {string}")
    public void iCreateGatewayEnvironment(String nameBase, String displayName, String host) throws IOException {
        createGatewayEnvironment(Utils.resolvePayloadPlaceholders(nameBase), displayName, host, null);
    }

    /** Create a gateway environment with a specific gateway type (e.g. APK). */
    @When("I create a gateway environment {string} with vhost host {string} and gateway type {string}")
    public void iCreateGatewayEnvironmentWithType(String nameBase, String host, String gatewayType) throws IOException {
        String name = Utils.resolvePayloadPlaceholders(nameBase);
        createGatewayEnvironment(name, name, host, gatewayType);
    }

    /**
     * Create a gateway environment with an ALLOW role permission (only users in that role may use it). Salvages
     * the (legacy-commented) env-permissions test as an admin-plane CRUD assertion: the permission persists.
     */
    @When("I create a gateway environment {string} with vhost host {string} allowing role {string}")
    public void iCreateGatewayEnvironmentAllowingRole(String nameBase, String host, String role) throws IOException {
        String name = Utils.resolvePayloadPlaceholders(nameBase);
        createGatewayEnvironment(name, name, host, null, role);
    }

    /** Retrieve all gateway environments (admin API). */
    @When("I retrieve all gateway environments")
    public void iRetrieveAllGatewayEnvironments() throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getEnvironmentsURL(getBaseUrl()), headers);
        TestContext.set("httpResponse", response);
    }

    /** Retrieve a gateway environment by id (admin API). */
    @When("I retrieve the gateway environment with id {string}")
    public void iRetrieveGatewayEnvironment(String idKey) throws IOException {
        String id = Utils.resolveFromContext(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getEnvironmentByIdURL(getBaseUrl(), id), headers);
        TestContext.set("httpResponse", response);
    }

    /** Update a gateway environment's description (GET → set → PUT). */
    @When("I update the gateway environment {string} setting its description to {string}")
    public void iUpdateGatewayEnvironment(String idKey, String description) throws IOException {
        String id = Utils.resolveFromContext(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        String url = Utils.getEnvironmentByIdURL(getBaseUrl(), id);
        HttpResponse getResp = SimpleHTTPClient.getInstance().doGet(url, headers);
        JSONObject env = new JSONObject(getResp.getData());
        env.put("description", description);
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPut(url, headers, env.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /** Delete a gateway environment by id (admin API). Non-asserting (also used for 404 / delete-Default 400).
     *  The id may be a context key (e.g. {@code environmentId}) or a literal id such as {@code Default}. */
    @When("I delete the gateway environment with id {string}")
    public void iDeleteGatewayEnvironment(String idKey) throws IOException {
        String id = TestContext.contains(idKey) ? TestContext.get(idKey).toString() : idKey;
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getEnvironmentByIdURL(getBaseUrl(), id), headers);
        TestContext.set("httpResponse", response);
    }

    /** Retrieve the gateway instances of an environment (admin API). The id may be a context key
     *  (e.g. {@code environmentId}) or a literal environment id such as {@code Default}. */
    @When("I retrieve the gateway instances of environment {string}")
    public void iRetrieveGatewayInstances(String idKey) throws IOException {
        String id = TestContext.contains(idKey) ? TestContext.get(idKey).toString() : idKey;
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getEnvironmentGatewaysURL(getBaseUrl(), id), headers);
        TestContext.set("httpResponse", response);
    }

    /** A vhost JSON object for a Regular (Synapse) gateway — http/https/ws/wss ports. */
    private static JSONObject regularVhost(String host) {
        return new JSONObject().put("host", host).put("httpContext", "")
                .put("httpPort", 8280).put("httpsPort", 8243).put("wsPort", 9099).put("wssPort", 8099);
    }

    /** POST an environment payload, storing/registering the id on success. Non-asserting. */
    private void postEnvironment(JSONObject env) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getEnvironmentsURL(getBaseUrl()), headers, env.toString(),
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object id = Utils.extractValueFromPayload(response.getData(), "id");
            if (id != null) {
                TestContext.set("environmentId", id);
                ResourceCleanup.register(Constants.CREATED_ENVIRONMENT_IDS, id);
            }
        }
    }

    /**
     * Create a gateway environment with MULTIPLE vhosts (comma-separated hosts). Non-asserting — the feature
     * confirms 201 (distinct hosts) or 400 (e.g. duplicate hostnames). Ports the multi-vhost half of
     * EnvironmentTestCase (legacy-disabled; carried as new verified coverage).
     */
    @When("I create a gateway environment {string} with vhost hosts {string}")
    public void iCreateGatewayEnvironmentMultiVhost(String nameBase, String hostsCsv) throws IOException {
        String name = Utils.resolvePayloadPlaceholders(nameBase);
        JSONArray vhosts = new JSONArray();
        for (String h : hostsCsv.split(",")) {
            vhosts.put(regularVhost(h.trim()));
        }
        JSONObject env = new JSONObject()
                .put("name", name).put("displayName", name)
                .put("description", "Multi-vhost gateway environment")
                .put("provider", "wso2").put("isReadOnly", false)
                .put("vhosts", vhosts);
        postEnvironment(env);
    }

    /**
     * Searches applications via the ADMIN API filtered by owner (the {@code user} query param). Ports the
     * owner-search of ApplicationsSearchByNameOrOwnerTestCase. (The v4 admin /applications endpoint supports
     * only owner search — there is no name-search query param — so only the by-owner path is portable.)
     * Non-asserting.
     *
     * @param actorRef the owning actor (resolved to its username)
     */
    @When("I search admin applications owned by actor {string}")
    public void iSearchAdminApplicationsByOwner(String actorRef) throws IOException {
        String owner = Identity.resolveActor(actorRef).getUserName();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAdminApplicationsByOwnerURL(getBaseUrl(), owner), headers);
        TestContext.set("httpResponse", response);
    }

    /**
     * Searches applications via the ADMIN API filtered by name (the {@code name} query param). Ports the
     * by-name search of ApplicationsSearchByNameOrOwnerTestCase. Non-asserting.
     *
     * @param name application name (or prefix); {@code {{contextKey}}} placeholders are resolved
     */
    @When("I search admin applications by name {string}")
    public void iSearchAdminApplicationsByName(String name) throws IOException {
        String actualName = Utils.resolveContextPlaceholders(name);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAdminApplicationsByNameURL(getBaseUrl(), actualName), headers);
        TestContext.set("httpResponse", response);
    }

    /** Update a gateway environment to a single vhost host (removing any others). Non-asserting. */
    @When("I update the gateway environment {string} to only vhost host {string}")
    public void iUpdateGatewayEnvironmentToSingleVhost(String idKey, String host) throws IOException {
        String id = Utils.resolveFromContext(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        String url = Utils.getEnvironmentByIdURL(getBaseUrl(), id);
        HttpResponse getResp = SimpleHTTPClient.getInstance().doGet(url, headers);
        JSONObject env = new JSONObject(getResp.getData());
        env.put("vhosts", new JSONArray().put(regularVhost(host)));
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPut(url, headers, env.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /**
     * Creates an application throttling policy with a BANDWIDTH limit (KB/min) rather than a request count
     * (admin API). Stores {@code appThrottlePolicyName}/{@code appThrottlePolicyId} (so the existing
     * "create an application with throttling policy from …" step can bind to it) and registers for teardown.
     * Used for bandwidth-throttling coverage/investigation.
     */
    @When("I create an application throttling policy {string} allowing {int} KB per minute")
    public void iCreateApplicationBandwidthPolicy(String policyBaseName, int kbPerMinute) throws IOException {

        String policyName = Utils.resolveContextPlaceholders(Utils.resolvePayloadPlaceholders(policyBaseName));
        String payload = String.format(
                "{\"policyName\":\"%s\",\"displayName\":\"%s\",\"description\":\"Bandwidth-enforcement test: %d KB/min\","
                        + "\"type\":\"ApplicationThrottlePolicy\",\"defaultLimit\":{\"type\":\"BANDWIDTHLIMIT\","
                        + "\"bandwidth\":{\"timeUnit\":\"min\",\"unitTime\":1,\"dataAmount\":%d,\"dataUnit\":\"KB\"}}}",
                policyName, policyName, kbPerMinute, kbPerMinute);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getApplicationThrottlingPoliciesURL(getBaseUrl()), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
        TestContext.set("appThrottlePolicyName", policyName);

        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object policyId = Utils.extractValueFromPayload(response.getData(), "policyId");
            if (policyId != null) {
                TestContext.set("appThrottlePolicyId", policyId);
                ResourceCleanup.register(Constants.CREATED_APPLICATION_POLICY_IDS, policyId);
            }
        }
    }

    /**
     * Creates a custom (Siddhi) throttling rule that throttles requests to ONE specific API context after N
     * requests/min (admin API). Custom rules are GLOBAL, so keying the Siddhi eligibility on the test's unique
     * apiContext keeps it isolation-safe (only the test's own API is affected). Stores
     * {@code customThrottlePolicyName}/{@code customThrottlePolicyId} and registers for teardown.
     */
    @When("I create a custom throttling policy {string} throttling API context {string} after {int} requests per minute")
    public void iCreateCustomThrottlingPolicy(String policyBaseName, String apiContext, int requestsPerMinute)
            throws IOException {

        String policyName = Utils.resolveContextPlaceholders(Utils.resolvePayloadPlaceholders(policyBaseName));
        String context = Utils.resolveContextPlaceholders(apiContext);
        // Isolation-safe: throttle ONLY this test's unique apiContext. Emit the request's apiContext as the
        // throttleKey and set keyTemplate=$apiContext so template and emitted key always align at runtime; match
        // both the bare context and the /version form (the runtime apiContext value differs across paths).
        String siddhiQuery = "FROM RequestStream\n"
                + "SELECT userId, apiContext, ( apiContext == '" + context + "' or apiContext == '" + context
                + "/1.0.0' ) AS isEligible, apiContext as throttleKey\n"
                + "INSERT INTO EligibilityStream;\n\n"
                + "FROM EligibilityStream[isEligible==true]#throttler:timeBatch(1 min)\n"
                + "SELECT throttleKey, (count(userId) >= " + requestsPerMinute + ") as isThrottled, expiryTimeStamp "
                + "group by throttleKey\n"
                + "INSERT ALL EVENTS into ResultStream;";

        // Build via JSONObject so the Siddhi query's newlines/quotes are correctly escaped.
        String payload = new JSONObject()
                .put("policyName", policyName)
                .put("description", "Custom throttle-enforcement test: context " + context + " @ " + requestsPerMinute + "/min")
                .put("siddhiQuery", siddhiQuery)
                .put("keyTemplate", "$apiContext")
                .toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getCustomThrottlingPoliciesURL(getBaseUrl()), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
        TestContext.set("customThrottlePolicyName", policyName);

        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object policyId = Utils.extractValueFromPayload(response.getData(), "policyId");
            if (policyId != null) {
                TestContext.set("customThrottlePolicyId", policyId);
                ResourceCleanup.register(Constants.CREATED_CUSTOM_POLICY_IDS, policyId);
            }
        }
    }

    /** Retrieves an application throttling policy by id (admin API), storing the raw response for assertions. */
    @When("I retrieve the application throttling policy {string}")
    public void iRetrieveApplicationThrottlingPolicy(String policyIdKey) throws IOException {

        String policyId = Utils.resolveFromContext(policyIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApplicationThrottlingPolicyByIdURL(getBaseUrl(), policyId), headers);
        TestContext.set("httpResponse", response);
    }

    /** Deletes an application throttling policy by id (admin API), storing the raw response for assertions. */
    @When("I delete the application throttling policy {string}")
    public void iDeleteApplicationThrottlingPolicy(String policyIdKey) throws IOException {

        String policyId = Utils.resolveFromContext(policyIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getApplicationThrottlingPolicyByIdURL(getBaseUrl(), policyId), headers);
        TestContext.set("httpResponse", response);
    }

    /** Retrieves a custom (Siddhi) throttling rule by id (admin API), storing the raw response for assertions. */
    @When("I retrieve the custom throttling policy {string}")
    public void iRetrieveCustomThrottlingPolicy(String policyIdKey) throws IOException {

        String policyId = Utils.resolveFromContext(policyIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getCustomThrottlingPolicyByIdURL(getBaseUrl(), policyId), headers);
        TestContext.set("httpResponse", response);
    }

    /** Deletes a custom (Siddhi) throttling rule by id (admin API), storing the raw response for assertions. */
    @When("I delete the custom throttling policy {string}")
    public void iDeleteCustomThrottlingPolicy(String policyIdKey) throws IOException {

        String policyId = Utils.resolveFromContext(policyIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getCustomThrottlingPolicyByIdURL(getBaseUrl(), policyId), headers);
        TestContext.set("httpResponse", response);
    }

    /**
     * Creates an application bound to a given application-throttling-policy name (read from context). Used by
     * the throttling-enforcement flow: the application tier is the low custom policy created above, so its
     * invocations are what get throttled. The created id is stored as {@code createdAppId} and registered for
     * teardown, mirroring {@link #iCreateAnApplicationWithJsonPayload}.
     *
     * @param appName        application name (may contain a {@code ${UNIQUE:...}} token)
     * @param policyNameKey  context key holding the application throttling-policy name to bind
     */
    @When("I create an application {string} with throttling policy from {string}")
    public void iCreateApplicationWithThrottlingPolicy(String appName, String policyNameKey) throws IOException {

        String resolvedName = Utils.resolvePayloadPlaceholders(appName);
        String policyName = Utils.resolveFromContext(policyNameKey).toString();
        String jsonPayload = String.format(
                "{\"name\":\"%s\",\"throttlingPolicy\":\"%s\",\"description\":\"Throttle-enforcement test application\"}",
                resolvedName, policyName);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getApplicationCreateURL(getBaseUrl()), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        Object createdAppId = Utils.extractValueFromPayload(response.getData(), "applicationId");
        TestContext.set("createdAppId", createdAppId);
        ResourceCleanup.register(Constants.CREATED_APPLICATION_IDS, createdAppId);
    }

    /**
     * Attempts to create an application without asserting success, storing the raw response as
     * {@code httpResponse} for the feature to assert. For negative / access-control scenarios where the
     * create is expected to be rejected (e.g. a non-consumer actor lacking the app-management scope): unlike
     * the positive step it neither extracts an id nor registers anything for cleanup.
     */
    @When("I attempt to create an application with payload {string}")
    public void iAttemptToCreateAnApplicationWithPayload(String payload) throws IOException {

        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getApplicationCreateURL(getBaseUrl()), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /**
     * Deletes an application by its ID.
     *
     * @param appId Context key containing the application ID to delete
     */
    @When("I delete the application with id {string}")
    public void iDeleteApplication(String appId) throws IOException{

        String actualAppId = Utils.resolveFromContext(appId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse applicationDeleteResponse = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getApplicationEndpointURL(getBaseUrl(), actualAppId), headers);

        TestContext.set("httpResponse", applicationDeleteResponse);
    }

    /**
     * Retrieves the details of a specific application by its ID.
     *
     * @param appId Context key containing the application ID to retrieve
     */
    @When("I retrieve the application with id {string}")
    public void iShouldBeAbleToRetrieveApplication(String appId) throws Exception {

        String actualAppId = Utils.resolveFromContext(appId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse applicationRetrieveResponse = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApplicationEndpointURL(getBaseUrl(), actualAppId), headers);

        TestContext.set("httpResponse", applicationRetrieveResponse);
    }

    /**
     * Searches for an application by name and stores its ID in the test context.
     *
     * @param applicationName The name of the application to search for
     * @param appId Context key where the found application ID will be stored
     */
    @When("I fetch the application with {string} as {string}")
    public void iFetchTheApplicationWithAs(String applicationName, String appId) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApplicationSearchURL(getBaseUrl(), applicationName), headers);

        TestContext.set("httpResponse", response);

        JSONObject responseJson = new JSONObject(response.getData());
        if (responseJson.has("list") && !responseJson.getJSONArray("list").isEmpty()) {
            String applicationId = responseJson
                    .getJSONArray("list")
                    .getJSONObject(0)
                    .getString("applicationId");
            TestContext.set(appId, applicationId);
        } else {
            throw new IOException("No applications found with name: " + applicationName);
        }
    }

    /**
     * Updates an application with a new payload.
     *
     * @param appId Context key containing the application ID to update
     * @param updatePayload Context key containing the application update JSON payload
     */
    @When("I update the application {string} with payload {string}")
    public void iUpdateTheApplicationWithPayload(String appId, String updatePayload) throws IOException {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        // Resolve any {{contextKey}} placeholders (e.g. a captured application name so the PUT keeps its
        // required name). No-op when the payload has none.
        String jsonPayload = Utils.resolveContextPlaceholders(Utils.resolveFromContext(updatePayload).toString());

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance().doPut(
                Utils.getApplicationEndpointURL(getBaseUrl(), actualAppId), headers, jsonPayload,
                Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
    }

    /**
     * Creates a subscription between an application and an API.
     * The payload is updated with the actual application ID and API ID before sending the request.
     * The created subscription ID is stored in the test context.
     * 
     * @param apiId Context key containing the API ID to subscribe to
     * @param appId Context key containing the application ID to use for subscription
     * @param payload Context key containing the subscription creation JSON payload
     * @param subscriptionID Context key where the created subscription ID will be stored
     */
    @When("I subscribe to API {string} using application {string} with payload {string} as {string}")
    public void iSubscribeToApi(String apiId, String appId, String payload, String subscriptionID) throws Exception {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String actualAppId = Utils.resolveFromContext(appId).toString();

        // Add application id and API id to the payload
        String jsonPayload = Utils.resolveFromContext(payload).toString();
        jsonPayload = jsonPayload.replace("{{applicationId}}", actualAppId);
        jsonPayload = jsonPayload.replace("{{apiId}}", actualApiId);
        // Resolve any remaining {{contextKey}} placeholders (e.g. a custom subscription throttling policy name
        // captured into context). The applicationId/apiId markers are already substituted above, so only genuine
        // context keys remain — resolveContextPlaceholders is a no-op when the payload has none.
        jsonPayload = Utils.resolveContextPlaceholders(jsonPayload);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getCreateSubscriptionURL(getBaseUrl()),
                headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        TestContext.set(subscriptionID,Utils.extractValueFromPayload(response.getData(), "subscriptionId"));
    }

    /**
     * Attempts to subscribe an application to an API without asserting success, storing the raw response as
     * {@code httpResponse} for the feature to assert. For negative / access-control scenarios where the
     * subscribe is expected to be rejected (e.g. an actor lacking the apim:subscribe scope): unlike the
     * positive step it neither asserts a status nor records a subscription id.
     */
    @When("I attempt to subscribe to API {string} using application {string} with payload {string}")
    public void iAttemptToSubscribeToApi(String apiId, String appId, String payload) throws Exception {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String actualAppId = Utils.resolveFromContext(appId).toString();

        String jsonPayload = Utils.resolveFromContext(payload).toString();
        jsonPayload = jsonPayload.replace("{{applicationId}}", actualAppId);
        jsonPayload = jsonPayload.replace("{{apiId}}", actualApiId);
        // Resolve any remaining {{contextKey}} placeholders, mirroring the positive iSubscribeToApi step. This
        // also makes a typo'd placeholder fail FAST (resolveContextPlaceholders throws on an unknown key) rather
        // than being sent to the server verbatim — an unresolved id yields a misleading 500 that masquerades as
        // a genuine rejection (it once produced a false "org-policy denial returns 500" finding).
        jsonPayload = Utils.resolveContextPlaceholders(jsonPayload);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getCreateSubscriptionURL(getBaseUrl()),
                headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /**
     * Retrieves a subscription between a specific API and application.
     *
     * @param apiId Context key containing the API ID
     * @param appId Context key containing the application ID
     */
    @Then("I retrieve the subscription for Api {string} by Application {string}")
    public void iShouldBeAbleToRetrieveSubscription(String apiId, String appId) throws Exception {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String actualAppId = Utils.resolveFromContext(appId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAllSubscriptionsURL(getBaseUrl(), actualApiId, actualAppId, null, null,
                        null), headers);

        TestContext.set("httpResponse", response);

        JSONObject responseJson = new JSONObject(response.getData());
        if (responseJson.has("list") && !responseJson.getJSONArray("list").isEmpty()) {
            String subscriptionId = responseJson
                    .getJSONArray("list")
                    .getJSONObject(0)
                    .getString("subscriptionId");
            TestContext.set("subscriptionId", subscriptionId);
        } else {
            throw new IOException("No subscription found");
        }
    }

    /**
     * Retrieves all existing keys (OAuth2 credentials) for an application.
     * The consumer secret and key mapping ID from the first key are extracted and stored in the test context.
     * 
     * @param appId Context key containing the application ID
     */
    @When("I retrieve existing application keys for {string}")
    public void iRetrieveExistingApplicationKeys(String appId) throws IOException {

        String actualAppId = Utils.resolveFromContext(appId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApplicationAllKeys(getBaseUrl(), actualAppId), headers);

        TestContext.set("httpResponse", response);

        JSONObject responseJson = new JSONObject(response.getData());
        if (responseJson.has("list") && !responseJson.getJSONArray("list").isEmpty()) {
            JSONObject firstKey = responseJson
                    .getJSONArray("list")
                    .getJSONObject(0);

            String consumerSecret = firstKey.optString("consumerSecret", null);
            String keyMappingId = firstKey.optString("keyMappingId", null);

            if (consumerSecret != null) {
                TestContext.set("consumerSecret", consumerSecret);
            }

            if (keyMappingId != null) {
                TestContext.set("keyMappingId", keyMappingId);
            }
        } else {
            throw new IOException("No application keys found in response");
        }

    }

    /**
     * Updates the keys (OAuth2 credentials) for an application.
     * The update payload should be stored in the test context under the key "updateKeysPayload".
     *
     * @param appId Context key containing the application ID
     */
    @And("I update the keys for application with {string}")
    public void iUpdateTheKeysForApplicationWith(String appId) throws IOException {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String keyMappingId = Utils.resolveFromContext("keyMappingId").toString();
        String jsonPayload =Utils.resolveFromContext("updateKeysPayload").toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPut(Utils.getUpdateKey(getBaseUrl(), actualAppId, keyMappingId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /**
     * Deletes the generated keys (OAuth2 credentials) for an application.
     *
     * @param appId Context key containing the application ID
     */
    @When("I delete the generated keys for {string}")
    public void iDeleteTheGeneratedKeysFor(String appId) throws IOException {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String keyMappingId = Utils.resolveFromContext("keyMappingId").toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getUpdateKey(getBaseUrl(), actualAppId, keyMappingId), headers);
        TestContext.set("httpResponse", response);
    }

    /**
     * Generates OAuth2 client credentials (consumer key and secret) for an application.
     * The generated consumer key, consumer secret, and key mapping ID are stored in the test context.
     * 
     * @param appId Context key containing the application ID
     * @param payload Context key containing the key generation JSON payload
     */
    @When("I generate client credentials for application id {string} with payload {string}")
    public void iGenerateClientCredentialsForApplication(String appId, String payload) throws Exception {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        // Resolve any {{contextKey}} placeholders (e.g. a captured key-manager name for a specific-KM key-gen).
        String jsonPayload = Utils.resolveContextPlaceholders(Utils.resolveFromContext(payload).toString());

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getGenerateApplicationKeysURL(getBaseUrl(), actualAppId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        // Only extract key fields on success — a non-2xx (e.g. a KM that denies the user's role → 403) has no
        // consumerKey, and extracting it would throw before the feature can assert the rejection status.
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            TestContext.set("consumerKey", Utils.extractValueFromPayload(response.getData(), "consumerKey"));
            TestContext.set("consumerSecret", Utils.extractValueFromPayload(response.getData(), "consumerSecret"));
            TestContext.set("keyMappingId", Utils.extractValueFromPayload(response.getData(), "keyMappingId"));
        }
    }

    /**
     * Registers a pre-existing (BYO) OAuth client directly in the resident Key Manager via the DCR
     * endpoint, capturing its raw {@code clientId}/{@code clientSecret} into context under
     * {@code <idKey>ClientId} / {@code <idKey>ClientSecret}. This is the v2-native analogue of the legacy
     * ApplicationTestCase#createOIDCApplication (which used the SOAP OAuthAdminService + a ServiceProvider):
     * DCR produces a real OAuth2 consumer app in the resident IS, which is exactly what the subsequent
     * map-keys step needs to bind to a Developer Portal application. The client name is made runner-unique
     * so parallel scenarios never collide.
     *
     * @param clientName base name for the BYO OAuth client (uniquified per runner)
     * @param idKey      context prefix under which the client id/secret are stored
     */
    @When("I register an OAuth client {string} as {string}")
    public void iRegisterOAuthClient(String clientName, String idKey) throws IOException {

        User actor = Identity.resolveActor(Identity.actingActorRef());
        String uniqueName = Names.unique(clientName);

        JSONObject json = new JSONObject();
        json.put("callbackUrl", "http://localhost:8490/callback");
        json.put("clientName", uniqueName);
        json.put("grantType", "client_credentials password refresh_token");
        json.put("saasApp", true);
        json.put("owner", actor.getUserName());

        String encodedCredentials = Base64.getEncoder().encodeToString(
                (actor.getUserName() + ":" + actor.getPassword()).getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + encodedCredentials);

        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPost(
                Utils.getDCREndpointURL(getBaseUrl()), headers, json.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
        Assert.assertEquals(response.getResponseCode(), 200,
                "BYO OAuth client registration (DCR) failed: " + response.getData());
        TestContext.set(idKey + "ClientId", Utils.extractValueFromPayload(response.getData(), "clientId"));
        TestContext.set(idKey + "ClientSecret", Utils.extractValueFromPayload(response.getData(), "clientSecret"));
    }

    /**
     * Maps a pre-existing (BYO) OAuth client's consumer key/secret to a Developer Portal application against
     * a named key manager (typically {@code "Resident Key Manager"}). Ports the legacy
     * ApplicationTestCase#mapApplicationKeys / mapApplicationKeysNegative arc. Non-asserting — the feature
     * asserts the status (200 on a clean map, 409 "Key Mappings already exists" when keys are already
     * generated/mapped) — so this one step serves both the positive and negative scenarios.
     *
     * @param idKey      context prefix of a client registered via the register-OAuth-client step
     * @param appId      context key holding the target application id
     * @param keyManager the key manager to map against (e.g. "Resident Key Manager")
     */
    @When("I map OAuth client {string} to application {string} via key manager {string}")
    public void iMapOAuthClientToApplication(String idKey, String appId, String keyManager) throws IOException {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String consumerKey = Utils.resolveFromContext(idKey + "ClientId").toString();
        String consumerSecret = Utils.resolveFromContext(idKey + "ClientSecret").toString();

        JSONObject json = new JSONObject();
        json.put("consumerKey", consumerKey);
        json.put("consumerSecret", consumerSecret);
        json.put("keyType", "PRODUCTION");
        json.put("keyManager", keyManager);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPost(
                Utils.getMapKeysURL(getBaseUrl(), actualAppId), headers, json.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /**
     * Cleans up an application's key registration for a key mapping (the DevPortal clean-up operation used
     * after a failed/partial key generation). Ports ApplicationTestCase#testCleanupApplicationRegistrationById.
     * Non-asserting — the feature asserts the status.
     *
     * @param appId           context key holding the application id
     * @param keyMappingIdKey context key holding the key mapping id
     */
    @When("I clean up the key registration for application {string} with key mapping {string}")
    public void iCleanUpKeyRegistration(String appId, String keyMappingIdKey) throws IOException {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String keyMappingId = Utils.resolveFromContext(keyMappingIdKey).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPost(
                Utils.getCleanupRegistrationURL(getBaseUrl(), actualAppId, keyMappingId), headers, "",
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /**
     * Asserts that the backend JWT (the {@code X-JWT-Assertion} request header the gateway injects towards the
     * backend, reflected back by the /reflect-headers backend route) carries the given application attribute.
     * The last {@code httpResponse} body is expected to be {@code {"headers": {"x-jwt-assertion": "<jwt>", ...}}};
     * the JWT payload segment is base64-decoded and checked for the attribute name and value. Ports the
     * applicationAttributes-claim assertion of ApplicationAttributesTestCase.
     *
     * @param attributeName  the application attribute name (e.g. "External Reference Id")
     * @param attributeValue the expected value (e.g. "c1237890")
     */
    @Then("The reflected backend JWT should contain application attribute {string} with value {string}")
    public void theReflectedBackendJwtShouldContainAttribute(String attributeName, String attributeValue) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertNotNull(response, "No invocation response captured");
        JSONObject body = new JSONObject(response.getData());
        Assert.assertTrue(body.has("headers"),
                "Reflected response has no 'headers' object: " + response.getData());
        JSONObject headers = body.getJSONObject("headers");

        String jwt = null;
        for (String key : headers.keySet()) {
            if ("x-jwt-assertion".equalsIgnoreCase(key)) {
                jwt = headers.getString(key);
                break;
            }
        }
        Assert.assertNotNull(jwt, "No X-JWT-Assertion header reached the backend: " + headers);

        String[] segments = jwt.split("\\.");
        Assert.assertTrue(segments.length >= 2, "Malformed JWT assertion (expected >= 2 segments): " + jwt);
        // The gateway emits the assertion base64-encoded (config: encoding = "base64"); decode tolerantly
        // (URL-safe first, then standard) so either encoding is accepted.
        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(segments[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            payload = new String(Base64.getDecoder().decode(segments[1]), StandardCharsets.UTF_8);
        }

        Assert.assertTrue(payload.contains(attributeName),
                "Decoded backend JWT does not contain attribute name '" + attributeName + "': " + payload);
        Assert.assertTrue(payload.contains(attributeValue),
                "Decoded backend JWT does not contain attribute value '" + attributeValue + "': " + payload);
    }

    /**
     * Regenerates (rotates) the consumer secret of an application's keys for a key type. Ports
     * ApplicationConsumerSecretRegenerateTestCase — the response carries a NEW consumer secret. Non-asserting.
     */
    @When("I regenerate the consumer secret for application {string} with key mapping {string}")
    public void iRegenerateConsumerSecret(String appId, String keyMappingIdKey) throws IOException {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String keyMappingId = Utils.resolveFromContext(keyMappingIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPost(
                Utils.getRegenerateConsumerSecretURL(getBaseUrl(), actualAppId, keyMappingId), headers, "",
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /**
     * Requests an OAuth2 access token for an application using the generated client credentials.
     * The consumer secret from the context is injected into the payload before sending the request.
     * The generated access token is stored in the test context.
     * 
     * @param appId Context key containing the application ID
     * @param payload Context key containing the token request JSON payload
     */
    @When("I request an access token for application id {string} using payload {string}")
    public void iRequestAccessToken(String appId, String payload) throws Exception {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String keyMappingId = Utils.resolveFromContext("keyMappingId").toString();
        String consumerSecret = Utils.resolveFromContext("consumerSecret").toString();

        // Add consumer secret to the payload
        String jsonPayload = Utils.resolveFromContext(payload).toString();
        jsonPayload = jsonPayload.replace("{{appConsumerSecret}}", consumerSecret);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getGenerateApplicationTokenURL(getBaseUrl(), actualAppId, keyMappingId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        String accessToken = Utils.extractValueFromPayload(response.getData(), "accessToken").toString();
        TestContext.set("generatedAccessToken", accessToken);
    }

    /**
     * Requests an OAuth2 access token for the current user via the token endpoint using the
     * password grant, authenticated with the application's generated client credentials
     * (consumerKey/consumerSecret in context). The raw token response is stored as "httpResponse",
     * the access token as "generatedAccessToken", and the refresh token (if any) as "refreshToken".
     *
     * @param scope OAuth scope to request (may be empty for no explicit scope)
     */
    @When("I request an OAuth access token for the current user using password grant with scope {string}")
    public void iRequestOAuthAccessTokenWithScope(String scope) throws Exception {

        User currentUser = Identity.defaultActor();

        StringBuilder body = new StringBuilder("grant_type=password")
                .append("&username=").append(urlEncode(currentUser.getUserName()))
                .append("&password=").append(urlEncode(currentUser.getPassword()));
        if (scope != null && !scope.isEmpty()) {
            body.append("&scope=").append(urlEncode(scope));
        }

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(getBaseUrl()),
                clientCredentialsHeader(), body.toString(), Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);

        TestContext.set("httpResponse", response);
        captureTokens(response);
    }

    /**
     * Requests a new access token using the refresh-token grant, authenticated with the application's
     * client credentials. Stores the new tokens and the raw response in context.
     *
     * @param refreshTokenKey Context key holding the refresh token to exchange
     */
    @When("I request a new OAuth access token using refresh token {string}")
    public void iRequestTokenUsingRefreshToken(String refreshTokenKey) throws Exception {

        String refreshToken = Utils.resolveFromContext(refreshTokenKey).toString();

        String body = "grant_type=refresh_token&refresh_token=" + urlEncode(refreshToken);

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(getBaseUrl()),
                clientCredentialsHeader(), body, Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);

        TestContext.set("httpResponse", response);
        captureTokens(response);
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Revokes the given OAuth access token via the revocation endpoint, authenticated with the
     * application's client credentials. Stores the response in context.
     *
     * @param tokenKey Context key holding the access token to revoke
     */
    @When("I revoke the OAuth access token {string}")
    public void iRevokeOAuthAccessToken(String tokenKey) throws Exception {

        String token = Utils.resolveFromContext(tokenKey).toString();

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getRevokeEndpointURL(getBaseUrl()),
                clientCredentialsHeader(), "token=" + token,
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);

        TestContext.set("httpResponse", response);
    }

    /**
     * Builds a Basic auth header from the application's generated client credentials
     * (consumerKey/consumerSecret) held in context.
     */
    private Map<String, String> clientCredentialsHeader() {

        String consumerKey = Utils.resolveFromContext("consumerKey").toString();
        String consumerSecret = Utils.resolveFromContext("consumerSecret").toString();
        String credentials = Base64.getEncoder().encodeToString(
                (consumerKey + ":" + consumerSecret).getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + credentials);
        return headers;
    }

    /**
     * Extracts access_token and refresh_token (when present) from a token-endpoint response into context.
     * Tolerates responses without a refresh token (e.g. some scope/grant combinations).
     */
    private void captureTokens(HttpResponse response) {

        JSONObject json;
        try {
            json = new JSONObject(response.getData());
        } catch (Exception e) {
            return;
        }
        if (json.has("access_token")) {
            TestContext.set("generatedAccessToken", json.getString("access_token"));
        }
        if (json.has("refresh_token")) {
            TestContext.set("refreshToken", json.getString("refresh_token"));
        }
    }

    /**
     * Asserts that the access token stored as "generatedAccessToken" is a self-contained JWT:
     * three base64url-encoded segments whose header decodes to JSON containing an "alg" claim.
     */
    @Then("The generated access token should be in JWT format")
    public void theGeneratedAccessTokenShouldBeInJWTFormat() {

        String token = Utils.resolveFromContext("generatedAccessToken").toString();
        String[] parts = token.split("\\.");
        Assert.assertEquals(parts.length, 3,
                "Access token is not in JWT format (expected 3 dot-separated segments): " + token);

        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        JSONObject header = new JSONObject(headerJson);
        Assert.assertTrue(header.has("alg"),
                "JWT header does not contain an 'alg' claim: " + headerJson);
    }

    /**
     * Generates an API Key for an application.
     *
     * @param appId Context key containing the application ID
     * @param payload Context key containing the API key generation JSON payload
     */
    @And("I request an api key for application id {string} using payload {string}")
    public void iRequestAnApiKeyForApplicationIdUsingPayload(String appId, String payload) throws IOException {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getGenerateAPIKeyURL(getBaseUrl(), actualAppId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        String apikey = Utils.extractValueFromPayload(response.getData(), "apikey").toString();
        TestContext.set("apiKey", apikey);
    }

    /**
     * Lists an application's PRODUCTION API keys and stores the FIRST key's {@code keyUUID} under {@code ctxKey}.
     * A store-generated (opaque) API key is revoked by its keyUUID (not the key value), so this captures it for
     * the revoke step. Each scenario creates a fresh application with a single key, so the first entry is it.
     *
     * @param appId  context key holding the application id
     * @param ctxKey context key to store the captured keyUUID under
     */
    @When("I retrieve the api key UUID for application id {string} as {string}")
    public void iRetrieveApiKeyUuid(String appId, String ctxKey) throws IOException {
        String actualAppId = Utils.resolveFromContext(appId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getListAPIKeysURL(getBaseUrl(), actualAppId), headers);
        TestContext.set("httpResponse", response);
        // The endpoint returns either a bare array [{...}] or a {"count":n,"list":[...]} wrapper depending on
        // the pack — handle both. Each scenario's app has a single key, so the first entry is the one to revoke.
        String data = response.getData().trim();
        org.json.JSONArray list = data.startsWith("[")
                ? new org.json.JSONArray(data)
                : new org.json.JSONObject(data).getJSONArray("list");
        String uuid = list.getJSONObject(0).getString("keyUUID");
        TestContext.set(Utils.normalizeContextKey(ctxKey), uuid);
    }

    /**
     * Revokes a store-generated (opaque) application API key by its {@code keyUUID} via the DevPortal revoke
     * endpoint ({@code applications/{id}/api-keys/PRODUCTION/revoke}, body {@code {"keyUUID":"<uuid>"}}). After
     * revocation the key must be rejected at the gateway (401). Ports the opaque api-key revocation of
     * APISecurityTestCase (which revokes by keyUUID — revoke-by-value rejects the opaque key as invalid).
     *
     * @param uuidRef context key holding the keyUUID to revoke
     * @param appId   context key holding the application id
     */
    @When("I revoke the api key with UUID {string} for application id {string}")
    public void iRevokeApiKeyByUuid(String uuidRef, String appId) throws IOException {
        String actualAppId = Utils.resolveFromContext(appId).toString();
        String uuid = Utils.resolveFromContext(uuidRef).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        String payload = "{\"keyUUID\":\"" + uuid + "\"}";

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getRevokeAPIKeyURL(getBaseUrl(), actualAppId), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /**
     * Deletes a subscription by its ID.
     *
     * @param subscriptionId Context key containing the subscription ID to delete
     */
    @When("I delete the subscription with id {string}")
    public void iDeleteSubscription(String subscriptionId) throws Exception {
        String actualSubscriptionId = Utils.resolveFromContext(subscriptionId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance().doDelete(Utils.getSubscriptionURL(getBaseUrl(),
                actualSubscriptionId), headers);

        TestContext.set("httpResponse", response);
    }

    /**
     * Updates a subscription's throttling policy (subscription plan).
     * The subscription payload should be stored in the test context under the key "subscriptionPayload".
     *
     * @param subscriptionId Context key containing the subscription ID to update
     * @param subscriptionPlan The new throttling policy/plan (e.g., "Gold", "Silver", "Bronze", "Unlimited")
     */
    @When("I update the subscription {string} with subscription plan {string}")
    public void iUpdateTheSubscriptionWithSubscriptionPlan(String subscriptionId, String subscriptionPlan) throws IOException {

        String actualSubscriptionId = Utils.resolveFromContext(subscriptionId).toString();

        // Add application id and API id to the payload
        String jsonPayload = Utils.resolveFromContext("subscriptionPayload").toString();
        // Set the throttling policy to the requested plan regardless of the current value or JSON spacing
        // (the payload may be a pretty-printed template or a compact subscription response).
        jsonPayload = jsonPayload.replaceAll("(\"throttlingPolicy\"\\s*:\\s*)\"[^\"]*\"",
                "$1\"" + subscriptionPlan + "\"");

        // Resolve application/api id placeholders when the payload is a hand-built template. When the
        // payload is taken from an actual subscription response (e.g. migration flow) it carries real
        // ids and no placeholders, so these replacements are a no-op.
        if (jsonPayload.contains("{{applicationId}}") && TestContext.contains("createdAppId")) {
            jsonPayload = jsonPayload.replace("{{applicationId}}", TestContext.get("createdAppId").toString());
        }
        if (jsonPayload.contains("{{apiId}}") && TestContext.contains("createdApiId")) {
            jsonPayload = jsonPayload.replace("{{apiId}}", TestContext.get("createdApiId").toString());
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance().doPut(Utils.getSubscriptionURL(getBaseUrl(), actualSubscriptionId),
                headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
    }

    /**
     * Retrieves the details of a specific subscription by its ID.
     *
     * @param subscriptionId Context key containing the subscription ID to retrieve
     */
    @When("I get the subscription with id {string}")
    public void iGetSubscription(String subscriptionId) throws Exception {

        String actualSubscriptionId = Utils.resolveFromContext(subscriptionId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance().doGet(Utils.getSubscriptionURL(getBaseUrl(),
                actualSubscriptionId), headers);

        TestContext.set("httpResponse", response);
    }

    /**
     * Verifies that a specific subscription ID exists in the list of all subscriptions.
     * This assertion step checks the most recent HTTP response (expected to contain a list of subscriptions)
     * to ensure the subscription was successfully created and is available.
     * 
     * @param subscriptionId Context key containing the subscription ID to verify
     */
    @Then("The subscription with id {string} should be in the list of all subscriptions")
    public void subscriptionShouldBeInTheListOfAllSubscriptions(String subscriptionId) {

        String actualSubscriptionId = Utils.resolveFromContext(subscriptionId).toString();
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        JSONArray subscriptionsList= new JSONObject(response.getData()).getJSONArray("list");

        boolean found = IntStream.range(0, subscriptionsList.length())
                .mapToObj(subscriptionsList::getJSONObject)
                .anyMatch(subJson -> actualSubscriptionId.equals(subJson.optString("subscriptionId", null)));

        Assert.assertTrue(found, "Subscription with id " + actualSubscriptionId + " not found in the list.");
    }

    /**
     * Composite step definition for,
     * Application creation - put the 'createdAppId' in context
     * Generate credentials for application - put 'consumerKey', 'consumerSecret' , and 'keyMappingId' in context
     * Subscribe to a given apiId - put 'subscriptionId' in context
     * Generate access tokens - put 'generatedAccessToken' in context
     *
     * @param apiId Api to be subscribed
     */
    @When("I have set up application with keys, subscribed to API {string}, and obtained access token for {string}")
    public void iSetupApplicationSubscribeAndGetToken(String apiId, String subscriptionID) throws Exception {
        iSetupApplicationSubscribeAndGetTokenWithPlan(apiId, "Bronze", subscriptionID);
    }

    /**
     * Plan-parameterised variant of the application-setup composite — subscribes with the given business plan
     * instead of the default {@code Bronze}. Needed for API types whose allowed tiers differ, e.g. a WebSocket
     * (async) API only accepts async plans such as {@code AsyncUnlimited} (a sync plan → 403 "not allowed").
     *
     * @param apiId          context key holding the API id
     * @param plan           the subscription business plan (e.g. {@code AsyncUnlimited})
     * @param subscriptionID context key to store the created subscription id under
     */
    @When("I have set up application with keys, subscribed to API {string} with plan {string}, and obtained access token for {string}")
    public void iSetupApplicationSubscribeAndGetTokenWithPlan(String apiId, String plan, String subscriptionID)
            throws Exception {

        // create an application
        baseSteps.putJsonPayloadFromFile("artifacts/payloads/create_apim_test_app.json", "<createAppPayload>");
        iCreateAnApplicationWithJsonPayload("<createAppPayload>");
        baseSteps.theResponseStatusCodeShouldBe(201);

        // generate credentials for application
        baseSteps.putJsonPayloadInContext("<generateApplicationKeysPayload>", "{\"keyType\": \"PRODUCTION\"," +
                "\"grantTypesToBeSupported\": [\"client_credentials\"]}");
        iGenerateClientCredentialsForApplication("<createdAppId>", "<generateApplicationKeysPayload>");
        baseSteps.theResponseStatusCodeShouldBe(200);

        // subscribe to an api with that created application
        baseSteps.putJsonPayloadInContext("<apiSubscriptionPayload>", "{\"applicationId\": \"{{applicationId}}\"," +
                "\"apiId\": \"{{apiId}}\",\"throttlingPolicy\": \"" + plan + "\"}");
        iSubscribeToApi(apiId, "<createdAppId>", "<apiSubscriptionPayload>", subscriptionID);
        baseSteps.theResponseStatusCodeShouldBe(201);

        // generate access token
        baseSteps.putJsonPayloadInContext("<createApplicationAccessTokenPayload>", "{\"consumerSecret\": \"{{appConsumerSecret}}\"," +
                "\"validityPeriod\": 3600}");
        iRequestAccessToken("<createdAppId>", "<createApplicationAccessTokenPayload>");
        baseSteps.theResponseStatusCodeShouldBe(200);
    }

    /**
     * Token-type-parameterised variant of the application-setup composite — creates the application with the given
     * {@code tokenType} ({@code JWT} = self-contained signed token, the product default; {@code OAUTH} = opaque
     * UUID token), then generates keys, subscribes on the given plan, and obtains an access token. Needed to
     * exercise BOTH token-issuance paths (e.g. a WebSocket API invoked with a JWT token and with an OAUTH/opaque
     * token — the legacy WebSocketAPITestCase covers both).
     *
     * @param tokenType      the application token type: {@code JWT} or {@code OAUTH}
     * @param apiId          context key holding the API id
     * @param plan           the subscription business plan (e.g. {@code AsyncUnlimited})
     * @param subscriptionID context key to store the created subscription id under
     */
    @When("I have set up a {string} token type application with keys, subscribed to API {string} with plan {string}, and obtained access token for {string}")
    public void iSetupTokenTypeApplicationSubscribeAndGetTokenWithPlan(String tokenType, String apiId, String plan,
                                                                       String subscriptionID) throws Exception {

        // create an application of the given token type
        baseSteps.putJsonPayloadFromFile("artifacts/payloads/create_apim_test_app.json", "<createAppPayload>");
        baseSteps.iSetFieldInPayload("tokenType", tokenType, "<createAppPayload>");
        iCreateAnApplicationWithJsonPayload("<createAppPayload>");
        baseSteps.theResponseStatusCodeShouldBe(201);

        // generate credentials for application
        baseSteps.putJsonPayloadInContext("<generateApplicationKeysPayload>", "{\"keyType\": \"PRODUCTION\"," +
                "\"grantTypesToBeSupported\": [\"client_credentials\"]}");
        iGenerateClientCredentialsForApplication("<createdAppId>", "<generateApplicationKeysPayload>");
        baseSteps.theResponseStatusCodeShouldBe(200);

        // subscribe to an api with that created application
        baseSteps.putJsonPayloadInContext("<apiSubscriptionPayload>", "{\"applicationId\": \"{{applicationId}}\"," +
                "\"apiId\": \"{{apiId}}\",\"throttlingPolicy\": \"" + plan + "\"}");
        iSubscribeToApi(apiId, "<createdAppId>", "<apiSubscriptionPayload>", subscriptionID);
        baseSteps.theResponseStatusCodeShouldBe(201);

        // generate access token
        baseSteps.putJsonPayloadInContext("<createApplicationAccessTokenPayload>", "{\"consumerSecret\": \"{{appConsumerSecret}}\"," +
                "\"validityPeriod\": 3600}");
        iRequestAccessToken("<createdAppId>", "<createApplicationAccessTokenPayload>");
        baseSteps.theResponseStatusCodeShouldBe(200);
    }

    /**
     * Composite step definition for,
     * Application creation - put the 'createdAppId' in context
     * Generate credentials for application - put 'consumerKey', 'consumerSecret' , and 'keyMappingId' in context
     */
    @When("I have set up a application with keys")
    public void iHaveSetUpAApplicationWithKeys() throws Exception {

        // create an application
        baseSteps.putJsonPayloadFromFile("artifacts/payloads/create_apim_test_app.json", "<createAppPayload>");
        iCreateAnApplicationWithJsonPayload("<createAppPayload>");

        // generate credentials for application
        baseSteps.putJsonPayloadInContext("<generateApplicationKeysPayload>", "{\"keyType\": \"PRODUCTION\"," +
                "\"grantTypesToBeSupported\": [\"client_credentials\"]}");
        iGenerateClientCredentialsForApplication("<createdAppId>", "<generateApplicationKeysPayload>");
        baseSteps.theResponseStatusCodeShouldBe(200);
    }

    /**
     * Composite step definition for,
     * Subscribe to a given apiId - put 'subscriptionId' in context
     * Generate access tokens - put 'generatedAccessToken' in context
     *
     * @param resourceID resource to be subscribed
     */
    @And("I subscribe to resource {string}, with {string} and obtained access token for {string} with scope {string}")
    public void iSubscribeToResourceAndObtainedAccessToken(String resourceID, String appId, String subscriptionID, String scope) throws Exception {

        // subscribe to an api with that created application
        baseSteps.putJsonPayloadInContext("<apiSubscriptionPayload>", "{\"applicationId\": \"{{applicationId}}\"," +
                "\"apiId\": \"{{apiId}}\",\"throttlingPolicy\": \"Bronze\"}");
        iSubscribeToApi(resourceID, appId, "<apiSubscriptionPayload>", subscriptionID);

        // generate access token
        String tokenPayload;
        if (scope != null && !scope.isEmpty()) {
            tokenPayload = "{\"consumerSecret\": \"{{appConsumerSecret}}\"," +
                    "\"validityPeriod\": 3600," +
                    "\"scopes\": [\"" + scope + "\"]}";
        } else {
            tokenPayload = "{\"consumerSecret\": \"{{appConsumerSecret}}\"," +
                    "\"validityPeriod\": 3600}";
        }

        baseSteps.putJsonPayloadInContext("<createApplicationAccessTokenPayload>", tokenPayload);
        iRequestAccessToken(appId, "<createApplicationAccessTokenPayload>");
    }

    /**
     * Searches for APIs in the Developer Portal using a search query.
     * The search query can include filters such as name, version, provider, etc.
     *
     * @param query The search query string
     */
    @When("I search DevPortal APIs with query {string}")
    public void iSearchDevPortalAPIsWithQuery(String query) throws IOException, InterruptedException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.devportalToken());

        // Resolve any {{contextKey}} placeholders so a query can target a uniquely-generated value, e.g.
        // "name:{{createdApiName}}" — necessary now that resource names are randomized by ${UNIQUE:...}.
        query = Utils.resolveContextPlaceholders(query);
        String url = Utils.getApiSearchURL(getBaseUrl(), query);
        long endTime = System.currentTimeMillis() + Constants.DEPLOYMENT_WAIT_TIME;

        // DevPortal search is backed by an asynchronous (Solr) index, so a freshly published API may
        // not be searchable immediately. Retry while the result set is empty until it appears or times out.
        HttpResponse response;
        while (true) {
            response = SimpleHTTPClient.getInstance().doGet(url, headers);
            boolean empty = response == null || response.getResponseCode() != 200
                    || new JSONObject(response.getData()).optInt("count", 0) == 0;
            if (!empty || System.currentTimeMillis() >= endTime) {
                break;
            }
            Thread.sleep(2000);
        }

        TestContext.set("httpResponse", response);
    }

    /**
     * Retrieves all documents available for an API in the Developer Portal.
     *
     * @param resourceId Context key containing the API ID
     */
    @And("I retrieve devportal documents for {string}")
    public void iRetrieveDevportalDocumentsFor(String resourceId) throws IOException {
        String actualApiId = Utils.resolveFromContext(resourceId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApiDocumentsURL(getBaseUrl(), actualApiId), headers);

        TestContext.set("httpResponse", response);
    }

    // ---- Key manager configuration (admin) -------------------------------------------------------------

    private Map<String, String> adminAuthHeaders() {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        return headers;
    }

    /**
     * Adds a system-scope role-alias mapping (admin REST {@code PUT /role-aliases}): maps {@code role} to include
     * {@code alias}. Ports APISystemScopesTestCase#testAddScopeMapping. Non-asserting.
     */
    @When("I set the role alias {string} for role {string}")
    public void iSetRoleAlias(String alias, String role) throws IOException {

        JSONObject entry = new JSONObject().put("role", role).put("aliases", new JSONArray().put(alias));
        JSONObject payload = new JSONObject().put("count", 1).put("list", new JSONArray().put(entry));
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPut(Utils.getRoleAliasesURL(getBaseUrl()),
                adminAuthHeaders(), payload.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /** Retrieves the system-scope role-alias mappings (admin REST {@code GET /role-aliases}). Non-asserting. */
    @When("I retrieve the role aliases")
    public void iRetrieveRoleAliases() throws IOException {

        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doGet(Utils.getRoleAliasesURL(getBaseUrl()),
                adminAuthHeaders());
        TestContext.set("httpResponse", response);
    }

    /**
     * Clears all system-scope role-alias mappings (admin REST {@code PUT /role-aliases} with an empty list) —
     * the teardown for the role-alias mapping test. Non-asserting.
     */
    @When("I clear all role aliases")
    public void iClearRoleAliases() throws IOException {

        JSONObject payload = new JSONObject().put("count", 0).put("list", new JSONArray());
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPut(Utils.getRoleAliasesURL(getBaseUrl()),
                adminAuthHeaders(), payload.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /** Maps a friendly throttling-policy kind to the export/import {@code type} token. */
    private static String throttleExportType(String friendlyType) {
        switch (friendlyType) {
            case "subscription": return "sub";
            case "application":  return "app";
            case "advanced":     return "api";
            case "custom":       return "global";
            default:             return friendlyType; // allow a raw token to pass through
        }
    }

    /**
     * Exports a throttling policy (admin REST {@code GET /throttling/policies/export?name=&type=}). {@code kind}
     * is the friendly type (subscription/application/advanced/custom); {@code nameKey} is the context key holding
     * the policy name (as stored by the create steps). On 2xx the exported JSON body is stored under
     * {@code exportKey} for a subsequent import. Non-asserting. Ports ThrottlePolicyExportImportTestCase (export).
     */
    @When("I export the {string} throttling policy named {string} as {string}")
    public void iExportThrottlePolicy(String kind, String nameKey, String exportKey) throws IOException {

        String name = Utils.resolveFromContext(nameKey).toString();
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doGet(
                Utils.getThrottlePolicyExportURL(getBaseUrl(), name, throttleExportType(kind)), adminAuthHeaders());
        TestContext.set("httpResponse", response);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            TestContext.set(Utils.normalizeContextKey(exportKey), response.getData());
        }
    }

    /**
     * Imports a throttling policy from a previously-exported JSON body (admin REST
     * {@code POST /throttling/policies/import?overwrite=} as a multipart {@code file}). {@code overwrite=false}
     * on an existing policy → 409; {@code overwrite=true} → 200; on an absent policy → 201. Non-asserting. Ports
     * ThrottlePolicyExportImportTestCase (import: conflict / update / new).
     */
    @When("I import the throttling policy from {string} with overwrite {string}")
    public void iImportThrottlePolicy(String exportKey, String overwrite) throws IOException {

        String body = Utils.resolveFromContext(exportKey).toString();
        java.io.File temp = java.io.File.createTempFile("throttle-policy", ".json");
        temp.deleteOnExit();
        java.nio.file.Files.write(temp.toPath(), body.getBytes(StandardCharsets.UTF_8));

        Map<String, java.io.File> files = new HashMap<>();
        files.put("file", temp);
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPostMultipartWithFiles(
                Utils.getThrottlePolicyImportURL(getBaseUrl(), overwrite), adminAuthHeaders(), files, new HashMap<>());
        TestContext.set("httpResponse", response);
    }

    /** Loads a key-manager JSON payload off the classpath, resolving {@code ${UNIQUE:...}} name placeholders. */
    private JSONObject loadKeyManagerPayload(String resourcePath) throws IOException {

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }
            String raw = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return new JSONObject(Utils.resolvePayloadPlaceholders(raw));
        }
    }

    private HttpResponse postKeyManager(JSONObject payload) throws IOException {

        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getKeyManagersURL(getBaseUrl()),
                adminAuthHeaders(), payload.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
        return response;
    }

    /**
     * Creates a key manager from a JSON payload fixture, asserts 201, stores the new id under {@code idKey} (and
     * its name under {@code <idKey>Name} for the duplicate-name negative), and registers it for teardown.
     */
    @When("I create a key manager from payload {string} as {string}")
    public void iCreateKeyManager(String resourcePath, String idKey) throws IOException {

        HttpResponse response = postKeyManager(loadKeyManagerPayload(resourcePath));
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        Object kmId = Utils.extractValueFromPayload(response.getData(), "id");
        Object kmName = Utils.extractValueFromPayload(response.getData(), "name");
        TestContext.set(idKey, kmId);
        TestContext.set(idKey + "Name", kmName);
        ResourceCleanup.register(Constants.CREATED_KEY_MANAGER_IDS, kmId);
    }

    /**
     * Attempts to create a key manager from a payload with its connector config (additionalProperties) stripped
     * — the mandatory-parameter negative. Non-asserting; the feature asserts the 400.
     */
    @When("I attempt to create a key manager from payload {string} without connector config")
    public void iAttemptToCreateKeyManagerWithoutConnectorConfig(String resourcePath) throws IOException {

        JSONObject payload = loadKeyManagerPayload(resourcePath);
        payload.remove("additionalProperties");
        postKeyManager(payload);
    }

    /**
     * Attempts to create a key manager from a payload but with its name overridden (resolving {@code {{...}}}
     * from context) — used for the duplicate-name negative. Non-asserting; the feature asserts the 409.
     */
    @When("I attempt to create a key manager from payload {string} with name {string}")
    public void iAttemptToCreateKeyManagerWithName(String resourcePath, String name) throws IOException {

        JSONObject payload = loadKeyManagerPayload(resourcePath);
        payload.put("name", Utils.resolveContextPlaceholders(name));
        postKeyManager(payload);
    }

    /** Retrieves a single key manager by the id held under {@code idKey}. */
    @When("I retrieve the key manager {string}")
    public void iRetrieveKeyManager(String idKey) throws IOException {

        String kmId = Utils.resolveFromContext(idKey).toString();
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getKeyManagerByIdURL(getBaseUrl(), kmId), adminAuthHeaders());
        TestContext.set("httpResponse", response);
    }

    /** Lists all key managers. */
    @When("I retrieve all key managers")
    public void iRetrieveAllKeyManagers() throws IOException {

        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getKeyManagersURL(getBaseUrl()), adminAuthHeaders());
        TestContext.set("httpResponse", response);
    }

    /**
     * Updates a key manager's description in place: retrieves the current config, replaces its description, and
     * PUTs it back. Non-asserting — the feature asserts the status and the reflected description.
     */
    @When("I update the key manager {string} setting its description to {string}")
    public void iUpdateKeyManagerDescription(String idKey, String newDescription) throws IOException {

        String kmId = Utils.resolveFromContext(idKey).toString();
        HttpResponse current = SimpleHTTPClient.getInstance()
                .doGet(Utils.getKeyManagerByIdURL(getBaseUrl(), kmId), adminAuthHeaders());
        Assert.assertEquals(current.getResponseCode(), 200, current.getData());

        JSONObject km = new JSONObject(current.getData());
        km.put("description", newDescription);

        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPut(
                Utils.getKeyManagerByIdURL(getBaseUrl(), kmId), adminAuthHeaders(), km.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /** Deletes the key manager held under {@code idKey}. Non-asserting — the feature asserts the status. */
    @When("I delete the key manager {string}")
    public void iDeleteKeyManager(String idKey) throws IOException {

        String kmId = Utils.resolveFromContext(idKey).toString();
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getKeyManagerByIdURL(getBaseUrl(), kmId), adminAuthHeaders());
        TestContext.set("httpResponse", response);
    }

    /**
     * Creates a key manager from a payload fixture with {@code allowedOrganizations} set (comma-separated,
     * resolves {@code {{...}}}), asserts 201, stores the id and registers it. For org key-manager visibility.
     */
    @When("I create a key manager from payload {string} with allowed organizations {string} as {string}")
    public void iCreateKeyManagerWithAllowedOrgs(String resourcePath, String allowedOrgs, String idKey)
            throws IOException {

        JSONObject payload = loadKeyManagerPayload(resourcePath);
        JSONArray orgs = new JSONArray();
        for (String o : Utils.resolveContextPlaceholders(allowedOrgs).split("\\s*,\\s*")) {
            orgs.put(o);
        }
        payload.put("allowedOrganizations", orgs);
        HttpResponse response = postKeyManager(payload);
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        Object kmId = Utils.extractValueFromPayload(response.getData(), "id");
        TestContext.set(idKey, kmId);
        ResourceCleanup.register(Constants.CREATED_KEY_MANAGER_IDS, kmId);
    }

    /**
     * Creates a key manager from a payload fixture with {@code permissions} set to DENY a single role, so users
     * in that role are refused key generation via this KM (403). Asserts 201, stores the id under {@code idKey}
     * and the KM name under {@code <idKey>Name}. Ports the KM-permissions check of KeyManagersTestCase.
     */
    @When("I create a key manager from payload {string} denying role {string} as {string}")
    public void iCreateKeyManagerDenyingRole(String resourcePath, String role, String idKey) throws IOException {

        JSONObject payload = loadKeyManagerPayload(resourcePath);
        payload.put("permissions", new JSONObject()
                .put("permissionType", "DENY")
                .put("roles", new JSONArray().put(role)));
        HttpResponse response = postKeyManager(payload);
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        TestContext.set(idKey, Utils.extractValueFromPayload(response.getData(), "id"));
        TestContext.set(idKey + "Name", Utils.extractValueFromPayload(response.getData(), "name"));
        ResourceCleanup.register(Constants.CREATED_KEY_MANAGER_IDS,
                Utils.extractValueFromPayload(response.getData(), "id"));
    }

    /** Sets a key manager's {@code allowedOrganizations} in place (GET→modify→PUT). Non-asserting. */
    @When("I set the allowed organizations of key manager {string} to {string}")
    public void iSetKeyManagerAllowedOrgs(String idKey, String allowedOrgs) throws IOException {

        String kmId = Utils.resolveFromContext(idKey).toString();
        HttpResponse current = SimpleHTTPClient.getInstance()
                .doGet(Utils.getKeyManagerByIdURL(getBaseUrl(), kmId), adminAuthHeaders());
        Assert.assertEquals(current.getResponseCode(), 200, current.getData());
        JSONObject km = new JSONObject(current.getData());
        JSONArray orgs = new JSONArray();
        for (String o : Utils.resolveContextPlaceholders(allowedOrgs).split("\\s*,\\s*")) {
            orgs.put(o);
        }
        km.put("allowedOrganizations", orgs);
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPut(
                Utils.getKeyManagerByIdURL(getBaseUrl(), kmId), adminAuthHeaders(), km.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    // ---- Deny (blocking-condition) policies (admin) ----------------------------------------------------

    private HttpResponse postDenyPolicy(String conditionType, Object conditionValue) throws IOException {

        JSONObject dto = new JSONObject();
        dto.put("conditionType", conditionType);
        dto.put("conditionValue", conditionValue);
        dto.put("conditionStatus", true);
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getDenyPoliciesURL(getBaseUrl()),
                adminAuthHeaders(), dto.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
        return response;
    }

    private void registerCreatedDenyPolicy(HttpResponse response, String idKey) throws IOException {

        Object conditionId = Utils.extractValueFromPayload(response.getData(), "conditionId");
        TestContext.set(idKey, conditionId);
        ResourceCleanup.register(Constants.CREATED_DENY_POLICY_IDS, conditionId);
    }

    private JSONObject ipConditionValue(String fixedIp) {

        JSONObject value = new JSONObject();
        value.put("invert", false);
        value.put("fixedIp", fixedIp);
        return value;
    }

    /**
     * Creates a deny (blocking-condition) policy of a string-valued type (API context / USER / APPLICATION),
     * asserts 201, stores the condition id under {@code idKey} and registers it for teardown.
     */
    @When("I create a deny policy of type {string} with value {string} as {string}")
    public void iCreateDenyPolicy(String conditionType, String conditionValue, String idKey) throws IOException {

        HttpResponse response = postDenyPolicy(conditionType, Utils.resolveContextPlaceholders(conditionValue));
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        registerCreatedDenyPolicy(response, idKey);
    }

    /** Attempts to create a string-valued deny policy without asserting — for negatives. */
    @When("I attempt to create a deny policy of type {string} with value {string}")
    public void iAttemptToCreateDenyPolicy(String conditionType, String conditionValue) throws IOException {

        postDenyPolicy(conditionType, Utils.resolveContextPlaceholders(conditionValue));
    }

    /** Creates an IP deny policy for a fixed IP, asserts 201, stores + registers it. */
    @When("I create an IP deny policy for fixed IP {string} as {string}")
    public void iCreateIpDenyPolicy(String fixedIp, String idKey) throws IOException {

        HttpResponse response = postDenyPolicy("IP", ipConditionValue(fixedIp));
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        registerCreatedDenyPolicy(response, idKey);
    }

    /** Attempts to create an IP deny policy for a fixed IP without asserting — for negatives. */
    @When("I attempt to create an IP deny policy for fixed IP {string}")
    public void iAttemptToCreateIpDenyPolicy(String fixedIp) throws IOException {

        postDenyPolicy("IP", ipConditionValue(fixedIp));
    }

    /** Creates an IP-range deny policy, asserts 201, stores + registers it. */
    @When("I create an IP range deny policy from {string} to {string} as {string}")
    public void iCreateIpRangeDenyPolicy(String startingIp, String endingIp, String idKey) throws IOException {

        JSONObject value = new JSONObject();
        value.put("invert", false);
        value.put("startingIp", startingIp);
        value.put("endingIp", endingIp);
        HttpResponse response = postDenyPolicy("IPRANGE", value);
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        registerCreatedDenyPolicy(response, idKey);
    }

    /** Retrieves a single deny policy by the condition id held under {@code idKey}. */
    @When("I retrieve the deny policy {string}")
    public void iRetrieveDenyPolicy(String idKey) throws IOException {

        String id = Utils.resolveFromContext(idKey).toString();
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getDenyPolicyByIdURL(getBaseUrl(), id), adminAuthHeaders());
        TestContext.set("httpResponse", response);
    }

    /** Updates a deny policy's enabled status (PATCH conditionStatus). Non-asserting — the feature asserts. */
    @When("I set the deny policy {string} status to {string}")
    public void iSetDenyPolicyStatus(String idKey, String status) throws IOException {

        String id = Utils.resolveFromContext(idKey).toString();
        JSONObject dto = new JSONObject();
        dto.put("conditionStatus", Boolean.parseBoolean(status));
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPatch(
                Utils.getDenyPolicyByIdURL(getBaseUrl(), id), adminAuthHeaders(), dto.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /** Deletes the deny policy held under {@code idKey}. Non-asserting — the feature asserts the status. */
    @When("I delete the deny policy {string}")
    public void iDeleteDenyPolicy(String idKey) throws IOException {

        String id = Utils.resolveFromContext(idKey).toString();
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getDenyPolicyByIdURL(getBaseUrl(), id), adminAuthHeaders());
        TestContext.set("httpResponse", response);
    }

    /** Searches deny policies by condition type and value (query grammar: conditionType:X&conditionValue:Y). */
    @When("I search deny policies of type {string} with value {string}")
    public void iSearchDenyPolicies(String conditionType, String conditionValue) throws IOException {

        String query = urlEncode("conditionType:" + conditionType + "&conditionValue:" + conditionValue);
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getDenyPoliciesURL(getBaseUrl()) + "?query=" + query, adminAuthHeaders());
        TestContext.set("httpResponse", response);
    }

    // ---- Tenant configuration (admin) ------------------------------------------------------------------

    /** Retrieves the tenant configuration. */
    @When("I retrieve the tenant configuration")
    public void iRetrieveTenantConfiguration() throws IOException {

        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getTenantConfigURL(getBaseUrl()), adminAuthHeaders());
        TestContext.set("httpResponse", response);
    }

    /** Retrieves the tenant configuration JSON schema. */
    @When("I retrieve the tenant configuration schema")
    public void iRetrieveTenantConfigurationSchema() throws IOException {

        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getTenantConfigSchemaURL(getBaseUrl()), adminAuthHeaders());
        TestContext.set("httpResponse", response);
    }

    /** Captures the current tenant configuration body under {@code contextKey} (for a round-trip update/restore). */
    @When("I capture the tenant configuration as {string}")
    public void iCaptureTenantConfiguration(String contextKey) throws IOException {

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getTenantConfigURL(getBaseUrl()), adminAuthHeaders());
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        TestContext.set(Utils.normalizeContextKey(contextKey), response.getData());
    }

    /** Updates the tenant configuration with the JSON held under {@code contextKey}. Non-asserting. */
    @When("I update the tenant configuration from {string}")
    public void iUpdateTenantConfiguration(String contextKey) throws IOException {

        String payload = Utils.resolveFromContext(contextKey).toString();
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPut(Utils.getTenantConfigURL(getBaseUrl()),
                adminAuthHeaders(), payload, Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /** Attempts to update the tenant configuration with a syntactically-invalid (bad-signature) bearer JWT. */
    @When("I attempt to update the tenant configuration from {string} with an invalid token")
    public void iAttemptUpdateTenantConfigInvalidToken(String contextKey) throws IOException {

        String payload = Utils.resolveFromContext(contextKey).toString();
        // A structurally-valid JWT with a bogus signature the server cannot verify.
        String invalidJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
                + "eyJzdWIiOiJhZG1pbiIsInNjb3BlIjoib3BlbmlkIGFwaW06YWRtaW4ifQ.aW52YWxpZF9zaWduYXR1cmU";
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + invalidJwt);
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPut(Utils.getTenantConfigURL(getBaseUrl()),
                headers, payload, Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /**
     * Attempts to update the tenant configuration with the acting actor's PUBLISHER token, which lacks the
     * {@code apim:admin} scope — the non-admin negative. Non-asserting; the feature asserts the status.
     */
    @When("I attempt to update the tenant configuration from {string} without admin scope")
    public void iAttemptUpdateTenantConfigNonAdmin(String contextKey) throws IOException {

        String payload = Utils.resolveFromContext(contextKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPut(Utils.getTenantConfigURL(getBaseUrl()),
                headers, payload, Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    // ---- Application organization sharing (devportal) --------------------------------------------------

    /**
     * Creates a devportal application with a given {@code visibility} (PRIVATE / SHARED_WITH_ORG) as the acting
     * actor, asserts 201, stores the id under {@code idKey} and registers it for teardown. The name resolves
     * {@code ${UNIQUE:...}}.
     */
    @When("I create an application {string} with visibility {string} as {string}")
    public void iCreateApplicationWithVisibility(String name, String visibility, String idKey) throws IOException {

        JSONObject app = new JSONObject();
        app.put("name", Utils.resolvePayloadPlaceholders(name));
        app.put("throttlingPolicy", "Unlimited");
        app.put("description", "Org-sharing test application");
        app.put("tokenType", "JWT");
        app.put("visibility", visibility);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getApplicationCreateURL(getBaseUrl()),
                headers, app.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        Object appId = Utils.extractValueFromPayload(response.getData(), "applicationId");
        TestContext.set(idKey, appId);
        ResourceCleanup.register(Constants.CREATED_APPLICATION_IDS, appId);
    }

    /** Retrieves a devportal application by id as the acting actor (200 visible / 403 not). Non-asserting. */
    @When("I retrieve the application {string}")
    public void iRetrieveApplicationById(String idKey) throws IOException {

        String appId = Utils.resolveFromContext(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApplicationEndpointURL(getBaseUrl(), appId), headers);
        TestContext.set("httpResponse", response);
    }

    /** Updates a devportal application's visibility in place (GET→modify→PUT). Non-asserting. */
    @When("I set the visibility of application {string} to {string}")
    public void iSetApplicationVisibility(String idKey, String visibility) throws IOException {

        String appId = Utils.resolveFromContext(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        HttpResponse current = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApplicationEndpointURL(getBaseUrl(), appId), headers);
        Assert.assertEquals(current.getResponseCode(), 200, current.getData());
        JSONObject app = new JSONObject(current.getData());
        app.put("visibility", visibility);
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPut(
                Utils.getApplicationEndpointURL(getBaseUrl(), appId), headers, app.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    // ---- Consumer secret management (multiple client secrets, devportal) -------------------------------

    private Map<String, String> devportalAuthHeaders() {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        return headers;
    }

    /**
     * Generates an additional consumer secret for an application's key mapping (requires multiple-client-secrets
     * mode enabled). Non-asserting; on a 2xx the new {@code secretId} is stored under {@code secretIdKey}.
     *
     * @param description     value for the secret's {@code description} additional-property (may be empty)
     * @param appIdKey        context key holding the application id
     * @param keyMappingIdKey context key holding the key-mapping id
     * @param secretIdKey     context key to store the generated secret id
     */
    @When("I generate a consumer secret with description {string} for application {string} with key mapping {string} as {string}")
    public void iGenerateConsumerSecret(String description, String appIdKey, String keyMappingIdKey,
                                        String secretIdKey) throws IOException {

        String appId = Utils.resolveFromContext(appIdKey).toString();
        String keyMappingId = Utils.resolveFromContext(keyMappingIdKey).toString();

        JSONObject additionalProperties = new JSONObject();
        if (description != null && !description.isEmpty()) {
            additionalProperties.put("description", description);
        }
        JSONObject request = new JSONObject();
        request.put("additionalProperties", additionalProperties);

        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPost(
                Utils.getGenerateApplicationSecretURL(getBaseUrl(), appId, keyMappingId), devportalAuthHeaders(),
                request.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
        if (response.getResponseCode() < 300) {
            TestContext.set(secretIdKey, Utils.extractValueFromPayload(response.getData(), "secretId"));
        }
    }

    /** Lists an application key mapping's consumer secrets. */
    @When("I retrieve the consumer secrets for application {string} with key mapping {string}")
    public void iRetrieveConsumerSecrets(String appIdKey, String keyMappingIdKey) throws IOException {

        String appId = Utils.resolveFromContext(appIdKey).toString();
        String keyMappingId = Utils.resolveFromContext(keyMappingIdKey).toString();
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAllApplicationSecretsURL(getBaseUrl(), appId, keyMappingId), devportalAuthHeaders());
        TestContext.set("httpResponse", response);
    }

    /** Revokes a consumer secret (by the id held under {@code secretIdKey}). Non-asserting. */
    @When("I revoke the consumer secret {string} for application {string} with key mapping {string}")
    public void iRevokeConsumerSecret(String secretIdKey, String appIdKey, String keyMappingIdKey)
            throws IOException {

        String appId = Utils.resolveFromContext(appIdKey).toString();
        String keyMappingId = Utils.resolveFromContext(keyMappingIdKey).toString();
        String secretId = Utils.resolveFromContext(secretIdKey).toString();

        JSONObject request = new JSONObject();
        request.put("secretId", secretId);
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance().doPost(
                Utils.getRevokeApplicationSecretURL(getBaseUrl(), appId, keyMappingId), devportalAuthHeaders(),
                request.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /** Fetches an application's OAuth key details by key-mapping id. */
    @When("I fetch the oauth key details for application {string} with key mapping {string}")
    public void iFetchKeyDetailsByKeyMappingId(String appIdKey, String keyMappingIdKey) throws IOException {

        String appId = Utils.resolveFromContext(appIdKey).toString();
        String keyMappingId = Utils.resolveFromContext(keyMappingIdKey).toString();
        TestContext.remove("httpResponse");
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getUpdateKey(getBaseUrl(), appId, keyMappingId), devportalAuthHeaders());
        TestContext.set("httpResponse", response);
    }
}
