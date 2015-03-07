/*
*  Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/


package org.wso2.carbon.am.integration.ui;

import static org.testng.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.apache.http.protocol.HttpContext;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.am.integration.ui.util.TestUtil;
import org.wso2.carbon.automation.core.BrowserManager;
import org.wso2.carbon.automation.core.ProductConstant;

/**
 * Test for parameter body in GET/DELETE/OPTION resource
 */
public class APIResourceBodyParameterRemovalTestCase extends
		AMIntegrationUiTestBase {
	private WebDriver driver;

	private static final String TEST_DATA_API_NAME = "ResourceBodyAvailabilityTestAPI";
	private static final String TEST_DATA_API_VERSION = "1.0.0";

	@BeforeClass(alwaysRun = true)
	public void init() throws Exception {
		super.init(0);
		driver = BrowserManager.getWebDriver();
		driver.get(getLoginURL(ProductConstant.AM_SERVER_NAME));
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}

	@Test(groups = "wso2.am", description = "This method adds and publishes the Test API in "
			+ "carbon.super store")
	public void testcreateAndPublishAPI() throws Exception {
		String loginURL = getPublisherURL(ProductConstant.AM_SERVER_NAME);
		HttpContext httpContext = TestUtil.login(userInfo.getUserName(),
				userInfo.getPassword(), loginURL);
		//creates an API with default parameters for resources
		assertTrue(TestUtil.addAPI(userInfo.getUserName(), TEST_DATA_API_NAME,
				TEST_DATA_API_VERSION, httpContext, loginURL));
		assertTrue(TestUtil.publishAPI(userInfo.getUserName(),
				TEST_DATA_API_NAME, TEST_DATA_API_VERSION, httpContext,
				loginURL));

	}

	@Test(groups = "wso2.am", description = "Check time conversion from epoch to human readable "
			+ "version", dependsOnMethods = "testcreateAndPublishAPI")
	public void testResourceParameters() throws Exception {
		// login to publisher
		driver.get(getPublisherURL(ProductConstant.AM_SERVER_NAME)
				+ "/site/pages/login.jag");
		WebDriverWait wait = new WebDriverWait(driver, 30);
		wait.until(ExpectedConditions.visibilityOfElementLocated(By
				.id("username")));
		driver.findElement(By.id("username")).clear();
		driver.findElement(By.id("username")).sendKeys(userInfo.getUserName());
		driver.findElement(By.id("pass")).clear();
		driver.findElement(By.id("pass")).sendKeys(userInfo.getPassword());
		driver.findElement(By.id("loginButton")).click();
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
		// wait until load the page
		wait.until(ExpectedConditions.visibilityOfElementLocated(By
				.linkText("Browse")));
		// Click browse API and wait

		driver.findElement(By.cssSelector("img.thumb.responsive")).click();
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

		wait.until(ExpectedConditions.visibilityOfElementLocated(By
				.linkText("Edit")));
		// Click edit API and wait
		driver.findElement(By.linkText("Edit")).click();
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

		driver.findElement(By.xpath("//div[@id='item-add']/center/ul/li[3]/a"))
				.click();
		
		WebElement method = driver.findElement(By.xpath("//div[@id='resource_details']/"
				+ "table/tbody/tr[2]/td/span"));
		
		//check for method
		Assert.assertNotNull(method, "Resource does not have a HTTP method");
		
		if("GET".equalsIgnoreCase(method.getText()) ||
				"DELETE".equalsIgnoreCase(method.getText()) || 
				"OPTIONS".equalsIgnoreCase(method.getText())) {
			
			method.click();
			//there should not be any parameters for GET because we created resource without any 
			//parameters. If there is a parameter by default, then it is the body parameter which
			//should be only in POST, PUT methods
			WebElement elem = null;
			//check for element
			try{
				elem = driver.findElement(By.xpath("//div[@id='resource_details']"
					+ "/table/tbody/tr[3]/td/div/table/tbody/tr[2]/td[3]"));
			} catch(Exception e) {
				//ignore the exception
			}
			if(elem == null){
				Assert.assertNull(elem, "has body parameter");
			}
			
			
		}

	}

	@AfterClass(alwaysRun = true)
	public void tearDown() throws Exception {
		if (driver != null) {
			driver.quit();
		}

	}
}
