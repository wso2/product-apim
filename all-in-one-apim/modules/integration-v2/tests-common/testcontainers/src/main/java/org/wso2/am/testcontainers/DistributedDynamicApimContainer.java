/*
 * Copyright (c) 2026, WSO2 LLC. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.wso2.am.testcontainers;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.wso2.am.integration.test.utils.Constants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Composite distributed APIM runtime. It deliberately presents the same
 * block-facing surface as {@link DynamicApimContainer}; callers do not need
 * to know that management, throttling, gateway traffic, and persistence are
 * separate containers.
 *
 * <p>Startup is ordered MySQL, CP, TM, Gateway. Teardown is the exact reverse
 * and is failure-safe so a component boot failure cannot leave the network or
 * database behind.</p>
 */
public class DistributedDynamicApimContainer implements ApimRuntime {

    private static final String CP_IMAGE = requiredImage("distributed.apim.cp.image.name");
    private static final String TM_IMAGE = requiredImage("distributed.apim.tm.image.name");
    private static final String GATEWAY_IMAGE = requiredImage("distributed.apim.gateway.image.name");
    private static final String SERVER_HOME = "/opt/wso2";
    private static final String TOML_PATH = SERVER_HOME + "/repository/conf/deployment.toml";
    private static final String DEFAULTS_DIRECTORY = "distributed-apim/defaults";
    private static final String DEFAULTS_DIRECTORY_PROPERTY = "distributed.apim.defaults.directory";

    private final String label;
    private final Path cpDefaults;
    private final Path tmDefaults;
    private final Path gatewayDefaults;
    private final Path sharedDatabaseSchema;
    private final Path apiManagerDatabaseSchema;
    private final Network network;
    private final boolean ownsNetwork;
    private final DistributedMySqlContainer mysql;
    private GenericContainer<?> cp;
    private GenericContainer<?> tm;
    private GenericContainer<?> gateway;
    private final Map<DistributedApimTomlBuilder.Component, String> extraOverlays =
            new EnumMap<>(DistributedApimTomlBuilder.Component.class);
    private final Map<DistributedApimTomlBuilder.Component, String> extraFiles =
            new EnumMap<>(DistributedApimTomlBuilder.Component.class);
    private boolean externalKmTrust;
    private boolean externalIsNotificationAlias;
    private boolean solaceJwksAlias;
    private boolean started;

    private static String requiredImage(String property) {
        String image = System.getProperty(property);
        if (image == null || image.isBlank()) {
            throw new IllegalStateException(property
                    + " is not set; resolve the image from the relevant product POM");
        }
        return image;
    }

    public DistributedDynamicApimContainer(String label) throws IOException {
        this(label, defaultPath("cp/deployment.toml"), defaultPath("tm/deployment.toml"),
                defaultPath("gateway/deployment.toml"), defaultPath("cp/mysql.sql"),
                defaultPath("cp/apimgt-mysql.sql"));
    }

    public DistributedDynamicApimContainer(String label, Network network) throws IOException {
        this(label, network, defaultPath("cp/deployment.toml"), defaultPath("tm/deployment.toml"),
                defaultPath("gateway/deployment.toml"), defaultPath("cp/mysql.sql"),
                defaultPath("cp/apimgt-mysql.sql"));
    }

    public DistributedDynamicApimContainer(String label, Path cpDefaults, Path tmDefaults, Path gatewayDefaults)
            throws IOException {
        this(label, cpDefaults, tmDefaults, gatewayDefaults, defaultPath("cp/mysql.sql"),
                defaultPath("cp/apimgt-mysql.sql"), Network.newNetwork(), true);
    }

    public DistributedDynamicApimContainer(String label, Network network, Path cpDefaults, Path tmDefaults,
                                            Path gatewayDefaults)
            throws IOException {
        this(label, cpDefaults, tmDefaults, gatewayDefaults, defaultPath("cp/mysql.sql"),
                defaultPath("cp/apimgt-mysql.sql"), network, false);
    }

    private DistributedDynamicApimContainer(String label, Path cpDefaults, Path tmDefaults, Path gatewayDefaults,
                                             Path sharedDatabaseSchema, Path apiManagerDatabaseSchema)
            throws IOException {
        this(label, cpDefaults, tmDefaults, gatewayDefaults, sharedDatabaseSchema, apiManagerDatabaseSchema,
                Network.newNetwork(), true);
    }

    private DistributedDynamicApimContainer(String label, Network network, Path cpDefaults, Path tmDefaults,
                                             Path gatewayDefaults, Path sharedDatabaseSchema,
                                             Path apiManagerDatabaseSchema) throws IOException {
        this(label, cpDefaults, tmDefaults, gatewayDefaults, sharedDatabaseSchema, apiManagerDatabaseSchema,
                network, false);
    }

