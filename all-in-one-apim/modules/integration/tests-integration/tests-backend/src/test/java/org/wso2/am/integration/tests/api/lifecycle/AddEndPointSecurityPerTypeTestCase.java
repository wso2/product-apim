/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.io.IOUtils;
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
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.ClientAuthenticator;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Add APIs with the endpoint security and invoke. Endpoint application was developed to return thr security token in
 * the response body.
 */
public class AddEndPointSecurityPerTypeTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(AddEndPointSecurityPerTypeTestCase.class);
    private final String API_NAME = "AddEndPointSecurityPerTypeTestCase";
    private final String API_CONTEXT = "AddEndPointSecurityPerTypeTestCase";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "AddEndPointSecurityPerTypeTestCase";
    ApplicationKeyBean applicationKeyBeanProduction;
    ApplicationKeyBean applicationKeyBeanSandbox;
    private HashMap<String, String> requestHeadersGet;
    private String providerName;
    private String apiEndPointUrl;
    private String applicationID;
    private ApplicationKeyDTO productionApplication;
    private ApplicationKeyDTO sandboxApplication;
    private APIIdentifier apiIdentifier;
    private String dcrURL;
    private String clientCredGrantTypeEndpointSecurityForProductionAndSandbox;
    ArrayList<String> apiIds = new ArrayList<>();

    @Factory(dataProvider = "userModeDataProvider")
    public AddEndPointSecurityPerTypeTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
                new Object[]{TestUserMode.SUPER_TENANT_USER},
                new Object[]{TestUserMode.TENANT_USER}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {

        super.init(userMode);
        dcrURL = backEndServerUrl.getWebAppURLHttp()+ "client-registration/v0.17/register";
        apiEndPointUrl = getGatewayURLNhttp() + "backendSecurity/1.0.0";
        providerName = user.getUserName();
        requestHeadersGet = new HashMap<>();
        requestHeadersGet.put("accept", "text/plain");
        requestHeadersGet.put("Content-Type", "text/plain");

        //Create application
        ApplicationDTO dto = restAPIStore.addApplicationWithTokenType(APPLICATION_NAME,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "",
                ApplicationDTO.TokenTypeEnum.OAUTH.getValue());
        applicationID = dto.getApplicationId();
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        productionApplication = restAPIStore
                .generateKeys(applicationID, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                        null, grantTypes);
        sandboxApplication = restAPIStore
                .generateKeys(applicationID, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX,
                        null, grantTypes);
        DCRParamRequest oauthAppProduction = new DCRParamRequest(UUID.randomUUID().toString(), null, null,
                user.getUserName(), "password client_credentials", dcrURL, user.getUserName(),
                user.getPassword(), null);
        DCRParamRequest oauthAppSandbox = new DCRParamRequest(UUID.randomUUID().toString(), null, null, user.getUserName(),
                "password client_credentials", dcrURL, user.getUserName(), user.getPassword(), null);
        applicationKeyBeanProduction = ClientAuthenticator.makeDCRRequest(oauthAppProduction);
        applicationKeyBeanSandbox = ClientAuthenticator.makeDCRRequest(oauthAppSandbox);
        clientCredGrantTypeEndpointSecurityForProductionAndSandbox = "{\n" +
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
    }

    @Test(groups = {"wso2.am"}, description = "Add Endpoint Security for production")
    public void testAddEndpointSecurityForProduction() throws Exception {

        // Create an API
        String apiName = API_NAME + "1";
        String apiContext = API_CONTEXT + "1";
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiName, apiContext, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl), new URL(apiEndPointUrl));
        apiCreationRequestBean.setTier(TIER_UNLIMITED);
        apiCreationRequestBean.setTiersCollection(TIER_UNLIMITED);
        apiCreationRequestBean.setProvider(user.getUserName());
        apiCreationRequestBean.setSetEndpointSecurityDirectlyToEndpoint(Boolean.TRUE);
        apiIdentifier = new APIIdentifier(providerName, apiName, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_UNLIMITED);
        String productionEndpointSecurity = "{\n" +
                "  \"production\":{\n" +
                "    \"enabled\":true,\n" +
                "    \"type\":\"BASIC\",\n" +
                "    \"username\":\"admin1234\",\n" +
                "    \"password\":\"admin123#QA\"\n" +
                "  }\n" +
                "  }";
        org.json.JSONObject endpointConfig = apiCreationRequestBean.getEndpoint();
        endpointConfig.put("endpoint_security", new JSONParser().parse(productionEndpointSecurity));
        apiCreationRequestBean.setEndpoint(endpointConfig);
        APIDTO apidto =
                createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean, restAPIPublisher, restAPIStore,
                        applicationID, TIER_UNLIMITED);
        apiIds.add(apidto.getId());
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apidto.getId(), restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), apiName, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        waitForAPIDeploymentSync(user.getUserName(), apiName, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);

        Map addedEndpointConfig = (Map) apidto.getEndpointConfig();
        Assert.assertNotNull(addedEndpointConfig.get("endpoint_security"));
        Map endpointSecurity = (Map) addedEndpointConfig.get("endpoint_security");
        Assert.assertNotNull(endpointSecurity.get("production"));
        Map endpointSecurityModel = (Map) endpointSecurity.get("production");
        Assert.assertTrue((Boolean) endpointSecurityModel.get("enabled"));
        Assert.assertTrue("admin1234".equals(endpointSecurityModel.get("username")));
        Assert.assertTrue("".equals(endpointSecurityModel.get("password")));
        Assert.assertTrue("basic".equalsIgnoreCase((String) endpointSecurityModel.get("type")));
        String prodAppTokenJti = TokenUtils.getJtiOfJwtToken(productionApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + prodAppTokenJti);
        HttpResponse productionResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(apiContext, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(productionResponse.getResponseCode(), 200);
        Map<String, String> headers = productionResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode("admin1234".concat(":").concat("admin123#QA").getBytes())));
        String sandAppTokenJti = TokenUtils.getJtiOfJwtToken(sandboxApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + sandAppTokenJti);
        HttpResponse sandboxResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(apiContext, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(sandboxResponse.getResponseCode(), 200);
        headers = sandboxResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "");
    }

    @Test(groups = {
            "wso2.am"}, description = "Add Endpoint Security for Sandbox", dependsOnMethods =
            "testAddEndpointSecurityForProduction")
    public void testAddEndpointSecurityForSandbox() throws Exception {

        // Create an API
        String apiName = API_NAME + "2";
        String apiContext = API_CONTEXT + "2";
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiName, apiContext, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl), new URL(apiEndPointUrl));
        apiCreationRequestBean.setTier(TIER_UNLIMITED);
        apiCreationRequestBean.setTiersCollection(TIER_UNLIMITED);
        apiCreationRequestBean.setProvider(user.getUserName());
        apiCreationRequestBean.setSetEndpointSecurityDirectlyToEndpoint(Boolean.TRUE);
        apiIdentifier = new APIIdentifier(providerName, apiName, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_UNLIMITED);
        String sandboxEndpointSecurity = "{\n" +
                "  \"sandbox\":{\n" +
                "    \"enabled\":true,\n" +
                "    \"type\":\"BASIC\",\n" +
                "    \"username\":\"sandboxusername\",\n" +
                "    \"password\":\"admin123#QA\"\n" +
                "  }\n" +
                "  }";
        org.json.JSONObject endpointConfig = apiCreationRequestBean.getEndpoint();
        endpointConfig.put("endpoint_security", new JSONParser().parse(sandboxEndpointSecurity));
        apiCreationRequestBean.setEndpoint(endpointConfig);
        APIDTO apidto =
                createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean, restAPIPublisher, restAPIStore,
                        applicationID, TIER_UNLIMITED);
        apiIds.add(apidto.getId());
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apidto.getId(), restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), apiName, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        waitForAPIDeploymentSync(user.getUserName(), apiName, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);

        Map addedEndpointConfig = (Map) apidto.getEndpointConfig();
        Assert.assertNotNull(addedEndpointConfig.get("endpoint_security"));
        Map endpointSecurity = (Map) addedEndpointConfig.get("endpoint_security");
        Assert.assertNotNull(endpointSecurity.get("sandbox"));
        Map endpointSecurityModel = (Map) endpointSecurity.get("sandbox");
        Assert.assertTrue((Boolean) endpointSecurityModel.get("enabled"));
        Assert.assertTrue("sandboxusername".equals(endpointSecurityModel.get("username")));
        Assert.assertTrue("".equals(endpointSecurityModel.get("password")));
        Assert.assertTrue("basic".equalsIgnoreCase((String) endpointSecurityModel.get("type")));
        String prodAppTokenJti = TokenUtils.getJtiOfJwtToken(productionApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + prodAppTokenJti);
        HttpResponse productionResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(apiContext, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(productionResponse.getResponseCode(), 200);
        Map<String, String> headers = productionResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "");
        String sandAppTokenJti = TokenUtils.getJtiOfJwtToken(sandboxApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + sandAppTokenJti);
        HttpResponse sandboxResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(apiContext, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(sandboxResponse.getResponseCode(), 200);
        headers = sandboxResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode("sandboxusername".concat(":").concat("admin123#QA").getBytes())));
    }

    @Test(groups = {
            "wso2.am"}, description = "Add Endpoint Security for Production and Sandbox", dependsOnMethods =
            "testAddEndpointSecurityForSandbox")
    public void testAddEndpointSecurityForSandboxAndProduction() throws Exception {

        // Create an API
        String apiName = API_NAME + "3";
        String apiContext = API_CONTEXT + "3";
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiName, apiContext, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl), new URL(apiEndPointUrl));
        apiCreationRequestBean.setTier(TIER_UNLIMITED);
        apiCreationRequestBean.setTiersCollection(TIER_UNLIMITED);
        apiCreationRequestBean.setProvider(user.getUserName());
        apiCreationRequestBean.setSetEndpointSecurityDirectlyToEndpoint(Boolean.TRUE);
        apiIdentifier = new APIIdentifier(providerName, apiName, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_UNLIMITED);
        String productionAndSandboxEndpointSecurity = "{\n" +
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
        org.json.JSONObject endpointConfig = apiCreationRequestBean.getEndpoint();
        endpointConfig.put("endpoint_security", new JSONParser().parse(productionAndSandboxEndpointSecurity));
        apiCreationRequestBean.setEndpoint(endpointConfig);
        APIDTO apidto =
                createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean, restAPIPublisher, restAPIStore,
                        applicationID, TIER_UNLIMITED);
        apiIds.add(apidto.getId());
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apidto.getId(), restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), apiName, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        waitForAPIDeploymentSync(user.getUserName(), apiName, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);

        Map addedEndpointConfig = (Map) apidto.getEndpointConfig();
        Assert.assertNotNull(addedEndpointConfig.get("endpoint_security"));
        Map endpointSecurity = (Map) addedEndpointConfig.get("endpoint_security");
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
        String prodAppTokenJti = TokenUtils.getJtiOfJwtToken(productionApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + prodAppTokenJti);
        HttpResponse productionResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(apiContext, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(productionResponse.getResponseCode(), 200);
        Map<String, String> headers = productionResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode("production1234".concat(":").concat("admin123#dev").getBytes())));
        String sandAppTokenJti = TokenUtils.getJtiOfJwtToken(sandboxApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + sandAppTokenJti);
        HttpResponse sandboxResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(apiContext, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(sandboxResponse.getResponseCode(), 200);
        headers = sandboxResponse.getHeaders();
        Assert.assertEquals(headers.get("BACKEND_AUTHORIZATION_HEADER"), "Basic".concat(" ")
                .concat(Base64.encode("sandbox1234".concat(":").concat("admin123#prod").getBytes())));
    }

    @Test(groups = { "wso2.am" }, description = "Test add OAuth endpoint security for CLIENT_CREDENTIALS grant type",
            dependsOnMethods = "testAddEndpointSecurityForSandboxAndProduction")
    public void testAddEndpointSecurityForOauthForClientCredentialsGrantType() throws Exception {

        // Create an API
        String apiName = API_NAME + "4";
        String apiContext = API_CONTEXT + "4";
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiName, apiContext, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl), new URL(apiEndPointUrl));
        apiCreationRequestBean.setTier(TIER_UNLIMITED);
        apiCreationRequestBean.setTiersCollection(TIER_UNLIMITED);
        apiCreationRequestBean.setProvider(user.getUserName());
        apiCreationRequestBean.setSetEndpointSecurityDirectlyToEndpoint(Boolean.TRUE);
        apiIdentifier = new APIIdentifier(providerName, apiName, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_UNLIMITED);
        org.json.JSONObject endpointConfig = apiCreationRequestBean.getEndpoint();
        endpointConfig.put("endpoint_security",
                new JSONParser().parse(clientCredGrantTypeEndpointSecurityForProductionAndSandbox));
        apiCreationRequestBean.setEndpoint(endpointConfig);
        APIDTO apidto =
                createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean, restAPIPublisher, restAPIStore,
                        applicationID, TIER_UNLIMITED);
        apiIds.add(apidto.getId());
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apidto.getId(), restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), apiName, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        waitForAPIDeploymentSync(user.getUserName(), apiName, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);

        Map addedEndpointConfig = (Map) apidto.getEndpointConfig();
        Assert.assertNotNull(addedEndpointConfig.get("endpoint_security"));
        Map endpointSecurityModel = (Map) addedEndpointConfig.get("endpoint_security");
        Assert.assertNotNull(endpointSecurityModel.get("sandbox"));
        Map sandboxEndpointSecurityModel = (Map) endpointSecurityModel.get("sandbox");
        Assert.assertTrue((Boolean) sandboxEndpointSecurityModel.get("enabled"));
        Assert.assertEquals(sandboxEndpointSecurityModel.get("type"), "OAUTH");
        Assert.assertEquals(sandboxEndpointSecurityModel.get("tokenUrl"), "https://localhost:9943/oauth2/token");
        Assert.assertEquals(sandboxEndpointSecurityModel.get("clientId"), applicationKeyBeanSandbox.getConsumerKey());
        Assert.assertEquals(sandboxEndpointSecurityModel.get("clientSecret"), "");
        Assert.assertNotNull(endpointSecurityModel.get("production"));
        Map productionEndpointSecurityModel = (Map) endpointSecurityModel.get("production");
        Assert.assertTrue((Boolean) productionEndpointSecurityModel.get("enabled"));
        Assert.assertEquals(productionEndpointSecurityModel.get("type"), "OAUTH");
        Assert.assertEquals(productionEndpointSecurityModel.get("tokenUrl"), "https://localhost:9943/oauth2/token");
        Assert.assertEquals(productionEndpointSecurityModel.get("clientId"),
                applicationKeyBeanProduction.getConsumerKey());
        Assert.assertEquals(productionEndpointSecurityModel.get("clientSecret"), "");

        String prodAppTokenJti = TokenUtils.getJtiOfJwtToken(productionApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + prodAppTokenJti);
        HttpResponse productionResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(apiContext, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(productionResponse.getResponseCode(), 200);
        Map<String, String> headers = productionResponse.getHeaders();
        String authorization = headers.get("BACKEND_AUTHORIZATION_HEADER");
        Assert.assertNotNull(authorization);
        Assert.assertTrue(authorization.contains("Bearer"));
        String backendToken = authorization.replaceFirst("Bearer ", "");
        validateIntrospectionResponse(user, backendToken, applicationKeyBeanProduction.getConsumerKey());

        // checking 2nd request also works
        HttpResponse productionResponse2 =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(apiContext, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(productionResponse2.getResponseCode(), 200);

        String sandAppTokenJti = TokenUtils.getJtiOfJwtToken(sandboxApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + sandAppTokenJti);
        HttpResponse sandboxResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(apiContext, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(sandboxResponse.getResponseCode(), 200);
        headers = sandboxResponse.getHeaders();
        authorization = headers.get("BACKEND_AUTHORIZATION_HEADER");
        Assert.assertNotNull(authorization);
        Assert.assertTrue(authorization.contains("Bearer"));
        backendToken = authorization.replaceFirst("Bearer ", "");
        validateIntrospectionResponse(user, backendToken, applicationKeyBeanSandbox.getConsumerKey());
    }

    @Test(groups = {"wso2.am"}, description = "API definition import with endpoint security",
            dependsOnMethods = "testAddEndpointSecurityForOauthForClientCredentialsGrantType")
    public void testAPIDefinitionImportWithEndpointSecurity() throws Exception {
        String resourcePath = "oas" + File.separator + "v3" + File.separator;
        String originalDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "oas_import.json"),
                "UTF-8");
        String additionalProperties = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "additionalProperties.json"),
                "UTF-8");

        org.json.JSONObject additionalPropertiesObj = new org.json.JSONObject(additionalProperties);
        additionalPropertiesObj.put("provider", user.getUserName());

        org.json.JSONObject endpointConfig = (org.json.JSONObject) additionalPropertiesObj.get("endpointConfig");
        endpointConfig.put("endpoint_security",
                new JSONParser().parse(clientCredGrantTypeEndpointSecurityForProductionAndSandbox));
        additionalPropertiesObj.put("endpointConfig", endpointConfig);

        File file = geTempFileWithContent(originalDefinition);
        APIDTO apidto = restAPIPublisher.importOASDefinition(file, additionalPropertiesObj.toString());
        apiIds.add(apidto.getId());

        restAPIPublisher.changeAPILifeCycleStatus(apidto.getId(), Constants.PUBLISHED);

        Map addedEndpointConfig = (Map) apidto.getEndpointConfig();
        Assert.assertNotNull(addedEndpointConfig.get("endpoint_security"));
        Map endpointSecurityModel = (Map) addedEndpointConfig.get("endpoint_security");
        Assert.assertNotNull(endpointSecurityModel.get("sandbox"));
        Map sandboxEndpointSecurityModel = (Map) endpointSecurityModel.get("sandbox");
        Assert.assertTrue((Boolean) sandboxEndpointSecurityModel.get("enabled"));
        Assert.assertEquals(sandboxEndpointSecurityModel.get("type"), "OAUTH");
        Assert.assertEquals(sandboxEndpointSecurityModel.get("tokenUrl"), "https://localhost:9943/oauth2/token");
        Assert.assertEquals(sandboxEndpointSecurityModel.get("clientId"), applicationKeyBeanSandbox.getConsumerKey());
        Assert.assertEquals(sandboxEndpointSecurityModel.get("clientSecret"), "");
        Assert.assertNotNull(endpointSecurityModel.get("production"));
        Map productionEndpointSecurityModel = (Map) endpointSecurityModel.get("production");
        Assert.assertTrue((Boolean) productionEndpointSecurityModel.get("enabled"));
        Assert.assertEquals(productionEndpointSecurityModel.get("type"), "OAUTH");
        Assert.assertEquals(productionEndpointSecurityModel.get("tokenUrl"), "https://localhost:9943/oauth2/token");
        Assert.assertEquals(productionEndpointSecurityModel.get("clientId"),
                applicationKeyBeanProduction.getConsumerKey());
        Assert.assertEquals(productionEndpointSecurityModel.get("clientSecret"), "");
    }

    @Test(groups = { "wso2.am"}, description = "Test add OAuth endpoint security for PASSWORD grant type",
            dependsOnMethods = "testAddEndpointSecurityForOauthForClientCredentialsGrantType")
    public void testAddEndpointSecurityForOauthForPasswordGrantType() throws Exception {

        final String clientCredGrantTypeEndpointSecurityForProductionAndSandboxForPasswordGrantType = "{\n" +
                "  \"production\":{\n" +
                "    \"enabled\":true,\n" +
                "    \"type\":\"OAUTH\",\n" +
                "    \"username\":\"" + user.getUserName() + "\",\n" +
                "    \"password\":\"" + user.getPassword() + "\",\n" +
                "    \"tokenUrl\":\"https://localhost:9943/oauth2/token\",\n" +
                "    \"clientId\":\"" + applicationKeyBeanProduction.getConsumerKey() + "\",\n" +
                "    \"clientSecret\":\"" + applicationKeyBeanProduction.getConsumerSecret() + "\",\n" +
                "    \"customParameters\":{},\n" +
                "    \"grantType\":\"PASSWORD\"\n" +
                "  },\n" +
                "  \"sandbox\":{\n" +
                "    \"enabled\":true,\n" +
                "    \"type\":\"OAUTH\",\n" +
                "    \"username\":\"" + user.getUserName() + "\",\n" +
                "    \"password\":\"" + user.getPassword() + "\",\n" +
                "    \"tokenUrl\":\"https://localhost:9943/oauth2/token\",\n" +
                "    \"clientId\":\"" + applicationKeyBeanSandbox.getConsumerKey() + "\",\n" +
                "    \"clientSecret\":\"" + applicationKeyBeanSandbox.getConsumerSecret() + "\",\n" +
                "    \"customParameters\":{},\n" +
                "    \"grantType\":\"PASSWORD\"\n" +
                "  }\n" +
                "  }";

        // Create an API
        String apiName = API_NAME + "5";
        String apiContext = API_CONTEXT + "5";
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiName, apiContext, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl), new URL(apiEndPointUrl));
        apiCreationRequestBean.setTier(TIER_UNLIMITED);
        apiCreationRequestBean.setTiersCollection(TIER_UNLIMITED);
        apiCreationRequestBean.setProvider(user.getUserName());
        apiCreationRequestBean.setSetEndpointSecurityDirectlyToEndpoint(Boolean.TRUE);
        apiIdentifier = new APIIdentifier(providerName, apiName, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_UNLIMITED);
        org.json.JSONObject endpointConfig = apiCreationRequestBean.getEndpoint();
        endpointConfig.put("endpoint_security",
                new JSONParser().parse(clientCredGrantTypeEndpointSecurityForProductionAndSandboxForPasswordGrantType));
        apiCreationRequestBean.setEndpoint(endpointConfig);
        APIDTO apidto =
                createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean, restAPIPublisher, restAPIStore,
                        applicationID, TIER_UNLIMITED);
        apiIds.add(apidto.getId());
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apidto.getId(), restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), apiName, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        waitForAPIDeploymentSync(user.getUserName(), apiName, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);

        Map addedEndpointConfig = (Map) apidto.getEndpointConfig();
        Assert.assertNotNull(addedEndpointConfig.get("endpoint_security"));
        Map endpointSecurityModel = (Map) addedEndpointConfig.get("endpoint_security");
        Assert.assertNotNull(endpointSecurityModel.get("sandbox"));
        Map sandboxEndpointSecurityModel = (Map) endpointSecurityModel.get("sandbox");
        Assert.assertTrue((Boolean) sandboxEndpointSecurityModel.get("enabled"));
        Assert.assertEquals(sandboxEndpointSecurityModel.get("type"), "OAUTH");
        Assert.assertEquals(sandboxEndpointSecurityModel.get("tokenUrl"), "https://localhost:9943/oauth2/token");
        Assert.assertEquals(sandboxEndpointSecurityModel.get("clientId"), applicationKeyBeanSandbox.getConsumerKey());
        Assert.assertEquals(sandboxEndpointSecurityModel.get("clientSecret"), "");
        Assert.assertNotNull(endpointSecurityModel.get("production"));
        Map productionEndpointSecurityModel = (Map) endpointSecurityModel.get("production");
        Assert.assertTrue((Boolean) productionEndpointSecurityModel.get("enabled"));
        Assert.assertEquals(productionEndpointSecurityModel.get("type"), "OAUTH");
        Assert.assertEquals(productionEndpointSecurityModel.get("tokenUrl"), "https://localhost:9943/oauth2/token");
        Assert.assertEquals(productionEndpointSecurityModel.get("clientId"),
                applicationKeyBeanProduction.getConsumerKey());
        Assert.assertEquals(productionEndpointSecurityModel.get("clientSecret"), "");

        String prodAppTokenJti = TokenUtils.getJtiOfJwtToken(productionApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + prodAppTokenJti);
        HttpResponse productionResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(apiContext, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(productionResponse.getResponseCode(), 200);
        Map<String, String> headers = productionResponse.getHeaders();
        String authorization = headers.get("BACKEND_AUTHORIZATION_HEADER");
        Assert.assertNotNull(authorization);
        Assert.assertTrue(authorization.contains("Bearer"));
        String backendToken = authorization.replaceFirst("Bearer ", "");
        validateIntrospectionResponse(user, backendToken, applicationKeyBeanProduction.getConsumerKey());

        // checking 2nd request also works
        HttpResponse productionResponse2 =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(apiContext, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(productionResponse2.getResponseCode(), 200);

        String sandAppTokenJti = TokenUtils.getJtiOfJwtToken(sandboxApplication.getToken().getAccessToken());
        requestHeadersGet.put("Authorization", "Bearer " + sandAppTokenJti);
        HttpResponse sandboxResponse =
                HTTPSClientUtils.doGet(getAPIInvocationURLHttp(apiContext, API_VERSION_1_0_0), requestHeadersGet);
        Assert.assertEquals(sandboxResponse.getResponseCode(), 200);
        headers = sandboxResponse.getHeaders();
        authorization = headers.get("BACKEND_AUTHORIZATION_HEADER");
        Assert.assertNotNull(authorization);
        Assert.assertTrue(authorization.contains("Bearer"));
        backendToken = authorization.replaceFirst("Bearer ", "");
        validateIntrospectionResponse(user, backendToken, applicationKeyBeanSandbox.getConsumerKey());
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {

        restAPIStore.removeApplicationById(applicationID);
        for (String apiId: apiIds) {
            undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
            restAPIPublisher.deleteAPI(apiId);
        }
        super.cleanUp();
    }

    private void validateIntrospectionResponse(User user, String accessToken, String clientId) throws Exception {

        String introspectionUrl = "https://localhost:9943/oauth2/introspect";
        if (!"carbon.super".equals(user.getUserDomain())) {
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
            log.error(e.getMessage());
            throw new Exception(e);
        }
    }

    private File geTempFileWithContent(String swagger) throws Exception {
        File temp = File.createTempFile("swagger", ".json");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(swagger);
        out.close();
        return temp;
    }
}
