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
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.am.integration.test.utils.http.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.registry.resource.stub.common.xsd.ResourceData;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class APIRevisionTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(APIRevisionTestCase.class);
    protected static final int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE =
            Response.Status.SERVICE_UNAVAILABLE.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_INTERNAL_SERVER_ERROR =
            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    private final String API_CONTEXT = "revisiontestapi";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String INVALID_API_UUID = "2C0q51h4-621g-3163-7eip-as246v8x681m";
    private final String INVALID_REVISION_UUID = "4bm28320-l75v-3895-70ks-025294jd85a5";
    private final String INVALID_VHOST = "ws.wso2.com";
    private  String API_TRACES_LOCATION = "/_system/governance/apimgt/applicationdata/apis/";
    private String apiEndPointUrl;
    private String apiId;
    private String revisionUUID;
    private String accessToken;
    private ResourceAdminServiceClient resourceAdminServiceClient;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + "jaxrs_basic/services/customers/customerservice/";
    }

    @Test(groups = {"wso2.am"}, description = "API Revision create test case")
    public void testCreateAPIRevision() throws Exception {
        // Create the API creation request object
        APIRequest apiRequest;
        apiRequest = new APIRequest("RevisionTestAPI", API_CONTEXT, new URL(apiEndPointUrl));
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

        // Create the API Revision creation request object
        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiId);
        apiRevisionRequest.setDescription("Test Revision 1");

        // Add the API Revision using the API Publisher
        HttpResponse apiRevisionResponse = restAPIPublisher.addAPIRevision(apiRevisionRequest);
        assertEquals(apiRevisionResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Create API Response Code is invalid." + apiRevisionResponse.getData());
        JSONObject revisionResponseData = new JSONObject(apiRevisionResponse.getData());
        revisionUUID = revisionResponseData.getString("id");
    }

    @Test(groups = {"wso2.am"}, description = "API Revision create with invalid API UUID",
            dependsOnMethods = "testCreateAPIRevision")
    public void testCreateAPIRevisionWithInvalidAPI() throws Exception {
        // Create the API Revision creation request object
        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(INVALID_API_UUID);
        apiRevisionRequest.setDescription("Test Revision 2");

        // Add the API Revision using the API publisher.
        HttpResponse apiRevisionResponse = restAPIPublisher.addAPIRevision(apiRevisionRequest);
        assertEquals(apiRevisionResponse.getResponseCode(), HTTP_RESPONSE_CODE_INTERNAL_SERVER_ERROR,
                "Invalid response code for API Revision with invalid API.");
    }

    @Test(groups = {"wso2.am"}, description = "API Revision create without description",
            dependsOnMethods = "testCreateAPIRevisionWithInvalidAPI")
    public void testCreateAPIRevisionWithoutDescription() throws Exception {
        // Create the API Revision creation request object
        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiId);

        // Add the API Revision using the API Publisher.
        HttpResponse apiRevisionResponse = restAPIPublisher.addAPIRevision(apiRevisionRequest);
        assertEquals(apiRevisionResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Create API Response Code is invalid." + apiRevisionResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Check the availability of API Revision in publisher before deploying.",
            dependsOnMethods = "testCreateAPIRevisionWithoutDescription")
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

    @Test(groups = {"wso2.am"}, description = "Check the availability of API Revision in publisher after deploying.",
            dependsOnMethods = "testGetAPIRevisions")
    public void testGetDeployedAPIRevisions() throws Exception {
        HttpResponse apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId,"deployed:true");
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve deployed revisions" + apiRevisionsGetResponse.getData());
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
            dependsOnMethods = "testGetDeployedAPIRevisions")
    public void testDeployAPIRevision() throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIRevision(apiId, revisionUUID,
                apiRevisionDeployRequestList,"API");
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to deploy API Revisions:" +apiRevisionsDeployResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test deploying API Revision to gateway environments " +
            "with invalid API UUID", dependsOnMethods = "testDeployAPIRevision")
    public void testDeployAPIRevisionWithInvalidAPI() throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIRevision(INVALID_API_UUID, revisionUUID,
                apiRevisionDeployRequestList,"API");
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Invalid response code for deploying API Revision with invalid API UUID:"
                        + apiRevisionsDeployResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test deploying API Revision to gateway environments " +
            "with invalid Revision UUID", dependsOnMethods = "testDeployAPIRevisionWithInvalidAPI")
    public void testDeployAPIRevisionWithInvalidRevisionUUID() throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIRevision(apiId, INVALID_REVISION_UUID,
                apiRevisionDeployRequestList,"API");
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Invalid response code for deploying API Revision with invalid Revision UUID:"
                        + apiRevisionsDeployResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test deploying API Revision to gateway environments with " +
            "invalid deployment information", dependsOnMethods = "testDeployAPIRevisionWithInvalidRevisionUUID")
    public void testDeployAPIRevisionWithInvalidDeploymentInfo() throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName("us-region");
        apiRevisionDeployRequest.setVhost("gw.apim.com");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIRevision(apiId, revisionUUID,
                apiRevisionDeployRequestList,"API");
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_BAD_REQUEST,
                "Unable to deploy API Revisions:" + apiRevisionsDeployResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test deploying API Revision to gateway environments " +
            "with invalid vhost", dependsOnMethods = "testDeployAPIRevisionWithInvalidDeploymentInfo", enabled = false)
    public void testDeployAPIRevisionWithInvalidVhost() throws Exception {

        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost(INVALID_VHOST);
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIRevision(apiId, revisionUUID,
                apiRevisionDeployRequestList, "API");
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_BAD_REQUEST,
                "Invalid response code for deploying API Revision with invalid Vhost:"
                        + apiRevisionsDeployResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test UnDeploying API Revision to gateway environments",
            dependsOnMethods = "testDeployAPIRevisionWithInvalidDeploymentInfo")
    public void testUnDeployAPIRevision() throws Exception {
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

    @Test(groups = {"wso2.am"}, description = "Test UnDeploying API Revision with invalid API UUID to gateway environments",
            dependsOnMethods = "testUnDeployAPIRevision")
    public void testUnDeployAPIRevisionWithInvalidAPI() throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionUnDeployRequest.setName("us-region");
        apiRevisionUnDeployRequest.setVhost("gw.apim.com");
        apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
        HttpResponse apiRevisionsUnDeployResponse = restAPIPublisher.undeployAPIRevision(INVALID_API_UUID, revisionUUID,
                apiRevisionUndeployRequestList);
        assertEquals(apiRevisionsUnDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Invalid Response Code for Undeploy API Revisions with Invalid Deployment Information:"
                        + apiRevisionsUnDeployResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test UnDeploying API Revision with invalid Revision UUID to gateway environments",
            dependsOnMethods = "testUnDeployAPIRevisionWithInvalidAPI")
    public void testUnDeployAPIRevisionWithInvalidRevisionUUID() throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionUnDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionUnDeployRequest.setVhost(null);
        apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
        HttpResponse apiRevisionsUnDeployResponse = restAPIPublisher.undeployAPIRevision(apiId, INVALID_REVISION_UUID,
                apiRevisionUndeployRequestList);
        assertEquals(apiRevisionsUnDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Invalid Response Code for Undeploy API Revisions with Invalid Revision UUID:" +
                        apiRevisionsUnDeployResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test UnDeploying API Revision with invalid Deployment Information" +
            " to gateway environments",
            dependsOnMethods = "testUnDeployAPIRevisionWithInvalidRevisionUUID")
    public void testUnDeployAPIRevisionWithInvalidDeploymentInfo() throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionUnDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionUnDeployRequest.setVhost(null);
        apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
        HttpResponse apiRevisionsUnDeployResponse = restAPIPublisher.undeployAPIRevision(apiId, INVALID_REVISION_UUID,
                apiRevisionUndeployRequestList);
        assertEquals(apiRevisionsUnDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Invalid Response Code for Undeploy API Revisions with Invalid Revision UUID:" +
                        apiRevisionsUnDeployResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test restoring API using created API Revision",
            dependsOnMethods = "testUnDeployAPIRevisionWithInvalidDeploymentInfo")
    public void testRestoreAPIRevision() throws Exception {
        HttpResponse apiRevisionsRestoreResponse = restAPIPublisher.restoreAPIRevision(apiId, revisionUUID);
        assertEquals(apiRevisionsRestoreResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to restore API Revisions:" + apiRevisionsRestoreResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test restoring API Revision with invalid API UUID",
            dependsOnMethods = "testRestoreAPIRevision")
    public void testRestoreAPIRevisionWithInvalidAPIUUID() throws Exception {
        HttpResponse apiRevisionsWithInvalidAPIUUIDRestoreResponse = restAPIPublisher
                .restoreAPIRevision(INVALID_API_UUID, revisionUUID);
        assertEquals(apiRevisionsWithInvalidAPIUUIDRestoreResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Unable to get API not found error: " + apiRevisionsWithInvalidAPIUUIDRestoreResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test restoring API Revision with invalid Revision UUID",
            dependsOnMethods = "testRestoreAPIRevisionWithInvalidAPIUUID")
    public void testRestoreAPIRevisionWithInvalidRevisionUUID() throws Exception {
        HttpResponse apiRevisionsWithInvalidRevisionUUIDRestoreResponse = restAPIPublisher
                .restoreAPIRevision(apiId, INVALID_REVISION_UUID);
        assertEquals(apiRevisionsWithInvalidRevisionUUIDRestoreResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Unable to get Revision not found error: " +
                        apiRevisionsWithInvalidRevisionUUIDRestoreResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test deleting API Revision having deployments",
            dependsOnMethods = "testRestoreAPIRevisionWithInvalidRevisionUUID")
    public void testDeleteAPIRevisionHavingDeployments() throws Exception {
        // Deploy Revision
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionDeployResponse = restAPIPublisher.deployAPIRevision(apiId, revisionUUID,
                apiRevisionDeployRequestList,"API");
        assertEquals(apiRevisionDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to deploy API Revisions: " + apiRevisionDeployResponse.getData());
        waitForAPIDeployment();

        // Delete Revision
        HttpResponse apiRevisionsHavingDeploymentsDeleteResponse = restAPIPublisher
                .deleteAPIRevision(apiId, revisionUUID);
        assertEquals(apiRevisionsHavingDeploymentsDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_BAD_REQUEST,
                "Unable to get error for deleting revisions having deployments: " +
                        apiRevisionsHavingDeploymentsDeleteResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test deleting API Revision",
            dependsOnMethods = "testDeleteAPIRevisionHavingDeployments")
    public void testDeleteAPIRevision() throws Exception {
        // Undeploy Revision
        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionUnDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionUnDeployRequest.setVhost(null);
        apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
        HttpResponse apiRevisionUnDeployResponse = restAPIPublisher.undeployAPIRevision(apiId, revisionUUID,
                apiRevisionUndeployRequestList);
        assertEquals(apiRevisionUnDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to undeploy API Revisions: " + apiRevisionUnDeployResponse.getData());

        // Delete Revision
        HttpResponse apiRevisionDeleteResponse = restAPIPublisher.deleteAPIRevision(apiId, revisionUUID);
        assertEquals(apiRevisionDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to delete API Revisions: " + apiRevisionDeleteResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test deleting API Revision with invalid API UUID",
            dependsOnMethods = "testDeleteAPIRevision")
    public void testDeleteAPIRevisionWithInvalidAPIUUID() throws Exception {
        HttpResponse apiRevisionsWithInvalidAPIUUIDDeleteResponse = restAPIPublisher
                .deleteAPIRevision(INVALID_API_UUID, revisionUUID);
        assertEquals(apiRevisionsWithInvalidAPIUUIDDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Unable to get API not found error: " +
                        apiRevisionsWithInvalidAPIUUIDDeleteResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test deleting API Revision with invalid Revision UUID",
            dependsOnMethods = "testDeleteAPIRevisionWithInvalidAPIUUID")
    public void testDeleteAPIRevisionWithInvalidRevisionUUID() throws Exception {
        HttpResponse apiRevisionsWithInvalidRevisionUUIDDeleteResponse = restAPIPublisher
                .deleteAPIRevision(apiId, INVALID_REVISION_UUID);
        assertEquals(apiRevisionsWithInvalidRevisionUUIDDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Unable to get Revision not found error: " +
                        apiRevisionsWithInvalidRevisionUUIDDeleteResponse.getData());
    }

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
                apiRevisionDeployRequestList, "API");
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
        waitForAPIDeployment();

        // Create application and subscribe to API
        ApplicationDTO applicationDTO = restAPIStore.addApplication("RevisionTestApplication",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
        String applicationId = applicationDTO.getApplicationId();
        restAPIStore.subscribeToAPI(apiId, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        // Generate access token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add("client_credentials");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = Objects.requireNonNull(applicationKeyDTO.getToken()).getAccessToken();
        assertNotNull(accessToken, "Unable to get application subscription access token");

        // Invoke API using application subscription token
        Map<String, String> invokeAPIRequestHeaders = new HashMap<>();
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
        waitForAPIDeployment();

        // Invoke API using application subscription token
        Map<String, String> invokeAPIRequestHeaders = new HashMap<>();
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
        waitForAPIDeployment();

        // Invoke API using application subscription token
        Map<String, String> invokeAPIRequestHeaders = new HashMap<>();
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
        waitForAPIDeployment();

        // Invoke API using application subscription token
        Map<String, String> invokeAPIRequestHeaders = new HashMap<>();
        invokeAPIRequestHeaders.put("accept", "*/*");
        invokeAPIRequestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse invokeAPIResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT,
                API_VERSION_1_0_0) + API_END_POINT_METHOD, invokeAPIRequestHeaders);
        assertEquals(invokeAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Unable to get error for invoking API in RETIRED stage using application subscription token");
    }

    @Test(groups = {"wso2.am"}, description = "Test traces of the deleted API wont appear in admin console",
            dependsOnMethods = "testInvokeAPIInRetiredLifecycleStage")
    public void testIfTracesOfDeletedApisVisible() throws Exception {
        API_TRACES_LOCATION = API_TRACES_LOCATION.concat(apiId);
        resourceAdminServiceClient =
                new ResourceAdminServiceClient(publisherContext.getContextUrls().getBackEndUrl(),
                        createSession(publisherContext));
        ResourceData[] apiResourcesTraces = resourceAdminServiceClient.getResourceData(API_TRACES_LOCATION);
        assertTrue(apiId.equals(apiResourcesTraces[0].getName()));
        restAPIPublisher.deleteAPI(apiId);
        try {
            resourceAdminServiceClient.getResourceData(API_TRACES_LOCATION);
            Assert.fail("The resource should not be accessible");
        } catch (org.apache.axis2.AxisFault e) {
            assertTrue(e.getMessage().contains("Resource does not exist at path"));
        }
    }
}
