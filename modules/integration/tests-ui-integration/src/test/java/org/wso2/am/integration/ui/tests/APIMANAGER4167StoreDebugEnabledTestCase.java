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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.ui.tests.util.APIMTestConstants;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;
import org.wso2.carbon.automation.test.utils.common.FileManager;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertTrue;

/**
 * Verify the behaviour of API store when debug logs are enabled
 */
public class APIMANAGER4167StoreDebugEnabledTestCase extends APIMIntegrationUiTestBase {
    private WebDriver driver;
    private String publisherURL;
    private APIPublisherRestClient apiPublisher;

    private String CUSTOM_JAGGERY_CONF_PATH = getAMResourceLocation() + File.separator +
            "configFiles/apim4167test/jaggery.conf";
    private String TARGET_JAGGERY_CONF_PATH = "repository/deployment/server/jaggeryapps/store/";

    private String TEST_API_NAME = "APIMANAGER4167";
    private String TEST_API_VERSION = "1.0.0";
    private String TEST_API_CONTEXT = "apimanager4167";
    private String TEST_API_ENDPOINT = "http://localhost:9090/test";

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        super.init();
        publisherURL = getPublisherURLHttp();
        driver = BrowserManager.getWebDriver();
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(), publisherContext
                .getContextTenant().getContextUser().getPassword());

        String target = System.getProperty("carbon.home") + File.separator + TARGET_JAGGERY_CONF_PATH;
        FileManager.copyResourceToFileSystem(CUSTOM_JAGGERY_CONF_PATH, target, "jaggery.conf");
    }

    @Test(groups = "wso2.am", description = "Create tenant and api")
    public void createAndPublishTestAPI() throws Exception {
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(TEST_API_NAME, publisherContext
                .getContextTenant().getContextUser().getUserName(), APILifeCycleState.PUBLISHED);
        APICreationRequestBean apiCreationReqBean = new APICreationRequestBean(TEST_API_NAME, TEST_API_CONTEXT,
                TEST_API_VERSION, publisherContext.getContextTenant().getContextUser().getUserName(), new URL
                (TEST_API_ENDPOINT));

        verifyResponse(apiPublisher.addAPI(apiCreationReqBean));
        verifyResponse(apiPublisher.changeAPILifeCycleStatus(updateRequest));
    }

    @Test(groups = "wso2.am", description = "Test if api page is successfully loaded",
            dependsOnMethods = "createAndPublishTestAPI")
    public void validateViewAPIPage() throws Exception {
        WebDriverWait wait = new WebDriverWait(driver, 30);
        driver.get(getStoreURL() + "?tenant=" + publisherContext.getContextTenant().getDomain());
        long loopMaxTime = APIMTestConstants.MAX_LOOP_WAIT_TIME_MILLISECONDS;
        long startTime = System.currentTimeMillis();
        while ((!driver.getPageSource().contains(TEST_API_NAME)) &&
                (System.currentTimeMillis() - startTime) < loopMaxTime) {
            driver.findElement(By.linkText("APIs")).click();
            // wait for 1 seconds and refresh the store since published APIs takes some time to appear in the store
            Thread.sleep(1000);
        }

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText(TEST_API_NAME)));
        driver.findElement(By.linkText(TEST_API_NAME)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("My Applications")));
        driver.findElement(By.linkText("My Applications")).click();
        assertTrue(isElementPresent(By.className("modal-backdrop")));
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
