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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Publisher-plane MCP-server glue (ports the create/proxy half of MCPServerTestCase). Focuses on the PROXY
 * subtype: create an MCP server that fronts a third-party MCP server (the node mock MCP server built on the
 * official SDK) — this is the mode that exercises the gateway proxying a REAL, session-stateful MCP server
 * (the legacy used a stateless WireMock, a coverage gap this closes).
 */
public class MCPServerSteps {

    /** Page size for the devportal MCP-server listing read — comfortably above any one block's server count. */
    private static final int DEVPORTAL_LIST_PAGE_SIZE = 200;

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

        JSONObject dto = fetchMcpServerDto(id, headers, "before updating its tools");
        dto.put("operations", new org.json.JSONArray(buildToolOperations(tools)));

        HttpResponse response = Requests.put(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), headers, dto.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Persisted-side check: fetches the MCP server and asserts the stored operation for {@code tool} keeps the
     * full wrapped definition (inputSchema + title/annotations/_meta/outputSchema) — proving metadata survives
     * to storage, independent of the gateway read path.
     */
    @Then("the stored MCP server {string} tool {string} retains full metadata")
    public void storedMcpServerToolRetainsMetadata(String idKey, String tool) throws IOException {
        String id = TestContext.resolve(idKey).toString();
        JSONObject dto = fetchMcpServerDto(id, publisherHeaders(), "before checking preserved metadata");

        JSONArray ops = dto.getJSONArray("operations");
        JSONObject op = null;
        for (int i = 0; i < ops.length(); i++) {
            if (tool.equals(ops.getJSONObject(i).optString("target"))) {
                op = ops.getJSONObject(i);
                break;
            }
        }
        Assert.assertNotNull(op, "No stored operation for tool '" + tool + "': " + ops);

        // Assert exact VALUES (not just key presence) against the mock's get_weather definition.
        JSONObject schema = new JSONObject(op.getString("schemaDefinition"));
        Assert.assertEquals(schema.optString("title"), "Weather Lookup", "stored title mismatch: " + schema);
        JSONObject annotations = schema.getJSONObject("annotations");
        Assert.assertTrue(annotations.optBoolean("readOnlyHint", false),
                "stored annotations.readOnlyHint must be true: " + schema);
        Assert.assertEquals(annotations.optString("title"), "Weather Lookup",
                "stored annotations.title mismatch: " + schema);
        Assert.assertEquals(schema.getJSONObject("_meta").optString("category"), "weather",
                "stored _meta.category mismatch: " + schema);
        assertStoredObjectSchema(schema.getJSONObject("outputSchema"), "tempC", "number", "outputSchema", schema);
        assertStoredObjectSchema(schema.getJSONObject("inputSchema"), "city", "string", "inputSchema", schema);
    }

