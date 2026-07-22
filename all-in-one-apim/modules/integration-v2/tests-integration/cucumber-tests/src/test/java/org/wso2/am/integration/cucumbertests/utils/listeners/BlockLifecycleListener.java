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

package org.wso2.am.integration.cucumbertests.utils.listeners;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.xml.XmlTest;
import org.wso2.am.integration.cucumbertests.utils.CoverageSupport;
import org.wso2.am.integration.cucumbertests.utils.ModulePathResolver;
import org.wso2.am.integration.cucumbertests.utils.ServerReadiness;
import org.wso2.am.integration.cucumbertests.utils.SecondaryUserStoreProvisioner;
import org.wso2.am.integration.cucumbertests.utils.TenantUserProvisioner;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.testcontainers.DynamicApimContainer;
import org.wso2.am.testcontainers.IdentityServerContainer;
import org.wso2.am.testcontainers.JacocoCoverage;
import org.wso2.am.testcontainers.NodeAppServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Semaphore;

/**
 * Per-block lifecycle for the parallel-on-shared-container lane. Fires once per TestNG {@code <test>}
 * block: {@code onStart} boots a single {@link DynamicApimContainer} for the block, gates on readiness,
 * and publishes the container plus its base/gateway URLs into the block's shared scope so every class in
 * the block sees one ready server; {@code onFinish} stops that container and clears the scope.
 *
 * <p>If boot or readiness fails it records the cause as the {@code bootError} attribute (consumed by
 * {@code BaseBlockRunner}'s guard) instead of throwing, so the block's classes are reported FAILED with a
 * single root cause rather than failing with an NPE cascade from the absent container. The build stays red
 * — a boot failure must never be reported as a skip, which would leave the run green.
 *
 * <p>Registered only in the new-lane verification suite; the legacy testng.xml is untouched.
 */
public class BlockLifecycleListener implements ITestListener {

    private static final Log logger = LogFactory.getLog(BlockLifecycleListener.class);

    /** Must match {@code BaseBlockRunner.BOOT_ERROR_ATTRIBUTE}. */
    static final String BOOT_ERROR_ATTRIBUTE = "bootError";

    static final String CONTAINER_KEY = "blockApimContainer";
    static final String BASE_URL_KEY = "baseUrl";
    static final String BASE_GATEWAY_URL_KEY = "baseGatewayUrl";
    static final String BASE_GATEWAY_WS_URL_KEY = "baseGatewayWsUrl";
    static final String BASE_GATEWAY_WSS_URL_KEY = "baseGatewayWssUrl";
    static final String GATEWAY_CLIENT_IP_KEY = "gatewayClientIp";

    /** Optional {@code <parameter>} names read from the block's {@code <test>}. */
    static final String PARAM_BLOCK_LABEL = "blockLabel";
    static final String PARAM_TOML_OVERLAY = "tomlOverlayPath";
    /**
     * Optional path to a small feature-specific TOML overlay merged on top of the default {@code basic}
     * overlay (which is itself merged onto the product distribution config). Use this — not the full-file
     * {@code tomlOverlayPath} — when a block only needs a few extra keys (e.g. a custom auth header or
     * application sharing) so it still inherits the distribution + basic defaults.
     */
    static final String PARAM_TOML_EXTRA_OVERLAY = "tomlExtraOverlayPath";
    /** When {@code true}, onStart provisions tenants/users into the block's own container after readiness. */
    static final String PARAM_INIT_TENANT_USERS = "initTenantUsers";
    /** Selects which tenant/user set to provision: {@code default} (the else branch) or {@code adpsample}. */
    static final String PARAM_TENANT_SET = "tenantSet";
    static final String TENANT_SET_ADPSAMPLE = "adpsample";
    /**
     * When {@code true}, onStart ensures the shared NodeAppServer backend (network alias {@code nodebackend})
     * is running before APIM boots, so gateway-invocation tests have a reachable backend for deployed APIs.
     */
    static final String PARAM_INIT_BACKEND = "initBackend";
    /**
     * Optional comma-separated list of {@code <hostPath>::<serverRelativePath>} pairs copied into the block's
     * server directory tree BEFORE boot (host paths relative to the module working dir). For fixtures the
     * server only reads at startup — e.g. a secondary user-store XML under
     * {@code repository/deployment/server/userstores/}: Carbon's User Store Configuration Deployer processes
     * that directory at boot. (NOTE: a JDBC secondary user store CAN be added at runtime via
     * UserStoreConfigAdminService — it hot-deploys asynchronously — which is what {@link #PARAM_INIT_SECONDARY_USER_STORE}
     * uses; serverFilesToCopy remains for genuinely boot-only fixtures.)
     */
    static final String PARAM_SERVER_FILES_TO_COPY = "serverFilesToCopy";

