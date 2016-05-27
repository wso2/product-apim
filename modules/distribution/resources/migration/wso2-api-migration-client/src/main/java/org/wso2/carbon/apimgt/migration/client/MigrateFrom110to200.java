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
import org.wso2.carbon.apimgt.migration.client._110Specific.dto.*;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.RegistryService;
import org.wso2.carbon.apimgt.migration.util.ResourceUtil;
import org.wso2.carbon.apimgt.migration.util.StatDBUtil;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.core.RegistryConstants;
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
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;


public class MigrateFrom110to200 extends MigrationClientBase implements MigrationClient {

    private static final Log log = LogFactory.getLog(MigrateFrom110to200.class);
    private RegistryService registryService;
    private boolean removeDecryptionFailedKeysFromDB;

    private static class AccessTokenInfo {
        AccessTokenInfo(String usernameWithoutDomain, String authzUser) {
            this.usernameWithoutDomain = usernameWithoutDomain;
            this.authzUser = authzUser;
        }
        public String usernameWithoutDomain;
        public String authzUser;
    }

    public MigrateFrom110to200(String tenantArguments, String blackListTenantArguments, RegistryService registryService,
            TenantManager tenantManager, boolean removeDecryptionFailedKeysFromDB) throws UserStoreException {
        super(tenantArguments, blackListTenantArguments, tenantManager);
        this.registryService = registryService;
        this.removeDecryptionFailedKeysFromDB = removeDecryptionFailedKeysFromDB;
    }

    @Override
    public void databaseMigration() throws APIMigrationException, SQLException {
        String amScriptPath = CarbonUtils.getCarbonHome() + File.separator + "migration-scripts" + File.separator +
                "110-200-migration" + File.separator;

        updateAPIManangerDatabase(amScriptPath);

        //updateAuthzUserName();
        
        if (StatDBUtil.isTokenEncryptionEnabled()) {
            decryptEncryptedConsumerKeys();
        }
    }

    @Override
    public void registryResourceMigration() throws APIMigrationException {
        rxtMigration();

       /* workflowExtensionsMigration();

        updateTiers();

        migrateLifeCycles();*/
    }

    @Override
    public void fileSystemMigration() throws APIMigrationException {
        //synapseAPIMigration();
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
                connection.setAutoCommit(false);

                updateStatement = connection.prepareStatement("UPDATE IDN_OAUTH2_ACCESS_TOKEN SET AUTHZ_USER = ?" +
                                                                " WHERE AUTHZ_USER = ?");

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
        log.info("Decrypting encrypted consumer keys started");
        Connection connection = null;

        try {
            connection = APIMgtDBUtil.getConnection();
            connection.setAutoCommit(false);

            if (updateAMApplicationKeyMapping(connection)) {
                if (updateAMAppKeyDomainMapping(connection)) {
                    if (updateIdnTableConsumerKeys(connection)) {
                        connection.commit();
                    }
                }
            }

        } finally {
            APIMgtDBUtil.closeAllConnections(null, connection, null);
        }
        log.info("Decrypting encrypted consumer keys completed");
    }

