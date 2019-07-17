/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.wso2.am.scenario.tests.delete.existing.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.scenario.test.common.*;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;

import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * DeleteAPINegativeTestCases contains test cases for API delete negative scenarios
 */
public class DeleteAPINegativeTestCases extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(DeleteExistingAPIsTestCases.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private List<String> applicationsList = new ArrayList<>();
    private List<String> apiList = new ArrayList<>();
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "DeleteAPICreatorNeg";
    private static final String API_CREATOR_PUBLISHER_PW = "DeleteAPICreatorNeg";
    private static final String API_SUBSCRIBER_USERNAME = "DeleteAPISubscriberNeg";
    private static final String API_SUBSCRIBER_PW = "DeleteAPISubscriberNeg";
    private static final String API_PUBLISHER_USERNAME = "DeleteAPIPublisherNeg";
    private static final String API_PUBLISHER_PW = "DeleteAPIPublisherNeg";
    private static final String API_NAME_PREFIX = "DeleteAPINeg_";
    private static final String API_VERSION = "1.0.0";

    /**
     * Initialize store and publisher
     *
     * @throws APIManagerIntegrationTestException
     * @throws APIManagementException
     * @throws RemoteException
     * @throws UserAdminUserAdminException
     */
    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException, APIManagementException, RemoteException,
            UserAdminUserAdminException {
        createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW, ADMIN_USERNAME,
                ADMIN_PW);
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW);
        createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, ADMIN_USERNAME, ADMIN_PW);
        apiStore = new APIStoreRestClient(storeURL);
        apiStore.login(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW);
    }

    /**
     * Test deleting an API with subscriptions
     *
     * @param state API life cycle state
     * @throws Exception
     */
    @Test(description = "1.4.1.4", dataProvider = "DeleteAPIAfterSubscribingDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testDeleteAPIWithSubscription(APILifeCycleState state) throws Exception {
        String name = API_NAME_PREFIX + "subAppNeg_" + state.toString();
        createAPIAndSubscribe(name, name);
        if (state.equals(APILifeCycleState.BLOCKED) || state.equals(APILifeCycleState.DEPRECATED)) {
            changeApiState(name, state);
        }
        checkDeleteAPI(name, "Cannot remove the API as active ");
        verifyAPIAvailableInPublisher(name);
        // check availability in store for only PUBLISHED apis
        if (state.equals(APILifeCycleState.PUBLISHED)) {
            verifyAPIAvailableInStore(name);
        }
    }

    /**
     * Test deleting an API by an authorized user
     *
     * @throws Exception
     */
    @Test(description = "1.4.1.5")
    public void testDeleteAPIByUnauthorizedUser() throws Exception {
        String name = API_NAME_PREFIX + "unauthUser";
        createApi(name);
        createUserWithPublisherRole(API_PUBLISHER_USERNAME, API_PUBLISHER_PW, ADMIN_USERNAME, ADMIN_PW);
        apiPublisher.login(API_PUBLISHER_USERNAME, API_PUBLISHER_PW);
        checkDeleteAPI(name,
                "does not have the required permission: /permission/admin/manage/api/create");
        apiPublisher.login(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW);
        // API is in CREATED state therefor only check availability in publisher
        verifyAPIAvailableInPublisher(name);
    }

    /**
     * Test deleting an API that doesn't exist
     *
     * @throws Exception
     */
    @Test(description = "1.4.1.6")
    public void testDeleteNonExistingAPI() throws Exception {
        checkDeleteAPI(API_NAME_PREFIX + "nonExist", "Unable to find the API");
    }

    /**
     * Create , publish and subscribe to API
     *
     * @param apiName API name
     * @param applicationName application name
     * @throws Exception
     */
    private void createAPIAndSubscribe(String apiName, String applicationName) throws Exception{
        createApplication(applicationName);
        createApi(apiName);
        changeApiState(apiName, APILifeCycleState.PUBLISHED);
        isAPIVisibleInStore(apiName, apiStore);
        subscribeToAPI(apiName, applicationName);
    }

    /**
     * Create an application
     *
     * @param applicationName application name
     * @throws Exception
     */
    private void createApplication(String applicationName) throws Exception{
        HttpResponse addApplicationResponse = apiStore
                .addApplication(applicationName,
                        APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
        applicationsList.add(applicationName);
        verifyResponse(addApplicationResponse);
        assertEquals(new JSONObject(addApplicationResponse.getData()).get("status"), "APPROVED",
                "Application creation failed for application: " + applicationName);
    }

    /**
     * Create an API
     *
     * @param apiName API name
     * @throws Exception
     */
    private void createApi(String apiName) throws Exception {
        APIRequest apiRequest = new APIRequest(apiName, "/" + apiName, "public", API_VERSION,
                "/menu", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                new URL("https://localhost:9443/am/sample/pizzashack/v1/api/"));
        HttpResponse createAPIResponse = apiPublisher.addAPI(apiRequest);
        apiList.add(apiName);
        verifyResponse(createAPIResponse);
        HttpResponse apiInfo = apiPublisher.getAPI(apiName, API_CREATOR_PUBLISHER_USERNAME, API_VERSION);
        verifyResponse(apiInfo);
    }

    /**
     * Change API life cycle state
     *
     * @param apiName API name
     * @param state life cycle state
     * @throws Exception
     */
    private void changeApiState(String apiName, APILifeCycleState state) throws Exception {
        APILifeCycleStateRequest updateRequest;
        switch (state) {
            case PROTOTYPED:
                updateRequest = new org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest(apiName,
                        API_CREATOR_PUBLISHER_USERNAME, APILifeCycleState.PROTOTYPED);
                verifyApiStatusChange(apiPublisher.changeAPILifeCycleStatus(updateRequest), state.toString());
                break;
            case PUBLISHED:
                updateRequest = new org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest(apiName,
                        API_CREATOR_PUBLISHER_USERNAME, APILifeCycleState.PUBLISHED);
                verifyApiStatusChange(apiPublisher.changeAPILifeCycleStatus(updateRequest), state.toString());
                break;
            case BLOCKED:
                updateRequest = new org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest(apiName,
                        API_CREATOR_PUBLISHER_USERNAME, APILifeCycleState.BLOCKED);
                verifyApiStatusChange(apiPublisher.changeAPILifeCycleStatus(updateRequest), state.toString());
                break;
            case DEPRECATED:
                updateRequest = new org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest(apiName,
                        API_CREATOR_PUBLISHER_USERNAME, APILifeCycleState.DEPRECATED);
                verifyApiStatusChange(apiPublisher.changeAPILifeCycleStatus(updateRequest), state.toString());
                break;
            case RETIRED:
                updateRequest = new org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest(apiName,
                        API_CREATOR_PUBLISHER_USERNAME, APILifeCycleState.RETIRED);
                verifyApiStatusChange(apiPublisher.changeAPILifeCycleStatus(updateRequest), state.toString());
                break;
        }
    }

    /**
     * Subscribe to API
     *
     * @param apiName API name
     * @param applicationName Application name
     * @throws Exception
     */
    private void subscribeToAPI(String apiName, String applicationName) throws Exception {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, API_CREATOR_PUBLISHER_USERNAME);
        subscriptionRequest.setApplicationName(applicationName);
        subscriptionRequest.setTier(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        HttpResponse subscribeAPIResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(subscribeAPIResponse);
    }

    /**
     * Verify whether API state change was successful
     *
     * @param apiUpdateResponse API state update response
     * @param status API state
     */
    private void verifyApiStatusChange(HttpResponse apiUpdateResponse, String status) {
        log.info("API life cycle state change response code : " + apiUpdateResponse.getResponseCode());
        log.info("API life cycle state change response data : " + apiUpdateResponse.getData());
        JSONArray updateStatus = new JSONObject(apiUpdateResponse.getData()).getJSONArray("lcs");
        assertTrue(updateStatus.get(updateStatus.length() - 1).toString().contains("\"newStatus\":\"" + status + "\""),
                "API life cycle state change failed");
    }

    /**
     * Verify whether API delete failed
     *
     * @param apiName API name
     * @param errorMessage Error message received when deleting API
     * @throws Exception
     */
    private void checkDeleteAPI(String apiName, String errorMessage) throws Exception {
        HttpResponse response = apiPublisher.deleteAPI(apiName, API_VERSION, API_CREATOR_PUBLISHER_USERNAME);
        log.info("API delete response code for API \'" + apiName + "\' : "
                + response.getResponseCode());
        log.info("API delete response data for API \'" + apiName + "\' : " + response.getData());
        JSONObject responseData = new JSONObject(response.getData());
        assertTrue(responseData.getBoolean("error"), "API has been deleted : " + apiName);
        assertTrue(responseData.getString("message").contains(errorMessage),
                "API has been deleted : " + apiName);
    }

    /**
     * Verify whether API is available in publisher
     *
     * @param apiName API name
     * @throws Exception
     */
    private void verifyAPIAvailableInPublisher(String apiName) throws Exception {
        HttpResponse response = apiPublisher.getAPI(apiName, API_CREATOR_PUBLISHER_USERNAME, API_VERSION);
        log.info("Check API available in publisher response code for API \'" + apiName + "\' : "
                + response.getResponseCode());
        log.info("Check API available in publisher response data for API \'" + apiName + "\' : "
                + response.getData());
        assertTrue(response.getData().contains(apiName),
                "API has been delete : " + apiName);
        assertFalse(new JSONObject(response.getData()).getBoolean("error"),
                "API has been deleted : " + apiName);

    }

    /**
     * Verify whether API is available in store
     *
     * @param apiName API name
     * @throws Exception
     */
    private void verifyAPIAvailableInStore(String apiName) throws Exception {
        isAPIVisibleInStore(apiName, apiStore);
    }

    /**
     * Clean up method
     *
     * @throws Exception
     */
    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        for (String name : applicationsList) {
            apiStore.removeApplication(name);
        }
        apiPublisher.login(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW);
        for (String name : apiList) {
            apiPublisher.deleteAPI(name, API_VERSION, API_CREATOR_PUBLISHER_USERNAME);
        }
        deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        deleteUser(API_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        applicationsList.clear();
    }
}
