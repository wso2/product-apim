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


package org.wso2.am.integration.tests.restapi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class RESTAPITestCase extends APIMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
//    private ServerConfigurationManager serverConfigurationManager;

    private static final Log log = LogFactory.getLog(RESTAPITestCase.class);

    @Factory(dataProvider = "userModeDataProvider")
    public RESTAPITestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);


        apiPublisher = new APIPublisherRestClient(getPublisherURLHttp());
        apiStore = new APIStoreRestClient(getStoreURLHttp());

        apiPublisher.login(user.getUserName(), user.getPassword());
        apiStore.login(user.getUserName(), user.getPassword());

    }

    @Test(groups = {"wso2.am"}, description = "DCR Test to obtain Token")
    public void registerOAuthApplication() throws Exception {
        String dcrEndpointURL =getKeyManagerURLHttp()+"client-registration/v1/register";
        String applicationRequestBody = "          {\n" +
                "          \"callbackUrl\": \"www.google.lk\",\n" +
                "          \"clientName\": \"fffff\",\n" +
                "          \"tokenScope\": \"Production\",\n" +
                "          \"owner\": \"admin\",\n" +
                "          \"grantType\": \"password refresh_token\",\n" +
                "          \"saasApp\": true\n" +
                "         }";
        Map<String, String> dcrRequestHeaders = new HashMap<String, String>();
        dcrRequestHeaders.put("Authorization", "Basic YWRtaW46YWRtaW4=");
        dcrRequestHeaders.put("Content-Type", "application/json");
        JSONObject clientRegistrationResponse =new JSONObject(HttpRequestUtil.doPost(new URL(dcrEndpointURL), applicationRequestBody,dcrRequestHeaders));

        String consumerKey =new JSONObject(clientRegistrationResponse.getString("data")).get("clientId").toString();
        String consumerSecret =new JSONObject(clientRegistrationResponse.getString("data")).get("clientSecret").toString();
        Thread.sleep(2000);
        String requestBody = "grant_type=password&username=admin&password=admin&scope=PRODUCTION";
        URL tokenEndpointURL = new URL(getGatewayURLNhttp() + "token");
        JSONObject accessTokenGenerationResponse = new JSONObject(
                apiStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody,
                                               tokenEndpointURL).getData()
        );
        String userAccessToken = accessTokenGenerationResponse.getString("access_token");
        String refreshToken = accessTokenGenerationResponse.getString("refresh_token");
        assertNotNull(userAccessToken);
        log.info(refreshToken);
        Map<String, String> requestHeaders = new HashMap<String, String>();
        //Check User Access Token
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);
        requestHeaders.put("accept", "text/xml");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication("TokenTestAPI-Application");
        super.cleanUp();
    }
}
