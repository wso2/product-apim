/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationPoliciesDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OperationPolicyDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OperationPolicyDataDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.impl.client.HttpClientBuilder;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Integration test for empty-body requests with content-aware API-level mediation policy for Json path.
 */
public class ContentAwareMediationPolicyEmptyBodyTestCase extends APIManagerLifecycleBaseTest {

    private final Log log = LogFactory.getLog(ContentAwareMediationPolicyEmptyBodyTestCase.class);

    private static final String API_NAME = "ContentAwareEmptyBodyAPITest";
    private static final String API_CONTEXT = "contentAwareEmptyBodyApi";
    private static final String API_END_POINT_POSTFIX_URL = "allMethodsBackend";
    private static final String APPLICATION_NAME = "ContentAwareEmptyBodyApp";
    private static final String POLICY_NAME = "contentAwarePropertyPolicy";
    private static final String POLICY_TYPE = "common";
    private static final String POLICY_VERSION = "v1";

    private static final String[] HTTP_METHODS = {"GET", "POST", "PUT", "PATCH", "DELETE"};

    private static final String RESOURCE_PATH = "/resource";

    private String applicationId;
    private String apiId;
    private String accessToken;
    private String contentAwarePolicyId;
    private CloseableHttpClient httpClient;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {

        super.init();
        String gatewaySessionCookie = createSession(gatewayContextMgt);

        loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM" + File.separator
                        + "synapseconfigs" + File.separator + "rest" + File.separator
                        + "all_methods_dummy_api.xml",
                gatewayContextMgt, gatewaySessionCookie);
        httpClient = HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier()).build();

        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Application for content-aware mediation policy empty body test",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        // Operations are set in testConfigureOperationsForAllVerbs (publisher update + new revision).
        // Endpoint: mock Synapse API AllMethods_Dummy_API (context /allMethodsBackend/1.0.0).
        String apiEndPointUrl = getAPIInvocationURLHttp(API_END_POINT_POSTFIX_URL, API_VERSION_1_0_0);
        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTags(API_TAGS);
        apiRequest.setOperationsDTOS(buildOperationsForAllVerbs());

        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();

        String policySpecPath = getAMResourceLocation() + File.separator + "operationPolicy" + File.separator
                + POLICY_NAME + ".json";
        String policyDefPath = getAMResourceLocation() + File.separator + "operationPolicy" + File.separator
                + POLICY_NAME + ".j2";
        File specification = new File(policySpecPath);
        File synapseDefinition = new File(policyDefPath);

        HttpResponse addPolicyResponse = restAPIPublisher.addAPISpecificOperationPolicy(apiId, specification,
                synapseDefinition, null);
        assertNotNull(addPolicyResponse, "Error adding operation policy " + POLICY_NAME);
        assertEquals(addPolicyResponse.getResponseCode(), 201, "Response code mismatched when adding the policy");

        OperationPolicyDataDTO policyData = new Gson().fromJson(addPolicyResponse.getData(),
                OperationPolicyDataDTO.class);
        contentAwarePolicyId = policyData.getId();
        assertNotNull(contentAwarePolicyId, "Policy Id is null after adding the content-aware policy");

        Map<String, String> apiSpecificPolicyMap = restAPIPublisher.getAllAPISpecificOperationPolicies(apiId);
        Assert.assertNotNull(apiSpecificPolicyMap.get(POLICY_NAME),
                "Unable to find the newly added API specific content-aware policy");

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        OperationPolicyDTO policyDTO = new OperationPolicyDTO();
        policyDTO.setPolicyName(POLICY_NAME);
        policyDTO.setPolicyType(POLICY_TYPE);
        policyDTO.setPolicyId(contentAwarePolicyId);
        policyDTO.setPolicyVersion(POLICY_VERSION);
        policyDTO.setParameters(new java.util.HashMap<>());

        List<OperationPolicyDTO> requestPolicies = new ArrayList<>();
        requestPolicies.add(policyDTO);

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(requestPolicies);
        apiOperationPoliciesDTO.setResponse(new ArrayList<>());
        apiOperationPoliciesDTO.setFault(new ArrayList<>());

        apidto.setOperations(buildOperationsForAllVerbs());
        apidto.setApiPolicies(apiOperationPoliciesDTO);

        APIDTO updated = restAPIPublisher.updateAPI(apidto);
        Assert.assertNotNull(updated, "API update failed while attaching api-level policy");
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();
    }

    @Test(groups = {"wso2.am"},
            description = "POST, PUT, PATCH, GET, DELETE with empty body + Content-Type application/json must succeed")
    public void testInvokeAllMethodsWithEmptyJsonBody() throws Exception {

        for (String verb : HTTP_METHODS) {
            org.apache.http.HttpResponse response = invokeWithEmptyJsonBody(verb);
            int statusCode = response.getStatusLine().getStatusCode();

            String responseBody = response.getEntity() != null
                    ? org.apache.http.util.EntityUtils.toString(response.getEntity()) : "";

            log.info("Verb=" + verb + " status=" + statusCode + " body=" + responseBody);

            Assert.assertFalse(responseBody.contains("Could not write JSON stream"),
                    "Verb " + verb + " produced 'Could not write JSON stream' error. Body: " + responseBody);
            Assert.assertFalse(responseBody.contains("Runtime Error"),
                    "Verb " + verb + " produced a Runtime Error. Body: " + responseBody);

            assertEquals(statusCode, HTTP_RESPONSE_CODE_OK,
                    "Verb " + verb + " did not return 200 OK when invoked with empty body and "
                            + "Content-Type: application/json against a content-aware mediator. Body: "
                            + responseBody);
        }
    }

    /**
     * Issues a request with {@code Content-Type: application/json} and an empty body.
     *
     * For POST/PUT/PATCH we intentionally do not attach an entity instance; this mirrors
     * clients that send the content-type header with zero payload bytes.
     */
    private org.apache.http.HttpResponse invokeWithEmptyJsonBody(String verb) throws Exception {

        String invocationUrl = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + RESOURCE_PATH;

        HttpRequestBase request;
        switch (verb) {
            case "GET":
                request = new HttpGet(invocationUrl);
                break;
            case "DELETE":
                request = new HttpDelete(invocationUrl);
                break;
            case "POST":
                request = new HttpPost(invocationUrl);
                break;
            case "PUT":
                request = new HttpPut(invocationUrl);
                break;
            case "PATCH":
                request = new HttpPatch(invocationUrl);
                break;
            default:
                throw new IllegalArgumentException("Unsupported verb: " + verb);
        }
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setHeader("Content-Type", "application/json");

        return httpClient.execute(request);
    }

    private List<APIOperationsDTO> buildOperationsForAllVerbs() {

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        for (String verb : HTTP_METHODS) {
            APIOperationsDTO op = new APIOperationsDTO();
            op.setVerb(verb);
            op.setTarget("/*");
            op.setAuthType("Application & Application User");
            op.setThrottlingPolicy("Unlimited");
            operationsDTOS.add(op);
        }
        return operationsDTOS;
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {

        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                log.warn("Unable to close HTTP client used for invocation requests.", e);
            }
        }
        if (applicationId != null) {
            restAPIStore.deleteApplication(applicationId);
        }
        if (apiId != null) {
            undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
            try {
                if (contentAwarePolicyId != null) {
                    restAPIPublisher.deleteAPISpecificPolicy(contentAwarePolicyId, apiId);
                }
            } catch (ApiException e) {
                log.warn("Unable to delete API specific content-aware policy. It may have been already removed.", e);
            }
            restAPIPublisher.deleteAPI(apiId);
        }
    }
}
