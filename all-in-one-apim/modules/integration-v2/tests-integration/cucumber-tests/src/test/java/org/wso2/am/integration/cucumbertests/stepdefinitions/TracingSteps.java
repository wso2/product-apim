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
import io.cucumber.java.en.Then;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.SkipException;
import org.testcontainers.containers.Container;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.MoesifManagementApiClient;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.testcontainers.ApimRuntime;
import org.wso2.am.testcontainers.DynamicOtlpCollector;
import org.wso2.am.testcontainers.DynamicZipkin;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Steps that read APIM's exported spans back, for both OTLP transports.
 *
 * <p>Two blocks feed these steps, and which block a scenario runs in decides which side is exercised:
 * {@code initOtlpCollector} (OTLP/gRPC into a REAL Jaeger) or the live Moesif block (OTLP/HTTP into the real
 * {@code api.moesif.net}). They are separate blocks because a server has one remote tracer and the protocol is
 * fixed at startup.
 *
 * <p><b>The assertion boundary.</b> Span SEMANTICS — span kind, parent/child linkage, attribute values and
 * types, the {@code process.pid} resource attribute — are read from the real collector, through the steps below
 * that talk to Jaeger's query API. The live Moesif leg asserts DELIVERY through Moesif's own Management API
 * instead: Moesif's search API returns modelled events, not the bytes the gateway sent, so it can never see the
 * wire facts (method, path, content type, gzip, the Moesif auth header) — those were the OTLP/HTTP leg's
 * subject and are no longer asserted now that the hermetic ingest stand-in is retired.
 *
 * <p><b>How a trace is scoped to the scenario that produced it.</b> Jaeger's tag filter matches values
 * EXACTLY, and the exact form of APIM's {@code url.path} (whether a tenant API's path carries the
 * {@code /t/<tenant>} prefix) is a product detail rather than a test input. So the query is deliberately
 * coarse — service name plus a lookback — and the precise scoping happens here: the trace picked is the one
 * whose spans carry the scenario's UNIQUE API context in a tag value. Because the context is generated per
 * scenario, a sibling scenario's trace cannot match, which is what makes the later exact-value assertions
 * sound rather than merely probable. The exact-value assertions themselves stay independent of this filter.
 *
 * <p><b>"Any tag matches" is deliberate.</b> Jaeger reports a span's {@code span.kind} TWICE when the
 * exporter both sets the OTLP {@code kind} field and carries an explicit {@code span.kind} attribute: the
 * collector maps the field to a tag of its own and stores the attribute alongside it. Asserting a single tag
 * would therefore fail against a correct build, so tag lookups return the first match and tolerate
 * duplicates.
 */
/**
 * Deliberately NOT {@code extends BaseSteps}: Cucumber 7 rejects a glue class that extends a class
 * declaring step definitions or hooks (InvalidMethodException), because the inherited steps would be
 * registered twice. This class uses nothing from BaseSteps anyway, and every other step class in this
 * module is standalone for the same reason.
 */
public class TracingSteps {

    private static final Logger log = LoggerFactory.getLogger(TracingSteps.class);

    /** Block infra published by {@code BlockLifecycleListener}; absent means the block did not opt in. */
    private static final String COLLECTOR_KEY = "blockOtlpCollector";
    private static final String CONTAINER_KEY = "blockApimContainer";

    /**
     * Scenario state, set by the first step of each scenario and read by the rest. A runner's local scope is
     * shared by all of its scenarios (§10), so a trace left behind by an earlier row must never satisfy a
     * later one — every scenario re-sets it in its first step, and nothing reads it before that.
     */
    private static final String TRACE_KEY = "otlpCollectorTrace";
    /** The trace the live leg read back through Moesif's own Management API, spans and all. */
    private static final String MOESIF_TRACE_KEY = "moesifTrace";
    /**
     * The spans resolved by {@link #assertGatewaySpanChain}, keyed by ROLE. Features address a gateway span by
     * one of these aliases rather than by its operation name, because the product renames two of its spans.
     */
    private static final String SPANS_KEY = "otlpCollectorSpans";
    /** The API context the scoped trace was selected by, re-used when re-polling for a complete trace. */
    private static final String TRACE_CONTEXT_KEY = "otlpCollectorTraceContext";
    private static final String ALIAS_REQUEST = "request";
    private static final String ALIAS_RESOURCE = "resource";
    private static final String ALIAS_BACKEND = "backend";

    /** The {@code service.name} APIM's gateway puts on the tracer resource (its SERVICE_NAME constant). */
    private static final String SERVICE_NAME = "API:Gateway";
    /** Cap on traces per query. Only this scenario's context-matched trace is read out of the batch. */
    private static final int QUERY_LIMIT = 20;

    // ---- OTLP/gRPC leg: spans read from the real collector -------------------------------------------

