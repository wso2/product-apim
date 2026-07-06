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

import com.google.gson.JsonObject;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.RequestAction;
import org.wso2.am.integration.cucumbertests.utils.ServerReadiness;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.bind.DatatypeConverter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class BaseSteps {

    private static final Log log = LogFactory.getLog(BaseSteps.class);

    protected String getBaseUrl() {

        Object baseUrlObj = TestContext.get("baseUrl");
        if (baseUrlObj == null) {
            throw new IllegalStateException("baseUrl is not available in the test context yet");
        }
        return baseUrlObj.toString();
    }

    /**
     * Readiness assertion only. Server boot/readiness is the block lifecycle listener's job, so this step
     * no longer caches any "current" tenant/user — it simply confirms the block published a baseUrl. Kept
     * so existing feature prose ("Given The system is ready") still resolves; carries no ordering contract.
     */
    @Given("The system is ready")
    public void theSystemIsReady() {

        getBaseUrl();
    }

    /**
     * Sets the scenario's acting actor for subsequent no-arg token lookups. Used by access-control scenarios
     * that switch identity mid-scenario (e.g. create a resource as a publisher, then act as a subscriber to
     * prove rejection) and need to switch back — notably so the {@code @cleanup} teardown deletes the
     * publisher-owned resources using the publisher's token rather than the last-acting (powerless) actor.
     */
    @Given("I act as {string}")
    public void iActAs(String actorRef) {
        Identity.setActingActor(actorRef);
    }

    /**
     * Creates a Dynamic Client Registration (DCR) application for the default actor (super-tenant admin).
     */
    @When("I have a valid DCR application for the current user")
    public void iHaveADCRApplication() throws IOException {

        createDcrApplication(Identity.defaultActor());
    }

    /**
     * Creates a DCR application for a named actor (e.g. {@code "userKey1"} or {@code "admin@tenant1.com"}).
     */
    @When("I have a valid DCR application as {string}")
    public void iHaveADCRApplicationAs(String actorRef) throws IOException {

        createDcrApplication(Identity.resolveActor(actorRef));
    }

    private void createDcrApplication(User actor) throws IOException {

        //Create json payload for DCR endpoint
        JsonObject json = new JsonObject();
        json.addProperty("callbackUrl", "test.com");
        json.addProperty("clientName", "integration_test_app_" + actor.getUserNameWithoutDomain() + "_" + actor.getUserDomain());
        json.addProperty("grantType", "client_credentials password refresh_token");
        json.addProperty("saasApp", true);
        json.addProperty("owner", actor.getUserName());

        String encodedCredentials = DatatypeConverter.printBase64Binary(
                    (actor.getUserName() + ':' + actor.getPassword()).getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + encodedCredentials);

        // The gateway health-check can pass before the client-registration webapp finishes deploying, so a
        // DCR POST fired immediately after boot can hit a transient 500 "Dynamic Client Registration Service
        // not available" — a race that parallel runners sharing one freshly-booted container widen. Retry the
        // registration (the failed call creates nothing, so retrying is safe) until it succeeds or the startup
        // window elapses, mirroring TenantUserProvisioner.awaitTenantMgtServiceReady for the admin services.
        String dcrUrl = Utils.getDCREndpointURL(getBaseUrl());
        long deadline = System.currentTimeMillis() + Constants.SERVER_STARTUP_WAIT_TIME;
        HttpResponse dcrResponse = SimpleHTTPClient.getInstance().doPost(dcrUrl, headers, json.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        while (dcrResponse.getResponseCode() != 200 && System.currentTimeMillis() < deadline) {
            log.info("DCR endpoint not ready yet (status " + dcrResponse.getResponseCode()
                    + "); retrying registration...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
            dcrResponse = SimpleHTTPClient.getInstance().doPost(dcrUrl, headers, json.toString(),
                    Constants.CONTENT_TYPES.APPLICATION_JSON);
        }
        Assert.assertEquals(dcrResponse.getResponseCode(), 200, dcrResponse.getData());

        String clientId = Utils.extractValueFromPayload(dcrResponse.getData(), "clientId").toString();
        String clientSecret = Utils.extractValueFromPayload(dcrResponse.getData(), "clientSecret").toString();
        // get base64 encoded "clientId:clientSecret"
        String dcrCredentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret)
                .getBytes(StandardCharsets.UTF_8));

        TestContext.set(Identity.dcrCredentialsKey(actor), dcrCredentials);
    }

    /**
     * Obtains a valid Publisher API access token for the default actor (super-tenant admin).
     */
    @Given("I have a valid Publisher access token for the current user")
    public void iHaveValidPublisherAccessToken() throws Exception {

        mintPublisherToken(Identity.defaultActor());
    }

    /**
     * Obtains a valid Publisher API access token for a named actor.
     */
    @Given("I have a valid Publisher access token as {string}")
    public void iHaveValidPublisherAccessTokenAs(String actorRef) throws Exception {

        mintPublisherToken(Identity.resolveActor(actorRef));
    }

    private void mintPublisherToken(User actor) throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Basic " + TestContext.get(Identity.dcrCredentialsKey(actor)).toString());

        // create json payload to obtain publisher access token
        JsonObject json = new JsonObject();
        json.addProperty("grant_type", "password");
        json.addProperty("username", actor.getUserName());
        json.addProperty("password", actor.getPassword());
        json.addProperty("scope", "apim:api_view apim:api_create apim:api_publish apim:api_delete apim:api_manage apim:api_import_export apim:subscription_manage apim:client_certificates_add apim:client_certificates_update apim:shared_scope_manage apim:common_operation_policy_manage apim:api_generate_key apim:gateway_policy_manage");

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(getBaseUrl()), headers,
            json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());

        String accessToken = Utils.extractValueFromPayload(response.getData(), "access_token").toString();
        TestContext.set(Identity.publisherTokenKey(actor), accessToken);
        log.info("Obtained Publisher access token for user " + actor.getUserName()
                + " with expires_in (seconds): "
                + Utils.extractValueFromPayload(response.getData(), "expires_in"));
    }

    /**
     * Obtains a valid Developer Portal access token for the default actor (super-tenant admin).
     */
    @Given("I have a valid Devportal access token for the current user")
    public void iHaveValidDevportalAccessToken() throws Exception {

        mintDevportalToken(Identity.defaultActor());
    }

    /**
     * Obtains a valid Developer Portal access token for a named actor.
     */
    @Given("I have a valid Devportal access token as {string}")
    public void iHaveValidDevportalAccessTokenAs(String actorRef) throws Exception {

        mintDevportalToken(Identity.resolveActor(actorRef));
    }

    private void mintDevportalToken(User actor) throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Basic " + TestContext.get(Identity.dcrCredentialsKey(actor)).toString());

        // create json payload to obtain devportal access token
        JsonObject json = new JsonObject();
        json.addProperty("grant_type", "password");
        json.addProperty("username", actor.getUserName());
        json.addProperty("password", actor.getPassword());
        json.addProperty("scope", "apim:app_manage apim:sub_manage apim:subscribe");

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(getBaseUrl()), headers,
            json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());

        String accessToken = Utils.extractValueFromPayload(response.getData(), "access_token").toString();
        TestContext.set(Identity.devportalTokenKey(actor), accessToken);
        log.info("Obtained Devportal access token for user " + actor.getUserName()
                + " with expires_in (seconds): "
                + Utils.extractValueFromPayload(response.getData(), "expires_in"));
    }

    /**
     * Obtains a valid Admin access token for the default actor (super-tenant admin).
     */
    public void iHaveValidAdminAccessToken() throws Exception {

        mintAdminToken(Identity.defaultActor());
    }

    private void mintAdminToken(User actor) throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Basic " + TestContext.get(Identity.dcrCredentialsKey(actor)).toString());

        // create json payload to obtain admin access token
        JsonObject json = new JsonObject();
        json.addProperty("grant_type", "password");
        json.addProperty("username", actor.getUserName());
        json.addProperty("password", actor.getPassword());
        json.addProperty("scope", "apim:admin apim:tier_view apim:api_provider_change");

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(getBaseUrl()), headers,
                json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());

        String accessToken = Utils.extractValueFromPayload(response.getData(), "access_token").toString();
        TestContext.set(Identity.adminTokenKey(actor), accessToken);
        log.info("Obtained Admin access token for user " + actor.getUserName()
                + " with expires_in (seconds): "
                + Utils.extractValueFromPayload(response.getData(), "expires_in"));
    }

    /**
     * Composite step that obtains DCR + all access tokens for the default actor (super-tenant admin).
     */
    @Given("The system is ready and I have valid access tokens for current user")
    public void iHaveSystemAndTokens() throws Exception {

        theSystemIsReady();
        User actor = Identity.defaultActor();
        createDcrApplication(actor);
        mintPublisherToken(actor);
        mintDevportalToken(actor);
        mintAdminToken(actor);
    }

    /**
     * Composite step that obtains DCR + all access tokens for a named actor (incl. the admin token, so the
     * actor must have admin rights). Records the actor as the scenario's acting actor so subsequent no-arg
     * token lookups in the glue resolve to it.
     */
    @Given("I have valid access tokens as {string}")
    public void iHaveTokensAs(String actorRef) throws Exception {

        Identity.setActingActor(actorRef);
        User actor = Identity.resolveActor(actorRef);
        createDcrApplication(actor);
        mintPublisherToken(actor);
        mintDevportalToken(actor);
        mintAdminToken(actor);
    }

    /**
     * Composite step for a least-privilege publisher actor: DCR + publisher + devportal tokens only (NO admin
     * token, since a non-admin publisher user is denied the {@code apim:admin} scope). Records the actor as the
     * scenario's acting actor so the publisher glue's no-arg token lookups resolve to it. This is the default
     * Background for publisher-plane features, parameterizable over a {@code Scenario Outline} actor column
     * (e.g. {@code publisherUser} vs {@code publisherUser@tenant1.com}).
     */
    @Given("The system is ready and I have valid publisher access tokens as {string}")
    public void iHavePublisherTokensAs(String actorRef) throws Exception {

        theSystemIsReady();
        Identity.setActingActor(actorRef);
        User actor = Identity.resolveActor(actorRef);
        createDcrApplication(actor);
        mintPublisherToken(actor);
        mintDevportalToken(actor);
    }

    /**
     * Composite step that obtains DCR + devportal token only for the default actor (super-tenant admin).
     * Useful for subscriber-only users who do not have publisher or admin permissions.
     */
    @Given("The system is ready and I have valid devportal access token for current user")
    public void iHaveSystemAndDevportalToken() throws Exception {

        theSystemIsReady();
        User actor = Identity.defaultActor();
        createDcrApplication(actor);
        mintDevportalToken(actor);
    }

    /**
     * Composite step for a DevPortal consumer actor: DCR + devportal token only, for a named actor. Records
     * the actor as the scenario's acting actor so the devportal glue's no-arg token lookups resolve to it.
     * This is the default Background for devportal-plane features (application CRUD, subscribe, etc.),
     * parameterizable over a {@code Scenario Outline} actor column (e.g. {@code subscriberUser} vs
     * {@code subscriberUser@tenant1.com}).
     */
    @Given("The system is ready and I have valid devportal access token as {string}")
    public void iHaveDevportalTokenAs(String actorRef) throws Exception {

        theSystemIsReady();
        Identity.setActingActor(actorRef);
        User actor = Identity.resolveActor(actorRef);
        createDcrApplication(actor);
        mintDevportalToken(actor);
    }

    /**
     * Stores a generic string value or a value from a different context key into the test context.
     *
     * @param value The raw string value or a context key to resolve
     * @param contextKey The key under which the value should be stored in TestContext
     */
    @When("I put value {string} in context as {string}")
    public void iPutValueInContextAs(String value, String contextKey) {
        // Resolve value if it's a reference to another context key
        Object resolvedValue = Utils.resolveFromContext(value);

        log.info("Setting context key: " + contextKey + " with value: " + resolvedValue);
        TestContext.set(Utils.normalizeContextKey(contextKey), resolvedValue.toString());
    }

    /**
     * Loads a JSON payload from a file and stores it in the test context.
     *
     * @param jsonFilePath Path to the JSON file
     * @param key Context key to store the JSON payload
     */
    @When("I put JSON payload from file {string} in context as {string}")
    public void putJsonPayloadFromFile(String jsonFilePath, String key) throws IOException {

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(jsonFilePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("File not found on classpath: " + jsonFilePath);
            }
            String jsonPayload = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            TestContext.set(Utils.normalizeContextKey(key), Utils.resolvePayloadPlaceholders(jsonPayload));
        }
    }

    /**
     * Stores a JSON payload in the test context.
     *
     * @param key Context key to store the JSON payload
     * @param docStringJson JSON payload provided as a doc string
     */
    @When("I put the following JSON payload in context as {string}")
    public void putJsonPayloadInContext(String key, String docStringJson)  {

        TestContext.set(Utils.normalizeContextKey(key), Utils.resolvePayloadPlaceholders(docStringJson));
    }

    /**
     * Stores the most recent HTTP response payload in the test context.
     *
     * @param key Context key to store the response payload
     */
    @When("I put the response payload in context as {string}")
    public void putResponsePayloadInContext(String key) throws InterruptedException {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        TestContext.set(Utils.normalizeContextKey(key), response.getData());
        Thread.sleep(Constants.WAIT_TIME);
    }

    /**
     * Verifies that the HTTP response status code matches the expected value.
     *
     * @param expectedStatusCode The expected HTTP status code
     */
    @Then("The response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatusCode) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertEquals(response.getResponseCode(), expectedStatusCode, response.getData());
    }


    /**
     * Retrieves a previously prepared request and executes it until
     * the desired HTTP status code is received or the maximum retry limit is reached.
     *
     * @param expectedCode The expected HTTP status code
     * @throws InterruptedException ]
     */
    @And("I wait until the response status code is {int}")
    public void iWaitUntilStatus(int expectedCode) throws InterruptedException {

        // Retrieve prepared request logic from context
        RequestAction requestAction = Utils.getPendingHttpRequest();

        try {
            // Execute with retry loop until expected status is met
            HttpResponse finalResponse = Utils.executeWithRetry(requestAction, expectedCode, res -> true);
            TestContext.set(Constants.HTTP_RESPONSE, finalResponse);

            Assert.assertEquals(finalResponse.getResponseCode(), expectedCode,
                    "The request failed to return the expected status code after retries." +
                            finalResponse.getData());

            String httpMethod = (String) TestContext.get(Constants.HTTP_METHOD);

            // Check if the HTTP method is DELETE
            if (Constants.HTTP_METHODS.DELETE.equalsIgnoreCase(httpMethod)) {
                Thread.sleep(2000);
            }

        } finally {
            TestContext.remove(Constants.HTTP_METHOD);
        }
    }

    /**
     * Waits until the pending HTTP request returns the expected response status code
     * and the specified JSON response field contains the expected value.
     *
     * @param expectedCode  the expected HTTP response status code
     * @param fieldName     the JSON response field to validate
     * @param expectedValue the expected value of the specified JSON field
     * @throws InterruptedException if the retry wait is interrupted
     * @throws IOException          if an error occurs while extracting the field value
     *                              from the response payload
     */
    @Then("I wait until the response status code is {int} and the value of response field {string} is {string}")
    public void iWaitUntilStatusAndFieldValue(int expectedCode, String fieldName, String expectedValue) throws InterruptedException, IOException {

        RequestAction requestAction = Utils.getPendingHttpRequest();

        HttpResponse finalResponse = Utils.executeWithRetry(requestAction, expectedCode,
                response -> {
                    // Extract value from JSON
                    Object actualValue;
                    try {
                        actualValue = Utils.extractValueFromPayload(response.getData(), fieldName);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    // Return true if value matches
                    return actualValue != null && String.valueOf(actualValue).equalsIgnoreCase(expectedValue);
                }
        );

        TestContext.set("httpResponse", finalResponse);

        Assert.assertEquals(finalResponse.getResponseCode(), expectedCode, "HTTP Status Code mismatch.");

        Object actualFieldVal = Utils.extractValueFromPayload(finalResponse.getData(), fieldName);
        Assert.assertTrue(expectedValue.equalsIgnoreCase(String.valueOf(actualFieldVal)),
                String.format("Field '%s' was [%s] but expected [%s]. Data: %s",
                        fieldName, actualFieldVal, expectedValue, finalResponse.getData()));
    }

    /**
     * Verifies that the HTTP response body contains the specified string value.
     *
     * @param expectedValue The string value that should be present in the response body
     */
    @Then("The response should contain {string}")
    public void responseShouldContainFieldValue(String expectedValue) {

        // Resolve any {{contextKey}} placeholders so assertions can reference uniquely-generated values
        // (e.g. a ${UNIQUE:...} API name captured into context). Literals without {{}} pass through unchanged.
        expectedValue = Utils.resolveContextPlaceholders(expectedValue);
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response.getData().contains(expectedValue),
                "Expected response to contain '" + expectedValue + "' but it did not: " + response.getData());
    }

    /**
     * Extracts a value from the stored HTTP response payload and saves it in TestContext.
     *
     * @param responseField field name or JSONPath to extract from the response body
     * @param contextKey context key under which the extracted value should be stored
     * @throws IOException if the HTTP response is missing or the field is not found
     */
    @Then("I extract response field {string} and store it as {string}")
    public void iExtractResponseFieldAndStoreItAs(String responseField, String contextKey) throws IOException {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        if (response == null) {
            throw new IOException("No HTTP response found in TestContext.");
        }

        Object value = Utils.extractValueFromPayload(response.getData(), responseField);
        if (value == null) {
            throw new IOException("No value found in response for field: " + responseField);
        }

        if (value instanceof net.minidev.json.JSONArray) {
            value = new JSONArray(value.toString());
        } else if (value instanceof net.minidev.json.JSONObject) {
            value = new JSONObject(value.toString());
        }
        TestContext.set(Utils.normalizeContextKey(contextKey), value);
    }

    /**
     * Extracts a field or JSONPath value from a JSON payload stored in TestContext
     * and stores the extracted value back in TestContext.
     *
     * @param sourceContextKey context key containing the source JSON payload
     * @param fieldPath field name or JSONPath to extract from the stored payload
     * @param targetContextKey context key under which the extracted value should be stored
     * @throws IOException if the source payload is missing or the field is not found
     */
    @And("I extract field {string} from json payload {string} and store it as {string}")
    public void iExtractFieldFromJsonPayloadAndStoreItAs(String fieldPath, String sourceContextKey,
                                                         String targetContextKey) throws IOException {

        Object sourceValue = Utils.resolveFromContext(sourceContextKey);

        Object value = Utils.extractValueFromPayload(String.valueOf(sourceValue), fieldPath);
        if (value == null) {
            throw new IOException("No value found in payload for field: " + fieldPath);
        }

        if (value instanceof net.minidev.json.JSONArray) {
            value = new JSONArray(value.toString());
        } else if (value instanceof net.minidev.json.JSONObject) {
            value = new JSONObject(value.toString());
        }

        TestContext.set(Utils.normalizeContextKey(targetContextKey), value);
    }

    /**
     * Extracts a field value from a JSONObject stored in TestContext and stores it under another key.
     *
     * @param sourceKey TestContext key containing the JSONObject
     * @param fieldName JSON field name to extract
     * @param targetKey TestContext key to store the extracted value
     */
    @And("I extract field {string} from {string} and store it as {string}")
    public void iExtractFieldFromAndStoreItAs(String fieldName, String sourceKey, String targetKey) {

        Object contextValue = Utils.resolveFromContext(sourceKey);

        if (!(contextValue instanceof JSONObject jsonObject)) {
            throw new IllegalStateException("Expected JSONObject in TestContext for key '" + sourceKey
                            + "' but found: " + contextValue.getClass().getSimpleName());
        }

        if (!jsonObject.has(fieldName)) {
            throw new AssertionError(
                    "Field '" + fieldName + "' not found in object stored under key '" + sourceKey + "'");
        }

        Object extractedValue = jsonObject.get(fieldName);
        TestContext.set(Utils.normalizeContextKey(targetKey), extractedValue);
    }

    /**
     * Verifies that the HTTP response body does not contain the specified string value.
     *
     * @param unexpectedValue The string value that should not be present in the response body
     */
    @Then("The response should not contain {string}")
    public void responseShouldNotContainFieldValue(String unexpectedValue) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertFalse(response.getData().contains(unexpectedValue));
    }

    /**
     * Verifies that the HTTP response contains a specific header with the expected value.
     *
     * @param headerName The name of the HTTP header to check
     * @param expectedValue The expected value of the header
     */
    @Then("The response should contain the header {string} with value {string}")
    public void responseShouldContainHeaderWithValue(String headerName, String expectedValue) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response.getHeaders().containsKey(headerName), "Header " + headerName + " not found in response");
        Assert.assertEquals(response.getHeaders().get(headerName), expectedValue, "Header value mismatch for " + headerName);
    }

    /**
     * Verifies that the HTTP response does not contain a specific header with the specified value.
     *
     * @param headerName The name of the HTTP header to check
     * @param expectedValue The value that should not be present in the header
     */
    @And("The response should not contain the header {string} with value {string}")
    public void theResponseShouldNotContainTheHeaderWithValue(String headerName, String expectedValue) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertFalse(response.getHeaders().containsKey(headerName), "Header " + headerName + "found in response");
        Assert.assertNotEquals(response.getHeaders().get(headerName), expectedValue, "Header value match for " + headerName);

    }

    /**
     * Verifies that a resource reflects an updated configuration value.
     *
     * @param resourceType The type of resource to check
     * @param config The configuration field name to verify
     * @param expectedConfigValue The expected configuration value
     */
    @And("The {string} resource should reflect the updated {string} as:")
    public void theResourceShouldReflectTheUpdatedAs(String resourceType, String config, String expectedConfigValue) throws IOException, InterruptedException {
        // Get the API ID from the update response
        HttpResponse updateResponse = (HttpResponse) TestContext.get("httpResponse");
        JSONObject updateResponseJson = new JSONObject(updateResponse.getData());
        String resourceId = updateResponseJson.optString("id", null);
        User actor = Identity.defaultActor();
        String tenantDomain = actor.getUserDomain();

        if ("endpointConfig".equals(config)){
            expectedConfigValue = Utils.resolveFromContext(expectedConfigValue).toString();
        }

        Object parsedExpectedValue = Utils.parseConfigValue(expectedConfigValue);
        String normalizedConfigValue = String.valueOf(parsedExpectedValue);

        if ("provider".equals(config)) {
            if (tenantDomain != null && !Constants.SUPER_TENANT_DOMAIN.equals(tenantDomain)) {
                if (!normalizedConfigValue.contains("@")) {
                    normalizedConfigValue = normalizedConfigValue + "@" + tenantDomain;
                }
            }
        }

        if (resourceId == null || resourceId.isEmpty()) {
            verifyConfigurationInResponse(updateResponse, config, normalizedConfigValue);
            return;
        }

        // Retry mechanism: retrieve the API and check until the configuration matches
        int maxRetries = 30;
        int delayMs = 4000;
        boolean configMatches = false;
        HttpResponse retrievedResponse = null;

        for (int i = 0; i < maxRetries; i++) {
            log.info("[Attempt " + i + "/" + maxRetries + "] Fetching resource " + resourceType + " with ID: "
                    + resourceId);
            Map<String, String> headers = new HashMap<>();
            headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                    "Bearer " + Identity.publisherToken(actor));

            retrievedResponse = SimpleHTTPClient.getInstance().doGet(
                    Utils.getResourceEndpointURL(getBaseUrl(), resourceType, resourceId), headers);

            if (retrievedResponse.getResponseCode() == 200) {
                try {
                    verifyConfigurationInResponse(retrievedResponse, config, normalizedConfigValue);
                    configMatches = true;
                    break;
                } catch (AssertionError e) {
                    // Configuration doesn't match yet, retry
                    if (i < maxRetries - 1) {
                        Thread.sleep(delayMs);
                    } else {
                        throw e;
                    }
                }
            } else {
                if (i == 0) {
                    verifyConfigurationInResponse(updateResponse, config, normalizedConfigValue);
                    return;
                }
                Thread.sleep(delayMs);
            }
        }

        // Final fall back
        if (!configMatches) {
            log.warn("Criteria not met. Falling back to initial update response verification.");
            verifyConfigurationInResponse(updateResponse, config, normalizedConfigValue);
        }
    }

    @And("The {string} resource should reflect the updated {string} as value from context {string}")
    public void theResourceShouldReflectTheUpdatedAsValueFromContext(String resourceType, String config,
               String configValueContextKey) throws IOException, InterruptedException {

        Object ctxValue = Utils.resolveFromContext(configValueContextKey);
        theResourceShouldReflectTheUpdatedAs(resourceType, config, ctxValue.toString());
    }

    /**
     * Helper method that verifies a specific configuration field in the HTTP response matches the expected value.
     *
     * @param response The HTTP response containing the configuration to verify
     * @param config The configuration field name to check
     * @param configValue The expected configuration value
     */
    private void verifyConfigurationInResponse(HttpResponse response, String config, String configValue) {
        JSONObject json = new JSONObject(response.getData());
        Assert.assertTrue(json.has(config), "Configuration '" + config + "' not found in response");

        Object actualValue = json.get(config);

        // Handle JSON true/false, numbers, or strings
        if (actualValue instanceof Boolean) {
            Assert.assertEquals(actualValue.toString(), configValue,
                    "Expected boolean " + configValue + " but got " + actualValue);
        } else if (actualValue instanceof Number) {
            Assert.assertEquals(String.valueOf(actualValue), configValue,
                    "Expected numeric " + configValue + " but got " + actualValue);
        } else if (actualValue instanceof JSONArray) {
            JSONArray expectedArray = new JSONArray(configValue);
            JSONArray actualArray = (JSONArray) actualValue;

            Assert.assertTrue(actualArray.similar(expectedArray),
                    "JSON Arrays do not match. Expected (order-insensitive): " + expectedArray
                            + "But got: " + actualArray);

        } else if (actualValue instanceof JSONObject) {
            JSONObject expectedObject = new JSONObject(configValue);
            JSONObject actualObject = (JSONObject) actualValue;

            Assert.assertTrue(actualObject.similar(expectedObject), "Expected JSON object:\n" + expectedObject
                    + "\nbut got:\n" + actualObject);
        } else {
            Assert.assertEquals(actualValue.toString(), configValue,
                    "Expected string " + configValue + " but got " + actualValue);
        }
    }

    /**
     * Waits for the APIM server to be ready by polling the gateway health check endpoint.
     */
    @Then("I wait for the APIM server to be ready")
    public void waitForAPIMServerToBeReady() {

        boolean isServerReady = ServerReadiness.awaitReady(getBaseUrl());
        Assert.assertTrue(isServerReady, "APIM server is not ready even after waiting for " +
                Constants.DEPLOYMENT_WAIT_TIME /60000 + " minutes");
    }

    /**
     * Waits for an API to be deployed in the gateway.
     *
     * @param apiDetailsPayload Context key containing the API details JSON payload
     */
    @Then("I wait for deployment of the resource in {string}")
    public void waitForAPIDeployment(String apiDetailsPayload) throws IOException, InterruptedException {

        String actualApiDetailsPayload = Utils.resolveFromContext(apiDetailsPayload).toString();

        String apiName  = Utils.extractValueFromPayload(actualApiDetailsPayload, "name").toString();
        String apiVersion = Utils.extractValueFromPayload(actualApiDetailsPayload, "version").toString();
        // Use the tenant ADMIN (not the acting actor) — the gateway-artifact admin endpoint requires admin
        // credentials, which a least-privilege publisher actor does not have.
        User tenantAdmin = Identity.actingTenantAdmin();
        String tenantDomain = tenantAdmin.getUserDomain();

        String url = Utils.getAPIArtifactDeployedInGatewayURL(getBaseUrl(), apiName, apiVersion, tenantDomain);

        String encodedCredentials = DatatypeConverter.printBase64Binary(
                (tenantAdmin.getUserName() + ':' + tenantAdmin.getPassword()).getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + encodedCredentials);

        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + Constants.DEPLOYMENT_WAIT_TIME;
        boolean isApiDeployed = false;

        while (System.currentTimeMillis() < waitTime) {
            HttpResponse response = null;
            try {
                response = SimpleHTTPClient.getInstance().doGet(url, headers);
            } catch (IOException ignored) {
                log.warn("API :" + apiName + " with version: " + apiVersion + " not yet deployed in tenant: " +
                        tenantDomain);
            }
            if (response != null && response.getResponseCode() == 200) {
                    isApiDeployed = true;
                    break;
            }
            try {
                log.info("Wait for availability of API: " + apiName + " with version: " + apiVersion +
                        " in tenant " + tenantDomain);
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }
        Assert.assertTrue(isApiDeployed);
        Thread.sleep(10000);
    }

    /**
     * Waits for a previous API revision to be undeployed from the gateway.
     *
     * @param apiDetailsPayload Context key containing the API details JSON payload
     */
    @Then("I wait for undeployment of the previous API revision in {string}")
    public void waitForPreviousAPIRevisionUndeployment(String apiDetailsPayload) throws IOException {

        String actualApiDetailsPayload = Utils.resolveFromContext(apiDetailsPayload).toString();

        String apiName  = Utils.extractValueFromPayload(actualApiDetailsPayload, "name").toString();
        String apiVersion = Utils.extractValueFromPayload(actualApiDetailsPayload, "version").toString();
        // Use the tenant ADMIN (not the acting actor) — the gateway-artifact admin endpoint requires admin
        // credentials, which a least-privilege publisher actor does not have.
        User tenantAdmin = Identity.actingTenantAdmin();
        String tenantDomain = tenantAdmin.getUserDomain();

        String url = Utils.getAPIArtifactDeployedInGatewayURL(getBaseUrl(), apiName, apiVersion, tenantDomain);

        String encodedCredentials = DatatypeConverter.printBase64Binary(
                (tenantAdmin.getUserName() + ':' + tenantAdmin.getPassword()).getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + encodedCredentials);

        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + Constants.UNDEPLOYMENT_WAIT_TIME;

        while (System.currentTimeMillis() < waitTime) {
            HttpResponse response = null;
            try {
                response = SimpleHTTPClient.getInstance().doGet(url, headers);
            } catch (IOException ignored) {}
            if (response != null && response.getResponseCode() == 404) {
                log.info("Previous API revision is undeployed successfully");
                break;
            }
            try {
                log.info("Wait for undeployment of API: " + apiName + " with version: " + apiVersion +
                        " in tenant " + tenantDomain);
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Extracts a value from a JSON payload using the given path and stores it in the test context.
     *
     * @param payloadContextKey the context key of the JSON payload
     * @param path the JSON path used to extract the value
     * @param outputContextKey the context key used to store the extracted value
     */
    @When("I get the value from json payload {string} at path {string} and store it as {string}")
    public void iGetTheValueFromPayloadAtPathAndStoreItAs(String payloadContextKey, String path,
                                                          String outputContextKey) throws IOException {

        Object ctxValue = Utils.resolveFromContext(payloadContextKey);
        Object value = Utils.extractValueFromPayload(ctxValue.toString(), path);
        TestContext.set(Utils.normalizeContextKey(outputContextKey), String.valueOf(value));
    }

    /**
     * Appends a parsed JSON value to the JSON array stored in the test context.
     *
     * @param arrayContextKey the context key of the target JSON array
     * @param valueToAppend the JSON value to append to the array
     */
    @And("I append the following value to the json array {string}:")
    public void iAppendTheFollowingValueToTheJsonArray(String arrayContextKey, String valueToAppend) {

        Object ctxValue = Utils.resolveFromContext(arrayContextKey);
        JSONArray jsonArray;
        // Convert the stored context value into a JSONArray
        if (ctxValue instanceof JSONArray) {
            jsonArray = (JSONArray) ctxValue;
        } else {
            jsonArray = new JSONArray(ctxValue.toString());
        }
        Object parsedValue = Utils.parseConfigValue(valueToAppend);
        jsonArray.put(parsedValue);
        TestContext.set(Utils.normalizeContextKey(arrayContextKey), jsonArray.toString());
    }

    /**
     * Finds a resource in the JSON array stored in the given context key using
     * the provided property-value pairs, and stores the matched object in TestContext.
     * Expected DataTable format:
     * | name    | addHeader |
     * | version | v1        |
     *
     * @param contextKey the TestContext key containing the JSONArray
     * @param outputKey the TestContext key to store the matched JSONObject
     * @param propertiesTable property-value pairs used for matching
     */
    @And("I find the resource with following properties in {string} as {string}")
    public void iFindTheResourceWithFollowingPropertiesInAs(String contextKey, String outputKey,
                                                            DataTable propertiesTable) {

        Object contextValue = Utils.resolveFromContext(contextKey);
        if (!(contextValue instanceof JSONArray jsonArray)) {
            throw new IllegalStateException(
                    "Expected JSONArray in TestContext for key '" + contextKey + "' but found: "
                            + contextValue.getClass().getSimpleName());
        }

        // Get the raw map from the DataTable
        Map<String, String> rawProperties = propertiesTable.asMap(String.class, String.class);
        Map<String, String> resolvedProperties = new HashMap<>();

        // Resolve if values are context keys
        for (Map.Entry<String, String> entry : rawProperties.entrySet()) {
            Object resolvedValue = Utils.resolveIfContextKey(entry.getValue());
            resolvedProperties.put(entry.getKey(), String.valueOf(resolvedValue));
        }

        JSONObject matchedObject = Utils.findMatchingJsonObjectInArray(jsonArray, resolvedProperties);
        TestContext.set(Utils.normalizeContextKey(outputKey), matchedObject);
    }

    /**
     * Verifies that the actual value stored in context matches the expected value.
     * Expected value can be a literal string or a context key.
     *
     * @param actualKey TestContext key containing the actual value
     * @param expectedValue expected value as a string
     */
    @Then("the actual value of {string} should match the expected value:")
    public void theActualValueShouldMatchTheExpectedValue(String actualKey, String expectedValue) {

        Object actualValue = Utils.resolveFromContext(actualKey);
        String finalExpectedValue = Utils.resolveIfContextKey(expectedValue).toString();
        Utils.assertConfigValueMatchesExpectedValue(actualValue, finalExpectedValue);
    }
}
