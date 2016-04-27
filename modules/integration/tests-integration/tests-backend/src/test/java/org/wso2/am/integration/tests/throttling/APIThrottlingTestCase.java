/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.am.integration.tests.throttling;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.user.RemoteUserStoreManagerServiceClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * This will API Throttling for APIs.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class APIThrottlingTestCase extends APIManagerLifecycleBaseTest {
    private ServerConfigurationManager serverConfigurationManager;
    private UserManagementClient userManagementClient1;
    private static final Log log = LogFactory.getLog(APIThrottlingTestCase.class);

    private String publisherURLHttp;
    private String storeURLHttp;
    private final String INTERNAL_ROLE_SUBSCRIBER = "Internal/subscriber";


    private String apiName = "APIThrottleAPI";
    private String apiContext = "api_throttle";
    private String tags = "token, throttling";
    private String description = "This is test API created by API manager integration test";
    private String providerName;
    private String apiVersion = "1.0.0";
    private String applicationName = "APIThrottle-application";
    private String backendURL;

    String subscriberUser = "subscriberUser2";
    String subscriberUserWithTenantDomain;
    String password = "password@123";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        subscriberUserWithTenantDomain = subscriberUser + "@" + user.getUserDomain();
        publisherURLHttp = getPublisherURLHttp();
        storeURLHttp = getStoreURLHttp();

        if(TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
            /*serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "throttling" + File.separator + "api-manager.xml"));
            serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation() + File.separator
                + "configFiles" + File.separator + "throttling" + File.separator + "log4j.properties"));
            serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                    + File.separator + "configFiles" + File.separator + "throttling" + File.separator + "jndi.properties"));*/
            subscriberUserWithTenantDomain = subscriberUser;
            //Load the back-end API
            String gatewaySessionCookie = createSession(gatewayContextMgt);
            loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM" + File.separator
                + "synapseconfigs" + File.separator + "rest" + File.separator + "api_throttle_backend.xml",
                gatewayContextMgt, gatewaySessionCookie);
            waitForAPIDeploymentSync(user.getUserName(), apiName, apiVersion,
                    APIMIntegrationConstants.IS_API_EXISTS);
            
            // restart the server 
            ServerConfigurationManager serverConfigManagerForTenant =
                    new ServerConfigurationManager(superTenantKeyManagerContext);
            serverConfigManagerForTenant.restartGracefully();
            super.init(userMode);
        }

        backendURL = getSuperTenantAPIInvocationURLHttp("api_throttle_backend", "1.0");
        providerName = user.getUserName();
    }

    @Test(groups = { "wso2.am" }, description = "API Throttling Test", enabled = true)
    public void testAPIThrottling_1() throws Exception {

        publishAndInvokeAPI(apiName, apiVersion, apiContext, description, backendURL, tags, providerName);        
    }
    
	private void publishAndInvokeAPI(String apiName, String apiVersion, String apiContext, String description, String endpointURL,
			String tags, String providerName)
					throws APIManagerIntegrationTestException, XPathExpressionException, IOException, JSONException {

		APIPublisherRestClient apiPublisher = new APIPublisherRestClient(publisherURLHttp);

		apiPublisher.login(user.getUserName(), user.getPassword());
		
		APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName,
				apiContext, apiVersion, user.getUserName(), new URL(backendURL));
        apiCreationRequestBean.setTiersCollection(APIMIntegrationConstants.API_TIER.SILVER);

        //define resources
        ArrayList<APIResourceBean> resList = new ArrayList<APIResourceBean>();
        APIResourceBean res = new APIResourceBean(APIMIntegrationConstants.HTTP_VERB_GET,
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.BASIC, "test");
        resList.add(res);
        apiCreationRequestBean.setResourceBeanList(resList);

        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiCreationRequestBean);
        verifyResponse(serviceResponse);
        waitForAPIDeploymentSync(user.getUserName(), apiName, apiVersion,
                APIMIntegrationConstants.IS_API_EXISTS);
        
        //add a application
        APIStoreRestClient apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(user.getUserName(), user.getPassword());
        serviceResponse = apiStore
                .addApplication(applicationName, APIMIntegrationConstants.APPLICATION_TIER.SMALL, "",
                        "this-is-test");
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);
        waitForAPIDeploymentSync(user.getUserName(), apiName, apiVersion,
                APIMIntegrationConstants.IS_API_EXISTS);

        //subscribe to the api
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, user.getUserName());
        subscriptionRequest.setApplicationName(applicationName);
        subscriptionRequest.setTier(APIMIntegrationConstants.API_TIER.SILVER);
        serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);
        
        //generate the key for the subscription
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(applicationName);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Assert.assertNotNull("Access Token not found " + responseString, accessToken);

        String invokeURL = getAPIInvocationURLHttps(apiContext);
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        log.info("=============================== Headers : "+requestHeaders);
        log.info("=============================== invokeURL : "+invokeURL);
        //https://localhost:8743/api_throttle
        serviceResponse = HTTPSClientUtils.doGet(invokeURL+"/1.0.0/test", requestHeaders);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
        
        //verify throttling
        checkThrottling(accessToken, invokeURL, requestHeaders);

	}
	
	private void checkThrottling(String accessToken, String invokeURL, Map<String, String> requestHeaders){
		int count = 0;
		int limit = 4;
		int numberOfIterations = 4;
		for(; count < numberOfIterations; ++count){
			try {
				log.info(" =================================== Number of time API Invoked : "+ count);
				if(count == limit){
					Thread.sleep(10000);
				}
				HttpResponse serviceResponse = callAPI(accessToken, invokeURL, requestHeaders);
				if(count == limit){
					Assert.assertEquals(serviceResponse.getResponseCode(), 429, "Response code is not as expected");
				}else{
					Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
				}
				
			} catch (Exception ex) {
				log.error("Error occurred while calling API : " +ex);
				break;
			}
		}
	}
	
	private HttpResponse callAPI(String accessToken, String invokeURL, Map<String, String> requestHeaders)throws Exception{
		//https://localhost:8743/api_throttle
        HttpResponse serviceResponse = HTTPSClientUtils.doGet(invokeURL+"/1.0.0/test", requestHeaders);
        return serviceResponse;
        
	}

	@DataProvider
	public static Object[][] userModeDataProvider() {
		return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN }/*,
				new Object[] { TestUserMode.TENANT_ADMIN }*/ };
	}
	
	@Factory(dataProvider = "userModeDataProvider")
    public APIThrottlingTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }
	
	@AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
        if(userManagementClient1 != null) {
            userManagementClient1.deleteRole(INTERNAL_ROLE_SUBSCRIBER);
            userManagementClient1.deleteUser(subscriberUser);
        }
        if(TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            serverConfigurationManager.restoreToLastConfiguration();
        }
    }

}
