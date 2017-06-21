/*
*Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import java.io.File;

import org.apache.axiom.om.OMElement;
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

/**
 * This test case is written to track the issue reported in
 * https://wso2.org/jira/browse/ESBJAVA-5121.This checks whether the order of
 * the auth headers is correct before sending the request to the endpoint
 */

public class ESBJAVA5121CheckAuthHeaderOrderTestCase extends APIMIntegrationBaseTest {

	public WireMonitorServer wireServer;

	@BeforeClass(alwaysRun = true)
	public void setEnvironment() throws Exception {
		super.init();
		wireServer = new WireMonitorServer(8991);

		AuthenticatorClient login = new AuthenticatorClient(gatewayContextMgt.getContextUrls().getBackEndUrl());
		String session = login.login("admin", "admin", "localhost");
		// Upload the synapse
		String file = "artifacts" + File.separator + "AM" + File.separator + "synapseconfigs" + File.separator
				+ "property" + File.separator + "auth_headers.xml";
		OMElement synapseConfig = APIMTestCaseUtils.loadResource(file);
		APIMTestCaseUtils.updateSynapseConfiguration(synapseConfig, gatewayContextMgt.getContextUrls().getBackEndUrl(),
				session);
		Thread.sleep(5000);
	}

	@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
	@org.testng.annotations.Test(groups = "wso2.am", description = "Test for checking the order of the auth headers before sending the request to the backend")
	public void testAuthHeaderOrder() throws Exception {

		wireServer.start();

		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(gatewayUrlsWrk.getWebAppURLNhttp() + "check/");

		httpGet.addHeader("WWW-Authenticate", "NTLM");
		httpGet.addHeader("WWW-Authenticate", "Basic realm=\"BasicSecurityFilterProvider\"");
		httpGet.addHeader("WWW-Authenticate", "ANTLM3");

		httpClient.execute(httpGet);

		String response = wireServer.getCapturedMessage();

		Assert.assertNotNull(response);
		Assert.assertTrue(response.contains(
				"WWW-Authenticate: NTLM\r\nWWW-Authenticate: Basic realm=\"BasicSecurityFilterProvider\"\r\nWWW-Authenticate: ANTLM3"));
	}

	@AfterClass(alwaysRun = true)
	public void stop() throws Exception {
		cleanUp();
	}

}
