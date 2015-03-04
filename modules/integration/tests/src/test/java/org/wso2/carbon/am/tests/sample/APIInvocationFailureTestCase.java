/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.am.tests.sample;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.am.tests.APIManagerIntegrationTest;
import org.wso2.carbon.am.tests.util.APIPublisherRestClient;
import org.wso2.carbon.am.tests.util.bean.APILifeCycleState;
import org.wso2.carbon.am.tests.util.bean.APILifeCycleStateRequest;
import org.wso2.carbon.am.tests.util.bean.APIRequest;
import org.wso2.carbon.automation.api.clients.stratos.tenant.mgt.TenantMgtAdminServiceClient;
import org.wso2.carbon.automation.core.utils.HttpRequestUtil;
import org.wso2.carbon.automation.core.utils.HttpResponse;

/**
 * Test api invocation failure messages
 */
public class APIInvocationFailureTestCase extends APIManagerIntegrationTest {


	private String publisherURLHttp;


	@BeforeClass(alwaysRun = true)
	public void init() throws Exception {
		super.init(0);
		if (isBuilderEnabled()) {
			publisherURLHttp = getServerURLHttp();			
		} else {
			publisherURLHttp = getPublisherServerURLHttp();	
		}		
		 // create a tenant
        TenantMgtAdminServiceClient tenantMgtAdminServiceClient =
                new TenantMgtAdminServiceClient(amServer.getBackEndUrl(), userInfo.getUserName(),
                                                userInfo.getPassword());

        tenantMgtAdminServiceClient.addTenant("testwso2.com", "admin", "admin", "demo");
	}


	@Test(groups = { "wso2.am" }, description = "Invalid token for tenant user api")
	public void APIInvocationFailureForTenant() throws Exception {
		
		 String APIName = "TokenTestAPI";
	        String APIContext = "tokenTestAPI";
	        String tags = "youtube, token, media";
	        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
	        String description = "This is test API create by API manager integration test";
	        String providerName = "admin@testwso2.com";
	        String APIVersion = "1.0.0";

	        APIPublisherRestClient apiPublisher = new APIPublisherRestClient(publisherURLHttp);
	        apiPublisher.login("admin@testwso2.com", "admin");

	        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
	        apiRequest.setTags(tags);
	        apiRequest.setDescription(description);
	        apiRequest.setVersion(APIVersion);
	        apiRequest.setSandbox(url);
	        apiRequest.setResourceMethod("GET");
	        apiPublisher.addAPI(apiRequest);
	        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName,
	        		providerName, APILifeCycleState.PUBLISHED);
	        apiPublisher.changeAPILifeCycleStatusTo(updateRequest);

	        Map<String, String> requestHeaders = new HashMap<String, String>();
	  
	        requestHeaders.put("Authorization", "Bearer xxxxxxxxxxxx"); 
	        Thread.sleep(2000);
	        	  
	        HttpResponse youTubeResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp("t/testwso2.com/" +
	        									APIContext + "/" +APIVersion+"/most_popular"), requestHeaders);
	        Assert.assertEquals(youTubeResponse.getResponseCode(), 401, 
	                            "Response code mismatched when api invocation");
	        Assert.assertTrue(youTubeResponse.getData().contains("900901"), "Error code mismach");
	        
	       
	     
	}
	
	@Test(groups = { "wso2.am" }, description = "Invalid token for tenant user api")
	public void APIInvocationFailureForSuperTenant() throws Exception {
		
		 String APIName = "TokenTestAPI";
	        String APIContext = "tokenTestAPI";
	        String tags = "youtube, token, media";
	        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
	        String description = "This is test API create by API manager integration test";
	        String providerName = userInfo.getUserName();
	        String APIVersion = "1.0.0";
	        APIPublisherRestClient apiPublisher = new APIPublisherRestClient(publisherURLHttp);
	        apiPublisher.login(userInfo.getUserName(), userInfo.getPassword());

	        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
	        apiRequest.setTags(tags);
	        apiRequest.setDescription(description);
	        apiRequest.setVersion(APIVersion);
	        apiRequest.setSandbox(url);
	        apiRequest.setResourceMethod("GET");
	        apiPublisher.addAPI(apiRequest);
	        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, 
	                                                                              providerName, 
	                                                                              APILifeCycleState.PUBLISHED);
	        apiPublisher.changeAPILifeCycleStatusTo(updateRequest);

	        Map<String, String> requestHeaders = new HashMap<String, String>();
	  
	        requestHeaders.put("Authorization", "Bearer xxxxxxxxxxxx"); 
	        Thread.sleep(2000);
	        	  
	        HttpResponse youTubeResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp( APIContext + 
	                                                            "/" +APIVersion+"/most_popular"), requestHeaders);
	        Assert.assertEquals(youTubeResponse.getResponseCode(), 401, "Response code mismatched " +
	        		"															when api invocation");
	        Assert.assertTrue(youTubeResponse.getData().contains("900901"), "Error code mismach");
	     
	}

	@AfterClass(alwaysRun = true)
	public void destroy() throws Exception {
		super.cleanup();
	}

	
}
