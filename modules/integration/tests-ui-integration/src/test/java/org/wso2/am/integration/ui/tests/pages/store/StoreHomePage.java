/*
 *
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.am.integration.ui.tests.pages.store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.wso2.am.integration.ui.tests.pages.PageHandler;
import org.wso2.am.integration.ui.tests.util.APIAccessInfo;
import org.wso2.am.integration.ui.tests.util.APIMTestConstants;

import java.io.IOException;

/**
 * UI Page class of Store Home page of API Manager
 */
public class StoreHomePage extends PageHandler {

    private static final Log log = LogFactory.getLog(StoreHomePage.class);
    private WebDriver driver;


    public StoreHomePage(WebDriver driver) throws IOException {
        super(driver);
        this.driver = driver;

        // Check that we're on the right page.
        if (!(driver.getCurrentUrl().contains(APIMTestConstants.STORE_HOME_PAGE_URL_VERIFICATION))) {
            throw new IllegalStateException("This is not the Store Home page");
        }
    }

    /**
     * Login to Store.
     *
     * @param userName User Name
     * @param password Password
     */
    public void loginAs(String userName, CharSequence password) throws IOException {
        waitUntilElementVisibilityByLinkText("store.login.link", 60);
        clickElementByLinkText("store.login.link");
        fillTextBoxById("store.login.username.id", userName);
        fillTextBoxById("store.login.password.id", password);
        waitUntilElementVisibilityById("store.login.button.id", 60);
        clickElementById("store.login.button.id");
        waitUntilElementVisibilityByLinkText("store.menu.apis.link", 60);
        log.info("login as " + userName + " to Store Page");
    }

    /**
     * Subscribe the given API. API Link name should  provide as a parameter.
     * It will wait for the time period configured in "MAX_LOOP_WAIT_TIME_MILLISECONDS" variable in "APIMTestConstants.java".
     * While in the wait it automatically click the "APIs"  menu link in every 1 seconds.
     *
     * @param apiLink API link text
     * @return APIAccessInfo which contains the access token and access url of the subscribed API.
     * @throws Exception
     */
    public APIAccessInfo doSubscribe(String apiLink) throws Exception {

        String accessToken;
        long loopMaxTime = APIMTestConstants.MAX_LOOP_WAIT_TIME_MILLISECONDS;
        long startTime = System.currentTimeMillis();
        long nowTime = startTime;

        // wait until the given API is appear in the UI
        while ((!driver.getPageSource().contains(apiLink)) && (nowTime - startTime) < loopMaxTime) {
            clickElementByLinkText("store.menu.apis.link");
            Thread.sleep(1000);
            nowTime = System.currentTimeMillis();
        }
        //Click given API
        clickLinkByName(apiLink);
        //get the access HTTP URL
        String accessHTTPURL = getTextOfElementByXPath("store.api.http.url.xpath");
        //click Subscribe button
        waitUntilElementVisibilityById("store.subscribe.button.id", APIMTestConstants.WAIT_TIME_VISIBILITY_ELEMENT_SECONDS);
        clickElementById("store.subscribe.button.id");
        waitUntilElementVisibilityByLinkText("store.mysubscription.linktext", APIMTestConstants.WAIT_TIME_VISIBILITY_ELEMENT_SECONDS);
        clickElementByLinkText("store.mysubscription.linktext");
        String genButtonText = getTextOfElementByXPath("store.mysubscription.generate.button.xpath");
        //Generation of access token
        if (genButtonText.equals("Generate")) { //if no token was generated.
            clickElementByXpath("store.mysubscription.generate.button.xpath");
            waitUntilElementVisibilityByClassName("store.access.token.classname", APIMTestConstants.WAIT_TIME_VISIBILITY_ELEMENT_SECONDS);
            accessToken = getTextOfElementByClassName("store.access.token.classname");
        } else {// Re-generate the token if the user  already has a token.
            String currentValue = getTextOfElementByClassName("store.access.token.classname");
            String newValue;
            startTime = System.currentTimeMillis();

            do {
                clickElementByXpath("store.mysubscription.generate.button.xpath");
                Thread.sleep(1000);
                newValue = getTextOfElementByClassName("store.access.token.classname");
                nowTime = System.currentTimeMillis();
            } while (currentValue.equals(newValue) && (nowTime - startTime) < loopMaxTime);
            accessToken = newValue;
        }

        log.info("API Subscribed :" + apiLink);
        return new APIAccessInfo(accessToken, accessHTTPURL);
    }

    /**
     * Go to RESTClient
     *
     * @return TestAPIPage : Returns the RESTClient page
     * @throws java.io.IOException
     */
    public TestAPIPage goToRestClient() throws IOException {
        clickElementByLinkText("home.theme.link");
        clickElementByXpath("home.theme.light.link");
        clickElementByLinkText("store.menu.tools");
        waitUntilElementVisibilityByLinkText("store.menu.tools.restclient", APIMTestConstants.WAIT_TIME_VISIBILITY_ELEMENT_SECONDS);
        clickElementByLinkText("store.menu.tools.restclient");
        return new TestAPIPage(driver);
    }

    /**
     * click a Link by Link Name.
     *
     * @param linkName Name of the link
     */
    private void clickLinkByName(String linkName) {
        driver.findElement(By.linkText(linkName)).click();
    }

    /**
     * Logout from Store
     */
    public void logOut() throws IOException {
        clickElementByCssSelector("store.menu.user.dropdown.css");
        clickElementById("store.menu.user.logout.link.id");
    }

}
