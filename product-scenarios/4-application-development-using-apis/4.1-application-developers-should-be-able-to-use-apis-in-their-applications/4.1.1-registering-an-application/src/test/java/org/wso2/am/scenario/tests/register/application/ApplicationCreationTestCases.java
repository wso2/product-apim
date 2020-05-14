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
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

public class ApplicationCreationTestCases extends ScenarioTestBase {
    private APIStoreRestClient apiStore;
    private List<String> applicationsList = new ArrayList<>();
    private final Log log = LogFactory.getLog(ApplicationCreationTestCases.class);
    private final String ADMIN_LOGIN_USERNAME = "admin";
    private final String ADMIN_LOGIN_PW = "admin";
    private final String SUBSCRIBER_LOGIN_USERNAME = "AppCreationPosSubscriber";
    private final String SUBSCRIBER_LOGIN_PW = "AppCreationPosSubscriber";
    private final String ERROR_APPLICATION_TIER_MISMATCH = "Application tier value mismatch for application: ";
    private final String ERROR_APPLICATION_DESCRIPTION_MISMATCH = "Application description value mismatch" +
            " for application: ";
    private final String ERROR_APPLICATION_TOKEN_TYPE_MISMATCH = "Application token type value mismatch" +
            " for application: ";
    private final String ERROR_GENERATING_KEY = " key generation failed for application:  ";
    private final String ERROR = "error";
    private final String STATUS = "status";
    private final String STATUS_APPROVED = "APPROVED";
    private final String NAME = "name";
    private final String TIER = "tier";
    private final String DESCRIPTION = "description";
    private final String TOKEN_TYPE = "tokenType";
    private final String APPLICATIONS = "applications";
    private final String DATA = "data";
    private final String KEY = "key";
    private final String KEY_STATE = "keyState";
    private final String APP_DETAILS = "appDetails";
    private final String KEY_TYPE = "key_type";
    private final String CONSUMER_KEY = "consumerKey";
    private final String CONSUMER_SECRET = "consumerSecret";
    private final String ACCESS_TOKEN = "accessToken";
    private final String PRODUCTION = "PRODUCTION";
    private final String SANDBOX = "SANDBOX";
    private final String APPLICATION_NAME = "Application";
    private final String APPLICATION_DESCRIPTION = "ApplicationDescription";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        apiStore = new APIStoreRestClient(storeURL);
        createUserWithSubscriberRole(SUBSCRIBER_LOGIN_USERNAME, SUBSCRIBER_LOGIN_PW,
                ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        apiStore.login(SUBSCRIBER_LOGIN_USERNAME, SUBSCRIBER_LOGIN_PW);
    }


