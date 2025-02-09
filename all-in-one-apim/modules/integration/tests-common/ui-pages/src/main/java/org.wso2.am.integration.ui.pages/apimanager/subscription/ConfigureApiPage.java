/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.ui.pages.apimanager.subscription;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.wso2.am.integration.ui.pages.login.LoginPage;
import org.wso2.am.integration.ui.pages.metadata.ApiPage;
import org.wso2.am.integration.ui.pages.util.UIElementMapper;

import java.io.IOException;

public class ConfigureApiPage {

    private static final Log log = LogFactory.getLog(ConfigureApiPage.class);
    private WebDriver driver;
    private UIElementMapper uiElementMapper;

    public ConfigureApiPage(WebDriver driver) throws IOException {
        this.driver = driver;
        this.uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        driver.findElement(By.id(uiElementMapper.getElement("configure.tab.id"))).click();
        driver.findElement(By.linkText(uiElementMapper.getElement("api.configure.add.link"))).click();

        log.info("Api Change Page");
        if (!driver.findElement(By.id(uiElementMapper.getElement("api.configure.dashboard.middle.text"))).
                getText().contains("APIs")) {

            throw new IllegalStateException("This is not the correct Page");
        }
    }

    public ApiPage configureApi(String newApiConfiguration)
            throws InterruptedException, IOException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("SaveConfiguration()");
        Thread.sleep(5000);
        return new ApiPage(driver);
    }

    public LoginPage logout() throws IOException {
        driver.findElement(By.linkText(uiElementMapper.getElement("home.greg.sign.out.xpath"))).click();
        return new LoginPage(driver);
    }
}
