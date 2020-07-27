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

import org.apache.http.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import static org.testng.Assert.*;

/**
 * Validate The Role Of An Existing and non-Existing User Through The Publisher Rest API
 * APIM2-638 / APIM2-639
 */

public class APIM638ValidateTheRoleOfAnExistingUserThroughThePublisherRestAPITestCase
        extends APIMIntegrationBaseTest {

    private final String adminRole = "carbonAdmin";
    private final String creatorRole = "creator";
    private final String publisherRole = "publisher";
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
    }

    @Test(groups = {"wso2.am"}, description = "Validate the Role of an existing user through" +
            " the publisher rest API ")
    public void testValidateTheRoleOfAnExistingUser() throws Exception {
        //Validate the admin role
        ApiResponse<Void> apiResponse = restAPIPublisher.validateRoles(adminRole);
        assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK,
                "Response code mismatch for: " + adminRole);

        //Validate the creator role
        apiResponse = restAPIPublisher.validateRoles(creatorRole);
        assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK,
                "Response code mismatch for: " + creatorRole);
    }

    @Test(groups = {"wso2.am"}, description = "Validate the Role of an non-existing user through" +
            " the publisher rest API ", dependsOnMethods = "testValidateTheRoleOfAnExistingUser")
    public void testValidateTheRoleOfAnNonExistingUser() throws Exception {
        //Validate the publisher role
        try {
            restAPIPublisher.validateRoles(publisherRole);
            Assert.fail("Exception was not thrown when validating role for non existing user");
        } catch (ApiException e) {
            assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND,
                    "User already exists for " + publisherRole);
        }
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
