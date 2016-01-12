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
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.APIStatus;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.client._110Specific.ResourceModifier;
import org.wso2.carbon.apimgt.migration.client._110Specific.dto.SynapseDTO;
import org.wso2.carbon.apimgt.migration.client._110Specific.dto.AppKeyMappingDTO;
import org.wso2.carbon.apimgt.migration.client.internal.ServiceHolder;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.RegistryService;
import org.wso2.carbon.apimgt.migration.util.ResourceUtil;
import org.wso2.carbon.apimgt.migration.util.StatDBUtil;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceConstants;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.jdbc.realm.RegistryAuthorizationManager;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.FileUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;


public class MigrateFrom19to110 extends MigrationClientBase implements MigrationClient {

    private static final Log log = LogFactory.getLog(MigrateFrom19to110.class);
    private RegistryService registryService;

    private static class AccessTokenInfo {
        AccessTokenInfo(String usernameWithoutDomain, String authzUser) {
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
        
        if (StatDBUtil.isTokenEncryptionEnabled()) {
            decryptEncryptedConsumerKeys();
        }
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
        synapseAPIMigration();
    }

    @Override
    public void cleanOldResources() throws APIMigrationException {

    }

    @Override
    public void statsMigration() throws APIMigrationException {

    }

    private void synapseAPIMigration() {
        for (Tenant tenant : getTenantsArray()) {
            String apiPath = ResourceUtil.getApiPath(tenant.getId(), tenant.getDomain());
            List<SynapseDTO> synapseDTOs = ResourceUtil.getVersionedAPIs(apiPath);
            ResourceModifier.updateSynapseConfigs(synapseDTOs);

            for (SynapseDTO synapseDTO : synapseDTOs) {
                ResourceUtil.transformXMLDocument(synapseDTO.getDocument(), synapseDTO.getFile());
            }
        }
    }


    private void updateAuthzUserName() throws SQLException {
        log.info("Updating Authz UserName for API Manager started");
        Connection connection = null;
        PreparedStatement selectStatement = null;
        ResultSet resultSet = null;

        ArrayList<AccessTokenInfo> updateValues = new ArrayList<>();
        try {
            String selectQuery = "SELECT DISTINCT AUTHZ_USER FROM IDN_OAUTH2_ACCESS_TOKEN WHERE AUTHZ_USER LIKE '%@%'";

            connection = APIMgtDBUtil.getConnection();
            selectStatement = connection.prepareStatement(selectQuery);
            resultSet = selectStatement.executeQuery();

            while (resultSet.next()) {
                String authzUser = resultSet.getString("AUTHZ_USER");

                String usernameWithoutDomain = MultitenantUtils.getTenantAwareUsername(authzUser);

                AccessTokenInfo accessTokenInfo = new AccessTokenInfo(usernameWithoutDomain, authzUser);
                updateValues.add(accessTokenInfo);
            }

        } finally {
            APIMgtDBUtil.closeAllConnections(selectStatement, connection, resultSet);
        }

        if (!updateValues.isEmpty()) { // If user names that need to be updated exist
            PreparedStatement updateStatement = null;

            try {
                connection = APIMgtDBUtil.getConnection();

                updateStatement = connection.prepareStatement("UPDATE IDN_OAUTH2_ACCESS_TOKEN SET AUTHZ_USER = ?" +
                                                                "WHERE AUTHZ_USER = ?");

                for (AccessTokenInfo accessTokenInfo : updateValues) {
                    updateStatement.setString(1, accessTokenInfo.usernameWithoutDomain);
                    updateStatement.setString(2, accessTokenInfo.authzUser);
                    updateStatement.addBatch();
                }
                updateStatement.executeBatch();

                connection.commit();
            } finally {
                APIMgtDBUtil.closeAllConnections(updateStatement, connection, null);
            }
        }
        log.info("Updating Authz UserName for API Manager completed");
    }

