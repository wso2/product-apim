/*
 *
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.am.integration.tests.throttling;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;

import static org.testng.Assert.assertTrue;

public class AdvancedThrottlingConfig extends APIMIntegrationBaseTest {
    private ServerConfigurationManager serverConfigurationManager;
    private AutomationContext superTenantKeyManagerContext;

    @BeforeTest(alwaysRun = true)
    public void disableAdvancedThrottling() throws Exception {
        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);

        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "apiManagerXmlWithoutAdvancedThrottling" +
                File.separator + "api-manager.xml"));
        serverConfigurationManager.copyToComponentLib(new File(getAMResourceLocation() + File.separator +
                "configFiles" + File.separator + "APIM5898" + File.separator + "subs-workflow-1.0.0.jar"));

        serverConfigurationManager.restartGracefully();

        deployWebApp();
    }

    @AfterTest(alwaysRun = true)
    public void enableAdvancedThrottling() throws Exception {
        serverConfigurationManager.removeFromComponentLib("subs-workflow-1.0.0.jar");
        serverConfigurationManager.restoreToLastConfiguration(false);
    }

    private void deployWebApp() throws Exception {
        super.init();
        String fileFormat = ".war";
        String webApp = "jaxrs_basic";
        String path = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" + File.separator;

        String sourcePath = path + webApp + fileFormat;

        String sessionId = createSession(gatewayContextWrk);
        WebAppAdminClient webAppAdminClient = new WebAppAdminClient(gatewayContextWrk.getContextUrls().
                getBackEndUrl(), sessionId);
        webAppAdminClient.uploadWarFile(sourcePath);
        boolean isWebAppDeployed = WebAppDeploymentUtil
                .isWebApplicationDeployed(gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId, webApp);
        assertTrue(isWebAppDeployed, "Web APP is not deployed: " + webApp);
    }
}