    private DistributedDynamicApimContainer(String label, Path cpDefaults, Path tmDefaults, Path gatewayDefaults,
                                             Path sharedDatabaseSchema, Path apiManagerDatabaseSchema,
                                             Network network, boolean ownsNetwork) throws IOException {
        this.label = Objects.requireNonNull(label, "label");
        this.cpDefaults = requireFile(cpDefaults, "CP deployment.toml");
        this.tmDefaults = requireFile(tmDefaults, "TM deployment.toml");
        this.gatewayDefaults = requireFile(gatewayDefaults, "Gateway deployment.toml");
        this.sharedDatabaseSchema = requireFile(sharedDatabaseSchema, "shared database schema");
        this.apiManagerDatabaseSchema = requireFile(apiManagerDatabaseSchema, "API Manager database schema");
        this.network = Objects.requireNonNull(network, "network");
        this.ownsNetwork = ownsNetwork;

        this.mysql = new DistributedMySqlContainer(network)
                .withSchema(DistributedMySqlContainer.SHARED_DATABASE,
                        Files.readString(this.sharedDatabaseSchema))
                .withSchema(DistributedMySqlContainer.APIM_DATABASE,
                        Files.readString(this.apiManagerDatabaseSchema));
    }

    /** Add a small overlay for one component; it is merged after the distributed base. */
    public DistributedDynamicApimContainer withTomlExtraOverlay(
            DistributedApimTomlBuilder.Component component, String content) {
        extraOverlays.put(Objects.requireNonNull(component, "component"), content);
        return this;
    }

    /** Add a boot-time file to one component. The legacy interface targets CP. */
    public DistributedDynamicApimContainer withComponentServerFile(
            DistributedApimTomlBuilder.Component component, String hostPath, String relativePath) {
        Objects.requireNonNull(component, "component");
        if (hostPath == null || relativePath == null || hostPath.isBlank() || relativePath.isBlank()) {
            throw new IllegalArgumentException("Component server file source and destination are required");
        }
        try {
            String encoded = hostPath + "::" + relativePath;
            extraFiles.merge(component, encoded, (oldValue, newValue) -> oldValue + "," + newValue);
            return this;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid component server file", e);
        }
    }

    @Override
    public void start() {
        try {
            createComponents();
            mysql.start();
            cp.start();
            tm.start();
            gateway.start();
            started = true;
        } catch (Throwable failure) {
            stop();
            throw new IllegalStateException("Distributed APIM startup failed for block '" + label + "'", failure);
        }
    }

    @Override
    public void stop() {
        stopQuietly(gateway);
        stopQuietly(tm);
        stopQuietly(cp);
        stopQuietly(mysql);
        if (ownsNetwork) {
            try {
                network.close();
            } catch (Throwable ignored) {
                // Teardown must not hide the original component failure.
            }
        }
        started = false;
    }

    public boolean isStarted() {
        return started;
    }

    @Override
    public String getServletHttpsUrl() {
        return url(cp, Constants.HTTPS_PORT, "https");
    }

    @Override
    public String getServletHttpUrl() {
        return url(cp, Constants.HTTP_PORT, "http");
    }

    @Override
    public String getBackendOAuthTokenUrl() {
        // The endpoint-security token request originates in the Gateway container. Use CP's network alias
        // rather than CP's host-mapped port, because localhost from the Gateway resolves to the Gateway.
        return "https://apim-cp:9443/oauth2/token";
    }

    @Override
    public String getGatewayHttpsUrl() {
        return url(gateway, Constants.GATEWAY_HTTPS_PORT, "https");
    }

    @Override
    public String getGatewayManagementHttpsUrl() {
        return url(gateway, Constants.HTTPS_PORT, "https");
    }

    @Override
    public String getGatewayHttpUrl() {
        return url(gateway, Constants.GATEWAY_HTTP_PORT, "http");
    }

    @Override
    public String getGatewayWsUrl() {
        return url(gateway, Constants.GATEWAY_WS_PORT, "ws");
    }

    @Override
    public String getGatewayWssUrl() {
        return url(gateway, Constants.GATEWAY_WSS_PORT, "wss");
    }

    @Override
    public String getWebSubEventReceiverUrl() {
        return url(gateway, Constants.WEBSUB_EVENT_RECEIVER_PORT, "http");
    }

    @Override
    public String getGatewayClientIp() {
        for (com.github.dockerjava.api.model.ContainerNetwork value
                : cp.getContainerInfo().getNetworkSettings().getNetworks().values()) {
            if (value.getGateway() != null && !value.getGateway().isBlank()) {
                return value.getGateway();
            }
        }
        throw new IllegalStateException("Could not determine distributed APIM network gateway IP");
    }

