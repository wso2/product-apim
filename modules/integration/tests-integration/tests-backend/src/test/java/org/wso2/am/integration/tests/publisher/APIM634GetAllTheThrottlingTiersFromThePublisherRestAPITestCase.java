/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.am.integration.clients.publisher.api.v1.dto.ThrottlingPolicyDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ThrottlingPolicyListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * APIM2-634:Get all the throttling tiers from the publisher REST api
 */

public class APIM634GetAllTheThrottlingTiersFromThePublisherRestAPITestCase
        extends APIMIntegrationBaseTest {


    @Factory(dataProvider = "userModeDataProvider")
    public APIM634GetAllTheThrottlingTiersFromThePublisherRestAPITestCase
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

    @Test(groups = {"wso2.am"}, description = "Get all the throttling tiers from the publisher " +
            "rest API ")
    public void testGetAllTheThrottlingTiers() throws Exception {
        //Get All the tiers
        ThrottlingPolicyListDTO throttlingPolicyListDTO = restAPIPublisher.getTiers(
                ThrottlingPolicyDTO.PolicyLevelEnum.SUBSCRIPTION.getValue());
        assertNotNull(throttlingPolicyListDTO, "There are no API level policies available");
        assertNotNull(throttlingPolicyListDTO.getCount(), "Throttle policy count should be available");
        assertEquals(throttlingPolicyListDTO.getCount().intValue(), 5, "There must be only 5 policies by default");

        //Validate the Tier Bronze
        ThrottlingPolicyDTO tierBronze = throttlingPolicyListDTO.getList().get(0);
        assertEquals(tierBronze.getDescription(), "Allows 1000 requests per minute",
                "Invalid description of the tier Bronze");
        assertEquals(tierBronze.getDisplayName(), "Bronze",
                "Invalid display name of the tier Bronze");
        assertEquals(tierBronze.getName(), "Bronze",
                "Invalid name of the tier Bronze");

        //Validate the Default Subscriptionless tier
        ThrottlingPolicyDTO tierSubscriptionless = throttlingPolicyListDTO.getList().get(1);
        assertEquals(tierSubscriptionless.getDescription(),
                "Allows 10000 requests per minute when subscription validation is disabled",
                "Invalid description of the tier DefaultSubscriptionless");
        assertEquals(tierSubscriptionless.getDisplayName(), "DefaultSubscriptionless",
                "Invalid display name of the tier DefaultSubscriptionless");
        assertEquals(tierSubscriptionless.getName(), "DefaultSubscriptionless",
                "Invalid name of the tier DefaultSubscriptionless");

        //Validate the Tier Gold
        ThrottlingPolicyDTO tierGold = throttlingPolicyListDTO.getList().get(2);
        assertEquals(tierGold.getDescription(), "Allows 5000 requests per minute",
                "Invalid description of the tier Gold");
        assertEquals(tierGold.getDisplayName(), "Gold",
                "Invalid display name of the tier Gold");
        assertEquals(tierGold.getName(), "Gold",
                "Invalid name of the tier Gold");

        //Validate the Tier Silver
        ThrottlingPolicyDTO tierSilver = throttlingPolicyListDTO.getList().get(3);
        assertEquals(tierSilver.getDescription(), "Allows 2000 requests per minute",
                "Invalid description of the tier Silver");
        assertEquals(tierSilver.getDisplayName(), "Silver",
                "Invalid display name of the tier Silver");
        assertEquals(tierSilver.getName(), "Silver",
                "Invalid name of the tier Silver");

        //Validate the Tier Unlimited
        ThrottlingPolicyDTO tierUnlimited = throttlingPolicyListDTO.getList().get(4);
        assertEquals(tierUnlimited.getDescription(), "Allows unlimited requests",
                "Invalid description of the tier Unlimited");
        assertEquals(tierUnlimited.getDisplayName(), "Unlimited",
                "Invalid display name of the tier Unlimited");
        assertEquals(tierUnlimited.getName(), "Unlimited",
                "Invalid name of the tier Unlimited");


    }

}
