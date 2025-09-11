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
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.datatable.DataTable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class PublisherStepDefinitions {

    private final String baseUrl;

    private final TestContext context;

    public PublisherStepDefinitions(TestContext testcontext) {
        this.context = testcontext;
        baseUrl = context.get("baseUrl").toString();
    }

    @When("I create an API with payload {string}")
    public void iCreateApiWithJsonPayload(String payload) throws IOException {

        String jsonPayload = Utils.resolveFromContext(context, payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + context.get("publisherAccessToken").toString());

        HttpResponse apiCreateResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPICreateEndpointURL(baseUrl), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        context.set("createdApiId", Utils.extractValueFromPayload(apiCreateResponse.getData(), "id"));
        context.set("httpResponse", apiCreateResponse);
    }

    @When("I update API of id {string} with payload {string}")
    public void iUpdateApiWithJsonPayloadFromContext(String apiId, String payload) throws IOException {

        String actualApiId = Utils.resolveFromContext(context, apiId).toString();
        String jsonPayload = Utils.resolveFromContext(context, payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + context.get("publisherAccessToken").toString());

        HttpResponse apiUpdateResponse = SimpleHTTPClient.getInstance().doPut(
                Utils.getAPIEndpointURL(baseUrl, actualApiId), headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);
        context.set("httpResponse", apiUpdateResponse);
    }

    @Then("The response should contain the following api policies")
    public void theResponseShouldContainFollowingApiPolicies(DataTable dataTable) throws IOException {
        HttpResponse response = (HttpResponse) context.get("httpResponse");
        String responsePayload = response.getData();

        Map<String, String> expectedPolicies = dataTable.asMap(String.class, String.class);
        JsonNode apiPolicies = new ObjectMapper().readTree(responsePayload).path("apiPolicies");

        for (Map.Entry<String, String> expectedPolicy : expectedPolicies.entrySet()) {
            validatePolicy(apiPolicies, expectedPolicy.getKey(), expectedPolicy.getValue());
        }
    }

    @When("I make a request to create a revision for API {string} with payload {string}")
    public void iCreateApiRevision(String apiId, String contextKey) throws IOException, InterruptedException {

        String actualApiId = Utils.resolveFromContext(context,apiId).toString();
        String jsonPayload = Utils.resolveFromContext(context, contextKey).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + context.get("publisherAccessToken").toString());

        HttpResponse createRevisionResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPIRevisionURL(baseUrl, actualApiId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        context.set("httpResponse", createRevisionResponse);
        context.set("revisionId", Utils.extractValueFromPayload(createRevisionResponse.getData(), "id"));
        Thread.sleep(3000);
    }

    @When("I make a request to deploy revision {string} of API {string} with payload {string}")
    public void iDeployApiRevisionGivenPayload(String revisionId, String apiId, String payload) throws IOException {

        String actualApiId = Utils.resolveFromContext(context, apiId).toString();
        String actualRevisionId = Utils.resolveFromContext(context, revisionId).toString();
        String jsonPayload = Utils.resolveFromContext(context, payload).toString();
        jsonPayload = jsonPayload.replace("{{gatewayEnvironment}}", System.getenv(Constants.GATEWAY_ENVIRONMENT));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + context.get("publisherAccessToken").toString());

        HttpResponse deployRevisionResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPIRevisionDeploymentURL(baseUrl, actualApiId, actualRevisionId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        context.set("httpResponse", deployRevisionResponse);
    }

    @When("I delete the API with id {string}")
    public void iDeleteTheApi(String apiId) throws IOException {

        String actualApiId = Utils.resolveFromContext(context, apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + context.get("publisherAccessToken").toString());

        HttpResponse apiDeleteResponse = SimpleHTTPClient.getInstance().doDelete(Utils.getAPIEndpointURL(baseUrl,
                actualApiId), headers);
        context.set("httpResponse", apiDeleteResponse);
    }

    @When("I publish the API with id {string}")
    public void iPublishTheApi(String apiId) throws IOException {

        String actualApiId = Utils.resolveFromContext(context, apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + context.get("publisherAccessToken").toString());
        HttpResponse publishResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getChangeLifecycleURL(baseUrl, actualApiId, "Publish", null), headers, null, null);
        context.set("httpResponse", publishResponse);
    }

    @When("I retrieve the API with id {string}")
    public void iRetrieveTheApi(String apiId) throws IOException {

        String actualApiId =Utils.resolveFromContext(context, apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + context.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPIEndpointURL(baseUrl, actualApiId), headers);
        context.set("httpResponse", response);
    }

    @When("I retrieve all APIs created through the Publisher REST API")
    public void iRetrieveAllApis() throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + context.get("publisherAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPISearchEndpointURL(baseUrl, null, null, null ), headers);

        context.set("httpResponse", response);
    }

    @Then("The API with id {string} should be in the list of all APIS")
    public void theApiShouldBeInTheListOfAllApis(String apiId)  {

        String actualApiId = Utils.resolveFromContext(context, apiId).toString();
        HttpResponse response = (HttpResponse) context.get("httpResponse");
        JSONArray apisList = new JSONObject(response.getData()).getJSONArray("list");

        boolean found = IntStream.range(0, apisList.length())
                .mapToObj(apisList::getJSONObject)
                .anyMatch(subJson -> actualApiId.equals(subJson.optString("id", null)));

        Assert.assertTrue(found, "API with id " + actualApiId + " not found in the list.");
    }

    @Then("The lifecycle status of API {string} should be {string}")
    public void theLifecycleStatusShouldBe(String apiId, String status) throws IOException {

        String actualApiId = Utils.resolveFromContext(context, apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + context.get("publisherAccessToken").toString());

        HttpResponse lifecycleStatusResponse = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPILifecycleStateURL(baseUrl, actualApiId), headers);
        context.set("httpResponse", lifecycleStatusResponse);

        Assert.assertEquals(new JSONObject(lifecycleStatusResponse.getData()).getString("state"), status);
    }

    @When("I find the apiUUID of the API created with the name {string} and version {string}")
    public void findApiUuidUsingName(String apiName, String apiVersion) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + context.get("publisherAccessToken").toString());

        String searchQuery = String.format("name:%s version:%s", apiName, apiVersion);
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPISearchEndpointURL(baseUrl, searchQuery, null, null ), headers);

        context.set("httpResponse", response);
        context.set("selectedApiId", Utils.extractAPIUUID(response.getData()));
    }

    /**
     * Validates that the given API policies JSON contains a policy of the specified type
     * with the expected policy name and a non-empty policy ID.
     *
     * @param apiPolicies JSON node containing API policies
     * @param expectedPolicyName Expected policy name
     * @param policyType Type of policy
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
                "Policy '" + expectedPolicyName + "' not found or missing policyId under apiPolicies['" + policyType + "']");
    }
}
