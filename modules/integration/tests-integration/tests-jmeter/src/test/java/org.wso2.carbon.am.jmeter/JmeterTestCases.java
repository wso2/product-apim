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

package org.wso2.carbon.am.jmeter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.carbon.automation.extensions.jmeter.JMeterTest;
import org.wso2.carbon.automation.extensions.jmeter.JMeterTestManager;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.io.IOException;

public class JmeterTestCases extends AMIntegrationBaseTest{

    private ServerConfigurationManager serverConfigurationManager;

    protected Log log = LogFactory.getLog(getClass());

    @BeforeClass(alwaysRun = true)
    public void testChangeTransportMechanism() throws Exception {

        init();
        serverConfigurationManager = new ServerConfigurationManager(apimContext);
        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);

        File axis2xmlFile = new File(carbonHome + File.separator + "repository" + File.separator + "conf"
                + File.separator + "axis2" + File.separator + "axis2.xml");

        File sourceAxis2xmlFile = new File(TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts"
                + File.separator + "AM" + File.separator + "axis2" + File.separator + "axis2.xml_NHTTP");

        if (!axis2xmlFile.exists() || !sourceAxis2xmlFile.exists()) {
            throw new IOException("File not found in given location");
        }

        serverConfigurationManager.applyConfiguration(sourceAxis2xmlFile, axis2xmlFile);
    }

    @Test(groups = "wso2.am", description = "Covers tenant creation, role creation, API creation, publish api," +
            "get default app id, subscribe users to default app, invoke api")
    public void testListServices() throws Exception {
        JMeterTest script =
                new JMeterTest(new File(TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts"
                        + File.separator + "AM" + File.separator + "scripts"
                        + File.separator + "API_Manager_functionality_and_loadTest.jmx"));

        JMeterTestManager manager = new JMeterTestManager();
        manager.runTest(script);
    }

    @AfterClass(alwaysRun = true)
    public void testCleanup() throws Exception {
        serverConfigurationManager.restoreToLastConfiguration();
        serverConfigurationManager = null;
    }
}
