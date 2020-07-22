/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.am.scenario.tests.rest.api.publicVisibility;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.net.URL;
import java.util.ArrayList;

import static org.testng.Assert.assertNotNull;

public class PublicRestApiVisibilityTestCase extends ScenarioTestBase {

    private String apiName = "PhoneVerification1";
    private String apiContext = "/verify";
    private String apiVersion = "1.0.0";
    private String tierCollection = "Gold,Bronze";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";

    private static final Log log = LogFactory.getLog(PublicRestApiVisibilityTestCase.class);
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";
    private static String apiID;


    @Factory(dataProvider = "userModeDataProvider")
    public PublicRestApiVisibilityTestCase(TestUserMode userMode) {
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
//                Add and activate wso2.com tenant
                createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                        TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, TENANT_ADMIN_USERNAME,
                        TENANT_ADMIN_PW);
            }
        }
        super.init(userMode);
    }

    @Test(description = "1.5.1.1 and 1.5.1.2")
    public void testVisibilityOfPublicAPIsWithoutLogin() throws Exception {

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion,
                "admin", new URL(backendEndPoint));
        apiCreationRequestBean.setTiersCollection(tierCollection);
        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);
        apiID = apiDto.getId();
        assertNotNull(apiDto.getId(), "API creation fails");
        restAPIPublisher.changeAPILifeCycleStatus(apiID, APILifeCycleAction.PUBLISH.getAction(), null);

        ApiClient existingAPIClient = restAPIStore.apIsApi.getApiClient();
        // Setting an unAuthenticatedClient.
        ApiClient unAuthenticatedApiClient = new ApiClient();
        unAuthenticatedApiClient.setBasePath(existingAPIClient.getBasePath());
        restAPIStore.apIsApi.setApiClient(unAuthenticatedApiClient);

        // Check visibility in store for unAuthenticated user.
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseStore = restAPIStore.getAPI(apiDto.getId());
            Assert.assertEquals(apiResponseStore.getId(), apiDto.getId(), "Response object API id mismatch");
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseStore = restAPIStore.apIsApi.apisApiIdGet(apiID, "wso2.com", null);
            Assert.assertEquals(apiResponseStore.getId(), apiDto.getId(), "Response object API id mismatch");
        }
        // Setting the authenticated Client again.
        restAPIStore.apIsApi.setApiClient(existingAPIClient);
        restAPIPublisher.deleteAPI(apiDto.getId());
    }

    @Test(description = "1.5.1.2")
    public void testVisibilityOfPublicAPIResourcesWithoutLogin() throws Exception {
        String resourcePath = "path1";
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion,
                "admin", new URL(backendEndPoint));
        apiCreationRequestBean.setTiersCollection(tierCollection);
        ArrayList<APIResourceBean> resourceBeanArrayList = new ArrayList<>();
        resourceBeanArrayList.add(new APIResourceBean(APIMIntegrationConstants.HTTP_VERB_GET,
                APIMIntegrationConstants.RESOURCE_AUTH_TYPE_NONE, APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED, resourcePath));
        apiCreationRequestBean.setResourceBeanList(resourceBeanArrayList);
        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);
        apiID = apiDto.getId();
        assertNotNull(apiDto.getId(), "API creation fails");
        restAPIPublisher.changeAPILifeCycleStatus(apiID, APILifeCycleAction.PUBLISH.getAction(), null);

        ApiClient existingAPIClient = restAPIStore.apIsApi.getApiClient();
        // Setting an unAuthenticatedClient.
        ApiClient unAuthenticatedApiClient = new ApiClient();
        unAuthenticatedApiClient.setBasePath(existingAPIClient.getBasePath());
        restAPIStore.apIsApi.setApiClient(unAuthenticatedApiClient);

        // Check visibility in store for unAuthenticated user.
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseStore = restAPIStore.getAPI(apiDto.getId());
            Assert.assertEquals(apiResponseStore.getId(), apiDto.getId(), "Response object API id mismatch");
            Assert.assertTrue(apiResponseStore.getApiDefinition().contains(resourcePath), "Response path mismatch");
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseStore = restAPIStore.apIsApi.apisApiIdGet(apiID, "wso2.com", null);
            Assert.assertEquals(apiResponseStore.getId(), apiDto.getId(), "Response object API id mismatch");
            Assert.assertTrue(apiResponseStore.getApiDefinition().contains(resourcePath), "Response path mismatch");
        }
        // Setting the authenticated Client again.
        restAPIStore.apIsApi.setApiClient(existingAPIClient);
        restAPIPublisher.deleteAPI(apiDto.getId());
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
