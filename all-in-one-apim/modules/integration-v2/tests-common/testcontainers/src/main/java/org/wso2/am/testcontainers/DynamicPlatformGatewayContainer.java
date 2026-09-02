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
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Comparator;

/**
 * WSO2 API Platform Gateway (data plane) for the parallel-on-shared lane — the standalone gateway runtime the
 * APIM control plane deploys APIs to. Unlike {@link DynamicISContainer} (one image), the platform gateway ships
 * as TWO images (gateway-controller + gateway-runtime), so this is a single-container ABSTRACTION over a
 * testcontainers {@link ComposeContainer} running the trimmed two-service compose ({@code platform-gateway/}).
 * testcontainers gives each instance a unique compose project identifier, so concurrent blocks' gateways are
 * fully isolated (containers / network / volumes namespaced) with no shared-network coordination.
 *
 * <p>Boot flow (mirrors the distribution's {@code scripts/setup.sh}, done in Java):
 * <ol>
 *   <li>materialize the compose + config.toml + listener certs into a fresh dist dir UNDER {@code $HOME} —
 *       Colima does NOT share {@code /tmp} into its VM, so a file bind-mount from {@code /tmp} silently becomes
 *       an empty directory and the gateway fails closed on "config.toml: is a directory";</li>
 *   <li>generate the AES-256 at-rest key and {@code api-platform.env} (the controller admin basic-auth cred and,
 *       when connecting, the control-plane host / token / gateway-name);</li>
 *   <li>run the compose and wait on the runtime's {@code /_gateway-health/ready} (HTTPS 8443).</li>
 * </ol>
 *
 * <p>The control-plane connection is optional at boot: a block that pre-seeds a token wires it via
 * {@link #withControlPlane} before {@link #start()}; the runtime-registration flow boots the gateway unconnected
 * and a feature step later calls {@link #connect} once {@code POST /gateways} has minted the token (registration
 * is admin product behaviour — a step, never a listener op, per CLAUDE.md §14). The gateway trusts the control
 * plane with {@code insecure_skip_verify=true} (test default), so no cert exchange is needed.
 *
 * <p>Gateway version is driven by {@code pg.gateway.version} (default {@value #DEFAULT_GW_VERSION}); the images
 * are multi-arch ({@code linux/arm64}+{@code amd64}), booting natively on Apple-silicon dev machines and amd64 CI.
 */
public class DynamicPlatformGatewayContainer {

    private static final Log logger = LogFactory.getLog(DynamicPlatformGatewayContainer.class);

    /** gateway-runtime data-plane HTTPS ingress — APIs are invoked here. */
    public static final int DATA_PLANE_HTTPS_PORT = 8443;
    private static final String RUNTIME_SERVICE = "gateway-runtime";
    private static final String CONTROLLER_SERVICE = "gateway-controller";
    private static final String HEALTH_READY_PATH = "/_gateway-health/ready";

    private static final String DEFAULT_GW_VERSION = "1.2.0";
    private static final String GW_VERSION = System.getProperty("pg.gateway.version", DEFAULT_GW_VERSION);

    /** The controller's OWN mgmt-API basic-auth cred — never called by tests; only needs to be valid config. */
    private static final String CONTROLLER_ADMIN_USER = "admin";
    // bcrypt ($2y$10$) hash of "admin"; the controller's basic authenticator requires a hash, not plaintext.
    private static final String CONTROLLER_ADMIN_PWD_HASH =
            "$2y$10$FwSBQPYCTSSjjnibmrI6i.9JB6zPjcfaP2kpLTQzkQWXHTu//63o.";

    private static final String[] RESOURCE_FILES = {
            "docker-compose.yaml", "config.toml",
            "listener-certs/default-listener.crt", "listener-certs/default-listener.key",
    };

    private final String blockLabel;
    private final Path distDir;
    private ComposeContainer compose;
    // control-plane connection (null host = standalone until connect()).
    private String cpHost;
    private String cpToken;
    private String cpGatewayName;

    public DynamicPlatformGatewayContainer(String blockLabel) {
        this.blockLabel = blockLabel;
        this.distDir = materializeDist();
        logger.info("Initializing DynamicPlatformGatewayContainer (gateway " + GW_VERSION + ", block='"
                + blockLabel + "') at " + distDir);
    }

    /** Pre-seed the control-plane connection BEFORE {@link #start()} (the pre-seed half of the hybrid flow). */
    public DynamicPlatformGatewayContainer withControlPlane(String host, String token, String gatewayName) {
        this.cpHost = host;
        this.cpToken = token;
        this.cpGatewayName = gatewayName;
        return this;
    }

