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

        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse apiCreateResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPICreateEndpointURL(getBaseUrl(), resourceType), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", apiCreateResponse);

        Assert.assertEquals(apiCreateResponse.getResponseCode(), 201, apiCreateResponse.getData());
        Object createdId = Utils.extractValueFromPayload(apiCreateResponse.getData(), "id");
        TestContext.set(resourceID, createdId);
        // Register for scenario teardown so a shared-server suite does not accumulate APIs across scenarios.
        TestContext.addToList(Constants.CREATED_API_IDS, createdId);
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

        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse apiCreateResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPICreateEndpointURL(getBaseUrl(), resourceType), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", apiCreateResponse);
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

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse apiUpdateResponse = SimpleHTTPClient.getInstance().doPut(
                Utils.getResourceEndpointURL(getBaseUrl(),resourceType ,actualResourceId), headers, jsonPayload,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", apiUpdateResponse);
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

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String jsonPayload = Utils.resolveFromContext(contextKey).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse createRevisionResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getRevisionURL(getBaseUrl(),resourceType, actualResourceId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        Assert.assertEquals(createRevisionResponse.getResponseCode(), 201, createRevisionResponse.getData());
        TestContext.set("httpResponse", createRevisionResponse);
        TestContext.set("revisionId", Utils.extractValueFromPayload(createRevisionResponse.getData(), "id"));
        Thread.sleep(3000);
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

        String actualResourceId= Utils.resolveFromContext(resourceId).toString();
        String actualRevisionId = Utils.resolveFromContext(revisionId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();
        jsonPayload = jsonPayload.replace("{{gatewayEnvironment}}", System.getenv(Constants.GATEWAY_ENVIRONMENT));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse deployRevisionResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getRevisionDeploymentURL(getBaseUrl(), resourceType, actualResourceId, actualRevisionId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", deployRevisionResponse);
    }

     /**
     * Deletes a resource (API, API Product, etc.) by its ID.
     *
     * @param resourceType Type of resource to delete (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID to delete
     */
    @When("I delete the {string} resource with id {string}")
    public void iDeleteTheResource(String resourceType, String resourceId) throws IOException {

        String actualResourceId= Utils.resolveFromContext(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse apiDeleteResponse = SimpleHTTPClient.getInstance().doDelete(Utils.getResourceEndpointURL(getBaseUrl(), resourceType,
                actualResourceId), headers);
        TestContext.set("httpResponse", apiDeleteResponse);
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

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());
        HttpResponse publishResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getChangeLifecycleURL(getBaseUrl(), resourceType, actualResourceId, "Publish", null), headers, null, null);
        TestContext.set("httpResponse", publishResponse);
    }

    /**
     * Retrieves the details of a specific resource by its ID.
     *
     * @param resourceType Type of resource to retrieve (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID to retrieve
     */
    @When("I retrieve the {string} resource with id {string}")
    public void iRetrieveTheResource(String resourceType, String resourceId) throws IOException {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getResourceEndpointURL(getBaseUrl(), resourceType, actualResourceId), headers);
        TestContext.set("httpResponse", response);
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

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPISearchEndpointURL(getBaseUrl(), null, null, null), headers);

        TestContext.set("httpResponse", response);
    }

    /**
     * Verifies that a specific API ID exists in the list of all APIs.
     *
     * @param apiId Context key containing the API ID to verify
     */
    @Then("The API with id {string} should be in the list of all APIS")
    public void theApiShouldBeInTheListOfAllApis(String apiId) {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
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

        String actualApiId = Utils.resolveFromContext(apiId).toString();
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
            lifecycleStatusResponse = SimpleHTTPClient.getInstance().doGet(url, headers);
            TestContext.set("httpResponse", lifecycleStatusResponse);
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
        baseSteps.putJsonPayloadInContext("<createRevisionPayload>","{\"description\":\"new Revision\"}");
        iCreateResourceRevision("apis", apiID , "<createRevisionPayload>");
        baseSteps.putJsonPayloadInContext("<deployRevisionPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        iDeployApiRevisionGivenPayload("<revisionId>", "apis" ,apiID, "<deployRevisionPayload>");
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

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String revisionId = Utils.resolveFromContext("revisionId").toString();

        String url = Utils.getRevisionDeployments(getBaseUrl(), resourceType, actualResourceId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        long endTime = System.currentTimeMillis() + Constants.DEPLOYMENT_WAIT_TIME;
        boolean deployed = false;

        while (System.currentTimeMillis() < endTime) {

            try {
                HttpResponse response = SimpleHTTPClient.getInstance().doGet(url, headers);

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

        String actualResourceID = Utils.resolveFromContext(resourceID).toString();
        Boolean defaultVersion = false;
        if (isDefault != null && !isDefault.isEmpty()) {
            defaultVersion = Boolean.parseBoolean(isDefault);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse apiNewVersionResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getNewAPIVersionURL(getBaseUrl(), resourceType, newVersion, defaultVersion, actualResourceID), headers, null, null);

        TestContext.set("httpResponse", apiNewVersionResponse);
        Object newVersionId = Utils.extractValueFromPayload(apiNewVersionResponse.getData(), "id");
        TestContext.set(newVersionID, newVersionId);
        // Register for scenario teardown so the version copy is cleaned up alongside the base API.
        TestContext.addToList(Constants.CREATED_API_IDS, newVersionId);
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

        String actualResourceID = Utils.resolveFromContext(resourceID).toString();
        boolean defaultVersion = isDefault != null && !isDefault.isEmpty() && Boolean.parseBoolean(isDefault);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getNewAPIVersionURL(getBaseUrl(), resourceType, newVersion, defaultVersion,
                        actualResourceID), headers, null, null);
        TestContext.set("httpResponse", response);
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

        String jsonPayload = Utils.resolveFromContext("<newDocumentPayload>").toString();
        String actualApiId = Utils.resolveFromContext(apiID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse documentCreationResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPIDocuments(getBaseUrl(), actualApiId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", documentCreationResponse);
        TestContext.set("documentID", Utils.extractValueFromPayload(documentCreationResponse.getData(), "documentId"));
    }

    /**
     * Retrieves all documents associated with an API.
     *
     * @param apiID Context key containing the API ID
     */
    @When("I retrieve all available documents for {string}")
    public void iRetrieveAllAvailableDocumentsFor(String apiID) throws IOException{

        String actualApiId = Utils.resolveFromContext(apiID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPIDocuments(getBaseUrl(), actualApiId), headers);
        TestContext.set("httpResponse", response);
    }

    /**
     * Retrieves a specific document by its ID for a given API.
     *
     * @param documentID Context key containing the document ID to retrieve
     * @param apiID Context key containing the API ID
     */
    @When("I retrieve document with {string} for {string}")
    public void iRetrieveDocumentWithFor(String documentID, String apiID) throws IOException{

        String documentId = Utils.resolveFromContext(documentID).toString();
        String actualApiId = Utils.resolveFromContext(apiID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPIDocument(getBaseUrl(), actualApiId, documentId), headers);
        TestContext.set("httpResponse", response);

    }

    /**
     * Deletes a document from an API.
     *
     * @param documentID Context key containing the document ID to delete
     * @param apiID Context key containing the API ID
     */
    @When("I delete the document with {string} for {string}")
    public void iDeleteTheDocumentWithFor(String documentID, String apiID) throws IOException{

        String documentId = Utils.resolveFromContext(documentID).toString();
        String actualApiId = Utils.resolveFromContext(apiID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getAPIDocument(getBaseUrl(), actualApiId, documentId), headers);
        TestContext.set("httpResponse", response);
    }

    /**
     * Updates a document using the payload stored in the test context.
     *
     * @param documentID Context key containing the document ID to update
     * @param apiID Context key containing the API ID
     */
    @And("I update the document with {string} for API {string}")
    public void iUpdateTheDocumentWithForAPI(String documentID, String apiID) throws IOException {

        String jsonPayload = Utils.resolveFromContext("<newDocumentPayload>").toString();
        String actualApiId = Utils.resolveFromContext(apiID).toString();
        String documentId = Utils.resolveFromContext(documentID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse documentUpdateResponse = SimpleHTTPClient.getInstance()
                .doPut(Utils.getAPIDocument(getBaseUrl(), actualApiId, documentId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", documentUpdateResponse);
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
        Object ctxValue = Utils.resolveFromContext(resourceUpdatePayload);
        JSONObject jsonPayload = (ctxValue instanceof JSONObject)
                ? (JSONObject) ctxValue
                : new JSONObject(ctxValue.toString());

        if ("endpointConfig".equals(configType)){
            configValue = Utils.resolveFromContext(configValue).toString();
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

        String subscriptionId = Utils.resolveFromContext(subscriptionID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse documentUpdateResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getSubscriptionBlockingURL(getBaseUrl(), subscriptionId), headers, null, null);

        TestContext.set("httpResponse", documentUpdateResponse);
    }

    /**
     * Unblocks a previously blocked subscription, allowing it to be used for API invocation again.
     *
     * @param subscriptionID Context key containing the subscription ID to unblock
     */
    @When("I unblock the subscription with {string} for the resource")
    public void iUnblockTheSubscriptionWithForTheResource(String subscriptionID) throws IOException {

        String subscriptionId = Utils.resolveFromContext(subscriptionID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse documentUpdateResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getSubscriptionUnBlockingURL(getBaseUrl(), subscriptionId), headers, null, null);

        TestContext.set("httpResponse", documentUpdateResponse);
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

        String jsonPayload = Utils.resolveFromContext("<newSharedScope>").toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse scopeCreationResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPIScopes(getBaseUrl()), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", scopeCreationResponse);
        Object scopeId = Utils.extractValueFromPayload(scopeCreationResponse.getData(), "id");
        TestContext.set("scopeID", scopeId);
        // Register for teardown: a shared scope is a tenant-wide resource that ResourceCleanup must remove so
        // it does not leak (and 409 a re-run on the same container) if the scenario fails before deleting it.
        if (scopeId != null) {
            TestContext.addToList(Constants.CREATED_SHARED_SCOPE_IDS, scopeId);
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

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPIScopes(getBaseUrl()), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /**
     * Deletes a shared scope by its ID.
     *
     * @param scopeID Context key containing the scope ID to delete
     */
    @When("I delete shared scope with {string}")
    public void iDeleteSharedScopeWith(String scopeID) throws IOException {

        String scopeId = Utils.resolveFromContext(scopeID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getAPIScopesById(getBaseUrl(), scopeId), headers);
        TestContext.set("httpResponse", response);
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

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPIScopes(getBaseUrl()), headers);

        TestContext.set("httpResponse", response);

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

        String additionalProperties = Utils.resolveFromContext(additionalPropertiesKey).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        Map<String, String> formFields = new HashMap<>();
        formFields.put("type", "GRAPHQL");
        formFields.put("additionalProperties", additionalProperties);

        Map<String, File> files = new HashMap<>();
        files.put("file", schemaFile);

        HttpResponse apiCreateResponse = SimpleHTTPClient.getInstance()
                .doPostMultipartWithFiles(Utils.getGraphQLSchema(getBaseUrl()), headers, files, formFields);

        TestContext.set("httpResponse", apiCreateResponse);
        Assert.assertEquals(apiCreateResponse.getResponseCode(), 201, apiCreateResponse.getData());
        Object createdId = Utils.extractValueFromPayload(apiCreateResponse.getData(), "id");
        TestContext.set(apiID, createdId);
        // Register for scenario teardown so a shared-server suite does not accumulate APIs across scenarios.
        TestContext.addToList(Constants.CREATED_API_IDS, createdId);
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
        String actualApiId = Utils.resolveFromContext(apiId).toString();
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

        HttpResponse policyCreateResponse = SimpleHTTPClient.getInstance()
                .doPostMultipartWithFiles(endpointUrl, headers, files, null);

        TestContext.set("httpResponse", policyCreateResponse);

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
                            TestContext.addToList(Constants.CREATED_OPERATION_POLICY_IDS, createdId);
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

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getCommonPolicy(getBaseUrl()), headers);

        TestContext.set("httpResponse", response);
    }

    /**
     * Deletes an API-specific operation policy.
     *
     * @param apiId Context key containing the API ID
     * @param policyId Context key containing the policy ID to delete
     */
    @When("I delete the api {string} specific policy {string}")
    public void iDeleteTheApiSpecificPolicy(String apiId, String policyId) throws IOException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String policyID = Utils.resolveFromContext(policyId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.publisherToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getAPISpecificPolicyById(getBaseUrl(), actualApiId, policyID), headers);
        TestContext.set("httpResponse", response);
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

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPostMultipartWithFiles(Utils.getAPIDefinitionURL(getBaseUrl()), headers, files, null);

        TestContext.set("httpResponse", response);
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
        TestContext.set(resourceId, createdId);
        // Register for scenario teardown so imported APIs do not accumulate across scenarios on a shared server.
        TestContext.addToList(Constants.CREATED_API_IDS, createdId);
    }

}
