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

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Publisher-plane MCP-server glue (ports the create/proxy half of MCPServerTestCase). Focuses on the PROXY
 * subtype: create an MCP server that fronts a third-party MCP server (the node mock MCP server built on the
 * official SDK) — this is the mode that exercises the gateway proxying a REAL, session-stateful MCP server
 * (the legacy used a stateless WireMock, a coverage gap this closes).
 */
public class MCPServerSteps {

    /**
     * Creates an MCP server by PROXYING a third-party MCP server (POST /mcp-servers/generate-from-mcp-server),
     * exposing the given comma-separated tool set. The gateway discovers the tools from {@code backendUrl} at
     * create time and, once deployed, proxies client MCP JSON-RPC to it. Non-asserting; stores the created id on
     * 2xx. The exposed operations control which discovered tools become MCP tools (docs "select tools to
     * import"); they are REQUIRED or the create returns "no URI templates were produced".
     *
     * @param backendUrl the third-party MCP server BASE URL (APIM appends {@code /mcp}); e.g. http://nodebackend:3020
     * @param tools      comma-separated tool names to expose (e.g. {@code echo,add})
     * @param idKey      context key to store the created MCP-server id under
     */
    @When("I create an MCP server proxy to {string} exposing tools {string} as {string}")
    public void iCreateMcpServerProxy(String backendUrl, String tools, String idKey) throws IOException {
        String requestJson = Utils.resolvePayloadPlaceholders(
                "{"
                        + "\"url\":\"" + backendUrl + "\","
                        + "\"securityInfo\":{\"isSecure\":false},"
                        + "\"additionalProperties\":{"
                        + "  \"name\":\"${UNIQUE:MCPProxy}\","
                        + "  \"displayName\":\"${UNIQUE:MCPProxy}\","
                        + "  \"version\":\"1.0.0\","
                        + "  \"context\":\"${UNIQUE:mcpProxyContext}\","
                        + "  \"policies\":[\"Unlimited\"],"
                        + "  \"endpointConfig\":{\"endpoint_type\":\"http\","
                        + "     \"production_endpoints\":{\"url\":\"" + backendUrl + "\"},"
                        + "     \"sandbox_endpoints\":{\"url\":\"" + backendUrl + "\"}},"
                        + "  \"operations\":" + buildToolOperations(tools)
                        + "}}");

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = Requests.post(Utils.getMCPServerProxyURL(Utils.getBaseUrl()), headers, requestJson,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
            TestContext.set(idKey, createdId);
            // Register for teardown so a scenario that fails before its explicit delete step still has the MCP
            // server swept by the @cleanup / @AfterClass hook.
            ResourceCleanup.register(ResourceCleanup.CREATED_MCP_SERVER_IDS, createdId);
        }
    }

