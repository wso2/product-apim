/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.scenario.tests.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertTrue;

/**
 * Initialize the deployment to run the remaining scenario tests.
 * <p>
 * Ex:
 * Deploys APIStatusMonitor webapp which be used to retrieve API deployment status in gateway nodes
 * </p>
 */
public class DeploymentInitializerTestCase extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(DeploymentInitializerTestCase.class);

    private WebAppAdminClient webAppAdminClient;
    private String resourceLocation = System.getProperty("test.resource.location");

    @Test
    public void deployingWebAPPs() throws Exception {
        setup();
        log.info(System.getProperty("test.resource.location"));
        log.info("Start deploying Web Apps");
        String testArtifactPath = resourceLocation + File.separator + "artifacts";
        String APIStatusMonitorWebAppSourcePath = testArtifactPath + File.separator +
                APIMIntegrationConstants.AM_MONITORING_WEB_APP_NAME + ".war";

        webAppAdminClient = getWebAppAdminClient();
        webAppAdminClient.uploadWarFile(APIStatusMonitorWebAppSourcePath);

        WebAppDeploymentUtil.isMonitoringAppDeployed(getBackendEndServiceEndPointHttps(""));

        String productionWebAppName = "jaxrs_basic";
        String sourcePathProd =  testArtifactPath + "/" + productionWebAppName + ".war";

        webAppAdminClient.uploadWarFile(sourcePathProd);

        String sessionCookie = login(serviceEndpoint, "admin", "admin");

        boolean isWebAppDeployProd = WebAppDeploymentUtil.isWebApplicationDeployed
                (serviceEndpoint, sessionCookie, productionWebAppName);
        assertTrue(isWebAppDeployProd, productionWebAppName + " is not deployed");
        // Once distributed setup is created use gateway webapp url from TG
    }
}
