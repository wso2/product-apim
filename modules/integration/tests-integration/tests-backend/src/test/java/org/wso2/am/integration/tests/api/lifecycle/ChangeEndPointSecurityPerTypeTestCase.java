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
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
import org.wso2.am.integration.test.ClientAuthenticator;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.ApplicationKeyBean;
import org.wso2.am.integration.test.utils.bean.DCRParamRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import waffle.util.Base64;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.wso2.am.integration.test.utils.base.APIMIntegrationConstants.SUPER_TENANT_DOMAIN;

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
    String endpointUsername = "admin";
    String endpointPassword = "admin123";
    ApplicationKeyBean applicationKeyBeanProduction;
    ApplicationKeyBean applicationKeyBeanSandbox;
    private HashMap<String, String> requestHeadersGet;
    private String providerName;
    private String apiEndPointUrl;
    private String applicationID;
    private String apiID;
    private ApplicationKeyDTO productionApplication;
    private ApplicationKeyDTO sandboxApplication;

    @Factory(dataProvider = "userModeDataProvider")
    public ChangeEndPointSecurityPerTypeTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.TENANT_ADMIN},
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {

        super.init(userMode);
        String dcrURL = backEndServerUrl.getWebAppURLHttp()+ "client-registration/v0.17/register";
        apiEndPointUrl = getGatewayURLNhttp() + "backendSecurity/1.0.0";
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
        apiCreationRequestBean.setProvider(user.getUserName());
        apiCreationRequestBean.setEndpointType(APIEndpointSecurityDTO.TypeEnum.BASIC.getValue());
        apiCreationRequestBean.setEpUsername(endpointUsername);
        apiCreationRequestBean.setEpPassword(endpointPassword);
        apiIdentifier.setTier(TIER_UNLIMITED);
        APIDTO apidto =
                createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean, restAPIPublisher, restAPIStore,
                        applicationID, TIER_UNLIMITED);
        apiID = apidto.getId();
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

        DCRParamRequest oauthAppProduction = new DCRParamRequest("dummyapp--production", null, null,
                user.getUserName(), "password client_credentials", dcrURL, user.getUserName(),
                user.getPassword(), null);
        DCRParamRequest oauthAppSandbox = new DCRParamRequest("dummyapp--sandbox", null, null, user.getUserName(),
                "password client_credentials", dcrURL, user.getUserName(), user.getPassword(), null);
        applicationKeyBeanProduction = ClientAuthenticator.makeDCRRequest(oauthAppProduction);
        applicationKeyBeanSandbox = ClientAuthenticator.makeDCRRequest(oauthAppSandbox);

    }

    // 3.1.0 related.
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

    // 3.1.0 related.
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
    // 3.1.0 related.
    @Test(groups = {
            "wso2.am"}, description = "Test Set Endpoint Security", dependsOnMethods =
            "testEndpointSecurityInGlobalUpdatingAPI")
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

    // Migrated API to 3.2.0 with Security updating production security.
    @Test(groups = {
            "wso2.am"}, description = "Override Endpoint Security for production", dependsOnMethods =
            "testUpdateEndpointSecurityForGlobal")
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
        Object endpointConfig = apidto.getEndpointConfig();
        JSONObject endpointConfigJson = new JSONObject();
        endpointConfigJson.putAll((Map) endpointConfig);
        endpointConfigJson.put("endpoint_security", new JSONParser().parse(productionEndpointSecurity));
        apidto.setEndpointConfig(endpointConfigJson);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiID);
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
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode(endpointUsername.concat(":").concat("wso2carbon").getBytes())));
    }

    // Migrated API to 3.2.0 with Security updating sandbox security.
    @Test(groups = {
            "wso2.am"}, description = "Override endpoint Security for Sandbox", dependsOnMethods =
            "testUpdateEndpointSecurityForProduction")
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
        Object endpointConfig = apidto.getEndpointConfig();
        JSONObject endpointConfigJson = new JSONObject();
        endpointConfigJson.putAll((Map) endpointConfig);
        endpointConfigJson.put("endpoint_security", new JSONParser().parse(sandboxEndpointSecurity));
        apidto.setEndpointConfig(endpointConfigJson);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiID);
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
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode(endpointUsername.concat(":").concat("wso2carbon").getBytes())));
        String sandAppTokenJti = TokenUtils.getJtiOfJwtToken(sandboxApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + sandAppTokenJti);
        HttpResponse sandboxResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(sandboxResponse.getResponseCode(), 200);
        headers = sandboxResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode("sandboxusername".concat(":").concat("admin123#QA").getBytes())));
    }

    // Migrated API to 3.2.0 with Security updating production and sandbox security.
    @Test(groups = {
            "wso2.am"}, description = "Override Endpoint Security for production and sandbox", dependsOnMethods =
            "testUpdateEndpointSecurityForSandbox")
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
        Object endpointConfig = apidto.getEndpointConfig();
        JSONObject endpointConfigJson = new JSONObject();
        endpointConfigJson.putAll((Map) endpointConfig);
        endpointConfigJson.put("endpoint_security", new JSONParser().parse(sandboxEndpointSecurity));
        apidto.setEndpointConfig(endpointConfigJson);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiID);
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

    @Test(groups = {
            "wso2.am"}, description = "Disable Endpoint Security In Sandbox.", dependsOnMethods =
            "testUpdateEndpointSecurityForSandboxAndProduction")
    public void testDisableEndpointSecurityInSandbox() throws Exception {

        String sandboxEndpointSecurity = "{\n" +
                "  \"production\":{\n" +
                "    \"enabled\":true,\n" +
                "    \"type\":\"BASIC\",\n" +
                "    \"username\":\"production1234\",\n" +
                "    \"password\":\"admin123#dev\"\n" +
                "  },\n" +
                "  \"sandbox\":{\n" +
                "    \"enabled\":false,\n" +
                "    \"type\":\"BASIC\",\n" +
                "    \"username\":\"sandbox1234\",\n" +
                "    \"password\":\"admin123#prod\"\n" +
                "  }\n" +
                "  }";
        HttpResponse response = restAPIPublisher.getAPI(apiID);
        APIDTO apidto = new Gson().fromJson(response.getData(), APIDTO.class);
        Object endpointConfig = apidto.getEndpointConfig();
        JSONObject endpointConfigJson = new JSONObject();
        endpointConfigJson.putAll((Map) endpointConfig);
        endpointConfigJson.put("endpoint_security", new JSONParser().parse(sandboxEndpointSecurity));
        apidto.setEndpointConfig(endpointConfigJson);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiID);
        Map updatedEndpointConfig = (Map) updatedAPI.getEndpointConfig();
        Assert.assertNotNull(updatedEndpointConfig.get("endpoint_security"));
        Map endpointSecurity = (Map) updatedEndpointConfig.get("endpoint_security");
        Assert.assertNotNull(endpointSecurity.get("sandbox"));
        Map endpointSecurityModel = (Map) endpointSecurity.get("sandbox");
        Assert.assertFalse((Boolean) endpointSecurityModel.get("enabled"));
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
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "");
    }

    @Test(groups = {
            "wso2.am"}, description = "Disable Endpoint Security In Sandbox.", dependsOnMethods =
            "testDisableEndpointSecurityInSandbox")
    public void testDisableEndpointSecurityInProduction() throws Exception {

        String sandboxEndpointSecurity = "{\n" +
                "  \"production\":{\n" +
                "    \"enabled\":false,\n" +
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
        Object endpointConfig = apidto.getEndpointConfig();
        JSONObject endpointConfigJson = new JSONObject();
        endpointConfigJson.putAll((Map) endpointConfig);
        endpointConfigJson.put("endpoint_security", new JSONParser().parse(sandboxEndpointSecurity));
        apidto.setEndpointConfig(endpointConfigJson);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiID);
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
        Assert.assertFalse((Boolean) productionMap.get("enabled"));
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
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "");
        String sandAppTokenJti = TokenUtils.getJtiOfJwtToken(sandboxApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + sandAppTokenJti);
        HttpResponse sandboxResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(sandboxResponse.getResponseCode(), 200);
        headers = sandboxResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode("sandbox1234".concat(":").concat("admin123#prod").getBytes())));
    }

    @Test(groups = {
            "wso2.am"}, description = "Disable Endpoint Security In sandbox and production.", dependsOnMethods =
            "testDisableEndpointSecurityInProduction")
    public void testDisableEndpointSecurityInProductionAndSandbox() throws Exception {

        String sandboxEndpointSecurity = "{\n" +
                "  \"production\":{\n" +
                "    \"enabled\":false,\n" +
                "    \"type\":\"BASIC\",\n" +
                "    \"username\":\"production1234\",\n" +
                "    \"password\":\"admin123#dev\"\n" +
                "  },\n" +
                "  \"sandbox\":{\n" +
                "    \"enabled\":false,\n" +
                "    \"type\":\"BASIC\",\n" +
                "    \"username\":\"sandbox1234\",\n" +
                "    \"password\":\"admin123#prod\"\n" +
                "  }\n" +
                "  }";
        HttpResponse response = restAPIPublisher.getAPI(apiID);
        APIDTO apidto = new Gson().fromJson(response.getData(), APIDTO.class);
        Object endpointConfig = apidto.getEndpointConfig();
        JSONObject endpointConfigJson = new JSONObject();
        endpointConfigJson.putAll((Map) endpointConfig);
        endpointConfigJson.put("endpoint_security", new JSONParser().parse(sandboxEndpointSecurity));
        apidto.setEndpointConfig(endpointConfigJson);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiID);
        Map updatedEndpointConfig = (Map) updatedAPI.getEndpointConfig();
        Assert.assertNotNull(updatedEndpointConfig.get("endpoint_security"));
        Map endpointSecurity = (Map) updatedEndpointConfig.get("endpoint_security");
        Assert.assertNotNull(endpointSecurity.get("sandbox"));
        Map endpointSecurityModel = (Map) endpointSecurity.get("sandbox");
        Assert.assertFalse((Boolean) endpointSecurityModel.get("enabled"));
        Assert.assertTrue("sandbox1234".equals(endpointSecurityModel.get("username")));
        Assert.assertTrue("".equals(endpointSecurityModel.get("password")));
        Assert.assertTrue("basic".equalsIgnoreCase((String) endpointSecurityModel.get("type")));
        Assert.assertNotNull(endpointSecurity.get("production"));
        Map productionMap = (Map) endpointSecurity.get("production");
        Assert.assertFalse((Boolean) productionMap.get("enabled"));
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
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "");
        String sandAppTokenJti = TokenUtils.getJtiOfJwtToken(sandboxApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + sandAppTokenJti);
        HttpResponse sandboxResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(sandboxResponse.getResponseCode(), 200);
        headers = sandboxResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "");
    }

    // 3.2.0 base test
    @Test(groups = {
            "wso2.am"}, description = "Update Endpoint Security for production as 3.2.0", dependsOnMethods =
            "testDisableEndpointSecurityInProductionAndSandbox")
    public void testUpdateEndpointSecurityForProduction320() throws Exception {

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

    // Base 3.2.0 api update
    @Test(groups = {
            "wso2.am"}, description = "Override endpoint Security for Sandbox", dependsOnMethods =
            "testUpdateEndpointSecurityForProduction320")
    public void testUpdateEndpointSecurityForSandbox320() throws Exception {

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

    // Migrated API to 3.2.0 with Security updating production and sandbox security.
    @Test(groups = {
            "wso2.am"}, description = "Override Endpoint Security for production and sandbox", dependsOnMethods =
            "testUpdateEndpointSecurityForSandbox320")
    public void testUpdateEndpointSecurityForSandboxAndProduction320() throws Exception {

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

    @Test(groups = {"wso2.am"}, description = "Test Set OAuth Endpoint Security with Client Credentials Grant Type",
            dependsOnMethods = "testUpdateEndpointSecurityForSandboxAndProduction")
    public void testUpdateEndpointSecurityForOauthWithClientCredentialsGrantType() throws Exception {

        String endpointSecurity = "{\n" +
                "  \"production\":{\n" +
                "    \"enabled\":true,\n" +
                "    \"type\":\"OAUTH\",\n" +
                "    \"tokenUrl\":\"https://localhost:9943/oauth2/token\",\n" +
                "    \"clientId\":\"" + applicationKeyBeanProduction.getConsumerKey() + "\",\n" +
                "    \"clientSecret\":\"" + applicationKeyBeanProduction.getConsumerSecret() + "\",\n" +
                "    \"customParameters\":{},\n" +
                "    \"grantType\":\"CLIENT_CREDENTIALS\"\n" +
                "  },\n" +
                "  \"sandbox\":{\n" +
                "    \"enabled\":true,\n" +
                "    \"type\":\"OAUTH\",\n" +
                "    \"tokenUrl\":\"https://localhost:9943/oauth2/token\",\n" +
                "    \"clientId\":\"" + applicationKeyBeanSandbox.getConsumerKey() + "\",\n" +
                "    \"clientSecret\":\"" + applicationKeyBeanSandbox.getConsumerSecret() + "\",\n" +
                "    \"customParameters\":{},\n" +
                "    \"grantType\":\"CLIENT_CREDENTIALS\"\n" +
                "  }\n" +
                "  }";
        HttpResponse response = restAPIPublisher.getAPI(apiID);
        APIDTO apidto = new Gson().fromJson(response.getData(), APIDTO.class);
        Object endpointConfig = apidto.getEndpointConfig();
        JSONObject endpointConfigJson = new JSONObject();
        endpointConfigJson.putAll((Map) endpointConfig);
        endpointConfigJson.put("endpoint_security", new JSONParser().parse(endpointSecurity));
        apidto.setEndpointConfig(endpointConfigJson);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiID);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        Map updatedEndpointConfig = (Map) updatedAPI.getEndpointConfig();
        Assert.assertNotNull(updatedEndpointConfig.get("endpoint_security"));
        Map endpointSecurityModel = (Map) updatedEndpointConfig.get("endpoint_security");
        Assert.assertNotNull(endpointSecurityModel.get("sandbox"));
        Map sandboxEndpointSecurityModel = (Map) endpointSecurityModel.get("sandbox");
        Assert.assertTrue((Boolean) sandboxEndpointSecurityModel.get("enabled"));
        Assert.assertEquals(sandboxEndpointSecurityModel.get("type"), "OAUTH");
        Assert.assertEquals(sandboxEndpointSecurityModel.get("tokenUrl"), "https://localhost:9943/oauth2/token");
        Assert.assertEquals(sandboxEndpointSecurityModel.get("clientId"), applicationKeyBeanSandbox.getConsumerKey());
        Assert.assertEquals(sandboxEndpointSecurityModel.get("clientSecret"),
                applicationKeyBeanSandbox.getConsumerSecret());
        Assert.assertNotNull(endpointSecurityModel.get("production"));
        Map productionEndpointSecurityModel = (Map) endpointSecurityModel.get("production");
        Assert.assertTrue((Boolean) productionEndpointSecurityModel.get("enabled"));
        Assert.assertEquals(productionEndpointSecurityModel.get("type"), "OAUTH");
        Assert.assertEquals(productionEndpointSecurityModel.get("tokenUrl"), "https://localhost:9943/oauth2/token");
        Assert.assertEquals(productionEndpointSecurityModel.get("clientId"),
                applicationKeyBeanProduction.getConsumerKey());
        Assert.assertEquals(productionEndpointSecurityModel.get("clientSecret"),
                applicationKeyBeanProduction.getConsumerSecret());

        String prodAppTokenJti = TokenUtils.getJtiOfJwtToken(productionApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + prodAppTokenJti);
        HttpResponse productionResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(productionResponse.getResponseCode(), 200);
        Map<String, String> headers = productionResponse.getHeaders();
        String authorization = headers.get("BACKEND_AUTHORIZATION_HEADER");
        Assert.assertNotNull(authorization);
        Assert.assertTrue(authorization.contains("Bearer"));
        String backendToken = authorization.replaceFirst("Bearer ", "");
        validateIntrospectionResponse(user, backendToken, applicationKeyBeanProduction.getConsumerKey());
        String sandAppTokenJti = TokenUtils.getJtiOfJwtToken(sandboxApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + sandAppTokenJti);
        HttpResponse sandboxResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(sandboxResponse.getResponseCode(), 200);
        headers = sandboxResponse.getHeaders();
        authorization = headers.get("BACKEND_AUTHORIZATION_HEADER");
        Assert.assertNotNull(authorization);
        Assert.assertTrue(authorization.contains("Bearer"));
        backendToken = authorization.replaceFirst("Bearer ", "");
        validateIntrospectionResponse(user, backendToken, applicationKeyBeanSandbox.getConsumerKey());
    }

    @Test(groups = {"wso2.am"}, description = "Test Set OAuth Endpoint Security with Password Grant Type",
            dependsOnMethods = "testUpdateEndpointSecurityForSandboxAndProduction")
    public void testUpdateEndpointSecurityForOauthWithPasswordGrantType() throws Exception {

        String username = user.getUserName();
        String password = user.getPassword();
        String endpointSecurity = "{\n" +
                "  \"production\":{\n" +
                "    \"enabled\":true,\n" +
                "    \"type\":\"OAUTH\",\n" +
                "    \"tokenUrl\":\"https://localhost:9943/oauth2/token\",\n" +
                "    \"clientId\":\"" + applicationKeyBeanProduction.getConsumerKey() + "\",\n" +
                "    \"clientSecret\":\"" + applicationKeyBeanProduction.getConsumerSecret() + "\",\n" +
                "    \"customParameters\":{},\n" +
                "    \"grantType\":\"PASSWORD\"\n" +
                "    \"username\":\"" + username + "\",\n" +
                "    \"password\":\"" + password + "\",\n" +
                "  },\n" +
                "  \"sandbox\":{\n" +
                "    \"enabled\":true,\n" +
                "    \"type\":\"OAUTH\",\n" +
                "    \"tokenUrl\":\"https://localhost:9943/oauth2/token\",\n" +
                "    \"clientId\":\"" + applicationKeyBeanSandbox.getConsumerKey() + "\",\n" +
                "    \"clientSecret\":\"" + applicationKeyBeanSandbox.getConsumerSecret() + "\",\n" +
                "    \"customParameters\":{},\n" +
                "    \"grantType\":\"PASSWORD\"\n" +
                "    \"username\":\"" + username + "\",\n" +
                "    \"password\":\"" + password + "\",\n" +
                "  }\n" +
                "  }";
        HttpResponse response = restAPIPublisher.getAPI(apiID);
        APIDTO apidto = new Gson().fromJson(response.getData(), APIDTO.class);
        Object endpointConfig = apidto.getEndpointConfig();
        JSONObject endpointConfigJson = new JSONObject();
        endpointConfigJson.putAll((Map) endpointConfig);
        endpointConfigJson.put("endpoint_security", new JSONParser().parse(endpointSecurity));
        apidto.setEndpointConfig(endpointConfigJson);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiID);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        Map updatedEndpointConfig = (Map) updatedAPI.getEndpointConfig();
        Assert.assertNotNull(updatedEndpointConfig.get("endpoint_security"));
        Map endpointSecurityModel = (Map) updatedEndpointConfig.get("endpoint_security");
        Assert.assertNotNull(endpointSecurityModel.get("sandbox"));
        Map sandboxEndpointSecurityModel = (Map) endpointSecurityModel.get("sandbox");
        Assert.assertTrue((Boolean) sandboxEndpointSecurityModel.get("enabled"));
        Assert.assertEquals(sandboxEndpointSecurityModel.get("type"), "OAUTH");
        Assert.assertEquals(sandboxEndpointSecurityModel.get("tokenUrl"), "https://localhost:9943/oauth2/token");
        Assert.assertEquals(sandboxEndpointSecurityModel.get("clientId"), applicationKeyBeanSandbox.getConsumerKey());
        Assert.assertEquals(sandboxEndpointSecurityModel.get("clientSecret"),
                applicationKeyBeanSandbox.getConsumerSecret());
        Assert.assertEquals(sandboxEndpointSecurityModel.get("username"), username);
        Assert.assertEquals(sandboxEndpointSecurityModel.get("password"), "");
        Assert.assertNotNull(endpointSecurityModel.get("production"));
        Map productionEndpointSecurityModel = (Map) endpointSecurityModel.get("production");
        Assert.assertTrue((Boolean) productionEndpointSecurityModel.get("enabled"));
        Assert.assertEquals(productionEndpointSecurityModel.get("type"), "OAUTH");
        Assert.assertEquals(productionEndpointSecurityModel.get("tokenUrl"), "https://localhost:9943/oauth2/token");
        Assert.assertEquals(productionEndpointSecurityModel.get("clientId"),
                applicationKeyBeanProduction.getConsumerKey());
        Assert.assertEquals(productionEndpointSecurityModel.get("clientSecret"),
                applicationKeyBeanProduction.getConsumerSecret());
        Assert.assertEquals(productionEndpointSecurityModel.get("username"), username);
        Assert.assertEquals(productionEndpointSecurityModel.get("password"), "");

        String prodAppTokenJti = TokenUtils.getJtiOfJwtToken(productionApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + prodAppTokenJti);
        HttpResponse productionResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(productionResponse.getResponseCode(), 200);
        Map<String, String> headers = productionResponse.getHeaders();
        String authorization = headers.get("BACKEND_AUTHORIZATION_HEADER");
        Assert.assertNotNull(authorization);
        Assert.assertTrue(authorization.contains("Bearer"));
        String backendToken = authorization.replaceFirst("Bearer ", "");
        validateIntrospectionResponse(user, backendToken, applicationKeyBeanProduction.getConsumerKey());
        String sandAppTokenJti = TokenUtils.getJtiOfJwtToken(sandboxApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + sandAppTokenJti);
        HttpResponse sandboxResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(sandboxResponse.getResponseCode(), 200);
        headers = sandboxResponse.getHeaders();
        authorization = headers.get("BACKEND_AUTHORIZATION_HEADER");
        Assert.assertNotNull(authorization);
        Assert.assertTrue(authorization.contains("Bearer"));
        backendToken = authorization.replaceFirst("Bearer ", "");
        validateIntrospectionResponse(user, backendToken, applicationKeyBeanSandbox.getConsumerKey());
    }

    private void validateIntrospectionResponse(User user, String accessToken, String clientId)
            throws UnsupportedEncodingException {

        String introspectionUrl = "https://localhost:9943/oauth2/introspect";
        if (!SUPER_TENANT_DOMAIN.equals(user.getUserDomain())) {
            introspectionUrl = "https://localhost:9943/t/" + user.getUserDomain() + "/oauth2/introspect";
        }
        CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost();
        httpPost.addHeader("Authorization",
                "Basic " + Base64.encode(user.getUserName().concat(":").concat(user.getPassword()).getBytes()));
        httpPost.setURI(URI.create(introspectionUrl));
        UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(Arrays.asList(new BasicNameValuePair(
                "token", accessToken)));
        httpPost.setEntity(urlEncodedFormEntity);
        try (CloseableHttpResponse response = closeableHttpClient.execute(httpPost)) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
            HttpEntity entity = response.getEntity();
            JSONObject jsonPayload = (JSONObject) new JSONParser().parse(new InputStreamReader(entity.getContent()));
            Assert.assertTrue((Boolean) jsonPayload.get("active"));
            Assert.assertNotNull(jsonPayload.get("client_id"));
            Assert.assertEquals(jsonPayload.get("client_id"), clientId);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {

        restAPIStore.removeApplicationById(applicationID);
        restAPIPublisher.deleteAPIByID(apiID);
        super.cleanUp();
    }

}
