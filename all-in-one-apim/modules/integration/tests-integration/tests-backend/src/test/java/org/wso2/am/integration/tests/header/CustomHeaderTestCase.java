/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.am.integration.tests.header;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.wso2.am.integration.tests.restapi.RESTAPITestConstants.APPLICATION_JSON_CONTENT;
import static org.wso2.am.integration.tests.restapi.RESTAPITestConstants.AUTHORIZATION_KEY;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomHeaderTestCase extends APIManagerLifecycleBaseTest {

    private ServerConfigurationManager serverConfigurationManager;
    private final String CUSTOM_AUTHORIZATION_HEADER = "Test-Custom-Header";
    private final String DEFAULT_API_KEY_HEADER = "ApiKey";
    private final String CUSTOM_API_KEY_HEADER = "Custom-ApiKey-Header";
    private final String API1_NAME = "CustomAuthHeaderTestAPI1";
    private final String API1_CONTEXT = "customAuthHeaderTest1";
    private final String API1_VERSION = "1.0.0";
    private final String API_END_POINT_METHOD = "customers/123";
    private String accessToken;
    private String applicationId;
    private String apiId;
    String invocationUrl;

    @Factory(dataProvider = "userModeDataProvider")
    public CustomHeaderTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
        serverConfigurationManager.applyConfiguration(new File(
                getAMResourceLocation() + File.separator + "configFiles" + File.separator + "customHeaderTest"
                        + File.separator + "deployment.toml"));
        //Create application
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse applicationResponse =
                restAPIStore.createApplication(APPLICATION_NAME,
                        APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN,
                        APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                        ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        // Create API
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        APIRequest apiRequest = new APIRequest(API1_NAME, API1_CONTEXT, new URL(url), new URL(url));
        apiRequest.setVersion(API1_VERSION);
        apiRequest.setProvider(user.getUserName());
        apiRequest.setTiersCollection(TIER_UNLIMITED);
        List<String> securitySchemes = new ArrayList<>();
        securitySchemes.add("oauth2");
        securitySchemes.add("api_key");
        apiRequest.setSecurityScheme(securitySchemes);
        invocationUrl = getAPIInvocationURLHttps(API1_CONTEXT, API1_VERSION) + "/" + API_END_POINT_METHOD;
        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        waitForAPIDeploymentSync(user.getUserName(), API1_NAME, API1_VERSION, APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(groups = {"wso2.am"}, description = "Set a customer Auth header for all APIs in the system. (Test ID: 3.1.1.5, 3.1.1.14)")
    public void testSystemWideCustomAuthHeader() throws Exception {

        //get access token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();

        // Test whether a request made with a valid token using the relevant custom auth header should yield the proper
        // response from the back-end, assuming the application has a valid subscription to the API. (Test ID: 3.1.1.5)
        Map<String, String> requestHeaders1 = new HashMap<>();
        requestHeaders1.put("accept", "application/json");
        requestHeaders1.put(CUSTOM_AUTHORIZATION_HEADER, "Bearer " + accessToken);
        HttpResponse apiResponse1 = HttpRequestUtil.doGet(invocationUrl, requestHeaders1);
        assertEquals(apiResponse1.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");

        //Test whether the 401 Unauthorized Response will be returned when the default Auth header "Authorization"
        //is used to invoke the API when the system wide custom Authorization header is configured (Test ID :3.1.1.14))
        Map<String, String> requestHeaders2 = new HashMap<>();
        requestHeaders2.put("accept", APPLICATION_JSON_CONTENT);
        requestHeaders2.put(AUTHORIZATION_KEY, "Bearer " + accessToken);
        HttpResponse apiResponse2 = HttpRequestUtil.doGet(invocationUrl, requestHeaders2);
        assertEquals(apiResponse2.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched");
    }

    @Test(groups = {"wso2.am"}, description = "Invoke an API with default API Key header",
            dependsOnMethods = "testSystemWideCustomAuthHeader")
    public void testInvokeAPIWIthDefaultApiKeyHeader() throws Exception {

        // Genarate API Keys for the application
        APIKeyDTO apiKeyDTO = restAPIStore
                .generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(),
                        -1, null, null);
        assertNotNull(apiKeyDTO, "API Key generation failed");
        String apiKey = apiKeyDTO.getApikey();

        // Test whether a request can be made with the default API Key header
        Map<String, String> requestHeaders1 = new HashMap<>();
        requestHeaders1.put("accept", APPLICATION_JSON_CONTENT);
        requestHeaders1.put(DEFAULT_API_KEY_HEADER, apiKey);
        HttpResponse apiResponse1 = HttpRequestUtil.doGet(invocationUrl, requestHeaders1);
        assertEquals(apiResponse1.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");

        // Test whether the 401 Unauthorized Response is returned with incorrect API Key header
        Map<String, String> requestHeaders2 = new HashMap<>();
        requestHeaders2.put("accept", APPLICATION_JSON_CONTENT);
        requestHeaders2.put(CUSTOM_API_KEY_HEADER, apiKey);
        HttpResponse apiResponse2 = HttpRequestUtil.doGet(invocationUrl, requestHeaders2);
        assertEquals(apiResponse2.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched");
    }

    @Test(groups = {"wso2.am"}, description = "Invoke an API with custom API Key header",
            dependsOnMethods = "testInvokeAPIWIthDefaultApiKeyHeader")
    public void testInvokeAPIWIthCustomApiKeyHeader() throws Exception {

        // Genarate API Keys for the application
        APIKeyDTO apiKeyDTO = restAPIStore
                .generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(),
                        -1, null, null);
        assertNotNull(apiKeyDTO, "API Key generation failed");
        String apiKey = apiKeyDTO.getApikey();

        // Update the API with custom API Key header
        APIDTO apidto = restAPIPublisher.getAPIByID(apiId);
        apidto.setApiKeyHeader(CUSTOM_API_KEY_HEADER);
        restAPIPublisher.updateAPI(apidto);
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        Thread.sleep(10000);

        // Test whether a request can be made with the custom API Key header
        Map<String, String> requestHeaders1 = new HashMap<>();
        requestHeaders1.put("accept", APPLICATION_JSON_CONTENT);
        requestHeaders1.put(CUSTOM_API_KEY_HEADER, apiKey);
        HttpResponse apiResponse1 = HttpRequestUtil.doGet(invocationUrl, requestHeaders1);
        assertEquals(apiResponse1.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");

        // Test whether the 401 Unauthorized Response is returned with default API Key header
        Map<String, String> requestHeaders2 = new HashMap<>();
        requestHeaders2.put("accept", APPLICATION_JSON_CONTENT);
        requestHeaders2.put(DEFAULT_API_KEY_HEADER, apiKey);
        HttpResponse apiResponse2 = HttpRequestUtil.doGet(invocationUrl, requestHeaders2);
        assertEquals(apiResponse2.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        SubscriptionListDTO subsDTO = restAPIStore.getAllSubscriptionsOfApplication(applicationId);
        for (SubscriptionDTO subscriptionDTO: subsDTO.getList()){
            restAPIStore.removeSubscription(subscriptionDTO);
        }
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            serverConfigurationManager.restoreToLastConfiguration();
        }
    }
}