    @Override
    public String getContainerTomlPath() {
        return TOML_PATH;
    }

    @Override
    public DistributedDynamicApimContainer withServerFile(String hostPath, String serverRelativePath) {
        return withComponentServerFile(DistributedApimTomlBuilder.Component.CP, hostPath, serverRelativePath);
    }

    @Override
    public DistributedDynamicApimContainer withCoverage() {
        return this;
    }

    @Override
    public DistributedDynamicApimContainer withExternalKmTrust() {
        externalKmTrust = true;
        return this;
    }

    @Override
    public DistributedDynamicApimContainer withExternalIsNotificationAlias() {
        externalIsNotificationAlias = true;
        return this;
    }

    @Override
    public DistributedDynamicApimContainer withSolaceJwksAlias() {
        solaceJwksAlias = true;
        return this;
    }

    @Override
    public String getCoverageDumpHost() {
        return cp.getHost();
    }

    @Override
    public int getCoverageDumpPort() {
        throw new UnsupportedOperationException("Distributed JaCoCo wiring is not available until Phase 9");
    }

    @Override
    public void createSecondaryUserStoreH2Schema(String dbRelativePath)
            throws IOException, InterruptedException {
        String runScript = "cd " + SERVER_HOME + " && java -cp \"$(ls repository/components/plugins/h2-engine_*.jar)\" "
                + "org.h2.tools.RunScript -url 'jdbc:h2:./" + dbRelativePath
                + "' -user wso2carbon -password wso2carbon -script dbscripts/h2.sql";
        Container.ExecResult result = cp.execInContainer("bash", "-c", runScript);
        if (result.getExitCode() != 0) {
            throw new IOException("Distributed CP secondary H2 schema creation failed: " + result.getStderr());
        }
    }

    @Override
    public String getContainerLog4j2Path() {
        return SERVER_HOME + "/repository/conf/log4j2.properties";
    }

    @Override
    public String getContainerLogFilePath(String fileName) {
        return SERVER_HOME + "/repository/logs/" + fileName;
    }

