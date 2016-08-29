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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.test.utils.APIMTestConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.context.ContextXpathConstants;
import org.wso2.carbon.automation.engine.exceptions.AutomationFrameworkException;
import org.wso2.carbon.automation.engine.extensions.ExecutionListenerExtension;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.extensions.ExtensionConstants;
import org.wso2.carbon.automation.extensions.servers.carbonserver.CarbonServerExtension;
import org.wso2.carbon.automation.extensions.servers.carbonserver.TestServerManager;
import org.wso2.carbon.integration.common.utils.FileManager;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;

public class APIMCarbonServerExtension extends ExecutionListenerExtension {

    private static final Log log = LogFactory.getLog(CarbonServerExtension.class);
    private TestServerManager serverManager;
    private String executionEnvironment;

    @Override
    public void initiate() {
        configureProduct();

        try {
            if(getParameters().get(ExtensionConstants.SERVER_STARTUP_PORT_OFFSET_COMMAND) == null) {
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

            public void configureServer() throws AutomationFrameworkException {

                String resourcePath = getAMResourceLocation();

                try {
                    FileManager.copyFile(new File(resourcePath + File.separator + "configFiles" + File.separator +
                                    "throttling" + File.separator + "jndi.properties")
                            , serverManager.getCarbonHome() + File.separator + "repository" + File.separator +
                                    "conf" + File.separator +"jndi.properties");

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
