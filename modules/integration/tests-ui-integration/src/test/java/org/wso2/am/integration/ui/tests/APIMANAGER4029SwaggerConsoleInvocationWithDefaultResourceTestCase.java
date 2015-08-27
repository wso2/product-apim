/*
 * Copyright (c) 2015​, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.am.integration.ui.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;

import java.net.MalformedURLException;
import java.net.URL;

public class APIMANAGER4029SwaggerConsoleInvocationWithDefaultResourceTestCase extends APIMIntegrationBaseTest {
	private WebDriver driver;
	WebDriverWait wait;
	private static final Log
			log = LogFactory.getLog(APIMANAGER4029SwaggerConsoleInvocationWithDefaultResourceTestCase.class);
	APIPublisherRestClient apiPublisher;
	APIStoreRestClient apiStoreRestClient;

	@BeforeClass(alwaysRun = true)
	public void init() throws APIManagerIntegrationTestException {
		super.init();
		/*
		Create API with default resource
		 */
		apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
		apiStoreRestClient = new APIStoreRestClient(storeUrls.getWebAppURLHttp());
		apiPublisher.login("admin", "admin");
		APIRequest apiRequest =
				null;
		try {
			apiRequest = new APIRequest("APIMANAGER4029", "/APIMANAGER4029", new URL("http://localhost:9443/store"));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		apiRequest.setVersion("1.0.0");
		apiPublisher.addAPI(apiRequest);
		APILifeCycleStateRequest apiLifeCycleStateRequest = new APILifeCycleStateRequest("APIMANAGER4029", "admin",
		                                                                                 APILifeCycleState.PUBLISHED);
		apiPublisher.changeAPILifeCycleStatus(apiLifeCycleStateRequest);
		apiPublisher.logout();
		apiStoreRestClient.login("admin", "admin");
		apiStoreRestClient.addApplication("APIMANAGER4029", "Unlimited", "http://localhost", "APIMANAGER4029");
		SubscriptionRequest subscriptionRequest = new SubscriptionRequest("APIMANAGER4029", "admin");
		apiStoreRestClient.subscribeToAPI(subscriptionRequest);
		apiStoreRestClient.generateApplicationKey(new APPKeyRequestGenerator("APIMANAGER4029"));
		driver = new FirefoxDriver();
		wait = new WebDriverWait(driver, 60);
	}

	@Test(groups = "wso2.am", description = "check weather API resource shown in swagger console having /* resource")
	public void testResourceShownInSwaggerUI() throws Exception {
		driver.get(storeUrls.getWebAppURLHttps() + "/store/?tenant=carbon.super");
		while (!isElementPresent(By.linkText("APIMANAGER4029-1.0.0"))) {
			driver.findElement(By.linkText("APIs")).click();
			Thread.sleep(5000);
		}
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-link")));
		driver.findElement(By.id("login-link")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
		driver.findElement(By.id("username")).sendKeys("admin");
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));
		driver.findElement(By.id("password")).sendKeys("admin");
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginBtn")));
		driver.findElement(By.id("loginBtn")).click();
		// waiting until logged in
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			log.warn("Interrupted Exception while log into to store" + e);
		}
		//	wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("store.menu.apis.link")));
		driver.findElement(By.linkText("APIMANAGER4029-1.0.0")).click();
		driver.findElement(By.linkText("API Console")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(
				By.xpath("//span[@class=\"http_method\"]/a[@class=\"toggleOperation\"][@href=\"#!/default" +
				         "/get\"]")));
		driver.findElement(By.xpath("//span[@class=\"http_method\"]/a[@class=\"toggleOperation\"][@href=\"#!/default" +
		                            "/get\"]")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(
				By.xpath("//div[@id=\"default_get_content\"]//input[@type=\"button\"][@value=\"Try it out!\"]")));
		driver.findElement(
				By.xpath("//div[@id=\"default_get_content\"]//input[@type=\"button\"][@value=\"Try it out!\"]"))
		      .click();
		Thread.sleep(5000);
		String gatewayRequestUrl = "/APIMANAGER4029/1.0.0";
		WebElement resourceSelector = driver.findElement(
				By.xpath("//span[@class=\"path\"]/a[@class=\"toggleOperation \"][@href=\"#!/default/get\"]"));
		Assert.assertEquals(resourceSelector.getText(), "/*", "Resource shown in Swagger Console is Correct");
		WebElement curlRequest = driver.findElement(
				By.xpath("//div[@id=\"default_get_content\"]//div[@class=\"block curl\"]/pre"));
		String requestUrl[] = curlRequest.getText().split(gatewayRequestUrl);
		Assert.assertEquals("\"", requestUrl[requestUrl.length - 1],
		                    "Curl Request Url is Correct");
		WebElement request = driver.findElement(
				By.xpath("//div[@id=\"default_get_content\"]//div[@class=\"block request_url\"]/pre"));
		String requestsUi = request.getText();
		Assert.assertEquals(requestsUi.split(gatewayRequestUrl).length, 1, "Request Url Shown in UI is Correct");
	}

	@AfterClass(alwaysRun = true)
	public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
		apiStoreRestClient.removeApplication("APIMANAGER4029");
		apiPublisher.login("admin", "admin");
		apiPublisher.deleteAPI("APIMANAGER4029", "1.0.0", "admin");
		apiPublisher.logout();
		driver.quit();
	}

	/**
	 * @param by element need to find in browser
	 * @return existence of element in browser
	 */
	private boolean isElementPresent(By by) {
		try {
			driver.findElement(by);
			return true;
		} catch (NoSuchElementException e) {
			return false;
		}
	}

}
