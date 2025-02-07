package org.wso2.am.integration.ui.pages.metadata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.wso2.am.integration.ui.pages.login.LoginPage;
import org.wso2.am.integration.ui.pages.resourcebrowse.ResourceBrowsePage;
import org.wso2.am.integration.ui.pages.util.UIElementMapper;

import java.io.IOException;

public class ApiPage {

    private static final Log log = LogFactory.getLog(ApiPage.class);
    private WebDriver driver;
    private UIElementMapper uiElementMapper;

    public ApiPage(WebDriver driver) throws IOException {
        this.driver = driver;
        this.uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        driver.findElement(By.id(uiElementMapper.getElement("carbon.Main.tab"))).click();
        driver.findElement(By.linkText(uiElementMapper.getElement("api.add.link"))).click();

        log.info("API Add Page");
        if (!driver.findElement(By.id(uiElementMapper.getElement("api.dashboard.middle.text"))).
                getText().contains("API")) {

            throw new IllegalStateException("This is not the API  Add Page");
        }
    }

    public ResourceBrowsePage uploadApi(String provider, String name, String context,
                                        String version)
            throws InterruptedException, IOException {

        WebElement apiProvider = driver.findElement(By.id(uiElementMapper.getElement("api.provider.id")));
        apiProvider.sendKeys(provider);

        WebElement apiName = driver.findElement(By.id(uiElementMapper.getElement("api.name.id")));
        apiName.sendKeys(name);

        WebElement apiContext = driver.findElement(By.id(uiElementMapper.getElement("api.context.id")));
        apiContext.sendKeys(context);

        WebElement apiVersion = driver.findElement(By.id(uiElementMapper.getElement("api.version.id")));
        apiVersion.sendKeys(version);

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("addEditArtifact()");
        log.info("successfully Saved");

        return new ResourceBrowsePage(driver);

    }

    public LoginPage logout() throws IOException {
        driver.findElement(By.linkText(uiElementMapper.getElement("home.greg.sign.out.xpath"))).click();
        return new LoginPage(driver);
    }

}
