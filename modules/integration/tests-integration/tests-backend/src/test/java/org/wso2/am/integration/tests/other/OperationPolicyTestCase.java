package org.wso2.am.integration.tests.other;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.*;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OperationPolicyTestCase extends APIMIntegrationBaseTest {
    private static final String API_CONTEXT = "operation-policies";
    private static final String API_VERSION = "1.0.0";
    private static final String END_POINT_URL = "https://localhost:9943/am-echo-service/api";
    private static final String IN_FLOW = "IN";
    private static final String OUT_FLOW = "OUT";
    private static final String RESOURCE_PATH = "/echo-request";

    private String apiId;
    private String revisionId;
    private String accessToken;

    @Factory(dataProvider = "userModeDataProvider")
    public OperationPolicyTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] {
                new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        //Add API
        String apiName = "OperationPolicyTestAPI";
        String description = "This is operation policy test API created by API manager integration test";
        String providerName = user.getUserName();

        APIRequest apiRequest = new APIRequest(apiName, API_CONTEXT, new URL(END_POINT_URL));
        apiRequest.setDescription(description);
        apiRequest.setVersion(API_VERSION);
        apiRequest.setProvider(providerName);
        addOperation(apiRequest, RESOURCE_PATH, APIMIntegrationConstants.HTTP_VERB_GET, null);

        HttpResponse addAPIResponse = restAPIPublisher.addAPI(apiRequest);
        Assert.assertEquals(addAPIResponse.getResponseCode(), HttpStatus.SC_CREATED,
                "Error while adding " + apiName + "-" + API_VERSION + " API: " + addAPIResponse.getData());
        apiId = addAPIResponse.getData();

        //publish API
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);

        //Add API Revision
        addAndDeployNewAPIRevision(apiId);

        //Add application and subscribe
        ApplicationDTO applicationDTO = restAPIStore
                .addApplication("PolicyTestApp", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "",
                        "Operation policy test App");
        restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(), APIMIntegrationConstants.API_TIER.GOLD);

        // generate token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "36000",
                "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();
    }

    @Test(groups = { "wso2.am" }, description = "Test mediation for SET_HEADER policy in request flow")
    public void testAddOperationPolicyInPostAndUpdateAPIFlow() throws Exception {
        String apiData = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("operation-policies/api-data.json"), "UTF-8");
        org.codehaus.jackson.map.ObjectMapper objectMapper = new org.codehaus.jackson.map.ObjectMapper();
        APIDTO apidto = objectMapper.readValue(apiData, APIDTO.class);
        apidto.setProvider(user.getUserName());
        APIDTO addAPIResponse = restAPIPublisher.addAPI(apidto, "v2");

        for(APIOperationsDTO operation : addAPIResponse.getOperations()){
            String verb = operation.getVerb();
            String resource = operation.getTarget();
            if ("GET".equals(verb) && "/menu".equals(resource)) {
                List<OperationPolicyDTO> inPolicies = operation.getOperationPolicies().getIn();
                List<OperationPolicyDTO> outPolicies = operation.getOperationPolicies().getOut();

                Assert.assertEquals(OperationPolicyDTO.PolicyTypeEnum.SET_HEADER.toString(),
                        inPolicies.get(0).getPolicyType().toString(), "Operation policy was not added to API");
                Assert.assertEquals(OperationPolicyDTO.PolicyTypeEnum.REMOVE_HEADER.toString(),
                        outPolicies.get(0).getPolicyType().toString(), "Operation policy was not added to API");
            }
        }

        //Remove policies in update flow
        addAPIResponse.getOperations().get(0).setOperationPolicies(new APIOperationPoliciesDTO());
        APIDTO updatedAPIResponse = restAPIPublisher.updateAPI(addAPIResponse, addAPIResponse.getId());
        for(APIOperationsDTO operation : updatedAPIResponse.getOperations()){
            String verb = operation.getVerb();
            String resource = operation.getTarget();
            //use constant
            if ("GET".equals(verb) && "/menu".equals(resource)) {
                List<OperationPolicyDTO> inPolicies = operation.getOperationPolicies().getIn();
                List<OperationPolicyDTO> outPolicies = operation.getOperationPolicies().getOut();

                Assert.assertEquals(inPolicies.size(), 0, "'IN' operation policy was not removed in update API flow");
                Assert.assertEquals(outPolicies.size(), 0, "'OUT' operation policy was not removed in update API flow");
            }
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test mediation for SET_HEADER policy in request flow")
    public void testSetRequestHeaderPolicy() throws Exception {
        //Fetch API
        APIDTO api = fetchAPI(apiId);

        //Attach SET_HEADER operation policy to resource in flow
        OperationPolicyDTO policy = constructPolicyDTO(OperationPolicyDTO.PolicyTypeEnum.SET_HEADER, "headerName:org",
                "headerValue:abcd");
        addInPolicy(api, policy, IN_FLOW, false);

        //Add new API Revision and deploy
        addAndDeployNewAPIRevision(apiId);

        //Invoke API
        JSONObject responseJson = invokeAPI(RESOURCE_PATH, accessToken);

        Map<String, String> result = new ObjectMapper()
                .readValue(responseJson.get("headers").toString(), Map.class);
        Assert.assertTrue(result.containsKey("org"), "Request Header is missing: " + responseJson.toString());
        Assert.assertTrue("abcd".equals(result.get("org")),
                "Incorrect Request Header value: " + responseJson.toString());
    }

    @Test(groups = { "wso2.am" }, description = "Test mediation for REMOVE_HEADER policy in request flow",
            dependsOnMethods = "testSetRequestHeaderPolicy")
    public void testRemoveRequestHeaderPolicy() throws Exception {
        //Fetch API
        APIDTO api = fetchAPI(apiId);

        //Attach REMOVE_HEADER operation policy to resource in flow
        OperationPolicyDTO policy = constructPolicyDTO(OperationPolicyDTO.PolicyTypeEnum.REMOVE_HEADER,
                "headerName:org");
        addInPolicy(api, policy, IN_FLOW, false);

        //Add new API Revision and deploy
        addAndDeployNewAPIRevision(apiId);

        //invoke API (/echo-request GET request)
        JSONObject responseJson = invokeAPI(RESOURCE_PATH, accessToken);

        Map<String, String> result = new ObjectMapper()
                .readValue(responseJson.get("headers").toString(), Map.class);
        Assert.assertTrue(!result.containsKey("org"), "Request Header is still there: " + responseJson.toString());
    }

    @Test(groups = { "wso2.am" }, description = "Test mediation for REMOVE_HEADER policy in request flow",
            dependsOnMethods = "testRemoveRequestHeaderPolicy")
    public void testRemapRequestHeaderPolicy() throws Exception {
        //Fetch API
        APIDTO api = fetchAPI(apiId);

        //Attach SET_HEADER operation policy to resource in flow
        OperationPolicyDTO policy1 = constructPolicyDTO(OperationPolicyDTO.PolicyTypeEnum.SET_HEADER,
                "headerName:emp-id", "headerValue:1234");
        addInPolicy(api, policy1, IN_FLOW, true);

        OperationPolicyDTO policy2 = constructPolicyDTO(OperationPolicyDTO.PolicyTypeEnum.SET_HEADER,
                "headerName:ref-no", "headerExpression:req.header.emp-id");
        addInPolicy(api, policy2, IN_FLOW, false);

        //Add new API Revision and deploy
        addAndDeployNewAPIRevision(apiId);

        //invoke API (/echo-request GET request)
        JSONObject responseJson = invokeAPI(RESOURCE_PATH, accessToken);

        Map<String, String> result = new ObjectMapper()
                .readValue(responseJson.get("headers").toString(), Map.class);
        Assert.assertTrue(result.containsKey("ref-no"), "Request Header is missing: " + responseJson.toString());
        Assert.assertTrue("1234".equals(result.get("ref-no")),
                "Incorrect Request Header value: " + responseJson.toString());
    }

    @Test(groups = { "wso2.am" }, description = "Test mediation for REMOVE_HEADER policy in request flow",
            dependsOnMethods = "testRemapRequestHeaderPolicy")
    public void testAddQueryParamPolicy() throws Exception {
        //Fetch API
        APIDTO api = fetchAPI(apiId);

        //Attach SET_HEADER operation policy to resource in flow
        OperationPolicyDTO policy = constructPolicyDTO(OperationPolicyDTO.PolicyTypeEnum.ADD_QUERY_PARAM,
                "paramName:idQuery", "paramValue:4567");
        addInPolicy(api, policy, IN_FLOW, true);

        //Add new API Revision and deploy
        addAndDeployNewAPIRevision(apiId);

        //invoke API (/echo-request GET request)
        JSONObject responseJson = invokeAPI(RESOURCE_PATH, accessToken);

        Map<String, String> result = new ObjectMapper()
                .readValue(responseJson.get("query-parameters").toString(), Map.class);
        Assert.assertTrue(result.containsKey("idQuery"), "Query Parameter is missing: " + responseJson.toString());
        Assert.assertTrue("4567".equals(result.get("idQuery")),
                "Incorrect query parameter value: " + responseJson.toString());
    }

    @Test(groups = { "wso2.am" }, description = "Test mediation for ADD_QUERY_PARAM policy in request flow with param remap",
            dependsOnMethods = "testAddQueryParamPolicy")
    public void testRemapQueryParamPolicy() throws Exception {
        //Fetch API
        APIDTO api = fetchAPI(apiId);

        //Attach ADD_QUERY_PARAM operation policy to resource in flow
        OperationPolicyDTO policy = constructPolicyDTO(OperationPolicyDTO.PolicyTypeEnum.ADD_QUERY_PARAM,
                "paramName:empName", "paramExpression:req.header.name");
        addInPolicy(api, policy, IN_FLOW, true);

        //Add new API Revision and deploy
        addAndDeployNewAPIRevision(apiId);

        //invoke API (/echo-request GET request)
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION) + RESOURCE_PATH;
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("name", "test1234");

        HttpResponse response = HTTPSClientUtils.doGet(invokeURL, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        JSONObject responseJson = (JSONObject) new JSONParser().parse(response.getData());

        Map<String, String> result = new ObjectMapper()
                .readValue(responseJson.get("query-parameters").toString(), Map.class);
        Assert.assertTrue(result.containsKey("empName"), "Query Parameter is missing: " + responseJson.toString());
        Assert.assertTrue("test1234".equals(result.get("empName")),
                "Mapped value for Query Parameter is incorrect: " + responseJson.toString());
    }

    @Test(groups = { "wso2.am" }, description = "Test mediation for REWRITE_HTTP_METHOD policy",
            dependsOnMethods = "testRemapQueryParamPolicy")
    public void testRewriteHttpMethodPolicy() throws Exception {
        //Fetch API
        APIDTO api = fetchAPI(apiId);

        //Attach REWRITE_HTTP_METHOD operation policy to resource in flow
        OperationPolicyDTO policy = constructPolicyDTO(OperationPolicyDTO.PolicyTypeEnum.REWRITE_HTTP_METHOD,
                "httpMethod:POST");
        addInPolicy(api, policy, IN_FLOW, true);

        //Add new API Revision and deploy
        addAndDeployNewAPIRevision(apiId);

        //invoke API (/echo-request GET request)
        JSONObject responseJson = invokeAPI(RESOURCE_PATH, accessToken);

        Assert.assertTrue("POST".equals(responseJson.get("http-verb")),
                "Incorrect http method invoked: " + responseJson.toString());
    }

    @Test(groups = { "wso2.am" }, description = "Test mediation for MOCK_RESPONSE policy",
            dependsOnMethods = "testRewriteHttpMethodPolicy")
    public void testMockResponsePolicy() throws Exception {
        //Fetch API
        APIDTO api = fetchAPI(apiId);

        //Attach REWRITE_HTTP_METHOD operation policy to resource in flow
        OperationPolicyDTO policy = constructPolicyDTO(OperationPolicyDTO.PolicyTypeEnum.MOCK_RESPONSE,
                "payload:{\"hello\": \"world\"}", "contentType:application/json", "statusCode:200");
        addInPolicy(api, policy, IN_FLOW, true);

        //Add new API Revision and deploy
        addAndDeployNewAPIRevision(apiId);

        //invoke API (/echo-request GET request)
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION) + RESOURCE_PATH;
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);

        HttpResponse response = HTTPSClientUtils.doGet(invokeURL, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        JSONObject responseJson = (JSONObject) new JSONParser().parse(response.getData().replace("\\", ""));

        Assert.assertTrue("world".equals(responseJson.get("hello")));
    }

    /*@Test(groups = { "wso2.am" }, description = "Test mediation for REMOVE_HEADER policy in request flow",
            dependsOnMethods = "testAddQueryParamPolicy")
    public void testRemoveQueryParamPolicy() throws Exception {
        //Fetch API
        APIDTO api = fetchAPI(apiId);

        //Attach REMOVE_HEADER operation policy to resource in flow
        OperationPolicyDTO policy = new OperationPolicyDTO();
        Map<String, String> parameters = new HashMap<>();
        parameters.put("paramName", "idQuery");
        policy.setPolicyType(OperationPolicyDTO.PolicyTypeEnum.REMOVE_QUERY_PARAM);
        policy.setParameters(parameters);

        addInPolicy(api, policy, IN_FLOW, true);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(api, apiId);
        Assert.assertNotNull(updatedAPI, "Error while updating API with Operation Policy");

        //Add new API Revision and deploy
        addAndDeployNewAPIRevision(apiId);

        //invoke API (/echo-request GET request)
        JSONObject responseJson = invokeAPI(RESOURCE_PATH + "?name=abcd&idQuery=1234&org=Test", accessToken);

        Map<String, String> result = new ObjectMapper()
                .readValue(responseJson.get("query-parameters").toString(), Map.class);
        Assert.assertTrue(!result.containsKey("idQuery"), "Query Parameter is still there: " + responseJson.toString());
        Assert.assertTrue(result.containsKey("name"), "Name Query Parameter is missing: " + responseJson.toString());
        Assert.assertTrue(result.containsKey("org"), "Org Query Parameter is missing: " + responseJson.toString());
    }*/

    @Test(groups = { "wso2.am" }, description = "Test mediation for SET_HEADER policy in response flow",
            dependsOnMethods = "testRemapRequestHeaderPolicy")
    public void testSetResponseHeaderPolicy() throws Exception {
        //Fetch API
        APIDTO api = fetchAPI(apiId);

        //Attach SET_HEADER operation policy to response in flow
        OperationPolicyDTO policy1 = new OperationPolicyDTO();
        Map<String, String> parameters1 = new HashMap<>();
        parameters1.put("headerName", "org-resp");
        parameters1.put("headerValue", "abcd");
        policy1.setPolicyType(OperationPolicyDTO.PolicyTypeEnum.SET_HEADER);
        policy1.setParameters(parameters1);
        addInPolicy(api, policy1, OUT_FLOW, true);

        APIDTO updatedAPI = restAPIPublisher.updateAPI(api, apiId);
        Assert.assertNotNull(updatedAPI, "Error while updating API with Operation Policy");

        //Add new API Revision and deploy
        addAndDeployNewAPIRevision(apiId);

        //invoke API (/echo-request GET request)
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION) + RESOURCE_PATH;
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);

        HttpResponse response = HTTPSClientUtils.doGet(invokeURL, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        Assert.assertTrue(response.getHeaders().containsKey("org-resp"), "Response header is missing");
        Assert.assertTrue("abcd".equals(response.getHeaders().get("org-resp")), "Header value is incorrect");
    }

    @Test(groups = { "wso2.am" }, description = "Test mediation for REMOVE_HEADER policy in response flow",
            dependsOnMethods = "testSetResponseHeaderPolicy")
    public void testRemoveResponseHeaderPolicy() throws Exception {
        //Fetch API
        APIDTO api = fetchAPI(apiId);

        //Attach SET_HEADER operation policy to response in flow
        OperationPolicyDTO policy = new OperationPolicyDTO();
        Map<String, String> parameters = new HashMap<>();
        parameters.put("headerName", "org-resp");
        policy.setPolicyType(OperationPolicyDTO.PolicyTypeEnum.REMOVE_HEADER);
        policy.setParameters(parameters);
        addInPolicy(api, policy, OUT_FLOW, false);

        APIDTO updatedAPI = restAPIPublisher.updateAPI(api, apiId);
        Assert.assertNotNull(updatedAPI, "Error while updating API with Operation Policy");

        //Add new API Revision and deploy
        addAndDeployNewAPIRevision(apiId);

        //invoke API (/echo-request GET request)
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION) + RESOURCE_PATH;
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);

        HttpResponse response = HTTPSClientUtils.doGet(invokeURL, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        Assert.assertTrue(!response.getHeaders().containsKey("org-resp"), "Response header was not removed");
    }

    @Test(groups = { "wso2.am" }, description = "Test mediation for REWRITE_RESOURCE_PATH policy",
            dependsOnMethods = "testRemoveResponseHeaderPolicy")
    public void testRewriteResourcePathPolicy() throws Exception {
        //Fetch API
        APIDTO api = fetchAPI(apiId);

        //expose GET /EMPLOYEES resource as GET /employees
        List<APIOperationsDTO> operationsDTOList = api.getOperations();
        APIOperationsDTO operationsDTO = new APIOperationsDTO();
        operationsDTO.setVerb("GET");
        operationsDTO.setTarget("/employees");

        OperationPolicyDTO policyDTO = constructPolicyDTO(OperationPolicyDTO.PolicyTypeEnum.REWRITE_RESOURCE_PATH,
                "resourcePath:/EMPLOYEE");
        APIOperationPoliciesDTO operationPoliciesDTO = new APIOperationPoliciesDTO();
        List<OperationPolicyDTO> inPolicies = new ArrayList<>();
        inPolicies.add(policyDTO);
        operationPoliciesDTO.setIn(inPolicies);
        operationsDTO.setOperationPolicies(operationPoliciesDTO);
        operationsDTOList.add(operationsDTO);

        APIDTO updatedAPI = restAPIPublisher.updateAPI(api, apiId);
        Assert.assertNotNull(updatedAPI, "Error while updating API with Operation Policy");

        //Add new API Revision and deploy
        addAndDeployNewAPIRevision(apiId);

        JSONObject response = invokeAPI("/employees", accessToken);
        Assert.assertEquals(((JSONArray) response.get("employees")).size(), 2,
                "Error while mediating REWRITE_RESOURCE_PATH");
    }

    private APIDTO fetchAPI(String id) throws Exception {
        HttpResponse apiResponse = restAPIPublisher.getAPI(id);
        Assert.assertEquals(apiResponse.getResponseCode(), 200, "Error while fetching API: " + apiResponse.getData());
        Gson gson = new Gson();
        APIDTO api = gson.fromJson(apiResponse.getData(), APIDTO.class);
        return api;
    }

    private void addAndDeployNewAPIRevision(String id) throws Exception {
        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(id);

        //Undeploy and delete previous revision
        if (revisionId != null) {
            List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
            APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
            apiRevisionUnDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
            apiRevisionUnDeployRequest.setVhost(null);
            apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
            apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
            HttpResponse undeployRevisionResponse = restAPIPublisher
                    .undeployAPIRevision(apiId, revisionId, apiRevisionUndeployRequestList);
            Assert.assertEquals(undeployRevisionResponse.getResponseCode(), HttpStatus.SC_CREATED,
                    "Unable to Undeploy API Revisions:" + undeployRevisionResponse.getData());

            HttpResponse deleteRevisionResponse = restAPIPublisher.deleteAPIRevision(apiId, revisionId);
            Assert.assertEquals(deleteRevisionResponse.getResponseCode(), HttpStatus.SC_OK,
                    "Unable to delete API Revisions: " + deleteRevisionResponse.getData());
        }

        //Add and deploy new revision
        HttpResponse addRevisionResponse = restAPIPublisher.addAPIRevision(apiRevisionRequest);
        Assert.assertEquals(addRevisionResponse.getResponseCode(), HttpStatus.SC_CREATED,
                "Error while adding API revision:" + addRevisionResponse.getData());
        org.json.JSONObject revisionResponseData = new org.json.JSONObject(addRevisionResponse.getData());
        revisionId = revisionResponseData.getString("id");

        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost("localhost");

        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIRevision(id, revisionId,
                apiRevisionDeployRequest,"API");
        Assert.assertEquals(apiRevisionsDeployResponse.getResponseCode(), 201,
                "Error while deploying revision: " + apiRevisionsDeployResponse.getData());
        waitForAPIDeployment();
    }

    private void addOperation(APIRequest apiRequest, String target, String verb,
            APIOperationPoliciesDTO operationPoliciesDTO) {
        List<APIOperationsDTO> operationsDTOS = apiRequest.getOperationsDTOS();
        if (operationsDTOS == null) {
            operationsDTOS = new ArrayList<>();
        }

        APIOperationsDTO operation = new APIOperationsDTO();
        operation.setTarget(target);
        operation.setVerb(verb);
        operationsDTOS.add(operation);
        apiRequest.setOperationsDTOS(operationsDTOS);
    }

    private void addInPolicy(APIDTO api, OperationPolicyDTO policy, String flow, boolean reset) throws Exception {
        List<APIOperationsDTO> operations = api.getOperations();
        for (APIOperationsDTO operation : operations) {
            String verb = operation.getVerb();
            String target = operation.getTarget();
            if (APIMIntegrationConstants.HTTP_VERB_GET.equals(verb) && RESOURCE_PATH.equals(target)) {
                APIOperationPoliciesDTO operationPolicies = operation.getOperationPolicies() != null ?
                        operation.getOperationPolicies() :
                        new APIOperationPoliciesDTO();

                if (IN_FLOW.equals(flow)) {
                    List<OperationPolicyDTO> policies =
                            operationPolicies.getIn() != null && !reset ? operationPolicies.getIn() : new ArrayList<>();
                    policies.add(policy);
                    operationPolicies.setIn(policies);
                } else {
                    List<OperationPolicyDTO> policies = operationPolicies.getOut() != null && !reset ?
                            operationPolicies.getOut() :
                            new ArrayList<>();
                    policies.add(policy);
                    operationPolicies.setOut(policies);
                }
                operation.setOperationPolicies(operationPolicies);
            }
        }
        APIDTO updatedAPI = restAPIPublisher.updateAPI(api, apiId);
        Assert.assertNotNull(updatedAPI, "Error while updating API with Operation Policy");
    }

    private JSONObject invokeAPI(String resource, String token) throws Exception {
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION) + resource;
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + token);

        HttpResponse response = HTTPSClientUtils.doGet(invokeURL, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK);
        JSONObject responseJson = (JSONObject) new JSONParser().parse(response.getData());
        return responseJson;
    }

    private OperationPolicyDTO constructPolicyDTO(OperationPolicyDTO.PolicyTypeEnum policyType, String...params) {
        OperationPolicyDTO policy = new OperationPolicyDTO();
        Map<String, String> parameters = new HashMap<>();
        for (String param : params) {
            String[] paramKeyValue = param.split(":", 2);
            if (paramKeyValue.length == 2) {
                parameters.put(paramKeyValue[0], paramKeyValue[1]);
            }
        }
        policy.setPolicyType(policyType);
        policy.setParameters(parameters);
        return policy;
    }
}
