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
package org.wso2.jmeter.tests;

import org.apache.axis2.AxisFault;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.extensions.jmeter.JMeterTest;
import org.wso2.carbon.automation.extensions.jmeter.JMeterTestManager;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.io.IOException;

public class JmeterTestCases extends APIManagerIntegrationTest {

	private ServerConfigurationManager serverConfigurationManager;

	@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
	@BeforeClass(alwaysRun = true)
	public void testChangeTransportMechanism() throws Exception, AxisFault {
		super.init(TestUserMode.SUPER_TENANT_USER);
		serverConfigurationManager = new ServerConfigurationManager(context);
		String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);

		File axis2xmlFile =
				new File(carbonHome + File.separator + "repository" + File.separator + "conf"
				         + File.separator + "axis2" + File.separator + "axis2.xml");

		File sourceAxis2xmlFile =
				new File(carbonHome + File.separator + "repository" + File.separator
				         + "conf" + File.separator + "axis2" + File.separator + "axis2.xml_NHTTP");

		if (!axis2xmlFile.exists() || !sourceAxis2xmlFile.exists()) {
			throw new IOException("File not found in given location");
		}

		serverConfigurationManager.applyConfiguration(sourceAxis2xmlFile, axis2xmlFile);
	}

	@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
	@Test(groups = "wso2.am",
	      description = "Covers tenant creation, role creation, API creation, publish api," +
	                    "get default app id, subscribe users to default app, invoke api")
	public void testListServices() throws Exception {
		JMeterTest script =
				new JMeterTest(new File(getAMResourceLocation() + File.separator + "scripts"
				                        + File.separator +
				                        "API_Manager_functionality_and_loadTest.jmx"));

		JMeterTestManager manager = new JMeterTestManager();
		manager.runTest(script);
	}

	@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
	@AfterClass(alwaysRun = true)
	public void testCleanup() throws Exception {
		serverConfigurationManager.restoreToLastConfiguration();
		serverConfigurationManager = null;
	}
}
