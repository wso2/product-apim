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

package org.wso2.am.integration.tests.restapi.admin.throttlingpolicy;

import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.CustomRuleDTO;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.UUID;

public class CustomThrottlingPolicyTestCase extends APIMIntegrationBaseTest {

    private String policyId;
    private CustomRuleDTO customRuleDTO;
    private AdminApiTestHelper adminApiTestHelper;

    @Factory(dataProvider = "userModeDataProvider")
    public CustomThrottlingPolicyTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        adminApiTestHelper = new AdminApiTestHelper();
    }

    @Test(groups = {"wso2.am"}, description = "Test add custom throttling policy")
    public void testAddPolicy() {

        //Create the custom throttling policy DTO
        String policyName = "TestPolicy";
        String description = "This is a test custom throttle policy";
        String siddhiQuery = "FROM RequestStream\nSELECT userId, ( userId == 'admin@carbon.super' ) AS isEligible, " +
                "str:concat('admin@carbon.super','') as throttleKey\nINSERT INTO EligibilityStream; \n\nFROM " +
                "EligibilityStream[isEligible==true]#throttler:timeBatch(1 min) \nSELECT throttleKey, (count(userId) >= 10) " +
                "as isThrottled, expiryTimeStamp group by throttleKey \nINSERT ALL EVENTS into ResultStream;";
        String keyTemplate = "$userId";
        customRuleDTO = DtoFactory
                .createCustomThrottlePolicyDTO(policyName, description, false, siddhiQuery, keyTemplate);

        ApiResponse<CustomRuleDTO> addedPolicy;
        try {
            //Add the custom throttling policy
            addedPolicy = restAPIAdmin.addCustomThrottlingPolicy(customRuleDTO);

            //Assert the status code and policy ID
            Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
            CustomRuleDTO addedPolicyDTO = addedPolicy.getData();
            policyId = addedPolicyDTO.getPolicyId();
            Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");

            customRuleDTO.setPolicyId(policyId);
            customRuleDTO.setIsDeployed(true);
            //Verify the created custom throttling policy DTO
            adminApiTestHelper.verifyCustomThrottlePolicyDTO(customRuleDTO, addedPolicyDTO);
        } catch (ApiException e) {
            if (userMode == TestUserMode.TENANT_ADMIN) {
                Assert.assertEquals(e.getCode(), HttpStatus.SC_FORBIDDEN);
            }
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test get custom throttling policy",
            dependsOnMethods = "testAddPolicy")
    public void testGetPolicy() {

        if (userMode == TestUserMode.TENANT_ADMIN) {
            policyId = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        }
        try {
            //Get the added custom throttling policy
            ApiResponse<CustomRuleDTO> retrievedPolicy = restAPIAdmin.getCustomThrottlingPolicy(policyId);
            CustomRuleDTO retrievedPolicyDTO = retrievedPolicy.getData();
            Assert.assertEquals(retrievedPolicy.getStatusCode(), HttpStatus.SC_OK);

            //Verify the retrieved custom throttling policy DTO
            adminApiTestHelper.verifyCustomThrottlePolicyDTO(customRuleDTO, retrievedPolicyDTO);
        } catch (ApiException e) {
            if (userMode == TestUserMode.TENANT_ADMIN) {
                Assert.assertEquals(e.getCode(), HttpStatus.SC_FORBIDDEN);
            }
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test update custom throttling policy",
            dependsOnMethods = "testGetPolicy")
    public void testUpdatePolicy() {

        try {
            //Update the custom throttling policy
            String updatedDescription = "This is a updated test custom throttle policy";
            customRuleDTO.setDescription(updatedDescription);
            ApiResponse<CustomRuleDTO> updatedPolicy;
            updatedPolicy = restAPIAdmin.updateCustomThrottlingPolicy(policyId, customRuleDTO);

            CustomRuleDTO updatedPolicyDTO = updatedPolicy.getData();
            Assert.assertEquals(updatedPolicy.getStatusCode(), HttpStatus.SC_OK);

            //Verify the updated custom throttling policy DTO
            adminApiTestHelper.verifyCustomThrottlePolicyDTO(customRuleDTO, updatedPolicyDTO);
        } catch (ApiException e) {
            if (userMode == TestUserMode.TENANT_ADMIN) {
                Assert.assertEquals(e.getCode(), HttpStatus.SC_FORBIDDEN);
            }
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add custom throttling policy with existing policy name",
            dependsOnMethods = "testUpdatePolicy")
    public void testAddPolicyWithExistingPolicyName() {

        //Exception occurs when adding an custom throttling policy with an existing policy name. The status code
        //in the Exception object is used to assert this scenario
        try {
            restAPIAdmin.addCustomThrottlingPolicy(customRuleDTO);
        } catch (ApiException e) {
            if (userMode == TestUserMode.TENANT_ADMIN) {
                Assert.assertEquals(e.getCode(), HttpStatus.SC_FORBIDDEN);
            } else {
                Assert.assertEquals(e.getCode(), HttpStatus.SC_CONFLICT);
            }
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test delete custom throttling policy",
            dependsOnMethods = "testAddPolicyWithExistingPolicyName")
    public void testDeletePolicy() {

        try {
            ApiResponse<Void> apiResponse = restAPIAdmin.deleteCustomThrottlingPolicy(policyId);
            Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);
        } catch (ApiException e) {
            if (userMode == TestUserMode.TENANT_ADMIN) {
                Assert.assertEquals(e.getCode(), HttpStatus.SC_FORBIDDEN);
            }
        }
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
            if (userMode == TestUserMode.TENANT_ADMIN) {
                Assert.assertEquals(e.getCode(), HttpStatus.SC_FORBIDDEN);
            } else {
                Assert.assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND);
            }
        }
    }
}
