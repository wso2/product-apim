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
import org.apache.http.NoHttpResponseException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
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
    private APIPublisherRestClient apiPublisherTenant;
    private APIStoreRestClient apiStoreTenant;

    private String apiName = "APIEndpointTypeUpdateTestCaseAPIName";
    private String APIContext = "APIEndpointTypeUpdateTestCaseAPIContext";
    private String tags = "test, EndpointType";
    private String endpointUrl;
    private String description = "This is test API create by API manager integration test";
    private String APIVersion = "1.0.0";
    private String appName = "APIEndpointTypeUpdateTestCaseAPIApp";
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private APIRequest apiRequest;
    private APIRequest apiRequestTenant;

    private String CARBON_SUPER_ADMIN = "admin";
    private String CARBON_SUPER_ADMIN_PASS = "admin";
    private String TENANT_WSO2_ADMIN = "admin@wso2.com";
    private String TENANT_WSO2_ADMIN_PASS = "admin";
    private String TENANT_WSO2 = "wso2.com";
    private String publisherURLHttp;
    private String storeURLHttp;
    private String ApiHTTPInvocationURLTenant;
    private String ApiHTTPSInvocationURLTenant;

    @Factory(dataProvider = "userModeDataProvider")
    public APIEndpointTypeUpdateTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        publisherURLHttp = getPublisherURLHttp();
        storeURLHttp = getStoreURLHttp();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api/add";
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(user.getUserName(), user.getPassword());
        apiStore = new APIStoreRestClient(storeURLHttp);
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
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Assert.assertNotNull("Access Token not found " + responseString, accessToken);

        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);

    }

    @Test(groups = { "wso2.am" }, description = "Invoke HTTP before Update", dependsOnMethods = "testAPICreation")
    public void testHTTPTransportBeforeUpdate() throws Exception {
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(user.getUserName(), user.getPassword());
        //invoke HTTP transport
        HttpResponse serviceResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp(APIContext + "/" + APIVersion), requestHeaders);
        assertEquals(serviceResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");

        serviceResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(APIContext + "/" + APIVersion), requestHeaders);
        assertEquals(serviceResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
    }

    @Test(groups = { "wso2.am" }, description = "Update to only HTTP transport and invoke",
            dependsOnMethods = "testHTTPTransportBeforeUpdate")
    public void testUpdatedHTTPTransport() throws Exception {

        //create update request for restrict HTTPS
        APIRequest apiRequest = new APIRequest(apiName, APIContext, new URL(endpointUrl));
        apiRequest.setHttps_checked("");
        apiRequest.setProvider(user.getUserName());
        HttpResponse serviceResponse = apiPublisher.updateAPI(apiRequest);
        waitForAPIDeployment();
        assertTrue(serviceResponse.getData().contains("\"error\" : false"), apiName + " is not updated properly");

        //Check whether API is updated from the above request
        HttpResponse apiUpdateResponsePublisher = apiPublisher
                .getAPI(apiName, apiRequest.getProvider(), apiRequest.getVersion());

        //invoke HTTP transport
        serviceResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(APIContext + "/" + APIVersion), requestHeaders);
        assertEquals(serviceResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");

        //invoke HTTPS transport
        try {
            serviceResponse = HTTPSClientUtils
                    .doGet(getAPIInvocationURLHttps(APIContext + "/" + APIVersion), requestHeaders);
            assertEquals(serviceResponse.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                    "Response code mismatched when api invocation");
        } catch (Exception ignored) {
            //exception throw because timeout and null pointer because transport is not allowed.
        }
    }

    @Test(groups = { "wso2.am" }, description = "Update to only HTTPS transport and invoke",
            dependsOnMethods = "testUpdatedHTTPTransport")
    public void testUpdatedHTTPSTransport() throws Exception {
        //create update request for restrict HTTP
        APIRequest apiRequest = new APIRequest(apiName, APIContext, new URL(endpointUrl));
        apiRequest.setProvider(user.getUserName());
        apiRequest.setHttp_checked("");
        apiRequest.setHttps_checked("https");
        apiPublisher.updateAPI(apiRequest);
        waitForAPIDeployment();

        try {
            //invoke HTTP transport
            HttpResponse serviceResponse = HttpRequestUtil
                    .doGet(getAPIInvocationURLHttp(APIContext + "/" + APIVersion), requestHeaders);
            assertEquals(serviceResponse.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                    "Response code mismatched when api invocation");
        } catch (Exception ignored) {
            //exception throw because timeout and null pointer because transport is not allowed.
        }

        //invoke HTTPS transport
        HttpResponse serviceResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(APIContext + "/" + APIVersion), requestHeaders);
        assertEquals(serviceResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");

    }

    @Test(groups = { "wso2.am" }, description = "Update to only HTTPS transport and invoke",
            dependsOnMethods = "testUpdatedHTTPSTransport")
    public void testAPICreationTenant() throws Exception {
        apiStoreTenant = new APIStoreRestClient(storeURLHttp);
        apiStoreTenant.login(TENANT_WSO2_ADMIN, TENANT_WSO2_ADMIN_PASS);
        apiPublisherTenant = new APIPublisherRestClient(publisherURLHttp);
        apiPublisherTenant.login(TENANT_WSO2_ADMIN, TENANT_WSO2_ADMIN_PASS);

        apiRequestTenant = new APIRequest(apiName, APIContext, new URL(endpointUrl));
        apiRequestTenant.setTags(tags);
        apiRequestTenant.setDescription(description);
        apiRequestTenant.setVersion(APIVersion);
        apiRequestTenant.setProvider(TENANT_WSO2_ADMIN);

        //add test api
        HttpResponse serviceResponse = apiPublisherTenant.addAPI(apiRequestTenant);
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, TENANT_WSO2_ADMIN,
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisherTenant.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

        //add a application
        serviceResponse = apiStoreTenant
                .addApplication(appName, APIThrottlingTier.UNLIMITED.getState(), "", "this-is-test");
        verifyResponse(serviceResponse);

        //subscribe to the api
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, TENANT_WSO2_ADMIN);
        subscriptionRequest.setApplicationName(appName);
        subscriptionRequest.setTier(APIMIntegrationConstants.API_TIER.GOLD);
        serviceResponse = apiStoreTenant.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);

        //generate the key for the subscription
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(appName);
        String responseString = apiStoreTenant.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Assert.assertNotNull("Access Token not found " + responseString, accessToken);

        requestHeaders.clear();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);

        ApiHTTPInvocationURLTenant = getAPIInvocationURLHttp("t/" + TENANT_WSO2 + "/" + APIContext + "/") + APIVersion;
        ApiHTTPSInvocationURLTenant =
                getAPIInvocationURLHttps("t/" + TENANT_WSO2 + "/" + APIContext + "/") + APIVersion;
    }

    @Test(groups = { "wso2.am" }, description = "Invoke HTTP before Update", dependsOnMethods = "testAPICreationTenant")
    public void testHTTPTransportBeforeUpdateInTenant() throws Exception {
        waitForAPIDeploymentSync(apiRequestTenant.getProvider(), apiRequestTenant.getName(),
                apiRequestTenant.getVersion(), APIMIntegrationConstants.IS_API_EXISTS);
        apiStoreTenant = new APIStoreRestClient(storeURLHttp);
        apiStoreTenant.login(TENANT_WSO2_ADMIN, TENANT_WSO2_ADMIN_PASS);
        //invoke HTTP transport
        HttpResponse serviceResponse = HttpRequestUtil.doGet(ApiHTTPInvocationURLTenant, requestHeaders);
        assertEquals(serviceResponse.getResponseCode(), Response.Status.OK.getStatusCode(), serviceResponse.getData());

        //invoke HTTPS transport
        serviceResponse = HttpRequestUtil.doGet(ApiHTTPSInvocationURLTenant, requestHeaders);
        assertEquals(serviceResponse.getResponseCode(), Response.Status.OK.getStatusCode(), serviceResponse.getData());

    }

    @Test(groups = { "wso2.am" }, description = "Update to only HTTP transport and invoke",
            dependsOnMethods = "testHTTPTransportBeforeUpdateInTenant")
    public void testUpdatedHTTPTransportTenant() throws Exception {

        //create update request for restrict HTTPS
        APIRequest apiRequest = new APIRequest(apiName, APIContext, new URL(endpointUrl));
        apiRequest.setHttps_checked("");
        apiRequest.setProvider(TENANT_WSO2_ADMIN);
        HttpResponse serviceResponse = apiPublisherTenant.updateAPI(apiRequest);
        waitForAPIDeployment();
        assertTrue(serviceResponse.getData().contains("\"error\" : false"), apiName + " is not updated properly");

        //Check whether API is updated from the above request
        HttpResponse apiUpdateResponsePublisher = apiPublisherTenant
                .getAPI(apiName, TENANT_WSO2_ADMIN, apiRequest.getVersion());

        //invoke HTTP transport
        serviceResponse = HttpRequestUtil.doGet(ApiHTTPInvocationURLTenant, requestHeaders);
        assertEquals(serviceResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");

        //invoke HTTPS transport
        try {
            serviceResponse = HttpRequestUtil.doGet(ApiHTTPSInvocationURLTenant, requestHeaders);
            assertEquals(serviceResponse.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                    "Response code mismatched when api invocation");
        } catch (Exception ignored) {
            //exception throw because timeout and null pointer because transport is not allowed.
        }
    }

    @Test(groups = { "wso2.am" }, description = "Update to only HTTPS transport and invoke",
            dependsOnMethods = "testUpdatedHTTPTransportTenant")
    public void testUpdatedHTTPSTransportTenant() throws Exception {
        //create update request for restrict HTTP
        APIRequest apiRequest = new APIRequest(apiName, APIContext, new URL(endpointUrl));
        apiRequest.setProvider(TENANT_WSO2_ADMIN);
        apiRequest.setHttp_checked("");
        apiRequest.setHttps_checked("https");
        apiPublisherTenant.updateAPI(apiRequest);
        waitForAPIDeployment();

        try {
            //invoke HTTP transport
            HttpResponse serviceResponse = HttpRequestUtil.doGet(ApiHTTPInvocationURLTenant, requestHeaders);
            assertEquals(serviceResponse.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                    "Response code mismatched when api invocation");
        } catch (Exception ignored) {
            //exception throw because timeout and null pointer because transport is not allowed.
        }

        //invoke HTTPS transport
        HttpResponse serviceResponse = HttpRequestUtil.doGet(ApiHTTPSInvocationURLTenant, requestHeaders);
        assertEquals(serviceResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication(appName);
        apiStoreTenant.removeApplication(appName);
        apiPublisher.deleteAPI(apiName, APIVersion, user.getUserName());
        apiPublisherTenant.deleteAPI(apiName, APIVersion, TENANT_WSO2_ADMIN);

        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN }, };
    }
}
