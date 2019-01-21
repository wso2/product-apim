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
package org.wso2.am.scenario.tests.rest.api.publisherRoleAndPermission;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.scenario.test.common.APILifeCycleStateRequest;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class PublishAPIByValidRolePermissionCategoryNegativeTestCase extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStoreClient;

    private APIRequest apiRequest;

    private String apiName;
    private String apiContext = "/verify";
    private String apiResource = "/find";
    private String apiVisibility = "public";
    private String apiVersion = "1.0.0";
    private String tierCollection = "Gold,Bronze";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    private String developer;
    private String testUser;
    private String password;
    private int count = 0;

    private static final Log log = LogFactory.getLog(PublishAPIByValidRolePermissionCategoryTestCase.class);
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

    @Test(description = "2.1.1.1", dataProvider = "ApiStateAndInvalidRoleDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testPublishAPIByInvalidRoleAssignedUser(String role, String state) throws Exception {

        apiName = "API" + count;
        apiContext = "/verify" + count;
        developer = "User_" + count;
        testUser = "User" + count;
        password = "password123$";
        count++;

        createUserWithPublisherAndCreatorRole(developer, password, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        apiPublisher.login(developer, password);

        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource, tierCollection,
                new URL(backendEndPoint));
        createAPI(apiRequest);
        getAPI(apiName, developer, apiVersion);
        apiNames.put(apiName, developer);

        createUser(testUser, password, new String[]{role}, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        userList.add(testUser);

        checkPublishAPI(state, testUser, developer, role);
        updateUser(developer, new String[]{"internal/subscriber"}, null, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        loginToStore(developer, password);
        isAPINotVisibleInStore(apiName, apiStoreClient);
    }

    @Test(description = "2.1.1.2", dataProvider = "ApiInvalidPermissionDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testPublishAPIByInvalidPermissionUser(String[] permissionList) throws Exception {

        apiName = "API_" + count;
        apiContext = "/verify_" + count;
        developer = "User_" + count;
        userRole = "role" + count;
        testUser = "User" + count;
        password = "password123$";
        count++;

        createUserWithPublisherAndCreatorRole(developer, password, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        apiPublisher.login(developer, password);

        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource, tierCollection,
                new URL(backendEndPoint));
        createAPI(apiRequest);
        getAPI(apiName, developer, apiVersion);
        apiNames.put(apiName, developer);

        createRole(ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD, userRole, permissionList);
        roleList.add(userRole);

        createUser(testUser, password, new String[]{userRole}, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        userList.add(testUser);

        apiPublisher.logout();
        apiPublisher.login(testUser, password);
        publishAPI(apiName, developer, userRole);

        updateUser(developer, new String[]{"internal/subscriber"}, null, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        loginToStore(developer, password);
        isAPINotVisibleInStore(apiName, apiStoreClient);
    }

    private void loginToStore(String userName, String password) throws Exception {

        setKeyStoreProperties();
        apiStoreClient = new APIStoreRestClient(storeURL);
        apiStoreClient.login(userName, password);
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

        org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest updateRequest =
                new org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest(apiName, username, apiLifeCycleState);
        HttpResponse apiResponsePublishAPI = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        return apiResponsePublishAPI;
    }

    public void publishAPI(String apiName, String creatorUser, String role)
            throws Exception {

        HttpResponse publishCreatedAPI = changeAPILifeCycleStatus(apiName, creatorUser, APILifeCycleState.PUBLISHED);
        assertFalse(publishCreatedAPI.getData().contains("PUBLISHED"),
                "API has been published using " + role);
    }

    public void rePublishAPI(String apiName, String creatorUser, String role)
            throws Exception {

        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, creatorUser, "Re-Publish");
        HttpResponse apiResponsePublishAPI = apiPublisher.changeAPILifeCycleStatusByAction(updateRequest);
        assertFalse(apiResponsePublishAPI.getData().contains("PUBLISHED"),
                "API has not been published using " + role);
    }

    public void checkPublishAPI(String state, String testUser, String creatorUser, String role) throws Exception {

        if (state == APILifeCycleState.CREATED.toString()) {
            apiPublisher.logout();
            apiPublisher.login(testUser, password);
            publishAPI(apiName, creatorUser, role);

        } else if (state == APILifeCycleState.PROTOTYPED.toString()) {
            HttpResponse publishAPI = changeAPILifeCycleStatus(apiName, creatorUser, APILifeCycleState.PROTOTYPED);
            assertTrue(publishAPI.getData().contains("PROTOTYPED"), "API has not been prototyped");

            apiPublisher.logout();
            apiPublisher.login(testUser, password);
            publishAPI(apiName, creatorUser, role);

        } else if (state == APILifeCycleState.BLOCKED.toString()) {
            HttpResponse publishCreatedAPI = changeAPILifeCycleStatus(apiName, creatorUser, APILifeCycleState.PUBLISHED);
            assertTrue(publishCreatedAPI.getData().contains("PUBLISHED"), "API has not been published");

            HttpResponse blockAPI = changeAPILifeCycleStatus(apiName, creatorUser, APILifeCycleState.BLOCKED);
            assertTrue(blockAPI.getData().contains("BLOCKED"), "API has not been blocked");

            apiPublisher.logout();
            apiPublisher.login(testUser, password);
            rePublishAPI(apiName, creatorUser, role);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        for (Map.Entry<String, String> entry : apiNames.entrySet()) {
            String apiName = entry.getKey();
            String provider = entry.getValue();
            apiPublisher.login(provider, password);
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
