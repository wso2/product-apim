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
import org.wso2.am.integration.cucumbertests.utils.IntegrationActors;
import org.wso2.am.integration.cucumbertests.utils.ModulePathResolver;
import org.wso2.am.integration.cucumbertests.utils.ServerReadiness;
import org.wso2.am.integration.cucumbertests.utils.SecondaryUserStoreProvisioner;
import org.wso2.am.integration.cucumbertests.utils.TenantUserProvisioner;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.testcontainers.containers.Network;
import org.wso2.am.testcontainers.DynamicApimContainer;
import org.wso2.am.testcontainers.ApimRuntime;
import org.wso2.am.testcontainers.DynamicISContainer;
import org.wso2.am.testcontainers.DynamicPlatformGatewayContainer;
import org.wso2.am.testcontainers.JacocoCoverage;
import org.wso2.am.testcontainers.DynamicSolaceBroker;
import org.wso2.am.testcontainers.NodeAppServer;
import org.wso2.am.testcontainers.SquidProxyServer;

import java.net.URI;
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
    static final String BASE_GATEWAY_MANAGEMENT_URL_KEY = "baseGatewayManagementUrl";
    static final String BASE_GATEWAY_WS_URL_KEY = "baseGatewayWsUrl";
    static final String BASE_GATEWAY_WSS_URL_KEY = "baseGatewayWssUrl";
    static final String BASE_WEBSUB_EVENT_RECEIVER_URL_KEY = "baseWebSubEventReceiverUrl";
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
    /**
     * FRAMEWORK VERIFICATION ONLY. When {@code true}, provisioning deliberately fails: it targets a tenant
     * domain that was never created on this container, so the user-admin SOAP call is refused and the
     * exception propagates out of {@code onStart}.
     *
     * <p>The failure is REAL rather than a synthetic {@code throw}, so the blocks below exercise the same path
     * a genuine misconfiguration would. Two guarantees depend on it, and both are what make every OTHER
     * block's boot failure visible instead of a silent pass:
     * <ul>
     *   <li>{@code Phase5.5-ProvisioningFailure} — a provisioning failure in {@code onStart} turns the build
     *       RED (recorded as {@code bootError}, rethrown by {@code BaseBlockRunner}'s {@code @BeforeClass})
     *       rather than leaving the block green and empty;</li>
     *   <li>{@code Phase6.1-BrokenBlock} — a broken block fails in ISOLATION while its siblings pass, and its
     *       container is still released by {@code onFinish}.</li>
     * </ul>
     * Never set this on a product block.
     */
    static final String PARAM_INJECT_PROVISIONING_FAILURE = "injectProvisioningFailure";
    /** The domain {@link #PARAM_INJECT_PROVISIONING_FAILURE} targets — never created, hence the failure. */
    static final String UNPROVISIONED_TENANT_DOMAIN = "never-created.invalid";
    /**
     * When {@code true}, every user provisioned by {@link #PARAM_INIT_TENANT_USERS} (except the super-tenant
     * {@code admin}) gets an EMAIL-FORM physical username — {@code <base>@email.com} instead of {@code <base>} —
     * making "email as username" a first-class block mode. Pair it with the
     * {@code artifacts/configFiles/emailUserName} extra overlay, which turns on {@code [tenant_mgt]
     * enable_email_domain}; the two must be set together (see that file for why).
     *
     * <p>This is the mechanism that closes the legacy {@code SUPER_TENANT_EMAIL_USER} /
     * {@code TENANT_EMAIL_USER} {@code TestUserMode} rows: legacy fanned every class over those modes at the
     * {@code @Factory}, whereas here the mode is a property of the BLOCK, so a feature earns email-username
     * coverage by being listed in an email-mode block rather than by carrying extra {@code Examples} rows.
     *
     * <p><b>Actor references are unaffected.</b> Only the PHYSICAL username changes;
     * {@code Identity.resolveActor} still resolves {@code publisherUser@tenant1.com} / {@code admin}, because it
     * splits a ref on its FIRST {@code @} into {@code <userKey>@<domain>} — an email address must therefore never
     * appear in an actor ref. So every existing {@code Scenario Outline} actor column keeps working verbatim.
     *
     * <p><b>Why the mode has to change the provisioned usernames at all</b> (this is the "the flag breaks
     * provisioning" failure the mode exists to solve). With {@code enable_email_domain = true},
     * {@code MultitenantUtils} resolves a username by counting {@code @}: for exactly ONE {@code @} it assumes a
     * SUPER-TENANT login and keeps the whole string as the username; only with TWO or more does it split the
     * tenant off the LAST {@code @}. The framework's plain tenant actors carry exactly one {@code @}
     * ({@code admin@tenant1.com}), so with the flag on they resolve to a non-existent super-tenant user named
     * "admin@tenant1.com" and every tenant SOAP/REST call as that principal is refused — provisioning dies on the
     * first {@code addUser} into {@code tenant1.com}. Giving the user an email-form name restores the second
     * {@code @} ({@code admin@email.com@tenant1.com}), which is exactly the tenant-qualified email form the
     * product expects (the same {@code <email>@<tenantDomain>} shape the legacy EmailUserNameLoginTestCase
     * builds). The super-tenant {@code admin} stays plain {@code admin} (zero {@code @}), which resolves
     * correctly either way — converting it would destabilise every SOAP admin call in the block.
     *
     * <p>The username regexes are deliberately NOT touched: this build's {@code database_unique_id} defaults are
     * {@code UsernameJavaRegEx}/{@code UsernameJavaScriptRegEx} = {@code ^[\S]{3,30}$}, which already admit
     * {@code @}, so both the plain and the email form validate.
     *
     * <p>The flag is published into the block's shared scope under
     * {@link TenantUserProvisioner#EMAIL_USER_MODE_KEY} BEFORE any provisioning, and the mail-domain transform
     * itself lives in {@link TenantUserProvisioner#physicalUserName(String)} — so a user a SCENARIO provisions
     * at runtime ({@code I provision user …}) gets the same form as the boot-time set. It has to: a plain
     * runtime user would carry a single {@code @} once tenant-qualified and could not authenticate at all under
     * this flag (see that method).
     */
    static final String PARAM_EMAIL_USER_MODE = "emailUserMode";
    /**
     * When {@code true}, onStart ensures the shared NodeAppServer backend singleton is running and MULTI-HOMES it
     * onto this block's private network under the {@code nodebackend} alias before APIM boots, so
     * gateway-invocation tests have a reachable upstream for deployed APIs. One backend instance serves every
     * opting-in block; it is detached from this block's network at teardown (before the network is closed), which
     * is what keeps the network removable — see {@link #BACKEND_ATTACHED_KEY}.
     *
     * <p>The backend is the ONLY container shared this way. It is a stateless upstream that resolves no peer
     * names, so being attached to several block networks at once is unambiguous; anything that must resolve a
     * name pointing at APIM (IS, Solace) is per-block instead.
     */
    static final String PARAM_INIT_BACKEND = "initBackend";
    /**
     * When {@code true}, onStart boots {@link DynamicSolaceBroker} — a faked Solace connector control plane
     * (network alias {@code solaceshim}) plus a REAL PubSub+ broker (alias {@code solacebroker}) — BEFORE APIM,
     * because the block's toml declares a gateway environment whose {@code service_url} is
     * {@code http://solaceshim:8081}. Ordering follows {@link #PARAM_INIT_BACKEND} (before APIM) rather than
     * {@link #PARAM_BOOT_EXTERNAL_IS} (after APIM): nothing in the Solace arc needs APIM's certificate, and an
     * environment pointing at an absent host is a needless race.
     *
     * <p>The broker and shim are PER-BLOCK, booted on this block's private network and stopped at its teardown.
     * They cannot be a shared singleton multi-homed across block networks the way the node backend is: the shim
     * RESOLVES {@code apimforsolace} to fetch APIM's JWKS, and a container attached to two block networks would
     * resolve that name on both, leaving which APIM answers arbitrary. Per-block networks scope the alias, so any
     * number of blocks may set this and run concurrently. Each boots a full event broker (~1-2 min), so the cost
     * is per opting-in block.
     *
     * <p>Infrastructure ONLY (CLAUDE.md 14). Registering the Solace environment is toml; importing, deploying,
     * subscribing and key generation are feature steps run as an {@code Identity} actor. Product calls do not
     * belong here.
     */
    static final String PARAM_INIT_SOLACE_BROKER = "initSolaceBroker";
    /** When true, attach the shared Squid proxy to this block's private network. */
    static final String PARAM_INIT_PROXY = "initProxy";
    static final String SQUID_PROXY_KEY = "blockSquidProxy";
    static final String PROXY_ATTACHED_KEY = "proxyAttachedToBlockNetwork";
    private static final Semaphore PROXY_PERMIT = new Semaphore(1);
    private static final String PROXY_PERMIT_HELD_ATTRIBUTE = "proxyPermitHeld";
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
     * APIM is ready and tenants/users are provisioned, this block's own {@link DynamicISContainer} is started on
     * the block's private network, its OIDC discovery is awaited, and its host-mapped base URL is published under
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
     * on the block's OWN private network, so the alias is network-scoped: any number of receiver blocks may hold
     * it concurrently with no collision and no cross-block serialization. Set this on every block whose tests
     * assert on a delivered notification (e.g. the self-validate revoke→401 walk); leave it off everywhere else.
     * See {@link DynamicApimContainer#withExternalIsNotificationAlias}.
     */
    static final String PARAM_RECEIVE_EXTERNAL_IS_NOTIFICATIONS = "receiveExternalIsNotifications";
    /**
     * Optional block param: module-relative path of an IS deployment.toml EXTRA overlay, appended AFTER the
     * built-in external-key-manager overlay (additive, mirroring the APIM {@code tomlExtraOverlayPath}
     * semantics) so a block can boot IS with block-specific config (e.g. the tenant-sync listener). Each block
     * gets its OWN {@link DynamicISContainer} on its own private network, so distinct overlays now coexist
     * across concurrent blocks — the old shared-singleton fail-fast is gone.
     */
    static final String PARAM_IS_TOML_EXTRA_OVERLAY = "isTomlExtraOverlayPath";
    /** Shared-scope key holding the host-mapped external IS base URL (for scenarios requesting a token from IS). */
    static final String IS_BASE_URL_KEY = "isBaseUrl";

    /**
     * Shared-scope key holding this block's private docker {@link Network}.
     *
     * <p>This is what makes every fixed network alias in the lane safe. {@code wso2am} (IS's reverse notification
     * channel), {@code wso2is}, {@code apimforsolace} and {@code solaceshim} are all FIXED names — baked into
     * tomls, certs and container env — so on one shared network at most one live container could own each, and
     * the listener had to serialize the holder blocks on JVM-wide permits. Giving each block its own network makes
     * the names network-scoped, so N blocks can each have their own {@code wso2am}/{@code wso2is}/broker
     * concurrently and the permits are gone.
     *
     * <p>Closed at block teardown ({@link #teardownBlockInfra}); the entry is cleared once closed so the
     * boot-failure path and {@link #onFinish} cannot double-close it.
     */
    static final String BLOCK_NETWORK_KEY = "blockNetwork";
    /** Shared-scope key holding this block's {@link DynamicISContainer} (stopped at block teardown), when booted. */
    static final String IS_CONTAINER_KEY = "blockIsContainer";
    /** Shared-scope key holding this block's {@link DynamicSolaceBroker} (stopped at block teardown), when booted. */
    static final String SOLACE_KEY = "blockSolaceBroker";
    /**
     * Shared-scope flag: the shared backend singleton was multi-homed onto this block's network and must be
     * detached at teardown BEFORE the network is closed — Docker refuses to remove a network that still has a
     * container connected, so skipping the detach leaks the network.
     */
    static final String BACKEND_ATTACHED_KEY = "backendAttachedToBlockNetwork";

    /**
     * Boots this block's {@link DynamicPlatformGatewayContainer} (the two-service gateway compose) STANDALONE
     * after APIM is ready, with its host-mapped data-plane URL and the gateway→control-plane host published to
     * shared scope. Infrastructure ONLY — registering the gateway ({@code POST /api/am/admin/v4/gateways}) and
     * connecting it are ADMIN PRODUCT BEHAVIOUR done by the feature journey (§14), which resolves the container
     * from {@link #PLATFORM_GATEWAY_KEY} and calls {@link DynamicPlatformGatewayContainer#connect} with the token.
     */
    static final String PARAM_BOOT_PLATFORM_GATEWAY = "bootPlatformGateway";
    /** Shared-scope key holding this block's {@link DynamicPlatformGatewayContainer} (stopped at teardown). */
    static final String PLATFORM_GATEWAY_KEY = "blockPlatformGateway";
    /** Shared-scope key: host-mapped platform-gateway data-plane HTTPS base URL (APIs are invoked here). */
    static final String PLATFORM_GATEWAY_DATA_PLANE_URL_KEY = "platformGatewayDataPlaneUrl";
    /** Shared-scope key: the {@code host:port} the gateway dials for this block's APIM control plane. */
    static final String PLATFORM_GATEWAY_CONTROLPLANE_HOST_KEY = "platformGatewayControlPlaneHost";
    /** Shared-scope key: token URL reachable by a Gateway-hosted backend endpoint-security flow. */
    static final String BACKEND_OAUTH_TOKEN_URL_KEY = "backendOAuthTokenUrl";

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

        // Held outside the try so the catch can release infrastructure that started but then failed before it was
        // handed off to TestContext (below). Without this a boot failure on the start()/URL/readiness path
        // leaks a live Docker container for the JVM lifetime (only reaped later by Ryuk).
        ApimRuntime container = null;
        try {
            // Every block runs on its OWN private docker network, which is what makes the lane's fixed aliases
            // (wso2am / wso2is / apimforsolace / solaceshim / nodebackend) network-scoped instead of JVM-unique.
            // Created first and published immediately so BOTH the boot-failure catch and onFinish can tear it
            // down (detaching the shared backend before closing it).
            Network blockNetwork = Network.newNetwork();
            TestContext.setShared(BLOCK_NETWORK_KEY, blockNetwork);

            // Multi-home the shared (stateless) backend onto THIS block's network when the block opts in, so its
            // APIM resolves "nodebackend". The flag is set only AFTER a successful attach, so teardown never
            // tries to detach something that was never attached.
            if (Boolean.parseBoolean(param(context, PARAM_INIT_BACKEND))) {
                NodeAppServer.getInstance().attachToNetwork(blockNetwork);
                TestContext.setShared(BACKEND_ATTACHED_KEY, Boolean.TRUE);
                logger.info("Block '" + label + "' attached the shared NodeAppServer backend to its network");
            }

            if (Boolean.parseBoolean(param(context, PARAM_INIT_PROXY))) {
                try {
                    PROXY_PERMIT.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted waiting for proxy permit in block '" + label + "'", e);
                }
                context.setAttribute(PROXY_PERMIT_HELD_ATTRIBUTE, Boolean.TRUE);
                SquidProxyServer proxy = SquidProxyServer.getInstance();
                proxy.attachToNetwork(blockNetwork);
                TestContext.setShared(PROXY_ATTACHED_KEY, Boolean.TRUE);
                TestContext.setShared(SQUID_PROXY_KEY, proxy);
                logger.info("Block '" + label + "' attached the shared Squid proxy to its private network");
            }

            // Solace: faked connector + real broker on THIS block's network, up BEFORE APIM so the toml-declared
            // solaceEnv (service_url = http://solaceshim:8081, by network alias) resolves the moment APIM needs
            // it. Published before start() so a start failure is still torn down by the catch below.
            if (Boolean.parseBoolean(param(context, PARAM_INIT_SOLACE_BROKER))) {
                DynamicSolaceBroker solace = new DynamicSolaceBroker(label, blockNetwork);
                TestContext.setShared(SOLACE_KEY, solace);
                solace.start();
                logger.info("Block '" + label + "' booted its Solace pair; connector="
                        + solace.getConnectorBaseUrl() + " SEMP=" + solace.getSempUrl());
            }

            // IS infrastructure only. Registering IS as a key manager is admin product behaviour and lives in
            // the features (see PARAM_BOOT_EXTERNAL_IS javadoc).
            boolean bootExternalIs = Boolean.parseBoolean(param(context, PARAM_BOOT_EXTERNAL_IS));

            container = createApimContainer(label, context, blockNetwork);
            configureServerFiles(context, container);
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
            // Reverse-channel receiver (opt-in): bind the wso2am alias so IS's notification POSTs reach THIS
            // container. Scoped to the block's private network, so any number of receiver blocks run
            // concurrently — no JVM-wide permit, no serialization.
            if (Boolean.parseBoolean(param(context, PARAM_RECEIVE_EXTERNAL_IS_NOTIFICATIONS))) {
                container.withExternalIsNotificationAlias();
            }
            // Solace: the BROKER validates APIM-issued tokens by fetching APIM's JWKS itself, so APIM needs an
            // alias the broker can resolve. APIM has none by default; without this every publish is rejected
            // 403 with the reason only inside the broker's event.log. Also network-scoped now: this block's
            // broker is on this block's network, so exactly one APIM answers that name by construction.
            if (Boolean.parseBoolean(param(context, PARAM_INIT_SOLACE_BROKER))) {
                container.withSolaceJwksAlias();
            }
            container.start();

            String baseUrl = container.getServletHttpsUrl();
            String gatewayUrl = container.getGatewayHttpsUrl();
            String gatewayManagementUrl = container.getGatewayManagementHttpsUrl();
            if (!awaitApimReady(container)) {
                throw new IllegalStateException("APIM block '" + label + "' did not become ready within "
                        + (Constants.SERVER_STARTUP_WAIT_TIME / 1000) + "s");
            }

            TestContext.setShared(CONTAINER_KEY, container);
            TestContext.setShared(BASE_URL_KEY, baseUrl);
            TestContext.setShared(BACKEND_OAUTH_TOKEN_URL_KEY, container.getBackendOAuthTokenUrl());
            TestContext.setShared(BASE_GATEWAY_URL_KEY, gatewayUrl);
            TestContext.setShared(BASE_GATEWAY_MANAGEMENT_URL_KEY, gatewayManagementUrl);
            TestContext.setShared(BASE_GATEWAY_WS_URL_KEY, container.getGatewayWsUrl());
            TestContext.setShared(BASE_GATEWAY_WSS_URL_KEY, container.getGatewayWssUrl());
            TestContext.setShared(BASE_WEBSUB_EVENT_RECEIVER_URL_KEY, container.getWebSubEventReceiverUrl());
            TestContext.setShared(GATEWAY_CLIENT_IP_KEY, container.getGatewayClientIp());
            logger.info("Block '" + label + "' booted and ready: baseUrl=" + baseUrl
                    + " baseGatewayUrl=" + gatewayUrl);

            // Adds the built-in super-tenant admin to the actor registry.
            TenantUserProvisioner.addSuperTenant();

            if (Boolean.parseBoolean(param(context, PARAM_INIT_TENANT_USERS))) {
                provisionTenantUsers(label,
                        Boolean.parseBoolean(param(context, PARAM_INJECT_PROVISIONING_FAILURE)),
                        Boolean.parseBoolean(param(context, PARAM_EMAIL_USER_MODE)));
            }
            // Runtime secondary user store (replaces the seeded .mv.db fixture). After tenant provisioning so the
            // tenant admin SOAP credentials exist. Registered + seeded for BOTH tenants — one shared H2 DB,
            // isolated by UM_TENANT_ID — so scenarios exercise the ×4 matrix (2 tenants × 2 store-user actors).
            if (Boolean.parseBoolean(param(context, PARAM_INIT_SECONDARY_USER_STORE))) {
                SecondaryUserStoreProvisioner.provision(container, Constants.SUPER_TENANT_DOMAIN, "tenant1.com");
            }

            // External IS: start THIS block's own IS on the block's private network and publish its host-mapped
            // base URL. Done AFTER provisioning so a super-admin token exists for any subsequent admin REST call.
            // APIM's truststore was already augmented above (before boot), so federated OIDC / JWKS /
            // introspection calls trust IS.
            if (bootExternalIs) {
                bootExternalIdentityServer(label, param(context, PARAM_IS_TOML_EXTRA_OVERLAY),
                        (Network) TestContext.get(BLOCK_NETWORK_KEY));
            }

            // API Platform Gateway: started AFTER APIM is ready, because the control-plane host it must dial is
            // derived from APIM's host-mapped management port. Its compose gets its own testcontainers-namespaced
            // network, so it needs no place on this block's network.
            if (Boolean.parseBoolean(param(context, PARAM_BOOT_PLATFORM_GATEWAY))) {
                bootPlatformGateway(label, baseUrl);
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
            // A boot failure must leave no live IS/Solace container and no dangling private network. Shared with
            // onFinish, and idempotent: teardownBlockInfra clears each scope entry as it releases it, so whichever
            // path runs second finds nothing left to do rather than double-closing (a double close would log a
            // spurious docker "network not found" and mask a genuine leak).
            teardownBlockInfra(label);
            releaseProxyPermitIfHeld(context);
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
            if (stored instanceof ApimRuntime container) {
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
            // AFTER the APIM container is stopped — a container still connected keeps the network un-removable,
            // so the order (APIM stop -> IS/Solace stop -> backend detach -> network close) is load-bearing.
            teardownBlockInfra(label);
            releaseProxyPermitIfHeld(context);
            TestContext.clear();
            TestContext.clearScope();
        }
    }

    /**
     * Releases this block's private-network infrastructure: its IS and Solace containers, the shared backend's
     * attachment to the network, and finally the network itself.
     *
     * <p>ORDER IS LOAD-BEARING. Docker refuses to remove a network that still has any container connected, so
     * every member must be gone first: the APIM container is stopped by the caller, then IS and Solace here, then
     * the multi-homed backend is detached (it is NOT stopped — it is shared with other blocks and lives for the
     * JVM). Only then can the network be closed.
     *
     * <p>IDEMPOTENT. Each scope entry is removed as it is released, so the two callers (the onStart boot-failure
     * catch and {@link #onFinish}) can both run without double-closing — a second close would log a misleading
     * docker "network not found" and make a real leak harder to spot.
     *
     * <p>Each step is independently guarded so one failure never masks another or the block's real outcome, and a
     * failure to close the network is logged at WARN with its id: that line is the leak signal to grep for.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private void teardownBlockInfra(String label) {

        // Platform gateway first: it lives on its own compose network, so it neither blocks this block's network
        // close nor depends on anything below — releasing it early keeps its compose from outliving a failure.
        Object platformGateway = TestContext.get(PLATFORM_GATEWAY_KEY);
        if (platformGateway instanceof DynamicPlatformGatewayContainer gateway) {
            TestContext.removeShared(PLATFORM_GATEWAY_KEY);
            TestContext.removeShared(PLATFORM_GATEWAY_DATA_PLANE_URL_KEY);
            TestContext.removeShared(PLATFORM_GATEWAY_CONTROLPLANE_HOST_KEY);
            try {
                gateway.stop();
                logger.info("Block '" + label + "' platform gateway compose stopped");
            } catch (Throwable e) {
                logger.warn("Block '" + label + "' platform gateway stop() failed (continuing teardown): "
                        + e.getMessage());
            }
        }
        Object is = TestContext.get(IS_CONTAINER_KEY);
        if (is instanceof DynamicISContainer isContainer) {
            TestContext.removeShared(IS_CONTAINER_KEY);
            try {
                isContainer.stop();
                logger.info("Block '" + label + "' IS container stopped");
            } catch (Throwable e) {
                logger.warn("Block '" + label + "' IS stop() failed (continuing teardown): " + e.getMessage());
            }
        }
        Object solace = TestContext.get(SOLACE_KEY);
        if (solace instanceof DynamicSolaceBroker broker) {
            TestContext.removeShared(SOLACE_KEY);
            try {
                broker.stop();
                logger.info("Block '" + label + "' Solace broker + shim stopped");
            } catch (Throwable e) {
                logger.warn("Block '" + label + "' Solace stop() failed (continuing teardown): " + e.getMessage());
            }
        }
        Object net = TestContext.get(BLOCK_NETWORK_KEY);
        if (net instanceof Network network) {
            Object proxy = TestContext.get(SQUID_PROXY_KEY);
            if (proxy instanceof SquidProxyServer squid
                    && Boolean.TRUE.equals(TestContext.get(PROXY_ATTACHED_KEY))) {
                TestContext.removeShared(SQUID_PROXY_KEY);
                TestContext.removeShared(PROXY_ATTACHED_KEY);
                try {
                    squid.detachFromNetwork(network);
                } catch (Throwable e) {
                    logger.warn("Block '" + label + "' Squid proxy detach failed (network may leak): "
                            + e.getMessage());
                }
            }
            TestContext.removeShared(BLOCK_NETWORK_KEY);
            if (Boolean.TRUE.equals(TestContext.get(BACKEND_ATTACHED_KEY))) {
                TestContext.removeShared(BACKEND_ATTACHED_KEY);
                NodeAppServer.getInstance().detachFromNetwork(network);
            }
            try {
                network.close();
                logger.info("Block '" + label + "' private network closed");
            } catch (Throwable e) {
                logger.warn("Block '" + label + "' network close() FAILED — this network has LEAKED: "
                        + e.getMessage());
            }
        }
    }

    private static void releaseProxyPermitIfHeld(ITestContext context) {
        if (Boolean.TRUE.equals(context.getAttribute(PROXY_PERMIT_HELD_ATTRIBUTE))) {
            context.setAttribute(PROXY_PERMIT_HELD_ATTRIBUTE, Boolean.FALSE);
            PROXY_PERMIT.release();
        }
    }

    /**
     * Starts this block's API Platform Gateway (the two-service gateway compose) STANDALONE — not yet connected
     * to the control plane — and publishes its host-mapped data-plane URL plus the control-plane host the gateway
     * should dial. Registration + connection are ADMIN PRODUCT BEHAVIOUR done by a feature step (§14), which
     * resolves the container from {@link #PLATFORM_GATEWAY_KEY} and calls
     * {@link DynamicPlatformGatewayContainer#connect} with the token minted by {@code POST /gateways}.
     *
     * <p>APIM runs on the block's private network, but the gateway's own compose network cannot resolve the
     * {@code wso2am} alias, so it reaches APIM's control plane over a host-routable address instead
     * ({@link #controlPlaneHostFor(String)}); {@code insecure_skip_verify=true} in the gateway config means no
     * cert exchange is needed.
     *
     * <p>Published to shared scope BEFORE {@code start()} so a start failure is still reaped by
     * {@link #teardownBlockInfra(String)} instead of leaking a live compose project.
     *
     * @param apimBaseUrl this block's APIM management HTTPS URL (host-mapped 9443)
     */
    private void bootPlatformGateway(String label, String apimBaseUrl) {
        DynamicPlatformGatewayContainer gateway = new DynamicPlatformGatewayContainer(label);
        TestContext.setShared(PLATFORM_GATEWAY_KEY, gateway);
        gateway.start();
        String controlPlaneHost = controlPlaneHostFor(apimBaseUrl);
        TestContext.setShared(PLATFORM_GATEWAY_DATA_PLANE_URL_KEY, gateway.getDataPlaneHttpsUrl());
        TestContext.setShared(PLATFORM_GATEWAY_CONTROLPLANE_HOST_KEY, controlPlaneHost);
        logger.info("Block '" + label + "' booted platform gateway (standalone); dataPlane="
                + gateway.getDataPlaneHttpsUrl() + " controlPlaneHost=" + controlPlaneHost);
    }

    /**
     * The {@code host:port} the gateway's own compose network must dial to reach this block's APIM control plane.
     *
     * <p>Prefers the host APIM is published on (the base URL's host), which a container can reach whenever the
     * docker host is addressed by IP rather than by a loopback name. Falls back to {@code host.docker.internal},
     * granted to the gateway services by the compose's {@code extra_hosts}, when the base URL's host IS a loopback
     * name — a container cannot reach a published port through one.
     *
     * <p>{@code host.docker.internal} is not correct unconditionally: on a VM-backed docker it resolves to the
     * VM's host, which does not hold the ports published inside the VM.
     */
    private static String controlPlaneHostFor(String apimBaseUrl) {
        URI uri = URI.create(apimBaseUrl);
        String host = uri.getHost();
        boolean unreachableFromContainer = host == null || host.isBlank()
                || "localhost".equalsIgnoreCase(host) || host.startsWith("127.");
        return (unreachableFromContainer ? "host.docker.internal" : host) + ":" + uri.getPort();
    }

    /**
     * Provisions the tenant/user set against the block's OWN booted container. {@code baseUrl} is already
     * published into the block's shared scope, so {@link TenantUserProvisioner} (which reads it from there)
     * targets this container's mapped port. Mirrors the legacy {@code tenant_users_initialisation.feature}.
     * Called inside onStart's try, so a provisioning failure becomes {@code bootError} and the block is skipped
     * cleanly rather than NPE-ing mid-scenario.
     *
     * @param injectProvisioningFailure see {@link #PARAM_INJECT_PROVISIONING_FAILURE} — framework verification
     *                                  only; makes this method fail on purpose
     * @param emailUserMode             see {@link #PARAM_EMAIL_USER_MODE} — provisions every user (bar the
     *                                  super-tenant admin) with an email-form physical username
     */
    private void provisionTenantUsers(String label, boolean injectProvisioningFailure, boolean emailUserMode)
            throws java.io.IOException, JaxenException {

        // Published BEFORE the first addUser so the provisioner's physicalUserName transform is in force for the
        // boot-time set, and stays in shared scope so a scenario's own `I provision user …` gets the same form.
        TestContext.setShared(TenantUserProvisioner.EMAIL_USER_MODE_KEY, emailUserMode);

        // Gateway readiness can pass before the SOAP admin services finish deploying; gate on the Tenant Mgt
        // service being live so provisioning never fires into a transient 404 (a race parallel boots widen).
        TenantUserProvisioner.awaitTenantMgtServiceReady();

        if (injectProvisioningFailure) {
            // Deliberate failure for the framework-verification blocks. Placed AFTER the readiness gate so the
            // cause is unambiguously "this tenant does not exist" and not "the admin service was not up yet".
            logger.info("Block '" + label + "' sets " + PARAM_INJECT_PROVISIONING_FAILURE
                    + "=true; provisioning into the never-created tenant '" + UNPROVISIONED_TENANT_DOMAIN
                    + "' so boot fails on purpose.");
            TenantUserProvisioner.addUnprovisionedTenant(UNPROVISIONED_TENANT_DOMAIN);
            TenantUserProvisioner.addUser(UNPROVISIONED_TENANT_DOMAIN, "unprovisionedUserKey",
                    "unprovisionedUser", "unprovisionedUser", "Internal/subscriber");
            throw new IllegalStateException("Provisioning into '" + UNPROVISIONED_TENANT_DOMAIN
                    + "' was expected to fail for block '" + label + "' but succeeded — the "
                    + PARAM_INJECT_PROVISIONING_FAILURE + " fault injector is no longer injecting a fault.");
        } else {
            String allRoles = "Internal/creator, Internal/publisher, Internal/subscriber";
            String publisherRoles = "Internal/creator, Internal/publisher";
            String subscriberRoles = "Internal/subscriber";
            // Base names only — TenantUserProvisioner applies the email form when the mode is on (published
            // above). Only the USERNAME takes that form; the password stays the plain base name, so an
            // emailUserMode block differs from a default one in exactly one dimension.
            TenantUserProvisioner.addTenant("tenant1.com", "admin", "admin",
                    "First", "Tenant", "admin@tenant1.com");
            // The configured super-tenant admin must remain the plain `admin` because SOAP provisioning depends
            // on it. Give both organizations a separate admin-role actor in email mode so the email-login admin
            // arc is still a genuine Tenant ×2 outline rather than a tenant-only special case.
            if (emailUserMode) {
                TenantUserProvisioner.addUser(Constants.SUPER_TENANT_DOMAIN, Constants.EMAIL_ADMIN_USER_KEY,
                        "emailAdmin", "emailAdmin", "admin");
                TenantUserProvisioner.addUser("tenant1.com", Constants.EMAIL_ADMIN_USER_KEY,
                        "emailAdmin", "emailAdmin", "admin");
            }
            // Keep the original all-roles user (back-compat for any actor that needs creator+publisher+subscriber).
            TenantUserProvisioner.addUser(Constants.SUPER_TENANT_DOMAIN, Constants.USER_KEY,
                    "testUser1", "testUser1", allRoles);
            TenantUserProvisioner.addUser("tenant1.com", Constants.USER_KEY,
                    "testUser11", "testUser11", allRoles);
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
        logger.info("Block '" + label + "' provisioned tenant users"
                + (emailUserMode ? " with email-form usernames (e.g. "
                        + TenantUserProvisioner.physicalUserName("publisherUser1") + ")" : ""));
    }

    /**
     * Starts THIS block's own external WSO2 IS container on the block's private network (alias {@code wso2is}),
     * waits for its OIDC discovery to serve 200, and publishes its host-mapped base URL under
     * {@link #IS_BASE_URL_KEY}. Does NOT register a key manager.
     *
     * <p>One IS per IS-booting block, stopped at that block's teardown ({@link #teardownBlockInfra}). The
     * {@code wso2is} alias is network-scoped, so blocks with distinct overlays coexist and each owns its IS's
     * lifetime. IS is per-block rather than multi-homed (as the node backend is) because it must RESOLVE
     * {@code wso2am} for its reverse notification channel.
     *
     * <p>Published to scope by the CALLER before this returns is not possible (the container is created here), so
     * the caller stores the returned instance immediately — a start failure is otherwise invisible to teardown.
     *
     * @param isTomlExtraOverlayPath module-relative path of an IS EXTRA overlay toml appended after the built-in
     *                               overlay, or {@code null}/blank for none
     * @param blockNetwork           the block's private network the IS joins under the {@code wso2is} alias
     * @return the started IS container, for the caller to publish and later stop
     */
    private DynamicISContainer bootExternalIdentityServer(String label, String isTomlExtraOverlayPath,
            Network blockNetwork) throws java.io.IOException {

        String extraContent = null;
        if (isTomlExtraOverlayPath != null && !isTomlExtraOverlayPath.isBlank()) {
            String moduleDir = ModulePathResolver.getModuleDir(BlockLifecycleListener.class);
            extraContent = Files.readString(Paths.get(moduleDir, isTomlExtraOverlayPath).normalize());
        }
        DynamicISContainer is = new DynamicISContainer(label, extraContent);
        is.withNetwork(blockNetwork).withNetworkAliases(DynamicISContainer.NETWORK_ALIAS);
        // Published BEFORE start so a start/readiness failure is still reachable by the boot-failure teardown.
        TestContext.setShared(IS_CONTAINER_KEY, is);
        is.start();
        String isBaseUrl = is.getBaseHttpsUrl();
        if (!ServerReadiness.awaitIdentityServerReady(isBaseUrl)) {
            throw new IllegalStateException("External Identity Server for block '" + label
                    + "' did not become ready within " + (Constants.SERVER_STARTUP_WAIT_TIME / 1000) + "s");
        }
        TestContext.setShared(IS_BASE_URL_KEY, isBaseUrl);
        // Seed the IS integration actor (CLAUDE.md §14: actor-registry seeding is provisioner-legitimate) so
        // steps operating on IS's management plane authenticate through IntegrationActors instead of
        // hand-building credentials, and ISResourceCleanup can sweep their resources as this principal.
        IntegrationActors.register(new IntegrationActors.IntegrationActor(IntegrationActors.IS,
                Constants.SUPER_TENANT_ADMIN_USERNAME, Constants.SUPER_TENANT_ADMIN_PASSWORD, isBaseUrl));
        logger.info("Block '" + label + "' booted its external Identity Server (extra overlay='"
                + (isTomlExtraOverlayPath == null || isTomlExtraOverlayPath.isBlank() ? "none"
                        : isTomlExtraOverlayPath) + "'); isBaseUrl=" + isBaseUrl);
        return is;
    }

    /** Factory hook used by the distributed listener; all remaining block lifecycle is shared. */
    protected ApimRuntime createApimContainer(String label, ITestContext context, Network blockNetwork)
            throws java.io.IOException {
        DynamicApimContainer container = new DynamicApimContainer(label, resolveTomlContent(context));
        container.withLabel("block", label);
        container.withNetwork(blockNetwork);
        return container;
    }

    protected boolean awaitApimReady(ApimRuntime container) {
        return ServerReadiness.awaitReady(container.getServletHttpsUrl());
    }

    /** Hook for topology-specific boot-time file parameter parsing. */
    protected void configureServerFiles(ITestContext context, ApimRuntime container) {
        String filesToCopy = param(context, PARAM_SERVER_FILES_TO_COPY);
        if (filesToCopy == null || filesToCopy.isBlank()) {
            return;
        }
        for (String pair : filesToCopy.split(",")) {
            String[] parts = pair.trim().split("::", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new IllegalArgumentException("Malformed " + PARAM_SERVER_FILES_TO_COPY
                        + " entry '" + pair + "' — expected <hostPath>::<serverRelativePath>");
            }
            container.withServerFile(parts[0].trim(), parts[1].trim());
            logger.info("Block '" + param(context, PARAM_BLOCK_LABEL) + "' will copy server file " + parts[0].trim()
                    + " -> <server-home>/" + parts[1].trim());
        }
    }

    protected String resolveTomlContent(ITestContext context) throws java.io.IOException {
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

    protected String param(ITestContext context, String name) {
        XmlTest xmlTest = context.getCurrentXmlTest();
        return xmlTest != null ? xmlTest.getLocalParameters().get(name) : null;
    }
}
