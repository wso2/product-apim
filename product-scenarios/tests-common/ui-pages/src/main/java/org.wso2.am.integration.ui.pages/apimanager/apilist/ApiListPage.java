package org.wso2.am.integration.ui.pages.apimanager.apilist;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;
import org.wso2.am.integration.ui.pages.login.LoginPage;
import org.wso2.am.integration.ui.pages.util.UIElementMapper;

import java.io.IOException;
import java.util.NoSuchElementException;

public class ApiListPage {

    private static final Log log = LogFactory.getLog(ApiListPage.class);
    private WebDriver driver;
    private UIElementMapper uiElementMapper;

    public ApiListPage(WebDriver driver) throws IOException {
        this.driver = driver;
        this.uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        driver.findElement(By.id(uiElementMapper.getElement("carbon.Main.tab"))).click();
        driver.findElement(By.linkText(uiElementMapper.getElement("api.list.link"))).click();

        log.info("API List Page");
        if (!driver.findElement(By.id(uiElementMapper.getElement("api.dashboard.middle.text"))).
                getText().contains("API List")) {

            throw new IllegalStateException("This is not the API  Add Page");
        }
    }

    public boolean checkOnUploadApi(String apiName) throws InterruptedException {


        log.info(apiName);
        driver.navigate().refresh();
        String firstElementXpath = "/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/div/" +
                                   "form[4]/table/tbody/tr/td/a";
        String apiNameOnServer = driver.findElement(By.xpath(firstElementXpath)).getText();
        log.info(apiNameOnServer);
        if (apiName.equals(apiNameOnServer)) {
            log.info("Uploaded Api exists");
            return true;
        } else {
            String resourceXpath = "/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/div/" +
                                   "form[4]/table/tbody/tr[";
            String resourceXpath2 = "]/td/a";
            for (int i = 2; i < 10; i++) {
                String apiNameOnAppServer = resourceXpath + i + resourceXpath2;

                String actualApiName = driver.findElement(By.xpath(apiNameOnAppServer)).getText();
                log.info("val on app is -------> " + actualApiName);
                log.info("Correct is    -------> " + apiName);

                try {
                    if (apiName.contains(actualApiName)) {
                        log.info("Uploaded API    exists");
                        return true; 
                        } else {
                          return false;
                        }
                } catch (NoSuchElementException ex) {
                    log.info("Cannot Find the Uploaded API");
                    
                }
            }
        }
        return false;
    }

        public void lifeCyclePromotion(String lifeCycleName) throws InterruptedException {
            driver.findElement(By.id(uiElementMapper.getElement("life.cycle.expand.id"))).click();
            driver.findElement(By.linkText(uiElementMapper.getElement("life.cycle.add"))).click();
            new Select(driver.findElement(By.id("aspect"))).selectByVisibleText(lifeCycleName);
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("addAspect()");

            //checking the checkList
             String lifeCycleStage= driver.findElement(By.xpath(uiElementMapper.getElement("life.cycle.stage"))).getText();

            if(lifeCycleStage.contains("Development")){
              log.info("lifecycle is at the Testing stage");

            driver.findElement(By.id(uiElementMapper.getElement("life.cycle.add.option"))).click();
            Thread.sleep(1500);
            driver.findElement(By.id(uiElementMapper.getElement("life.cycle.add.option1"))).click();
            Thread.sleep(3000);
            driver.findElement(By.id(uiElementMapper.getElement("life.cycle.add.option2"))).click();
            Thread.sleep(1500);

            //promoting the lifecycle
            driver.findElement(By.id(uiElementMapper.getElement("life.cycle.promote"))).click();


            driver.findElement(By.cssSelector(uiElementMapper.getElement("life.cycle.promote.ok.button"))).click();

            String nextLifeCycleStage= driver.findElement(By.xpath(uiElementMapper.getElement("life.cycle.stage"))).getText();

                  if(nextLifeCycleStage.contains("Testing")){
                      log.info("lifecycle is at the Testing stage");


                  }  else {
                      log.info("lifecycle is not  at the Testing stage");
                      throw new NoSuchElementException();
                  }

            } else {
                  log.info("lifecycle is not  at the Development stage");
                  throw new NoSuchElementException();
                   }


            String lifeCycleStage2= driver.findElement(By.xpath(uiElementMapper.getElement("life.cycle.stage"))).getText();


            if(lifeCycleStage2.contains("Testing")){
                log.info("lifecycle is promoting from  Testing stage");

                driver.findElement(By.id(uiElementMapper.getElement("life.cycle.add.option"))).click();
                Thread.sleep(1000);
                driver.findElement(By.id(uiElementMapper.getElement("life.cycle.add.option1"))).click();
                Thread.sleep(1000);
                driver.findElement(By.id(uiElementMapper.getElement("life.cycle.add.option2"))).click();

                Thread.sleep(1000);
                //promoting the lifecycle
                driver.findElement(By.id(uiElementMapper.getElement("life.cycle.promote"))).click();
                driver.findElement(By.cssSelector(uiElementMapper.getElement("life.cycle.promote.ok.button"))).click();
                Thread.sleep(1000);

                String FinalLifeCycleStage = driver.findElement(By.xpath(uiElementMapper.getElement("life.cycle" +
                        ".stage"))).getText();
                if(FinalLifeCycleStage.contains("Production")){
                    log.info("lifecycle is at the production stage");
                    driver.findElement(By.id(uiElementMapper.getElement("life.cycle.publish"))).click();
                    driver.findElement(By.cssSelector(uiElementMapper.getElement("life.cycle.promote.ok.button"))).click();


                }
                else {
                    log.info("lifecycle is not at the production stage");
                    throw new NoSuchElementException();

                }

            }

            else {
                log.info("cannot promote the lifecycle its not at the Testing stage");
                throw new NoSuchElementException();
            }

        }



