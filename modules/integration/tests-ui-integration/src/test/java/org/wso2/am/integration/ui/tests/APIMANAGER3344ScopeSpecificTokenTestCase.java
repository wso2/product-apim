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
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;

public class APIMANAGER3344ScopeSpecificTokenTestCase extends AMIntegrationUiTestBase {
	private WebDriver driver;
	private String publisherURL;
	private String storeURL;
	WebDriverWait wait;

	private static final String SUPER_TENANT_DOMAIN_NAME = "carbon.super";

	private static final Log log =
			LogFactory.getLog(APIMANAGER3344ScopeSpecificTokenTestCase.class);

	@BeforeClass(alwaysRun = true) public void setUp() throws Exception {
		super.init();
		driver = BrowserManager.getWebDriver();
		publisherURL = getPublisherURL();
		storeURL = getStoreURL();
		wait = new WebDriverWait(driver, 60);
	}

	@Test(groups = "wso2.am", description = "publish api with scopes defined and generate a token") public void testPublishApiWithScopesDefined()
			throws Exception {

		// logging into publisher
		driver.get(publisherURL + "/site/pages/login.jag");
		WebElement userNameField = driver.findElement(By.id("username"));
		WebElement passwordField = driver.findElement(By.id("pass"));
		userNameField.sendKeys(userInfo.getUserName());
		passwordField.sendKeys(userInfo.getPassword());
		driver.findElement(By.id("loginButton")).click();

		//add api details
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add")));
		driver.findElement(By.linkText("Add")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("name")));
		driver.findElement(By.id("name")).clear();
		driver.findElement(By.id("name")).sendKeys("Twitter");
		driver.findElement(By.id("context")).clear();
		driver.findElement(By.id("context")).sendKeys("twitter");
		driver.findElement(By.id("version")).clear();
		driver.findElement(By.id("version")).sendKeys("1.0");
		driver.findElement(By.id("resource_url_pattern")).clear();
		driver.findElement(By.id("resource_url_pattern")).sendKeys("tweet");
		driver.findElement(By.cssSelector("input.http_verb_select")).click();
		driver.findElement(By.id("add_resource")).click();
		driver.findElement(By.id("resource_url_pattern")).clear();
		driver.findElement(By.id("resource_url_pattern")).sendKeys("retweet");
		driver.findElement(By.cssSelector("input.http_verb_select")).click();
		driver.findElement(By.id("add_resource")).click();
		driver.findElement(By.id("resource_url_pattern")).clear();
		driver.findElement(By.id("resource_url_pattern")).sendKeys("view");
		driver.findElement(By.cssSelector("input.http_verb_select")).click();
		driver.findElement(By.id("add_resource")).click();
		driver.findElement(By.id("resource_url_pattern")).clear();
		driver.findElement(By.id("resource_url_pattern")).sendKeys("delete");
		driver.findElement(By.cssSelector("input.http_verb_select")).click();
		driver.findElement(By.id("add_resource")).click();

		//go to implement and select specify inline
		driver.findElement(By.id("go_to_implement")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(
				By.xpath("(//input[@name='implementation_methods'])[2]")));
		driver.findElement(By.xpath("(//input[@name='implementation_methods'])[2]")).click();

		//go to manage
		driver.findElement(By.id("go_to_manage")).click();
		wait.until(ExpectedConditions
				           .visibilityOfElementLocated(By.xpath("//button[@type='button']")));
		driver.findElement(By.xpath("//button[@type='button']")).click();
		driver.findElement(By.xpath("//input[@value='Unlimited']")).click();
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
		threadWait(1000);

		driver.findElement(By.linkText("+ Scope")).click();
		new Select(driver.findElement(By.cssSelector("select.input-medium")))
				.selectByVisibleText("Retweet");
		driver.findElement(By.xpath("//button[@type='submit']")).click();
		threadWait(1000);

		driver.findElement(By.xpath("(//a[contains(text(),'+ Scope')])[2]")).click();
		new Select(driver.findElement(By.cssSelector("select.input-medium")))
				.selectByVisibleText("Delete");
		driver.findElement(By.xpath("//button[@type='submit']")).click();
		threadWait(1000);

		//publish api
		driver.findElement(By.id("publish_api")).click();
		//wait 15 seconds for API to get published
		threadWait(15000);

		// Go to the Tenant store and click Login
		driver.get(getStoreURL() + "?tenant=" + SUPER_TENANT_DOMAIN_NAME);
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Login")));
		driver.findElement(By.linkText("Login")).click();

		// Find and fill Username
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
		WebElement usernameEle = driver.findElement(By.id("username"));
		usernameEle.sendKeys(userInfo.getUserName());

		// Find and fill Password
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));

		WebElement passwordEle = driver.findElement(By.id("password"));
		passwordEle.sendKeys(userInfo.getPassword());

		// find Login button and click on it.
		driver.findElement(By.id("loginBtn")).click();
		threadWait(1000);

		//go to my applications and add an application
		driver.findElement(By.linkText("My Applications")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("My Applications")));
		driver.findElement(By.id("application-name")).clear();
		driver.findElement(By.id("application-name")).sendKeys("app01");
		driver.findElement(By.id("application-add-button")).click();
		threadWait(1000);

		//go to created API and subscribe
		driver.findElement(By.linkText("APIs")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Twitter")));
		driver.findElement(By.linkText("Twitter")).click();
		new Select(driver.findElement(By.id("application-list"))).selectByVisibleText("app01");
		driver.findElement(By.id("subscribe-button")).click();
		threadWait(1000);

		//go to my subscriptions and generate key using defined scopes
		driver.findElement(By.linkText("Go to My Subscriptions")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Select Scopes")));
		driver.findElement(By.linkText("Select Scopes")).click();
		threadWait(1000);
		driver.findElement(By.id("retweetScope")).click();
		threadWait(1000);
		driver.findElement(By.id("tweetScope")).click();
		threadWait(1000);
		driver.findElement(By.id("deleteScope")).click();
		threadWait(1000);
		driver.findElement(By.id("scopeSelectButtonPop")).click();
		threadWait(1000);
		driver.findElement(By.xpath(
				"//button[@onclick=\"jagg.sessionAwareJS({redirect:'/site/pages/subscriptions.jag'})\"]"))
		      .click();
		//wait 5 seconds for token to get generated
		threadWait(5000);

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

	@AfterClass(alwaysRun = true) public void tearDown() throws Exception {
		driver.quit();
	}

}