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
 */

package org.wso2.am.integration.tests.restapi.admin.throttlingpolicy;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.api.dto.ThrottlePolicyDetailsDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottlePolicyDetailsListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.List;

/**
 * This test case is used to test the Get Throttling Policies RESTAPI
 */
public class GetThrottlingPoliciesTestCase extends APIMIntegrationBaseTest {
    private final String APP_POLICY_NAME = "50PerMin";
    private final String SUB_POLICY_NAME = "Gold";
    private final String API_POLICY_NAME = "10KPerMin";

    @Factory(dataProvider = "userModeDataProvider") public GetThrottlingPoliciesTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN } };
    }

    @BeforeClass(alwaysRun = true) public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Test(groups = {
            "wso2.am" }, description = "Get ThrottlePolicies List and check few default policies")
    public void testThrottlePoliciesGet() throws Exception {

        ThrottlePolicyDetailsListDTO policies = restAPIAdmin.getThrottlePolicies("type:all");
        List<ThrottlePolicyDetailsDTO> policyList = policies.getList();
        Assert.assertTrue(containsPolicy(policyList, APP_POLICY_NAME), "Doesn't contain app Policy");
        Assert.assertTrue(containsPolicy(policyList, SUB_POLICY_NAME), "Doesn't contain sub Policy");
        Assert.assertTrue(containsPolicy(policyList, API_POLICY_NAME), "Doesn't contain api Policy");
    }

    /**
     * Checks whether a certain policy exists in the policy list
     *
     * @param list throttle policy details list
     * @param name throttle policy name
     * @return true or false on the policy presence in the policy list
     */
    private boolean containsPolicy(List<ThrottlePolicyDetailsDTO> list, String name) {

        return list.stream().anyMatch(o -> o.getPolicyName().equals(name));
    }
}
