/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.=[=
 *
 */

package org.wso2.am.integration.tests.token;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Invoke API from Production and Sandbox key by giving valid end points Urls for the both
 */
public class APIM34InvokeAPIWithSandboxTokenTestCase extends APIMIntegrationBaseTest {
    private final String apiNameTest1 = "APIM34SandBoxTest1API";
    private final String apiNameTest2 = "APIM34SandBoxTest2API";
    private final String apiNameTest3 = "APIM34SandBoxTest3API";
    private final String apiNameTest4 = "APIM34SandBoxTest4API";
    private final String apiNameTest5 = "APIM34SandBoxTest5API";
    private final String apiVersion = "1.0.0";
    private final String apiDescription = "This is Test API Created by API Manager Integration Test";
    private String apiTag = "tag34-1, tag34-2, tag34-3";
    private String applicationNameTest1 = "APIM34InvokeAPIWithSandboxTokenTest1";
    private String applicationNameTest2 = "APIM34InvokeAPIWithSandboxTokenTest2";
    private String applicationNameTest3 = "APIM34InvokeAPIWithSandboxTokenTest3";
    private String applicationNameTest4 = "APIM34InvokeAPIWithSandboxTokenTest4";
    private String applicationNameTest5 = "APIM34InvokeAPIWithSandboxTokenTest5";
    private String sandboxEndpointResponse = "HelloWSO2 from File 1";
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String apiProviderName;
    private String apiProductionEndPointUrl;
    private String apiSandBoxEndPointUrl;
    private APIIdentifier apiIdentifierPublisherTest1;
    private APIIdentifier apiIdentifierPublisherTest2;
    private APIIdentifier apiIdentifierPublisherTest3;
    private APIIdentifier apiIdentifierPublisherTest4;
    private APIIdentifier apiIdentifierPublisherTest5;
    private List<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();

    @Factory(dataProvider = "userModeDataProvider")
    public APIM34InvokeAPIWithSandboxTokenTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        String productionWebAppName = "jaxrs_basic";
        String SanBoxWebAppName = "name-check1";
        String apiSandBoxEndpointPostfixUrl = "name-check1/";
        String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/customers/" +
                "customerservice/customers/123";

        String sourcePathProd = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" +
                File.separator + productionWebAppName + ".war";

        String sourcePathSand = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" +
                File.separator + SanBoxWebAppName + ".war";

        String sessionId = createSession(gatewayContextWrk);

        WebAppAdminClient webAppAdminClient =
                new WebAppAdminClient(gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId);
        webAppAdminClient.uploadWarFile(sourcePathProd);
        webAppAdminClient.uploadWarFile(sourcePathSand);

        boolean isWebAppDeployProd = WebAppDeploymentUtil.isWebApplicationDeployed
                (gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId, productionWebAppName);
        assertTrue(isWebAppDeployProd, productionWebAppName + " is not deployed");

        boolean isWebAppDeploySand = WebAppDeploymentUtil.isWebApplicationDeployed
                (gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId, SanBoxWebAppName);
        assertTrue(isWebAppDeploySand, SanBoxWebAppName + " is not deployed");

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        apiProductionEndPointUrl = getGatewayURLHttp() + apiProductionEndpointPostfixUrl;
        apiSandBoxEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiSandBoxEndpointPostfixUrl;

        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();

        apiIdentifierPublisherTest1 = new APIIdentifier(apiProviderName, apiNameTest1, apiVersion);
        apiIdentifierPublisherTest2 = new APIIdentifier(apiProviderName, apiNameTest2, apiVersion);
        apiIdentifierPublisherTest3 = new APIIdentifier(apiProviderName, apiNameTest3, apiVersion);
        apiIdentifierPublisherTest4 = new APIIdentifier(apiProviderName, apiNameTest4, apiVersion);
        apiIdentifierPublisherTest5 = new APIIdentifier(apiProviderName, apiNameTest5, apiVersion);

    }

    @Test(groups = {"wso2.am"}, description = "Published an API with valid Production endpoint" +
            " and Sandbox endpoint then invoke API from Sandbox token")
    public void testInvokeAPIFromSandboxTokenWhenProvideBothEndPoints() throws Exception {

        String apiContextTest1 = "apim34SandBoxTest1API";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest1, apiContextTest1, apiVersion, apiProviderName,
                        new URL(apiProductionEndPointUrl), new URL(apiSandBoxEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        resourceBeanList.add(new APIResourceBean("GET", "Application & Application User",
                "Unlimited", "name"));
        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                apiNameTest1 + "is not created as expected");
        assertTrue(apiCreationResponse.getData().contains("\"error\" : false"),
                apiNameTest1 + "is not created as expected");

        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiNameTest1, apiProviderName,
                        APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        //Check the availability of API in Publisher and Store
        List<APIIdentifier> publisherAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiPublisher.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierPublisherTest1, publisherAPIList),
                apiNameTest1 + "is not visible in API Publisher.");

        List<APIIdentifier> storeAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiStore.getAllPublishedAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierPublisherTest1, storeAPIList),
                apiNameTest1 + "is not visible in API Store.");

        apiStore.addApplication(applicationNameTest1, "Gold", "", "");
        SubscriptionRequest subscriptionRequest =
                new SubscriptionRequest(apiNameTest1, apiProviderName);
        subscriptionRequest.setApplicationName(applicationNameTest1);

        //Validate Subscription of the API
        HttpResponse subscribeApiResponse = apiStore.subscribe(subscriptionRequest);
        assertEquals(subscribeApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                apiNameTest1 + "is not Subscribed");
        assertTrue(subscribeApiResponse.getData().contains("\"error\" : false"),
                apiNameTest1 + "is not Subscribed");

        APPKeyRequestGenerator generateAppKeyRequestSandBox =
                new APPKeyRequestGenerator(applicationNameTest1);
        generateAppKeyRequestSandBox.setKeyType("SANDBOX");
        String responseSandBox = apiStore.generateApplicationKey
                (generateAppKeyRequestSandBox).getData();
        JSONObject jsonObject = new JSONObject(responseSandBox);

        String sandboxAccessToken =
                jsonObject.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + sandboxAccessToken);

        Thread.sleep(2000);

        HttpResponse sandBoxResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp (apiContextTest1 + "/" + apiVersion )+ "/name", requestHeaders);
        assertEquals(sandBoxResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation from sandbox token");
        assertTrue(sandBoxResponse.getData().contains(sandboxEndpointResponse),
                "Response code mismatched when api invocation from sandbox token");
    }


    @Test(groups = {"wso2.am"}, description = "Published an API only with valid Sandbox endpoint" +
            " and invoke API from Sandbox token")

    public void testInvokeAPIFromSandboxTokenWhenProvideOnlySandboxEndPoint() throws Exception {

        String apiContextTest2 = "apim34SandBoxTest2API";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest2, apiContextTest2, apiVersion,
                        apiProviderName, null, new URL(apiSandBoxEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        resourceBeanList.add(new APIResourceBean("GET", "Application & Application User",
                "Unlimited", "name"));
        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                apiNameTest2 + "is not created as expected");
        assertTrue(apiCreationResponse.getData().contains("\"error\" : false"),
                apiNameTest2 + "is not created as expected");

        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiNameTest2, apiProviderName,
                        APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        //Check the availability of API in Publisher and Store
        List<APIIdentifier> publisherAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiPublisher.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierPublisherTest2, publisherAPIList),
                "Published Api is visible in API Publisher.");

        List<APIIdentifier> storeAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiStore.getAllPublishedAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierPublisherTest2, storeAPIList),
                "Published Api is visible in API Store.");

        apiStore.addApplication(applicationNameTest2, "Gold", "", "");
        SubscriptionRequest subscriptionRequest =
                new SubscriptionRequest(apiNameTest2, apiProviderName);
        subscriptionRequest.setApplicationName(applicationNameTest2);

        //Validate Subscription of the API
        HttpResponse subscribeApiResponse = apiStore.subscribe(subscriptionRequest);
        assertEquals(subscribeApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                apiNameTest2 + "is not Subscribed");
        assertTrue(subscribeApiResponse.getData().contains("\"error\" : false"),
                apiNameTest2 + "is not Subscribed");

        APPKeyRequestGenerator generateAppKeyRequestSandBox =
                new APPKeyRequestGenerator(applicationNameTest2);
        generateAppKeyRequestSandBox.setKeyType("SANDBOX");
        String responseSandBox = apiStore.generateApplicationKey
                (generateAppKeyRequestSandBox).getData();
        JSONObject jsonObject = new JSONObject(responseSandBox);

        String sandboxAccessToken =
                jsonObject.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + sandboxAccessToken);

        Thread.sleep(2000);

        HttpResponse sandBoxResponse = HttpRequestUtil.doGet(gatewayUrlsWrk.getWebAppURLNhttp() +
                apiContextTest2 + "/" + apiVersion + "/name", requestHeaders);
        assertEquals(sandBoxResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation from sandbox token");
        assertTrue(sandBoxResponse.getData().contains(sandboxEndpointResponse),
                "Response data mismatched when api invocation from sandbox token");

    }

    @Test(groups = {"wso2.am"}, description = "Published an API only with valid Production " +
            "endpoint and trying to invoke API from Sandbox token")

    public void testInvokeAPIFromSandboxTokenWhenProvideOnlyProductionEndPoint() throws Exception {

        String apiContextTest3 = "apim34SandBoxTest3API";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest3, apiContextTest3, apiVersion,
                        apiProviderName, new URL(apiProductionEndPointUrl), null);
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                apiNameTest3 + "is not created as expected");
        assertTrue(apiCreationResponse.getData().contains("\"error\" : false"),
                apiNameTest3 + "is not created as expected");

        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiNameTest3, apiProviderName,
                        APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        //Check the availability of API in Publisher and Store
        List<APIIdentifier> publisherAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiPublisher.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierPublisherTest3, publisherAPIList),
                "Published Api is visible in API Publisher.");

        List<APIIdentifier> storeAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiStore.getAllPublishedAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierPublisherTest3, storeAPIList),
                "Published Api is visible in API Store.");

        apiStore.addApplication(applicationNameTest3, "Gold", "", "");
        SubscriptionRequest subscriptionRequest =
                new SubscriptionRequest(apiNameTest3, apiProviderName);
        subscriptionRequest.setApplicationName(applicationNameTest3);

        //Validate Subscription of the API
        HttpResponse subscribeApiResponse = apiStore.subscribe(subscriptionRequest);
        assertEquals(subscribeApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                apiNameTest3 + "is not Subscribed");
        assertTrue(subscribeApiResponse.getData().contains("\"error\" : false"),
                apiNameTest3 + "is not Subscribed");

        APPKeyRequestGenerator generateAppKeyRequestSandBox =
                new APPKeyRequestGenerator(applicationNameTest3);
        generateAppKeyRequestSandBox.setKeyType("SANDBOX");
        String responseSandBox = apiStore.generateApplicationKey
                (generateAppKeyRequestSandBox).getData();
        JSONObject jsonObject = new JSONObject(responseSandBox);

        String sandboxAccessToken =
                jsonObject.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + sandboxAccessToken);

        Thread.sleep(2000);

        HttpResponse sandBoxResponse = HttpRequestUtil.doGet(gatewayUrlsWrk.getWebAppURLNhttp() +
                apiContextTest3 + "/" + apiVersion, requestHeaders);
        assertEquals(sandBoxResponse.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                "Response code mismatched when api invocation from incorrect token");
        assertTrue(sandBoxResponse.getData().contains
                        ("Sandbox key offered to the API with no sandbox endpoint"),
                "API which is created only with Production endpoint can be " +
                        "invoke from SandBox Key");

    }

    @Test(groups = {"wso2.am"}, description = "Published an API only with valid Sandbox Endpoint" +
            " and invoke API from Production token")

    public void testInvokeAPIFromProductionTokenWhenProvideOnlySandboxEndPoint() throws Exception {

        String apiContextTest4 = "apim34SandBoxTest4API";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest4, apiContextTest4, apiVersion,
                        apiProviderName, null, new URL(apiSandBoxEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        resourceBeanList.add(new APIResourceBean("GET", "Application & Application User",
                "Unlimited", "name"));

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                apiNameTest4 + "is not created as expected");
        assertTrue(apiCreationResponse.getData().contains("\"error\" : false"),
                apiNameTest4 + "is not created as expected");

        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiNameTest4, apiProviderName,
                        APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        //Check the availability of API in Publisher and Store
        List<APIIdentifier> publisherAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiPublisher.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierPublisherTest4, publisherAPIList),
                "Published Api is visible in API Publisher.");

        List<APIIdentifier> storeAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiStore.getAllPublishedAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierPublisherTest4, storeAPIList),
                "Published Api is visible in API Store.");

        apiStore.addApplication(applicationNameTest4, "Gold", "", "");
        SubscriptionRequest subscriptionRequest =
                new SubscriptionRequest(apiNameTest4, apiProviderName);
        subscriptionRequest.setApplicationName(applicationNameTest4);

        //Validate Subscription of the API
        HttpResponse subscribeApiResponse = apiStore.subscribe(subscriptionRequest);
        assertEquals(subscribeApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                apiNameTest4 + "is not Subscribed");
        assertTrue(subscribeApiResponse.getData().contains("\"error\" : false"),
                apiNameTest4 + "is not Subscribed");

        APPKeyRequestGenerator generateAppKeyRequestSandBox =
                new APPKeyRequestGenerator(applicationNameTest4);
        String responseSandBox = apiStore.generateApplicationKey
                (generateAppKeyRequestSandBox).getData();
        JSONObject jsonObject = new JSONObject(responseSandBox);

        String sandboxAccessToken =
                jsonObject.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + sandboxAccessToken);
        requestHeaders.put("accept", "text/xml");

        Thread.sleep(2000);

        HttpResponse sandBoxResponse = HttpRequestUtil.doGet(gatewayUrlsWrk.getWebAppURLNhttp() +
                apiContextTest4 + "/" + apiVersion + "/name", requestHeaders);
        assertEquals(sandBoxResponse.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                "Response code mismatched when api invocation from incorrect token");
        assertTrue(sandBoxResponse.getData().contains("Production key offered to the API with" +
                        " no production endpoint"),
                "API which is created only with sandbox endpoint can be invoke from production Key");

    }

    @Test(groups = {"wso2.am"}, description = "Published an API with valid Production endpoint " +
            "and Sandbox endpoint then invoke API from Production and Sandbox token")
    public void testInvokeAPIFromSandboxAndProductionTokenWhenProvideBothEndPoints() throws
            Exception {

        String apiContextTest5 = "apim34SandBoxTest5API";
             String productionEndpointResponse = "<Customer><id>123</id><name>John</name></Customer>";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest5, apiContextTest5, apiVersion, apiProviderName,
                        new URL(apiProductionEndPointUrl), new URL(apiSandBoxEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        resourceBeanList.add(new APIResourceBean("GET", "Application & Application User",
                "Unlimited", "name"));

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                apiNameTest5 + "is not created as expected");
        assertTrue(apiCreationResponse.getData().contains("\"error\" : false"),
                apiNameTest5 + "is not created as expected");

        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiNameTest5, apiProviderName,
                        APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        //Check the availability of API in Publisher and Store
        List<APIIdentifier> publisherAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiPublisher.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierPublisherTest5, publisherAPIList),
                "Published Api is visible in API Publisher.");

        List<APIIdentifier> storeAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiStore.getAllPublishedAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierPublisherTest5, storeAPIList),
                "Published Api is visible in API Store.");

        apiStore.addApplication(applicationNameTest5, "Gold", "", "");
        SubscriptionRequest subscriptionRequest =
                new SubscriptionRequest(apiNameTest5, apiProviderName);
        subscriptionRequest.setApplicationName(applicationNameTest5);

        //Validate Subscription of the API
        HttpResponse subscribeApiResponse = apiStore.subscribe(subscriptionRequest);
        assertEquals(subscribeApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                apiNameTest5 + "is not Subscribed");
        assertTrue(subscribeApiResponse.getData().contains("\"error\" : false"),
                apiNameTest5 + "is not Subscribed");

        //Generate Production and Sandbox Key
        APPKeyRequestGenerator generateAppKeyRequestProduction =
                new APPKeyRequestGenerator(applicationNameTest5);
        APPKeyRequestGenerator generateAppKeyRequestSandBox =
                new APPKeyRequestGenerator(applicationNameTest5);
        generateAppKeyRequestSandBox.setKeyType("SANDBOX");

        String responseProduction = apiStore.generateApplicationKey
                (generateAppKeyRequestProduction).getData();
        JSONObject jsonObjectProduction = new JSONObject(responseProduction);

        String productionAccessToken =
                jsonObjectProduction.getJSONObject("data").getJSONObject("key").
                        get("accessToken").toString();
        Map<String, String> productionRequestHeaders = new HashMap<String, String>();
        productionRequestHeaders.put("Authorization", "Bearer " + productionAccessToken);
        productionRequestHeaders.put("accept", "text/xml");


        String responseSandBox = apiStore.generateApplicationKey
                (generateAppKeyRequestSandBox).getData();
        JSONObject jsonObjectSandBox = new JSONObject(responseSandBox);

        String sandboxAccessToken =
                jsonObjectSandBox.getJSONObject("data").getJSONObject("key").
                        get("accessToken").toString();
        Map<String, String> sandboxRequestHeaders = new HashMap<String, String>();
        sandboxRequestHeaders.put("Authorization", "Bearer " + sandboxAccessToken);

        Thread.sleep(2000);

        //Invoke API From Production Key
        HttpResponse productionResponse = HttpRequestUtil.doGet(gatewayUrlsWrk.getWebAppURLNhttp() +
                apiContextTest5 + "/" + apiVersion, productionRequestHeaders);
        assertEquals(productionResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation from Production Token");
        assertTrue(productionResponse.getData().contains
                (productionEndpointResponse), "Response data mismatched when api " +
                "invocation from Production Token");

        //Invoke API From SandBox Key
        HttpResponse sandBoxResponse = HttpRequestUtil.doGet(gatewayUrlsWrk.getWebAppURLNhttp() +
                apiContextTest5 + "/" + apiVersion + "/name", sandboxRequestHeaders);
        assertEquals(sandBoxResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation From SandBox Token");
        assertTrue(sandBoxResponse.getData().contains(sandboxEndpointResponse),
                "Response data mismatched when api invocation from Sanbox Token");
    }


    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        apiStore.removeAPISubscriptionByName
                (apiNameTest1, apiVersion, apiProviderName, applicationNameTest1);
        apiStore.removeAPISubscriptionByName
                (apiNameTest2, apiVersion, apiProviderName, applicationNameTest2);
        apiStore.removeAPISubscriptionByName
                (apiNameTest3, apiVersion, apiProviderName, applicationNameTest3);
        apiStore.removeAPISubscriptionByName
                (apiNameTest4, apiVersion, apiProviderName, applicationNameTest4);
        apiStore.removeAPISubscriptionByName
                (apiNameTest5, apiVersion, apiProviderName, applicationNameTest5);
        apiStore.removeApplication(applicationNameTest1);
        apiStore.removeApplication(applicationNameTest2);
        apiStore.removeApplication(applicationNameTest3);
        apiStore.removeApplication(applicationNameTest4);
        apiStore.removeApplication(applicationNameTest5);
        apiPublisher.deleteAPI(apiNameTest1, apiVersion, apiProviderName);
        apiPublisher.deleteAPI(apiNameTest2, apiVersion, apiProviderName);
        apiPublisher.deleteAPI(apiNameTest3, apiVersion, apiProviderName);
        apiPublisher.deleteAPI(apiNameTest4, apiVersion, apiProviderName);
        apiPublisher.deleteAPI(apiNameTest5, apiVersion, apiProviderName);
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
//                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

}


