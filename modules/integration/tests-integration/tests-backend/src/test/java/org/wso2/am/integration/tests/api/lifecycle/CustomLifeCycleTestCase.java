/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.am.integration.tests.api.lifecycle;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.wso2.am.admin.clients.lifecycle.LifeCycleAdminClient;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class CustomLifeCycleTestCase extends APIManagerLifecycleBaseTest {

    private static final String API_NAME = "APICustomLifecycleTestApi";
    private LifeCycleAdminClient lifeCycleAdminClient;

    private String apiEndPointUrl;
    private AuthenticatorClient loginClient;
    private String backendUrl;

    private static final String API_LIFECYCLE_PATH = "artifacts/AM/configFiles/lifecycle/APILifeCycle.json";
    private static final String TENANT_CONFIG_PATH = "artifacts/AM/configFiles/tenantConf/tenant-conf.json";
    private static final String UTF_8 = "UTF-8";
    private String apiId;

    @Factory(dataProvider = "userModeDataProvider")
    public CustomLifeCycleTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    @BeforeClass
    public void initialize() throws  Exception {
        super.init(userMode);
        backendUrl = gatewayContextMgt.getContextUrls().getBackEndUrl();
        loginClient = new AuthenticatorClient(backendUrl);
        //admin login to use the GovernanceAdminClient
        String session = loginClient.login(user.getUserName(), user.getPassword(), "localhost");
        lifeCycleAdminClient = new LifeCycleAdminClient(backendUrl, session);

        InputStream tenantConfigStream = getClass().getClassLoader().getResourceAsStream(TENANT_CONFIG_PATH);
        JSONParser jsonParser = new JSONParser();
        JSONObject tenantJsonObject = (JSONObject) jsonParser.parse(
                new InputStreamReader(tenantConfigStream, UTF_8));

        InputStream lcStream = getClass().getClassLoader().getResourceAsStream(API_LIFECYCLE_PATH);
        jsonParser = new JSONParser();
        JSONObject lcJsonObj = (JSONObject) jsonParser.parse(
                new InputStreamReader(lcStream, UTF_8));

        tenantJsonObject.put("LifeCycle",lcJsonObj);
        restAPIAdmin.updateTenantConfig(tenantJsonObject);

        String gatewayUrl;
        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(gatewayContextWrk.getContextTenant().getDomain())) {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp();
        } else {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" +
                    gatewayContextWrk.getContextTenant().getDomain() + "/";
        }
        apiEndPointUrl = gatewayUrl + "jaxrs_basic/services/customers/customerservice";
        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setSandbox(apiEndPointUrl);
        apiRequest.setProvider(user.getUserName());
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();
    }

    @Test(groups = {"wso2.am"}, description = "Check custom life cycle state.")
    public void testCustomLifeCycle() throws Exception {
        //Create and publish api
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction());
        Assert.assertEquals(APILifeCycleState.PUBLISHED.getState(), restAPIPublisher.getLifecycleStatus(apiId).
                getData(), "lifecycle not changed to published");
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PROMOTE.getAction());
        Assert.assertEquals(APILifeCycleState.PROMOTED.getState(), restAPIPublisher.getLifecycleStatus(apiId).getData(),
                "lifecycle not changed to custom");
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.RE_PUBLISH.getAction());
        Assert.assertEquals(APILifeCycleState.PUBLISHED.getState(), restAPIPublisher.getLifecycleStatus(apiId).
                        getData(),
                "lifecycle not changed to published");
    }

    @AfterClass
    public void cleanupArtifacts() throws Exception {
        //Remove test api and revert to original lifecycle config
        restAPIPublisher.deleteAPI(apiId);
    }
}
