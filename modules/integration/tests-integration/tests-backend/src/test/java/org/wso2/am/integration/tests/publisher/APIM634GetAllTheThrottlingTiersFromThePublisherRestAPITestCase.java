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
import org.wso2.am.integration.clients.publisher.api.v1.dto.ThrottlingPolicyDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ThrottlingPolicyListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

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

        ThrottlingPolicyDTO tierAsyncBronze = throttlingPolicyListDTO.getList().get(0);
        assertEquals(tierAsyncBronze.getDescription(), "Allows 5000 events per day",
                "Invalid description of the tier Async Bronze");
        assertEquals(tierAsyncBronze.getDisplayName(), "AsyncBronze",
                "Invalid display name of the tier Async Bronze");
        assertEquals(tierAsyncBronze.getName(), "AsyncBronze",
                "Invalid name of the tier Async Bronze");

        ThrottlingPolicyDTO tierAsyncGold = throttlingPolicyListDTO.getList().get(1);
        assertEquals(tierAsyncGold.getDescription(), "Allows 50000 events per day",
                "Invalid description of the tier Async Gold");
        assertEquals(tierAsyncGold.getDisplayName(), "AsyncGold",
                "Invalid display name of the tier Async Gold");
        assertEquals(tierAsyncGold.getName(), "AsyncGold",
                "Invalid name of the tier Async Gold");

        ThrottlingPolicyDTO tierAsyncSilver = throttlingPolicyListDTO.getList().get(2);
        assertEquals(tierAsyncSilver.getDescription(), "Allows 25000 events per day",
                "Invalid description of the tier Async Silver");
        assertEquals(tierAsyncSilver.getDisplayName(), "AsyncSilver",
                "Invalid display name of the tier Async Silver");
        assertEquals(tierAsyncSilver.getName(), "AsyncSilver",
                "Invalid name of the tier Async Silver");

        ThrottlingPolicyDTO tierAsyncUnlimited = throttlingPolicyListDTO.getList().get(3);
        assertEquals(tierAsyncUnlimited.getDescription(), "Allows unlimited events",
                "Invalid description of the tier Async Unlimited");
        assertEquals(tierAsyncUnlimited.getDisplayName(), "AsyncUnlimited",
                "Invalid display name of the tier Async Unlimited");
        assertEquals(tierAsyncUnlimited.getName(), "AsyncUnlimited",
                "Invalid name of the tier Async Unlimited");

        ThrottlingPolicyDTO tierAsyncWHBronze = throttlingPolicyListDTO.getList().get(4);
        assertEquals(tierAsyncWHBronze.getDescription(), "Allows 1000 events per month and 500 active " +
                        "subscriptions", "Invalid description of the tier AsyncWHBronze");
        assertEquals(tierAsyncWHBronze.getDisplayName(), "AsyncWHBronze",
                "Invalid display name of the tier AsyncWHBronze");
        assertEquals(tierAsyncWHBronze.getName(), "AsyncWHBronze",
                "Invalid name of the tier AsyncWHBronze");

        ThrottlingPolicyDTO tierAsyncWHGold = throttlingPolicyListDTO.getList().get(5);
        assertEquals(tierAsyncWHGold.getDescription(), "Allows 10000 events per month and 1000 active" +
                        " subscriptions",  "Invalid description of the tier AsyncWHGold");
        assertEquals(tierAsyncWHGold.getDisplayName(), "AsyncWHGold",
                "Invalid display name of the tier AsyncWHGold");
        assertEquals(tierAsyncWHGold.getName(), "AsyncWHGold",
                "Invalid name of the tier AsyncWHGold");

        ThrottlingPolicyDTO tierAsyncWHSilver = throttlingPolicyListDTO.getList().get(6);
        assertEquals(tierAsyncWHSilver.getDescription(), "Allows 5000 events per month and 500 active " +
                        "subscriptions", "Invalid description of the tier AsyncWHSilver");
        assertEquals(tierAsyncWHSilver.getDisplayName(), "AsyncWHSilver",
                "Invalid display name of the tier AsyncWHSilver");
        assertEquals(tierAsyncWHSilver.getName(), "AsyncWHSilver",
                "Invalid name of the tier AsyncWHSilver");

        ThrottlingPolicyDTO tierAsyncWHUnlimited = throttlingPolicyListDTO.getList().get(7);
        assertEquals(tierAsyncWHUnlimited.getDescription(), "Allows unlimited events and unlimited active " +
                        "subscriptions", "Invalid description of the tier AsyncWHUnlimited");
        assertEquals(tierAsyncWHUnlimited.getDisplayName(), "AsyncWHUnlimited",
                "Invalid display name of the tier Bronze");
        assertEquals(tierAsyncWHUnlimited.getName(), "AsyncWHUnlimited",
                "Invalid name of the tier AsyncWHUnlimited");


        //Validate the Tier Bronze
        ThrottlingPolicyDTO tierBronze = throttlingPolicyListDTO.getList().get(8);
        assertEquals(tierBronze.getDescription(), "Allows 1000 requests per minute",
                "Invalid description of the tier Bronze");
        assertEquals(tierBronze.getDisplayName(), "Bronze",
                "Invalid display name of the tier Bronze");
        assertEquals(tierBronze.getName(), "Bronze",
                "Invalid name of the tier Bronze");

        //Validate the Tier Gold
        ThrottlingPolicyDTO tierGold = throttlingPolicyListDTO.getList().get(9);
        assertEquals(tierGold.getDescription(), "Allows 5000 requests per minute",
                "Invalid description of the tier Gold");
        assertEquals(tierGold.getDisplayName(), "Gold",
                "Invalid display name of the tier Gold");
        assertEquals(tierGold.getName(), "Gold",
                "Invalid name of the tier Gold");

        //Validate the Tier Silver
        ThrottlingPolicyDTO tierSilver = throttlingPolicyListDTO.getList().get(10);
        assertEquals(tierSilver.getDescription(), "Allows 2000 requests per minute",
                "Invalid description of the tier Silver");
        assertEquals(tierSilver.getDisplayName(), "Silver",
                "Invalid display name of the tier Silver");
        assertEquals(tierSilver.getName(), "Silver",
                "Invalid name of the tier Silver");

        //Validate the Tier Unlimited
        ThrottlingPolicyDTO tierUnlimited = throttlingPolicyListDTO.getList().get(11);
        assertEquals(tierUnlimited.getDescription(), "Allows unlimited requests",
                "Invalid description of the tier Unlimited");
        assertEquals(tierUnlimited.getDisplayName(), "Unlimited",
                "Invalid display name of the tier Unlimited");
        assertEquals(tierUnlimited.getName(), "Unlimited",
                "Invalid name of the tier Unlimited");


    }
}
