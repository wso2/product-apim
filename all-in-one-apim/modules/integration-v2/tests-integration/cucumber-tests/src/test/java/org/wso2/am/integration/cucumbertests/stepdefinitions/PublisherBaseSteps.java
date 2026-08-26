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
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.HealGate;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Names;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.FileNotFoundException;

public class PublisherBaseSteps {

    private static final Logger logger = LoggerFactory.getLogger(PublisherBaseSteps.class);

    BaseSteps baseSteps = new BaseSteps();

    /**
     * Creates a new resource (API, API Product, etc.) using a JSON payload and stores
     * both the HTTP response and the created resource ID in the test context.
     *
     * @param resourceType Type of resource to create (e.g., "apis", "api-products")
     * @param payload Context key containing the resource creation JSON payload
     * @param resourceID Context key where the created resource ID will be stored
     */
    @And("I create an {string} resource with payload {string} as {string}")
    public void iCreateAnAPIWithPayloadAs(String resourceType, String payload, String resourceID) throws IOException {

        String jsonPayload = TestContext.resolve(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse apiCreateResponse = Requests.post(Utils.getAPICreateEndpointURL(Utils.getBaseUrl(), resourceType), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        Assert.assertEquals(apiCreateResponse.getResponseCode(), 201, apiCreateResponse.getData());
        Object createdId = Utils.extractValueFromPayload(apiCreateResponse.getData(), "id");
        TestContext.set(resourceID, createdId);
        // Register for scenario teardown so a shared-server suite does not accumulate APIs across scenarios.
        ResourceCleanup.register(cleanupListFor(resourceType), createdId);
    }

    /**
     * Attempts to create an API without asserting success, storing the raw response as {@code httpResponse}
     * so the feature can assert the resulting status itself. Unlike {@code iCreateAnAPIWithPayloadAs} (which
     * asserts 201), this is for negative / access-control scenarios where the create is expected to be rejected
     * (e.g. a subscriber-role user receiving 401/403), so it asserts no status.
     *
     * <p>It DOES register an unexpectedly-created resource: {@code registerIfCreated} enqueues the id only on a
     * 2xx, so a refusal is a no-op while a create that unexpectedly succeeds is still swept (§5) instead of
     * leaking into the shared container.</p>
     */
    @When("I attempt to create an {string} resource with payload {string}")
    public void iAttemptToCreateAnAPIWithPayload(String resourceType, String payload) throws IOException {

        String jsonPayload = TestContext.resolve(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        // Callers expect a refusal; an unexpected success still creates a real resource, so it is swept (§5).
        ResourceCleanup.registerIfCreated(cleanupListFor(resourceType),
                Requests.post(Utils.getAPICreateEndpointURL(Utils.getBaseUrl(), resourceType), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON), "id");
    }

    /**
     * The teardown list for a publisher resourceType. Registering an api-product under the API list would make the
     * sweep DELETE it via the APIs endpoint — a 404 WARN, and the product leaks. Unknown types fail loudly rather
     * than silently registering to the wrong list.
     */
    private static String cleanupListFor(String resourceType) {
        switch (resourceType) {
            case "apis":
                return Constants.CREATED_API_IDS;
            case "api-products":
                return Constants.CREATED_API_PRODUCT_IDS;
            // Reached only from the DELETE path, which additionally takes these two. Mapping them here rather
            // than defaulting matters: a delete that deregisters from the wrong list is a no-op, so the id stays
            // queued, the sweep deletes it a second time, and the resulting "may leak" WARN reads as a product
            // defect (it was filed as one — "deleting an already-deleted API product returns 500, not 404").
            case "operation-policies":
                return Constants.CREATED_OPERATION_POLICY_IDS;
            case "mcp-servers":
                return ResourceCleanup.CREATED_MCP_SERVER_IDS;
            default:
                throw new IllegalArgumentException("No cleanup list is wired for resource type '" + resourceType
                        + "' — add one before creating it here, or the resource will leak (CLAUDE.md §5).");
        }
    }

    /**
     * Attempts to create a resource WITHOUT asserting a status, but stores and registers the created id when the
     * create does succeed. This is the primitive for a create whose outcome is exactly what the scenario pins and
     * which may legitimately be a 201 — e.g. the APIM514 "missing mandatory field" cases where the product
     * ACCEPTS an omitted endpoint configuration / empty operation list. It differs from its sibling
     * {@link #iAttemptToCreateAnAPIWithPayload} only in STORING the new id under a caller-named context key —
     * both register an unexpectedly-created resource for the teardown sweep (§5).
     *
     * @param resourceType type of resource to create (e.g. {@code apis})
     * @param payload      context key holding the create payload
     * @param resourceID   context key to store the created id under (set only on a 2xx)
     */
    @When("I attempt to create an {string} resource with payload {string} as {string}")
    public void iAttemptToCreateAnAPIWithPayloadAs(String resourceType, String payload, String resourceID)
            throws IOException {

        String jsonPayload = TestContext.resolve(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse response = Requests.post(Utils.getAPICreateEndpointURL(Utils.getBaseUrl(), resourceType),
                headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);
        if (response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
            if (createdId != null) {
                TestContext.set(resourceID, createdId);
                ResourceCleanup.register(cleanupListFor(resourceType), createdId);
            }
        }
    }

    /**
     * Attempts to create a resource with NO Authorization header — the unauthenticated-create negative (401).
     * Non-asserting; the feature asserts the status.
     */
    @When("I attempt to create an {string} resource with payload {string} without authentication")
    public void iAttemptToCreateAnAPIWithoutAuth(String resourceType, String payload) throws IOException {

        String jsonPayload = TestContext.resolve(payload).toString();

        // An unauthenticated create MUST be refused; if the product ever accepted one, the resource is real and
        // would leak, so it is swept (§5). registerIfCreated is a no-op on the expected 401.
        ResourceCleanup.registerIfCreated(cleanupListFor(resourceType),
                Requests.post(Utils.getAPICreateEndpointURL(Utils.getBaseUrl(), resourceType), new HashMap<>(),
                        jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON), "id");
    }

    /**
     * Attempts the same unauthenticated create through an explicitly tenant-qualified publisher endpoint.
     * The request has no token, so the {@code /t/{tenantDomain}} path is the only tenant-routing signal.
     */
    @When("I attempt to create an {string} resource with payload {string} without authentication in tenant {string}")
    public void iAttemptToCreateAnAPIWithoutAuthInTenant(String resourceType, String payload, String tenantDomain)
            throws IOException {

        String jsonPayload = TestContext.resolve(payload).toString();
        String tenantPrefix = "carbon.super".equals(tenantDomain) ? "" : "t/" + tenantDomain + "/";
        String endpoint = Utils.getBaseUrl() + tenantPrefix + Constants.DEFAULT_APIM_API_DEPLOYER + resourceType;

        ResourceCleanup.registerIfCreated(cleanupListFor(resourceType),
                Requests.post(endpoint, new HashMap<>(), jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON),
                "id");
    }

    /**
     * Updates an existing resource using a JSON payload stored in the test context.
     *
     * @param resourceType Type of resource to update (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID to update
     * @param payload Context key containing the resource update JSON payload
     */
    @When("I update {string} resource of id {string} with payload {string}")
    public void iUpdateResourceWithJsonPayloadFromContext(String resourceType, String resourceId, String payload) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        String jsonPayload = TestContext.resolve(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Requests.put(
                Utils.getResourceEndpointURL(Utils.getBaseUrl(),resourceType ,actualResourceId), headers, jsonPayload,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Creates a new revision for a resource (API or API Product).
     * The revision ID is stored in the test context as "revisionId" for use in deployment steps.
     *
     * @param resourceType Type of resource (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID
     * @param contextKey Context key containing the revision creation JSON payload
     */
    @When("I make a request to create a revision for {string} resource {string} with payload {string}")
    public void iCreateResourceRevision(String resourceType, String resourceId, String contextKey) throws IOException, InterruptedException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        String jsonPayload = TestContext.resolve(contextKey).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        // Creating a revision immediately after creating the API races the publisher plane's ASYNC registry/Solr
        // artifact indexing: the revision endpoint reads the API by id (getPublisherAPI), which under load may
        // not be consistently readable yet, so the POST 500s with "Error while adding new API Revision ...
        // artifact does not exist". Under full-suite parallel load this window widened enough to cascade — a
        // failed revision in the non-asserting _setup_config_api fixture orphaned every downstream scenario.
        // Retry the POST until it returns 201 (the artifact settles), catching only transient IOException; a
        // genuinely bad payload still fails after the deadline. The final 201 is published as httpResponse.
        String url = Utils.getRevisionURL(Utils.getBaseUrl(), resourceType, actualResourceId);
        long endTimeStart = System.currentTimeMillis();
        long endTime = endTimeStart + Constants.RUNTIME_PROPAGATION_TIMEOUT;
        HttpResponse createRevisionResponse = null;
        while (true) {
            try {
                createRevisionResponse = Requests.post(url, headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);
                if (createRevisionResponse.getResponseCode() == 201) {
                    break;
                }
            } catch (IOException transientDuringIndexing) {
                // Transient connectivity during warm-up — retry. Revision-create is NOT idempotent, so a lost
                // response could leave a phantom revision and this re-POST would produce a second one. That is
                // accepted deliberately rather than reconciled: the API is scenario-owned, and a revision is a
                // child resource that dies with it (§5), so nothing leaks; and no scenario creates more than 2
                // revisions against APIM's cap of 5, so a phantom cannot exhaust the quota. Reconciling would
                // mean adopting "the newest revision" heuristically (the payload description is a fixed string,
                // not a unique key), and DELETING a discovered revision would be worse still — a deployed
                // revision must be undeployed first. The documented failure mode here is a 500 (API artifact not
                // yet indexed), which creates no revision and is safe to re-POST.
            }
            if (System.currentTimeMillis() >= endTime) {
                break;
            }
            Utils.pollPause(endTimeStart, Constants.RETRY_INTERVAL_TIME);
        }

        Assert.assertNotNull(createRevisionResponse,
                "Revision creation never returned a response for " + resourceType + " " + actualResourceId);
        Assert.assertEquals(createRevisionResponse.getResponseCode(), 201, createRevisionResponse.getData());
        Object newRevisionId = Utils.extractValueFromPayload(createRevisionResponse.getData(), "id");
        TestContext.set("revisionId", newRevisionId);
        awaitRevisionReadable(resourceType, actualResourceId, newRevisionId.toString());
    }

    /**
     * Polls until a just-created revision is READABLE by id, replacing the blind {@code Thread.sleep(3000)} that
     * used to end this step (CLAUDE.md §4 — "wait, never sleep").
     *
     * <p>The sleep was covering something real: the very next thing every caller does is POST the deploy for this
     * revision ({@link #iDeployApiRevisionGivenPayload}), which issues a single request with NO retry and whose
     * 201 is asserted immediately — so a revision that is not yet addressable fails the deploy outright. Deleting
     * the sleep without this gate would surface as an intermittent deploy failure on EVERY deploy path. Polling
     * the revisions LIST until the new id appears is the condition the sleep was approximating, and it exits as
     * soon as the revision is there rather than always paying 3s.
     *
     * <p>Deliberately the LIST endpoint, not {@code Utils.getRevisionByID}: the product answers a GET on
     * {@code /revisions/{id}} with <b>501 Not Implemented</b> (that builder serves DELETE), so polling it can
     * never succeed — measured, after this gate first shipped against it and failed on every deploy.
     */
    private void awaitRevisionReadable(String resourceType, String resourceId, String revisionId)
            throws IOException, InterruptedException {

        String url = Utils.getRevisionURL(Utils.getBaseUrl(), resourceType, resourceId);
        Map<String, String> headers = Identity.publisherHeaders();
        HttpResponse last = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                () -> SimpleHTTPClient.getInstance().doGet(url, headers),
                response -> response != null && response.getResponseCode() == 200
                        && response.getData() != null && response.getData().contains(revisionId));
        Assert.assertTrue(last != null && last.getResponseCode() == 200 && last.getData() != null
                        && last.getData().contains(revisionId),
                "Revision " + revisionId + " of " + resourceType + " " + resourceId + " never appeared in the "
                        + "revisions listing; got=" + (last == null ? "null"
                        : last.getResponseCode() + "/" + last.getData()));
    }

    /**
     * Attempts to create a revision without asserting success — for negatives (e.g. a revision expected to be
     * blocked by a governance BLOCK-on-deploy policy with 903300). The feature asserts the resulting status
     * and error code.
     */
    @When("I attempt to create a revision for {string} resource {string} with payload {string}")
    public void iAttemptToCreateResourceRevision(String resourceType, String resourceId, String contextKey)
            throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        String jsonPayload = TestContext.resolve(contextKey).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Requests.post(Utils.getRevisionURL(Utils.getBaseUrl(), resourceType, actualResourceId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Deploys a specific revision of a resource to the gateway environment.
     *
     * @param revisionId Context key containing the revision ID to deploy
     * @param resourceType Type of resource (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID
     * @param payload Context key containing the deployment configuration JSON payload
     */
    @When("I make a request to deploy revision {string} of {string} resource {string} with payload {string}")
    public void iDeployApiRevisionGivenPayload(String revisionId, String resourceType, String resourceId, String payload) throws IOException {

        String actualResourceId= TestContext.resolve(resourceId).toString();
        String actualRevisionId = TestContext.resolve(revisionId).toString();
        String jsonPayload = TestContext.resolve(payload).toString();
        jsonPayload = jsonPayload.replace("{{gatewayEnvironment}}", System.getenv(Constants.GATEWAY_ENVIRONMENT));
        // Resolve any remaining {{contextKey}} placeholders (e.g. a captured custom environment name for a
        // deploy-to-vhost scenario). No-op when the payload has none; fails fast on an unknown key.
        jsonPayload = Utils.resolveContextPlaceholders(jsonPayload);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Requests.post(Utils.getRevisionDeploymentURL(Utils.getBaseUrl(), resourceType, actualResourceId, actualRevisionId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

     /**
     * Deletes a resource (API, API Product, etc.) by its ID.
     *
     * @param resourceType Type of resource to delete (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID to delete
     */
    @When("I delete the {string} resource with id {string}")
    public void iDeleteTheResource(String resourceType, String resourceId) throws IOException {

        String actualResourceId= TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse response = Requests.delete(Utils.getResourceEndpointURL(Utils.getBaseUrl(), resourceType,
                actualResourceId), headers);
        // A resource the scenario deletes ITSELF must be dropped from the teardown sweep: otherwise
        // ResourceCleanup later chases the already-gone id and logs a spurious "resource NOT deleted; may leak"
        // that misdirects triage (e.g. an export/delete/import scenario whose import then fails). Deregister only
        // on a confirmed 2xx delete — a failed delete (negative test) means the resource still exists and must
        // stay registered.
        if (response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            ResourceCleanup.deregister(cleanupListFor(resourceType), actualResourceId);
        }
    }

    /**
     * Generates the inline mock implementation script for an API (POST /apis/{id}/generate-mock-scripts).
     * Ports the generateMockScript call of PrototypedAPITestcase inline-mock tests. Non-asserting.
     *
     * @param apiId context key holding the API id
     */
    @When("I generate the mock implementation script for API {string}")
    public void iGenerateMockScript(String apiId) throws IOException {

        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.post(Utils.getGenerateMockScriptsURL(Utils.getBaseUrl(), actualApiId), headers, "", null);
    }

    /**
     * Retrieves the generated inline mock implementation script for an API
     * (GET /apis/{id}/generate-mock-scripts). Ports getGenerateMockScript. Non-asserting.
     *
     * @param apiId context key holding the API id
     */
    @When("I retrieve the mock implementation script for API {string}")
    public void iRetrieveMockScript(String apiId) throws IOException {

        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.get(Utils.getGeneratedMockScriptsURL(Utils.getBaseUrl(), actualApiId), headers);
    }

    /**
     * Validates a system role via the publisher {@code GET /roles/{base64url(role)}} endpoint. Ports APIM638.
     * Non-asserting — the feature asserts 200 (existing role) or 404 (non-existing).
     *
     * @param role the role name (e.g. {@code admin}, {@code Internal/publisher})
     */
    @When("I validate the role {string}")
    public void iValidateRole(String role) throws IOException {
        Map<String, String> headers = new HashMap<>();
        // Legacy validates roles with a publisher token (api_create/publish/manage) → 200; the earlier 401 was
        // a padded-base64 path bug, not a scope issue (see Utils.getValidateRoleURL).
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.head(Utils.getValidateRoleURL(Utils.getBaseUrl(), role), headers);
    }

    /**
     * Force-changes a subscription's business plan via the publisher endpoint
     * (POST /subscriptions/change-business-plan?subscriptionId=&throttlingPolicy=). Unlike the devportal
     * subscription PUT (which silently ignores an invalid plan → 200), this endpoint validates the plan.
     * Ports ChangeSubscriptionBusinessPlanForcefullyTestCase. Non-asserting.
     *
     * @param subId context key holding the subscription id
     * @param plan  the throttling policy / business plan to set; may be a {@code {{contextKey}}} reference to a
     *              uniquely-named policy created by the scenario, as well as a built-in tier name
     */
    @When("I change the subscription business plan of {string} to {string}")
    public void iChangeSubscriptionBusinessPlan(String subId, String plan) throws IOException {
        String actualSubId = TestContext.resolve(subId).toString();
        plan = Utils.resolveContextPlaceholders(plan);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.post(Utils.getChangeSubscriptionBusinessPlanURL(Utils.getBaseUrl(), actualSubId, plan), headers, "", null);
    }

    /**
     * As {@link #iChangeSubscriptionBusinessPlan}, but takes the subscription id LITERALLY instead of as a
     * context key — the only way to exercise an id that no subscription has: an EMPTY string, or a syntactically
     * valid but nonexistent one. The sibling step resolves its argument through {@code TestContext.resolve},
     * which throws on an unknown key, so it cannot express these cases at all. Ports
     * ChangeSubscriptionBusinessPlanForcefullyTestCase#testUpdateSubscriptionBusinessPlanWithInvalidSubscriptionId.
     * Non-asserting.
     *
     * @param subscriptionId the literal subscription id to send (may be empty)
     * @param plan           the throttling policy / business plan to set
     */
    @When("I change the subscription business plan of subscription id {string} to {string}")
    public void iChangeSubscriptionBusinessPlanByLiteralId(String subscriptionId, String plan) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.post(Utils.getChangeSubscriptionBusinessPlanURL(Utils.getBaseUrl(), subscriptionId, plan),
                headers, "", null);
    }

    /**
     * Asserts that the PUBLISHER's view of a subscription carries the given business plan, by listing the API's
     * subscriptions ({@code GET /subscriptions?apiId=}) and matching on the subscription id. This is the read
     * that makes a successful force-change meaningful: the change-business-plan POST returning 200 says only
     * that the request was accepted, not that the plan was written — and the publisher plane is where the
     * legacy assertion looked. Ports the verification half of
     * ChangeSubscriptionBusinessPlanForcefullyTestCase#testUpdateSubscriptionBusinessPlanWithValidTiers.
     *
     * @param subIdKey     context key holding the subscription id
     * @param apiIdKey     context key holding the API id whose subscriptions are listed
     * @param expectedPlan the business plan the subscription must now carry
     */
    @Then("The publisher subscription {string} of API {string} should have business plan {string}")
    public void thePublisherSubscriptionShouldHaveBusinessPlan(String subIdKey, String apiIdKey,
                                                               String expectedPlan) throws IOException {
        String subscriptionId = TestContext.resolve(subIdKey).toString();
        String apiId = TestContext.resolve(apiIdKey).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = Requests.get(Utils.getSubscriptions(Utils.getBaseUrl(), apiId), headers);
        Assert.assertTrue(response != null && response.getResponseCode() == 200 && response.getData() != null
                        && !response.getData().isBlank(),
                "Failed to list publisher subscriptions of API '" + apiId + "': got "
                        + (response == null ? "no response"
                        : response.getResponseCode() + " / body=" + response.getData()));

        JSONArray subscriptions = new JSONObject(response.getData()).getJSONArray("list");
        String actualPlan = null;
        for (int i = 0; i < subscriptions.length(); i++) {
            JSONObject subscription = subscriptions.getJSONObject(i);
            if (subscriptionId.equals(subscription.optString("subscriptionId"))) {
                actualPlan = subscription.optString("throttlingPolicy");
                break;
            }
        }
        Assert.assertNotNull(actualPlan, "Subscription '" + subscriptionId + "' is not in the publisher's "
                + "subscription list for API '" + apiId + "'. Body: " + response.getData());
        Assert.assertEquals(actualPlan, expectedPlan,
                "Publisher subscription '" + subscriptionId + "' business plan mismatch. Body: "
                        + response.getData());
    }

    /**
     * Publishes a resource, changing its lifecycle state to "PUBLISHED".
     * A published resource becomes available in the Developer Portal for subscription.
     *
     * @param resourceType Type of resource to publish (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID to publish
     */
    @When("I publish the {string} resource with id {string}")
    public void iPublishTheResource(String resourceType, String resourceId) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());
        // The Publish lifecycle-change POST can transiently fail (or be briefly rejected while a just-completed
        // deploy settles) under parallel load on the shared container. This response used to be ignored, so a
        // failed publish was SWALLOWED: the API silently stayed in Created and surfaced later as a misleading
        // "did not reach Published" at the following lifecycle-status assertion. Retry the POST until it succeeds
        // (200) — or until the API is already Published, since a re-POST on an already-published API can fault —
        // catching only transient IOException, then assert. On success the final 200 is published as httpResponse
        // for any following "The response status code should be 200".
        String url = Utils.getChangeLifecycleURL(Utils.getBaseUrl(), resourceType, actualResourceId, "Publish", null);
        long endTimeStart = System.currentTimeMillis();
        long endTime = endTimeStart + Constants.RUNTIME_PROPAGATION_TIMEOUT;
        HttpResponse publishResponse = null;
        boolean published = false;
        while (true) {
            try {
                publishResponse = Requests.post(url, headers, null, null);
                if (publishResponse != null && publishResponse.getResponseCode() == 200) {
                    published = true;
                    break;
                }
            } catch (IOException transientFailure) {
                // transient — fall through to the state check / retry
            }
            // The POST may have applied despite a lost/failed response; treat an already-Published API as success.
            if ("Published".equals(currentApiLifecycleState(actualResourceId, headers))) {
                published = true;
                break;
            }
            if (System.currentTimeMillis() >= endTime) {
                break;
            }
            try {
                Utils.pollPause(endTimeStart, Constants.RETRY_INTERVAL_TIME);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Assert.assertTrue(published, "Publish lifecycle-change did not succeed for " + resourceType + " "
                + actualResourceId + " within the deadline; last response: "
                + (publishResponse == null ? "null"
                : publishResponse.getResponseCode() + " / " + publishResponse.getData()));
    }

    /**
     * Publishes the resource and then waits until the lifecycle state has actually reached {@code Published},
     * re-firing the Publish action if the transition was lost. The opt-in variant of
     * {@code I publish the "apis" resource with id}, which asserts only the POST's status.
     *
     * <p>A 200 from the lifecycle-change POST means no exception was thrown, not that the state moved, so this
     * gates on a read-back. It distinguishes four outcomes: the target state (done); the source state (lost —
     * re-POST); a pending {@code AM_API_STATE} approval task (fails, naming the task, since the state is waiting
     * on approval rather than on propagation); and an unexpected state, 401/403 or a rejected re-POST (fails
     * immediately). Every re-fire is logged.
     */
    @When("I publish the {string} resource with id {string}, healing if the transition is lost")
    public void iPublishTheResourceHealingLostTransition(String resourceType, String resourceId) throws Exception {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        String url = Utils.getChangeLifecycleURL(Utils.getBaseUrl(), resourceType, actualResourceId, "Publish", null);

        // Fire once up front so the healthy path is a single POST, exactly like the plain step.
        HttpResponse first = Requests.post(url, headers, null, null);
        if (first != null && (first.getResponseCode() == 401 || first.getResponseCode() == 403)) {
            Assert.fail("Publish of " + actualResourceId + " was rejected with " + first.getResponseCode()
                    + " — credentials/scope, not propagation: " + first.getData());
        }

        HealGate.awaitOrHeal("Published state of " + resourceType + " " + actualResourceId,
                () -> {
                    HttpResponse lc = SimpleHTTPClient.getInstance()
                            .doGet(Utils.getAPILifecycleStateURL(Utils.getBaseUrl(), actualResourceId), headers);
                    if (lc == null) {
                        return new HealGate.NotReady("no response from lifecycle-state");
                    }
                    int code = lc.getResponseCode();
                    if (code == 401 || code == 403) {
                        return new HealGate.Fatal("lifecycle-state read returned " + code
                                + " — credentials/scope, not propagation");
                    }
                    if (code >= 500) {
                        return new HealGate.Fatal("lifecycle-state read returned " + code
                                + " (already past the client's transient 900967 retry): " + lc.getData());
                    }
                    if (code != 200 || lc.getData() == null || lc.getData().isBlank()) {
                        return new HealGate.NotReady("HTTP " + code + " from lifecycle-state");
                    }
                    String state = new JSONObject(lc.getData()).optString("state", null);
                    if (APIConstants_PUBLISHED.equalsIgnoreCase(state)) {
                        return new HealGate.Ready();
                    }
                    if (!"Created".equalsIgnoreCase(state)) {
                        return new HealGate.Fatal("lifecycle state is '" + state + "', neither the source state "
                                + "nor Published — something else moved this API, so re-publishing is wrong");
                    }
                    String pending = pendingApiStateWorkflowReference(actualResourceId);
                    if (pending != null) {
                        return new HealGate.Fatal("a PENDING AM_API_STATE workflow task (" + pending + ") is "
                                + "blocking the transition: APIProviderImpl only changes the lifecycle once the "
                                + "workflow is APPROVED, so the 200 was a silent no-op. Not a lost event — approve "
                                + "or clear the task.");
                    }
                    return new HealGate.NotReady("state=Created");
                },
                attempt -> {
                    logger.warn("self-heal: re-POSTing Publish for {} {} — the previous 200 did not persist",
                            resourceType, actualResourceId);
                    HttpResponse again = SimpleHTTPClient.getInstance().doPost(url, headers, "",
                            Constants.CONTENT_TYPES.APPLICATION_JSON);
                    if (again != null && again.getResponseCode() == 400) {
                        return new HealGate.Fatal("re-POST of Publish returned 400 (action not allowed from the "
                                + "current state): " + again.getData());
                    }
                    return new HealGate.Ready();
                },
                3);
    }

    /** {@code Published} — inlined so this file needs no product-constant dependency. */
    private static final String APIConstants_PUBLISHED = "Published";

    /**
     * The {@code externalWorkflowReference} of a PENDING API-state workflow task for this API, or null. Read as
     * the acting actor's admin token; a non-200 (e.g. no admin scope) yields null so the caller keeps treating the
     * state as merely unpropagated rather than inventing a diagnosis.
     */
    private String pendingApiStateWorkflowReference(String apiId) {
        try {
            Object adminToken = TestContext.get(Identity.adminTokenKey(Identity.actingActor()));
            if (adminToken == null) {
                return null;
            }
            HttpResponse list = SimpleHTTPClient.getInstance().doGet(
                    Utils.getWorkflowsByTypeURL(Utils.getBaseUrl(), "AM_API_STATE"),
                    Identity.bearerHeaders(adminToken.toString()));
            if (list == null || list.getResponseCode() != 200 || list.getData() == null) {
                return null;
            }
            JSONArray tasks = new JSONObject(list.getData()).optJSONArray("list");
            for (int i = 0; tasks != null && i < tasks.length(); i++) {
                JSONObject task = tasks.getJSONObject(i);
                if (apiId.equals(task.optJSONObject("properties") == null ? null
                        : task.getJSONObject("properties").optString("apiId", null))
                        || (task.optString("description", "").contains(apiId))) {
                    return task.optString("externalWorkflowReference", "unknown-reference");
                }
            }
        } catch (Exception ignored) {
            // Diagnosis is best-effort: never turn a failed lookup into a misleading verdict.
        }
        return null;
    }

    /**
     * Reads an API's current lifecycle state (e.g. {@code Created}/{@code Published}) via a direct GET that is
     * NOT published as {@code httpResponse} — an intermediate read consumed locally by the publish retry loop.
     * Returns {@code null} on any non-2xx/empty/transient response so the caller keeps polling.
     */
    private String currentApiLifecycleState(String apiId, Map<String, String> headers) {
        try {
            HttpResponse response = SimpleHTTPClient.getInstance()
                    .doGet(Utils.getAPILifecycleStateURL(Utils.getBaseUrl(), apiId), headers);
            if (response != null && response.getResponseCode() == 200
                    && response.getData() != null && !response.getData().isBlank()) {
                return new JSONObject(response.getData()).optString("state", null);
            }
        } catch (IOException ignored) {
            // transient — caller retries
        }
        return null;
    }

    /**
     * Retrieves the details of a specific resource by its ID.
     *
     * @param resourceType Type of resource to retrieve (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID to retrieve
     */
    @When("I retrieve the {string} resource with id {string}")
    public void iRetrieveTheResource(String resourceType, String resourceId) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Requests.get(Utils.getResourceEndpointURL(Utils.getBaseUrl(), resourceType, actualResourceId), headers);
    }

    /**
     * Polls the Publisher resource-GET (publisher token) until the response body contains {@code marker},
     * publishing the LAST response for any following assertion. Asserts the marker was seen — use this instead
     * of asserting on a mutating request's own echo when the property under test must be DURABLY saved: under
     * load a PUT's 200 response has been observed echoing a stale (pre-update) representation, so only a fresh
     * read proves persistence (and a genuinely-lost update fails here with the read-back body in the message).
     *
     * <p>NOTE on the window: {@code timeoutSeconds} is a FLOOR-ed hint, not the effective deadline — it is raised
     * to {@link Constants#RUNTIME_PROPAGATION_TIMEOUT} when smaller (the same convention as the invoke-until
     * steps). Feature-file values were authored before the propagation tails were measured (~90-100s in CI, once
     * &gt;120s locally under full-suite load), so a literal 60s would sit below the observed tail and re-flake.
     * The constant stays the single tuning point; a passing poll returns on its first read regardless.
     */
    @When("I retrieve the {string} resource with id {string} until it contains {string} within {int} seconds")
    public void iRetrieveTheResourceUntilContains(String resourceType, String resourceId, String marker,
            int timeoutSeconds) throws InterruptedException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        String expected = Utils.resolveContextPlaceholders(marker);
        Map<String, String> headers = Identity.publisherHeaders();
        String url = Utils.getResourceEndpointURL(Utils.getBaseUrl(), resourceType, actualResourceId);
        long deadlineStart = System.currentTimeMillis();
        long deadline = deadlineStart + Math.max(timeoutSeconds * 1000L, Constants.RUNTIME_PROPAGATION_TIMEOUT);
        HttpResponse last = null;
        boolean found = false;
        while (System.currentTimeMillis() < deadline) {
            try {
                last = SimpleHTTPClient.getInstance().doGet(url, headers);
                if (last.getResponseCode() == 200 && last.getData() != null && last.getData().contains(expected)) {
                    found = true;
                    break;
                }
            } catch (IOException transientFailure) {
                // transient — keep polling; the deadline bounds a persistent failure
            }
            Utils.pollPause(deadlineStart, 2000);
        }
        TestContext.set("httpResponse", last);
        Assert.assertNotNull(last, "No publisher response received for " + resourceType + " " + actualResourceId);
        Assert.assertTrue(found, resourceType + " " + actualResourceId + " did not contain '" + expected
                + "' within the window; last: " + last.getData());
    }

    /**
     * Reconciles and re-deploys for the gate above: reaps the previously attempted revision (undeploy, then
     * delete) and deploys a freshly created one, returning {@link HealGate.Fatal} if the product rejects either
     * step outright.
     *
     * <p>A fresh revision rather than a re-POST of the same one, because a revision already recorded as deployed
     * accepts the re-POST without emitting a new deployment event. The stale one must go first: the product caps
     * revisions per API ({@code MAXIMUM_REVISIONS_REACHED}), so accumulating them would convert a propagation
     * flake into a hard failure. The new id replaces {@code revisionId} in context so a later heal reaps it.
     */
    private HealGate.Verdict reconcileAndRedeployRevision(String resourceType, String resourceId) {
        try {
            String staleRevision = TestContext.contains("revisionId")
                    ? TestContext.resolve("revisionId").toString() : null;
            Map<String, String> publisherHeaders = Identity.publisherHeaders();
            String deploymentPayload = "[{\"name\":\"" + System.getenv(Constants.GATEWAY_ENVIRONMENT)
                    + "\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]";

            if (staleRevision != null) {
                // Undeploy first: a deployed revision cannot be deleted. Both are best-effort.
                HttpResponse undeploy = SimpleHTTPClient.getInstance().doPost(
                        Utils.getRevisionUnDeploymentURL(Utils.getBaseUrl(), resourceType, resourceId,
                                staleRevision), publisherHeaders, deploymentPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
                HttpResponse delete = SimpleHTTPClient.getInstance().doDelete(
                        Utils.getRevisionByID(Utils.getBaseUrl(), resourceType, resourceId, staleRevision),
                        publisherHeaders);
                logger.warn("self-heal: reaped stale revision {} of {} (undeploy={}, delete={})", staleRevision,
                        resourceId, undeploy == null ? "no response" : undeploy.getResponseCode(),
                        delete == null ? "no response" : delete.getResponseCode());
            }

            HttpResponse created = SimpleHTTPClient.getInstance().doPost(
                    Utils.getRevisionURL(Utils.getBaseUrl(), resourceType, resourceId), publisherHeaders,
                    "{\"description\":\"self-heal revision\"}", Constants.CONTENT_TYPES.APPLICATION_JSON);
            if (created == null || created.getResponseCode() < 200 || created.getResponseCode() >= 300) {
                return new HealGate.Fatal("could not create a fresh revision to re-deploy: got="
                        + (created == null ? "null" : created.getResponseCode() + "/" + created.getData()));
            }
            String freshRevision = new JSONObject(created.getData()).getString("id");
            // No cleanup registration: a revision vanishes with its API (CLAUDE.md 5).
            TestContext.set("revisionId", freshRevision);

            HttpResponse deployed = SimpleHTTPClient.getInstance().doPost(
                    Utils.getRevisionDeploymentURL(Utils.getBaseUrl(), resourceType, resourceId, freshRevision),
                    publisherHeaders, deploymentPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);
            if (deployed == null || deployed.getResponseCode() < 200 || deployed.getResponseCode() >= 300) {
                return new HealGate.Fatal("could not deploy the fresh revision " + freshRevision + ": got="
                        + (deployed == null ? "null" : deployed.getResponseCode() + "/" + deployed.getData()));
            }
            logger.warn("self-heal: re-deployed {} {} as fresh revision {}", resourceType, resourceId,
                    freshRevision);
            return new HealGate.Ready();
        } catch (Exception e) {
            return new HealGate.Fatal("revision reconcile/redeploy threw: " + e);
        }
    }

    /**
     * Waits until the resource's synapse artifact is live on the gateway, re-deploying it if the deploy event was
     * lost. Fixture readiness, never an assertion target.
     *
     * <p>Classifies what it sees so a permanent condition is not mistaken for a slow one: a 404 or empty body is
     * "not deployed yet", while 401/403 and any 5xx fail immediately. On an exhausted window it reconciles and
     * re-deploys (see {@link #reconcileAndRedeployRevision}). All reads go through the raw client so the step's
     * published {@code httpResponse} is never clobbered.
     */
    @Then("the {string} resource {string} should be live on the gateway, redeploying if propagation is lost")
    public void resourceShouldBeLiveOnGateway(String resourceType, String resourceId) throws Exception {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        // Intermediate read: resolve the resource's name/version for the gateway artifact query.
        HttpResponse api = SimpleHTTPClient.getInstance().doGet(
                Utils.getResourceEndpointURL(Utils.getBaseUrl(), resourceType, actualResourceId),
                Identity.publisherHeaders());
        Assert.assertTrue(api != null && api.getResponseCode() == 200 && api.getData() != null
                        && !api.getData().isBlank(),
                "Could not read " + resourceType + " " + actualResourceId + " to resolve its gateway artifact: got="
                        + (api == null ? "null" : api.getResponseCode() + "/" + api.getData()));
        JSONObject dto = new JSONObject(api.getData());
        String name = dto.getString("name");
        String version = dto.getString("version");

        User tenantAdmin = Identity.actingTenantAdmin();
        String artifactUrl = Utils.getGatewayArtifactURL(Utils.getBaseUrl(), "api-artifact", name, version,
                tenantAdmin.getUserDomain());
        Map<String, String> gatewayAuth = Identity.basicAuthHeaders(tenantAdmin.getUserName(),
                tenantAdmin.getPassword());
        String revisionId = TestContext.resolve("revisionId").toString();
        String redeployPayload = "[{\"name\":\"" + System.getenv(Constants.GATEWAY_ENVIRONMENT)
                + "\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]";

        HealGate.awaitOrHeal("gateway deployment of " + resourceType + " " + name + " v" + version,
                () -> {
                    HttpResponse r = SimpleHTTPClient.getInstance().doGet(artifactUrl, gatewayAuth);
                    if (r == null) {
                        return new HealGate.NotReady("no response from the gateway artifact endpoint");
                    }
                    int code = r.getResponseCode();
                    if (code == 200 && r.getData() != null && !r.getData().isBlank()) {
                        return new HealGate.Ready();
                    }
                    // 401/403 can never become a 200, and any 5xx here already survived the client's
                    // transient 900967 retry, so neither is worth waiting out.
                    if (code == 401 || code == 403) {
                        return new HealGate.Fatal("gateway artifact endpoint returned " + code
                                + " for " + tenantAdmin.getUserName() + " — credentials/config, not propagation");
                    }
                    if (code >= 500) {
                        return new HealGate.Fatal("gateway artifact endpoint returned " + code
                                + " (not the transient 900967 the client already retries): " + r.getData());
                    }
                    return new HealGate.NotReady("HTTP " + code);
                },
                attempt -> reconcileAndRedeployRevision(resourceType, actualResourceId),
                3);
    }

    /**
     * Probes a backend endpoint URL via the Publisher endpoint-validation API and publishes the response for
     * assertion. A reachable endpoint validates with statusCode 202 (Accepted) in 4.7.0. The {@code endpointUrl}
     * may carry {{...}} placeholders; the API id is a context key.
     *
     * @param endpointUrl the backend endpoint URL to probe (placeholders resolved)
     * @param apiId       context key holding the API id the probe is associated with
     */
    @When("I validate the endpoint {string} for API {string}")
    public void iValidateEndpointForApi(String endpointUrl, String apiId) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        String resolvedEndpoint = Utils.resolveContextPlaceholders(endpointUrl);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.post(Utils.getValidateEndpointURL(Utils.getBaseUrl(), resolvedEndpoint, actualApiId), headers, "",
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Retrieves a list of all APIs created through the Publisher REST API.
     * This step performs a search query without filters to get all available APIs.
     */
    @When("I retrieve all APIs created through the Publisher REST API")
    public void iRetrieveAllApis() throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Requests.get(Utils.getAPISearchEndpointURL(Utils.getBaseUrl(), null, null, null), headers);
    }

    /**
     * Verifies that a specific API ID exists in the list of all APIs.
     *
     * <p>The publisher listing is served from the search index, which is NOT read-your-writes: a 201 from
     * {@code POST /apis} does not guarantee the API is indexed yet, so asserting on a single listing read is a
     * race (§7). It only looked stable because its one existing caller (api_lifecycle) reaches this step after
     * an update + publish + lifecycle poll, i.e. behind an incidental delay. So the assertion is funnelled
     * through a retry that RE-ISSUES the listing — retrying the parse of the response an earlier step already
     * fetched could never change its verdict.
     *
     * <p>The response already in context is checked FIRST and short-circuits on a hit, so a caller that is
     * already indexed by the time it gets here pays no added latency and its behaviour is unchanged; only the
     * previously-failing path polls. The assertion itself is untouched: still presence-by-id, still exact.
     *
     * @param apiId Context key containing the API ID to verify
     */
    @Then("The API with id {string} should be in the list of all APIS")
    public void theApiShouldBeInTheListOfAllApis(String apiId) throws IOException, InterruptedException {

        String actualApiId = TestContext.resolve(apiId).toString();
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        // Guard before parsing — a cleared/failed list retrieval must fail clearly, not as an NPE/JSONException.
        // Deliberately NOT retried: an absent response means the feature never ran the retrieve step, which is an
        // authoring error that must fail fast rather than be reported as a propagation timeout (§7).
        Assert.assertTrue(response != null && response.getData() != null && !response.getData().isBlank(),
                "No API-list response with a body captured to search for API '" + actualApiId + "' in");

        if (listContainsApiId(response, actualApiId)) {
            return;
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        String listUrl = Utils.getAPISearchEndpointURL(Utils.getBaseUrl(), null, null, null);
        HttpResponse lastResponse = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                () -> Requests.get(listUrl, headers),
                listResponse -> listContainsApiId(listResponse, actualApiId));

        // Carry the last body into the failure text: without it, diagnosing "not found" costs another full run.
        Assert.assertTrue(listContainsApiId(lastResponse, actualApiId),
                "API with id " + actualApiId + " not found in the list within the retry window; last response: "
                        + (lastResponse == null ? "none (requests failed)"
                        : lastResponse.getResponseCode() + " / " + lastResponse.getData()));
    }

    /**
     * True when the given publisher-listing response carries an entry whose {@code id} is {@code apiId}. Tolerates
     * a null/empty/non-2xx response by reporting "not present", so a listing read during warm-up keeps the poll in
     * {@link #theApiShouldBeInTheListOfAllApis} going instead of throwing out of the accept condition.
     */
    private static boolean listContainsApiId(HttpResponse response, String apiId) {
        if (response == null || response.getResponseCode() < 200 || response.getResponseCode() >= 300
                || response.getData() == null || response.getData().isBlank()) {
            return false;
        }
        try {
            JSONObject payload = new JSONObject(response.getData());
            if (!payload.has("list")) {
                return false;
            }
            JSONArray apisList = payload.getJSONArray("list");
            return IntStream.range(0, apisList.length())
                    .mapToObj(apisList::getJSONObject)
                    .anyMatch(subJson -> apiId.equals(subJson.optString("id", null)));
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * Step definition: Verifies that the lifecycle status of an API matches the expected status.
     *
     * @param apiId Context key containing the API ID to check
     * @param status Expected lifecycle status (e.g., "PUBLISHED", "CREATED")
     */
    @Then("The lifecycle status of API {string} should be {string}")
    public void theLifecycleStatusShouldBe(String apiId, String status) throws IOException {

        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        // Lifecycle changes (publish/deploy) propagate asynchronously, so the state can lag a publish call —
        // poll until it reaches the expected value rather than asserting on a single GET (the latter is a
        // flaky race that wider parallel load on the shared container exposes). The window is
        // RUNTIME_PROPAGATION_TIMEOUT: under full-suite load in CI the container's async pipelines have been
        // observed running ~90-100s behind a change-lifecycle 200, so the poll must ride out that backlog
        // like the publish retry above does.
        String url = Utils.getAPILifecycleStateURL(Utils.getBaseUrl(), actualApiId);
        long endTimeStart = System.currentTimeMillis();
        long endTime = endTimeStart + Constants.RUNTIME_PROPAGATION_TIMEOUT;
        HttpResponse lifecycleStatusResponse = null;
        String actualState = null;
        while (true) {
            try {
                lifecycleStatusResponse = Requests.get(url, headers);
            } catch (IOException transientFailure) {
                // transient — keep polling; the deadline bounds a persistent failure (§7 retry-loop rule)
            }
            // Only parse a 200 that actually has a body; a non-2xx/empty response during warm-up falls through
            // and we keep polling rather than throwing an uncaught JSONException.
            if (lifecycleStatusResponse != null && lifecycleStatusResponse.getResponseCode() == 200
                    && lifecycleStatusResponse.getData() != null && !lifecycleStatusResponse.getData().isBlank()) {
                actualState = new JSONObject(lifecycleStatusResponse.getData()).optString("state", null);
                if (status.equals(actualState)) {
                    return;
                }
            }
            if (System.currentTimeMillis() >= endTime) {
                break;
            }
            try {
                Utils.pollPause(endTimeStart, Constants.RETRY_INTERVAL_TIME);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Assert.assertEquals(actualState, status,
                "API lifecycle state did not reach '" + status + "' within the retry window; last response: "
                        + (lifecycleStatusResponse == null ? "none (requests failed)"
                        : lifecycleStatusResponse.getResponseCode() + " / " + lifecycleStatusResponse.getData()));
    }

    /**
     * Asserts that the API's current lifecycle state (GET /apis/{id}/lifecycle-state) offers exactly the given
     * set of available transition events — no more, no fewer. The expected events are supplied as a
     * comma-separated list (each a LifecycleStateAvailableTransitionsDTO {@code event}, e.g. "Block,Deprecate").
     * Ports the available-transitions-per-state assertions of RegistryLifeCycleInclusionTest. Because the legacy
     * only checked containment, this is a stricter set-equality check that also catches unexpected extra
     * transitions.
     */
    @Then("The available lifecycle transitions of API {string} should be exactly {string}")
    public void theAvailableTransitionsShouldBeExactly(String apiId, String expectedEventsCsv) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = Requests.get(Utils.getAPILifecycleStateURL(Utils.getBaseUrl(), actualApiId), headers);
        Assert.assertTrue(response != null && response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isBlank(),
                "lifecycle-state fetch failed for api=" + actualApiId + " got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        JSONArray transitions = new JSONObject(response.getData()).optJSONArray("availableTransitions");
        Set<String> actual = new HashSet<>();
        if (transitions != null) {
            for (int i = 0; i < transitions.length(); i++) {
                actual.add(transitions.getJSONObject(i).getString("event"));
            }
        }
        Set<String> expected = new HashSet<>();
        for (String e : expectedEventsCsv.split(",")) {
            expected.add(e.trim());
        }
        // Cardinality before the set compare (§12): transition events are distinct by nature, so a repeated
        // entry is a defect -- and a Set would collapse it and still match.
        Assert.assertEquals(transitions == null ? 0 : transitions.length(), expected.size(),
                "Available lifecycle transitions count mismatch for api=" + actualApiId + " (got " + actual + ")");
        Assert.assertEquals(actual, expected,
                "Available lifecycle transitions mismatch for api=" + actualApiId + " (got " + actual + ")");
    }

    /**
     * Asserts that the API's lifecycle history / audit-trail (GET /apis/{id}/lifecycle-history) records a
     * transition from {@code previousState} to {@code postState} (matched case-insensitively on the DTO's
     * {@code previousState}/{@code postState}). Ports the lifecycle-history assertions of
     * RegistryLifeCycleInclusionTest (CREATED→PUBLISHED, PUBLISHED→BLOCKED, BLOCKED→DEPRECATED).
     */
    @Then("The lifecycle history of API {string} should record a transition from {string} to {string}")
    public void theLifecycleHistoryShouldRecord(String apiId, String previousState, String postState)
            throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = Requests.get(Utils.getAPILifecycleHistoryURL(Utils.getBaseUrl(), actualApiId), headers);
        Assert.assertTrue(response != null && response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isBlank(),
                "lifecycle-history fetch failed for api=" + actualApiId + " got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        JSONArray list = new JSONObject(response.getData()).optJSONArray("list");
        boolean found = false;
        if (list != null) {
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.getJSONObject(i);
                if (previousState.equalsIgnoreCase(item.optString("previousState"))
                        && postState.equalsIgnoreCase(item.optString("postState"))) {
                    found = true;
                    break;
                }
            }
        }
        Assert.assertTrue(found, "Lifecycle history did not record transition " + previousState + "->" + postState
                + " for api=" + actualApiId + " (history=" + response.getData() + ")");
    }

    /**
     * Composite step that creates an API, creates a revision, and deploys it.
     * This step combines multiple operations of creating and deploying an API
     *
     * @param payloadPath Path to the JSON file containing the API creation payload
     * @param apiID Context key where the created API ID will be stored
     */
    @Given("I have created an api from {string} as {string} and deployed it")
    public void iHaveCreatedAnApiFromAsAndDeployedIt(String payloadPath, String apiID) throws IOException, InterruptedException {

        baseSteps.putJsonPayloadFromFile(payloadPath, "<createApiPayload>");
        iCreateAnAPIWithPayloadAs("apis","<createApiPayload>", apiID);
        baseSteps.putJsonPayloadInContext("<createRevisionPayload>","{\"description\":\"Initial Revision\"}");
        iCreateResourceRevision("apis",apiID, "<createRevisionPayload>");
        baseSteps.putJsonPayloadInContext("<deployRevisionPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        iDeployApiRevisionGivenPayload("<revisionId>", "apis",apiID, "<deployRevisionPayload>");
        baseSteps.theResponseStatusCodeShouldBe(201);
    }

    /**
     * Composite step that deploys a revision using a default deployment payload.
     * This step simplifies revision deployment by using a standard deployment configuration.
     *
     * @param revisionID Context key containing the revision ID to deploy
     * @param resourceType Type of resource (e.g., "apis", "api-products")
     * @param resourceID Context key containing the resource ID
     */
    @When("I deploy revision {string} of {string} resource {string}")
    public void iDeployRevision(String revisionID, String resourceType, String resourceID) throws IOException {
        baseSteps.putJsonPayloadInContext("<deployRevisionPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        iDeployApiRevisionGivenPayload(revisionID, resourceType, resourceID, "<deployRevisionPayload>");
    }

    /**
     * Composite step that creates a new revision and deploys the API.
     * This step combines revision creation and deployment into a single operation.
     *
     * @param apiID Context key containing the API ID to deploy
     */
    @Given("I deploy the API with id {string}")
    public void iDeployAPI(String apiID) throws IOException, InterruptedException{
        iDeployResource("apis", apiID);
    }

    /**
     * Resource-typed deploy (create a revision + deploy it to the gateway env) — the general form of
     * {@link #iDeployAPI(String)} for other deployable resource types, e.g. {@code "mcp-servers"}. The revision
     * and deploy-revision endpoints are path-typed by resourceType, so the same payloads apply.
     */
    @Given("I deploy the {string} resource with id {string}")
    public void iDeployResource(String resourceType, String resourceID) throws IOException, InterruptedException {
        baseSteps.putJsonPayloadInContext("<createRevisionPayload>","{\"description\":\"new Revision\"}");
        iCreateResourceRevision(resourceType, resourceID , "<createRevisionPayload>");
        baseSteps.putJsonPayloadInContext("<deployRevisionPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        iDeployApiRevisionGivenPayload("<revisionId>", resourceType ,resourceID, "<deployRevisionPayload>");
    }

    /**
     * Waits until a specific revision is deployed, by polling the MANAGEMENT plane's deployment-status endpoint
     * until the revision appears in the deployed list.
     *
     * <p><b>This is a management-plane check ONLY — it is NOT a gateway readiness gate.</b> It reports when the
     * revision ROW is written, which is not when synapse has hot-swapped the running sequence, so an invocation
     * issued straight after it can still be served by the OLD artifact. It previously papered over that with a
     * blind {@code Thread.sleep(10000)}; that violated CLAUDE.md §4 ("wait, never sleep"), was simultaneously too
     * short under CI load (it let mediation_policies.feature:374 read the pre-detach response) and pure waste
     * everywhere else, and — worst — stood as a copyable template for the next author. It is gone.
     *
     * <p>If the following step ASSERTS on gateway behaviour, gate it on the DATA plane instead, so the condition
     * is false in the old state and true in the new one: {@code ... until response body contains "<marker>"} for
     * an effect that should appear, {@code ... until response body no longer contains "<marker>"} for one that
     * should disappear, or a status poll to a code the old state cannot return. Use this step only to fail fast
     * with a clear message when a deploy never landed at all.
     *
     * @param resourceType Type of resource (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID
     */
    @Then("I wait until {string} {string} revision is deployed in the gateway")
    public void waitUntilRevisionIsDeployed(String resourceType, String resourceId) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        String revisionId = TestContext.resolve("revisionId").toString();

        String url = Utils.getRevisionDeployments(Utils.getBaseUrl(), resourceType, actualResourceId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        long endTimeStart = System.currentTimeMillis();
        long endTime = endTimeStart + Constants.RUNTIME_PROPAGATION_TIMEOUT;
        boolean deployed = false;

        while (System.currentTimeMillis() < endTime) {

            try {
                HttpResponse response = Requests.get(url, headers);

                if (response != null && response.getResponseCode() == 200) {
                    JSONObject responseJson = new JSONObject(response.getData());
                    JSONArray revisions = responseJson.getJSONArray("list");

                    for (int i = 0; i < revisions.length(); i++) {
                        JSONObject revision = revisions.getJSONObject(i);
                        String deployedRevisionId =
                                revision.optString("id");

                        if (revisionId.equals(deployedRevisionId)) {
                            deployed = true;
                            logger.info("Revision {} is deployed for API {}", revisionId, actualResourceId);
                            break;
                        }
                    }
                }

                if (deployed) {
                    break;
                }

            } catch (IOException | JSONException e) {
                logger.debug("Revision {} not deployed yet – retrying", revisionId
                );
            }

            try {
                Utils.pollPause(endTimeStart, 1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Assert.assertTrue(deployed, "Revision " + revisionId + " was not deployed within the timeout");
    }

    /**
     * Creates a new version of an existing API or API Product.
     *
     * @param newVersion The new version string (e.g., "2.0.0")
     * @param resourceType Type of resource (e.g., "apis", "api-products")
     * @param resourceID Context key containing the existing resource ID
     * @param isDefault Whether the new version should be set as the default version ("true" or "false")
     * @param newVersionID Context key where the new version's resource ID will be stored
     */
    @When("I create a new version {string} of {string} resource {string} with default version {string} as {string}")
    public void iCreateANewVersionOfAPI(String newVersion, String resourceType, String resourceID, String isDefault, String newVersionID) throws IOException{

        String actualResourceID = TestContext.resolve(resourceID).toString();
        Boolean defaultVersion = false;
        if (isDefault != null && !isDefault.isEmpty()) {
            defaultVersion = Boolean.parseBoolean(isDefault);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse apiNewVersionResponse = Requests.post(Utils.getNewAPIVersionURL(Utils.getBaseUrl(), resourceType, newVersion, defaultVersion, actualResourceID), headers, null, null);
        Object newVersionId = Utils.extractValueFromPayload(apiNewVersionResponse.getData(), "id");
        TestContext.set(newVersionID, newVersionId);
        // Register for scenario teardown so the version copy is cleaned up alongside the base API.
        ResourceCleanup.register(cleanupListFor(resourceType), newVersionId);
    }

    /**
     * Attempts to create a new version without asserting success, storing the raw response as
     * {@code httpResponse} for the feature to assert. For negative / access-control scenarios where the
     * version-create is expected to be rejected (e.g. a subscriber-role token receiving 401): unlike the
     * positive step it extracts no id (the error body has none), but a new version is a top-level resource, so
     * an unexpectedly-created one IS registered for teardown (§5) — a no-op on the expected rejection.
     */
    @When("I attempt to create a new version {string} of {string} resource {string} with default version {string}")
    public void iAttemptToCreateANewVersionOfAPI(String newVersion, String resourceType, String resourceID,
                                                 String isDefault) throws IOException {

        String actualResourceID = TestContext.resolve(resourceID).toString();
        boolean defaultVersion = isDefault != null && !isDefault.isBlank() && Boolean.parseBoolean(isDefault);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        ResourceCleanup.registerIfCreated(cleanupListFor(resourceType),
                Requests.post(Utils.getNewAPIVersionURL(Utils.getBaseUrl(), resourceType, newVersion,
                        defaultVersion, actualResourceID), headers, null, null), "id");
    }

    /**
     * Prepares a document payload template by loading a base template file
     * and replacing placeholders with the provided values.
     *
     * @param type Document type (e.g., "HOWTO", "SAMPLES")
     * @param sourceType Source type (e.g., "INLINE", "URL", "FILE")
     * @param inlineContent The inline content for the document (used when sourceType is "INLINE")
     */
    @When("I prepare a new document payload with type {string}, sourceType {string}, and inlineContent {string}")
    public void iPrepareANewDocumentPayloadWithTypeSourceTypeAndInlineContent(String type, String sourceType, String inlineContent) throws IOException {

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("artifacts/payloads/add_new_document_api.json")) {
            if (inputStream == null) {
                throw new FileNotFoundException("File not found on classpath: " + "artifacts/payloads/add_new_document_api.json");
            }
            String jsonPayload = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            jsonPayload = jsonPayload.replace("<type>", type)
                    .replace("<sourceType>", sourceType)
                    .replace("<inlineContent>", inlineContent);
            TestContext.set(Utils.normalizeContextKey("<newDocumentPayload>"), jsonPayload);
        }
    }

    /**
     * Adds a document to an API using the prepared document payload.
     *
     * @param apiID Context key containing the API ID to which the document will be added
     */
    @And("I add the document to API {string}")
    public void iAddTheDocumentToAPI(String apiID) throws IOException{

        String jsonPayload = TestContext.resolve("<newDocumentPayload>").toString();
        String actualApiId = TestContext.resolve(apiID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse documentCreationResponse = Requests.post(Utils.getAPIDocuments(Utils.getBaseUrl(), actualApiId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("documentID", Utils.extractValueFromPayload(documentCreationResponse.getData(), "documentId"));
    }

    /**
     * Retrieves all documents associated with an API.
     *
     * @param apiID Context key containing the API ID
     */
    @When("I retrieve all available documents for {string}")
    public void iRetrieveAllAvailableDocumentsFor(String apiID) throws IOException{

        String actualApiId = TestContext.resolve(apiID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Requests.get(Utils.getAPIDocuments(Utils.getBaseUrl(), actualApiId), headers);
    }

    /**
     * Retrieves a specific document by its ID for a given API.
     *
     * @param documentID Context key containing the document ID to retrieve
     * @param apiID Context key containing the API ID
     */
    @When("I retrieve document with {string} for {string}")
    public void iRetrieveDocumentWithFor(String documentID, String apiID) throws IOException{

        String documentId = TestContext.resolve(documentID).toString();
        String actualApiId = TestContext.resolve(apiID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Requests.get(Utils.getAPIDocument(Utils.getBaseUrl(), actualApiId, documentId), headers);

    }

    /**
     * Deletes a document from an API.
     *
     * @param documentID Context key containing the document ID to delete
     * @param apiID Context key containing the API ID
     */
    @When("I delete the document with {string} for {string}")
    public void iDeleteTheDocumentWithFor(String documentID, String apiID) throws IOException{

        String documentId = TestContext.resolve(documentID).toString();
        String actualApiId = TestContext.resolve(apiID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Requests.delete(Utils.getAPIDocument(Utils.getBaseUrl(), actualApiId, documentId), headers);
    }

    /**
     * Updates a document using the payload stored in the test context.
     *
     * @param documentID Context key containing the document ID to update
     * @param apiID Context key containing the API ID
     */
    @And("I update the document with {string} for API {string}")
    public void iUpdateTheDocumentWithForAPI(String documentID, String apiID) throws IOException {

        String jsonPayload = TestContext.resolve("<newDocumentPayload>").toString();
        String actualApiId = TestContext.resolve(apiID).toString();
        String documentId = TestContext.resolve(documentID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Requests.put(Utils.getAPIDocument(Utils.getBaseUrl(), actualApiId, documentId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Prepares a document payload for any doc type / source, built programmatically so the source-specific field
     * is set correctly: INLINE/MARKDOWN → {@code inlineContent}, URL → {@code sourceUrl}, FILE → no content
     * field (uploaded separately). An OTHER type sets {@code otherTypeName}. The name is resolved for
     * {@code ${UNIQUE:...}} so several documents can be added to one API without name collisions.
     */
    @When("I prepare a document named {string} of type {string} with sourceType {string} and content {string}")
    public void iPrepareDocumentOfTypeAndSource(String name, String type, String sourceType, String content) {

        JSONObject doc = new JSONObject();
        doc.put("name", Utils.resolvePayloadPlaceholders(name));
        doc.put("type", type);
        doc.put("summary", "Summary of test Documentation");
        doc.put("sourceType", sourceType);
        doc.put("visibility", "API_LEVEL");
        if ("URL".equals(sourceType)) {
            doc.put("sourceUrl", content);
        } else if ("INLINE".equals(sourceType) || "MARKDOWN".equals(sourceType)) {
            doc.put("inlineContent", content);
        }
        if ("OTHER".equals(type)) {
            doc.put("otherTypeName", "CustomDocType");
        }
        TestContext.set(Utils.normalizeContextKey("<newDocumentPayload>"), doc.toString());
    }

    /**
     * Posts inline text as the content of an INLINE-source document (multipart, form field {@code inlineContent}).
     * The document must already exist (created with sourceType INLINE). The {@code inlineContent} field on the
     * document-create payload sets metadata only — the retrievable content served by {@code /documents/{id}/content}
     * must be posted here separately (verified live: an INLINE doc created with only the create-payload
     * inlineContent 404s on the content endpoint until this POST). Content resolves {@code {{...}}} placeholders so
     * a scenario-unique searchable word can be planted for content search.
     *
     * @param content    the inline document body (placeholders resolved)
     * @param documentID context key holding the document id
     * @param apiID      context key holding the API id
     */
    @When("I add inline content {string} to document {string} of API {string}")
    public void iAddInlineDocumentContent(String content, String documentID, String apiID) throws IOException {

        String docId = TestContext.resolve(documentID).toString();
        String actualApiId = TestContext.resolve(apiID).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, String> formFields = new HashMap<>();
        formFields.put("inlineContent", Utils.resolveContextPlaceholders(content));

        Requests.postMultipart(
                Utils.getAPIDocumentContent(Utils.getBaseUrl(), actualApiId, docId), headers,
                new HashMap<>(), formFields);
    }

    /**
     * Uploads a file as the content of a FILE-source document (multipart, form field {@code file}). The document
     * must already exist (created with sourceType FILE via the add step).
     */
    @When("I upload the document file {string} for document {string} of API {string}")
    public void iUploadDocumentFile(String resourcePath, String documentID, String apiID) throws IOException {

        String docId = TestContext.resolve(documentID).toString();
        String actualApiId = TestContext.resolve(apiID).toString();

        File temp;
        String suffix = resourcePath.substring(resourcePath.lastIndexOf('.'));
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }
            temp = File.createTempFile("doc-content", suffix);
            temp.deleteOnExit();
            Files.copy(inputStream, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("file", temp);

        Requests.postMultipart(
                Utils.getAPIDocumentContent(Utils.getBaseUrl(), actualApiId, docId), headers, files, new HashMap<>());
    }

    /**
     * Requests publisher document content with the API id and document id taken as LITERALS — deliberately not
     * resolved through {@link TestContext}, because the whole point is to send an injected/tampered value that is
     * not a real id (e.g. {@code ;alert(1)}). Ports DocAPIParameterTamperingTest: the endpoint must reject the
     * tampered path rather than reflecting it back or leaking a stack trace. Non-asserting; the feature pins the
     * exact status and the absence of any stack trace in the body.
     *
     * @param tamperedApiId the literal value to place in the {@code apiId} path segment
     * @param documentId    the literal value to place in the {@code documentId} path segment
     */
    @When("I attempt to retrieve publisher document content with tampered API id {string} and document id {string}")
    public void iAttemptToRetrieveTamperedDocumentContent(String tamperedApiId, String documentId)
            throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.get(Utils.getAPIDocumentContent(Utils.getBaseUrl(), tamperedApiId, documentId), headers);
    }

    // Helper to parse the values correctly for update document steps
    private Object parseConfigValue(String value) {
        value = value.trim();

        try {
            if (value.startsWith("{")) {
                return new JSONObject(value);
            } else if (value.startsWith("[")) {
                return new JSONArray(value);
            }
        } catch (Exception ignored) {}

        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}

        return value;
    }

    /**
     * Updates a specific configuration field of a resource with a new value.
     * This step retrieves the existing resource payload, updates the specified configuration field,
     * and then performs the update operation. Supports various data types including JSON objects and arrays.
     *
     * @param resourceType Type of resource to update (e.g., "apis", "api-products")
     * @param resourceID Context key containing the resource ID to update
     * @param resourceUpdatePayload Context key containing the existing resource payload
     * @param configType The configuration field name to update (e.g., "endpointConfig")
     * @param configValue The new value for the configuration field (can be JSON, boolean, number, or string)
     */
    @When("I update the {string} resource {string} and {string} with configuration type {string} and value:")
    public void iUpdateTheResourceWithConfigurationTypeAndValue(String resourceType, String resourceID, String resourceUpdatePayload, String configType, String configValue) throws IOException, InterruptedException {

        // Retrieve a JSON object safely
        Object ctxValue = TestContext.resolve(resourceUpdatePayload);
        JSONObject jsonPayload = (ctxValue instanceof JSONObject)
                ? (JSONObject) ctxValue
                : new JSONObject(ctxValue.toString());

        if ("endpointConfig".equals(configType)){
            // The value is a context key whose stored endpointConfig JSON may itself embed {{contextKey}}
            // references to runtime-captured values (e.g. a DCR-registered clientId/clientSecret for OAUTH
            // endpoint security), so resolve those after fetching the stored payload.
            configValue = Utils.resolveContextPlaceholders(TestContext.resolve(configValue).toString());
        } else {
            // Resolve any {{contextKey}} placeholders in the value (e.g. a custom throttle-tier name captured
            // into context, when setting an API's business-plan "policies"). No-op when the value has none.
            configValue = Utils.resolveContextPlaceholders(configValue);
        }
        Object parsedValue = parseConfigValue(configValue);

        // update or overwrite the payload
        jsonPayload.put(configType, parsedValue);
        String updatedJsonPayload = jsonPayload.toString();
        TestContext.set(Utils.normalizeContextKey("<apiConfigUpdate>"), updatedJsonPayload);

        iUpdateResourceWithJsonPayloadFromContext(resourceType, resourceID, "<apiConfigUpdate>");
        Thread.sleep(3000);
    }

    /**
     * Blocks a subscription, preventing it from being used for API invocation.
     *
     * @param subscriptionID Context key containing the subscription ID to block
     */
    @When("I block the subscription with {string} for the resource")
    public void iBlockTheSubscriptionWithForTheResource(String subscriptionID) throws IOException {

        String subscriptionId = TestContext.resolve(subscriptionID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Requests.post(Utils.getSubscriptionBlockingURL(Utils.getBaseUrl(), subscriptionId), headers, null, null);
    }

    /**
     * Unblocks a previously blocked subscription, allowing it to be used for API invocation again.
     *
     * @param subscriptionID Context key containing the subscription ID to unblock
     */
    @When("I unblock the subscription with {string} for the resource")
    public void iUnblockTheSubscriptionWithForTheResource(String subscriptionID) throws IOException {

        String subscriptionId = TestContext.resolve(subscriptionID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Requests.post(Utils.getSubscriptionUnBlockingURL(Utils.getBaseUrl(), subscriptionId), headers, null, null);
    }

    /**
     * Creates a new shared scope in APIM.
     * Shared scopes can be used across multiple APIs to define common authorization scopes.
     * The scope ID is stored in the test context after creation.
     *
     * <p>The supplied name is a BASE: it is uniquified through {@link Names#unique} before the scope is
     * created, because a shared scope is tenant-wide and a literal would 409 a re-run on the same container
     * whenever teardown could not remove it (a hard scenario failure), and would collide outright if two
     * features ever picked the same literal. Capture the created name from the response —
     * {@code I extract response field "name" and store it as "..."} — and reference that key wherever the
     * scope is bound or requested; the base alone will not match. Mirrors the
     * {@code ... bound to role} variant in JwtGrantSteps, which already works this way.
     *
     * @param scopeName base name of the shared scope to create
     */
    @When("I create a new shared scope as {string}")
    public void iCreateANewSharedScopeAs(String scopeName) throws IOException{

        scopeName = Names.unique(Utils.resolveContextPlaceholders(scopeName));
        // Create payload
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("artifacts/payloads/create_apim_shared_scope_payload.json")) {
            if (inputStream == null) {
                throw new FileNotFoundException("File not found on classpath: " + "artifacts/payloads/create_apim_shared_scope_payload.json");
            }
            String jsonPayload = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            jsonPayload = jsonPayload.replace("<name>", scopeName);

            TestContext.set(Utils.normalizeContextKey("<newSharedScope>"), jsonPayload);
        }

        String jsonPayload = TestContext.resolve("<newSharedScope>").toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse scopeCreationResponse = Requests.post(Utils.getAPIScopes(Utils.getBaseUrl()), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        Object scopeId = Utils.extractValueFromPayload(scopeCreationResponse.getData(), "id");
        TestContext.set("scopeID", scopeId);
        // Register for teardown: a shared scope is a tenant-wide resource that ResourceCleanup must remove so
        // it does not leak (and 409 a re-run on the same container) if the scenario fails before deleting it.
        if (scopeId != null) {
            ResourceCleanup.register(Constants.CREATED_SHARED_SCOPE_IDS, scopeId);
        }
      }

    /**
     * Creates a shared scope bound to a freshly-generated, unique role name and stores the IS7 role name that
     * the WSO2-IS-7 connector will derive from it. Used to exercise runtime role creation: when the scope is
     * registered, APIM calls the IS7 KM's registerScope, which (with enable_roles_creation=true) creates the
     * bound role in IS via the SCIM2 Roles API. The connector maps a plain APIM role {@code r} to the IS role
     * {@code system_primary_r} (an {@code Internal/r} role maps to {@code r}); we bind a plain role, so the
     * expected IS role name is {@code system_primary_<role>}, stored under {@code isRoleKey} for the verify step.
     * Note: APIProviderImpl.addSharedScope SWALLOWS a KM registerScope failure (logs, does not throw), so the
     * scope create still returns 201 regardless - role creation must be verified directly in IS, not via this
     * response.
     */
    @When("I create a shared scope bound to a new IS7 role, storing the expected IS7 role name as {string}")
    public void iCreateSharedScopeBoundToNewIs7Role(String isRoleKey) throws Exception {

        // KM-propagation loop: the key manager these features register (a feature-level admin step, not block
        // infra) reaches addSharedScope's registerScope fan-out ASYNCHRONOUSLY (eventhub -> in-memory KM
        // holder). A scope created before propagation gets 201 but NO IS role - and never will (the fan-out is
        // synchronous within the create), so waiting on the role alone cannot converge. Instead: create a
        // scope, briefly probe IS for the derived role, and if absent DELETE the scope and recreate with fresh
        // unique names until the fan-out lands (bounded). Mirrors the invoke-until-200 polls that absorb the
        // same propagation at the gateway.
        long deadlineStart = System.currentTimeMillis();
        long deadline = deadlineStart + 60_000;
        int attempts = 0;
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        while (true) {
            attempts++;
            String scopeName = Names.unique("is7scope");
            String apimRole = Names.unique("is7role");
            // The IS7 connector derives the IS role name from the (plain) APIM role: system_primary_<role>.
            String expectedIsRole = "system_primary_" + apimRole;
            String jsonPayload = "{\"name\":\"" + scopeName + "\",\"displayName\":\"" + scopeName
                    + "\",\"description\":\"IS7 runtime role-creation test scope\",\"bindings\":[\""
                    + apimRole + "\"]}";
            // Requests.post publishes httpResponse; the LAST (kept) create is what the feature's following
            // status assertion reads, matching the pre-loop contract.
            HttpResponse response;
            try {
                response = Requests.post(Utils.getAPIScopes(Utils.getBaseUrl()), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
            } catch (IOException transientFailure) {
                // Outcome UNKNOWN — the create may have committed with the response lost, and the next
                // attempt uses FRESH names, so an orphan would never be swept. Register any survivor for
                // teardown, then fall through to the retryable branch below (deadline-bounded).
                String orphanId = Utils.findIdByNameInListResponse(Utils.getAPIScopes(Utils.getBaseUrl()), headers,
                        scopeName, "id");
                if (orphanId != null) {
                    ResourceCleanup.register(Constants.CREATED_SHARED_SCOPE_IDS, orphanId);
                }
                response = null;
            }
            // A non-2xx create is RETRYABLE within the deadline, not fatal: the fan-out also races KM
            // DELETION propagation - a KM another runner just REST-deleted (e.g. the keygen-negatives'
            // unreachable KM) can linger in the in-memory holder for a moment, and registerScope fanning out
            // to it fails the whole create with a 500 until the holder catches up.
            if (response == null || response.getResponseCode() < 200 || response.getResponseCode() >= 300
                    || response.getData() == null || response.getData().isBlank()) {
                Assert.assertFalse(System.currentTimeMillis() > deadline,
                        "Shared scope create failed until the deadline: got=" + (response == null ? "null"
                                : response.getResponseCode() + "/" + response.getData()));
                Utils.pollPause(deadlineStart, 2000);   // mutating probe: each retry creates/deletes a scope
                continue;
            }
            Object scopeId = Utils.extractValueFromPayload(response.getData(), "id");
            // Register for teardown IMMEDIATELY: the role probe below and this loop's sleeps can all throw,
            // and a created-but-unregistered scope would leak silently. A pre-propagation attempt that is
            // successfully deleted below is deregistered again, so the sweep never chases an already-gone id.
            if (scopeId != null) {
                ResourceCleanup.register(Constants.CREATED_SHARED_SCOPE_IDS, scopeId);
            }
            // Short per-attempt probe: if the KM was propagated, the role exists as of the 201 (registerScope
            // is synchronous inside the create); the brief re-checks only absorb IS-side latency.
            boolean roleCreated = false;
            for (int probe = 0; probe < 3 && !roleCreated; probe++) {
                if (probe > 0) {
                    Utils.pollPause(deadlineStart, 2000);   // mutating probe: each retry creates/deletes a scope
                }
                roleCreated = ApplicationBaseSteps.is7RoleExists(expectedIsRole);
            }
            if (roleCreated) {
                TestContext.set("scopeID", scopeId);
                TestContext.set(Utils.normalizeContextKey(isRoleKey), expectedIsRole);
                // Expose the scope name so a scope-protected API can require it (role-based authorization flow).
                TestContext.set("is7ScopeName", scopeName);
                return;
            }
            // Pre-propagation attempt: this scope will never get its role. Remove it and retry with fresh
            // names. Intermediate delete goes through the raw client (not Requests.*) so it cannot clobber
            // the published httpResponse; only a SUCCESSFUL delete deregisters — a failed or throwing delete
            // leaves the id registered for the teardown sweep.
            if (scopeId != null) {
                HttpResponse del = SimpleHTTPClient.getInstance()
                        .doDelete(Utils.getAPIScopes(Utils.getBaseUrl()) + "/" + scopeId, headers);
                if (del != null && del.getResponseCode() >= 200 && del.getResponseCode() < 300) {
                    ResourceCleanup.deregister(Constants.CREATED_SHARED_SCOPE_IDS, scopeId);
                }
            }
            if (System.currentTimeMillis() > deadline) {
                Assert.fail("Key-manager scope fan-out never created the derived IS role within 60s ("
                        + attempts + " create attempts) - the registered KM did not propagate to the scope-"
                        + "registration path");
            }
            Utils.pollPause(deadlineStart, 2000);   // mutating probe: each retry creates/deletes a scope
        }
    }

    /**
     * Creates, revisions and deploys an API whose GET operation requires an already-created SHARED scope (name in
     * context under {@code is7ScopeName}), storing the new API id under {@code apiIdKey}. The payload is built in
     * code (not from a fixture) because the API-create path resolves only {@code ${...}} payload placeholders, not
     * {@code {{context}}} ones, so the runtime scope name must be substituted here. A shared scope is used
     * deliberately: an API-local scope bound to a role is rejected (903250 "Role does not exist") unless the role
     * pre-exists in APIM, whereas a shared scope referenced with {@code shared:true} carries its binding and needs
     * no role re-validation. Drives the IS7 role-based authorization flow.
     */
    @Given("I create and deploy a scope-protected API requiring the shared scope as {string}")
    public void iCreateScopeProtectedApi(String apiIdKey) throws IOException, InterruptedException {

        String scopeName = TestContext.resolve("is7ScopeName").toString();
        String name = Names.unique("APIMScopedTest");
        String context = Names.unique("apiscopedctx");
        String backend = "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/";

        JSONObject prod = new JSONObject().put("url", backend).put("config", JSONObject.NULL)
                .put("template_not_supported", false);
        JSONObject api = new JSONObject()
                .put("name", name)
                .put("context", context)
                .put("version", "1.0.0")
                .put("description", "Scope-protected API for IS7 role-based authorization")
                .put("endpointConfig", new JSONObject()
                        .put("endpoint_type", "http")
                        .put("production_endpoints", prod)
                        .put("sandbox_endpoints", new JSONObject(prod.toString())))
                .put("policies", new JSONArray().put("Unlimited"))
                .put("scopes", new JSONArray().put(new JSONObject()
                        .put("scope", new JSONObject().put("name", scopeName).put("displayName", scopeName))
                        .put("shared", true)))
                .put("operations", new JSONArray().put(new JSONObject()
                        .put("verb", "GET").put("target", "/customers/{id}")
                        .put("scopes", new JSONArray().put(scopeName))))
                .put("isDefaultVersion", true);

        TestContext.set(Utils.normalizeContextKey("<scopedApiPayload>"), api.toString());
        iCreateAnAPIWithPayloadAs("apis", "<scopedApiPayload>", apiIdKey);
        baseSteps.putJsonPayloadInContext("<scopedApiRevision>", "{\"description\":\"Initial Revision\"}");
        iCreateResourceRevision("apis", apiIdKey, "<scopedApiRevision>");
        baseSteps.putJsonPayloadInContext("<scopedApiDeploy>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        iDeployApiRevisionGivenPayload("<revisionId>", "apis", apiIdKey, "<scopedApiDeploy>");
        baseSteps.theResponseStatusCodeShouldBe(201);
    }

    /**
     * Attempts to create a shared scope without asserting success, storing the raw response as
     * {@code httpResponse} for the feature to assert. For negative / access-control scenarios where the
     * create is expected to be rejected (e.g. a subscriber-role token receiving 401): unlike the positive
     * step it asserts no status and stores no id under a caller-named key.
     *
     * <p>An unexpectedly-created scope IS still registered — {@code registerIfCreated} enqueues the id only on a
     * 2xx, so a refusal is a no-op while a create that unexpectedly succeeds is swept rather than leaked (§5).</p>
     */
    @When("I attempt to create a shared scope as {string}")
    public void iAttemptToCreateASharedScopeAs(String scopeName) throws IOException {

        String jsonPayload;
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("artifacts/payloads/create_apim_shared_scope_payload.json")) {
            if (inputStream == null) {
                throw new FileNotFoundException(
                        "File not found on classpath: artifacts/payloads/create_apim_shared_scope_payload.json");
            }
            jsonPayload = IOUtils.toString(inputStream, StandardCharsets.UTF_8).replace("<name>", scopeName);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        // Callers expect a refusal; an unexpected success still creates a real resource, so it is swept (§5).
        ResourceCleanup.registerIfCreated(Constants.CREATED_SHARED_SCOPE_IDS,
                Requests.post(Utils.getAPIScopes(Utils.getBaseUrl()), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON), "id");
    }

    /**
     * Deletes a shared scope by its ID.
     *
     * @param scopeID Context key containing the scope ID to delete
     */
    @When("I delete shared scope with {string}")
    public void iDeleteSharedScopeWith(String scopeID) throws IOException {

        String scopeId = TestContext.resolve(scopeID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Requests.delete(Utils.getAPIScopesById(Utils.getBaseUrl(), scopeId), headers);
    }

    /**
     * Updates a shared scope's description. Fetches the current scope DTO by id, mutates its {@code
     * description}, and PUTs it back (the update API needs the full DTO — this mirrors the legacy
     * get-then-update flow). {@code scopeIdKey} is a context key holding the scope id (e.g. {@code scopeID}).
     *
     * @param scopeIdKey     context key holding the scope id
     * @param newDescription the new description to set
     */
    @When("I update the shared scope {string} setting its description to {string}")
    public void iUpdateSharedScopeDescription(String scopeIdKey, String newDescription) throws IOException {

        String scopeId = TestContext.resolve(scopeIdKey).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse current = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPIScopesById(Utils.getBaseUrl(), scopeId), headers);
        // Confirm the GET succeeded with a body BEFORE parsing/mutating — otherwise new JSONObject(null/"") throws
        // an opaque JSONException/NPE instead of a clear failure.
        Assert.assertTrue(current != null && current.getResponseCode() >= 200 && current.getResponseCode() < 300
                        && current.getData() != null && !current.getData().isBlank(),
                "Failed to fetch shared scope '" + scopeId + "' before update: expected a 2xx response with a body, got "
                        + (current == null ? "no response" : current.getResponseCode() + " / body="
                        + current.getData()));
        JSONObject scope = new JSONObject(current.getData());
        scope.put("description", newDescription);

        Requests.put(Utils.getAPIScopesById(Utils.getBaseUrl(), scopeId), headers, scope.toString(),
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Searches for a shared scope by name and stores its ID in the test context.
     *
     * @param scopeName Name of the shared scope to search for
     * @param scopeId Context key where the found scope ID will be stored
     */
    @When("I fetch the shared scope with name {string} into context as {string}")
    public void fetchSharedScopeByName(String scopeName, String scopeId) throws IOException {

        // Resolved: the creating step uniquifies the base name, so callers pass the captured {{key}}, not a literal.
        scopeName = Utils.resolveContextPlaceholders(scopeName);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse response = Requests.get(Utils.getAPIScopes(Utils.getBaseUrl()), headers);

        // Confirm the GET succeeded with a body BEFORE parsing — otherwise new JSONObject(null/"") throws an
        // opaque JSONException/NPE instead of a clear failure.
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isBlank(),
                "Failed to list shared scopes while searching for '" + scopeName + "': expected a 2xx response with a "
                        + "body, got " + (response == null ? "no response" : response.getResponseCode() + " / body="
                        + response.getData()));

        // --- Parse JSON to extract scope id ---
        JSONObject json = new JSONObject(response.getData());
        JSONArray list = json.getJSONArray("list");

        String foundedScopeId = null;
        for (int i = 0; i < list.length(); i++) {
            JSONObject scope = list.getJSONObject(i);
            if (scope.getString("name").equals(scopeName)) {
                foundedScopeId = scope.getString("id");
                break;
            }
        }

        if (foundedScopeId == null) {
            throw new RuntimeException("Scope name not found: " + scopeName);
        }
        TestContext.set(scopeId, foundedScopeId);
    }

    /**
     * Creates a GraphQL API by uploading a GraphQL schema file along with additional properties.
     * This step handles the multipart file upload required for GraphQL API creation.
     *
     * @param schemaFilePath Path to the GraphQL schema file (.graphql) in the classpath resources
     * @param additionalPropertiesKey Context key containing the additional properties JSON payload
     * @param apiID Context key where the created API ID will be stored
     */
    @When("I create a GraphQL API with schema file {string} and additional properties {string} as {string}")
    public void iCreateAGraphQLAPIWithSchemaFileAndAdditionalPropertiesAs(String schemaFilePath, String additionalPropertiesKey, String apiID) throws IOException {
        // Load GraphQL schema file from resources
        File schemaFile;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(schemaFilePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("GraphQL schema file not found: " + schemaFilePath);
            }

            // Create temporary file object
            schemaFile = File.createTempFile("graphql-schema", ".graphql");
            schemaFile.deleteOnExit();
            Files.copy(inputStream, schemaFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        String additionalProperties = TestContext.resolve(additionalPropertiesKey).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Map<String, String> formFields = new HashMap<>();
        formFields.put("type", "GRAPHQL");
        formFields.put("additionalProperties", additionalProperties);

        Map<String, File> files = new HashMap<>();
        files.put("file", schemaFile);

        HttpResponse apiCreateResponse = Requests.postMultipart(Utils.getGraphQLSchema(Utils.getBaseUrl()), headers,
                files, formFields);
        Assert.assertEquals(apiCreateResponse.getResponseCode(), 201, apiCreateResponse.getData());
        Object createdId = Utils.extractValueFromPayload(apiCreateResponse.getData(), "id");
        TestContext.set(apiID, createdId);
        // Register for scenario teardown so a shared-server suite does not accumulate APIs across scenarios.
        ResourceCleanup.register(Constants.CREATED_API_IDS, createdId);
    }

    /**
     * NON-ASSERTING counterpart of {@link #iCreateAGraphQLAPIWithSchemaFileAndAdditionalPropertiesAs} — imports a
     * GraphQL schema and publishes the raw response so the FEATURE asserts the status. The positive step above
     * asserts 201 internally and so cannot express a rejection (§12); this is the {@code I attempt to …} variant
     * the GraphQL schema-import negatives need (e.g. a malformed context, or a subscriber lacking the scope).
     *
     * <p>Callers expect a refusal, but an UNEXPECTED success still creates a real API, so the id is registered for
     * teardown (§5). Without it a regression that started accepting these payloads would leak one API per run into
     * the shared container, where the residue collides on duplicate names and fails unrelated scenarios.</p>
     *
     * @param schemaFilePath          classpath path of the GraphQL schema to import
     * @param additionalPropertiesKey context key holding the additionalProperties JSON
     */
    @When("I attempt to create a GraphQL API with schema file {string} and additional properties {string}")
    public void iAttemptToCreateAGraphQLAPIWithSchemaFile(String schemaFilePath, String additionalPropertiesKey)
            throws IOException {

        File schemaFile = Utils.classpathToTempFile(schemaFilePath, "graphql-schema", ".graphql");
        String additionalProperties = TestContext.resolve(additionalPropertiesKey).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Map<String, String> formFields = new HashMap<>();
        formFields.put("type", "GRAPHQL");
        formFields.put("additionalProperties", additionalProperties);

        Map<String, File> files = new HashMap<>();
        files.put("file", schemaFile);

        // Callers expect a refusal; an unexpected success still creates a real resource, so it is swept (§5).
        ResourceCleanup.registerIfCreated(Constants.CREATED_API_IDS,
                Requests.postMultipart(Utils.getGraphQLSchema(Utils.getBaseUrl()), headers, files, formFields), "id");
    }

    /**
     * Creates a GraphQL API from an ENDPOINT URL (import-graphql-schema with a {@code url} form field instead of a
     * schema file) — the gateway derives the schema from the URL (introspection of a live endpoint, or fetching an
     * SDL served at that URL). Ports GraphqlTestCase's "create using endpoint" / SDL-URL paths. Non-asserting on the
     * create status so the feature can assert it; stores the id on 2xx.
     *
     * @param endpointUrl the GraphQL endpoint (introspection) or SDL URL, reachable from the gateway
     * @param additionalPropertiesKey context key holding the additionalProperties JSON
     * @param apiID context key to store the created API id under
     */
    @When("I create a GraphQL API from endpoint URL {string} with additional properties {string} as {string}")
    public void iCreateAGraphQLAPIFromEndpointURL(String endpointUrl, String additionalPropertiesKey, String apiID)
            throws IOException {
        String url = Utils.resolveContextPlaceholders(endpointUrl);
        String additionalProperties = TestContext.resolve(additionalPropertiesKey).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Map<String, String> formFields = new HashMap<>();
        formFields.put("type", "GRAPHQL");
        formFields.put("url", url);
        formFields.put("additionalProperties", additionalProperties);

        HttpResponse response = Requests.postMultipart(Utils.getGraphQLSchema(Utils.getBaseUrl()), headers,
                new HashMap<>(), formFields);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
            TestContext.set(apiID, createdId);
            ResourceCleanup.register(Constants.CREATED_API_IDS, createdId);
        }
    }

    /**
     * Validates a GraphQL schema obtained from an endpoint URL — via INTROSPECTION of a live endpoint
     * ({@code useIntrospection=true}) or by fetching an SDL at the URL ({@code false}) — and stores the derived
     * SDL (from {@code graphQLInfo.graphQLSchema.schemaDefinition}) under {@code schemaKey}. Ports the
     * validateGraphqlSchemaDefinitionByURL step of GraphqlTestCase (the create-using-endpoint / SDL-URL paths).
     */
    @When("I validate the GraphQL schema from endpoint URL {string} with introspection {string} and store schema as {string}")
    public void iValidateGraphQLSchemaFromURL(String endpointUrl, String useIntrospection, String schemaKey)
            throws IOException {
        String url = Utils.resolveContextPlaceholders(endpointUrl);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, String> formFields = new HashMap<>();
        formFields.put("url", url);

        String validateUrl = Utils.getValidateGraphQLSchemaURL(Utils.getBaseUrl()) + "?useIntrospection=" + useIntrospection;
        HttpResponse response = Requests.postMultipart(validateUrl, headers, new HashMap<>(), formFields);
        // Confirm the validate call succeeded with a body BEFORE parsing — otherwise the graphQLInfo drill-down
        // throws an opaque JSONException/NPE instead of a clear failure.
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isBlank(),
                "Failed to validate the GraphQL schema from '" + url + "': expected a 2xx response with a body, got "
                        + (response == null ? "no response" : response.getResponseCode() + " / body=" + response.getData()));
        String sdl = new JSONObject(response.getData())
                .getJSONObject("graphQLInfo").getJSONObject("graphQLSchema").getString("schemaDefinition");
        TestContext.set(Utils.normalizeContextKey(schemaKey), sdl);
    }

    /**
     * Creates a GraphQL API from a schema STRING held in context (e.g. an SDL derived by introspecting an
     * endpoint) — import-graphql-schema with the schema uploaded as a file. Non-asserting; stores the id on 2xx.
     */
    @When("I create a GraphQL API with schema {string} and additional properties {string} as {string}")
    public void iCreateAGraphQLAPIWithSchemaStringAs(String schemaKey, String additionalPropertiesKey, String apiID)
            throws IOException {
        String schema = TestContext.resolve(schemaKey).toString();
        String additionalProperties = TestContext.resolve(additionalPropertiesKey).toString();

        File schemaFile = File.createTempFile("graphql-derived", ".graphql");
        schemaFile.deleteOnExit();
        Files.writeString(schemaFile.toPath(), schema);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, String> formFields = new HashMap<>();
        formFields.put("type", "GRAPHQL");
        formFields.put("additionalProperties", additionalProperties);
        Map<String, File> files = new HashMap<>();
        files.put("file", schemaFile);

        HttpResponse response = Requests.postMultipart(Utils.getGraphQLSchema(Utils.getBaseUrl()), headers, files,
                formFields);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
            TestContext.set(apiID, createdId);
            ResourceCleanup.register(Constants.CREATED_API_IDS, createdId);
        }
    }

    /**
     * Asserts an API appears EXACTLY ONCE, by id, in the PUBLISHER or the DEVPORTAL listing — i.e. that publishing
     * actually made the API discoverable on that plane, which a create/publish status code does not show. Written
     * as ONE step over a {@code plane} argument rather than two near-duplicates: the only differences are the
     * collection URL and which actor's token reads it.
     *
     * <p>Reads the UNFILTERED paged collection, not the {@code ?query=<name>} search: that search form goes through
     * the artifact index and answers {@code total:0} for a just-published API, so a membership check written
     * against it would assert nothing (the same trap {@code ApiProductSteps} documents for products). Visibility is
     * eventually consistent, so the listing is polled to the shared propagation ceiling; the count is asserted
     * AFTER the loop so a persistently missing (or DUPLICATED) API fails this step rather than a later one.</p>
     *
     * @param plane    {@code publisher} or {@code devportal}
     * @param apiIdKey context key holding the API id
     */
    @Then("The {string} listing should report API {string} exactly once")
    public void theListingShouldReportApiExactlyOnce(String plane, String apiIdKey) throws Exception {

        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        boolean publisherPlane = "publisher".equalsIgnoreCase(plane);
        if (publisherPlane) {
            headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        } else if ("devportal".equalsIgnoreCase(plane)) {
            headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        } else {
            throw new IllegalArgumentException("Unknown listing plane '" + plane
                    + "'; expected \"publisher\" or \"devportal\"");
        }

        // EVERY page, not just the first: with more APIs in the tenant than one page holds, a single-page read
        // reports 0 for an API sitting later in the collection and the retry then times out as a false failure.
        // The count is only meaningful across the whole collection, which is also what "exactly once" claims.
        Integer occurrences = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                () -> countAcrossAllPages(publisherPlane, headers, apiId),
                total -> total != null && total == 1);
        Assert.assertNotNull(occurrences, "The " + plane + " API listing could not be read at all");
        Assert.assertEquals(occurrences.intValue(), 1,
                "API " + apiId + " should appear exactly once across the whole " + plane + " listing, but was "
                        + "found " + occurrences + " time(s)");
    }

    /**
     * Walks the whole listing, page by page, and totals the entries carrying {@code apiId}. Returns null when a page
     * is unreadable so the caller's retry keeps waiting. Paging stops when a page returns fewer entries than the page
     * size, or once {@code pagination.total} has been consumed — the shape the listing actually returns
     * ({@code "pagination":{"offset":N,"limit":N,"total":N,...}}), verified against a captured body.
     */
    private static Integer countAcrossAllPages(boolean publisherPlane, Map<String, String> headers, String apiId)
            throws IOException {
        int found = 0;
        int offset = 0;
        while (true) {
            String pageUrl = publisherPlane
                    ? Utils.getAPISearchEndpointURL(Utils.getBaseUrl(), null, API_LISTING_PAGE_SIZE, offset)
                    : Utils.getDevportalApiListURL(Utils.getBaseUrl(), API_LISTING_PAGE_SIZE, offset);
            HttpResponse page = Requests.get(pageUrl, headers);
            if (page == null || page.getResponseCode() != 200
                    || page.getData() == null || page.getData().isBlank()) {
                return null;
            }
            found += countApiOccurrences(page, apiId);
            int onThisPage;
            int total;
            try {
                JSONObject body = new JSONObject(page.getData());
                JSONArray list = body.optJSONArray("list");
                onThisPage = list == null ? 0 : list.length();
                JSONObject pagination = body.optJSONObject("pagination");
                total = pagination == null ? -1 : pagination.optInt("total", -1);
            } catch (org.json.JSONException malformedDuringWarmup) {
                return null;
            }
            offset += onThisPage;
            // Last page: short page, an empty page, or the reported total consumed. The offset guard also stops a
            // listing whose total never shrinks from spinning forever.
            if (onThisPage == 0 || onThisPage < API_LISTING_PAGE_SIZE || (total >= 0 && offset >= total)) {
                return found;
            }
        }
    }

    /** Page size for the unfiltered API listings read by {@link #theListingShouldReportApiExactlyOnce}. */
    private static final int API_LISTING_PAGE_SIZE = 200;

    /**
     * How many entries of a {@code {"list":[…]}} API listing carry {@code apiId} as their {@code id}. Returns 0 for
     * a non-2xx/unparseable body so the caller's poll simply keeps waiting instead of throwing mid-loop.
     */
    private static int countApiOccurrences(HttpResponse response, String apiId) {
        if (response == null || response.getResponseCode() != 200
                || response.getData() == null || response.getData().isBlank()) {
            return 0;
        }
        try {
            JSONArray list = new JSONObject(response.getData()).optJSONArray("list");
            if (list == null) {
                return 0;
            }
            int found = 0;
            for (int i = 0; i < list.length(); i++) {
                if (apiId.equals(list.getJSONObject(i).optString("id"))) {
                    found++;
                }
            }
            return found;
        } catch (JSONException notJsonYet) {
            return 0;
        }
    }

    /** Retrieves a GraphQL API's schema definition (publisher), storing the raw response for assertions. */
    @When("I retrieve the GraphQL schema of API {string}")
    public void iRetrieveGraphQLSchemaOfApi(String apiIdKey) throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Requests.get(Utils.getGraphQLSchemaOfApiURL(Utils.getBaseUrl(), apiId), headers);
    }

    /** Updates a GraphQL API's schema definition (publisher, PUT multipart {@code schemaDefinition}). */
    @When("I update the GraphQL schema of API {string} with schema file {string}")
    public void iUpdateGraphQLSchemaOfApi(String apiIdKey, String schemaFilePath) throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Map<String, File> files = new HashMap<>();
        files.put("schemaDefinition", Utils.classpathToTempFile(schemaFilePath, "fixture", ".graphql"));

        Requests.putMultipart(Utils.getGraphQLSchemaOfApiURL(Utils.getBaseUrl(), apiId), headers,
                files, new HashMap<>());
    }

    /** Validates a GraphQL schema file (publisher, POST multipart {@code file}), storing the raw response. */
    @When("I validate the GraphQL schema file {string}")
    public void iValidateGraphQLSchemaFile(String schemaFilePath) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Map<String, File> files = new HashMap<>();
        files.put("file", Utils.classpathToTempFile(schemaFilePath, "fixture", ".graphql"));

        Requests.postMultipart(Utils.getValidateGraphQLSchemaURL(Utils.getBaseUrl()), headers,
                files, new HashMap<>());
    }

    /**
     * Asserts the {@code schemaDefinition} of the current response equals the WHOLE uploaded GraphQL schema file —
     * the assertion legacy {@code GraphqlTestCase.testRetrieveSchemaDefinitionAtPublisher} actually made, and which
     * a {@code The response should contain "languages"} substring check cannot make: a substring passes even if the
     * server truncated, reordered or dropped every other type in the schema.
     *
     * <p>Compared EXACTLY — measured: the server stores and returns the SDL byte-for-byte, so it does not
     * re-indent. A whitespace-normalising compare would additionally accept a schema collapsed onto one line.</p>
     *
     * @param schemaFilePath classpath path of the schema that was uploaded
     */
    @Then("The GraphQL schema definition in the response should equal the schema file {string}")
    public void theGraphQLSchemaShouldEqualFile(String schemaFilePath) throws IOException {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isBlank(),
                "Expected a 2xx response with a body carrying the GraphQL schema, but got: "
                        + (response == null ? "null" : response.getResponseCode() + " / " + response.getData()));
        Object retrieved = Utils.extractValueFromPayload(response.getData(), "schemaDefinition");
        Assert.assertNotNull(retrieved, "Response carries no 'schemaDefinition' field: " + response.getData());
        Assert.assertEquals(String.valueOf(retrieved), Utils.readClasspathResource(schemaFilePath),
                "The retrieved GraphQL schema does not equal the uploaded definition '" + schemaFilePath + "'.");
    }

    /**
     * Retrieves a single shared scope by id (GET /scopes/{id}), so the feature can assert the scope's stored
     * fields — name, displayName and the role {@code bindings}. The by-NAME step above publishes the scope LIST
     * response, in which a bindings assertion could be satisfied by a DIFFERENT scope's bindings; this one puts
     * exactly one scope's DTO under assertion.
     *
     * @param scopeIdKey context key holding the scope id
     */
    @When("I retrieve the shared scope with id {string}")
    public void iRetrieveSharedScopeById(String scopeIdKey) throws IOException {

        String scopeId = TestContext.resolve(scopeIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.get(Utils.getAPIScopesById(Utils.getBaseUrl(), scopeId), headers);
    }

    /**
     * Creates a new common (shared) operation policy.
     * Common policies can be reused across multiple APIs.
     *
     * @param synapsePolicyJ2 Path to the synapse policy definition file (.j2) in classpath resources
     * @param policySpecYaml Path to the policy specification YAML file in classpath resources
     * @param policyId Context key where the created policy ID will be stored
     */
    @And("I create a new common policy with spec {string} and {string} as {string}")
    public void iCreateANewCommonPolicyWithSpecAndSynapse(String synapsePolicyJ2, String policySpecYaml, String policyId) throws IOException {
        iCreateANewPolicyWithSpecAndSynapse(null, synapsePolicyJ2, policySpecYaml, policyId);
    }

    /**
     * Creates a new API-specific operation policy.
     * API-specific policies are scoped to a single API and cannot be reused.
     *
     * @param apiId Context key containing the API ID for which the policy is being created
     * @param synapsePolicyJ2 Path to the synapse policy definition file (.j2) in classpath resources
     * @param policySpecYaml Path to the policy specification YAML file in classpath resources
     * @param policyId Context key where the created policy ID will be stored
     */
    @And("I create a new API specific policy for api {string} with spec {string} and {string} as {string}")
    public void iCreateANewAPISpecificPolicyWithSpecAndSynapse(String apiId, String synapsePolicyJ2, String policySpecYaml, String policyId) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        iCreateANewPolicyWithSpecAndSynapse(actualApiId, synapsePolicyJ2, policySpecYaml, policyId);
    }

    /**
     * Internal method to create either a common policy or API-specific policy
     *
     * @param apiId If null, creates a common policy. If provided, creates an API-specific policy.
     * @param synapsePolicyJ2 Path to the synapse policy definition file (.j2)
     * @param policySpecYaml Path to the policy specification YAML file
     * @param policyId Context key to store the created policy ID
     */
    private void iCreateANewPolicyWithSpecAndSynapse(String apiId, String synapsePolicyJ2, String policySpecYaml, String policyId) throws IOException {
        // Extract original filenames
        String yamlFileName = policySpecYaml.substring(policySpecYaml.lastIndexOf('/') + 1);
        String j2FileName = synapsePolicyJ2.substring(synapsePolicyJ2.lastIndexOf('/') + 1);

        // Load policy spec YAML file
        File policySpecFile;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(policySpecYaml)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Policy spec YAML file not found: " + policySpecYaml);
            }
            // Create temp file with original filename
            policySpecFile = File.createTempFile("policy-spec-", "-" + yamlFileName);
            policySpecFile.deleteOnExit();
            Files.copy(inputStream, policySpecFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // Load synapse policy definition file (.j2)
        File synapsePolicyDefinitionFile;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(synapsePolicyJ2)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Synapse policy file not found: " + synapsePolicyJ2);
            }
            synapsePolicyDefinitionFile = File.createTempFile("synapse-policy-", "-" + j2FileName);
            synapsePolicyDefinitionFile.deleteOnExit();
            Files.copy(inputStream, synapsePolicyDefinitionFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Map<String, File> files = new HashMap<>();
        files.put("policySpecFile", policySpecFile);
        files.put("synapsePolicyDefinitionFile", synapsePolicyDefinitionFile);

        String endpointUrl;
        if (apiId == null || apiId.isEmpty()) {
            // Common policy
            endpointUrl = Utils.getCommonPolicy(Utils.getBaseUrl());
        } else {
            // API-specific policy
            endpointUrl = Utils.getAPISpecificPolicy(Utils.getBaseUrl(), apiId);
        }

        HttpResponse policyCreateResponse = Requests.postMultipart(endpointUrl, headers, files, null);

        // Extract and store the policy ID from response if available
        if (policyId != null && policyCreateResponse.getResponseCode() == 201) {
            String responseData = policyCreateResponse.getData();
            if (responseData != null && !responseData.isBlank()) {
                try {
                    JSONObject responseJson = new JSONObject(responseData);
                    if (responseJson.has("id")) {
                        String createdId = responseJson.getString("id");
                        TestContext.set(policyId, createdId);
                        // Register common (reusable) policies for the runner's teardown sweep. API-specific
                        // policies are tied to their API and removed when the API is deleted, so only the
                        // tenant-global common policies need explicit cleanup registration here.
                        if (apiId == null || apiId.isEmpty()) {
                            ResourceCleanup.register(Constants.CREATED_OPERATION_POLICY_IDS, createdId);
                        }
                    }
                } catch (Exception e) {
                    // Ignore if policy ID extraction fails
                }
            }
        }
    }

    /**
     * Retrieves all available common (shared) operation policies.
     */
    @When("I retrieve available common policies")
    public void iRetrieveCommonPolicies() throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Requests.get(Utils.getCommonPolicy(Utils.getBaseUrl()), headers);
    }

    /**
     * Exports a common operation policy by name/version/format (GET /operation-policies/export, returns a zip) and
     * stores the downloaded archive path under the given key. Asserts 200. First half of the common-policy
     * export/import round-trip (OperationPolicyTestCase). Binary download so the zip is not corrupted.
     */
    @When("I export the common operation policy named {string} version {string} format {string} as {string}")
    public void iExportCommonPolicy(String name, String version, String format, String archivePathKey)
            throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        SimpleHTTPClient.DownloadResult result = Requests.getToFile(
                Utils.getCommonPolicyExportURL(Utils.getBaseUrl(), name, version, format), headers, ".zip");
        Assert.assertEquals(result.getStatusCode(), 200,
                "Common operation policy export did not return 200 (archive download failed)");
        TestContext.set(Utils.normalizeContextKey(archivePathKey), result.getFile().getAbsolutePath());
    }

    /**
     * Attempts to export a common operation policy that does not exist, asserting the expected status (404). A
     * binary download whose status is checked directly (nothing published to httpResponse).
     */
    @When("I export a non-existing common operation policy named {string} version {string} format {string} expecting status {int}")
    public void iExportNonExistingCommonPolicy(String name, String version, String format, int expectedStatus)
            throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        SimpleHTTPClient.DownloadResult result = Requests.getToFile(
                Utils.getCommonPolicyExportURL(Utils.getBaseUrl(), name, version, format), headers, ".zip");
        Assert.assertEquals(result.getStatusCode(), expectedStatus,
                "Non-existing common operation policy export status mismatch");
    }

    /**
     * Deletes a common operation policy by id (DELETE /operation-policies/{id}) and drops it from the teardown
     * sweep (the test removed it itself, so a later sweep-delete would log a spurious 404). Publishes the response.
     */
    @When("I delete the common operation policy {string}")
    public void iDeleteCommonPolicy(String policyIdKey) throws IOException {
        String policyId = TestContext.resolve(policyIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = Requests.delete(Utils.getCommonPolicyById(Utils.getBaseUrl(), policyId), headers);
        // Drop it from the teardown sweep ONLY when it is actually gone (2xx) or already absent (404). A failed
        // delete (401/409/500/...) leaves the still-existing policy TRACKED so the AfterClass sweep can retry it.
        int code = response == null ? -1 : response.getResponseCode();
        if ((code >= 200 && code < 300) || code == 404) {
            ResourceCleanup.deregister(Constants.CREATED_OPERATION_POLICY_IDS, policyId);
        }
    }

    /**
     * Imports a common operation policy from a previously-exported archive (multipart field "file"). Stores the
     * created policy id (if the 201 body carries one) and registers it for teardown. Second half of the round-trip.
     */
    @When("I import the common operation policy archive {string} as {string}")
    public void iImportCommonPolicy(String archivePathKey, String policyIdKey) throws IOException {
        String path = TestContext.resolve(archivePathKey).toString();
        File archiveFile = new File(path);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("file", archiveFile);
        HttpResponse response = Requests.postMultipart(Utils.getCommonPolicyImportURL(Utils.getBaseUrl()), headers, files,
                null);
        // A successful import (201) returns the recreated policy JSON carrying its new id; store and register it so
        // the re-imported policy is swept by teardown.
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300
                && response.getData() != null && !response.getData().isBlank()) {
            Object id = Utils.extractValueFromPayload(response.getData(), "id");
            if (id != null) {
                if (policyIdKey != null) {
                    TestContext.set(Utils.normalizeContextKey(policyIdKey), id);
                }
                ResourceCleanup.register(Constants.CREATED_OPERATION_POLICY_IDS, id);
            }
        }
    }

    /**
     * Exports a published API as an archive (GET /apis/export, returns a zip) and stores the downloaded file's
     * path under the given context key. Asserts 200. First half of the API import/export round-trip
     * (APIImportExportTestCase). Binary download so the zip is not corrupted.
     */
    @When("I export the API {string} to an archive as {string}")
    public void iExportApiToArchive(String apiId, String archivePathKey) throws IOException, InterruptedException {
        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        String url = Utils.getApiExportURL(Utils.getBaseUrl(), actualApiId, "JSON");

        // Retries the export until the archive is complete. Concurrent exports of the same API by the same user collide
        // in a shared, non-unique temp directory. ExportUtils/CommonUtil.archiveDirectory keys the working dir
        // only on <user>-<apiName>-<version> (not per-export) and deletes it after zipping, so when the Governance
        // compliance evaluator materialises a just-created API at the same time as this export, one empties the dir
        // while the other is still zipping. The export then returns a 200 zip MISSING its project definition
        // (api.yaml/api.json), which later fails at import with the confusing 900909 "cannot find the project
        // definition". Re-exporting until the archive is COMPLETE makes the intermittency self-heal, so a
        // genuine never-completes failure surfaces here with a clear message instead of downstream as 900909.
        // TODO(cleanup): drop this retry and go back to a single export once concurrent exports of the same API
        // no longer share a temp directory. The retry is a test-side accommodation, not the desired shape.
        long pollStart = System.currentTimeMillis();
        long deadline = pollStart + Constants.RUNTIME_PROPAGATION_TIMEOUT;
        SimpleHTTPClient.DownloadResult result = null;
        boolean complete = false;
        while (true) {
            result = Requests.getToFile(url, headers, ".zip");
            if (result.getStatusCode() == 200
                    && Utils.zipContainsEntryNamed(result.getFile(), "api.yaml", "api.json")) {
                complete = true;
                break;
            }
            if (System.currentTimeMillis() >= deadline) {
                break;
            }
            logger.warn("API export archive for {} was incomplete (status {}, no api.yaml/api.json — "
                    + "export/compliance temp-dir race); re-exporting", actualApiId, result.getStatusCode());
            Utils.pollPause(pollStart, Constants.RETRY_INTERVAL_TIME);
        }
        Assert.assertTrue(complete, "API export for " + actualApiId + " never produced a complete archive (200 "
                + "with a project definition) within the window; last status="
                + (result == null ? "none" : result.getStatusCode()));
        TestContext.set(Utils.normalizeContextKey(archivePathKey), result.getFile().getAbsolutePath());
    }

    /**
     * Imports a previously-exported API archive (the temp-file path stored by the export step) with an inline
     * additionalProperties JSON, publishing the response. Second half of the round-trip. The /apis/import response
     * is a plain-text message (not the API id), so the imported API is located by name afterwards (see the
     * find-by-name step) for verification + cleanup.
     */
    @When("I import the exported archive {string} with additional properties {string} as {string}")
    public void iImportExportedArchive(String archivePathKey, String additionalPropsJson, String resourceId)
            throws IOException {
        String path = TestContext.resolve(archivePathKey).toString();
        File archiveFile = new File(path);

        File additionalPropertiesFile = File.createTempFile("data", ".json");
        additionalPropertiesFile.deleteOnExit();
        Files.write(additionalPropertiesFile.toPath(),
                Utils.resolveContextPlaceholders(additionalPropsJson).getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("file", archiveFile);
        files.put("additionalProperties", additionalPropertiesFile);

        HttpResponse response = Requests.postMultipart(Utils.getApiArchiveImportURL(Utils.getBaseUrl()), headers, files,
                null);
        TestContext.set(Utils.normalizeContextKey(resourceId), response.getData());
    }

    /**
     * Imports a previously-exported API archive with an explicit {@code preserveProvider} query flag, publishing
     * the response. Ports the preserveProvider matrix of APIImportExportTestCase
     * (testPreserveProviderTrue/FalseSameProviderApiImport): with {@code preserveProvider=true} the imported API
     * keeps the archive's original provider; with {@code preserveProvider=false} it is re-owned by the importing
     * user (the current acting actor). Uses the acting actor's publisher token so a DIFFERENT importer identity is
     * driven purely by {@code I act as}.
     */
    @When("I import the exported archive {string} with additional properties {string} and preserveProvider {string} as {string}")
    public void iImportExportedArchiveWithPreserveProvider(String archivePathKey, String additionalPropsJson,
            String preserveProvider, String resourceId) throws IOException {
        String path = TestContext.resolve(archivePathKey).toString();
        File archiveFile = new File(path);

        File additionalPropertiesFile = File.createTempFile("data", ".json");
        additionalPropertiesFile.deleteOnExit();
        Files.write(additionalPropertiesFile.toPath(),
                Utils.resolveContextPlaceholders(additionalPropsJson).getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("file", archiveFile);
        files.put("additionalProperties", additionalPropertiesFile);

        String url = Utils.getApiArchiveImportURL(Utils.getBaseUrl()) + "?preserveProvider=" + preserveProvider.trim();
        HttpResponse response = Requests.postMultipart(url, headers, files, null);
        TestContext.set(Utils.normalizeContextKey(resourceId), response.getData());
    }

    /**
     * Attempts to export an API to an archive as the CURRENT acting actor, asserting the download returns the
     * expected HTTP status (used by the restricted-role authz negative: a user lacking the access-control role
     * gets 401). A binary download whose status is checked directly (nothing published to httpResponse).
     */
    @When("I attempt to export the API {string} to an archive expecting status {int}")
    public void iAttemptToExportApi(String apiId, int expectedStatus) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        SimpleHTTPClient.DownloadResult result = Requests.getToFile(
                Utils.getApiExportURL(Utils.getBaseUrl(), actualApiId, "JSON"), headers, ".zip");
        Assert.assertEquals(result.getStatusCode(), expectedStatus,
                "API export status mismatch for api=" + actualApiId);
    }

    /**
     * Asserts an API's {@code provider} field (GET /apis/{id}) equals the given actor's full username. Ports the
     * provider assertions of the preserveProvider matrix (provider stays the original with preserveProvider=true;
     * becomes the importer with preserveProvider=false).
     */
    @Then("The provider of API {string} should match actor {string}")
    public void theProviderShouldMatchActor(String apiId, String actorRef) throws IOException {
        theProviderOfResourceShouldMatchActor("apis", apiId, actorRef);
    }

    /**
     * As {@link #theProviderShouldMatchActor}, but for ANY provider-owning publisher resource — today
     * {@code apis} and {@code api-products}. An API PRODUCT carries the same {@code provider} field and the same
     * super-tenant suffix rule, so the assertion is one body rather than two near-twins.
     *
     * <p>Load-bearing for the email-as-username dimension: it is the assertion that proves an email-form
     * username round-trips as a stored PROVIDER, not merely that the create returned 201. A 201 alone would
     * still pass if the product were attributed to a mangled or truncated principal.
     *
     * @param resourceType publisher resource collection ({@code apis} / {@code api-products})
     * @param idKey        context key holding the resource id
     * @param actorRef     actor whose full username the provider must equal
     */
    @Then("The provider of {string} resource {string} should match actor {string}")
    public void theProviderOfResourceShouldMatchActor(String resourceType, String idKey, String actorRef)
            throws IOException {
        String actualApiId = TestContext.resolve(idKey).toString();
        // The publisher API's provider field carries a tenant user's full username (e.g. admin@tenant1.com) but
        // strips the carbon.super suffix for a super-tenant user (e.g. ppImporter, not ppImporter@carbon.super).
        String expectedProvider = Identity.resolveActor(actorRef).getUserName();
        String superSuffix = "@" + Constants.SUPER_TENANT_DOMAIN;
        if (expectedProvider.endsWith(superSuffix)) {
            expectedProvider = expectedProvider.substring(0, expectedProvider.length() - superSuffix.length());
        }
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = Requests.get(
                Utils.getResourceEndpointURL(Utils.getBaseUrl(), resourceType, actualApiId), headers);
        Assert.assertTrue(response != null && response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isBlank(),
                resourceType + " fetch failed for id=" + actualApiId + " got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        String actualProvider = new JSONObject(response.getData()).getString("provider");
        Assert.assertEquals(actualProvider, expectedProvider,
                resourceType + " provider mismatch for id=" + actualApiId);
    }

    /**
     * Extracts a previously-downloaded API export archive (path under {@code archivePathKey}) and asserts the
     * inner {@code <name>-<version>/api.json} has its production AND sandbox endpoint-security passwords stripped
     * (empty). Ports APIImportExportTestCase#testAPIExport — the export must not leak backend credentials in
     * plain text. The api.json is a wrapper: {@code data.endpointConfig.endpoint_security.<production|sandbox>.password}.
     */
    @Then("The exported API archive {string} should have empty endpoint-security passwords")
    public void theExportedApiArchiveShouldStripSecrets(String archivePathKey) throws IOException {
        String zipPath = TestContext.resolve(archivePathKey).toString();
        File extractDir = Files.createTempDirectory("api-export-extract").toFile();
        extractDir.deleteOnExit();
        Utils.unzip(new File(zipPath), extractDir);

        // The archive top-level directory is "<name>-<version>"; locate the api.json under it.
        File apiJson = findFileByName(extractDir, "api.json");
        Assert.assertNotNull(apiJson, "Exported archive does not contain an api.json");
        String content = new String(Files.readAllBytes(apiJson.toPath()), StandardCharsets.UTF_8);
        JSONObject data = new JSONObject(content).getJSONObject("data");
        JSONObject endpointSecurity = data.getJSONObject("endpointConfig").getJSONObject("endpoint_security");
        String productionPassword = endpointSecurity.getJSONObject("production").optString("password", "");
        String sandboxPassword = endpointSecurity.getJSONObject("sandbox").optString("password", "");
        // Assert the password was stripped WITHOUT echoing the value — a non-empty value here is a real backend
        // credential and must not be printed into CI output (the very leak this test guards against).
        // isEmpty, NOT isBlank: the claim is that the exporter STRIPPED the field, and stripping yields "".
        // Any surviving content — including " " or "\n" — means it did not strip, so tolerating whitespace
        // would tolerate the one outcome this assertion exists to catch. Do not sweep this to isBlank.
        Assert.assertTrue(productionPassword.isEmpty(),
                "Production endpoint password was exported in plain text (expected empty)");
        Assert.assertTrue(sandboxPassword.isEmpty(),
                "Sandbox endpoint password was exported in plain text (expected empty)");
    }

    /** Depth-first search for a file with the given name under {@code root}. */
    private File findFileByName(File root, String name) {
        File[] children = root.listFiles();
        if (children == null) {
            return null;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                File found = findFileByName(child, name);
                if (found != null) {
                    return found;
                }
            } else if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Searches the Publisher API list for an API by exact name, stores the first match's id under the given key,
     * and registers it for teardown. Used to locate an API created out-of-band (e.g. by an archive import, whose
     * response carries only a message) so it can be asserted on and cleaned up. The Publisher search index is
     * eventually consistent, so this polls until the named API appears (or times out).
     */
    @When("I find the Publisher API named {string} and store its id as {string}")
    public void iFindPublisherApiByName(String name, String idKey) throws IOException, InterruptedException {
        String resolvedName = Utils.resolveContextPlaceholders(name);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        String url = Utils.getAPISearchEndpointURL(Utils.getBaseUrl(), "name:" + resolvedName, null, null);
        long endTimeStart = System.currentTimeMillis();
        long endTime = endTimeStart + Constants.RUNTIME_PROPAGATION_TIMEOUT;
        Object id = null;
        while (true) {
            try {
                HttpResponse response = Requests.get(url, headers);
                if (response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isBlank()) {
                    JSONArray list = new JSONObject(response.getData()).optJSONArray("list");
                    if (list != null && list.length() > 0) {
                        id = list.getJSONObject(0).get("id");
                        break;
                    }
                }
            } catch (IOException transientFailure) {
                // transient network failure — keep polling
            }
            if (System.currentTimeMillis() >= endTime) {
                break;
            }
            Utils.pollPause(endTimeStart, 2000);
        }
        Assert.assertNotNull(id, "No Publisher API named '" + resolvedName + "' was found within the deadline");
        TestContext.set(Utils.normalizeContextKey(idKey), id);
        ResourceCleanup.register(Constants.CREATED_API_IDS, id);
    }

    /**
     * Uploads a custom Synapse sequence as an API's sequence backend (PUT /apis/{id}/sequence-backend, multipart:
     * the sequence XML as the "sequence" part + a "type" form field PRODUCTION/SANDBOX). The API's endpoint type
     * must be sequence_backend. Publishes the response. Ports the sequence-backend side of REST invocation.
     */
    @When("I upload the sequence backend {string} of type {string} for API {string}")
    public void iUploadSequenceBackend(String sequencePath, String type, String apiId) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        File sequenceFile = Utils.classpathToTempFile(sequencePath, "fixture", ".xml");
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("sequence", sequenceFile);
        Map<String, String> formFields = new HashMap<>();
        formFields.put("type", type);
        Requests.putMultipart(Utils.getSequenceBackendURL(Utils.getBaseUrl(), actualApiId), headers, files, formFields);
    }

    /**
     * Imports an API from a WSDL file (POST /apis/import-wsdl, multipart: the WSDL as the "file" part plus the
     * additionalProperties JSON and implementationType form fields). implementationType is "SOAP" (pass-through
     * SOAP proxy) or "SOAPTOREST" (generate REST resources from the WSDL). Publishes the response and, on a 2xx,
     * stores the created API id and registers it for teardown. Ports WSDLImportTestCase / the create side of SOAP.
     */
    @When("I import a WSDL API from file {string} with additional properties {string} and implementation type {string} as {string}")
    public void iImportWsdlApi(String wsdlPath, String additionalProps, String implType, String resourceId)
            throws IOException {
        // Preserve the source extension so an archive import (.zip) is detected as an archive, not a raw WSDL.
        String suffix = wsdlPath.toLowerCase().endsWith(".zip") ? ".zip" : ".wsdl";
        File wsdlFile = Utils.classpathToTempFile(wsdlPath, "fixture", suffix);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("file", wsdlFile);
        // additionalProps is a context key holding the JSON doc-string; resolve the key then any inner {{...}}.
        String additionalPropsJson = Utils.resolveContextPlaceholders(
                TestContext.resolve(additionalProps).toString());
        Map<String, String> formFields = new HashMap<>();
        formFields.put("additionalProperties", additionalPropsJson);
        formFields.put("implementationType", implType);
        HttpResponse response = Requests.postMultipart(Utils.getImportWsdlURL(Utils.getBaseUrl()), headers, files,
                formFields);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
            TestContext.set(Utils.normalizeContextKey(resourceId), createdId);
            ResourceCleanup.register(Constants.CREATED_API_IDS, createdId);
        }
    }

    /**
     * Imports a WSDL API by URL (no file part) — the {@code url} form field of the import-wsdl endpoint. APIM
     * fetches and parses the WSDL from the given in-network URL (served by the soap-stub at
     * {@code http://nodebackend:3019/wsdl}). Ports the WSDL-URL import arc of WSDLImportTestCase (which used a
     * WireMock-hosted WSDL). Registers the created API for teardown on success; publishes the response so a
     * following assertion can check the status.
     */
    @When("I import a WSDL API from URL {string} with additional properties {string} and implementation type {string} as {string}")
    public void iImportWsdlApiFromUrl(String wsdlUrl, String additionalProps, String implType, String resourceId)
            throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        String additionalPropsJson = Utils.resolveContextPlaceholders(
                TestContext.resolve(additionalProps).toString());
        Map<String, String> formFields = new HashMap<>();
        formFields.put("url", wsdlUrl);
        formFields.put("additionalProperties", additionalPropsJson);
        formFields.put("implementationType", implType);
        HttpResponse response = Requests.postMultipart(Utils.getImportWsdlURL(Utils.getBaseUrl()), headers,
                new HashMap<>(), formFields);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
            TestContext.set(Utils.normalizeContextKey(resourceId), createdId);
            ResourceCleanup.register(Constants.CREATED_API_IDS, createdId);
        }
    }

    /**
     * Retrieves the WSDL definition of a WSDL-imported API from the Publisher ({@code GET /apis/{id}/wsdl}).
     * Publishes the response for a following status/content assertion. Ports WSDLImportTestCase#testGetWsdlDefinitions.
     */
    @When("I retrieve the WSDL definition of API {string}")
    public void iRetrieveWsdlDefinition(String apiId) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.get(Utils.getWsdlOfApiURL(Utils.getBaseUrl(), actualApiId), headers);
    }

    /**
     * Downloads the WSDL definition of a deployed API from the DevPortal store
     * ({@code GET /apis/{id}/wsdl?environmentName=}). Publishes the response for a following assertion. Ports
     * WSDLImportTestCase#testDownloadWsdlDefinitionsFromStore.
     */
    @When("I download the WSDL definition of API {string} from the devportal store")
    public void iDownloadWsdlFromStore(String apiId) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        String environmentName = System.getenv(Constants.GATEWAY_ENVIRONMENT);
        Requests.get(Utils.getDevPortalWsdlOfApiURL(Utils.getBaseUrl(), actualApiId, environmentName), headers);
    }

    /**
     * Retrieves the generated in/out conversion resource policies of a SOAP-to-REST API
     * ({@code GET /apis/{apiId}/resource-policies?sequenceType=<in|out>}), publishing the {@code {list:[...]}}
     * response so a following content + cardinality assertion can inspect the sequence {@code content} — the
     * synapse mediation that performs the JSON&lt;-&gt;SOAP conversion. Nothing else in this suite reads these
     * sequences, so an empty/degraded conversion would otherwise be invisible. Ports the retrieve half of
     * SoapToRestTestCase#testValidateInOutSequence.
     */
    @When("I retrieve the {string} sequence resource policies of API {string}")
    public void iRetrieveSequenceResourcePolicies(String sequenceType, String apiId) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        Requests.get(Utils.getApiResourcePoliciesURL(Utils.getBaseUrl(), actualApiId, sequenceType),
                Identity.publisherHeaders());
    }

    /**
     * Updates the single in-sequence resource policy of a SOAP-to-REST API by PREPENDING a unique marker property
     * mediator, then PUTs it back ({@code PUT /apis/{apiId}/resource-policies/{id}}) and publishes the PUT
     * response. A following re-retrieve asserts the marker persisted. Ports the INTENT of
     * SoapToRestTestCase#testUpdateInOutSequence, whose forEach swallowed update failures so it never verified
     * the update actually took.
     *
     * <p>Prepended rather than spliced in after the first tag: the policy {@code content} is a FRAGMENT of sibling
     * mediators, not a rooted document (it opens with a self-closing {@code <header/>} followed by sibling
     * {@code <property/>} / {@code <filter>} elements), so a leading property is a valid sibling and needs no
     * scanning. Locating an insertion point with {@code indexOf('>')} would be a raw-text scan with no notion of
     * markup — {@code >} is legal unescaped inside an XML attribute value, and synapse {@code regex}/
     * {@code expression} attributes can carry one — so it could split a tag and produce content the PUT rejects,
     * which would surface as a product fault rather than a test bug. Position is irrelevant here: the scenario
     * re-retrieves and asserts the marker persisted, it never invokes the sequence.
     */
    @When("I update the in-sequence resource policy of API {string} inserting marker {string}")
    public void iUpdateInSequenceResourcePolicy(String apiId, String marker) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        String markerValue = TestContext.resolve(marker).toString();
        Map<String, String> headers = Identity.publisherHeaders();
        // Intermediate read consumed locally (not the asserted response) — raw client per §7.
        HttpResponse list = SimpleHTTPClient.getInstance().doGet(
                Utils.getApiResourcePoliciesURL(Utils.getBaseUrl(), actualApiId, "in"), headers);
        Assert.assertTrue(list != null && list.getResponseCode() >= 200 && list.getResponseCode() < 300
                        && list.getData() != null && !list.getData().isBlank(),
                "Could not read in-sequence resource policy of " + actualApiId + " before update: got="
                        + (list == null ? "null" : list.getResponseCode() + "/" + list.getData()));
        JSONArray policies = new JSONObject(list.getData()).getJSONArray("list");
        Assert.assertEquals(policies.length(), 1,
                "Expected exactly one in-sequence resource policy, got: " + list.getData());
        JSONObject policy = policies.getJSONObject(0);
        String content = policy.getString("content");
        String markerElement = "<property name=\"" + markerValue + "\" value=\"" + markerValue + "\" scope=\"default\"/>";
        policy.put("content", markerElement + content);
        Requests.put(Utils.getApiResourcePolicyByIdURL(Utils.getBaseUrl(), actualApiId, policy.getString("id")),
                headers, policy.toString(), "application/json");
    }

    /**
     * Creates + deploys an API from a payload file, injecting a comma-separated tag list into its {@code tags}
     * field first (each tag placeholder resolved), so a DevPortal tag search can match on those tags. Registers
     * the API for teardown via the create primitive.
     */
    @Given("I have created an api from {string} with tags {string} as {string} and deployed it")
    public void iHaveCreatedAnApiFromWithTagsAsAndDeployedIt(String payloadPath, String tagsCsv, String apiID)
            throws IOException, InterruptedException {
        baseSteps.putJsonPayloadFromFile(payloadPath, "<createApiPayload>");
        JSONObject json = new JSONObject(TestContext.resolve("<createApiPayload>").toString());
        JSONArray tags = new JSONArray();
        for (String t : tagsCsv.split(",")) {
            tags.put(Utils.resolveContextPlaceholders(t.trim()));
        }
        json.put("tags", tags);
        baseSteps.putJsonPayloadInContext("<createApiPayload>", json.toString());
        iCreateAnAPIWithPayloadAs("apis", "<createApiPayload>", apiID);
        baseSteps.putJsonPayloadInContext("<createRevisionPayload>", "{\"description\":\"Initial Revision\"}");
        iCreateResourceRevision("apis", apiID, "<createRevisionPayload>");
        baseSteps.putJsonPayloadInContext("<deployRevisionPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        iDeployApiRevisionGivenPayload("<revisionId>", "apis", apiID, "<deployRevisionPayload>");
        baseSteps.theResponseStatusCodeShouldBe(201);
    }

    /**
     * Bulk-creates and publishes {@code count} APIs whose name and context are {@code prefix}0..N-1, so a single
     * DevPortal search by {@code prefix} matches exactly this scenario's set (the prefix must be a scenario-unique
     * value — see the unique-value step — so parallel scenarios never collide). No revision/deploy: DevPortal store
     * visibility follows the PUBLISHED lifecycle state, not gateway deployment, so this stays light for pagination
     * coverage. Each API is registered for teardown by the create primitive.
     */
    @Given("I create and publish {int} APIs from {string} named {string}")
    public void iCreateAndPublishApis(int count, String payloadPath, String namePrefixRef) throws IOException {
        String prefix = Utils.resolveContextPlaceholders(namePrefixRef);
        for (int i = 0; i < count; i++) {
            baseSteps.putJsonPayloadFromFile(payloadPath, "<bulkApiPayload>");
            JSONObject json = new JSONObject(TestContext.resolve("<bulkApiPayload>").toString());
            json.put("name", prefix + i);
            json.put("context", prefix + i);
            baseSteps.putJsonPayloadInContext("<bulkApiPayload>", json.toString());
            iCreateAnAPIWithPayloadAs("apis", "<bulkApiPayload>", "bulkApiId");
            iPublishTheResource("apis", "bulkApiId");
        }
    }

    /**
     * Creates {@code count} APIs (from the base test-API payload) whose PRODUCTION endpoint is the given URL,
     * uniquely named/contexted by {@code prefix}0..N-1. No publish/deploy — used by the endpoint-certificate usage
     * test, where "usage" is computed from the endpoint config, not from deployment. The endpoint URL resolves
     * {@code {{...}}} placeholders; each API is registered for teardown by the create primitive.
     *
     * <p>Publishes the created ids BOTH ways: {@code epUsageApiId} holds the last one (as before) and
     * {@code epUsageApiIds} the comma-separated set — the latter is what lets the usage assertion check WHICH APIs
     * are listed and not merely how many (a count-only assertion passes on three unrelated APIs).
     */
    @Given("I create {int} APIs with production endpoint {string} named {string}")
    public void iCreateApisWithProductionEndpoint(int count, String endpointUrl, String namePrefixRef)
            throws IOException {
        String prefix = Utils.resolveContextPlaceholders(namePrefixRef);
        String resolvedEndpoint = Utils.resolveContextPlaceholders(endpointUrl);
        List<String> createdIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            baseSteps.putJsonPayloadFromFile("artifacts/payloads/create_apim_test_api.json", "<epApiPayload>");
            JSONObject json = new JSONObject(TestContext.resolve("<epApiPayload>").toString());
            json.put("name", prefix + i);
            json.put("context", prefix + i);
            JSONObject endpointConfig = new JSONObject();
            endpointConfig.put("endpoint_type", "http");
            endpointConfig.put("production_endpoints", new JSONObject().put("url", resolvedEndpoint));
            endpointConfig.put("sandbox_endpoints", new JSONObject().put("url", resolvedEndpoint));
            json.put("endpointConfig", endpointConfig);
            baseSteps.putJsonPayloadInContext("<epApiPayload>", json.toString());
            iCreateAnAPIWithPayloadAs("apis", "<epApiPayload>", "epUsageApiId");
            createdIds.add(TestContext.resolve("epUsageApiId").toString());
        }
        TestContext.set("epUsageApiIds", String.join(",", createdIds));
    }

    /**
     * Asserts that an imported SOAP API's {@code wsdlUrl} field points at the correct tenant-scoped registry WSDL
     * path. Ports SOAPAPIImportExportTestCase#testAPIWSDLUrl. The expected path is derived from the API's own
     * {@code provider}/{@code name}/{@code version} fields (retrieved via GET /apis/{id}), so it is correct for
     * both the super tenant and a sub-tenant without hard-coding a specific unique name. The registry layout is:
     *   super:  /registry/resource/_system/governance/apimgt/applicationdata/provider/{p}/{name}/{ver}/{p}--{name}{ver}.wsdl
     *   tenant: /t/{domain}/registry/resource/.../provider/{p-enc}/{name}/{ver}/{p-enc}--{name}{ver}.wsdl
     * where the provider is registry-encoded (an '@' becomes '-AT-').
     */
    @Then("The wsdlUrl of API {string} should be the tenant-scoped registry WSDL path")
    public void theWsdlUrlShouldBeTenantScoped(String apiId) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = Requests.get(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", actualApiId), headers);
        Assert.assertTrue(response != null && response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isBlank(),
                "API fetch failed for api=" + actualApiId + " got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        JSONObject api = new JSONObject(response.getData());
        String provider = api.getString("provider");
        String name = api.getString("name");
        String version = api.getString("version");
        String actualWsdlUrl = api.optString("wsdlUrl", null);

        // Registry-encode the provider (tenant users carry an '@domain' which the registry stores as '-AT-domain').
        String providerEncoded = provider.replace("@", "-AT-");
        String tenantPrefix = "";
        int at = provider.indexOf('@');
        if (at >= 0 && !Constants.SUPER_TENANT_DOMAIN.equals(provider.substring(at + 1))) {
            tenantPrefix = "/t/" + provider.substring(at + 1);
        }
        String expectedWsdlUrl = tenantPrefix
                + "/registry/resource/_system/governance/apimgt/applicationdata/provider/"
                + providerEncoded + "/" + name + "/" + version + "/"
                + providerEncoded + "--" + name + version + ".wsdl";
        Assert.assertEquals(actualWsdlUrl, expectedWsdlUrl, "WSDL URI set to the imported API is incorrect");
    }

    /**
     * Deletes an API-specific operation policy.
     *
     * @param apiId Context key containing the API ID
     * @param policyId Context key containing the policy ID to delete
     */
    @When("I delete the api {string} specific policy {string}")
    public void iDeleteTheApiSpecificPolicy(String apiId, String policyId) throws IOException {

        String actualApiId = TestContext.resolve(apiId).toString();
        String policyID = TestContext.resolve(policyId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Requests.delete(Utils.getAPISpecificPolicyById(Utils.getBaseUrl(), actualApiId, policyID), headers);
    }

    /**
     * Retrieves the operation-policy list of an API (GET {@code apis/{apiId}/operation-policies}) and publishes it,
     * so a following assertion can confirm an API-specific policy is present (create paths) or ABSENT (post-delete).
     * The generic {@code I retrieve the "{type}" resource with id "{id}"} step cannot express this two-segment
     * sub-collection path — it resolves {@code id} through {@code TestContext.resolve} (a context key) and builds
     * {@code {type}/{id}}, with no way to add the trailing {@code operation-policies} collection segment.
     *
     * @param apiId context key holding the API id whose policy list is retrieved
     */
    @When("I retrieve the operation policies of API {string}")
    public void iRetrieveTheApiSpecificOperationPolicies(String apiId) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.get(Utils.getAPISpecificPolicy(Utils.getBaseUrl(), actualApiId), headers);
    }

    /** Resolves a shipped/created COMMON operation policy (its {@code id}/{@code name}/{@code version}) by display
     *  name (GET /operation-policies). Returns the list entry so callers get the version too — the attach path
     *  validates policyName+policyVersion against the spec identified by policyId (a missing version → 400). */
    private JSONObject resolveCommonPolicyByName(String policyName, Map<String, String> headers) throws IOException {
        HttpResponse listResp = SimpleHTTPClient.getInstance().doGet(Utils.getCommonPolicy(Utils.getBaseUrl()), headers);
        Assert.assertTrue(listResp != null && listResp.getResponseCode() >= 200 && listResp.getResponseCode() < 300
                        && listResp.getData() != null && !listResp.getData().isBlank(),
                "Failed to list common policies while resolving '" + policyName + "': got "
                        + (listResp == null ? "no response" : listResp.getResponseCode() + " / " + listResp.getData()));
        JSONArray policies = new JSONObject(listResp.getData()).optJSONArray("list");
        for (int i = 0; policies != null && i < policies.length(); i++) {
            JSONObject p = policies.getJSONObject(i);
            if (policyName.equals(p.optString("name"))) {
                return p;
            }
        }
        throw new IllegalStateException("Common operation policy '" + policyName + "' not found in the pack");
    }

    /**
     * Attaches a shipped COMMON operation policy (looked up by name) to operation index {@code opIndex} of an API,
     * in the given comma-separated flows ({@code request}/{@code response}/{@code fault}), then PUTs the API back —
     * publishing the PUT response for the feature to assert. Parameters are supplied as an inline JSON object (use
     * {@code {}} for none). Ports the operation-policy attach path of OperationPolicyTestCase — the negatives
     * (missing required attributes → 400; a policy attached to an unsupported flow → 400) and the positive attach
     * (whose clone is then checked by the md5 step below).
     *
     * @param policyName shipped common policy name (e.g. {@code addHeader}, {@code removeHeader}, {@code jsonFault})
     * @param opIndex    zero-based index into the API's {@code operations} array
     * @param apiId      context key holding the API id
     * @param flowsCsv   comma-separated flows the policy is applied to
     * @param paramsJson inline JSON object of policy parameters ({@code {}} for none/missing)
     */
    @When("I attach the common operation policy {string} to operation {int} of API {string} in flows {string} with parameters {string}")
    public void iAttachCommonPolicyToOperation(String policyName, int opIndex, String apiId, String flowsCsv,
            String paramsJson) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        JSONObject commonPolicy = resolveCommonPolicyByName(policyName, headers);
        String policyId = commonPolicy.optString("id");
        String policyVersion = commonPolicy.optString("version");

        HttpResponse getApi = SimpleHTTPClient.getInstance()
                .doGet(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", actualApiId), headers);
        Assert.assertTrue(getApi != null && getApi.getResponseCode() >= 200 && getApi.getResponseCode() < 300
                        && getApi.getData() != null && !getApi.getData().isBlank(),
                "Failed to fetch API '" + actualApiId + "' before attaching operation policy: got "
                        + (getApi == null ? "no response" : getApi.getResponseCode() + " / " + getApi.getData()));
        JSONObject api = new JSONObject(getApi.getData());
        JSONObject operation = api.getJSONArray("operations").getJSONObject(opIndex);

        JSONObject operationPolicies = operation.optJSONObject("operationPolicies");
        if (operationPolicies == null) {
            operationPolicies = new JSONObject();
        }
        for (String flow : flowsCsv.split(",")) {
            String flowKey = flow.trim();
            JSONObject policyEntry = new JSONObject()
                    .put("policyName", policyName)
                    .put("policyVersion", policyVersion)
                    .put("policyType", "common")
                    .put("policyId", policyId)
                    .put("parameters", new JSONObject(paramsJson));
            operationPolicies.put(flowKey, new JSONArray().put(policyEntry));
        }
        operation.put("operationPolicies", operationPolicies);

        Requests.put(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", actualApiId), headers, api.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Asserts that attaching a common operation policy cloned it to the API level: the policy id now recorded on
     * operation {@code opIndex}'s {@code request} flow differs from the original common policy's id, but the two
     * policies have an identical md5 (same content). Ports
     * OperationPolicyTestCase#testCommonOperationPolicyCloneToAPILevelWithUpdate.
     */
    @Then("The operation {int} of API {string} should have a clone of common policy {string} with a new id and matching md5")
    public void theClonedPolicyShouldMatchMd5(int opIndex, String apiId, String commonPolicyName) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        String commonPolicyId = resolveCommonPolicyByName(commonPolicyName, headers).optString("id");

        HttpResponse getApi = SimpleHTTPClient.getInstance()
                .doGet(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", actualApiId), headers);
        Assert.assertTrue(getApi != null && getApi.getResponseCode() == 200
                        && getApi.getData() != null && !getApi.getData().isBlank(),
                "Failed to fetch API '" + actualApiId + "' for clone md5 check: got "
                        + (getApi == null ? "no response" : getApi.getResponseCode() + " / " + getApi.getData()));
        JSONObject api = new JSONObject(getApi.getData());
        String clonedPolicyId = api.getJSONArray("operations").getJSONObject(opIndex)
                .getJSONObject("operationPolicies").getJSONArray("request").getJSONObject(0).getString("policyId");
        Assert.assertNotEquals(clonedPolicyId, commonPolicyId,
                "Attaching a common policy should clone it to the API level with a NEW id");

        // md5 of the common policy vs the API-specific clone must match (identical content).
        HttpResponse commonResp = SimpleHTTPClient.getInstance()
                .doGet(Utils.getCommonPolicyById(Utils.getBaseUrl(), commonPolicyId), headers);
        Assert.assertTrue(commonResp != null && commonResp.getResponseCode() == 200
                        && commonResp.getData() != null && !commonResp.getData().isBlank(),
                "Failed to fetch common policy '" + commonPolicyId + "': got "
                        + (commonResp == null ? "no response" : commonResp.getResponseCode() + " / " + commonResp.getData()));
        HttpResponse clonedResp = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPISpecificPolicyById(Utils.getBaseUrl(), actualApiId, clonedPolicyId), headers);
        Assert.assertTrue(clonedResp != null && clonedResp.getResponseCode() == 200
                        && clonedResp.getData() != null && !clonedResp.getData().isBlank(),
                "Failed to fetch API-specific clone '" + clonedPolicyId + "': got "
                        + (clonedResp == null ? "no response" : clonedResp.getResponseCode() + " / " + clonedResp.getData()));
        String commonMd5 = new JSONObject(commonResp.getData()).getString("md5");
        String clonedMd5 = new JSONObject(clonedResp.getData()).getString("md5");
        Assert.assertEquals(clonedMd5, commonMd5,
                "Cloned API-level policy md5 must match the common policy md5");
    }

    /**
     * Extracts a previously-downloaded common-operation-policy archive (path stored under {@code archivePathKey})
     * and asserts (a) it contains a spec file named {@code <policyName>.<ext>} whose content parses as the given
     * format ({@code json} or {@code yaml}) carrying the policy name, plus the {@code <policyName>.j2} synapse
     * template, AND (b) the exported artefacts faithfully carry the CONTENT of the SOURCE files the policy was
     * created from ({@code artifacts/payloads/policySpecFiles/<policyName>.yaml} + {@code .j2}) — an export that
     * produced an EMPTY or WRONG spec/synapse would pass a presence-only check but fails here. Ports the
     * archive-content assertions of OperationPolicyTestCase#testCommonOperationPolicyExport (YAML) and
     * testCommonOperationPolicyExportWithJSONContent (JSON), which likewise compared parsed spec content (not text).
     *
     * <p>The source spec is always YAML (v2 supplies YAML specs); it is parsed structurally and the exported spec
     * (yaml OR json, both parsed with the same YAML parser since JSON is a YAML subset) must carry every
     * {@code data} field/value the source declares (deep containment; list-valued fields compared as sets so a
     * server re-ordering of e.g. {@code supportedApiTypes} is not a spurious failure). The synapse template is
     * stored verbatim, so it is compared as normalized text (line-trimmed, blank lines dropped).</p>
     */
    @Then("The exported operation policy archive {string} should contain a {string} spec for policy {string}")
    public void theExportedPolicyArchiveShouldContain(String archivePathKey, String format, String policyName)
            throws IOException {
        String zipPath = TestContext.resolve(archivePathKey).toString();
        File extractDir = Files.createTempDirectory("op-policy-extract").toFile();
        extractDir.deleteOnExit();
        Utils.unzip(new File(zipPath), extractDir);

        String ext = "json".equalsIgnoreCase(format) ? "json" : "yaml";
        File specFile = new File(extractDir, policyName + File.separator + policyName + "." + ext);
        Assert.assertTrue(specFile.exists(),
                "Exported archive is missing the " + ext + " policy spec: " + specFile.getPath());
        File synapseFile = new File(extractDir, policyName + File.separator + policyName + ".j2");
        Assert.assertTrue(synapseFile.exists(),
                "Exported archive is missing the synapse template: " + synapseFile.getPath());

        String specContent = new String(Files.readAllBytes(specFile.toPath()), StandardCharsets.UTF_8);
        if ("json".equalsIgnoreCase(format)) {
            // Must parse as JSON and carry the policy's name in its spec data.
            JSONObject spec = new JSONObject(specContent);
            String specName = spec.getJSONObject("data").getString("name");
            Assert.assertEquals(specName, policyName, "Exported JSON spec name mismatch");
        } else {
            Assert.assertTrue(specContent.contains("name: " + policyName)
                            || specContent.contains("name:" + policyName),
                    "Exported YAML spec does not carry the policy name");
        }

        // Content equality against the SOURCE files the policy was created from. The source spec is YAML; the
        // exported spec may be YAML or JSON but both parse with the same YAML parser (JSON ⊂ YAML).
        String sourceSpecResource = "artifacts/payloads/policySpecFiles/" + policyName + ".yaml";
        String sourceSynapseResource = "artifacts/payloads/policySpecFiles/" + policyName + ".j2";
        Object sourceSpec = new Yaml().load(Utils.readClasspathResource(sourceSpecResource));
        Object exportedSpec = new Yaml().load(specContent);
        Object sourceData = ((Map<?, ?>) sourceSpec).get("data");
        Assert.assertTrue(exportedSpec instanceof Map && ((Map<?, ?>) exportedSpec).get("data") instanceof Map,
                "Exported spec has no 'data' object: " + specContent);
        Object exportedData = ((Map<?, ?>) exportedSpec).get("data");
        assertSpecContentContainsSource(sourceData, exportedData, "data");

        // RAW comparison, no normalization. The previous form trimmed every line and dropped blank lines, which
        // silently accepted a reformatted template: custom_add_common_header.j2 indents its <header> by 3 spaces,
        // so an export that stripped or re-indented it passed. Synapse content is whitespace-significant enough
        // that a reformat is worth failing on, and the product does round-trip it verbatim (verified by running
        // PublisherOperationPoliciesRunner against this exact assertion).
        String sourceSynapse = Utils.readClasspathResource(sourceSynapseResource);
        String exportedSynapse = new String(Files.readAllBytes(synapseFile.toPath()), StandardCharsets.UTF_8);
        Assert.assertEquals(exportedSynapse, sourceSynapse,
                "Exported synapse template content does not match the source " + sourceSynapseResource);
    }

    /**
     * Asserts the exported spec content ({@code actual}) faithfully carries the SOURCE content ({@code expected}):
     * every scalar value is equal, every source list element is present in the exported list (set containment), and
     * every source map key resolves recursively. Tolerant of any extra field the server may add on export, but
     * catches a dropped field, a changed value, or an empty/wrong spec — the hole this strengthening closes.
     */
    private void assertSpecContentContainsSource(Object expected, Object actual, String path) {
        if (expected instanceof Map<?, ?> expectedMap) {
            Assert.assertTrue(actual instanceof Map,
                    "Exported spec content at '" + path + "' is not an object: " + actual);
            Map<?, ?> actualMap = (Map<?, ?>) actual;
            for (Map.Entry<?, ?> entry : expectedMap.entrySet()) {
                String childPath = path + "." + entry.getKey();
                // An empty/null source value carries no content to verify — the server may legitimately omit an
                // empty collection (e.g. policyAttributes: []) on export, so don't require the key in that case.
                if (isEmptyValue(entry.getValue())) {
                    continue;
                }
                Assert.assertTrue(actualMap.containsKey(entry.getKey()),
                        "Exported spec is missing field '" + childPath + "'");
                assertSpecContentContainsSource(entry.getValue(), actualMap.get(entry.getKey()), childPath);
            }
        } else if (expected instanceof List<?> expectedList) {
            Assert.assertTrue(actual instanceof List,
                    "Exported spec content at '" + path + "' is not a list: " + actual);
            List<String> actualCanonical = new ArrayList<>();
            Set<String> actualSet = new HashSet<>();
            for (Object o : (List<?>) actual) {
                String canonical = String.valueOf(o);
                actualCanonical.add(canonical);
                actualSet.add(canonical);
            }
            Set<String> expectedSet = new HashSet<>();
            for (Object o : expectedList) {
                expectedSet.add(String.valueOf(o));
            }
            Assert.assertEquals(actualCanonical.size(), expectedList.size(),
                    "Exported spec list '" + path + "' has unexpected cardinality; expected "
                            + expectedList.size() + " but got " + actualCanonical.size());
            Assert.assertEquals(actualSet.size(), actualCanonical.size(),
                    "Exported spec list '" + path + "' contains duplicate elements: " + actualCanonical);
            Assert.assertEquals(actualSet, expectedSet,
                    "Exported spec list '" + path + "' does not exactly match the source set");
        } else {
            Assert.assertEquals(String.valueOf(actual), String.valueOf(expected),
                    "Exported spec value at '" + path + "' does not match source");
        }
    }

    /** True when a parsed spec value carries no content to verify (null, empty list, or empty map). */
    private boolean isEmptyValue(Object value) {
        return value == null
                || (value instanceof List<?> list && list.isEmpty())
                || (value instanceof Map<?, ?> map && map.isEmpty());
    }

    /**
     * Builds a deliberately-malformed common-operation-policy archive whose inner spec/template are named
     * differently from the archive's declared policy (a mismatched-name archive) and imports it, asserting the
     * server rejects it. Ports OperationPolicyTestCase#testImportInvalidCommonOperationPolicy — verified live on
     * 4.7.0: the import of a mismatched/malformed archive surfaces as HTTP 500. The 500 is the actual product
     * contract for this garbage-input path (not enshrined as desirable; pinned to characterise it).
     */
    @When("I import a malformed common operation policy archive built from spec {string} and synapse {string} expecting status {int}")
    public void iImportMalformedCommonPolicyArchive(String specResource, String synapseResource, int expectedStatus)
            throws IOException {
        // Assemble a <name>_<version> directory whose files are NOT named after the (mismatched) spec inside — the
        // spec file omits the required name (custom_invalid_header) while the archive dir claims another policy.
        String archiveBaseName = "mismatchedPolicy_v1";
        File workDir = Files.createTempDirectory("op-policy-bad").toFile();
        workDir.deleteOnExit();
        File policyDir = new File(workDir, archiveBaseName);
        policyDir.mkdirs();

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(specResource)) {
            if (in == null) {
                throw new FileNotFoundException("Policy spec resource not found: " + specResource);
            }
            Files.copy(in, new File(policyDir, "mismatchedPolicy.yaml").toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(synapseResource)) {
            if (in == null) {
                throw new FileNotFoundException("Synapse resource not found: " + synapseResource);
            }
            Files.copy(in, new File(policyDir, "mismatchedPolicy.j2").toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        File zipFile = new File(workDir, archiveBaseName + ".zip");
        Utils.zipDirectory(policyDir, zipFile);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("file", zipFile);
        HttpResponse response = Requests.postMultipart(Utils.getCommonPolicyImportURL(Utils.getBaseUrl()), headers, files,
                null);
        Assert.assertEquals(response.getResponseCode(), expectedStatus,
                "Malformed common operation policy import status mismatch (body=" + response.getData() + ")");
        // If the server unexpectedly created a policy, register it so teardown removes it (defensive).
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300
                && response.getData() != null && !response.getData().isBlank()) {
            Object id = Utils.extractValueFromPayload(response.getData(), "id");
            if (id != null) {
                ResourceCleanup.register(Constants.CREATED_OPERATION_POLICY_IDS, id);
            }
        }
    }

    /**
     * Rewrites the {@code parameters} of the operation policy in a given flow of an operation and PUTs the API
     * back — publishing the response for the feature to assert. Used to exercise the secret-attribute PRESERVE
     * semantics: an UPDATE that supplies an EMPTY value for a Secret attribute must NOT clear the previously-set
     * secret (the server preserves it). Ports the update side of
     * OperationPolicyTestCase#testUpdatePolicyWithSecretAttributes.
     *
     * @param flow       flow whose policy is updated ({@code request}/{@code response}/{@code fault})
     * @param opIndex    zero-based index into the API's {@code operations} array
     * @param apiId      context key holding the API id
     * @param paramsJson inline JSON object of the new policy parameters (e.g. {@code {"apiKey":"","token":""}})
     */
    @When("I update the parameters of the operation policy in flow {string} of operation {int} of API {string} to {string}")
    public void iUpdateOperationPolicyParameters(String flow, int opIndex, String apiId, String paramsJson)
            throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        HttpResponse getApi = SimpleHTTPClient.getInstance()
                .doGet(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", actualApiId), headers);
        Assert.assertTrue(getApi != null && getApi.getResponseCode() >= 200 && getApi.getResponseCode() < 300
                        && getApi.getData() != null && !getApi.getData().isBlank(),
                "Failed to fetch API '" + actualApiId + "' before updating operation policy parameters: got "
                        + (getApi == null ? "no response" : getApi.getResponseCode() + " / " + getApi.getData()));
        JSONObject api = new JSONObject(getApi.getData());
        JSONArray flowPolicies = api.getJSONArray("operations").getJSONObject(opIndex)
                .getJSONObject("operationPolicies").getJSONArray(flow);
        Assert.assertTrue(flowPolicies.length() > 0,
                "No operation policy present in flow '" + flow + "' of operation " + opIndex + " to update");
        flowPolicies.getJSONObject(0).put("parameters", new JSONObject(paramsJson));

        Requests.put(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", actualApiId), headers, api.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Asserts that a Secret operation-policy attribute is still SET (masked) on the API — i.e. it was preserved,
     * not cleared. A set-but-masked secret is returned with the parameter key PRESENT and its value blanked to
     * {@code ""} (the server never echoes a secret's real value); a cleared/never-set secret is absent (or null).
     * So "present and blank" is the publisher-plane signature of a preserved secret. Ports the retrieve-side
     * assertion of OperationPolicyTestCase#testRetrievePolicyWithSecretAttributes /
     * testUpdatePolicyWithSecretAttributes.
     *
     * @param paramName the Secret attribute name (e.g. {@code apiKey})
     * @param flow      flow whose policy is inspected ({@code request}/{@code response}/{@code fault})
     * @param opIndex   zero-based index into the API's {@code operations} array
     * @param apiId     context key holding the API id
     */
    @Then("The secret parameter {string} of the operation policy in flow {string} of operation {int} of API {string} should be preserved and masked")
    public void theSecretParameterShouldBePreservedAndMasked(String paramName, String flow, int opIndex, String apiId)
            throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        HttpResponse getApi = SimpleHTTPClient.getInstance()
                .doGet(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", actualApiId), headers);
        Assert.assertTrue(getApi != null && getApi.getResponseCode() == 200
                        && getApi.getData() != null && !getApi.getData().isBlank(),
                "Failed to fetch API '" + actualApiId + "' for secret-preservation check: got "
                        + (getApi == null ? "no response" : getApi.getResponseCode() + " / " + getApi.getData()));
        JSONObject parameters = new JSONObject(getApi.getData()).getJSONArray("operations").getJSONObject(opIndex)
                .getJSONObject("operationPolicies").getJSONArray(flow).getJSONObject(0).getJSONObject("parameters");
        // Preserved secret: key present AND blank (masked). Cleared/never-set: absent (or null).
        Assert.assertTrue(parameters.has(paramName) && !parameters.isNull(paramName),
                "Secret parameter '" + paramName + "' was CLEARED (absent/null) — the empty-value update should have "
                        + "preserved it. parameters=" + parameters);
        Assert.assertEquals(parameters.getString(paramName), "",
                "Secret parameter '" + paramName + "' should be masked to an empty string on retrieval, got: "
                        + parameters.get(paramName));
    }

    /**
     * Imports an OpenAPI definition from a file and creates an API.
     * This step handles the multipart file upload required for API import, including both
     * the OpenAPI definition file and additional properties file.
     *
     * @param filepath Path to the OpenAPI definition file (.json or .yaml) in classpath resources
     * @param additionalData Path to the additional properties JSON file in classpath resources
     * @param resourceId Context key where the created API ID will be stored
     */
    @When("I import open api definition from {string} , additional properties from {string} and create api as {string}")
    public void iImportOpenApiDefinitionFromAndCreateApiAs(String filepath, String additionalData, String resourceId) throws IOException {

        File openapiFile;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filepath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("API definition file not found: " + filepath);
            }

            // Create temporary file object
            openapiFile = File.createTempFile("openapi", ".json");
            openapiFile.deleteOnExit();
            Files.copy(inputStream, openapiFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        File additionalPropertiesFile;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(additionalData)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Additional properties file not found: " + additionalData);
            }

            // The additional-properties file carries the created API's name/context, so resolve any
            // ${UNIQUE:...} placeholders here (this file is uploaded as-is, not routed through the
            // context-payload steps) to keep every imported API unique-named across parallel runs.
            String additionalProperties = Utils.resolvePayloadPlaceholders(
                    IOUtils.toString(inputStream, StandardCharsets.UTF_8));

            // Create temporary file object
            additionalPropertiesFile = File.createTempFile("data", ".json");
            additionalPropertiesFile.deleteOnExit();
            Files.write(additionalPropertiesFile.toPath(),
                    additionalProperties.getBytes(StandardCharsets.UTF_8));
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Map<String, File> files = new HashMap<>();
        files.put("file", openapiFile);
        files.put("additionalProperties", additionalPropertiesFile);

        HttpResponse response = Requests.postMultipart(Utils.getImportOpenAPIURL(Utils.getBaseUrl()), headers, files, null);
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
        TestContext.set(resourceId, createdId);
        // Register for scenario teardown so imported APIs do not accumulate across scenarios on a shared server.
        ResourceCleanup.register(Constants.CREATED_API_IDS, createdId);
    }

    /**
     * Imports an API from an OpenAPI ARCHIVE (.zip, may contain remote $refs) — POST /apis/import-openapi,
     * multipart {@code file} (the .zip, extension preserved) + {@code additionalProperties} (name/context/
     * endpoint/policies, ${UNIQUE} resolved). Non-asserting — the feature asserts (valid archive → 201;
     * incorrect archive → the observed error). On 2xx the created id is stored + registered for teardown.
     * Ports APIM18 testCreateApiWithArchivesWithRemoteReferences[WithIncorrectSwagger].
     *
     * @param archivePath    classpath path to the .zip archive
     * @param additionalData classpath path to the additional-properties JSON
     * @param resourceId     context key to store the created API id under (on success)
     */
    @When("I import api from archive {string} with additional properties {string} as {string}")
    public void iImportApiFromArchive(String archivePath, String additionalData, String resourceId) throws IOException {

        File archiveFile = Utils.classpathToTempFile(archivePath, "fixture", ".zip");

        String additionalProperties;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(additionalData)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Additional properties file not found: " + additionalData);
            }
            additionalProperties = Utils.resolvePayloadPlaceholders(
                    IOUtils.toString(inputStream, StandardCharsets.UTF_8));
        }
        File additionalPropertiesFile = File.createTempFile("data", ".json");
        additionalPropertiesFile.deleteOnExit();
        Files.write(additionalPropertiesFile.toPath(), additionalProperties.getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("file", archiveFile);
        files.put("additionalProperties", additionalPropertiesFile);

        HttpResponse response = Requests.postMultipart(Utils.getImportOpenAPIURL(Utils.getBaseUrl()), headers, files,
                null);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
            TestContext.set(resourceId, createdId);
            ResourceCleanup.register(Constants.CREATED_API_IDS, createdId);
        }
    }

    /**
     * Imports an AsyncAPI definition (multipart {@code file} + {@code additionalProperties}) via the publisher
     * {@code apis/import-asyncapi} endpoint and stores the created API id under {@code resourceId} on success.
     * ASYNC APIs can only be created as third-party (advertise-only), so the additional-properties JSON must carry
     * {@code advertiseInfo.advertised=true}. The response is published as {@code httpResponse} so the feature can
     * assert the exact status (201 on success) and, on the negative/invalid paths, the validation error body.
     * Ports the import arc of AsyncAPITestWithValidationCase (V2 + V3).
     *
     * @param filepath       classpath path to the AsyncAPI YAML definition
     * @param additionalData classpath path to the additional-properties JSON (name/context/version/type/policies/
     *                       advertiseInfo, {@code ${UNIQUE}}/{@code {{...}}} resolved)
     * @param resourceId     context key to store the created API id under (null for negative attempts)
     */
    @When("I import asyncapi definition from {string} with additional properties {string} as {string}")
    public void iImportAsyncApiAsResource(String filepath, String additionalData, String resourceId)
            throws IOException {
        importAsyncApiDefinition(filepath, additionalData, resourceId);
    }

    /** Non-asserting AsyncAPI import for negative/invalid-spec scenarios (publishes {@code httpResponse}; stores
     *  no id). The feature asserts the rejection status and error message. */
    @When("I attempt to import asyncapi definition from {string} with additional properties {string}")
    public void iAttemptImportAsyncApi(String filepath, String additionalData) throws IOException {
        importAsyncApiDefinition(filepath, additionalData, null);
    }

    private void importAsyncApiDefinition(String filepath, String additionalData, String resourceId)
            throws IOException {

        File asyncApiFile = Utils.classpathToTempFile(filepath, "fixture", ".yaml");
        File additionalPropertiesFile;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(additionalData)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Additional properties file not found: " + additionalData);
            }
            String additionalProperties = Utils.resolvePayloadPlaceholders(
                    IOUtils.toString(inputStream, StandardCharsets.UTF_8));
            additionalPropertiesFile = File.createTempFile("data", ".json");
            additionalPropertiesFile.deleteOnExit();
            Files.write(additionalPropertiesFile.toPath(), additionalProperties.getBytes(StandardCharsets.UTF_8));
        }
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("file", asyncApiFile);
        files.put("additionalProperties", additionalPropertiesFile);
        HttpResponse response = Requests.postMultipart(Utils.getImportAsyncApiURL(Utils.getBaseUrl()), headers, files,
                null);
        if (resourceId != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
            TestContext.set(resourceId, createdId);
            ResourceCleanup.register(Constants.CREATED_API_IDS, createdId);
        }
    }

    /** Loads a classpath resource into a temp .json file (for multipart OAS upload). */
    private File loadJsonResourceAsTempFile(String resourcePath) throws IOException {
        return Utils.classpathToTempFile(resourcePath, "fixture", ".json");
    }

    /**
     * Uploads a thumbnail image for an API (POST /apis/{id}/thumbnail, multipart form field {@code file}).
     * Ports the thumbnail-set half of APIMANAGER5872. Non-asserting.
     *
     * @param imagePath classpath path to the image (e.g. artifacts/images/thumbnail.png)
     * @param apiId     context key holding the API id
     */
    @When("I upload thumbnail {string} for API {string}")
    public void iUploadThumbnail(String imagePath, String apiId) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("file", Utils.classpathToTempFile(imagePath, "fixture", ".png"));
        // Thumbnail upload is a PUT (updateAPIThumbnail), not POST — a POST returns 405.
        Requests.putMultipart(Utils.getThumbnailURL(Utils.getBaseUrl(), actualApiId), headers,
                files, new HashMap<>());
    }

    /**
     * Downloads an API's thumbnail (GET /apis/{id}/thumbnail) — 200 when a thumbnail is set. Non-asserting.
     *
     * @param apiId context key holding the API id
     */
    @When("I retrieve the thumbnail for API {string}")
    public void iRetrieveThumbnail(String apiId) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.get(Utils.getThumbnailURL(Utils.getBaseUrl(), actualApiId), headers);
    }

    /**
     * Updates an API's OpenAPI definition (PUT /apis/{id}/swagger, form field {@code apiDefinition}) from a
     * classpath OAS file. Non-asserting — the feature asserts the status (a valid definition → 200; an invalid
     * one, e.g. empty resource paths, → 400).
     */
    @When("I update the swagger of {string} resource {string} from file {string}")
    public void iUpdateSwaggerFromFile(String resourceType, String resourceId, String filepath) throws IOException {

        String actualId = TestContext.resolve(resourceId).toString();
        String definition;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(filepath)) {
            if (in == null) {
                throw new FileNotFoundException("OAS file not found: " + filepath);
            }
            definition = IOUtils.toString(in, StandardCharsets.UTF_8);
        }
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        // The swagger PUT is multipart/form-data with the definition as the text field "apiDefinition".
        Map<String, String> formFields = new HashMap<>();
        formFields.put("apiDefinition", definition);
        Requests.putMultipart(
                Utils.getSwaggerURL(Utils.getBaseUrl(), resourceType, actualId), headers, new HashMap<>(), formFields);
    }

    /**
     * Flips one operation's {@code x-auth-type} IN THE OPENAPI DEFINITION and PUTs the definition back
     * (PUT /apis/{id}/swagger) — the SWAGGER route to changing a resource's authentication type, as opposed to
     * setting {@code authType} on the API's {@code operations} array.
     *
     * <p>The two routes are different product surfaces and only this one proves the definition→operations
     * direction: APIM re-derives the API's URI templates from the uploaded definition, so an {@code x-auth-type} of
     * {@code None} here must land as {@code authType "None"} on the operation and then reach the gateway. Legacy
     * {@code DisableSecurityAndTryOutRESTResourceWithElkAnalyticsEnabledTestCase#testTurnOffSecurityAndInvokeGETResource}
     * drove exactly this by PUTting a whole hand-written swagger; this step instead reads the API's CURRENT
     * definition and mutates the one extension, so it cannot silently overwrite unrelated parts of the definition
     * (endpoints, security schemes, other paths) or drift when the generated definition changes shape.
     *
     * <p>The GET is an intermediate read consumed locally (CLAUDE.md §7) and is guarded before parsing; the PUT is
     * the response under test, published through {@code Requests.putMultipart} for the feature to assert.
     *
     * @param authType    the {@code x-auth-type} value to set (e.g. {@code None})
     * @param path        the OpenAPI path to mutate (e.g. {@code /customers/{id}})
     * @param verb        the HTTP verb under that path (case-insensitive; lower-cased for the OAS key)
     * @param resourceId  context key holding the API id
     */
    @When("I set x-auth-type {string} for path {string} verb {string} in the swagger of API {string}")
    public void iSetSwaggerAuthType(String authType, String path, String verb, String resourceId) throws IOException {

        String actualId = TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        String swaggerUrl = Utils.getSwaggerURL(Utils.getBaseUrl(), "apis", actualId);

        HttpResponse current = SimpleHTTPClient.getInstance().doGet(swaggerUrl, headers);
        Assert.assertTrue(current != null && current.getResponseCode() >= 200 && current.getResponseCode() < 300
                        && current.getData() != null && !current.getData().isBlank(),
                "could not read the swagger of API " + actualId + " before setting x-auth-type: got="
                        + (current == null ? "null" : current.getResponseCode() + "/" + current.getData()));

        JSONObject definition = new JSONObject(current.getData());
        JSONObject paths = definition.optJSONObject("paths");
        Assert.assertNotNull(paths, "swagger of API " + actualId + " has no 'paths' object: " + current.getData());
        JSONObject pathItem = paths.optJSONObject(path);
        Assert.assertNotNull(pathItem, "swagger of API " + actualId + " has no path '" + path + "'; paths present: "
                + paths.keySet());
        JSONObject operation = pathItem.optJSONObject(verb.toLowerCase());
        Assert.assertNotNull(operation, "swagger of API " + actualId + " has no '" + verb + "' under path '" + path
                + "'; verbs present: " + pathItem.keySet());
        operation.put("x-auth-type", authType);

        Map<String, String> formFields = new HashMap<>();
        formFields.put("apiDefinition", definition.toString());
        Requests.putMultipart(swaggerUrl, headers, new HashMap<>(), formFields);
    }

    /** Retrieves the publisher linter custom rules. */
    @When("I retrieve the linter custom rules")
    public void iRetrieveLinterCustomRules() throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.get(Utils.getLinterCustomRulesURL(Utils.getBaseUrl()), headers);
    }

    /**
     * Retrieves the publisher settings document (GET /settings), which advertises the tenant's resolved default
     * throttling policies ({@code defaultAdvancePolicy} / {@code defaultSubscriptionPolicy}) alongside the
     * environments and other publisher-UI settings. Non-asserting — the feature asserts the status and the fields.
     */
    @When("I retrieve the publisher settings")
    public void iRetrievePublisherSettings() throws IOException {

        Requests.get(Utils.getPublisherSettingsURL(Utils.getBaseUrl()), Identity.publisherHeaders());
    }

    /**
     * Asserts that EVERY operation of the API in the last (2xx) response carries exactly the given throttling
     * policy. The all-operations form matters for the default-tier substitution tests: the product applies the
     * resolved default to each tier-less resource, so an assertion on one operation would miss a partial
     * substitution. Reads back the shared {@code httpResponse}, so it follows a retrieve step.
     *
     * @param expectedPolicy the exact throttling policy name every operation must carry
     */
    @Then("Every operation in the response should have throttling policy {string}")
    public void everyOperationShouldHaveThrottlingPolicy(String expectedPolicy) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isBlank(),
                "Expected a 2xx API response with a body to read operations from, but got: "
                        + (response == null ? "null" : response.getResponseCode() + " / " + response.getData()));

        JSONArray operations = new JSONObject(response.getData()).optJSONArray("operations");
        Assert.assertTrue(operations != null && !operations.isEmpty(),
                "The API response carries no operations to assert a throttling policy on: " + response.getData());

        String expected = Utils.resolveContextPlaceholders(expectedPolicy);
        for (int i = 0; i < operations.length(); i++) {
            JSONObject operation = operations.getJSONObject(i);
            Assert.assertEquals(operation.optString("throttlingPolicy", null), expected,
                    "Operation " + operation.optString("verb") + " " + operation.optString("target")
                            + " carries the wrong throttling policy; all operations: " + operations);
        }
    }

    /** Retrieves the available publisher throttling policies for a policy level (subscription / api / application). */
    @When("I retrieve the publisher {string} throttling policies")
    public void iRetrievePublisherThrottlingPolicies(String policyLevel) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.get(Utils.getPublisherThrottlingPoliciesURL(Utils.getBaseUrl(), policyLevel), headers);
    }

    /**
     * Retrieves the STREAMING subscription throttling policies (GET /throttling-policies/streaming/subscription).
     *
     * <p>This endpoint IS the event-count filter: {@code ThrottlingPoliciesApiServiceImpl
     * #getSubscriptionThrottlingPolicies} lists the subscription-level policies and keeps only those whose default
     * quota policy type is {@code eventCount}. It therefore cannot be reached through the
     * {@code /throttling-policies/{policyLevel}} step above, whose path parameter accepts only
     * {@code subscription}/{@code api} and applies no quota filtering. (Legacy reached it via
     * {@code RestAPIPublisherImpl#getSubscriptionPolicies(tierQuotaTypes)}, which DISCARDS its argument and calls
     * the same unparameterised operation — so the legacy "quotaType=eventCount" was never on the wire at all;
     * the filtering has always been server-side.)</p>
     */
    @When("I retrieve the publisher streaming subscription throttling policies")
    public void iRetrieveStreamingSubscriptionThrottlingPolicies() throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.get(Utils.getPublisherThrottlingPoliciesURL(Utils.getBaseUrl(), "streaming/subscription"), headers);
    }

    /** Retrieves an API's OpenAPI definition (GET /apis/{id}/swagger). */
    @When("I retrieve the swagger of {string} resource {string}")
    public void iRetrieveSwagger(String resourceType, String resourceId) throws IOException {

        String actualId = TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.get(Utils.getSwaggerURL(Utils.getBaseUrl(), resourceType, actualId), headers);
    }

    /**
     * Validates an OpenAPI definition file (POST /apis/validate-openapi, multipart {@code file}). The response
     * carries an {@code isValid} flag; the feature asserts it (a valid def → isValid true; an invalid one →
     * isValid false).
     */
    @When("I validate the openapi definition from file {string}")
    public void iValidateOpenApiFromFile(String filepath) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("file", loadJsonResourceAsTempFile(filepath));
        Requests.postMultipart(Utils.getValidateOpenAPIURL(Utils.getBaseUrl()), headers, files,
                new HashMap<>());
    }

    /**
     * Attempts to import an OpenAPI definition without asserting success — for the invalid-definition negative
     * (an empty-resource-path OAS import → 400). Mirrors the positive import step but neither asserts nor
     * registers (nothing is created on failure).
     */
    @When("I attempt to import openapi definition from {string} with additional properties from {string}")
    public void iAttemptImportOpenApi(String filepath, String additionalData) throws IOException {
        importOpenApiDefinition(filepath, additionalData, null);
    }

    /**
     * Imports an API from an OpenAPI DEFINITION file (.json/.yaml, not an archive) and STORES the created API id
     * for the deploy/publish/subscribe/invoke arc that follows. Non-asserting (the feature asserts 201); on a
     * 2xx the id is stored under {@code resourceId} and registered for teardown. Used by the gateway
     * schema-validation port, which must import an OAS carrying the request/response schemas (a create-from-
     * payload API has only operation targets, not the body schemas the gateway validates against).
     *
     * @param filepath       classpath path to the OpenAPI definition
     * @param additionalData classpath path to the additional-properties JSON (name/context/endpoint/
     *                       enableSchemaValidation, ${UNIQUE} resolved)
     * @param resourceId     context key to store the created API id under (on success)
     */
    @When("I import openapi definition from {string} with additional properties {string} as {string}")
    public void iImportOpenApiAsResource(String filepath, String additionalData, String resourceId)
            throws IOException {
        importOpenApiDefinition(filepath, additionalData, resourceId);
    }

    /**
     * Imports an API from an OpenAPI definition already CAPTURED IN CONTEXT rather than read from a classpath
     * fixture, sending it through the {@code inlineAPIDefinition} form field of {@code POST /apis/import-openapi}
     * (the same wire shape the legacy {@code importOASDefinitionWithInlineContent} used). Non-asserting; on a 2xx
     * the id is stored under {@code resourceId} and registered for teardown, exactly as the file-based variant.
     *
     * <p>This is a different wire shape, not a convenience wrapper: the file variant sends the definition as a
     * binary {@code file} part, which cannot carry a definition the test only obtains at runtime. Its consumer is
     * the AI-API arc, where the definition must be the one the shipped AI service provider serves from
     * {@code /ai-service-providers/{id}/api-definition} — copying that 300 KB document into a fixture would make
     * every assertion about it a property of the copy.</p>
     *
     * @param definitionKey  context key holding the OpenAPI definition text (JSON or YAML)
     * @param additionalData classpath path to the additional-properties JSON ({@code ${UNIQUE}} resolved)
     * @param resourceId     context key to store the created API id under (on success)
     */
    @When("I import openapi definition captured as {string} with additional properties {string} as {string}")
    public void iImportOpenApiFromContextAsResource(String definitionKey, String additionalData, String resourceId)
            throws IOException {

        String definition = TestContext.resolve(definitionKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, String> formFields = new HashMap<>();
        formFields.put("inlineAPIDefinition", definition);
        formFields.put("additionalProperties", loadAdditionalProperties(additionalData));
        HttpResponse response = Requests.postMultipart(Utils.getImportOpenAPIURL(Utils.getBaseUrl()), headers, null,
                formFields);
        registerImportedApi(response, resourceId);
    }

    /** Shared OpenAPI-definition import (multipart {@code file} + {@code additionalProperties}); when
     *  {@code resourceId} is non-null and the import succeeds, stores + registers the created API id. */
    private void importOpenApiDefinition(String filepath, String additionalData, String resourceId)
            throws IOException {

        File openapiFile = loadJsonResourceAsTempFile(filepath);
        File additionalPropertiesFile = File.createTempFile("data", ".json");
        additionalPropertiesFile.deleteOnExit();
        Files.write(additionalPropertiesFile.toPath(),
                loadAdditionalProperties(additionalData).getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("file", openapiFile);
        files.put("additionalProperties", additionalPropertiesFile);
        HttpResponse response = Requests.postMultipart(Utils.getImportOpenAPIURL(Utils.getBaseUrl()), headers, files,
                null);
        registerImportedApi(response, resourceId);
    }

    /** Reads an additional-properties fixture off the classpath with {@code ${UNIQUE}} placeholders resolved. */
    private String loadAdditionalProperties(String additionalData) throws IOException {

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(additionalData)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Additional properties file not found: " + additionalData);
            }
            return Utils.resolvePayloadPlaceholders(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
        }
    }

    /** Stores + registers the id of an API created by an import, when the caller asked for one and it succeeded. */
    private void registerImportedApi(HttpResponse response, String resourceId) throws IOException {

        if (resourceId != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
            TestContext.set(resourceId, createdId);
            ResourceCleanup.register(Constants.CREATED_API_IDS, createdId);
        }
    }

    /**
     * Transitions an API through an arbitrary lifecycle action (e.g. {@code Block}, {@code Deprecate},
     * {@code Retire}, {@code Re-Publish}) via the publisher change-lifecycle API — the general form of the
     * publish-only {@code I publish the … resource} step. Does not assert the status itself, so the feature can
     * confirm it (a valid transition returns 200). Used by lifecycle-stage gateway invocation tests.
     */
    @When("I change the lifecycle of API {string} with action {string}")
    public void iChangeTheLifecycleOfApi(String apiId, String action) throws IOException {
        changeLifecycle("apis", apiId, action, null);
    }

    /**
     * Resource-typed variant of the lifecycle transition (e.g. {@code "api-products"}) — the change-lifecycle
     * endpoint keys on {@code apiProductId} for products. Non-asserting; the feature confirms the status.
     */
    @When("I change the lifecycle of {string} resource {string} with action {string}")
    public void iChangeTheLifecycleOfResource(String resourceType, String resourceId, String action) throws IOException {
        changeLifecycle(resourceType, resourceId, action, null);
    }

    /**
     * Lifecycle transition carrying a lifecycle-checklist option (the publisher's {@code lifecycleChecklist}
     * query param), e.g. {@code "Deprecate old versions after publishing the API:true"} or
     * {@code "Requires re-subscription when publishing the API:true"}. These options only take effect on the
     * {@code Publish} action; without a checklist the {@link #iChangeTheLifecycleOfApi} form is used. Non-asserting.
     */
    @When("I change the lifecycle of API {string} with action {string} and checklist {string}")
    public void iChangeTheLifecycleOfApiWithChecklist(String apiId, String action, String checklist)
            throws IOException {
        changeLifecycle("apis", apiId, action, checklist);
    }

    /**
     * Issues a SINGLE lifecycle-change POST and publishes whatever comes back, without retrying and without
     * asserting. Needed where the transition itself is the negative under test: {@link #iChangeTheLifecycleOfApi}
     * retries until the API reaches the action's target state and fails the test otherwise, so it can never be
     * used to pin a REJECTED transition. Ports the publish half of the APIM514 blank-tier case, where the
     * Publish is refused.
     */
    @When("I attempt to change the lifecycle of API {string} with action {string}")
    public void iAttemptToChangeTheLifecycleOfApi(String apiId, String action) throws IOException {

        String actualId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.post(Utils.getChangeLifecycleURL(Utils.getBaseUrl(), "apis", actualId, action, null), headers,
                null, null);
    }

    private void changeLifecycle(String resourceType, String resourceId, String action, String checklist)
            throws IOException {
        String actualId = TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        // A lifecycle-change POST can transiently fail (or be briefly rejected while a just-completed transition
        // settles) under parallel load on the shared container. This response used to be IGNORED, so a failed
        // Deprecate/Retire was SWALLOWED — the API silently stayed in its prior state and surfaced later as a
        // misleading "did not reach <state>" at the following lifecycle-status assertion. CI hit exactly this on
        // the Published->Deprecated->Retired arc (AccessibilityOfRetireAPITestCase): the Retire POST was lost and
        // the API stayed Deprecated. Mirror the publish step: retry the POST until it succeeds (2xx) — or, for an
        // API, until it already reads the action's target state (the POST may have applied despite a lost
        // response, and re-POSTing on an already-transitioned API faults) — catching only transient IOException.
        String url = Utils.getChangeLifecycleURL(Utils.getBaseUrl(), resourceType, actualId, action, checklist);
        String targetState = "apis".equals(resourceType) ? lifecycleTargetState(action) : null;
        long endTimeStart = System.currentTimeMillis();
        long endTime = endTimeStart + Constants.RUNTIME_PROPAGATION_TIMEOUT;
        HttpResponse response = null;
        boolean changed = false;
        while (true) {
            try {
                response = Requests.post(url, headers, null, null);
                if (response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
                    changed = true;
                    break;
                }
            } catch (IOException transientFailure) {
                // transient — fall through to the state check / retry
            }
            if (targetState != null && targetState.equals(currentApiLifecycleState(actualId, headers))) {
                changed = true;
                break;
            }
            if (System.currentTimeMillis() >= endTime) {
                break;
            }
            try {
                Utils.pollPause(endTimeStart, Constants.RETRY_INTERVAL_TIME);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Assert.assertTrue(changed, "Lifecycle-change '" + action + "' did not succeed for " + resourceType + " "
                + actualId + " within the deadline; last response: "
                + (response == null ? "null" : response.getResponseCode() + " / " + response.getData()));
    }

    /**
     * Maps a publisher lifecycle ACTION to the API state it transitions to — used by {@link #changeLifecycle}'s
     * retry to recognise a transition that already applied despite a lost response (re-POSTing then faults).
     * Returns {@code null} for actions without a simple 1:1 target state (the retry then relies on the 2xx POST).
     */
    private static String lifecycleTargetState(String action) {
        switch (action) {
            case "Publish":
            case "Re-Publish":
                return "Published";
            case "Deprecate":
                return "Deprecated";
            case "Retire":
                return "Retired";
            case "Block":
                return "Blocked";
            default:
                return null;
        }
    }

    /**
     * Builds an API-Product create payload that aggregates the operations of one or more existing APIs: for each
     * API it retrieves the API and embeds its {@code operations} under its own {@code ProductAPIDTO}, then wraps
     * them with the product's name/context/version/policies. (Products reference existing APIs + a selected set
     * of their resources.) A product over SEVERAL APIs is what the legacy suite exercised — the member APIs must
     * therefore carry DISJOINT resource paths, since a product cannot hold the same target/verb twice.
     */
    private String buildApiProductPayload(String name, String context, List<String> apiIds) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        JSONArray productApis = new JSONArray();
        for (String apiId : apiIds) {
            HttpResponse apiResp = SimpleHTTPClient.getInstance()
                    .doGet(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", apiId), headers);
            // Confirm the GET succeeded with a body BEFORE parsing — otherwise new JSONObject(null/"") throws an
            // opaque JSONException/NPE instead of a clear failure.
            Assert.assertTrue(apiResp != null && apiResp.getResponseCode() >= 200 && apiResp.getResponseCode() < 300
                            && apiResp.getData() != null && !apiResp.getData().isBlank(),
                    "Failed to fetch API '" + apiId + "' while building the API-product payload: expected a 2xx response "
                            + "with a body, got " + (apiResp == null ? "no response" : apiResp.getResponseCode()
                            + " / body=" + apiResp.getData()));
            JSONObject api = new JSONObject(apiResp.getData());
            JSONArray operations = api.optJSONArray("operations");
            productApis.put(new JSONObject()
                    .put("apiId", apiId)
                    .put("name", api.optString("name"))
                    .put("operations", operations == null ? new JSONArray() : operations));
        }
        return new JSONObject()
                .put("name", name)
                .put("context", context)
                .put("version", "1.0.0")
                // Offer the standard business plans so the shared "set up application …" composite (which
                // subscribes with Bronze) can subscribe to the product.
                .put("policies", new JSONArray().put("Gold").put("Bronze").put("Unlimited"))
                .put("apis", productApis)
                .toString();
    }

    /** Resolves a comma-separated list of API-id context keys to the ids they hold, in order. */
    private List<String> resolveApiIds(String apiIdKeysCsv) {
        List<String> apiIds = new ArrayList<>();
        for (String key : apiIdKeysCsv.split(",")) {
            if (!key.isBlank()) {
                apiIds.add(TestContext.resolve(key.trim()).toString());
            }
        }
        Assert.assertFalse(apiIds.isEmpty(), "No API id context keys given for the API product: " + apiIdKeysCsv);
        return apiIds;
    }

    /**
     * Creates an API Product aggregating the resources of an existing API, and stores its id
     * (registered for owner-aware teardown — swept before the underlying APIs). Asserts 201.
     */
    @When("I create an API product {string} with context {string} from API {string} as {string}")
    public void iCreateApiProduct(String nameBase, String contextBase, String apiIdKey, String productIdKey)
            throws IOException {
        iCreateApiProductFromApis(nameBase, contextBase, apiIdKey, productIdKey);
    }

    /**
     * Creates an API Product aggregating SEVERAL existing APIs (comma-separated API-id context keys) — the shape
     * the legacy suite used throughout (a product over apiOne + apiTwo). The member APIs must expose disjoint
     * resource paths. Asserts 201 and registers the product for owner-aware teardown.
     */
    @When("I create an API product {string} with context {string} from APIs {string} as {string}")
    public void iCreateApiProductFromApis(String nameBase, String contextBase, String apiIdKeysCsv,
                                          String productIdKey) throws IOException {

        String payload = buildApiProductPayload(Utils.resolvePayloadPlaceholders(nameBase),
                Utils.resolvePayloadPlaceholders(contextBase), resolveApiIds(apiIdKeysCsv));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        HttpResponse response = Requests.post(Utils.getAPICreateEndpointURL(Utils.getBaseUrl(), "api-products"), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        Object productId = Utils.extractValueFromPayload(response.getData(), "id");
        TestContext.set(productIdKey, productId);
        ResourceCleanup.register(Constants.CREATED_API_PRODUCT_IDS, productId);
    }

    /**
     * Non-asserting API-Product create (for negatives such as a malformed context → 400): stores the raw
     * response for the feature to assert and extracts no id, but an unexpectedly-created product IS registered
     * for teardown (§5) — {@code registerIfCreated} is a no-op on the expected 4xx.
     */
    @When("I attempt to create an API product {string} with context {string} from API {string}")
    public void iAttemptToCreateApiProduct(String nameBase, String contextBase, String apiIdKey) throws IOException {

        String payload = buildApiProductPayload(Utils.resolvePayloadPlaceholders(nameBase),
                Utils.resolvePayloadPlaceholders(contextBase), resolveApiIds(apiIdKey));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        ResourceCleanup.registerIfCreated(Constants.CREATED_API_PRODUCT_IDS,
                Requests.post(Utils.getAPICreateEndpointURL(Utils.getBaseUrl(), "api-products"), headers,
                        payload, Constants.CONTENT_TYPES.APPLICATION_JSON), "id");
    }

    /** Creates a new version (copy) of an API Product; stores the new product's id and registers it for teardown. */
    @When("I create a new version {string} of API product {string} with default version {string} as {string}")
    public void iCreateNewApiProductVersion(String newVersion, String productIdKey, String isDefault,
                                            String newProductIdKey) throws IOException {

        String productId = TestContext.resolve(productIdKey).toString();
        boolean defaultVersion = Boolean.parseBoolean(isDefault);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        HttpResponse response = Requests.post(Utils.getAPIProductNewVersionURL(Utils.getBaseUrl(), newVersion, defaultVersion, productId),
                        headers, null, null);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object newId = Utils.extractValueFromPayload(response.getData(), "id");
            TestContext.set(newProductIdKey, newId);
            ResourceCleanup.register(Constants.CREATED_API_PRODUCT_IDS, newId);
        }
    }

    /** Retrieves an API Product's swagger/OpenAPI definition, storing the raw response for assertions. */
    @When("I retrieve the API product swagger of {string}")
    public void iRetrieveApiProductSwagger(String productIdKey) throws IOException {

        String productId = TestContext.resolve(productIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.get(Utils.getAPIProductSwaggerURL(Utils.getBaseUrl(), productId), headers);
    }

    /** Lists an API's revisions (no filter). Non-asserting — the feature confirms the 200. */
    @When("I retrieve the revisions of {string} resource {string}")
    public void iRetrieveTheRevisions(String resourceType, String resourceId) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Requests.get(Utils.getRevisionURL(Utils.getBaseUrl(), resourceType, actualResourceId), headers);
    }

    /** Lists an API's currently-deployed revisions ({@code query=deployed:true}). Non-asserting. */
    @When("I retrieve the deployed revisions of {string} resource {string}")
    public void iRetrieveTheDeployedRevisions(String resourceType, String resourceId) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Requests.get(Utils.getRevisionDeployments(Utils.getBaseUrl(), resourceType, actualResourceId), headers);
    }

    /**
     * Undeploys a revision from the gateway environment. Non-asserting (a successful undeploy returns 201), so
     * the feature can confirm the code. Sends the same deployment descriptor shape as the deploy step.
     */
    @When("I undeploy revision {string} of {string} resource {string}")
    public void iUndeployRevision(String revisionId, String resourceType, String resourceId) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        String actualRevisionId = TestContext.resolve(revisionId).toString();
        String payload = "[{\"name\":\"" + System.getenv(Constants.GATEWAY_ENVIRONMENT)
                + "\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]";

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Requests.post(Utils.getRevisionUnDeploymentURL(Utils.getBaseUrl(), resourceType, actualResourceId, actualRevisionId),
                        headers, payload, Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Undeploys a revision using a caller-supplied deployment payload — needed to undeploy from a specific
     * (e.g. custom) environment/vhost rather than the default. Resolves {@code {{gatewayEnvironment}}} and any
     * {@code {{contextKey}}} placeholders (e.g. a captured custom environment name). Non-asserting.
     */
    @When("I undeploy revision {string} of {string} resource {string} with payload {string}")
    public void iUndeployRevisionGivenPayload(String revisionId, String resourceType, String resourceId,
                                              String payload) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        String actualRevisionId = TestContext.resolve(revisionId).toString();
        String jsonPayload = TestContext.resolve(payload).toString();
        jsonPayload = jsonPayload.replace("{{gatewayEnvironment}}", System.getenv(Constants.GATEWAY_ENVIRONMENT));
        jsonPayload = Utils.resolveContextPlaceholders(jsonPayload);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Requests.post(Utils.getRevisionUnDeploymentURL(Utils.getBaseUrl(), resourceType, actualResourceId, actualRevisionId),
                        headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Restores the API's working copy from a revision. Non-asserting (a successful restore returns 201). */
    @When("I restore revision {string} of {string} resource {string}")
    public void iRestoreRevision(String revisionId, String resourceType, String resourceId) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        String actualRevisionId = TestContext.resolve(revisionId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Requests.post(Utils.getRevisionRestoreURL(Utils.getBaseUrl(), resourceType, actualResourceId, actualRevisionId),
                        headers, null, null);
    }

    /**
     * Deletes a revision. Non-asserting so the feature can confirm BOTH the reject-while-deployed case (400)
     * and the successful delete after undeploy (200).
     */
    @When("I delete revision {string} of {string} resource {string}")
    public void iDeleteRevision(String revisionId, String resourceType, String resourceId) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        String actualRevisionId = TestContext.resolve(revisionId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Requests.delete(Utils.getRevisionByID(Utils.getBaseUrl(), resourceType, actualResourceId, actualRevisionId), headers);
    }

    /**
     * Generates a publisher-plane internal API key ({@code apis/{id}/generate-key}) and stores it. This is the
     * short-lived test key sent in the {@code Internal-Key} header — it lets a deployed-but-not-yet-published
     * (CREATED-stage) API be invoked for try-out, distinct from the devportal application API key.
     */
    @When("I generate an internal API key for API {string} and store it as {string}")
    public void iGenerateInternalApiKey(String apiId, String keyContextKey) throws IOException {

        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        HttpResponse response = Requests.post(Utils.getInternalAPIKey(Utils.getBaseUrl(), actualApiId), headers, null, null);
        // Assert success before extracting: generate-key returns 200 (APIKey). Without this, a non-2xx body has no
        // "apikey" field and surfaces as a confusing "Path 'apikey' not found" IOException instead of the status.
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        Object apiKey = Utils.extractValueFromPayload(response.getData(), "apikey");
        TestContext.set(Utils.normalizeContextKey(keyContextKey), apiKey);
    }

    /**
     * Reads the SOAP-to-REST conversion sequences ("resource policies") of ONE resource path + verb and publishes
     * the response for the feature to assert. {@code sequenceType} is {@code in} or {@code out}. The id may be an
     * API id OR a revision UUID — a revision's sequences are read through the same endpoint.
     *
     * @param sequenceType {@code in} or {@code out}
     * @param apiIdKey     context key holding the API (or revision) id
     * @param resourcePath the generated REST resource path (e.g. {@code sayHello})
     * @param verb         the HTTP verb of that resource (e.g. {@code post})
     */
    @When("I retrieve the {string} resource policies of API {string} for resource {string} verb {string}")
    public void iRetrieveResourcePolicies(String sequenceType, String apiIdKey, String resourcePath, String verb)
            throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Requests.get(Utils.getApiResourcePoliciesURL(Utils.getBaseUrl(), apiId, sequenceType, resourcePath, verb),
                headers);
    }

    /**
     * Snapshots the sequences of one resource path + verb into context as a canonical
     * {@code {"<resourcePath> <httpVerb>": "<content>"}} map, so a later read can be compared BYTE-IDENTICALLY.
     * That exact comparison is what catches silent sequence loss/rewrite across a provider change — a mere
     * "non-empty" check would pass on a regenerated-but-different sequence. Asserts the list is non-empty so the
     * baseline itself can never be vacuous. Intermediate read: consumed locally, never published as
     * {@code httpResponse} (§7).
     */
    @When("I snapshot the {string} resource policies of API {string} for resource {string} verb {string} as {string}")
    public void iSnapshotResourcePolicies(String sequenceType, String apiIdKey, String resourcePath, String verb,
                                          String snapshotKey) throws IOException {

        JSONObject snapshot = readResourcePolicyContents(sequenceType, apiIdKey, resourcePath, verb);
        Assert.assertTrue(snapshot.length() > 0, "No " + sequenceType + " resource policies returned for resource "
                + resourcePath + " " + verb + " — the byte-identical baseline would be vacuous");
        TestContext.set(Utils.normalizeContextKey(snapshotKey), snapshot.toString());
    }

    /**
     * Asserts the sequences of one resource path + verb are BYTE-IDENTICAL to an earlier snapshot — same set of
     * resource/verb keys and, for each, exactly the same content. Deliberately an equality assertion, not a
     * presence one.
     */
    @Then("The {string} resource policies of API {string} for resource {string} verb {string} should be byte-identical to snapshot {string}")
    public void theResourcePoliciesShouldMatchSnapshot(String sequenceType, String apiIdKey, String resourcePath,
                                                       String verb, String snapshotKey) throws IOException {

        JSONObject expected = new JSONObject(TestContext.resolve(snapshotKey).toString());
        JSONObject actual = readResourcePolicyContents(sequenceType, apiIdKey, resourcePath, verb);
        Assert.assertEquals(actual.keySet(), expected.keySet(),
                "The set of " + sequenceType + "-sequences changed for resource " + resourcePath + " " + verb);
        for (String key : expected.keySet()) {
            Assert.assertEquals(actual.getString(key), expected.getString(key),
                    "The " + sequenceType + "-sequence of [" + key + "] is not byte-identical to the snapshot");
        }
    }

    /**
     * Appends a marker comment to EVERY sequence of one resource path + verb (GET the list, then PUT each policy
     * back with the marker), proving the sequences remain UPDATABLE by the API's current owner. Each PUT is
     * asserted to return 200 with the marker echoed back; the last PUT is published as {@code httpResponse}.
     */
    @When("I append {string} to each {string} resource policy of API {string} for resource {string} verb {string}")
    public void iAppendToEachResourcePolicy(String marker, String sequenceType, String apiIdKey, String resourcePath,
                                            String verb) throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        JSONArray policies = fetchResourcePolicyList(sequenceType, apiId, resourcePath, verb);
        Assert.assertTrue(policies.length() > 0, "No " + sequenceType + " resource policies to update for resource "
                + resourcePath + " " + verb);
        for (int i = 0; i < policies.length(); i++) {
            JSONObject policy = policies.getJSONObject(i);
            JSONObject body = new JSONObject();
            body.put("id", policy.getString("id"));
            body.put("resourcePath", policy.getString("resourcePath"));
            body.put("httpVerb", policy.getString("httpVerb"));
            body.put("content", policy.getString("content") + "\n" + marker);

            HttpResponse response = Requests.put(
                    Utils.getApiResourcePolicyByIdURL(Utils.getBaseUrl(), apiId, policy.getString("id")),
                    headers, body.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
            Assert.assertEquals(response.getResponseCode(), 200, "Updating the " + sequenceType + "-sequence "
                    + policy.getString("id") + " failed: " + response.getData());
            Assert.assertTrue(response.getData() != null && response.getData().contains(marker),
                    "The updated " + sequenceType + "-sequence did not echo the marker: " + response.getData());
        }
    }

    /** Sequences of one resource path + verb as a canonical {@code {"<resourcePath> <httpVerb>": content}} map. */
    private JSONObject readResourcePolicyContents(String sequenceType, String apiIdKey, String resourcePath,
                                                  String verb) throws IOException {

        JSONArray policies = fetchResourcePolicyList(sequenceType, TestContext.resolve(apiIdKey).toString(),
                resourcePath, verb);
        JSONObject contents = new JSONObject();
        for (int i = 0; i < policies.length(); i++) {
            JSONObject policy = policies.getJSONObject(i);
            String key = policy.getString("resourcePath") + " " + policy.getString("httpVerb");
            // A duplicate key would silently overwrite and hide a lost sequence — fail instead.
            Assert.assertFalse(contents.has(key), "Duplicate resource policy key [" + key + "] in the "
                    + sequenceType + "-sequence list; the canonical snapshot would drop one");
            contents.put(key, policy.getString("content"));
        }
        return contents;
    }

    /**
     * Raw GET of the resource-policy list, guarded before parsing (§7). An intermediate read consumed inside the
     * calling step, so it uses the raw client and does NOT publish {@code httpResponse}.
     */
    private JSONArray fetchResourcePolicyList(String sequenceType, String apiId, String resourcePath, String verb)
            throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = SimpleHTTPClient.getInstance().doGet(
                Utils.getApiResourcePoliciesURL(Utils.getBaseUrl(), apiId, sequenceType, resourcePath, verb), headers);
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isBlank(),
                "Reading the " + sequenceType + " resource policies of " + apiId + " (" + resourcePath + " " + verb
                        + ") failed; got=" + (response == null ? "null"
                        : response.getResponseCode() + "/" + response.getData()));
        return new JSONObject(response.getData()).getJSONArray("list");
    }

}
