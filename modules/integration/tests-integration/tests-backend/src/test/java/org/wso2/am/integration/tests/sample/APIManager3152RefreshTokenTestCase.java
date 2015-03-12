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

package org.wso2.am.integration.tests.sample;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.publisher.utils.APIPublisherRestClient;
import org.wso2.carbon.integration.common.admin.client.TenantManagementServiceClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.net.URL;

import static org.testng.Assert.assertNotNull;

/*
 In this test case, It will check refresh token is present in the token response with grant type as password in
 tenant mode.
 */
public class APIManager3152RefreshTokenTestCase extends AMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private ServerConfigurationManager serverConfigurationManager;
    private String userName;

    @BeforeClass(alwaysRun = true)
    public void deployService() throws Exception {

        super.init();

        /*
         If test run in external distributed deployment you need to copy following resources accordingly.
         configFiles/hostobjecttest/api-manager.xml
         configFiles/tokenTest/log4j.properties
         */

        String publisherURLHttp = getPublisherServerURLHttp();
        String storeURLHttp = getStoreServerURLHttp();

        userName = apimContext.getContextTenant().getTenantAdmin().getUserName();

        serverConfigurationManager = new ServerConfigurationManager(apimContext);
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "tokenTest" + File.separator + "api-manager.xml"));
        serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "tokenTest" + File.separator + "log4j.properties"));
        super.init();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);

        // create a tenant
        TenantManagementServiceClient tenantManagementServiceClient = new TenantManagementServiceClient(
                apimContext.getContextUrls().getBackEndUrl(), getSessionCookie());

        tenantManagementServiceClient.addTenant("11wso2.com",
                apimContext.getContextTenant().getTenantAdmin().getPassword(),
                apimContext.getContextTenant().getTenantAdmin().getUserName(), "demo");
    }

    @Test(groups = "wso2.am", description = "Check whether refresh token issued in tenant mode")
    public void testForRefreshToken() throws Exception {

        String APIName = "TokenTestAPI";
        String APIContext = "tokenTestAPI";
        String tags = "youtube, token, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String APIVersion = "1.0.0";

        apiPublisher.login(userName + "@11wso2.com", userName);

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiPublisher.addAPI(apiRequest);
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, userName + "@11wso2.com",
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatusTo(updateRequest);

        apiStore.login(userName + "@11wso2.com", apimContext.getContextTenant().getTenantAdmin().getPassword());
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName, userName+"-AT-11wso2.com");
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
        String requestBody = "grant_type=password&username=" + userName + "@11wso2.com&password=" +
                apimContext.getContextTenant().getTenantAdmin().getPassword() + "&scope=PRODUCTION";
        URL tokenEndpointURL = new URL(getGatewayServerURLHttps() + "/token");
        JSONObject accessTokenGenerationResponse = new JSONObject(apiStore.generateUserAccessKey(consumerKey,
                consumerSecret,
                requestBody,
                tokenEndpointURL).getData());

        /*
        Response would be like 
        {"token_type":"bearer","expires_in":3600,"refresh_token":"736b6b5354e4cf24f217718b2f3f72b",
        "access_token":"e06f12e3d6b1367d8471b093162f6729"}
        */

        // get Refresh Token

        assertNotNull(accessTokenGenerationResponse.getString("refresh_token"), "Refresh Token Can Not Be Null");

    }

    @AfterClass(alwaysRun = true)
    public void unDeployService() throws Exception {
        super.cleanup();
        serverConfigurationManager.restoreToLastConfiguration();
    }

}
