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

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ApplicationCreationNegativeTestCases extends ScenarioTestBase {
    private APIStoreRestClient apiStore;
    private List<String> applicationsList = new ArrayList<>();
    private String storeURL;
    private static final String ADMIN_LOGIN_USERNAME = "admin";
    private static final String ADMIN_LOGIN_PW = "admin";
    private static final String DEFAULT_STORE_URL = "https://localhost:9443/";
    private static final String UTF_8 = "UTF-8";
    private static final String ERROR_APP_CREATION_FAILED = "Application creation failed for application: ";
    private static final String ERROR_APP_CREATION_NEGATIVE_TEST = "Error in application creation" +
            " negative test cases. Application: ";
    private static final String ERROR_APPLICATION_NAME_LONGER_THAN_70_CHARACTERS
            = "Application name longer than 70 characters. Application: ";
    private static final String ERROR_DUPLICATE_APPLICATION_EXIST = "A duplicate application already exists" +
            " by the name - ";
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
    public void testApplicationCreationWithMissingMandatoryValues() throws Exception {
        String urlPrefix = "{{backendURL}}store/site/blocks/application/application-add/ajax/application-add.jag?" +
                "action=addApplication";
        String[][] appDetails = {{"", urlPrefix + "&tier=" + APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED +
                "&callbackUrl=&description=description", "Missing parameters."},
                {"application-missingMandatory1", urlPrefix + "&tier="
                        + APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED +
                        "&callbackUrl=&application=application-missingMandatory1", "Missing parameters."},
                {"application-missingMandatory2", urlPrefix + "&callbackUrl=&description=description" +
                        "&application=application-missingMandatory2", "Specified application tier does not exist."}};

        for(String[] appDetail : appDetails) {
            HttpResponse addApplicationResponse = apiStore.addApplication(
                    appDetail[1].replace("{{backendURL}}", storeURL));
            verifyApplicationNotCreated(addApplicationResponse, appDetail[2], appDetail[0]);
        }
    }

    @Test(description = "4.1.1.6")
    public void testApplicationCreationWithInvalidMandatoryValues() throws Exception {
        String[][] appDetails = {
            {" App 1", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "New App Description",
                    "Application name cannot contain leading or trailing white spaces"},
            {"App 2 ", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "New App Description",
                    "Application name cannot contain leading or trailing white spaces"},
//                todo fix the error message when the fix to remove ["application"] is working
            {"App !@#$%^", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "",
                    "Invalid inputs [\"application\"]"},
            {" ", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "Application Name is empty."},
            {"App 3", "", "New App Description", "Specified application tier does not exist."},
            {"App 4", "TierAbc", "New App Description", "Specified application tier does not exist."},
        };

        for(String[] appDetail:appDetails) {
            HttpResponse addApplicationResponse = apiStore
                    .addApplication(URLEncoder.encode(appDetail[0], UTF_8), URLEncoder.encode(appDetail[1], UTF_8)
                            , "", URLEncoder.encode(appDetail[2], UTF_8));
            verifyApplicationNotCreated(addApplicationResponse, appDetail[3], appDetail[0]);
        }
    }

    @Test(description = "4.1.1.7")
    public void testDuplicateApplicationName() throws Exception {
        HttpResponse addApplicationResponse = apiStore
                .addApplication(URLEncoder.encode(APPLICATION_DUPLICATE_NAME_CHECK, UTF_8),
                        URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8),
                        "", URLEncoder.encode("", UTF_8));
        verifyResponse(addApplicationResponse);
        assertEquals(new JSONObject(addApplicationResponse.getData()).get(STATUS), STATUS_APPROVED,
                ERROR_APP_CREATION_FAILED + APPLICATION_DUPLICATE_NAME_CHECK);
        applicationsList.add(APPLICATION_DUPLICATE_NAME_CHECK);
//        add duplicate application - case sensitive
        addApplicationResponse = apiStore
                .addApplication(URLEncoder.encode(APPLICATION_DUPLICATE_NAME, UTF_8),
                        URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8),
                        "", URLEncoder.encode(APPLICATION_DESCRIPTION, UTF_8));
        verifyApplicationNotCreated(addApplicationResponse,
                ERROR_DUPLICATE_APPLICATION_EXIST + APPLICATION_DUPLICATE_NAME,
                APPLICATION_DUPLICATE_NAME);
//        todo uncomment if duplicate name check should be case insensitive
////        add duplicate application - case insensitive
//        addApplicationResponse = apiStore
//                .addApplication(URLEncoder.encode(APPLICATION_DUPLICATE_NAME.toLowerCase(), UTF_8),
//                        URLEncoder.encode(APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, UTF_8),
//                        "", URLEncoder.encode(APPLICATION_DESCRIPTION, UTF_8));
//        verifyApplicationNotCreated(addApplicationResponse,
//                ERROR_DUPLICATE_APPLICATION_EXIST + APPLICATION_DUPLICATE_NAME,
//                APPLICATION_DUPLICATE_NAME);
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

    private void verifyApplicationNotCreated(HttpResponse response, String errorMessage, String applicationName) {
        JSONObject responseJsonObject = new JSONObject(response.getData());
//        if application added due to test failure add it to application list so that it could be removed later
        if (!responseJsonObject.getBoolean(ERROR)
                && responseJsonObject.get(STATUS).equals(STATUS_APPROVED)) {
            applicationsList.add(applicationName);
        }
//        validate application wasn't created
        assertTrue(responseJsonObject.getBoolean(ERROR),
                ERROR_APP_CREATION_NEGATIVE_TEST + applicationName);
        assertEquals(responseJsonObject.getString(MESSAGE).trim(), errorMessage,
                ERROR_APP_CREATION_NEGATIVE_TEST + applicationName);
    }


    @AfterMethod(alwaysRun = true)
    public void destroy() throws Exception {
        for (String name : applicationsList) {
            apiStore.removeApplication(URLEncoder.encode(name, UTF_8));
        }
        applicationsList.clear();
    }

}
