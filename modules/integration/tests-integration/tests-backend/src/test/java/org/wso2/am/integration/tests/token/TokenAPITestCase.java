/*
 *Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.token;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class TokenAPITestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(TokenAPITestCase.class);
    private String apiId;
    private String apiIdForOauth;
    private String tokenTestApiAppId;
    private String oauthTokenTestApiId;

    @Factory(dataProvider = "userModeDataProvider")
    public TokenAPITestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[] { TestUserMode.SUPER_TENANT_USER_STORE_USER },
                new Object[] { TestUserMode.SUPER_TENANT_EMAIL_USER },
                new Object[] { TestUserMode.TENANT_EMAIL_USER },
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Test(groups = {"wso2.am"}, description = "Token API Test other")
    public void testTokenAPITestCase() throws Exception {

        String APIName = "TokenTestAPI";
        String APIContext = "tokenTestAPI";
        String tags = "youtube, token, media";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        String description = "This is test API create by API manager integration test";
        String providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        String APIVersion = "1.0.0";

        //APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url), new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiRequest.setProvider(user.getUserName());
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatus(apiId, Constants.PUBLISHED);

        String gatewayUrl = getAPIInvocationURLHttp("tokenTestAPI/1.0.0/customers/123");
        // Create application
        ApplicationDTO applicationDTO = restAPIStore.addApplication("TokenTestAPI-Application",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");
        tokenTestApiAppId = applicationDTO.getApplicationId();

        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(),
                APIMIntegrationConstants.API_TIER.GOLD);
        Assert.assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals("Gold"));

        //Generate sandbox Token and invoke with that
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "3600",
                null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, null, grantTypes);
        String sandboxAccessToken = applicationKeyDTO.getToken().getAccessToken();
        Map<String, String> requestHeadersSandBox = new HashMap<String, String>();
        requestHeadersSandBox.put("Authorization", "Bearer " + sandboxAccessToken);
        requestHeadersSandBox.put("accept", "text/xml");
        HttpResponse youTubeResponseSandBox = HttpRequestUtil.doGet(gatewayUrl, requestHeadersSandBox);
        log.info("Response " + youTubeResponseSandBox);
        assertEquals(youTubeResponseSandBox.getResponseCode(), 200, "Response code mismatched");

        ApplicationKeyDTO productionApplicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(),
                "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = productionApplicationKeyDTO.getToken().getAccessToken();
        String consumerKey = productionApplicationKeyDTO.getConsumerKey();
        String consumerSecret = productionApplicationKeyDTO.getConsumerSecret();
        //Obtain user access token
        Thread.sleep(2000);
        String requestBody = "grant_type=password&username=" + user.getUserName() + "&password=" +
                user.getPassword() + "&scope=PRODUCTION";
        URL tokenEndpointURL = new URL(getGatewayURLNhttp() + "token");
        HttpResponse httpAccessTokenGenerationResponse = restAPIStore.generateUserAccessKey(consumerKey,
                consumerSecret, requestBody, tokenEndpointURL);
        JSONObject accessTokenGenerationResponse = new JSONObject(httpAccessTokenGenerationResponse.getData());
        String userAccessToken = accessTokenGenerationResponse.getString("access_token");
        Map<String, String> requestHeaders = new HashMap<String, String>();
        //Check User Access Token
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);
        requestHeaders.put("accept", "text/xml");

        Thread.sleep(2000);
        HttpResponse youTubeResponse = HttpRequestUtil.doGet(gatewayUrl, requestHeaders);
        assertEquals(youTubeResponse.getResponseCode(), Response.Status.OK.getStatusCode(), "Response code mismatched");
        assertTrue(youTubeResponse.getData().contains("John"), "Response data mismatched");
        assertTrue(youTubeResponse.getData().contains("<name"), "Response data mismatched");
        assertTrue(youTubeResponse.getData().contains("<Customer>"), "Response data mismatched");

        //Check Application Access Token
        requestHeaders.clear();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        HttpResponse youTubeResponseWithApplicationToken = HttpRequestUtil.doGet(gatewayUrl, requestHeaders);
        assertEquals(youTubeResponseWithApplicationToken.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        assertTrue(youTubeResponseWithApplicationToken.getData().contains("John"), "Response data mismatched");
        assertTrue(youTubeResponseWithApplicationToken.getData().contains("<name>"), "Response data mismatched");
        assertTrue(youTubeResponseWithApplicationToken.getData().contains("<Customer>"), "Response data mismatched");
        //Invoke Https end point
        HttpResponse youTubeResponseWithApplicationTokenHttps = HttpRequestUtil
                .doGet(gatewayUrl, requestHeaders);
        log.info("Response " + youTubeResponseWithApplicationTokenHttps);
        assertEquals(youTubeResponseWithApplicationTokenHttps.getResponseCode(), 200, "Response code mismatched");

        HttpResponse errorResponse = null;
        errorResponse = HttpRequestUtil.doGet(gatewayUrl, requestHeaders);
        log.info("Error response " + errorResponse);

        restAPIPublisher.revokeAccessToken(accessToken, consumerKey, providerName);
        requestHeaders.clear();
        requestHeaders.put("Authorization", "Bearer " + "this-is-incorrect-token");
        requestHeaders.put("accept", "text/xml");
        errorResponse = HttpRequestUtil.doGet(gatewayUrl, requestHeaders);
        assertEquals(errorResponse.getResponseCode(), 401, "Response code mismatched while token API test case");
        //TODO handle this in automation core level
        try {
            errorResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp("tokenTestAPI/1.0.0/most_popular"),
                    requestHeaders);
            log.info("Error Response " + errorResponse);
        } catch (Exception e) {
            //handle error
        }
        assertEquals(errorResponse.getResponseCode(), 401, "Response code mismatched while token API test case");
    }

    @Test(groups = { "wso2.am" }, description = "Oauth Token API Test other", dependsOnMethods = {
            "testTokenAPITestCase" })
    public void testOauthTokenAPITestCase() throws Exception {

        String APIName = "oauthTokenTestAPI";
        String APIContext = "oauthTokenTestAPI";
        String tags = "Oauth, token";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        String description = "This is test API create by API manager integration test";
        String providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        String APIVersion = "1.0.0";

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url), new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiRequest.setProvider(user.getUserName());
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiIdForOauth = serviceResponse.getData();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiIdForOauth, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatus(apiIdForOauth, Constants.PUBLISHED);
        String gatewayUrl = getAPIInvocationURLHttp("oauthTokenTestAPI/1.0.0/customers/123");

        //Time to index the published api in store.
        Thread.sleep(3000);
        ApplicationDTO applicationDTO = restAPIStore.addApplicationWithTokenType("oauthTokenTestAPI-Application",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test", "OAUTH");
        oauthTokenTestApiId = applicationDTO.getApplicationId();

        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiIdForOauth,
                applicationDTO.getApplicationId(), APIMIntegrationConstants.API_TIER.GOLD);
        Assert.assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals("Gold"));
        //Generate sandbox Token and invoke with that
        ArrayList<String> grantTypes = new ArrayList<>();
        //get access token
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(),
                "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, null, grantTypes);

        String sandboxAccessToken = applicationKeyDTO.getToken().getAccessToken();
        Map<String, String> requestHeadersSandBox = new HashMap<String, String>();
        String sandTokenJti = TokenUtils.getJtiOfJwtToken(sandboxAccessToken);
        requestHeadersSandBox.put("Authorization", "Bearer " + sandTokenJti);
        requestHeadersSandBox.put("accept", "text/xml");
        HttpResponse youTubeResponseSandBox = HttpRequestUtil.doGet(gatewayUrl, requestHeadersSandBox);
        log.info("Response " + youTubeResponseSandBox);
        assertEquals(youTubeResponseSandBox.getResponseCode(), 200, "Response code mismatched");

        ApplicationKeyDTO productionApplicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(),
                "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = productionApplicationKeyDTO.getToken().getAccessToken();
        String consumerKey = productionApplicationKeyDTO.getConsumerKey();
        String consumerSecret = productionApplicationKeyDTO.getConsumerSecret();
        //Obtain user access token
        Thread.sleep(2000);
        String requestBody = "grant_type=password&username=" + user.getUserName() + "&password=" + user.getPassword() +
                "&scope=PRODUCTION";
        URL tokenEndpointURL = new URL(getGatewayURLNhttp() + "token");
        HttpResponse httpAccessTokenGenerationResponse = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret,
                requestBody, tokenEndpointURL);
        JSONObject accessTokenGenerationResponse = new JSONObject(httpAccessTokenGenerationResponse.getData());
        String userAccessToken = accessTokenGenerationResponse.getString("access_token");
        Map<String, String> requestHeaders = new HashMap<String, String>();
        //Check User Access Token
        String accessTokenJti = TokenUtils.getJtiOfJwtToken(userAccessToken);
        requestHeaders.put("Authorization", "Bearer " + accessTokenJti);
        requestHeaders.put("accept", "text/xml");
        Thread.sleep(2000);

        HttpResponse youTubeResponse = HttpRequestUtil.doGet(gatewayUrl, requestHeaders);
        assertEquals(youTubeResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        assertTrue(youTubeResponse.getData().contains("John"), "Response data mismatched");
        assertTrue(youTubeResponse.getData().contains("<name"), "Response data mismatched");
        assertTrue(youTubeResponse.getData().contains("<Customer>"), "Response data mismatched");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(tokenTestApiAppId);
        restAPIStore.deleteApplication(oauthTokenTestApiId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        undeployAndDeleteAPIRevisionsUsingRest(apiIdForOauth, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiIdForOauth);
    }
}
