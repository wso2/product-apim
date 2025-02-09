/*
 * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com/).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.publisher;

import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyPermissionDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This test case tests the ability to change the subscription business plan of an existing subscription forcefully.
 */
public class ChangeSubscriptionBusinessPlanForcefullyTestCase extends APIMIntegrationBaseTest {

    // Constants
    private final String CREATOR_USER_NAME = "creator";
    private final String PUBLISHER_USER_NAME = "publisher";
    private final String SUBSCRIBER_USER_NAME = "subscriber";
    private final String DEFAULT_WF_EXTENSIONS_XML_REG_CONFIG_LOCATION =
            "/_system/governance/apimgt/applicationdata/workflow-extensions.xml";
    private final String CUSTOM_SUBSCRIPTION_POLICY = "TestPolicyOne";

    // Instance variables
    private String apiId;
    private String subscriptionId;
    private String originalWFExtensionsXML;
    private String tenantDomain;

    // DTOs and Clients
    private ApplicationDTO tenantApplication;
    private SubscriptionDTO restAPIPublisherSubscriptionDTO;
    private ResourceAdminServiceClient resourceAdminServiceClient;
    private RestAPIPublisherImpl publisherRestAPIForPublisher;
    private RestAPIStoreImpl storeRestAPIForSubscriber;
    private ApiResponse<SubscriptionThrottlePolicyDTO> testPolicyOne;
    private final List<String> roleList = new ArrayList<>();

    @Factory(dataProvider = "userModeDataProvider")
    public ChangeSubscriptionBusinessPlanForcefullyTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        final String apiName = "TestApi";
        final String apiContext = "/testContext";
        final String apiEndpoint = "http://localhost:9443";
        final String apiVersion = "1.0.0";
        final String tenantApplicationName = "TestApp";
        final String password = "password123";
        tenantDomain = MultitenantUtils.getTenantDomain(user.getUserName());

