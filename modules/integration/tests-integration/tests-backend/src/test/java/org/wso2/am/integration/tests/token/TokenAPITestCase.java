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
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class TokenAPITestCase extends APIMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
//    private ServerConfigurationManager serverConfigurationManager;

    private static final Log log = LogFactory.getLog(TokenAPITestCase.class);

    @Factory(dataProvider = "userModeDataProvider")
    public TokenAPITestCase(TestUserMode userMode) {
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
          To tests issue mentioned in https://wso2.org/jira/browse/APIMANAGER-2065 please run this test against
          WSo2 Load balancer fronted 2 gateways 2 key manager setup with WSClient mode. Please refer resource api-manager.xml file.
        */
//        String sourcePath = TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" +
//                            File.separator + "AM" + File.separator + "lifecycletest" + File.separator + "jaxrs_basic.war";
//
//        String targetPath = FrameworkPathUtil.getCarbonHome() + File.separator + "repository" + File.separator +
//                            "deployment" + File.separator + "server" + File.separator + "webapps";
//
//        FileManager.copyResourceToFileSystem(sourcePath, targetPath, "jaxrs_basic.war");

//        serverConfigurationManager = new ServerConfigurationManager(
//                new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
//                                      APIMIntegrationConstants.AM_GATEWAY_WRK_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN));
//
//        serverConfigurationManager.applyConfigurationWithoutRestart(
//                new File(getAMResourceLocation() + File.separator + "configFiles/tokenTest/" + "api-manager.xml"));
//        serverConfigurationManager.applyConfiguration(
//                new File(getAMResourceLocation() + File.separator + "configFiles/tokenTest/" + "log4j.properties"));

        apiPublisher = new APIPublisherRestClient(getPublisherURLHttp());
        apiStore = new APIStoreRestClient(getStoreURLHttp());

        apiPublisher.login(user.getUserName(), user.getPassword());
        apiStore.login(user.getUserName(), user.getPassword());

    }

    @Test(groups = {"wso2.am"}, description = "Token API Test other")
    public void testTokenAPITestCase() throws Exception {

        String APIName = "TokenTestAPI";
        String APIContext = "tokenTestAPI";
        String tags = "youtube, token, media";
        String url = getGatewayURLHttp()+ "jaxrs_basic/services/customers/customerservice";
        String description = "This is test API create by API manager integration test";
        String providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        String APIVersion = "1.0.0";

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiRequest.setProvider(user.getUserName());
        apiPublisher.addAPI(apiRequest);
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(APIName, providerName, APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        String gatewayUrl = getAPIInvocationURLHttp("tokenTestAPI/1.0.0/customers/123");

        // Create application
        apiStore.addApplication("TokenTestAPI-Application", "Gold", "", "this-is-test");

        String provider = storeContext.getContextTenant().getContextUser().getUserName();

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName, provider);
        subscriptionRequest.setTier("Gold");
        subscriptionRequest.setApplicationName("TokenTestAPI-Application");
        apiStore.subscribe(subscriptionRequest);

        //Generate sandbox Token and invoke with that
        APPKeyRequestGenerator generateAppKeyRequestSandBox =
                new APPKeyRequestGenerator("TokenTestAPI-Application");
        generateAppKeyRequestSandBox.setKeyType("SANDBOX");
        String responseStringSandBox =
                apiStore.generateApplicationKey(generateAppKeyRequestSandBox).getData();
        JSONObject responseSandBOX = new JSONObject(responseStringSandBox);
        String SANDbOXAccessToken =
                responseSandBOX.getJSONObject("data").getJSONObject("key").get("accessToken")
                        .toString();
        Map<String, String> requestHeadersSandBox = new HashMap<String, String>();
        requestHeadersSandBox.put("Authorization", "Bearer " + SANDbOXAccessToken);
        HttpResponse youTubeResponseSandBox = HttpRequestUtil
                .doGet(gatewayUrl,
                       requestHeadersSandBox);

        log.info("Response " + youTubeResponseSandBox);
        // assertEquals(youTubeResponseSandBox.getResponseCode(), 200, "Response code mismatched");

        //Generate production token and invoke with that
        APPKeyRequestGenerator generateAppKeyRequest =
                new APPKeyRequestGenerator("TokenTestAPI-Application");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        /*Response would be like -
         {"validityTime":"360000","consumerKey":"Ow2cGYBf3xlAPpG3Q51W_3qnoega",
        "accessToken":"qo3oNebQaF16C6qw1a56aZn2nwEa","enableRegenarate":true,"accessallowdomains":"ALL","
        consumerSecret":"ctHfsc1jFR7ovUgZ0oeHK8i9F9oa"}*/

        String accessToken =
                response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        String consumerKey =
                response.getJSONObject("data").getJSONObject("key").getString("consumerKey");
        String consumerSecret =
                response.getJSONObject("data").getJSONObject("key").getString("consumerSecret");
        //Obtain user access token
        Thread.sleep(2000);
        String requestBody = "grant_type=password&username=admin&password=admin&scope=PRODUCTION";
        URL tokenEndpointURL = new URL(getGatewayURLNhttp() + "token");
        JSONObject accessTokenGenerationResponse = new JSONObject(
                apiStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody,
                                               tokenEndpointURL).getData()
        );
        /*Response would be like -
        {"token_type":"bearer","expires_in":3600,"refresh_token":"736b6b5354e4cf24f217718b2f3f72b",
        "access_token":"e06f12e3d6b1367d8471b093162f6729"}
         */
        String userAccessToken = accessTokenGenerationResponse.getString("access_token");
        String refreshToken = accessTokenGenerationResponse.getString("refresh_token");
        log.info(refreshToken);
        Map<String, String> requestHeaders = new HashMap<String, String>();
        //Check User Access Token
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);
        requestHeaders.put("accept", "text/xml");

        Thread.sleep(2000);
        HttpResponse youTubeResponse = HttpRequestUtil
                .doGet(gatewayUrl, requestHeaders);

        assertEquals(youTubeResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");
        assertTrue(youTubeResponse.getData().contains("John"), "Response data mismatched");
        assertTrue(youTubeResponse.getData().contains("<name"),
                   "Response data mismatched");
        assertTrue(youTubeResponse.getData().contains("<Customer>"),
                   "Response data mismatched");

        //Check Application Access Token
        requestHeaders.clear();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        HttpResponse youTubeResponseWithApplicationToken = HttpRequestUtil
                .doGet(gatewayUrl, requestHeaders);
        assertEquals(youTubeResponseWithApplicationToken.getResponseCode(), Response.Status.OK.getStatusCode(),
                     "Response code mismatched");
        assertTrue(youTubeResponseWithApplicationToken.getData().contains("John"),
                   "Response data mismatched");
        assertTrue(youTubeResponseWithApplicationToken.getData().contains("<name>"),
                   "Response data mismatched");
        assertTrue(youTubeResponseWithApplicationToken.getData().contains("<Customer>"),
                   "Response data mismatched");

        //Invoke Https end point
        HttpResponse youTubeResponseWithApplicationTokenHttps = HttpRequestUtil
                .doGet(gatewayUrl, requestHeaders);
        log.info("Response " + youTubeResponseWithApplicationTokenHttps);
        assertEquals(youTubeResponseWithApplicationTokenHttps.getResponseCode(), 200, "Response code mismatched");

        HttpResponse errorResponse = null;
        for (int i = 0; i < 40; i++) {
            errorResponse = HttpRequestUtil
                    .doGet(gatewayUrl,
                           requestHeaders);
        }

        assertEquals(errorResponse.getResponseCode(), 429,
                     "Response code mismatched while token API test case");
        Thread.sleep(60000);
        errorResponse = HttpRequestUtil
                .doGet(gatewayUrl, requestHeaders);
        log.info("Error response " + errorResponse);

        apiPublisher.revokeAccessToken(accessToken, consumerKey, providerName);
        requestHeaders.clear();
        requestHeaders.put("Authorization", "Bearer " + "this-is-incorrect-token");
        requestHeaders.put("accept", "text/xml");
        errorResponse = HttpRequestUtil
                .doGet(gatewayUrl, requestHeaders);
        assertEquals(errorResponse.getResponseCode(), 401,
                     "Response code mismatched while token API test case");
        //TODO handle this in automation core level
        try {
            StringBuilder soapRequest = new StringBuilder(
                    "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            soapRequest.append(" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" ");
            soapRequest
                    .append("xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>");
            soapRequest
                    .append("<GetMyName xmlns=\"http://tempuri.org/\"><name>Sam</name></GetMyName>");
            soapRequest.append("</soap:Body></soap:Envelope>");
            errorResponse = HttpRequestUtil
                    .doPost(new URL(getAPIInvocationURLHttp("tokenTestAPI/1.0.0/most_popular")),
                            soapRequest.toString(), requestHeaders);
            log.info("Error Response " + errorResponse);
        } catch (Exception e) {
            //handle error
        }
        assertEquals(errorResponse.getResponseCode(), 401, "Response code mismatched while token API test case");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication("TokenTestAPI-Application");
        super.cleanUp();
//        serverConfigurationManager.restoreToLastConfiguration();
    }
}
