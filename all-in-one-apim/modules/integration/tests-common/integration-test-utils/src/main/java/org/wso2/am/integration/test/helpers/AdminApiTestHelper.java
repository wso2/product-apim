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

package org.wso2.am.integration.test.helpers;

import org.testng.Assert;
import org.wso2.am.integration.clients.admin.api.dto.APICategoryDTO;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.CustomRuleDTO;
import org.wso2.am.integration.clients.admin.api.dto.EnvironmentDTO;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerDTO;
import org.wso2.am.integration.clients.admin.api.dto.LabelDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.VHostDTO;
import org.wso2.am.integration.clients.admin.api.dto.RoleAliasListDTO;
import org.wso2.am.integration.clients.admin.api.dto.RoleAliasDTO;

/**
 * A collection of helper methods to aid Admin REST API related tests
 */
public class AdminApiTestHelper {

    /**
     * Verify whether the field values of the application throttling policy contains the expected values.
     *
     * @param expectedPolicy Expected policy which contains the expected field values.
     * @param actualPolicy   Policy object of which the field values should be verified.
     */
    public void verifyApplicationThrottlePolicyDTO(ApplicationThrottlePolicyDTO expectedPolicy,
            ApplicationThrottlePolicyDTO actualPolicy) {

        Assert.assertEquals(actualPolicy.getPolicyId(), expectedPolicy.getPolicyId(),
                "Policy ID does not match with the expected policy ID");
        Assert.assertEquals(actualPolicy.getPolicyName(), expectedPolicy.getPolicyName(),
                "Policy name does not match with the expected name");
        Assert.assertEquals(actualPolicy.getDisplayName(), expectedPolicy.getDisplayName(),
                "Policy display name does not match with the expected display name");
        Assert.assertEquals(actualPolicy.getDescription(), expectedPolicy.getDescription(),
                "Policy description does not match with the expected description");
        boolean isDefaultLimitEqual = actualPolicy.getDefaultLimit().equals(expectedPolicy.getDefaultLimit());
        Assert.assertTrue(isDefaultLimitEqual, "Policy default limit does not match with the expected default limit");
    }

    /**
     * Verify whether the field values of the subscription throttling policy contains the expected values.
     *
     * @param expectedPolicy Expected policy which contains the expected field values.
     * @param actualPolicy   Policy object of which the field values should be verified.
     */
    public void verifySubscriptionThrottlePolicyDTO(SubscriptionThrottlePolicyDTO expectedPolicy,
            SubscriptionThrottlePolicyDTO actualPolicy) {

        Assert.assertEquals(actualPolicy.getPolicyId(), expectedPolicy.getPolicyId(),
                "Policy ID does not match with the expected policy ID");
        Assert.assertEquals(actualPolicy.getPolicyName(), expectedPolicy.getPolicyName(),
                "Policy name does not match with the expected name");
        Assert.assertEquals(actualPolicy.getDisplayName(), expectedPolicy.getDisplayName(),
                "Policy display name does not match with the expected display name");
        Assert.assertEquals(actualPolicy.getDescription(), expectedPolicy.getDescription(),
                "Policy description does not match with the expected description");
        boolean isDefaultLimitEqual = actualPolicy.getDefaultLimit().equals(expectedPolicy.getDefaultLimit());
        Assert.assertTrue(isDefaultLimitEqual, "Policy default limit does not match with the expected default limit");
        Assert.assertEquals(actualPolicy.getGraphQLMaxComplexity(), expectedPolicy.getGraphQLMaxComplexity(),
                "Policy graphQL max complexity does not match with the expected graphQL max complexity");
        Assert.assertEquals(actualPolicy.getGraphQLMaxDepth(), expectedPolicy.getGraphQLMaxDepth(),
                "Policy graphQL max depth does not match with the expected graphQL max depth");
        Assert.assertEquals(actualPolicy.getRateLimitCount(), expectedPolicy.getRateLimitCount(),
                "Policy rate limit count does not match with the expected rate limit count");
        Assert.assertEquals(actualPolicy.getRateLimitTimeUnit(), expectedPolicy.getRateLimitTimeUnit(),
                "Policy rate limit time unit does not match with the expected rate limit time unit");
        boolean isCustomAttributesEqual = actualPolicy.getCustomAttributes()
                .equals(expectedPolicy.getCustomAttributes());
        Assert.assertTrue(isCustomAttributesEqual,
                "Policy custom attributes does not match with the expected " + "custom attributes");
        Assert.assertEquals(actualPolicy.getBillingPlan(), expectedPolicy.getBillingPlan(),
                "Policy billing plan does not match with the expected billing plan");
    }

