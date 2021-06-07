/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.jwt.idp;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.nio.file.Paths;

public class ExternalIDPJWTTestSuite extends APIManagerLifecycleBaseTest {

    private ServerConfigurationManager serverConfigurationManager;

    @BeforeTest(alwaysRun = true)
    public void loadConfiguration() throws Exception {

        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
        File targetFile = Paths.get(FrameworkPathUtil.getCarbonHome(), "repository", "resources", "security",
                "client-truststore.jks").toFile();
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "idpjwt" + File.separator + "deployment.toml"));
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "idpjwt" + File.separator + "client-truststore" +
                ".jks"), targetFile,true);
        serverConfigurationManager.restartGracefully();
    }

    @AfterTest(alwaysRun = true)
    public void restoreConfiguration() throws Exception {

        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
        serverConfigurationManager.restoreToLastConfiguration();
    }

}
