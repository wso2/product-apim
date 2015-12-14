/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;

import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import java.io.File;
import java.net.URL;


/**
 * Test to check the Http 201 response when location header is a relative URL
 */
public class RelativeUrlLocationHeaderTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(RelativeUrlLocationHeaderTestCase.class);

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
                + File.separator + "dummy_api_relative_url_loc_header.xml", gatewayContextMgt, gatewaySessionCookie);
    }

    @Test(groups = "wso2.am", description = "Check functionality of the API for relative URL location header")
    public void testAPIWithRelativeUrlLocationHeader() throws Exception {

        //Login to the API Publisher
        HttpResponse response;
        response = apiPublisher.login(user.getUserName(), user.getPassword());
        verifyResponse(response);

        String apiName = "RelativeUrlLocationHeaderAPI";
        String apiVersion = "1.0.0";
        String apiContext = "relative";
        String endpointUrl = getAPIInvocationURLHttp("response") + "/1.0.0";

        String appName = "RelativeLocHeaderAPP";

        //Create the api creation request object
        APIRequest apiRequest;
        apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));

        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        //Add the API using the API publisher.
        response = apiPublisher.addAPI(apiRequest);
        verifyResponse(response);

        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, user.getUserName(),
                APILifeCycleState.PUBLISHED);

        //Publish the API
        response = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(response);

        //Login to the API Store
        response = apiStore.login(user.getUserName(), user.getPassword());
        verifyResponse(response);

        //Add an Application in the Store.
        response = apiStore.addApplication(appName, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
        verifyResponse(response);

        //Subscribe the API to the DefaultApplication
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, apiVersion, user.getUserName()
                , appName, APIMIntegrationConstants.API_TIER.UNLIMITED);
        response = apiStore.subscribe(subscriptionRequest);
        verifyResponse(response);

        //Generate production token and invoke with that
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(appName);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject responseProduction = new JSONObject(responseString);

        //Get the accessToken which was generated.
        String accessToken = responseProduction.getJSONObject("data").getJSONObject("key").getString("accessToken");

        Assert.assertEquals(accessToken.isEmpty(), false, "Production access token is Empty");

        //Going to access the API with the version in the request url.
        String apiInvocationUrl = getAPIInvocationURLHttp(apiContext, apiVersion);

        HttpClient httpclient = new DefaultHttpClient();
        HttpUriRequest get = new HttpGet(apiInvocationUrl);
        get.addHeader(new BasicHeader("Authorization", "Bearer " + accessToken));

        org.apache.http.HttpResponse httpResponse = httpclient.execute(get);

        Assert.assertEquals(httpResponse.getStatusLine().getStatusCode(), 201, "Response Code Mismatched");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }

}