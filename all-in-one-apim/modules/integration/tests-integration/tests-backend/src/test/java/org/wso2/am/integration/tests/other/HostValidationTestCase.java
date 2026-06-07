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
import org.wso2.am.integration.clients.publisher.api.v1.dto.ApiEndpointValidationResponseDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MCPServerProxyRequestDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

/**
 * Integration tests for host validation (outbound request security).
 */
public class HostValidationTestCase extends APIMIntegrationBaseTest {

    private static final String BLOCKED_URL = "http://attacker.internal.corp/endpoint";
    private static final String ALLOWED_URL = "http://api.allowed.example.com/endpoint";

    private static final String TENANT_DOMAIN         = "hvtest.com";
    private static final String TENANT_ADMIN_USERNAME  = "hvTestAdmin";
    private static final String TENANT_ADMIN_PASSWORD  = "admin123";

    private String originalTenantConfig;
    private String apiId;
    private RestAPIPublisherImpl tenantPublisher;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(TestUserMode.SUPER_TENANT_ADMIN);
        originalTenantConfig = restAPIAdmin.getTenantConfig();

        APIRequest apiRequest = new APIRequest("HostValidationTestAPI", "/hostvalidationtest",
                new URL(backEndServerUrl.getWebAppURLHttp()
                        + "jaxrs_basic/services/customers/customerservice/"));
        apiRequest.setVersion("1.0.0");
        apiRequest.setProvider(user.getUserName());
        apiId = restAPIPublisher.addAPI(apiRequest).getData();