    /** Asserts a stored object schema: {@code type} object, {@code prop} present with {@code propType}, in required. */
    private void assertStoredObjectSchema(JSONObject schema, String prop, String propType, String label, Object ctx) {
        Assert.assertEquals(schema.optString("type"), "object", "stored " + label + ".type must be object: " + ctx);
        Assert.assertEquals(schema.getJSONObject("properties").getJSONObject(prop).optString("type"), propType,
                "stored " + label + ".properties." + prop + ".type mismatch: " + ctx);
        JSONArray required = schema.optJSONArray("required");
        Assert.assertTrue(required != null && required.toList().contains(prop),
                "stored " + label + ".required must contain " + prop + ": " + ctx);
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

        JSONObject dto = fetchMcpServerDto(id, headers, "before gating a tool with a scope");

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

        JSONObject dto = fetchMcpServerDto(id, headers, "before updating its business plans");
        JSONArray policies = new JSONArray();
        for (String p : resolved.split(",")) {
            policies.put(p.trim());
        }
        dto.put("policies", policies);

        HttpResponse response = Requests.put(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), headers, dto.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Sets an MCP server's SERVER-LEVEL (advanced/"API-level") throttling policy (PUT /mcp-servers/{id}) — the
     * {@code throttlingPolicy} field, which caps the whole server irrespective of the subscription tier. Ports
     * the API-level half of testThrottlingForProxySubtype. Non-asserting; the response reflects the persisted
     * policy so the feature can pin it.
     *
     * @param idKey      context key holding the MCP-server id
     * @param policyName the advanced throttling policy name (may carry {@code {{contextKey}}} placeholders)
     */
    @When("I update the MCP server {string} to use API-level throttling policy {string}")
    public void iSetMcpServerLevelThrottlingPolicy(String idKey, String policyName) throws IOException {
        String id = TestContext.resolve(idKey).toString();
        String resolved = Utils.resolveContextPlaceholders(policyName);
        Map<String, String> headers = publisherHeaders();

        JSONObject dto = fetchMcpServerDto(id, headers, "before setting its API-level throttling policy");
        dto.put("throttlingPolicy", resolved);
        // Neutralise every operation-level policy explicitly, so the server-level policy is the only thing that
        // can throttle. The product would do this anyway — a non-null server-level policy overwrites each
        // operation's throttlingPolicy with it — but stating it here keeps the intent independent of that.
        JSONArray ops = dto.getJSONArray("operations");
        for (int i = 0; i < ops.length(); i++) {
            ops.getJSONObject(i).put("throttlingPolicy", "Unlimited");
        }

        Requests.put(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), headers, dto.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Sets an OPERATION-LEVEL throttling policy on one MCP tool (PUT /mcp-servers/{id}) and CLEARS the
     * SERVER-LEVEL policy, so any throttling observed afterwards can only come from the operation's own policy.
     * Ports the operation-level half of testThrottlingForProxySubtype. Non-asserting.
     * <p>
     * The server-level policy must be sent as JSON {@code null}, not {@code "Unlimited"}: a NON-NULL
     * server-level {@code throttlingPolicy} overwrites EVERY operation's {@code throttlingPolicy} with it, so a
     * per-tool policy survives only when the server level is unset. Verified against 4.7.0-SNAPSHOT — sending
     * {@code "Unlimited"} at the server level echoes the tool back as {@code Unlimited}, and sending the policy
     * at the server level forces it onto every tool including the ones left at {@code Unlimited}. This is why
     * the legacy called {@code setThrottlingPolicy(null)} before setting the operation policy.
     *
     * @param idKey      context key holding the MCP-server id
     * @param tool       the tool (operation target) to bind the policy to
     * @param policyName the advanced throttling policy name (may carry {@code {{contextKey}}} placeholders)
     */
    @When("I update the MCP server {string} setting tool {string} throttling policy {string}")
    public void iSetMcpOperationThrottlingPolicy(String idKey, String tool, String policyName) throws IOException {
        String id = TestContext.resolve(idKey).toString();
        String resolved = Utils.resolveContextPlaceholders(policyName);
        Map<String, String> headers = publisherHeaders();

        JSONObject dto = fetchMcpServerDto(id, headers, "before setting an operation-level throttling policy");
        dto.put("throttlingPolicy", JSONObject.NULL);
        JSONArray ops = dto.getJSONArray("operations");
        boolean matched = false;
        for (int i = 0; i < ops.length(); i++) {
            JSONObject op = ops.getJSONObject(i);
            if (tool.equals(op.optString("target"))) {
                op.put("throttlingPolicy", resolved);
                matched = true;
            } else {
                op.put("throttlingPolicy", "Unlimited");
            }
        }
        Assert.assertTrue(matched, "MCP server " + id + " has no operation with target '" + tool
                + "' to bind a throttling policy to; operations=" + ops);

        Requests.put(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), headers, dto.toString(),
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

        JSONObject dto = fetchMcpServerDto(id, headers, "before removing a tool");
        JSONArray ops = dto.getJSONArray("operations");
        JSONArray kept = new JSONArray();
        for (int i = 0; i < ops.length(); i++) {
            JSONObject op = ops.getJSONObject(i);
            // Match by the op's top-level target (the tool name for both proxy and DirectBackend subtypes —
            // see the scope-assignment step above); a substring match on the whole op could select the wrong
            // one if the tool name appears in another op's schema/description.
            if (tool.equals(op.optString("target"))) {
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

        JSONObject dto = fetchMcpServerDto(id, headers, "before re-adding the removed tool");
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
        JSONObject match = fetchOperation(idKey, tool, "the schema check");
        String actualSchema = match.optString("schemaDefinition", null);
        // Reject a BLANK schemaDefinition too, not just an absent one: optString returns "" for a present-but-
        // empty value, which would slip past a null check into an opaque JSONException at the parse below.
        Assert.assertTrue(actualSchema != null && !actualSchema.isBlank(),
                "Operation for '" + tool + "' has no (or a blank) schemaDefinition: " + match);
        JSONObject actual = new JSONObject(actualSchema);
        JSONObject expected = new JSONObject(expectedSchemaJson);
        Assert.assertTrue(expected.similar(actual),
                "Tool '" + tool + "' schemaDefinition mismatch (structural): expected=" + expected
                        + " actual=" + actual);
    }

    /**
     * Asserts an MCP tool's DESCRIPTION equals {@code expected} EXACTLY — the sibling of
     * {@link #mcpToolShouldHaveSchemaDefinition} for the other half of the legacy tool-fidelity check
     * (schemaDefinition + description). A description is what an MCP client shows the model to decide whether to
     * call the tool, so it is the tool's contract as much as its schema: a {@code contains} check on the whole DTO
     * would pass on a description that merely mentions the text, and would not notice one silently inherited from
     * another operation. The GET is an intermediate read (local, not the published httpResponse).
     *
     * @param idKey    context key holding the MCP-server id
     * @param tool     the tool whose operation carries the description
     * @param expected the exact expected description
     */
    @Then("the MCP server {string} tool {string} should have description {string}")
    public void mcpToolShouldHaveDescription(String idKey, String tool, String expected) throws IOException {
        JSONObject match = fetchOperation(idKey, tool, "the description check");
        Assert.assertEquals(match.optString("description", null), expected,
                "Tool '" + tool + "' description mismatch: " + match);
    }

    /**
     * Asserts the MCP server is visible to a CONSUMER in the devportal: it appears in the devportal MCP-server
     * LISTING exactly once and its detail representation agrees with the publisher DTO (id / name / version, the
     * devportal {@code context} carrying the version, and {@code lifeCycleStatus} equal to the publisher
     * {@code state}). Ports the {@code isMCPServerAvailableInList} visibility check of
     * testMCPServerSubscribeAndInvokeForDirectBackendSubtype.
     *
     * <p>FINDING (verify-first): an MCP server has its OWN devportal collection — {@code /mcp-servers}. It is NOT
     * in {@code /apis}, and {@code /apis/{mcpId}} answers 404, so the listing below is the only way a consumer
     * discovers it. The listing is Solr-indexed and therefore eventually consistent (observed answering
     * {@code count:0} for a just-published server), so it is POLLED — a single read would flake.</p>
     *
     * @param idKey          context key holding the MCP-server id
     * @param timeoutSeconds how long to wait for the devportal index to catch up
     */
    @Then("The devportal should report MCP server {string} exactly once with the same fields within {int} seconds")
    public void theDevportalShouldReportMcpServerOnce(String idKey, int timeoutSeconds)
            throws IOException, InterruptedException {
        String id = TestContext.resolve(idKey).toString();
        JSONObject publisherDto = fetchMcpServerDto(id, publisherHeaders(), "for the devportal visibility check");

        Map<String, String> devportalHeaders = new HashMap<>();
        devportalHeaders.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        String listUrl = Utils.getDevportalMcpServerListURL(Utils.getBaseUrl(), DEVPORTAL_LIST_PAGE_SIZE);
        // EVERY page: a single-page read reports 0 for a server sitting past the first page, and the retry then
        // burns its whole window before failing as though the server were never published.
        Integer matches = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> countMcpServerAcrossAllPages(devportalHeaders, id), count -> count != null && count == 1);
        Assert.assertNotNull(matches, "MCP server " + id + " (" + publisherDto.optString("name")
                + ") could not be read from the devportal MCP-server listing " + listUrl
                + " within " + timeoutSeconds + "s");
        Assert.assertEquals(matches.intValue(), 1, "MCP server " + id + " (" + publisherDto.optString("name")
                + ") should appear exactly once across the whole devportal MCP-server listing, but was found "
                + matches + " time(s)");

        HttpResponse detail = SimpleHTTPClient.getInstance()
                .doGet(Utils.getDevportalMcpServerDetailURL(Utils.getBaseUrl(), id), devportalHeaders);
        Assert.assertTrue(detail != null && detail.getResponseCode() == 200 && detail.getData() != null
                        && !detail.getData().isBlank(),
                "Devportal MCP server " + id + " is not retrievable: got="
                        + (detail == null ? "null" : detail.getResponseCode() + "/" + detail.getData()));
        JSONObject portal = new JSONObject(detail.getData());

        assertSameNonBlankField(portal, publisherDto, "id", id);
        assertSameNonBlankField(portal, publisherDto, "name", id);
        assertSameNonBlankField(portal, publisherDto, "version", id);
        assertSameNonBlankField(portal, publisherDto, "lifeCycleStatus", id);
        // The devportal context carries the version: /<context>/<version> (or {version} substituted in place).
        String context = publisherDto.getString("context");
        String version = publisherDto.getString("version");
        String expectedContext = context.contains("{version}")
                ? context.replace("{version}", version) : context + "/" + version;
        Assert.assertEquals(portal.optString("context"), expectedContext,
                "Devportal context does not carry the version for MCP server " + id);
    }

    /**
     * Walks the whole devportal MCP-server listing and totals the entries carrying {@code id}. Returns null when a
     * page is unreadable or malformed so the caller's retry keeps waiting rather than throwing mid-loop. Paging
     * stops on a short/empty page or once {@code pagination.total} is consumed.
     */
    private static Integer countMcpServerAcrossAllPages(Map<String, String> headers, String id) throws IOException {
        int found = 0;
        int offset = 0;
        while (true) {
            String pageUrl = Utils.getDevportalMcpServerListURL(Utils.getBaseUrl(), DEVPORTAL_LIST_PAGE_SIZE, offset);
            HttpResponse page = SimpleHTTPClient.getInstance().doGet(pageUrl, headers);
            if (page == null || page.getResponseCode() != 200
                    || page.getData() == null || page.getData().isBlank()) {
                return null;
            }
            int onThisPage;
            int total;
            try {
                JSONObject body = new JSONObject(page.getData());
                JSONArray list = body.optJSONArray("list");
                if (list == null) {
                    return null;
                }
                for (int i = 0; i < list.length(); i++) {
                    if (id.equals(list.getJSONObject(i).optString("id"))) {
                        found++;
                    }
                }
                onThisPage = list.length();
                JSONObject pagination = body.optJSONObject("pagination");
                total = pagination == null ? -1 : pagination.optInt("total", -1);
            } catch (org.json.JSONException malformedDuringWarmup) {
                return null;
            }
            offset += onThisPage;
            if (onThisPage == 0 || onThisPage < DEVPORTAL_LIST_PAGE_SIZE || (total >= 0 && offset >= total)) {
                return found;
            }
        }
    }

    /**
     * Asserts a field holds the same NON-BLANK value on both planes. optString returns "" when absent, so a bare
     * compare would pass vacuously if both responses dropped the field.
     */
    private void assertSameNonBlankField(JSONObject portal, JSONObject publisherDto, String field, String id) {
        String publisherValue = publisherDto.optString(field);
        String portalValue = portal.optString(field);
        Assert.assertFalse(publisherValue.isBlank(),
                "Publisher DTO carries no '" + field + "' for MCP server " + id + ": " + publisherDto);
        Assert.assertFalse(portalValue.isBlank(),
                "Devportal DTO carries no '" + field + "' for MCP server " + id + ": " + portal);
        Assert.assertEquals(portalValue, publisherValue,
                "Devportal '" + field + "' does not match the publisher DTO for MCP server " + id);
    }

    /**
     * Replaces an MCP server's whole tool set (PUT /mcp-servers/{id}) with exactly two operations, submitted in
     * this order: (1) a BRAND-NEW tool for {@code newToolSpec}, and (2) the server's existing {@code keptTool}
     * with its description rewritten. Every other operation is dropped. Ports the update half of
     * testMCPServerToolOperationsForDirectBackendSubtype / testToolsForExistingApiSubtype, which submit exactly
     * {@code [addOp, updateOp]}.
     *
     * @param idKey       context key holding the MCP-server id
     * @param newToolSpec the new tool's backend operation as {@code "<VERB> <path>"} (e.g. {@code DELETE /oldpets})
     * @param keptTool    tool name of the existing operation to keep (submitted second)
     * @param description the description to set on {@code keptTool}
     */
    @When("I update the MCP server {string} replacing its tools with {string} then {string} re-described as {string}")
    public void iReplaceMcpToolsInOrder(String idKey, String newToolSpec, String keptTool, String description)
            throws IOException {
        String id = TestContext.resolve(idKey).toString();
        Map<String, String> headers = publisherHeaders();

        HttpResponse dtoResp = SimpleHTTPClient.getInstance().doGet(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id),
                headers);
        Assert.assertTrue(dtoResp != null && dtoResp.getResponseCode() >= 200 && dtoResp.getResponseCode() < 300
                        && dtoResp.getData() != null && !dtoResp.getData().isBlank(),
                "Could not read MCP server " + id + " before replacing its tools; got="
                        + (dtoResp == null ? "null" : dtoResp.getResponseCode() + "/" + dtoResp.getData()));
        JSONObject dto = new JSONObject(dtoResp.getData());

        String[] spec = newToolSpec.trim().split("\\s+", 2);
        Assert.assertEquals(spec.length, 2, "New tool spec must be \"<VERB> <path>\", got: " + newToolSpec);
        String verb = spec[0].trim();
        String path = spec[1].trim();

        JSONArray existing = dto.getJSONArray("operations");
        JSONObject keptOp = null;
        JSONObject shapeSource = existing.length() > 0 ? existing.getJSONObject(0) : null;
        for (int i = 0; i < existing.length(); i++) {
            JSONObject op = existing.getJSONObject(i);
            if (keptTool.equals(op.optString("target"))) {
                keptOp = op;
            }
        }
        Assert.assertNotNull(keptOp, "MCP server " + id + " has no operation with target '" + keptTool
                + "'; operations=" + existing);
        keptOp.put("description", description);

        // Mirror the existing operations' mapping shape so one step works for both subtypes (see javadoc). Both
        // keys are always PRESENT — the one that does not apply to the subtype is serialized as JSON null — so
        // the shape has to be picked by which one holds an object, not by which key exists.
        JSONObject apiMapping = shapeSource == null ? null : shapeSource.optJSONObject("apiOperationMapping");
        JSONObject backendMapping = shapeSource == null ? null
                : shapeSource.optJSONObject("backendOperationMapping");
        JSONObject backendOperation = new JSONObject().put("target", path).put("verb", verb);
        JSONObject newOp = new JSONObject().put("feature", "TOOL");
        if (apiMapping != null) {
            newOp.put("apiOperationMapping", new JSONObject()
                    .put("apiId", apiMapping.getString("apiId"))
                    .put("backendOperation", backendOperation));
        } else if (backendMapping != null) {
            newOp.put("backendOperationMapping", new JSONObject()
                    .put("backendId", backendMapping.getString("backendId"))
                    .put("backendOperation", backendOperation));
        } else {
            Assert.fail("MCP server " + id + " operations carry neither apiOperationMapping nor "
                    + "backendOperationMapping, so a new tool cannot be shaped for this subtype: " + existing);
        }

        dto.put("operations", new JSONArray().put(newOp).put(keptOp));
        Requests.put(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), headers, dto.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Asserts the MCP server in the last response exposes EXACTLY these tools, in EXACTLY this order.
     * The publisher returns the operations in the order their URL mappings were inserted, which is the order they
     * were submitted, so a scenario that submitted a known order can pin it here.
     *
     * @param csvTargets comma-separated tool names (operation targets) in the exact expected order
     */
    @Then("the MCP server operations should be exactly {string} in that order")
    public void mcpOperationsShouldBeExactlyInOrder(String csvTargets) {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isBlank(),
                "No successful MCP server response to read operations from; got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));

        JSONArray operations = new JSONObject(response.getData()).getJSONArray("operations");
        List<String> actual = new ArrayList<>();
        for (int i = 0; i < operations.length(); i++) {
            actual.add(operations.getJSONObject(i).optString("target"));
        }
        List<String> expected = Arrays.stream(csvTargets.split(",")).map(String::trim)
                .collect(Collectors.toList());
        Assert.assertEquals(actual, expected, "MCP server tool order mismatch — expected " + expected
                + " but the publisher returned " + actual);
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

    /**
     * Guarded GET of an MCP server plus the lookup of ONE of its operations by tool name — the single read the
     * per-tool fidelity assertions (schemaDefinition / description) share, so neither re-implements the guard or
     * the target match. Fails clearly when the tool is absent rather than returning null into an opaque NPE.
     */
    private JSONObject fetchOperation(String idKey, String tool, String purpose) throws IOException {
        String id = TestContext.resolve(idKey).toString();
        JSONObject dto = fetchMcpServerDto(id, publisherHeaders(), "for " + purpose);
        JSONArray ops = dto.getJSONArray("operations");
        for (int i = 0; i < ops.length(); i++) {
            if (tool.equals(ops.getJSONObject(i).optString("target"))) {
                return ops.getJSONObject(i);
            }
        }
        Assert.fail("MCP server " + id + " has no operation for tool '" + tool + "': " + ops);
        return null;
    }

    /**
     * Guarded GET of an MCP server's DTO: asserts a 2xx response carrying a non-blank body BEFORE parsing, so an
     * error body is never silently parsed as the DTO and PUT back (which would corrupt the update), and a null
     * response fails with a clear message rather than an NPE. {@code purpose} is folded into that message.
     */
    private JSONObject fetchMcpServerDto(String id, Map<String, String> headers, String purpose) throws IOException {
        HttpResponse resp = SimpleHTTPClient.getInstance()
                .doGet(Utils.getMCPServerByIdURL(Utils.getBaseUrl(), id), headers);
        Assert.assertTrue(resp != null && resp.getResponseCode() >= 200 && resp.getResponseCode() < 300
                        && resp.getData() != null && !resp.getData().isBlank(),
                "Failed to fetch MCP server '" + id + "' " + purpose + ": expected a 2xx response with a body, got "
                        + (resp == null ? "no response" : resp.getResponseCode() + " / body=" + resp.getData()));
        return new JSONObject(resp.getData());
    }

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
