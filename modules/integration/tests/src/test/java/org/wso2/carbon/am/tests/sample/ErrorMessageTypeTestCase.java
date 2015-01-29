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

package org.wso2.carbon.am.tests.sample;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.am.tests.APIManagerIntegrationTest;
import org.wso2.carbon.am.tests.util.APIPublisherRestClient;
import org.wso2.carbon.am.tests.util.APIStoreRestClient;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.HttpRequestUtil;
import org.wso2.carbon.automation.core.utils.HttpResponse;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;
import org.wso2.carbon.utils.FileManipulator;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.io.IOException;

public class ErrorMessageTypeTestCase extends APIManagerIntegrationTest {
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private ServerConfigurationManager serverConfigurationManager;
    private String publisherURLHttp;
    private String storeURLHttp;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        /*
          This test will check API Manager will return auth failures in JSON format
         */
        super.init(0);
        serverConfigurationManager = new ServerConfigurationManager(amServer.getBackEndUrl());
        String destinationPath =computeDestinationPathForDataSource("axis2.xml");
        String sourcePath = computeAxis2SourceResourcePath("axis2.xml");
        copyAxis2ConfigFile(sourcePath, destinationPath);
        super.init(0);
        loadESBConfigurationFromClasspath("artifacts" + File.separator + "AM"
                + File.separator + "synapseconfigs" + File.separator + "error"+ File.separator + "handle"
                + File.separator + "error-handling-test-synapse.xml");


    }

    @Test(groups = {"wso2.am"}, description = "Error Message format test sample")
    public void errorMessageTypeTestCase() throws Exception {
        HttpResponse response = HttpRequestUtil.doGet(getApiInvocationURLHttp("stockquote") + "/test/", null);
        Assert.assertEquals(response.getResponseCode(), 403, "Response code mismatch");
        //message contains json string or not
        Assert.assertTrue(response.getData().contains("{\"fault\":{"));
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanup();
        serverConfigurationManager.restoreToLastConfiguration();
    }
    private String computeDestinationPathForDataSource(String fileName) {
        String serverRoot = System.getProperty(ServerConstants.CARBON_HOME);
        String deploymentPath = serverRoot + "/repository/conf/axis2";
        File depFile = new File(deploymentPath);
        if (!depFile.exists() && !depFile.mkdir()) {
            log.error("Error while creating the deployment folder : "
                    + deploymentPath);
        }
        return deploymentPath + File.separator + fileName;
    }

    private String computeAxis2SourceResourcePath(String fileName) {

        String sourcePath = ProductConstant.getResourceLocations(ProductConstant.AM_SERVER_NAME).replace("//","/")
                + File.separator + "configFiles/error/" + fileName;
        return sourcePath;
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
