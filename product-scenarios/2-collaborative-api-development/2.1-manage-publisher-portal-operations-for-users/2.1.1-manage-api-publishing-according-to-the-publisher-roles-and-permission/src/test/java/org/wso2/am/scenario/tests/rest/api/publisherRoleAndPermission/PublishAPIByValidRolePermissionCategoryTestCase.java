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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.net.URL;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class PublishAPIByValidRolePermissionCategoryTestCase extends ScenarioTestBase {

    private String devPortalUser;
    private String apiName;
    private String apiContext;
    private String userRole;
    private String apiVersion = "1.0.0";
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
    public PublishAPIByValidRolePermissionCategoryTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                    ADMIN_USERNAME, ADMIN_PW);
            createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, ADMIN_USERNAME, ADMIN_PW);
            devPortalUser = "adminUser1";
        }

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
//           create user in wso2.com tenant
            addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_USERNAME, ADMIN_PW);
            if (isActivated(ScenarioTestConstants.TENANT_WSO2)) {
//           Add and activate wso2.com tenant
                createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                        TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, TENANT_ADMIN_USERNAME,
                        TENANT_ADMIN_PW);
            }
            devPortalUser = "adminUser1" + "@" + ScenarioTestConstants.TENANT_WSO2;
        }
        super.init(userMode);
    }

    //TODO : Investigate test failures and fix
    @Test(description = "2.1.1.1", dataProvider = "ValidRoleDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testPublishAPIByValidRoleAssignedUser(String role) throws Exception {

        apiName = "API_" + count;
        apiContext = "/verify" + count;
        devPortalUser = devPortalUser + count;
        count++;

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion, API_CREATOR_PUBLISHER_USERNAME, new URL(backendEndPoint));
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);

        apiID = apiDto.getId();
        assertNotNull(apiDto.getId(), "API creation fails");

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUser(devPortalUser, "password123$", new String[]{role},
                    ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createUser(devPortalUser, "password123$", new String[]{role},
                    TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

        RestAPIPublisherImpl restAPIPublisherNew;
        restAPIPublisherNew = new RestAPIPublisherImpl(devPortalUser, "password123$", publisherContext.getContextTenant().getDomain(), publisherURLHttps);

        restAPIPublisherNew.changeAPILifeCycleStatusToPublish(apiID, false);

        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseGetAPI = restAPIStore.getAPI(apiDto.getId());
        assertTrue(apiResponseGetAPI.getName().contains(apiName), apiName + " is not visible in dev-Portal");

        restAPIPublisher.deleteAPI(apiDto.getId());

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(devPortalUser, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(devPortalUser, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }
    }


    //TODO : Investigate test failures and fix
    @Test(description = "2.1.1.2", dataProvider = "ValidPermissionDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testPublishAPIByValidPermissionUser(String[] permissionList) throws Exception {

        apiName = "API_1.2_" + count;
        apiContext = "/verify1.2_" + count;
        devPortalUser = devPortalUser + count;
        userRole = "role" + count;
        count++;

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion, API_CREATOR_PUBLISHER_USERNAME, new URL(backendEndPoint));
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);

        apiID = apiDto.getId();
        assertNotNull(apiDto.getId(), "API creation failed");

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createRole(ADMIN_USERNAME, ADMIN_PW, userRole, permissionList);
            createUser(devPortalUser, "password123$", new String[]{userRole},
                    ADMIN_USERNAME, ADMIN_PW);
            updateUser(devPortalUser, new String[]{ScenarioTestConstants.PUBLISHER_ROLE}, new String[]{null}, ADMIN_USERNAME, ADMIN_PW);

        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createRole(TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW, userRole, permissionList);
            createUser(devPortalUser, "password123$", new String[]{userRole},
                    TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            updateUser(devPortalUser, new String[]{ScenarioTestConstants.PUBLISHER_ROLE}, new String[]{null}, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

        RestAPIPublisherImpl restAPIPublisherNew;
        restAPIPublisherNew = new RestAPIPublisherImpl(devPortalUser, "password123$", publisherContext.getContextTenant().getDomain(), publisherURLHttps);

        restAPIPublisherNew.changeAPILifeCycleStatusToPublish(apiID, false);

        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseGetAPI = restAPIStore.getAPI(apiDto.getId());
        assertTrue(apiResponseGetAPI.getName().contains(apiName), apiName + " is not visible in dev-Portal");

        restAPIPublisher.deleteAPI(apiDto.getId());

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(devPortalUser, ADMIN_USERNAME, ADMIN_PW);
            deleteRole(userRole, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(devPortalUser, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deleteRole(userRole, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }
    }

    //TODO : Investigate test failures and fix
    @Test(description = "2.1.1.3", dataProvider = "ValidRoleDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testPublishAlreadyPublishedAPIByValidRoleAssignedUser(String role) throws Exception {

        apiName = "API_AlreadyPublished_" + count;
        apiContext = "/verifyAlreadyPublished" + count;
        devPortalUser = devPortalUser + count;
        count++;

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion, API_CREATOR_PUBLISHER_USERNAME, new URL(backendEndPoint));
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);

        apiID = apiDto.getId();
        assertNotNull(apiDto.getId(), "API creation failed");
        restAPIPublisher.changeAPILifeCycleStatus(apiID, APILifeCycleAction.PUBLISH.getAction());

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUser(devPortalUser, "password123$", new String[]{role},
                    ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createUser(devPortalUser, "password123$", new String[]{role},
                    TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

        RestAPIPublisherImpl restAPIPublisherNew;
        restAPIPublisherNew = new RestAPIPublisherImpl(devPortalUser, "password123$", publisherContext.getContextTenant().getDomain(), publisherURLHttps);

        restAPIPublisherNew.changeAPILifeCycleStatus(apiID, APILifeCycleAction.PUBLISH.getAction());

        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseGetAPI = restAPIStore.getAPI(apiDto.getId());
        assertTrue(apiResponseGetAPI.getName().contains(apiName), apiName + " is not visible in dev-Portal");

        restAPIPublisher.deleteAPI(apiID);

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(devPortalUser, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(devPortalUser, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }
    }

    //TODO : Investigate test failures and fix
    @Test(description = "2.1.1.4", dataProvider = "ValidPermissionDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testPublishAlreadyPublishedAPIByValidPermissionAssignedUser(String[] permissionList) throws Exception {

        apiName = "Published_Permission_" + count;
        apiContext = "/verifyPublishedPermission_" + count;
        devPortalUser = devPortalUser + count;
        userRole = "Newuserrole_" + count;
        count++;

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion, API_CREATOR_PUBLISHER_USERNAME, new URL(backendEndPoint));
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);

        apiID = apiDto.getId();
        assertNotNull(apiDto.getId(), "API creation failed");
        restAPIPublisher.changeAPILifeCycleStatusToPublish(apiID, false);

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createRole(ADMIN_USERNAME, ADMIN_PW, userRole, permissionList);
            createUser(devPortalUser, "password123$", new String[]{ScenarioTestConstants.PUBLISHER_ROLE},
                    ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createRole(TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW, userRole, permissionList);
            createUser(devPortalUser, "password123$", new String[]{ScenarioTestConstants.PUBLISHER_ROLE},
                    TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

        RestAPIPublisherImpl restAPIPublisherNew;
        restAPIPublisherNew = new RestAPIPublisherImpl(devPortalUser, "password123$", publisherContext.getContextTenant().getDomain(), publisherURLHttps);

        restAPIPublisherNew.changeAPILifeCycleStatus(apiID, APILifeCycleAction.PUBLISH.getAction());

        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseGetAPI = restAPIStore.getAPI(apiDto.getId());
        assertTrue(apiResponseGetAPI.getName().contains(apiName), apiName + " is not visible in dev-Portal");

        restAPIPublisher.deleteAPI(apiID);

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(devPortalUser, ADMIN_USERNAME, ADMIN_PW);
            deleteRole(userRole, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(devPortalUser, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
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
                // new Object[]{TestUserMode.TENANT_USER},
        };
    }


}
