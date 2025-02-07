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

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.net.URL;

import static org.testng.Assert.assertEquals;

/**
 */

public class APIMANAGER4480AllSubscriptionsByApplicationTestCase extends APIMIntegrationBaseTest {

    public static final int numberOfApplications = 5;

    // We are using the tier silver for this test case. The reason is that all the other tests are using the gold tier.
    public static final String SILVER = "Silver";

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
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();

        apiStore = new APIStoreRestClient(storeURLHttp);
        APIPublisherRestClient apiPublisher = new APIPublisherRestClient(publisherURLHttp);

        String APIName = "APIGetAllSubscriptionsTestAPI";
        String APIContext = "getAllSubscriptionsTestAPI";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersion = "1.0.0";

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                           publisherContext.getContextTenant().getContextUser().getPassword());
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles("admin");

        apiRequest.setTiersCollection(SILVER);
        apiRequest.setTier(SILVER);

        apiPublisher.addAPI(apiRequest);
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, providerName,
                                                                              APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                       storeContext.getContextTenant().getContextUser().getPassword());

        for (int i = 0; i < numberOfApplications; i++) {
            String applicationName = applicationNamePrefix + i;
            apiStore.addApplication(applicationName,
                    APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN, "", "this-is-test");

            SubscriptionRequest subscriptionRequest =
                    new SubscriptionRequest(APIName, storeContext.getContextTenant().getContextUser().getUserName());
            subscriptionRequest.setApplicationName(applicationName);
            subscriptionRequest.setTier(SILVER);
            apiStore.subscribe(subscriptionRequest);

            APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(applicationName);
            String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();

            JSONObject response = new JSONObject(responseString);
            String error = response.getString("error");
            if("true".equals(error)){
                throw new Exception("Unable to generate the tokens. Hence unable to execute the test case");
            }
        }
        Thread.sleep(60000);
    }

    @Test(description = "List all Subscriptions By by calling the getAllSubscriptions")
    public void testGetAllSubscriptions() throws Exception {
        String subscriptionData = apiStore.getAllSubscriptions().getData();
        JSONObject jsonSubscription = new JSONObject(subscriptionData);

        int returnedSubscriptions = 0;
        if(jsonSubscription.getString("error").equals("false")) {
            JSONObject jsonSubscriptionsObject = jsonSubscription.getJSONObject("subscriptions");
            JSONArray jsonApplicationsArray = jsonSubscriptionsObject.getJSONArray("applications");

            //Remove API Subscriptions
            for (int i = 0; i < jsonApplicationsArray.length(); i++) {
                JSONObject appObject = jsonApplicationsArray.getJSONObject(i);
                String applicationName = appObject.getString("name");
                if("DefaultApplication".equals(applicationName)){
                    // This is the default application. We have not added any subscriptions for this. Hence skipping
                    // this one
                    continue;
                }
                JSONArray subscribedAPIJSONArray = appObject.getJSONArray("subscriptions");
                // check whether the subscriptions are empty.
                int length = subscribedAPIJSONArray.length();
                assertEquals(1, length, "No subscriptions found for application : " + applicationName);
                // We do not check whether the subscriptions are correct or not since that is covered from the other
                // test cases. What we are interested is that, the getAllSubscriptions() is returning subscription
                // details for all the applications. If they are found, then our test case is successful.
                if(length > 0){
                    returnedSubscriptions++;
                }
            }
            // We also validate the total number of returned subscriptions.
            assertEquals(5, returnedSubscriptions, "Invalid number of total subscriptions were returned by " +
                                                   "getAllSubscriptions()");
        }else{
            throw new Exception("Unable to get the list of subscriptions.");
        }
    }

    @Test(description = "List all Subscriptions By by calling the getAllSubscriptionsOfApplication")
    public void testGetAllSubscriptionsOfApplication() throws Exception {
        String subscriptionData = apiStore.getAllSubscriptionsOfApplication().getData();
        JSONObject jsonSubscription = new JSONObject(subscriptionData);

        int returnedSubscriptions = 0;
        if(jsonSubscription.getString("error").equals("false")) {
            JSONObject jsonSubscriptionsObject = jsonSubscription.getJSONObject("subscriptions");
            JSONArray jsonApplicationsArray = jsonSubscriptionsObject.getJSONArray("applications");

            //Remove API Subscriptions
            for (int i = 0; i < jsonApplicationsArray.length(); i++) {
                JSONObject appObject = jsonApplicationsArray.getJSONObject(i);
                String applicationName = appObject.getString("name");
                if("DefaultApplication".equals(applicationName)){
                    // This is the default application. We have not added any subscriptions for this. Hence skipping
                    // this one
                    continue;
                }
                JSONArray subscribedAPIJSONArray = appObject.getJSONArray("subscriptions");
                int length = subscribedAPIJSONArray.length();
                if (i ==0) {
                    // We are checking the first application as the method should return that.
                    // check whether the subscriptions are empty.
                    assertEquals(1, length, "No subscriptions found for application : " + applicationName);
                }else{
                    // If there are subscriptions for other applications returned, that is wrong
                    assertEquals(0, length, "Subscriptions found for application : " + applicationName);
                }

                if(length > 0){
                    returnedSubscriptions++;
                }
                // We do not check whether the subscriptions are correct or not since that is covered from the other
                // test cases. What we are interested is that, the testGetAllSubscriptionsOfApplication() is returning
                // subscription details for first application. If they are found, then our test case is successful.
            }
            assertEquals(1, returnedSubscriptions, "More than the number of expected subscriptions were returned");
        }else{
            throw new Exception("Unable to get the list of subscriptions.");
        }
    }

    @Test(description = "List all Subscriptions By by calling the getAllSubscriptionsOfApplication with application name")
    public void testGetAllSubscriptionsOfApplicationWithSelectedApp() throws Exception {
        String selectedApplication = applicationNamePrefix + 1;
        String subscriptionData = apiStore.getAllSubscriptionsOfApplication(selectedApplication).getData();
        JSONObject jsonSubscription = new JSONObject(subscriptionData);

        if(jsonSubscription.getString("error").equals("false")) {
            JSONObject jsonSubscriptionsObject = jsonSubscription.getJSONObject("subscriptions");
            JSONArray jsonApplicationsArray = jsonSubscriptionsObject.getJSONArray("applications");

            //Remove API Subscriptions
            for (int i = 0; i < jsonApplicationsArray.length(); i++) {
                JSONObject appObject = jsonApplicationsArray.getJSONObject(i);
                String applicationName = appObject.getString("name");

                JSONArray subscribedAPIJSONArray = appObject.getJSONArray("subscriptions");
                int length = subscribedAPIJSONArray.length();
                if("DefaultApplication".equals(applicationName)){
                    // This is the default application. We have not added any subscriptions for this. Hence should be 0
                    assertEquals(0, length, "subscriptions found for the default application : " + applicationName);
                }else if(selectedApplication.equals(applicationName)) {
                    // check whether the subscriptions are empty.
                    assertEquals(1, length, "No subscriptions found for the selected application : " + applicationName);
                }else{
                    // check whether the subscriptions are empty.
                    assertEquals(0, length, "subscriptions found for invalid application : " + applicationName);
                }
                // We do not check whether the subscriptions are correct or not since that is covered from the other
                // test cases. What we are interested is that, the
                // testGetAllSubscriptionsOfApplication(selectedApplication) is returning subscription
                // details for given application. If they are found, then our test case is successful.
            }
        }else{
            throw new Exception("Unable to get the list of subscriptions.");
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        cleanUp();
    }
}