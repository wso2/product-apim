/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class APIEndpointTypeUpdateTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(APIEndpointTypeUpdateTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    private String apiName = "APIEndpointTypeUpdateTestCaseAPIName";
    private String APIContext = "APIEndpointTypeUpdateTestCaseAPIContext";
    private String tags = "test, EndpointType";
    private String endpointUrl;
    private String description = "This is test API create by API manager integration test";
    private String APIVersion = "1.0.0";
    private String appName = "APIEndpointTypeUpdateTestCaseAPIApp";
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private APIRequest apiRequest;

    @Factory(dataProvider = "userModeDataProvider")
    public APIEndpointTypeUpdateTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api/add";
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(user.getUserName(), user.getPassword());
        apiStore.login(user.getUserName(), user.getPassword());

    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation and subscribe")
    public void testAPICreation() throws Exception {
        String providerName = user.getUserName();

        apiRequest = new APIRequest(apiName, APIContext, new URL(endpointUrl));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setProvider(providerName);

        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

        //add a application
        serviceResponse = apiStore.addApplication(appName, APIThrottlingTier.UNLIMITED.getState(), "", "this-is-test");
        verifyResponse(serviceResponse);

        String provider = user.getUserName();

        //subscribe to the api
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, provider);
        subscriptionRequest.setApplicationName(appName);
        subscriptionRequest.setTier(APIMIntegrationConstants.API_TIER.GOLD);
        serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);

        //generate the key for the subscription
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(appName);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        log.info(responseString);
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Assert.assertNotNull("Access Token not found " + responseString, accessToken);

        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);

    }

    @Test(groups = { "wso2.am" }, description = "Invoke HTTP before Update", dependsOnMethods = "testAPICreation")
    public void testHTTPTransportBeforeUpdate() throws Exception {
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        Map<String, String> had = new HashMap<String, String>();

        //invoke HTTP transport
        HttpResponse serviceResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp(APIContext + "/" + APIVersion), requestHeaders);
        assertEquals(serviceResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");

    }

    @Test(groups = {
            "wso2.am" }, description = "Invoke HTTPS before Update", dependsOnMethods = "testHTTPTransportBeforeUpdate")
    public void testHTTPSTransportBeforeUpdate() throws Exception {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER).build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttps(APIContext + "/" + APIVersion));
        get.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                requestHeaders.get(APIMIntegrationConstants.AUTHORIZATION_HEADER));
        //invoke HTTPS transport
        CloseableHttpResponse response = httpClient.execute(get);
        assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");

    }

    @Test(groups = {
            "wso2.am" }, description = "Update to only HTTP transport and invoke", dependsOnMethods = "testHTTPSTransportBeforeUpdate")
    public void testUpdatedHTTPTransport() throws Exception {

        //create update request for restrict HTTPS
        APIRequest apiRequest = new APIRequest(apiName, APIContext, new URL(endpointUrl));
        apiRequest.setHttps_checked("");
        apiRequest.setProvider(user.getUserName());
        System.out.println(apiRequest.getProvider());
        HttpResponse serviceResponse = apiPublisher.updateAPI(apiRequest);
        assertTrue(serviceResponse.getData().contains("\"error\" : false"), apiName + " is not updated properly");

        //Check whether API is updated from the above request
        HttpResponse apiUpdateResponsePublisher = apiPublisher
                .getAPI(apiName, apiRequest.getProvider(), apiRequest.getVersion());

        //invoke HTTP transport
        serviceResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(APIContext + "/" + APIVersion), requestHeaders);
        assertEquals(serviceResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");

        CloseableHttpClient httpClient = HttpClients.custom()
                .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER).build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttps(APIContext + "/" + APIVersion));
        get.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                requestHeaders.get(APIMIntegrationConstants.AUTHORIZATION_HEADER));
        //invoke HTTPS transport
        CloseableHttpResponse response = httpClient.execute(get);
        assertEquals(response.getStatusLine().getStatusCode(), Response.Status.FORBIDDEN.getStatusCode(),
                "Response code mismatched when api invocation");

    }

    @Test(groups = {
            "wso2.am" }, description = "Update to only HTTPS transport and invoke", dependsOnMethods = "testUpdatedHTTPTransport")
    public void testUpdatedHTTPSTransport() throws Exception {
        //create update request for restrict HTTP
        APIRequest apiRequest = new APIRequest(apiName, APIContext, new URL(endpointUrl));
        apiRequest.setProvider(user.getUserName());
        apiRequest.setHttp_checked("");
        apiRequest.setHttps_checked("https");
        apiPublisher.updateAPI(apiRequest);

        //invoke HTTP transport
        HttpResponse serviceResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp(APIContext + "/" + APIVersion), requestHeaders);
        assertEquals(serviceResponse.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                "Response code mismatched when api invocation");

        CloseableHttpClient httpClient = HttpClients.custom()
                .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER).build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttps(APIContext + "/" + APIVersion));
        get.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                requestHeaders.get(APIMIntegrationConstants.AUTHORIZATION_HEADER));
        //invoke HTTPS transport
        CloseableHttpResponse response = httpClient.execute(get);
        assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication(appName);
        apiPublisher.deleteAPI(apiName, APIVersion, user.getUserName());

        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                //not for tenant now due to SYNC issue
//                 new Object[] { TestUserMode.TENANT_ADMIN },
        };
    }

}
