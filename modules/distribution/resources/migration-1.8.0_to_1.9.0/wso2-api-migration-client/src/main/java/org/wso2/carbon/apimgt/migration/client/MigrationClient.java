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

import org.wso2.carbon.apimgt.migration.APIMigrationException;
import java.sql.SQLException;

/**
 * Public interface for all migrations.
 * All the migrations after 1.8.0 to 1.9.0 migrations
 * Migration handled in three different steps as Database migrations, registry resource migrations and
 * file system resource migrations
 *
 */
@SuppressWarnings("unused")
public interface MigrationClient {

    /**
     * This method is used to migrate databases. This method adds the newly added columns, tables and alters the tables
     * according to the new database
     *
     * @param migrateVersion version to be migrated
     * @throws APIMigrationException
     * @throws SQLException
     */
    public void databaseMigration(String migrateVersion) throws APIMigrationException, SQLException;

    /**
     * This method is used to migrate all the registry resources
     * Swagger, RXTs and all other registry resources will be migrated
     *
     * @throws APIMigrationException
     */
    public void registryResourceMigration() throws APIMigrationException;


    /**
     * This method is used to migrate all file system resources.
     * Synapse APIs and sequences will be migrate from ths
     *
     * @throws APIMigrationException
     */
    public void fileSystemMigration() throws APIMigrationException;


    /**
     * This method is used to clean old resources from registry.
     * All older registry resources will be removed here
     *
     * @throws APIMigrationException
     */
    public void cleanOldResources() throws APIMigrationException;

}
