/*
 *Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.scenario.tests.register.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ApplicationCreationNegativeTestCases extends ScenarioTestBase {
    private APIStoreRestClient apiStore;
    private List<String> applicationsList = new ArrayList<>();
    private final Log log = LogFactory.getLog(ApplicationCreationNegativeTestCases.class);
    private final String ADMIN_LOGIN_USERNAME = "admin";
    private final String ADMIN_LOGIN_PW = "admin";
    private final String SUBSCRIBER_LOGIN_USERNAME_1 = "AppCreationNegSubscriberA";
    private final String SUBSCRIBER_LOGIN_PW_1 = "AppCreationNegSubscriberA";
    private final String SUBSCRIBER_LOGIN_USERNAME_2 = "AppCreationNegSubscriberB";
    private final String SUBSCRIBER_LOGIN_PW_2= "AppCreationNegSubscriberB";
    private static final String UTF_8 = "UTF-8";
    private final String ERROR_APP_CREATION_FAILED = "Application creation failed for application: ";
    private final String ERROR_APP_CREATION_NEGATIVE_TEST = "Error in application creation" +
            " negative test cases. Application: ";
    private final String ERROR_APPLICATION_NAME_LONGER_THAN_70_CHARACTERS
            = "Application name longer than 70 characters. Application: ";
    private final String ERROR_DUPLICATE_APPLICATION_EXIST = "A duplicate application already exists" +
            " by the name - ";
    private final String ERROR_GENERATING_KEY = " key generated for unowned application:  ";
    private final String PRODUCTION = "PRODUCTION";
    private final String SANDBOX = "SANDBOX";
    private final String ERROR = "error";
    private final String MESSAGE = "message";
    private final String STATUS = "status";
    private final String STATUS_APPROVED = "APPROVED";
    private final String APPLICATION_NAME_PREFIX = "Application_";
    private final String APPLICATION_NAME_LONGER_THAN_70_CHARS =
            "ApplicationNameLongerThan70CharactersApplicationNameLongerThan70Characters";
    private final String APPLICATION_DESCRIPTION = "Application description";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        apiStore = new APIStoreRestClient(storeURL);
        createUserWithSubscriberRole(SUBSCRIBER_LOGIN_USERNAME_1, SUBSCRIBER_LOGIN_PW_1,
                ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        apiStore.login(SUBSCRIBER_LOGIN_USERNAME_1, SUBSCRIBER_LOGIN_PW_1);
    }

    @Test(description = "4.1.1.5", dataProvider = "MissingMandatoryApplicationValuesDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testApplicationCreationWithMissingMandatoryValues(String applicationName,
                                                                  String url, String errorMessage) throws Exception {
        HttpResponse addApplicationResponse = apiStore.addApplication(url.replace("{{backendURL}}", storeURL));
        verifyApplicationNotCreated(addApplicationResponse, errorMessage, applicationName);
    }

    @Test(description = "4.1.1.6", dataProvider = "InvalidMandatoryApplicationValuesDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testApplicationCreationWithInvalidMandatoryValues(String applicationName, String tier,
                                                                  String errorMessage) throws Exception {
        HttpResponse addApplicationResponse = apiStore
                .addApplication(URLEncoder.encode(applicationName, UTF_8), URLEncoder.encode(tier, UTF_8)
                        , "", "");
        verifyApplicationNotCreated(addApplicationResponse, errorMessage, applicationName);
    }

    @Test(description = "4.1.1.7")
    public void testDuplicateApplicationName() throws Exception {
        HttpResponse addApplicationResponse = apiStore
                .addApplication(URLEncoder.encode(APPLICATION_NAME_PREFIX + "duplicate" , UTF_8),
                        URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8),
                        "", URLEncoder.encode("", UTF_8));
        verifyResponse(addApplicationResponse);
        assertEquals(new JSONObject(addApplicationResponse.getData()).get(STATUS), STATUS_APPROVED,
                ERROR_APP_CREATION_FAILED + APPLICATION_NAME_PREFIX + "duplicate");
        applicationsList.add(APPLICATION_NAME_PREFIX + "duplicate");
//        add duplicate application - case sensitive
        addApplicationResponse = apiStore
                .addApplication(URLEncoder.encode(APPLICATION_NAME_PREFIX + "duplicate", UTF_8),
                        URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8),
                        "", URLEncoder.encode(APPLICATION_DESCRIPTION, UTF_8));
        verifyApplicationNotCreated(addApplicationResponse,
                ERROR_DUPLICATE_APPLICATION_EXIST + APPLICATION_NAME_PREFIX + "duplicate",
                APPLICATION_NAME_PREFIX + "duplicate");
    }

//    todo uncomment once jappery api validation is fixed
//    @Test(description = "4.1.1.8")
//    public void testApplicationNameLongerThan70CharactersDataProvider() throws Exception {
//        HttpResponse addApplicationResponse = apiStore
//                .addApplication(URLEncoder.encode(APPLICATION_NAME_LONGER_THAN_70_CHARS, UTF_8),
//                        URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8)
//                        , "", URLEncoder.encode("", UTF_8));
//        JSONObject addApplicationJsonObject = new JSONObject(addApplicationResponse.getData());
////        if application added due to test failure add it to application list so that it could be removed later
//        if (!addApplicationJsonObject.getBoolean(ERROR)
//                && addApplicationJsonObject.get(STATUS).equals(STATUS_APPROVED)) {
//            applicationsList.add(APPLICATION_NAME_LONGER_THAN_70_CHARS);
//        }
////        validate application wasn't created
//        assertTrue(addApplicationJsonObject.getBoolean(ERROR),
//                ERROR_APPLICATION_NAME_LONGER_THAN_70_CHARACTERS + APPLICATION_NAME_LONGER_THAN_70_CHARS);
//    }

//    todo uncomment once prohibiting case insensitive duplicate app names is supported
/*    @Test(description = "4.1.1.9", dependsOnMethods = {"testDuplicateApplicationName"})
    public void testCaseInsensitiveDuplicateApplicationName() throws Exception {
//        add duplicate application - case insensitive
        HttpResponse addApplicationResponse = apiStore
                .addApplication(URLEncoder.encode((APPLICATION_NAME_PREFIX + "duplicate").toLowerCase(), UTF_8),
                        URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8),
                        "", URLEncoder.encode(APPLICATION_DESCRIPTION, UTF_8));
        verifyApplicationNotCreated(addApplicationResponse,
                ERROR_DUPLICATE_APPLICATION_EXIST + APPLICATION_NAME_PREFIX + "duplicate",
                APPLICATION_NAME_PREFIX + "duplicate");
    }*/

    @Test(description = "4.1.1.10")
    public void testTokenGenerationForOthersApplications() throws Exception {
        HttpResponse addApplicationResponse = apiStore
                .addApplication(URLEncoder.encode(APPLICATION_NAME_PREFIX + "generateTokensForUnownedApplications",
                        UTF_8), URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8),
                        "", URLEncoder.encode("", UTF_8));
        applicationsList.add(APPLICATION_NAME_PREFIX + "generateTokensForUnownedApplications");
        verifyResponse(addApplicationResponse);
        assertEquals(new JSONObject(addApplicationResponse.getData()).get(STATUS), STATUS_APPROVED,
                ERROR_APP_CREATION_FAILED + APPLICATION_NAME_PREFIX + "generateTokensForUnownedApplications");
        createUserWithSubscriberRole(SUBSCRIBER_LOGIN_USERNAME_2, SUBSCRIBER_LOGIN_PW_2,
                ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        apiStore.login(SUBSCRIBER_LOGIN_USERNAME_2, SUBSCRIBER_LOGIN_PW_2);
        testTokenGenerationFailure(APPLICATION_NAME_PREFIX + "generateTokensForUnownedApplications",
                PRODUCTION);
        testTokenGenerationFailure(APPLICATION_NAME_PREFIX + "generateTokensForUnownedApplications",
                SANDBOX);
    }

    private void verifyApplicationNotCreated(HttpResponse response, String errorMessage, String applicationName) {
        log.info("Verify application is not created. Response code : " + response.getResponseCode());
        log.info("Verify application is not created. Response data : " + response.getData());
        JSONObject responseJsonObject = new JSONObject(response.getData());
//        if application added due to test failure add it to application list so that it could be removed later
        if (!responseJsonObject.getBoolean(ERROR)
                && responseJsonObject.get(STATUS).equals(STATUS_APPROVED)) {
            applicationsList.add(applicationName);
        }
//        validate application wasn't created
        assertTrue(responseJsonObject.getBoolean(ERROR),
                ERROR_APP_CREATION_NEGATIVE_TEST + applicationName);
        assertTrue(responseJsonObject.getString(MESSAGE).trim().contains(errorMessage),
                ERROR_APP_CREATION_NEGATIVE_TEST + applicationName);
    }

    private void testTokenGenerationFailure(String applicationName, String keyType)
            throws APIManagerIntegrationTestException{
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(APPLICATION_NAME_PREFIX +
                "generateTokensForUnownedApplications");
        appKeyRequestGenerator.setKeyType(keyType);
        HttpResponse response = apiStore.generateApplicationKey(appKeyRequestGenerator);
        log.info("Verify application token is not generated. Response code : " + response.getResponseCode());
        log.info("Verify application token is not generated. Response data : " + response.getData());
        JSONObject responseStringJson = new JSONObject(response.getData());
        assertTrue(responseStringJson.getBoolean(ERROR), keyType + ERROR_GENERATING_KEY +applicationName);
        assertEquals(responseStringJson.getString("message"),
                "Error occurred while executing the action generateApplicationKey", keyType +
                        ERROR_GENERATING_KEY + applicationName);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.login(SUBSCRIBER_LOGIN_USERNAME_1, SUBSCRIBER_LOGIN_PW_1);
        for (String name : applicationsList) {
            apiStore.removeApplication(URLEncoder.encode(name, UTF_8));
        }
        applicationsList.clear();
        deleteUser(SUBSCRIBER_LOGIN_USERNAME_1, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        deleteUser(SUBSCRIBER_LOGIN_USERNAME_2, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
    }
}
