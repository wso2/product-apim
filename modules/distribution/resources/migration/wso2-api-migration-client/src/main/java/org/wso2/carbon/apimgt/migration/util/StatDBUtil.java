/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.apimgt.migration.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.client.internal.ServiceHolder;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import java.sql.*;

public class StatDBUtil {
    private static volatile DataSource dataSource = null;
    private static final String DATA_SOURCE_NAME = "jdbc/WSO2AM_STATS_DB";

    private static final String TABLE_API_DESTINATION_SUMMARY = "API_DESTINATION_SUMMARY";
    private static final String TABLE_API_FAULT_SUMMARY = "API_FAULT_SUMMARY";
    private static final String TABLE_API_REQUEST_SUMMARY = "API_REQUEST_SUMMARY";
    private static final String TABLE_API_RESOURCE_USAGE_SUMMARY = "API_Resource_USAGE_SUMMARY";
    private static final String TABLE_API_VERSION_USAGE_SUMMARY = "API_VERSION_USAGE_SUMMARY";

    private static final Log log = LogFactory.getLog(StatDBUtil.class);

    public static void initialize() throws APIMigrationException {
        try {
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup(DATA_SOURCE_NAME);
        } catch (NamingException e) {
            throw new APIMigrationException("Error while looking up the data " +
                    "source: " + DATA_SOURCE_NAME, e);
        }
    }


    public static void updateContext() throws APIMigrationException {
        if (dataSource == null) {
            throw new APIMigrationException("Stats Data source is not configured properly.");
        }

        updateResponseSummaryTable();

        executeSQL(getCommonUpdateSQL(TABLE_API_DESTINATION_SUMMARY));
        executeSQL(getCommonUpdateSQL(TABLE_API_FAULT_SUMMARY));
        executeSQL(getCommonUpdateSQL(TABLE_API_REQUEST_SUMMARY));
        executeSQL(getCommonUpdateSQL(TABLE_API_RESOURCE_USAGE_SUMMARY));
        executeSQL(getCommonUpdateSQL(TABLE_API_VERSION_USAGE_SUMMARY));
    }

    private static void updateResponseSummaryTable() {
        Connection connection = null;
        Statement statement = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            String sql = "SELECT CONTEXT, VERSION, API_VERSION FROM API_REQUEST_SUMMARY GROUP BY CONTEXT, VERSION, API_VERSION";

            connection = dataSource.getConnection();
            statement = connection.createStatement();
            connection.setAutoCommit(false);
            statement.setFetchSize(50);
            resultSet = statement.executeQuery(sql);

            preparedStatement = connection.prepareStatement("UPDATE API_RESPONSE_SUMMARY SET CONTEXT = concat(?, '/', ?) " +
                    "WHERE CONTEXT = ? AND API_VERSION = ?");
            while (resultSet.next()) {
                final String context = resultSet.getString("CONTEXT");
                final String version = resultSet.getString("VERSION");

                if (!context.endsWith('/' + version)) {
                    preparedStatement.setString(1, context);
                    preparedStatement.setString(2, version);
                    preparedStatement.setString(3, context);
                    preparedStatement.setString(4, resultSet.getString("API_VERSION"));
                    preparedStatement.addBatch();
                }
            }
            preparedStatement.executeBatch();
            connection.commit();

        } catch (SQLException e) {
            log.error("SQLException when updating API_RESPONSE_SUMMARY table", e);
        }
        finally {
            try {
                if (preparedStatement != null) preparedStatement.close();
                if (statement != null) statement.close();
                if (resultSet != null) resultSet.close();
                if (connection != null) connection.close();
            }
            catch (SQLException e) {
                log.error("SQLException when closing resource", e);
            }
        }
    }

    private static String getCommonUpdateSQL(String table) {
        return "UPDATE " + table + " SET CONTEXT = concat(CONTEXT,'/',VERSION) " +
                "WHERE CONTEXT NOT LIKE concat('%', VERSION)";
    }


    private static void executeSQL(String sql) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            connection.setAutoCommit(false);
            statement.execute(sql);
            connection.commit();

        } catch (SQLException e) {
            log.error("SQLException when executing: " + sql, e);
        }
        finally {
            try {
                if (statement != null) { statement.close(); }
                if (connection != null) { connection.close(); }
            } catch (SQLException e) {
                log.error("SQLException when closing resource", e);
            }

        }
    }
    
    public static boolean isTokenEncryptionEnabled() {
        APIManagerConfiguration config = ServiceHolder.getAPIManagerConfigurationService().getAPIManagerConfiguration();        
        return Boolean.parseBoolean(config.getFirstProperty(Constants.API_KEY_VALIDATOR_ENCRYPT_TOKENS));
    }
}
