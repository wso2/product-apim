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

package org.wso2.am.testcontainers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

/**
 * WSO2 Identity Server 7.x container used as a third-party (external) Key Manager for APIM in the
 * integration-v2 lane. Modelled on {@link NodeAppServer}: a lazy singleton {@link GenericContainer} joined to
 * {@link ContainerNetwork#SHARED_NETWORK} under the network alias {@code wso2is}, so APIM (alias {@code wso2am})
 * reaches it at {@code https://wso2is:9443/...} — the same host baked into the KM registration payload and the
 * IS OAuth issuer.
 *
 * <p>Before start it injects these fixtures over the image defaults:
 * <ul>
 *   <li>a {@code deployment.toml} produced by deep-merging the small overlay {@code is7/deployment-overlay.toml}
 *       (only the documented key-manager additions: {@code hostname = "wso2is"}, the {@code wso2is.p12} TLS
 *       keystore, and the oauth/scim/role_mgt/revocation config) onto the IS image's OWN default
 *       {@code deployment.toml} (extracted at build time to {@code target/is7/is-default-deployment.toml}).
 *       This keeps IS on its distribution defaults across versions - same base+overlay pattern the APIM
 *       container uses - instead of restating a full file;</li>
 *   <li>a fixed TLS keystore {@code wso2is.p12} (CN=wso2is + SAN dns:wso2is) so IS presents a cert valid for
 *       the {@code wso2is} hostname that APIM trusts (APIM's truststore is augmented with its public cert —
 *       see {@code DynamicApimContainer#withExternalKmTrust});</li>
 *   <li>the {@code wso2is.notification.event.handlers} jar into {@code repository/components/dropins/}, whose
 *       class the deployment.toml's token-revocation {@code event_listener} references.</li>
 * </ul>
 *
 * <p>IS 7.3.0 is multi-arch (native {@code linux/arm64} and {@code linux/amd64}), so it boots natively on both
 * Apple-silicon dev machines and amd64 CI - typically well under a minute. The version is driven by the
 * {@code is.server.version} system property (default {@value #DEFAULT_IS_VERSION}), which also derives the
 * in-container {@code IS_HOME}; the image can be overridden wholesale via {@code is.docker.image.name}.
 */
public class IdentityServerContainer {

    private static final Log logger = LogFactory.getLog(IdentityServerContainer.class);

    /** Network alias APIM and the KM registration payload use to reach IS on the shared network. */
    public static final String NETWORK_ALIAS = "wso2is";
    /** IS management HTTPS port (token, JWKS, DCR, SCIM, admin REST). */
    public static final int HTTPS_PORT = 9443;

    /** Default IS version; overridable via {@code -Dis.server.version} (kept in sync with the pom property). */
    private static final String DEFAULT_IS_VERSION = "7.3.0";
    private static final String IS_VERSION = System.getProperty("is.server.version", DEFAULT_IS_VERSION);
    private static final String DEFAULT_IMAGE = "wso2/wso2is:" + IS_VERSION;
    private static final String IS_HOME = "/home/wso2carbon/wso2is-" + IS_VERSION;
    private static final String CONTAINER_TOML_PATH = IS_HOME + "/repository/conf/deployment.toml";
    private static final String DEPLOYMENT_OVERLAY_RESOURCE = "is7/deployment-overlay.toml";
    private static final String CONTAINER_TLS_KEYSTORE_PATH =
            IS_HOME + "/repository/resources/security/wso2is.p12";
    private static final String CONTAINER_DROPINS_DIR = IS_HOME + "/repository/components/dropins/";
    private static final String NOTIFICATION_JAR_NAME = "wso2is.notification.event.handlers-2.1.3.jar";
    private static final String CONTAINER_TRUSTSTORE_PATH =
            IS_HOME + "/repository/resources/security/client-truststore.p12";

