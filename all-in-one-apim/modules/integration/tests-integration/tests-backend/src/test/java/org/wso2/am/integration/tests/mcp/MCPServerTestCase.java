/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.mcp;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;;
import org.wso2.am.admin.clients.mediation.SynapseConfigAdminClient;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationMappingDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIRevisionDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIRevisionDeploymentDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIRevisionListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIScopeDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.BackendDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.BackendOperationDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.BackendOperationMappingDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MCPServerDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MCPServerOperationDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MCPServerProxyRequestDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ScopeDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SecurityInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO.TokenTypeEnum;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.testng.Assert.assertEquals;

/**
 * Integration tests for MCP Servers created via OpenAPI, API and third-party MCP Server proxying.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class MCPServerTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(MCPServerTestCase.class);

    private static final String GROUP_WSO2_AM = "wso2.am";
    private static final String OAS_FILE = "petstore-oas3.json";
    private static final String ADDITIONAL_PROPS_MCP_FILE = "petstore-oas3-mcp-additional-props.json";
    private static final String ADDITIONAL_PROPS_API_FILE = "petstore-oas3-api-additional-props.json";

    private static final String URL_KEY = "url";
    private static final String ENDPOINT_CONFIG = "endpointConfig";
    private static final String PROD_ENDPOINTS = "production_endpoints";
    private static final String SANDBOX_ENDPOINTS = "sandbox_endpoints";
    private static final String ENDPOINT_TYPE = "endpoint_type";

    private static final String PETSTORE_BACKEND_CONTEXT = "SwaggerPetstorev3";
    private static final String MCP_SERVER_BACKEND_CONTEXT = "mcp";

    private static final String TARGET_GET_PETS = "get_pets";
    private static final String TARGET_GET_PETS_BY_ID = "get_pets_by_petId";
    private static final String TARGET_DELETE_OLD_PETS = "delete_oldpets";

    private static final String TARGET_ECHO = "echo";
    private static final String TARGET_ADD = "add";
    private static final String TARGET_VIEW_MENU = "viewPizzaMenu";

    private static final String SERVER_NAME_PETSTORE_OPENAPI = "PetstoreMCPFromOpenAPI";
    private static final String SERVER_CONTEXT_PETSTORE_OPENAPI = "petstore-mcp-openapi";

    private static final String SERVER_NAME_PETSTORE_API = "PetstoreMCPFromAPI";
    private static final String SERVER_CONTEXT_PETSTORE_API = "petstore-mcp-api";
    private static final String SERVER_VERSION_1 = "1.0.0";

    private static final String SERVER_NAME_EVERYTHING = "EverythingMCP";
    private static final String SERVER_DISPLAY_EVERYTHING = "Everything MCP";
    private static final String SERVER_CONTEXT_EVERYTHING = "everything-mcp";

    private static final String PATH_PETS = "/pets";
    private static final String PATH_PETS_BY_ID = "/pets/{petId}";
    private static final String PATH_OLD_PETS = "/oldpets";

    private static final String DESC_GET_PET_BY_ID = "Get a pet by ID";
    private static final String DESC_GET_ECHO = "Echoes back the input";
    private static final String DESC_DELETE_OLD_PETS = "Delete all old pets";
    private static final String DESC_UPDATE_GET_PETS = "This description has been updated";

    private static final String LIFECYCLE_ACTION_PUBLISH = "Publish";
    private static final String LIFECYCLE_STATE_PUBLISHED = "Published";
    private static final String REVISION_NAME_DEFAULT = "Default";
    private static final String REVISION_VHOST_LOCALHOST = "localhost";
    private static final String DEPLOYMENT_STATUS_APPROVED = "APPROVED";
    private static final String REVISION_DESCRIPTION_1 = "Revision 1";

    private static final String APP_NAME_1 = "MCP Application 1";
    private static final String APP_NAME_2 = "MCP Application 2";
    private static final String APP_NAME_3 = "MCP Application 3";
    private static final String APP_DESC = "Test Application";
    private static final String DEVPORTAL_VISIBILITY_ERROR = "MCP Server is not visible in Developer Portal.";

    private static final int PORT_RANGE_START = 9950;
    private static final int PORT_RANGE_END_INCLUSIVE = 9999;

    private static final Duration POST_PUBLISH_SETTLE_WAIT = Duration.ofSeconds(1);

    private static final String EXPECTED_SCHEMA_GET_PETS_BY_ID =
            "{\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"properties\": {\n" +
                    "    \"path_petId\": {\n" +
                    "      \"type\": \"string\"\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"required\": [\"path_petId\"]\n" +
                    "}";

    private static final String EXPECTED_SCHEMA_ECHO =
            "{\n" +
                    "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                    "  \"additionalProperties\": false,\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"properties\": {\n" +
                    "    \"message\": {\n" +
                    "      \"description\": \"Message to echo\",\n" +
                    "      \"type\": \"string\"\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"required\": [\"message\"]\n" +
                    "}";

    private static final String EXPECTED_SCHEMA_DELETE_OLD_PETS =
            "{\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"properties\": {}\n" +
                    "}";

    private static final String EXPECTED_ENDPOINT_CONFIG =
            "{\n" +
                    "    \"endpoint_type\": \"http\",\n" +
                    "    \"sandbox_endpoints\": { \"url\": \"http://api.yourdomain.com/\" },\n" +
                    "    \"production_endpoints\": { \"url\": \"http://api.yourdomain.com/\" }\n" +
                    "  }\n";

    private static final String UPDATED_ENDPOINT_CONFIG =
            "{\n" +
                    "  \"endpoint_type\": \"http\",\n" +
                    "  \"sandbox_endpoints\": {\n" +
                    "    \"url\": \"https://localhost:9443/am/sample/pizzashack/v1/api/\"\n" +
                    "  },\n" +
                    "  \"endpoint_security\": {\n" +
                    "    \"production\": {\n" +
                    "      \"apiKeyValue\": null,\n" +
                    "      \"tokenUrl\": null,\n" +
                    "      \"connectionTimeoutDuration\": -1,\n" +
                    "      \"apiKeyIdentifier\": null,\n" +
                    "      \"type\": \"BASIC\",\n" +
                    "      \"enabled\": false,\n" +
                    "      \"apiKeyIdentifierType\": null,\n" +
                    "      \"password\": \"admin-pass\",\n" +
                    "      \"connectionTimeoutConfigType\": null,\n" +
                    "      \"clientSecret\": null,\n" +
                    "      \"connectionRequestTimeoutDuration\": -1,\n" +
                    "      \"uniqueIdentifier\": null,\n" +
                    "      \"clientId\": null,\n" +
                    "      \"secretKey\": null,\n" +
                    "      \"socketTimeoutDuration\": -1,\n" +
                    "      \"proxyConfigs\": {\n" +
                    "        \"proxyEnabled\": false,\n" +
                    "        \"proxyPort\": \"\",\n" +
                    "        \"proxyPasswordAlias\": null,\n" +
                    "        \"proxyProtocol\": \"\",\n" +
                    "        \"proxyUsername\": \"\",\n" +
                    "        \"proxyPassword\": \"\",\n" +
                    "        \"proxyHost\": \"\"\n" +
                    "      },\n" +
                    "      \"accessKey\": null,\n" +
                    "      \"service\": null,\n" +
                    "      \"proxyConfigType\": null,\n" +
                    "      \"customParameters\": {},\n" +
                    "      \"additionalProperties\": {},\n" +
                    "      \"grantType\": null,\n" +
                    "      \"region\": null,\n" +
                    "      \"username\": \"admin\"\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"production_endpoints\": {\n" +
                    "    \"url\": \"https://localhost:9443/am/sample/pizzashack/v1/api/test\"\n" +
                    "  }\n" +
                    "}";

    private static final String EXPECTED_UPDATED_ENDPOINT_CONFIG =
            "{\n" +
                    "  \"endpoint_type\": \"http\",\n" +
                    "  \"sandbox_endpoints\": {\n" +
                    "    \"url\": \"https://localhost:9443/am/sample/pizzashack/v1/api/\"\n" +
                    "  },\n" +
                    "  \"endpoint_security\": {\n" +
                    "    \"production\": {\n" +
                    "      \"apiKeyValue\": null,\n" +
                    "      \"tokenUrl\": null,\n" +
                    "      \"connectionTimeoutDuration\": -1,\n" +
                    "      \"apiKeyIdentifier\": null,\n" +
                    "      \"type\": \"BASIC\",\n" +
                    "      \"enabled\": false,\n" +
                    "      \"apiKeyIdentifierType\": null,\n" +
                    "      \"password\": \"\",\n" +
                    "      \"connectionTimeoutConfigType\": null,\n" +
                    "      \"clientSecret\": null,\n" +
                    "      \"connectionRequestTimeoutDuration\": -1,\n" +
                    "      \"uniqueIdentifier\": null,\n" +
                    "      \"clientId\": null,\n" +
                    "      \"secretKey\": null,\n" +
                    "      \"socketTimeoutDuration\": -1,\n" +
                    "      \"proxyConfigs\": {\n" +
                    "        \"proxyEnabled\": false,\n" +
                    "        \"proxyPort\": \"\",\n" +
                    "        \"proxyPasswordAlias\": null,\n" +
                    "        \"proxyProtocol\": \"\",\n" +
                    "        \"proxyUsername\": \"\",\n" +
                    "        \"proxyPassword\": \"\",\n" +
                    "        \"proxyHost\": \"\"\n" +
                    "      },\n" +
                    "      \"accessKey\": null,\n" +
                    "      \"service\": null,\n" +
                    "      \"proxyConfigType\": null,\n" +
                    "      \"customParameters\": {},\n" +
                    "      \"additionalProperties\": {},\n" +
                    "      \"grantType\": null,\n" +
                    "      \"region\": null,\n" +
                    "      \"username\": \"admin\"\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"production_endpoints\": {\n" +
                    "    \"url\": \"https://localhost:9443/am/sample/pizzashack/v1/api/test\"\n" +
                    "  }\n" +
                    "}";

    private static final String LIFECYCLE_CHECKLIST =
            "Deprecate old versions after publishing the API:true, Requires re-subscription when publishing the " +
                    "API:false";
    private static final String EXPECTED_INIT_RESPONSE = "{\n" +
            "  \"jsonrpc\" : \"2.0\",\n" +
            "  \"id\" : 0,\n" +
            "  \"result\" : {\n" +
            "    \"protocolVersion\" : \"2025-06-18\",\n" +
            "    \"capabilities\" : {\n" +
            "      \"tools\" : {\n" +
            "        \"listChanged\" : false\n" +
            "      }\n" +
            "    },\n" +
            "    \"serverInfo\" : {\n" +
            "      \"name\" : \"PetstoreMCPFromAPI\",\n" +
            "      \"version\" : \"1.0.0\",\n" +
            "      \"description\" : \"This is an MCP Server\"\n" +
            "    }\n" +
            "  }\n" +
            "}\n";
    private static final String EXPECTED_TOOL_LIST_RESPONSE = "{\n" +
            "  \"jsonrpc\" : \"2.0\",\n" +
            "  \"id\" : 0,\n" +
            "  \"result\" : {\n" +
            "    \"tools\" : [ {\n" +
            "      \"name\" : \"get_pets\",\n" +
            "      \"description\" : \"This description has been updated\",\n" +
            "      \"inputSchema\" : {\n" +
            "        \"type\" : \"object\",\n" +
            "        \"properties\" : { },\n" +
            "        \"required\" : [ ]\n" +
            "      }\n" +
            "    }, {\n" +
            "      \"name\" : \"delete_oldpets\",\n" +
            "      \"description\" : \"Delete all old pets\",\n" +
            "      \"inputSchema\" : {\n" +
            "        \"type\" : \"object\",\n" +
            "        \"properties\" : { },\n" +
            "        \"required\" : [ ]\n" +
            "      }\n" +
            "    } ]\n" +
            "  }\n" +
            "}";

    private static final String EXPECTED_TOOL_CALL_RESPONSE = "[ {\n" +
            "  \"id\" : \"1\",\n" +
            "  \"name\" : \"Fido\",\n" +
            "  \"tag\" : \"dog\"\n" +
            "}, {\n" +
            "  \"id\" : \"2\",\n" +
            "  \"name\" : \"Whiskers\",\n" +
            "  \"tag\" : \"cat\"\n" +
            "} ]";

    private static String INIT_REQUEST_PAYLOAD =
            "{\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-06-18\"," +
                    "\"capabilities\":{\"sampling\":{},\"roots\":{\"listChanged\":true}}," +
                    "\"clientInfo\":{\"name\":\"mcp-playground\",\"version\":\"0.13.0\"}},\"jsonrpc\":\"2.0\"," +
                    "\"id\":0}";

    private static String TOOL_LIST_REQUEST_PAYLOAD =
            "{\"method\":\"tools/list\",\"params\":{},\"jsonrpc\":\"2.0\",\"id\":0}";

    private static String TOOL_CALL_REQUEST_PAYLOAD =
            "{\"method\":\"tools/call\",\"params\":{\"name\":\"get_pets\",\"arguments\":{}," +
                    "\"_meta\":{\"progressToken\":2}},\"jsonrpc\":\"2.0\",\"id\":1}";

    private static String ECHO_TOOL_CALL_REQUEST_PAYLOAD =
            "{\"method\":\"tools/call\",\"params\":{\"name\":\"get_pets\",\"arguments\":{}," +
                    "\"_meta\":{\"progressToken\":2}},\"jsonrpc\":\"2.0\",\"id\":1}";

    private static String SCOPE = "mcp-scope";
    private static String ADMIN_ROLE = "admin";

    private static String tenantDomain;

    private String mcpTestResourcePath;
    private String mcpServerFromOpenApiId;
    private String mcpServerFromApiId;
    private String mcpServerProxyId;
    private String apiId;
    private String applicationId1;
    private String applicationId2;
    private String applicationId3;
    private int port;
    private MCPWireMock mcpWireMock;

    private SynapseConfigAdminClient synapseConfigAdminClient;

    @Factory(dataProvider = "userModeDataProvider")
    public MCPServerTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{{TestUserMode.SUPER_TENANT_ADMIN}, {TestUserMode.TENANT_ADMIN}};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);

        String gatewaySessionCookie = createSession(gatewayContextMgt);
        String petstoreApiSeq =
                TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator + "AM" +
                        File.separator + "synapseconfigs" + File.separator + "rest" + File.separator +
                        "petstore-api.xml";
        String petStoreApiSeqStr = readFile(petstoreApiSeq);
        synapseConfigAdminClient =
                new SynapseConfigAdminClient(gatewayContextWrk.getContextUrls().getBackEndUrl(), gatewaySessionCookie);

        synapseConfigAdminClient.updateConfiguration(petStoreApiSeqStr);

        mcpTestResourcePath = TestConfigurationProvider.getResourceLocation() + "mcp" + File.separator;
        tenantDomain = publisherContext.getContextTenant().getDomain();
        port = findAvailablePort(PORT_RANGE_START, PORT_RANGE_END_INCLUSIVE);
        mcpWireMock = new MCPWireMock();
        mcpWireMock.start(port);
        log.info("Starting MCPWireMock on port " + port);
        log.info("Initialized test environment for user mode: " + userMode);
    }

    @Test(groups = {GROUP_WSO2_AM}, description = "Create MCP server using OpenAPI definition")
    public void createMCPServerUsingOpenAPIDefinition() throws Exception {

        final File openApiDefFile = resolveExistingFile(mcpTestResourcePath, OAS_FILE);
        final JSONObject additionalPropsObj =
                new JSONObject(readFileContent(mcpTestResourcePath, ADDITIONAL_PROPS_MCP_FILE));
        final MCPServerDTO mcpServerResponse =
                restAPIPublisher.createMCPServerFromOpenAPI(openApiDefFile, additionalPropsObj.toString());
        Assert.assertNotNull(mcpServerResponse, "MCP Server response is null (OpenAPI flow)");
        Assert.assertNotNull(mcpServerResponse.getId(), "MCP Server ID is null (OpenAPI flow)");
        assertNonEmpty(mcpServerResponse.getOperations(), "No operations found in MCP Server (OpenAPI flow)");
        assertTargets(mcpServerResponse.getOperations(),
                listOf(TARGET_GET_PETS, TARGET_GET_PETS_BY_ID, TARGET_DELETE_OLD_PETS),
                "OpenAPI flow: Missing expected operations");

        final MCPServerOperationDTO getByIdOp =
                getOperationByTarget(mcpServerResponse.getOperations(), TARGET_GET_PETS_BY_ID);
        Assert.assertEquals(compactJson(getByIdOp.getSchemaDefinition()), compactJson(EXPECTED_SCHEMA_GET_PETS_BY_ID),
                mismatch("Schema definition", TARGET_GET_PETS_BY_ID, EXPECTED_SCHEMA_GET_PETS_BY_ID,
                        getByIdOp.getSchemaDefinition()));
        Assert.assertEquals(getByIdOp.getDescription(), DESC_GET_PET_BY_ID,
                mismatch("Description", TARGET_GET_PETS_BY_ID, DESC_GET_PET_BY_ID, getByIdOp.getDescription()));

        mcpServerFromOpenApiId = mcpServerResponse.getId();
        assertHttpOk(restAPIPublisher.getMCPServer(mcpServerFromOpenApiId),
                "Fetching created MCP Server (OpenAPI flow) failed");
    }

    @Test(groups = {GROUP_WSO2_AM}, description = "Validate and update MCP Server backends",
            dependsOnMethods = {"createMCPServerUsingOpenAPIDefinition"})
    public void testMCPServerBackends() throws Exception {

        final MCPServerDTO mcpServer = fetchMCPServer(mcpServerFromOpenApiId);
        assertNonEmpty(mcpServer.getOperations(), "No operations found in MCP Server");

        final MCPServerOperationDTO firstOp = mcpServer.getOperations().get(0);
        Assert.assertNotNull(firstOp.getBackendOperationMapping(),
                "Backend operation mapping is null for operation: " + firstOp);

        final String backendId = firstOp.getBackendOperationMapping().getBackendId();
        final BackendDTO backend = restAPIPublisher.getMCPServerBackend(mcpServerFromOpenApiId, backendId);
        Assert.assertNotNull(backend, "MCP Server backend is null");
        Assert.assertNotNull(backend.getEndpointConfig(), "MCP Server backend EndpointConfig is null");
        Assert.assertFalse(StringUtils.isBlank(backend.getEndpointConfig().toString()),
                "MCP Server backend EndpointConfig is empty");
        Assert.assertEquals(compactJson(backend.getEndpointConfig().toString()), compactJson(EXPECTED_ENDPOINT_CONFIG),
                "Expected endpoint config does not match");

//        backend.setEndpointConfig(UPDATED_ENDPOINT_CONFIG);
//        final BackendDTO updatedBackend =
//                restAPIPublisher.updateMCPServerBackend(mcpServerFromOpenApiId, backendId, backend);
//        Assert.assertNotNull(updatedBackend, "Updated MCP Server backend is null");
//        Assert.assertNotNull(updatedBackend.getEndpointConfig(), "Updated MCP Server backend EndpointConfig is null");
//        Assert.assertEquals(compactJson(updatedBackend.getEndpointConfig().toString()),
//                compactJson(EXPECTED_UPDATED_ENDPOINT_CONFIG), "Updated endpoint config does not match");
    }

    @Test(groups = {GROUP_WSO2_AM}, description = "Create, deploy and validate MCP Server revision",
            dependsOnMethods = {"testMCPServerBackends"})
    public void testMCPServerRevisionDeploymentForDirectBackendSubtype() throws Exception {

        final APIRevisionDTO revisionReq = new APIRevisionDTO();
        revisionReq.setDescription(REVISION_DESCRIPTION_1);
        final APIRevisionDTO revision = restAPIPublisher.addMCPServerRevision(mcpServerFromOpenApiId, revisionReq);
        Assert.assertNotNull(revision, "MCP Server revision creation failed");

        final APIRevisionListDTO revisions = restAPIPublisher.getMCPServerRevisions(mcpServerFromOpenApiId);
        Assert.assertNotNull(revisions, "MCP Server revisions fetch failed");
        assertNonEmpty(revisions.getList(), "No revisions found in MCP Server");

        final APIRevisionDTO createdRevision = revisions.getList().get(0);
        Assert.assertEquals(createdRevision.getApiInfo().getId(), mcpServerFromOpenApiId,
                "MCP Server revision - MCP Server ID mismatch");
        Assert.assertEquals(createdRevision.getDescription(), revisionReq.getDescription(),
                "MCP Server revision description mismatch");

        final APIRevisionDeploymentDTO deployment = new APIRevisionDeploymentDTO();
        deployment.setName(REVISION_NAME_DEFAULT);
        deployment.setVhost(REVISION_VHOST_LOCALHOST);
        deployment.setDisplayOnDevportal(true);

        final List<APIRevisionDeploymentDTO> deployments =
                restAPIPublisher.deployMCPServerRevision(mcpServerFromOpenApiId, createdRevision.getId(),
                        Collections.singletonList(deployment));
        assertNonEmpty(deployments, "MCP Server revision deployment failed. Deployment list is empty");
        Assert.assertEquals(String.valueOf(deployments.get(0).getStatus()), DEPLOYMENT_STATUS_APPROVED,
                "Deployment status is not 'APPROVED'");

        waitForAPIDeploymentSync(tenantDomain, SERVER_NAME_PETSTORE_OPENAPI, SERVER_VERSION_1,
                APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(groups = {GROUP_WSO2_AM}, description = "Publish, subscribe and validate visibility",
            dependsOnMethods = {"testMCPServerRevisionDeploymentForDirectBackendSubtype"})
    public void testMCPServerSubscribeAndInvokeForDirectBackendSubtype() throws Exception {

        final WorkflowResponseDTO workflowResponse =
                restAPIPublisher.changeMCPServerLifecycle(LIFECYCLE_ACTION_PUBLISH, mcpServerFromOpenApiId,
                        LIFECYCLE_CHECKLIST);
        Assert.assertNotNull(workflowResponse, "MCP Server lifecycle change failed");
        Assert.assertEquals(workflowResponse.getLifecycleState().getState(), LIFECYCLE_STATE_PUBLISHED,
                "MCP Server lifecycle state mismatch.");

        Thread.sleep(POST_PUBLISH_SETTLE_WAIT.toMillis());

        final List<APIIdentifier> publisherAPIList = APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                restAPIStore.getAllMCPServers(10, 0, tenantDomain, null));
        Assert.assertTrue(isMCPServerAvailableInList(SERVER_NAME_PETSTORE_OPENAPI, publisherAPIList),
                DEVPORTAL_VISIBILITY_ERROR);

        final HttpResponse applicationResponse = restAPIStore.createApplication(APP_NAME_1, APP_DESC,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, TokenTypeEnum.JWT);
        applicationId2 = applicationResponse.getData();
        final HttpResponse subscribeResponse = restAPIStore.createSubscription(mcpServerFromOpenApiId, applicationId2,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        Assert.assertEquals(subscribeResponse.getResponseCode(), HttpStatus.SC_OK, "MCP Server subscription failed");
        Assert.assertTrue(StringUtils.isNotEmpty(subscribeResponse.getData()),
                "MCP Server subscription failed. Subscription data is missing");

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId2, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        requestHeaders.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        requestHeaders.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        String petstoreBackendURL = getAPIInvocationURLHttps(
                SERVER_CONTEXT_PETSTORE_OPENAPI + "/" + SERVER_VERSION_1 + "/mcp");
        HttpResponse initResponse =
                HTTPSClientUtils.doPost(petstoreBackendURL, requestHeaders, INIT_REQUEST_PAYLOAD);
        Assert.assertEquals(initResponse.getResponseCode(), 200, "MCP Server init call failed");
        Assert.assertEquals(compactJson(initResponse.getData()), compactJson(EXPECTED_INIT_RESPONSE),
                "MCP Server init response mismatch");

        HttpResponse toolListResponse =
                HTTPSClientUtils.doPost(petstoreBackendURL, requestHeaders, TOOL_LIST_REQUEST_PAYLOAD);
        Assert.assertEquals(toolListResponse.getResponseCode(), 200, "Tool list call failed");
        Assert.assertEquals(compactJson(toolListResponse.getData()), compactJson(EXPECTED_TOOL_LIST_RESPONSE),
                "MCP Server tool list response mismatch");

        HttpResponse toolCallResponse =
                HTTPSClientUtils.doPost(petstoreBackendURL, requestHeaders, TOOL_CALL_REQUEST_PAYLOAD);
        Assert.assertEquals(toolCallResponse.getResponseCode(), 200, "Tool call failed");
        Assert.assertEquals(compactJson(toolCallResponse.getData()), compactJson(EXPECTED_TOOL_CALL_RESPONSE),
                "MCP Server tool call response mismatch");
    }

    @Test(groups = {GROUP_WSO2_AM}, description = "Create MCP server using an API")
    public void createMCPServerUsingAPI() throws Exception {

        final File openApiDefFile = new File(mcpTestResourcePath + OAS_FILE);
        Assert.assertTrue(openApiDefFile.exists(),
                "OpenAPI definition file not found: " + openApiDefFile.getAbsolutePath());

        final String additionalPropsContent = readFileContent(mcpTestResourcePath, ADDITIONAL_PROPS_API_FILE);
        final JSONObject additionalPropsObj = new JSONObject(additionalPropsContent);

        final JSONObject endpointConfig = new JSONObject();
        final JSONObject endpoints = new JSONObject().put(URL_KEY, getGatewayURLNhttps() + PETSTORE_BACKEND_CONTEXT);
        endpointConfig.put(PROD_ENDPOINTS, endpoints);
        endpointConfig.put(SANDBOX_ENDPOINTS, endpoints);
        endpointConfig.put(ENDPOINT_TYPE, "http");
        additionalPropsObj.put(ENDPOINT_CONFIG, endpointConfig);
        final APIDTO apiDto = restAPIPublisher.importOASDefinition(openApiDefFile, additionalPropsObj.toString());
        Assert.assertNotNull(apiDto, "API import response is null");

        apiId = apiDto.getId();
        Assert.assertNotNull(apiId, "Imported API ID is null");
        assertHttpOk(restAPIPublisher.getAPI(apiId), "API creation failed");

        String apiRevisionUuid = createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        Assert.assertNotNull(apiRevisionUuid, "API revision deployment failed. API Revision UUID is null");

        final MCPServerDTO request = new MCPServerDTO();
        request.setName(SERVER_NAME_PETSTORE_API);
        request.setContext(SERVER_CONTEXT_PETSTORE_API);
        request.setVersion(SERVER_VERSION_1);
        request.setOperations(Arrays.asList(buildToolOpForApi(apiId, PATH_PETS, BackendOperationDTO.VerbEnum.GET),
                buildToolOpForApi(apiId, PATH_PETS_BY_ID, BackendOperationDTO.VerbEnum.GET)));
        request.setPolicies(Collections.singletonList("Unlimited"));

        final MCPServerDTO response = restAPIPublisher.createMCPServerFromAPI(request);
        Assert.assertNotNull(response, "MCP Server response is null (API flow)");
        assertNonEmpty(response.getOperations(), "No operations found in MCP Server (API flow)");
        assertTargets(response.getOperations(), listOf(TARGET_GET_PETS, TARGET_GET_PETS_BY_ID),
                "API flow: Missing expected operations");

        final MCPServerOperationDTO getByIdOp = getOperationByTarget(response.getOperations(), TARGET_GET_PETS_BY_ID);
        Assert.assertEquals(compactJson(getByIdOp.getSchemaDefinition()), compactJson(EXPECTED_SCHEMA_GET_PETS_BY_ID),
                mismatch("Schema definition", TARGET_GET_PETS_BY_ID, EXPECTED_SCHEMA_GET_PETS_BY_ID,
                        getByIdOp.getSchemaDefinition()));
        Assert.assertEquals(getByIdOp.getDescription(), DESC_GET_PET_BY_ID,
                mismatch("Description", TARGET_GET_PETS_BY_ID, DESC_GET_PET_BY_ID, getByIdOp.getDescription()));

        mcpServerFromApiId = response.getId();
        assertHttpOk(restAPIPublisher.getMCPServer(mcpServerFromApiId),
                "Fetching created MCP Server (API flow) failed");
    }

    @Test(groups = {GROUP_WSO2_AM}, description = "Update tool operations in a MCP Server",
            dependsOnMethods = {"createMCPServerUsingAPI"})
    public void testMCPServerToolOperations() throws Exception {

        final MCPServerDTO mcpServer = fetchMCPServer(mcpServerFromApiId);
        assertNonEmpty(mcpServer.getOperations(), "MCP Server operations list is empty");

        final MCPServerOperationDTO updateOp = new MCPServerOperationDTO();
        copyOperation(mcpServer.getOperations().get(0), updateOp);
        updateOp.setDescription(DESC_UPDATE_GET_PETS);

        final MCPServerOperationDTO addOp =
                buildToolOpForApi(apiId, PATH_OLD_PETS, BackendOperationDTO.VerbEnum.DELETE);

        mcpServer.setOperations(Arrays.asList(addOp, updateOp));

        final MCPServerDTO updated = restAPIPublisher.updateMCPServer(mcpServerFromApiId, mcpServer);
        assertTargets(updated.getOperations(), listOf(TARGET_GET_PETS, TARGET_DELETE_OLD_PETS),
                "API flow: Expected operations do not match after update");

        final MCPServerOperationDTO deleteOldPets =
                getOperationByTarget(updated.getOperations(), TARGET_DELETE_OLD_PETS);
        Assert.assertEquals(compactJson(deleteOldPets.getSchemaDefinition()),
                compactJson(EXPECTED_SCHEMA_DELETE_OLD_PETS),
                mismatch("Schema definition", TARGET_DELETE_OLD_PETS, EXPECTED_SCHEMA_DELETE_OLD_PETS,
                        deleteOldPets.getSchemaDefinition()));
        Assert.assertEquals(deleteOldPets.getDescription(), DESC_DELETE_OLD_PETS,
                mismatch("Description", TARGET_DELETE_OLD_PETS, DESC_DELETE_OLD_PETS, deleteOldPets.getDescription()));

        final MCPServerOperationDTO getPetsOp = getOperationByTarget(updated.getOperations(), TARGET_GET_PETS);
        Assert.assertEquals(getPetsOp.getDescription(), DESC_UPDATE_GET_PETS,
                mismatch("Description", TARGET_GET_PETS, DESC_UPDATE_GET_PETS, getPetsOp.getDescription()));
    }

    @Test(groups = {GROUP_WSO2_AM}, description = "Create, deploy and validate MCP Server revision",
            dependsOnMethods = {"testMCPServerToolOperations"})
    public void testMCPServerRevisionDeploymentForExistingApiSubtype() throws Exception {

        final APIRevisionDTO revisionReq = new APIRevisionDTO();
        revisionReq.setDescription(REVISION_DESCRIPTION_1);
        final APIRevisionDTO revision = restAPIPublisher.addMCPServerRevision(mcpServerFromApiId, revisionReq);
        Assert.assertNotNull(revision, "MCP Server revision creation failed");

        final APIRevisionListDTO revisions = restAPIPublisher.getMCPServerRevisions(mcpServerFromApiId);
        Assert.assertNotNull(revisions, "MCP Server revisions fetch failed");
        assertNonEmpty(revisions.getList(), "No revisions found in MCP Server");

        final APIRevisionDTO createdRevision = revisions.getList().get(0);
        Assert.assertEquals(createdRevision.getApiInfo().getId(), mcpServerFromApiId,
                "MCP Server revision - MCP Server ID mismatch");
        Assert.assertEquals(createdRevision.getDescription(), revisionReq.getDescription(),
                "MCP Server revision description mismatch");

        final APIRevisionDeploymentDTO deployment = new APIRevisionDeploymentDTO();
        deployment.setName(REVISION_NAME_DEFAULT);
        deployment.setVhost(REVISION_VHOST_LOCALHOST);
        deployment.setDisplayOnDevportal(true);

        final List<APIRevisionDeploymentDTO> deployments =
                restAPIPublisher.deployMCPServerRevision(mcpServerFromApiId, createdRevision.getId(),
                        Collections.singletonList(deployment));
        assertNonEmpty(deployments, "MCP Server revision deployment failed. Deployment list is empty");
        Assert.assertEquals(String.valueOf(deployments.get(0).getStatus()), DEPLOYMENT_STATUS_APPROVED,
                "Deployment status is not 'APPROVED'");

        waitForAPIDeploymentSync(tenantDomain, SERVER_NAME_PETSTORE_API, SERVER_VERSION_1,
                APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(groups = {GROUP_WSO2_AM}, description = "Publish, subscribe and validate visibility",
            dependsOnMethods = {"testMCPServerRevisionDeploymentForExistingApiSubtype"})
    public void testMCPServerSubscribeAndInvokeForExistingApiSubtype() throws Exception {

        final HttpResponse applicationResponse = restAPIStore.createApplication(APP_NAME_2, APP_DESC,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, TokenTypeEnum.JWT);
        applicationId1 = applicationResponse.getData();
        final HttpResponse subscribeResponse = restAPIStore.createSubscription(mcpServerFromApiId, applicationId1,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        Assert.assertEquals(subscribeResponse.getResponseCode(), HttpStatus.SC_OK, "MCP Server subscription failed");
        Assert.assertTrue(StringUtils.isNotEmpty(subscribeResponse.getData()),
                "MCP Server subscription failed. Subscription data is missing");

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId1, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        requestHeaders.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        requestHeaders.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        String petstoreBackendURL = getAPIInvocationURLHttps(
                SERVER_CONTEXT_PETSTORE_API + "/" + SERVER_VERSION_1 + "/mcp");

        HttpResponse toolCallResponse =
                HTTPSClientUtils.doPost(petstoreBackendURL, requestHeaders, TOOL_CALL_REQUEST_PAYLOAD);
        Assert.assertEquals(toolCallResponse.getResponseCode(), 200, "Tool call failed");
        Assert.assertEquals(compactJson(toolCallResponse.getData()), compactJson(EXPECTED_TOOL_CALL_RESPONSE),
                "MCP Server tool call response mismatch");
    }

    @Test(groups = {GROUP_WSO2_AM}, description = "Create MCP server using a third-party MCP Server (proxy)")
    public void createMCPServerUsingThirdPartyMCPServer() throws Exception {

        final MCPServerDTO mcpServerRequest = new MCPServerDTO();
        mcpServerRequest.setName(SERVER_NAME_EVERYTHING);
        mcpServerRequest.setDisplayName(SERVER_DISPLAY_EVERYTHING);
        mcpServerRequest.setVersion(SERVER_VERSION_1);
        mcpServerRequest.setContext(SERVER_CONTEXT_EVERYTHING);
        mcpServerRequest.setOperations(Arrays.asList(buildToolOpForThirdParty(TARGET_ECHO),
                buildToolOpForThirdParty(TARGET_ADD), buildToolOpForThirdParty(TARGET_VIEW_MENU)));

        final MCPServerProxyRequestDTO proxyRequest = new MCPServerProxyRequestDTO();
        proxyRequest.setUrl(getGatewayURLNhttps() + MCP_SERVER_BACKEND_CONTEXT);
        final SecurityInfoDTO securityInfoDTO = new SecurityInfoDTO();
        securityInfoDTO.setIsSecure(false);
        proxyRequest.setSecurityInfo(securityInfoDTO);
        proxyRequest.setAdditionalProperties(mcpServerRequest);

        final MCPServerDTO mcpServerResponse = restAPIPublisher.createMCPServerProxy(proxyRequest);
        Assert.assertNotNull(mcpServerResponse, "MCP Server response is null (third-party flow)");
        assertNonEmpty(mcpServerResponse.getOperations(), "No operations found in MCP Server (third-party flow)");
        assertTargets(mcpServerResponse.getOperations(), listOf(TARGET_ECHO, TARGET_ADD, TARGET_VIEW_MENU),
                "Third-party flow: Missing expected operations");

        final MCPServerOperationDTO echoOp = getOperationByTarget(mcpServerResponse.getOperations(), TARGET_ECHO);
        Assert.assertEquals(compactJson(echoOp.getSchemaDefinition()), compactJson(EXPECTED_SCHEMA_ECHO),
                mismatch("Schema definition", TARGET_ECHO, EXPECTED_SCHEMA_ECHO, echoOp.getSchemaDefinition()));
        Assert.assertEquals(echoOp.getDescription(), DESC_GET_ECHO,
                mismatch("Description", TARGET_ECHO, DESC_GET_ECHO, echoOp.getDescription()));

        mcpServerProxyId = mcpServerResponse.getId();
        assertHttpOk(restAPIPublisher.getMCPServer(mcpServerProxyId),
                "Fetching created MCP Server (third-party flow) failed");
    }

    @Test(groups = {GROUP_WSO2_AM}, description = "Create MCP server using a third-party MCP Server (proxy)",
            dependsOnMethods = {"createMCPServerUsingThirdPartyMCPServer"})
    public void testMCPServerScopes() throws Exception {

        final MCPServerDTO mcpServer = fetchMCPServer(mcpServerProxyId);
        assertNonEmpty(mcpServer.getOperations(), "MCP Server operations list is empty");

        final MCPServerOperationDTO updateOp = new MCPServerOperationDTO();
        copyOperation(mcpServer.getOperations().get(0), updateOp);

        List role = new ArrayList();
        role.add(ADMIN_ROLE);

        ScopeDTO scopeObject = new ScopeDTO();
        scopeObject.setName(SCOPE);
        scopeObject.setBindings(role);
        APIScopeDTO apiScope1DTO = new APIScopeDTO();
        apiScope1DTO.setScope(scopeObject);

        List apiScopeList = new ArrayList();
        apiScopeList.add(apiScope1DTO);
        mcpServer.setScopes(apiScopeList);

        ArrayList<String> scopes = new ArrayList<>();
        scopes.add(SCOPE);
        updateOp.setScopes(scopes);
        mcpServer.getOperations().set(0, updateOp);

        final MCPServerDTO mcpServerResponse = restAPIPublisher.updateMCPServer(mcpServerProxyId, mcpServer);
        Assert.assertNotNull(mcpServerResponse, "MCP Server response is null (third-party flow)");
        final MCPServerOperationDTO echoOp = getOperationByTarget(mcpServerResponse.getOperations(), TARGET_ECHO);
        Assert.assertNotNull(echoOp, "Scopes are not updated properly");
        Assert.assertEquals(echoOp.getScopes(), scopes, "Scopes are not updated properly");

        final APIRevisionDTO revisionReq = new APIRevisionDTO();
        revisionReq.setDescription(REVISION_DESCRIPTION_1);
        final APIRevisionDTO revision = restAPIPublisher.addMCPServerRevision(mcpServerProxyId, revisionReq);
        Assert.assertNotNull(revision, "MCP Server revision creation failed");

        final APIRevisionListDTO revisions = restAPIPublisher.getMCPServerRevisions(mcpServerProxyId);
        Assert.assertNotNull(revisions, "MCP Server revisions fetch failed");
        assertNonEmpty(revisions.getList(), "No revisions found in MCP Server");

        final APIRevisionDTO createdRevision = revisions.getList().get(0);
        Assert.assertEquals(createdRevision.getApiInfo().getId(), mcpServerProxyId,
                "MCP Server revision - MCP Server ID mismatch");

        final APIRevisionDeploymentDTO deployment = new APIRevisionDeploymentDTO();
        deployment.setName(REVISION_NAME_DEFAULT);
        deployment.setVhost(REVISION_VHOST_LOCALHOST);
        deployment.setDisplayOnDevportal(true);

        final List<APIRevisionDeploymentDTO> deployments =
                restAPIPublisher.deployMCPServerRevision(mcpServerProxyId, createdRevision.getId(),
                        Collections.singletonList(deployment));
        assertNonEmpty(deployments, "MCP Server revision deployment failed. Deployment list is empty");
        Assert.assertEquals(String.valueOf(deployments.get(0).getStatus()), DEPLOYMENT_STATUS_APPROVED,
                "Deployment status is not 'APPROVED'");

        waitForAPIDeploymentSync(tenantDomain, SERVER_NAME_EVERYTHING, SERVER_VERSION_1,
                APIMIntegrationConstants.IS_API_EXISTS);

        final HttpResponse applicationResponse = restAPIStore.createApplication(APP_NAME_3, APP_DESC,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, TokenTypeEnum.JWT);
        applicationId3 = applicationResponse.getData();
        final HttpResponse subscribeResponse = restAPIStore.createSubscription(mcpServerProxyId, applicationId3,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        Assert.assertEquals(subscribeResponse.getResponseCode(), HttpStatus.SC_OK, "MCP Server subscription failed");
        Assert.assertTrue(StringUtils.isNotEmpty(subscribeResponse.getData()),
                "MCP Server subscription failed. Subscription data is missing");

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId3, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, scopes, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        requestHeaders.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        requestHeaders.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        String petstoreBackendURL = getAPIInvocationURLHttps(
                SERVER_CONTEXT_EVERYTHING + "/" + SERVER_VERSION_1 + "/mcp");

        HttpResponse toolCallResponse =
                HTTPSClientUtils.doPost(petstoreBackendURL, requestHeaders, ECHO_TOOL_CALL_REQUEST_PAYLOAD);
        Assert.assertEquals(toolCallResponse.getResponseCode(), 200, "Tool call failed");
        Assert.assertEquals(compactJson(toolCallResponse.getData()), compactJson(EXPECTED_TOOL_CALL_RESPONSE),
                "MCP Server tool call response mismatch");
    }

    /**
     * Returns true if the given MCP Server name is present in the provided identifier list.
     *
     * @param mcpServerName    MCP Server name to look for
     * @param publisherAPIList List of identifiers returned from the portal
     * @return true if present; false otherwise
     */
    private boolean isMCPServerAvailableInList(String mcpServerName, List<APIIdentifier> publisherAPIList) {

        for (APIIdentifier apiIdentifier : publisherAPIList) {
            if (StringUtils.equals(apiIdentifier.getApiName(), mcpServerName)) {
                log.info("MCP Server is visible in Developer portal.");
                return true;
            }
        }
        return false;
    }

    /**
     * Build a TOOL-feature operation referencing an existing API's backend operation.
     *
     * @param apiId  API identifier
     * @param target backend target path
     * @param verb   HTTP verb
     * @return the operation DTO
     */
    private static MCPServerOperationDTO buildToolOpForApi(String apiId, String target,
                                                           BackendOperationDTO.VerbEnum verb) {

        final MCPServerOperationDTO op = new MCPServerOperationDTO();
        op.setFeature(MCPServerOperationDTO.FeatureEnum.TOOL);

        final APIOperationMappingDTO mapping = new APIOperationMappingDTO();
        mapping.setApiId(apiId);

        final BackendOperationDTO backendOperation = new BackendOperationDTO();
        backendOperation.setTarget(target);
        backendOperation.setVerb(verb);
        mapping.setBackendOperation(backendOperation);

        op.setApiOperationMapping(mapping);
        return op;
    }

    /**
     * Build a TOOL-feature operation mapped to a third-party MCP backend tool target.
     *
     * @param target tool target identifier
     * @return the operation DTO
     */
    private static MCPServerOperationDTO buildToolOpForThirdParty(String target) {

        final MCPServerOperationDTO op = new MCPServerOperationDTO();
        op.setFeature(MCPServerOperationDTO.FeatureEnum.TOOL);

        final BackendOperationMappingDTO backendMapping = new BackendOperationMappingDTO();
        final BackendOperationDTO backendOperation = new BackendOperationDTO();
        backendOperation.setVerb(BackendOperationDTO.VerbEnum.TOOL);
        backendOperation.setTarget(target);
        backendMapping.setBackendOperation(backendOperation);

        op.backendOperationMapping(backendMapping);
        return op;
    }

    /**
     * Retrieve an operation by its target.
     *
     * @param operations list of operations
     * @param target     expected target
     * @return operation if found, otherwise null
     */
    private static MCPServerOperationDTO getOperationByTarget(List<MCPServerOperationDTO> operations, String target) {

        if (operations == null) {
            return null;
        }
        for (MCPServerOperationDTO op : operations) {
            if (op != null && target.equals(op.getTarget())) {
                return op;
            }
        }
        return null;
    }

    /**
     * Assert that the given operations contain exactly the provided targets, with no missing, unexpected, or
     * duplicated targets.
     * Produces a detailed diagnostic message for easier debugging.
     *
     * @param operations      operations to inspect
     * @param expectedTargets expected targets
     * @param messagePrefix   a short prefix to identify the assertion context
     */
    private static void assertTargets(List<MCPServerOperationDTO> operations, Collection<String> expectedTargets,
                                      String messagePrefix) {

        Map<String, Long> counts = Optional.ofNullable(operations).orElse(Collections.emptyList()).stream()
                .map(op -> op == null ? null : op.getTarget())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        long nullCount = counts.getOrDefault(null, 0L);
        if (nullCount > 0) {
            Assert.fail(messagePrefix + ": Found " + nullCount + " operation(s) with null target");
        }
        counts.remove(null);

        Set<String> received = counts.keySet();
        Set<String> expected = new HashSet<>(expectedTargets);

        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(received);

        Set<String> unexpected = new HashSet<>(received);
        unexpected.removeAll(expected);

        List<String> duplicates = counts.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue() > 1)
                .map(e -> e.getKey() + " x" + e.getValue())
                .collect(Collectors.toList());

        String diag = messagePrefix +
                ": missing=" + missing +
                ", unexpected=" + unexpected +
                ", duplicates=" + duplicates +
                ", received=" + received;

        Assert.assertTrue(missing.isEmpty() && unexpected.isEmpty() && duplicates.isEmpty(), diag);
    }

    /**
     * Assert HTTP 200 OK from a REST call.
     *
     * @param response       HTTP response
     * @param failureMessage message on failure
     */
    private static void assertHttpOk(HttpResponse response, String failureMessage) {

        Assert.assertNotNull(response, failureMessage + ": null response");
        Assert.assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                failureMessage + ": HTTP " + response.getResponseCode() + ", payload=" + response.getData());
    }

    /**
     * Read a UTF-8 text file from a base directory.
     *
     * @param baseDir  base directory
     * @param fileName file name
     * @return file content as string
     * @throws IOException if file read fails or file is missing
     */
    private static String readFileContent(String baseDir, String fileName) throws IOException {

        Path path = Paths.get(baseDir, fileName);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + path.toAbsolutePath());
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /**
     * Attempts to bind to the first free port within [start, end].
     *
     * @param start        inclusive start of the port range
     * @param endInclusive inclusive end of the port range
     * @return a free port in the provided range
     * @throws IllegalStateException if no free port found
     */
    private static int findAvailablePort(int start, int endInclusive) {

        for (int p = start; p <= endInclusive; p++) {
            try (ServerSocket ss = new ServerSocket()) {
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress("localhost", p));
                return p;
            } catch (IOException ignore) {
            }
        }
        throw new IllegalStateException("No free port found in range " + start + "-" + endInclusive);
    }

    /**
     * Convenience to create an immutable list.
     */
    private static List<String> listOf(String... items) {

        return Arrays.asList(items);
    }

    /**
     * Ensures a list is non-null and non-empty.
     */
    private static <T> void assertNonEmpty(List<T> list, String message) {

        Assert.assertNotNull(list, message + " (list is null)");
        Assert.assertFalse(list.isEmpty(), message);
    }

    /**
     * Fetches an MCP Server by ID and parses into DTO.
     *
     * @param mcpServerId MCP Server ID
     * @return the parsed {@link MCPServerDTO}
     */
    private MCPServerDTO fetchMCPServer(String mcpServerId) {

        final HttpResponse response = restAPIPublisher.getMCPServer(mcpServerId);
        assertHttpOk(response, "Fetching MCP Server failed");
        return new Gson().fromJson(response.getData(), MCPServerDTO.class);
    }

    /**
     * Creates a structured mismatch message for assertions.
     */
    private static String mismatch(String object, String target, String expected, String actual) {

        return String.format("%s mismatch for operation '%s'. Expected: %s, Actual: %s", object, target, expected,
                actual);
    }

    /**
     * Copies minimal fields from source operation to destination for update scenarios to avoid accidental mutation
     * of the original list object reference returned by API calls.
     *
     * @param src source operation
     * @param dst destination operation (will be mutated)
     */
    private static void copyOperation(MCPServerOperationDTO src, MCPServerOperationDTO dst) {

        if (src == null || dst == null) return;
        dst.setFeature(src.getFeature());
        dst.setApiOperationMapping(src.getApiOperationMapping());
        dst.setBackendOperationMapping(src.getBackendOperationMapping());
        dst.setTarget(src.getTarget());
        dst.setSchemaDefinition(src.getSchemaDefinition());
        dst.setDescription(src.getDescription());
    }

    /**
     * Resolves a file under {@code baseDir} and asserts it exists.
     */
    private static File resolveExistingFile(String baseDir, String fileName) {

        File file = new File(baseDir, fileName);
        Assert.assertTrue(file.exists(), "File not found: " + file.getAbsolutePath());
        return file;
    }

    /**
     * Compacts JSON by removing unnecessary whitespace.
     * If input is not valid JSON, trims and collapses internal whitespace to single spaces.
     */
    private static String compactJson(String src) throws JSONException {

        if (src == null) return null;
        String trimmed = src.trim();
        if (trimmed.startsWith("{")) {
            return new JSONObject(trimmed).toString();
        } else if (trimmed.startsWith("[")) {
            return new JSONArray(trimmed).toString();
        }
        return trimmed.replaceAll("\\s+", " ");
    }

    /**
     * Cleans up created entities.
     **/
    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        mcpWireMock.stop();
        log.info("Cleaning up artifacts");
        SubscriptionListDTO subs1DTO = restAPIStore.getAllSubscriptionsOfApplication(applicationId1);
        for (org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO subscriptionDTO : subs1DTO.getList()) {
            restAPIStore.removeSubscription(subscriptionDTO);
        }
        SubscriptionListDTO subs2DTO = restAPIStore.getAllSubscriptionsOfApplication(applicationId2);
        for (org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO subscriptionDTO : subs2DTO.getList()) {
            restAPIStore.removeSubscription(subscriptionDTO);
        }
        SubscriptionListDTO subs3DTO = restAPIStore.getAllSubscriptionsOfApplication(applicationId3);
        for (org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO subscriptionDTO : subs3DTO.getList()) {
            restAPIStore.removeSubscription(subscriptionDTO);
        }
        restAPIStore.deleteApplication(applicationId1);
        restAPIStore.deleteApplication(applicationId2);
        restAPIStore.deleteApplication(applicationId3);
        restAPIPublisher.deleteMCPServer(mcpServerFromApiId);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteMCPServer(mcpServerFromOpenApiId);
        restAPIPublisher.deleteMCPServer(mcpServerProxyId);
    }

    public class MCPWireMock {

        private static final String MCP_PATH = "/mcp";
        private static final String SESSION_ID = "5d2755db-2e92-4016-b7f2-f49691ae1a08";

        private WireMockServer server;

        public WireMockServer start(int port) throws JSONException {

            if (server == null) {
                server = new WireMockServer(options().port(port));
            }
            if (server.isRunning()) {
                return server;
            }

            server.start();
            WireMock.configureFor("localhost", port);

            // Stub: initialize
            server.stubFor(
                    post(urlEqualTo(MCP_PATH))
                            .withHeader("Accept", containing("application/json"))
                            .withHeader("Accept", containing("text/event-stream"))
                            .withHeader("Content-Type", containing("application/json"))
                            .withRequestBody(matchingJsonPath("$.method", equalTo("initialize")))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "text/event-stream")
                                            .withHeader("Cache-Control", "no-cache")
                                            .withHeader("mcp-session-id", SESSION_ID)
                                            .withBody(
                                                    sse(
                                                            "message",
                                                            "e08d6cbc-556e-4b50-a110" +
                                                                    "-4cd053120747_1757927745129_ylb4b2ya",
                                                            initializeEnvelope().toString()
                                                    )
                                            )
                            )
            );

            // Stub: tools/list
            server.stubFor(
                    post(urlEqualTo(MCP_PATH))
                            .withHeader("Mcp-Session-Id", equalTo(SESSION_ID))
                            .withHeader("Accept", containing("application/json"))
                            .withHeader("Accept", containing("text/event-stream"))
                            .withHeader("Content-Type", containing("application/json"))
                            .withRequestBody(matchingJsonPath("$.method", equalTo("tools/list")))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "text/event-stream")
                                            .withHeader("Cache-Control", "no-cache")
                                            .withBody(
                                                    sse(
                                                            "message",
                                                            "a199b53a-901b-458e-9aac" +
                                                                    "-1526d5ce9eac_1757927789084_hcsr20bs",
                                                            toolsListEnvelope().toString()
                                                    )
                                            )
                            )
            );

            return server;
        }

        private String sse(String event, String id, String jsonPayload) {

            return "event: " + event + "\n" +
                    "id: " + id + "\n" +
                    "data: " + jsonPayload + "\n\n";
        }

        private JSONObject initializeEnvelope() throws JSONException {

            return new JSONObject()
                    .put("result", new JSONObject()
                            .put("protocolVersion", "2025-06-18")
                            .put("capabilities", new JSONObject()
                                    .put("prompts", new JSONObject())
                                    .put("resources", new JSONObject().put("subscribe", true))
                                    .put("tools", new JSONObject())
                                    .put("logging", new JSONObject())
                                    .put("completions", new JSONObject())
                                    .put("elicitation", new JSONObject())
                            )
                            .put("serverInfo", new JSONObject()
                                    .put("name", "bijira-mcp-everything")
                                    .put("version", "1.0.0")
                            )
                            .put("instructions",
                                    "Testing and demonstration server for MCP protocol features.\n\n" +
                                            "Resources: Resources 1-100 follow pattern: even IDs contain text, odd " +
                                            "IDs contain binary data. " +
                                            "Resources paginated at 10 items per page with cursor-based navigation" +
                                            ".\n\n" +
                                            "Multi-modal testing: complex_prompt includes both text arguments and " +
                                            "image content...\n"
                            )
                    )
                    .put("jsonrpc", "2.0")
                    .put("id", 1);
        }

        private JSONObject toolsListEnvelope() throws JSONException {

            JSONArray tools = new JSONArray()

                    // echo
                    .put(new JSONObject()
                            .put("name", "echo")
                            .put("description", "Echoes back the input")
                            .put("inputSchema", new JSONObject()
                                    .put("type", "object")
                                    .put("properties", new JSONObject()
                                            .put("message", new JSONObject()
                                                    .put("type", "string")
                                                    .put("description", "Message to echo")
                                            )
                                    )
                                    .put("required", new JSONArray().put("message"))
                                    .put("additionalProperties", false)
                                    .put("$schema", "http://json-schema.org/draft-07/schema#")
                            )
                    )

                    // add
                    .put(new JSONObject()
                            .put("name", "add")
                            .put("description", "Adds two numbers")
                            .put("inputSchema", new JSONObject()
                                    .put("type", "object")
                                    .put("properties", new JSONObject()
                                            .put("a", new JSONObject()
                                                    .put("type", "number")
                                                    .put("description", "First number")
                                            )
                                            .put("b", new JSONObject()
                                                    .put("type", "number")
                                                    .put("description", "Second number")
                                            )
                                    )
                                    .put("required", new JSONArray().put("a").put("b"))
                                    .put("additionalProperties", false)
                                    .put("$schema", "http://json-schema.org/draft-07/schema#")
                            )
                    )

                    // viewPizzaMenu
                    .put(new JSONObject()
                            .put("name", "viewPizzaMenu")
                            .put("description", "View the pizza menu. This tool provides a list of available pizzas.")
                            .put("inputSchema", new JSONObject()
                                    .put("type", "object")
                                    .put("properties", new JSONObject())
                                    .put("additionalProperties", false)
                                    .put("$schema", "http://json-schema.org/draft-07/schema#")
                            )
                    )

                    // orderPizza
                    .put(new JSONObject()
                            .put("name", "orderPizza")
                            .put("description",
                                    "Order a pizza from the menu. This tool allows you to place an order for a pizza.")
                            .put("inputSchema", new JSONObject()
                                    .put("type", "object")
                                    .put("properties", new JSONObject()
                                            .put("pizzaType", new JSONObject().put("type", "string"))
                                            .put("quantity", new JSONObject().put("type", "integer").put("minimum", 1))
                                            .put("customerName", new JSONObject().put("type", "string"))
                                            .put("deliveryAddress", new JSONObject().put("type", "string"))
                                            .put("creditCardNumber", new JSONObject().put("type", "string"))
                                    )
                                    .put("required", new JSONArray()
                                            .put("pizzaType")
                                            .put("quantity")
                                            .put("customerName")
                                            .put("deliveryAddress")
                                            .put("creditCardNumber")
                                    )
                                    .put("additionalProperties", false)
                                    .put("$schema", "http://json-schema.org/draft-07/schema#")
                            )
                    );

            return new JSONObject()
                    .put("result", new JSONObject().put("tools", tools))
                    .put("jsonrpc", "2.0")
                    .put("id", 2);
        }

        public boolean stop() {

            if (server != null && server.isRunning()) {
                server.stop();
                server = null;
            }
            return true;
        }
    }
}
