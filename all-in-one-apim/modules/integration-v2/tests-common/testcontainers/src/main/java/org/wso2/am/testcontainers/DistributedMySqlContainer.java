/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Block-owned MySQL resource for the distributed APIM topology.
 *
 * <p>The container intentionally exposes the database through its network alias
 * (rather than making callers depend on a host-mapped port). APIM components
 * can therefore use {@code mysql:3306} on the private block network while the
 * test JVM can use {@link #getJdbcUrl(String)} for diagnostics.</p>
 *
 * <p>Schema files are supplied by the runtime builder. This keeps this class
 * independent of a particular product ZIP and makes it possible for the
 * composite runtime to select the scripts belonging to the exact CP/TM build
 * under test.</p>
 */
public class DistributedMySqlContainer extends GenericContainer<DistributedMySqlContainer> {

    public static final String DEFAULT_IMAGE = "mysql:8.4.0-oraclelinux8";
    public static final String NETWORK_ALIAS = "mysql";
    public static final int MYSQL_PORT = 3306;
    public static final String ROOT_USER = "root";
    public static final String ROOT_PASSWORD = "root";
    public static final String DATABASE_USER = "wso2carbon";
    public static final String DATABASE_PASSWORD = "wso2carbon";
    public static final String APIM_DATABASE = "WSO2AM_DB";
    public static final String SHARED_DATABASE = "WSO2AM_SHARED_DB";

    private static final CpuLimit CPU_LIMIT = CpuLimit.of(0.5);
    private static final long MEMORY_LIMIT_BYTES = 384L * 1024 * 1024;
    private static final String SEED_MARKER_TABLE = "INTEGRATION_V2_SCHEMA_SEED";

    private final Map<String, Path> schemaFiles = new LinkedHashMap<>();
    private boolean initialized;
    private boolean registryLogMillisecondPrecision;
    private String blockLabel;

    public DistributedMySqlContainer(Network network) {
        this(DEFAULT_IMAGE, network);
    }

    public DistributedMySqlContainer(String image, Network network) {
        super(image);
        Objects.requireNonNull(network, "network");
        withNetwork(network);
        withNetworkAliases(NETWORK_ALIAS);
        withExposedPorts(MYSQL_PORT);
        withEnv("MYSQL_ROOT_PASSWORD", ROOT_PASSWORD);
        withEnv("MYSQL_USER", DATABASE_USER);
        withEnv("MYSQL_PASSWORD", DATABASE_PASSWORD);
        withCreateContainerCmdModifier(cmd -> {
            CPU_LIMIT.applyTo(cmd);
            cmd.getHostConfig().withMemory(MEMORY_LIMIT_BYTES)
                    .withMemorySwap(DistributedMemoryLimits.memoryAndSwapLimitBytes(MEMORY_LIMIT_BYTES));
        });
        // Keep buffers and instrumentation within the container's 384 MiB budget.
        withCommand("--performance-schema=OFF",
                "--innodb-buffer-pool-size=64M",
                "--innodb-log-buffer-size=16M",
                "--temptable-max-ram=16M",
                "--table-open-cache=512",
                "--table-definition-cache=512");
        // The image supplies a private anonymous disk volume at /var/lib/mysql.
        // Testcontainers removes that volume with the container, preserving block isolation.
        waitingFor(Wait.forLogMessage(".*ready for connections.*\\n", 2)
                .withStartupTimeout(Duration.ofMinutes(5)));
    }

    /** Register a product-provided SQL schema to be applied to a database. */
    public DistributedMySqlContainer withSchema(String database, Path schemaFile) {
        requireDatabaseName(database);
        Objects.requireNonNull(schemaFile, "schemaFile");
        if (!Files.isRegularFile(schemaFile)) {
            throw new IllegalArgumentException("Schema file does not exist: " + schemaFile);
        }
        schemaFiles.put(database, schemaFile);
        copySchemaToContainer(database, schemaFile);
        return this;
    }

    /** Convenience registration for generated or test-owned schema content. */
    public DistributedMySqlContainer withSchema(String database, String schema) {
        requireDatabaseName(database);
        Objects.requireNonNull(schema, "schema");
        try {
            Path file = Files.createTempFile("integration-v2-", ".sql");
            Files.writeString(file, schema, StandardCharsets.UTF_8);
            file.toFile().deleteOnExit();
            schemaFiles.put(database, file);
            copySchemaToContainer(database, file);
            return this;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to stage schema for " + database, e);
        }
    }

    public DistributedMySqlContainer withRegistryLogMillisecondPrecision(String label, boolean enabled) {
        blockLabel = Objects.requireNonNull(label, "label");
        registryLogMillisecondPrecision = enabled;
        return this;
    }

    @Override
    public void start() {
        super.start();
        try {
            initializeDatabases();
            RegistryLogTimestampPrecision.configure(registryLogMillisecondPrecision, "mysql", blockLabel,
                    SHARED_DATABASE, () -> DriverManager.getConnection(getHostJdbcUrl(SHARED_DATABASE),
                            DATABASE_USER, DATABASE_PASSWORD));
            initialized = true;
        } catch (Exception e) {
            stop();
            throw new IllegalStateException("Unable to initialize distributed APIM MySQL", e);
        }
    }

    @Override
    public void stop() {
        initialized = false;
        super.stop();
    }

    public String getJdbcUrl(String database) {
        requireDatabaseName(database);
        return "jdbc:mysql://" + NETWORK_ALIAS + ":" + MYSQL_PORT + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
    }

    public String getHostJdbcUrl(String database) {
        requireDatabaseName(database);
        return "jdbc:mysql://" + getHost() + ":" + getMappedPort(MYSQL_PORT) + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void initializeDatabases() throws Exception {
        waitForAuthenticatedMySql();
        execute("mysql", "-h127.0.0.1", "-uroot", "-p" + ROOT_PASSWORD,
                "-e", "CREATE DATABASE IF NOT EXISTS " + APIM_DATABASE + " CHARACTER SET latin1;"
                        + " CREATE DATABASE IF NOT EXISTS " + SHARED_DATABASE + " CHARACTER SET latin1;"
                        + " CREATE USER IF NOT EXISTS '" + DATABASE_USER + "'@'%' IDENTIFIED BY '"
                        + DATABASE_PASSWORD + "';"
                        + " CREATE USER IF NOT EXISTS '" + DATABASE_USER + "'@'localhost' IDENTIFIED BY '"
                        + DATABASE_PASSWORD + "';"
                        + " GRANT ALL PRIVILEGES ON " + APIM_DATABASE + ".* TO '" + DATABASE_USER + "'@'%';"
                        + " GRANT ALL PRIVILEGES ON " + SHARED_DATABASE + ".* TO '" + DATABASE_USER + "'@'%';"
                        + " GRANT ALL PRIVILEGES ON " + APIM_DATABASE + ".* TO '" + DATABASE_USER + "'@'localhost';"
                        + " GRANT ALL PRIVILEGES ON " + SHARED_DATABASE + ".* TO '" + DATABASE_USER + "'@'localhost';"
                        + " FLUSH PRIVILEGES;");

        for (Map.Entry<String, Path> schema : schemaFiles.entrySet()) {
            seedSchema(schema.getKey());
        }
    }

    private void waitForAuthenticatedMySql() throws Exception {
        Exception lastFailure = null;
        for (int attempt = 0; attempt < 60; attempt++) {
            try {
                execute("mysqladmin", "-h127.0.0.1", "-uroot", "-p" + ROOT_PASSWORD, "ping");
                return;
            } catch (Exception e) {
                lastFailure = e;
                Thread.sleep(1000);
            }
        }
        throw new IllegalStateException("MySQL did not accept authenticated connections", lastFailure);
    }

    private void seedSchema(String database) throws Exception {
        String markerCheck = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='"
                + database + "' AND table_name='" + SEED_MARKER_TABLE + "';";
        String marker = output("mysql", "-h127.0.0.1", "-u" + DATABASE_USER,
                "-p" + DATABASE_PASSWORD, "-N", "-B", database, "-e", markerCheck).trim();
        if ("1".equals(marker)) {
            return;
        }

        String target = "/tmp/" + database + "-schema.sql";
        execute("sh", "-c", "mysql -h127.0.0.1 -u" + DATABASE_USER + " -p" + DATABASE_PASSWORD
                + " " + database + " < " + target);
        execute("mysql", "-h127.0.0.1", "-u" + DATABASE_USER, "-p" + DATABASE_PASSWORD, database,
                "-e", "CREATE TABLE IF NOT EXISTS " + SEED_MARKER_TABLE
                        + " (ID INT PRIMARY KEY, SEEDED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP);");
    }

    private void requireDatabaseName(String database) {
        if (database == null || !database.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Unsafe MySQL database name: " + database);
        }
    }

    private void copySchemaToContainer(String database, Path schemaFile) {
        try {
            withCopyToContainer(Transferable.of(Files.readAllBytes(schemaFile), 0644),
                    "/tmp/" + database + "-schema.sql");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read schema file: " + schemaFile, e);
        }
    }

    private void execute(String... command) throws Exception {
        var result = execInContainer(command);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("MySQL command failed: " + result.getStderr());
        }
    }

    private String output(String... command) throws Exception {
        var result = execInContainer(command);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("MySQL command failed: " + result.getStderr());
        }
        return result.getStdout();
    }
}
