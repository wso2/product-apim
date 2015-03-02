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

package org.wso2.jmeter.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.automation.tools.jmeter.JMeterTest;
import org.wso2.automation.tools.jmeter.JMeterTestManager;
import org.wso2.carbon.am.tests.APIManagerIntegrationTest;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;

import java.io.File;

public class APIMANAGER3195JWTWithWSClientTestCase extends APIManagerIntegrationTest {
    private ServerConfigurationManager serverConfigurationManager;
    private Log log = LogFactory.getLog(getClass());

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init(0);
        if (isBuilderEnabled()) {
            String apiManagerXml = ProductConstant.getResourceLocations(ProductConstant.AM_SERVER_NAME) + File
                    .separator + "configFiles/jwt_wsclient_config/api-manager.xml";

            serverConfigurationManager = new ServerConfigurationManager(amServer.getBackEndUrl());
            serverConfigurationManager.applyConfiguration(new File(apiManagerXml));
        }

        super.init(0);
    }

    @Test(groups = {"wso2.am"}, description = "Create APIs and subscribe")
    public void createAndSubscribeForAPI() throws Exception {
        JMeterTest script =
                new JMeterTest(new File(ProductConstant.SYSTEM_TEST_RESOURCE_LOCATION + File.separator + "artifacts"
                                        + File.separator + "AM" + File.separator + "scripts"
                                        + File.separator + "APICreateSubscribeInvoke.jmx"));

        JMeterTestManager manager = new JMeterTestManager();
        manager.runTest(script);

        log.info("JWTWithWSClientTestCase completed successfully");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (serverConfigurationManager != null) {
            serverConfigurationManager.restoreToLastConfiguration();
        }
        super.cleanup();
    }
}
