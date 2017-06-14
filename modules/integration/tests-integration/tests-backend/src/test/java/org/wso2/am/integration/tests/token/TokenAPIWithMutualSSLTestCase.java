/*
*Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.token;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.util.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE})
public class TokenAPIWithMutualSSLTestCase extends APIMIntegrationBaseTest {
    private APIStoreRestClient apiStore;
    private ServerConfigurationManager serverConfigurationManager;

    private static final Log log = LogFactory.getLog(TokenAPITestCase.class);

    @Factory(dataProvider = "userModeDataProvider")
    public TokenAPIWithMutualSSLTestCase(TestUserMode userMode) {
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
        apiStore = new APIStoreRestClient(getStoreURLHttp());
        apiStore.login(user.getUserName(), user.getPassword());
        serverConfigurationManager =
                new ServerConfigurationManager(new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME
                        , APIMIntegrationConstants.AM_STORE_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN));
        serverConfigurationManager.applyConfigurationWithoutRestart(
                new File(getAMResourceLocation() + File.separator + "configFiles/tokenTest/" + "axis2.xml"));
    }

    @Test(groups = {"wso2.am"}, description = "Token API with mutual SSL Test")
    public void testTokenAPIWithSSLTestCase() throws Exception {
        // Create application
        apiStore.addApplication("TokenTestAPI-Application", APIMIntegrationConstants.APPLICATION_TIER.LARGE, "",
                "this-is-test");

        //Generate production token and invoke with that
        APPKeyRequestGenerator generateAppKeyRequest =
                new APPKeyRequestGenerator("TokenTestAPI-Application");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Assert.notNull(accessToken, "Unable to retrieve access token with mutual SSL");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication("TokenTestAPI-Application");
        serverConfigurationManager.restoreToLastConfiguration();
        super.cleanUp();
    }
}
