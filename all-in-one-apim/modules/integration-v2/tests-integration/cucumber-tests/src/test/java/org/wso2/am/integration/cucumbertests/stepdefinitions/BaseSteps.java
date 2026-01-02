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
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.carbon.automation.engine.context.beans.Tenant;
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

    private static final Logger logger = LoggerFactory.getLogger(BaseSteps.class);

    private final String baseUrl;
    private Tenant tenant;
    private User currentuser;

    public BaseSteps() {

        baseUrl = TestContext.get("baseUrl").toString();
    }

    @Given("The system is ready")
    public void theSystemIsReady() {

        tenant = Utils.getTenantFromContext("currentTenant");
        currentuser = tenant.getContextUser();
        logger.info("Running with user: {}", currentuser.getUserName());
    }

    @When("I have a valid DCR application for the current user")
    public void iHaveADCRApplication() throws IOException {

        //Create json payload for DCR endpoint
        JsonObject json = new JsonObject();
        json.addProperty("callbackUrl", "test.com");
        json.addProperty("clientName", "integration_test_app_" + currentuser.getUserNameWithoutDomain() + "_" + currentuser.getUserDomain());
        json.addProperty("grantType", "client_credentials password refresh_token");
        json.addProperty("saasApp", true);
        json.addProperty("owner", currentuser.getUserName());

        String encodedCredentials = DatatypeConverter.printBase64Binary(
                    (currentuser.getUserName() + ':' + currentuser.getPassword()).getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + encodedCredentials);

        HttpResponse dcrResponse = SimpleHTTPClient.getInstance().doPost(Utils.getDCREndpointURL(baseUrl), headers, json.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(dcrResponse.getResponseCode(), 200, dcrResponse.getData());

        String clientId = Utils.extractValueFromPayload(dcrResponse.getData(), "clientId").toString();
        String clientSecret = Utils.extractValueFromPayload(dcrResponse.getData(), "clientSecret").toString();
        // get base64 encoded "clientId:clientSecret"
        String dcrCredentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret)
                .getBytes(StandardCharsets.UTF_8));

        TestContext.set("dcrCredentials", dcrCredentials);
    }

    @Given("I have a valid Publisher access token for the current user")
    public void iHaveValidPublisherAccessToken() throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + TestContext.get("dcrCredentials").toString());

        // create json payload to obtain publisher access token
        JsonObject json = new JsonObject();
        json.addProperty("grant_type", "password");
        json.addProperty("username", currentuser.getUserName());
        json.addProperty("password", currentuser.getPassword());
        json.addProperty("scope", "apim:api_view apim:api_create apim:api_publish apim:api_delete apim:api_manage apim:api_import_export apim:subscription_manage apim:client_certificates_add apim:client_certificates_update apim:shared_scope_manage apim:common_operation_policy_manage apim:api_generate_key apim:gateway_policy_manage");

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(baseUrl), headers,
            json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());

        String accessToken = Utils.extractValueFromPayload(response.getData(), "access_token").toString();
        TestContext.set("publisherAccessToken", accessToken);
    }

    @Given("I have a valid Devportal access token for the current user")
    public void iHaveValidDevportalAccessToken() throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + TestContext.get("dcrCredentials").toString());

        // create json payload to obtain devportal access token
        JsonObject json = new JsonObject();
        json.addProperty("grant_type", "password");
        json.addProperty("username", currentuser.getUserName());
        json.addProperty("password", currentuser.getPassword());
        json.addProperty("scope", "apim:app_manage apim:sub_manage apim:subscribe");

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(baseUrl), headers,
            json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());

        String accessToken = Utils.extractValueFromPayload(response.getData(), "access_token").toString();
        TestContext.set("devportalAccessToken", accessToken);
    }

    public void iHaveValidAdminAccessToken() throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + TestContext.get("dcrCredentials").toString());

        // create json payload to obtain admin access token
        JsonObject json = new JsonObject();
        json.addProperty("grant_type", "password");
        json.addProperty("username", currentuser.getUserName());
        json.addProperty("password", currentuser.getPassword());
        json.addProperty("scope", "apim:admin apim:tier_view apim:api_provider_change");

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(baseUrl), headers,
                json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());

        String accessToken = Utils.extractValueFromPayload(response.getData(), "access_token").toString();
        TestContext.set("adminAccessToken", accessToken);
    }

    // Composite function to Combine the above four steps to reduce length of feature definition
    @Given("The system is ready and I have valid access tokens for current user")
    public void iHaveSystemAndTokens() throws Exception {

        theSystemIsReady();
        iHaveADCRApplication();
        iHaveValidPublisherAccessToken();
        iHaveValidDevportalAccessToken();
        iHaveValidAdminAccessToken();
    }

    @When("I put JSON payload from file {string} in context as {string}")
    public void putJsonPayloadFromFile(String jsonFilePath, String key) throws IOException {

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(jsonFilePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("File not found on classpath: " + jsonFilePath);
            }
            String jsonPayload = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            TestContext.set(Utils.normalizeContextKey(key), jsonPayload);
        }
    }

    @When("I put the following JSON payload in context as {string}")
    public void putJsonPayloadInContext(String key, String docStringJson)  {

        TestContext.set(Utils.normalizeContextKey(key), docStringJson);
    }

    @When("I put the response payload in context as {string}")
    public void putResponsePayloadInContext(String key) throws InterruptedException {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        TestContext.set(Utils.normalizeContextKey(key), response.getData());
        Thread.sleep(Constants.INITIAL_INDEXING_TIME);
    }

    @Then("The response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatusCode) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertEquals(response.getResponseCode(), expectedStatusCode, response.getData());
    }

    @Then("The response should contain {string}")
    public void responseShouldContainFieldValue(String expectedValue) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");;
        Assert.assertTrue(response.getData().contains(expectedValue));
    }

    @Then("The response should not contain {string}")
    public void responseShouldNotContainFieldValue(String unexpectedValue) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertFalse(response.getData().contains(unexpectedValue));
    }

    @Then("The response should contain the header {string} with value {string}")
    public void responseShouldContainHeaderWithValue(String headerName, String expectedValue) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response.getHeaders().containsKey(headerName), "Header " + headerName + " not found in response");
        Assert.assertEquals(response.getHeaders().get(headerName), expectedValue, "Header value mismatch for " + headerName);
    }

    @And("The response should not contain the header {string} with value {string}")
    public void theResponseShouldNotContainTheHeaderWithValue(String headerName, String expectedValue) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertFalse(response.getHeaders().containsKey(headerName), "Header " + headerName + "found in response");
        Assert.assertNotEquals(response.getHeaders().get(headerName), expectedValue, "Header value match for " + headerName);

    }

    /**
     * Verifies that a resource (e.g., API) reflects an updated configuration value.
     * This step implements a retry mechanism that polls the resource endpoint until the configuration matches
     * the expected value, accounting for eventual consistency in distributed systems.
     *
     * @param resourceType The type of resource to check
     * @param config The configuration field name to verify
     * @param configValue The expected configuration value
     */
    @And("The {string} resource should reflect the updated {string} as:")
    public void theResourceShouldReflectTheUpdatedAs(String resourceType, String config, String configValue) throws IOException, InterruptedException {
        // Get the API ID from the update response
        HttpResponse updateResponse = (HttpResponse) TestContext.get("httpResponse");
        JSONObject updateResponseJson = new JSONObject(updateResponse.getData());
        String resourceId = updateResponseJson.optString("id", null);

        if ("endpointConfig".equals(config)){
            configValue = Utils.resolveFromContext(configValue).toString();
        }

        if (resourceId == null || resourceId.isEmpty()) {
            verifyConfigurationInResponse(updateResponse, config, configValue);
            return;
        }

        // Retry mechanism: retrieve the API and check until the configuration matches
        int maxRetries = 20;
        int delayMs = 3000;
        boolean configMatches = false;
        HttpResponse retrievedResponse = null;

        for (int i = 0; i < maxRetries; i++) {
            Map<String, String> headers = new HashMap<>();
            headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                    "Bearer " + TestContext.get("publisherAccessToken").toString());

            retrievedResponse = SimpleHTTPClient.getInstance().doGet(
                    Utils.getResourceEndpointURL(baseUrl,resourceType,resourceId), headers);

            if (retrievedResponse.getResponseCode() == 200) {
                try {
                    verifyConfigurationInResponse(retrievedResponse, config, configValue);
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
                    verifyConfigurationInResponse(updateResponse, config, configValue);
                    return;
                }
                Thread.sleep(delayMs);
            }
        }

        // Final fall back
        if (!configMatches) {
            verifyConfigurationInResponse(updateResponse, config, configValue);
        }
    }

    /**
     * Helper method which verifies that a specific configuration field in the HTTP response matches the expected value.
     * This method handles different data types including booleans, numbers, JSON arrays, JSON objects, and strings.
     *
     * @param response The HTTP response containing the configuration to verify
     * @param config The configuration field name to check
     * @param configValue The expected configuration value (as a string representation)
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

            Assert.assertEquals(actualArray.toString(), expectedArray.toString(),
                    "Expected array " + expectedArray + " but got " + actualArray);
        } else if (actualValue instanceof JSONObject) {
            JSONObject expectedObject = new JSONObject(configValue);
            JSONObject actualObject = (JSONObject) actualValue;

            Assert.assertTrue(actualObject.similar(expectedObject), "Expected JSON object:\n" + expectedObject
                    + "\nbut got:\n" + actualObject);
        } else {
            Assert.assertEquals(actualValue, configValue,
                    "Expected string " + configValue + " but got " + actualValue);
        }
    }

    @Then("I wait for {int} seconds")
    public void waitForSeconds(int seconds) throws InterruptedException {

        Thread.sleep(seconds * 1000L);
    }

    @Then("I wait for the APIM server to be ready")
    public void waitForAPIMServerToBeReady() throws IOException, InterruptedException {

        String url = Utils.getGatewayHealthCheckURL(baseUrl);
        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + Constants.SERVER_STARTUP_WAIT_TIME;
        boolean isServerReady = false;

        while (System.currentTimeMillis() < waitTime) {
            HttpResponse response = null;
            try {
                response = SimpleHTTPClient.getInstance().doGet(url, null);
            } catch (IOException ignored) {}
            if (response != null && response.getResponseCode() == 200) {
                    isServerReady = true;
                    break;
            }
            try {
                logger.info("Waiting for APIM server to be ready...");
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
        Assert.assertTrue(isServerReady, "APIM server is not ready even after waiting for " +
                Constants.DEPLOYMENT_WAIT_TIME /60000 + " minutes");
    }

    @Then("I wait for deployment of the resource in {string}")
    public void waitForAPIDeployment(String apiDetailsPayload) throws IOException, InterruptedException {

        String actualApiDetailsPayload = Utils.resolveFromContext(apiDetailsPayload).toString();

        String apiName  = Utils.extractValueFromPayload(actualApiDetailsPayload, "name").toString();
        String apiVersion = Utils.extractValueFromPayload(actualApiDetailsPayload, "version").toString();
        User tenantAdmin = tenant.getTenantAdmin();

        String url = Utils.getAPIArtifactDeployedInGatewayURL(baseUrl, apiName, apiVersion, tenant.getDomain());

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
                logger.warn("API :{} with version: {} not yet deployed in tenant: {}", apiName, apiVersion,
                        tenant.getDomain());
            }
            if (response != null && response.getResponseCode() == 200) {
                    isApiDeployed = true;
                    break;
            }
            try {
                logger.info("Wait for availability of API: {} with version: {} in tenant {}", apiName, apiVersion,
                        tenant.getDomain());
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }
        Assert.assertTrue(isApiDeployed);
        Thread.sleep(10000);
    }

    @Then("I wait for undeployment of the previous API revision in {string}")
    public void waitForPreviousAPIRevisionUndeployment(String apiDetailsPayload) throws IOException {

        String actualApiDetailsPayload = Utils.resolveFromContext(apiDetailsPayload).toString();

        String apiName  = Utils.extractValueFromPayload(actualApiDetailsPayload, "name").toString();
        String apiVersion = Utils.extractValueFromPayload(actualApiDetailsPayload, "version").toString();
        User tenantAdmin = tenant.getTenantAdmin();

        String url = Utils.getAPIArtifactDeployedInGatewayURL(baseUrl, apiName, apiVersion, tenant.getDomain());

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
                logger.info("Previous API revision is undeployed successfully");
                break;
            }
            try {
                logger.info("Wait for undeployment of API: {} with version: {} in tenant {}", apiName, apiVersion,
                        tenant.getDomain());
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
