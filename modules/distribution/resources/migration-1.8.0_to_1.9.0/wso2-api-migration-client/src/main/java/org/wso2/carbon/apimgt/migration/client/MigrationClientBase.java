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

package org.wso2.carbon.apimgt.migration.client;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.client.internal.ServiceHolder;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public abstract class MigrationClientBase {
    private static final Log log = LogFactory.getLog(MigrationClientBase.class);
    private List<Tenant> tenantsArray;

    public MigrationClientBase(String tenantArguments, String blackListTenantArguments) throws UserStoreException {
        TenantManager tenantManager = ServiceHolder.getRealmService().getTenantManager();

        if (tenantArguments != null) {  // Tenant arguments have been provided so need to load specific ones
            tenantArguments = tenantArguments.replaceAll("\\s", ""); // Remove spaces and tabs

            tenantsArray = new ArrayList();

            buildTenantList(tenantManager, tenantsArray, tenantArguments);
        } else if (blackListTenantArguments != null) {
            blackListTenantArguments = blackListTenantArguments.replaceAll("\\s", ""); // Remove spaces and tabs

            List<Tenant> blackListTenants = new ArrayList();
            buildTenantList(tenantManager, blackListTenants, blackListTenantArguments);

            List<Tenant> allTenants = new ArrayList(Arrays.asList(tenantManager.getAllTenants()));
            Tenant superTenant = new Tenant();
            superTenant.setDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            superTenant.setId(MultitenantConstants.SUPER_TENANT_ID);
            allTenants.add(superTenant);

            tenantsArray = new ArrayList();

            for (Tenant tenant : allTenants) {
                boolean isBlackListed = false;
                for (Tenant blackListTenant : blackListTenants) {
                    if (blackListTenant.getId() == tenant.getId()) {
                        isBlackListed = true;
                        break;
                    }
                }

                if (!isBlackListed) {
                    tenantsArray.add(tenant);
                }
            }

        } else {  // Load all tenants
            tenantsArray = new ArrayList(Arrays.asList(tenantManager.getAllTenants()));
            Tenant superTenant = new Tenant();
            superTenant.setDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            superTenant.setId(MultitenantConstants.SUPER_TENANT_ID);
            tenantsArray.add(superTenant);
        }
    }


    private void buildTenantList(TenantManager tenantManager, List<Tenant> tenantList, String tenantArguments) throws UserStoreException {
        if (tenantArguments.contains(",")) { // Multiple arguments specified
            String[] parts = tenantArguments.split(",");

            for (int i = 0; i < parts.length; ++i) {
                if (parts[i].length() > 0) {
                    populateTenants(tenantManager, tenantList, parts[i]);
                }
            }
        } else { // Only single argument provided
            populateTenants(tenantManager, tenantList, tenantArguments);
        }
    }


    private void populateTenants(TenantManager tenantManager, List<Tenant> tenantList, String argument) throws UserStoreException {
        log.debug("Argument provided : " + argument);

        if (argument.contains("@")) { // Username provided as argument
            int tenantID = tenantManager.getTenantId(argument);

            if (tenantID != -1) {
                tenantList.add(tenantManager.getTenant(tenantID));
            } else {
                log.error("Tenant does not exist for username " + argument);
            }
        } else { // Domain name provided as argument
            Tenant[] tenants = tenantManager.getAllTenantsForTenantDomainStr(argument);

            if (tenants.length > 0) {
                tenantList.addAll(Arrays.asList(tenants));
            } else {
                log.error("Tenant does not exist for domain " + argument);
            }
        }
    }

    protected List<Tenant> getTenantsArray() { return tenantsArray; }

    protected void updateAPIManangerDatabase(String sqlScriptPath) throws SQLException {
        log.info("Database migration for API Manager started");
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        BufferedReader bufferedReader = null;
        try {
            connection = APIMgtDBUtil.getConnection();
            String dbType = MigrationDBCreator.getDatabaseType(connection);

            InputStream is = new FileInputStream(sqlScriptPath + dbType + ".sql");
            bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF8"));
            String sqlQuery = "";
            boolean isFoundQueryEnd = false;
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("//") || line.startsWith("--")) {
                    continue;
                }
                StringTokenizer stringTokenizer = new StringTokenizer(line);
                if (stringTokenizer.hasMoreTokens()) {
                    String token = stringTokenizer.nextToken();
                    if ("REM".equalsIgnoreCase(token)) {
                        continue;
                    }
                }

                if (line.contains("\\n")) {
                    line = line.replace("\\n", "");
                }

                sqlQuery += " " + line;

                if (line.contains(";")) {
                    isFoundQueryEnd = true;
                }

                if (org.wso2.carbon.apimgt.migration.util.Constants.DB_TYPE_ORACLE.equals(dbType)) {
                    sqlQuery = sqlQuery.replace(";", "");
                }

                if (isFoundQueryEnd) {
                    if (sqlQuery.length() > 0) {
                        log.debug("SQL to be executed : " + sqlQuery);
                        preparedStatement = connection.prepareStatement(sqlQuery.trim());
                        preparedStatement.execute();
                        connection.commit();
                    }

                    // Reset variables to read next SQL
                    sqlQuery = "";
                    isFoundQueryEnd = false;
                }
            }

            bufferedReader.close();

        } catch (IOException e) {
            //Errors logged to let user know the state of the db migration and continue other resource migrations
            log.error("Error occurred while migrating databases", e);
        } catch (Exception e) {
            /* MigrationDBCreator extends from org.wso2.carbon.utils.dbcreator.DatabaseCreator and in the super class
            method getDatabaseType throws generic Exception */
            log.error("Error occurred while migrating databases", e);
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        log.info("DB resource migration done for all the tenants");
    }

    /**
     * This method is used to remove the FK constraint which is unnamed
     * This finds the name of the constraint and build the query to delete the constraint and execute it
     *
     * @param sqlScriptPath path of sql script
     * @throws SQLException
     * @throws IOException
     */
    protected void dropFKConstraint(String sqlScriptPath) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Statement statement = null;
        try {
            String queryToExecute = IOUtils.toString(new FileInputStream(new File(sqlScriptPath + "drop-fk.sql")), "UTF-8");
            String queryArray[] = queryToExecute.split(Constants.LINE_BREAK);

            connection = APIMgtDBUtil.getConnection();
            String dbType = MigrationDBCreator.getDatabaseType(connection);
            connection.setAutoCommit(false);
            statement = connection.createStatement();
            if (Constants.DB_TYPE_ORACLE.equals(dbType)) {
                queryArray[0] = queryArray[0].replace(Constants.DELIMITER, "");
            }
            resultSet = statement.executeQuery(queryArray[0]);
            String constraintName = null;

            while (resultSet.next()) {
                constraintName = resultSet.getString("constraint_name");
            }

            if (constraintName != null) {
                queryToExecute = queryArray[1].replace("<temp_key_name>", constraintName);
                if (Constants.DB_TYPE_ORACLE.equals(dbType)) {
                    queryToExecute = queryToExecute.replace(Constants.DELIMITER, "");
                }

                if (queryToExecute.contains("\\n")) {
                    queryToExecute = queryToExecute.replace("\\n", "");
                }
                preparedStatement = connection.prepareStatement(queryToExecute);
                preparedStatement.execute();
                connection.commit();
            }
        } catch (APIMigrationException e) {
            //Foreign key might be already deleted, log the error and let it continue
            log.error("Error occurred while deleting foreign key", e);
        } catch (IOException e) {
            //If user does not add the file migration will continue and migrate the db without deleting
            // the foreign key reference
            log.error("Error occurred while finding the foreign key deletion query for execution", e);
        } catch (Exception e) {
            /* MigrationDBCreator extends from org.wso2.carbon.utils.dbcreator.DatabaseCreator and in the super class
            method getDatabaseType throws generic Exception */
            log.error("Error occurred while deleting foreign key", e);
        } finally {
            if (statement != null) {
                statement.close();
            }

            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, resultSet);
        }

    }
}
