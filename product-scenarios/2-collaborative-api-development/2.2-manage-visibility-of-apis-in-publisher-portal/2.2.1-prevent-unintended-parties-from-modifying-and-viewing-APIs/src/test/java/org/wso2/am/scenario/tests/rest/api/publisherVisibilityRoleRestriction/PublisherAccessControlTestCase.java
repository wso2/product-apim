/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.scenario.tests.rest.api.publisherVisibilityRoleRestriction;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertTrue;

public class PublisherAccessControlTestCase extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private String apiName;
    private String apiContext;
    private String testUser;
    private String creatorUsername;
    private String testUsername;
    private String adminUsername;
    private String roleSet;
    private String creator = "creator";
    private String password = "password123$";
    private String publisherRole = "publisherRole";
    private String creatorRole = "creatorRole";
    private String apiVersion = "1.0.0";
    private String apiResource = "/find";
    private String apiVisibility = "restricted";
    private String tierCollection = "Gold,Bronze";
    private String visibilityType = "publisher";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    private String[] permissionArray = new String[]{"/permission/admin/login",
            "/permission/admin/manage/api/publish"};

    private static final String ADMIN_LOGIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String TENANT_LOGIN_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_USER = "tenantUser";

    Map<String, String> apiNames = new HashMap<>();
    Set<String> userSet = new HashSet<>();
    List<String> roleList = new ArrayList();
    private int count = 0;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        apiPublisher = new APIPublisherRestClient(publisherURL);
        createUsers();
    }

    @Test(description = "2.2.1.1", dataProvider = "UserTypeDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testVisibilityOfAPIsInPublisherRestrictedByRoles(String userType, String role) throws Exception {

        apiName = "API__" + count;
        apiContext = "/check" + count;
        count++;

        if (role.equals(ADMIN_LOGIN_USERNAME)) {
            testUser = "adminUser1";
        } else {
            testUser = "publisherUser1";
            role = publisherRole;
        }

        if (userType.equals(TENANT_USER)) {
            adminUsername = TENANT_LOGIN_ADMIN_USERNAME;
            creatorUsername = creator + "@" + ScenarioTestConstants.TENANT_WSO2;
            testUsername = testUser + "@" + ScenarioTestConstants.TENANT_WSO2;
        } else {
            adminUsername = ADMIN_LOGIN_USERNAME;
            creatorUsername = creator;
            testUsername = testUser;
        }

        APIRequest apiRequest = new APIRequest(apiName, apiContext, apiVisibility, publisherRole, visibilityType,
                apiVersion, apiResource, tierCollection, new URL(backendEndPoint));
        apiPublisher.login(creatorUsername, password);
        createAPI(apiRequest);
        createUser(testUser, password, new String[]{role}, adminUsername, ADMIN_PASSWORD);
        apiPublisher.logout();
        apiPublisher.login(testUsername, password);
        getAPI(apiName, creatorUsername);
        apiNames.put(apiName, creatorUsername);
        userSet.add(testUser);
    }

    @Test(description = "2.2.1.2", dataProvider = "UserTypeDataProvider", dataProviderClass = ScenarioDataProvider.class)
    public void testVisibilityOfAPIsPublisherRestrictedByMultipleRoles(String userType, String role) throws Exception {

        testUser = "MultipleRoleUser";
        apiName = "RestAPI1" + count;
        apiContext = "/Add" + count;
        count++;

        if (role.equals(ADMIN_LOGIN_USERNAME)) {
            testUser = "adminUser2";
        } else {
            testUser = "publisherUser2";
            role = publisherRole;
        }

        if (userType.equals(TENANT_USER)) {
            adminUsername = TENANT_LOGIN_ADMIN_USERNAME;
            creatorUsername = creator + "@" + ScenarioTestConstants.TENANT_WSO2;
            testUsername = testUser + "@" + ScenarioTestConstants.TENANT_WSO2;
        } else {
            adminUsername = ADMIN_LOGIN_USERNAME;
            creatorUsername = creator;
            testUsername = testUser;
        }

        String multipleRoles = publisherRole + "," + creatorRole;
        APIRequest apiRequest = new APIRequest(apiName, apiContext, apiVisibility, multipleRoles, visibilityType,
                apiVersion, apiResource, tierCollection, new URL(backendEndPoint));

        apiPublisher.login(creatorUsername, password);
        validateRoles(multipleRoles);
        createAPI(apiRequest);
        createUser(testUser, password, new String[]{role}, adminUsername, ADMIN_PASSWORD);
        apiPublisher.logout();
        apiPublisher.login(testUsername, password);
        getAPI(apiName, creatorUsername);
        apiNames.put(apiName, creatorUsername);
        userSet.add(testUser);
    }

    @Test(description = "2.2.1.3")
    public void testVisibilityInPublisherRestrictedByRolesWithSpace() throws Exception {

        apiName = "API";
        apiContext = "/verify";
        testUser = "testUser__1";
        roleSet = "creator Role, publisher Role";

        createUser(testUser, password, new String[]{publisherRole, creatorRole}, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        APIRequest apiRequest = new APIRequest(apiName, apiContext, apiVisibility, roleSet, visibilityType,
                apiVersion, apiResource, tierCollection, new URL(backendEndPoint));

        apiPublisher.login(creator, password);
        createAPI(apiRequest);
        apiPublisher.logout();
        apiPublisher.login(testUser, password);
        getAPI(apiName, creator);
        apiNames.put(apiName, creator);
        userSet.add(testUser);
    }

    @Test(description = "2.2.1.4")
    public void testCreateAPIsInPublisherRestrictedByRoles() throws Exception {

        apiName = "API-X";
        apiContext = "/check";

        APIRequest apiRequest = new APIRequest(apiName, apiContext, apiVisibility, publisherRole, visibilityType,
                apiVersion, apiResource, tierCollection, new URL(backendEndPoint));
        apiPublisher.login(creator, password);
        createAPI(apiRequest);
        getAPI(apiName, creator);
        apiNames.put(apiName, creator);
    }

    public void createUsers() throws Exception {

        createRole(ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD, publisherRole, permissionArray);
        createRole(ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD, creatorRole, permissionArray);
        createUser(creator, password, new String[]{ScenarioTestConstants.CREATOR_ROLE, publisherRole, creatorRole}, ADMIN_LOGIN_USERNAME,
                ADMIN_PASSWORD);

        addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        createRole(TENANT_LOGIN_ADMIN_USERNAME, ADMIN_PASSWORD, publisherRole, permissionArray);
        createRole(TENANT_LOGIN_ADMIN_USERNAME, ADMIN_PASSWORD, creatorRole, permissionArray);
        createUser(creator, password, new String[]{ScenarioTestConstants.CREATOR_ROLE, publisherRole, creatorRole},
                TENANT_LOGIN_ADMIN_USERNAME, ADMIN_PASSWORD);

        userSet.add(creator);
        roleList.add(publisherRole);
        roleList.add(creatorRole);
    }

    private void validateRoles(String roles) throws APIManagerIntegrationTestException {

        HttpResponse checkValidationRole = apiPublisher.validateRoles(roles);
        verifyResponse(checkValidationRole);
        assertTrue(checkValidationRole.getData().contains("true"));
    }

    private void createAPI(APIRequest apiCreationRequest) throws APIManagerIntegrationTestException {

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequest);
        verifyResponse(apiCreationResponse);
    }

    public void getAPI(String apiName, String provider) throws APIManagerIntegrationTestException {

        HttpResponse apiResponseGetAPI = apiPublisher.getAPI(apiName, provider, apiVersion);
        verifyResponse(apiResponseGetAPI);
        assertTrue(apiResponseGetAPI.getData().contains(apiName), apiName + " is not visible in publisher");
    }

    @AfterTest(alwaysRun = true)
    public void destroy() throws Exception {

        for (Map.Entry<String, String> entry : apiNames.entrySet()) {
            String apiName = entry.getKey();
            String provider = entry.getValue();
            apiPublisher.login(provider, password);
            apiPublisher.deleteAPI(apiName, apiVersion, provider);
        }

        if (roleList.size() > 0) {
            for (String role : roleList) {
                deleteRole(role, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
                deleteRole(role, TENANT_LOGIN_ADMIN_USERNAME, ADMIN_PASSWORD);
            }
        }

        for (String username : userSet) {
            if (!username.equals("testUser__1")) {
                deleteUser(username, TENANT_LOGIN_ADMIN_USERNAME, ADMIN_PASSWORD);
            }
            deleteUser(username, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        }
    }
}