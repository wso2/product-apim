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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Integration coverage for per-policy compliance affecting severities on a deployment which has not opted in.
 * <p>
 * The column that holds a policy's selection, {@code GOV_POLICY.COMPLIANCE_AFFECTING_SEVERITIES}, is part of the
 * product schema and always exists. The feature still needs a deliberate opt-in: the
 * {@code apim.governance.per_policy_severity_filtering_enabled} configuration, which ships off. The standard
 * integration pack does not turn it on, and this suite deliberately does not try to: restarting the server
 * mid-suite to flip a configuration would make every later test in the run depend on that restart having worked.
 * <p>
 * What is asserted here is the half that actually carries release risk, and the half that only a running server
 * can show: that a deployment which has not opted in behaves exactly as it did before this feature existed. The
 * severity decision logic itself is unit tested in {@code carbon-apimgt}, against a real database for the SQL and
 * against the mapping layer for the verdicts, where every combination can be exercised cheaply.
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
     * The field must be present in the contract and null on a deployment which has not opted in, because null is
     * what tells a portal to hide the control entirely. An empty string would wrongly advertise the feature as
     * available, and an absent field would leave a client unable to tell the two apart.
     */
    @Test(groups = {"wso2.am"},
            description = "The compliance affecting severities of a policy read null when the feature is not enabled")
    public void testSeverityFieldIsNullWhenTheFeatureIsNotEnabled() throws Exception {

        ApiResponse<APIMGovernancePolicyDTO> policy = restAPIGovernance.getPolicy(policyId);
        assertEquals(policy.getStatusCode(), Response.Status.OK.getStatusCode(), "Cannot read the policy");

        assertNull(policy.getData().getComplianceAffectingSeverities(),
                "Without the configuration enabled the field must be null, so that a client knows not to offer "
                        + "the control at all");
    }

    /**
     * Hiding the control in the portal is not enough, because the REST API can be called directly. A deployment
     * which has not opted in must say so rather than accept the value and silently drop it, which would leave an
     * operator believing a threshold is in force when nothing changed.
     */
    @Test(groups = {"wso2.am"},
            description = "Storing severities is rejected when the deployment has not opted in",
            dependsOnMethods = "testSeverityFieldIsNullWhenTheFeatureIsNotEnabled")
    public void testStoringSeveritiesIsRejectedWhenTheFeatureIsNotEnabled() throws Exception {

        APIMGovernancePolicyDTO policy = restAPIGovernance.getPolicy(policyId).getData();
        policy.setComplianceAffectingSeverities("ERROR,WARN");

        try {
            restAPIGovernance.updatePolicy(policyId, policy);
            fail("Storing compliance affecting severities must be rejected on a deployment which has not enabled "
                    + "the configuration");
        } catch (ApiException e) {
            // A deployment which has not opted in is a supported state, not a fault, so the client is told its
            // request was unacceptable rather than that the server broke. The client can retry without the field.
            assertEquals(e.getCode(), Response.Status.BAD_REQUEST.getStatusCode(),
                    "The rejection must surface as a client error rather than a server failure");
            // ERROR and WARN are both severities the product defines, so this request fails on exactly one
            // condition: the deployment has not opted in. The specific code proves that is the reason, rather
            // than the request merely failing to reach the server for some unrelated cause.
            assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("990213"),
                    "The rejection must be reported under the code for an unavailable feature");
        }

        assertNull(restAPIGovernance.getPolicy(policyId).getData().getComplianceAffectingSeverities(),
                "A rejected write must leave the policy unconfigured");
    }

    /**
     * A severity the product does not define is dropped when a stored selection is read back, so accepting one
     * would leave a policy judged on severities nobody asked for. It is refused on the way in instead, and refused
     * as a client error under its own code, so a client can tell "fix the request" from "the deployment has not
     * opted in".
     */
    @Test(groups = {"wso2.am"},
            description = "A severity the product does not define is rejected rather than stored",
            dependsOnMethods = "testStoringSeveritiesIsRejectedWhenTheFeatureIsNotEnabled")
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

        assertNull(restAPIGovernance.getPolicy(policyId).getData().getComplianceAffectingSeverities(),
                "A rejected write must leave the policy exactly as it was");
    }

    /**
     * The regression that matters for every existing deployment. Before this feature, a ruleset whose only failing
     * rule was INFO severity failed, its policy was violated and the API was non compliant. That was the complaint
     * behind api-manager#16242, and it must stay exactly true until an operator opts in, because anything else
     * would change the compliance posture of every upgraded deployment without being asked.
     */
    @Test(groups = {"wso2.am"},
            description = "An info only violation still fails the ruleset, the policy and the artifact",
            dependsOnMethods = "testStoringSeveritiesIsRejectedWhenTheFeatureIsNotEnabled")
    public void testInfoViolationStillFailsEverythingWhenTheFeatureIsNotEnabled() throws Exception {

        ApiResponse<ArtifactComplianceDetailsDTO> compliance = restAPIGovernance.getAPICompliance(apiId);
        assertEquals(compliance.getStatusCode(), Response.Status.OK.getStatusCode(),
                "Cannot read the compliance details of the API");

        PolicyAdherenceWithRulesetsDTO policy = governedPolicy(compliance.getData());
        assertEquals(rulesetResult(policy).getStatus(),
                RulesetValidationResultWithoutRulesDTO.StatusEnum.FAILED,
                "A ruleset failing only its INFO rule must still be reported as failed, exactly as it was before "
                        + "per policy severity filtering existed");
        assertEquals(policy.getStatus(), PolicyAdherenceWithRulesetsDTO.StatusEnum.VIOLATED,
                "The policy holding that ruleset must still be violated");
        assertEquals(compliance.getData().getStatus(), ArtifactComplianceDetailsDTO.StatusEnum.NON_COMPLIANT,
                "The API must still be non compliant");

        // The aggregate statuses above are consistent with an INFO only violation, but consistent is not the same
        // as caused by: the ruleset holds one ERROR rule too, and only naming the individual results proves it is
        // the INFO rule doing the failing, not some other regression the aggregate status cannot distinguish.
        RulesetValidationResultDTO ruleset = restAPIGovernance.getRulesetValidationResults(apiId, infoRulesetId)
                .getData();
        assertTrue(ruleset.getViolatedRules().stream().anyMatch(rule ->
                        "severity-test-contact-required".equals(rule.getName())
                                && RuleValidationResultDTO.StatusEnum.FAILED.equals(rule.getStatus())
                                && RuleValidationResultDTO.SeverityEnum.INFO.equals(rule.getSeverity())),
                "The INFO contact rule must be violated");
        assertTrue(ruleset.getFollowedRules().stream().anyMatch(rule ->
                        "severity-test-title-required".equals(rule.getName())
                                && RuleValidationResultDTO.StatusEnum.PASSED.equals(rule.getStatus())),
                "The ERROR title rule must be followed");
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
