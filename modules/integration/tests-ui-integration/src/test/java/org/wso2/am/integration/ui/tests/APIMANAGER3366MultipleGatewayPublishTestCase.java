

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

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class APIMANAGER3366MultipleGatewayPublishTestCase extends AMIntegrationUiTestBase {
	private WebDriver driver;
	private static final String API_DESCRIPTION = "Publish into Gateways";
	private static final String API_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
	private static final String API_METHOD = "/most_popular";
	private String accessHTTPURL;
	WebDriverWait wait;
	String carbonLogFilePath = CarbonUtils.getCarbonLogsPath() + "/wso2carbon.log";

	@BeforeClass(alwaysRun = true)
	public void setUp() throws Exception {
		super.init();
		driver = BrowserManager.getWebDriver();
		driver.get(getPublisherURL());
		wait = new WebDriverWait(driver, 60);

	}

	@Test(groups = "wso2.am", description = "publish api without environment tab selection")
	public void testPublishApiWithOutEnvironmentTabSelection() throws Exception {

		driver.findElement(By.id("username")).clear();
		driver.findElement(By.id("username")).sendKeys(userInfo.getUserName());
		driver.findElement(By.id("pass")).clear();
		driver.findElement(By.id("pass")).sendKeys(userInfo.getPassword());
		driver.findElement(By.id("loginButton")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add")));
		driver.findElement(By.linkText("Add")).click();
		driver.findElement(By.id("name")).clear();
		driver.findElement(By.id("name")).sendKeys("publishWithEnvironments1");
		driver.findElement(By.id("context")).clear();
		driver.findElement(By.id("context")).sendKeys("publishWithEnvironments1");
		driver.findElement(By.id("version")).clear();
		driver.findElement(By.id("version")).sendKeys("1.0.0");
		driver.findElement(By.id("description")).clear();
		driver.findElement(By.id("description")).sendKeys(API_DESCRIPTION);
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
		Thread.sleep(2000);
		Assert.assertTrue(isAPIPublished("publishWithEnvironments1", "1.0.0"),
		                  "API successfully published api without environment tab selection");
		driver.findElement(By.id("userMenu")).click();
		driver.findElement(By.cssSelector("button.btn.btn-danger")).click();

	}

	@Test(groups = "wso2.am", description = "published with select environments section")
	public void testPublishApiWithEnvironmentTabSelection() throws Exception {
		driver.findElement(By.id("username")).clear();
		driver.findElement(By.id("username")).sendKeys(userInfo.getUserName());
		driver.findElement(By.id("pass")).clear();
		driver.findElement(By.id("pass")).sendKeys(userInfo.getPassword());
		driver.findElement(By.id("loginButton")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add")));
		driver.findElement(By.linkText("Add")).click();
		driver.findElement(By.id("name")).clear();
		driver.findElement(By.id("name")).sendKeys("publishWithEnvironments2");
		driver.findElement(By.id("context")).clear();
		driver.findElement(By.id("context")).sendKeys("publishWithEnvironments2");
		driver.findElement(By.id("version")).clear();
		driver.findElement(By.id("version")).sendKeys("1.0.0");
		driver.findElement(By.id("description")).clear();
		driver.findElement(By.id("description")).sendKeys(API_DESCRIPTION);
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
		driver.findElement(By.cssSelector("legend.legend-with-hidden-info.js_hidden_section_title")).click();
		WebElement checkBox = driver.findElement(By.cssSelector("input.env"));
		if (!checkBox.isSelected()) {
			checkBox.click();
		}
		driver.findElement(By.id("publish_api")).click();
		Thread.sleep(2000);
		Assert.assertTrue(isAPIPublished("publishWithEnvironments2", "1.0.0"),
		                  "API is Successfully published with select environments section");
		driver.findElement(By.id("userMenu")).click();
		driver.findElement(By.cssSelector("button.btn.btn-danger")).click();
	}

	@Test(groups = "wso2.am", description = "published with deselect environment")
	public void testPublishApiWithDeSelectEnvironment() throws Exception {
		driver.findElement(By.id("username")).clear();
		driver.findElement(By.id("username")).sendKeys(userInfo.getUserName());
		driver.findElement(By.id("pass")).clear();
		driver.findElement(By.id("pass")).sendKeys(userInfo.getPassword());
		driver.findElement(By.id("loginButton")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add")));
		driver.findElement(By.linkText("Add")).click();
		driver.findElement(By.id("name")).clear();
		driver.findElement(By.id("name")).sendKeys("publishWithEnvironments3");
		driver.findElement(By.id("context")).clear();
		driver.findElement(By.id("context")).sendKeys("publishWithEnvironments3");
		driver.findElement(By.id("version")).clear();
		driver.findElement(By.id("version")).sendKeys("1.0.0");
		driver.findElement(By.id("description")).clear();
		driver.findElement(By.id("description")).sendKeys(API_DESCRIPTION);
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
		driver.findElement(By.cssSelector("legend.legend-with-hidden-info.js_hidden_section_title")).click();
		WebElement checkBox = driver.findElement(By.cssSelector("input.env"));
		if (checkBox.isSelected()) {
			checkBox.click();
		}
		driver.findElement(By.id("publish_api")).click();
		Thread.sleep(2000);
		Assert.assertTrue(!isAPIPublished("publishWithEnvironments3", "1.0.0"),
		                  "API is Successfully published with de select environment in environment section");
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Browse")));
		driver.findElement(By.linkText("Browse")).click();
		driver.findElement(By.linkText("publishWithEnvironments3")).click();
		driver.findElement(By.linkText("Edit")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("go_to_implement")));
		Assert.assertTrue(!elementIsVisible(By.id("editAPIWarn")),
		                  "Warning message for API without subscriptions is not Shown Successfully");
		driver.findElement(By.id("userMenu")).click();
		driver.findElement(By.cssSelector("button.btn.btn-danger")).click();
	}

	@Test(groups = "wso2.am", description = "Warning Message when Publish API Having subscribers", dependsOnMethods = {
			"testPublishApiWithOutEnvironmentTabSelection" })
	public void testWarningMessageWhenPublishAPIHavingSubscribers() throws Exception {
		driver.get(getStoreURL() + "/?tenant=carbon.super");
		driver.findElement(By.id("login-link")).click();
		driver.findElement(By.id("username")).clear();
		driver.findElement(By.id("username")).sendKeys("admin");
		driver.findElement(By.id("password")).clear();
		driver.findElement(By.id("password")).sendKeys("admin");
		driver.findElement(By.id("loginBtn")).click();
		while (true) {
			driver.findElement(By.linkText("APIs")).click();
			Thread.sleep(3000);
			boolean apiElement = elementIsVisible(By.linkText("publishWithEnvironments1-1.0.0"));
			if (apiElement) {
				break;
			}
		}
		driver.findElement(By.linkText("publishWithEnvironments1-1.0.0")).click();
		driver.findElement(By.id("subscribe-button")).click();
		driver.findElement(By.linkText("Stay on this page")).click();
		driver.findElement(By.linkText("admin")).click();
		driver.findElement(By.id("logout-link")).click();
		driver.get(getPublisherURL());
		driver.findElement(By.id("username")).clear();
		driver.findElement(By.id("username")).sendKeys(userInfo.getUserName());
		driver.findElement(By.id("pass")).clear();
		driver.findElement(By.id("pass")).sendKeys(userInfo.getPassword());
		driver.findElement(By.id("loginButton")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Browse")));
		driver.findElement(By.linkText("Browse")).click();
		driver.findElement(By.linkText("publishWithEnvironments1")).click();
		driver.findElement(By.linkText("Edit")).click();
		WebElement warning = driver.findElement(By.id("editAPIWarn"));
		Assert.assertTrue(warning.isDisplayed(), "Warning message for subscribed API is Shown Successful");
		driver.findElement(By.id("userMenu")).click();
		driver.findElement(By.cssSelector("button.btn.btn-danger")).click();
	}

	@AfterClass(alwaysRun = true)
	public void tearDown() throws Exception {
		driver.quit();
	}

	private boolean isAPIPublished(String apiName, String version) throws IOException {
		BufferedReader input = new BufferedReader(new FileReader(carbonLogFilePath));
		String lastLine = null, line;

		while ((line = input.readLine()) != null) {
			lastLine = line;
		}
		input.close();
		if (lastLine != null && (lastLine.contains("INFO {org.apache.synapse.rest.API}"))) {
			return true;
		} else {
			return false;
		}
	}

	public boolean elementIsVisible(By elementToFind) {
		try {
			driver.findElement(elementToFind);
			return true;
		} catch (NoSuchElementException ex) {
			return false;
		}
	}
}
