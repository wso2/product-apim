/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.wso2.am.integration.tests.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIThrottlingTier;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

/**
 * This test case is used to test the Application callback URL validation when Application update is perform
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class ApplicationCallbackURLTestCase extends APIMIntegrationBaseTest {

    private final Log log = LogFactory.getLog(ApplicationCallbackURLTestCase.class);
    private APIStoreRestClient apiStore;
    private String storeURLHttp;
    private String description = "description";
    private String appName = "ApplicationCallbackURLApp";

    @Factory(dataProvider = "userModeDataProvider")
    public ApplicationCallbackURLTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        storeURLHttp = getStoreURLHttp();
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(user.getUserName(), user.getPassword());
    }

    @Test(groups = { "wso2.am" }, description = "Sample Application creation")
    public void testApplicationCreation() throws Exception {
        String callbackURL = "http://localhost:9443/store/";
        //adding an application
        HttpResponse serviceResponse = apiStore
                .addApplication(appName, APIThrottlingTier.UNLIMITED.getState(), callbackURL, description);
        verifyResponse(serviceResponse);

        JSONObject res = new JSONObject(apiStore.getApplications().getData());
        Assert.assertFalse(res.getBoolean("error"));
        JSONArray arr = res.getJSONArray("applications");
        JSONObject app;
        for (int i = 0; i < arr.length(); i++) {
            app = arr.getJSONObject(i);
            if (appName.equals(app.getString("name"))) {
                Assert.assertEquals(callbackURL, app.getString("callbackUrl"),
                        "Application callback URL is not Updated as Expected");
                return;
            }
        }
        Assert.fail("Created application not found");
    }

    @Test(groups = { "wso2.am" }, description = "Update application with callback URL",
            dependsOnMethods = "testApplicationCreation")
    public void testApplicationUpdate() throws Exception {
        String callbackURL = "malformedUrl";
        //update application with malformed callback URL
        HttpResponse serviceResponse = apiStore
                .updateApplication(appName, appName, callbackURL, description, APIThrottlingTier.UNLIMITED.getState());
        verifyResponse(serviceResponse);
        JSONObject res = new JSONObject(apiStore.getApplications().getData());
        Assert.assertFalse(res.getBoolean("error"));
        JSONArray arr = res.getJSONArray("applications");
        JSONObject app;
        for (int i = 0; i < arr.length(); i++) {
            app = arr.getJSONObject(i);
            if (appName.equals(app.getString("name"))) {
                Assert.assertNotEquals(callbackURL, app.getString("callbackUrl"),
                        "Application callback URL is updated with malformed URL");
                Assert.assertNotEquals("Not Specified", app.getString("callbackUrl"),
                        "Application callback URL is updated with malformed URL");
                return;
            }
        }
        Assert.fail("Updated application not found");
    }

    @Test(groups = { "wso2.am" }, description = "Sample Application creation",
            dependsOnMethods = "testApplicationUpdate")
    public void testApplicationUpdateIpAsCallBackURL() throws Exception {
        String callbackURL = "https://10.100.7.74:9443/store/";
        //update application with valid callback URL
        HttpResponse serviceResponse = apiStore
                .updateApplication(appName, appName, callbackURL, description, APIThrottlingTier.UNLIMITED.getState());
        verifyResponse(serviceResponse);
        JSONObject res = new JSONObject(apiStore.getApplications().getData());
        Assert.assertFalse(res.getBoolean("error"));
        JSONArray arr = res.getJSONArray("applications");
        JSONObject app;
        for (int i = 0; i < arr.length(); i++) {
            app = arr.getJSONObject(i);
            if (appName.equals(app.getString("name"))) {
                Assert.assertEquals(callbackURL, app.getString("callbackUrl"),
                        "Application callback URL is not Updated as Expected");
                return;
            }
        }
        Assert.fail("Updated application not found");
    }

    @Test(groups = { "wso2.am" }, description = "Update application with callback URL",
            dependsOnMethods = "testApplicationUpdateIpAsCallBackURL")
    public void testApplicationUpdateValidIpAsCallBackURL() throws Exception {
        String callbackURL = "10.100.7.74:9443/store/";
        //update application with malformed callback URL
        HttpResponse serviceResponse = apiStore
                .updateApplication(appName, appName, callbackURL, description, APIThrottlingTier.UNLIMITED.getState());
        verifyResponse(serviceResponse);
        JSONObject res = new JSONObject(apiStore.getApplications().getData());
        Assert.assertFalse(res.getBoolean("error"));
        JSONArray arr = res.getJSONArray("applications");
        JSONObject app;
        for (int i = 0; i < arr.length(); i++) {
            app = arr.getJSONObject(i);
            if (appName.equals(app.getString("name"))) {
                Assert.assertNotEquals(callbackURL, app.getString("callbackUrl"),
                        "Application callback URL is updated with malformed URL");
                Assert.assertNotEquals("Not Specified", app.getString("callbackUrl"),
                        "Application callback URL is updated with malformed URL");
                return;
            }
        }
        Assert.fail("Updated application not found");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication(appName);
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }
}
