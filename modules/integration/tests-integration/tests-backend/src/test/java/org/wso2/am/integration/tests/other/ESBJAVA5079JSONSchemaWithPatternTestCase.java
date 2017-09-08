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

package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * This is related to public jira - https://wso2.org/jira/browse/ESBJAVA-5079 &
 * This class tests the conversion of json with special characters to xml.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class ESBJAVA5079JSONSchemaWithPatternTestCase extends APIMIntegrationBaseTest {

    private ServerConfigurationManager serverConfigurationManager;
    private String gatewaySessionCookie;
    private static final Log log = LogFactory.getLog(ESBJAVA5079JSONSchemaWithPatternTestCase.class);

    @Factory(dataProvider = "userModeDataProvider")
    public ESBJAVA5079JSONSchemaWithPatternTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider(name = "userModeDataProvider")
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        serverConfigurationManager =
                new ServerConfigurationManager(new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME
                        , APIMIntegrationConstants.AM_GATEWAY_WRK_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN));

        /*
         * If test run in external distributed deployment you need to copy
         * following resources accordingly. configFiles/ESBJAVA5079/axis2.xml
        */
        serverConfigurationManager.applyConfigurationWithoutRestart(
                new File(getAMResourceLocation() + File.separator + "configFiles/ESBJAVA5079/" + "axis2.xml"));
        super.init(userMode);
        gatewaySessionCookie = createSession(gatewayContextMgt);
        String apiMngrSynapseConfigPath = "/artifacts/AM/synapseconfigs/rest/dummy_api_ESBJAVA5079.xml";
        String relativeFilePath = apiMngrSynapseConfigPath.replaceAll(
                "[\\\\/]", File.separator);
        loadSynapseConfigurationFromClasspath(relativeFilePath, gatewayContextMgt, gatewaySessionCookie);
        serverConfigurationManager.restartGracefully();
    }

    @Test(groups = {"wso2.am"}, description = "Json schema validation with pattern")
    public void jsonSchemaValidationWithPattern() throws Exception {

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.clear();
        requestHeaders.put("Content-Type", "application/json");
        // Send the payload with special character ":"
        String payload = "{\n" +
                "  \"getQuote\": {\n" +
                "    \"request\": {\n" +
                "      \"symbol\": \"WSO2\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        HttpResponse response = null;
        response = HttpRequestUtil.doPost(new URL(getAPIInvocationURLHttp("jsonAPI/jsonapi/1.0.0")), payload,
                requestHeaders);
        assert response != null;
        Assert.assertEquals(response.getResponseCode(), 200,
                "Response code mismatched while doing json schema validation with pattern");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
        serverConfigurationManager.restoreToLastConfiguration();
    }

}
