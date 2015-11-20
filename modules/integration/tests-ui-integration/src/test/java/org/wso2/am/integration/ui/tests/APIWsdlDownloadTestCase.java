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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.ui.tests.util.APIMTestConstants;
import org.wso2.am.integration.ui.tests.util.TestUtil;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.test.utils.common.FileManager;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;

/**
 * This test case evaluates whether the WSDL file of provided SOAP endpoint
 * can be downloaded from both API publisher and store in super tenant and tenant modes.
 */
public class APIWsdlDownloadTestCase extends APIMIntegrationUiTestBase {
    protected AutomationContext gatewayContext;
    private ServerConfigurationManager configManagerDB, configManagerMasterDatasource, configManagerReg;
    private WebDriver driver;
    private WebDriverWait wait;
    private final String SUPER_TENANT_USERNAME = "admin";
    private final String SUPER_TENANT_PASSWORD = "admin";
    private final String TENANT_DOMAIN_NAME = "wso2.com";
    private final String TENANT_USERNAME = "testuser11";
    private final String TENANT_PASSWORD = "testuser11";
    private final String TEST_API_NAME = "AZ176";

    private static final Log log = LogFactory.getLog(APIWsdlDownloadTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();

        //create gateway server instance based on configuration given at automation.xml
        gatewayContext =
                new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_GATEWAY_MGT_INSTANCE, userMode);
        configManagerDB = new ServerConfigurationManager(gatewayContext);
        configManagerDB.applyConfigurationWithoutRestart(
                new File(TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator +
                        "AM" + File.separator + "configFiles" + File.separator + "wsdldownloadtest" + File.separator +
                        "WSO2REG_DB.h2.db"),
                new File(System.getProperty("carbon.home") + File.separator + "repository" + File.separator +
                        "database" + File.separator + "WSO2REG_DB.h2.db"), false);
        configManagerMasterDatasource = new ServerConfigurationManager(gatewayContext);
        configManagerMasterDatasource.applyConfigurationWithoutRestart(
                new File(TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator +
                        "AM" + File.separator + "configFiles" + File.separator + "wsdldownloadtest" + File.separator +
                        "master-datasources.xml"),
                new File(System.getProperty("carbon.home") + File.separator + "repository" + File.separator +
                        "conf" + File.separator + "datasources" + File.separator + "master-datasources.xml"), true);
        configManagerReg = new ServerConfigurationManager(gatewayContext);
        configManagerReg.applyConfigurationWithoutRestart(
                new File(TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator +
                        "AM" + File.separator + "configFiles" + File.separator + "wsdldownloadtest" + File.separator +
                        "registry.xml"),
                new File(System.getProperty("carbon.home") + File.separator + "repository" + File.separator +
                        "conf" + File.separator + "registry.xml"), true);
        //init is required to apply to modified configurations to the server
        super.init();
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

