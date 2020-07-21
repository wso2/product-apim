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
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class PublishAPIByValidRolePermissionCategoryNegativeTestCase extends ScenarioTestBase {

    private String publisherUser;
    private String apiName;
    private String apiContext;
    private String userRole;
    private String testUser;
    private String creatorUser;
    private String apiVersion = "1.0.0";
    private String password = "password123$";
    private String tierCollection = "Gold,Bronze";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    private int count = 0;
    private static String apiID;

    private static final Log log = LogFactory.getLog(PublishAPIByValidRolePermissionCategoryTestCase.class);
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";

    @Factory(dataProvider = "userModeDataProvider")
    public PublishAPIByValidRolePermissionCategoryNegativeTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                    ADMIN_USERNAME, ADMIN_PW);
            createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, ADMIN_USERNAME, ADMIN_PW);
            publisherUser = "adminUser1";
        }

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // create user in wso2.com tenant
            addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_USERNAME, ADMIN_PW);
            if (isActivated(ScenarioTestConstants.TENANT_WSO2)) {
//                Add and activate wso2.com tenant
                createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                        TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, TENANT_ADMIN_USERNAME,
                        TENANT_ADMIN_PW);
            }
            publisherUser = "adminUser1" + "@" + ScenarioTestConstants.TENANT_WSO2;
        }
        super.init(userMode);
    }

    //TODO : Investigate test failures and fix
    @Test(description = "2.1.1.1", dataProvider = "testinvalidRoleDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testPublishAPIByValidRoleAssignedUser(String role) throws Exception {

        boolean apiIsVisible = false;
        apiName = "API_" + count;
        apiContext = "/verify" + count;
        publisherUser = publisherUser + count;
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseGetAPI = null;
        count++;

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion, API_CREATOR_PUBLISHER_USERNAME, new URL(backendEndPoint));
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);

        apiID = apiDto.getId();
        assertNotNull(apiDto.getId(), "API creation fails");

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUser(publisherUser, password, new String[]{role},
                    ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createUser(publisherUser, password, new String[]{role},
                    TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

        RestAPIPublisherImpl restAPIPublisherNew;
        restAPIPublisherNew = new RestAPIPublisherImpl(publisherUser, "password123$", publisherContext.getContextTenant().getDomain(), publisherURLHttps);

        try {
            restAPIPublisherNew.changeAPILifeCycleStatus(apiID, APILifeCycleAction.PUBLISH.getAction());
        } catch (ApiException e) {
            e.printStackTrace();
        }
        RestAPIStoreImpl restAPIStoreImplNew;
        restAPIStoreImplNew = new RestAPIStoreImpl(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, publisherContext.getContextTenant().getDomain(), storeURLHttps);

        try {
            apiResponseGetAPI = restAPIStoreImplNew.getAPI(apiDto.getId());
            if (apiResponseGetAPI.getName().contains(apiName)) {
                apiIsVisible = true;
            }
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            e.printStackTrace();
        }
        assertFalse(apiIsVisible, apiName + " is visible in dev-Portal");


        restAPIPublisher.deleteAPI(apiID);

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(publisherUser, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(publisherUser, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }
    }

    // Investigate test failures and fix
    @Test(description = "2.1.1.2", dataProvider = "ApiInvalidPermissionDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testPublishAPIByInvalidPermissionUser(String[] permissionList) throws Exception {

        apiName = "API_1.2_" + count;
        apiContext = "/verify_1.2_" + count;
        publisherUser = publisherUser + count;
        userRole = "testrole" + count;
        boolean apiIsVisible = false;
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseGetAPI = null;
        count++;

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            apiName = apiName + "tenant";
            apiContext = apiContext + "tenant";
            userRole = userRole + "tenant";
        }
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion, API_CREATOR_PUBLISHER_USERNAME, new URL(backendEndPoint));
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);

        apiID = apiDto.getId();
        assertNotNull(apiDto.getId(), "API creation fails");

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createRole(ADMIN_USERNAME, ADMIN_PW, userRole, permissionList);
            createUser(publisherUser, "password123$", new String[]{userRole},
                    ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createRole(ADMIN_USERNAME, ADMIN_PW, userRole, permissionList);
            createUser(publisherUser, password, new String[]{userRole}, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

        RestAPIPublisherImpl restAPIPublisherNew;
        restAPIPublisherNew = new RestAPIPublisherImpl(publisherUser, "password123$", publisherContext.getContextTenant().getDomain(), publisherURLHttps);


        try {
            ApiResponse<WorkflowResponseDTO> workflowResponseDTOApiResponse = restAPIPublisherNew.apiLifecycleApi
                    .apisChangeLifecyclePostWithHttpInfo(APILifeCycleAction.PUBLISH.getAction(), apiID, null, null);
            Assert.assertEquals(HttpStatus.SC_OK, workflowResponseDTOApiResponse.getStatusCode());
        } catch (ApiException e) {
            e.printStackTrace();
        }

        RestAPIStoreImpl restAPIStoreImplNew;
        restAPIStoreImplNew = new RestAPIStoreImpl(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, publisherContext.getContextTenant().getDomain(), storeURLHttps);

        try {
            apiResponseGetAPI = restAPIStoreImplNew.getAPI(apiDto.getId());
            if (apiResponseGetAPI.getName().contains(apiName)) {
                apiIsVisible = true;
            }
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            e.printStackTrace();
        }
        assertFalse(apiIsVisible, apiName + " is visible in dev-Portal");

        restAPIPublisher.deleteAPI(apiID);

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(publisherUser, ADMIN_USERNAME, ADMIN_PW);
            deleteRole(userRole, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(publisherUser, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deleteRole(userRole, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }
    }

    // Investigate test failures and fix
    @Test(description = "2.1.1.3", dataProvider = "RoleUpdatingDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testPublishAPIByUpdatingRoleInUser(String validRole, String inValidRole) throws Exception {

        apiName = "API_1.3_" + count;
        apiContext = "/verify_1.3_" + count;
        creatorUser = "User_updateRole" + count;
        testUser = "User1_updateRole" + count;
        userRole = "role" + count;
        boolean apiIsVisible = false;
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseGetAPI = null;
        count++;

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            apiName = apiName + "tenant";
        }
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion, API_CREATOR_PUBLISHER_USERNAME, new URL(backendEndPoint));
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);

        apiID = apiDto.getId();
        assertNotNull(apiDto.getId(), "API creation fails");

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUser(testUser, "password123$", new String[]{validRole},
                    ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createUser(testUser, password, new String[]{validRole}, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

        RestAPIPublisherImpl restAPIPublisherNew;
        restAPIPublisherNew = new RestAPIPublisherImpl(testUser, "password123$", publisherContext.getContextTenant().getDomain(), publisherURLHttps);

        restAPIPublisherNew.changeAPILifeCycleStatusToPublish(apiID, false);
        assertTrue(restAPIPublisherNew.getLifecycleStatus(apiID).getData().contains("Published"), "API has not been published by internal/publisher");

        restAPIPublisherNew.changeAPILifeCycleStatus(apiID, APILifeCycleAction.DEMOTE_TO_CREATE.getAction());

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            updateUser(testUser, new String[]{inValidRole}, new String[]{validRole}, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            updateUser(testUser, new String[]{inValidRole}, new String[]{validRole}, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

        try {
            ApiResponse<WorkflowResponseDTO> workflowResponseDTOApiResponse = restAPIPublisherNew.apiLifecycleApi
                    .apisChangeLifecyclePostWithHttpInfo(APILifeCycleAction.PUBLISH.getAction(), apiID, null, null);
            Assert.assertEquals(HttpStatus.SC_OK, workflowResponseDTOApiResponse.getStatusCode());
        } catch (ApiException e) {
            e.printStackTrace();
        }
        assertFalse(restAPIPublisherNew.getLifecycleStatus(apiID).getData().contains("Published"), "API has been published by internal/publisher");

        restAPIPublisher.deleteAPI(apiID);

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(testUser, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(testUser, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }
    }

    //TODO : Investigate test failures and fix
    @Test(description = "2.1.1.4", dataProvider = "permissionUpdatingDataProvider",
            dataProviderClass = ScenarioDataProvider.class, enabled = false)
    public void testPublishAPIByUpdatingPermissionInUser(String[] validPermissionList, String[] inValidPermissionList) throws Exception {

        apiName = "API_1.4_" + count;
        apiContext = "/verify_1.4_" + count;
        testUser = "User1_updateRole" + count;
        userRole = "role" + count;
        boolean apiIsVisible = false;
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseGetAPI = null;
        count++;

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            apiName = apiName + "tenant";
        }
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion, API_CREATOR_PUBLISHER_USERNAME, new URL(backendEndPoint));
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);

        apiID = apiDto.getId();
        assertNotNull(apiDto.getId(), "API creation fails");

        String[] permissionArray = new String[]{"/permission/admin/login", "/permission/admin/manage/api/publish"};
        String[] InvalidPermissionArray = new String[]{"/permission/admin/login", "/permission/admin/manage/api/create"};

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createRole(ADMIN_USERNAME, ADMIN_PW, userRole, validPermissionList);
            createUser(testUser, password, new String[]{userRole}, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createRole(TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW, userRole, validPermissionList);
            createUser(testUser, password, new String[]{userRole}, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

        RestAPIPublisherImpl restAPIPublisherNew;
        restAPIPublisherNew = new RestAPIPublisherImpl(testUser, "password123$", publisherContext.getContextTenant().getDomain(), publisherURLHttps);

        restAPIPublisher.changeAPILifeCycleStatusToPublish(apiID, false);
        assertTrue(restAPIPublisher.getLifecycleStatus(apiID).getData().contains("Published"), "API has not been published by internal/publisher");

        restAPIPublisher.changeAPILifeCycleStatus(apiID, APILifeCycleAction.DEMOTE_TO_CREATE.getAction());

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            updateRole(ADMIN_USERNAME, ADMIN_PW, userRole, new String[]{testUser}, inValidPermissionList);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            updateRole(TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW, userRole, new String[]{testUser}, inValidPermissionList);
        }

        try {
            ApiResponse<WorkflowResponseDTO> workflowResponseDTOApiResponse = restAPIPublisherNew.apiLifecycleApi
                    .apisChangeLifecyclePostWithHttpInfo(APILifeCycleAction.PUBLISH.getAction(), apiID, null, null);
            Assert.assertEquals(HttpStatus.SC_OK, workflowResponseDTOApiResponse.getStatusCode());
        } catch (ApiException e) {
            e.printStackTrace();
        }
        assertFalse(restAPIPublisher.getLifecycleStatus(apiID).getData().contains("Published"), "API has been published by internal/publisher");

        restAPIPublisher.deleteAPI(apiID);

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(testUser, ADMIN_USERNAME, ADMIN_PW);
            deleteRole(userRole, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(testUser, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deleteRole(userRole, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() throws Exception {
        setup();
        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_USER},
                new Object[]{TestUserMode.TENANT_USER},
        };
    }


}
