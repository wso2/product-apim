package org.wso2.am.integration.ui.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;

/*
 * This test should run with the following configuration
 * Configure AM and IS to use in SSO mode
 */
public class APIMANAGER3277SSOLogin extends AMIntegrationUiTestBase {

	private WebDriver driver;
	private String TEST_DATA_PASSWORD = "admin", TEST_DATA_FULL_USERNAME = "admin@carbon.super";
	protected String publisherURL;

	@BeforeClass(alwaysRun = true)
	protected void init() throws Exception {
		super.init();
		driver = BrowserManager.getWebDriver();
		publisherURL = getPublisherURL();
	}

	@Test(groups = "wso2.am")
	public void loginToPublisher() throws Exception {
		WebDriverWait wait = new WebDriverWait(driver, 10);
		// login to publisher
		driver.get(publisherURL + "/site/pages/login.jag");
		// wait until load the page
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
		driver.findElement(By.id("username")).clear();
		driver.findElement(By.id("username")).sendKeys(TEST_DATA_FULL_USERNAME);

		// this is the password field in IS sso login page
		if (driver.findElement(By.id("password")) != null) {
			driver.findElement(By.id("password")).clear();
			driver.findElement(By.id("password")).sendKeys(TEST_DATA_PASSWORD);

			driver.findElement(By.cssSelector(".btn")).click();
		}

		// wait until load the page
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add")));
		// if element present, test it successful
	}

	@AfterClass(alwaysRun = true)
	public void tearDown() throws Exception {
		driver.quit();
	}

}
