/*
 *Copyright (c), WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.scenario.tests.define.subscription.tiers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class SubscribeToAssignedTiersNegativeTestCase extends ScenarioTestBase {

    private final Log log = LogFactory.getLog(SubscribeToAssignedTiersTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private APIRequest apiRequest;

    private String apiNameNoTiers = "API_1";
    private String apiContextNoTiers = "/api1";
    private String apiNameSubscribeToNotAssigned = "API_2";
    private String apiContextSubscribeToNotAssigned = "/api2";
    private String endpointUrl = "http://test";
    private String goldTier = "Gold";
    private String silverTier = "Silver";
    private String apiVersion = "1.0.0";
    private String apiVisibility = "public";
    private String providerName = "admin";
    private String apiResource = "/groups";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    private String applicationName = "NewApplication";
    private String applicationDescription = "Application_Description";

    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";


    @Factory(dataProvider = "userModeDataProvider")
    public SubscribeToAssignedTiersNegativeTestCase(TestUserMode userMode) {
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

    @Test(description = "7.1.1.5")
    public void testCreateAPIWithNoSubscriptionTiers() throws Exception {

        APIDTO apiDto = new APIDTO();
        apiDto.setName(apiNameNoTiers);
        apiDto.setContext(apiContextNoTiers);
        apiDto.setVersion(apiVersion);

        org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
        jsonObject.put("endpoint_type", "http");
        org.json.simple.JSONObject sandUrl = new org.json.simple.JSONObject();
        sandUrl.put("url", endpointUrl);
        jsonObject.put("sandbox_endpoints", sandUrl);
        jsonObject.put("production_endpoints", sandUrl);
        apiDto.setEndpointConfig(jsonObject);
        ArrayList<String> gatewayEnvironments = new ArrayList<>();
        gatewayEnvironments.add("Production and Sandbox");
        apiDto.setGatewayEnvironments(gatewayEnvironments);
        apiDto.setPolicies(null);
        APIDTO response = restAPIPublisher.addAPI(apiDto, "3.0");
        assertNotNull(response.getId());

        try {
            restAPIPublisher.changeAPILifeCycleStatus(response.getId(), APILifeCycleAction.PUBLISH.getAction(), null);
        } catch (ApiException e) {
            assertTrue(e.getResponseBody().contains("Error"));
        }
        restAPIPublisher.deleteAPI(response.getId());
    }

    @Test(description = "7.1.1.6", dependsOnMethods = "testCreateAPIWithNoSubscriptionTiers")
    public void testSubscribeWithTierNotAssignedToAPI() throws Exception {

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiNameSubscribeToNotAssigned, apiContextSubscribeToNotAssigned, apiVersion,
                API_CREATOR_PUBLISHER_USERNAME, new URL(endpointUrl));

        apiCreationRequestBean.setSubPolicyCollection(goldTier);
        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);
        String apiId1 = apiDto.getId();
        restAPIPublisher.changeAPILifeCycleStatus(apiId1, APILifeCycleAction.PUBLISH.getAction(), null);

        // wait till API indexed in Store
        isAPIVisibleInStore(apiId1);

        //Create Application for single tier
        HttpResponse addApplicationResponse = restAPIStore.createApplication(applicationName,
                applicationDescription, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        String applicationId1 = addApplicationResponse.getData();
        verifyResponse(addApplicationResponse);
        //Subscribe to the API with single tier
        HttpResponse subscriptionResponse = restAPIStore.createSubscription(apiId1, applicationId1, silverTier);
        assertEquals("Subscribed to an incorrect tier.", null, subscriptionResponse);
        restAPIPublisher.deleteAPI(apiId1);
        restAPIStore.deleteApplication(applicationId1);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            // deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            // deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            // deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            // deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
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
