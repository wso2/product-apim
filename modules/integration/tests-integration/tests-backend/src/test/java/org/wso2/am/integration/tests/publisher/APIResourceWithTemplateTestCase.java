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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.publisher;

import org.apache.commons.httpclient.HttpStatus;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class APIResourceWithTemplateTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME_DEFAULT = "APIResourceWithTemplateDef";
    private final String API_CONTEXT_DEFAULT = "APIResourceWithTemplateDef";
    private final String API_NAME = "APIResourceWithTemplate";
    private final String API_CONTEXT = "APIResourceWithTemplate";
    private final String TEMPLATE_API_NAME = "urlTemplateAPIName";
    private final String TEMPLATE_API_CONTEXT = "urlTemplateContext";
    private final String TEMPLATE_APP_NAME = "urlTemplateApp";

    private static final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private APIPublisherRestClient apiPublisherClientUser1;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private APIIdentifier apiIdentifier;

    @Factory(dataProvider = "userModeDataProvider")
    public APIResourceWithTemplateTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();

        String publisherURLHttp = getPublisherURLHttps();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);

        // Login to API Publisher with admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());
        String gatewaySessionCookie = createSession(gatewayContextMgt);
        //Load the back-end dummy API
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            loadSynapseConfigurationFromClasspath(
                    "artifacts" + File.separator + "AM" + File.separator + "synapseconfigs" + File.separator + "rest" +
                            File.separator + "APIResourceWithTemplateTestCaseAPI.xml", gatewayContextMgt,
                    gatewaySessionCookie);
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test API with resouce containing url template for default api")
    public void testAPIwithResourceTemplateForDefaultAPI() throws Exception {
        apiCreationRequestBean = new APICreationRequestBean(API_NAME_DEFAULT, API_CONTEXT_DEFAULT, API_VERSION_1_0_0,
                providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDefaultVersionChecked("default_version");
        List<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();

        APIResourceBean res = new APIResourceBean("GET", "Application & Application User", "Unlimited",
                "/resource/{param}");
        resourceBeanList.add(res);
        apiCreationRequestBean.setResourceBeanList(resourceBeanList);

        HttpResponse createAPIResponse = apiPublisherClientUser1.addAPI(apiCreationRequestBean);

        assertEquals(createAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API APIResourceWithTemplateDef creation failed");

        // publish the api
        apiIdentifier = new APIIdentifier(providerName, API_NAME_DEFAULT, API_VERSION_1_0_0);
        HttpResponse publishAPIResponse = publishAPI(apiIdentifier, apiPublisherClientUser1, false);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API APIResourceWithTemplateDef publish failed");
    }

    @Test(groups = { "wso2.am" }, description = "Test API with resouce containing url template")
    public void testAPIwithResourceTemplateForAPI() throws Exception {
        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        List<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();

        APIResourceBean res = new APIResourceBean("GET", "Application & Application User", "Unlimited",
                "/resource/{param}");
        resourceBeanList.add(res);
        apiCreationRequestBean.setResourceBeanList(resourceBeanList);

        HttpResponse createAPIResponse = apiPublisherClientUser1.addAPI(apiCreationRequestBean);

        assertEquals(createAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API APIResourceWithTemplateDef creation failed");

        // publish the api
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        HttpResponse publishAPIResponse = publishAPI(apiIdentifier, apiPublisherClientUser1, false);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API APIResourceWithTemplateDef publish failed");
    }

    @Test(groups = { "wso2.am" }, description = "Test API uri template default/encode behavior")
    public void testUriEncodingInTemplate() throws Exception {
        //according to the Test api - APIResourceWithTemplateTestCaseAPI.xml
        String uriTemplate = "/S2222-0496%2815%2927436-0";
        String context = "customcontextendpoint";

        APIStoreRestClient apiStore = new APIStoreRestClient(getStoreURLHttp());
        apiStore.login(user.getUserName(), user.getPassword());
        String endpointUrl = getGatewayURLNhttp() + context + "/sub{uri.var.urlcontext}";
        APIPublisherRestClient apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(user.getUserName(), user.getPassword());

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(TEMPLATE_API_NAME,
                TEMPLATE_API_CONTEXT, API_VERSION_1_0_0, user.getUserName(), new URL(endpointUrl));
        apiCreationRequestBean.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);

        //define resources
        ArrayList<APIResourceBean> resList = new ArrayList<APIResourceBean>();
        APIResourceBean res = new APIResourceBean(APIMIntegrationConstants.HTTP_VERB_GET,
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED, "{urlcontext}");
        resList.add(res);
        apiCreationRequestBean.setResourceBeanList(resList);

        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiCreationRequestBean);
        verifyResponse(serviceResponse);

        //add a application
        serviceResponse = apiStore
                .addApplication(TEMPLATE_APP_NAME, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "",
                        "this-is-test");
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(TEMPLATE_API_NAME, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

        waitForAPIDeploymentSync(user.getUserName(), TEMPLATE_API_NAME, API_VERSION_1_0_0,
                                 APIMIntegrationConstants.IS_API_EXISTS);

        //subscribe to the api
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(TEMPLATE_API_NAME, user.getUserName());
        subscriptionRequest.setApplicationName(TEMPLATE_APP_NAME);
        subscriptionRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);

        //generate the key for the subscription
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(TEMPLATE_APP_NAME);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Assert.assertNotNull("Access Token not found " + responseString, accessToken);

        String invokeURL = getAPIInvocationURLHttp(TEMPLATE_API_CONTEXT, API_VERSION_1_0_0) + uriTemplate;
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        serviceResponse = HTTPSClientUtils.doGet(invokeURL, requestHeaders);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        //update test api with endpoint for legacy-encoding
        URL endpoint = new URL(endpointUrl);
        JSONObject encodeEndpointUrl = new JSONObject(
                "{\"production_endpoints\":{\"url\":\"" + "legacy-encoding:" + endpoint
                        + "\",\"config\":null},\"endpoint_type\":\"" + endpoint.getProtocol() + "\"}");
        apiCreationRequestBean.setEndpoint(encodeEndpointUrl);

        serviceResponse = apiPublisher.updateAPI(apiCreationRequestBean);
        verifyResponse(serviceResponse);
        waitForAPIDeployment();
        //expected to hit backend "S2222-0496(15)27436-0" of decoded context "S2222-0496%2815%2927436-0"
        serviceResponse = HTTPSClientUtils.doGet(invokeURL, requestHeaders);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        super.cleanUp();
    }

}
