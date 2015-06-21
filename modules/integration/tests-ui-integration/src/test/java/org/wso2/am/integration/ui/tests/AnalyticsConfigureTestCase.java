/*
 *
 *   Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.am.integration.ui.tests;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.ui.tests.pages.adminDashboard.AdminDashboardLoginPage;
import org.wso2.am.integration.ui.tests.pages.adminDashboard.ConfigureAnalyticsPage;
import org.wso2.carbon.automation.extensions.selenium.BrowserManager;

import static org.testng.Assert.assertEquals;

public class AnalyticsConfigureTestCase extends APIMIntegrationUiTestBase {

    private WebDriver driver;

    private static final String EVENT_RECEIVER_URL = "tcp://localhost:7614";
    private static final String EVENT_RECEIVER_USERNAME = "admin";
    private static final String EVENT_RECEIVER_PASSWORD = "admin";
    private static final String DATA_ANALYZER_URL = "https://localhost:9446";
    private static final String DATA_ANALYZER_USERNAME = "admin";
    private static final String DATA_ANALYZER_PASSWORD = "admin";
    private static final String STAT_DS_URL = "jdbc:h2:../wso2bam-2.5.0/repository/database/WSO2AM_STATS_DB;DB_CLOSE_ON_EXIT=FALSE";
    private static final String STAT_DS_CLASS_NAME = "org.h2.Driver";
    private static final String STAT_DS_USERNAME = "wso2carbon";
    private static final String STAT_DS_PASSWORD = "wso2carbon";

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        super.init();
        driver = BrowserManager.getWebDriver();
        driver.get(getAdminDashboardURL());

    }

    /*
    To run this test case the bam analyzer node should be run on the https://localhost:9446 (port offset 3) with the
     credentials admin,admin
     */
    @Test(groups = "wso2.apim", description = "verify admin dashboard - configure analytics page", enabled = false)
    public void testAdminDashboardAnalyticsPage() throws Exception {
        AdminDashboardLoginPage adminDashboardLoginPage = new AdminDashboardLoginPage(driver);
        ConfigureAnalyticsPage configureAnalyticsPage = adminDashboardLoginPage.
                getConfigureAnalyticsPage(gatewayContext.getContextTenant().getContextUser().getUserName(),
                        gatewayContext.getContextTenant().getContextUser().getPassword());
        String configSavedMessage = configureAnalyticsPage.addConfigurations(EVENT_RECEIVER_URL, EVENT_RECEIVER_USERNAME,
                EVENT_RECEIVER_PASSWORD, DATA_ANALYZER_URL, DATA_ANALYZER_USERNAME, DATA_ANALYZER_PASSWORD, STAT_DS_URL,
                STAT_DS_CLASS_NAME,STAT_DS_USERNAME, STAT_DS_PASSWORD);
        assertEquals(configSavedMessage,"Configurations Saved!");
        configureAnalyticsPage.logOut();
    }

    @AfterClass()
    public void tearDown() throws Exception {
        driver.quit();
    }


}
