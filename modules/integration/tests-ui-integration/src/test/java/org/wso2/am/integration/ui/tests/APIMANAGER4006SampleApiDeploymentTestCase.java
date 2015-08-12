/*
 *
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package org.wso2.am.integration.ui.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.ui.tests.util.APIMTestConstants;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.File;

/**
 * This test case evaluates whether the WSDL file of provided SOAP endpoint
 * can be downloaded from both API publisher and store in super tenant and tenant modes.
 */
public class APIMANAGER4006SampleApiDeploymentTestCase extends APIMIntegrationUiTestBase {
    protected AutomationContext gatewayContext;
    private ServerConfigurationManager configManagerApiManager;
    private WebDriver driver;
    private WebDriverWait wait;
    private final String SUPER_TENANT_USERNAME = "admin";
    private final String SUPER_TENANT_PASSWORD = "admin";
    private final String TENANT_DOMAIN_NAME = "wso2.com";
    private final String TENANT_USERNAME = "testuser11";
    private final String TENANT_PASSWORD = "testuser11";

    private static final Log log = LogFactory.getLog(APIMANAGER4006SampleApiDeploymentTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        init();

        //create gateway server instance based on configuration given at automation.xml
        gatewayContext =
            new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_GATEWAY_INSTANCE, userMode);
        configManagerApiManager = new ServerConfigurationManager(gatewayContext);
        configManagerApiManager.applyConfiguration(
            new File(TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator +
                "AM" + File.separator + "configFiles" + File.separator + "apim4006test" + File.separator +
                "api-manager.xml"));
    }

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        FirefoxProfile firefoxProfile = new FirefoxProfile();
        firefoxProfile.setPreference("browser.download.folderList", 2);
        firefoxProfile.setPreference("browser.download.manager.showWhenStarting", false);
        firefoxProfile.setPreference("browser.download.dir", System.getProperty("download.location"));
        firefoxProfile.setPreference("browser.helperApps.alwaysAsk.force", false);
        //Set browser settings to automatically download wsdl files
        firefoxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/force-download");
        driver = new FirefoxDriver(firefoxProfile);
        wait = new WebDriverWait(driver, 60);
    }

    @Test(groups = "wso2.am", priority = 1, description = "Deploy sample API when unlimited tier unavailable" +
        " in super tenant mode")
    public void testDeploySampleAPISuperTenantUser() {
        deploySampleAPI(SUPER_TENANT_USERNAME, SUPER_TENANT_PASSWORD, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
    }

    @Test(groups = "wso2.am", priority = 2, description = "Deploy sample API when unlimited tier unavailable" +
        " in tenant mode")
    public void testDeploySampleAPITenantUser() {
        deploySampleAPI(TENANT_USERNAME, TENANT_PASSWORD, TENANT_DOMAIN_NAME);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        driver.quit();
        //remove added APIs from all tenants
        super.cleanUp(SUPER_TENANT_USERNAME, SUPER_TENANT_PASSWORD, storeUrls.getWebAppURLHttp(),
            publisherUrls.getWebAppURLHttp());
        super.cleanUp(TENANT_USERNAME + APIMTestConstants.EMAIL_DOMAIN_SEPARATOR + TENANT_DOMAIN_NAME, TENANT_PASSWORD,
            storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());
        //restore modified configurations to previous status
        configManagerApiManager.restoreToLastConfiguration();
    }

    /**
     * Deploy sample API when Unlimited tier is not available
     *
     * @param username Username to log into publisher
     * @param password Password of the user
     * @param domain tenant domain of the user
     */
    private void deploySampleAPI(String username, String password, String domain) {
        try {
            driver.get(getPublisherURL());
        } catch (Exception e) { //This exception doesn't need to be thrown since the test case gets failed
            // if an exception occurs.
            log.error("couldn't retrieve publisher url", e);
        }
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginButton")));
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(domain)) {
            driver.findElement(By.id("username")).
                sendKeys(username + APIMTestConstants.EMAIL_DOMAIN_SEPARATOR + domain);
        } else {
            driver.findElement(By.id("username")).sendKeys(username);
        }
        driver.findElement(By.id("pass")).sendKeys(password);
        driver.findElement(By.id("loginButton")).click();
        log.info("After publisher login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add")));
        driver.findElement(By.id("deploy_sample1")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("WeatherAPI")));
        driver.findElement(By.linkText("WeatherAPI")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("viewLink")));
        driver.findElement(By.id("viewLink")).click();
        String assignedTier = driver.findElement(By.id("tierAvb")).getText();

        publisherLogout();

        Assert.assertEquals(assignedTier, "Gold", "Sample API deployment failed");
    }

    /**
     * Logout from the publisher
     */
    private void publisherLogout() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("userMenu")));
        driver.findElement(By.id("userMenu")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button.btn.btn-danger")));
        driver.findElement(By.cssSelector("button.btn.btn-danger")).click();
        log.info("After publisher logout");
    }

}
