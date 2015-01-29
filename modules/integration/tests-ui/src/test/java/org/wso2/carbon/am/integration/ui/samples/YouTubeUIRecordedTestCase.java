/*
 *
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.carbon.am.integration.ui.samples;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.am.integration.ui.AMIntegrationUiTestBase;
import org.wso2.carbon.am.integration.ui.util.APIMTestConstants;
import org.wso2.carbon.automation.core.BrowserManager;
import org.wso2.carbon.automation.core.ProductConstant;

/**
 * UI Test case for the Youtube API sample.
 * This test case was created using Selenium Record ui method.
 */
public class YouTubeUIRecordedTestCase extends AMIntegrationUiTestBase {
    private WebDriver driver;


    private static final String API_NAME = "YoutubeFeeds";
    private static final String API_CONTEXT = "/youtube";
    private static final String API_VERSION = "1.0.0";
    private static final String API_DESCRIPTION = "Youtube Live Feeds";
    private static final String API_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private static final String API_METHOD = "/most_popular";
    private static final String[] TAG_NAMES = new String[]{"youtube", "gdata", "multimedia"};
    private String accessToken;
    private String accessHTTPURL;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        super.init();
        driver = BrowserManager.getWebDriver();
        driver.get(getPublisherURL(ProductConstant.AM_SERVER_NAME));

    }


    @Test(groups = "wso2.greg", description = "verify API publish")
    public void testPublishAPI() throws Exception {

        WebDriverWait wait = new WebDriverWait(driver, 60);
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys(userInfo.getUserName());
        driver.findElement(By.id("pass")).clear();
        driver.findElement(By.id("pass")).sendKeys(userInfo.getPassword());
        driver.findElement(By.id("loginButton")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add")));
        driver.findElement(By.linkText("Add")).click();

        driver.findElement(By.id("name")).clear();
        driver.findElement(By.id("name")).sendKeys(API_NAME);
        driver.findElement(By.id("context")).clear();
        driver.findElement(By.id("context")).sendKeys(API_CONTEXT);
        driver.findElement(By.id("version")).clear();
        driver.findElement(By.id("version")).sendKeys(API_VERSION);
        driver.findElement(By.id("description")).clear();
        driver.findElement(By.id("description")).sendKeys(API_DESCRIPTION);

        for (String tagName : TAG_NAMES) {
            driver.findElement(By.xpath("//input[@size='8']")).sendKeys(tagName + "\n");
        }

        driver.findElement(By.id("resource_url_pattern")).clear();
        driver.findElement(By.id("resource_url_pattern")).sendKeys("*");
        driver.findElement(By.id("inputResource")).clear();
        driver.findElement(By.id("inputResource")).sendKeys("default");
        driver.findElement(By.cssSelector("input.http_verb_select")).click();
        driver.findElement(By.id("add_resource")).click();
        driver.findElement(By.id("go_to_implement")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("go_to_manage")));
        driver.findElement(By.id("jsonform-0-elt-production_endpoints")).clear();
        driver.findElement(By.id("jsonform-0-elt-production_endpoints")).sendKeys(API_URL);
        driver.findElement(By.id("go_to_manage")).click();


        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("publish_api")));
        driver.findElement(By.xpath("//button[@type='button']")).click();
        driver.findElement(By.xpath("//input[@value='Gold']")).click();
        driver.findElement(By.id("publish_api")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("apiView")));
        Assert.assertTrue("Youtube Live Feeds".equals(driver.findElement(By.id("apiView")).getText()), "Youtube Live Feeds");

        driver.findElement(By.id("userMenu")).click();
        driver.findElement(By.cssSelector("button.btn.btn-danger")).click();


    }


    @Test(groups = "wso2.greg", dependsOnMethods = {"testPublishAPI"}, description = "verify API subscribe")
    public void testSubscribeAPI() throws Exception {
        WebDriverWait wait = new WebDriverWait(driver, 60);
        driver.get(getStoreURL(ProductConstant.AM_SERVER_NAME));
        driver.findElement(By.id("login-link")).click();
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys(userInfo.getUserName());
        driver.findElement(By.id("password")).clear();
        driver.findElement(By.id("password")).sendKeys(userInfo.getPassword());
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginBtn")));
        driver.findElement(By.id("loginBtn")).click();


        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("APIs")));
        long loopMaxTime = APIMTestConstants.MAX_LOOP_WAIT_TIME_MILLISECONDS;
        long startTime = System.currentTimeMillis();
        long nowTime = startTime;
        while ((!driver.getPageSource().contains("YoutubeFeeds-1.0.0")) && (nowTime - startTime) < loopMaxTime) {
            driver.findElement(By.linkText("APIs")).click();
            Thread.sleep(1000);
            nowTime = System.currentTimeMillis();
        }

        driver.findElement(By.linkText("YoutubeFeeds-1.0.0")).click();
        accessHTTPURL = driver.findElement(By.xpath("//div[@id='overview']/div")).getText();
        driver.findElement(By.id("subscribe-button")).click();
        driver.findElement(By.linkText("Go to My Subscriptions")).click();

        WebElement generateButton = driver.findElement(By.xpath("//div/div/div/div/button"));
        String genButtonText = generateButton.getText();
        if (genButtonText.equals("Generate")) {
            generateButton.click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span.accessTokenDisplayPro.keyValues")));
            accessToken = driver.findElement(By.cssSelector("span.accessTokenDisplayPro.keyValues")).getText();
        } else {
            String currentValue = driver.findElement(By.className("accessTokenDisplayPro")).getText();
            String newValue;
            startTime = System.currentTimeMillis();

            do {
                generateButton.click();
                Thread.sleep(1000);
                newValue = driver.findElement(By.className("accessTokenDisplayPro")).getText();
                nowTime = System.currentTimeMillis();
            } while (currentValue.equals(newValue) && nowTime - startTime < loopMaxTime);
            accessToken = newValue;
        }

    }


    @Test(groups = "wso2.greg", dependsOnMethods = {"testSubscribeAPI"}, description = "verify subscribed api using RESTClient")
    public void testRestClient() throws Exception {
        WebDriverWait wait = new WebDriverWait(driver, 60);
        driver.findElement(By.linkText("Tools")).click();
        driver.findElement(By.linkText("RESTClient")).click();

        driver.findElement(By.linkText("Form")).click();

        driver.findElement(By.id("req_url")).clear();
        driver.findElement(By.id("req_url")).sendKeys(accessHTTPURL + API_METHOD);
        driver.findElement(By.cssSelector("input.input-large.key")).clear();
        driver.findElement(By.cssSelector("input.input-large.key")).sendKeys("Authorization");
        driver.findElement(By.cssSelector("input.input-xxlarge.value")).clear();
        driver.findElement(By.cssSelector("input.input-xxlarge.value")).sendKeys("Bearer  " + accessToken);
        driver.findElement(By.id("sendBtn")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Response Body")));

        Assert.assertTrue(driver.findElement(By.id("responseDivContent")).getText().contains("Twitter"), "Twitter");
        driver.findElement(By.cssSelector("a.link-to-user.dropdown-toggle")).click();
        driver.findElement(By.id("logout-link")).click();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        driver.quit();
    }


}