    @Test(groups = "wso2.am", priority = 1, description = "Download WSDL from publisher in super tenant mode")
    public void testDownloadWSDLFromPublisherSuperTenantUser()
            throws InterruptedException, IOException {
        createAPIWithWSDLEndpoint(TEST_API_NAME, SUPER_TENANT_USERNAME, SUPER_TENANT_PASSWORD,
                MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        downloadWSDLFromPublisher(SUPER_TENANT_USERNAME, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, TEST_API_NAME);
    }

    @Test(groups = "wso2.am", priority = 2, description = "Download WSDL from store in super tenant mode",
            dependsOnMethods = { "testDownloadWSDLFromPublisherSuperTenantUser" })
    public void testDownloadWSDLFromStoreSuperTenantUser() throws IOException, InterruptedException {
        downloadWSDLFromStore(SUPER_TENANT_USERNAME,
                              SUPER_TENANT_PASSWORD,
                              MultitenantConstants.SUPER_TENANT_DOMAIN_NAME,
                              TEST_API_NAME);
    }

    @Test(groups = "wso2.am", priority = 3, description = "Download WSDL from publisher in tenant mode",
            dependsOnMethods = { "testDownloadWSDLFromStoreSuperTenantUser" })
    public void testDownloadFromPublisherTenantUser()
            throws TenantMgtAdminServiceExceptionException, InterruptedException, IOException,
            XPathExpressionException {
        createAPIWithWSDLEndpoint(TEST_API_NAME, TENANT_USERNAME, TENANT_PASSWORD, TENANT_DOMAIN_NAME);
        downloadWSDLFromPublisher(TENANT_USERNAME, TENANT_DOMAIN_NAME, TEST_API_NAME);
    }

    @Test(groups = "wso2.am", priority = 4, description = "Download WSDL from store in tenant mode",
            dependsOnMethods = { "testDownloadFromPublisherTenantUser" })
    public void testDownloadFromStoreTenantUser() throws IOException, InterruptedException {
        downloadWSDLFromStore(TENANT_USERNAME, TENANT_PASSWORD, TENANT_DOMAIN_NAME, TEST_API_NAME);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        driver.quit();
        //remove added APIs from all tenants
        TestUtil.cleanUp(SUPER_TENANT_USERNAME, SUPER_TENANT_PASSWORD, storeUrls.getWebAppURLHttp(),
                publisherUrls.getWebAppURLHttp());
        TestUtil.cleanUp(TENANT_USERNAME + APIMTestConstants.EMAIL_DOMAIN_SEPARATOR + TENANT_DOMAIN_NAME, TENANT_PASSWORD,
                storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());
        //restore modified configurations to previous status
        configManagerReg.restoreToLastConfiguration();
        configManagerMasterDatasource.restoreToLastConfiguration();
        FileManager.deleteFile(System.getProperty("carbon.home") + File.separator + "repository" + File.separator +
                "database" + File.separator + "WSO2REG_DB.h2.db");
    }

    /**
     * Create api with a WSDL endpoint
     *
     * @param apiName  api name
     * @param username username to login to publisher
     * @param password password of the user
     * @param domain   domain of the user
     */
    private void createAPIWithWSDLEndpoint(String apiName, String username, String password, String domain)
            throws InterruptedException {
        try {
            driver.get(getPublisherURL());
        } catch (Exception e) {
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
        driver.findElement(By.linkText("Add")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("create-new-api")));
        driver.findElement(By.xpath("(//input[@name='create-api'])[2]")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("startFromExistingSOAPEndpoint")));
        driver.findElement(By.id("wsdl-url")).clear();
        driver.findElement(By.id("wsdl-url")).sendKeys("http://www.webservicex.com/globalweather.asmx?WSDL");
        driver.findElement(By.id("startFromExistingSOAPEndpoint")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("go_to_implement")));
        driver.findElement(By.id("name")).clear();
        driver.findElement(By.id("name")).sendKeys(apiName);
        driver.findElement(By.id("context")).clear();
        driver.findElement(By.id("context")).sendKeys(apiName);
        driver.findElement(By.id("version")).clear();
        driver.findElement(By.id("version")).sendKeys("1.0.0");
        driver.findElement(By.id("go_to_implement")).click();
        log.info("After API design");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@value='#managed-api']")));
        driver.findElement(By.xpath("//div[@value='#managed-api']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("go_to_manage")));
        driver.findElement(By.id("jsonform-0-elt-production_endpoints")).clear();
        driver.findElement(By.id("jsonform-0-elt-production_endpoints")).sendKeys(storeUrls.getWebAppURLHttp());
        driver.findElement(By.id("jsonform-0-elt-sandbox_endpoints")).clear();
        driver.findElement(By.id("jsonform-0-elt-sandbox_endpoints")).sendKeys(storeUrls.getWebAppURLHttp());
        driver.findElement(By.id("go_to_manage")).click();
        log.info("After API manage");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("publish_api")));
        driver.findElement(By.xpath("//button[@type='button']")).click();
        driver.findElement(By.xpath("//input[@value='Gold']")).click();
        driver.findElement(By.id("publish_api")).click();
        log.info("After API publish");
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

    /**
     * Download WSDL file from the publisher
     *
     * @param username username to login to store
     * @param tenant   domain of the login user
     * @param apiName  api name of the published api
     * @throws java.io.IOException          if downloaded file couldn't found
     * @throws InterruptedException If an interruption occurs in the threads
     */
    private void downloadWSDLFromPublisher(String username, String tenant, String apiName)
            throws IOException, InterruptedException {

        String tenantUnawareUsername;
        //Set the domain name for non super tenant users
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenant)) {
            tenantUnawareUsername = username.concat("-AT-" + tenant);
        } else {
            tenantUnawareUsername = username;
        }

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Go to Overview")));
        driver.findElement(By.linkText("Go to Overview")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("lifecyclesLink")));
        driver.findElement(By.id("lifecyclesLink")).click();

        //Change api status to published if it is not already set
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("editStatus")));
        Select status = new Select(driver.findElement(By.id("editStatus")));
        if (!"PUBLISHED".equals(status.getFirstSelectedOption().getText())) {
            status.selectByVisibleText("PUBLISHED");
        }

        driver.findElement(By.id("updateStateButton")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("viewLink")));
        driver.findElement(By.id("viewLink")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("/" + tenantUnawareUsername + "--" +
                apiName + "1.0.0.wsdl")));
        //Download wsdl by clicking its link
        driver.findElement(By.linkText("/" + tenantUnawareUsername + "--" + apiName + "1.0.0.wsdl")).click();
        File file = new File(
                System.getProperty("download.location") + File.separator + tenantUnawareUsername + "--" + apiName +
                        "1.0.0.wsdl");
        while (!file.exists()) {
            Thread.sleep(1000);
        }
        //check whether downloaded file is readable
        boolean isReadableFile = file.canRead();
        file.delete();
        //Logout from publisher once the test is completed
        publisherLogout();
        Assert.assertEquals(isReadableFile, true, "Download WSDL from publisher failed");
    }

    /**
     * Download WSDL file from API Store
     *
     * @param username username to login to store
     * @param password password of the user
     * @param tenant   domain of the login user
     * @param apiName  api name of the published api
     * @throws java.io.IOException          if downloaded file couldn't found
     * @throws InterruptedException If an interruption occurs in the threads
     */
    private void downloadWSDLFromStore(String username, String password, String tenant, String apiName)
            throws IOException, InterruptedException {
        try {
            driver.get(getStoreURL() + "?tenant=" + tenant);
        } catch (Exception e) {
            log.error("Couldn't retrieve store url of api manager");
        }

        Thread.sleep(2000);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-link")));
        WebElement login = driver.findElement(By.id("login-link"));
        String tenantUnawareUsername = null;
        if (login != null) {
            login.click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginBtn")));
            if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenant)) {
                driver.findElement(By.id("username")).sendKeys(username + "@" + tenant);
                tenantUnawareUsername = username.concat("-AT-" + tenant);
            } else {
                driver.findElement(By.id("username")).sendKeys(username);
                tenantUnawareUsername = username;
            }
            driver.findElement(By.id("password")).clear();
            driver.findElement(By.id("password")).sendKeys(password);
            driver.findElement(By.id("loginBtn")).click();
            log.info("After store login");
        }

        long waitTime = 30000; // Maximum waiting time till API appear on UI
        long targetWaitTime = System.currentTimeMillis() + waitTime;

        while ((!driver.getPageSource().contains(apiName + "-1.0.0")) && System.currentTimeMillis() < targetWaitTime) {
            Thread.sleep(500); // Waiting 0.5 second to check API is visible on UI
            driver.navigate().refresh();
        }
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText(apiName + "-1.0.0")));
        driver.findElement(By.linkText(apiName + "-1.0.0")).click();
        driver.findElement(By.linkText("Overview")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Download WSDL")));
        driver.findElement(By.linkText("Download WSDL")).click();
        File file = new File(
                System.getProperty("download.location") + File.separator + tenantUnawareUsername + "--" + apiName +
                        "1.0.0.wsdl");
        while (!file.exists()) {
            Thread.sleep(1000);
        }
        boolean isReadableFile = file.canRead();
        file.delete();
        //Logout from store once the test is completed
        storeLogout(username);
        Assert.assertEquals(isReadableFile, true, "Download WSDL from store failed");
    }

    /**
     * Logout from the store
     */
    private void storeLogout(String username) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.partialLinkText(username)));
        driver.findElement(By.partialLinkText(username)).click();
        driver.findElement(By.id("logout-link")).click();
        log.info("After store logout");
    }
}
