package org.wso2.am.integration.ui.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.ui.tests.util.TestUtil;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;
import org.wso2.carbon.utils.CarbonUtils;

public class APIMANAGER3371BusinessInformationClearedWhenAPISavedButton extends APIMIntegrationUiTestBase {

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
		driver.findElement(By.id("username")).sendKeys(gatewayContext.getContextTenant().getContextUser().getUserName());
		driver.findElement(By.id("pass")).clear();
		driver.findElement(By.id("pass")).sendKeys(gatewayContext.getContextTenant().getContextUser().getPassword());
		driver.findElement(By.id("loginButton")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add")));
		driver.findElement(By.linkText("Add")).click();

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("create-new-api")));
		driver.findElement(By.id("create-new-api")).click();

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("designNewAPI")));
		driver.findElement(By.id("designNewAPI")).click();

		driver.findElement(By.id("name")).clear();
		driver.findElement(By.id("name")).sendKeys("APIMANAGER3371");
		driver.findElement(By.id("context")).clear();
		driver.findElement(By.id("context")).sendKeys("APIMANAGER3371");
		driver.findElement(By.id("version")).clear();
		driver.findElement(By.id("version")).sendKeys("1.0.0");
		driver.findElement(By.id("description")).clear();
		driver.findElement(By.id("description")).sendKeys(API_DESCRIPTION);
		driver.findElement(By.id("resource_url_pattern")).clear();
		driver.findElement(By.id("resource_url_pattern")).sendKeys("*");
		/*driver.findElement(By.id("inputResource")).clear();
		driver.findElement(By.id("inputResource")).sendKeys("default");*/
		driver.findElement(By.cssSelector("input.http_verb_select")).click();
		driver.findElement(By.id("add_resource")).click();
		driver.findElement(By.id("go_to_implement")).click();

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@value='#managed-api']")));
		driver.findElement(By.xpath("//div[@value='#managed-api']")).click();

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("go_to_manage")));
		driver.findElement(By.id("jsonform-0-elt-production_endpoints")).clear();
		driver.findElement(By.id("jsonform-0-elt-production_endpoints")).sendKeys(API_URL);
		driver.findElement(By.id("go_to_manage")).click();
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("publish_api")));
		driver.findElement(By.xpath("//button[@type='button']")).click();
		driver.findElement(By.xpath("//input[@value='Gold']")).click();
		driver.findElement(By.xpath("//form[@id='manage_form']/fieldset[3]/legend")).click();
		driver.findElement(By.id("bizOwner")).clear();
		driver.findElement(By.id("bizOwner")).sendKeys("abc");
		driver.findElement(By.id("bizOwnerMail")).clear();
		driver.findElement(By.id("bizOwnerMail")).sendKeys("abc@abc.com");
		driver.findElement(By.id("techOwner")).clear();
		driver.findElement(By.id("techOwner")).sendKeys("tes");
		driver.findElement(By.id("techOwnerMail")).clear();
		driver.findElement(By.id("techOwnerMail")).sendKeys("tec@tech.com");
		driver.findElement(By.id("publish_api")).click();
		driver.findElement(By.linkText("Edit")).click();
		/*wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input.btn.btn-primary")));
		driver.findElement(By.cssSelector("input.btn.btn-primary")).click();*/
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[@id='saveBtn']")));
		driver.findElement(By.xpath("//button[@id='saveBtn']")).click();
		driver.findElement(By.cssSelector("a.wizard-done")).click();
		driver.findElement(By.xpath("//div[@id='item-add']/center/ul/li[3]/a")).click();
		driver.findElement(By.xpath("//form[@id='manage_form']/fieldset[3]/legend")).click();
		Assert.assertEquals(driver.findElement(By.id("techOwner")).getAttribute("value"), "tes");
		Assert.assertEquals(driver.findElement(By.id("techOwnerMail")).getAttribute("value"), "tec@tech.com");
		Assert.assertEquals(driver.findElement(By.id("bizOwner")).getAttribute("value"), "abc");
		Assert.assertEquals(driver.findElement(By.id("bizOwnerMail")).getAttribute("value"), "abc@abc.com");
		driver.findElement(By.id("userMenu")).click();
		driver.findElement(By.cssSelector("button.btn.btn-danger")).click();
	}

	@AfterClass(alwaysRun = true)
	public void tearDown() throws Exception {
        TestUtil.cleanUp(gatewayContext.getContextTenant().getContextUser().getUserName(),
                         gatewayContext.getContextTenant().getContextUser().getPassword(),
                         storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());
		driver.quit();
	}
}
