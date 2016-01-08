/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.am.integration.ui.tests;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.ui.pages.login.LoginPage;
import org.wso2.am.integration.ui.pages.tenant.TenantHomePage;
import org.wso2.am.integration.ui.pages.tenant.TenantListpage;
import org.wso2.am.integration.ui.tests.util.TestUtil;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertTrue;

/*
    Need to configure LB with APIM 1.8 and run the test case
    note that replace the server urls with LB urls
 */
public class APIMANAGER3412DifferentRegistryContextPath extends APIMIntegrationUiTestBase {
    private String TEST_DATA_API_NAME = "APIMANAGER3412";
    private String TEST_DATA_API_VERSION = "1.0.0";
    private String TEST_DATA_TENANT = "apimanager3412.com";
    private String TEST_DATA_TENANT_ADMIN_USER = "admin";
    private String TEST_DATA_TENANT_ADMIN_PASSWORD = "123456";
    private String TEST_DATA_TENANT_PUBLISHER = "admin@apimanager3412.com";
    private String TEST_DATA_ICON_PATH= TestConfigurationProvider.getResourceLocation() +
                                       "/images/apimanager3412-api_icon/twitter.png";

    private WebDriver driver;
    private String publisherURL;
    private String storeURL;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        publisherURL = getPublisherURL();
        storeURL = getStoreURL();

        driver = BrowserManager.getWebDriver();
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
    }

    @Test(groups = "wso2.am", description = "Create tenant and api")
    public void createTenantAndAPI() throws Exception {
        WebDriverWait wait = new WebDriverWait(driver, 60);
        driver.get(getLoginURL());
        LoginPage login = new LoginPage(driver);
        login.loginAs(gatewayContextMgt.getContextTenant().getContextUser().getUserName(),
                      gatewayContextMgt.getContextTenant().getContextUser().getPassword());
        TenantHomePage addNewTenantHome = new TenantHomePage(driver);

        String firstName = "admin";
        String lastName = "admin";
        addNewTenantHome.addNewTenant(TEST_DATA_TENANT, firstName, lastName, TEST_DATA_TENANT_ADMIN_USER,
                                      TEST_DATA_TENANT_ADMIN_PASSWORD, TEST_DATA_TENANT_PUBLISHER);
        TenantListpage tenantListpage = new TenantListpage(driver);
        tenantListpage.checkOnUplodedTenant(TEST_DATA_TENANT);

        driver.get(publisherURL + "/site/pages/login.jag");
        driver.findElement(By.id("username")).clear();
        driver.findElement(By.id("username")).sendKeys(TEST_DATA_TENANT_PUBLISHER);
        driver.findElement(By.id("pass")).clear();
        driver.findElement(By.id("pass")).sendKeys(TEST_DATA_TENANT_ADMIN_PASSWORD);
        driver.findElement(By.id("loginButton")).click();
        driver.findElement(By.linkText("Add")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("create-new-api")));
        driver.findElement(By.id("create-new-api")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("designNewAPI")));
        driver.findElement(By.id("designNewAPI")).click();

        driver.findElement(By.id("name")).clear();
        driver.findElement(By.id("name")).sendKeys(TEST_DATA_API_NAME);
        driver.findElement(By.id("context")).clear();
        driver.findElement(By.id("context")).sendKeys(TEST_DATA_API_NAME.toLowerCase());
        driver.findElement(By.id("version")).clear();
        driver.findElement(By.id("version")).sendKeys(TEST_DATA_API_VERSION);

        driver.findElement(By.id("apiThumb")).sendKeys(TEST_DATA_ICON_PATH);

        driver.findElement(By.id("description")).clear();
        driver.findElement(By.id("description")).sendKeys("This is test API");
        driver.findElement(By.id("resource_url_pattern")).clear();
        driver.findElement(By.id("resource_url_pattern")).sendKeys("testapi");
        //driver.findElement(By.cssSelector("input.http_verb_select")).click();
        driver.findElement(By.xpath("//label[contains(.,'post')]")).click();
        driver.findElement(By.id("add_resource")).click();
        driver.findElement(By.id("go_to_implement")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@value='#managed-api']")));
        driver.findElement(By.xpath("//div[@value='#managed-api']")).click();

        driver.findElement(By.id("jsonform-0-elt-production_endpoints")).clear();
        driver.findElement(By.id("jsonform-0-elt-production_endpoints")).sendKeys("whatever");
        driver.findElement(By.id("go_to_manage")).click();
        driver.findElement(By.cssSelector("div.btn-group > button.multiselect.dropdown-toggle.btn")).click();
        driver.findElement(By.xpath("//input[@value='Gold']")).click();
        driver.findElement(By.xpath("//input[@value='Silver']")).click();
        driver.findElement(By.xpath("//input[@value='Unlimited']")).click();
        driver.findElement(By.cssSelector("#api_designer > legend")).click();
        driver.findElement(By.id("publish_api")).click();

    }

    @Test(groups = "wso2.am", description = "Create tenant and api", dependsOnMethods = "createTenantAndAPI")
    public void checkAPIConsoleAvailability() throws Exception {

        driver.get(storeURL + "?tenant=" + TEST_DATA_TENANT);
        // https://localhost/apimanager/store/apis/info?name=Test&version=1&provider=admin%40test.com&tenant=test.com
        driver.get(storeURL + "/apis/info?name=" + TEST_DATA_API_NAME + "&version=" + TEST_DATA_API_VERSION + "&provider" +
                   "=" + TEST_DATA_TENANT_PUBLISHER + "&tenant=" + TEST_DATA_TENANT);
        WebElement imgElement = driver.findElement(By.cssSelector("div.thumbnail.span2 > img"));

        HttpContext httpContext = new BasicHttpContext();
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(imgElement.getAttribute("src"));
        HttpResponse response = httpclient.execute(httpGet, httpContext);
        assertTrue(response.getStatusLine().getStatusCode() == 200);

    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        TestUtil.cleanUp(TEST_DATA_TENANT_PUBLISHER,
                         TEST_DATA_TENANT_ADMIN_PASSWORD,
                         storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());
        if (driver != null) {
            driver.quit();
        }
    }
}
