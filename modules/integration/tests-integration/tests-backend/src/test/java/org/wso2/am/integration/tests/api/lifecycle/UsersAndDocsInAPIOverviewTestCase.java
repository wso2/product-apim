/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.api.lifecycle;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Subscribe the APi by more users and add documentations yo API. in API overview it should show the correct
 * user subscription count. Document tab should show the correct  information about documents and the Users
 * tab should show the correct information about subscribed uses.
 */
public class UsersAndDocsInAPIOverviewTestCase extends APIManagerLifecycleBaseTest {


    private APIIdentifier apiIdentifier;
    private String applicationName;
    private static String APPLICATION_DESCRIPTION = "";
    private static String APPLICATION_CALL_BACK_URL = "";

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        apiIdentifier = new APIIdentifier(USER_NAME1, API1_NAME, API_VERSION_1_0_0);
        applicationName =
                (this.getClass().getName().replace(this.getClass().getPackage().getName(), "")).replace(".", "");


    }


    @Test(groups = {"wso2.am"}, description = "test the user count in API overview is correct")
    public void testNumberOfUsersInAPIOverview() throws Exception {


        apiStoreClientUser1.addApplication(applicationName, TIER_GOLD, APPLICATION_CALL_BACK_URL, APPLICATION_DESCRIPTION);
        apiStoreClientUser2.addApplication(applicationName, TIER_GOLD, APPLICATION_CALL_BACK_URL, APPLICATION_DESCRIPTION);
        //Create publish and subscribe a API by user 1
        APIIdentifier apiIdentifier = new APIIdentifier(USER_NAME1, API1_NAME, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_GOLD);
        createPublishAndSubscribeToAPI(
                apiIdentifier, API1_CONTEXT, apiPublisherClientUser1, apiStoreClientUser1, applicationName);

        HttpResponse publisherOverviewPageResponse1 =
                apiPublisherClientUser1.getAPIInformationPage(API1_NAME, USER_NAME1, API_VERSION_1_0_0);
        assertEquals(publisherOverviewPageResponse1.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when retrieving the Publisher Overview page");
        assertEquals(getUserStringInOverview(publisherOverviewPageResponse1.getData()),
                "1 User", "User count is not equal to 1 , when only one user subscription is available");

        // subscribe 2nd user
        subscribeToAPI(this.apiIdentifier, applicationName, apiStoreClientUser2);


        HttpResponse publisherOverviewPageResponse2 =
                apiPublisherClientUser1.getAPIInformationPage(API1_NAME, USER_NAME1, API_VERSION_1_0_0);
        assertEquals(publisherOverviewPageResponse2.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertEquals(getUserStringInOverview(publisherOverviewPageResponse2.getData()),
                "2 Users", "User count is not equal to 2 , when only one user subscription is available");


    }


    @Test(groups = {"wso2.am"}, description = "test user information in API overview Users tab is correct",
            dependsOnMethods = "testNumberOfUsersInAPIOverview")
    public void testUsersInformationInUserTabInAPIOverview() throws APIManagerIntegrationTestException {
        HttpResponse publisherOverviewPageResponse =
                apiPublisherClientUser1.getAPIInformationPage(API1_NAME, USER_NAME1, API_VERSION_1_0_0);
        assertEquals(publisherOverviewPageResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when retrieving the Publisher Overview page");
        assertTrue(isUserAvailableInActiveSubscriptionInUserTab(publisherOverviewPageResponse.getData(), USER_NAME1), "");
        assertTrue(isUserAvailableInActiveSubscriptionInUserTab(publisherOverviewPageResponse.getData(), USER_NAME2), "");


    }


    @Test(groups = {"wso2.am"}, description = "test user information in API overview Docs tab is correct",
            dependsOnMethods = "testUsersInformationInUserTabInAPIOverview")
    public void testDocInformationInDocsTabInAPIOverview() throws APIManagerIntegrationTestException {
        // Add 2 documents
        String doc1Name = "Doc1";
        String doc2Name = "Doc2";
        apiPublisherClientUser1.addDocument(API1_NAME, API_VERSION_1_0_0, USER_NAME1, doc1Name,
                "how to", "inline", "", "test doc 1", "");
        apiPublisherClientUser1.addDocument(API1_NAME, API_VERSION_1_0_0, USER_NAME1, doc2Name,
                "how to", "inline", "", "test doc 2", "");
        HttpResponse publisherOverviewPageResponse =
                apiPublisherClientUser1.getAPIInformationPage(API1_NAME, USER_NAME1, API_VERSION_1_0_0);
        assertEquals(publisherOverviewPageResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when retrieving the Publisher Overview page");

        assertTrue(publisherOverviewPageResponse.getData().contains("id=\"" + API1_NAME + "-" + doc1Name + "\""),
                " Doc Name:" + doc1Name + " is not available in API overview Page");
        assertTrue(publisherOverviewPageResponse.getData().contains("id=\"" + API1_NAME + "-" + doc2Name + "\""),
                " Doc Name:" + doc2Name + " is not available in API overview Page");

    }

    /**
     * Check the User availability under active subscription in Users Tab.
     *
     * @param responsePageOfUsersTabData - response that contain the Users Tab page.
     * @param userName                   - User name that need to find availability
     * @return boolean - true if user is available else false.
     */
    private boolean isUserAvailableInActiveSubscriptionInUserTab(String responsePageOfUsersTabData, String userName) {
        String temp1 =
                responsePageOfUsersTabData.substring(responsePageOfUsersTabData.indexOf("Active Subscriptions<"));
        String temp2 =
                temp1.substring(temp1.indexOf("Active Subscriptions<"), temp1.indexOf("Usage by Current Subscribers"));
        return temp2.contains(userName);
    }


    /**
     * Get the User count  String in API Overview page.
     *
     * @param responseData - response that contain the API Overview Tab.
     * @return String -  Return the user count string Ex : "2 Users"
     */
    private String getUserStringInOverview(String responseData) {
        String temp1 =
                responseData.substring(responseData.indexOf("<span class=\"userCount\">"));
        String temp2 =
                temp1.substring(temp1.indexOf("<span class=\"userCount\">"), temp1.indexOf("</span>"));
        return temp2.replaceAll("<span class=\"userCount\">", "").trim();

    }


    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        apiStoreClientUser1.removeApplication(applicationName);
        apiStoreClientUser2.removeApplication(applicationName);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);

    }

}