    /**
     * When {@code true}, onStart stands up a JDBC {@code SECONDARY.COM} user store at runtime (schema via the
     * product's own dbscripts + addUserStore SOAP + poll-until-active) after tenant provisioning — the framework
     * facility that replaces the seeded {@code .mv.db} fixture. See {@link SecondaryUserStoreProvisioner}.
     */
    static final String PARAM_INIT_SECONDARY_USER_STORE = "initSecondaryUserStore";
    /**
     * When {@code true}, provisions the external-Identity-Server INFRASTRUCTURE for this block: APIM's
     * client-truststore is augmented with the IS TLS cert BEFORE APIM boots (via
     * {@link DynamicApimContainer#withExternalKmTrust}), so APIM trusts {@code https://wso2is:9443}; and after
     * APIM is ready and tenants/users are provisioned, the {@code IdentityServerContainer} is started, its OIDC
     * discovery is awaited, and the host-mapped IS base URL is published to the block's shared scope under
     * {@link #IS_BASE_URL_KEY}. This is deliberately infrastructure ONLY — registering IS as a key manager is
     * ADMIN PRODUCT BEHAVIOUR and is done by the features themselves ({@code I create a key manager from
     * payload …}, typically in a {@code _setup_*} fixture or inline where registration is the subject). The IS
     * toml can be extended per block via {@link #PARAM_IS_TOML_EXTRA_OVERLAY}.
     */
    static final String PARAM_BOOT_EXTERNAL_IS = "bootExternalIdentityServer";
    /**
     * When {@code true}, this block's APIM binds the fixed {@code wso2am} shared-network alias and becomes the
     * receiver of the external IS's reverse-channel notifications (token-revocation / tenant-sync POSTs to
     * {@code https://wso2am:9443/internal/data/v1/notify}). The alias is fixed — baked into the IS toml and the
     * wso2am.p12 cert — so at most one LIVE container may hold it (duplicate holders make Docker DNS route
     * notifications to an arbitrary APIM). The listener enforces this with a JVM-wide permit
     * ({@link #IS_NOTIFY_ALIAS_PERMIT}): holder blocks queue for the permit before booting and release it after
     * their container stops, serializing ONLY among themselves — alias-free external-KM blocks (which make
     * APIM→IS calls only) keep running fully concurrently under {@code parallel="tests"}. Set this on every
     * block whose tests assert on a delivered notification (e.g. the self-validate revoke→401 walk); leave it
     * off everywhere else. See {@link DynamicApimContainer#withExternalIsNotificationAlias}.
     */
    static final String PARAM_RECEIVE_EXTERNAL_IS_NOTIFICATIONS = "receiveExternalIsNotifications";
    /**
     * JVM-wide permit backing {@link #PARAM_RECEIVE_EXTERNAL_IS_NOTIFICATIONS}: at most one live container may
     * hold the {@code wso2am} alias, so holder blocks serialize on this while all other blocks run free. A
     * {@link Semaphore} (not a lock) because acquire happens on the block's onStart thread and release on its
     * onFinish thread. Release is guarded by {@link #ALIAS_PERMIT_HELD_ATTRIBUTE} so the boot-failure path and
     * onFinish can't double-release.
     */
    private static final Semaphore IS_NOTIFY_ALIAS_PERMIT = new Semaphore(1);
    /** Test-context attribute marking that this block holds {@link #IS_NOTIFY_ALIAS_PERMIT} (single-release guard). */
    private static final String ALIAS_PERMIT_HELD_ATTRIBUTE = "isNotifyAliasPermitHeld";
    /**
     * Optional block param: module-relative path of an IS deployment.toml EXTRA overlay, appended AFTER the
     * built-in external-key-manager overlay (additive, mirroring the APIM {@code tomlExtraOverlayPath}
     * semantics) so a block can boot IS with block-specific config (e.g. the tenant-sync listener). Distinct
     * extra overlays map to distinct IS containers ({@link IdentityServerContainer#getInstance(String, String)});
     * note distinct overlays cannot run in parallel within one JVM (see that method) - they belong in separate
     * suites.
     */
    static final String PARAM_IS_TOML_EXTRA_OVERLAY = "isTomlExtraOverlayPath";
    /** Shared-scope key holding the host-mapped external IS base URL (for scenarios requesting a token from IS). */
    static final String IS_BASE_URL_KEY = "isBaseUrl";

