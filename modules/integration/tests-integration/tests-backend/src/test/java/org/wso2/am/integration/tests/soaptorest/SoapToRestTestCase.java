/*
 *
 *   Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.soaptorest;

import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.proto.RequestHeader;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIScopeDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ScopeDTO;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class SoapToRestTestCase extends APIMIntegrationBaseTest {
    private static final Log log = LogFactory.getLog(SoapToRestTestCase.class);

    private final String SOAPTOREST_API_NAME = "PhoneVerification";
    private final String API_CONTEXT = "phoneverify";
    private final String API_VERSION_1_0_0 = "1.0";
    private final String END_POINT_URL = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    private static final String SOAPTOREST_TEST_USER = "soaptorestuser";
    private static final String SOAPTOREST_TEST_USER_PASSWORD = "soaptorestuser";
    private static final String SOAPTOREST_ROLE = "soaptorestrole";
    private static final long WAIT_TIME = 45 * 1000;

    private String wsdlDefinition;
    private String soapToRestAPIId;
    private String testAppId1;
    private String testAppId2;
    private String testAppId3;
    private String payload = "{\n" + "   \"CheckPhoneNumber\":{\n" + "      \"PhoneNumber\":\"18006785432\",\n"
            + "      \"LicenseKey\":\"0\"\n" + "   }\n" + "}";
    private String resourceName = "/checkPhoneNumber";

    @Factory(dataProvider = "userModeDataProvider")
    public SoapToRestTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        userManagementClient.addUser(SOAPTOREST_TEST_USER, SOAPTOREST_TEST_USER_PASSWORD, new String[]{}, null);
        userManagementClient.addRole(SOAPTOREST_ROLE, new String[]{SOAPTOREST_TEST_USER}, new String[]{});
        wsdlDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("soap" + File.separator + "phoneverify.wsdl"),
                "UTF-8");

        File file = getTempFileWithContent(wsdlDefinition);
        restAPIPublisher.validateWsdlDefinition(null, file);

        ArrayList<String> environment = new ArrayList<String>();
        environment.add(Constants.GATEWAY_ENVIRONMENT);

        ArrayList<String> policies = new ArrayList<String>();
        policies.add("Unlimited");

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", SOAPTOREST_API_NAME);
        additionalPropertiesObj.put("context", API_CONTEXT);
        additionalPropertiesObj.put("version", API_VERSION_1_0_0);

        JSONObject endpointObject = new JSONObject();
        endpointObject.put("type", "address");
        endpointObject.put("url", END_POINT_URL);

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "address");
        endpointConfig.put("sandbox_endpoints", endpointObject);
        endpointConfig.put("production_endpoints", endpointObject);

        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("gatewayEnvironments", environment);
        additionalPropertiesObj.put("policies", policies);

        // Create SOAPTOREST API
        APIDTO apidto = restAPIPublisher
                .importWSDLDefinition(file, null, additionalPropertiesObj.toString(), "SOAPTOREST");
        soapToRestAPIId = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(soapToRestAPIId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
                SOAPTOREST_API_NAME + " API creation is failed");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(soapToRestAPIId, restAPIPublisher);
        // Publish api
        restAPIPublisher.changeAPILifeCycleStatus(soapToRestAPIId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(user.getUserName(), SOAPTOREST_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
    }


    @Test(groups = {"wso2.am"}, description = "API invocation using JWT App")
    public void testInvokeSoapToRestAPIUsingJWTApplication() throws Exception {
        String graphqlJwtAppName = "PhoneVerificationJWTAPP";
        testAppId1 = createSoapToRestAppAndSubscribeToAPI(graphqlJwtAppName, "JWT");

        // Generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(testAppId1, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + resourceName;

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type",  "application/json");
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);

        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected");

    }

    @Test(groups = {"wso2.am"}, description = "API invocation using oauth App")
    public void testInvokeSoapToRestAPIUsingOAuthApplication() throws Exception {
        String soapToRestOAUTHAppName = "PhoneVerificationOauthAPP";
        testAppId2 = createSoapToRestAppAndSubscribeToAPI(soapToRestOAUTHAppName, "OAUTH");

        // Generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(testAppId2, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + resourceName;

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type",  "application/json");
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);

        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected");
    }

    @Test(groups = { "wso2.am" }, description = "Oauth Scopes", dependsOnMethods = {
            "testInvokeSoapToRestAPIUsingOAuthApplication",
            "testInvokeSoapToRestAPIUsingJWTApplication" })
    public void testOperationalLevelOAuthScopesForSoapToRest()
            throws Exception {
        ArrayList role = new ArrayList();
        role.add(SOAPTOREST_ROLE);

        ScopeDTO scopeObject = new ScopeDTO();
        scopeObject.setName("subscriber");
        scopeObject.setBindings(role);

        APIScopeDTO apiScopeDTO = new APIScopeDTO();
        apiScopeDTO.setScope(scopeObject);

        ArrayList apiScopeList = new ArrayList();
        apiScopeList.add(apiScopeDTO);

        HttpResponse createdApiResponse = restAPIPublisher.getAPI(soapToRestAPIId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(createdApiResponse.getData(), APIDTO.class);
        apidto.setScopes(apiScopeList);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, soapToRestAPIId);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(soapToRestAPIId, restAPIPublisher);

        ArrayList scope = new ArrayList();
        scope.add("subscriber");
        List<APIOperationsDTO> operations = updatedAPI.getOperations();
        operations.forEach((item) ->
                {
                    if (item.getTarget().equals(resourceName)) {
                        item.setScopes(scope);
                    }
                }
        );

        apidto.operations(operations);
        restAPIPublisher.updateAPI(apidto, soapToRestAPIId);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(soapToRestAPIId, restAPIPublisher);

        testAppId3 = createSoapToRestAppAndSubscribeToAPI("testOperationalLevelOAuthScopesForSoapToRest", "OAUTH");
        // Keep sufficient time to update map
        Thread.sleep(10000);
        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        // Generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);

        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + resourceName;
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Content-Type", "application/json");

        // Invoke api without authorized scope
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(testAppId3, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        String tokenJti = TokenUtils.getJtiOfJwtToken(accessToken);
        log.info("Access Token response without scope: " + accessToken);
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + tokenJti);
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_FORBIDDEN,
                "Response code is not as expected");

        String consumerKey = applicationKeyDTO.getConsumerKey();
        String consumerSecret = applicationKeyDTO.getConsumerSecret();
        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        HttpResponse response;
        String requestBody;
        JSONObject accessTokenGenerationResponse;
        String username = SOAPTOREST_TEST_USER;
        // Obtain user access token for Admin
        if (userMode != TestUserMode.SUPER_TENANT_ADMIN) {
            username = username.concat("@").concat(user.getUserDomain());
        }
        requestBody =
                "grant_type=password&username=" + username + "&password=" + SOAPTOREST_TEST_USER_PASSWORD +
                        "&scope=subscriber";

        response = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL);
        accessTokenGenerationResponse = new JSONObject(response.getData());
        log.info("Access Token response with scope: " + response.getData());
        accessToken = accessTokenGenerationResponse.getString("access_token");
        tokenJti = TokenUtils.getJtiOfJwtToken(accessToken);
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + tokenJti);
        serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected");
    }

    @Test(groups = {"wso2.am"}, description = "Oauth Scopes", dependsOnMethods = {
            "testOperationalLevelOAuthScopesForSoapToRest"})
    public void testOperationalLevelSecurityForSoapToRest() throws Exception {
        HttpResponse response = restAPIPublisher.getAPI(soapToRestAPIId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);

        List<APIOperationsDTO> operations = apidto.getOperations();
        operations.forEach((item) ->
                {
                    if (item.getTarget().equals(resourceName)) {
                        item.setAuthType("None");
                    }
                }
        );

        apidto.operations(operations);
        restAPIPublisher.updateAPI(apidto, soapToRestAPIId);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(soapToRestAPIId, restAPIPublisher);
        waitForAPIDeployment();

        // Generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);

        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + resourceName;
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Content-Type", "application/json");

        // Invoke api without security
        String accessToken = "";
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        boolean status = false;
        long waitTime = System.currentTimeMillis() + WAIT_TIME;
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected");
    }

    private String createSoapToRestAppAndSubscribeToAPI(String appName, String tokenType) throws ApiException {
        ApplicationDTO applicationDTO = restAPIStore.addApplicationWithTokenType(appName,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "",
                "test app for SOAPTOREST API", tokenType);
        String testApiId = applicationDTO.getApplicationId();
        restAPIStore.subscribeToAPI(soapToRestAPIId, testApiId, APIMIntegrationConstants.API_TIER.UNLIMITED);
        return testApiId;
    }

    private File getTempFileWithContent(String schema) throws Exception {
        File temp = File.createTempFile("phoneverify", ".wsdl");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(schema);
        out.close();
        return temp;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        userManagementClient.deleteRole(SOAPTOREST_ROLE);
        userManagementClient.deleteUser(SOAPTOREST_TEST_USER);
        restAPIStore.deleteApplication(testAppId1);
        restAPIStore.deleteApplication(testAppId2);
        restAPIStore.deleteApplication(testAppId3);
        undeployAndDeleteAPIRevisionsUsingRest(soapToRestAPIId, restAPIPublisher);
        restAPIPublisher.deleteAPI(soapToRestAPIId);
        super.cleanUp();
    }
}