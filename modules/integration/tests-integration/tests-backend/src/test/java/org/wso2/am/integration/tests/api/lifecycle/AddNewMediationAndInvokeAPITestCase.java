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

import com.google.gson.Gson;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MediationPolicyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Add new Log mediation to the in-flow and check the logs to verify the  added mediation is working.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class AddNewMediationAndInvokeAPITestCase extends APIManagerLifecycleBaseTest {
    private final String API_NAME = "AddNewMediationAndInvokeAPITest";
    private final String API_CONTEXT = "AddNewMediationAndInvokeAPI";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_END_POINT_POSTFIX_URL = "xmlapi";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "AddNewMediationAndInvokeAPI";
    private String accessToken;
    private String applicationId;
    private String apiId;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application AccessibilityOfBlockAPITestCase", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

    }


    @Test(groups = {"wso2.am"}, description = "Invoke the API before adding the log mediation")
    public void testAPIInvocationBeforeAddingNewMediation() throws Exception {

        //Create the api creation request object
        APIRequest apiRequest;
        String apiEndPointUrl = getAPIInvocationURLHttp(API_END_POINT_POSTFIX_URL, API_VERSION_1_0_0);
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTags(API_TAGS);

        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();

        HttpClient client = HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier()).build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0));
        request.setHeader("Authorization", "Bearer " + accessToken);
        org.apache.http.HttpResponse response = client.execute(request);

        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");

        assertEquals(response.getHeaders("Content-Type")[0].getValue(), "application/xml; charset=UTF-8");

    }


    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the log mediation",
            dependsOnMethods = "testAPIInvocationBeforeAddingNewMediation")
    public void testAPIInvocationAfterAddingNewMediation() throws Exception {

        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        MediationPolicyDTO mediationPolicyDTO = new MediationPolicyDTO();
        mediationPolicyDTO.setType("out");
        mediationPolicyDTO.setName("xml_to_json_out_message");

        List<MediationPolicyDTO> mediationPolicyDTOList = new ArrayList<>();
        mediationPolicyDTOList.add(mediationPolicyDTO);
        apidto.setMediationPolicies(mediationPolicyDTOList);
        restAPIPublisher.updateAPI(apidto);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();
        HttpClient client = HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier()).build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0));
        request.setHeader("Authorization", "Bearer " + accessToken);
        org.apache.http.HttpResponse response2 = client.execute(request);

        assertEquals(response2.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");

        assertEquals(response2.getHeaders("Content-Type")[0].getValue(), "application/json; charset=UTF-8");
    }


    @Test(groups = {"wso2.am"}, description = "IInvoke the API after removing the log mediation",
            dependsOnMethods = "testAPIInvocationAfterAddingNewMediation")
    public void testAPIInvocationBeforeRemovingNewMediation() throws Exception {

        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        List<MediationPolicyDTO> mediationPolicyDTOList = new ArrayList<>();
        apidto.setMediationPolicies(mediationPolicyDTOList);
        restAPIPublisher.updateAPI(apidto);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();
        //Send GET Request
        HttpClient client = HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier()).build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0));
        request.setHeader("Authorization", "Bearer " + accessToken);
        org.apache.http.HttpResponse response2 = client.execute(request);

        assertEquals(response2.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");

        assertEquals(response2.getHeaders("Content-Type")[0].getValue(), "application/xml; charset=UTF-8");
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException, ApiException, JSONException {
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);

    }


}
