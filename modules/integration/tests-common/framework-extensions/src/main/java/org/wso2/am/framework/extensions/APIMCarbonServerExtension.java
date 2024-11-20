/*
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

package org.wso2.am.framework.extensions;

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
import org.wso2.carbon.automation.extensions.servers.carbonserver.TestServerManager;
import org.wso2.carbon.integration.common.utils.FileManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.xml.xpath.XPathExpressionException;

public class APIMCarbonServerExtension extends ExecutionListenerExtension {

    private static final Log log = LogFactory.getLog(APIMCarbonServerExtension.class);
    private TestServerManager serverManager;
    private String executionEnvironment;
    private static final String CUSTOM_AUTH_HANDLER_JAR = "CustomAPIAuthenticationHandler-1.0.0.jar";
    protected static final String CARBON_HOME = FrameworkPathUtil.getCarbonHome();

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
                    String customHandlerTargetPath =
                            serverManager.getCarbonHome() + File.separator + "repository" + File.separator +
                                    "components" + File.separator + "lib";
                    String dropinsPath =
                            serverManager.getCarbonHome() + File.separator + "repository" + File.separator +
                                    "components" + File.separator + "dropins";
                    String webappsPath = serverManager.getCarbonHome() + File.separator + "repository" + File.separator
                            + "deployment" + File.separator + "server" + File.separator + "webapps" + File.separator;

                    String synapseApiPath =
                            serverManager.getCarbonHome() + File.separator + "repository" + File.separator
                                    + "deployment" + File.separator + "server" + File.separator + "synapse-configs"
                                    + File.separator + "default" + File.separator + "api";

                    try {
                        FileManager.readFile(synapseApiPath);
                    } catch (FileNotFoundException e) {
                        new File(synapseApiPath).mkdirs();
                    }

                    FileManager.copyFile(new File(
                            resourcePath + File.separator + "synapseconfigs" + File.separator + "rest" + File.separator
                                    + "BackEndSecurity.xml"), synapseApiPath + File.separator + "BackEndSecurity.xml");
                    FileManager.copyFile(new File(
                            resourcePath + File.separator + "synapseconfigs" + File.separator + "rest" + File.separator
                                    + "JWKS-Backend.xml"), synapseApiPath + File.separator + "JWKS-Backend.xml");

                    String userStorePath = serverManager.getCarbonHome() + File.separator + "repository" + File.separator
                            + "deployment" + File.separator + "server" + File.separator + "userstores";
                    String databasePath = serverManager.getCarbonHome() + File.separator + "repository" + File.separator
                            + "database";

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

                    FileManager.copyJarFile(new File(getAMResourceLocation() + File.separator +
                                    "configFiles" + File.separator + "APIM5898" + File.separator + "subs-workflow-1.0.0.jar"),
                            customHandlerTargetPath);
                    String apimVersion = System.getProperty("apim.server.version");
                    FileManager.copyJarFile(new File(
                            getAMResourceLocation() + File.separator + "configFiles" + File.separator + "idpjwt" +
                                    File.separator + "org.wso2.am.thirdparty.km-" + apimVersion + ".jar"), dropinsPath);
                    String customHandlerSourcePath = getAMResourceLocation() + File.separator + "lifecycletest"
                            + File.separator + CUSTOM_AUTH_HANDLER_JAR;
                    FileManager.copyJarFile(new File(customHandlerSourcePath), customHandlerTargetPath);
                    String log4jPropertiesFile = getAMResourceLocation() + File.separator + "lifecycletest" +
                            File.separator + "log4j2.properties";
                    String log4jPropertiesTargetLocation =
                            serverManager.getCarbonHome() + File.separator + "repository" + File.separator + "conf"
                                    + File.separator + "log4j2.properties";
                    FileManager.copyFile(new File(log4jPropertiesFile), log4jPropertiesTargetLocation);

                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.JAXRS_BASIC_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.JAXRS_BASIC_WEB_APP_NAME);

                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.EP1_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.EP1_WEB_APP_NAME);
                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.PRODEP1_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.PRODEP1_WEB_APP_NAME);
                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.PRODEP2_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.PRODEP2_WEB_APP_NAME);
                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.PRODEP3_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.PRODEP3_WEB_APP_NAME);
                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.SANDBOXEP1_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.SANDBOXEP1_WEB_APP_NAME);
                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.SANDBOXEP2_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.SANDBOXEP2_WEB_APP_NAME);
                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.SANDBOXEP3_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.SANDBOXEP3_WEB_APP_NAME);
                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.WILDCARD_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.WILDCARD_WEB_APP_NAME);
                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.AM_MONITORING_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.AM_MONITORING_WEB_APP_NAME);
                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.GRAPHQL_API_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.GRAPHQL_API_WEB_APP_NAME);
                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.AUDIT_API_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.AUDIT_API_WEB_APP_NAME);
                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.ETCD_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.ETCD_WEB_APP_NAME);
                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.DUPLICATE_HEADER_BACKEND_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.DUPLICATE_HEADER_BACKEND_WEB_APP_NAME);

                    WebAppDeploymentUtil.copyWebApp(relativeResourcePath + File.separator + "war" + File.separator
                                    + APIMIntegrationConstants.BPMN_PROCESS_ENGINE_WEB_APP_NAME + ".war",
                            webappsPath + APIMIntegrationConstants.BPMN_PROCESS_ENGINE_WEB_APP_NAME);
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

    private static void handleException(String msg, Exception e) {

        log.error(msg, e);
        throw new RuntimeException(msg, e);
    }

}
