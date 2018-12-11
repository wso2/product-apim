/*
 *Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
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

import java.net.URLEncoder;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DeleteRegisteredApplicationTestCases extends ScenarioTestBase {
    private APIStoreRestClient apiStore;
    private APIPublisherRestClient apiPublisher;
    private List<String> applicationsList = new ArrayList<>();
    private List<String> apiList = new ArrayList<>();
    private static final String API_NAME_PREFIX = "APIForDeleteApplication_";
    private static final String API_VERSION = "1.0.0";
    private static final String TIER_GOLD = "Gold";
    private static final String ADMIN_LOGIN_USERNAME = "admin";
    private static final String ADMIN_LOGIN_PW = "admin";
    private static final String DEFAULT_URL_PREFIX = "https://localhost:9443/";
    private static final String PUBLISHER_URL_SUFFIX = "publisher/";
    private static final String UTF_8 = "UTF-8";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String APPLICATION_NAME_PREFIX = "ApplicationDeletion_";
    private static final String KEY_GENERATION_SUFFIX = "KeyGeneration";
    private static final String WITH_SUBSCRIPTION_SUFFIX = "WithSubscription";
    private static final String APPLICATION_DESCRIPTION = "New application description";
    private static final String ERROR_APPLICATION_KEY_GENERATION_FAILED = " key generation failed for application:  ";
    private static final String DATA = "data";
    private static final String KEY = "key";
    private static final String KEY_STATE = "keyState";
    private static final String APP_DETAILS = "appDetails";
    private static final String KEY_TYPE = "key_type";
    private static final String PRODUCTION = "PRODUCTION";
    private static final String SANDBOX = "SANDBOX";

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {
        Properties infraProperties = getDeploymentProperties();
        String storeURL = infraProperties.getProperty(STORE_URL);
        if (storeURL == null) {
            storeURL = DEFAULT_URL_PREFIX;
        }
        setKeyStoreProperties();
        apiStore = new APIStoreRestClient(storeURL);
        apiStore.login(ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);

        String publisherURL = infraProperties.getProperty(PUBLISHER_URL);
        if (publisherURL == null) {
            publisherURL = DEFAULT_URL_PREFIX + PUBLISHER_URL_SUFFIX;
        }
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login(ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
    }

    @Test(description = "4.1.2.1")
    public void testDeleteApplication() throws Exception {
        createApplication(APPLICATION_NAME_PREFIX);
        deleteApplication(APPLICATION_NAME_PREFIX);
    }

    @Test(description = "4.1.2.2")
    public void testDeleteApplicationWithSubscription() throws Exception {
        createApplication(APPLICATION_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX);
        createAndPublishAPI(API_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX);
        subscribeToAPI(API_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX,
                APPLICATION_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX);
        deleteApplication(APPLICATION_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX);
        verifyRemovalOfSubscriptionToAPI(API_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX);
    }

    @Test(description = "4.1.2.3")
    public void testDeleteApplicationWithKeys() throws Exception {
//        test deletion of applications with keys
        applicationDeletionWithKeys(PRODUCTION);
        applicationDeletionWithKeys(SANDBOX);
//        test deletion of applications with subscription and keys
        createApplication(APPLICATION_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX + KEY_GENERATION_SUFFIX);
        createAndPublishAPI(API_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX + KEY_GENERATION_SUFFIX);
        subscribeToAPI(API_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX + KEY_GENERATION_SUFFIX,
                APPLICATION_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX + KEY_GENERATION_SUFFIX);
        keyGenerationForApplication(APPLICATION_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX
                + KEY_GENERATION_SUFFIX, PRODUCTION);
        keyGenerationForApplication(APPLICATION_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX
                + KEY_GENERATION_SUFFIX, SANDBOX);
        deleteApplication(APPLICATION_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX + KEY_GENERATION_SUFFIX);
        verifyRemovalOfSubscriptionToAPI(API_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX + KEY_GENERATION_SUFFIX);
    }

    @Test(description = "4.1.2.4", dependsOnMethods = {"testDeleteApplication"})
    public void testRecreateDeletedApplication() throws Exception {
        createApplication(APPLICATION_NAME_PREFIX);
    }

    @Test(description = "4.1.2.5", dependsOnMethods = {"testRecreateDeletedApplication"})
    public void testRecreateDeletedApplicationKeyGeneration() throws Exception {
        keyGenerationForApplication(APPLICATION_NAME_PREFIX, PRODUCTION);
        keyGenerationForApplication(APPLICATION_NAME_PREFIX, SANDBOX);
    }

    private void createApplication(String applicationName) throws Exception {
        HttpResponse addApplicationResponse = apiStore
                .addApplication(URLEncoder.encode(applicationName, UTF_8),
                        URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8),
                        "", URLEncoder.encode(APPLICATION_DESCRIPTION, UTF_8));
        applicationsList.add(applicationName);
        verifyResponse(addApplicationResponse);
        assertEquals(new JSONObject(addApplicationResponse.getData()).get("status"), STATUS_APPROVED,
                "Application creation failed for application: " + applicationName);
    }

    private void deleteApplication(String applicationName) throws Exception {
        HttpResponse deleteResponse = apiStore.removeApplication(URLEncoder.encode(applicationName, UTF_8));
        verifyResponse(deleteResponse);
        verifyApplicationDeletionFromStore(applicationName);
    }

    private void verifyApplicationDeletionFromStore(String applicationName) throws Exception {
//        verify whether the application doesn't exist in store
        HttpResponse allApplicationsResponse = apiStore.getAllApplications();
        JSONArray applications = new JSONObject(allApplicationsResponse.getData()).getJSONArray("applications");
        boolean success = true;
        for (int i = 0; i < applications.length(); i++) {
            JSONObject application = applications.getJSONObject(i);
            if(application.getString("name").equals(applicationName)) {
                success = false;
            }
        }
        assertTrue(success, "Application still available in store: " + applicationName);
    }

    private void createAPI(String apiName) throws Exception {
        APIRequest apiRequest = new APIRequest(apiName, "/" + apiName,
                "public", API_VERSION, "/find", TIER_GOLD,
                new URL("http://ws.cdyne.com/phoneverify/phoneverify.asmx"));

        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        apiList.add(apiName);
        verifyResponse(serviceResponse);
        verifyApiCreation(apiName);
    }

    private void verifyApiCreation(String apiName) throws Exception {
        HttpResponse apiInfo = apiPublisher.getAPI(apiName, ADMIN_LOGIN_USERNAME, API_VERSION);
        verifyResponse(apiInfo);
    }

    private void createAndPublishAPI(String apiName) throws Exception {
        createAPI(apiName);
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, ADMIN_LOGIN_USERNAME, APILifeCycleState.PUBLISHED);
        HttpResponse creationResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        assertTrue(creationResponse.getData().contains("PUBLISHED"), "API has not been created in publisher");
    }

    private void subscribeToAPI(String apiName, String applicationName) throws Exception {
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName,
                ADMIN_LOGIN_USERNAME);
        subscriptionRequest.setApplicationName(applicationName);
        subscriptionRequest.setTier(TIER_GOLD);
        HttpResponse serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);
    }

    private void verifyRemovalOfSubscriptionToAPI(String apiName) throws Exception {
//        verify subscription is removed from api
        HttpResponse apiInfo = apiPublisher.getAPI(apiName, ADMIN_LOGIN_USERNAME, API_VERSION);
        verifyResponse(apiInfo);
        assertEquals(0, new JSONObject(apiInfo.getData()).getJSONObject("api").getInt("subs"),
                "Incorrect subscription count for api \'" + apiName + "\'");
    }

    private void applicationDeletionWithKeys(String keyType) throws Exception {
        createApplication(APPLICATION_NAME_PREFIX + KEY_GENERATION_SUFFIX + keyType);
        keyGenerationForApplication(APPLICATION_NAME_PREFIX + KEY_GENERATION_SUFFIX + keyType, keyType);
        deleteApplication(APPLICATION_NAME_PREFIX + KEY_GENERATION_SUFFIX + keyType);
    }

    private void keyGenerationForApplication(String applicationName, String keyType) throws Exception {
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(applicationName);
        appKeyRequestGenerator.setKeyType(keyType);
        HttpResponse responseString = apiStore.generateApplicationKey(appKeyRequestGenerator);
        verifyResponse(responseString);
        JSONObject responseStringJson = new JSONObject(responseString.getData());
        assertEquals(responseStringJson.getJSONObject(DATA).getJSONObject(KEY).getString(KEY_STATE), STATUS_APPROVED,
                keyType.toLowerCase() + ERROR_APPLICATION_KEY_GENERATION_FAILED + applicationName);
        assertEquals(new JSONObject(responseStringJson.getJSONObject(DATA).getJSONObject(KEY).getString(APP_DETAILS))
                .get(KEY_TYPE), keyType, keyType.toLowerCase() + ERROR_APPLICATION_KEY_GENERATION_FAILED
                + applicationName);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        for (String name : applicationsList) {
            apiStore.removeApplication(URLEncoder.encode(name, UTF_8));
        }
        for (String name : apiList) {
            apiPublisher.deleteAPI(name, API_VERSION, ADMIN_LOGIN_USERNAME);
        }
    }
}