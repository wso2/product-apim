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

package org.wso2.am.integration.test.impl;

import org.testng.Assert;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyDTO;

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
                "Policy name does not match with the expected policy name");
        Assert.assertEquals(actualPolicy.getDisplayName(), expectedPolicy.getDisplayName(),
                "Policy display name does not match with the expected policy display name");
        Assert.assertEquals(actualPolicy.getDescription(), expectedPolicy.getDescription(),
                "Policy description does not match with the expected policy description");
        boolean isDefaultLimitEqual = actualPolicy.getDefaultLimit().equals(expectedPolicy.getDefaultLimit());
        Assert.assertTrue(isDefaultLimitEqual, "Policy default limit does not match with the expected policy default " +
                "limit");
    }
}
