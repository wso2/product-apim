/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.scenario.tests.delete.existing.api;

import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
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
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DeleteAPINegativeTestCases extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(DeleteExistingAPIsTestCases.class);
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";
    private static final String API_NAME_PREFIX = "DeleteAPINeg_";
    private static final String API_VERSION = "1.0.0";
    private static String apiID;


    @Factory(dataProvider = "userModeDataProvider")
    public DeleteAPINegativeTestCases(TestUserMode userMode) {
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

    @Test(description = "1.4.1.4", dataProvider = "DeleteAPIAfterSubscribingDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testDeleteAPIWithSubscription(APILifeCycleState state) throws Exception {
        String name = API_NAME_PREFIX + "subAppNeg_" + state.toString();
        apiID = createApi(name);
        String applicationID = createApplication(name);
        changeApiStateTo(apiID, state);

        if (state.equals(APILifeCycleState.PUBLISHED)) {
            createSubscription(apiID, applicationID);
        }
        checkDeleteAPI(apiID);
        try {
            restAPIPublisher.deleteAPI(apiID);
        } catch (ApiException e) {
            if (state.equals(APILifeCycleState.PUBLISHED)) {
                assertTrue(e.getResponseBody().contains("Cannot remove the API " + apiID + " as active subscriptions exist"));
            }
            if (state.equals(APILifeCycleState.BLOCKED)) {
                assertTrue(e.getResponseBody().contains("Requested API with Id '" + apiID + "' not found"));
            }
            if (state.equals(APILifeCycleState.DEPRECATED)) {
                assertTrue(e.getResponseBody().contains("Requested API with Id '" + apiID + "' not found"));
            }
        }
        verifyAPIAvailableInPublisher(apiID);
//        check availability in store for only PUBLISHED apis
        if (state.equals(APILifeCycleState.PUBLISHED)) {
            verifyAPIAvailableInStore(apiID);
        }
        restAPIStore.deleteApplication(applicationID);
        if (state.equals(APILifeCycleState.PUBLISHED)) {
            restAPIPublisher.deleteAPI(apiID);
        }
    }

    @Test(description = "1.4.1.5")
    public void testDeleteAPIByUnauthorizedUser() throws Exception {
        String name = API_NAME_PREFIX + "subAppNeg_" + APILifeCycleState.PUBLISHED.toString();
        apiID = createApi(name);
        restAPIPublisher = new RestAPIPublisherImpl(
                publisherContext.getContextTenant().getTenantUserList().get(1).getUserNameWithoutDomain(),
                publisherContext.getContextTenant().getTenantUserList().get(1).getPassword(),
                publisherContext.getContextTenant().getDomain(), publisherURLHttps);

        try {
            restAPIPublisher.deleteAPI(apiID);
        } catch (ApiException e) {
            assertTrue(e.getResponseBody().contains("Unauthenticated request"));
        }
        restAPIPublisher = new RestAPIPublisherImpl(
                publisherContext.getContextTenant().getTenantUserList().get(0).getUserNameWithoutDomain(),
                publisherContext.getContextTenant().getTenantUserList().get(0).getPassword(),
                publisherContext.getContextTenant().getDomain(), publisherURLHttps);
        restAPIPublisher.deleteAPI(apiID);
    }

    @Test(description = "1.4.1.6")
    public void testDeleteNonExistingAPI() throws Exception {
        checkDeleteAPI(apiID);
    }


    private String createApi(String apiName) throws Exception {
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, "/menu", API_VERSION,
                "admin", new URL("https://localhost:9443/am/sample/pizzashack/v1/api/"));

        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);
        assertTrue(StringUtils.isNotEmpty(apiDto.getId()), "Error occurred when creating api");
        return apiDto.getId();
    }

    private void changeApiStateTo(String apiid, APILifeCycleState state) throws Exception {

        switch (state) {
            case PROTOTYPED:
            case PUBLISHED:
                changeApiState(apiid, APILifeCycleState.PUBLISHED);
                break;
            case BLOCKED:
                changeApiState(apiid, APILifeCycleState.PUBLISHED);
                changeApiState(apiid, APILifeCycleState.BLOCKED);
                break;
            case DEPRECATED:
                changeApiState(apiid, APILifeCycleState.PUBLISHED);
                changeApiState(apiid, APILifeCycleState.DEPRECATED);
                break;
            case RETIRED:
                changeApiState(apiid, APILifeCycleState.PUBLISHED);
                changeApiState(apiid, APILifeCycleState.DEPRECATED);
                changeApiState(apiid, APILifeCycleState.RETIRED);
                break;
            case CREATED:
            default:
                break;
        }
    }

    private void changeApiState(String apiId, APILifeCycleState state) throws Exception {

        switch (state) {
            case PROTOTYPED:
                restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.DEPLOY_AS_PROTOTYPE.getAction(), null);
                break;
            case PUBLISHED:
                restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);
                break;
            case BLOCKED:
                restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.BLOCK.getAction(), null);
                break;
            case DEPRECATED:
                restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.DEPRECATE.getAction(), null);
                break;
            case RETIRED:
                restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.RETIRE.getAction(), null);
                break;
            case CREATED:
                break;
        }
    }

    private void checkDeleteAPI(String apiId) throws Exception {
        HttpResponse response = null;
        try {
            response = restAPIPublisher.deleteAPI(apiId);
        } catch (Exception e) {
            assertEquals(response, null);
        }
    }

    private void verifyAPIAvailableInPublisher(String apiId) throws Exception {
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        assertTrue(response.getData().contains(apiId), "API has been delete : " + apiId);
    }

    private HttpResponse verifyAPIAvailableInStore(String apiID) throws Exception {
        HttpResponse response = null;
        Gson gson = new Gson();
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apidto = restAPIStore.getAPI(apiID);
        if (StringUtils.isNotEmpty(apidto.getId())) {
            response = new HttpResponse(gson.toJson(apidto), 200);
        }
        return response;

    }

    private String createApplication(String applicationName) {
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        return applicationResponse.getData();
    }

    private HttpResponse createSubscription(String apiID, String applicationId) throws Exception {
        return restAPIStore.createSubscription(apiID, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);
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
