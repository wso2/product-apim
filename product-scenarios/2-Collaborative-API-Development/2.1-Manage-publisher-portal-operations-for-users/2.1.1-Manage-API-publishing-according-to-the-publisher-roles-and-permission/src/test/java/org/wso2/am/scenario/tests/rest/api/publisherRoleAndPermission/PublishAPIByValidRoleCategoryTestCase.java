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
package org.wso2.am.scenario.tests.rest.api.publisherRoleAndPermission;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.scenario.test.common.APIRequest;

import java.net.URL;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class PublishAPIByValidRoleCategoryTestCase extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private APIRequest apiRequest;

    private String apiName;
    private String apiContext = "/verify";
    private String apiResource = "/find";
    private String apiVisibility = "public";
    private String apiVersion = "1.0.0";
    private String tierCollection = "Gold,Bronze";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    private String creatorUser;
    private String testUser;
    private String password;
    private int count = 0;

    private static final Log log = LogFactory.getLog(PublishAPIByValidRoleCategoryTestCase.class);
    private final String ADMIN_LOGIN_USERNAME = "admin";
    private final String ADMIN_PASSWORD = "admin";
    Map<String, String> apiNames = new HashMap<>();
    List<String> userList = new ArrayList();
    List<String> roleList = new ArrayList();

    String userRole;

    @BeforeClass(alwaysRun = true)
    public void init() {
        apiPublisher = new APIPublisherRestClient(publisherURL);
    }

    @Test(description = "2.1.1.1", dataProvider = "RoleValidationDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testPublishAPIByValidRoleAssignedUser(String creatorRole, String role) throws Exception {

        String[] roleList;
        roleList = role.split(",");
        apiName = "API" + count;
        apiContext = "/verify" + count;
        creatorUser = "User_" + count;
        testUser = "User" + count;
        password = "password123$";
        count++;

        createUser(creatorUser, password, new String[]{creatorRole}, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        apiPublisher.login(creatorUser, password);

        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource, tierCollection,
                new URL(backendEndPoint));
        createAPI(apiRequest);
        getAPI(apiName, creatorUser, apiVersion);
        apiNames.put(apiName, creatorUser);

        apiPublisher.logout();
        createUser(testUser, password, roleList, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        userList.add(testUser);
        apiPublisher.login(testUser, password);

        HttpResponse publishAPI = changeAPILifeCycleStatus(apiName, creatorUser, APILifeCycleState.PUBLISHED);
        verifyResponse(publishAPI);
        assertTrue(publishAPI.getData().contains("PUBLISHED"),
                "API has not been published using " + role);
    }

    @Test(description = "2.1.1.2", dataProvider = "PermissionValidationDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testPublishAPIByValidPermissionUser(String[] permissionList) throws Exception {

        apiName = "API_" + count;
        apiContext = "/verify_" + count;
        creatorUser = "User_" + count;
        userRole = "role" + count;
        testUser = "User" + count;
        password = "password123$";
        count++;

        createUserWithCreatorRole(creatorUser, password, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        apiPublisher.login(creatorUser, password);

        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource, tierCollection,
                new URL(backendEndPoint));
        createAPI(apiRequest);
        getAPI(apiName, creatorUser, apiVersion);
        apiNames.put(apiName, creatorUser);

        createRole(ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD, userRole, permissionList);
        roleList.add(userRole);

        createUser(testUser, password, new String[]{userRole}, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        userList.add(testUser);
        apiPublisher.logout();

        apiPublisher.login(testUser, password);
        HttpResponse publishAPI = changeAPILifeCycleStatus(apiName, creatorUser, APILifeCycleState.PUBLISHED);
        verifyResponse(publishAPI);
        assertTrue(publishAPI.getData().contains("PUBLISHED"),
                "API has not been published");
    }

    @Test(description = "2.1.1.3", dataProvider = "RoleUpdatingDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testPublishAPIByUpdatingRoleInUser(String role) throws Exception {

        apiName = "API" + count;
        apiContext = "/verify" + count;
        creatorUser = "User_" + count;
        testUser = "User" + count;
        password = "password123$";
        count++;

        createUserWithCreatorRole(creatorUser, password, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        apiPublisher.login(creatorUser, password);
        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource, tierCollection,
                new URL(backendEndPoint));
        createAPI(apiRequest);
        getAPI(apiName, creatorUser, apiVersion);
        apiNames.put(apiName, creatorUser);
        userList.add(testUser);
        apiPublisher.logout();

        createUser(testUser, password, new String[]{role}, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        apiPublisher.login(testUser, password);
        HttpResponse publishAPI = changeAPILifeCycleStatus(apiName, creatorUser, APILifeCycleState.PUBLISHED);
        assertFalse(publishAPI.getData().contains("PUBLISHED"), "API has been published");

        updateUser(testUser, "internal/publisher", role, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        apiPublisher.login(testUser, password);
        HttpResponse publisherPublishAPI = changeAPILifeCycleStatus(apiName, creatorUser, APILifeCycleState.PUBLISHED);
        verifyResponse(publisherPublishAPI);
        assertTrue(publisherPublishAPI.getData().contains("PUBLISHED"),
                "API has not been published by internal/publisher");

        HttpResponse changeAPIStatus = changeAPILifeCycleStatus(apiName, creatorUser, APILifeCycleState.CREATED);
        verifyResponse(changeAPIStatus);
        assertTrue(changeAPIStatus.getData().contains("CREATED"));

        updateUser(testUser, "admin", role, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        apiPublisher.login(testUser, password);
        HttpResponse adminChangeAPIStatus = changeAPILifeCycleStatus(apiName, creatorUser, APILifeCycleState.PUBLISHED);
        verifyResponse(adminChangeAPIStatus);
        assertTrue(adminChangeAPIStatus.getData().contains("PUBLISHED"),
                "API has not been published by admin");
    }

    public void createAPI(APIRequest apiRequest) throws Exception {

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiRequest);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        verifyResponse(apiCreationResponse);
    }

    public void getAPI(String apiName, String username, String apiVersion) throws Exception {

        HttpResponse apiResponsePublisher = apiPublisher.getAPI(apiName, username, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(apiName), apiName + " is not visible in publisher");
        verifyResponse(apiResponsePublisher);
    }

    public HttpResponse changeAPILifeCycleStatus(String apiName, String username, APILifeCycleState apiLifeCycleState)
            throws Exception {

        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, username, apiLifeCycleState);
        HttpResponse apiResponsePublishAPI = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        return apiResponsePublishAPI;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        for (Map.Entry<String, String> entry : apiNames.entrySet()) {
            String apiName = entry.getKey();
            String provider = entry.getValue();
            apiPublisher.deleteAPI(apiName, apiVersion, provider);
            deleteUser(provider, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        }

        for (String username : userList) {
            deleteUser(username, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        }

        if (roleList.size() > 0) {
            for (String role : roleList) {
                deleteRole(role, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
            }
        }
    }
}
