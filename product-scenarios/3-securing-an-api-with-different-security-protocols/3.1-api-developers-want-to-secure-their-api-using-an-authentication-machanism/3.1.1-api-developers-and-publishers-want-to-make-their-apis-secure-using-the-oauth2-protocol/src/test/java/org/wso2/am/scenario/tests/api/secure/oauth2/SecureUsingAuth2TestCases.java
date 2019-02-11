/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.scenario.tests.api.secure.oauth2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.HttpClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class SecureUsingAuth2TestCases extends ScenarioTestBase {
    private APIStoreRestClient apiStore;
    private List<String> applicationsList = new ArrayList<>();
    private static final Log log = LogFactory.getLog(SecureUsingAuth2TestCases.class);
    private APIPublisherRestClient apiPublisher;
    private final String SUBSCRIBER_USERNAME = "subscriberUser2";
    private final String SUBSCRIBER_PASSWORD = "password@123";
    private static final String ADMIN_LOGIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private final String API_DEVELOPER_USERNAME = "3.1.1-user";
    private final String API_DEVELOPER_PASSWORD = "password@3.1.1-user";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    // private String backendEndPoint = "http://localhost:9443/am/sample/pizzashack/v1/api";

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        apiStore = new APIStoreRestClient(storeURL);
        apiPublisher = new APIPublisherRestClient(publisherURL);

        //  createUserWithSubscriberRole(SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        //    createUserWithPublisherAndCreatorRole(API_DEVELOPER_USERNAME, API_DEVELOPER_PASSWORD, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);

        apiStore.login(SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD);
        apiPublisher.login(API_DEVELOPER_USERNAME, API_DEVELOPER_PASSWORD);
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        for (String name : applicationsList) {
            apiStore.removeApplication(name);
        }
        applicationsList.clear();
        deleteUser(SUBSCRIBER_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteUser(API_DEVELOPER_USERNAME,  ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        apiPublisher.deleteAPI("PizzaShackTestAPI", "1.0.0", API_DEVELOPER_USERNAME);
    }

    @Test(description = "3.1.1.1")
    public void testOAuth2Authorization() throws Exception {
        // create and publish sample API
        apiPublisher.developSampleAPI("swaggerFiles/phoneverify-swagger.json", API_DEVELOPER_USERNAME, backendEndPoint,
                true, "public");
        String testApplication = "TestApp1";
        applicationsList.add(testApplication);

        // Create an application
        HttpResponse addApplicationResponse = apiStore.addApplication(testApplication,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "description");
        verifyResponse(addApplicationResponse);
        log.info("Application - " + testApplication + "is created successfully");

        // Generate keys for the application
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(testApplication);
        HttpResponse keyGenerationResponse = apiStore.generateApplicationKey(appKeyRequestGenerator);
        // add logs to verify http response 404 when generating tokens
        JSONObject responseStringJson = new JSONObject(keyGenerationResponse.getData());
        log.info("key generation response for application \'" + testApplication + "\' response data :"
                + keyGenerationResponse.getData());

        if (!responseStringJson.getBoolean("error")) {
            verifyResponse(keyGenerationResponse);

            // Check the visibility of the API in API store
            isAPIVisibleInStore("PhoneVerifyAPI", apiStore);

            // Add subscription to API
            SubscriptionRequest subscriptionRequest = new SubscriptionRequest("PhoneVerifyAPI", "1.0.0", API_DEVELOPER_USERNAME,
                    testApplication, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
            HttpResponse addSubscriptionResponse = apiStore.subscribe(subscriptionRequest);
            verifyResponse(addSubscriptionResponse);
            log.info(testApplication + " is subscribed to " + "PhoneVerifyAPI");

            // Generate Keys
            JSONObject keyGenerationRespData = new JSONObject(keyGenerationResponse.getData());
            String accessToken = (keyGenerationRespData.getJSONObject("data").getJSONObject("key"))
                    .get("accessToken").toString();

            // Invoke the API
            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
            requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
            String gatewayHttpsUrl = getHttpsAPIInvocationURL("/phone", "1.0.0", "/CheckPhoneNumber");
            log.debug("Gateway HTTPS URL : " + gatewayHttpsURL);
            List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            urlParameters.add(new BasicNameValuePair("PhoneNumber", "18006785432"));
            urlParameters.add(new BasicNameValuePair("LicenseKey", "0"));
            HttpResponse apiResponse = HttpClient.doPost(gatewayHttpsUrl, requestHeaders, urlParameters);
            log.info("API response : " + apiResponse.getData());
            assertEquals(apiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched when api invocation");
        }
    }
}
