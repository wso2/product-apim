package org.wso2.am.integration.cucumbertests.stepdefinitions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.datatable.DataTable;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationPoliciesDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIScopeDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIBusinessInformationDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OperationPolicyDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ScopeDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SearchResultListDTO;
import org.wso2.am.integration.cucumbertests.TestContext;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PublisherStepDefinitions {

    private final String API_ENDPOINT_POSTFIX_URL = "http://nodebackend:3001/jaxrs_basic/services/customers/customerservice/";

    RestAPIPublisherImpl publisher;
    String createdDocId;
    String baseUrl;

    private final TestContext context;

    public PublisherStepDefinitions(TestContext testcontext) {
        this.context = testcontext;
        baseUrl = context.get("baseUrl").toString();
    }

    @When("I initialize the Publisher REST API client with username {string}, password {string} and tenant {string}")
    public void i_initialize_publisher_client(String username, String password, String tenantDomain) {
        String containerLabel = context.get("label").toString();
        publisher = new RestAPIPublisherImpl(username, password, tenantDomain, baseUrl, containerLabel);
    }

    @When("I create an API with the following details")
    public void i_create_api(DataTable dataTable) throws Exception {
        Map<String, String> data = dataTable.asMap(String.class, String.class);

        String name = data.getOrDefault("name", "DefaultAPI");
        String contextPath = data.getOrDefault("context", "/defaultContext");
        String version = data.getOrDefault("version", "1.0.0");
        String apiProductionEndPointUrl = data.getOrDefault("apiEndpointURL", API_ENDPOINT_POSTFIX_URL);
        context.set("apiProductionEndPointUrl", apiProductionEndPointUrl);

        APIRequest apiRequest = new APIRequest(name, contextPath, new URL(apiProductionEndPointUrl));

        apiRequest.setVersion(version);
        if (data.containsKey("description")) {
            apiRequest.setDescription(data.get("description"));
        }
        if (data.containsKey("tags")) {
            apiRequest.setTags(data.get("tags"));
        }
        if (data.containsKey("tiersCollection")) {
            apiRequest.setTiersCollection(data.get("tiersCollection"));
        }
        if (data.containsKey("tier")) {
            apiRequest.setTier(data.get("tier"));
        }

        HttpResponse apiCreationResponse = publisher.addAPI(apiRequest);

        String apiId = apiCreationResponse.getData();
        this.context.set("createdApiId", apiId);
    }

    @When("I create an API with the JSON payload from file {string}")
    public void i_create_api_with_json_payload_from_file(String jsonFilePath) throws ApiException, IOException {

        String jsonPayload = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(jsonFilePath),
                "UTF-8");
        ObjectMapper objectMapper = new ObjectMapper();
        APIRequest apiRequest = objectMapper.readValue(jsonPayload, APIRequest.class);
        HttpResponse apiCreationResponse = publisher.addAPI(apiRequest);
        String apiId = apiCreationResponse.getData();
        context.set("createdApiId", apiId);
        context.set("httpResponse", apiCreationResponse);
    }


    @When("I update API of id {string} with the following details")
    public void i_update_api(String appId, DataTable dataTable) throws Exception {
        Map<String, String> data = dataTable.asMap(String.class, String.class);
        String actualApiId = resolveFromContext(appId);
        APIDTO apiDto = publisher.getAPIByID(actualApiId);

        if (data.containsKey("name")) {apiDto.setName(data.get("name"));};
        if (data.containsKey("version")) {apiDto.setVersion(data.get("version"));};
        if (data.containsKey("context")) {apiDto.setContext(data.get("context"));};
        if (data.containsKey("description")) {apiDto.setDescription(data.get("description"));};
        if (data.containsKey("tags")) {apiDto.setTags(Collections.singletonList(data.get("tags")));};
        if (data.containsKey("defaultVersion")) {apiDto.setIsDefaultVersion(Boolean.valueOf(data.get("defaultVersion")));};

        APIBusinessInformationDTO businessinfo = new APIBusinessInformationDTO();
        if (data.containsKey("businessOwner")) {businessinfo.setBusinessOwner(data.get("businessOwner"));};
        if (data.containsKey("businessOwnerEmail")) {businessinfo.setBusinessOwnerEmail(data.get("businessOwnerEmail"));};
        if (data.containsKey("technicalOwner")) {businessinfo.setTechnicalOwner(data.get("technicalOwner"));};
        if (data.containsKey("technicalOwnerEmail")) {businessinfo.setTechnicalOwnerEmail(data.get("technicalOwnerEmail"));};
        apiDto.setBusinessInformation(businessinfo);

        if (data.containsKey("securitySchemes")) {
            List<String> securitySchemes = Arrays.stream(data.get("securitySchemes").split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
            apiDto.setSecurityScheme(securitySchemes);
        } else {
            apiDto.setSecurityScheme(Arrays.asList("oauth2", "api_key"));
        }

        if (data.containsKey("scopes")) {
            List<APIScopeDTO> scopesList = new ArrayList<>();
            for (String scopeName : data.get("scopes").split(",")) {
                APIScopeDTO apiScopeDTO = new APIScopeDTO();
                apiScopeDTO.setScope((ScopeDTO) context.get(scopeName));
                scopesList.add(apiScopeDTO);
            }
            apiDto.setScopes(scopesList);
        }

        if (data.containsKey("operations")) {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, String>> operations = objectMapper.readValue(
                    data.get("operations"),
                    new TypeReference<List<Map<String, String>>>() {}
            );

            List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
            for (Map<String, String> op : operations) {
                APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
                apiOperationsDTO.setTarget(op.get("target"));
                apiOperationsDTO.setVerb(op.get("verb"));
                String authType = op.getOrDefault("authType", "Application & Application User");
                apiOperationsDTO.setAuthType(authType);
                String throttlingPolicy = op.getOrDefault("throttlingPolicy", "Unlimited");
                apiOperationsDTO.setThrottlingPolicy(throttlingPolicy);
                if (op.containsKey("scopes")) {
                    List<String> scopesList = new ArrayList<>();
                    for (String scopeName : op.get("scopes").split(",")) {
                        scopesList.add(scopeName.trim());
                    }
                    apiOperationsDTO.setScopes(scopesList);
                }
                operationsDTOS.add(apiOperationsDTO);
            }
            apiDto.setOperations(operationsDTOS);
        }

        HttpResponse apiUpdateResponse = publisher.updateAPIWithHttpInfo(apiDto);
        context.set("httpResponse", apiUpdateResponse);
    }

    @When("I update API of id {string} with the JSON payload from file {string}")
    public void i_update_api_with_json_payload_from_file(String appId, String jsonFilePath) throws IOException, ApiException {

        String jsonPayload = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(jsonFilePath),
                "UTF-8");
        ObjectMapper objectMapper = new ObjectMapper();
        APIRequest apiRequest = objectMapper.readValue(jsonPayload, APIRequest.class);
        String actualApiId = resolveFromContext(appId);
        HttpResponse apiUpdateResponse = publisher.updateAPI(apiRequest, actualApiId);
        context.set("httpResponse", apiUpdateResponse);
    }


    @Then("I verify the updated API with id {string} has the following api policies")
    public void i_verify_updated_api_policies(String apiId, DataTable dataTable) throws Exception {

        String actualApiId = resolveFromContext(apiId);
        APIDTO apiDto = publisher.getAPIByID(actualApiId);
        Map<String, String> expectedPolicies = dataTable.asMap(String.class, String.class);

        APIOperationPoliciesDTO apiPolicies = apiDto.getApiPolicies();

        if (apiPolicies != null){
            validatePolicy("request", apiPolicies.getRequest(), expectedPolicies.get("request"));
            validatePolicy("response", apiPolicies.getResponse(), expectedPolicies.get("response"));
            validatePolicy("fault", apiPolicies.getFault(), expectedPolicies.get("fault"));
        }

        else {
            Assert.fail("API policies are null for API with ID: " + actualApiId);
        }

    }

    @When("I add an operation with the following details to the created API with id {string}")
    public void i_add_operation_to_api(String apiId, DataTable dataTable) throws Exception {
        Map<String, String> data = dataTable.asMap(String.class, String.class);
        String actualApiId = resolveFromContext(apiId);
        APIDTO apiDto = publisher.getAPIByID(actualApiId);

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setTarget(data.get("target"));
        apiOperationsDTO.setVerb(data.get("verb"));
        if (data.containsKey("authType")) {apiOperationsDTO.setAuthType(data.get("authTYpe"));};
        if (data.containsKey("throttlingPolicy")) {apiOperationsDTO.setAuthType(data.get("throttlingPolicy"));};

        if (data.containsKey("scopes")) {
            List<String> scopesList = new ArrayList<>();
            for (String scopeName : data.get("scopes").split(",")) {
                scopesList.add(scopeName.trim());
            }
            apiOperationsDTO.setScopes(scopesList);
        }
        operationsDTOS.add(apiOperationsDTO);
        apiDto.setOperations(operationsDTOS);
        publisher.updateAPI(apiDto, actualApiId);
    }

    @When("I update the API with id {string} to use API key header {string}")
    public void update_api_with_custom_api_key_header(String apiId, String headerName) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        APIDTO apiDto = publisher.getAPIByID(actualApiId);
        apiDto.setApiKeyHeader(headerName);
        publisher.updateAPI(apiDto);
        APIDTO apiDtoResponse = publisher.getAPIByID(actualApiId);
        Assert.assertEquals(apiDtoResponse.getApiKeyHeader(),headerName);
    }

    @When("I create scope {string} with roles {string}")
    public void i_create_scope_with_roles(String scopeName, String rolesCsv) throws Exception {
        List<String> rolesList = new ArrayList<>();
        for (String role : rolesCsv.split(",")) {
            rolesList.add(role.trim());
        }
        ScopeDTO scope = new ScopeDTO();
        scope.setName(scopeName.trim());
        scope.setBindings(rolesList);
        context.set(scopeName.trim(), scope);
    }

    @When("I add scopes {string} to the created API with id {string}")
    public void i_add_scopes_to_created_api(String scopesCsv, String apiId) throws Exception {

        String actualApiId = resolveFromContext(apiId);
        APIDTO apiDto= publisher.getAPIByID(actualApiId);

        List<APIScopeDTO> scopesList = new ArrayList<>();
        for (String scopeName : scopesCsv.split(",")) {
            APIScopeDTO apiScopeDTO = new APIScopeDTO();
            apiScopeDTO.setScope((ScopeDTO) context.get(scopeName));
            scopesList.add(apiScopeDTO);
        }
        apiDto.setScopes(scopesList);
        ApiResponse<APIDTO> apiDtoUpdateApiResponse = publisher.updateAPI(apiDto, actualApiId);
        context.set("ApiDtoApiResponse", apiDtoUpdateApiResponse);
    }

    @When("I deploy a revision of the API with id {string}")
    public void i_deploy_revision(String apiId) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        String revisionUUID = publisher.createAPIRevisionAndDeployUsingRest(actualApiId);
        Thread.sleep(10000);
        context.set("revisionID",revisionUUID);
    }

    @When("I delete the API with id {string}")
    public void i_delete_the_api(String apiId) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        HttpResponse apiDeleteResponse = publisher.deleteAPI(actualApiId);
        context.set("httpResponse", apiDeleteResponse);
    }

    @When("I publish the API with id {string}")
    public void i_publish_the_api(String apiId) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        publisher.changeAPILifeCycleStatus(actualApiId, "Publish", null);
    }

    @When("I block the API with id {string}")
    public void i_block_the_api(String apiId) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        publisher.blockAPI(actualApiId);
    }

    @When("I deprecate the API with id {string}")
    public void i_deprecate_the_api(String apiId) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        publisher.deprecateAPI(actualApiId);
    }

    @When("I create a new version {string} from API with id {string}")
    public void i_create_new_version_of_api(String newVersion, String apiId) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        publisher.createNewAPIVersion(newVersion, actualApiId, false);
    }

    @When("I retrieve the API with id {string}")
    public void i_retrieve_the_api(String apiId) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        HttpResponse response = publisher.getAPI(actualApiId);
        context.set("httpResponse", response);
    }

    @When("I retrieve all APIs created through the Publisher REST API")
    public void i_retrieve_all_apis() throws APIManagerIntegrationTestException, ApiException {
        APIListDTO response = publisher.getAllAPIs();
        Gson gson = new Gson();
        String json = gson.toJson(response);
        JSONObject jsonObject = new JSONObject(json);
        JSONArray jsonArray = jsonObject.getJSONArray("list");
        Map<String, JSONObject> apiMap = new HashMap<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject apiObj = jsonArray.getJSONObject(i);
            String apiId = apiObj.getString("id");
            apiMap.put(apiId, apiObj);
        }
        context.set("allApisMap", apiMap);
    }


    @Then("The API with id {string} should be in the list of all APIS")
    public void the_api_should_be_in_the_list_of_all_apis(String apiId)  {
        String actualApiId = resolveFromContext(apiId);
        Map<String, JSONObject> apiMap = (Map<String, JSONObject>) context.get("allApisMap");
        Assert.assertTrue(apiMap.containsKey(actualApiId),"API with ID " + actualApiId + " was not found.");
    }


    @Then("The lifecycle status of API {string} should be {string}")
    public void the_lifecycle_status_should_be(String apiId, String status) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        HttpResponse response = publisher.getLifecycleStatus(actualApiId);
        Assert.assertEquals(response.getData(), status);
    }

    @When("I search APIs with query {string}")
    public void i_search_apis_with_query(String query) throws Exception {
        SearchResultListDTO searchResults = publisher.searchAPIs(query);
        System.out.println("Search results: " + searchResults);
        context.set("searchResults", searchResults);
    }

    @When("I find the apiUUID of the API created with the name {string} and version {string}")
    public void find_api_uuid_using_name(String apiName, String apiVersion) throws Exception {

        String searchQuery = String.format("name:%s version:%s", apiName, apiVersion);
        SearchResultListDTO searchResults = publisher.searchAPIs(searchQuery);
        Assert.assertTrue(searchResults != null && searchResults.getList() !=null &&
                        ! searchResults.getList().isEmpty(), "No APIs found with the given name and version");

        Map<String, Object> apiInfo = (Map<String, Object>) searchResults.getList().get(0);
        context.set("selectedApiId", apiInfo.get("id"));
    }


    @When("I add a document to API {string} with name {string}")
    public void i_add_document_to_api(String apiId, String docName) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        DocumentDTO doc = new DocumentDTO();
        doc.setName(docName);
        doc.setType(DocumentDTO.TypeEnum.HOWTO);
        doc.setSourceType(DocumentDTO.SourceTypeEnum.INLINE);
        doc.setVisibility(DocumentDTO.VisibilityEnum.API_LEVEL);
        HttpResponse response = publisher.addDocument(actualApiId, doc);
        createdDocId = response.getData();
    }

    @When("I add content {string} to document {string} of API {string}")
    public void i_add_content_to_document(String content, String docId, String apiId) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        publisher.addContentDocument(actualApiId, docId, content);
    }

    @When("I delete the document {string} from API {string}")
    public void i_delete_document(String docId, String apiId) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        publisher.deleteDocument(actualApiId, docId);
    }

    @When("I import Swagger file {string} for API with properties {string}")
    public void i_import_swagger_file(String filePath, String properties) throws Exception {
        File file = new File(filePath);
        publisher.importOASDefinition(file, properties);
    }

    @When("I import GraphQL schema file {string} for API with properties {string}")
    public void i_import_graphql_schema_file(String filePath, String properties) throws Exception {
        File file = new File(filePath);
        publisher.importGraphqlSchemaDefinition(file, properties);
    }

    @When("I import WSDL file {string} for API with properties {string}")
    public void i_import_wsdl_file(String filePath, String properties) throws Exception {
        File file = new File(filePath);
        publisher.importWSDLSchemaDefinition(file, null, properties, "WSDL1");
    }

    @When("I create a revision for API {string} with description {string}")
    public void i_create_api_revision(String apiId, String description) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        APIRevisionRequest request = new APIRevisionRequest();
        request.setApiUUID(actualApiId);
        request.setDescription(description);
        publisher.addAPIRevision(request);
    }

    @When("I deploy revision {string} of API {string} to gateway {string}")
    public void i_deploy_api_revision(String revisionId, String apiId, String gateway) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        APIRevisionDeployUndeployRequest deployRequest = new APIRevisionDeployUndeployRequest();
        deployRequest.setName(gateway);
        deployRequest.setVhost("localhost");
        deployRequest.setDisplayOnDevportal(true);
        publisher.deployAPIRevision(actualApiId, revisionId, deployRequest, "API");
    }

    @When("I undeploy revision {string} of API {string} from gateway {string}")
    public void i_undeploy_api_revision(String revisionId, String apiId, String gateway) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        APIRevisionDeployUndeployRequest deployRequest = new APIRevisionDeployUndeployRequest();
        deployRequest.setName(gateway);
        deployRequest.setVhost("localhost");
        deployRequest.setDisplayOnDevportal(true);
        List<APIRevisionDeployUndeployRequest> list = Collections.singletonList(deployRequest);
        publisher.undeployAPIRevision(actualApiId, revisionId, list);
    }

    @When("I upload endpoint certificate {string} with alias {string} for endpoint {string}")
    public void i_upload_endpoint_certificate(String certPath, String alias, String endpoint) throws Exception {
        File file = new File(certPath);
        publisher.uploadEndpointCertificate(file, alias, endpoint);
    }

    @When("I validate endpoint {string} for API {string}")
    public void i_validate_endpoint(String endpoint, String apiId) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        publisher.checkValidEndpoint(endpoint, actualApiId);
    }

    @When("I delete shared scope with id {string}")
    public void i_delete_shared_scope(String scopeId) throws Exception {
        String actualScopeId = resolveFromContext(scopeId);
        publisher.deleteSharedScope(actualScopeId);
    }

    @Then("I clean up API with id {string}")
    public void i_clean_up_api_and_application(String apiId) throws Exception {
        String actualApiId = resolveFromContext(apiId);
        publisher.deleteAPI(actualApiId);
    }

    private String resolveFromContext(String input) {
        if (input.startsWith("<") && input.endsWith(">")) {
            return (String) context.get(input.substring(1, input.length() - 1));
        }
        return input;
    }

    /**
     * Validates that the expected policy exists in the given list and has a non-empty ID.
     *
     * @param policies List of OperationPolicyDTO
     * @param expectedPolicyName Expected policy name
     * @param policyType Type of policy
     */
    private void validatePolicy(String policyType, List<OperationPolicyDTO> policies, String expectedPolicyName) {

        boolean hasPolicy = policies != null && policies.stream()
                .anyMatch(p -> expectedPolicyName.equals(p.getPolicyName()) && p.getPolicyId() != null
                        && !p.getPolicyId().isEmpty());

        Assert.assertTrue(hasPolicy, "Expected " + policyType + " policy '" + expectedPolicyName
                + "' was not found.");
    }

}
