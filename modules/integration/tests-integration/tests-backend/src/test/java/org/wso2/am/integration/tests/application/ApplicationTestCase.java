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

package org.wso2.am.integration.tests.application;

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
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;
import static org.testng.Assert.assertTrue;

public class ApplicationTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(ApplicationTestCase.class);
    private static final String webApp = "jaxrs_basic";
    private final String version = "1.0.0";
    private final String visibility = "public";
    private final String description = "API subscription";
    private final String tier = "Unlimited";
    private final String tags = "subscription";
    private final String applicationName = "NewApplicationTest";
    private final String newApplicationName = "UpdatedApplicationTest";
    private final String endPointType = "http";
    private String apiName = "SubscriptionAPITest";
    private String apiContext = "subscriptionapicontext";
    private int applicationId;
    private String providerName;
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    @Factory(dataProvider = "userModeDataProvider")
    public ApplicationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN }, };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        log.info("Test Starting user mode:" + userMode);

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        String uri = "customers/{id}/";
        List<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();
        resourceBeanList.add(new APIResourceBean("GET", "Application & Application User", tier, uri));
        String endpoint = "/services/customers/customerservice";

        String endpointUrl = gatewayUrlsWrk.getWebAppURLHttp() + webApp + endpoint;
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();

        String tempApiName = apiName;
        String tempApiContext = apiContext;
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(tempApiName, tempApiContext, version,
                providerName, new URL(endpointUrl));
        apiCreationRequestBean.setEndpointType(endPointType);
        apiCreationRequestBean.setTiersCollection(tier);
        apiCreationRequestBean.setTags(tags);
        apiCreationRequestBean.setResourceBeanList(resourceBeanList);
        apiCreationRequestBean.setDescription(description);
        apiCreationRequestBean.setVisibility(visibility);

        HttpResponse apiCreateResponse = apiPublisher.addAPI(apiCreationRequestBean);
        verifyResponse(apiCreateResponse);

        //assert JSON object
        JSONObject createApiJsonObject = new JSONObject(apiCreateResponse.getData());
        assertEquals(createApiJsonObject.getBoolean("error"), false, "Error in API Creation");

        HttpResponse verifyApiResponse = apiPublisher.getApi(tempApiName, providerName, version);
        JSONObject verifyApiJsonObject = new JSONObject(verifyApiResponse.getData());
        assertFalse(verifyApiJsonObject.getBoolean("error"), "Error in Verify API Response");

        //publish API
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(tempApiName, providerName,
                APILifeCycleState.PUBLISHED);

        HttpResponse statusUpdateResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(statusUpdateResponse);

        JSONObject statusUpdateJsonObject = new JSONObject(statusUpdateResponse.getData());
        assertFalse(statusUpdateJsonObject.getBoolean("error"), "API is not published");

        providerName = storeContext.getContextTenant().getContextUser().getUserName();

        //create Application
        HttpResponse createAppResponse = apiStore.addApplication(applicationName, tier, "", "");
        verifyResponse(createAppResponse);
        JSONObject createAppJsonObject = new JSONObject(createAppResponse.getData());
        assertFalse(createAppJsonObject.getBoolean("error"),
                "Error in Application creation Response: " + applicationName);
        applicationId = createAppJsonObject.getInt("applicationId");

    }

    @Test(groups = {"webapp"}, description = "Get Application By Application Id")
    public void testGetApplicationById() throws Exception {
        JSONObject applicationObject = getApplicationById();
        assertEquals(applicationObject.getString("name"), applicationName, "Application name is Mismatched");
    }

    @Test(groups = {
            "webapp" }, description = "Get Application By Application Id", dependsOnMethods = "testGetApplicationById")
    public void testApplicationKeyGenerationById() throws Exception {
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(applicationName);
        generateAppKeyRequest.setAppId(Integer.toString(applicationId));
        generateAppKeyRequest.setAction("generateApplicationKeyByApplicationId");
        HttpResponse applicationKeyResponse = apiStore.generateApplicationKeyById(generateAppKeyRequest);
        verifyResponse(applicationKeyResponse);
        JSONObject responseData = new JSONObject(applicationKeyResponse.getData());
        assertNotNull(responseData.getJSONObject("data").getJSONObject("key").get("accessToken"));
    }

    @Test(groups = {"webapp" }, description = "Update Client Application By Application Id",
            dependsOnMethods = "testApplicationKeyGenerationById")
    public void testUpdateClientApplicationById() throws Exception {
        String keyType = "PRODUCTION";
        String authorizedDomains = "ALL";
        String retryAfterFailure = String.valueOf(false);
        String jsonParams = "{\"grant_types\":\"urn:ietf:params:oauth:grant-type:saml2-bearer,iwa:ntlm\"}";
        String callbackUrl = "test-callback";

        HttpResponse response = apiStore
                .updateClientApplicationById(applicationId, applicationName, keyType, authorizedDomains,
                        retryAfterFailure, jsonParams, callbackUrl);
        verifyResponse(response);
    }

    @Test(groups = {"webapp" },
            description = "Update Application By Application Id", dependsOnMethods = "testUpdateClientApplicationById")
    public void testUpdateApplicationById() throws Exception {
        HttpResponse updateApplicationResponse = apiStore
                .updateApplicationById(applicationId, applicationName, newApplicationName, "test-url",
                        "this-is-updated", "bronze");
        verifyResponse(updateApplicationResponse);
        JSONObject applicationObject = getApplicationById();
        assertEquals(applicationObject.getString("name"), newApplicationName, "Application name is Mismatched");
    }

    @Test(groups = {"webapp" },
            description = "Add subscription By Application Id", dependsOnMethods = "testUpdateApplicationById")
    public void testAddSubscriptionApplicationById() throws Exception {
        //subscribe to the api
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, version, providerName,
                applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);
        HttpResponse serviceResponse = apiStore.subscribe(subscriptionRequest, "addAPISubscriptionByAppId");
        verifyResponse(serviceResponse);
    }

    @Test(groups = {"webapp" },
            description = "Add subscription By Application Id", dependsOnMethods = "testUpdateApplicationById")
    public void testGetSubscriptionForApplicationById() throws Exception {
        HttpResponse publishedApiByAppResponse;
        JSONObject publishedApiByAppJsonObject;
        //get first set of apis by created application
        publishedApiByAppResponse = apiStore.getPublishedAPIsByApplicationId(newApplicationName, applicationId);
        verifyResponse(publishedApiByAppResponse);
        publishedApiByAppJsonObject = new JSONObject(publishedApiByAppResponse.getData());
        log.info(publishedApiByAppJsonObject);
        JSONArray applicationSubscribedJsonArray = publishedApiByAppJsonObject.getJSONArray("apis");

        //verify application names response
        boolean isApiAvailable = false;
        for (int index = 0; index < applicationSubscribedJsonArray.length(); index++) {
            if (apiName.equals(applicationSubscribedJsonArray.getJSONObject(index).getString("apiName"))) {
                isApiAvailable = true;
                break;
            }
        }
        assertTrue(isApiAvailable,"Response Error in Api");
    }

    @Test(groups = {"webapp" }, description = "Add subscription By Application Id",
            dependsOnMethods = "testGetSubscriptionForApplicationById")
    public void testCleanupApplicationRegistrationById() throws Exception {
        HttpResponse cleanupAppResponse;
        cleanupAppResponse = apiStore.cleanUpApplicationRegistrationByApplicationId(applicationId, newApplicationName);
        verifyResponse(cleanupAppResponse);
    }

    @Test(groups = {"webapp" }, description = "Remove application By Application Id",
            dependsOnMethods = "testCleanupApplicationRegistrationById")
    public void testRemoveApplicationById() throws Exception {
        HttpResponse removeAppResponse = apiStore.removeApplicationById(applicationId);
        verifyResponse(removeAppResponse);
        JSONObject json = new JSONObject(removeAppResponse.getData());
        assertTrue(json.isNull("application"), "Application with ID: " + applicationId + " not removed.");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }

    private JSONObject getApplicationById() throws Exception{
        HttpResponse applicationResponse = apiStore.getApplicationById(applicationId);
        verifyResponse(applicationResponse);
        JSONObject applicationResponseJsonObject = new JSONObject(applicationResponse.getData());
        assertFalse(applicationResponseJsonObject.getBoolean("error"), "Application Response is Mismatched");
        return applicationResponseJsonObject.getJSONObject("application");
    }
}
