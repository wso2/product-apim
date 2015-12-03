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


import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.client._110Specific.ResourceModifier;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.RegistryService;
import org.wso2.carbon.apimgt.migration.util.ResourceUtil;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;


public class MigrateFrom19to110 extends MigrationClientBase implements MigrationClient {

    private static final Log log = LogFactory.getLog(MigrateFrom19to110.class);
    private RegistryService registryService;

    public MigrateFrom19to110(String tenantArguments, String blackListTenantArguments, RegistryService registryService) throws UserStoreException {
        super(tenantArguments, blackListTenantArguments);
        this.registryService = registryService;
    }

    @Override
    public void databaseMigration(String migrateVersion) throws APIMigrationException, SQLException {
        /*There are no changes in APIM tables, but there are changes in IDN tables, So use the database migration
        /method defined in IS Migration Client */

        String idnScriptPath = CarbonUtils.getCarbonHome() + File.separator + "migration-scripts" + File.separator +
                            "19-110-migration" + File.separator + "idn" + File.separator;

        updateAPIManangerDatabase(idnScriptPath);

        String amScriptPath = CarbonUtils.getCarbonHome() + File.separator + "migration-scripts" + File.separator +
                "19-110-migration" + File.separator + "am" + File.separator;

        updateAPIManangerDatabase(amScriptPath);

        updateAuthzUserName();
    }

