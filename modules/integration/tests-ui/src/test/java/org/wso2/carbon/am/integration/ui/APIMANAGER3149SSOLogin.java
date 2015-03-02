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

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.core.BrowserManager;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.SetEnvironment;

/*
  This test should be run with following configurations
  > Configure AM and IS to use in SSO mode
  > Create a new tenant in IS side
  > Create new user role and user in IS side
  > User should be able to login to publisher just after user creation in IS side
 */
public class APIMANAGER3149SSOLogin extends AMIntegrationUiTestBase {
    private WebDriver driver;

    @BeforeClass(alwaysRun = true)
    protected void init() throws Exception {
        super.init(0);
        driver = BrowserManager.getWebDriver();
    }

    @Test(groups = "wso2.am")
    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.platform_user})
    public void loginToPublisher() throws Exception {
        WebDriverWait wait = new WebDriverWait(driver, 10);
        //login to publisher
        driver.get(getPublisherURL(ProductConstant.AM_SERVER_NAME) + "/site/pages/login.jag");
        // wait until load the page
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys(userInfo.getUserName());

        // this is the password field in IS sso login page
        if(driver.findElement(By.id("password"))!=null){
            driver.findElement(By.id("password")).clear();
            driver.findElement(By.id("password")).sendKeys(userInfo.getPassword());
        }else{
            driver.findElement(By.id("pass")).clear();
            driver.findElement(By.id("pass")).sendKeys(userInfo.getPassword());
        }

        driver.findElement(By.cssSelector(".btn")).click();

        // wait until load the page
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add")));
        // if element present, test it successful
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        driver.quit();
    }
}
