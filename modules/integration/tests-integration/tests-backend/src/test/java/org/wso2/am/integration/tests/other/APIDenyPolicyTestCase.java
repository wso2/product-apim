package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.BlockingConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.BlockingConditionListDTO;
import org.wso2.am.integration.clients.admin.api.dto.BlockingConditionStatusDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class APIDenyPolicyTestCase extends APIManagerLifecycleBaseTest {
    private final Log log = LogFactory.getLog(APIDenyPolicyTestCase.class);
    private AdminApiTestHelper adminApiTestHelper;
    private String apiId;
    private BlockingConditionDTO blockingConditionDTO;
    private List<String> addedPolicyIds = new ArrayList<>();

    @Factory(dataProvider = "userModeDataProvider")
    public APIDenyPolicyTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.SUPER_TENANT_EMAIL_USER }};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        adminApiTestHelper = new AdminApiTestHelper();
    }

    //api context test case
    @Test(groups = { "wso2.am" }, description = "Test add API deny policy")
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
        apiId = postResponse.getData();
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

    @Test(groups = { "wso2.am" }, description = "Test get API deny policy")
    public void testGetAddedDenyPolicy() throws ApiException {
        ApiResponse<BlockingConditionDTO> response = restAPIAdmin.getDenyThrottlingPolicy(blockingConditionDTO.getConditionId());
        String retrievedConditionId = response.getData().getConditionId();

        Assert.assertEquals(blockingConditionDTO.getConditionId(),retrievedConditionId);
    }

    @Test(groups = { "wso2.am" }, description = "Test add API deny policy with non existing context")
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

    @Test(groups = { "wso2.am" }, description = "Test add API deny policy with the same name", dependsOnMethods = "testAddAPIDenyPolicy")
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

    @Test(groups = { "wso2.am" }, description = "Test update API deny policy", dependsOnMethods = "testAddAPIDenyPolicy")
    public void testUpdateAPIDenyPolicyStatus() throws ApiException {

        String denyPolicyId = blockingConditionDTO.getConditionId();
        String conditionType = blockingConditionDTO.getConditionType().toString();
        boolean conditionStatus = false;

        BlockingConditionStatusDTO blockingConditionStatusDTO = new BlockingConditionStatusDTO();
        blockingConditionStatusDTO.setConditionStatus(conditionStatus);
        blockingConditionStatusDTO.setConditionId(denyPolicyId);

       ApiResponse<BlockingConditionDTO> updatedCondition =  restAPIAdmin.updateDenyThrottlingPolicy(denyPolicyId,conditionType, blockingConditionStatusDTO);
       Assert.assertEquals(updatedCondition.getStatusCode(), HttpStatus.SC_OK);

       BlockingConditionDTO updatedBlockedCondition = updatedCondition.getData();
       Assert.assertEquals(updatedBlockedCondition.isConditionStatus().booleanValue(), conditionStatus);
    }

    @Test(groups = { "wso2.am" }, description = "Test delete API deny policy", dependsOnMethods = "testUpdateAPIDenyPolicyStatus")
    public void testDeleteAPIDenyPolicy() throws ApiException {
        String denyPolicyId = blockingConditionDTO.getConditionId();

        ApiResponse<Void> response = restAPIAdmin.deleteDenyThrottlingPolicy(denyPolicyId);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
    }

    @Test(groups = { "wso2.am" }, description = "Test add API Application Vise deny policy")
    public void testAddAPIDenyPolicyApplicationVise() throws Exception {

        //create application
        HttpResponse applicationResponse = restAPIStore.createApplication("denyPolicyCheckApp",
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);

        boolean conditionStatus = true;
        Object conditionValue = "admin:DefaultApplication";

        BlockingConditionDTO blockingConditionDTO = new BlockingConditionDTO();
        blockingConditionDTO.setConditionStatus(conditionStatus);
        blockingConditionDTO.setConditionValue(conditionValue);
        blockingConditionDTO.setConditionType(BlockingConditionDTO.ConditionTypeEnum.APPLICATION);

        ApiResponse<BlockingConditionDTO> addedPolicy = restAPIAdmin.addDenyThrottlingPolicy(blockingConditionDTO);

        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        BlockingConditionDTO addedBlockingConditionDTO = addedPolicy.getData();
        String policyId = addedBlockingConditionDTO.getConditionId();
        Assert.assertNotNull(policyId, "The deny policy ID cannot be null or empty");

        addedPolicyIds.add(policyId);
    }

    @Test(groups = { "wso2.am" }, description = "Test add API Deny Policy to Non Existing Application")
    public void testAddAPIDenyPolicyToNonExistingApplication() {

        boolean conditionStatus = true;
        Object conditionValue = "admin:NonExistingApp";

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

    @Test(groups = { "wso2.am" }, description = "Test add API User Vise deny policy", dependsOnMethods = "testAddAPIDenyPolicyApplicationVise")
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

        addedPolicyIds.add(policyId);

    }

    @Test(groups = { "wso2.am" }, description = "Test add API Deny Policy to Invalid User", dependsOnMethods = "testAddAPIDenyPolicyApplicationVise")
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

    @Test(groups = { "wso2.am" }, description = "Test add API IP Address Vise deny policy", dependsOnMethods = "testAddAPIDenyPolicyApplicationVise")
    public void testAddAPIDenyPolicyIPAddressWise() throws Exception {

        boolean conditionStatus = true;
        Map<String, Object> valueMap = new LinkedHashMap<>();
        valueMap.put("invert", false);
        valueMap.put("fixedIp","127.0.0.1");

        BlockingConditionDTO blockingConditionDTO = new BlockingConditionDTO();
        blockingConditionDTO.setConditionStatus(conditionStatus);
        blockingConditionDTO.setConditionValue(valueMap);
        blockingConditionDTO.setConditionType(BlockingConditionDTO.ConditionTypeEnum.IP);

        ApiResponse<BlockingConditionDTO> addedPolicy = restAPIAdmin.addDenyThrottlingPolicy(blockingConditionDTO);

        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        BlockingConditionDTO addedBlockingConditionDTO = addedPolicy.getData();
        String policyId = addedBlockingConditionDTO.getConditionId();
        Assert.assertNotNull(policyId, "The deny policy ID cannot be null or empty");

        addedPolicyIds.add(policyId);
    }

    @Test(groups = { "wso2.am" }, description = "Test add API deny policy with invalid IP Address", dependsOnMethods = "testAddAPIDenyPolicyApplicationVise")
    public void testAddAPIDenyPolicyInvalidIPAddress() {

        boolean conditionStatus = true;
        Map<String, Object> valueMap = new LinkedHashMap<>();
        valueMap.put("invert", false);
        valueMap.put("fixedIp","127..0.0.1");

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

    @Test(groups = { "wso2.am" }, description = "Test add API IP Address Range Vise deny policy", dependsOnMethods = "testAddAPIDenyPolicyIPAddressWise")
    public void testAddAPIDenyPolicyIPRangeWise() throws Exception {

        boolean conditionStatus = true;
        Map<String, Object> valueMap = new LinkedHashMap<>();
        valueMap.put("invert", false);
        valueMap.put("startingIp","127.0.0.1");
        valueMap.put("endingIp","127.0.0.5");


        BlockingConditionDTO blockingConditionDTO = new BlockingConditionDTO();
        blockingConditionDTO.setConditionStatus(conditionStatus);
        blockingConditionDTO.setConditionValue(valueMap);
        blockingConditionDTO.setConditionType(BlockingConditionDTO.ConditionTypeEnum.IPRANGE);

        ApiResponse<BlockingConditionDTO> addedPolicy = restAPIAdmin.addDenyThrottlingPolicy(blockingConditionDTO);

        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        BlockingConditionDTO addedBlockingConditionDTO = addedPolicy.getData();
        String policyId = addedBlockingConditionDTO.getConditionId();
        Assert.assertNotNull(policyId, "The deny policy ID cannot be null or empty");

        addedPolicyIds.add(policyId);
    }

    @Test(groups = { "wso2.am" }, description = "Test add API deny policy with invalid Ip Address range", dependsOnMethods = "testAddAPIDenyPolicyApplicationVise")
    public void testAddAPIDenyPolicyInvalidIPAddressRange() {

        boolean conditionStatus = true;
        Map<String, Object> valueMap = new LinkedHashMap<>();
        valueMap.put("startingIp","127..0.0.1");
        valueMap.put("endingIp","127.0.0.5");

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
        for (String conditionId :addedPolicyIds) {
            restAPIAdmin.deleteDenyThrottlingPolicy(conditionId);
        }
        super.cleanUp();
    }

}