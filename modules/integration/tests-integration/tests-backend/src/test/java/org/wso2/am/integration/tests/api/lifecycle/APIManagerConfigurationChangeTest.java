/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.api.lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;

/**
 * Change the api-manager.xml file before start the test suit and  restore it after  all test executions are finished.
 */
public class APIManagerConfigurationChangeTest extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(APIManagerConfigurationChangeTest.class);
    private ServerConfigurationManager serverManager;
    private static final String APIM_CONFIG_XML = "api-manager.xml";

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
    @BeforeTest(alwaysRun = true)
    public void startChangeAPIMConfigureXml() throws Exception {
        super.init();
        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
        String artifactsLocation = TestConfigurationProvider.getResourceLocation() +
                File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "configFiles" + File.separator + "lifecycletest" +
                File.separator;
        String apimConfigArtifactLocation = artifactsLocation + APIM_CONFIG_XML;
        String apimRepositoryConfigLocation = carbonHome + File.separator + "repository" +
                File.separator + "conf" + File.separator + APIM_CONFIG_XML;

        File sourceFile = new File(apimConfigArtifactLocation);
        File targetFile = new File(apimRepositoryConfigLocation);
        serverManager = new ServerConfigurationManager(gatewayContext);

        // apply configuration to  api-manager.xml
        serverManager.applyConfigurationWithoutRestart(sourceFile, targetFile, true);
        log.info("api-manager.xml configuration file copy from :" + apimConfigArtifactLocation +
                " to :" + apimRepositoryConfigLocation);

        serverManager.restartGracefully();

    }

    @AfterTest(alwaysRun = true)
    public void startRestoreAPIMConfigureXml() throws Exception {
        serverManager.restoreToLastConfiguration();
        log.info("Restore the api-manager.xml configuration file");

    }


}
