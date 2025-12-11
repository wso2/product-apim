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

public class PublisherStepDefinitions {

    private final String baseUrl;
    private static final Logger logger = LoggerFactory.getLogger(PublisherStepDefinitions.class);

    public PublisherStepDefinitions() {
        baseUrl = TestContext.get("baseUrl").toString();
    }

    @When("I create an API with payload {string}")
    public void iCreateApiWithJsonPayload(String payload) throws IOException {

        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse apiCreateResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPICreateEndpointURL(baseUrl, "apis"), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        Assert.assertEquals(apiCreateResponse.getResponseCode(), 201, apiCreateResponse.getData());
        TestContext.set("createdApiId", Utils.extractValueFromPayload(apiCreateResponse.getData(), "id"));

    }

    @And("I create an {string} resource with payload {string} as {string}")
    public void iCreateAnAPIWithPayloadAs(String resourceType, String payload, String resourceID) throws IOException {

        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse apiCreateResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPICreateEndpointURL(baseUrl, resourceType), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", apiCreateResponse);

        Assert.assertEquals(apiCreateResponse.getResponseCode(), 201, apiCreateResponse.getData());
        TestContext.set(resourceID, Utils.extractValueFromPayload(apiCreateResponse.getData(), "id"));

    }

    @When("I update {string} resource of id {string} with payload {string}")
    public void iUpdateResourceWithJsonPayloadFromContext(String resourceType, String resourceId, String payload) throws IOException {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse apiUpdateResponse = SimpleHTTPClient.getInstance().doPut(
                Utils.getResourceEndpointURL(baseUrl,resourceType ,actualResourceId), headers, jsonPayload,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", apiUpdateResponse);
    }

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

   // Step definitions for revisions
    @When("I make a request to create a revision for {string} resource {string} with payload {string}")
    public void iCreateResourceRevision(String resourceType, String resourceId, String contextKey) throws IOException, InterruptedException {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String jsonPayload = Utils.resolveFromContext(contextKey).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse createRevisionResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getRevisionURL(baseUrl,resourceType, actualResourceId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        Assert.assertEquals(createRevisionResponse.getResponseCode(), 201, createRevisionResponse.getData());
        TestContext.set("httpResponse", createRevisionResponse);
        TestContext.set("revisionId", Utils.extractValueFromPayload(createRevisionResponse.getData(), "id"));
        Thread.sleep(3000);
    }

    @When("I get the existing revision as {string} for {string} resource with {string}")
    public void iGetTheExistingRevisionAsForResourceWith(String revisionID, String resourceType, String resourceId) throws IOException, InterruptedException {
        String actualResourceId = Utils.resolveFromContext(resourceId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse getRevisionResponse = SimpleHTTPClient.getInstance()
                .doGet(Utils.getRevisionURL(baseUrl,resourceType, actualResourceId), headers);

        TestContext.set("httpResponse", getRevisionResponse);
        Assert.assertEquals(getRevisionResponse.getResponseCode(), 200, getRevisionResponse.getData());

        // extract and store  exiting revision ID in context
        JSONObject responseJson = new JSONObject(getRevisionResponse.getData());
        if (responseJson.has("list") && !responseJson.getJSONArray("list").isEmpty()) {
            String firstRevisionId = responseJson
                    .getJSONArray("list")
                    .getJSONObject(0)
                    .getString("id");
            TestContext.set(revisionID, firstRevisionId);
        } else {
            throw new RuntimeException("No revisions found for API: " + actualResourceId);
        }
    }

    @When("I Delete the {string} resource revision with {string} for {string}")
    public void iDeleteTheRevisionWithFor(String resourceType,String revisionId, String resourceId) throws IOException{
        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String actualRevisionId = Utils.resolveFromContext(revisionId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse getRevisionResponse = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getRevisionByID(baseUrl, resourceType, actualResourceId, actualRevisionId), headers);

        TestContext.set("httpResponse", getRevisionResponse);
    }

    @When("I make a request to deploy revision {string} of {string} resource {string} with payload {string}")
    public void iDeployApiRevisionGivenPayload(String revisionId, String resourceType, String resourceId, String payload) throws IOException {

        String actualResourceId= Utils.resolveFromContext(resourceId).toString();
        String actualRevisionId = Utils.resolveFromContext(revisionId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();
        jsonPayload = jsonPayload.replace("{{gatewayEnvironment}}", System.getenv(Constants.GATEWAY_ENVIRONMENT));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse deployRevisionResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getRevisionDeploymentURL(baseUrl, resourceType, actualResourceId, actualRevisionId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", deployRevisionResponse);
    }

    @When("I undeploy revision {string} of {string} resource {string}")
    public void iUndeployRevision(String revisionId, String resourceType, String resourceId) throws IOException{

        baseSteps.putJsonPayloadInContext("<undeployRevisionPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");

        String actualrRsourceId = Utils.resolveFromContext(resourceId).toString();
        String actualRevisionId = Utils.resolveFromContext(revisionId).toString();
        String jsonPayload = Utils.resolveFromContext("<undeployRevisionPayload>").toString();
        jsonPayload = jsonPayload.replace("{{gatewayEnvironment}}", System.getenv(Constants.GATEWAY_ENVIRONMENT));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse unDeployRevisionResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getRevisionUnDeploymentURL(baseUrl, resourceType, actualrRsourceId, actualRevisionId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", unDeployRevisionResponse);

    }

    @When("I restore a previous revision {string} of {string} resource {string}")
    public void iRestoreAPreviousRevision(String revisionId, String resourceType, String resourceId) throws IOException{

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String actualRevisionId = Utils.resolveFromContext(revisionId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse restoreRevisionResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getRevisionRestoreURL(baseUrl, resourceType, actualResourceId, actualRevisionId), headers, null, null);

        TestContext.set("httpResponse", restoreRevisionResponse);
    }

    @When("I delete the {string} resource with id {string}")
    public void iDeleteTheResource(String resourceType, String resourceId) throws IOException {

        String actualResourceId= Utils.resolveFromContext(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse apiDeleteResponse = SimpleHTTPClient.getInstance().doDelete(Utils.getResourceEndpointURL(baseUrl, resourceType,
                actualResourceId), headers);
        TestContext.set("httpResponse", apiDeleteResponse);
    }

    @When("I publish the {string} resource with id {string}")
    public void iPublishTheResource(String resourceType, String resourceId) throws IOException {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());
        HttpResponse publishResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getChangeLifecycleURL(baseUrl, resourceType, actualResourceId, "Publish", null), headers, null, null);
        TestContext.set("httpResponse", publishResponse);
    }

    @When("I retrieve the {string} resource with id {string}")
    public void iRetrieveTheResource(String resourceType, String resourceId) throws IOException {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getResourceEndpointURL(baseUrl, resourceType, actualResourceId), headers);
        TestContext.set("httpResponse", response);
    }

    @When("I retrieve all APIs created through the Publisher REST API")
    public void iRetrieveAllApis() throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPISearchEndpointURL(baseUrl, null, null, null), headers);

        TestContext.set("httpResponse", response);
    }

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

    @Then("The lifecycle status of API {string} should be {string}")
    public void theLifecycleStatusShouldBe(String apiId, String status) throws IOException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse lifecycleStatusResponse = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPILifecycleStateURL(baseUrl, actualApiId), headers);
        TestContext.set("httpResponse", lifecycleStatusResponse);

        Assert.assertEquals(new JSONObject(lifecycleStatusResponse.getData()).getString("state"), status);
    }

