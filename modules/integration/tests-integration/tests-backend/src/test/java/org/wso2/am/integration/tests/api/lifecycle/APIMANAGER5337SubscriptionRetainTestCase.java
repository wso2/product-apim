/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.apimgt.test.Constants;
import org.wso2.carbon.apimgt.test.impl.RestAPIPublisherImpl;
import org.wso2.carbon.apimgt.test.impl.RestAPIStoreImpl;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.net.URL;

import static org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO.StatusEnum;

/**
 * This class checks whether the API subscriptions are retained when an API is demote to the CREATED state of
 * the API Lifecycle
 */
public class APIMANAGER5337SubscriptionRetainTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(APIMANAGER5337SubscriptionRetainTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        String gatewaySessionCookie = createSession(gatewayContextMgt);
        //Load the back-end dummy API
        loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM"
                + File.separator + "synapseconfigs" + File.separator + "rest"
                + File.separator + "error_response_check_dummy_api.xml", gatewayContextMgt, gatewaySessionCookie);
    }

    @Test(groups = "wso2.am", description = "testing error responses")
    public void testAPIErrorResponse() throws Exception {

        //Login to the API Publisher
        HttpResponse response;
        String apiName = "SubscriptionCheckAPI";
        String apiVersion = "1.0.0";
        String apiContext = "subscriptionCheck";
        String endpointUrl = getAPIInvocationURLHttp("response");

        try {
            //Create the api creation request object
            APIRequest apiRequest;
            apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));

            apiRequest.setVersion(apiVersion);
            apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

            //Add the API using the API publisher.
            RestAPIPublisherImpl restAPIPublisher = new RestAPIPublisherImpl();
            HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
            //verifyResponse(apiResponse);

            String apiId = apiResponse.getData();

            //Publish the API
            restAPIPublisher.changeAPILifeCycleStatus(apiId, Constants.PUBLISHED);

            //Add an Application in the Store.
            RestAPIStoreImpl restAPIStore = new RestAPIStoreImpl();

            HttpResponse applicationResponse = restAPIStore.createApplication("subscriptionCheckApp1",
                    "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                    ApplicationDTO.TokenTypeEnum.OAUTH);
            //verifyResponse(applicationResponse);

            String applicationID = applicationResponse.getData();

            //Subscribe the API to the Application
            response = restAPIStore.createSubscription(apiId, applicationID, APIMIntegrationConstants.API_TIER.UNLIMITED,
                    StatusEnum.UNBLOCKED, SubscriptionDTO.TypeEnum.API);
            //verifyResponse(response);

            //Demote the API to the Created State
            restAPIPublisher.changeAPILifeCycleStatus(apiId, "Demote to Created");

            Thread.sleep(1000);

//            //Check For subscriptions
            SubscriptionListDTO subsDTO = restAPIStore.getAllSubscriptionsOfApplication(applicationID);
//            verifyResponse(response);


            JSONObject subscriptionJson = new JSONObject(subsDTO);
            Assert.assertEquals(subscriptionJson.toString().contains("SubscriptionCheckAPI"), true,
                    "Subscription of the SubscriptionCheckAPI has been removed.");

        } catch (APIManagerIntegrationTestException e) {
            log.error("APIManagerIntegrationTestException " + e.getMessage(), e);
            Assert.assertTrue(false);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }
}
