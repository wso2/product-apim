package org.wso2.am.integration.ui.pages.Jaggery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.wso2.am.integration.ui.pages.admin.LoginPage;
import org.wso2.am.integration.ui.pages.util.UIElementMapper;

import java.io.IOException;
import java.util.NoSuchElementException;

public class JaggeryListPage {

    private static final Log log = LogFactory.getLog(JaggeryListPage.class);
    private WebDriver driver;
    private UIElementMapper uiElementMapper;

    public JaggeryListPage(WebDriver driver) throws IOException {
        this.driver = driver;
        this.uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
         log.info("in the jaggeryList page");
        if (!driver.findElement(By.id(uiElementMapper.getElement("jaggery.dashboard.middle.text"))).
                getText().contains("Running Applications")) {
            throw new IllegalStateException("This is not the Jaggery list page");
        }
    }

    public boolean checkOnUploadJaggeryItem(String serviceName) throws InterruptedException {
        log.info(serviceName);
        Thread.sleep(15000);
        driver.navigate().refresh();

           String ServiceNameOnServer = driver.findElement(By.xpath("/html/body/table/tbody/tr[2]/td[3]" +
                "/table/tbody/tr[2]/td/div/div/form[2]/table/tbody/tr/td[2]/a")).getText();

        log.info(ServiceNameOnServer);

        if (serviceName.equals(ServiceNameOnServer)) {
            log.info("Uploaded service exists");
            return true;

        } else {
            String resourceXpath = "/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/div/form[2]/table/tbody/tr[";

            String resourceXpath2 = "]/td[2]/a";
            for (int i = 2; i < 10; i++) {
                String serviceNameOnAppServer = resourceXpath + i + resourceXpath2;
                String actualResourceName = driver.findElement(By.xpath(serviceNameOnAppServer)).getText();
                log.info("val on app is -------> " + actualResourceName);
                log.info("Correct is    -------> " + serviceName);
                try {
                    if (serviceName.contains(actualResourceName)) {
                        log.info("Uploaded service exists");
                        return true;

                    }  else {
                        return false ;
                    }

                } catch (NoSuchElementException ex) {
                    log.info("Cannot Find the Uploaded service");

                }

            }

        }

        return false;
    }


    public LoginPage logout() throws IOException {
        driver.findElement(By.xpath(uiElementMapper.getElement("home.greg.sign.out.xpath"))).click();
        return new LoginPage(driver);
    }

}
