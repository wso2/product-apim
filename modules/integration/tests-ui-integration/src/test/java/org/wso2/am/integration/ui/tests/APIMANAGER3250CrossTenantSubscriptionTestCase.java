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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

/**
 * In order to run this test case, there needs to mount registry in API Manager
 */
public class APIMANAGER3250CrossTenantSubscriptionTestCase extends AMIntegrationUiTestBase {

    public static final String PUBLISHED = "PUBLISHED";
    //public static final String AVAILABLE_TO_ALL_TENANTS = "Available to all the Tenants";
    public static final String DEFAULT_APPLICATION = "DefaultApplication";
    private WebDriver driver;

    private static final Log log = LogFactory.getLog(APIMANAGER3250CrossTenantSubscriptionTestCase.class);

    private String TEST_DATA_TENANT_FIRST_NAME = "admin",
            TEST_DATA_TENANT_LAST_NAME = "admin",
            TEST_DATA_ADMIN_USER_NAME = "admin",
            TEST_DATA_PASSWORD = "123456",
            TEST_DATA_API_NAME = "testAPI",
            TEST_DATA_API_END_POINT = "http://localhost:8080/api",
            TEST_DATA_API_VERSION = "1.0.0";

    @BeforeClass(alwaysRun = true)
    protected void init() throws Exception {
        super.init();
        driver = BrowserManager.getWebDriver();
    }


    public void generateTenant(String postfix) throws Exception {

        WebDriverWait wait = new WebDriverWait(driver, 10);
        // wait until load the page
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#menu-panel-button3 > span")));

        driver.findElement(By.cssSelector("#menu-panel-button3 > span")).click();
        driver.findElement(By.linkText("Add New Tenant")).click();
        // wait until load the page
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("domain")));

        driver.findElement(By.id("domain")).clear();
        driver.findElement(By.id("domain")).sendKeys("test" + postfix + ".com");
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
        driver.findElement(By.id("admin-email")).sendKeys("admin@test" + postfix + ".com");
        driver.findElement(By.cssSelector("input.button")).click();
        driver.findElement(By.cssSelector("button[type=\"button\"]")).click();
    }

    @Test(groups = "wso2.am")
    public void checkCrossTenantSubscription() throws Exception {

        WebDriverWait wait = new WebDriverWait(driver, 10);


        driver.get(getLoginURL());

        // wait until load the page
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("txtUserName")));

        driver.findElement(By.id("txtUserName")).clear();
        driver.findElement(By.id("txtUserName")).sendKeys("admin");
        driver.findElement(By.id("txtPassword")).clear();
        driver.findElement(By.id("txtPassword")).sendKeys("admin");
        driver.findElement(By.cssSelector("input.button")).click();
        // create two tenant
        generateTenant("1");
        generateTenant("2");


        //login to publisher
        driver.get(getPublisherURL() + "/site/pages/login.jag");
        // wait until load the page
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys("admin@test1.com");
        driver.findElement(By.id("pass")).clear();
        driver.findElement(By.id("pass")).sendKeys(TEST_DATA_PASSWORD);
        driver.findElement(By.id("loginButton")).click();
        // wait until load the page
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add")));
        // create new API
        driver.findElement(By.linkText("Add")).click();
        driver.findElement(By.id("name")).clear();
        driver.findElement(By.id("name")).sendKeys(TEST_DATA_API_NAME);
        driver.findElement(By.id("context")).clear();
        driver.findElement(By.id("context")).sendKeys(TEST_DATA_API_NAME);
        driver.findElement(By.id("version")).clear();
        driver.findElement(By.id("version")).sendKeys(TEST_DATA_API_VERSION);

        driver.findElement(By.id("go_to_implement")).click();

        driver.findElement(By.cssSelector("a.btn:nth-child(4)")).click();


        driver.findElement(By.id("jsonform-0-elt-production_endpoints")).clear();
        driver.findElement(By.id("jsonform-0-elt-production_endpoints")).sendKeys(TEST_DATA_API_END_POINT);

        driver.findElement(By.id("go_to_manage")).click();

        driver.findElement(By.cssSelector("button.multiselect")).click();
        driver.findElement(By.cssSelector(
                ".multiselect-container > li:nth-child(2) > a:nth-child(1) > label:nth-child(1) > input:nth-child(1)"))
                .click();
        new Select(driver.findElement(By.id("subscriptions"))).selectByValue("all_tenants");
        driver.findElement(By.id("publish_api")).click();

        //check whether the publish is success
        driver.findElement(By.id("lifecyclesLink")).click();

        //browse store
        driver.get(getStoreURL() + "?tenant=test2.com");

        log.info("Started to Login to Store");
        driver.findElement(By.id("login-link")).click();
        WebElement userNameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));

        userNameField.sendKeys("admin@test2.com");
        passwordField.sendKeys(TEST_DATA_PASSWORD);
        driver.findElement(By.id("loginBtn")).click();

        //check the presence of admin name in store home page to verify the user has logged to store.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("admin@test2.com")));
        log.info("Logging to store is successful");

        driver.get(getStoreURL() + "?tenant=test1.com");

        //wait for few seconds and refresh the store since it will take little time to appear the published APIs in store
        Thread.sleep(20000);
        driver.navigate().refresh();


        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".title")));

        driver.findElement(By.cssSelector(".title")).click();

        new Select(driver.findElement(By.id("application-list"))).selectByVisibleText(DEFAULT_APPLICATION);

        driver.findElement(By.id("subscribe-button")).click();

        //restart the server to unload the tenants
        ServerConfigurationManager serverConfigurationManager = new ServerConfigurationManager(apimContext);
        serverConfigurationManager.restartGracefully();

        //browse store
        driver.get(getStoreURL() + "?tenant=test2.com");

        log.info("Started to Login to Store");
        driver.findElement(By.id("login-link")).click();
        WebElement userNameField1 = driver.findElement(By.id("username"));
        WebElement passwordField1 = driver.findElement(By.id("password"));

        userNameField1.sendKeys("admin@test2.com");
        passwordField1.sendKeys(TEST_DATA_PASSWORD);
        driver.findElement(By.id("loginBtn")).click();

        //check the presence of admin name in store home page to verify the user has logged to store.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("admin@test2.com")));
        log.info("Logging to store is successful");

        //go to my subscription page
        driver.get(getStoreURL() + "?tenant=test1.com");
        driver.findElement(By.cssSelector(".link-mysubscriptions")).click();


        //wait until subscribed APIs visible in UI
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".content-data > div:nth-child(8) > h3:nth-child(2)")));

    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        if (driver != null) {
            driver.quit();
        }
    }

}
