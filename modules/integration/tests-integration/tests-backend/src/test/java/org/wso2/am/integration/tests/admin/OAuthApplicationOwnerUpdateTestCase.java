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
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.clients.AdminDashboardRestClient;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * This test case is used to test OAuth application owner update
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class OAuthApplicationOwnerUpdateTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(OAuthApplicationOwnerUpdateTestCase.class);
    private UserManagementClient userManagementClient1 = null;
    private static final String USER_JOHN = "john1";
    private static final String USER_JOHN_PWD = "john1@";
    private static final String JOHN_APPLICATION = "johnApp";
    private static final String USER_MARY = "mary1";
    private static final String USER_MARY_PWD = "mary1@";
    private static final String MARY_APPLICATION = "maryApp";
    private static final String ADMIN_TENANT = "admin";
    private static final String ADMIN_TENANT_PWD = "admin1@";
    private static final String USER1_TENANT = "user1";
    private static final String USER1_TENANT_PWD = "user1@";
    private static final String USER1_TENANT_APP= "user1App";
    private static final String USER2_TENANT = "user2";
    private static final String USER2_TENANT_PWD = "user2@";
    private static final String USER2_TENANT_APP= "user2App";
    private static final String USER3_TENANT = "user3";
    private static final String USER3_TENANT_PWD = "user3@";
    private static final String USER3_TENANT_APP= "user3App";
    private static final String TENANT_DOMAIN = "tenant.com";
    private static final String ADMIN_TENANT_DOMAIN = ADMIN_TENANT + "@" + TENANT_DOMAIN;
    private static final String USER1_TENANT_DOMAIN = USER1_TENANT + "@" + TENANT_DOMAIN;
    private static final String USER2_TENANT_DOMAIN = USER2_TENANT + "@" + TENANT_DOMAIN;
    private static final String USER3_TENANT_DOMAIN = USER3_TENANT + "@" + TENANT_DOMAIN;
    private static final String USER4_TENANT_DOMAIN = "user4" + "@" + TENANT_DOMAIN;
    private static final String TIER = "10PerMin";
    private String[] subscriberRole = { "Internal/subscriber" };

    private String storeURLHttp;
    private APIStoreRestClient apiStore;
    private APIStoreRestClient apiStoreRestClient2;
    private  AdminDashboardRestClient adminDashboardRestClient;
    private JSONObject ownerJsonObject;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        storeURLHttp = getStoreURLHttp();
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(user.getUserName(), user.getPassword());
        apiStoreRestClient2 = new APIStoreRestClient(storeURLHttp);
        adminDashboardRestClient = new AdminDashboardRestClient(getPublisherURLHttps());
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                createSession(keyManagerContext));
        // add users John and Mary as subscribers
        userManagementClient.addUser(USER_JOHN, USER_JOHN_PWD, subscriberRole, USER_JOHN);
        userManagementClient.addUser(USER_MARY, USER_MARY_PWD, subscriberRole, USER_MARY);
        // add a tenant domain
        tenantManagementServiceClient.addTenant(TENANT_DOMAIN, ADMIN_TENANT_PWD, ADMIN_TENANT, "demo");
        userManagementClient1 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                ADMIN_TENANT_DOMAIN, ADMIN_TENANT_PWD);
        // add users within the tenant domain
        userManagementClient1.addUser(USER1_TENANT, USER1_TENANT_PWD, subscriberRole, USER1_TENANT);
        userManagementClient1.addUser(USER2_TENANT, USER2_TENANT_PWD, subscriberRole, USER2_TENANT);
        userManagementClient1.addUser(USER3_TENANT, USER3_TENANT_PWD, subscriberRole, USER3_TENANT);
        // create application in the store using carbon super user credentials
        apiStore.login(USER_JOHN, USER_JOHN_PWD);
        HttpResponse addApplicationOfJohn = apiStore.addApplication(JOHN_APPLICATION,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
        APPKeyRequestGenerator generateKeysOfJohnApp = new APPKeyRequestGenerator(JOHN_APPLICATION);
        String responseJohnApp = apiStore.generateApplicationKey(generateKeysOfJohnApp).getData();
        apiStore.logout();
        apiStore.login(USER_MARY, USER_MARY_PWD);
        HttpResponse addApplicationOfMary = apiStore.addApplication(MARY_APPLICATION,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
        APPKeyRequestGenerator generateKeysOfMaryApp = new APPKeyRequestGenerator(MARY_APPLICATION);
        String responseMaryApp = apiStore.generateApplicationKey(generateKeysOfMaryApp).getData();
        apiStore.logout();
        // create applications in the store using tenant admin credentials
        apiStoreRestClient2.login(ADMIN_TENANT_DOMAIN, ADMIN_TENANT_PWD);
        HttpResponse adminAppResponse = apiStoreRestClient2.addApplication(
                "adminApp", TIER, "", "super-tenant-app");
        apiStoreRestClient2.logout();
        // create applications in the store using tenant user credentials
        apiStoreRestClient2.login(USER1_TENANT_DOMAIN, USER1_TENANT_PWD);
        HttpResponse User1AppResponse = apiStoreRestClient2.addApplication(
                USER1_TENANT_APP, "10PerMin", "", "");
        APPKeyRequestGenerator generateKeysOfTenantUser1App = new APPKeyRequestGenerator(USER1_TENANT_APP);
        String responseTenantUser1= apiStoreRestClient2.generateApplicationKey(generateKeysOfTenantUser1App).getData();
        apiStoreRestClient2.logout();
        apiStoreRestClient2.login(USER2_TENANT_DOMAIN, USER2_TENANT_PWD);
        HttpResponse User2AppResponse = apiStoreRestClient2.addApplication(
                USER2_TENANT_APP, "10PerMin", "", "");
        APPKeyRequestGenerator generateKeysOfTenantUser2App = new APPKeyRequestGenerator(USER2_TENANT_APP);
        String responseTenantUser2 = apiStoreRestClient2.generateApplicationKey(generateKeysOfTenantUser2App).getData();
        apiStoreRestClient2.logout();
        apiStoreRestClient2.login(USER3_TENANT_DOMAIN, USER3_TENANT_PWD);
        HttpResponse User3AppResponse = apiStoreRestClient2.addApplication(
                USER3_TENANT_APP, "10PerMin", "", "");
        APPKeyRequestGenerator generateKeysOfTenantUser3App = new APPKeyRequestGenerator(USER3_TENANT_APP);
        String responseTenantUser3 = apiStoreRestClient2.generateApplicationKey(generateKeysOfTenantUser3App).getData();
        apiStoreRestClient2.logout();

    }

    @Test(groups = {"wso2.am"}, description = "Check whether the new owner is a valid subscriber")
    public void checkSubscriberValidity() throws Exception {
        adminDashboardRestClient.login(ADMIN_TENANT_DOMAIN, ADMIN_TENANT_PWD);
        updateOwner(USER3_TENANT_APP,USER4_TENANT_DOMAIN,USER3_TENANT_DOMAIN);
        assertTrue(ownerJsonObject.getBoolean("error"), USER4_TENANT_DOMAIN +" is not a subscriber");
    }

    @Test(groups = {"wso2.am"}, description = "Update application ownership to a user from another tenant domain")
    public void updateApplicationOwnerAcrossTenant() throws Exception {
        adminDashboardRestClient.login(ADMIN_TENANT_DOMAIN, ADMIN_TENANT_PWD);
        updateOwner(USER1_TENANT_APP,USER_MARY,USER1_TENANT_DOMAIN);
        assertTrue(ownerJsonObject.getBoolean("error"), "Unable to update application owner to the user "
                + USER_MARY + " as this user does not belong to this domain");
    }

    @Test(groups = {"wso2.am"}, description = "Check whether the new owner already has an application with that name")
    public void checkApplicationExist() throws Exception {
        adminDashboardRestClient.login(user.getUserName(), user.getPassword());
        updateOwner("DefaultApplication",USER_JOHN,USER_MARY);
        assertTrue(ownerJsonObject.getBoolean("error"), "Unable to update application owner to the user " +
                USER_JOHN +" as this user already have a application with this name");
    }

    @Test(groups = {"wso2.am"}, description = "Update application ownership to another user within the same domain")
    public void updateApplicationOwner() throws Exception {
        //Update application owner for carbon super
        adminDashboardRestClient.login(user.getUserName(), user.getPassword());
        updateOwner(MARY_APPLICATION,USER_JOHN,USER_MARY);
        assertFalse(ownerJsonObject.getBoolean("error"), "Successfully update owner of the application "
                + MARY_APPLICATION + ".");
        //Update application Owner for tenant domain
        adminDashboardRestClient.login(ADMIN_TENANT_DOMAIN, ADMIN_TENANT_PWD);
        updateOwner(USER2_TENANT_APP,USER1_TENANT_DOMAIN,USER2_TENANT_DOMAIN);
        assertFalse(ownerJsonObject.getBoolean("error"), "Successfully update owner of the application "
                + USER2_TENANT_APP + ".");
    }

    /**
     * Update Application Owner with the new userId
     *
     * @param application Application name
     * @param userId new application owner
     * @param owner current application owner
     * @return return response of updating application owner
     */
    private void updateOwner(String application, String userId, String owner) throws Exception {
        HttpResponse tenantApplications = adminDashboardRestClient.getapplicationsByTenantId(application,
                "0", "0", "10", "1", "asc");
        JSONObject jsonObject = new JSONObject(tenantApplications.getData());
        JSONArray jsonArray = jsonObject.getJSONArray("response");
        int i;
        for (i = 0; i < jsonArray.length(); i++) {
            if (owner.equals(jsonArray.getJSONObject(i).getString("owner"))) {
                String uuid = jsonArray.getJSONObject(i).getString("uuid");
                HttpResponse response = adminDashboardRestClient.updateApplicationOwner(userId, owner, uuid, application);
                ownerJsonObject = new JSONObject(response.getData());
                break;
            }
        }
        if (!ownerJsonObject.getBoolean("error")) {
            HttpResponse updatedApplications = adminDashboardRestClient.getapplicationsByTenantId(application,
                    "0", "0", "10", "1", "asc");
            JSONObject jsonObject1 = new JSONObject(updatedApplications.getData());
            JSONArray jsonArray1 = jsonObject1.getJSONArray("response");
            String ownerApp = jsonArray1.getJSONObject(i).getString("owner");
            Assert.assertEquals(ownerApp, userId);
        }
    }
}
