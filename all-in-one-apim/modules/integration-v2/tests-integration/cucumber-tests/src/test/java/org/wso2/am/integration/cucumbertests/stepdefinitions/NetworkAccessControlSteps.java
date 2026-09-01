/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Step definitions for the outbound network access-control (host-validation) URL-gate ports. These drive the
 * publisher/admin entry points that validate a USER-SUPPLIED URL before any outbound fetch (endpoint validation,
 * Key Manager create/update/discover, OpenAPI/GraphQL validate-from-URL, MCP proxy). They are the new-framework
 * port of the HostValidation*TestCase suites; the definition/remote-reference gate (OpenAPI $ref, nested WSDL
 * xsd:import) is covered by the SafeRefResolution feature ports instead.
 *
 * <p>Each step performs the request through {@link Requests}, which stores the response for the shared
 * {@code The response status code should be} / {@code The response should [not] contain} assertions - so the
 * feature file, not this class, asserts the outcome. Negative creates use {@code registerIfCreated}, a no-op on
 * the expected refusal, so an unexpectedly-created resource is still swept in teardown.
 */
public class NetworkAccessControlSteps {

    private static final String ORIGINAL_TENANT_CONFIG_KEY = "nacOriginalTenantConfig";
    private static final String POLICY_ADMIN_KEY = "nacPolicyAdmin";

