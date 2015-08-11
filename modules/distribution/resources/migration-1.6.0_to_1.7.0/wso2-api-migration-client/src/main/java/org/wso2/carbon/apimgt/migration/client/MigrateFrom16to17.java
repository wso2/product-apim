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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.client.internal.ServiceHolder;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.ResourceUtil;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.sql.*;
import java.util.*;


/**
 * This class contains all the methods which is used to migrate APIs from APIManager 1.6.0 to APIManager 1.7.0.
 * The migration performs in database, registry and file system
 */

@SuppressWarnings("unchecked")
public class MigrateFrom16to17 {

    private static final Log log = LogFactory.getLog(MigrateFrom16to17.class);
    private List<Tenant> tenantsArray;

    public MigrateFrom16to17(String tenantArguments) throws UserStoreException {
        TenantManager tenantManager = ServiceHolder.getRealmService().getTenantManager();

        if (tenantArguments != null) {  // Tenant arguments have been provided so need to load specific ones
            tenantArguments = tenantArguments.replaceAll("\\s",""); // Remove spaces and tabs

            tenantsArray = new ArrayList();

            if (tenantArguments.contains(",")) { // Multiple arguments specified
                String[] parts = tenantArguments.split(",");

                for (int i = 0; i < parts.length; ++i) {
                    if (parts[i].length() > 0) {
                        populateTenants(tenantManager, tenantsArray, parts[i]);
                    }
                }
            }
            else { // Only single argument provided
                populateTenants(tenantManager, tenantsArray, tenantArguments);
            }
        } else {  // Load all tenants
            tenantsArray = new ArrayList(Arrays.asList(tenantManager.getAllTenants()));
            Tenant superTenant = new Tenant();
            superTenant.setDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            superTenant.setId(MultitenantConstants.SUPER_TENANT_ID);
            tenantsArray.add(superTenant);
        }
    }

    private void populateTenants(TenantManager tenantManager, List<Tenant> tenantList, String argument) throws UserStoreException {
        log.debug("Argument provided : " + argument);

        if (argument.contains("@")) { // Username provided as argument
            int tenantID = tenantManager.getTenantId(argument);

            if (tenantID != -1) {
                tenantList.add(tenantManager.getTenant(tenantID));
            }
            else {
                log.error("Tenant does not exist for username " + argument);
            }
        }
        else { // Domain name provided as argument
            Tenant[] tenants = tenantManager.getAllTenantsForTenantDomainStr(argument);

            if (tenants.length > 0) {
                tenantList.addAll(Arrays.asList(tenants));
            }
            else {
                log.error("Tenant does not exist for domain " + argument);
            }
        }
    }


    /**
     * This method is used to migrate all registry resources
     * This migrates swagger resources and rxts
     *
     * @throws APIMigrationException
     */
    public void registryResourceMigration() throws APIMigrationException {
        swaggerMigration();
        docResourceMigration();
        rxtMigration();
    }


