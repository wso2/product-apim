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
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Failure-safe teardown of WebSub hub registrations, which are not cascade-deleted with their API or app. */
public final class WebSubSubscriptionCleanup {

    private static final Log logger = LogFactory.getLog(WebSubSubscriptionCleanup.class);

    public static final String REGISTRATIONS = "createdWebSubSubscriptions";

    private static final String INTERNAL_LIST_PATH = "internal/data/v1/webhooks-subscriptions";
    private static final String TENANT_HEADER = "xWSO2Tenant";

    private WebSubSubscriptionCleanup() {
    }

    /** The exact hub request identity and credentials needed to remove an accepted registration later. */
    public record Registration(String tenantDomain, String actorRef, String accessToken, String apiId, String apiContext,
                               String callback, String topic, String secret, String leaseSeconds,
                               boolean queryParameters, int successStatus) {

        public String identity() {
            return tenantDomain + "|" + apiId + "|" + callback + "|" + topic;
        }
    }

    public static void register(String tenantDomain, String actorRef, String accessToken, String apiId, String apiContext,
                                String callback, String topic, String secret, String leaseSeconds,
                                boolean queryParameters, int successStatus) {
        Registration registration = new Registration(tenantDomain, actorRef, accessToken, apiId, apiContext,
                callback, topic, secret, leaseSeconds, queryParameters, successStatus);
        List<Object> registrations = TestContext.getList(REGISTRATIONS);
        if (registrations.stream().noneMatch(item -> item instanceof Registration existing
                && existing.identity().equals(registration.identity()))) {
            TestContext.addToList(REGISTRATIONS, registration);
        }
    }

    /** An explicit successful hub unsubscribe removes the same registration from the fallback sweep. */
    public static void deregister(String tenantDomain, String apiId, String callback, String topic) {
        String identity = tenantDomain + "|" + apiId + "|" + callback + "|" + topic;
        TestContext.getList(REGISTRATIONS).removeIf(item -> item instanceof Registration registration
                && registration.identity().equals(identity));
    }

    /** Unsubscribes only registrations created in this cleanup scope and verifies their persisted rows are gone. */
    public static void sweep() {
        List<Registration> registrations = TestContext.getList(REGISTRATIONS).stream()
                .filter(Registration.class::isInstance).map(Registration.class::cast).toList();
        if (registrations.isEmpty()) {
            return;
        }

        Map<String, JSONArray> rowsBeforeByTenant = new HashMap<>();
        Map<String, Set<String>> ownedApiIdsByTenant = new HashMap<>();
        Set<String> successfulIdentities = new HashSet<>();
        Set<String> tenantsWithoutSnapshot = new HashSet<>();
        for (Registration registration : registrations) {
            ownedApiIdsByTenant.computeIfAbsent(registration.tenantDomain(), ignored -> new HashSet<>())
                    .add(registration.apiId());
            if (rowsBeforeByTenant.containsKey(registration.tenantDomain())
                    || tenantsWithoutSnapshot.contains(registration.tenantDomain())) {
                continue;
            }
            try {
                rowsBeforeByTenant.put(registration.tenantDomain(), readRows(registration.tenantDomain(),
                        registration.actorRef()));
            } catch (IOException failure) {
                tenantsWithoutSnapshot.add(registration.tenantDomain());
                logger.warn("Cleanup: could not snapshot WebSub rows for tenant " + registration.tenantDomain()
                        + "; will still attempt each owned unsubscribe and verify the owned API rows afterward.",
                        failure);
            }
        }

        for (Registration registration : registrations) {
            try {
                HttpResponse response = unsubscribe(registration);
                if (response == null || response.getResponseCode() != registration.successStatus()) {
                    logger.warn("Cleanup: WebSub unsubscribe for tenant " + registration.tenantDomain() + " API "
                            + registration.apiId() + " callback " + registration.callback() + " returned "
                            + (response == null ? "no response" : response.getResponseCode() + "/"
                            + response.getData()) + "; registration may have leaked.");
                    continue;
                }

                Integer remaining = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                        () -> matchingRows(readRows(registration.tenantDomain(), registration.actorRef()),
                                registration).size(), count -> count == 0);
                if (remaining == null || remaining != 0) {
                    logger.warn("Cleanup: WebSub unsubscribe did not remove the persisted registration for tenant "
                            + registration.tenantDomain() + " API " + registration.apiId() + " callback "
                            + registration.callback() + "; remaining matching rows=" + remaining
                            + "; registration may have leaked.");
                    continue;
                }
                successfulIdentities.add(registration.identity());
                logger.info("Cleanup: removed and verified WebSub registration for tenant "
                        + registration.tenantDomain() + " API " + registration.apiId() + " callback "
                        + registration.callback() + " topic " + registration.topic());
            } catch (Exception | AssertionError failure) {
                logger.warn("Cleanup: failed to remove WebSub registration for tenant "
                        + registration.tenantDomain() + " API context " + registration.apiContext() + " callback "
                        + registration.callback() + "; registration may have leaked.", failure);
            }
        }