    private void decryptEncryptedConsumerKeys() throws SQLException {
        log.info("Decrypting encrypted consumer keys for API Manager started");
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        ArrayList<AppKeyMappingDTO> appKeyMappingDTOs = new ArrayList<>();

        try {
            String query = "SELECT APPLICATION_ID, CONSUMER_KEY, KEY_TYPE FROM AM_APPLICATION_KEY_MAPPING";

            connection = APIMgtDBUtil.getConnection();
            preparedStatement = connection.prepareStatement(query);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                AppKeyMappingDTO appKeyMappingDTO = new AppKeyMappingDTO();
                appKeyMappingDTO.setApplicationId(resultSet.getString("APPLICATION_ID"));
                appKeyMappingDTO.setConsumerKey(resultSet.getString("CONSUMER_KEY"));
                appKeyMappingDTO.setKeyType(resultSet.getString("KEY_TYPE"));

                appKeyMappingDTOs.add(appKeyMappingDTO);
            }

            ResourceModifier.decryptConsumerKeyIfEncrypted(appKeyMappingDTOs);
        } finally {
            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, resultSet);
        }

        if (!appKeyMappingDTOs.isEmpty()) {
            PreparedStatement updateStatement = null;

            try {
                connection = APIMgtDBUtil.getConnection();
                updateStatement = connection.prepareStatement("UPDATE AM_APPLICATION_KEY_MAPPING SET CONSUMER_KEY = ?" +
                                                                    "WHERE APPLICATION_ID = ? AND KEY_TYPE = ?");

                for (AppKeyMappingDTO appKeyMappingDTO : appKeyMappingDTOs) {
                    updateStatement.setString(1, appKeyMappingDTO.getConsumerKey());
                    updateStatement.setString(2, appKeyMappingDTO.getApplicationId());
                    updateStatement.setString(3, appKeyMappingDTO.getKeyType());
                    updateStatement.addBatch();
                }

                updateStatement.executeBatch();

                connection.commit();
            } finally {
                APIMgtDBUtil.closeAllConnections(updateStatement, connection, null);
            }
        }
    }
    
    
    /**
     * This method is used to migrate rxt and rxt data
     * This adds three new attributes to the api rxt
     *
     * @throws APIMigrationException
     */
    private void rxtMigration() throws APIMigrationException {
        log.info("Rxt migration for API Manager started.");
        
        String rxtName = "api.rxt";
        String rxtDir = CarbonUtils.getCarbonHome() + File.separator + "migration-scripts" + File.separator +
                "19-110-migration" + File.separator + "rxts" + File.separator + rxtName;

        
        for (Tenant tenant : getTenantsArray()) {
                        
            log.debug("Updating api.rxt for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');

            try {                
                registryService.startTenantFlow(tenant);                
                
                //Update api.rxt file
                String rxt = FileUtil.readFileToString(rxtDir);
                registryService.updateRXTResource(rxtName, rxt);                
                log.debug("End Updating api.rxt for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
                
                log.debug("Start rxt data migration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
                GenericArtifact[] artifacts = registryService.getGenericAPIArtifacts();
                for (GenericArtifact artifact : artifacts) {
                    artifact.setAttribute("overview_endpointAuthDigest", "false");
                }
                registryService.updateGenericAPIArtifacts(artifacts);
                log.debug("End rxt data migration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            }
            catch (GovernanceException e) {
                log.error("Error when accessing API artifact in registry for tenant "+ tenant.getId() + '(' 
                          + tenant.getDomain() + ')', e);
            } catch (IOException e) {
                log.error("Error when reading api.rxt from " + rxtDir + "for tenant " + tenant.getId() + '(' 
                          + tenant.getDomain() + ')', e);
            } catch (org.wso2.carbon.registry.core.exceptions.RegistryException e) {
                log.error("Error while updating api.rxt in the registry for tenant " + tenant.getId() + '(' 
                          + tenant.getDomain() + ')', e);
            } catch (UserStoreException e) {
                log.error("Error while updating api.rxt in the registry for tenant " + tenant.getId() + '(' 
                          + tenant.getDomain() + ')', e);
            }
            finally {
                registryService.endTenantFlow();
            }
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
