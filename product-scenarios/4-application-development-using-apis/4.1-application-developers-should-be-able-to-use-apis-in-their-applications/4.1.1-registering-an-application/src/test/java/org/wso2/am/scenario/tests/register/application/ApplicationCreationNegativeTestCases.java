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

import org.json.JSONObject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ApplicationCreationNegativeTestCases extends ScenarioTestBase {
    private APIStoreRestClient apiStore;
    private List<String> applicationsList = new ArrayList<>();
    private static final String ADMIN_LOGIN_USERNAME = "admin";
    private static final String ADMIN_LOGIN_PW = "admin";
    private static final String DEFAULT_STORE_URL = "https://localhost:9443/";
    private static final String UTF_8 = "UTF-8";
    private static final String ERROR_APP_CREATION_FAILED = "Application creation failed for application: ";
    private static final String ERROR_IN_APP_CREATION_WITH_INVALID_INPUT = "Error in Application Creation with " +
            "invalid input. Application: ";
    private static final String ERROR_IN_RESPONSE_CODE_ADD_APP = "Response Code is mismatched in add application: ";
    private static final String ERROR_APPLICATION_NAME_LONGER_THAN_70_CHARACTERS
            = "Application name longer than 70 characters. Application: ";
    private static final String ERROR_DUPLICATE_APPLICATION_EXIST = "A duplicate application already exists" +
            " by the name - ";
    private static final String ERROR_DUPLICATE_APPLICATION_CREATION = "Duplicate application created with name: ";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String STATUS = "status";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String APPLICATION_DUPLICATE_NAME_CHECK = "Application";
    private static final String APPLICATION_NAME_LONGER_THAN_70_CHARS =
            "ApplicationNameLongerThan70CharactersApplicationNameLongerThan70Characters";
    private static final String APPLICATION_DUPLICATE_NAME = "Application";
    private static final String APPLICATION_DESCRIPTION = "Application description";

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {
        String storeURL;
        Properties infraProperties;

        infraProperties = getDeploymentProperties();
        storeURL = infraProperties.getProperty(STORE_URL);

        if (storeURL == null) {
            storeURL = DEFAULT_STORE_URL;
        }
        setKeyStoreProperties();
        apiStore = new APIStoreRestClient(storeURL);
        apiStore.login(ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
    }

    @Test(description = "4.1.1.5")
    public void testDuplicateApplicationName() throws Exception {
//        create application
        HttpResponse addApplicationResponse = apiStore
                .addApplication(URLEncoder.encode(APPLICATION_DUPLICATE_NAME_CHECK, UTF_8),
                        URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8),
                        "", URLEncoder.encode("", UTF_8));
//        validate application created successfully
        assertEquals(addApplicationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                ERROR_IN_RESPONSE_CODE_ADD_APP + APPLICATION_DUPLICATE_NAME_CHECK);
        JSONObject addApplicationJsonObject = new JSONObject(addApplicationResponse.getData());
        assertFalse(addApplicationJsonObject.getBoolean(ERROR),
                ERROR_APP_CREATION_FAILED + APPLICATION_DUPLICATE_NAME_CHECK);
        assertEquals(addApplicationJsonObject.get(STATUS), STATUS_APPROVED,
                ERROR_APP_CREATION_FAILED + APPLICATION_DUPLICATE_NAME_CHECK);
        applicationsList.add(APPLICATION_DUPLICATE_NAME_CHECK);
//        add duplicate application - case sensitive
        addApplicationResponse = apiStore
                .addApplication(URLEncoder.encode(APPLICATION_DUPLICATE_NAME, UTF_8),
                        URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8),
                        "", URLEncoder.encode(APPLICATION_DESCRIPTION, UTF_8));
        addApplicationJsonObject = new JSONObject(addApplicationResponse.getData());
//        if application added due to test failure add it to application list so that it could be removed later
        if (!addApplicationJsonObject.getBoolean(ERROR)
                && addApplicationJsonObject.get(STATUS).equals(STATUS_APPROVED)) {
            applicationsList.add(APPLICATION_DUPLICATE_NAME);
        }
//        validate application wasn't created
        assertTrue(addApplicationJsonObject.getBoolean(ERROR),
                ERROR_DUPLICATE_APPLICATION_CREATION + APPLICATION_DUPLICATE_NAME);
        assertEquals(addApplicationJsonObject.get(MESSAGE).toString().trim(),
                ERROR_DUPLICATE_APPLICATION_EXIST + APPLICATION_DUPLICATE_NAME,
                ERROR_DUPLICATE_APPLICATION_CREATION + APPLICATION_DUPLICATE_NAME);
//        todo uncomment if duplicate name check should be case insensitive
////        add duplicate application - case insensitive
//        addApplicationResponse = apiStore
//                .addApplication(URLEncoder.encode(APPLICATION_DUPLICATE_NAME.toLowerCase(), UTF_8),
//                        URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8),
//                        "", URLEncoder.encode(APPLICATION_DESCRIPTION, UTF_8));
//        addApplicationJsonObject = new JSONObject(addApplicationResponse.getData());
////        if application added due to test failure add it to application list so that it could be removed later
//        if (!addApplicationJsonObject.getBoolean(ERROR)
//                && addApplicationJsonObject.get(STATUS).equals(STATUS_APPROVED)) {
//            applicationsList.add(APPLICATION_DUPLICATE_NAME);
//        }
////        validate application wasn't created
//        assertTrue(addApplicationJsonObject.getBoolean(ERROR),
//                ERROR_DUPLICATE_APPLICATION_CREATION + APPLICATION_DUPLICATE_NAME);
//        assertEquals(addApplicationJsonObject.get(MESSAGE).toString().trim(),
//                ERROR_DUPLICATE_APPLICATION_EXIST + APPLICATION_DUPLICATE_NAME,
//                ERROR_DUPLICATE_APPLICATION_CREATION + APPLICATION_DUPLICATE_NAME);
    }

//    todo uncomment once jappery api validation is fixed
//    @Test(description = "4.1.1.6")
//    public void testApplicationNameLongerThan70CharactersDataProvider() throws Exception {
////        create application
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

    @Test(description = "4.1.1.7", dataProvider = "InvalidMandatoryApplicationValuesDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testApplicationCreationWithInvalidMandatoryValues(String applicationName, String tier,
                                                                  String description) throws Exception {
//        create application
        HttpResponse addApplicationResponse = apiStore
                .addApplication(URLEncoder.encode(applicationName, UTF_8), URLEncoder.encode(tier, UTF_8)
                        , "", URLEncoder.encode(description, UTF_8));
        JSONObject addApplicationJsonObject = new JSONObject(addApplicationResponse.getData());
//        if application added due to test failure add it to application list so that it could be removed later
        if (!addApplicationJsonObject.getBoolean(ERROR)
                && addApplicationJsonObject.get(STATUS).equals(STATUS_APPROVED)) {
            applicationsList.add(applicationName);
        }
//        validate application wasn't created
        assertTrue(addApplicationJsonObject.getBoolean(ERROR),
                ERROR_IN_APP_CREATION_WITH_INVALID_INPUT + applicationName);
    }

    @AfterMethod(alwaysRun = true)
    public void destroy() throws Exception {
        for (String name : applicationsList) {
            apiStore.removeApplication(URLEncoder.encode(name, UTF_8));
        }
        applicationsList.clear();
    }

}
