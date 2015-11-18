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


import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.client.internal.ServiceHolder;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.ResourceUtil;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.utils.CarbonUtils;


import java.io.File;
import java.sql.SQLException;


public class MigrateFrom19to110 extends MigrationClientBase implements MigrationClient {

    private static final Log log = LogFactory.getLog(MigrateFrom19to110.class);

    public MigrateFrom19to110(String tenantArguments, String blackListTenantArguments) throws UserStoreException {
        super(tenantArguments, blackListTenantArguments);
    }

    @Override
    public void databaseMigration(String migrateVersion) throws APIMigrationException, SQLException {
        /*There are no changes in APIM tables, but there are changes in IDN tables, So use the database migration
        /method defined in IS Migration Client */

        String idnScriptPath = CarbonUtils.getCarbonHome() + File.separator + "migration-scripts" + File.separator +
                            "19-110-migration" + File.separator + "idn" + File.separator;

        updateAPIManangerDatabase(idnScriptPath);
    }

    @Override
    public void registryResourceMigration() throws APIMigrationException {
        rxtMigration();
    }

    @Override
    public void fileSystemMigration() throws APIMigrationException {

    }

    @Override
    public void cleanOldResources() throws APIMigrationException {

    }

    @Override
    public void statsMigration() throws APIMigrationException {

    }






    /**
     * This method is used to migrate rxt
     * This adds three new attributes to the api rxt
     *
     * @throws APIMigrationException
     */
    private void rxtMigration() throws APIMigrationException {
        log.info("Rxt migration for API Manager " + org.wso2.carbon.apimgt.migration.util.Constants.VERSION_1_9 + " started.");
        boolean isTenantFlowStarted = false;
        for (Tenant tenant : getTenantsArray()) {
            log.debug("Start rxtMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
            try {
                PrivilegedCarbonContext.startTenantFlow();
                isTenantFlowStarted = true;

                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenant.getId(), true);

                String adminName = ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId())
                        .getRealmConfiguration().getAdminUserName();

                log.debug("Tenant admin username : " + adminName);
                ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
                Registry registry = ServiceHolder.getRegistryService().getGovernanceUserRegistry(adminName, tenant
                        .getId());
                GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);

                if (artifactManager != null) {
                    GovernanceUtils.loadGovernanceArtifacts((UserRegistry) registry);
                    GenericArtifact[] artifacts = artifactManager.getAllGenericArtifacts();
                    for (GenericArtifact artifact : artifacts) {
                        API api = APIUtil.getAPI(artifact, registry);

                        if (api == null) {
                            log.error("Cannot find corresponding api for registry artifact " +
                                    artifact.getAttribute("overview_name") + "-" +
                                    artifact.getAttribute("overview_version") + "-" +
                                    artifact.getAttribute("overview_provider") +
                                    " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ") in AM_DB");
                            continue;
                        }

                        artifact.addAttribute("overview_endpointAuthType", "Basic Auth");

                        artifactManager.updateGenericArtifact(artifact);
                    }
                } else {
                    log.debug("No api artifacts found in registry for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
                }
            } catch (APIManagementException e) {
                ResourceUtil.handleException("Error occurred while reading API from the artifact ", e);
            } catch (RegistryException e) {
                ResourceUtil.handleException("Error occurred while accessing the registry", e);
            } catch (UserStoreException e) {
                ResourceUtil.handleException("Error occurred while reading tenant information", e);
            } finally {
                if (isTenantFlowStarted) {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            }
            log.debug("End rxtMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
        }

        log.info("Rxt resource migration done for all the tenants");
    }
}
