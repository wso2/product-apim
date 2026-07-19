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

    /**
     * Teardown list for custom AI service providers (LLM providers). Not in the shared {@link Constants}
     * because it is specific to the AI-API glue. An AI provider cannot be deleted while an AIAPI-subtype API
     * still references it (an {@code AM_API_AI_CONFIGURATION} foreign key), so it is swept AFTER the APIs — see
     * the ordering in {@link #deleteRegisteredResources()}.
     */
    public static final String CREATED_AI_PROVIDER_IDS = "createdAiProviderIds";

    /**
     * Teardown list for publisher MCP servers. Not in the shared {@link Constants} because it is specific to the
     * MCP glue. An MCP server built FROM an API references that API (its delete is rejected while the MCP server
     * binds it), so it is swept BEFORE the APIs — see the ordering in {@link #deleteRegisteredResources()}. It is
     * created with the publisher token and so deleted with it.
     */
    public static final String CREATED_MCP_SERVER_IDS = "createdMcpServerIds";

    /**
     * Teardown list for "bring your own" OAuth clients registered via DCR ({@code iRegisterOAuthClient}), holding
     * each client's consumer key. A DCR client is a standalone OAuth service provider that deleting the DevPortal
     * application does NOT remove, and the DCR REST endpoint exposes no delete — so it is deregistered explicitly
     * via the Carbon {@code OAuthAdminService} SOAP admin service (as its owner) or it leaks. Swept by
     * {@link #deleteDcrClients(String)}, not the generic bearer-token {@link #deleteResources}.
     */
    public static final String CREATED_DCR_CLIENT_IDS = "createdDcrClientIds";

    /**
     * Teardown list for endpoint certificates uploaded via the Publisher {@code /endpoint-certificates} REST API,
     * holding each certificate's ALIAS (the delete path segment). An endpoint certificate is tenant-global config
     * (not owned by an API) that nothing else removes, so it must be swept explicitly or it leaks across scenarios.
     * Deleted via the generic bearer-token {@link #deleteResources} with the owner's publisher token.
     */
    public static final String CREATED_ENDPOINT_CERTIFICATE_ALIASES = "createdEndpointCertificateAliases";

    /**
     * Teardown list for Service Catalog entries created via {@code /api/am/service-catalog/v1/services}, holding
     * each service's id. A catalog service is admin-plane tenant config that nothing else removes; a service still
     * referenced by an API 409s on delete, so it is swept AFTER the APIs. Deleted via the generic bearer-token
     * {@link #deleteResources} with the owner's admin token.
     */
    public static final String CREATED_SERVICE_CATALOG_IDS = "createdServiceCatalogIds";

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

    /**
     * Drops a previously-registered id from the teardown sweep — for a resource a test deletes itself mid-scenario
     * (e.g. a common operation policy in an export/delete/import round-trip), so the later sweep does not attempt a
     * spurious delete of an already-gone id and log a misleading 404. Matches on id only. No-op if not present.
     */
    public static void deregister(String listKey, Object id) {
        String target = String.valueOf(id);
        TestContext.getList(listKey).removeIf(entry ->
                entry instanceof OwnedResource && ((OwnedResource) entry).id().equals(target));
    }

    /** Deletes all registered applications then APIs from the context's current scope. No-op if no baseUrl. */
    public static void deleteRegisteredResources() {

        Object baseUrlObj = TestContext.get("baseUrl");
        if (baseUrlObj == null) {
            return;
        }

        // Nothing was registered (e.g. a framework-probe block that boots a container but creates no resources
        // and never sets an acting actor). Skip before resolving any actor-scoped token key — doing so would
        // call Identity.actingActor() and fail with "Tenant not found in context" for an actor-less block.
        if (TestContext.getList(Constants.CREATED_APPLICATION_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_API_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_OPERATION_POLICY_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_SHARED_SCOPE_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_APPLICATION_POLICY_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_SUBSCRIPTION_POLICY_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_ADVANCED_POLICY_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_CUSTOM_POLICY_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_API_PRODUCT_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_ENVIRONMENT_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_GOVERNANCE_POLICY_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_GOVERNANCE_RULESET_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_KEY_MANAGER_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_DENY_POLICY_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_ORGANIZATION_IDS).isEmpty()
                && TestContext.getList(Constants.CREATED_API_CATEGORY_IDS).isEmpty()
                && TestContext.getList(CREATED_AI_PROVIDER_IDS).isEmpty()
                && TestContext.getList(CREATED_MCP_SERVER_IDS).isEmpty()
                && TestContext.getList(CREATED_DCR_CLIENT_IDS).isEmpty()
                && TestContext.getList(CREATED_ENDPOINT_CERTIFICATE_ALIASES).isEmpty()
                && TestContext.getList(CREATED_SERVICE_CATALOG_IDS).isEmpty()) {
            return;
        }
        String baseUrl = baseUrlObj.toString();

        try {
            deleteResources(Constants.CREATED_APPLICATION_IDS, Identity::devportalTokenKey,
                    id -> Utils.getApplicationEndpointURL(baseUrl, id));
            // MCP servers AFTER applications (whose removal clears any MCP subscriptions) but BEFORE the APIs they
            // may be built from — a from-api MCP server references an API, whose delete is rejected while the MCP
            // server still binds it. Created with the publisher token.
            deleteResources(CREATED_MCP_SERVER_IDS, Identity::publisherTokenKey,
                    id -> Utils.getMCPServerByIdURL(baseUrl, id));
            // API Products BEFORE their underlying APIs — a product references APIs, so an API delete is
            // rejected while a product still uses it. Deleted with the publisher token.
            deleteResources(Constants.CREATED_API_PRODUCT_IDS, Identity::publisherTokenKey,
                    id -> Utils.getResourceEndpointURL(baseUrl, "api-products", id));
            deleteResources(Constants.CREATED_API_IDS, Identity::publisherTokenKey,
                    id -> Utils.getResourceEndpointURL(baseUrl, "apis", id));
            // Custom AI service providers AFTER the AIAPI-subtype APIs that reference them — an AI provider
            // delete is rejected with a foreign-key violation (AM_API_AI_CONFIGURATION → AM_LLM_PROVIDER) while
            // any API still binds it. Deleted with the admin token (the provider is an admin-plane resource).
            deleteResources(CREATED_AI_PROVIDER_IDS, Identity::adminTokenKey,
                    id -> Utils.getAIServiceProviderByIdURL(baseUrl, id));
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
            // Gateway environments (admin). No revisions are deployed to test envs, so delete is unblocked.
            deleteResources(Constants.CREATED_ENVIRONMENT_IDS, Identity::adminTokenKey,
                    id -> Utils.getEnvironmentByIdURL(baseUrl, id));
            // Governance policies BEFORE governance rulesets — a policy references rulesets, so a ruleset
            // delete is rejected with 409 while a policy still attaches it. Both deleted with the governance
            // token (apim:gov_*), not the admin token.
            deleteResources(Constants.CREATED_GOVERNANCE_POLICY_IDS, Identity::governanceTokenKey,
                    id -> Utils.getGovernancePolicyByIdURL(baseUrl, id));
            deleteResources(Constants.CREATED_GOVERNANCE_RULESET_IDS, Identity::governanceTokenKey,
                    id -> Utils.getGovernanceRulesetByIdURL(baseUrl, id));
            // Key managers (admin, tenant-scoped config registry). No resource references them at test time, so
            // delete is unblocked.
            deleteResources(Constants.CREATED_KEY_MANAGER_IDS, Identity::adminTokenKey,
                    id -> Utils.getKeyManagerByIdURL(baseUrl, id));
            // Deny (blocking-condition) policies (admin, tenant-global). Deleted via the singular by-id path.
            deleteResources(Constants.CREATED_DENY_POLICY_IDS, Identity::adminTokenKey,
                    id -> Utils.getDenyPolicyByIdURL(baseUrl, id));
            // B2B organizations (admin) AFTER the APIs — an API restricted via visibleOrganizations references
            // the org, so an org delete would be rejected while such an API still exists. The APIs are swept
            // above, so this is unblocked. The org-visibility feature also deletes these inline on its happy
            // path; this sweep is the failure-safe backstop and idempotently 404s on the already-deleted ones.
            deleteResources(Constants.CREATED_ORGANIZATION_IDS, Identity::adminTokenKey,
                    id -> Utils.getOrganizationByIdURL(baseUrl, id));
            // API categories (admin, tenant-global) AFTER the APIs — deleting a category that an API still
            // references is not FK-blocked (APIM detaches it, returns 200), but sweeping after the APIs keeps the
            // ordering consistent. The api-categories feature also deletes its category inline; this is the
            // failure-safe backstop and idempotently 404s on the already-deleted ones.
            deleteResources(Constants.CREATED_API_CATEGORY_IDS, Identity::adminTokenKey,
                    id -> Utils.getApiCategoryByIdURL(baseUrl, id));
            // Endpoint certificates (publisher, tenant-global) AFTER the APIs — deleting a certificate an API's
            // endpoint still references is not FK-blocked, but sweeping after the APIs keeps ordering consistent.
            // Deleted by alias with the owner's publisher token.
            deleteResources(CREATED_ENDPOINT_CERTIFICATE_ALIASES, Identity::publisherTokenKey,
                    alias -> Utils.getEndpointCertificateByAliasURL(baseUrl, alias));
            // Service Catalog entries (admin) AFTER the APIs — a service still referenced by an API 409s on
            // delete, so the APIs (swept above) must go first. Deleted by id with the owner's admin token.
            deleteResources(CREATED_SERVICE_CATALOG_IDS, Identity::adminTokenKey,
                    id -> Utils.getServiceCatalogByIdURL(baseUrl, id));
            // BYO OAuth clients registered via DCR: swept separately because they authenticate with the owner's
            // Basic credentials (not a bearer token) and are not removed by the DevPortal application's deletion.
            deleteDcrClients(baseUrl);
        } finally {
            TestContext.remove(Constants.CREATED_API_PRODUCT_IDS);
            TestContext.remove(Constants.CREATED_API_IDS);
            TestContext.remove(Constants.CREATED_APPLICATION_IDS);
            TestContext.remove(Constants.CREATED_SHARED_SCOPE_IDS);
            TestContext.remove(Constants.CREATED_OPERATION_POLICY_IDS);
            TestContext.remove(Constants.CREATED_APPLICATION_POLICY_IDS);
            TestContext.remove(Constants.CREATED_SUBSCRIPTION_POLICY_IDS);
            TestContext.remove(Constants.CREATED_ADVANCED_POLICY_IDS);
            TestContext.remove(Constants.CREATED_CUSTOM_POLICY_IDS);
            TestContext.remove(Constants.CREATED_ENVIRONMENT_IDS);
            TestContext.remove(Constants.CREATED_GOVERNANCE_POLICY_IDS);
            TestContext.remove(Constants.CREATED_GOVERNANCE_RULESET_IDS);
            TestContext.remove(Constants.CREATED_KEY_MANAGER_IDS);
            TestContext.remove(Constants.CREATED_DENY_POLICY_IDS);
            TestContext.remove(Constants.CREATED_ORGANIZATION_IDS);
            TestContext.remove(Constants.CREATED_API_CATEGORY_IDS);
            TestContext.remove(CREATED_AI_PROVIDER_IDS);
            TestContext.remove(CREATED_MCP_SERVER_IDS);
            TestContext.remove(CREATED_DCR_CLIENT_IDS);
            TestContext.remove(CREATED_ENDPOINT_CERTIFICATE_ALIASES);
            TestContext.remove(CREATED_SERVICE_CATALOG_IDS);
        }
    }

    /**
     * Deregisters DCR-registered ("bring your own") OAuth clients. These are NOT deleted with a bearer token:
     * the DCR endpoint authenticates with the OWNER's Basic credentials (the same way {@code iRegisterOAuthClient}
     * created them), and a DCR client is a standalone service provider that the DevPortal application's deletion
     * does not remove — so it must be swept explicitly or it leaks. Best-effort: a 404 (already gone) is fine.
     */
    private static void deleteDcrClients(String baseUrl) {
        String serviceUrl = baseUrl + "services/OAuthAdminService";
        // Axis2 wraps operation elements in this namespace (same as other Carbon admin-service SOAP calls).
        String ns = "http://org.apache.axis2/xsd";
        for (Object o : TestContext.getList(CREATED_DCR_CLIENT_IDS)) {
            if (!(o instanceof OwnedResource res) || res.id() == null) {
                continue;
            }
            User owner = Identity.resolveActor(res.actorRef());
            String removeEnvelope = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                    + "xmlns:xsd=\"" + ns + "\"><soapenv:Header/><soapenv:Body>"
                    + "<xsd:removeOAuthApplicationData><xsd:consumerKey>" + res.id() + "</xsd:consumerKey>"
                    + "</xsd:removeOAuthApplicationData></soapenv:Body></soapenv:Envelope>";
            try {
                HttpResponse resp = SimpleHTTPClient.getInstance().sendSoapRequest(serviceUrl, removeEnvelope,
                        "urn:removeOAuthApplicationData", owner.getUserName(), owner.getPassword());
                int code = resp.getResponseCode();
                if (code >= 200 && code < 300) {
                    logger.info("Cleanup: deregistered DCR client " + res.id() + " (HTTP " + code + ").");
                } else {
                    logger.warn("Cleanup: deregister of DCR client " + res.id() + " returned HTTP " + code
                            + " — it may leak: " + trunc(resp.getData()));
                }
            } catch (Exception e) {
                logger.warn("Cleanup failed to deregister DCR client " + res.id() + ": " + e.getMessage());
            }
        }
    }

    private static String trunc(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 300 ? s.substring(0, 300) : s;
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
