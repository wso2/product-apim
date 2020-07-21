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

import com.google.gson.Gson;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.api.dto.CustomAttributeDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertTrue;

public class SubscribeToAssignedTiersTestCase extends ScenarioTestBase {

    private String apiNameSingleTier = "Single_Tier_API";
    private String apiNameMultipleTier = "Multi_Tier_API";
    private String apiRepublishedWithDiffTier = "Republish_Diff_Tier_API";
    private String apiNameCustomTier = "Custom_Tier_API";
    private String apiContextSingleTier = "/singleTierApi";
    private String apiContextMultipleTier = "/multiTierApi";
    private String apiContextRepublishedWithDiffTier = "/republishDiffTier";
    private String apiContextCustomTier = "/customTierApi";
    private String endpointUrl = "http://test";
    private String singleTier = "Gold";
    private String goldTier = "Gold";
    private String silverTier = "Silver";
    private String multiTier = "Gold,Silver";
    private String apiVersion = "1.0.0";
    private String customTier = "CustomTier3";
    private static String apiId1;
    private static String apiId2;
    private static String apiId3;
    private static String apiId4;

    private static String applicationId1;
    private static String applicationId2;
    private static String applicationId3;
    private static String applicationId4;
    private static String applicationId5;

    private static String policyId;

    private String applicationNameSingleTier = "SingleTierApplication";
    private String applicationNameMultipleTier = "MultipleTierApplication";
    private String applicationNameCustomTier = "CustomTierApplication";
    private String applicationDescription = "Application_Description";
    private String applicationNameBeforeAPIRepublish = "BeforeRepublishApplication";
    private String applicationNameAfterAPIRepublish = "AfterRepublishApplication";

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";


