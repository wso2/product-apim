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
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
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

public class SecureUsingUserRolesTestCases extends ScenarioTestBase {

    private APIStoreRestClient apiStore;
    private APIPublisherRestClient apiPublisher;
    private APIPublisherRestClient apiPublisherAdmin;
    private List<String> applicationsList = new ArrayList<>();
    private static final Log log = LogFactory.getLog(SecureUsingUserRolesTestCases.class);
    private static final String ADMIN_LOGIN_USERNAME = "admin";
    private static final String ADMIN_LOGIN_PW = "admin";
    private static final String LOGIN_PERMISSION = "/permission/admin/login";
    private static final String API_ADMIN_PERMISSION = "/permission/admin";
    private static final String API_PUBLISHER_PERMISSION = "/permission/admin/manage/api/publish";
    private static final String API_CREATOR_PERMISSION = "/permission/admin/manage/api/create";
    private static final String API_SUBSCRIBER_PERMISSION = "/permission/admin/manage/api/subscribe";
    private static final String MANAGER_ROLE = "managerRole";
    private static final String MANAGER_LOGIN_PW = "manager";
    private static final String AGENT_ROLE = "agentRole";
    private static final String AGENT_LOGIN_PW = "agent";
    private static final String CUSTOMER_ROLE = "customerRole";
    private static final String CUSTOMER_LOGIN_PW = "customer";
    private static final String MANAGER = "Alex";
    private static final String AGENT = "Paul";
    private static final String CUSTOMER = "Rick";
    private static final String SUPER_USER = "Harry";
    private static final String SUPER_USER_LOGIN_PW = "super";
    private static final String ITEM_VIEW = "item_view";
    private static final String ITEM_ADD = "item_add";
    private static final String ORDER_VIEW = "order_view";
    private static final String ORDER_ADD = "order_add";
    private static final String SCOPE_EXISTANCE = "isScopeExist";
    private static final String ROLE_EXISTANCE = "isRoleExist";
    private String apiVersion = "1.0.0";
    private String apiVisibility = "public";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    private String apiName = "APIScopeTestAPI";
    private String applicationName = "TestApplication";

    List<String> userList = new ArrayList();
    List<String> roleList = new ArrayList();
    File swaggerFile;
    String resourceLocation = System.getProperty("test.resource.location");

    @DataProvider(name = "ScopeAndValidRoleDataProvider")
    public static Object[][] ValidRoleDataProvider() {
        return new Object[][]{
                {MANAGER_ROLE, ITEM_ADD},
                {AGENT_ROLE, ORDER_ADD},
                {CUSTOMER_ROLE, ORDER_VIEW}
        };
    }

    @DataProvider(name = "SwaggerFilesAndVerb")
    public static Object[][] SwaggerFileAndHttpVerb() {
        ArrayList<String> httpverb1 = new ArrayList<>() ;
        httpverb1.add("PUT");
        ArrayList<String> httpverb2 = new ArrayList<>() ;
        httpverb2.add("PUT");
        httpverb2.add("GET");
        return new Object[][]{
                {"APIScopeTest1.json", httpverb1},
                {"APIScopeTest2.json", httpverb2}
        };
    }

    @DataProvider(name="StoreUserDataProvider")
    public static  Object[][] storeUserDataProvider() {
        return new Object[][]{
                {AGENT, AGENT_LOGIN_PW},
                {CUSTOMER, CUSTOMER_LOGIN_PW}
        };
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
        createRole(ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW, AGENT_ROLE, new String[]{API_PUBLISHER_PERMISSION,
                API_CREATOR_PERMISSION, API_SUBSCRIBER_PERMISSION, LOGIN_PERMISSION});
        roleList.add(AGENT_ROLE);
        createRole(ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW, CUSTOMER_ROLE,
                new String[] { LOGIN_PERMISSION, API_SUBSCRIBER_PERMISSION });
        roleList.add(CUSTOMER_ROLE);
    }

