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
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.HttpClient;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.wso2.am.scenario.test.common.ScenarioTestUtils.readFromFile;

public class SecureUsingAuth2TestCases extends ScenarioTestBase {
    private APIStoreRestClient apiStore;
    private static final Log log = LogFactory.getLog(SecureUsingAuth2TestCases.class);
    private APIPublisherRestClient apiPublisher;
    private final String SUBSCRIBER_USERNAME = "3.1.1-subscriber";
    private final String SUBSCRIBER_PASSWORD = "password@123";
    private static final String ADMIN_LOGIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String API_DEVELOPER_USERNAME = "3.1.1-developer";
    private static final String API_DEVELOPER_PASSWORD = "password@123";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    String accessToken;
    private static final String TEST_API_1_NAME = "PhoneVerifyAPI-1";
    private static final String TEST_API_2_NAME = "PhoneVerifyAPI-2";
    private static final String TEST_API_1_CONTEXT = "/phone";
    private static final String TEST_API_2_CONTEXT = "/phones";
    private static final String TEST_API_1_VERSION = "1.0.0";
    private static final String TEST_API_2_VERSION = "1.0.0";
    private static final String TEST_APPLICATION_NAME = "TestApp1";

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        apiStore = new APIStoreRestClient(storeURL);
        apiPublisher = new APIPublisherRestClient(publisherURL);

        createUserWithSubscriberRole(SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        createUserWithPublisherAndCreatorRole(API_DEVELOPER_USERNAME, API_DEVELOPER_PASSWORD, ADMIN_LOGIN_USERNAME,
                ADMIN_PASSWORD);

        apiStore.login(SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD);
        apiPublisher.login(API_DEVELOPER_USERNAME, API_DEVELOPER_PASSWORD);

        // create and publish sample API
        String swaggerFilePath = System.getProperty("framework.resource.location") + "swaggerFiles" + File.separator +
                "phoneverify-swagger.json";
        File swaggerFile = new File(swaggerFilePath);
        String swaggerContent = readFromFile(swaggerFile.getAbsolutePath());
        JSONObject swaggerJson = new JSONObject(swaggerContent);

        apiPublisher.developSampleAPI(swaggerJson, API_DEVELOPER_USERNAME, backendEndPoint, true, "public");
        createApplication(TEST_APPLICATION_NAME);

        // Check the visibility of the API in API store
        isAPIVisibleInStore(TEST_API_1_NAME, apiStore);

        // Add subscription to API
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(TEST_API_1_NAME, TEST_API_1_VERSION, API_DEVELOPER_USERNAME,
                TEST_APPLICATION_NAME, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        HttpResponse addSubscriptionResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(addSubscriptionResponse);
        log.info(TEST_APPLICATION_NAME + " is subscribed to " + TEST_API_1_NAME);

        accessToken = generateAppKeys();
    }

    @Test(description = "3.1.1.1", dataProvider = "AuthorizationHeadersDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testOAuth2Authorization(String tokenPrefix) throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, tokenPrefix + accessToken);
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        String gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION, "/CheckPhoneNumber");
        log.debug("Gateway HTTPS URL : " + gatewayHttpsURL);
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("PhoneNumber", "18006785432"));
        urlParameters.add(new BasicNameValuePair("LicenseKey", "0"));
        HttpResponse apiResponse = HttpClient.doPost(gatewayHttpsUrl, requestHeaders, urlParameters);
        log.debug("API response : " + apiResponse.getData());
        assertEquals(apiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
    }

    public void createApplication(String applicationName) throws APIManagerIntegrationTestException {
        HttpResponse addApplicationResponse = null;
        try {
            addApplicationResponse = apiStore.addApplication(applicationName,
                    APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "description");
        } catch (APIManagerIntegrationTestException e) {
            String error = "Error in creating Testing Application " + applicationName;
            throw new APIManagerIntegrationTestException(error, e);
        }
        verifyResponse(addApplicationResponse);
        log.info("Application - " + applicationName + "is created successfully");
    }

    public String generateAppKeys() throws APIManagerIntegrationTestException {
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(TEST_APPLICATION_NAME);
        HttpResponse keyGenerationResponse = null;
        try {
            keyGenerationResponse = apiStore.generateApplicationKey(appKeyRequestGenerator);
        } catch (APIManagerIntegrationTestException e) {
            String error = "Error in generating Application Keys";
            throw new APIManagerIntegrationTestException(error, e);
        }
        log.info("Key generation response for application \'" + TEST_APPLICATION_NAME + "\' response data :"
                + keyGenerationResponse.getData());
        verifyResponse(keyGenerationResponse);
        JSONObject keyGenerationRespData = new JSONObject(keyGenerationResponse.getData());

        accessToken = (keyGenerationRespData.getJSONObject("data").getJSONObject("key"))
                .get("accessToken").toString();
        return accessToken;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication(TEST_APPLICATION_NAME);
        apiPublisher.deleteAPI(TEST_API_1_NAME, TEST_API_1_VERSION, API_DEVELOPER_USERNAME);
        deleteUser(SUBSCRIBER_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteUser(API_DEVELOPER_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
    }
}
