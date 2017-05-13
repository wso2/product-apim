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

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
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
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class APIScopeTestForTenantsTestCase extends APIMIntegrationBaseTest {

    // Details of the first tenant
    private final String TENANT1_DOMAIN = "tenantscope1.com";
    private final String TENANT1_ADMIN_USERNAME = "firstAdmin";
    private final String TENANT1_ADMIN_PASSWORD = "password1";
    private final String TENANT1_API_NAME = "APIScopeTenantAPI1";
    private final String TENANT1_API_VERSION = "1.0.0";
    private final String TENANT1_APP_NAME = "TenantScope1App";
    private final String TENANT1_ADMIN_USER = TENANT1_ADMIN_USERNAME + "@" + TENANT1_DOMAIN;
    private final String TENANT1_API_CONTEXT = "testScopeAPITenant1";
    private UserManagementClient userManagementClient1 = null;
    private final String TENANT1_SUBSCRIBER_ROLE = "subscriberTenant1";
    private final String USER_PETER = "peter";
    private final String USER_PASSWORD = "peter123";
    private final String TENANT1_USER_PETER = USER_PETER + "@" + TENANT1_DOMAIN;
    // Details of the second tenant
    private final String TENANT2_DOMAIN = "tenantscope2.com";
    private final String TENANT2_ADMIN_USERNAME = "secondAdmin";
    private final String TENANT2_ADMIN_PASSWORD = "password2";
    private final String TENANT2_API_NAME = "APIScopeTenantAPI2";
    private final String TENANT2_API_VERSION = "1.0.0";
    private final String TENANT2_APP_NAME = "TenantScope2App";
    private final String TENANT2_ADMIN_USER = TENANT2_ADMIN_USERNAME + "@" + TENANT2_DOMAIN;
    private final String TENANT2_API_CONTEXT = "testScopeAPITenant2";
    private final String TENANT2_SUBSCRIBER_ROLE = "subscriberTenant2";
    private final String TENANT2_USER_PETER = USER_PETER + "@" + TENANT2_DOMAIN;
    private UserManagementClient userManagementClient2 = null;

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String gatewaySessionCookie;


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();

        apiPublisher = new APIPublisherRestClient(getPublisherURLHttp());
        apiStore = new APIStoreRestClient(getStoreURLHttp());
        String[] userPermissions = new String[]{"/permission/admin/login", "/permission/admin/manage/api/subscribe"};

        // Create the first tenant
        tenantManagementServiceClient.addTenant(TENANT1_DOMAIN, TENANT1_ADMIN_PASSWORD, TENANT1_ADMIN_USERNAME,
                "demo");

        userManagementClient1 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                TENANT1_ADMIN_USER, TENANT1_ADMIN_PASSWORD);
        // Adding new role
        userManagementClient1.addRole(TENANT1_SUBSCRIBER_ROLE, new String[]{}, userPermissions);
        userManagementClient1.addUser(USER_PETER, USER_PASSWORD, new String[]{TENANT1_SUBSCRIBER_ROLE}, USER_PETER);


        // Create the second tenant
        tenantManagementServiceClient.addTenant(TENANT2_DOMAIN, TENANT2_ADMIN_PASSWORD, TENANT2_ADMIN_USERNAME,
                "demo");
        userManagementClient2 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                TENANT2_ADMIN_USER, TENANT2_ADMIN_PASSWORD);
        // Adding new role
        userManagementClient2.addRole(TENANT2_SUBSCRIBER_ROLE, new String[]{}, userPermissions);
        userManagementClient2.addUser(USER_PETER, USER_PASSWORD, new String[]{TENANT2_SUBSCRIBER_ROLE}, USER_PETER);

        gatewaySessionCookie = createSession(gatewayContextMgt);
        //Load the back-end dummy API
        loadSynapseConfigurationFromClasspath(
                "artifacts" + File.separator + "AM" + File.separator + "synapseconfigs" + File.separator + "rest"
                        + File.separator + "dummy_api.xml", gatewayContextMgt, gatewaySessionCookie);

    }

    @Test(groups = {"wso2.am"}, description = "Testing using same scope key in tenants")
    public void testSameScopeInTenants() throws Exception {

        // Publish the API for Tenant1
        publishAPI(TENANT1_ADMIN_USER, TENANT1_ADMIN_PASSWORD, TENANT1_API_NAME, TENANT1_API_CONTEXT,
                TENANT1_API_VERSION, TENANT1_SUBSCRIBER_ROLE);

        apiStore.login(TENANT1_USER_PETER, USER_PASSWORD);

        // App creation and subscribe to the API
        subscribeToAPI(TENANT1_APP_NAME, TENANT1_API_NAME, TENANT1_ADMIN_USER);

        // Generate production token
        JSONObject jsonResponse = getApplicationKeys(TENANT1_APP_NAME);
        String consumerKey = jsonResponse.getJSONObject("data").getJSONObject("key").getString("consumerKey");
        String consumerSecret = jsonResponse.getJSONObject("data").getJSONObject("key").getString("consumerSecret");

        // Generate user access token
        JSONObject accessTokenGenerationResponse = getUserAccessKeys(TENANT1_USER_PETER, USER_PASSWORD, consumerKey,
                consumerSecret);

        apiStore.logout();

        String accessToken = accessTokenGenerationResponse.getString("access_token");
        String gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" + TENANT1_DOMAIN + "/";

        // Invoke the API
        HttpResponse response = invokeAPI(accessToken, gatewayUrl, TENANT1_API_CONTEXT, TENANT1_API_VERSION);
        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                TENANT1_USER_PETER + " can access the POST method");


        // Publish the API for Tenant2
        publishAPI(TENANT2_ADMIN_USER, TENANT2_ADMIN_PASSWORD, TENANT2_API_NAME, TENANT2_API_CONTEXT,
                TENANT2_API_VERSION, TENANT2_SUBSCRIBER_ROLE);

        apiStore.login(TENANT2_USER_PETER, USER_PASSWORD);

        // App creation and subscribe to the API
        subscribeToAPI(TENANT2_APP_NAME, TENANT2_API_NAME, TENANT2_ADMIN_USER);

        // Generate production token
        jsonResponse = getApplicationKeys(TENANT2_APP_NAME);
        consumerKey = jsonResponse.getJSONObject("data").getJSONObject("key").getString("consumerKey");
        consumerSecret = jsonResponse.getJSONObject("data").getJSONObject("key").getString("consumerSecret");

        // Generate user access token
        accessTokenGenerationResponse = getUserAccessKeys(TENANT2_USER_PETER, USER_PASSWORD, consumerKey,
                consumerSecret);

        apiStore.logout();

        accessToken = accessTokenGenerationResponse.getString("access_token");
        gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" + TENANT2_DOMAIN + "/";

        // Invoke the API
        response = invokeAPI(accessToken, gatewayUrl, TENANT2_API_CONTEXT, TENANT2_API_VERSION);
        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                TENANT2_USER_PETER + " can access the POST method");

    }

    private void publishAPI(String adminUser, String adminPassword, String apiName, String apiContext,
                            String apiVersion, String role) throws Exception {

        String url = getGatewayURLNhttp() + "response";
        String description = "This is a test API created by API manager integration test";

        apiPublisher.login(adminUser, adminPassword);
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(url));
        apiRequest.setDescription(description);
        apiRequest.setVersion(apiVersion);
        apiRequest.setProvider(adminUser);
        apiPublisher.addAPI(apiRequest);

        // Publishing API
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, adminUser,
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        waitForAPIDeploymentSync(adminUser, apiName, apiVersion,
                APIMIntegrationConstants.IS_API_EXISTS);

        // Resources are modified using swagger doc.
        // user_scope(used for POST)
        String modifiedResource = "{\"paths\":{ \"/test\":{\"put\":{ \"responses\":{\"200\":{}},\"x-auth-type\":" +
                "\"Application User\",\"x-throttling-tier\":\"Unlimited\" },\"post\":{ \"responses\":{\"200\":{}}," +
                "\"x-auth-type\":\"Application User\",\"x-throttling-tier\":\"Unlimited\",\"x-scope\":" +
                "\"user_scope\"},\"get\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"Application User\"," +
                "\"x-throttling-tier\":\"Unlimited\" },\"delete\":{ \"responses\":{\"200\":{}},\"x-auth-type\":" +
                "\"Application User\",\"x-throttling-tier\":\"Unlimited\"},\"options\":{ \"responses\":{\"200\":{}}," +
                "\"x-auth-type\":\"None\",\"x-throttling-tier\":\"Unlimited\"}}},\"swagger\":\"2.0\",\"info" +
                "\":{\"title\":\"APIScopeTestAPI\",\"version\":\"1.0.0\"},\"x-wso2-security\":{\"apim" +
                "\":{\"x-wso2-scopes\":[{\"name\":\"user_scope\",\"description\":\"\",\"key\":\"user_scope\",\"roles" +
                "\":\"" + role + "\"}]}}}";

        apiPublisher.updateResourceOfAPI(adminUser, apiName, apiVersion,
                modifiedResource);

        waitForAPIDeployment();

        apiPublisher.logout();

    }

    private void subscribeToAPI(String appName, String apiName, String adminUser) throws Exception {

        apiStore.addApplication(appName, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "some_url",
                "NewApp");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, adminUser);
        subscriptionRequest.setApplicationName(appName);
        apiStore.subscribe(subscriptionRequest);
    }

    private JSONObject getApplicationKeys(String appName) throws Exception {

        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(appName);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        return new JSONObject(responseString);
    }

    private JSONObject getUserAccessKeys(String username, String password, String consumerKey,
                                         String consumerSecret) throws Exception {

        URL tokenEndpointURL = new URL(gatewayUrlsWrk.getWebAppURLNhttp() + "token");
        HttpResponse response;
        String requestBody;

        // Obtain user access token
        requestBody = "grant_type=password&username=" + username +
                "&password=" + password +
                "&scope=user_scope";

        response = apiStore.generateUserAccessKey(consumerKey, consumerSecret,
                requestBody, tokenEndpointURL);
        return new JSONObject(response.getData());
    }

    private HttpResponse invokeAPI(String accessToken, String gatewayUrl, String apiContext, String apiVersion)
            throws Exception {

        Map<String, String> requestHeaders = new HashMap<String, String>();
        URL endPointURL = new URL(gatewayUrl + apiContext + "/" + apiVersion + "/test");
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        // Accessing POST method
        return HttpRequestUtil.doPost(endPointURL, "", requestHeaders);

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        if (apiStore != null) {
            apiStore.login(TENANT1_USER_PETER, USER_PASSWORD);
            apiStore.removeApplication(TENANT1_APP_NAME);
            apiStore.logout();

            apiStore.login(TENANT2_USER_PETER, USER_PASSWORD);
            apiStore.removeApplication(TENANT2_APP_NAME);
            apiStore.logout();
        }

        if (apiPublisher != null) {
            apiPublisher.login(TENANT1_ADMIN_USER, TENANT1_ADMIN_PASSWORD);
            apiPublisher.deleteAPI(TENANT1_API_NAME, TENANT1_API_VERSION, TENANT1_ADMIN_USER);
            apiPublisher.logout();

            apiPublisher.login(TENANT2_ADMIN_USER, TENANT2_ADMIN_PASSWORD);
            apiPublisher.deleteAPI(TENANT2_API_NAME, TENANT2_API_VERSION, TENANT2_ADMIN_USER);
            apiPublisher.logout();
        }

        if (userManagementClient1 != null) {
            userManagementClient1.deleteUser(USER_PETER);
            userManagementClient1.deleteRole(TENANT1_SUBSCRIBER_ROLE);
        }

        if (userManagementClient2 != null) {
            userManagementClient2.deleteUser(USER_PETER);
            userManagementClient2.deleteRole(TENANT2_SUBSCRIBER_ROLE);
        }

        super.cleanUp();
    }

}
