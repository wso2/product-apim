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

import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestUtils;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.wso2.am.scenario.test.common.ScenarioTestUtils.readFromFile;

public class SecureUsingUserRolesNegativeTestCases extends ScenarioTestBase {

    private static final Log log = LogFactory.getLog(SecureUsingUserRolesNegativeTestCases.class);
    private APIPublisherRestClient apiPublisher;
    private APIPublisherRestClient apiPublisherAdmin;
    private static final String ADMIN_LOGIN_USERNAME = "admin";
    private static final String ADMIN_LOGIN_PW = "admin";
    private static final String API_ADMIN_PERMISSION = "/permission/admin";
    private static final String API_PUBLISHER_PERMISSION = "/permission/admin/manage/api/publish";
    private static final String API_CREATOR_PERMISSION = "/permission/admin/manage/api/create";
    private static final String MANAGER_ROLE = "managerRole";
    private static final String AGENT_ROLE = "agentRole";
    private static final String SUPER_USER = "Harry";
    private static final String SUPER_USER_LOGIN_PW = "super";
    private static final String ITEM_VIEW = "item_view";
    private static final String ITEM_ADD = "item_add";
    private static final String ORDER_VIEW = "order_view";
    private static final String ORDER_ADD = "order_add";
    private static final String SCOPE_EXISTANCE = "isScopeExist";
    private static final String ROLE_EXISTANCE = "isRoleExist";
    List<String> userList = new ArrayList();
    List<String> roleList = new ArrayList();
    private String apiVersion = "1.0.0";
    private String apiVisibility = "public";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    private String apiName = "APIScopeTestAPI";
    private File swaggerFile;
    String resourceLocation = System.getProperty("test.resource.location");

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
                API_CREATOR_PERMISSION});
        roleList.add(AGENT_ROLE);
    }

    private void createUsers() throws APIManagementException {
        createUser(SUPER_USER, SUPER_USER_LOGIN_PW, new String[]{MANAGER_ROLE, AGENT_ROLE}, ADMIN_LOGIN_USERNAME,
                ADMIN_LOGIN_PW);
        userList.add(SUPER_USER);
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

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException, IOException {
        setupUserData();
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login(SUPER_USER, SUPER_USER_LOGIN_PW);
        apiPublisherAdmin = new APIPublisherRestClient(publisherURL);
        apiPublisherAdmin.login(ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        // create and publish sample API
        String swaggerFilePath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "test" +
                File.separator + "resources" + File.separator + "swaggerFiles" +
                File.separator + "APIScopeTest1.json";
        File swaggerFile = new File(swaggerFilePath);
        String swaggerContent = readFromFile(swaggerFile.getAbsolutePath());
        JSONObject swaggerJson = new JSONObject(swaggerContent);
        try {
            apiPublisher.developSampleAPI(swaggerJson, SUPER_USER, backendEndPoint, true, apiVisibility);
        } catch (Exception ex) {
            log.error("API publication failed", ex);
        }
    }

    @Test(description = "3.2.1.9", dataProvider = "ScopeAndInValidRoleDataProvider",
            dataProviderClass = SecureUsingUserRolesNegativeTestCases.class)
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
