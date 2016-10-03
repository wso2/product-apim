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


import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.client._110Specific.ResourceModifier;
import org.wso2.carbon.apimgt.migration.client._110Specific.dto.AppKeyMappingTableDTO;
import org.wso2.carbon.apimgt.migration.client._110Specific.dto.ConsumerAppsTableDTO;
import org.wso2.carbon.apimgt.migration.client._110Specific.dto.ConsumerKeyDTO;
import org.wso2.carbon.apimgt.migration.client._110Specific.dto.KeyDomainMappingTableDTO;
import org.wso2.carbon.apimgt.migration.client._110Specific.dto.SynapseDTO;
import org.wso2.carbon.apimgt.migration.client._200Specific.ResourceModifier200;
import org.wso2.carbon.apimgt.migration.util.RegistryService;
import org.wso2.carbon.apimgt.migration.util.ResourceUtil;
import org.wso2.carbon.apimgt.migration.util.StatDBUtil;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.FileUtil;

public class MigrateFrom110to200 extends MigrationClientBase implements MigrationClient {

    private static final Log log = LogFactory.getLog(MigrateFrom110to200.class);
    private RegistryService registryService;
    private boolean removeDecryptionFailedKeysFromDB;

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

        updateAPIManagerDatabase(amScriptPath);

        if (StatDBUtil.isTokenEncryptionEnabled()) {
            decryptEncryptedConsumerKeys();
        }
    }

    @Override
    public void registryResourceMigration() throws APIMigrationException {
        rxtMigration();
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

    /**
     * Implementation of new throttling migration using optional migration argument
     *
     * @param options list of command line options
     * @throws APIMigrationException throws if any exception occured
     */
    @Override
    public void optionalMigration(List<String> options) throws APIMigrationException {
        if (options.contains("migrateThrottling")) {
            for (Tenant tenant : getTenantsArray()) {
                String apiPath = ResourceUtil.getApiPath(tenant.getId(), tenant.getDomain());
                List<SynapseDTO> synapseDTOs = ResourceUtil.getVersionedAPIs(apiPath);
                ResourceModifier200.updateThrottleHandler(synapseDTOs);

                for (SynapseDTO synapseDTO : synapseDTOs) {
                    ResourceModifier200.transformXMLDocument(synapseDTO.getDocument(), synapseDTO.getFile());
                }
            }
            log.info("Throttling migration is finished.");
        }
    }

    private void synapseAPIMigration() {
        for (Tenant tenant : getTenantsArray()) {
            String apiPath = ResourceUtil.getApiPath(tenant.getId(), tenant.getDomain());
            List<SynapseDTO> synapseDTOs = ResourceUtil.getVersionedAPIs(apiPath);
            ResourceModifier200.updateSynapseConfigs(synapseDTOs);

            for (SynapseDTO synapseDTO : synapseDTOs) {
                ResourceModifier200.transformXMLDocument(synapseDTO.getDocument(), synapseDTO.getFile());
            }
        }
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
                "110-200-migration" + File.separator + "rxts" + File.separator + rxtName;

        
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
                    String val="{\"corsConfigurationEnabled\": false,"
                            + "\"accessControlAllowOrigins\": [\"*\"],"
                            + "\"accessControlAllowCredentials\": false,"
                            + "\"accessControlAllowHeaders\": [\"authorization\",   \"Access-Control-Allow-Origin\", \"Content-Type\", \"SOAPAction\"],"
                            + "\"accessControlAllowMethods\": [\"GET\", \"PUT\", \"POST\", \"DELETE\", \"PATCH\", \"OPTIONS\"]"
                            + "}";
                    artifact.setAttribute("overview_corsConfiguration", val);
                    artifact.setAttribute("overview_endpointSecured", "false");
                    artifact.setAttribute("overview_endpointAuthDigest", "false");

                    String env = artifact.getAttribute("overview_environments");
                    if (env == null) {
                        artifact.setAttribute("overview_environments", "Production and Sandbox");
                    }
                    String trans = artifact.getAttribute("overview_transports");
                    if (trans == null) {
                        artifact.setAttribute("overview_transports", "http,https");
                    }
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

}
