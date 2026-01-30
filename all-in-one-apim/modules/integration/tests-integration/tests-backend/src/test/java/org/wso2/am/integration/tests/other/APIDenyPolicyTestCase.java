/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.BlockingConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.BlockingConditionStatusDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class APIDenyPolicyTestCase extends APIManagerLifecycleBaseTest {
    private final Log log = LogFactory.getLog(APIDenyPolicyTestCase.class);
    private BlockingConditionDTO blockingConditionDTO;
    private List<String> addedPolicyIds = new ArrayList<>();
    private ArrayList<String> grantTypes;

    private String invokingAPIId;

    @Factory(dataProvider = "userModeDataProvider")
    public APIDenyPolicyTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN}};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);


        grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
    }

    //api context test case
    @Test(groups = {"wso2.am"}, description = "Test add API deny policy")
    public void testAddAPIDenyPolicy() throws Exception {

        //Add API
        String apiName = "DenyPolicyTestAPI";
        String apiContext = "deny";
        String apiVersion = "1.0";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(url));
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(user.getUserName());

        //Add the API using the API publisher.
        HttpResponse postResponse = restAPIPublisher.addAPI(apiRequest);
        String apiId = postResponse.getData();
        waitForAPIDeployment();
        //create Deny Policy
        boolean conditionStatus = true;
        Object conditionValue = "/deny/1.0";

        blockingConditionDTO = new BlockingConditionDTO();
        blockingConditionDTO.setConditionStatus(conditionStatus);
        blockingConditionDTO.setConditionValue(conditionValue);
        blockingConditionDTO.setConditionType(BlockingConditionDTO.ConditionTypeEnum.API);

        ApiResponse<BlockingConditionDTO> addedPolicy = restAPIAdmin.addDenyThrottlingPolicy(blockingConditionDTO);

        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        BlockingConditionDTO addedBlockingConditionDTO = addedPolicy.getData();
        String policyId = addedBlockingConditionDTO.getConditionId();
        Assert.assertNotNull(policyId, "The deny policy ID cannot be null or empty");
        blockingConditionDTO.setConditionId(policyId);
    }

    @Test(groups = {"wso2.am"}, description = "Test get API deny policy", dependsOnMethods = "testAddAPIDenyPolicy")
    public void testGetAddedDenyPolicy() throws ApiException {
        ApiResponse<BlockingConditionDTO> response = restAPIAdmin.getDenyThrottlingPolicy(blockingConditionDTO.getConditionId());
        String retrievedConditionId = response.getData().getConditionId();

        Assert.assertEquals(blockingConditionDTO.getConditionId(), retrievedConditionId);
    }

    @Test(groups = {"wso2.am"}, description = "Test add API deny policy with non existing context", dependsOnMethods = "testGetAddedDenyPolicy")
    public void testAddDenyPolicyWithNonExistingContext() {

        //create Deny Policy
        boolean conditionStatus = true;
        Object conditionValue = "/denyNonExisting/1.0.0/";

        BlockingConditionDTO blockingConditionDTO = new BlockingConditionDTO();
        blockingConditionDTO.setConditionStatus(conditionStatus);
        blockingConditionDTO.setConditionValue(conditionValue);
        blockingConditionDTO.setConditionType(BlockingConditionDTO.ConditionTypeEnum.API);
        try {
            ApiResponse<BlockingConditionDTO> addedPolicy = restAPIAdmin.addDenyThrottlingPolicy(blockingConditionDTO);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
            Assert.assertTrue(e.getResponseBody().contains("Couldn't Save Block Condition Due to Invalid API Context /denyNonExisting/1.0.0/"));
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add API deny policy with the same name", dependsOnMethods = "testAddDenyPolicyWithNonExistingContext")
    public void testAddAPIDenyPolicyWithTheSameContext() {

        //create Deny Policy
        boolean conditionStatus = true;
        Object conditionValue = "/deny/1.0";

        BlockingConditionDTO blockingConditionDTO = new BlockingConditionDTO();
        blockingConditionDTO.setConditionStatus(conditionStatus);
        blockingConditionDTO.setConditionValue(conditionValue);
        blockingConditionDTO.setConditionType(BlockingConditionDTO.ConditionTypeEnum.API);

        try {
            ApiResponse<BlockingConditionDTO> addedPolicy = restAPIAdmin.addDenyThrottlingPolicy(blockingConditionDTO);
            Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED, "Duplicated Context Deny Policy Added");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_CONFLICT);
            Assert.assertTrue(e.getResponseBody().contains("A black list item with type: API, value: /deny/1.0 already exists"));
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test update API deny policy", dependsOnMethods = "testAddAPIDenyPolicyWithTheSameContext")
    public void testUpdateAPIDenyPolicyStatus() throws ApiException {

        String denyPolicyId = blockingConditionDTO.getConditionId();
        String conditionType = blockingConditionDTO.getConditionType().toString();
        boolean conditionStatus = false;

        BlockingConditionStatusDTO blockingConditionStatusDTO = new BlockingConditionStatusDTO();
        blockingConditionStatusDTO.setConditionStatus(conditionStatus);
        blockingConditionStatusDTO.setConditionId(denyPolicyId);

        ApiResponse<BlockingConditionDTO> updatedCondition = restAPIAdmin.updateDenyThrottlingPolicy(denyPolicyId, conditionType, blockingConditionStatusDTO);
        Assert.assertEquals(updatedCondition.getStatusCode(), HttpStatus.SC_OK);

        BlockingConditionDTO updatedBlockedCondition = updatedCondition.getData();
        Assert.assertEquals(updatedBlockedCondition.isConditionStatus().booleanValue(), conditionStatus);
    }

    @Test(groups = {"wso2.am"}, description = "Test delete API deny policy", dependsOnMethods = "testUpdateAPIDenyPolicyStatus")
    public void testDeleteAPIDenyPolicy() throws ApiException {
        String denyPolicyId = blockingConditionDTO.getConditionId();

        ApiResponse<Void> response = restAPIAdmin.deleteDenyThrottlingPolicy(denyPolicyId);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
    }

    @Test(groups = {"wso2.am"}, description = "Test add API Application Vise deny policy", dependsOnMethods = "testDeleteAPIDenyPolicy")
    public void testAddAPIDenyPolicyApplicationVise() throws Exception {

        //create application
        String userName = user.getUserName();

        HttpResponse applicationResponse = restAPIStore.createApplication("denyPolicyCheckApp",
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);

        boolean conditionStatus = true;
        Object conditionValue = userName+":denyPolicyCheckApp";

        BlockingConditionDTO blockingConditionDTO = new BlockingConditionDTO();
        blockingConditionDTO.setConditionStatus(conditionStatus);
        blockingConditionDTO.setConditionValue(conditionValue);
        blockingConditionDTO.setConditionType(BlockingConditionDTO.ConditionTypeEnum.APPLICATION);

        ApiResponse<BlockingConditionDTO> addedPolicy = restAPIAdmin.addDenyThrottlingPolicy(blockingConditionDTO);

        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        BlockingConditionDTO addedBlockingConditionDTO = addedPolicy.getData();
        String policyId = addedBlockingConditionDTO.getConditionId();
        Assert.assertNotNull(policyId, "The deny policy ID cannot be null or empty");

        restAPIAdmin.deleteDenyThrottlingPolicy(policyId);
        restAPIStore.deleteApplication(applicationResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test add API Deny Policy to Non Existing Application", dependsOnMethods = "testAddAPIDenyPolicyApplicationVise")
    public void testAddAPIDenyPolicyToNonExistingApplication() {
        String userName = user.getUserName();
        boolean conditionStatus = true;
        Object conditionValue = userName+":NonExistingApp";

        BlockingConditionDTO blockingConditionDTO = new BlockingConditionDTO();
        blockingConditionDTO.setConditionStatus(conditionStatus);
        blockingConditionDTO.setConditionValue(conditionValue);
        blockingConditionDTO.setConditionType(BlockingConditionDTO.ConditionTypeEnum.APPLICATION);

        try {
            ApiResponse<BlockingConditionDTO> addedPolicy = restAPIAdmin.addDenyThrottlingPolicy(blockingConditionDTO);
            Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED, "Deny Policy Added without a non-existing application");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add API User Vise deny policy", dependsOnMethods = "testAddAPIDenyPolicyToNonExistingApplication")
    public void testAddAPIDenyPolicyUserVise() throws Exception {

        boolean conditionStatus = true;
        Object conditionValue = "admin";

        BlockingConditionDTO blockingConditionDTO = new BlockingConditionDTO();
        blockingConditionDTO.setConditionStatus(conditionStatus);
        blockingConditionDTO.setConditionValue(conditionValue);
        blockingConditionDTO.setConditionType(BlockingConditionDTO.ConditionTypeEnum.USER);

        ApiResponse<BlockingConditionDTO> addedPolicy = restAPIAdmin.addDenyThrottlingPolicy(blockingConditionDTO);

        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        BlockingConditionDTO addedBlockingConditionDTO = addedPolicy.getData();
        String policyId = addedBlockingConditionDTO.getConditionId();
        Assert.assertNotNull(policyId, "The deny policy ID cannot be null or empty");

        restAPIAdmin.deleteDenyThrottlingPolicy(policyId);

    }

    @Test(groups = {"wso2.am"}, description = "Test add API Deny Policy to Invalid User", dependsOnMethods = "testAddAPIDenyPolicyUserVise")
    public void testAddAPIDenyPolicyWithInvalidUser() {

        boolean conditionStatus = true;
        Object conditionValue = "nonExistingAdmin";

        BlockingConditionDTO blockingConditionDTO = new BlockingConditionDTO();
        blockingConditionDTO.setConditionStatus(conditionStatus);
        blockingConditionDTO.setConditionValue(conditionValue);
        blockingConditionDTO.setConditionType(BlockingConditionDTO.ConditionTypeEnum.USER);

        try {
            ApiResponse<BlockingConditionDTO> addedPolicy = restAPIAdmin.addDenyThrottlingPolicy(blockingConditionDTO);
            Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED, "Deny Policy Added with an invalid IP");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_CONFLICT);
        }

    }

    @Test(groups = {"wso2.am"}, description = "Test add API IP Address Vise deny policy", dependsOnMethods = "testAddAPIDenyPolicyWithInvalidUser")
    public void testAddAPIDenyPolicyIPAddressWise() throws Exception {

        boolean conditionStatus = true;
        Map<String, Object> valueMap = new LinkedHashMap<>();
        valueMap.put("invert", false);
        valueMap.put("fixedIp", "127.0.0.1");

        BlockingConditionDTO blockingConditionDTO = new BlockingConditionDTO();
        blockingConditionDTO.setConditionStatus(conditionStatus);
        blockingConditionDTO.setConditionValue(valueMap);
        blockingConditionDTO.setConditionType(BlockingConditionDTO.ConditionTypeEnum.IP);

        ApiResponse<BlockingConditionDTO> addedPolicy = restAPIAdmin.addDenyThrottlingPolicy(blockingConditionDTO);

        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        BlockingConditionDTO addedBlockingConditionDTO = addedPolicy.getData();
        String policyId = addedBlockingConditionDTO.getConditionId();
        Assert.assertNotNull(policyId, "The deny policy ID cannot be null or empty");

        restAPIAdmin.deleteDenyThrottlingPolicy(policyId);
    }

    @Test(groups = {"wso2.am"}, description = "Test add API deny policy with invalid IP Address", dependsOnMethods = "testAddAPIDenyPolicyIPAddressWise")
    public void testAddAPIDenyPolicyInvalidIPAddress() {

        boolean conditionStatus = true;
        Map<String, Object> valueMap = new LinkedHashMap<>();
        valueMap.put("invert", false);
        valueMap.put("fixedIp", "127..0.0.1");

        BlockingConditionDTO blockingConditionDTO = new BlockingConditionDTO();
        blockingConditionDTO.setConditionStatus(conditionStatus);
        blockingConditionDTO.setConditionValue(valueMap);
        blockingConditionDTO.setConditionType(BlockingConditionDTO.ConditionTypeEnum.IP);

        try {
            ApiResponse<BlockingConditionDTO> addedPolicy = restAPIAdmin.addDenyThrottlingPolicy(blockingConditionDTO);
            Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED, "Deny Policy Added with an invalid IP");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

    }

    @Test(groups = {"wso2.am"}, description = "Test add API IP Address Range Vise deny policy", dependsOnMethods = "testAddAPIDenyPolicyInvalidIPAddress")
    public void testAddAPIDenyPolicyIPRangeWise() throws Exception {

        boolean conditionStatus = true;
        Map<String, Object> valueMap = new LinkedHashMap<>();
        valueMap.put("invert", false);
        valueMap.put("startingIp", "127.0.0.1");
        valueMap.put("endingIp", "127.0.0.5");


        BlockingConditionDTO blockingConditionDTO = new BlockingConditionDTO();
        blockingConditionDTO.setConditionStatus(conditionStatus);
        blockingConditionDTO.setConditionValue(valueMap);
        blockingConditionDTO.setConditionType(BlockingConditionDTO.ConditionTypeEnum.IPRANGE);

        ApiResponse<BlockingConditionDTO> addedPolicy = restAPIAdmin.addDenyThrottlingPolicy(blockingConditionDTO);

        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        BlockingConditionDTO addedBlockingConditionDTO = addedPolicy.getData();
        String policyId = addedBlockingConditionDTO.getConditionId();
        Assert.assertNotNull(policyId, "The deny policy ID cannot be null or empty");

        restAPIAdmin.deleteDenyThrottlingPolicy(policyId);
    }

    @Test(groups = {"wso2.am"}, description = "Test add API deny policy with invalid Ip Address range", dependsOnMethods = "testAddAPIDenyPolicyIPRangeWise")
    public void testAddAPIDenyPolicyInvalidIPAddressRange() {

        boolean conditionStatus = true;
        Map<String, Object> valueMap = new LinkedHashMap<>();
        valueMap.put("startingIp", "127..0.0.1");
        valueMap.put("endingIp", "127.0.0.5");

        BlockingConditionDTO blockingConditionDTO = new BlockingConditionDTO();
        blockingConditionDTO.setConditionStatus(conditionStatus);
        blockingConditionDTO.setConditionValue(valueMap);
        blockingConditionDTO.setConditionType(BlockingConditionDTO.ConditionTypeEnum.IP);

        try {
            ApiResponse<BlockingConditionDTO> addedPolicy = restAPIAdmin.addDenyThrottlingPolicy(blockingConditionDTO);
            Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED, "Deny Policy Added with an invalid IP");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        for (String conditionId : addedPolicyIds) {
            restAPIAdmin.deleteDenyThrottlingPolicy(conditionId);
        }
        super.cleanUp();
    }

}
