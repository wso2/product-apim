/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.wso2.am.integration.tests.api.lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.net.URL;
import java.util.List;

/**
 * This test case test the API visibility by roles. When the API is access with direct url of the API, it also preserve
 * the role based restriction on anonymous logged users with and without specified role name.
 */
public class APIVisibilityWithDirectURLTestCase extends APIManagerLifecycleBaseTest {
    private final Log log = LogFactory.getLog(APIManagerLifecycleBaseTest.class);
    private final String apiName = "APIVisibilityWithDirectURLTestCaseAPIName";
    private final String APIContext = "APIVisibilityWithDirectURLTestCaseAPIContext";
    private final String tags = "test, EndpointType";
    private final String description = "This is test API create by API manager integration test";
    private final String APIVersion = "1.0.0";

    private String CARBON_SUPER_SUBSCRIBER_USERNAME = "directUrlUser";
    private final char[] CARBON_SUPER_SUBSCRIBER_PASSWORD = "password@123".toCharArray();
    private String CARBON_SUPER_SUBSCRIBER_1_USERNAME = "directUrlUser1";
    private final char[] CARBON_SUPER_SUBSCRIBER_1_PASSWORD = "password@123".toCharArray();
    private String INTERNAL_ROLE_SUBSCRIBER = "directUrlRole";
    private String INTERNAL_ROLE_SUBSCRIBER_1 = "directUrlRole1";
    private final String[] permissions = { "/permission/admin/login", "/permission/admin/manage/api/subscribe" };

    private APIRequest apiRequest;
    private UserManagementClient userManagementClient;
    private String endpointUrl;
    private String apiID;

    @Factory(dataProvider = "userModeDataProvider")
    public APIVisibilityWithDirectURLTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init(userMode);
        //Creating CarbonSuper context
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api/add";

        //adding new role and two users
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());

        if (TestUserMode.SUPER_TENANT_USER_STORE_USER.equals(userMode)) {
            INTERNAL_ROLE_SUBSCRIBER = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + INTERNAL_ROLE_SUBSCRIBER;
            INTERNAL_ROLE_SUBSCRIBER_1 =
                    APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + INTERNAL_ROLE_SUBSCRIBER_1;
            CARBON_SUPER_SUBSCRIBER_USERNAME =
                    APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + CARBON_SUPER_SUBSCRIBER_USERNAME;
            CARBON_SUPER_SUBSCRIBER_1_USERNAME =
                    APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + CARBON_SUPER_SUBSCRIBER_1_USERNAME;
        }

        userManagementClient.addRole(INTERNAL_ROLE_SUBSCRIBER, null, permissions);
        userManagementClient.addRole(INTERNAL_ROLE_SUBSCRIBER_1, null, permissions);
        userManagementClient.addUser(CARBON_SUPER_SUBSCRIBER_USERNAME, String.valueOf(CARBON_SUPER_SUBSCRIBER_PASSWORD),
                new String[] { INTERNAL_ROLE_SUBSCRIBER }, null);
        userManagementClient
                .addUser(CARBON_SUPER_SUBSCRIBER_1_USERNAME, String.valueOf(CARBON_SUPER_SUBSCRIBER_1_PASSWORD),
                        new String[] { INTERNAL_ROLE_SUBSCRIBER_1 }, null);
    }

    @Test(groups = { "wso2.am" }, description = "Test availability of the api without login")
    public void testDirectLinkAnonymous() throws Exception {
        String providerName = user.getUserName();

        apiRequest = new APIRequest(apiName, APIContext, new URL(endpointUrl));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setProvider(providerName);
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles(INTERNAL_ROLE_SUBSCRIBER);
        //add test api
        APIDTO apidto = restAPIPublisher.addAPI(apiRequest, "v3");
        apiID = apidto.getId();

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);

        //publish the api
        restAPIPublisher.changeAPILifeCycleStatus(apidto.getId(), Constants.PUBLISHED);
        int retry = 0;
        while (retry < 15) {
            List<APIIdentifier> apiStoreAPIIdentifierList =
                    APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(restAPIStore.getAllAPIs());
            if (APIMTestCaseUtils.isAPIAvailable(new APIIdentifier(apidto.getProvider(), apidto.getName(), apidto.getVersion()), apiStoreAPIIdentifierList)) {
                log.info("API '" + API_NAME + "' is available on store.");
                break;
            }
            log.info("Waiting for API '" + API_NAME + "' is available on store.");
            Thread.sleep(1000);
            retry++;
        }

        RestAPIStoreImpl anonymousRestAPIStore = getRestAPIStoreForAnonymousUser(user.getUserDomain());
        try {
            ApiResponse<org.wso2.am.integration.clients.store.api.v1.dto.APIDTO> apiResponse =
                    anonymousRestAPIStore.apIsApi.apisApiIdGetWithHttpInfo(apidto.getId(), user.getUserDomain(), null);
        } catch (ApiException e) {
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getCode());
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test availability of the api with login",
            dependsOnMethods = "testDirectLinkAnonymous")
    public void testDirectLink() throws Exception {
        RestAPIStoreImpl apiStore = getRestAPIStoreForUser(CARBON_SUPER_SUBSCRIBER_USERNAME,
                String.valueOf(CARBON_SUPER_SUBSCRIBER_PASSWORD), user.getUserDomain());
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apidto = apiStore.getAPI(apiID);
        Assert.assertNotNull(apidto);
    }

    @Test(groups = { "wso2.am" }, description = "Test availability of the api from user without restricted role",
            dependsOnMethods = "testDirectLink")
    public void testDirectLinkWithoutRestrictedRoleUser() throws Exception {
        RestAPIStoreImpl apiStore = getRestAPIStoreForUser(CARBON_SUPER_SUBSCRIBER_1_USERNAME,
                String.valueOf(CARBON_SUPER_SUBSCRIBER_1_PASSWORD), user.getUserDomain());
        try {
            ApiResponse<?> apiResponse = apiStore.apIsApi.apisApiIdGetWithHttpInfo(apiID, user.getUserDomain(), null);
        } catch (ApiException e) {
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getCode());
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (apiID != null) {
            undeployAndDeleteAPIRevisionsUsingRest(apiID, restAPIPublisher);
            restAPIPublisher.deleteAPI(apiID);
        }
        userManagementClient.deleteRole(INTERNAL_ROLE_SUBSCRIBER);
        userManagementClient.deleteRole(INTERNAL_ROLE_SUBSCRIBER_1);
        userManagementClient.deleteUser(CARBON_SUPER_SUBSCRIBER_USERNAME);
        userManagementClient.deleteUser(CARBON_SUPER_SUBSCRIBER_1_USERNAME);
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN },
                new Object[] { TestUserMode.SUPER_TENANT_USER_STORE_USER },
                new Object[] { TestUserMode.SUPER_TENANT_EMAIL_USER },
                new Object[] { TestUserMode.TENANT_EMAIL_USER },
        };
    }

}
