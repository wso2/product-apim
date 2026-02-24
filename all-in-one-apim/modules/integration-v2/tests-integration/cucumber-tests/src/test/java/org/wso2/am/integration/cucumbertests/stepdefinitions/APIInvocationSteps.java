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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.core.options.CurlOption;
import io.cucumber.java.en.When;
import org.apache.commons.io.IOUtils;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.Tenant;
import org.wso2.carbon.automation.engine.context.beans.User;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class APIInvocationSteps {

    private final String baseGatewayUrl;
    private final Tenant tenant;
    private User currentuser;

    public APIInvocationSteps() {

        baseGatewayUrl= TestContext.get("baseGatewayUrl").toString();
        tenant = Utils.getTenantFromContext("currentTenant");
        currentuser = tenant.getContextUser();
    }

    /**
     * Loads a pre-generated access token from a JSON file based on the current database type,
     * tenant domain, and username. The token is stored in the test context for use in API invocations.
     *
     * @param accessTokenFilePath Path to the JSON file containing access tokens
     */
    @When("I get the generated access token from file {string}")
    public void iGetTheGeneratedAccessTokenFromFile(String accessTokenFilePath) throws Exception {

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(accessTokenFilePath);
        if (inputStream == null) {
            throw new FileNotFoundException("Access token file not found on classpath: " + accessTokenFilePath);
        }

        String jsonContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> accessTokensMap = objectMapper.readValue(jsonContent, new TypeReference<>() {});

        String dbType = System.getenv(Constants.API_MANAGER_DATABASE_TYPE);
        if (dbType == null) {
            throw new IllegalStateException("DB type is not set in environment variables");
        }

        String tenantDomain = tenant.getDomain();
        String username = currentuser.getUserNameWithoutDomain();

        if (tenantDomain == null || username == null) {
            throw new IllegalStateException(
                    "Tenant domain or username not found in TestContext. " +
                            "tenantDomain=" + tenantDomain + ", username=" + username);
        }

        String tokenKey = dbType + "|" + tenantDomain + "|" + username;

        String accessToken = accessTokensMap.get(tokenKey);
        if (accessToken == null) {
            throw new IllegalStateException(
                    "No access token found for key: " + tokenKey +
                            ". Available keys: " + accessTokensMap.keySet());
        }

        TestContext.set("generatedAccessToken", accessToken);
    }

    /**
     * Invokes an API endpoint using an OAuth2 access token for authentication.
     * Supports GET, POST, PUT, and DELETE HTTP methods. The access token is resolved from the test context
     * and included in the Authorization header as a Bearer token.
     * 
     * @param path The API resource path to invoke
     * @param httpMethod The HTTP method to use (GET, POST, PUT, or DELETE)
     * @param accessToken Context key containing the access token to use for authentication
     * @param payload Context key containing the request payload
     */
    @When("I invoke the API resource at path {string} with method {string} using access token {string} and payload {string}")
    public void invokeApiUsingAccessToken(String path,String httpMethod, String accessToken, String payload) throws Exception {

        String actualAccessToken = Utils.resolveFromContext(accessToken).toString();
        String actualPayload = (payload == null || payload.isEmpty()) ? "" : Utils.resolveFromContext(payload).toString();
        String endpointUrl = Utils.getAPIInvocationURL(baseGatewayUrl, path, tenant.getDomain());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualAccessToken);

        CurlOption.HttpMethod method = CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase());
        switch (method) {
            case GET:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doGet(endpointUrl, headers));
                break;
            case DELETE:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doDelete(endpointUrl, headers));
                break;
            case POST:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doPost(endpointUrl, headers, actualPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON));
                break;
            case PUT:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doPut(endpointUrl, headers, actualPayload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON));
                break;
        }
    }

    /**
     * Invokes an API endpoint using an API Key for authentication.
     * Supports GET, POST, PUT, and DELETE HTTP methods. The API key is resolved from the test context
     * and included in the ApiKey header.
     * 
     * @param path The API resource path to invoke
     * @param httpMethod The HTTP method to use (GET, POST, PUT, or DELETE)
     * @param apikey Context key containing the API key to use for authentication
     */
    @When("I invoke the API resource at path {string} with method {string} using api key {string}")
    public void invokeApiUsingKey(String path, String httpMethod, String apikey) throws Exception {

        String actualKey = Utils.resolveFromContext(apikey).toString();
        String endpointUrl = Utils.getAPIInvocationURL(baseGatewayUrl, path, tenant.getDomain());

        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("ApiKey", actualKey);

        CurlOption.HttpMethod method = CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase());
        switch (method) {
            case GET:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doGet(endpointUrl, headers));
                break;
            case DELETE:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doDelete(endpointUrl, headers));
                break;
            case POST:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doPost(endpointUrl, headers, "", Constants.CONTENT_TYPES.APPLICATION_JSON));
                break;
            case PUT:
                TestContext.set("httpResponse", SimpleHTTPClient.getInstance().doPut(endpointUrl, headers, "", Constants.CONTENT_TYPES.APPLICATION_JSON));
                break;
        }
    }

    /**
     * Invokes a SOAP API endpoint using an OAuth2 access token for authentication.
     * This step sets the appropriate Content-Type header for SOAP (text/xml) and includes the SOAPAction header
     * if provided. The payload should be a SOAP envelope in XML format.
     * 
     * @param path The SOAP API resource path to invoke
     * @param accessToken Context key containing the access token to use for authentication
     * @param payload Context key containing the SOAP request payload (XML format)
     * @param soapAction The SOAPAction header value
     */
    @When("I invoke the SOAP API at path {string} using access token {string} and payload {string} and soap action {string}")
    public void iInvokeTheSOAPAPIAtPathUsingAccessTokenAndPayloadAndSoapAction(String path, String accessToken, String payload, String soapAction) throws IOException {

        String actualAccessToken = Utils.resolveFromContext(accessToken).toString();
        String actualPayload = Utils.resolveFromContext(payload).toString();
        String endpointUrl = Utils.getAPIInvocationURL(baseGatewayUrl, path, tenant.getDomain());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualAccessToken);
        headers.put("Content-Type", Constants.CONTENT_TYPES.TEXT_XML);
        if (soapAction != null && !soapAction.isEmpty()) {
            headers.put("SOAPAction", soapAction);
        }

        TestContext.set("httpResponse", SimpleHTTPClient.getInstance()
                .doPost(endpointUrl, headers, actualPayload, Constants.CONTENT_TYPES.TEXT_XML));
    }
}
