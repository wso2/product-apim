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
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Best-effort, idempotent teardown of the APIs and applications (and operation policies / shared scopes) a
 * test registered via {@link #register} under {@link Constants#CREATED_API_IDS} /
 * {@link Constants#CREATED_APPLICATION_IDS} (etc.). Each id is registered as an {@link OwnedResource} tagged
 * with the actor that created it, so teardown deletes every resource as its OWNING actor's token — not as
 * whoever happens to be acting when cleanup runs. This matters because deleting a resource with the wrong
 * principal (a different actor/tenant) is denied and would silently leak it onto the shared container.
 * Shared by two callers driving different teardown granularities:
 *
 * <ul>
 *   <li>the {@code @After("@cleanup")} hook — per-scenario teardown, for self-contained single-scenario
 *       features that create and tear down their own resources; and</li>
 *   <li>the per-runner {@code @AfterClass} sweep on {@code BaseBlockRunner} — for the {@code _setup_*}
 *       fixture pattern, where a setup feature creates resources that several later scenarios in the SAME
 *       runner consume, so teardown must run once at the end of the runner rather than after each scenario
 *       (a per-scenario hook would delete the shared fixtures out from under the scenarios that follow).</li>
 * </ul>
 *
 * Applications are deleted before APIs because removing an application also removes its subscriptions, which
 * would otherwise block deletion of a subscribed API with a 409. Deletes are best-effort: a resource the
 * scenario already deleted itself simply 404s and is ignored. The id lists are cleared afterwards so ids do
 * not leak into a later scenario/runner reusing the same scope.
 */
public final class ResourceCleanup {

    private static final Log logger = LogFactory.getLog(ResourceCleanup.class);

    private ResourceCleanup() {
    }

    /**
     * A created resource tagged with the reference of the actor that created it, so teardown can delete it as
     * that principal. {@code actorRef} is {@code null} for the super-tenant admin default.
     */
    public record OwnedResource(String id, String actorRef) {
    }

    /**
     * Registers a created resource for teardown under the given {@code CREATED_*} list, tagged with the actor
     * currently acting (so cleanup deletes it as its owner). Use this instead of
     * {@code TestContext.addToList(...)} for anything that must be torn down.
     */
    public static void register(String listKey, Object id) {
        TestContext.addToList(listKey, new OwnedResource(String.valueOf(id), Identity.actingActorRef()));
    }

    /** Deletes all registered applications then APIs from the context's current scope. No-op if no baseUrl. */
    public static void deleteRegisteredResources() {

        Object baseUrlObj = TestContext.get("baseUrl");
        if (baseUrlObj == null) {
            return;
        }

        // Nothing was registered (e.g. a framework-probe block that boots a container but creates no resources
        // and never sets an acting actor). Skip before resolving any actor-scoped token key — doing so would
        // call Identity.defaultActor() and fail with "Tenant not found in context" for an actor-less block.
        if (TestContext.getList(Constants.CREATED_APPLICATION_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_API_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_OPERATION_POLICY_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_SHARED_SCOPE_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_APPLICATION_POLICY_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_SUBSCRIPTION_POLICY_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_ADVANCED_POLICY_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_CUSTOM_POLICY_IDS).isEmpty()) {
            return;
        }
        String baseUrl = baseUrlObj.toString();

        try {
            deleteResources(Constants.CREATED_APPLICATION_IDS, Identity::devportalTokenKey,
                    id -> Utils.getApplicationEndpointURL(baseUrl, id));
            deleteResources(Constants.CREATED_API_IDS, Identity::publisherTokenKey,
                    id -> Utils.getResourceEndpointURL(baseUrl, "apis", id));
            // Common (reusable) operation policies are tenant-global, so they outlive their API and must be
            // swept explicitly (API-specific policies are removed when their API is deleted above).
            deleteResources(Constants.CREATED_OPERATION_POLICY_IDS, Identity::publisherTokenKey,
                    id -> Utils.getResourceEndpointURL(baseUrl, "operation-policies", id));
            // Shared scopes last: an API that references a scope must be deleted before the scope itself.
            deleteResources(Constants.CREATED_SHARED_SCOPE_IDS, Identity::publisherTokenKey,
                    id -> Utils.getAPIScopesById(baseUrl, id));
            // Application throttling policies (admin, tenant-global) after the applications that reference
            // them are gone — else the policy delete is rejected as in-use. Deleted with the admin token.
            deleteResources(Constants.CREATED_APPLICATION_POLICY_IDS, Identity::adminTokenKey,
                    id -> Utils.getApplicationThrottlingPolicyByIdURL(baseUrl, id));
            // Subscription throttling policies (admin, tenant-global) likewise after the subscriptions that
            // reference them are gone (a subscription is removed when its application is deleted above).
            deleteResources(Constants.CREATED_SUBSCRIPTION_POLICY_IDS, Identity::adminTokenKey,
                    id -> Utils.getSubscriptionThrottlingPolicyByIdURL(baseUrl, id));
            // Advanced (API-level) throttling policies (admin, tenant-global) after the APIs that reference them
            // via apiThrottlingPolicy are gone — else the policy delete is rejected as in-use.
            deleteResources(Constants.CREATED_ADVANCED_POLICY_IDS, Identity::adminTokenKey,
                    id -> Utils.getAdvancedThrottlingPolicyByIdURL(baseUrl, id));
            // Custom (Siddhi) throttling rules (admin, global). No resource references them, but they are
            // global rules that would keep matching traffic, so delete them explicitly.
            deleteResources(Constants.CREATED_CUSTOM_POLICY_IDS, Identity::adminTokenKey,
                    id -> Utils.getCustomThrottlingPolicyByIdURL(baseUrl, id));
        } finally {
            TestContext.remove(Constants.CREATED_API_IDS);
            TestContext.remove(Constants.CREATED_APPLICATION_IDS);
            TestContext.remove(Constants.CREATED_SHARED_SCOPE_IDS);
            TestContext.remove(Constants.CREATED_OPERATION_POLICY_IDS);
            TestContext.remove(Constants.CREATED_APPLICATION_POLICY_IDS);
            TestContext.remove(Constants.CREATED_SUBSCRIPTION_POLICY_IDS);
            TestContext.remove(Constants.CREATED_ADVANCED_POLICY_IDS);
            TestContext.remove(Constants.CREATED_CUSTOM_POLICY_IDS);
        }
    }

    /**
     * Deletes every resource registered under {@code contextKey}, each as the actor that CREATED it — not
     * whoever happens to be acting when teardown runs. Deleting with the wrong principal is denied (403) for
     * a resource owned by another actor/tenant, which would silently leak it; resolving the owner per resource
     * (via the recorded {@link OwnedResource#actorRef()}) avoids that. {@code tokenKeyFor} selects the token
     * TYPE for this resource kind (devportal for applications, publisher for APIs/policies/scopes).
     */
    private static void deleteResources(String contextKey, Function<User, String> tokenKeyFor,
                                        Function<String, String> urlBuilder) {

        for (Object o : TestContext.getList(contextKey)) {
            if (!(o instanceof OwnedResource res) || res.id() == null) {
                continue;
            }
            User owner = Identity.resolveActor(res.actorRef());
            Object tokenObj = TestContext.get(tokenKeyFor.apply(owner));
            if (tokenObj == null) {
                logger.warn("Cleanup: no token for the owner (actor '" + res.actorRef() + "') of " + contextKey
                        + " " + res.id() + "; cannot delete — it may leak.");
                continue;
            }
            Map<String, String> headers = new HashMap<>();
            headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + tokenObj);
            try {
                HttpResponse resp = SimpleHTTPClient.getInstance().doDelete(urlBuilder.apply(res.id()), headers);
                int code = resp.getResponseCode();
                if (code >= 200 && code < 300) {
                    continue;   // deleted
                }
                if (code == 404) {
                    // Best-effort: resource already deleted by the scenario itself. Logged so a leak masked
                    // as 404 stays reviewable.
                    logger.info("Cleanup: " + contextKey + " " + res.id() + " returned 404 — assumed already "
                            + "deleted.");
                    continue;
                }
                // Neither deleted nor absent: the delete did NOT happen, so the resource leaks onto the shared
                // container. With per-owner tokens this should not be a permission denial — surface it loudly.
                logger.warn("Cleanup: delete of " + contextKey + " " + res.id() + " returned HTTP " + code
                        + " — resource NOT deleted; it may leak: " + resp.getData());
            } catch (Exception e) {
                // Transient/connectivity failure during teardown.
                logger.warn("Cleanup failed to delete resource " + res.id() + " (" + contextKey + "): "
                        + e.getMessage());
            }
        }
    }
}