    @Override
    public void onStart(ITestContext context) {

        // Opt-in gate: a block joins the parallel-on-shared lane only by declaring a blockLabel param.
        // Without it, the listener no-ops so it never boots a stray container or disturbs a <test> block
        // that manages its own container lifecycle.
        String label = param(context, PARAM_BLOCK_LABEL);
        if (label == null || label.isBlank()) {
            return;
        }

        String sharedScopeId = TestContext.sharedScopeId(context);
        TestContext.setScope(sharedScopeId, sharedScopeId);

        // Held outside the try so the catch can stop a container that started but then failed before it was
        // handed off to TestContext (below). Without this a boot failure on the start()/URL/readiness path
        // leaks a live Docker container for the JVM lifetime (only reaped later by Ryuk).
        DynamicApimContainer container = null;
        try {
            // Start the shared backend first (idempotent singleton on the shared network) when the block opts
            // in, so APIs deployed by gateway-invocation tests have a reachable "nodebackend" upstream.
            if (Boolean.parseBoolean(param(context, PARAM_INIT_BACKEND))) {
                NodeAppServer.getInstance();
                logger.info("Block '" + label + "' ensured NodeAppServer backend is running");
            }

            // IS infrastructure only. Registering IS as a key manager is admin product behaviour and lives in
            // the features (see PARAM_BOOT_EXTERNAL_IS javadoc).
            boolean bootExternalIs = Boolean.parseBoolean(param(context, PARAM_BOOT_EXTERNAL_IS));

            container = new DynamicApimContainer(label, resolveTomlContent(context));
            container.withLabel("block", label);
            // Boot-time server files (see PARAM_SERVER_FILES_TO_COPY): copied before start so boot-only
            // deployers (e.g. the user-store config deployer) pick them up.
            String filesToCopy = param(context, PARAM_SERVER_FILES_TO_COPY);
            if (filesToCopy != null && !filesToCopy.isBlank()) {
                for (String pair : filesToCopy.split(",")) {
                    String[] parts = pair.trim().split("::", 2);
                    if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                        throw new IllegalArgumentException("Malformed " + PARAM_SERVER_FILES_TO_COPY
                                + " entry '" + pair + "' — expected <hostPath>::<serverRelativePath>");
                    }
                    container.withServerFile(parts[0].trim(), parts[1].trim());
                    logger.info("Block '" + label + "' will copy server file " + parts[0].trim()
                            + " -> <server-home>/" + parts[1].trim());
                }
            }
            // Opt-in integration coverage: attach the JaCoCo agent before boot (see CoverageSupport).
            if (CoverageSupport.enabled()) {
                container.withCoverage();
            }
            // External IS: augment APIM's truststore with the IS TLS cert BEFORE boot (the JVM reads the
            // truststore once at start), so APIM trusts https://wso2is:9443 for the federated OIDC/JWKS/
            // introspection calls. Needed whenever IS is booted, independent of KM registration.
            if (bootExternalIs) {
                container.withExternalKmTrust();
            }
            // Reverse-channel receiver (opt-in): queue for the JVM-wide alias permit, then bind the fixed
            // wso2am alias so IS's notification POSTs reach THIS container. Holder blocks serialize among
            // themselves here; alias-free external-KM blocks never touch the permit and stay concurrent.
            if (Boolean.parseBoolean(param(context, PARAM_RECEIVE_EXTERNAL_IS_NOTIFICATIONS))) {
                logger.info("Block '" + label + "' waiting for the wso2am notification-alias permit"
                        + (IS_NOTIFY_ALIAS_PERMIT.availablePermits() == 0 ? " (held by another block)" : ""));
                IS_NOTIFY_ALIAS_PERMIT.acquireUninterruptibly();
                context.setAttribute(ALIAS_PERMIT_HELD_ATTRIBUTE, Boolean.TRUE);
                container.withExternalIsNotificationAlias();
            }
            container.start();

            String baseUrl = container.getServletHttpsUrl();
            String gatewayUrl = container.getGatewayHttpsUrl();
            if (!ServerReadiness.awaitReady(baseUrl)) {
                throw new IllegalStateException("APIM block '" + label + "' did not become ready within "
                        + (Constants.SERVER_STARTUP_WAIT_TIME / 1000) + "s");
            }

            TestContext.setShared(CONTAINER_KEY, container);
            TestContext.setShared(BASE_URL_KEY, baseUrl);
            TestContext.setShared(BASE_GATEWAY_URL_KEY, gatewayUrl);
            TestContext.setShared(BASE_GATEWAY_WS_URL_KEY, container.getGatewayWsUrl());
            TestContext.setShared(BASE_GATEWAY_WSS_URL_KEY, container.getGatewayWssUrl());
            TestContext.setShared(GATEWAY_CLIENT_IP_KEY, container.getGatewayClientIp());
            logger.info("Block '" + label + "' booted and ready: baseUrl=" + baseUrl
                    + " baseGatewayUrl=" + gatewayUrl);

            if (Boolean.parseBoolean(param(context, PARAM_INIT_TENANT_USERS))) {
                provisionTenantUsers(label, param(context, PARAM_TENANT_SET));
            }
            // Runtime secondary user store (replaces the seeded .mv.db fixture). After tenant provisioning so the
            // tenant admin SOAP credentials exist. Registered + seeded for BOTH tenants — one shared H2 DB,
            // isolated by UM_TENANT_ID — so scenarios exercise the ×4 matrix (2 tenants × 2 store-user actors).
            if (Boolean.parseBoolean(param(context, PARAM_INIT_SECONDARY_USER_STORE))) {
                SecondaryUserStoreProvisioner.provision(container, Constants.SUPER_TENANT_DOMAIN, "tenant1.com");
            }

            // External IS: start IS on the shared network and publish its host-mapped base URL. Done AFTER
            // provisioning so a super-admin token exists for any subsequent admin REST call. APIM's truststore
            // was already augmented above (before boot), so federated OIDC / JWKS / introspection calls trust IS.
            if (bootExternalIs) {
                bootExternalIdentityServer(label, param(context, PARAM_IS_TOML_EXTRA_OVERLAY));
            }
        } catch (Throwable t) {
            context.setAttribute(BOOT_ERROR_ATTRIBUTE, t);
            // Not "skipped": BaseBlockRunner's @BeforeClass rethrows this bootError, so the block's classes
            // are reported FAILED and the build stays red (a skip would leave it green — see BaseBlockRunner).
            logger.error("Block '" + label + "' boot/readiness failed; its classes will be reported as failed", t);
            // Stop the failed container here (the readiness/URL/start path never handed it to TestContext, so
            // onFinish can't reap it). Guard the stop in its own try so a stop() failure doesn't mask the
            // original boot cause. A container already stored/stopped tolerates a redundant stop() as a no-op.
            if (container != null) {
                try {
                    container.stop();
                } catch (Throwable stopErr) {
                    logger.warn("Block '" + label + "' failed-container stop() also failed", stopErr);
                }
            }
            // A boot failure after the alias permit was acquired must free it here — onFinish also releases,
            // but only-if-held, so the two paths can't double-release (see releaseAliasPermitIfHeld).
            releaseAliasPermitIfHeld(context);
        } finally {
            // Defensive hygiene: never leave this block's scope bound to the (pooled) thread that ran
            // onStart. Per-invocation scoping in BlockScopeListener already resets scope before any body
            // reads it, and the block's shared entries persist (keyed by scope id in the static map), so
            // clearing the ThreadLocal here is safe and mirrors onFinish.
            TestContext.clearScope();
        }
    }

    @Override
    public void onFinish(ITestContext context) {

        // Mirror the onStart opt-in: a block this listener never managed must be left entirely alone.
        String label = param(context, PARAM_BLOCK_LABEL);
        if (label == null || label.isBlank()) {
            return;
        }

        String sharedScopeId = TestContext.sharedScopeId(context);
        TestContext.setScope(sharedScopeId, sharedScopeId);
        try {
            Object stored = TestContext.get(CONTAINER_KEY);
            if (stored instanceof DynamicApimContainer container) {
                // Dump JaCoCo counters over the mapped tcpserver port BEFORE stopping (all-in-one lane).
                // Best-effort: a dump failure must never break teardown or fail the block.
                if (CoverageSupport.enabled()) {
                    try {
                        String moduleDir = ModulePathResolver.getModuleDir(BlockLifecycleListener.class);
                        JacocoCoverage.dump(container.getCoverageDumpHost(), container.getCoverageDumpPort(),
                                CoverageSupport.execFile(moduleDir, label));
                    } catch (Exception e) {
                        logger.warn("Coverage dump failed for block '" + label + "': " + e.getMessage());
                    }
                }
                container.stop();
                logger.info("Block '" + context.getName()
                        + "' container stopped; dynamic host ports released by Docker");
            }
        } finally {
            // AFTER the container is stopped (its wso2am alias is gone from the network only then), hand the
            // notification-alias permit to the next queued holder block. No-op if this block never held it or
            // the boot-failure path already released it.
            releaseAliasPermitIfHeld(context);
            TestContext.clear();
            TestContext.clearScope();
        }
    }

    /**
     * Releases {@link #IS_NOTIFY_ALIAS_PERMIT} exactly once per holding block: the held-marker attribute is
     * flipped before releasing, so whichever of the two callers (boot-failure catch, onFinish) runs second
     * finds it cleared and no-ops — a double release would let two alias holders run live simultaneously.
     */
    private static void releaseAliasPermitIfHeld(ITestContext context) {
        if (Boolean.TRUE.equals(context.getAttribute(ALIAS_PERMIT_HELD_ATTRIBUTE))) {
            context.setAttribute(ALIAS_PERMIT_HELD_ATTRIBUTE, Boolean.FALSE);
            IS_NOTIFY_ALIAS_PERMIT.release();
        }
    }

    /**
     * Provisions the selected tenant/user set against the block's OWN booted container. {@code baseUrl} is
     * already published into the block's shared scope, so {@link TenantUserProvisioner} (which reads it from
     * there) targets this container's mapped port. Mirrors the legacy init features: the {@code default} set
     * matches {@code tenant_users_initialisation.feature}; {@code adpsample} matches
     * {@code migrated_tenant_user_initialization.feature}. Called inside onStart's try, so a provisioning
     * failure becomes {@code bootError} and the block is skipped cleanly rather than NPE-ing mid-scenario.
     */
    private void provisionTenantUsers(String label, String tenantSet) throws java.io.IOException, JaxenException {

        // Gateway readiness can pass before the SOAP admin services finish deploying; gate on the Tenant Mgt
        // service being live so provisioning never fires into a transient 404 (a race parallel boots widen).
        TenantUserProvisioner.awaitTenantMgtServiceReady();

        if (TENANT_SET_ADPSAMPLE.equalsIgnoreCase(tenantSet)) {
            TenantUserProvisioner.addAdpsampleTenant();
            TenantUserProvisioner.addUser(Constants.ADPSAMPLE_TENANT_DOMAIN, "userKey1",
                    "testTenantUser11", "testTenantUser11", "ADP_CREATOR, ADP_PUBLISHER, ADP_SUBSCRIBER");
        } else {
            String allRoles = "Internal/creator, Internal/publisher, Internal/subscriber";
            String publisherRoles = "Internal/creator, Internal/publisher";
            String subscriberRoles = "Internal/subscriber";
            TenantUserProvisioner.addSuperTenant();
            TenantUserProvisioner.addTenant("tenant1.com", "admin", "admin", "First", "Tenant",
                    "admin@tenant1.com");
            // Keep the original all-roles user (back-compat for any actor that needs creator+publisher+subscriber).
            TenantUserProvisioner.addUser(Constants.SUPER_TENANT_DOMAIN, Constants.USER_KEY,
                    "testUser1", "testUser1", allRoles);
            TenantUserProvisioner.addUser("tenant1.com", Constants.USER_KEY, "testUser11", "testUser11", allRoles);
            // Least-privilege publisher (creator+publisher, NOT admin) — the default actor for publisher tests.
            TenantUserProvisioner.addUser(Constants.SUPER_TENANT_DOMAIN, Constants.PUBLISHER_USER_KEY,
                    "publisherUser1", "publisherUser1", publisherRoles);
            TenantUserProvisioner.addUser("tenant1.com", Constants.PUBLISHER_USER_KEY,
                    "publisherUser11", "publisherUser11", publisherRoles);
            // Subscriber-only (self-signup-equivalent) — for access-control negatives (publisher ops -> 403).
            TenantUserProvisioner.addUser(Constants.SUPER_TENANT_DOMAIN, Constants.SUBSCRIBER_USER_KEY,
                    "subscriberUser1", "subscriberUser1", subscriberRoles);
            TenantUserProvisioner.addUser("tenant1.com", Constants.SUBSCRIBER_USER_KEY,
                    "subscriberUser11", "subscriberUser11", subscriberRoles);
        }
        logger.info("Block '" + label + "' provisioned tenant set '"
                + (tenantSet == null || tenantSet.isBlank() ? "default" : tenantSet) + "'");
    }

    /**
     * Starts the WSO2 IS 7.1.0 container on the shared network (alias {@code wso2is}), waits for its OIDC
     * discovery to serve 200, publishes its host-mapped base URL to the block's shared scope, and registers it
     * as the {@code WSO2-IS7-KM} key manager in APIM. Called inside onStart's try, so any failure here becomes
     * {@code bootError} and the block fails cleanly rather than NPE-ing mid-scenario. The IS singleton is not
     * stopped in onFinish: like {@code NodeAppServer}, it is a shared, reuse-enabled singleton reaped by Ryuk at
     * JVM exit — stopping it per block would break other external-KM blocks sharing it.
     */
    /**
     * Starts (or reuses) the external WSO2 IS container for this block's IS toml overlay, waits for its OIDC
     * discovery to serve 200, and publishes its host-mapped base URL under {@link #IS_BASE_URL_KEY}. Does NOT
     * register a key manager. The IS singleton(s) are reaped by Ryuk at JVM exit (not per block), like
     * {@code NodeAppServer} - stopping one per block would break sibling blocks sharing it.
     *
     * @param isTomlExtraOverlayPath module-relative path of an IS EXTRA overlay toml appended after the built-in
     *                               overlay, or {@code null}/blank for none
     */
    private void bootExternalIdentityServer(String label, String isTomlExtraOverlayPath) throws java.io.IOException {

        IdentityServerContainer is;
        if (isTomlExtraOverlayPath != null && !isTomlExtraOverlayPath.isBlank()) {
            String moduleDir = ModulePathResolver.getModuleDir(BlockLifecycleListener.class);
            String extraContent = Files.readString(Paths.get(moduleDir, isTomlExtraOverlayPath).normalize());
            is = IdentityServerContainer.getInstance(isTomlExtraOverlayPath, extraContent);
        } else {
            is = IdentityServerContainer.getInstance();
        }
        String isBaseUrl = is.getBaseHttpsUrl();
        if (!ServerReadiness.awaitIdentityServerReady(isBaseUrl)) {
            throw new IllegalStateException("External Identity Server for block '" + label
                    + "' did not become ready within " + (Constants.SERVER_STARTUP_WAIT_TIME / 1000) + "s");
        }
        TestContext.setShared(IS_BASE_URL_KEY, isBaseUrl);
        logger.info("Block '" + label + "' booted external Identity Server (extra overlay='"
                + (isTomlExtraOverlayPath == null || isTomlExtraOverlayPath.isBlank() ? "none"
                        : isTomlExtraOverlayPath) + "'); isBaseUrl=" + isBaseUrl);
    }

    private String resolveTomlContent(ITestContext context) throws java.io.IOException {
        String overlayPath = param(context, PARAM_TOML_OVERLAY);
        if (overlayPath != null && !overlayPath.isBlank()) {
            // Explicit full-file replacement: the block supplies a complete deployment.toml verbatim.
            return Files.readString(Path.of(overlayPath));
        }
        // Default lane: merge the small basic overlay onto the product distribution toml (the base
        // shipped in the image), so the test config tracks distribution defaults instead of a stale copy.
        String moduleDir = ModulePathResolver.getModuleDir(BlockLifecycleListener.class);
        Path basePath = Paths.get(moduleDir, Constants.DISTRIBUTION_TOML_PATH).normalize();
        Path overlay = Paths.get(moduleDir, Constants.DEFAULT_TOML_PATH).normalize();

        // A block may layer a small feature-specific overlay on top of basic (e.g. custom auth header /
        // application sharing) without restating the whole distribution config.
        String extraOverlayPath = param(context, PARAM_TOML_EXTRA_OVERLAY);
        if (extraOverlayPath != null && !extraOverlayPath.isBlank()) {
            Path extraOverlay = Paths.get(moduleDir, extraOverlayPath).normalize();
            return Utils.mergeTomls(basePath.toString(),
                    java.util.List.of(overlay.toString(), extraOverlay.toString()));
        }
        return Utils.mergeToml(basePath.toString(), overlay.toString());
    }

    private String param(ITestContext context, String name) {
        XmlTest xmlTest = context.getCurrentXmlTest();
        return xmlTest != null ? xmlTest.getLocalParameters().get(name) : null;
    }
}
