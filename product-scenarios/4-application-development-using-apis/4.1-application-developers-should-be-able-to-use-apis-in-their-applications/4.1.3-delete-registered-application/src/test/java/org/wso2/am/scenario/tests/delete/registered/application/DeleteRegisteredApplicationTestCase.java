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
package org.wso2.am.scenario.tests.delete.registered.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DeleteRegisteredApplicationTestCase extends ScenarioTestBase {
    private APIStoreRestClient apiStore;
    private APIPublisherRestClient apiPublisher;
    private List<String> apiList = new ArrayList<>();
    private List<String> applicationsList = new ArrayList<>();
    private String apiName = "";
    private String applicationName = "";
    private final Log log = LogFactory.getLog(DeleteRegisteredApplicationTestCase.class);
    private final String ADMIN_LOGIN_USERNAME = "admin";
    private final String ADMIN_LOGIN_PW = "admin";
    private final String API_NAME_PREFIX = "AppDeleteAPI_";
    private final String API_VERSION = "1.0.0";
    private final String APPLICATION_DESCRIPTION = "ApplicationDescription";
    private final String APPLICATION_NAME_PREFIX = "AppDelete_";
    private final String CREATOR_PUBLISHER_USERNAME = "deleteAppCreatorPublisher";
    private final String CREATOR_PUBLISHER_PW = "deleteAppCreatorPublisher";
    private final String KEY_GENERATION_SUFFIX = "KeyGen";
    private final String PRODUCTION = "PRODUCTION";
    private final String SANDBOX = "SANDBOX";
    private final String STATUS_APPROVED = "APPROVED";
    private final String SUBSCRIBER_USERNAME = "deleteAppSubscriber";
    private final String SUBSCRIBER_PW = "deleteAppSubscriber";
    private final String TIER_GOLD = "Gold";
    private final String WITH_SUBSCRIPTION_SUFFIX = "WithSubs";

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        createUserWithSubscriberRole(SUBSCRIBER_USERNAME, SUBSCRIBER_PW, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        apiStore = new APIStoreRestClient(storeURL);
        apiStore.login(SUBSCRIBER_USERNAME, SUBSCRIBER_PW);

        createUserWithPublisherAndCreatorRole(CREATOR_PUBLISHER_USERNAME, CREATOR_PUBLISHER_PW, ADMIN_LOGIN_USERNAME,
                ADMIN_LOGIN_PW);
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login(CREATOR_PUBLISHER_USERNAME, CREATOR_PUBLISHER_PW);
    }

    @Test(description = "4.1.3.2")
    public void testDeleteApplication() throws Exception {
        createApplication(APPLICATION_NAME_PREFIX);
        deleteApplication(APPLICATION_NAME_PREFIX);
    }

    @Test(description = "4.1.3.1")
    public void testDeleteApplicationWithSubscription() throws Exception {
//        delete app with subscriptions
        apiName = API_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX;
        applicationName = API_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX;

        createApplication(applicationName);
        createAndPublishAPI(apiName);
        subscribeToAPI(apiName, applicationName);
        deleteApplication(applicationName);
        verifyRemovalOfSubscriptionToAPI(apiName);

//        delete applications with keys
        applicationDeletionWithKeys(PRODUCTION);
        applicationDeletionWithKeys(SANDBOX);

//        delete app with subscription and keys
        apiName = API_NAME_PREFIX +  WITH_SUBSCRIPTION_SUFFIX + KEY_GENERATION_SUFFIX;
        applicationName = API_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX + KEY_GENERATION_SUFFIX;

        createApplication(applicationName);
        createAndPublishAPI(apiName);
        subscribeToAPI(apiName, applicationName);
        keyGenerationForApplication(applicationName, PRODUCTION);
        keyGenerationForApplication(applicationName, SANDBOX);
        deleteApplication(applicationName);
        verifyRemovalOfSubscriptionToAPI(apiName);
    }

    @Test(description = "4.1.3.3", dependsOnMethods = {"testDeleteApplication"})
    public void testRecreateDeletedApplication() throws Exception {
        createApplication(APPLICATION_NAME_PREFIX);
    }

    @Test(description = "4.1.3.4", dependsOnMethods = {"testRecreateDeletedApplication"})
    public void testKeyGenerationForRecreateDeletedApplication() throws Exception {
        keyGenerationForApplication(APPLICATION_NAME_PREFIX, PRODUCTION);
        keyGenerationForApplication(APPLICATION_NAME_PREFIX, SANDBOX);
    }

    private void createApplication(String applicationName) throws Exception {
        HttpResponse addApplicationResponse = apiStore.addApplication(applicationName,
                        APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", APPLICATION_DESCRIPTION);
        applicationsList.add(applicationName);
        verifyResponse(addApplicationResponse);
        assertEquals(new JSONObject(addApplicationResponse.getData()).get("status"), STATUS_APPROVED,
                "Application creation failed for application: " + applicationName);
    }

    private void deleteApplication(String applicationName) throws Exception {
        HttpResponse deleteResponse = apiStore.removeApplication(applicationName);
        verifyResponse(deleteResponse);
        verifyApplicationDeletionFromStore(applicationName);
    }

    private void verifyApplicationDeletionFromStore(String applicationName) throws Exception {
//        verify whether the application doesn't exist in store
        HttpResponse getApplicationsResponse = apiStore.getAllApplications();
        log.info("Verify application does not exist in store response code : " +
                getApplicationsResponse.getResponseCode());
        log.info("Verify application does not exist in store response message : " +
                getApplicationsResponse.getData());
        assertFalse(getApplicationsResponse.getData().contains(applicationName),
                "Application still available in store: " + applicationName);
    }

    private void createAPI(String apiName) throws Exception {
        APIRequest apiRequest = new APIRequest(apiName, "/" + apiName, "public", API_VERSION,
                "/find", TIER_GOLD, new URL("http://ws.cdyne.com/phoneverify/phoneverify.asmx"));

        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        apiList.add(apiName);
        verifyResponse(serviceResponse);
        verifyApiCreation(apiName);
    }

    private void verifyApiCreation(String apiName) throws Exception {
        HttpResponse apiInfo = apiPublisher.getAPI(apiName, CREATOR_PUBLISHER_USERNAME, API_VERSION);
        verifyResponse(apiInfo);
    }

    private void createAndPublishAPI(String apiName) throws Exception {
        createAPI(apiName);
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, CREATOR_PUBLISHER_USERNAME, APILifeCycleState.PUBLISHED);
        HttpResponse creationResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        log.info("API publish response code: " + creationResponse.getResponseCode());
        log.info("API publish response data: " + creationResponse.getData());
        assertTrue(creationResponse.getData().contains("PUBLISHED"), "API has not been created in publisher");
    }

    private void subscribeToAPI(String apiName, String applicationName) throws Exception {
        isAPIVisibleInStore(apiName, apiStore);
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, CREATOR_PUBLISHER_USERNAME);
        subscriptionRequest.setApplicationName(applicationName);
        subscriptionRequest.setTier(TIER_GOLD);
        HttpResponse serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);
    }

    private void verifyRemovalOfSubscriptionToAPI(String apiName) throws Exception {
//        verify subscription is removed from api
        HttpResponse apiInfo = apiPublisher.getAPI(apiName, CREATOR_PUBLISHER_USERNAME, API_VERSION);
        verifyResponse(apiInfo);
        assertEquals(0, new JSONObject(apiInfo.getData()).getJSONObject("api").getInt("subs"),
                "Incorrect subscription count for api \'" + apiName + "\'");
    }

    private void applicationDeletionWithKeys(String keyType) throws Exception {
        applicationName = APPLICATION_NAME_PREFIX + KEY_GENERATION_SUFFIX + keyType;

        createApplication(applicationName);
        keyGenerationForApplication(applicationName, keyType);
        deleteApplication(applicationName);
    }

    private void keyGenerationForApplication(String applicationName, String keyType) throws Exception {
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(applicationName);
        appKeyRequestGenerator.setKeyType(keyType);
        HttpResponse responseString = apiStore.generateApplicationKey(appKeyRequestGenerator);
        verifyResponse(responseString);
        JSONObject responseStringJson = new JSONObject(responseString.getData());
        JSONObject key = responseStringJson.getJSONObject("data").getJSONObject("key");
        assertEquals(key.getString("keyState"),
                STATUS_APPROVED, keyType.toLowerCase() + " key generation failed for application:  "
                        + applicationName);
        assertEquals(new JSONObject(key.getString("appDetails")).get("key_type"), keyType,
                keyType.toLowerCase() + " key generation failed for application:  " + applicationName);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        for (String name : applicationsList) {
            apiStore.removeApplication(name);
        }
        for (String name : apiList) {
            apiPublisher.deleteAPI(name, API_VERSION, CREATOR_PUBLISHER_USERNAME);
        }
        deleteUser(CREATOR_PUBLISHER_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        deleteUser(SUBSCRIBER_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
    }
}