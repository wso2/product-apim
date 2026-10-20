/*
 * Copyright (c) 2026, WSO2 LLC. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.wso2.am.testcontainers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Creates and owns the two databases used by one all-in-one v2 block when the hosted DB profile is selected.
 * The database server is external to Docker; only the block's logical databases belong to this object.
 */
public final class HostedDatabaseBlock implements AutoCloseable {

    public static final String PROFILE_SYSTEM_PROPERTY = "apim.test.db.profile";
    public static final String ENV_FILE_SYSTEM_PROPERTY = "apim.test.db.envFile";

    private static final String MYSQL_PROFILE = "mysql";
    private static final String POSTGRESQL_PROFILE = "postgresql";
    private static final String POSTGRESQL_DATABASE_TYPE = "postgre";
    private static final int MYSQL_IDENTIFIER_LIMIT = 64;
    private static final int POSTGRESQL_IDENTIFIER_LIMIT = 63;

    private final String apimDatabase;
    private final String sharedDatabase;
    private final String adminUrl;
    private final String provisionUsername;
    private final String provisionPassword;
    private final String provisionJdbcUrlTemplate;
    private final String jdbcUrlTemplate;
    private final String runtimeUsername;
    private final String runtimePassword;
    private final String validationQuery;
    private final String databaseType;
    private final String databaseDriver;
    private final String databaseProfile;
    private final Path productZip;
    private boolean closed;

    private HostedDatabaseBlock(String label, Path envFile, Path productZip) throws IOException {
        Properties properties = loadProperties(envFile);
        databaseProfile = normalizeProfile(System.getProperty(PROFILE_SYSTEM_PROPERTY));
        databaseType = required(properties, "database.type");
        String expectedType = MYSQL_PROFILE.equals(databaseProfile) ? MYSQL_PROFILE : POSTGRESQL_DATABASE_TYPE;
        if (!expectedType.equalsIgnoreCase(databaseType)) {
            throw new IllegalArgumentException("Hosted database profile '" + databaseProfile
                    + "' requires database.type=" + expectedType + "; got: " + databaseType);
        }
        databaseDriver = required(properties, "database.driver");
        String expectedDriver = MYSQL_PROFILE.equals(databaseProfile)
                ? "com.mysql.cj.jdbc.Driver" : "org.postgresql.Driver";
        if (!expectedDriver.equals(databaseDriver)) {
            throw new IllegalArgumentException("Hosted database profile '" + databaseProfile
                    + "' requires database.driver=" + expectedDriver);
        }
        adminUrl = required(properties, "database.provision.url");
        provisionUsername = required(properties, "database.provision.username");
        provisionPassword = required(properties, "database.provision.password");
        provisionJdbcUrlTemplate = required(properties, "database.provision.urlTemplate");
        jdbcUrlTemplate = required(properties, "database.jdbc.urlTemplate");
        runtimeUsername = required(properties, "database.runtime.username");
        runtimePassword = required(properties, "database.runtime.password");
        validationQuery = properties.getProperty("database.validationQuery", "SELECT 1").trim();
        this.productZip = Objects.requireNonNull(productZip, "productZip");
        if (!productZip.toFile().isFile()) {
            throw new IllegalArgumentException("APIM distribution ZIP not found: " + productZip);
        }
        String suffix = shortHash(label);
        String prefix = "apim_v2_";
        String readable = label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        int identifierLimit = MYSQL_PROFILE.equals(databaseProfile)
                ? MYSQL_IDENTIFIER_LIMIT : POSTGRESQL_IDENTIFIER_LIMIT;
        int readableLimit = identifierLimit - prefix.length() - suffix.length() - 1;
        if (readable.length() > readableLimit) {
            readable = readable.substring(0, readableLimit);
        }
        String base = prefix + readable + "_" + suffix;
        apimDatabase = base + "_apim";
        sharedDatabase = base + "_shared";
        if (apimDatabase.length() > identifierLimit || sharedDatabase.length() > identifierLimit) {
            throw new IllegalStateException("Generated hosted database name exceeds " + databaseProfile
                    + " identifier limit");
        }
    }

    /** Provisions a fresh pair of block-specific databases, refusing to overwrite any existing database. */
    public static HostedDatabaseBlock provision(String label, Path envFile, Path productZip) throws Exception {
        HostedDatabaseBlock block = new HostedDatabaseBlock(label, envFile, productZip);
        block.createAndSeed();
        return block;
    }

    public static boolean isProfileSelected() {
        String profile = System.getProperty(PROFILE_SYSTEM_PROPERTY);
        return profile != null && !profile.isBlank();
    }

