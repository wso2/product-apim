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
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.activation.DataHandler;
import java.io.File;
import java.net.URL;

public class ThrottlingTestCase extends APIManagerIntegrationTest {
	private APIPublisherRestClient apiPublisher;
	private APIStoreRestClient apiStore;
	private ServerConfigurationManager serverConfigurationManager;

	@BeforeClass(alwaysRun = true)
	public void init() throws Exception {
	    /*
        Before run this test we need to add throttling tiers xml file to registry and also we need to deploy API
        to gateway Server.
        Deploy API available in AM/synapseconfigs/throttling/throttling-api-synapse.xml
        Add throttling definition available in configFiles/throttling/throttle-policy.xml to /_system/governance/apimgt/applicationdata/test-tiers.xml
         */
		super.init();
		serverConfigurationManager = new ServerConfigurationManager(context);
		super.init();
		loadESBConfigurationFromClasspath(getAMResourceLocation()
		                                  + File.separator + "synapseconfigs" + File.separator +
		                                  "throttling"
		                                  + File.separator + "throttling-api-synapse.xml");
		//apiPublisher = new APIPublisherRestClient(getServerURLHttp());
		//apiStore = new APIStoreRestClient(getServerURLHttp());

	}

	@Test(groups = { "wso2.am" }, description = "Token API Test sample")
	public void throttlingTestCase() throws Exception {
		//APIProviderHostObject test=new APIProviderHostObject("admin");
		//add client IP to tiers xml
		ResourceAdminServiceClient resourceAdminServiceStub =
				new ResourceAdminServiceClient(contextUrls.getBackEndUrl(), sessionCookie);

		//resourceAdminServiceStub.deleteResource("/_system/config/proxy");
		resourceAdminServiceStub.addCollection("/_system/config/", "proxy", "",
		                                       "Contains test proxy tests files");

		Assert.assertTrue(resourceAdminServiceStub.addResource(
				"/_system/governance/apimgt/applicationdata/test-tiers.xml", "application/xml",
				"xml files",
				setEndpoints(new DataHandler(new URL("file:///" + getAMResourceLocation()
				                                     + File.separator + "configFiles/throttling/" +
				                                     "throttle-policy.xml")))
		)
				, "Adding Resource failed");
		Thread.sleep(2000);
		HttpResponse response = HttpRequestUtil
				.sendGetRequest(getApiInvocationURLHttp("stockquote") + "/test/", null);
		Assert.assertEquals(response.getResponseCode(), 200, "Response code mismatch");

		HttpResponse errorResponse =
				HttpRequestUtil.doGet(getApiInvocationURLHttp("stockquote") + "/test/", null);
		Assert.assertEquals(errorResponse.getResponseCode(), 503, "Response code mismatch");
		//assert response

	}

	@AfterClass(alwaysRun = true)
	public void destroy() throws Exception {
		super.cleanup();
	}
}