    /**
     * Verify whether the field values of the custom throttling policy contains the expected values.
     *
     * @param expectedPolicy Expected policy which contains the expected field values.
     * @param actualPolicy   Policy object of which the field values should be verified.
     */
    public void verifyCustomThrottlePolicyDTO(CustomRuleDTO expectedPolicy, CustomRuleDTO actualPolicy) {

        Assert.assertEquals(actualPolicy.getPolicyId(), expectedPolicy.getPolicyId(),
                "Policy ID does not match with the expected policy ID");
        Assert.assertEquals(actualPolicy.getPolicyName(), expectedPolicy.getPolicyName(),
                "Policy name does not match with the expected name");
        Assert.assertEquals(actualPolicy.getDescription(), expectedPolicy.getDescription(),
                "Policy description does not match with the expected description");
        Assert.assertEquals(actualPolicy.getSiddhiQuery(), expectedPolicy.getSiddhiQuery(),
                "Policy siddhi query does not match with the expected siddhi query");
        Assert.assertEquals(actualPolicy.getKeyTemplate(), expectedPolicy.getKeyTemplate(),
                "Policy key template does not match with the expected key template");
    }

    /**
     * Verify whether the field values of the advanced throttling policy contains the expected values.
     *
     * @param expectedPolicy Expected policy which contains the expected field values.
     * @param actualPolicy   Policy object of which the field values should be verified.
     */
    public void verifyAdvancedThrottlePolicyDTO(AdvancedThrottlePolicyDTO expectedPolicy,
            AdvancedThrottlePolicyDTO actualPolicy) {

        Assert.assertEquals(actualPolicy.getPolicyId(), expectedPolicy.getPolicyId(),
                "Policy ID does not match with the expected policy ID");
        Assert.assertEquals(actualPolicy.getPolicyName(), expectedPolicy.getPolicyName(),
                "Policy name does not match with the expected name");
        Assert.assertEquals(actualPolicy.getDisplayName(), expectedPolicy.getDisplayName(),
                "Policy display name does not match with the expected display name");
        Assert.assertEquals(actualPolicy.getDescription(), expectedPolicy.getDescription(),
                "Policy description does not match with the expected description");
        boolean isDefaultLimitEqual = actualPolicy.getDefaultLimit().equals(expectedPolicy.getDefaultLimit());
        Assert.assertTrue(isDefaultLimitEqual, "Policy default limit does not match with the expected default limit");
        boolean isConditionGroupsEqual = actualPolicy.getConditionalGroups()
                .equals(expectedPolicy.getConditionalGroups());
        Assert.assertTrue(isConditionGroupsEqual,
                "Policy conditional groups does not match with the expected " + "conditional groups");
    }

    /**
     * Verify whether the field values of the label DTO contains the expected values.
     *
     * @param expectedLabel Expected label which contains the expected field values.
     * @param actualLabel   Label object of which the field values should be verified.
     */
    public void verifyLabelDTO(LabelDTO expectedLabel, LabelDTO actualLabel) {

        Assert.assertEquals(actualLabel.getId(), expectedLabel.getId(),
                "Label ID does not match with the expected label ID");
        Assert.assertEquals(actualLabel.getName(), expectedLabel.getName(),
                "Label name does not match with the expected name");
        Assert.assertEquals(actualLabel.getDescription(), expectedLabel.getDescription(),
                "Label description does not match with the expected description");
        boolean isAccessUrlsEqual = actualLabel.getAccessUrls().equals(expectedLabel.getAccessUrls());
        Assert.assertTrue(isAccessUrlsEqual, "Access URLs does not match with the expected access URLs");
    }

