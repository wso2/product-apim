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
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.restapi.RESTAPITestConstants;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Change the Auth type of the Resource and invoke the APi
 */
public class ChangeAuthTypeOfResourceTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME = "ChangeAuthTypeOfResourceTest999";
    private final String API_CONTEXT = "ChangeAuthTypeOfResource999";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String RESPONSE_GET = "<id>123</id><name>John</name></Customer>";
    private final String API_GET_ENDPOINT_METHOD = "customers/123";
    private String APPLICATION_NAME = "ChangeAuthTypeOfResourceTestCase";
    private String apiId;
    private String applicationId;
    private String apiEndPointUrl;
    private String providerName;
    private String consumerKey, consumerSecret;
    private HashMap<String, String> requestHeadersGet;
    private ArrayList<String> grantTypes = new ArrayList<>();
    private APIIdentifier apiIdentifier;


    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp()+ API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        requestHeadersGet = new HashMap<>();
        requestHeadersGet.put("accept", "text/xml");
        //Create publish and subscribe a API
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);

        //Create application
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN,
                ApplicationDTO.TokenTypeEnum.JWT);
        assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        applicationId = applicationResponse.getData();

        List<APIOperationsDTO> apiOperationsDTOS = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb(RESTAPITestConstants.GET_METHOD);
        apiOperationsDTO
                .setAuthType(APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType());
        apiOperationsDTO.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED);
        apiOperationsDTO.setTarget("/*");
        apiOperationsDTOS.add(apiOperationsDTO);

        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setProvider(providerName);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setOperationsDTOS(apiOperationsDTOS);
        apiRequest.setTags(API_TAGS);
        apiRequest.setDescription(API_DESCRIPTION);

        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);

    }


    @Test(groups = {"wso2.am"}, description = "Invoke a resource with auth type Application And Application User")
    public void testInvokeResourceWithAuthTypeApplicationAndApplicationUser() throws Exception {
        //generate keys for the subscription
        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(applicationId, "3600", "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                        null, grantTypes);

        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();
        assertNotNull(consumerKey, "Consumer Key not found");
        assertNotNull(consumerSecret, "Consumer Secret not found ");

        assertNotNull(applicationKeyDTO.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + applicationKeyDTO.getToken().getAccessToken());

        //Send GET request
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0, APIMIntegrationConstants.IS_API_EXISTS);
        HttpResponse httpResponseGet =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                "auth type Application & Application User");
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request for" +
                " auth type Application & Application User. Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponseGet.getData() + "\"");

    }


    @Test(groups = {"wso2.am"}, description = "Invoke a resource with auth type Application",
            dependsOnMethods = "testInvokeResourceWithAuthTypeApplicationAndApplicationUser")
    public void testInvokeResourceWithAuthTypeApplication() throws Exception {
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apiDto = g.fromJson(response.getData(), APIDTO.class);

        List<APIOperationsDTO> apiOperationsDTOS = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb(RESTAPITestConstants.GET_METHOD);
        apiOperationsDTO
                .setAuthType(APIMIntegrationConstants.ResourceAuthTypes.APPLICATION.getAuthType());
        apiOperationsDTO.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED);
        apiOperationsDTO.setTarget("/*");
        apiOperationsDTOS.add(apiOperationsDTO);

        apiDto.setOperations(apiOperationsDTOS);

        //Update API with Edited information
        APIDTO updateAPIHTTPResponse = restAPIPublisher.updateAPI(apiDto, apiId);
        assertTrue(StringUtils.isNotEmpty(updateAPIHTTPResponse.getId()),
                "Update API end point URL Response Code is invalid." + getAPIIdentifierString(apiIdentifier));
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();
        waitForAPIDeploymentSync(apiDto.getProvider(), apiDto.getName(), apiDto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        //Send GET request
        HttpResponse httpResponseGet =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" +  API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                "auth type Application");
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request for" +
                " auth type Application. Expected value :\"" + RESPONSE_GET + "\" not contains in response data:\"" +
                httpResponseGet.getData() + "\"");

    }


    @Test(groups = {"wso2.am"}, description = "Invoke a resource with auth type Application User",
            dependsOnMethods = "testInvokeResourceWithAuthTypeApplication")
    public void testInvokeGETResourceWithAuthTypeApplicationUser() throws Exception {
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apiDto = g.fromJson(response.getData(), APIDTO.class);

        List<APIOperationsDTO> apiOperationsDTOS = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb(RESTAPITestConstants.GET_METHOD);
        apiOperationsDTO
                .setAuthType(APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_USER.getAuthType());
        apiOperationsDTO.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED);
        apiOperationsDTO.setTarget("/*");
        apiOperationsDTOS.add(apiOperationsDTO);

        apiDto.setOperations(apiOperationsDTOS);


        //Update API with Edited information
        APIDTO updateAPIHTTPResponse = restAPIPublisher.updateAPI(apiDto, apiId);
        assertTrue(StringUtils.isNotEmpty(updateAPIHTTPResponse.getId()),
                "Update API end point URL Response Code is invalid." + getAPIIdentifierString(apiIdentifier));
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();
        waitForAPIDeploymentSync(apiDto.getProvider(), apiDto.getName(), apiDto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        //Generate User Access Key
        String requestBody = "grant_type=password&username=admin&password=admin&scope=PRODUCTION";
        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        HttpResponse firstResponse = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody,
                tokenEndpointURL);
        JSONObject firstAccessTokenGenerationResponse = new JSONObject(firstResponse.getData());
        //get an access token for the first time
        String accessToken = firstAccessTokenGenerationResponse.getString("access_token");

        requestHeadersGet.put("Authorization", "Bearer " + accessToken);
        //Send GET request
        HttpResponse httpResponseGet =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                "auth type Application User");
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request for" +
                " auth type Application User. Expected value :\"" + RESPONSE_GET + "\" not contains in response data:\"" +
                httpResponseGet.getData() + "\"");

    }

    @Test(groups = {"wso2.am"}, description = "Invoke a resource with auth type None",
            dependsOnMethods = "testInvokeGETResourceWithAuthTypeApplicationUser")
    public void testInvokeGETResourceWithAuthTypeNone() throws Exception {

        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apiDto = g.fromJson(response.getData(), APIDTO.class);

        List<APIOperationsDTO> apiOperationsDTOS = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb(RESTAPITestConstants.GET_METHOD);
        apiOperationsDTO
                .setAuthType(APIMIntegrationConstants.ResourceAuthTypes.NONE.getAuthType());
        apiOperationsDTO.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED);
        apiOperationsDTO.setTarget("/*");
        apiOperationsDTOS.add(apiOperationsDTO);

        apiDto.setOperations(apiOperationsDTOS);


        //Update API with Edited information
        APIDTO updateAPIHTTPResponse = restAPIPublisher.updateAPI(apiDto, apiId);
        assertTrue(StringUtils.isNotEmpty(updateAPIHTTPResponse.getId()),
                "Update API end point URL Response Code is invalid." + getAPIIdentifierString(apiIdentifier));
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();
        waitForAPIDeploymentSync(apiDto.getProvider(), apiDto.getName(), apiDto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        //Send GET request
        HttpResponse httpResponseGet =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" + API_GET_ENDPOINT_METHOD,
                                       requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                "auth type None");
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request for" +
                " auth type Non3. Expected value :\"" + RESPONSE_GET + "\" not contains in response data:\"" +
                httpResponseGet.getData() + "\"");
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }

}
