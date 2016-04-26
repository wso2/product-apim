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
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.AddDocumentRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Subscribe the API by more users and add documentations to API. In API overview it should show the correct
 * user subscription count. Document tab should show the correct  information about documents and the Users
 * tab should show the correct information about subscribed uses.
 */
public class UsersAndDocsInAPIOverviewTestCase extends APIManagerLifecycleBaseTest {
    private final String API_NAME = "UsersAndDocsInAPIOverviewTest";
    private final String API_CONTEXT = "UsersAndDocsInAPIOverview";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String USER_KEY_USER2 = "userKey1";
    private final String APPLICATION_NAME = "UsersAndDocsInAPIOverviewTestCase";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private APIIdentifier apiIdentifier;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private APIStoreRestClient apiStoreClientUser2;
    private APICreationRequestBean apiCreationRequestBean;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);

        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());

        //Login to API Store with  admin
        apiStoreClientUser1.login(user.getUserName(), user.getPassword());
        apiStoreClientUser2 = new APIStoreRestClient(storeURLHttp);

        apiStoreClientUser2.login(
                publisherContext.getContextTenant().getTenantUser(USER_KEY_USER2).getUserName(),
                publisherContext.getContextTenant().getTenantUser(USER_KEY_USER2).getPassword());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
    }


    @Test(groups = {"wso2.am"}, description = "test the user count in API overview is correct")
    public void testNumberOfUsersInAPIOverview() throws APIManagerIntegrationTestException {
        String applicationDescription = "";
        String applicationCallBackUrl = "";
        apiStoreClientUser1.addApplication(APPLICATION_NAME, APIMIntegrationConstants.APPLICATION_TIER.LARGE,
                applicationCallBackUrl, applicationDescription);
        apiStoreClientUser2.addApplication(APPLICATION_NAME, APIMIntegrationConstants.APPLICATION_TIER.LARGE, applicationCallBackUrl, applicationDescription);
        //Create publish and subscribe a API by user 1
        APIIdentifier apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_GOLD);
        createPublishAndSubscribeToAPI(
                apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, apiStoreClientUser1, APPLICATION_NAME);
        HttpResponse publisherOverviewPageResponse1 =
                apiPublisherClientUser1.getAPIInformationPage(API_NAME, providerName, API_VERSION_1_0_0);
        assertEquals(publisherOverviewPageResponse1.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when retrieving the Publisher Overview page");
        assertTrue(getUserStringInOverview(publisherOverviewPageResponse1.getData()).contains("1 User")
                , "User count is not equal to 1 , when only one user subscription is available");
        // subscribe 2nd user
        subscribeToAPI(this.apiIdentifier, APPLICATION_NAME, apiStoreClientUser2);
        HttpResponse publisherOverviewPageResponse2 =
                apiPublisherClientUser1.getAPIInformationPage(API_NAME, providerName, API_VERSION_1_0_0);
        assertEquals(publisherOverviewPageResponse2.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(getUserStringInOverview(publisherOverviewPageResponse2.getData()).contains("2 Users")
                , "User count is not equal to 2 , when only one user subscription is available");
    }


    @Test(groups = {"wso2.am"}, description = "test user information in API overview Users tab is correct",
            dependsOnMethods = "testNumberOfUsersInAPIOverview")
    public void testUsersInformationInUserTabInAPIOverview() throws APIManagerIntegrationTestException {
        HttpResponse publisherOverviewPageResponse =
                apiPublisherClientUser1.getAPIInformationPage(API_NAME, providerName, API_VERSION_1_0_0);
        assertEquals(publisherOverviewPageResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when retrieving the Publisher Overview page");
        assertTrue(isUserAvailableInActiveSubscriptionInUserTab(publisherOverviewPageResponse.getData(), providerName), "");

    }


    @Test(groups = {"wso2.am"}, description = "test user information in API overview Docs tab is correct",
            dependsOnMethods = "testUsersInformationInUserTabInAPIOverview")
    public void testDocInformationInDocsTabInAPIOverview() throws APIManagerIntegrationTestException {
        // Add 2 documents
        AddDocumentRequestBean addDocumentRequestBean1 = new AddDocumentRequestBean();
        addDocumentRequestBean1.setApiName(API_NAME);
        addDocumentRequestBean1.setApiVersion(API_VERSION_1_0_0);
        addDocumentRequestBean1.setApiProvider(providerName);
        addDocumentRequestBean1.setDocName("Doc1");
        addDocumentRequestBean1.setDocType("how to");
        addDocumentRequestBean1.setDocSourceType("inline");
        addDocumentRequestBean1.setDocSummary("test doc 1");
        addDocumentRequestBean1.setDocLocation("");
        addDocumentRequestBean1.setDocUrl("");
        AddDocumentRequestBean addDocumentRequestBean2 = new AddDocumentRequestBean();
        addDocumentRequestBean2.setApiName(API_NAME);
        addDocumentRequestBean2.setApiVersion(API_VERSION_1_0_0);
        addDocumentRequestBean2.setApiProvider(providerName);
        addDocumentRequestBean2.setDocName("Doc2");
        addDocumentRequestBean2.setDocType("how to");
        addDocumentRequestBean2.setDocSourceType("inline");
        addDocumentRequestBean2.setDocSummary("test doc 2");
        addDocumentRequestBean2.setDocLocation("");
        addDocumentRequestBean2.setDocUrl("");
        apiPublisherClientUser1.addDocument(addDocumentRequestBean1);
        apiPublisherClientUser1.addDocument(addDocumentRequestBean2);
        HttpResponse publisherOverviewPageResponse =
                apiPublisherClientUser1.getAPIInformationPage(API_NAME, providerName, API_VERSION_1_0_0);
        assertEquals(publisherOverviewPageResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when retrieving the Publisher Overview page");
        assertTrue(publisherOverviewPageResponse.getData().contains("id=\"" + API_NAME + "-" +
                addDocumentRequestBean1.getDocName() + "\""), " Doc Name:" + addDocumentRequestBean1.getDocName()
                + " is not available in API overview Page");
        assertTrue(publisherOverviewPageResponse.getData().contains("id=\"" + API_NAME + "-" +
                addDocumentRequestBean2.getDocName() + "\""), " Doc Name:" + addDocumentRequestBean2.getDocName() +
                " is not available in API overview Page");
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        apiStoreClientUser2.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
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

}
