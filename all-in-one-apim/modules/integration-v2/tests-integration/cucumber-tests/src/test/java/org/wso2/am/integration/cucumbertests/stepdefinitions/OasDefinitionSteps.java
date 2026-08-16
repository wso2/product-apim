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
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * OpenAPI-definition FIDELITY glue — ports the assertion half of {@code OASTestCase} (its {@code OAS2Utils}/
 * {@code OAS3Utils}/{@code OASBaseUtils} validators), which asserted far more than "the response contains
 * paths": that the definition a plane serves genuinely matches the API's own DTO, that the DevPortal copy is
 * stripped of publisher-only information, that a stored definition still matches the one that was submitted,
 * and that whatever the product regenerates is itself a VALID OpenAPI document.
 *
 * <p>Every step here reads a definition from a CONTEXT KEY rather than from {@code httpResponse}. That is
 * deliberate: several of these checks compare TWO definitions (submitted vs stored, publisher vs devportal) and
 * one of them issues its own request (the validator round trip, which necessarily replaces {@code httpResponse}
 * per §7). Capturing each definition with the existing {@code I put the response payload in context as …} /
 * {@code I put JSON payload from file … in context as …} steps keeps the comparisons explicit and immune to the
 * stale-response trap.</p>
 *
 * <p>Definitions are compared as JSON (the publisher/devportal endpoints serve JSON for JSON-authored APIs), so
 * the same steps work for OAS 2, OAS 3 and OAS 3.1 — the properties asserted (operation targets/verbs, the
 * {@code x-auth-type}/{@code x-throttling-tier} operation extensions and the root {@code x-wso2-*} extensions)
 * are spelled identically in all three.</p>
 */
public class OasDefinitionSteps {

    /** The OpenAPI operation keys of a path item; anything else under a path (e.g. {@code parameters}) is not one. */
    private static final Set<String> HTTP_METHODS = new HashSet<>(Arrays.asList(
            "get", "put", "post", "delete", "patch", "head", "options", "trace"));

    /**
     * Root extensions the product strips from the DevPortal copy of a definition
     * ({@code OASParserUtil#removePublisherSpecificInfo}) — gateway/publisher-only information a consumer must
     * never see (backend endpoint URLs among it).
     */
    private static final List<String> PUBLISHER_ONLY_ROOT_EXTENSIONS = Arrays.asList(
            "x-wso2-cors", "x-wso2-auth-header", "x-wso2-throttling-tier", "x-throttling-tier",
            "x-wso2-production-endpoints", "x-wso2-sandbox-endpoints", "x-wso2-basePath", "x-wso2-transports",
            "x-wso2-application-security", "x-wso2-response-cache", "x-wso2-mutual-ssl");

    /**
     * Operation-level extensions the product strips from the DevPortal copy
     * ({@code OASParserUtil#removePublisherSpecificInfofromOperation}), plus the mediation script the DevPortal
     * layer removes separately ({@code APIUtil#removeXMediationScriptsFromSwagger}).
     */
    private static final List<String> PUBLISHER_ONLY_OPERATION_EXTENSIONS = Arrays.asList(
            "x-wso2-application-security", "x-wso2-sandbox-endpoints", "x-wso2-production-endpoints",
            "x-wso2-disable-security", "x-wso2-throttling-tier", "x-mediation-script");

