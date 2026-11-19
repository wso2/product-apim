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

/** Block-scoped MySQL registry activity timestamps with verified millisecond precision. */
public final class RegistryLogTimestampPrecision {

    private static final Logger logger = LoggerFactory.getLogger(RegistryLogTimestampPrecision.class);
    private static final String ALTER =
            "ALTER TABLE REG_LOG MODIFY REG_LOGGED_TIME TIMESTAMP(3) NOT NULL";
    private static final String PRECISION = "SELECT DATETIME_PRECISION FROM INFORMATION_SCHEMA.COLUMNS"
            + " WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'REG_LOG' AND COLUMN_NAME = 'REG_LOGGED_TIME'";

    private RegistryLogTimestampPrecision() {
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
        if (!"mysql".equalsIgnoreCase(databaseType)) {
            logger.info("Block '{}' registryLogMillisecondPrecision does not apply to {} database '{}'",
                    blockLabel, databaseType, database);
            return;
        }
        try (Connection connection = connections.open()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(ALTER);
            }
            try (PreparedStatement statement = connection.prepareStatement(PRECISION)) {
                statement.setString(1, database);
                try (ResultSet columns = statement.executeQuery()) {
                    if (!columns.next()) {
                        throw new SQLException("REG_LOG.REG_LOGGED_TIME is missing from INFORMATION_SCHEMA.COLUMNS");
                    }
                    int precision = columns.getInt(1);
                    if (columns.wasNull() || precision != 3) {
                        throw new SQLException("Expected DATETIME_PRECISION=3 for REG_LOG.REG_LOGGED_TIME; got "
                                + (columns.wasNull() ? "NULL" : precision));
                    }
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Block '" + blockLabel + "' could not provision registryLogMillisecondPrecision"
                    + " in database '" + database + "': " + e.getMessage(), e);
        }
        logger.info("Block '{}' database '{}' REG_LOG.REG_LOGGED_TIME set to TIMESTAMP(3) NOT NULL;"
                + " verified DATETIME_PRECISION=3", blockLabel, database);
    }
}