    /**
     * Updates an MCP server's exposed tool set (PUT /mcp-servers/{id}): fetches the server, replaces its
     * {@code operations} with the given comma-separated tools, and PUTs it back. Non-asserting; stores the
     * response (which reflects the persisted operations). Ports the tool-update half of MCPServerTestCase.
     *
     * @param idKey context key holding the MCP-server id
     * @param tools comma-separated tool set the server should now expose
     */
    @When("I update the MCP server {string} to expose tools {string}")
    public void iUpdateMcpServerTools(String idKey, String tools) throws IOException {
        String id = TestContext.resolve(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        HttpResponse getResp = SimpleHTTPClient.getInstance()
                .doGet(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), headers);
        // Confirm the GET succeeded with a body BEFORE parsing — otherwise new JSONObject(null/"") throws an
        // opaque JSONException/NPE instead of a clear failure.
        org.testng.Assert.assertTrue(getResp != null && getResp.getResponseCode() >= 200
                        && getResp.getResponseCode() < 300 && getResp.getData() != null && !getResp.getData().isEmpty(),
                "Failed to fetch MCP server '" + id + "' before updating its tools: expected a 2xx response with a "
                        + "body, got " + (getResp == null ? "no response" : getResp.getResponseCode()
                        + " / body=" + getResp.getData()));
        JSONObject dto = new JSONObject(getResp.getData());
        dto.put("operations", new org.json.JSONArray(buildToolOperations(tools)));

        HttpResponse response = Requests.put(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), headers, dto.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Gates an MCP tool with a scope (PUT /mcp-servers/{id}): defines the scope on the MCP server (name +
     * role binding) and assigns it to the operation whose backend target is {@code tool}. Ports the scope half
     * of testScopesForProxySubtype. Non-asserting. After redeploy, a token WITHOUT the scope is refused (403)
     * at the tool call and one WITH it succeeds (200).
     *
     * @param idKey     context key holding the MCP-server id
     * @param tool      the tool (backend target) to gate
     * @param scopeName the scope name
     * @param role      role bound to the scope (e.g. {@code admin})
     */
    @When("I gate the MCP server {string} tool {string} with scope {string} bound to role {string}")
    public void iGateMcpTool(String idKey, String tool, String scopeName, String role) throws IOException {
        String id = TestContext.resolve(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        JSONObject dto = new JSONObject(SimpleHTTPClient.getInstance()
                .doGet(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), headers).getData());

        // Define the scope on the MCP server (inline local scope, bound to a role).
        JSONObject scopeDef = new JSONObject().put("scope", new JSONObject()
                .put("name", scopeName).put("displayName", scopeName)
                .put("description", "mcp scope enforcement")
                .put("bindings", new JSONArray().put(role)));
        dto.put("scopes", new JSONArray().put(scopeDef));

        // Assign the scope to the matching tool operation (the top-level target is the tool name for both the
        // proxy and DirectBackend subtypes).
        JSONArray ops = dto.getJSONArray("operations");
        for (int i = 0; i < ops.length(); i++) {
            JSONObject op = ops.getJSONObject(i);
            if (tool.equals(op.optString("target"))) {
                op.put("scopes", new JSONArray().put(scopeName));
            }
        }

        HttpResponse response = Requests.put(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), headers, dto.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Updates the business plans (subscription policies) an MCP server OFFERS (PUT /mcp-servers/{id}). Needed
     * before a subscription can use a bespoke low policy (a subscription may only use a tier the resource
     * offers). The policy list may contain {@code {{contextKey}}} placeholders (e.g. a runtime-created policy
     * name). Non-asserting.
     *
     * @param idKey        context key holding the MCP-server id
     * @param csvPolicies  comma-separated policy names (e.g. {@code Unlimited,{{subThrottlePolicyName}}})
     */
    @When("I update the MCP server {string} to offer policies {string}")
    public void iUpdateMcpServerPolicies(String idKey, String csvPolicies) throws IOException {
        String id = TestContext.resolve(idKey).toString();
        String resolved = Utils.resolveContextPlaceholders(csvPolicies);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        JSONObject dto = new JSONObject(SimpleHTTPClient.getInstance()
                .doGet(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), headers).getData());
        JSONArray policies = new JSONArray();
        for (String p : resolved.split(",")) {
            policies.put(p.trim());
        }
        dto.put("policies", policies);

        HttpResponse response = Requests.put(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), headers, dto.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Builds the MCP operations JSON array (one TOOL op per name) from a comma-separated tool list. */
    private String buildToolOperations(String csvTools) {
        StringBuilder sb = new StringBuilder("[");
        String[] names = csvTools.split(",");
        for (int i = 0; i < names.length; i++) {
            String t = names[i].trim();
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"feature\":\"TOOL\",\"backendOperationMapping\":{\"backendOperation\":{\"verb\":\"TOOL\",\"target\":\"")
                    .append(t).append("\"}}}");
        }
        return sb.append("]").toString();
    }

    /**
     * Creates an MCP server FROM an OpenAPI definition (POST /mcp-servers/generate-from-openapi, multipart
     * {@code file} = OAS + {@code additionalProperties}). The gateway generates a TOOL per OAS operation and, at
     * runtime, translates each {@code tools/call} into an HTTP request to the configured REST {@code backendUrl}
     * (MCP↔HTTP). Non-asserting; stores the created id on 2xx. Ports createMCPServerUsingOpenAPIDefinition.
     *
     * @param oasPath    classpath path to the OpenAPI definition
     * @param backendUrl the REST backend base URL the generated tools call at runtime
     * @param idKey      context key to store the created MCP-server id under
     */
    @When("I create an MCP server from openapi {string} with backend {string} as {string}")
    public void iCreateMcpFromOpenApi(String oasPath, String backendUrl, String idKey) throws IOException {
        File oasFile;
        try (java.io.InputStream in = getClass().getClassLoader().getResourceAsStream(oasPath)) {
            if (in == null) {
                throw new java.io.FileNotFoundException("OAS not found: " + oasPath);
            }
            oasFile = File.createTempFile("mcp-oas", ".json");
            oasFile.deleteOnExit();
            java.nio.file.Files.copy(in, oasFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        // The create maps selected OAS operations to tools; each op's backendOperation.target is the REST PATH
        // and verb is the HTTP method (DirectBackend shape). Without operations: "no URI templates were
        // produced". The petstore OAS exposes GET /pets (tool get_pets) and GET /pets/{petId} (get_pets_by_petId).
        String additionalProperties = Utils.resolvePayloadPlaceholders(
                "{"
                        + "\"name\":\"${UNIQUE:MCPFromOAS}\","
                        + "\"version\":\"1.0.0\","
                        + "\"context\":\"${UNIQUE:mcpOasContext}\","
                        + "\"policies\":[\"Unlimited\"],"
                        + "\"endpointConfig\":{\"endpoint_type\":\"http\","
                        + "  \"production_endpoints\":{\"url\":\"" + backendUrl + "\"},"
                        + "  \"sandbox_endpoints\":{\"url\":\"" + backendUrl + "\"}},"
                        + "\"operations\":["
                        + "  {\"feature\":\"TOOL\",\"backendOperationMapping\":{\"backendOperation\":{\"target\":\"/pets\",\"verb\":\"GET\"}}},"
                        + "  {\"feature\":\"TOOL\",\"backendOperationMapping\":{\"backendOperation\":{\"target\":\"/pets/{petId}\",\"verb\":\"GET\"}}}"
                        + "]"
                        + "}");

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("file", oasFile);
        Map<String, String> formFields = new HashMap<>();
        formFields.put("additionalProperties", additionalProperties);

        HttpResponse response = Requests.postMultipart(Utils.getMCPServerFromOpenAPIURL(Utils.getBaseUrl()), headers,
                files, formFields);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
            TestContext.set(idKey, createdId);
            // Register for teardown so a scenario that fails before its explicit delete step still has the MCP
            // server swept by the @cleanup / @AfterClass hook.
            ResourceCleanup.register(ResourceCleanup.CREATED_MCP_SERVER_IDS, createdId);
        }
    }

    /**
     * Creates an MCP server FROM an existing (published/deployed) API (POST /mcp-servers/generate-from-api,
     * JSON MCPServer body). Each tool maps to one of the API's resources via
     * {@code apiOperationMapping{apiId, backendOperation{target:<path>, verb}}}; at runtime the gateway routes
     * the tool call through that API to its backend. Non-asserting; stores the created id on 2xx. Ports
     * createMCPServerUsingAPI.
     *
     * @param apiKey context key holding the existing API id
     * @param paths  comma-separated GET resource paths to expose as tools (e.g. {@code /pets,/pets/{petId}})
     * @param idKey  context key to store the created MCP-server id under
     */
    @When("I create an MCP server from api {string} exposing paths {string} as {string}")
    public void iCreateMcpFromApi(String apiKey, String paths, String idKey) throws IOException {
        String apiId = TestContext.resolve(apiKey).toString();
        StringBuilder ops = new StringBuilder("[");
        String[] targets = paths.split(",");
        for (int i = 0; i < targets.length; i++) {
            if (i > 0) {
                ops.append(",");
            }
            ops.append("{\"feature\":\"TOOL\",\"apiOperationMapping\":{\"apiId\":\"").append(apiId)
                    .append("\",\"backendOperation\":{\"target\":\"").append(targets[i].trim())
                    .append("\",\"verb\":\"GET\"}}}");
        }
        ops.append("]");
        String requestJson = Utils.resolvePayloadPlaceholders(
                "{"
                        + "\"name\":\"${UNIQUE:MCPFromAPI}\","
                        + "\"version\":\"1.0.0\","
                        + "\"context\":\"${UNIQUE:mcpApiContext}\","
                        + "\"policies\":[\"Unlimited\"],"
                        + "\"operations\":" + ops
                        + "}");

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = Requests.post(Utils.getMCPServerFromAPIURL(Utils.getBaseUrl()), headers, requestJson,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
            TestContext.set(idKey, createdId);
            // Register for teardown so a scenario that fails before its explicit delete step still has the MCP
            // server swept by the @cleanup / @AfterClass hook.
            ResourceCleanup.register(ResourceCleanup.CREATED_MCP_SERVER_IDS, createdId);
        }
    }

    /**
     * Removes a tool from an MCP server (PUT /mcp-servers/{id}) by dropping the operation whose tool name
     * ({@code target}) matches — preserving the generated shape of the remaining operations (works for any
     * subtype). Non-asserting. Used to narrow the exposed tool set (docs "select tools to import").
     *
     * @param idKey context key holding the MCP-server id
     * @param tool  the tool name to remove
     */
    @When("I update the MCP server {string} removing tool {string}")
    public void iRemoveMcpTool(String idKey, String tool) throws IOException {
        String id = TestContext.resolve(idKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        JSONObject dto = new JSONObject(SimpleHTTPClient.getInstance()
                .doGet(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), headers).getData());
        JSONArray ops = dto.getJSONArray("operations");
        JSONArray kept = new JSONArray();
        for (int i = 0; i < ops.length(); i++) {
            JSONObject op = ops.getJSONObject(i);
            // Match by the operation's serialized JSON containing the tool name — robust across subtypes (the
            // tool name may sit in `target` or be derived; a substring match on the op is unambiguous here).
            if (op.toString().contains(tool)) {
                // Capture the removed operation verbatim so it can be re-added later (preserving its exact
                // subtype shape) — enables testing tool-update ADD as the inverse of REMOVE on any subtype.
                TestContext.set("removedMcpTool", op.toString());
            } else {
                kept.put(op);
            }
        }
        dto.put("operations", kept);

        HttpResponse response = Requests.put(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), headers, dto.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Re-adds the operation most recently removed by {@code I update the MCP server … removing tool …} back to
     * an MCP server (PUT /mcp-servers/{id}) — the inverse of remove, preserving the operation's exact subtype
     * shape (proxy TOOL-verb, DirectBackend path+verb, or API apiOperationMapping). Tests tool-update ADD on any
     * subtype. Non-asserting.
     *
     * @param idKey context key holding the MCP-server id
     */
    @When("I re-add the removed tool to the MCP server {string}")
    public void iReAddMcpTool(String idKey) throws IOException {
        String id = TestContext.resolve(idKey).toString();
        String removed = TestContext.resolve("removedMcpTool").toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        JSONObject dto = new JSONObject(SimpleHTTPClient.getInstance()
                .doGet(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), headers).getData());
        dto.getJSONArray("operations").put(new JSONObject(removed));

        HttpResponse response = Requests.put(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), headers, dto.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Asserts an MCP tool's PRODUCT-GENERATED input schema STRUCTURALLY — {@code JSONObject.similar}, so key
     * order is irrelevant (ports the legacy MCPServerTestCase schema check as hardened by upstream PR #14237,
     * whose exact-string compare flaked on unguaranteed key order). The docstring is the expected
     * schemaDefinition JSON. The GET is an intermediate read (local, not the published httpResponse).
     *
     * @param idKey context key holding the MCP-server id
     * @param tool  the tool whose operation carries the schema
     */
    @Then("the MCP server {string} tool {string} should have schema definition:")
    public void mcpToolShouldHaveSchemaDefinition(String idKey, String tool, String expectedSchemaJson)
            throws IOException {
        String id = TestContext.resolve(idKey).toString();
        HttpResponse resp = SimpleHTTPClient.getInstance()
                .doGet(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), publisherHeaders());
        Assert.assertTrue(resp != null && resp.getResponseCode() == 200 && resp.getData() != null
                        && !resp.getData().isEmpty(),
                "MCP server fetch failed for the schema check: got="
                        + (resp == null ? "null" : resp.getResponseCode() + "/" + resp.getData()));
        JSONArray ops = new JSONObject(resp.getData()).getJSONArray("operations");
        JSONObject match = null;
        for (int i = 0; i < ops.length(); i++) {
            if (ops.getJSONObject(i).toString().contains(tool)) {
                match = ops.getJSONObject(i);
                break;
            }
        }
        Assert.assertNotNull(match, "MCP server has no operation for tool '" + tool + "': " + ops);
        String actualSchema = match.optString("schemaDefinition", null);
        Assert.assertNotNull(actualSchema, "Operation for '" + tool + "' has no schemaDefinition: " + match);
        JSONObject actual = new JSONObject(actualSchema);
        JSONObject expected = new JSONObject(expectedSchemaJson);
        Assert.assertTrue(expected.similar(actual),
                "Tool '" + tool + "' schemaDefinition mismatch (structural): expected=" + expected
                        + " actual=" + actual);
    }

    /** Deletes an MCP server (DELETE /mcp-servers/{id}) — the explicit delete scenarios use to assert removal.
     *  Teardown is additionally covered by ResourceCleanup (the create side registers the server). */
    @When("I delete the MCP server {string}")
    public void iDeleteMcpServer(String idKey) throws IOException {
        Object id = TestContext.get(idKey);
        if (id == null) {
            return;
        }
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = Requests.delete(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id.toString()), headers);
    }

    // ---- MCP backend-endpoint management (/mcp-servers/{id}/backends) ----
    // An MCP server's backend endpoint is created implicitly with the server (proxy → the upstream MCP URL;
    // from-OpenAPI → the OpenAPI backend). The backends resource exposes list/get/update (there is no separate
    // add/delete — the backend's lifecycle is bound to the server). from-API MCP servers have NO own backend
    // (they proxy an existing API), so this resource applies only to the proxy and from-OpenAPI subtypes.

    private Map<String, String> publisherHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        return headers;
    }

