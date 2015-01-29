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

package org.wso2.carbon.am.tests.sample;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.am.tests.APIManagerIntegrationTest;
import org.wso2.carbon.am.tests.util.bean.APILifeCycleState;
import org.wso2.carbon.am.tests.util.APIPublisherRestClient;
import org.wso2.carbon.am.tests.util.APIStoreRestClient;
import org.wso2.carbon.am.tests.util.bean.APILifeCycleStateRequest;
import org.wso2.carbon.am.tests.util.bean.APIRequest;
import org.wso2.carbon.am.tests.util.bean.SubscriptionRequest;
import org.wso2.carbon.am.tests.util.bean.GenerateAppKeyRequest;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.HttpRequestUtil;
import org.wso2.carbon.automation.core.utils.HttpResponse;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class TokenAPITestCase extends APIManagerIntegrationTest {
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private ServerConfigurationManager serverConfigurationManager;
    private String publisherURLHttp;
    private String storeURLHttp;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        super.init(0);
          /*
            If test run in external distributed deployment you need to copy following resources accordingly.
            configFiles/hostobjecttest/api-manager.xml
            configFiles/tokenTest/log4j.properties
            To tests issue mentioned in https://wso2.org/jira/browse/APIMANAGER-2065 please run this test against
            WSo2 Load balancer fronted 2 gateways 2 key manager setup with WSClient mode. Please refer resource api-manager.xml file.
          */
        if (isBuilderEnabled()) {
            publisherURLHttp = getServerURLHttp();
            storeURLHttp = getServerURLHttp();
            serverConfigurationManager = new ServerConfigurationManager(amServer.getBackEndUrl());
            serverConfigurationManager.applyConfiguration(new File(ProductConstant.getResourceLocations(ProductConstant.AM_SERVER_NAME)
                    + File.separator + "configFiles/tokenTest/" + "api-manager.xml"));
            serverConfigurationManager.applyConfiguration(new File(ProductConstant.getResourceLocations(ProductConstant.AM_SERVER_NAME)
                    + File.separator + "configFiles/tokenTest/" + "log4j.properties"));
            super.init(0);
        } else {
            publisherURLHttp = getPublisherServerURLHttp();
            storeURLHttp = getStoreServerURLHttp();
        }
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);

    }

    @Test(groups = {"wso2.am"}, description = "Token API Test sample")
    public void testTokenAPITestCase() throws Exception {
        String APIName = "TokenTestAPI";
        String APIContext = "tokenTestAPI";
        String tags = "youtube, token, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersion = "1.0.0";
        apiPublisher.login(userInfo.getUserName(), userInfo.getPassword());
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiPublisher.addAPI(apiRequest);
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, providerName, APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatusTo(updateRequest);

        apiStore.login(userInfo.getUserName(), userInfo.getPassword());

        // Create application
        apiStore.addApplication("TokenTestAPIApplication", "Gold", "", "this-is-test");

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName, userInfo.getUserName());
        subscriptionRequest.setApplicationName("TokenTestAPIApplication");
        subscriptionRequest.setTier("Gold");
        apiStore.subscribe(subscriptionRequest);

        //Generate sandbox Token and invoke with that
        GenerateAppKeyRequest generateAppKeyRequestSandBox = new GenerateAppKeyRequest("TokenTestAPIApplication");
        generateAppKeyRequestSandBox.setKeyType("SANDBOX");
        String responseStringSandBox = apiStore.generateApplicationKey(generateAppKeyRequestSandBox).getData();
        JSONObject responseSandBOX = new JSONObject(responseStringSandBox);
        String SANDbOXAccessToken = responseSandBOX.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeadersSandBox = new HashMap<String, String>();
        requestHeadersSandBox.put("Authorization", "Bearer " + SANDbOXAccessToken);
        HttpResponse youTubeResponseSandBox = HttpRequestUtil.doGet(getApiInvocationURLHttp("tokenTestAPI/1.0.0/most_popular"), requestHeadersSandBox);
        Assert.assertEquals(youTubeResponseSandBox.getResponseCode(), 202, "Response code mismatched");

        //Generate production token and invoke with that
        GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest("TokenTestAPIApplication");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
         /*Response would be like -
          {"validityTime":"360000","consumerKey":"Ow2cGYBf3xlAPpG3Q51W_3qnoega",
         "accessToken":"qo3oNebQaF16C6qw1a56aZn2nwEa","enableRegenarate":true,"accessallowdomains":"ALL","
         consumerSecret":"ctHfsc1jFR7ovUgZ0oeHK8i9F9oa"}*/

        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        String consumerKey = response.getJSONObject("data").getJSONObject("key").getString("consumerKey");
        String consumerSecret = response.getJSONObject("data").getJSONObject("key").getString("consumerSecret");
        //Obtain user access token
        Thread.sleep(2000);
        String requestBody = "grant_type=password&username=admin&password=admin&scope=PRODUCTION";
        URL tokenEndpointURL = new URL("https://localhost:8243/token");
        JSONObject accessTokenGenerationResponse = new JSONObject(apiStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL).getData());
        /*Response would be like -
        {"token_type":"bearer","expires_in":3600,"refresh_token":"736b6b5354e4cf24f217718b2f3f72b",
        "access_token":"e06f12e3d6b1367d8471b093162f6729"}
         */
        String userAccessToken = accessTokenGenerationResponse.getString("access_token");
        String refreshToken = accessTokenGenerationResponse.getString("refresh_token");
        Map<String, String> requestHeaders = new HashMap<String, String>();
        //Check User Access Token
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);
        Thread.sleep(2000);
        HttpResponse youTubeResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp("tokenTestAPI/1.0.0/most_popular"), requestHeaders);
        //check JWT headers here
        Assert.assertEquals(youTubeResponse.getResponseCode(), 200, "Response code mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<feed"), "Response data mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<category"), "Response data mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<entry>"), "Response data mismatched");

        //Check Application Access Token
        requestHeaders.clear();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse youTubeResponseWithApplicationToken = HttpRequestUtil.doGet(getApiInvocationURLHttp("tokenTestAPI/1.0.0/most_popular"), requestHeaders);
        Assert.assertEquals(youTubeResponseWithApplicationToken.getResponseCode(), 200, "Response code mismatched");
        Assert.assertTrue(youTubeResponseWithApplicationToken.getData().contains("<feed"), "Response data mismatched");
        Assert.assertTrue(youTubeResponseWithApplicationToken.getData().contains("<category"), "Response data mismatched");
        Assert.assertTrue(youTubeResponseWithApplicationToken.getData().contains("<entry>"), "Response data mismatched");

        //Invoke Https end point
        String httpsEndpoint = getApiInvocationURLHttps("tokenTestAPI/1.0.0/most_popular");

        HttpResponse youTubeResponseWithApplicationTokenHttps = HttpRequestUtil.doGet(httpsEndpoint, requestHeaders);
        //Assert.assertEquals(youTubeResponseWithApplicationTokenHttps.getResponseCode(), 202, "Response code mismatched");


        HttpResponse errorResponse = null;
        for (int i = 0; i < 40; i++) {
            errorResponse = HttpRequestUtil.doGet(getApiInvocationURLHttps("tokenTestAPI/1.0.0/most_popular"), requestHeaders);
            errorResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp("tokenTestAPI/1.0.0/most_popular"), requestHeaders);
        }
        Assert.assertEquals(errorResponse.getResponseCode(), 503, "Response code mismatched while token API test case");
        Thread.sleep(60000);
        errorResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp("tokenTestAPI/1.0.0/most_popular"), requestHeaders);

        apiPublisher.revokeAccessToken(accessToken, consumerKey, providerName);
        requestHeaders.clear();
        requestHeaders.put("Authorization", "Bearer " + "this-is-incorrect-token");
        errorResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp("tokenTestAPI/1.0.0/most_popular"), requestHeaders);
        Assert.assertEquals(errorResponse.getResponseCode(), 401, "Response code mismatched while token API test case");
        //TODO handle this in automation core level
        try{
            StringBuilder soapRequest = new StringBuilder("<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            soapRequest.append(" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" ");
            soapRequest.append("xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>");
            soapRequest.append("<GetMyName xmlns=\"http://tempuri.org/\"><name>Sam</name></GetMyName>");
            soapRequest.append("</soap:Body></soap:Envelope>");
            errorResponse = HttpRequestUtil.doPost(new URL(getApiInvocationURLHttp("tokenTestAPI/1.0.0/most_popular")), soapRequest.toString(), requestHeaders);
        }
        catch (Exception e){
            //handle error
        }
        //Assert.assertEquals(errorResponse.getResponseCode(), 401, "Response code mismatched while token API test case");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanup();
        serverConfigurationManager.restoreToLastConfiguration();
    }
}
