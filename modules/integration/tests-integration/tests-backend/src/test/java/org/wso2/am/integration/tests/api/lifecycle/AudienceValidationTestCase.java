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
public class AudienceValidationTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME = "AudienceValidationTest";
    private final String API_CONTEXT = "AudienceValidationTest";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
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

    }


    @Test(groups = {"wso2.am"}, description = "Invoke a resource with auth type Application And Application User")
    public void testInvokeApiWithoutAudienceValidation() throws Exception {
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
                "audience validation test when audience validation disabled");
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request for" +
                " audience validation test. Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponseGet.getData() + "\"");


    }


    @Test(groups = {"wso2.am"}, description = "Invoke a resource with auth type Application",
            dependsOnMethods = "testInvokeApiWithoutAudienceValidation")
    public void testInvokeApiWithAudienceValidation() throws Exception {
        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        List<String> audiences = new ArrayList<String>();
        audiences.add("Hello");
        apidto.setAudiences(audiences);

        restAPIPublisher.updateAPI(apidto);

        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();


        HttpResponse httpResponseGet =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" + API_GET_ENDPOINT_METHOD,
                                       requestHeadersGet);

        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_FORBIDDEN, "Invocation fails to forbid "
                + "token with invalid audience");
        assertTrue(httpResponseGet.getData().contains(AUTH_VALIDATION_ERROR_CODE), "Response do not contains expected"
                + " error code :\"" + AUTH_VALIDATION_ERROR_CODE + "\". The response received : \"" + httpResponseGet.getData() + "\"");


        audiences.add(consumerKey);
        apidto.setAudiences(audiences);

        restAPIPublisher.updateAPI(apidto);

        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();


        httpResponseGet = HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" + API_GET_ENDPOINT_METHOD,
                                                 requestHeadersGet);

        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                "audience validation test when audience validation enabled and valid audience is passed");
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request for" +
                " audience validation test. Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponseGet.getData() + "\"");
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }

}
