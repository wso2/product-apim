/*
*Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.am.integration.ui;

import org.apache.http.protocol.HttpContext;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.am.integration.ui.util.TestUtil;
import org.wso2.carbon.automation.api.clients.registry.ResourceAdminServiceClient;
import org.wso2.carbon.automation.core.BrowserManager;
import org.wso2.carbon.automation.core.ProductConstant;

import static org.testng.Assert.assertTrue;

public class APIMANAGER3017ExternalStoreLifeCycleTestCase extends AMIntegrationUiTestBase {
    public static final String BLOCKED = "BLOCKED", PUBLISHED = "PUBLISHED";
    private WebDriver driver;
    private String TEST_DATA_TENANT_DOMAIN = "apim3017.com",
            TEST_DATA_TENANT_FIRST_NAME = "admin",
            TEST_DATA_TENANT_LAST_NAME = "admin",
            TEST_DATA_ADMIN_USER_NAME = "admin",
            TEST_DATA_PASSWORD = "123456",
            TEST_DATA_EMAIL = "admin@apim3017.com",
            TEST_DATA_API_NAME = "APIM3017API",
            TEST_DATA_API_VERSION = "1.0.0";
    private String STORE_URL;

    @BeforeClass(alwaysRun = true)
    protected void init() throws Exception {
        super.init(0);
        STORE_URL = getStoreURL(ProductConstant.AM_SERVER_NAME);

        ResourceAdminServiceClient resourceAdminServiceStub = new ResourceAdminServiceClient(amServer.getBackEndUrl()
                , amServer.getSessionCookie());

        String configuration = "<ExternalAPIStores>\n" +
                               "    <StoreURL>" + STORE_URL + "</StoreURL>\n" +
                               "    <ExternalAPIStore id=\"Store1\" type=\"wso2\">\n" +
                               "        <DisplayName>Store1</DisplayName>\n" +
                               "        <Endpoint>" + STORE_URL + "</Endpoint>\n" +
                               "        <Username>" + TEST_DATA_EMAIL + "</Username>\n" +
                               "        <Password>" + TEST_DATA_PASSWORD + "</Password>\n" +
                               "    </ExternalAPIStore>\n" +
                               "</ExternalAPIStores>";

        resourceAdminServiceStub.updateTextContent
                ("/_system/governance/apimgt/externalstores/external-api-stores.xml", configuration);

        driver = BrowserManager.getWebDriver();
    }

    public void generateTenant() throws Exception {
        WebDriverWait wait = new WebDriverWait(driver, 10);

        driver.get(getLoginURL(ProductConstant.AM_SERVER_NAME));

        // wait until load the page
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("txtUserName")));

        driver.findElement(By.id("txtUserName")).clear();
        driver.findElement(By.id("txtUserName")).sendKeys("admin");
        driver.findElement(By.id("txtPassword")).clear();
        driver.findElement(By.id("txtPassword")).sendKeys("admin");
        driver.findElement(By.cssSelector("input.button")).click();

        // wait until load the page
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#menu-panel-button3 > span")));

        driver.findElement(By.cssSelector("#menu-panel-button3 > span")).click();
        driver.findElement(By.linkText("Add New Tenant")).click();
        // wait until load the page
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("domain")));

        driver.findElement(By.id("domain")).clear();
        driver.findElement(By.id("domain")).sendKeys(TEST_DATA_TENANT_DOMAIN);
        driver.findElement(By.id("admin-firstname")).clear();
        driver.findElement(By.id("admin-firstname")).sendKeys(TEST_DATA_TENANT_FIRST_NAME);
        driver.findElement(By.id("admin-lastname")).clear();
        driver.findElement(By.id("admin-lastname")).sendKeys(TEST_DATA_TENANT_LAST_NAME);
        driver.findElement(By.id("admin")).clear();
        driver.findElement(By.id("admin")).sendKeys(TEST_DATA_ADMIN_USER_NAME);
        driver.findElement(By.id("admin-password")).clear();
        driver.findElement(By.id("admin-password")).sendKeys(TEST_DATA_PASSWORD);
        driver.findElement(By.id("admin-password-repeat")).clear();
        driver.findElement(By.id("admin-password-repeat")).sendKeys(TEST_DATA_PASSWORD);
        driver.findElement(By.id("admin-email")).clear();
        driver.findElement(By.id("admin-email")).sendKeys(TEST_DATA_EMAIL);
        driver.findElement(By.cssSelector("input.button")).click();
        driver.findElement(By.cssSelector("button[type=\"button\"]")).click();
    }

    public void testCreateAndPublishAPI() throws Exception {
        String loginURL = getPublisherURL(ProductConstant.AM_SERVER_NAME);
        HttpContext httpContext = TestUtil.login(userInfo.getUserName(), userInfo.getPassword(), loginURL);

        assertTrue(TestUtil.addAPI(userInfo.getUserName(), TEST_DATA_API_NAME, TEST_DATA_API_VERSION, httpContext, loginURL));
        Thread.sleep(3000);
        assertTrue(TestUtil.publishAPI(userInfo.getUserName(), TEST_DATA_API_NAME, TEST_DATA_API_VERSION, httpContext, loginURL));

        System.out.println("API Create and Publish test case is completed ");
    }

    @Test(groups = "wso2.am")
    public void testNotificationWhenExternalStoresAvailable() throws Exception {
        WebDriverWait wait = new WebDriverWait(driver, 10);
        // create new tenant
        generateTenant();

        testCreateAndPublishAPI();

        //login to publisher
        driver.get(getPublisherURL(ProductConstant.AM_SERVER_NAME) + "/site/pages/login.jag");
        // wait until load the page
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("pass")).clear();
        driver.findElement(By.id("pass")).sendKeys("admin");
        driver.findElement(By.id("loginButton")).click();

        // wait until load the page
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Browse")));

        driver.findElement(By.linkText("Browse")).click();
        // wait until display APIs and select first API
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".thumbnail > a:nth-child(1)")));
        driver.findElement(By.cssSelector(".thumbnail > a:nth-child(1)")).click();

        // publish to external store
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("External API Stores")));
        driver.findElement(By.linkText("External API Stores")).click();
        driver.findElement(By.id("store0.0")).click();
        driver.findElement(By.id("updateButtonExt")).click();
        // wait until doc link appear in API info page
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("ul.nav:nth-child(2) > li:nth-child(5) > a:nth-child(1)")));

        // change status in lifecycle tab
        driver.findElement(By.cssSelector("#lifecyclesLink")).click();
        new Select(driver.findElement(By.id("editStatus"))).selectByVisibleText(BLOCKED);
        driver.findElement(By.id("updateStateButton")).click();

        // Navigate to life cycle page and check for notification
        driver.findElement(By.linkText("Browse")).click();
        // wait until display APIs and select first API
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".thumbnail > a:nth-child(1)")));
        driver.findElement(By.cssSelector(".thumbnail > a:nth-child(1)")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#lifecyclesLink")));
        driver.findElement(By.cssSelector("#lifecyclesLink")).click();

        // check whether notification is there or not
        Assert.assertTrue(isElementPresent(By.id("removeFromExternalStoresMsg")));
    }

    private boolean isElementPresent(By by) {
        try {
            driver.findElement(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        if (driver != null) {
            driver.quit();
        }
    }
}