    private boolean updateAMApplicationKeyMapping(Connection connection) throws SQLException {
        log.info("Updating consumer keys in AM_APPLICATION_KEY_MAPPING");
        PreparedStatement preparedStatementUpdate = null;
        PreparedStatement preparedStatementDelete = null;
        Statement statement = null;
        ResultSet resultSet = null;
        boolean continueUpdatingDB = true;
        long totalRecords = 0;
        long decryptionFailedRecords = 0;

        try {
            String query = "SELECT APPLICATION_ID, CONSUMER_KEY, KEY_TYPE FROM AM_APPLICATION_KEY_MAPPING";
            ArrayList<AppKeyMappingTableDTO> appKeyMappingTableDTOs = new ArrayList<>();
            ArrayList<AppKeyMappingTableDTO> appKeyMappingTableDTOsFailed = new ArrayList<>();

            statement = connection.createStatement();
            statement.setFetchSize(50);
            resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                ConsumerKeyDTO consumerKeyDTO = new ConsumerKeyDTO();
                consumerKeyDTO.setEncryptedConsumerKey(resultSet.getString("CONSUMER_KEY"));

                AppKeyMappingTableDTO appKeyMappingTableDTO = new AppKeyMappingTableDTO();
                appKeyMappingTableDTO.setApplicationId(resultSet.getString("APPLICATION_ID"));
                appKeyMappingTableDTO.setConsumerKey(consumerKeyDTO);
                appKeyMappingTableDTO.setKeyType(resultSet.getString("KEY_TYPE"));
                totalRecords ++;
                if (ResourceModifier.decryptConsumerKeyIfEncrypted(consumerKeyDTO)) {

                    appKeyMappingTableDTOs.add(appKeyMappingTableDTO);
                    log.debug("Successfully decrypted consumer key : " + consumerKeyDTO.getEncryptedConsumerKey()
                            + " as : " + consumerKeyDTO.getDecryptedConsumerKey()
                            + " in AM_APPLICATION_KEY_MAPPING table");
                } else {
                    log.error("Cannot decrypt consumer key : " + consumerKeyDTO.getEncryptedConsumerKey() +
                            " in AM_APPLICATION_KEY_MAPPING table");
                    decryptionFailedRecords++;
                    appKeyMappingTableDTOsFailed.add(appKeyMappingTableDTO);

                    //If its not allowed to remove decryption failed entries from DB, we will not continue updating 
                    // tables even with successfully decrypted entries to maintain DB integrity
                    if (!removeDecryptionFailedKeysFromDB) {
                        continueUpdatingDB = false;
                    }
                }
            }

            if (continueUpdatingDB) {
                preparedStatementUpdate = connection.prepareStatement("UPDATE AM_APPLICATION_KEY_MAPPING SET CONSUMER_KEY = ?" +
                        " WHERE APPLICATION_ID = ? AND KEY_TYPE = ?");

                for (AppKeyMappingTableDTO appKeyMappingTableDTO : appKeyMappingTableDTOs) {
                    preparedStatementUpdate.setString(1, appKeyMappingTableDTO.getConsumerKey().getDecryptedConsumerKey());
                    preparedStatementUpdate.setString(2, appKeyMappingTableDTO.getApplicationId());
                    preparedStatementUpdate.setString(3, appKeyMappingTableDTO.getKeyType());
                    preparedStatementUpdate.addBatch();
                }
                preparedStatementUpdate.executeBatch();

                //deleting rows where consumer key decryption was unsuccessful
                preparedStatementDelete = connection.prepareStatement("DELETE FROM AM_APPLICATION_KEY_MAPPING WHERE CONSUMER_KEY = ?");

                for (AppKeyMappingTableDTO appKeyMappingTableDTO : appKeyMappingTableDTOsFailed) {
                    preparedStatementDelete.setString(1, appKeyMappingTableDTO.getConsumerKey().getEncryptedConsumerKey());
                    preparedStatementDelete.addBatch();
                }
                preparedStatementDelete.executeBatch();
                log.info("AM_APPLICATION_KEY_MAPPING table updated with " + decryptionFailedRecords + "/"
                        + totalRecords + " of the CONSUMER_KEY entries deleted as they cannot be decrypted");
            } else {
                log.error("AM_APPLICATION_KEY_MAPPING table not updated as " + decryptionFailedRecords + "/"
                        + totalRecords + " of the CONSUMER_KEY entries cannot be decrypted");
            }
        } finally {
            if (preparedStatementUpdate != null) preparedStatementUpdate.close();
            if (preparedStatementDelete != null) preparedStatementDelete.close();
            if (statement != null) statement.close();
            if (resultSet != null) resultSet.close();
        }