    @Override
    public void registryResourceMigration() throws APIMigrationException {
        rxtMigration();

        workflowExtensionsMigration();

        updateTiers();
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


    private void updateAuthzUserName() throws SQLException {
        log.info("Updating Authz UserName for API Manager started");
        Connection connection = null;
        PreparedStatement selectStatement = null;
        ResultSet resultSet = null;

        Vector<Pair<String, String>> updateValues = new Vector<Pair<String, String>>();

        try {
            String selectQuery = "SELECT DISTINCT AUTHZ_USER FROM IDN_OAUTH2_ACCESS_TOKEN WHERE AUTHZ_USER LIKE '%@%'";

            connection = APIMgtDBUtil.getConnection();
            selectStatement = connection.prepareStatement(selectQuery);
            resultSet = selectStatement.executeQuery();

            while (resultSet.next()) {
                String authzUser = resultSet.getString("AUTHZ_USER");

                String usernameWithoutDomain = authzUser.split("@")[0];

                Pair<String, String> pair = Pair.of(usernameWithoutDomain, authzUser);
                updateValues.add(pair);
            }

        } finally {
            APIMgtDBUtil.closeAllConnections(selectStatement, connection, resultSet);
        }

        if (!updateValues.isEmpty()) { // If user names that need to be updated exist
            PreparedStatement updateStatement = null;

            try {
                connection = APIMgtDBUtil.getConnection();

                String updateQuery = "UPDATE IDN_OAUTH2_ACCESS_TOKEN SET AUTHZ_USER = ? WHERE AUTHZ_USER = ?";

                for (Pair<String, String> pair : updateValues) {
                    updateStatement = connection.prepareStatement(updateQuery);
                    updateStatement.setString(1, pair.getLeft());
                    updateStatement.setString(2, pair.getRight());
                    updateStatement.execute();
                }

                connection.commit();
            } finally {
                APIMgtDBUtil.closeAllConnections(updateStatement, connection, null);
            }
        }
        log.info("Updating Authz UserName for API Manager completed");
    }

    /**
     * This method is used to migrate rxt
     * This adds three new attributes to the api rxt
     *
     * @throws APIMigrationException
     */
    private void rxtMigration() throws APIMigrationException {
        log.info("Rxt migration for API Manager started.");
        for (Tenant tenant : getTenantsArray()) {
            log.debug("Start rxtMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");

            GenericArtifact[] artifacts = registryService.getGenericAPIArtifacts(tenant);

            for (GenericArtifact artifact : artifacts) {
                API api = registryService.getAPI(artifact);

                try {
                    if (api == null) {
                        log.error("Cannot find corresponding api for registry artifact " +
                                artifact.getAttribute("overview_name") + "-" +
                                artifact.getAttribute("overview_version") + "-" +
                                artifact.getAttribute("overview_provider") +
                                " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ") in AM_DB");
                        continue;
                    }

                    artifact.addAttribute("overview_endpointAuthType", "Basic Auth");
                } catch (GovernanceException e) {
                    log.error("Error when accessing API artifact in registry", e);
                }
            }

            registryService.updateGenericAPIArtifacts(tenant, artifacts);

            log.debug("End rxtMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
        }

        log.info("Rxt resource migration done for all the tenants");
    }

    /**
     * This method is used to workflow-extensions.xml configuration by handling the addition of the executors
     * to handle work flow executors
     *
     * @throws APIMigrationException
     */
    private void workflowExtensionsMigration() throws APIMigrationException {
        log.info("workflow extensions migration for API Manager started.");
        for (Tenant tenant : getTenantsArray()) {
            log.debug("Start workflow extensions migration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");

            try {
                if (!registryService.isGovernanceRegistryResourceExists(tenant, APIConstants.WORKFLOW_EXECUTOR_LOCATION)) {
                    log.debug("Workflow extensions resource does not exist for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
                    continue;
                }

                String workFlowExtensions = ResourceUtil.getResourceContent(registryService.getGovernanceRegistryResource(tenant,
                                                                            APIConstants.WORKFLOW_EXECUTOR_LOCATION));
                String updatedWorkFlowExtensions = ResourceModifier.modifyWorkFlowExtensions(workFlowExtensions);
                registryService.updateGovernanceRegistryResource(tenant,
                                                    APIConstants.WORKFLOW_EXECUTOR_LOCATION, updatedWorkFlowExtensions);
            } catch (RegistryException e) {
                ResourceUtil.handleException("Error occurred while accessing the registry", e);
            } catch (UserStoreException e) {
                ResourceUtil.handleException("Error occurred while accessing the user store", e);
            }

            log.debug("End workflow extensions for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
        }

        log.info("workflow extensions migration done for all the tenants");
    }

    private void updateTiers() throws APIMigrationException {
        log.info("tier migration for API Manager started.");

        for (Tenant tenant : getTenantsArray()) {
            log.debug("Start tier migration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");

            try {
                if (!registryService.isGovernanceRegistryResourceExists(tenant, APIConstants.API_TIER_LOCATION)) {
                    continue;
                } else {
                    String apiTiers = ResourceUtil.getResourceContent(
                            registryService.getGovernanceRegistryResource(tenant, APIConstants.API_TIER_LOCATION));

                    String updatedApiTiers = ResourceModifier.modifyTiers(apiTiers, APIConstants.API_TIER_LOCATION);
                    registryService.updateGovernanceRegistryResource(tenant,
                                                    APIConstants.API_TIER_LOCATION, updatedApiTiers);
                }

                // Since API tiers.xml has been updated it will be used as app and resource tier.xml
                if (!registryService.isGovernanceRegistryResourceExists(tenant, APIConstants.APP_TIER_LOCATION)) {
                    String apiTiers = ResourceUtil.getResourceContent(
                            registryService.getGovernanceRegistryResource(tenant, APIConstants.API_TIER_LOCATION));

                    registryService.updateGovernanceRegistryResource(tenant,
                                                    APIConstants.APP_TIER_LOCATION, apiTiers);
                }

                if (!registryService.isGovernanceRegistryResourceExists(tenant, APIConstants.RES_TIER_LOCATION)) {
                    String apiTiers = ResourceUtil.getResourceContent(
                            registryService.getGovernanceRegistryResource(tenant, APIConstants.API_TIER_LOCATION));

                    registryService.updateGovernanceRegistryResource(tenant,
                                                    APIConstants.RES_TIER_LOCATION, apiTiers);
                }


            } catch (UserStoreException e) {
                ResourceUtil.handleException("Error occurred while accessing the user store", e);
            } catch (RegistryException e) {
                ResourceUtil.handleException("Error occurred while accessing the registry", e);
            }
        }
        log.info("tier migration for API Manager completed.");
    }

    private void migrateLifeCycles() throws APIMigrationException {
        log.info("migrating life cycles for API Manager started.");

        String apiLifeCycleXMLPath = CarbonUtils.getCarbonHome() + APIConstants.RESOURCE_FOLDER_LOCATION +
                File.separator + Constants.LIFE_CYCLES_FOLDER + File.separator +
                APIConstants.API_LIFE_CYCLE + ".xml";

        String executorlessApiLifeCycle = ResourceModifier.removeExecutorsFromAPILifeCycle(apiLifeCycleXMLPath);

        for (Tenant tenant : getTenantsArray()) {
            log.debug("Start life cycle migration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");

            try {
                final String apiLifeCycleRegistryPath = RegistryConstants.CONFIG_REGISTRY_BASE_PATH +
                        RegistryConstants.LIFECYCLE_CONFIGURATION_PATH + APIConstants.API_LIFE_CYCLE;

                if (registryService.isConfigRegistryResourceExists(tenant, apiLifeCycleRegistryPath))
                {
                    String apiLifeCycle = ResourceUtil.getResourceContent(
                            registryService.getConfigRegistryResource(tenant, apiLifeCycleRegistryPath));




                }
            } catch (UserStoreException e) {
                ResourceUtil.handleException("Error occurred while accessing the user store", e);
            } catch (RegistryException e) {
                ResourceUtil.handleException("Error occurred while accessing the registry", e);
            }

        }
    }
}
