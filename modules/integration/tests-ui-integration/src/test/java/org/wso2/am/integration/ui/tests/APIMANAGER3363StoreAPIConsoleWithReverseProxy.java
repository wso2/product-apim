/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.am.integration.ui.tests;

import org.apache.http.protocol.HttpContext;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.ui.pages.login.LoginPage;
import org.wso2.am.integration.ui.pages.tenant.TenantHomePage;
import org.wso2.am.integration.ui.pages.tenant.TenantListpage;
import org.wso2.am.integration.ui.tests.util.TestUtil;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/*
    Need to configure LB with APIM 1.8 and run the test case
    note that replace the server urls with LB urls
 */
public class APIMANAGER3363StoreAPIConsoleWithReverseProxy extends APIMIntegrationUiTestBase {
    private String TEST_DATA_API_NAME = "APIMANAGER3363";
    private String TEST_DATA_API_VERSION = "1.0.0";
    private String TEST_DATA_TENANT = "apimanager3363.com";
    private String TEST_DATA_TENANT_ADMIN_USER = "admin";
    private String TEST_DATA_TENANT_ADMIN_PASSWORD = "123456";
    private String TEST_DATA_TENANT_PUBLISHER = "admin@apimanager3363.com";

    private WebDriver driver;
    private boolean acceptNextAlert = true;
    private String publisherURL;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        driver = BrowserManager.getWebDriver();
        driver.get(getLoginURL());

        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
    }

    @Test(groups = "wso2.am", description = "Create tenant and api")
    public void createTenantAndAPI() throws Exception {
        LoginPage login = new LoginPage(driver);
        login.loginAs(gatewayContext.getContextTenant().getContextUser().getUserName(),
                gatewayContext.getContextTenant().getContextUser().getPassword());
        TenantHomePage addNewTenantHome = new TenantHomePage(driver);

        String firstName = "admin";
        String lastName = "admin";
        String email = "admin@apimanager3363.com";
        addNewTenantHome.addNewTenant(TEST_DATA_TENANT, firstName, lastName, TEST_DATA_TENANT_ADMIN_USER, TEST_DATA_TENANT_ADMIN_PASSWORD, email);
        TenantListpage tenantListpage = new TenantListpage(driver);
        tenantListpage.checkOnUplodedTenant(TEST_DATA_TENANT);

        // login to publisher and create new API
        publisherURL = getPublisherURL();
        HttpContext httpContext = TestUtil.login(TEST_DATA_TENANT_PUBLISHER, TEST_DATA_TENANT_ADMIN_PASSWORD, publisherURL);
        assertTrue(TestUtil.addAPI(TEST_DATA_TENANT_PUBLISHER, TEST_DATA_API_NAME, TEST_DATA_API_VERSION, httpContext, publisherURL));
        assertTrue(TestUtil.publishAPI(TEST_DATA_TENANT_PUBLISHER, TEST_DATA_API_NAME, TEST_DATA_API_VERSION, httpContext, publisherURL));

    }

    @Test(groups = "wso2.am", description = "Create tenant and api", dependsOnMethods = "createTenantAndAPI")
    public void checkAPIConsoleAvailability() throws Exception {

        // login to publisher and add new doc
        driver.get(publisherURL + "/site/pages/login.jag");
        WebDriverWait wait = new WebDriverWait(driver, 30);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys(TEST_DATA_TENANT_PUBLISHER);
        driver.findElement(By.id("pass")).clear();
        driver.findElement(By.id("pass")).sendKeys(TEST_DATA_TENANT_ADMIN_PASSWORD);
        driver.findElement(By.id("loginButton")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Browse")));
        driver.findElement(By.linkText("Browse")).click();

        // waiting for API in store page
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("img.thumb.responsive")));

        driver.findElement(By.cssSelector("img.thumb.responsive")).click();
        driver.findElement(By.id("docsLink")).click();
        driver.findElement(By.linkText("Add New Document")).click();
        driver.findElement(By.id("docName")).clear();
        driver.findElement(By.id("docName")).sendKeys("Test");
        driver.findElement(By.id("summary")).clear();
        driver.findElement(By.id("summary")).sendKeys("test");
        driver.findElement(By.id("saveDocBtn")).click();

        Thread.sleep(60 * 1000); // waiting to publish API in store

        // go to store > API > API Console
        driver.get(getStoreURL() + "?tenant=" + TEST_DATA_TENANT);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText(TEST_DATA_API_NAME)));
        driver.findElement(By.linkText(TEST_DATA_API_NAME)).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("API Console")));
        driver.findElement(By.linkText("API Console")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.info_title")));
        // API name is visible if the page loaded successfully
        assertEquals(driver.findElement(By.cssSelector("div.info_title")).getText(), TEST_DATA_API_NAME);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        if (driver != null) {
            driver.quit();
        }
    }
}
