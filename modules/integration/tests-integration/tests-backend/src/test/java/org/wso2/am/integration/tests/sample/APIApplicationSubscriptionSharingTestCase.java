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

package org.wso2.am.integration.tests.sample;

import static org.testng.Assert.assertTrue;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.user.RemoteUserStoreManagerServiceClient;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

public class APIApplicationSubscriptionSharingTestCase extends AMIntegrationBaseTest {
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
   //move to base class
    private String publisherURLHttp;
    private String storeURLHttp;
    private ServerConfigurationManager serverConfigurationManager;
    
    private static final Log log = LogFactory.getLog(APIApplicationSubscriptionSharingTestCase.class);
    
   private String apiName = "APILifeCycleTestAPI4";
   private String applicationName = "APILifeCycleTestAPI-application4";
   private String apiContext = "testAPI4";
   private String apiVersion = "1.0.0";
   private String apiURL =  "http://gdata.youtube.com/feeds/api/standardfeeds";

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
       
        super.init(TestUserMode.SUPER_TENANT_ADMIN);
        
        serverConfigurationManager = new ServerConfigurationManager(apimContext);
        RemoteUserStoreManagerServiceClient remoteUserStoreManagerServiceClient =
                new RemoteUserStoreManagerServiceClient(
                        apimContext.getContextUrls().getBackEndUrl(),
                        apimContext.getContextTenant().getContextUser().getUserName(),
                        apimContext.getContextTenant().getContextUser().getPassword());
        
        remoteUserStoreManagerServiceClient.setUserClaimValue("testu1", "http://wso2.org/claims/organization", "1", null);
        remoteUserStoreManagerServiceClient.setUserClaimValue("testu2", "http://wso2.org/claims/organization", "1", null);
        remoteUserStoreManagerServiceClient.setUserClaimValue("testu3", "http://wso2.org/claims/organization", "2", null);
        
        serverConfigurationManager.restartGracefully();
        super.init("superTenant","userKey1");

        publisherURLHttp = getPublisherServerURLHttp();
        storeURLHttp = getStoreServerURLHttp();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);
      }

    @Test(groups = {"wso2.am"}, description = "API application sharing test case")
    public void testAPIAddApplicationAndSubscribeTestCase() throws Exception {
                
        apiPublisher.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        APIRequest apiRequest = new APIRequest(apiName , apiContext,
                new URL(apiURL));
        apiRequest.setVersion(apiVersion);
        apiPublisher.addAPI(apiRequest);
        
        String un = MultitenantUtils.getTenantAwareUsername(apimContext
                .getContextTenant().getContextUser().getUserName());
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, un,
                        APILifeCycleState.PUBLISHED
                );
       
       
        apiPublisher.changeAPILifeCycleStatus(updateRequest);
        
               
        apiStore.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        
        apiStore.addApplication(applicationName, "Gold", "", "this-is-test");
        
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, un);
        subscriptionRequest.setApplicationName(applicationName);
        apiStore.subscribe(subscriptionRequest);
        
        HttpResponse applicationResponse1 =  apiStore.getAllApplications();
        assertTrue(applicationResponse1.getData().contains(applicationName), "Application is not added");
        
        HttpResponse subscriptionResponse1 = apiStore.getAllSubscriptions(applicationName);
        assertTrue(subscriptionResponse1.getData().contains(applicationName), "Subscription is not successful");
            
        serverConfigurationManager.restartGracefully();
        super.init("superTenant", "userKey2");
        
        apiStore.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        
        HttpResponse applicationResponse =  apiStore.getAllApplications();
        assertTrue(applicationResponse.getData().contains(applicationName), "Application is not shared");
        
        HttpResponse subscriptionResponse = apiStore.getAllSubscriptions(applicationName);
        assertTrue(subscriptionResponse.getData().contains(applicationName), "Subscription is not shared");
        
    
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
//
//        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
//        apiPublisherRestClient.login(apimContext.getContextTenant().getContextUser().getUserName(),
//                apimContext.getContextTenant().getContextUser().getPassword());
//
//        apiPublisherRestClient.deleteAPI(apiName, apiVersion, apimContext.getContextTenant().getContextUser().getUserName());
//
//        Thread.sleep(5000);
        super.cleanup();
    }
}