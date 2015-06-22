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

package org.wso2.am.integration.tests.json;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
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
 * This is related to public jira - https://wso2.org/jira/browse/ESBJAVA-3380 &
 * https://wso2.org/jira/browse/APIMANAGER-3076
 * This class tests the conversion of json with special characters to xml.
 */
public class ESBJAVA3380TestCase extends APIMIntegrationBaseTest {

    private ServerConfigurationManager serverConfigurationManager;
    String gatewaySessionCookie;


    @Factory(dataProvider = "userModeDataProvider")
    public ESBJAVA3380TestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        serverConfigurationManager =
                new ServerConfigurationManager(new AutomationContext("APIM", "gateway",
                                                                     TestUserMode.SUPER_TENANT_ADMIN));
        super.init(userMode);
        /*
           * If test run in external distributed deployment you need to copy
           * following resources accordingly. configFiles/json_to_xml/axis2.xml
           * configFiles/json_to_xml/synapse.properties
           */

        serverConfigurationManager.applyConfigurationWithoutRestart(
                new File(getAMResourceLocation() + File.separator + "configFiles/json_to_xml/" + "axis2.xml"));

        serverConfigurationManager.applyConfiguration(
                new File(getAMResourceLocation() + File.separator + "configFiles/json_to_xml/" + "synapse.properties"));

        gatewaySessionCookie = createSession(gatewayContext);

        String apiMngrSynapseConfigPath = "/artifacts/AM/synapseconfigs/property/json_to_xml.xml";
        String relativeFilePath = apiMngrSynapseConfigPath.replaceAll(
                "[\\\\/]", File.separator);
        loadSynapseConfigurationFromClasspath(relativeFilePath, gatewayContext, gatewaySessionCookie);
    }

    @Test(groups = {"wso2.am"}, description = "Json to XML Test other")
    public void jsonToXmlTestCase() throws Exception {

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.clear();
        requestHeaders.put("Content-Type", "application/json");
        // Send the payload with special character ":"
        String payload = "{ \"http://purl.org/dc/elements/1.1/creator\" : \"url\"}";
        HttpResponse response = null;

        try {
            response = HttpRequestUtil.doPost(new URL(
                                                      gatewayUrls.getWebAppURLNhttp() + "Weather/1.0.0"), payload,
                                              requestHeaders);
        } catch (Exception e) {
            Assert.assertFalse(
                    e.getLocalizedMessage().contains("Connection error"),
                    "Problem in converting json to xml");
        }

        assert response != null;
        Assert.assertEquals(response.getResponseCode(), 404,
                            "Response code mismatched while Json to XML test case");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp(gatewayContext.getContextTenant().getTenantAdmin().getUserName(),
                      gatewayContext.getContextTenant().getContextUser().getPassword(),
                      storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());
        serverConfigurationManager.restoreToLastConfiguration();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

}