    /**
     * Retrieves an API's DevPortal copy of its OpenAPI definition (devportal {@code GET /apis/{apiId}/swagger}).
     * The publisher-plane counterpart {@code I retrieve the swagger of "apis" resource …} serves the publisher
     * copy; legacy asserted BOTH planes on every definition change, and the two differ by design — the DevPortal
     * copy carries none of the publisher-only extensions the publisher copy does, which
     * {@link #definitionShouldNotExposePublisherOnlyExtensions} asserts as an observed plane difference rather
     * than an assumed one.
     *
     * <p>Distinct from the polling {@code the devportal swagger of API … should resolve its server host to …}
     * steps in {@code DevPortalSwaggerSteps}: those assert the resolved gateway URL of a DEPLOYED API and own
     * their own deadline; this one is a plain retrieve whose status/body the feature asserts.</p>
     *
     * @param apiIdKey context key holding the API id
     */
    @When("I retrieve the devportal swagger of API {string}")
    public void iRetrieveDevportalSwagger(String apiIdKey) throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        Requests.get(Utils.getDevportalApiSwaggerURL(Utils.getBaseUrl(), apiId), headers);
    }

    /**
     * Replaces the {@code operations} array of an API payload held in context with the operation list read from a
     * classpath JSON ARRAY file — the setup for the DTO-update path of legacy {@code OASTestCase#testAPIUpdate},
     * where the resource set changes through {@code PUT /apis/{id}} rather than through a swagger update, and the
     * product must regenerate the definition to match.
     *
     * <p>A narrow step on purpose: the shared {@code I set the JSON field … from file … in the payload …} parses
     * the file as a JSON OBJECT ({@code new JSONObject(content)}), so it cannot carry an array, and widening that
     * step's accepted input to keep one caller happy would change shared-glue semantics.</p>
     *
     * @param payloadKey        context key holding the API payload to mutate (written back under the same key)
     * @param operationsFile    classpath path of a JSON array of operation objects
     */
    @When("I replace the operations of the API payload {string} with the operations from file {string}")
    public void iReplaceOperationsOfApiPayload(String payloadKey, String operationsFile) throws IOException {

        JSONObject payload = new JSONObject(TestContext.resolve(payloadKey).toString());
        payload.put("operations", new JSONArray(Utils.readClasspathResource(operationsFile)));
        TestContext.set(Utils.normalizeContextKey(payloadKey), payload.toString());
    }

    /**
     * Feeds a definition held in context back through the publisher's own validator
     * ({@code POST /apis/validate-openapi}) and asserts it reports {@code isValid: true} — legacy's
     * {@code validateDefinition} round trip, applied to every definition the product GENERATED or REGENERATED
     * (after a create, an import, a swagger update or a DTO update). A definition the product itself emits but
     * cannot parse back is a real defect that no "contains paths" grep can catch.
     *
     * <p>This step issues a request, so per §7 it publishes that validator response as {@code httpResponse} —
     * it asserts the outcome itself and does not rely on a following {@code Then}, but a status/body assertion
     * placed after it would read the VALIDATOR's response, not the definition retrieve that preceded it.</p>
     *
     * @param definitionKey context key holding the OpenAPI definition text
     */
    @Then("The definition stored as {string} should be reported valid by the definition validator")
    public void definitionShouldBeReportedValid(String definitionKey) throws IOException {

        String definition = TestContext.resolve(definitionKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("file", definitionToTempFile(definition));
        HttpResponse response = Requests.postMultipart(Utils.getValidateOpenAPIURL(Utils.getBaseUrl()), headers,
                files, new HashMap<>());
        assertSuccessfulWithBody(response, "validate the definition stored as '" + definitionKey + "'");
        JSONObject body = Utils.requireJsonBody(response, "Validating the OpenAPI definition");
        Assert.assertTrue(body.has("isValid"), "Validation response carries no isValid flag: " + response.getData());
        Assert.assertTrue(body.getBoolean("isValid"),
                "The definition stored as '" + definitionKey + "' was rejected by the publisher's own OpenAPI "
                        + "validator: " + response.getData());
    }

    /**
     * Asserts the definition matches the API's DTO operation for operation — legacy's
     * {@code validateOperationCount} + {@code validateResourcesOfOASDefinition}:
     * <ul>
     *   <li>the definition declares EXACTLY as many operations as the DTO lists (neither a dropped resource nor
     *       a phantom one);</li>
     *   <li>every DTO operation has a matching path + verb in the definition;</li>
     *   <li>that operation's {@code x-auth-type} equals the DTO's {@code authType} and its
     *       {@code x-throttling-tier} equals the DTO's {@code throttlingPolicy};</li>
     *   <li>every scope the DTO attaches to the operation appears in the operation's {@code security} →
     *       {@code default} scope list.</li>
     * </ul>
     *
     * <p>The API DTO is read with a direct GET consumed locally (§7 — an intermediate read is NOT published as
     * {@code httpResponse}), so this assertion never disturbs the response under test.</p>
     *
     * @param definitionKey context key holding the OpenAPI definition text
     * @param apiIdKey      context key holding the id of the API the definition belongs to
     */
    @Then("The definition stored as {string} should declare exactly the operations of API {string}")
    public void definitionShouldDeclareExactlyTheOperationsOfApi(String definitionKey, String apiIdKey)
            throws IOException {

        JSONObject definition = definitionFromContext(definitionKey);
        JSONObject dto = retrieveApiDto(apiIdKey);
        JSONArray dtoOperations = dto.optJSONArray("operations");
        Assert.assertNotNull(dtoOperations, "API " + apiIdKey + " lists no operations: " + dto);

        JSONObject paths = definition.optJSONObject("paths");
        Assert.assertNotNull(paths, "Definition '" + definitionKey + "' has no paths section: " + definition);
        Assert.assertEquals(countOperations(paths), dtoOperations.length(),
                "The definition stored as '" + definitionKey + "' declares a different number of operations than "
                        + "API " + apiIdKey + " does. Definition operations=" + describeOperations(paths)
                        + ", DTO operations=" + dtoOperations);

        for (int i = 0; i < dtoOperations.length(); i++) {
            JSONObject dtoOperation = dtoOperations.getJSONObject(i);
            String target = dtoOperation.getString("target");
            String verb = dtoOperation.getString("verb");
            JSONObject pathItem = paths.optJSONObject(target);
            Assert.assertNotNull(pathItem, "Definition '" + definitionKey + "' has no path '" + target
                    + "' although the API DTO declares it. Definition paths=" + paths.keySet());
            JSONObject operation = pathItem.optJSONObject(verb.toLowerCase());
            Assert.assertNotNull(operation, "Definition '" + definitionKey + "' has no " + verb + " operation on '"
                    + target + "' although the API DTO declares it. Path item=" + pathItem);

            String expectedAuthType = dtoOperation.optString("authType", null);
            if (expectedAuthType != null) {
                Assert.assertEquals(operation.optString("x-auth-type", null), expectedAuthType,
                        "x-auth-type of " + verb + " " + target + " in '" + definitionKey
                                + "' does not match the API DTO's authType. Operation=" + operation);
            }
            String expectedTier = dtoOperation.optString("throttlingPolicy", null);
            if (expectedTier != null) {
                Assert.assertEquals(operation.optString("x-throttling-tier", null), expectedTier,
                        "x-throttling-tier of " + verb + " " + target + " in '" + definitionKey
                                + "' does not match the API DTO's throttlingPolicy. Operation=" + operation);
            }

            JSONArray dtoScopes = dtoOperation.optJSONArray("scopes");
            if (dtoScopes != null && dtoScopes.length() > 0) {
                Set<String> declaredScopes = defaultSecurityScopes(operation);
                for (int s = 0; s < dtoScopes.length(); s++) {
                    String scope = dtoScopes.getString(s);
                    Assert.assertTrue(declaredScopes.contains(scope),
                            "Scope '" + scope + "' the API DTO attaches to " + verb + " " + target
                                    + " is missing from that operation's security scopes in '" + definitionKey
                                    + "'. Declared scopes=" + declaredScopes + ", operation=" + operation);
                }
            }
        }
    }

    /**
     * Asserts EVERY operation of an API's DTO declares the given resource-level {@code authType} — the ABSOLUTE
     * half of legacy {@code APISecurityTestCase#testValidateSecurityOfResources}, which walks the DTO's operation
     * list and pins each {@code authType} to a literal ({@code "None"} for its security-disabled API,
     * {@code "Application & Application User"} for its security-enabled one).
     *
     * <p>Deliberately separate from
     * {@link #definitionShouldDeclareExactlyTheOperationsOfApi(String, String)}, which asserts the OAS
     * {@code x-auth-type} of each operation EQUALS the DTO's {@code authType}. That one is a consistency check
     * between two representations and would pass if BOTH drifted to the same wrong value; this one pins the value
     * itself. Used together they give legacy's pair of assertions (DTO {@code authType} and swagger
     * {@code x-auth-type}, each non-null and each the expected literal) without hardcoding the extension name in
     * two places.</p>
     *
     * <p>The DTO is read with a direct GET consumed locally (§7 — an intermediate read is NOT published as
     * {@code httpResponse}), so this assertion never disturbs the response under test.</p>
     *
     * @param apiIdKey         context key holding the API id
     * @param expectedAuthType the resource auth type every operation must declare (e.g. {@code None})
     */
    @Then("Every operation of API {string} should declare authType {string}")
    public void everyOperationShouldDeclareAuthType(String apiIdKey, String expectedAuthType) throws IOException {

        JSONObject dto = retrieveApiDto(apiIdKey);
        JSONArray operations = dto.optJSONArray("operations");
        Assert.assertNotNull(operations, "API " + apiIdKey + " lists no operations: " + dto);
        Assert.assertTrue(operations.length() > 0,
                "API " + apiIdKey + " has an EMPTY operations list, so 'every operation declares " + expectedAuthType
                        + "' would be vacuously true: " + dto);

        for (int i = 0; i < operations.length(); i++) {
            JSONObject operation = operations.getJSONObject(i);
            Assert.assertEquals(operation.optString("authType", null), expectedAuthType,
                    "authType of " + operation.optString("verb") + " " + operation.optString("target") + " on API "
                            + apiIdKey + " is not '" + expectedAuthType + "'. Operation=" + operation);
        }
    }

    /**
     * Asserts EVERY operation of an API declares the given resource-level {@code throttlingPolicy}.
     *
     * <p>The twin of {@link #everyOperationShouldDeclareAuthType(String, String)} for the throttling dimension,
     * and it exists for the unlimited-tier-disabled arc: with {@code enable_unlimited_tier = false} the product
     * stops DEFAULTING tier-less resources to {@code Unlimited} and falls through to the first key of a TreeMap of
     * the tenant's API-level policies, i.e. {@code 10KPerMin} for the shipped set
     * (10KPerMin / 20KPerMin / 50KPerMin).</p>
     *
     * <p>Pins the EXACT substituted tier rather than legacy's "not Unlimited"
     * ({@code assertNotEquals(operation.getThrottlingPolicy(), UNLIMITED)}). A not-equals assertion is satisfied by
     * any value at all — including a blank or a nonsense tier — so it cannot distinguish "the fall-through rule
     * works" from "the tier was lost"; and a change of substitution rule would pass unnoticed. Expressing it as a
     * per-operation list of expected values is not an option either: the consuming API is imported from a shipped
     * AI provider definition with ~60 paths, so the property under test is uniformity across all of them.</p>
     *
     * <p>Fails on an EMPTY operation list, so "every operation" can never be vacuously true. The DTO is read with
     * a direct GET consumed locally (§7), so this assertion never disturbs the response under test.</p>
     *
     * @param apiIdKey       context key holding the API id
     * @param expectedPolicy the resource-level throttling policy every operation must declare (e.g.
     *                       {@code 10KPerMin})
     */
    @Then("Every operation of API {string} should declare throttling policy {string}")
    public void everyOperationShouldDeclareThrottlingPolicy(String apiIdKey, String expectedPolicy)
            throws IOException {

        JSONObject dto = retrieveApiDto(apiIdKey);
        JSONArray operations = dto.optJSONArray("operations");
        Assert.assertNotNull(operations, "API " + apiIdKey + " lists no operations: " + dto);
        Assert.assertTrue(operations.length() > 0,
                "API " + apiIdKey + " has an EMPTY operations list, so 'every operation declares " + expectedPolicy
                        + "' would be vacuously true: " + dto);

        for (int i = 0; i < operations.length(); i++) {
            JSONObject operation = operations.getJSONObject(i);
            Assert.assertEquals(operation.optString("throttlingPolicy", null), expectedPolicy,
                    "throttlingPolicy of " + operation.optString("verb") + " " + operation.optString("target")
                            + " on API " + apiIdKey + " is not '" + expectedPolicy + "'. Operation=" + operation);
        }
    }

    /**
     * Asserts ONE operation's {@code authType} on the API's DTO, addressed by verb + target.
     *
     * <p>The counterpart of {@link #everyOperationShouldDeclareAuthType(String, String)} for the case where the
     * operations are deliberately NOT uniform: when a scenario flips the auth type of a single resource, the
     * interesting property is that the change is SCOPED — the target operation moved and its siblings did not. An
     * "every operation" assertion cannot express that, and flipping every operation just to satisfy it would
     * discard exactly the evidence that the update was resource-specific.
     *
     * <p>The DTO is read with a direct GET consumed locally (§7), so this assertion never disturbs the response
     * under test.
     *
     * @param verb             the operation's HTTP verb (e.g. {@code GET})
     * @param target           the operation's target path (e.g. {@code /customers/&#123;id&#125;})
     * @param apiIdKey         context key holding the API id
     * @param expectedAuthType the auth type that one operation must declare (e.g. {@code None})
     */
    @Then("The {string} operation on {string} of API {string} should declare authType {string}")
    public void operationShouldDeclareAuthType(String verb, String target, String apiIdKey, String expectedAuthType)
            throws IOException {

        JSONObject dto = retrieveApiDto(apiIdKey);
        JSONArray operations = dto.optJSONArray("operations");
        Assert.assertNotNull(operations, "API " + apiIdKey + " lists no operations: " + dto);

        for (int i = 0; i < operations.length(); i++) {
            JSONObject operation = operations.getJSONObject(i);
            if (verb.equalsIgnoreCase(operation.optString("verb")) && target.equals(operation.optString("target"))) {
                Assert.assertEquals(operation.optString("authType", null), expectedAuthType,
                        "authType of " + verb + " " + target + " on API " + apiIdKey + " is not '" + expectedAuthType
                                + "'. Operation=" + operation);
                return;
            }
        }
        Assert.fail("API " + apiIdKey + " has no " + verb + " operation on '" + target + "'. Operations=" + operations);
    }

    /**
     * Asserts the PUBLISHER copy of a definition carries the WSO2 management extensions the product stamps into
     * it, consistent with the API's DTO — legacy's {@code validateSwaggerExtensionDataInPublisher}:
     * {@code x-wso2-basePath} equals the API's {@code context/version}, {@code x-throttling-tier} equals the
     * API-level throttling policy, {@code x-wso2-auth-header} equals the DTO's authorization header when the API
     * sets one (and is ABSENT when it does not), and the CORS / production-endpoint / sandbox-endpoint /
     * transports extensions are present.
     *
     * <p>Like legacy, the auth-header check is driven off the DTO rather than hardcoded — the extension exists
     * only for an API that overrides the header. The endpoint-extension checks are unconditional because they
     * are what distinguishes a publisher copy from the stripped DevPortal one; an API genuinely without a
     * sandbox endpoint would need a separate expectation, which no scenario using this step has.</p>
     *
     * @param definitionKey context key holding the publisher OpenAPI definition text
     * @param apiIdKey      context key holding the id of the API the definition belongs to
     */
    @Then("The definition stored as {string} should carry the publisher extensions of API {string}")
    public void definitionShouldCarryPublisherExtensions(String definitionKey, String apiIdKey) throws IOException {

        JSONObject definition = definitionFromContext(definitionKey);
        JSONObject dto = retrieveApiDto(apiIdKey);

        String expectedBasePath = dto.getString("context") + "/" + dto.getString("version");
        Assert.assertEquals(definition.optString("x-wso2-basePath", null), expectedBasePath,
                "x-wso2-basePath of the definition stored as '" + definitionKey + "' does not match the API's "
                        + "context/version. Definition root keys=" + definition.keySet());

        String apiTier = dto.optString("apiThrottlingPolicy", null);
        if (apiTier != null && !apiTier.isBlank()) {
            Assert.assertEquals(definition.optString("x-throttling-tier", null), apiTier,
                    "x-throttling-tier of the definition stored as '" + definitionKey + "' does not match the "
                            + "API's apiThrottlingPolicy");
        }

        String authHeader = dto.optString("authorizationHeader", null);
        if (authHeader != null && !authHeader.isBlank()) {
            Assert.assertEquals(definition.optString("x-wso2-auth-header", null), authHeader,
                    "x-wso2-auth-header of the definition stored as '" + definitionKey + "' does not match the "
                            + "API's authorizationHeader");
        } else {
            Assert.assertFalse(definition.has("x-wso2-auth-header"),
                    "The API sets no authorizationHeader, so the definition stored as '" + definitionKey
                            + "' must carry no x-wso2-auth-header, but it does: "
                            + definition.opt("x-wso2-auth-header"));
        }

        for (String extension : Arrays.asList("x-wso2-cors", "x-wso2-production-endpoints",
                "x-wso2-sandbox-endpoints", "x-wso2-transports")) {
            Assert.assertTrue(definition.has(extension), "The publisher definition stored as '" + definitionKey
                    + "' is missing the " + extension + " extension. Root keys=" + definition.keySet());
        }
    }

    /**
     * Asserts the DevPortal copy of a definition exposes none of the publisher-only information that the
     * PUBLISHER copy of the same API actually carries — legacy's {@code validateSwaggerDataInStore}, and the half
     * the v2 port was missing entirely. This is a consumer-facing information-disclosure boundary, not
     * cosmetics: {@code x-wso2-production-endpoints} / {@code x-wso2-sandbox-endpoints} carry the BACKEND URLs
     * and {@code x-mediation-script} carries mediation source, none of which may reach a DevPortal consumer.
     *
     * <p><b>Stated relative to the publisher copy on purpose, and this is the whole point of the step.</b> An
     * absolute "the DevPortal copy has no {@code x-wso2-*}" assertion passes both when the product strips an
     * extension AND when nothing ever put it there — so it can certify a boundary nobody exercised. Here the
     * expectation set is DERIVED from what the publisher copy carries, and the step FAILS AS UNEXERCISED when
     * the publisher copy carries none of them, so a green result always means a real difference between the two
     * planes was observed.</p>
     *
     * <p>Only the {@code x-mediation-script} element is traced to a strip on the DevPortal path:
     * {@code store.v1 ApisApiServiceImpl.apisApiIdSwaggerGet:1118} calls
     * {@code APIUtil.removeXMediationScriptsFromSwagger} (and {@code removeInterceptorsFromSwagger}) on
     * {@code api.getSwaggerDefinition()}, which reaches the served body because
     * {@code APIConsumerImpl.getOpenAPIDefinitionForDeployment:4775} re-reads that same field before handing it
     * to {@code OAS3Parser.getOASDefinitionForStore:1136}. That store method does only
     * {@code updateOperations} (removes {@code x-mediation-script}, maps {@code x-wso2-scopes} into
     * {@code security}), {@code updateEndpoints} (rebuilds {@code servers} only) and
     * {@code updateSwaggerSecurityDefinitionForStore} — it does NOT call
     * {@code OASParserUtil.removePublisherSpecificInfo}, whose only callers are publisher-side
     * ({@code OAS3Parser} 798/1088/1104, reached from {@code populateCustomManagementInfo}). So for the other
     * extensions this step pins the OBSERVABLE plane difference rather than a mechanism, which is the property
     * that actually matters to a consumer.</p>
     *
     * @param definitionKey          context key holding the DevPortal OpenAPI definition text
     * @param publisherDefinitionKey context key holding the PUBLISHER copy of the same API's definition, which
     *                               supplies the expectation set
     */
    @Then("The definition stored as {string} should not expose the publisher-only extensions carried by {string}")
    public void definitionShouldNotExposePublisherOnlyExtensions(String definitionKey,
            String publisherDefinitionKey) {

        JSONObject definition = definitionFromContext(definitionKey);
        JSONObject publisherDefinition = definitionFromContext(publisherDefinitionKey);

        List<String> rootExpectations = new ArrayList<>();
        for (String extension : PUBLISHER_ONLY_ROOT_EXTENSIONS) {
            if (publisherDefinition.has(extension)) {
                rootExpectations.add(extension);
            }
        }

        JSONObject paths = definition.optJSONObject("paths");
        Assert.assertNotNull(paths, "Definition '" + definitionKey + "' has no paths section: " + definition);
        JSONObject publisherPaths = publisherDefinition.optJSONObject("paths");
        Assert.assertNotNull(publisherPaths,
                "Definition '" + publisherDefinitionKey + "' has no paths section: " + publisherDefinition);

        int operationExpectations = 0;
        for (String pathKey : publisherPaths.keySet()) {
            JSONObject publisherPathItem = publisherPaths.optJSONObject(pathKey);
            if (publisherPathItem == null) {
                continue;
            }
            for (String methodKey : publisherPathItem.keySet()) {
                if (!HTTP_METHODS.contains(methodKey.toLowerCase())) {
                    continue;
                }
                JSONObject publisherOperation = publisherPathItem.optJSONObject(methodKey);
                if (publisherOperation == null) {
                    continue;
                }
                JSONObject pathItem = paths.optJSONObject(pathKey);
                JSONObject operation = pathItem == null ? null : pathItem.optJSONObject(methodKey);
                for (String extension : PUBLISHER_ONLY_OPERATION_EXTENSIONS) {
                    if (!publisherOperation.has(extension)) {
                        continue;
                    }
                    operationExpectations++;
                    // An operation the DevPortal copy does not declare at all cannot expose the extension; the
                    // resource surfaces are compared by the same-operations step, so absence is not masked here.
                    if (operation == null) {
                        continue;
                    }
                    Assert.assertFalse(operation.has(extension), "Operation " + methodKey.toUpperCase() + " "
                            + pathKey + " of the DevPortal definition stored as '" + definitionKey + "' exposes "
                            + extension + " = " + operation.opt(extension) + ", which the publisher copy '"
                            + publisherDefinitionKey + "' also carries. Publisher-only information must not reach "
                            + "a DevPortal consumer.");
                }
            }
        }

        Assert.assertFalse(rootExpectations.isEmpty() && operationExpectations == 0,
                "UNEXERCISED: the publisher definition stored as '" + publisherDefinitionKey + "' carries none of "
                        + "the publisher-only extensions " + PUBLISHER_ONLY_ROOT_EXTENSIONS + " / "
                        + PUBLISHER_ONLY_OPERATION_EXTENSIONS + ", so asserting their absence from '"
                        + definitionKey + "' would prove nothing about what the product strips. Publisher root "
                        + "keys=" + publisherDefinition.keySet());

        for (String extension : rootExpectations) {
            Assert.assertFalse(definition.has(extension),
                    "The DevPortal definition stored as '" + definitionKey + "' exposes the publisher-only "
                            + "extension " + extension + " = " + definition.opt(extension) + ", which the "
                            + "publisher copy '" + publisherDefinitionKey + "' also carries. Publisher-only "
                            + "information must not reach a DevPortal consumer.");
        }
    }

    /**
     * Asserts two stored definitions are IDENTICAL, not merely operation-compatible — a semantic (canonicalised)
     * comparison via {@code JSONObject.similar}, so a re-serialisation that reorders keys is not a difference.
     *
     * <p>The operation-level check alone cannot see a rejected update that still corrupted the document on the way
     * out: descriptions, {@code x-} extensions, endpoint config and {@code servers} all live outside {@code paths}.
     * On failure the differing top-level sections are named, because a raw dump of two OpenAPI documents is
     * unreadable.</p>
     */
    @Then("The definitions stored as {string} and {string} should be identical")
    public void definitionsShouldBeIdentical(String firstKey, String secondKey) {

        JSONObject first = definitionFromContext(firstKey);
        JSONObject second = definitionFromContext(secondKey);
        if (first.similar(second)) {
            return;
        }
        Set<String> sections = new TreeSet<>(first.keySet());
        sections.addAll(second.keySet());
        List<String> differing = new ArrayList<>();
        for (String section : sections) {
            Object a = first.opt(section);
            Object b = second.opt(section);
            boolean same;
            if (a == null || b == null) {
                same = a == null && b == null;
            } else if (a instanceof JSONObject && b instanceof JSONObject) {
                same = ((JSONObject) a).similar(b);
            } else if (a instanceof JSONArray && b instanceof JSONArray) {
                same = ((JSONArray) a).similar(b);
            } else {
                same = a.equals(b);
            }
            if (!same) {
                differing.add(section);
            }
        }
        // Caller-neutral wording: this step only compares two stored definitions, so it must not narrate one
        // call site's story (a rejected update) for failures that may come from any other.
        // opt(), not optString(): two org.json implementations are on the test classpath (org.json and
        // android-json via jsonassert). The one that wins today renders a JSONObject fine, but android-json's
        // optString yields "" for a non-String — and every section reported here is an object or array.
        String section = differing.isEmpty() ? "info" : differing.get(0);
        Assert.fail("The definitions stored as '" + firstKey + "' and '" + secondKey + "' are not identical:"
                + " section(s) " + differing + " differ."
                + " '" + section + "' before=" + String.valueOf(first.opt(section))
                + " after=" + String.valueOf(second.opt(section)));
    }

    /**
     * Asserts two definitions declare the SAME resource surface: the same set of paths, and per path the same
     * set of verbs. Ports legacy's {@code validateUpdatedDefinition} — used both to confirm a submitted
     * definition survived the round trip into a plane's stored copy, and to confirm a REJECTED update left the
     * previously stored definition untouched.
     *
     * <p>Legacy additionally compared parsed {@code Operation} objects with {@code equals}. That is not
     * reproduced: the product legitimately augments an operation it stores (management extensions, generated
     * responses), so model equality would only hold for a definition it had just emitted itself — the resource
     * surface is the property that actually distinguishes "the update took" from "the update was lost". The
     * per-plane extension expectations are asserted separately by
     * {@link #definitionShouldCarryPublisherExtensions} and
     * {@link #definitionShouldNotExposePublisherOnlyExtensions}.</p>
     *
     * @param firstKey  context key holding the first definition
     * @param secondKey context key holding the second definition
     */
    @Then("The definitions stored as {string} and {string} should declare the same operations")
    public void definitionsShouldDeclareTheSameOperations(String firstKey, String secondKey) {

        JSONObject first = definitionFromContext(firstKey);
        JSONObject second = definitionFromContext(secondKey);
        JSONObject firstPaths = first.optJSONObject("paths");
        JSONObject secondPaths = second.optJSONObject("paths");
        Assert.assertNotNull(firstPaths, "Definition '" + firstKey + "' has no paths section: " + first);
        Assert.assertNotNull(secondPaths, "Definition '" + secondKey + "' has no paths section: " + second);

        Assert.assertEquals(new TreeSet<>(secondPaths.keySet()), new TreeSet<>(firstPaths.keySet()),
                "The definitions stored as '" + secondKey + "' and '" + firstKey + "' declare different paths");
        for (String pathKey : firstPaths.keySet()) {
            Assert.assertEquals(verbsOf(secondPaths.optJSONObject(pathKey)), verbsOf(firstPaths.optJSONObject(pathKey)),
                    "Path '" + pathKey + "' declares different verbs in '" + secondKey + "' than in '" + firstKey
                            + "'");
        }
    }

    /** Parses a definition held in context, failing clearly when the key holds something that is not JSON. */
    private JSONObject definitionFromContext(String definitionKey) {

        String definition = TestContext.resolve(definitionKey).toString();
        Assert.assertFalse(definition.isBlank(), "The definition stored as '" + definitionKey + "' is empty");
        try {
            return new JSONObject(definition);
        } catch (RuntimeException notJson) {
            throw new AssertionError("The value stored as '" + definitionKey + "' is not a JSON OpenAPI definition: "
                    + notJson.getMessage() + ". Value starts with: "
                    + definition.substring(0, Math.min(200, definition.length())), notJson);
        }
    }

    /**
     * Reads an API's publisher DTO with a direct GET whose body is consumed LOCALLY — an intermediate read, so
     * it must not touch {@code httpResponse} (§7).
     */
    private JSONObject retrieveApiDto(String apiIdKey) throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doGet(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", apiId), headers);
        assertSuccessfulWithBody(response, "read the DTO of API " + apiId);
        return Utils.requireJsonBody(response, "Retrieving the API DTO");
    }

    /** Guards a response before its body is parsed (§7). */
    private void assertSuccessfulWithBody(HttpResponse response, String what) {

        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isBlank(),
                "Failed to " + what + ": got " + (response == null ? "no response"
                        : response.getResponseCode() + " / " + response.getData()));
    }

    /** Total number of OpenAPI operations declared across every path of a definition. */
    private int countOperations(JSONObject paths) {

        int count = 0;
        for (String pathKey : paths.keySet()) {
            count += verbsOf(paths.optJSONObject(pathKey)).size();
        }
        return count;
    }

    /** The HTTP verbs a path item declares, upper-cased; ignores non-operation keys such as {@code parameters}. */
    private Set<String> verbsOf(JSONObject pathItem) {

        Set<String> verbs = new TreeSet<>();
        if (pathItem == null) {
            return verbs;
        }
        for (String key : pathItem.keySet()) {
            if (HTTP_METHODS.contains(key.toLowerCase())) {
                verbs.add(key.toUpperCase());
            }
        }
        return verbs;
    }

    /** {@code VERB path} listing of everything a definition declares — for a mismatch failure message. */
    private String describeOperations(JSONObject paths) {

        Set<String> operations = new TreeSet<>();
        for (String pathKey : paths.keySet()) {
            for (String verb : verbsOf(paths.optJSONObject(pathKey))) {
                operations.add(verb + " " + pathKey);
            }
        }
        return operations.toString();
    }

    /** The scopes an operation declares under its {@code security} → {@code default} requirement. */
    private Set<String> defaultSecurityScopes(JSONObject operation) {

        Set<String> scopes = new HashSet<>();
        JSONArray security = operation.optJSONArray("security");
        if (security == null) {
            return scopes;
        }
        for (int i = 0; i < security.length(); i++) {
            JSONObject requirement = security.optJSONObject(i);
            if (requirement == null) {
                continue;
            }
            JSONArray defaultScopes = requirement.optJSONArray("default");
            if (defaultScopes == null) {
                continue;
            }
            for (int s = 0; s < defaultScopes.length(); s++) {
                scopes.add(defaultScopes.getString(s));
            }
        }
        return scopes;
    }

    /**
     * Writes a definition held in context to a temp file for the multipart validate call. {@code Utils} offers
     * {@code classpathToTempFile} for a CLASSPATH resource, but the definition validated here is one the SERVER
     * just returned, so there is no shared helper to reuse.
     */
    private File definitionToTempFile(String definition) throws IOException {

        File temp = File.createTempFile("definition", ".json");
        temp.deleteOnExit();
        Files.write(temp.toPath(), definition.getBytes(StandardCharsets.UTF_8));
        return temp;
    }
}
