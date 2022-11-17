/*
 *  Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.tests.application;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.SubscriptionsApi;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.UserManagementUtils;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

import static org.wso2.am.integration.test.utils.base.APIMIntegrationConstants.SUPER_TENANT_DOMAIN;

public class ApplicationBlockSubscriptionTestCase extends APIManagerLifecycleBaseTest {
    private static final String APPLICATION_NAME = "test-app";
    private static final String USER_NAME = "test-user";
    private static final String PASSWORD = "password";
    private static final String ORGANIZATION = "test";
    private static final String FIRST_NAME = "John";
    private RestAPIStoreImpl restAPIStoreClientUser;
    private String applicationId;
    private String apiId;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private SubscriptionsApi subscriptionsApi;
    private String subscriptionId;

    private static final String PROD_ONLY_BLOCK_STATE = "PROD_ONLY_BLOCKED";

    @Factory(dataProvider = "userModeDataProvider")
    public ApplicationBlockSubscriptionTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        subscriptionsApi = new SubscriptionsApi(restAPIPublisher.apiPublisherClient);
        createUsersAndApplications();
    }

    @Test(description = "Block and unblock subscription")
    public void testBlockUnblockSubscription() throws Exception {
        subscriptionsApi.blockSubscription(subscriptionId, PROD_ONLY_BLOCK_STATE, null);
        subscriptionsApi.unBlockSubscription(subscriptionId, null);
    }

    private void createUsersAndApplications() throws Exception{
        // Signup user.
        UserManagementUtils.signupUser(USER_NAME, PASSWORD, FIRST_NAME, ORGANIZATION);
        restAPIStoreClientUser = new RestAPIStoreImpl(USER_NAME, PASSWORD, SUPER_TENANT_DOMAIN, storeURLHttps);
        // Create application
        HttpResponse appCreationResponse1 = restAPIStoreClientUser.createApplication(APPLICATION_NAME,
                "App created by user", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = appCreationResponse1.getData();

        APIRequest apiRequest;
        String apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(user.getUserName());
        //Create, Publish
        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);
        // Subscribe
        subscriptionId = restAPIStoreClientUser.subscribeToAPI(apiId, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED).getSubscriptionId();
    }

}
