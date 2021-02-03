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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.clients.admin.api.dto.WorkflowDTO;
import org.wso2.am.integration.clients.admin.api.dto.WorkflowInfoDTO;
import org.wso2.am.integration.clients.admin.api.dto.WorkflowListDTO;

import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.UserManagementUtils;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.clients.AdminDashboardRestClient;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;

import org.wso2.carbon.apimgt.api.WorkflowStatus;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import static org.testng.Assert.assertEquals;
import static org.wso2.am.integration.test.utils.base.APIMIntegrationConstants.SUPER_TENANT_DOMAIN;

public class WorkflowApprovalExecutorTest extends APIManagerLifecycleBaseTest {

    private UserManagementClient userManagementClient = null;
    protected String user;
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
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private RestAPIStoreImpl APIStoreClient;

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

        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());
    }

    @Test(groups = {"wso2.am"}, description = "Api workflow process check")
    public void testAPIWorkflowProcess() throws Exception {

        //add API
        String apiName = "WorkflowCheckAPI";
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
        Gson gsonResponse = new Gson();
        WorkflowListDTO workflowListDTO = gsonResponse.fromJson(jsonResponse, WorkflowListDTO.class);
        List<WorkflowInfoDTO> list = new ArrayList<WorkflowInfoDTO>();
        list = workflowListDTO.getList();
        String externalWorkflowRef = "";
        for (WorkflowInfoDTO workflowinfo : list) {
            externalWorkflowRef = workflowinfo.getReferenceId();
        }

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

    @Test(groups = {"wso2.am"}, description = "Application workflow process check", dependsOnMethods =
            "testAPIWorkflowProcess", enabled = true)
    public void testApplicationWorkflowProcess() throws Exception {

        //create Application
        HttpResponse applicationResponse = restAPIStore.createApplication("ThisApp",
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
        Gson gsonResponse = new Gson();
        WorkflowListDTO workflowListDTO = gsonResponse.fromJson(jsonResponse, WorkflowListDTO.class);
        List<WorkflowInfoDTO> list = new ArrayList<WorkflowInfoDTO>();
        list = workflowListDTO.getList();
        String externalWorkflowRef = "";
        for (WorkflowInfoDTO workflowinfo : list) {
            externalWorkflowRef = workflowinfo.getReferenceId();
        }

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

    @Test(groups = {"wso2.am"}, description = "Subscription workflow process check", dependsOnMethods =
            "testApplicationWorkflowProcess")
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
            SubscriptionStatus = subscriptioninfo.getSubscriptionStatus();
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
        Gson gsonResponse = new Gson();
        WorkflowListDTO workflowListDTO = gsonResponse.fromJson(jsonResponse, WorkflowListDTO.class);
        List<WorkflowInfoDTO> list = new ArrayList<WorkflowInfoDTO>();
        list = workflowListDTO.getList();
        String externalWorkflowRef = "";
        for (WorkflowInfoDTO workflowinfo : list) {
            externalWorkflowRef = workflowinfo.getReferenceId();
        }

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
            SubscriptionStatus = subscriptioninfo.getSubscriptionStatus();
        }

        //Subscription state should change as UNBLOCKED
        assertEquals(SubscriptionStatus, SubscriptionDTO.SubscriptionStatusEnum.UNBLOCKED,
                "Subscription state should change after approval. ");
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
        Gson gsonResponse = new Gson();
        WorkflowListDTO workflowListDTO = gsonResponse.fromJson(jsonResponse, WorkflowListDTO.class);
        List<WorkflowInfoDTO> list = new ArrayList<WorkflowInfoDTO>();
        list = workflowListDTO.getList();
        String externalWorkflowRef = "";
        for (WorkflowInfoDTO workflowinfo : list) {
            externalWorkflowRef = workflowinfo.getReferenceId();
        }

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
        Gson gsonResponse = new Gson();
        WorkflowListDTO workflowListDTO = gsonResponse.fromJson(jsonResponse, WorkflowListDTO.class);
        List<WorkflowInfoDTO> list = new ArrayList<WorkflowInfoDTO>();
        list = workflowListDTO.getList();
        String externalWorkflowRef = "";
        for (WorkflowInfoDTO workflowinfo : list) {
            externalWorkflowRef = workflowinfo.getReferenceId();
        }

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

        //create application
        HttpResponse applicationResponse = restAPIStore.createApplication("MyApp",
                "Testing application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        String applicationIDNew = applicationResponse.getData();
        //create api
        String apiName = "WorkflowCheckingAPI";
        String apiVersion = "1.0.0";
        String apiContext = "workflowChecking";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";

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
        Gson gsonAPIStateChangeGetResponse = new Gson();
        WorkflowListDTO workflowListDTO = gsonAPIStateChangeGetResponse.fromJson(jsonAPIStateChangeGetResponse,
                WorkflowListDTO.class);
        List<WorkflowInfoDTO> apiStateChangelist = new ArrayList<WorkflowInfoDTO>();
        apiStateChangelist = workflowListDTO.getList();
        String apiStateChangeExternalWorkflowRef = "";
        for (WorkflowInfoDTO workflowinfo : apiStateChangelist) {
            apiStateChangeExternalWorkflowRef = workflowinfo.getReferenceId();
        }

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
        Gson gsonApplicationCreationGetResponse = new Gson();
        workflowListDTO = gsonApplicationCreationGetResponse.fromJson(jsonApplicationCreationGetResponse,
                WorkflowListDTO.class);
        List<WorkflowInfoDTO> applicationCreationlist = new ArrayList<WorkflowInfoDTO>();
        applicationCreationlist = workflowListDTO.getList();
        String applicationCreationExternalWorkflowRef = "";
        for (WorkflowInfoDTO workflowinfo : applicationCreationlist) {
            applicationCreationExternalWorkflowRef = workflowinfo.getReferenceId();
        }

        //get workflow pending request by external workflow reference by admin
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(applicationCreationExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");
        undeployAndDeleteAPIRevisionsUsingRest(apiIdNew, restAPIPublisher);
        //clean up process for API and application
        restAPIPublisher.deleteAPI(apiIdNew);
        restAPIStore.deleteApplication(applicationIDNew);
        //check workflow pending requests are cleaned (Application Creation, API State Change)
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(applicationCreationExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 500,
                "Clean up pending task process is failed for Application creation");
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(apiStateChangeExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 500,
                "Clean up pending task process is failed for API state change");

        //create Application
        HttpResponse applicationResponseNew = restAPIStore.createApplication("MyApp",
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
        Gson gsonAPIStateGetResponse = new Gson();
        workflowListDTO = gsonAPIStateGetResponse.fromJson(jsonAPIStateGetResponse, WorkflowListDTO.class);
        List<WorkflowInfoDTO> apiStateChangenewlist = new ArrayList<WorkflowInfoDTO>();
        apiStateChangenewlist = workflowListDTO.getList();
        String apiStateChangeNewExternalWorkflowRef = "";
        for (WorkflowInfoDTO workflowinfo : apiStateChangenewlist) {
            apiStateChangeNewExternalWorkflowRef = workflowinfo.getReferenceId();
        }

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
        Gson gsonApplicationCreation = new Gson();
        workflowListDTO = gsonApplicationCreation.fromJson(jsonApplicationCreation, WorkflowListDTO.class);
        List<WorkflowInfoDTO> applicationCreationNewlist = new ArrayList<WorkflowInfoDTO>();
        applicationCreationNewlist = workflowListDTO.getList();
        String applicationCreationNewExternalWorkflowRef = "";
        for (WorkflowInfoDTO workflowinfo : applicationCreationNewlist) {
            applicationCreationNewExternalWorkflowRef = workflowinfo.getReferenceId();
        }

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
        Gson gsonSubscriptionCreationGet = new Gson();
        workflowListDTO = gsonSubscriptionCreationGet.fromJson(jsonSubscriptionCreationGet, WorkflowListDTO.class);
        List<WorkflowInfoDTO> subscriptionCreationlist = new ArrayList<WorkflowInfoDTO>();
        subscriptionCreationlist = workflowListDTO.getList();
        String subscriptionCreationExternalWorkflowRef = "";
        for (WorkflowInfoDTO workflowinfo : subscriptionCreationlist) {
            subscriptionCreationExternalWorkflowRef = workflowinfo.getReferenceId();
        }

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
        Gson gsonKeyGenerationGetResponse = new Gson();
        workflowListDTO = gsonKeyGenerationGetResponse.fromJson(jsonKeyGenerationGetResponse, WorkflowListDTO.class);
        List<WorkflowInfoDTO> keyGenerationlist = new ArrayList<WorkflowInfoDTO>();
        keyGenerationlist = workflowListDTO.getList();
        String keyGenerationExternalWorkflowRef = "";
        for (WorkflowInfoDTO workflowinfo : keyGenerationlist) {
            keyGenerationExternalWorkflowRef = workflowinfo.getReferenceId();
        }

        //get workflow pending request by external workflow reference
        response = restAPIAdmin.getWorkflowByExternalWorkflowReference(keyGenerationExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");
        //clean up process for all pending requests
        restAPIStore.deleteApplication(applicationIDSecond);
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

    @Test(groups = {"wso2.am"}, description = "pending tasks workflow process check", dependsOnMethods =
            "testCleanUpWorkflowProcess")
    public void testPendingTaskWorkflowProcess() throws Exception {

        //create application
        HttpResponse applicationResponse = restAPIStore.createApplication("AppApp",
                "Testing application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        String applicationIDNew = applicationResponse.getData();
        //get workflow pending requests by admin
        String workflowType = null;
        org.wso2.am.integration.test.HttpResponse response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String jsonResponse = response.getData();
        Gson gsonResponse = new Gson();
        WorkflowListDTO workflowListDTO = gsonResponse.fromJson(jsonResponse, WorkflowListDTO.class);
        int count = workflowListDTO.getCount();
        assertEquals(count, 1,
                "Correct Number of Workflow requests never received");
        List<WorkflowInfoDTO> firstlist = new ArrayList<WorkflowInfoDTO>();
        firstlist = workflowListDTO.getList();
        String firstExternalWorkflowRef = "";
        for (WorkflowInfoDTO workflowinfo : firstlist) {
            firstExternalWorkflowRef = workflowinfo.getReferenceId();
        }

        //create API and request to publish API
        String apiName = "WorkflowCheckCount";
        String apiVersion = "1.0.0";
        String apiContext = "workflowCheckCount";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";

        APIRequest apiRequest;
        apiRequest = new APIRequest(apiName, apiContext, new URL(url));
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(USER_SMITH);
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        String apiIdFirst = apiResponse.getData();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiIdFirst, restAPIPublisher);
        HttpResponse lifeCycleChangeResponse = restAPIPublisher
                .changeAPILifeCycleStatus(apiIdFirst, APILifeCycleAction.PUBLISH.getAction(), null);

        //get workflow pending requests by admin
        response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        jsonResponse = response.getData();
        workflowListDTO = gsonResponse.fromJson(jsonResponse, WorkflowListDTO.class);
        count = workflowListDTO.getCount();
        assertEquals(count, 2,
                "Correct Number of Workflow requests never received");
        List<WorkflowInfoDTO> secondlist = new ArrayList<WorkflowInfoDTO>();
        secondlist = workflowListDTO.getList();
        String secondExternalWorkflowRef = "";

        //update workflow status by user admin
        response = restAPIAdmin.updateWorkflowStatus(firstExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Workflow request can only be viewed for the admin");

        for (WorkflowInfoDTO workflowinfo : secondlist) {
            secondExternalWorkflowRef = workflowinfo.getReferenceId();
        }

        //upload workflow status by user admin
        response = restAPIAdmin.updateWorkflowStatus(secondExternalWorkflowRef);
        assertEquals(response.getResponseCode(), 200,
                "Workflow request can only be viewed for the admin");
        //create subscription
        HttpResponse SubscribeResponse = restAPIStore.createSubscription(apiIdFirst, applicationIDNew,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        subscriptionId = SubscribeResponse.getData();
        assertEquals(SubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful");
        //get workflow pending requests for user admin
        response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        jsonResponse = response.getData();
        workflowListDTO = gsonResponse.fromJson(jsonResponse, WorkflowListDTO.class);
        count = workflowListDTO.getCount();
        assertEquals(count, 1,
                "Correct Number of Workflow requests never received");

        //generate keys
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationIDNew,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        //get workflow pending requests by admin
        response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        jsonResponse = response.getData();
        workflowListDTO = gsonResponse.fromJson(jsonResponse, WorkflowListDTO.class);
        count = workflowListDTO.getCount();
        assertEquals(count, 2,
                "Correct Number of Workflow requests never received");

        //delete application and delete API clean up pending task process
        restAPIStore.deleteApplication(applicationIDNew);
        undeployAndDeleteAPIRevisionsUsingRest(apiIdFirst, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiIdFirst);
        //get workflow pending requests by admin
        response = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        jsonResponse = response.getData();
        workflowListDTO = gsonResponse.fromJson(jsonResponse, WorkflowListDTO.class);
        count = workflowListDTO.getCount();
        assertEquals(count, 0,
                "Correct Number of Workflow requests never received");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationID);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        userManagementClient.deleteUser(USER_SMITH);
        userManagementClient.deleteUser(USER_ADMIN);
        userManagementClient.deleteUser("JaneDoe");
        resourceAdminServiceClient.updateTextContent(DEFAULT_WF_EXTENTIONS_XML_REG_CONFIG_LOCATION, originalWFExtentionsXML);
        super.cleanUp();
    }
}
