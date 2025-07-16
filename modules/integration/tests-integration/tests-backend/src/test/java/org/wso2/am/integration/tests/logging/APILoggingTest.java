/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.am.integration.tests.logging;

import org.apache.axis2.AxisFault;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.tests.restapi.RESTAPITestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.ServerConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class APILoggingTest extends APIManagerLifecycleBaseTest {
    private String apiId;
    private String applicationId;

    @Factory(dataProvider = "userModeDataProvider")
    public APILoggingTest(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN}, new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, AxisFault, XPathExpressionException {
        super.init();
    }

    @Test(groups = {"wso2.am" }, description = "Sending http request to per API logging enabled API: ")
    public void testAPIPerAPILoggingTestcase() throws Exception {

        // Get list of APIs without any API
        Map<String, String> header = new HashMap<>();
        byte[] encodedBytes = Base64.encodeBase64(RESTAPITestConstants.BASIC_AUTH_HEADER
                .getBytes(StandardCharsets.UTF_8));
        header.put("Authorization", "Basic " + new String(encodedBytes, StandardCharsets.UTF_8));
        header.put("Content-Type", "application/json");
        HttpResponse loggingResponse = HTTPSClientUtils.doGet(getStoreURLHttps()
                + "api/am/devops/v0/tenant-logs/carbon.super/apis", header);
        assertEquals(loggingResponse.getData(), "{\"apis\":[]}");

        String API_NAME = "APILoggingTestAPI";
        String API_CONTEXT = "apiloggingtest";
        String API_TAGS = "testTag1, testTag2, testTag3";
        String API_END_POINT_POSTFIX_URL = "xmlapi";
        String API_VERSION = "1.0.0";
        String APPLICATION_NAME = "APILoggingTestApp";

        // Create an application
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application AccessibilityOfBlockAPITestCase", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        // Create an API and subscribe to it using created application
        APIRequest apiRequest;
        String apiEndPointUrl = getAPIInvocationURLHttp(API_END_POINT_POSTFIX_URL, API_VERSION);
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTags(API_TAGS);
        apiRequest.setProvider(user.getUserName());
        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore,
                applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        // Get list of APIs with an API
        loggingResponse = HTTPSClientUtils.doGet(getStoreURLHttps()
                + "api/am/devops/v0/tenant-logs/carbon.super/apis", header);
        assertEquals(loggingResponse.getData(), "{\"apis\":[{\"context\":\"/" + API_CONTEXT + "/" + API_VERSION
                + "\"," + "\"logLevel\":\"OFF\",\"apiId\":\"" + apiId + "\"}]}");

        // Change logLevel to FULL
        String addNewLoggerPayload = "{ \"logLevel\": \"FULL\" }";
        HTTPSClientUtils.doPut(getStoreURLHttps() + "api/am/devops/v0/tenant-logs/carbon.super/apis/" + apiId, header,
                addNewLoggerPayload);

        // Get list of APIs which have log-level=FULL
        loggingResponse = HTTPSClientUtils.doGet(getStoreURLHttps()
                + "api/am/devops/v0/tenant-logs/carbon.super/apis?log-level=full", header);
        assertEquals(loggingResponse.getData(), "{\"apis\":[{\"context\":\"/" + API_CONTEXT + "/" + API_VERSION +
                "\"," + "\"logLevel\":\"FULL\",\"apiId\":\"" + apiId + "\"}]}");

        // Invoke the API
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add("client_credentials");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        assertNotNull(applicationKeyDTO.getToken());
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        HttpClient client = HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier()).build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION));
        request.setHeader("Authorization", "Bearer " + accessToken);
        org.apache.http.HttpResponse response = client.execute(request);
        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");

        // Validate API Logs
        String apiLogFilePath = System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "repository"
                + File.separator + "logs" + File.separator + "api.log";
        BufferedReader bufferedReader = new BufferedReader(new FileReader(apiLogFilePath));
        String logLine;
        while ((logLine = bufferedReader.readLine()) != null) {
            assertTrue(logLine.contains("INFO {API_LOG} " + API_NAME));
        }
    }

    @AfterClass(alwaysRun = true)
    void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId);
    }
}
