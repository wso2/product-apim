package org.wso2.am.integration.tests.other;

import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;

import javax.xml.xpath.XPathExpressionException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

/**
 * The test class that tests the custom Subscription creation workflow which has a HTTP redirect.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class SubscriptionWFHTTPRedirectTest  extends APIManagerLifecycleBaseTest{


    private APIPublisherRestClient apiPublisher;
    private String publisherURLHttp;
    private String storeURLHttp;
    private String userName;
    private APIIdentifier apiIdentifier;
    private String appName = "sample-application-workflow2";
    private static JSONParser parser = new JSONParser();
    private org.json.simple.JSONObject originalTenantConf;
    private static final String UTF_8 = "UTF-8";
    private static final String TENANT_CONFIG_PATH = "artifacts/AM/configFiles/tenantConf/tenant-conf.json";
    private static final String WORKFLOW_CONFIG_PATH
            = "artifacts/AM/configFiles/workflowHTTPRedirect/tenant-workflow-conf.json";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws AutomationUtilException, XPathExpressionException, IOException,
            APIManagerIntegrationTestException, URISyntaxException, ResourceAdminServiceExceptionException, ParseException, ApiException {
        super.init();

        ServerConfigurationManager serverConfigurationManager;
        serverConfigurationManager = new ServerConfigurationManager(gatewayContextMgt);
        //Copies the custom workflow jar to the lib.
        serverConfigurationManager.copyToComponentLib(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "workflowHTTPRedirect" + File.separator +
                "SubscriptionCreationCustomWorkflowExecutor-1.0.0.jar"));
        serverConfigurationManager.restartGracefully();
        super.init();

        publisherURLHttp = getPublisherURLHttp();
        storeURLHttp = getStoreURLHttp();
        userName = user.getUserName();

        originalTenantConf =  (org.json.simple.JSONObject) new JSONParser().parse(restAPIAdmin.getTenantConfig());
        InputStream tenantConfigStream = getClass().getClassLoader().getResourceAsStream(TENANT_CONFIG_PATH);
        assertNotNull("Tenant config stream can not be null", tenantConfigStream);

        org.json.simple.JSONObject tenantJsonObject = (org.json.simple.JSONObject) new JSONParser().parse(
                new InputStreamReader(tenantConfigStream, UTF_8));

        InputStream wfStream = getClass().getClassLoader().getResourceAsStream(WORKFLOW_CONFIG_PATH);
        assertNotNull("Workflow stream can not be null", wfStream);
        org.json.simple.JSONObject wfJsonObj = (org.json.simple.JSONObject) new JSONParser().parse(
                new InputStreamReader(wfStream, UTF_8));

        tenantJsonObject.put("Workflows",wfJsonObj);
        //Updates the workflow configuration to have a custom SubscriptionCreation workflow.
        restAPIAdmin.updateTenantConfig(tenantJsonObject);

    }


    @Test(groups = "wso2.am", description = "Test if the custom subscription workflow response returns the redirect URI")
    public void testHTTPredirectInSubscriptionWorkflow() throws Exception {

        APIStoreRestClient apiStore;
        String apiName = "HTTPRedirectTestAPI2";
        //Just a dummy URL
        String url = "http://localhost:9443/carbon";
        String description = "This is test API create by API manager integration test";
        String apiVersion = "1.0.0";

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);

        apiPublisher.login(userName, userName);

        APIRequest apiRequest = new APIRequest(apiName, apiVersion, new URL(url));
        apiRequest.setDescription(description);
        apiRequest.setVersion(apiVersion);
        apiRequest.setSandbox(url);
        apiRequest.setProvider(userName);
        apiPublisher.addAPI(apiRequest);

        apiIdentifier = new APIIdentifier(userName, apiName, apiVersion);
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, userName,
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        apiStore.login(userName, user.getPassword());

        apiStore.addApplication(appName,
                APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN, "", "this-is-test");

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, userName);
        subscriptionRequest.setApplicationName(appName);
       //Subscribed to the API.
        HttpResponse httpresponse= apiStore.subscribe(subscriptionRequest);
        String httpresponseData = httpresponse.getData();
        JSONObject jsondata = new JSONObject(httpresponseData);
        JSONObject status = (JSONObject)jsondata.get("status");
        if(status != null){
            JSONObject workflowResponse = null;
            org.json.simple.JSONObject jsonpayloadObject = null;
            workflowResponse = (JSONObject)status.get("workflowResponse");
            if(workflowResponse != null) {
                jsonpayloadObject = (org.json.simple.JSONObject) parser.parse((String) (workflowResponse.get("jsonPayload")));
                if(jsonpayloadObject != null) {
                    assertEquals(jsonpayloadObject.get("redirectUrl").toString(), "http://google.lk");
                }
            }
        }else{
            assertFalse(true);
        }
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        restAPIAdmin.updateTenantConfig(originalTenantConf);
        super.cleanUp();
    }

}
