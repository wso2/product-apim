/*
 * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.gatewayPolicy;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationPoliciesDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GatewayPolicyDeploymentDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GatewayPolicyMappingInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GatewayPolicyMappingsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OperationPolicyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class GatewayPolicyTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME = "GatewayPolicyApplicableAPITest";
    private final String API_CONTEXT = "gatewayPolicyApplicableAPITest";
    private final String API_END_POINT_POSTFIX_URL = "xmlapi";
    private String applicationId;
    private String apiId;
    String gatewayPolicyId;
    String newGatewayPolicyId;
    private String accessToken;
    private Map<String, String> policyMap;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {

        super.init();
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application AccessibilityOfBlockAPITestCase", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();
        policyMap = restAPIPublisher.getAllCommonOperationPolicies();

        String apiEndPointUrl = getAPIInvocationURLHttp(API_END_POINT_POSTFIX_URL, API_VERSION_1_0_0);
        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
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
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API before deploying a gateway policy")
    public void testAPIInvocationBeforeAddingNewGatewayPolicy() throws Exception {

        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        assertEquals(invokeAPIResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");
        assertEquals(invokeAPIResponse.getHeaders("TestHeader").length, 0);
        assertEquals(invokeAPIResponse.getHeaders("Content-Type")[0].getValue(), "application/xml; charset=UTF-8");

    }

    @Test(groups = {"wso2.am"}, description = "Add gateway policy",  dependsOnMethods = "testAPIInvocationBeforeAddingNewGatewayPolicy")
    public void testAddNewGatewayPolicy() {

        GatewayPolicyMappingsDTO policyMapping = new GatewayPolicyMappingsDTO();
        policyMapping.setDisplayName("Policy_Mapping");
        policyMapping.setDescription("Description about the policy mapping");
        String policyName = "addHeader";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("headerName", "TestHeader");
        attributeMap.put("headerValue", "TestValue");

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setResponse(getPolicyList(policyName, policyMap, attributeMap));
        policyMapping.setPolicyMapping(apiOperationPoliciesDTO);
        HttpResponse addPolicyResponse = restAPIPublisher.addGatewayPolicy(policyMapping);
        assertEquals(addPolicyResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Error while adding gateway policy");
        GatewayPolicyMappingInfoDTO policyMappingInfoDTO = new Gson().fromJson(addPolicyResponse.getData(),
                GatewayPolicyMappingInfoDTO.class);
        gatewayPolicyId = policyMappingInfoDTO.getId();
        assertNotNull(gatewayPolicyId, "Policy Id is null");
    }

    @Test(groups = {"wso2.am"}, description = "Deploy sample gateway policy", dependsOnMethods = "testAddNewGatewayPolicy")
    public void testDeployGatewayPolicy() {

        List<GatewayPolicyDeploymentDTO> gatewayPolicyDeploymentDTOList = new ArrayList<>();
        GatewayPolicyDeploymentDTO gatewayPolicyDeploymentDTO = new GatewayPolicyDeploymentDTO();
        gatewayPolicyDeploymentDTO.setGatewayLabel(Constants.GATEWAY_ENVIRONMENT);
        gatewayPolicyDeploymentDTO.setGatewayDeployment(true);
        gatewayPolicyDeploymentDTOList.add(gatewayPolicyDeploymentDTO);
        HttpResponse deployPolicyResponse = restAPIPublisher.deployGatewayPolicy(gatewayPolicyId,
                gatewayPolicyDeploymentDTOList);
        assertEquals(deployPolicyResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Error while deploying gateway policy");
        List<GatewayPolicyDeploymentDTO> policyMappingDeploymentDTOList = new Gson().fromJson(deployPolicyResponse.getData(),
                new TypeToken<List<GatewayPolicyDeploymentDTO>>() {}.getType());
        String policyMappingId = policyMappingDeploymentDTOList.get(0).getMappingUUID();
        assertEquals(policyMappingId, gatewayPolicyId, "Policy Id mismatch");
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after deploying the gateway policy", dependsOnMethods = "testDeployGatewayPolicy")
    public void testAPIInvocationAfterDeployingNewGatewayPolicy() throws Exception {

        int maxAttempts = 10;
        int currentAttempt = 0;
        boolean conditionMet = false;

        while (!conditionMet && currentAttempt < maxAttempts) {
            org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
            if (invokeAPIResponse.getStatusLine().getStatusCode() == HTTP_RESPONSE_CODE_OK) {
                if (invokeAPIResponse.getHeaders("TestHeader").length > 0 &&
                        invokeAPIResponse.getHeaders("TestHeader")[0].getValue().equals("TestValue")) {
                    conditionMet = true;
                    assertEquals(invokeAPIResponse.getHeaders("TestHeader")[0].getValue(), "TestValue");
                }
            }
            currentAttempt++;
            if (!conditionMet && currentAttempt < maxAttempts) {
                Thread.sleep(2000);
            }
        }
        if (!conditionMet) {
            Assert.fail("Header value was not set correctly after the gateway policy was deployed.");
        }
    }

    @Test(groups = {"wso2.am"}, description = "Update deployed gateway policy", dependsOnMethods = "testAPIInvocationAfterDeployingNewGatewayPolicy")
    public void testUpdateDeployedGatewayPolicy() throws Exception {
        GatewayPolicyMappingsDTO policyMapping = new GatewayPolicyMappingsDTO();
        policyMapping.setDisplayName("Policy_Mapping");
        policyMapping.setDescription("Description about the policy mapping");
        String policyName = "addHeader";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("headerName", "TestHeader");
        attributeMap.put("headerValue", "UpdatedTestValue");

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setResponse(getPolicyList(policyName, policyMap, attributeMap));
        policyMapping.setPolicyMapping(apiOperationPoliciesDTO);
        HttpResponse updatePolicyResponse = restAPIPublisher.updateGatewayPolicy(gatewayPolicyId, policyMapping);
        assertEquals(updatePolicyResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Error while updating gateway policy");

        int maxAttempts = 10;
        int currentAttempt = 0;
        boolean conditionMet = false;

        while (!conditionMet && currentAttempt < maxAttempts) {
            org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
            if (invokeAPIResponse.getStatusLine().getStatusCode() == HTTP_RESPONSE_CODE_OK) {
                if (invokeAPIResponse.getHeaders("TestHeader").length > 0 &&
                        invokeAPIResponse.getHeaders("TestHeader")[0].getValue().equals("UpdatedTestValue")) {
                    conditionMet = true;
                    assertEquals(invokeAPIResponse.getHeaders("TestHeader")[0].getValue(), "UpdatedTestValue");
                }
            }
            currentAttempt++;
            if (!conditionMet && currentAttempt < maxAttempts) {
                Thread.sleep(2000);
            }
        }
        if (!conditionMet) {
            Assert.fail("Header value was not set correctly after the gateway policy was updated.");
        }
    }

    @Test(groups = {"wso2.am"}, description = "Deploy another gateway policy to same gateway", dependsOnMethods = "testUpdateDeployedGatewayPolicy")
    public void testDeployAnotherGatewayPolicyInSameGateway() {
        GatewayPolicyMappingsDTO policyMapping = new GatewayPolicyMappingsDTO();
        policyMapping.setDisplayName("Policy_Mapping_1");
        policyMapping.setDescription("Description about the new policy mapping");
        String policyName = "addHeader";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("headerName", "TestHeader1");
        attributeMap.put("headerValue", "TestValue1");

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setResponse(getPolicyList(policyName, policyMap, attributeMap));
        policyMapping.setPolicyMapping(apiOperationPoliciesDTO);
        HttpResponse addPolicyResponse = restAPIPublisher.addGatewayPolicy(policyMapping);
        assertEquals(addPolicyResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Error while adding gateway policy");
        GatewayPolicyMappingInfoDTO policyMappingInfoDTO = new Gson().fromJson(addPolicyResponse.getData(),
                GatewayPolicyMappingInfoDTO.class);
        newGatewayPolicyId = policyMappingInfoDTO.getId();
        assertNotNull(newGatewayPolicyId, "Policy Id is null");

        List<GatewayPolicyDeploymentDTO> gatewayPolicyDeploymentDTOList = new ArrayList<>();
        GatewayPolicyDeploymentDTO gatewayPolicyDeploymentDTO = new GatewayPolicyDeploymentDTO();
        gatewayPolicyDeploymentDTO.setGatewayLabel(Constants.GATEWAY_ENVIRONMENT);
        gatewayPolicyDeploymentDTO.setGatewayDeployment(true);
        gatewayPolicyDeploymentDTOList.add(gatewayPolicyDeploymentDTO);
        HttpResponse deployPolicyResponse = restAPIPublisher.deployGatewayPolicy(newGatewayPolicyId,
                gatewayPolicyDeploymentDTOList);
        assertEquals(deployPolicyResponse.getResponseCode(), HTTP_RESPONSE_CODE_BAD_REQUEST,
                "Not allowed to deploy multiple gateway policies in a single gateway");
    }

    @Test(groups = {"wso2.am"}, description = "Delete active gateway deployments available gateway policy", dependsOnMethods = "testDeployAnotherGatewayPolicyInSameGateway")
    public void testDeployedGatewayPolicyDeletion() throws Exception {

        HttpResponse deletePolicyResponse = restAPIPublisher.deleteGatewayPolicy(gatewayPolicyId);
        assertEquals(deletePolicyResponse.getResponseCode(), HTTP_RESPONSE_PRECONDITION_FAILED,
                "Should not allow to delete active deployments available gateway policy");
        GatewayPolicyMappingsDTO getPolicyResponse = restAPIPublisher.getGatewayPolicy(gatewayPolicyId);
        assertNotNull(getPolicyResponse, "Gateway policy should not be deleted since it has active deployments");
    }

    @Test(groups = {"wso2.am"}, description = "Undeploy sample gateway policy", dependsOnMethods = "testDeployedGatewayPolicyDeletion")
    public void testUndeployGatewayPolicy() {

        List<GatewayPolicyDeploymentDTO> gatewayPolicyDeploymentDTOList = new ArrayList<>();
        GatewayPolicyDeploymentDTO gatewayPolicyDeploymentDTO = new GatewayPolicyDeploymentDTO();
        gatewayPolicyDeploymentDTO.setGatewayLabel(Constants.GATEWAY_ENVIRONMENT);
        gatewayPolicyDeploymentDTO.setGatewayDeployment(false);
        gatewayPolicyDeploymentDTOList.add(gatewayPolicyDeploymentDTO);
        HttpResponse deployPolicyResponse = restAPIPublisher.deployGatewayPolicy(gatewayPolicyId,
                gatewayPolicyDeploymentDTOList);
        assertEquals(deployPolicyResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Error while un-deploying gateway policy");
    }

    @Test(groups = {"wso2.am"}, description = "Delete active gateway deployments unavailable gateway policy", dependsOnMethods = "testUndeployGatewayPolicy")
    public void testGatewayPolicyDeletion() throws Exception {
        HttpResponse deletePolicyResponse = restAPIPublisher.deleteGatewayPolicy(gatewayPolicyId);
        assertEquals(deletePolicyResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Error while deleting gateway policy");
        GatewayPolicyMappingsDTO getPolicyResponse = restAPIPublisher.getGatewayPolicy(gatewayPolicyId);
        Assert.assertNull(getPolicyResponse.getId(),
                "Gateway policy is not deleted even though it does not have any active deployments");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {

        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteGatewayPolicy(newGatewayPolicyId);
    }

    public org.apache.http.HttpResponse invokeAPI(String version) throws XPathExpressionException, IOException {

        HttpClient client = HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier()).build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, version));
        request.setHeader("Authorization", "Bearer " + accessToken);
        org.apache.http.HttpResponse response = client.execute(request);

        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");

        return response;
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
}
