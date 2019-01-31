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
import static org.testng.Assert.assertTrue;

public class DeleteExistingAPIsTestCases extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(DeleteExistingAPIsTestCases.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private List<String> applicationsList = new ArrayList<>();
    private List<String> apiList = new ArrayList<>();
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "DeleteAPICreatorPos";
    private static final String API_CREATOR_PUBLISHER_PW = "DeleteAPICreatorPos";
    private static final String API_SUBSCRIBER_USERNAME = "DeleteAPISubscriberPos";
    private static final String API_SUBSCRIBER_PW = "DeleteAPISubscriberPos";
    private static final String API_NAME_PREFIX = "DeleteAPI_";
    private static final String API_VERSION = "1.0.0";

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException, APIManagementException, RemoteException,
            UserAdminUserAdminException {
        apiPublisher = new APIPublisherRestClient(publisherURL);
        createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW, ADMIN_USERNAME,
                ADMIN_PW);
        apiPublisher.login(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW);
        createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, ADMIN_USERNAME, ADMIN_PW);
        apiStore = new APIStoreRestClient(storeURL);
        apiStore.login(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW);
    }

    @Test(description = "1.4.1.1", dataProvider = "DeleteAPIInLifeCycleStateDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testDeleteAPI(APILifeCycleState state) throws Exception {
        String name = API_NAME_PREFIX + state.toString();
        createApi(name);
        changeApiStateTo(name, state);
        deleteAPI(name);
    }

    @Test(description = "1.4.1.2", dataProvider = "DeleteAPIAfterSubscribingDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testDeleteAPIAfterDeleteSubscribedApplication(APILifeCycleState state) throws Exception {
        String name = API_NAME_PREFIX + "deleteSubApp_" + state.toString();
        createAPIAndSubscribe(name, name);
        if (state.equals(APILifeCycleState.BLOCKED) || state.equals(APILifeCycleState.DEPRECATED)) {
            changeApiState(name, state);
        }
        verifyResponse(apiStore.removeApplication(name));
        deleteAPI(name);
    }

    @Test(description = "1.4.1.3", dataProvider = "DeleteAPIAfterSubscribingDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testDeleteAPIAfterUnsubscribeApplication(APILifeCycleState state) throws Exception {
        String name = API_NAME_PREFIX + "unsubApp_" + state.toString();
        createAPIAndSubscribe(name, name);
        if (state.equals(APILifeCycleState.BLOCKED) || state.equals(APILifeCycleState.DEPRECATED)) {
            changeApiState(name, state);
        }
        verifyResponse(apiStore.removeAPISubscription(name, API_VERSION, API_CREATOR_PUBLISHER_USERNAME,
                apiStore.getApplicationId(name)));
        deleteAPI(name);
    }

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

    private void changeApiStateTo(String apiName, APILifeCycleState state) throws Exception{
        switch (state) {
            case PROTOTYPED:
            case PUBLISHED:
                changeApiState(apiName, state);
                break;
            case BLOCKED:
            case DEPRECATED:
                changeApiState(apiName, APILifeCycleState.PUBLISHED);
                changeApiState(apiName, state);
                break;
            case RETIRED:
                changeApiState(apiName, APILifeCycleState.PUBLISHED);
                changeApiState(apiName, APILifeCycleState.DEPRECATED);
                changeApiState(apiName, state);
                break;
            case CREATED:
            default:
//                    do nothing
        }
    }

    private void changeApiState(String apiName, APILifeCycleState state) throws Exception {
        APILifeCycleStateRequest updateRequest;
        switch (state) {
            case PROTOTYPED:
                updateRequest = new APILifeCycleStateRequest(apiName, API_CREATOR_PUBLISHER_USERNAME,
                        APILifeCycleState.PROTOTYPED);
                verifyApiStatusChange(apiPublisher.changeAPILifeCycleStatus(updateRequest), state.toString());
                break;
            case PUBLISHED:
                updateRequest = new APILifeCycleStateRequest(apiName, API_CREATOR_PUBLISHER_USERNAME,
                        APILifeCycleState.PUBLISHED);
                verifyApiStatusChange(apiPublisher.changeAPILifeCycleStatus(updateRequest), state.toString());
                break;
            case BLOCKED:
                updateRequest = new APILifeCycleStateRequest(apiName, API_CREATOR_PUBLISHER_USERNAME,
                        APILifeCycleState.BLOCKED);
                verifyApiStatusChange(apiPublisher.changeAPILifeCycleStatus(updateRequest), state.toString());
                break;
            case DEPRECATED:
                updateRequest = new APILifeCycleStateRequest(apiName, API_CREATOR_PUBLISHER_USERNAME,
                        APILifeCycleState.DEPRECATED);
                verifyApiStatusChange(apiPublisher.changeAPILifeCycleStatus(updateRequest), state.toString());
                break;
            case RETIRED:
                updateRequest = new APILifeCycleStateRequest(apiName, API_CREATOR_PUBLISHER_USERNAME,
                        APILifeCycleState.RETIRED);
                verifyApiStatusChange(apiPublisher.changeAPILifeCycleStatus(updateRequest), state.toString());
                break;
            case CREATED:
                default:
//                    do nothing
        }
    }

    private void deleteAPI(String apiName) throws Exception {
        verifyResponse(apiPublisher.deleteAPI(apiName, API_VERSION, API_CREATOR_PUBLISHER_USERNAME));
//        verify API not available in publisher
        HttpResponse response = apiPublisher.getAPI(apiName, API_CREATOR_PUBLISHER_USERNAME, API_VERSION);
        log.info("API delete response code for API \'" + apiName + "\' : " + response.getResponseCode());
        log.info("API delete response data for API \'" + apiName + "\' : " + response.getData());
        JSONObject responseData = new JSONObject(response.getData());
        assertTrue(responseData.getBoolean("error"), "API deletion unsuccessful for API : " + apiName);
        assertTrue(responseData.getString("message").contains("Cannot find the requested API"),
                "API deletion unsuccessful for API : " + apiName);
//        verify API not available in store
        isAPINotVisibleInStore(apiName, apiStore);
    }

    private void verifyApiStatusChange(HttpResponse apiUpdateResponse, String status) {
        log.info("API life cycle state change response code : " + apiUpdateResponse.getResponseCode());
        log.info("API life cycle state change response data : " + apiUpdateResponse.getData());
        JSONArray updateStatus = new JSONObject(apiUpdateResponse.getData()).getJSONArray("lcs");
        assertTrue(updateStatus.get(updateStatus.length() - 1).toString().contains("\"newStatus\":\"" + status + "\""),
                "API life cycle state change failed");
    }

    private void createApplication(String applicationName) throws Exception{
        HttpResponse addApplicationResponse = apiStore
                .addApplication(applicationName,
                        APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
        applicationsList.add(applicationName);
        verifyResponse(addApplicationResponse);
        assertEquals(new JSONObject(addApplicationResponse.getData()).get("status"), "APPROVED",
                "Application creation failed for application: " + applicationName);
    }


    private void subscribeToAPI(String apiName, String applicationName) throws Exception {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, API_CREATOR_PUBLISHER_USERNAME);
        subscriptionRequest.setApplicationName(applicationName);
        subscriptionRequest.setTier(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        HttpResponse subscribeAPIResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(subscribeAPIResponse);
    }

    private void createAPIAndSubscribe(String apiName, String applicationName) throws Exception{
        createApplication(applicationName);
        createApi(apiName);
        changeApiState(apiName, APILifeCycleState.PUBLISHED);
        subscribeToAPI(apiName, applicationName);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        for (String name : applicationsList) {
            apiStore.removeApplication(name);
        }
        for (String name : apiList) {
            apiPublisher.deleteAPI(name, API_VERSION, API_CREATOR_PUBLISHER_USERNAME);
        }
        deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        applicationsList.clear();
    }
}
