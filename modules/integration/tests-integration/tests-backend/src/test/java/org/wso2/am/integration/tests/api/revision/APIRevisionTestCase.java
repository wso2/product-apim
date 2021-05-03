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
import org.codehaus.plexus.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class APIRevisionTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(APIRevisionTestCase.class);
    protected static final int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_UNAUTHORIZED = Response.Status.UNAUTHORIZED.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE =
            Response.Status.SERVICE_UNAVAILABLE.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_TOO_MANY_REQUESTS = 429; // Define manually since value is not available in enum
    protected static final int HTTP_RESPONSE_CODE_FORBIDDEN = Response.Status.FORBIDDEN.getStatusCode();
    private final String API_NAME = "RevisionTestAPI";
    private final String API_CONTEXT = "revisiontestapi";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String APPLICATION_NAME = "RevisionTestApplication";
    private String apiEndPointUrl;
    private String apiId;
    private String revisionUUID;
    private String invalidApiId;
    private String invalidRevisionUUID;
    private String applicationId;
    private String accessToken;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;

    }

    @Test(groups = {"wso2.am"}, description = "API Revision create test case")
    public void testCreateAPIRevision() throws Exception {
        // Create the api creation request object
        APIRequest apiRequest;
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        // Add the API using the API Publisher.
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiResponse.getData();
        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Create API Response Code is invalid." + apiId);

        // Verify the API in API Publisher
        HttpResponse apiDto = restAPIPublisher.getAPI(apiResponse.getData());
        assertTrue(StringUtils.isNotEmpty(apiDto.getData()),
                "Added Api is not available in APi Publisher. API ID " + apiId);

        // Add the API Revision using the API Publisher
        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiId);
        apiRevisionRequest.setDescription("Test Revision 1");
        HttpResponse apiRevisionResponse = restAPIPublisher.addAPIRevision(apiRevisionRequest);
        assertEquals(apiRevisionResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Create API Response Code is invalid." + apiRevisionResponse.getData());
        JSONObject revisionResponseData = new JSONObject(apiRevisionResponse.getData());
        revisionUUID = revisionResponseData.getString("id");
    }

    @Test(groups = {"wso2.am"}, description = "Check the availability of API Revision in publisher before deploying.",
            dependsOnMethods = "testCreateAPIRevision")
    public void testGetAPIRevisions() throws Exception {
        HttpResponse apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId,null);
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve revisions" + apiRevisionsGetResponse.getData());

        List<JSONObject> revisionList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(apiRevisionsGetResponse.getData());
        JSONArray arrayList = jsonObject.getJSONArray("list");
        for (int i = 0, l = arrayList.length(); i < l; i++) {
            revisionList.add(arrayList.getJSONObject(i));
        }
        for (JSONObject revision :revisionList) {
            assertNotNull(revision.getString("id"), "Unable to retrieve revision UUID");
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test deploying API Revision to gateway environments",
            dependsOnMethods = "testGetAPIRevisions")
    public void testDeployAPIRevisions() throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionDeployResponse = restAPIPublisher.deployAPIRevision(apiId, revisionUUID,
                apiRevisionDeployRequestList);
        assertEquals(apiRevisionDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to deploy API Revisions:" + apiRevisionDeployResponse.getData());
//        List<JSONObject> deploymentList = new ArrayList<>();
//        JSONArray jsonArray = new JSONArray(apiRevisionDeployResponse.getData());
//
//        for (int i = 0, l = jsonArray.length(); i < l; i++) {
//            deploymentList.add(jsonArray.getJSONObject(i));
//        }
//        String deploymentName = null;
//        for (JSONObject deployment :deploymentList) {
//            deploymentName = deployment.getString("name");
//        }
//        assertNotNull(deploymentName, "Unable to retrieve deployed deployment name");
    }

    @Test(groups = {"wso2.am"}, description = "Test UnDeploying API Revision to gateway environments",
            dependsOnMethods = "testDeployAPIRevisions")
    public void testUnDeployAPIRevisions() throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionUnDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionUnDeployRequest.setVhost(null);
        apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
        HttpResponse apiRevisionsUnDeployResponse = restAPIPublisher.undeployAPIRevision(apiId, revisionUUID,
                apiRevisionUndeployRequestList);
        assertEquals(apiRevisionsUnDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to Undeploy API Revisions:" + apiRevisionsUnDeployResponse.getData());

    }

    @Test(groups = {"wso2.am"}, description = "Test restoring API using created API Revision",
            dependsOnMethods = "testUnDeployAPIRevisions")
    public void testRestoreAPIRevision() throws Exception {
        HttpResponse apiRevisionsRestoreResponse = restAPIPublisher.restoreAPIRevision(apiId, revisionUUID);
        assertEquals(apiRevisionsRestoreResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to restore API Revisions: " + apiRevisionsRestoreResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test restoring API Revision with invalid API UUID",
            dependsOnMethods = "testRestoreAPIRevision")
    public void testRestoreAPIRevisionWithInvalidAPIUUID() throws Exception {
        invalidApiId = "12345678-1234-5678-abcd-abcdefghijkl";
        HttpResponse apiRevisionsWithInvalidAPIUUIDRestoreResponse = restAPIPublisher
                .restoreAPIRevision(invalidApiId, revisionUUID);
        assertEquals(apiRevisionsWithInvalidAPIUUIDRestoreResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Unable to get API not found error: " + apiRevisionsWithInvalidAPIUUIDRestoreResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test restoring API Revision with invalid Revision UUID",
            dependsOnMethods = "testRestoreAPIRevisionWithInvalidAPIUUID")
    public void testRestoreAPIRevisionWithInvalidRevisionUUID() throws Exception {
        invalidRevisionUUID = "12345678-1234-5678-abcd-abcdefghijkl";
        HttpResponse apiRevisionsWithInvalidRevisionUUIDRestoreResponse = restAPIPublisher
                .restoreAPIRevision(apiId, invalidRevisionUUID);
        assertEquals(apiRevisionsWithInvalidRevisionUUIDRestoreResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Unable to get Revision not found error: " +
                        apiRevisionsWithInvalidRevisionUUIDRestoreResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test deleting API Revision",
            dependsOnMethods = "testRestoreAPIRevisionWithInvalidRevisionUUID")
    public void testDeleteAPIRevision() throws Exception {
        // Undeploy Revision
        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionUnDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionUnDeployRequest.setVhost(null);
        apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
        HttpResponse apiRevisionsUnDeployResponse = restAPIPublisher.undeployAPIRevision(apiId, revisionUUID,
                apiRevisionUndeployRequestList);
        assertEquals(apiRevisionsUnDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to undeploy API Revisions: " + apiRevisionsUnDeployResponse.getData());

        // Delete Revision
        HttpResponse apiRevisionDeleteResponse = restAPIPublisher.deleteAPIRevision(apiId, revisionUUID);
        assertEquals(apiRevisionDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to delete API Revisions: " + apiRevisionDeleteResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test deleting API Revision with invalid API UUID",
            dependsOnMethods = "testDeleteAPIRevision")
    public void testDeleteAPIRevisionWithInvalidAPIUUID() throws Exception {
        HttpResponse apiRevisionsWithInvalidAPIUUIDDeleteResponse = restAPIPublisher
                .deleteAPIRevision(invalidApiId, revisionUUID);
        assertEquals(apiRevisionsWithInvalidAPIUUIDDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Unable to get API not found error: " +
                        apiRevisionsWithInvalidAPIUUIDDeleteResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test deleting API Revision with invalid Revision UUID",
            dependsOnMethods = "testDeleteAPIRevisionWithInvalidAPIUUID")
    public void testDeleteAPIRevisionWithInvalidRevisionUUID() throws Exception {
        HttpResponse apiRevisionsWithInvalidRevisionUUIDDeleteResponse = restAPIPublisher
                .deleteAPIRevision(apiId, invalidRevisionUUID);
        assertEquals(apiRevisionsWithInvalidRevisionUUIDDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Unable to get Revision not found error: " +
                        apiRevisionsWithInvalidRevisionUUIDDeleteResponse.getData());
    }
//
//    @Test(groups = {"wso2.am"}, description = "Test deleting API Revision having deployments",
//            dependsOnMethods = "testDeleteAPIRevisionWithInvalidRevisionUUID")
//    public void testDeleteAPIRevisionHavingDeployments() throws Exception {
//        // Get revisionUUID of the un-deployed revision
//        HttpResponse apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId,null);
//        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
//                "Unable to retrieve revisions" + apiRevisionsGetResponse.getData());
//
//        JSONObject apiRevisionsGetResponseData = new JSONObject(apiRevisionsGetResponse.getData());
//        JSONArray revisionArray = apiRevisionsGetResponseData.getJSONArray("list");
//        for (int i = 0; i < revisionArray.length(); i++) {
//            revisionUUID = revisionArray.getJSONObject(i).getString("id");
//        }
//
//        // Deploy Revision
//        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
//        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
//        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
//        apiRevisionDeployRequest.setVhost("localhost");
//        apiRevisionDeployRequest.setDisplayOnDevportal(true);
//        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
//        HttpResponse apiRevisionDeployResponse = restAPIPublisher.deployAPIRevision(apiId, revisionUUID,
//                apiRevisionDeployRequestList);
//        assertEquals(apiRevisionDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
//                "Unable to deploy API Revisions: " +apiRevisionDeployResponse.getData());
//
//        // Delete deployed Revision
//        HttpResponse apiRevisionsHavingDeploymentsDeleteResponse = restAPIPublisher
//                .deleteAPIRevision(apiId, revisionUUID);
//        assertEquals(apiRevisionsHavingDeploymentsDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_BAD_REQUEST,
//                "Unable to get error for deleting revisions having deployments: " +
//                        apiRevisionsHavingDeploymentsDeleteResponse.getData());
//
//        // Undeploy Revision
//        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
//        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
//        apiRevisionUnDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
//        apiRevisionUnDeployRequest.setVhost(null);
//        apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
//        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
//        HttpResponse apiRevisionsUnDeployResponse = restAPIPublisher.undeployAPIRevision(apiId, revisionUUID,
//                apiRevisionUndeployRequestList);
//        assertEquals(apiRevisionsUnDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
//                "Unable to undeploy API Revisions: " + apiRevisionsUnDeployResponse.getData());
//
//        // Delete Revision
//        HttpResponse apiRevisionDeleteResponse = restAPIPublisher.deleteAPIRevision(apiId, revisionUUID);
//        assertEquals(apiRevisionDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
//                "Unable to delete API Revisions: " + apiRevisionDeleteResponse.getData());
//    }
    @Test(groups = {"wso2.am"}, description = "Test invoking API for a new Revision in CREATED lifecycle stage",
            dependsOnMethods = "testDeleteAPIRevisionWithInvalidRevisionUUID")
    public void testInvokeAPIInCreatedLifecycleStage() throws Exception {
        // Create a Revision
        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiId);
        apiRevisionRequest.setDescription("Test Revision for Lifecycle Changes Testing API");
        HttpResponse apiRevisionResponse = restAPIPublisher.addAPIRevision(apiRevisionRequest);
        assertEquals(apiRevisionResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to create API Revision: " + apiRevisionResponse.getData());
        JSONObject revisionResponseData = new JSONObject(apiRevisionResponse.getData());
        revisionUUID = revisionResponseData.getString("id");

        // Deploy the Revision
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionDeployResponse = restAPIPublisher.deployAPIRevision(apiId, revisionUUID,
                apiRevisionDeployRequestList);
        assertEquals(apiRevisionDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to deploy API Revision in CREATED stage: " + apiRevisionDeployResponse.getData());
        waitForAPIDeployment();

        // Invoke API using internal api key
        ApiResponse<APIKeyDTO> apiKeyDTO = restAPIPublisher.generateInternalApiKey(apiId);
        String apiKey = apiKeyDTO.getData().getApikey();
        Map<String, String> invokeAPIRequestHeaders = new HashMap<>();
        invokeAPIRequestHeaders.put("accept", "*/*");
        invokeAPIRequestHeaders.put("Internal-Key", apiKey);
        HttpResponse invokeAPIResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT,
                        API_VERSION_1_0_0) + API_END_POINT_METHOD, invokeAPIRequestHeaders);
        assertEquals(invokeAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to invoke API in CREATED stage using a test token");
    }

    @Test(groups = {"wso2.am"}, description = "Test invoking API for a new Revision in PUBLISHED lifecycle stage",
            dependsOnMethods = "testInvokeAPIInCreatedLifecycleStage")
    public void testInvokeAPIInPublishedLifecycleStage() throws Exception {
        // Change lifecycle stage from CREATED to PUBLISHED
        HttpResponse apiLifecycleChangeResponse = restAPIPublisher.changeAPILifeCycleStatus(apiId,
                APILifeCycleAction.PUBLISH.getAction(), null);
        assertEquals(apiLifecycleChangeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to change lifecycle stage to PUBLISHED: " + apiLifecycleChangeResponse.getData());

        // Create application and subscribe to API
        ApplicationDTO applicationDTO = restAPIStore.addApplication(APPLICATION_NAME,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
        applicationId = applicationDTO.getApplicationId();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        // Generate access token
        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();


        // Invoke API using application subscription token
        Map<String, String> invokeAPIRequestHeaders = new HashMap<String, String>();
        invokeAPIRequestHeaders.put("accept", "*/*");
        invokeAPIRequestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse invokeAPIResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT,
                API_VERSION_1_0_0) + API_END_POINT_METHOD, invokeAPIRequestHeaders);
        assertEquals(invokeAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to invoke API in PUBLISHED stage using application subscription token");
    }

    @Test(groups = {"wso2.am"}, description = "Test invoking API for a new Revision in BLOCKED lifecycle stage",
            dependsOnMethods = "testInvokeAPIInPublishedLifecycleStage")
    public void testInvokeAPIInBlockedLifecycleStage() throws Exception {
        // Change lifecycle stage from PUBLISHED to BLOCKED
        HttpResponse apiLifecycleChangeResponse = restAPIPublisher.changeAPILifeCycleStatus(apiId,
                APILifeCycleAction.BLOCK.getAction(), null);
        assertEquals(apiLifecycleChangeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to change lifecycle stage to BLOCKED: " + apiLifecycleChangeResponse.getData());

        // Invoke API using application subscription token
        Map<String, String> invokeAPIRequestHeaders = new HashMap<String, String>();
        invokeAPIRequestHeaders.put("accept", "*/*");
        invokeAPIRequestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse invokeAPIResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT,
                API_VERSION_1_0_0) + API_END_POINT_METHOD, invokeAPIRequestHeaders);
        assertEquals(invokeAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE,
                "Unable to get error for invoking API in BLOCKED stage using application subscription token");
    }

    @Test(groups = {"wso2.am"}, description = "Test invoking API for a new Revision in DEPRECATED lifecycle stage",
            dependsOnMethods = "testInvokeAPIInBlockedLifecycleStage")
    public void testInvokeAPIInDeprecatedLifecycleStage() throws Exception {
        // Change lifecycle stage from BLOCKED to DEPRECATED
        HttpResponse apiLifecycleChangeResponse = restAPIPublisher.changeAPILifeCycleStatus(apiId,
                APILifeCycleAction.DEPRECATE.getAction(), null);
        assertEquals(apiLifecycleChangeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to change lifecycle stage to DEPRECATED: " + apiLifecycleChangeResponse.getData());

        // Invoke API using application subscription token
        Map<String, String> invokeAPIRequestHeaders = new HashMap<String, String>();
        invokeAPIRequestHeaders.put("accept", "*/*");
        invokeAPIRequestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse invokeAPIResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT,
                API_VERSION_1_0_0) + API_END_POINT_METHOD, invokeAPIRequestHeaders);
        assertEquals(invokeAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to invoke API in DEPRECATED stage using application subscription token");
    }

    @Test(groups = {"wso2.am"}, description = "Test invoking API for a new Revision in RETIRED lifecycle stage",
            dependsOnMethods = "testInvokeAPIInDeprecatedLifecycleStage")
    public void testInvokeAPIInRetiredLifecycleStage() throws Exception {
        // Change lifecycle stage from DEPRECATED to RETIRED
        HttpResponse apiLifecycleChangeResponse = restAPIPublisher.changeAPILifeCycleStatus(apiId,
                APILifeCycleAction.RETIRE.getAction(), null);
        assertEquals(apiLifecycleChangeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to change lifecycle stage to RETIRED: " + apiLifecycleChangeResponse.getData());

        // Invoke API using application subscription token
        Map<String, String> invokeAPIRequestHeaders = new HashMap<>();
        invokeAPIRequestHeaders.put("accept", "*/*");
        invokeAPIRequestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse invokeAPIResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT,
                API_VERSION_1_0_0) + API_END_POINT_METHOD, invokeAPIRequestHeaders);
        assertEquals(invokeAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Unable to get error for invoking API in RETIRED stage using application subscription token");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

    }
}
