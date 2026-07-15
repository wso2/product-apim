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
import org.wso2.am.integration.cucumbertests.utils.TenantUserProvisioner;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.testcontainers.DynamicApimContainer;
import org.wso2.am.testcontainers.JacocoCoverage;
import org.wso2.am.testcontainers.NodeAppServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

            container = new DynamicApimContainer(label, resolveTomlContent(context));
            container.withLabel("block", label);
            // Opt-in integration coverage: attach the JaCoCo agent before boot (see CoverageSupport).
            if (CoverageSupport.enabled()) {
                container.withCoverage();
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
            TestContext.clear();
            TestContext.clearScope();
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
