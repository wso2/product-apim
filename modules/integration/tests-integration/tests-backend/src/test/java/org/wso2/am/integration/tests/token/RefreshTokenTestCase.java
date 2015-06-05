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
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.common.FileManager;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RefreshTokenTestCase extends APIMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private ServerConfigurationManager serverConfigurationManager;

    @Factory(dataProvider = "userModeDataProvider")
    public RefreshTokenTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        /*
          If test run in external distributed deployment you need to copy following resources accordingly.
          configFiles/hostobjecttest/api-manager.xml
          configFiles/tokenTest/log4j.properties
        */
        String sourcePath = TestConfigurationProvider.getResourceLocation() + File.separator +
                            "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" +
                            File.separator + "jaxrs_basic.war";

        String targetPath = FrameworkPathUtil.getCarbonHome() + File.separator + "repository" + File.separator +
                            "deployment" + File.separator + "server" + File.separator + "webapps";

        serverConfigurationManager = new ServerConfigurationManager(gatewayContext);
        FileManager.copyResourceToFileSystem(sourcePath, targetPath, "jaxrs_basic.war");

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();

        serverConfigurationManager = new ServerConfigurationManager(
                new AutomationContext("APIM", "gateway", TestUserMode.SUPER_TENANT_ADMIN));
        serverConfigurationManager.applyConfigurationWithoutRestart(
                new File(getAMResourceLocation() + File.separator + "configFiles" + File.separator +
                         "tokenTest" + File.separator + "api-manager.xml"));
        serverConfigurationManager.applyConfiguration(
                new File(getAMResourceLocation() + File.separator + "configFiles" + File.separator +
                         "tokenTest" + File.separator + "log4j.properties"));

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);

    }

    @Test(groups = {"wso2.am"}, description = "Token API Test other")
    public void testTokenAPITestCase() throws Exception {

        String APIName = "RefreshTokenTestAPI";
        String APIContext = "refreshTokenTestAPI";
        String tags = "sample, token, media";
        String url = gatewayUrls.getWebAppURLHttp() + "jaxrs_basic/services/customers/customerservice";
        String description = "This is test API create by API manager integration test";
        String name = publisherContext.getSuperTenant().getContextUser().getUserName();
        String password = publisherContext.getSuperTenant().getContextUser().getPassword();
        String APIVersion = "1.0.0";
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                           publisherContext.getContextTenant().getContextUser().getPassword());
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiPublisher.addAPI(apiRequest);
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(APIName, name, APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                       storeContext.getContextTenant().getContextUser().getPassword());
        apiStore.addApplication("RefreshTokenTestAPI-Application", "Gold", "", "this-is-test");

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName, storeContext.getContextTenant()
                .getContextUser()
                .getUserName());

        subscriptionRequest.setTier("Gold");
        subscriptionRequest.setApplicationName("RefreshTokenTestAPI-Application");
        apiStore.subscribe(subscriptionRequest);


        // get Consumer Key and Consumer Secret//Generate production token and invoke with that
        APPKeyRequestGenerator generateAppKeyRequest =
                new APPKeyRequestGenerator("RefreshTokenTestAPI-Application");
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
        URL tokenEndpointURL = new URL(gatewayUrls.getWebAppURLNhttp() + "token");
        JSONObject accessTokenGenerationResponse = new JSONObject(
                apiStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody,
                                               tokenEndpointURL).getData());

        // get Access Token and Refresh Token
        String userAccessToken = accessTokenGenerationResponse.getString("access_token");
        String refreshToken = accessTokenGenerationResponse.getString("refresh_token");

        Map<String, String> requestHeaders = new HashMap<String, String>();
        //Check Access Token
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);
        requestHeaders.put("accept", "text/xml");
        Thread.sleep(2000);

        String apiUrl;
        if (gatewayContext.getContextTenant().getDomain().equals("carbon.super")) {
            apiUrl = gatewayUrls.getWebAppURLNhttp() + "refreshTokenTestAPI/1.0.0/customers/123";
        } else {
            apiUrl = gatewayUrls.getWebAppURLNhttp() + "t/" + gatewayContext.getContextTenant().getDomain() +
                     "/refreshTokenTestAPI/1.0.0/customers/123";
        }

        HttpResponse httpResponse = HttpRequestUtil.doGet(apiUrl, requestHeaders);
        //check JWT headers here
        assertEquals(httpResponse.getResponseCode(), Response.Status.OK.getStatusCode(), "Response code mismatched");
        assertTrue(httpResponse.getData().contains("John"), "Response data mismatched");
        assertTrue(httpResponse.getData().contains("<name>"),
                   "Response data mismatched");
        assertTrue(httpResponse.getData().contains("<Customer>"),
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
        requestHeaders.put("accept", "text/xml");
        Thread.sleep(2000);
        httpResponse = HttpRequestUtil.doGet(apiUrl, requestHeaders);
        //check JWT headers here
        assertEquals(httpResponse.getResponseCode(), Response.Status.OK.getStatusCode(), "Response code mismatched");
        assertTrue(httpResponse.getData().contains("John"), "Response data mismatched");
        assertTrue(httpResponse.getData().contains("<name"),
                   "Response data mismatched");
        assertTrue(httpResponse.getData().contains("<Customer>"),
                   "Response data mismatched");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication("RefreshTokenTestAPI-Application");
        super.cleanUp(gatewayContext.getContextTenant().getTenantAdmin().getUserName(),
                      gatewayContext.getContextTenant().getContextUser().getPassword(),
                      storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());
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