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
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.bind.DatatypeConverter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.wso2.am.integration.test.Constants.CHAR_AT;

public class BaseSteps {

    protected final TestContext context;
    private String baseUrl;

    public BaseSteps(TestContext context) {

        this.context = context;
        baseUrl = context.get("baseUrl").toString();
    }

    @Given("The system is ready")
    public void theSystemIsReady() {
    }

    @When("I have a valid DCR application for username {string}, password {string} and tenant {string}")
    public void iHaveADCRApplication(String username, String password, String tenantDomain) throws IOException {

        //Create json payload for DCR endpoint
        JsonObject json = new JsonObject();
        json.addProperty("callbackUrl", "test.com");
        json.addProperty("clientName", "integration_test_app_publisher");
        json.addProperty("grantType", "client_credentials password refresh_token");
        json.addProperty("saasApp", true);
        json.addProperty("owner", username + CHAR_AT + tenantDomain);

        String clientEncoded = DatatypeConverter.printBase64Binary(
                    (username + CHAR_AT + tenantDomain + ':' + password).getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + clientEncoded);

        HttpResponse dcrResponse = SimpleHTTPClient.getInstance().doPost(Utils.getDCREndpointURL(baseUrl), headers, json.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);

        context.set("httpResponse", dcrResponse);

        String clientId = Utils.extractValueFromPayload(dcrResponse.getData(), "clientId").toString();
        String clientSecret = Utils.extractValueFromPayload(dcrResponse.getData(), "clientSecret").toString();
        // get base64 encoded "clientId:clientSecret"
        String dcrCredentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret)
                .getBytes(StandardCharsets.UTF_8));

        context.set("dcrCredentials", dcrCredentials);
    }

    @Given("I have a valid Publisher access token with username {string} and password {string}")
    public void iHaveValidPublisherAccessToken(String username, String password) throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + context.get("dcrCredentials").toString());

        // create json payload to obtain publisher access token
        JsonObject json = new JsonObject();
        json.addProperty("grant_type", "password");
        json.addProperty("username", username);
        json.addProperty("password", password);
        json.addProperty("scope", "apim:api_view apim:api_create apim:api_publish apim:api_delete apim:api_manage apim:api_import_export apim:subscription_manage apim:client_certificates_add apim:client_certificates_update");

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(baseUrl), headers,
            json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);

        String accessToken = Utils.extractValueFromPayload(response.getData(), "access_token").toString();
        context.set("publisherAccessToken", accessToken);
    }

    @Given("I have a valid Devportal access token with username {string} and password {string}")
    public void iHaveValidDevportalAccessToken(String username, String password) throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + context.get("dcrCredentials").toString());

        // create json payload to obtain devportal access token
        JsonObject json = new JsonObject();
        json.addProperty("grant_type", "password");
        json.addProperty("username", username);
        json.addProperty("password", password);
        json.addProperty("scope", "apim:app_manage apim:sub_manage apim:subscribe");

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(baseUrl), headers,
            json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);

        String accessToken = Utils.extractValueFromPayload(response.getData(), "access_token").toString();
        context.set("devportalAccessToken", accessToken);
    }

    @When("I put JSON payload from file {string} in context as {string}")
    public void putJsonPayloadFromFile(String jsonFilePath, String key) throws IOException {

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(jsonFilePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("File not found on classpath: " + jsonFilePath);
            }
            String jsonPayload = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            context.set(Utils.normalizeContextKey(key), jsonPayload);
        }
    }

    @When("I put the following JSON payload in context as {string}")
    public void putJsonPayloadInContext(String key, String docStringJson)  {

        context.set(Utils.normalizeContextKey(key), docStringJson);
    }

    @When("I put the response payload in context as {string}")
    public void putResponsePayloadInContext(String key) {

        HttpResponse response = (HttpResponse) context.get("httpResponse");
        context.set(Utils.normalizeContextKey(key), response.getData());
    }


    @Then("The response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatusCode) {

        HttpResponse response = (HttpResponse) context.get("httpResponse");
        Assert.assertEquals(response.getResponseCode(), expectedStatusCode, response.getData());
    }

    @Then("The response should contain {string}")
    public void responseShouldContainFieldValue(String expectedValue) {

        HttpResponse response = (HttpResponse) context.get("httpResponse");;
        Assert.assertTrue(response.getData().contains(expectedValue));
    }

    @Then("The response should not contain {string}")
    public void responseShouldNotContainFieldValue(String unexpectedValue) {

        HttpResponse response = (HttpResponse) context.get("httpResponse");
        Assert.assertFalse(response.getData().contains(unexpectedValue));
    }

    @Then("The response should contain the header {string} with value {string}")
    public void responseShouldContainHeaderWithValue(String headerName, String expectedValue) {

        HttpResponse response = (HttpResponse) context.get("httpResponse");
        Assert.assertTrue(response.getHeaders().containsKey(headerName), "Header " + headerName + " not found in response");
        Assert.assertEquals(response.getHeaders().get(headerName), expectedValue, "Header value mismatch for " + headerName);
    }

    @Then("I wait for {int} seconds")
    public void waitForSeconds(int seconds) throws InterruptedException {

        Thread.sleep(seconds * 1000L);
    }
}
