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

import java.util.List;

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
        assertEquals(subscriptionPolicyList.getCount().intValue(), 9, "There must be only 9 policies by default");
        assertNotNull(subscriptionPolicyList.getList(), "Subscription policy list should be available");

        SubscriptionPolicyDTO tierAsyncBronze = getSubscriptionPolicy("AsyncBronze",
                subscriptionPolicyList.getList());
        assertNotNull(tierAsyncBronze, "Tier AsyncBronze is not available");
        assertEquals(tierAsyncBronze.getDisplayName(), "AsyncBronze",
                "Invalid display name of the tier AsyncBronze");
        assertEquals(tierAsyncBronze.getDescription(), "Allows 5000 events per day",
                "Invalid description of the tier AsyncBronze");

        SubscriptionPolicyDTO tierAsyncGold = getSubscriptionPolicy("AsyncGold",
                subscriptionPolicyList.getList());
        assertNotNull(tierAsyncGold, "Tier AsyncGold is not available");
        assertEquals(tierAsyncGold.getDisplayName(), "AsyncGold",
                "Invalid display name of the tier AsyncGold");
        assertEquals(tierAsyncGold.getDescription(), "Allows 50000 events per day",
                "Invalid description of the tier AsyncGold");

        SubscriptionPolicyDTO tierAsyncSilver = getSubscriptionPolicy("AsyncSilver",
                subscriptionPolicyList.getList());
        assertNotNull(tierAsyncSilver, "Tier AsyncSilver is not available");
        assertEquals(tierAsyncSilver.getDisplayName(), "AsyncSilver",
                "Invalid display name of the tier AsyncSilver");
        assertEquals(tierAsyncSilver.getDescription(), "Allows 25000 events per day",
                "Invalid description of the tier AsyncSilver");

        SubscriptionPolicyDTO tierAsyncUnlimited = getSubscriptionPolicy("AsyncUnlimited",
                subscriptionPolicyList.getList());
        assertNotNull(tierAsyncUnlimited, "Tier AsyncUnlimited is not available");
        assertEquals(tierAsyncUnlimited.getDisplayName(), "AsyncUnlimited",
                "Invalid display name of the tier AsyncUnlimited");
        assertEquals(tierAsyncUnlimited.getDescription(), "Allows unlimited events",
                "Invalid description of the tier AsyncUnlimited");

        SubscriptionPolicyDTO tierAsyncSubscriptionless
                = getSubscriptionPolicy("AsyncDefaultSubscriptionless", subscriptionPolicyList.getList());
        assertNotNull(tierAsyncSubscriptionless, "Tier AsyncDefaultSubscriptionless is not available");
        assertEquals(tierAsyncSubscriptionless.getDisplayName(), "AsyncDefaultSubscriptionless",
                "Invalid display name of the tier AsyncDefaultSubscriptionless");
        assertEquals(tierAsyncSubscriptionless.getDescription(),
                "Allows 10000 events per day when subscription validation is disabled",
                "Invalid description of the tier AsyncDefaultSubscriptionless");

        SubscriptionPolicyDTO tierAsyncWHBronze = getSubscriptionPolicy("AsyncWHBronze",
                subscriptionPolicyList.getList());
        assertNotNull(tierAsyncWHBronze, "Tier AsyncWHBronze is not available");
        assertEquals(tierAsyncWHBronze.getDisplayName(), "AsyncWHBronze",
                "Invalid display name of the tier AsyncWHBronze");
        assertEquals(tierAsyncWHBronze.getDescription(), "Allows 1000 events per month and 500 active " +
                "subscriptions", "Invalid description of the tier AsyncWHBronze");

        SubscriptionPolicyDTO tierAsyncWHGold = getSubscriptionPolicy("AsyncWHGold",
                subscriptionPolicyList.getList());
        assertNotNull(tierAsyncWHGold, "Tier AsyncWHGold is not available");
        assertEquals(tierAsyncWHGold.getDisplayName(), "AsyncWHGold",
                "Invalid display name of the tier AsyncWHGold");
        assertEquals(tierAsyncWHGold.getDescription(), "Allows 10000 events per month and 1000 active " +
                "subscriptions", "Invalid description of the tier AsyncWHGold");

        SubscriptionPolicyDTO tierAsyncWHSilver = getSubscriptionPolicy("AsyncWHSilver",
                subscriptionPolicyList.getList());
        assertNotNull(tierAsyncWHSilver, "Tier AsyncWHSilver is not available");
        assertEquals(tierAsyncWHSilver.getDisplayName(), "AsyncWHSilver",
                "Invalid display name of the tier AsyncWHSilver");
        assertEquals(tierAsyncWHSilver.getDescription(), "Allows 5000 events per month and 500 active " +
                "subscriptions", "Invalid description of the tier AsyncWHSilver");

        SubscriptionPolicyDTO tierAsyncWHUnlimited = getSubscriptionPolicy("AsyncWHUnlimited",
                subscriptionPolicyList.getList());
        assertNotNull(tierAsyncWHUnlimited, "Tier AsyncWHUnlimited is not available");
        assertEquals(tierAsyncWHUnlimited.getDisplayName(), "AsyncWHUnlimited",
                "Invalid display name of the tier AsyncWHUnlimited");
        assertEquals(tierAsyncWHUnlimited.getDescription(), "Allows unlimited events and unlimited active " +
                "subscriptions", "Invalid description of the tier AsyncWHUnlimited");
    }

    public SubscriptionPolicyDTO getSubscriptionPolicy(String policyName, List<SubscriptionPolicyDTO> subscriptionPolicyDTOList) {
        for (SubscriptionPolicyDTO subscriptionPolicyDTO: subscriptionPolicyDTOList) {
            if (policyName.equals(subscriptionPolicyDTO.getPolicyName())) {
                return subscriptionPolicyDTO;
            }
        }
        return null;
    }
}
