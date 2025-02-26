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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.governance.ApiResponse;
import org.wso2.am.integration.clients.governance.api.dto.APIMGovernancePolicyDTO;
import org.wso2.am.integration.clients.governance.api.dto.ActionDTO;
import org.wso2.am.integration.clients.governance.api.dto.ArtifactComplianceDetailsDTO;
import org.wso2.am.integration.clients.governance.api.dto.PolicyAdherenceWithRulesetsDTO;
import org.wso2.am.integration.clients.governance.api.dto.RulesetInfoDTO;
import org.wso2.am.integration.clients.governance.api.dto.RulesetListDTO;
import org.wso2.am.integration.test.Constants.APIMGovernanceTestConstants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * This class contains the test cases for API Compliance.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class APIComplianceTestCase extends APIMIntegrationBaseTest{


    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final List<String> defaultRulesets = new ArrayList<>();
    private String createdAPIId;
    private String apiEndpointUrl;

    @Factory(dataProvider = "userModeDataProvider")
    public APIComplianceTestCase(TestUserMode userMode) {

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
        defaultRulesets.add(APIMGovernanceTestConstants.DEFAULT_RULESET_WSO2_API);
        defaultRulesets.add(APIMGovernanceTestConstants.DEFAULT_RULESET_WSO2_REST);
        defaultRulesets.add(APIMGovernanceTestConstants.DEFAULT_RULESET_OWASP);

        apiEndpointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
    }

    /**
     * This test case is to test compliance details of an API after API creation with default policy
     * @throws Exception if an error occurs while retrieving the compliance details
     */
    @Test(groups = {"wso2.am"}, description = "Test compliance details of an API after API creation with default policy")
    public void testComplianceDetailsOfRestAPIAfterAPICreateWithDefaultPolicy() throws Exception {

        // Create API
        APIRequest apiRequest = new APIRequest("GovernanceAPI01", "/governance-01",
                new URL(apiEndpointUrl));
        apiRequest.setVersion("1.0.0");
        apiRequest.setProvider(user.getUserName());
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        createdAPIId = apiResponse.getData();
        assertNotNull(createdAPIId, "API ID is null");

        // Wait for more than 2 minutes for compliance to run
        Thread.sleep(150000);

        // Get compliance details of the API
        ApiResponse<ArtifactComplianceDetailsDTO> compDetails =
                restAPIGovernance.getAPICompliance(createdAPIId);
        assertEquals(compDetails.getStatusCode(), Response.Status.OK.getStatusCode(),
                "Cannot retrieve compliance details of the API");
        assertNotNull(compDetails.getData(),
                "Cannot retrieve compliance details of the API");

        ArtifactComplianceDetailsDTO.StatusEnum compState =  compDetails.getData().getStatus();
        assertEquals(compState, ArtifactComplianceDetailsDTO.StatusEnum.NON_COMPLIANT,
                "API is not compliant with the default policy");

        List<PolicyAdherenceWithRulesetsDTO> governedPolicies = compDetails.getData().getGovernedPolicies();
        assertTrue(governedPolicies!=null
                && governedPolicies.size() > 0, "No governed policies found");
    }

    /**
     * This test case is to test whether API deployment is blocked with a policy
     * @throws Exception if an error occurs
     */
    @Test(groups = {"wso2.am"}, description = "Test API deployment blocking with policy")
    public void testRestAPIDeploymentBlockingWithPolicy() throws Exception {
        // Create a test policy with default rulesets and a blocking action for deployt
        ApiResponse<RulesetListDTO> allRulesets = restAPIGovernance.getRulesets(10,0,"");
        assertNotNull(allRulesets.getData().getList(), "Failed to retrieve rulesets in the organization");

        List<String> defaultRulesetIds = allRulesets.getData().getList().stream()
                .filter(ruleset -> defaultRulesets.contains(ruleset.getName()))
                .map(RulesetInfoDTO::getId).collect(Collectors.toList());

        APIMGovernancePolicyDTO policyDTO = new APIMGovernancePolicyDTO();
        policyDTO.setName(APIMGovernanceTestConstants.TEST_POLICY_NAME);
        policyDTO.setDescription(APIMGovernanceTestConstants.TEST_POLICY_DESCRIPTION);
        policyDTO.setRulesets(defaultRulesetIds);
        policyDTO.setGovernableStates(Collections.singletonList
                (APIMGovernancePolicyDTO.GovernableStatesEnum.API_DEPLOY));
        policyDTO.setLabels(Collections.singletonList(APIMGovernanceTestConstants.GLOBAL_LABEL));

        ActionDTO actionDTO = new ActionDTO();
        actionDTO.setRuleSeverity(ActionDTO.RuleSeverityEnum.WARN);
        actionDTO.setType(ActionDTO.TypeEnum.BLOCK);
        actionDTO.setState(ActionDTO.StateEnum.API_DEPLOY);
        policyDTO.setActions(Collections.singletonList(actionDTO));

        ApiResponse<APIMGovernancePolicyDTO> createdPolicyResp = restAPIGovernance.createPolicy(policyDTO);
        assertEquals(createdPolicyResp.getStatusCode(), Response.Status.CREATED.getStatusCode(),
                "Error in creating test policy with blocking action");
        String createdPolicyId = createdPolicyResp.getData().getId();
        assertNotNull(createdPolicyId,
                "Failed to create test policy with blocking action");

        // Create API
        APIRequest apiRequest = new APIRequest("GovernanceAPI02", "/governance-02",
                new URL(apiEndpointUrl));
        apiRequest.setVersion("1.0.0");
        apiRequest.setProvider(user.getUserName());
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        createdAPIId = apiResponse.getData();
        assertNotNull(createdAPIId, "API ID is null");

        //Add the API Revision using the API publisher.
        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(createdAPIId);
        apiRevisionRequest.setDescription("Test Revision 1");

        HttpResponse apiRevisionResponse = restAPIPublisher.addAPIRevision(apiRevisionRequest);
        assertEquals(apiRevisionResponse.getResponseCode(), Response.Status.BAD_REQUEST.getStatusCode(),
                "New API revision created despite the blocking policy");
        assertTrue(apiRevisionResponse.getData().contains("903300"),
                "API revision created despite the blocking policy");

        // Delete the test policy
        ApiResponse<Void> deletePolicyResp = restAPIGovernance.deletePolicy(createdPolicyId);
        assertEquals(deletePolicyResp.getStatusCode(), Response.Status.NO_CONTENT.getStatusCode(),
                "Error in deleting test policy with blocking action");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }

}
