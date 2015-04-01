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

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;

public class APIMANAGER2611EndpointValidationTestCase extends AMIntegrationBaseTest {
	@BeforeClass(alwaysRun = true)
	public void init() throws Exception {
		super.init();
	}

	@Test(groups = { "wso2.am" }, description = "Validate endpoint with Http Head not support End point")
	public void checkEndpointValidation() throws Exception {
		APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
		apiPublisherRestClient.login("admin", "admin");
		org.wso2.carbon.automation.test.utils.http.client.HttpResponse
				response = apiPublisherRestClient.checkValidEndpoint("http", "http://localhost:9763/oauth2/token");
		int statusCode =response.getResponseCode();
		if (statusCode == 200) {
			String responseString = response.getData();
			Assert.assertEquals(responseString.contains("success"), true);
		} else {
			Assert.assertTrue(false,
			                  "Endpoint Validation Fail due to endpoint validation endpoint didn't work" + statusCode);
		}

	}

	@AfterClass(alwaysRun = true)
	public void destroy() throws Exception {
		super.cleanup();
	}
}
