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
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class PublisherStepDefinitions {

    private final String baseUrl;

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
                .doPost(Utils.getAPICreateEndpointURL(baseUrl), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        Assert.assertEquals(apiCreateResponse.getResponseCode(), 201, apiCreateResponse.getData());
        TestContext.set("createdApiId", Utils.extractValueFromPayload(apiCreateResponse.getData(), "id"));

    }

    @When("I update API of id {string} with payload {string}")
    public void iUpdateApiWithJsonPayloadFromContext(String apiId, String payload) throws IOException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse apiUpdateResponse = SimpleHTTPClient.getInstance().doPut(
                Utils.getAPIEndpointURL(baseUrl, actualApiId), headers, jsonPayload,
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

    @When("I make a request to create a revision for API {string} with payload {string}")
    public void iCreateApiRevision(String apiId, String contextKey) throws IOException, InterruptedException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String jsonPayload = Utils.resolveFromContext(contextKey).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse createRevisionResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPIRevisionURL(baseUrl, actualApiId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        Assert.assertEquals(createRevisionResponse.getResponseCode(), 201, createRevisionResponse.getData());
        TestContext.set("revisionId", Utils.extractValueFromPayload(createRevisionResponse.getData(), "id"));
        Thread.sleep(3000);
    }

    @When("I make a request to deploy revision {string} of API {string} with payload {string}")
    public void iDeployApiRevisionGivenPayload(String revisionId, String apiId, String payload) throws IOException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String actualRevisionId = Utils.resolveFromContext(revisionId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();
        jsonPayload = jsonPayload.replace("{{gatewayEnvironment}}", System.getenv(Constants.GATEWAY_ENVIRONMENT));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse deployRevisionResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPIRevisionDeploymentURL(baseUrl, actualApiId, actualRevisionId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", deployRevisionResponse);
    }

    @When("I delete the API with id {string}")
    public void iDeleteTheApi(String apiId) throws IOException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse apiDeleteResponse = SimpleHTTPClient.getInstance().doDelete(Utils.getAPIEndpointURL(baseUrl,
                actualApiId), headers);
        TestContext.set("httpResponse", apiDeleteResponse);
    }

    @When("I publish the API with id {string}")
    public void iPublishTheApi(String apiId) throws IOException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());
        HttpResponse publishResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getChangeLifecycleURL(baseUrl, actualApiId, "Publish", null), headers, null, null);
        TestContext.set("httpResponse", publishResponse);
    }

    @When("I retrieve the API with id {string}")
    public void iRetrieveTheApi(String apiId) throws IOException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPIEndpointURL(baseUrl, actualApiId), headers);
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
    public void iFindTheApiUUIDOfTheAPICreatedWithTheNameAndVersionAs(String apiName, String apiVersion, String apiID) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        String searchQuery = String.format("name:%s version:%s", apiName, apiVersion);
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPISearchEndpointURL(baseUrl, searchQuery, null, null), headers);

        TestContext.set("httpResponse", response);
        TestContext.set(apiID, Utils.extractAPIUUID(response.getData()));
    }

    BaseSteps baseSteps = new BaseSteps();

    // Composite function to create and deploy an api with
    @Given("I have created an api and deployed it")
    public void iCreateAndDeployApi() throws IOException, InterruptedException {
        baseSteps.putJsonPayloadFromFile("artifacts/payloads/create_apim_test_api.json", "<createApiPayload>");
        iCreateApiWithJsonPayload("<createApiPayload>");
        baseSteps.putJsonPayloadInContext("<createRevisionPayload>","{\"description\":\"Initial Revision\"}");
        iCreateApiRevision("<createdApiId>", "<createRevisionPayload>");
        baseSteps.putJsonPayloadInContext("<deployRevisionPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        iDeployApiRevisionGivenPayload("<revisionId>", "<createdApiId>", "<deployRevisionPayload>");
        baseSteps.theResponseStatusCodeShouldBe(201);
    }

    @Given("I deploy the API with id {string}")
    public void iDeployAPI(String apiID) throws IOException, InterruptedException{
        baseSteps.putJsonPayloadInContext("<createRevisionPayload>","{\"description\":\"Initial Revision\"}");
        iCreateApiRevision(apiID, "<createRevisionPayload>");
        baseSteps.putJsonPayloadInContext("<deployRevisionPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        iDeployApiRevisionGivenPayload("<revisionId>", apiID, "<deployRevisionPayload>");

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
    @When("I create a new version {string} of API {string} with default version {string} as {string}")
    public void iCreateANewVersionOfAPI(String newVersion, String apiID, String isDefault, String newVersionAPIID) throws IOException{

        String actualApiId = Utils.resolveFromContext(apiID).toString();
        Boolean defaultVersion = false;
        if (isDefault != null && !isDefault.isEmpty()) {
            defaultVersion = Boolean.parseBoolean(isDefault);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("publisherAccessToken").toString());

        HttpResponse apiNewVersionResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getNewAPIVersionURL(baseUrl, newVersion, defaultVersion, actualApiId), headers, null, null);

        TestContext.set("httpResponse", apiNewVersionResponse);
        TestContext.set(newVersionAPIID, Utils.extractValueFromPayload(apiNewVersionResponse.getData(), "id"));
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
    @When("I update the API with {string} configuration type {string} and value:")
    public void iUpdateTheAPIWithConfigurationTypeAndValue(String apiID, String configType, String configValue) throws IOException {
        baseSteps.putJsonPayloadFromFile("artifacts/payloads/update_apim_test_api.json", "apiConfigUpdate");

        // Retrieve a JSON object safely
        Object ctxValue = Utils.resolveFromContext("<apiConfigUpdate>");
        JSONObject jsonPayload = (ctxValue instanceof JSONObject)
                ? (JSONObject) ctxValue
                : new JSONObject(ctxValue.toString());

        Object parsedValue = parseConfigValue(configValue);

        // update or overwrite the payload
        jsonPayload.put(configType, parsedValue);
        String updatedJsonPayload = jsonPayload.toString();
        TestContext.set(Utils.normalizeContextKey("<apiConfigUpdate>"), updatedJsonPayload);

        iUpdateApiWithJsonPayloadFromContext(apiID, "<apiConfigUpdate>");
    }
}
