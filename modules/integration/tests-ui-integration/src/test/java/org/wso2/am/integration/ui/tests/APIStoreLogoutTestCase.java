package org.wso2.am.integration.ui.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.HttpContext;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.ui.tests.util.TestUtil;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * This test class verifies the fix for
 * https://wso2.org/jira/browse/APIMANAGER-3224
 *
 */
public class APIStoreLogoutTestCase extends APIMIntegrationUiTestBase {

	private WebDriver driver;

	private static final String TEST_DATA_API_NAME = "LogoutTestAPI";
	private static final String TEST_DATA_API_VERSION = "1.0.0";
	private static final String SUPER_TENANT_DOMAIN_NAME = "carbon.super";

	private static final Log log = LogFactory.getLog(APIStoreLogoutTestCase.class);

	@BeforeClass(alwaysRun = true)
	public void setEnvironment() throws Exception {
		super.init();
		driver = BrowserManager.getWebDriver();
		driver.get(getLoginURL());
	}

	@Test(groups = "wso2.am", description = "This method adds and publishes the Test API in carbon.super store")
	public void testCreateAndPublishAPI() throws Exception {
		String loginURL = getPublisherURL();
		HttpContext httpContext =
		                          TestUtil.login(gatewayContext.getContextTenant().getContextUser().getUserName(),
										  gatewayContext.getContextTenant().getContextUser().getPassword(),
		                                         loginURL);

		assertTrue(TestUtil.addAPI(gatewayContext.getContextTenant().getContextUser().getUserName(), TEST_DATA_API_NAME,
		                           TEST_DATA_API_VERSION, httpContext, loginURL));
		assertTrue(TestUtil.publishAPI(gatewayContext.getContextTenant().getContextUser().getUserName(), TEST_DATA_API_NAME,
		                               TEST_DATA_API_VERSION, httpContext, loginURL));

		System.out.println("API Create and Publish test case is completed ");
	}

	@Test(groups = "wso2.am", description = "verify Logout action after selecting an API", dependsOnMethods = { "testCreateAndPublishAPI" })
	public void testLogout() throws Exception {

		// Go to the Tenant store and click Login
		driver.get(getStoreURL() + "?tenant=" +
		           SUPER_TENANT_DOMAIN_NAME);
		WebDriverWait wait = new WebDriverWait(driver, 60);
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
		driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
		driver.findElement(By.id("loginBtn")).click();

		// waiting to finish the login
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[@class='link-myapplications']")));

		// select API
		driver.navigate().to(getStoreURL() + "/apis/info?name=" +
		                             TEST_DATA_API_NAME + "&version=" + TEST_DATA_API_VERSION +
		                             "&provider=" + gatewayContext.getContextTenant().getContextUser().getUserName() + "&tenant=" +
		                             SUPER_TENANT_DOMAIN_NAME);

		// waiting till load
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText(gatewayContext.getContextTenant().getContextUser().getUserName())));
		driver.findElement(By.linkText(gatewayContext.getContextTenant().getContextUser().getUserName())).click();

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("logout-link")));
		driver.findElement(By.id("logout-link")).click();

		assertFalse(driver.getPageSource().contains("Error 500"), "500 error when Logout");
	}

	@AfterClass(alwaysRun = true)
	public void tearDown() throws Exception {
		driver.quit();
	}

}
