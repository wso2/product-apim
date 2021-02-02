/*
*  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.wso2.am.integration.tests.other;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APICorsConfigurationDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class APIMANAGER3965TestCase extends APIManagerLifecycleBaseTest {
    private String apiName = "APIMANAGER3965";
    private String apiContext = "apimanager3965";
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private String apiId;
    APIRequest apiRequest;

    @Factory(dataProvider = "userModeDataProvider")
    public APIMANAGER3965TestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String backendEndPoint = getBackendEndServiceEndPointHttp("jaxrs_basic/services/customers/customerservice");
        apiRequest = new APIRequest(apiName, apiContext,
                                    new URL(backendEndPoint));
    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation")
    public void testAPICreationWithOutCorsConfiguration() throws Exception {
        apiRequest.setProvider(user.getUserName());
        //Create and publish API version 1.0.0
        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);

        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                                 APIMIntegrationConstants.IS_API_EXISTS);
        String apiInvocationUrl = getAPIInvocationURLHttp(apiContext + "/1.0.0/customers/123");

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpUriRequest option = new HttpOptions(apiInvocationUrl);
        option.addHeader("Origin", "http://localhost:9443");
        option.addHeader("Access-Control-Request-Method", "GET");
        HttpResponse serviceResponse = httpclient.execute(option);
        String accessControlAllowOrigin = serviceResponse.getFirstHeader("Access-Control-Allow-Origin").getValue();
        String accessControlAllowHeaders = serviceResponse.getFirstHeader("Access-Control-Allow-Headers").getValue();
        String accessControlAllowMethods = serviceResponse.getFirstHeader("Access-Control-Allow-Methods").getValue();
        assertEquals(serviceResponse.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                     "Response code mismatched when api invocation");
        assertEquals(accessControlAllowOrigin, "*", "Access Control allow origin values get mismatched in option Call");
        assertEquals(accessControlAllowHeaders, "authorization,Access-Control-Allow-Origin,Content-Type,SOAPAction,Authorization",
                     "Access Control allow Headers values get mismatched in option Call");
        assertTrue(accessControlAllowMethods.contains("GET")
                   && !accessControlAllowMethods.contains("POST")
                   && !accessControlAllowMethods.contains("DELETE")
                   && !accessControlAllowMethods.contains("PUT")
                   && !accessControlAllowMethods.contains("PATCH"),
                   "Access Control allow Method values get mismatched in option Call");
    }

    @Test(groups = {
            "wso2.am" }, description = "Sample API creation", dependsOnMethods =
            "testAPICreationWithOutCorsConfiguration")
    public void testAPICreationWithCorsConfiguration() throws Exception {
        JSONObject corsConfiguration = new JSONObject("{\"corsConfigurationEnabled\" : true, " +
                                                      "\"accessControlAllowOrigins\" : [\"https://localhost:9443," +
                                                      "http://localhost:8080\"], " +
                                                      "\"accessControlAllowCredentials\" : true, " +
                                                      "\"accessControlAllowHeaders\" : " +
                                                      "[\"Access-Control-Allow-Origin\", \"authorization\", " +
                                                      "\"Content-Type\", \"SOAPAction\"], " +
                                                      "\"accessControlAllowMethods\" : [\"POST\", " +
                                                      "\"PATCH\", \"GET\", \"DELETE\", \"OPTIONS\", \"PUT\"]}");
        APIDTO apiDto = restAPIPublisher.getAPIByID(apiId, user.getUserDomain());

        List<String> accessControlAllowOrigins = new ArrayList<>();
        accessControlAllowOrigins.add("https://localhost:9443");
        accessControlAllowOrigins.add("http://localhost:8080");

        List<String> accessControlAllowHeadersList = new ArrayList<>();
        accessControlAllowHeadersList.add("Access-Control-Allow-Origin");
        accessControlAllowHeadersList.add("authorization");
        accessControlAllowHeadersList.add("Content-Type");
        accessControlAllowHeadersList.add("SOAPAction");

        List<String> accessControlAllowMethodsList = new ArrayList<>();
        accessControlAllowMethodsList.add("PATCH");
        accessControlAllowMethodsList.add("GET");
        accessControlAllowMethodsList.add("DELETE");
        accessControlAllowMethodsList.add("OPTIONS");
        accessControlAllowMethodsList.add("PUT");

        APICorsConfigurationDTO apiCorsConfigurationDTO = apiDto.getCorsConfiguration();
        apiCorsConfigurationDTO.setCorsConfigurationEnabled(true);
        apiCorsConfigurationDTO.setAccessControlAllowOrigins(accessControlAllowOrigins);
        apiCorsConfigurationDTO.setAccessControlAllowCredentials(true);
        apiCorsConfigurationDTO.setAccessControlAllowHeaders(accessControlAllowHeadersList);
        apiCorsConfigurationDTO.setAccessControlAllowMethods(accessControlAllowMethodsList);
        apiDto.setCorsConfiguration(apiCorsConfigurationDTO);
        restAPIPublisher.updateAPI(apiDto);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                                 APIMIntegrationConstants.IS_API_EXISTS);
        String apiInvocationUrl = getAPIInvocationURLHttp(apiContext + "/1.0.0/customers/123");
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpUriRequest option = new HttpOptions(apiInvocationUrl);
        option.addHeader("Origin", "http://localhost:8080");
        option.addHeader("Access-Control-Request-Method", "GET");
        HttpResponse serviceResponse = httpclient.execute(option);
        String accessControlAllowOrigin = serviceResponse.getFirstHeader("Access-Control-Allow-Origin").getValue();
        String accessControlAllowHeaders = serviceResponse.getFirstHeader("Access-Control-Allow-Headers").getValue();
        String accessControlAllowMethods = serviceResponse.getFirstHeader("Access-Control-Allow-Methods").getValue();
        String accessControlAllowCredentials =
                serviceResponse.getFirstHeader("Access-Control-Allow-Credentials").getValue();

        assertEquals(serviceResponse.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                     "Response code mismatched when api invocation");
        assertEquals(accessControlAllowOrigin, "http://localhost:8080",
                     "Access Control allow origin values get mismatched in option " +
                     "Call");
        assertEquals(accessControlAllowHeaders, "Access-Control-Allow-Origin,authorization,Content-Type,SOAPAction,Authorization",
                     "Access Control allow Headers values get mismatched in option Call");
        assertTrue(accessControlAllowMethods.contains("GET")
                   && !accessControlAllowMethods.contains("POST")
                   && !accessControlAllowMethods.contains("DELETE")
                   && !accessControlAllowMethods.contains("PUT")
                   && !accessControlAllowMethods.contains("PATCH"),
                   "Access Control allow Method values get mismatched in option Call");
        assertEquals(accessControlAllowCredentials, "true",
                     "Access Control allow Credentials values get mismatched in option Call");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] {
                new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN },
        };
    }
}

