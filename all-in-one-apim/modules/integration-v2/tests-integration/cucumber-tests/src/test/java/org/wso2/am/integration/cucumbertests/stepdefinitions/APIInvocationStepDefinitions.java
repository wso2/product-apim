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
import org.wso2.am.integration.test.Constants;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class APIInvocationStepDefinitions {

    private final TestContext context;
    String baseGatewayUrl;

    public APIInvocationStepDefinitions(TestContext context) {
        this.context = context;
        baseGatewayUrl= this.context.get("baseGatewayUrl").toString();
    }

    @When("I get the generated access token from file {string}")
    public void iGetTheGeneratedAccessTokenFromFile(String accessTokenFilePath) throws Exception {

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(accessTokenFilePath);
        if (inputStream == null) {
            throw new FileNotFoundException("Access token file not found on classpath: " + accessTokenFilePath);
        }

        String jsonContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> accessTokensMap = objectMapper.readValue(jsonContent, new TypeReference<>() {});
        context.set("generatedAccessToken", accessTokensMap.get(System.getenv(Constants.API_MANAGER_DATABASE_TYPE)));
    }

    @When("I invoke the API resource at path {string} with method {string} using access token {string} and payload {string}")
    public void invokeApiUsingAccessToken(String path,String httpMethod, String accessToken, String payload) throws Exception {

        String actualAccessToken = Utils.resolveFromContext(context, accessToken).toString();
        String endpointUrl = baseGatewayUrl + path;

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + actualAccessToken);

        CurlOption.HttpMethod method = CurlOption.HttpMethod.valueOf(httpMethod.toUpperCase());
        switch (method) {
            case GET:
                context.set("httpResponse", SimpleHTTPClient.getInstance().doGet(endpointUrl, headers));
                break;
            case DELETE:
                context.set("httpResponse", SimpleHTTPClient.getInstance().doDelete(endpointUrl, headers));
                break;
            case POST:
                context.set("httpResponse", SimpleHTTPClient.getInstance().doPost(endpointUrl, headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON));
                break;
            case PUT:
                context.set("httpResponse", SimpleHTTPClient.getInstance().doPut(endpointUrl, headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON));
                break;
        }
    }
}
