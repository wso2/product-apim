/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.sample;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class RefreshTokenTestCase extends AMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private ServerConfigurationManager serverConfigurationManager;
    private String publisherURLHttp;
    private String storeURLHttp;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        super.init();
        /*
          If test run in external distributed deployment you need to copy following resources accordingly.
          configFiles/hostobjecttest/api-manager.xml
          configFiles/tokenTest/log4j.properties
        */
        if (isBuilderEnabled()) {
            publisherURLHttp = getServerURLHttp();
            storeURLHttp = getServerURLHttp();
            serverConfigurationManager = new ServerConfigurationManager(apimContext);
            serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation()
                    + File.separator +
                    "configFiles/tokenTest/" +
                    "api-manager.xml"));
            serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation()
                    + File.separator +
                    "configFiles/tokenTest/" +
                    "log4j.properties"));
            super.init();
        } else {
            publisherURLHttp = getPublisherServerURLHttp();
            storeURLHttp = getStoreServerURLHttp();
        }
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);

    }

    @Test(groups = {"wso2.am"}, description = "Token API Test sample")
    public void testTokenAPITestCase() throws Exception {
        String APIName = "RefreshTokenTestAPI";
        String APIContext = "refreshTokenTestAPI";
        String tags = "youtube, token, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String name = apimContext.getSuperTenant().getContextUser().getUserName();
        String password = apimContext.getSuperTenant().getContextUser().getPassword();
        String APIVersion = "1.0.0";
        apiPublisher.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiPublisher.addAPI(apiRequest);
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(APIName, name, APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatusTo(updateRequest);

        apiStore.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        apiStore.addApplication("RefreshTokenTestAPI-Application", "Gold", "", "this-is-test");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName,
                apimContext.getContextTenant()
                        .getContextUser()
                        .getUserName());
        subscriptionRequest.setTier("Gold");
        subscriptionRequest.setApplicationName("RefreshTokenTestAPI-Application");
        apiStore.subscribe(subscriptionRequest);

        //Generate production token and invoke with that
        GenerateAppKeyRequest generateAppKeyRequest =
                new GenerateAppKeyRequest("RefreshTokenTestAPI-Application");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);

        // get Consumer Key and Consumer Secret
        String consumerKey =
                response.getJSONObject("data").getJSONObject("key").getString("consumerKey");
        String consumerSecret =
                response.getJSONObject("data").getJSONObject("key").getString("consumerSecret");

        //Obtain user access token
        Thread.sleep(2000);
        String requestBody = "grant_type=password&username=" + name + "&password=" + password + "&scope=PRODUCTION";
        URL tokenEndpointURL = new URL(getGatewayServerURLHttps()+"token");
        JSONObject accessTokenGenerationResponse = new JSONObject(
                apiStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody,
                        tokenEndpointURL).getData());
        /*
        Response would be like -
        {"token_type":"bearer","expires_in":3600,"refresh_token":"736b6b5354e4cf24f217718b2f3f72b",
        "access_token":"e06f12e3d6b1367d8471b093162f6729"}
        */

        // get Access Token and Refresh Token
        String userAccessToken = accessTokenGenerationResponse.getString("access_token");
        String refreshToken = accessTokenGenerationResponse.getString("refresh_token");

        Map<String, String> requestHeaders = new HashMap<String, String>();
        //Check Access Token
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);
        Thread.sleep(2000);
        HttpResponse youTubeResponse = HttpRequestUtil
                .doGet(getGatewayServerURLHttp()+"refreshTokenTestAPI/1.0.0/most_popular", requestHeaders);
        //check JWT headers here
        Assert.assertEquals(youTubeResponse.getResponseCode(), 200, "Response code mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<feed"), "Response data mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<category"),
                "Response data mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<entry>"),
                "Response data mismatched");

        // get a new access token using refresh token
        String getAccessTokenFromRefreshTokenRequestBody =
                "grant_type=refresh_token&refresh_token=" + refreshToken + "&scope=PRODUCTION";
        accessTokenGenerationResponse = new JSONObject(
                apiStore.generateUserAccessKey(consumerKey, consumerSecret,
                        getAccessTokenFromRefreshTokenRequestBody,
                        tokenEndpointURL).getData());
        userAccessToken = accessTokenGenerationResponse.getString("access_token");

        requestHeaders = new HashMap<String, String>();
        //Check with new Access Token
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);
        Thread.sleep(2000);
        youTubeResponse = HttpRequestUtil
                .doGet(getGatewayServerURLHttp()+"refreshTokenTestAPI/1.0.0/most_popular", requestHeaders);
        //check JWT headers here
        Assert.assertEquals(youTubeResponse.getResponseCode(), 200, "Response code mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<feed"), "Response data mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<category"),
                "Response data mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<entry>"),
                "Response data mismatched");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication("RefreshTokenTestAPI-Application");
        super.cleanup();
        serverConfigurationManager.restoreToLastConfiguration();
    }
}