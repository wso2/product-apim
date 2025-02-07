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

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ErrorDTO;
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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class TokenAPITestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(TokenAPITestCase.class);
    private String apiId;
    private String appId;
    private String oauthTokenTestAppId;
    private String infiniteTokenTestAppId;
    private String gatewayUrl;
    private String consumerKey;
    private String consumerSecret;

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

        String APIName = "TokenTestAPI";
        String APIContext = "tokenTestAPI";
        String tags = "youtube, token, media";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        String description = "This is test API create by API manager integration test";
        String APIVersion = "1.0.0";

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
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        gatewayUrl = getAPIInvocationURLHttp("tokenTestAPI/1.0.0/customers/123");
    }

    @Test(groups = {"wso2.am"}, description = "Token API Test other")
    public void testTokenAPITestCase() throws Exception {

        // Create application
        ApplicationDTO applicationDTO = restAPIStore.addApplication("TokenTestAPI-Application",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");
        appId = applicationDTO.getApplicationId();

        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, appId,
                APIMIntegrationConstants.API_TIER.GOLD);
        Assert.assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals("Gold"));

        //Generate sandbox Token and invoke with that
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(appId, "3600",
                null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, null, grantTypes);
        String sandboxAccessToken = applicationKeyDTO.getToken().getAccessToken();
        Map<String, String> requestHeadersSandBox = new HashMap<String, String>();
        requestHeadersSandBox.put("Authorization", "Bearer " + sandboxAccessToken);
        requestHeadersSandBox.put("accept", "text/xml");
        HttpResponse youTubeResponseSandBox = HttpRequestUtil.doGet(gatewayUrl, requestHeadersSandBox);
        log.info("Response " + youTubeResponseSandBox);
        assertEquals(youTubeResponseSandBox.getResponseCode(), 200, "Response code mismatched");

        ApplicationKeyDTO productionApplicationKeyDTO = restAPIStore.generateKeys(appId,
                "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = productionApplicationKeyDTO.getToken().getAccessToken();
        consumerKey = productionApplicationKeyDTO.getConsumerKey();
        consumerSecret = productionApplicationKeyDTO.getConsumerSecret();
        //Obtain user access token
        Thread.sleep(2000);
        String requestBody = "grant_type=password&username=" + user.getUserName() + "&password=" +
                user.getPassword() + "&scope=PRODUCTION";
        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
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

    @Test(groups = {"wso2.am"}, description = "Test Refresh token functionality", dependsOnMethods = {
            "testTokenAPITestCase" })
    public void testRefreshTokenAPITestCase() throws Exception {

        //Obtain user access token
        String requestBody = "grant_type=password&username=" + user.getUserName() + "&password=" + user.getPassword()
                + "&scope=PRODUCTION";
        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");

        JSONObject accessTokenGenerationResponse = new JSONObject(
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL)
                        .getData());

        // get Access Token and Refresh Token
        String userAccessToken = accessTokenGenerationResponse.getString("access_token");
        String refreshToken = accessTokenGenerationResponse.getString("refresh_token");

        Map<String, String> requestHeaders = new HashMap<String, String>();
        //Check Access Token
        String tokenJti = TokenUtils.getJtiOfJwtToken(userAccessToken);
        requestHeaders.put("Authorization", "Bearer " + tokenJti);
        requestHeaders.put("accept", "text/xml");

        HttpResponse httpResponse = HttpRequestUtil.doGet(gatewayUrl, requestHeaders);

        assertEquals(httpResponse.getResponseCode(), Response.Status.OK.getStatusCode(), "Response code mismatched");
        assertTrue(httpResponse.getData().contains("John"), "Response data mismatched");
        assertTrue(httpResponse.getData().contains("<name>"), "Response data mismatched");
        assertTrue(httpResponse.getData().contains("<Customer>"), "Response data mismatched");

        //Get a new access token using refresh token
        String getAccessTokenFromRefreshTokenRequestBody = "grant_type=refresh_token&refresh_token=" + refreshToken;
        accessTokenGenerationResponse = new JSONObject(restAPIStore
                .generateUserAccessKey(consumerKey, consumerSecret, getAccessTokenFromRefreshTokenRequestBody,
                        tokenEndpointURL).getData());
        userAccessToken = accessTokenGenerationResponse.getString("access_token");

        requestHeaders = new HashMap<String, String>();
        //Check with new Access Token
        tokenJti = TokenUtils.getJtiOfJwtToken(userAccessToken);
        requestHeaders.put("Authorization", "Bearer " + tokenJti);
        requestHeaders.put("accept", "text/xml");
        httpResponse = HttpRequestUtil.doGet(gatewayUrl, requestHeaders);

        assertEquals(httpResponse.getResponseCode(), Response.Status.OK.getStatusCode(), "Response code mismatched");
        assertTrue(httpResponse.getData().contains("John"), "Response data mismatched");
        assertTrue(httpResponse.getData().contains("<name"), "Response data mismatched");
        assertTrue(httpResponse.getData().contains("<Customer>"), "Response data mismatched");
    }

    @Test(groups = { "wso2.am" }, description = "Oauth Token API Test other", dependsOnMethods = {
            "testRefreshTokenAPITestCase" })
    public void testOauthTokenAPITestCase() throws Exception {

        ApplicationDTO applicationDTO = restAPIStore.addApplicationWithTokenType("oauthTokenTestAPI-Application",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test", "OAUTH");
        oauthTokenTestAppId = applicationDTO.getApplicationId();

        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId,
                oauthTokenTestAppId, APIMIntegrationConstants.API_TIER.GOLD);
        Assert.assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals("Gold"));
        //Generate sandbox Token and invoke with that
        ArrayList<String> grantTypes = new ArrayList<>();
        //get access token
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(oauthTokenTestAppId,
                "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, null, grantTypes);

        String sandboxAccessToken = applicationKeyDTO.getToken().getAccessToken();
        Map<String, String> requestHeadersSandBox = new HashMap<String, String>();
        String sandTokenJti = TokenUtils.getJtiOfJwtToken(sandboxAccessToken);
        requestHeadersSandBox.put("Authorization", "Bearer " + sandTokenJti);
        requestHeadersSandBox.put("accept", "text/xml");
        HttpResponse youTubeResponseSandBox = HttpRequestUtil.doGet(gatewayUrl, requestHeadersSandBox);
        log.info("Response " + youTubeResponseSandBox);
        assertEquals(youTubeResponseSandBox.getResponseCode(), 200, "Response code mismatched");

        ApplicationKeyDTO productionApplicationKeyDTO = restAPIStore.generateKeys(oauthTokenTestAppId,
                "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String consumerKey = productionApplicationKeyDTO.getConsumerKey();
        String consumerSecret = productionApplicationKeyDTO.getConsumerSecret();
        //Obtain user access token
        Thread.sleep(2000);
        String requestBody = "grant_type=password&username=" + user.getUserName() + "&password=" + user.getPassword() +
                "&scope=PRODUCTION";
        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
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

    @Test(groups = { "wso2.am" }, description = "Infinite Token API Test other", dependsOnMethods = {
            "testOauthTokenAPITestCase" })
    public void testInfiniteTokenAPITestCase() throws Exception {
        ApplicationDTO applicationDTO = restAPIStore.addApplicationWithTokenType("infiniteTokenTestAPI-Application",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "test app for Infinite Token API invocation",
                "JWT");
        infiniteTokenTestAppId = applicationDTO.getApplicationId();

        SubscriptionDTO subscriptionDTO = restAPIStore
                .subscribeToAPI(apiId, infiniteTokenTestAppId, APIMIntegrationConstants.API_TIER.GOLD);
        Assert.assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals("Gold"));

        // Set a negative value for the application token expiry time
        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("application_access_token_expiry_time", "-1");
        // Generate a sandbox token and invoke with that
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        try {
            ApiResponse<ApplicationKeyDTO> applicationKeyDTOApiResponse = restAPIStore
                    .generateKeysWithApiResponse(infiniteTokenTestAppId, "3600", null,
                            ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, null, grantTypes,
                            additionalProperties, null);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 400);
            ErrorDTO errorDTO = new Gson().fromJson(e.getResponseBody(), ErrorDTO.class);
            Assert.assertEquals(errorDTO.getCode().longValue(), 900970);
            Assert.assertEquals(errorDTO.getMessage(), "Invalid application additional properties");
            Assert.assertTrue(
                    errorDTO.getDescription().contains("Application configuration values cannot have negative values"));
        }

        // Set a long for the application token expiry time (to get an infinite token)
        additionalProperties.put("application_access_token_expiry_time", String.valueOf(Long.MAX_VALUE));
        // Generate a sandbox token and invoke with that
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(infiniteTokenTestAppId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, null, grantTypes, additionalProperties, null);
        String sandboxAccessToken = applicationKeyDTO.getToken().getAccessToken();
        Map<String, String> requestHeadersSandBox = new HashMap<>();
        requestHeadersSandBox.put("Authorization", "Bearer " + sandboxAccessToken);
        requestHeadersSandBox.put("accept", "text/xml");
        HttpResponse youTubeResponseSandBox = HttpRequestUtil.doGet(gatewayUrl, requestHeadersSandBox);
        log.info("Response " + youTubeResponseSandBox);
        assertEquals(youTubeResponseSandBox.getResponseCode(), 200, "Response code mismatched");
        // Generate a production token and invoke with that
        applicationKeyDTO = restAPIStore.generateKeys(infiniteTokenTestAppId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes, additionalProperties, null);
        String productionAccessToken = applicationKeyDTO.getToken().getAccessToken();
        Map<String, String> requestHeadersProduction = new HashMap<>();
        requestHeadersProduction.put("Authorization", "Bearer " + productionAccessToken);
        requestHeadersProduction.put("accept", "text/xml");
        HttpResponse youTubeResponseProduction = HttpRequestUtil.doGet(gatewayUrl, requestHeadersProduction);
        log.info("Response " + youTubeResponseProduction);
        assertEquals(youTubeResponseProduction.getResponseCode(), 200, "Response code mismatched");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(appId);
        restAPIStore.deleteApplication(oauthTokenTestAppId);
        restAPIStore.deleteApplication(infiniteTokenTestAppId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }
}
