/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *    WSO2 Inc. licenses this file to you under the Apache License,
 *    Version 2.0 (the "License"); you may not use this file except
 *    in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.am.integration.tests.application.groupSharing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class ApplicationSharingTestCase extends APIMIntegrationBaseTest {
    private static final Log log = LogFactory.getLog(ApplicationSharingTestCase.class);

    private APIStoreRestClient apiStore;

    private static final String APPLICATION_NAME = "TestApplication";
    private static final String APP_TIER = APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED;
    private static final String USER_ONE = "userOne";
    private static final String USER_TWO = "userTwo";
    private static final String PASSWORD = "test@123";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String EMAIL = "john.doe@test.com";
    private static final String ORGANIZATION = "Test";
    private int userOneApplicationId;
    private int userTwoApplicationId;

    @Factory(dataProvider = "userModeDataProvider")
    public ApplicationSharingTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
        };
    }
    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiStore = new APIStoreRestClient(storeURLHttp);
        createUsersAndApplications();
    }

    @Test(description = "Remove user two's application and check if user one's application is getting deleted")
    public void testUserTwoApplicationRemoval() throws Exception {
        loginUser(USER_ONE, PASSWORD);
        HttpResponse removeAppResponse = apiStore.removeApplicationById(userOneApplicationId);
        JSONObject removeAppJsonObject = new JSONObject(removeAppResponse.getData());
        assertFalse(removeAppJsonObject.getBoolean("error"),
                "Error in Remove Application Response: " + APPLICATION_NAME);
        apiStore.logout();

        loginUser(USER_TWO, PASSWORD);
        HttpResponse applicationsResponse = apiStore.getAllApplications();
        assertEquals(applicationsResponse.getResponseCode(), Response.Status.OK.getStatusCode());
        JSONObject verifyAppNameJsonObject = new JSONObject(applicationsResponse.getData());
        log.info(verifyAppNameJsonObject);
        JSONArray verifyAppJsonArray = verifyAppNameJsonObject.getJSONArray("applications");

        boolean isUserTwoAppDeleted = true;
        for (int appsIndex = 0; appsIndex < verifyAppJsonArray.length(); appsIndex++) {
            String applicationName = verifyAppJsonArray.getJSONObject(appsIndex).getString("name");
            if (APPLICATION_NAME.equals(applicationName)) {
                isUserTwoAppDeleted = false;
                break;
            }
        }
        assertFalse(isUserTwoAppDeleted, "Deletion of User One's application also deletes User Two's application");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        HttpResponse removeAppResponse = apiStore.removeApplicationById(userTwoApplicationId);
        JSONObject removeAppJsonObject = new JSONObject(removeAppResponse.getData());
        assertFalse(removeAppJsonObject.getBoolean("error"),
                "Error in Remove Application Response: " + APPLICATION_NAME);
        super.cleanUp();
    }

    private void createUsersAndApplications() throws Exception {
        //signup of user one
        HttpResponse storeSignUpResponse = apiStore
                .signUpWithOrganization(USER_ONE, PASSWORD, FIRST_NAME, LAST_NAME, EMAIL, ORGANIZATION);
        JSONObject signUpJsonObject = new JSONObject(storeSignUpResponse.getData());
        assertFalse(signUpJsonObject.getBoolean("error"), "Error in user sign up Response");
        assertFalse(signUpJsonObject.getBoolean("showWorkflowTip"), "Error in sign up Response");
        log.info("Signed Up User: " + USER_ONE);

        //signup of user two
        storeSignUpResponse = apiStore
                .signUpWithOrganization(USER_TWO, PASSWORD, FIRST_NAME, LAST_NAME, EMAIL, ORGANIZATION);
        signUpJsonObject = new JSONObject(storeSignUpResponse.getData());
        assertFalse(signUpJsonObject.getBoolean("error"), "Error in user sign up Response");
        assertFalse(signUpJsonObject.getBoolean("showWorkflowTip"), "Error in sign up Response");
        log.info("Signed Up User: " + USER_TWO);

        //login as user "user one"
        loginUser(USER_ONE, PASSWORD);

        log.info("Logged in as User: " + USER_ONE);

        // Create Application for user one
        HttpResponse applicationResponse = apiStore
                .addApplicationWithGroup(APPLICATION_NAME, APP_TIER, "", "APPCreatedByUserOne", "");
        assertEquals(applicationResponse.getResponseCode(), Response.Status.OK.getStatusCode());
        JSONObject verifyAppJsonObject = new JSONObject(applicationResponse.getData());
        log.info(verifyAppJsonObject);
        userOneApplicationId = verifyAppJsonObject.getInt("applicationId");
        apiStore.logout();

        //login as user "user Two"
        loginUser(USER_TWO, PASSWORD);

        log.info("Logged in as User: " + USER_TWO);

        // Create Application for user one
        applicationResponse = apiStore
                .addApplicationWithGroup(APPLICATION_NAME, APP_TIER, "", "APPCreatedByUserTwo", ORGANIZATION);
        assertEquals(applicationResponse.getResponseCode(), Response.Status.OK.getStatusCode());
        verifyAppJsonObject = new JSONObject(applicationResponse.getData());
        log.info(verifyAppJsonObject);
        userTwoApplicationId = verifyAppJsonObject.getInt("applicationId");
        apiStore.logout();
    }

    private void loginUser(String user, String password) throws Exception {
        HttpResponse loginResponse = apiStore.login(user, password);
        JSONObject loginJsonObject = new JSONObject(loginResponse.getData());
        assertFalse(loginJsonObject.getBoolean("error"), "Error in Login Request: User Name : " + user);
    }

}
