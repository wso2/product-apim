/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.apimGovernance;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.governance.ApiResponse;
import org.wso2.am.integration.clients.governance.api.dto.APIMGovernancePolicyDTO;
import org.wso2.am.integration.clients.governance.api.dto.APIMGovernancePolicyListDTO;
import org.wso2.am.integration.clients.governance.api.dto.RulesetInfoDTO;
import org.wso2.am.integration.clients.governance.api.dto.RulesetListDTO;
import org.wso2.am.integration.test.Constants.APIMGovernanceTestConstants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class PolicyMgtTestCase extends APIMIntegrationBaseTest {

    private String createdPolicyId;
    @Factory(dataProvider = "userModeDataProvider")
    public PolicyMgtTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                {TestUserMode.SUPER_TENANT_ADMIN},
                {TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
    }

    /**
     * Test default policy retrieval
     * @throws Exception if an error occurs while retrieving the policy
     */
    @Test(groups = {"wso2.am"}, description = "Test default policy retrieval")
    public void testDefaultPolicyRetrieval() throws Exception {
        ApiResponse<APIMGovernancePolicyListDTO> defaultPolicies =
                restAPIGovernance.getPolicies(10, 0, "");
        assertNotNull(defaultPolicies.getData().getList(), "Failed to retrieve default policy");
        List<String> defaultPolicyNames = defaultPolicies.getData().getList().stream()
                .map(APIMGovernancePolicyDTO::getName).collect(Collectors.toList());
        assertTrue(defaultPolicyNames.size() > 0, "Failed to retrieve default policy");
        assertTrue(defaultPolicyNames.contains(APIMGovernanceTestConstants.DEFAULT_POLICY_NAME),
                "Default policy not found");
    }

    /**
     * Test valid global governance policy creation
     * @throws Exception if an error occurs while creating the policy
     */
    @Test(groups = {"wso2.am"}, description = "Test valid global governance policy creation")
    public void testValidGlobalPolicyCreation() throws Exception {
        ApiResponse<RulesetListDTO> allRulesets = restAPIGovernance.getRulesets(10,0,"");
        assertNotNull(allRulesets.getData().getList(), "Failed to retrieve rulesets in the organization");

        List<String> rulesetIds = allRulesets.getData().getList().stream()
                .map(RulesetInfoDTO::getId).collect(Collectors.toList());

        APIMGovernancePolicyDTO policyDTO = new APIMGovernancePolicyDTO();
        policyDTO.setName(APIMGovernanceTestConstants.TEST_POLICY_NAME);
        policyDTO.setDescription(APIMGovernanceTestConstants.TEST_POLICY_DESCRIPTION);
        policyDTO.setRulesets(rulesetIds);
        policyDTO.setGovernableStates(Collections.singletonList(APIMGovernancePolicyDTO.GovernableStatesEnum.API_UPDATE));

        ApiResponse<APIMGovernancePolicyDTO> createdPolicyResp = restAPIGovernance.createPolicy(policyDTO);

        assertEquals(Response.Status.CREATED.getStatusCode(), createdPolicyResp.getStatusCode(),
                "Error in creating global governance policy");

        createdPolicyId = createdPolicyResp.getData().getId();

        assertNotNull(createdPolicyId, "Failed to create global governance policy");
    }

    /**
     * Test valid global governance policy update
     * @throws Exception if an error occurs while updating the policy
     */
    @Test(groups = {"wso2.am"}, description = "Test valid global governance policy update"
            , dependsOnMethods = "testValidGlobalPolicyCreation")
    public void testValidGlobalPolicyUpdate() throws Exception {
        ApiResponse<RulesetListDTO> allRulesets = restAPIGovernance.getRulesets(10,0,"");
        assertNotNull(allRulesets.getData().getList(), "Failed to retrieve rulesets in the organization");

        List<String> rulesetIds = allRulesets.getData().getList().stream()
                .map(RulesetInfoDTO::getId).collect(Collectors.toList());

        APIMGovernancePolicyDTO policyDTO = new APIMGovernancePolicyDTO();
        policyDTO.setName(APIMGovernanceTestConstants.TEST_POLICY_NAME);
        policyDTO.setDescription(APIMGovernanceTestConstants.TEST_POLICY_DESCRIPTION);
        policyDTO.setRulesets(rulesetIds);
        policyDTO.setGovernableStates(Collections.singletonList(APIMGovernancePolicyDTO.GovernableStatesEnum.API_UPDATE));

        ApiResponse<APIMGovernancePolicyDTO> updatedPolicyResp = restAPIGovernance.updatePolicy(createdPolicyId, policyDTO);

        assertEquals(Response.Status.OK.getStatusCode(), updatedPolicyResp.getStatusCode(),
                "Error in updating global governance policy");
    }

    /**
     * Test valid global governance policy deletion
     * @throws Exception if an error occurs while deleting the policy
     */
    @Test(groups = {"wso2.am"}, description = "Test valid global governance policy delete"
            , dependsOnMethods = "testValidGlobalPolicyUpdate")
    public void testValidGlobalPolicyDeletion() throws Exception {
        ApiResponse<Void> deletedPolicyResp = restAPIGovernance.deletePolicy(createdPolicyId);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deletedPolicyResp.getStatusCode(),
                "Error in deleting global governance policy");
    }

    @AfterTest(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }
}
