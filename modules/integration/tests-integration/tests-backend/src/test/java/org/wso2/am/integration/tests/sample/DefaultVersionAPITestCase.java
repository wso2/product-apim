/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.tests.sample;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * This class is used to check the functionality of the Default Version API.
 * <p>
 * Note: By default an API will always have a version. Ex: 1.0.0. It is mandatory that the API request url contains this
 * version field. Ex: http://localhost:8280/twitter/1.0.0. By specifying a particular version of an API as the 'default'
 * versioned API, it makes it possible to invoke that particular api without having the version as part of the request
 * url. Ex: http://localhost:8280/twitter
 * </p>
 */
public class DefaultVersionAPITestCase extends AMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;

    private APIStoreRestClient apiStore;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();

        //Initialize publisher and store.
        apiPublisher = new APIPublisherRestClient(getPublisherServerURLHttp());
        apiStore = new APIStoreRestClient(getStoreServerURLHttp());

        //Load the back-end dummy API
        loadAPIMConfigurationFromClasspath("artifacts" + File.separator + "AM"
                + File.separator + "synapseconfigs" + File.separator + "rest"
                + File.separator + "dummy_api.xml");
    }

    @Test(groups = "wso2.am", description = "Check functionality of the default version API")
    public void testDefaultVersionAPI() throws Exception {

        //Login to the API Publisher
        apiPublisher.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());

        String apiName = "DefaultVersionAPI";
        String apiVersion = "1.0.0";
        String apiContext = "/defaultversion";
        String endpointUrl = getGatewayServerURLHttp() + "/response";

        //Create the api creation request object
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));
        apiRequest.setDefault_version("default_version");
        apiRequest.setDefault_version_checked("default_version");
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection("Unlimited");
        apiRequest.setTier("Unlimited");

        //Add the API using the API publisher.
        apiPublisher.addAPI(apiRequest);

        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName,
                apimContext.getContextTenant().getContextUser().getUserName(),
                APILifeCycleState.PUBLISHED);
        //Publish the API
        apiPublisher.changeAPILifeCycleStatusTo(updateRequest);

        //Login to the API Store
        apiStore.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());

        //Add an Application in the Store.
        apiStore.addApplication("DefaultVersionAPP", "Unlimited", "", "");

        //Subscribe the API to the DefaultApplication
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, apiVersion,
                apimContext.getContextTenant().getContextUser().getUserName(),
                "DefaultVersionAPP", "Unlimited");
        apiStore.subscribe(subscriptionRequest);

        //Generate production token and invoke with that
        GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest("DefaultVersionAPP");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);

        //Get the accessToken which was generated.
        String accessToken = response.getJSONObject("data").getJSONObject("key").getString("accessToken");

        String directBackEndEndpoint = getGatewayServerURLHttp() + "/response";

        //Going to access the API without the version in the request url.
        String apiInvocationUrl = getGatewayServerURLHttp() + apiContext;

        HttpResponse directResponse = HttpRequestUtil.doGet(directBackEndEndpoint, null);

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);
        //Invoke the API
        HttpResponse httpResponse = HttpRequestUtil.doGet(apiInvocationUrl, headers);

        //Check if accessing the back-end directly and accessing it via the API yield the same responses.
        assertEquals(httpResponse.getData(), directResponse.getData(), "Default version API test failed while " +
                "invoking the API.");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanup();
    }
}