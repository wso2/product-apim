/*
*Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.am.integration.ui.util.TestUtil;
import org.wso2.carbon.automation.core.BrowserManager;
import org.wso2.carbon.automation.core.ProductConstant;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.testng.Assert.assertTrue;

public class APIMANAGER2957EpochTimeTestCase extends AMIntegrationUiTestBase {
    private WebDriver driver;
    private boolean acceptNextAlert = true;
    private StringBuffer verificationErrors = new StringBuffer();

    private static final String TEST_DATA_API_NAME = "EpochAPI";
    private static final String TEST_DATA_API_VERSION = "1.0.0";
    private static final String TEST_DATA_API_TIER = "Unlimited";
    private static final String TEST_DATA_APP_NAME = "DefaultApplication";

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init(0);
        driver = BrowserManager.getWebDriver();
        driver.get(getLoginURL(ProductConstant.AM_SERVER_NAME));

        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
    }

    @Test(groups = "wso2.am", description = "This method adds and publishes the Test API in carbon.super store")
    public void testCreatePublishAndSubscribeAPI() throws Exception {

        // login to publisher and create new API
        String publisherURL = getPublisherURL(ProductConstant.AM_SERVER_NAME);
        HttpContext httpContext = TestUtil.login(userInfo.getUserName(), userInfo.getPassword(), publisherURL);
        assertTrue(TestUtil.addAPI(userInfo.getUserName(), TEST_DATA_API_NAME, TEST_DATA_API_VERSION, httpContext, publisherURL));
        assertTrue(TestUtil.publishAPI(userInfo.getUserName(), TEST_DATA_API_NAME, TEST_DATA_API_VERSION, httpContext, publisherURL));

        // login to store and subscribe to API
        String storeURL = getStoreURL(ProductConstant.AM_SERVER_NAME);
        httpContext = TestUtil.login(userInfo.getUserName(), userInfo.getPassword(), storeURL);
        assertTrue(TestUtil.addSubscription(userInfo.getUserName(), TEST_DATA_API_NAME, TEST_DATA_API_VERSION, TEST_DATA_API_TIER, TEST_DATA_APP_NAME, httpContext, storeURL));

        System.out.println("API Create and Publish test case is completed ");
    }

    @Test(groups = "wso2.am", description = "Check time conversion from epoch to human readable version", dependsOnMethods = "testCreatePublishAndSubscribeAPI")
    public void testFIDELITYDEV86EpochTimeTestCase() throws Exception {
        // login to publisher
        driver.get(getPublisherURL(ProductConstant.AM_SERVER_NAME) + "/site/pages/login.jag");
        WebDriverWait wait = new WebDriverWait(driver, 30);
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("username")));
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys(userInfo.getUserName());
        driver.findElement(By.id("pass")).clear();
        driver.findElement(By.id("pass")).sendKeys(userInfo.getPassword());
        driver.findElement(By.id("loginButton")).click();
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        // wait until load the page
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.linkText("Browse")));
        // Click browse API and wait
        driver.findElement(By.linkText("Browse")).click();
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        // clieck user link and wait
        driver.findElement(By.id("noOfUsers")).click();
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        // validate date format
        try {
            // Regex : check is there at least 2 characters such as Mon, AM.
            Assert.assertTrue(Pattern.compile(".*[a-zA-Z]{2}.*").matcher(driver.findElement(By.xpath("//tbody[@id='userList']/tr[1]/td[2]")).getText()).find());
        } catch (Error e) {
            verificationErrors.append(e.toString());
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        if (driver != null) {
            driver.quit();
        }
        String verificationErrorString = verificationErrors.toString();
        if (!"".equals(verificationErrorString)) {
            Assert.fail(verificationErrorString);
        }
    }
}
