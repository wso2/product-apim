/*
 *  Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.server.restart;

import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.CustomRuleDTO;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.UUID;

public class CustomThrottlingPolicyServerRestartTestCase extends APIMIntegrationBaseTest {

    private String customThrottlingPolicyId;
    private CustomRuleDTO customThrottlingRuleDTO;
    private AdminApiTestHelper customThrottlingAdminApiTestHelper;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment(ITestContext ctx) throws Exception {
        super.init();
        customThrottlingPolicyId = (String) ctx.getAttribute("customThrottlingPolicyId");
        customThrottlingRuleDTO = (CustomRuleDTO) ctx.getAttribute("customThrottlingRuleDTO");
        customThrottlingAdminApiTestHelper = (AdminApiTestHelper) ctx.getAttribute("customThrottlingAdminApiTestHelper");
    }

    @Test(groups = {"wso2.am"}, description = "Test get custom throttling policy")
    public void testGetPolicy() throws ApiException {

        if (userMode == TestUserMode.TENANT_ADMIN) {
            customThrottlingPolicyId = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        }
        //Get the added custom throttling policy
        ApiResponse<CustomRuleDTO> retrievedPolicy = restAPIAdmin.getCustomThrottlingPolicy(customThrottlingPolicyId);
        CustomRuleDTO retrievedPolicyDTO = retrievedPolicy.getData();
        Assert.assertEquals(retrievedPolicy.getStatusCode(), HttpStatus.SC_OK);

        //Verify the retrieved custom throttling policy DTO
        customThrottlingAdminApiTestHelper.verifyCustomThrottlePolicyDTO(customThrottlingRuleDTO, retrievedPolicyDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test update custom throttling policy",
            dependsOnMethods = "testGetPolicy")
    public void testUpdatePolicy() throws ApiException {

        //Update the custom throttling policy
        String updatedDescription = "This is a updated test custom throttle policy";
        customThrottlingRuleDTO.setDescription(updatedDescription);
        ApiResponse<CustomRuleDTO> updatedPolicy;
        updatedPolicy = restAPIAdmin.updateCustomThrottlingPolicy(customThrottlingPolicyId, customThrottlingRuleDTO);

        CustomRuleDTO updatedPolicyDTO = updatedPolicy.getData();
        Assert.assertEquals(updatedPolicy.getStatusCode(), HttpStatus.SC_OK);

        //Verify the updated custom throttling policy DTO
        customThrottlingAdminApiTestHelper.verifyCustomThrottlePolicyDTO(customThrottlingRuleDTO, updatedPolicyDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test add custom throttling policy with existing policy name",
            dependsOnMethods = "testUpdatePolicy")
    public void testAddPolicyWithExistingPolicyName() {

        //Exception occurs when adding an custom throttling policy with an existing policy name. The status code
        //in the Exception object is used to assert this scenario
        try {
            restAPIAdmin.addCustomThrottlingPolicy(customThrottlingRuleDTO);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_CONFLICT);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test delete custom throttling policy",
            dependsOnMethods = "testAddPolicyWithExistingPolicyName")
    public void testDeletePolicy() throws ApiException {

        ApiResponse<Void> apiResponse = restAPIAdmin.deleteCustomThrottlingPolicy(customThrottlingPolicyId);
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);
    }

    @Test(groups = {"wso2.am"}, description = "Test delete custom throttling policy with non existing policy ID",
            dependsOnMethods = "testDeletePolicy")
    public void testDeletePolicyWithNonExistentPolicyId() {

        //Exception occurs when deleting a custom throttling policy with a non existing policy ID. The
        //status code in the Exception object is used to assert this scenario
        try {
            //The policy ID is created by combining two generated UUIDs
            restAPIAdmin
                    .deleteCustomThrottlingPolicy(UUID.randomUUID().toString() + UUID.randomUUID().toString());
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND);
        }
    }
}
