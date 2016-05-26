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
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;
import org.wso2.carbon.automation.test.utils.common.FileManager;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertTrue;

/**
 * Verify the publisher behaviour when debug logs are enabled
 */
public class APIMANAGER4279PublisherDebugEnabledTestCase extends APIMIntegrationUiTestBase {
    private WebDriver driver;
    private String publisherURL;
    private APIPublisherRestClient apiPublisher;

    private String CUSTOM_JAGGERY_CONF_PATH = getAMResourceLocation() + File.separator +
            "configFiles/apim4267test/jaggery.conf";
    private String TARGET_JAGGERY_CONF_PATH = "repository/deployment/server/jaggeryapps/publisher/";

    private String TEST_API_NAME = "APIMANAGER4279";
    private String TEST_API_VERSION = "1.0.0";
    private String TEST_API_CONTEXT = "apimanager4279";
    private String TEST_API_ENDPOINT = "http://localhost:9090/test";

    private String ELEMENT_USERNAME = "username";
    private String ELEMENT_PASSWORD = "pass";
    private String ELEMENT_LOGIN = "loginButton";
    private String ELEMENT_PUBLISH = "publish_api";
    private String MANAGE_PATH = "/manage?name=" + TEST_API_NAME + "&version=" + TEST_API_VERSION +
            "&provider=";

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        super.init();
        publisherURL = getPublisherURLHttp();
        driver = BrowserManager.getWebDriver();
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        String target = System.getProperty("carbon.home") + File.separator + TARGET_JAGGERY_CONF_PATH;
        FileManager.copyResourceToFileSystem(CUSTOM_JAGGERY_CONF_PATH, target, "jaggery.conf");
    }

    @Test(groups = "wso2.am", description = "Create tenant and api")
    public void createTestAPI() throws Exception {
        APIRequest apiRequest = new APIRequest(TEST_API_NAME, TEST_API_CONTEXT, new URL(TEST_API_ENDPOINT));
        apiRequest.setVersion(TEST_API_VERSION);
        verifyResponse(apiPublisher.addAPI(apiRequest));
    }

    @Test(groups = "wso2.am", description = "Go to manage tab", dependsOnMethods = "createTestAPI")
    public void manageAPI() throws Exception {
        WebDriverWait wait = new WebDriverWait(driver, 60);
        String userName = publisherContext.getContextTenant().getContextUser().getUserName();
        String password = publisherContext.getContextTenant().getContextUser().getPassword();

        driver.get(getPublisherURL() + MANAGE_PATH + userName);

        // Go to the publisher and Login
        // Find and fill Username
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(ELEMENT_USERNAME)));
        WebElement usernameEle = driver.findElement(By.id(ELEMENT_USERNAME));
        usernameEle.sendKeys(userName);

        // Find and fill Password
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(ELEMENT_PASSWORD)));
        WebElement passwordEle = driver.findElement(By.id(ELEMENT_PASSWORD));
        passwordEle.sendKeys(password);

        // find Login button and click on it.
        driver.manage().timeouts().implicitlyWait(4, TimeUnit.SECONDS);
        driver.findElement(By.id(ELEMENT_LOGIN)).click();

        driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
        assertTrue(isElementPresent(By.id(ELEMENT_PUBLISH)));
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        if (driver != null) {
            driver.quit();
        }
        super.cleanUp();
    }

    private boolean isElementPresent(By by) {
        try {
            driver.findElement(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
