/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.am.integration.tests.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationInfoDTO;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationListDTO;
import org.wso2.am.integration.clients.admin.api.dto.ScopeSettingsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertTrue;
import static org.wso2.am.integration.test.utils.base.APIMIntegrationConstants.SUPER_TENANT_DOMAIN;

/**
 * This test case is used to test OAuth application owner update
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class OAuthApplicationOwnerUpdateTestCase extends APIMIntegrationBaseTest {

    private final Log log = LogFactory.getLog(OAuthApplicationOwnerUpdateTestCase.class);
    private UserManagementClient userManagementClient1 = null;
    private static final String USER_JOHN = "john1";
    private static final String USER_JOHN_PWD = "john1@";
    private static final String JOHN_APP = "johnApp";
    private static final String USER_MARY = "mary1";
    private static final String USER_MARY_PWD = "mary1@";
    private static final String MARY_APP = "maryApp";
    private static final String TENANT_ADMIN = "admin";
    private static final String TENANT_ADMIN_PWD = "admin1@";
    private static final String TENANT_USER1 = "user1";
    private static final String TENANT_USER1_PWD = "user1@";
    private static final String TENANT_USER1_APP = "user1App";
    private static final String TENANT_USER2 = "user2";
    private static final String TENANT_USER2_PWD = "user2@";
    private static final String TENANT_USER2_APP = "user2App";
    private static final String TENANT_USER3 = "user3";
    private static final String TENANT_USER3_PWD = "user3@";
    private static final String TENANT_USER3_APP = "user3App";
    private static final String TENANT_DOMAIN = "tenant.com";
    private static final String TENANT_ADMIN_WITH_DOMAIN = TENANT_ADMIN + "@" + TENANT_DOMAIN;
    private static final String TENANT_ADMIN_APP = "tenantAdminApp";
    private static final String APIM_SUBSCRIBE_SCOPE = "apim:subscribe";
    private static final String CUSTOM_ROLE1 = "customRole1";
    private static final String CUSTOM_ROLE1_PWD = "customRole1@";
    private static final String CUSTOM_ROLE2 = "customRole2";
    private static final String CUSTOM_ROLE2_PWD = "customRole2@";
    private static final String CUSTOM_ROLE1_ASSIGNED_USER = "customRole1AssignedUser";
    private static final String CUSTOM_ROLE2_ASSIGNED_USER = "customRole2AssignedUser";
    private static final String[] PERMISSIONS = {"/permission/admin/manage/api/subscribe"};

    private static final String TENANT_USER1_WITH_DOMAIN = TENANT_USER1 + "@" + TENANT_DOMAIN;
    private static final String TENANT_USER2_WITH_DOMAIN = TENANT_USER2 + "@" + TENANT_DOMAIN;
    private static final String TENANT_USER3_WITH_DOMAIN = TENANT_USER3 + "@" + TENANT_DOMAIN;
    private static final String TENANT_USER4_WITH_DOMAIN = "user4" + "@" + TENANT_DOMAIN;
    private String[] subscriberRole = { APIMIntegrationConstants.APIM_INTERNAL_ROLE.SUBSCRIBER };

    private RestAPIStoreImpl restAPIStoreClient1;
    private RestAPIStoreImpl restAPIStoreClient2;
    private RestAPIStoreImpl restAPIStoreClient3;
    private RestAPIStoreImpl restAPIStoreClient4;
    private RestAPIStoreImpl restAPIStoreClient5;
    private RestAPIStoreImpl restAPIStoreClient6;
    private RestAPIAdminImpl restAPIAdminClient;
    private RestAPIStoreImpl restAPIStoreClientForCustomRole1AssignedUser;
    private RestAPIStoreImpl restAPIStoreClientForCustomRole2AssignedUser;

    private String appIdOfJohnApp;
    private String appIdOfMaryApp;
    private String appIdOfJohnMaryApp;
    private String appIdOfTenantAdminApp;
    private String appIdOfTenantUser1App;
    private String appIdOfTenantUser2App;
    private String appIdOfTenantUser3App;
    private String appIdOftestAppCreatedByCustomRole1AssignedUser;
    private String appIdOftestAppCreatedByCustomRole2AssignedUser;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                createSession(keyManagerContext));
        // add users John and Mary as subscribers
        userManagementClient.addUser(USER_JOHN, USER_JOHN_PWD, subscriberRole, USER_JOHN);
        userManagementClient.addUser(USER_MARY, USER_MARY_PWD, subscriberRole, USER_MARY);
        // add a tenant domain
        tenantManagementServiceClient.addTenant(TENANT_DOMAIN, TENANT_ADMIN_PWD, TENANT_ADMIN, "demo");
        userManagementClient1 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                TENANT_ADMIN_WITH_DOMAIN, TENANT_ADMIN_PWD);
        // add users within the tenant domain
        userManagementClient1.addUser(TENANT_USER1, TENANT_USER1_PWD, subscriberRole, TENANT_USER1);
        userManagementClient1.addUser(TENANT_USER2, TENANT_USER2_PWD, subscriberRole, TENANT_USER2);
        userManagementClient1.addUser(TENANT_USER3, TENANT_USER3_PWD, subscriberRole, TENANT_USER3);
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        // create application in the store using super tenant user john1's credentials
        restAPIStoreClient1 = new RestAPIStoreImpl(USER_JOHN, USER_JOHN_PWD, SUPER_TENANT_DOMAIN, storeURLHttps
        );
        ApplicationDTO appOfJohnDTO = restAPIStoreClient1.addApplication(JOHN_APP,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "App of user John");
        appIdOfJohnApp = appOfJohnDTO.getApplicationId();
        waitForKeyManagerDeployment(TENANT_DOMAIN,"Default");
        restAPIStoreClient1.generateKeys(appIdOfJohnApp,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes );

        // create application in the store using super tenant user mary1's credentials
        restAPIStoreClient2 = new RestAPIStoreImpl(USER_MARY, USER_MARY_PWD, SUPER_TENANT_DOMAIN, storeURLHttps);
        ApplicationDTO appOfMaryDTO = restAPIStoreClient2.addApplication(MARY_APP,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "App of user Mary");
        appIdOfMaryApp = appOfMaryDTO.getApplicationId();
        restAPIStoreClient2.generateKeys(appIdOfMaryApp,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes );

        // create another application in the store using super tenant user mary1's credentials
        ApplicationDTO appOfJohnMaryDTO = restAPIStoreClient2.addApplication(JOHN_APP,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "App of user Mary");
        appIdOfJohnMaryApp = appOfJohnMaryDTO.getApplicationId();
        restAPIStoreClient2.generateKeys(appIdOfJohnMaryApp,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes );

        // create application in the store using tenant admin user's credentials
        restAPIStoreClient3 = new RestAPIStoreImpl(TENANT_ADMIN, TENANT_ADMIN_PWD, TENANT_DOMAIN, storeURLHttps
        );
        ApplicationDTO appOfTenantAdminDTO = restAPIStoreClient3.addApplication(TENANT_ADMIN_APP,
                APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN, "", "App of tenant admin");
        appIdOfTenantAdminApp = appOfTenantAdminDTO.getApplicationId();
        restAPIStoreClient3.generateKeys(appIdOfTenantAdminApp,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes );

        // create application in the store using tenant user user1's credentials
        restAPIStoreClient4 = new RestAPIStoreImpl(TENANT_USER1, TENANT_USER1_PWD, TENANT_DOMAIN, storeURLHttps
        );
        ApplicationDTO appOfTenantUser1DTO = restAPIStoreClient4.addApplication(TENANT_USER1_APP,
                APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN, "", "App of tenant user 1");
        appIdOfTenantUser1App = appOfTenantUser1DTO.getApplicationId();
        restAPIStoreClient4.generateKeys(appIdOfTenantUser1App,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes );

        // create application in the store using tenant user user2's credentials
        restAPIStoreClient5 = new RestAPIStoreImpl(TENANT_USER2, TENANT_USER2_PWD, TENANT_DOMAIN, storeURLHttps
        );
        ApplicationDTO appOfTenantUser2DTO = restAPIStoreClient5.addApplication(TENANT_USER2_APP,
                APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN, "", "App of tenant user 2");
        appIdOfTenantUser2App = appOfTenantUser2DTO.getApplicationId();
        restAPIStoreClient5.generateKeys(appIdOfTenantUser2App,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes );

        // create application in the store using tenant user user3's credentials
        restAPIStoreClient6 = new RestAPIStoreImpl(TENANT_USER3, TENANT_USER3_PWD, TENANT_DOMAIN, storeURLHttps
        );
        ApplicationDTO appOfTenantUser3DTO = restAPIStoreClient6.addApplication(TENANT_USER3_APP,
                APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN, "", "App of tenant user 3");
        appIdOfTenantUser3App = appOfTenantUser3DTO.getApplicationId();
        restAPIStoreClient6.generateKeys(appIdOfTenantUser3App,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes );
    }

    @Test(groups = {"wso2.am"}, description = "Check whether the new owner is a valid subscriber")
    public void checkSubscriberValidity() {

        restAPIAdminClient = new RestAPIAdminImpl(TENANT_ADMIN, TENANT_ADMIN_PWD, TENANT_DOMAIN, publisherURLHttps);
        try {
            updateOwner(appIdOfTenantUser3App, TENANT_USER4_WITH_DOMAIN, TENANT_DOMAIN);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Update application ownership to a user from another tenant domain")
    public void updateApplicationOwnerAcrossTenant() {

        restAPIAdminClient = new RestAPIAdminImpl(TENANT_ADMIN, TENANT_ADMIN_PWD, TENANT_DOMAIN, publisherURLHttps);
        try {
            updateOwner(appIdOfTenantUser1App, USER_MARY, TENANT_DOMAIN);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Check whether the new owner already has an application with that name")
    public void checkApplicationExist() {

        restAPIAdminClient = new RestAPIAdminImpl(user.getUserName(), user.getPassword(), SUPER_TENANT_DOMAIN,
                publisherURLHttps);
        try {
            updateOwner(appIdOfJohnMaryApp, USER_JOHN, user.getUserDomain());
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Update application ownership to another user within the same domain")
    public void updateApplicationOwner() throws Exception {
        //Update application owner for carbon super
        restAPIAdminClient = new RestAPIAdminImpl(user.getUserName(), user.getPassword(), SUPER_TENANT_DOMAIN,
                publisherURLHttps);
        updateOwner(appIdOfMaryApp, USER_JOHN, user.getUserDomain());

        //Update application Owner for tenant domain
        restAPIAdminClient = new RestAPIAdminImpl(TENANT_ADMIN, TENANT_ADMIN_PWD, TENANT_DOMAIN, publisherURLHttps);
        updateOwner(appIdOfTenantUser2App, TENANT_USER1_WITH_DOMAIN, TENANT_DOMAIN);
    }

    @Test(groups = {"wso2.am"}, description = "test Update application by new owner",
            dependsOnMethods = "updateApplicationOwner")
    public void testApplicationUpdateAfterOwnerChange() throws Exception {

        String newAppName = "JohnUpdatedApplication";
        String newAppDescription = "Application updated After Ownership Change to JOHN";
        String newAppTier = "Gold";

        //Update AppTier
        HttpResponse updateTierResponse = restAPIStoreClient1.updateApplicationByID(appIdOfMaryApp,
                MARY_APP, "App of user Mary", newAppTier,
                ApplicationDTO.TokenTypeEnum.JWT);
        assertTrue(updateTierResponse.getData().contains(newAppTier), "Error while updating application tier" +
                MARY_APP);

        //Update AppName
        HttpResponse updateNameResponse = restAPIStoreClient1.updateApplicationByID(appIdOfMaryApp,
                newAppName, "App of user Mary", newAppTier,
                ApplicationDTO.TokenTypeEnum.JWT);
        assertTrue(updateNameResponse.getData().contains(newAppName), "Error while updating application name" +
                MARY_APP);

        //Update AppDescription
        HttpResponse updateDesResponse = restAPIStoreClient1.updateApplicationByID(appIdOfMaryApp,
                newAppName, newAppDescription, newAppTier,
                ApplicationDTO.TokenTypeEnum.JWT);
        assertTrue(updateDesResponse.getData().contains(newAppDescription), "Error while updating application " +
                "description" + MARY_APP);

        //Update OAuthApp - Can be enabled after fixing the issue
        /*
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.SAML2);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.NTLM);
        ApplicationKeyDTO applicationKeyDTO = new ApplicationKeyDTO();
        applicationKeyDTO.setKeyType(ApplicationKeyDTO.KeyTypeEnum.PRODUCTION);
        applicationKeyDTO.setCallbackUrl("wso2.com");
        applicationKeyDTO.setSupportedGrantTypes(grantTypes);
        org.wso2.am.integration.clients.store.api.ApiResponse<ApplicationKeyDTO> updateResponse = restAPIStoreClient1
                .updateKeys(appIdOfMaryApp, ApplicationKeyDTO.KeyTypeEnum.PRODUCTION.toString(), applicationKeyDTO);
        assertEquals(updateResponse.getStatusCode(), HttpStatus.SC_OK,
                "Response code mismatched when updating an application");
         */

    }

    @Test(groups = {"wso2.am"}, description = "Update application ownership to another user when custom user roles assigned to them")
    public void updateApplicationOwnerWhenHavingCustomRoles() throws Exception {

        //Add custom roles
        userManagementClient.addRole(CUSTOM_ROLE1, null, PERMISSIONS);
        userManagementClient.addRole(CUSTOM_ROLE2, null, PERMISSIONS);

        //Add users with custom roles created above
        userManagementClient.addUser(CUSTOM_ROLE1_ASSIGNED_USER, CUSTOM_ROLE1_PWD, new String[]{CUSTOM_ROLE1}, CUSTOM_ROLE1_ASSIGNED_USER);
        userManagementClient.addUser(CUSTOM_ROLE2_ASSIGNED_USER, CUSTOM_ROLE2_PWD, new String[]{CUSTOM_ROLE2}, CUSTOM_ROLE2_ASSIGNED_USER);

        //Add role alias mapping for system scope roles
        restAPIAdminClient = new RestAPIAdminImpl(user.getUserName(), user.getPassword(), SUPER_TENANT_DOMAIN, publisherURLHttps);
        restAPIAdminClient.addRoleAliasMappingForSystemScopeRoles(1, APIMIntegrationConstants.APIM_INTERNAL_ROLE.SUBSCRIBER, new String[]{CUSTOM_ROLE1, CUSTOM_ROLE2});

        //Create applications
        restAPIStoreClientForCustomRole1AssignedUser = new RestAPIStoreImpl(CUSTOM_ROLE1_ASSIGNED_USER, CUSTOM_ROLE1_PWD, SUPER_TENANT_DOMAIN, storeURLHttps);
        appIdOftestAppCreatedByCustomRole1AssignedUser = restAPIStoreClientForCustomRole1AssignedUser.addApplication("testAppCreatedByCustomRole1AssignedUser",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "App of user customRole1AssignedUser").getApplicationId();
        restAPIStoreClientForCustomRole2AssignedUser = new RestAPIStoreImpl(CUSTOM_ROLE2_ASSIGNED_USER, CUSTOM_ROLE2_PWD, SUPER_TENANT_DOMAIN, storeURLHttps);
        appIdOftestAppCreatedByCustomRole2AssignedUser = restAPIStoreClientForCustomRole2AssignedUser.addApplication("testAppCreatedByCustomRole2AssignedUser",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "App of user customRole2AssignedUser").getApplicationId();

        // Change application owner of testAppCreatedByCustomRole1AssignedUser from CUSTOM_ROLE1_ASSIGNED_USER to CUSTOM_ROLE2_ASSIGNED_USER
        updateOwner(appIdOftestAppCreatedByCustomRole1AssignedUser, CUSTOM_ROLE2_ASSIGNED_USER, SUPER_TENANT_DOMAIN);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        restAPIStoreClient1.deleteApplication(appIdOfJohnApp);
        restAPIStoreClient2.deleteApplication(appIdOfMaryApp);
        restAPIStoreClient3.deleteApplication(appIdOfTenantAdminApp);
        restAPIStoreClient4.deleteApplication(appIdOfTenantUser1App);
        restAPIStoreClient5.deleteApplication(appIdOfTenantUser2App);
        restAPIStoreClient6.deleteApplication(appIdOfTenantUser3App);

        if (userManagementClient != null) {
            userManagementClient.deleteUser(USER_JOHN);
            userManagementClient.deleteUser(USER_MARY);
            userManagementClient.deleteUser(CUSTOM_ROLE1_ASSIGNED_USER);
            userManagementClient.deleteUser(CUSTOM_ROLE2_ASSIGNED_USER);

            userManagementClient.deleteRole(CUSTOM_ROLE1);
            userManagementClient.deleteRole(CUSTOM_ROLE2);
        }
        if (userManagementClient1 != null) {
            userManagementClient1.deleteUser(TENANT_USER1);
            userManagementClient1.deleteUser(TENANT_USER2);
            userManagementClient1.deleteUser(TENANT_USER3);
        }
        if (restAPIStoreClientForCustomRole1AssignedUser != null && appIdOftestAppCreatedByCustomRole1AssignedUser != null) {
            restAPIStoreClientForCustomRole1AssignedUser.deleteApplication(appIdOftestAppCreatedByCustomRole1AssignedUser);
        }
        if (restAPIStoreClientForCustomRole2AssignedUser != null && appIdOftestAppCreatedByCustomRole2AssignedUser != null) {
            restAPIStoreClientForCustomRole2AssignedUser.deleteApplication(appIdOftestAppCreatedByCustomRole2AssignedUser);
        }
        tenantManagementServiceClient.deleteTenant(TENANT_DOMAIN);
    }

    /**
     * Update Owner of an application
     *
     * @param applicationId   Application ID of the application
     * @param newOwner        New owner of the application
     * @param appTenantDomain Tenant domain of the application
     */
    private void updateOwner(String applicationId, String newOwner, String appTenantDomain) throws ApiException {


        // Verify whether the new owner has the scope "apim:subscribe"
        ScopeSettingsDTO scopeSettingsDTO = restAPIAdminClient.retrieveScopesForParticularUser(APIM_SUBSCRIBE_SCOPE, newOwner);
        Assert.assertEquals(scopeSettingsDTO.getName(), APIM_SUBSCRIBE_SCOPE);

        //Update owner of the application
        ApiResponse<Void> changeOwnerResponse = restAPIAdminClient.changeApplicationOwner(newOwner, applicationId);
        Assert.assertEquals(changeOwnerResponse.getStatusCode(), HttpStatus.SC_OK);

        //Verify the owner of the updated application
        ApiResponse<ApplicationListDTO> getApplicationsResponse =
                restAPIAdminClient.getApplications(newOwner, null, null, appTenantDomain, null);
        Assert.assertEquals(getApplicationsResponse.getStatusCode(), HttpStatus.SC_OK);
        ApplicationListDTO applicationList = getApplicationsResponse.getData();
        List<ApplicationInfoDTO> applicationInfoList = applicationList.getList();
        for (ApplicationInfoDTO applicationInfo : applicationInfoList) {
            if (applicationInfo.getApplicationId().equals(applicationId)) {
                String owner = applicationInfo.getOwner();
                Assert.assertEquals(owner, newOwner);
                break;
            }
        }
    }
}
