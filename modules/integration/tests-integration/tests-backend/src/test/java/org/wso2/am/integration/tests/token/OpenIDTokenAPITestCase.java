/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * This test will cover OpenId based access token generation and validation for users
 * Here we will retrieve access tokens with open id scope and use it for user info API
 */
public class OpenIDTokenAPITestCase extends APIMIntegrationBaseTest {
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private ServerConfigurationManager serverConfigurationManager;
    private String publisherURLHttp;
    private String storeURLHttp;

    @Factory(dataProvider = "userModeDataProvider")
    public OpenIDTokenAPITestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

            publisherURLHttp = publisherUrls.getWebAppURLHttp();;
            storeURLHttp = storeUrls.getWebAppURLHttp();
            serverConfigurationManager = new ServerConfigurationManager(new AutomationContext("APIM", "gateway", TestUserMode.SUPER_TENANT_ADMIN));
            serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                    + File.separator +
                    "configFiles/tokenTest/" +
                    "api-manager.xml"));
            serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation()
                    + File.separator +
                    "configFiles/tokenTest/" +
                    "log4j.properties"));

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);

    }

    @Test(groups = {"wso2.am"}, description = "Token API Test sample")
    public void testTokenAPITestCase() throws Exception {
        String APIName = "openIDTokenTestAPI";
        String APIContext = "openIDTokenTestAPI";
        String tags = "youtube, token, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersion = "1.0.0";
        apiPublisher.login(publisherContext.getSuperTenant().getContextUser().getUserName(), 
                           publisherContext.getSuperTenant().getContextUser().getPassword());
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiPublisher.addAPI(apiRequest);
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, providerName, APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        apiStore.login(publisherContext.getSuperTenant().getContextUser().getUserName(), 
                       publisherContext.getSuperTenant().getContextUser().getPassword());

        // Create application
        apiStore.addApplication("OpenIDTokenTestAPIApplication", "Gold", "", "this-is-test");

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName, 
                                                            publisherContext.getSuperTenant().getContextUser().getUserName());
        subscriptionRequest.setApplicationName("OpenIDTokenTestAPIApplication");
        subscriptionRequest.setTier("Gold");
        apiStore.subscribe(subscriptionRequest);

        //Generate sandbox Token and invoke with that
        APPKeyRequestGenerator generateAppKeyRequestSandBox = new APPKeyRequestGenerator("OpenIDTokenTestAPIApplication");
        generateAppKeyRequestSandBox.setKeyType("SANDBOX");
        String responseStringSandBox = apiStore.generateApplicationKey(generateAppKeyRequestSandBox).getData();
        JSONObject responseSandBOX = new JSONObject(responseStringSandBox);
        String SANDbOXAccessToken = responseSandBOX.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeadersSandBox = new HashMap<String, String>();
        requestHeadersSandBox.put("Authorization", "Bearer " + SANDbOXAccessToken);
        HttpResponse youTubeResponseSandBox = HttpRequestUtil.doGet(gatewayUrls.getWebAppURLNhttp() + 
                                                                    "OpenIDTokenTestAPI/1.0.0/most_popular", requestHeadersSandBox);
        //Assert.assertEquals(youTubeResponseSandBox.getResponseCode(), 202, "Response code mismatched");

        //Generate production token and invoke with that
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator("OpenIDTokenTestAPIApplication");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
         /*Response would be like -
          {"validityTime":"360000","consumerKey":"Ow2cGYBf3xlAPpG3Q51W_3qnoega",
         "accessToken":"qo3oNebQaF16C6qw1a56aZn2nwEa","enableRegenarate":true,"accessallowdomains":"ALL","
         consumerSecret":"ctHfsc1jFR7ovUgZ0oeHK8i9F9oa"}*/

        String consumerKey = response.getJSONObject("data").getJSONObject("key").getString("consumerKey");
        String consumerSecret = response.getJSONObject("data").getJSONObject("key").getString("consumerSecret");
        //Obtain user access token
        Thread.sleep(2000);
        String requestBody = "grant_type=password&username=admin&password=admin&scope=openid";
        URL tokenEndpointURL = new URL("https://localhost:8243/token");
        JSONObject accessTokenGenerationResponse = new JSONObject(apiStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL).getData());
        /*Response would be like -
        {"token_type":"bearer","expires_in":3600,"refresh_token":"736b6b5354e4cf24f217718b2f3f72b",
        "access_token":"e06f12e3d6b1367d8471b093162f6729"}
         */

        String userAccessToken = accessTokenGenerationResponse.getString("access_token");
        String scope = accessTokenGenerationResponse.getString("scope");
        Assert.assertTrue(scope.contains("openid"), "Response data mismatched, openid scope test failed due to error in response");
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);
        Thread.sleep(2000);
        HttpResponse youTubeResponse = HttpRequestUtil
                .doGet(gatewayUrls.getWebAppURLHttp() + "oauth2/userinfo?schema=openid", requestHeaders);
        Assert.assertEquals(youTubeResponse.getResponseCode(), 200, "Response code mismatched");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
//        super.cleanUp(gatewayContext.getContextTenant().getTenantAdmin().getUserName(),
//                      gatewayContext.getContextTenant().getContextUser().getPassword(),
//                      storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());
        serverConfigurationManager.restoreToLastConfiguration();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }
}