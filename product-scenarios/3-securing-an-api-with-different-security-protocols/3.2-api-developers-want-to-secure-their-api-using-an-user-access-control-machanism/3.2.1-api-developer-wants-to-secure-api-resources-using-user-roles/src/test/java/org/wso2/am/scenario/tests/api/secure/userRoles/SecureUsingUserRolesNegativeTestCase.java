/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.scenario.tests.api.secure.userRoles;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;

import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestUtils;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.wso2.am.scenario.test.common.ScenarioTestUtils.readFromFile;

public class SecureUsingUserRolesNegativeTestCase extends ScenarioTestBase {

    private static final Log log = LogFactory.getLog(SecureUsingUserRolesNegativeTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIPublisherRestClient apiPublisherAdmin;
    private APIStoreRestClient apiStore;

    private static final String ADMIN_LOGIN_USERNAME = "admin";
    private static final String ADMIN_LOGIN_PW = "admin";
    private static final String LOGIN_PERMISSION = "/permission/admin/login";
    private static final String API_ADMIN_PERMISSION = "/permission/admin";
    private static final String API_SUBSCRIBER_PERMISSION = "/permission/admin/manage/api/subscribe";
    private static final String MANAGER_ROLE = "managerRole";
    private static final String AGENT_ROLE = "agentRole";
    private static final String AGENT_LOGIN_PW = "agent";
    private static final String SUPER_USER = "Harry";
    private static final String SUPER_USER_LOGIN_PW = "super";
    private static final String CUSTOMER_ROLE = "customerRole";
    private static final String CUSTOMER_LOGIN_PW = "customer";
    private static final String ITEM_VIEW = "item_view";
    private static final String ITEM_ADD = "item_add";
    private static final String ORDER_VIEW = "order_view";
    private static final String ORDER_ADD = "order_add";
    private static final String SCOPE_EXISTANCE = "isScopeExist";
    private static final String ROLE_EXISTANCE = "isRoleExist";
    private static final String AGENT = "Paul";
    private static final String CUSTOMER = "Richard";


    List<String> userList = new ArrayList();
    List<String> roleList = new ArrayList();
    private String apiVersion = "1.0.0";
    private String apiVisibility = "public";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    private String apiName = "APIScopeTestAPI";
    private String applicationName = "TestApplication3";
    private File swaggerFile;
    JSONObject swaggerJson;
    String resourceLocation = System.getProperty("test.resource.location");

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        setupUserData();
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login(SUPER_USER, SUPER_USER_LOGIN_PW);
        apiPublisherAdmin = new APIPublisherRestClient(publisherURL);
        apiPublisherAdmin.login(ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        apiStore = new APIStoreRestClient(storeURL);

        // create and publish sample API
        String swaggerFilePath = resourceLocation + "swaggerFiles" + File.separator + "APIScopeTest2.json";
        File swaggerFile = new File(swaggerFilePath);
        String swaggerContent = readFromFile(swaggerFile.getAbsolutePath());
        swaggerJson = new JSONObject(swaggerContent);

        try {
            apiPublisher.developSampleAPI(swaggerJson, SUPER_USER, backendEndPoint, true, apiVisibility);
        } catch (Exception ex) {
            log.error("API publication failed", ex);
        }
    }

    private void setupUserData() {
        try {
            createRoles();
            createUsers();
        } catch (APIManagementException ex) {
            log.error("Users or roles creation failed.", ex);
        }
    }

    private void createRoles() throws APIManagementException {
        createRole(ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW, MANAGER_ROLE, new String[]{API_ADMIN_PERMISSION});
        roleList.add(MANAGER_ROLE);
        createRole(ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW, AGENT_ROLE, new String[]{API_SUBSCRIBER_PERMISSION, LOGIN_PERMISSION});
        roleList.add(AGENT_ROLE);
        createRole(ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW, CUSTOMER_ROLE,
                new String[] { LOGIN_PERMISSION, API_SUBSCRIBER_PERMISSION });
        roleList.add(CUSTOMER_ROLE);
    }

    private void createUsers() throws APIManagementException {
        createUser(SUPER_USER, SUPER_USER_LOGIN_PW, new String[]{MANAGER_ROLE, AGENT_ROLE}, ADMIN_LOGIN_USERNAME,
                ADMIN_LOGIN_PW);
        userList.add(SUPER_USER);
        createUser(AGENT, AGENT_LOGIN_PW, new String[]{AGENT_ROLE}, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        userList.add(AGENT);
        createUser(CUSTOMER, CUSTOMER_LOGIN_PW, new String[]{CUSTOMER_ROLE}, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        userList.add(CUSTOMER);
    }

    private void deleteUsers() throws APIManagementException {
        if (userList.size() > 0) {
            for (String username : userList) {
                this.deleteUser(username, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
            }
        }
    }

    private void deleteRoles() throws APIManagementException {
        if (roleList.size() > 0) {
            for (String role : roleList) {
                this.deleteRole(role, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
            }
        }
    }

    @DataProvider(name = "ScopeAndInValidRoleDataProvider")
    public static Object[][] ValidRoleDataProvider() {
        return new Object[][]{
                {"everyone", ITEM_ADD},
                {"admn", ORDER_ADD},
                {"Internal/Craetor", ORDER_VIEW}
        };
    }

    @Test(description = "3.2.1.5")
    public void testInvokeResourceByOldTokenAfterUpdatingRoleOfScope() throws Exception {
        //Adding a scope with two roles
        Map<String, String> requestHeaders;
        HttpResponse response;
        requestHeaders = new HashMap<String, String>();
        String accessToken;

        //Generate a token
        apiStore.login(SUPER_USER, SUPER_USER_LOGIN_PW);
        updateAPISwagger(CUSTOMER_ROLE, swaggerJson);
        apiStore.login(CUSTOMER, CUSTOMER_LOGIN_PW);
        createNewApplicationAndSubscribe(CUSTOMER, CUSTOMER_LOGIN_PW);
        accessToken = generateKeysAndAccessToken(CUSTOMER, CUSTOMER_LOGIN_PW, ITEM_VIEW);

        //Update the Role in scope
        updateAPISwagger(AGENT_ROLE, swaggerJson);

        //Test invocation of related resource with old token
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        response = HttpRequestUtil
                .doGet(gatewayHttpsURL + "/contextJsonV2/" + apiVersion, requestHeaders);
        assertNotEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                "user can access the GET Method using previous scope token");

        //Reset the API to initial state
        updateAPISwagger(MANAGER_ROLE, swaggerJson);
        apiStore.removeAPISubscriptionByName(apiName, apiVersion, SUPER_USER, applicationName);
        apiStore.removeApplication(applicationName);
    }

    @Test(description = "3.2.1.7")
    public void testResourceWithConfiguredRoleBasedScopeInvokeByInvalidUser() throws Exception {
        //Adding a scope with two roles
        Map<String, String> requestHeaders;
        HttpResponse response;
        requestHeaders = new HashMap<String, String>();
        String accessToken;

        apiStore.login(AGENT, AGENT_LOGIN_PW);
        createNewApplicationAndSubscribe(AGENT, AGENT_LOGIN_PW);
        accessToken = generateKeysAndAccessToken(AGENT, AGENT_LOGIN_PW, ITEM_VIEW);

        requestHeaders.put("Authorization", "Bearer " + accessToken);
        response = HttpRequestUtil
                .doGet(gatewayHttpsURL + "/contextJsonV2/" + apiVersion, requestHeaders);
        assertNotEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Invalid user can access the GET Method");
        apiStore.removeAPISubscriptionByName(apiName, apiVersion, AGENT, applicationName);
        apiStore.removeApplication(applicationName);
    }

    @Test(description = "3.2.1.11")
    public void testInvokeResourceByTokenWithoutRequiredRole() throws Exception {
        //Adding a scope with two roles
        Map<String, String> requestHeaders;
        HttpResponse response;
        requestHeaders = new HashMap<String, String>();
        String accessToken;

        apiStore.login(SUPER_USER, SUPER_USER_LOGIN_PW);
        createNewApplicationAndSubscribe(SUPER_USER, SUPER_USER_LOGIN_PW);
        accessToken = generateKeysAndAccessToken(SUPER_USER, SUPER_USER_LOGIN_PW, "");

        requestHeaders.put("Authorization", "Bearer " + accessToken);
        response = HttpRequestUtil
                .doGet(gatewayHttpsURL + "/contextJsonV2/" + apiVersion, requestHeaders);
        assertNotEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                "user can access the GET Method by token which ");
        apiStore.removeAPISubscriptionByName(apiName, apiVersion, SUPER_USER, applicationName);
        apiStore.removeApplication(applicationName);
    }

    @Test(description = "3.2.1.9", dataProvider = "ScopeAndInValidRoleDataProvider",
            dataProviderClass = SecureUsingUserRolesNegativeTestCase.class)
    public void testScopeCreationWithInValidRoles(String role, String scope) throws Exception {
        HttpResponse httpResponse = apiPublisher.validateScope(scope, role);
        verifyResponse(httpResponse);
        assertEquals(new JSONObject(httpResponse.getData()).get(ROLE_EXISTANCE).toString(), "false",
                "Error in scope creation with Invalid values. Role  : " + role);
    }

    @Test(description = "3.2.1.13")
    public void testScopeWithDuplicateKey() throws Exception {
        // This swagger will create "item_view" scope and assign it to a resource.
        swaggerFile = new File(resourceLocation + File.separator + "swaggerFiles/APIScopeTest1.json");
        String payload = ScenarioTestUtils.readFromFile(swaggerFile.getAbsolutePath());
        HttpResponse updateResponse = apiPublisher.updateResourceOfAPI(SUPER_USER, apiName, apiVersion, payload);
        verifyResponse(updateResponse);
        // Redeclare scope with item_view key.
        HttpResponse httpResponse = apiPublisher.validateScope(ITEM_VIEW, AGENT_ROLE);
        verifyResponse(httpResponse);
        assertEquals(new JSONObject(httpResponse.getData()).get(SCOPE_EXISTANCE).toString(), "true",
                "Error in scope creation with duplicate key : " + ITEM_VIEW);
    }

    private void updateAPISwagger(String role, JSONObject swaggerJson) throws Exception {
        swaggerJson.getJSONObject("x-wso2-security").getJSONObject("apim").getJSONArray("x-wso2-scopes").
                getJSONObject(0).remove("roles");

        swaggerJson.getJSONObject("x-wso2-security").getJSONObject("apim").getJSONArray("x-wso2-scopes").
                getJSONObject(0).accumulate("roles", role);

        String updatedPayload = swaggerJson.toString();

        HttpResponse updateResponse = apiPublisher.updateResourceOfAPI(SUPER_USER, apiName, apiVersion, updatedPayload);
        verifyResponse(updateResponse);
    }

    private void createNewApplicationAndSubscribe(String user, String password) throws Exception {
        apiStore.login(user, password);
        apiStore.addApplication(applicationName, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, SUPER_USER);
        subscriptionRequest.setApplicationName(applicationName);
        apiStore.subscribe(subscriptionRequest);
    }

    private String generateKeysAndAccessToken(String user, String password, String requiredScope) throws Exception {
        //Generate production token and invoke with that
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(applicationName);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject jsonResponse = new JSONObject(responseString);

        // get Consumer Key and Consumer Secret
        String consumerKey = jsonResponse.getJSONObject("data").getJSONObject("key").getString("consumerKey");
        String consumerSecret = jsonResponse.getJSONObject("data").getJSONObject("key").getString("consumerSecret");

        URL tokenEndpointURL = new URL(gatewayHttpsURL + "/token");
        HttpResponse response;
        String requestBody;
        JSONObject accessTokenGenerationResponse;

        //Obtain user access token for user
        if (requiredScope.isEmpty()) {
            requestBody = "grant_type=password&username=" + user + "&password=" + password;
        } else {
            requestBody = "grant_type=password&username=" + user + "&password=" + password + "&scope=" + requiredScope;
        }

        response = apiStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL);
        accessTokenGenerationResponse = new JSONObject(response.getData());
        return accessTokenGenerationResponse.getString("access_token");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws APIManagerIntegrationTestException {
        try {
            deleteUsers();
            deleteRoles();
        } catch (APIManagementException ex) {
            log.error("Users or role deletion failed", ex);
        }
        apiPublisherAdmin.deleteAPI(apiName, apiVersion, SUPER_USER);
    }
}
