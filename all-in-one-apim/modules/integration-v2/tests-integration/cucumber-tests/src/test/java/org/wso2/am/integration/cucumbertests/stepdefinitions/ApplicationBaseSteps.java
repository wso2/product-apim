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
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.ISResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.IntegrationActors;
import org.wso2.am.integration.cucumbertests.utils.Names;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.JwtTestUtils;
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
import java.security.PrivateKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class ApplicationBaseSteps {

    BaseSteps baseSteps = new BaseSteps();

    /**
     * Creates a new application in the Developer Portal using a JSON payload.
     * The created application ID is stored as "createdAppId" in the test context for use in subsequent steps.
     * 
     * @param payload Context key containing the application creation JSON payload
     */
    @When("I create an application with payload {string}")
    public void iCreateAnApplicationWithJsonPayload(String payload) throws IOException {

        // Resolve any {{contextKey}} in the payload (e.g. an application name set to a generated unique value
        // referenced from context) — a file/doc-string payload with no placeholders is returned unchanged.
        String jsonPayload = Utils.resolveContextPlaceholders(TestContext.resolve(payload).toString());

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse applicationCreateResponse = Requests.post(Utils.getApplicationCreateURL(Utils.getBaseUrl()), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(applicationCreateResponse.getResponseCode(), 201, applicationCreateResponse.getData());
        Object createdAppId = Utils.extractValueFromPayload(applicationCreateResponse.getData(), "applicationId");
        TestContext.set("createdAppId", createdAppId);
        // Register for scenario teardown so a shared-server suite does not accumulate applications across scenarios.
        ResourceCleanup.register(Constants.CREATED_APPLICATION_IDS, createdAppId);
    }

    /**
     * Requests a client SDK for a subscribed/published API in a given programming language via the DevPortal
     * SDK-generation endpoint {@code GET /apis/{apiId}/sdks/{language}} and publishes the response for the
     * following assertion. Ports {@code restAPIStore.generateSDKUpdated} used by
     * CORSAccessControlAllowCredentialsHeaderTestCase#testAllSupportedSDKGeneration — a 200 response carries a
     * downloadable SDK zip. Uses the devportal (store) token of the acting actor.
     *
     * @param language context-resolvable SDK language (e.g. "java", "python", "swift5")
     * @param apiIdKey context key holding the API id
     */
    @When("I generate a client SDK in language {string} for API {string}")
    public void iGenerateClientSdk(String language, String apiIdKey) throws IOException {
        String lang = Utils.resolveContextPlaceholders(language);
        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        Requests.get(Utils.getApiSdkURL(Utils.getBaseUrl(), apiId, lang), headers);
    }

    /**
     * Resets (clears) an application's throttle counters via the DevPortal reset-throttle-policy endpoint, so an
     * application that was just throttled (429) can invoke successfully (200) again without waiting out the window.
     * The endpoint takes the application-owner's username in the body; {@code owner} is the acting actor reference
     * (e.g. {@code admin@tenant1.com}), resolved to a bare username. Publishes the response for assertion.
     *
     * @param appId context key holding the application id
     * @param owner the application owner's username (the acting actor)
     */
    @When("I reset the application throttle policy for {string} owned by {string}")
    public void iResetApplicationThrottlePolicy(String appId, String owner) throws IOException {
        String actualAppId = TestContext.resolve(appId).toString();
        String ownerName = Utils.resolveContextPlaceholders(owner);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        String body = "{\"userName\": \"" + ownerName + "\"}";
        Requests.post(Utils.getResetThrottlePolicyURL(Utils.getBaseUrl(), actualAppId), headers, body,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        HttpResponse response = Requests.post(Utils.getApplicationThrottlingPoliciesURL(Utils.getBaseUrl()), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        HttpResponse response = Requests.post(Utils.getSubscriptionThrottlingPoliciesURL(Utils.getBaseUrl()), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        HttpResponse response = Requests.post(Utils.getAdvancedThrottlingPoliciesURL(Utils.getBaseUrl()), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
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
        HttpResponse response = Requests.post(listUrl, headers, payload, Constants.CONTENT_TYPES.APPLICATION_JSON);
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
        postAdminPolicy(Utils.getSubscriptionThrottlingPoliciesURL(Utils.getBaseUrl()), payload, "subThrottlePolicyName",
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
        postAdminPolicy(Utils.getSubscriptionThrottlingPoliciesURL(Utils.getBaseUrl()), payload, "subThrottlePolicyName",
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
        postAdminPolicy(Utils.getSubscriptionThrottlingPoliciesURL(Utils.getBaseUrl()), payload, "subThrottlePolicyName",
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
        postAdminPolicy(Utils.getSubscriptionThrottlingPoliciesURL(Utils.getBaseUrl()), payload, "subThrottlePolicyName",
                policyName, "subThrottlePolicyId", Constants.CREATED_SUBSCRIPTION_POLICY_IDS);
    }

    /**
     * Sets an API's per-field GraphQL complexity values (PUT /apis/{id}/graphql-policies/complexity). Ports
     * addGraphQLComplexityDetails — the per-field weights the gateway sums to compute a query's complexity.
     */
    @When("I set the GraphQL complexity for API {string} from payload {string}")
    public void iSetGraphqlComplexity(String apiId, String payloadKey) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        String payload = TestContext.resolve(payloadKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.put(Utils.getGraphQLComplexityURL(Utils.getBaseUrl(), actualApiId), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
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
        postAdminPolicy(Utils.getAdvancedThrottlingPoliciesURL(Utils.getBaseUrl()), payload, "advThrottlePolicyName",
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
        postAdminPolicy(Utils.getAdvancedThrottlingPoliciesURL(Utils.getBaseUrl()), payload, "advThrottlePolicyName",
                policyName, "advThrottlePolicyId", Constants.CREATED_ADVANCED_POLICY_IDS);
    }

    /**
     * Builds and POSTs an advanced (API-level) throttling policy carrying ONE conditional group, with a HIGH
     * default limit and a LOW group limit so a request that matches the condition is throttled long before the
     * default would trip — the mechanism the four condition steps below exercise. {@code conditionJson} is the
     * single ThrottleCondition object (ipCondition / headerCondition / queryParameterCondition /
     * jwtClaimsCondition). Ports the policy shape from JWTRequestCountThrottlingTestCase.
     */
    private void postConditionalAdvancedPolicy(String policyBaseName, int defaultLimit, int groupLimit,
                                               String conditionJson) throws IOException {
        String policyName = Utils.resolveContextPlaceholders(Utils.resolvePayloadPlaceholders(policyBaseName));
        // Resolve any {{contextKey}} in the condition value (e.g. a JWT-claim attribute set to a generated
        // application name) — otherwise the literal placeholder is stored and never matches at runtime.
        conditionJson = Utils.resolveContextPlaceholders(conditionJson);
        String payload = String.format(
                "{\"policyName\":\"%s\",\"displayName\":\"%s\",\"description\":\"Advanced with a conditional group\","
                        + "\"type\":\"AdvancedThrottlePolicy\",\"defaultLimit\":{\"type\":\"REQUESTCOUNTLIMIT\","
                        + "\"requestCount\":{\"timeUnit\":\"min\",\"unitTime\":1,\"requestCount\":%d}},"
                        + "\"conditionalGroups\":[{\"description\":\"conditional group\",\"conditions\":[%s],"
                        + "\"limit\":{\"type\":\"REQUESTCOUNTLIMIT\",\"requestCount\":{\"timeUnit\":\"min\","
                        + "\"unitTime\":1,\"requestCount\":%d}}}]}",
                policyName, policyName, defaultLimit, conditionJson, groupLimit);
        postAdminPolicy(Utils.getAdvancedThrottlingPoliciesURL(Utils.getBaseUrl()), payload, "advThrottlePolicyName",
                policyName, "advThrottlePolicyId", Constants.CREATED_ADVANCED_POLICY_IDS);
    }

    @When("I create an advanced throttling policy {string} allowing {int} requests per minute with an IP conditional group of {int} requests per minute for IP {string}")
    public void iCreateAdvancedIpConditionalPolicy(String policyBaseName, int defaultLimit, int groupLimit,
                                                   String ip) throws IOException {
        String condition = String.format("{\"type\":\"IPCONDITION\",\"invertCondition\":false,\"ipCondition\":"
                + "{\"ipConditionType\":\"IPSPECIFIC\",\"specificIP\":\"%s\"}}", ip);
        postConditionalAdvancedPolicy(policyBaseName, defaultLimit, groupLimit, condition);
    }

    @When("I create an advanced throttling policy {string} allowing {int} requests per minute with a header conditional group of {int} requests per minute for header {string} value {string}")
    public void iCreateAdvancedHeaderConditionalPolicy(String policyBaseName, int defaultLimit, int groupLimit,
                                                       String headerName, String headerValue) throws IOException {
        String condition = String.format("{\"type\":\"HEADERCONDITION\",\"invertCondition\":false,"
                + "\"headerCondition\":{\"headerName\":\"%s\",\"headerValue\":\"%s\"}}", headerName, headerValue);
        postConditionalAdvancedPolicy(policyBaseName, defaultLimit, groupLimit, condition);
    }

    @When("I create an advanced throttling policy {string} allowing {int} requests per minute with a query conditional group of {int} requests per minute for query {string} value {string}")
    public void iCreateAdvancedQueryConditionalPolicy(String policyBaseName, int defaultLimit, int groupLimit,
                                                      String paramName, String paramValue) throws IOException {
        String condition = String.format("{\"type\":\"QUERYPARAMETERCONDITION\",\"invertCondition\":false,"
                + "\"queryParameterCondition\":{\"parameterName\":\"%s\",\"parameterValue\":\"%s\"}}",
                paramName, paramValue);
        postConditionalAdvancedPolicy(policyBaseName, defaultLimit, groupLimit, condition);
    }

    @When("I create an advanced throttling policy {string} allowing {int} requests per minute with a JWT claim conditional group of {int} requests per minute for claim {string} value {string}")
    public void iCreateAdvancedJwtClaimConditionalPolicy(String policyBaseName, int defaultLimit, int groupLimit,
                                                         String claimUrl, String attribute) throws IOException {
        String condition = String.format("{\"type\":\"JWTCLAIMSCONDITION\",\"invertCondition\":false,"
                + "\"jwtClaimsCondition\":{\"claimUrl\":\"%s\",\"attribute\":\"%s\"}}", claimUrl, attribute);
        postConditionalAdvancedPolicy(policyBaseName, defaultLimit, groupLimit, condition);
    }

    /** Generic retrieve of a throttling policy by type + id (admin API). Non-asserting. */
    @When("I retrieve the {string} throttling policy with id {string}")
    public void iRetrieveThrottlingPolicyByType(String policyType, String idKey) throws IOException {
        String policyId = TestContext.resolve(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        Requests.get(Utils.getThrottlingPolicyByTypeURL(Utils.getBaseUrl(), policyType, policyId), headers);
    }

    /** Generic delete of a throttling policy by type + id (admin API). Non-asserting (also used for 404 checks). */
    @When("I delete the {string} throttling policy with id {string}")
    public void iDeleteThrottlingPolicyByType(String policyType, String idKey) throws IOException {
        String policyId = TestContext.resolve(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        Requests.delete(Utils.getThrottlingPolicyByTypeURL(Utils.getBaseUrl(), policyType, policyId), headers);
    }

    /** Generic update: retrieve the policy, set a new description, and PUT it back. */
    @When("I update the {string} throttling policy {string} setting its description to {string}")
    public void iUpdateThrottlingPolicyDescription(String policyType, String idKey, String description)
            throws IOException {
        String policyId = TestContext.resolve(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        String url = Utils.getThrottlingPolicyByTypeURL(Utils.getBaseUrl(), policyType, policyId);
        HttpResponse getResp = SimpleHTTPClient.getInstance().doGet(url, headers);
        // Confirm the GET succeeded with a body BEFORE parsing — otherwise new JSONObject(null/"") throws an
        // opaque JSONException/NPE instead of a clear failure.
        Assert.assertTrue(getResp != null && getResp.getResponseCode() >= 200 && getResp.getResponseCode() < 300
                        && getResp.getData() != null && !getResp.getData().isEmpty(),
                "Failed to fetch " + policyType + " throttling policy '" + policyId + "' before updating it: expected a "
                        + "2xx response with a body, got " + (getResp == null ? "no response" : getResp.getResponseCode()
                        + " / body=" + getResp.getData()));
        JSONObject policy = new JSONObject(getResp.getData());
        policy.put("description", description);
        Requests.put(url, headers, policy.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Retrieve all throttling policies of a type (admin API). Non-asserting. */
    @When("I retrieve all {string} throttling policies")
    public void iRetrieveAllThrottlingPolicies(String policyType) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        Requests.get(Utils.getThrottlingPoliciesByTypeURL(Utils.getBaseUrl(), policyType), headers);
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
        HttpResponse response = Requests.post(Utils.getEnvironmentsURL(Utils.getBaseUrl()), headers, env.toString(),
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
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
        Requests.get(Utils.getEnvironmentsURL(Utils.getBaseUrl()), headers);
    }

    /** Retrieve a gateway environment by id (admin API). */
    @When("I retrieve the gateway environment with id {string}")
    public void iRetrieveGatewayEnvironment(String idKey) throws IOException {
        String id = TestContext.resolve(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        Requests.get(Utils.getEnvironmentByIdURL(Utils.getBaseUrl(), id), headers);
    }

    /** Update a gateway environment's description (GET → set → PUT). */
    @When("I update the gateway environment {string} setting its description to {string}")
    public void iUpdateGatewayEnvironment(String idKey, String description) throws IOException {
        String id = TestContext.resolve(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        String url = Utils.getEnvironmentByIdURL(Utils.getBaseUrl(), id);
        HttpResponse getResp = SimpleHTTPClient.getInstance().doGet(url, headers);
        // Confirm the GET succeeded with a body BEFORE parsing — otherwise new JSONObject(null/"") throws an
        // opaque JSONException/NPE instead of a clear failure.
        Assert.assertTrue(getResp != null && getResp.getResponseCode() >= 200 && getResp.getResponseCode() < 300
                        && getResp.getData() != null && !getResp.getData().isEmpty(),
                "Failed to fetch gateway environment '" + id + "' before updating it: expected a 2xx response with a "
                        + "body, got " + (getResp == null ? "no response" : getResp.getResponseCode()
                        + " / body=" + getResp.getData()));
        JSONObject env = new JSONObject(getResp.getData());
        env.put("description", description);
        Requests.put(url, headers, env.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Delete a gateway environment by id (admin API). Non-asserting (also used for 404 / delete-Default 400).
     *  The id may be a context key (e.g. {@code environmentId}) or a literal id such as {@code Default}. */
    @When("I delete the gateway environment with id {string}")
    public void iDeleteGatewayEnvironment(String idKey) throws IOException {
        String id = TestContext.contains(idKey) ? TestContext.get(idKey).toString() : idKey;
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        Requests.delete(Utils.getEnvironmentByIdURL(Utils.getBaseUrl(), id), headers);
    }

    /** Retrieve the gateway instances of an environment (admin API). The id may be a context key
     *  (e.g. {@code environmentId}) or a literal environment id such as {@code Default}. */
    @When("I retrieve the gateway instances of environment {string}")
    public void iRetrieveGatewayInstances(String idKey) throws IOException {
        String id = TestContext.contains(idKey) ? TestContext.get(idKey).toString() : idKey;
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        Requests.get(Utils.getEnvironmentGatewaysURL(Utils.getBaseUrl(), id), headers);
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
        HttpResponse response = Requests.post(Utils.getEnvironmentsURL(Utils.getBaseUrl()), headers, env.toString(),
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
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
        Requests.get(Utils.getAdminApplicationsByOwnerURL(Utils.getBaseUrl(), owner), headers);
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
        Requests.get(Utils.getAdminApplicationsByNameURL(Utils.getBaseUrl(), actualName), headers);
    }

    /** Update a gateway environment to a single vhost host (removing any others). Non-asserting. */
    @When("I update the gateway environment {string} to only vhost host {string}")
    public void iUpdateGatewayEnvironmentToSingleVhost(String idKey, String host) throws IOException {
        String id = TestContext.resolve(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        String url = Utils.getEnvironmentByIdURL(Utils.getBaseUrl(), id);
        HttpResponse getResp = SimpleHTTPClient.getInstance().doGet(url, headers);
        // Confirm the GET succeeded with a body BEFORE parsing — otherwise new JSONObject(null/"") throws an
        // opaque JSONException/NPE instead of a clear failure.
        Assert.assertTrue(getResp != null && getResp.getResponseCode() >= 200 && getResp.getResponseCode() < 300
                        && getResp.getData() != null && !getResp.getData().isEmpty(),
                "Failed to fetch gateway environment '" + id + "' before updating its vhost: expected a 2xx response "
                        + "with a body, got " + (getResp == null ? "no response" : getResp.getResponseCode()
                        + " / body=" + getResp.getData()));
        JSONObject env = new JSONObject(getResp.getData());
        env.put("vhosts", new JSONArray().put(regularVhost(host)));
        Requests.put(url, headers, env.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        HttpResponse response = Requests.post(Utils.getApplicationThrottlingPoliciesURL(Utils.getBaseUrl()), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        HttpResponse response = Requests.post(Utils.getCustomThrottlingPoliciesURL(Utils.getBaseUrl()), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        String policyId = TestContext.resolve(policyIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());

        Requests.get(Utils.getApplicationThrottlingPolicyByIdURL(Utils.getBaseUrl(), policyId), headers);
    }

    /** Deletes an application throttling policy by id (admin API), storing the raw response for assertions. */
    @When("I delete the application throttling policy {string}")
    public void iDeleteApplicationThrottlingPolicy(String policyIdKey) throws IOException {

        String policyId = TestContext.resolve(policyIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());

        Requests.delete(Utils.getApplicationThrottlingPolicyByIdURL(Utils.getBaseUrl(), policyId), headers);
    }

    /** Retrieves a custom (Siddhi) throttling rule by id (admin API), storing the raw response for assertions. */
    @When("I retrieve the custom throttling policy {string}")
    public void iRetrieveCustomThrottlingPolicy(String policyIdKey) throws IOException {

        String policyId = TestContext.resolve(policyIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());

        Requests.get(Utils.getCustomThrottlingPolicyByIdURL(Utils.getBaseUrl(), policyId), headers);
    }

    /** Deletes a custom (Siddhi) throttling rule by id (admin API), storing the raw response for assertions. */
    @When("I delete the custom throttling policy {string}")
    public void iDeleteCustomThrottlingPolicy(String policyIdKey) throws IOException {

        String policyId = TestContext.resolve(policyIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());

        Requests.delete(Utils.getCustomThrottlingPolicyByIdURL(Utils.getBaseUrl(), policyId), headers);
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
        String policyName = TestContext.resolve(policyNameKey).toString();
        String jsonPayload = String.format(
                "{\"name\":\"%s\",\"throttlingPolicy\":\"%s\",\"description\":\"Throttle-enforcement test application\"}",
                resolvedName, policyName);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = Requests.post(Utils.getApplicationCreateURL(Utils.getBaseUrl()), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        String jsonPayload = TestContext.resolve(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        Requests.post(Utils.getApplicationCreateURL(Utils.getBaseUrl()), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Deletes an application by its ID.
     *
     * @param appId Context key containing the application ID to delete
     */
    @When("I delete the application with id {string}")
    public void iDeleteApplication(String appId) throws IOException{

        String actualAppId = TestContext.resolve(appId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        Requests.delete(Utils.getApplicationEndpointURL(Utils.getBaseUrl(), actualAppId), headers);
    }

    /**
     * Retrieves the details of a specific application by its ID.
     *
     * @param appId Context key containing the application ID to retrieve
     */
    @When("I retrieve the application with id {string}")
    public void iShouldBeAbleToRetrieveApplication(String appId) throws Exception {

        String actualAppId = TestContext.resolve(appId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        Requests.get(Utils.getApplicationEndpointURL(Utils.getBaseUrl(), actualAppId), headers);
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

        HttpResponse response = Requests.get(Utils.getApplicationSearchURL(Utils.getBaseUrl(), applicationName), headers);

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

        String actualAppId = TestContext.resolve(appId).toString();
        // Resolve any {{contextKey}} placeholders (e.g. a captured application name so the PUT keeps its
        // required name). No-op when the payload has none.
        String jsonPayload = Utils.resolveContextPlaceholders(TestContext.resolve(updatePayload).toString());

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.devportalToken());

        Requests.put(
                Utils.getApplicationEndpointURL(Utils.getBaseUrl(), actualAppId), headers, jsonPayload,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        String actualApiId = TestContext.resolve(apiId).toString();
        String actualAppId = TestContext.resolve(appId).toString();

        // Add application id and API id to the payload
        String jsonPayload = TestContext.resolve(payload).toString();
        jsonPayload = jsonPayload.replace("{{applicationId}}", actualAppId);
        jsonPayload = jsonPayload.replace("{{apiId}}", actualApiId);
        // Resolve any remaining {{contextKey}} placeholders (e.g. a custom subscription throttling policy name
        // captured into context). The applicationId/apiId markers are already substituted above, so only genuine
        // context keys remain — resolveContextPlaceholders is a no-op when the payload has none.
        jsonPayload = Utils.resolveContextPlaceholders(jsonPayload);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = Requests.post(Utils.getCreateSubscriptionURL(Utils.getBaseUrl()),
                headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        String actualApiId = TestContext.resolve(apiId).toString();
        String actualAppId = TestContext.resolve(appId).toString();

        String jsonPayload = TestContext.resolve(payload).toString();
        jsonPayload = jsonPayload.replace("{{applicationId}}", actualAppId);
        jsonPayload = jsonPayload.replace("{{apiId}}", actualApiId);
        // Resolve any remaining {{contextKey}} placeholders, mirroring the positive iSubscribeToApi step. This
        // also makes a typo'd placeholder fail FAST (resolveContextPlaceholders throws on an unknown key) rather
        // than being sent to the server verbatim — an unresolved id yields a misleading 500 that masquerades as
        // a genuine rejection (it once produced a false "org-policy denial returns 500" finding).
        jsonPayload = Utils.resolveContextPlaceholders(jsonPayload);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        Requests.post(Utils.getCreateSubscriptionURL(Utils.getBaseUrl()),
                headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Retrieves a subscription between a specific API and application.
     *
     * @param apiId Context key containing the API ID
     * @param appId Context key containing the application ID
     */
    @Then("I retrieve the subscription for Api {string} by Application {string}")
    public void iShouldBeAbleToRetrieveSubscription(String apiId, String appId) throws Exception {

        String actualApiId = TestContext.resolve(apiId).toString();
        String actualAppId = TestContext.resolve(appId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = Requests.get(Utils.getAllSubscriptionsURL(Utils.getBaseUrl(), actualApiId, actualAppId, null, null,
                        null), headers);

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

        String actualAppId = TestContext.resolve(appId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = Requests.get(Utils.getApplicationAllKeys(Utils.getBaseUrl(), actualAppId), headers);

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

        String actualAppId = TestContext.resolve(appId).toString();
        String keyMappingId = TestContext.resolve("keyMappingId").toString();
        String jsonPayload =TestContext.resolve("updateKeysPayload").toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        Requests.put(Utils.getUpdateKey(Utils.getBaseUrl(), actualAppId, keyMappingId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Deletes the generated keys (OAuth2 credentials) for an application.
     *
     * @param appId Context key containing the application ID
     */
    @When("I delete the generated keys for {string}")
    public void iDeleteTheGeneratedKeysFor(String appId) throws IOException {

        String actualAppId = TestContext.resolve(appId).toString();
        String keyMappingId = TestContext.resolve("keyMappingId").toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        Requests.delete(Utils.getUpdateKey(Utils.getBaseUrl(), actualAppId, keyMappingId), headers);
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

        String actualAppId = TestContext.resolve(appId).toString();
        // Resolve any {{contextKey}} placeholders (e.g. a captured key-manager name for a specific-KM key-gen).
        String jsonPayload = Utils.resolveContextPlaceholders(TestContext.resolve(payload).toString());

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = Requests.post(Utils.getGenerateApplicationKeysURL(Utils.getBaseUrl(), actualAppId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        HttpResponse response = Requests.post(
                Utils.getDCREndpointURL(Utils.getBaseUrl()), headers, json.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 200,
                "BYO OAuth client registration (DCR) failed: " + response.getData());
        Object clientId = Utils.extractValueFromPayload(response.getData(), "clientId");
        TestContext.set(idKey + "ClientId", clientId);
        TestContext.set(idKey + "ClientSecret", Utils.extractValueFromPayload(response.getData(), "clientSecret"));
        // A DCR client is a standalone OAuth service provider not removed by app deletion, and the DCR REST
        // endpoint has no delete — so register its consumer key for teardown, which deregisters it via the
        // OAuthAdminService SOAP admin service (as its owner) rather than leaking it onto the shared server.
        ResourceCleanup.register(ResourceCleanup.CREATED_DCR_CLIENT_IDS, clientId);
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

        String actualAppId = TestContext.resolve(appId).toString();
        String consumerKey = TestContext.resolve(idKey + "ClientId").toString();
        String consumerSecret = TestContext.resolve(idKey + "ClientSecret").toString();

        JSONObject json = new JSONObject();
        json.put("consumerKey", consumerKey);
        json.put("consumerSecret", consumerSecret);
        json.put("keyType", "PRODUCTION");
        json.put("keyManager", keyManager);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        Requests.post(
                Utils.getMapKeysURL(Utils.getBaseUrl(), actualAppId), headers, json.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        String actualAppId = TestContext.resolve(appId).toString();
        String keyMappingId = TestContext.resolve(keyMappingIdKey).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        Requests.post(
                Utils.getCleanupRegistrationURL(Utils.getBaseUrl(), actualAppId, keyMappingId), headers, "",
                Constants.CONTENT_TYPES.APPLICATION_JSON);
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
        String payload = decodeReflectedBackendJwtPayload();
        Assert.assertTrue(payload.contains(attributeName),
                "Decoded backend JWT does not contain attribute name '" + attributeName + "': " + payload);
        Assert.assertTrue(payload.contains(attributeValue),
                "Decoded backend JWT does not contain attribute value '" + attributeValue + "': " + payload);
    }

    /**
     * Asserts the reflected backend JWT's applicationAttributes claim carries the given attribute name mapped to
     * an EMPTY value (i.e. the exact JSON fragment {@code "<name>":""}). Proves
     * enable_empty_values_in_application_attributes = true surfaces an optional attribute left empty (a
     * plain "contains value" check with value "" would be trivially true, so this pins the empty-value form).
     * Ports the applicationAttributes empty-value assertion of JWTTestCase. The claim serialises the attributes
     * map as a JSON string, so the fragment appears with escaped quotes inside the decoded payload.
     *
     * @param attributeName the application attribute name (e.g. "Optional attribute")
     */
    @Then("The reflected backend JWT applicationAttributes claim should contain {string} with an empty value")
    public void theReflectedBackendJwtAttributeIsEmpty(String attributeName) {
        String payload = decodeReflectedBackendJwtPayload();
        // The applicationAttributes claim is a nested JSON object (e.g. {"External Reference Id":"c1237890",
        //   "Optional attribute":""}). Parse it and assert the attribute key is present with an empty-string
        //   value — robust to key order and to whether the gateway renders the claim as an object or a
        //   string-encoded object.
        JSONObject jwt = new JSONObject(payload);
        Object attrsClaim = jwt.opt("http://wso2.org/claims/applicationAttributes");
        Assert.assertNotNull(attrsClaim,
                "Decoded backend JWT has no applicationAttributes claim: " + payload);
        JSONObject attrs = (attrsClaim instanceof JSONObject)
                ? (JSONObject) attrsClaim
                : new JSONObject(attrsClaim.toString());
        Assert.assertTrue(attrs.has(attributeName) && attrs.optString(attributeName).isEmpty(),
                "Decoded backend JWT applicationAttributes claim does not carry '" + attributeName
                        + "' with an empty value: " + payload);
    }

    /**
     * Asserts the gateway-injected backend JWT (X-JWT-Assertion) carries a claim name and value (substring match,
     * so the short dialect suffix — e.g. "applicationname", "subscriber", "givenname" — suffices for the
     * fully-qualified claim URIs). Shares the decode helper with the application-attribute assertion above.
     * {@code {{...}}} placeholders in the expected name/value are resolved first.
     */
    @Then("The reflected backend JWT should contain claim {string} with value {string}")
    public void theReflectedBackendJwtShouldContainClaim(String claimName, String claimValue) {
        String payload = decodeReflectedBackendJwtPayload();
        String name = Utils.resolveContextPlaceholders(claimName);
        String value = Utils.resolveContextPlaceholders(claimValue);
        Assert.assertTrue(payload.contains(name),
                "Decoded backend JWT does not contain claim '" + name + "': " + payload);
        Assert.assertTrue(payload.contains(value),
                "Decoded backend JWT claim '" + name + "' does not carry value '" + value + "': " + payload);
    }

    /**
     * Reads the reflected invocation response (the /reflect-headers backend echoes the request headers as JSON),
     * extracts the gateway-injected {@code X-JWT-Assertion} header and base64-decodes its payload segment
     * (URL-safe first, then standard, since the gateway config uses {@code encoding = "base64"}). Each step is
     * guarded so a missing header / malformed assertion fails with a clear message.
     */
    private String decodeReflectedBackendJwtPayload() {
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
        try {
            return new String(Base64.getUrlDecoder().decode(segments[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return new String(Base64.getDecoder().decode(segments[1]), StandardCharsets.UTF_8);
        }
    }

    /**
     * Regenerates (rotates) the consumer secret of an application's keys for a key type. Ports
     * ApplicationConsumerSecretRegenerateTestCase — the response carries a NEW consumer secret. Non-asserting.
     */
    @When("I regenerate the consumer secret for application {string} with key mapping {string}")
    public void iRegenerateConsumerSecret(String appId, String keyMappingIdKey) throws IOException {

        String actualAppId = TestContext.resolve(appId).toString();
        String keyMappingId = TestContext.resolve(keyMappingIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        Requests.post(
                Utils.getRegenerateConsumerSecretURL(Utils.getBaseUrl(), actualAppId, keyMappingId), headers, "",
                Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        String actualAppId = TestContext.resolve(appId).toString();
        String keyMappingId = TestContext.resolve("keyMappingId").toString();
        String consumerSecret = TestContext.resolve("consumerSecret").toString();

        // Add consumer secret to the payload
        String jsonPayload = TestContext.resolve(payload).toString();
        jsonPayload = jsonPayload.replace("{{appConsumerSecret}}", consumerSecret);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = Requests.post(Utils.getGenerateApplicationTokenURL(Utils.getBaseUrl(), actualAppId, keyMappingId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        User currentUser = Identity.actingActor();

        StringBuilder body = new StringBuilder("grant_type=password")
                .append("&username=").append(Utils.urlEncode(currentUser.getUserName()))
                .append("&password=").append(Utils.urlEncode(currentUser.getPassword()));
        if (scope != null && !scope.isEmpty()) {
            body.append("&scope=").append(Utils.urlEncode(scope));
        }

        HttpResponse response = Requests.post(Utils.getAPIMTokenEndpointURL(Utils.getBaseUrl()),
                clientCredentialsHeader(), body.toString(), Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
        captureTokens(response);
    }

    /**
     * Requests an OAuth2 access token directly from the EXTERNAL key manager (WSO2 IS) using the client
     * credentials grant, authenticated with the consumerKey/consumerSecret generated for the application against
     * that key manager. When keys are generated against an external KM, APIM does DCR into IS and returns IS's
     * OAuth client credentials, so the token must be issued by IS's own token endpoint (not APIM's). IS is
     * reached at the host-mapped {@code isBaseUrl} published to the block's shared scope by
     * {@code BlockLifecycleListener}. Captures the issued token as {@code generatedAccessToken} (and the raw
     * response as {@code httpResponse}) via the shared {@link #captureTokens} path, so downstream gateway-invoke
     * and status-assertion steps work unchanged.
     */
    @When("I request an OAuth access token from the external key manager using client credentials grant")
    public void iRequestTokenFromExternalKeyManager() throws Exception {

        HttpResponse response = Requests.post(IntegrationActors.tokenEndpoint(IntegrationActors.IS), clientCredentialsHeader(),
                "grant_type=client_credentials", Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
        captureTokens(response);
    }

    /**
     * As the password-grant step, but requests a specific scope in addition to {@code openid} (the given scope
     * string may contain {@code {{contextKey}}} placeholders, e.g. a generated API scope name). IS grants the
     * requested scope only if the user holds a role bound to it, so this drives the role-based-authorization
     * check: a user WITH the mapped role gets a token carrying the scope; a user WITHOUT it gets a token that
     * lacks it. Captures the tokens and the raw response.
     */
    @When("I request an OAuth access token from the external key manager using password grant as {string} with password {string} requesting scope {string}")
    public void iRequestPasswordTokenWithScopeFromExternalKm(String username, String password, String scope)
            throws Exception {

        String resolvedUser = Utils.resolveContextPlaceholders(username);
        String resolvedScope = Utils.resolveContextPlaceholders(scope);
        // The scope is sent VERBATIM — features state the full scope they need (e.g. "openid", or
        // "openid <customScope>"); nothing is implicitly prefixed here.
        String body = "grant_type=password&username=" + Utils.urlEncode(resolvedUser) + "&password=" + Utils.urlEncode(password)
                + "&scope=" + Utils.urlEncode(resolvedScope);
        HttpResponse response = Requests.post(IntegrationActors.tokenEndpoint(IntegrationActors.IS), clientCredentialsHeader(), body,
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
        captureTokens(response);
    }

    /**
     * Creates a user in the EXTERNAL key manager (IS) via SCIM2 and, when {@code isRoleKey} resolves to a
     * non-empty role name, assigns that IS role to the user (SCIM2 v2 Roles PATCH add-member). Authenticated as
     * the IS super admin. Stores the created user id under {@code <username>UserId}. Used by the role-based
     * authorization flow to mint a user that does (or does not) hold the scope-bound role.
     *
     * @param username  the IS user name to create (used verbatim; the ephemeral per-block IS makes it unique enough)
     * @param password  the user's password (also used for the later password grant)
     * @param isRoleKey context key holding the IS role to assign, or a literal empty string for no role
     */
    @When("I create an IS user {string} with password {string} assigned the IS role stored as {string}")
    public void iCreateIsUserWithRole(String usernameBase, String password, String isRoleKey) throws Exception {

        // The external IS is a shared, JVM-lifetime singleton, so IS usernames must be unique BY CONSTRUCTION
        // (SCIM2 create 409s on a duplicate). Derive a unique name from the base and store it under the base
        // key so later scenarios reference it as {{<base>}} (e.g. {{is7roleuser}}).
        String username = Names.unique(usernameBase);
        TestContext.set(usernameBase, username);

        String base = IntegrationActors.baseUrl(IntegrationActors.IS);
        Map<String, String> headers = IntegrationActors.authHeaders(IntegrationActors.IS);

        // Create the user (SCIM2 Users).
        String userPayload = new JSONObject()
                .put("schemas", new JSONArray().put("urn:ietf:params:scim:schemas:core:2.0:User"))
                .put("userName", username)
                .put("password", password)
                .put("name", new JSONObject().put("givenName", username).put("familyName", "is7test"))
                .toString();
        HttpResponse userResp = Requests.post(base + "scim2/Users", headers, userPayload,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertTrue(userResp != null && userResp.getResponseCode() == 201 && userResp.getData() != null
                        && !userResp.getData().isBlank(),
                "SCIM2 user create failed for '" + username + "': got="
                        + (userResp == null ? "null" : userResp.getResponseCode() + "/" + userResp.getData()));
        String userId = new JSONObject(userResp.getData()).getString("id");
        TestContext.set(username + "UserId", userId);
        // Register for the IS-side teardown sweep (deleted as the IS integration actor — see ISResourceCleanup).
        ISResourceCleanup.registerUser(userId);

        // Empty key => create the user with NO role (the "without the mapped role" actor).
        if (isRoleKey == null || isRoleKey.trim().isEmpty()) {
            return;
        }
        String isRole = TestContext.resolve(isRoleKey).toString();
        if (isRole.isEmpty()) {
            return;
        }
        // Find the role id (SCIM2 v2 Roles, via the shared asserted-query primitive), then add the user as a member.
        HttpResponse rolesResp = queryIs7Role(isRole);
        JSONObject rolesBody = new JSONObject(rolesResp.getData());
        Assert.assertTrue(rolesBody.optInt("totalResults", 0) >= 1,
                "IS role '" + isRole + "' not found to assign to user '" + username + "': " + rolesResp.getData());
        String roleId = rolesBody.getJSONArray("Resources").getJSONObject(0).getString("id");

        String patch = new JSONObject()
                .put("schemas", new JSONArray().put("urn:ietf:params:scim:api:messages:2.0:PatchOp"))
                .put("Operations", new JSONArray().put(new JSONObject()
                        .put("op", "add")
                        .put("path", "users")
                        .put("value", new JSONArray().put(new JSONObject().put("value", userId)))))
                .toString();
        HttpResponse patchResp = Requests.patch(base + "scim2/v2/Roles/" + roleId, headers, patch,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertTrue(patchResp != null && patchResp.getResponseCode() >= 200 && patchResp.getResponseCode() < 300,
                "SCIM2 role member add failed for user '" + username + "' role '" + isRole + "': got="
                        + (patchResp == null ? "null" : patchResp.getResponseCode() + "/" + patchResp.getData()));
    }

    /**
     * Exchanges the stored {@code refreshToken} at the EXTERNAL key manager (IS) token endpoint for a fresh
     * access token (refresh_token grant), authenticated with the application's IS-issued client credentials.
     * Captures the new tokens and the raw response.
     */
    @When("I request a new OAuth access token from the external key manager using the stored refresh token")
    public void iRefreshTokenAtExternalKm() throws Exception {

        String refreshToken = TestContext.resolve("refreshToken").toString();
        String body = "grant_type=refresh_token&refresh_token=" + Utils.urlEncode(refreshToken);
        HttpResponse response = Requests.post(IntegrationActors.tokenEndpoint(IntegrationActors.IS), clientCredentialsHeader(), body,
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
        captureTokens(response);
    }

    /** Redirect URI registered (via keygen callbackUrl -> IS DCR) for the authorization_code / device flows. */
    private static final String IS7_AUTHZ_REDIRECT_URI = "https://localhost/callback";
    /** Fixed RFC 7636 sample PKCE verifier (43 chars); its S256 challenge is derived at request time. */
    private static final String IS7_PKCE_CODE_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

    /**
     * Obtains an access token from the EXTERNAL key manager (IS) via the authorization_code grant, headlessly.
     * IS skips consent for DCR-registered clients, so the flow is: GET /oauth2/authorize -> 302 to login (carries
     * sessionDataKey) -> POST /commonauth with credentials -> 302 back to /oauth2/authorize (new sessionDataKey)
     * -> 302 to redirect_uri?code=... -> exchange the code at /oauth2/token. Every request targets the host-mapped
     * isBaseUrl and only query params are read from the 302 Location headers, so IS's internal hostname in those
     * Locations is never navigated. A shared cookie jar carries the session; auto-redirect is disabled so each
     * Location is captured. When {@code codeVerifier} is non-null, PKCE (S256) is used. Captures generatedAccessToken
     * and publishes the token response as httpResponse.
     */
    private void authorizationCodeTokenFromExternalKm(String username, String password, String codeVerifier)
            throws Exception {

        String base = IntegrationActors.baseUrl(IntegrationActors.IS);
        String key = TestContext.resolve("consumerKey").toString();
        java.net.http.HttpClient http = trustAllHttpClientWithCookies();

        // Step 1: /oauth2/authorize -> 302 to login; carry sessionDataKey.
        StringBuilder authz = new StringBuilder(base).append("oauth2/authorize?response_type=code&client_id=")
                .append(Utils.urlEncode(key)).append("&redirect_uri=").append(Utils.urlEncode(IS7_AUTHZ_REDIRECT_URI))
                .append("&scope=").append(Utils.urlEncode("openid"));
        if (codeVerifier != null) {
            authz.append("&code_challenge=").append(Utils.urlEncode(pkceS256Challenge(codeVerifier)))
                    .append("&code_challenge_method=S256");
        }
        String sdk = Utils.queryParam(redirectLocation(http, "GET", authz.toString(), null), "sessionDataKey");
        Assert.assertNotNull(sdk, "No sessionDataKey in the authorize redirect for the authorization_code flow");

        // Step 2: authenticate at /commonauth -> 302 back to /oauth2/authorize with a fresh sessionDataKey.
        String loginForm = "username=" + Utils.urlEncode(username) + "&password=" + Utils.urlEncode(password)
                + "&sessionDataKey=" + Utils.urlEncode(sdk);
        String sdk2 = Utils.queryParam(redirectLocation(http, "POST", base + "commonauth", loginForm), "sessionDataKey");
        Assert.assertNotNull(sdk2, "Login did not redirect back to /oauth2/authorize (bad credentials?)");

        // Step 3: resume /oauth2/authorize -> 302 to redirect_uri?code=...
        String code = Utils.queryParam(
                redirectLocation(http, "GET", base + "oauth2/authorize?sessionDataKey=" + Utils.urlEncode(sdk2), null),
                "code");
        Assert.assertNotNull(code, "No authorization code in the final redirect of the authorization_code flow");

        // Step 4: exchange the code at the IS token endpoint (client-authed) for an access token.
        String tokenForm = "grant_type=authorization_code&code=" + Utils.urlEncode(code)
                + "&redirect_uri=" + Utils.urlEncode(IS7_AUTHZ_REDIRECT_URI);
        if (codeVerifier != null) {
            tokenForm += "&code_verifier=" + Utils.urlEncode(codeVerifier);
        }
        HttpResponse response = Requests.post(IntegrationActors.tokenEndpoint(IntegrationActors.IS), clientCredentialsHeader(), tokenForm,
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
        captureTokens(response);
    }

    /** Sends one request WITHOUT following redirects and returns its Location header (asserts a redirect). */
    private String redirectLocation(java.net.http.HttpClient http, String method, String url, String formBody)
            throws Exception {
        java.net.http.HttpRequest.Builder b = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url));
        if ("POST".equals(method)) {
            b.header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(formBody == null ? "" : formBody));
        } else {
            b.GET();
        }
        java.net.http.HttpResponse<String> resp =
                http.send(b.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        java.util.Optional<String> loc = resp.headers().firstValue("Location");
        Assert.assertTrue(loc.isPresent(), "Expected a redirect with a Location header from " + url
                + " but got HTTP " + resp.statusCode() + " / body=" + resp.body());
        return loc.get();
    }

    /** Builds an HttpClient that trusts IS's self-signed cert, keeps a cookie jar, and never auto-redirects. */
    private java.net.http.HttpClient trustAllHttpClientWithCookies() throws Exception {
        javax.net.ssl.TrustManager[] trustAll = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) { }
                    public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) { }
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }
                }
        };
        javax.net.ssl.SSLContext ssl = javax.net.ssl.SSLContext.getInstance("TLS");
        ssl.init(null, trustAll, new java.security.SecureRandom());
        return java.net.http.HttpClient.newBuilder()
                .sslContext(ssl)
                .cookieHandler(new java.net.CookieManager())
                .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
                .build();
    }

    /** Computes the PKCE S256 code_challenge (base64url, no padding) for a verifier. */
    private static String pkceS256Challenge(String verifier) throws Exception {
        byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        return JwtTestUtils.base64Url(digest);
    }

    /**
     * Obtains an access token from the external KM via the authorization_code grant (headless login, no consent),
     * then captures it. See {@link #authorizationCodeTokenFromExternalKm}.
     */
    @When("I request an OAuth access token from the external key manager using authorization code grant as {string} with password {string}")
    public void iRequestAuthzCodeTokenFromExternalKm(String username, String password) throws Exception {
        authorizationCodeTokenFromExternalKm(username, password, null);
    }

    /** As the authorization_code step, but with PKCE (S256). */
    @When("I request an OAuth access token from the external key manager using authorization code grant with PKCE as {string} with password {string}")
    public void iRequestAuthzCodePkceTokenFromExternalKm(String username, String password) throws Exception {
        authorizationCodeTokenFromExternalKm(username, password, IS7_PKCE_CODE_VERIFIER);
    }

    /**
     * Obtains an access token from the EXTERNAL key manager (IS) via the device_code grant (RFC 8628), headlessly.
     * Flow: POST /oauth2/device_authorize (client-authed) for a device_code + user_code; POST /oauth2/device with
     * the user_code -> 302 to login (carries sessionDataKey); authenticate at /commonauth; resume /oauth2/authorize
     * to commit the device approval (IS skips consent for the DCR client); then exchange the device_code at the
     * token endpoint. The resume step commits approval synchronously, so the token poll succeeds without waiting.
     * Captures generatedAccessToken and publishes the token response as httpResponse.
     */
    @When("I request an OAuth access token from the external key manager using device code grant as {string} with password {string}")
    public void iRequestDeviceCodeTokenFromExternalKm(String username, String password) throws Exception {

        String base = IntegrationActors.baseUrl(IntegrationActors.IS);
        java.net.http.HttpClient http = trustAllHttpClientWithCookies();

        // 1. device_authorize -> device_code + user_code.
        HttpResponse da = Requests.post(base + "oauth2/device_authorize", clientCredentialsHeader(),
                "response_type=device&scope=" + Utils.urlEncode("openid"),
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
        Assert.assertTrue(da != null && da.getResponseCode() == 200 && da.getData() != null && !da.getData().isBlank(),
                "device_authorize failed: got=" + (da == null ? "null" : da.getResponseCode() + "/" + da.getData()));
        JSONObject daj = new JSONObject(da.getData());
        String deviceCode = daj.getString("device_code");
        String userCode = daj.getString("user_code");

        // 2. submit the user_code -> 302 to login (carries sessionDataKey).
        String sdk = Utils.queryParam(
                redirectLocation(http, "POST", base + "oauth2/device", "user_code=" + Utils.urlEncode(userCode)),
                "sessionDataKey");
        Assert.assertNotNull(sdk, "No sessionDataKey after submitting the device user_code");

        // 3. authenticate -> 302 back to /oauth2/authorize with a fresh sessionDataKey.
        String loginForm = "username=" + Utils.urlEncode(username) + "&password=" + Utils.urlEncode(password)
                + "&sessionDataKey=" + Utils.urlEncode(sdk);
        String sdk2 = Utils.queryParam(redirectLocation(http, "POST", base + "commonauth", loginForm), "sessionDataKey");
        Assert.assertNotNull(sdk2, "Device login did not redirect back to /oauth2/authorize (bad credentials?)");

        // 4. resume /oauth2/authorize to commit the device approval.
        redirectLocation(http, "GET", base + "oauth2/authorize?sessionDataKey=" + Utils.urlEncode(sdk2), null);

        // 5. exchange the device_code for a token (approval already committed -> no polling wait needed).
        HttpResponse response = Requests.post(IntegrationActors.tokenEndpoint(IntegrationActors.IS), clientCredentialsHeader(),
                "grant_type=" + Utils.urlEncode("urn:ietf:params:oauth:grant-type:device_code")
                        + "&device_code=" + Utils.urlEncode(deviceCode),
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
        captureTokens(response);
    }

    /**
     * Attempts a token request at the EXTERNAL key manager (IS) token endpoint with a grant the application's
     * client is not authorized for (client-authed). Non-asserting; the feature pins the rejection status. Used
     * for the disallowed-grant negative.
     */
    @When("I attempt an OAuth token from the external key manager using the unsupported grant {string}")
    public void iAttemptUnsupportedGrantAtExternalKm(String grant) throws Exception {

        Requests.post(IntegrationActors.tokenEndpoint(IntegrationActors.IS), clientCredentialsHeader(), "grant_type=" + Utils.urlEncode(grant),
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
    }

    /**
     * Token-endpoint URL IS advertises inside the docker network. The KM payload registers this value as the
     * issuer/tokenEndpoint, and IS's jwt-bearer grant handler validates the assertion's {@code aud} against the
     * trusted IdP's token-endpoint alias - so the IdP alias and the assertion audience must both be exactly this.
     */
    private static final String IS7_ADVERTISED_TOKEN_ENDPOINT = "https://wso2is:9443/oauth2/token";
    /** Committed test-IdP signing key/cert for the jwt-bearer and saml2-bearer grants (RSA 2048; cert valid until 2036). */
    private static final String IS7_TRUSTED_IDP_KEY_RESOURCE = "artifacts/certs/is7trustedidp/idp-key.pem";
    private static final String IS7_TRUSTED_IDP_CERT_RESOURCE = "artifacts/certs/is7trustedidp/idp-cert.pem";

    /**
     * Registers a trusted identity provider in the EXTERNAL key manager (IS) for the RFC 7523 jwt-bearer grant,
     * via IS's IdP REST API ({@code /api/server/v1/identity-providers}, super-admin basic auth). IS 7.x does NOT
     * ship the legacy SOAP admin services (IdentityProviderMgtService answers with an Axis2 "service cannot be
     * found for the EPR" fault), so REST is the only registration path. The IdP carries the committed test
     * certificate (IS validates the assertion signature against it) and a token-endpoint alias (IS validates the
     * assertion audience against it); {@code isEnabled} must be omitted - the create schema rejects it with 400
     * and IdPs are created enabled anyway. The generated unique IdP name doubles as the trusted issuer and is
     * stored under {@code idpNameKey} for the token step. The IdP dies with the per-block IS container, so no
     * cleanup registration is needed (same lifecycle as the SCIM-created IS users).
     */
    @When("I register a JWT bearer identity provider in the external key manager storing its name as {string}")
    public void iRegisterJwtBearerIdpAtExternalKm(String idpNameKey) throws Exception {

        String idpName = Names.unique("jwtIdp");
        // The IdP REST API expects each certificate as a base64-encoded PEM blob.
        String certPem = Utils.readClasspathResource(IS7_TRUSTED_IDP_CERT_RESOURCE);
        String certB64 = Base64.getEncoder().encodeToString(certPem.getBytes(StandardCharsets.UTF_8));
        String payload = new JSONObject()
                .put("name", idpName)
                .put("alias", IS7_ADVERTISED_TOKEN_ENDPOINT)
                .put("certificate", new JSONObject()
                        .put("certificates", new JSONArray().put(certB64)))
                .toString();
        Map<String, String> headers = IntegrationActors.authHeaders(IntegrationActors.IS);
        HttpResponse resp = Requests.post(IntegrationActors.baseUrl(IntegrationActors.IS) + "api/server/v1/identity-providers", headers,
                payload, Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertTrue(resp != null && resp.getResponseCode() == 201,
                "IS IdP create failed for jwt-bearer trusted issuer '" + idpName + "': got="
                        + (resp == null ? "null" : resp.getResponseCode() + "/" + resp.getData()));
        TestContext.set(idpNameKey, idpName);
    }

    /**
     * Requests an access token from the EXTERNAL key manager (IS) via the RFC 7523 jwt-bearer grant: builds an
     * RS256-signed JWT assertion ({@code iss} = the trusted IdP name stored under {@code idpNameKey}, {@code aud}
     * = the IdP's token-endpoint alias, {@code sub} = the IS super admin) with the committed test-IdP private
     * key, and exchanges it at the token endpoint authenticated with the application's IS-issued client
     * credentials. IS resolves the trusted IdP by the issuer name, checks the audience against the IdP alias and
     * the signature against the IdP certificate. Captures the issued token via the shared captureTokens path.
     */
    @When("I request an OAuth access token from the external key manager using JWT bearer grant with issuer stored as {string}")
    public void iRequestJwtBearerTokenFromExternalKm(String idpNameKey) throws Exception {

        String issuer = TestContext.resolve(idpNameKey).toString();
        long now = System.currentTimeMillis() / 1000;
        String header = JwtTestUtils.base64Url(new JSONObject().put("alg", "RS256").put("typ", "JWT").toString());
        String claims = JwtTestUtils.base64Url(new JSONObject()
                .put("iss", issuer)
                .put("sub", Constants.SUPER_TENANT_ADMIN_USERNAME)
                .put("aud", IS7_ADVERTISED_TOKEN_ENDPOINT)
                .put("iat", now)
                .put("exp", now + 300)
                .put("jti", Names.unique("jwtAssertion"))
                .toString());
        String signingInput = header + "." + claims;

        PrivateKey privateKey = JwtTestUtils.rsaPrivateKeyFromPem(
                Utils.readClasspathResource(IS7_TRUSTED_IDP_KEY_RESOURCE));
        String assertion = signingInput + "." + JwtTestUtils.signRs256(signingInput, privateKey);

        String body = "grant_type=" + Utils.urlEncode("urn:ietf:params:oauth:grant-type:jwt-bearer")
                + "&assertion=" + Utils.urlEncode(assertion);
        HttpResponse response = Requests.post(IntegrationActors.tokenEndpoint(IntegrationActors.IS), clientCredentialsHeader(), body,
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
        captureTokens(response);
    }

    /**
     * Registers a trusted SAML identity provider in the EXTERNAL key manager (IS) for the RFC 7522 saml2-bearer
     * grant, via IS's IdP REST API. Unlike jwt-bearer (which resolves the trusted IdP by name), the saml2-bearer
     * grant handler resolves the assertion issuer through the SAML federated authenticator's {@code IdPEntityId}
     * property - an IdP with only a name/cert/alias is rejected with "Identity provider is null" - so the IdP is
     * created with a SAMLSSOAuthenticator config carrying IdPEntityId = the generated IdP name (the SPEntityId /
     * SSOUrl values are required by the authenticator schema but never used: the grant only reads IdPEntityId,
     * the certificate and the token-endpoint alias). Stores the name (= assertion issuer) under {@code idpNameKey}.
     */
    @When("I register a SAML bearer identity provider in the external key manager storing its name as {string}")
    public void iRegisterSamlBearerIdpAtExternalKm(String idpNameKey) throws Exception {

        String idpName = Names.unique("samlIdp");
        String certPem = Utils.readClasspathResource(IS7_TRUSTED_IDP_CERT_RESOURCE);
        String certB64 = Base64.getEncoder().encodeToString(certPem.getBytes(StandardCharsets.UTF_8));
        // "U0FNTFNTT0F1dGhlbnRpY2F0b3I" = base64("SAMLSSOAuthenticator"), the fixed authenticator id.
        String payload = new JSONObject()
                .put("name", idpName)
                .put("alias", IS7_ADVERTISED_TOKEN_ENDPOINT)
                .put("certificate", new JSONObject()
                        .put("certificates", new JSONArray().put(certB64)))
                .put("federatedAuthenticators", new JSONObject()
                        .put("defaultAuthenticatorId", "U0FNTFNTT0F1dGhlbnRpY2F0b3I")
                        .put("authenticators", new JSONArray().put(new JSONObject()
                                .put("authenticatorId", "U0FNTFNTT0F1dGhlbnRpY2F0b3I")
                                .put("isEnabled", true)
                                .put("properties", new JSONArray()
                                        .put(new JSONObject().put("key", "IdPEntityId").put("value", idpName))
                                        .put(new JSONObject().put("key", "SPEntityId").put("value", idpName + "-sp"))
                                        .put(new JSONObject().put("key", "SSOUrl")
                                                .put("value", "https://" + idpName + ".invalid/sso"))))))
                .toString();
        Map<String, String> headers = IntegrationActors.authHeaders(IntegrationActors.IS);
        HttpResponse resp = Requests.post(IntegrationActors.baseUrl(IntegrationActors.IS) + "api/server/v1/identity-providers", headers,
                payload, Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertTrue(resp != null && resp.getResponseCode() == 201,
                "IS IdP create failed for saml2-bearer trusted issuer '" + idpName + "': got="
                        + (resp == null ? "null" : resp.getResponseCode() + "/" + resp.getData()));
        TestContext.set(idpNameKey, idpName);
    }

    /**
     * Requests an access token from the EXTERNAL key manager (IS) via the RFC 7522 saml2-bearer grant: builds a
     * SAML 2.0 assertion (Issuer = the trusted IdP's entity id stored under {@code idpNameKey}, bearer subject
     * confirmation and audience restriction on the IdP's token-endpoint alias), signs it enveloped (RSA-SHA256,
     * JDK XML-dsig) with the committed test-IdP key, and exchanges the base64url-encoded assertion at the token
     * endpoint authenticated with the application's IS-issued client credentials. Captures the issued token.
     */
    @When("I request an OAuth access token from the external key manager using SAML bearer grant with issuer stored as {string}")
    public void iRequestSamlBearerTokenFromExternalKm(String idpNameKey) throws Exception {

        String issuer = TestContext.resolve(idpNameKey).toString();
        String assertionXml = buildSignedSamlAssertion(issuer, Constants.SUPER_TENANT_ADMIN_USERNAME,
                IS7_ADVERTISED_TOKEN_ENDPOINT);
        String assertion = JwtTestUtils.base64Url(assertionXml.getBytes(StandardCharsets.UTF_8));
        String body = "grant_type=" + Utils.urlEncode("urn:ietf:params:oauth:grant-type:saml2-bearer")
                + "&assertion=" + Utils.urlEncode(assertion);
        HttpResponse response = Requests.post(IntegrationActors.tokenEndpoint(IntegrationActors.IS), clientCredentialsHeader(), body,
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
        captureTokens(response);
    }

    /**
     * Builds a SAML 2.0 bearer assertion and signs it with the committed test-IdP key using the JDK's XML-dsig
     * (enveloped RSA-SHA256 signature placed after the Issuer element as the SAML schema requires). IS validates
     * the signature against the trusted IdP's certificate, the audience/recipient against the IdP's
     * token-endpoint alias, and the NotOnOrAfter windows.
     */
    private String buildSignedSamlAssertion(String issuer, String subject, String audience) throws Exception {

        // Back-date the window by 60s: IS validates NotBefore/IssueInstant against ITS clock, and the
        // containerized IS can lag the test JVM's clock (colima/Lima VM drift after host sleep - observed ~4s),
        // which rejects a NotBefore==now assertion with "Assertion is not valid according to the time window
        // provided in Conditions". A standard skew allowance keeps the assertion valid either way.
        java.time.Instant now = java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
                .minusSeconds(60);
        java.time.Instant notAfter = now.plusSeconds(360);
        String assertionId = "_" + java.util.UUID.randomUUID().toString().replace("-", "");
        String template = "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\""
                + assertionId + "\" IssueInstant=\"" + now + "\" Version=\"2.0\">"
                + "<saml2:Issuer Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:entity\">" + issuer
                + "</saml2:Issuer>"
                + "<saml2:Subject>"
                + "<saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\">" + subject
                + "</saml2:NameID>"
                + "<saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">"
                + "<saml2:SubjectConfirmationData NotOnOrAfter=\"" + notAfter + "\" Recipient=\"" + audience
                + "\"/></saml2:SubjectConfirmation></saml2:Subject>"
                + "<saml2:Conditions NotBefore=\"" + now + "\" NotOnOrAfter=\"" + notAfter + "\">"
                + "<saml2:AudienceRestriction><saml2:Audience>" + audience
                + "</saml2:Audience></saml2:AudienceRestriction></saml2:Conditions>"
                + "<saml2:AuthnStatement AuthnInstant=\"" + now + "\">"
                + "<saml2:AuthnContext><saml2:AuthnContextClassRef>"
                + "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport"
                + "</saml2:AuthnContextClassRef></saml2:AuthnContext></saml2:AuthnStatement>"
                + "</saml2:Assertion>";

        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        org.w3c.dom.Document doc = dbf.newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(template.getBytes(StandardCharsets.UTF_8)));
        org.w3c.dom.Element root = doc.getDocumentElement();
        // Mark ID as an XML ID so the signature's same-document reference "#<id>" resolves.
        root.setIdAttribute("ID", true);

        PrivateKey privateKey = JwtTestUtils.rsaPrivateKeyFromPem(
                Utils.readClasspathResource(IS7_TRUSTED_IDP_KEY_RESOURCE));
        String certPem = Utils.readClasspathResource(IS7_TRUSTED_IDP_CERT_RESOURCE);
        java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate)
                java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(
                        new java.io.ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8)));

        javax.xml.crypto.dsig.XMLSignatureFactory fac = javax.xml.crypto.dsig.XMLSignatureFactory.getInstance("DOM");
        javax.xml.crypto.dsig.Reference ref = fac.newReference("#" + assertionId,
                fac.newDigestMethod(javax.xml.crypto.dsig.DigestMethod.SHA256, null),
                java.util.List.of(
                        fac.newTransform(javax.xml.crypto.dsig.Transform.ENVELOPED,
                                (javax.xml.crypto.dsig.spec.TransformParameterSpec) null),
                        fac.newTransform(javax.xml.crypto.dsig.CanonicalizationMethod.EXCLUSIVE,
                                (javax.xml.crypto.dsig.spec.TransformParameterSpec) null)),
                null, null);
        javax.xml.crypto.dsig.SignedInfo signedInfo = fac.newSignedInfo(
                fac.newCanonicalizationMethod(javax.xml.crypto.dsig.CanonicalizationMethod.EXCLUSIVE,
                        (javax.xml.crypto.dsig.spec.C14NMethodParameterSpec) null),
                fac.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null),
                java.util.List.of(ref));
        javax.xml.crypto.dsig.keyinfo.KeyInfoFactory kif = fac.getKeyInfoFactory();
        javax.xml.crypto.dsig.keyinfo.KeyInfo keyInfo =
                kif.newKeyInfo(java.util.List.of(kif.newX509Data(java.util.List.of(cert))));

        org.w3c.dom.Element subjectEl = (org.w3c.dom.Element) root
                .getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Subject").item(0);
        javax.xml.crypto.dsig.dom.DOMSignContext signContext =
                new javax.xml.crypto.dsig.dom.DOMSignContext(privateKey, root, subjectEl);
        fac.newXMLSignature(signedInfo, keyInfo).sign(signContext);

        javax.xml.transform.Transformer transformer =
                javax.xml.transform.TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
        java.io.StringWriter writer = new java.io.StringWriter();
        transformer.transform(new javax.xml.transform.dom.DOMSource(doc),
                new javax.xml.transform.stream.StreamResult(writer));
        return writer.toString();
    }

    /**
     * Calls the admin key-manager well-known discovery endpoint ({@code POST /key-managers/discover}, multipart
     * form fields {@code url} + {@code type}) against the external IS's OIDC discovery document. APIM fetches the
     * document server-side (so IS must be reachable from the APIM container) and maps it to a KeyManager DTO;
     * the feature pins the populated endpoints/grants/certs - including the documented UserInfo auto-populate
     * gotcha ({@code oauth2/userinfo} from discovery vs the {@code scim2/Me} the IS7 connector actually needs).
     */
    @When("I discover key manager configuration from the external key manager well-known endpoint")
    public void iDiscoverKeyManagerFromWellKnown() throws IOException {

        Map<String, String> formFields = new HashMap<>();
        formFields.put("url", IS7_ADVERTISED_TOKEN_ENDPOINT + "/.well-known/openid-configuration");
        formFields.put("type", "WSO2-IS-7");
        Requests.postMultipart(Utils.getKeyManagersURL(Utils.getBaseUrl()) + "/discover", Identity.adminHeaders(),
                new HashMap<>(), formFields);
    }

    /**
     * Fetches the current user's profile from the EXTERNAL key manager's UserInfo endpoint using the stored
     * {@code generatedAccessToken}. For the IS7 connector this is SCIM2 {@code /scim2/Me} (the documented gotcha
     * vs {@code /oauth2/userinfo}); the token must carry the {@code openid} scope. Publishes the response as
     * httpResponse for the following assertions.
     */
    @When("I retrieve the current user profile from the external key manager userinfo endpoint")
    public void iRetrieveUserinfoFromExternalKm() throws Exception {

        String token = TestContext.resolve("generatedAccessToken").toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + token);
        Requests.get(IntegrationActors.baseUrl(IntegrationActors.IS) + "scim2/Me", headers);
    }

    /**
     * Tampers the stored {@code generatedAccessToken} JWT by altering a payload claim while keeping the original
     * header and signature, then stores the result back as {@code generatedAccessToken}. The signature no longer
     * matches the payload, so a gateway that validates the IS7 JWT against IS's JWKS must reject it (401). Only
     * valid for a 3-part JWT (self-validate mode).
     */
    @When("I tamper a claim in the generated access token")
    public void iTamperGeneratedAccessToken() {

        String token = TestContext.resolve("generatedAccessToken").toString();
        // Flip the subject so the payload no longer matches the (kept) signature — any claim change suffices.
        TestContext.set("generatedAccessToken", JwtTestUtils.tamperClaim(token, "sub"));
    }

    /**
     * Revokes the current {@code generatedAccessToken} at the EXTERNAL key manager's own revoke endpoint
     * (IS's {@code /oauth2/revoke}), authenticated with the application's IS-issued client credentials. For an
     * external-KM token, revocation must happen at the issuing IdP, not APIM's revoke endpoint.
     */
    @When("I revoke the access token at the external key manager")
    public void iRevokeTokenAtExternalKeyManager() throws Exception {

        String token = TestContext.resolve("generatedAccessToken").toString();
        String revokeEndpoint = IntegrationActors.baseUrl(IntegrationActors.IS) + "oauth2/revoke";

        Requests.post(revokeEndpoint, clientCredentialsHeader(), "token=" + token,
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
    }

    /**
     * Asserts that the current {@code generatedAccessToken} is reported as inactive by the EXTERNAL key
     * manager's own introspection endpoint (IS {@code /oauth2/introspect}), authenticated as the IS super
     * admin. Used after a revoke to prove the revocation actually propagated to IS itself - which isolates an
     * IS-side revoke problem from an APIM-gateway-side enforcement problem. The assertion message includes the
     * token structure (JWT vs opaque) and the raw introspection body for diagnostics.
     */
    @Then("the access token should be inactive at the external key manager introspection endpoint")
    public void theAccessTokenShouldBeInactiveAtExternalKmIntrospection() throws Exception {

        String token = TestContext.resolve("generatedAccessToken").toString();
        int parts = token.split("\\.").length;
        String introspectEndpoint = IntegrationActors.baseUrl(IntegrationActors.IS) + "oauth2/introspect";
        Map<String, String> headers = IntegrationActors.authHeaders(IntegrationActors.IS);
        HttpResponse resp = Requests.post(introspectEndpoint, headers, "token=" + token,
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
        // A failed/empty introspection must FAIL the step, never read as "inactive": RFC 7662 introspection
        // answers 200 with {"active":false} for a revoked token, so anything but a 2xx-with-body is a broken
        // call, and treating it as inactive would false-pass the revocation check.
        Assert.assertTrue(resp != null && resp.getResponseCode() >= 200 && resp.getResponseCode() < 300
                        && resp.getData() != null && !resp.getData().isBlank(),
                "IS introspection request failed (" + (parts == 3 ? "JWT" : "opaque/" + parts + "-part")
                        + " token): got=" + (resp == null ? "null" : resp.getResponseCode() + "/" + resp.getData()));
        boolean active = new JSONObject(resp.getData()).optBoolean("active", true);
        Assert.assertFalse(active, "Revoked token should be reported inactive by IS introspection ("
                + (parts == 3 ? "JWT" : "opaque/" + parts + "-part") + " token); IS /introspect returned: "
                + resp.getData());
    }

    /**
     * Asserts that a role (name stored under {@code isRoleKey}) exists in the EXTERNAL key manager (IS) by
     * querying IS's SCIM2 Roles API ({@code /scim2/v2/Roles?filter=displayName eq <name>}) as the IS super
     * admin. Used to prove RUNTIME role creation: when a shared scope bound to a new role is registered, the
     * WSO2-IS-7 connector (with enable_roles_creation=true) creates the derived role in IS. Because
     * APIProviderImpl.addSharedScope swallows a KM registerScope failure, the role must be verified here in IS
     * rather than via the scope-create response.
     */
    @Then("the role stored as {string} should exist at the external key manager")
    public void theRoleStoredShouldExistAtExternalKm(String isRoleKey) throws Exception {

        String roleName = TestContext.resolve(isRoleKey).toString();
        HttpResponse resp = queryIs7Role(roleName);
        int total = new JSONObject(resp.getData()).optInt("totalResults", 0);
        Assert.assertTrue(total >= 1, "Expected the IS7 connector to have created role '" + roleName
                + "' in IS, but SCIM2 Roles filter returned totalResults=" + total + ": " + resp.getData());
    }

    /**
     * Queries IS's SCIM2 Roles API for a role by displayName as the IS super admin and returns the (asserted
     * 200-with-body) response — the raw primitive behind the role-exists assertion above, also used by the
     * shared-scope-create step's KM-propagation poll (see PublisherBaseSteps).
     */
    static HttpResponse queryIs7Role(String roleName) throws Exception {

        String url = IntegrationActors.baseUrl(IntegrationActors.IS) + "scim2/v2/Roles?filter=" + Utils.urlEncode("displayName eq " + roleName);
        Map<String, String> headers = IntegrationActors.authHeaders(IntegrationActors.IS);
        HttpResponse resp = SimpleHTTPClient.getInstance().doGet(url, headers);
        Assert.assertTrue(resp != null && resp.getResponseCode() == 200 && resp.getData() != null
                        && !resp.getData().isBlank(),
                "IS SCIM2 Roles query failed for role '" + roleName + "': got="
                        + (resp == null ? "null" : resp.getResponseCode() + "/" + resp.getData()));
        return resp;
    }

    /** True once IS reports a role with the given displayName (see {@link #queryIs7Role}). */
    static boolean is7RoleExists(String roleName) throws Exception {
        return new JSONObject(queryIs7Role(roleName).getData()).optInt("totalResults", 0) >= 1;
    }

    /**
     * Requests a new access token using the refresh-token grant, authenticated with the application's
     * client credentials. Stores the new tokens and the raw response in context.
     *
     * @param refreshTokenKey Context key holding the refresh token to exchange
     */
    @When("I request a new OAuth access token using refresh token {string}")
    public void iRequestTokenUsingRefreshToken(String refreshTokenKey) throws Exception {

        String refreshToken = TestContext.resolve(refreshTokenKey).toString();

        String body = "grant_type=refresh_token&refresh_token=" + Utils.urlEncode(refreshToken);

        HttpResponse response = Requests.post(Utils.getAPIMTokenEndpointURL(Utils.getBaseUrl()),
                clientCredentialsHeader(), body, Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
        captureTokens(response);
    }

    /**
     * Revokes the given OAuth access token via the revocation endpoint, authenticated with the
     * application's client credentials. Stores the response in context.
     *
     * @param tokenKey Context key holding the access token to revoke
     */
    @When("I revoke the OAuth access token {string}")
    public void iRevokeOAuthAccessToken(String tokenKey) throws Exception {

        String token = TestContext.resolve(tokenKey).toString();

        Requests.post(Utils.getRevokeEndpointURL(Utils.getBaseUrl()),
                clientCredentialsHeader(), "token=" + token,
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
    }

    /**
     * Builds a Basic auth header from the application's generated client credentials
     * (consumerKey/consumerSecret) held in context.
     */
    private Map<String, String> clientCredentialsHeader() {
        return Identity.basicAuthHeaders(TestContext.resolve("consumerKey").toString(),
                TestContext.resolve("consumerSecret").toString());
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

        String token = TestContext.resolve("generatedAccessToken").toString();
        String[] parts = token.split("\\.");
        Assert.assertEquals(parts.length, 3,
                "Access token is not in JWT format (expected 3 dot-separated segments): " + token);

        String headerJson = JwtTestUtils.decodeHeader(token);
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

        String actualAppId = TestContext.resolve(appId).toString();
        String jsonPayload = TestContext.resolve(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = Requests.post(Utils.getGenerateAPIKeyURL(Utils.getBaseUrl(), actualAppId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
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
        String actualAppId = TestContext.resolve(appId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = Requests.get(Utils.getListAPIKeysURL(Utils.getBaseUrl(), actualAppId), headers);
        // The endpoint returns either a bare array [{...}] or a {"count":n,"list":[...]} wrapper depending on
        // the pack — handle both. Each scenario's app has a single key, so the first entry is the one to revoke.
        String data = response.getData().trim();
        JSONArray list = data.startsWith("[")
                ? new JSONArray(data)
                : new JSONObject(data).getJSONArray("list");
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
        String actualAppId = TestContext.resolve(appId).toString();
        String uuid = TestContext.resolve(uuidRef).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        String payload = "{\"keyUUID\":\"" + uuid + "\"}";

        Requests.post(Utils.getRevokeAPIKeyURL(Utils.getBaseUrl(), actualAppId), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Deletes a subscription by its ID.
     *
     * @param subscriptionId Context key containing the subscription ID to delete
     */
    @When("I delete the subscription with id {string}")
    public void iDeleteSubscription(String subscriptionId) throws Exception {
        String actualSubscriptionId = TestContext.resolve(subscriptionId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        Requests.delete(Utils.getSubscriptionURL(Utils.getBaseUrl(),
                actualSubscriptionId), headers);
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

        String actualSubscriptionId = TestContext.resolve(subscriptionId).toString();

        // Add application id and API id to the payload
        String jsonPayload = TestContext.resolve("subscriptionPayload").toString();
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

        Requests.put(Utils.getSubscriptionURL(Utils.getBaseUrl(), actualSubscriptionId),
                headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Retrieves the details of a specific subscription by its ID.
     *
     * @param subscriptionId Context key containing the subscription ID to retrieve
     */
    @When("I get the subscription with id {string}")
    public void iGetSubscription(String subscriptionId) throws Exception {

        String actualSubscriptionId = TestContext.resolve(subscriptionId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        Requests.get(Utils.getSubscriptionURL(Utils.getBaseUrl(),
                actualSubscriptionId), headers);
    }

    /**
     * Retrieves all subscriptions of an application (DevPortal {@code /subscriptions?applicationId=...}) as the
     * acting actor's devportal token, publishing the response for a following assertion. Used by APIMANAGER4373:
     * after one subscribed API becomes inaccessible to the subscriber (its visibility role changed away), the
     * subscription list must STILL return the other (healthy) subscription rather than breaking wholesale.
     *
     * @param appIdKey Context key containing the application ID
     */
    @When("I retrieve all subscriptions of application {string}")
    public void iRetrieveAllSubscriptionsOfApplication(String appIdKey) throws IOException {

        String appId = TestContext.resolve(appIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        Requests.get(
                Utils.getAllSubscriptionsURL(Utils.getBaseUrl(), null, appId, null, null, null), headers);
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

        String actualSubscriptionId = TestContext.resolve(subscriptionId).toString();
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        // Guard before parsing — a cleared/failed list retrieval must fail clearly, not as an NPE/JSONException.
        Assert.assertTrue(response != null && response.getData() != null && !response.getData().isEmpty(),
                "No subscription-list response with a body captured to search for subscription '"
                        + actualSubscriptionId + "' in");
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
        String url = Utils.getApiSearchURL(Utils.getBaseUrl(), query);
        long endTimeStart = System.currentTimeMillis();
        long endTime = endTimeStart + Constants.RUNTIME_PROPAGATION_TIMEOUT;

        // DevPortal search is backed by an asynchronous (Solr) index, so a freshly published API may
        // not be searchable immediately. Retry while the result set is pending (non-200, absent/empty body, or
        // zero count) until it appears or times out, riding out transient IOExceptions (the previous response,
        // if any, is retained). The body is parsed only when non-null and non-empty, and a malformed body is
        // treated as still-pending too (JSONException is a RuntimeException that would otherwise escape the
        // IOException-only catch and kill the poll mid-warm-up).
        HttpResponse response = null;
        while (true) {
            try {
                response = SimpleHTTPClient.getInstance().doGet(url, headers);
                boolean pending = response.getResponseCode() != 200
                        || response.getData() == null || response.getData().isEmpty()
                        || new JSONObject(response.getData()).optInt("count", 0) == 0;
                if (!pending) {
                    break;
                }
            } catch (IOException | JSONException stillPending) {
                // transient network failure or a not-yet-well-formed body during index warm-up — keep polling
            }
            if (System.currentTimeMillis() >= endTime) {
                break;
            }
            Utils.pollPause(endTimeStart, 2000);
        }

        Assert.assertNotNull(response, "DevPortal search '" + query + "' returned no response (every poll "
                + "attempt failed)");
        TestContext.set("httpResponse", response);
    }

    /**
     * Retrieves all documents available for an API in the Developer Portal.
     *
     * @param resourceId Context key containing the API ID
     */
    @And("I retrieve devportal documents for {string}")
    public void iRetrieveDevportalDocumentsFor(String resourceId) throws IOException {
        String actualApiId = TestContext.resolve(resourceId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        Requests.get(Utils.getApiDocumentsURL(Utils.getBaseUrl(), actualApiId), headers);
    }

    // ---- Key manager configuration (admin) -------------------------------------------------------------

    /**
     * Adds a system-scope role-alias mapping (admin REST {@code PUT /role-aliases}): maps {@code role} to include
     * {@code alias}. Ports APISystemScopesTestCase#testAddScopeMapping. Non-asserting.
     */
    @When("I set the role alias {string} for role {string}")
    public void iSetRoleAlias(String alias, String role) throws IOException {

        JSONObject entry = new JSONObject().put("role", role).put("aliases", new JSONArray().put(alias));
        JSONObject payload = new JSONObject().put("count", 1).put("list", new JSONArray().put(entry));
        Requests.put(Utils.getRoleAliasesURL(Utils.getBaseUrl()), Identity.adminHeaders(), payload.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Retrieves the system-scope role-alias mappings (admin REST {@code GET /role-aliases}). Non-asserting. */
    @When("I retrieve the role aliases")
    public void iRetrieveRoleAliases() throws IOException {

        Requests.get(Utils.getRoleAliasesURL(Utils.getBaseUrl()),
                Identity.adminHeaders());
    }

    /**
     * Clears all system-scope role-alias mappings (admin REST {@code PUT /role-aliases} with an empty list) —
     * the teardown for the role-alias mapping test. Non-asserting.
     */
    @When("I clear all role aliases")
    public void iClearRoleAliases() throws IOException {

        JSONObject payload = new JSONObject().put("count", 0).put("list", new JSONArray());
        Requests.put(Utils.getRoleAliasesURL(Utils.getBaseUrl()),
                Identity.adminHeaders(), payload.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        String name = TestContext.resolve(nameKey).toString();
        HttpResponse response = Requests.get(
                Utils.getThrottlePolicyExportURL(Utils.getBaseUrl(), name, throttleExportType(kind)), Identity.adminHeaders());
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

        String body = TestContext.resolve(exportKey).toString();
        java.io.File temp = java.io.File.createTempFile("throttle-policy", ".json");
        temp.deleteOnExit();
        java.nio.file.Files.write(temp.toPath(), body.getBytes(StandardCharsets.UTF_8));

        Map<String, java.io.File> files = new HashMap<>();
        files.put("file", temp);
        Requests.postMultipart(
                Utils.getThrottlePolicyImportURL(Utils.getBaseUrl(), overwrite), Identity.adminHeaders(), files, new HashMap<>());
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

        HttpResponse response = Requests.post(Utils.getKeyManagersURL(Utils.getBaseUrl()),
                Identity.adminHeaders(), payload.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
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
     * Creates a key manager like {@link #iCreateKeyManager} and then BLOCKS until it is OPERATIONAL — i.e.
     * visible to the runtime key-manager holder, not just persisted. A freshly-created KM propagates to the
     * in-memory holder ASYNCHRONOUSLY (eventhub); a keygen inside that window fails deep in the registration
     * workflow with "Key Manager ... not configured" AFTER the key-mapping row is inserted, leaking the row —
     * and the HTTP client's general-error retry then re-POSTs and surfaces a misleading 901409 "Key Mappings
     * already exists" on a fresh app. The probe here is the exact call that needs the holder: a throwaway
     * application + keygen against the new KM, deleting the throwaway app after every attempt (which also
     * removes any leaked mapping row). Use this variant when a scenario generates keys shortly after
     * registering the KM; NOT usable in KM config-CRUD tests that register deliberately unreachable/disabled
     * key managers (the probe would never converge — those tests don't generate keys anyway).
     */
    @When("I create a key manager from payload {string} as {string} and wait until it is operational")
    public void iCreateKeyManagerAndAwaitOperational(String resourcePath, String idKey) throws Exception {

        iCreateKeyManager(resourcePath, idKey);
        String kmId = TestContext.resolve(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        long deadlineStart = System.currentTimeMillis();
        long deadline = deadlineStart + 60_000;
        int attempts = 0;
        while (true) {
            attempts++;
            // Probe via the raw client so the step's published httpResponse stays the KM-create 201.
            String probeAppName = Names.unique("kmProbeApp");
            String appPayload = "{\"name\":\"" + probeAppName
                    + "\",\"throttlingPolicy\":\"Unlimited\",\"description\":\"KM propagation probe\"}";
            HttpResponse appResp;
            try {
                appResp = SimpleHTTPClient.getInstance().doPost(
                        Utils.getApplicationCreateURL(Utils.getBaseUrl()), headers, appPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
            } catch (IOException transientFailure) {
                // Outcome UNKNOWN — the create may have committed with the response lost, and the next probe
                // uses a FRESH name, so an orphan would never be swept. Register any survivor for teardown,
                // then retry within the deadline.
                String orphanId = Utils.findIdByNameInListResponse(
                        Utils.getApplicationSearchURL(Utils.getBaseUrl(), probeAppName), headers, probeAppName,
                        "applicationId");
                if (orphanId != null) {
                    ResourceCleanup.register(Constants.CREATED_APPLICATION_IDS, orphanId);
                }
                if (System.currentTimeMillis() > deadline) {
                    throw transientFailure;
                }
                Utils.pollPause(deadlineStart, 2000);   // mutating probe: each retry creates an app + keygen
                continue;
            }
            Assert.assertTrue(appResp != null && appResp.getResponseCode() == 201,
                    "KM-propagation probe app create failed: got=" + (appResp == null ? "null"
                            : appResp.getResponseCode() + "/" + appResp.getData()));
            String probeAppId = String.valueOf(Utils.extractValueFromPayload(appResp.getData(), "applicationId"));
            // Register the throwaway app for teardown IMMEDIATELY — the keygen probe or the delete below can
            // throw, and a created-but-unregistered app would leak. The successful per-attempt delete
            // deregisters it again so the sweep never chases an already-gone id.
            ResourceCleanup.register(Constants.CREATED_APPLICATION_IDS, probeAppId);
            boolean operational = false;
            try {
                String keygenPayload = "{\"keyType\":\"PRODUCTION\",\"keyManager\":\"" + kmId
                        + "\",\"grantTypesToBeSupported\":[\"client_credentials\"]}";
                HttpResponse keyResp = SimpleHTTPClient.getInstance().doPost(
                        Utils.getGenerateApplicationKeysURL(Utils.getBaseUrl(), probeAppId), headers, keygenPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
                operational = keyResp != null && keyResp.getResponseCode() >= 200 && keyResp.getResponseCode() < 300;
            } catch (IOException transientKeygenFailure) {
                // transient — the probe simply counts as not operational this round; the finally still
                // deletes the throwaway app and the deadline bounds the overall wait
            } finally {
                // Delete the probe app regardless: it removes the probe keys AND any mapping row a
                // pre-propagation keygen attempt leaked. Only a successful delete deregisters — a failed or
                // throwing delete leaves the id registered for the teardown sweep.
                HttpResponse del = SimpleHTTPClient.getInstance().doDelete(
                        Utils.getApplicationCreateURL(Utils.getBaseUrl()) + "/" + probeAppId, headers);
                if (del != null && del.getResponseCode() >= 200 && del.getResponseCode() < 300) {
                    ResourceCleanup.deregister(Constants.CREATED_APPLICATION_IDS, probeAppId);
                }
            }
            if (operational) {
                return;
            }
            if (System.currentTimeMillis() > deadline) {
                Assert.fail("Key manager '" + kmId + "' did not become operational (holder propagation) within "
                        + "60s (" + attempts + " keygen probes)");
            }
            Utils.pollPause(deadlineStart, 2000);   // mutating probe: each retry creates an app + keygen
        }
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

    /**
     * Attempts to create a key manager from a payload verbatim (connector config intact) — for connector-config
     * validation negatives where a specific mandatory key inside additionalProperties is missing, so the whole-
     * config-stripping variant does not apply. Non-asserting; the feature asserts the expected status.
     */
    @When("I attempt to create a key manager from payload {string}")
    public void iAttemptToCreateKeyManager(String resourcePath) throws IOException {

        postKeyManager(loadKeyManagerPayload(resourcePath));
    }

    /**
     * Attempts to create a key manager using the acting actor's PUBLISHER token (which lacks {@code apim:admin}) —
     * the non-admin-authorization negative. Non-asserting; the feature asserts the 401. On the expected rejection
     * nothing is created, so no cleanup registration is needed.
     */
    @When("I attempt to create a key manager from payload {string} using the publisher token")
    public void iAttemptToCreateKeyManagerAsPublisher(String resourcePath) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.post(Utils.getKeyManagersURL(Utils.getBaseUrl()), headers,
                loadKeyManagerPayload(resourcePath).toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Retrieves a single key manager by the id held under {@code idKey}. */
    @When("I retrieve the key manager {string}")
    public void iRetrieveKeyManager(String idKey) throws IOException {

        String kmId = TestContext.resolve(idKey).toString();
        Requests.get(Utils.getKeyManagerByIdURL(Utils.getBaseUrl(), kmId), Identity.adminHeaders());
    }

    /** Lists all key managers. */
    @When("I retrieve all key managers")
    public void iRetrieveAllKeyManagers() throws IOException {

        Requests.get(Utils.getKeyManagersURL(Utils.getBaseUrl()), Identity.adminHeaders());
    }

    /**
     * Updates a key manager's description in place: retrieves the current config, replaces its description, and
     * PUTs it back. Non-asserting — the feature asserts the status and the reflected description.
     */
    @When("I update the key manager {string} setting its description to {string}")
    public void iUpdateKeyManagerDescription(String idKey, String newDescription) throws IOException {

        String kmId = TestContext.resolve(idKey).toString();
        HttpResponse current = SimpleHTTPClient.getInstance()
                .doGet(Utils.getKeyManagerByIdURL(Utils.getBaseUrl(), kmId), Identity.adminHeaders());
        // Intermediate GET of a GET→mutate→PUT: confirm a 2xx response WITH a body before parsing, so a
        // failed/empty fetch fails clearly instead of throwing an opaque JSONException/NPE.
        Assert.assertTrue(current != null && current.getResponseCode() >= 200 && current.getResponseCode() < 300
                        && current.getData() != null && !current.getData().isEmpty(),
                "Failed to fetch key manager '" + kmId + "' before updating its description: expected a 2xx response "
                        + "with a body, got " + (current == null ? "no response" : current.getResponseCode()
                        + " / body=" + current.getData()));

        JSONObject km = new JSONObject(current.getData());
        km.put("description", newDescription);

        Requests.put(
                Utils.getKeyManagerByIdURL(Utils.getBaseUrl(), kmId), Identity.adminHeaders(), km.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Toggles a key manager's {@code enabled} flag in place (GET→modify→PUT), resolving {@code "true"}/{@code
     * "false"}. Non-asserting — the feature asserts the status and the reflected {@code enabled} value. Used for
     * the enable/disable round-trip.
     */
    @When("I update the key manager {string} setting its enabled state to {string}")
    public void iUpdateKeyManagerEnabled(String idKey, String enabled) throws IOException {

        String kmId = TestContext.resolve(idKey).toString();
        HttpResponse current = SimpleHTTPClient.getInstance()
                .doGet(Utils.getKeyManagerByIdURL(Utils.getBaseUrl(), kmId), Identity.adminHeaders());
        // Intermediate GET of a GET→mutate→PUT: confirm a 2xx response WITH a body before parsing, so a
        // failed/empty fetch fails clearly instead of throwing an opaque JSONException/NPE.
        Assert.assertTrue(current != null && current.getResponseCode() >= 200 && current.getResponseCode() < 300
                        && current.getData() != null && !current.getData().isBlank(),
                "Failed to fetch key manager '" + kmId + "' before updating its enabled state: expected a 2xx response "
                        + "with a body, got " + (current == null ? "no response" : current.getResponseCode()
                        + " / body=" + current.getData()));

        JSONObject km = new JSONObject(current.getData());
        km.put("enabled", Boolean.parseBoolean(enabled));

        HttpResponse response = Requests.put(
                Utils.getKeyManagerByIdURL(Utils.getBaseUrl(), kmId), Identity.adminHeaders(), km.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Changes a key manager's {@code type} in place (GET→modify→PUT) — used to pin how the admin API treats a
     * change to the immutable connector type (rejected vs ignored). Non-asserting; the feature asserts the status
     * and, on a 2xx, whether the persisted type actually changed.
     */
    @When("I update the key manager {string} setting its type to {string}")
    public void iUpdateKeyManagerType(String idKey, String newType) throws IOException {

        String kmId = TestContext.resolve(idKey).toString();
        HttpResponse current = SimpleHTTPClient.getInstance()
                .doGet(Utils.getKeyManagerByIdURL(Utils.getBaseUrl(), kmId), Identity.adminHeaders());
        // Intermediate GET of a GET→mutate→PUT: confirm a 2xx response WITH a body before parsing, so a
        // failed/empty fetch fails clearly instead of throwing an opaque JSONException/NPE.
        Assert.assertTrue(current != null && current.getResponseCode() >= 200 && current.getResponseCode() < 300
                        && current.getData() != null && !current.getData().isBlank(),
                "Failed to fetch key manager '" + kmId + "' before updating its type: expected a 2xx response "
                        + "with a body, got " + (current == null ? "no response" : current.getResponseCode()
                        + " / body=" + current.getData()));

        JSONObject km = new JSONObject(current.getData());
        km.put("type", newType);

        HttpResponse response = Requests.put(
                Utils.getKeyManagerByIdURL(Utils.getBaseUrl(), kmId), Identity.adminHeaders(), km.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Deletes the key manager held under {@code idKey}. Non-asserting — the feature asserts the status. */
    @When("I delete the key manager {string}")
    public void iDeleteKeyManager(String idKey) throws IOException {

        String kmId = TestContext.resolve(idKey).toString();
        Requests.delete(Utils.getKeyManagerByIdURL(Utils.getBaseUrl(), kmId), Identity.adminHeaders());
    }

    /**
     * Swaps the key manager's certificate to a PEM read from a classpath certificate file (GET→modify→PUT):
     * sets {@code certificates} to {@code type=PEM, value=base64(PEM)} — the same encoding the create payloads
     * use. Drives the PEM key-rotation scenario: pinning a cert whose key pair differs from IS's live signer
     * makes gateway self-validation reject freshly-issued tokens, and re-uploading the live signing cert
     * restores them. Non-asserting — the feature asserts the status.
     */
    @When("I update the key manager {string} setting its PEM certificate from file {string}")
    public void iUpdateKeyManagerPemCertificate(String idKey, String certResourcePath) throws IOException {

        String pem;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(certResourcePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Resource not found: " + certResourcePath);
            }
            pem = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        String kmId = TestContext.resolve(idKey).toString();
        HttpResponse current = SimpleHTTPClient.getInstance()
                .doGet(Utils.getKeyManagerByIdURL(Utils.getBaseUrl(), kmId), Identity.adminHeaders());
        // Intermediate GET of a GET→mutate→PUT: confirm a 2xx response WITH a body before parsing, so a
        // failed/empty fetch fails clearly instead of throwing an opaque JSONException/NPE.
        Assert.assertTrue(current != null && current.getResponseCode() >= 200 && current.getResponseCode() < 300
                        && current.getData() != null && !current.getData().isBlank(),
                "Failed to fetch key manager '" + kmId + "' before updating its certificate: expected a 2xx "
                        + "response with a body, got " + (current == null ? "no response" : current.getResponseCode()
                        + " / body=" + current.getData()));

        JSONObject km = new JSONObject(current.getData());
        km.put("certificates", new JSONObject()
                .put("type", "PEM")
                .put("value", Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8))));

        HttpResponse response = Requests.put(
                Utils.getKeyManagerByIdURL(Utils.getBaseUrl(), kmId), Identity.adminHeaders(), km.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        String kmId = TestContext.resolve(idKey).toString();
        HttpResponse current = SimpleHTTPClient.getInstance()
                .doGet(Utils.getKeyManagerByIdURL(Utils.getBaseUrl(), kmId), Identity.adminHeaders());
        // Intermediate GET of a GET→mutate→PUT: confirm a 2xx response WITH a body before parsing, so a
        // failed/empty fetch fails clearly instead of throwing an opaque JSONException/NPE.
        Assert.assertTrue(current != null && current.getResponseCode() >= 200 && current.getResponseCode() < 300
                        && current.getData() != null && !current.getData().isEmpty(),
                "Failed to fetch key manager '" + kmId + "' before updating its allowed organizations: expected a 2xx "
                        + "response with a body, got " + (current == null ? "no response" : current.getResponseCode()
                        + " / body=" + current.getData()));
        JSONObject km = new JSONObject(current.getData());
        JSONArray orgs = new JSONArray();
        for (String o : Utils.resolveContextPlaceholders(allowedOrgs).split("\\s*,\\s*")) {
            orgs.put(o);
        }
        km.put("allowedOrganizations", orgs);
        Requests.put(
                Utils.getKeyManagerByIdURL(Utils.getBaseUrl(), kmId), Identity.adminHeaders(), km.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    // ---- Deny (blocking-condition) policies (admin) ----------------------------------------------------

    private HttpResponse postDenyPolicy(String conditionType, Object conditionValue) throws IOException {

        JSONObject dto = new JSONObject();
        dto.put("conditionType", conditionType);
        dto.put("conditionValue", conditionValue);
        dto.put("conditionStatus", true);
        HttpResponse response = Requests.post(Utils.getDenyPoliciesURL(Utils.getBaseUrl()),
                Identity.adminHeaders(), dto.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        String id = TestContext.resolve(idKey).toString();
        Requests.get(Utils.getDenyPolicyByIdURL(Utils.getBaseUrl(), id), Identity.adminHeaders());
    }

    /** Updates a deny policy's enabled status (PATCH conditionStatus). Non-asserting — the feature asserts. */
    @When("I set the deny policy {string} status to {string}")
    public void iSetDenyPolicyStatus(String idKey, String status) throws IOException {

        String id = TestContext.resolve(idKey).toString();
        JSONObject dto = new JSONObject();
        dto.put("conditionStatus", Boolean.parseBoolean(status));
        Requests.patch(
                Utils.getDenyPolicyByIdURL(Utils.getBaseUrl(), id), Identity.adminHeaders(), dto.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Deletes the deny policy held under {@code idKey}. Non-asserting — the feature asserts the status. */
    @When("I delete the deny policy {string}")
    public void iDeleteDenyPolicy(String idKey) throws IOException {

        String id = TestContext.resolve(idKey).toString();
        Requests.delete(Utils.getDenyPolicyByIdURL(Utils.getBaseUrl(), id), Identity.adminHeaders());
    }

    /** Searches deny policies by condition type and value (query grammar: conditionType:X&conditionValue:Y). */
    @When("I search deny policies of type {string} with value {string}")
    public void iSearchDenyPolicies(String conditionType, String conditionValue) throws IOException {

        String query = Utils.urlEncode("conditionType:" + conditionType + "&conditionValue:" + conditionValue);
        Requests.get(Utils.getDenyPoliciesURL(Utils.getBaseUrl()) + "?query=" + query, Identity.adminHeaders());
    }

    // ---- Tenant configuration (admin) ------------------------------------------------------------------

    /** Retrieves the tenant configuration. */
    @When("I retrieve the tenant configuration")
    public void iRetrieveTenantConfiguration() throws IOException {

        Requests.get(Utils.getTenantConfigURL(Utils.getBaseUrl()), Identity.adminHeaders());
    }

    /** Retrieves the tenant configuration JSON schema. */
    @When("I retrieve the tenant configuration schema")
    public void iRetrieveTenantConfigurationSchema() throws IOException {

        Requests.get(Utils.getTenantConfigSchemaURL(Utils.getBaseUrl()), Identity.adminHeaders());
    }

    /** Captures the current tenant configuration body under {@code contextKey} (for a round-trip update/restore). */
    @When("I capture the tenant configuration as {string}")
    public void iCaptureTenantConfiguration(String contextKey) throws IOException {

        HttpResponse response = Requests.get(Utils.getTenantConfigURL(Utils.getBaseUrl()), Identity.adminHeaders());
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        TestContext.set(Utils.normalizeContextKey(contextKey), response.getData());
    }

    /** Updates the tenant configuration with the JSON held under {@code contextKey}. Non-asserting. */
    @When("I update the tenant configuration from {string}")
    public void iUpdateTenantConfiguration(String contextKey) throws IOException {

        String payload = TestContext.resolve(contextKey).toString();
        Requests.put(Utils.getTenantConfigURL(Utils.getBaseUrl()), Identity.adminHeaders(), payload,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Attempts to update the tenant configuration with a syntactically-invalid (bad-signature) bearer JWT. */
    @When("I attempt to update the tenant configuration from {string} with an invalid token")
    public void iAttemptUpdateTenantConfigInvalidToken(String contextKey) throws IOException {

        String payload = TestContext.resolve(contextKey).toString();
        // A structurally-valid JWT with a bogus signature the server cannot verify.
        String invalidJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
                + "eyJzdWIiOiJhZG1pbiIsInNjb3BlIjoib3BlbmlkIGFwaW06YWRtaW4ifQ.aW52YWxpZF9zaWduYXR1cmU";
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + invalidJwt);
        Requests.put(Utils.getTenantConfigURL(Utils.getBaseUrl()),
                headers, payload, Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Attempts to update the tenant configuration with the acting actor's PUBLISHER token, which lacks the
     * {@code apim:admin} scope — the non-admin negative. Non-asserting; the feature asserts the status.
     */
    @When("I attempt to update the tenant configuration from {string} without admin scope")
    public void iAttemptUpdateTenantConfigNonAdmin(String contextKey) throws IOException {

        String payload = TestContext.resolve(contextKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.put(Utils.getTenantConfigURL(Utils.getBaseUrl()),
                headers, payload, Constants.CONTENT_TYPES.APPLICATION_JSON);
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
        HttpResponse response = Requests.post(Utils.getApplicationCreateURL(Utils.getBaseUrl()),
                headers, app.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        Object appId = Utils.extractValueFromPayload(response.getData(), "applicationId");
        TestContext.set(idKey, appId);
        ResourceCleanup.register(Constants.CREATED_APPLICATION_IDS, appId);
    }

    /** Retrieves a devportal application by id as the acting actor (200 visible / 403 not). Non-asserting. */
    @When("I retrieve the application {string}")
    public void iRetrieveApplicationById(String idKey) throws IOException {

        String appId = TestContext.resolve(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        Requests.get(Utils.getApplicationEndpointURL(Utils.getBaseUrl(), appId), headers);
    }

    /** Updates a devportal application's visibility in place (GET→modify→PUT). Non-asserting. */
    @When("I set the visibility of application {string} to {string}")
    public void iSetApplicationVisibility(String idKey, String visibility) throws IOException {

        String appId = TestContext.resolve(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        HttpResponse current = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApplicationEndpointURL(Utils.getBaseUrl(), appId), headers);
        // Intermediate GET of a GET→mutate→PUT: confirm a 2xx response WITH a body before parsing, so a
        // failed/empty fetch fails clearly instead of throwing an opaque JSONException/NPE.
        Assert.assertTrue(current != null && current.getResponseCode() >= 200 && current.getResponseCode() < 300
                        && current.getData() != null && !current.getData().isEmpty(),
                "Failed to fetch application '" + appId + "' before updating its visibility: expected a 2xx response "
                        + "with a body, got " + (current == null ? "no response" : current.getResponseCode()
                        + " / body=" + current.getData()));
        JSONObject app = new JSONObject(current.getData());
        app.put("visibility", visibility);
        Requests.put(
                Utils.getApplicationEndpointURL(Utils.getBaseUrl(), appId), headers, app.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    // ---- Consumer secret management (multiple client secrets, devportal) -------------------------------

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

        String appId = TestContext.resolve(appIdKey).toString();
        String keyMappingId = TestContext.resolve(keyMappingIdKey).toString();

        JSONObject additionalProperties = new JSONObject();
        if (description != null && !description.isEmpty()) {
            additionalProperties.put("description", description);
        }
        JSONObject request = new JSONObject();
        request.put("additionalProperties", additionalProperties);

        HttpResponse response = Requests.post(
                Utils.getGenerateApplicationSecretURL(Utils.getBaseUrl(), appId, keyMappingId), Identity.devportalHeaders(),
                request.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        if (response.getResponseCode() < 300) {
            TestContext.set(secretIdKey, Utils.extractValueFromPayload(response.getData(), "secretId"));
        }
    }

    /** Lists an application key mapping's consumer secrets. */
    @When("I retrieve the consumer secrets for application {string} with key mapping {string}")
    public void iRetrieveConsumerSecrets(String appIdKey, String keyMappingIdKey) throws IOException {

        String appId = TestContext.resolve(appIdKey).toString();
        String keyMappingId = TestContext.resolve(keyMappingIdKey).toString();
        Requests.get(Utils.getAllApplicationSecretsURL(Utils.getBaseUrl(), appId, keyMappingId), Identity.devportalHeaders());
    }

    /** Revokes a consumer secret (by the id held under {@code secretIdKey}). Non-asserting. */
    @When("I revoke the consumer secret {string} for application {string} with key mapping {string}")
    public void iRevokeConsumerSecret(String secretIdKey, String appIdKey, String keyMappingIdKey)
            throws IOException {

        String appId = TestContext.resolve(appIdKey).toString();
        String keyMappingId = TestContext.resolve(keyMappingIdKey).toString();
        String secretId = TestContext.resolve(secretIdKey).toString();

        JSONObject request = new JSONObject();
        request.put("secretId", secretId);
        Requests.post(
                Utils.getRevokeApplicationSecretURL(Utils.getBaseUrl(), appId, keyMappingId), Identity.devportalHeaders(),
                request.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Fetches an application's OAuth key details by key-mapping id. */
    @When("I fetch the oauth key details for application {string} with key mapping {string}")
    public void iFetchKeyDetailsByKeyMappingId(String appIdKey, String keyMappingIdKey) throws IOException {

        String appId = TestContext.resolve(appIdKey).toString();
        String keyMappingId = TestContext.resolve(keyMappingIdKey).toString();
        Requests.get(Utils.getUpdateKey(Utils.getBaseUrl(), appId, keyMappingId), Identity.devportalHeaders());
    }

    /**
     * Single-shot DevPortal API search (GET /apis?query=), publishing the response. Used where the expected result
     * can NEVER change to a match (e.g. a non-existent tag → empty result) so polling would only burn the timeout.
     *
     * @param query the DevPortal search query (placeholders resolved), e.g. {@code tags:xyz}
     */
    @When("I search DevPortal APIs once with query {string}")
    public void iSearchDevPortalAPIsOnceWithQuery(String query) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        String resolvedQuery = Utils.resolveContextPlaceholders(query);
        Requests.get(Utils.getApiSearchURL(Utils.getBaseUrl(), resolvedQuery), headers);
    }

    /**
     * DevPortal API search that polls until the result set actually CONTAINS the expected value (a specific API
     * name), not merely until it is non-empty — needed when asserting multiple APIs share a tag: the async index
     * can surface the first match before the others, so a plain non-empty poll would race. Publishes the last
     * response and asserts the presence after the loop, so a timeout fails the step itself.
     */
    @When("I search DevPortal APIs with query {string} until it contains {string} within {int} seconds")
    public void iSearchDevPortalAPIsWithQueryUntilContains(String query, String expected, int seconds)
            throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        String resolvedQuery = Utils.resolveContextPlaceholders(query);
        String resolvedExpected = Utils.resolveContextPlaceholders(expected);
        String url = Utils.getApiSearchURL(Utils.getBaseUrl(), resolvedQuery);
        long endTimeStart = System.currentTimeMillis();
        long endTime = endTimeStart + seconds * 1000L;
        HttpResponse response = null;
        boolean found = false;
        while (true) {
            try {
                response = Requests.get(url, headers);
                found = response.getResponseCode() == 200
                        && response.getData() != null && response.getData().contains(resolvedExpected);
            } catch (IOException transientFailure) {
                // transient network failure — keep polling; the previous response (if any) is retained
            }
            if (found || System.currentTimeMillis() >= endTime) {
                break;
            }
            Utils.pollPause(endTimeStart, 2000);
        }
        Assert.assertNotNull(response, "DevPortal search '" + resolvedQuery + "' returned no response (every poll "
                + "attempt failed)");
        Assert.assertTrue(found, "DevPortal search '" + resolvedQuery + "' did not contain '" + resolvedExpected
                + "' within " + seconds + "s; last response: " + response.getResponseCode()
                + " / " + response.getData());
    }

    /**
     * DevPortal API search that polls until the result set NO LONGER contains the given value — the removal
     * counterpart of the {@code until it contains} variant. Needed after mutating an API so a stale index entry
     * clears (e.g. ChangeAPITags: after removing a tag, the API must drop out of that tag's results). Polls on the
     * DISTINGUISHING new state (the value absent), publishes the last response, and asserts the absence after the
     * loop — so a timeout fails the step itself rather than passing silently (no null guard on the response:
     * {@code Requests.get} either throws or returns a real response, never null).
     */
    @When("I search DevPortal APIs with query {string} until it does not contain {string} within {int} seconds")
    public void iSearchDevPortalAPIsUntilAbsent(String query, String unexpected, int seconds)
            throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        String resolvedQuery = Utils.resolveContextPlaceholders(query);
        String resolvedUnexpected = Utils.resolveContextPlaceholders(unexpected);
        String url = Utils.getApiSearchURL(Utils.getBaseUrl(), resolvedQuery);
        long endTimeStart = System.currentTimeMillis();
        long endTime = endTimeStart + seconds * 1000L;
        HttpResponse response = null;
        boolean absent = false;
        while (true) {
            try {
                response = Requests.get(url, headers);
                absent = response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().contains(resolvedUnexpected);
            } catch (IOException transientFailure) {
                // transient network failure — keep polling; the previous response (if any) is retained
            }
            if (absent || System.currentTimeMillis() >= endTime) {
                break;
            }
            Utils.pollPause(endTimeStart, 2000);
        }
        Assert.assertNotNull(response, "DevPortal search '" + resolvedQuery + "' returned no response (every poll "
                + "attempt failed)");
        Assert.assertTrue(absent, "DevPortal search '" + resolvedQuery + "' still contained '" + resolvedUnexpected
                + "' after " + seconds + "s; last response: " + response.getResponseCode()
                + " / " + response.getData());
    }

    /**
     * Paginated DevPortal search: polls until the returned page {@code count} equals the expected value, then
     * leaves the response for assertion. Verifies the DevPortal page-size cap — with more matches than the limit,
     * the page count saturates at the limit. The count is compared exactly (asserted after the loop, not left to a
     * later Then), so a persistently wrong count fails the step itself. Guards the response before parsing its body.
     */
    @When("I search DevPortal APIs with query {string} and limit {int} until the result count is {int} within {int} seconds")
    public void iSearchDevPortalAPIsWithLimitUntilCount(String query, int limit, int expectedCount, int seconds)
            throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        String resolvedQuery = Utils.resolveContextPlaceholders(query);
        String url = Utils.getApiSearchURLWithLimit(Utils.getBaseUrl(), resolvedQuery, limit);
        long endTimeStart = System.currentTimeMillis();
        long endTime = endTimeStart + seconds * 1000L;
        HttpResponse response = null;
        int actual = -1;
        while (true) {
            try {
                response = Requests.get(url, headers);
                if (response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isEmpty()) {
                    actual = new JSONObject(response.getData()).optInt("count", -1);
                }
            } catch (IOException transientFailure) {
                // transient network failure — keep polling; the previous response (if any) is retained
            }
            if (actual == expectedCount || System.currentTimeMillis() >= endTime) {
                break;
            }
            Utils.pollPause(endTimeStart, 2000);
        }
        Assert.assertNotNull(response, "No paginated search response");
        Assert.assertEquals(actual, expectedCount,
                "DevPortal paginated page count did not reach the expected value");
    }

    /**
     * Retrieves the DevPortal tag cloud (GET /tags), polling until it contains the expected value — the tag cloud
     * is backed by the same async index as search, so a freshly published API's tags may not appear immediately.
     * Publishes the last response for the following count assertions, and asserts the presence after the loop so a
     * timeout fails the step itself.
     */
    @When("I retrieve the DevPortal tag cloud until it contains {string} within {int} seconds")
    public void iRetrieveDevPortalTagCloudUntilContains(String expected, int seconds)
            throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        String resolvedExpected = Utils.resolveContextPlaceholders(expected);
        String url = Utils.getTagsURL(Utils.getBaseUrl());
        long endTimeStart = System.currentTimeMillis();
        long endTime = endTimeStart + seconds * 1000L;
        HttpResponse response = null;
        boolean found = false;
        while (true) {
            try {
                response = Requests.get(url, headers);
                found = response.getResponseCode() == 200
                        && response.getData() != null && response.getData().contains(resolvedExpected);
            } catch (IOException transientFailure) {
                // transient network failure — keep polling; the previous response (if any) is retained
            }
            if (found || System.currentTimeMillis() >= endTime) {
                break;
            }
            Utils.pollPause(endTimeStart, 2000);
        }
        Assert.assertNotNull(response, "DevPortal tag cloud returned no response (every poll attempt failed)");
        Assert.assertTrue(found, "DevPortal tag cloud did not contain '" + resolvedExpected + "' within "
                + seconds + "s; last response: " + response.getResponseCode() + " / " + response.getData());
    }

    /**
     * Retrieves the DevPortal tag cloud, polling until it does NOT contain the given value — the absence
     * counterpart of the {@code until it contains} variant. Needed for role-restricted tag visibility: right after
     * a restricted API is published its visibility filter converges asynchronously in the index, so the tag can
     * LEAK transiently into an unauthorised viewer's cloud before settling. Polls to the steady state (a
     * persistent leak times out and fails with the cloud body); the absent flag requires a 200-with-body, so an
     * error response can never vacuously satisfy it. Publishes the last response for the following assertions.
     */
    @When("I retrieve the DevPortal tag cloud until it does not contain {string} within {int} seconds")
    public void iRetrieveDevPortalTagCloudUntilAbsent(String unexpected, int seconds)
            throws InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        String resolved = Utils.resolveContextPlaceholders(unexpected);
        String url = Utils.getTagsURL(Utils.getBaseUrl());
        long endTimeStart = System.currentTimeMillis();
        long endTime = endTimeStart + seconds * 1000L;
        HttpResponse response = null;
        boolean absent = false;
        while (true) {
            try {
                response = Requests.get(url, headers);
                absent = response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().contains(resolved);
            } catch (IOException transientFailure) {
                // transient network failure — keep polling; the previous response (if any) is retained
            }
            if (absent || System.currentTimeMillis() >= endTime) {
                break;
            }
            Utils.pollPause(endTimeStart, 2000);
        }
        Assert.assertNotNull(response, "DevPortal tag cloud returned no response (every poll attempt failed)");
        Assert.assertTrue(absent, "DevPortal tag cloud still contained '" + resolved + "' after " + seconds
                + "s (a restricted tag leaking persistently, not a transient index window); last response: "
                + response.getResponseCode() + " / " + response.getData());
    }

    /**
     * Asserts a specific tag value appears in the captured tag-cloud response with an exact usage count. Parses the
     * JSON list (not a substring match) so case- and space-distinct tag values are compared exactly.
     */
    @Then("the DevPortal tag cloud should contain tag {string} with count {int}")
    public void tagCloudShouldContainTagWithCount(String tagValue, int expectedCount) {
        String resolvedTag = Utils.resolveContextPlaceholders(tagValue);
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertNotNull(response, "No tag cloud response captured");
        Assert.assertEquals(response.getResponseCode(), 200, "Tag cloud retrieval failed");
        JSONArray list = new JSONObject(response.getData()).getJSONArray("list");
        Integer actual = null;
        for (int i = 0; i < list.length(); i++) {
            JSONObject tag = list.getJSONObject(i);
            if (resolvedTag.equals(tag.optString("value"))) {
                actual = tag.optInt("count");
                break;
            }
        }
        Assert.assertNotNull(actual, "Tag '" + resolvedTag + "' not found in the tag cloud");
        Assert.assertEquals(actual.intValue(), expectedCount,
                "Tag '" + resolvedTag + "' has an unexpected count in the tag cloud");
    }
}
