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
package org.wso2.am.integration.tests.operationPolicy;

import com.google.gson.Gson;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationPoliciesDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OperationPolicyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
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

public class JWTClaimBasedAccessValidatorPolicyTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME = "GatewayPolicyApplicableAPITest";
    private final String API_CONTEXT = "gatewayPolicyApplicableAPITest";
    private final String API_END_POINT_POSTFIX_URL = "xmlapi";
    private String applicationId;
    private String apiId;
    String newGatewayPolicyId;
    private String accessToken;
    private Map<String, String> policyMap;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {

        super.init();
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application AccessibilityOfBlockAPITestCase",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);
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

    @Test(groups = {"wso2.am"}, description = "Invoke the API before adding the policy")
    public void testAPIInvocationBeforeAddingJWTClaimBasedAccessValidationPolicy() throws Exception {

        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        assertEquals(invokeAPIResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");
        assertEquals(invokeAPIResponse.getHeaders("TestHeader").length, 0);
        assertEquals(invokeAPIResponse.getHeaders("Content-Type")[0].getValue(), "application/xml; charset=UTF-8");

    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the JWT claim based access validation policy",
            dependsOnMethods = "testAPIInvocationBeforeAddingJWTClaimBasedAccessValidationPolicy")
    public void testAPIInvocationAfterAddingJWTClaimBasedAccessValidationPolicy() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        String policyName = "jwtClaimBasedAccessValidator";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("accessVerificationClaim", "aut");
        attributeMap.put("accessVerificationClaimValue", "APPLICATION");

        List<OperationPolicyDTO> opList = getPolicyList(policyName, policyMap, attributeMap);
        opList.get(0).setPolicyVersion("v1");

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(opList);

        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);
        restAPIPublisher.updateAPI(apidto);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();

        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        assertEquals(invokeAPIResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK);

        // Clean up artifacts and reinitialize for the next test case.
        cleanUpArtifactsAndReInitialize();
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the JWT claim based access validation policy with invalid claim name",
            dependsOnMethods = "testAPIInvocationAfterAddingJWTClaimBasedAccessValidationPolicy")
    public void testAPIInvocationAfterAddingJWTClaimBasedAccessValidationPolicyWithInvalidClaimName() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        String policyName = "jwtClaimBasedAccessValidator";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("accessVerificationClaim", "claim");
        attributeMap.put("accessVerificationClaimValue", "APPLICATION");

        List<OperationPolicyDTO> opList = getPolicyList(policyName, policyMap, attributeMap);
        opList.get(0).setPolicyVersion("v1");

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(opList);

        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);
        restAPIPublisher.updateAPI(apidto);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();

        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        assertEquals(invokeAPIResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_FORBIDDEN);

        // Clean up artifacts and reinitialize for the next test case.
        cleanUpArtifactsAndReInitialize();
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the JWT claim based access validation policy with invalid claim value",
            dependsOnMethods = "testAPIInvocationAfterAddingJWTClaimBasedAccessValidationPolicyWithInvalidClaimName")
    public void testAPIInvocationAfterAddingJWTClaimBasedAccessValidationPolicyWithInvalidClaimValue() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        String policyName = "jwtClaimBasedAccessValidator";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("accessVerificationClaim", "aut");
        attributeMap.put("accessVerificationClaimValue", "INVALID");

        List<OperationPolicyDTO> opList = getPolicyList(policyName, policyMap, attributeMap);
        opList.get(0).setPolicyVersion("v1");

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(opList);

        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);
        restAPIPublisher.updateAPI(apidto);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();

        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        assertEquals(invokeAPIResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_FORBIDDEN);

        // Clean up artifacts and reinitialize for the next test case.
        cleanUpArtifactsAndReInitialize();
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the JWT claim based access validation policy with a valid regex",
            dependsOnMethods = "testAPIInvocationAfterAddingJWTClaimBasedAccessValidationPolicyWithInvalidClaimValue")
    public void testAPIInvocationAfterAddingJWTClaimBasedAccessValidationPolicyWithValidRegex() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        String policyName = "jwtClaimBasedAccessValidator";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("accessVerificationClaim", "aut");
        attributeMap.put("accessVerificationClaimValue", "APPLICATION");
        // Set a test regex which only supports lower case alphabetical values.
        attributeMap.put("accessVerificationClaimValueRegex", "^[A-Z]+$");

        List<OperationPolicyDTO> opList = getPolicyList(policyName, policyMap, attributeMap);
        opList.get(0).setPolicyVersion("v1");

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(opList);

        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);
        restAPIPublisher.updateAPI(apidto);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();

        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        assertEquals(invokeAPIResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK);

        // Clean up artifacts and reinitialize for the next test case.
        cleanUpArtifactsAndReInitialize();
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the JWT claim based access validation policy with an invalid regex",
            dependsOnMethods = "testAPIInvocationAfterAddingJWTClaimBasedAccessValidationPolicyWithValidRegex")
    public void testAPIInvocationAfterAddingJWTClaimBasedAccessValidationPolicyWithInvalidRegex() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        String policyName = "jwtClaimBasedAccessValidator";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("accessVerificationClaim", "aut");
        attributeMap.put("accessVerificationClaimValue", "APPLICATION");
        // Set a test regex which only supports numbers.
        attributeMap.put("accessVerificationClaimValueRegex", "^[0-9]+$");

        List<OperationPolicyDTO> opList = getPolicyList(policyName, policyMap, attributeMap);
        opList.get(0).setPolicyVersion("v1");

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(opList);

        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);
        restAPIPublisher.updateAPI(apidto);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();

        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        assertEquals(invokeAPIResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_FORBIDDEN);

        // Clean up artifacts and reinitialize for the next test case.
        cleanUpArtifactsAndReInitialize();
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the JWT claim based access validation policy with validation inverted",
            dependsOnMethods = "testAPIInvocationAfterAddingJWTClaimBasedAccessValidationPolicyWithInvalidRegex")
    public void testAPIInvocationAfterAddingJWTClaimBasedAccessValidationPolicyWithInvertedValidation() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        String policyName = "jwtClaimBasedAccessValidator";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("accessVerificationClaim", "aut");
        attributeMap.put("accessVerificationClaimValue", "NON_MATCHING");
        // Set a test regex which only supports numbers.
        attributeMap.put("shouldAllowValidation", "true");

        List<OperationPolicyDTO> opList = getPolicyList(policyName, policyMap, attributeMap);
        opList.get(0).setPolicyVersion("v1");

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(opList);

        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);
        restAPIPublisher.updateAPI(apidto);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();

        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        assertEquals(invokeAPIResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK);
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {

        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteGatewayPolicy(newGatewayPolicyId);
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

    public org.apache.http.HttpResponse invokeAPI(String version) throws XPathExpressionException, IOException {

        HttpClient client = HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier()).build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, version));
        request.setHeader("Authorization", "Bearer " + accessToken);
        return client.execute(request);
    }

    public int deleteOperationPolicy(String policyId, String apiId) throws ApiException {

        HttpResponse deletePolicyResponse;
        if (apiId == null) {
            deletePolicyResponse = restAPIPublisher.deleteCommonOperationPolicy(policyId);
        } else {
            deletePolicyResponse = restAPIPublisher.deleteAPISpecificPolicy(policyId, apiId);
        }
        return deletePolicyResponse.getResponseCode();
    }

    public void cleanUpArtifactsAndReInitialize() throws Exception {

        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteGatewayPolicy(newGatewayPolicyId);
        initialize();
    }
}
