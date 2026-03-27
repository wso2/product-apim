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
import org.apache.commons.lang3.StringUtils;
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
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationListDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.ApiProductTestHelper;
import org.wso2.am.integration.test.impl.ApiTestHelper;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.UserManagementUtils;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.AdminDashboardRestClient;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;

import org.wso2.carbon.apimgt.api.WorkflowStatus;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.*;

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
    private String apiId2;
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
    private static final String appTier = APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN;

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
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());
        userManagementClient.addUser(USER_SMITH, "john123", new String[]{INTERNAL_ROLE_SUBSCRIBER}, USER_SMITH);
        userManagementClient.addUser(USER_ADMIN, "admin", new String[]{ALLOWED_ROLE}, ADMIN_ROLE);

        ServerConfigurationManager serverConfigurationManager = new ServerConfigurationManager(gatewayContextMgt);
        serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "approveWorkflow" + File.separator
                + "deployment.toml"));

        adminDashboardRestClient = new AdminDashboardRestClient(getPublisherURLHttps());

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
        // Enable both Unlimited and Gold so subscription tier updates can be tested between valid tiers.
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.GOLD + "," + APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(USER_SMITH);
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiResponse.getData();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        String externalRef = getExternalRef(apiName);
        acceptDeployRequestByAdmin(externalRef);
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
        assertEquals(workflowResponseDTO.getWorkflowStatus().getValue(), "CREATED");
        APIProductDTO returnedAPIProductDTO = restAPIPublisher.getApiProduct(apiProductId);
        assertEquals(returnedAPIProductDTO.getState(), "CREATED");
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
        final String version = "1.0.0";

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(USER_SMITH, name, context, version, apisToBeUsed,
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
        Map<String, String> applicationAttributes = new HashMap<>();
        applicationAttributes.put("Department Name", "Finance");
        applicationAttributes.put("External Reference ID", "10");
        applicationAttributes.put("Technical Contact", "bob@example.com");

        HttpResponse applicationResponse = restAPIStore.createApplicationWithCustomAttribute(
                applicationName,
                "Default version testing application",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH,
                applicationAttributes);
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

        JSONObject appWorkflowObj = new JSONObject(response.getData());
        JSONObject appWorkflowProperties = appWorkflowObj.getJSONObject("properties");

        assertEquals(appWorkflowProperties.getString("applicationName"), applicationName);

        assertTrue(appWorkflowProperties.has("applicationAttributes"),
                "Application attributes should be present when applicationAttributesVisibility is enabled");

        JSONObject appAttributesObj = new JSONObject(appWorkflowProperties.getString("applicationAttributes"));
        assertEquals(appAttributesObj.getString("Department Name"), "Finance");
        assertEquals(appAttributesObj.getString("External Reference ID"), "10");
        assertEquals(appAttributesObj.getString("Technical Contact"), "bob@example.com");

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

    @Test(groups = {"wso2.am"}, description = "Application update workflow process check", dependsOnMethods =
            "testAPIWorkflowProcess", enabled = true)
    public void testApplicationUpdateWorkflowProcess() throws Exception {

        final String appName = "AppUpdateWorkflowTestAPP";
        final String appDescription = "Update workflow testing application";

        final String appNameForApproval = "AppUpdateWorkflowTestAPPForApproval";
        final String appDescriptionForApproval = "Update workflow testing application For Approval";

        final String appNameForRejection = "AppUpdateWorkflowTestAPPForRejection";
        final String appDescriptionForRejection = "Update workflow testing application For Rejection";

        //create Application
        Map<String, String> initialApplicationAttributes = new HashMap<>();
        initialApplicationAttributes.put("Department Name", "Finance");
        initialApplicationAttributes.put("External Reference ID", "10");
        initialApplicationAttributes.put("Technical Contact", "bob@example.com");

        HttpResponse applicationResponse = restAPIStore.createApplicationWithCustomAttribute(
                appName,
                appDescription,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH,
                initialApplicationAttributes);
        String applicationID = applicationResponse.getData();
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

        JSONObject workflowRespObj = new JSONObject(response.getData());
        String externalWorkflowRef = null;
        JSONArray arr = (JSONArray) workflowRespObj.get("list");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject listItem = (JSONObject) arr.get(i);
            JSONObject properties = (JSONObject) listItem.get("properties");
            if (properties.has("applicationName") && appName.equals(properties.get("applicationName"))) {
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

        //Update Application to check the approval flow
        Map<String, String> updatedApplicationAttributes = new HashMap<>();
        updatedApplicationAttributes.put("Department Name", "Finance");
        updatedApplicationAttributes.put("External Reference ID", "20");
        updatedApplicationAttributes.put("Technical Contact", "alice@example.com");

        HttpResponse updateResponseForApprovalCheck = restAPIStore.updateApplicationByID(
                applicationID,
                appNameForApproval,
                appDescriptionForApproval,
                appTier,
                updatedApplicationAttributes);

        assertEquals(updateResponseForApprovalCheck.getResponseCode(), 200, "Application update approval workflow failure");

        //Application state should be UPDATE_PENDING
        ApplicationDTO updatePendingAppDTO = restAPIStore.getApplicationById(applicationID);
        String applicationStatusAfterUpdate = updatePendingAppDTO.getStatus();
        assertEquals(applicationStatusAfterUpdate, "UPDATE_PENDING",
                "Application state should remain as UPDATE_PENDING till approval.");

        //Check re updating and UPDATE_PENDING application
        HttpResponse reuUpdateResponseForPendingApp = restAPIStore.updateApplicationByID(applicationID,
                appNameForApproval, appDescriptionForApproval, appTier);

        assertEquals(reuUpdateResponseForPendingApp.getResponseCode(), 409,
                "Update operation should be blocked for applications in UPDATE PENDING state.");

        //Approve the pending changes for the application
        approveUpdatePendingApplication(appName);

        ApplicationDTO appRetrieveAfterApproving = restAPIStore.getApplicationById(applicationID);
        assertEquals(appRetrieveAfterApproving.getStatus(), "APPROVED"
                , "Application status should be APPROVED after the approval");

        assertEquals(appRetrieveAfterApproving.getName(), appNameForApproval
                , "Application name should be updated after the approval");

        assertEquals(appRetrieveAfterApproving.getDescription(), appDescriptionForApproval
                , "Application description should be updated after the approval");

        assertNotNull(appRetrieveAfterApproving.getAttributes());
        assertEquals(appRetrieveAfterApproving.getAttributes().get("Department Name"), "Finance");
        assertEquals(appRetrieveAfterApproving.getAttributes().get("External Reference ID"), "20");
        assertEquals(appRetrieveAfterApproving.getAttributes().get("Technical Contact"), "alice@example.com");

        //Update Application again to check the rejection flow
        Map<String, String> rejectedApplicationAttributes = new HashMap<>();
        rejectedApplicationAttributes.put("Department Name", "HR");
        rejectedApplicationAttributes.put("External Reference ID", "30");
        rejectedApplicationAttributes.put("Technical Contact", "charlie@example.com");

        HttpResponse updateAppResponseForRejectionCheck = restAPIStore.updateApplicationByID(
                applicationID,
                appNameForRejection,
                appDescriptionForRejection,
                appTier,
                rejectedApplicationAttributes);
        assertEquals(updateAppResponseForRejectionCheck.getResponseCode(), 200,
                "Application update approval workflow failure");

        ApplicationDTO rejectedUpdatePendingAppDTO = restAPIStore.getApplicationById(applicationID);
        assertEquals(rejectedUpdatePendingAppDTO.getStatus(), "UPDATE_PENDING",
                "Application state should remain as UPDATE_PENDING till approval.");

        //Reject the pending changes for the application
        rejectUpdatePendingApplication(appNameForApproval);

        ApplicationDTO appRetrieveAfterRejecting = restAPIStore.getApplicationById(applicationID);
        assertEquals(appRetrieveAfterRejecting.getStatus(), "UPDATE_REJECTED"
                , "Application status should be UPDATE_REJECTED after the rejection");

        assertNotEquals(appRetrieveAfterRejecting.getName(), appNameForRejection
                , "Application name shouldn't be updated after the rejection");

        assertNotEquals(appRetrieveAfterRejecting.getDescription(), appDescriptionForRejection
                , "Application description shouldn't be updated after the rejection");

        assertNotNull(appRetrieveAfterRejecting.getAttributes());
        assertEquals(appRetrieveAfterRejecting.getAttributes().get("Department Name"), "Finance");
        assertEquals(appRetrieveAfterRejecting.getAttributes().get("External Reference ID"), "20");
        assertEquals(appRetrieveAfterRejecting.getAttributes().get("Technical Contact"), "alice@example.com");

        //Delete the application after the test is completed
        restAPIStore.deleteApplication(applicationID);
    }

    private void approveUpdatePendingApplication(String applicationName) throws ApiException, JSONException {

        final String appUpdateWorkflowType = "AM_APPLICATION_UPDATE";
        org.wso2.am.integration.test.HttpResponse updateWorkflowsResponse =
                restAPIAdmin.getWorkflows(appUpdateWorkflowType);
        assertEquals(updateWorkflowsResponse.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        JSONObject updateWorkflowObject = new JSONObject(updateWorkflowsResponse.getData());
        String updateExternalWorkflowRef = null;

        JSONArray wfArray = updateWorkflowObject.getJSONArray("list");
        for (int i = 0; i < wfArray.length(); i++) {
            JSONObject listItem = wfArray.getJSONObject(i);
            JSONObject properties = listItem.getJSONObject("properties");
            if (properties.has("applicationName") && applicationName.equals(properties.get("applicationName"))) {
                updateExternalWorkflowRef = listItem.getString("referenceId");
                break;
            }
        }
        assertNotNull("Workflow reference is not available ", updateExternalWorkflowRef);

        updateWorkflowsResponse = restAPIAdmin.getWorkflowByExternalWorkflowReference(updateExternalWorkflowRef);
        assertEquals(updateWorkflowsResponse.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");

        JSONObject updateWorkflowDetails = new JSONObject(updateWorkflowsResponse.getData());
        JSONObject updateWorkflowProperties = updateWorkflowDetails.getJSONObject("properties");

        assertEquals(updateWorkflowProperties.getString("applicationName"), applicationName);

        assertTrue(updateWorkflowProperties.has("updates"),
                "Updates property should be present for application update workflow");

        JSONArray updatesArray = new JSONArray(updateWorkflowProperties.getString("updates"));
        assertTrue(updatesArray.length() > 0, "Updates array should not be empty");

        assertTrue(updateWorkflowProperties.has("applicationAttributes"),
                "Application attributes should be present when applicationAttributesVisibility is enabled");

        boolean externalReferenceUpdated = false;
        for (int i = 0; i < updatesArray.length(); i++) {
            JSONObject update = updatesArray.getJSONObject(i);
            if ("External Reference ID".equals(update.optString("attributeName"))
                    && "10".equals(update.optString("current"))
                    && "20".equals(update.optString("expected"))) {
                externalReferenceUpdated = true;
                break;
            }
        }
        assertTrue(externalReferenceUpdated,
                "External Reference ID update entry should be present in updates property");

        org.wso2.am.integration.test.HttpResponse updateWorkflowResponse =
                restAPIAdmin.updateWorkflowStatus(updateExternalWorkflowRef);
        assertEquals(updateWorkflowResponse.getResponseCode(), 200, "Workflow state update failed for user admin");

    }

    private void rejectUpdatePendingApplication(String applicationName) throws ApiException, JSONException {

        final String appUpdateWorkflowType = "AM_APPLICATION_UPDATE";
        org.wso2.am.integration.test.HttpResponse updateWorkflowsResponse =
                restAPIAdmin.getWorkflows(appUpdateWorkflowType);
        assertEquals(updateWorkflowsResponse.getResponseCode(),
                200, "Get Workflow Pending requests failed for User Admin");

        JSONObject updateWorkflowObject = new JSONObject(updateWorkflowsResponse.getData());
        String updateExternalWorkflowRef = null;

        JSONArray wfArray = (JSONArray) updateWorkflowObject.get("list");
        for (int i = 0; i < wfArray.length(); i++) {
            JSONObject listItem = (JSONObject) wfArray.get(i);
            JSONObject properties = (JSONObject) listItem.get("properties");
            if (properties.has("applicationName") && applicationName.equals(properties.get("applicationName"))) {
                updateExternalWorkflowRef = (String) listItem.get("referenceId");
                break;
            }
        }
        assertNotNull("Workflow reference is not available ", updateExternalWorkflowRef);

        updateWorkflowsResponse = restAPIAdmin.getWorkflowByExternalWorkflowReference(updateExternalWorkflowRef);
        assertEquals(updateWorkflowsResponse.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");
        org.wso2.am.integration.test.HttpResponse updateWorkflowResponse =
                restAPIAdmin.rejectWorkflowStatus(updateExternalWorkflowRef);
        assertEquals(updateWorkflowResponse.getResponseCode(),
                200, "Workflow state rejection failed for user admin");
    }
    @Test(groups = {"wso2.am"}, description = "Application workflow process check", dependsOnMethods =
            "testAPIWorkflowProcess", enabled = true)
    public void testApplicationDeletionWorkflowProcess() throws Exception {

        final String deletingAppName = "AppDeletionWorkflowTestAPP";
        //create Application
        Map<String, String> deletingAppAttributes = new HashMap<>();
        deletingAppAttributes.put("Department Name", "Finance");
        deletingAppAttributes.put("External Reference ID", "30");
        deletingAppAttributes.put("Technical Contact", "delete@example.com");

        HttpResponse applicationResponse = restAPIStore.createApplicationWithCustomAttribute(
                deletingAppName,
                "Default version testing application",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH,
                deletingAppAttributes);
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

        final String deletionWorkflowType = "AM_APPLICATION_DELETION";
        org.wso2.am.integration.test.HttpResponse deletionWorkflowResponse =
                restAPIAdmin.getWorkflows(deletionWorkflowType);
        assertEquals(deletionWorkflowResponse.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        JSONObject deletionWorkflowRespObj = new JSONObject(deletionWorkflowResponse.getData());
        String deletionExternalWorkflowRef = null;
        JSONArray deletionArr = deletionWorkflowRespObj.getJSONArray("list");
        for (int i = 0; i < deletionArr.length(); i++) {
            JSONObject listItem = deletionArr.getJSONObject(i);
            JSONObject properties = listItem.getJSONObject("properties");
            if (properties.has("applicationName") &&
                    deletingAppName.equals(properties.get("applicationName"))) {
                deletionExternalWorkflowRef = listItem.getString("referenceId");
                break;
            }
        }
        assertNotNull("Workflow reference is not available ", deletionExternalWorkflowRef);

        deletionWorkflowResponse =
                restAPIAdmin.getWorkflowByExternalWorkflowReference(deletionExternalWorkflowRef);
        assertEquals(deletionWorkflowResponse.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");

        JSONObject deletionWorkflowObj = new JSONObject(deletionWorkflowResponse.getData());
        JSONObject deletionWorkflowProperties = deletionWorkflowObj.getJSONObject("properties");

        assertEquals(deletionWorkflowProperties.getString("applicationName"), deletingAppName);

        assertTrue(deletionWorkflowProperties.has("applicationAttributes"),
                "Application attributes should be present when applicationAttributesVisibility is enabled");

        JSONObject deletionAttributesObj =
                new JSONObject(deletionWorkflowProperties.getString("applicationAttributes"));
        assertEquals(deletionAttributesObj.getString("Department Name"), "Finance");
        assertEquals(deletionAttributesObj.getString("External Reference ID"), "30");
        assertEquals(deletionAttributesObj.getString("Technical Contact"), "delete@example.com");

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

    @Test(groups = {"wso2.am"}, description = "Subscription update workflow process check", dependsOnMethods =
            {"testApplicationWorkflowProcess", "testAPIWorkflowProcess"})
    public void testSubscriptionUpdateWorkflowProcess() throws Exception {

        final String subUpdateTestAppName = "SubUpdateTestApp";
        String subUpdateTestAppID = null;
        String subscriptionId = null;
        String subUpdateExternalWorkflowRef = null;

        try {
            // create Application
            ApplicationDTO applicationResponse = restAPIStore.addApplication(
                    subUpdateTestAppName,
                    APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                    StringUtils.EMPTY,
                    "Default version testing application");

            assertNotNull("Application creation response should not be null", applicationResponse);
            subUpdateTestAppID = applicationResponse.getApplicationId();
            assertNotNull("Application ID should not be null", subUpdateTestAppID);

            // approve application creation workflow
            final String workflowTypeAppCreation = "AM_APPLICATION_CREATION";
            org.wso2.am.integration.test.HttpResponse response = restAPIAdmin.getWorkflows(workflowTypeAppCreation);
            assertEquals(response.getResponseCode(), 200,
                    "Get Workflow Pending requests failed for User Admin");

            JSONObject workflowRespObj = new JSONObject(response.getData());
            String externalWorkflowRef = null;
            JSONArray arr = workflowRespObj.getJSONArray("list");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject listItem = arr.getJSONObject(i);
                JSONObject properties = listItem.getJSONObject("properties");
                if (properties.has("applicationName")
                        && subUpdateTestAppName.equals(properties.getString("applicationName"))) {
                    externalWorkflowRef = listItem.getString("referenceId");
                    break;
                }
            }
            assertNotNull("Workflow reference is not available ", externalWorkflowRef);

            response = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef);
            assertEquals(response.getResponseCode(), 200,
                    "Updating application creation workflow failed for user admin");

            // create Subscription
            org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO subCreationResponse =
                    restAPIStore.subscribeToAPI(apiId, subUpdateTestAppID, APIMIntegrationConstants.API_TIER.UNLIMITED);

            assertNotNull("Subscription creation response should not be null", subCreationResponse);
            subscriptionId = subCreationResponse.getSubscriptionId();
            assertNotNull("Subscription ID should not be null", subscriptionId);

            // approve subscription creation workflow
            final String workflowTypeSubCreation = "AM_SUBSCRIPTION_CREATION";
            org.wso2.am.integration.test.HttpResponse subWorkflowResponse = restAPIAdmin.getWorkflows(workflowTypeSubCreation);
            assertEquals(subWorkflowResponse.getResponseCode(), 200,
                    "Get Workflow Pending requests failed for User Admin");

            JSONObject subCreationWorkflowRespObj = new JSONObject(subWorkflowResponse.getData());
            String subCreationExternalWorkflowRef = null;
            JSONArray subCreationWFArray = subCreationWorkflowRespObj.getJSONArray("list");
            for (int i = 0; i < subCreationWFArray.length(); i++) {
                JSONObject listItem = subCreationWFArray.getJSONObject(i);
                JSONObject properties = listItem.getJSONObject("properties");
                if (properties.has("applicationName")
                        && subUpdateTestAppName.equals(properties.getString("applicationName"))
                        && properties.has("apiName")
                        && apiName.equals(properties.getString("apiName"))) {
                    subCreationExternalWorkflowRef = listItem.getString("referenceId");
                    break;
                }
            }
            assertNotNull("Workflow reference is not available ", subCreationExternalWorkflowRef);

            subWorkflowResponse = restAPIAdmin.getWorkflowByExternalWorkflowReference(subCreationExternalWorkflowRef);
            assertEquals(subWorkflowResponse.getResponseCode(), 200,
                    "Get Workflow Pending request failed for User Admin");

            subWorkflowResponse = restAPIAdmin.updateWorkflowStatus(subCreationExternalWorkflowRef);
            assertEquals(subWorkflowResponse.getResponseCode(), 200,
                    "Updated workflow state is failed for user admin");

            Gson gsonUpdateResponse = new Gson();
            WorkflowDTO workflowDTO = gsonUpdateResponse.fromJson(subWorkflowResponse.getData(), WorkflowDTO.class);
            String workflowStatus = workflowDTO.getStatus().toString();
            assertEquals(workflowStatus, WorkflowStatus.APPROVED.toString(),
                    "Workflow state should change by the authorized admin.");

            SubscriptionListDTO subscriptionFinalListDTO = restAPIPublisher.getSubscriptionByAPIID(apiId);
            List<SubscriptionDTO> subscriptionFinalList = subscriptionFinalListDTO.getList();
            assertNotNull(subscriptionFinalList);

            SubscriptionDTO.SubscriptionStatusEnum subscriptionStatus = SubscriptionDTO.SubscriptionStatusEnum.BLOCKED;
            for (SubscriptionDTO subscriptionInfo : subscriptionFinalList) {
                if (subscriptionInfo.getApplicationInfo() != null
                        && subUpdateTestAppID.equals(subscriptionInfo.getApplicationInfo().getApplicationId())) {
                    subscriptionStatus = subscriptionInfo.getSubscriptionStatus();
                    log.info("Found valid subscription for the application");
                    break;
                }
            }

            assertEquals(subscriptionStatus, SubscriptionDTO.SubscriptionStatusEnum.UNBLOCKED,
                    "Subscription state should change after approval.");

            // update subscription
            restAPIStore.updateSubscriptionToAPI(
                    apiId,
                    subUpdateTestAppID,
                    APIMIntegrationConstants.API_TIER.UNLIMITED,
                    APIMIntegrationConstants.API_TIER.GOLD,
                    org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO.StatusEnum.UNBLOCKED,
                    subscriptionId,
                    "carbon.super"
            );

            // verify subscription update workflow created
            final String workflowTypeSubUpdate = "AM_SUBSCRIPTION_UPDATE";
            org.wso2.am.integration.test.HttpResponse subUpdateWorkflowResponse =
                    restAPIAdmin.getWorkflows(workflowTypeSubUpdate);
            assertEquals(subUpdateWorkflowResponse.getResponseCode(), 200,
                    "Get Workflow Pending requests failed for User Admin");

            JSONObject subUpdateWorkflowRespObj = new JSONObject(subUpdateWorkflowResponse.getData());
            JSONArray subUpdateWFArray = subUpdateWorkflowRespObj.getJSONArray("list");
            for (int i = 0; i < subUpdateWFArray.length(); i++) {
                JSONObject listItem = subUpdateWFArray.getJSONObject(i);
                JSONObject properties = listItem.getJSONObject("properties");
                if (properties.has("applicationName")
                        && subUpdateTestAppName.equals(properties.getString("applicationName"))
                        && properties.has("apiName")
                        && apiName.equals(properties.getString("apiName"))) {
                    subUpdateExternalWorkflowRef = listItem.getString("referenceId");
                    break;
                }
            }
            assertNotNull("Workflow reference is not available ", subUpdateExternalWorkflowRef);

            subUpdateWorkflowResponse =
                    restAPIAdmin.getWorkflowByExternalWorkflowReference(subUpdateExternalWorkflowRef);
            assertEquals(subUpdateWorkflowResponse.getResponseCode(), 200,
                    "Get Workflow Pending request failed for User Admin");

            JSONObject subUpdateWorkflowObj = new JSONObject(subUpdateWorkflowResponse.getData());
            JSONObject subUpdateWorkflowProperties = subUpdateWorkflowObj.getJSONObject("properties");

            assertEquals(subUpdateWorkflowProperties.getString("applicationName"), subUpdateTestAppName);
            assertEquals(subUpdateWorkflowProperties.getString("apiName"), apiName);

            assertTrue(subUpdateWorkflowProperties.has("updates"),
                    "Updates property should be present for subscription update workflow");

            JSONArray updatesArray = new JSONArray(subUpdateWorkflowProperties.getString("updates"));
            assertTrue(updatesArray.length() > 0,
                    "Updates array should not be empty for subscription update workflow");

            boolean tierUpdateFound = false;
            for (int i = 0; i < updatesArray.length(); i++) {
                JSONObject updateEntry = updatesArray.getJSONObject(i);
                String attributeName = updateEntry.optString("attributeName");
                if ("Subscription Tier".equals(attributeName)) {
                    assertEquals(updateEntry.getString("current"), APIMIntegrationConstants.API_TIER.UNLIMITED);
                    assertEquals(updateEntry.getString("expected"), APIMIntegrationConstants.API_TIER.GOLD);
                    tierUpdateFound = true;
                    break;
                }
            }
            assertTrue(tierUpdateFound, "Subscription tier update entry should be present");

            // approve subscription update workflow
            subUpdateWorkflowResponse = restAPIAdmin.updateWorkflowStatus(subUpdateExternalWorkflowRef);
            assertEquals(subUpdateWorkflowResponse.getResponseCode(), 200,
                    "Updated workflow state is failed for user admin");

            // verify updated tier after approval
            SubscriptionListDTO subscriptionsAfterUpdateDTO = restAPIPublisher.getSubscriptionByAPIID(apiId);
            List<SubscriptionDTO> subscriptionsAfterUpdate = subscriptionsAfterUpdateDTO.getList();
            assertNotNull(subscriptionsAfterUpdate);

            SubscriptionDTO updatedSubscription = null;

            for (SubscriptionDTO sub : subscriptionsAfterUpdate) {
                if (sub.getApplicationInfo() != null
                        && subUpdateTestAppID.equals(sub.getApplicationInfo().getApplicationId())) {
                    updatedSubscription = sub;
                    break;
                }
            }

            assertNotNull("Updated subscription should be available", updatedSubscription);
            assertEquals(updatedSubscription.getThrottlingPolicy(),
                    APIMIntegrationConstants.API_TIER.GOLD,
                    "Subscription tier should be updated after approval");

        } finally {
            // cleanup resources created by this test
            if (subUpdateTestAppID != null) {
                try {
                    HttpResponse deleteAppResponse = restAPIStore.deleteApplicationWithHttpResponse(subUpdateTestAppID);
                    if (deleteAppResponse != null && deleteAppResponse.getResponseCode() == 201) {
                        try {
                            removeDeletePendingApplication(subUpdateTestAppName);
                        } catch (AssertionError | Exception e) {
                            log.warn("Failed to approve application deletion workflow for app: " + subUpdateTestAppName, e);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to delete test application: " + subUpdateTestAppName, e);
                }
            }
        }
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
        assertEquals(subDelWorkflowResponse.getResponseCode(), 200,
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

        JSONObject registrationWorkflowObj = new JSONObject(response.getData());
        JSONObject registrationWorkflowProperties = registrationWorkflowObj.getJSONObject("properties");

        assertEquals(registrationWorkflowProperties.getString("applicationName"), applicationName);

        assertTrue(registrationWorkflowProperties.has("applicationAttributes"),
                "Application attributes should be present when applicationAttributesVisibility is enabled");

        JSONObject registrationAttributesObj =
                new JSONObject(registrationWorkflowProperties.getString("applicationAttributes"));
        assertEquals(registrationAttributesObj.getString("Department Name"), "Finance");
        assertEquals(registrationAttributesObj.getString("External Reference ID"), "10");
        assertEquals(registrationAttributesObj.getString("Technical Contact"), "bob@example.com");

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
        RestAPIStoreImpl signupUser = new RestAPIStoreImpl(username, password, "carbon.super", store);
        // Check whether user can access application list page. only signup user can access it. Pending user cannot.
        try {
            ApplicationListDTO apps = signupUser.getAllApps();
            assertFalse(true, "Signup user can access restricted pages");
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            assertTrue(true, "Signup user cannot access restricted pages");
        }

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
        signupUser = new RestAPIStoreImpl(username, password, "carbon.super", store);
        // Check whether user can access application list page. only signup user can access it. Pending user cannot.
        try {
            ApplicationListDTO apps = signupUser.getAllApps();
            assertTrue(true, "Approved signup user can access restricted pages");
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            assertFalse(true, "Approved signup user cannot access restricted pages");
        }
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
        String externalRef = getExternalRef(apiName);
        acceptDeployRequestByAdmin(externalRef);
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
        assertEquals(response.getResponseCode(), 404,
                "Clean up pending task process is failed for Application creation");
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(apiStateChangeExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 404,
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
        String externalWorkflowRef = getExternalRef(apiName);
        acceptDeployRequestByAdmin(externalRef);
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
        //check the cleanup process is successful (Application key generation, API subscription)
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(subscriptionCreationExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 404,
                "Clean up pending task process is failed for Subscription Creation");
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(keyGenerationExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 404,
                "Clean up pending task process is failed for Application Key generation");
    }

    /**
     * Test API revision deployment workflow
     * Following flows will be tested,
     * Deploy revision request will be sent and check whether it is not deployed
     * Reject request and check whether it is not deployed
     * Deploy request will be sent, admin will approve it and check whether it is deployed
     * Undeploy the revision and check whether it is undeployed
     * Deploy a revision and send another request and check whether the second request is not deployed
     * Reject the second request and check whether it is not deployed
     * Deploy the second request and check whether it is deployed and the first deployment is undeployed
     *
     * @throws Exception if an error occurs when revision deployment workflow test is running
     **/
    @Test(groups = { "wso2.am" }, description = "Testing the API Revision Deployment Workflow Process")
    public void testAPIRevisionDeploymentWorkflowProcess() throws Exception {
        //Add API
        String apiName = "WorkflowRevisionDeployment";
        String apiVersion = "1.0.0";
        String apiContext = "workflowrevision";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        APIRequest apiRequest;
        apiRequest = new APIRequest(apiName, apiContext, new URL(url));
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(USER_SMITH);
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiId2 = apiResponse.getData();
        assertEquals(apiResponse.getResponseCode(), 201,
                "API creation failed in Revision Deployment Workflow Executor");

        // Create revision and send deploy request
        String firstRevisionUUID = createAPIRevisionAndDeployUsingRest(apiId2, restAPIPublisher);

        String firstExternalWorkflowRef = getExternalRef(apiName);

        // Get workflow pending request by external workflow reference by admin
        org.wso2.am.integration.test.HttpResponse response = restAPIAdmin.getWorkflowByExternalWorkflowReference(
                firstExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Get First Workflow Pending request by external workflow reference failed for User Admin");

        // Reject workflow and check whether the revision is not deployed
        response = restAPIAdmin.rejectWorkflowStatus(firstExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 200, "First Workflow request reject failed for user admin");
        HttpResponse apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId2, "deployed:true");
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve deployed revisions" + apiRevisionsGetResponse.getData());
        JSONObject apiRevisionsGetResponseObj = new JSONObject(apiRevisionsGetResponse.getData());
        assertEquals(apiRevisionsGetResponseObj.getJSONArray("list").length(), 0, "First Revision is deployed");

        deployAPIRevisionWithWorkflow(apiId2, firstRevisionUUID, firstExternalWorkflowRef);
        apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId2, "deployed:true");
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve deployed revisions" + apiRevisionsGetResponse.getData());
        apiRevisionsGetResponseObj = new JSONObject(apiRevisionsGetResponse.getData());
        JSONArray listArray = apiRevisionsGetResponseObj.getJSONArray("list");
        // Iterate through the "list" array
        for (int i = 0; i < listArray.length(); i++) {
            JSONObject item = listArray.getJSONObject(i);
            String status = item.getJSONArray("deploymentInfo").getJSONObject(0).getString("status");
            if ("APPROVED".equals(status)) {
                assertEquals(firstRevisionUUID, item.getString("id"), "First Revision is not deployed");
            }
        }

        // Undeploy revision and check whether it is undeployed
        undeployAPIRevisionWithWorkflow(apiId2, firstRevisionUUID, firstExternalWorkflowRef);
        apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId2, "deployed:true");
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve deployed revisions" + apiRevisionsGetResponse.getData());
        apiRevisionsGetResponseObj = new JSONObject(apiRevisionsGetResponse.getData());
        assertEquals(apiRevisionsGetResponseObj.getJSONArray("list").length(), 0, "First Revision is deployed");

        // Deploy a revision then send another request and check whether the second request is not deployed
        deployAPIRevisionWithWorkflow(apiId2, firstRevisionUUID, firstExternalWorkflowRef);
        // Create Second Revision and send deploy request
        String secondRevisionUUID = createAPIRevisionAndDeployUsingRest(apiId2, restAPIPublisher);
        apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId2, "deployed:true");
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve deployed revisions" + apiRevisionsGetResponse.getData());
        apiRevisionsGetResponseObj = new JSONObject(apiRevisionsGetResponse.getData());
        listArray = apiRevisionsGetResponseObj.getJSONArray("list");
        // Iterate through the "list" array
        for (int i = 0; i < listArray.length(); i++) {
            JSONObject item = listArray.getJSONObject(i);
            String status = item.getJSONArray("deploymentInfo").getJSONObject(0).getString("status");
            if ("APPROVED".equals(status)) {
                assertEquals(firstRevisionUUID, item.getString("id"), "Second Revision is deployed");
            }
        }

        String secondExternalWorkflowRef = getExternalRef(apiName);
        apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId2, "deployed:true");
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve deployed revisions" + apiRevisionsGetResponse.getData());

        // Reject the second workflow request and check whether the second revision is not deployed
        response = restAPIAdmin.rejectWorkflowStatus(secondExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 200, "Workflow request update failed for user admin");
        apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId2, "deployed:true");
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve deployed revisions" + apiRevisionsGetResponse.getData());
        apiRevisionsGetResponseObj = new JSONObject(apiRevisionsGetResponse.getData());
        listArray = apiRevisionsGetResponseObj.getJSONArray("list");
        // Iterate through the "list" array
        for (int i = 0; i < listArray.length(); i++) {
            JSONObject item = listArray.getJSONObject(i);
            String status = item.getJSONArray("deploymentInfo").getJSONObject(0).getString("status");
            if ("APPROVED".equals(status)) {
                Assert.assertNotEquals(secondRevisionUUID, item.getString("id"), "Second Revision is deployed");
            }
        }

        // Deploy the second revision and check whether the first revision is undeployed
        deployAPIRevisionWithWorkflow(apiId2, secondRevisionUUID, secondExternalWorkflowRef);
        apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId2, "deployed:true");
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve deployed revisions" + apiRevisionsGetResponse.getData());
        apiRevisionsGetResponseObj = new JSONObject(apiRevisionsGetResponse.getData());
        listArray = apiRevisionsGetResponseObj.getJSONArray("list");
        // Iterate through the "list" array
        for (int i = 0; i < listArray.length(); i++) {
            JSONObject item = listArray.getJSONObject(i);
            String status = item.getJSONArray("deploymentInfo").getJSONObject(0).getString("status");
            if ("APPROVED".equals(status)) {
                Assert.assertNotEquals(firstRevisionUUID, item.getString("id"), "First Revision is deployed");
                assertEquals(secondRevisionUUID, item.getString("id"), "Second Revision is not deployed");
            }
        }
    }

    /**
     * Get the external reference for AM_REVISION_DEPLOYMENT workflow
     *
     * @param apiName        API Name
     * @return external reference
     * @throws Exception if an error occurs when deploying a revision
     **/
    private String getExternalRef(String apiName) throws Exception {
        String workflowType = "AM_REVISION_DEPLOYMENT";
        // Get workflow pending requests by admin
        org.wso2.am.integration.test.HttpResponse response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200, "Get Workflow Pending requests failed for User Admin");

        // Get the externalReference of the workflow
        JSONObject workflowRespObj = new JSONObject(response.getData());
        String externalWorkflowRef = null;
        JSONArray revisionArray = (JSONArray) workflowRespObj.get("list");
        for (int item = 0; item < revisionArray.length(); item++) {
            JSONObject revision = (JSONObject) revisionArray.get(item);
            JSONObject properties = (JSONObject) revision.get("properties");
            if (properties.has("apiName") && apiName.equals(properties.get("apiName"))) {
                externalWorkflowRef = (String) revision.get("referenceId");
            }
        }
        assertNotNull("External Workflow reference is not available ", externalWorkflowRef);
        return externalWorkflowRef;
    }

    /**
     * Deploy a revision when revision deployment workflow is active
     *
     * @param apiId        API Id
     * @param revisionUUID Revision UUID
     * @param externalRef  External reference
     * @throws Exception if an error occurs when deploying a revision
     **/
    private void deployAPIRevisionWithWorkflow(String apiId, String revisionUUID, String externalRef) throws Exception {
        // Send deploy request, approve and check whether it is deployed
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIRevision(apiId, revisionUUID,
                apiRevisionDeployRequestList, "API");
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to deploy API Revisions:" + apiRevisionsDeployResponse.getData());
        org.wso2.am.integration.test.HttpResponse response = restAPIAdmin.updateWorkflowStatus(externalRef);
        assertEquals(response.getResponseCode(), 200, "First Workflow request update failed for user admin");
        acceptDeployRequestByAdmin(externalRef);
    }

    /**
     * Accept the deployment workflow request from the admin side
     *
     * @param externalRef  External reference
     * @throws Exception if an error occurs when deploying a revision
     **/
    private void acceptDeployRequestByAdmin(String externalRef) throws Exception {
        org.wso2.am.integration.test.HttpResponse response = restAPIAdmin.updateWorkflowStatus(externalRef);
        assertEquals(response.getResponseCode(), 200, "First Workflow request update failed for user admin");
    }

    /**
     * Undeploy a revision when revision deployment workflow is active
     *
     * @param apiId        API Id
     * @param revisionUUID Revision UUID
     * @param externalRef  External reference
     * @throws Exception if an error occurs when undeploying a revision
     **/
    private void undeployAPIRevisionWithWorkflow(String apiId, String revisionUUID, String externalRef)
            throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.undeployAPIRevision(apiId, revisionUUID,
                apiRevisionDeployRequestList);
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to deploy API Revisions:" + apiRevisionsDeployResponse.getData());
        org.wso2.am.integration.test.HttpResponse response = restAPIAdmin.updateWorkflowStatus(externalRef);
        assertEquals(response.getResponseCode(), 200, "First Workflow request update failed for user admin");
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationID);
        removeDeletePendingApplication(applicationName);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(apiId2);
        userManagementClient.deleteUser(USER_SMITH);
        userManagementClient.deleteUser(USER_ADMIN);
        userManagementClient.deleteUser("JaneDoe");
        resourceAdminServiceClient.updateTextContent(DEFAULT_WF_EXTENTIONS_XML_REG_CONFIG_LOCATION, originalWFExtentionsXML);
        super.cleanUp();
    }
}
