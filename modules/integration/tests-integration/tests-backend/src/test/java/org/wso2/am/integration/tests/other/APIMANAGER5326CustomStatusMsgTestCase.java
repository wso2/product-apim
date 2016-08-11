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
import org.wso2.am.integration.tests.header.util.SimpleSocketServer;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Test to check the custom status message response is not dropped in the response for 400
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL })
public class APIMANAGER5326CustomStatusMsgTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(APIMANAGER5326CustomStatusMsgTestCase.class);
    public static final int PORT = 1989;

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private SimpleSocketServer simpleSocketServer;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        //String gatewaySessionCookie = createSession(gatewayContextMgt);
        //Initialize publisher and store.
        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());

        String expectedResponse = "HTTP/1.1 400 Custom response\r\nServer: testServer\r\n" +
                "Content-Type: text/xml; charset=UTF-8\r\n Transfer-Encoding: chunked\r\n\r\n"
                + "\"<?xml version=\"1.0\" encoding=\"UTF-8\"?><test></test>";

        simpleSocketServer = new SimpleSocketServer(PORT, expectedResponse);
        simpleSocketServer.start();
        Thread.sleep(10000);
    }

    @Test(groups = "wso2.am", description = "testing error responses")
    public void testAPIErrorResponse() throws Exception {

        //Login to the API Publisher
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse response;
        response = apiPublisher.login(user.getUserName(), user.getPassword());
        verifyResponse(response);

        String apiName = "ErrorResponseCheckAPI";
        String apiVersion = "1.0.0";
        String apiContext = "message";
        String endpointUrl = "http://localhost:1989";
        String appName =  "testApplication";

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
            response = apiStore.addApplication(appName, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
            verifyResponse(response);

            //Subscribe the API to the DefaultApplication
            SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, apiVersion,
                    user.getUserName(), appName,
                    APIMIntegrationConstants.API_TIER.UNLIMITED);
            response = apiStore.subscribe(subscriptionRequest);
            verifyResponse(response);

            //Generate production token and invoke with that
            APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(appName);
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
            Assert.assertEquals(httpResponse.getStatusLine().getStatusCode(), 400, "Response Code Mismatched");

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
