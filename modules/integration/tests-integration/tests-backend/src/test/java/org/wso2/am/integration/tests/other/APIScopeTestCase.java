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

package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class APIScopeTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(APIScopeTestCase.class);

    private APIPublisherRestClient apiPublisher;

    private APIStoreRestClient apiStore;

    private UserManagementClient userManagementClient = null;

    private static final String API_NAME = "APIScopeTestAPI";

    private static final String API_VERSION = "1.0.0";

    private static final String APP_NAME = "NewApplication";

    private static final String USER_JOHN = "john";

    private static final String SUBSCRIBER_ROLE = "subscriber";

    private static String apiProvider;


    @Factory(dataProvider = "userModeDataProvider")
    public APIScopeTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);

        apiProvider = publisherContext.getSuperTenant().getContextUser().getUserName();

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();

        String storeURLHttp = storeUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);

        apiStore = new APIStoreRestClient(storeURLHttp);
    }

    @Test(groups = {"wso2.am"}, description = "Testing the scopes with admin, subscriber roles")
    public void testSetScopeToResourceTestCase() throws Exception {


        userManagementClient = new UserManagementClient(gatewayContext.getContextUrls().getBackEndUrl(),
                                                        gatewayContext.getContextTenant().getContextUser().getUserName(),
                                                        gatewayContext.getContextTenant().getContextUser().getPassword());

        // adding new role subscriber
        userManagementClient.addRole(SUBSCRIBER_ROLE, new String[]{}, new String[]{"/permission/admin/login",
                                                                                   "/permission/admin/manage/api/subscribe"});

        // crating user john
        String userJohn;
        String gatewayUrl;
        if (gatewayContext.getContextTenant().getDomain().equals("carbon.super")) {
            gatewayUrl = gatewayUrls.getWebAppURLNhttp();
            userJohn = USER_JOHN;
        } else {
            gatewayUrl = gatewayUrls.getWebAppURLNhttp() + "t/" + gatewayContext.getContextTenant().getDomain() + "/";
            userJohn = USER_JOHN + "@" + gatewayContext.getContextTenant().getDomain();
        }
        userManagementClient.addUser(USER_JOHN, "john123", new String[]{SUBSCRIBER_ROLE}, USER_JOHN);


        // Adding API
        String apiContext = "testScopeAPI";
        String tags = "thomas-bayer, testing, rest-Apis";
        String url = "http://www.thomas-bayer.com/sqlrest/";
        String description = "This is a test API created by API manager integration test";

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                           publisherContext.getContextTenant().getContextUser().getPassword());
        APIRequest apiRequest = new APIRequest(API_NAME, apiContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(API_VERSION);
        apiRequest.setProvider(apiProvider);
        apiPublisher.addAPI(apiRequest);

        //publishing API
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(API_NAME, apiProvider,
                                                                              APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);


        //resources are modified using swagger doc.
        // admin_scope(used for POST) :- admin
        // user_scope (used for GET) :- admin,subscriber
        String modifiedResource = "{\"paths\":{ \"/*\":{\"put\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"Application User\"," +
                                  "\"x-throttling-tier\":\"Unlimited\" },\"post\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"Application User\"," +
                                  "\"x-throttling-tier\":\"Unlimited\",\"x-scope\":\"admin_scope\"},\"get\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"Application User\"," +
                                  "\"x-throttling-tier\":\"Unlimited\",\"x-scope\":\"user_scope\"},\"delete\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"Application User\"," +
                                  "\"x-throttling-tier\":\"Unlimited\"},\"options\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"None\"," +
                                  "\"x-throttling-tier\":\"Unlimited\"}}},\"swagger\":\"2.0\",\"info\":{\"title\":\"APIScopeTestAPI\",\"version\":\"1.0.0\"}," +
                                  "\"x-wso2-security\":{\"apim\":{\"x-wso2-scopes\":[{\"name\":\"admin_scope\",\"description\":\"\",\"key\":\"admin_scope\",\"roles\":\"admin\"}," +
                                  "{\"name\":\"user_scope\",\"description\":\"\",\"key\":\"user_scope\",\"roles\":\"admin,subscriber\"}]}}}";


        apiPublisher.updateResourceOfAPI(apiProvider, API_NAME, API_VERSION, modifiedResource);

        // For Admin user
        // create new application and subscribing
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                       storeContext.getContextTenant().getContextUser().getPassword());
        apiStore.addApplication(APP_NAME, "Unlimited", "some_url", "NewApp");

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(API_NAME, apiProvider);
        subscriptionRequest.setApplicationName(APP_NAME);
        apiStore.subscribe(subscriptionRequest);

        //Generate production token and invoke with that
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(APP_NAME);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject jsonResponse = new JSONObject(responseString);

        // get Consumer Key and Consumer Secret
        String consumerKey = jsonResponse.getJSONObject("data").getJSONObject("key").getString("consumerKey");
        String consumerSecret = jsonResponse.getJSONObject("data").getJSONObject("key").getString("consumerSecret");

        URL tokenEndpointURL = new URL(gatewayUrls.getWebAppURLNhttp() + "token");
        String accessToken;
        Map<String, String> requestHeaders;
        HttpResponse response;
        URL endPointURL;
        String requestBody;
        JSONObject accessTokenGenerationResponse;

        //Obtain user access token for Admin
        requestBody = "grant_type=password&username=" + storeContext.getContextTenant().getContextUser().getUserName() +
                      "&password=" + storeContext.getContextTenant().getContextUser().getPassword() +
                      "&scope=admin_scope user_scope";

        accessTokenGenerationResponse = new JSONObject(apiStore.generateUserAccessKey(consumerKey, consumerSecret,
                                                                                      requestBody, tokenEndpointURL).getData());
        accessToken = accessTokenGenerationResponse.getString("access_token");

        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        // Accessing GET method
        response = HttpRequestUtil.doGet(gatewayUrl + "testScopeAPI/1.0.0/ITEM", requestHeaders);
        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                     "Admin user cannot access the GET Method");

        // Accessing POST method
        endPointURL = new URL(gatewayUrl + "testScopeAPI/1.0.0/PRODUCT/35");
        response = HttpRequestUtil.doPost(endPointURL, "<resource><PRICE>8.5</PRICE></resource>", requestHeaders);
        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                     "Admin user cannot access the POST Method");


        //Obtaining user access token for john
        requestBody = "grant_type=password&username=" + userJohn + "&password=john123&scope=admin_scope user_scope";
        accessTokenGenerationResponse = new JSONObject(apiStore.generateUserAccessKey(consumerKey, consumerSecret,
                                                                                      requestBody, tokenEndpointURL)
                                                               .getData());
        accessToken = accessTokenGenerationResponse.getString("access_token");

        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        // Accessing GET method
        response = HttpRequestUtil.doGet(gatewayUrl + "testScopeAPI/1.0.0/ITEM", requestHeaders);
        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                     "User John cannot access the GET Method");

        try {
            // Accessing POST method
            endPointURL = new URL(gatewayUrl + "testScopeAPI/1.0.0/PRODUCT/35");
            response = HttpRequestUtil.doPost(endPointURL, "<resource><PRICE>8.5</PRICE></resource>", requestHeaders);
            assertTrue(response.getResponseCode() != Response.Status.OK.getStatusCode(),
                       "testRole John can access the POST Method");

        } catch (Exception e) {
            log.error("user john cannot access the resources (expected behaviour)");
            assertTrue(true, "user john cannot access the resources");
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        if (apiStore != null) {
            apiStore.removeApplication(APP_NAME);
        }

        if (apiPublisher != null) {
            apiPublisher.deleteAPI(API_NAME, API_VERSION, apiProvider);
        }

        if (userManagementClient != null) {
            userManagementClient.deleteUser(USER_JOHN);
            userManagementClient.deleteRole(SUBSCRIBER_ROLE);
        }
        super.cleanUp(gatewayContext.getContextTenant().getTenantAdmin().getUserName(),
                      gatewayContext.getContextTenant().getContextUser().getPassword(),
                      storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
//                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }
}

