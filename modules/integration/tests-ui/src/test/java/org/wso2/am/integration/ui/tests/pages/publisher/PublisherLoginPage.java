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
 * /
 */

package org.wso2.am.integration.ui.tests.pages.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.WebDriver;
import org.wso2.am.integration.ui.tests.pages.PageHandler;
import org.wso2.am.integration.ui.tests.util.APIMTestConstants;

import java.io.IOException;


/**
 * UI Page class of Publisher Login page of API Manager
 */
public class PublisherLoginPage extends PageHandler {

    private static final Log log = LogFactory.getLog(PublisherLoginPage.class);
    private WebDriver driver;

    public PublisherLoginPage(WebDriver driver) {
        super(driver);
        this.driver = driver;
        // Check that we're on the right page.
        if (!(driver.getCurrentUrl().contains(APIMTestConstants.PUBLISHER_LOGIN_PAGE_URL_VERIFICATION))) {
            throw new IllegalStateException("This is not the publisher login page");
        }
    }

    /**
     * Login to Publisher. after login this page will return to Publisher home page.
     *
     * @param userName Login username
     * @param password Login password
     * @return instance of a PublisherHomePage
     * @throws Exception
     */
    public PublisherHomePage loginAs(String userName, CharSequence password)
            throws IOException {

        fillTextBoxById("publisher.login.username.id", userName);
        fillTextBoxById("publisher.login.password.id", password);
        clickElementById("publisher.login.button.id");
        log.info("login as " + userName + " to Publisher Page");
        waitUntilElementVisibilityByLinkText("publisher.api.newapi.linktext", APIMTestConstants.WAIT_TIME_VISIBILITY_ELEMENT_SECONDS);
        return new PublisherHomePage(driver);

    }


}
