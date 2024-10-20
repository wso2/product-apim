/*
 *   Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.publisher;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyPermissionDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APITiersDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DeleteTierAlreadyAttachedToAPITestCase extends APIMIntegrationBaseTest {

    private String apiId;
    SubscriptionThrottlePolicyDTO subscriptionThrottlePolicyDTO;

    @Factory(dataProvider = "userModeDataProvider")
    public DeleteTierAlreadyAttachedToAPITestCase(
            TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] {
                new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Test(groups = { "wso2.am" }, description = "Update API after deleting attached subscription tier")
    public void testUpdateAPIAfterDeletingAttachedSubscriptionTier()
            throws Exception {
        String displayName = "NewSubscriptionPolicy";
        String description = "This is a new subscription throttle policy";
        String timeUnit = "min";
        Integer unitTime = 1;
        int graphQLMaxComplexity = 400;
        int graphQLMaxDepth = 10;
        int rateLimitCount = -1;
        String rateLimitTimeUnit = "NA";
        boolean stopQuotaOnReach = false;
        String billingPlan = "FREE";
        int subscriberCount = 0;
        String policyName = "NewSubscriptionPolicy";
        //Add a new subscription tier
        Long requestCount = 50L;
        RequestCountLimitDTO requestCountLimit = DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime,
                requestCount);
        ThrottleLimitDTO defaultLimit = DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT,
                requestCountLimit, null);
        String INTERNAL_EVERYONE= "Internal/everyone";
        List<String> roleList = new ArrayList<>();
        roleList.add(INTERNAL_EVERYONE);
        SubscriptionThrottlePolicyPermissionDTO permissions = DtoFactory.
                createSubscriptionThrottlePolicyPermissionDTO(SubscriptionThrottlePolicyPermissionDTO.
                PermissionTypeEnum.ALLOW, roleList);
        subscriptionThrottlePolicyDTO = DtoFactory.createSubscriptionThrottlePolicyDTO(policyName, displayName,
                description, false, defaultLimit, graphQLMaxComplexity, graphQLMaxDepth, rateLimitCount,
                rateLimitTimeUnit, null, stopQuotaOnReach, billingPlan, subscriberCount, permissions);
        ApiResponse<SubscriptionThrottlePolicyDTO> addedPolicy = restAPIAdmin.addSubscriptionThrottlingPolicy(
                subscriptionThrottlePolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        SubscriptionThrottlePolicyDTO addedPolicyDTO = addedPolicy.getData();
        String policyId = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");
        //Check if policy is available
        ApiResponse<SubscriptionThrottlePolicyDTO> returnedPolicy = restAPIAdmin.getSubscriptionThrottlingPolicy(
                policyId);
        Assert.assertNotNull(returnedPolicy);

        subscriptionThrottlePolicyDTO.setPolicyId(policyId);
        //Create an API adding the newly created subscription tier to it
        String backendEndPoint = getBackendEndServiceEndPointHttp("jaxrs_basic/services/customers/customerservice");
        APIRequest apiRequest = new APIRequest("APIForTestingUpdateAfterTierDelete", "/updateAfterTierDelete",
                new URL(backendEndPoint));
        apiRequest.setProvider(user.getUserName());
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.BRONZE + "," + displayName);
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiResponse.getData();
        Assert.assertEquals(apiResponse.getResponseCode(), HttpStatus.SC_CREATED,
                "Create API Response Code is invalid." + apiId);

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        //Publish the API
        HttpResponse response = restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(),
                null);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "API publish Response code is invalid " + apiId);

        //Check tiers in Store API before new tier deletion
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDtoFromStore = restAPIStore.getAPI(apiId);
        Assert.assertTrue(StringUtils.isNotEmpty(apiDtoFromStore.getId()),
                "Api with API ID " + apiId + " is not visible in API Store");
        List<APITiersDTO> tiersDTOList = apiDtoFromStore.getTiers();
        Assert.assertNotNull(tiersDTOList);
        List<String> tierNameList = new ArrayList<>();
        for (APITiersDTO tiersDTO : tiersDTOList) {
            tierNameList.add(tiersDTO.getTierName());
        }
        Assert.assertTrue(tierNameList.contains(displayName),
                "API with API ID " + apiId + " does not contain the new tier " + displayName);

        //Delete policy
        restAPIAdmin.deleteSubscriptionThrottlingPolicy(subscriptionThrottlePolicyDTO.getPolicyId());

        //Update API
        HttpResponse retrievedAPI = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apiDto = g.fromJson(retrievedAPI.getData(), APIDTO.class);

        //Update API with Edited information
        APIDTO updateAPIHTTPResponse = restAPIPublisher.updateAPI(apiDto, apiId);
        Assert.assertTrue(StringUtils.isNotEmpty(updateAPIHTTPResponse.getId()),
                "Error updating API after attached tier deletion");

        //Check tiers in Store API after new tier deletion
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDtoFromStore2 = restAPIStore.getAPI(apiId);
        Assert.assertTrue(StringUtils.isNotEmpty(apiDtoFromStore2.getId()),
                "Api with API ID " + apiId + " is not visible in API Store");
        List<APITiersDTO> tiersDTOList2 = apiDtoFromStore2.getTiers();
        Assert.assertNotNull(tiersDTOList2);
        List<String> tierNameList2 = new ArrayList<>();
        for (APITiersDTO tiersDTO : tiersDTOList2) {
            tierNameList2.add(tiersDTO.getTierName());
        }
        Assert.assertFalse(tierNameList2.contains(displayName),
                "API with API ID " + apiId + " contains the already deleted tier " + displayName);
    }

    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }
}
