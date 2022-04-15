package org.wso2.am.integration.tests.publisher;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class SubscriptionBlockingFromPublisherTestCase extends APIManagerLifecycleBaseTest {


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Factory(dataProvider = "userModeDataProvider")
    public SubscriptionBlockingFromPublisherTestCase(TestUserMode userMode) {
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

    @Test(groups = "wso2.am", description = "Block all subscription for apps that has dashes in app name")
    public void testAPISubscriptionAfterDemotingToCreated() throws Exception {

        String apiName = "SubscriptionCheckAPI";
        String apiVersion = "1.0.0";
        String apiContext = "subscriptionCheck";
        String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
        String endpointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;

        //Create the api creation request object
        APIRequest apiRequest;
        apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));
        apiRequest.setProvider(user.getUserName());

        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        //Add the API using the API publisher.
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        String apiId = apiResponse.getData();

        //Publish the API
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);

        ApplicationDTO applicationDTO = restAPIStore.addApplication("name-with-dash-app",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");
        String applicationId = applicationDTO.getApplicationId();

        //Subscribe the API to the Application
        restAPIStore.subscribeToAPI(apiId, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        //Check For subscriptions and if API invocation fails
        SubscriptionListDTO subsDTO = restAPIStore.getAllSubscriptionsOfApplication(applicationId);
        String subscriptionId = subsDTO.getList().get(0).getSubscriptionId();
        JSONObject subscriptionJson = new JSONObject(subsDTO);
        Assert.assertTrue(subscriptionJson.toString().contains("SubscriptionCheckAPI"),
                "Subscription of the SubscriptionCheckAPI has been removed.");

        restAPIPublisher.blockSubscription(subscriptionId, "PROD_ONLY_BLOCKED", null);

        SubscriptionListDTO subsDTO2 = restAPIStore.getAllSubscriptionsOfApplication(applicationId);
        JSONObject subscriptionJson2 = new JSONObject(subsDTO2);
        Assert.assertTrue(subscriptionJson2.toString().contains("PROD_ONLY_BLOCKED"),
                "Subscription of the SubscriptionCheckAPI block fails.");
    }
}
