/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.api.lifecycle;

import static org.testng.Assert.assertEquals;

import java.net.URL;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

public class RegistryLifeCycleInclusionTest extends APIManagerLifecycleBaseTest{
    
    private final String API_NAME = "RegistryLifeCycleInclusionAPI";
    private final String API_CONTEXT = "RegistryLifeCycleInclusionAPI";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String API_VERSION_2_0_0 = "2.0.0";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private APIPublisherRestClient apiPublisherClientUser1;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private APIIdentifier apiIdentifier;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        String publisherURLHttp = getPublisherURLHttps();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);

        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());       
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
    }
    
    @Test(groups = {"wso2.am"}, description = "Test LC tab of an published api")
    public void testAPIInfoLifecycleTabForPublishedAPI() throws Exception {
        
        //Create and publish API version 1.0.0
        createAndPublishAPI(apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, false);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0, APIMIntegrationConstants.IS_API_EXISTS);
        
       // Thread.sleep(30000);
        HttpResponse resp = apiPublisherClientUser1.getAPIInformationPage(API_NAME, providerName, API_VERSION_1_0_0);
     
        //check for LC state change buttons 
        assertEquals(resp.getData().contains("Deploy as a Prototype"), true, "'Deploy as a Prototype' not visible");
        assertEquals(resp.getData().contains("Demote to Created"), true, "'Demote to Created' not visible");       
        assertEquals(resp.getData().contains("Block"), true, "'Block' not visible");    
        assertEquals(resp.getData().contains("Deprecate"), true, "'Deprecate' not visible");
        
        //check LC history change
        assertEquals(resp.getData().contains("changed the API status from  'CREATED' to 'PUBLISHED'"), true,
                "Incorrect state change displayed");
    }
    
    @Test(groups = {"wso2.am"}, description = "Test checklist item visibility for new version of an api", 
            dependsOnMethods = "testAPIInfoLifecycleTabForPublishedAPI" )
    public void testChecklistItemsVisibility() throws Exception {
        
        //Copy api to version 2.0.0
        copyAPI(apiIdentifier, API_VERSION_2_0_0, apiPublisherClientUser1);
       
        //get the info page from the publisher
        HttpResponse resp = apiPublisherClientUser1.getAPIInformationPage(API_NAME, providerName, API_VERSION_2_0_0);
     
        //check for some of the LC state change buttons 
        assertEquals(resp.getData().contains("Deploy as a Prototype"), true, "'Deploy as a Prototype' not visible");
        assertEquals(resp.getData().contains("Publish"), true, "'Publish' not visible");  
        
        //check for checklist items
        assertEquals(resp.getData().contains("Require re-subscription"), true,
                "'Require re-subscription' checklist item not visible");
        assertEquals(resp.getData().contains("Deprecate old versions"), true,
                "'Deprecate old versions' checklist item not visible");
        
    }
    
    @Test(groups = {"wso2.am"}, description = "Test LC state change visibility in the LC tab in publisher",
            dependsOnMethods = "testChecklistItemsVisibility")
    public void testLCStateChengeVisibility() throws Exception {
       
        //get the info page from the publisher
        HttpResponse resp = apiPublisherClientUser1.getAPIInformationPage(API_NAME, providerName, API_VERSION_2_0_0);
      //  Thread.sleep(30000);
        assertEquals(resp.getData().contains("CREATED"), true, "API is not in CREATED state");
        
        //check for LC state change buttons 
        assertEquals(resp.getData().contains("Deploy as a Prototype"), true, "'Deploy as a Prototype' not visible");
        assertEquals(resp.getData().contains("Publish"), true, "'Publish' not visible");  
        
        APILifeCycleStateRequest updateRequest =  new APILifeCycleStateRequest(API_NAME, providerName,  APILifeCycleState.PUBLISHED);
        updateRequest.setVersion(API_VERSION_2_0_0);
        apiPublisherClientUser1.changeAPILifeCycleStatus(updateRequest);
        
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_2_0_0, APIMIntegrationConstants.IS_API_EXISTS);
        
        //get the info page from the publisher
        resp = apiPublisherClientUser1.getAPIInformationPage(API_NAME, providerName, API_VERSION_2_0_0);
        //state should be in PUBLISHED now
        assertEquals(resp.getData().contains("PUBLISHED"), true, "API is not in PUBLISHED state");
        //check LC history
        assertEquals(resp.getData().contains("changed the API status from  'CREATED' to 'PUBLISHED'"), true,
                "Incorrect state change displayed");
       
        
        //check for LC state change buttons 
        assertEquals(resp.getData().contains("Deploy as a Prototype"), true, "'Deploy as a Prototype' not visible");
        assertEquals(resp.getData().contains("Demote to Created"), true, "'Demote to Created' not visible");       
        assertEquals(resp.getData().contains("Block"), true, "'Block' not visible");    
        assertEquals(resp.getData().contains("Deprecate"), true, "'Deprecate' not visible");
        
        //change state to blocked
        updateRequest =  new APILifeCycleStateRequest(API_NAME, providerName,  APILifeCycleState.BLOCKED);
        updateRequest.setVersion(API_VERSION_2_0_0);
        apiPublisherClientUser1.changeAPILifeCycleStatus(updateRequest);
        
        waitForAPIDeployment();
        
        //get the info page from the publisher
        resp = apiPublisherClientUser1.getAPIInformationPage(API_NAME, providerName, API_VERSION_2_0_0);
        //state should be in BLOCKED now
        assertEquals(resp.getData().contains("BLOCKED"), true, "API is not in BLOCKED state");
        //check LC history
        assertEquals(resp.getData().contains("changed the API status from  'PUBLISHED' to 'BLOCKED'"), true,
                "Incorrect state change displayed");
        
        //check for LC state change buttons     
        assertEquals(resp.getData().contains("Re-Publish"), true, "'Re-Publish' not visible");    
        assertEquals(resp.getData().contains("Deprecate"), true, "'Deprecate' not visible");
        
        //change state to Depricate
        updateRequest =  new APILifeCycleStateRequest(API_NAME, providerName,  APILifeCycleState.DEPRECATED);
        updateRequest.setVersion(API_VERSION_2_0_0);
        apiPublisherClientUser1.changeAPILifeCycleStatus(updateRequest);
        
        waitForAPIDeployment();
        
        //get the info page from the publisher
        resp = apiPublisherClientUser1.getAPIInformationPage(API_NAME, providerName, API_VERSION_2_0_0);
        //state should be in DEPRECATED now
        assertEquals(resp.getData().contains("DEPRECATED"), true, "API is not in DEPRECATED state");
        //check LC history
        assertEquals(resp.getData().contains("changed the API status from  'BLOCKED' to 'DEPRECATED'"), true,
                "Incorrect state change displayed");
        
        //check for LC state change buttons       
        assertEquals(resp.getData().contains("Retire"), true, "'Retire' not visible");    
  
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }
}
