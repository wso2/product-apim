/*
 *
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;

/**
 * APIM2-710:List all the the subscriptions by application
 * APIM2-711:Remove a subscription from the application with application name through store rest api
 * APIM2-713:Remove a subscription from the application with application id through store rest api
 */

public class APIM710AllSubscriptionsByApplicationTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(APIM710AllSubscriptionsByApplicationTestCase.class);
    private final String API_NAME = "SubscriptionByApplication";
    private final String API_NAME1 = "SubscriptionByApplication1";
    private final String API_CONTEXT = "SubscriptionByApplicationContext";
    private final String API_CONTEXT1 = "SubscriptionByApplicationContext1";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private APIRequest apiRequest;
    private ArrayList<String> applicationList = new ArrayList<>();
    private int listcount = 3;
    private ArrayList<String> apiList1 = new ArrayList<>();
    private ArrayList<String> apiList2 = new ArrayList<>();


    @Factory(dataProvider = "userModeDataProvider")
    public APIM710AllSubscriptionsByApplicationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        log.info("Test Starting user mode:" + userMode);

        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        HttpResponse applicationResponse = restAPIStore.createApplication("app1",
                "app1", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationList.add(0, applicationResponse.getData());
        HttpResponse applicationResponse2 = restAPIStore.createApplication("app2",
                "app2", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationList.add(1, applicationResponse2.getData());

        //creating 1st set of apis and subscribe to app1
        for (int apiCount = 0; apiCount < listcount; apiCount++) {

            String tempApiName = API_NAME + apiCount;
            String tempApiContext = API_CONTEXT + apiCount;

            apiRequest = new APIRequest(tempApiName, tempApiContext, new URL(apiEndPointUrl));
            apiRequest.setVersion(API_VERSION_1_0_0);
            apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setProvider(providerName);
            apiRequest.setTags(API_TAGS);
            apiRequest.setDescription(API_DESCRIPTION);
            HttpResponse createResponse = restAPIPublisher.addAPI(apiRequest);

            assertEquals(createResponse.getResponseCode(), 201, "Error in API Creation");
            apiList1.add(apiCount, createResponse.getData());
            //publish API
            restAPIPublisher
                    .changeAPILifeCycleStatus(createResponse.getData(), APILifeCycleAction.PUBLISH.getAction(),
                            null);

            restAPIStore.createSubscription(createResponse.getData(),
                    applicationList.get(0), "Unlimited");
        }

        //creating 2nd set of apis and subscribe to app2
        for (int apiCount = 0; apiCount < listcount; apiCount++) {

            String tempApiName = API_NAME1 + apiCount;
            String tempApiContext = API_CONTEXT1 + apiCount;

            apiRequest = new APIRequest(tempApiName, tempApiContext, new URL(apiEndPointUrl));
            apiRequest.setVersion(API_VERSION_1_0_0);
            apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setProvider(providerName);
            apiRequest.setTags(API_TAGS);
            apiRequest.setDescription(API_DESCRIPTION);
            HttpResponse createResponse = restAPIPublisher.addAPI(apiRequest);

            assertEquals(createResponse.getResponseCode(), 201, "Error in API Creation");
            apiList2.add(apiCount, createResponse.getData());
            //publish API
            restAPIPublisher
                    .changeAPILifeCycleStatus(createResponse.getData(), APILifeCycleAction.PUBLISH.getAction(),
                            null);

            restAPIStore.createSubscription(createResponse.getData(),
                    applicationList.get(1), "Unlimited");
        }
    }

    @Test(groups = {"webapp"}, description = "List all Subscriptions By Application Id")
    public void testGetAllSubscriptionsByAppId() throws Exception {

        SubscriptionListDTO subscriptionListDTO, subscriptionList1DTO;
        subscriptionListDTO = restAPIStore.getSubscription(null, applicationList.get(0),
                null, null);
        int listCount1 = 0, listCount2 = 0;
        for (SubscriptionDTO subscription : subscriptionListDTO.getList()) {
            for (String apiId : apiList1) {
                if (apiId.equalsIgnoreCase(subscription.getApiId())) {
                    listCount1++;
                    continue;
                }
            }
        }
        assertEquals(listcount, listCount1, "Application subscription count mismatch");

        subscriptionList1DTO = restAPIStore.getSubscription(null, applicationList.get(1),
                null, null);
        for (SubscriptionDTO subscription : subscriptionList1DTO.getList()) {
            for (String apiId : apiList2) {
                if (apiId.equalsIgnoreCase(subscription.getApiId())) {
                    listCount2++;
                    continue;
                }
            }
        }
        assertEquals(listcount, listCount2, "Application subscription count mismatch");
    }

    @Test(groups = {"webapp"}, description = "List all Subscriptions By API Id")
    public void testGetAllSubscriptionsByAPIId() throws Exception {
        for (String apiId : apiList1) {
            restAPIStore.createSubscription(apiId,
                    applicationList.get(1), "Unlimited");
        }
        for (String apiId : apiList1) {
            SubscriptionListDTO subscriptionListDTO = restAPIStore.getSubscription(apiId, null,
                    null, null);
            assertEquals(2, subscriptionListDTO.getCount().intValue(),
                    "API subscription count mismatch" + apiId);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        for (String appId : applicationList) {
            restAPIStore.deleteApplication(appId);
        }
        for (String api : apiList1) {
            restAPIPublisher.deleteAPI(api);
        }
        for (String api : apiList2) {
            restAPIPublisher.deleteAPI(api);
        }

    }

}