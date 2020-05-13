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

package org.wso2.am.integration.ui.pages.apimanager.store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;
import org.wso2.am.integration.ui.pages.apimanager.subscription.SubscriptionPage;
import org.wso2.am.integration.ui.pages.util.UIElementMapper;

import java.io.IOException;

public class ApiStorePage {
    private static final Log log = LogFactory.getLog(ApiStorePage.class);
    private WebDriver driver;
    private UIElementMapper uiElementMapper;

    public ApiStorePage(WebDriver driver) throws IOException {
        this.driver = driver;
        this.uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        if (!driver.findElement(By.className(uiElementMapper.getElement("app.api.manager.class.name.text"))).
                getText().contains("APIs")) {
            throw new IllegalStateException("This is not the api home Page");
        }
    }

    //this method is used to subscript to api manager
    public SubscriptionPage subscribeToApiManager(String appName)
            throws IOException, InterruptedException {
        driver.findElement(By.cssSelector(uiElementMapper.getElement
                ("app.factory.subscribe.api.element"))).click();
        //This thread waits until Api details gets load
        Thread.sleep(15000);
        new Select(driver.findElement(By.id(uiElementMapper.getElement("app.api.select.app.name")))).
                selectByVisibleText(appName);
        driver.findElement(By.id(uiElementMapper.getElement
                ("app.api.subscribe.button"))).click();
        //this thread waits for the subscription
        Thread.sleep(10000);
        driver.findElement(By.linkText(uiElementMapper.getElement
                ("app.go.to.subscriptions.text"))).click();

        return new SubscriptionPage(driver);
    }

    public SubscriptionPage gotoSubscribeAPiPage() throws IOException, InterruptedException {

        driver.findElement(By.linkText(uiElementMapper.getElement("app.factory.subscription.page")))
                .click();
        //This Thread waits until Subscription Page gets loaded.
        Thread.sleep(30000);
        return new SubscriptionPage(driver);
    }
}