    /**
     * GET /mcp-servers/{id}/backends — lists the MCP server's backend endpoints and stores the first backend's
     * id under {@code idKey} (for the subsequent get/update). Sets httpResponse for list assertions.
     */
    @When("I retrieve the backends of MCP server {string} and store the first backend id as {string}")
    public void iRetrieveMcpServerBackends(String mcpServerId, String idKey) throws IOException {
        String actualId = TestContext.resolve(mcpServerId).toString();
        HttpResponse response = Requests.get(Utils.getMCPServerBackendsURL(Utils.getBaseUrl(), actualId),
                publisherHeaders());
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            // The MCP backends collection is a BARE JSON array ([{id,name,endpointConfig,...}]), not a
            // {"list":[…]} envelope — index the array root directly.
            TestContext.set(idKey, Utils.extractValueFromPayload(response.getData(), "[0].id"));
        }
    }

    /** GET /mcp-servers/{id}/backends/{backendId} — retrieves a single MCP backend endpoint by id. */
    @When("I retrieve backend {string} of MCP server {string}")
    public void iRetrieveMcpServerBackend(String backendId, String mcpServerId) throws IOException {
        String actualId = TestContext.resolve(mcpServerId).toString();
        String actualBackendId = TestContext.resolve(backendId).toString();
        HttpResponse response = Requests.get(Utils.getMCPServerBackendByIdURL(Utils.getBaseUrl(), actualId, actualBackendId),
                publisherHeaders());
    }

    /** PUT /mcp-servers/{id}/backends/{backendId} — updates a single MCP backend endpoint (body = BackendDTO). */
    @When("I update backend {string} of MCP server {string} with payload {string}")
    public void iUpdateMcpServerBackend(String backendId, String mcpServerId, String payload) throws IOException {
        String actualId = TestContext.resolve(mcpServerId).toString();
        String actualBackendId = TestContext.resolve(backendId).toString();
        String jsonPayload = TestContext.resolve(payload).toString();
        HttpResponse response = Requests.put(Utils.getMCPServerBackendByIdURL(Utils.getBaseUrl(), actualId, actualBackendId),
                publisherHeaders(), jsonPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);
    }
}
