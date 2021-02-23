/*
*Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.tests.header;

import org.apache.axiom.om.OMElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.tests.header.util.SimpleSocketServer;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;

import java.io.File;
import java.util.HashMap;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class APIMANAGER3614DuplicateTransferEncodingHeaderTestCase extends APIMIntegrationBaseTest {

    private SimpleSocketServer simpleSocketServer;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();

        AuthenticatorClient login = new AuthenticatorClient(gatewayContextWrk.getContextUrls().getBackEndUrl());
        String session = login.login("admin", "admin", "localhost");
        // Upload the synapse
        String file = "artifacts" + File.separator + "AM" + File.separator + "synapseconfigs" +
                      File.separator + "property" + File.separator +
                      "duplicate_transfer_encoding.xml";
        OMElement synapseConfig = apimTestCaseUtils.loadResource(file);
        apimTestCaseUtils.updateSynapseConfiguration(synapseConfig, gatewayContextWrk.getContextUrls().getBackEndUrl(),
                                                     session);

        int port = 9785;
        String expectedResponse = "HTTP/1.0 200 OK\r\nServer: testServer\r\n" +
                                  "Content-Type: text/html\r\n" +
                                  "Transfer-Encoding: chunked\r\n" +
                                  "Transfer-Encoding: chunked\r\n" +
                                  "\r\n" + "<HTML>\n" + "<!DOCTYPE HTML PUBLIC " +
                                  "\"-//W3C//DTD HTML 4.0 Transitional//EN\">\n" +
                                  "<HEAD>\n" + " <TITLE>Test Server Results</TITLE>\n" +
                                  "</HEAD>\n" + "\n" + "<BODY BGCOLOR=\"#FDF5E6\">\n" +
                                  "<H1 ALIGN=\"CENTER\"> Results</H1>\n" +
                                  "Here is the request line and request headers\n" +
                                  "sent by your browser:\n" + "<PRE>\r\n0\r\n";
        simpleSocketServer = new SimpleSocketServer(port, expectedResponse);
        simpleSocketServer.start();
        Thread.sleep(10000);
    }

    @Test(groups = "wso2.am", description = "Test for reading the duplicate transfer-encoding header in the response")
    public void testDuplicateTransferEncodingPropertyTest() throws Exception {

        String endPoint = getAPIInvocationURLHttp("helloService");

        HttpResponse httpResponse = HttpRequestUtil.doGet(endPoint, new HashMap<String, String>());
        Assert.assertNotNull(httpResponse, "Response should be available");
        Assert.assertEquals(httpResponse.getResponseCode(), 200, "Response should be success");
    }

    @AfterClass(alwaysRun = true)
    public void stop() throws Exception {
        simpleSocketServer.shutdown();
        super.cleanUp();
    }
}
