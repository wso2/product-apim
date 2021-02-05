/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.tests.restapi.admin.throttlingpolicy;

import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.BandwidthLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.ConditionalGroupDTO;
import org.wso2.am.integration.clients.admin.api.dto.HeaderConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.IPConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.JWTClaimsConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.QueryParameterConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdvancedThrottlingPolicyTestCase extends APIMIntegrationBaseTest {

    private String displayName = "Test Policy";
    private String description = "This is a test advanced throttle policy";
    private String timeUnit = "min";
    private Integer unitTime = 1;
    private AdvancedThrottlePolicyDTO requestCountPolicyDTO;
    private AdvancedThrottlePolicyDTO bandwidthPolicyDTO;
    private AdvancedThrottlePolicyDTO conditionalGroupsPolicyDTO;
    private AdminApiTestHelper adminApiTestHelper;

    @Factory(dataProvider = "userModeDataProvider")
    public AdvancedThrottlingPolicyTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        adminApiTestHelper = new AdminApiTestHelper();
    }

    @Test(groups = {"wso2.am"}, description = "Test add advanced throttling policy with request count limit")
    public void testAddPolicyWithRequestCountLimit() throws Exception {

        //Create the advanced throttling policy DTO with request count limit
        String policyName = "TestPolicyOne";
        Long requestCount = 50L;
        List<ConditionalGroupDTO> conditionalGroups = new ArrayList<>();
        RequestCountLimitDTO requestCountLimit =
                DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime, requestCount);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, requestCountLimit, null);
        requestCountPolicyDTO = DtoFactory
                .createAdvancedThrottlePolicyDTO(policyName, displayName, description, false, defaultLimit,
                        conditionalGroups);

        //Add the advanced throttling policy
        ApiResponse<AdvancedThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(requestCountPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedPolicyDTO = addedPolicy.getData();
        String policyId = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");

        requestCountPolicyDTO.setPolicyId(policyId);
        requestCountPolicyDTO.setIsDeployed(true);
        //Verify the created advanced throttling policy DTO
        adminApiTestHelper.verifyAdvancedThrottlePolicyDTO(requestCountPolicyDTO, addedPolicyDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test add advanced throttling policy with bandwidth limit",
            dependsOnMethods = "testAddPolicyWithRequestCountLimit")
    public void testAddPolicyWithBandwidthLimit() throws Exception {

        //Create the advanced throttling policy DTO with bandwidth limit
        String policyName = "TestPolicyTwo";
        Long dataAmount = 2L;
        String dataUnit = "KB";
        List<ConditionalGroupDTO> conditionalGroups = new ArrayList<>();
        BandwidthLimitDTO bandwidthLimit = DtoFactory.createBandwidthLimitDTO(timeUnit, unitTime, dataAmount, dataUnit);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.BANDWIDTHLIMIT, null, bandwidthLimit);
        bandwidthPolicyDTO = DtoFactory
                .createAdvancedThrottlePolicyDTO(policyName, displayName, description, false, defaultLimit,
                        conditionalGroups);

        //Add the advanced throttling policy
        ApiResponse<AdvancedThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(bandwidthPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedPolicyDTO = addedPolicy.getData();
        String policyId = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");

        bandwidthPolicyDTO.setPolicyId(policyId);
        bandwidthPolicyDTO.setIsDeployed(true);
        //Verify the created advanced throttling policy DTO
        adminApiTestHelper.verifyAdvancedThrottlePolicyDTO(bandwidthPolicyDTO, addedPolicyDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test add advanced throttling policy with conditional groups",
            dependsOnMethods = "testAddPolicyWithBandwidthLimit")
    public void testAddPolicyWithConditionalGroups() throws Exception {

        //Create the advanced throttling policy DTO with conditional groups
        String policyName = "TestPolicyThree";
        Long requestCount = 50L;
        List<ConditionalGroupDTO> conditionalGroups = new ArrayList<>();
        RequestCountLimitDTO requestCountLimit =
                DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime, requestCount);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, requestCountLimit, null);

        conditionalGroups.add(createConditionalGroup(defaultLimit));
        conditionalGroupsPolicyDTO = DtoFactory
                .createAdvancedThrottlePolicyDTO(policyName, displayName, description, false, defaultLimit,
                        conditionalGroups);

        //Add the advanced throttling policy
        ApiResponse<AdvancedThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(conditionalGroupsPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedPolicyDTO = addedPolicy.getData();
        String policyId = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");

        conditionalGroupsPolicyDTO.setPolicyId(policyId);
        conditionalGroupsPolicyDTO.setIsDeployed(true);
        //Verify the created advanced throttling policy DTO
        adminApiTestHelper.verifyAdvancedThrottlePolicyDTO(conditionalGroupsPolicyDTO, addedPolicyDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test get and update advanced throttling policy",
            dependsOnMethods = "testAddPolicyWithConditionalGroups")
    public void testGetAndUpdatePolicy() throws Exception {

        //Get the added advanced throttling policy with request count limit
        String policyId = requestCountPolicyDTO.getPolicyId();
        ApiResponse<AdvancedThrottlePolicyDTO> retrievedPolicy =
                restAPIAdmin.getAdvancedThrottlingPolicy(policyId);
        AdvancedThrottlePolicyDTO retrievedPolicyDTO = retrievedPolicy.getData();
        Assert.assertEquals(retrievedPolicy.getStatusCode(), HttpStatus.SC_OK);

        //Verify the retrieved advanced throttling policy DTO
        adminApiTestHelper.verifyAdvancedThrottlePolicyDTO(requestCountPolicyDTO, retrievedPolicyDTO);

        //Update the advanced throttling policy
        String updatedDescription = "This is a updated test advanced throttle policy";
        requestCountPolicyDTO.setDescription(updatedDescription);
        ApiResponse<AdvancedThrottlePolicyDTO> updatedPolicy =
                restAPIAdmin.updateAdvancedThrottlingPolicy(policyId, requestCountPolicyDTO);
        AdvancedThrottlePolicyDTO updatedPolicyDTO = updatedPolicy.getData();
        Assert.assertEquals(updatedPolicy.getStatusCode(), HttpStatus.SC_OK);

        //Verify the updated advanced throttling policy DTO
        adminApiTestHelper.verifyAdvancedThrottlePolicyDTO(requestCountPolicyDTO, updatedPolicyDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test delete advanced throttling policy",
            dependsOnMethods = "testGetAndUpdatePolicy")
    public void testDeletePolicy() throws Exception {

        ApiResponse<Void> apiResponse =
                restAPIAdmin.deleteAdvancedThrottlingPolicy(requestCountPolicyDTO.getPolicyId());
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);
    }

    @Test(groups = {"wso2.am"}, description = "Test add advanced throttling policy with existing policy name",
            dependsOnMethods = "testDeletePolicy")
    public void testAddPolicyWithExistingPolicyName() {

        //Exception occurs when adding an advanced throttling policy with an existing policy name. The status code
        //in the Exception object is used to assert this scenario
        try {
            restAPIAdmin.addAdvancedThrottlingPolicy(bandwidthPolicyDTO);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_CONFLICT);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test delete advanced throttling policy with non existing policy ID",
            dependsOnMethods = "testAddPolicyWithExistingPolicyName")
    public void testDeletePolicyWithNonExistingPolicyId() {

        //Exception occurs when deleting an advanced throttling policy with a non existing policy ID. The
        //status code in the Exception object is used to assert this scenario
        try {
            //The policy ID is created by combining two generated UUIDs
            restAPIAdmin
                    .deleteAdvancedThrottlingPolicy(UUID.randomUUID().toString() + UUID.randomUUID().toString());
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    /**
     * Creates a conditional group with a list of conditions
     *
     * @param limit Throttle limit of the conditional group.
     * @return Created conditional group DTO
     */
    public ConditionalGroupDTO createConditionalGroup(ThrottleLimitDTO limit) {

        String conditionalGroupDescription = "This is a test conditional group";
        List<ThrottleConditionDTO> conditions = createThrottlingConditions();
        return DtoFactory.createConditionalGroupDTO(conditionalGroupDescription, conditions, limit);
    }

    /**
     * Creates a list of throttling conditions
     *
     * @return Created list of throttling condition DTOs
     */
    public List<ThrottleConditionDTO> createThrottlingConditions() {

        List<ThrottleConditionDTO> throttleConditions = new ArrayList<>();

        //Create the IP condition and add it to the throttle conditions list
        String specificIP = "10.100.1.22";
        IPConditionDTO ipConditionDTO =
                DtoFactory.createIPConditionDTO(IPConditionDTO.IpConditionTypeEnum.IPSPECIFIC, specificIP, null, null);
        ThrottleConditionDTO ipCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.IPCONDITION, false, null, ipConditionDTO,
                        null, null);
        throttleConditions.add(ipCondition);

        //Create the header condition and add it to the throttle conditions list
        String headerName = "Host";
        String headerValue = "10.100.7.77";
        HeaderConditionDTO headerConditionDTO = DtoFactory.createHeaderConditionDTO(headerName, headerValue);
        ThrottleConditionDTO headerCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.HEADERCONDITION, false, headerConditionDTO,
                        null, null, null);
        throttleConditions.add(headerCondition);

        //Create the query parameter condition and add it to the throttle conditions list
        String claimUrl = "claimUrl";
        String attribute = "claimAttribute";
        QueryParameterConditionDTO queryParameterConditionDTO =
                DtoFactory.createQueryParameterConditionDTO(claimUrl, attribute);
        ThrottleConditionDTO queryParameterCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.QUERYPARAMETERCONDITION, false, null, null,
                        null, queryParameterConditionDTO);
        throttleConditions.add(queryParameterCondition);

        //Create the JWT claims condition and add it to the throttle conditions list
        String parameterName = "name";
        String parameterValue = "admin";
        JWTClaimsConditionDTO jwtClaimsConditionDTO =
                DtoFactory.createJWTClaimsConditionDTO(parameterName, parameterValue);
        ThrottleConditionDTO jwtClaimsCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.JWTCLAIMSCONDITION, false, null, null,
                        jwtClaimsConditionDTO, null);
        throttleConditions.add(jwtClaimsCondition);

        return throttleConditions;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIAdmin.deleteAdvancedThrottlingPolicy(bandwidthPolicyDTO.getPolicyId());
        restAPIAdmin.deleteAdvancedThrottlingPolicy(conditionalGroupsPolicyDTO.getPolicyId());
    }
}
