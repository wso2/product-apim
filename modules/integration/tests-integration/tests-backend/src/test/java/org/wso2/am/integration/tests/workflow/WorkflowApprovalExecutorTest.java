/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.wso2.am.integration.tests.workflow;

import java.io.File;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.testng.annotations.*;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.api.dto.WorkflowDTO;
import org.wso2.am.integration.clients.admin.api.dto.WorkflowInfoDTO;
import org.wso2.am.integration.clients.admin.api.dto.WorkflowListDTO;

import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.ApiProductTestHelper;
import org.wso2.am.integration.test.impl.ApiTestHelper;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.UserManagementUtils;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.AdminDashboardRestClient;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;

import org.wso2.carbon.apimgt.api.WorkflowStatus;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class WorkflowApprovalExecutorTest extends APIManagerLifecycleBaseTest {

    private UserManagementClient userManagementClient = null;
    private String originalWFExtentionsXML;
    private String newWFExtentionsXML;
    private String USER_SMITH = "smith";
    private String ADMIN_ROLE = "admin";
    private String USER_ADMIN = "jackson";
    private String userName;
    private final String INTERNAL_ROLE_SUBSCRIBER = "Internal/subscriber";
    private ResourceAdminServiceClient resourceAdminServiceClient;
    private final String ALLOWED_ROLE = "admin";
    private static final String SUBSCRIBER_ROLE = "subscriber";
    private final String[] ADMIN_PERMISSIONS = { "/permission/admin/login", "/permission/admin/manage",
            "/permission/admin/configure", "/permission/admin/monitor" };
    private final String[] NEW_ROLE_LIST = { "Internal/publisher", "Internal/creator",
            "Internal/subscriber", "Internal/everyone", "admin" };
    private final String APIM_CONFIG_XML = "api-manager.xml";
    private final String DEFAULT_WF_EXTENTIONS_XML_REG_CONFIG_LOCATION =
            "/_system/governance/apimgt/applicationdata/workflow-extensions.xml";
    private RestAPIAdminImpl restAPIAdminUser;
    private static final Log log = LogFactory.getLog(WorkflowApprovalExecutorTest.class);
    private APIIdentifier apiIdentifier;
    private  AdminDashboardRestClient adminDashboardRestClient;
    private String apiId;
    private String applicationID;
    private String subscriptionId;
    private ApiProductTestHelper apiProductTestHelper;
    private ApiTestHelper apiTestHelper;
    private RestAPIStoreImpl APIStoreClient;
    private String apiName = "WorkflowTestAPI";
    private String applicationName = "AppCreationWorkflowTestAPP";
    private ArrayList<APIDTO> apisToBeUsed;
    private APIProductDTO apiProductDTO;
    private String apiProductId;

    @Factory(dataProvider = "userModeDataProvider")
    public WorkflowApprovalExecutorTest(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.SUPER_TENANT_USER}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        adminDashboardRestClient = new AdminDashboardRestClient(getPublisherURLHttps());
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());

        userManagementClient.addUser(USER_SMITH, "john123", new String[]{INTERNAL_ROLE_SUBSCRIBER}, USER_SMITH);
        userManagementClient.addUser(USER_ADMIN, "admin", new String[]{ALLOWED_ROLE}, ADMIN_ROLE);

        resourceAdminServiceClient = new ResourceAdminServiceClient(gatewayContextMgt.getContextUrls().getBackEndUrl(),
                createSession(gatewayContextMgt));
        // Gets the original workflow-extentions.xml file's content from the registry.
        originalWFExtentionsXML = resourceAdminServiceClient
                .getTextContent(DEFAULT_WF_EXTENTIONS_XML_REG_CONFIG_LOCATION);
        // Gets the new configuration of the workflow-extentions.xml
        newWFExtentionsXML = readFile(getAMResourceLocation() + File.separator + "configFiles" + File.separator
                + "approveWorkflow" + File.separator + "workflow-extensions.xml");
        // Updates the content of the workflow-extentions.xml of the registry file, to have the new configurations.
        resourceAdminServiceClient.updateTextContent(DEFAULT_WF_EXTENTIONS_XML_REG_CONFIG_LOCATION, newWFExtentionsXML);

        apiProductTestHelper = new ApiProductTestHelper(restAPIPublisher, restAPIStore);
        apiTestHelper = new ApiTestHelper(restAPIPublisher, restAPIStore, getAMResourceLocation(),
                keyManagerContext.getContextTenant().getDomain(), keyManagerHTTPSURL, user);
    }

    @Test(groups = {"wso2.am"}, description = "Api workflow process check")
    public void testAPIWorkflowProcess() throws Exception {

        //add API
        String apiVersion = "1.0.0";
        String apiContext = "workflowCheck";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";

        APIRequest apiRequest;
        apiRequest = new APIRequest(apiName, apiContext, new URL(url));
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(USER_SMITH);
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiResponse.getData();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        //request to publish the API
        HttpResponse lifeCycleChangeResponse = restAPIPublisher
                .changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);
        assertEquals(apiResponse.getResponseCode(), 201,
                "API creation test failed in Approval Workflow Executor");
        assertEquals(lifeCycleChangeResponse.getResponseCode(), 200,
                "Status code mismatch when request is make for change API state");

        // check the current state
        HttpResponse api = restAPIPublisher.getAPI(apiId);
        JSONObject apiObj = new JSONObject(api.getData());
        String apiStatus = (String) apiObj.get("workflowStatus");
        // lifecycle state should not change
        assertEquals(apiStatus, APILifeCycleState.CREATED.toString(),
                "Lifecycle state should remain without changing till approval. ");
        //Get workflow pending requests by unauthorized user
        String workflowType = "AM_API_STATE";
        restAPIAdminUser = new RestAPIAdminImpl(USER_SMITH, "john123", "carbon.super",
                adminURLHttps);
        org.wso2.am.integration.test.HttpResponse response = restAPIAdminUser.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 401,
                "Workflow requests can only be viewed for the admin");
        //Get workflow pending requests by admin
        response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String jsonResponse = response.getData();
        JSONObject workflowRespObj = new JSONObject(response.getData());
        String externalWorkflowRef = null;
        JSONArray arr = (JSONArray) workflowRespObj.get("list");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject listItem = (JSONObject) arr.get(i);
            JSONObject properties = (JSONObject) listItem.get("properties");
            if (properties.has("apiName") && apiName.equals(properties.get("apiName"))) {
                externalWorkflowRef = (String) listItem.get("referenceId");
            }
        }

        assertNotNull("Workflow reference is not available ", externalWorkflowRef);

        //get workflow pending request by external workflow reference by admin
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending request by external workflow reference failed for User Admin");
        //get workflow pending request by external workflow reference by unauthorized user
        response = restAPIAdminUser.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 401,
                "Workflow request can only be viewed for the admin");
        //update workflow pending request by unauthorized user
        response = restAPIAdminUser.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 401,
                "Workflow request can only be updated by the admin");
        //update workflow pending request by admin
        response = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Workflow request update failed for user admin");

        String jsonUpdateResponse = response.getData();
        Gson gsonUpdateResponse = new Gson();
        WorkflowDTO workflowDTO = gsonUpdateResponse.fromJson(jsonUpdateResponse, WorkflowDTO.class);
        String status = workflowDTO.getStatus().toString();
        //workflow status should change as APPROVED
        assertEquals(status, WorkflowStatus.APPROVED.toString(),
                "Workflow state should change by the authorized admin. ");

        HttpResponse apiNew = restAPIPublisher.getAPI(apiId);
        JSONObject apiObject = new JSONObject(apiNew.getData());
        String apiNewStatus = (String) apiObject.get("lifeCycleStatus");
        // lifecycle state should change as PUBLISHED
        assertEquals(apiNewStatus, APILifeCycleState.PUBLISHED.toString(),
                "Lifecycle state should change after approval. ");
    }

    @Test(groups = {"wso2.am"}, description = "API Product workflow process check")
    public void testAPIProductWorkflowProcess() throws Exception {

        createAndDeployAPIProduct();

        // Request to Publish the API Product
        WorkflowResponseDTO workflowResponseDTO = apiProductTestHelper.changeLifecycleStateOfApiProduct(apiProductId,
                "Publish", null);
        assertEquals("CREATED", workflowResponseDTO.getWorkflowStatus().getValue());
        APIProductDTO returnedAPIProductDTO = restAPIPublisher.getApiProduct(apiProductId);
        assertEquals("CREATED", returnedAPIProductDTO.getState());
        assertEquals(returnedAPIProductDTO.getWorkflowStatus(), APILifeCycleState.CREATED.toString(), "Lifecycle "
                + "state should remain without changing till approval. ");

        String workflowType = "AM_API_PRODUCT_STATE";

        WorkflowListDTO workflowListDTO = restAPIAdmin.getWorkflowsByWorkflowType(workflowType);
        assertNotNull(workflowListDTO);
        assertNotNull(workflowListDTO.getList());
        String workflowReferenceID = null;
        for (WorkflowInfoDTO workflow : workflowListDTO.getList()) {
            LinkedTreeMap workflowProperties = (LinkedTreeMap) workflow.getProperties();
            assert workflowProperties != null;
            if (workflowProperties.containsKey("apiName") && apiProductDTO.getName().equals(workflowProperties.get(
                    "apiName"))) {
                workflowReferenceID = workflow.getReferenceId();
            }
        }
        assertNotNull("Workflow reference is not available ", workflowReferenceID);

        org.wso2.am.integration.test.HttpResponse response =
                restAPIAdmin.getWorkflowByExternalWorkflowReference(workflowReferenceID);
        assertEquals(response.getResponseCode(), 200, "Get Workflow Pending request by external workflow " +
                "reference failed for User Admin");

        response = restAPIAdmin.updateWorkflowStatus(workflowReferenceID);
        assertEquals(response.getResponseCode(), 200, "Workflow request update failed for user admin");

        String jsonUpdateResponse = response.getData();
        Gson gsonUpdateResponse = new Gson();
        WorkflowDTO workflowDTO = gsonUpdateResponse.fromJson(jsonUpdateResponse, WorkflowDTO.class);
        String status = workflowDTO.getStatus().toString();
        assertEquals(status, WorkflowStatus.APPROVED.toString());

        returnedAPIProductDTO = restAPIPublisher.getApiProduct(apiProductId);
        assertEquals(returnedAPIProductDTO.getState().toUpperCase(),
                APILifeCycleState.PUBLISHED.toString().toUpperCase(), "Lifecycle state should change after approval");
    }

    private void createAndDeployAPIProduct() throws Exception {

        apisToBeUsed = new ArrayList<>();
        APIDTO apiOne = apiTestHelper.createApiOne(getBackendEndServiceEndPointHttp("wildcard/resources"));
        APIDTO apiTwo = apiTestHelper.createApiTwo(getBackendEndServiceEndPointHttp("wildcard/resources"));
        apisToBeUsed.add(apiOne);
        apisToBeUsed.add(apiTwo);

        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID();

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(USER_SMITH, name, context, apisToBeUsed,
                policies);
        apiProductId = apiProductDTO.getId();
        assert apiProductDTO.getState() != null;
        Assert.assertTrue(APILifeCycleState.CREATED.getState().equalsIgnoreCase(apiProductDTO.getState()));

        waitForAPIDeployment();

        createAPIProductRevisionAndDeployUsingRest(apiProductId, restAPIPublisher);
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Application workflow process check", dependsOnMethods =
            "testAPIWorkflowProcess", enabled = true)
    public void testApplicationWorkflowProcess() throws Exception {

        //create Application
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                "Default version testing application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        applicationID = applicationResponse.getData();
        assertEquals(applicationResponse.getResponseCode(), 200,
                "Application Creation test failed in Approval Workflow Executor");
        //Application State should be CREATED
        ApplicationDTO appResponse = restAPIStore.getApplicationById(applicationID);
        String status1 = appResponse.getStatus();
        assertEquals(status1, "CREATED",
                "Application state should remain without changing till approval. ");
        //get workflow pending requests by unauthorized user
        String workflowType = "AM_APPLICATION_CREATION";
        org.wso2.am.integration.test.HttpResponse response = restAPIAdminUser.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 401,
                "Workflow requests an only view by Admin");
        //get workflow pending requests by Admin
        response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String jsonResponse = response.getData();
        JSONObject workflowRespObj = new JSONObject(response.getData());
        String externalWorkflowRef = null;
        JSONArray arr = (JSONArray) workflowRespObj.get("list");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject listItem = (JSONObject) arr.get(i);
            JSONObject properties = (JSONObject) listItem.get("properties");
            if (properties.has("applicationName") && applicationName.equals(properties.get("applicationName"))) {
                externalWorkflowRef = (String) listItem.get("referenceId");
            }
        }
        assertNotNull("Workflow reference is not available ", externalWorkflowRef);
        
        //get workflow pending requests by external workflow reference by Admin
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");
        //get workflow pending requests by external workflow reference by unauthorized user
        response = restAPIAdminUser.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 401,
                "Workflow request can only be viewed for the admin");
        //update workflow pending request by unauthorized user
        response = restAPIAdminUser.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 401,
                "Workflow request can only be updated by the admin");
        //update workflow pending request by admin
        response = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Workflow state update failed for user admin");

        String jsonUpdateResponse = response.getData();
        Gson gsonUpdateResponse = new Gson();
        WorkflowDTO workflowDTO = gsonUpdateResponse.fromJson(jsonUpdateResponse, WorkflowDTO.class);
        String workflowStatus = workflowDTO.getStatus().toString();
        //Workflow status should be changed as APPROVED
        assertEquals(workflowStatus, WorkflowStatus.APPROVED.toString(),
                "Workflow state should change by the authorized admin. ");
        //Application status should be changed as APPROVED
        ApplicationDTO appFinalResponse = restAPIStore.getApplicationById(applicationID);
        String status = appFinalResponse.getStatus();
        assertEquals(status, "APPROVED",
                "Application state should change after  approval. ");
    }

    @Test(groups = {"wso2.am"}, description = "Application workflow process check", dependsOnMethods =
            "testAPIWorkflowProcess", enabled = true)
    public void testApplicationDeletionWorkflowProcess() throws Exception {

        final String deletingAppName = "AppDeletionWorkflowTestAPP";
        //create Application
        HttpResponse applicationResponse = restAPIStore.createApplication(deletingAppName,
                "Default version testing application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        String deletingAppID = applicationResponse.getData();
        assertEquals(applicationResponse.getResponseCode(), 200,
                "Application Creation test failed in Approval Workflow Executor");
        //Application State should be CREATED
        ApplicationDTO appResponse = restAPIStore.getApplicationById(deletingAppID);
        String status1 = appResponse.getStatus();
        assertEquals(status1, "CREATED", "Application state should remain without changing till approval.");
        //get workflow pending requests by unauthorized user
        String workflowType = "AM_APPLICATION_CREATION";
        org.wso2.am.integration.test.HttpResponse response = restAPIAdminUser.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 401,
                "Workflow requests an only view by Admin");
        //get workflow pending requests by Admin
        response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200, "Get Workflow Pending requests failed for User Admin");

        JSONObject workflowRespObj = new JSONObject(response.getData());
        String externalWorkflowRef = null;
        JSONArray arr = (JSONArray) workflowRespObj.get("list");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject listItem = (JSONObject) arr.get(i);
            JSONObject properties = (JSONObject) listItem.get("properties");
            if (properties.has("applicationName") && deletingAppName.equals(properties.get("applicationName"))) {
                externalWorkflowRef = (String) listItem.get("referenceId");
            }
        }
        assertNotNull("Workflow reference is not available ", externalWorkflowRef);

        //get workflow pending requests by external workflow reference by Admin
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");
        //get workflow pending requests by external workflow reference by unauthorized user
        response = restAPIAdminUser.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 401,
                "Workflow request can only be viewed for the admin");
        //update workflow pending request by unauthorized user
        response = restAPIAdminUser.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 401,
                "Workflow request can only be updated by the admin");
        //update workflow pending request by admin
        response = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Workflow state update failed for user admin");

        String jsonUpdateResponse = response.getData();
        Gson gsonUpdateResponse = new Gson();
        WorkflowDTO workflowDTO = gsonUpdateResponse.fromJson(jsonUpdateResponse, WorkflowDTO.class);
        String workflowStatus = workflowDTO.getStatus().toString();
        //Workflow status should be changed as APPROVED
        assertEquals(workflowStatus, WorkflowStatus.APPROVED.toString(),
                "Workflow state should change by the authorized admin. ");
        //Application status should be changed as APPROVED
        ApplicationDTO appFinalResponse = restAPIStore.getApplicationById(deletingAppID);
        String status = appFinalResponse.getStatus();
        assertEquals(status, "APPROVED",
                "Application state should change after  approval. ");

        //Delete application
        HttpResponse delResponse = restAPIStore.deleteApplicationWithHttpResponse(deletingAppID);
        assertEquals(delResponse.getResponseCode(), 201, "Application deletion approval workflow failure");

        //Application State should be DELETE_PENDING
        ApplicationDTO deletePendingAppDTO = restAPIStore.getApplicationById(deletingAppID);
        assertNotNull("Application deleted without approval process", deletePendingAppDTO);
        String deletePendingAppStatus = deletePendingAppDTO.getStatus();
        assertEquals(deletePendingAppStatus, "DELETE_PENDING",
                "Application state should remain as DELETE_PENDING till approval. ");

        //Delete a DELETE_PENDING application
        HttpResponse delRetryResponse = restAPIStore.deleteApplicationWithHttpResponse(deletingAppID);
        assertEquals(delRetryResponse.getResponseCode(), 400,
                "Delete pending application should not be able to be deleted by deletion request");

        removeDeletePendingApplication(deletingAppName);

        HttpResponse appRetrieveAfterDelResponse = restAPIStore.getApplicationByIdWithHttpResponse(deletingAppID);
        int responseCode = appRetrieveAfterDelResponse.getResponseCode();
        assertEquals(responseCode, 404, "Application deletion failed for approval flow");
    }

    private void removeDeletePendingApplication(String applicationName) throws ApiException, JSONException {

        final String appDeletionWorkflowType = "AM_APPLICATION_DELETION";
        org.wso2.am.integration.test.HttpResponse delWorkflowsResponse = restAPIAdmin.getWorkflows(appDeletionWorkflowType);
        assertEquals(delWorkflowsResponse.getResponseCode(), 200, "Get Workflow Pending requests failed for User Admin");

        JSONObject delWorkflowsObject = new JSONObject(delWorkflowsResponse.getData());
        String delExternalWorkflowRef = null;
        JSONArray wfArray = (JSONArray) delWorkflowsObject.get("list");
        for (int i = 0; i < wfArray.length(); i++) {
            JSONObject listItem = (JSONObject) wfArray.get(i);
            JSONObject properties = (JSONObject) listItem.get("properties");
            if (properties.has("applicationName") && applicationName.equals(properties.get("applicationName"))) {
                delExternalWorkflowRef = (String) listItem.get("referenceId");
                break;
            }
        }
        assertNotNull("Workflow reference is not available ", delExternalWorkflowRef);

        delWorkflowsResponse = restAPIAdmin.getWorkflowByExternalWorkflowReference(delExternalWorkflowRef);
        assertEquals(delWorkflowsResponse.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");
        org.wso2.am.integration.test.HttpResponse updateWorkflowResponse = restAPIAdmin.updateWorkflowStatus(delExternalWorkflowRef);
        assertEquals(updateWorkflowResponse.getResponseCode(), 200, "Workflow state update failed for user admin");
    }

    @Test(groups = {"wso2.am"}, description = "Subscription workflow process check", dependsOnMethods = 
        {"testApplicationWorkflowProcess", "testAPIWorkflowProcess" })
    public void testSubscriptionWorkflowProcess() throws Exception {

        //create Subscription
        HttpResponse SubscribeResponse = restAPIStore.createSubscription(apiId, applicationID,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        subscriptionId = SubscribeResponse.getData();
        assertEquals(SubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of API is not successful");

        SubscriptionListDTO subscriptionListDTO = restAPIPublisher.getSubscriptionByAPIID(apiId);
        List<SubscriptionDTO> subscriptionList = new ArrayList<SubscriptionDTO>();
        subscriptionList = subscriptionListDTO.getList();
        SubscriptionDTO.SubscriptionStatusEnum SubscriptionStatus = SubscriptionDTO.SubscriptionStatusEnum.BLOCKED;
        for (SubscriptionDTO subscriptioninfo : subscriptionList) {
            if (applicationID.equals(subscriptioninfo.getApplicationInfo().getApplicationId())) {
                SubscriptionStatus = subscriptioninfo.getSubscriptionStatus();
                log.info("Found valid subscription for the application");
                break;
            }
        }

        //Subscription Status should not change
        assertEquals(SubscriptionStatus, SubscriptionDTO.SubscriptionStatusEnum.ON_HOLD,
                "Subscription state should remain without changing till approval. ");
        //get pending workflow requests by unauthorized user
        String workflowType = "AM_SUBSCRIPTION_CREATION";
        org.wso2.am.integration.test.HttpResponse response = restAPIAdminUser.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 401,
                "Workflow Pending requests can only viewed for User Admin");
        //get pending workflow requests by Admin
        response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String jsonResponse = response.getData();
        JSONObject workflowRespObj = new JSONObject(response.getData());
        String externalWorkflowRef = null;
        JSONArray arr = (JSONArray) workflowRespObj.get("list");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject listItem = (JSONObject) arr.get(i);
            JSONObject properties = (JSONObject) listItem.get("properties");
            if (properties.has("applicationName") && applicationName.equals(properties.get("applicationName"))
                    && properties.has("apiName") &&  apiName.equals(properties.get("apiName"))) {
                externalWorkflowRef = (String) listItem.get("referenceId");
            }
        }
        assertNotNull("Workflow reference is not available ", externalWorkflowRef);

        //get pending workflow request by ExternalWorkflow Reference by Admin
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");
        //get pending workflow request by ExternalWorkflow Reference by unauthorized user
        response = restAPIAdminUser.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 401,
                "Workflow request can only be viewed for the admin");
        //update pending workflow request by ExternalWorkflow Reference by unauthorized user
        response = restAPIAdminUser.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 401,
                "Workflow request can only be updated by the admin");
        //update pending workflow requests by ExternalWorkflow Reference by Admin
        response = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Updated workflow state is failed for user admin");

        String jsonUpdateResponse = response.getData();
        Gson gsonUpdateResponse = new Gson();
        WorkflowDTO workflowDTO = gsonUpdateResponse.fromJson(jsonUpdateResponse, WorkflowDTO.class);
        String workflowStatus = workflowDTO.getStatus().toString();
        //Workflow status should change to APPROVED
        assertEquals(workflowStatus, WorkflowStatus.APPROVED.toString(),
                "Workflow state should change by the authorized admin. ");

        SubscriptionListDTO subscriptionFinalListDTO = restAPIPublisher.getSubscriptionByAPIID(apiId);
        List<SubscriptionDTO> subscriptionFinalList = new ArrayList<SubscriptionDTO>();
        subscriptionFinalList = subscriptionFinalListDTO.getList();
        for (SubscriptionDTO subscriptioninfo : subscriptionFinalList) {
            if (applicationID.equals(subscriptioninfo.getApplicationInfo().getApplicationId())) {
                SubscriptionStatus = subscriptioninfo.getSubscriptionStatus();
                log.info("Found valid subscription for the application");
                break;
            }
        }

        //Subscription state should change as UNBLOCKED
        assertEquals(SubscriptionStatus, SubscriptionDTO.SubscriptionStatusEnum.UNBLOCKED,
                "Subscription state should change after approval. ");
    }

    @Test(groups = {"wso2.am"}, description = "Subscription workflow process check", dependsOnMethods =
            {"testApplicationWorkflowProcess", "testAPIWorkflowProcess", "testSubscriptionWorkflowProcess"})
    public void testSubscriptionDeletionWorkflowProcess() throws Exception {

        //create Application
        final String subDelTestAppName = "SubDelTestApp";
        HttpResponse applicationResponse = restAPIStore.createApplication(subDelTestAppName,
                "Default version testing application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        String subDelTestAppID = applicationResponse.getData();

        final String workflowTypeAppCreation = "AM_APPLICATION_CREATION";
        org.wso2.am.integration.test.HttpResponse response = restAPIAdmin.getWorkflows(workflowTypeAppCreation);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        JSONObject workflowRespObj = new JSONObject(response.getData());
        String externalWorkflowRef = null;
        JSONArray arr = (JSONArray) workflowRespObj.get("list");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject listItem = (JSONObject) arr.get(i);
            JSONObject properties = (JSONObject) listItem.get("properties");
            if (properties.has("applicationName") && subDelTestAppName.equals(properties.get("applicationName"))) {
                externalWorkflowRef = (String) listItem.get("referenceId");
            }
        }
        assertNotNull("Workflow reference is not available ", externalWorkflowRef);

        response = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Updated workflow state is failed for user admin");

        //create Subscription
        HttpResponse subCreationResponse = restAPIStore.createSubscription(apiId, subDelTestAppID,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        String subscriptionId = subCreationResponse.getData();
        assertEquals(subCreationResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of API is not successful");

        //get pending workflow requests by unauthorized user
        final String workflowTypeSubCreation = "AM_SUBSCRIPTION_CREATION";
        org.wso2.am.integration.test.HttpResponse subWorkflowResponse = restAPIAdmin.getWorkflows(workflowTypeSubCreation);
        assertEquals(subWorkflowResponse.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        JSONObject subCreationWorkflowRespObj = new JSONObject(subWorkflowResponse.getData());
        String subCreationExternalWorkflowRef = null;
        JSONArray subCreationWFArray = (JSONArray) subCreationWorkflowRespObj.get("list");
        for (int i = 0; i < subCreationWFArray.length(); i++) {
            JSONObject listItem = (JSONObject) subCreationWFArray.get(i);
            JSONObject properties = (JSONObject) listItem.get("properties");
            if (properties.has("applicationName") && subDelTestAppName.equals(properties.get("applicationName"))
                    && properties.has("apiName") && apiName.equals(properties.get("apiName"))) {
                subCreationExternalWorkflowRef = (String) listItem.get("referenceId");
            }
        }
        assertNotNull("Workflow reference is not available ", subCreationExternalWorkflowRef);

        //get pending workflow request by ExternalWorkflow Reference by Admin
        subWorkflowResponse = restAPIAdmin.getWorkflowByExternalWorkflowReference(subCreationExternalWorkflowRef);
        assertEquals(subWorkflowResponse.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");

        //update pending workflow requests by ExternalWorkflow Reference by Admin
        subWorkflowResponse = restAPIAdmin.updateWorkflowStatus(subCreationExternalWorkflowRef);
        assertEquals(subWorkflowResponse.getResponseCode(), 200,
                "Updated workflow state is failed for user admin");

        String jsonUpdateResponse = subWorkflowResponse.getData();
        Gson gsonUpdateResponse = new Gson();
        WorkflowDTO workflowDTO = gsonUpdateResponse.fromJson(jsonUpdateResponse, WorkflowDTO.class);
        String workflowStatus = workflowDTO.getStatus().toString();
        //Workflow status should change to APPROVED
        assertEquals(workflowStatus, WorkflowStatus.APPROVED.toString(),
                "Workflow state should change by the authorized admin. ");

        SubscriptionListDTO subscriptionFinalListDTO = restAPIPublisher.getSubscriptionByAPIID(apiId);
        List<SubscriptionDTO> subscriptionFinalList = subscriptionFinalListDTO.getList();
        assertNotNull(subscriptionFinalList);
        SubscriptionDTO.SubscriptionStatusEnum subscriptionStatus = SubscriptionDTO.SubscriptionStatusEnum.BLOCKED;
        for (SubscriptionDTO subscriptionInfo : subscriptionFinalList) {
            if (subDelTestAppID.equals(subscriptionInfo.getApplicationInfo().getApplicationId())) {
                subscriptionStatus = subscriptionInfo.getSubscriptionStatus();
                log.info("Found valid subscription for the application");
                break;
            }
        }

        //Subscription state should change as UNBLOCKED
        assertEquals(subscriptionStatus, SubscriptionDTO.SubscriptionStatusEnum.UNBLOCKED,
                "Subscription state should change after approval. ");

        HttpResponse subDeletionResponse = restAPIStore.removeSubscriptionWithHttpInfo(subscriptionId);
        assertEquals(subDeletionResponse.getResponseCode(), 201,
                "Subscription deletion approval workflow failure");

        SubscriptionListDTO subscriptionListDTO = restAPIPublisher.getSubscriptionByAPIID(apiId);
        List<SubscriptionDTO> subscriptionList = subscriptionListDTO.getList();
        assertNotNull(subscriptionList);
        subscriptionStatus = SubscriptionDTO.SubscriptionStatusEnum.BLOCKED;
        for (SubscriptionDTO subscriptionInfo : subscriptionList) {
            if (subDelTestAppID.equals(subscriptionInfo.getApplicationInfo().getApplicationId())) {
                subscriptionStatus = subscriptionInfo.getSubscriptionStatus();
                log.info("Found valid subscription for the application");
                break;
            }
        }

        //Subscription Status should not change
        assertEquals(subscriptionStatus, SubscriptionDTO.SubscriptionStatusEnum.DELETE_PENDING,
                "Subscription state should be DELETE_PENDING in the subscription deletion approval process");

        final String workflowTypeSubDeletion = "AM_SUBSCRIPTION_DELETION";
        org.wso2.am.integration.test.HttpResponse subDelWorkflowResponse = restAPIAdmin.getWorkflows(workflowTypeSubDeletion);
        assertEquals(subWorkflowResponse.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        JSONObject subDelWorkflowRespObj = new JSONObject(subDelWorkflowResponse.getData());
        String subDelExternalWorkflowRef = null;
        JSONArray subDelWFArray = (JSONArray) subDelWorkflowRespObj.get("list");
        for (int i = 0; i < subDelWFArray.length(); i++) {
            JSONObject listItem = (JSONObject) subDelWFArray.get(i);
            JSONObject properties = (JSONObject) listItem.get("properties");
            if (properties.has("applicationName") && subDelTestAppName.equals(properties.get("applicationName"))
                    && properties.has("apiName") && apiName.equals(properties.get("apiName"))) {
                subDelExternalWorkflowRef = (String) listItem.get("referenceId");
            }
        }
        assertNotNull("Workflow reference is not available ", subDelExternalWorkflowRef);

        //update pending workflow requests by ExternalWorkflow Reference by Admin
        subWorkflowResponse = restAPIAdmin.updateWorkflowStatus(subDelExternalWorkflowRef);
        assertEquals(subWorkflowResponse.getResponseCode(), 200,
                "Updated workflow state is failed for user admin");

        SubscriptionListDTO subscriptionsAfterDelDTO = restAPIPublisher.getSubscriptionByAPIID(apiId);
        List<SubscriptionDTO> subscriptionsAfterDel = subscriptionsAfterDelDTO.getList();
        assertNotNull(subscriptionsAfterDel);
        SubscriptionDTO deletedSubscription = subscriptionsAfterDel.stream()
                .filter(subscriptionDTO -> subscriptionDTO.getApplicationInfo() != null &&
                        subDelTestAppID.equals(subscriptionDTO.getApplicationInfo().getApplicationId()))
                .findAny().orElse(null);
        assertNull(deletedSubscription, "Subscription deletion failed for approval flow");

        //Delete application
        HttpResponse delResponse = restAPIStore.deleteApplicationWithHttpResponse(subDelTestAppID);
        assertEquals(delResponse.getResponseCode(), 201, "Application deletion approval workflow failure");

        removeDeletePendingApplication(subDelTestAppName);

        HttpResponse appRetrieveAfterDelResponse = restAPIStore.getApplicationByIdWithHttpResponse(subDelTestAppID);
        int responseCode = appRetrieveAfterDelResponse.getResponseCode();
        assertEquals(responseCode, 404, "Application deletion failed for approval flow");
    }

    @Test(groups = {"wso2.am"}, description = "Registration workflow process check",
            dependsOnMethods = "testSubscriptionWorkflowProcess")
    public void testRegistrationWorkflowProcess() throws Exception {

        //generate keys
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationID,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        //application key generation state should not change
        String keyState = applicationKeyDTO.getKeyState();
        assertEquals(keyState, "CREATED",
                "Application key generation should not change until approval");
        //get pending workflow requests by unauthorized user
        String workflowType = "AM_APPLICATION_REGISTRATION_PRODUCTION";
        org.wso2.am.integration.test.HttpResponse response = restAPIAdminUser.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 401,
                "Workflow pending requests can only view by admin");
        //get pending workflow requests by admin
        response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String jsonResponse = response.getData();
        JSONObject workflowRespObj = new JSONObject(response.getData());
        String externalWorkflowRef = null;
        JSONArray arr = (JSONArray) workflowRespObj.get("list");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject listItem = (JSONObject) arr.get(i);
            JSONObject properties = (JSONObject) listItem.get("properties");
            if (properties.has("applicationName") && applicationName.equals(properties.get("applicationName"))) {
                externalWorkflowRef = (String) listItem.get("referenceId");
            }
        }
        assertNotNull("Workflow reference is not available ", externalWorkflowRef);

        //get pending workflow requests by external workflow reference by admin
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");
        //get pending workflow requests by external workflow reference by unauthorized user
        response = restAPIAdminUser.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 401,
                "Workflow request can only be viewed for the admin");
        //update pending workflow requests  by unauthorized user
        response = restAPIAdminUser.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 401,
                "Workflow request can only be updated for the admin");
        //update pending workflow requests  by admin
        response = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "failed to update the workflow request for user admin");

        String jsonUpdateResponse = response.getData();
        Gson gsonUpdateResponse = new Gson();
        WorkflowDTO workflowDTO = gsonUpdateResponse.fromJson(jsonUpdateResponse, WorkflowDTO.class);
        String workflowStatus = workflowDTO.getStatus().toString();
        //workflow status should change to APPROVED
        assertEquals(workflowStatus, WorkflowStatus.APPROVED.toString(),
                "Workflow state should change by the authorized admin. ");

        ApiResponse<ApplicationKeyDTO> apiresponse = restAPIStore.getApplicationKeysByKeyType(applicationID,
                "PRODUCTION");
        ApplicationKeyDTO applicationKeyData = apiresponse.getData();
        //key state should change to COMPLETED
        assertEquals(applicationKeyData.getKeyState(), "COMPLETED",
                "Application key generation stat should change after approval");
    }

    @Test(groups = {"wso2.am"}, description = "User Sign Up workflow process check",
            dependsOnMethods = "testRegistrationWorkflowProcess")
    public void testUserSignUpWorkflowProcess() throws Exception {

        String giveName = "Jane";
        String username = "JaneDoe";
        String password = "admin";
        String organization = "wso2";
        String email = "janedoe@gmail.com";
        String store = storeURLHttps;

        //User self sign up process
        UserManagementUtils.signupUser(username, password, giveName, organization, email);
        //APIStoreClient = new RestAPIStoreImpl(username, password, SUPER_TENANT_DOMAIN, store);
        //get workflow pending requests by unauthorized user
        String workflowType = "AM_USER_SIGNUP";
        org.wso2.am.integration.test.HttpResponse response = restAPIAdminUser.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 401,
                "Workflow requests can only viewed by admin");
        //get workflow pending requests by admin
        response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String jsonResponse = response.getData();
        JSONObject workflowRespObj = new JSONObject(response.getData());
        String externalWorkflowRef = null;
        JSONArray arr = (JSONArray) workflowRespObj.get("list");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject listItem = (JSONObject) arr.get(i);
            JSONObject properties = (JSONObject) listItem.get("properties");
            if (properties.has("tenantAwareUserName") && username.equals(properties.get("tenantAwareUserName"))) {
                externalWorkflowRef = (String) listItem.get("referenceId");
            }
        }
        assertNotNull("Workflow reference is not available ", externalWorkflowRef);

        //get workflow pending requests by external workflow reference by admin
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");
        //get workflow pending requests by external workflow reference by unauthorized user
        response = restAPIAdminUser.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 401,
                "Workflow request can only be viewed for the admin");
        //update workflow pending requests  by unauthorized user
        response = restAPIAdminUser.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 401,
                "Workflow request can only be updated by the the admin");
        //update workflow pending requests  by Admin
        response = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Failed to update Workflow request for user admin");

        String jsonUpdateResponse = response.getData();
        Gson gsonUpdateResponse = new Gson();
        WorkflowDTO workflowDTO = gsonUpdateResponse.fromJson(jsonUpdateResponse, WorkflowDTO.class);
        String workflowStatus = workflowDTO.getStatus().toString();

        //Workflow state should change after approval
        assertEquals(workflowStatus, WorkflowStatus.APPROVED.toString(),
                "Workflow state should change by the authorized admin. ");
        //Login in to developer portal
        HttpResponse loginResponse = apiStore.login(username, password);
        assertEquals(loginResponse.getResponseCode(), 302,
                "Failed to login to developer portal");
    }

    @Test(groups = {"wso2.am"}, description = "clean up workflow process check", dependsOnMethods =
            "testUserSignUpWorkflowProcess")
    public void testCleanUpWorkflowProcess() throws Exception {
        String applicationName = "MyApp";
        //create application
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                "Testing application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        String applicationIDNew = applicationResponse.getData();
        //create api
        String apiName = "WorkflowCheckingAPINew";
        String apiVersion = "1.0.0";
        String apiContext = "workflowChecking";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        JSONObject properties;
        APIRequest apiRequest;
        apiRequest = new APIRequest(apiName, apiContext, new URL(url));
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(USER_SMITH);
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        String apiIdNew = apiResponse.getData();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiIdNew, restAPIPublisher);
        //request to publish the API
        HttpResponse lifeCycleChangeResponse = restAPIPublisher
                .changeAPILifeCycleStatus(apiIdNew, APILifeCycleAction.PUBLISH.getAction(), null);

        //get pending workflow requests of API state change
        String workflowType = "AM_API_STATE";
        org.wso2.am.integration.test.HttpResponse response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String jsonAPIStateChangeGetResponse = response.getData();
        String apiStateChangeExternalWorkflowRef = null;
        JSONObject workflowRespObj = new JSONObject(response.getData());
        String externalWorkflowRef = null;
        JSONArray arr = (JSONArray) workflowRespObj.get("list");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject listItem = (JSONObject) arr.get(i);
            properties = (JSONObject) listItem.get("properties");
            if (properties.has("apiName") && apiName.equals(properties.get("apiName"))) {
                apiStateChangeExternalWorkflowRef = (String) listItem.get("referenceId");
            }
        }
        assertNotNull("Workflow reference is not available ", apiStateChangeExternalWorkflowRef);
        //get workflow pending request by external workflow reference by admin
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(apiStateChangeExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");
        //get workflow pending requests of Application creation
        workflowType = "AM_APPLICATION_CREATION";
        response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String jsonApplicationCreationGetResponse = response.getData();
        String applicationCreationExternalWorkflowRef = null;
        workflowRespObj = new JSONObject(response.getData());
        arr = (JSONArray) workflowRespObj.get("list");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject listItem = (JSONObject) arr.get(i);
            properties = (JSONObject) listItem.get("properties");
            if (properties.has("applicationName") && applicationName.equals(properties.get("applicationName"))) {
                applicationCreationExternalWorkflowRef = (String) listItem.get("referenceId");
            }
        }
        assertNotNull("Workflow reference is not available ", applicationCreationExternalWorkflowRef);

        //get workflow pending request by external workflow reference by admin
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(applicationCreationExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");
        undeployAndDeleteAPIRevisionsUsingRest(apiIdNew, restAPIPublisher);
        //clean up process for API and application
        restAPIPublisher.deleteAPI(apiIdNew);
        HttpResponse delResponse = restAPIStore.deleteApplicationWithHttpResponse(applicationIDNew);
        assertEquals(delResponse.getResponseCode(), 201, "Application deletion approval workflow failure");
        removeDeletePendingApplication(applicationName);
        //check workflow pending requests are cleaned (Application Creation, API State Change)
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(applicationCreationExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 500,
                "Clean up pending task process is failed for Application creation");
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(apiStateChangeExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 500,
                "Clean up pending task process is failed for API state change");

        //create Application
        HttpResponse applicationResponseNew = restAPIStore.createApplication(applicationName,
                "Testing application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        String applicationIDSecond = applicationResponseNew.getData();
        //create API and request for publish the API
        HttpResponse apiResponseNew = restAPIPublisher.addAPI(apiRequest);
        String apiIdSecond = apiResponseNew.getData();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiIdSecond, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatus(apiIdSecond, APILifeCycleAction.PUBLISH.getAction(),
                null);
        //get workflow requests of API state change
        workflowType = "AM_API_STATE";
        response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String jsonAPIStateGetResponse = response.getData();
        String apiStateChangeNewExternalWorkflowRef = null;
        workflowRespObj = new JSONObject(response.getData());
        arr = (JSONArray) workflowRespObj.get("list");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject listItem = (JSONObject) arr.get(i);
            properties = (JSONObject) listItem.get("properties");
            if (properties.has("apiName") && apiName.equals(properties.get("apiName"))) {
                apiStateChangeNewExternalWorkflowRef = (String) listItem.get("referenceId");
            }
        }
        assertNotNull("Workflow reference is not available ", apiStateChangeNewExternalWorkflowRef);
        //update workflow request and approve the API state change
        response = restAPIAdmin.updateWorkflowStatus(apiStateChangeNewExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Update workflow pending process is failed for user admin");
        //get workflow pending request for application creation
        workflowType = "AM_APPLICATION_CREATION";
        response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String jsonApplicationCreation = response.getData();
        String applicationCreationNewExternalWorkflowRef = null;
        workflowRespObj = new JSONObject(response.getData());
        arr = (JSONArray) workflowRespObj.get("list");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject listItem = (JSONObject) arr.get(i);
            properties = (JSONObject) listItem.get("properties");
            if (properties.has("applicationName") && applicationName.equals(properties.get("applicationName"))) {
                applicationCreationNewExternalWorkflowRef = (String) listItem.get("referenceId");
            }
        }
        assertNotNull("Application workflow reference is not available ", applicationCreationNewExternalWorkflowRef);
        //update workflow pending task of application creation
        response = restAPIAdmin.updateWorkflowStatus(applicationCreationNewExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Update workflow pending process is failed for user admin");
        //subscription creation
        HttpResponse SubscribeResponse = restAPIStore.createSubscription(apiIdSecond, applicationIDSecond,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        subscriptionId = SubscribeResponse.getData();
        assertEquals(SubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of  API  request not successful");
        //get workflow pending requests of subscription creation
        workflowType = "AM_SUBSCRIPTION_CREATION";
        response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String jsonSubscriptionCreationGet = response.getData();
        String subscriptionCreationExternalWorkflowRef = null;
        workflowRespObj = new JSONObject(response.getData());
        arr = (JSONArray) workflowRespObj.get("list");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject listItem = (JSONObject) arr.get(i);
            properties = (JSONObject) listItem.get("properties");
            if (properties.has("applicationName") && applicationName.equals(properties.get("applicationName"))
                    && properties.has("apiName") &&  apiName.equals(properties.get("apiName"))) {
                subscriptionCreationExternalWorkflowRef = (String) listItem.get("referenceId");
            }
        }
        assertNotNull("Subscription workflow reference is not available ", subscriptionCreationExternalWorkflowRef);
        //get workflow pending task by external workflow reference by admin
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(subscriptionCreationExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");
        //generate keys
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationIDSecond,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        //get workflow pending requests for application key generation
        workflowType = "AM_APPLICATION_REGISTRATION_PRODUCTION";
        response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String jsonKeyGenerationGetResponse = response.getData();
        String keyGenerationExternalWorkflowRef = null;
        workflowRespObj = new JSONObject(response.getData());
        arr = (JSONArray) workflowRespObj.get("list");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject listItem = (JSONObject) arr.get(i);
            properties = (JSONObject) listItem.get("properties");
            if (properties.has("applicationName") && applicationName.equals(properties.get("applicationName"))) {
                keyGenerationExternalWorkflowRef = (String) listItem.get("referenceId");
            }
        }
        assertNotNull("application key workflow reference is not available ", keyGenerationExternalWorkflowRef);
        //get workflow pending request by external workflow reference
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(keyGenerationExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");
        //clean up process for all pending requests
        restAPIStore.deleteApplication(applicationIDSecond);
        removeDeletePendingApplication(applicationName);
        undeployAndDeleteAPIRevisionsUsingRest(apiIdSecond, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiIdSecond);
        //check the clean up process is successful (Application key generation, API subscription)
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(subscriptionCreationExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 500,
                "Clean up pending task process is failed for Subscription Creation");
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(keyGenerationExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 500,
                "Clean up pending task process is failed for Application Key generation");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationID);
        removeDeletePendingApplication(applicationName);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        userManagementClient.deleteUser(USER_SMITH);
        userManagementClient.deleteUser(USER_ADMIN);
        userManagementClient.deleteUser("JaneDoe");
        resourceAdminServiceClient.updateTextContent(DEFAULT_WF_EXTENTIONS_XML_REG_CONFIG_LOCATION, originalWFExtentionsXML);
        super.cleanUp();
    }
}
