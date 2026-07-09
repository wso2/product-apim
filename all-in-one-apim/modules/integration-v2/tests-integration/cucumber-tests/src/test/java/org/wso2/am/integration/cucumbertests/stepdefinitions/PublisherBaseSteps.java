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
import java.util.Map;
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

        HttpResponse apiCreateResponse = Requests.post(Utils.getAPICreateEndpointURL(getBaseUrl(), resourceType), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Attempts to create a resource with NO Authorization header — the unauthenticated-create negative (401).
     * Non-asserting; the feature asserts the status.
     */
    @When("I attempt to create an {string} resource with payload {string} without authentication")
    public void iAttemptToCreateAnAPIWithoutAuth(String resourceType, String payload) throws IOException {

        String jsonPayload = TestContext.resolve(payload).toString();

        HttpResponse response = Requests.post(Utils.getAPICreateEndpointURL(getBaseUrl(), resourceType), new HashMap<>(), jsonPayload,
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

        HttpResponse apiUpdateResponse = Requests.put(
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

        HttpResponse createRevisionResponse = Requests.post(Utils.getRevisionURL(getBaseUrl(),resourceType, actualResourceId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

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

        HttpResponse response = Requests.post(Utils.getRevisionURL(getBaseUrl(), resourceType, actualResourceId), headers, jsonPayload,
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

        HttpResponse deployRevisionResponse = Requests.post(Utils.getRevisionDeploymentURL(getBaseUrl(), resourceType, actualResourceId, actualRevisionId), headers, jsonPayload,
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

        HttpResponse apiDeleteResponse = Requests.delete(Utils.getResourceEndpointURL(getBaseUrl(), resourceType,
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
        HttpResponse response = Requests.post(Utils.getGenerateMockScriptsURL(getBaseUrl(), actualApiId), headers, "", null);
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
        HttpResponse response = Requests.get(Utils.getGeneratedMockScriptsURL(getBaseUrl(), actualApiId), headers);
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
        HttpResponse response = Requests.head(Utils.getValidateRoleURL(getBaseUrl(), role), headers);
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
        HttpResponse response = Requests.post(Utils.getChangeSubscriptionBusinessPlanURL(getBaseUrl(), actualSubId, plan), headers, "", null);
    }

    @When("I publish the {string} resource with id {string}")
    public void iPublishTheResource(String resourceType, String resourceId) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());
        HttpResponse publishResponse = Requests.post(Utils.getChangeLifecycleURL(getBaseUrl(), resourceType, actualResourceId, "Publish", null), headers, null, null);
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

        HttpResponse response = Requests.get(Utils.getResourceEndpointURL(getBaseUrl(), resourceType, actualResourceId), headers);
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
            actualState = new JSONObject(lifecycleStatusResponse.getData()).optString("state", null);
            if (status.equals(actualState)) {
                return;
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
                            Thread.sleep(10000);
                            break;
                        }
                    }
                }

                if (deployed) {
                    break;
                }

            } catch (Exception e) {
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

        HttpResponse response = Requests.get(Utils.getAPIDocuments(getBaseUrl(), actualApiId), headers);
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

        HttpResponse response = Requests.get(Utils.getAPIDocument(getBaseUrl(), actualApiId, documentId), headers);

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

        HttpResponse response = Requests.delete(Utils.getAPIDocument(getBaseUrl(), actualApiId, documentId), headers);
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

        HttpResponse documentUpdateResponse = Requests.put(Utils.getAPIDocument(getBaseUrl(), actualApiId, documentId), headers, jsonPayload,
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

        HttpResponse response = Requests.postMultipart(
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

        HttpResponse documentUpdateResponse = Requests.post(Utils.getSubscriptionBlockingURL(getBaseUrl(), subscriptionId), headers, null, null);
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

        HttpResponse documentUpdateResponse = Requests.post(Utils.getSubscriptionUnBlockingURL(getBaseUrl(), subscriptionId), headers, null, null);
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

        HttpResponse response = Requests.post(Utils.getAPIScopes(getBaseUrl()), headers, jsonPayload,
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

        HttpResponse response = Requests.delete(Utils.getAPIScopesById(getBaseUrl(), scopeId), headers);
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

        HttpResponse response = Requests.put(Utils.getAPIScopesById(getBaseUrl(), scopeId), headers, scope.toString(),
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
        String sdl = new org.json.JSONObject(response.getData())
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

        HttpResponse response = Requests.get(Utils.getGraphQLSchemaOfApiURL(getBaseUrl(), apiId), headers);
    }

    /** Updates a GraphQL API's schema definition (publisher, PUT multipart {@code schemaDefinition}). */
    @When("I update the GraphQL schema of API {string} with schema file {string}")
    public void iUpdateGraphQLSchemaOfApi(String apiIdKey, String schemaFilePath) throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Map<String, File> files = new HashMap<>();
        files.put("schemaDefinition", loadResourceAsTempFile(schemaFilePath));

        HttpResponse response = Requests.putMultipart(Utils.getGraphQLSchemaOfApiURL(getBaseUrl(), apiId), headers,
                files, new HashMap<>());
    }

    /** Validates a GraphQL schema file (publisher, POST multipart {@code file}), storing the raw response. */
    @When("I validate the GraphQL schema file {string}")
    public void iValidateGraphQLSchemaFile(String schemaFilePath) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Map<String, File> files = new HashMap<>();
        files.put("file", loadResourceAsTempFile(schemaFilePath));

        HttpResponse response = Requests.postMultipart(Utils.getValidateGraphQLSchemaURL(getBaseUrl()), headers,
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

        HttpResponse response = Requests.get(Utils.getCommonPolicy(getBaseUrl()), headers);
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
        HttpResponse response = Requests.putMultipart(Utils.getThumbnailURL(getBaseUrl(), actualApiId), headers,
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
        HttpResponse response = Requests.get(Utils.getThumbnailURL(getBaseUrl(), actualApiId), headers);
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
        HttpResponse response = Requests.putMultipart(
                Utils.getSwaggerURL(getBaseUrl(), resourceType, actualId), headers, new HashMap<>(), formFields);
    }

    /** Retrieves the publisher linter custom rules. */
    @When("I retrieve the linter custom rules")
    public void iRetrieveLinterCustomRules() throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = Requests.get(Utils.getLinterCustomRulesURL(getBaseUrl()), headers);
    }

    /** Retrieves the available publisher throttling policies for a policy level (subscription / api / application). */
    @When("I retrieve the publisher {string} throttling policies")
    public void iRetrievePublisherThrottlingPolicies(String policyLevel) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = Requests.get(Utils.getPublisherThrottlingPoliciesURL(getBaseUrl(), policyLevel), headers);
    }

    /** Retrieves an API's OpenAPI definition (GET /apis/{id}/swagger). */
    @When("I retrieve the swagger of {string} resource {string}")
    public void iRetrieveSwagger(String resourceType, String resourceId) throws IOException {

        String actualId = TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = Requests.get(Utils.getSwaggerURL(getBaseUrl(), resourceType, actualId), headers);
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
        HttpResponse response = Requests.postMultipart(Utils.getValidateOpenAPIURL(getBaseUrl()), headers, files,
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
        changeLifecycle("apis", apiId, action);
    }

    /**
     * Resource-typed variant of the lifecycle transition (e.g. {@code "api-products"}) — the change-lifecycle
     * endpoint keys on {@code apiProductId} for products. Non-asserting; the feature confirms the status.
     */
    @When("I change the lifecycle of {string} resource {string} with action {string}")
    public void iChangeTheLifecycleOfResource(String resourceType, String resourceId, String action) throws IOException {
        changeLifecycle(resourceType, resourceId, action);
    }

    private void changeLifecycle(String resourceType, String resourceId, String action) throws IOException {
        String actualId = TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = Requests.post(Utils.getChangeLifecycleURL(getBaseUrl(), resourceType, actualId, action, null), headers,
                        null, null);
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

        HttpResponse response = Requests.post(Utils.getAPICreateEndpointURL(getBaseUrl(), "api-products"), headers, payload,
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
        HttpResponse response = Requests.get(Utils.getAPIProductSwaggerURL(getBaseUrl(), productId), headers);
    }

    /** Lists an API's revisions (no filter). Non-asserting — the feature confirms the 200. */
    @When("I retrieve the revisions of {string} resource {string}")
    public void iRetrieveTheRevisions(String resourceType, String resourceId) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        HttpResponse response = Requests.get(Utils.getRevisionURL(getBaseUrl(), resourceType, actualResourceId), headers);
    }

    /** Lists an API's currently-deployed revisions ({@code query=deployed:true}). Non-asserting. */
    @When("I retrieve the deployed revisions of {string} resource {string}")
    public void iRetrieveTheDeployedRevisions(String resourceType, String resourceId) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        HttpResponse response = Requests.get(Utils.getRevisionDeployments(getBaseUrl(), resourceType, actualResourceId), headers);
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

        HttpResponse response = Requests.post(Utils.getRevisionUnDeploymentURL(getBaseUrl(), resourceType, actualResourceId, actualRevisionId),
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

        HttpResponse response = Requests.post(Utils.getRevisionUnDeploymentURL(getBaseUrl(), resourceType, actualResourceId, actualRevisionId),
                        headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Restores the API's working copy from a revision. Non-asserting (a successful restore returns 201). */
    @When("I restore revision {string} of {string} resource {string}")
    public void iRestoreRevision(String revisionId, String resourceType, String resourceId) throws IOException {

        String actualResourceId = TestContext.resolve(resourceId).toString();
        String actualRevisionId = TestContext.resolve(revisionId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        HttpResponse response = Requests.post(Utils.getRevisionRestoreURL(getBaseUrl(), resourceType, actualResourceId, actualRevisionId),
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

        HttpResponse response = Requests.delete(Utils.getRevisionByID(getBaseUrl(), resourceType, actualResourceId, actualRevisionId), headers);
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
