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
import org.jaxen.JaxenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.TestUser;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.bind.DatatypeConverter;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BaseSteps {

    private static final Logger logger = LoggerFactory.getLogger(BaseSteps.class);

    private final String baseUrl;
    private String username;
    private String password;
    private String tenantDomain;
    private String fullUsername;

    public BaseSteps() {

        baseUrl = TestContext.get("baseUrl").toString();
        Object user = TestContext.get("currentUser");

        if (user instanceof TestUser) {
            TestUser testUser = (TestUser) user;
            username = testUser.getUsername();
            tenantDomain = testUser.getTenant() ;
            password = testUser.getPassword();
            fullUsername = username + Constants.CHAR_AT + tenantDomain;
        }
    }

    @Given("The system is ready")
    public void theSystemIsReady() {

        logger.info("Running with user: {}", fullUsername);
    }

    @When("I have a valid DCR application for the current user")
    public void iHaveADCRApplication() throws IOException {

        //Create json payload for DCR endpoint
        JsonObject json = new JsonObject();
        json.addProperty("callbackUrl", "test.com");
        json.addProperty("clientName", "integration_test_app_" + username + "_" + tenantDomain);
        json.addProperty("grantType", "client_credentials password refresh_token");
        json.addProperty("saasApp", true);
        json.addProperty("owner", fullUsername);

        String clientEncoded = DatatypeConverter.printBase64Binary(
                    (fullUsername + ':' + password).getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + clientEncoded);

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
        json.addProperty("username", fullUsername);
        json.addProperty("password", password);
        json.addProperty("scope", "apim:api_view apim:api_create apim:api_publish apim:api_delete apim:api_manage apim:api_import_export apim:subscription_manage apim:client_certificates_add apim:client_certificates_update");

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
        json.addProperty("username", fullUsername);
        json.addProperty("password", password);
        json.addProperty("scope", "apim:app_manage apim:sub_manage apim:subscribe");

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(baseUrl), headers,
            json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());

        String accessToken = Utils.extractValueFromPayload(response.getData(), "access_token").toString();
        TestContext.set("devportalAccessToken", accessToken);
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
    public void putResponsePayloadInContext(String key) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        TestContext.set(Utils.normalizeContextKey(key), response.getData());
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

    @Then("I wait for {int} seconds")
    public void waitForSeconds(int seconds) throws InterruptedException {

        Thread.sleep(seconds * 1000L);
    }

    @Then("I wait for deployment of the API in {string}")
    public void waitForAPIDeployment(String apiDetailsPayload) throws JaxenException, IOException {

        String actualApiDetailsPayload = Utils.resolveFromContext(apiDetailsPayload).toString();

        String apiName  = Utils.extractValueFromPayload(actualApiDetailsPayload, "name").toString();
        String apiVersion = Utils.extractValueFromPayload(actualApiDetailsPayload, "version").toString();

        // todo: how to get these details (api can be deployed by any user, but here we need the admin credentials
        //  of that tenant)
        String tenantAdminUsername = "admin";
        String tenantAdminPassword = "admin";

        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:ns1=\"http://org.apache.axis2/xsd\">" +
                "   <soapenv:Header/>" +
                "   <soapenv:Body>" +
                "      <ns1:getApiNames/>" +
                "   </soapenv:Body>" +
                "</soapenv:Envelope>";

        String url = Utils.getRestApiAdminServiceURL(baseUrl);

        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + Constants.WAIT_TIME;
        boolean isApiDeployed = false;

        while (System.currentTimeMillis() < waitTime) {
            HttpResponse response = null;
            try {
                response = SimpleHTTPClient.getInstance().sendSoapRequest(
                        url, payload, "urn:getApiNames",
                        tenantAdminUsername, tenantAdminPassword);
            } catch (IOException ignored) {
                logger.warn("API :{} with version: {} not yet deployed in tenant: {}", apiName, apiVersion,
                        tenantDomain);
            }

            if (response != null && response.getResponseCode() == 200) {
                List<String> apiNames = Utils.getNodeTextsByXPath(response.getData(),
                        "//*[local-name()='getApiNamesResponse']/*[local-name()='return']");
                if (apiNames.contains(apiName + ":v" + apiVersion)) {
                    isApiDeployed = true;
                    break;
                }
            }
            try {
                logger.info("Wait for availability of API: {} with version: {} in tenant {}", apiName, apiVersion,
                        tenantDomain);
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }
        Assert.assertTrue(isApiDeployed);
    }
}
