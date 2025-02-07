/*
*Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.other;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.net.URL;

import static org.testng.Assert.assertNotEquals;

public class APICreationForTenantsTestCase extends APIManagerLifecycleBaseTest {
    private final String TENANT_DOMAIN = "test.com";
    private final String TENANT_ADMIN_USERNAME = "thirdAdmin";
    private final String TENANT_ADMIN_PASSWORD = "password";
    private final String TENANT_ADMIN_USER = TENANT_ADMIN_USERNAME + "@" + TENANT_DOMAIN;
    private UserManagementClient userManagementClient = null;
    private final String[] TENANT_ROLE_LIST = { "Internal/publisher", "Internal/everyone"};
    private String API_CREATOR = "APICreatorRole";
    private String[] API_CREATOR_PERMISSIONS = { "/permission/admin/login", "/permission/admin/manage/api/create" };
    private String[] API_CREATOR_PERMISSIONS_UPDATED = { "/permission/admin/login" };
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String USER_TEST = "test";
    private final String USER_TEST_PASSWORD = "test123";
    private String apiEndPointUrl;
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private APIIdentifier apiIdentifier;
    private String API_NAME = "DummyApi";
    private APICreationRequestBean apiCreationRequestBean;


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        apiPublisher = new APIPublisherRestClient(getPublisherURLHttp());
        apiStore = new APIStoreRestClient(getStoreURLHttp());
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + "jaxrs_basic/services/customers/customerservice/";
        apiIdentifier = new APIIdentifier(USER_TEST, API_NAME, API_VERSION_1_0_0);
    }

    @Test(groups = {"wso2.am"}, description = "Custom scope assignments for role test")
    public void testScope() throws Exception {
        tenantManagementServiceClient.addTenant(TENANT_DOMAIN, TENANT_ADMIN_PASSWORD, TENANT_ADMIN_USERNAME,
                "demo");
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                TENANT_ADMIN_USER, TENANT_ADMIN_PASSWORD);
        userManagementClient.addUser(USER_TEST, USER_TEST_PASSWORD, TENANT_ROLE_LIST, USER_TEST);
        userManagementClient.addRole(API_CREATOR, new String[]{ USER_TEST }, API_CREATOR_PERMISSIONS);
        userManagementClient.setRoleUIPermission(API_CREATOR, API_CREATOR_PERMISSIONS_UPDATED);

        APIRequest apiRequest;
        apiRequest = new APIRequest("TenantScopeTestAPI", "TenantScopeTestAPI", new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);

        // Add the API using the API Publisher.
        apiPublisher.login(USER_TEST,USER_TEST_PASSWORD);
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, USER_TEST,
                        new URL(apiEndPointUrl));
        HttpResponse createAPIResponse = apiPublisher.addAPI(apiCreationRequestBean);
        assertNotEquals(createAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Tenant should not have permission to create api");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        if (userManagementClient != null) {
            userManagementClient.deleteUser(USER_TEST);
            userManagementClient.deleteRole(API_CREATOR);
        }

        super.cleanUp();
    }

}
