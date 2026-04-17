/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.api.revision;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.ApiProductTestHelper;
import org.wso2.am.integration.test.impl.ApiTestHelper;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

public class APIProductRevisionTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(APIProductRevisionTestCase.class);
    protected static final int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
    private String apiId;
    private String revisionUUID;
    private ApiTestHelper apiTestHelper;
    private ApiProductTestHelper apiProductTestHelper;
    protected static final String TIER_UNLIMITED = "Unlimited";
    protected static final String TIER_GOLD = "Gold";
    List<APIDTO> apisToBeUsed = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        apiTestHelper = new ApiTestHelper(restAPIPublisher, restAPIStore, getAMResourceLocation(),
                keyManagerContext.getContextTenant().getDomain(), keyManagerHTTPSURL, user);
        apiProductTestHelper = new ApiProductTestHelper(restAPIPublisher, restAPIStore);

    }

    @Test(groups = {"wso2.am"}, description = "API Product Revision create test case")
    public void testAddingAPIProductRevision() throws Exception {
        // Pre-Conditions : Create APIs

        apisToBeUsed.add(apiTestHelper.
                createApiOne(getBackendEndServiceEndPointHttp("wildcard/resources")));
        apisToBeUsed.add(apiTestHelper.
                createApiTwo(getBackendEndServiceEndPointHttp("wildcard/resources")));

        // Step 1 : Create APIProduct
        final String provider = UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID().toString();
        final String version = "1.0.0";

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context, version,
                apisToBeUsed, policies);
        apiId = apiProductDTO.getId();

        waitForAPIDeployment();

        // Step 2 : Verify created APIProduct in publisher
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiId);
        apiRevisionRequest.setDescription("Test Revision 1");
        //Add the API Revision using the API publisher.
        HttpResponse apiRevisionResponse = restAPIPublisher.addAPIProductRevision(apiRevisionRequest);

        assertEquals(apiRevisionResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Create API Response Code is invalid." + apiRevisionResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Check the availability of API Product Revision in publisher before deploying.",
            dependsOnMethods = "testAddingAPIProductRevision")
    public void testGetAPIProductRevisions() throws Exception {
        HttpResponse apiRevisionsGetResponse = restAPIPublisher.getAPIProductRevisions(apiId, null);
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve revisions" + apiRevisionsGetResponse.getData());
        List<JSONObject> revisionList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(apiRevisionsGetResponse.getData());

        JSONArray arrayList = jsonObject.getJSONArray("list");
        for (int i = 0, l = arrayList.length(); i < l; i++) {
            revisionList.add(arrayList.getJSONObject(i));
        }
        for (JSONObject revision : revisionList) {
            revisionUUID = revision.getString("id");
        }
        assertNotNull(revisionUUID, "Unable to retrieve revision UUID");
    }

    @Test(groups = {"wso2.am"}, description = "Test deploying API Product Revision to gateway environments",
            dependsOnMethods = "testGetAPIProductRevisions")
    public void testDeployAPIProductRevisions() throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIProductRevision(apiId, revisionUUID,
                apiRevisionDeployRequestList, "APIProduct");
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to deploy API Product Revisions:" + apiRevisionsDeployResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test UnDeploying API Product Revision to gateway environments",
            dependsOnMethods = "testDeployAPIProductRevisions")
    public void testUnDeployAPIProductRevisions() throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionUnDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionUnDeployRequest.setVhost(null);
        apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
        HttpResponse apiRevisionsUnDeployResponse = restAPIPublisher.undeployAPIProductRevision(apiId, revisionUUID,
                apiRevisionUndeployRequestList);
        assertEquals(apiRevisionsUnDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to Undeploy API Product Revisions:" + apiRevisionsUnDeployResponse.getData());

    }

    @Test(groups = {"wso2.am"}, description = "Test restoring API when the restoring revision does not have" +
            " one or more resources in associated Product")
    public void testRestoreAPIWithDeletedResourcesInProduct() throws Exception {
        // Create an API Revision
        APIDTO apiOne = apiTestHelper.createApiOne(getBackendEndServiceEndPointHttp("order"));

        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiOne.getId());
        apiRevisionRequest.setDescription("Test Revision 1");

        HttpResponse apiRevisionResponse = restAPIPublisher.addAPIRevision(apiRevisionRequest);
        assertEquals(apiRevisionResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Create API Response Code is invalid." + apiRevisionResponse.getData());
        JSONObject revisionResponseData = new JSONObject(apiRevisionResponse.getData());
        String revision1UUID = revisionResponseData.getString("id");

        // Update API with new resources
        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb("GET");
        apiOperationsDTO1.setTarget("/missing-resource");
        apiOperationsDTO1.setAuthType("Application & Application User");
        apiOperationsDTO1.setThrottlingPolicy("Unlimited");

        APIOperationsDTO apiOperationsDTO2 = new APIOperationsDTO();
        apiOperationsDTO2.setVerb("POST");
        apiOperationsDTO2.setTarget("/missing-resource");
        apiOperationsDTO2.setAuthType("Application & Application User");
        apiOperationsDTO2.setThrottlingPolicy("Unlimited");

        APIOperationsDTO apiOperationsDTO3 = new APIOperationsDTO();
        apiOperationsDTO3.setVerb("GET");
        apiOperationsDTO3.setTarget("/missing-resource/{id}");
        apiOperationsDTO3.setAuthType("Application & Application User");
        apiOperationsDTO3.setThrottlingPolicy("Unlimited");

        List<APIOperationsDTO> operationsDTOs2 = new ArrayList<>();
        operationsDTOs2.add(apiOperationsDTO1);
        operationsDTOs2.add(apiOperationsDTO2);
        operationsDTOs2.add(apiOperationsDTO3);
        apiOne.setOperations(operationsDTOs2);
        restAPIPublisher.updateAPI(apiOne, apiOne.getId());

        // Create another API revision
        APIRevisionRequest apiRevisionRequest2 = new APIRevisionRequest();
        apiRevisionRequest2.setApiUUID(apiOne.getId());
        apiRevisionRequest2.setDescription("Test Revision 2");

        HttpResponse apiRevisionResponse2 = restAPIPublisher.addAPIRevision(apiRevisionRequest2);
        assertEquals(apiRevisionResponse2.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Create API Response Code is invalid." + apiRevisionResponse2.getData());
        JSONObject revisionResponseData2 = new JSONObject(apiRevisionResponse2.getData());
        String revision2UUID = revisionResponseData2.getString("id");

        // Create API Product with new resources
        final String provider = user.getUserName();
        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID().toString();
        final String version = "1.0.0";
        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);
        List<APIDTO> apisToBeUsed = new ArrayList<>();
        apisToBeUsed.add(apiOne);
        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context, version,
                apisToBeUsed, policies);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();
        // Attempt restoring API with revision 2 that contains the new resources
        HttpResponse apiRevisionsRestoreResponse = restAPIPublisher.restoreAPIRevision(apiOne.getId(), revision2UUID);
        assertEquals(apiRevisionsRestoreResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Failed to restore API Revision:" + apiRevisionsRestoreResponse.getData());
        // Attempt restoring API with revision 1 that does not contain the new resources
        HttpResponse apiRevisionsRestoreResponse2 = restAPIPublisher.restoreAPIRevision(apiOne.getId(), revision1UUID);
        assertEquals(apiRevisionsRestoreResponse2.getResponseCode(), HTTP_RESPONSE_CODE_BAD_REQUEST,
                "Underlying API Revision restore is expected to fail. Able to restore API Revision successfully:"
                        + apiRevisionsRestoreResponse2.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test restoring API Product using created API Product Revision",
            dependsOnMethods = "testUnDeployAPIProductRevisions")
    public void testRestoreAPIProductRevision() throws Exception {
        HttpResponse apiRevisionsRestoreResponse = restAPIPublisher.restoreAPIProductRevision(apiId, revisionUUID);
        assertEquals(apiRevisionsRestoreResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to resotre API Revisions:" + apiRevisionsRestoreResponse.getData());

        APIProductDTO restoredAPIProduct = restAPIPublisher.getApiProduct(apiId);
        assertFalse(restoredAPIProduct.getApis().isEmpty(), "API Product's APIs list is empty after API Product "
                + "restore: " + restoredAPIProduct);
    }

    @Test(groups = {"wso2.am"}, description = "Test deleting API using created API Revision",
            dependsOnMethods = "testRestoreAPIProductRevision")
    public void testDeleteAPIProductRevision() throws Exception {
        HttpResponse apiRevisionsDeleteResponse = restAPIPublisher.deleteAPIProductRevision(apiId, revisionUUID);
        assertEquals(apiRevisionsDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to delete API Product Revisions:" + apiRevisionsDeleteResponse.getData());
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
        for (APIDTO api : apisToBeUsed) {
            restAPIPublisher.deleteAPI(api.getId());
        }
    }
}
