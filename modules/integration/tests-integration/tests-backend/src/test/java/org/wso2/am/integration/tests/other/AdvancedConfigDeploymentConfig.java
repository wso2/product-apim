/*
 *
 *   Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.other;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.io.FileInputStream;

public class AdvancedConfigDeploymentConfig extends APIMIntegrationBaseTest {
    private static final Log log = LogFactory.getLog(AdvancedConfigDeploymentConfig.class);
    private final String TENANT_CONFIG_LOCATION = "/_system/config/apimgt/applicationdata/tenant-conf.json";
    private static final String ADAPTER_CONFIG_XML = "output-event-adapters.xml";
    private ServerConfigurationManager serverConfigurationManager;
    private AutomationContext superTenantKeyManagerContext;
    private ResourceAdminServiceClient resourceAdminServiceClient;

    @BeforeTest(alwaysRun = true)
    public void startServerWithMultipleConfigs() throws Exception {
        super.init();
        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);

        resourceAdminServiceClient =
                new ResourceAdminServiceClient(gatewayContextMgt.getContextUrls().getBackEndUrl(),
                        createSession(gatewayContextMgt));

        //Configurations for Notification Test
        String tenantConfSrcLocation = IOUtils.toString(new FileInputStream(
                getAMResourceLocation() + File.separator + "configFiles"
                        + File.separator + "notification" + File.separator + "tenant-conf.json"));

        resourceAdminServiceClient.updateTextContent(TENANT_CONFIG_LOCATION, tenantConfSrcLocation);

        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
        String artifactsLocation = TestConfigurationProvider.getResourceLocation() +
                File.separator + "artifacts" + File.separator + "AM" + File.separator +
                "configFiles" + File.separator + "notification" + File.separator;

        String apimConfigArtifactLocation = artifactsLocation + ADAPTER_CONFIG_XML;
        String apimRepositoryConfigLocation = carbonHome + File.separator + "repository" +
                File.separator + "conf" + File.separator + ADAPTER_CONFIG_XML;

        File apimConfSourceFile = new File(apimConfigArtifactLocation);
        File apimConfTargetFile = new File(apimRepositoryConfigLocation);

        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);

        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "common" +
                File.separator + "api-manager.xml"));
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "common" +
                File.separator + "axis2.xml"));
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "common" +
                File.separator + "passthru-http.properties"));
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "common" +
                File.separator + "synapse.properties"));

        serverConfigurationManager.applyConfigurationWithoutRestart(apimConfSourceFile, apimConfTargetFile, true);

        serverConfigurationManager.restartGracefully();
    }

    @AfterTest(alwaysRun = true)
    public void restoreConfigs() throws Exception {
        String tenantConfSrcLocation = IOUtils.toString(new FileInputStream(
                getAMResourceLocation() + File.separator + "configFiles" + File.separator + "common"
                        + File.separator + "tenant-conf.json"));
        resourceAdminServiceClient =
                new ResourceAdminServiceClient(gatewayContextMgt.getContextUrls().getBackEndUrl(),
                        createSession(gatewayContextMgt));

        resourceAdminServiceClient.updateTextContent(TENANT_CONFIG_LOCATION, tenantConfSrcLocation);
        serverConfigurationManager.restoreToLastConfiguration(false);
    }
}
