package org.wso2.am.integration.tests.api.lifecycle;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;

import java.io.File;
import java.net.URL;

/**
 * This class checks whether the API subscriptions are retained when an API is demote to the CREATED state of
 * the API Lifecycle
 */
public class APIMANAGER5337SubscriptionRetainTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(APIMANAGER5337SubscriptionRetainTestCase.class);

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        String gatewaySessionCookie = createSession(gatewayContextMgt);
        //Initialize publisher and store.
        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());

        //Load the back-end dummy API
        loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM"
                + File.separator + "synapseconfigs" + File.separator + "rest"
                + File.separator + "error_response_check_dummy_api.xml", gatewayContextMgt, gatewaySessionCookie);
    }
    @Test(groups = "wso2.am", description = "testing error responses")
    public void testAPIErrorResponse() throws Exception {

        //Login to the API Publisher
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse response;
        response = apiPublisher.login(user.getUserName(), user.getPassword());
        verifyResponse(response);

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
            response = apiPublisher.addAPI(apiRequest);
            verifyResponse(response);

            APILifeCycleStateRequest updateRequest1 = new APILifeCycleStateRequest(apiName,
                    user.getUserName(), APILifeCycleState.PUBLISHED);
            //Publish the API
            response = apiPublisher.changeAPILifeCycleStatus(updateRequest1);
            verifyResponse(response);

            //Login to the API Store
            response = apiStore.login(user.getUserName(), user.getPassword());
            verifyResponse(response);

            //Add an Application in the Store.
            response = apiStore.addApplication("subscriptionCheckApp1", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
            verifyResponse(response);

            //Subscribe the API to the Application
            SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, apiVersion,
                    user.getUserName(), "subscriptionCheckApp1",
                    APIMIntegrationConstants.API_TIER.UNLIMITED);
            response = apiStore.subscribe(subscriptionRequest);
            verifyResponse(response);

            Thread.sleep(1000);

            //Demote the API to the Created State
            APILifeCycleStateRequest updateRequest2 = new APILifeCycleStateRequest(apiName,
                    user.getUserName(), APILifeCycleState.CREATED);
            response = apiPublisher.changeAPILifeCycleStatus(updateRequest2);
            verifyResponse(response);

            Thread.sleep(1000);

            //Check For subscriptions
            response = apiStore.getAllSubscriptionsOfApplication("subscriptionCheckApp1");
            verifyResponse(response);

            String subscriptions = response.getData();
            JSONObject subscriptionJson = new JSONObject(subscriptions);

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