    /**
     * This method is used to migrate rxt
     * This adds three new attributes to the api rxt
     *
     * @throws APIMigrationException
     */
    void rxtMigration() throws APIMigrationException {
        log.info("Rxt migration for API Manager 1.9.0 started.");
        boolean isTenantFlowStarted = false;
        for (Tenant tenant : tenantsArray) {
            log.debug("Start rxtMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
            try {
                PrivilegedCarbonContext.startTenantFlow();
                isTenantFlowStarted = true;

                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenant.getId(), true);

                Registry registry = getRegistry(tenant);
                if (registry != null) {
                    GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
                    GenericArtifact[] artifacts = getGenericArtifacts(registry, artifactManager);

                    if (artifacts != null && artifactManager != null) {
                        for (GenericArtifact artifact : artifacts) {
                            RegistryMigration.rxtMigration(artifactManager, artifacts);
                        }
                    }
                }
                else {
                    log.debug("No api artifacts found in registry for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
                }
            }

            catch (APIManagementException e) {
                ResourceUtil.handleException("Error occurred while reading API from the artifact ", e);
            } finally {
                if (isTenantFlowStarted) {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            }
            log.debug("End rxtMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
        }

        log.info("Rxt resource migration done for all the tenants");
    }


    /**
     * Doc resource migration
     *
     *
     */
    void docResourceMigration() {
        log.info("Doc migration for API Manager 1.9.0 started.");
        boolean isTenantFlowStarted = false;

        for (Tenant tenant : tenantsArray) {
            if (log.isDebugEnabled()) {
                log.debug("Start docResourceMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
            }

            try {
                PrivilegedCarbonContext.startTenantFlow();
                isTenantFlowStarted = true;

                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain());
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenant.getId());

                Registry registry = getRegistry(tenant);

                if (registry != null) {
                    GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
                    GenericArtifact[] artifacts = getGenericArtifacts(registry, artifactManager);

                    if (artifacts != null) {
                        RegistryMigration.updateDocResourceAssociation(artifacts, registry, tenant);
                    }
                }
            } catch (APIManagementException e) {
                log.error("Error when getting GenericArtifactManager for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
            } finally {
                if (isTenantFlowStarted) {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            }

            log.debug("End docResourceMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
        }

        log.info("Doc resource migration done for all the tenants.");
    }


    /**
     * This method is used to migrate swagger v1.2 resources to swagger v2.0 resource
     * This reads the swagger v1.2 doc from the registry and creates swagger v2.0 doc
     *
     * @throws APIMigrationException
     */
    void swaggerMigration() {
        log.info("Swagger migration for API Manager 1.9.0 started.");
        boolean isTenantFlowStarted = false;

        for (Tenant tenant : tenantsArray) {
            if (log.isDebugEnabled()) {
                log.debug("Start swaggerMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
            }

            try {
                PrivilegedCarbonContext.startTenantFlow();
                isTenantFlowStarted = true;

                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain());
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenant.getId());

                Registry registry = getRegistry(tenant);

                if (registry != null) {
                    GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
                    GenericArtifact[] artifacts = getGenericArtifacts(registry, artifactManager);

                    if (artifacts != null) {
                        RegistryMigration.swagger12Migration(artifacts, registry, tenant);
                    }
                }
            } catch (APIManagementException e) {
                log.error("Error when getting GenericArtifactManager for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
            } finally {
                if (isTenantFlowStarted) {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            }

            log.debug("End swaggerMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
        }

        log.info("Swagger resource migration done for all the tenants.");
    }

    private Registry getRegistry(Tenant tenant) {
        log.debug("Calling getRegistry");
        Registry registry = null;

        try {
            String adminName = ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).getRealmConfiguration().getAdminUserName();
            log.debug("Tenant admin username : " + adminName);
            registry = ServiceHolder.getRegistryService().getGovernanceUserRegistry(adminName, tenant.getId());
            ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
        } catch (UserStoreException e) {
            log.error("Error occurred while reading tenant information of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
        } catch (RegistryException e) {
            log.error("Error occurred while accessing the registry of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
        }

        return registry;
    }


    private GenericArtifact[] getGenericArtifacts(Registry registry, GenericArtifactManager manager) {
        log.debug("Calling getGenericArtifacts");
        GenericArtifact[] artifacts = null;

        try {
            if (GovernanceUtils.findGovernanceArtifactConfiguration(Constants.API, registry) != null) {
                GovernanceUtils.loadGovernanceArtifacts((UserRegistry) registry);
                artifacts = manager.getAllGenericArtifacts();

                log.debug("Total number of api artifacts : " + artifacts.length);
            }
            else {
                log.debug("API artifacts do not exist in registry");
            }

        } catch (RegistryException e) {
            log.error("Error occurred when getting GenericArtifacts from registry", e);
        }

        return artifacts;
    }


    /**
     * This method is used to migrate all the file system components
     * such as sequences and synapse files
     *
     * @throws APIMigrationException
     */
    public void fileSystemMigration() throws APIMigrationException {
        synapseAPIMigration();
    }


    /**
     * This method is used to migrate synapse files
     * This changes the synapse api and add the new handlers
     *
     * @throws APIMigrationException
     */

    void synapseAPIMigration() {
        String repository = CarbonUtils.getCarbonRepository();
        String tenantRepository = CarbonUtils.getCarbonTenantsDirPath();
        for (Tenant tenant : tenantsArray) {
            log.debug("Start synapseAPIMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
            String apiFilePath;
            if (tenant.getId() != MultitenantConstants.SUPER_TENANT_ID) {
                apiFilePath = tenantRepository + "/" + tenant.getId() +
                        "/synapse-configs/default/api";
            } else {
                apiFilePath = repository + "synapse-configs/default/api";
            }
            File APIFiles = new File(apiFilePath);
            File[] synapseFiles = APIFiles.listFiles();

            if (synapseFiles == null) {
                log.debug("No api folder " + apiFilePath + " exists for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
                continue;
            }

            for (File synapseFile : synapseFiles) {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                docFactory.setNamespaceAware(true);

                try {
                    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                    Document doc = docBuilder.parse(synapseFile);

                    doc.getDocumentElement().normalize();

                    Element rootElement = doc.getDocumentElement();

                    if (Constants.SYNAPSE_API_ROOT_ELEMENT.equals(rootElement.getNodeName()) &&
                            rootElement.hasAttribute(Constants.SYNAPSE_API_ATTRIBUTE_VERSION)) {
                        SynapseMigration.modifyGoogleAnalyticsTrackingHandler(synapseFile, doc);
                    }


                } catch (ParserConfigurationException e) {
                    log.error("Parsing exception encountered for " + synapseFile.getAbsolutePath(), e);
                } catch (SAXException e) {
                    log.error("SAX exception encountered for " + synapseFile.getAbsolutePath(), e);
                } catch (IOException e) {
                    log.error("IO exception encountered for " + synapseFile.getAbsolutePath(), e);
                } catch (XPathExpressionException e) {
                    log.error("XPathExpression exception encountered for " + synapseFile.getAbsolutePath(), e);
                } catch (TransformerException e) {
                    log.error("Transformer exception encountered for " + synapseFile.getAbsolutePath(), e);
                }
            }

            log.debug("End synapseAPIMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
        }
    }
}
