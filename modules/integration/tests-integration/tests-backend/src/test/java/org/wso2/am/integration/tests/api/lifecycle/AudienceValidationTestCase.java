/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.api.lifecycle;

import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.LifecycleStateDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.ApiProductTestHelper;
import org.wso2.am.integration.test.impl.ApiTestHelper;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.restapi.RESTAPITestConstants;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
//import static org.testng.Assert.*;

/**
 * Change the Auth type of the Resource and invoke the APi
 */
public class AudienceValidationTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME = "AudienceValidationTest";
    private final String API_CONTEXT = "AudienceValidationTest";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_PRODUCT_NAME = "AudienceValidationApiProduct";
    private final String API_PRODUCT_CONTEXT = "AudienceValidationApiProduct";
    private final String API_PRODUCT_VERSION_1_0_0 = "1.0.0";
    private final String RESPONSE_GET = "<id>123</id><name>John</name></Customer>";
    private final String AUTH_VALIDATION_ERROR_CODE = "900914";
    private final String API_GET_ENDPOINT_METHOD = "customers/123";
    private String APPLICATION_NAME = "AudienceValidationTest";
    private String apiId;
    private String applicationId;
    private String apiEndPointUrl;
    private String providerName;
    private String consumerKey, consumerSecret;
    private HashMap<String, String> requestHeadersGet;
    private ArrayList<String> grantTypes = new ArrayList<>();
    private APIIdentifier apiIdentifier;
    private ArrayList<String> audiences = new ArrayList<String>();
    private ApiProductTestHelper apiProductTestHelper;
    private HttpResponse getAPIResponse;
    private APIDTO apidto;
    private ApplicationKeyDTO applicationKeyDTO;
    private APIProductDTO apiProductDTO;
    private ApiTestHelper apiTestHelper;
    private String sandboxToken;
    private ApplicationKeyDTO sandboxAppKey;
    private String resourcePath;
    private static final String INTERNAL_ROLE_SUBSCRIBER = "Internal/subscriber";
    private static final String STANDARD_SUBSCRIBER = "standard_user";
    private static final String PASSWORD = "$3213#@sd";

    @Factory(dataProvider = "userModeDataProvider")
    public AudienceValidationTestCase(TestUserMode userMode) {

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
    public void initialize() throws Exception {
        super.init();
        resourcePath = TestConfigurationProvider.getResourceLocation() + File.separator + "oas" + File.separator + "v3"
                + File.separator + "api-product" + File.separator;
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                                                        createSession(keyManagerContext));
        apiTestHelper  = new ApiTestHelper(restAPIPublisher, restAPIStore, getAMResourceLocation(),
                                           keyManagerContext.getContextTenant().getDomain(), keyManagerHTTPSURL, user);
        apiProductTestHelper = new ApiProductTestHelper(restAPIPublisher, restAPIStore);

        if (userManagementClient.userNameExists(INTERNAL_ROLE_SUBSCRIBER, STANDARD_SUBSCRIBER)) {
            userManagementClient.deleteUser(STANDARD_SUBSCRIBER);
        }

        userManagementClient.addUser(STANDARD_SUBSCRIBER, PASSWORD,
                                     new String[]{INTERNAL_ROLE_SUBSCRIBER}, null);

        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp()+ API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        requestHeadersGet = new HashMap<>();
        requestHeadersGet.put("accept", "text/xml");

        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);

        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                                                                          "Test Application", APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN,
                                                                          ApplicationDTO.TokenTypeEnum.JWT);
        Assert.assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as "
                + "expected");

        applicationId = applicationResponse.getData();

        List<APIOperationsDTO> apiOperationsDTOS = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb(RESTAPITestConstants.GET_METHOD);
        apiOperationsDTO.setTarget("/*");
        apiOperationsDTOS.add(apiOperationsDTO);

        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setProvider(providerName);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setOperationsDTOS(apiOperationsDTOS);
        apiRequest.setDescription(API_DESCRIPTION);

        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                                                        APIMIntegrationConstants.API_TIER.UNLIMITED);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                                 APIMIntegrationConstants.IS_API_EXISTS);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);

        applicationKeyDTO = restAPIStore
                .generateKeys(applicationId, "3600", "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                              null, grantTypes);

        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();

        getAPIResponse = restAPIPublisher.getAPI(apiId);
        apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        List<APIDTO> apisToBeUsed = new ArrayList<>();
        apisToBeUsed.add(apidto);


        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        apiProductTestHelper = new ApiProductTestHelper(restAPIPublisher, restAPIStore);
        apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(user.getUserName(), API_PRODUCT_NAME,
                                                                         "/"+API_PRODUCT_CONTEXT,
                                                                         API_PRODUCT_VERSION_1_0_0,
                                                                         apisToBeUsed, policies);

        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        apiProductDTO = publishAPIProduct(apiProductDTO.getId());

        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO =
                apiProductTestHelper.verifyApiProductInPortal(apiProductDTO);


        apiTestHelper = new ApiTestHelper(restAPIPublisher, restAPIStore, getAMResourceLocation(),
                                          keyManagerContext.getContextTenant().getDomain(), keyManagerHTTPSURL, user);

        ApplicationDTO applicationDTO = apiTestHelper.verifySubscription(apiDTO, UUID.randomUUID().toString(),
                                                                         TIER_UNLIMITED);

        sandboxAppKey = apiTestHelper.verifyKeyGeneration(applicationDTO,
                                                          ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, new ArrayList<>(), grantTypes);

        sandboxToken = apiTestHelper.generateTokenPasswordGrant(sandboxAppKey.getConsumerKey(),
                                                                sandboxAppKey.getConsumerSecret(), STANDARD_SUBSCRIBER, PASSWORD,
                                                                Collections.emptyList());

        requestHeadersGet.put("Authorization", "Bearer " + sandboxToken);
    }


    @Test(groups = {"wso2.am"}, description = "Invoke a API without audience validation")
    public void testInvokeApiWithoutAudienceValidation() throws Exception {

        Assert.assertNotNull(consumerKey, "Consumer Key not found");
        Assert.assertNotNull(consumerSecret, "Consumer Secret not found ");

        Assert.assertNotNull(applicationKeyDTO.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + applicationKeyDTO.getToken().getAccessToken());

        //Send GET request
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0, APIMIntegrationConstants.IS_API_EXISTS);
        HttpResponse httpResponseGet =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" + API_GET_ENDPOINT_METHOD,
                                       requestHeadersGet);
        Assert.assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "API invocation fails  when audience " +
                "validation disabled");
        Assert.assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request for" +
                " audience validation test. Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponseGet.getData() + "\"");

        requestHeadersGet.put("Authorization", "Bearer " + sandboxToken);

        HttpResponse httpResponse = invokeWithGet(getAPIInvocationURLHttp(API_PRODUCT_CONTEXT, API_PRODUCT_VERSION_1_0_0)  + "/" + API_GET_ENDPOINT_METHOD,
                                                  requestHeadersGet);

        Assert.assertEquals(httpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "API product invocation fails  "
                + "when audience validation disabled");
        Assert.assertTrue(httpResponse.getData().contains(RESPONSE_GET), "Response Data not match for GET request for" +
                " audience validation test. Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponse.getData() + "\"");



    }


    @Test(groups = {"wso2.am"}, description = "Invoke a API with invalid audience",
            dependsOnMethods = "testInvokeApiWithoutAudienceValidation")
    public void testInvokeApiWithAudienceValidationFail() throws Exception {


        audiences.add("Hello");
        apidto.setAudiences(audiences);

        restAPIPublisher.updateAPI(apidto);

        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();

        requestHeadersGet.put("Authorization", "Bearer " + applicationKeyDTO.getToken().getAccessToken());

        HttpResponse httpResponseGet =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" + API_GET_ENDPOINT_METHOD,
                                       requestHeadersGet);

        Assert.assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_FORBIDDEN, "API invocation fails to forbid "
                + "token with invalid audience");
        Assert.assertTrue(httpResponseGet.getData().contains(AUTH_VALIDATION_ERROR_CODE), "Response do not contains expected"
                + " error code :\"" + AUTH_VALIDATION_ERROR_CODE + "\". The response received : \"" + httpResponseGet.getData() + "\"");


        apiProductDTO.setAudiences(audiences);
        restAPIPublisher.updateAPIProduct(apiProductDTO);

        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        requestHeadersGet.put("Authorization", "Bearer " + sandboxToken);

        HttpResponse httpResponse = invokeWithGet(getAPIInvocationURLHttp(API_PRODUCT_CONTEXT, API_PRODUCT_VERSION_1_0_0)  + "/" + API_GET_ENDPOINT_METHOD,
                                                  requestHeadersGet);

        Assert.assertEquals(httpResponse.getResponseCode(), HTTP_RESPONSE_CODE_FORBIDDEN, "API product invocation "
                + "fails to forbid token with invalid audience");
        Assert.assertTrue(httpResponse.getData().contains(AUTH_VALIDATION_ERROR_CODE), "Response do not contains expected"
                + " error code :\"" + AUTH_VALIDATION_ERROR_CODE + "\". The response received : \"" + httpResponse.getData() + "\"");

    }


    @Test(groups = {"wso2.am"}, description = "Invoke API with valid audience",
            dependsOnMethods = "testInvokeApiWithoutAudienceValidation")
    public void testInvokeApiWithAudienceValidationPass() throws Exception {

        audiences.add(consumerKey);
        apidto.setAudiences(audiences);

        restAPIPublisher.updateAPI(apidto);

        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();

        requestHeadersGet.put("Authorization", "Bearer " + applicationKeyDTO.getToken().getAccessToken());

        HttpResponse httpResponseGet =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" + API_GET_ENDPOINT_METHOD,
                                       requestHeadersGet);

        Assert.assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "API invocation fails when audience "
                + "validation enabled and valid audience is passed");
        Assert.assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request for" +
                " audience validation test. Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponseGet.getData() + "\"");

        audiences.add(sandboxAppKey.getConsumerKey());
        apiProductDTO.setAudiences(audiences);
        restAPIPublisher.updateAPIProduct(apiProductDTO);

        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        requestHeadersGet.put("Authorization", "Bearer " + sandboxToken);

        HttpResponse httpResponse = invokeWithGet(getAPIInvocationURLHttp(API_PRODUCT_CONTEXT, API_PRODUCT_VERSION_1_0_0)  + "/" + API_GET_ENDPOINT_METHOD,
                                                  requestHeadersGet);

        Assert.assertEquals(httpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "API product invocation fails when"
                + " audience validation enabled and valid audience is passed");
        Assert.assertTrue(httpResponse.getData().contains(RESPONSE_GET), "Response Data not match for GET request for" +
                " audience validation test. Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponse.getData() + "\"");


    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        undeployAndDeleteAPIProductRevisionsUsingRest(apiProductDTO.getId(), restAPIPublisher);
        userManagementClient.deleteUser(STANDARD_SUBSCRIBER);
    }

    private APIProductDTO publishAPIProduct(String uuid) throws ApiException, APIManagerIntegrationTestException {

        WorkflowResponseDTO workflowResponseDTO = apiProductTestHelper.changeLifecycleStateOfApiProduct(uuid,
                                                                                                        "Publish", null);
        Assert.assertNotNull(workflowResponseDTO);
        LifecycleStateDTO lifecycleStateDTO = workflowResponseDTO.getLifecycleState();
        Assert.assertNotNull(lifecycleStateDTO);
        Assert.assertEquals("APPROVED", workflowResponseDTO.getWorkflowStatus().getValue());
        assert APILifeCycleState.PUBLISHED.getState().equals(lifecycleStateDTO.getState());

        return restAPIPublisher.getApiProduct(uuid);
    }

    private HttpResponse invokeWithGet(String url, Map<String, String> headers) throws IOException {

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(url);
        headers.forEach(get::addHeader);
        org.apache.http.HttpResponse response = httpclient.execute(get);
        InputStream stream = response.getEntity().getContent();
        String content = IOUtils.toString(stream);
        Map<String, String> outputHeaders = new HashMap();
        for (Header header : response.getAllHeaders()) {
            outputHeaders.put(header.getName(), header.getValue());
        }
        return new HttpResponse(content, response.getStatusLine().getStatusCode(), outputHeaders);
    }

}