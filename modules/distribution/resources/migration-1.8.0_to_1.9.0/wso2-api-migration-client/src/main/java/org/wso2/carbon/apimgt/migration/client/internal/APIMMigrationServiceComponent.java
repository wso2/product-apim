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

package org.wso2.carbon.apimgt.migration.client.internal;

import com.sun.tools.jxc.apt.Const;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.client.MigrateFrom18to19;
import org.wso2.carbon.apimgt.migration.client.MigrationClient;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.ResourceUtil;
import org.wso2.carbon.apimgt.migration.util.StatDBUtil;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.service.TenantRegistryLoader;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


/**
 * @scr.component name="org.wso2.carbon.apimgt.migration.client" immediate="true"
 * @scr.reference name="realm.service"
 * interface="org.wso2.carbon.user.core.service.RealmService" cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="registry.core.dscomponent"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="tenant.registryloader" interface="org.wso2.carbon.registry.core.service.TenantRegistryLoader" cardinality="1..1"
 * policy="dynamic" bind="setTenantRegistryLoader" unbind="unsetTenantRegistryLoader"
 * @scr.reference name="apim.configuration" interface="org.wso2.carbon.apimgt.impl.APIManagerConfigurationService" cardinality="1..1"
 * policy="dynamic" bind="setApiManagerConfig" unbind="unsetApiManagerConfig"
 */

@SuppressWarnings("unused")
public class APIMMigrationServiceComponent {

    private static final Log log = LogFactory.getLog(APIMMigrationServiceComponent.class);

    /**
     * Method to activate bundle.
     *
     * @param context OSGi component context.
     */
    protected void activate(ComponentContext context) {
        try {
            APIMgtDBUtil.initialize();
        } catch (Exception e) {
            //APIMgtDBUtil.initialize() throws generic exception
            log.error("Error occurred while initializing DB Util ", e);
        }

        String migrateToVersion = System.getProperty(Constants.ARG_MIGRATE_TO_VERSION);
        boolean migrateAll = Boolean.parseBoolean(System.getProperty(Constants.ARG_MIGRATE_ALL));
        boolean cleanupNeeded = Boolean.parseBoolean(System.getProperty(Constants.ARG_CLEANUP));
        boolean isDBMigration = Boolean.parseBoolean(System.getProperty(Constants.ARG_MIGRATE_DB));
        boolean isRegistryMigration = Boolean.parseBoolean(System.getProperty(Constants.ARG_MIGRATE_REG));
        boolean isFileSystemMigration = Boolean.parseBoolean(System.getProperty(Constants.ARG_MIGRATE_FILE_SYSTEM));
        boolean isStatMigration = Boolean.parseBoolean(System.getProperty(Constants.ARG_MIGRATE_STATS));

        try {
            if (migrateToVersion != null) {
                if (Constants.VERSION_1_9.equalsIgnoreCase(migrateToVersion)) {
                    log.info("Migrating WSO2 API Manager 1.8.0 resources to WSO2 API Manager 1.9.0");

                    // Create a thread and wait till the APIManager DBUtils is initialized

                    MigrationClient migrateFrom18to19 = new MigrateFrom18to19();

                    //Default operation will migrate all three types of resources
                    if (migrateAll) {
                        log.info("Migrating WSO2 API Manager 1.8.0 resources to WSO2 API Manager 1.9.0");
                        migrateFrom18to19.databaseMigration(migrateToVersion);
                        migrateFrom18to19.registryResourceMigration();
                        migrateFrom18to19.fileSystemMigration();
                    } else {
                        //Only performs database migration
                        if (isDBMigration) {
                            log.info("Migrating WSO2 API Manager 1.8.0 databases to WSO2 API Manager 1.9.0");
                            migrateFrom18to19.databaseMigration(migrateToVersion);
                        }
                        //Only performs registry migration
                        if (isRegistryMigration) {
                            log.info("Migrating WSO2 API Manager 1.8.0 registry resources to WSO2 API Manager 1.9.0");
                            migrateFrom18to19.registryResourceMigration();
                        }
                        //Only performs file system migration
                        if (isFileSystemMigration) {
                            log.info("Migrating WSO2 API Manager 1.8.0 file system resources to WSO2 API Manager 1.9.0");
                            migrateFrom18to19.fileSystemMigration();
                        }
                    }
                    //Old resource cleanup
                    if (cleanupNeeded) {
                        migrateFrom18to19.cleanOldResources();
                        log.info("Old resources cleaned up.");
                    }

                    if (isStatMigration) {
                        StatDBUtil.initialize();
                        migrateFrom18to19.statsMigration();
                        log.info("Stat migration completed");
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("API Manager 1.8.0 to 1.9.0 migration successfully completed");
                    }
                } else {
                    log.error("The given migrate version " + migrateToVersion + " is not supported. Please check the version and try again.");

                }
            }
            else {
                if (migrateAll || cleanupNeeded || isDBMigration || isRegistryMigration || isFileSystemMigration) {
                    log.error("The property " + Constants.ARG_MIGRATE_TO_VERSION + " has not been specified . Please specify the property and try again.");
                }
            }
        } catch (APIMigrationException e) {
            log.error("API Management  exception occurred while migrating", e);
        } catch (UserStoreException e) {
            log.error("User store  exception occurred while migrating", e);
        } catch (SQLException e) {
            log.error("SQL exception occurred while migrating", e);
        }
        log.info("WSO2 API Manager migration component successfully activated.");
    }

