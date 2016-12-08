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
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.am.integration.tests.header;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.monitor.utils.WireMonitorServer;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;

import java.io.File;
import java.io.IOException;

/**
 * When we set "http.headers.preserve=Content-Type" in "passthru-http.properties" file,
 * the charset part of the content type should be preserved for GET requests
 * to address Endpoints with soap11 format.
 */
public class ESBJAVA3447PreserveCharsetInContentTypeTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(ESBJAVA3447PreserveCharsetInContentTypeTestCase.class);

    private WireMonitorServer wireServer;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();

        wireServer = new WireMonitorServer(8992);

        AuthenticatorClient login = new AuthenticatorClient(gatewayContextMgt.getContextUrls().getBackEndUrl());
        String session = login.login("admin", "admin", "localhost");
        // Upload the synapse
        String file = "artifacts" + File.separator + "AM" + File.separator + "synapseconfigs" + File.separator
                + "property" + File.separator + "CONTENT_TYPE_TEST.xml";
        OMElement synapseConfig = APIMTestCaseUtils.loadResource(file);
        APIMTestCaseUtils.updateSynapseConfiguration(synapseConfig, gatewayContextMgt.getContextUrls().getBackEndUrl(),
                session);
        Thread.sleep(5000);
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
    @org.testng.annotations.Test(groups = "wso2.am",
            description = "Test for preserving Charset in Content-Type header in the request")
    public void testPreserveCharsetInContentTypeHeader() throws Exception {

        wireServer.start();
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(gatewayUrlsWrk.getWebAppURLNhttp() + "charset");
        httpget.setHeader("Content-type", "text/xml; charset=UTF-8");
        try {
            httpclient.execute(httpget);
        } catch (IOException e) {
            log.error("Error in executing GET request for CharsetAPI",e);
        }

        String[] wireResponse = wireServer.getCapturedMessage().split(System.lineSeparator());
        String charSet = "";
        for (String line : wireResponse) {
            if (line.contains("Content-Type")) {
                charSet = line;
                break;
            }
        }
        Assert.assertTrue(charSet.contains("text/xml; charset=UTF-8"), "Content-Type header has dropped Charset");

    }
    @AfterClass(alwaysRun = true)
    public void stop() throws Exception {
        cleanUp();
    }
}