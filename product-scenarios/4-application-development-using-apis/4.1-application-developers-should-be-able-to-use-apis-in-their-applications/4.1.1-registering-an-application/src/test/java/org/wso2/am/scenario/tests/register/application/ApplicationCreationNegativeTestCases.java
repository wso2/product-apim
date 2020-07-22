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

import org.apache.commons.httpclient.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.ArrayList;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ApplicationCreationNegativeTestCases extends ScenarioTestBase {

    private final String SUBSCRIBER_LOGIN_USERNAME_2 = "AppCreationNegSubscriberB";
    private final String SUBSCRIBER_LOGIN_PW_2 = "AppCreationNegSubscriberB";
    private final String APPLICATION_DESCRIPTION = "Application description";

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";

    @Factory(dataProvider = "userModeDataProvider")
    public ApplicationCreationNegativeTestCases(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                    ADMIN_USERNAME, ADMIN_PW);
            createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, ADMIN_USERNAME, ADMIN_PW);
        }

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // create user in wso2.com tenant
            addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_USERNAME, ADMIN_PW);
            if (isActivated(ScenarioTestConstants.TENANT_WSO2)) {
                //Add and activate wso2.com tenant
                createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                        TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, TENANT_ADMIN_USERNAME,
                        TENANT_ADMIN_PW);
            }
        }
        super.init(userMode);
    }

    @Test(description = "4.1.1.5", dataProvider = "MissingMandatoryApplicationValuesDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testApplicationCreationWithMissingMandatoryValues(String applicationName,
                                                                  String url, String errorMessage) throws Exception {
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName, APPLICATION_DESCRIPTION,
                null, ApplicationDTO.TokenTypeEnum.OAUTH);
        assertEquals(applicationResponse, null, errorMessage);
    }

    @Test(description = "4.1.1.6", dataProvider = "InvalidMandatoryApplicationValuesDataProvider",
            dataProviderClass = ScenarioDataProvider.class, dependsOnMethods = "testApplicationCreationWithMissingMandatoryValues")
    public void testApplicationCreationWithInvalidMandatoryValues(String applicationName, String tier,
                                                                  String errorMessage) {
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName, APPLICATION_DESCRIPTION,
                "null", ApplicationDTO.TokenTypeEnum.OAUTH);
        assertEquals(applicationResponse, null, errorMessage);
    }

    @Test(description = "4.1.1.7", dependsOnMethods = "testApplicationCreationWithInvalidMandatoryValues")
    public void testDuplicateApplicationName() {
        String APPLICATION_NAME = "Application";
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME, APPLICATION_DESCRIPTION,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        HttpResponse applicationResponseError = restAPIStore.createApplication(APPLICATION_NAME, APPLICATION_DESCRIPTION,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        assertEquals(applicationResponseError, null, "Duplicate application creation successful.");
        restAPIStore.deleteApplication(applicationResponse.getData());

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

    @Test(description = "4.1.1.10", dependsOnMethods = {"testDuplicateApplicationName"})
    public void testTokenGenerationForOthersApplications() throws Exception {
        String APPLICATION_NAME = "Application";
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME, APPLICATION_DESCRIPTION,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUserWithSubscriberRole(SUBSCRIBER_LOGIN_USERNAME_2, SUBSCRIBER_LOGIN_PW_2,
                    ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createUserWithSubscriberRole(SUBSCRIBER_LOGIN_USERNAME_2, SUBSCRIBER_LOGIN_PW_2,
                    TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

        RestAPIStoreImpl restAPIStoreNew = new RestAPIStoreImpl(
                SUBSCRIBER_LOGIN_USERNAME_2,
                SUBSCRIBER_LOGIN_PW_2,
                storeContext.getContextTenant().getDomain(), storeURLHttps);

        try {
            ArrayList grantTypes = new ArrayList();
            grantTypes.add("client_credentials");
            grantTypes.add("password");
            restAPIStoreNew.generateKeys(applicationResponse.getData(), "3600", null,
                    ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
            assertTrue(false, "Key generated a different owners application");
        } catch (ApiException e) {
            restAPIStore.deleteApplication(applicationResponse.getData());
            assertTrue(true);
        }
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            // deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            // deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            deleteUser(SUBSCRIBER_LOGIN_USERNAME_2, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            // deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deleteUser(SUBSCRIBER_LOGIN_USERNAME_2, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            // deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        setup();
        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_USER},
                new Object[]{TestUserMode.TENANT_USER},
        };
    }
}
