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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.testcontainers.ApimRuntime;
import org.wso2.am.testcontainers.DistributedDynamicApimContainer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Greps the product's OWN log inside the block's container(s) and renders the matches as an addendum to a failing
 * assertion message.
 *
 * <p>This is a DIAGNOSTIC, never an assertion. Nothing here decides pass or fail — the point is that a failure
 * whose cause lives only in the server log stops being reported as a bare status mismatch. The endpoint-certificate
 * arc is the motivating case: when the control plane's trust-store write is torn by a concurrent read the whole
 * chain is invisible from the test's side, which reports only {@code expected [200] but found [500]}, while the
 * server log says in as many words that it could not read the trust store and then could not reach
 * {@code /internal/data/v1/endpoint-certificates}. Attaching those lines is the difference between a flake nobody
 * can action and a report someone can take to the product team.
 *
 * <p><b>Never throws.</b> A diagnostic that fails must not replace the real failure it was trying to explain, so
 * every path returns text — including "diagnostics unavailable, because …".
 *
 * <p><b>Topology-agnostic.</b> Both {@code DynamicApimContainer} and {@code DistributedDynamicApimContainer}
 * implement {@link ApimRuntime}, which exposes the gateway and control-plane log readers. In the all-in-one lane
 * those are the same file, so it is read once; in the distributed lane they are different containers and both are
 * read and labelled, because the writer (control plane) and the reader (gateway) of the shared trust store are
 * different JVMs there.
 */
public final class ContainerLogDiagnostics {

    private static final Log log = LogFactory.getLog(ContainerLogDiagnostics.class);

    /** The carbon server log every APIM component writes to. */
    public static final String SERVER_LOG = "wso2carbon.log";

    /**
     * The signature of the product race behind the endpoint-certificate arc, in the order the chain unfolds:
     * the torn read of {@code client-truststore.jks}, the unusable SSL context it falls back to, and the
     * consequent failure to fetch the certificate the gateway was told to deploy.
     *
     * <p><b>One other channel shares this exposure, and has no test yet.</b> {@code GA_CONFIG_UPDATE} (Google
     * Analytics config) reaches the same racy code: {@code GatewayJMSMessageListener} handles it by calling
     * {@code GoogleAnalyticsConfigDeployer(tenantDomain).deploy()}, whose {@code invokeService} builds its client
     * through {@code APIUtil.getHttpClient} ({@code GoogleAnalyticsConfigDeployer.java:119}) and therefore
     * {@code APIUtil.getSSLContext()} — the same re-read of {@code client-truststore.jks}, on a client built once
     * and then retried. Like the certificate event it is AT-MOST-ONCE: the product gives up after ~465s and
     * nothing re-fires it, so waiting longer cannot recover it.
     *
     * <p>There is deliberately no gate for it today because there is no test to attach one to — verified with
     * positive controls: zero behavioural coverage in EITHER suite (0 hits across 338 legacy test files, 0 in
     * v2; the {@code GoogleAnalyticsTracking} matches in legacy are synapse-config fixtures where the handler is
     * chain boilerplate, plus two OpenAPI {@code example:} strings). If a GA scenario is ever written, it needs
     * from its first commit: the prerequisite gated with {@link HealGate#awaitOrHeal} re-firing the GA config
     * update as the heal (mirror {@code EndpointCertificateSteps}), the gateway-effect kept as the ASSERTION
     * target rather than the probe, and {@link #explainTrustStoreRace} attached on exhaustion.
     */
    public static final List<String> TRUST_STORE_RACE_PATTERNS = List.of(
            "Unable to read the trust store file",
            "No X509TrustManager implementation available",
            "Failed to retrieve /internal/data/v1/endpoint-certificates");

