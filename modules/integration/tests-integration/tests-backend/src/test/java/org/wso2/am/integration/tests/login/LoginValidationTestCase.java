/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.login;


import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import static org.testng.Assert.assertTrue;

/**
 * test fail test scenarios
 */

public class LoginValidationTestCase extends AMIntegrationBaseTest {



    private String publisherURLHttp;
    private String storeURLHttp;
    private UserManagementClient userManagementClient;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        publisherURLHttp = publisherUrls.getWebAppURLHttp();
        storeURLHttp = storeUrls.getWebAppURLHttp();

        userManagementClient = new UserManagementClient(
                apimContext.getContextUrls().getBackEndUrl(), getSessionCookie());

    }

    @Test(groups = {"wso2.am"}, description = "Login as invalid user to publisher")
    public void testInvalidLoginAsPublisherTestCase() {

        boolean loginFailed = false;
        String error = "";

        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        //Try invalid login to publisher
        try {
            apiPublisherRestClient.login(publisherContext.getContextTenant().getContextUser().getUserName()
                            + "invalid",
                    publisherContext.getContextTenant().getContextUser().getPassword());
        } catch (Exception e) {
            loginFailed = true;
            error = e.getMessage().toString();
        }

        assertTrue(loginFailed && error.contains("Please recheck the username and password and try again"),
                "Invalid user can login to the API publisher");

    }

    @Test(groups = {"wso2.am"}, description = "Login to publisher as subscriber user")
    public void testInvalidLoginAsSubscriberTestCase()
            throws Exception {

        //Try login to publisher with subscriber user
        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        boolean loginFailed = false;
        String error = "";

        if ((userManagementClient != null) &&
                !userManagementClient.userNameExists("Internal/subscriber", "subscriberUser")) {
            userManagementClient.addUser("subscriberUser", "password@123",
                    new String[]{"Internal/subscriber"}, null);
        }

        try {
            apiPublisherRestClient.login("subscriberUser",
                    "password@123");
        } catch (Exception e) {
            loginFailed = true;
            error = e.getMessage().toString();
        }

        Assert.assertTrue(loginFailed && error.contains("Login failed.Insufficient privileges"),
                "Invalid subscriber can login to the API publisher");
    }

    @Test(groups = {"wso2.am"}, description = "Login to publisher as subscriber user in tenant ")
    public void testInvalidLoginAsTenantSubscriberTestCase()
            throws Exception {

        //Try login to publisher with tenant subscriber user

        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        boolean loginFailed = false;
        String error = "";

        if ((userManagementClient != null) &&
                !userManagementClient.userNameExists("Internal/subscriber", "subscriberUser@wso2.com")) {
            userManagementClient.addUser("subscriberUser@wso2.com", "password@123",
                    new String[]{"Internal/subscriber"}, null);
        }

        try {
            apiPublisherRestClient.login("subscriberUser@wso2.com",
                    "password@123");
        } catch (Exception e) {
            loginFailed = true;
            error = e.getMessage().toString();
        }

        Assert.assertTrue(loginFailed && error.contains("Operation not successful: " +
                        "Login failed.Please recheck the username and password and try again"),
                "Invalid tenant subscriber can login to the API publisher");

    }

    @Test(groups = {"wso2.am"}, description = "Login to API store test scenario")
    public void testLoginToStoreTestCase()
            throws Exception {

        //Try login to publisher with tenant subscriber user
        String APICreatorRole = "APICreatorRole";
        String APIPublisherRole = "APIPublisherRole";
        String APIPublisherUser = "APIPublisherUser";
        String APICreatorUser = "APICreatorUser";
        String password = "password@123";
        boolean loginFailed = false;
        String errorString = "";

        APIStoreRestClient apiStoreRestClient = new APIStoreRestClient(storeURLHttp);

        String[] createPermissions = {
                "/permission/admin/login",
                "/permission/admin/manage/api/create"};

        if (!userManagementClient.roleNameExists(APICreatorRole)) {
            userManagementClient.addRole(APICreatorRole, null, createPermissions);
        }

        if ((userManagementClient != null) &&
                !userManagementClient.userNameExists(APICreatorRole, APICreatorUser)) {
            userManagementClient.addUser(APICreatorUser, password,
                    new String[]{APICreatorRole}, null);
        }

        String[] publishPermissions = {
                "/permission/admin/login",
                "/permission/admin/manage/api/publish"};

        if (!userManagementClient.roleNameExists(APIPublisherRole)) {
            userManagementClient.addRole(APIPublisherRole, null, publishPermissions);
        }

        if ((userManagementClient != null) &&
                !userManagementClient.userNameExists(APIPublisherRole, APIPublisherUser)) {
            userManagementClient.addUser(APIPublisherUser, password,
                    new String[]{APIPublisherRole}, null);
        }

        try {
            apiStoreRestClient.login("invaliduser", "invaliduser@123");
        } catch (Exception e) {
            loginFailed = true;
            errorString = e.getMessage().toString();
        }

        Assert.assertTrue(loginFailed && errorString.contains("Operation not successful: " +
                        "Login failed.Please recheck the username and password and try again"),
                "Invalid user can login to the API store");
        loginFailed = false;

        try {
            apiStoreRestClient.login(APICreatorUser, password);
        } catch (Exception e) {
            loginFailed = true;
            errorString = e.getMessage().toString();
        }

        Assert.assertTrue(loginFailed && errorString.contains("Login failed.Insufficient Privileges"),
                "API creator can login to the API store");
        loginFailed = false;

        try {
            apiStoreRestClient.login(APIPublisherUser, password);
        } catch (Exception e) {
            loginFailed = true;
            errorString = e.getMessage().toString();
        }

        Assert.assertTrue(loginFailed && errorString.contains("Login failed.Insufficient Privileges"),
                "API publisher can login to the API store");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanup();
    }
}
