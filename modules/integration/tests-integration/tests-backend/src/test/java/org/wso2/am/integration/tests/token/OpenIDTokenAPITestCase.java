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
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.ContextXpathConstants;
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
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL})
public class OpenIDTokenAPITestCase extends APIMIntegrationBaseTest {
    private APIStoreRestClient apiStore;
    private String consumerKey;
    private String consumerSecret;
    private String userAccessToken;

    @Factory(dataProvider = "userModeDataProvider")
    public OpenIDTokenAPITestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(user.getUserName(), user.getPassword());
        apiStore.addApplication("OpenIDTokenTestAPIApplication", "Gold", "", "this-is-test");

        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator("OpenIDTokenTestAPIApplication");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        consumerKey = response.getJSONObject("data").getJSONObject("key").getString("consumerKey");
        consumerSecret = response.getJSONObject("data").getJSONObject("key").getString("consumerSecret");
    }

    @Test(groups = {"wso2.am"}, description = "Token API Test sample")
    public void testGenerateAccessTokenWithOpenIdScope() throws Exception {
        String requestBody = "grant_type=password&username=" + user.getUserName() + "&password="
                             + user.getPassword() + "&scope=openid";
        URL tokenEndpointURL = new URL(gatewayUrlsWrk.getWebAppURLNhttps() + "token");
        JSONObject accessTokenGenerationResponse = new JSONObject(apiStore.generateUserAccessKey(consumerKey,
            consumerSecret, requestBody, tokenEndpointURL).getData());

        userAccessToken = accessTokenGenerationResponse.getString("access_token");
        String scope = accessTokenGenerationResponse.getString("scope");
        Assert.assertTrue(scope.contains("openid"), "Response data mismatched, openid scope test failed due to " +
                                                    "error in response");
    }

    @Test(groups = {"wso2.am"}, description = "Token API Test sample",
            dependsOnMethods = "testGenerateAccessTokenWithOpenIdScope")
    public void testCallUserInfoApiWithOpenIdAccessToken() throws Exception {
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);

        HttpResponse userInfoResponse = HttpRequestUtil.doGet(gatewayUrlsMgt.getWebAppURLNhttp()
                                                             + "oauth2/userinfo?schema=openid", requestHeaders);
        Assert.assertEquals(userInfoResponse.getResponseCode(), 200, "Response code mismatched");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }
}