    @When("I find the apiUUID of the API created with the name {string} and version {string}")
    public void findApiUuidUsingName(String apiName, String apiVersion) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        String searchQuery = String.format("name:%s version:%s", apiName, apiVersion);
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPISearchEndpointURL(baseUrl, searchQuery, null, null), headers);

        TestContext.set("httpResponse", response);
        TestContext.set("selectedApiId", Utils.extractAPIUUID(response.getData()));
    }

    @When("I find the apiUUID of the API created with the name {string} and version {string} as {string}")
    public void iFindTheApiUUIDOfTheAPICreatedWithTheNameAndVersionAs(String apiName, String apiVersion, String apiID) throws IOException, InterruptedException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        String searchQuery = String.format("name:%s version:%s", apiName, apiVersion);
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPISearchEndpointURL(baseUrl, searchQuery, null, null), headers);

        TestContext.set("httpResponse", response);
        TestContext.set(apiID, Utils.extractAPIUUID(response.getData()));
        Thread.sleep(1000000000);
    }

    BaseSteps baseSteps = new BaseSteps();

    // Composite function to create and deploy an api with
    @Given("I have created an api and deployed it")
    public void iCreateAndDeployApi() throws IOException, InterruptedException {

        baseSteps.putJsonPayloadFromFile("artifacts/payloads/create_apim_test_api.json", "<createApiPayload>");
        iCreateApiWithJsonPayload("<createApiPayload>");
        baseSteps.putJsonPayloadInContext("<createRevisionPayload>","{\"description\":\"Initial Revision\"}");
        iCreateResourceRevision("apis","<createdApiId>", "<createRevisionPayload>");
        baseSteps.putJsonPayloadInContext("<deployRevisionPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        iDeployApiRevisionGivenPayload("<revisionId>", "apis","<createdApiId>", "<deployRevisionPayload>");
        baseSteps.theResponseStatusCodeShouldBe(201);
    }


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

    // Composite function to create API
    @And("I create an API as {string}")
    public void iCreateAnAPIAs(String apiID) throws IOException {

        baseSteps.putJsonPayloadFromFile("artifacts/payloads/create_apim_test_api.json", "<createApiPayload>");
        iCreateAnAPIWithPayloadAs("apis","<createApiPayload>", apiID);
    }

    // Composite function to deploy a revision of API
    @When("I deploy revision {string} of {string} resource {string}")
    public void iDeployRevision(String revisionID, String resourceType, String resourceID) throws IOException {
        baseSteps.putJsonPayloadInContext("<deployRevisionPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        iDeployApiRevisionGivenPayload(revisionID, resourceType, resourceID, "<deployRevisionPayload>");
    }

    // Composite function to create a revision and then deploy a API
    @Given("I deploy the API with id {string}")
    public void iDeployAPI(String apiID) throws IOException, InterruptedException{
        baseSteps.putJsonPayloadInContext("<createRevisionPayload>","{\"description\":\"new Revision\"}");
        iCreateResourceRevision("apis", apiID , "<createRevisionPayload>");
        baseSteps.putJsonPayloadInContext("<deployRevisionPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        iDeployApiRevisionGivenPayload("<revisionId>", "apis" ,apiID, "<deployRevisionPayload>");

    }

    @Then("I wait until {string} {string} revision is deployed in the gateway")
    public void waitUntilRevisionIsDeployed(String resourceType, String resourceId) throws IOException {

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
                HttpResponse response =
                        SimpleHTTPClient.getInstance().doGet(url, headers);

                if (response != null && response.getResponseCode() == 200) {

                    System.out.println(response.getData());

                    JSONObject responseJson =
                            new JSONObject(response.getData());
                    JSONArray revisions =
                            responseJson.getJSONArray("list");

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
     * Validates that the given API policies JSON contains a policy of the specified
     * type
     * with the expected policy name and a non-empty policy ID.
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
        Assert.assertTrue(policyExists,
                "Policy '" + expectedPolicyName + "' not found or missing policyId under apiPolicies['" + policyType
                        + "']");
    }

    // Step definitions for versioning
    @When("I create a new version {string} of {string} resource {string} with default version {string} as {string}")
    public void iCreateANewVersionOfAPI(String newVersion, String resourceType, String resourceID, String isDefault, String newVersionID) throws IOException{

        String actualResourceID = Utils.resolveFromContext(resourceID).toString();
        Boolean defaultVersion = false;
        if (isDefault != null && !isDefault.isEmpty()) {
            defaultVersion = Boolean.parseBoolean(isDefault);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse apiNewVersionResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getNewAPIVersionURL(baseUrl, resourceType, newVersion, defaultVersion, actualResourceID), headers, null, null);

        TestContext.set("httpResponse", apiNewVersionResponse);
        TestContext.set(newVersionID, Utils.extractValueFromPayload(apiNewVersionResponse.getData(), "id"));
    }

    // Step definitions for documenting
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

    @And("I add the document to API {string}")
    public void iAddTheDocumentToAPI(String apiID) throws IOException{

        String jsonPayload = Utils.resolveFromContext("<newDocumentPayload>").toString();
        String actualApiId = Utils.resolveFromContext(apiID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse documentCreationResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPIDocuments(baseUrl, actualApiId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", documentCreationResponse);
        TestContext.set("documentID", Utils.extractValueFromPayload(documentCreationResponse.getData(), "documentId"));
    }

    @When("I retrieve all available documents for {string}")
    public void iRetrieveAllAvailableDocumentsFor(String apiID) throws IOException{

        String actualApiId = Utils.resolveFromContext(apiID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPIDocuments(baseUrl, actualApiId), headers);
        TestContext.set("httpResponse", response);
    }

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

    @When("I retrieve document with {string} for {string}")
    public void iRetrieveDocumentWithFor(String documentID, String apiID) throws IOException{

        String documentId = Utils.resolveFromContext(documentID).toString();
        String actualApiId = Utils.resolveFromContext(apiID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPIDocument(baseUrl, actualApiId, documentId), headers);
        TestContext.set("httpResponse", response);

    }

    @When("I delete the document with {string} for {string}")
    public void iDeleteTheDocumentWithFor(String documentID, String apiID) throws IOException{

        String documentId = Utils.resolveFromContext(documentID).toString();
        String actualApiId = Utils.resolveFromContext(apiID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getAPIDocument(baseUrl, actualApiId, documentId), headers);
        TestContext.set("httpResponse", response);

    }

    @When("I update the document {string} with {string} for {string} as {string} and value:")
    public void iUpdateTheDocumentWithForAsAndValue(String documentID, String documentPayload, String apiID, String config, String configValue) throws InterruptedException, IOException {

        // Retrieve a JSON object safely
        Object ctxValue = Utils.resolveFromContext(documentPayload);
        JSONObject jsonPayload = (ctxValue instanceof JSONObject)
                ? (JSONObject) ctxValue
                : new JSONObject(ctxValue.toString());

        Object parsedValue = parseConfigValue(configValue);

        // update or overwrite the payload
        jsonPayload.put(config, parsedValue);
        String updatedJsonPayload = jsonPayload.toString();
        TestContext.set(Utils.normalizeContextKey("<newDocumentPayload>"), updatedJsonPayload);

        iUpdateTheDocumentWithForAPI(documentID, apiID);
        Thread.sleep(3000);
    }

    @And("I update the document with {string} for API {string}")
    public void iUpdateTheDocumentWithForAPI(String documentID, String apiID) throws IOException {

        String jsonPayload = Utils.resolveFromContext("<newDocumentPayload>").toString();
        String actualApiId = Utils.resolveFromContext(apiID).toString();
        String documentId = Utils.resolveFromContext(documentID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse documentUpdateResponse = SimpleHTTPClient.getInstance()
                .doPut(Utils.getAPIDocument(baseUrl, actualApiId, documentId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", documentUpdateResponse);
    }

    // Helper to parse the values correctly
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

    // Step definitions for updating apis for runtime configurations
//    @When("I update the API with {string} configuration type {string} and value:")
//    public void iUpdateTheAPIWithConfigurationTypeAndValue(String apiID, String configType, String configValue) throws IOException, InterruptedException {
//        baseSteps.putJsonPayloadFromFile("artifacts/payloads/update_apim_test_api.json", "apiConfigUpdate");
//
//        // Retrieve a JSON object safely
//        Object ctxValue = Utils.resolveFromContext("<apiConfigUpdate>");
//        JSONObject jsonPayload = (ctxValue instanceof JSONObject)
//                ? (JSONObject) ctxValue
//                : new JSONObject(ctxValue.toString());
//
//        Object parsedValue = parseConfigValue(configValue);
//
//        // update or overwrite the payload
//        jsonPayload.put(configType, parsedValue);
//        String updatedJsonPayload = jsonPayload.toString();
//        TestContext.set(Utils.normalizeContextKey("<apiConfigUpdate>"), updatedJsonPayload);
//
//        iUpdateApiWithJsonPayloadFromContext(apiID, "<apiConfigUpdate>");
//        Thread.sleep(1000);
//    }

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

    // Subscription related step definitions
    @When("I block the subscription with {string} for the resource")
    public void iBlockTheSubscriptionWithForTheResource(String subscriptionID) throws IOException {

        String subscriptionId = Utils.resolveFromContext(subscriptionID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse documentUpdateResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getSubscriptionBlockingURL(baseUrl, subscriptionId), headers, null, null);

        TestContext.set("httpResponse", documentUpdateResponse);
    }

    @When("I unblock the subscription with {string} for the resource")
    public void iUnblockTheSubscriptionWithForTheResource(String subscriptionID) throws IOException {

        String subscriptionId = Utils.resolveFromContext(subscriptionID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse documentUpdateResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getSubscriptionUnBlockingURL(baseUrl, subscriptionId), headers, null, null);

        TestContext.set("httpResponse", documentUpdateResponse);
    }

    //All subscriptions
    @When("I retrieve the subscriptions for resource {string}")
    public void iRetrieveTheSubscriptionsForResource(String resourceID) throws IOException {

        String actualResourceID = Utils.resolveFromContext(resourceID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getSubscriptions(baseUrl, actualResourceID), headers);
        TestContext.set("httpResponse", response);
    }


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
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse scopeCreationResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPIScopes(baseUrl), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", scopeCreationResponse);
        TestContext.set("scopeID", Utils.extractValueFromPayload(scopeCreationResponse.getData(), "id"));
      }

    @When("I delete shared scope with {string}")
    public void iDeleteSharedScopeWith(String scopeID) throws IOException {

        String scopeId = Utils.resolveFromContext(scopeID).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getAPIScopesById(baseUrl, scopeId), headers);
        TestContext.set("httpResponse", response);

    }

    @And("I update shared scope {string} with payload {string}")
    public void iUpdateSharedScopeWithPayload(String scopeID, String scopePayload) throws IOException {

        String scopeId = Utils.resolveFromContext(scopeID).toString();
        String jsonPayload = Utils.resolveFromContext(scopePayload).toString();

        jsonPayload = jsonPayload.replace("<scopeID>", scopeID);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPut(Utils.getAPIScopesById(baseUrl, scopeId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);

    }

    @When("I fetch the shared scope with name {string} into context as {string}")
    public void fetchSharedScopeByName(String scopeName, String scopeId) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPIScopes(baseUrl), headers);

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

        if (scopeId == null) {
            throw new RuntimeException("Scope name not found: " + scopeName);
        }

        TestContext.set(scopeId, foundedScopeId);

    }

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
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        Map<String, String> formFields = new HashMap<>();
        formFields.put("type", "GRAPHQL");
        formFields.put("additionalProperties", additionalProperties);

        Map<String, File> files = new HashMap<>();
        files.put("file", schemaFile);

        HttpResponse apiCreateResponse = SimpleHTTPClient.getInstance()
                .doPostMultipartWithFiles(Utils.getGraphQLSchema(baseUrl), headers, files, formFields);

        Assert.assertEquals(apiCreateResponse.getResponseCode(), 201, apiCreateResponse.getData());
        TestContext.set(apiID, Utils.extractValueFromPayload(apiCreateResponse.getData(), "id"));
    }


    @When("I find the api product created with the name {string} as {string}")
    public void iFindTheApiProductCreatedWithTheNameAs(String productName, String productId) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getProductSearchEndpointURL(baseUrl, productName), headers);

        TestContext.set("httpResponse", response);
        TestContext.set(productId, Utils.extractAPIUUID(response.getData()));

    }

    @When("I create a new API product as {string} from apis {string} and {string}")
    public void iCreateANewAPIProductAsFromApis(String productID, String firstAPIID, String secondAPIID) throws IOException {

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
        iCreateAnAPIWithPayloadAs("api-products","<newAPIProductPayload>",productID );

    }

    @And("I update api product resource of id {string} with payload {string}")
    public void iUpdateApiProductResourceOfIdWithPayload(String resourceId, String payload) throws IOException {

        String actualResourceId = Utils.resolveFromContext(resourceId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();
        String firstApiUuid = Utils.resolveFromContext("firstApiUuid").toString();
        String secondApiUuid = Utils.resolveFromContext("secondApiUuid").toString();

        jsonPayload = jsonPayload.replace("<FirstAPIId>", firstApiUuid)
                .replace("<SecondAPIId>", secondApiUuid);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse apiUpdateResponse = SimpleHTTPClient.getInstance().doPut(
                Utils.getResourceEndpointURL(baseUrl,"api-products" ,actualResourceId), headers, jsonPayload,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", apiUpdateResponse);

    }

    @And("I create a new common policy with spec {string} and {string} as {string}")
    public void iCreateANewCommonPolicyWithSpecAndSynapse(String synapsePolicyJ2, String policySpecYaml, String policyId) throws IOException {
        iCreateANewPolicyWithSpecAndSynapse(null, synapsePolicyJ2, policySpecYaml, policyId);
    }

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
        System.out.printf(policyCreateResponse.getData());

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

    @When("I retrieve available common policies")
    public void iRetrieveCommonPolicies() throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getCommonPolicy(baseUrl), headers);

        TestContext.set("httpResponse", response);
    }

    @When("I delete the api {string} specific policy {string}")
    public void iDeleteTheApiSpecificPolicy(String apiId, String policyId) throws IOException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String policyID = Utils.resolveFromContext(policyId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getAPISpecificPolicyById(baseUrl, actualApiId, policyID), headers);
        TestContext.set("httpResponse", response);
    }


    @And("I generate internal API Key to invoke api {string}")
    public void iGenerateInternalAPIKeyToInvokeAPIS(String apiId) throws IOException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getInternalAPIKey(baseUrl, actualApiId), headers, null, null);

        TestContext.set("httpResponse", response);
        TestContext.set("apiKey", Utils.extractValueFromPayload(response.getData(), "apikey"));

    }


    @And("I create a new global policy as {string} with {string}")
    public void iCreateANewGlobalPolicyAs(String globalPolicyId, String policyPayload) throws IOException {

        String jsonPayload = Utils.resolveFromContext(policyPayload).toString();
        String commonPolicyId = Utils.resolveFromContext("existingPolicyId").toString();

        jsonPayload = jsonPayload.replace("<id>", commonPolicyId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getGlobalPolicy(baseUrl), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        System.out.println(response.getData());
        TestContext.set(globalPolicyId, Utils.extractValueFromPayload(response.getData(), "id"));
    }

    @And("I engage the gateway policy mapping {string} to the gateways {string}")
    public void iEngageTheGatewayPolicyMappingToTheGateways(String globalPolicyId, String payload) throws IOException, InterruptedException {

        String policyId = Utils.resolveFromContext(globalPolicyId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();

        jsonPayload = jsonPayload.replace("{{gatewayEnvironment}}", System.getenv(Constants.GATEWAY_ENVIRONMENT));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getGlobalPolicyDeploy(baseUrl,policyId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        Thread.sleep(10000);
    }


    @And("I retrieve {string} resource definition for {string}")
    public void iRetrieveResourceDefinitionFor(String resourceType, String resourceId) throws IOException {

        String actualResourceID = Utils.resolveFromContext(resourceId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getSwaggerURL(baseUrl, resourceType, actualResourceID), headers);

        TestContext.set("httpResponse", response);
    }

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

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPostMultipartWithFiles(Utils.getAPIDefinitionURL(baseUrl), headers, files, null);

        TestContext.set("httpResponse", response);
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        TestContext.set(resourceId, Utils.extractValueFromPayload(response.getData(), "id"));

    }


    @And("I update the {string} resource definition with {string} for {string}")
    public void iUpdateTheResourceDefinitionWith(String resourceType, String filepath,  String resourceId) throws IOException,InterruptedException {

        String actualResourceID = Utils.resolveFromContext(resourceId).toString();

        File swaggerFile;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filepath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("API definition file not found: " + filepath);
            }

            // Create temporary file object
            swaggerFile = File.createTempFile("swagger", ".json");
            swaggerFile.deleteOnExit();
            Files.copy(inputStream, swaggerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }


        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        Map<String, File> files = new HashMap<>();
        files.put("apiDefinition", swaggerFile);

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPutMultipartWithFiles(Utils.getSwaggerURL(baseUrl, resourceType, actualResourceID), headers, files, null);

        TestContext.set("httpResponse", response);
    }



}
