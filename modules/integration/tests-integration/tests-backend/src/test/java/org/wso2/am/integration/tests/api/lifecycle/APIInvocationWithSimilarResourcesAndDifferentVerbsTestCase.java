/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Change the Auth type of the Resource and invoke the APi
 */
    public class APIInvocationWithSimilarResourcesAndDifferentVerbsTestCase extends APIManagerLifecycleBaseTest {

    private static final String API_NAME = "MultiVerbSimilarResourceAPI";
    private static final String API_CONTEXT = "multiVerbSimilarResourceAPI";
    private static final String API_END_POINT_POSTFIX_URL = "multiVSR/";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String RESPONSE_GET = "<response><value>Received GET /comp/cartes*</value></response>";
    private static final String RESPONSE_POST = "<response><value>Received POST /comp/cartes/op*</value></response>";
    private static final String API_ENDPOINT_RESOURCE = "/comp/cartes/op/123";
    private String APPLICATION_NAME = "MultiVerbSimilarResourceApp";
    private String apiEndPointUrl;
    private String providerName;
    private APIIdentifier apiIdentifier;
    private String apiId;
    private String applicarionId;
    private Map<String, String> requestHeaders;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        apiEndPointUrl = getGatewayURLNhttp() + API_END_POINT_POSTFIX_URL;
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        //Load the back-end dummy API
    }

    @Test(groups = {"wso2.am"}, description = "Invoke all resources and verbs that are valid")
    public void testInvokeAllResources() throws Exception {
        //Create application
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME, " Description",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);
        applicarionId = applicationResponse.getData();

        //Create publish and subscribe a API
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);


        APIRequest apiRequest;
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb("GET");
        apiOperationsDTO1.setTarget("/comp/cartes*");
        apiOperationsDTO1.setAuthType("Application & Application User");
        apiOperationsDTO1.setThrottlingPolicy("Unlimited");

        APIOperationsDTO apiOperationsDTO2 = new APIOperationsDTO();
        apiOperationsDTO2.setVerb("POST");
        apiOperationsDTO2.setTarget("/comp/cartes/op*");
        apiOperationsDTO2.setAuthType("Application & Application User");
        apiOperationsDTO2.setThrottlingPolicy("Unlimited");


        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        operationsDTOS.add(apiOperationsDTO2);
        apiRequest.setOperationsDTOS(operationsDTOS);
        apiRequest.setVisibility("public");
        apiRequest.setDescription(API_DESCRIPTION);

        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicarionId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(applicarionId, "3600", "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null,
                        grantTypes);

        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + applicationKeyDTO.getToken().getAccessToken());
        //Send GET request
        HttpResponse httpResponseGet =
                HttpRequestUtil.doGet(getGatewayURLNhttp() + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_ENDPOINT_RESOURCE,
                        requestHeaders);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                API_ENDPOINT_RESOURCE);
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request for" +
                API_ENDPOINT_RESOURCE + " Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponseGet.getData() + "\"");

        //Send POST request
        HttpResponse httpResponsePost = HttpRequestUtil
                .doPost(new URL(getGatewayURLNhttp() + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_ENDPOINT_RESOURCE), "",
                        requestHeaders);
        assertEquals(httpResponsePost.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for POST request for " + API_ENDPOINT_RESOURCE);
        assertTrue(httpResponsePost.getData().contains(RESPONSE_POST), "Response Data not match for POST request for" +
                " auth type Application & Application User. Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponsePost.getData() + "\"");

    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        restAPIStore.deleteApplication(applicarionId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }

}
