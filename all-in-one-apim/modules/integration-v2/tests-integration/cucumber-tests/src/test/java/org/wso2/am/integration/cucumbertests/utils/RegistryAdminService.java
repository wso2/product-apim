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

import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.automation.engine.context.beans.User;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Narrow wrapper for the Carbon registry admin SOAP operations that have no REST equivalent.
 *
 * <p>{@link org.wso2.am.integration.cucumbertests.utils.listeners.BlockLifecycleListener} calls this during
 * block startup to ensure the throttle-data registry hierarchy exists before the event subscription is
 * initialized. The listener supplies each tenant administrator's credentials; this utility performs the SOAP
 * registry operations using that actor rather than a side-channel identity.</p>
 * <p>It is also called by explicit setup steps and the distributed infrastructure initializer, which establishes
 * the shared broker hierarchy before consumers start. The caller supplies the actor; no token is minted here.</p>
 */
public final class RegistryAdminService {

    private static final Log LOG = LogFactory.getLog(RegistryAdminService.class);
    private static final long INITIALIZATION_TIMEOUT_MILLIS = 180_000;
    private static final String SERVICE_PATH = "services/ResourceAdminService";
    private static final String OPERATION_NAMESPACE = "http://services.resource.registry.carbon.wso2.org";
    private static final String COLLECTION_MEDIA_TYPE = "application/vnd.wso2.registry.collection";
    private static final String THROTTLE_DATA_PATH = "/_system/governance/event/topics/throttledata";
    private static final String THROTTLE_DATA_SUBSCRIPTIONS_PATH = THROTTLE_DATA_PATH + "/jms.subscriptions";

    private RegistryAdminService() {
    }

    /** Waits for the actual registry service, not just Carbon's HTTP listener, before dependent components start. */
    public static void awaitAvailable(String baseUrl, User actor, long timeoutMillis) throws InterruptedException {
        HttpResponse response = Utils.retryUntilWithInterval(timeoutMillis, 1000L, () -> {
            HttpResponse observed = getResourceData(baseUrl, actor, "/");
            if (observed != null && observed.getResponseCode() != 200
                    && observed.getResponseCode() != 404 && observed.getResponseCode() != 502
                    && observed.getResponseCode() != 503) {
                throw new IllegalStateException("Registry service prerequisite failed: " + describe(observed));
            }
            return observed;
        }, observed -> observed != null && observed.getResponseCode() == 200);
        if (response == null || response.getResponseCode() != 200) {
            throw new IllegalStateException("Registry service was not available within " + timeoutMillis
                    + "ms: " + describe(response));
        }
    }

    /** Ensures the complete hierarchy within one bounded initialization window. */
    public static boolean ensureThrottleDataHierarchy(String baseUrl, User actor) throws IOException {
        return ensureThrottleDataHierarchy(baseUrl, actor, INITIALIZATION_TIMEOUT_MILLIS, () -> { });
    }

    public static boolean ensureThrottleDataHierarchy(String baseUrl, User actor, Runnable diagnostics)
            throws IOException {
        return ensureThrottleDataHierarchy(baseUrl, actor, INITIALIZATION_TIMEOUT_MILLIS, diagnostics);
    }

    public static boolean ensureThrottleDataHierarchy(String baseUrl, User actor, long timeoutMillis,
                                                     Runnable diagnostics) throws IOException {
        return ensureCollectionPath(baseUrl, actor, THROTTLE_DATA_PATH, timeoutMillis, diagnostics);
    }

    /**
     * Creates the Carbon Andes subscription collection before distributed broker consumers connect.
     * The collection is normally created on the first subscription. In distributed APIM, Gateway connects to
     * both CP and TM brokers during startup; those writes can concurrently try to create this shared path.
     */
    public static boolean ensureThrottleDataSubscriptionsCollection(String baseUrl, User actor, long timeoutMillis,
                                                                    Runnable diagnostics) throws IOException {
        return ensureCollectionPath(baseUrl, actor, THROTTLE_DATA_SUBSCRIPTIONS_PATH, timeoutMillis, diagnostics);
    }

