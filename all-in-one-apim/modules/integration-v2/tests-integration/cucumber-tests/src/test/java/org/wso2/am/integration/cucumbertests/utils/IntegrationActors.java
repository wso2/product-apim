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

import org.wso2.am.integration.test.utils.Constants;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of INTEGRATION ACTORS — the principals the tests use to operate on EXTERNAL systems an APIM
 * deployment integrates with (an external Identity Server, and in future any third-party/SaaS integration).
 * The external-system counterpart of {@link Identity}, deliberately SEPARATE from it: an APIM actor is a
 * carbon user in a tenant with publisher/devportal/admin tokens and acting-actor semantics; an integration
 * actor is just {@code (system, credentials, base URL)} for that system's own management plane, addressed
 * explicitly by the steps that operate on it. Never fold one into the other — {@code Identity.actingActor()}
 * must stay APIM-only ({@code ResourceCleanup}'s APIM sweeps and {@code actingTenantAdmin()} depend on it).
 *
 * <p>Per CLAUDE.md §14: REGISTERING an integration actor is actor-registry seeding (provisioner-legitimate —
 * the {@code BlockLifecycleListener} seeds {@link #IS} after booting the Identity Server; a SaaS integration
 * would seed from env/config), but every OPERATION against the system is performed by a feature step
 * authenticating via this registry. Resources those steps create are registered with the system's cleanup
 * (e.g. {@link ISResourceCleanup}) so they are swept as this principal — which stops being cosmetic the
 * moment an integration is a SaaS rather than an ephemeral container.
 *
 * <p>Actors live in the block's SHARED {@link TestContext} scope (keyed {@code integrationActor:<system>}),
 * so registration is per-block-isolated exactly like {@code baseUrl}/{@code isBaseUrl} and dies with the
 * block scope.
 */
public final class IntegrationActors {

    /** System id of the external WSO2 Identity Server integration (the {@code wso2is} container). */
    public static final String IS = "is";

    private static final String KEY_PREFIX = "integrationActor:";

    private IntegrationActors() {
    }

    /** An external system's principal: its management base URL and basic-auth credentials. */
    public record IntegrationActor(String systemId, String username, String password, String baseUrl) {

        /** {@code Authorization: Basic …} header value for this principal. */
        public String basicAuthValue() {
            return "Basic " + Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Seeds (or replaces) the given system's integration actor in the block's shared scope. */
    public static void register(IntegrationActor actor) {
        TestContext.setShared(KEY_PREFIX + actor.systemId(), actor);
    }

    /**
     * Resolves the given system's integration actor, failing fast with a remediation message if the system's
     * infrastructure was not provisioned for this block.
     */
    public static IntegrationActor resolve(String systemId) {
        Object actor = TestContext.get(KEY_PREFIX + systemId);
        if (actor == null) {
            throw new IllegalStateException("No integration actor registered for system '" + systemId + "'. "
                    + (IS.equals(systemId)
                        ? "The block must set bootExternalIdentityServer=true so the external Identity Server "
                            + "is started and its actor seeded."
                        : "Provision the system's infrastructure so its actor is seeded."));
        }
        return (IntegrationActor) actor;
    }

    /** True if the given system's actor is registered in the current block scope. */
    public static boolean isRegistered(String systemId) {
        return TestContext.contains(KEY_PREFIX + systemId);
    }

    /** The system's management base URL (trailing slash included, e.g. {@code https://localhost:32771/}). */
    public static String baseUrl(String systemId) {
        return resolve(systemId).baseUrl();
    }

    /** A headers map carrying the system principal's {@code Authorization: Basic …} header. */
    public static Map<String, String> authHeaders(String systemId) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, resolve(systemId).basicAuthValue());
        return headers;
    }
}