    private void createUsers() throws APIManagementException {
        createUser(MANAGER, MANAGER_LOGIN_PW, new String[]{MANAGER_ROLE}, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        userList.add(MANAGER);
        createUser(AGENT, AGENT_LOGIN_PW, new String[]{AGENT_ROLE}, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        userList.add(AGENT);
        createUser(CUSTOMER, CUSTOMER_LOGIN_PW, new String[]{CUSTOMER_ROLE}, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        userList.add(CUSTOMER);
        createUser(SUPER_USER, SUPER_USER_LOGIN_PW, new String[]{MANAGER_ROLE, AGENT_ROLE}, ADMIN_LOGIN_USERNAME,
                ADMIN_LOGIN_PW);
        userList.add(SUPER_USER);
    }

    private void createNewApplicationAndSubscribe(String user, String password) throws Exception {
        apiStore.login(user, password);
        apiStore.addApplication(applicationName, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, SUPER_USER);
        subscriptionRequest.setApplicationName(applicationName);
        apiStore.subscribe(subscriptionRequest);
    }

    private String generateKeysAndAccessToken(String user, String password) throws Exception {
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

        //Obtain user access token for Admin
        requestBody = "grant_type=password&username=" + user + "&password=" + password + "&scope=order_view";

        response = apiStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL);
        accessTokenGenerationResponse = new JSONObject(response.getData());
        return accessTokenGenerationResponse.getString("access_token");
    }

    private void deleteUsers() throws APIManagementException {
        if (userList.size() > 0) {
            for (String username : userList) {
                deleteUser(username, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
            }
        }
    }

    private void deleteRoles() throws APIManagementException {
        if (roleList.size() > 0) {
            for (String role : roleList) {
                deleteRole(role, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
            }
        }
    }

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException, APIManagementException {
        setupUserData();
        apiStore = new APIStoreRestClient(storeURL);
        apiStore.login(SUPER_USER, SUPER_USER_LOGIN_PW);
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login(SUPER_USER, SUPER_USER_LOGIN_PW);
        apiPublisherAdmin = new APIPublisherRestClient(publisherURL);
        apiPublisherAdmin.login(ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        try {
            apiPublisher.developSampleAPI("swaggerFiles/APIScopeTest1.json",
                    SUPER_USER, backendEndPoint, true, apiVisibility);
        } catch (Exception ex) {
            log.error("API publication failed", ex);
        }

    }

    @Test(description = "3.2.1.1", dataProvider = "ScopeAndValidRoleDataProvider",
            dataProviderClass = SecureUsingUserRolesTestCases.class)
    public void testScopeCreationWithValidValues(String role, String scope) throws Exception {
        HttpResponse httpResponse = apiPublisher.validateScope(scope, role);
        verifyResponse(httpResponse);
        assertEquals(new JSONObject(httpResponse.getData()).get(SCOPE_EXISTANCE).toString(), "false",
                "Error in scope creation with valid values. Scope  : " + scope);
        assertEquals(new JSONObject(httpResponse.getData()).get(ROLE_EXISTANCE).toString(), "true",
                "Error in scope creation with valid values. Role  : " + role);

    }

    @Test(description = "3.2.1.2", dataProvider = "SwaggerFilesAndVerb",
            dataProviderClass = SecureUsingUserRolesTestCases.class)
    public void testScopeAssigningToMultipleResources(String file, ArrayList<String> httpVerbs) throws Exception {
        swaggerFile = new File(resourceLocation + File.separator + "swaggerFiles/"+file);
        String payload = ScenarioTestUtils.readFromFile(swaggerFile.getAbsolutePath());
        HttpResponse updateResponse = apiPublisher.updateResourceOfAPI(SUPER_USER, apiName, apiVersion, payload);
        verifyResponse(updateResponse);
        HttpResponse updatedResponse = apiPublisher.getAPI(apiName, SUPER_USER, apiVersion);
        JSONObject jasonPayload = new JSONObject(updatedResponse.getData());
        for (String httpverb: httpVerbs) {
            String scope = new JSONObject(
                    new JSONObject(
                            (new JSONObject(
                                    new JSONArray(
                                            jasonPayload.getJSONObject("api").
                                                    get("resources").toString()
                                    ).get(0).toString()
                            ).get("http_verbs")
                            ).toString()
                    ).get(httpverb).toString()
            ).get("scope").toString();
            assertEquals(ITEM_VIEW, scope);
        }
    }

    @Test(description = "3.2.1.3", dataProvider = "StoreUserDataProvider",
            dataProviderClass = SecureUsingUserRolesTestCases.class)
    public void testScopeWithMultipleRoles(String user, String password) throws Exception {
        //Adding a scope with two roles
        HttpResponse httpResponse = apiPublisher.validateScope(ORDER_VIEW, AGENT_ROLE + "," + CUSTOMER_ROLE);
        verifyResponse(httpResponse);

        //Updating the resource with the new scope
        swaggerFile = new File(resourceLocation + File.separator + "swaggerFiles/APIScopeTest3.json");
        String payload = ScenarioTestUtils.readFromFile(swaggerFile.getAbsolutePath());
        HttpResponse updateResponse = apiPublisher.updateResourceOfAPI(SUPER_USER, apiName, apiVersion, payload);
        verifyResponse(updateResponse);

        createNewApplicationAndSubscribe(user, password);

        String accessToken = generateKeysAndAccessToken(user, password);
        Map<String, String> requestHeaders;
        HttpResponse response;
        requestHeaders = new HashMap<String, String>();

        requestHeaders.put("Authorization", "Bearer " + accessToken);
        response = HttpRequestUtil
                .doGet(gatewayHttpsURL + "/contextJsonV2/" + apiVersion, requestHeaders);
        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Admin user cannot access the GET Method");
        apiStore.removeAPISubscriptionByName(apiName, apiVersion, SUPER_USER, applicationName);
        apiStore.removeApplication(applicationName);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws APIManagerIntegrationTestException {
        for (String name : applicationsList) {
            apiStore.removeApplication(name);
        }
        applicationsList.clear();
        try {
            deleteUsers();
            deleteRoles();
        } catch (APIManagementException ex) {
            log.error("Users or role deletion failed", ex);
        }
        apiPublisherAdmin.deleteAPI(apiName, apiVersion, SUPER_USER);
    }
}
