/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.apimgt.migration.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.migration.DocFileMigration;
import org.wso2.carbon.apimgt.migration.Swagger18Migration;
import org.wso2.carbon.apimgt.migration.Swagger19Migration;
import org.wso2.carbon.apimgt.migration.SwaggerResMigration;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.service.TenantRegistryLoader;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * @scr.component name="org.wso2.carbon.apimgt.migration" immediate="true"
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
            log.error("Error occurred while initializing DB Util " + e.getMessage());
        }
        String migrateVersion = System.getProperty("migrate");
        log.info("Migrate version => " + migrateVersion);
        if (migrateVersion != null && migrateVersion.equalsIgnoreCase(Constants.VERSION_1_6)) {
            log.info("Migrating APIM 1.6 swagger and documentation resources to APIM 1.7");
            SwaggerResMigration swaggerMigration = new SwaggerResMigration();
            DocFileMigration docMigration = new DocFileMigration();
            try {
                swaggerMigration.migrate();
                if(log.isDebugEnabled()) {
                    log.debug("Swagger migration successfully completed");
                }
                docMigration.migrate();
                if(log.isDebugEnabled()) {
                    log.debug("Document file migration successfully completed");
                }
            } catch (UserStoreException e) {
                log.error("User store exception occurred while migrating " + e.getMessage());
            }
        } else if (migrateVersion != null && migrateVersion.equalsIgnoreCase(Constants.VERSION_1_7)) {
            log.info("Migrating APIM 1.7 Swagger resources to APIM 1.8");
            // Create a thread and wait till the APIM DBUtils is initialized
            try {
                Swagger18Migration swagger18Migration = new Swagger18Migration();
                swagger18Migration.migrate();
                if(log.isDebugEnabled()) {
                    log.debug("Swagger migration successfully completed");
                }
            } catch (UserStoreException e) {
                log.error("User store exception occurred while migrating " + e.getMessage());
            } catch (InterruptedException e) {
                log.error("Interrupted exception occurred while migrating " + e.getMessage());
            }

        } else if (migrateVersion != null && migrateVersion.equalsIgnoreCase(Constants.VERSION_1_8)) {
            log.info("Migrating APIM 1.8 Swagger resources to APIM 1.9");
            // Create a thread and wait till the APIM DBUtils is initialized
            try {
                Swagger19Migration swagger19Migration = new Swagger19Migration();
                swagger19Migration.migrate();
                if(log.isDebugEnabled()) {
                    log.debug("Swagger migration successfully completed");
                }
            } catch (UserStoreException e) {
                log.error("User store exception occurred while migrating " + e.getMessage());
            } catch (InterruptedException e) {
                log.error("Inttionerrupted excep occurred while migrating " + e.getMessage());
            }
        }
        log.info("APIM migration bundle activated successfully");
    }

    /**
     * Method to deactivate bundle.
     *
     * @param context OSGi component context.
     */
    protected void deactivate(ComponentContext context) {
        log.info("APIM migration bundle is deactivated");
    }

    /**
     * Method to set registry service.
     *
     * @param registryService service to get tenant data.
     */
    protected void setRegistryService(RegistryService registryService) {
        if(log.isDebugEnabled()) {
            log.debug("Setting RegistryService for APIM migration");
        }
        ServiceHolder.setRegistryService(registryService);
    }

    /**
     * Method to unset registry service.
     *
     * @param registryService service to get registry data.
     */
    protected void unsetRegistryService(RegistryService registryService) {
        if(log.isDebugEnabled()) {
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
        log.debug("Setting RealmService for APIM migration");
        ServiceHolder.setRealmService(realmService);
    }

    /**
     * Method to unset realm service.
     *
     * @param realmService service to get tenant data.
     */
    protected void unsetRealmService(RealmService realmService) {
        if(log.isDebugEnabled()) {
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
        log.debug("Setting TenantRegistryLoader for APIM migration");
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