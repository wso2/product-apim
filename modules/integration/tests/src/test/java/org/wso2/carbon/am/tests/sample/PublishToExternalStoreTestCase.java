/*
 *  Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.carbon.am.tests.sample;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.wso2.carbon.am.tests.APIManagerIntegrationTest;
import org.wso2.carbon.automation.core.BrowserManager;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class PublishToExternalStoreTestCase extends APIManagerIntegrationTest {

    private WebDriver driver;

    private static final String TENANT_DOMAIN = "tenant13.org";
    private static final String TENANT_FIRST_NAME = "FirstName";
    private static final String TENANT_LAST_NAME = "LastName";
    private static final String TENANT_ADMIN_USER_NAME = "t13Admin";
    private static final String TENANT_ADMIN_PASSWORD = "t13AdminPassword";
    private static final String EMAIL = "test@wso2.com";
    private static final String API_NAME = "CreateAndPublishTestAPI";
    private static final String API_VERSION = "1.0.0";

    private String storeUrl;
    private String baseUrl;

    @Override
    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init(0);
        driver = BrowserManager.getWebDriver();
        baseUrl = "https://localhost:9443/";
        storeUrl = "http://localhost:9763/";
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);

        String apiManagerXml =
                ProductConstant.getResourceLocations(ProductConstant.AM_SERVER_NAME) +
                        File.separator +
                        "configFiles/externalstoretest/" +
                        "api-manager.xml";

        ServerConfigurationManager serverConfigurationManager = new ServerConfigurationManager(amServer.getBackEndUrl());
        serverConfigurationManager.applyConfiguration(new File(apiManagerXml));

    }

    @Test(groups = "wso2.am", description = "This method creates a tenant")
    public void createTenantTestCase() throws Exception {
        driver.get(baseUrl + "/carbon/admin/login.jsp");
        driver.findElement(By.id("txtUserName")).clear();
        driver.findElement(By.id("txtUserName")).sendKeys("admin");
        driver.findElement(By.id("txtPassword")).clear();
        driver.findElement(By.id("txtPassword")).sendKeys("admin");
        driver.findElement(By.name("rememberMe")).click();
        driver.findElement(By.cssSelector("input.button")).click();
        driver.findElement(By.cssSelector("#menu-panel-button4 > span")).click();
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
        driver.findElement(By.linkText("Sign-out")).click();
        Assert.assertTrue(true);
    }


    @Test(groups = "wso2.am", description = "This method publishes the Test API to the tenant store and super store", dependsOnMethods = {"createTenantTestCase"})
    public void createAndPublishToExternalStoreTestCase() throws Exception {
        driver.get(storeUrl + "/publisher/site/pages/login.jag");
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys(TENANT_ADMIN_USER_NAME + "@" + TENANT_DOMAIN);
        driver.findElement(By.id("pass")).clear();
        driver.findElement(By.id("pass")).sendKeys(TENANT_ADMIN_PASSWORD);
        driver.findElement(By.id("loginButton")).click();
        driver.findElement(By.linkText("New API...")).click();
        driver.findElement(By.id("name")).clear();
        driver.findElement(By.id("name")).sendKeys(API_NAME);
        driver.findElement(By.id("context")).clear();
        driver.findElement(By.id("context")).sendKeys("testapi");
        driver.findElement(By.id("version")).clear();
        driver.findElement(By.id("version")).sendKeys(API_VERSION);
        driver.findElement(By.id("description")).clear();
        driver.findElement(By.id("description")).sendKeys("This is test API");
        driver.findElement(By.id("resource_url_pattern")).clear();
        driver.findElement(By.id("resource_url_pattern")).sendKeys("testapi");
        driver.findElement(By.cssSelector("input.http_verb_select")).click();
        driver.findElement(By.xpath("//input[@value='POST']")).click();
        driver.findElement(By.id("add_resource")).click();
        driver.findElement(By.id("go_to_implement")).click();
        driver.findElement(By.id("jsonform-0-elt-production_endpoints")).clear();
        driver.findElement(By.id("jsonform-0-elt-production_endpoints")).sendKeys("whatever");
        driver.findElement(By.id("go_to_manage")).click();
        driver.findElement(By.cssSelector("div.btn-group > button.multiselect.dropdown-toggle.btn")).click();
        driver.findElement(By.xpath("//input[@value='Gold']")).click();
        driver.findElement(By.xpath("//input[@value='Silver']")).click();
        driver.findElement(By.xpath("//input[@value='Unlimited']")).click();
        driver.findElement(By.cssSelector("#api_designer > legend")).click();
        driver.findElement(By.id("publish_api")).click();
        driver.findElement(By.linkText("External API Stores")).click();
        driver.findElement(By.id("store0.0")).click();
        driver.findElement(By.id("updateButtonExt")).click();

        Assert.assertTrue(driver.getPageSource().contains(API_NAME));
        driver.close();
    }


    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        driver.quit();
        super.cleanup();
    }
}
