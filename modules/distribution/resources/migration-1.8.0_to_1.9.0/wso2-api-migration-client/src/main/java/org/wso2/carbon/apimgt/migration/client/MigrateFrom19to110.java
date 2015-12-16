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
import org.wso2.carbon.apimgt.api.model.APIStatus;
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
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.CarbonUtils;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;


public class MigrateFrom19to110 extends MigrationClientBase implements MigrationClient {

    private static final Log log = LogFactory.getLog(MigrateFrom19to110.class);
    private RegistryService registryService;

    private static class Pair {
        Pair(String usernameWithoutDomain, String authzUser) {
            this.usernameWithoutDomain = usernameWithoutDomain;
            this.authzUser = authzUser;
        }
        public String usernameWithoutDomain;
        public String authzUser;
    }

    public MigrateFrom19to110(String tenantArguments, String blackListTenantArguments,
                              RegistryService registryService, TenantManager tenantManager) throws UserStoreException {
        super(tenantArguments, blackListTenantArguments, tenantManager);
        this.registryService = registryService;
    }

    @Override
    public void databaseMigration() throws APIMigrationException, SQLException {
        String amScriptPath = CarbonUtils.getCarbonHome() + File.separator + "migration-scripts" + File.separator +
                "19-110-migration" + File.separator;

        updateAPIManangerDatabase(amScriptPath);

        updateAuthzUserName();
    }

