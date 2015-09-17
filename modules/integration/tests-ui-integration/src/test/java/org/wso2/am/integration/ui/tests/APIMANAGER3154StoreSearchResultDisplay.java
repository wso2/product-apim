/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.ui.tests.util.TestUtil;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;

/**
 * This test class is added to test the pagination when doing a api search
 */
public class APIMANAGER3154StoreSearchResultDisplay extends APIMIntegrationUiTestBase {
    private WebDriver driver;
    private String publisherURL;
    private String storeURL;
    private WebDriverWait wait;

    private static final Log log = LogFactory.getLog(APIMANAGER3154StoreSearchResultDisplay.class);

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        super.init();
        driver = BrowserManager.getWebDriver();

        publisherURL = getPublisherURL();
        storeURL = getStoreURL();
        wait = new WebDriverWait(driver, 30);
    }

    @Test(groups = "wso2.am", description = "verify login to api manager")
    public void testLogin() throws Exception {

        // logging into publisher
        driver.get(publisherURL + "/site/pages/login.jag");
        WebElement userNameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("pass"));

        userNameField.sendKeys(gatewayContextMgt.getContextTenant().getContextUser().getUserName());
        passwordField.sendKeys(gatewayContextMgt.getContextTenant().getContextUser().getPassword());
        driver.findElement(By.id("loginButton")).click();

        // Adding 13 APIS
        for (int i = 0; i < 13; i++) {
            publishAPI("testApi" + i, "context" + i);
        }


        // Searching in store
        driver.get(storeURL + "/site/pages/list-apis.jag?tenant=carbon.super");
        WebElement searchTxtBox = driver.findElement(By.name("query"));
        searchTxtBox.sendKeys("testApi");
        driver.findElement(By.className("search-button")).click();

        // Waiting for search results
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".thumbnails>li")));

        int searchResultCount = driver.findElements(By.cssSelector(".thumbnails>li")).size();

        // checking result count
        Boolean result;
        if (searchResultCount == 10) {
            result = true;
        } else {
            result = false;
        }

        Assert.assertTrue(result, "Incorrect Pagination during API Search");
    }

    /**
     * This method adds a new api with the passed apiname and apicontext
     *
     * @param apiname    - Name of the API
     * @param apicontext - Context of the API
     */

    public void publishAPI(String apiname, String apicontext) {



        driver.findElement(By.linkText("Add")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("create-new-api")));
        driver.findElement(By.id("create-new-api")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("designNewAPI")));
        driver.findElement(By.id("designNewAPI")).click();

        WebElement name = driver.findElement(By.id("name"));
        name.sendKeys(apiname);

        WebElement context = driver.findElement(By.id("context"));
        context.sendKeys(apicontext);

        WebElement version = driver.findElement(By.id("version"));
        version.sendKeys("1.0.0");

        driver.findElement(By.id("resource_url_pattern")).sendKeys("*");
        driver.findElement(By.cssSelector("input.http_verb_select")).click();
        driver.findElement(By.id("add_resource")).click();

        // waiting until resource is saved
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[@class='resource-path']")));

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[@id='saveBtn']")));
        driver.findElement(By.xpath("//button[@id='saveBtn']")).click();

        // waiting until API is saved
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("a.wizard-done > span")));

        driver.findElement(By.cssSelector("a.wizard-done > span")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@value='#managed-api']")));
        driver.findElement(By.xpath("//div[@value='#managed-api']")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("go_to_manage")));
        driver.findElement(By.id("jsonform-0-elt-production_endpoints")).clear();
        driver.findElement(By.id("jsonform-0-elt-production_endpoints")).sendKeys("/testEndpoint");
        driver.findElement(By.id("go_to_manage")).click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("publish_api")));
        driver.findElement(By.xpath("//button[@type='button']")).click();
        driver.findElement(By.xpath("//input[@value='Gold']")).click();
        driver.findElement(By.id("publish_api")).click();
        
        // waiting to finish the API state update
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[@href='#lifecycles']")));
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        TestUtil.cleanUp(gatewayContextMgt.getContextTenant().getContextUser().getUserName(),
                         gatewayContextMgt.getContextTenant().getContextUser().getPassword(),
                         storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());
        driver.quit();
    }
}
