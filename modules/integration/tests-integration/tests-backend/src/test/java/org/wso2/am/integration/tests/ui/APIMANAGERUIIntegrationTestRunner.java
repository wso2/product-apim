/*
 *Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


import java.io.File;
import java.util.Map;

import static org.testng.Assert.assertTrue;

/**
 * This test case triggers the Puppeteer based UI integration tests.
 * Relevant JavaScript tests are in following directory.
 * <product-apim>/modules/integration/tests-integration/tests-backend/src/test/resources/jest-integration-tests
 *
 */
public class APIMANAGERUIIntegrationTestRunner extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(APIMANAGERUIIntegrationTestRunner.class);

    private String npmCommand;
    private String npmSourcePath;

    static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static String output(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line + System.getProperty("line.separator"));
            }
        } finally {
            br.close();
        }
        return sb.toString();
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() {

        try {
            super.init();
            publisherURLHttp = publisherUrls.getWebAppURLHttp();
            npmCommand = isWindows() ? "npm.cmd" : "npm";
            npmSourcePath = FrameworkPathUtil.getSystemResourceLocation() + File.separator + "jest-integration-tests";

        } catch (APIManagerIntegrationTestException e) {
            assertTrue(false, "Error occurred while initializing UI test executor");
        }
    }

    @Test(groups = {"wso2.am"}, description = "UI Integration test executor")
    public void testAllUI() {
        run("install");
        run("test");
    }

    void run(String arguments) {
        try {
            ProcessBuilder process = new ProcessBuilder(npmCommand, arguments)
                    .directory(new File(npmSourcePath));
            Map<String, String> env = process.environment();
            env.put("WSO2_PORT_OFFSET", String.valueOf(portOffset));
            Process activeProcess = process.start();
            int exitCode = activeProcess.waitFor();
            log.info("Echo Input:\n" + output(activeProcess.getInputStream()));
            String errorOut = output(activeProcess.getErrorStream());
            log.warn("Echo Error:\n" + errorOut);
            Assert.assertEquals(errorOut,0, exitCode);
        } catch (IOException | InterruptedException e) {
            log.error("Something went wrong while executing the UI tests", e);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }

}
