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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.client.MigrateFrom18to19;
import org.wso2.carbon.apimgt.migration.client.MigrationClient;
import org.wso2.carbon.apimgt.migration.util.Constants;
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
        String migrateVersion = null;
        boolean cleanupNeeded = false;
        boolean isDBMigrationNeeded = false;
        boolean isRegistryMigrationNeeded = false;
        boolean isFileSystemMigrationNeeded = false;

        try {
            APIMgtDBUtil.initialize();
        } catch (Exception e) {
            //APIMgtDBUtil.initialize() throws generic exception
            log.error("Error occurred while initializing DB Util " + e.getMessage());
        }

        Map<String, String> argsMap = new HashMap<String, String>();
        argsMap.put("migrateVersion", System.getProperty("migrate"));
        argsMap.put("isCleanUpNeeded", System.getProperty("cleanup"));
        argsMap.put("isDBMigrationNeeded", System.getProperty("migrateDB"));
        argsMap.put("isRegMigrationNeeded", System.getProperty("migrateReg"));
        argsMap.put("isFileSysMigrationNeeded", System.getProperty("migrateFS"));


        if (!argsMap.isEmpty()) {
            migrateVersion = argsMap.get("migrateVersion");
            if (argsMap.get("isCleanUpNeeded") != null) {
                cleanupNeeded = Boolean.parseBoolean(argsMap.get("isCleanUpNeeded"));
            }
            if (argsMap.get("isDBMigrationNeeded") != null) {
                isDBMigrationNeeded = Boolean.parseBoolean(argsMap.get("isDBMigrationNeeded"));
            }
            if (argsMap.get("isRegMigrationNeeded") != null) {
                isRegistryMigrationNeeded = Boolean.parseBoolean(argsMap.get("isRegMigrationNeeded"));
            }
            if (argsMap.get("isFileSysMigrationNeeded") != null) {
                isFileSystemMigrationNeeded = Boolean.parseBoolean(argsMap.get("isFileSysMigrationNeeded"));
            }
        }

        try {
            if (migrateVersion != null) {
                if (Constants.VERSION_1_9.equalsIgnoreCase(migrateVersion)) {
                    log.info("Migrating WSO2 API Manager 1.8.0 resources to WSO2 API Manager 1.9.0");

                    // Create a thread and wait till the APIManager DBUtils is initialized

                    MigrationClient migrateFrom18to19 = new MigrateFrom18to19();

                    //Default operation will migrate all three types of resources
                    if (argsMap.get("isDBMigrationNeeded") == null && argsMap.get("isRegMigrationNeeded") == null && argsMap.get("isFileSysMigrationNeeded") == null) {
                        log.info("Migrating WSO2 API Manager 1.8.0 resources to WSO2 API Manager 1.9.0");
                        migrateFrom18to19.databaseMigration(migrateVersion);
                        migrateFrom18to19.registryResourceMigration();
                        migrateFrom18to19.fileSystemMigration();
                    } else {
                        //Only performs database migration
                        if (isDBMigrationNeeded) {
                            log.info("Migrating WSO2 API Manager 1.8.0 databases to WSO2 API Manager 1.9.0");
                            migrateFrom18to19.databaseMigration(migrateVersion);
                        }
                        //Only performs registry migration
                        if (isRegistryMigrationNeeded) {
                            log.info("Migrating WSO2 API Manager 1.8.0 registry resources to WSO2 API Manager 1.9.0");
                            migrateFrom18to19.registryResourceMigration();
                        }
                        //Only performs file system migration
                        if (isFileSystemMigrationNeeded) {
                            log.info("Migrating WSO2 API Manager 1.8.0 file system resources to WSO2 API Manager 1.9.0");
                            migrateFrom18to19.fileSystemMigration();
                        }
                    }
                    //Old resource cleanup
                    if (cleanupNeeded) {
                        migrateFrom18to19.cleanOldResources();
                        log.info("Old resources cleaned up.");
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("API Manager 1.8.0 to 1.9.0 migration successfully completed");
                    }
                } else {
                    log.error("The given migrate version " + migrateVersion + " is not supported. Please check the version and try again.");
                }
            }
        } catch (APIMigrationException e) {
            log.error("API Management  exception occurred while migrating " + e.getMessage());
        } catch (UserStoreException e) {
            log.error("User store  exception occurred while migrating " + e.getMessage());
        } catch (SQLException e) {
            log.error("SQL exception occurred while migrating " + e.getMessage());
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
