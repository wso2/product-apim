/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.publisher;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Validate The Role Of An Existing and non-Existing User Through The Publisher Rest API
 * APIM2-638 / APIM2-639
 */

public class APIM638ValidateTheRoleOfAnExistingUserThroughThePublisherRestAPITestCase
        extends APIMIntegrationBaseTest {

    private final String adminRole = "carbonAdmin";
    private final String creatorRole = "creator";
    private APIPublisherRestClient apiPublisher;
    private String publisherBackEndUrl;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM638ValidateTheRoleOfAnExistingUserThroughThePublisherRestAPITestCase
            (TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        String permissionListApiLogin = "/permission/admin/login";
        String permissionListApiManage = "/permission/admin/manage/api";
        String permissionListApiCreate = "/permission/admin/manage/api/create";
        String permissionListApiPublish = "/permission/admin/manage/api/publish";
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        publisherBackEndUrl = publisherContext.getContextUrls().getBackEndUrl();

        //Create a new Role with Login and API Manage Permission - API Publisher
        UserManagementClient userManagementClientAdmin = new UserManagementClient
                (publisherBackEndUrl, createSession(publisherContext));

        userManagementClientAdmin.addRole(adminRole, new String[]{},
                new String[]{permissionListApiLogin, permissionListApiManage,
                        permissionListApiCreate, permissionListApiPublish});

        //Create a new Role with Login and Create API Permission - API Publisher
        userManagementClientAdmin.addRole(creatorRole, new String[]{},
                new String[]{permissionListApiLogin, permissionListApiCreate});

        //Login to API Publisher with carbon super admin user
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

    }

    @Test(groups = {"wso2.am"}, description = "Validate the Role of an existing user through" +
            " the publisher rest API ")
    public void testValidateTheRoleOfAnExistingUser() throws Exception {

        //Validate the admin role
        JSONObject jsonObjectAdmin = new JSONObject
                (apiPublisher.validateRoles(adminRole).getData());
        assertFalse(jsonObjectAdmin.getBoolean("error"), "Invalid Role Name - " + adminRole);
        assertTrue(jsonObjectAdmin.getBoolean("response"),
                "Invalid response for the Role - " + adminRole);

        //Validate the creator role
        JSONObject jsonObjectCreator = new JSONObject
                (apiPublisher.validateRoles(creatorRole).getData());
        assertFalse(jsonObjectCreator.getBoolean("error"), "Invalid Role Name - " + creatorRole);
        assertTrue(jsonObjectCreator.getBoolean("response"),
                "Invalid response for the Role - " + creatorRole);

    }

    @Test(groups = {"wso2.am"}, description = "Validate the Role of an non-existing user through" +
            " the publisher rest API ")
    public void testValidateTheRoleOfAnNonExistingUser() throws Exception {
        String publisherRole = "publisher";

        //Validate the publisher role
        JSONObject jsonObjectPublisher = new JSONObject
                (apiPublisher.validateRoles(publisherRole).getData());
        assertFalse(jsonObjectPublisher.getBoolean("error"), "Invalid Role Name - " + publisherRole);
        assertFalse(jsonObjectPublisher.getBoolean("response"),
                "Invalid response for the Role - " + publisherRole);

    }

    @AfterClass(alwaysRun = true)
    public void clearRoles() throws Exception {
        UserManagementClient userManagementClientAdmin = new UserManagementClient
                (publisherBackEndUrl, createSession(publisherContext));
        userManagementClientAdmin.deleteRole(adminRole);
        userManagementClientAdmin.deleteRole(creatorRole);
        super.cleanUp();
    }

}