    /** Multiton key for the built-in default overlay ({@link #DEPLOYMENT_OVERLAY_RESOURCE}). */
    public static final String DEFAULT_OVERLAY_KEY = "__default__";

    private final GenericContainer<?> container;

    public IdentityServerContainer() {
        this(DEFAULT_OVERLAY_KEY, null);
    }

    /**
     * @param overlayKey          multiton key identifying the overlay variant (a block's
     *                            {@code isTomlExtraOverlayPath}, or {@link #DEFAULT_OVERLAY_KEY})
     * @param extraOverlayContent additional IS deployment toml text appended AFTER the built-in
     *                            {@link #DEPLOYMENT_OVERLAY_RESOURCE} (additive, mirroring the APIM
     *                            {@code tomlExtraOverlayPath} semantics), or {@code null} for none
     */
    public IdentityServerContainer(String overlayKey, String extraOverlayContent) {

        logger.info("Initializing IdentityServerContainer (WSO2 IS " + IS_VERSION + ", overlay='"
                + overlayKey + "')...");
        String deploymentToml = buildDeploymentToml(extraOverlayContent);
        container = new GenericContainer<>(System.getProperty("is.docker.image.name", DEFAULT_IMAGE))
                .withExposedPorts(HTTPS_PORT)
                .withNetwork(ContainerNetwork.SHARED_NETWORK)
                .withNetworkAliases(NETWORK_ALIAS)
                // deployment.toml = IS image default (extracted at build time) + is7/deployment-overlay.toml
                // (hostname=wso2is, TLS keystore, oauth/scim/role_mgt/revocation config) - see buildDeploymentToml.
                .withCopyToContainer(Transferable.of(deploymentToml), CONTAINER_TOML_PATH)
                // Fixed TLS keystore whose cert (CN=wso2is, SAN dns:wso2is) APIM trusts.
                .withCopyToContainer(MountableFile.forClasspathResource("is7/wso2is.p12"),
                        CONTAINER_TLS_KEYSTORE_PATH)
                // Notification handler jar referenced by the revocation event_listener in deployment.toml.
                .withCopyToContainer(MountableFile.forHostPath(notificationJarPath()),
                        CONTAINER_DROPINS_DIR + NOTIFICATION_JAR_NAME)
                // Truststore augmented with APIM's wso2am cert, so IS trusts https://wso2am:9443 for the reverse
                // (token-revocation notification) channel — see build-augmented-is-truststore in the pom. IS's
                // outbound EventSender does STRICT hostname verification by default; APIM presents a cert whose
                // SAN includes 'wso2am' (see DynamicApimContainer#withExternalKmTrust + the is7km toml overlay),
                // so verification passes without the -Dhttpclient.hostnameVerifier=AllowAll workaround.
                .withCopyToContainer(MountableFile.forHostPath(augmentedTruststorePath()),
                        CONTAINER_TRUSTSTORE_PATH)
                // IS 7.3.0 is multi-arch and boots natively (well under a minute); allow margin for CI/load.
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(4)));

        JclLogConsumer logConsumer = new JclLogConsumer(logger);
        container.withLogConsumer(logConsumer);
        container.start();
        logger.info("IdentityServerContainer successfully initialized: " + getBaseHttpsUrl());
    }

    /**
     * Builds the IS {@code deployment.toml} = IS image default (with {@code [server] hostname} rewritten to the
     * network alias) + the built-in {@link #DEPLOYMENT_OVERLAY_RESOURCE} + optionally a block-specific EXTRA
     * overlay appended after it ({@code isTomlExtraOverlayPath}) - additive layering mirroring the APIM
     * {@code tomlExtraOverlayPath} semantics, since any networked IS still needs the default overlay's
     * hostname/keystore/oauth config.
     */
    private static String buildDeploymentToml(String extraOverlayContent) {
        File base = new File(defaultTomlPath());
        if (!base.isFile()) {
            throw new IllegalStateException("IS default deployment.toml not found at '" + base + "'. It is "
                    + "extracted from the IS image by the testcontainers pre-integration-test exec "
                    + "(extract-is-default-toml); run the build so it is staged, or set -Dis.default.toml.path "
                    + "to its location.");
        }
        try {
            String baseToml = java.nio.file.Files.readString(base.toPath());
            // Override the single default key we change: [server] hostname = "localhost" -> the network alias,
            // so the OAuth issuer and endpoints resolve to https://wso2is:9443 on the shared network. Done in
            // place (not via the overlay) to avoid a duplicate [server] table when the overlay is appended.
            String withHost = baseToml.replaceFirst("(?m)^(\\s*hostname\\s*=\\s*).*$",
                    "$1\"" + NETWORK_ALIAS + "\"");
            if (withHost.equals(baseToml)) {
                throw new IllegalStateException("Could not find a [server] hostname entry to override in the IS "
                        + "default deployment.toml at " + base);
            }
            StringBuilder toml = new StringBuilder(withHost)
                    .append(System.lineSeparator()).append(System.lineSeparator())
                    .append("# --- integration-v2 IS deployment overlay (appended) ---")
                    .append(System.lineSeparator())
                    .append(defaultOverlayContent());
            if (extraOverlayContent != null && !extraOverlayContent.isBlank()) {
                toml.append(System.lineSeparator()).append(System.lineSeparator())
                        .append("# --- block-specific IS extra overlay (appended) ---")
                        .append(System.lineSeparator())
                        .append(extraOverlayContent);
            }
            return toml.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build IS deployment.toml from " + base, e);
        }
    }

    /** Reads the built-in default external-key-manager overlay from the classpath. */
    private static String defaultOverlayContent() throws IOException {
        try (InputStream overlay = IdentityServerContainer.class.getClassLoader()
                .getResourceAsStream(DEPLOYMENT_OVERLAY_RESOURCE)) {
            if (overlay == null) {
                throw new IllegalStateException("IS deployment overlay resource not found on the classpath: "
                        + DEPLOYMENT_OVERLAY_RESOURCE);
            }
            return new String(overlay.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * Absolute path of the IS image's default deployment.toml, extracted to the testcontainers module's
     * {@code target/is7/} at build time by {@code extract-is-default-toml}. Overridable via
     * {@code -Dis.default.toml.path}.
     */
    private static String defaultTomlPath() {
        String configured = System.getProperty("is.default.toml.path");
        return (configured != null && !configured.isBlank())
                ? configured
                : System.getProperty("module.dir", ".") + "/target/is7/is-default-deployment.toml";
    }

    /**
     * Absolute path of the notification-handler jar the Maven {@code download-is-notification-jar} exec places
     * under the testcontainers module's {@code target/} at build time. Overridable via
     * {@code -Dis.notification.jar.path} for callers that stage it elsewhere. Fails fast with a clear message
     * if the jar is absent, rather than letting the container start without the class the event_listener needs.
     */
    private static String notificationJarPath() {
        String configured = System.getProperty("is.notification.jar.path");
        String path = (configured != null && !configured.isBlank())
                ? configured
                : System.getProperty("module.dir", ".") + "/target/is7/" + NOTIFICATION_JAR_NAME;
        File jar = new File(path);
        if (!jar.isFile()) {
            throw new IllegalStateException("IS notification handler jar not found at '" + path
                    + "'. It is downloaded by the testcontainers pre-integration-test exec "
                    + "(download-is-notification-jar); run the build so the jar is staged, or set "
                    + "-Dis.notification.jar.path to its location.");
        }
        return jar.getAbsolutePath();
    }

    /**
     * Absolute path of the augmented IS client-truststore (IS base truststore + APIM's cert) built by the
     * testcontainers {@code build-augmented-is-truststore} exec. Overridable via {@code -Dis.truststore.path}.
     * Fails fast if absent — without APIM's cert IS cannot POST revocation notifications to APIM over HTTPS.
     */
    private static String augmentedTruststorePath() {
        String configured = System.getProperty("is.truststore.path");
        String path = (configured != null && !configured.isBlank())
                ? configured
                : System.getProperty("module.dir", ".") + "/target/is7/is-client-truststore.p12";
        File ts = new File(path);
        if (!ts.isFile()) {
            throw new IllegalStateException("Augmented IS client-truststore not found at '" + path
                    + "'. It is built by the testcontainers pre-integration-test exec "
                    + "(build-augmented-is-truststore); run the build so it is staged, or set "
                    + "-Dis.truststore.path to its location.");
        }
        return ts.getAbsolutePath();
    }

    /** Host-mapped IS management HTTPS base URL (valid after start), e.g. {@code https://localhost:32771/}. */
    public String getBaseHttpsUrl() {
        return String.format("https://%s:%d/", container.getHost(), container.getMappedPort(HTTPS_PORT));
    }

    /**
     * Per-JVM registry of IS instances keyed by overlay. Each distinct overlay is a distinct IS container, but
     * all share {@link ContainerNetwork#SHARED_NETWORK} and the {@link #NETWORK_ALIAS} {@code wso2is} - because
     * the committed TLS cert (CN=wso2is, SAN dns:wso2is) is bound to that alias. Two DISTINCT overlays therefore
     * cannot coexist on the shared network in one JVM (docker DNS would round-robin the alias between them), so
     * {@link #getInstance(String, String)} fails fast in that case. This is not a limitation in practice: blocks
     * that need different IS tomls live in separate suites (separate JVMs, each with its own shared network).
     * The extension point for running distinct overlays IN PARALLEL within one JVM is per-overlay docker networks
     * (one {@code Network} per overlay, APIM joining its IS's network) - deferred until a suite actually needs it,
     * since it also requires the shared NodeAppServer backend to join those networks.
     */
    private static final java.util.Map<String, IdentityServerContainer> INSTANCES =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Returns the shared IS instance for the built-in default overlay (external key-manager config). */
    public static IdentityServerContainer getInstance() {
        return getInstance(DEFAULT_OVERLAY_KEY, null);
    }

    /**
     * Returns the IS instance for the given overlay variant, creating and starting it on first request.
     * Instances are cached per {@code overlayKey}. All instances share the network alias {@code wso2is};
     * requesting a SECOND distinct overlay in the same JVM throws (see {@link #INSTANCES} for why and the
     * extension point).
     *
     * @param overlayKey          a stable key for the variant ({@link #DEFAULT_OVERLAY_KEY} or a block's
     *                            {@code isTomlExtraOverlayPath})
     * @param extraOverlayContent extra toml appended after the built-in default overlay, or {@code null}
     */
    public static IdentityServerContainer getInstance(String overlayKey, String extraOverlayContent) {
        String key = (overlayKey == null || overlayKey.isBlank()) ? DEFAULT_OVERLAY_KEY : overlayKey;
        IdentityServerContainer existing = INSTANCES.get(key);
        if (existing != null) {
            return existing;
        }
        synchronized (INSTANCES) {
            IdentityServerContainer already = INSTANCES.get(key);
            if (already != null) {
                return already;
            }
            if (!INSTANCES.isEmpty()) {
                throw new IllegalStateException("An Identity Server with a different overlay is already running in "
                        + "this JVM (existing overlays: " + INSTANCES.keySet() + ", requested: '" + key + "'). "
                        + "Distinct IS overlays share the '" + NETWORK_ALIAS + "' alias on the shared network and "
                        + "cannot coexist in one JVM - run blocks needing different IS overlays in separate suites, "
                        + "or add per-overlay docker networks (see IdentityServerContainer.INSTANCES).");
            }
            IdentityServerContainer created = new IdentityServerContainer(key, extraOverlayContent);
            INSTANCES.put(key, created);
            return created;
        }
    }
}
