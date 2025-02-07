/*
 *Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.server.restart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.*;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class GraphqlServerRestartTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(GraphqlServerRestartTestCase.class);
    private final String API_CONTEXT = "info";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String RESPONSE_DATA = "[{\"name\":\"Afrikaans\",\"code\":\"af\"},{\"name\":\"Amharic\",\"code\":\"am\"}," +
            "{\"name\":\"Arabic\",\"code\":\"ar\"},{\"name\":\"Aymara\",\"code\":\"ay\"},{\"name\":\"Azerbaijani\"," +
            "\"code\":\"az\"},{\"name\":\"Belarusian\",\"code\":\"be\"}]";
    private static final String GRAPHQL_TEST_USER = "graphqluser";
    private static final String GRAPHQL_TEST_USER_PASSWORD = "graphqlUser";
    private static final String GRAPHQL_ROLE = "graphqlrole";
    private static final long WAIT_TIME = 45 * 1000;
    private String graphQLSchemaDefinition;
    private String graphQLAPIId;
    private String testAppId1;
    private String testAppId2;
    private String testAppId3;
    private String testAppId4;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment(ITestContext ctx) throws Exception {
        super.init();
        graphQLAPIId = (String) ctx.getAttribute("graphQLAPIId");
        graphQLSchemaDefinition = (String) ctx.getAttribute("graphQLSchemaDefinition");
    }

    @Test(groups = {"wso2.am"}, description = "Create and publish GraphQL APIs by providing GraphQL schema with " +
            "interface types")
    public void createAndPublishGraphQLAPIUsingSchemaWithInterfaces() throws Exception {

        waitForAPIDeploymentSync(user.getUserName(), "CountriesGraphqlAPI", "1.0.0",
                APIMIntegrationConstants.IS_API_EXISTS);

        String schemaDefinitionWithInterface = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("graphql" + File.separator
                        + "schemaWithInterface.graphql"), StandardCharsets.UTF_8);
        File file = getTempFileWithContent(schemaDefinitionWithInterface);
        GraphQLValidationResponseDTO responseApiDto = restAPIPublisher.validateGraphqlSchemaDefinition(file);
        GraphQLValidationResponseGraphQLInfoDTO graphQLInfo = responseApiDto.getGraphQLInfo();
        Assert.assertNotNull(graphQLInfo);
        String arrayToJson = new ObjectMapper().writeValueAsString(graphQLInfo.getOperations());
        JSONArray operations = new JSONArray(arrayToJson);

        ArrayList<String> policies = new ArrayList<>();
        policies.add("Unlimited");

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", "GraphQLAPIWithInterface");
        additionalPropertiesObj.put("context", "interface");
        additionalPropertiesObj.put("version", API_VERSION_1_0_0);

        JSONObject url = new JSONObject();
        String END_POINT_URL = "https://localhost:9943/am-graphQL-sample/api/graphql/";
        url.put("url", END_POINT_URL);
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("sandbox_endpoints", url);
        endpointConfig.put("production_endpoints", url);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("policies", policies);
        additionalPropertiesObj.put("operations", operations);

        // create Graphql API
        APIDTO apidto = restAPIPublisher.importGraphqlSchemaDefinition(file, additionalPropertiesObj.toString());
        String graphqlAPIId = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(graphqlAPIId);
        String GRAPHQL_API_NAME = "CountriesGraphqlAPI";
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
                GRAPHQL_API_NAME + " API creation is failed");

        // publish api
        restAPIPublisher.changeAPILifeCycleStatus(graphqlAPIId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(user.getUserName(), GRAPHQL_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(groups = {"wso2.am"}, description = "test retrieve schemaDefinition at publisher")
    public void testRetrieveSchemaDefinitionAtPublisher() throws Exception {
        GraphQLSchemaDTO schema = restAPIPublisher.getGraphqlSchemaDefinition(graphQLAPIId);
        Assert.assertEquals(schema.getSchemaDefinition(), graphQLSchemaDefinition);
    }

    @Test(groups = {"wso2.am"}, description = "test update schemaDefinition at publisher",
            dependsOnMethods = "testRetrieveSchemaDefinitionAtPublisher")
    public void testUpdateSchemaDefinitionOfAPI() throws Exception {
        //UpdatedSchema
        String updatedSchemaDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("graphql" + File.separator + "updatedSchema.graphql"),
                StandardCharsets.UTF_8);
        restAPIPublisher.updateGraphqlSchemaDefinition(graphQLAPIId, updatedSchemaDefinition);
        GraphQLSchemaDTO schema = restAPIPublisher.getGraphqlSchemaDefinition(graphQLAPIId);
        Assert.assertEquals(schema.getSchemaDefinition(), updatedSchemaDefinition);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(graphQLAPIId, restAPIPublisher);
    }


    @Test(groups = {"wso2.am"}, description = "API invocation using JWT App")
    public void testInvokeGraphqlAPIUsingJWTApplication() throws Exception {
        String graphqlJwtAppName = "CountriesJWTAPP";
        testAppId1 = createGraphqlAppAndSubscribeToAPI(graphqlJwtAppName, "JWT");

        // generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(testAppId1, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO.getToken());
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/";
        Map<String, String> requestHeaders = new HashMap<>();
        JSONObject queryObject = new JSONObject();
        queryObject.put("query", "{languages{code name}}");
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type",  "application/json");
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject.toString());

        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected");
        Assert.assertEquals(serviceResponse.getData(), RESPONSE_DATA, "Response data is not as expected");

    }

    @Test(groups = {"wso2.am"}, description = "API invocation using oauth App")
    public void testInvokeGraphqlAPIUsingOAuthApplication() throws Exception {
        String graphqlOAUTHAppName = "CountriesOauthAPP";
        testAppId2 = createGraphqlAppAndSubscribeToAPI(graphqlOAUTHAppName, "JWT");

        // generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(testAppId2, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO.getToken());
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/";
        Map<String, String> requestHeaders = new HashMap<>();
        JSONObject queryObject = new JSONObject();
        queryObject.put("query", "{languages{code name}}");
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type",  "application/json");
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject.toString());

        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected");
        Assert.assertEquals(serviceResponse.getData(), RESPONSE_DATA, "Response data is not as expected");
    }

    @Test(groups = {"wso2.am"}, description = "Oauth Scopes",
            dependsOnMethods = { "testInvokeGraphqlAPIUsingOAuthApplication","testInvokeGraphqlAPIUsingJWTApplication"})
    public void testOperationalLevelOAuthScopesForGraphql() throws Exception {
        ArrayList role = new ArrayList();
        role.add(GRAPHQL_ROLE);

        ScopeDTO scopeObject = new ScopeDTO();
        scopeObject.setName("subscriber");
        scopeObject.setBindings(role);

        APIScopeDTO apiScopeDTO = new APIScopeDTO();
        apiScopeDTO.setScope(scopeObject);

        ArrayList apiScopeList = new ArrayList();
        apiScopeList.add(apiScopeDTO);

        HttpResponse createdApiResponse = restAPIPublisher.getAPI(graphQLAPIId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(createdApiResponse.getData(), APIDTO.class);
        apidto.setScopes(apiScopeList);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, graphQLAPIId);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(graphQLAPIId, restAPIPublisher);

        ArrayList scope = new ArrayList();
        scope.add("subscriber");
        List<APIOperationsDTO> operations = updatedAPI.getOperations();
        Assert.assertNotNull(operations);
        operations.forEach((item) ->
                {
                    if (item.getTarget()!=null && item.getTarget().equals("languages")) {
                        item.setScopes(scope);
                    }
                }
        );

        apidto.operations(operations);
        restAPIPublisher.updateAPI(apidto, graphQLAPIId);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(graphQLAPIId, restAPIPublisher);

        testAppId3 = createGraphqlAppAndSubscribeToAPI("testOperationalLevelOAuthScopesForGraphql", "OAUTH");
        // Keep sufficient time to update map
        Thread.sleep(10000);
        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        // generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);

        JSONObject queryObject = new JSONObject();
        queryObject.put("query", "{languages{code name}}");
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/";
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Content-Type", "application/json");

        // invoke api without authorized scope
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(testAppId3, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO.getToken());
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        String tokenJti = TokenUtils.getJtiOfJwtToken(accessToken);
        log.info("Access Token response without scope: " + accessToken);
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + tokenJti);
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject.toString());
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_FORBIDDEN,
                "Response code is not as expected");

        String consumerKey = applicationKeyDTO.getConsumerKey();
        String consumerSecret = applicationKeyDTO.getConsumerSecret();
        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        HttpResponse response;
        String requestBody;
        JSONObject accessTokenGenerationResponse;
        String username = GRAPHQL_TEST_USER;
        //Obtain user access token for Admin
        if (userMode != TestUserMode.SUPER_TENANT_ADMIN) {
            username = username.concat("@").concat(user.getUserDomain());
        }
        requestBody =
                "grant_type=password&username=" + username + "&password=" + GRAPHQL_TEST_USER_PASSWORD +
                        "&scope=subscriber";

        response = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL);
        accessTokenGenerationResponse = new JSONObject(response.getData());
        log.info("Access Token response with scope: " + response.getData());
        accessToken = accessTokenGenerationResponse.getString("access_token");
        tokenJti = TokenUtils.getJtiOfJwtToken(accessToken);
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + tokenJti);
        serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject.toString());
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected");
        Assert.assertEquals(serviceResponse.getData(), RESPONSE_DATA, "Response data is not as expected");

    }

    @Test(groups = { "wso2.am" }, description = "Oauth Scopes", dependsOnMethods = {
            "testOperationalLevelOAuthScopesForGraphql" })
    public void testOperationalLevelSecurityForGraphql()
            throws Exception {
        HttpResponse response = restAPIPublisher.getAPI(graphQLAPIId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);

        List<APIOperationsDTO> operations = apidto.getOperations();
        Assert.assertNotNull(operations);
        operations.forEach((item) ->
                {
                    if (item.getTarget()!=null && item.getTarget().equals("languages")) {
                        item.setAuthType("None");
                    }
                }
        );

        apidto.operations(operations);
        restAPIPublisher.updateAPI(apidto, graphQLAPIId);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(graphQLAPIId, restAPIPublisher);
        waitForAPIDeployment();

        testAppId4 = createGraphqlAppAndSubscribeToAPI(  "CountriesOauthAPPForSecurityCheck","OAUTH");
        // Keep sufficient time to update map
        Thread.sleep(10000);
        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        // generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        JSONObject queryObject = new JSONObject();
        queryObject.put("query", "{languages{code name}}");

        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/";
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type",  "application/json");

        // invoke api without security
        String accessToken = "";
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        boolean status = false;
        long waitTime = System.currentTimeMillis() + WAIT_TIME;
        HttpResponse serviceResponse = null;
        while (waitTime > System.currentTimeMillis()) {
            serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject.toString());
            if (HttpStatus.SC_OK == serviceResponse.getResponseCode()) {
                status = true;
                break;
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
        }
        Assert.assertTrue(status,
                "Response code is not as expected");
        Assert.assertEquals(serviceResponse.getData(), RESPONSE_DATA, "Response data is not as expected");
    }

    private String createGraphqlAppAndSubscribeToAPI(String appName, String tokenType) throws ApiException,
            APIManagerIntegrationTestException {

        org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO applicationDTO =
                restAPIStore.addApplicationWithTokenType(appName, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                        "", "test app for countries API", tokenType);
        String testApiId = applicationDTO.getApplicationId();
        restAPIStore.subscribeToAPI(graphQLAPIId, testApiId, APIMIntegrationConstants.API_TIER.UNLIMITED);
        return testApiId;
    }

    private File getTempFileWithContent(String schema) throws Exception {
        File temp = File.createTempFile("schema", ".graphql");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(schema);
        out.close();
        return temp;
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        // Clean up the role only if it is available
        if(userManagementClient.roleNameExists(GRAPHQL_ROLE)){
                userManagementClient.deleteRole(GRAPHQL_ROLE);
            }
        userManagementClient.deleteUser(GRAPHQL_TEST_USER);
        restAPIStore.deleteApplication(testAppId1);
        restAPIStore.deleteApplication(testAppId2);
        restAPIStore.deleteApplication(testAppId3);
        restAPIStore.deleteApplication(testAppId4);
    }
}
