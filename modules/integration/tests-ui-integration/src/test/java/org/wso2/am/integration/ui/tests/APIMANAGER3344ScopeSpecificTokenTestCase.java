/*
*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.am.integration.ui.tests.util.APIMTestConstants;
import org.wso2.am.integration.ui.tests.util.TestUtil;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;

public class APIMANAGER3344ScopeSpecificTokenTestCase extends APIMIntegrationUiTestBase {
    private WebDriver driver;
    private String publisherURL;
    private String storeURL;
    WebDriverWait wait;

    private static final String SUPER_TENANT_DOMAIN_NAME = "carbon.super";
    private static final String API_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";

    private static final Log log =
            LogFactory.getLog(APIMANAGER3344ScopeSpecificTokenTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        super.init();
        driver = BrowserManager.getWebDriver();
        publisherURL = getPublisherURL();
        storeURL = getStoreURL();
        wait = new WebDriverWait(driver, 60);
    }

    @Test(groups = "wso2.am", description = "publish api with scopes defined and generate a token")
    public void testPublishApiWithScopesDefined()
            throws Exception {

        // logging into publisher
        driver.get(publisherURL + "/site/pages/login.jag");
        WebElement userNameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("pass"));
        userNameField.sendKeys(gatewayContext.getContextTenant().getContextUser().getUserName());
        passwordField.sendKeys(gatewayContext.getContextTenant().getContextUser().getPassword());
        driver.findElement(By.id("loginButton")).click();

        //add api details
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add")));
        driver.findElement(By.linkText("Add")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("create-new-api")));
        driver.findElement(By.id("create-new-api")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("designNewAPI")));
        driver.findElement(By.id("designNewAPI")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("name")));
        driver.findElement(By.id("name")).clear();
        driver.findElement(By.id("name")).sendKeys("TwitterAPI");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("context")));
        driver.findElement(By.id("context")).clear();
        driver.findElement(By.id("context")).sendKeys("twitter");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("version")));
        driver.findElement(By.id("version")).clear();
        driver.findElement(By.id("version")).sendKeys("1.0");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("resource_url_pattern")));
        driver.findElement(By.id("resource_url_pattern")).clear();
        driver.findElement(By.id("resource_url_pattern")).sendKeys("tweet");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input.http_verb_select")));
        driver.findElement(By.cssSelector("input.http_verb_select")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("resource_url_pattern")));
        driver.findElement(By.id("add_resource")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("context")));
        driver.findElement(By.id("resource_url_pattern")).clear();
        driver.findElement(By.id("resource_url_pattern")).sendKeys("retweet");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input.http_verb_select")));
        driver.findElement(By.cssSelector("input.http_verb_select")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("add_resource")));
        driver.findElement(By.id("add_resource")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("resource_url_pattern")));
        driver.findElement(By.id("resource_url_pattern")).clear();
        driver.findElement(By.id("resource_url_pattern")).sendKeys("view");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input.http_verb_select")));
        driver.findElement(By.cssSelector("input.http_verb_select")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("add_resource")));
        driver.findElement(By.id("add_resource")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("resource_url_pattern")));
        driver.findElement(By.id("resource_url_pattern")).clear();
        driver.findElement(By.id("resource_url_pattern")).sendKeys("delete");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input.http_verb_select")));
        driver.findElement(By.cssSelector("input.http_verb_select")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("add_resource")));
        driver.findElement(By.id("add_resource")).click();

        //go to implement and select specify inline
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("go_to_implement")));
        driver.findElement(By.id("go_to_implement")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@value='#managed-api']")));
        driver.findElement(By.xpath("//div[@value='#managed-api']")).click();

        //go to manage
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("go_to_manage")));
        driver.findElement(By.id("jsonform-0-elt-production_endpoints")).clear();
        driver.findElement(By.id("jsonform-0-elt-production_endpoints")).sendKeys(API_URL);
        driver.findElement(By.id("go_to_manage")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("publish_api")));
        driver.findElement(By.xpath("//button[@type='button']")).click();
        driver.findElement(By.xpath("//input[@value='Unlimited']")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#layout-base > div.row-fluid")));
        driver.findElement(By.cssSelector("#layout-base > div.row-fluid")).click();

        //define scopes
        defineScope("tweetScope", "Tweet", "admin");
        defineScope("retweetScope", "Retweet", "admin");
        defineScope("deleteScope", "Delete", "reader");

        //assign defined scopes
        driver.findElement(By.linkText("+ Scope")).click();
        new Select(driver.findElement(By.cssSelector("select.input-medium")))
                .selectByVisibleText("Tweet");
        driver.findElement(By.xpath("//button[@type='submit']")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("+ Scope")));

        driver.findElement(By.linkText("+ Scope")).click();
        new Select(driver.findElement(By.cssSelector("select.input-medium")))
                .selectByVisibleText("Retweet");
        driver.findElement(By.xpath("//button[@type='submit']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("(//a[contains(text(),'+ Scope')])[2]")));

        driver.findElement(By.xpath("(//a[contains(text(),'+ Scope')])[2]")).click();
        new Select(driver.findElement(By.cssSelector("select.input-medium")))
                .selectByVisibleText("Delete");
        driver.findElement(By.xpath("//button[@type='submit']")).click();
//		threadWait(1000);

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("publish_api")));
        //publish api
        driver.findElement(By.id("publish_api")).click();
        //wait 15 seconds for API to get published
//		threadWait(15000);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[@href='#lifecycles']")));

        // Go to the Tenant store and click Login
        driver.get(getStoreURL() + "?tenant=" + SUPER_TENANT_DOMAIN_NAME);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Login")));
        driver.findElement(By.linkText("Login")).click();

        // Find and fill Username
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        WebElement usernameEle = driver.findElement(By.id("username"));
        usernameEle.sendKeys(gatewayContext.getContextTenant().getContextUser().getUserName());

        // Find and fill Password
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));

        WebElement passwordEle = driver.findElement(By.id("password"));
        passwordEle.sendKeys(gatewayContext.getContextTenant().getContextUser().getPassword());

        // find Login button and click on it.
        driver.findElement(By.id("loginBtn")).click();

        //go to my applications and add an application
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[@class='link-myapplications']")));
        driver.findElement(By.xpath("//a[@class='link-myapplications']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("application-add-button")));
        driver.findElement(By.id("application-name")).clear();
        driver.findElement(By.id("application-name")).sendKeys("app01");
        driver.findElement(By.id("application-add-button")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[@class='link-home']")));
        driver.findElement(By.xpath("//a[@class='link-home']")).click();
        long loopMaxTime = APIMTestConstants.MAX_LOOP_WAIT_TIME_MILLISECONDS;
        long startTime = System.currentTimeMillis();
        long nowTime = startTime;
        while ((!driver.getPageSource().contains("TwitterAPI")) && (nowTime - startTime) < loopMaxTime) {
            driver.findElement(By.linkText("APIs")).click();
            threadWait(500);
            nowTime = System.currentTimeMillis();
        }
//        driver.findElement(By.xpath("//input[@name='query']")).sendKeys("Twitter");
//        driver.findElement(By.xpath("//button[@class='btn btn-primary search-button']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[contains(.,'TwitterAPI-1.0')]")));
        driver.findElement(By.xpath("//a[contains(.,'TwitterAPI-1.0')]")).click();
        new Select(driver.findElement(By.id("application-list"))).selectByVisibleText("app01");
        driver.findElement(By.id("subscribe-button")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Go to My Subscriptions")));
        //go to my subscriptions and generate key using defined scopes
        driver.findElement(By.linkText("Go to My Subscriptions")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Select Scopes")));
        driver.findElement(By.linkText("Select Scopes")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("retweetScope")));
        driver.findElement(By.id("retweetScope")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("tweetScope")));
        driver.findElement(By.id("tweetScope")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("deleteScope")));
        driver.findElement(By.id("deleteScope")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("scopeSelectButtonPop")));
        driver.findElement(By.id("scopeSelectButtonPop")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//button[@class='app-key-generate-button btn btn-primary btn-generatekeys']")));
        driver.findElement(By.xpath("//button[@class='app-key-generate-button btn btn-primary btn-generatekeys']")).click();

        //wait 5 seconds for token to get generated
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("prodAccessScope")));


        //get the generated scope
        WebElement scope = driver.findElement(By.id("prodAccessScope"));
        String finalScope = scope.getText();

		/*
            out of the three scopes defined, admin only has privilages to tweetScope and
			retweetScope only. The test is success if the generated key's scope is only within
			those two scopes.
		 */
        if (!(finalScope.equals("Retweet, Tweet") || finalScope.equals("Tweet, Retweet"))) {
            throw new Exception("Generated scope doesn't match");
        }

    }

    private void defineScope(String scopeKey, String scopeName, String roles) {
        driver.findElement(By.id("define_scopes")).click();
        driver.findElement(By.id("scopeKey")).clear();
        driver.findElement(By.id("scopeKey")).sendKeys(scopeKey);
        driver.findElement(By.id("scopeName")).clear();
        driver.findElement(By.id("scopeName")).sendKeys(scopeName);
        driver.findElement(By.id("scopeRoles")).clear();
        driver.findElement(By.id("scopeRoles")).sendKeys(roles);
        driver.findElement(By.id("scope_submit")).click();
        threadWait(1000);
    }

    private void threadWait(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            log.warn("Interrupted Exception while scope specific token test " + e);
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        TestUtil.cleanUp(gatewayContext.getContextTenant().getContextUser().getUserName(),
                         gatewayContext.getContextTenant().getContextUser().getPassword(),
                         storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());
        driver.quit();
    }

}