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
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

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
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String apiId1;
    private String apiId2;
    private String apiId3;
    private String appId;

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
        String gatewaySessionCookie = createSession(gatewayContextMgt);
        //Load the back-end dummy API
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test API with resouce containing url template for default api")
    public void testAPIwithResourceTemplateForDefaultAPI() throws Exception {
        apiCreationRequestBean = new APICreationRequestBean(API_NAME_DEFAULT, API_CONTEXT_DEFAULT, API_VERSION_1_0_0,
                        providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDefaultVersionChecked("default_version");
        List<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();

        APIResourceBean res = new APIResourceBean("GET", "Application & Application " +
                "User", "Unlimited", "/resource/{param}");
        resourceBeanList.add(res);
        apiCreationRequestBean.setResourceBeanList(resourceBeanList);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setDefaultVersion("default_version");
        APIDTO apiCreationResponse = restAPIPublisher.addAPI(apiCreationRequestBean);
        apiId1 = apiCreationResponse.getId();

        HttpResponse createdApiResponse = restAPIPublisher.getAPI(apiId1);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
                API_NAME_DEFAULT + " API creation is failed");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId1, restAPIPublisher);

        HttpResponse publishAPIResponse = publishAPI(apiCreationResponse.getId(), restAPIPublisher, false);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API APIResourceWithTemplateDef publish failed");
    }

    @Test(groups = { "wso2.am" }, description = "Test API with resource containing url template")
    public void testAPIwithResourceTemplateForAPI() throws Exception {
        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        List<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();
        APIResourceBean res = new APIResourceBean("GET", "Application & Application User",
                "Unlimited", "/resource/{param}");
        resourceBeanList.add(res);
        apiCreationRequestBean.setResourceBeanList(resourceBeanList);

        APIDTO createAPIResponse = restAPIPublisher.addAPI(apiCreationRequestBean);
        apiId2 = createAPIResponse.getId();

        HttpResponse createdApiResponse = restAPIPublisher.getAPI(apiId2);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
                API_NAME_DEFAULT + " API creation is failed");

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId2, restAPIPublisher);

        // publish the api
        HttpResponse publishAPIResponse = publishAPI(apiId2, restAPIPublisher,
                false);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API APIResourceWithTemplateDef publish failed");
    }

    @Test(groups = { "wso2.am" }, description = "Test API uri template default/encode behavior")
    public void testUriEncodingInTemplate() throws Exception {
        //according to the Test api - APIResourceWithTemplateTestCaseAPI.xml
        String uriTemplate = "/S2222-0496%2815%2927436-0";
        String context = "customcontextendpoint";
        String endpointUrl = getGatewayURLNhttp() + context + "/sub{uri.var.urlcontext}";

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(TEMPLATE_API_NAME,
                TEMPLATE_API_CONTEXT, API_VERSION_1_0_0, user.getUserName(), new URL(endpointUrl));
        apiCreationRequestBean.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);

        //define resources
        ArrayList<APIResourceBean> resList = new ArrayList<APIResourceBean>();
        APIResourceBean res = new APIResourceBean(APIMIntegrationConstants.HTTP_VERB_GET,
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType(),
                // "/" should be append to url template in rest api level if it is not there.
                APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED, "/{urlcontext}");
        resList.add(res);
        apiCreationRequestBean.setResourceBeanList(resList);

        //add test api
        APIDTO createAPIResponse = restAPIPublisher.addAPI(apiCreationRequestBean);
        apiId3 = createAPIResponse.getId();

        //add a application
        ApplicationDTO createApplication = restAPIStore
                .addApplication(TEMPLATE_APP_NAME, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "",
                        "this-is-test");
        appId = createApplication.getApplicationId();

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId3, restAPIPublisher);

        // publish api
        HttpResponse publishAPIResponse = publishAPI(apiId3, restAPIPublisher, false);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API " + TEMPLATE_API_NAME + " publish failed");

        waitForAPIDeploymentSync(user.getUserName(), TEMPLATE_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);

        // subscribe to api
        restAPIStore.subscribeToAPI(apiId3, appId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        // generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(appId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        // Invoke api
        String invokeURL = getAPIInvocationURLHttp(TEMPLATE_API_CONTEXT, API_VERSION_1_0_0) + uriTemplate;
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        HttpResponse serviceResponse = HTTPSClientUtils.doGet(invokeURL, requestHeaders);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        //update test api with endpoint for legacy-encoding
        URL endpoint = new URL(endpointUrl);
        JSONParser parser = new JSONParser();
        String endPointString = "{\n" +
                "  \"production_endpoints\": {\n" +
                "    \"template_not_supported\": false,\n" +
                "    \"config\": null,\n" +
                "    \"url\": \"" + endpointUrl + "\"\n" +
                "  \"legacy-encoding\": \"" + endpoint + "\"\n" +
                "  },\n" +
                "  \"sandbox_endpoints\": {\n" +
                "    \"url\": \"" + endpointUrl + "\",\n" +
                "    \"config\": null,\n" +
                "    \"template_not_supported\": false\n" +
                "  \"legacy-encoding\": \"" + endpoint + "\"\n" +
                "  },\n" +
                "  \"endpoint_type\": \"http\"\n" +
                "}";

        Object jsonObject = parser.parse(endPointString);
        createAPIResponse.setEndpointConfig(jsonObject);
        restAPIPublisher.updateAPI(createAPIResponse);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId3, restAPIPublisher);
        waitForAPIDeployment();
        //expected to hit backend "S2222-0496(15)27436-0" of decoded context "S2222-0496%2815%2927436-0"
        serviceResponse = HTTPSClientUtils.doGet(invokeURL, requestHeaders);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        restAPIStore.deleteApplication(appId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId1, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(apiId2, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(apiId3, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId1);
        restAPIPublisher.deleteAPI(apiId2);
        restAPIPublisher.deleteAPI(apiId3);
    }
}