    public void start() {
        try {
            writeRuntimeFiles();
            // The no-image ComposeContainer(File) constructor runs the LOCAL `docker compose` executable (in
            // testcontainers 2.x the constructor choice sets local-vs-container mode; there is no withLocalCompose).
            // Local mode is the path proven against Colima — the dist lives under $HOME so its bind-mounts resolve.
            compose = new ComposeContainer(distDir.resolve("docker-compose.yaml").toFile())
                    .withEnv("PG_GATEWAY_VERSION", GW_VERSION)
                    .withExposedService(RUNTIME_SERVICE, DATA_PLANE_HTTPS_PORT,
                            Wait.forHttp(HEALTH_READY_PATH).usingTls().allowInsecure()
                                    .forStatusCode(200).withStartupTimeout(Duration.ofMinutes(3)))
                    .withLogConsumer(CONTROLLER_SERVICE,
                            new JclLogConsumer(logger).withPrefix(blockLabel + "/gw-ctrl"))
                    .withLogConsumer(RUNTIME_SERVICE,
                            new JclLogConsumer(logger).withPrefix(blockLabel + "/gw-rt"));
            compose.start();
            logger.info("Platform gateway up (block='" + blockLabel + "'), data plane " + getDataPlaneHttpsUrl());
        } catch (RuntimeException e) {
            if (compose != null) {
                try {
                    compose.stop();
                } catch (RuntimeException cleanupError) {
                    logger.debug("Platform gateway startup cleanup failed: " + cleanupError.getMessage());
                }
            }
            deleteRecursively(distDir);
            throw e;
        }
    }

    public void stop() {
        try {
            if (compose != null) {
                compose.stop();
            }
        } finally {
            deleteRecursively(distDir);
        }
    }

    /** Host-mapped data-plane HTTPS base URL (valid after start), e.g. {@code https://localhost:32773/}. */
    public String getDataPlaneHttpsUrl() {
        return String.format("https://%s:%d/",
                compose.getServiceHost(RUNTIME_SERVICE, DATA_PLANE_HTTPS_PORT),
                compose.getServicePort(RUNTIME_SERVICE, DATA_PLANE_HTTPS_PORT));
    }

    /**
     * Connect a running (standalone) gateway to the control plane with a freshly-minted registration token — the
     * runtime-registration flow. Rewrites {@code api-platform.env} and restarts the compose so the controller
     * opens the control-plane WebSocket. Called from a feature step after {@code POST /gateways} (§14).
     */
    public void connect(String host, String token, String gatewayName) {
        withControlPlane(host, token, gatewayName);
        if (compose != null) {
            compose.stop();
        }
        start();
    }

    // --- internals ---

    private void writeRuntimeFiles() {
        try {
            // AES-256 at-rest key (the server never auto-generates it — fails closed if absent).
            byte[] key = new byte[32];
            new SecureRandom().nextBytes(key);
            Path aes = distDir.resolve("aesgcm-keys/default-aesgcm256-v1.bin");
            Files.createDirectories(aes.getParent());
            Files.write(aes, key);
            // certificates/ is a read-write mount for the controller's dynamic certificate management.
            Files.createDirectories(distDir.resolve("certificates"));
            // api-platform.env — controller admin cred + (optional) control-plane connection.
            StringBuilder env = new StringBuilder()
                    .append("GATEWAY_CONTROLLER_HOST=").append(CONTROLLER_SERVICE).append('\n')
                    .append("LOG_LEVEL=info\n")
                    .append("APIP_GW_CONTROLLER_AUTH_BASIC_ADMIN_USERNAME=")
                    .append(CONTROLLER_ADMIN_USER).append('\n')
                    .append("APIP_GW_CONTROLLER_AUTH_BASIC_ADMIN_PASSWORD_HASH=")
                    .append(CONTROLLER_ADMIN_PWD_HASH).append('\n');
            if (cpHost != null) {
                env.append("APIP_GW_CONTROLLER_CONTROLPLANE_HOST=").append(cpHost).append('\n')
                        .append("APIP_GW_CONTROLLER_CONTROLPLANE_TOKEN=").append(cpToken).append('\n')
                        .append("APIP_GW_CONTROLLER_CONTROLPLANE_GATEWAY_NAME=").append(cpGatewayName).append('\n');
            }
            Files.writeString(distDir.resolve("api-platform.env"), env.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write platform-gateway runtime files", e);
        }
    }

    private Path materializeDist() {
        Path dir = null;
        try {
            // Under $HOME so the file bind-mounts are visible inside the Colima VM (it does not share /tmp).
            Path base = Path.of(System.getProperty("user.home"), ".wso2-pgw-testcontainers");
            Files.createDirectories(base);
            dir = Files.createTempDirectory(base, "gw-");
            for (String rel : RESOURCE_FILES) {
                Path target = dir.resolve(rel);
                Files.createDirectories(target.getParent());
                try (InputStream in = cl().getResourceAsStream("platform-gateway/" + rel)) {
                    if (in == null) {
                        throw new IOException("missing classpath resource platform-gateway/" + rel);
                    }
                    Files.copy(in, target);
                }
            }
            return dir;
        } catch (IOException e) {
            deleteRecursively(dir);
            throw new UncheckedIOException("Failed to materialize platform-gateway dist", e);
        }
    }

    private static ClassLoader cl() {
        ClassLoader c = Thread.currentThread().getContextClassLoader();
        return c != null ? c : DynamicPlatformGatewayContainer.class.getClassLoader();
    }

    private static void deleteRecursively(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    logger.debug("best-effort cleanup could not delete " + p + ": " + e.getMessage());
                }
            });
        } catch (IOException e) {
            logger.debug("best-effort cleanup of " + dir + " failed: " + e.getMessage());
        }
    }
}
