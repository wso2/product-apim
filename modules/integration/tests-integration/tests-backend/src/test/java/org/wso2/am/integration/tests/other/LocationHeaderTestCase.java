/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
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
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class LocationHeaderTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(LocationHeaderTestCase.class);

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        String gatewaySessionCookie = createSession(gatewayContext);
        //Initialize publisher and store.
        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());

        //Load the back-end dummy API
        loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM"
                + File.separator + "synapseconfigs" + File.separator + "rest"
                + File.separator + "dummy_api_loc_header.xml", gatewayContext, gatewaySessionCookie);
    }

    @Test(groups = "wso2.am", description = "Check whether the Location header is correct")
    public void testAPIWithLocationHeader()  {

        //Login to the API Publisher
        try {
            apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                    publisherContext.getContextTenant().getContextUser().getPassword());
        } catch (APIManagerIntegrationTestException e) {
            log.error("APIManagerIntegrationTestException " + e.getMessage());
            Assert.assertTrue(false);
        } catch (XPathExpressionException e) {
            log.error("XPathExpressionException " + e.getMessage());
            Assert.assertTrue(false);
        }

        String apiName = "LocationHeaderAPI";
        String apiVersion = "1.0.0";
        String apiContext = "/locheader";
        String endpointUrl = gatewayUrls.getWebAppURLNhttp() + "response";

        //Create the api creation request object
        APIRequest apiRequest = null;
        try {
            apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));
        } catch (APIManagerIntegrationTestException e) {
            log.error("Error creating APIRequest " + e.getMessage());
            Assert.assertTrue(false);
        } catch (MalformedURLException e) {
            log.error("Invalid URL " + gatewayUrls.getWebAppURLNhttp() + "response", e);
            Assert.assertTrue(false);
        }

        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection("Unlimited");
        apiRequest.setTier("Unlimited");

        //Add the API using the API publisher.
        try {
            apiPublisher.addAPI(apiRequest);

            APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName,
                    publisherContext.getContextTenant().getContextUser().getUserName(),
                    APILifeCycleState.PUBLISHED);
            //Publish the API
            apiPublisher.changeAPILifeCycleStatus(updateRequest);

            //Login to the API Store
            apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                    storeContext.getContextTenant().getContextUser().getPassword());

            //Add an Application in the Store.
            apiStore.addApplication("LocHeaderAPP", "Unlimited", "", "");

            //Subscribe the API to the DefaultApplication
            SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, apiVersion,
                    storeContext.getContextTenant().getContextUser().getUserName(),
                    "LocHeaderAPP", "Unlimited");
            apiStore.subscribe(subscriptionRequest);

            //Generate production token and invoke with that
            APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator("LocHeaderAPP");
            String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
            JSONObject response = new JSONObject(responseString);

            //Get the accessToken which was generated.
            String accessToken = response.getJSONObject("data").getJSONObject("key").getString("accessToken");

            //Going to access the API without the version in the request url.
            String apiInvocationUrl = gatewayUrls.getWebAppURLNhttp() + apiContext + "/" + apiVersion;

            HttpClient httpclient = new DefaultHttpClient();
            HttpUriRequest get = new HttpGet(apiInvocationUrl);
            get.addHeader(new BasicHeader("Authorization", "Bearer " + accessToken));

            org.apache.http.HttpResponse httpResponse = httpclient.execute(get);
            Header locationHeader  = httpResponse.getFirstHeader("Location");

            Assert.assertEquals(!locationHeader.getValue().endsWith("//abc/domain"), true,
                                                                    "Location header contains additional / character");
            Assert.assertEquals(locationHeader.getValue().endsWith("/abc/domain"), true,
                    "Unexpected Location header. Expected to end with "
                            + "/abc/domain but received " + locationHeader);

            // Test 201 response with location header
            HttpUriRequest createdRequest = new HttpGet(apiInvocationUrl + "?" + "response" + "=" + 201);
            createdRequest.addHeader(new BasicHeader("Authorization", "Bearer " + accessToken));

            httpclient = new DefaultHttpClient();
            httpResponse = httpclient.execute(createdRequest);
            locationHeader  = httpResponse.getFirstHeader("Location");

            Assert.assertEquals(locationHeader.getValue().equalsIgnoreCase("http://google.lk/abc/domain"), true,
                    "Location header has been modified for " + 201 + " response");

        } catch (APIManagerIntegrationTestException e) {
            log.error("APIManagerIntegrationTestException " + e.getMessage(), e);
            Assert.assertTrue(false);
        } catch (JSONException e) {
            log.error("Error parsing JSON to get access token " + e.getMessage(), e);
            Assert.assertTrue(false);
        } catch (XPathExpressionException e) {
            log.error("XPathExpressionException " + e.getMessage(), e);
            Assert.assertTrue(false);
        } catch (IOException e) {
            log.error("IOException " + e.getMessage(), e);
            Assert.assertTrue(false);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanup();
    }
}
