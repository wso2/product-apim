/*
 *Copyright (c), WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.scenario.tests.define.subscription.tiers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.Properties;

public class SubscribeToAssignedTiersTestCase extends ScenarioTestBase {

    private final Log log = LogFactory.getLog(SubscribeToAssignedTiersTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String publisherURL;
    private String storeURL;
    private APIRequest apiRequest;

    private String apiNameSingleTier = "Single_Tier_API";
    private String apiNameMultipleTier = "Multi_Tier_API";
    private String apiRepublishedWithDiffTier = "Republish_Diff_Tier_API";
    private String apiContextSingleTier = "/singleTierApi";
    private String apiContextMultipleTier = "/multiTierApi";
    private String apiContextRepublishedWithDiffTier = "/republishDiffTier";
    private String endpointUrl = "http://test";
    private String singleTier = "Gold";
    private String goldTier = "Gold";
    private String silverTier = "Silver";
    private String multiTier = "Gold,Silver";
    private String apiVersion = "1.0.0";
    private String apiVisibility = "public";
    private String providerName = "admin";
    private String apiResource = "/groups";
    private Properties infraProperties;
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    private String applicationNameSingleTier = "SingleTierApplication";
    private String applicationNameMultipleTier = "MultipleTierApplication";
    private String applicationDescription = "Application_Description";
    private String applicationNameBeforeAPIRepublish = "BeforeRepublishApplication";
    private String applicationNameAfterAPIRepublish = "AfterRepublishApplication";

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        infraProperties = getDeploymentProperties();
        publisherURL = infraProperties.getProperty(PUBLISHER_URL);
        storeURL = infraProperties.getProperty(STORE_URL);

        if (publisherURL == null) {
            publisherURL = "https://localhost:9443/publisher";
        }
        if (storeURL == null) {
            storeURL = "https://localhost:9443/store";
        }
        setKeyStoreProperties();
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiStore = new APIStoreRestClient(storeURL);
        apiPublisher.login(ADMIN_USERNAME, ADMIN_PASSWORD);
        apiStore.login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    @Test(description = "7.1.1.1")
    public void testSingleTierSubscriptionAvailability() throws Exception {
        apiRequest = new APIRequest(apiNameSingleTier, apiContextSingleTier, apiVisibility, apiVersion, apiResource,
                singleTier, new URL(endpointUrl));
        //Create API with single tier
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);
        //Publish API
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiNameSingleTier, providerName,
                APILifeCycleState.PUBLISHED);
        HttpResponse publishServiceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        Assert.assertTrue(publishServiceResponse.getData().contains(APILifeCycleState.PUBLISHED.getState()));
        //Create Application for single tier
        HttpResponse addApplicationResponse = apiStore
                .addApplication(applicationNameSingleTier, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "",
                        applicationDescription);
        verifyResponse(addApplicationResponse);
        //Subscribe to the API with single tier
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiNameSingleTier, apiVersion, providerName,
                applicationNameSingleTier, goldTier);
        HttpResponse subscriptionResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(subscriptionResponse);
    }

    @Test(description = "7.1.1.2")
    public void testMultipleTierSubscriptionAvailability() throws Exception {
        apiRequest = new APIRequest(apiNameMultipleTier, apiContextMultipleTier, apiVisibility, apiVersion, apiResource,
                multiTier, new URL(endpointUrl));
        //Create API with multiple tiers
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);
        //Publish API
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiNameMultipleTier, providerName,
                APILifeCycleState.PUBLISHED);
        HttpResponse publishServiceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        Assert.assertTrue(publishServiceResponse.getData().contains(APILifeCycleState.PUBLISHED.getState()));
        //Create Application for multiple tiers
        HttpResponse addApplicationResponse = apiStore
                .addApplication(applicationNameMultipleTier, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "",
                        applicationDescription);
        verifyResponse(addApplicationResponse);
        //Subscribe to the API with multiple tiers
        //subscribe with gold tier
        SubscriptionRequest subscriptionRequestGold = new SubscriptionRequest(apiNameMultipleTier, apiVersion,
                providerName, applicationNameMultipleTier, goldTier);
        HttpResponse subscriptionResponseGold = apiStore.subscribe(subscriptionRequestGold);
        verifyResponse(subscriptionResponseGold);
        //remove previous subscription
        apiStore.removeAPISubscriptionByName(apiNameMultipleTier, apiVersion, providerName,
                applicationNameMultipleTier);
        //subscribe with silver tier
        SubscriptionRequest subscriptionRequestSilver = new SubscriptionRequest(apiNameMultipleTier, apiVersion,
                providerName, applicationNameMultipleTier, silverTier);
        HttpResponse subscriptionResponseSilver = apiStore.subscribe(subscriptionRequestSilver);
        verifyResponse(subscriptionResponseSilver);
    }

    @Test(description = "7.1.1.3")
    public void testRepublishWithDifferentTier() throws Exception {
        apiRequest = new APIRequest(apiRepublishedWithDiffTier, apiContextRepublishedWithDiffTier, apiVisibility,
                apiVersion, apiResource, goldTier, new URL(endpointUrl));
        //Create API with gold tier first
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);
        //Publish API with gold tier
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiRepublishedWithDiffTier, providerName,
                APILifeCycleState.PUBLISHED);
        HttpResponse publishServiceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        Assert.assertTrue(publishServiceResponse.getData().contains(APILifeCycleState.PUBLISHED.getState()));
        //Create Application to subscribe before republishing
        HttpResponse addApplicationResponse = apiStore
                .addApplication(applicationNameBeforeAPIRepublish, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                        "", applicationDescription);
        verifyResponse(addApplicationResponse);
        //Subscribe to created API before republish
        SubscriptionRequest subscriptionRequestGold = new SubscriptionRequest(apiRepublishedWithDiffTier, apiVersion,
                providerName, applicationNameBeforeAPIRepublish, goldTier);
        HttpResponse subscriptionResponseGold = apiStore.subscribe(subscriptionRequestGold);
        verifyResponse(subscriptionResponseGold);
        //Update API with silver tier
        APIRequest requestUpdated = new APIRequest(apiRepublishedWithDiffTier, apiContextRepublishedWithDiffTier,
                apiVisibility, apiVersion, apiResource, silverTier, new URL(endpointUrl));
        HttpResponse updateAPIResponse = apiPublisher.updateAPI(requestUpdated);
        verifyResponse(updateAPIResponse);
        //Republish API
        APILifeCycleStateRequest republishRequest = new APILifeCycleStateRequest(apiRepublishedWithDiffTier, providerName,
                APILifeCycleState.PUBLISHED);
        HttpResponse rePublishServiceResponse = apiPublisher.changeAPILifeCycleStatus(republishRequest);
        Assert.assertTrue(rePublishServiceResponse.getData().contains(APILifeCycleState.PUBLISHED.getState()));
        //Create new application to subscribe to updated API
        HttpResponse newAddApplicationResponse = apiStore
                .addApplication(applicationNameAfterAPIRepublish, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                        "", applicationDescription);
        verifyResponse(newAddApplicationResponse);
        //subscribe new app to updated tier
        SubscriptionRequest subscriptionRequestSilver = new SubscriptionRequest(apiRepublishedWithDiffTier, apiVersion,
                providerName, applicationNameAfterAPIRepublish, silverTier);
        HttpResponse subscriptionResponseSilver = apiStore.subscribe(subscriptionRequestSilver);
        verifyResponse(subscriptionResponseSilver);
        //check which tier the previous application has
        HttpResponse subscriptionTierResponse = apiStore
                .getAllSubscriptionsOfApplication(applicationNameBeforeAPIRepublish);
        JSONObject responseData = new JSONObject(subscriptionTierResponse.getData());
        JSONObject applicationData = responseData.getJSONObject("subscriptions").getJSONArray("applications")
                .getJSONObject(0);
        JSONObject subscriptionOfApplication = applicationData.getJSONArray("subscriptions").getJSONObject(0);
        Assert.assertEquals(applicationData.getString("name"), applicationNameBeforeAPIRepublish);
        Assert.assertEquals(subscriptionOfApplication.getString("tier"), goldTier);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        //Remove single tier artifacts
        apiStore.removeApplication(applicationNameSingleTier);
        apiPublisher.deleteAPI(apiNameSingleTier, apiVersion, providerName);
        //Remove multiple tier artifacts
        apiStore.removeApplication(applicationNameMultipleTier);
        apiPublisher.deleteAPI(apiNameMultipleTier, apiVersion, providerName);
        //Remove artifacts used for republish test
        apiStore.removeApplication(applicationNameBeforeAPIRepublish);
        apiStore.removeApplication(applicationNameAfterAPIRepublish);
        apiPublisher.deleteAPI(apiRepublishedWithDiffTier, apiVersion, providerName);
    }
}
