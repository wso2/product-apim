/*
 *   Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.am.integration.tests.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIScopeDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLCustomComplexityInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLQueryComplexityInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLSchemaTypeDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLSchemaTypeListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseGraphQLInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ScopeDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.tests.graphql.websocket.client.SubscriptionWSClientImpl;
import org.wso2.am.integration.tests.graphql.websocket.server.SubscriptionServerCreator;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.xml.StringUtils;

import javax.ws.rs.core.Response;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class GraphqlSubscriptionTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(GraphqlSubscriptionTestCase.class);
    private static final String GRAPHQL_ROLE = "graphqlSubRole";
    private static final String GRAPHQL_TEST_USER = "graphqlSubUser";
    private static final String GRAPHQL_TEST_USER_PASSWORD = "graphqlSubUser";
    private static final String GRAPHQL_API_NAME = "SnowtoothGraphQLSubAPI";
    private static final String GRAPHQL_API_CONTEXT = "snowtooth";
    private static final String GRAPHQL_API_VERSION = "1.0.0";
    private int webSocketServerPort;
    private String webSocketServerHost;
    private String graphqlApiId;
    private String apiEndPoint;
    String appJWTId;
    ApplicationKeyDTO applicationKeyDTO;
    String throttleAppId;
    String complexAppId;
    String depthAppId;
    Server server = null;
    private String wsRequestEventPublisherSource = "WS_Req_Logger.xml";
    private String wsThrottleOutEventPublisherSource = "WS_Throttle_Out_Logger.xml";
    private ServerConfigurationManager serverConfigurationManager;
    private String wsEventPublisherSource = TestConfigurationProvider.getResourceLocation() + File.separator +
            "artifacts"
            + File.separator + "AM" + File.separator + "configFiles" + File.separator + "webSocketTest"
            + File.separator;
    private String wsEventPublisherTarget = FrameworkPathUtil.getCarbonHome() + File.separator + "repository"
            + File.separator + "deployment" + File.separator + "server" + File.separator + "eventpublishers"
            + File.separator;

    private enum AUTH_IN {
        HEADER,
        QUERY
    }

    @Factory(dataProvider = "userModeDataProvider")
    public GraphqlSubscriptionTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        // Removed the tenant user due to https://github.com/wso2/product-apim/issues/12621
        // Need to revisit this
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN } };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
        serverConfigurationManager.applyConfigurationWithoutRestart
                (new File(wsEventPublisherSource + wsRequestEventPublisherSource),
                        new File(wsEventPublisherTarget + wsRequestEventPublisherSource), false);
        serverConfigurationManager.applyConfigurationWithoutRestart
                (new File(wsEventPublisherSource + wsThrottleOutEventPublisherSource),
                        new File(wsEventPublisherTarget + wsThrottleOutEventPublisherSource), false);
        userManagementClient.addUser(GRAPHQL_TEST_USER, GRAPHQL_TEST_USER_PASSWORD, new String[]{}, null);
        userManagementClient.addRole(GRAPHQL_ROLE, new String[]{GRAPHQL_TEST_USER}, new String[]{});
        webSocketServerHost = InetAddress.getLocalHost().getHostName();
        int lowerPortLimit = 9950;
        int upperPortLimit = 9999;
        webSocketServerPort = getAvailablePort(lowerPortLimit, upperPortLimit);
        if (webSocketServerPort == -1) {
            throw new APIManagerIntegrationTestException("No available port in the range " +
                    lowerPortLimit + "-" + upperPortLimit + " was found");
        }
        log.info("Selected port " + webSocketServerPort + " to start graphql subscription backend server");
        startGraphQLSubscriptionServer(webSocketServerPort);

    }

    @Test(groups = {"wso2.am"}, description = "Publish GraphQL API with Subscriptions")
    public void publishGraphQLAPIWithSubscriptions() throws Exception {

        String arrayToJson = null;
        String schemaDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("graphql" + File.separator + "subscriptions"
                        + File.separator + "schema.graphql"), StandardCharsets.UTF_8);
        File file = getTempFileWithContent(schemaDefinition);
        GraphQLValidationResponseDTO responseApiDto = restAPIPublisher.validateGraphqlSchemaDefinition(file);
        GraphQLValidationResponseGraphQLInfoDTO graphQLInfo = responseApiDto.getGraphQLInfo();
        if (graphQLInfo != null) {
            arrayToJson = new ObjectMapper().writeValueAsString(graphQLInfo.getOperations());
        }
        JSONArray operations = new JSONArray(arrayToJson);

        // add new QueryComplexPolicy Subscription throttling policy
        SubscriptionThrottlePolicyDTO subscriptionThrottlePolicyDTO = new SubscriptionThrottlePolicyDTO();
        createNewComplexSubscriptionPolicyObject(subscriptionThrottlePolicyDTO);
        ApiResponse<SubscriptionThrottlePolicyDTO>
                response = restAPIAdmin.addSubscriptionThrottlingPolicy(subscriptionThrottlePolicyDTO);
        assertEquals(response.getStatusCode(), HttpStatus.SC_CREATED);
        SubscriptionThrottlePolicyDTO addedSubPolicy = response.getData();
        String subPolicyPolicyId = addedSubPolicy.getPolicyId();
        Assert.assertNotNull(subPolicyPolicyId, "The policy ID cannot be null or empty");

        // add new QueryDepthPolicy Subscription throttling policy
        subscriptionThrottlePolicyDTO = new SubscriptionThrottlePolicyDTO();
        createNewDepthSubscriptionPolicyObject(subscriptionThrottlePolicyDTO);
        response = restAPIAdmin.addSubscriptionThrottlingPolicy(subscriptionThrottlePolicyDTO);
        assertEquals(response.getStatusCode(), HttpStatus.SC_CREATED);
        addedSubPolicy = response.getData();
        subPolicyPolicyId = addedSubPolicy.getPolicyId();
        Assert.assertNotNull(subPolicyPolicyId, "The policy ID cannot be null or empty");

        ArrayList<String> policies = new ArrayList<>();
        policies.add("Unlimited");
        policies.add("QueryComplexPolicy");
        policies.add("QueryDepthPolicy");

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", GRAPHQL_API_NAME);
        additionalPropertiesObj.put("context", GRAPHQL_API_CONTEXT);
        additionalPropertiesObj.put("version", GRAPHQL_API_VERSION);

        JSONObject url = new JSONObject();
        url.put("url", "http://" + webSocketServerHost + ":" + webSocketServerPort);
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("sandbox_endpoints", url);
        endpointConfig.put("production_endpoints", url);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("policies", policies);
        additionalPropertiesObj.put("operations", operations);

        // create Graphql API
        APIDTO apidto = restAPIPublisher.importGraphqlSchemaDefinition(file, additionalPropertiesObj.toString());
        graphqlApiId = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(graphqlApiId);
        System.out.println(createdApiResponse.getData());
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
                GRAPHQL_API_NAME + " API creation is failed");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(graphqlApiId, restAPIPublisher);
        // publish api
        restAPIPublisher.changeAPILifeCycleStatus(graphqlApiId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(user.getUserName(), GRAPHQL_API_NAME, GRAPHQL_API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

        // replace port with inbound endpoint port
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode) || TestUserMode.SUPER_TENANT_USER.equals(userMode)) {
            apiEndPoint = getWebSocketAPIInvocationURL(GRAPHQL_API_CONTEXT, GRAPHQL_API_VERSION);
        } else {
            apiEndPoint = getWebSocketTenantAPIInvocationURL(GRAPHQL_API_CONTEXT, GRAPHQL_API_VERSION, user.getUserDomain());
        }
        log.info("API Endpoint URL" + apiEndPoint);
        APIIdentifier apiIdentifierWebSocket = new APIIdentifier(user.getUserName(), GRAPHQL_API_NAME, GRAPHQL_API_VERSION);
        APIListDTO apiPublisherAllAPIs = restAPIPublisher.getAllAPIs();
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierWebSocket, apiPublisherAllAPIs),
                "Published API is visible in API Publisher.");
        org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO restAPIStoreAllAPIs;
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            restAPIStoreAllAPIs = restAPIStore.getAllAPIs();
        } else {
            restAPIStoreAllAPIs = restAPIStore.getAllAPIs(user.getUserDomain());
        }
        assertTrue(APIMTestCaseUtils.isAPIAvailableInStore(apiIdentifierWebSocket, restAPIStoreAllAPIs),
                "Published API is visible in Dev Portal.");
    }

    @Test(description = "Create JWT Type Application and subscribe",
            dependsOnMethods = "publishGraphQLAPIWithSubscriptions")
    public void testGraphQLAPIJWTApplicationSubscription() throws Exception {

        String applicationName = "GraphQLSubApplication";
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                "", APIMIntegrationConstants.API_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);
        appJWTId = applicationResponse.getData();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(graphqlApiId, appJWTId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        //Validate Subscription of the API
        Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED);
    }

    @Test(groups = {"wso2.am"}, description = "Invoke Subscriptions using token", dependsOnMethods =
            "testGraphQLAPIJWTApplicationSubscription")
    public void testGraphQLAPIInvocationWithJWTToken() throws Exception {

        List grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        applicationKeyDTO = restAPIStore.generateKeys(appJWTId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        WebSocketClient client = new WebSocketClient();
        try {
            Assert.assertNotNull(applicationKeyDTO.getToken());
            invokeGraphQLSubscriptionSuccess(client, applicationKeyDTO.getToken().getAccessToken(), AUTH_IN.HEADER);
            invokeGraphQLSubscriptionSuccess(client, applicationKeyDTO.getToken().getAccessToken(), AUTH_IN.QUERY);
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
            Assert.fail("Client cannot connect to server");
        } finally {
            client.stop();
        }
    }

    @Test(description = "Invoke subscription with invalid payload", dependsOnMethods =
            "testGraphQLAPIInvocationWithJWTToken")
    public void testGraphQLAPIInvocationWithInvalidPayload() throws Exception {

        WebSocketClient client = new WebSocketClient();
        try {
            invokeGraphQLSubscriptionForInvalidPayloadError(client, applicationKeyDTO.getToken().getAccessToken());
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
            Assert.fail("Client cannot connect to server");
        } finally {
            client.stop();
        }
    }

    @Test(groups = {"wso2.am"}, description = "Invoke Subscriptions for complexity",
            dependsOnMethods = "testGraphQLAPIInvocationWithJWTToken")
    public void testGraphQLAPIInvocationForComplexity() throws Exception {

        //Get GraphQL Schema Type List
        GraphQLSchemaTypeListDTO graphQLSchemaTypeList = restAPIPublisher.getGraphQLSchemaTypeList(graphqlApiId);
        HttpResponse response = restAPIPublisher.getGraphQLSchemaTypeListResponse(graphqlApiId);
        assertEquals(Response.Status.OK.getStatusCode(), response.getResponseCode());

        // add GraphQL Complexity Details
        List<GraphQLSchemaTypeDTO> list = graphQLSchemaTypeList.getTypeList();
        List<GraphQLCustomComplexityInfoDTO> complexityList = new ArrayList<>();
        for (GraphQLSchemaTypeDTO graphQLSchemaTypeDTO : list) {
            List<String> fieldList = graphQLSchemaTypeDTO.getFieldList();
            for (String field : fieldList) {
                GraphQLCustomComplexityInfoDTO graphQLCustomComplexityInfoDTO = new GraphQLCustomComplexityInfoDTO();
                graphQLCustomComplexityInfoDTO.setType(graphQLSchemaTypeDTO.getType());
                graphQLCustomComplexityInfoDTO.setField(field);
                graphQLCustomComplexityInfoDTO.setComplexityValue(1);
                log.info(graphQLCustomComplexityInfoDTO);
                complexityList.add(graphQLCustomComplexityInfoDTO);
            }
        }
        GraphQLQueryComplexityInfoDTO graphQLQueryComplexityInfoDTO = new GraphQLQueryComplexityInfoDTO();
        graphQLQueryComplexityInfoDTO.setList(complexityList);
        restAPIPublisher.addGraphQLComplexityDetails(graphQLQueryComplexityInfoDTO, graphqlApiId);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(graphqlApiId, restAPIPublisher);
        Thread.sleep(10000);
        waitForAPIDeploymentSync(user.getUserName(), GRAPHQL_API_NAME, GRAPHQL_API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

        //Get GraphQLComplexity Details
        HttpResponse complexityResponse = restAPIPublisher.getGraphQLComplexityResponse(graphqlApiId);
        assertEquals(Response.Status.OK.getStatusCode(), complexityResponse.getResponseCode());

        //create new JWT Application
        ApplicationDTO applicationDTO = restAPIStore.addApplicationWithTokenType("GraphQLSubComplexApp",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "complexity analysis test-app",
                ApplicationDTO.TokenTypeEnum.JWT.toString());
        complexAppId = applicationDTO.getApplicationId();
        //Subscribe to the API
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(graphqlApiId, complexAppId, "QueryComplexPolicy");
        assertEquals(subscriptionDTO.getThrottlingPolicy(), "QueryComplexPolicy");
        // generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "36000",
                "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        WebSocketClient client = new WebSocketClient();
        try {
            invokeGraphQLSubscriptionSuccess(client, accessToken, AUTH_IN.HEADER);
            invokeGraphQLSubscriptionForComplexityError(client, accessToken);
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
            Assert.fail("Client cannot connect to server");
        } finally {
            client.stop();
        }
    }

    @Test(groups = {
            "wso2.am" }, description = "Invoke Subscriptions for depth", dependsOnMethods = "testGraphQLAPIInvocationForComplexity")
    public void testGraphQLAPIInvocationForDepth() throws Exception {

        //create new JWT Application
        ApplicationDTO applicationDTO = restAPIStore.addApplicationWithTokenType("GraphQLSubDepthApp",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "depth analysis test-app",
                ApplicationDTO.TokenTypeEnum.JWT.toString());
        //Subscribe to the API
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(graphqlApiId, applicationDTO.getApplicationId(),
                "QueryDepthPolicy");
        assertEquals(subscriptionDTO.getThrottlingPolicy(), "QueryDepthPolicy");
        // generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        complexAppId = applicationDTO.getApplicationId();

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(complexAppId, "36000",
                "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        WebSocketClient client = new WebSocketClient();
        try {
            invokeGraphQLSubscriptionForDepthError(client, accessToken);
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
            Assert.fail("Client cannot connect to server");
        } finally {
            client.stop();
        }
    }

    @Test(groups = {
            "wso2.am" }, description = "Invoke Subscriptions using token", dependsOnMethods = "testGraphQLAPIInvocationForDepth")
    public void testGraphQLAPIInvocationWithScopes() throws Exception {

        List role = new ArrayList();
        role.add(GRAPHQL_ROLE);
        ScopeDTO scopeObject = new ScopeDTO();
        scopeObject.setName("subscriber");
        scopeObject.setBindings(role);

        APIScopeDTO apiScopeDTO = new APIScopeDTO();
        apiScopeDTO.setScope(scopeObject);
        List apiScopeList = new ArrayList();
        apiScopeList.add(apiScopeDTO);
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(graphqlApiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(createdApiResponse.getData(), APIDTO.class);
        apidto.setScopes(apiScopeList);
        List scope = new ArrayList();
        scope.add("subscriber");
        List<APIOperationsDTO> operations = apidto.getOperations();
        operations.forEach((item) ->
                {
                    if (item.getTarget().equals("liftStatusChange")) {
                        item.setScopes(scope);
                    }
                }
        );
        apidto.operations(operations);
        restAPIPublisher.updateAPI(apidto, graphqlApiId);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(graphqlApiId, restAPIPublisher);
        // Keep sufficient time to update map
        Thread.sleep(20000);
        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        // generate token
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        // invoke api without authorized scope
        log.info("Access Token response without scope: " + applicationKeyDTO.getToken().getAccessToken());
        WebSocketClient client = new WebSocketClient();
        try {
            invokeGraphQLSubscriptionScopeInvalidError(client, applicationKeyDTO.getToken().getAccessToken());
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
            Assert.fail("Client cannot connect to server");
        } finally {
            client.stop();
        }
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
        String accessToken = accessTokenGenerationResponse.getString("access_token");
        try {
            invokeGraphQLSubscriptionSuccess(client, accessToken, AUTH_IN.HEADER);
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
            Assert.fail("Client cannot connect to server");
        } finally {
            client.stop();
        }

        //Remove scopes
        createdApiResponse = restAPIPublisher.getAPI(graphqlApiId);
        apidto = g.fromJson(createdApiResponse.getData(), APIDTO.class);
        apidto.setScopes(null);
        operations = apidto.getOperations();
        operations.forEach((item) ->
                {
                    if (item.getTarget().equals("liftStatusChange")) {
                        item.setScopes(null);
                    }
                }
        );
        apidto.operations(operations);
        restAPIPublisher.updateAPI(apidto, graphqlApiId);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(graphqlApiId, restAPIPublisher);
        // Keep sufficient time to update map
        Thread.sleep(20000);
        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(groups = {"wso2.am"}, description = "Invoke Subscriptions for throttling", dependsOnMethods = "testGraphQLAPIInvocationWithScopes")
    public void testGraphQLAPISubscriptionThrottling() throws Exception {

        // Deploy Throttling policy with throttle limit set as 4 frames.
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("graphql" + File.separator
                + "subscriptions" + File.separator + "policy.json");

        //Extract the field values from the input stream
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonMap = mapper.readTree(inputStream);
        String policyName = jsonMap.get("policyName").textValue();
        String policyDescription = jsonMap.get("policyDescription").textValue();
        JsonNode defaultLimitJson = jsonMap.get("defaultLimit");
        JsonNode requestCountJson = defaultLimitJson.get("requestCount");
        Long requestCountLimit = Long.valueOf(String.valueOf(requestCountJson.get("requestCount")));
        String timeUnit = requestCountJson.get("timeUnit").textValue();
        Integer unitTime = Integer.valueOf(String.valueOf(requestCountJson.get("unitTime")));

        //Create the advanced throttling policy with request count quota type
        RequestCountLimitDTO requestCountLimitDTO = DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime,
                requestCountLimit);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, requestCountLimitDTO,
                        null);
        AdvancedThrottlePolicyDTO advancedThrottlePolicyDTO = DtoFactory
                .createAdvancedThrottlePolicyDTO(policyName, "", policyDescription, false, defaultLimit,
                        new ArrayList<>());

        //Add the advanced throttling policy
        ApiResponse<AdvancedThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(advancedThrottlePolicyDTO);
        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), org.apache.http.HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedAdvancedPolicyDTO = addedPolicy.getData();
        String apiPolicyId = addedAdvancedPolicyDTO.getPolicyId();
        Assert.assertNotNull(apiPolicyId, "The policy ID cannot be null or empty");

        //Update Throttling policy of the API
        HttpResponse response = restAPIPublisher.getAPI(graphqlApiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        apidto.setApiThrottlingPolicy("GraphQLSubThrottlingPolicy");
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto);
        createAPIRevisionAndDeployUsingRest(updatedAPI.getId(), restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), "GraphQLSubThrottlingPolicy");
        //create new JWT Application
        ApplicationDTO applicationDTO = restAPIStore.addApplicationWithTokenType("GraphQLThrottleApp",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "advanced throttle test-app",
                ApplicationDTO.TokenTypeEnum.JWT.toString());
        throttleAppId = applicationDTO.getApplicationId();
        //Subscribe to the API
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(graphqlApiId, throttleAppId,
                "Unlimited");
        assertEquals(subscriptionDTO.getThrottlingPolicy(), "Unlimited");
        // generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "36000",
                "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        testThrottling(accessToken);
    }

    /**
     * Invoke deployed API via GraphQL Websocket client and wait for success reply (happy path)
     *
     * @param client      WebSocketClient object
     * @param accessToken API access Token
     * @param in          location of the Auth header. {@code query} or {@code header}
     * @throws Exception If an error occurs while invoking WebSocket API
     */
    private void invokeGraphQLSubscriptionSuccess(WebSocketClient client, String accessToken, AUTH_IN in)
            throws Exception {

        SubscriptionWSClientImpl socket = new SubscriptionWSClientImpl();
        client.start();
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        URI echoUri = null;

        request.setSubProtocols("graphql-ws");
        if (AUTH_IN.HEADER == in) {
            request.setHeader("Authorization", "Bearer " + accessToken);
            echoUri = new URI(apiEndPoint);
        } else if (AUTH_IN.QUERY == in) {
            echoUri = new URI(apiEndPoint + "?access_token=" + accessToken);
        }

        client.connect(socket, echoUri, request);
        if (socket.getLatch().await(30, TimeUnit.SECONDS)) {
            String textMessage;
            //Send connection init message
            textMessage = "{\"type\":\"connection_init\",\"payload\":{}}";
            Thread.sleep(20000);
            socket.sendMessage(textMessage);
            waitForReply(socket);
            Thread.sleep(40000);
            assertFalse(StringUtils.isEmpty(socket.getResponseMessage()),
                    "Client did not receive response from server");
            assertEquals(socket.getResponseMessage(), "{\"type\":\"connection_ack\"}",
                    "Received response in not a Connection Ack response");
            socket.setResponseMessage(null);
            //Send graphQL subscription request message
            textMessage = "{\"id\":\"1\",\"type\":\"start\",\"payload\":{\"variables\":{},\"extensions\":{},"
                    + "\"operationName\":null,\"query\":"
                    + "\"subscription {\\n  liftStatusChange {\\n    name\\n  }\\n}\\n\"}}";
            socket.sendMessage(textMessage);
            waitForReply(socket);
            assertFalse(StringUtils.isEmpty(socket.getResponseMessage()),
                    "Client did not receive response from server");
            assertEquals(socket.getResponseMessage(), "{\"type\":\"data\",\"id\":\"1\",\"payload\":{\"data\":"
                            + "{\"liftStatusChange\":{\"name\":\"Astra Express\"}}}}",
                    "Received response in not a lift status change sub topic event response");
            socket.setResponseMessage(null);
        } else {
            throw new APIManagerIntegrationTestException("Unable to create client connection");
        }
    }

    /**
     * Starts backend graphQL web socket server in given port
     *
     * @param serverPort Port that WebSocket Server starts
     */
    private void startGraphQLSubscriptionServer(final int serverPort) {

            WebSocketHandler wsHandler = new WebSocketHandler() {
                @Override
                public void configure(WebSocketServletFactory factory) {

                    factory.setCreator(new SubscriptionServerCreator());
                }
            };

            server = new Server(serverPort);
            server.setHandler(wsHandler);
            try {
                server.start();
                log.info("GraphQL WebSocket backend server started at port: " + serverPort);
            } catch (InterruptedException ignore) {
            } catch (Exception e) {
                log.error("Error while starting graphql backend server at port: " + serverPort, e);
                Assert.fail("Cannot start GraphQL WebSocket server");
            }
    }

    /**
     * Find a free port to start backend WebSocket server in given port range
     *
     * @param lowerPortLimit from port number
     * @param upperPortLimit to port number
     * @return Available Port Number
     */
    protected int getAvailablePort(int lowerPortLimit, int upperPortLimit) {

        while (lowerPortLimit < upperPortLimit) {
            if (isPortFree(lowerPortLimit)) {
                return lowerPortLimit;
            }
            lowerPortLimit += 1;
        }
        return -1;
    }

    /**
     * Check whether give port is available
     *
     * @param port Port Number
     * @return status
     */
    private boolean isPortFree(int port) {

        Socket s = null;
        try {
            s = new Socket("localhost", port);
            // something is using the port and has responded.
            return false;
        } catch (IOException e) {
            //port available
            return true;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to close connection ", e);
                }
            }
        }
    }

    private File getTempFileWithContent(String schema) throws Exception {
        File temp = File.createTempFile("schema", ".graphql");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(schema);
        out.close();
        return temp;
    }

    /**
     * Wait for client to receive reply from the server
     *
     * @param clientSocket WebSocket Client Object
     */
    private void waitForReply(SubscriptionWSClientImpl clientSocket) {

        long currentTime = System.currentTimeMillis();
        long WAIT_TIME = 30 * 1000;
        long waitTime = currentTime + WAIT_TIME;
        while (StringUtils.isEmpty(clientSocket.getResponseMessage()) && waitTime > System.currentTimeMillis()) {
            try {
                log.info("Waiting for reply from server:");
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
        log.info("Client received :" + clientSocket.getResponseMessage());
    }

    private void
    createNewComplexSubscriptionPolicyObject(SubscriptionThrottlePolicyDTO subscriptionThrottlePolicyDTO) {

        subscriptionThrottlePolicyDTO.setPolicyName("QueryComplexPolicy");
        subscriptionThrottlePolicyDTO.setDisplayName("QueryComplexPolicy");
        subscriptionThrottlePolicyDTO.setDescription("Policy to test query complexity");
        subscriptionThrottlePolicyDTO.setRateLimitCount(100);
        subscriptionThrottlePolicyDTO.setRateLimitTimeUnit("min");
        subscriptionThrottlePolicyDTO.setBillingPlan("COMMERCIAL");
        subscriptionThrottlePolicyDTO.setStopOnQuotaReach(true);
        subscriptionThrottlePolicyDTO.setIsDeployed(true);
        subscriptionThrottlePolicyDTO.setGraphQLMaxComplexity(3);
        subscriptionThrottlePolicyDTO.setGraphQLMaxDepth(2);
        subscriptionThrottlePolicyDTO.setSubscriberCount(0);

        ThrottleLimitDTO throttleLimitDTO = new ThrottleLimitDTO();
        throttleLimitDTO.setType(ThrottleLimitDTO.TypeEnum.valueOf("REQUESTCOUNTLIMIT"));
        RequestCountLimitDTO requestCountLimitDTO = new RequestCountLimitDTO();
        requestCountLimitDTO.setRequestCount(1000L);
        requestCountLimitDTO.setTimeUnit("min");
        requestCountLimitDTO.setUnitTime(10);
        throttleLimitDTO.setRequestCount(requestCountLimitDTO);
        subscriptionThrottlePolicyDTO.setDefaultLimit(throttleLimitDTO);
    }

    private void createNewDepthSubscriptionPolicyObject(SubscriptionThrottlePolicyDTO subscriptionThrottlePolicyDTO) {

        subscriptionThrottlePolicyDTO.setPolicyName("QueryDepthPolicy");
        subscriptionThrottlePolicyDTO.setDisplayName("QueryDepthPolicy");
        subscriptionThrottlePolicyDTO.setDescription("Policy to test query depth");
        subscriptionThrottlePolicyDTO.setRateLimitCount(100);
        subscriptionThrottlePolicyDTO.setRateLimitTimeUnit("min");
        subscriptionThrottlePolicyDTO.setBillingPlan("COMMERCIAL");
        subscriptionThrottlePolicyDTO.setStopOnQuotaReach(true);
        subscriptionThrottlePolicyDTO.setIsDeployed(true);
        subscriptionThrottlePolicyDTO.setGraphQLMaxComplexity(3);
        subscriptionThrottlePolicyDTO.setGraphQLMaxDepth(1);
        subscriptionThrottlePolicyDTO.setSubscriberCount(0);

        ThrottleLimitDTO throttleLimitDTO = new ThrottleLimitDTO();
        throttleLimitDTO.setType(ThrottleLimitDTO.TypeEnum.valueOf("REQUESTCOUNTLIMIT"));
        RequestCountLimitDTO requestCountLimitDTO = new RequestCountLimitDTO();
        requestCountLimitDTO.setRequestCount(1000L);
        requestCountLimitDTO.setTimeUnit("min");
        requestCountLimitDTO.setUnitTime(10);
        throttleLimitDTO.setRequestCount(requestCountLimitDTO);
        subscriptionThrottlePolicyDTO.setDefaultLimit(throttleLimitDTO);
    }

    private void invokeGraphQLSubscriptionScopeInvalidError(WebSocketClient client, String accessToken) throws
            Exception {

        SubscriptionWSClientImpl socket = new SubscriptionWSClientImpl();
        client.start();
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        URI echoUri = new URI(apiEndPoint);
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setSubProtocols("graphql-ws");

        client.connect(socket, echoUri, request);
        if (socket.getLatch().await(30, TimeUnit.SECONDS)) {
            String textMessage;
            //Send connection init message
            textMessage = "{\"type\":\"connection_init\",\"payload\":{}}";
            Thread.sleep(20000);
            socket.sendMessage(textMessage);
            Thread.sleep(20000);
            waitForReply(socket);
            Thread.sleep(20000);
            assertFalse(StringUtils.isEmpty(socket.getResponseMessage()),
                    "Client did not receive response from server");
            assertEquals(socket.getResponseMessage(), "{\"type\":\"connection_ack\"}",
                    "Received response in not a Connection Ack response");
            socket.setResponseMessage(null);
            //Send graphQL subscription request message
            textMessage = "{\"id\":\"1\",\"type\":\"start\",\"payload\":{\"variables\":{},\"extensions\":{},"
                    + "\"operationName\":null,\"query\":"
                    + "\"subscription {\\n  liftStatusChange {\\n    name\\n  }\\n}\\n\"}}";
            socket.sendMessage(textMessage);
            waitForReply(socket);
            assertFalse(StringUtils.isEmpty(socket.getResponseMessage()),
                    "Client did not receive response from server");
            String errorMessage = socket.getResponseMessage();
            assertNotNull(errorMessage);
            JSONParser jsonParser = new JSONParser();
            org.json.simple.JSONObject errorJson = (org.json.simple.JSONObject) jsonParser.parse(errorMessage);
            assertTrue(errorJson.containsKey("type"));
            assertEquals(errorJson.get("type"), "error");
            assertTrue(errorJson.containsKey("id"));
            assertEquals(errorJson.get("id"), "1");
            assertTrue(errorJson.containsKey("payload"));
            org.json.simple.JSONObject payload =
                    (org.json.simple.JSONObject) ((org.json.simple.JSONArray) errorJson.get("payload")).get(0);
            assertTrue(payload.containsKey("message"));
            assertTrue(payload.containsKey("code"));
            assertEquals(payload.get("message"), "User is NOT authorized to access the Resource: liftStatusChange. "
                            + "Scope validation failed.",
                    "Received response in a invalid error message");
            assertEquals(payload.get("code"), 4002L, "Received response code is a invalid response code");
        } else {
            throw new APIManagerIntegrationTestException("Unable to create client connection");
        }
    }

    private void invokeGraphQLSubscriptionForComplexityError(WebSocketClient client, String accessToken) throws
            Exception {

        SubscriptionWSClientImpl socket = new SubscriptionWSClientImpl();
        client.start();
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        URI echoUri = new URI(apiEndPoint);
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setSubProtocols("graphql-ws");

        client.connect(socket, echoUri, request);
        if (socket.getLatch().await(30, TimeUnit.SECONDS)) {
            String textMessage;
            //Send connection init message
            textMessage = "{\"type\":\"connection_init\",\"payload\":{}}";
            Thread.sleep(20000);
            socket.sendMessage(textMessage);
            Thread.sleep(20000);
            waitForReply(socket);
            Thread.sleep(20000);
            assertFalse(StringUtils.isEmpty(socket.getResponseMessage()),
                    "Client did not receive response from server");
            assertEquals(socket.getResponseMessage(), "{\"type\":\"connection_ack\"}",
                    "Received response in not a Connection Ack response");
            socket.setResponseMessage(null);
            //Send graphQL subscription request message
            textMessage = "{\"id\":\"1\",\"type\":\"start\",\"payload\":{\"variables\":{},\"extensions\":{},"
                    + "\"operationName\":null,\"query\": \"subscription {\\n  "
                    + "liftStatusChange {\\n name\\n id\\n status\\n night\\n capacity\\n }\\n}\\n\"}}";
            socket.sendMessage(textMessage);
            waitForReply(socket);
            assertFalse(StringUtils.isEmpty(socket.getResponseMessage()),
                    "Client did not receive response from server");
            String errorMessage = socket.getResponseMessage();
            assertNotNull(errorMessage);
            JSONParser jsonParser = new JSONParser();
            org.json.simple.JSONObject errorJson = (org.json.simple.JSONObject) jsonParser.parse(errorMessage);
            assertTrue(errorJson.containsKey("type"));
            assertEquals(errorJson.get("type"), "error");
            assertTrue(errorJson.containsKey("id"));
            assertEquals(errorJson.get("id"), "1");
            assertTrue(errorJson.containsKey("payload"));
            org.json.simple.JSONObject payload =
                    (org.json.simple.JSONObject) ((org.json.simple.JSONArray) errorJson.get("payload")).get(0);
            assertTrue(payload.containsKey("message"));
            assertTrue(payload.containsKey("code"));
            assertTrue(((String) payload.get("message")).contains("QUERY TOO COMPLEX"),
                    "Invalid query too complex error");
            assertTrue(((String) payload.get("message")).contains("maximum query complexity exceeded"),
                    "Invalid query too complex error");
            assertEquals(payload.get("code"), 4021L, "Received response code is a invalid response code");
        } else {
            throw new APIManagerIntegrationTestException("Unable to create client connection");
        }
    }

    private void invokeGraphQLSubscriptionForDepthError(WebSocketClient client, String accessToken)
            throws Exception {

        SubscriptionWSClientImpl socket = new SubscriptionWSClientImpl();
        client.start();
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        URI echoUri = new URI(apiEndPoint);
        request.setHeader("Authorization", "Bearer " + accessToken);

        request.setSubProtocols("graphql-ws");
        client.connect(socket, echoUri, request);
        if (socket.getLatch().await(30, TimeUnit.SECONDS)) {
            String textMessage;
            //Send connection init message
            textMessage = "{\"type\":\"connection_init\",\"payload\":{}}";
            Thread.sleep(40000);
            socket.sendMessage(textMessage);
            Thread.sleep(30000);
            waitForReply(socket);
            Thread.sleep(30000);
            assertFalse(StringUtils.isEmpty(socket.getResponseMessage()),
                    "Client did not receive response from server");
            assertEquals(socket.getResponseMessage(), "{\"type\":\"connection_ack\"}",
                    "Received response in not a Connection Ack response");
            socket.setResponseMessage(null);
            //Send graphQL subscription request message
            textMessage = "{\"id\":\"1\",\"type\":\"start\",\"payload\":{\"variables\":{},\"extensions\":{},"
                    + "\"operationName\":null,\"query\": \"subscription {\\n  "
                    + "liftStatusChange {\\n name\\n}\\n}\\n\"}}";
            socket.sendMessage(textMessage);
            waitForReply(socket);
            assertFalse(StringUtils.isEmpty(socket.getResponseMessage()),
                    "Client did not receive response from server");
            String errorMessage = socket.getResponseMessage();
            assertNotNull(errorMessage);
            JSONParser jsonParser = new JSONParser();
            org.json.simple.JSONObject errorJson = (org.json.simple.JSONObject) jsonParser.parse(errorMessage);
            assertTrue(errorJson.containsKey("type"));
            assertEquals(errorJson.get("type"), "error");
            assertTrue(errorJson.containsKey("id"));
            assertEquals(errorJson.get("id"), "1");
            assertTrue(errorJson.containsKey("payload"));
            org.json.simple.JSONObject payload =
                    (org.json.simple.JSONObject) ((org.json.simple.JSONArray) errorJson.get("payload")).get(0);
            assertTrue(payload.containsKey("message"));
            assertTrue(payload.containsKey("code"));
            assertTrue(((String) payload.get("message")).contains("QUERY TOO DEEP"),
                    "Invalid query too deep error");
            assertTrue(((String) payload.get("message")).contains("maximum query depth exceeded 2 > 1"),
                    "Invalid query too deep error message");
            assertEquals(payload.get("code"), 4020L, "Received response code is a invalid response code");
        } else {
            throw new APIManagerIntegrationTestException("Unable to create client connection");
        }
    }

    private void invokeGraphQLSubscriptionForInvalidPayloadError(WebSocketClient client, String accessToken) throws
            Exception {

        SubscriptionWSClientImpl socket = new SubscriptionWSClientImpl();
        client.start();
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        URI echoUri = new URI(apiEndPoint);
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setSubProtocols("graphql-ws");

        client.connect(socket, echoUri, request);
        if (socket.getLatch().await(30, TimeUnit.SECONDS)) {
            String textMessage;
            //Send connection init message
            textMessage = "{\"type\":\"connection_init\",\"payload\":{}}";
            Thread.sleep(40000);
            socket.sendMessage(textMessage);
            Thread.sleep(30000);
            waitForReply(socket);
            Thread.sleep(30000);
            assertFalse(StringUtils.isEmpty(socket.getResponseMessage()),
                    "Client did not receive response from server");
            assertEquals(socket.getResponseMessage(), "{\"type\":\"connection_ack\"}",
                    "Received response in not a Connection Ack response");
            socket.setResponseMessage(null);
            //Send graphQL subscription request message
            textMessage = "{\"id\":\"1\",\"type\":\"start\",\"payload\":{\"variables\":{},\"extensions\":{},"
                    + "\"operationName\":null,\"query\": \"subscription {\\n  "
                    + "liftStatusChange {\\n name\\n invalidField\\n }\\n}\\n\"}}";
            socket.sendMessage(textMessage);
            waitForReply(socket);
            assertFalse(StringUtils.isEmpty(socket.getResponseMessage()),
                    "Client did not receive response from server");
            String errorMessage = socket.getResponseMessage();
            assertNotNull(errorMessage);
            JSONParser jsonParser = new JSONParser();
            org.json.simple.JSONObject errorJson = (org.json.simple.JSONObject) jsonParser.parse(errorMessage);
            assertTrue(errorJson.containsKey("type"));
            assertEquals(errorJson.get("type"), "error");
            assertTrue(errorJson.containsKey("id"));
            assertEquals(errorJson.get("id"), "1");
            assertTrue(errorJson.containsKey("payload"));
            org.json.simple.JSONObject payload =
                    (org.json.simple.JSONObject) ((org.json.simple.JSONArray) errorJson.get("payload")).get(0);
            assertTrue(payload.containsKey("message"));
            assertTrue(payload.containsKey("code"));
            assertTrue(((String) payload.get("message")).contains("INVALID QUERY"),
                    "Invalid query payload error not received");
            assertEquals(payload.get("code"), 4022L, "Received response code is a invalid response code");
        } else {
            throw new APIManagerIntegrationTestException("Unable to create client connection");
        }
    }

    private void testThrottling(String accessToken) throws Exception {

        waitUntilClockMinute();
        int startingDistinctUnitTime = LocalDateTime.now().getMinute();
        log.info("Starting throttling test at: " + LocalDateTime.now());
        int limit = 4;
        WebSocketClient client = new WebSocketClient();
        SubscriptionWSClientImpl socket = new SubscriptionWSClientImpl();
        client.start();
        URI echoUri = new URI(apiEndPoint);
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setSubProtocols("graphql-ws");
        client.connect(socket, echoUri, request);
        socket.getLatch().await(3L, TimeUnit.SECONDS);
        try {
            String textMessage;
            //Send connection init message
            textMessage = "{\"type\":\"connection_init\",\"payload\":{}}";
            Thread.sleep(10000);
            socket.sendMessage(textMessage);
            waitForReply(socket);
            assertFalse(StringUtils.isEmpty(socket.getResponseMessage()),
                    "Client did not receive response from server");
            assertEquals(socket.getResponseMessage(), "{\"type\":\"connection_ack\"}",
                    "Received response in not a Connection Ack response");
            socket.setResponseMessage(null);
            for (int count = 1; count <= limit; count++) {
                if (count == limit) {
                    Thread.sleep(3000);
                }
                if (count == 1) {
                    //Send initial graphQL subscription request message
                    textMessage = "{\"id\":\"2\",\"type\":\"start\",\"payload\":{\"variables\":{},\"extensions\":{},"
                            + "\"operationName\":null,\"query\": \"subscription {\\n  "
                            + "liftStatusChange {\\n name\\n }\\n}\\n\"}}";
                    socket.sendMessage(textMessage);
                }
                waitForReply(socket);
                String responseMessage = socket.getResponseMessage();
                log.info("Count :" + count + " Message :" + responseMessage + " At: " + LocalDateTime.now());
                // At the 3rd message check frame is throttled out.
                if (count == limit) {
                    log.info("Current minute: " + LocalDateTime.now().getMinute() + " Started minute: " + startingDistinctUnitTime);
                    //If throttling testing time duration is dispersed into two separate unit times, repeat the test
                    if (LocalDateTime.now().getMinute() != startingDistinctUnitTime) {
                        //repeat the test
                        log.info("Repeating the test as throttling testing time duration is dispersed into two " +
                                "separate units of time");
                        testThrottling(accessToken);
                    }
                    assertNotNull(responseMessage);
                    JSONParser jsonParser = new JSONParser();
                    org.json.simple.JSONObject errorJson =
                            (org.json.simple.JSONObject) jsonParser.parse(responseMessage);
                    assertTrue(errorJson.containsKey("type"));
                    assertEquals(errorJson.get("type"), "error");
                    assertTrue(errorJson.containsKey("id"));
                    assertEquals(errorJson.get("id"), "2");
                    assertTrue(errorJson.containsKey("payload"));
                    org.json.simple.JSONObject payload =
                            (org.json.simple.JSONObject) ((org.json.simple.JSONArray) errorJson.get("payload")).get(0);
                    assertTrue(payload.containsKey("message"));
                    assertTrue(payload.containsKey("code"));
                    assertTrue(((String) payload.get("message")).contains("Websocket frame throttled out"),
                            "Received response is not matching");
                    assertEquals(payload.get("code"), 4003L, "Received response code is a invalid response code");
                }
                socket.setResponseMessage(null);
            }
        } catch (Exception ex) {
            log.error("Error occurred while calling API.", ex);
            Assert.fail("Client cannot connect to server");
        } finally {
            client.stop();
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (server != null) {
            server.stop();
        }
        serverConfigurationManager.restoreToLastConfiguration(false);
        userManagementClient.deleteRole(GRAPHQL_ROLE);
        userManagementClient.deleteUser(GRAPHQL_TEST_USER);
        restAPIStore.deleteApplication(appJWTId);
        restAPIStore.deleteApplication(complexAppId);
        restAPIStore.deleteApplication(depthAppId);
        restAPIStore.deleteApplication(throttleAppId);
        undeployAndDeleteAPIRevisionsUsingRest(graphqlApiId, restAPIPublisher);
        super.cleanUp();
    }
}
