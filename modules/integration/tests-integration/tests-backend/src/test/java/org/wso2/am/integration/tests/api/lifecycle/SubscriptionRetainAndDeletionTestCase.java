/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.am.integration.tests.api.lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.store.api.v1.dto.*;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;


/**
 * This class checks whether the API subscriptions are retained when an API is demote to the CREATED state of
 * the API Lifecycle
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class SubscriptionRetainAndDeletionTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(SubscriptionRetainAndDeletionTestCase.class);
    private static final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiId;
    private String applicationId;
    private String subscriptionId;
    private Map<String, String> requestHeadersSandBox = new HashMap<String, String>();
    private String sandboxAccessToken;
    private String gatewayUrl;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Factory(dataProvider = "userModeDataProvider")
    public SubscriptionRetainAndDeletionTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.SUPER_TENANT_USER_STORE_USER},
                new Object[]{TestUserMode.SUPER_TENANT_EMAIL_USER},
                new Object[]{TestUserMode.TENANT_EMAIL_USER},
        };
    }

    @Test(groups = "wso2.am", description = "testing subscription availability and API invocation when demoted to"
            + "CREATED state from PUBLISHED state.")
    public void testAPISubscriptionAfterDemotingToCreated() throws Exception {

        String apiName = "SubscriptionCheckAPI";
        String apiVersion = "1.0.0";
        String apiContext = "subscriptionCheck";
        String endpointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;

        try {
            //Create the api creation request object
            APIRequest apiRequest;
            apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));
            apiRequest.setProvider(user.getUserName());

            apiRequest.setVersion(apiVersion);
            apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

            //Add the API using the API publisher.
            HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);

            apiId = apiResponse.getData();

            //Publish the API
            restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);
            gatewayUrl = getAPIInvocationURLHttp("subscriptionCheck/1.0.0/customers/123");

            ApplicationDTO applicationDTO = restAPIStore.addApplication("subscriptionCheckApp",
                    APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");
            applicationId = applicationDTO.getApplicationId();

            //Subscribe the API to the Application
            SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationId,
                    APIMIntegrationConstants.API_TIER.UNLIMITED);

            //Generate sandbox Token
            ArrayList<String> grantTypes = new ArrayList<>();
            grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
            ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(),
                    "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, null,
                    grantTypes);
            sandboxAccessToken = applicationKeyDTO.getToken().getAccessToken();
            requestHeadersSandBox.put("Authorization", "Bearer " + sandboxAccessToken);
            requestHeadersSandBox.put("accept", "text/xml");

            //Demote the API to the Created State
            restAPIPublisher.changeAPILifeCycleStatus(apiId, "Demote to Created", null);
            Thread.sleep(1000);

            //Check For subscriptions and if API invocation fails
            SubscriptionListDTO subsDTO = restAPIStore.getAllSubscriptionsOfApplication(applicationId);
            subscriptionId = subsDTO.getList().get(0).getSubscriptionId();
            JSONObject subscriptionJson = new JSONObject(subsDTO);
            Assert.assertTrue(subscriptionJson.toString().contains("SubscriptionCheckAPI"),
                    "Subscription of the SubscriptionCheckAPI has been removed.");

            HttpResponse responseSandBox = HttpRequestUtil.doGet(gatewayUrl, requestHeadersSandBox);
            log.info("Response " + responseSandBox);
            assertEquals(responseSandBox.getResponseCode(), 404, "Response code mismatched");

            //Promote the API to the Published State
            restAPIPublisher.changeAPILifeCycleStatus(apiId, "Publish", null);
            Thread.sleep(1000);

            //Check if API can still be invoked with an old token
            responseSandBox = HttpRequestUtil.doGet(gatewayUrl, requestHeadersSandBox);
            log.info("Response " + responseSandBox);
            assertEquals(responseSandBox.getResponseCode(), 200, "Response code mismatched");
        } catch (APIManagerIntegrationTestException e) {
            log.error("APIManagerIntegrationTestException " + e.getMessage(), e);
            Assert.fail();
        }
    }

    @Test(groups = "wso2.am", description = "testing subscription deletion when demoted to CREATED state from "
            + "PUBLISHED state.", dependsOnMethods = {"testAPISubscriptionAfterDemotingToCreated"})
    public void testAPISubscriptionDeletionAfterDemotingToCreated() throws Exception {
        //Demote the API to the Created State
        restAPIPublisher.changeAPILifeCycleStatus(apiId, "Demote to Created", null);
        Thread.sleep(1000);

        //Remove API subscription
        restAPIStore.removeSubscription(subscriptionId);

        //Promote the API to the Published State
        restAPIPublisher.changeAPILifeCycleStatus(apiId, "Publish", null);
        Thread.sleep(1000);

        //Check if API can still be invoked with token from old subscription
        HttpResponse responseSandBox = HttpRequestUtil.doGet(gatewayUrl, requestHeadersSandBox);
        log.info("Response " + responseSandBox);
        assertEquals(responseSandBox.getResponseCode(), 403, "Response code mismatched");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPIByID(apiId);
    }
}
