/*
*Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.am.integration.ui;


import java.util.concurrent.TimeUnit;

import org.apache.http.protocol.HttpContext;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.am.integration.ui.util.TestUtil;
import org.wso2.carbon.automation.api.selenium.login.LoginPage;
import org.wso2.carbon.automation.api.selenium.tenant.TenantHomePage;
import org.wso2.carbon.automation.api.selenium.tenant.TenantListpage;
import org.wso2.carbon.automation.core.BrowserManager;
import org.wso2.carbon.automation.core.ProductConstant;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class SubscriptionAvailabilityTestCase extends AMIntegrationUiTestBase {
	private WebDriver driver;
	
	private static final String TEST_DATA_TENANT_DOMAIN = "test3.org";
	private static final String TEST_DATA_TENANT_FIRST_NAME = "testname";
	private static final String TEST_DATA_TENANT_LAST_NAME = "testLastName";
	private static final String TEST_DATA_ADMIN_USER_NAME = "testAdmin";
	private static final String TEST_DATA_PASSWORD = "tenantpassword";
	private static final String TEST_DATA_EMAIL = "test@wso2.com";
	private static final String TEST_DATA_API_NAME = "SubAvailabilityTestAPI";
	private static final String TEST_DATA_API_VERSION = "1.0.0";
	private static final String SUPER_TENANT_DOMAIN_NAME = "carbon.super";
	
    
	@BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init(0);
        driver = BrowserManager.getWebDriver();
        driver.get(getLoginURL(ProductConstant.AM_SERVER_NAME));
	}
	
	@Test(groups = "wso2.am", description = "This method adds and publishes the Test API in carbon.super store")
    public void testcreateAndPublishAPI() throws Exception {
		String loginURL = getPublisherURL(ProductConstant.AM_SERVER_NAME);		
		HttpContext httpContext = TestUtil.login(userInfo.getUserName(), userInfo.getPassword(), loginURL);			
	
		assertTrue(TestUtil.addAPI(userInfo.getUserName(), TEST_DATA_API_NAME, TEST_DATA_API_VERSION, httpContext, loginURL));
		assertTrue(TestUtil.publishAPI(userInfo.getUserName(), TEST_DATA_API_NAME, TEST_DATA_API_VERSION, httpContext, loginURL));
		
        System.out.println("API Create and Publish test case is completed ");
    }
	
	public void testCreateTenant() throws Exception {
		
	}
	
	@Test(groups = "wso2.am", description = "This method creates the test tenant")
    public void testTenantCreation() throws Exception {

        LoginPage test = new LoginPage(driver);
        test.loginAs(userInfo.getUserName(), userInfo.getPassword());
        TenantHomePage addNewTenantHome = new TenantHomePage(driver);

        addNewTenantHome.addNewTenant(TEST_DATA_TENANT_DOMAIN, TEST_DATA_TENANT_FIRST_NAME, TEST_DATA_TENANT_LAST_NAME, 
                                      TEST_DATA_ADMIN_USER_NAME, TEST_DATA_PASSWORD, TEST_DATA_EMAIL);
        TenantListpage tenantListpage = new TenantListpage(driver);
        tenantListpage.checkOnUplodedTenant(TEST_DATA_TENANT_DOMAIN);
    }
	
	@Test(groups = "wso2.am", description = "verify visibility of subscription option", 
			dependsOnMethods = {"testcreateAndPublishAPI","testTenantCreation"})
	public void testSubscription() throws Exception {
		
		//Go to the Tenant store and click Login
		driver.get(getStoreURL(ProductConstant.AM_SERVER_NAME) + "?tenant=" + TEST_DATA_TENANT_DOMAIN);
        WebDriverWait wait = new WebDriverWait(driver, 30);
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.linkText("Login")));
        driver.findElement(By.linkText("Login")).click();
        
        //Find and fill Username
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));
        WebElement usernameEle = driver.findElement(By.id("username"));
        usernameEle.sendKeys(TEST_DATA_ADMIN_USER_NAME + "@" + TEST_DATA_TENANT_DOMAIN);
        
        //Find and fill Password
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));
        WebElement passwordEle = driver.findElement(By.id("password"));                                                               
        passwordEle.sendKeys(TEST_DATA_PASSWORD);
        
        //find Login button and click on it.
        driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
        driver.findElement(By.id("loginBtn")).click();
        
        //Go to Public store
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Public API Store")));
        driver.findElement(By.linkText("Public API Store")).click();
        
        //Go to Carbon.super store
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("a[href*='carbon.super']")));
        driver.findElement(By.cssSelector("a[href*='carbon.super']")).click();
        
        //select API
        driver.navigate().to(getStoreURL(ProductConstant.AM_SERVER_NAME) + "/apis/info?name=" + TEST_DATA_API_NAME 
                             + "&version=" + TEST_DATA_API_VERSION + "&provider=" + userInfo.getUserName() + "&tenant=" + SUPER_TENANT_DOMAIN_NAME);        
        
      
        assertFalse(driver.getPageSource().contains("subscribe-button"), "Subscription availability of " + userInfo.getUserName() + "-" 
        				+ TEST_DATA_API_NAME + "-" + TEST_DATA_API_VERSION + " is \'Available to current Tenant only\'. " +
        						"Hence Subscribe option shouldn't be visible to Tenant " + TEST_DATA_TENANT_DOMAIN);
        driver.close();
        
	}


}
