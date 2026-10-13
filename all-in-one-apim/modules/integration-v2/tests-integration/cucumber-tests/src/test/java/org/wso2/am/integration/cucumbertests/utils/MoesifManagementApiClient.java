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

package org.wso2.am.integration.cucumbertests.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-back client for the <b>Moesif Management API</b>, used by the live Moesif tracing leg to assert that
 * spans the gateway exported really were delivered to the real Moesif collector.
 *
 * <p>Moesif exposes two separate planes with two separate credentials, which is why the live leg needs both:
 * <ul>
 *   <li>the <b>Collector API</b> ({@code api.moesif.net}) is write-only and authenticates with the
 *       {@code X-Moesif-Application-Id} header. That credential is what the <i>gateway</i> is configured with
 *       (see {@code moesifOtlpHttpLive.toml}) and it grants no way to read anything back;</li>
 *   <li>the <b>Management API</b> ({@code api.moesif.com}) is query-only and authenticates with a Bearer
 *       <b>Management API key</b>. That is the credential this client uses.</li>
 * </ul>
 *
 * <p>The two are NOT interchangeable, and confusing them fails in a way that looks like a code bug. Passing
 * either the collector application id <i>or</i> a Moesif <b>portal session token</b> (the {@code JWT} that
 * {@code www.moesif.com}'s own UI sends, the token you see in the browser's network tab) is rejected with HTTP
 * 401 code 107 "Authorization token exists, but malformed". A real Management API key instead carries
 * {@code org}, {@code app} and a {@code permissions} map naming the required scope — e.g.
 * {@code {"456:823":{"scp":"read:events"}}} — and no {@code exp}. Because the {@code ~} path segment below
 * resolves the organization from the key itself, no org id has to be configured separately; the commonest
 * cause of an empty result set is a key minted against a different organization or application than the one
 * being read.
 *
 * <p><b>How a trace is found.</b> Two queries are needed, because the context-bearing field does not cover the
 * whole trace: {@code API:Backend_Latency} records the backend call under its own URI
 * ({@code http://nodebackend:3001/…}) and {@code API:Google_Analytics_Latency} under {@code /}, so neither
 * carries the API context in {@code request.uri} even though both belong to the same trace. Matching the
 * context therefore finds only part of a trace. So the context is used only to LOCATE candidate trace ids,
 * and each candidate is then expanded by {@code trace_id}, which is what makes the backend span assertable.
 *
 * <p><b>Field names.</b> Moesif indexes the exact-match form of a string under a {@code .raw} keyword
 * sub-field, and only the {@code .raw} form is usable in a {@code terms}/{@code wildcard} clause —
 * {@code request.uri} as a bare field matches nothing. The filter is a {@code post_filter} (the time range
 * comes from {@code from}/{@code to}), and the sort must carry {@code "unmapped_type":"long"} because some
 * spans have no {@code request.time} at all and would otherwise fail the sort outright.
 *
 * <p><b>Log events are not spans.</b> The collector ingests the gateway's ordinary request logs into the same
 * application, and those events also carry the API context but have no {@code trace_id} at all. Every clause
 * here pairs the context match with {@code exists: trace_id.raw} so a log event can never be mistaken for a
 * delivered span.
 *
 * <p>Queries go through Moesif's search endpoint, which accepts an Elasticsearch/OpenSearch query body. Note that
 * the {@code from}/{@code to} window parameters are <b>calendar-aligned</b>, so a narrow "since 90 seconds ago"
 * window is not expressible; the reliability of a lookup therefore comes from filtering on a value that is
 * unique to the run rather than from the time window.
 *
 * <p>This client only ever READS. It creates nothing on Moesif, so there is nothing to register with any cleanup
 * sweep (the CLAUDE.md §14 cleanup rule exists for resources a scenario creates on an external system).
 */
public final class MoesifManagementApiClient {

    /** Environment variable carrying the Moesif <b>Management API key</b> (Bearer), used to read events back. */
    public static final String MANAGEMENT_API_KEY_ENV = "MOESIF_MANAGEMENT_API_KEY";

    /** Environment variable carrying the Moesif <b>collector application id</b> the gateway exports with. */
    public static final String COLLECTOR_APP_ID_ENV = "MOESIF_COLLECTOR_APP_ID";

    /** Optional override of the Management API host, so an EU-resident account can be tested. */
    private static final String MANAGEMENT_BASE_URL_ENV = "MOESIF_MANAGEMENT_BASE_URL";

    /**
     * Management API host including the {@code /v1} version segment. Moesif's published OpenAPI spec declares
     * {@code https://api.moesif.com/v1} as the server and {@code /search/~/search/events} as the path, so the
     * full endpoint is {@code .../v1/search/~/search/events}.
     */
    private static final String DEFAULT_MANAGEMENT_BASE_URL = "https://api.moesif.com/v1";

    /**
     * Moesif's search endpoint. The {@code ~} path segment is Moesif's placeholder for "the organization of the
     * presented key", which is why no org id has to be configured.
     */
    private static final String SEARCH_EVENTS_PATH = "/search/~/search/events";

    /** Indexed exact-match form of the URI, which is the only form a wildcard clause can match on. */
    private static final String URI_RAW_FIELD = "request.uri.raw";

    /** Indexed exact-match form of the OTLP trace id, present on spans and absent on plain log events. */
    private static final String TRACE_ID_RAW_FIELD = "trace_id.raw";

    /**
     * The span that records the backend call. It is the one span whose {@code request.uri} is the backend's own
     * address rather than the gateway's, so it cannot be found by the API context — only by expanding a trace id.
     */
    public static final String BACKEND_SPAN_NAME = "API:Backend_Latency";

    /**
     * Fields projected onto each span hit. Requesting them explicitly keeps the response to what is asserted —
     * Moesif events also carry request/response headers and bodies, which are large and irrelevant here.
     */
    private static final String SPAN_FIELDS = "action_name,trace_id,span,direction,request.uri,request.time,"
            + "response.status,duration_ms,resource";

    /**
     * How long to keep asking Moesif for an event before giving up. Moesif treats anything older than about five
     * minutes as a "late event", so the window has to stay well inside that. The exporter batches a POST roughly
     * every 5s, so this is generous for a healthy delivery.
     */
    private static final long DEFAULT_TIMEOUT_MILLIS = 120_000L;

    private static final Log log = LogFactory.getLog(MoesifManagementApiClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MoesifManagementApiClient() {
    }

    /** One span as Moesif models it: the OTLP span name becomes {@code action_name} and its parent is a link. */
    public static final class MoesifSpan {

        private final String actionName;
        private final String direction;
        private final String spanId;
        private final String parentId;
        private final String uri;
        private final Integer status;

        MoesifSpan(String actionName, String direction, String spanId, String parentId, String uri,
                Integer status) {
            this.actionName = actionName;
            this.direction = direction;
            this.spanId = spanId;
            this.parentId = parentId;
            this.uri = uri;
            this.status = status;
        }

        public String actionName() {
            return actionName;
        }

        /** Moesif's routing classification of the span: {@code Incoming}, {@code Internal} or {@code Outgoing}. */
        public String direction() {
            return direction;
        }

        public String spanId() {
            return spanId;
        }

        /** The enclosing span's id, or {@code null} for the trace's root span. */
        public String parentId() {
            return parentId;
        }

        public String uri() {
            return uri;
        }

        public Integer status() {
            return status;
        }

        public boolean isRoot() {
            return parentId == null || parentId.isBlank();
        }

        @Override
        public String toString() {
            return actionName + " [" + direction + "] span=" + spanId + " parent=" + (isRoot() ? "<root>" : parentId)
                    + " status=" + status + " uri=" + uri;
        }
    }

    /** A whole trace, flattened into spans and indexed by parent so the chain can be walked. */
    public static final class MoesifTrace {

        private final String traceId;
        private final List<MoesifSpan> spans;

        MoesifTrace(String traceId, List<MoesifSpan> spans) {
            this.traceId = traceId;
            this.spans = spans;
        }

        public String traceId() {
            return traceId;
        }

        public List<MoesifSpan> spans() {
            return spans;
        }

        public List<MoesifSpan> roots() {
            List<MoesifSpan> found = new ArrayList<>();
            for (MoesifSpan span : spans) {
                if (span.isRoot()) {
                    found.add(span);
                }
            }
            return found;
        }

        public List<MoesifSpan> childrenOf(MoesifSpan parent) {
            List<MoesifSpan> found = new ArrayList<>();
            for (MoesifSpan span : spans) {
                if (!span.isRoot() && span.parentId().equals(parent.spanId())) {
                    found.add(span);
                }
            }
            return found;
        }

        public MoesifSpan byActionName(String name) {
            for (MoesifSpan span : spans) {
                if (name.equals(span.actionName())) {
                    return span;
                }
            }
            return null;
        }

        /**
         * True when the trace carries the gateway's full chain: a single root, exactly one child of it, and a
         * backend span under that child. Polling on this rather than on "some span arrived" is what keeps a
         * partially exported trace from being read as a failure — the collector exposes a trace as soon as its
         * first span lands, so a trace can still be mid-flight.
         */
        public boolean hasCompleteGatewayChain() {
            List<MoesifSpan> roots = roots();
            if (roots.size() != 1) {
                return false;
            }
            List<MoesifSpan> resourceSpans = childrenOf(roots.get(0));
            return resourceSpans.size() == 1 && byActionName(BACKEND_SPAN_NAME) != null;
        }
    }

    /**
     * True when both Moesif credentials are present in the runner JVM's environment.
     *
     * <p>The live block skips rather than fails when this is false, so a CI job without the secrets does not go
     * red for a missing credential.
     */
    public static boolean credentialsAvailable() {
        return isSet(MANAGEMENT_API_KEY_ENV) && isSet(COLLECTOR_APP_ID_ENV);
    }

    /**
     * Polls Moesif until a trace carrying {@code contextFragment} AND the gateway's whole span chain is readable,
     * then returns that trace.
     *
     * @param contextFragment a value unique to the run being asserted on — the API context, which
     *                        {@code ${UNIQUE:…}} resolution makes distinct per runner instance
     * @return the matching trace, whose {@link MoesifTrace#hasCompleteGatewayChain()} holds
     * @throws AssertionError if no such trace arrives within the timeout
     */
    public static MoesifTrace awaitTraceFor(String contextFragment) throws InterruptedException {
        return awaitTraceFor(contextFragment, DEFAULT_TIMEOUT_MILLIS);
    }

    /**
     * Polls Moesif until a trace carrying {@code contextFragment} AND the gateway's whole span chain is readable,
     * then returns that trace.
     *
     * @param contextFragment a value unique to the run being asserted on
     * @param timeoutMillis   how long to keep asking Moesif
     * @return the matching trace, whose {@link MoesifTrace#hasCompleteGatewayChain()} holds
     * @throws AssertionError if no such trace arrives within the timeout
     */
    public static MoesifTrace awaitTraceFor(String contextFragment, long timeoutMillis)
            throws InterruptedException {

        String key = System.getenv(MANAGEMENT_API_KEY_ENV);
        if (key == null || key.isBlank()) {
            throw new AssertionError("Environment variable " + MANAGEMENT_API_KEY_ENV + " is not set, so delivered "
                    + "spans cannot be read back from Moesif. The block should have skipped before reaching here.");
        }

        long pollStart = System.currentTimeMillis();
        String lastDiagnostic = "no search was performed";
        while (System.currentTimeMillis() - pollStart < timeoutMillis) {
            try {
                // Step 1 — the context locates candidate traces. It is matched on the URI only because that is
                // the field the run-unique context is guaranteed to appear in; the full chain comes in step 2.
                Set<String> traceIds = locateTraceIds(key, contextFragment);
                lastDiagnostic = "context '" + contextFragment + "' matched " + traceIds.size() + " trace(s)";

                // Step 2 — expand each candidate and take the first whose whole chain is present. Candidates are
                // expected because the invocation step retries a not-yet-routable API, and each attempt produces
                // its own trace.
                for (String traceId : traceIds) {
                    MoesifTrace trace = readTrace(key, traceId);
                    if (trace != null && trace.hasCompleteGatewayChain()) {
                        return trace;
                    }
                    lastDiagnostic += "; trace " + traceId + " has " + (trace == null ? 0 : trace.spans().size())
                            + " span(s) so far";
                }
            } catch (IOException e) {
                lastDiagnostic = "search failed: " + e;
                log.warn("Moesif search for '" + contextFragment + "' failed, retrying: " + e);
            }
            Utils.pollPause(pollStart, Constants.RETRY_INTERVAL_TIME);
        }
        throw new AssertionError("Moesif never returned a complete exported trace for the API context '"
                + contextFragment + "' within " + (timeoutMillis / 1000) + "s. Last poll: " + lastDiagnostic
                + ". Either the spans were rejected on ingest, they were still in flight, or " + MANAGEMENT_API_KEY_ENV
                + " is a Management API key for a different Moesif application than the one the gateway exports to.");
    }

    /**
     * Returns every trace id whose events carry {@code contextFragment}, discarding log events (which have no
     * trace id) so only real spans contribute.
     */
    private static Set<String> locateTraceIds(String key, String contextFragment) throws IOException {

        ObjectNode clause = MAPPER.createObjectNode();
        ArrayNode must = clause.putObject("bool").putArray("must");
        must.addObject().putObject("wildcard").put(URI_RAW_FIELD, "*" + contextFragment + "*");
        must.addObject().putObject("exists").put("field", TRACE_ID_RAW_FIELD);

        ObjectNode body = MAPPER.createObjectNode();
        body.set("post_filter", clause);
        body.put("size", 25);
        ArrayNode source = body.putArray("_source");
        source.add("trace_id");

        JsonNode hits = post(key, body, "locating trace for " + contextFragment).path("hits").path("hits");
        Set<String> traceIds = new LinkedHashSet<>();
        for (JsonNode hit : hits) {
            String traceId = hit.path("_source").path("trace_id").asText(null);
            if (traceId != null && !traceId.isBlank()) {
                traceIds.add(traceId);
            }
        }
        return traceIds;
    }

    /** Reads every span recorded under {@code traceId}, oldest first. */
    private static MoesifTrace readTrace(String key, String traceId) throws IOException {

        ObjectNode clause = MAPPER.createObjectNode();
        ArrayNode must = clause.putObject("bool").putArray("must");
        must.addObject().putObject("terms").putArray(TRACE_ID_RAW_FIELD).add(traceId);
        must.addObject().putObject("exists").put("field", TRACE_ID_RAW_FIELD);

        ObjectNode body = MAPPER.createObjectNode();
        body.set("post_filter", clause);
        body.put("size", 100);
        // Moesif stores request.time as a string, so the sort has to declare the unmapped long type — and it must
        // be declared, or a span without a request.time (there are some) fails the sort rather than sorting last.
        body.putArray("sort").addObject().putObject("request.time")
                .put("order", "asc").put("unmapped_type", "long");
        body.putArray("_source");
        for (String field : SPAN_FIELDS.split(",")) {
            body.withArray("_source").add(field.trim());
        }

        JsonNode hits = post(key, body, "reading trace " + traceId).path("hits").path("hits");
        if (!hits.isArray() || hits.isEmpty()) {
            return null;
        }
        List<MoesifSpan> spans = new ArrayList<>();
        for (JsonNode hit : hits) {
            JsonNode source = hit.path("_source");
            JsonNode span = source.path("span");
            spans.add(new MoesifSpan(
                    source.path("action_name").asText(null),
                    source.path("direction").asText(null),
                    span.path("id").asText(null),
                    span.path("parent_id").asText(null),
                    source.path("request").path("uri").asText(null),
                    source.path("response").path("status").isNumber()
                            ? source.path("response").path("status").asInt() : null));
        }
        return new MoesifTrace(traceId, spans);
    }

    /** POSTs a search body and fails loudly on a non-200, since every caller here treats non-200 as fatal. */
    private static JsonNode post(String key, ObjectNode body, String what) throws IOException {

        String url = managementBaseUrl() + SEARCH_EVENTS_PATH + "?from=-1d&to=now";
        HttpResponse response = SimpleHTTPClient.getInstance()
                .doPost(url, bearerHeaders(key), body.toString(), "application/json");
        if (response.getResponseCode() != 200) {
            String detail = "status=" + response.getResponseCode() + " body=" + abbreviate(response.getData());
            // 401/403 cannot be fixed by asking again — the credential is wrong, revoked, or for another
            // application — so this is raised at once rather than consuming the whole poll window.
            if (response.getResponseCode() == 401 || response.getResponseCode() == 403) {
                throw new AssertionError("Moesif Management API rejected " + what + " (" + detail + "). Check that "
                        + MANAGEMENT_API_KEY_ENV + " holds a Management API key (its payload carries an 'org', an "
                        + "'app' and a 'read:events' scope). Neither the collector application id nor a Moesif "
                        + "portal session token from the browser network tab is accepted here.");
            }
            throw new AssertionError("Moesif Management API failed " + what + " (" + detail + ").");
        }
        if (response.getData() == null || response.getData().isBlank()) {
            throw new AssertionError("Moesif Management API returned an empty body for " + what + ".");
        }
        try {
            return MAPPER.readTree(response.getData());
        } catch (IOException e) {
            throw new AssertionError("Moesif Management API returned a body for " + what + " that is not JSON: "
                    + abbreviate(response.getData()));
        }
    }

    private static Map<String, String> bearerHeaders(String managementApiKey) {
        Map<String, String> headers = new HashMap<>();
        // Trimmed deliberately: a credential pasted out of a browser often carries a trailing space, which fails
        // as a "malformed" token in exactly the same way as a genuinely bad key.
        headers.put("Authorization", "Bearer " + managementApiKey.trim());
        headers.put("Accept", "application/json");
        return headers;
    }

    /** The Management API host, overridable for an EU-resident account. */
    public static String managementBaseUrl() {
        String override = System.getenv(MANAGEMENT_BASE_URL_ENV);
        return (override == null || override.isBlank()) ? DEFAULT_MANAGEMENT_BASE_URL : override;
    }

    private static boolean isSet(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return "<null>";
        }
        return value.length() <= 400 ? value : value.substring(0, 400) + "…";
    }
}