    // Endpoint validation of a user-supplied URL (POST /apis/validate-endpoint). Returns 200 with an error
    // field when the URL is blocked, so the feature asserts the body, not a 4xx.
    @When("I validate the endpoint url {string}")
    public void iValidateEndpointUrl(String endpointUrl) throws IOException {
        Requests.post(Utils.getValidateEndpointURL(Utils.getBaseUrl(), endpointUrl, ""),
                Identity.publisherHeaders(), "", Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    // OAS definition validation from a user-supplied URL (POST /apis/validate-openapi, multipart url field).
    @When("I validate the openapi definition from url {string}")
    public void iValidateOpenApiFromUrl(String url) throws IOException {
        Map<String, String> formFields = new HashMap<>();
        formFields.put("url", url);
        Requests.postMultipart(Utils.getValidateOpenAPIURL(Utils.getBaseUrl()), Identity.publisherHeaders(),
                new HashMap<>(), formFields);
    }

    // GraphQL schema validation from a user-supplied URL (POST /apis/validate-graphql-schema). Returns 200 with
    // isValid=false + an error message when the URL is blocked.
    @When("I validate the graphql schema from url {string}")
    public void iValidateGraphqlFromUrl(String url) throws IOException {
        Map<String, String> formFields = new HashMap<>();
        formFields.put("url", url);
        formFields.put("useIntrospection", "false");
        Requests.postMultipart(Utils.getValidateGraphQLSchemaURL(Utils.getBaseUrl()), Identity.publisherHeaders(),
                new HashMap<>(), formFields);
    }

    // Key Manager create whose OAuth endpoints all resolve to a given (blocked) host (POST /key-managers).
    @When("I create a key manager with endpoints on host {string}")
    public void iCreateKeyManagerOnHost(String host) throws IOException {
        JSONObject payload = keyManagerPayload("NacBlockedKM", host, null);
        ResourceCleanup.registerIfCreated(Constants.CREATED_KEY_MANAGER_IDS,
                Requests.post(Utils.getKeyManagersURL(Utils.getBaseUrl()), Identity.adminHeaders(),
                        payload.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON), "id");
    }

    // Key Manager create with allow-listed endpoints but a blocked JWKS certificate URL (POST /key-managers).
    @When("I create a key manager with a blocked jwks certificate url {string}")
    public void iCreateKeyManagerWithBlockedJwks(String jwksUrl) throws IOException {
        JSONObject certs = new JSONObject();
        certs.put("type", "JWKS");
        certs.put("value", jwksUrl);
        JSONObject payload = keyManagerPayload("NacBlockedJwksKM", "api.allowed.example.com", certs);
        ResourceCleanup.registerIfCreated(Constants.CREATED_KEY_MANAGER_IDS,
                Requests.post(Utils.getKeyManagersURL(Utils.getBaseUrl()), Identity.adminHeaders(),
                        payload.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON), "id");
    }

    // Key Manager created with allow-listed endpoints (passes), then updated so its token endpoint resolves to a
    // blocked host (PUT /key-managers/{id}). The UPDATE is the asserted call; the create must succeed first.
    @When("I create a key manager on host {string} then update its token endpoint to {string}")
    public void iCreateThenUpdateKeyManager(String host, String blockedTokenEndpoint) throws IOException {
        JSONObject payload = keyManagerPayload("NacUpdateKM", host, null);
        HttpResponse create = Requests.post(Utils.getKeyManagersURL(Utils.getBaseUrl()), Identity.adminHeaders(),
                payload.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        if (create.getResponseCode() != 201) {
            return;
        }
        Object id = Utils.extractValueFromPayload(create.getData(), "id");
        ResourceCleanup.register(Constants.CREATED_KEY_MANAGER_IDS, id);
        JSONObject toUpdate = new JSONObject(create.getData());
        toUpdate.put("tokenEndpoint", blockedTokenEndpoint);
        Requests.put(Utils.getKeyManagerByIdURL(Utils.getBaseUrl(), id.toString()), Identity.adminHeaders(),
                toUpdate.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    // Key Manager discovery of an OIDC/OAuth well-known configuration from a user-supplied URL
    // (POST /key-managers/discover, multipart url + type).
    @When("I discover key manager configuration from url {string} of type {string}")
    public void iDiscoverKeyManagerFromUrl(String url, String type) throws IOException {
        Map<String, String> formFields = new HashMap<>();
        formFields.put("url", url);
        formFields.put("type", type);
        Requests.postMultipart(Utils.getKeyManagersURL(Utils.getBaseUrl()) + "/discover", Identity.adminHeaders(),
                new HashMap<>(), formFields);
    }

    // MCP server proxy create to a user-supplied backend URL (POST /mcp-servers/proxy).
    @When("I create an mcp server proxy to url {string}")
    public void iCreateMcpServerProxyToUrl(String url) throws IOException {
        JSONObject request = new JSONObject();
        request.put("url", url);
        request.put("securityInfo", new JSONObject());
        JSONObject props = new JSONObject();
        props.put("name", "NacProxyBlocked");
        props.put("context", "/nacproxyblocked");
        props.put("version", "1.0.0");
        request.put("additionalProperties", props);
        ResourceCleanup.registerIfCreated(Constants.CREATED_API_IDS,
                Requests.post(Utils.getMCPServerProxyURL(Utils.getBaseUrl()), Identity.publisherHeaders(),
                        request.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON), "id");
    }

    // API create whose production backend resolves to a user-supplied (blocked) host (POST /apis).
    @When("I create an api with a blocked backend {string}")
    public void iCreateApiWithBlockedBackend(String url) throws IOException {
        JSONObject api = new JSONObject();
        api.put("name", "NacBlockedBackendApi");
        api.put("context", "/nacblockedbackend");
        api.put("version", "1.0.0");
        api.put("policies", new JSONArray().put("Unlimited"));
        JSONObject operation = new JSONObject();
        operation.put("verb", "GET");
        operation.put("target", "/*");
        operation.put("authType", "Application & Application User");
        operation.put("throttlingPolicy", "Unlimited");
        api.put("operations", new JSONArray().put(operation));
        api.put("endpointConfig", httpEndpointConfig(url));
        ResourceCleanup.registerIfCreated(Constants.CREATED_API_IDS,
                Requests.post(Utils.getAPICreateEndpointURL(Utils.getBaseUrl(), "apis"), Identity.publisherHeaders(),
                        api.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON), "id");
    }

    // Updates an existing API's production endpoint to a user-supplied (blocked) host (PUT /apis/{id}). The API is
    // fetched first so the update carries a full, valid definition with only the endpoint changed.
    @When("I set the production endpoint of api {string} to {string}")
    public void iSetApiProductionEndpoint(String apiRef, String url) throws IOException {
        String apiId = TestContext.resolve(apiRef).toString();
        HttpResponse current = Requests.get(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", apiId),
                Identity.publisherHeaders());
        JSONObject api = new JSONObject(current.getData());
        api.put("endpointConfig", httpEndpointConfig(url));
        Requests.put(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", apiId), Identity.publisherHeaders(),
                api.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    // MCP server create from an existing-API subtype whose backend resolves to a user-supplied (blocked) host
    // (POST /mcp-servers). The endpoint is validated before the referenced API is resolved.
    @When("I create an mcp server from api with a blocked backend {string}")
    public void iCreateMcpServerFromApiWithBlockedBackend(String url) throws IOException {
        JSONObject mcp = new JSONObject();
        mcp.put("name", "NacMcpFromApi");
        mcp.put("context", "/nacmcpfromapi");
        mcp.put("version", "1.0.0");
        mcp.put("endpointConfig", httpEndpointConfig(url));
        JSONObject subtype = new JSONObject();
        subtype.put("subtype", "EXISTING_API");
        mcp.put("subtypeConfiguration", subtype);
        ResourceCleanup.registerIfCreated(Constants.CREATED_API_IDS,
                Requests.post(Utils.getMCPServerFromAPIURL(Utils.getBaseUrl()), Identity.publisherHeaders(),
                        mcp.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON), "id");
    }

    // WSDL API import from a user-supplied URL with the additional properties read from a classpath FILE
    // (POST /apis/import-wsdl). The shared "from URL" step resolves its properties argument as a context key,
    // so this file-based variant is used where the properties live in a fixture file.
    @When("I import a wsdl api from url {string} with properties file {string}")
    public void iImportWsdlFromUrlWithPropsFile(String wsdlUrl, String propsPath) throws IOException {
        String props = Utils.resolvePayloadPlaceholders(Utils.readClasspathResource(propsPath));
        Map<String, String> formFields = new HashMap<>();
        formFields.put("url", wsdlUrl);
        formFields.put("additionalProperties", props);
        formFields.put("implementationType", "SOAP");
        ResourceCleanup.registerIfCreated(Constants.CREATED_API_IDS,
                Requests.postMultipart(Utils.getImportWsdlURL(Utils.getBaseUrl()), Identity.publisherHeaders(),
                        new HashMap<>(), formFields), "id");
    }

    // GraphQL API import from a user-supplied endpoint URL with the additional properties read from a classpath
    // FILE (POST /apis/import-graphql-schema). File-based variant of the shared "from endpoint URL" step.
    @When("I import a graphql api from url {string} with properties file {string}")
    public void iImportGraphqlFromUrlWithPropsFile(String url, String propsPath) throws IOException {
        String props = Utils.resolvePayloadPlaceholders(Utils.readClasspathResource(propsPath));
        Map<String, String> formFields = new HashMap<>();
        formFields.put("type", "GRAPHQL");
        formFields.put("url", url);
        formFields.put("additionalProperties", props);
        ResourceCleanup.registerIfCreated(Constants.CREATED_API_IDS,
                Requests.postMultipart(Utils.getGraphQLSchema(Utils.getBaseUrl()), Identity.publisherHeaders(),
                        new HashMap<>(), formFields), "id");
    }

    // Applies a tenant-level allow policy (NetworkSecurityAccessControl) to the acting tenant's tenant-conf.json,
    // capturing the original so a later "remove" step can restore it. The admin actor and its tenant are given.
    @Given("I apply a tenant allow policy for hosts {string} as {string}")
    public void iApplyTenantAllowPolicy(String hosts, String adminActor) throws IOException {
        Identity.setActingActor(adminActor);
        HttpResponse current = Requests.get(Utils.getTenantConfigURL(Utils.getBaseUrl()), Identity.adminHeaders());
        TestContext.set(ORIGINAL_TENANT_CONFIG_KEY, current.getData());
        TestContext.set(POLICY_ADMIN_KEY, adminActor);

        JSONObject config = new JSONObject(current.getData());
        JSONObject accessControl = new JSONObject();
        accessControl.put("Mode", "allow");
        JSONArray hostsArray = new JSONArray();
        for (String host : hosts.split(",")) {
            hostsArray.put(host.trim());
        }
        accessControl.put("Hosts", hostsArray);
        config.put("NetworkSecurityAccessControl", accessControl);
        Requests.put(Utils.getTenantConfigURL(Utils.getBaseUrl()), Identity.adminHeaders(), config.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    // Restores the tenant-conf.json captured by the matching "apply" step, so the mutation cannot bleed into a
    // sibling scenario sharing the container.
    @Given("I remove the applied tenant policy")
    public void iRemoveAppliedTenantPolicy() throws IOException {
        Object original = TestContext.get(ORIGINAL_TENANT_CONFIG_KEY);
        Object adminActor = TestContext.get(POLICY_ADMIN_KEY);
        if (original == null || adminActor == null) {
            return;
        }
        Identity.setActingActor(adminActor.toString());
        Requests.put(Utils.getTenantConfigURL(Utils.getBaseUrl()), Identity.adminHeaders(), original.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** An HTTP endpoint-config block with the given production URL (sandbox mirrors production). */
    private JSONObject httpEndpointConfig(String url) {
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        JSONObject production = new JSONObject();
        production.put("url", url);
        endpointConfig.put("production_endpoints", production);
        return endpointConfig;
    }

    /**
     * Builds a WSO2-IS Key Manager payload whose four OAuth endpoints resolve to {@code host}. When {@code certs}
     * is given it is attached as the JWKS certificate block (for the certificate-URL host-validation port).
     */
    private JSONObject keyManagerPayload(String name, String host, JSONObject certs) {
        JSONObject payload = new JSONObject();
        payload.put("name", name);
        payload.put("displayName", name);
        payload.put("type", "WSO2-IS");
        payload.put("introspectionEndpoint", "https://" + host + "/oauth2/introspect");
        payload.put("issuer", "https://" + host + "/oauth2/token");
        payload.put("clientRegistrationEndpoint", "https://" + host + "/keymanager-operations/dcr/register");
        payload.put("tokenEndpoint", "https://" + host + "/oauth2/token");
        payload.put("revokeEndpoint", "https://" + host + "/oauth2/revoke");
        payload.put("consumerKeyClaim", "azp");
        payload.put("scopesClaim", "scope");
        payload.put("enabled", true);
        payload.put("availableGrantTypes", new JSONArray());
        JSONObject additionalProperties = new JSONObject();
        additionalProperties.put("Username", "admin");
        additionalProperties.put("Password", "admin");
        additionalProperties.put("self_validate_jwt", true);
        payload.put("additionalProperties", additionalProperties);
        if (certs != null) {
            payload.put("certificates", certs);
        }
        return payload;
    }
}