    @Then("the OTLP collector should have received a trace for the API context {string}")
    public void assertCollectorReceivedTrace(String context) throws InterruptedException {
        // Features pass the published context as "{{contextKey}}", so the {{...}} must be resolved to the value
        // before it is used as a search needle — searching for the literal placeholder matches nothing and looks
        // like a product failure. Every invocation step in APIInvocationSteps resolves for the same reason.
        String apiContext = Utils.resolveContextPlaceholders(context);
        String queryBaseUrl = collector().getQueryBaseUrl();
        JSONObject trace = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                () -> findTraceForContext(queryBaseUrl, apiContext, null),
                found -> found != null);
        Assert.assertNotNull(trace, "the OTLP collector never received a trace carrying the API context '"
                + apiContext + "' within the propagation window. Either no span was exported for this invocation "
                + "(check the block overlay enabled the tracer and that the API was actually invoked), or the "
                + "exporter's endpoint did not resolve — an endpoint naming an absent host drops spans with no "
                + "visible error anywhere in APIM.");
        logTagsOf(trace);
        TestContext.set(TRACE_KEY, trace);
        TestContext.set(TRACE_CONTEXT_KEY, apiContext);
    }

    /**
     * Resolves the gateway's span chain by ROLE and asserts its shape, because the gateway RENAMES two of its
     * spans on the way out. {@code APIMgtLatencySynapseHandler.handleResponseOutFlow} finishes the request span
     * with {@code Util.updateOperation(span, apiName--version--tenantDomain)}, and
     * {@code GatewayUtils.setAPIResource} finishes the resource span with
     * {@code (httpMethod--electedResource)}. So the literals "API:Response_Latency" and "API:Resource" never reach
     * a collector — only e.g. {@code APIMTest_ab12cd34_2--1.0.0--carbon.super} and
     * {@code GET--/customers/{id}} do. Asserting on the literals would fail against a correct build.
     *
     * <p>The request span's name is derived from what the scenario itself created — the RESOLVED creation payload
     * (its {@code ${UNIQUE:APIMTest}} placeholder is already substituted) plus the acting actor's tenant domain —
     * which is the only non-circular way to state the expectation, since the name is a pure function of inputs
     * the test owns. The two remaining spans are found by walking parent/child links, so the step asserts the
     * documented chain {@code request -> API resource -> backend} (APIMgtLatencyStatsHandler:93 makes the
     * resource span a child of the response span; handleRequestOutFlow makes the backend span a child of the
     * resource span). The backend call is therefore a GRANDCHILD of the request span, not a direct child.
     *
     * <p>Each span is then stored under a stable alias, which is what the attribute steps below address. Naming a
     * span by role keeps the feature readable and survives any future renaming of the product's span names.
     */
    @Then("the trace should have the gateway's request span chain for the API created with payload {string}")
    public void assertGatewaySpanChain(String payloadKey) throws InterruptedException {
        JSONObject payload = new JSONObject(String.valueOf(TestContext.resolve(payloadKey)));
        String apiName = payload.optString("name");
        String version = payload.optString("version");
        Assert.assertFalse(apiName.isEmpty() || version.isEmpty(), "the creation payload stored as '" + payloadKey
                + "' carries no name/version, so the gateway's request span name cannot be derived. Its keys: "
                + payload.keySet());
        String tenant = Identity.actingTenantDomain();
        String expectedRequestSpan = apiName + "--" + version + "--" + tenant;

        // Re-poll for a trace that carries our context AND the request span. Waiting only for "a trace mentioning
        // the context" is not enough: the collector exposes a trace as soon as its first span lands, so a trace can
        // still be mid-export and hold one or two spans when it is first matched. Reading spans from a partial
        // trace turns an export-timing detail into an intermittent product-looking failure — observed as a trace
        // whose only span was API:CORS_Request_Latency.
        String apiContext = String.valueOf(TestContext.resolve(TRACE_CONTEXT_KEY));
        String queryBaseUrl = collector().getQueryBaseUrl();
        JSONObject complete = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                () -> findTraceForContext(queryBaseUrl, apiContext, expectedRequestSpan),
                found -> found != null);
        Assert.assertNotNull(complete, "the collector received a trace carrying the API context '" + apiContext
                + "' but never one containing the gateway's request span '" + expectedRequestSpan + "'. Either the "
                + "request span is not exported at all, or the trace stayed incomplete. Spans seen in the best "
                + "attempt: " + operationNames());
        TestContext.set(TRACE_KEY, complete);
        logTagsOf(complete);

        JSONObject requestSpan = spanByOperationName(expectedRequestSpan);
        Assert.assertNotNull(requestSpan, "the gateway exported no request span named '" + expectedRequestSpan
                + "'. APIM renames API:Response_Latency to <apiName>--<version>--<tenantDomain> when it finishes it "
                + "(APIMgtLatencySynapseHandler.handleResponseOutFlow), so that is the only name it can carry. "
                + "Spans present: " + operationNames() + ". Trace id " + requireTrace().optString("traceID"));

        List<JSONObject> resourceSpans = childrenOf(requestSpan);
        Assert.assertEquals(resourceSpans.size(), 1, "expected exactly one child of the request span '"
                + expectedRequestSpan + "' — the API resource span — but found " + resourceSpans.size() + ": "
                + operationNamesOf(resourceSpans) + ". Every other gateway span hangs off the resource span, so a "
                + "count other than 1 means the handler chain changed shape.");

        JSONObject resourceSpan = resourceSpans.get(0);
        // The resource span is the PARENT of every gateway mediation span, not just the backend call: key
        // validation, throttling, CORS, the two mediation phases and Google Analytics all hang off it. So the
        // backend span is selected BY NAME from among the resource span's children rather than by being the only
        // child. What this pins is the linkage: the backend call is recorded under the API resource span, and
        // therefore under the request span — not as a root span of its own.
        JSONObject backendSpan = null;
        for (JSONObject child : childrenOf(resourceSpan)) {
            if ("API:Backend_Latency".equals(child.optString("operationName"))) {
                backendSpan = child;
                break;
            }
        }
        Assert.assertNotNull(backendSpan, "no span named 'API:Backend_Latency' is a child of the API resource span '"
                + resourceSpan.optString("operationName") + "', so the backend call is not recorded under the traced "
                + "request. Its children: " + operationNamesOf(childrenOf(resourceSpan)) + ".");

        JSONObject spans = new JSONObject();
        spans.put(ALIAS_REQUEST, requestSpan);
        spans.put(ALIAS_RESOURCE, resourceSpan);
        spans.put(ALIAS_BACKEND, backendSpan);
        TestContext.set(SPANS_KEY, spans);
    }

    @Then("the span {string} in that trace should have the attribute {string} with value {string}")
    public void assertSpanAttribute(String spanName, String key, String expected) {
        Object actual = tagValue(requireSpan(spanName), key);
        Assert.assertNotNull(actual, "the span '" + spanName + "' carries no '" + key + "' tag. Its tags: "
                + tagsOf(requireSpan(spanName)));
        Assert.assertEquals(String.valueOf(actual), expected, "the span '" + spanName + "' tag '" + key
                + "' expected '" + expected + "' but was '" + actual + "'. Its tags: " + tagsOf(requireSpan(spanName)));
    }

    @Then("the span {string} in that trace should have the attribute {string} with numeric value {int}")
    public void assertSpanNumericAttribute(String spanName, String key, int expected) {
        JSONObject span = requireSpan(spanName);
        Object actual = tagValue(span, key);
        Assert.assertNotNull(actual, "the span '" + spanName + "' carries no '" + key + "' tag. Its tags: "
                + tagsOf(span));
        // A JSON NUMBER, not the string "200". The semconv attribute is an integer and the collector reports the
        // type it actually received, so comparing as text would let a string-typed export satisfy an integer
        // contract — exactly the kind of widened check §12 forbids.
        Assert.assertTrue(actual instanceof Number, "the tag '" + key + "' on span '" + spanName + "' is not numeric: "
                + "got " + actual + " (" + actual.getClass().getSimpleName() + "). Its tags: " + tagsOf(span));
        Assert.assertEquals(((Number) actual).longValue(), (long) expected, "the span '" + spanName + "' tag '"
                + key + "' expected the number " + expected + " but was " + actual + ". Its tags: " + tagsOf(span));
    }

    /**
     * Baseline form of the path assertion: the attribute is present and carries THIS scenario's UNIQUE API
     * context, without pinning the surrounding string. The exact-value variants above stay valid on the very
     * value the collector reports; this one exists because the surrounding form (whether a tenant API's path
     * carries the {@code /t/<tenant>} prefix) is a product rendering rather than a test input, so it is
     * MEASURED on the first run and the exact string pinned afterwards.
     */
    @Then("the span {string} in that trace should have the attribute {string} whose value contains the API context stored as {string}")
    public void assertSpanAttributeContainsContext(String spanName, String key, String contextKey) {
        String context = String.valueOf(TestContext.resolve(contextKey));
        JSONObject span = requireSpan(spanName);
        Object actual = tagValue(span, key);
        Assert.assertNotNull(actual, "the span '" + spanName + "' carries no '" + key + "' tag at all. Its tags: "
                + tagsOf(span));
        Assert.assertTrue(String.valueOf(actual).contains(context), "the span '" + spanName + "' tag '" + key
                + "' does not carry this scenario's API context '" + context + "'; got '" + actual + "'. Either the "
                + "attribute is not the request path, or the traced invocation is a different one. Its tags: "
                + tagsOf(span));
    }

    @Then("the span {string} in that trace should not have the attribute {string}")
    public void assertSpanLacksAttribute(String spanName, String key) {
        JSONObject span = requireSpan(spanName);
        Object actual = tagValue(span, key);
        Assert.assertNull(actual, "the span '" + spanName + "' still carries the pre-patch attribute '" + key
                + "' (value " + actual + "); the attribute was renamed to the semconv form and the old key must "
                + "be gone. All tags: " + tagsOf(span));
    }

    @Then("the span {string} in that trace should be the parent of the span {string}")
    public void assertSpanIsParentOf(String parentName, String childName) {
        JSONObject child = requireSpan(childName);
        String parentSpanId = parentSpanIdOf(child);
        Assert.assertNotNull(parentSpanId, "the span '" + childName + "' has no CHILD_OF reference, so the backend "
                + "call is not recorded as a child of the response span. Its references: " + child.opt("references"));
        JSONObject parent = spanById(child, parentSpanId);
        Assert.assertNotNull(parent, "the span '" + childName + "' points at parent span id " + parentSpanId
                + ", which is not present in this trace. Trace spans: " + operationNames());
        Assert.assertEquals(parent.optString("operationName"), parentName, "the parent of span '" + childName
                + "' (id " + parentSpanId + ") is '" + parent.optString("operationName") + "', not '" + parentName
                + "'. Trace spans: " + operationNames());
    }

    @Then("the trace should have the process tag {string} with value {int}")
    public void assertProcessTag(String key, int expected) {
        Object actual = processTagValue(key);
        Assert.assertTrue(actual instanceof Number, "the process tag '" + key + "' is not numeric: got " + actual
                + " (" + (actual == null ? "absent" : actual.getClass().getSimpleName()) + "). Process tags: "
                + processTagsOf(requireTrace()));
        Assert.assertEquals(((Number) actual).longValue(), (long) expected, "the process tag '" + key
                + "' expected " + expected + " but was " + actual + ". Process tags: "
                + processTagsOf(requireTrace()));
    }

    /**
     * The attribute is PRESENT and an integer; its value is deliberately not pinned, because it is a live
     * process id. The cross-check against the container's real pid is the separate step below — asserting
     * only "a number" here is what proves the resource attribute survived the export path as a typed int64
     * rather than being stringified somewhere along the way.
     */
    @Then("the trace should have a numeric process tag {string}")
    public void assertNumericProcessTag(String key) {
        Object actual = processTagValue(key);
        Assert.assertTrue(actual instanceof Number, "the process tag '" + key + "' is not numeric: got " + actual
                + (actual == null ? " (absent)" : " (" + actual.getClass().getSimpleName() + ")") + ". Process "
                + "tags: " + processTagsOf(requireTrace()));
    }

    /**
     * Asserts the {@code process.pid} resource attribute against the carbon JVM's real pid, read from inside
     * the container. The container is the independent source: comparing the tag to itself would prove nothing,
     * and comparing it to a pid this JVM guessed would prove nothing either.
     */
    @Then("the carbon server process id should match the trace's process tag {string}")
    public void assertCarbonPidMatchesProcessTag(String key) throws Exception {
        long expected = carbonServerPid();
        Object actual = processTagValue(key);
        Assert.assertTrue(actual instanceof Number, "the process tag '" + key + "' is not numeric: got " + actual
                + ". Process tags: " + processTagsOf(requireTrace()));
        long exported = ((Number) actual).longValue();
        Assert.assertEquals(exported, expected, "the trace reports " + key + "=" + exported + " but the carbon "
                + "server process inside this container is pid " + expected + ". The attribute is taken from the "
                + "exporting JVM, so the two can only disagree if the export came from somewhere else.");
    }

    // ---- live Moesif leg ---------------------------------------------------------------------------
    //
    // The exporter points at the real api.moesif.net, so the raw export is never observed locally; delivery is
    // asserted by reading the event back out of Moesif through its own Management API (MoesifManagementApiClient).
    // Moesif's search API returns modelled events, not the bytes the gateway sent, so the wire facts the hermetic
    // OTLP/HTTP leg used to assert (method, path, auth header, content type, gzip, payload contents) are no
    // longer covered — that stand-in leg was retired along with this suite's move to live-only coverage.

    /**
     * Skips the scenario when the Moesif credentials are absent, so a CI job without the secrets reports
     * skipped rather than failed for a missing credential.
     *
     * <p>This module had no conditional-skip pattern before this, so it introduces the first one: TestNG's
     * {@link SkipException} is what marks a Cucumber scenario skipped rather than failed.
     */
    @Given("the Moesif live tracing credentials are available")
    public void requireMoesifCredentials() {
        if (!MoesifManagementApiClient.credentialsAvailable()) {
            throw new SkipException("Skipping the live Moesif leg: environment variables "
                    + MoesifManagementApiClient.COLLECTOR_APP_ID_ENV + " and "
                    + MoesifManagementApiClient.MANAGEMENT_API_KEY_ENV + " are not both set. This leg exports to "
                    + "the real api.moesif.net collector and confirms delivery through Moesif's Management API, "
                    + "which needs credentials this job does not have.");
        }
    }

    @Then("Moesif should have received the exported trace for the API context {string}")
    public void assertMoesifReceivedTrace(String context) throws InterruptedException {
        // Placeholder-resolved for the same reason as the recorder step: the feature passes "{{contextKey}}".
        String apiContext = Utils.resolveContextPlaceholders(context);
        // Polls until the whole chain is present, not merely until one span arrives — see
        // MoesifTrace#hasCompleteGatewayChain for why reading a partly exported trace is a flaky assertion.
        MoesifManagementApiClient.MoesifTrace trace = MoesifManagementApiClient.awaitTraceFor(apiContext);
        TestContext.set(MOESIF_TRACE_KEY, trace);
    }

    /**
     * Asserts the gateway's span chain in what Moesif actually returned. It is the same chain, in the same order
     * and with the same parent/child links, that {@link #assertGatewaySpanChain} asserts against the hermetic
     * collector — Moesif simply renames OTLP's fields ({@code operationName} becomes {@code action_name},
     * {@code spanID}/{@code parentSpanID} become {@code span.id}/{@code span.parent_id}) rather than hiding any
     * span. So the request span is still named {@code <apiName>--<version>--<tenantDomain>} (APIM renames
     * {@code API:Response_Latency} when it finishes the request), the API resource span is still its only child,
     * and {@code API:Backend_Latency} is still a child of the resource span — i.e. a grandchild of the request
     * span.
     */
    @Then("that Moesif trace should carry the gateway's request span chain for the API created with payload {string}")
    public void assertMoesifTraceCarriesRequestSpanChain(String payloadKey) {

        Object stored = TestContext.get(MOESIF_TRACE_KEY);
        Assert.assertTrue(stored instanceof MoesifManagementApiClient.MoesifTrace, "no Moesif trace was read back — "
                + "assertMoesifReceivedTrace must run first");

        JSONObject payload = new JSONObject(String.valueOf(TestContext.resolve(payloadKey)));
        String apiName = payload.optString("name");
        String version = payload.optString("version");
        Assert.assertFalse(apiName.isEmpty() || version.isEmpty(), "the creation payload stored as '" + payloadKey
                + "' carries no name/version, so the gateway's request span name cannot be derived. Its keys: "
                + payload.keySet());
        String expectedRequestSpan = apiName + "--" + version + "--" + Identity.actingTenantDomain();

        MoesifManagementApiClient.MoesifTrace trace = (MoesifManagementApiClient.MoesifTrace) stored;
        log.info("Moesif returned trace " + trace.traceId() + " with " + trace.spans().size() + " span(s):");
        for (MoesifManagementApiClient.MoesifSpan span : trace.spans()) {
            log.info("    " + span);
        }

        // A 32-hex identifier is the OTLP trace id.
        Assert.assertTrue(trace.traceId() != null && trace.traceId().matches("[0-9a-f]{32}"),
                "Moesif returned trace id '" + trace.traceId() + "', which is not a 32-hex OTLP trace id, so these "
                + "events do not look like ingested spans.");

        List<MoesifManagementApiClient.MoesifSpan> roots = trace.roots();
        Assert.assertEquals(roots.size(), 1, "expected exactly one root span in trace " + trace.traceId() + " — the "
                + "gateway's request span — but found " + roots.size() + ". Spans: " + trace.spans());

        MoesifManagementApiClient.MoesifSpan requestSpan = roots.get(0);
        Assert.assertEquals(requestSpan.actionName(), expectedRequestSpan, "the root span of the trace Moesif stored "
                + "is not the gateway's request span. APIM renames API:Response_Latency to "
                + "<apiName>--<version>--<tenantDomain> when it finishes the request "
                + "(APIMgtLatencySynapseHandler.handleResponseOutFlow), so that is the only name it can carry. "
                + "Spans: " + trace.spans());
        Assert.assertEquals(requestSpan.direction(), "Incoming", "the request span should be Moesif's 'Incoming' "
                + "direction but was '" + requestSpan.direction() + "'. Spans: " + trace.spans());
        Assert.assertEquals(requestSpan.status(), Integer.valueOf(200), "the request span should carry the "
                + "successful gateway response status but carried " + requestSpan.status() + ". Spans: "
                + trace.spans());

        // Every other gateway span hangs off the API resource span, so a child count other than 1 means the
        // handler chain changed shape.
        List<MoesifManagementApiClient.MoesifSpan> resourceSpans = trace.childrenOf(requestSpan);
        Assert.assertEquals(resourceSpans.size(), 1, "expected exactly one child of the request span '"
                + expectedRequestSpan + "' — the API resource span — but found " + resourceSpans.size() + ": "
                + resourceSpans + ". Spans: " + trace.spans());

        MoesifManagementApiClient.MoesifSpan resourceSpan = resourceSpans.get(0);
        // The resource span is the PARENT of every mediation span, not just the backend call, so the backend span
        // is selected BY NAME from among its children rather than by being the only child. What this pins is the
        // linkage: the backend call is recorded under the API resource span, and therefore under the request span.
        MoesifManagementApiClient.MoesifSpan backendSpan =
                trace.byActionName(MoesifManagementApiClient.BACKEND_SPAN_NAME);
        Assert.assertNotNull(backendSpan, "no span named '" + MoesifManagementApiClient.BACKEND_SPAN_NAME
                + "' is in the trace, so the backend call was never recorded under the traced request. Spans: "
                + trace.spans());
        Assert.assertTrue(trace.childrenOf(resourceSpan).contains(backendSpan), "the backend span '"
                + MoesifManagementApiClient.BACKEND_SPAN_NAME + "' is not a child of the API resource span '"
                + resourceSpan.actionName() + "' (its parent is '" + backendSpan.parentId() + "'), so the backend "
                + "call is not recorded under the traced request. Its siblings: "
                + trace.childrenOf(resourceSpan) + ". Spans: " + trace.spans());
    }

    // ---- Zipkin (v2 JSON) leg: spans read from a real Zipkin collector --------------------------------
    //
    // The block that runs these steps boots DynamicZipkin (initZipkinCollector) and selects the "zipkin"
    // tracer via its overlay. ZipkinTelemetry composes the exporter endpoint from hostname + port
    // (http://zipkin:9411/api/v2/spans), and the steps below read the spans back from Zipkin's OWN query API
    // (GET /api/v2/traces) — the same "real collector contradicts us" boundary as the Jaeger leg.
    //
    // Zipkin's v2 model differs from Jaeger v1: a trace is a plain JSON ARRAY of spans (not {traceID, spans}),
    // parent/child linkage is a parentId FIELD (not a references[] array), span kind is a top-level "kind"
    // field (not a tag), and attributes are a flat string->string "tags" OBJECT (not an array of typed
    // {key,value,type}). So the assertions here use Zipkin-specific helpers instead of the Jaeger-v1 ones
    // further down — sharing the tag math would risk asserting the wrong model on both.

    /** Block infra published by {@code BlockLifecycleListener}; absent means the block did not opt in. */
    private static final String ZIPKIN_COLLECTOR_KEY = "blockZipkinCollector";
    /** Scenario state, set by the first Zipkin step and read by the rest (runner-local like TRACE_KEY). */
    private static final String ZIPKIN_TRACE_KEY = "zipkinCollectorTrace";
    private static final String ZIPKIN_SPANS_KEY = "zipkinCollectorSpans";
    private static final String ZIPKIN_CONTEXT_KEY = "zipkinCollectorTraceContext";

    @Then("the Zipkin collector should have received a trace for the API context {string}")
    public void assertZipkinCollectorReceivedTrace(String context) throws InterruptedException {
        String apiContext = Utils.resolveContextPlaceholders(context);
        String queryBaseUrl = zipkinCollector().getQueryBaseUrl();
        JSONArray trace = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                () -> findZipkinTraceForContext(queryBaseUrl, apiContext, null),
                found -> found != null);
        Assert.assertNotNull(trace, "the Zipkin collector never received a trace carrying the API context '"
                + apiContext + "' within the propagation window. Either no span was exported for this invocation "
                + "(check the block overlay enabled the zipkin tracer and that the API was actually invoked), or "
                + "the exporter's endpoint did not resolve — an endpoint naming an absent host drops spans with "
                + "no visible error anywhere in APIM.");
        zipkinLogTrace(trace);
        TestContext.set(ZIPKIN_TRACE_KEY, trace);
        TestContext.set(ZIPKIN_CONTEXT_KEY, apiContext);
    }

    /**
     * Names are stored by Zipkin LOWERCASED — measured on the probe run: the exporter sent
     * {@code API:Response_Latency / API:Backend_Latency / GET--/customers/{id}} and the collector reported
     * {@code api:response_latency / api:backend_latency / get--/customers/{id}}. So every comparison the Zipkin
     * steps make against a span name goes through this. (The LOG leg is NOT lowercased: its Operation keys kept
     * their case.)
     */
    private static String zipkinName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    /**
     * The Zipkin analogue of {@link #assertGatewaySpanChain}: resolves the chain by ROLE and asserts its shape.
     * The spans the gateway renames are renamed for every exporter alike (the rename happens in the handler
     * before export), so the request span is still {@code <apiName>--<version>--<tenantDomain>}, the API
     * resource span is still its only child, and the backend call ({@code API:Backend_Latency}) is still a
     * child of the resource span — a grandchild of the request span. Zipkin stores every name LOWERCASED
     * (see {@link #zipkinName}), so both the expected request span and the backend literal are case-folded
     * before matching. Zipkin's v2 model expresses the linkage as {@code parentId} rather than Jaeger's
     * references array.
     */
    @Then("the Zipkin trace should have the gateway's request span chain for the API created with payload {string}")
    public void assertZipkinGatewaySpanChain(String payloadKey) throws InterruptedException {
        JSONObject payload = new JSONObject(String.valueOf(TestContext.resolve(payloadKey)));
        String apiName = payload.optString("name");
        String version = payload.optString("version");
        Assert.assertFalse(apiName.isEmpty() || version.isEmpty(), "the creation payload stored as '" + payloadKey
                + "' carries no name/version, so the gateway's request span name cannot be derived. Its keys: "
                + payload.keySet());
        String expectedRequestSpan = zipkinName(apiName + "--" + version + "--" + Identity.actingTenantDomain());

        // Re-poll for a trace that carries our context AND the request span, exactly like the Jaeger leg: the
        // collector exposes a trace as soon as its first span lands, so a trace can still be mid-export when
        // first matched, and reading spans from a partial trace turns an export-timing detail into a
        // product-looking failure.
        String apiContext = String.valueOf(TestContext.resolve(ZIPKIN_CONTEXT_KEY));
        JSONArray complete = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                () -> findZipkinTraceForContext(zipkinCollector().getQueryBaseUrl(), apiContext, expectedRequestSpan),
                found -> found != null);
        Assert.assertNotNull(complete, "the Zipkin collector received a trace carrying the API context '"
                + apiContext + "' but never one containing the gateway's request span '" + expectedRequestSpan
                + "'. Either the request span is not exported at all, or the trace stayed incomplete. Spans seen "
                + "in the best attempt: " + zipkinSpanNames(zipkinTraceFromContext(apiContext)));
        TestContext.set(ZIPKIN_TRACE_KEY, complete);
        zipkinLogTrace(complete);

        JSONObject requestSpan = zipkinSpanByName(expectedRequestSpan);
        Assert.assertNotNull(requestSpan, "the gateway exported no request span named '" + expectedRequestSpan
                + "'. APIM renames API:Response_Latency to <apiName>--<version>--<tenantDomain> when it finishes "
                + "it (APIMgtLatencySynapseHandler.handleResponseOutFlow), so that is the only name it can carry. "
                + "Spans present: " + zipkinSpanNames(complete));

        List<JSONObject> resourceSpans = zipkinChildrenOf(requestSpan);
        Assert.assertEquals(resourceSpans.size(), 1, "expected exactly one child of the request span '"
                + expectedRequestSpan + "' — the API resource span — but found " + resourceSpans.size() + ": "
                + zipkinSpanNames(resourceSpans) + ". Every other gateway span hangs off the resource span, so a "
                + "count other than 1 means the handler chain changed shape.");

        JSONObject resourceSpan = resourceSpans.get(0);
        JSONObject backendSpan = null;
        for (JSONObject child : zipkinChildrenOf(resourceSpan)) {
            if (zipkinName("API:Backend_Latency").equals(child.optString("name"))) {
                backendSpan = child;
                break;
            }
        }
        Assert.assertNotNull(backendSpan, "no span named 'API:Backend_Latency' is a child of the API resource span '"
                + resourceSpan.optString("name") + "', so the backend call is not recorded under the traced "
                + "request. Its children: " + zipkinSpanNames(zipkinChildrenOf(resourceSpan)) + ".");

        JSONObject spans = new JSONObject();
        spans.put(ALIAS_REQUEST, requestSpan);
        spans.put(ALIAS_RESOURCE, resourceSpan);
        spans.put(ALIAS_BACKEND, backendSpan);
        TestContext.set(ZIPKIN_SPANS_KEY, spans);
    }

    @Then("the span {string} in the Zipkin trace should have kind {string}")
    public void assertZipkinSpanKind(String spanName, String expectedKind) {
        JSONObject span = zipkinRequireSpan(spanName);
        String kind = span.optString("kind");
        Assert.assertEquals(kind, expectedKind, "the span '" + spanName + "' has kind '" + kind + "' but expected '"
                + expectedKind + "'. The gateway opens the request span as SERVER and the backend call as CLIENT; "
                + "Zipkin's v2 exporter writes the kind as a top-level field (absent means INTERNAL). The span: "
                + zipkinRender(span));
    }

    @Then("the span {string} in the Zipkin trace should have the tag {string} with value {string}")
    public void assertZipkinSpanTag(String spanName, String key, String expected) {
        JSONObject span = zipkinRequireSpan(spanName);
        String actual = zipkinTagValue(span, key);
        Assert.assertNotNull(actual, "the span '" + spanName + "' carries no '" + key + "' tag. Its tags: "
                + zipkinTagsOf(span));
        Assert.assertEquals(actual, expected, "the span '" + spanName + "' tag '" + key + "' expected '" + expected
                + "' but was '" + actual + "'. Its tags: " + zipkinTagsOf(span));
    }

    @Then("the span {string} in the Zipkin trace should have the tag {string} whose value contains the API context stored as {string}")
    public void assertZipkinSpanTagContainsContext(String spanName, String key, String contextKey) {
        String context = String.valueOf(TestContext.resolve(contextKey));
        JSONObject span = zipkinRequireSpan(spanName);
        String actual = zipkinTagValue(span, key);
        Assert.assertNotNull(actual, "the span '" + spanName + "' carries no '" + key + "' tag at all. Its tags: "
                + zipkinTagsOf(span));
        Assert.assertTrue(actual.contains(context), "the span '" + spanName + "' tag '" + key
                + "' does not carry this scenario's API context '" + context + "'; got '" + actual + "'. Either "
                + "the attribute is not the request path, or the traced invocation is a different one. Its tags: "
                + zipkinTagsOf(span));
    }

    @Then("the span {string} in the Zipkin trace should not have the tag {string}")
    public void assertZipkinSpanLacksTag(String spanName, String key) {
        JSONObject span = zipkinRequireSpan(spanName);
        String actual = zipkinTagValue(span, key);
        Assert.assertNull(actual, "the span '" + spanName + "' still carries the pre-patch attribute '" + key
                + "' (value " + actual + "); the attribute was renamed to the semconv form and the old key must "
                + "be gone. All tags: " + zipkinTagsOf(span));
    }

    // ---- LOG tracing leg: spans read back from the open tracing log -----------------------------------
    //
    // The block that runs these steps turns the remote tracer OFF and the log tracer ON (overlay), so nothing
    // is exported anywhere; LogTelemetry writes one TRACE line per finished span to
    // wso2-apimgt-open-tracing.log (the distribution's log4j2.properties wires the "tracer" logger at TRACE to
    // the OPEN_TRACING appender). The read-back therefore IS the assertion target, read from inside the
    // container via ApimRuntime.
    //
    // Each line is a single-line JSON object with the keys TelemetryConstants declares: "Span Id", "Tracer Id",
    // "Operation", "Latency", "Tags". "Tags" is the span's AttributesMap toString() — e.g.
    // AttributesMap{data={url.path=/t/<tenant>/<context>/1.0.0/customers/123, ...}, capacity=128,
    // totalAddedValues=N}. The keys and layout are the product's (verified in LogExporter: the string constants
    // are exactly Span Id/Tracer Id/Operation/Latency/Tags), so parsing them is asserting on the product's own
    // contract, not on an outdated doc sample.
    //
    // Scoping a scenario's spans: the LOG FILE is shared by every row of the block, so the scenario is scoped
    // by its UNIQUE API context (which the request span's url.path tag carries). The request span is then
    // identified by its renamed Operation name, and the chain is every other line carrying the SAME "Tracer Id"
    // — the log cannot express parent/child linkage (no parent id field), so "the chain" is asserted by
    // presence of the mediation/backend operations rather than by topology.

    private static final String LOG_FILE_NAME = "wso2-apimgt-open-tracing.log";
    /** Scenario state: the coherent snapshot of THIS request's spans the request-span step captured. */
    private static final String LOG_SPANS_KEY = "tracedLogSpans";
    private static final String LOG_TRACE_ID_KEY = "tracedLogTraceId";

    @Then("the API Manager open tracing log should record a span for the API context {string}")
    public void assertOpenTracingLogRecordedSpan(String context) throws InterruptedException {
        String apiContext = Utils.resolveContextPlaceholders(context);
        Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                () -> logSpansForContext(apiContext),
                spans -> !spans.isEmpty());
        List<JSONObject> spans = logSpansForContext(apiContext);
        Assert.assertFalse(spans.isEmpty(), "the open tracing log never recorded a span carrying the API context '"
                + apiContext + "' within the propagation window. Either no span was exported for this invocation "
                + "(check the block overlay enables the log tracer and that the API was actually invoked), or the "
                + "log tracer is silent. The file '" + LOG_FILE_NAME + "' currently holds " + logSpans() + " span "
                + "line(s).");
        // The first span that carries the context is this request; its Tracer Id scopes the whole request.
        String traceId = spans.get(0).optString(TRACER_ID_KEY);
        Assert.assertTrue(traceId.matches("[0-9a-f]{32}"), "the open tracing log recorded the API context '"
                + apiContext + "' but the span's " + TRACER_ID_KEY + " is '" + traceId
                + "', which is not a 32-hex OTLP trace id. Full line: " + spans.get(0));
        TestContext.set(LOG_TRACE_ID_KEY, traceId);
    }

    /**
     * The request span's Operation: the gateway renames API:Response_Latency to
     * {@code <apiName>--<version>--<tenantDomain>} when it finishes the request, and the log exporter writes the
     * Operation at export time — after the rename — so the expectation is derived from the scenario's own
     * creation payload plus the acting actor's tenant, the same non-circular statement as the collector legs.
     */
