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
import java.util.regex.Pattern;

/**
 * Readiness contract for the gateway key-manager event consumer.
 *
 * <p>HTTP/system readiness does not guarantee that the gateway is subscribed to the key-manager topic. An API key
 * generated before that consumer is listening can be accepted by the control plane while never becoming available to
 * gateway authentication. This gate observes the product's own connection and listener markers before a test emits a
 * key-created event.</p>
 */
public final class KeyManagerReadiness {

    private static final String SERVER_LOG = "wso2carbon.log";
    private static final Pattern KEY_MANAGER_CONNECTION_READY = Pattern.compile(
            "Connection successfully created towards the JMS provider for the listener:\\s*Siddhi-JMS-Consumer#keyManager\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern KEY_MANAGER_LISTENER_READY = Pattern.compile(
            "Started to listen on destination\\s*:\\s*keyManager\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern KEY_MANAGER_FAILURE = Pattern.compile(
            "\\b(?:keyManager|key-manager)\\b[^\\r\\n]*\\b(?:error|exception|failed|permission\\s+denied|403|504)\\b",
            Pattern.CASE_INSENSITIVE);

    private KeyManagerReadiness() {
    }

    /** Polls the gateway log until the key-manager JMS consumer is connected and listening. */
    public static Result await(ApimRuntime runtime, int timeoutSeconds) throws InterruptedException {

        return Utils.retryUntil(timeoutSeconds * 1000L, () -> inspect(runtime), Result::ready);
    }

    /** Takes one gateway-log snapshot relevant to the key-manager consumer. */
    public static Result inspect(ApimRuntime runtime) {

        String gatewayLog = runtime.readGatewayLogFile(SERVER_LOG);
        int connectionReady = lastMatchEnd(gatewayLog, KEY_MANAGER_CONNECTION_READY);
        int listenerReady = lastMatchEnd(gatewayLog, KEY_MANAGER_LISTENER_READY);
        int failure = lastMatchStart(gatewayLog, KEY_MANAGER_FAILURE);
        return new Result(connectionReady >= 0 && listenerReady >= 0
                && failure < Math.min(connectionReady, listenerReady), diagnostics(gatewayLog));
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

    private static String diagnostics(String gatewayLog) {

        List<String> evidence = new ArrayList<>();
        for (String line : gatewayLog.split("\\R")) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("keymanager") || lower.contains("key-manager")) {
                evidence.add(line.trim());
            }
        }
        int from = Math.max(0, evidence.size() - 12);
        StringBuilder result = new StringBuilder("Gateway key-manager markers=")
                .append(evidence.isEmpty() ? "not observed" : "observed");
        for (int i = from; i < evidence.size(); i++) {
            String line = evidence.get(i);
            result.append("\n  ").append(line, 0, Math.min(line.length(), 500));
        }
        return result.toString();
    }

    public record Result(boolean ready, String diagnostics) {
    }
}
