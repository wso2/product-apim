/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.am.tests.header;

import org.apache.axiom.om.OMElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTest;
import org.wso2.am.integration.test.utils.WireMonitorServer;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;
import org.wso2.carbon.integration.common.admin.client.TenantManagementServiceClient;

import java.io.*;
import java.net.URL;

/**
 * Property Mediator FORCE_HTTP_CONTENT_LENGTH Property Test
 */

public class ContentLengthHeaderTestCase extends APIManagerIntegrationTest {
	public WireMonitorServer wireServer;
	private TenantManagementServiceClient tenantManagementServiceClient;
	private AutomationContext context;

	@BeforeClass(alwaysRun = true)
	public void setEnvironment() throws Exception {
		super.init();

		// todo set the correct user and create context
		//        int userId = 16;
		//        userInfo = UserListCsvReader.getUserInfo(userId);
		//        EnvironmentBuilder builder = new EnvironmentBuilder().am(userId);
		//        context = builder.build().getAm();

		// UserInfo userInfo = UserListCsvReader.getUserInfo(16);
		AuthenticatorClient loginUtil = new AuthenticatorClient(contextUrls.getBackEndUrl());

		String sessionCookieAdmin = loginUtil.login(
				context.getContextTenant().getContextUser().getUserName(),
				context.getContextTenant().getContextUser().getPassword(),
				contextUrls.getBackEndUrl());

		// https://localhost:9443/t/abc.com/services

		tenantManagementServiceClient =
				new TenantManagementServiceClient(contextUrls.getBackEndUrl(), sessionCookieAdmin);
		tenantManagementServiceClient.addTenant("abc.com", "abc123", "abc", "demo");

		String apiMngrSynapseConfigPath =
				"/artifacts/AM/synapseconfigs/property/FORCE_HTTP_CONTENT_LENGTH.xml";
		String relativeFilePath = apiMngrSynapseConfigPath.replaceAll("[\\\\/]", File.separator);
		OMElement apiMngrSynapseConfig = esbUtils.loadResource(relativeFilePath);

		esbUtils.updateESBConfiguration(setEndpoints(apiMngrSynapseConfig),
		                                contextUrls.getBackEndUrl(),
		                                sessionCookie);

		wireServer = new WireMonitorServer(8991);
	}

	@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
	@Test(groups = "wso2.am",
	      description = "Test for reading the Content-Length header in the request")
	public void testFORCE_HTTP_CONTENT_LENGTHPropertyTest() throws Exception {

		wireServer.start();

		//axis2Client.sendSimpleStockQuoteRequest("http://localhost:8280/t/abc.com/helloabc/1.0.0", null, "WSO2");
		FileInputStream fis = new FileInputStream(
				"/artifacts/AM/synapseconfigs/property/FORCE_HTTP_CONTENT_LENGTH.xml");
		InputStreamReader isr = new InputStreamReader(fis, "UTF8");
		Reader inputReader = new BufferedReader(isr);

		URL postEndpoint = new URL("http://localhost:8280/t/abc.com/stock/1.0.0");
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

