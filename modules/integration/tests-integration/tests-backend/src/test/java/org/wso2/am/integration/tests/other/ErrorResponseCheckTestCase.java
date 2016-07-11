/*
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Test to check some security issues in Error responses
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL })
public class ErrorResponseCheckTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(ErrorResponseCheckTestCase.class);

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

        String apiName = "ErrorResponseSecAPI";
        String apiVersion = "1.0.0";
        String apiContext = "sec";
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

            APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName,
                    user.getUserName(), APILifeCycleState.PUBLISHED);
            //Publish the API
            response = apiPublisher.changeAPILifeCycleStatus(updateRequest);
            verifyResponse(response);

            //Login to the API Store
            response = apiStore.login(user.getUserName(), user.getPassword());
            verifyResponse(response);

            //Add an Application in the Store.
            response = apiStore.addApplication("SecApp", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
            verifyResponse(response);

            //Subscribe the API to the DefaultApplication
            SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, apiVersion,
                    user.getUserName(), "SecApp",
                    APIMIntegrationConstants.API_TIER.UNLIMITED);
            response = apiStore.subscribe(subscriptionRequest);
            verifyResponse(response);

            //Generate production token and invoke with that
            APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator("SecApp");
            String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
            JSONObject responseJson = new JSONObject(responseString);

            //Get the accessToken which was generated.
            String accessToken = responseJson.getJSONObject("data").getJSONObject("key").getString("accessToken");

            //Going to access the API with the version in the request url.
            String apiInvocationUrl = getAPIInvocationURLHttp(apiContext, apiVersion);

            HttpClient httpclient = new DefaultHttpClient();
            HttpUriRequest getRequest1 = new HttpGet(apiInvocationUrl);
            getRequest1.addHeader(new BasicHeader("Authorization", "Bearer " + accessToken));

            waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                    APIMIntegrationConstants.IS_API_EXISTS);

            org.apache.http.HttpResponse httpResponse = httpclient.execute(getRequest1);
            Assert.assertEquals(httpResponse.getStatusLine().getStatusCode(), 201, "Response Code Mismatched");


        /* -----------------test 1 : invoke with invalid resource path wit invalid context --------------- */
            String invalidApiInvocationUrl = getAPIInvocationURLHttp("invalidContext", apiVersion);

            HttpUriRequest getRequest2 = new HttpGet(invalidApiInvocationUrl);
            getRequest2.addHeader(new BasicHeader("Authorization", "Bearer " + accessToken));

            waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                    APIMIntegrationConstants.IS_API_EXISTS);

            //releasing the connection
            if( httpResponse.getEntity() != null ) {
                httpResponse.getEntity().consumeContent();
            }

            org.apache.http.HttpResponse httpResponse2 = httpclient.execute(getRequest2);
            Assert.assertEquals(httpResponse2.getStatusLine().getStatusCode(), 404, "Response Code Mismatched");
            Assert.assertEquals(httpResponse2.toString().contains("invalidContext/1.0.0"),
                    false, "The message contains the resource path requested.");

        /* ----------------------------test 2 : invoke with invalid access token ---------------------------- */

            HttpUriRequest getRequest3 = new HttpGet(apiInvocationUrl);
            getRequest3.addHeader(new BasicHeader("Authorization", "Bearer " + "invalidAccessToken"));

            waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                    APIMIntegrationConstants.IS_API_EXISTS);

            //releasing the connection
            if( httpResponse2.getEntity() != null ) {
                httpResponse2.getEntity().consumeContent();
            }

            org.apache.http.HttpResponse httpResponse3 = httpclient.execute(getRequest3);
            Assert.assertEquals(httpResponse3.getStatusLine().getStatusCode(), 401, "Response Code Mismatched");
            Assert.assertEquals(httpResponse3.toString().contains("invalid_access_token"),
                    false, "Access token entered is valid");


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
        super.cleanUp();
    }
}
