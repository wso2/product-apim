package org.wso2.carbon.am.integration.ui;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.wso2.carbon.am.integration.ui.util.APIMTestConstants;
import org.wso2.carbon.automation.core.BrowserManager;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.api.selenium.login.LoginPage;
import org.wso2.carbon.automation.api.selenium.tenant.TenantHomePage;
import org.wso2.carbon.automation.api.selenium.tenant.TenantListpage;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


/**
 * Created by nisala on 2/23/15.
 */
public class VisibilityTestCase extends AMIntegrationUiTestBase {
    private WebDriver driver;
    private static final String USER_NAME = "admin";
    private static final CharSequence PASSWORD = "admin";


    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        super.init();
        driver = BrowserManager.getWebDriver();
        //driver.get(getLoginURL(ProductConstant.AM_SERVER_NAME));
        driver.get(getPublisherURL(ProductConstant.AM_SERVER_NAME));
    }
    public void testTenantCreate()throws Exception{
        driver.get(getLoginURL(ProductConstant.AM_SERVER_NAME));
        driver.findElement(By.id(APIMTestConstants.APIMANAGEMENTCONSOLE_LOGIN_USERNAME_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIMANAGEMENTCONSOLE_LOGIN_USERNAME_ID)).
                sendKeys(APIMTestConstants.APIMANAGEMENTCONSOLE_LOGIN_USERNAME);
        driver.findElement(By.id(APIMTestConstants.APIMANAGEMENTCONSOLE_LOGIN_PASSWORD_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIMANAGEMENTCONSOLE_LOGIN_PASSWORD_ID)).
                sendKeys(APIMTestConstants.APIMANAGEMENTCONSOLE_LOGIN_PASSWORD);
        driver.findElement(By.cssSelector("input.button")).click();
        driver.findElement(By.cssSelector("#menu-panel-button3 > span")).click();
        driver.findElement(By.linkText("Add New Tenant")).click();
        driver.findElement(By.id(APIMTestConstants.APIMCONSOLE_ADDTENANT_DOMAIN_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIMCONSOLE_ADDTENANT_DOMAIN_ID)).
                sendKeys(APIMTestConstants.APIMCONSOLE_DOMAIN);
        driver.findElement(By.id(APIMTestConstants.APIMCONSOLE_ADDTENANT_FIRSTNAME_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIMCONSOLE_ADDTENANT_FIRSTNAME_ID)).
                sendKeys(APIMTestConstants.APIMCONSOLE_ADDTENANT_FIRSTNAME);
        driver.findElement(By.id(APIMTestConstants.APIMCONSOLE_ADDTENANT_LASTNAME_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIMCONSOLE_ADDTENANT_LASTNAME_ID)).
                sendKeys(APIMTestConstants.APIMCONSOLE_ADDTENANT_LASTNAME);
        driver.findElement(By.id(APIMTestConstants.APIMCONSOLE_ADDTENANT_USERNAME_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIMCONSOLE_ADDTENANT_USERNAME_ID)).sendKeys(APIMTestConstants.APIMCONSOLE_ADDTENANT_USERNAME);
        driver.findElement(By.id(APIMTestConstants.APIMCONSOLE_ADDTENANT_PASSW0RD_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIMCONSOLE_ADDTENANT_PASSW0RD_ID)).
                sendKeys(APIMTestConstants.APIMCONSOLE_ADDTENANT_PASSW0RD);
        driver.findElement(By.id(APIMTestConstants.APIMCONSOLE_ADDTENANT_PASSWORDREPEAT_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIMCONSOLE_ADDTENANT_PASSWORDREPEAT_ID)).
                sendKeys(APIMTestConstants.APIMCONSOLE_ADDTENANT_PASSWORDREPEAT);
        driver.findElement(By.id(APIMTestConstants.APIMCONSOLE_ADDTENANT_EMAIL_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIMCONSOLE_ADDTENANT_EMAIL_ID)).
                sendKeys(APIMTestConstants.APIMCONSOLE_ADDTENANT_EMAIL);
        driver.findElement(By.cssSelector("input.button")).click();
        driver.findElement(By.cssSelector("button[type=\"button\"]")).click();
        driver.findElement(By.linkText("Sign-out")).click();

    }

    @Test(groups = "wso2.am", priority=1,description = "verify visibility options with only super tenant")
    public void testVisibilityWithOnlySuperTenant() throws Exception {
        driver.get(getPublisherURL(ProductConstant.AM_SERVER_NAME));
        (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.visibilityOfElementLocated(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_USERNAME_ID)));
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_USERNAME_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_USERNAME_ID)).
                sendKeys(APIMTestConstants.APIPUBLISHER_LOGIN_USERNAME);
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_PASSWORD_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_PASSWORD_ID)).
                sendKeys(APIMTestConstants.APIPUBLISHER_LOGIN_PASSWORD);
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_BUTTON_ID)).click();
        (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.presenceOfElementLocated
                        (By.linkText(APIMTestConstants.APIPUBLISHER_ADD_LINKTEXT)));
        driver.findElement(By.linkText(APIMTestConstants.APIPUBLISHER_ADD_LINKTEXT)).click();
        WebElement visibility = driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APIVISIBILITY_ID));
        String[] visibilityOptions = visibility.getText().split("\n");
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_USERMENU_ID)).click();
        driver.findElement(By.cssSelector(APIMTestConstants.APIPUBLISHER_LOGOUT_BUTTONCSS)).click();
        assertEquals(visibilityOptions.length,2);

    }
    @Test(groups = "wso2.am", priority=2,description = "verify subscriptions options with only super tenant")
    public void testSubcriptionsWithOnlySuperTenant() throws Exception {
        driver.get(getPublisherURL(ProductConstant.AM_SERVER_NAME));
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_USERNAME_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_USERNAME_ID)).
                sendKeys(APIMTestConstants.APIPUBLISHER_LOGIN_USERNAME);
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_PASSWORD_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_PASSWORD_ID)).
                sendKeys(APIMTestConstants.APIPUBLISHER_LOGIN_PASSWORD);
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_BUTTON_ID)).click();
        (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.presenceOfElementLocated
                        (By.linkText(APIMTestConstants.APIPUBLISHER_ADD_LINKTEXT)));
        driver.findElement(By.linkText(APIMTestConstants.APIPUBLISHER_ADD_LINKTEXT)).click();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APINAME_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APINAME_ID)).sendKeys(APIMTestConstants.APIPUBLISHER_ADD_APINAME_SUPERTENANT);
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APICONTEXT_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APICONTEXT_ID)).sendKeys(APIMTestConstants.APIPUBLISHER_ADD_APICONTEXT_SUPERTENANT);
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APIVERSION_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APIVERSION_ID)).sendKeys(APIMTestConstants.APIPUBLISHER_ADD_APIVERSION);
        new Select(driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APIVISIBILITY_ID))).selectByVisibleText(APIMTestConstants.APIPUBLISHER_ADD_APIVISIBILITY);
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APIRESOURCEURL_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APIRESOURCEURL_ID)).sendKeys(APIMTestConstants.APIPUBLISHER_ADD_APIRESOURCEURL);
        driver.findElement(By.cssSelector(APIMTestConstants.APIPUBLISHER_ADD_HTTPVERB_CSS)).click();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_ADDRESOURCE_ID)).click();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_IMPLEMENT_ID)).click();

        (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//form[@id='implement_form']/div/fieldset/div/div/label[2]")));
        driver.findElement(By.xpath("//form[@id='implement_form']/div/fieldset/div/div/label[2]")).click();
        driver.findElement(By.id("go_to_manage")).click();
        (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[@type='button']")));

        boolean isSubcriptionAvailabel;
        try {
            WebElement listW = driver.findElement(By.id("subscriptions"));
            isSubcriptionAvailabel = true;
        } catch (NoSuchElementException e) {
            isSubcriptionAvailabel = false;
        }
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_USERMENU_ID)).click();
        driver.findElement(By.cssSelector(APIMTestConstants.APIPUBLISHER_LOGOUT_BUTTONCSS)).click();
        assertFalse(isSubcriptionAvailabel);
    }
    @Test(groups = "wso2.am",priority=3, description = "verify visibility options with multiple tenants")
    public void testVisibilityWithMultipleTenants() throws Exception {
        testTenantCreate();
        driver.get(getPublisherURL(ProductConstant.AM_SERVER_NAME));
        (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.presenceOfElementLocated(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_USERNAME_ID)));
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_USERNAME_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_USERNAME_ID)).
                sendKeys(APIMTestConstants.APIPUBLISHER_LOGIN_USERNAME);
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_PASSWORD_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_PASSWORD_ID)).
                sendKeys(APIMTestConstants.APIPUBLISHER_LOGIN_PASSWORD);
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_BUTTON_ID)).click();
        (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.presenceOfElementLocated
                        (By.linkText(APIMTestConstants.APIPUBLISHER_ADD_LINKTEXT)));
        driver.findElement(By.linkText(APIMTestConstants.APIPUBLISHER_ADD_LINKTEXT)).click();
        WebElement visibility = driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APIVISIBILITY_ID));
        String[] visibilityOptions = visibility.getText().split("\n");
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_USERMENU_ID)).click();
        driver.findElement(By.cssSelector(APIMTestConstants.APIPUBLISHER_LOGOUT_BUTTONCSS)).click();
        assertEquals(visibilityOptions.length,3);
    }
    @Test(groups = "wso2.am", priority=4,description = "verify subscriptions options with multiple tenants")
    public void testSubcriptionsWithMultipleTenants() throws Exception {
        driver.get(getPublisherURL(ProductConstant.AM_SERVER_NAME));
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_USERNAME_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_USERNAME_ID)).
                sendKeys(APIMTestConstants.APIPUBLISHER_LOGIN_USERNAME);
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_PASSWORD_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_PASSWORD_ID)).
                sendKeys(APIMTestConstants.APIPUBLISHER_LOGIN_PASSWORD);
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_LOGIN_BUTTON_ID)).click();
        (new WebDriverWait(driver, 10)).until(ExpectedConditions.presenceOfElementLocated
                (By.linkText(APIMTestConstants.APIPUBLISHER_ADD_LINKTEXT)));
        driver.findElement(By.linkText(APIMTestConstants.APIPUBLISHER_ADD_LINKTEXT)).click();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APINAME_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APINAME_ID)).
                sendKeys(APIMTestConstants.APIPUBLISHER_ADD_APINAME_MultipleTENANT);
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APICONTEXT_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APICONTEXT_ID)).
                sendKeys(APIMTestConstants.APIPUBLISHER_ADD_APICONTEXT_MultipleTENANT);
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APIVERSION_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APIVERSION_ID)).
                sendKeys(APIMTestConstants.APIPUBLISHER_ADD_APIVERSION);
        new Select(driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APIVISIBILITY_ID))).
                selectByVisibleText(APIMTestConstants.APIPUBLISHER_ADD_APIVISIBILITY);
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APIRESOURCEURL_ID)).clear();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_APIRESOURCEURL_ID)).
                sendKeys(APIMTestConstants.APIPUBLISHER_ADD_APIRESOURCEURL);
        driver.findElement(By.cssSelector(APIMTestConstants.APIPUBLISHER_ADD_HTTPVERB_CSS)).click();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_ADDRESOURCE_ID)).click();
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_ADD_IMPLEMENT_ID)).click();

        (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//form[@id='implement_form']/div/fieldset/div/div/label[2]")));
        driver.findElement(By.xpath("//form[@id='implement_form']/div/fieldset/div/div/label[2]")).click();
        driver.findElement(By.id("go_to_manage")).click();
        (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[@type='button']")));

        boolean isSubcriptionAvailabel;
        try {
            WebElement listW = driver.findElement(By.id("subscriptions"));
            isSubcriptionAvailabel = true;
        } catch (NoSuchElementException e) {
            isSubcriptionAvailabel = false;
        }
        driver.findElement(By.id(APIMTestConstants.APIPUBLISHER_USERMENU_ID)).click();
        driver.findElement(By.cssSelector(APIMTestConstants.APIPUBLISHER_LOGOUT_BUTTONCSS)).click();
        assertTrue(isSubcriptionAvailabel);
    }



}
