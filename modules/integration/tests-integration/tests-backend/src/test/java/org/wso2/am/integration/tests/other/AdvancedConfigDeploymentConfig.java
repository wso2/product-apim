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

import org.testng.annotations.AfterGroups;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;

public class AdvancedConfigDeploymentConfig extends APIMIntegrationBaseTest {
    private ServerConfigurationManager serverConfigurationManager;
    private AutomationContext superTenantKeyManagerContext;

    @BeforeTest(alwaysRun = true)
    public void startServerWithMultipleConfigs() throws Exception {
        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);

        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);

        String artifactsLocation = TestConfigurationProvider.getResourceLocation() +
                File.separator + "artifacts" + File.separator + "AM" + File.separator +
                "configFiles" + File.separator + "common" + File.separator;

        String identityConfigArtifactLocation = artifactsLocation + "identity.xml";

        String identityRepositoryConfigLocation = carbonHome + File.separator + "repository" +
                File.separator + "conf" + File.separator + "identity" + File.separator +
                "identity.xml";

        File identityConfSourceFile = new File(identityConfigArtifactLocation);
        File identityConfTargetFile = new File(identityRepositoryConfigLocation);

        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "common" +
                File.separator + "api-manager.xml"));
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "common" +
                File.separator + "axis2.xml"));
        /*serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "common" +
                File.separator + "carbon.xml"));
        serverConfigurationManager
                .applyConfigurationWithoutRestart(identityConfSourceFile, identityConfTargetFile, true);*/
        /*serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "common" +
                File.separator + "user-mgt.xml"));*/
        /*serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "common" +
                File.separator + "log4j.properties"));*/
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "common" +
                File.separator + "passthru-http.properties"));
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "common" +
                File.separator + "synapse.properties"));

        serverConfigurationManager.restartGracefully();
    }

    @AfterTest(alwaysRun = true)
    public void restoreConfigs() throws Exception {
        serverConfigurationManager.restoreToLastConfiguration(false);
    }
}
