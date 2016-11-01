/* * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 */
package org.wso2.am.integration.tests.other;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;
import org.wso2.carbon.utils.ServerConstants;

/**
 * The test class that tests the custom Subscription creation workflow which has a HTTP redirect.
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class APIStateChangeWorkflowTestCase extends APIManagerLifecycleBaseTest {

    private APIPublisherRestClient apiPublisher;
    private String publisherURLHttp;
    private String storeURLHttp;
    private String userName;
    private String originalWFExtentionsXML;
    private String newWFExtentionsXML;
    private ResourceAdminServiceClient resourceAdminServiceClient;
    private APIIdentifier apiIdentifier;
    private final String APIM_CONFIG_XML = "api-manager.xml";
    private final String DEFAULT_WF_EXTENTIONS_XML_REG_CONFIG_LOCATION =
            "/_system/governance/apimgt/applicationdata/workflow-extensions.xml";
    private String appName = "sample-application-workflow2";
    private static JSONParser parser = new JSONParser();
    private ServerConfigurationManager serverConfigurationManager;
    private String apiName = "APIStateWf";
    private String apiVersion = "1.0.0";
    private WebAppAdminClient webAppAdminClient;
    private String wfreferenceId = null;
    private String clientId;
    private String clientSecrect;

    /*
    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }
    @Factory(dataProvider = "userModeDataProvider")
    public APIStateChangeWorkflowTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }*/
    
    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws AutomationUtilException, XPathExpressionException, IOException,
            APIManagerIntegrationTestException, URISyntaxException, ResourceAdminServiceExceptionException {
        super.init(userMode);
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
            String apimConfigArtifactLocation = getAMResourceLocation() + File.separator + "configFiles"
                    + File.separator + "workflowapistatechange" + File.separator + "api-manager.xml";
            serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
            String apimRepositoryConfigLocation = carbonHome + File.separator + "repository" + File.separator + "conf"
                    + File.separator + APIM_CONFIG_XML;
            File apimConfSourceFile = new File(apimConfigArtifactLocation);
            File apimConfTargetFile = new File(apimRepositoryConfigLocation);
            // apply configuration to api-manager.xml
            serverConfigurationManager.applyConfiguration(apimConfSourceFile, apimConfTargetFile);

        }

        String testArtifactPath = TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts"
                + File.separator + "AM" + File.separator + "configFiles" + File.separator + "workflowapistatechange"
                + File.separator + APIMIntegrationConstants.BPMN_PROCESS_ENGINE_WEB_APP_NAME + ".war";

        String gatewayMgtSessionId = createSession(gatewayContextMgt);

        webAppAdminClient = new WebAppAdminClient(gatewayContextMgt.getContextUrls().getBackEndUrl(),
                gatewayMgtSessionId);
        webAppAdminClient.uploadWarFile(testArtifactPath);

        WebAppDeploymentUtil.isWebApplicationDeployed(gatewayContextMgt.getContextUrls().getBackEndUrl(),
                gatewayMgtSessionId, APIMIntegrationConstants.BPMN_PROCESS_ENGINE_WEB_APP_NAME);

        String url = getGatewayURLHttp();

        publisherURLHttp = getPublisherURLHttp();
        storeURLHttp = getStoreURLHttp();
        userName = user.getUserName();
        resourceAdminServiceClient = new ResourceAdminServiceClient(gatewayContextMgt.getContextUrls().getBackEndUrl(),
                createSession(gatewayContextMgt));
        // Gets the original workflow-extentions.xml file's content from the registry.
        originalWFExtentionsXML = resourceAdminServiceClient
                .getTextContent(DEFAULT_WF_EXTENTIONS_XML_REG_CONFIG_LOCATION);
        // Gets the new configuration of the workflow-extentions.xml
        newWFExtentionsXML = readFile(getAMResourceLocation() + File.separator + "configFiles" + File.separator
                + "workflowapistatechange" + File.separator + "workflow-extentions.xml");
        // Updates the content of the workflow-extentions.xml of the registry file, to have the new configurations.
        resourceAdminServiceClient.updateTextContent(DEFAULT_WF_EXTENTIONS_XML_REG_CONFIG_LOCATION, newWFExtentionsXML);

        APIStoreRestClient apiStore;

        // Just a dummy URL
        String backendurl = "http://localhost:9443/carbon";
        String description = "This is test API create by API manager integration test";
        // String apiVersion = "1.0.0";

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);

        apiPublisher.login(userName, userName);

        APIRequest apiRequest = new APIRequest(apiName, apiVersion, new URL(backendurl));
        apiRequest.setDescription(description);
        apiRequest.setVersion(apiVersion);
        apiRequest.setProvider(userName);
        apiPublisher.addAPI(apiRequest);

    }

    @Test(groups = "wso2.am", description = "Test api state change approval process")
    public void testAPIStateChangeAndApproveWorkflow() throws Exception {

        apiIdentifier = new APIIdentifier(userName, apiName, apiVersion);

        // change the state to publish. wf is setup for publish
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, userName,
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        // check the current state
        HttpResponse api = apiPublisher.getAPI(apiName, userName);
        // check whether request is sent to bpmn engin
        JSONObject apiObj = new JSONObject(api.getData());
        // apiObj.get("state");
        String apiStatus = (String) apiObj.getJSONObject("api").get("status");

        // lifecycle state should not change
        assertEquals(apiStatus, APILifeCycleState.CREATED.toString(),
                "Lifecycle state should remain without changing till approval. ");

        // check request's parameters.
        HttpResponse requestSentFromAM = HTTPSClientUtils.doGet(
                publisherURLHttp + "BPMNProcessServerApp-1.0.0/runtime/process-instances?debugInfo=startRequest", null);
        // businessKey
        JSONObject sentMsgObj = new JSONObject(requestSentFromAM.getData());
        JSONArray variables = sentMsgObj.getJSONArray("variables");
        wfreferenceId = (String) sentMsgObj.get("businessKey");
        Assert.assertNotNull(wfreferenceId, "businessKey should not be null");

        String scope = null;
        String apiCurrentState = null;
        String apiLCAction = null;
        String apiNameReq = null;
        String apiVersionReq = null;
        String apiProvider = null;
        String invoker = null;

        for (int i = 0; i < variables.length(); i++) {
            JSONObject variable = (JSONObject) variables.get(i);

            String name = variable.getString("name");
            if ("clientId".equals(name)) {
                clientId = variable.getString("value");
            } else if ("clientSecret".equals(name)) {
                clientSecrect = variable.getString("value");
            } else if ("scope".equals(name)) {
                scope = variable.getString("value");
            } else if ("apiCurrentState".equals(name)) {
                apiCurrentState = variable.getString("value");
            } else if ("apiLCAction".equals(name)) {
                apiLCAction = variable.getString("value");
            } else if ("apiName".equals(name)) {
                apiNameReq = variable.getString("value");
            } else if ("apiVersion".equals(name)) {
                apiVersionReq = variable.getString("value");
            } else if ("apiProvider".equals(name)) {
                apiProvider = variable.getString("value");
            } else if ("invoker".equals(name)) {
                invoker = variable.getString("value");
            }

        }
        Assert.assertNotNull(clientId, "clientId should not be null");
        Assert.assertNotNull(clientSecrect, "clientSecrect should not be null");
        Assert.assertNotNull(scope, "scope should not be null");

        HttpResponse resp = completeWorkflowTask(clientId, clientSecrect, scope, apiCurrentState, apiLCAction,
                apiNameReq, apiVersionReq, apiProvider, invoker, "APPROVED");
        Assert.assertEquals(resp.getResponseCode(), 200, "Invalid status code:" + resp.getResponseCode());

        // check the state of the api. this should be PUBLISHED now
        // check the current state
        api = apiPublisher.getAPI(apiName, userName);
        // check whether request is sent to bpmn engin
        apiObj = new JSONObject(api.getData());
        apiStatus = (String) apiObj.getJSONObject("api").get("status");

        assertEquals(apiStatus, APILifeCycleState.PUBLISHED.toString(),
                "Lifecycle state has not changed after the approval:  " + apiStatus);

    }

    @Test(groups = "wso2.am", dependsOnMethods = "testAPIStateChangeAndApproveWorkflow",
            description = "check API state change reject process")
    public void testAPIStateChangeAndRejectWorkflow() throws Exception {

        apiIdentifier = new APIIdentifier(userName, apiName, apiVersion);

        // change the state to blocked. wf is setup for publish
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, userName,
                APILifeCycleState.BLOCKED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        // check the current state
        HttpResponse api = apiPublisher.getAPI(apiName, userName);
        // check whether request is sent to bpmn engin
        JSONObject apiObj = new JSONObject(api.getData());
        // apiObj.get("state");
        String apiStatus = (String) apiObj.getJSONObject("api").get("status");

        // lifecycle state should not change
        assertEquals(apiStatus, APILifeCycleState.PUBLISHED.toString(),
                "Lifecycle state should remain without changing till approval. ");

        // check request's parameters.
        HttpResponse requestSentFromAM = HTTPSClientUtils.doGet(
                publisherURLHttp + "BPMNProcessServerApp-1.0.0/runtime/process-instances?debugInfo=startRequest", null);
                // businessKey
        JSONObject sentMsgObj = new JSONObject(requestSentFromAM.getData());
        JSONArray variables = sentMsgObj.getJSONArray("variables");
        wfreferenceId = (String) sentMsgObj.get("businessKey");
        Assert.assertNotNull(wfreferenceId, "businessKey should not be null");

        String scope = null;
        String apiCurrentState = null;
        String apiLCAction = null;
        String apiNameReq = null;
        String apiVersionReq = null;
        String apiProvider = null;
        String invoker = null;

        for (int i = 0; i < variables.length(); i++) {
            JSONObject variable = (JSONObject) variables.get(i);

            String name = variable.getString("name");
            if ("clientId".equals(name)) {
                clientId = variable.getString("value");
            } else if ("clientSecret".equals(name)) {
                clientSecrect = variable.getString("value");
            } else if ("scope".equals(name)) {
                scope = variable.getString("value");
            } else if ("apiCurrentState".equals(name)) {
                apiCurrentState = variable.getString("value");
            } else if ("apiLCAction".equals(name)) {
                apiLCAction = variable.getString("value");
            } else if ("apiName".equals(name)) {
                apiNameReq = variable.getString("value");
            } else if ("apiVersion".equals(name)) {
                apiVersionReq = variable.getString("value");
            } else if ("apiProvider".equals(name)) {
                apiProvider = variable.getString("value");
            } else if ("invoker".equals(name)) {
                invoker = variable.getString("value");
            }

        }
        Assert.assertNotNull(clientId, "clientId should not be null");
        Assert.assertNotNull(clientSecrect, "clientSecrect should not be null");
        Assert.assertNotNull(scope, "scope should not be null");

        HttpResponse resp = completeWorkflowTask(clientId, clientSecrect, scope, apiCurrentState, apiLCAction,
                apiNameReq, apiVersionReq, apiProvider, invoker, "REJECTED");
        Assert.assertEquals(resp.getResponseCode(), 200, "Invalid status code:" + resp.getResponseCode());

        // check the state of the api. this should be PUBLISHED now
        // check the current state
        api = apiPublisher.getAPI(apiName, userName);
        // check whether request is sent to bpmn engin
        apiObj = new JSONObject(api.getData());
        apiStatus = (String) apiObj.getJSONObject("api").get("status");

        assertEquals(apiStatus, APILifeCycleState.PUBLISHED.toString(),
                "Lifecycle has changed after rejecting the state transition:  " + apiStatus);

    }

    @Test(groups = "wso2.am", dependsOnMethods = "testAPIStateChangeAndRejectWorkflow", 
            description = "check rest api for workflow statue change")
    public void testWorkflowCallbackRestAPI() throws Exception {

        JSONObject workflowResBody = new JSONObject();
        String workflowCallbackUrl;

        // check for token with different scope other than apim:api_workfow
        String accessToken = generateAccessToken("default", clientId, clientSecrect);
        Map<String, String> authenticationRequestHeaders = new HashMap<String, String>();
        authenticationRequestHeaders.put("Authorization", "Bearer " + accessToken);
        authenticationRequestHeaders.put("Content-Type", "application/json");

        workflowCallbackUrl = publisherURLHttp + "api/am/publisher/" + APIMIntegrationConstants.REST_API_VERSION
                + "/workflows/update-workflow-status?workflowReferenceId=" + wfreferenceId;
        HttpResponse response = HTTPSClientUtils.doPost(workflowCallbackUrl, authenticationRequestHeaders,
                workflowResBody.toString());
        Assert.assertEquals(response.getResponseCode(), 401,
                "Status code mismatch when request is done without valid token");

        // get the correct scope
        accessToken = generateAccessToken("apim:api_workflow", clientId, clientSecrect);
        authenticationRequestHeaders = new HashMap<String, String>();
        authenticationRequestHeaders.put("Authorization", "Bearer " + accessToken);
        authenticationRequestHeaders.put("Content-Type", "application/json");

        // check for invalid reference id
        String refId = "xxxxxxxxxxxxxxxxx";
        workflowCallbackUrl = publisherURLHttp + "api/am/publisher/" + APIMIntegrationConstants.REST_API_VERSION
                + "/workflows/update-workflow-status?workflowReferenceId=" + refId;
        workflowResBody.put("description", "nothing");
        workflowResBody.put("status", "APPROVED");

        JSONObject attributes = new JSONObject();
        attributes.put("apiCurrentState", "");
        attributes.put("apiLCAction", "");
        attributes.put("apiName", "");
        attributes.put("apiVersion", "");
        attributes.put("apiProvider", "");
        attributes.put("invoker", "");
        workflowResBody.put("attributes", attributes);
        
        response = HTTPSClientUtils.doPost(workflowCallbackUrl, authenticationRequestHeaders,
                workflowResBody.toString());
        Assert.assertEquals(response.getResponseCode(), 404,
                "Status code mismatch when request is done without workflowReferenceId");

   
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        super.cleanUp();
        // restore the original workflow-extentions.xml content.
        resourceAdminServiceClient.updateTextContent(DEFAULT_WF_EXTENTIONS_XML_REG_CONFIG_LOCATION,
                originalWFExtentionsXML);
        List<String> webAppList = new ArrayList<String>();
        webAppList.add(APIMIntegrationConstants.BPMN_PROCESS_ENGINE_WEB_APP_NAME);
        webAppAdminClient.deleteWebAppList(webAppList,
                gatewayContextMgt.getDefaultInstance().getHosts().get("default"));
    }

    private HttpResponse completeWorkflowTask(String clientId, String clientSecrect, String scope,
            String apiCurrentState, String apiLCAction, String apiNameReq, String apiVersionReq, String apiProvider,
            String invoker, String approval) throws JSONException, APIManagerIntegrationTestException, IOException {
        // generate tokens

        String requestBody = "grant_type=client_credentials" + "&scope=" + scope;
        URL tokenEndpointURL = new URL(gatewayUrlsWrk.getWebAppURLNhttps() + "token");
        JSONObject accessTokenGenerationResponse = new JSONObject(
                apiStore.generateUserAccessKey(clientId, clientSecrect, requestBody, tokenEndpointURL).getData());

        String accessToken = accessTokenGenerationResponse.getString("access_token");
        String scopeInToken = accessTokenGenerationResponse.getString("scope");

        // check whether token has the workflow scope
        Assert.assertTrue(scopeInToken.contains(scope), "Generated token does not have scope " + scope);

        // call worklfow resume rest api using the token
        JSONObject workflowResBody = new JSONObject();
        workflowResBody.put("status", approval);
        workflowResBody.put("description", "nothing");

        JSONObject attributes = new JSONObject();
        attributes.put("apiCurrentState", apiCurrentState);
        attributes.put("apiLCAction", apiLCAction);
        attributes.put("apiName", apiNameReq);
        attributes.put("apiVersion", apiVersionReq);
        attributes.put("apiProvider", apiProvider);
        attributes.put("invoker", invoker);
        workflowResBody.put("attributes", attributes);

        String workflowCallbackUrl = publisherURLHttp + "api/am/publisher/" + APIMIntegrationConstants.REST_API_VERSION
                + "/workflows/update-workflow-status?workflowReferenceId=" + wfreferenceId;


        Map<String, String> authenticationRequestHeaders = new HashMap<String, String>();
        authenticationRequestHeaders.put("Authorization", "Bearer " + accessToken);
        authenticationRequestHeaders.put("Content-Type", "application/json");

        return HTTPSClientUtils.doPost(workflowCallbackUrl, authenticationRequestHeaders, workflowResBody.toString());
    }

    private String generateAccessToken(String scope, String clientId, String clientSecrect)
            throws MalformedURLException, JSONException, APIManagerIntegrationTestException {
        String requestBody = "grant_type=client_credentials" + "&scope=" + scope;
        URL tokenEndpointURL = new URL(gatewayUrlsWrk.getWebAppURLNhttps() + "token");
        JSONObject accessTokenGenerationResponse = new JSONObject(
                apiStore.generateUserAccessKey(clientId, clientSecrect, requestBody, tokenEndpointURL).getData());

        String accessToken = accessTokenGenerationResponse.getString("access_token");
        return accessToken;
    }
}
