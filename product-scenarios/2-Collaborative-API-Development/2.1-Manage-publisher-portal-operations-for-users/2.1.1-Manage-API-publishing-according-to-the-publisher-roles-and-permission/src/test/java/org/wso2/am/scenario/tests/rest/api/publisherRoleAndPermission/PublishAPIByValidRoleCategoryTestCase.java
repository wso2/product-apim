/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.scenario.test.common.APIRequest;

import java.net.URL;
import java.util.Properties;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import java.util.*;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class PublishAPIByValidRoleCategoryTestCase extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private String publisherURL;
    private String storeURL;
    private APIRequest apiRequest;
    private Properties infraProperties;

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

    private static final long WAIT_TIME = 3 * 1000;
    private APIStoreRestClient apiStoreClient;

    @BeforeClass(alwaysRun = true)
    public void init(){

        infraProperties = getDeploymentProperties();
        publisherURL = infraProperties.getProperty(PUBLISHER_URL);
        storeURL = infraProperties.getProperty(STORE_URL);

        if (publisherURL == null) {
            publisherURL = "https://localhost:9443/publisher";
        }

        if (storeURL == null) {
            storeURL = "https://localhost:9443/store";
        }

        setKeyStoreProperties();
        apiPublisher = new APIPublisherRestClient(publisherURL);
    }

    @Test(description = "2.1.1.1", dataProvider = "RoleValidationDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testPublishAPIByValidRoleAssignedUser(String creatorRole, String role) throws Exception {

        apiName = "API" + count;
        apiContext = "/verify" + count;
        creatorUser = "User_" + count;
        testUser = "User" + count;
        password = "password123$";
        count++;
        String[] roleList;
        roleList = role.split(",");

        createUser(creatorUser, password,new String[] { creatorRole }, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        apiPublisher.login(creatorUser, password);

        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource, tierCollection,
                new URL(backendEndPoint));

        apiNames.put(apiName,creatorUser);
        userList.add(testUser);

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiRequest);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        verifyResponse(apiCreationResponse);
        HttpResponse apiResponsePublisher = apiPublisher.getAPI(apiName, creatorUser, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(apiName), apiName + " is not visible in publisher");
        verifyResponse(apiResponsePublisher);

        apiPublisher.logout();
        createUser(testUser, password, roleList , ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        apiPublisher.login(testUser, password);

        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, creatorUser, APILifeCycleState.PUBLISHED);
        HttpResponse apiResponsePublishedAPI = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(apiResponsePublishedAPI);
        assertTrue(apiResponsePublishedAPI.getData().contains("PUBLISHED"),
                "API has not been created in publisher");
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

        apiNames.put(apiName,creatorUser);
        userList.add(testUser);

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiRequest);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        verifyResponse(apiCreationResponse);
        HttpResponse apiResponsePublisher = apiPublisher.getAPI(apiName, creatorUser, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(apiName), apiName + " is not visible in publisher");
        verifyResponse(apiResponsePublisher);


        createRole(ADMIN_LOGIN_USERNAME,ADMIN_PASSWORD,userRole,permissionList);
        roleList.add(userRole);

        createUser(testUser, password,new String[] {userRole},ADMIN_LOGIN_USERNAME,ADMIN_PASSWORD);

        apiPublisher.logout();
        apiPublisher.login(testUser, password);

        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, creatorUser, APILifeCycleState.PUBLISHED);
        HttpResponse apiResponsePublishedAPI = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(apiResponsePublishedAPI);
        assertTrue(apiResponsePublishedAPI.getData().contains("PUBLISHED"),
                "API has not been created in publisher");
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

        apiNames.put(apiName,creatorUser);
        userList.add(testUser);

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiRequest);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        verifyResponse(apiCreationResponse);
        HttpResponse apiResponsePublisher = apiPublisher.getAPI(apiName, creatorUser, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(apiName), apiName + " is not visible in publisher");
        verifyResponse(apiResponsePublisher);

        apiPublisher.logout();
        createUser(testUser, password, new String[] { role } , ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        apiPublisher.login(testUser, password);

        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, creatorUser, APILifeCycleState.PUBLISHED);
        HttpResponse apiResponsePublishedAPI = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        assertFalse(apiResponsePublishedAPI.getData().contains("PUBLISHED"),
                "API has been created in publisher");

        updateUser(testUser,"internal/publisher", role, ADMIN_LOGIN_USERNAME,ADMIN_PASSWORD);
        apiPublisher.login(testUser, password);
        APILifeCycleStateRequest updatePublishRequest1 =
                new APILifeCycleStateRequest(apiName, creatorUser, APILifeCycleState.PUBLISHED);
        HttpResponse updatePublishResponse1 = apiPublisher.changeAPILifeCycleStatus(updatePublishRequest1);
        verifyResponse(updatePublishResponse1);
        assertTrue(updatePublishResponse1.getData().contains("PUBLISHED"),
                "API has been created in publisher");

        APILifeCycleStateRequest updatePublishRequest =
                new APILifeCycleStateRequest(apiName, creatorUser, APILifeCycleState.CREATED);
        HttpResponse updatePublishResponse = apiPublisher.changeAPILifeCycleStatus(updatePublishRequest);
        verifyResponse(updatePublishResponse);
        assertTrue(updatePublishResponse.getData().contains("CREATED"),
                "API has been created in publisher");

        updateUser(testUser,"admin", role, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        apiPublisher.login(testUser, password);
        APILifeCycleStateRequest updatePublishRequest2 =
                new APILifeCycleStateRequest(apiName, creatorUser, APILifeCycleState.PUBLISHED);
        HttpResponse updatePublishResponse2 = apiPublisher.changeAPILifeCycleStatus(updatePublishRequest2);
        verifyResponse(updatePublishResponse2);
        assertTrue(updatePublishResponse2.getData().contains("PUBLISHED"),
                "API has been created in publisher");

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
            deleteUser(username,ADMIN_LOGIN_USERNAME,ADMIN_PASSWORD);
        }

        if (roleList.size() > 0) {
            for (String username : userList) {
                deleteUser(username, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
            }
        }
    }
}