        // Create a new subscription throttling policy
        roleList.add(APIMIntegrationConstants.APIM_INTERNAL_ROLE.SUBSCRIBER);
        RequestCountLimitDTO requestCountLimit = DtoFactory.createRequestCountLimitDTO("min", 1, 50L);
        ThrottleLimitDTO defaultLimit = DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT,
                requestCountLimit, null);
        SubscriptionThrottlePolicyPermissionDTO permissions = DtoFactory.createSubscriptionThrottlePolicyPermissionDTO(
                SubscriptionThrottlePolicyPermissionDTO.PermissionTypeEnum.ALLOW, roleList);
        SubscriptionThrottlePolicyDTO requestCountPolicyDTO = DtoFactory.createSubscriptionThrottlePolicyDTO(
                CUSTOM_SUBSCRIPTION_POLICY, "Test Policy", "This is a test subscription throttle policy",
                true, defaultLimit, 400, 10, -1, "NA", null, false, "FREE", 0, permissions);
        testPolicyOne = restAPIAdmin.addSubscriptionThrottlingPolicy(requestCountPolicyDTO);

        // Create API Creator user and create an API
        userManagementClient.addUser(CREATOR_USER_NAME, password, new String[]{APIMIntegrationConstants.
                APIM_INTERNAL_ROLE.CREATOR}, CREATOR_USER_NAME);
        RestAPIPublisherImpl publisherRestAPIForCreator = new RestAPIPublisherImpl(CREATOR_USER_NAME, password,
                tenantDomain, publisherURLHttps);

        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(apiEndpoint));
        apiRequest.setVersion(apiVersion);
        // Enable only GOLD, SILVER, BRONZE and TestPolicyOne as valid tiers for the API
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.GOLD + "," + APIMIntegrationConstants.API_TIER.SILVER
                + "," + APIMIntegrationConstants.API_TIER.BRONZE + "," + testPolicyOne.getData().getPolicyName());
        apiRequest.setSubscriptionAvailability(org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO.
                SubscriptionAvailabilityEnum.ALL_TENANTS.toString());
        HttpResponse response = publisherRestAPIForCreator.addAPI(apiRequest);
        apiId = response.getData();

        // Create API Publisher user and publish the API
        userManagementClient.addUser(PUBLISHER_USER_NAME, password, new String[]{APIMIntegrationConstants.
                APIM_INTERNAL_ROLE.PUBLISHER}, PUBLISHER_USER_NAME);
        publisherRestAPIForPublisher = new RestAPIPublisherImpl(PUBLISHER_USER_NAME, password, tenantDomain,
                publisherURLHttps);
        publisherRestAPIForPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction());

        // Create API Subscriber user and subscribe to the API
        userManagementClient.addUser(SUBSCRIBER_USER_NAME, password, new String[]{APIMIntegrationConstants.
                APIM_INTERNAL_ROLE.SUBSCRIBER}, SUBSCRIBER_USER_NAME);
        storeRestAPIForSubscriber = new RestAPIStoreImpl(SUBSCRIBER_USER_NAME, password, tenantDomain, storeURLHttps);
        tenantApplication = storeRestAPIForSubscriber.addApplication(tenantApplicationName,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, StringUtils.EMPTY, StringUtils.EMPTY);
        org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO restAPIStoreSubscriptionDTO =
                storeRestAPIForSubscriber.subscribeToAPI(apiId, tenantApplication.getApplicationId(),
                        APIMIntegrationConstants.API_TIER.BRONZE, tenantDomain);
        subscriptionId = restAPIStoreSubscriptionDTO.getSubscriptionId();
    }

    @Test(groups = {"wso2.am"}, description = "Test updating subscription business plan when subscriptionId is invalid")
    public void testUpdateSubscriptionBusinessPlanWithInvalidSubscriptionId() {
        try {
            publisherRestAPIForPublisher.changeSubscriptionBusinessPlan(StringUtils.EMPTY, APIMIntegrationConstants.
                    API_TIER.SILVER, null);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 400, "Subscription business plan is updated with an " +
                    "empty subscriptionId");
        }

        try {
            publisherRestAPIForPublisher.changeSubscriptionBusinessPlan("INVALID_SUBSCRIPTION_ID",
                    APIMIntegrationConstants.API_TIER.SILVER, null);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 404, "Subscription business plan is updated with an " +
                    "invalid subscriptionId");
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test updating subscription business plan when business plan is invalid",
            dependsOnMethods = {"testUpdateSubscriptionBusinessPlanWithInvalidSubscriptionId"})
    public void testUpdateSubscriptionBusinessPlanWithInvalidBusinessPlan() {
        try {
            publisherRestAPIForPublisher.changeSubscriptionBusinessPlan(subscriptionId, StringUtils.EMPTY, null);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 400, "Subscription business plan is updated when the " +
                    "business plan is empty");
        }

        try {
            publisherRestAPIForPublisher.changeSubscriptionBusinessPlan(subscriptionId, "INVALID_BUSINESS_PLAN", null);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 400, "Subscription business plan is updated with an " +
                    "invalid business plan");
        }

        try {
            publisherRestAPIForPublisher.changeSubscriptionBusinessPlan(subscriptionId, APIMIntegrationConstants.
                    API_TIER.UNLIMITED, null);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 400, "Subscription business plan is updated with an " +
                    "business plan that is not valid for the API");
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test updating subscription business plan when the subscriber is " +
            "restricted to use specific tier",
            dependsOnMethods = {"testUpdateSubscriptionBusinessPlanWithInvalidBusinessPlan"})
    public void testUpdateSubscriptionBusinessPlanWhenSubscriberRestrictedToUseSpecificTier() throws Exception {
        // This business plan update should not fail since the subscriber is not restricted to use TestPolicyOne tier
        publisherRestAPIForPublisher.changeSubscriptionBusinessPlan(subscriptionId, CUSTOM_SUBSCRIPTION_POLICY, null);

        // Restrict the subscriber using TestPolicyOne tier
        SubscriptionThrottlePolicyPermissionDTO denyPermission = DtoFactory.createSubscriptionThrottlePolicyPermissionDTO(
                SubscriptionThrottlePolicyPermissionDTO.PermissionTypeEnum.DENY, roleList);
        testPolicyOne.getData().setPermissions(denyPermission);
        restAPIAdmin.updateSubscriptionThrottlingPolicy(testPolicyOne.getData().getPolicyId(), testPolicyOne.getData());

        try {
            publisherRestAPIForPublisher.changeSubscriptionBusinessPlan(subscriptionId, CUSTOM_SUBSCRIPTION_POLICY, null);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 403, "Subscription business plan is updated when the " +
                    "subscriber is restricted to a specific tier");
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test updating subscription business plan to valid tiers",
            dependsOnMethods = {"testUpdateSubscriptionBusinessPlanWhenSubscriberRestrictedToUseSpecificTier"})
    public void testUpdateSubscriptionBusinessPlanWithValidTiers() throws ApiException {
        publisherRestAPIForPublisher.changeSubscriptionBusinessPlan(subscriptionId, APIMIntegrationConstants.API_TIER.
                SILVER, null);
        restAPIPublisherSubscriptionDTO = getSubscription();
        Assert.assertEquals(restAPIPublisherSubscriptionDTO.getThrottlingPolicy(), APIMIntegrationConstants.API_TIER.
                SILVER, "Subscription business plan is not updated as expected");
    }

    @Test(groups = {"wso2.am"}, description = "Test updating subscription business plan when the subscription is in" +
            "TIER_UPDATE_PENDING status",
            dependsOnMethods = {"testUpdateSubscriptionBusinessPlanWithValidTiers"})
    public void testUpdateSubscriptionBusinessPlanWhenSubscriptionIsInTierUpdatePendingStatus() throws Exception {
        // Enable SubscriptionUpdateApprovalWorkflowExecutor
        resourceAdminServiceClient = new ResourceAdminServiceClient(gatewayContextMgt.getContextUrls().getBackEndUrl(),
                createSession(gatewayContextMgt));
        // Gets the original workflow-extensions.xml file's content from the registry.
        originalWFExtensionsXML = resourceAdminServiceClient.getTextContent(DEFAULT_WF_EXTENSIONS_XML_REG_CONFIG_LOCATION);
        // Gets the new configuration of the workflow-extensions.xml
        String newWFExtensionsXML = readFile(getAMResourceLocation() + File.separator + "configFiles" +
                File.separator + "changeSubscriptionBusinessPlanForcefully" + File.separator + "workflow-extensions" +
                ".xml");
        // Updates the content of the workflow-extensions.xml of the registry file, to have the new configurations.
        resourceAdminServiceClient.updateTextContent(DEFAULT_WF_EXTENSIONS_XML_REG_CONFIG_LOCATION, newWFExtensionsXML);

        // Change the subscription business plan from developer portal
        storeRestAPIForSubscriber.updateSubscriptionToAPI(apiId, tenantApplication.getApplicationId(),
                APIMIntegrationConstants.API_TIER.GOLD, APIMIntegrationConstants.API_TIER.BRONZE,
                org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO.StatusEnum.UNBLOCKED,
                subscriptionId, tenantDomain);

        // Check the subscription is in TIER_UPDATE_PENDING status
        restAPIPublisherSubscriptionDTO = getSubscription();
        Assert.assertEquals(restAPIPublisherSubscriptionDTO.getSubscriptionStatus().getValue(),
                "TIER_UPDATE_PENDING", "Subscription status is not updated as expected");

        // Since the subscription is in TIER_UPDATE_PENDING status, this business plan update should not fail
        try {
            publisherRestAPIForPublisher.changeSubscriptionBusinessPlan(subscriptionId, APIMIntegrationConstants.
                    API_TIER.SILVER, null);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 409, "Subscription business plan is updated when the " +
                    "subscription is in TIER_UPDATE_PENDING status");
        }
    }

    /**
     * Fetch the subscription details from the publisher API and return the subscription DTO.
     */
    private SubscriptionDTO getSubscription() throws ApiException {
        SubscriptionListDTO subscriptionListDTO = publisherRestAPIForPublisher.getSubscriptionByAPIID(apiId);
        for (SubscriptionDTO subscriptionDTO : subscriptionListDTO.getList()) {
            if (subscriptionDTO.getSubscriptionId().equals(subscriptionId)) {
                return subscriptionDTO;
            }
        }
        return null;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIAdmin.deleteSubscriptionThrottlingPolicy(testPolicyOne.getData().getPolicyId());
        storeRestAPIForSubscriber.deleteApplication(tenantApplication.getApplicationId());
        restAPIPublisher.deleteAPI(apiId);
        userManagementClient.deleteUser(CREATOR_USER_NAME);
        userManagementClient.deleteUser(PUBLISHER_USER_NAME);
        userManagementClient.deleteUser(SUBSCRIBER_USER_NAME);
        resourceAdminServiceClient.updateTextContent(DEFAULT_WF_EXTENSIONS_XML_REG_CONFIG_LOCATION,
                originalWFExtensionsXML);
        super.cleanUp();
    }
}
