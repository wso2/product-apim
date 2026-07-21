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
import io.cucumber.datatable.DataTable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.FileNotFoundException;

public class PublisherBaseSteps {

    private static final Logger logger = LoggerFactory.getLogger(PublisherBaseSteps.class);

    BaseSteps baseSteps = new BaseSteps();

    private String getBaseUrl() {

        return baseSteps.getBaseUrl();
    }

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

        HttpResponse apiCreateResponse = Requests.post(Utils.getAPICreateEndpointURL(getBaseUrl(), resourceType), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        Assert.assertEquals(apiCreateResponse.getResponseCode(), 201, apiCreateResponse.getData());
        Object createdId = Utils.extractValueFromPayload(apiCreateResponse.getData(), "id");
        TestContext.set(resourceID, createdId);
        // Register for scenario teardown so a shared-server suite does not accumulate APIs across scenarios.
        ResourceCleanup.register(Constants.CREATED_API_IDS, createdId);
    }

    /**
     * Attempts to create an API without asserting success, storing the raw response as {@code httpResponse}
     * so the feature can assert the resulting status itself. Unlike {@code iCreateAnAPIWithPayloadAs} (which
     * asserts 201 and registers the API for cleanup), this is for negative / access-control scenarios where
     * the create is expected to be rejected (e.g. a subscriber-role user receiving 401/403) — so it neither
     * asserts a status nor registers an id (nothing is created to clean up).
     */
    @When("I attempt to create an {string} resource with payload {string}")
    public void iAttemptToCreateAnAPIWithPayload(String resourceType, String payload) throws IOException {

        String jsonPayload = TestContext.resolve(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Requests.post(Utils.getAPICreateEndpointURL(getBaseUrl(), resourceType), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Attempts to create a resource with NO Authorization header — the unauthenticated-create negative (401).
     * Non-asserting; the feature asserts the status.
     */
    @When("I attempt to create an {string} resource with payload {string} without authentication")
    public void iAttemptToCreateAnAPIWithoutAuth(String resourceType, String payload) throws IOException {

        String jsonPayload = TestContext.resolve(payload).toString();

        Requests.post(Utils.getAPICreateEndpointURL(getBaseUrl(), resourceType), new HashMap<>(), jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
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
                Utils.getResourceEndpointURL(getBaseUrl(),resourceType ,actualResourceId), headers, jsonPayload,
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
        String url = Utils.getRevisionURL(getBaseUrl(), resourceType, actualResourceId);
        long endTime = System.currentTimeMillis() + Constants.DEPLOYMENT_WAIT_TIME;
        HttpResponse createRevisionResponse = null;
        while (true) {
            try {
                createRevisionResponse = Requests.post(url, headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);
                if (createRevisionResponse.getResponseCode() == 201) {
                    break;
                }
            } catch (IOException transientDuringIndexing) {
                // transient connectivity during warm-up — retry
            }
            if (System.currentTimeMillis() >= endTime) {
                break;
            }
            Thread.sleep(Constants.RETRY_INTERVAL_TIME);
        }

        Assert.assertNotNull(createRevisionResponse,
                "Revision creation never returned a response for " + resourceType + " " + actualResourceId);
        Assert.assertEquals(createRevisionResponse.getResponseCode(), 201, createRevisionResponse.getData());
        TestContext.set("revisionId", Utils.extractValueFromPayload(createRevisionResponse.getData(), "id"));
        Thread.sleep(3000);
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

        Requests.post(Utils.getRevisionURL(getBaseUrl(), resourceType, actualResourceId), headers, jsonPayload,
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

        Requests.post(Utils.getRevisionDeploymentURL(getBaseUrl(), resourceType, actualResourceId, actualRevisionId), headers, jsonPayload,
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

        Requests.delete(Utils.getResourceEndpointURL(getBaseUrl(), resourceType,
                actualResourceId), headers);
    }

    /**
     * Publishes a resource, changing its lifecycle state to "PUBLISHED".
     * A published resource becomes available in the Developer Portal for subscription.
     *
     * @param resourceType Type of resource to publish (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID to publish
     */
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
        Requests.post(Utils.getGenerateMockScriptsURL(getBaseUrl(), actualApiId), headers, "", null);
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
        Requests.get(Utils.getGeneratedMockScriptsURL(getBaseUrl(), actualApiId), headers);
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
        Requests.head(Utils.getValidateRoleURL(getBaseUrl(), role), headers);
    }

    /**
     * Force-changes a subscription's business plan via the publisher endpoint
     * (POST /subscriptions/change-business-plan?subscriptionId=&throttlingPolicy=). Unlike the devportal
     * subscription PUT (which silently ignores an invalid plan → 200), this endpoint validates the plan.
     * Ports ChangeSubscriptionBusinessPlanForcefullyTestCase. Non-asserting.
     *
     * @param subId context key holding the subscription id
     * @param plan  the throttling policy / business plan to set
     */
    @When("I change the subscription business plan of {string} to {string}")
    public void iChangeSubscriptionBusinessPlan(String subId, String plan) throws IOException {
        String actualSubId = TestContext.resolve(subId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.post(Utils.getChangeSubscriptionBusinessPlanURL(getBaseUrl(), actualSubId, plan), headers, "", null);
    }

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
        String url = Utils.getChangeLifecycleURL(getBaseUrl(), resourceType, actualResourceId, "Publish", null);
        long endTime = System.currentTimeMillis() + Constants.DEPLOYMENT_WAIT_TIME;
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
                Thread.sleep(Constants.RETRY_INTERVAL_TIME);
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
     * Reads an API's current lifecycle state (e.g. {@code Created}/{@code Published}) via a direct GET that is
     * NOT published as {@code httpResponse} — an intermediate read consumed locally by the publish retry loop.
     * Returns {@code null} on any non-2xx/empty/transient response so the caller keeps polling.
     */
    private String currentApiLifecycleState(String apiId, Map<String, String> headers) {
        try {
            HttpResponse response = SimpleHTTPClient.getInstance()
                    .doGet(Utils.getAPILifecycleStateURL(getBaseUrl(), apiId), headers);
            if (response != null && response.getResponseCode() == 200
                    && response.getData() != null && !response.getData().isEmpty()) {
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

        Requests.get(Utils.getResourceEndpointURL(getBaseUrl(), resourceType, actualResourceId), headers);
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
        Requests.post(Utils.getValidateEndpointURL(getBaseUrl(), resolvedEndpoint, actualApiId), headers, "",
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

        Requests.get(Utils.getAPISearchEndpointURL(getBaseUrl(), null, null, null), headers);
    }

    /**
     * Verifies that a specific API ID exists in the list of all APIs.
     *
     * @param apiId Context key containing the API ID to verify
     */
    @Then("The API with id {string} should be in the list of all APIS")
    public void theApiShouldBeInTheListOfAllApis(String apiId) {

        String actualApiId = TestContext.resolve(apiId).toString();
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        // Guard before parsing — a cleared/failed list retrieval must fail clearly, not as an NPE/JSONException.
        Assert.assertTrue(response != null && response.getData() != null && !response.getData().isEmpty(),
                "No API-list response with a body captured to search for API '" + actualApiId + "' in");
        JSONArray apisList = new JSONObject(response.getData()).getJSONArray("list");

        boolean found = IntStream.range(0, apisList.length())
                .mapToObj(apisList::getJSONObject)
                .anyMatch(subJson -> actualApiId.equals(subJson.optString("id", null)));

        Assert.assertTrue(found, "API with id " + actualApiId + " not found in the list.");
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

        // Lifecycle changes (publish/deploy) propagate asynchronously, so the state can briefly lag a publish
        // call — poll until it reaches the expected value rather than asserting on a single GET (the latter is
        // a flaky race that wider parallel load on the shared container exposes).
        String url = Utils.getAPILifecycleStateURL(getBaseUrl(), actualApiId);
        HttpResponse lifecycleStatusResponse = null;
        String actualState = null;
        for (int attempt = 1; attempt <= Constants.MAX_RETRIES; attempt++) {
            lifecycleStatusResponse = Requests.get(url, headers);
            // Only parse a 200 that actually has a body; a non-2xx/empty response during warm-up falls through
            // and we keep polling rather than throwing an uncaught JSONException.
            if (lifecycleStatusResponse != null && lifecycleStatusResponse.getResponseCode() == 200
                    && lifecycleStatusResponse.getData() != null && !lifecycleStatusResponse.getData().isEmpty()) {
                actualState = new JSONObject(lifecycleStatusResponse.getData()).optString("state", null);
                if (status.equals(actualState)) {
                    return;
                }
            }
            try {
                Thread.sleep(Constants.RETRY_INTERVAL_TIME);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Assert.assertEquals(actualState, status,
                "API lifecycle state did not reach '" + status + "' within the retry window");
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
        HttpResponse response = Requests.get(Utils.getAPILifecycleStateURL(getBaseUrl(), actualApiId), headers);
        Assert.assertTrue(response != null && response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isEmpty(),
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
        HttpResponse response = Requests.get(Utils.getAPILifecycleHistoryURL(getBaseUrl(), actualApiId), headers);
        Assert.assertTrue(response != null && response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isEmpty(),
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
     * Waits until a specific revision is deployed in the gateway.
     * This step polls the deployment status endpoint until the revision appears in the deployed revisions list,
     * with a timeout mechanism to prevent indefinite waiting.
     *
     * @param resourceType Type of resource (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID
     */
    @Then("I wait until {string} {string} revision is deployed in the gateway")
    public void waitUntilRevisionIsDeployed(String resourceType, String resourceId) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        String revisionId = TestContext.resolve("revisionId").toString();

        String url = Utils.getRevisionDeployments(getBaseUrl(), resourceType, actualResourceId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        long endTime = System.currentTimeMillis() + Constants.DEPLOYMENT_WAIT_TIME;
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
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException ignored) {
                            }
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
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
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

        HttpResponse apiNewVersionResponse = Requests.post(Utils.getNewAPIVersionURL(getBaseUrl(), resourceType, newVersion, defaultVersion, actualResourceID), headers, null, null);
        Object newVersionId = Utils.extractValueFromPayload(apiNewVersionResponse.getData(), "id");
        TestContext.set(newVersionID, newVersionId);
        // Register for scenario teardown so the version copy is cleaned up alongside the base API.
        ResourceCleanup.register(Constants.CREATED_API_IDS, newVersionId);
    }

    /**
     * Attempts to create a new version without asserting success, storing the raw response as
     * {@code httpResponse} for the feature to assert. For negative / access-control scenarios where the
     * version-create is expected to be rejected (e.g. a subscriber-role token receiving 401): unlike the
     * positive step it neither extracts an id (the error body has none) nor registers anything for cleanup.
     */
    @When("I attempt to create a new version {string} of {string} resource {string} with default version {string}")
    public void iAttemptToCreateANewVersionOfAPI(String newVersion, String resourceType, String resourceID,
                                                 String isDefault) throws IOException {

        String actualResourceID = TestContext.resolve(resourceID).toString();
        boolean defaultVersion = isDefault != null && !isDefault.isEmpty() && Boolean.parseBoolean(isDefault);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Requests.post(Utils.getNewAPIVersionURL(getBaseUrl(), resourceType, newVersion, defaultVersion,
                actualResourceID), headers, null, null);
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

        HttpResponse documentCreationResponse = Requests.post(Utils.getAPIDocuments(getBaseUrl(), actualApiId), headers, jsonPayload,
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

        Requests.get(Utils.getAPIDocuments(getBaseUrl(), actualApiId), headers);
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

        Requests.get(Utils.getAPIDocument(getBaseUrl(), actualApiId, documentId), headers);

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

        Requests.delete(Utils.getAPIDocument(getBaseUrl(), actualApiId, documentId), headers);
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

        Requests.put(Utils.getAPIDocument(getBaseUrl(), actualApiId, documentId), headers, jsonPayload,
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
                Utils.getAPIDocumentContent(getBaseUrl(), actualApiId, docId), headers,
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
                Utils.getAPIDocumentContent(getBaseUrl(), actualApiId, docId), headers, files, new HashMap<>());
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
            configValue = TestContext.resolve(configValue).toString();
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

        Requests.post(Utils.getSubscriptionBlockingURL(getBaseUrl(), subscriptionId), headers, null, null);
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

        Requests.post(Utils.getSubscriptionUnBlockingURL(getBaseUrl(), subscriptionId), headers, null, null);
    }

    /**
     * Creates a new shared scope in APIM.
     * Shared scopes can be used across multiple APIs to define common authorization scopes.
     * The scope ID is stored in the test context after creation.
     *
     * @param scopeName Name of the shared scope to create
     */
    @When("I create a new shared scope as {string}")
    public void iCreateANewSharedScopeAs(String scopeName) throws IOException{

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

        HttpResponse scopeCreationResponse = Requests.post(Utils.getAPIScopes(getBaseUrl()), headers, jsonPayload,
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
     * Attempts to create a shared scope without asserting success, storing the raw response as
     * {@code httpResponse} for the feature to assert. For negative / access-control scenarios where the
     * create is expected to be rejected (e.g. a subscriber-role token receiving 401): unlike the positive
     * step it neither extracts an id (the error body has none) nor registers anything for cleanup.
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

        Requests.post(Utils.getAPIScopes(getBaseUrl()), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
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

        Requests.delete(Utils.getAPIScopesById(getBaseUrl(), scopeId), headers);
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
                .doGet(Utils.getAPIScopesById(getBaseUrl(), scopeId), headers);
        // Confirm the GET succeeded with a body BEFORE parsing/mutating — otherwise new JSONObject(null/"") throws
        // an opaque JSONException/NPE instead of a clear failure.
        Assert.assertTrue(current != null && current.getResponseCode() >= 200 && current.getResponseCode() < 300
                        && current.getData() != null && !current.getData().isEmpty(),
                "Failed to fetch shared scope '" + scopeId + "' before update: expected a 2xx response with a body, got "
                        + (current == null ? "no response" : current.getResponseCode() + " / body="
                        + current.getData()));
        JSONObject scope = new JSONObject(current.getData());
        scope.put("description", newDescription);

        Requests.put(Utils.getAPIScopesById(getBaseUrl(), scopeId), headers, scope.toString(),
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

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse response = Requests.get(Utils.getAPIScopes(getBaseUrl()), headers);

        // Confirm the GET succeeded with a body BEFORE parsing — otherwise new JSONObject(null/"") throws an
        // opaque JSONException/NPE instead of a clear failure.
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isEmpty(),
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

        HttpResponse apiCreateResponse = Requests.postMultipart(Utils.getGraphQLSchema(getBaseUrl()), headers,
                files, formFields);
        Assert.assertEquals(apiCreateResponse.getResponseCode(), 201, apiCreateResponse.getData());
        Object createdId = Utils.extractValueFromPayload(apiCreateResponse.getData(), "id");
        TestContext.set(apiID, createdId);
        // Register for scenario teardown so a shared-server suite does not accumulate APIs across scenarios.
        ResourceCleanup.register(Constants.CREATED_API_IDS, createdId);
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

        HttpResponse response = Requests.postMultipart(Utils.getGraphQLSchema(getBaseUrl()), headers,
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

        String validateUrl = Utils.getValidateGraphQLSchemaURL(getBaseUrl()) + "?useIntrospection=" + useIntrospection;
        HttpResponse response = Requests.postMultipart(validateUrl, headers, new HashMap<>(), formFields);
        // Confirm the validate call succeeded with a body BEFORE parsing — otherwise the graphQLInfo drill-down
        // throws an opaque JSONException/NPE instead of a clear failure.
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isEmpty(),
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

        HttpResponse response = Requests.postMultipart(Utils.getGraphQLSchema(getBaseUrl()), headers, files,
                formFields);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
            TestContext.set(apiID, createdId);
            ResourceCleanup.register(Constants.CREATED_API_IDS, createdId);
        }
    }

    /** Loads a .graphql (or any) resource off the classpath into a temp file for multipart upload. */
    private File loadResourceAsTempFile(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }
            File temp = File.createTempFile("gql-schema", ".graphql");
            temp.deleteOnExit();
            Files.copy(inputStream, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return temp;
        }
    }

    /** Retrieves a GraphQL API's schema definition (publisher), storing the raw response for assertions. */
    @When("I retrieve the GraphQL schema of API {string}")
    public void iRetrieveGraphQLSchemaOfApi(String apiIdKey) throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Requests.get(Utils.getGraphQLSchemaOfApiURL(getBaseUrl(), apiId), headers);
    }

    /** Updates a GraphQL API's schema definition (publisher, PUT multipart {@code schemaDefinition}). */
    @When("I update the GraphQL schema of API {string} with schema file {string}")
    public void iUpdateGraphQLSchemaOfApi(String apiIdKey, String schemaFilePath) throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Map<String, File> files = new HashMap<>();
        files.put("schemaDefinition", loadResourceAsTempFile(schemaFilePath));

        Requests.putMultipart(Utils.getGraphQLSchemaOfApiURL(getBaseUrl(), apiId), headers,
                files, new HashMap<>());
    }

    /** Validates a GraphQL schema file (publisher, POST multipart {@code file}), storing the raw response. */
    @When("I validate the GraphQL schema file {string}")
    public void iValidateGraphQLSchemaFile(String schemaFilePath) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Map<String, File> files = new HashMap<>();
        files.put("file", loadResourceAsTempFile(schemaFilePath));

        Requests.postMultipart(Utils.getValidateGraphQLSchemaURL(getBaseUrl()), headers,
                files, new HashMap<>());
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
            endpointUrl = Utils.getCommonPolicy(getBaseUrl());
        } else {
            // API-specific policy
            endpointUrl = Utils.getAPISpecificPolicy(getBaseUrl(), apiId);
        }

        HttpResponse policyCreateResponse = Requests.postMultipart(endpointUrl, headers, files, null);

        // Extract and store the policy ID from response if available
        if (policyId != null && policyCreateResponse.getResponseCode() == 201) {
            String responseData = policyCreateResponse.getData();
            if (responseData != null && !responseData.isEmpty()) {
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

        Requests.get(Utils.getCommonPolicy(getBaseUrl()), headers);
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
                Utils.getCommonPolicyExportURL(getBaseUrl(), name, version, format), headers, ".zip");
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
                Utils.getCommonPolicyExportURL(getBaseUrl(), name, version, format), headers, ".zip");
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
        HttpResponse response = Requests.delete(Utils.getCommonPolicyById(getBaseUrl(), policyId), headers);
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
        HttpResponse response = Requests.postMultipart(Utils.getCommonPolicyImportURL(getBaseUrl()), headers, files,
                null);
        // A successful import (201) returns the recreated policy JSON carrying its new id; store and register it so
        // the re-imported policy is swept by teardown.
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300
                && response.getData() != null && !response.getData().isEmpty()) {
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
    public void iExportApiToArchive(String apiId, String archivePathKey) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        SimpleHTTPClient.DownloadResult result = Requests.getToFile(
                Utils.getApiExportURL(getBaseUrl(), actualApiId, "JSON"), headers, ".zip");
        Assert.assertEquals(result.getStatusCode(), 200,
                "API export did not return 200 (archive download failed)");
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

        HttpResponse response = Requests.postMultipart(Utils.getApiArchiveImportURL(getBaseUrl()), headers, files,
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

        String url = Utils.getApiArchiveImportURL(getBaseUrl()) + "?preserveProvider=" + preserveProvider.trim();
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
                Utils.getApiExportURL(getBaseUrl(), actualApiId, "JSON"), headers, ".zip");
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
        String actualApiId = TestContext.resolve(apiId).toString();
        // The publisher API's provider field carries a tenant user's full username (e.g. admin@tenant1.com) but
        // strips the carbon.super suffix for a super-tenant user (e.g. ppImporter, not ppImporter@carbon.super).
        String expectedProvider = Identity.resolveActor(actorRef).getUserName();
        String superSuffix = "@" + Constants.SUPER_TENANT_DOMAIN;
        if (expectedProvider.endsWith(superSuffix)) {
            expectedProvider = expectedProvider.substring(0, expectedProvider.length() - superSuffix.length());
        }
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = Requests.get(Utils.getResourceEndpointURL(getBaseUrl(), "apis", actualApiId), headers);
        Assert.assertTrue(response != null && response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isEmpty(),
                "API fetch failed for api=" + actualApiId + " got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        String actualProvider = new JSONObject(response.getData()).getString("provider");
        Assert.assertEquals(actualProvider, expectedProvider,
                "API provider mismatch for api=" + actualApiId);
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
        String url = Utils.getAPISearchEndpointURL(getBaseUrl(), "name:" + resolvedName, null, null);
        long endTime = System.currentTimeMillis() + Constants.DEPLOYMENT_WAIT_TIME;
        Object id = null;
        while (true) {
            try {
                HttpResponse response = Requests.get(url, headers);
                if (response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isEmpty()) {
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
            Thread.sleep(2000);
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
        File sequenceFile = loadResourceAsTempFile(sequencePath, ".xml");
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("sequence", sequenceFile);
        Map<String, String> formFields = new HashMap<>();
        formFields.put("type", type);
        Requests.putMultipart(Utils.getSequenceBackendURL(getBaseUrl(), actualApiId), headers, files, formFields);
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
        File wsdlFile = loadResourceAsTempFile(wsdlPath, suffix);
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
        HttpResponse response = Requests.postMultipart(Utils.getImportWsdlURL(getBaseUrl()), headers, files,
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
        HttpResponse response = Requests.postMultipart(Utils.getImportWsdlURL(getBaseUrl()), headers,
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
        Requests.get(Utils.getWsdlOfApiURL(getBaseUrl(), actualApiId), headers);
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
        Requests.get(Utils.getDevPortalWsdlOfApiURL(getBaseUrl(), actualApiId, environmentName), headers);
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
     */
    @Given("I create {int} APIs with production endpoint {string} named {string}")
    public void iCreateApisWithProductionEndpoint(int count, String endpointUrl, String namePrefixRef)
            throws IOException {
        String prefix = Utils.resolveContextPlaceholders(namePrefixRef);
        String resolvedEndpoint = Utils.resolveContextPlaceholders(endpointUrl);
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
        }
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
        HttpResponse response = Requests.get(Utils.getResourceEndpointURL(getBaseUrl(), "apis", actualApiId), headers);
        Assert.assertTrue(response != null && response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isEmpty(),
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

        Requests.delete(Utils.getAPISpecificPolicyById(getBaseUrl(), actualApiId, policyID), headers);
    }

    /** Resolves a shipped/created COMMON operation policy (its {@code id}/{@code name}/{@code version}) by display
     *  name (GET /operation-policies). Returns the list entry so callers get the version too — the attach path
     *  validates policyName+policyVersion against the spec identified by policyId (a missing version → 400). */
    private JSONObject resolveCommonPolicyByName(String policyName, Map<String, String> headers) throws IOException {
        HttpResponse listResp = SimpleHTTPClient.getInstance().doGet(Utils.getCommonPolicy(getBaseUrl()), headers);
        Assert.assertTrue(listResp != null && listResp.getResponseCode() >= 200 && listResp.getResponseCode() < 300
                        && listResp.getData() != null && !listResp.getData().isEmpty(),
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
                .doGet(Utils.getResourceEndpointURL(getBaseUrl(), "apis", actualApiId), headers);
        Assert.assertTrue(getApi != null && getApi.getResponseCode() >= 200 && getApi.getResponseCode() < 300
                        && getApi.getData() != null && !getApi.getData().isEmpty(),
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

        Requests.put(Utils.getResourceEndpointURL(getBaseUrl(), "apis", actualApiId), headers, api.toString(),
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
                .doGet(Utils.getResourceEndpointURL(getBaseUrl(), "apis", actualApiId), headers);
        Assert.assertTrue(getApi != null && getApi.getResponseCode() == 200
                        && getApi.getData() != null && !getApi.getData().isEmpty(),
                "Failed to fetch API '" + actualApiId + "' for clone md5 check: got "
                        + (getApi == null ? "no response" : getApi.getResponseCode() + " / " + getApi.getData()));
        JSONObject api = new JSONObject(getApi.getData());
        String clonedPolicyId = api.getJSONArray("operations").getJSONObject(opIndex)
                .getJSONObject("operationPolicies").getJSONArray("request").getJSONObject(0).getString("policyId");
        Assert.assertNotEquals(clonedPolicyId, commonPolicyId,
                "Attaching a common policy should clone it to the API level with a NEW id");

        // md5 of the common policy vs the API-specific clone must match (identical content).
        HttpResponse commonResp = SimpleHTTPClient.getInstance()
                .doGet(Utils.getCommonPolicyById(getBaseUrl(), commonPolicyId), headers);
        Assert.assertTrue(commonResp != null && commonResp.getResponseCode() == 200
                        && commonResp.getData() != null && !commonResp.getData().isEmpty(),
                "Failed to fetch common policy '" + commonPolicyId + "': got "
                        + (commonResp == null ? "no response" : commonResp.getResponseCode() + " / " + commonResp.getData()));
        HttpResponse clonedResp = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPISpecificPolicyById(getBaseUrl(), actualApiId, clonedPolicyId), headers);
        Assert.assertTrue(clonedResp != null && clonedResp.getResponseCode() == 200
                        && clonedResp.getData() != null && !clonedResp.getData().isEmpty(),
                "Failed to fetch API-specific clone '" + clonedPolicyId + "': got "
                        + (clonedResp == null ? "no response" : clonedResp.getResponseCode() + " / " + clonedResp.getData()));
        String commonMd5 = new JSONObject(commonResp.getData()).getString("md5");
        String clonedMd5 = new JSONObject(clonedResp.getData()).getString("md5");
        Assert.assertEquals(clonedMd5, commonMd5,
                "Cloned API-level policy md5 must match the common policy md5");
    }

    /**
     * Extracts a previously-downloaded common-operation-policy archive (path stored under {@code archivePathKey})
     * and asserts it contains a spec file named {@code <policyName>.<ext>} whose content parses as the given
     * format ({@code json} or {@code yaml}), plus the {@code <policyName>.j2} synapse template. Ports the
     * archive-content assertion of OperationPolicyTestCase#testCommonOperationPolicyExportWithJSONContent (JSON)
     * and complements the YAML round-trip already covered.
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
        HttpResponse response = Requests.postMultipart(Utils.getCommonPolicyImportURL(getBaseUrl()), headers, files,
                null);
        Assert.assertEquals(response.getResponseCode(), expectedStatus,
                "Malformed common operation policy import status mismatch (body=" + response.getData() + ")");
        // If the server unexpectedly created a policy, register it so teardown removes it (defensive).
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300
                && response.getData() != null && !response.getData().isEmpty()) {
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
                .doGet(Utils.getResourceEndpointURL(getBaseUrl(), "apis", actualApiId), headers);
        Assert.assertTrue(getApi != null && getApi.getResponseCode() >= 200 && getApi.getResponseCode() < 300
                        && getApi.getData() != null && !getApi.getData().isEmpty(),
                "Failed to fetch API '" + actualApiId + "' before updating operation policy parameters: got "
                        + (getApi == null ? "no response" : getApi.getResponseCode() + " / " + getApi.getData()));
        JSONObject api = new JSONObject(getApi.getData());
        JSONArray flowPolicies = api.getJSONArray("operations").getJSONObject(opIndex)
                .getJSONObject("operationPolicies").getJSONArray(flow);
        Assert.assertTrue(flowPolicies.length() > 0,
                "No operation policy present in flow '" + flow + "' of operation " + opIndex + " to update");
        flowPolicies.getJSONObject(0).put("parameters", new JSONObject(paramsJson));

        Requests.put(Utils.getResourceEndpointURL(getBaseUrl(), "apis", actualApiId), headers, api.toString(),
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
                .doGet(Utils.getResourceEndpointURL(getBaseUrl(), "apis", actualApiId), headers);
        Assert.assertTrue(getApi != null && getApi.getResponseCode() == 200
                        && getApi.getData() != null && !getApi.getData().isEmpty(),
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

        HttpResponse response = Requests.postMultipart(Utils.getAPIDefinitionURL(getBaseUrl()), headers, files, null);
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

        File archiveFile = loadResourceAsTempFile(archivePath, ".zip");

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

        HttpResponse response = Requests.postMultipart(Utils.getAPIDefinitionURL(getBaseUrl()), headers, files,
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

        File asyncApiFile = loadResourceAsTempFile(filepath, ".yaml");
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
        HttpResponse response = Requests.postMultipart(Utils.getImportAsyncApiURL(getBaseUrl()), headers, files,
                null);
        if (resourceId != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
            TestContext.set(resourceId, createdId);
            ResourceCleanup.register(Constants.CREATED_API_IDS, createdId);
        }
    }

    /** Loads a classpath resource into a temp .json file (for multipart OAS upload). */
    private File loadJsonResourceAsTempFile(String resourcePath) throws IOException {
        return loadResourceAsTempFile(resourcePath, ".json");
    }

    /** Loads a classpath resource into a temp file PRESERVING a given suffix — needed when the server cares
     *  about the uploaded file's extension (e.g. {@code .png} thumbnails, {@code .zip} OpenAPI archives). */
    private File loadResourceAsTempFile(String resourcePath, String suffix) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }
            File temp = File.createTempFile("res", suffix);
            temp.deleteOnExit();
            Files.copy(inputStream, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return temp;
        }
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
        files.put("file", loadResourceAsTempFile(imagePath, ".png"));
        // Thumbnail upload is a PUT (updateAPIThumbnail), not POST — a POST returns 405.
        Requests.putMultipart(Utils.getThumbnailURL(getBaseUrl(), actualApiId), headers,
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
        Requests.get(Utils.getThumbnailURL(getBaseUrl(), actualApiId), headers);
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
                Utils.getSwaggerURL(getBaseUrl(), resourceType, actualId), headers, new HashMap<>(), formFields);
    }

    /** Retrieves the publisher linter custom rules. */
    @When("I retrieve the linter custom rules")
    public void iRetrieveLinterCustomRules() throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.get(Utils.getLinterCustomRulesURL(getBaseUrl()), headers);
    }

    /** Retrieves the available publisher throttling policies for a policy level (subscription / api / application). */
    @When("I retrieve the publisher {string} throttling policies")
    public void iRetrievePublisherThrottlingPolicies(String policyLevel) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.get(Utils.getPublisherThrottlingPoliciesURL(getBaseUrl(), policyLevel), headers);
    }

    /** Retrieves an API's OpenAPI definition (GET /apis/{id}/swagger). */
    @When("I retrieve the swagger of {string} resource {string}")
    public void iRetrieveSwagger(String resourceType, String resourceId) throws IOException {

        String actualId = TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.get(Utils.getSwaggerURL(getBaseUrl(), resourceType, actualId), headers);
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
        Requests.postMultipart(Utils.getValidateOpenAPIURL(getBaseUrl()), headers, files,
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

    /** Shared OpenAPI-definition import (multipart {@code file} + {@code additionalProperties}); when
     *  {@code resourceId} is non-null and the import succeeds, stores + registers the created API id. */
    private void importOpenApiDefinition(String filepath, String additionalData, String resourceId)
            throws IOException {

        File openapiFile = loadJsonResourceAsTempFile(filepath);
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
        files.put("file", openapiFile);
        files.put("additionalProperties", additionalPropertiesFile);
        HttpResponse response = Requests.postMultipart(Utils.getAPIDefinitionURL(getBaseUrl()), headers, files,
                null);
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
        String url = Utils.getChangeLifecycleURL(getBaseUrl(), resourceType, actualId, action, checklist);
        String targetState = "apis".equals(resourceType) ? lifecycleTargetState(action) : null;
        long endTime = System.currentTimeMillis() + Constants.DEPLOYMENT_WAIT_TIME;
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
                Thread.sleep(Constants.RETRY_INTERVAL_TIME);
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
     * Builds an API-Product create payload that aggregates an existing API's operations: retrieves the API,
     * embeds its {@code operations} under a single {@code ProductAPIDTO}, and wraps it with the product's
     * name/context/version/policies. (Products reference existing APIs + a selected set of their resources.)
     */
    private String buildApiProductPayload(String name, String context, String apiId) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse apiResp = SimpleHTTPClient.getInstance()
                .doGet(Utils.getResourceEndpointURL(getBaseUrl(), "apis", apiId), headers);
        // Confirm the GET succeeded with a body BEFORE parsing — otherwise new JSONObject(null/"") throws an
        // opaque JSONException/NPE instead of a clear failure.
        Assert.assertTrue(apiResp != null && apiResp.getResponseCode() >= 200 && apiResp.getResponseCode() < 300
                        && apiResp.getData() != null && !apiResp.getData().isEmpty(),
                "Failed to fetch API '" + apiId + "' while building the API-product payload: expected a 2xx response "
                        + "with a body, got " + (apiResp == null ? "no response" : apiResp.getResponseCode()
                        + " / body=" + apiResp.getData()));
        JSONObject api = new JSONObject(apiResp.getData());
        JSONArray operations = api.optJSONArray("operations");
        JSONObject productApi = new JSONObject()
                .put("apiId", apiId)
                .put("name", api.optString("name"))
                .put("operations", operations == null ? new JSONArray() : operations);
        return new JSONObject()
                .put("name", name)
                .put("context", context)
                .put("version", "1.0.0")
                // Offer the standard business plans so the shared "set up application …" composite (which
                // subscribes with Bronze) can subscribe to the product.
                .put("policies", new JSONArray().put("Gold").put("Bronze").put("Unlimited"))
                .put("apis", new JSONArray().put(productApi))
                .toString();
    }

    /**
     * Creates an API Product aggregating the resources of an existing API, and stores its id
     * (registered for owner-aware teardown — swept before the underlying APIs). Asserts 201.
     */
    @When("I create an API product {string} with context {string} from API {string} as {string}")
    public void iCreateApiProduct(String nameBase, String contextBase, String apiIdKey, String productIdKey)
            throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        String payload = buildApiProductPayload(Utils.resolvePayloadPlaceholders(nameBase),
                Utils.resolvePayloadPlaceholders(contextBase), apiId);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        HttpResponse response = Requests.post(Utils.getAPICreateEndpointURL(getBaseUrl(), "api-products"), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        Object productId = Utils.extractValueFromPayload(response.getData(), "id");
        TestContext.set(productIdKey, productId);
        ResourceCleanup.register(Constants.CREATED_API_PRODUCT_IDS, productId);
    }

    /**
     * Non-asserting API-Product create (for negatives such as a malformed context → 400): stores the raw
     * response for the feature to assert; does not extract an id or register anything.
     */
    @When("I attempt to create an API product {string} with context {string} from API {string}")
    public void iAttemptToCreateApiProduct(String nameBase, String contextBase, String apiIdKey) throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        String payload = buildApiProductPayload(Utils.resolvePayloadPlaceholders(nameBase),
                Utils.resolvePayloadPlaceholders(contextBase), apiId);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Requests.post(Utils.getAPICreateEndpointURL(getBaseUrl(), "api-products"), headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Creates a new version (copy) of an API Product; stores the new product's id and registers it for teardown. */
    @When("I create a new version {string} of API product {string} with default version {string} as {string}")
    public void iCreateNewApiProductVersion(String newVersion, String productIdKey, String isDefault,
                                            String newProductIdKey) throws IOException {

        String productId = TestContext.resolve(productIdKey).toString();
        boolean defaultVersion = Boolean.parseBoolean(isDefault);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        HttpResponse response = Requests.post(Utils.getAPIProductNewVersionURL(getBaseUrl(), newVersion, defaultVersion, productId),
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
        Requests.get(Utils.getAPIProductSwaggerURL(getBaseUrl(), productId), headers);
    }

    /** Lists an API's revisions (no filter). Non-asserting — the feature confirms the 200. */
    @When("I retrieve the revisions of {string} resource {string}")
    public void iRetrieveTheRevisions(String resourceType, String resourceId) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Requests.get(Utils.getRevisionURL(getBaseUrl(), resourceType, actualResourceId), headers);
    }

    /** Lists an API's currently-deployed revisions ({@code query=deployed:true}). Non-asserting. */
    @When("I retrieve the deployed revisions of {string} resource {string}")
    public void iRetrieveTheDeployedRevisions(String resourceType, String resourceId) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Requests.get(Utils.getRevisionDeployments(getBaseUrl(), resourceType, actualResourceId), headers);
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

        Requests.post(Utils.getRevisionUnDeploymentURL(getBaseUrl(), resourceType, actualResourceId, actualRevisionId),
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

        Requests.post(Utils.getRevisionUnDeploymentURL(getBaseUrl(), resourceType, actualResourceId, actualRevisionId),
                        headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Restores the API's working copy from a revision. Non-asserting (a successful restore returns 201). */
    @When("I restore revision {string} of {string} resource {string}")
    public void iRestoreRevision(String revisionId, String resourceType, String resourceId) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        String actualRevisionId = TestContext.resolve(revisionId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Requests.post(Utils.getRevisionRestoreURL(getBaseUrl(), resourceType, actualResourceId, actualRevisionId),
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

        Requests.delete(Utils.getRevisionByID(getBaseUrl(), resourceType, actualResourceId, actualRevisionId), headers);
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

        HttpResponse response = Requests.post(Utils.getInternalAPIKey(getBaseUrl(), actualApiId), headers, null, null);
        // Assert success before extracting: generate-key returns 200 (APIKey). Without this, a non-2xx body has no
        // "apikey" field and surfaces as a confusing "Path 'apikey' not found" IOException instead of the status.
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        Object apiKey = Utils.extractValueFromPayload(response.getData(), "apikey");
        TestContext.set(Utils.normalizeContextKey(keyContextKey), apiKey);
    }

}
