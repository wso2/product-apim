/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.cucumbertests.utils;

import org.wso2.am.testcontainers.ApimRuntime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Readiness contract for the asynchronous throttle-data path.
 *
 * <p>Carbon HTTP readiness only proves that a component serves requests. This gate additionally requires the
 * Traffic Manager publisher and Gateway JMS consumer to be active. A readiness marker is accepted only when it
 * is newer than any later registry/binding failure in that component's current log, so a historical successful
 * startup cannot hide a subsequent {@code REG_PATH} failure.</p>
 */
public final class ThrottleDataReadiness {

    private static final String SERVER_LOG = "wso2carbon.log";
    private static final Pattern TM_PUBLISHER_READY = Pattern.compile(
            "Event Publisher configuration successfully deployed and in active state\\s*:\\s*jmsEventPublisher2",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern GATEWAY_CONSUMER_READY = Pattern.compile(
            "Started to listen on destination\\s*:\\s*throttleData\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTROL_PLANE_RECOVERED = Pattern.compile(
            "WSO2 Carbon started in", Pattern.CASE_INSENSITIVE);
    private static final Pattern THROTTLE_FAILURE = Pattern.compile(
            "REG_PATH|jms\\.subscriptions|permission denied.*(?:binding|throttledata)"
                    + "|(?:binding|throttledata).*permission denied"
                    + "|(?:binding|throttledata).*(?:\\b403\\b|\\b504\\b)",
            Pattern.CASE_INSENSITIVE);
    private static final long STABLE_PIPELINE_MILLIS = 3000L;

    private ThrottleDataReadiness() {
    }

    /** Polls the component logs until the complete throttle-data pipeline is healthy or the deadline expires. */
    public static Result await(ApimRuntime runtime, int timeoutSeconds) throws InterruptedException {

        AtomicLong healthySince = new AtomicLong(-1L);
        return Utils.retryUntil(timeoutSeconds * 1000L,
                () -> inspect(runtime), result -> {
                    if (!result.ready()) {
                        healthySince.set(-1L);
                        return false;
                    }
                    long now = System.currentTimeMillis();
                    long firstHealthy = healthySince.get();
                    if (firstHealthy < 0L) {
                        healthySince.compareAndSet(-1L, now);
                        return false;
                    }
                    return now - firstHealthy >= STABLE_PIPELINE_MILLIS;
                });
    }

    /** Takes one snapshot of all logs relevant to the throttle-data pipeline. */
    public static Result inspect(ApimRuntime runtime) {
        String controlPlaneLog = runtime.readControlPlaneLogFile(SERVER_LOG);
        String trafficManagerLog = runtime.readTrafficManagerLogFile(SERVER_LOG);
        String gatewayLog = runtime.readGatewayLogFile(SERVER_LOG);

        boolean controlPlaneHealthy = markerIsCurrent(controlPlaneLog, CONTROL_PLANE_RECOVERED);
        boolean publisherReady = markerIsCurrent(trafficManagerLog, TM_PUBLISHER_READY);
        boolean consumerReady = markerIsCurrent(gatewayLog, GATEWAY_CONSUMER_READY);
        return new Result(controlPlaneHealthy, publisherReady, consumerReady,
                diagnostics("Control Plane", controlPlaneLog)
                        + diagnostics("Traffic Manager", trafficManagerLog)
                        + diagnostics("Gateway", gatewayLog));
    }

    /**
     * A marker from an earlier startup attempt is not sufficient. The last relevant event must be the positive
     * marker; this permits a product retry that genuinely recovers while rejecting a pipeline that failed after
     * it had once reported ready.
     */
    private static boolean markerIsCurrent(String log, Pattern readyPattern) {
        int readyIndex = lastMatchEnd(log, readyPattern);
        int failureIndex = lastMatchStart(log, THROTTLE_FAILURE);
        return readyIndex >= 0 && failureIndex < readyIndex;
    }

    private static int lastMatchEnd(String value, Pattern pattern) {
        var matcher = pattern.matcher(value);
        int end = -1;
        while (matcher.find()) {
            end = matcher.end();
        }
        return end;
    }

    private static int lastMatchStart(String value, Pattern pattern) {
        var matcher = pattern.matcher(value);
        int start = -1;
        while (matcher.find()) {
            start = matcher.start();
        }
        return start;
    }

    private static String diagnostics(String component, String log) {
        List<String> evidence = new ArrayList<>();
        for (String line : log.split("\\R")) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("jmseventpublisher2") || lower.contains("throttledata")
                    || lower.contains("jms.subscriptions") || THROTTLE_FAILURE.matcher(line).find()
                    || lower.contains("event publisher")) {
                evidence.add(line.trim());
            }
        }
        int from = Math.max(0, evidence.size() - 10);
        StringBuilder result = new StringBuilder(" ").append(component).append(" marker=")
                .append(evidence.isEmpty() ? "not observed" : "observed");
        for (int i = from; i < evidence.size(); i++) {
            String line = evidence.get(i);
            result.append("\n  ").append(line, 0, Math.min(line.length(), 500));
        }
        return result.toString();
    }

    public record Result(boolean controlPlaneHealthy, boolean publisherReady, boolean consumerReady,
                         String diagnostics) {
        public boolean ready() {
            return controlPlaneHealthy && publisherReady && consumerReady;
        }
    }
}
