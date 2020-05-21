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

import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.ArrayList;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class ApplicationCreationTestCases extends ScenarioTestBase {

    private final String STATUS_APPROVED = "APPROVED";
    private final String APPLICATION_DESCRIPTION = "ApplicationDescription";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";

    @Factory(dataProvider = "userModeDataProvider")
    public ApplicationCreationTestCases(TestUserMode userMode) {
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

    @Test(description = "4.1.1.1")
    public void testApplicationCreationWithMandatoryValues() throws Exception {
        String APPLICATION_NAME = "Application";
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME, APPLICATION_DESCRIPTION,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
        verifyResponse(applicationResponse);

        Gson g = new Gson();
        ApplicationDTO applicationDTO = g.fromJson(restAPIStore
                .getApplicationById(applicationResponse.getData()).getData(), ApplicationDTO.class);

        assertEquals(applicationDTO.getName(), APPLICATION_NAME,
                "Error in application creation with mandatory values. Application  : " + APPLICATION_NAME);
        assertEquals(applicationDTO.getDescription(), APPLICATION_DESCRIPTION,
                "Error in application creation with mandatory values. Application  : " + APPLICATION_NAME);
        assertEquals(applicationDTO.getStatus(), STATUS_APPROVED,
                "Error in application creation with mandatory values. Application  : " + APPLICATION_NAME);
        restAPIStore.deleteApplication(applicationResponse.getData());
    }

    @Test(description = "4.1.1.2", dataProvider = "OptionalApplicationValuesDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testApplicationCreationWithMandatoryAndOptionalValues(String tokenType) throws Exception {
        String applicationName = "AppToken";
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                APPLICATION_DESCRIPTION, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        verifyResponse(applicationResponse);
        JSONObject applicationData = new JSONObject(restAPIStore.getApplicationById(applicationResponse.getData()).getData());
        String STATUS = "status";
        assertEquals(applicationData.get(STATUS), STATUS_APPROVED,
                "Error in application creation with mandatory and optional values. Application STATUS");
        String DESCRIPTION = "description";
        assertEquals(applicationData.get(DESCRIPTION), APPLICATION_DESCRIPTION,
                "Error in application creation with mandatory and optional values. Application DESCRIPTION");
        String THROTTLING_POLICY = "throttlingPolicy";
        assertEquals(applicationData.get(THROTTLING_POLICY), APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                "Error in application creation with mandatory and optional values. Application TIER");
        String TOKEN_TYPE = "tokenType";
        assertEquals(applicationData.get(TOKEN_TYPE), ApplicationDTO.TokenTypeEnum.OAUTH.getValue(),
                "Error in application creation with mandatory and optional values. Application TOKEN_TYPE");
        restAPIStore.deleteApplication(applicationResponse.getData());
    }

    @Test(description = "4.1.1.3", dependsOnMethods = {"testApplicationCreationWithMandatoryValues"})
    public void testGenerateProductionKeysForApplication() throws Exception {
        String applicationName = "AppToken";
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                APPLICATION_DESCRIPTION, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");
        grantTypes.add("password");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationResponse.getData(), "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String consumerKey = applicationKeyDTO.getConsumerKey();
        String consumerSecret = applicationKeyDTO.getConsumerSecret();
        assertNotNull(consumerKey, "consumer key is not found");
        assertNotNull(consumerSecret, "consumer secret is not found");
        restAPIStore.deleteApplication(applicationResponse.getData());
    }

    @Test(description = "4.1.1.4", dependsOnMethods = {"testApplicationCreationWithMandatoryValues"})
    public void testGenerateSandboxKeysForApplication() throws Exception {
        String applicationName = "AppToken";
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                APPLICATION_DESCRIPTION, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");
        grantTypes.add("password");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationResponse.getData(), "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, null, grantTypes);
        String consumerKey = applicationKeyDTO.getConsumerKey();
        String consumerSecret = applicationKeyDTO.getConsumerSecret();
        assertNotNull(consumerKey, "consumer key is not found");
        assertNotNull(consumerSecret, "consumer secret is not found");
        restAPIStore.deleteApplication(applicationResponse.getData());
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() throws Exception {
        setup();
        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][]{
//                new Object[]{TestUserMode.SUPER_TENANT_USER},
                new Object[]{TestUserMode.TENANT_USER},
        };
    }
}
