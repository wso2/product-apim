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
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class ApplicationBaseSteps {

    BaseSteps baseSteps = new BaseSteps();

    private String getBaseUrl() {

        return baseSteps.getBaseUrl();
    }

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
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse applicationCreateResponse = SimpleHTTPClient.getInstance()
                .doPost(Utils.getApplicationCreateURL(getBaseUrl()), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", applicationCreateResponse);
        Assert.assertEquals(applicationCreateResponse.getResponseCode(), 201, applicationCreateResponse.getData());
        Object createdAppId = Utils.extractValueFromPayload(applicationCreateResponse.getData(), "applicationId");
        TestContext.set("createdAppId", createdAppId);
        // Register for scenario teardown so a shared-server suite does not accumulate applications across scenarios.
        ResourceCleanup.register(Constants.CREATED_APPLICATION_IDS, createdAppId);
    }

    /**
     * Attempts to create an application without asserting success, storing the raw response as
     * {@code httpResponse} for the feature to assert. For negative / access-control scenarios where the
     * create is expected to be rejected (e.g. a non-consumer actor lacking the app-management scope): unlike
     * the positive step it neither extracts an id nor registers anything for cleanup.
     */
    @When("I attempt to create an application with payload {string}")
    public void iAttemptToCreateAnApplicationWithPayload(String payload) throws IOException {

        String jsonPayload = Utils.resolveFromContext(payload).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getApplicationCreateURL(getBaseUrl()), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
        TestContext.set("httpResponse", response);
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
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse applicationDeleteResponse = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getApplicationEndpointURL(getBaseUrl(), actualAppId), headers);

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
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse applicationRetrieveResponse = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApplicationEndpointURL(getBaseUrl(), actualAppId), headers);

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
                "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApplicationSearchURL(getBaseUrl(), applicationName), headers);

        TestContext.set("httpResponse", response);

        JSONObject responseJson = new JSONObject(response.getData());
        if (responseJson.has("list") && !responseJson.getJSONArray("list").isEmpty()) {
            String applicationId = responseJson
                    .getJSONArray("list")
                    .getJSONObject(0)
                    .getString("applicationId");
            TestContext.set(appId, applicationId);
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
                "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance().doPut(
                Utils.getApplicationEndpointURL(getBaseUrl(), actualAppId), headers, jsonPayload,
                Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
    }

    /**
     * Creates a subscription between an application and an API.
     * The payload is updated with the actual application ID and API ID before sending the request.
     * The created subscription ID is stored in the test context.
     * 
     * @param apiId Context key containing the API ID to subscribe to
     * @param appId Context key containing the application ID to use for subscription
     * @param payload Context key containing the subscription creation JSON payload
     * @param subscriptionID Context key where the created subscription ID will be stored
     */
    @When("I subscribe to API {string} using application {string} with payload {string} as {string}")
    public void iSubscribeToApi(String apiId, String appId, String payload, String subscriptionID) throws Exception {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String actualAppId = Utils.resolveFromContext(appId).toString();

        // Add application id and API id to the payload
        String jsonPayload = Utils.resolveFromContext(payload).toString();
        jsonPayload = jsonPayload.replace("{{applicationId}}", actualAppId);
        jsonPayload = jsonPayload.replace("{{apiId}}", actualApiId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getCreateSubscriptionURL(getBaseUrl()),
                headers, jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        TestContext.set(subscriptionID,Utils.extractValueFromPayload(response.getData(), "subscriptionId"));
    }

    /**
     * Attempts to subscribe an application to an API without asserting success, storing the raw response as
     * {@code httpResponse} for the feature to assert. For negative / access-control scenarios where the
     * subscribe is expected to be rejected (e.g. an actor lacking the apim:subscribe scope): unlike the
     * positive step it neither asserts a status nor records a subscription id.
     */
    @When("I attempt to subscribe to API {string} using application {string} with payload {string}")
    public void iAttemptToSubscribeToApi(String apiId, String appId, String payload) throws Exception {

        String actualApiId = Utils.resolveFromContext(apiId).toString();
        String actualAppId = Utils.resolveFromContext(appId).toString();

        String jsonPayload = Utils.resolveFromContext(payload).toString();
        jsonPayload = jsonPayload.replace("{{applicationId}}", actualAppId);
        jsonPayload = jsonPayload.replace("{{apiId}}", actualApiId);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getCreateSubscriptionURL(getBaseUrl()),
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
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getAllSubscriptionsURL(getBaseUrl(), actualApiId, actualAppId, null, null,
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
     * The consumer secret and key mapping ID from the first key are extracted and stored in the test context.
     * 
     * @param appId Context key containing the application ID
     */
    @When("I retrieve existing application keys for {string}")
    public void iRetrieveExistingApplicationKeys(String appId) throws IOException {

        String actualAppId = Utils.resolveFromContext(appId).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApplicationAllKeys(getBaseUrl(), actualAppId), headers);

        TestContext.set("httpResponse", response);

        JSONObject responseJson = new JSONObject(response.getData());
        if (responseJson.has("list") && !responseJson.getJSONArray("list").isEmpty()) {
            JSONObject firstKey = responseJson
                    .getJSONArray("list")
                    .getJSONObject(0);

            String consumerSecret = firstKey.optString("consumerSecret", null);
            String keyMappingId = firstKey.optString("keyMappingId", null);

            if (consumerSecret != null) {
                TestContext.set("consumerSecret", consumerSecret);
            }

            if (keyMappingId != null) {
                TestContext.set("keyMappingId", keyMappingId);
            }
        } else {
            throw new IOException("No application keys found in response");
        }

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
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPut(Utils.getUpdateKey(getBaseUrl(), actualAppId, keyMappingId), headers, jsonPayload,
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
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doDelete(Utils.getUpdateKey(getBaseUrl(), actualAppId, keyMappingId), headers);
        TestContext.set("httpResponse", response);
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
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getGenerateApplicationKeysURL(getBaseUrl(), actualAppId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        TestContext.set("consumerKey", Utils.extractValueFromPayload(response.getData(), "consumerKey"));
        TestContext.set("consumerSecret", Utils.extractValueFromPayload(response.getData(), "consumerSecret"));
        TestContext.set("keyMappingId", Utils.extractValueFromPayload(response.getData(), "keyMappingId"));
    }

    /**
     * Requests an OAuth2 access token for an application using the generated client credentials.
     * The consumer secret from the context is injected into the payload before sending the request.
     * The generated access token is stored in the test context.
     * 
     * @param appId Context key containing the application ID
     * @param payload Context key containing the token request JSON payload
     */
    @When("I request an access token for application id {string} using payload {string}")
    public void iRequestAccessToken(String appId, String payload) throws Exception {

        String actualAppId = Utils.resolveFromContext(appId).toString();
        String keyMappingId = Utils.resolveFromContext("keyMappingId").toString();
        String consumerSecret = Utils.resolveFromContext("consumerSecret").toString();

        // Add consumer secret to the payload
        String jsonPayload = Utils.resolveFromContext(payload).toString();
        jsonPayload = jsonPayload.replace("{{appConsumerSecret}}", consumerSecret);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getGenerateApplicationTokenURL(getBaseUrl(), actualAppId, keyMappingId), headers, jsonPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON);

        TestContext.set("httpResponse", response);
        String accessToken = Utils.extractValueFromPayload(response.getData(), "accessToken").toString();
        TestContext.set("generatedAccessToken", accessToken);
    }

    /**
     * Requests an OAuth2 access token for the current user via the token endpoint using the
     * password grant, authenticated with the application's generated client credentials
     * (consumerKey/consumerSecret in context). The raw token response is stored as "httpResponse",
     * the access token as "generatedAccessToken", and the refresh token (if any) as "refreshToken".
     *
     * @param scope OAuth scope to request (may be empty for no explicit scope)
     */
    @When("I request an OAuth access token for the current user using password grant with scope {string}")
    public void iRequestOAuthAccessTokenWithScope(String scope) throws Exception {

        User currentUser = Identity.defaultActor();

        StringBuilder body = new StringBuilder("grant_type=password")
                .append("&username=").append(urlEncode(currentUser.getUserName()))
                .append("&password=").append(urlEncode(currentUser.getPassword()));
        if (scope != null && !scope.isEmpty()) {
            body.append("&scope=").append(urlEncode(scope));
        }

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(getBaseUrl()),
                clientCredentialsHeader(), body.toString(), Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);

        TestContext.set("httpResponse", response);
        captureTokens(response);
    }

    /**
     * Requests a new access token using the refresh-token grant, authenticated with the application's
     * client credentials. Stores the new tokens and the raw response in context.
     *
     * @param refreshTokenKey Context key holding the refresh token to exchange
     */
    @When("I request a new OAuth access token using refresh token {string}")
    public void iRequestTokenUsingRefreshToken(String refreshTokenKey) throws Exception {

        String refreshToken = Utils.resolveFromContext(refreshTokenKey).toString();

        String body = "grant_type=refresh_token&refresh_token=" + urlEncode(refreshToken);

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(getBaseUrl()),
                clientCredentialsHeader(), body, Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);

        TestContext.set("httpResponse", response);
        captureTokens(response);
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Revokes the given OAuth access token via the revocation endpoint, authenticated with the
     * application's client credentials. Stores the response in context.
     *
     * @param tokenKey Context key holding the access token to revoke
     */
    @When("I revoke the OAuth access token {string}")
    public void iRevokeOAuthAccessToken(String tokenKey) throws Exception {

        String token = Utils.resolveFromContext(tokenKey).toString();

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getRevokeEndpointURL(getBaseUrl()),
                clientCredentialsHeader(), "token=" + token,
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);

        TestContext.set("httpResponse", response);
    }

    /**
     * Builds a Basic auth header from the application's generated client credentials
     * (consumerKey/consumerSecret) held in context.
     */
    private Map<String, String> clientCredentialsHeader() {

        String consumerKey = Utils.resolveFromContext("consumerKey").toString();
        String consumerSecret = Utils.resolveFromContext("consumerSecret").toString();
        String credentials = Base64.getEncoder().encodeToString(
                (consumerKey + ":" + consumerSecret).getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + credentials);
        return headers;
    }

    /**
     * Extracts access_token and refresh_token (when present) from a token-endpoint response into context.
     * Tolerates responses without a refresh token (e.g. some scope/grant combinations).
     */
    private void captureTokens(HttpResponse response) {

        JSONObject json;
        try {
            json = new JSONObject(response.getData());
        } catch (Exception e) {
            return;
        }
        if (json.has("access_token")) {
            TestContext.set("generatedAccessToken", json.getString("access_token"));
        }
        if (json.has("refresh_token")) {
            TestContext.set("refreshToken", json.getString("refresh_token"));
        }
    }

    /**
     * Asserts that the access token stored as "generatedAccessToken" is a self-contained JWT:
     * three base64url-encoded segments whose header decodes to JSON containing an "alg" claim.
     */
    @Then("The generated access token should be in JWT format")
    public void theGeneratedAccessTokenShouldBeInJWTFormat() {

        String token = Utils.resolveFromContext("generatedAccessToken").toString();
        String[] parts = token.split("\\.");
        Assert.assertEquals(parts.length, 3,
                "Access token is not in JWT format (expected 3 dot-separated segments): " + token);

        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        JSONObject header = new JSONObject(headerJson);
        Assert.assertTrue(header.has("alg"),
                "JWT header does not contain an 'alg' claim: " + headerJson);
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
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(Utils.getGenerateAPIKeyURL(getBaseUrl(), actualAppId), headers, jsonPayload,
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
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance().doDelete(Utils.getSubscriptionURL(getBaseUrl(),
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
        // Set the throttling policy to the requested plan regardless of the current value or JSON spacing
        // (the payload may be a pretty-printed template or a compact subscription response).
        jsonPayload = jsonPayload.replaceAll("(\"throttlingPolicy\"\\s*:\\s*)\"[^\"]*\"",
                "$1\"" + subscriptionPlan + "\"");

        // Resolve application/api id placeholders when the payload is a hand-built template. When the
        // payload is taken from an actual subscription response (e.g. migration flow) it carries real
        // ids and no placeholders, so these replacements are a no-op.
        if (jsonPayload.contains("{{applicationId}}") && TestContext.contains("createdAppId")) {
            jsonPayload = jsonPayload.replace("{{applicationId}}", TestContext.get("createdAppId").toString());
        }
        if (jsonPayload.contains("{{apiId}}") && TestContext.contains("createdApiId")) {
            jsonPayload = jsonPayload.replace("{{apiId}}", TestContext.get("createdApiId").toString());
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance().doPut(Utils.getSubscriptionURL(getBaseUrl(), actualSubscriptionId),
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
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance().doGet(Utils.getSubscriptionURL(getBaseUrl(),
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
     * Composite step definition for,
     * Application creation - put the 'createdAppId' in context
     * Generate credentials for application - put 'consumerKey', 'consumerSecret' , and 'keyMappingId' in context
     * Subscribe to a given apiId - put 'subscriptionId' in context
     * Generate access tokens - put 'generatedAccessToken' in context
     *
     * @param apiId Api to be subscribed
     */
    @When("I have set up application with keys, subscribed to API {string}, and obtained access token for {string}")
    public void iSetupApplicationSubscribeAndGetToken(String apiId, String subscriptionID) throws Exception {

        // create an application
        baseSteps.putJsonPayloadFromFile("artifacts/payloads/create_apim_test_app.json", "<createAppPayload>");
        iCreateAnApplicationWithJsonPayload("<createAppPayload>");
        baseSteps.theResponseStatusCodeShouldBe(201);

        // generate credentials for application
        baseSteps.putJsonPayloadInContext("<generateApplicationKeysPayload>", "{\"keyType\": \"PRODUCTION\"," +
                "\"grantTypesToBeSupported\": [\"client_credentials\"]}");
        iGenerateClientCredentialsForApplication("<createdAppId>", "<generateApplicationKeysPayload>");
        baseSteps.theResponseStatusCodeShouldBe(200);

        // subscribe to an api with that created application
        baseSteps.putJsonPayloadInContext("<apiSubscriptionPayload>", "{\"applicationId\": \"{{applicationId}}\"," +
                "\"apiId\": \"{{apiId}}\",\"throttlingPolicy\": \"Bronze\"}");
        iSubscribeToApi(apiId, "<createdAppId>", "<apiSubscriptionPayload>", subscriptionID);
        baseSteps.theResponseStatusCodeShouldBe(201);

        // generate access token
        baseSteps.putJsonPayloadInContext("<createApplicationAccessTokenPayload>", "{\"consumerSecret\": \"{{appConsumerSecret}}\"," +
                "\"validityPeriod\": 3600}");
        iRequestAccessToken("<createdAppId>", "<createApplicationAccessTokenPayload>");
        baseSteps.theResponseStatusCodeShouldBe(200);
    }

    /**
     * Composite step definition for,
     * Application creation - put the 'createdAppId' in context
     * Generate credentials for application - put 'consumerKey', 'consumerSecret' , and 'keyMappingId' in context
     */
    @When("I have set up a application with keys")
    public void iHaveSetUpAApplicationWithKeys() throws Exception {

        // create an application
        baseSteps.putJsonPayloadFromFile("artifacts/payloads/create_apim_test_app.json", "<createAppPayload>");
        iCreateAnApplicationWithJsonPayload("<createAppPayload>");

        // generate credentials for application
        baseSteps.putJsonPayloadInContext("<generateApplicationKeysPayload>", "{\"keyType\": \"PRODUCTION\"," +
                "\"grantTypesToBeSupported\": [\"client_credentials\"]}");
        iGenerateClientCredentialsForApplication("<createdAppId>", "<generateApplicationKeysPayload>");
        baseSteps.theResponseStatusCodeShouldBe(200);
    }

    /**
     * Composite step definition for,
     * Subscribe to a given apiId - put 'subscriptionId' in context
     * Generate access tokens - put 'generatedAccessToken' in context
     *
     * @param resourceID resource to be subscribed
     */
    @And("I subscribe to resource {string}, with {string} and obtained access token for {string} with scope {string}")
    public void iSubscribeToResourceAndObtainedAccessToken(String resourceID, String appId, String subscriptionID, String scope) throws Exception {

        // subscribe to an api with that created application
        baseSteps.putJsonPayloadInContext("<apiSubscriptionPayload>", "{\"applicationId\": \"{{applicationId}}\"," +
                "\"apiId\": \"{{apiId}}\",\"throttlingPolicy\": \"Bronze\"}");
        iSubscribeToApi(resourceID, appId, "<apiSubscriptionPayload>", subscriptionID);

        // generate access token
        String tokenPayload;
        if (scope != null && !scope.isEmpty()) {
            tokenPayload = "{\"consumerSecret\": \"{{appConsumerSecret}}\"," +
                    "\"validityPeriod\": 3600," +
                    "\"scopes\": [\"" + scope + "\"]}";
        } else {
            tokenPayload = "{\"consumerSecret\": \"{{appConsumerSecret}}\"," +
                    "\"validityPeriod\": 3600}";
        }

        baseSteps.putJsonPayloadInContext("<createApplicationAccessTokenPayload>", tokenPayload);
        iRequestAccessToken(appId, "<createApplicationAccessTokenPayload>");
    }

    /**
     * Searches for APIs in the Developer Portal using a search query.
     * The search query can include filters such as name, version, provider, etc.
     *
     * @param query The search query string
     */
    @When("I search DevPortal APIs with query {string}")
    public void iSearchDevPortalAPIsWithQuery(String query) throws IOException, InterruptedException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + Identity.devportalToken());

        // Resolve any {{contextKey}} placeholders so a query can target a uniquely-generated value, e.g.
        // "name:{{createdApiName}}" — necessary now that resource names are randomized by ${UNIQUE:...}.
        query = Utils.resolveContextPlaceholders(query);
        String url = Utils.getApiSearchURL(getBaseUrl(), query);
        long endTime = System.currentTimeMillis() + Constants.DEPLOYMENT_WAIT_TIME;

        // DevPortal search is backed by an asynchronous (Solr) index, so a freshly published API may
        // not be searchable immediately. Retry while the result set is empty until it appears or times out.
        HttpResponse response;
        while (true) {
            response = SimpleHTTPClient.getInstance().doGet(url, headers);
            boolean empty = response == null || response.getResponseCode() != 200
                    || new JSONObject(response.getData()).optInt("count", 0) == 0;
            if (!empty || System.currentTimeMillis() >= endTime) {
                break;
            }
            Thread.sleep(2000);
        }

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
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getApiDocumentsURL(getBaseUrl(), actualApiId), headers);

        TestContext.set("httpResponse", response);
    }
}
