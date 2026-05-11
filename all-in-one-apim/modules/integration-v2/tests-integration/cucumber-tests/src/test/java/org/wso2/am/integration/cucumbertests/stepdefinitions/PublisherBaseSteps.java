/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.wso2.am.integration.cucumbertests.utils.RequestAction;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.FileNotFoundException;

public class PublisherBaseSteps {

    private final String baseUrl;
    private static final Logger logger = LoggerFactory.getLogger(PublisherBaseSteps.class);

    public PublisherBaseSteps() {
        baseUrl = TestContext.get("baseUrl").toString();
    }

    BaseSteps baseSteps = new BaseSteps();


    /**
     * Creates a new resource (API, API Product, etc.) using a JSON payload and stores
     * both the HTTP response and the created resource ID in the test context.
     *
     * @param resourceType Type of resource to create (e.g., "apis", "api-products")
     * @param payload Context key containing the resource creation JSON payload
     */
    @And("I create an {string} resource with payload {string}")
    public void iCreateAnAPIWithPayloadAs(String resourceType, String payload)  {

        String jsonPayload = Utils.resolveFromContext(payload).toString();
        String url = Utils.getAPICreateEndpointURL(baseUrl, resourceType);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPost(url, headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
            } catch (IOException e) {
                throw new RuntimeException("API Resource creation failed", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction) ;
    }

    /**
     * Updates an existing resource using a JSON payload stored in the test context.
     *
     * @param resourceType Type of resource to update (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID to update
     * @param payload Context key containing the resource update JSON payload
     */
    @When("I update {string} resource of id {string} with payload {string}")
    public void iUpdateResourceWithJsonPayloadFromContext(String resourceType, String resourceId, String payload)  {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();
        String url = Utils.getResourceEndpointURL(baseUrl, resourceType, actualResourceId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPut(url, headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
            } catch (IOException e) {
                throw new RuntimeException("Resource update failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Verifies that the HTTP response contains the expected API policies.
     *
     * @param dataTable Data table containing policy type and expected policy name pairs
     */
    @Then("The response should contain the following api policies")
    public void theResponseShouldContainFollowingApiPolicies(DataTable dataTable) throws IOException {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        String responsePayload = response.getData();
        JsonNode apiPolicies = new ObjectMapper().readTree(responsePayload).path("apiPolicies");

        Map<String, String> expectedPolicies = dataTable.asMap();

        for (Map.Entry<String, String> expectedPolicy : expectedPolicies.entrySet()) {
            validatePolicy(apiPolicies, expectedPolicy.getKey(), expectedPolicy.getValue());
        }
    }

    /**
     * Validates that the given API policies JSON contains a policy of the specified
     * type with the expected policy name and a non-empty policy ID.
     *
     * @param apiPolicies        JSON node containing API policies
     * @param expectedPolicyName Expected policy name
     * @param policyType         Type of policy
     */
    private void validatePolicy(JsonNode apiPolicies, String policyType, String expectedPolicyName) {

        JsonNode receivedPolicesArray = apiPolicies.path(policyType);

        boolean policyExists = false;
        for (JsonNode node : receivedPolicesArray) {
            String policyName = node.path("policyName").asText(null);
            String policyId = node.path("policyId").asText(null);
            if (expectedPolicyName.equals(policyName) && StringUtils.isNotBlank(policyId)) {
                policyExists = true;
                break;
            }
        }
        Assert.assertTrue(policyExists, "Policy '" + expectedPolicyName + "' not found or missing policyId under apiPolicies['" + policyType
                + "']");
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
    public void iCreateResourceRevision(String resourceType, String resourceId, String contextKey) {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String jsonPayload = Utils.resolveFromContext(contextKey).toString();
        String url = Utils.getRevisionURL(baseUrl, resourceType, actualResourceId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPost(url, headers, jsonPayload,
                                Constants.CONTENT_TYPES.APPLICATION_JSON);
            } catch (IOException e) {
                throw new RuntimeException("Revision creation failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Retrieves the list of existing revisions for a resource and extracts the first revision ID.
     *
     * @param resourceType Type of resource (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID
     */
    @When("I get the existing revision for {string} resource with {string}")
    public void iGetTheExistingRevisionForResourceWith( String resourceType, String resourceId)  {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String url = Utils.getRevisionURL(baseUrl, resourceType, actualResourceId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doGet(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("Failed to retrieve revisions: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Deletes a specific revision of a resource.
     *
     * @param resourceType Type of resource (e.g., "apis", "api-products")
     * @param revisionId Context key containing the revision ID to delete
     * @param resourceId Context key containing the resource ID
     */
    @When("I Delete the {string} resource revision with {string} for {string}")
    public void iDeleteTheRevisionWithFor(String resourceType, String revisionId, String resourceId)  {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String actualRevisionId = Utils.resolveFromContext(revisionId).toString();
        String url = Utils.getRevisionByID(baseUrl, resourceType, actualResourceId, actualRevisionId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doDelete(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("Revision deletion failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
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
    public void iDeployApiRevisionGivenPayload(String revisionId, String resourceType, String resourceId, String payload)  {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String actualRevisionId = Utils.resolveFromContext(revisionId).toString();
        String url = Utils.getRevisionDeploymentURL(baseUrl, resourceType, actualResourceId, actualRevisionId);
        
        String initialJsonPayload = Utils. resolveFromContext(payload).toString();
        String jsonPayload = initialJsonPayload.replace("{{gatewayEnvironment}}", 
                System.getenv(Constants.GATEWAY_ENVIRONMENT));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPost(url, headers, jsonPayload,
                                Constants.CONTENT_TYPES.APPLICATION_JSON);
            } catch (IOException e) {
                throw new RuntimeException("Revision deployment failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Undeploy a specific revision of a resource from the gateway environment.
     *
     * @param revisionId Context key containing the revision ID to undeploy
     * @param resourceType Type of resource (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID
     */
    @When("I undeploy revision {string} of {string} resource {string}")
    public void iUndeployRevision(String revisionId, String resourceType, String resourceId)  {

        baseSteps.putJsonPayloadInContext("<undeployRevisionPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String actualRevisionId = Utils.resolveFromContext(revisionId).toString();
        String url = Utils.getRevisionUnDeploymentURL(baseUrl, resourceType, actualResourceId, actualRevisionId);
        
        String initialJsonPayload = Utils.resolveFromContext("<undeployRevisionPayload>").toString();
        String jsonPayload = initialJsonPayload.replace("{{gatewayEnvironment}}", 
                System.getenv(Constants.GATEWAY_ENVIRONMENT));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPost(url, headers, jsonPayload,
                                Constants.CONTENT_TYPES.APPLICATION_JSON);
            } catch (IOException e) {
                throw new RuntimeException("Revision undeployment failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Restores a previous revision of a resource, making it the current version.
     *
     * @param revisionId Context key containing the revision ID to restore
     * @param resourceType Type of resource (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID
     */
    @When("I restore a previous revision {string} of {string} resource {string}")
    public void iRestoreAPreviousRevision(String revisionId, String resourceType, String resourceId)  {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String actualRevisionId = Utils.resolveFromContext(revisionId).toString();
        String url = Utils.getRevisionRestoreURL(baseUrl, resourceType, actualResourceId, actualRevisionId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPost(url, headers, null, null);
            } catch (IOException e) {
                throw new RuntimeException("Revision restore failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

     /**
     * Deletes a resource (API, API Product, etc.) by its ID.
     *
     * @param resourceType Type of resource to delete (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID to delete
     */
    @When("I delete the {string} resource with id {string}")
    public void iDeleteTheResource(String resourceType, String resourceId)  {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String url = Utils.getResourceEndpointURL(baseUrl, resourceType, actualResourceId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doDelete(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("Resource deletion failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Publishes a resource, changing its lifecycle state to "PUBLISHED".
     * A published resource becomes available in the Developer Portal for subscription.
     *
     * @param resourceType Type of resource to publish (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID to publish
     */
    @When("I publish the {string} resource with id {string}")
    public void iPublishTheResource(String resourceType, String resourceId) {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String url = Utils.getChangeLifecycleURL(baseUrl, resourceType, actualResourceId, "Publish",
                null);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPost(url, headers, null, null);
            } catch (IOException e) {
                throw new RuntimeException("Resource publish failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Demotes a resource, changing its lifecycle state back to "CREATED".
     * This is typically used to move a "PUBLISHED" or "PROTOTYPED" API back to the draft state.
     *
     * @param resourceType Type of resource to demote (e.g., "apis")
     * @param resourceId Context key containing the resource ID to demote
     */
    @When("I demote the {string} resource with id {string} to created state")
    public void iDemoteTheResourceToCreated(String resourceType, String resourceId) {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String action = "Demote to Created";
        String url = Utils.getChangeLifecycleURL(baseUrl, resourceType, actualResourceId, action, null);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                logger.info("Attempting to demote resource {} to CREATED state.", actualResourceId);
                return SimpleHTTPClient.getInstance().doPost(url, headers, null, null);
            } catch (IOException e) {
                throw new RuntimeException("Resource demotion failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Retrieves the details of a specific resource by its ID.
     *
     * @param resourceType Type of resource to retrieve (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID to retrieve
     */
    @When("I retrieve the {string} resource with id {string}")
    public void iRetrieveTheResource(String resourceType, String resourceId)  {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String url = Utils.getResourceEndpointURL(baseUrl, resourceType, actualResourceId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doGet(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("Resource retrieval failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Retrieves a list of all APIs created through the Publisher REST API.
     * This step performs a search query without filters to get all available APIs.
     */
    @When("I retrieve all APIs created through the Publisher REST API")
    public void iRetrieveAllApis()  {

        String url = Utils.getAPISearchEndpointURL(baseUrl, null, null, null);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doGet(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("Failed to retrieve all APIs: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    @Then("I get the lifecycle status of API {string}")
    public void IGetLifecycleStatusOf(String apiId) throws IOException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String url = Utils.getAPILifecycleStateURL(baseUrl, actualApiId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doGet(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("Error fetching lifecycle state for " + actualApiId, e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Searches for an API by name and version, then stores its UUID in the test context.
     * This step implements a retry mechanism to handle eventual consistency, waiting for the API
     * to be indexed before searching.
     *
     * @param apiName Name of the API to search for
     * @param apiVersion Version of the API to search for
     */
    @When("I find the API created with the name {string} and version {string}")
    public void iFindTheAPICreatedWithTheNameAndVersionAs(String apiName, String apiVersion) throws InterruptedException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("publisherAccessToken"));

        String searchQuery = String.format("name:%s version:%s", apiName, apiVersion);
        String url = Utils.getAPISearchEndpointURL(baseUrl, searchQuery, null, null);

        RequestAction  requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doGet(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("API Search request failed for: " + searchQuery, e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
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
    public void waitUntilRevisionIsDeployed(String resourceType, String resourceId)  {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String revisionId = Utils.resolveFromContext("revisionId").toString();

        String url = Utils.getRevisionDeployments(baseUrl, resourceType, actualResourceId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

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
     */
    @When("I create a new version {string} of {string} resource {string} with default version {string}")
    public void iCreateANewVersionOfAPI(String newVersion, String resourceType, String resourceID, String isDefault) {

        String actualResourceID = Utils.resolveFromContext(resourceID).toString();
        boolean defaultVersion = Boolean.parseBoolean(isDefault);
        String url = Utils.getNewAPIVersionURL(baseUrl, resourceType, newVersion, defaultVersion, actualResourceID);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPost(url, headers, null, null);
            } catch (IOException e) {
                throw new RuntimeException("New version creation request failed for resource: " + actualResourceID, e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
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
    public void iAddTheDocumentToAPI(String apiID)  {

        String jsonPayload = Utils.resolveFromContext("<newDocumentPayload>").toString();
        String actualApiId = Utils.resolveFromContext(apiID).toString();
        String url = Utils.getAPIDocuments(baseUrl, actualApiId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPost(url, headers, jsonPayload,
                                Constants.CONTENT_TYPES.APPLICATION_JSON);
            } catch (IOException e) {
                throw new RuntimeException("Document addition failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Retrieves all documents associated with an API.
     *
     * @param apiID Context key containing the API ID
     */
    @When("I retrieve all available documents for {string}")
    public void iRetrieveAllAvailableDocumentsFor(String apiID)  {

        String actualApiId = Utils.resolveFromContext(apiID).toString();
        String url = Utils.getAPIDocuments(baseUrl, actualApiId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doGet(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("Failed to retrieve documents: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Searches for a document (or other resource) by name in the most recent HTTP response
     * and stores its ID in the test context.
     *
     * @param key The field name to extract
     * @param name The name of the document/resource to search for
     * @param id Context key where the found ID will be stored
     */
    @When("I find the {string} with name {string} as {string}")
    public void iFindTheDocumentWithNameAs(String key,String name, String id) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        JSONObject json = new JSONObject(response.getData());

        JSONArray docs = json.getJSONArray("list");
        String foundId = null;

        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            if (doc.has("name") && doc.getString("name").equalsIgnoreCase(name)) {
                foundId = doc.getString(key);
                break;
            }
        }

        if (foundId == null) {
            throw new AssertionError("Resource with name '" + name + "' not found");
        }

        TestContext.set(id, foundId);
    }

    /**
     * Retrieves a specific document by its ID for a given API.
     *
     * @param documentID Context key containing the document ID to retrieve
     * @param apiID Context key containing the API ID
     */
    @When("I retrieve document with {string} for {string}")
    public void iRetrieveDocumentWithFor(String documentID, String apiID)  {

        String documentId = Utils.resolveFromContext(documentID).toString();
        String actualApiId = Utils.resolveFromContext(apiID).toString();
        String url = Utils.getAPIDocument(baseUrl, actualApiId, documentId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doGet(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("Document retrieval failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Deletes a document from an API.
     *
     * @param documentID Context key containing the document ID to delete
     * @param apiID Context key containing the API ID
     */
    @When("I delete the document with {string} for {string}")
    public void iDeleteTheDocumentWithFor(String documentID, String apiID)  {

        String documentId = Utils.resolveFromContext(documentID).toString();
        String actualApiId = Utils.resolveFromContext(apiID).toString();
        String url = Utils.getAPIDocument(baseUrl, actualApiId, documentId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doDelete(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("Document deletion failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Updates a specific field of a document with a new value.
     * This step retrieves the existing document payload, updates the specified configuration field,
     * and then performs the update operation.
     *
     * @param documentID Context key containing the document ID to update
     * @param documentPayload Context key containing the existing document payload
     * @param apiID Context key containing the API ID
     * @param config The configuration field name to update
     * @param configValue The new value for the configuration field (can be JSON, boolean, number, or string)
     */
    @When("I update the document {string} with {string} for {string} as {string} and value:")
    public void iUpdateTheDocumentWithForAsAndValue(String documentID, String documentPayload, String apiID, String config, String configValue) throws InterruptedException, IOException {

        // Retrieve a JSON object safely
        Object ctxValue = Utils.resolveFromContext(documentPayload);
        JSONObject jsonPayload = (ctxValue instanceof JSONObject)
                ? (JSONObject) ctxValue
                : new JSONObject(ctxValue.toString());

        Object parsedValue = Utils.parseConfigValue(configValue);

        // update or overwrite the payload
        jsonPayload.put(config, parsedValue);
        String updatedJsonPayload = jsonPayload.toString();
        TestContext.set(Utils.normalizeContextKey("<newDocumentPayload>"), updatedJsonPayload);

        iUpdateTheDocumentWithForAPI(documentID, apiID);
        Thread.sleep(3000);
    }

    /**
     * Updates a document using the payload stored in the test context.
     *
     * @param documentID Context key containing the document ID to update
     * @param apiID Context key containing the API ID
     */
    @And("I update the document with {string} for API {string}")
    public void iUpdateTheDocumentWithForAPI(String documentID, String apiID)  {

        String jsonPayload = Utils.resolveFromContext("<newDocumentPayload>").toString();
        String actualApiId = Utils.resolveFromContext(apiID).toString();
        String documentId = Utils.resolveFromContext(documentID).toString();
        String url = Utils.getAPIDocument(baseUrl, actualApiId, documentId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPut(url, headers, jsonPayload,
                                Constants.CONTENT_TYPES.APPLICATION_JSON);
            } catch (IOException e) {
                throw new RuntimeException("Document update failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
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
    public void iUpdateTheResourceWithConfigurationTypeAndValue(String resourceType, String resourceID, String resourceUpdatePayload, String configType, String configValue) throws  InterruptedException {

        // Retrieve a JSON object safely
        Object ctxValue = Utils.resolveFromContext(resourceUpdatePayload);
        JSONObject jsonPayload = (ctxValue instanceof JSONObject)
                ? (JSONObject) ctxValue
                : new JSONObject(ctxValue.toString());

        if ("endpointConfig".equals(configType)){
            configValue = Utils.resolveFromContext(configValue).toString();
        }
        Object parsedValue = Utils.parseConfigValue(configValue);

        // update or overwrite the payload
        jsonPayload.put(configType, parsedValue);
        String updatedJsonPayload = jsonPayload.toString();
        TestContext.set(Utils.normalizeContextKey("<apiConfigUpdate>"), updatedJsonPayload);

        iUpdateResourceWithJsonPayloadFromContext(resourceType, resourceID, "<apiConfigUpdate>");
        baseSteps.iWaitUntilStatus(200);;
    }


    /**
     * Updates a specific configuration field of a resource with a value fetched from test context.
     *
     * @param resourceType Type of resource to update (e.g., "apis", "api-products")
     * @param resourceID Context key containing the resource ID to update
     * @param resourceUpdatePayload Context key containing the existing resource payload
     * @param configType The configuration field name to update
     * @param configValueContextKey Context key containing the new value
     */
    @When("I update the {string} resource {string} and {string} with configuration type {string} and value from context {string}")
    public void iUpdateTheResourceWithConfigurationTypeAndValueFromContext(String resourceType, String resourceID,
            String resourceUpdatePayload, String configType, String configValueContextKey) throws InterruptedException {

        Object ctxValue = Utils.resolveFromContext(configValueContextKey);
        iUpdateTheResourceWithConfigurationTypeAndValue(resourceType, resourceID, resourceUpdatePayload, configType,
                ctxValue.toString()
        );
    }

    /**
     * Prepares an endpoint update payload by loading a base template file
     * and replacing placeholders with the provided endpoint configuration values.
     *
     * @param type Endpoint type (e.g., "http", "https")
     * @param productionEndpoint The production endpoint URL
     * @param sandboxEndpoint The sandbox endpoint URL
     * @param contextKey Context key where the prepared payload will be stored
     */
    @When("I prepare an endpoint update with {string}, {string} and {string} as {string}")
    public void iPrepareAnEndpointUpdateWithAnd(String type, String productionEndpoint, String sandboxEndpoint, String contextKey) throws IOException{
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("artifacts/payloads/update_api_endpoint.json")) {
            if (inputStream == null) {
                throw new FileNotFoundException("File not found on classpath: " + "artifacts/payloads/update_api_endpoint.json");
            }
            String jsonPayload = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            jsonPayload = jsonPayload.replace("<type>", type)
                    .replace("<productionEndpoint>", productionEndpoint)
                    .replace("<sandboxEndpoint>", sandboxEndpoint);
            TestContext.set(Utils.normalizeContextKey(contextKey), jsonPayload);
        }
    }

    /**
     * Blocks a subscription, preventing it from being used for API invocation.
     *
     * @param subscriptionID Context key containing the subscription ID to block
     */
    @When("I block the subscription with {string} for the resource")
    public void iBlockTheSubscriptionWithForTheResource(String subscriptionID)  {

        String subscriptionId = Utils.resolveFromContext(subscriptionID).toString();
        String url = Utils.getSubscriptionBlockingURL(baseUrl, subscriptionId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPost(url, headers, null, null);
            } catch (IOException e) {
                throw new RuntimeException("Subscription blocking failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Unblocks a previously blocked subscription, allowing it to be used for API invocation again.
     *
     * @param subscriptionID Context key containing the subscription ID to unblock
     */
    @When("I unblock the subscription with {string} for the resource")
    public void iUnblockTheSubscriptionWithForTheResource(String subscriptionID)  {

        String subscriptionId = Utils.resolveFromContext(subscriptionID).toString();
        String url = Utils.getSubscriptionUnBlockingURL(baseUrl, subscriptionId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPost(url, headers, null, null);
            } catch (IOException e) {
                throw new RuntimeException("Subscription unblocking failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Retrieves all subscriptions associated with a resource (API or API Product).
     *
     * @param resourceID Context key containing the resource ID
     */
    @When("I retrieve the subscriptions for resource {string}")
    public void iRetrieveTheSubscriptionsForResource(String resourceID)  {

        String actualResourceID = Utils.resolveFromContext(resourceID).toString();
        String url = Utils.getSubscriptions(baseUrl, actualResourceID);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doGet(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("Failed to retrieve subscriptions: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
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

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("artifacts/payloads/create_apim_shared_scope_payload.json")) {
            if (inputStream == null) {
                throw new FileNotFoundException("File not found on classpath: " + "artifacts/payloads/create_apim_shared_scope_payload.json");
            }
            String jsonPayload = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            jsonPayload = jsonPayload.replace("<name>", scopeName);

            TestContext.set(Utils.normalizeContextKey("<newSharedScope>"), jsonPayload);
        }

        String jsonPayload = Utils.resolveFromContext("<newSharedScope>").toString();
        String url = Utils.getAPIScopes(baseUrl);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPost(url, headers, jsonPayload,
                                Constants.CONTENT_TYPES.APPLICATION_JSON);
            } catch (IOException e) {
                throw new RuntimeException("Shared scope creation failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Deletes a shared scope by its ID.
     *
     * @param scopeID Context key containing the scope ID to delete
     */
    @When("I delete shared scope with {string}")
    public void iDeleteSharedScopeWith(String scopeID)  {

        String scopeId = Utils.resolveFromContext(scopeID).toString();
        String url = Utils.getAPIScopesById(baseUrl, scopeId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doDelete(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("Shared scope deletion failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Updates a shared scope with a new payload.
     *
     * @param scopeID Context key containing the scope ID to update
     * @param scopePayload Context key containing the updated scope JSON payload
     */
    @And("I update shared scope {string} with payload {string}")
    public void iUpdateSharedScopeWithPayload(String scopeID, String scopePayload)  {

        String scopeId = Utils.resolveFromContext(scopeID).toString();
        String url = Utils.getAPIScopesById(baseUrl, scopeId);
        
        String initialJsonPayload = Utils.resolveFromContext(scopePayload).toString();
        String jsonPayload = initialJsonPayload.replace("<scopeID>", scopeID);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPut(url, headers, jsonPayload,
                                Constants.CONTENT_TYPES.APPLICATION_JSON);
            } catch (IOException e) {
                throw new RuntimeException("Shared scope update failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Retrieves all shared scopes
     *
     */
    @When("I fetch all shared scopes")
    public void fetchAllSharedScopes()  {

        String url = Utils.getAPIScopes(baseUrl);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doGet(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("Failed to fetch shared scope: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Creates a GraphQL API by uploading a GraphQL schema file along with additional properties.
     * This step handles the multipart file upload required for GraphQL API creation.
     *
     * @param schemaFilePath Path to the GraphQL schema file (.graphql) in the classpath resources
     * @param additionalPropertiesKey Context key containing the additional properties JSON payload
     */
    @When("I create a GraphQL API with schema file {string} and additional properties {string}")
    public void iCreateAGraphQLAPIWithSchemaFileAndAdditionalPropertiesAs(String schemaFilePath,
                      String additionalPropertiesKey) throws IOException {

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
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        Map<String, String> formFields = new HashMap<>();
        formFields.put("type", "GRAPHQL");
        formFields.put("additionalProperties", additionalProperties);

        Map<String, File> files = new HashMap<>();
        files.put("file", schemaFile);

        String url = Utils.getGraphQLSchema(baseUrl);

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPostMultipartWithFiles(url, headers, files, formFields);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create GraphQL API: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Searches for an API Product by name and stores its ID in the test context.
     *
     * @param productName Name of the API Product to search for
     */
    @When("I find the api product created with the name {string}")
    public void iSearchApiProductCreatedWithName(String productName) {

        String url = Utils.getProductSearchEndpointURL(baseUrl, productName);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction action = () -> {
            try {
                return SimpleHTTPClient.getInstance().doGet(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("API Product search failed", e);
            }
        };
        TestContext.set(Constants.PENDING_HTTP_REQUEST, action);
    }

    /**
     * Creates a new API Product by combining multiple APIs.
     * This step loads a template payload and replaces placeholders with the provided API IDs.
     *
     * @param firstAPIID Context key containing the first API ID to include in the product
     * @param secondAPIID Context key containing the second API ID to include in the product
     */
    @When("I create a new API product from apis {string} and {string}")
    public void iCreateANewAPIProductAsFromApis(String firstAPIID, String secondAPIID)
            throws IOException {

        String firstApiUuid = Utils.resolveFromContext(firstAPIID).toString();
        String secondApiUuid = Utils.resolveFromContext(secondAPIID).toString();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("artifacts/payloads/create_apim_test_product.json")) {
            if (inputStream == null) {
                throw new FileNotFoundException("File not found on classpath: " + "artifacts/payloads/create_apim_test_product.json");
            }
            String jsonPayload = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            jsonPayload = jsonPayload.replace("<FirstAPIId>", firstApiUuid)
                    .replace("<SecondAPIId>", secondApiUuid);
            TestContext.set(Utils.normalizeContextKey("<newAPIProductPayload>"), jsonPayload);
        }
        iCreateAnAPIWithPayloadAs("api-products","<newAPIProductPayload>");
    }

    /**
     * Updates an API Product resource with a new payload.
     *
     * @param resourceId Context key containing the API Product ID to update
     * @param payload Context key containing the update JSON payload
     */
    @And("I update api product resource of id {string} with payload {string}")
    public void iUpdateApiProductResourceOfIdWithPayload(String resourceId, String payload) throws IOException {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String rawPayload = Utils.resolveFromContext(payload).toString();
        String firstApiUuid = Utils.resolveFromContext("firstApiUuid").toString();
        String secondApiUuid = Utils.resolveFromContext("secondApiUuid").toString();

        String finalPayload = rawPayload.replace("<FirstAPIId>", firstApiUuid)
                .replace("<SecondAPIId>", secondApiUuid);

        String url = Utils.getResourceEndpointURL(baseUrl, "api-products", actualResourceId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPut(url, headers, finalPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
            } catch (IOException e) {
                throw new RuntimeException("API Product update failed for ID: " + actualResourceId, e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
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
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        Map<String, File> files = new HashMap<>();
        files.put("policySpecFile", policySpecFile);
        files.put("synapsePolicyDefinitionFile", synapsePolicyDefinitionFile);

        String endpointUrl;
        if (apiId == null || apiId.isEmpty()) {
            // Common policy
            endpointUrl = Utils.getCommonPolicy(baseUrl);
        } else {
            // API-specific policy
            endpointUrl = Utils.getAPISpecificPolicy(baseUrl, apiId);
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
                        TestContext.set(policyId, responseJson.getString("id"));
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

        String url = Utils.getCommonPolicy(baseUrl);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doGet(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("Failed to fetch common policies", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Deletes an API-specific operation policy.
     *
     * @param apiId Context key containing the API ID
     * @param policyId Context key containing the policy ID to delete
     */
    @When("I delete the api {string} specific policy {string}")
    public void iDeleteTheApiSpecificPolicy(String apiId, String policyId)  {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String policyID = Utils.resolveFromContext(policyId).toString();
        String url = Utils.getAPISpecificPolicyById(baseUrl, actualApiId, policyID);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doDelete(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("API-specific policy deletion failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Creates a new global policy (gateway policy) from a common policy.
     *
     * @param policyPayload Context key containing the global policy creation JSON payload
     */
    @And("I create a new global policy with payload {string}")
    public void iCreateANewGlobalPolicy(String policyPayload)  {

        String jsonPayloadTemplate = Utils.resolveFromContext(policyPayload).toString();
        String jsonPayload = Utils.resolveContextPlaceholders(jsonPayloadTemplate);
        String url = Utils.getGlobalPolicy(baseUrl);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPost(url, headers, jsonPayload,
                                Constants.CONTENT_TYPES.APPLICATION_JSON);
            } catch (IOException e) {
                throw new RuntimeException("Global policy creation failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Deploys a global policy mapping to specific gateway environments.
     * This step associates the global policy with gateway environments, making it available for use.
     *
     * @param globalPolicyId Context key containing the global policy ID to engage
     * @param payload Context key containing the deployment configuration JSON payload
     */
    @And("I engage the gateway policy mapping {string} to the gateways {string}")
    public void iEngageTheGatewayPolicyMappingToTheGateways(String globalPolicyId, String payload) {

        String policyId = Utils.resolveFromContext(globalPolicyId).toString();
        String url = Utils.getGlobalPolicyDeploy(baseUrl, policyId);

        String initialJsonPayload = Utils.resolveFromContext(payload).toString();
        String jsonPayload = initialJsonPayload.replace("{{gatewayEnvironment}}", System.getenv(Constants.GATEWAY_ENVIRONMENT));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPost(url, headers, jsonPayload,
                                Constants.CONTENT_TYPES.APPLICATION_JSON);
            } catch (IOException e) {
                throw new RuntimeException("Gateway policy mapping engagement failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Retrieves the API definition for a resource.
     * This step fetches the current API definition document (Swagger/OpenAPI specification).
     *
     * @param resourceType Type of resource (e.g., "apis", "api-products")
     * @param resourceId Context key containing the resource ID
     */
    @And("I retrieve {string} resource definition for {string}")
    public void iRetrieveResourceDefinitionFor(String resourceType, String resourceId)  {

        String actualResourceID = Utils.resolveFromContext(resourceId).toString();
        String url = Utils.getSwaggerURL(baseUrl, resourceType, actualResourceID);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doGet(url, headers);
            } catch (IOException e) {
                throw new RuntimeException("Resource definition retrieval failed: ", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Imports an OpenAPI definition from a file and creates an API.
     * This step handles the multipart file upload required for API import, including both
     * the OpenAPI definition file and additional properties file.
     *
     * @param filepath Path to the OpenAPI definition file (.json or .yaml) in classpath resources
     * @param additionalData Path to the additional properties JSON file in classpath resources
     */
    @When("I import open api definition from {string}, additional properties from {string} and create api")
    public void iImportOpenApiDefinitionFromAndCreateApiAs(String filepath, String additionalData) throws IOException {

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

            // Create temporary file object
            additionalPropertiesFile = File.createTempFile("data", ".json");
            additionalPropertiesFile.deleteOnExit();
            Files.copy(inputStream, additionalPropertiesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        Map<String, File> files = new HashMap<>();
        files.put("file", openapiFile);
        files.put("additionalProperties", additionalPropertiesFile);
        String url = Utils.getAPIDefinitionURL(baseUrl);

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance().doPostMultipartWithFiles(url, headers, files, null);
            } catch (IOException e) {
                throw new RuntimeException("Multipart API import failed", e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }

    /**
     * Updates the API definition (OpenAPI/Swagger) for a resource.
     * This step uploads a new API definition file to replace the existing one.
     *
     * @param resourceType Type of resource (e.g., "apis", "api-products")
     * @param filepath Path to the new API definition file (.json or .yaml) in classpath resources
     * @param resourceId Context key containing the resource ID to update
     */
    @And("I update the {string} resource definition with {string} for {string}")
    public void iUpdateTheResourceDefinitionWith(String resourceType, String filepath,  String resourceId) throws IOException,InterruptedException {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String url = Utils.getSwaggerURL(baseUrl, resourceType, actualResourceId);

        File swaggerFile;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filepath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("API definition file not found: " + filepath);
            }

            // Create temporary file object
            swaggerFile = File.createTempFile("swagger", ".json");
            swaggerFile.deleteOnExit();
            Files.copy(inputStream, swaggerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Thread.sleep(3000);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        Map<String, File> files = new HashMap<>();
        files.put("apiDefinition", swaggerFile);

        RequestAction requestAction = () -> {
            try {
                return SimpleHTTPClient.getInstance()
                        .doPutMultipartWithFiles(url, headers, files, null);
            } catch (IOException e) {
                throw new RuntimeException("Failed to update resource definition for: " + actualResourceId, e);
            }
        };

        TestContext.set(Constants.PENDING_HTTP_REQUEST, requestAction);
    }
}
