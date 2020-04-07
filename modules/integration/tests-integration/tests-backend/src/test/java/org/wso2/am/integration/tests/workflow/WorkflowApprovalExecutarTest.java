package org.wso2.am.integration.tests.workflow;

import java.io.File;

import com.google.gson.Gson;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.clients.admin.api.v1.dto.WorkflowDTO;
import org.wso2.am.integration.clients.admin.api.v1.dto.WorkflowInfoDTO;
import org.wso2.am.integration.clients.admin.api.v1.dto.WorkflowListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.UserManagementUtils;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.clients.AdminDashboardRestClient;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.apimgt.api.WorkflowStatus;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;


import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.net.URL;
import java.util.*;

import static org.testng.Assert.assertEquals;
import static org.wso2.am.integration.test.utils.base.APIMIntegrationConstants.SUPER_TENANT_DOMAIN;

public class WorkflowApprovalExecutarTest extends APIManagerLifecycleBaseTest {

    private UserManagementClient userManagementClient1 = null;
    protected String user;
    private String originalWFExtentionsXML;
    private String newWFExtentionsXML;
    private String USER_SMITH = "smith";
    private String ADMIN_ROLE = "admin";
    private String USER_ADMIN= "adminadmin";
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
    private RestAPIAdminImpl restAPIStoreFirstUser;
    private static final Log log = LogFactory.getLog(WorkflowApprovalExecutarTest.class);
    private APIIdentifier apiIdentifier;
    private  AdminDashboardRestClient adminDashboardRestClient;
    private String apiId;
    private String applicationID;
    private String subscriptionId;
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private RestAPIStoreImpl restAPIStoreClient3;



    @Factory(dataProvider = "userModeDataProvider")
    public WorkflowApprovalExecutarTest(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN }
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        adminDashboardRestClient = new AdminDashboardRestClient(getPublisherURLHttps());