    /**
     * The MySQL constraint the control plane violates when two registry writes race on the same path, and the
     * prefix that carries which path failed. Matched on the constraint name rather than the message so a
     * different foreign-key failure is never miscounted as this one.
     */
    private static final String REGISTRY_FK_CONSTRAINT = "REG_RESOURCE_FK_BY_PATH_ID";
    private static final String FAILED_PATH_PREFIX = "Failed to add resource to path ";

    /** Cap on reported matches per source, newest last — enough to show the chain without flooding the report. */
    private static final int MAX_MATCHES_PER_SOURCE = 12;
    /** Cap on a single rendered line; carbon stack frames are long and add nothing after the message. */
    private static final int MAX_LINE_LENGTH = 300;

    private ContainerLogDiagnostics() {
    }

    /**
     * Renders every line of the block's server log(s) matching any of {@code patterns}, as a block of text meant to
     * be appended to an assertion message. Returns an empty string when nothing matched, so a caller can append it
     * unconditionally without producing a dangling heading.
     *
     * @param what     short description of what was being awaited, used in the heading
     * @param patterns substrings to match; a line matching ANY of them is reported
     */
    public static String explain(String what, List<String> patterns) {
        return explain(what, SERVER_LOG, patterns);
    }

    /** {@link #explain(String, List)} against a named log file. */
    public static String explain(String what, String fileName, List<String> patterns) {
        try {
            Object candidate = TestContext.get("blockApimContainer");
            if (!(candidate instanceof ApimRuntime runtime)) {
                return note("no APIM runtime in the test context, so the server log could not be read");
            }
            StringBuilder report = new StringBuilder();
            if (runtime instanceof DistributedDynamicApimContainer) {
                // Distinct JVMs in this topology: the control plane writes the trust store, the gateway reads it.
                appendSource(report, "control plane", safeRead(() -> runtime.readControlPlaneLogFile(fileName)),
                        patterns);
                appendSource(report, "gateway", safeRead(() -> runtime.readGatewayLogFile(fileName)), patterns);
            } else {
                // All-in-one: one JVM, and readGatewayLogFile/readControlPlaneLogFile resolve to the same file.
                appendSource(report, "server", safeRead(() -> runtime.readGatewayLogFile(fileName)), patterns);
            }
            if (report.length() == 0) {
                return "";
            }
            return "\n\n--- product log evidence for " + what + " (" + fileName + ") ---\n" + report;
        } catch (Throwable diagnosticFailed) {
            // Deliberately Throwable: this runs on the failure path and must never mask the real assertion error.
            log.warn("Container log diagnostics failed (continuing with the original failure): " + diagnosticFailed);
            return note("the server log could not be read: " + diagnosticFailed);
        }
    }

    /**
     * The endpoint-certificate trust-store race, ready to append to a failing gate. Named rather than inlined so
     * the patterns live in one place and a second consumer cannot drift from the first.
     */
    public static String explainTrustStoreRace(String what) {
        return explain(what, TRUST_STORE_RACE_PATTERNS);
    }

