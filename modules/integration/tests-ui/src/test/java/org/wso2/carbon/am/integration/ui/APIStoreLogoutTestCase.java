package org.wso2.carbon.am.integration.ui;

import java.util.concurrent.TimeUnit;

import org.apache.http.protocol.HttpContext;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.am.integration.ui.util.TestUtil;
import org.wso2.carbon.automation.core.BrowserManager;
import org.wso2.carbon.automation.core.ProductConstant;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

/**
 * This test class verifies the fix for
 * https://wso2.org/jira/browse/APIMANAGER-3224
 *
 */
public class APIStoreLogoutTestCase extends AMIntegrationUiTestBase {

	private WebDriver driver;

	private static final String TEST_DATA_API_NAME = "LogoutTestAPI";
	private static final String TEST_DATA_API_VERSION = "1.0.0";
	private static final String SUPER_TENANT_DOMAIN_NAME = "carbon.super";

	@BeforeClass(alwaysRun = true)
	public void init() throws Exception {
		super.init(0);
		driver = BrowserManager.getWebDriver();
		driver.get(getLoginURL(ProductConstant.AM_SERVER_NAME));
	}

	@Test(groups = "wso2.am", description = "This method adds and publishes the Test API in carbon.super store")
	public void testcreateAndPublishAPI() throws Exception {
		String loginURL = getPublisherURL(ProductConstant.AM_SERVER_NAME);
		HttpContext httpContext =
		                          TestUtil.login(userInfo.getUserName(), userInfo.getPassword(),
		                                         loginURL);

		assertTrue(TestUtil.addAPI(userInfo.getUserName(), TEST_DATA_API_NAME,
		                           TEST_DATA_API_VERSION, httpContext, loginURL));
		assertTrue(TestUtil.publishAPI(userInfo.getUserName(), TEST_DATA_API_NAME,
		                               TEST_DATA_API_VERSION, httpContext, loginURL));

		System.out.println("API Create and Publish test case is completed ");
	}

	@Test(groups = "wso2.am", description = "verify Logout action after selecting an API", dependsOnMethods = { "testcreateAndPublishAPI" })
	public void testLogout() throws Exception {

		// Go to the Tenant store and click Login
		driver.get(getStoreURL(ProductConstant.AM_SERVER_NAME) + "?tenant=" +
		           SUPER_TENANT_DOMAIN_NAME);
		WebDriverWait wait = new WebDriverWait(driver, 30);
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
		driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
		driver.findElement(By.id("loginBtn")).click();

		// select API
		driver.navigate().to(getStoreURL(ProductConstant.AM_SERVER_NAME) + "/apis/info?name=" +
		                             TEST_DATA_API_NAME + "&version=" + TEST_DATA_API_VERSION +
		                             "&provider=" + userInfo.getUserName() + "&tenant=" +
		                             SUPER_TENANT_DOMAIN_NAME);

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText(userInfo.getUserName())));
		driver.findElement(By.linkText(userInfo.getUserName())).click();

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("logout-link")));
		driver.findElement(By.id("logout-link")).click();

		assertFalse(driver.getPageSource().contains("Error 500"), "500 error when Logout");
		driver.close();
	}

	@AfterClass(alwaysRun = true)
	public void tearDown() throws Exception {
		driver.quit();
	}

}
