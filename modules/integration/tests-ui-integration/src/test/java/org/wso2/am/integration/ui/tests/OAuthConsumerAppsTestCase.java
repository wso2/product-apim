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

package org.wso2.am.integration.ui.tests;

import static org.testng.Assert.assertTrue;

import org.apache.http.protocol.HttpContext;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.ui.pages.login.LoginPage;
import org.wso2.am.integration.ui.pages.tenant.TenantHomePage;
import org.wso2.am.integration.ui.pages.tenant.TenantListpage;
import org.wso2.am.integration.ui.pages.util.UIElementMapper;
import org.wso2.am.integration.ui.tests.util.TestUtil;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;

/**
 * This Test requires APIM and IS as a key manager setup
 * JIRA : https://wso2.org/jira/browse/APIMANAGER-3125
 *
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.PLATFORM})
public class OAuthConsumerAppsTestCase extends AMIntegrationUiTestBase {
	
	private WebDriver driver;
	private UIElementMapper uiElementMapper;
	
	private static final String TEST_DATA_TENANT_DOMAIN = "test4.org";
	private static final String TEST_DATA_TENANT_FIRST_NAME = "testname";
	private static final String TEST_DATA_TENANT_LAST_NAME = "testLastName";
	private static final String TEST_DATA_ADMIN_USER_NAME = "testAdmin";
	private static final String TEST_DATA_PASSWORD = "tenantpassword";
	private static final String TEST_DATA_EMAIL = "test@wso2.com";
	private static final String TEST_DATA_API_NAME = "OAuthAppTestAPI";
	private static final String TEST_DATA_API_VERSION = "1.0.0";
	private static final String DEFAULT_TIER = "Unlimited";
	private static final String DEFAULT_APPNAME = "DefaultApplication";
	private static final String DEFAULT_KEY_TYPE = "PRODUCTION";

	
	@BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        driver = BrowserManager.getWebDriver();
        this.uiElementMapper = UIElementMapper.getInstance();
        
        driver.get(getLoginURL());
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
	
	@Test(groups = "wso2.am", description = "This method adds and publishes the Test API in carbon.super store",
			dependsOnMethods = {"testTenantCreation"})
    public void testcreateAndPublishAPI() throws Exception {
		String loginURL = getPublisherURL();
		String tenantUserName = TestUtil.getTenantUserName(TEST_DATA_ADMIN_USER_NAME , TEST_DATA_TENANT_DOMAIN);
		HttpContext httpContext = TestUtil.login(tenantUserName, TEST_DATA_PASSWORD, loginURL);			
	
		assertTrue(TestUtil.addAPI(tenantUserName, TEST_DATA_API_NAME, TEST_DATA_API_VERSION, httpContext, loginURL));
		assertTrue(TestUtil.publishAPI(tenantUserName, TEST_DATA_API_NAME, TEST_DATA_API_VERSION, httpContext, loginURL));
		
        System.out.println("API Create and Publish test case is completed ");
    }
	
	@Test(groups = "wso2.am", description = "This method subscribes the Test API", dependsOnMethods = { "testcreateAndPublishAPI" })
	public void testSubscribeAndGenerateTokens() throws Exception {
		String loginURL = getStoreURL();
		String tenantUserName =
		                        TestUtil.getTenantUserName(TEST_DATA_ADMIN_USER_NAME,
		                                                   TEST_DATA_TENANT_DOMAIN);

		HttpContext httpContext = TestUtil.login(tenantUserName, TEST_DATA_PASSWORD, loginURL);

		assertTrue(TestUtil.addSubscription(tenantUserName, TEST_DATA_API_NAME,
		                                    TEST_DATA_API_VERSION, DEFAULT_TIER, DEFAULT_APPNAME,
		                                    httpContext, loginURL));

		assertTrue(TestUtil.generateApplicationtokens(DEFAULT_KEY_TYPE, DEFAULT_APPNAME,
		                                              httpContext, loginURL));
	}
	
	@Test(groups = "wso2.am", description = "This method checks whether OAuth Application get listed in IS", dependsOnMethods = { "testSubscribeAndGenerateTokens" })
	public void testOAuthConsumerApps() throws Exception {
		driver.get(getLoginURL());
		WebElement userNameField =
		                           driver.findElement(By.name(uiElementMapper.getElement("login.username.name")));
		WebElement passwordField =
		                           driver.findElement(By.name(uiElementMapper.getElement("login.password")));

		userNameField.sendKeys(TestUtil.getTenantUserName(TEST_DATA_ADMIN_USER_NAME,
		                                                  TEST_DATA_TENANT_DOMAIN));
		passwordField.sendKeys(TEST_DATA_PASSWORD);
		driver.findElement(By.className(uiElementMapper.getElement("login.sign.in.button")))
		      .click();

		driver.navigate().to(TestUtil.getTenantURL(getLoginURL(),
		                                           TEST_DATA_TENANT_DOMAIN,
		                                           "/carbon/oauth/index.jsp"));

		assertTrue(driver.getPageSource().contains(DEFAULT_APPNAME),
		           "Registered OAuth Consumer Application " + DEFAULT_APPNAME +
		                   "is not listed in IS");
		driver.close();
	}
	
	@AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        driver.quit();
    }

}