    /**
     * The control plane's registry foreign-key violations, reported as a COUNT rather than as lines.
     *
     * <p>This one is distributed-only and arrives in the thousands — 14,508 on a single path in one measured
     * block — so listing lines the way {@link #explain} does would flood a report and tell the reader nothing.
     * The number and the affected paths are the signal.
     *
     * <p><b>Why a failing assertion wants to know.</b> The violations are not evenly spread; they come in bursts,
     * and failures cluster inside them. Measured across a CI run and a local distributed run, 4 of 4
     * registry-or-lifecycle failures landed inside a burst while neither of the two non-registry events did (a
     * platform-gateway xDS failure with a separately established cause, and a drop that healed). One of those
     * four was {@code 903220 "Failed to get API"} — a registry READ failing mid-burst, which is a direct
     * mechanistic link rather than a coincidence of timing.
     *
     * <p>So this does not diagnose a failure by itself; it says "the control plane's registry was in a
     * contention window when this failed", which is the difference between an unattributable flake and a lead.
     * The violations are a MARKER for that window, not its cause: 99.96% of them are on the throttling
     * event-topic paths, which have nothing to do with API artifacts or lifecycle state. Both are symptoms of the
     * same shared-database contention.
     *
     * <p>Zero occurrences render as an empty string, so a caller can append it unconditionally. All-in-one runs
     * are always empty (positive-controlled: zero there, thousands in distributed).
     */
    public static String explainRegistryContention(String what) {
        try {
            Object candidate = TestContext.get("blockApimContainer");
            if (!(candidate instanceof ApimRuntime runtime)) {
                return "";
            }
            String content = safeRead(() -> runtime.readControlPlaneLogFile(SERVER_LOG));
            if (content == null) {
                return "";
            }
            int violations = 0;
            Map<String, Integer> paths = new LinkedHashMap<>();
            for (String line : content.split("\\R")) {
                // BOTH markers, on the SAME line. The constraint name alone also appears on the stack-trace
                // frames that follow each failure — counting those inflated a measured 16,244 violations to
                // 24,366. One "Failed to add resource to path ..." line is one violation, and every such line
                // carries the constraint (verified on a real run), so requiring both is exact, not merely safer.
                int at = line.indexOf(FAILED_PATH_PREFIX);
                if (at < 0 || !line.contains(REGISTRY_FK_CONSTRAINT)) {
                    continue;
                }
                violations++;
                String tail = line.substring(at + FAILED_PATH_PREFIX.length()).trim();
                int end = tail.indexOf(' ');
                String path = end > 0 ? tail.substring(0, end) : tail;
                // The message renders as "... path <path>. Cannot add or update a child row: ...", so the path
                // carries a trailing sentence period that would otherwise appear in the report.
                paths.merge(path.endsWith(".") ? path.substring(0, path.length() - 1) : path, 1, Integer::sum);
            }
            if (violations == 0) {
                return "";
            }
            StringBuilder report = new StringBuilder("\n\n--- control-plane registry contention during " + what
                    + ": " + violations + " foreign-key violation(s) in this run ---\n");
            paths.forEach((path, count) -> report.append("  ").append(count).append("x ").append(path)
                    .append('\n'));
            report.append("  Failures cluster inside these bursts; the violations MARK a shared-registry"
                    + " contention window rather than causing this failure directly.\n");
            return report.toString();
        } catch (Throwable diagnosticFailed) {
            // Never mask the real assertion error with a diagnostic's own failure.
            log.warn("Registry-contention diagnostics failed (continuing with the original failure): "
                    + diagnosticFailed);
            return "";
        }
    }

    private static void appendSource(StringBuilder report, String label, String content, List<String> patterns) {
        if (content == null) {
            report.append('[').append(label).append("] log unavailable\n");
            return;
        }
        List<String> matches = new ArrayList<>();
        for (String line : content.split("\\R")) {
            for (String pattern : patterns) {
                if (line.contains(pattern)) {
                    matches.add(line.length() > MAX_LINE_LENGTH ? line.substring(0, MAX_LINE_LENGTH) + " …" : line);
                    break;
                }
            }
        }
        if (matches.isEmpty()) {
            return;
        }
        int from = Math.max(0, matches.size() - MAX_MATCHES_PER_SOURCE);
        if (from > 0) {
            report.append('[').append(label).append("] … ").append(from).append(" earlier match(es) omitted\n");
        }
        for (String match : matches.subList(from, matches.size())) {
            report.append('[').append(label).append("] ").append(match).append('\n');
        }
    }

    /** Reads a log, mapping any failure to {@code null} so one unreadable source cannot lose the other. */
    private static String safeRead(LogRead read) {
        try {
            return read.get();
        } catch (Throwable unreadable) {
            log.warn("Could not read a container log for diagnostics: " + unreadable);
            return null;
        }
    }

    private static String note(String why) {
        return "\n\n--- product log evidence unavailable: " + why + " ---";
    }

    @FunctionalInterface
    private interface LogRead {
        String get();
    }
}
