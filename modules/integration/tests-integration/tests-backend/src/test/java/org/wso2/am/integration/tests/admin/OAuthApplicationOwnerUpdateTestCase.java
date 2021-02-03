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
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.util.ArrayList;
import java.util.List;

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

    private String appIdOfJohnApp;
    private String appIdOfMaryApp;
    private String appIdOfJohnMaryApp;
    private String appIdOfTenantAdminApp;
    private String appIdOfTenantUser1App;
    private String appIdOfTenantUser2App;
    private String appIdOfTenantUser3App;

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
        restAPIStoreClient1 = new RestAPIStoreImpl(USER_JOHN, USER_JOHN_PWD, SUPER_TENANT_DOMAIN, storeURLHttps);
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
        restAPIStoreClient3 = new RestAPIStoreImpl(TENANT_ADMIN, TENANT_ADMIN_PWD, TENANT_DOMAIN, storeURLHttps);
        ApplicationDTO appOfTenantAdminDTO = restAPIStoreClient3.addApplication(TENANT_ADMIN_APP,
                APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN, "", "App of tenant admin");
        appIdOfTenantAdminApp = appOfTenantAdminDTO.getApplicationId();
        restAPIStoreClient3.generateKeys(appIdOfTenantAdminApp,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes );

        // create application in the store using tenant user user1's credentials
        restAPIStoreClient4 = new RestAPIStoreImpl(TENANT_USER1, TENANT_USER1_PWD, TENANT_DOMAIN, storeURLHttps);
        ApplicationDTO appOfTenantUser1DTO = restAPIStoreClient4.addApplication(TENANT_USER1_APP,
                APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN, "", "App of tenant user 1");
        appIdOfTenantUser1App = appOfTenantUser1DTO.getApplicationId();
        restAPIStoreClient4.generateKeys(appIdOfTenantUser1App,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes );

        // create application in the store using tenant user user2's credentials
        restAPIStoreClient5 = new RestAPIStoreImpl(TENANT_USER2, TENANT_USER2_PWD, TENANT_DOMAIN, storeURLHttps);
        ApplicationDTO appOfTenantUser2DTO = restAPIStoreClient5.addApplication(TENANT_USER2_APP,
                APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN, "", "App of tenant user 2");
        appIdOfTenantUser2App = appOfTenantUser2DTO.getApplicationId();
        restAPIStoreClient5.generateKeys(appIdOfTenantUser2App,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes );

        // create application in the store using tenant user user3's credentials
        restAPIStoreClient6 = new RestAPIStoreImpl(TENANT_USER3, TENANT_USER3_PWD, TENANT_DOMAIN, storeURLHttps);
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
            Assert.assertEquals(e.getCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
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
        }
        if (userManagementClient1 != null) {
            userManagementClient1.deleteUser(TENANT_USER1);
            userManagementClient1.deleteUser(TENANT_USER2);
            userManagementClient1.deleteUser(TENANT_USER3);
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
        //Update owner of the application
        ApiResponse<Void> changeOwnerResponse = restAPIAdminClient.changeApplicationOwner(newOwner, applicationId);
        Assert.assertEquals(changeOwnerResponse.getStatusCode(), HttpStatus.SC_OK);

        //Verify the owner of the updated application
        ApiResponse<ApplicationListDTO> getApplicationsResponse =
                restAPIAdminClient.getApplications(newOwner, null, null, appTenantDomain);
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