        verifyOwnedApiRows(registrations, rowsBeforeByTenant, tenantsWithoutSnapshot, ownedApiIdsByTenant,
                successfulIdentities);
        TestContext.getList(REGISTRATIONS).removeIf(item -> item instanceof Registration registration
                && successfulIdentities.contains(registration.identity()));
    }

    private static HttpResponse unsubscribe(Registration registration) throws IOException {
        String params = parameters("unsubscribe", registration.callback(), registration.topic(),
                registration.secret(), registration.leaseSeconds());
        String gatewayUrl = Utils.getBaseGatewayUrl() + (registration.apiContext().startsWith("/") ? "" : "/")
                + registration.apiContext();
        Map<String, String> headers = Identity.bearerHeaders(registration.accessToken());
        if (registration.queryParameters()) {
            String url = gatewayUrl + "?" + params;
            return SimpleHTTPClient.getInstance().withSocketTimeout(30_000,
                    () -> SimpleHTTPClient.getInstance().withoutAutomaticRetries(
                            () -> SimpleHTTPClient.getInstance().withoutGeneralErrorRetry(
                                    () -> SimpleHTTPClient.getInstance().doPost(url, headers, "",
                                            Constants.CONTENT_TYPES.APPLICATION_JSON))));
        }
        String url = gatewayUrl;
        return SimpleHTTPClient.getInstance().withSocketTimeout(30_000,
                () -> SimpleHTTPClient.getInstance().withoutAutomaticRetries(
                        () -> SimpleHTTPClient.getInstance().withoutGeneralErrorRetry(
                                () -> SimpleHTTPClient.getInstance().doPost(url, headers, params,
                                        Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED))));
    }

    private static String parameters(String mode, String callback, String topic, String secret, String leaseSeconds) {
        StringBuilder params = new StringBuilder("hub.callback=").append(encode(callback))
                .append("&hub.mode=").append(encode(mode))
                .append("&hub.secret=").append(encode(secret))
                .append("&hub.topic=").append(encode(topic));
        if (leaseSeconds != null && !leaseSeconds.isBlank()) {
            params.append("&hub.lease_seconds=").append(encode(leaseSeconds));
        }
        return params.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static JSONArray readRows(String tenantDomain, String actorRef) throws IOException {
        User actor = Identity.resolveActor(actorRef);
        Map<String, String> headers = new HashMap<>(Identity.basicAuthHeaders(actor.getUserName(), actor.getPassword()));
        headers.put(TENANT_HEADER, tenantDomain);
        String url = Utils.getBaseUrl() + INTERNAL_LIST_PATH;
        HttpResponse response = SimpleHTTPClient.getInstance().doGet(url, headers);
        if (response == null || response.getResponseCode() != 200 || response.getData() == null
                || response.getData().isBlank()) {
            throw new IOException("Could not read persisted WebSub registrations for tenant " + tenantDomain
                    + ": " + (response == null ? "no response" : response.getResponseCode() + "/"
                    + response.getData()));
        }
        JSONArray rows = new JSONObject(response.getData()).optJSONArray("list");
        return rows == null ? new JSONArray() : rows;
    }

    private static List<JSONObject> matchingRows(JSONArray rows, Registration registration) {
        List<JSONObject> matches = new ArrayList<>();
        for (int index = 0; index < rows.length(); index++) {
            JSONObject row = rows.getJSONObject(index);
            if (registration.apiId().equals(row.optString("apiUUID", null))
                    && registration.apiContext().equals(row.optString("apiContext", null))
                    && registration.callback().equals(row.optString("callbackURL", null))
                    && registration.topic().equals(row.optString("topicName", null))) {
                matches.add(row);
            }
        }
        return matches;
    }

    private static void verifyOwnedApiRows(List<Registration> registrations,
                                          Map<String, JSONArray> rowsBeforeByTenant,
                                          Set<String> tenantsWithoutSnapshot,
                                          Map<String, Set<String>> ownedApiIdsByTenant,
                                          Set<String> successfulIdentities) {
        for (Map.Entry<String, Set<String>> tenantEntry : ownedApiIdsByTenant.entrySet()) {
            String tenant = tenantEntry.getKey();
            try {
                JSONArray after = readRows(tenant, registrations.stream()
                        .filter(registration -> tenant.equals(registration.tenantDomain()))
                        .map(Registration::actorRef).findFirst().orElse(null));
                for (String apiId : tenantEntry.getValue()) {
                    Set<String> beforeApiRows = new HashSet<>();
                    JSONArray before = rowsBeforeByTenant.get(tenant);
                    if (before != null) {
                        beforeApiRows.addAll(rowIdentities(before, apiId));
                    }
                    Set<String> ownedRows = new HashSet<>();
                    for (Registration registration : registrations) {
                        if (!tenant.equals(registration.tenantDomain())
                                || !successfulIdentities.contains(registration.identity())) {
                            continue;
                        }
                        if (before != null) {
                            for (JSONObject row : matchingRows(before, registration)) {
                                ownedRows.add(rowIdentity(row));
                            }
                        }
                    }
                    beforeApiRows.removeAll(ownedRows);
                    Set<String> afterApiRows = rowIdentities(after, apiId);
                    if (tenantsWithoutSnapshot.contains(tenant)) {
                        if (!afterApiRows.isEmpty()) {
                            logger.warn("Cleanup: could not establish an initial WebSub row snapshot; owned API "
                                    + apiId + " in tenant " + tenant + " still has " + afterApiRows.size()
                                    + " persisted row(s) after unsubscribe.");
                            continue;
                        }
                        logger.info("Cleanup: verified owned API " + apiId + " in tenant " + tenant
                                + " has zero persisted WebSub rows after unsubscribe (initial snapshot unavailable).");
                        continue;
                    }
                    if (!beforeApiRows.equals(afterApiRows)) {
                        logger.warn("Cleanup: WebSub rows for owned API " + apiId + " in tenant " + tenant
                                + " differ from the exact pre-existing rows after unsubscribe; expected "
                                + beforeApiRows + " but found " + afterApiRows + ".");
                        continue;
                    }
                    logger.info("Cleanup: verified API " + apiId + " in tenant " + tenant + " has "
                            + afterApiRows.size() + " persisted WebSub row(s) after removing owned registrations"
                            + (afterApiRows.isEmpty() ? " (zero rows)." : "."));
                }
            } catch (Exception | AssertionError failure) {
                logger.warn("Cleanup: could not verify persisted WebSub rows for owned APIs in tenant " + tenant,
                        failure);
            }
        }
    }

    private static Set<String> rowIdentities(JSONArray rows, String apiId) {
        Set<String> identities = new HashSet<>();
        for (int index = 0; index < rows.length(); index++) {
            JSONObject row = rows.getJSONObject(index);
            if (apiId.equals(row.optString("apiUUID", null))) {
                identities.add(rowIdentity(row));
            }
        }
        return identities;
    }

    private static String rowIdentity(JSONObject row) {
        return row.optString("apiUUID", "") + "|" + row.optString("apiContext", "") + "|"
                + row.optString("callbackURL", "") + "|" + row.optString("topicName", "");
    }

}
