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
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class APIEndpointTypeUpdateTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(APIEndpointTypeUpdateTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    private String apiNamePrefix = "APIEndpointTypeUpdateTestCaseAPIName";
    private String APIContextPrefix = "APIEndpointTypeUpdateTestCaseAPIContext";
    private String tags = "test, EndpointType";
    private String endpointUrl;
    private String description = "This is test API create by API manager integration test";
    private String APIVersion = "1.0.0";
    String appName = "APIEndpointTypeUpdateTestCaseAPIApp";
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private String gatewaySessionCookie;

    @Factory(dataProvider = "userModeDataProvider")
    public APIEndpointTypeUpdateTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        gatewaySessionCookie = createSession(gatewayContextMgt);

        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();

        endpointUrl = getGatewayURLNhttp() + "response";
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(user.getUserName(), user.getPassword());
        apiStore.login(user.getUserName(), user.getPassword());

        //Load the back-end dummy API
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            loadSynapseConfigurationFromClasspath(
                    "artifacts" + File.separator + "AM" + File.separator + "synapseconfigs" + File.separator + "rest"
                            + File.separator + "dummy_api.xml", gatewayContextMgt, gatewaySessionCookie);
        }

    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation")
    public void testAPICreation() throws Exception {
        String providerName = user.getUserName();

        APIRequest apiRequest = new APIRequest(apiNamePrefix, APIContextPrefix, new URL(endpointUrl));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setProvider(providerName);
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);

        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiNamePrefix, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

        serviceResponse = apiStore.addApplication(appName, APIThrottlingTier.UNLIMITED.getState(), "", "this-is-test");
        verifyResponse(serviceResponse);

        String provider = user.getUserName();

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiNamePrefix, provider);
        subscriptionRequest.setApplicationName(appName);
        subscriptionRequest.setTier(APIMIntegrationConstants.API_TIER.GOLD);
        serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);

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
        Map<String, String> had = new HashMap<String, String>();
        HttpResponse serviceResponse1 = HttpRequestUtil.doGet(endpointUrl, had);
        HttpResponse serviceResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp(APIContextPrefix + "/" + APIVersion), requestHeaders);

        log.info(serviceResponse.getData());
        assertEquals(serviceResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");

    }

    @Test(groups = {
            "wso2.am" }, description = "Invoke HTTPS before Update", dependsOnMethods = "testHTTPTransportBeforeUpdate")
    public void testHTTPSTransportBeforeUpdate() throws Exception {

        CloseableHttpClient httpClient = HttpClients.custom()
                .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER).build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttps(APIContextPrefix + "/" + APIVersion));
        get.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                requestHeaders.get(APIMIntegrationConstants.AUTHORIZATION_HEADER));
        CloseableHttpResponse response = httpClient.execute(get);
        assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");

    }

    @Test(groups = {
            "wso2.am" }, description = "Invoke HTTP after Update", dependsOnMethods = "testHTTPSTransportBeforeUpdate")
    public void testUpdatedHTTPTransport() throws Exception {

        APIRequest apiRequest = new APIRequest(apiNamePrefix, APIContextPrefix, new URL(endpointUrl));
        apiRequest.setHttps_checked("");
        apiPublisher.updateAPI(apiRequest);

        HttpResponse serviceResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp(APIContextPrefix + "/" + APIVersion), requestHeaders);
        log.info(serviceResponse.getData());
        assertEquals(serviceResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");

        CloseableHttpClient httpClient = HttpClients.custom()
                .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER).build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttps(APIContextPrefix + "/" + APIVersion));
        get.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                requestHeaders.get(APIMIntegrationConstants.AUTHORIZATION_HEADER));
        CloseableHttpResponse response = httpClient.execute(get);
        assertEquals(response.getStatusLine().getStatusCode(), Response.Status.FORBIDDEN.getStatusCode(),
                "Response code mismatched when api invocation");

    }

    @Test(groups = {
            "wso2.am" }, description = "Invoke HTTPS after Update", dependsOnMethods = "testUpdatedHTTPTransport")
    public void testUpdatedHTTPSTransport() throws Exception {

        APIRequest apiRequest = new APIRequest(apiNamePrefix, APIContextPrefix, new URL(endpointUrl));
        apiRequest.setHttp_checked("");
        apiRequest.setHttps_checked("https");
        apiPublisher.updateAPI(apiRequest);

        HttpResponse serviceResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp(APIContextPrefix + "/" + APIVersion), requestHeaders);
        log.info(serviceResponse.getData());
        assertEquals(serviceResponse.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                "Response code mismatched when api invocation");

        CloseableHttpClient httpClient = HttpClients.custom()
                .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER).build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttps(APIContextPrefix + "/" + APIVersion));
        get.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                requestHeaders.get(APIMIntegrationConstants.AUTHORIZATION_HEADER));
        CloseableHttpResponse response = httpClient.execute(get);
        assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication(appName);
        apiPublisher.deleteAPI(apiNamePrefix, APIVersion, user.getUserName());

        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }
}
