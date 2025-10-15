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

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;


/**
 * This class checks whether the API subscriptions are retained when an API is demote to the CREATED state of
 * the API Lifecycle
 */
public class APIMANAGER5337SubscriptionRetainTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(APIMANAGER5337SubscriptionRetainTestCase.class);
    private String apiId;
    private String applicationID;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
    }

    @Test(groups = "wso2.am", description = "testing error responses")
    public void testAPIErrorResponse() throws Exception {

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
            HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
            //verifyResponse(apiResponse);

            apiId = apiResponse.getData();

            // Create Revision and Deploy to Gateway
            createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);

            //Publish the API
            restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);

            HttpResponse applicationResponse = restAPIStore.createApplication("subscriptionCheckApp1",
                    "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                    ApplicationDTO.TokenTypeEnum.JWT);
            //verifyResponse(applicationResponse);

            applicationID = applicationResponse.getData();

            //Subscribe the API to the Application
            response = restAPIStore.createSubscription(apiId, applicationID, APIMIntegrationConstants.API_TIER.UNLIMITED);
            //verifyResponse(response);

            //Demote the API to the Created State
            restAPIPublisher.changeAPILifeCycleStatus(apiId, "Demote to Created", null);

            Thread.sleep(1000);

//            //Check For subscriptions
            SubscriptionListDTO subsDTO = restAPIStore.getAllSubscriptionsOfApplication(applicationID);
//            verifyResponse(response);

            Gson g = new Gson();
            String subscriptionJsonString = g.toJson(subsDTO, SubscriptionListDTO.class);
            Assert.assertEquals(subscriptionJsonString.contains("SubscriptionCheckAPI"), true,
                    "Subscription of the SubscriptionCheckAPI has been removed.");

        } catch (APIManagerIntegrationTestException e) {
            log.error("APIManagerIntegrationTestException " + e.getMessage(), e);
            Assert.assertTrue(false);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationID);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }
}
