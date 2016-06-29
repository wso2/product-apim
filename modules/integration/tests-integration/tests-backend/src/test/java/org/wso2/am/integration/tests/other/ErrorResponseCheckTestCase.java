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
import org.json.JSONException;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;

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
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Test to check some security issues in Error responses
 */
public class ErrorResponseCheckTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(RelativeUrlLocationHeaderTestCase.class);

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    final String ACCESS_TOKEN_TYPE = "PRODUCTION";
    final String RESOURCE_NAME = "/TestStatus";

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

    @Test(groups = "wso2.am", description = "testing the vulnerable content in error responses")
    public void testInvalidAccessTokenInvocation() throws Exception {

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

        String apiName = "StatusCheckAPI";
        String apiVersion = "1.0.0";
        String apiContext = "status";
        String endpointUrl = gatewayUrlsMgt.getWebAppURLNhttp() + "response/1.0.0";
        String invalidEndpointUrl = gatewayUrlsMgt.getWebAppURLNhttp() + "response/1.0.0";
        String tier = "Unlimited";
        String appName = "statusCheckApp";

        try {
            //create API
            APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));
            apiRequest.setVersion(apiVersion);
            apiRequest.setTiersCollection(tier);
            apiRequest.setTier(tier);

            //Add a GET resource
            apiRequest.setResourceCount("0");
            apiRequest.setResourceMethod("GET");
            apiRequest.setUriTemplate("TestStatus");
            apiRequest.setResourceMethodAuthType("Any");
            apiRequest.setResourceMethodThrottlingTier("Unlimited");
            apiRequest.setVisibility("public");
            apiRequest.setRoles("admin");

            apiPublisher.addAPI(apiRequest);

            APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName,
                    publisherContext.getContextTenant().getContextUser().getUserName(), APILifeCycleState.PUBLISHED);
            //Publish the API
            apiPublisher.changeAPILifeCycleStatus(updateRequest);

            //subscribe to the API
            apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                    storeContext.getContextTenant().getContextUser().getPassword());

            //Add an Application in the Store.
            apiStore.addApplication(appName, tier, "", "");

            //Subscribe the API to the application
            SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, apiVersion,
                    storeContext.getContextTenant().getContextUser().getUserName(),
                    appName, tier);
            apiStore.subscribe(subscriptionRequest);

            //Generate production access token
            APPKeyRequestGenerator generateAppKeyRequestProduction = new APPKeyRequestGenerator(appName);
            generateAppKeyRequestProduction.setKeyType(ACCESS_TOKEN_TYPE);

            String responseString =
                    apiStore.generateApplicationKey(generateAppKeyRequestProduction).getData();
            JSONObject responseProduction = new JSONObject(responseString);
            Assert.assertEquals(responseProduction.getJSONObject("data").equals(null), false,
                    "Generating production key failed");

            String accessToken =
                    responseProduction.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
            Assert.assertEquals(accessToken.isEmpty(), false, "Production access token is Empty");

            //Send GET Request
            Map<String, String> requestHeadersGet = new HashMap<String, String>();
            requestHeadersGet.put("accept", "text/xml");


            requestHeadersGet.put("Authorization", "Bearer " + accessToken);

            HttpResponse httpResponse = HttpRequestUtil.doGet(gatewayUrlsMgt.getWebAppURLNhttp() + apiContext + "/"
                    + apiVersion + RESOURCE_NAME, requestHeadersGet);
            Assert.assertEquals(httpResponse.getResponseCode(), 201, "Response Code Mismatched");


            /* ----------------------------test 1 : invoke with invalid apiContext ---------------------------- */

            HttpResponse HttpResponseForInvalidContext = HttpRequestUtil.doGet(gatewayUrlsMgt.getWebAppURLNhttp() +
                    "invalidContext/" + apiVersion + RESOURCE_NAME, requestHeadersGet);
            Assert.assertEquals(HttpResponseForInvalidContext.getResponseCode(), 404, "Response Code Mismatched");
            Assert.assertEquals(HttpResponseForInvalidContext.getResponseMessage().contains("invalidContext/1.0.0"),
                    false,"The message contains the resource path requested.");

            /* ------------------------ test 2 : invoke with request to an invalid resource ------------------ */

            HttpResponse HttpResponseForInvalidResource = HttpRequestUtil.doGet(gatewayUrlsMgt.getWebAppURLNhttp() +
                    apiContext + "/" + apiVersion + "/TestResource", requestHeadersGet);
            Assert.assertEquals(HttpResponseForInvalidResource.getResponseCode(), 403, "Response Code Mismatched");
            Assert.assertEquals(HttpResponseForInvalidResource.getResponseMessage().contains(apiContext + "/" +
                    apiVersion + "/TestResource"), false, "The message contains the resource path requested.");

            /* ----------------------------test 3 : invoke with invalid access token ---------------------------- */

            requestHeadersGet.clear();
            requestHeadersGet.put("accept", "text/xml");
            requestHeadersGet.put("Authorization", "Bearer " + "invalid_access_token");
            HttpResponse HttpResponseForInvalidAccessToken = HttpRequestUtil.doGet(gatewayUrlsMgt.getWebAppURLNhttp() +
                    apiContext + "/" + apiVersion + "/TestStatus", requestHeadersGet); //201
            Assert.assertEquals(HttpResponseForInvalidAccessToken.getResponseCode(), 401, "Response Code Mismatched");
            Assert.assertEquals(HttpResponseForInvalidAccessToken.getResponseMessage().contains("invalid_access_token"),
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

        //removing APIs and Applications
        super.cleanUp();
        log.info("Cleaned up API Manager");
    }

}
