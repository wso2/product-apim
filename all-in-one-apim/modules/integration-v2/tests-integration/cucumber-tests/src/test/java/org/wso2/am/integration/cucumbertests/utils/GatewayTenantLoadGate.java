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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.testcontainers.ApimRuntime;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/** Ensures framework-provisioned tenants finish Gateway load-time API deployment before block work proceeds. */
public final class GatewayTenantLoadGate {

    private static final Log logger = LogFactory.getLog(GatewayTenantLoadGate.class);
    private static final String SERVER_LOG = "wso2carbon.log";
    private static final Pattern SERVER_START = Pattern.compile("Starting WSO2 Carbon", Pattern.CASE_INSENSITIVE);
    private static final String DEPLOYMENT_COMPLETE = "Synapse Artifacts deployed Successfully in the Gateway";
    private static final long POLL_INTERVAL_MILLIS = 1000L;

    private GatewayTenantLoadGate() {
    }

    /** Triggers and waits for each tenant sequentially on the runtime's Gateway node. */
    public static void await(ApimRuntime runtime, String blockLabel, List<String> tenantDomains) {

        for (String tenantDomain : tenantDomains) {
            awaitTenant(runtime, blockLabel, tenantDomain);
        }
    }

    private static void awaitTenant(ApimRuntime runtime, String blockLabel, String tenantDomain) {

        String gateway = runtime.getGatewayHttpsUrl();
        String currentLog;
        try {
            currentLog = currentBootLog(runtime.readGatewayLogFile(SERVER_LOG));
        } catch (RuntimeException e) {
            throw failure(gateway, tenantDomain, "could not read the current Gateway boot log: " + e);
        }
        if (currentLog == null) {
            throw failure(gateway, tenantDomain, "current boot has no 'Starting WSO2 Carbon' marker");
        }
        long startedAt = System.currentTimeMillis();
        triggerTenantLoad(gateway, tenantDomain);

        AtomicReference<String> lastEvidence = new AtomicReference<>("no tenant-load lines observed after trigger");
        try {
            long remaining = Constants.RUNTIME_PROPAGATION_TIMEOUT - (System.currentTimeMillis() - startedAt);
            Result result = remaining <= 0 ? null : Utils.retryUntilWithInterval(remaining,
                    POLL_INTERVAL_MILLIS, () -> inspect(runtime, tenantDomain, lastEvidence),
                    Result::ready);
            if (result == null || !result.ready()) {
                throw failure(gateway, tenantDomain, lastEvidence.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure(gateway, tenantDomain, "wait interrupted; last log evidence: " + lastEvidence.get());
        }
        logger.info("Gateway tenant-load gate passed for block '" + blockLabel + "': gateway=" + gateway
                + " tenant=" + tenantDomain + " elapsedMs=" + (System.currentTimeMillis() - startedAt)
                + " evidence=" + lastEvidence.get());
    }

    private static void triggerTenantLoad(String gateway, String tenantDomain) {

        String url = gateway + (gateway.endsWith("/") ? "" : "/") + "t/" + tenantDomain + "/";
        try {
            HttpResponse response = SimpleHTTPClient.getInstance().doGetWithSocketTimeout(url,
                    Collections.emptyMap(), (int) Constants.RUNTIME_PROPAGATION_TIMEOUT);
            if (response == null) {
                throw new IOException("Gateway request returned no HTTP response");
            }
        } catch (Exception e) {
            throw failure(gateway, tenantDomain, "trigger request to " + url + " failed: " + e);
        }
    }

    private static Result inspect(ApimRuntime runtime, String tenantDomain, AtomicReference<String> lastEvidence) {

        String all;
        try {
            all = runtime.readGatewayLogFile(SERVER_LOG);
        } catch (RuntimeException e) {
            lastEvidence.set("Gateway log read failed: " + e);
            return new Result(false);
        }
        String current = currentBootLog(all);
        if (current == null) {
            lastEvidence.set("current boot has no 'Starting WSO2 Carbon' marker");
            return new Result(false);
        }
        String loadedLine = null;
        String completedLine = null;
        Pattern loaded = Pattern.compile("Loaded tenant\\s+" + Pattern.quote(tenantDomain) + "\\s+in\\s+\\d+\\s+ms");
        for (String line : current.split("\\R")) {
            if (loaded.matcher(line).find()) {
                loadedLine = line.trim();
            }
            if (loadedLine != null && line.contains(DEPLOYMENT_COMPLETE)) {
                completedLine = line.trim();
                break;
            }
        }
        String evidence = "load=" + (loadedLine == null ? "pending" : loadedLine)
                + "; load-time-deployment=" + (completedLine == null ? "pending" : completedLine);
        lastEvidence.set(evidence);
        return new Result(loadedLine != null && completedLine != null);
    }

    private static String currentBootLog(String log) {

        if (log == null) {
            return null;
        }
        var matcher = SERVER_START.matcher(log);
        int start = -1;
        while (matcher.find()) {
            start = matcher.start();
        }
        return start < 0 ? null : log.substring(start);
    }

    private static IllegalStateException failure(String gateway, String tenantDomain, String evidence) {

        return new IllegalStateException("Gateway tenant-load gate failed for gateway '" + gateway + "', tenant '"
                + tenantDomain + "' within " + Constants.RUNTIME_PROPAGATION_TIMEOUT + " ms; last log evidence: "
                + evidence);
    }

    private record Result(boolean ready) {
    }
}
