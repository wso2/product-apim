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
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
                                              + File.separator + "dummy_api_relative_url_loc_header.xml"
                , gatewayContextMgt, gatewaySessionCookie);
    }

    @Test(groups = "wso2.am", description = "Check functionality of the API for relative URL location header")
    public void testAPIWithRelativeUrlLocationHeader() throws Exception {

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                           publisherContext.getContextTenant().getContextUser().getPassword());

        String apiName = "RelativeUrlLocationHeaderAPI";
        String apiVersion = "1.0.0";
        String apiContext = "/relative";
        String endpointUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "response/1.0.0";

        String appName = "RelativeLocHeaderAPP";

        //Create the api creation request object
        APIRequest apiRequest = null;
        apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));

        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection("Unlimited");
        apiRequest.setTier("Unlimited");

        //Add the API using the API publisher.
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
        apiStore.addApplication(appName, "Unlimited", "", "");

        //Subscribe the API to the application
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, apiVersion,
                                                                          storeContext.getContextTenant().getContextUser().getUserName(),
                                                                          appName, "Unlimited");
        apiStore.subscribe(subscriptionRequest);

        APPKeyRequestGenerator generateAppKeyRequestProduction = new APPKeyRequestGenerator(appName);
        generateAppKeyRequestProduction.setKeyType("PRODUCTION");

        String responseString =
                apiStore.generateApplicationKey(generateAppKeyRequestProduction).getData();
        JSONObject responseProduction = new JSONObject(responseString);
        Assert.assertEquals(responseProduction.getJSONObject("data").equals(null), false,
                            "Generating production key failed");

        String accessToken =
                responseProduction.getJSONObject("data").getJSONObject("key").get("accessToken")
                        .toString();
        Assert.assertEquals(accessToken.isEmpty(), false, "Production access token is Empty");

        //Send GET Request

        Map<String, String> requestHeadersGet = new HashMap<String, String>();
        requestHeadersGet.put("accept", "text/xml");

        requestHeadersGet.put("Authorization", "Bearer " + accessToken);

        HttpResponse httpResponse = HttpRequestUtil.doGet(endpointUrl + "/" + apiVersion, requestHeadersGet);

        Assert.assertEquals(httpResponse.getResponseCode(), 201, "Response Code Mismatched");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        //removing APIs and Applications
        super.cleanUp();

        log.info("Cleaned up API Manager");
    }

}
