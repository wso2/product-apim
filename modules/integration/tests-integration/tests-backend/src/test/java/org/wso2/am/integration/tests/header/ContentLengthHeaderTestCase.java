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

package org.wso2.am.integration.tests.header;

import org.apache.axiom.om.OMElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.wso2.am.admin.clients.common.AuthenticatorClient;
import org.wso2.am.admin.clients.common.TenantManagementServiceClient;
import org.wso2.am.integration.test.utils.monitor.utils.WireMonitorServer;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;

import java.io.*;
import java.net.URL;

/**
 * Property Mediator FORCE_HTTP_CONTENT_LENGTH Property Test
 * https://wso2.org/jira/browse/ESBJAVA-2686
 */

public class ContentLengthHeaderTestCase extends AMIntegrationBaseTest {

	public WireMonitorServer wireServer;

	@BeforeClass(alwaysRun = true)
	public void setEnvironment() throws Exception {
		super.init();

		// Create tenant
		TenantManagementServiceClient tenantManagementServiceClient =
				new TenantManagementServiceClient(contextUrls.getBackEndUrl(), sessionCookie);
		tenantManagementServiceClient.addTenant("abc.com", "abc123", "abc", "demo");

		// Upload the synapse
		String file = "artifacts" + File.separator + "AM" + File.separator + "synapseconfigs" +
		              File.separator + "property" + File.separator +
		              "FORCE_HTTP_CONTENT_LENGTH.xml";

		OMElement synapseConfig = apimTestCaseUtils.loadResource(file);

		AuthenticatorClient login = new AuthenticatorClient(contextUrls.getBackEndUrl());
		String session = login.login("abc@abc.com", "abc123", "localhost");

		apimTestCaseUtils.updateAPIMConfiguration(setEndpoints(synapseConfig), contextUrls.getBackEndUrl(),
				session);
		Thread.sleep(5000);

		// Start wireserver
		wireServer = new WireMonitorServer(8991);
	}

	@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
	@org.testng.annotations.Test(groups = "wso2.am",
	                             description = "Test for reading the Content-Length header in the request")
	public void testFORCE_HTTP_CONTENT_LENGTHPropertyTest() throws Exception {

		wireServer.start();

		FileInputStream fis = new FileInputStream(
				getAMResourceLocation() + File.separator + "synapseconfigs" + File.separator +
				"property" + File.separator + "placeOrder.xml"
		);
		InputStreamReader isr = new InputStreamReader(fis, "UTF8");
		Reader inputReader = new BufferedReader(isr);

		URL postEndpoint = new URL(getGatewayServerURLHttp()+"/t/abc.com/stock/1.0.0");
		//URL postEndpoint = new URL("http://localhost:8280/t/abc.com/helloabc/1.0.0");

		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("foo.out")));

		HttpRequestUtil.sendPostRequest(inputReader, postEndpoint, out);

		String response = wireServer.getCapturedMessage();
		Assert.assertTrue(response.contains("Content-Length"),
		                  "Content-Length not found in out going message");

	}

	@AfterClass(alwaysRun = true)
	public void stop() throws Exception {
		cleanup();
	}
}

