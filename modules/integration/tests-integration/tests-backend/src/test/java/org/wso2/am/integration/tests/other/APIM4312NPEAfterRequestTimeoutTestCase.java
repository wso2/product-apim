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
 */
package org.wso2.am.integration.tests.other;

import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.nio.file.Paths;

/**
 * This test is for APIMANAGER-4312 where a Null Pointer Exception occurred
 * with response submit failures while doing a load test.
 * Root cause has been trying to submit a request to a closed connection so it has been emulated by
 * having a Script mediator calling Thread.sleep before the Send mediator of out sequence of the API
 * in the synapse configuration.
 */

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE }) 
public class APIM4312NPEAfterRequestTimeoutTestCase extends APIMIntegrationBaseTest {
    private static final Log log = LogFactory.getLog(APIM4312NPEAfterRequestTimeoutTestCase.class);

    private String gatewaySessionCookie;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM4312NPEAfterRequestTimeoutTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider(name = "userModeDataProvider")
    public static Object[][] userModeDataProvider() {
        return new Object[][] {
                new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        gatewaySessionCookie = createSession(gatewayContextMgt);
        if (userMode != TestUserMode.SUPER_TENANT_ADMIN) {
            String apiMngrSynapseConfigPath =
                    "artifacts" + File.separator+ "AM" + File.separator + "synapseconfigs" +
                            File.separator + "rest" + File.separator + "dummy_api_APIMANAGER-4312_tenant.xml";
            loadSynapseConfigurationFromClasspath(apiMngrSynapseConfigPath, gatewayContextMgt, gatewaySessionCookie);
        }
    }

    @Test(groups = { "noRestart" },
          description = "Test for NPE after timeout",
          expectedExceptions = NoHttpResponseException.class)
    public void nullPointerAfterTimeoutTest() throws Exception {
        try {
            HttpClient httpclient = new DefaultHttpClient();

            String apiInvocationURL = getAPIInvocationURLHttp("pizzashack/1.0.0");

            HttpUriRequest get = new HttpGet(apiInvocationURL + "/menu");
            //There is a Script mediator executing Thread.sleep(of 180000 ms) before the send mediator in the
            //out sequence of the API. And the socket time out of the passthru-http.properties is
            //set to 60000 ms. The test is to verify a Null Pointer Exception doesn't occur in this scenario
            httpclient.execute(get);
        } catch (NullPointerException e) {
            Assert.fail("Null pointer exception shouldn't have occurred after request time out");
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }
}
