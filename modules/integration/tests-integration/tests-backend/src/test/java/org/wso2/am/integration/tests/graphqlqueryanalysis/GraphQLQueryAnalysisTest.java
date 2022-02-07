package org.wso2.am.integration.tests.graphqlqueryanalysis;
import java.io.BufferedWriter;
import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.*;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.*;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.clients.AdminDashboardRestClient;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import javax.ws.rs.core.Response;
import java.io.FileWriter;
import java.util.*;

import static org.testng.Assert.assertEquals;

public class GraphQLQueryAnalysisTest extends APIMIntegrationBaseTest {

    private  AdminDashboardRestClient adminDashboardRestClient;
    private RestAPIAdminImpl restAPIAdminUser;
    private final String GRAPHQL_API_NAME = "CountriesGraphqlAPIQueryAnalysis";
    private final String API_CONTEXT = "infoS";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String END_POINT_URL = "https://localhost:9943/am-graphQL-sample/api/graphql/";
    private String schemaDefinition;
    private String graphqlApiId;
    private String tokenTestApiAppId;
    private String oauthTokenTestApiId;

    @Factory(dataProvider = "userModeDataProvider")
    public GraphQLQueryAnalysisTest(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN}};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        adminDashboardRestClient = new AdminDashboardRestClient(getPublisherURLHttps());
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());

        // add new Subscription throttling policy
        SubscriptionThrottlePolicyDTO subscriptionThrottlePolicyDTO = new SubscriptionThrottlePolicyDTO();
        createNewSubscriptionPolicyObject(subscriptionThrottlePolicyDTO);
        restAPIAdminUser = new RestAPIAdminImpl("admin", "admin", "carbon.super",
                adminURLHttps);
        ApiResponse<SubscriptionThrottlePolicyDTO>
                response = restAPIAdminUser.addSubscriptionThrottlingPolicy(subscriptionThrottlePolicyDTO);
        assertEquals(response.getStatusCode(), HttpStatus.SC_CREATED);

        //create  and publish GraphQL API
        schemaDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("graphql" + File.separator + "schema.graphql"),
                "UTF-8");
        File file = getTempFileWithContent(schemaDefinition);
        GraphQLValidationResponseDTO responseApiDto = restAPIPublisher.validateGraphqlSchemaDefinition(file);
        GraphQLValidationResponseGraphQLInfoDTO graphQLInfo = responseApiDto.getGraphQLInfo();
        String arrayToJson = new ObjectMapper().writeValueAsString(graphQLInfo.getOperations());
        JSONArray operations = new JSONArray(arrayToJson);

        ArrayList<String> environment = new ArrayList<String>();
        environment.add(Constants.GATEWAY_ENVIRONMENT);

        ArrayList<String> policies = new ArrayList<String>();
        policies.add("Platinum");

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", GRAPHQL_API_NAME);
        additionalPropertiesObj.put("context", API_CONTEXT);
        additionalPropertiesObj.put("version", API_VERSION_1_0_0);

        JSONObject url = new JSONObject();
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
        graphqlApiId = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(graphqlApiId);
        System.out.println(createdApiResponse.getData());
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
                GRAPHQL_API_NAME + " API creation is failed");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(graphqlApiId, restAPIPublisher);
        // publish api
        restAPIPublisher.changeAPILifeCycleStatus(graphqlApiId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(user.getUserName(), GRAPHQL_API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(groups = {"wso2.am"}, description = "Add GraphQL Complexity Values in Publisher Portal")
    public void testAddGraphQLComplexity() throws Exception {
        //Get GraphQL Schema Type List
        GraphQLSchemaTypeListDTO graphQLSchemaTypeList = restAPIPublisher.getGraphQLSchemaTypeList(graphqlApiId);
        HttpResponse response = restAPIPublisher.getGraphQLSchemaTypeListResponse(graphqlApiId);
        assertEquals(Response.Status.OK.getStatusCode(), response.getResponseCode());

        // add GraphQL Complexity Details
        List<GraphQLSchemaTypeDTO> list = graphQLSchemaTypeList.getTypeList();
        System.out.println(list);
        List<GraphQLCustomComplexityInfoDTO> complexityList = new ArrayList<GraphQLCustomComplexityInfoDTO>();;
        for (GraphQLSchemaTypeDTO graphQLSchemaTypeDTO : list) {
            List<String> fieldList = graphQLSchemaTypeDTO.getFieldList();
            for(String field : fieldList) {
                GraphQLCustomComplexityInfoDTO graphQLCustomComplexityInfoDTO = new GraphQLCustomComplexityInfoDTO();
                graphQLCustomComplexityInfoDTO.setType(graphQLSchemaTypeDTO.getType());
                graphQLCustomComplexityInfoDTO.setField(field);
                graphQLCustomComplexityInfoDTO.setComplexityValue(1);
                System.out.println(graphQLCustomComplexityInfoDTO);
                complexityList.add(graphQLCustomComplexityInfoDTO);
            }
        }
        GraphQLQueryComplexityInfoDTO graphQLQueryComplexityInfoDTO = new GraphQLQueryComplexityInfoDTO();
        graphQLQueryComplexityInfoDTO.setList(complexityList);
        restAPIPublisher.addGraphQLComplexityDetails(graphQLQueryComplexityInfoDTO,graphqlApiId);

        //Get GraphQLComplexity Details
        HttpResponse complexityResponse = restAPIPublisher.getGraphQLComplexityResponse(graphqlApiId);
        assertEquals(Response.Status.OK.getStatusCode(), complexityResponse.getResponseCode());
    }

    @Test(groups = {"wso2.am"}, description = "View GraphQL Complexity Values in Developer Portal")
    public void testRetrieveGraphQLComplexity() throws Exception {
        //Get GraphQLComplexity Details
        HttpResponse complexityResponse = restAPIStore.getGraphQLComplexityResponse(graphqlApiId);
        assertEquals(Response.Status.OK.getStatusCode(), complexityResponse.getResponseCode());

        //Get GraphQL Schema Type List
        HttpResponse response = restAPIStore.getGraphQLSchemaTypeListResponse(graphqlApiId);
        assertEquals(Response.Status.OK.getStatusCode(), response.getResponseCode());
    }

    @Test(groups = {"wso2.am"}, description = "API invocation using JWT App")
    public void testInvokeGraphqlAPIUsingJWTApplication() throws Exception {
        String graphqlOAUTHAppName = "CountriesJWTAPPForQueryAnalysis";

        //create new JWT Application
        ApplicationDTO applicationDTO = restAPIStore.addApplicationWithTokenType(graphqlOAUTHAppName,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "test-app for JWT",
                "JWT");
        tokenTestApiAppId = applicationDTO.getApplicationId();

        //Subscribe to the API
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(graphqlApiId, applicationDTO.getApplicationId(),
                "Platinum");
        Assert.assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals("Platinum"));

        // generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "36000",
                "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/";
        Map<String, String> requestHeaders = new HashMap<String, String>();

        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type",  "application/json");

        JSONObject queryObject = new JSONObject();
        queryObject.put("query", "{languages{code name native rtl}}");

        //Maximum query complexity exceed, max_query_complexity = 4 < query complexity = 5
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject.toString());
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_BAD_REQUEST);

        JSONObject queryObject2 = new JSONObject();
        queryObject2.put("query", "{countries{code name languages{code name}}}");

        //Maximum query depth exceed, max_query_depth = 2 < query_depth = 3
        HttpResponse serviceResponse2 = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject2.toString());
        Assert.assertEquals(serviceResponse2.getResponseCode(), HttpStatus.SC_BAD_REQUEST);

        JSONObject queryObject3 = new JSONObject();
        queryObject3.put("query", "{limitLanguage(limit:1000, where:{eq:100})}");

        //Maximum query complexity exceed, max_query_complexity = 4 < query complexity = 1001 with a limit
        //Test for https://github.com/wso2/product-apim/issues/11773
        HttpResponse serviceResponse3 = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject3.toString());
        Assert.assertEquals(serviceResponse3.getResponseCode(), HttpStatus.SC_BAD_REQUEST);
    }

    @Test(groups = {"wso2.am"}, description = "API invocation using oauth App")
    public void testInvokeGraphqlAPIUsingOAuthApplication() throws Exception {
        String graphqlOAUTHAppName = "CountriesOauthAPPForQueryAnalysis";

        //create new OAUTH Application
        ApplicationDTO applicationDTO = restAPIStore.addApplicationWithTokenType(graphqlOAUTHAppName,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "test-app for OAUTH",
                "OAUTH");
        oauthTokenTestApiId = applicationDTO.getApplicationId();

        //Subscribe to the API
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(graphqlApiId, applicationDTO.getApplicationId(),
                "Platinum");
        Assert.assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals("Platinum"));

        // generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "36000",
                "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        String tokenJti = TokenUtils.getJtiOfJwtToken(accessToken);

        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/";
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + tokenJti);
        requestHeaders.put("Content-Type",  "application/json");

        JSONObject queryObject = new JSONObject();
        queryObject.put("query", "{languages{code name native rtl}}");

        //Maximum query complexity exceed, max_query_complexity = 4 < query complexity = 5
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject.toString());
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_BAD_REQUEST);

        JSONObject queryObject2 = new JSONObject();
        queryObject2.put("query", "{countries{code name languages{code name}}}");

        //Maximum query depth exceed, max_query_depth = 2 < query_depth = 3
        HttpResponse serviceResponse2 = HTTPSClientUtils.doPost(invokeURL, requestHeaders, queryObject2.toString());
        Assert.assertEquals(serviceResponse2.getResponseCode(), HttpStatus.SC_BAD_REQUEST);
    }

    public SubscriptionThrottlePolicyDTO createNewSubscriptionPolicyObject(SubscriptionThrottlePolicyDTO subscriptionThrottlePolicyDTO){
        subscriptionThrottlePolicyDTO.setPolicyId("0c6439fd-9b16-3c2e-be6e-1086e0b9aa92");
        subscriptionThrottlePolicyDTO.setPolicyName("Platinum");
        subscriptionThrottlePolicyDTO.setDisplayName("Platinum");
        subscriptionThrottlePolicyDTO.setDescription("Platinum");
        subscriptionThrottlePolicyDTO.setRateLimitCount(1000);
        subscriptionThrottlePolicyDTO.setRateLimitTimeUnit("min");
        subscriptionThrottlePolicyDTO.setBillingPlan("COMMERCIAL");
        subscriptionThrottlePolicyDTO.setStopOnQuotaReach(true);
        subscriptionThrottlePolicyDTO.setIsDeployed(true);
        subscriptionThrottlePolicyDTO.setGraphQLMaxComplexity(4);
        subscriptionThrottlePolicyDTO.setGraphQLMaxDepth(2);
        subscriptionThrottlePolicyDTO.setSubscriberCount(0);

        ThrottleLimitDTO throttleLimitDTO = new ThrottleLimitDTO();
        throttleLimitDTO.setType(ThrottleLimitDTO.TypeEnum.valueOf("REQUESTCOUNTLIMIT"));
        RequestCountLimitDTO requestCountLimitDTO = new RequestCountLimitDTO();
        requestCountLimitDTO.setRequestCount(Long.valueOf(1000));
        requestCountLimitDTO.setTimeUnit("min");
        requestCountLimitDTO.setUnitTime(10);
        throttleLimitDTO.setRequestCount(requestCountLimitDTO);

        subscriptionThrottlePolicyDTO.setDefaultLimit(throttleLimitDTO);
        return subscriptionThrottlePolicyDTO;
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
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(oauthTokenTestApiId);
        restAPIStore.deleteApplication(tokenTestApiAppId);
        undeployAndDeleteAPIRevisionsUsingRest(graphqlApiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(graphqlApiId);
        super.cleanUp();
    }
}