    private static boolean ensureCollectionPath(String baseUrl, User actor, String path, long timeoutMillis,
                                                Runnable diagnostics) throws IOException {
        SimpleHTTPClient http = SimpleHTTPClient.getInstance();
        return ensureCollectionPath(new RegistryClient() {
            @Override
            public HttpResponse read(String path, int timeout) throws IOException {
                return http.withRequestDeadline(timeout, () -> http.withoutAutomaticRetries(() ->
                        http.withoutGeneralErrorRetry(() ->
                        http.sendSoapRequest(baseUrl + SERVICE_PATH, getResourceDataEnvelope(path),
                                "urn:getResourceData", actor.getUserName(), actor.getPassword()))));
            }

            @Override
            public HttpResponse create(String parent, String name, int timeout) throws IOException {
                return http.withRequestDeadline(timeout, () -> http.withoutAutomaticRetries(() ->
                        http.withoutGeneralErrorRetry(() ->
                        http.sendSoapRequest(baseUrl + SERVICE_PATH, addCollectionEnvelope(parent, name),
                                "urn:addCollection", actor.getUserName(), actor.getPassword()))));
            }
        }, path, timeoutMillis, diagnostics);
    }

    interface RegistryClient {
        HttpResponse read(String path, int timeoutMillis) throws IOException;
        HttpResponse create(String parent, String name, int timeoutMillis) throws IOException;
    }

    static boolean ensureThrottleDataHierarchy(RegistryClient client, long timeoutMillis, Runnable diagnostics)
            throws IOException {
        return ensureCollectionPath(client, THROTTLE_DATA_PATH, timeoutMillis, diagnostics);
    }

    static boolean ensureThrottleDataSubscriptionsCollection(RegistryClient client, long timeoutMillis,
                                                             Runnable diagnostics) throws IOException {
        return ensureCollectionPath(client, THROTTLE_DATA_SUBSCRIPTIONS_PATH, timeoutMillis, diagnostics);
    }

    private static boolean ensureCollectionPath(RegistryClient client, String path, long timeoutMillis,
                                                Runnable diagnostics) throws IOException {
        Budget budget = new Budget(timeoutMillis, diagnostics);
        String current = "";
        boolean repaired = false;
        for (String segment : path.substring(1).split("/")) {
            current += "/" + segment;
            HttpResponse existing = awaitResource(client, current, budget, false);
            if (existing.getResponseCode() == 200) {
                requireCollection(existing, current);
                continue;
            }
            if (!isMissingCollection(existing, current)) {
                throw new IOException("ResourceAdminService could not inspect " + current + ": " + describe(existing));
            }
            int separator = current.lastIndexOf('/');
            String parent = separator == 0 ? "/" : current.substring(0, separator);
            String name = current.substring(separator + 1);
            int mutationTimeout = budget.requestTimeout(true);
            // An interrupted write has an unknown outcome. Submit once, then reconcile only through reads.
            long mutationStarted = System.nanoTime();
            try {
                HttpResponse created = client.create(parent, name, mutationTimeout);
                if (created == null || created.getResponseCode() != 200) {
                    throw new RejectedCreation("ResourceAdminService.addCollection failed for " + current
                            + ": " + describe(created));
                }
            } catch (RejectedCreation rejected) {
                throw rejected;
            } catch (IOException uncertain) {
                budget.lastFailure = uncertain;
                LOG.warn("Registry creation outcome is unknown for " + current
                        + "; awaiting a readable collection without replaying addCollection", uncertain);
                budget.diagnose();
            } finally {
                if (System.nanoTime() - mutationStarted >= TimeUnit.SECONDS.toNanos(10)) {
                    budget.diagnose();
                }
            }
            requireCollection(awaitResource(client, current, budget, true), current);
            repaired = true;
        }
        return repaired;
    }

    private static HttpResponse awaitResource(RegistryClient client, String path, Budget budget,
                                             boolean awaitCreation) throws IOException {
        try {
            HttpResponse observed = Utils.retryUntilWithInterval(budget.remainingMillis(), 250, () -> {
                if (budget.expired()) {
                    return null;
                }
                HttpResponse response;
                try {
                    response = client.read(path, budget.requestTimeout(false));
                } catch (IOException transientRead) {
                    budget.lastFailure = transientRead;
                    budget.diagnose();
                    throw transientRead;
                }
                if (response == null || response.getResponseCode() == 404
                        || response.getResponseCode() == 502 || response.getResponseCode() == 503) {
                    return null;
                }
                if (awaitCreation) {
                    try {
                        if (isMissingCollection(response, path)) {
                            return null;
                        }
                    } catch (IOException malformedFault) {
                        throw new UncheckedIOException(malformedFault);
                    }
                }
                return response;
            }, response -> response != null);
            if (observed == null) {
                budget.diagnose();
                throw new IOException("Registry initialization deadline exhausted at " + path
                        + (awaitCreation ? "; addCollection was submitted once and its outcome could not be verified"
                        : "; registry service remained unavailable"), budget.lastFailure);
            }
            if (awaitCreation && observed.getResponseCode() != 200) {
                throw new IOException("Registry verification failed for " + path + ": " + describe(observed));
            }
            return observed;
        } catch (UncheckedIOException malformedFault) {
            throw malformedFault.getCause();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while initializing registry collection " + path, interrupted);
        }
    }

