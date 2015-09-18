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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.ui.tests.util.TestUtil;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;

import java.net.URL;

/**
 * The issue in APIMANAGER3026 is when api is created by rest call via publisher APIs
 * and when we try to edit description of created API, the description was not found.
 * This was fixed and will test by this test case.
 */
public class APIMANAGER3026DescriptionFieldTestCase extends APIMIntegrationUiTestBase {

	private WebDriver driver;
	private APIPublisherRestClient apiPublisher;
	private String userName = "admin";
	private String password = "admin";

	private static final Log log = LogFactory.getLog(APIMANAGER3026DescriptionFieldTestCase.class);

	@BeforeClass(alwaysRun = true)
	public void setUp() throws Exception {
		super.init();
		apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
		driver = BrowserManager.getWebDriver();
	}

	@Test(groups = "wso2.am", description = "testing description field")
	public void testDescriptionField() throws Exception {
		String APIName = "TokenRefreshTestAPI";
		String APIContext = "tokenRefreshTestAPI";
		String tags = "youtube, token, media";
		String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
		String description = "This is test API create by API manager integration test";
		String APIVersion = "1.0.0";

		apiPublisher.login(userName, password);

		APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
		apiRequest.setTags(tags);
		apiRequest.setDescription(description);
		apiRequest.setVersion(APIVersion);
		apiRequest.setSandbox(url);
		apiPublisher.addAPI(apiRequest);

		WebDriverWait wait = new WebDriverWait(driver, 30);

		driver.get(getPublisherURL());

		driver.findElement(By.id("username")).clear();
		driver.findElement(By.id("username")).sendKeys(userName);
		driver.findElement(By.id("pass")).clear();
		driver.findElement(By.id("pass")).sendKeys(password);
		driver.findElement(By.id("loginButton")).click();

		driver.get(getPublisherURL() + "/info?name=" + APIName + "&version=" + APIVersion +
		           "&provider=" + userName);

		driver.get(getPublisherURL() + "/design?name=" + APIName + "&version=" + APIVersion +
		           "&provider=" + userName);

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("description")));
		WebElement descriptionElement = driver.findElement(By.id("description"));
		String elementVal = descriptionElement.getText();
		log.info("description value : " + elementVal);
		Assert.assertEquals(description, elementVal);

	}

	@AfterClass(alwaysRun = true)
	public void tearDown() throws Exception {
        super.cleanUp();
		driver.quit();
	}
}
