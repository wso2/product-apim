/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.apimgt.migration.client;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.StatDBUtil;

import java.sql.SQLException;
import java.util.*;

public final class MigrationExecutor {
    private static final Log log = LogFactory.getLog(MigrationExecutor.class);

    public static class Arguments {
        private String migrateFromVersion;
        private String specificVersion;
        private String component;
        private String options;
        private boolean migrateAll;
        private boolean cleanupNeeded;
        private boolean isDBMigration;
        private boolean isRegistryMigration;
        private boolean isFileSystemMigration;
        private boolean isStatMigration;

        public void setMigrateAll(boolean migrateAll) {
            this.migrateAll = migrateAll;
        }

        public void setCleanupNeeded(boolean cleanupNeeded) {
            this.cleanupNeeded = cleanupNeeded;
        }

        public void setDBMigration(boolean DBMigration) {
            isDBMigration = DBMigration;
        }

        public void setRegistryMigration(boolean registryMigration) {
            isRegistryMigration = registryMigration;
        }

        public void setFileSystemMigration(boolean fileSystemMigration) {
            isFileSystemMigration = fileSystemMigration;
        }

        public void setStatMigration(boolean statMigration) {
            isStatMigration = statMigration;
        }

        public void setMigrateFromVersion(String migrateFromVersion) {
            this.migrateFromVersion = migrateFromVersion;
        }

        public void setSpecificVersion(String specificVersion) {
            this.specificVersion = specificVersion;
        }

        public void setComponent(String component) {
            this.component = component;
        }

        public String getOptions() {
            return options;
        }

        public void setOptions(String options) {
            this.options = options;
        }
    }


    public static void execute(Arguments arguments) throws APIMigrationException,
            SQLException {
        if (arguments.component != null && arguments.component.contains(Constants.APIM_COMPONENT)) {
            if (arguments.migrateFromVersion != null) {
                MigrationClient[] migrationClients = MigrationClientFactory.getAllClients(arguments.migrateFromVersion);

                if (migrationClients.length > 0) {
                    log.info("Starting WSO2 API Manager migration");

                    for (MigrationClient migrationClient : migrationClients) {
                        invoke(migrationClient, arguments);
                    }

                    log.info("Ending WSO2 API Manager migration");
                } else {
                    log.error("Migrating from " + arguments.migrateFromVersion +
                            " is not supported. Please check the version and try again.");
                }
            }
            else if (arguments.specificVersion != null) {
                MigrationClient migrationClient = MigrationClientFactory.getClient(arguments.specificVersion);

                if (migrationClient != null) {
                    invoke(migrationClient, arguments);
                } else {
                    log.error("The given migration version " + arguments.specificVersion +
                            " is not supported. Please check the version and try again.");
                }
            } else { // Migration version not specified
                if (arguments.migrateAll || arguments.cleanupNeeded || arguments.isDBMigration ||
                        arguments.isRegistryMigration || arguments.isFileSystemMigration) {
                    log.error("The property -D" + Constants.ARG_MIGRATE_FROM_VERSION + " or -D" +
                            Constants.ARG_RUN_SPECIFIC_VERSION +
                            " has not been specified. Please specify the property you wish to use and try again.");
                }
            }
        }
    }


    private static void invoke(MigrationClient migrationClient, Arguments arguments) throws APIMigrationException,
                                                                    SQLException {
        //Default operation will migrate all three types of resources
        if (arguments.migrateAll) {
            log.info("Migrating All WSO2 API Manager resources");
            migrationClient.databaseMigration();
            migrationClient.registryResourceMigration();
            migrationClient.fileSystemMigration();
        } else {
            //Only performs database migration
            if (arguments.isDBMigration) {
                log.info("Migrating WSO2 API Manager databases");
                migrationClient.databaseMigration();
            }
            //Only performs registry migration
            if (arguments.isRegistryMigration) {
                log.info("Migrating WSO2 API Manager registry resources");
                migrationClient.registryResourceMigration();
            }
            //Only performs file system migration
            if (arguments.isFileSystemMigration) {
                log.info("Migrating WSO2 API Manager file system resources");
                migrationClient.fileSystemMigration();
            }
        }
        //Old resource cleanup
        if (arguments.cleanupNeeded) {
            migrationClient.cleanOldResources();
            log.info("Old resources cleaned up.");
        }

        if (arguments.isStatMigration) {
            StatDBUtil.initialize();
            migrationClient.statsMigration();
            log.info("Stat migration completed");
        }

        List<String> options = parseOptions(arguments.getOptions());
        if (options != null && options.size() > 0) {
            StatDBUtil.initialize();
            migrationClient.optionalMigration(options);
            log.info("optional migration completed");
        }
    }

    private static List<String> parseOptions(String arguments) {
        if (StringUtils.isBlank(arguments)) {
            return Collections.emptyList();
        }
        List<String> options = null;
        try {
            String[] optionsList = arguments.split(",");
            options= Arrays.asList(optionsList);
        } catch (Exception e) {
            log.error("error parsing options arguments");
            log.warn("Skipping options migration");
        }
        return options;
    }
}
