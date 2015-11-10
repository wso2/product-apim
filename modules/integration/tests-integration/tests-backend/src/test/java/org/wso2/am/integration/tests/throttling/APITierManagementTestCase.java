/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIThrottlingTierRequest;
import org.wso2.am.integration.test.utils.clients.AdminDashboardRestClient;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class APITierManagementTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(APITierManagementTestCase.class);
    
    private AdminDashboardRestClient adminDashboard;
    private String tierName = "SmapleTier";
    
    @Factory(dataProvider = "userModeDataProvider")
    public APITierManagementTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }
    
    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }
    
    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String storeURLHttp = getStoreURLHttp();
        adminDashboard = new AdminDashboardRestClient(storeURLHttp);
        
        adminDashboard.login(user.getUserName(), user.getPassword());
        
    }
    
    @Test(groups = {"wso2.am"}, description = "Add tier through admin-dashboard")
    public void addTierTestCase() throws Exception {
        APIThrottlingTierRequest tierRequest = new APIThrottlingTierRequest(tierName, "5", "Sample Tier", 
                                                                            "120", "true", "FREE");
        tierRequest.setAction("addTier");
        HttpResponse addTierResponse = adminDashboard.addTier(tierRequest);
        verifyResponse(addTierResponse);
        
        String getTiersResponse = adminDashboard.getAllTiers().getData();        
        log.info(getTiersResponse);
        Assert.assertTrue(getTiersResponse.contains(tierName), "Added tier not found");
        
    }
    
    @Test(groups = {"wso2.am"}, description = "Delete tier through admin-dashboard")
    public void deleteTierTestCase() throws Exception {
        HttpResponse addTierResponse = adminDashboard.deleteTier(tierName);
        verifyResponse(addTierResponse);
        
        String getTiersResponse = adminDashboard.getAllTiers().getData();        
        log.info(getTiersResponse);
        Assert.assertTrue(!getTiersResponse.contentEquals(tierName), "Tier not deleted");
        
    }

}
