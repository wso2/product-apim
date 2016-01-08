/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.wso2.am.integration.ui.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.ui.tests.util.TestUtil;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class APIMANAGER4139SubscriptionAfterRecreationTestCase extends APIMIntegrationUiTestBase {

    private WebDriver driver;
    private static final String TENANT_DOMAIN = "tenant4139.org";
    private static final String TENANT_FIRST_NAME = "FirstName";
    private static final String TENANT_LAST_NAME = "LastName";
    private static final String TENANT_ADMIN_USER_NAME = "t4139Admin";
    private static final String TENANT_ADMIN_PASSWORD = "t4139Password";
    private static final String EMAIL = "test@wso2.com";
    private static final String API_NAME = "APIManager4139TestAPI";
    private static final String API_CONTEXT = "api4139testAPI";
    private static final String API_VERSION = "1.0.0";
    private String baseUrl;
    private String storeUrl;


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        super.init();
        driver = BrowserManager.getWebDriver();
        baseUrl = "https://localhost:9443/";
        storeUrl = "http://localhost:9763/";
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
    }

    @Test(groups = "wso2.am", description = "This method creates a tenant for APIMANAGER4139")
    public void createTenantTestCase() throws Exception {
        driver.get(baseUrl + "/carbon/admin/login.jsp");
        driver.findElement(By.id("txtUserName")).clear();
        driver.findElement(By.id("txtUserName")).sendKeys("admin");
        driver.findElement(By.id("txtPassword")).clear();
        driver.findElement(By.id("txtPassword")).sendKeys("admin");
        driver.findElement(By.name("rememberMe")).click();
        driver.findElement(By.cssSelector("input.button")).click();
        driver.findElement(By.cssSelector("#menu-panel-button3 > span")).click();
        driver.findElement(By.linkText("Add New Tenant")).click();
        driver.findElement(By.id("domain")).clear();
        driver.findElement(By.id("domain")).sendKeys(TENANT_DOMAIN);
        driver.findElement(By.id("admin-firstname")).clear();
        driver.findElement(By.id("admin-firstname")).sendKeys(TENANT_FIRST_NAME);
        driver.findElement(By.id("admin-lastname")).clear();
        driver.findElement(By.id("admin-lastname")).sendKeys(TENANT_LAST_NAME);
        driver.findElement(By.id("admin")).clear();
        driver.findElement(By.id("admin")).sendKeys(TENANT_ADMIN_USER_NAME);
        driver.findElement(By.id("admin-password")).clear();
        driver.findElement(By.id("admin-password")).sendKeys(TENANT_ADMIN_PASSWORD);
        driver.findElement(By.id("admin-password-repeat")).clear();
        driver.findElement(By.id("admin-password-repeat")).sendKeys(TENANT_ADMIN_PASSWORD);
        driver.findElement(By.id("admin-email")).clear();
        driver.findElement(By.id("admin-email")).sendKeys(EMAIL);
        driver.findElement(By.cssSelector("input.button")).click();
        driver.findElement(By.cssSelector("button[type=\"button\"]")).click();
        assertTrue(driver.getPageSource().contains(TENANT_DOMAIN));

    }

    @Test(groups = "wso2.am", description = "This method publishes the Test API", dependsOnMethods = {"createTenantTestCase"})
    public void createAndPublishAPI() throws Exception {
        driver.get(storeUrl + "/publisher/site/pages/login.jag");
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys(TENANT_ADMIN_USER_NAME + "@" + TENANT_DOMAIN);
        driver.findElement(By.id("pass")).clear();
        driver.findElement(By.id("pass")).sendKeys(TENANT_ADMIN_PASSWORD);
        driver.findElement(By.id("loginButton")).click();

        driver.findElement(By.linkText("Add")).click();
        driver.findElement(By.id("name")).clear();
        driver.findElement(By.id("name")).sendKeys(API_NAME);
        driver.findElement(By.id("context")).clear();
        driver.findElement(By.id("context")).sendKeys(API_CONTEXT);
        driver.findElement(By.id("version")).clear();
        driver.findElement(By.id("version")).sendKeys(API_VERSION);
        driver.findElement(By.id("resource_url_pattern")).clear();
        driver.findElement(By.id("resource_url_pattern")).sendKeys("testapi");
        driver.findElement(By.cssSelector("input.http_verb_select")).click();
        driver.findElement(By.xpath("//input[@value='POST']")).click();
        driver.findElement(By.xpath("//input[@value='PUT']")).click();
        driver.findElement(By.xpath("//input[@value='DELETE']")).click();
        driver.findElement(By.xpath("//input[@value='OPTIONS']")).click();
        driver.findElement(By.id("add_resource")).click();
        driver.findElement(By.id("go_to_implement")).click();
        driver.findElement(By.id("jsonform-0-elt-production_endpoints")).clear();
        driver.findElement(By.id("jsonform-0-elt-production_endpoints")).sendKeys("hello");
        driver.findElement(By.id("go_to_manage")).click();

        driver.findElement(By.cssSelector("div.btn-group > button.multiselect.dropdown-toggle.btn")).click();
        driver.findElement(By.xpath("//input[@value='Gold']")).click();
        driver.findElement(By.xpath("//input[@value='Silver']")).click();
        driver.findElement(By.xpath("//input[@value='Unlimited']")).click();
        driver.findElement(By.cssSelector("#api_designer > legend")).click();
        driver.findElement(By.id("publish_api")).click();
        assertTrue(driver.getPageSource().contains(API_NAME));
    }


    @Test(groups = "wso2.am", description = "This method publishes the Test API", dependsOnMethods = {"createAndPublishAPI"})
    public void subscribeAPI() throws Exception {
        driver.get(baseUrl + "/store/");
        driver.findElement(By.cssSelector("center > h3")).click();
        driver.findElement(By.linkText("APIs")).click();
        driver.findElement(By.id("login-link")).click();
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("password")).clear();
        driver.findElement(By.id("password")).sendKeys("admin");
        driver.findElement(By.xpath("//div[@id='wrap']/div[6]/div/div/div/form/button")).click();
        driver.findElement(By.linkText("My Subscriptions")).click();
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("password")).clear();
        driver.findElement(By.id("password")).sendKeys("admin");
        driver.findElement(By.id("loginBtn")).click();
        driver.findElement(By.linkText(API_NAME)).click();
        new Select(driver.findElement(By.id("tiers-list"))).selectByVisibleText("Unlimited");
        driver.findElement(By.id("subscribe-button")).click();
        driver.findElement(By.linkText("Go to My Subscriptions")).click();
        driver.findElement(By.xpath("//button[@onclick=\"jagg.sessionAwareJS({redirect:'/site/pages/subscriptions.jag'})\"]")).click();
        driver.findElement(By.cssSelector("img.closeBtn")).click();
        driver.findElement(By.linkText("Yes")).click();
        driver.findElement(By.linkText("APIs")).click();
        assertTrue(driver.getPageSource().contains(API_NAME));
    }


    @Test(groups = "wso2.am", description = "This method publishes the Test API", dependsOnMethods = {"subscribeAPI"})
    public void DeleteAPI() throws Exception {
        driver.get(storeUrl + "/publisher/site/pages/login.jag");
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys(TENANT_ADMIN_USER_NAME + "@" + TENANT_DOMAIN);
        driver.findElement(By.id("pass")).clear();
        driver.findElement(By.id("pass")).sendKeys(TENANT_ADMIN_PASSWORD);
        driver.findElement(By.id("loginButton")).click();
        driver.findElement(By.cssSelector("button.close.btn-api-del")).click();
        driver.findElement(By.linkText("Yes")).click();
        assertFalse(driver.getPageSource().contains(API_NAME));
    }

    @Test(groups = "wso2.am", description = "This method publishes the Test API", dependsOnMethods = {"DeleteAPI"})
    public void recreateAPI() throws Exception {
        createAndPublishAPI();
    }

    @Test(groups = "wso2.am", description = "This method publishes the Test API", dependsOnMethods = {"recreateAPI"})
    public void loadSubscriptionPage() throws Exception {
        driver.findElement(By.linkText("My Subscriptions")).click();
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys("admin");
        driver.findElement(By.id("password")).clear();
        driver.findElement(By.id("password")).sendKeys("admin");
        driver.findElement(By.id("loginBtn")).click();
        assertTrue(driver.getPageSource().contains(API_NAME));
    }


    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        TestUtil.cleanUp(TENANT_ADMIN_USER_NAME + "@" + TENANT_DOMAIN, TENANT_ADMIN_PASSWORD,
                storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());
        if (driver != null) {
            driver.quit();
        }
    }

}
