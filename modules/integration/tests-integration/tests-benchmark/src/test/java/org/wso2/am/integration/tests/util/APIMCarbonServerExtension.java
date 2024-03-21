/*
 *   Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.util;

import java.io.File;
import java.io.IOException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.test.utils.APIMTestConstants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.context.ContextXpathConstants;
import org.wso2.carbon.automation.engine.exceptions.AutomationFrameworkException;
import org.wso2.carbon.automation.engine.extensions.ExecutionListenerExtension;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.extensions.ExtensionConstants;
import org.wso2.carbon.automation.extensions.servers.carbonserver.CarbonServerExtension;
import org.wso2.carbon.automation.extensions.servers.carbonserver.TestServerManager;
import org.wso2.carbon.integration.common.utils.FileManager;

public class APIMCarbonServerExtension extends ExecutionListenerExtension {

    private static final Log log = LogFactory.getLog(CarbonServerExtension.class);
    private TestServerManager serverManager;
    private String executionEnvironment;

    private static void handleException(String msg, Exception e) {

        log.error(msg, e);
        throw new RuntimeException(msg, e);
    }

    @Override
    public void initiate() {

        configureProduct();

        try {
            if (getParameters().get(ExtensionConstants.SERVER_STARTUP_PORT_OFFSET_COMMAND) == null) {
                getParameters().put(ExtensionConstants.SERVER_STARTUP_PORT_OFFSET_COMMAND, "0");
            }
            executionEnvironment =
                    getAutomationContext().getConfigurationValue(ContextXpathConstants.EXECUTION_ENVIRONMENT);
        } catch (XPathExpressionException e) {
            handleException("Error while initiating test environment", e);
        }
    }

    @Override
    public void onExecutionStart() throws AutomationFrameworkException {

        try {
            if (executionEnvironment.equalsIgnoreCase(ExecutionEnvironment.STANDALONE.name())) {
                String carbonHome = serverManager.startServer();
                System.setProperty(ExtensionConstants.CARBON_HOME, carbonHome);
            }
        } catch (Exception e) {
            handleException("Fail to start carbon server ", e);
        }
    }

    @Override
    public void onExecutionFinish() throws AutomationFrameworkException {

        try {
            if (executionEnvironment.equalsIgnoreCase(ExecutionEnvironment.STANDALONE.name())) {
                serverManager.stopServer();
            }
        } catch (Exception e) {
            handleException("Fail to stop carbon server ", e);
        }
    }

    private void configureProduct() {

        getParameters().put("-DosgiConsole", APIMTestConstants.OSGI_CONSOLE_TELNET_PORT);
        serverManager = new TestServerManager(getAutomationContext(), null, getParameters()) {
            @Override
            public String startServer() throws AutomationFrameworkException, IOException, XPathExpressionException {

                return super.startServer();
            }

            public void configureServer() throws AutomationFrameworkException {

                String resourcePath = getAMResourceLocation();
                String relativeResourcePath = File.separator + "artifacts" + File.separator + "AM";

                try {
                    String userStorePath = serverManager.getCarbonHome() + File.separator + "repository" + File.separator
                            + "deployment" + File.separator + "server" + File.separator + "userstores";
                    String databasePath = serverManager.getCarbonHome() + File.separator + "repository" + File.separator
                            + "database";
                    String webappsPath = serverManager.getCarbonHome() + File.separator + "repository" + File.separator
                            + "deployment" + File.separator + "server" + File.separator + "webapps" + File.separator;

                    FileManager.copyFile(new File(resourcePath + File.separator + "configFiles" + File.separator +
                                    "originalFile" + File.separator + "deployment.toml")
                            , serverManager.getCarbonHome() + File.separator + "repository" + File.separator +
                                    "conf" + File.separator + "deployment.toml");

                    File userStoreFile = new File(userStorePath);
                    if (!userStoreFile.exists() && !userStoreFile.mkdir()) {
                        log.error("Error while creating the user store directory : "
                                + userStorePath);
                    }

                    FileUtils.copyFile(new File(
                                    resourcePath + File.separator + "configFiles" + File.separator + "userstores"
                                            + File.separator + "database" + File.separator + "WSO2SEC_DB.mv.db"),
                            new File(databasePath + File.separator + "WSO2SEC_DB.mv.db"));

                    FileManager.copyFile(new File(
                                    resourcePath + File.separator + "configFiles" + File.separator + "userstores"
                                            + File.separator + "secondary.xml"),
                            userStorePath + File.separator + "secondary.xml");

                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.JAXRS_BASIC_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.JAXRS_BASIC_WEB_APP_NAME);

                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.AM_MONITORING_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.AM_MONITORING_WEB_APP_NAME);

                    log.info("Web Apps Deployed");
                } catch (IOException e) {
                    throw new AutomationFrameworkException(e.getMessage(), e);
                }

            }
        };
    }

    private String getAMResourceLocation() {

        return FrameworkPathUtil.getSystemResourceLocation() + "artifacts" + File.separator + "AM";
    }

}
