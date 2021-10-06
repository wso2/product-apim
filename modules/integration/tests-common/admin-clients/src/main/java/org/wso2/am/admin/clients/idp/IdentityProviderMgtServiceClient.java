/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.admin.clients.idp;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.am.admin.clients.client.utils.AuthenticateStub;
import org.wso2.carbon.identity.application.common.model.idp.xsd.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.idp.xsd.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.idp.xsd.ProvisioningConnectorConfig;
import org.wso2.carbon.idp.mgt.stub.IdentityProviderMgtServiceStub;
import org.wso2.carbon.user.mgt.stub.UserAdminStub;
import org.wso2.carbon.user.mgt.stub.types.carbon.UserStoreInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdentityProviderMgtServiceClient {

    private static Log log = LogFactory.getLog(IdentityProviderMgtServiceClient.class);

    private IdentityProviderMgtServiceStub idPMgtStub;

    private UserAdminStub userAdminStub;

    /**
     *
     * @param userName User name
     * @param password Password
     * @param backEndUrl Backend Carbon server URL
     * @throws AxisFault
     */
    public IdentityProviderMgtServiceClient(String userName, String password, String backEndUrl)
            throws AxisFault {
        String idPMgtServiceURL = backEndUrl + "IdentityProviderMgtService";
        String userAdminServiceURL = backEndUrl + "UserAdmin";

        idPMgtStub = new IdentityProviderMgtServiceStub(idPMgtServiceURL);
        userAdminStub = new UserAdminStub(userAdminServiceURL);

        AuthenticateStub.authenticateStub(userName, password, idPMgtStub);
    }

    /**
     *
     * @param sessionCookie HttpSession cookie
     * @param backEndUrl Backend Carbon server URL
     * @throws AxisFault
     */
    public IdentityProviderMgtServiceClient(String backEndUrl, String sessionCookie)
            throws AxisFault {
        String idPMgtServiceURL = backEndUrl + "IdentityProviderMgtService";
        String userAdminServiceURL = backEndUrl + "UserAdmin";

        idPMgtStub = new IdentityProviderMgtServiceStub(idPMgtServiceURL);
        userAdminStub = new UserAdminStub(userAdminServiceURL);

        AuthenticateStub.authenticateStub(sessionCookie, idPMgtStub);

    }

    /**
     * @param cookie HttpSession cookie
     * @param backendServerURL Backend Carbon server URL
     * @param configCtx Axis2 Configuration Context
     */
    public IdentityProviderMgtServiceClient(String cookie, String backendServerURL,
                                            ConfigurationContext configCtx) {

        String idPMgtServiceURL = backendServerURL + "IdentityProviderMgtService";
        String userAdminServiceURL = backendServerURL + "UserAdmin";
        try {
            idPMgtStub = new IdentityProviderMgtServiceStub(configCtx, idPMgtServiceURL);
        } catch (AxisFault axisFault) {
            log.error("Error while instantiating IdentityProviderMgtServiceStub", axisFault);
        }
        try {
            userAdminStub = new UserAdminStub(configCtx, userAdminServiceURL);
        } catch (AxisFault axisFault) {
            log.error("Error while instantiating UserAdminServiceStub", axisFault);
        }
        ServiceClient idPMgtClient = idPMgtStub._getServiceClient();
        ServiceClient userAdminClient = userAdminStub._getServiceClient();
        Options idPMgtOptions = idPMgtClient.getOptions();
        idPMgtOptions.setManageSession(true);
        idPMgtOptions.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
                cookie);
        Options userAdminOptions = userAdminClient.getOptions();
        userAdminOptions.setManageSession(true);
        userAdminOptions.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
                cookie);
    }

    /**
     * Retrieves Resident Identity provider for a given tenant
     * 
     * @return <code>FederatedIdentityProvider</code>
     * @throws Exception Error when getting Resident Identity Providers
     */
    public IdentityProvider getResidentIdP() throws Exception {
        try {
            return idPMgtStub.getResidentIdP();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception("Error occurred while retrieving list of Identity Providers");
        }
    }

    /**
     * Updated Resident Identity provider for a given tenant
     * 
     * @return <code>FederatedIdentityProvider</code>
     * @throws Exception Error when getting Resident Identity Providers
     */
    public void updateResidentIdP(IdentityProvider identityProvider) throws Exception {
        try {
            idPMgtStub.updateResidentIdP(identityProvider);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception("Error occurred while updating resident identity provider");
        }
    }

    /**
     * Retrieves registered Identity providers for a given tenant
     * 
     * @return List of <code>FederatedIdentityProvider</code>. IdP names, primary IdP and home realm
     *         identifiers of each IdP
     * @throws Exception Error when getting list of Identity Providers
     */
    public List<IdentityProvider> getIdPs() throws Exception {
        try {
            IdentityProvider[] identityProviders = idPMgtStub.getAllIdPs();
            if (identityProviders != null && identityProviders.length > 0) {
                return Arrays.asList(identityProviders);
            } else {
                return new ArrayList<IdentityProvider>();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception("Error occurred while retrieving list of Identity Providers");
        }
    }

    /**
     * Retrieves registered Identity providers for a given filter, pageNumber and tenant.
     *
     * @param filter     filter value.
     * @param pageNumber page number.
     * @return List of <code>FederatedIdentityProvider</code>. IdP names, primary IdP and home realm
     * identifiers of each IdP.
     * @throws Exception Error when getting list of Identity Providers.
     */
    public List<IdentityProvider> getPaginatedIdPsInfo(String filter, int pageNumber) throws Exception {

        IdentityProvider[] identityProviders = idPMgtStub.getPaginatedIdpInfo(filter, pageNumber);
        if (identityProviders != null && identityProviders.length > 0) {
            return Arrays.asList(identityProviders);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves registered Identity providers for a given tenant and pageNumber.
     *
     * @param pageNumber page number.
     * @return List of <code>FederatedIdentityProvider</code>. IdP names, primary IdP and home realm
     * identifiers of each IdP.
     * @throws Exception Error when getting list of Identity Providers.
     */
    public List<IdentityProvider> getAllPaginatedIdPsInfo(int pageNumber) throws Exception {

        IdentityProvider[] identityProviders = idPMgtStub.getAllPaginatedIdpInfo(pageNumber);
        if (identityProviders != null && identityProviders.length > 0) {
            return Arrays.asList(identityProviders);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves filtered registered Identity providers count for a given tenant
     *
     * @param filter filter value.
     * @return filtered identity provider count.
     * @throws Exception Error when getting count of Identity Providers.
     */
    public int getFilteredIdpCount(String filter) throws Exception {

        return idPMgtStub.getFilteredIdpCount(filter);
    }

    /**
     * Retrieves all registered Identity providers count for a given tenant
     *
     * @return identity provider count.
     * @throws Exception Error when getting count of Identity Providers.
     */
    public int getAllIdpCount() throws Exception {

        return idPMgtStub.getAllIdpCount();
    }

    /**
     * Retrieves Enabled registered Identity providers for a given tenant
     * 
     * @return List of <code>FederatedIdentityProvider</code>. IdP names, primary IdP and home realm
     *         identifiers of each IdP
     * @throws Exception Error when getting list of Identity Providers
     */
    public List<IdentityProvider> getEnabledIdPs() throws Exception {
        try {
            IdentityProvider[] identityProviders = idPMgtStub.getEnabledAllIdPs();
            if (identityProviders != null && identityProviders.length > 0) {
                return Arrays.asList(identityProviders);
            } else {
                return new ArrayList<IdentityProvider>();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception(
                    "Error occurred while retrieving list of Enabled Identity Providers");
        }
    }

    /**
     * Retrieves Identity provider information about a given tenant by Identity Provider name
     * 
     * @param idPName Unique name of the Identity provider of whose information is requested
     * @return <code>FederatedIdentityProvider</code> Identity Provider information
     * @throws Exception Error when getting Identity Provider information by IdP name
     */
    public IdentityProvider getIdPByName(String idPName) throws Exception {
        try {
            return idPMgtStub.getIdPByName(idPName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception("Error occurred while retrieving information about " + idPName);
        }
    }

    /**
     * Adds an Identity Provider to the given tenant
     * 
     * @param identityProvider <code><FederatedIdentityProvider/code></code> federated Identity
     *        Provider information
     * @throws Exception Error when adding Identity Provider information
     */
    public void addIdP(IdentityProvider identityProvider) throws Exception {

        try {
            idPMgtStub.addIdP(identityProvider);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception("Error occurred while adding Identity Provider "
                    + identityProvider.getIdentityProviderName());
        }
    }

    /**
     * Deletes an Identity Provider from a given tenant
     * 
     * @param idPName Name of the IdP to be deleted
     * @throws Exception Error when deleting Identity Provider information
     */
    public void deleteIdP(String idPName) throws Exception {
        try {
            idPMgtStub.deleteIdP(idPName);
        } catch (Exception e) {
            throw e;
        }
    }

    public IdentityProviderMgtServiceStub getIdPMgtStub() {
        return idPMgtStub;
    }

    /**
     * Updates a given Identity Provider information
     * 
     * @param oldIdPName existing IdP name
     * @param identityProvider <code>FederatedIdentityProvider</code> new IdP information
     * @throws Exception Error when updating Identity Provider information
     */
    public void updateIdP(String oldIdPName, IdentityProvider identityProvider) throws Exception {
        try {
            idPMgtStub.updateIdP(oldIdPName, identityProvider);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception("Error occurred while deleting Identity Provider " + oldIdPName);
        }
    }

    /**
     * Get all available custom federated authenticators
     * @return
     * @throws Exception
     */
    public Map<String, FederatedAuthenticatorConfig> getAllFederatedAuthenticators()
            throws Exception {

        Map<String, FederatedAuthenticatorConfig> configMap = new HashMap<String, FederatedAuthenticatorConfig>();

        try {
            FederatedAuthenticatorConfig[] fedAuthConfigs = idPMgtStub
                    .getAllFederatedAuthenticators();

            if (fedAuthConfigs != null && fedAuthConfigs.length > 0) {
                for (FederatedAuthenticatorConfig config : fedAuthConfigs) {
                    if (!(config.getDisplayName().equals("facebook")
                            || config.getDisplayName().equals("openid")
                            || config.getDisplayName().equals("openidconnect")
                            || config.getDisplayName().equals("samlsso") || config.getDisplayName()
                            .equals("passovests")))
                        configMap.put(config.getName(), config);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception("Error occurred while retrieving all local claim URIs");
        }

        return configMap;

    }

    /**
     * Get all available federated authenticators
     * @return all available federated authenticators
     * @throws Exception
     */
    public Map<String, FederatedAuthenticatorConfig> getAllAvailableFederatedAuthenticators()
            throws Exception {

        Map<String, FederatedAuthenticatorConfig> configMap = new HashMap<String, FederatedAuthenticatorConfig>();

        try {
            FederatedAuthenticatorConfig[] fedAuthConfigs = idPMgtStub.getAllFederatedAuthenticators();

            if (fedAuthConfigs != null && fedAuthConfigs.length > 0) {
                for (FederatedAuthenticatorConfig config : fedAuthConfigs) {
                    configMap.put(config.getName(), config);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception("Error occurred while retrieving all local claim URIs");
        }

        return configMap;
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public Map<String, ProvisioningConnectorConfig> getCustomProvisioningConnectors() throws Exception {
        Map<String, ProvisioningConnectorConfig> provisioningConnectors = new HashMap<String, ProvisioningConnectorConfig>();
        try {
            ProvisioningConnectorConfig[] provisioningConnectorConfigs = idPMgtStub
                    .getAllProvisioningConnectors();
            if (provisioningConnectorConfigs != null && provisioningConnectorConfigs.length > 0
                    && provisioningConnectorConfigs[0] != null) {
                for (ProvisioningConnectorConfig config : provisioningConnectorConfigs) {
                    if (!(config.getName().equals("scim") || config.getName().equals("salesforce")
                            || config.getName().equals("googleapps")))
                        provisioningConnectors.put(config.getName(), config);

                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception("Error occurred while retrieving all Provisioning Connectors");
        }
        return provisioningConnectors;
    }

    /**
     * Function to retrieve all provisioning connectors
     * @return all provisioning connectors
     * @throws Exception
     */
    public Map<String, ProvisioningConnectorConfig> getAllProvisioningConnectors() throws Exception {
        Map<String, ProvisioningConnectorConfig> provisioningConnectors = new HashMap<String, ProvisioningConnectorConfig>();
        try {
            ProvisioningConnectorConfig[] provisioningConnectorConfigs = idPMgtStub
                    .getAllProvisioningConnectors();
            if (provisioningConnectorConfigs != null && provisioningConnectorConfigs.length > 0
                && provisioningConnectorConfigs[0] != null) {
                for (ProvisioningConnectorConfig config : provisioningConnectorConfigs) {
                        provisioningConnectors.put(config.getName(), config);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception("Error occurred while retrieving all Provisioning Connectors");
        }
        return provisioningConnectors;
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public String[] getAllLocalClaimUris() throws Exception {

        try {
            return idPMgtStub.getAllLocalClaimUris();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception("Error occurred while retrieving all local claim URIs");
        }
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public String[] getUserStoreDomains() throws Exception {

        try {
            List<String> readWriteDomainNames = new ArrayList<String>();
            UserStoreInfo[] storesInfo = userAdminStub.getUserRealmInfo().getUserStoresInfo();
            for (UserStoreInfo storeInfo : storesInfo) {
                if (!storeInfo.getReadOnly()) {
                    readWriteDomainNames.add(storeInfo.getDomainName());
                }
            }
            return readWriteDomainNames.toArray(new String[readWriteDomainNames.size()]);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception(
                    "Error occurred while retrieving Read-Write User Store Domain IDs for logged-in user's tenant realm");
        }
    }

}