    public static String normalizeProfile(String profile) {
        if (profile == null || profile.isBlank()) {
            throw new IllegalArgumentException("Hosted database profile must be set to mysql or postgresql");
        }
        String normalized = profile.trim().toLowerCase(Locale.ROOT);
        if ("postgres".equals(normalized) || POSTGRESQL_DATABASE_TYPE.equals(normalized)) {
            normalized = POSTGRESQL_PROFILE;
        }
        if (!MYSQL_PROFILE.equals(normalized) && !POSTGRESQL_PROFILE.equals(normalized)) {
            throw new IllegalArgumentException("Unsupported hosted database profile '" + profile
                    + "'; supported profiles: mysql, postgresql");
        }
        return normalized;
    }

    public ConnectionSettings connectionSettings() {
        return new ConnectionSettings(databaseType, databaseDriver,
                jdbcUrl(apimDatabase), jdbcUrl(sharedDatabase), runtimeUsername, runtimePassword, validationQuery);
    }

    public String apimDatabaseName() {
        return apimDatabase;
    }

    public String sharedDatabaseName() {
        return sharedDatabase;
    }

    private void createAndSeed() throws Exception {
        Class.forName(databaseDriver);
        List<String> created = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(adminUrl, provisionUsername, provisionPassword)) {
            createDatabase(connection, apimDatabase);
            created.add(apimDatabase);
            createDatabase(connection, sharedDatabase);
            created.add(sharedDatabase);
        } catch (Exception failure) {
            dropCreatedDatabases(created, failure);
            throw failure;
        }
        try {
            executeSchema(apimDatabase, "/dbscripts/apimgt/mysql.sql");
            executeSchema(sharedDatabase, "/dbscripts/mysql.sql");
        } catch (Exception failure) {
            dropCreatedDatabases(List.of(apimDatabase, sharedDatabase), failure);
            throw failure;
        }
    }

    private void createDatabase(Connection connection, String database) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + quoteIdentifier(database));
        }
    }

    private void executeSchema(String database, String entryName) throws Exception {
        String schemaEntry = schemaEntry(entryName);
        String script = readZipEntry(productZip, productZipRoot(schemaEntry) + schemaEntry);
        String username = MYSQL_PROFILE.equals(databaseProfile) ? provisionUsername : runtimeUsername;
        String password = MYSQL_PROFILE.equals(databaseProfile) ? provisionPassword : runtimePassword;
        if (!MYSQL_PROFILE.equals(databaseProfile)) {
            grantPostgreSqlRuntimeAccess(database);
        }
        try (Connection connection = DriverManager.getConnection(provisionJdbcUrl(database), username, password);
             Statement statement = connection.createStatement()) {
            for (String sql : splitSqlStatements(script)) {
                statement.execute(sql);
            }
        }
    }

    private String schemaEntry(String requestedEntry) {
        boolean apim = requestedEntry.contains("apimgt");
        if (MYSQL_PROFILE.equals(databaseProfile)) {
            return apim ? "/dbscripts/apimgt/mysql.sql" : "/dbscripts/mysql.sql";
        }
        return apim ? "/dbscripts/apimgt/postgresql.sql" : "/dbscripts/postgresql.sql";
    }

    private String productZipRoot(String sharedSchemaEntry) throws IOException {
        try (ZipFile zip = new ZipFile(productZip.toFile())) {
            return zip.stream().map(ZipEntry::getName)
                    .filter(name -> name.endsWith(sharedSchemaEntry))
                    .findFirst()
                    .map(name -> name.substring(0, name.length() - sharedSchemaEntry.length()))
                    .orElseThrow(() -> new IOException("APIM ZIP does not contain " + sharedSchemaEntry));
        }
    }

    private void grantPostgreSqlRuntimeAccess(String database) throws Exception {
        String quotedDatabase = quoteIdentifier(database);
        String quotedRuntimeUser = quoteIdentifier(runtimeUsername);
        try (Connection connection = DriverManager.getConnection(adminUrl, provisionUsername, provisionPassword);
             Statement statement = connection.createStatement()) {
            statement.execute("GRANT ALL PRIVILEGES ON DATABASE " + quotedDatabase + " TO " + quotedRuntimeUser);
        }
        try (Connection connection = DriverManager.getConnection(provisionJdbcUrl(database), provisionUsername,
                provisionPassword); Statement statement = connection.createStatement()) {
            // PostgreSQL 15+ no longer grants CREATE on public to every user. The runtime account executes the
            // product DDL and will own those tables, so explicitly grant it access to the public schema.
            statement.execute("GRANT ALL PRIVILEGES ON SCHEMA public TO " + quotedRuntimeUser);
        }
    }

    private String quoteIdentifier(String identifier) {
        if (MYSQL_PROFILE.equals(databaseProfile)) {
            return "`" + identifier.replace("`", "``") + "`";
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static String readZipEntry(Path zipPath, String entryName) throws IOException {
        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                throw new IOException("APIM ZIP does not contain required database schema: " + entryName);
            }
            try (InputStream stream = zip.getInputStream(entry)) {
                return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
    }

    private String jdbcUrl(String database) {
        return jdbcUrlTemplate.replace("{database}", database);
    }

    private String provisionJdbcUrl(String database) {
        return provisionJdbcUrlTemplate.replace("{database}", database);
    }

    private void dropCreatedDatabases(List<String> databases, Exception original) {
        for (String database : databases) {
            try (Connection connection = DriverManager.getConnection(adminUrl, provisionUsername, provisionPassword);
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS " + quoteIdentifier(database));
            } catch (Exception cleanupFailure) {
                original.addSuppressed(cleanupFailure);
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (closed) {
            return;
        }
        closed = true;
        try (Connection connection = DriverManager.getConnection(adminUrl, provisionUsername, provisionPassword);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + quoteIdentifier(apimDatabase));
            statement.execute("DROP DATABASE IF EXISTS " + quoteIdentifier(sharedDatabase));
        }
    }

    private static Properties loadProperties(Path path) throws IOException {
        if (path == null || !path.toFile().isFile()) {
            throw new IllegalArgumentException("Hosted database env file not found: " + path
                    + " (copy a database-*.env.example file to a private env file or set -D"
                    + ENV_FILE_SYSTEM_PROPERTY + ")");
        }
        Properties properties = new Properties();
        try (InputStream stream = java.nio.file.Files.newInputStream(path)) {
            properties.load(stream);
        }
        return properties;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Hosted database env file is missing required setting: " + key);
        }
        return value.trim();
    }

    private static String shortHash(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest).substring(0, 10);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    /** Split the product's ordinary semicolon-delimited SQL while respecting quotes and SQL comments. */
    private static List<String> splitSqlStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean single = false;
        boolean quoted = false;
        boolean backtick = false;
        boolean lineComment = false;
        boolean blockComment = false;
        String dollarQuote = null;
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            char next = i + 1 < script.length() ? script.charAt(i + 1) : '\0';
            if (dollarQuote != null) {
                if (script.startsWith(dollarQuote, i)) {
                    current.append(dollarQuote);
                    i += dollarQuote.length() - 1;
                    dollarQuote = null;
                } else {
                    current.append(c);
                }
                continue;
            }
            if (lineComment) {
                if (c == '\n') {
                    lineComment = false;
                    current.append(c);
                }
                continue;
            }
            if (blockComment) {
                if (c == '*' && next == '/') {
                    blockComment = false;
                    i++;
                }
                continue;
            }
            if (!single && !quoted && !backtick && c == '-' && next == '-') {
                lineComment = true;
                i++;
                continue;
            }
            if (!single && !quoted && !backtick && c == '/' && next == '*') {
                blockComment = true;
                i++;
                continue;
            }
            if (!single && !quoted && !backtick && c == '$') {
                int end = script.indexOf('$', i + 1);
                if (end >= 0) {
                    String tag = script.substring(i + 1, end);
                    if (tag.isEmpty() || tag.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                        dollarQuote = script.substring(i, end + 1);
                        current.append(dollarQuote);
                        i = end;
                        continue;
                    }
                }
            }
            if (c == '\\' && (single || quoted) && i + 1 < script.length()) {
                current.append(c).append(script.charAt(++i));
                continue;
            }
            if (c == '\'' && !quoted && !backtick) {
                single = !single;
            } else if (c == '"' && !single && !backtick) {
                quoted = !quoted;
            } else if (c == '`' && !single && !quoted) {
                backtick = !backtick;
            }
            if (c == ';' && !single && !quoted && !backtick) {
                addStatement(statements, current);
            } else {
                current.append(c);
            }
        }
        addStatement(statements, current);
        return statements;
    }

    private static void addStatement(List<String> statements, StringBuilder current) {
        String sql = current.toString().trim();
        current.setLength(0);
        if (!sql.isEmpty()) {
            statements.add(sql);
        }
    }

    public static final class ConnectionSettings {
        private final String type;
        private final String driver;
        private final String apimUrl;
        private final String sharedUrl;
        private final String username;
        private final String password;
        private final String validationQuery;

        private ConnectionSettings(String type, String driver, String apimUrl, String sharedUrl, String username,
                                  String password, String validationQuery) {
            this.type = type;
            this.driver = driver;
            this.apimUrl = apimUrl;
            this.sharedUrl = sharedUrl;
            this.username = username;
            this.password = password;
            this.validationQuery = validationQuery;
        }

        public String type() { return type; }
        public String driver() { return driver; }
        public String apimUrl() { return apimUrl; }
        public String sharedUrl() { return sharedUrl; }
        public String username() { return username; }
        public String password() { return password; }
        public String validationQuery() { return validationQuery; }
    }
}
