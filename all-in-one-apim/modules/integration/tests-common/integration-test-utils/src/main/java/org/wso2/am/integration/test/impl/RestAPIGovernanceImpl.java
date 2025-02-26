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

package org.wso2.am.integration.test.impl;


import org.wso2.am.integration.clients.governance.ApiClient;
import org.wso2.am.integration.clients.governance.ApiException;
import org.wso2.am.integration.clients.governance.ApiResponse;
import org.wso2.am.integration.clients.governance.api.ArtifactComplianceApi;
import org.wso2.am.integration.clients.governance.api.GovernancePoliciesApi;
import org.wso2.am.integration.clients.governance.api.PolicyAdherenceApi;
import org.wso2.am.integration.clients.governance.api.RulesetsApi;
import org.wso2.am.integration.clients.governance.api.dto.APIMGovernancePolicyDTO;
import org.wso2.am.integration.clients.governance.api.dto.APIMGovernancePolicyListDTO;
import org.wso2.am.integration.clients.governance.api.dto.ArtifactComplianceDetailsDTO;
import org.wso2.am.integration.clients.governance.api.dto.PolicyAdherenceDetailsDTO;
import org.wso2.am.integration.clients.governance.api.dto.RulesetInfoDTO;
import org.wso2.am.integration.clients.governance.api.dto.RulesetListDTO;
import org.wso2.am.integration.test.ClientAuthenticator;

import java.io.File;

public class RestAPIGovernanceImpl {
    public ApiClient apiGovernanceClient = new ApiClient();
    
    private RulesetsApi rulesetsApi = new RulesetsApi();
    private GovernancePoliciesApi policiesApi = new GovernancePoliciesApi();
    private ArtifactComplianceApi artifactComplianceApi = new ArtifactComplianceApi();
    private PolicyAdherenceApi policyAdherenceApi = new PolicyAdherenceApi();
    // Reusing the admin portal SP for gov tests as there is no separate gov portal
    public static final String appName = "Integration_Test_App_Admin";
    public static final String callBackURL = "test.com";
    public static final String tokenScope = "Production";
    public static final String appOwner = "admin";
    public static final String grantType = "password";
    public static final String username = "admin";
    public static final String password = "admin";
    public String tenantDomain;

    /**
     * Constructor for the RestAPIGovernanceImpl
     *
     * @param username Username of the user
     * @param password Password of the user
     * @param tenantDomain Tenant domain of the user
     * @param url server URL
     */
    public RestAPIGovernanceImpl(String username, String password, String tenantDomain, String url) {
        // token/DCR of publisher node itself will be used
        String tokenURL = url + "oauth2/token";
        String dcrURL = url + "client-registration/v0.17/register";
        String scopeList = "openid  " +
                "apim:gov_rule_read "+
                "apim:gov_rule_manage "+
                "apim:gov_policy_read "+
                "apim:gov_policy_manage "+
                "apim:gov_result_read";

        String accessToken = ClientAuthenticator
                .getAccessToken(scopeList,
                        appName, callBackURL, tokenScope, appOwner, grantType, dcrURL, username, password, tenantDomain,
                        tokenURL);

        apiGovernanceClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
        apiGovernanceClient.setBasePath(url + "api/am/governance/v1");
        apiGovernanceClient.setDebugging(true);
        apiGovernanceClient.setReadTimeout(600000);
        apiGovernanceClient.setConnectTimeout(600000);
        apiGovernanceClient.setWriteTimeout(600000);
        rulesetsApi.setApiClient(apiGovernanceClient);
        policiesApi.setApiClient(apiGovernanceClient);
        artifactComplianceApi.setApiClient(apiGovernanceClient);
        policyAdherenceApi.setApiClient(apiGovernanceClient);
        this.tenantDomain = tenantDomain;
    }

    /**
     * Create a new ruleset
     *
     * @param name Name of the ruleset
     * @param rulesetContent Content of the ruleset
     * @param ruleType Type of the rule
     * @param artifactType Type of the artifact
     * @param description Description of the ruleset
     * @param ruleCategory Category of the rule
     * @param documentationLink Documentation link of the ruleset
     * @param provider Provider of the ruleset
     * @return RulesetInfoDTO
     * @throws ApiException If there is an issue with the API request
     */
    public ApiResponse<RulesetInfoDTO> createRuleset(String name, File rulesetContent, String ruleType,
                                                     String artifactType,
                                                     String description, String ruleCategory,
                                                     String documentationLink, String provider)  throws ApiException {
        return rulesetsApi.createRulesetWithHttpInfo(name, rulesetContent, ruleType, artifactType,
                                  description, ruleCategory, documentationLink, provider);
    }