    /**
     * Verify whether the field values of the environment DTO contains the expected values.
     *
     * @param expectedEnv Expected environment which contains the expected field values.
     * @param actualEnv   Environment object of which the field values should be verified.
     */
    public void verifyEnvironmentDTO(EnvironmentDTO expectedEnv, EnvironmentDTO actualEnv) {

        Assert.assertEquals(actualEnv.getId(), expectedEnv.getId(),
                "Environment ID does not match with the expected environment ID");
        Assert.assertEquals(actualEnv.getName(), expectedEnv.getName(),
                "Environment name does not match with the expected name");
        Assert.assertEquals(actualEnv.getDisplayName(), expectedEnv.getDisplayName(),
                "Environment display name does not match with the expected display name");
        Assert.assertEquals(actualEnv.getDescription(), expectedEnv.getDescription(),
                "Environment description does not match with the expected description");
        Assert.assertEquals(actualEnv.isIsReadOnly(), expectedEnv.isIsReadOnly());
        Assert.assertEquals(actualEnv.getProvider(), expectedEnv.getProvider(),
                "Environment provider does not match with the expected provider");
        for (VHostDTO vhost : expectedEnv.getVhosts()) {
            Assert.assertTrue(actualEnv.getVhosts().contains(vhost),
                    "Vhosts of environment does not contain an expected vhost");
        }
    }

    /**
     * Verify whether the field values of the vhost DTO contains the expected values.
     *
     * @param expectedVhost Expected vhost which contains the expected field values.
     * @param actualVhost   Vhost object of which the field values should be verified.
     */
    public void verifyVhostDTO(VHostDTO expectedVhost, VHostDTO actualVhost) {

        Assert.assertEquals(actualVhost.getHost(), expectedVhost.getHost(),
                "Host of vhost does not match with the expected host");
        Assert.assertEquals(actualVhost.getHttpContext(), expectedVhost.getHttpContext(),
                "HTTP context of vhost does not match with the expected HTTP context");
        Assert.assertEquals(actualVhost.getHttpPort(), expectedVhost.getHttpPort(),
                "HTTP port of vhost does not match with the expected HTTP port");
        Assert.assertEquals(actualVhost.getHttpsPort(), expectedVhost.getHttpsPort(),
                "HTTPS port of vhost does not match with the expected HTTPS port");
        Assert.assertEquals(actualVhost.getWsPort(), expectedVhost.getWsPort(),
                "WS port of vhost does not match with the expected WS port");
        Assert.assertEquals(actualVhost.getWssPort(), expectedVhost.getWssPort(),
                "WSS port of vhost does not match with the expected WSS port");
    }

    /**
     * Verify whether the field values of the api category DTO contains the expected values.
     *
     * @param expectedApiCategory Expected api category which contains the expected field values.
     * @param actualApiCategory   Api category object of which the field values should be verified.
     */
    public void verifyApiCategoryDTO(APICategoryDTO expectedApiCategory, APICategoryDTO actualApiCategory) {

        Assert.assertEquals(actualApiCategory.getId(), expectedApiCategory.getId(),
                "Api category ID does not match with the expected api category ID");
        Assert.assertEquals(actualApiCategory.getName(), expectedApiCategory.getName(),
                "Api category name does not match with the expected api category name");
        Assert.assertEquals(actualApiCategory.getDescription(), expectedApiCategory.getDescription(),
                "Api category description does not match with the expected api category description");
    }

