/*
 *   Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.operationPolicy;

import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.wso2.am.integration.clients.publisher.api.v1.dto.OperationPolicyDataDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.apache.http.client.HttpClient;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class OperationPolicyTestCase extends APIManagerLifecycleBaseTest {

    private final Log log = LogFactory.getLog(OperationPolicyTestCase.class);

    private final String API_NAME = "AddNewPolicyAndInvokeAPITest";
    private final String API_CONTEXT = "AddNewPolicyAndInvokeAPI";
    private final String API_END_POINT_POSTFIX_URL = "xmlapi";

    private String applicationId;
    private String apiId;
    private String newVersionAPIId;
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

    @Test(groups = {"wso2.am"}, description = "Add common operation policy")
    public void testAddNewCommonOperationPolicy() throws Exception {

        HttpResponse addPolicyResponse = addPolicy(null, "customCommonLogPolicy.json", "customCommonLogPolicy.j2");

        assertNotNull(addPolicyResponse, "Error adding operation policy customCommonLogPolicy");
        assertEquals(addPolicyResponse.getResponseCode(), 201, "Response code mismatched");

        OperationPolicyDataDTO policyDTO =
                new Gson().fromJson(addPolicyResponse.getData(), OperationPolicyDataDTO.class);
        String newPolicyId = policyDTO.getId();
        assertNotNull(newPolicyId, "Policy Id is null");

        Map<String, String> updatedCommonPolicyMap = restAPIPublisher.getAllCommonOperationPolicies();
        Assert.assertNotNull(updatedCommonPolicyMap.get("customCommonLogPolicy"),
                "Unable to find the newly added common policy");
        policyMap.put("customCommonLogPolicy", newPolicyId);
    }

    @Test(groups = {"wso2.am"}, description = "Delete common operation policy")
    public void testDeleteCommonOperationPolicy() throws Exception {

        int responseCode = deleteOperationPolicy(policyMap.get("customCommonLogPolicy"), null);
        assertEquals(responseCode, 200);
        Map<String, String> updatedCommonPolicyMap = restAPIPublisher.getAllCommonOperationPolicies();
        Assert.assertNull(updatedCommonPolicyMap.get("customCommonLogPolicy"));
        policyMap.remove("customCommonLogPolicy");
    }

    @Test(groups = {"wso2.am"}, description = "Add API specific operation policy")
    public void testAddAPISpecificOperationPolicy() throws Exception {

        HttpResponse addPolicyResponse =
                addPolicy(apiId, "customAPISpecificLogPolicy.json", "customAPISpecificLogPolicy.j2");
        assertNotNull(addPolicyResponse, "Error adding operation policy customAPISpecificLogPolicy");
        assertEquals(addPolicyResponse.getResponseCode(), 201, "Response code mismatched");

        OperationPolicyDataDTO policyDTO =
                new Gson().fromJson(addPolicyResponse.getData(), OperationPolicyDataDTO.class);
        String newPolicyId = policyDTO.getId();
        assertNotNull(newPolicyId, "Policy Id is null");

        Map<String, String> apiSpecificPolicyMap = restAPIPublisher.getAllAPISpecificOperationPolicies(apiId);
        assertTrue(apiSpecificPolicyMap.size() > 0);
        policyMap.put("customAPISpecificLogPolicy", newPolicyId);
    }

    @Test(groups = {"wso2.am"}, description = "Add another API specific operation policy with same name",
            dependsOnMethods = "testAddAPISpecificOperationPolicy")
    public void testAddAPISpecificOperationPolicyWithSamePolicyName() throws Exception {

        try {
            HttpResponse addPolicyResponse =
                    addPolicy(apiId, "customAPISpecificLogPolicy.json", "customAPISpecificLogPolicy.j2");
            assertNotEquals(addPolicyResponse.getResponseCode(), 201);
        } catch (ApiException e) {
            log.error(e);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Delete API specific operation policy",
            dependsOnMethods = "testAddAPISpecificOperationPolicyWithSamePolicyName")
    public void testDeleteAPISpecificOperationPolicy() throws Exception {

        int responseCode = deleteOperationPolicy(policyMap.get("customAPISpecificLogPolicy"), apiId);
        assertEquals(responseCode, 200);
        Map<String, String> updatedAPISpecificPolicyMap = restAPIPublisher.getAllAPISpecificOperationPolicies(apiId);
        Assert.assertNull(updatedAPISpecificPolicyMap.get("customAPISpecificLogPolicy"));
        policyMap.remove("customAPISpecificLogPolicy");
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API before adding the log mediation")
    public void testAPIInvocationBeforeAddingNewOperationPolicy() throws Exception {

        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        assertEquals(invokeAPIResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");
        assertEquals(invokeAPIResponse.getHeaders("TestHeader").length, 0);
        assertEquals(invokeAPIResponse.getHeaders("Content-Type")[0].getValue(), "application/xml; charset=UTF-8");

    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the add header operation policy",
            dependsOnMethods = "testAPIInvocationBeforeAddingNewOperationPolicy")
    public void testAPIInvocationAfterAddingNewOperationPolicy() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        String policyName = "addHeader";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("headerName", "TestHeader");
        attributeMap.put("headerValue", "TestValue");

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(getPolicyList(policyName, policyMap, attributeMap));
        apiOperationPoliciesDTO.setResponse(getPolicyList(policyName, policyMap, attributeMap));

        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);
        restAPIPublisher.updateAPI(apidto);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();

        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(API_VERSION_1_0_0);
        assertEquals(invokeAPIResponse.getHeaders("TestHeader")[0].getValue(), "TestValue");
    }

    @Test(groups = {"wso2.am"}, description = "Validate the common operation policy clone at the update",
            dependsOnMethods = "testAPIInvocationAfterAddingNewOperationPolicy")
    public void testCommonOperationPolicyCloneToAPILevelWithUpdate() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);
        String clonedPolicyId = apidto.getOperations().get(0).getOperationPolicies().getRequest().get(0).getPolicyId();
        assertNotEquals(clonedPolicyId, policyMap.get("addHeader"));

        OperationPolicyDataDTO commonPolicy = restAPIPublisher.getCommonOperationPolicy(policyMap.get("addHeader"));
        OperationPolicyDataDTO clonedPolicy = restAPIPublisher.getAPISpecificOperationPolicy(clonedPolicyId, apiId);
        assertEquals(commonPolicy.getMd5(), clonedPolicy.getMd5());
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the add header operation policy",
            dependsOnMethods = "testCommonOperationPolicyCloneToAPILevelWithUpdate")
    public void testOperationPolicyAdditionWithMissingAttributes() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        String policyName = "removeHeader";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(getPolicyList(policyName, policyMap, null));
        apiOperationPoliciesDTO.setResponse(getPolicyList(policyName, policyMap, null));

        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);

        HttpResponse updateResponse = restAPIPublisher.updateAPIWithHttpInfo(apidto);
        assertEquals(updateResponse.getResponseCode(), 500);
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the add header operation policy",
            dependsOnMethods = "testCommonOperationPolicyCloneToAPILevelWithUpdate")
    public void testAddOperationPolicyForNotSupportedFlow() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        String policyName = "jsonFault";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(getPolicyList(policyName, policyMap, null));
        apiOperationPoliciesDTO.setResponse(getPolicyList(policyName, policyMap, null));

        apidto.getOperations().get(0).setOperationPolicies(apiOperationPoliciesDTO);

        HttpResponse updateResponse = restAPIPublisher.updateAPIWithHttpInfo(apidto);
        assertEquals(updateResponse.getResponseCode(), 500);
    }

    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the add header operation policy",
            dependsOnMethods = "testAPIInvocationAfterAddingNewOperationPolicy")
    public void testCreateNewVersionAfterAddingOperationPolicy() throws Exception {

        String newVersion = "2.0.0";
        HttpResponse newVersionResponse = restAPIPublisher.copyAPI(newVersion, apiId, null);
        assertEquals(newVersionResponse.getResponseCode(), HttpStatus.SC_OK, "Response Code Mismatch");
        newVersionAPIId = newVersionResponse.getData();

        HttpResponse getNewAPIResponse = restAPIPublisher.getAPI(newVersionAPIId);
        APIDTO apidto = new Gson().fromJson(getNewAPIResponse.getData(), APIDTO.class);

        Assert.assertNotNull(apidto.getOperations().get(0).getOperationPolicies(),
                "Unable to find a operation policies for the new version");
        Assert.assertNotNull(apidto.getOperations().get(0).getOperationPolicies().getRequest(),
                "Unable to find a operation policies for the new version request flow");
        String newVersionClonedPolicyId =
                apidto.getOperations().get(0).getOperationPolicies().getRequest().get(0).getPolicyId();

        assertNotEquals(newVersionClonedPolicyId, policyMap.get("addHeader"));
        OperationPolicyDataDTO clonedPolicy =
                restAPIPublisher.getAPISpecificOperationPolicy(newVersionClonedPolicyId, newVersionAPIId);
        assertNotNull(clonedPolicy);

        createAPIRevisionAndDeployUsingRest(newVersionAPIId, restAPIPublisher);
        HttpResponse apiLifecycleChangeResponse = restAPIPublisher.changeAPILifeCycleStatus(newVersionAPIId,
                APILifeCycleAction.PUBLISH.getAction(), null);
        assertEquals(apiLifecycleChangeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to change lifecycle stage to PUBLISHED for the new version " + newVersionAPIId);
        waitForAPIDeployment();
        subscribeToAPIUsingRest(newVersionAPIId, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED,
                restAPIStore);

        org.apache.http.HttpResponse invokeAPIResponse = invokeAPI(newVersion);
        assertEquals(invokeAPIResponse.getHeaders("TestHeader")[0].getValue(), "TestValue");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {

        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(newVersionAPIId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(newVersionAPIId);
    }

    public HttpResponse addPolicy(String apiId, String policySpecName, String policyDefinitionName)
            throws ApiException {

        String policySpecPath = getAMResourceLocation() + File.separator + "operationPolicy" +
                File.separator + policySpecName;

        String synapsePolicyDefPath = getAMResourceLocation() + File.separator + "operationPolicy" +
                File.separator + policyDefinitionName;

        File specification = new File(policySpecPath);
        File synapseDefinition = new File(synapsePolicyDefPath);

        HttpResponse addPolicyResponse;
        if (apiId == null) {
            addPolicyResponse = restAPIPublisher.addCommonOperationPolicy(specification, synapseDefinition, null);
        } else {
            addPolicyResponse =
                    restAPIPublisher.addAPISpecificOperationPolicy(apiId, specification, synapseDefinition, null);
        }
        return addPolicyResponse;
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
        org.apache.http.HttpResponse response = client.execute(request);

        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");

        return response;
    }
}
