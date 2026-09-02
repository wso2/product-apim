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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;
import org.wso2.am.integration.test.utils.Constants;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

/**
 * APIM container variant for the parallel-on-shared lane. Unlike {@link APIMContainer}, this
 * runs with {@code portOffset=0} and exposes the canonical ports via {@code withExposedPorts},
 * letting Docker map each to an ephemeral host port. Callers resolve the host port through the
 * {@code get*Url()} accessors. Because every container has its own network namespace, no offset
 * counter or per-container DB renaming is needed.
 */
public class DynamicApimContainer extends GenericContainer<DynamicApimContainer> implements ApimRuntime {

    private static final Logger logger = LoggerFactory.getLogger(DynamicApimContainer.class);
    private static final String DEFAULT_APIM_IMAGE = "wso2am:4.7.0-SNAPSHOT-jdk21";
    /** Fixed shared-network alias for the IS→APIM reverse channel; see {@link #withExternalIsNotificationAlias}. */
    private static final String APIM_NETWORK_ALIAS = "wso2am";

    public DynamicApimContainer(String containerLabel, String deploymentTomlContent) {

        super(System.getProperty("apim.docker.image.name", DEFAULT_APIM_IMAGE));

        String apimDbUrl = System.getenv(Constants.API_MANAGER_DATABASE_URL).replace("&", "&amp;");
        String sharedDbUrl = System.getenv(Constants.SHARED_DATABASE_URL).replace("&", "&amp;");

        logger.info("APIM DB URL: {}", apimDbUrl);
        logger.info("SHARED DB URL: {}", sharedDbUrl);

        // Expose canonical ports; Docker assigns ephemeral host ports resolved via getMappedPort.
        // GATEWAY_WS_PORT (9099) is the gateway WebSocket inbound; GATEWAY_WSS_PORT (8099) is the SECURE WebSocket
        // inbound — both needed by WebSocket-API invocation tests (ws:// and wss://).
        // WEBSUB_EVENT_RECEIVER_PORT (9021) is the WebSub event-receiver inbound (synapse WebhookServer) an event
        // source POSTs content to — needed by WebSub-API invocation tests, which publish from the test JVM.
        // Note every port listed here also joins the startup liveness set (Wait.forListeningPort waits on ALL
        // exposed ports), so only list ports the server always binds — 9021 does, unconditionally, at boot.
        withExposedPorts(Constants.HTTPS_PORT, Constants.HTTP_PORT,
                Constants.GATEWAY_HTTPS_PORT, Constants.GATEWAY_HTTP_PORT, Constants.GATEWAY_WS_PORT,
                Constants.GATEWAY_WSS_PORT, Constants.WEBSUB_EVENT_RECEIVER_PORT);

        // Env vars for APIMGT_DB
        withEnv(Constants.API_MANAGER_DATABASE_TYPE, System.getenv(Constants.API_MANAGER_DATABASE_TYPE));
        withEnv(Constants.API_MANAGER_DATABASE_DRIVER, System.getenv(Constants.API_MANAGER_DATABASE_DRIVER));
        withEnv(Constants.API_MANAGER_DATABASE_URL, apimDbUrl);
        withEnv(Constants.API_MANAGER_DATABASE_USERNAME, System.getenv(Constants.API_MANAGER_DATABASE_USERNAME));
        withEnv(Constants.API_MANAGER_DATABASE_PASSWORD, System.getenv(Constants.API_MANAGER_DATABASE_PASSWORD));
        withEnv(Constants.API_MANAGER_DATABASE_VALIDATION_QUERY, System.getenv(Constants.
                API_MANAGER_DATABASE_VALIDATION_QUERY));

        // Env vars for SHARED_DB
        withEnv(Constants.SHARED_DATABASE_TYPE, System.getenv(Constants.SHARED_DATABASE_TYPE));
        withEnv(Constants.SHARED_DATABASE_DRIVER, System.getenv(Constants.SHARED_DATABASE_DRIVER));
        withEnv(Constants.SHARED_DATABASE_URL, sharedDbUrl);
        withEnv(Constants.SHARED_DATABASE_USERNAME, System.getenv(Constants.SHARED_DATABASE_USERNAME));
        withEnv(Constants.SHARED_DATABASE_PASSWORD, System.getenv(Constants.SHARED_DATABASE_PASSWORD));
        withEnv(Constants.SHARED_DATABASE_VALIDATION_QUERY, System.getenv(Constants.
                SHARED_DATABASE_VALIDATION_QUERY));

        // Add host.docker.internal mapping for Linux compatibility (needed for accessing host services)
        withExtraHost("host.docker.internal", "host-gateway");

        // The docker network is assigned by the CALLER: BlockLifecycleListener creates one private network per
        // block and joins APIM plus that block's IS/Solace to it, so the wso2am / wso2is / apimforsolace aliases
        // are network-scoped and cannot collide across concurrent blocks. Direct constructions (the
        // framework-verification boot probes) run standalone on the default bridge — they have no peers to resolve.
        // Copy the modified deployment.toml to the container
        withCopyToContainer(Transferable.of(deploymentTomlContent), getContainerTomlPath());

        if (Boolean.parseBoolean(System.getProperty(Constants.APIM_DEBUG_ENABLED))) {
            String debugPortStr = System.getProperty(Constants.APIM_DEBUG_PORT);
            int debugPort;
            try {
                debugPort = Integer.parseInt(debugPortStr);
            } catch (NumberFormatException ignored) {
                debugPort = Constants.DEFAULT_APIM_DEBUG_PORT;
                if (!debugPortStr.contains(String.valueOf(debugPort))) {
                    throw new RuntimeException("Invalid APIM_DEBUG_PORT. APIM debug port must be " + debugPort);
                }
            }
            logger.info("Debugging enabled for the API Manager Instance on port {}", debugPortStr);
            // Debug uses a fixed host port so the IDE can attach to a known address.
            addFixedExposedPort(debugPort, debugPort);
            withCommand("-DportOffset=0", "-debug", debugPortStr);
        } else {
            withCommand("-DportOffset=0");
        }

        String testName = MDC.get("testName") != null ? MDC.get("testName") : "default";
        // send container logs with MDC fields
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(logger)
                .withPrefix(containerLabel)
                .withSeparateOutputStreams()
                .withMdc("testName", testName);

        withLogConsumer(logConsumer);
        waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(20)));
    }

    /**
     * Enables JaCoCo integration coverage (opt-in; the caller decides when to attach it). Copies the JaCoCo
     * runtime agent into the container and attaches it to the SERVER JVM ONLY, via the {@code JVM_MEM_OPTS}
     * env var. Exposes the agent's tcpserver port so counters can be dumped over a mapped host port at
     * teardown (see {@link JacocoCoverage}). Must be called before {@link #start()}.
     *
     * <p>Getting the agent onto ONLY the server JVM is essential. {@code JAVA_TOOL_OPTIONS} cannot be used: the
     * JVM honours it for EVERY process, so the helper JVMs {@code bin/api-manager.sh} spawns — and especially
     * the {@code diagnostics-tool} JVM it launches in the BACKGROUND, concurrently with the server — also load
     * the agent, race the server to bind its tcpserver port 6300, hit {@code BindException: Address already in
     * use}, and abort with {@code FATAL ERROR ... processing of -javaagent failed}. That non-deterministic race
     * intermittently kills the boot-critical JVM and fails the block boot. {@code JAVA_OPTS} does not work
     * either — {@code diagnostics.sh} also references {@code $JAVA_OPTS}. We therefore use {@code JVM_MEM_OPTS}:
     * {@code api-manager.sh} applies it (from the env) to the main Carbon launch line only, while
     * {@code diagnostics.sh} overwrites it with its own {@code -Xms128m -Xmx256m} and the other helper JVMs
     * never reference it — so the agent reaches the single server JVM and there is no port race. (A
     * {@code -javaagent} passed as a container command arg does NOT attach at all: api-manager.sh places command
     * args after the {@code Bootstrap} main class, i.e. as program args, not JVM options.)
     */
    public DynamicApimContainer withCoverage() {
        try {
            File agentJar = JacocoCoverage.extractAgentJar();
            withCopyToContainer(MountableFile.forHostPath(agentJar.getAbsolutePath()),
                    JacocoCoverage.CONTAINER_AGENT_PATH);
        } catch (IOException e) {
            throw new RuntimeException("Failed to stage JaCoCo agent jar", e);
        }
        // Attach the agent to the SERVER JVM ONLY via JVM_MEM_OPTS. Preserve api-manager.sh's own default heap
        // (-Xms256m -Xmx1024m) and append the agent, so behaviour is unchanged except for the added agent. The
        // background diagnostics-tool JVM overwrites JVM_MEM_OPTS with its own value, so it never sees the agent
        // → no tcpserver port-6300 race (the cause of the intermittent block-boot failures).
        withEnv("JVM_MEM_OPTS", "-Xms256m -Xmx1024m " + JacocoCoverage.containerAgentVmArg());
        // addExposedPort (not withExposedPorts) so the canonical APIM ports set in the constructor are kept.
        addExposedPort(JacocoCoverage.TCP_PORT);
        logger.info("JaCoCo coverage enabled (server-JVM-only via JVM_MEM_OPTS): agent at {}, tcpserver port {}",
                JacocoCoverage.CONTAINER_AGENT_PATH, JacocoCoverage.TCP_PORT);
        return this;
    }

    /**
     * Augments the container's client-truststore so APIM trusts the external Identity Server's TLS cert
     * (CN=wso2is), enabling the KM HTTP client's JWKS fetch / introspection over {@code https://wso2is:9443}.
     * Must be called before {@link #start()}: the truststore is read once when the Carbon JVM boots, so the
     * augmented file must already be in place — the only pre-boot injection point is {@code withCopyToContainer},
     * hence we copy a fully-augmented truststore over the default rather than importing into the running server.
     *
     * <p>The augmented truststore is produced at build time by the testcontainers module's
     * {@code build-augmented-truststore} exec (base truststore extracted from the APIM image + the fixed
     * {@code wso2is} public cert imported), staged at {@code <module.dir>/target/is7/client-truststore.jks};
     * overridable via {@code -Dapim.km.truststore.path}. Only the external-KM block calls this, so the default
     * lane's truststore is untouched.
     */
    public DynamicApimContainer withExternalKmTrust() {
        String configured = System.getProperty("apim.km.truststore.path");
        String path = (configured != null && !configured.isBlank())
                ? configured
                : System.getProperty("module.dir", ".") + "/target/is7/client-truststore.jks";
        File truststore = new File(path);
        if (!truststore.isFile()) {
            throw new IllegalStateException("Augmented APIM client-truststore not found at '" + path
                    + "'. It is built by the testcontainers pre-integration-test exec "
                    + "(build-augmented-truststore); run the build so it is staged, or set "
                    + "-Dapim.km.truststore.path to its location.");
        }
        withCopyToContainer(MountableFile.forHostPath(truststore.getAbsolutePath()),
                Constants.APIM_CONTAINER_USER_HOME + "/" + requireServerName()
                        + "/repository/resources/security/client-truststore.jks");
        // Present a TLS cert valid for the 'wso2am' network alias (fixed keystore, CN=wso2am,
        // SAN dns:localhost,dns:wso2am) so IS's token-revocation notification POST to https://wso2am:9443
        // passes strict hostname verification - no AllowAll workaround needed on IS. The block's toml overlay
        // points [keystore.tls] at this file; the augmented client-truststore above also trusts it (loopback).
        withCopyToContainer(MountableFile.forClasspathResource("is7/wso2am.p12"),
                Constants.APIM_CONTAINER_USER_HOME + "/" + requireServerName()
                        + "/repository/resources/security/wso2am.p12");
        logger.info("External-KM mode: APIM client-truststore augmented (wso2is + wso2am certs) from {}, "
                + "TLS keystore set to wso2am.p12", truststore.getAbsolutePath());
        return this;
    }

    /**
     * Binds the fixed {@code wso2am} shared-network alias so the external IS can reach this container on its
     * reverse channel — IS's baked {@code notification_endpoint} POSTs to
     * {@code https://wso2am:9443/internal/data/v1/notify} (token-revocation events in the default IS overlay,
     * tenant-sync events in the SSO overlay). Without a live alias holder the IS EventSender fails with
     * {@code UnknownHostException: wso2am} (harmless unless a test asserts on the delivered notification).
     *
     * <p>Deliberately separate from {@link #withExternalKmTrust()}: the alias name is fixed (baked into the IS
     * toml and the wso2am.p12 cert), so at most one container may hold it PER NETWORK. Since every block runs on
     * its own private network, that is satisfied by construction and any number of blocks may bind it
     * concurrently — which is why the JVM-wide permit this used to require is gone. Most external-KM blocks only
     * make APIM→IS calls and never consume the reverse channel, so they skip it; only the block whose tests
     * assert on IS→APIM notifications binds it (via the {@code receiveExternalIsNotifications} block param).
     */
    public DynamicApimContainer withExternalIsNotificationAlias() {
        withNetworkAliases(APIM_NETWORK_ALIAS);
        logger.info("APIM network alias set to '{}' (IS reverse-channel notification receiver)",
                APIM_NETWORK_ALIAS);
        return this;
    }

    /**
     * Binds the alias the SOLACE BROKER resolves to reach this APIM, so it can fetch APIM's JWKS and validate
     * an APIM-issued access token itself. Without it the broker has no host to resolve and every publish is
     * rejected 403 — with the reason visible ONLY inside the broker's own event.log, which is exactly how it
     * cost a whole suite run to find.
     *
     * <p>APIM has NO network alias by default (nothing normally calls IN to it), and this deliberately does not
     * reuse the {@code wso2am} notification alias: that one is baked into the IS toml and the wso2am.p12 cert,
     * so binding it here would let a Solace block receive — and steal — an external-IS block's notifications if
     * the two ever shared a network. A separate name keeps the two channels independent regardless.
     *
     * <p>No permit is needed. This alias, the block's Solace broker and the block's APIM all live on the SAME
     * private per-block network, so the broker's single {@code endpointJwks} resolves to exactly one APIM by
     * construction. (On the old shared network each opting-in block bound this same name on one network, so the
     * listener had to serialize those blocks on a JVM-wide permit; per-block networks removed that.)
     */
    public DynamicApimContainer withSolaceJwksAlias() {
        withNetworkAliases(DynamicSolaceBroker.APIM_JWKS_ALIAS);
        logger.info("APIM network alias set to '{}' (Solace broker -> APIM JWKS)",
                DynamicSolaceBroker.APIM_JWKS_ALIAS);
        return this;
    }

    /** Host for dumping coverage (valid after start). */
    public String getCoverageDumpHost() {
        return getHost();
    }

    /** Ephemeral host port mapped to the in-container JaCoCo agent tcpserver (valid after start). */
    public int getCoverageDumpPort() {
        return getMappedPort(JacocoCoverage.TCP_PORT);
    }

    public String getServletHttpsUrl() {
        return String.format("https://%s:%d/", getHost(), getMappedPort(Constants.HTTPS_PORT));
    }

    public String getServletHttpUrl() {
        return String.format("http://%s:%d/", getHost(), getMappedPort(Constants.HTTP_PORT));
    }

    @Override
    public String getBackendOAuthTokenUrl() {
        return getServletHttpsUrl() + "oauth2/token";
    }

    public String getGatewayHttpsUrl() {
        return String.format("https://%s:%d/", getHost(), getMappedPort(Constants.GATEWAY_HTTPS_PORT));
    }

    @Override
    public String getGatewayManagementHttpsUrl() {
        return getServletHttpsUrl();
    }

    public String getGatewayHttpUrl() {
        return String.format("http://%s:%d/", getHost(), getMappedPort(Constants.GATEWAY_HTTP_PORT));
    }

    /** Gateway WebSocket base URL (ws://host:mappedWsPort/) for WebSocket-API invocation. */
    public String getGatewayWsUrl() {
        return String.format("ws://%s:%d/", getHost(), getMappedPort(Constants.GATEWAY_WS_PORT));
    }

    /** Gateway SECURE WebSocket base URL (wss://host:mappedWssPort/) for wss:// WebSocket-API invocation. */
    public String getGatewayWssUrl() {
        return String.format("wss://%s:%d/", getHost(), getMappedPort(Constants.GATEWAY_WSS_PORT));
    }

    /**
     * WebSub event-receiver base URL (http://host:mappedWebhookPort/) — where an event source POSTs content for a
     * WebSub API's hub to fan out. Append the API context/version and
     * {@link Constants#WEBSUB_EVENT_RECEIVER_RESOURCE}.
     */
    public String getWebSubEventReceiverUrl() {
        return String.format("http://%s:%d/", getHost(), getMappedPort(Constants.WEBSUB_EVENT_RECEIVER_PORT));
    }

    /**
     * The container's docker-network GATEWAY IP — the source IP the gateway observes for a host→published-port
     * connection (verified: a WS client on the host is seen by APIM as this address). Used to pin an api-key
     * {@code permittedIP} to the test client's effective IP so the api-key IP-restriction POSITIVE case can be
     * asserted (matching IP → allowed). Read from the live container inspect, so it adapts per environment.
     */
    public String getGatewayClientIp() {
        var networks = getContainerInfo().getNetworkSettings().getNetworks();
        for (com.github.dockerjava.api.model.ContainerNetwork net : networks.values()) {
            if (net.getGateway() != null && !net.getGateway().isEmpty()) {
                return net.getGateway();
            }
        }
        throw new IllegalStateException("Could not determine the container network gateway IP");
    }

    public String getContainerTomlPath() {
        return Constants.APIM_CONTAINER_USER_HOME + "/" + requireServerName() +
                Constants.DEPLOYMENT_TOML_PATH;
    }

    /** Explicit contract bridge; Testcontainers supplies this method through {@code ContainerState}. */
    @Override
    public Container.ExecResult execInContainer(String... command) throws IOException, InterruptedException {
        return super.execInContainer(command);
    }

    /**
     * Copies a host file into the server directory tree BEFORE the container starts, at
     * {@code <server-home>/<serverRelativePath>}. For fixtures the server only picks up at BOOT — e.g. a
     * secondary user-store definition under {@code repository/deployment/server/userstores/} (Carbon's
     * User Store Configuration Deployer reads that directory at startup; a config written into a running
     * container at runtime is not hot-deployed). Driven per test block via the {@code serverFilesToCopy}
     * listener parameter. Must be called before {@link #start()}.
     */
    public DynamicApimContainer withServerFile(String hostPath, String serverRelativePath) {
        String target = Constants.APIM_CONTAINER_USER_HOME + "/" + requireServerName()
                + "/" + serverRelativePath;
        // Explicit 0666: files copied into the container are owned by root while the server runs as
        // wso2carbon. A read-only file is fine for config the server only READS (a userstore XML), but a
        // data file the server must WRITE (e.g. an embedded H2 .mv.db) silently degrades - H2 opens a
        // non-writable file read-only, so store reads work while writes no-op with no server-side error.
        withCopyToContainer(MountableFile.forHostPath(hostPath, 0666), target);
        return this;
    }

    /**
     * Creates the usermgt (UM_*) schema for a secondary JDBC user store in a fresh embedded H2 DB, at runtime,
     * using the PRODUCT'S OWN shipped DDL ({@code dbscripts/h2.sql}) and the bundled H2 engine — the framework
     * owns zero DDL. This replaces the copied pre-seeded {@code .mv.db} (and its 0666 hack): the DB is created
     * live, then {@code chmod 0666} guarantees the {@code wso2carbon} server process can write it regardless of
     * which user the exec ran as. Must run BEFORE the store is registered (addUserStore), while nothing holds the
     * embedded-H2 file lock. {@code dbRelativePath} is the H2 url path, e.g. {@code repository/database/WSO2SEC_DB}.
     */
    public void createSecondaryUserStoreH2Schema(String dbRelativePath) throws IOException, InterruptedException {
        String home = Constants.APIM_CONTAINER_USER_HOME + "/" + requireServerName();
        String runScript = "cd " + home + " && java -cp \"$(ls repository/components/plugins/h2-engine_*.jar)\" "
                + "org.h2.tools.RunScript -url 'jdbc:h2:./" + dbRelativePath + "' "
                + "-user wso2carbon -password wso2carbon -script dbscripts/h2.sql";
        Container.ExecResult r = execInContainer("bash", "-c", runScript);
        if (r.getExitCode() != 0) {
            throw new IOException("Secondary user-store H2 schema creation (RunScript) failed, exit "
                    + r.getExitCode() + "\nstdout: " + r.getStdout() + "\nstderr: " + r.getStderr());
        }
        // execInContainer may create the file as root; the server runs as wso2carbon and must WRITE it. A failed
        // chmod must FAIL here: H2 silently opens a non-writable file READ-ONLY, so store writes would return 2xx
        // while no-opping (isExistingUser false after a successful-looking addUser) — a silent degradation far
        // harder to diagnose than a boot failure.
        Container.ExecResult chmod = execInContainer("bash", "-c",
                "chmod 0666 " + home + "/" + dbRelativePath + ".mv.db");
        if (chmod.getExitCode() != 0) {
            throw new IOException("Secondary user-store H2 DB chmod failed (the .mv.db would be read-only for "
                    + "wso2carbon and store writes would silently no-op), exit " + chmod.getExitCode()
                    + "\nstdout: " + chmod.getStdout() + "\nstderr: " + chmod.getStderr());
        }
        logger.info("Created secondary user-store H2 schema at {} (via product dbscripts/h2.sql)", dbRelativePath);
    }

    /**
     * The running server's directory name, validated. Mirrors the fail-fast in
     * {@code CoverageSupport#distributionZip}: an unset property otherwise resolves to a path containing
     * {@code null}, which surfaces much later as a generic "file not found" that hides the real cause.
     */
    private static String requireServerName() {
        String serverName = System.getProperty("apim.server.name"); // e.g. wso2am-4.7.0-SNAPSHOT
        if (serverName == null || serverName.isBlank()) {
            throw new IllegalStateException("apim.server.name is not set — required to resolve in-container "
                    + "paths (normally passed via the surefire systemPropertyVariables; pass "
                    + "-Dapim.server.name=<name> if running the suite outside the Maven config).");
        }
        return serverName;
    }

    /** In-container path of the running server's {@code log4j2.properties} (for remote-logging appender checks). */
    public String getContainerLog4j2Path() {
        return Constants.APIM_CONTAINER_USER_HOME + "/" + requireServerName()
                + "/repository/conf/log4j2.properties";
    }

    /**
     * In-container path of one of the running server's log files (for the logging assertions). Owned here with
     * {@link #getContainerLog4j2Path()} so the in-container layout is encoded in ONE module, not restated by
     * each caller.
     *
     * @param fileName log file name under {@code repository/logs}, e.g. {@code api.log}
     */
    public String getContainerLogFilePath(String fileName) {
        return Constants.APIM_CONTAINER_USER_HOME + "/" + requireServerName() + "/repository/logs/" + fileName;
    }

    @Override
    public String readGatewayLogFile(String fileName) {
        return readContainerFile(getContainerLogFilePath(fileName));
    }

    /**
     * Reads a file's content from inside the running container as UTF-8. Used by the remote-logging tests to
     * assert how the server rewrote {@code log4j2.properties} (e.g. an appender flipped to a SecuredHttp type).
     */
    public String readContainerFile(String containerPath) {
        try {
            return copyFileFromContainer(containerPath,
                    is -> new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read container file: " + containerPath, e);
        }
    }

    /**
     * Overwrites a file inside the RUNNING container with {@code content} (UTF-8). Used by the remote-logging
     * tests to seed a {@code log4j2.properties} fixture the server then rewrites (e.g. one whose AUDIT_LOGFILE
     * block is missing, to assert the config service creates it).
     *
     * <p>The mode is an explicit 0666 for the same reason as {@link #withServerFile}: a file copied in is
     * root-owned while the server runs as {@code wso2carbon}, so anything the server must WRITE — and the
     * logging service rewrites this file in place — has to be world-writable or the rewrite fails.</p>
     */
    public void writeContainerFile(String containerPath, String content) {
        copyFileToContainer(Transferable.of(content.getBytes(java.nio.charset.StandardCharsets.UTF_8), 0666),
                containerPath);
    }
}
