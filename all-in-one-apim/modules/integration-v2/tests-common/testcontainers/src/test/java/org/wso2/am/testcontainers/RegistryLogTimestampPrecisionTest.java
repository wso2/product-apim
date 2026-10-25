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

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RegistryLogTimestampPrecisionTest {

    @Test
    public void leavesTheShippedSchemaWhenDisabled() throws Exception {
        RegistryLogTimestampPrecision.configure(false, "mysql", "publisher", "shared", () -> {
            Assert.fail("Disabled precision setting must not open a database connection");
            return null;
        });
    }

    @DataProvider
    public Object[][] otherDatabases() {
        return new Object[][]{{"h2"}, {"postgre"}, {"oracle"}, {"db2"}, {"mssql"}};
    }

    @Test(dataProvider = "otherDatabases")
    public void doesNotConnectToOtherDatabaseTypes(String databaseType) throws Exception {
        RegistryLogTimestampPrecision.configure(true, databaseType, "devportal", "shared", () -> {
            Assert.fail("MySQL precision setting must not connect to " + databaseType);
            return null;
        });
    }

    @Test
    public void altersOnlyTheMySqlColumnAndVerifiesTheSelectedDatabase() throws Exception {
        Database database = new Database(3, true);
        RegistryLogTimestampPrecision.configure(true, "mysql", "devportal", "block_shared", database::connection);
        Assert.assertEquals(database.statements, List.of(
                "ALTER TABLE REG_LOG MODIFY REG_LOGGED_TIME TIMESTAMP(3) NOT NULL",
                "SELECT DATETIME_PRECISION FROM INFORMATION_SCHEMA.COLUMNS"
                        + " WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'REG_LOG' AND COLUMN_NAME = 'REG_LOGGED_TIME'"));
        Assert.assertEquals(database.schema, "block_shared");
        Assert.assertTrue(database.connectionClosed);
    }

    @DataProvider
    public Object[][] invalidColumns() {
        return new Object[][]{{0, true}, {6, true}, {null, true}, {3, false}};
    }

    @Test(dataProvider = "invalidColumns")
    public void rejectsAnUnverifiedColumn(Integer precision, boolean present) throws Exception {
        Database database = new Database(precision, present);
        SQLException failure = Assert.expectThrows(SQLException.class,
                () -> RegistryLogTimestampPrecision.configure(true, "mysql", "devportal", "block_shared",
                        database::connection));
        Assert.assertTrue(failure.getMessage().contains("Block 'devportal'"));
        Assert.assertTrue(failure.getMessage().contains("database 'block_shared'"));
        Assert.assertTrue(database.connectionClosed);
    }

    @Test
    public void identifiesTheBlockAndDatabaseOnSqlFailure() {
        SQLException failure = Assert.expectThrows(SQLException.class,
                () -> RegistryLogTimestampPrecision.configure(true, "mysql", "devportal", "block_shared", () -> {
                    throw new SQLException("Access denied");
                }));
        Assert.assertTrue(failure.getMessage().contains("Block 'devportal'"));
        Assert.assertTrue(failure.getMessage().contains("database 'block_shared'"));
        Assert.assertTrue(failure.getMessage().contains("Access denied"));
    }

    private static final class Database {
        private final Integer precision;
        private final boolean present;
        private final List<String> statements = new ArrayList<>();
        private String schema;
        private boolean connectionClosed;

        private Database(Integer precision, boolean present) {
            this.precision = precision;
            this.present = present;
        }

        private Connection connection() {
            ResultSet result = stub(ResultSet.class, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "next": return present;
                    case "getInt": return precision == null ? 0 : precision;
                    case "wasNull": return precision == null;
                    case "close": return null;
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });
            PreparedStatement query = stub(PreparedStatement.class, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "setString":
                        Assert.assertEquals(args[0], 1);
                        schema = (String) args[1];
                        return null;
                    case "executeQuery":
                        return result;
                    case "close":
                        return null;
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });
            Statement alter = stub(Statement.class, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "execute":
                        statements.add((String) args[0]);
                        return true;
                    case "close": return null;
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });
            return stub(Connection.class, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "createStatement": return alter;
                    case "prepareStatement":
                        statements.add((String) args[0]);
                        return query;
                    case "close":
                        connectionClosed = true;
                        return null;
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });
        }
    }

    private static <T> T stub(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
