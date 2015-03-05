/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.am.tests.sample;

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
import org.wso2.carbon.automation.api.clients.user.mgt.UserManagementClient;
import org.wso2.carbon.automation.core.utils.HttpRequestUtil;
import org.wso2.carbon.automation.core.utils.HttpResponse;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * This covers adding two scopes with different roles and accessing them with users with those roles
 * This Test-case uses the existing admin role and admin user
 * Doc For API used :- http://sqlrest.sourceforge.net/5-minutes-guide.htm
 */
public class APIScopeTestCase extends APIManagerIntegrationTest {

    private APIPublisherRestClient apiPublisher;

    private APIStoreRestClient apiStore;

    private UserManagementClient userManagementClient = null;

    private static final String API_NAME = "APIScopeTestAPI";

    private static final String API_VERSION = "1.0.0";

    private static final String API_PROVIDER = "admin";

    private static final String APP_NAME = "NewApplication";

    private static final String USER_JOHN = "john";

    private static final String SUBSCRIBER_ROLE = "subscriber";


    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init(0);

        String publisherURLHttp;

        String storeURLHttp;

        if (isBuilderEnabled()) {
            publisherURLHttp = getServerURLHttp();
            storeURLHttp = getServerURLHttp();

        } else {
            publisherURLHttp = getPublisherServerURLHttp();
            storeURLHttp = getStoreServerURLHttp();
        }

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);

        apiStore = new APIStoreRestClient(storeURLHttp);
    }

    @Test(groups = {"wso2.am"}, description = "Testing the scopes with admin, subscriber roles")
    public void testSetScopeToResourceTestCase() throws Exception {


        userManagementClient = new UserManagementClient(amServer.getBackEndUrl(), "admin",
                                                        "admin");

        // adding new role subscriber
        userManagementClient.addRole(SUBSCRIBER_ROLE, new String[]{}, new String[]{"/permission/admin/login",
                                                                                   "/permission/admin/manage/api/subscribe"});

        // crating user john
        userManagementClient.addUser(USER_JOHN, "john123", new String[]{SUBSCRIBER_ROLE}, USER_JOHN);


        // Adding API
        String apiContext = "testScopeAPI";
        String tags = "thomas-bayer, testing, rest-Apis";
        String url = "http://www.thomas-bayer.com/sqlrest/";
        String description = "This is a test API created by API manager integration test";

        apiPublisher.login(userInfo.getUserName(), userInfo.getPassword());
        APIRequest apiRequest = new APIRequest(API_NAME, apiContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(API_VERSION);
        apiPublisher.addAPI(apiRequest);

        //publishing API
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(API_NAME, API_PROVIDER,
                                                                              APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatusTo(updateRequest);


        //resources are modified using swagger doc.
        // admin_scope(used for POST) :- admin
        // user_scope (used for GET) :- admin,subscriber
        String modifiedResource = "{\"apiVersion\":\"1.0.0\",\"swaggerVersion\":\"1.2\"," +
                                  "\"authorizations\":{\"oauth2\":{\"scopes\":[{\"description\":\"\", " +
                                  "\"name\":\"admin_scope\",\"roles\":\"admin\",\"key\":\"admin_scope\"}," +
                                  "{\"description\":\"\",\"name\":\"user_scope\",\"roles\":\"admin,subscriber\"," +
                                  "\"key\":\"user_scope\"}]," +
                                  "\"type\":\"oauth2\"}},\"apis\":[{\"index\":0,\"file\":{\"apiVersion\":\"1.0.0\"," +
                                  "\"swaggerVersion\":\"1.2\",\"resourcePath\":\"/default\",\"apis\":[{\"index\":0," +
                                  "\"path\":\"/*\",\"operations\":[{\"scope\":\"user_scope\"," +
                                  "\"auth_type\":\"Application User\"," +
                                  "\"throttling_tier\":\"Unlimited\",\"method\":\"GET\",\"parameters\":[]}," +
                                  "{\"scope\":\"admin_scope\",\"auth_type\":\"Application User\"," +
                                  "\"throttling_tier\":\"Unlimited\"," +
                                  "\"method\":\"POST\",\"parameters\":[]},{\"scope\":\"\",\"auth_type\":\"Application" +
                                  " User\"," +
                                  "\"throttling_tier\":\"Unlimited\",\"method\":\"PUT\",\"parameters\":[]}," +
                                  "{\"auth_type\":\"Application User\",\"throttling_tier\":\"Unlimited\"," +
                                  "\"method\":\"DELETE\"," +
                                  "\"parameters\":[]},{\"auth_type\":\"None\",\"throttling_tier\":\"Unlimited\"," +
                                  "\"method\":\"OPTIONS\",\"parameters\":[]}]}]},\"description\":\"\",\"path\":" +
                                  "\"/default\"}],\"info\":{\"title\":\"" + API_NAME + "\",\"termsOfServiceUrl\":\"" +
                                  "\",\"description\":\"\",\"license\":\"\",\"contact\":\"\",\"licenseUrl\":\"\"}}";


        apiPublisher.updateResourceOfAPI(API_PROVIDER, API_NAME, API_VERSION, modifiedResource);

        // For Admin user
        // create new application and subscribing
        apiStore.login("admin", "admin");
        apiStore.addApplication(APP_NAME, "Unlimited", "some_url", "NewApp");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(API_NAME, API_PROVIDER);
        subscriptionRequest.setApplicationName(APP_NAME);
        apiStore.subscribe(subscriptionRequest);

        //Generate production token and invoke with that
        GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest(APP_NAME);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject jsonResponse = new JSONObject(responseString);

        // get Consumer Key and Consumer Secret
        String consumerKey = jsonResponse.getJSONObject("data").getJSONObject("key").getString("consumerKey");
        String consumerSecret = jsonResponse.getJSONObject("data").getJSONObject("key").getString("consumerSecret");

        URL tokenEndpointURL = new URL("https://localhost:8243/token");
        String accessToken;
        Map<String, String> requestHeaders;
        HttpResponse response;
        URL endPointURL;
        String requestBody;
        JSONObject accessTokenGenerationResponse;

        //Obtain user access token for Admin
        requestBody = "grant_type=password&username=admin&password=admin&scope=admin_scope user_scope";
        accessTokenGenerationResponse = new JSONObject(apiStore.generateUserAccessKey(consumerKey, consumerSecret,
                                                                                      requestBody, tokenEndpointURL)
                                                               .getData());
        accessToken = accessTokenGenerationResponse.getString("access_token");

        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        // Accessing GET method
        response = HttpRequestUtil.doGet(getApiInvocationURLHttp("testScopeAPI/1.0.0/ITEM"), requestHeaders);
        Assert.assertEquals(response.getResponseCode(), 200, "Admin user cannot access the GET Method");

        // Accessing POST method
        endPointURL = new URL(getApiInvocationURLHttp("testScopeAPI/1.0.0/PRODUCT/35"));
        response = HttpRequestUtil.doPost(endPointURL, "<resource><PRICE>8.5</PRICE></resource>", requestHeaders);
        Assert.assertEquals(response.getResponseCode(), 200, "Admin user cannot access the POST Method");


        //Obtaining user access token for john
        requestBody = "grant_type=password&username=" + USER_JOHN + "&password=john123&scope=admin_scope user_scope";
        accessTokenGenerationResponse = new JSONObject(apiStore.generateUserAccessKey(consumerKey, consumerSecret,
                                                                                      requestBody, tokenEndpointURL)
                                                               .getData());
        accessToken = accessTokenGenerationResponse.getString("access_token");

        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        // Accessing GET method
        response = HttpRequestUtil.doGet(getApiInvocationURLHttp("testScopeAPI/1.0.0/ITEM"), requestHeaders);
        Assert.assertEquals(response.getResponseCode(), 200, "User John cannot access the GET Method");

        try {
            // Accessing POST method
            endPointURL = new URL(getApiInvocationURLHttp("testScopeAPI/1.0.0/PRODUCT/35"));
            response = HttpRequestUtil.doPost(endPointURL, "<resource><PRICE>8.5</PRICE></resource>", requestHeaders);
            Assert.assertTrue(response.getResponseCode() != 200, "testRole John can access the POST Method");

        } catch (Exception e) {
            log.error("user john cannot access the resources (expected behaviour)");
            Assert.assertTrue(true);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        if (apiStore != null) {
            apiStore.removeApplication(APP_NAME);
        }

        if (apiPublisher != null) {
            apiPublisher.deleteApi(API_NAME, API_VERSION, API_PROVIDER);
        }

        if (userManagementClient != null) {
            userManagementClient.deleteUser(USER_JOHN);
            userManagementClient.deleteRole(SUBSCRIBER_ROLE);
        }
        super.cleanup();
    }
}
