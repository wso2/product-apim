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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.Tenant;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class ApplicationBaseSteps {

    private final String baseUrl;
    private final Tenant tenant;
    private final User currentuser;

    public ApplicationBaseSteps() {

        baseUrl = TestContext.get("baseUrl").toString();
        tenant = Utils.getTenantFromContext("currentTenant");
        currentuser = tenant.getContextUser();
    }

    BaseSteps baseSteps = new BaseSteps();

    /**
     * Creates a new application in the Developer Portal using a JSON payload.
     * The created application ID is stored as "createdAppId" in the test context for use in subsequent steps.
     * 
     * @param payload Context key containing the application creation JSON payload
     */
    @When("I create an application with payload {string}")
    public void iCreateAnApplicationWithJsonPayload(String payload) throws IOException {

        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse applicationCreateResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getApplicationCreateURL(baseUrl), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", applicationCreateResponse);
        Assert.assertEquals(applicationCreateResponse.getResponseCode(), 201, applicationCreateResponse.getData());
        TestContext.set("createdAppId", Utils.extractValueFromPayload(applicationCreateResponse.getData(), "applicationId"));
    }

    /**
     * Deletes an application by its ID.
     *
     * @param appId Context key containing the application ID to delete
     */
    @When("I delete the application with id {string}")
    public void iDeleteApplication(String appId) throws IOException{

        String actualAppId = Utils.resolveFromContext(appId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse applicationDeleteResponse = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getApplicationEndpointURL(baseUrl, actualAppId), headers);

        TestContext.set("httpResponse", applicationDeleteResponse);
    }

    /**
     * Retrieves the details of a specific application by its ID.
     *
     * @param appId Context key containing the application ID to retrieve
     */
    @When("I retrieve the application with id {string}")
    public void iShouldBeAbleToRetrieveApplication(String appId) throws Exception {

        String actualAppId = Utils.resolveFromContext(appId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse applicationRetrieveResponse = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApplicationEndpointURL(baseUrl, actualAppId), headers);

        TestContext.set("httpResponse", applicationRetrieveResponse);
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
                "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApplicationSearchURL(baseUrl, applicationName), headers);

        TestContext.set("httpResponse", response);

        JSONObject responseJson = new JSONObject(response.getData());
        if (responseJson.has("list") && !responseJson.getJSONArray("list").isEmpty()) {
            String applicationId = responseJson
                    .getJSONArray("list")
                    .getJSONObject(0)
                    .getString("applicationId");
            TestContext.set(Utils.normalizeContextKey(appId), applicationId);
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

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String jsonPayload = Utils.resolveFromContext(updatePayload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance().doPut(
                Utils.getApplicationEndpointURL(baseUrl, actualAppId), headers, jsonPayload,
                Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
    }

    /**
     * Creates a subscription between an application and an API.
     * The payload is updated with the actual application ID and API ID before sending the request.
     *
     * @param payloadContextKey Context key containing the subscription creation JSON payload
     */
    @When("I create a subscription using payload {string}")
    public void iSubscribeToApi(String payloadContextKey) throws Exception {

        // Add application id and API id to the payload
        String jsonPayloadTemplate = String.valueOf(Utils.resolveFromContext(payloadContextKey));
        String jsonPayload = Utils.resolveContextPlaceholders(jsonPayloadTemplate);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getCreateSubscriptionURL(baseUrl),
                headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
    }

    /**
     * Retrieves a subscription between a specific API and application.
     *
     * @param apiId Context key containing the API ID
     * @param appId Context key containing the application ID
     */
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
     * Stores the full HttpResponse in the test context.
     *
     * @param appId Context key containing the application ID
     */
    @When("I retrieve existing application keys for {string}")
    public void iRetrieveExistingApplicationKeys(String appId) throws IOException {

        String actualAppId = Utils.resolveFromContext(appId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApplicationAllKeys(baseUrl, actualAppId), headers);

        TestContext.set("httpResponse", response);
    }

    /**
     * Extracts the consumer secret and key mapping ID from the first OAuth2 key entry
     * in the latest application keys response and stores them in the given context keys.
     *
     * @param consumerSecretContextKey context key used to store the extracted consumer secret
     * @param keyMappingIdContextKey context key used to store the extracted key mapping ID
     */
    @And("I extract the first oauth2 key details from the application keys response and store them as {string} and {string}")
    public void iExtractFirstKeyDetails(String consumerSecretContextKey, String keyMappingIdContextKey)
            throws IOException {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        String responseData = response.getData();

        // Check whether the response contains at least one key entry
        Object listSize = Utils.extractValueFromPayload(responseData, "$.list.length()");
        if (!(listSize instanceof Integer) || (Integer) listSize <= 0) {
            throw new IOException("No application keys found in the stored response list.");
        }

        // Extract first key details
        Object consumerSecretObj = Utils.extractValueFromPayload(responseData, "$.list[0].consumerSecret");
        Object keyMappingIdObj = Utils.extractValueFromPayload(responseData, "$.list[0].keyMappingId");

        // Fail if required values are missing
        if (consumerSecretObj == null || keyMappingIdObj == null) {
            throw new IOException("consumerSecret or keyMappingId is missing in the first application key entry.");
        }

        TestContext.set(Utils.normalizeContextKey(consumerSecretContextKey), String.valueOf(consumerSecretObj));
        TestContext.set(Utils.normalizeContextKey(keyMappingIdContextKey), String.valueOf(keyMappingIdObj));
    }

    /**
     * Retrieves all existing secrets for an application's key mapping.
     * Stores the full HttpResponse in the test context.
     *
     * @param appId context key containing the application ID
     * @param keyMappingIdContextKey context key containing the key mapping ID
     */
    @When("I retrieve existing application secrets for {string} using key mapping id {string}")
    public void iRetrieveExistingApplicationSecrets(String appId, String keyMappingIdContextKey) throws IOException {

        String actualAppId = String.valueOf(Utils.resolveFromContext(appId));
        String keyMappingId = String.valueOf(Utils.resolveFromContext(keyMappingIdContextKey));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAllApplicationSecretsURL(baseUrl, actualAppId, keyMappingId), headers);

        TestContext.set("httpResponse", response);
    }

    /**
     * Extracts the secret ID from the first secret entry in the latest secrets response
     * and stores it in the given test context key.
     *
     * @param secretIdContextKey context key used to store the extracted secret ID
     */
    @And("I extract the first secret details from the response and store it as {string}")
    public void iExtractFirstSecretDetailsFromTheResponse(String secretIdContextKey) throws IOException {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        String responseData = response.getData();

        // Ensure at least one secret exists in the response
        Object listSize = Utils.extractValueFromPayload(responseData, "$.list.length()");
        if (!(listSize instanceof Integer) || (Integer) listSize <= 0) {
            throw new IOException("No secrets found in the stored response list.");
        }

        // Extract first secret ID
        Object secretIdObj = Utils.extractValueFromPayload(responseData, "$.list[0].secretId");

        if (secretIdObj == null) {
            throw new IOException("secretId is missing in the first secret entry.");
        }
        TestContext.set(Utils.normalizeContextKey(secretIdContextKey), String.valueOf(secretIdObj));
    }
    /**
     * Loads OAuth key details from a file and stores the extracted values in the given context keys.
     *
     * @param filePath path to the file containing OAuth key details
     * @param consumerSecretContextKey context key under which the consumer secret should be stored
     * @param keyMappingIdContextKey context key under which the key mapping ID should be stored
     */
    @When("I get the consumer secret and key mapping id from file {string} and store them as {string} and {string}")
    public void iGetTheConsumerSecretAndKeyMappingIdFromFile(String filePath, String consumerSecretContextKey,
                        String keyMappingIdContextKey) throws Exception {

        String lookupKey = Utils.buildUserScopedKey(tenant, currentuser);

        // Read matching value and convert to JSON string
        Object oauthKeysObj = Utils.getValueFromFileByKey(filePath, lookupKey);
        String oauthKeys = new ObjectMapper().writeValueAsString(oauthKeysObj);

        String keyMappingId = String.valueOf(Utils.extractValueFromPayload(oauthKeys, "keyMappingId"));
        String consumerSecret = String.valueOf(Utils.extractValueFromPayload(oauthKeys, "consumerSecret"));

        TestContext.set(Utils.normalizeContextKey(consumerSecretContextKey), consumerSecret);
        TestContext.set(Utils.normalizeContextKey(keyMappingIdContextKey), keyMappingId);
    }

    /**
     * Updates the keys (OAuth2 credentials) for an application.
     * The update payload should be stored in the test context under the key "updateKeysPayload".
     *
     * @param appId Context key containing the application ID
     */
    @And("I update the keys for application with {string}")
    public void iUpdateTheKeysForApplicationWith(String appId) throws IOException {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String keyMappingId = Utils.resolveFromContext("keyMappingId").toString();
        String jsonPayload =Utils.resolveFromContext("updateKeysPayload").toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPut(Utils.getUpdateKey(baseUrl, actualAppId, keyMappingId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
    }

    /**
     * Deletes the generated keys (OAuth2 credentials) for an application.
     *
     * @param appId Context key containing the application ID
     */
    @When("I delete the generated keys for {string}")
    public void iDeleteTheGeneratedKeysFor(String appId) throws IOException {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String keyMappingId = Utils.resolveFromContext("keyMappingId").toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getUpdateKey(baseUrl, actualAppId, keyMappingId), headers);
        TestContext.set("httpResponse", response);
    }

    /**
     * Verifies the oauth-keys count in the latest response payload.
     *
     * @param expectedCount the expected number of oauth keys
     */
    @Then("Oauth-keys count should be {int}")
    public void oauthKeysCountShouldBe(int expectedCount) throws IOException {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        // Get oauth key count from response payload
        Object oauthKeyCount = Utils.extractValueFromPayload(response.getData(), "count");
        int actualCount = Integer.parseInt(String.valueOf(oauthKeyCount));
        Assert.assertEquals(actualCount, expectedCount,
                "Expected oauth-keys count " + expectedCount + " but found " + actualCount);
    }

    /**
     * Verifies the secrets count in the latest response payload.
     *
     * @param expectedCount the expected number of secrets
     */
    @Then("Secrets count should be {int}")
    public void secretsCountShouldBe(int expectedCount) throws IOException {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        // Get secrets count from response payload
        Object secretsCountObj = Utils.extractValueFromPayload(response.getData(), "count");
        int actualCount = Integer.parseInt(String.valueOf(secretsCountObj));
        Assert.assertEquals(actualCount, expectedCount,
                "Expected secrets count " + expectedCount + " but found " + actualCount);
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

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getGenerateApplicationKeysURL(baseUrl, actualAppId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
    }

    /**
     * Generates a client secret for an application using the given payload and key mapping ID.
     * The payload is resolved from the test context, and any placeholders in the payload
     * are replaced using values from the test context.
     *
     * @param appId context key containing the application ID
     * @param payloadContextKey context key containing the client secret generation payload
     * @param keyMappingIdContextKey context key containing the key mapping ID
     */
    @When("I generate a client secret for application id {string} using payload {string} and key mapping id {string}")
    public void iGenerateClientSecretForApplication(String appId, String payloadContextKey,
                                                    String keyMappingIdContextKey) throws Exception {

        String actualAppId = String.valueOf(Utils.resolveFromContext(appId));
        String keyMappingId = String.valueOf(Utils.resolveFromContext(keyMappingIdContextKey));
        String jsonPayload = String.valueOf(Utils.resolveFromContext(payloadContextKey));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(
                Utils.getGenerateApplicationSecretURL(baseUrl, actualAppId, keyMappingId), headers, jsonPayload,
                Constants.CONTENT_TYPES.APPLICATION_JSON
        );

        TestContext.set("httpResponse", response);
    }

    /**
     * Revokes a client secret for the given application and key mapping.
     *
     * @param appId context key containing the application ID
     * @param payloadContextKey context key containing the revoke secret payload
     * @param keyMappingIdContextKey context key containing the key mapping ID
     */
    @When("I revoke the client secret for application id {string} using payload {string} and key mapping id {string}")
    public void iRevokeClientSecretForApplication(String appId, String payloadContextKey,
            String keyMappingIdContextKey) throws Exception {

        String actualAppId = String.valueOf(Utils.resolveFromContext(appId));
        String keyMappingId = String.valueOf(Utils.resolveFromContext(keyMappingIdContextKey));

        String jsonPayloadTemplate = String.valueOf(Utils.resolveFromContext(payloadContextKey));
        String jsonPayload = Utils.resolveContextPlaceholders(jsonPayloadTemplate);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " +TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(
                Utils.getRevokeApplicationSecretURL(baseUrl, actualAppId, keyMappingId), headers, jsonPayload,
                Constants.CONTENT_TYPES.APPLICATION_JSON
        );

        TestContext.set("httpResponse", response);
    }

    /**
     * Requests an OAuth2 access token for an application using the generated client credentials.
     * The request payload is resolved from the test context, and any placeholders in the payload
     * are replaced using values from the test context.
     * The generated access token is stored in the test context.
     *
     * @param appId context key containing the application ID
     * @param payloadContextKey context key containing the token request JSON payload
     * @param keyMappingIdContextKey context key containing the key mapping ID
     */
    @When("I request an access token for application id {string} using payload {string} and key mapping id {string}")
    public void iRequestAccessToken(String appId, String payloadContextKey, String keyMappingIdContextKey)
            throws Exception {

        String actualAppId = String.valueOf(Utils.resolveFromContext(appId));
        String keyMappingId = String.valueOf(Utils.resolveFromContext(keyMappingIdContextKey));

        String jsonPayloadTemplate = String.valueOf(Utils.resolveFromContext(payloadContextKey));
        String jsonPayload = Utils.resolveContextPlaceholders(jsonPayloadTemplate);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " +
                TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getGenerateApplicationTokenURL(baseUrl, actualAppId, keyMappingId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
    }

    /**
     * Generates an API Key for an application.
     *
     * @param appId Context key containing the application ID
     * @param payload Context key containing the API key generation JSON payload
     */
    @And("I request an api key for application id {string} using payload {string}")
    public void iRequestAnApiKeyForApplicationIdUsingPayload(String appId, String payload) throws IOException {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getGenerateAPIKeyURL(baseUrl, actualAppId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        String apikey = Utils.extractValueFromPayload(response.getData(), "apikey").toString();
        TestContext.set("apiKey", apikey);
    }

    /**
     * Deletes a subscription by its ID.
     *
     * @param subscriptionId Context key containing the subscription ID to delete
     */
    @When("I delete the subscription with id {string}")
    public void iDeleteSubscription(String subscriptionId) throws Exception {
        String actualSubscriptionId = Utils.resolveFromContext(subscriptionId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance().doDelete(Utils.getSubscriptionURL(baseUrl,
                actualSubscriptionId), headers);

        TestContext.set("httpResponse", response);
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

        String actualSubscriptionId = Utils.resolveFromContext(subscriptionId).toString();

        // Add application id and API id to the payload
        String jsonPayload = Utils.resolveFromContext("subscriptionPayload").toString();
        jsonPayload = jsonPayload.replace("\"throttlingPolicy\":\"Unlimited\"", "\"throttlingPolicy\":\"" + subscriptionPlan +"\"");

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance().doPut(Utils.getSubscriptionURL(baseUrl, actualSubscriptionId),
                headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
    }

    /**
     * Retrieves the details of a specific subscription by its ID.
     *
     * @param subscriptionId Context key containing the subscription ID to retrieve
     */
    @When("I get the subscription with id {string}")
    public void iGetSubscription(String subscriptionId) throws Exception {

        String actualSubscriptionId = Utils.resolveFromContext(subscriptionId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance().doGet(Utils.getSubscriptionURL(baseUrl,
                actualSubscriptionId), headers);

        TestContext.set("httpResponse", response);
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

        String actualSubscriptionId = Utils.resolveFromContext(subscriptionId).toString();
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        JSONArray subscriptionsList= new JSONObject(response.getData()).getJSONArray("list");

        boolean found = IntStream.range(0, subscriptionsList.length())
                .mapToObj(subscriptionsList::getJSONObject)
                .anyMatch(subJson -> actualSubscriptionId.equals(subJson.optString("subscriptionId", null)));

        Assert.assertTrue(found, "Subscription with id " + actualSubscriptionId + " not found in the list.");
    }

    /**
     * Searches for APIs in the Developer Portal using a search query.
     * The search query can include filters such as name, version, provider, etc.
     *
     * @param query The search query string
     */
    @When("I search DevPortal APIs with query {string}")
    public void iSearchDevPortalAPIsWithQuery(String query) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApiSearchURL(baseUrl, query), headers);

        TestContext.set("httpResponse", response);
    }

    /**
     * Searches for an API by name and version in the Developer Portal, then stores its UUID in the test context.
     * This step uses the devportal access token (suitable for subscriber-only users)
     * and implements a retry mechanism to handle eventual consistency.
     *
     * @param apiName Name of the API to search for
     * @param apiVersion Version of the API to search for
     * @param apiID Context key where the found API UUID will be stored
     */
    @When("I find the apiUUID of the API with name {string} and version {string} from devportal as {string}")
    public void iFindApiUUIDFromDevportal(String apiName, String apiVersion, String apiID) throws IOException, InterruptedException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("devportalAccessToken").toString());

        String searchQuery = String.format("name:%s version:%s", apiName, apiVersion);
        Thread.sleep(Constants.INITIAL_INDEXING_TIME);

        HttpResponse response = null;
        String apiUUID = null;

        for (int attempt = 1; attempt <= Constants.MAX_RETRIES; attempt++) {
            response = SimpleHTTPClient.getInstance()
                    .doGet(Utils.getApiSearchURL(baseUrl, searchQuery), headers);

            if (response.getResponseCode() == 200) {
                apiUUID = Utils.extractAPIUUID(response.getData());
                if (apiUUID != null && !apiUUID.isEmpty()) {
                    break;
                }
            }
            if (attempt < Constants.MAX_RETRIES) {
                Thread.sleep(Constants.RETRY_INTERVAL_TIME);
            }
        }

        Assert.assertNotNull(apiUUID, "API UUID not found for API: " + apiName + " version: " + apiVersion);
        TestContext.set(apiID, apiUUID);
        TestContext.set("httpResponse", response);
    }

    /**
     * Retrieves all documents available for an API in the Developer Portal.
     *
     * @param resourceId Context key containing the API ID
     */
    @And("I retrieve devportal documents for {string}")
    public void iRetrieveDevportalDocumentsFor(String resourceId) throws IOException {
        String actualApiId = Utils.resolveFromContext(resourceId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApiDocumentsURL(baseUrl, actualApiId), headers);

        TestContext.set("httpResponse", response);
    }

    /**
     * Retrieves the details of a specific API from the Developer Portal by its ID.
     *
     * @param apiId Context key containing the API ID to retrieve
     */
    @When("I retrieve the API with id {string} from devportal")
    public void iRetrieveApiFromDevportal(String apiId) throws IOException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getDevportalApiDetailURL(baseUrl, actualApiId), headers);

        TestContext.set("httpResponse", response);
    }

    // --- API-bound API Key steps ---

    /**
     * Generates a new API-bound API key from the devportal for a specific API.
     * The generated API key value is stored in the context under the given contextKey,
     * and the keyUUID is stored as "{contextKey}UUID".
     *
     * @param apiId Context key containing the API ID
     * @param payload The JSON payload for API key generation
     * @param contextKey Context key to store the generated API key
     */
    @When("I generate an api-bound api key for api {string} with payload {string} as {string}")
    public void iGenerateApiBoundApiKey(String apiId, String payload, String contextKey) throws IOException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPIBoundApiKeyGenerateURL(baseUrl, actualApiId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        if (response.getResponseCode() == 200 || response.getResponseCode() == 201) {
            JSONObject responseJson = new JSONObject(response.getData());
            String apikey = responseJson.getString("apikey");
            TestContext.set(contextKey, apikey);
            String keyName = responseJson.getString("keyName");
            TestContext.set(contextKey + "Name", keyName);
            if (responseJson.has("keyUUID")) {
                TestContext.set(contextKey + "UUID", responseJson.getString("keyUUID"));
            }
        }
    }

    /**
     * Retrieves the list of API keys for an API and extracts the keyUUID for a key by name.
     *
     * @param apiId Context key containing the API ID
     * @param keyNameKey Context key containing the key name to find
     * @param uuidKey Context key to store the found keyUUID
     */
    @When("I find the keyUUID of api key {string} for api {string} as {string}")
    public void iFindKeyUUIDOfApiKey(String keyNameKey, String apiId, String uuidKey) throws IOException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String keyName = Utils.resolveFromContext(keyNameKey).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAPIBoundApiKeysListURL(baseUrl, actualApiId), headers);

        TestContext.set("httpResponse", response);

        String data = response.getData();
        JSONArray keysArray;
        if (data.trim().startsWith("[")) {
            keysArray = new JSONArray(data);
        } else {
            JSONObject wrapper = new JSONObject(data);
            keysArray = wrapper.getJSONArray("list");
        }
        for (int i = 0; i < keysArray.length(); i++) {
            JSONObject keyObj = keysArray.getJSONObject(i);
            if (keyName.equals(keyObj.getString("keyName"))) {
                TestContext.set(uuidKey, keyObj.getString("keyUUID"));
                return;
            }
        }
        throw new IOException("No API key found with name: " + keyName);
    }

    /**
     * Associates an API-bound API key to an application from the API side.
     *
     * @param apiId Context key containing the API ID
     * @param keyUUID Context key containing the key UUID
     * @param appId Context key containing the application UUID
     */
    @When("I associate api key {string} to application {string} from api {string}")
    public void iAssociateApiKeyToAppFromApi(String keyUUID, String appId, String apiId) throws IOException, InterruptedException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String actualKeyUUID = Utils.resolveFromContext(keyUUID).toString();
        String actualAppId = Utils.resolveFromContext(appId).toString();

        JSONObject payload = new JSONObject();
        payload.put("keyUUID", actualKeyUUID);
        payload.put("applicationUUID", actualAppId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPIBoundApiKeyAssociateURL(baseUrl, actualApiId), headers,
                        payload.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        Thread.sleep(2000);
    }

    /**
     * Dissociates an API-bound API key from an application from the API side.
     *
     * @param keyUUID Context key containing the key UUID
     * @param apiId Context key containing the API ID
     */
    @When("I dissociate api key {string} from api {string}")
    public void iDissociateApiKeyFromApi(String keyUUID, String apiId) throws IOException, InterruptedException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String actualKeyUUID = Utils.resolveFromContext(keyUUID).toString();

        JSONObject payload = new JSONObject();
        payload.put("keyUUID", actualKeyUUID);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPIBoundApiKeyDissociateURL(baseUrl, actualApiId), headers,
                        payload.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        Thread.sleep(2000);
    }

    /**
     * Revokes an API-bound API key.
     *
     * @param keyUUID Context key containing the key UUID to revoke
     * @param apiId Context key containing the API ID
     */
    @When("I revoke api key {string} for api {string}")
    public void iRevokeApiKey(String keyUUID, String apiId) throws IOException, InterruptedException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String actualKeyUUID = Utils.resolveFromContext(keyUUID).toString();

        JSONObject payload = new JSONObject();
        payload.put("keyUUID", actualKeyUUID);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPIBoundApiKeyRevokeURL(baseUrl, actualApiId), headers,
                        payload.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        Thread.sleep(2000);
    }

    /**
     * Regenerates an API-bound API key. The new API key value replaces the old one in the context.
     *
     * @param keyUUID Context key containing the key UUID to regenerate
     * @param apiId Context key containing the API ID
     * @param contextKey Context key to store the new API key value
     */
    @When("I regenerate api key {string} for api {string} as {string}")
    public void iRegenerateApiKey(String keyUUID, String apiId, String contextKey) throws IOException, InterruptedException {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String actualKeyUUID = Utils.resolveFromContext(keyUUID).toString();

        JSONObject payload = new JSONObject();
        payload.put("keyUUID", actualKeyUUID);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAPIBoundApiKeyRegenerateURL(baseUrl, actualApiId), headers,
                        payload.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        if (response.getResponseCode() == 200 || response.getResponseCode() == 201) {
            String apikey = Utils.extractValueFromPayload(response.getData(), "apikey").toString();
            TestContext.set(contextKey, apikey);
        }
        Thread.sleep(2000);
    }

    /**
     * Associates an API key to an application from the application side.
     *
     * @param keyUUID Context key containing the key UUID
     * @param apiId Context key containing the API UUID
     * @param appId Context key containing the application ID
     * @param keyType The key type (PRODUCTION or SANDBOX)
     */
    @When("I associate api key {string} for api {string} to application {string} with key type {string}")
    public void iAssociateApiKeyFromAppSide(String keyUUID, String apiId, String appId, String keyType) throws IOException, InterruptedException {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String actualKeyUUID = Utils.resolveFromContext(keyUUID).toString();
        String actualApiId = Utils.resolveFromContext(apiId).toString();

        JSONObject payload = new JSONObject();
        payload.put("keyUUID", actualKeyUUID);
        payload.put("apiUUID", actualApiId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAppApiKeyAssociateURL(baseUrl, actualAppId, keyType), headers,
                        payload.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        Thread.sleep(2000);
    }

    /**
     * Dissociates an API key from an application from the application side.
     *
     * @param keyUUID Context key containing the key UUID
     * @param appId Context key containing the application ID
     * @param keyType The key type (PRODUCTION or SANDBOX)
     */
    @When("I dissociate api key {string} from application {string} with key type {string}")
    public void iDissociateApiKeyFromAppSide(String keyUUID, String appId, String keyType) throws IOException, InterruptedException {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String actualKeyUUID = Utils.resolveFromContext(keyUUID).toString();

        JSONObject payload = new JSONObject();
        payload.put("keyUUID", actualKeyUUID);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getAppApiKeyDissociateURL(baseUrl, actualAppId, keyType), headers,
                        payload.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        Thread.sleep(2000);
    }

    // --- Legacy (application-level) API Key steps ---

    /**
     * Generates a legacy (application-level) API key for an application.
     * The generated API key value is stored in the context under the given contextKey,
     * and the keyName is stored as "{contextKey}Name".
     *
     * @param appId Context key containing the application ID
     * @param payload Context key containing the API key generation JSON payload
     * @param contextKey Context key to store the generated API key
     */
    @When("I generate a legacy api key for application {string} with payload {string} as {string}")
    public void iGenerateLegacyApiKey(String appId, String payload, String contextKey) throws IOException, InterruptedException {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getGenerateAPIKeyURL(baseUrl, actualAppId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        if (response.getResponseCode() == 200 || response.getResponseCode() == 201) {
            JSONObject responseJson = new JSONObject(response.getData());
            String apikey = responseJson.getString("apikey");
            TestContext.set(contextKey, apikey);
            if (responseJson.has("keyName")) {
                TestContext.set(contextKey + "Name", responseJson.getString("keyName"));
            }
        }
        Thread.sleep(2000);
    }

    /**
     * Lists legacy API keys for an application and finds the keyUUID by key name.
     *
     * @param keyNameKey Context key containing the key name to search for
     * @param appId Context key containing the application ID
     * @param keyType The key type (PRODUCTION or SANDBOX)
     * @param uuidKey Context key to store the found keyUUID
     */
    @When("I find the keyUUID of legacy api key {string} for application {string} with key type {string} as {string}")
    public void iFindLegacyApiKeyUUID(String keyNameKey, String appId, String keyType, String uuidKey) throws IOException {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String keyName = Utils.resolveFromContext(keyNameKey).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getLegacyApiKeysListURL(baseUrl, actualAppId, keyType), headers);

        TestContext.set("httpResponse", response);

        String data = response.getData();
        JSONArray keysArray;
        if (data.trim().startsWith("[")) {
            keysArray = new JSONArray(data);
        } else {
            JSONObject wrapper = new JSONObject(data);
            keysArray = wrapper.getJSONArray("list");
        }
        for (int i = 0; i < keysArray.length(); i++) {
            JSONObject keyObj = keysArray.getJSONObject(i);
            if (keyName.equals(keyObj.getString("keyName"))) {
                TestContext.set(uuidKey, keyObj.getString("keyUUID"));
                return;
            }
        }
        throw new IOException("No legacy API key found with name: " + keyName);
    }

    /**
     * Regenerates a legacy (application-level) API key using the keyUUID.
     * The new API key value is stored in the context under the given contextKey.
     *
     * @param keyUUID Context key containing the key UUID to regenerate
     * @param appId Context key containing the application ID
     * @param keyType The key type (PRODUCTION or SANDBOX)
     * @param contextKey Context key to store the regenerated API key
     */
    @When("I regenerate legacy api key {string} for application {string} with key type {string} as {string}")
    public void iRegenerateLegacyApiKey(String keyUUID, String appId, String keyType, String contextKey) throws IOException, InterruptedException {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String actualKeyUUID = Utils.resolveFromContext(keyUUID).toString();

        JSONObject payload = new JSONObject();
        payload.put("keyUUID", actualKeyUUID);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + TestContext.get("devportalAccessToken").toString());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getLegacyApiKeyRegenerateURL(baseUrl, actualAppId, keyType), headers,
                        payload.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        if (response.getResponseCode() == 200 || response.getResponseCode() == 201) {
            String apikey = Utils.extractValueFromPayload(response.getData(), "apikey").toString();
            TestContext.set(contextKey, apikey);
        }
        Thread.sleep(2000);
    }

}
