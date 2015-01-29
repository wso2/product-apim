/*
 *   Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.am.tests.token;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.am.tests.APIManagerIntegrationTest;
import org.wso2.carbon.am.tests.util.APIPublisherRestClient;
import org.wso2.carbon.am.tests.util.APIStoreRestClient;
import org.wso2.carbon.am.tests.util.bean.APILifeCycleState;
import org.wso2.carbon.am.tests.util.bean.APILifeCycleStateRequest;
import org.wso2.carbon.am.tests.util.bean.APIRequest;
import org.wso2.carbon.am.tests.util.bean.GenerateAppKeyRequest;
import org.wso2.carbon.am.tests.util.bean.SubscriptionRequest;
import org.wso2.carbon.automation.api.clients.stratos.tenant.mgt.TenantMgtAdminServiceClient;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;

import java.io.File;
import java.net.URL;

/*
 In this test case, It will check refresh token is present in the token response with grant type as password in tenant mode.
 */
public class APIMANAGER3152RefreshTokenTestCase extends APIManagerIntegrationTest {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private ServerConfigurationManager serverConfigurationManager;
    private String publisherURLHttp;
    private String storeURLHttp;

    @BeforeClass(alwaysRun = true)
    public void deployService() throws Exception {
        super.init(2);

        /*
         If test run in external distributed deployment you need to copy following resources accordingly.
         configFiles/hostobjecttest/api-manager.xml
         configFiles/tokenTest/log4j.properties
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

        // create a tenant
        TenantMgtAdminServiceClient tenantMgtAdminServiceClient =
                new TenantMgtAdminServiceClient(amServer.getBackEndUrl(), amServer.getSessionCookie());

        tenantMgtAdminServiceClient.addTenant("11wso2.com", "admin", "admin", "demo");


    }

    @Test(groups = "wso2.am", description = "Check whether refresh token issued in tenant mode")
    public void testForRefreshToken() throws Exception {
        String APIName = "TokenTestAPI";
        String APIContext = "tokenTestAPI";
        String tags = "youtube, token, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin@11wso2.com";
        String APIVersion = "1.0.0";

        apiPublisher.login("admin@11wso2.com", "admin");

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiPublisher.addAPI(apiRequest);
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, providerName, APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatusTo(updateRequest);

        apiStore.login("admin@11wso2.com", "admin");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName, "admin-AT-11wso2.com");
        subscriptionRequest.setTier("Gold");
        apiStore.subscribe(subscriptionRequest);

        //Generate production token and invoke with that
        GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest("DefaultApplication");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);

        // get Consumer Key and Consumer Secret
        String consumerKey = response.getJSONObject("data").getJSONObject("key").getString("consumerKey");
        String consumerSecret = response.getJSONObject("data").getJSONObject("key").getString("consumerSecret");

        //Obtain user access token
        Thread.sleep(2000);
        String requestBody = "grant_type=password&username=admin@11wso2.com&password=admin&scope=PRODUCTION";
        URL tokenEndpointURL = new URL("https://localhost:8243/token");
        JSONObject accessTokenGenerationResponse = new JSONObject(apiStore.generateUserAccessKey(consumerKey,
                                                                                                 consumerSecret,
                                                                                                 requestBody,
                                                                                                 tokenEndpointURL).getData());

        /*
        Response would be like -
        {"token_type":"bearer","expires_in":3600,"refresh_token":"736b6b5354e4cf24f217718b2f3f72b",
        "access_token":"e06f12e3d6b1367d8471b093162f6729"}
        */

        // get Refresh Token
        String refreshToken = accessTokenGenerationResponse.getString("refresh_token");

        Assert.assertNotNull(refreshToken);

    }

    @AfterClass(alwaysRun = true)
    public void unDeployService() throws Exception {
        super.cleanup();
        serverConfigurationManager.restoreToLastConfiguration();
    }

}

