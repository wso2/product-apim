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
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.CustomRuleDTO;
import org.wso2.am.integration.clients.admin.api.dto.LabelDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;

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
}