    @Override
    public void registryResourceMigration() throws APIMigrationException {
        rxtMigration();

        workflowExtensionsMigration();

        updateTiers();

        migrateLifeCycles();
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

        ArrayList<Pair> updateValues = new ArrayList<>();
        StringBuilder whenConditions = new StringBuilder();
        try {
            String selectQuery = "SELECT DISTINCT AUTHZ_USER FROM IDN_OAUTH2_ACCESS_TOKEN WHERE AUTHZ_USER LIKE '%@%'";

            connection = APIMgtDBUtil.getConnection();
            selectStatement = connection.prepareStatement(selectQuery);
            resultSet = selectStatement.executeQuery();

            while (resultSet.next()) {
                String authzUser = resultSet.getString("AUTHZ_USER");

                String usernameWithoutDomain = authzUser.split("@")[0];

                Pair pair = new Pair(usernameWithoutDomain, authzUser);
                updateValues.add(pair);

                whenConditions.append(System.lineSeparator());
                whenConditions.append("WHEN AUTHZ_USER = ? THEN ?");
            }

            whenConditions.append(System.lineSeparator());
            whenConditions.append("END");

        } finally {
            APIMgtDBUtil.closeAllConnections(selectStatement, connection, resultSet);
        }

        if (!updateValues.isEmpty()) { // If user names that need to be updated exist
            PreparedStatement updateStatement = null;

            try {
                connection = APIMgtDBUtil.getConnection();

                String updateQuery = "UPDATE IDN_OAUTH2_ACCESS_TOKEN SET AUTHZ_USER = CASE " + whenConditions.toString();
                updateStatement = connection.prepareStatement(updateQuery);

                int i = 1;
                for (Pair pair : updateValues) {
                    updateStatement.setString(i++, pair.usernameWithoutDomain);
                    updateStatement.setString(i++, pair.authzUser);
                }

                //String updateQuery = "UPDATE IDN_OAUTH2_ACCESS_TOKEN SET AUTHZ_USER = ? WHERE AUTHZ_USER = ?";
                //final StringBuilder updateQuery = new StringBuilder("UPDATE IDN_OAUTH2_ACCESS_TOKEN SET AUTHZ_USER = CASE ");

                //for (Pair pair : updateValues) {
                //    updateQuery.append(" WHEN AUTHZ_USER = '");
                //    updateQuery.append(pair.authzUser);
                //    updateQuery.append("' THEN '");
                //    updateQuery.append(pair.usernameWithoutDomain);
                    /*
                    updateStatement = connection.prepareStatement(updateQuery);
                    updateStatement.setString(1, pair.usernameWithoutDomain);
                    updateStatement.setString(2, pair.authzUser);
                    updateStatement.execute();
                    */
                //}


                //updateQuery.append(" END");;
                updateStatement.execute();

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
            log.debug("Start rxtMigration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');

            try {
                registryService.startTenantFlow(tenant);

                GenericArtifact[] artifacts = registryService.getGenericAPIArtifacts();

                for (GenericArtifact artifact : artifacts) {
                    artifact.setAttribute("overview_endpointAuthType", "Basic Auth");

                    //updateResourcePermissions(artifact, tenant);
                }

                registryService.updateGenericAPIArtifacts(artifacts);

                log.debug("End rxtMigration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            }
            catch (GovernanceException e) {
                log.error("Error when accessing API artifact in registry", e);
            }
            finally {
                registryService.endTenantFlow();
            }
        }

        log.info("Rxt resource migration done for all the tenants");
    }

    /*
    private void updateResourcePermissions(GenericArtifact artifact, Tenant tenant) throws APIMigrationException {
        API api = registryService.getAPI(artifact);

        if (api != null) {
            APIIdentifier apiIdentifier = api.getId();
            String apiName = apiIdentifier.getApiName();
            String apiVersion = apiIdentifier.getVersion();
            String apiProviderName = apiIdentifier.getProviderName();

            String swagger2Path = ResourceUtil.getSwagger2ResourceLocation(apiName, apiVersion, apiProviderName);

            try {
                registryService.setGovernanceRegistryResourcePermissions(apiProviderName, null, null, swagger2Path);
            } catch (APIManagementException e) {
                log.error("Error when setting permissions in registry for " + swagger2Path, e);
            }

            String apiArtifactPath = null;

            try {
                apiArtifactPath = registryService.getGenericArtifactPath(artifact);
            } catch (UserStoreException e) {
                log.error("User Store Error when getting generic artifacts path", e);
            } catch (RegistryException e) {
                log.error("Registry Error when getting generic artifacts path", e);
            }

            if (apiArtifactPath != null) {
                try {
                    String visibleRolesList = api.getVisibleRoles();
                    String[] visibleRoles = new String[0];
                    if (visibleRolesList != null) {
                        visibleRoles = visibleRolesList.split(",");
                    }
                    registryService.setGovernanceRegistryResourcePermissions(api.getId().getProviderName(),
                                                                api.getVisibility(), visibleRoles, apiArtifactPath);
                } catch (APIManagementException e) {
                    log.error("Error when setting permissions in registry for " + apiArtifactPath, e);
                }
            }
        }
        else {
            try {
                log.error("Cannot find corresponding api for registry artifact " + artifact.getAttribute("overview_name")
                        + "-" + artifact.getAttribute("overview_version") + "-" + artifact.getAttribute("overview_provider") +
                        " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ") in AM_DB");
            } catch (GovernanceException e) {
                log.error("Cannot find corresponding api for registry artifact of tenant "
                        + tenant.getId() + "(" + tenant.getDomain() + ") in AM_DB");

            }
        }
    }
    */

    /**
     * This method is used to workflow-extensions.xml configuration by handling the addition of the executors
     * to handle work flow executors
     *
     * @throws APIMigrationException
     */
    private void workflowExtensionsMigration() throws APIMigrationException {
        log.info("workflow extensions migration for API Manager started.");
        for (Tenant tenant : getTenantsArray()) {
            log.debug("Start workflow extensions migration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');

            try {
                registryService.startTenantFlow(tenant);

                if (!registryService.isGovernanceRegistryResourceExists(APIConstants.WORKFLOW_EXECUTOR_LOCATION)) {
                    log.debug("Workflow extensions resource does not exist for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
                    continue;
                }

                String workFlowExtensions = ResourceUtil.getResourceContent(registryService.getGovernanceRegistryResource(
                                                                            APIConstants.WORKFLOW_EXECUTOR_LOCATION));
                String updatedWorkFlowExtensions = ResourceModifier.modifyWorkFlowExtensions(workFlowExtensions);
                registryService.updateGovernanceRegistryResource(
                                                    APIConstants.WORKFLOW_EXECUTOR_LOCATION, updatedWorkFlowExtensions);
            } catch (RegistryException e) {
                ResourceUtil.handleException("Error occurred while accessing the registry", e);
            } catch (UserStoreException e) {
                ResourceUtil.handleException("Error occurred while accessing the user store", e);
            }
            finally {
                registryService.endTenantFlow();
            }

            log.debug("End workflow extensions for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
        }

        log.info("workflow extensions migration done for all the tenants");
    }

    private void updateTiers() throws APIMigrationException {
        log.info("tier migration for API Manager started.");

        for (Tenant tenant : getTenantsArray()) {
            log.debug("Start tier migration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');

            try {
                registryService.startTenantFlow(tenant);

                if (!registryService.isGovernanceRegistryResourceExists(APIConstants.API_TIER_LOCATION)) {
                    continue;
                } else {
                    String apiTiers = ResourceUtil.getResourceContent(
                            registryService.getGovernanceRegistryResource(APIConstants.API_TIER_LOCATION));

                    String updatedApiTiers = ResourceModifier.modifyTiers(apiTiers, APIConstants.API_TIER_LOCATION);
                    registryService.updateGovernanceRegistryResource(
                                                    APIConstants.API_TIER_LOCATION, updatedApiTiers);
                }

                // Since API tiers.xml has been updated it will be used as app and resource tier.xml
                if (!registryService.isGovernanceRegistryResourceExists(APIConstants.APP_TIER_LOCATION)) {
                    String apiTiers = ResourceUtil.getResourceContent(
                            registryService.getGovernanceRegistryResource(APIConstants.API_TIER_LOCATION));

                    registryService.addGovernanceRegistryResource(
                                                    APIConstants.APP_TIER_LOCATION, apiTiers, "application/xml");
                }

                if (!registryService.isGovernanceRegistryResourceExists(APIConstants.RES_TIER_LOCATION)) {
                    String apiTiers = ResourceUtil.getResourceContent(
                            registryService.getGovernanceRegistryResource(APIConstants.API_TIER_LOCATION));

                    registryService.addGovernanceRegistryResource(
                                                    APIConstants.RES_TIER_LOCATION, apiTiers, "application/xml");
                }
            } catch (UserStoreException e) {
                ResourceUtil.handleException("Error occurred while accessing the user store", e);
            } catch (RegistryException e) {
                ResourceUtil.handleException("Error occurred while accessing the registry", e);
            }
            finally {
                registryService.endTenantFlow();
            }
        }
        log.info("tier migration for API Manager completed.");
    }

    private void migrateLifeCycles() throws APIMigrationException {
        log.info("migrating life cycles for API Manager started.");

        String apiLifeCycleXMLPath = CarbonUtils.getCarbonHome() + File.separator + APIConstants.RESOURCE_FOLDER_LOCATION +
                File.separator + Constants.LIFE_CYCLES_FOLDER + File.separator +
                APIConstants.API_LIFE_CYCLE + ".xml";
        String executorlessApiLifeCycle = null;
        String apiLifeCycle = null;

        try (FileInputStream fileInputStream = new FileInputStream(new File(apiLifeCycleXMLPath))) {
            apiLifeCycle = IOUtils.toString(fileInputStream);

            executorlessApiLifeCycle = ResourceModifier.removeExecutorsFromAPILifeCycle(apiLifeCycle);
        } catch (FileNotFoundException e) {
            ResourceUtil.handleException("File " + apiLifeCycleXMLPath + " not found", e);
        } catch (IOException e) {
            ResourceUtil.handleException("Error reading file " + apiLifeCycleXMLPath, e);
        }

        final String apiLifeCycleRegistryPath = RegistryConstants.LIFECYCLE_CONFIGURATION_PATH +
                APIConstants.API_LIFE_CYCLE;

        for (Tenant tenant : getTenantsArray()) {
            try {
                registryService.startTenantFlow(tenant);

                addExecutorlessLifeCycle(tenant, apiLifeCycleRegistryPath, executorlessApiLifeCycle);

                updateApiLifeCycleStatus(tenant);

                updateWithCompleteLifeCycle(tenant, apiLifeCycleRegistryPath, apiLifeCycle);
            }
            finally {
                registryService.endTenantFlow();
            }
        }
    }

    private void addExecutorlessLifeCycle(Tenant tenant, String apiLifeCycleRegistryPath,
                                          String executorlessApiLifeCycle) throws APIMigrationException {
        try {
            if (!registryService.isConfigRegistryResourceExists(apiLifeCycleRegistryPath)) {
                registryService.addConfigRegistryResource(apiLifeCycleRegistryPath, executorlessApiLifeCycle,
                        "application/xml");
            }
            else {
                registryService.updateConfigRegistryResource(apiLifeCycleRegistryPath, executorlessApiLifeCycle);
            }
        } catch (UserStoreException e) {
            ResourceUtil.handleException("Error occurred while accessing the user store when adding executorless " +
                    APIConstants.API_LIFE_CYCLE + " for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        } catch (RegistryException e) {
            ResourceUtil.handleException("Error occurred while accessing the registry when adding executorless " +
                    APIConstants.API_LIFE_CYCLE + " for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        }
    }

    private void updateApiLifeCycleStatus(Tenant tenant) throws APIMigrationException {
        HashMap<String, String[]> statuses = new HashMap<>();
        statuses.put(APIStatus.PUBLISHED.toString(), new String[]{"Publish"});
        statuses.put(APIStatus.PROTOTYPED.toString(), new String[]{"Deploy as a Prototype"});
        statuses.put(APIStatus.BLOCKED.toString(), new String[]{"Publish", "Block"});
        statuses.put(APIStatus.DEPRECATED.toString(), new String[]{"Publish", "Deprecate"});
        statuses.put(APIStatus.RETIRED.toString(), new String[]{"Publish", "Deprecate", "Retire"});

        log.debug("Start life cycle migration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
        try {
            registryService.addDefaultLifecycles();
            GenericArtifact[] artifacts = registryService.getGenericAPIArtifacts();

            for (GenericArtifact artifact : artifacts) {
                String currentState = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                artifact.attachLifecycle(APIConstants.API_LIFE_CYCLE);
                String[] actions = statuses.get(currentState);

                if (actions != null) {
                    for (String action : actions) {
                        artifact.invokeAction(action, APIConstants.API_LIFE_CYCLE);
                    }
                }
            }
        }  catch (RegistryException e) {
            ResourceUtil.handleException("Error occurred while accessing the registry when updating " +
                    "API life cycles for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        } catch (XMLStreamException e) {
            ResourceUtil.handleException("XMLStreamException while adding default life cycles if " +
                    "not available for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        } catch (FileNotFoundException e) {
            ResourceUtil.handleException("FileNotFoundException while adding default life cycles if " +
                    "not available for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        } catch (UserStoreException e) {
            ResourceUtil.handleException("UserStoreException while adding default life cycles if " +
                    "not available for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        }
    }

    private void updateWithCompleteLifeCycle(Tenant tenant, String apiLifeCycleRegistryPath, String apiLifeCycle)
                                                                throws APIMigrationException {
        try {
            registryService.updateConfigRegistryResource(apiLifeCycleRegistryPath, apiLifeCycle);
        } catch (UserStoreException e) {
            ResourceUtil.handleException("Error occurred while accessing the user store when adding complete " +
                    APIConstants.API_LIFE_CYCLE + " for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        } catch (RegistryException e) {
            ResourceUtil.handleException("Error occurred while accessing the registry when adding complete " +
                    APIConstants.API_LIFE_CYCLE + " for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        }
    }
}
