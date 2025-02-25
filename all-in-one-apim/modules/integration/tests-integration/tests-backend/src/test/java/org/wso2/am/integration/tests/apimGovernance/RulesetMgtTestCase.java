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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.governance.ApiResponse;
import org.wso2.am.integration.clients.governance.api.dto.RulesetInfoDTO;
import org.wso2.am.integration.clients.governance.api.dto.RulesetListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * This class contains the test cases for the Ruleset Management API.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class RulesetMgtTestCase extends APIMIntegrationBaseTest {

    private List<String> defaultRulesets = new ArrayList<>();

    @Factory(dataProvider = "userModeDataProvider")
    public RulesetMgtTestCase(TestUserMode userMode) {

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
        defaultRulesets.add("WSO2 API Management Best Practices");
        defaultRulesets.add("WSO2 REST API Design Guidelines");
    }

    /**
     * This tests whether the default rulesets have been created within the organization.
     */
    @Test(groups = {"wso2.am"}, description = "Test retrieve of Default Rulesets")
    public void testDefaultRulesets() throws Exception {
        ApiResponse<RulesetListDTO> rulesets = restAPIGovernance.getRulesets(10,0,"");
        assertEquals(Response.Status.OK.getStatusCode(), rulesets.getStatusCode(),
                "Error in retrieving default rulesets");
        List<RulesetInfoDTO> obtainedRulesets = rulesets.getData().getList();
        assertNotNull(obtainedRulesets, "No default rulesets found");
        for (String defaultRuleset : defaultRulesets) {
            boolean found = false;
            for (RulesetInfoDTO ruleset : obtainedRulesets) {
                if (obtainedRulesets.contains(ruleset.getName())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Default ruleset " + defaultRuleset + " not found");
        }

    }

}
