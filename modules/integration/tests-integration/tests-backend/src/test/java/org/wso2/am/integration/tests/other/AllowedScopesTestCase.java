/*
 *Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.tests.other;

import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIScopeDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ScopeDTO;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.*;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class AllowedScopesTestCase extends APIManagerLifecycleBaseTest {
    private ServerConfigurationManager serverConfigurationManager;
    private String apiEndPointUrl;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String ALLOWED_SCOPES_API = "allowedScopesAPI";
    private String apiId;
    private String applicationId;
    private final String API_END_POINT_METHOD = "/customers/123";

    @Factory(dataProvider = "userModeDataProvider")
    public AllowedScopesTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
        serverConfigurationManager.applyConfiguration(new File(
                getAMResourceLocation() + File.separator + "configFiles" + File.separator + "allowedScopes"
                        + File.separator + "deployment.toml"));

        userManagementClient = new UserManagementClient(
                keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());

        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;

        //Create API
        APIRequest apiRequest = new APIRequest(ALLOWED_SCOPES_API, ALLOWED_SCOPES_API, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setProvider(user.getUserName());

        HttpResponse response = restAPIPublisher.addAPI(apiRequest);
        apiId = response.getData();

        HttpResponse createdApiResponse = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(createdApiResponse.getData(), APIDTO.class);

        // Adding admin role
        List role = new ArrayList();
        role.add("admin");
        //Adding scope1
        ScopeDTO scopeObject = new ScopeDTO();
        scopeObject.setName("scope1");
        scopeObject.setBindings(role);
        APIScopeDTO apiScope1DTO = new APIScopeDTO();
        apiScope1DTO.setScope(scopeObject);
        //Adding scope2
        ScopeDTO scopeObject2 = new ScopeDTO();
        scopeObject2.setName("scope2");
        scopeObject2.setBindings(role);
        APIScopeDTO apiScope2DTO = new APIScopeDTO();
        apiScope2DTO.setScope(scopeObject2);
        // Adding scopes to API
        List apiScopeList = new ArrayList();
        apiScopeList.add(apiScope1DTO);
        apiScopeList.add(apiScope2DTO);
        apidto.setScopes(apiScopeList);

        // Adding Operation with scopes
        List<String> scopes = new ArrayList<>();
        scopes.add("scope1");
        scopes.add("scope2");
        List<APIOperationsDTO> operations = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget("/customers/{id}");
        apiOperationsDTO.setAuthType("Application & Application User");
        apiOperationsDTO.setThrottlingPolicy("Unlimited");
        apiOperationsDTO.setScopes(scopes);
        operations.add(apiOperationsDTO);
        apidto.operations(operations);

        // Update the API
        restAPIPublisher.updateAPI(apidto, apiId);

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(apiId, false);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(description = "Generate access token for white listed scopes and invoke APIs")
    public void testGenerateAccessTokenWithWhiteListedScopes() throws Exception {
        // Create application
        HttpResponse applicationResponse = restAPIStore.createApplication("TestAppScope",
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        // Subscribe to API
        HttpResponse subscribeResponse = subscribeToAPIUsingRest(apiId, applicationId,
                APIMIntegrationConstants.API_TIER.GOLD, restAPIStore);
        assertEquals(subscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful " +
                        " API Name:" + ALLOWED_SCOPES_API + " API Version:" + API_VERSION_1_0_0 +
                        " API Provider Name :" + user.getUserName());

        //Generate Keys
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

        //Get Consumer Key and Consumer Secret
        String consumerKey = applicationKeyDTO.getConsumerKey();
        String consumerSecret = applicationKeyDTO.getConsumerSecret();
        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");

        // Generate token for scope 1
        String requestBodyForScope1 = "grant_type=password&username=" + user.getUserName() + "&password=" + user.getPassword() + "&scope=scope1";
        JSONObject accessTokenGenerationResponseScope1 = new JSONObject(
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBodyForScope1, tokenEndpointURL)
                        .getData());
        // Validate access token
        Assert.assertNotNull(accessTokenGenerationResponseScope1);
        Assert.assertTrue(accessTokenGenerationResponseScope1.getString("scope").contains("scope1"));
        Assert.assertTrue(accessTokenGenerationResponseScope1.getString("expires_in").equals("3600"));
        String accessTokenScope1 = accessTokenGenerationResponseScope1.getString("access_token");

        Map<String, String> requestHeadersScope1 = new HashMap<String, String>();
        requestHeadersScope1.put("Authorization", "Bearer " + accessTokenScope1);
        requestHeadersScope1.put("accept", "text/xml");
        // Invoke API using token of scope 1
        HttpResponse apiResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(ALLOWED_SCOPES_API, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                        requestHeadersScope1);
        assertEquals(apiResponse.getResponseCode(), HttpStatus.SC_OK);

        // Generate token for scope 2
        String requestBodyForScope2 = "grant_type=password&username=" + user.getUserName() + "&password=" + user.getPassword() + "&scope=scope2";
        JSONObject accessTokenGenerationResponseScope2 = new JSONObject(
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBodyForScope2, tokenEndpointURL)
                        .getData());
        // Validate access token
        Assert.assertNotNull(accessTokenGenerationResponseScope2);
        Assert.assertTrue(accessTokenGenerationResponseScope2.getString("scope").contains("scope2"));
        Assert.assertTrue(accessTokenGenerationResponseScope2.getString("expires_in").equals("3600"));
        String accessTokenScope2 = accessTokenGenerationResponseScope2.getString("access_token");

        Map<String, String> requestHeadersScope2 = new HashMap<String, String>();
        requestHeadersScope2.put("Authorization", "Bearer " + accessTokenScope2);
        requestHeadersScope2.put("accept", "text/xml");
        // Invoke API using token of scope 2
        HttpResponse apiResponse2 = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(ALLOWED_SCOPES_API, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                        requestHeadersScope2);
        assertEquals(apiResponse2.getResponseCode(), HttpStatus.SC_OK);

        // Check if scope1 token is valid
        HttpResponse apiResponse3 = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(ALLOWED_SCOPES_API, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                        requestHeadersScope1);
        assertEquals(apiResponse3.getResponseCode(), HttpStatus.SC_OK);

        // Generate token with scope3
        String requestBodyForWithScope3 = "grant_type=password&username=" + user.getUserName() + "&password=" + user.getPassword()+ "&scope=scope3";
        JSONObject accessTokenGenerationResponseWithScope3 = new JSONObject(
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBodyForWithScope3, tokenEndpointURL)
                        .getData());
        // Validate access token
        Assert.assertNotNull(accessTokenGenerationResponseWithScope3);
        Assert.assertTrue(accessTokenGenerationResponseWithScope3.getString("scope").contains("scope3"));
        Assert.assertTrue(accessTokenGenerationResponseWithScope3.getString("expires_in").equals("3600"));
        String accessTokenWithScope3 = accessTokenGenerationResponseWithScope3.getString("access_token");

        Map<String, String> requestHeadersWithScope3 = new HashMap<String, String>();
        requestHeadersWithScope3.put("Authorization", "Bearer " + accessTokenWithScope3);
        requestHeadersWithScope3.put("accept", "text/xml");

        // Check if scope3 token is forbidden
        HttpResponse apiResponse4 = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(ALLOWED_SCOPES_API, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                        requestHeadersWithScope3);
        assertEquals(apiResponse4.getResponseCode(), HttpStatus.SC_FORBIDDEN);

        // Generate token without scope
        String requestBodyForWithoutScope = "grant_type=password&username=" + user.getUserName() + "&password=" + user.getPassword();
        JSONObject accessTokenGenerationResponseWithoutScope = new JSONObject(
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBodyForWithoutScope, tokenEndpointURL)
                        .getData());
        // Validate access token
        Assert.assertNotNull(accessTokenGenerationResponseWithoutScope);
        Assert.assertTrue(accessTokenGenerationResponseWithoutScope.getString("scope").contains("default"));
        Assert.assertTrue(accessTokenGenerationResponseWithoutScope.getString("expires_in").equals("3600"));
        String accessTokenWithoutScope = accessTokenGenerationResponseWithoutScope.getString("access_token");

        Map<String, String> requestHeadersWithoutScope = new HashMap<String, String>();
        requestHeadersWithoutScope.put("Authorization", "Bearer " + accessTokenWithoutScope);
        requestHeadersWithoutScope.put("accept", "text/xml");

        // Check if default scoped token is forbidden
        HttpResponse apiResponse5 = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(ALLOWED_SCOPES_API, API_VERSION_1_0_0) + API_END_POINT_METHOD,
                        requestHeadersWithoutScope);
        assertEquals(apiResponse5.getResponseCode(), HttpStatus.SC_FORBIDDEN);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId);
        serverConfigurationManager.restoreToLastConfiguration();
    }

}
