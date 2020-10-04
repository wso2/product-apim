package org.wso2.am.integration.tests.benchmarktest;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;

import java.util.concurrent.TimeUnit;

public class UiComponents {
    private static final String APIM_URL_SYSTEM_PROPERTY = "apim.url";
    private final String HTTP_PROTOCOL = "https://";
    BenchmarkUtils benchmarkUtils = new BenchmarkUtils();
    WebDriver driver = null;
    WebDriverWait wait;
    int waitTime = 5000;

    public void openBrowser() throws IOException {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("start-maximized");
        options.addArguments("enable-automation");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-browser-side-navigation");
        options.addArguments("--disable-gpu");
        options.addArguments(" --ignore-ssl-errors=yes");
        options.addArguments("--ignore-certificate-errors");
//        options.addArguments("--headless");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
    }

    public void navigateToPublisher() throws IOException {
        if (System.getProperty(APIM_URL_SYSTEM_PROPERTY) == null) {
            System.setProperty(APIM_URL_SYSTEM_PROPERTY, benchmarkUtils.getApimURL());
        }
        driver.get(HTTP_PROTOCOL + System
            .getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + BenchmarkUtils.apimPort + "/publisher/apis");
    }

    public void loginToPublisher(String userName, String password) throws InterruptedException {
        benchmarkUtils.setTenancy(userName);
        driver.findElement(By.id("usernameUserInput")).sendKeys(userName);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.xpath("//button[@type='submit']")).click();
    }

    public void validateNoOfAPIS(String noOfApis) {
        driver.findElement(By.xpath("//p[contains(text(),'" + noOfApis + "')]")).isDisplayed();
    }

    public void navigateToAPIsPage() {
        driver.findElement(By.xpath("//a[@href='/publisher/apis']")).click();
    }

    public void navigateToCreateAnApiPage() throws InterruptedException {
        driver.findElement(By.id("itest-id-createapi")).click();
        driver.findElement(By.xpath("//span[contains(text(),'Design a New REST API')]")).click();
        Thread.sleep(waitTime);
    }

    public void createAnAPI(String apiName, String apiContext, String apiVersion)
        throws InterruptedException {
        String apiEndpoint = "http://jsonplaceholder.typicode.com/";
        Actions action = new Actions(driver);
        driver.findElement(By.name("name")).sendKeys(apiName);
        driver.findElement(By.name("context")).sendKeys(apiContext);
        driver.findElement(By.name("version")).sendKeys(apiVersion);
        driver.findElement(By.name("endpoint")).sendKeys(apiEndpoint);
        driver.findElement(By.id("mui-component-select-policies")).click();
        driver.findElement(By.xpath("//li[@id='Unlimited']")).click();
        action.sendKeys(Keys.ESCAPE).build().perform();
        driver.findElement(By.xpath("//span[contains(text(),'Create')]//../../button[1]")).click();
        driver.findElement(By.xpath("//h4[contains(text(),'Overview')]")).isDisplayed();
        Thread.sleep(waitTime);
    }

    public void publishApi() {
        driver.findElement(By.xpath("//span[contains(text(),'Publish')]")).click();
    }

    public void navigateToDevPortal() throws IOException {
        if (System.getProperty(APIM_URL_SYSTEM_PROPERTY) == null) {
            System.setProperty(APIM_URL_SYSTEM_PROPERTY, benchmarkUtils.getApimURL());
        }
        driver.get(HTTP_PROTOCOL + System
            .getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + BenchmarkUtils.apimPort + "/devportal");
        driver.findElement(By.xpath("//p[contains(text(),'carbon.super')]")).click();
    }

    public void loginToDevPortal(String userName, String password) throws IOException {
        benchmarkUtils.setTenancy(userName);
        navigateToDevPortal();
        driver.findElement(By.xpath("//a[@href='/devportal/applications']")).click();
        driver.findElement(By.id("usernameUserInput")).sendKeys(userName);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.xpath("//button[@type='submit']")).click();
    }

    public void navigateToDevPortalAPIsPage() {
        driver.get(HTTP_PROTOCOL + System
            .getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + BenchmarkUtils.apimPort + "/devportal/apis");
    }

    public void navigateToCreateAnApplicationPage() {
        driver.get(HTTP_PROTOCOL + System
            .getProperty(APIM_URL_SYSTEM_PROPERTY) + ':' + BenchmarkUtils.apimPort + "/devportal/applications/create");
    }

    public void createAnApplication(String appName) {
        driver.findElement(By.xpath("//input[@id='application-name']")).sendKeys(appName);
        driver.findElement(By.xpath("//textarea[@name='description']")).sendKeys("Test Description");
        driver.findElement(By.xpath("//span[contains(text(),'SAVE')]")).click();
        driver.findElement(By.xpath("//p[contains(text(),'Overview')]")).isDisplayed();
    }

    public void closeBrowser() {
        driver.quit();
    }

}
