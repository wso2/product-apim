/*
 *
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.other;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.net.URL;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;

/**
 */

public class APIMANAGER4480AllSubscriptionsByApplicationTestCase extends APIManagerLifecycleBaseTest {

    public static final int numberOfApplications = 5;

    // We are using the tier silver for this test case. The reason is that all the other tests are using the gold tier.
    public static final String SILVER = "Silver";
    ArrayList<String> applicationIdList = new ArrayList<>();
    String apiId = null;

    private APIStoreRestClient apiStore;
    private final String applicationNamePrefix = "APILifeCycleTestAPI-application_";

    @Factory(dataProvider = "userModeDataProvider")
    public APIMANAGER4480AllSubscriptionsByApplicationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);

        String APIName = "APIGetAllSubscriptionsTestAPI";
        String APIContext = "getAllSubscriptionsTestAPI";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String APIVersion = "1.0.0";

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles("admin");

        apiRequest.setTiersCollection(SILVER);
        apiRequest.setTier(SILVER);

        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);

        for (int i = 0; i < numberOfApplications; i++) {
            String applicationName = applicationNamePrefix + i;
            ApplicationDTO applicationDTO = restAPIStore.addApplication(applicationName,
                    APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED,
                    "", "");
            applicationIdList.add(i, applicationDTO.getApplicationId());
            restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(),
                    SILVER);
        }
    }

    @Test(description = "List all Subscriptions By by calling the getAllSubscriptionsOfApplication")
    public void testGetAllSubscriptionsOfApplication() throws Exception {
        SubscriptionListDTO subscriptionListDTO;
        subscriptionListDTO = restAPIStore.getSubscription(null, applicationIdList.get(0),
                null, null);
        assertEquals(1, subscriptionListDTO.getList().size(), "Subscription count mismatch." +
                "Error while getting subscriptions");
    }

    @Test(description = "Remove Subscriptions By ApplicationId")
    public void removeSubscriptionsOfApplicationById() throws Exception {
        SubscriptionListDTO subscriptionListDTO;
        // Get initial subscription list by ID
        subscriptionListDTO = restAPIStore.getSubscription(null, applicationIdList.get(0),
                null, null);
        assertEquals(1, subscriptionListDTO.getList().size(), "Subscription count mismatch." +
                "Error while getting subscriptions");
        // remove subscriptions
        for(SubscriptionDTO subscriptionDTO: subscriptionListDTO.getList()){
            restAPIStore.removeSubscription(subscriptionDTO.getSubscriptionId());
        }

        //Get subscription list after remove by id
        subscriptionListDTO = restAPIStore.getSubscription(null, applicationIdList.get(0),
                null, null);
        assertEquals(0, subscriptionListDTO.getList().size(), "Subscription count mismatch." +
                "Error while getting subscriptions");
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
        for(String appId: applicationIdList) {
            restAPIStore.deleteApplication(appId);
        }
        cleanUp();
    }
}