    private static final class RejectedCreation extends IOException {
        private RejectedCreation(String message) {
            super(message);
        }
    }

    private static final class Budget {
        private final long deadline;
        private final Runnable diagnostics;
        private boolean diagnosed;
        private IOException lastFailure;

        private Budget(long timeoutMillis, Runnable diagnostics) {
            if (timeoutMillis <= 0 || timeoutMillis > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Registry initialization timeout must be positive and fit an int");
            }
            deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
            this.diagnostics = java.util.Objects.requireNonNull(diagnostics);
        }

        private boolean expired() {
            return deadline - System.nanoTime() <= 0;
        }

        private long remainingMillis() throws IOException {
            long nanos = deadline - System.nanoTime();
            if (nanos <= 0) {
                diagnose();
                throw new IOException("Registry initialization deadline exhausted", lastFailure);
            }
            return Math.max(1, TimeUnit.NANOSECONDS.toMillis(nanos));
        }

        private int requestTimeout(boolean mutation) throws IOException {
            // Reserve time to verify an ambiguous write; reads retain their short, retryable timeout.
            long remaining = remainingMillis();
            return (int) Math.max(1, Math.min(mutation ? 60_000 : 10_000,
                    mutation ? remaining / 2 : remaining));
        }

        private void diagnose() {
            if (diagnosed) {
                return;
            }
            diagnosed = true;
            try {
                diagnostics.run();
            } catch (RuntimeException diagnosticFailure) {
                LOG.warn("Registry diagnostics unavailable; preserving the initialization result", diagnosticFailure);
            }
        }
    }

    private static void requireCollection(HttpResponse response, String path) throws IOException {
        try {
            var types = Utils.getNodeTextsByXPath(response.getData(), "//*[local-name()='resourceType']");
            if (types.size() != 1 || !"collection".equals(types.get(0).trim())) {
                throw new IOException("Registry path " + path + " is not a collection: " + describe(response));
            }
        } catch (org.jaxen.JaxenException | RuntimeException e) {
            throw new IOException("Could not parse registry collection metadata for " + path, e);
        }
    }

    /** Intermediate read: its response is consumed here and must not replace a scenario assertion response. */
    private static HttpResponse getResourceData(String baseUrl, User actor, String path) throws IOException {

        return SimpleHTTPClient.getInstance().withSocketTimeout(10000,
                () -> SimpleHTTPClient.getInstance().sendSoapRequest(baseUrl + SERVICE_PATH,
                        getResourceDataEnvelope(path), "urn:getResourceData", actor.getUserName(), actor.getPassword()));
    }

    /** A storage/authentication fault must never be interpreted as permission to create a missing collection. */
    private static boolean isMissingCollection(HttpResponse response, String path) throws IOException {
        if (response == null || response.getResponseCode() != 500 || response.getData() == null) {
            return false;
        }
        try {
            return Utils.getNodeTextsByXPath(response.getData(), "//*[local-name()='Fault']/*[local-name()='faultstring']")
                    .stream().anyMatch(fault -> fault.trim().equals("Resource does not exist at path " + path));
        } catch (org.jaxen.JaxenException | RuntimeException e) {
            throw new IOException("Could not parse registry collection fault for " + path, e);
        }
    }

    private static String addCollectionEnvelope(String parent, String name) {

        return envelope("<ser:addCollection>"
                + "<ser:parentPath>" + Utils.escapeXml(parent) + "</ser:parentPath>"
                + "<ser:collectionName>" + Utils.escapeXml(name) + "</ser:collectionName>"
                + "<ser:mediaType>" + COLLECTION_MEDIA_TYPE + "</ser:mediaType>"
                + "<ser:description>Integration-v2 throttleData infrastructure</ser:description>"
                + "</ser:addCollection>");
    }

    private static String getResourceDataEnvelope(String path) {

        return envelope("<ser:getResourceData><ser:paths>" + Utils.escapeXml(path)
                + "</ser:paths></ser:getResourceData>");
    }

    private static String envelope(String body) {

        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"" + OPERATION_NAMESPACE + "\"><soapenv:Header/><soapenv:Body>"
                + body + "</soapenv:Body></soapenv:Envelope>";
    }

    private static String describe(HttpResponse response) {

        return response == null ? "null" : response.getResponseCode() + "/" + response.getData();
    }
}
