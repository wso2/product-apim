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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.carbon.automation.extensions.jmeter.JMeterTest;
import org.wso2.carbon.automation.extensions.jmeter.JMeterTestManager;

import java.io.File;

public class JmeterLoginTestCase extends AMIntegrationBaseTest {

	@BeforeClass(alwaysRun = true)
	public void init() throws Exception {
		super.init();

	}

	@Test(groups = "wso2.am", description = "Login to api manager as user2")
	public void testListServices() throws Exception {
		JMeterTest script =
				new JMeterTest(new File(getAMResourceLocation() + File.separator + "scripts"
				                        + File.separator + "API_Manager_Login_Test.jmx"));

		JMeterTestManager manager = new JMeterTestManager();
		manager.runTest(script);
	}
}
