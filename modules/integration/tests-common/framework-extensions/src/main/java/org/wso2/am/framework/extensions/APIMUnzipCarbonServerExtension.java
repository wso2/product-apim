/*
 *   Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import java.io.File;
import java.io.IOException;
import javax.xml.xpath.XPathExpressionException;

public class APIMUnzipCarbonServerExtension extends ExecutionListenerExtension {

    private static final Log log = LogFactory.getLog(CarbonServerExtension.class);
    private TestServerManager serverManager;
    private String executionEnvironment;

    public APIMUnzipCarbonServerExtension() {
    }

    @Override
    public void initiate() {
        if(getParameters().get(ExtensionConstants.SERVER_STARTUP_PORT_OFFSET_COMMAND) == null) {
            getParameters().put(ExtensionConstants.SERVER_STARTUP_PORT_OFFSET_COMMAND, "0");
        }
        try {
            executionEnvironment =
                    getAutomationContext().getConfigurationValue(ContextXpathConstants.EXECUTION_ENVIRONMENT);
        } catch (XPathExpressionException e) {
            handleException("Error while initiating test environment", e);
        }
        configureProduct();
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
    }

    private void configureProduct() {
        serverManager = new TestServerManager(getAutomationContext(), null, getParameters()) {

            @Override
            public String startServer() throws AutomationFrameworkException, IOException {
                if (this.carbonHome == null) {
                    if (this.carbonZip == null) {
                        this.carbonZip = System.getProperty("carbon.zip");
                    }

                    if (this.carbonZip == null) {
                        throw new IllegalArgumentException("carbon zip file cannot find in the given location");
                    }

                    this.carbonHome = this.carbonServer.setUpCarbonHome(this.carbonZip);
                    this.configureServer();
                }

                log.info("Carbon Home - " + this.carbonHome);
                return this.carbonHome;

            }
        };
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new RuntimeException(msg, e);
    }

}
