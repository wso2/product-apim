package org.wso2.am.integration.ui.tests;


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestNameLengthTestCase extends APIMIntegrationUiTestBase {
    private WebDriver driver;
    private static final String USER_NAME = "admin";
    private static final CharSequence PASSWORD = "admin";
    private static final String API_NAME = "YoutubeFeedszlongName1YoutubeFeedszlongName2YoutubeFeedszlongName3";
    private static final String API_CONTEXT = "youtubeContext";
    private static final String API_VERSION = "1.0.0";
    private static final String API_DESCRIPTION = "Youtube Live Feeds";
    private static final String API_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        super.init();
        driver = BrowserManager.getWebDriver();
        driver.get(getPublisherURL());
    }

    @Test(groups = "wso2.greg", description = "verify API publish")
    public void testPublishAPI() throws Exception {
        WebDriverWait wait = new WebDriverWait(driver, 60);
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys(USER_NAME);
        driver.findElement(By.id("pass")).clear();
        driver.findElement(By.id("pass")).sendKeys(PASSWORD);
        driver.findElement(By.id("loginButton")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Add")));
        driver.findElement(By.linkText("Add")).click();


        driver.findElement(By.id("name")).clear();
        driver.findElement(By.id("name")).sendKeys(API_NAME);
        driver.findElement(By.id("context")).clear();
        driver.findElement(By.id("context")).sendKeys(API_CONTEXT);
        driver.findElement(By.id("version")).clear();
        driver.findElement(By.id("version")).sendKeys(API_VERSION);
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

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("apiView")));
        Assert.assertTrue("Youtube Live Feeds".equals(driver.findElement(By.id("apiView")).getText()), "Youtube Live Feeds");
    }


    @Test(groups = "wso2.apim", description = "adding new api with long name", dependsOnMethods = "testPublishAPI")
    public void testVerifyAPIName() throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, 60);
        String apiXpath = "//div[2]/div/div[2]/div/ul/li/div/div/a";
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Browse")));
        driver.findElement(By.linkText("Browse")).click();
        Thread.sleep(5000);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(apiXpath)));

        long startTime = System.currentTimeMillis();
        long nowTime = startTime;

        while ((driver.findElement(By.xpath(apiXpath))).getText().isEmpty() && (nowTime - startTime) < (180 * 1000)) {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Browse")));
            driver.findElement(By.linkText("Browse")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(apiXpath)));
            nowTime = System.currentTimeMillis();
        }

        assertTrue(driver.findElement(By.xpath(apiXpath)).getText().contains("YoutubeFeedszlongName1"), "truncated API name found");
        assertEquals(driver.findElement(By.xpath(apiXpath)).getSize().getWidth(), 150, "Expected width of element not matching");

    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        driver.quit();
    }
}


