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
 * UI Page class of Publisher Home page of API Manager
 */
public class PublisherHomePage extends PageHandler {
    private static final Log log = LogFactory.getLog(PublisherHomePage.class);


    public PublisherHomePage(WebDriver driver) throws IOException {
        super(driver);
        // Check that we're on the right page.
        if (!driver.getCurrentUrl().contains(APIMTestConstants.PUBLISHER_HOME_PAGE_URL_VERIFICATION)) {
            throw new IllegalStateException(driver.getCurrentUrl() + ":    This is not the Publisher home page");
        }
        log.info("Page load : Publisher Home Page");
    }

    /**
     * Create a API. Resource URL Pattern is"*", resource name is "default" and resource http method is "GET".
     *
     * @param apiName        name of the APi
     * @param apiContext     API context
     * @param apiVersion     API version
     * @param apiDescription API Description
     * @param apiUrl         API URL
     * @param tagNames       API Tags
     */
    public void createNewAPI(String apiName, String apiContext, String apiVersion,
                             String apiDescription, String apiUrl, String[] tagNames)
            throws IOException {
        log.info("Create API : Start :: API Name:" + apiName + "-> API Context:" + apiContext + "-> API Version:" + apiVersion + "-> API URL:" + apiUrl);
        //Go to new API section
        clickElementByLinkText("publisher.api.newapi.linktext");
        //Design phase
        fillTextBoxById("publisher.api.name", apiName);
        fillTextBoxById("publisher.api.context", apiContext);
        fillTextBoxById("publisher.api.version", apiVersion);
        fillTextBoxById("publisher.api.description", apiDescription);
        for (String tagName : tagNames) {
            fillTextBoxByXPath("publisher.api.tagname.bootstrap.tagsinput.xpath", tagName + "\n");
        }
        fillTextBoxById("publisher.api.resource_url_pattern", APIMTestConstants.ASTERISK);
        fillTextBoxById("publisher.api.resource", APIMTestConstants.DEFAULT);
        clickElementByXpath("publisher.api.resource_http_method.get");
        clickElementById("publisher.api.addresource.button.id");
        clickElementById("publisher.api.go_to_implement.button.id");
        waitUntilElementVisibilityById("publisher.api.go_to_manage.button.id", APIMTestConstants.WAIT_TIME_VISIBILITY_ELEMENT_SECONDS);
        //Implement phase
        fillTextBoxById("publisher.api.production.endpoint", apiUrl);
        clickElementById("publisher.api.go_to_manage.button.id");
        waitUntilElementVisibilityById("publisher.api.publish_api.button.id", APIMTestConstants.WAIT_TIME_VISIBILITY_ELEMENT_SECONDS);
        //Manage phase
        clickElementByXpath("publisher.api.tier.noneselected.xpath");
        clickElementByXpath("publisher.api.tier.gold.xpath");
        clickElementById("publisher.api.publish_api.button.id");
        waitUntilElementVisibilityById("publisher.api.apiview.id", APIMTestConstants.WAIT_TIME_VISIBILITY_ELEMENT_SECONDS);
        log.info("Create API : Finish :: API Name:" + apiName + "-> API Context:" + apiContext + "-> API Version:" + apiVersion + "-> API URL:" + apiUrl);
    }

    /**
     * Returns the text of API view
     *
     * @return value of the APIView UI element
     */
    public String getAPIViewText() throws IOException {
        return getTextOfElementById("publisher.api.apiview.id",  30);
    }

    /**
     * Logout from publisher
     */
    public void logOut() throws IOException {
        clickElementById("publisher.usermenu.id");
        clickElementByCssSelector("publisher.logout.button.css");
    }


}