    /**
     * Method to deactivate bundle.
     *
     * @param context OSGi component context.
     */
    protected void deactivate(ComponentContext context) {
        log.info("WSO2 API Manager migration bundle is deactivated");
    }

    /**
     * Method to set registry service.
     *
     * @param registryService service to get tenant data.
     */
    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting RegistryService for WSO2 API Manager migration");
        }
        ServiceHolder.setRegistryService(registryService);
    }

    /**
     * Method to unset registry service.
     *
     * @param registryService service to get registry data.
     */
    protected void unsetRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("Unset Registry service");
        }
        ServiceHolder.setRegistryService(null);
    }

    /**
     * Method to set realm service.
     *
     * @param realmService service to get tenant data.
     */
    protected void setRealmService(RealmService realmService) {
        log.debug("Setting RealmService for WSO2 API Manager migration");
        ServiceHolder.setRealmService(realmService);
    }

    /**
     * Method to unset realm service.
     *
     * @param realmService service to get tenant data.
     */
    protected void unsetRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("Unset Realm service");
        }
        ServiceHolder.setRealmService(null);
    }

    /**
     * Method to set tenant registry loader
     *
     * @param tenantRegLoader tenant registry loader
     */
    protected void setTenantRegistryLoader(TenantRegistryLoader tenantRegLoader) {
        log.debug("Setting TenantRegistryLoader for WSO2 API Manager migration");
        ServiceHolder.setTenantRegLoader(tenantRegLoader);
    }

    /**
     * Method to unset tenant registry loader
     *
     * @param tenantRegLoader tenant registry loader
     */
    protected void unsetTenantRegistryLoader(TenantRegistryLoader tenantRegLoader) {
        log.debug("Unset Tenant Registry Loader");
        ServiceHolder.setTenantRegLoader(null);
    }

    /**
     * Method to set API Manager configuration
     *
     * @param apiManagerConfig api manager configuration
     */
    protected void setApiManagerConfig(APIManagerConfigurationService apiManagerConfig) {
        log.info("Setting APIManager configuration");
    }

    /**
     * Method to unset API manager configuration
     *
     * @param apiManagerConfig api manager configuration
     */
    protected void unsetApiManagerConfig(APIManagerConfigurationService apiManagerConfig) {
        log.info("Un-setting APIManager configuration");
    }

}
