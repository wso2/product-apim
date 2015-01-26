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
package org.wso2.carbon.am.jmeter;

import org.apache.axis2.AxisFault;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTest;
import org.wso2.am.integration.test.utils.WireMonitorServer;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.extensions.jmeter.JMeterTest;
import org.wso2.carbon.automation.extensions.jmeter.JMeterTestManager;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;

public class PassthroughTestCase extends APIManagerIntegrationTest {

	private ServerConfigurationManager serverConfigurationManager;

	@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
	@BeforeClass(alwaysRun = true)
	public void init() throws Exception, AxisFault {
		super.init();
	}

	@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
	@Test(groups = "wso2.am",
	      description = "Covers tenant creation, role creation, API creation, publish api," +
	                    "get default app id, subscribe users to default app, invoke api")
	public void testListServices() throws Exception {
		JMeterTest script =
				new JMeterTest(new File(getAMResourceLocation() + File.separator + "scripts"
				                        + File.separator +
				                        "API_Manager_functionality_and_loadTest_new_tenant.jmx"));

		JMeterTestManager manager = new JMeterTestManager();
		manager.runTest(script);
	}

	@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
	@Test(groups = "wso2.am", description =
			"creates and API, subscribe to it and send GET and DELETE requests without " +
			"Content-Type header and checks for if the Content-Type header is forcefully added by APIM, " +
			"which should not happen")
	public void JIRA_APIMANAGER_1397_testContentTypeHeaderInsertionCheck() throws Exception {
		JMeterTest publishScript =
				new JMeterTest(new File(getAMResourceLocation() + File.separator + "scripts"
				                        + File.separator +
				                        "content_type_check_publish_and_subscribe_script.jmx"));

		JMeterTestManager manager = new JMeterTestManager();
		manager.runTest(publishScript);

		WireMonitorServer wireMonitorServer = new WireMonitorServer(6789);

		wireMonitorServer.start();

		Thread.sleep(1000);

		JMeterTest scriptGET =
				new JMeterTest(new File(getAMResourceLocation() + File.separator + "scripts"
				                        + File.separator +
				                        "content_type_check_for_GET_script.jmx"));

		manager.runTest(scriptGET);

		while (true) {
			String reply = wireMonitorServer.getCapturedMessage();
			if (reply.length() > 1) {
				if (reply.contains("ThisParamIsRequiredForTest_GET")) {
					/**
					 * Assert for the Content-Type header
					 */
					Assert.assertTrue(!reply.contains("Content-Type"),
					                  "Content-Type header has been added to GET request forcefully!!");
				}
			}
			break;
		}

		wireMonitorServer = new WireMonitorServer(6789);

		wireMonitorServer.start();

		Thread.sleep(1000);

		JMeterTest scriptDELETE =
				new JMeterTest(new File(getAMResourceLocation() + File.separator + "scripts"
				                        + File.separator +
				                        "content_type_check_for_DELETE_script.jmx"));

		manager.runTest(scriptDELETE);

		while (true) {
			String reply = wireMonitorServer.getCapturedMessage();
			if (reply.length() > 1) {
				if (reply.contains("ThisParamIsRequiredForTest_DELETE")) {
					/**
					 * Assert for the Content-Type header
					 */
					Assert.assertTrue(!reply.contains("Content-Type"),
					                  "Content-Type header has been added to DELETE request forcefully!!");
				}
			}
			break;
		}

	}
}
