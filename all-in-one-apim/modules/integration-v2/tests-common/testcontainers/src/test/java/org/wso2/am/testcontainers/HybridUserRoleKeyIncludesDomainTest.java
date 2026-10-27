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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HybridUserRoleKeyIncludesDomainTest {

    private static final String OLD_CONSTRAINT = "UM_HYBRID_USER_ROLE_UK";
    private static final List<String> THREE_COLUMNS = List.of("UM_USER_NAME", "UM_ROLE_ID", "UM_TENANT_ID");
    private static final List<String> FOUR_COLUMNS =
            List.of("UM_USER_NAME", "UM_ROLE_ID", "UM_TENANT_ID", "UM_DOMAIN_ID");

    @Test
    public void leavesTheShippedSchemaWhenDisabled() throws Exception {
        HybridUserRoleKeyIncludesDomain.configure(false, "db2", "publisher", "shared", () -> {
            Assert.fail("Disabled key adjustment must not open a database connection");
            return null;
        });
    }

    @DataProvider
    public Object[][] otherDatabases() {
        return new Object[][]{{"mysql"}, {"postgre"}, {"mssql"}, {"oracle"}, {"h2"}};
    }

    @Test(dataProvider = "otherDatabases")
    public void doesNotConnectToOtherDatabaseTypes(String databaseType) throws Exception {
        HybridUserRoleKeyIncludesDomain.configure(true, databaseType, "devportal", "shared", () -> {
            Assert.fail("Db2 key adjustment must not connect to " + databaseType);
            return null;
        });
    }

    @Test
    public void discoversDropsAndVerifiesTheThreeColumnDb2Key() throws Exception {
        Database database = new Database(true);
        HybridUserRoleKeyIncludesDomain.configure(true, "db2", "publisher", "block_shared", database::connection);
        Assert.assertTrue(database.statements.contains(
                "ALTER TABLE UM_HYBRID_USER_ROLE DROP CONSTRAINT \"" + OLD_CONSTRAINT + "\""));
        Assert.assertTrue(database.statements.contains(
                "ALTER TABLE UM_HYBRID_USER_ROLE ADD CONSTRAINT UM_HYBRID_USER_ROLE_UK_DOM"
                        + " UNIQUE (UM_USER_NAME, UM_ROLE_ID, UM_TENANT_ID, UM_DOMAIN_ID)"));
        Assert.assertEquals(database.keys.get("UM_HYBRID_USER_ROLE_UK_DOM"), FOUR_COLUMNS);
        Assert.assertFalse(database.keys.containsKey(OLD_CONSTRAINT));
        Assert.assertTrue(database.connectionClosed);
    }

    @Test
    public void failsWhenTheFourColumnKeyCannotBeReadBack() {
        Database database = new Database(false);
        SQLException failure = Assert.expectThrows(SQLException.class,
                () -> HybridUserRoleKeyIncludesDomain.configure(true, "db2", "publisher", "block_shared",
                        database::connection));
        Assert.assertTrue(failure.getMessage().contains("Block 'publisher'"));
        Assert.assertTrue(failure.getMessage().contains("database 'block_shared'"));
        Assert.assertTrue(failure.getMessage().contains("Expected exactly one unique key"));
        Assert.assertTrue(database.connectionClosed);
    }

    @Test
    public void identifiesTheBlockAndDatabaseOnSqlFailure() {
        SQLException failure = Assert.expectThrows(SQLException.class,
                () -> HybridUserRoleKeyIncludesDomain.configure(true, "db2", "publisher", "block_shared", () -> {
                    throw new SQLException("Catalog access denied");
                }));
        Assert.assertTrue(failure.getMessage().contains("Block 'publisher'"));
        Assert.assertTrue(failure.getMessage().contains("database 'block_shared'"));
        Assert.assertTrue(failure.getMessage().contains("Catalog access denied"));
    }

    private static final class Database {
        private final Map<String, List<String>> keys = new LinkedHashMap<>();
        private final List<String> statements = new ArrayList<>();
        private final boolean applyAddConstraint;
        private boolean connectionClosed;

        private Database(boolean applyAddConstraint) {
            this.applyAddConstraint = applyAddConstraint;
            keys.put(OLD_CONSTRAINT, new ArrayList<>(THREE_COLUMNS));
        }

        private Connection connection() {
            return stub(Connection.class, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "createStatement": return statement();
                    case "prepareStatement": return preparedStatement((String) args[0]);
                    case "close": connectionClosed = true; return null;
                    default: throw new UnsupportedOperationException(method.getName());
                }
            });
        }

        private Statement statement() {
            return stub(Statement.class, (proxy, method, args) -> {
                if ("execute".equals(method.getName())) {
                    String sql = (String) args[0];
                    statements.add(sql);
                    if (sql.contains(" DROP CONSTRAINT ")) {
                        keys.remove(OLD_CONSTRAINT);
                    } else if (sql.contains(" ADD CONSTRAINT ") && applyAddConstraint) {
                        keys.put("UM_HYBRID_USER_ROLE_UK_DOM", new ArrayList<>(FOUR_COLUMNS));
                    }
                    return true;
                }
                if ("executeQuery".equals(method.getName())) {
                    statements.add((String) args[0]);
                    return rows(List.<String[]>of(new String[]{"DB2INST1"}));
                }
                if ("close".equals(method.getName())) return null;
                throw new UnsupportedOperationException(method.getName());
            });
        }

        private PreparedStatement preparedStatement(String sql) {
            statements.add(sql);
            return stub(PreparedStatement.class, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                    if ("executeQuery".equals(method.getName())) {
                        List<String[]> catalogRows = new ArrayList<>();
                        keys.forEach((constraint, columns) -> columns.forEach(
                                column -> catalogRows.add(new String[]{constraint, column})));
                        return rows(catalogRows);
                    }
                    if ("setString".equals(method.getName()) || "close".equals(method.getName())) return null;
                    throw new UnsupportedOperationException(method.getName());
                }
            });
        }

        private ResultSet rows(List<String[]> values) {
            return stub(ResultSet.class, new InvocationHandler() {
                private int index = -1;

                @Override
                public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                    switch (method.getName()) {
                        case "next": return ++index < values.size();
                        case "getString": return values.get(index)[(Integer) args[0] - 1];
                        case "close": return null;
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                }
            });
        }
    }

    private static <T> T stub(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
