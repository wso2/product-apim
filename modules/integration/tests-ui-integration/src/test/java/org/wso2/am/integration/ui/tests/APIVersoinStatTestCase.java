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

package org.wso2.am.integration.ui.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.wso2.carbon.automation.extensions.selenium.BrowserManager;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertTrue;

public class APIVersoinStatTestCase extends APIMIntegrationUiTestBase {
    private WebDriver driver;

    private static final String TEST_DATA_API_NAME = "SubAvailabilityTestAPI";
    private static final String TEST_DATA_API_VERSION = "1.0.0";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        driver = BrowserManager.getWebDriver();
        driver.get(getLoginURL());
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.PLATFORM})
    @Test(groups = "wso2.am", description = "verify Bam api stats visibility")
    public void testVersionStats() throws Exception {

        driver.get(getPublisherURL() );
        WebDriverWait wait = new WebDriverWait(driver, 30);
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("loginButton")));

        //Find and fill Username
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        WebElement usernameEle = driver.findElement(By.id("username"));
        usernameEle.sendKeys("admin");

        //Find and fill Password
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("pass")));
        WebElement passwordEle = driver.findElement(By.id("pass"));
        passwordEle.sendKeys("admin");

        //find Login button and click on it.
        driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
        driver.findElement(By.id("loginButton")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.className("versionTxt")));
        // Go to test API
        driver.navigate().to(getPublisherURL() + "/info?name=" + TEST_DATA_API_NAME
                             + "&version=" + TEST_DATA_API_VERSION + "&provider=" +
                gatewayContext.getContextTenant().getContextUser().getUserName());

        //click on versions tab
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("versionsLink")));
        driver.findElement(By.id("versionsLink")).click();

        //wating for data to load
        driver.manage().timeouts().implicitlyWait(6, TimeUnit.SECONDS);

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("versionChart")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("versionChart")));

        assertTrue(driver.findElement(By.xpath("//div[@id=\"versionChart\"]/span")).getText().contains("No Data Found"));
        assertTrue(driver.findElement(By.xpath("//div[@id=\"versionUserChart\"]/span")).getText().contains("No Data Found"));

    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        if (driver != null) {
            driver.quit();
        }
    }
}
