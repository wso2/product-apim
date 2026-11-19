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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Block-scoped user-store role key adjustment for databases containing same-named users in separate domains. */
public final class HybridUserRoleKeyIncludesDomain {

    private static final Logger logger = LoggerFactory.getLogger(HybridUserRoleKeyIncludesDomain.class);
    private static final Set<String> THREE_COLUMN_KEY = Set.of("UM_USER_NAME", "UM_ROLE_ID", "UM_TENANT_ID");
    private static final Set<String> FOUR_COLUMN_KEY = Set.of(
            "UM_USER_NAME", "UM_ROLE_ID", "UM_TENANT_ID", "UM_DOMAIN_ID");
    private static final String CURRENT_SCHEMA = "SELECT CURRENT SCHEMA FROM SYSIBM.SYSDUMMY1";
    private static final String UNIQUE_KEYS = "SELECT T.CONSTNAME, K.COLNAME FROM SYSCAT.TABCONST T"
            + " JOIN SYSCAT.KEYCOLUSE K ON T.TABSCHEMA = K.TABSCHEMA AND T.TABNAME = K.TABNAME"
            + " AND T.CONSTNAME = K.CONSTNAME WHERE T.TABSCHEMA = ? AND T.TABNAME = ?"
            + " AND T.TYPE = 'U' ORDER BY T.CONSTNAME, K.COLSEQ";
    private static final String TABLE = "UM_HYBRID_USER_ROLE";
    private static final String FOUR_COLUMN_CONSTRAINT = "UM_HYBRID_USER_ROLE_UK_DOM";

    private HybridUserRoleKeyIncludesDomain() {
    }

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    public static void configure(boolean enabled, String databaseType, String blockLabel, String database,
                                 ConnectionProvider connections) throws SQLException {
        if (!enabled) {
            return;
        }
        if (!"db2".equalsIgnoreCase(databaseType)) {
            logger.info("Block '{}' hybridUserRoleKeyIncludesDomain is already satisfied by {} database '{}'",
                    blockLabel, databaseType, database);
            return;
        }
        try (Connection connection = connections.open()) {
            String schema = currentSchema(connection);
            Map<String, List<String>> keys = uniqueKeys(connection, schema);
            List<String> threeColumnConstraints = matchingConstraints(keys, THREE_COLUMN_KEY);
            List<String> fourColumnConstraints = matchingConstraints(keys, FOUR_COLUMN_KEY);
            if (!threeColumnConstraints.isEmpty()) {
                for (String constraint : threeColumnConstraints) {
                    dropConstraint(connection, constraint);
                }
            }
            if (fourColumnConstraints.isEmpty()) {
                addFourColumnConstraint(connection);
            }
            Map<String, List<String>> verifiedKeys = uniqueKeys(connection, schema);
            if (!matchingConstraints(verifiedKeys, THREE_COLUMN_KEY).isEmpty()
                    || matchingConstraints(verifiedKeys, FOUR_COLUMN_KEY).size() != 1) {
                throw new SQLException("Expected exactly one unique key on " + TABLE
                        + " (UM_USER_NAME, UM_ROLE_ID, UM_TENANT_ID, UM_DOMAIN_ID) and no three-column key");
            }
        } catch (SQLException e) {
            throw new SQLException("Block '" + blockLabel + "' could not provision hybridUserRoleKeyIncludesDomain"
                    + " in database '" + database + "': " + e.getMessage(), e);
        }
        logger.info("Block '{}' database '{}' changed {} unique key to include UM_DOMAIN_ID; verified four-column key",
                blockLabel, database, TABLE);
    }

    private static String currentSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(CURRENT_SCHEMA)) {
            if (!result.next()) {
                throw new SQLException("Db2 did not return CURRENT SCHEMA");
            }
            String schema = result.getString(1);
            if (schema == null || schema.isBlank()) {
                throw new SQLException("Db2 returned an empty CURRENT SCHEMA");
            }
            return schema;
        }
    }

    private static Map<String, List<String>> uniqueKeys(Connection connection, String schema) throws SQLException {
        Map<String, List<String>> keys = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(UNIQUE_KEYS)) {
            statement.setString(1, schema);
            statement.setString(2, TABLE);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    keys.computeIfAbsent(result.getString(1), ignored -> new ArrayList<>())
                            .add(result.getString(2));
                }
            }
        }
        return keys;
    }

    private static List<String> matchingConstraints(Map<String, List<String>> keys, Set<String> expected) {
        List<String> matches = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : keys.entrySet()) {
            Set<String> columns = new LinkedHashSet<>(entry.getValue());
            if (columns.equals(expected) && columns.size() == entry.getValue().size()) {
                matches.add(entry.getKey());
            }
        }
        return matches;
    }

    private static void dropConstraint(Connection connection, String constraint) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + TABLE + " DROP CONSTRAINT " + quoteIdentifier(constraint));
        }
    }

    private static void addFourColumnConstraint(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + TABLE + " ADD CONSTRAINT " + FOUR_COLUMN_CONSTRAINT
                    + " UNIQUE (UM_USER_NAME, UM_ROLE_ID, UM_TENANT_ID, UM_DOMAIN_ID)");
        }
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
