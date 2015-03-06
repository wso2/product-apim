/*
*Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.server.mgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.common.LogViewerClient;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.logging.view.stub.LogViewerLogViewerException;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;

import java.rmi.RemoteException;

import static org.testng.Assert.assertFalse;

/**
 * This class verifies errors on server startup console
 */
public class APIMgtServerStartupTestCase {

    private LogViewerClient logViewerClient;
    private static final Log log = LogFactory.getLog(APIMgtServerStartupTestCase.class);
    private static final String SERVER_START_LINE = "Starting WSO2 Carbon";
    private static final String MANAGEMENT_CONSOLE_URL = "Mgt Console URL";

    @BeforeSuite(alwaysRun = true)
    public void initialize() throws Exception {
        AutomationContext autoContext = new AutomationContext();
        logViewerClient = new LogViewerClient(autoContext.getContextUrls().getBackEndUrl(),
                                              autoContext.getSuperTenant().getTenantAdmin().getUserName(),
                                              autoContext.getSuperTenant().getTenantAdmin().getPassword());
    }

    @Test(groups = "wso2.all", description = "verify server startup errors")
    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
    public void testVerifyLogs() throws RemoteException, LogViewerLogViewerException {
        boolean status = false;
        int startLine = 0;
        int stopLine = 0;
        String errorMessage = "";
        LogEvent[] logEvents = logViewerClient.getAllRemoteSystemLogs();
        if (logEvents.length > 0) {
            for (int i = 0; i < logEvents.length; i++) {
                if (logEvents[i] != null) {
                    if (logEvents[i].getMessage().contains(SERVER_START_LINE)) {
                        stopLine = i;
                        log.info("Server started message found - " + logEvents[i].getMessage());
                    }
                    if (logEvents[i].getMessage().contains(MANAGEMENT_CONSOLE_URL)) {
                        startLine = i;
                        log.info("Server stopped message found - " + logEvents[i].getMessage());
                    }
                }
                if (startLine != 0 && stopLine != 0) {
                    break;
                }
            }
            while (startLine <= stopLine) {
                if (logEvents[startLine].getPriority().contains("ERROR")) {
                    errorMessage = logEvents[startLine].getMessage() + "\n" + logEvents[startLine].getStacktrace();
                    log.error("Startup contain errors - " + errorMessage);
                    status = true;
                    break;
                }
                startLine++;
            }
        }
        assertFalse(status, "Server started with errors. [" + errorMessage + "]");
    }

}
