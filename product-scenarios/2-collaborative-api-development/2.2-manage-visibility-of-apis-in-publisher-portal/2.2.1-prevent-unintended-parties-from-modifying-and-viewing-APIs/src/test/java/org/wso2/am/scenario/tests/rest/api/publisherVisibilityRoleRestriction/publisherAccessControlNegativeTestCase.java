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

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class publisherAccessControlNegativeTestCase extends ScenarioTestBase {

    private String apiProvider;
    private String apiName;
    private String apiContext;
    private String apiVersion;
    private String visibilityType = "publisher";
    private APIPublisherRestClient apiPublisherRestClient;
    private String apiRole;
    private List<Map<String, String>> apisToDeleteList = new ArrayList<>();
    private List<String> rolesToDeleteList = new ArrayList<>();
    private List<String> usersToDeleteList = new ArrayList<>();

    private static final String CREATOR_PUBLISHER_USERNAME = "creatorPublisherUser";
    private static final String TEST_PUBLISHER_USERNAME = "creatorPublisherUser2";
    private static final String PUBLISHER_ROLE = "publisherTestRole";
    private static final String PASSWORD = "Wso2123!";
    private static final String ADMIN_LOGIN_USERNAME = "admin";
    private static final String ADMIN_LOGIN_PW = "admin";
    
    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagementException {

        String apiPublishPermission = "/permission/admin/manage/api/publish";
        String apiCreatePermission = "/permission/admin/manage/api/create";
        String loginPermission = "/permission/admin/login";

        createRole(ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW, PUBLISHER_ROLE, new String[]{apiPublishPermission,
                apiCreatePermission, loginPermission});
        createUser(CREATOR_PUBLISHER_USERNAME, PASSWORD, new String[]{PUBLISHER_ROLE},
                ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        usersToDeleteList.add(CREATOR_PUBLISHER_USERNAME);
        rolesToDeleteList.add(PUBLISHER_ROLE);
        apiPublisherRestClient = new APIPublisherRestClient(publisherURL);
    }
    
    @Test(description = "2.2.1.1")
    private void testAccessControlUsingNoneExistingRoles() throws APIManagerIntegrationTestException {

        apiProvider = CREATOR_PUBLISHER_USERNAME;
        apiName = "testAccessControlUsingNoneExistingRoles";
        apiContext = "/testAccessControlUsingNoneExistingRoles";
        apiRole = "somerole";
        apiVersion = "1.0.0";

        apiPublisherRestClient.login(CREATOR_PUBLISHER_USERNAME, PASSWORD);
        assertTrue(validateRoles(apiRole).getData().contains("false"));
        apiPublisherRestClient.logout();
    }

    @Test(description = "2.2.1.2")
    private void testVisibilityOfRestrictedApi() throws MalformedURLException, APIManagerIntegrationTestException,
            APIManagementException {

        apiProvider = CREATOR_PUBLISHER_USERNAME;
        apiName = "testVisibilityOfRestrictedApi";
        apiContext = "/testVisibilityOfRestrictedApi";
        apiVersion = "1.0.0";
        apiRole = PUBLISHER_ROLE;

        Map<String, String> apiDeleteParams = new HashMap<>();
        apiDeleteParams.put("apiName", apiName);
        apiDeleteParams.put("apiProvider", apiProvider);
        apiDeleteParams.put("apiVersion", apiVersion);
        apisToDeleteList.add(apiDeleteParams);
        apiPublisherRestClient.login(CREATOR_PUBLISHER_USERNAME, PASSWORD);
        verifyResponse(createRestrictedApi(apiPublisherRestClient));
        apiPublisherRestClient.logout();
        createUserWithPublisherAndCreatorRole(TEST_PUBLISHER_USERNAME, PASSWORD, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        usersToDeleteList.add(TEST_PUBLISHER_USERNAME);
        apiPublisherRestClient.login(TEST_PUBLISHER_USERNAME, PASSWORD);
        assertFalse(isApiAvailable(apiName, apiPublisherRestClient));
        apiPublisherRestClient.logout();
    }

    private boolean isApiAvailable(String apiName, APIPublisherRestClient client) throws
            APIManagerIntegrationTestException {

        HttpResponse api = client.getAllAPIs();
        verifyResponse(api);

        return api.getData().contains(apiName);
    }

    private HttpResponse validateRoles(String roles) throws APIManagerIntegrationTestException {

        HttpResponse checkValidationRole = apiPublisherRestClient.validateRoles(roles);
        verifyResponse(checkValidationRole);
        return checkValidationRole;
    }

    private HttpResponse createRestrictedApi(APIPublisherRestClient client) throws MalformedURLException,
            APIManagerIntegrationTestException {

        String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
        String apiResource = "/find";
        String apiVisibility = "restricted";
        String tierCollection = "Silver";

        APIRequest apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiRole, visibilityType,
                apiVersion, apiResource, tierCollection, new URL(backendEndPoint));

        return client.addAPI(apiRequest);
    }

    private HttpResponse deleteApi(String apiName, String apiVersion, String apiProvider,
                                   APIPublisherRestClient client) throws APIManagerIntegrationTestException {

        return client.deleteAPI(apiName, apiVersion, apiProvider);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        apiPublisherRestClient.login(ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        for (Map<String, String> api : apisToDeleteList) {
            deleteApi(api.get("apiName"), api.get("apiVersion"), api.get("apiProvider"), apiPublisherRestClient);
        }

        for (String user : usersToDeleteList) {
            deleteUser(user, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        }

        for (String role : rolesToDeleteList) {
            deleteRole(role, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        }
        apiPublisherRestClient.logout();
    }
}
