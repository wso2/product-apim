/*
 * Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.tests.restapi.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;

import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationInfoDTO;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays;

import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class ApplicationsSearchByNameOrOwnerTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(ApplicationsSearchByNameOrOwnerTestCase.class);

    private static final String ADMIN_USER_KEY = "admin";
    private static final String CARBON_USER_KEY = "userKey1";
    private static final String TENANT_DOMAIN = "wso2.com";
    private static final String TENANT_USER_KEY = "user1";

    private static final String DEFAULT_APP = "DefaultApplication";
    private static final String ADMIN_APP1 = "MainApp1";
    private static final String ADMIN_APP2 = "MainApp2";
    private static final String USER_APP1 = "TestApp1";
    private static final String USER_APP2 = "TestApp2";

    private static final String ADMIN_USER_SEARCH_QUERY_BY_NAME = "MainApp";
    private static final String ADMIN_USER_SEARCH_QUERY_BY_OWNER = "admin";
    private static final String USER_SEARCH_QUERY_BY_NAME = "TestApp";
    private static final String USER_SEARCH_QUERY_BY_OWNER = "test";

    private RestAPIAdminImpl restAPIAdminAdminUserClient;
    private RestAPIStoreImpl restAPIStoreAdminUserClient;
    private RestAPIStoreImpl restAPIStoreSubscriberUserClient;

    private String adminApp1Id;
    private String adminApp2Id;
    private String userApp1Id;
    private String userApp2Id;

    private User adminUser;
    private User user;

    private ArrayList<ApplicationDTO> applicationList;

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

    @Factory(dataProvider = "userModeDataProvider")
    public ApplicationsSearchByNameOrOwnerTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            adminUser = storeContext.getContextTenant().getTenantAdmin();
            user = storeContext.getContextTenant().getTenantUser(CARBON_USER_KEY);

        } else {
            adminUser = storeContext.getContextTenant().getTenantAdmin();
            user = storeContext.getContextTenant().getTenantUser(TENANT_USER_KEY);
        }

        restAPIStoreAdminUserClient = new RestAPIStoreImpl(adminUser.getUserNameWithoutDomain(),
                adminUser.getPassword(), adminUser.getUserDomain(), storeURLHttps);

        restAPIAdminAdminUserClient = new RestAPIAdminImpl(adminUser.getUserNameWithoutDomain(),
                adminUser.getPassword(), adminUser.getUserDomain(), adminURLHttps);

        restAPIStoreSubscriberUserClient = new RestAPIStoreImpl(user.getUserNameWithoutDomain(), user.getPassword(),
                adminUser.getUserDomain(), storeURLHttps);

        ApplicationDTO adminUserApp1 = restAPIStoreAdminUserClient.addApplication(ADMIN_APP1,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "App 1 of admin");

        ApplicationDTO adminUserApp2 = restAPIStoreAdminUserClient.addApplication(ADMIN_APP2,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "App 2 of admin");

        ApplicationDTO userApp1 = restAPIStoreSubscriberUserClient.addApplication(USER_APP1,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "App 1 of user");

        ApplicationDTO userApp2 = restAPIStoreSubscriberUserClient.addApplication(USER_APP2,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "App 2 of user");

        adminApp1Id = adminUserApp1.getApplicationId();
        adminApp2Id = adminUserApp2.getApplicationId();
        userApp1Id = userApp1.getApplicationId();
        userApp2Id = userApp2.getApplicationId();

        applicationList = new ArrayList<>();

        applicationList.add(adminUserApp1);
        applicationList.add(adminUserApp2);
        applicationList.add(userApp1);
        applicationList.add(userApp2);
    }

    @Test(groups = { "wso2.am" }, description = "Test the application search of admin user by application name")
    public void testApplicationSearchAdminByName() throws Exception {
        Set<String> expectedApplications = new HashSet<>(Arrays.asList(ADMIN_APP1, ADMIN_APP2));
        String searchQuery = ADMIN_USER_SEARCH_QUERY_BY_NAME;

        if (applicationList != null && !applicationList.isEmpty()) {

            ApiResponse<ApplicationListDTO> getApplicationsResponse = restAPIAdminAdminUserClient.getApplications(
                    searchQuery, null, null, null, searchQuery);

            ApplicationListDTO applicationList = getApplicationsResponse.getData();
            List<ApplicationInfoDTO> applicationInfoList = applicationList.getList();

            assert applicationInfoList != null;
            assertTrue(verifyApplicationSearchQueryResults(expectedApplications, applicationInfoList));
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test the application search of admin user by application owner")
    public void testApplicationSearchAdminByOwner() throws Exception {
        Set<String> expectedApplications = new HashSet<>(Arrays.asList(ADMIN_APP1, ADMIN_APP2, DEFAULT_APP));
        String searchQuery = ADMIN_USER_SEARCH_QUERY_BY_OWNER;

        if (applicationList != null && !applicationList.isEmpty()) {

            ApiResponse<ApplicationListDTO> getApplicationsResponse = restAPIAdminAdminUserClient.getApplications(
                    searchQuery, null, null, null, searchQuery);

            ApplicationListDTO applicationList = getApplicationsResponse.getData();
            List<ApplicationInfoDTO> applicationInfoList = applicationList.getList();

            assert applicationInfoList != null;
            assertTrue(verifyApplicationSearchQueryResults(expectedApplications, applicationInfoList));
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test the application search of non admin user by application name")
    public void testApplicationSearchUserByName() throws Exception {
        Set<String> expectedApplications = new HashSet<>(Arrays.asList(USER_APP1, USER_APP2));
        String searchQuery = USER_SEARCH_QUERY_BY_NAME;

        if (applicationList != null && !applicationList.isEmpty()) {

            ApiResponse<ApplicationListDTO> getApplicationsResponse = restAPIAdminAdminUserClient.getApplications(
                    searchQuery, null, null, null, searchQuery);

            ApplicationListDTO applicationList = getApplicationsResponse.getData();
            List<ApplicationInfoDTO> applicationInfoList = applicationList.getList();

            assert applicationInfoList != null;
            assertTrue(verifyApplicationSearchQueryResults(expectedApplications, applicationInfoList));
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test the application search of non admin user by application owner")
    public void testApplicationSearchUserByOwner() throws Exception {
        Set<String> expectedApplications = new HashSet<>(Arrays.asList(USER_APP1, USER_APP2, DEFAULT_APP));
        String searchQuery = USER_SEARCH_QUERY_BY_OWNER;

        if (applicationList != null && !applicationList.isEmpty()) {

            ApiResponse<ApplicationListDTO> getApplicationsResponse = restAPIAdminAdminUserClient.getApplications(
                    searchQuery, null, null, null, searchQuery);

            ApplicationListDTO applicationList = getApplicationsResponse.getData();
            List<ApplicationInfoDTO> applicationInfoList = applicationList.getList();

            assert applicationInfoList != null;
            assertTrue(verifyApplicationSearchQueryResults(expectedApplications, applicationInfoList));
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStoreAdminUserClient.deleteApplication(adminApp1Id);
        restAPIStoreAdminUserClient.deleteApplication(adminApp2Id);
        restAPIStoreSubscriberUserClient.deleteApplication(userApp1Id);
        restAPIStoreSubscriberUserClient.deleteApplication(userApp2Id);
    }

    public boolean verifyApplicationSearchQueryResults(Set<String> expectedResultSet,
                                                       List<ApplicationInfoDTO> applications) {
        for (String expectedName : expectedResultSet) {
            boolean found = false;
            for (ApplicationInfoDTO app : applications) {
                if (app.getName().equals(expectedName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return !expectedResultSet.isEmpty();
    }
}