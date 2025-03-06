/*
 * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.restapi.admin.throttlingpolicy;

import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.BlockingConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.BlockingConditionListDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;

public class DenyPolicySearchTestCase extends APIMIntegrationBaseTest {

    private final String API1_NAME = "TestAPI1";
    private final String API1_CONTEXT = "test";
    private final String API1_VERSION = "1.0.0";
    private final String API2_NAME = "TestAPI2";
    private final String API2_CONTEXT = "test/abc";
    private final String API2_VERSION = "2.0.0";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String CONDITION_TYPE_API = "API";
    private String blockingCondition1ID;
    private String blockingCondition2ID;
    private String providerName;
    private String api1Id;
    private String api2Id;
    private BlockingConditionDTO blockingConditionDTO;

    @Factory(dataProvider = "userModeDataProvider")
    public DenyPolicySearchTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        providerName = user.getUserName();
    }
    @Test(groups = {"wso2.am"}, description = "Test add new blocking conditions")
    public void testAddNewBlockingConditions() throws Exception {
        // Create and publish API 1
        APIRequest apiRequest;
        apiRequest = new APIRequest(API1_NAME, API1_CONTEXT, new URL(backEndServerUrl.getWebAppURLHttps()
                + API_END_POINT_POSTFIX_URL));
        apiRequest.setVersion(API1_VERSION);
        apiRequest.setProvider(providerName);
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        Assert.assertEquals(apiResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Response Code miss matched when creating the API");
        api1Id = apiResponse.getData();
        restAPIPublisher.changeAPILifeCycleStatus(api1Id, Constants.PUBLISHED);
        waitForAPIDeployment();

        // Create and publish API 2
        apiRequest = new APIRequest(API2_NAME, API2_CONTEXT, new URL(backEndServerUrl.getWebAppURLHttps()
                + API_END_POINT_POSTFIX_URL));
        apiRequest.setVersion(API2_VERSION);
        apiRequest.setProvider(providerName);
        apiResponse = restAPIPublisher.addAPI(apiRequest);
        Assert.assertEquals(apiResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Response Code miss matched when creating the API");
        api2Id = apiResponse.getData();
        restAPIPublisher.changeAPILifeCycleStatus(api2Id, Constants.PUBLISHED);
        waitForAPIDeployment();

        // Add new blocking condition for API1's context
        blockingConditionDTO = DtoFactory.createBlockingConditionDTO(BlockingConditionDTO.ConditionTypeEnum.API,
                apiContextResolver(API1_CONTEXT, API1_VERSION), true);
        ApiResponse<BlockingConditionDTO> addedBlacklistThrottlingPolicy = restAPIAdmin.addDenyThrottlingPolicy(
                blockingConditionDTO);
        Assert.assertEquals(addedBlacklistThrottlingPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        blockingCondition1ID = addedBlacklistThrottlingPolicy.getData().getConditionId();

        // Add new blocking condition for API2's context
        blockingConditionDTO = DtoFactory.createBlockingConditionDTO(BlockingConditionDTO.ConditionTypeEnum.API,
                apiContextResolver(API2_CONTEXT, API2_VERSION), true);
        addedBlacklistThrottlingPolicy = restAPIAdmin.addDenyThrottlingPolicy(
                blockingConditionDTO);
        Assert.assertEquals(addedBlacklistThrottlingPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        blockingCondition2ID = addedBlacklistThrottlingPolicy.getData().getConditionId();
    }

    @Test(groups = {
            "wso2.am" }, description = "Test retrieve block conditions by condition type and value", dependsOnMethods = "testAddNewBlockingConditions")
    public void testGetBlockConditionsByConditionTypeAndValue() throws Exception {
        // Retrieve blocking condition values that contains /test
        BlockingConditionListDTO conditionsList = restAPIAdmin.getBlockingConditionsByConditionTypeAndValue(
                "conditionType:API&conditionValue:/test");
        Assert.assertNotNull(conditionsList);

        int countTest1 = 0;
        int countTest2 = 0;
        for (BlockingConditionDTO condition : conditionsList.getList()) {
            if (apiContextResolver(API1_CONTEXT, API1_VERSION).equals(condition.getConditionValue())) {
                countTest1++;
            } else if (apiContextResolver(API2_CONTEXT, API2_VERSION).equals(condition.getConditionValue())) {
                countTest2++;
            }
        }
        Assert.assertEquals(countTest1, 1);
        Assert.assertEquals(countTest2, 1);

        // Retrieve blocking condition values that contains /test/abc
        conditionsList = restAPIAdmin.getBlockingConditionsByConditionTypeAndValue(
                "conditionType:API&conditionValue:/test/abc");
        Assert.assertNotNull(conditionsList);

        countTest1 = 0;
        countTest2 = 0;
        for (BlockingConditionDTO condition : conditionsList.getList()) {
            if (apiContextResolver(API1_CONTEXT, API1_VERSION).equals(condition.getConditionValue())) {
                countTest1++;
            } else if (apiContextResolver(API2_CONTEXT, API2_VERSION).equals(condition.getConditionValue())) {
                countTest2++;
            }
        }
        Assert.assertEquals(countTest1, 0);
        Assert.assertEquals(countTest2, 1);
    }

    @Test(groups = {
            "wso2.am" }, description = "Test retrieve block conditions by condition type and exact value", dependsOnMethods = "testAddNewBlockingConditions")
    public void testGetBlockConditionsByConditionTypeAndExactValue() throws Exception {
        BlockingConditionListDTO conditionsList = restAPIAdmin.getBlockingConditionsByConditionTypeAndValue(
                "conditionType:API&conditionValue:\"/test\"");
        Assert.assertNotNull(conditionsList);
        Assert.assertEquals(conditionsList.getList().size(), 0);

        conditionsList = restAPIAdmin.getBlockingConditionsByConditionTypeAndValue(
                "conditionType:API&conditionValue:\"/test/abc\"");
        Assert.assertNotNull(conditionsList);
        Assert.assertEquals(conditionsList.getList().size(), 0);

        String conditionValue = apiContextResolver(API1_CONTEXT, API1_VERSION);
        conditionsList = restAPIAdmin.getBlockingConditionsByConditionTypeAndValue(
                "conditionType:API&conditionValue:" + conditionValue);
        Assert.assertNotNull(conditionsList);
        Assert.assertEquals(conditionsList.getList().size(), 1);
        Assert.assertEquals(conditionsList.getList().get(0).getConditionValue(), conditionValue);

        conditionValue = apiContextResolver(API2_CONTEXT, API2_VERSION);
        conditionsList = restAPIAdmin.getBlockingConditionsByConditionTypeAndValue(
                "conditionType:API&conditionValue:" + conditionValue);
        Assert.assertNotNull(conditionsList);
        Assert.assertEquals(conditionsList.getList().size(), 1);
        Assert.assertEquals(conditionsList.getList().get(0).getConditionValue(), conditionValue);
    }

    private String apiContextResolver(String context, String version) {
        if (userMode == TestUserMode.TENANT_ADMIN || userMode == TestUserMode.TENANT_USER) {
            return "/t/wso2.com/" + context + "/" + version;
        } else {
            return "/" + context + "/" + version;
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIAdmin.deleteDenyThrottlingPolicy(blockingCondition1ID);
        restAPIAdmin.deleteDenyThrottlingPolicy(blockingCondition2ID);
        restAPIPublisher.deleteAPI(api1Id);
        restAPIPublisher.deleteAPI(api2Id);
    }
}