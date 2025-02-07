/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.monitor.utils.WireMonitorServer;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;

import java.io.File;
import java.io.IOException;

public class APIMANAGER3357ContentTypeTestCase extends APIMIntegrationBaseTest {
    public WireMonitorServer wireServer;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        wireServer = new WireMonitorServer(8991);

        AuthenticatorClient login = new AuthenticatorClient(gatewayContextMgt.getContextUrls().getBackEndUrl());
        String session = login.login("admin", "admin", "localhost");
        // Upload the synapse
        String file = "artifacts" + File.separator + "AM" + File.separator + "synapseconfigs" +
                      File.separator + "property" + File.separator +
                      "CONTENT_TYPE_TEST.xml";
        OMElement synapseConfig = APIMTestCaseUtils.loadResource(file);
        APIMTestCaseUtils.updateSynapseConfiguration(synapseConfig, gatewayContextMgt.getContextUrls().getBackEndUrl(),
                                                     session);
        Thread.sleep(5000);
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL})
    @org.testng.annotations.Test(groups = "wso2.am",
            description = "Test for reading the multipart/form-data Content-Type header in the request")
    public void testTRANSPORT_HEADERSPropertTest() throws Exception {

        wireServer.start();

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(gatewayUrlsWrk.getWebAppURLNhttp() + "ContentTypeAPI");

        String relativeFilePath = "/artifacts/AM/synapseconfigs/property/CONTENT_TYPE_TEST.xml";
        relativeFilePath = relativeFilePath.replaceAll("[\\\\/]", File.separator);
        String path = TestConfigurationProvider.getResourceLocation() + relativeFilePath;
        File file = new File(path);


        FileBody uploadFilePart = new FileBody(file);
        MultipartEntity reqEntity = new MultipartEntity();
        reqEntity.addPart("file", uploadFilePart);
        httppost.setEntity(reqEntity);

        try {
            httpclient.execute(httppost);
        } catch (IOException e) {
            //e.printStackTrace();
        }
        String wireResponse = wireServer.getCapturedMessage();

        Assert.assertTrue(wireResponse.contains("Content-Type: multipart/form-data"),
                          "Content-Type header have multipart/form-data value properly");

        // response should be something like multipart/form-data; boundary=9u5f_0bx6sx1lHerRtXmOkKAprCjG0ESSS
        // If it contains something like bellow, that is incorrect
        Assert.assertFalse(wireResponse.contains(
                                   "Content-Type: multipart/form-data; charset=UTF-8; boundary=MIMEBoundary_"),
                           "Content-Type header contains invalid charset and boundary values");

    }

    @AfterClass(alwaysRun = true)
    public void stop() throws Exception {
        cleanUp();
    }
}