        // Create a fresh tenant for the tenant isolation test.
        // This tenant starts with a clean (default) config — no OutboundRequestSecurity entry.
        tenantManagementServiceClient.addTenant(
                TENANT_DOMAIN, TENANT_ADMIN_PASSWORD, TENANT_ADMIN_USERNAME, "demo");
        tenantPublisher = getRestAPIPublisherForUser(
                TENANT_ADMIN_USERNAME, TENANT_ADMIN_PASSWORD, TENANT_DOMAIN);
    }

    @Test(groups = {"wso2.am"},
            description = "Host validation: allowlist disabled — endpoint validation is not blocked by allowlist")
    public void testAllowlistDisabled_EndpointValidationNotBlocked() throws Exception {
        // Default: EnableHostAllowlist=false. The host allowlist must not trigger.
        ApiEndpointValidationResponseDTO dto = restAPIPublisher.validateEndpointRaw(BLOCKED_URL, apiId);
        if (dto.getError() != null) {
            Assert.assertFalse(dto.getError().contains("not trusted"),
                    "URL should not be blocked when allowlist is disabled, error: " + dto.getError());
        }
    }

    @Test(groups = {"wso2.am"},
            description = "Host validation: genuine DNS failure should reported as a connectivity error.",
            dependsOnMethods = "testAllowlistDisabled_EndpointValidationNotBlocked")
    public void testNonExistentHost_DNSFailureNotTreatedAsHostValidationBlock() throws Exception {
        ApiEndpointValidationResponseDTO dto =
                restAPIPublisher.validateEndpointRaw("http://definitely-does-not-exist.invalid/api", apiId);
        Assert.assertNotNull(dto.getError(), "Expected a connectivity error for a nonexistent host");
        Assert.assertFalse(dto.getError().contains("not trusted"),
                "DNS/connection failure must not be mis-classified as a host validation block, error: " + dto.getError());
    }

    @Test(groups = {"wso2.am"},
            description = "Host validation: allowlist enabled — non-matching URL blocked on endpoint validation",
            dependsOnMethods = "testNonExistentHost_DNSFailureNotTreatedAsHostValidationBlock")
    public void testAllowlistEnabled_NonMatchingURLBlockedOnEndpointValidation() throws Exception {
        try {
            enableTenantAllowlist(new String[]{"*.allowed.example.com"});
            ApiEndpointValidationResponseDTO dto =
                    restAPIPublisher.validateEndpointRaw(BLOCKED_URL, apiId);
            Assert.assertNotNull(dto.getError(), "Expected an error for blocked URL");
            Assert.assertTrue(
                    dto.getError().contains("not trusted"),
                    "Expected host validation block message, got: " + dto.getError());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "Host validation: allowlist enabled — matching URL passes endpoint validation",
            dependsOnMethods = "testAllowlistEnabled_NonMatchingURLBlockedOnEndpointValidation")
    public void testAllowlistEnabled_MatchingURLPassesEndpointValidation() throws Exception {
        try {
            enableTenantAllowlist(new String[]{"*.allowed.example.com"});
            ApiEndpointValidationResponseDTO dto =
                    restAPIPublisher.validateEndpointRaw(ALLOWED_URL, apiId);
            if (dto.getError() != null) {
                Assert.assertFalse(dto.getError().contains("not trusted"),
                        "URL matching the allowlist should not be blocked, error: " + dto.getError());
            }
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "Host validation: allowlist enabled — KM create with non-matching URL returns 400",
            dependsOnMethods = "testAllowlistEnabled_MatchingURLPassesEndpointValidation")
    public void testAllowlistEnabled_KeyManagerCreateWithBlockedURLRejected() throws Exception {
        try {
            enableTenantAllowlist(new String[]{"*.allowed.example.com"});
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
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "Host validation: allowlist enabled — KM JWKS cert URL blocked returns 400",
            dependsOnMethods = "testAllowlistEnabled_KeyManagerCreateWithBlockedURLRejected")
    public void testAllowlistEnabled_KeyManagerJWKSCertURLBlocked() throws Exception {
        try {
            enableTenantAllowlist(new String[]{"*.allowed.example.com"});
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
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "Host validation: allowlist enabled — KM update with non-matching URL returns 400",
            dependsOnMethods = "testAllowlistEnabled_KeyManagerJWKSCertURLBlocked")
    public void testAllowlistEnabled_KeyManagerUpdateWithBlockedURLRejected() throws Exception {
        // Create a valid KM with all URLs on the allowlist, then update with a blocked URL
        String createdKmId = null;
        try {
            enableTenantAllowlist(new String[]{"*.allowed.example.com"});
            ApiResponse<KeyManagerDTO> createResponse = restAPIAdmin.addKeyManager(
                    buildKeyManagerDTO("HVUpdateTestKM",
                            "https://api.allowed.example.com/oauth2/introspect",
                            "https://api.allowed.example.com/keymanager-operations/dcr/register",
                            "https://api.allowed.example.com/oauth2/token",
                            "https://api.allowed.example.com/oauth2/revoke",
                            null));
            Assert.assertEquals(createResponse.getStatusCode(), HttpStatus.SC_CREATED);
            createdKmId = createResponse.getData().getId();

            // Update: change token endpoint to a blocked host
            KeyManagerDTO toUpdate = createResponse.getData();
            toUpdate.setTokenEndpoint("https://attacker.corp/oauth2/token");
            try {
                restAPIAdmin.updateKeyManager(createdKmId, toUpdate);
                Assert.fail("Expected ApiException for KM update with blocked URL");
            } catch (ApiException e) {
                Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                        "Expected HTTP 400 for KM update with blocked URL");
            }
        } finally {
            if (createdKmId != null) {
                restAPIAdmin.deleteKeyManager(createdKmId);
            }
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "Host validation: allowlist enabled — GraphQL schema validation by non-matching URL returns isValid=false",
            dependsOnMethods = "testAllowlistEnabled_KeyManagerUpdateWithBlockedURLRejected")
    public void testAllowlistEnabled_GraphQLValidationByBlockedURLFails() throws Exception {
        try {
            enableTenantAllowlist(new String[]{"*.allowed.example.com"});
            GraphQLValidationResponseDTO response =
                    restAPIPublisher.validateGraphqlSchemaDefinitionByURL(BLOCKED_URL, false);
            Assert.assertNotNull(response, "Response should not be null");
            Assert.assertFalse(Boolean.TRUE.equals(response.isIsValid()),
                    "GraphQL validation should fail (isValid=false) for blocked URL");
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "Host validation: allowlist enabled — WSDL import by non-matching URL returns 400",
            dependsOnMethods = "testAllowlistEnabled_GraphQLValidationByBlockedURLFails")
    public void testAllowlistEnabled_WSDLImportByBlockedURLRejected() throws Exception {
        try {
            enableTenantAllowlist(new String[]{"*.allowed.example.com"});
            restAPIPublisher.importWSDLSchemaDefinition(null,
                    "http://attacker.internal.corp/service?wsdl",
                    "{\"name\":\"HVWSDLBlockTest\",\"context\":\"/hvwsdlblock\","
                            + "\"version\":\"1.0.0\",\"provider\":\"" + user.getUserName() + "\"}",
                    "SOAP");
            Assert.fail("Expected ApiException for WSDL import from blocked URL");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for WSDL import from blocked URL");
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "Host validation: allowlist enabled — OAS definition import from non-matching URL returns 400",
            dependsOnMethods = "testAllowlistEnabled_WSDLImportByBlockedURLRejected")
    public void testAllowlistEnabled_OASDefinitionFromBlockedURLRejected() throws Exception {
        try {
            enableTenantAllowlist(new String[]{"*.allowed.example.com"});
            restAPIPublisher.validateOASDefinitionByURL(BLOCKED_URL);
            Assert.fail("Expected ApiException for OAS validate from blocked URL");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for OAS definition validate from blocked URL");
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "Host validation: allowlist enabled — KM discover with non-matching URL returns 400",
            dependsOnMethods = "testAllowlistEnabled_OASDefinitionFromBlockedURLRejected")
    public void testAllowlistEnabled_KeyManagerDiscoverWithBlockedURLRejected() throws Exception {
        try {
            enableTenantAllowlist(new String[]{"*.allowed.example.com"});
            restAPIAdmin.discoverKeyManager("https://attacker.corp/.well-known/openid-configuration", "OIDC");
            Assert.fail("Expected ApiException for KM discover with blocked URL");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for KM discover with blocked URL");
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "Host validation: allowlist enabled — MCP proxy create with non-matching URL returns 400",
            dependsOnMethods = "testAllowlistEnabled_KeyManagerDiscoverWithBlockedURLRejected")
    public void testAllowlistEnabled_MCPProxyCreateWithBlockedURLRejected() throws Exception {
        try {
            enableTenantAllowlist(new String[]{"*.allowed.example.com"});
            MCPServerProxyRequestDTO proxyRequest = new MCPServerProxyRequestDTO();
            proxyRequest.setUrl(BLOCKED_URL);
            restAPIPublisher.createMCPServerProxy(proxyRequest);
            Assert.fail("Expected ApiException for MCP proxy create with blocked URL");
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST,
                    "Expected HTTP 400 for MCP proxy create with blocked URL");
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @Test(groups = {"wso2.am"},
            description = "Host validation: super-tenant allowlist must not affect a new tenant whose config is at default",
            dependsOnMethods = "testAllowlistEnabled_MCPProxyCreateWithBlockedURLRejected")
    public void testAllowlistIsolation_NewTenantUnaffectedBySuperTenantAllowlist() throws Exception {
        try {
            // Enable allowlist on the super tenant only
            enableTenantAllowlist(new String[]{"*.allowed.example.com"});
            // super tenant is blocked by its own allowlist
            ApiEndpointValidationResponseDTO superDto =
                    restAPIPublisher.validateEndpointRaw(BLOCKED_URL, apiId);
            Assert.assertNotNull(superDto.getError(), "Expected host validation block for super tenant");
            Assert.assertTrue(superDto.getError().contains("not trusted"),
                    "Super tenant allowlist should block non-matching URL, got: " + superDto.getError());

            // New tenant has default config (no OutboundRequestSecurity / EnableHostAllowlist=false).
            ApiEndpointValidationResponseDTO tenantDto =
                    tenantPublisher.validateEndpointRaw(BLOCKED_URL, null);
            if (tenantDto.getError() != null) {
                Assert.assertFalse(tenantDto.getError().contains("not trusted"),
                        "New tenant with default config must not inherit super-tenant allowlist, got: "
                                + tenantDto.getError());
            }
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    private void enableTenantAllowlist(String[] patterns) throws Exception {
        JSONParser parser = new JSONParser();
        JSONObject config = (JSONObject) parser.parse(restAPIAdmin.getTenantConfig());

        JSONObject outboundSecurity = new JSONObject();
        outboundSecurity.put("EnableHostAllowlist", Boolean.TRUE);
        JSONArray patternArray = new JSONArray();
        for (String p : patterns) {
            patternArray.add(p);
        }
        outboundSecurity.put("HostAllowlistPatterns", patternArray);
        config.put("OutboundRequestSecurity", outboundSecurity);
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
        restoreOriginalTenantConfig();
        if (apiId != null) {
            restAPIPublisher.deleteAPI(apiId);
        }
        super.cleanUp();
    }
}
