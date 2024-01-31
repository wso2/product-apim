/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;

import static org.wso2.am.integration.tests.server.restart.ServerRestartTestCase.restartServer;

public class ApplicationThrottlingPolicyServerRestartTestCase extends APIMIntegrationBaseTest {

    private ApplicationThrottlePolicyDTO defaultApplicationThrottlePolicyDTO;
    private AdminApiTestHelper adminApiTestHelper;
    private String policyId;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment(ITestContext ctx) throws Exception {
        super.init();
        ThrottleLimitDTO defaultLimit = new ThrottleLimitDTO()
                .type(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT)
                .requestCount(new RequestCountLimitDTO().requestCount(10l).unitTime(1).timeUnit("min")).bandwidth(null);

        adminApiTestHelper = new AdminApiTestHelper();

        ApplicationThrottlePolicyDTO applicationThrottlePolicyDTO = new ApplicationThrottlePolicyDTO();
        applicationThrottlePolicyDTO.setPolicyName("DefaultApplicationLevelTier");
        applicationThrottlePolicyDTO.setDefaultLimit(defaultLimit);

        ApiResponse<ApplicationThrottlePolicyDTO> applicationThrottlePolicyDTOApiResponse =
                restAPIAdmin.addApplicationThrottlingPolicy(applicationThrottlePolicyDTO);

        Assert.assertEquals(applicationThrottlePolicyDTOApiResponse.getStatusCode(), 201);
        this.defaultApplicationThrottlePolicyDTO = applicationThrottlePolicyDTOApiResponse.getData();

        policyId = defaultApplicationThrottlePolicyDTO.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");

    }

    @Test(groups = {"wso2.am"}, description = "Test get application throttling policy")
    public void testGetPolicy() throws ApiException {

        ApiResponse<ApplicationThrottlePolicyDTO> retrievedPolicy =
                restAPIAdmin.getApplicationThrottlingPolicy(policyId);
        ApplicationThrottlePolicyDTO retrievedPolicyDTO = retrievedPolicy.getData();
        Assert.assertEquals(retrievedPolicy.getStatusCode(), HttpStatus.SC_OK);

        //Verify the retrieved application throttling policy DTO
        adminApiTestHelper.verifyApplicationThrottlePolicyDTO(defaultApplicationThrottlePolicyDTO, retrievedPolicyDTO);
    }


    @Test(groups = {"wso2.am"}, description = "Test delete application throttling policy",
            dependsOnMethods = "testGetPolicy")
    public void testRestartAfterDeletePolicy() throws Exception {

        ApiResponse<Void> apiResponse = restAPIAdmin.deleteApplicationThrottlingPolicy(policyId);
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);
        Thread.sleep(5000);
        restartServer();
        ApiResponse<ApplicationThrottlePolicyDTO> retrievedPolicy = null;
        try {
            retrievedPolicy = restAPIAdmin.getApplicationThrottlingPolicy(policyId);
            Assert.fail("The policy should not be visible after deleting");
        } catch (ApiException e) {
            Assert.assertNull(retrievedPolicy, "Response should not contain the deleted policy");
        }
    }

}