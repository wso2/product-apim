/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.tests.sample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.FileManipulator;
import org.wso2.carbon.utils.ServerConstants;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ErrorMessageTypeTestCase extends AMIntegrationBaseTest {

    private ServerConfigurationManager serverConfigurationManager;
    private static final Log log = LogFactory.getLog(ErrorMessageTypeTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        /*
          This test will check API Manager will return auth failures in JSON format
         */
        super.init();
        serverConfigurationManager = new ServerConfigurationManager(apimContext);
        String destinationPath = computeDestinationPathForDataSource("axis2.xml");
        String sourcePath = computeAxis2SourceResourcePath("axis2.xml");
        copyAxis2ConfigFile(sourcePath, destinationPath);
        loadAPIMConfigurationFromClasspath("artifacts" + File.separator + "AM"
                + File.separator + "synapseconfigs" + File.separator + "error" + File.separator + "handle"
                + File.separator + "error-handling-test-synapse.xml");
    }

    @Test(groups = {"wso2.am"}, description = "Error Message format test sample")
    public void errorMessageTypeTestCase() throws Exception {
        HttpResponse response = HttpRequestUtil.doGet(getGatewayServerURLHttp() + "stockquote/test/", null);
        assertEquals(response.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(), "Response code mismatch");
        //message contains json string or not
        assertTrue(response.getData().contains("{\"fault\":{"),"Did not receive Json error response");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanup();
        serverConfigurationManager.restoreToLastConfiguration();
    }

    private String computeDestinationPathForDataSource(String fileName) {
        String serverRoot = System.getProperty(ServerConstants.CARBON_HOME);
        String deploymentPath = serverRoot + File.separator + "repository" + File.separator + "conf" +
                File.separator + "axis2";
        File depFile = new File(deploymentPath);
        if (!depFile.exists() && !depFile.mkdir()) {
            log.error("Error while creating the deployment folder : "
                    + deploymentPath);
        }
        return deploymentPath + File.separator + fileName;
    }

    private String computeAxis2SourceResourcePath(String fileName) {

        return getAMResourceLocation().replace("//", "/")
                + File.separator + "configFiles" + File.separator + "error" + File.separator + fileName;
    }

    private void copyAxis2ConfigFile(String sourcePath, String destPath) {
        File sourceFile = new File(sourcePath);
        File destFile = new File(destPath);
        try {
            FileManipulator.copyFile(sourceFile, destFile);
        } catch (IOException e) {
            log.error("Error while copying the sample into Jaggery server", e);
        }
    }

}