        return continueUpdatingDB;
    }

    private boolean updateAMAppKeyDomainMapping(Connection connection) throws SQLException {
        log.info("Updating consumer keys in AM_APP_KEY_DOMAIN_MAPPING");
        Statement selectStatement = null;
        Statement deleteStatement = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        boolean continueUpdatingDB = true;
        long totalRecords = 0;
        long decryptionFailedRecords = 0;

        try {
            ArrayList<KeyDomainMappingTableDTO> keyDomainMappingTableDTOs = new ArrayList<>();
            String query = "SELECT * FROM AM_APP_KEY_DOMAIN_MAPPING";

            selectStatement = connection.createStatement();
            selectStatement.setFetchSize(50);
            resultSet = selectStatement.executeQuery(query);
            while (resultSet.next()) {
                ConsumerKeyDTO consumerKeyDTO = new ConsumerKeyDTO();
                consumerKeyDTO.setEncryptedConsumerKey(resultSet.getString("CONSUMER_KEY"));
                totalRecords++;
                if (ResourceModifier.decryptConsumerKeyIfEncrypted(consumerKeyDTO)) {
                    KeyDomainMappingTableDTO keyDomainMappingTableDTO = new KeyDomainMappingTableDTO();
                    keyDomainMappingTableDTO.setConsumerKey(consumerKeyDTO);
                    keyDomainMappingTableDTO.setAuthzDomain(resultSet.getString("AUTHZ_DOMAIN"));

                    keyDomainMappingTableDTOs.add(keyDomainMappingTableDTO);
                }
                else {
                    log.error("Cannot decrypt consumer key : " + consumerKeyDTO.getEncryptedConsumerKey() +
                                            " in AM_APP_KEY_DOMAIN_MAPPING table");
                    decryptionFailedRecords++;
                    //If its not allowed to remove decryption failed entries from DB, we will not continue updating 
                    // tables even with successfully decrypted entries to maintain DB integrity
                    if (!removeDecryptionFailedKeysFromDB) {
                        continueUpdatingDB = false;
                    }
                }
            }

            if (continueUpdatingDB) { // Modify table only if decryption is successful
                preparedStatement = connection.prepareStatement("INSERT INTO AM_APP_KEY_DOMAIN_MAPPING " +
                                                                        "(CONSUMER_KEY, AUTHZ_DOMAIN) VALUES (?, ?)");

                for (KeyDomainMappingTableDTO keyDomainMappingTableDTO : keyDomainMappingTableDTOs) {
                    preparedStatement.setString(1, keyDomainMappingTableDTO.getConsumerKey().getDecryptedConsumerKey());
                    preparedStatement.setString(2, keyDomainMappingTableDTO.getAuthzDomain());
                    preparedStatement.addBatch();
                }

                deleteStatement = connection.createStatement();
                deleteStatement.execute("DELETE FROM AM_APP_KEY_DOMAIN_MAPPING");

                preparedStatement.executeBatch();
                log.info("AM_APP_KEY_DOMAIN_MAPPING table updated with " + decryptionFailedRecords + "/"
                        + totalRecords + " of the CONSUMER_KEY entries deleted as they cannot be decrypted");
            } else {
                log.error("AM_APP_KEY_DOMAIN_MAPPING table not updated as " + decryptionFailedRecords + "/"
                        + totalRecords + " of the CONSUMER_KEY entries" + " cannot be decrypted");
            }
        }
        finally {
            if (selectStatement != null) selectStatement.close();
            if (deleteStatement != null) deleteStatement.close();
            if (preparedStatement != null) preparedStatement.close();
            if (resultSet != null) resultSet.close();
        }

        return continueUpdatingDB;
    }


    private boolean updateIdnTableConsumerKeys(Connection connection) throws SQLException {
        log.info("Updating consumer keys in IDN Tables");
        Statement consumerAppsLookup = null;
        PreparedStatement consumerAppsDelete = null;
        PreparedStatement consumerAppsInsert = null;
        PreparedStatement consumerAppsDeleteFailedRecords = null;
        PreparedStatement accessTokenUpdate = null;
        PreparedStatement accessTokenDelete = null;

        ResultSet consumerAppsResultSet = null;
        boolean continueUpdatingDB = true;

        try {
            String consumerAppsQuery = "SELECT * FROM IDN_OAUTH_CONSUMER_APPS";
            consumerAppsLookup = connection.createStatement();
            consumerAppsLookup.setFetchSize(50);
            consumerAppsResultSet = consumerAppsLookup.executeQuery(consumerAppsQuery);

            ArrayList<ConsumerAppsTableDTO> consumerAppsTableDTOs = new ArrayList<>();
            ArrayList<ConsumerAppsTableDTO> consumerAppsTableDTOsFailed = new ArrayList<>();


            while (consumerAppsResultSet.next()) {
                ConsumerKeyDTO consumerKeyDTO = new ConsumerKeyDTO();
                consumerKeyDTO.setEncryptedConsumerKey(consumerAppsResultSet.getString("CONSUMER_KEY"));

                ConsumerAppsTableDTO consumerAppsTableDTO = new ConsumerAppsTableDTO();
                consumerAppsTableDTO.setConsumerKey(consumerKeyDTO);
                consumerAppsTableDTO.setConsumerSecret(consumerAppsResultSet.getString("CONSUMER_SECRET"));
                consumerAppsTableDTO.setUsername(consumerAppsResultSet.getString("USERNAME"));
                consumerAppsTableDTO.setTenantID(consumerAppsResultSet.getInt("TENANT_ID"));
                consumerAppsTableDTO.setAppName(consumerAppsResultSet.getString("APP_NAME"));
                consumerAppsTableDTO.setOauthVersion(consumerAppsResultSet.getString("OAUTH_VERSION"));
                consumerAppsTableDTO.setCallbackURL(consumerAppsResultSet.getString("CALLBACK_URL"));
                consumerAppsTableDTO.setGrantTypes(consumerAppsResultSet.getString("GRANT_TYPES"));
                if (ResourceModifier.decryptConsumerKeyIfEncrypted(consumerKeyDTO)) {
                    consumerAppsTableDTOs.add(consumerAppsTableDTO);
                    log.debug("Successfully decrypted consumer key : " + consumerKeyDTO.getEncryptedConsumerKey()
                            + " in IDN_OAUTH_CONSUMER_APPS table");
                }
                else {
                    consumerAppsTableDTOsFailed.add(consumerAppsTableDTO);
                    log.error("Cannot decrypt consumer key : " + consumerKeyDTO.getEncryptedConsumerKey() +
                                                                            " in IDN_OAUTH_CONSUMER_APPS table");
                    //If its not allowed to remove decryption failed entries from DB, we will not continue updating 
                    // tables even with successfully decrypted entries to maintain DB integrity
                    if (!removeDecryptionFailedKeysFromDB) {
                        continueUpdatingDB = false;
                    }
                }
            }

            if (continueUpdatingDB) {
                // Add new entries for decrypted consumer keys into IDN_OAUTH_CONSUMER_APPS
                consumerAppsInsert = connection.prepareStatement("INSERT INTO IDN_OAUTH_CONSUMER_APPS (CONSUMER_KEY, " +
                                                    "CONSUMER_SECRET, USERNAME, TENANT_ID, APP_NAME, OAUTH_VERSION, " +
                                                    "CALLBACK_URL, GRANT_TYPES) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

                for (ConsumerAppsTableDTO consumerAppsTableDTO : consumerAppsTableDTOs) {
                    updateIdnConsumerApps(consumerAppsInsert, consumerAppsTableDTO);
                }
                consumerAppsInsert.executeBatch();
                log.info("Inserted entries in IDN_OAUTH_CONSUMER_APPS");

                // Update IDN_OAUTH2_ACCESS_TOKEN foreign key reference to CONSUMER_KEY
                accessTokenUpdate = connection.prepareStatement("UPDATE IDN_OAUTH2_ACCESS_TOKEN SET CONSUMER_KEY = ? " +
                                                                "WHERE CONSUMER_KEY = ?");

                for (ConsumerAppsTableDTO consumerAppsTableDTO : consumerAppsTableDTOs) {
                    ConsumerKeyDTO consumerKeyDTO = consumerAppsTableDTO.getConsumerKey();
                    updateIdnAccessToken(accessTokenUpdate, consumerKeyDTO);
                }
                accessTokenUpdate.executeBatch();
                log.info("Updated entries in IDN_OAUTH2_ACCESS_TOKEN");

                // Remove redundant records in IDN_OAUTH_CONSUMER_APPS
                consumerAppsDelete = connection.prepareStatement("DELETE FROM IDN_OAUTH_CONSUMER_APPS WHERE " +
                                                                                                    "CONSUMER_KEY = ?");

                for (ConsumerAppsTableDTO consumerAppsTableDTO : consumerAppsTableDTOs) {
                    ConsumerKeyDTO consumerKeyDTO = consumerAppsTableDTO.getConsumerKey();
                    deleteIdnConsumerApps(consumerAppsDelete, consumerKeyDTO);
                }
                consumerAppsDelete.executeBatch();
                log.info("Removed redundant entries in IDN_OAUTH_CONSUMER_APPS");

                //deleting rows where consumer key decryption was unsuccessful from IDN_OAUTH_CONSUMER_APPS table
                consumerAppsDeleteFailedRecords = connection.prepareStatement("DELETE FROM IDN_OAUTH_CONSUMER_APPS WHERE " +
                        "CONSUMER_KEY = ?");
                for (ConsumerAppsTableDTO consumerAppsTableDTO : consumerAppsTableDTOsFailed) {
                    ConsumerKeyDTO consumerKeyDTO = consumerAppsTableDTO.getConsumerKey();
                    deleteIdnConsumerApps(consumerAppsDeleteFailedRecords, consumerKeyDTO);
                }
                consumerAppsDeleteFailedRecords.executeBatch();
                log.info("Removed decryption failed entries in IDN_OAUTH_CONSUMER_APPS");

                //deleting rows where consumer key decryption was unsuccessful from IDN_OAUTH2_ACCESS_TOKEN table
                accessTokenDelete = connection.prepareStatement("DELETE FROM IDN_OAUTH2_ACCESS_TOKEN " +
                        "WHERE CONSUMER_KEY = ?");
                for (ConsumerAppsTableDTO consumerAppsTableDTO : consumerAppsTableDTOsFailed) {
                    ConsumerKeyDTO consumerKeyDTO = consumerAppsTableDTO.getConsumerKey();
                    deleteIdnAccessToken(consumerAppsDeleteFailedRecords, consumerKeyDTO);
                }
                accessTokenDelete.executeBatch();
                log.info("Removed decryption failed entries in IDN_OAUTH2_ACCESS_TOKEN");
            }
        } finally {
            if (consumerAppsLookup != null) consumerAppsLookup.close();
            if (consumerAppsDelete != null) consumerAppsDelete.close();
            if (consumerAppsDeleteFailedRecords != null) consumerAppsDeleteFailedRecords.close();
            if (consumerAppsInsert != null) consumerAppsInsert.close();
            if (accessTokenUpdate != null) accessTokenUpdate.close();
            if (accessTokenDelete != null) accessTokenDelete.close();
            if (consumerAppsResultSet != null) consumerAppsResultSet.close();
        }

        return continueUpdatingDB;
    }


    private void updateIdnConsumerApps(PreparedStatement consumerAppsInsert, ConsumerAppsTableDTO consumerAppsTableDTO)
                                                                                                throws SQLException {
        consumerAppsInsert.setString(1, consumerAppsTableDTO.getConsumerKey().getDecryptedConsumerKey());
        consumerAppsInsert.setString(2, consumerAppsTableDTO.getConsumerSecret());
        consumerAppsInsert.setString(3, consumerAppsTableDTO.getUsername());
        consumerAppsInsert.setInt(4, consumerAppsTableDTO.getTenantID());
        consumerAppsInsert.setString(5, consumerAppsTableDTO.getAppName());
        consumerAppsInsert.setString(6, consumerAppsTableDTO.getOauthVersion());
        consumerAppsInsert.setString(7, consumerAppsTableDTO.getCallbackURL());
        consumerAppsInsert.setString(8, consumerAppsTableDTO.getGrantTypes());
        consumerAppsInsert.addBatch();
    }


    private void updateIdnAccessToken(PreparedStatement accessTokenUpdate, ConsumerKeyDTO consumerKeyDTO)
    throws SQLException {
        accessTokenUpdate.setString(1, consumerKeyDTO.getDecryptedConsumerKey());
        accessTokenUpdate.setString(2, consumerKeyDTO.getEncryptedConsumerKey());
        accessTokenUpdate.addBatch();
    }

    private void deleteIdnAccessToken(PreparedStatement accessTokenDelete, ConsumerKeyDTO consumerKeyDTO)
            throws SQLException {
        accessTokenDelete.setString(1, consumerKeyDTO.getEncryptedConsumerKey());
        accessTokenDelete.addBatch();
    }

    private void deleteIdnConsumerApps(PreparedStatement consumerAppsDelete, ConsumerKeyDTO consumerKeyDTO)
    throws SQLException{
        consumerAppsDelete.setString(1, consumerKeyDTO.getEncryptedConsumerKey());
        consumerAppsDelete.addBatch();
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
            try {                
                registryService.startTenantFlow(tenant);                
                
                log.info("Updating api.rxt for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
                //Update api.rxt file
                String rxt = FileUtil.readFileToString(rxtDir);
                registryService.updateRXTResource(rxtName, rxt);                
                log.info("End Updating api.rxt for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
                
                log.info("Start rxt data migration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
                GenericArtifact[] artifacts = registryService.getGenericAPIArtifacts();
                for (GenericArtifact artifact : artifacts) {
                    artifact.setAttribute("overview_endpointAuthDigest", "false");
                }
                registryService.updateGenericAPIArtifacts(artifacts);
                log.info("End rxt data migration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            
            } catch (GovernanceException e) {
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
        log.info("Workflow Extensions configuration file migration for API Manager started.");
        for (Tenant tenant : getTenantsArray()) {
            log.info("Start workflow extensions configuration migration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');

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
                log.error("Error occurred while accessing the registry for tenant " + tenant.getId() + 
                          '(' + tenant.getDomain() + ')', e);
            } catch (UserStoreException e) {
                log.error("Error occurred while accessing the user store " + tenant.getId() + 
                          '(' + tenant.getDomain() + ')', e);
            }
            finally {
                registryService.endTenantFlow();
            }

            log.info("End workflow extensions configuration migration for tenant " + tenant.getId() 
                      + '(' + tenant.getDomain() + ')');
        }

        log.info("Workflow Extensions configuration file migration done for all the tenants");
    }

    private void updateTiers() throws APIMigrationException {
        log.info("Tiers configuration migration for API Manager started.");

        for (Tenant tenant : getTenantsArray()) {
            log.info("Start tiers configuration migration for tenant " + tenant.getId() 
                      + '(' + tenant.getDomain() + ')');

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
                log.error("Error occurred while accessing the user store " + + tenant.getId() + 
                          '(' + tenant.getDomain() + ')', e);
            } catch (RegistryException e) {
                log.error("Error occurred while accessing the registry " + + tenant.getId() + 
                                             '(' + tenant.getDomain() + ')', e);
            }
            finally {
                registryService.endTenantFlow();
            }
            log.info("End tiers configuration migration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
        }
        log.info("Tiers configuration migration for API Manager completed.");
    }

    private void migrateLifeCycles() throws APIMigrationException {
        log.info("Life Cycles executor migration for API Manager started.");

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
            log.info("Start life cycle migration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            try {
                registryService.startTenantFlow(tenant);

                addExecutorlessLifeCycle(tenant, apiLifeCycleRegistryPath, executorlessApiLifeCycle);

                updateApiLifeCycleStatus(tenant);

                updateWithCompleteLifeCycle(tenant, apiLifeCycleRegistryPath, apiLifeCycle);
            }
            finally {
                registryService.endTenantFlow();
            }
            log.info("End life cycle migration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
        }
        log.info("Life Cycles executor migration for API Manager completed.");
    }

    private void addExecutorlessLifeCycle(Tenant tenant, String apiLifeCycleRegistryPath,
                                          String executorlessApiLifeCycle) throws APIMigrationException {
        try {
            log.debug("Adding executorless LC for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            if (!registryService.isConfigRegistryResourceExists(apiLifeCycleRegistryPath)) {
                registryService.addConfigRegistryResource(apiLifeCycleRegistryPath, executorlessApiLifeCycle,
                        "application/xml");
            }
            else {
                registryService.updateConfigRegistryResource(apiLifeCycleRegistryPath, executorlessApiLifeCycle);
            }
            log.debug("Completed adding executorless LC for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
        } catch (UserStoreException e) {
            log.error("Error occurred while accessing the user store when adding executorless " +
                    APIConstants.API_LIFE_CYCLE + " for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        } catch (RegistryException e) {
            log.error("Error occurred while accessing the registry when adding executorless " +
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

        log.debug("Updating LC status for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
        try {
            registryService.addDefaultLifecycles();
            GenericArtifact[] artifacts = registryService.getGenericAPIArtifacts();

            for (GenericArtifact artifact : artifacts) {
                String currentState = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);
                //Check whether API is already migrated
                if (currentState != null && !currentState.equalsIgnoreCase(artifact.getLifecycleState())) {                    
                    artifact.attachLifecycle(APIConstants.API_LIFE_CYCLE);
                    String[] actions = statuses.get(currentState);

                    if (actions != null) {
                        for (String action : actions) {
                            artifact.invokeAction(action, APIConstants.API_LIFE_CYCLE);
                            if (log.isDebugEnabled()) {
                                log.debug("Target LC Status : " + currentState + ". Performing LC Action : " + action);
                            }
                        }
                    }
                } else {
                    log.info("API is already in target LC state: " + currentState + ". Skipping migration for API " +
                            artifact.getAttribute(APIConstants.API_OVERVIEW_NAME));
                }                
            }
            log.debug("Completed updating LC status for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
        }  catch (RegistryException e) {
            log.error("Error occurred while accessing the registry when updating " +
                    "API life cycles for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        } catch (XMLStreamException e) {
            log.error("XMLStreamException while adding default life cycles if " +
                    "not available for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        } catch (FileNotFoundException e) {
            log.error("FileNotFoundException while adding default life cycles if " +
                    "not available for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        } catch (UserStoreException e) {
            log.error("UserStoreException while adding default life cycles if " +
                    "not available for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        }
    }

    private void updateWithCompleteLifeCycle(Tenant tenant, String apiLifeCycleRegistryPath, String apiLifeCycle)
                                                                throws APIMigrationException {
        log.debug("Update with complete LC for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
        try {
            registryService.updateConfigRegistryResource(apiLifeCycleRegistryPath, apiLifeCycle);
            log.debug("Completed update with complete LC for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
        } catch (UserStoreException e) {
            log.error("Error occurred while accessing the user store when adding complete " +
                    APIConstants.API_LIFE_CYCLE + " for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        } catch (RegistryException e) {
            log.error("Error occurred while accessing the registry when adding complete " +
                    APIConstants.API_LIFE_CYCLE + " for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        }
    }
}
