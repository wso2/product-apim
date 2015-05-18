/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.ui.tests;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.TestException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.ui.tests.util.TestUtil;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;
import org.wso2.carbon.utils.ServerConstants;

import java.io.*;

/**
 * This test should be run with following configurations
 * Configure AM and IS to use in SSO mode
 * Add the externalLogoutPage attribute to SSO configuration
 * This test case check whether session already invalidated error get or not when the externalLogoutPage
 * is specified in site.json file
 */
public class APIMANAGER3272ExternalLogoutPageTestCase extends APIMIntegrationUiTestBase {
    private WebDriver driver;
    private final String TEST_DATA_USERNAME = "admin";
    private final String TEST_DATA_PASSWORD = "admin";
    private static final Log log = LogFactory.getLog(APIMANAGER3272ExternalLogoutPageTestCase.class);
    String externalLogoutPage = "custom logout page url"; //add the custom logout page url here
    private LogViewerClient logViewerClient;
    private String apiStoreUrl;
    private String sessionInvalidatedError = "java.lang.IllegalStateException: invalidate: Session already invalidated";


    @BeforeClass(alwaysRun = true)
    protected void setEnvironment() throws Exception {
        super.init();
        // Remove password from site.json if specified.
        if (!editStoreConfig(externalLogoutPage)) {
            throw new TestException("Failed to edit site.json");
        }

        driver = BrowserManager.getWebDriver();

        apiStoreUrl = getStoreURL();
        this.logViewerClient = new LogViewerClient(gatewayContext.getContextUrls().getBackEndUrl(),
                                                   TEST_DATA_USERNAME, TEST_DATA_PASSWORD);

    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.PLATFORM})
    @Test(groups = "wso2.am")
    public void loginAndLogoutToStoreCheckForSessionInvalidated() throws Exception {
        log.info("Started Logging into Store...");

        WebDriverWait wait = new WebDriverWait(driver, 30);
        //login to store
        driver.get(apiStoreUrl + "/site/pages/login.jag");

        // wait until load the page
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys(TEST_DATA_USERNAME);

        // this is the password field in IS sso login page
        if (driver.findElement(By.id("password")) != null) {
            driver.findElement(By.id("password")).clear();
            driver.findElement(By.id("password")).sendKeys(TEST_DATA_PASSWORD);

            driver.findElement(By.cssSelector(".btn")).click();
        }

        //logout from store
        driver.findElement(By.id("logout-link")).click();


        boolean status = false;
        int startLine = 0;
        int stopLine = 0;
        LogEvent[] logEvents = this.logViewerClient.getAllSystemLogs();
        if (logEvents.length > 0) {
            for (int i = 0; i < logEvents.length; ++i) {
                if (logEvents[i] != null) {
                    if (logEvents[i].getMessage().contains("ERROR")) {
                        log.error("Server log reports error " + logEvents[i].getMessage());
                        if (logEvents[i].getMessage().contains(sessionInvalidatedError)) {
                            Assert.fail("Session already invalidated...");
                        }

                    }

                }

            }

        }

    }

    /*
    * Edits site.json by setting keyStorePassword to empty and enabling SSO.
    */
    private boolean editStoreConfig(String externalLogoutPage) {
        String serverRoot = System.getProperty(ServerConstants.CARBON_HOME);
        String deploymentPath = serverRoot + getStoreSiteConfPath();
        File file = new File(deploymentPath);
        StringBuilder content = new StringBuilder();
        try {
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
                try {
                    while (reader.ready()) {
                        content.append(reader.readLine() + "\r\n");
                    }
                } finally {
                    reader.close();
                }

                int ssoConfigIndex = content.indexOf("ssoConfiguration");

                if (ssoConfigIndex > -1) {
                    String ssoConfigElement = content.substring(ssoConfigIndex);
                    log.debug("SSO Configuration before editing : " + ssoConfigElement);
                    int originalLength = ssoConfigElement.length();
                    ssoConfigElement = ssoConfigElement.replaceFirst("\"enabled\" : \"false\"",
                            "\"enabled\" : \"true\"").replaceAll
                            ("\"keyStorePassword\" : \"[a-zA-Z0-9]*\"", "\"keyStorePassword\" : \"\"");
                    ssoConfigElement.concat("\"externalLogoutPage\" : " + externalLogoutPage);
                    content.replace(ssoConfigIndex, originalLength, ssoConfigElement);
                    String jsonConfig = content.toString();
                    log.debug("SSO Configuration after editing : " + jsonConfig);

                    OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
                    try {
                        writer.write(jsonConfig);
                    } finally {
                        writer.close();
                    }
                    return true;
                }
            }

        } catch (IOException ex) {
            log.error("Exception occurred while file reading or writing " + ex);
        }

        return false;
    }


    /**
     * Gets the site.json location of Store App.
     *
     * @return path for site.json
     */
    private String getStoreSiteConfPath() {
        return "/repository/deployment/server/jaggeryapps/store/site/conf/site.json";
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        TestUtil.cleanUp(gatewayContext.getContextTenant().getContextUser().getUserName(),
                         gatewayContext.getContextTenant().getContextUser().getPassword(),
                         storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());
        driver.quit();
    }

}
