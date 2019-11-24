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

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class RefreshTokenTestCase extends APIMIntegrationBaseTest {

    private ServerConfigurationManager serverConfigurationManager;
    private static String backEndEndpointUrl;
    private String apiId;
    private String tokenTestApiAppId;
    private static final String APPLICATION_NAME = "RefreshTokenTestAPI-Application";

    @Factory(dataProvider = "userModeDataProvider")
    public RefreshTokenTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {


        super.init(userMode);

        /*
          If test run in external distributed deployment you need to copy following resources accordingly.
          configFiles/hostobjecttest/api-manager.xml
          configFiles/tokenTest/log4j.properties
        */

//            serverConfigurationManager.applyConfiguration(
//                    new File(getAMResourceLocation() + File.separator + "configFiles" + File.separator +
//                             "tokenTest" + File.separator + "api-manager.xml"));
//            serverConfigurationManager.applyConfiguration(
//                    new File(getAMResourceLocation() + File.separator + "configFiles" + File.separator +
//                             "tokenTest" + File.separator + "log4j2.properties"));

        backEndEndpointUrl = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";

        serverConfigurationManager = new ServerConfigurationManager(
                new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                      APIMIntegrationConstants.AM_GATEWAY_WRK_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN));
    }

    @Test(groups = {"wso2.am"}, description = "Test Refresh token functionality")
    public void testRefreshTokenAPITestCase() throws Exception {

        String apiName = "RefreshTokenTestAPI";
        String apiContext = "refreshTokenTestAPI";
        String tags = "sample, token, media";
        String description = "This is test API create by API manager integration test";
        String apiVersion = "1.0.0";
        //Create API.
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(backEndEndpointUrl));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(apiVersion);
        apiRequest.setSandbox(backEndEndpointUrl);
        apiRequest.setProvider(user.getUserName());
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        //Publish API.
        apiId = serviceResponse.getData();
        restAPIPublisher.changeAPILifeCycleStatus(apiId, Constants.PUBLISHED);
        String gatewayUrl = getAPIInvocationURLHttp("tokenTestAPI/1.0.0/customers/123");
        // Add application
        ApplicationDTO applicationDTO = restAPIStore.addApplicationWithTokenType(APPLICATION_NAME,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test",
                "OAUTH");
        tokenTestApiAppId = applicationDTO.getApplicationId();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(),
                APIMIntegrationConstants.API_TIER.GOLD);
        assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals(APIMIntegrationConstants.API_TIER.GOLD));

        //Get Consumer Key and Consumer Secret//Generate production token and invoke with that
        ArrayList<String> grantTypes = new ArrayList<>();
        //get access token
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO productionApplicationKeyDTO = restAPIStore.generateKeys(tokenTestApiAppId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

        String consumerKey = productionApplicationKeyDTO.getConsumerKey();
        String consumerSecret = productionApplicationKeyDTO.getConsumerSecret();
        //Obtain user access token
        String requestBody = "grant_type=password&username=" + user.getUserName() + "&password=" + user.getPassword() +
                "&scope=PRODUCTION";
        URL tokenEndpointURL = new URL(getGatewayURLNhttp() + "token");

        HttpResponse firstResponse = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody,
                tokenEndpointURL);
        JSONObject firstAccessTokenGenerationResponse = new JSONObject(firstResponse.getData());
        //get an access token for the first time
        String firstAccessToken = firstAccessTokenGenerationResponse.getString("access_token");

        HttpResponse secondResponse = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody,
                tokenEndpointURL);
        JSONObject secondAccessTokenGenerationResponse = new JSONObject(secondResponse.getData());
        //get an access token for the second time (using the same consumerKey, consumerSecret)
        String secondAccessToken = secondAccessTokenGenerationResponse.getString("access_token");
        String secondrefreshToken = secondAccessTokenGenerationResponse.getString("refresh_token");
        //compare the two tokens, those should be equal
        assertEquals(firstAccessToken, secondAccessToken, "Token mismatch while generating access token twice.");

        //get an access token for the third time (using refresh grant type)
        String requestBodyForRefreshGrant = "grant_type=refresh_token&refresh_token=" + secondrefreshToken;
        HttpResponse thirdResponse = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret,
                requestBodyForRefreshGrant, tokenEndpointURL);
        JSONObject thirdAccessTokenGenerationResponse = new JSONObject(thirdResponse.getData());
        String thirdAccessToken = thirdAccessTokenGenerationResponse.getString("access_token");
        //compare the two tokens, those should not be equal
        assertNotEquals(firstAccessToken, thirdAccessToken, "Both tokens can not be the same");
        JSONObject accessTokenGenerationResponse = new JSONObject(restAPIStore.generateUserAccessKey(consumerKey,
                consumerSecret, requestBody, tokenEndpointURL).getData());

        // get Access Token and Refresh Token
        String userAccessToken = accessTokenGenerationResponse.getString("access_token");
        String refreshToken = accessTokenGenerationResponse.getString("refresh_token");

        Map<String, String> requestHeaders = new HashMap<String, String>();
        //Check Access Token
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);
        requestHeaders.put("accept", "text/xml");

        String apiUrl = getAPIInvocationURLHttp("refreshTokenTestAPI/1.0.0/customers/123");

        HttpResponse httpResponse = HttpRequestUtil.doGet(apiUrl, requestHeaders);

        //TODO - Remove the second request below. This is a temporary workaround to avoid the issue caused by a bug in
        // carbon-mediation 4.4.11-SNAPSHOT See the thread "[Dev] [ESB] EmptyStackException when resuming a paused
        // message processor" on dev@wso2.org for information about the bug.
        Thread.sleep(5000);
        httpResponse = HttpRequestUtil.doGet(apiUrl, requestHeaders);

        assertEquals(httpResponse.getResponseCode(), Response.Status.OK.getStatusCode(), "Response code mismatched");
        assertTrue(httpResponse.getData().contains("John"), "Response data mismatched");
        assertTrue(httpResponse.getData().contains("<name>"),
                   "Response data mismatched");
        assertTrue(httpResponse.getData().contains("<Customer>"),
                   "Response data mismatched");

        //Get a new access token using refresh token
        String getAccessTokenFromRefreshTokenRequestBody =
                "grant_type=refresh_token&refresh_token=" + refreshToken;
        accessTokenGenerationResponse = new JSONObject(restAPIStore.generateUserAccessKey(consumerKey, consumerSecret,
                getAccessTokenFromRefreshTokenRequestBody, tokenEndpointURL).getData());
        userAccessToken = accessTokenGenerationResponse.getString("access_token");

        requestHeaders = new HashMap<String, String>();
        //Check with new Access Token
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);
        requestHeaders.put("accept", "text/xml");
        httpResponse = HttpRequestUtil.doGet(apiUrl, requestHeaders);

        assertEquals(httpResponse.getResponseCode(), Response.Status.OK.getStatusCode(), "Response code mismatched");
        assertTrue(httpResponse.getData().contains("John"), "Response data mismatched");
        assertTrue(httpResponse.getData().contains("<name"), "Response data mismatched");
        assertTrue(httpResponse.getData().contains("<Customer>"), "Response data mismatched");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(tokenTestApiAppId);
        restAPIPublisher.deleteAPI(apiId);
    }
}
