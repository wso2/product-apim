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

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class StoreStepDefinitions {

    private final String baseUrl;

    public StoreStepDefinitions() {

        baseUrl = TestContext.get("baseUrl").toString();
    }

    @When("I create an application with payload {string}")
    public void iCreateAnApplicationWithJsonPayload(String payload) throws IOException {

        String jsonPayload = Utils.resolveFromContext(payload).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse applicationCreateResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getApplicationCreateURL(baseUrl), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        Assert.assertEquals(applicationCreateResponse.getResponseCode(), 201, applicationCreateResponse.getData());
        TestContext.set("createdAppId", Utils.extractValueFromPayload(applicationCreateResponse.getData(), "applicationId"));
    }

    @When("I delete the application with id {string}")
    public void iDeleteApplication(String appId) throws IOException{
        String actualAppId = Utils.resolveFromContext(appId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse applicationDeleteResponse = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getApplicationEndpointURL(baseUrl, actualAppId), headers);

        TestContext.set("httpResponse", applicationDeleteResponse);
    }

    @When("I subscribe to API {string} using application {string} with payload {string}")
    public void iSubscribeToApi(String apiId, String appId, String payload) throws Exception {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String actualAppId = Utils.resolveFromContext(appId).toString();

        // Add application id and API id to the payload
        String jsonPayload = Utils.resolveFromContext(payload).toString();
        jsonPayload = jsonPayload.replace("{{applicationId}}", actualAppId);
        jsonPayload = jsonPayload.replace("{{apiId}}", actualApiId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getCreateSubscriptionURL(baseUrl),
                headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);

        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        TestContext.set("subscriptionId",Utils.extractValueFromPayload(response.getData(), "subscriptionId"));
    }

    @When("I retrieve the application with id {string}")
    public void iShouldBeAbleToRetrieveApplication(String appId) throws Exception {
        String actualAppId = Utils.resolveFromContext(appId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse applicationRetrieveResponse = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApplicationEndpointURL(baseUrl, actualAppId), headers);

        TestContext.set("httpResponse", applicationRetrieveResponse);
    }

    @Then("I retrieve the subscription for Api {string} by Application {string}")
    public void iShouldBeAbleToRetrieveSubscription(String apiId, String appId) throws Exception {
        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String actualAppId = Utils.resolveFromContext(appId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAllSubscriptionsURL(baseUrl, actualApiId, actualAppId, null, null,
                        null), headers);

        TestContext.set("httpResponse", response);
    }

    @When("I generate client credentials for application id {string} with payload {string}")
    public void iGenerateClientCredentialsForApplication(String appId, String payload) throws Exception {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getGenerateApplicationKeysURL(baseUrl, actualAppId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        TestContext.set("consumerKey", Utils.extractValueFromPayload(response.getData(), "consumerKey"));
        TestContext.set("consumerSecret", Utils.extractValueFromPayload(response.getData(), "consumerSecret"));
        TestContext.set("keyMappingId", Utils.extractValueFromPayload(response.getData(), "keyMappingId"));
    }

    @When("I request an access token for application id {string} using payload {string}")
    public void iRequestAccessToken(String appId, String payload) throws Exception {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String keyMappingId = Utils.resolveFromContext("keyMappingId").toString();
        String consumerSecret = Utils.resolveFromContext("consumerSecret").toString();

        // Add consumer secret to the payload
        String jsonPayload = Utils.resolveFromContext(payload).toString();
        jsonPayload = jsonPayload.replace("{{appConsumerSecret}}", consumerSecret);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getGenerateApplicationTokenURL(baseUrl, actualAppId, keyMappingId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        String accessToken = Utils.extractValueFromPayload(response.getData(), "accessToken").toString();
        TestContext.set("generatedAccessToken", accessToken);
    }

    @When("I delete the subscription with id {string}")
    public void iDeleteSubscription(String subscriptionId) throws Exception {
        String actualSubscriptionId = Utils.resolveFromContext(subscriptionId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance().doDelete(Utils.getSubscriptionURL(baseUrl,
                actualSubscriptionId), headers);

        TestContext.set("httpResponse", response);
    }

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
}