    @Override
    public String readGatewayLogFile(String fileName) {
        String path = SERVER_HOME + "/repository/logs/" + fileName;
        try {
            return gateway.copyFileFromContainer(path,
                    stream -> new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read distributed Gateway file: " + path, e);
        }
    }

    @Override
    public String readContainerFile(String containerPath) {
        try {
            return cp.copyFileFromContainer(containerPath,
                    stream -> new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read distributed CP file: " + containerPath, e);
        }
    }

    @Override
    public void writeContainerFile(String containerPath, String content) {
        cp.copyFileToContainer(Transferable.of(content.getBytes(StandardCharsets.UTF_8), 0666), containerPath);
    }

    @Override
    public Container.ExecResult execInContainer(String... command) throws IOException, InterruptedException {
        return cp.execInContainer(command);
    }

    @Override
    public String getContainerId() {
        return cp.getContainerId();
    }

    public String getControlPlaneContainerId() {
        return cp.getContainerId();
    }

    public String getTrafficManagerContainerId() {
        return tm.getContainerId();
    }

    public String getGatewayContainerId() {
        return gateway.getContainerId();
    }

    public String getDatabaseHostJdbcUrl(String database) {
        return mysql.getHostJdbcUrl(database);
    }

    private GenericContainer<?> component(String image, String alias, String toml, int portOffset, int... ports) {
        Integer[] exposedPorts = new Integer[ports.length];
        for (int i = 0; i < ports.length; i++) {
            exposedPorts[i] = ports[i];
        }
        GenericContainer<?> container = new GenericContainer<>(image)
                .withNetwork(network)
                .withNetworkAliases(alias)
                .withExposedPorts(exposedPorts)
                .withCopyToContainer(Transferable.of(toml, 0666), TOML_PATH)
                .withCommand("-DportOffset=" + portOffset)
                .waitingFor(Wait.forLogMessage(".*WSO2 Carbon started in.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(20)));
        return container;
    }

    private void createComponents() throws IOException {
        String cpToml = buildToml(cpDefaults, DistributedApimTomlBuilder.Component.CP, "cp-base-overlay.toml");
        String tmToml = buildToml(tmDefaults, DistributedApimTomlBuilder.Component.TM, "tm-base-overlay.toml");
        String gatewayToml = buildToml(gatewayDefaults, DistributedApimTomlBuilder.Component.GATEWAY,
                "gateway-base-overlay.toml");
        cp = component(CP_IMAGE, "apim-cp", cpToml, 0, 9443, 9763, 5672);
        tm = component(TM_IMAGE, "apim-tm", tmToml, 0, 9443, 5672, 9611, 9711);
        gateway = component(GATEWAY_IMAGE, "apim-gw", gatewayToml, 0, 9443,
                Constants.GATEWAY_HTTPS_PORT, Constants.GATEWAY_HTTP_PORT, Constants.GATEWAY_WS_PORT,
                Constants.GATEWAY_WSS_PORT, Constants.WEBSUB_EVENT_RECEIVER_PORT);
        if (externalKmTrust) {
            String configured = System.getProperty("apim.km.truststore.path");
            String path = configured == null || configured.isBlank()
                    ? System.getProperty("module.dir", ".") + "/target/is7/client-truststore.jks" : configured;
            copyToCp(path, SERVER_HOME + "/repository/resources/security/client-truststore.jks");
            copyClasspathToCp("is7/wso2am.p12", SERVER_HOME + "/repository/resources/security/wso2am.p12");
        }
        if (externalIsNotificationAlias) {
            cp.withNetworkAliases("wso2am");
        }
        if (solaceJwksAlias) {
            cp.withNetworkAliases(DynamicSolaceBroker.APIM_JWKS_ALIAS);
        }
        copyComponentFiles(DistributedApimTomlBuilder.Component.CP, cp);
        copyComponentFiles(DistributedApimTomlBuilder.Component.TM, tm);
        copyComponentFiles(DistributedApimTomlBuilder.Component.GATEWAY, gateway);
    }

    private void copyComponentFiles(DistributedApimTomlBuilder.Component component,
                                    GenericContainer<?> container) {
        String encoded = extraFiles.get(component);
        if (encoded == null) {
            return;
        }
        for (String entry : encoded.split(",")) {
            String[] parts = entry.split("::", 2);
            try {
                container.withCopyToContainer(Transferable.of(Files.readAllBytes(Path.of(parts[0])), 0666),
                        SERVER_HOME + "/" + parts[1]);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to stage distributed component file: " + parts[0], e);
            }
        }
    }

    private String buildToml(Path defaultsPath, DistributedApimTomlBuilder.Component component, String resource)
            throws IOException {
        String defaults = Files.readString(defaultsPath);
        String baseOverlay;
        try (var input = getClass().getClassLoader().getResourceAsStream("distributed-apim/" + resource)) {
            if (input == null) {
                throw new IOException("Missing distributed overlay resource: " + resource);
            }
            baseOverlay = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        return DistributedApimTomlBuilder.build(defaults, baseOverlay, null,
                extraOverlays.get(component), new HashMap<>());
    }

    private void copyToCp(String source, String target) {
        try {
            Path path = Path.of(source);
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("Distributed CP file does not exist: " + path);
            }
            cp.withCopyToContainer(Transferable.of(Files.readAllBytes(path), 0666), target);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to stage distributed CP file: " + source, e);
        }
    }

    private void copyClasspathToCp(String resource, String target) {
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Classpath resource not found: " + resource);
            }
            cp.withCopyToContainer(Transferable.of(input.readAllBytes(), 0666), target);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to stage classpath file: " + resource, e);
        }
    }

    private static String url(GenericContainer<?> container, int port, String scheme) {
        return scheme + "://" + container.getHost() + ":" + container.getMappedPort(port) + "/";
    }

    private static void stopQuietly(GenericContainer<?> container) {
        try {
            container.stop();
        } catch (Throwable ignored) {
            // Continue stopping the remaining children.
        }
    }

    private static void stopQuietly(DistributedMySqlContainer container) {
        try {
            container.stop();
        } catch (Throwable ignored) {
            // Continue teardown.
        }
    }

    static Path defaultPath(String relativePath) {
        String configuredDirectory = System.getProperty(DEFAULTS_DIRECTORY_PROPERTY);
        if (configuredDirectory != null && !configuredDirectory.isBlank()) {
            return Path.of(configuredDirectory).resolve(relativePath);
        }

        Path[] starts = {
                Path.of(System.getProperty("module.dir", ".")),
                Path.of(System.getProperty("user.dir", "."))
        };
        for (Path start : starts) {
            Path current = start.toAbsolutePath().normalize();
            while (current != null) {
                Path[] candidates = {
                        current.resolve("tests-common/testcontainers/target").resolve(DEFAULTS_DIRECTORY),
                        current.resolve("target").resolve(DEFAULTS_DIRECTORY)
                };
                for (Path candidate : candidates) {
                    Path resolved = candidate.resolve(relativePath);
                    if (Files.isRegularFile(resolved)) {
                        return resolved;
                    }
                }
                current = current.getParent();
            }
        }

        return starts[0].toAbsolutePath().normalize()
                .resolve("tests-common/testcontainers/target")
                .resolve(DEFAULTS_DIRECTORY).resolve(relativePath);
    }

    private static Path requireFile(Path path, String description) {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Distributed image fixture " + description
                    + " does not exist: " + path);
        }
        return path;
    }
}