    /**
     * Verify whether the field values of the key manager DTO contains the expected values.
     *
     * @param expectedKeyManager Expected key manager which contains the expected field values.
     * @param actualKeyManager   Key manager object of which the field values should be verified.
     */
    public void verifyKeyManagerDTO(KeyManagerDTO expectedKeyManager, KeyManagerDTO actualKeyManager) {

        Assert.assertEquals(actualKeyManager.getId(), expectedKeyManager.getId(),
                "Key Manager ID does not match with the expected Key Manager ID");
        Assert.assertEquals(actualKeyManager.getName(), expectedKeyManager.getName(),
                "Key Manager name does not match with the expected Key Manager name");
        Assert.assertEquals(actualKeyManager.getDescription(), expectedKeyManager.getDescription(),
                "Key Manager description does not match with the expected Key Manager description");
        Assert.assertEquals(actualKeyManager.getDisplayName(), expectedKeyManager.getDisplayName(),
                "Key Manager display name does not match with the expected Key Manager display name");
        Assert.assertEquals(actualKeyManager.getCertificates(), expectedKeyManager.getCertificates(),
                "Key Manager certificates  does not match with the expected Key Manager certificates");
        Assert.assertEquals(actualKeyManager.getIntrospectionEndpoint(), expectedKeyManager.getIntrospectionEndpoint(),
                "Key Manager introspection endpoint does not match with the expected Key Manager " +
                        "introspection endpoint");
        Assert.assertEquals(actualKeyManager.getRevokeEndpoint(), expectedKeyManager.getRevokeEndpoint(),
                "Key Manager revoke endpoint does not match with the expected Key Manager revoke endpoint");
        Assert.assertEquals(actualKeyManager.getClientRegistrationEndpoint(),
                expectedKeyManager.getClientRegistrationEndpoint(),
                "Key Manager client registration endpoint does not match with the expected Key Manager " +
                        "client registration endpoint");
        Assert.assertEquals(actualKeyManager.getTokenEndpoint(), expectedKeyManager.getTokenEndpoint(),
                "Key Manager token endpoint does not match with the expected Key Manager token endpoint");
        Assert.assertEquals(actualKeyManager.getUserInfoEndpoint(), expectedKeyManager.getUserInfoEndpoint(),
                "Key Manager user info endpoint  does not match with the expected user info endpoint");
        Assert.assertEquals(actualKeyManager.getAuthorizeEndpoint(), expectedKeyManager.getAuthorizeEndpoint(),
                "Key Manager authorize endpoint does not match with the expected Key Manager authorize endpoint");
        Assert.assertEquals(actualKeyManager.getScopeManagementEndpoint(),
                expectedKeyManager.getScopeManagementEndpoint(),
                "Key Manager scope management endpoint does not match with the expected Key Manager " +
                        "scope management endpoint");
        Assert.assertEquals(actualKeyManager.getConsumerKeyClaim(), expectedKeyManager.getConsumerKeyClaim(),
                "Key Manager consumer key claim does not match with the expected Key Manager consumer key claim");
        Assert.assertEquals(actualKeyManager.getScopesClaim(), expectedKeyManager.getScopesClaim(),
                "Key Manager scopes claim does not match with the expected Key Manager scopes claim");
        Assert.assertEquals(actualKeyManager.getIssuer(), expectedKeyManager.getIssuer(),
                "Key Manager issuer  does not match with the expected Key Manager issuer");
        Assert.assertEquals(actualKeyManager.getAvailableGrantTypes(), expectedKeyManager.getAvailableGrantTypes(),
                "Key Manager available grant types does not match with the expected Key Manager " +
                        "available grant types");
    }

    /**
     * Verify whether the additional properties values of the key manager DTO contains the expected values.
     *
     * @param expectedAdditionalProperties Expected key manager which contains the expected field values.
     * @param actualAdditionalProperties   Key manager object of which the field values should be verified.
     */
    public void verifyKeyManagerAdditionalProperties(Object expectedAdditionalProperties,
                                                     Object actualAdditionalProperties) {
        Assert.assertEquals(actualAdditionalProperties, expectedAdditionalProperties,
                "Key Manager additional properties does not match with the expected Key Manager " +
                        "additional properties");

    }


    /**
     * Verify whether the field values of the role alias list DTO contains the expected values.
     *
     * @param expectedRoleAliasListDTO Expected role alias list which contains the expected field values.
     * @param actualRoleAliasListDTO   Role alias list object of which the field values should be verified.
     */
    public void verifyRoleAliasListDTO(RoleAliasListDTO expectedRoleAliasListDTO, RoleAliasListDTO actualRoleAliasListDTO) {

        Assert.assertEquals(expectedRoleAliasListDTO.getCount(), actualRoleAliasListDTO.getCount(),
                "Expected role alias count does not match with the actual role alias count");

        if (!expectedRoleAliasListDTO.getList().isEmpty() && !actualRoleAliasListDTO.getList().isEmpty()) {

            RoleAliasDTO expectedRoleAliasDTO =  expectedRoleAliasListDTO.getList().get(0);
            RoleAliasDTO actualRoleAliasDTO =  actualRoleAliasListDTO.getList().get(0);

            Assert.assertEquals(expectedRoleAliasDTO.getRole(), actualRoleAliasDTO.getRole(),
                    "Expected role does not match with the actial role");
            for (String alias: expectedRoleAliasDTO.getAliases()) {
                Assert.assertTrue(actualRoleAliasDTO.getAliases().contains(alias),
                        "Expected aliases does not contain the actual alias");
            }
        }
    }

}
