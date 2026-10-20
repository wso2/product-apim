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
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Creates and owns the two block-specific database resources used by one all-in-one v2 block when the hosted DB
 * profile is selected. Oracle represents those resources as schemas inside the configured PDB; other supported
 * profiles use separate databases. The block owns only its databases, not the database server lifecycle.
 */
public final class HostedDatabaseBlock implements AutoCloseable {

    public static final String PROFILE_SYSTEM_PROPERTY = "apim.test.db.profile";
    public static final String ENV_FILE_SYSTEM_PROPERTY = "apim.test.db.envFile";

    private static final String MYSQL_PROFILE = "mysql";
    private static final String POSTGRESQL_PROFILE = "postgresql";
    private static final String MSSQL_PROFILE = "mssql";
    private static final String ORACLE_PROFILE = "oracle";
    private static final String ORACLE_RAC_PROFILE = "oracle-rac";
    private static final String DB2_PROFILE = "db2";
    private static final String POSTGRESQL_DATABASE_TYPE = "postgre";
    private static final int MYSQL_IDENTIFIER_LIMIT = 64;
    private static final int POSTGRESQL_IDENTIFIER_LIMIT = 63;
    private static final int MSSQL_IDENTIFIER_LIMIT = 128;
    // Preserve compatibility with Oracle installations using the traditional 30-byte identifier limit.
    private static final int ORACLE_IDENTIFIER_LIMIT = 30;
    // Db2 LUW database names are limited to 8 characters.
    private static final int DB2_IDENTIFIER_LIMIT = 8;
    private static final long DB2_COMMAND_TIMEOUT_MINUTES = 10;

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
    private final String db2ProvisionContainer;
    private final Path productZip;
    private boolean closed;

