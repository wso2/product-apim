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
import org.wso2.am.integration.ui.tests.util.APIMTestConstants;

import java.io.IOException;

/**
 * UI Page class of RESTClient of API Manager
 */
public class TestAPIPage extends PageHandler {
    private static final Log log = LogFactory.getLog(TestAPIPage.class);

    public TestAPIPage(WebDriver driver) throws IOException {
        super(driver);

        if (!(driver.getCurrentUrl().contains(APIMTestConstants.STORE_REST_CLIENT_URL_VERIFICATION))) {
            throw new IllegalStateException("This is not the RESTClient page");
        }
        log.info("RESTClient loaded" + driver.findElement(By.className("title-section")).findElement(By.tagName("h2")));
    }

    /**
     * Test the API. This method  tests the API using RESTClient available in Store.
     *
     * @param apiURL      URL of the API to test
     * @param accessToken access token of the api
     */
    public void testAPI(String apiURL, String accessToken) throws IOException {
        waitUntilElementVisibilityByLinkText("restclient.api.url.form.linktext", 30);
        clickElementByLinkText("restclient.api.url.form.linktext");
        fillTextBoxById("restclient.api.test.url.id", apiURL + "\n");
        waitUntilElementVisibilityByCssSelector("restclient.api.test.header.key.css",30);
        fillTextBoxByCssSelector("restclient.api.test.header.key.css", APIMTestConstants.AUTHORIAZATION);
        waitUntilElementVisibilityByCssSelector("restclient.api.test.header.value.css",30);
        fillTextBoxByCssSelector("restclient.api.test.header.value.css", APIMTestConstants.BEARER + APIMTestConstants.SPACE + accessToken);
        log.info("API Test :start : accessToken:" + accessToken + "-> apiURL" + apiURL);
        waitUntilElementVisibilityById("restclient.api.test.send.button.id", 30);
        clickElementById("restclient.api.test.send.button.id");
        waitUntilElementVisibilityByLinkText("restclient.api.test.response.body.linktext", APIMTestConstants.WAIT_TIME_VISIBILITY_ELEMENT_SECONDS);
        log.info("API Test :finish: accessToken:" + accessToken + "-> apiURL" + apiURL);

    }


    /**
     * Return the response body of the result of API test.
     *
     * @return String: Texts in the response body.
     */
    public String getTestResponseBody() throws IOException {
        return getTextOfElementById("restclient.api.test.response.body.content.id", 60);
    }


}
