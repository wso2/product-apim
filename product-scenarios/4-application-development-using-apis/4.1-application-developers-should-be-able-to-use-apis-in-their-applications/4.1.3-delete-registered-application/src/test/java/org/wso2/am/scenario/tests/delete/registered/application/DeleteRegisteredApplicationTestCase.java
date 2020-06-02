/*
 *Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.scenario.tests.delete.registered.application;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class DeleteRegisteredApplicationTestCase extends ScenarioTestBase {

    private String apiName = "";
    private String applicationName = "";
    private final Log log = LogFactory.getLog(DeleteRegisteredApplicationTestCase.class);
    private final String API_VERSION = "1.0.0";
    private final String APPLICATION_DESCRIPTION = "ApplicationDescription";
    private final String APPLICATION_NAME_PREFIX = "AppDelete_";
    private final String KEY_GENERATION_SUFFIX = "KeyGen";
    private String applicationId = null;
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";

    @Factory(dataProvider = "userModeDataProvider")
    public DeleteRegisteredApplicationTestCase(TestUserMode userMode) {
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

    @Test(description = "4.1.3.2")
    public void testDeleteApplication() {
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME_PREFIX, APPLICATION_DESCRIPTION,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
        verifyResponse(applicationResponse);
        restAPIStore.deleteApplication(applicationResponse.getData());
    }

    @Test(description = "4.1.3.3", dependsOnMethods = {"testDeleteApplication"})
    public void testRecreateDeletedApplication() throws Exception {
        applicationId = createApplication(APPLICATION_NAME_PREFIX);
        assertNotNull(applicationId);
    }

    @Test(description = "4.1.3.4", dependsOnMethods = {"testRecreateDeletedApplication"})
    public void testKeyGenerationForRecreateDeletedApplication() throws Exception {
        keyGenerationForApplication(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION);
        keyGenerationForApplication(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX);
        restAPIStore.deleteApplication(applicationId);
    }

    @Test(description = "4.1.3.1")
    public void testDeleteApplicationWithSubscription() throws Exception {
//        delete app with subscriptions
        String API_NAME_PREFIX = "AppDeleteAPI_";
        String WITH_SUBSCRIPTION_SUFFIX = "WithSubs";
        apiName = API_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX;
        applicationName = API_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX;

        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME_PREFIX, APPLICATION_DESCRIPTION,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        String applicationId = applicationResponse.getData();

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, "apiContext2", API_VERSION,
                "admin", new URL("http://ws.cdyne.com/phoneverify/phoneverify.asmx"));

        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);
        String apiID = apiDto.getId();
        restAPIPublisher.changeAPILifeCycleStatus(apiID, APILifeCycleAction.PUBLISH.getAction(), null);

        restAPIStore.createSubscription(apiID, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        restAPIStore.deleteApplication(applicationId);
        verifyRemovalOfSubscriptionToAPI(apiID);

//        delete applications with keys
        applicationDeletionWithKeys(ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION);
        applicationDeletionWithKeys(ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX);

//        delete app with subscription and keys
        apiName = API_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX + KEY_GENERATION_SUFFIX;
        applicationName = API_NAME_PREFIX + WITH_SUBSCRIPTION_SUFFIX + KEY_GENERATION_SUFFIX;

        String apiID2 = createAndPublishAPI(apiName);
        String applicationId2 = createApplication(applicationName);
        restAPIStore.createSubscription(applicationId2, apiID2, APIMIntegrationConstants.API_TIER.UNLIMITED);
        keyGenerationForApplication(applicationId2, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION);
        keyGenerationForApplication(applicationId2, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX);
        restAPIStore.deleteApplication(applicationId2);
        verifyRemovalOfSubscriptionToAPI(apiID);
        restAPIPublisher.deleteAPI(apiID);
        restAPIPublisher.deleteAPI(apiID2);
    }

    private String createApplication(String applicationName) throws Exception {
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName, APPLICATION_DESCRIPTION,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        return applicationResponse.getData();
    }


    private String createAPI(String apiName) throws Exception {
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, "apiContext", API_VERSION,
                "admin", new URL("http://ws.cdyne.com/phoneverify/phoneverify.asmx"));
        APIDTO apidto = restAPIPublisher.addAPI(apiCreationRequestBean);
        return apidto.getId();
    }

    private String createAndPublishAPI(String apiName) throws Exception {
        String apiId = createAPI(apiName);
        HttpResponse stateChangeRequest = restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);
        log.info("API publish response code: " + stateChangeRequest.getResponseCode());
        log.info("API publish response data: " + stateChangeRequest.getData());
        assertTrue(stateChangeRequest.getData().contains("Published"), "API has not been created in publisher");
        return apiId;
    }

    private void verifyRemovalOfSubscriptionToAPI(String apiId) throws Exception {
//        verify subscription is removed from api
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiInfo = restAPIStore.getAPI(apiId);
        assertNotNull(apiInfo.getId());
        SubscriptionListDTO subscriptionListDTO = restAPIStore.getSubscription(apiId, null,
                null, null);
        Assert.assertFalse(new JSONObject(apiInfo).toString().contains("subs"),
                "Incorrect subscription count for api \'" + apiName + "\'");
    }

    private void applicationDeletionWithKeys(ApplicationKeyGenerateRequestDTO.KeyTypeEnum keyType) throws Exception {
        applicationName = APPLICATION_NAME_PREFIX + KEY_GENERATION_SUFFIX + keyType;
        String applicationId = createApplication(applicationName);
        keyGenerationForApplication(applicationId, keyType);
        restAPIStore.deleteApplication(applicationId);
    }

    private void keyGenerationForApplication(String applicationID, ApplicationKeyGenerateRequestDTO.KeyTypeEnum keyType) throws Exception {

        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");
        grantTypes.add("password");
        ApplicationKeyDTO generatedKeys = restAPIStore.generateKeys(applicationID, "3600", null,
                keyType, null, grantTypes);
        String consumerKey = generatedKeys.getConsumerKey();
        String consumerSecret = generatedKeys.getConsumerSecret();
        Assert.assertNotNull(consumerKey, "Error in generating keys for application, consumer key not found");
        Assert.assertNotNull(consumerSecret, "Error in generating keys for application, consumerSecret key not found");
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
                new Object[]{TestUserMode.SUPER_TENANT_USER},
                new Object[]{TestUserMode.TENANT_USER},
        };
    }
}