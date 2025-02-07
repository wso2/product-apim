/*
 *Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.am.integration.tests.server.restart;

import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AccessibilityOfBlockAPIServerRestartTestCase extends APIManagerLifecycleBaseTest {

    private final String API_CONTEXT = "BlockAPI";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private Map<String, String> requestHeaders;
    private String accessibilityOfBlockApiId;
    private String accessibilityOfBlockApplicationId;

    @BeforeClass(alwaysRun = true)
    public void initialize(ITestContext ctx) throws APIManagerIntegrationTestException {
        super.init();
        accessibilityOfBlockApplicationId = (String) ctx.getAttribute("accessibilityOfBlockApplicationId");
        accessibilityOfBlockApiId = (String) ctx.getAttribute("accessibilityOfBlockApiId");

    }

    @Test(groups = {"wso2.am"}, description = "Test invocation of the APi before block")
    public void testInvokeAPIBeforeChangeAPILifecycleToBlock() throws Exception {

        waitForAPIDeploymentSync(user.getUserName(), "BlockAPITest", "1.0.0",
                APIMIntegrationConstants.IS_API_EXISTS);

        ArrayList accessibilityOfBlockGrantTypes = new ArrayList();
        accessibilityOfBlockGrantTypes.add("client_credentials");

        //get access token
        ApplicationKeyDTO accessibilityOfBlockApplicationKeyDTO = restAPIStore.generateKeys(accessibilityOfBlockApplicationId,
                "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                null, accessibilityOfBlockGrantTypes);
        Assert.assertNotNull(accessibilityOfBlockApplicationKeyDTO.getToken());
        String accessibilityOfBlockAccessToken = accessibilityOfBlockApplicationKeyDTO.getToken().getAccessToken();
        // Create requestHeaders
        requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessibilityOfBlockAccessToken);

        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT,
                        API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api before block");
        String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
        Assert.assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA),
                "Response data mismatched when invoke  API  before block" +
                        " Response Data:" + oldVersionInvokeResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Change API lifecycle to block",
            dependsOnMethods = "testInvokeAPIBeforeChangeAPILifecycleToBlock")
    public void testChangeAPILifecycleToBlock() throws Exception {
        //Block the API version 1.0.0
        HttpResponse response = restAPIPublisher
                .changeAPILifeCycleStatus(accessibilityOfBlockApiId, APILifeCycleAction.BLOCK.getAction(), null);
        Assert.assertEquals(response.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API publish Response code is invalid " + accessibilityOfBlockApiId);
    }


    @Test(groups = {"wso2.am"}, description = "Invocation og the APi after block",
            dependsOnMethods = "testChangeAPILifecycleToBlock")
    public void testInvokeAPIAfterChangeAPILifecycleToBlock() throws Exception {
        waitForAPIDeployment();

        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                                API_END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE,
                "Response code mismatched when invoke api after block");
        Assert.assertTrue(oldVersionInvokeResponse.getData().contains(HTTP_RESPONSE_DATA_API_BLOCK),
                "Response data mismatched when invoke  API  after block" +
                        " Response Data:" + oldVersionInvokeResponse.getData());
    }
}
