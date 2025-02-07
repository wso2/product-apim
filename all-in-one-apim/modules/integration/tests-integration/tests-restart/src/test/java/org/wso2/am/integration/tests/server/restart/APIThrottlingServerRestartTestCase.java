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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This will API Throttling for APIs.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL})
public class APIThrottlingServerRestartTestCase extends APIManagerLifecycleBaseTest {
    private static final Log log = LogFactory.getLog(APIThrottlingServerRestartTestCase.class);
    private String apiThrottleAccessToken;

    @BeforeClass
    public void initialize(ITestContext ctx) throws Exception {
        super.init();
        apiThrottleAccessToken = (String) ctx.getAttribute("apiThrottleAccessToken");
    }

    @Test(groups = {"throttling"}, description = "API Throttling Test")
    public void testAPIThrottling_1() throws Exception {

        waitForAPIDeploymentSync(user.getUserName(), "APIThrottleAPI", "1.0.0",
                APIMIntegrationConstants.IS_API_EXISTS);

        String invokeURL = getAPIInvocationURLHttps("api_throttle");
        Map<String, String> requestHeaders = new HashMap<>();
        String tokenJti = TokenUtils.getJtiOfJwtToken(apiThrottleAccessToken);
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + tokenJti);
        log.info("=============================== Headers : " + requestHeaders);
        log.info("=============================== invokeURL : " + invokeURL);

        HttpResponse serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/1.0.0/test", requestHeaders);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        //verify throttling
        checkThrottling(invokeURL, requestHeaders);
    }

    private void checkThrottling(String invokeURL, Map<String, String> requestHeaders) {

        int count = 0;
        int limit = 4;
        int numberOfIterations = 4;
        for (; count < numberOfIterations; ++count) {
            try {
                log.info(" =================================== Number of time API Invoked : " + count);
                if (count == limit) {
                    Thread.sleep(10000);
                }
                HttpResponse serviceResponse = callAPI(invokeURL, requestHeaders);
                if (count == limit) {
                    Assert.assertEquals(serviceResponse.getResponseCode(), 429, "Response code is not as expected");
                } else {
                    Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
                }

            } catch (Exception ex) {
                log.error("Error occurred while calling API : " + ex);
                break;
            }
        }
    }

    private HttpResponse callAPI(String invokeURL, Map<String, String> requestHeaders) throws Exception {
        return HTTPSClientUtils.doGet(invokeURL + "/1.0.0/test", requestHeaders);

    }

}
