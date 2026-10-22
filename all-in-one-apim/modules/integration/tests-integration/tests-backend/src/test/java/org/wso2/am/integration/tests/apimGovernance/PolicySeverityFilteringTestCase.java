/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.governance.ApiException;
import org.wso2.am.integration.clients.governance.ApiResponse;
import org.wso2.am.integration.clients.governance.api.dto.APIMGovernancePolicyDTO;
import org.wso2.am.integration.clients.governance.api.dto.ArtifactComplianceDetailsDTO;
import org.wso2.am.integration.clients.governance.api.dto.PolicyAdherenceWithRulesetsDTO;
import org.wso2.am.integration.clients.governance.api.dto.RuleValidationResultDTO;
import org.wso2.am.integration.clients.governance.api.dto.RulesetInfoDTO;
import org.wso2.am.integration.clients.governance.api.dto.RulesetValidationResultDTO;
import org.wso2.am.integration.clients.governance.api.dto.RulesetValidationResultWithoutRulesDTO;
import org.wso2.am.integration.test.Constants.APIMGovernanceTestConstants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Integration coverage for per-policy compliance affecting severities, which on master ships enabled by default:
 * {@code GOV_POLICY.COMPLIANCE_AFFECTING_SEVERITIES} is part of the product schema and always exists, so there is
 * no missing column to guard against the way the 4.6.0 line does, and
 * {@code apim.governance.per_policy_severity_filtering_enabled} defaults to true. Nothing needs to be turned on
 * for this suite to exercise the feature end to end against a real server.
 * <p>
 * The severity decision logic itself is unit tested in {@code carbon-apimgt}, against a real database for the SQL
 * and against the mapping layer for the verdicts, where every combination can be exercised cheaply. What is
 * asserted here is the half only a running server can show: that the REST contract and the scheduled evaluation
 * agree with each other once a policy actually narrows its severities, and that doing so changes the compliance
 * posture that was the subject of api-manager#16242.
 *
 * @see <a href="https://github.com/wso2/api-manager/issues/16242">api-manager#16242</a>
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class PolicySeverityFilteringTestCase extends APIMIntegrationBaseTest {

    private static final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private static final String INFO_RULESET_FILE = "severity-info-ruleset.yaml";
    private static final String INFO_RULESET_NAME = "SeverityInfoRuleset";
    private static final String POLICY_NAME = "SeverityFilteringPolicy";

    /**
     * Governance evaluation is scheduled rather than synchronous and the scheduler runs on a two minute interval,
     * so results are read after a single wait. The other governance suites wait the same way.
     */
    private static final long EVALUATION_WAIT_MILLIS = 150000;

    private String apiId;
    private String infoRulesetId;
    private String policyId;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(TestUserMode.SUPER_TENANT_ADMIN);

        String resourcePath = TestConfigurationProvider.getResourceLocation()
                + APIMGovernanceTestConstants.TEST_RESOURCE_DIRECTORY + File.separator;

        File rulesetContent = new File(resourcePath + INFO_RULESET_FILE);
        ApiResponse<RulesetInfoDTO> rulesetResponse = restAPIGovernance.createRuleset(INFO_RULESET_NAME,
                rulesetContent, APIMGovernanceTestConstants.API_DEFINITION_RULE_TYPE,
                APIMGovernanceTestConstants.REST_API_ARTIFACT_TYPE,
                "Fails only at INFO severity, which is the scenario of api-manager#16242",
                APIMGovernanceTestConstants.SPECTRAL_RULE_CATEGORY, null, user.getUserName());
        assertEquals(rulesetResponse.getStatusCode(), Response.Status.CREATED.getStatusCode(),
                "Failed to create the info severity ruleset");
        infoRulesetId = rulesetResponse.getData().getId();
        assertNotNull(infoRulesetId, "Created ruleset has no id");

        APIMGovernancePolicyDTO policyDTO = new APIMGovernancePolicyDTO();
        policyDTO.setName(POLICY_NAME);
        policyDTO.setDescription("Holds only the info severity ruleset");
        policyDTO.setRulesets(Collections.singletonList(infoRulesetId));
        // API_CREATE is included so the policy evaluates the API this suite creates, rather than waiting for an
        // update that never comes.
        policyDTO.setGovernableStates(Arrays.asList(
                APIMGovernancePolicyDTO.GovernableStatesEnum.API_CREATE,
                APIMGovernancePolicyDTO.GovernableStatesEnum.API_UPDATE));
        policyDTO.setLabels(Collections.singletonList(APIMGovernanceTestConstants.GLOBAL_LABEL));

        ApiResponse<APIMGovernancePolicyDTO> policyResponse = restAPIGovernance.createPolicy(policyDTO);
        assertEquals(policyResponse.getStatusCode(), Response.Status.CREATED.getStatusCode(),
                "Failed to create the policy under test");
        policyId = policyResponse.getData().getId();
        assertNotNull(policyId, "Created policy has no id");

        APIRequest apiRequest = new APIRequest("SeverityFilteringAPI", "/severity-filtering",
                new URL(backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL));
        apiRequest.setVersion("1.0.0");
        apiRequest.setProvider(user.getUserName());
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiResponse.getData();
        assertNotNull(apiId, "Failed to create the API under test");

        Thread.sleep(EVALUATION_WAIT_MILLIS);
    }

    /**
     * The field must be present in the contract and report an empty string before a policy narrows anything,
     * since the feature is enabled by default on this product line. Null would wrongly suggest the capability is
     * unavailable here, the way it is on a deployment which has explicitly disabled it.
     */
    @Test(groups = {"wso2.am"},
            description = "The compliance affecting severities of a policy read as an empty string before it is "
                    + "configured")
    public void testTheFieldIsEmptyStringWhenUnconfigured() throws Exception {

        ApiResponse<APIMGovernancePolicyDTO> policy = restAPIGovernance.getPolicy(policyId);
        assertEquals(policy.getStatusCode(), Response.Status.OK.getStatusCode(), "Cannot read the policy");

        assertEquals(policy.getData().getComplianceAffectingSeverities(), "",
                "Before a policy narrows anything, the field must read as an empty string rather than null, "
                        + "since the feature is enabled by default on this product line");
    }

    /**
     * The feature being enabled by default must actually let a policy narrow its severities over the REST API,
     * not just report that it could. The wait at the end lets the scheduled re-evaluation this write queues
     * complete, so the later tests in this class see compliance results computed under the narrowed selection
     * rather than a stale one computed before it.
     */
    @Test(groups = {"wso2.am"},
            description = "Storing a valid severity selection succeeds",
            dependsOnMethods = "testTheFieldIsEmptyStringWhenUnconfigured")
    public void testStoringAValidSelectionSucceeds() throws Exception {

        APIMGovernancePolicyDTO policy = restAPIGovernance.getPolicy(policyId).getData();
        policy.setComplianceAffectingSeverities("ERROR,WARN");

        ApiResponse<APIMGovernancePolicyDTO> updated = restAPIGovernance.updatePolicy(policyId, policy);
        assertEquals(updated.getStatusCode(), Response.Status.OK.getStatusCode(),
                "Storing a selection of severities the product defines must succeed");
        assertEquals(updated.getData().getComplianceAffectingSeverities(), "ERROR,WARN",
                "The response must reflect the selection that was just stored");
        assertEquals(restAPIGovernance.getPolicy(policyId).getData().getComplianceAffectingSeverities(),
                "ERROR,WARN", "The selection must still be in force on a fresh read, not just in the response "
                        + "to the write");

        Thread.sleep(EVALUATION_WAIT_MILLIS);
    }

    /**
     * A severity the product does not define is dropped when a stored selection is read back, so accepting one
     * would leave a policy judged on severities nobody asked for. It is refused on the way in instead, and refused
     * as a client error under its own code, so a client can tell "fix the request" from a server failure.
     */
    @Test(groups = {"wso2.am"},
            description = "A severity the product does not define is rejected rather than stored",
            dependsOnMethods = "testStoringAValidSelectionSucceeds")
    public void testAnUnknownSeverityIsRejected() throws Exception {

        APIMGovernancePolicyDTO policy = restAPIGovernance.getPolicy(policyId).getData();
        policy.setComplianceAffectingSeverities("ERROR,BLOCKER");

        try {
            restAPIGovernance.updatePolicy(policyId, policy);
            fail("A severity the product does not define must not be accepted");
        } catch (ApiException e) {
            assertEquals(e.getCode(), Response.Status.BAD_REQUEST.getStatusCode(),
                    "An unusable selection is the caller's mistake rather than a server failure");
            assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("BLOCKER"),
                    "The response has to name the token that was refused, so the caller can correct it");
        }

        assertEquals(restAPIGovernance.getPolicy(policyId).getData().getComplianceAffectingSeverities(),
                "ERROR,WARN", "A rejected write must leave the policy exactly as the previous test left it");
    }

    /**
     * The point of narrowing severities, and the regression behind api-manager#16242: before this feature, a
     * ruleset whose only failing rule was INFO severity failed, its policy was violated, with no way for an
     * operator to say that INFO findings should not block anything. Once ERROR and WARN are the only severities
     * a policy counts, that same ruleset must no longer fail that policy, even though the violation is still
     * evaluated and reported.
     * <p>
     * This stops at the policy under test rather than asserting the API's overall compliance, because the
     * default, global policy this product ships also governs this API on its own rulesets, independently of
     * anything this test narrows - which is exactly the "global policies also count" behaviour the feature
     * documents.
     */
    @Test(groups = {"wso2.am"},
            description = "An info only violation no longer fails the ruleset or the policy once narrowed",
            dependsOnMethods = "testStoringAValidSelectionSucceeds")
    public void testInfoViolationNoLongerFailsOnceSeverityIsNarrowed() throws Exception {

        ApiResponse<ArtifactComplianceDetailsDTO> compliance = restAPIGovernance.getAPICompliance(apiId);
        assertEquals(compliance.getStatusCode(), Response.Status.OK.getStatusCode(),
                "Cannot read the compliance details of the API");

        PolicyAdherenceWithRulesetsDTO policy = governedPolicy(compliance.getData());
        assertEquals(rulesetResult(policy).getStatus(),
                RulesetValidationResultWithoutRulesDTO.StatusEnum.PASSED,
                "A ruleset failing only its INFO rule must pass once the policy no longer counts INFO");
        assertEquals(policy.getStatus(), PolicyAdherenceWithRulesetsDTO.StatusEnum.FOLLOWED,
                "The policy holding that ruleset must now be followed");

        // The statuses above are consistent with narrowing having worked, but consistent is not the same as
        // caused by: the ruleset holds one ERROR rule too, and only naming the individual results proves the
        // INFO rule is still being evaluated rather than silently dropped once it stops affecting compliance.
        RulesetValidationResultDTO ruleset = restAPIGovernance.getRulesetValidationResults(apiId, infoRulesetId)
                .getData();
        assertTrue(ruleset.getViolatedRules().stream().anyMatch(rule ->
                        "severity-test-contact-required".equals(rule.getName())
                                && RuleValidationResultDTO.StatusEnum.FAILED.equals(rule.getStatus())
                                && RuleValidationResultDTO.SeverityEnum.INFO.equals(rule.getSeverity())),
                "The INFO contact rule must still be reported as violated, even though it no longer fails the "
                        + "policy");
        assertTrue(ruleset.getFollowedRules().stream().anyMatch(rule ->
                        "severity-test-title-required".equals(rule.getName())
                                && RuleValidationResultDTO.StatusEnum.PASSED.equals(rule.getStatus())),
                "The ERROR title rule must still be followed");
    }

    /**
     * Read the policy under test out of the compliance response
     *
     * @param compliance Compliance details of the API
     * @return Adherence of the policy under test
     */
    private PolicyAdherenceWithRulesetsDTO governedPolicy(ArtifactComplianceDetailsDTO compliance) {

        List<PolicyAdherenceWithRulesetsDTO> governedPolicies = compliance.getGovernedPolicies();
        assertNotNull(governedPolicies, "The API is governed by no policy at all");

        for (PolicyAdherenceWithRulesetsDTO policy : governedPolicies) {
            if (policyId.equals(policy.getId())) {
                return policy;
            }
        }
        fail("Policy " + policyId + " does not govern the API");
        return null;
    }

    /**
     * Read the result of the info severity ruleset as reported under the policy under test
     *
     * @param policy Adherence of the policy under test
     * @return Validation result of the info severity ruleset
     */
    private RulesetValidationResultWithoutRulesDTO rulesetResult(PolicyAdherenceWithRulesetsDTO policy) {

        List<RulesetValidationResultWithoutRulesDTO> results = policy.getRulesetValidationResults();
        assertNotNull(results, "The policy reported no ruleset results");

        for (RulesetValidationResultWithoutRulesDTO result : results) {
            if (infoRulesetId.equals(result.getId())) {
                return result;
            }
        }
        fail("Ruleset " + infoRulesetId + " was not reported under policy " + policyId);
        return null;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        if (apiId != null) {
            restAPIPublisher.deleteAPI(apiId);
        }
        if (policyId != null) {
            restAPIGovernance.deletePolicy(policyId);
        }
        if (infoRulesetId != null) {
            restAPIGovernance.deleteRuleset(infoRulesetId);
        }
        super.cleanUp();
    }
}