    @Test(description = "4.1.1.1")
    public void testApplicationCreationWithMandatoryValues() throws Exception {
        HttpResponse addApplicationResponse = apiStore.addApplication(APPLICATION_NAME,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", APPLICATION_DESCRIPTION);
        applicationsList.add(APPLICATION_NAME);
        verifyResponse(addApplicationResponse);
        assertEquals(new JSONObject(addApplicationResponse.getData()).get(STATUS), STATUS_APPROVED,
                "Error in application creation with mandatory values. Application  : " + APPLICATION_NAME);
        validateApplicationWithValidMandatoryValues(APPLICATION_NAME,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, APPLICATION_DESCRIPTION);
    }

    @Test(description = "4.1.1.2", dataProvider = "OptionalApplicationValuesDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testApplicationCreationWithMandatoryAndOptionalValues(String tokenType) throws Exception {
        String applicationName = "AppToken";

        HttpResponse addApplicationResponse = apiStore.addApplicationWithTokenType(
                applicationName + tokenType, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                "", APPLICATION_DESCRIPTION, tokenType);
        applicationsList.add(applicationName + tokenType);
        verifyResponse(addApplicationResponse);
        assertEquals(new JSONObject(addApplicationResponse.getData()).get(STATUS), STATUS_APPROVED,
                "Error in application creation with mandatory and optional values. Application  : "
                        + applicationName + tokenType);
        validateApplicationWithMandatoryAndOptionsValues(applicationName + tokenType,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, APPLICATION_DESCRIPTION, tokenType);
    }

    @Test(description = "4.1.1.3", dependsOnMethods = {"testApplicationCreationWithMandatoryValues"})
    public void testGenerateProductionKeysForApplication() throws Exception {
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(APPLICATION_NAME);
        verifyKeyGeneration(apiStore.generateApplicationKey(appKeyRequestGenerator), PRODUCTION);
    }

    @Test(description = "4.1.1.4", dependsOnMethods = {"testApplicationCreationWithMandatoryValues"})
    public void testGenerateSandboxKeysForApplication() throws Exception {
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(APPLICATION_NAME);
        appKeyRequestGenerator.setKeyType(SANDBOX);
        verifyKeyGeneration(apiStore.generateApplicationKey(appKeyRequestGenerator), SANDBOX);
    }

    private void validateApplicationWithValidMandatoryValues(String applicationName, String tier, String description)
            throws Exception {
        HttpResponse getAllAppResponse = apiStore.getAllApplications();
        verifyResponse(getAllAppResponse);
        JSONArray getAllAppJsonArray = new JSONObject(getAllAppResponse.getData()).getJSONArray(APPLICATIONS);

        for (int i = 0; i < getAllAppJsonArray.length(); i++) {
            if (applicationName.equals(getAllAppJsonArray.getJSONObject(i).getString(NAME))) {
                assertEquals(getAllAppJsonArray.getJSONObject(i).getString(TIER), tier,
                        ERROR_APPLICATION_TIER_MISMATCH + applicationName);
                assertEquals(getAllAppJsonArray.getJSONObject(i).getString(DESCRIPTION), description,
                        ERROR_APPLICATION_DESCRIPTION_MISMATCH + applicationName);
            }
        }
    }

    private void validateApplicationWithMandatoryAndOptionsValues(String applicationName, String tier,
                                                                  String description, String tokenType)
            throws Exception {
        HttpResponse getAllAppResponse = apiStore.getAllApplications();
        verifyResponse(getAllAppResponse);
        JSONArray getAllAppJsonArray = new JSONObject(getAllAppResponse.getData()).getJSONArray(APPLICATIONS);

        for (int i = 0; i < getAllAppJsonArray.length(); i++) {
            if (applicationName.equals(getAllAppJsonArray.getJSONObject(i).getString(NAME))) {
                assertEquals(getAllAppJsonArray.getJSONObject(i).getString(TIER), tier,
                        ERROR_APPLICATION_TIER_MISMATCH + applicationName);
                assertEquals(getAllAppJsonArray.getJSONObject(i).getString(DESCRIPTION), description,
                        ERROR_APPLICATION_DESCRIPTION_MISMATCH + applicationName);
                assertEquals(getAllAppJsonArray.getJSONObject(i).getString(TOKEN_TYPE), tokenType,
                        ERROR_APPLICATION_TOKEN_TYPE_MISMATCH + applicationName);
            }
        }
    }

    private void verifyKeyGeneration(HttpResponse response, String keyType) {
        JSONObject responseStringJson = new JSONObject(response.getData());
        log.info(keyType + " key generation response for application \'" + APPLICATION_NAME + "\' response data :"
                + response.getData());
        assertFalse(responseStringJson.getBoolean(ERROR),
                keyType + ERROR_GENERATING_KEY + APPLICATION_NAME);
        assertEquals(responseStringJson.getJSONObject(DATA).getJSONObject(KEY).getString(KEY_STATE), STATUS_APPROVED,
                keyType + ERROR_GENERATING_KEY + APPLICATION_NAME);
        assertEquals(new JSONObject(responseStringJson.getJSONObject(DATA).getJSONObject(KEY).getString(APP_DETAILS))
                .get(KEY_TYPE), keyType, keyType + ERROR_GENERATING_KEY + APPLICATION_NAME);
        assertNotNull(responseStringJson.getJSONObject(DATA).getJSONObject(KEY).get(ACCESS_TOKEN),
                keyType + ERROR_GENERATING_KEY + APPLICATION_NAME);
        assertNotNull(responseStringJson.getJSONObject(DATA).getJSONObject(KEY).get(CONSUMER_KEY),
                keyType + ERROR_GENERATING_KEY + APPLICATION_NAME);
        assertNotNull(responseStringJson.getJSONObject(DATA).getJSONObject(KEY).get(CONSUMER_SECRET),
                keyType + ERROR_GENERATING_KEY + APPLICATION_NAME);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        for (String name : applicationsList) {
            apiStore.removeApplication(name);
        }
        applicationsList.clear();
        deleteUser(SUBSCRIBER_LOGIN_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
    }
}
