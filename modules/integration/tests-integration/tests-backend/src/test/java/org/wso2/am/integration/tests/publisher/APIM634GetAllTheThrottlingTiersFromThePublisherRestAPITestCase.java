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

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import static org.testng.Assert.assertEquals;

/**
 * APIM2-634:Get all the throttling tiers from the publisher REST api
 */

public class APIM634GetAllTheThrottlingTiersFromThePublisherRestAPITestCase
        extends APIMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;

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

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

    }

    @Test(groups = {"wso2.am"}, description = "Get all the throttling tiers from the publisher " +
            "rest API ")
    public void testGetAllTheThrottlingTiers() throws Exception {
        //Get All the tiers
        JSONObject tiersResponse = new JSONObject(apiPublisher.getTiers().getData());
        JSONArray tierArrayList = tiersResponse.getJSONArray("tiers");

        //Validate the Tier Unlimited
        JSONObject tierUnlimited = (JSONObject) tierArrayList.get(0);
        assertEquals(tierUnlimited.getString("tierDescription"), "Allows unlimited requests",
                "Invalid description of the tier Unlimited");
        assertEquals(tierUnlimited.getString("tierDisplayName"), "Unlimited",
                "Invalid display name of the tier Unlimited");
        assertEquals(tierUnlimited.getString("tierName"), "Unlimited",
                "Invalid name of the tier Unlimited");
        assertEquals(tierUnlimited.getString("defaultTier"), "true",
                "Invalid value for the default tier");

        //Validate the Tier Gold
        JSONObject tierGold = (JSONObject) tierArrayList.get(1);
        assertEquals(tierGold.getString("tierDescription"), "Allows 5000 requests per minute",
                "Invalid description of the tier Gold");
        assertEquals(tierGold.getString("tierDisplayName"), "Gold",
                "Invalid display name of the tier Gold");
        assertEquals(tierGold.getString("tierName"), "Gold",
                "Invalid name of the tier Gold");
        assertEquals(tierGold.getString("defaultTier"), "false",
                "Invalid value for the default tier");

        //Validate the Tier Silver
        JSONObject tierSilver = (JSONObject) tierArrayList.get(2);
        assertEquals(tierSilver.getString("tierDescription"), "Allows 2000 requests per minute",
                "Invalid description of the tier Silver");
        assertEquals(tierSilver.getString("tierDisplayName"), "Silver",
                "Invalid display name of the tier Silver");
        assertEquals(tierSilver.getString("tierName"), "Silver",
                "Invalid name of the tier Silver");
        assertEquals(tierSilver.getString("defaultTier"), "false",
                "Invalid value for the default tier");

        //Validate the Tier Silver
        JSONObject tierBronze = (JSONObject) tierArrayList.get(3);
        assertEquals(tierBronze.getString("tierDescription"), "Allows 1000 requests per minute",
                "Invalid description of the tier Bronze");
        assertEquals(tierBronze.getString("tierDisplayName"), "Bronze",
                "Invalid display name of the tier Bronze");
        assertEquals(tierBronze.getString("tierName"), "Bronze",
                "Invalid name of the tier Bronze");
        assertEquals(tierBronze.getString("defaultTier"), "false",
                "Invalid value for the default tier");

    }


}