    public boolean promoteApiLifecycle(String apiName,String lifeCycleName) throws InterruptedException {


        log.info(apiName);
        Thread.sleep(5000);

        String firstElementXpath ="/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/div/" +
                "form[4]/table/tbody/tr/td/a";
        String apiNameOnServer = driver.findElement(By.xpath(firstElementXpath)).getText();
        log.info(apiNameOnServer);
        if (apiName.equals(apiNameOnServer)) {
            log.info("Uploaded Api exists");
           driver.findElement(By.xpath(firstElementXpath)).click();
           lifeCyclePromotion(lifeCycleName);
           return true;
        } else {
            String resourceXpath = "/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/div/" +
                    "form[4]/table/tbody/tr[";
            String resourceXpath2 = "]/td/a";
            for (int i = 2; i < 10; i++) {
                String apiNameOnAppServer = resourceXpath + i + resourceXpath2;

                String actualApiName = driver.findElement(By.xpath(apiNameOnAppServer)).getText();
                log.info("val on app is -------> " + actualApiName);
                log.info("Correct is    -------> " + apiName);

                try {
                    if (apiName.contains(actualApiName)) {
                        log.info("Uploaded API    exists");
                        driver.findElement(By.xpath(apiNameOnAppServer)).click();
                        lifeCyclePromotion(lifeCycleName);
                        return true;
                    } else {
                        return false;
                    }
                } catch (NoSuchElementException ex) {
                    log.info("Cannot Find the Uploaded API");

                }
            }
        }
        return false;
    }

    public boolean checkFilterStatePersistence(String lifecycleName, String lifecycleState) {
        driver.findElement(By.linkText(uiElementMapper.getElement("api.list.link"))).click();
        String lcState = lifecycleState;
        String lcName = lifecycleName;
        new Select(driver.findElement(By.id("stateList"))).selectByVisibleText(lcState);
        new Select(driver.findElement(By.id("lifeCycleList"))).selectByVisibleText(lcName);

        driver.findElement(By.xpath(uiElementMapper.getElement("filter.search.button"))).click();

        String listSecondPageXpath = "/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/div/form[4]/table[2]/tbody/tr/td/a[2]";

        try {
            String paginationText = driver.findElement(By.xpath(listSecondPageXpath))
                    .getText();
            if (paginationText.equals("2")) {
                driver.findElement(By.xpath(listSecondPageXpath))
                        .click();
                String newlcState = new Select(driver.findElement(By.id("stateList"))).getFirstSelectedOption()
                        .getText();
                String newlcName = new Select(driver.findElement(By.id("lifeCycleList"))).getFirstSelectedOption()
                        .getText();

                if (newlcName.equals(lcName) && newlcState.equals(lcState)) {
                    log.info("Filter state was presisted correctly");
                    return true;
                } else {
                    log.info("Filter state was not presisted correctly");
                    return false;
                }
            }
        } catch (NoSuchElementException ex) {
            log.info("Not enough API's in the list to check Filter State with pagination");
            return false;
        }

        return false;
    }

    public LoginPage logout() throws IOException {
        driver.findElement(By.linkText(uiElementMapper.getElement("home.greg.sign.out.xpath"))).click();
        return new LoginPage(driver);
    }

}
