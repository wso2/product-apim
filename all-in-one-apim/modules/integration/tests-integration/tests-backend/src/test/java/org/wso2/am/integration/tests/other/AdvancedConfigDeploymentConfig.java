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

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;

import java.io.File;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class AdvancedConfigDeploymentConfig extends APIMIntegrationBaseTest {
    private static final Log log = LogFactory.getLog(AdvancedConfigDeploymentConfig.class);
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
        String tenantConfContent = FileUtils.readFileToString(new File(getAMResourceLocation() + File.separator
                + "configFiles" + File.separator + "notification" + File.separator + "tenant-conf.json"), "UTF-8");
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(tenantConfContent);
        restAPIAdmin.updateTenantConfig(jsonObject);

        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);

        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "common" +
                File.separator + "deployment.toml"));
        serverConfigurationManager.restartGracefully();
    }

    @AfterTest(alwaysRun = true)
    public void restoreConfigs() throws Exception {
        String tenantConfContent = FileUtils.readFileToString(new File(getAMResourceLocation() + File.separator
                + "configFiles" + File.separator + "common" + File.separator + "tenant-conf.json"), "UTF-8");
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(tenantConfContent);
        restAPIAdmin.updateTenantConfig(jsonObject);
        resourceAdminServiceClient =
                new ResourceAdminServiceClient(gatewayContextMgt.getContextUrls().getBackEndUrl(),
                        createSession(gatewayContextMgt));
        serverConfigurationManager.restoreToLastConfiguration();
    }
}
