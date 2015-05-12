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

package org.wso2.am.integration.ui.tests.samples;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.ui.tests.util.APIAccessInfo;
import org.wso2.am.integration.ui.tests.APIMIntegrationUiTestBase;
import org.wso2.am.integration.ui.tests.pages.publisher.PublisherHomePage;
import org.wso2.am.integration.ui.tests.pages.publisher.PublisherLoginPage;
import org.wso2.am.integration.ui.tests.pages.store.StoreHomePage;
import org.wso2.am.integration.ui.tests.pages.store.TestAPIPage;
import org.wso2.am.integration.ui.tests.util.APIMTestConstants;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * UI Test case for the Youtube API other.
 * This test case was created using Page object method.
 */
public class YouTubeUIPagesTestCase extends APIMIntegrationUiTestBase {
    private WebDriver driver;

    private static final String API_NAME = "YoutubeFeeds1";
    private static final String API_CONTEXT = "youtube1";
    private static final String API_VERSION = "1.0.0";
    private static final String API_DESCRIPTION = "Youtube Live Feeds1";
    private static final String API_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private static final String API_METHOD = "/most_popular";
    private static final String RESPONSE_BODY_TEST_STRING = "YouTube data API";
    private static final String[] TAG_NAMES = new String[]{"youtube", "gdata", "multimedia"};

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        super.init();
        driver = BrowserManager.getWebDriver();
        driver.get(getPublisherURL());


    }

    @Test(groups = "wso2.greg", description = "verify YoutubeAPI Sample ")
    public void testYoutubeAPI() throws Exception {
        //Publisher activities
        PublisherLoginPage pubLoginPage = new PublisherLoginPage(driver);
        PublisherHomePage publisherHomePage = pubLoginPage.loginAs(
                gatewayContext.getContextTenant().getContextUser().getUserName(),
                gatewayContext.getContextTenant().getContextUser().getPassword());
        publisherHomePage.createNewAPI(API_NAME, API_CONTEXT, API_VERSION, API_DESCRIPTION, API_URL, TAG_NAMES);
        //Test Publishing API
        assertEquals(publisherHomePage.getAPIViewText(), API_DESCRIPTION, API_DESCRIPTION + " Should appear in API List");
        publisherHomePage.logOut();
        //Navigate to Store
        driver.get(getStoreURL() + "/?tenant=carbon.super" );
        StoreHomePage storeHomePage = new StoreHomePage(driver);
        storeHomePage.loginAs(gatewayContext.getContextTenant().getContextUser().getUserName(),
                gatewayContext.getContextTenant().getContextUser().getPassword());

        APIAccessInfo apiAccessInfo = storeHomePage.doSubscribe(API_NAME + APIMTestConstants.HYPHEN + API_VERSION);

        TestAPIPage testAPIPage = storeHomePage.goToRestClient();
        testAPIPage.testAPI(apiAccessInfo.getAccessURL() + API_METHOD, apiAccessInfo.getAccessToken());
        //Test response body
        assertTrue(testAPIPage.getTestResponseBody().contains(RESPONSE_BODY_TEST_STRING), RESPONSE_BODY_TEST_STRING +
                " should be in the respond body");
        storeHomePage.logOut();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception {
        driver.quit();
    }

}
