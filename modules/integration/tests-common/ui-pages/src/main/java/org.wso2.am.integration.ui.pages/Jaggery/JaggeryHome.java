package org.wso2.am.integration.ui.pages.Jaggery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.wso2.am.integration.ui.pages.login.LoginPage;
import org.wso2.am.integration.ui.pages.util.UIElementMapper;

import java.io.IOException;
import java.util.NoSuchElementException;

public class JaggeryHome {

    private static final Log log = LogFactory.getLog(JaggeryHome.class);
    private WebDriver driver;
    private UIElementMapper uiElementMapper;

    public JaggeryHome(WebDriver driver) throws IOException {
        this.driver = driver;
        this.uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        driver.findElement(By.id(uiElementMapper.getElement("jaggery.Main.tab"))).click();
        driver.findElement(By.id(uiElementMapper.getElement("jaggery.Region1.tab"))).click();
        driver.findElement(By.linkText(uiElementMapper.getElement("add.jaggery.href"))).click();
        log.info("in the Jaggery upload page");
        if (!driver.findElement(By.id(uiElementMapper.getElement("jaggery.dashboard.middle.text"))).
                getText().contains("Upload Jaggery Applications")) {
            throw new IllegalStateException("This is not the Jaggery page");
        }
    }

    public JaggeryListPage UploadJaggeryItem(String UploadItem) throws IOException, InterruptedException {
        log.info(UploadItem);
        WebElement jaggeryUploadField = driver.findElement(By.name(uiElementMapper.getElement
                ("jaggery.war.file.upload.name")));

        jaggeryUploadField.sendKeys(UploadItem);
        driver.findElement(By.name(uiElementMapper.getElement("jaggery.upload.button.name"))).click();
        if (!driver.findElement(By.id(uiElementMapper.getElement("jaggery.upload.successful.message"))).
                getText().contains("successfully")) {

            throw new NoSuchElementException();
        }

        log.info("Successfully Uploaded");
        driver.findElement(By.className(uiElementMapper.getElement("jaggery.upload.successful.button"))).click();
        log.info("Ready to sign out");
        Thread.sleep(10000);
        driver.navigate().refresh();
        return new JaggeryListPage(driver);

    }


    public LoginPage logout() throws IOException {
        driver.findElement(By.xpath(uiElementMapper.getElement("home.greg.sign.out.xpath"))).click();
        return new LoginPage(driver);
    }

}