        userManagementClient1 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());


        userManagementClient1.addUser(USER_SMITH, "john123", new String[]{INTERNAL_ROLE_SUBSCRIBER}, USER_SMITH);
        userManagementClient1.addUser(USER_ADMIN, "admin", new String[]{ALLOWED_ROLE}, ADMIN_ROLE);


        resourceAdminServiceClient = new ResourceAdminServiceClient(gatewayContextMgt.getContextUrls().getBackEndUrl(),
                createSession(gatewayContextMgt));
        // Gets the original workflow-extentions.xml file's content from the registry.
        originalWFExtentionsXML = resourceAdminServiceClient
                .getTextContent(DEFAULT_WF_EXTENTIONS_XML_REG_CONFIG_LOCATION);
        // Gets the new configuration of the workflow-extentions.xml
        newWFExtentionsXML = readFile(getAMResourceLocation() + File.separator + "configFiles" + File.separator
                + "approveWorkflow" + File.separator + "workflow-extentions.xml");
        // Updates the content of the workflow-extentions.xml of the registry file, to have the new configurations.
        resourceAdminServiceClient.updateTextContent(DEFAULT_WF_EXTENTIONS_XML_REG_CONFIG_LOCATION, newWFExtentionsXML);

        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());
    }

    @Test(groups = { "wso2.am" }, description = "Api workflow process check")
    public void apiWorkflowProcessCheck() throws Exception {

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
        apiId= apiResponse.getData();
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

        //Get workflow pending requests by admin
        String workflowType = "AM_API_STATE";
        HttpResponse response1 = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response1.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        //Get workflow pending requests by unauthorized user
        restAPIStoreFirstUser = new RestAPIAdminImpl(USER_SMITH, "john123", "carbon.super" , "https://localhost:9943/");
        HttpResponse response2 = restAPIStoreFirstUser.getWorkflows(workflowType);
        assertEquals(response2.getResponseCode(), 401,
                "Workflow requests can only be viewed for the admin");

        String json = response1.getData();
        Gson g = new Gson();
        WorkflowListDTO workflowListDTO = g.fromJson(json , WorkflowListDTO.class);
        List<WorkflowInfoDTO> list = new ArrayList<WorkflowInfoDTO>();
        list = workflowListDTO.getList();
        String externalWorkflowRef="";

        for(WorkflowInfoDTO workflowinfo: list) {
            externalWorkflowRef = workflowinfo.getReferenceId();
        }

        //get workflow pending request by external workflow reference by admin
        HttpResponse response3 = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response3.getResponseCode(), 200,
                "Get Workflow Pending request by external workflow reference failed for User Admin");

        //get workflow pending request by external workflow reference by unauthorized user
        HttpResponse response4 = restAPIStoreFirstUser.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response4.getResponseCode(), 401,
                "Workflow request can only be viewed for the admin");

        //update workflow pending request by unauthorized user
        HttpResponse response5 = restAPIStoreFirstUser.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response5.getResponseCode(), 401,
                "Workflow request can only be updated by the admin");

        //update workflow pending request by admin
        HttpResponse response6 = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response6.getResponseCode(), 200,
                "Workflow request update failed for user admin");

        String json1 = response6.getData();
        Gson g1 = new Gson();
        WorkflowDTO workflowDTO = g1.fromJson(json1 , WorkflowDTO.class);
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

    @Test(groups = { "wso2.am" }, description = "Application workflow process check" ,dependsOnMethods = "apiWorkflowProcessCheck",enabled = true)
    public void applicationWorkflowProcessCheck() throws Exception {

        //create Application
        HttpResponse applicationResponse = restAPIStore.createApplication("ThisApp",
                "Default version testing application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        applicationID = applicationResponse.getData();
        assertEquals(applicationResponse.getResponseCode(), 200,
                "Application Creation test failed in Approval Workflow Executor");

        //Application State should be CREATED
        ApplicationDTO appResponse=restAPIStore.getApplicationById(applicationID);
        String status1 = appResponse.getStatus();
        assertEquals(status1, "CREATED",
                "Application state should remain without changing till approval. ");

        //get workflow pending requests by unauthorized user
        String workflowType = "AM_APPLICATION_CREATION";
        HttpResponse response1 = restAPIStoreFirstUser.getWorkflows(workflowType);
        assertEquals(response1.getResponseCode(), 401,
                "Workflow requests an only view by Admin");

        //get workflow pending requests by Admin
        HttpResponse response2 = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response2.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String json = response2.getData();
        Gson g = new Gson();
        WorkflowListDTO workflowListDTO = g.fromJson(json , WorkflowListDTO.class);
        List<WorkflowInfoDTO> list = new ArrayList<WorkflowInfoDTO>();
        list = workflowListDTO.getList();
        String externalWorkflowRef="";

        for(WorkflowInfoDTO workflowinfo: list) {
            externalWorkflowRef = workflowinfo.getReferenceId();
        }

        //get workflow pending requests by external workflow reference by Admin
        HttpResponse response3 = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response3.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");

        //get workflow pending requests by external workflow reference by unauthorized user
        HttpResponse response4 = restAPIStoreFirstUser.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response4.getResponseCode(), 401,
                "Workflow request can only be viewed for the admin");

        //update workflow pending request by unauthorized user
        HttpResponse response5 = restAPIStoreFirstUser.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response5.getResponseCode(), 401,
                "Workflow request can only be updated by the admin");

        //update workflow pending request by admin
        HttpResponse response6 = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response6.getResponseCode(), 200,
                "Workflow state update failed for user admin");

        String json1 = response6.getData();
        Gson g1 = new Gson();
        WorkflowDTO workflowDTO = g1.fromJson(json1 , WorkflowDTO.class);
        String status = workflowDTO.getStatus().toString();

        //Workflow status should be changed as APPROVED
        assertEquals(status, WorkflowStatus.APPROVED.toString(),
                "Workflow state should change by the authorized admin. ");

        //Application status should be changed as APPROVED
        ApplicationDTO appResponse1=restAPIStore.getApplicationById(applicationID);
        String status2 = appResponse1.getStatus();
        assertEquals(status2, "APPROVED",
                "Application state should change after  approval. ");

    }

    @Test(groups = { "wso2.am" }, description = "Subscription workflow process check" , dependsOnMethods = "applicationWorkflowProcessCheck")
    public void subscriptionWorkflowProcessCheck() throws Exception {

        //create Subscription
        HttpResponse SubscribeResponse = restAPIStore.createSubscription(apiId, applicationID, APIMIntegrationConstants.API_TIER.UNLIMITED);
        subscriptionId = SubscribeResponse.getData();
        assertEquals(SubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of API is not successful");

        SubscriptionListDTO subscriptionListDTO=restAPIPublisher.getSubscriptionByAPIID(apiId);
        List<SubscriptionDTO> subscriptionList = new ArrayList<SubscriptionDTO>();
        subscriptionList = subscriptionListDTO.getList();
        SubscriptionDTO.SubscriptionStatusEnum SubscriptionStatus = SubscriptionDTO.SubscriptionStatusEnum.BLOCKED;

        for(SubscriptionDTO subscriptioninfo: subscriptionList) {
            SubscriptionStatus =subscriptioninfo.getSubscriptionStatus();
        }

        //Subscription Status should not change
        assertEquals(SubscriptionStatus,SubscriptionDTO.SubscriptionStatusEnum.ON_HOLD,
                "Subscription state should remain without changing till approval. ");

        //get pending workflow requests by unauthorized user
        String workflowType = "AM_SUBSCRIPTION_CREATION";
        HttpResponse response1 = restAPIStoreFirstUser.getWorkflows(workflowType);
        assertEquals(response1.getResponseCode(), 401,
                "Workflow Pending requests can only viewed for User Admin");

        //get pending workflow requests by Admin
        HttpResponse response2 = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response2.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String json = response2.getData();
        Gson g = new Gson();
        WorkflowListDTO workflowListDTO = g.fromJson(json , WorkflowListDTO.class);
        List<WorkflowInfoDTO> list = new ArrayList<WorkflowInfoDTO>();
        list = workflowListDTO.getList();
        String externalWorkflowRef="";

        for(WorkflowInfoDTO workflowinfo: list) {
            externalWorkflowRef = workflowinfo.getReferenceId();
        }

        //get pending workflow request by ExternalWorkflow Reference by Admin
        HttpResponse response3 = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response3.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");

        //get pending workflow request by ExternalWorkflow Reference by unauthorized user
        HttpResponse response4 = restAPIStoreFirstUser.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response4.getResponseCode(), 401,
                "Workflow request can only be viewed for the admin");

        //update pending workflow request by ExternalWorkflow Reference by unauthorized user
        HttpResponse response5 = restAPIStoreFirstUser.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response5.getResponseCode(), 401,
                "Workflow request can only be updated by the admin");

        //update pending workflow requests by ExternalWorkflow Reference by Admin
        HttpResponse response6 = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response6.getResponseCode(), 200,
                "Updated workflow state is failed for user admin");

        String json1 = response6.getData();
        Gson g1 = new Gson();
        WorkflowDTO workflowDTO = g1.fromJson(json1 , WorkflowDTO.class);
        String status = workflowDTO.getStatus().toString();

        //Workflow status should change to APPROVED
        assertEquals(status, WorkflowStatus.APPROVED.toString(),
                "Workflow state should change by the authorized admin. ");

        SubscriptionListDTO subscriptionListDTO1=restAPIPublisher.getSubscriptionByAPIID(apiId);
        List<SubscriptionDTO> subscriptionList1 = new ArrayList<SubscriptionDTO>();
        subscriptionList1 = subscriptionListDTO1.getList();

        for(SubscriptionDTO subscriptioninfo: subscriptionList1) {
            SubscriptionStatus =subscriptioninfo.getSubscriptionStatus();
        }

        //Subscription state should change as UNBLOCKED
        assertEquals(SubscriptionStatus,SubscriptionDTO.SubscriptionStatusEnum.UNBLOCKED,
                "Subscription state should change after approval. ");

    }

    @Test(groups = { "wso2.am" }, description = "Registration workflow process check" , dependsOnMethods = "subscriptionWorkflowProcessCheck")
    public void registrationWorkflowProcessCheck() throws Exception {

        //generate keys
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationID,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes );

        //application key generation state should not change
        String keyState=applicationKeyDTO.getKeyState();
        assertEquals(keyState, "CREATED",
                "Application key generation should not change until approval");

        //get pending workflow requests by unauthorized user
        String workflowType = "AM_APPLICATION_REGISTRATION_PRODUCTION";
        HttpResponse response1 = restAPIStoreFirstUser.getWorkflows(workflowType);
        assertEquals(response1.getResponseCode(), 401,
                "Workflow pending requests can only view by admin");

        //get pending workflow requests by admin
        HttpResponse response2 = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response2.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String json = response2.getData();
        Gson g = new Gson();
        WorkflowListDTO workflowListDTO = g.fromJson(json , WorkflowListDTO.class);
        List<WorkflowInfoDTO> list = new ArrayList<WorkflowInfoDTO>();
        list = workflowListDTO.getList();

        String externalWorkflowRef="";

        for(WorkflowInfoDTO workflowinfo: list) {
            externalWorkflowRef = workflowinfo.getReferenceId();
        }

        //get pending workflow requests by external workflow reference by admin
        HttpResponse response3 = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response3.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");

        //get pending workflow requests by external workflow reference by unauthorized user
        HttpResponse response4 = restAPIStoreFirstUser.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response4.getResponseCode(), 401,
                "Workflow request can only be viewed for the admin");

        //update pending workflow requests  by unauthorized user
        HttpResponse response5 = restAPIStoreFirstUser.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response5.getResponseCode(), 401,
                "Workflow request can only be updated for the admin");

        //update pending workflow requests  by admin
        HttpResponse response6 = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response6.getResponseCode(), 200,
                "failed to update the workflow request for user admin");

        String json1 = response6.getData();
        Gson g1 = new Gson();
        WorkflowDTO workflowDTO = g1.fromJson(json1 , WorkflowDTO.class);
        String status = workflowDTO.getStatus().toString();

        //workflow status should change to APPROVED
        assertEquals(status, WorkflowStatus.APPROVED.toString(),
                "Workflow state should change by the authorized admin. ");

        ApiResponse<ApplicationKeyDTO> apiresponse= restAPIStore.getApplicationKeysByKeyType(applicationID,"PRODUCTION");
        ApplicationKeyDTO applicationKeyData=apiresponse.getData();

        //key generation state should change to COMPLETED
        assertEquals(applicationKeyData.getKeyState(), "COMPLETED",
                "Application key generation stat should change after approval");

    }

    @Test(groups = { "wso2.am" }, description = "User Sign Up workflow process check" , dependsOnMethods = "registrationWorkflowProcessCheck")
    public void userSignUpWorkflowProcessCheck() throws Exception {

        String giveName="Sahan12";
        String username="SahanHerath";
        String password="admin";
        String organization="wso2";
        String email= "sahand.herath@gmail.com";
        String storeURLHttp1 = "https://localhost:9943/";

        //User self sign up process
        UserManagementUtils.signupUser(username, password, giveName, organization,email);
        HttpResponse respo = restAPIAdmin.getWorkflows("AM_USER_SIGNUP");

        restAPIStoreClient3 = new
                RestAPIStoreImpl(username, password, SUPER_TENANT_DOMAIN, storeURLHttp1);

        //get workflow pending requests by unauthorized user
        String workflowType = "AM_USER_SIGNUP";
        HttpResponse response1 = restAPIStoreFirstUser.getWorkflows(workflowType);
        assertEquals(response1.getResponseCode(), 401,
                "Workflow requests can only viewed by admin");

        //get workflow pending requests by admin
        HttpResponse response2 = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response2.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String json = response2.getData();
        Gson g = new Gson();
        WorkflowListDTO workflowListDTO = g.fromJson(json , WorkflowListDTO.class);
        List<WorkflowInfoDTO> list = new ArrayList<WorkflowInfoDTO>();
        list = workflowListDTO.getList();

        String externalWorkflowRef="";

        for(WorkflowInfoDTO workflowinfo: list) {
            externalWorkflowRef = workflowinfo.getReferenceId();
        }

        //get workflow pending requests by external workflow reference by admin
        HttpResponse response3 = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response3.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");

        //get workflow pending requests by external workflow reference by unauthorized user
        HttpResponse response4 = restAPIStoreFirstUser.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response4.getResponseCode(), 401,
                "Workflow request can only be viewed for the admin");

        //update workflow pending requests  by unauthorized user
        HttpResponse response5 = restAPIStoreFirstUser.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response5.getResponseCode(), 401,
                "Workflow request can only be updated by the the admin");

        //update workflow pending requests  by Admin
        HttpResponse response6 = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response6.getResponseCode(), 200,
                "Failed to update Workflow request for user admin");

        String json1 = response6.getData();
        Gson g1 = new Gson();
        WorkflowDTO workflowDTO = g1.fromJson(json1 , WorkflowDTO.class);
        String status = workflowDTO.getStatus().toString();

        //Workflow state should change after approval
        assertEquals(status, WorkflowStatus.APPROVED.toString(),
                "Workflow state should change by the authorized admin. ");

        //Loin in to developer portal
        HttpResponse loginResponse= apiStore.login(username,password);
        assertEquals(loginResponse.getResponseCode(), 302,
                "Failed to login to developer portal");
    }

    @Test(groups = { "wso2.am" }, description = "clean up workflow process check", dependsOnMethods = "userSignUpWorkflowProcessCheck")
    public void cleanUpWorkflowProcessCheck() throws Exception {

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

        //request to publish the API
        String apiIdNew= apiResponse.getData();
        HttpResponse lifeCycleChangeResponse = restAPIPublisher
                .changeAPILifeCycleStatus(apiIdNew, APILifeCycleAction.PUBLISH.getAction(), null);

        //get pending workflow requests of API state change
        String workflowType = "AM_API_STATE";
        HttpResponse response1 = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response1.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String json = response1.getData();

        Gson g = new Gson();
        WorkflowListDTO workflowListDTO = g.fromJson(json , WorkflowListDTO.class);
        List<WorkflowInfoDTO> list = new ArrayList<WorkflowInfoDTO>();
        list = workflowListDTO.getList();

        String externalWorkflowRef="";

        for(WorkflowInfoDTO workflowinfo: list) {
            externalWorkflowRef = workflowinfo.getReferenceId();
        }

        //get workflow pending request by external workflow reference by admin
        HttpResponse response3 = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response3.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");

        //get workflow pending requests of Application creation
        String workflowType1 = "AM_APPLICATION_CREATION";
        HttpResponse response2 = restAPIAdmin.getWorkflows(workflowType1);
        assertEquals(response2.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String json1 = response2.getData();

        Gson g1 = new Gson();
        WorkflowListDTO workflowListDTO1 = g1.fromJson(json1 , WorkflowListDTO.class);
        List<WorkflowInfoDTO> list1 = new ArrayList<WorkflowInfoDTO>();
        list1 = workflowListDTO1.getList();

        String externalWorkflowRef1="";

        for(WorkflowInfoDTO workflowinfo: list1) {
            externalWorkflowRef1 = workflowinfo.getReferenceId();
        }

        //get workflow pending request by external workflow reference by admin
        HttpResponse response = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef1);
        assertEquals(response.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");

        //clean up process for API and application
        restAPIPublisher.deleteAPI(apiIdNew);
        restAPIStore.deleteApplication(applicationIDNew);

        //check workflow pending requests are cleaned (Application Creation, API State Change)
        HttpResponse response4 = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef1);
        assertEquals(response4.getResponseCode(), 500,
                "Clean up pending task process is failed for Application creation");

        HttpResponse response5 = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef);
        assertEquals(response5.getResponseCode(), 500,
                "Clean up pending task process is failed for API state change");

        //create Application
        HttpResponse applicationResponseNew = restAPIStore.createApplication("MyApp",
                "Testing application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        String applicationIDSecond = applicationResponseNew.getData();

        //create API and request for publish the API
        HttpResponse apiResponseNew = restAPIPublisher.addAPI(apiRequest);
        String apiIdSecond= apiResponseNew.getData();
        restAPIPublisher.changeAPILifeCycleStatus(apiIdSecond, APILifeCycleAction.PUBLISH.getAction(), null);

        //get workflow requests of API state change
        String workflowTypeAPIState = "AM_API_STATE";
        HttpResponse response6 = restAPIAdmin.getWorkflows(workflowTypeAPIState);
        assertEquals(response6.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String json2 = response6.getData();
        Gson g2 = new Gson();
        WorkflowListDTO workflowListDTO2 = g2.fromJson(json2 , WorkflowListDTO.class);
        List<WorkflowInfoDTO> list2 = new ArrayList<WorkflowInfoDTO>();
        list2 = workflowListDTO2.getList();

        String externalWorkflowRef2="";

        for(WorkflowInfoDTO workflowinfo: list2) {
            externalWorkflowRef2 = workflowinfo.getReferenceId();
        }

        //update workflow request and approve the API state change
        HttpResponse response7 = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef2);
        assertEquals(response7.getResponseCode(), 200,
                "Update workflow pending process is failed for user admin");

        //get workflow pending request for application creation
        String workflowTypeApplicationCreation = "AM_APPLICATION_CREATION";
        HttpResponse response8 = restAPIAdmin.getWorkflows(workflowTypeApplicationCreation);
        assertEquals(response8.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String json3 = response8.getData();
        Gson g3 = new Gson();
        WorkflowListDTO workflowListDTO3 = g3.fromJson(json3 , WorkflowListDTO.class);
        List<WorkflowInfoDTO> list3 = new ArrayList<WorkflowInfoDTO>();
        list3 = workflowListDTO3.getList();

        String externalWorkflowRef3="";

        for(WorkflowInfoDTO workflowinfo: list3) {
            externalWorkflowRef3 = workflowinfo.getReferenceId();
        }

        //update workflow pending task of application creation
        HttpResponse response9 = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef3);
        assertEquals(response9.getResponseCode(), 200,
                "Update workflow pending process is failed for user admin");

        //subscription creation
        HttpResponse SubscribeResponse = restAPIStore.createSubscription(apiIdSecond, applicationIDSecond, APIMIntegrationConstants.API_TIER.UNLIMITED);
        subscriptionId = SubscribeResponse.getData();
        assertEquals(SubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of  API  request not successful");

        //get workflow pending requests of subscription creation
        String workflowTypeSubscriptionCreation = "AM_SUBSCRIPTION_CREATION";
        HttpResponse response10 = restAPIAdmin.getWorkflows(workflowTypeSubscriptionCreation);
        assertEquals(response1.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String json4 = response10.getData();
        Gson g4 = new Gson();
        WorkflowListDTO workflowListDTO4 = g4.fromJson(json4 , WorkflowListDTO.class);
        List<WorkflowInfoDTO> list4 = new ArrayList<WorkflowInfoDTO>();
        list4 = workflowListDTO4.getList();

        String externalWorkflowRef4="";

        for(WorkflowInfoDTO workflowinfo: list4) {
            externalWorkflowRef4 = workflowinfo.getReferenceId();
        }

        //get workflow pending task by external workflow reference by admin
        HttpResponse response11 = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef4);
        assertEquals(response11.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");

        //generate keys
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationIDSecond,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes );

        //get workflow pending requests for application key generation
        String workflowTypeRegistration = "AM_APPLICATION_REGISTRATION_PRODUCTION";
        HttpResponse response12 = restAPIAdmin.getWorkflows(workflowTypeRegistration);
        assertEquals(response12.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String json5 = response10.getData();
        Gson g5 = new Gson();
        WorkflowListDTO workflowListDTO5 = g5.fromJson(json5 , WorkflowListDTO.class);
        List<WorkflowInfoDTO> list5 = new ArrayList<WorkflowInfoDTO>();
        list5 = workflowListDTO5.getList();

        String externalWorkflowRef5="";

        for(WorkflowInfoDTO workflowinfo: list5) {
            externalWorkflowRef5 = workflowinfo.getReferenceId();
        }

        //get workflow pending request by external workflow reference
        HttpResponse response13 = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef5);
        assertEquals(response13.getResponseCode(), 200,
                "Get Workflow Pending request failed for User Admin");

        //clean up process for all pending requests
        restAPIStore.deleteApplication(applicationIDSecond);
        restAPIPublisher.deleteAPI(apiIdSecond);

        //check the clean up process is successful
        HttpResponse response14 = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef4);
        assertEquals(response14.getResponseCode(), 500,
                "Clean up pending task process is failed for Subscription Creation");

        HttpResponse response15 = restAPIAdmin.getWorkflowByExternalWorkflowReference(externalWorkflowRef5);
        assertEquals(response15.getResponseCode(), 500,
                "Clean up pending task process is failed for Application Key generation");

    }

    @Test(groups = { "wso2.am" }, description = "pending tasks workflow process check", dependsOnMethods = "cleanUpWorkflowProcessCheck")
    public void pendingTaskWorkflowProcessCheck() throws Exception {

        //create application
        HttpResponse applicationResponse = restAPIStore.createApplication("AppApp",
                "Testing application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        String applicationIDNew = applicationResponse.getData();

        //get workflow pending requests by admin
        String workflowType=null;
        HttpResponse response1 = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response1.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String json1 = response1.getData();
        Gson g1 = new Gson();
        WorkflowListDTO workflowListDTO1 = g1.fromJson(json1 , WorkflowListDTO.class);
        int count = workflowListDTO1.getCount();

        assertEquals(count, 2,
                "Correct Number of Workflow requests never received");

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
        String apiIdFirst= apiResponse.getData();
        HttpResponse lifeCycleChangeResponse = restAPIPublisher
                .changeAPILifeCycleStatus(apiIdFirst, APILifeCycleAction.PUBLISH.getAction(), null);

        //get workflow pending requests by admin
        HttpResponse response2 = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response2.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String json2 = response2.getData();
        Gson g2 = new Gson();
        WorkflowListDTO workflowListDTO2 = g2.fromJson(json2 , WorkflowListDTO.class);
        count = workflowListDTO2.getCount();

        assertEquals(count, 3,
                "Correct Number of Workflow requests never received");


        List<WorkflowInfoDTO> list = new ArrayList<WorkflowInfoDTO>();
        list = workflowListDTO1.getList();

        String externalWorkflowRef="";

        for(WorkflowInfoDTO workflowinfo: list) {
            externalWorkflowRef = workflowinfo.getReferenceId();
        }

        //update workflow status by user admin
        HttpResponse response3 = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef);
        assertEquals(response3.getResponseCode(), 200,
                "Workflow request can only be viewed for the admin");

        List<WorkflowInfoDTO> list1 = new ArrayList<WorkflowInfoDTO>();
        list1 = workflowListDTO2.getList();

        String externalWorkflowRef1="";

        for(WorkflowInfoDTO workflowinfo: list1) {
            externalWorkflowRef1 = workflowinfo.getReferenceId();
        }

        //upload workflow status by user admin
        HttpResponse response4 = restAPIAdmin.updateWorkflowStatus(externalWorkflowRef1);
        assertEquals(response4.getResponseCode(), 200,
                "Workflow request can only be viewed for the admin");

        //create subscription
        HttpResponse SubscribeResponse = restAPIStore.createSubscription(apiIdFirst, applicationIDNew, APIMIntegrationConstants.API_TIER.UNLIMITED);
        subscriptionId = SubscribeResponse.getData();
        assertEquals(SubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful");

        //get workflow pending requests for user admin
        HttpResponse response5 = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response5.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String json3 = response5.getData();
        Gson g3 = new Gson();
        WorkflowListDTO workflowListDTO3 = g3.fromJson(json3 , WorkflowListDTO.class);
        count = workflowListDTO3.getCount();

        assertEquals(count, 2,
                "Correct Number of Workflow requests never received");

        //generate keys
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationIDNew,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes );

        //get workflow pending requests by admin
        HttpResponse response6 = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response6.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String json4 = response6.getData();
        Gson g4 = new Gson();
        WorkflowListDTO workflowListDTO4 = g4.fromJson(json4 , WorkflowListDTO.class);
        count = workflowListDTO4.getCount();

        assertEquals(count, 3,
                "Correct Number of Workflow requests never received");

        //delete application and delete API clean up pending task process
        restAPIStore.deleteApplication(applicationIDNew);
        restAPIPublisher.deleteAPI(apiIdFirst);

        //get workflow pending requests by admin
        HttpResponse response7 = restAPIAdmin.getWorkflows(workflowType);
        assertEquals(response7.getResponseCode(), 200,
                "Get Workflow Pending requests failed for User Admin");

        String json5 = response7.getData();
        Gson g5 = new Gson();
        WorkflowListDTO workflowListDTO5 = g5.fromJson(json5 , WorkflowListDTO.class);
        count = workflowListDTO5.getCount();

        assertEquals(count, 1,
                "Correct Number of Workflow requests never received");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        restAPIStore.deleteApplication(applicationID);
        restAPIPublisher.deleteAPI(apiId);
        userManagementClient1.deleteUser(USER_SMITH);
        userManagementClient1.deleteUser(USER_ADMIN);
        super.cleanUp();
    }



}
