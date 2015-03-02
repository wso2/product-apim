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
package org.wso2.carbon.am.integration.ui;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.core.BrowserManager;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.BufferedReader;
import java.io.FileReader;

public class APIMANAGER3087UnableToPublishSecondAttemptWithTiers extends AMIntegrationUiTestBase {

	private WebDriver driver;
	private WebDriverWait wait;

	@BeforeClass(alwaysRun = true)
	public void init() throws Exception {
		super.init(0);
		driver = BrowserManager.getWebDriver();
		wait = new WebDriverWait(driver, 60);
	}

	@Test(groups = "wso2.am", description = "verify Publishing API when applying tiers in second attempt")
	public void testPublishSecondAttemptWithTiers() throws Exception {
		String carbonLogFilePath = CarbonUtils.getCarbonLogsPath() + "/wso2carbon.log";
		driver.get(getPublisherURL(ProductConstant.AM_SERVER_NAME));
		driver.findElement(By.id("username")).sendKeys(userInfo.getUserName());
		driver.findElement(By.id("pass")).sendKeys(userInfo.getPassword());
		driver.findElement(By.id("loginButton")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add")));
		driver.findElement(By.linkText("Add")).click();
		driver.findElement(By.id("name")).clear();
		driver.findElement(By.id("name")).sendKeys("SecondAttemptPublish");
		driver.findElement(By.id("context")).clear();
		driver.findElement(By.id("context")).sendKeys("SecondAttemptPublish");
		driver.findElement(By.id("version")).clear();
		driver.findElement(By.id("version")).sendKeys("1.0.0");
		driver.findElement(By.id("go_to_implement")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Yes")));
		driver.findElement(By.linkText("Yes")).click();
		driver.findElement(By.id("go_to_implement")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(
				"jsonform-0-elt-production_endpoints")));
		driver.findElement(By.id("jsonform-0-elt-production_endpoints")).sendKeys("http");
		driver.findElement(By.id("go_to_manage")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("publish_api")));
		driver.findElement(By.id("publish_api")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("tier_error")));
		driver.findElement(By.xpath("//button[@type='button']")).click();
		driver.findElement(By.xpath("//input[@value='Gold']")).click();
		driver.findElement(By.id("publish_api")).click();
		Thread.sleep(2000);
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Browse")));
		Thread.sleep(3000);
		BufferedReader input = new BufferedReader(new FileReader(carbonLogFilePath));
		String lastLine = null, line;

		while ((line = input.readLine()) != null) {
			lastLine = line;
		}

		if (lastLine != null && (lastLine.contains("INFO {org.apache.synapse.rest.API}"))) {
			Assert.assertTrue(true, "API is Successfully Initialized  With second attempt");
		} else {
			Assert.assertTrue(false, "API is not Successfully Initialized  With second attempt");

		}
	}

	@Test(groups = "wso2.am", description = "verify Publishing API when applying tiers in first attempt",
			dependsOnMethods = {
					"testPublishSecondAttemptWithTiers" })
	public void testPublishAttemptWithTiers() throws Exception {
		String carbonLogFilePath = CarbonUtils.getCarbonLogsPath() + "/wso2carbon.log";
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add")));
		driver.findElement(By.linkText("Add")).click();
		driver.findElement(By.id("name")).clear();
		driver.findElement(By.id("name")).sendKeys("FirstAttemptPublish");
		driver.findElement(By.id("context")).clear();
		driver.findElement(By.id("context")).sendKeys("FirstAttemptPublish");
		driver.findElement(By.id("version")).clear();
		driver.findElement(By.id("version")).sendKeys("1.0.0");
		driver.findElement(By.id("go_to_implement")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Yes")));
		driver.findElement(By.linkText("Yes")).click();
		driver.findElement(By.id("go_to_implement")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(
				"jsonform-0-elt-production_endpoints")));
		driver.findElement(By.id("jsonform-0-elt-production_endpoints")).sendKeys("http");
		driver.findElement(By.id("go_to_manage")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("publish_api")));
		driver.findElement(By.xpath("//button[@type='button']")).click();
		driver.findElement(By.xpath("//input[@value='Gold']")).click();
		driver.findElement(By.id("publish_api")).click();
		Thread.sleep(2000);
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Browse")));
		Thread.sleep(3000);
		BufferedReader input = new BufferedReader(new FileReader(carbonLogFilePath));
		String lastLine = null, line;

		while ((line = input.readLine()) != null) {
			lastLine = line;
		}

		if (lastLine != null && (lastLine.contains("INFO {org.apache.synapse.rest.API}"))) {
			Assert.assertTrue(true, "API is Successfully Initialized  With first attempt");
		} else {
			Assert.assertTrue(false, "API is not Successfully Initialized  With first attempt");
		}
	}

	@AfterClass(alwaysRun = true)
	public void tearDown() throws Exception {
		driver.quit();
		super.cleanup();
	}

}