    @Factory(dataProvider = "userModeDataProvider")
    public SubscribeToAssignedTiersTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                    ADMIN_USERNAME, ADMIN_PW);
            createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, ADMIN_USERNAME, ADMIN_PW);
        }

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // create user in wso2.com tenant
            addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_USERNAME, ADMIN_PW);
            if (isActivated(ScenarioTestConstants.TENANT_WSO2)) {
                //Add and activate wso2.com tenant
                createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                        TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, TENANT_ADMIN_USERNAME,
                        TENANT_ADMIN_PW);
            }
        }
        super.init(userMode);
    }

    @Test(description = "7.1.1.1")
    public void testSingleTierSubscriptionAvailability() throws Exception {

        APIRequest apiRequest = new APIRequest(apiNameSingleTier, apiContextSingleTier, new URL(endpointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(singleTier);
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId1 = serviceResponse.getData();
        restAPIPublisher.changeAPILifeCycleStatus(apiId1, APILifeCycleAction.PUBLISH.getAction(), null);

        // wait till API indexed in Store
        isAPIVisibleInStore(apiId1);

        //Create Application for single tier
        HttpResponse addApplicationResponse = restAPIStore.createApplication(applicationNameSingleTier,
                applicationDescription, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        applicationId1 = addApplicationResponse.getData();
        verifyResponse(addApplicationResponse);
        //Subscribe to the API with single tier
        HttpResponse subscriptionResponse = restAPIStore.createSubscription(apiId1, applicationId1, singleTier);
        verifyResponse(subscriptionResponse);
    }

    @Test(description = "7.1.1.2")
    public void testSingleCustomTierSubscriptionAvailability() throws Exception {
        SubscriptionThrottlePolicyDTO subscriptionThrottlePolicyDTO = new SubscriptionThrottlePolicyDTO();
        subscriptionThrottlePolicyDTO.setBillingPlan("FREE");
        List<CustomAttributeDTO> customAttributeDTOS = new ArrayList<>();
        subscriptionThrottlePolicyDTO.setCustomAttributes(customAttributeDTOS);
        subscriptionThrottlePolicyDTO.setPolicyName(customTier);
        subscriptionThrottlePolicyDTO.setDescription("6000 requests per minute");
        subscriptionThrottlePolicyDTO.setStopOnQuotaReach(true);

        RequestCountLimitDTO requestCountLimitDTO = new RequestCountLimitDTO();
        requestCountLimitDTO.setRequestCount(Long.valueOf(6000));
        requestCountLimitDTO.setTimeUnit("min");
        requestCountLimitDTO.setUnitTime(1);


        ThrottleLimitDTO throttleLimitDTO = new ThrottleLimitDTO();
        throttleLimitDTO.setRequestCount(requestCountLimitDTO);
        throttleLimitDTO.setType(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT);

        subscriptionThrottlePolicyDTO.setDefaultLimit(throttleLimitDTO);
        subscriptionThrottlePolicyDTO.setRateLimitTimeUnit("min");
        subscriptionThrottlePolicyDTO.setRateLimitCount(0);
        subscriptionThrottlePolicyDTO.setRateLimitTimeUnit("sec");

        org.wso2.am.integration.test.HttpResponse policyAddResponse = restAPIAdmin
                .addSubscriptionPolicy(subscriptionThrottlePolicyDTO, "application/json");
        policyId = policyAddResponse.getData();

        APIRequest apiRequest = new APIRequest(apiNameCustomTier, apiContextCustomTier, new URL(endpointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(customTier);
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId2 = serviceResponse.getData();
        restAPIPublisher.changeAPILifeCycleStatus(apiId2, APILifeCycleAction.PUBLISH.getAction(), null);

        // wait till API indexed in Store
        isAPIVisibleInStore(apiId2);

        //Create Application for single tier
        HttpResponse addApplicationResponse = restAPIStore.createApplication(applicationNameCustomTier,
                applicationDescription, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        verifyResponse(addApplicationResponse);
        applicationId2 = addApplicationResponse.getData();
        //Subscribe to the API with single tier
        HttpResponse subscriptionResponse = restAPIStore.createSubscription(apiId2, applicationId2, customTier);
        verifyResponse(subscriptionResponse);
    }

    @Test(description = "7.1.1.3", dependsOnMethods = "testSingleCustomTierSubscriptionAvailability")
    public void testMultipleTierSubscriptionAvailability() throws Exception {

        APIRequest apiRequest = new APIRequest(apiNameMultipleTier, apiContextMultipleTier, new URL(endpointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(multiTier);
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId3 = serviceResponse.getData();
        restAPIPublisher.changeAPILifeCycleStatus(apiId3, APILifeCycleAction.PUBLISH.getAction(), null);

        // wait till API indexed in Store
        isAPIVisibleInStore(apiId3);

        //Create Application for single tier
        HttpResponse addApplicationResponse = restAPIStore.createApplication(applicationNameMultipleTier,
                applicationDescription, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        verifyResponse(addApplicationResponse);
        applicationId3 = addApplicationResponse.getData();
        //Subscribe to the API with single tier
        HttpResponse subscriptionResponse = restAPIStore.createSubscription(apiId3, applicationId3, goldTier);
        verifyResponse(subscriptionResponse);
        restAPIStore.removeSubscription(subscriptionResponse.getData());
        HttpResponse subscriptionResponse2 = restAPIStore.createSubscription(apiId3, applicationId3, silverTier);
        verifyResponse(subscriptionResponse2);
    }

    @Test(description = "7.1.1.4", dependsOnMethods = "testMultipleTierSubscriptionAvailability")
    public void testRepublishWithDifferentTier() throws Exception {

        APIRequest apiRequest = new APIRequest(apiRepublishedWithDiffTier, apiContextRepublishedWithDiffTier, new URL(endpointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(goldTier);
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId4 = serviceResponse.getData();
        restAPIPublisher.changeAPILifeCycleStatus(apiId4, APILifeCycleAction.PUBLISH.getAction(), null);

        // wait till API indexed in Store
        isAPIVisibleInStore(apiId4);

        //Create Application for single tier
        HttpResponse addApplicationResponse = restAPIStore.createApplication(applicationNameBeforeAPIRepublish,
                applicationDescription, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        applicationId4 = addApplicationResponse.getData();
        verifyResponse(addApplicationResponse);
        //Subscribe to the API with single tier
        HttpResponse subscriptionResponse = restAPIStore.createSubscription(apiId4, addApplicationResponse.getData(), goldTier);
        verifyResponse(subscriptionResponse);

        //Update API with silver tier
        HttpResponse response = restAPIPublisher.getAPI(apiId4);
        Gson g = new Gson();
        APIDTO apiDTO2 = g.fromJson(response.getData(), APIDTO.class);
        List<String> tierList = new ArrayList<>();
        tierList.add(silverTier);
        apiDTO2.setPolicies(tierList);
        restAPIPublisher.updateAPI(apiDTO2, apiId4);
        //Republish API
        restAPIPublisher.changeAPILifeCycleStatus(apiId4, APILifeCycleAction.PUBLISH.getAction(), null);

        //Create new application to subscribe to updated API
        HttpResponse newAddApplicationResponse = restAPIStore.createApplication(applicationNameAfterAPIRepublish,
                applicationDescription, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        applicationId5 = newAddApplicationResponse.getData();
        verifyResponse(newAddApplicationResponse);

        //subscribe new app to updated tier
        HttpResponse subscriptionResponseSilver = restAPIStore.createSubscription(apiId4, applicationId5, silverTier);
        verifyResponse(subscriptionResponseSilver);

        SubscriptionListDTO subscriptionListDTO = restAPIStore.getAllSubscriptionsOfApplication(applicationId5);
        assertTrue(subscriptionListDTO.getList().get(0).getApplicationInfo().getName().equals(applicationNameAfterAPIRepublish));
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId1);
        restAPIStore.deleteApplication(applicationId2);
        restAPIStore.deleteApplication(applicationId3);
        restAPIStore.deleteApplication(applicationId4);
        restAPIStore.deleteApplication(applicationId5);

        restAPIPublisher.deleteAPI(apiId1);
        restAPIPublisher.deleteAPI(apiId2);
        restAPIPublisher.deleteAPI(apiId3);
        restAPIPublisher.deleteAPI(apiId4);

        restAPIAdmin.deleteSubscriptionPolicy(policyId);

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
        }

    }

    @DataProvider
    public static Object[][] userModeDataProvider() throws Exception {
        setup();
        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_USER},
                // new Object[]{TestUserMode.TENANT_USER},
        };
    }


}
