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


package org.wso2.am.integration.tests.throttling;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;

import javax.activation.DataHandler;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;

import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class ThrottlingTestCase extends APIMIntegrationBaseTest {

    private String gatewaySessionCookie;

    @Factory(dataProvider = "userModeDataProvider")
    public ThrottlingTestCase(TestUserMode userMode) {
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
        /*
        Before run this test we need to add throttling tiers xml file to registry and also we need to deploy API
        to gateway Server.
        Deploy API available in AM/synapseconfigs/throttling/throttling-api-synapse.xml
        Add throttling definition available in configFiles/throttling/throttle-policy.xml to
        /_system/governance/apimgt/applicationdata/test-tiers.xml
         */
        super.init(userMode);
        gatewaySessionCookie = createSession(gatewayContextMgt);

        String throttlingSynapseConfFile;
        if (gatewayContextMgt.getContextTenant().getDomain().equals(FrameworkConstants.SUPER_TENANT_DOMAIN_NAME)) {
            throttlingSynapseConfFile = "throttling-api-synapse.xml";
        } else {
            throttlingSynapseConfFile = "throttling-api-synapse-tenant.xml";
        }

        loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM" + File.separator +
                                              "synapseconfigs" + File.separator + "throttling" + File.separator +
                                              throttlingSynapseConfFile, gatewayContextMgt, gatewaySessionCookie);

    }

    @Test(groups = {"wso2.am"}, description = "Token API Test other")
    public void throttlingTestCase() throws Exception {
        //APIProviderHostObject test=new APIProviderHostObject("admin");
        //add client IP to tiers xml
        ResourceAdminServiceClient resourceAdminServiceStub =
                new ResourceAdminServiceClient(publisherContext.getContextUrls().getBackEndUrl(), gatewaySessionCookie);

        resourceAdminServiceStub.addCollection("/_system/config/", "proxy", "", "Contains test proxy tests files");

        assertTrue(resourceAdminServiceStub.addResource(
                "/_system/governance/apimgt/applicationdata/test-tiers.xml", "application/xml",
                "xml files", new DataHandler(new URL("file:///" + getAMResourceLocation() + File.separator +
                                                     "configFiles/throttling/" + "throttle-policy.xml"))
        )
                , "Adding Resource failed");
        Thread.sleep(2000);

        String gatewayUrl = getAPIInvocationURLHttp("stockquote/test/");

        int responseCode = HttpRequestUtil.sendGetRequest(gatewayUrl, null).getResponseCode();
        assertTrue(validateStatusCode(responseCode), "Response code mismatch. Received "+responseCode);

    }

    private boolean validateStatusCode(int responseCode) {
        if (responseCode == Response.Status.OK.getStatusCode() || responseCode == Response.Status.ACCEPTED
                .getStatusCode()) {
            return true;
        }
        return false;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }

}