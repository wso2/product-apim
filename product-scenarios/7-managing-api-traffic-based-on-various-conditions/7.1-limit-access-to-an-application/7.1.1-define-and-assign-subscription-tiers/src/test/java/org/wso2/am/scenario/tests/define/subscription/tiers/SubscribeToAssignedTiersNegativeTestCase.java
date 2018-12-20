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

public class SubscribeToAssignedTiersNegativeTestCase extends ScenarioTestBase {

    private final Log log = LogFactory.getLog(SubscribeToAssignedTiersTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String publisherURL;
    private String storeURL;
    private APIRequest apiRequest;

    private String apiNameNoTiers = "API_1";
    private String apiContextNoTiers = "/api1";
    private String apiNameSubscribeToNotAssigned = "API_2";
    private String apiContextSubscribeToNotAssigned = "/api2";
    private String endpointUrl = "http://test";
    private String goldTier = "Gold";
    private String silverTier = "Silver";
    private String apiVersion = "1.0.0";
    private String apiVisibility = "public";
    private String providerName = "admin";
    private String apiResource = "/groups";
    private Properties infraProperties;
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    private String applicationName = "NewApplication";
    private String applicationDescription = "Application_Description";

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

    @Test(description = "7.1.1.4")
    public void testCreateAPIWithNoSubscriptionTiers() throws Exception {
        apiRequest = new APIRequest(apiNameNoTiers, apiContextNoTiers, apiVisibility, apiVersion, apiResource, null,
                new URL(endpointUrl));
        //Create API without a tier
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        Assert.assertNotNull(serviceResponse, "Response object is null");
        JSONObject responseData = new JSONObject(serviceResponse.getData());
        Assert.assertTrue(responseData.getBoolean("error"), "Error message received " + serviceResponse.getData());
        Assert.assertTrue(responseData.getString("message").contains("No tier defined for the API"));
    }

    @Test(description = "7.1.1.5")
    public void testSubscribeWithTierNotAssignedToAPI() throws Exception {
        apiRequest = new APIRequest(apiNameSubscribeToNotAssigned, apiContextSubscribeToNotAssigned, apiVisibility,
                apiVersion, apiResource, goldTier, new URL(endpointUrl));
        //Create API with gold tier
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);
        //Publish API
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiNameSubscribeToNotAssigned,
                providerName, APILifeCycleState.PUBLISHED);
        HttpResponse publishServiceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        Assert.assertTrue(publishServiceResponse.getData().contains(APILifeCycleState.PUBLISHED.getState()));
        //Create Application
        HttpResponse addApplicationResponse = apiStore
                .addApplication(applicationName, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "",
                        applicationDescription);
        verifyResponse(addApplicationResponse);
        //Subscribe to the API with silver tier
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiNameSubscribeToNotAssigned, apiVersion,
                providerName, applicationName, silverTier);
        HttpResponse subscriptionResponse = apiStore.subscribe(subscriptionRequest);
        Assert.assertNotNull(subscriptionResponse, "Response object is null");
        JSONObject responseData = new JSONObject(subscriptionResponse.getData());
        Assert.assertTrue(responseData.getBoolean("error"), "Error message received " + subscriptionResponse.getData());
        Assert.assertTrue(responseData.getString("message")
                .contains("Tier Silver is not allowed for API " + apiNameSubscribeToNotAssigned + "-" + apiVersion));
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        //Remove artifacts
        apiStore.removeApplication(applicationName);
        apiPublisher.deleteAPI(apiNameSubscribeToNotAssigned, apiVersion, providerName);
    }
}
