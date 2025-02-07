/*
 *   Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.am.integration.tests.other;

import org.apache.commons.lang.ArrayUtils;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.admin.clients.user.RemoteUserStoreManagerServiceClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.um.ws.api.stub.ClaimValue;
import org.wso2.carbon.um.ws.api.stub.PermissionDTO;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;

public class SecondaryUserStoreCaseInsensitiveTestCase extends APIManagerLifecycleBaseTest {

    private ServerConfigurationManager serverConfigurationManager;
    private String FIRST_USER = "SECONDARY/testUser1";
    private String SECOND_USER = "SECONDARY/testUser2";
    private String FIRST_USER_UPPERCASE = "SECONDARY/TESTUSER1";
    private String SECOND_USER_UPPERCASE = "SECONDARY/TESTUSER2";
    private String FIRST_ROLE = "SECONDARY/userrole1";
    private String SECOND_ROLE = "SECONDARY/userrole2";
    private String FIRST_ROLE_UPPERCASE = "SECONDARY/USERROLE1";
    private String SECOND_ROLE_UPPERCASE = "SECONDARY/USERROLE2";
    private String PASSWORD = "password123";
    private final String INTERNAL_PUBLISHER = "Internal/publisher";
    private final String INTERNAL_SUBSCRIBER = "Internal/subscriber";
    String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);

    @Factory(dataProvider = "userModeDataProvider")
    public SecondaryUserStoreCaseInsensitiveTestCase(TestUserMode userMode) {
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
        try {
            super.init(userMode);
            superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                    APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                    userMode);
            serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
            File secondaryUserStoreFile = new File(
                    getAMResourceLocation() + File.separator + "configFiles" + File.separator + "caseInsensitiveUsername"
                            + File.separator + "secondary.xml");
            File targetSecondaryUserStoreFile = new File(carbonHome + File.separator + "repository" + File.separator
                    + "deployment" + File.separator + "server" + File.separator + "userstores" + File.separator + "secondary.xml");
            serverConfigurationManager.applyConfiguration(secondaryUserStoreFile, targetSecondaryUserStoreFile, true, true);

            remoteUserStoreManagerServiceClient = new RemoteUserStoreManagerServiceClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                    keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                    keyManagerContext.getContextTenant().getTenantAdmin().getPassword());
        } catch (Exception e) {
            Assert.assertTrue(false, "Error occurred while configuring the server instance: " + e.getCause());
        }
    }

    @Test(groups = {"wso2.am"}, description = "Add role with any case to the secondary userstore")
    public void testAddSecondaryUserRoleWithAnyCase() throws Exception {
        try {
            PermissionDTO permissionDTO = null;
            PermissionDTO[] permissionDTOList = {permissionDTO};
            //Add role
            remoteUserStoreManagerServiceClient.addRole(FIRST_ROLE, new String[]{}, permissionDTOList);
            remoteUserStoreManagerServiceClient.addRole(SECOND_ROLE_UPPERCASE, new String[]{}, permissionDTOList);
            //Add user
            remoteUserStoreManagerServiceClient.addUser(FIRST_USER, PASSWORD,
                    new String[]{INTERNAL_PUBLISHER, INTERNAL_SUBSCRIBER, FIRST_ROLE}, new ClaimValue[]{},
                    "default", false);
            remoteUserStoreManagerServiceClient.addUser(SECOND_USER_UPPERCASE, PASSWORD,
                    new String[]{INTERNAL_SUBSCRIBER, SECOND_ROLE_UPPERCASE}, new ClaimValue[]{},
                    "default", false);
            //Verify added roles
            String[] roleListUser1 = remoteUserStoreManagerServiceClient.getRoleListOfUser(FIRST_USER);
            String[] roleListUser2 = remoteUserStoreManagerServiceClient.getRoleListOfUser(SECOND_USER);
            Assert.assertTrue(ArrayUtils.contains(roleListUser1, FIRST_ROLE));
            Assert.assertTrue(ArrayUtils.contains(roleListUser2, SECOND_ROLE_UPPERCASE));
        } catch (Exception e) {
            Assert.assertTrue(false, "Error occurred while adding user: " + e.getCause());
        }
    }

    @Test(groups = {"wso2.am"}, description = "Delete role with any case from secondary userstore",
            dependsOnMethods = "testAddSecondaryUserRoleWithAnyCase")
    public void testDeleteSecondaryRoleWithAnyCase() throws Exception {
        try {
            String[] deletedRoles = {SECOND_ROLE_UPPERCASE};
            String[] addedRoles = {FIRST_ROLE};
            // Update role list of user
            remoteUserStoreManagerServiceClient.updateRoleListOfUser(SECOND_USER,
                    deletedRoles, addedRoles);
            String[] updatedUser = remoteUserStoreManagerServiceClient.getRoleListOfUser(SECOND_USER);
            Assert.assertFalse(ArrayUtils.contains(updatedUser, SECOND_ROLE_UPPERCASE));

            //Delete roles
            remoteUserStoreManagerServiceClient.deleteRole(FIRST_ROLE);
            remoteUserStoreManagerServiceClient.deleteRole(SECOND_ROLE_UPPERCASE);

            //Verify whether roles are deleted
            String[] roleListUser1 = remoteUserStoreManagerServiceClient.getRoleListOfUser(FIRST_USER);
            String[] roleListUser2 = remoteUserStoreManagerServiceClient.getRoleListOfUser(SECOND_USER);

            Assert.assertFalse(ArrayUtils.contains(roleListUser1, FIRST_ROLE));
            Assert.assertFalse(ArrayUtils.contains(roleListUser1, SECOND_ROLE));
            Assert.assertFalse(ArrayUtils.contains(roleListUser1, FIRST_ROLE_UPPERCASE));
            Assert.assertFalse(ArrayUtils.contains(roleListUser1, SECOND_ROLE_UPPERCASE));
            Assert.assertFalse(ArrayUtils.contains(roleListUser2, FIRST_ROLE));
            Assert.assertFalse(ArrayUtils.contains(roleListUser2, SECOND_ROLE_UPPERCASE));

            // Delete users
            remoteUserStoreManagerServiceClient.deleteUser(FIRST_USER);
            remoteUserStoreManagerServiceClient.deleteUser(SECOND_USER_UPPERCASE);
        } catch (Exception e) {
            Assert.assertTrue(false, "Error occurred while deleting role: " + e.getCause());
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
        serverConfigurationManager.restoreToLastConfiguration();
    }
}
