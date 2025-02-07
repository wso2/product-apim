/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.axiom.om.OMElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.monitor.utils.WireMonitorServer;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;

import java.io.File;
import java.util.HashMap;

/**
 * When we have an API with an in-sequence with messageType property
 * check whether the GET request behaves as expected.
 */
public class APIInvocationWithMessageTypeProperty extends APIMIntegrationBaseTest {

    private WireMonitorServer wireServer;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        wireServer = new WireMonitorServer(8991);
        AuthenticatorClient login = new AuthenticatorClient(gatewayContextMgt.getContextUrls().getBackEndUrl());
        String session = login.login(APIMIntegrationConstants.ADMIN_USERNAME, APIMIntegrationConstants.ADMIN_PASSWORD,
                APIMIntegrationConstants.LOCAL_HOST_NAME);
        // Upload the synapse
        String file = "artifacts" + File.separator + "AM" + File.separator + "synapseconfigs" + File.separator
                + "property" + File.separator + "dummy_api_with_in_sequence.xml";
        OMElement synapseConfig = APIMTestCaseUtils.loadResource(file);
        APIMTestCaseUtils.updateSynapseConfiguration(synapseConfig, gatewayContextMgt.getContextUrls().getBackEndUrl(),
                session);
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
    @Test(groups = "wso2.am", description = "Test for GET request for an API with in-sequence with messageType "
            + "property")
    public void testInovkeAPIWithMessageTypePropertyInSequence() throws Exception {
        wireServer.start();
        HttpResponse response = HttpRequestUtil
                .doGet(gatewayUrlsWrk.getWebAppURLNhttp() + "msgtypeproperty", new HashMap<String, String>());
        Assert.assertNotNull(response, "Error invoking API with in-sequence with messageType property");
        Assert.assertTrue(response.getResponseMessage().contains("Accepted"),
                "Error invoking API with in-sequence with messageType property");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

    }
}