@Then("the open tracing log should carry the gateway's request span for the API created with payload {string}")
    public void assertOpenTracingLogRequestSpan(String payloadKey) throws InterruptedException {
        JSONObject payload = new JSONObject(String.valueOf(TestContext.resolve(payloadKey)));
        String expectedOperation = payload.optString("name") + "--" + payload.optString("version")
                + "--" + Identity.actingTenantDomain();
        // The exporter writes asynchronously (BatchSpanProcessor worker threads), so a single read can catch the
        // file between the CORS span and the request span and look like a missing span (observed on the probe run:
        // the first read held only the CORS line). Poll until the request span's line AND at least 3 spans for
        // this request are present, then SNAPSHOT that read so the follow-on assertions are deterministic — the
        // file keeps growing, but they must all assert on one coherent capture, not on a moving target.
        List<JSONObject> snapshot = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                () -> completeRequestSnapshot(expectedOperation),
                spans -> spans != null);
        Assert.assertNotNull(snapshot, "the open tracing log recorded this request's early spans but never the "
                + "gateway's request-span Operation '" + expectedOperation + "'. APIM renames "
                + "API:Response_Latency to <apiName>--<version>--<tenantDomain> when it finishes it, so that is "
                + "the only name it can carry. Operations logged for this request: " + logSpanOperations()
                + ". Full file: " + readOpenTracingLog());
        TestContext.set(LOG_SPANS_KEY, snapshot);
    }

    /** The snapshot the request-span step captured: at least 3 spans sharing this request's Tracer Id. */
    private List<JSONObject> completeRequestSnapshot(String expectedOperation) {
        List<JSONObject> spans = spansOfRequest();
        for (JSONObject span : spans) {
            if (expectedOperation.equals(span.optString(OPERATION_KEY)) && spans.size() >= 3) {
                return spans;
            }
        }
        return null;
    }

    @Then("the open tracing log's spans for this request should share one trace id")
    public void assertOpenTracingLogSpansShareTraceId() {
        List<JSONObject> spans = snapshotSpans();
        Assert.assertTrue(spans.size() >= 3, "the open tracing log recorded only " + spans.size()
                + " span(s) for this request; the gateway's chain — request, API resource, API:Backend_Latency "
                + "plus the mediation spans — is at least 3. Operations: " + logSpanOperations());
        String traceId = spans.get(0).optString(TRACER_ID_KEY);
        for (JSONObject span : spans) {
            Assert.assertEquals(span.optString(TRACER_ID_KEY), traceId, "the open tracing log's spans for this "
                    + "request do not share one " + TRACER_ID_KEY + " — every span of a request carries the SAME "
                    + "trace id. Found " + traceId + " and " + span.optString(TRACER_ID_KEY) + ". Operations: "
                    + logSpanOperations());
        }
    }

    @Then("the open tracing log should record the {string} span for the payload {string}")
    public void assertOpenTracingLogRecordsSpan(String operationName, String payloadKey) {
        JSONObject payload = new JSONObject(String.valueOf(TestContext.resolve(payloadKey)));
        Assert.assertFalse(payload.optString("name").isEmpty(), "the creation payload stored as '" + payloadKey
                + "' carries no name, so the request cannot be identified. Its keys: " + payload.keySet());
        for (JSONObject span : snapshotSpans()) {
            if (operationName.equals(span.optString(OPERATION_KEY))) {
                return;
            }
        }
        Assert.fail("the open tracing log recorded no '" + operationName + "' span for this request. Operations "
                + "logged for this request: " + logSpanOperations() + ". Full file: " + readOpenTracingLog());
    }

    // ---- Zipkin plumbing ------------------------------------------------------------------------------

    private DynamicZipkin zipkinCollector() {
        Object collector = TestContext.get(ZIPKIN_COLLECTOR_KEY);
        Assert.assertTrue(collector instanceof DynamicZipkin, "no Zipkin collector in scope — this scenario's "
                + "block must declare <parameter name=\"initZipkinCollector\" value=\"true\"/>");
        return (DynamicZipkin) collector;
    }

    /**
     * Mirrors {@link #findTraceForContext} for Zipkin's v2 query API. The response is a JSON ARRAY of traces,
     * where each trace is itself a JSON ARRAY of spans (Zipkin has no per-trace envelope). The service filter
     * uses the same {@link #SERVICE_NAME} resource attribute the Jaeger leg deploys on its query — the two
     * exporters share the same OpenTelemetry Resource, so Zipkin's localEndpoint.serviceName is that same
     * value; if the probe shows otherwise, the filter is the one place to correct.
     */
    private JSONArray findZipkinTraceForContext(String queryBaseUrl, String context, String requiredName)
            throws IOException {
        String url = queryBaseUrl + "/api/v2/traces?serviceName=" + Utils.urlEncode(SERVICE_NAME)
                + "&limit=" + QUERY_LIMIT;
        HttpResponse response = SimpleHTTPClient.getInstance().doGet(url, Collections.emptyMap());
        Assert.assertTrue(response.getResponseCode() == 200 && response.getData() != null,
                "the Zipkin collector query API answered " + response.getResponseCode() + ": " + response.getData());
        JSONArray traces = new JSONArray(response.getData());
        for (int i = 0; i < traces.length(); i++) {
            JSONArray candidate = traces.getJSONArray(i);
            if (zipkinTraceMentionsContext(candidate, context) && (requiredName == null
                    || zipkinSpanByName(candidate, requiredName) != null)) {
                return candidate;
            }
        }
        return null;
    }

    /** Any tag value anywhere in the (v2) trace containing the context — the scenario-scoping predicate. */
    private boolean zipkinTraceMentionsContext(JSONArray trace, String context) {
        for (int i = 0; i < trace.length(); i++) {
            JSONObject tags = trace.getJSONObject(i).optJSONObject("tags");
            if (tags == null) {
                continue;
            }
            for (String key : tags.keySet()) {
                if (String.valueOf(tags.opt(key)).contains(context)) {
                    return true;
                }
            }
        }
        return false;
    }

    private JSONArray zipkinTraceFromContext(String context) {
        try {
            JSONArray trace = findZipkinTraceForContext(zipkinCollector().getQueryBaseUrl(), context, null);
            return trace != null ? trace : new JSONArray();
        } catch (IOException e) {
            return new JSONArray();
        }
    }

    private JSONArray zipkinRequireTrace() {
        Object trace = TestContext.get(ZIPKIN_TRACE_KEY);
        Assert.assertTrue(trace instanceof JSONArray, "no Zipkin trace in scope — the scenario must first assert "
                + "that the Zipkin collector received a trace for its API context");
        return (JSONArray) trace;
    }

    private JSONObject zipkinRequireSpan(String spanRef) {
        JSONObject resolved = TestContext.contains(ZIPKIN_SPANS_KEY)
                ? ((JSONObject) TestContext.get(ZIPKIN_SPANS_KEY)).optJSONObject(spanRef) : null;
        if (resolved != null) {
            return resolved;
        }
        JSONObject span = zipkinSpanByName(zipkinRequireTrace(), spanRef);
        if (span != null) {
            return span;
        }
        Assert.fail("the collected Zipkin trace has no span named '" + spanRef + "', and no span was resolved "
                + "under that role alias — the gateway's request span chain step must run first. Spans present: "
                + zipkinSpanNames(zipkinRequireTrace()));
        throw new IllegalStateException("unreachable");
    }

    private JSONObject zipkinSpanByName(String name) {
        return zipkinSpanByName(zipkinRequireTrace(), name);
    }

    private JSONObject zipkinSpanByName(JSONArray trace, String name) {
        for (int i = 0; i < trace.length(); i++) {
            JSONObject span = trace.getJSONObject(i);
            if (name.equals(span.optString("name"))) {
                return span;
            }
        }
        return null;
    }

    /** Direct children of a v2 span — the spans whose parentId field points at its id. */
    private List<JSONObject> zipkinChildrenOf(JSONObject parent) {
        List<JSONObject> children = new ArrayList<>();
        String parentId = parent.optString("id");
        JSONArray trace = zipkinRequireTrace();
        for (int i = 0; i < trace.length(); i++) {
            JSONObject span = trace.getJSONObject(i);
            if (parentId.equals(span.optString("parentId"))) {
                children.add(span);
            }
        }
        return children;
    }

    private List<String> zipkinSpanNames(List<JSONObject> spans) {
        List<String> names = new ArrayList<>();
        for (JSONObject span : spans) {
            names.add(span.optString("name"));
        }
        return names;
    }

    private List<String> zipkinSpanNames(JSONArray trace) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < trace.length(); i++) {
            names.add(trace.getJSONObject(i).optString("name"));
        }
        return names;
    }

    /** v2 tags are a string->string OBJECT, so the value comes back as the string it was exported as. */
    private String zipkinTagValue(JSONObject span, String key) {
        JSONObject tags = span.optJSONObject("tags");
        if (tags == null) {
            return null;
        }
        Object value = tags.opt(key);
        return value == null ? null : String.valueOf(value);
    }

    private String zipkinTagsOf(JSONObject span) {
        JSONObject tags = span.optJSONObject("tags");
        return tags == null ? "{}" : tags.toString();
    }

    private String zipkinRender(JSONObject span) {
        StringBuilder sb = new StringBuilder("v2 span id=").append(span.optString("id"));
        if (span.has("parentId")) {
            sb.append(" parent=").append(span.optString("parentId"));
        }
        return sb.append(" name=").append(span.optString("name")).append(" kind=").append(span.optString("kind"))
                .append(" trace=").append(span.optString("traceId")).append(" tags=").append(zipkinTagsOf(span))
                .append(" localEndpoint=").append(
                        span.optJSONObject("localEndpoint") == null ? "{}"
                                : span.optJSONObject("localEndpoint").toString())
                .toString();
    }

    private void zipkinLogTrace(JSONArray trace) {
        for (int i = 0; i < trace.length(); i++) {
            log.info("zipkin-span " + zipkinRender(trace.getJSONObject(i)));
        }
    }

    // ---- LOG tracing plumbing -------------------------------------------------------------------------

    private static final String SPAN_ID_KEY = "Span Id";
    private static final String TRACER_ID_KEY = "Tracer Id";
    private static final String OPERATION_KEY = "Operation";
    private static final String TAGS_KEY = "Tags";

    private ApimRuntime apimRuntime() {
        Object container = TestContext.get(CONTAINER_KEY);
        Assert.assertTrue(container instanceof ApimRuntime, "no block APIM container in scope to read the open "
                + "tracing log from");
        return (ApimRuntime) container;
    }

    private String readOpenTracingLog() {
        ApimRuntime container = apimRuntime();
        return container.readContainerFile(container.getContainerLogFilePath(LOG_FILE_NAME));
    }

    /**
     * Every span line currently in the log file, JSON-parsed. The log4j2 layout is {@code %5p %m%n}, so each
     * line carries " TRACE " before the JSON message; parsing takes the first '{' to the end of the line and
     * skips lines that do not parse (e.g. a partial write mid-rollover).
     */
    private List<JSONObject> logSpans() {
        List<JSONObject> spans = new ArrayList<>();
        String content = readOpenTracingLog();
        if (content == null || content.trim().isEmpty()) {
            return spans;
        }
        for (String line : content.split("\\R")) {
            int brace = line.indexOf('{');
            if (brace < 0) {
                continue;
            }
            try {
                spans.add(new JSONObject(line.substring(brace).trim()));
            } catch (org.json.JSONException ignored) {
                // A line that is not span JSON (or a torn partial write) is not one of ours.
            }
        }
        return spans;
    }

    /** Spans whose Tags string mentions the scenario's UNIQUE API context. */
    private List<JSONObject> logSpansForContext(String apiContext) {
        List<JSONObject> matched = new ArrayList<>();
        for (JSONObject span : logSpans()) {
            if (String.valueOf(span.opt(TAGS_KEY)).contains(apiContext)) {
                matched.add(span);
            }
        }
        return matched;
    }

    /** Every span line belonging to THIS scenario's request, i.e. carrying its Tracer Id. */
    private List<JSONObject> spansOfRequest() {
        Object traceIdObject = TestContext.get(LOG_TRACE_ID_KEY);
        Assert.assertTrue(traceIdObject instanceof String, "no open-tracing request in scope — the scenario must "
                + "first assert that the open tracing log recorded a span for its API context");
        String traceId = (String) traceIdObject;
        List<JSONObject> spans = new ArrayList<>();
        for (JSONObject span : logSpans()) {
            if (traceId.equals(span.optString(TRACER_ID_KEY))) {
                spans.add(span);
            }
        }
        Assert.assertFalse(spans.isEmpty(), "no open tracing log line carries Tracer Id '" + traceId
                + "' (this scenario's request). The log file currently holds " + logSpans().size() + " span "
                + "line(s). If the file grew across a rollover this can happen mid-request.");
        return spans;
    }

    /**
     * The coherent snapshot {@link #assertOpenTracingLogRequestSpan} captured (all lines of this request present
     * in ONE read). The follow-on assertions read this, never a fresh file read, so a file still growing mid-run
     * cannot change what they see between steps.
     */
    @SuppressWarnings("unchecked")
    private List<JSONObject> snapshotSpans() {
        Object spans = TestContext.get(LOG_SPANS_KEY);
        Assert.assertTrue(spans instanceof List, "no open-tracing request snapshot in scope — the scenario must "
                + "first assert that the open tracing log carries the gateway's request span");
        return (List<JSONObject>) spans;
    }

    private List<String> logSpanOperations() {
        List<String> operations = new ArrayList<>();
        for (JSONObject span : spansOfRequest()) {
            operations.add(span.optString(OPERATION_KEY));
        }
        return operations;
    }

    // ---- collector plumbing ---------------------------------------------------------------------------

    private DynamicOtlpCollector collector() {
        Object collector = TestContext.get(COLLECTOR_KEY);
        Assert.assertTrue(collector instanceof DynamicOtlpCollector, "no OTLP collector in scope — this scenario's "
                + "block must declare <parameter name=\"initOtlpCollector\" value=\"true\"/>");
        return (DynamicOtlpCollector) collector;
    }

    private JSONObject findTraceForContext(String queryBaseUrl, String context, String requiredOperationName)
            throws IOException {
        String url = queryBaseUrl + "/api/traces?service=" + Utils.urlEncode(SERVICE_NAME)
                + "&lookback=1h&limit=" + QUERY_LIMIT;
        HttpResponse response = SimpleHTTPClient.getInstance().doGet(url, Collections.emptyMap());
        // A non-200 is a MALFORMED query, never "no traces yet": Jaeger answers data=null only alongside an
        // errors[] entry, so failing here with the message beats letting a null slip into the poll loop. The
        // assertion throws (not IOException), so retryUntil propagates it instead of retrying.
        Assert.assertTrue(response.getResponseCode() == 200 && response.getData() != null,
                "the OTLP collector query API answered " + response.getResponseCode() + ": " + response.getData());
        JSONArray traces = new JSONObject(response.getData()).optJSONArray("data");
        if (traces == null) {
            return null;
        }
        for (int i = 0; i < traces.length(); i++) {
            JSONObject candidate = traces.getJSONObject(i);
            if (mentionsContext(candidate, context) && (requiredOperationName == null
                    || spanByOperationName(candidate, requiredOperationName) != null)) {
                return candidate;
            }
        }
        return null;
    }

    /** Any tag value anywhere in the trace containing the context — the scenario-scoping predicate. */
    private boolean mentionsContext(JSONObject trace, String context) {
        JSONArray spans = trace.optJSONArray("spans");
        if (spans == null) {
            return false;
        }
        for (int i = 0; i < spans.length(); i++) {
            JSONArray tags = spans.getJSONObject(i).optJSONArray("tags");
            if (tags == null) {
                continue;
            }
            for (int t = 0; t < tags.length(); t++) {
                Object value = tags.getJSONObject(t).opt("value");
                if (value != null && String.valueOf(value).contains(context)) {
                    return true;
                }
            }
        }
        return false;
    }

    private JSONObject requireTrace() {
        Object trace = TestContext.get(TRACE_KEY);
        Assert.assertTrue(trace instanceof JSONObject, "no collected trace in scope — the scenario must first assert "
                + "that the collector received a trace for its API context");
        return (JSONObject) trace;
    }

    private JSONObject requireSpan(String spanRef) {
        // A role alias resolved by the span-chain step wins; anything else is a literal operation name, which is
        // how the spans the gateway does NOT rename (API:Backend_Latency, API:Throttle_Latency, ...) are still
        // addressed.
        JSONObject resolved = TestContext.contains(SPANS_KEY)
                ? ((JSONObject) TestContext.get(SPANS_KEY)).optJSONObject(spanRef) : null;
        if (resolved != null) {
            return resolved;
        }
        JSONObject trace = requireTrace();
        JSONArray spans = trace.optJSONArray("spans");
        if (spans != null) {
            for (int i = 0; i < spans.length(); i++) {
                JSONObject span = spans.getJSONObject(i);
                if (spanRef.equals(span.optString("operationName"))) {
                    return span;
                }
            }
        }
        Assert.fail("the collected trace has no span named '" + spanRef + "', and no span was resolved under that "
                + "role alias — the gateway's request span chain step must run first. Spans present: "
                + operationNames() + ". Trace id " + trace.optString("traceID"));
        throw new IllegalStateException("unreachable");
    }

    private JSONObject spanByOperationName(String operationName) {
        return spanByOperationName(requireTrace(), operationName);
    }

    /** Looks in an EXPLICIT trace, so a poll can test a candidate before it is adopted as the scenario's trace. */
    private JSONObject spanByOperationName(JSONObject trace, String operationName) {
        JSONArray spans = trace == null ? null : trace.optJSONArray("spans");
        if (spans == null) {
            return null;
        }
        for (int i = 0; i < spans.length(); i++) {
            JSONObject span = spans.getJSONObject(i);
            if (operationName.equals(span.optString("operationName"))) {
                return span;
            }
        }
        return null;
    }

    /** Direct children of a span — the spans whose CHILD_OF reference points at its span id. */
    private List<JSONObject> childrenOf(JSONObject parent) {
        List<JSONObject> children = new ArrayList<>();
        String parentSpanId = parent.optString("spanID");
        JSONArray spans = requireTrace().optJSONArray("spans");
        if (spans == null) {
            return children;
        }
        for (int i = 0; i < spans.length(); i++) {
            JSONObject span = spans.getJSONObject(i);
            if (parentSpanId.equals(parentSpanIdOf(span))) {
                children.add(span);
            }
        }
        return children;
    }

    private List<String> operationNamesOf(List<JSONObject> spans) {
        List<String> names = new ArrayList<>();
        for (JSONObject span : spans) {
            names.add(span.optString("operationName"));
        }
        return names;
    }

    private JSONObject spanById(JSONObject someSpan, String spanId) {
        JSONArray spans = requireTrace().optJSONArray("spans");
        if (spans == null) {
            return null;
        }
        for (int i = 0; i < spans.length(); i++) {
            JSONObject span = spans.getJSONObject(i);
            if (spanId.equals(span.optString("spanID"))) {
                return span;
            }
        }
        return null;
    }

    private String parentSpanIdOf(JSONObject span) {
        JSONArray references = span.optJSONArray("references");
        if (references == null) {
            return null;
        }
        for (int i = 0; i < references.length(); i++) {
            JSONObject reference = references.getJSONObject(i);
            if ("CHILD_OF".equals(reference.optString("refType"))) {
                return reference.optString("spanID");
            }
        }
        return null;
    }

    /** First tag with this key, tolerating the duplicates Jaeger reports for span.kind. */
    private Object tagValue(JSONObject span, String key) {
        JSONArray tags = span.optJSONArray("tags");
        if (tags == null) {
            return null;
        }
        for (int i = 0; i < tags.length(); i++) {
            JSONObject tag = tags.getJSONObject(i);
            if (key.equals(tag.optString("key"))) {
                return tag.opt("value");
            }
        }
        return null;
    }

    private Object processTagValue(String key) {
        JSONObject processes = requireTrace().optJSONObject("processes");
        if (processes == null) {
            return null;
        }
        for (String processId : processes.keySet()) {
            JSONArray tags = processes.getJSONObject(processId).optJSONArray("tags");
            if (tags == null) {
                continue;
            }
            for (int i = 0; i < tags.length(); i++) {
                JSONObject tag = tags.getJSONObject(i);
                if (key.equals(tag.optString("key"))) {
                    return tag.opt("value");
                }
            }
        }
        return null;
    }

    /**
     * The carbon JVM's pid, read from the container's own proc filesystem: one {@code cmdline} file per
     * numeric {@code /proc} entry, matched with {@code grep} rather than {@code ps} because procps is not
     * guaranteed in the image.
     *
     * <p>The pattern is written with escaped dots on purpose: {@code org\.wso2\.carbon\.} as a REGEX matches
     * {@code org.wso2.carbon.} but NOT this scanning shell's own cmdline, which contains the backslashes. An
     * unescaped pattern would match the scanner and report its own pid.
     */
    private long carbonServerPid() throws Exception {
        Object container = TestContext.get(CONTAINER_KEY);
        Assert.assertTrue(container instanceof ApimRuntime, "no block APIM container in scope to read the carbon "
                + "server pid from");
        String script = "for d in /proc/[0-9]*/cmdline; do "
                + "if grep -qa 'org\\.wso2\\.carbon\\.' \"$d\" 2>/dev/null; then echo \"${d#/proc/}\" | cut -d/ -f1; fi; "
                + "done";
        Container.ExecResult result = ((ApimRuntime) container).execInContainer("bash", "-c", script);
        Assert.assertEquals(result.getExitCode(), 0, "the in-container scan for the carbon pid failed: "
                + result.getStderr());
        List<Long> pids = parsePids(result.getStdout());
        Assert.assertEquals(pids.size(), 1, "expected exactly one carbon JVM pid in the container but found "
                + pids.size() + " (" + pids + "); scan output: '" + result.getStdout().trim() + "'");
        return pids.get(0);
    }

    private List<Long> parsePids(String stdout) {
        List<Long> pids = new ArrayList<>();
        for (String line : stdout.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                pids.add(Long.parseLong(trimmed));
            } catch (NumberFormatException e) {
                Assert.fail("the in-container pid scan returned a non-numeric line '" + trimmed + "' from: "
                        + stdout);
            }
        }
        return pids;
    }

    // ---- diagnostics ----------------------------------------------------------------------------------

    private String operationNames() {
        JSONArray spans = requireTrace().optJSONArray("spans");
        if (spans == null) {
            return "[]";
        }
        List<String> names = new ArrayList<>();
        for (int i = 0; i < spans.length(); i++) {
            names.add(spans.getJSONObject(i).optString("operationName"));
        }
        return names.toString();
    }

    private String tagsOf(JSONObject span) {
        JSONArray tags = span.optJSONArray("tags");
        if (tags == null) {
            return "[]";
        }
        List<String> rendered = new ArrayList<>();
        for (int i = 0; i < tags.length(); i++) {
            JSONObject tag = tags.getJSONObject(i);
            rendered.add(tag.optString("key") + "=" + tag.opt("value") + " (" + tag.optString("type") + ")");
        }
        return rendered.toString();
    }

    private String processTagsOf(JSONObject trace) {
        JSONObject processes = trace.optJSONObject("processes");
        if (processes == null) {
            return "[]";
        }
        List<String> rendered = new ArrayList<>();
        for (String processId : processes.keySet()) {
            rendered.add(processes.getJSONObject(processId).optString("serviceName") + " -> "
                    + processes.getJSONObject(processId).optJSONArray("tags"));
        }
        return rendered.toString();
    }

    /**
     * Logs every matched span's full tag set. This is the measurement the exact-value assertions are pinned
     * from: the collector's rendering of APIM's runtime-derived paths is not a test input, so the first run
     * is what establishes the expected strings rather than a guess.
     */
    private void logTagsOf(JSONObject trace) {
        JSONArray spans = trace.optJSONArray("spans");
        if (spans == null) {
            return;
        }
        for (int i = 0; i < spans.length(); i++) {
            JSONObject span = spans.getJSONObject(i);
            log.info("otlp-span trace=" + trace.optString("traceID") + " span=" + span.optString("operationName")
                    + " tags=" + tagsOf(span));
        }
        log.info("otlp-process trace=" + trace.optString("traceID") + " processes=" + processTagsOf(trace));
    }
}
