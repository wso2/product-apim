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


package org.wso2.am.integration.tests.sample;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.activation.DataHandler;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ThrottlingTestCase extends AMIntegrationBaseTest {

    @BeforeClass(alwaysRun = true)
	public void init() throws Exception {
	    /*
        Before run this test we need to add throttling tiers xml file to registry and also we need to deploy API
        to gateway Server.
        Deploy API available in AM/synapseconfigs/throttling/throttling-api-synapse.xml
        Add throttling definition available in configFiles/throttling/throttle-policy.xml to
        /_system/governance/apimgt/applicationdata/test-tiers.xml
         */
		super.init();
		loadAPIMConfigurationFromClasspath("artifacts" + File.separator + "AM"
				+ File.separator + "synapseconfigs" + File.separator +
				"throttling"
				+ File.separator + "throttling-api-synapse.xml");

	}

	@Test(groups = { "wso2.am" }, description = "Token API Test sample")
	public void throttlingTestCase() throws Exception {
		//APIProviderHostObject test=new APIProviderHostObject("admin");
		//add client IP to tiers xml
		ResourceAdminServiceClient resourceAdminServiceStub =
				new ResourceAdminServiceClient(contextUrls.getBackEndUrl(), sessionCookie);

		resourceAdminServiceStub.addCollection("/_system/config/", "proxy", "",
		                                       "Contains test proxy tests files");

		assertTrue(resourceAdminServiceStub.addResource(
                "/_system/governance/apimgt/applicationdata/test-tiers.xml", "application/xml",
                "xml files",
                setEndpoints(new DataHandler(new URL("file:///" + getAMResourceLocation()
                        + File.separator + "configFiles/throttling/" +
                        "throttle-policy.xml")))
        )
                , "Adding Resource failed");
		Thread.sleep(2000);
		HttpResponse response = HttpRequestUtil
				.sendGetRequest(getGatewayServerURLHttp()+"/stockquote" + "/test/", null);
		assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(), "Response code mismatch " +
                "did not receive 200");

	}

	@AfterClass(alwaysRun = true)
	public void destroy() throws Exception {
		super.cleanup();
	}
}
