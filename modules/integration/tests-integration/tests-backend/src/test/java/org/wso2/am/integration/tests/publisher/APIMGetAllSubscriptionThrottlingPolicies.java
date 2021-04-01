/*
 *
 *   Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.publisher;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SubscriptionPolicyDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SubscriptionPolicyListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;


import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.wso2.carbon.apimgt.api.model.policy.PolicyConstants.EVENT_COUNT_TYPE;

public class APIMGetAllSubscriptionThrottlingPolicies extends APIMIntegrationBaseTest {

    @Factory(dataProvider = "userModeDataProvider")
    public APIMGetAllSubscriptionThrottlingPolicies
            (TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
//                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Test(groups = {"wso2.am"}, description = "Get all the subscription throttling polices for event count quota type ")
    public void testGetAllSubscriptionThrottlingPoliciesByQuotaType() throws Exception {
        SubscriptionPolicyListDTO subscriptionPolicyList = restAPIPublisher.getSubscriptionPolicies(EVENT_COUNT_TYPE);
        assertNotNull(subscriptionPolicyList, "There are no subscription policies available");
        assertNotNull(subscriptionPolicyList.getCount(), "Subscription policy count should be available");
        assertEquals(subscriptionPolicyList.getCount().intValue(), 8, "There must be only 8 policies by default");

        SubscriptionPolicyDTO tierAsyncBronze = subscriptionPolicyList.getList().get(2);
        assertEquals(tierAsyncBronze.getDescription(), "Allows 5000 events per day",
                "Invalid description of the tier Async Bronze");
        assertEquals(tierAsyncBronze.getDisplayName(), "AsyncBronze",
                "Invalid display name of the tier Async Bronze");
        assertEquals(tierAsyncBronze.getPolicyName(), "AsyncBronze",
                "Invalid name of the tier Async Bronze");

        SubscriptionPolicyDTO tierAsyncGold = subscriptionPolicyList.getList().get(0);
        assertEquals(tierAsyncGold.getDescription(), "Allows 50000 events per day",
                "Invalid description of the tier Async Gold");
        assertEquals(tierAsyncGold.getDisplayName(), "AsyncGold",
                "Invalid display name of the tier Async Gold");
        assertEquals(tierAsyncGold.getPolicyName(), "AsyncGold",
                "Invalid name of the tier Async Gold");

        SubscriptionPolicyDTO tierAsyncSilver = subscriptionPolicyList.getList().get(1);
        assertEquals(tierAsyncSilver.getDescription(), "Allows 25000 events per day",
                "Invalid description of the tier Async Silver");
        assertEquals(tierAsyncSilver.getDisplayName(), "AsyncSilver",
                "Invalid display name of the tier Async Silver");
        assertEquals(tierAsyncSilver.getPolicyName(), "AsyncSilver",
                "Invalid name of the tier Async Silver");

        SubscriptionPolicyDTO tierAsyncUnlimited = subscriptionPolicyList.getList().get(3);
        assertEquals(tierAsyncUnlimited.getDescription(), "Allows unlimited events",
                "Invalid description of the tier Async Unlimited");
        assertEquals(tierAsyncUnlimited.getDisplayName(), "AsyncUnlimited",
                "Invalid display name of the tier Async Unlimited");
        assertEquals(tierAsyncUnlimited.getPolicyName(), "AsyncUnlimited",
                "Invalid name of the tier Async Unlimited");

        SubscriptionPolicyDTO tierAsyncWHBronze = subscriptionPolicyList.getList().get(6);
        assertEquals(tierAsyncWHBronze.getDescription(), "Allows 1000 events per month and 500 active " +
                "subscriptions", "Invalid description of the tier AsyncWHBronze");
        assertEquals(tierAsyncWHBronze.getDisplayName(), "AsyncWHBronze",
                "Invalid display name of the tier AsyncWHBronze");
        assertEquals(tierAsyncWHBronze.getPolicyName(), "AsyncWHBronze",
                "Invalid name of the tier AsyncWHBronze");

        SubscriptionPolicyDTO tierAsyncWHGold = subscriptionPolicyList.getList().get(4);
        assertEquals(tierAsyncWHGold.getDescription(), "Allows 10000 events per month and 1000 active" +
                " subscriptions", "Invalid description of the tier AsyncWHGold");
        assertEquals(tierAsyncWHGold.getDisplayName(), "AsyncWHGold",
                "Invalid display name of the tier AsyncWHGold");
        assertEquals(tierAsyncWHGold.getPolicyName(), "AsyncWHGold",
                "Invalid name of the tier AsyncWHGold");

        SubscriptionPolicyDTO tierAsyncWHSilver = subscriptionPolicyList.getList().get(5);
        assertEquals(tierAsyncWHSilver.getDescription(), "Allows 5000 events per month and 500 active " +
                "subscriptions", "Invalid description of the tier AsyncWHSilver");
        assertEquals(tierAsyncWHSilver.getDisplayName(), "AsyncWHSilver",
                "Invalid display name of the tier AsyncWHSilver");
        assertEquals(tierAsyncWHSilver.getPolicyName(), "AsyncWHSilver",
                "Invalid name of the tier AsyncWHSilver");

        SubscriptionPolicyDTO tierAsyncWHUnlimited = subscriptionPolicyList.getList().get(7);
        assertEquals(tierAsyncWHUnlimited.getDescription(), "Allows unlimited events and unlimited active " +
                "subscriptions", "Invalid description of the tier AsyncWHUnlimited");
        assertEquals(tierAsyncWHUnlimited.getDisplayName(), "AsyncWHUnlimited",
                "Invalid display name of the tier Bronze");
        assertEquals(tierAsyncWHUnlimited.getPolicyName(), "AsyncWHUnlimited",
                "Invalid name of the tier AsyncWHUnlimited");
    }
}
