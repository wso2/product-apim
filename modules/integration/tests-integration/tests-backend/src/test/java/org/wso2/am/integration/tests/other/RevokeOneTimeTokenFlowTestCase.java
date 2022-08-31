/*
 *   Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.am.integration.tests.other;

import com.google.gson.Gson;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationPoliciesDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OperationPolicyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.apache.http.client.HttpClient;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;

/**
 * The test case for One Time Revocation flow. The test case will create an API and attach the revoke
 * one time token policy. Then will test with JWT tokens that has the same scope claim as policy attribute scope
 */
public class RevokeOneTimeTokenFlowTestCase extends APIManagerLifecycleBaseTest {

    private final String API_CONTEXT = "RevokeOneTimeTokenAPITest";
    private String applicationId;
    private String apiId;
    private String accessToken;
    private Map<String, String> policyMap;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {

        super.init();

        //Create the API
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application RevokeOneTimeToken", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();
        policyMap = restAPIPublisher.getAllCommonOperationPolicies();

        String API_END_POINT_POSTFIX_URL = "xmlapi";
        String apiEndPointUrl = getAPIInvocationURLHttp(API_END_POINT_POSTFIX_URL, API_VERSION_1_0_0);
        String API_NAME = "RevokeOneTimeTokenAPITest";
        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTags(API_TAGS);
        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        //Create the JWT access token with the scope OTT
        List<String> grantTypes = new ArrayList<>();
        grantTypes.add("client_credentials");
        ArrayList<String> scopes = new ArrayList<>();
        scopes.add("OTT");

        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(applicationId, "3600", null,
                        ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, scopes, grantTypes);
        assert applicationKeyDTO.getToken() != null;
        accessToken = applicationKeyDTO.getToken().getAccessToken();
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API before adding the revoke one time token policy")
    public void testAPIInvocationBeforeAddingRevokeOneTimePolicy() throws Exception {

        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI();
        assertEquals(invokeAPIResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK);
    }

    @Test(groups = {"wso2.am"}, description = "Attach the policy without defining the policy attribute")
    public void testRevokeOneTimePolicyAdditionWithMissingAttributes() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        //Attach the revoke one time token policy without defining the scope
        String policyName = "revokeOneTimeToken";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);
        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(getPolicyList(policyName, policyMap, null));
        assert apidto.getOperations() != null;
        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);

        HttpResponse updateResponse = restAPIPublisher.updateAPIWithHttpInfo(apidto);
        assertEquals(updateResponse.getResponseCode(), 500);
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the revoke one time policy")
    public void testAPIInvocationRevokeOneTimePolicy() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        //Attach the policy with scope attribute OTT which is the same as the JWT token
        String policyName = "revokeOneTimeToken";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("scope", "OTT");

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(getPolicyList(policyName, policyMap, attributeMap));

        assert apidto.getOperations() != null;
        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);
        restAPIPublisher.updateAPI(apidto);
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();

        //Invoking the API for the first time
        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI();
        assertEquals(invokeAPIResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK);

        //Wait for some time and invoke again with same token. Token should be revoked
        Thread.sleep(15000);
        org.apache.http.HttpResponse invokeAPIResponse2 = invokeAPI();
        assertEquals(invokeAPIResponse2.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_UNAUTHORIZED);
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {

        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }

    public List<OperationPolicyDTO> getPolicyList(String policyName, Map<String, String> policyMap,
                                                  Map<String, Object> attributeMap) {

        List<OperationPolicyDTO> policyList = new ArrayList<>();
        OperationPolicyDTO policyDTO = new OperationPolicyDTO();
        policyDTO.setPolicyName(policyName);
        policyDTO.setPolicyId(policyMap.get(policyName));
        policyDTO.setParameters(attributeMap);
        policyList.add(policyDTO);

        return policyList;
    }

    public org.apache.http.HttpResponse invokeAPI() throws XPathExpressionException, IOException {

        HttpClient client = HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier()).build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0));
        request.setHeader("Authorization", "Bearer " + accessToken);
        return client.execute(request);
    }
}
