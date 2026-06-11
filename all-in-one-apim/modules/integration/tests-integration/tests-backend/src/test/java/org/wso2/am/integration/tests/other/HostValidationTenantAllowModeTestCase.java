/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.other;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerCertificatesDTO;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerDTO;
import org.wso2.am.integration.clients.publisher.api.v1.ImportExportApi;
import org.wso2.am.integration.test.ClientAuthenticator;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ApiEndpointValidationResponseDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.BackendDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MCPServerDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MCPServerProxyRequestDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SecurityInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SubtypeConfigurationDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Integration tests for outbound host validation via tenant-level allow mode (NetworkSecurityAccessControl).
 * Runs under {@link HostValidationTenantAllowModeTestSuite}, which applies a platform-level "allow all" configuration
 * (no mode restriction, block_private_network_access=false), isolating tenant-level allow mode behaviour.
 */
public class HostValidationTenantAllowModeTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(HostValidationTenantAllowModeTestCase.class);

    private static final String BLOCKED_URL = "http://evil.attacker.com/endpoint";
    private static final String ALLOWED_URL = "http://api.allowed.example.com/endpoint";

    private static final String TENANT_DOMAIN        = "hvtest.com";
    private static final String TENANT_ADMIN_USERNAME = "hvTestAdmin";
    private static final String TENANT_ADMIN_PASSWORD = "admin123";

    private String originalTenantConfig;
    private String apiId;
    private String mcpServerId;
    private String backendId;
    private RestAPIPublisherImpl tenantPublisher;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(TestUserMode.SUPER_TENANT_ADMIN);
        originalTenantConfig = restAPIAdmin.getTenantConfig();

        APIRequest apiRequest = new APIRequest("HVTestAPI", "/hvtest",
                new URL(backEndServerUrl.getWebAppURLHttp()
                        + "jaxrs_basic/services/customers/customerservice/"));
        apiRequest.setVersion("1.0.0");
        apiRequest.setProvider(user.getUserName());
        apiId = restAPIPublisher.addAPI(apiRequest).getData();

        // Create an MCP server for MCP-update and backend-update host validation tests.
        File mcpOasFile = new File(getAMResourceLocation()
                + File.separator + "hostValidationResources" + File.separator + "hv-test-openapi.yaml");
        JSONObject mcpAdditionalProps = new JSONObject();
        mcpAdditionalProps.put("name", "hvMcpValid");
        mcpAdditionalProps.put("version", "v1");
        mcpAdditionalProps.put("context", "/hvmcpvalid");
        mcpAdditionalProps.put("gatewayType", "wso2/synapse");
        JSONArray mcpPolicies = new JSONArray();
        mcpPolicies.add("Unlimited");
        mcpAdditionalProps.put("policies", mcpPolicies);
        JSONArray mcpOperations = new JSONArray();
        JSONObject mcpOp = new JSONObject();
        mcpOp.put("feature", "TOOL");
        JSONObject mcpBackendMapping = new JSONObject();
        JSONObject mcpBackendOp = new JSONObject();
        mcpBackendOp.put("target", "/test");
        mcpBackendOp.put("verb", "GET");
        mcpBackendMapping.put("backendOperation", mcpBackendOp);
        mcpOp.put("backendOperationMapping", mcpBackendMapping);
        mcpOperations.add(mcpOp);
        mcpAdditionalProps.put("operations", mcpOperations);
        JSONObject mcpProdUrl = new JSONObject();
        mcpProdUrl.put("url", backEndServerUrl.getWebAppURLHttp() + "jaxrs_basic/");
        JSONObject mcpEndpointConfig = new JSONObject();
        mcpEndpointConfig.put("endpoint_type", "http");
        mcpEndpointConfig.put("production_endpoints", mcpProdUrl);
        mcpEndpointConfig.put("sandbox_endpoints", mcpProdUrl);
        mcpAdditionalProps.put("endpointConfig", mcpEndpointConfig);
        MCPServerDTO createdMcp = restAPIPublisher.createMCPServerFromOpenAPI(
                mcpOasFile, mcpAdditionalProps.toJSONString());
        mcpServerId = createdMcp.getId();
        // The backends API returns a raw JSON array; parse it directly to avoid BackendListDTO deserialization error.
        okhttp3.Call backendListCall = restAPIPublisher.mcpServersBackendsApi
                .getMCPServerBackendsCall(mcpServerId, null);
        try (okhttp3.Response rawBackendResp = backendListCall.execute()) {
            if (rawBackendResp.body() != null) {
                String backendsBody = rawBackendResp.body().string();
                JSONParser backendsParser = new JSONParser();
                JSONArray backends = (JSONArray) backendsParser.parse(backendsBody);
                if (backends != null && !backends.isEmpty()) {
                    backendId = (String) ((JSONObject) backends.get(0)).get("id");
                }
            }
        }
        Assert.assertNotNull(backendId,
                "Setup failed: could not retrieve backendId from MCP server — backend update host validation tests will be skipped");

        // Create a fresh tenant for the tenant isolation test.
        // This tenant starts with a clean (default) config — no NetworkSecurityAccessControl block.
        tenantManagementServiceClient.addTenant(
                TENANT_DOMAIN, TENANT_ADMIN_PASSWORD, TENANT_ADMIN_USERNAME, "demo");
        tenantPublisher = getRestAPIPublisherForUser(
                TENANT_ADMIN_USERNAME, TENANT_ADMIN_PASSWORD, TENANT_DOMAIN);
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode disabled — endpoint validation is not blocked")
    public void testAllowModeDisabled_EndpointValidationNotBlocked() throws Exception {
        // Default: no NetworkSecurityAccessControl in tenant config. Allow mode must not trigger.
        ApiEndpointValidationResponseDTO dto = restAPIPublisher.validateEndpointRaw(BLOCKED_URL, apiId);
        if (dto.getError() != null) {
            Assert.assertFalse(dto.getError().contains("not trusted"),
                    "URL should not be blocked when allow mode is disabled, error: " + dto.getError());
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — non-matching URL blocked on endpoint validation",
            dependsOnMethods = "testAllowModeDisabled_EndpointValidationNotBlocked")
    public void testAllowModeEnabled_NonMatchingURLBlockedOnEndpointValidation() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            ApiEndpointValidationResponseDTO dto =
                    restAPIPublisher.validateEndpointRaw(BLOCKED_URL, apiId);
            Assert.assertNotNull(dto.getError(),
                    "Endpoint validation must return an error for a URL blocked by the tenant allow mode");
            Assert.assertTrue(dto.getError().contains("not trusted"),
                    "Expected host validation block message, got: " + dto.getError());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — matching URL passes endpoint validation",
            dependsOnMethods = "testAllowModeEnabled_NonMatchingURLBlockedOnEndpointValidation")
    public void testAllowModeEnabled_MatchingURLPassesEndpointValidation() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            ApiEndpointValidationResponseDTO dto =
                    restAPIPublisher.validateEndpointRaw(ALLOWED_URL, apiId);
            if (dto.getError() != null) {
                Assert.assertFalse(dto.getError().contains("not trusted"),
                        "URL matching the allow mode hosts should not be blocked, error: " + dto.getError());
            }
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — KM create with non-matching URL returns 400",
            dependsOnMethods = "testAllowModeEnabled_MatchingURLPassesEndpointValidation")
    public void testAllowModeEnabled_KeyManagerCreateWithBlockedURLRejected() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            restAPIAdmin.addKeyManager(buildKeyManagerDTO("HVBlockedKM",
                    "https://attacker.corp/oauth2/introspect",
                    "https://attacker.corp/keymanager-operations/dcr/register",
                    "https://attacker.corp/oauth2/token",
                    "https://attacker.corp/oauth2/revoke",
                    null));
            Assert.fail("Expected ApiException for KM with blocked URLs");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for KM creation with blocked URL");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in KM create response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — KM JWKS cert URL blocked returns 400",
            dependsOnMethods = "testAllowModeEnabled_KeyManagerCreateWithBlockedURLRejected")
    public void testAllowModeEnabled_KeyManagerJWKSCertURLBlocked() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            KeyManagerCertificatesDTO blockedCerts = DtoFactory.createKeyManagerCertificatesDTO(
                    KeyManagerCertificatesDTO.TypeEnum.JWKS,
                    "https://attacker.corp/.well-known/jwks.json");
            restAPIAdmin.addKeyManager(buildKeyManagerDTO("HVBlockedJWKSKM",
                    "https://api.allowed.example.com/oauth2/introspect",
                    "https://api.allowed.example.com/keymanager-operations/dcr/register",
                    "https://api.allowed.example.com/oauth2/token",
                    "https://api.allowed.example.com/oauth2/revoke",
                    blockedCerts));
            Assert.fail("Expected ApiException for KM with blocked JWKS cert URL");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for KM creation with blocked JWKS URL");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in KM JWKS cert response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — KM update with non-matching URL returns 400",
            dependsOnMethods = "testAllowModeEnabled_KeyManagerJWKSCertURLBlocked")
    public void testAllowModeEnabled_KeyManagerUpdateWithBlockedURLRejected() throws Exception {
        String createdKmId = null;
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            ApiResponse<KeyManagerDTO> createResponse = restAPIAdmin.addKeyManager(
                    buildKeyManagerDTO("HVUpdateTestKM",
                            "https://api.allowed.example.com/oauth2/introspect",
                            "https://api.allowed.example.com/keymanager-operations/dcr/register",
                            "https://api.allowed.example.com/oauth2/token",
                            "https://api.allowed.example.com/oauth2/revoke",
                            null));
            Assert.assertEquals(createResponse.getStatusCode(), HttpStatus.SC_CREATED);
            createdKmId = createResponse.getData().getId();

            KeyManagerDTO toUpdate = createResponse.getData();
            toUpdate.setTokenEndpoint("https://attacker.corp/oauth2/token");
            try {
                restAPIAdmin.updateKeyManager(createdKmId, toUpdate);
                Assert.fail("Expected ApiException for KM update with blocked URL");
            } catch (ApiException e) {
                Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                        "Expected HTTP 400 for KM update with blocked URL");
                Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                        "Expected host validation block error in KM update response body, got: " + e.getResponseBody());
            }
        } finally {
            if (createdKmId != null) {
                restAPIAdmin.deleteKeyManager(createdKmId);
            }
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — GraphQL schema validation by non-matching URL returns isValid=false",
            dependsOnMethods = "testAllowModeEnabled_KeyManagerUpdateWithBlockedURLRejected")
    public void testAllowModeEnabled_GraphQLValidationByBlockedURLFails() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            GraphQLValidationResponseDTO response =
                    restAPIPublisher.validateGraphqlSchemaDefinitionByURL(BLOCKED_URL, false);
            Assert.assertNotNull(response, "Response should not be null");
            Assert.assertFalse(Boolean.TRUE.equals(response.isIsValid()),
                    "GraphQL schema validation must return isValid=false for a URL blocked by the tenant allow mode");
            Assert.assertNotNull(response.getErrorMessage(),
                    "GraphQL validation response must include an error message for blocked URL");
            Assert.assertTrue(response.getErrorMessage().contains("not trusted"),
                    "Expected host validation block error in GraphQL validation errorMessage, got: " + response.getErrorMessage());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — WSDL import by non-matching URL returns 400",
            dependsOnMethods = "testAllowModeEnabled_GraphQLValidationByBlockedURLFails")
    public void testAllowModeEnabled_WSDLImportByBlockedURLRejected() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            restAPIPublisher.importWSDLSchemaDefinition(null,
                    "http://attacker.internal.corp/service?wsdl",
                    "{\"name\":\"HVWSDLBlockTest\",\"context\":\"/hvwsdlblock\","
                            + "\"version\":\"1.0.0\",\"provider\":\"" + user.getUserName() + "\"}",
                    "SOAP");
            Assert.fail("Expected ApiException for WSDL import from blocked URL");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for WSDL import from blocked URL");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in WSDL import response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — OAS definition validate from non-matching URL returns 400",
            dependsOnMethods = "testAllowModeEnabled_WSDLImportByBlockedURLRejected")
    public void testAllowModeEnabled_OASDefinitionFromBlockedURLRejected() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            restAPIPublisher.validateOASDefinitionByURL(BLOCKED_URL);
            Assert.fail("Expected ApiException for OAS validate from blocked URL");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for OAS definition validate from blocked URL");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in OAS validate response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — KM discover with non-matching URL returns 400",
            dependsOnMethods = "testAllowModeEnabled_OASDefinitionFromBlockedURLRejected")
    public void testAllowModeEnabled_KeyManagerDiscoverWithBlockedURLRejected() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            restAPIAdmin.discoverKeyManager("https://attacker.corp/.well-known/openid-configuration", "OIDC");
            Assert.fail("Expected ApiException for KM discover with blocked URL");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for KM discover with blocked URL");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in KM discover response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — MCP proxy create with non-matching URL returns 400",
            dependsOnMethods = "testAllowModeEnabled_KeyManagerDiscoverWithBlockedURLRejected")
    public void testAllowModeEnabled_MCPProxyCreateWithBlockedURLRejected() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            MCPServerProxyRequestDTO proxyRequest = new MCPServerProxyRequestDTO();
            proxyRequest.setUrl(BLOCKED_URL);
            proxyRequest.setSecurityInfo(new SecurityInfoDTO());
            MCPServerDTO proxyProps = new MCPServerDTO();
            proxyProps.setName("HVProxyTest");
            proxyProps.setContext("/hvproxytest");
            proxyProps.setVersion("1.0.0");
            proxyRequest.setAdditionalProperties(proxyProps);
            restAPIPublisher.createMCPServerProxy(proxyRequest);
            Assert.fail("Expected ApiException for MCP proxy create with blocked URL");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for MCP proxy create with blocked URL");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in MCP proxy create response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — API creation with blocked backend URL returns 400",
            dependsOnMethods = "testAllowModeEnabled_MCPProxyCreateWithBlockedURLRejected")
    public void testAllowModeEnabled_APICreateWithBlockedBackendURLRejected() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            APIDTO body = new APIDTO();
            body.setName("HVBackendBlockTest");
            body.setContext("/hvbackendblock");
            body.setVersion("1.0.0");
            body.setProvider(user.getUserName());
            body.setVisibility(APIDTO.VisibilityEnum.PUBLIC);
            body.setType(APIDTO.TypeEnum.HTTP);
            body.setPolicies(Collections.singletonList("Unlimited"));
            APIOperationsDTO op = new APIOperationsDTO();
            op.setVerb("GET");
            op.setTarget("/*");
            op.setAuthType("Application & Application User");
            op.setThrottlingPolicy("Unlimited");
            body.setOperations(Collections.singletonList(op));
            Map<String, Object> epConfig = new HashMap<>();
            epConfig.put("endpoint_type", "http");
            Map<String, Object> prod = new HashMap<>();
            prod.put("url", "http://attacker.internal.corp/backend");
            epConfig.put("production_endpoints", prod);
            body.setEndpointConfig(epConfig);
            restAPIPublisher.apIsApi.createAPIWithHttpInfo(body, "v3");
            Assert.fail("Expected ApiException for API creation with blocked backend URL");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for API creation with blocked backend URL");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in API create response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — API update with blocked backend URL returns 400",
            dependsOnMethods = "testAllowModeEnabled_APICreateWithBlockedBackendURLRejected")
    public void testAllowModeEnabled_APIUpdateWithBlockedBackendURLRejected() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            APIDTO apidto = restAPIPublisher.apIsApi.getAPI(apiId, null, null);
            Map<String, Object> blockedConfig = new HashMap<>();
            blockedConfig.put("endpoint_type", "http");
            Map<String, Object> production = new HashMap<>();
            production.put("url", BLOCKED_URL);
            blockedConfig.put("production_endpoints", production);
            apidto.setEndpointConfig(blockedConfig);
            restAPIPublisher.updateAPI(apidto, apiId);
            Assert.fail("Expected ApiException for API update with blocked backend URL");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for API update with blocked backend URL");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in API update response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — create MCP server from existing API with blocked endpoint returns 400",
            dependsOnMethods = "testAllowModeEnabled_APIUpdateWithBlockedBackendURLRejected")
    public void testAllowModeEnabled_MCPServerCreateFromAPIWithBlockedEndpointRejected() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            MCPServerDTO mcpDTO = new MCPServerDTO();
            mcpDTO.setName("HVMcpFromAPITest");
            mcpDTO.setContext("/hvmcpfromapitest");
            mcpDTO.setVersion("1.0.0");
            Map<String, Object> blockedConfig = new HashMap<>();
            blockedConfig.put("endpoint_type", "http");
            Map<String, Object> production = new HashMap<>();
            production.put("url", BLOCKED_URL);
            blockedConfig.put("production_endpoints", production);
            mcpDTO.setEndpointConfig(blockedConfig);
            SubtypeConfigurationDTO subtypeConfig = new SubtypeConfigurationDTO();
            subtypeConfig.setSubtype("EXISTING_API");
            mcpDTO.setSubtypeConfiguration(subtypeConfig);
            restAPIPublisher.createMCPServerFromAPI(mcpDTO);
            Assert.fail("Expected ApiException for MCP server create from API with blocked endpoint");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for MCP server create from API with blocked endpoint");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in MCP server create response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — MCP server update with blocked backend URL returns 400",
            dependsOnMethods = "testAllowModeEnabled_MCPServerCreateFromAPIWithBlockedEndpointRejected")
    public void testAllowModeEnabled_MCPServerUpdateWithBlockedEndpointRejected() throws Exception {
        Assert.assertNotNull(mcpServerId, "MCP server must be set up before this test");
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            MCPServerDTO mcpServerDTO = restAPIPublisher.mcpServersApi.getMCPServer(mcpServerId, null, null);
            Map<String, Object> blockedConfig = new HashMap<>();
            blockedConfig.put("endpoint_type", "http");
            Map<String, Object> production = new HashMap<>();
            production.put("url", BLOCKED_URL);
            blockedConfig.put("production_endpoints", production);
            mcpServerDTO.setEndpointConfig(blockedConfig);
            restAPIPublisher.updateMCPServer(mcpServerId, mcpServerDTO);
            Assert.fail("Expected ApiException for MCP server update with blocked backend URL");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for MCP server update with blocked backend URL");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in MCP server update response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — MCP server backend update with blocked URL returns 400",
            dependsOnMethods = "testAllowModeEnabled_MCPServerUpdateWithBlockedEndpointRejected")
    public void testAllowModeEnabled_MCPServerBackendUpdateWithBlockedURLRejected() throws Exception {
        Assert.assertNotNull(backendId, "MCP server backend must be set up before this test");
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            BackendDTO blockedBackend = new BackendDTO();
            JSONObject blockedEndpointConfig = new JSONObject();
            blockedEndpointConfig.put("endpoint_type", "http");
            JSONObject blockedProductionUrl = new JSONObject();
            blockedProductionUrl.put("url", BLOCKED_URL);
            blockedEndpointConfig.put("production_endpoints", blockedProductionUrl);
            blockedBackend.setEndpointConfig(blockedEndpointConfig);
            restAPIPublisher.updateMCPServerBackend(mcpServerId, backendId, blockedBackend);
            Assert.fail("Expected ApiException for MCP server backend update with blocked URL");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for MCP server backend update with blocked URL");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in MCP server backend update response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — OpenAPI import with blocked endpoint in additionalProperties returns 400",
            dependsOnMethods = "testAllowModeEnabled_MCPServerBackendUpdateWithBlockedURLRejected")
    public void testAllowModeEnabled_OpenAPIImportWithBlockedEndpointRejected() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            File openAPIFile = new File(getAMResourceLocation()
                    + File.separator + "hostValidationResources" + File.separator + "hv-test-openapi.yaml");
            JSONObject endpointUrl = new JSONObject();
            endpointUrl.put("url", BLOCKED_URL);
            JSONObject endpointConfig = new JSONObject();
            endpointConfig.put("endpoint_type", "http");
            endpointConfig.put("production_endpoints", endpointUrl);
            JSONObject additionalProperties = new JSONObject();
            additionalProperties.put("name", "HVOASImportTest");
            additionalProperties.put("context", "/hvoasimporttest");
            additionalProperties.put("version", "1.0.0");
            additionalProperties.put("provider", user.getUserName());
            additionalProperties.put("endpointConfig", endpointConfig);
            restAPIPublisher.importOASDefinition(openAPIFile, additionalProperties.toJSONString());
            Assert.fail("Expected ApiException for OpenAPI import with blocked endpoint");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for OpenAPI import with blocked endpoint");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in OpenAPI import response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — AsyncAPI import with blocked endpoint in additionalProperties returns 400",
            dependsOnMethods = "testAllowModeEnabled_OpenAPIImportWithBlockedEndpointRejected")
    public void testAllowModeEnabled_AsyncAPIImportWithBlockedEndpointRejected() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            File asyncAPIFile = new File(getAMResourceLocation()
                    + File.separator + "hostValidationResources" + File.separator + "hv-test-asyncapi.yaml");
            JSONObject endpointUrl = new JSONObject();
            endpointUrl.put("url", BLOCKED_URL);
            JSONObject endpointConfig = new JSONObject();
            endpointConfig.put("endpoint_type", "ws");
            endpointConfig.put("production_endpoints", endpointUrl);
            JSONObject additionalProperties = new JSONObject();
            additionalProperties.put("name", "HVAsyncAPIImportTest");
            additionalProperties.put("context", "/hvasyncimporttest");
            additionalProperties.put("version", "1.0.0");
            additionalProperties.put("provider", user.getUserName());
            additionalProperties.put("endpointConfig", endpointConfig);
            restAPIPublisher.importAsyncAPIDefinition(asyncAPIFile, additionalProperties.toJSONString());
            Assert.fail("Expected ApiException for AsyncAPI import with blocked endpoint");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for AsyncAPI import with blocked endpoint");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in AsyncAPI import response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — GraphQL schema import from blocked URL returns 400",
            dependsOnMethods = "testAllowModeEnabled_AsyncAPIImportWithBlockedEndpointRejected")
    public void testAllowModeEnabled_GraphQLImportByBlockedURLRejected() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            JSONObject additionalProperties = new JSONObject();
            additionalProperties.put("name", "HVGraphQLImportTest");
            additionalProperties.put("context", "/hvgqlimporttest");
            additionalProperties.put("version", "1.0.0");
            additionalProperties.put("provider", user.getUserName());
            restAPIPublisher.importGraphqlSchemaDefinitionByURL(
                    BLOCKED_URL, additionalProperties.toJSONString());
            Assert.fail("Expected ApiException for GraphQL import from blocked URL");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for GraphQL schema import from blocked URL");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in GraphQL import response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — MCP server from OpenAPI with blocked endpoint in additionalProperties returns 400",
            dependsOnMethods = "testAllowModeEnabled_GraphQLImportByBlockedURLRejected")
    public void testAllowModeEnabled_MCPServerFromOpenAPIWithBlockedEndpointRejected() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            File openAPIFile = new File(getAMResourceLocation()
                    + File.separator + "hostValidationResources" + File.separator + "hv-test-openapi.yaml");
            JSONObject endpointUrl = new JSONObject();
            endpointUrl.put("url", BLOCKED_URL);
            JSONObject endpointConfig = new JSONObject();
            endpointConfig.put("endpoint_type", "http");
            endpointConfig.put("production_endpoints", endpointUrl);
            JSONObject additionalProperties = new JSONObject();
            additionalProperties.put("name", "HVMcpFromOASTest");
            additionalProperties.put("context", "/hvmcpfromoastest");
            additionalProperties.put("version", "1.0.0");
            additionalProperties.put("provider", user.getUserName());
            additionalProperties.put("endpointConfig", endpointConfig);
            restAPIPublisher.createMCPServerFromOpenAPI(openAPIFile, additionalProperties.toJSONString());
            Assert.fail("Expected ApiException for MCP server from OpenAPI with blocked endpoint");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for MCP server from OpenAPI with blocked endpoint");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in MCP server from OpenAPI response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — API ZIP import with blocked endpoint URL returns 400",
            dependsOnMethods = "testAllowModeEnabled_MCPServerFromOpenAPIWithBlockedEndpointRejected")
    public void testAllowModeEnabled_APIZIPImportWithBlockedEndpointRejected() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            File apiZip = new File(getAMResourceLocation()
                    + File.separator + "hostValidationResources" + File.separator + "hv-test-api-blocked.zip");
            buildImportExportApiClient().importAPIWithHttpInfo(apiZip, false, false, false);
            Assert.fail("Expected ApiException for API ZIP import with blocked endpoint");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for API ZIP import with blocked endpoint");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in API ZIP import response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: allow mode enabled — MCP server ZIP import with blocked endpoint URL returns 400",
            dependsOnMethods = "testAllowModeEnabled_APIZIPImportWithBlockedEndpointRejected")
    public void testAllowModeEnabled_MCPServerZIPImportWithBlockedEndpointRejected() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            File mcpZip = new File(getAMResourceLocation()
                    + File.separator + "hostValidationResources" + File.separator + "hv-test-mcp-blocked.zip");
            buildImportExportApiClient().importMCPServerWithHttpInfo(mcpZip, false, false, false, false, false, null);
            Assert.fail("Expected ApiException for MCP server ZIP import with blocked endpoint");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for MCP server ZIP import with blocked endpoint");
            Assert.assertTrue(e.getResponseBody() != null && e.getResponseBody().contains("not trusted"),
                    "Expected host validation block error in MCP server ZIP import response body, got: " + e.getResponseBody());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "host validation: super-tenant allow mode must not affect a new tenant whose config is at default",
            dependsOnMethods = "testAllowModeEnabled_MCPServerZIPImportWithBlockedEndpointRejected")
    public void testAllowModeIsolation_NewTenantUnaffectedBySuperTenantAllowMode() throws Exception {
        try {
            enableTenantAllowMode(new String[]{"*.allowed.example.com"});
            // Super tenant is blocked by its own allow mode config.
            ApiEndpointValidationResponseDTO superDto =
                    restAPIPublisher.validateEndpointRaw(BLOCKED_URL, apiId);
            Assert.assertNotNull(superDto.getError(), "Expected host validation block for super tenant");
            Assert.assertTrue(superDto.getError().contains("not trusted"),
                    "Super tenant allow mode should block non-matching URL, got: " + superDto.getError());

            // New tenant has default config (no NetworkSecurityAccessControl block — feature inactive).
            ApiEndpointValidationResponseDTO tenantDto =
                    tenantPublisher.validateEndpointRaw(BLOCKED_URL, null);
            Assert.assertNotNull(tenantDto, "Tenant endpoint validation response must not be null");
            if (tenantDto.getError() != null) {
                Assert.assertFalse(tenantDto.getError().contains("not trusted"),
                        "New tenant with default config must not inherit super-tenant allow mode, got: "
                                + tenantDto.getError());
            }
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    /**
     * Builds an ImportExportApi client with apim:api_import_export scope.
     * restAPIPublisher.apiPublisherClient omits that scope, so a dedicated token is needed.
     */
    private ImportExportApi buildImportExportApiClient() throws Exception {
        String tokenURL = publisherURLHttps + "oauth2/token";
        String dcrURL = publisherURLHttps + "client-registration/v0.17/register";
        String token = ClientAuthenticator.getAccessToken(
                "apim:api_import_export apim:mcp_server_import_export",
                RestAPIPublisherImpl.appName, RestAPIPublisherImpl.callBackURL,
                RestAPIPublisherImpl.tokenScope, RestAPIPublisherImpl.appOwner,
                RestAPIPublisherImpl.grantType, dcrURL,
                user.getUserName(), user.getPassword(), user.getUserDomain(), tokenURL);
        org.wso2.am.integration.clients.publisher.api.ApiClient client =
                new org.wso2.am.integration.clients.publisher.api.ApiClient();
        client.addDefaultHeader("Authorization", "Bearer " + token);
        client.setBasePath(publisherURLHttps + "api/am/publisher/v4");
        client.setReadTimeout(600000);
        client.setConnectTimeout(600000);
        return new ImportExportApi(client);
    }

    private void enableTenantAllowMode(String[] patterns) throws Exception {
        JSONParser parser = new JSONParser();
        JSONObject config = (JSONObject) parser.parse(restAPIAdmin.getTenantConfig());
        JSONObject accessControl = new JSONObject();
        accessControl.put("Mode", "allow");
        JSONArray hostsArray = new JSONArray();
        for (String p : patterns) {
            hostsArray.add(p);
        }
        accessControl.put("Hosts", hostsArray);
        config.put("NetworkSecurityAccessControl", accessControl);
        restAPIAdmin.updateTenantConfig(config);
    }

    private void restoreOriginalTenantConfig() throws Exception {
        if (originalTenantConfig != null) {
            JSONParser parser = new JSONParser();
            JSONObject config = (JSONObject) parser.parse(originalTenantConfig);
            restAPIAdmin.updateTenantConfig(config);
        }
    }

    private KeyManagerDTO buildKeyManagerDTO(String name, String introspectionEndpoint,
            String clientRegistrationEndpoint, String tokenEndpoint, String revokeEndpoint,
            KeyManagerCertificatesDTO certs) {
        JsonObject additionalProperties = new JsonObject();
        additionalProperties.addProperty("Username", "admin");
        additionalProperties.addProperty("Password", "admin");
        additionalProperties.addProperty("self_validate_jwt", true);
        Object additionalPropertiesMap = new Gson().fromJson(additionalProperties, Map.class);

        return DtoFactory.createKeyManagerDTO(name, null, "WSO2-IS", name,
                introspectionEndpoint, null, clientRegistrationEndpoint,
                tokenEndpoint, revokeEndpoint, null, null, null,
                "azp", "scope", Collections.emptyList(), additionalPropertiesMap, certs);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        try {
            restoreOriginalTenantConfig();
        } finally {
            try {
                if (mcpServerId != null) {
                    restAPIPublisher.deleteMCPServer(mcpServerId);
                }
                if (apiId != null) {
                    restAPIPublisher.deleteAPI(apiId);
                }
            } finally {
                try {
                    tenantManagementServiceClient.deleteTenant(TENANT_DOMAIN);
                } catch (Exception e) {
                    log.warn("Failed to delete test tenant " + TENANT_DOMAIN
                            + " during cleanup; it may need manual removal", e);
                } finally {
                    super.cleanUp();
                }
            }
        }
    }
}
