/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIEndpointSecurityDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.Endpoint;
import org.wso2.carbon.apimgt.api.model.EndpointSecurity;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import waffle.util.Base64;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Change the endpoint security of APi and invoke. Endpoint application was developed to return thr security token in
 * the response body.
 */
public class ChangeEndPointSecurityPerTypeTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(ChangeEndPointSecurityPerTypeTestCase.class);
    private final String API_NAME = "ChangeEndPointSecurityPerTypeTestCase";
    private final String API_CONTEXT = "ChangeEndPointSecurityPerTypeTestCase";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "ChangeEndPointSecurityPerTypeTestCase";
    private HashMap<String, String> requestHeadersGet;
    private String providerName;
    private String apiEndPointUrl;
    private String applicationID;
    private String apiID;
    private ApplicationKeyDTO productionApplication;
    private ApplicationKeyDTO sandboxApplication;
    String endpointUsername = "admin";
    String endpointPassword = "admin123";

    @Factory(dataProvider = "userModeDataProvider")
    public ChangeEndPointSecurityPerTypeTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {

        super.init();
        apiEndPointUrl = getAPIInvocationURLHttp("backendSecurity") + "/1.0.0";
        providerName = user.getUserName();
        requestHeadersGet = new HashMap<>();
        requestHeadersGet.put("accept", "text/plain");
        requestHeadersGet.put("Content-Type", "text/plain");
        APIIdentifier apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        //Create application

        ApplicationDTO dto = restAPIStore.addApplicationWithTokenType(APPLICATION_NAME,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "",
                ApplicationDTO.TokenTypeEnum.OAUTH.getValue());
        applicationID = dto.getApplicationId();
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTier(TIER_UNLIMITED);
        apiCreationRequestBean.setTiersCollection(TIER_UNLIMITED);

        apiCreationRequestBean.setEndpointType(APIEndpointSecurityDTO.TypeEnum.BASIC.getValue());
        apiCreationRequestBean.setEpUsername(endpointUsername);
        apiCreationRequestBean.setEpPassword(endpointPassword);
        apiIdentifier.setTier(TIER_UNLIMITED);
        APIDTO apidto =
                createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean, restAPIPublisher, restAPIStore,
                        applicationID, TIER_UNLIMITED);
        apiID = apidto.getId();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        productionApplication = restAPIStore
                .generateKeys(applicationID, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                        null, grantTypes);
        sandboxApplication = restAPIStore
                .generateKeys(applicationID, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX,
                        null, grantTypes);

    }

    @Test(groups = {"wso2.am"}, description = "Test Set Endpoint Security")
    public void testEndpointSecurityInGlobal() throws Exception {
        String prodAppTokenJti = TokenUtils.getJtiOfJwtToken(productionApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + prodAppTokenJti);
        HttpResponse productionResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(productionResponse.getResponseCode(), 200);
        Map<String, String> headers = productionResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode(endpointUsername.concat(":").concat(endpointPassword).getBytes())));
        String sandAppTokenJti = TokenUtils.getJtiOfJwtToken(sandboxApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + sandAppTokenJti);
        HttpResponse sandboxResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(sandboxResponse.getResponseCode(), 200);
        headers = sandboxResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode(endpointUsername.concat(":").concat(endpointPassword).getBytes())));
    }

    @Test(groups = {
            "wso2.am"}, description = "Test Set Endpoint Security", dependsOnMethods = "testEndpointSecurityInGlobal")
    public void testEndpointSecurityInGlobalUpdatingAPI() throws Exception {
        HttpResponse response = restAPIPublisher.getAPI(apiID);
        APIDTO apidto = new Gson().fromJson(response.getData(), APIDTO.class);
        Assert.assertNotNull(apidto.getEndpointSecurity());
        APIEndpointSecurityDTO endpointSecurity = apidto.getEndpointSecurity();
        Assert.assertEquals(endpointSecurity.getPassword(), "");
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiID);
        APIEndpointSecurityDTO updatedEndpointSecurity = updatedAPI.getEndpointSecurity();
        Assert.assertEquals(updatedEndpointSecurity.getPassword(), "");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        String prodAppTokenJti = TokenUtils.getJtiOfJwtToken(productionApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + prodAppTokenJti);
        HttpResponse productionResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(productionResponse.getResponseCode(), 200);
        Map<String, String> headers = productionResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode(endpointUsername.concat(":").concat(endpointPassword).getBytes())));
        String sandAppTokenJti = TokenUtils.getJtiOfJwtToken(sandboxApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + sandAppTokenJti);
        HttpResponse sandboxResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(sandboxResponse.getResponseCode(), 200);
        headers = sandboxResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode(endpointUsername.concat(":").concat(endpointPassword).getBytes())));
    }
    @Test(groups = {
            "wso2.am"}, description = "Test Set Endpoint Security", dependsOnMethods = "testEndpointSecurityInGlobalUpdatingAPI")
    public void testUpdateEndpointSecurityForGlobal() throws Exception {
        HttpResponse response = restAPIPublisher.getAPI(apiID);
        APIDTO apidto = new Gson().fromJson(response.getData(), APIDTO.class);
        Assert.assertNotNull(apidto.getEndpointSecurity());
        APIEndpointSecurityDTO endpointSecurity = apidto.getEndpointSecurity();
        Assert.assertEquals(endpointSecurity.getPassword(), "");
        endpointSecurity.setPassword("wso2carbon");
        apidto.setEndpointSecurity(endpointSecurity);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiID);
        APIEndpointSecurityDTO updatedEndpointSecurity = updatedAPI.getEndpointSecurity();
        Assert.assertEquals(updatedEndpointSecurity.getPassword(), "");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);
        String prodAppTokenJti = TokenUtils.getJtiOfJwtToken(productionApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + prodAppTokenJti);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        HttpResponse productionResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(productionResponse.getResponseCode(), 200);
        Map<String, String> headers = productionResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode(endpointUsername.concat(":").concat("wso2carbon").getBytes())));
        String sandAppTokenJti = TokenUtils.getJtiOfJwtToken(sandboxApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + sandAppTokenJti);
        HttpResponse sandboxResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(sandboxResponse.getResponseCode(), 200);
        headers = sandboxResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode(endpointUsername.concat(":").concat("wso2carbon").getBytes())));
    }

    @Test(groups = {
            "wso2.am"}, description = "Test Set Endpoint Security", dependsOnMethods = "testEndpointSecurityInGlobalUpdatingAPI")
    public void testUpdateEndpointSecurityForProduction() throws Exception {
        String productionEndpointSecurity = "{\n" +
                "  \"production\":{\n" +
                "    \"enabled\":true,\n" +
                "    \"type\":\"BASIC\",\n" +
                "    \"username\":\"admin1234\",\n" +
                "    \"password\":\"admin123#QA\"\n" +
                "  }\n" +
                "  }";
        HttpResponse response = restAPIPublisher.getAPI(apiID);
        APIDTO apidto = new Gson().fromJson(response.getData(), APIDTO.class);
        apidto.setEndpointSecurity(null);
        Object endpointConfig = apidto.getEndpointConfig();
        JSONObject endpointConfigJson = new JSONObject();
        endpointConfigJson.putAll((Map) endpointConfig);
        endpointConfigJson.put("endpoint_security", new JSONParser().parse(productionEndpointSecurity));
        apidto.setEndpointConfig(endpointConfigJson);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiID);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);
        Map updatedEndpointConfig = (Map) updatedAPI.getEndpointConfig();
        Assert.assertNotNull(updatedEndpointConfig.get("endpoint_security"));
        Map endpointSecurity = (Map) updatedEndpointConfig.get("endpoint_security");
        Assert.assertNotNull(endpointSecurity.get("production"));
        Map endpointSecurityModel = (Map) endpointSecurity.get("production");
        Assert.assertTrue((Boolean) endpointSecurityModel.get("enabled"));
        Assert.assertTrue("admin1234".equals(endpointSecurityModel.get("username")));
        Assert.assertTrue("".equals(endpointSecurityModel.get("password")));
        Assert.assertTrue("basic".equalsIgnoreCase((String) endpointSecurityModel.get("type")));
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        String prodAppTokenJti = TokenUtils.getJtiOfJwtToken(productionApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + prodAppTokenJti);
        HttpResponse productionResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(productionResponse.getResponseCode(), 200);
        Map<String, String> headers = productionResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode("admin1234".concat(":").concat("admin123#QA").getBytes())));
        String sandAppTokenJti = TokenUtils.getJtiOfJwtToken(sandboxApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + sandAppTokenJti);
        HttpResponse sandboxResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(sandboxResponse.getResponseCode(), 200);
        headers = sandboxResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "");
    }
    @Test(groups = {
            "wso2.am"}, description = "Test Set Endpoint Security", dependsOnMethods = "testUpdateEndpointSecurityForProduction")
    public void testUpdateEndpointSecurityForSandbox() throws Exception {
        String sandboxEndpointSecurity = "{\n" +
                "  \"sandbox\":{\n" +
                "    \"enabled\":true,\n" +
                "    \"type\":\"BASIC\",\n" +
                "    \"username\":\"sandboxusername\",\n" +
                "    \"password\":\"admin123#QA\"\n" +
                "  }\n" +
                "  }";
        HttpResponse response = restAPIPublisher.getAPI(apiID);
        APIDTO apidto = new Gson().fromJson(response.getData(), APIDTO.class);
        apidto.setEndpointSecurity(null);
        Object endpointConfig = apidto.getEndpointConfig();
        JSONObject endpointConfigJson = new JSONObject();
        endpointConfigJson.putAll((Map) endpointConfig);
        endpointConfigJson.put("endpoint_security", new JSONParser().parse(sandboxEndpointSecurity));
        apidto.setEndpointConfig(endpointConfigJson);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiID);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);
        Map updatedEndpointConfig = (Map) updatedAPI.getEndpointConfig();
        Assert.assertNotNull(updatedEndpointConfig.get("endpoint_security"));
        Map endpointSecurity = (Map) updatedEndpointConfig.get("endpoint_security");
        Assert.assertNotNull(endpointSecurity.get("sandbox"));
        Map endpointSecurityModel = (Map) endpointSecurity.get("sandbox");
        Assert.assertTrue((Boolean) endpointSecurityModel.get("enabled"));
        Assert.assertTrue("sandboxusername".equals(endpointSecurityModel.get("username")));
        Assert.assertTrue("".equals(endpointSecurityModel.get("password")));
        Assert.assertTrue("basic".equalsIgnoreCase((String) endpointSecurityModel.get("type")));
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        String prodAppTokenJti = TokenUtils.getJtiOfJwtToken(productionApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + prodAppTokenJti);
        HttpResponse productionResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(productionResponse.getResponseCode(), 200);
        Map<String, String> headers = productionResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "");
        String sandAppTokenJti = TokenUtils.getJtiOfJwtToken(sandboxApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + sandAppTokenJti);
        HttpResponse sandboxResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(sandboxResponse.getResponseCode(), 200);
        headers = sandboxResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode("sandboxusername".concat(":").concat("admin123#QA").getBytes())));
    }
    @Test(groups = {
            "wso2.am"}, description = "Test Set Endpoint Security", dependsOnMethods = "testUpdateEndpointSecurityForSandbox")
    public void testUpdateEndpointSecurityForSandboxAndProduction() throws Exception {
        String sandboxEndpointSecurity = "{\n" +
                "  \"production\":{\n" +
                "    \"enabled\":true,\n" +
                "    \"type\":\"BASIC\",\n" +
                "    \"username\":\"production1234\",\n" +
                "    \"password\":\"admin123#dev\"\n" +
                "  },\n" +
                "  \"sandbox\":{\n" +
                "    \"enabled\":true,\n" +
                "    \"type\":\"BASIC\",\n" +
                "    \"username\":\"sandbox1234\",\n" +
                "    \"password\":\"admin123#prod\"\n" +
                "  }\n" +
                "  }";
        HttpResponse response = restAPIPublisher.getAPI(apiID);
        APIDTO apidto = new Gson().fromJson(response.getData(), APIDTO.class);
        apidto.setEndpointSecurity(null);
        Object endpointConfig = apidto.getEndpointConfig();
        JSONObject endpointConfigJson = new JSONObject();
        endpointConfigJson.putAll((Map) endpointConfig);
        endpointConfigJson.put("endpoint_security", new JSONParser().parse(sandboxEndpointSecurity));
        apidto.setEndpointConfig(endpointConfigJson);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiID);
        // Undeploy and Delete existing API Revisions Since it has reached 5 max revision limit
        undeployAndDeleteAPIRevisionsUsingRest(apiID, restAPIPublisher);
        waitForAPIDeployment();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);
        Map updatedEndpointConfig = (Map) updatedAPI.getEndpointConfig();
        Assert.assertNotNull(updatedEndpointConfig.get("endpoint_security"));
        Map endpointSecurity = (Map) updatedEndpointConfig.get("endpoint_security");
        Assert.assertNotNull(endpointSecurity.get("sandbox"));
        Map endpointSecurityModel = (Map) endpointSecurity.get("sandbox");
        Assert.assertTrue((Boolean) endpointSecurityModel.get("enabled"));
        Assert.assertTrue("sandbox1234".equals(endpointSecurityModel.get("username")));
        Assert.assertTrue("".equals(endpointSecurityModel.get("password")));
        Assert.assertTrue("basic".equalsIgnoreCase((String) endpointSecurityModel.get("type")));
        Assert.assertNotNull(endpointSecurity.get("production"));
        Map productionMap = (Map) endpointSecurity.get("production");
        Assert.assertTrue((Boolean) productionMap.get("enabled"));
        Assert.assertTrue("production1234".equals(productionMap.get("username")));
        Assert.assertTrue("".equals(productionMap.get("password")));
        Assert.assertTrue("basic".equalsIgnoreCase((String) productionMap.get("type")));
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        String prodAppTokenJti = TokenUtils.getJtiOfJwtToken(productionApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + prodAppTokenJti);
        HttpResponse productionResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(productionResponse.getResponseCode(), 200);
        Map<String, String> headers = productionResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode("production1234".concat(":").concat("admin123#dev").getBytes())));
        String sandAppTokenJti = TokenUtils.getJtiOfJwtToken(sandboxApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + sandAppTokenJti);
        HttpResponse sandboxResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(sandboxResponse.getResponseCode(), 200);
        headers = sandboxResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode("sandbox1234".concat(":").concat("admin123#prod").getBytes())));
    }
    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {

        restAPIStore.removeApplicationById(applicationID);
        undeployAndDeleteAPIRevisionsUsingRest(apiID, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiID);
        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
        };
    }

}
