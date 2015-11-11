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

package org.wso2.am.integration.tests.rest;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;

import static org.testng.Assert.assertEquals;

/**
 * Related to Patch Automation  https://wso2.org/jira/browse/APIMANAGER-769
 * This test class test the Rest URI template patterns like uri-template="/view/*"
 */
public class URLMappingRESTTestCase extends APIMIntegrationBaseTest {

	private String gatewaySessionCookie;

    @Factory(dataProvider = "userModeDataProvider")
    public URLMappingRESTTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

	@BeforeClass(alwaysRun = true)
	public void setEnvironment() throws Exception {
        String synapseConfFile;
		super.init(userMode);

		gatewaySessionCookie = createSession(gatewayContextMgt);
        if (gatewayContextWrk.getContextTenant().getDomain().equals(FrameworkConstants.SUPER_TENANT_DOMAIN_NAME)) {
            synapseConfFile = "url-mapping-synapse.xml";
        } else {
            synapseConfFile = "url-mapping-synapse-tenant.xml";
        }

		loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM"
				+ File.separator + "synapseconfigs" + File.separator + "rest"
                + File.separator + synapseConfFile, gatewayContextMgt, gatewaySessionCookie);

        waitForAPIDeploymentSync(gatewayContextWrk.getContextTenant().getContextUser().getUserName(),
                                 "stockquote", "1.0.0",
                                 APIMIntegrationConstants.IS_API_EXISTS);
	}

	@Test(groups = { "wso2.am" },
	      description = "Sending a Message Via REST to test uri template fix")
	public void testRESTURITemplate() throws Exception {
		// Before apply this patch uri template not recognize localhost:8280/stockquote/test/
		// and localhost:8280/stockquote/test
		//maps to same resource. It will return correct response only if request hits localhost:8280/stockquote/test
		//after fixing issue both will work.

		HttpResponse response = HttpRequestUtil.sendGetRequest(getAPIInvocationURLHttp("stockquote/test/"), null);
		assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(), "Response code mismatch");
	}

	@AfterClass(alwaysRun = true)
	public void destroy() throws Exception {
        super.cleanUp();
	}

}