    private HostedDatabaseBlock(String label, Path envFile, Path productZip) throws IOException {
        Properties properties = loadProperties(envFile);
        databaseProfile = normalizeProfile(System.getProperty(PROFILE_SYSTEM_PROPERTY));
        databaseType = required(properties, "database.type");
        String expectedType = MYSQL_PROFILE.equals(databaseProfile) ? MYSQL_PROFILE
                : POSTGRESQL_PROFILE.equals(databaseProfile) ? POSTGRESQL_DATABASE_TYPE
                : isOracleProfile() ? ORACLE_PROFILE : databaseProfile;
        if (!expectedType.equalsIgnoreCase(databaseType)) {
            throw new IllegalArgumentException("Hosted database profile '" + databaseProfile
                    + "' requires database.type=" + expectedType + "; got: " + databaseType);
        }
        databaseDriver = required(properties, "database.driver");
        String expectedDriver = MYSQL_PROFILE.equals(databaseProfile) ? "com.mysql.cj.jdbc.Driver"
                : POSTGRESQL_PROFILE.equals(databaseProfile) ? "org.postgresql.Driver"
                : MSSQL_PROFILE.equals(databaseProfile) ? "com.microsoft.sqlserver.jdbc.SQLServerDriver"
                : isOracleProfile() ? "oracle.jdbc.driver.OracleDriver"
                : "com.ibm.db2.jcc.DB2Driver";
        if (!expectedDriver.equals(databaseDriver)) {
            throw new IllegalArgumentException("Hosted database profile '" + databaseProfile
                    + "' requires database.driver=" + expectedDriver);
        }
        adminUrl = required(properties, "database.provision.url");
        provisionUsername = required(properties, "database.provision.username");
        provisionPassword = required(properties, "database.provision.password");
        provisionJdbcUrlTemplate = required(properties, "database.provision.urlTemplate");
        jdbcUrlTemplate = required(properties, "database.jdbc.urlTemplate");
        runtimeUsername = isOracleProfile()
                ? properties.getProperty("database.runtime.username", "").trim()
                : required(properties, "database.runtime.username");
        runtimePassword = required(properties, "database.runtime.password");
        db2ProvisionContainer = DB2_PROFILE.equals(databaseProfile)
                ? required(properties, "database.provision.container") : null;
        if (DB2_PROFILE.equals(databaseProfile)) {
            if (!provisionUsername.equals(runtimeUsername) || !provisionPassword.equals(runtimePassword)) {
                throw new IllegalArgumentException("DB2 profile requires the instance owner credentials for both "
                        + "database provisioning and APIM runtime access");
            }
            if (!provisionUsername.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException("Invalid DB2 instance owner username");
            }
            if (!db2ProvisionContainer.matches("[A-Za-z0-9][A-Za-z0-9_.-]*")) {
                throw new IllegalArgumentException("Invalid DB2 provisioning container name");
            }
        }
        validationQuery = properties.getProperty("database.validationQuery", "SELECT 1").trim();
        this.productZip = Objects.requireNonNull(productZip, "productZip");
        if (!productZip.toFile().isFile()) {
            throw new IllegalArgumentException("APIM distribution ZIP not found: " + productZip);
        }
        String suffix = shortHash(label);
        String prefix = "apim_v2_";
        String readable = label.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        int identifierLimit = DB2_PROFILE.equals(databaseProfile) ? DB2_IDENTIFIER_LIMIT
                : MYSQL_PROFILE.equals(databaseProfile) ? MYSQL_IDENTIFIER_LIMIT
                : POSTGRESQL_PROFILE.equals(databaseProfile) ? POSTGRESQL_IDENTIFIER_LIMIT
                : isOracleProfile() ? ORACLE_IDENTIFIER_LIMIT : MSSQL_IDENTIFIER_LIMIT;
        if (DB2_PROFILE.equals(databaseProfile)) {
            // Db2 LUW permits only 8 characters, so use type prefix plus a stable per-block hash.
            String blockHash = shortHash(label).substring(0, DB2_IDENTIFIER_LIMIT - 1).toUpperCase(Locale.ROOT);
            apimDatabase = "A" + blockHash;
            sharedDatabase = "S" + blockHash;
            return;
        }
        int readableLimit = identifierLimit - prefix.length() - suffix.length() - 1 - "_shared".length();
        if (readable.length() > readableLimit) {
            readable = readable.substring(0, readableLimit);
        }
        String base = prefix + readable + "_" + suffix;
        String apimName = base + "_apim";
        String sharedName = base + "_shared";
        apimDatabase = isOracleProfile() ? apimName.toUpperCase(Locale.ROOT) : apimName;
        sharedDatabase = isOracleProfile() ? sharedName.toUpperCase(Locale.ROOT) : sharedName;
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
            throw new IllegalArgumentException("Hosted database profile must be set to mysql, postgresql, mssql, "
                    + "oracle, oracle-rac, or db2");
        }
        String normalized = profile.trim().toLowerCase(Locale.ROOT);
        if ("postgres".equals(normalized) || POSTGRESQL_DATABASE_TYPE.equals(normalized)) {
            normalized = POSTGRESQL_PROFILE;
        }
        if ("oracle_rac".equals(normalized) || "oraclerac".equals(normalized)) {
            normalized = ORACLE_RAC_PROFILE;
        }
        if (!MYSQL_PROFILE.equals(normalized) && !POSTGRESQL_PROFILE.equals(normalized)
                && !MSSQL_PROFILE.equals(normalized) && !ORACLE_PROFILE.equals(normalized)
                && !ORACLE_RAC_PROFILE.equals(normalized)
                && !DB2_PROFILE.equals(normalized)) {
            throw new IllegalArgumentException("Unsupported hosted database profile '" + profile
                    + "'; supported profiles: mysql, postgresql, mssql, oracle, oracle-rac, db2");
        }
        return normalized;
    }

    public ConnectionSettings connectionSettings() {
        return new ConnectionSettings(databaseType, databaseDriver,
                jdbcUrl(apimDatabase), jdbcUrl(sharedDatabase), username(apimDatabase), username(sharedDatabase),
                runtimePassword, validationQuery);
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
            createDatabase(connection, apimDatabase, created);
            createDatabase(connection, sharedDatabase, created);
        } catch (Exception failure) {
            dropCreatedDatabases(created, failure);
            throw failure;
        }
        try {
            String schemaProfile = schemaProfileName();
            executeSchema(apimDatabase, "/dbscripts/apimgt/" + schemaProfile + ".sql");
            executeSchema(sharedDatabase, "/dbscripts/" + schemaProfile + ".sql");
        } catch (Exception failure) {
            dropCreatedDatabases(List.of(apimDatabase, sharedDatabase), failure);
            throw failure;
        }
    }

    private void createDatabase(Connection connection, String database, List<String> created) throws Exception {
        try (Statement statement = connection.createStatement()) {
            if (DB2_PROFILE.equals(databaseProfile)) {
                runDb2Command("CREATE DATABASE " + database + " USING CODESET UTF-8 TERRITORY US "
                        + "COLLATE USING SYSTEM");
                created.add(database);
            } else if (isOracleProfile()) {
                // Oracle isolates each APIM database as a schema (user) inside the shared PDB service.
                String password = "\"" + runtimePassword.replace("\"", "\"\"") + "\"";
                statement.execute("CREATE USER " + quoteIdentifier(database) + " IDENTIFIED BY " + password
                        + " DEFAULT TABLESPACE USERS TEMPORARY TABLESPACE TEMP QUOTA UNLIMITED ON USERS");
                created.add(database);
                statement.execute("GRANT CONNECT, RESOURCE, CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, "
                        + "CREATE TRIGGER, CREATE PROCEDURE, DBA TO " + quoteIdentifier(database));
            } else {
                String collation = MSSQL_PROFILE.equals(databaseProfile) ? " COLLATE Latin1_General_CS_AS" : "";
                statement.execute("CREATE DATABASE " + quoteIdentifier(database) + collation);
                created.add(database);
            }
        }
    }

    private void executeSchema(String database, String entryName) throws Exception {
        String script = readZipEntry(productZip, productZipRoot(entryName) + entryName);
        String username = MYSQL_PROFILE.equals(databaseProfile) ? provisionUsername : username(database);
        String password = MYSQL_PROFILE.equals(databaseProfile) ? provisionPassword : runtimePassword;
        if (POSTGRESQL_PROFILE.equals(databaseProfile)) {
            grantPostgreSqlRuntimeAccess(database);
        } else if (MSSQL_PROFILE.equals(databaseProfile)) {
            grantMssqlRuntimeAccess(database);
        }
        try (Connection connection = DriverManager.getConnection(provisionJdbcUrl(database), username, password);
             Statement statement = connection.createStatement()) {
            List<String> statements = DB2_PROFILE.equals(databaseProfile) ? splitDb2Statements(script)
                    : MSSQL_PROFILE.equals(databaseProfile) ? splitSqlServerBatches(script)
                    : isOracleProfile() ? splitOracleStatements(script) : splitSqlStatements(script);
            for (String sql : statements) {
                statement.execute(sql);
                if (DB2_PROFILE.equals(databaseProfile) && needsDb2ApplicationUuidConstraint(sql)) {
                    // The bundled DB2 schema adds a foreign key from AM_API_KEY_APPLICATION_MAPPING to
                    // AM_APPLICATION.UUID, but does not declare UUID as a unique key. DB2 requires a unique
                    // parent key for a foreign key, so add the missing constraint before the mapping table.
                    statement.execute("ALTER TABLE AM_APPLICATION ADD CONSTRAINT AM_APPLICATION_UUID_UK "
                            + "UNIQUE (UUID)");
                }
            }
        }
    }

    private static boolean needsDb2ApplicationUuidConstraint(String sql) {
        String normalized = sql.stripLeading().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
        return normalized.startsWith("CREATE TABLE AM_APPLICATION ")
                && !normalized.matches("(?s).*\\bUNIQUE\\s*\\(\\s*UUID\\s*\\).*");
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

    private void grantMssqlRuntimeAccess(String database) throws Exception {
        String quotedDatabase = quoteIdentifier(database);
        String quotedRuntimeUser = quoteIdentifier(runtimeUsername);
        try (Connection connection = DriverManager.getConnection(adminUrl, provisionUsername, provisionPassword);
             Statement statement = connection.createStatement()) {
            statement.execute("USE " + quotedDatabase);
            statement.execute("CREATE USER " + quotedRuntimeUser + " FOR LOGIN " + quotedRuntimeUser);
            statement.execute("ALTER ROLE [db_owner] ADD MEMBER " + quotedRuntimeUser);
        }
    }

    private String quoteIdentifier(String identifier) {
        if (MYSQL_PROFILE.equals(databaseProfile)) {
            return "`" + identifier.replace("`", "``") + "`";
        }
        if (MSSQL_PROFILE.equals(databaseProfile)) {
            return "[" + identifier.replace("]", "]]") + "]";
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

    private String username(String database) {
        return isOracleProfile() ? database : runtimeUsername;
    }

    private boolean isOracleProfile() {
        return ORACLE_PROFILE.equals(databaseProfile) || ORACLE_RAC_PROFILE.equals(databaseProfile);
    }

    private String schemaProfileName() {
        return ORACLE_RAC_PROFILE.equals(databaseProfile) ? "oracle_rac" : databaseProfile;
    }

    private String provisionJdbcUrl(String database) {
        return provisionJdbcUrlTemplate.replace("{database}", database);
    }

    private void dropCreatedDatabases(List<String> databases, Exception original) {
        for (String database : databases) {
            try (Connection connection = DriverManager.getConnection(adminUrl, provisionUsername, provisionPassword);
                 Statement statement = connection.createStatement()) {
                dropDatabase(statement, database);
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
            Exception cleanupFailure = null;
            for (String database : List.of(apimDatabase, sharedDatabase)) {
                try {
                    dropDatabase(statement, database);
                } catch (Exception failure) {
                    if (cleanupFailure == null) {
                        cleanupFailure = failure;
                    } else {
                        cleanupFailure.addSuppressed(failure);
                    }
                }
            }
            if (cleanupFailure != null) {
                throw cleanupFailure;
            }
        }
    }

    private void dropDatabase(Statement statement, String database) throws Exception {
        String quotedDatabase = quoteIdentifier(database);
        if (DB2_PROFILE.equals(databaseProfile)) {
            runDb2Command("DROP DATABASE " + database);
        } else if (isOracleProfile()) {
            statement.execute("BEGIN EXECUTE IMMEDIATE 'DROP USER " + quotedDatabase
                    + " CASCADE'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -1918 THEN RAISE; END IF; END;");
        } else if (MSSQL_PROFILE.equals(databaseProfile)) {
            statement.execute("IF DB_ID(N'" + database.replace("'", "''") + "') IS NOT NULL BEGIN "
                    + "ALTER DATABASE " + quotedDatabase + " SET SINGLE_USER WITH ROLLBACK IMMEDIATE; "
                    + "DROP DATABASE " + quotedDatabase + "; END");
        } else {
            statement.execute("DROP DATABASE IF EXISTS " + quotedDatabase);
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
        return splitSqlStatements(script, ';', true);
    }

    private static List<String> splitSqlStatements(String script, char delimiter, boolean backslashEscapes) {
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
            if (backslashEscapes && c == '\\' && (single || quoted) && i + 1 < script.length()) {
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
            if (c == delimiter && !single && !quoted && !backtick) {
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

    /** Split the product's Db2 CLP scripts as with -td/, retaining semicolons inside trigger bodies. */
    private static List<String> splitDb2Statements(String script) {
        return splitSqlStatements(script, '/', false);
    }

    private void runDb2Command(String command) throws Exception {
        Process process = new ProcessBuilder("docker", "exec", db2ProvisionContainer, "su", "-", provisionUsername,
                "-c", "db2 " + command + "; db2_status=$?; db2 terminate >/dev/null 2>&1; exit \"$db2_status\"")
                .redirectErrorStream(true).start();
        if (!process.waitFor(DB2_COMMAND_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new IllegalStateException("Timed out running Db2 command in container '" + db2ProvisionContainer
                    + "' after " + DB2_COMMAND_TIMEOUT_MINUTES + " minutes: " + command);
        }
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                .trim();
        if (process.exitValue() != 0) {
            throw new IllegalStateException("Db2 command failed in container '" + db2ProvisionContainer + "' (exit "
                    + process.exitValue() + "): " + command + (output.isEmpty() ? "" : "\n" + output));
        }
    }

    /** SQL Server's GO token separates batches and is not part of SQL sent over JDBC. */
    private static List<String> splitSqlServerBatches(String script) {
        List<String> batches = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : script.split("\\R", -1)) {
            if (line.trim().matches("(?i)GO(?:\\s+\\d+)?(?:\\s+--.*)?")) {
                addStatement(batches, current);
            } else {
                current.append(line).append('\n');
            }
        }
        addStatement(batches, current);
        return batches;
    }

    /** Split Oracle SQL*Plus scripts on standalone slash delimiters, retaining PL/SQL block terminators. */
    private static List<String> splitOracleStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : script.split("\\R", -1)) {
            if (line.trim().equals("/")) {
                addOracleStatement(statements, current);
            } else {
                current.append(line).append('\n');
            }
        }
        addOracleStatement(statements, current);
        return statements;
    }

    private static void addOracleStatement(List<String> statements, StringBuilder current) {
        String sql = current.toString().trim();
        current.setLength(0);
        if (sql.isEmpty()) {
            return;
        }
        String commentFree = sql.replaceAll("(?m)--.*$", "").replaceAll("(?s)/\\*.*?\\*/", "").trim();
        if (commentFree.isEmpty()) {
            return;
        }
        boolean plsqlBlock = sql.matches("(?is)^CREATE\\s+(OR\\s+REPLACE\\s+)?(TRIGGER|PROCEDURE|FUNCTION|PACKAGE)(\\s|$).*");
        if (!plsqlBlock && sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        if (!sql.isEmpty()) {
            statements.add(sql);
        }
    }

    public static final class ConnectionSettings {
        private final String type;
        private final String driver;
        private final String apimUrl;
        private final String sharedUrl;
        private final String apimUsername;
        private final String sharedUsername;
        private final String password;
        private final String validationQuery;

        private ConnectionSettings(String type, String driver, String apimUrl, String sharedUrl,
                                  String apimUsername, String sharedUsername, String password, String validationQuery) {
            this.type = type;
            this.driver = driver;
            this.apimUrl = apimUrl;
            this.sharedUrl = sharedUrl;
            this.apimUsername = apimUsername;
            this.sharedUsername = sharedUsername;
            this.password = password;
            this.validationQuery = validationQuery;
        }

        public String type() { return type; }
        public String driver() { return driver; }
        public String apimUrl() { return apimUrl; }
        public String sharedUrl() { return sharedUrl; }
        public String apimUsername() { return apimUsername; }
        public String sharedUsername() { return sharedUsername; }
        public String password() { return password; }
        public String validationQuery() { return validationQuery; }
    }
}
