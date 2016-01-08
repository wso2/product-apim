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

package org.wso2.am.integration.tests.other;

import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * This class is used to check the functionality of an API created with Digest Authentication as
 * its authentication mechanism.
 */

public class DigestAuthenticationTestCase extends APIMIntegrationBaseTest {

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
        loadSynapseConfigurationFromClasspath(
                "artifacts" + File.separator + "AM" + File.separator + "synapseconfigs" + File.separator + "rest"
                        + File.separator + "dummy_digest_api.xml", gatewayContextMgt, gatewaySessionCookie);
    }

    @Test(groups = "wso2.am", description = "Check functionality of the digest authenticated API")
    public void testDigestAuthentication() throws Exception {
        //Login to the API Publisher
        apiPublisher.login(user.getUserName(), user.getPassword());

        String apiName = "DigestAuthAPI";
        String apiVersion = "1.0.0";
        String apiContext = "digest";
        String providerName = user.getUserName();
        String endpointUrl = getGatewayURLNhttp() + "digestAuth";

        //Create API request bean object
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion,
                providerName, new URL(endpointUrl));
        apiCreationRequestBean.setEndpointType("secured");
        apiCreationRequestBean.setEndpointAuthType("digestAuth");
        apiCreationRequestBean.setEpUsername("DigestAuth");
        apiCreationRequestBean.setEpPassword("digest123");
        apiCreationRequestBean.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiCreationRequestBean.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiCreationRequestBean.setProvider(providerName);

        //Add the API to the publisher
        apiPublisher.addAPI(apiCreationRequestBean);

        //Publish the API
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, providerName,
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        //Login to the API Store
        apiStore.login(user.getUserName(), user.getPassword());

        //Add an Application in the Store.
        apiStore.addApplication("DigestAuthAPP", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "",
                "Test-Digest-Auth");

        //Subscribe to the new application
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, apiVersion, providerName,
                "DigestAuthAPP",  APIMIntegrationConstants.API_TIER.UNLIMITED);

        waitForAPIDeploymentSync(providerName, apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);

        apiStore.subscribe(subscriptionRequest);

        //Generate a production token and invoke the API
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator("DigestAuthAPP");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject jsonResponse = new JSONObject(responseString);

        //Get the accessToken generated.
        String accessToken = jsonResponse.getJSONObject("data").getJSONObject("key").getString("accessToken");

        String apiInvocationUrl = getAPIInvocationURLHttp(apiContext, apiVersion);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/xml");
        headers.put("Authorization", "Bearer " + accessToken);

        //Invoke the API
        HttpResponse httpResponseGet = HttpRequestUtil.doGet(apiInvocationUrl, headers);
        //Assertion
        assertEquals(httpResponseGet.getResponseCode(), Response.Status.OK.getStatusCode(),
                "The response code is not 200 OK");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }
}