    /**
     * Update an existing ruleset
     *
     * @param rulesetId Id of the ruleset
     * @param name Name of the ruleset
     * @param rulesetContent Content of the ruleset
     * @param ruleType Type of the rule
     * @param artifactType Type of the artifact
     * @param description Description of the ruleset
     * @param ruleCategory Category of the rule
     * @param documentationLink Documentation link of the ruleset
     * @param provider Provider of the ruleset
     * @return RulesetInfoDTO
     * @throws ApiException If there is an issue with the API request
     */
    public ApiResponse<RulesetInfoDTO> updateRuleset(String rulesetId, String name, File rulesetContent,
                                                     String ruleType, String artifactType,
                                                     String description, String ruleCategory,
                                                     String documentationLink, String provider)  throws ApiException {
        return rulesetsApi.updateRulesetByIdWithHttpInfo(rulesetId, name, rulesetContent, ruleType, artifactType,
                                  description, ruleCategory, documentationLink, provider);
    }

    /**
     * Delete a ruleset
     *
     * @param rulesetId Id of the ruleset
     * @return Void
     * @throws ApiException If there is an issue with the API request
     */
    public ApiResponse<Void> deleteRuleset(String rulesetId) throws ApiException {
        return rulesetsApi.deleteRulesetWithHttpInfo(rulesetId);
    }

    /**
     * Get a ruleset
     *
     * @param rulesetId Id of the ruleset
     * @return RulesetInfoDTO
     * @throws ApiException If there is an issue with the API request
     */
    public ApiResponse<RulesetInfoDTO> getRuleset(String rulesetId) throws ApiException {
        return rulesetsApi.getRulesetByIdWithHttpInfo(rulesetId);
    }

    /**
     * Get all rulesets
     *
     * @param limit Limit of the rulesets
     * @param offset Offset of the rulesets
     * @param query Query to search for rulesets
     * @return RulesetListDTO
     * @throws ApiException If there is an issue with the API request
     */
    public ApiResponse<RulesetListDTO> getRulesets(Integer limit, Integer offset, String query) throws ApiException {
        return rulesetsApi.getRulesetsWithHttpInfo(limit, offset, query);
    }

    /**
     * Create a new governance policy
     *
     * @param policyDTO APIMGovernancePolicyDTO object
     * @return APIMGovernancePolicyDTO
     * @throws ApiException If there is an issue with the API request
     */
    public ApiResponse<APIMGovernancePolicyDTO> createPolicy(APIMGovernancePolicyDTO policyDTO) throws ApiException {
        return policiesApi.createGovernancePolicyWithHttpInfo(policyDTO);
    }

    /**
     * Update an existing governance policy
     *
     * @param policyId Id of the policy
     * @param policyDTO APIMGovernancePolicyDTO object
     * @return APIMGovernancePolicyDTO
     * @throws ApiException If there is an issue with the API request
     */
    public ApiResponse<APIMGovernancePolicyDTO> updatePolicy(String policyId, APIMGovernancePolicyDTO policyDTO)
            throws ApiException {
        return policiesApi.updateGovernancePolicyByIdWithHttpInfo(policyId, policyDTO);
    }

    /**
     * Delete a governance policy
     *
     * @param policyId Id of the policy
     * @return Void
     * @throws ApiException If there is an issue with the API request
     */
    public ApiResponse<Void> deletePolicy(String policyId) throws ApiException {
        return policiesApi.deleteGovernancePolicyWithHttpInfo(policyId);
    }

    /**
     * Get a governance policy
     *
     * @param policyId Id of the policy
     * @return APIMGovernancePolicyDTO
     * @throws ApiException If there is an issue with the API request
     */
    public ApiResponse<APIMGovernancePolicyDTO> getPolicy(String policyId) throws ApiException {
        return policiesApi.getGovernancePolicyByIdWithHttpInfo(policyId);
    }

    /**
     * Get all governance policies
     *
     * @param limit Limit of the policies
     * @param offset Offset of the policies
     * @param query Query to search for policies
     * @return APIMGovernancePolicyListDTO
     * @throws ApiException If there is an issue with the API request
     */
    public ApiResponse<APIMGovernancePolicyListDTO> getPolicies(Integer limit, Integer offset, String query) throws ApiException {
        return policiesApi.getGovernancePoliciesWithHttpInfo(limit, offset, query);
    }

    /**
     * Get the compliance details of an artifact
     *
     * @param apiId Id of the API
     * @return ArtifactComplianceDetailsDTO
     * @throws ApiException If there is an issue with the API request
     */
    public ApiResponse<ArtifactComplianceDetailsDTO> getAPICompliance(String apiId) throws ApiException {
        return artifactComplianceApi.getComplianceByAPIIdWithHttpInfo(apiId);
    }

    /**
     * Get the policy adherence details of a policy
     *
     * @param poilcyId Id of the policy
     * @return PolicyAdherenceDetailsDTO
     * @throws ApiException If there is an issue with the API request
     */
    public ApiResponse<PolicyAdherenceDetailsDTO> getPolicyAdherence(String poilcyId) throws ApiException {
        return policyAdherenceApi.getPolicyAdherenceByPolicyIdWithHttpInfo(poilcyId);
    }
}
