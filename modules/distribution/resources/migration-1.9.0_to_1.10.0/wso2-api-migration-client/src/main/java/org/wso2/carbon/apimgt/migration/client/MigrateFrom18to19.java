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

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.definitions.APIDefinitionFromSwagger20;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.client.internal.ServiceHolder;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.ResourceUtil;
import org.wso2.carbon.apimgt.migration.util.StatDBUtil;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.api.Resource;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.MalformedURLException;
import java.sql.*;
import java.util.*;


/**
 * This class contains all the methods which is used to migrate APIs from APIManager 1.8.0 to APIManager 1.9.0.
 * The migration performs in database, registry and file system
 */

@SuppressWarnings("unchecked")
public class MigrateFrom18to19 implements MigrationClient {

    private static final Log log = LogFactory.getLog(MigrateFrom18to19.class);
    private List<Tenant> tenantsArray;

    public MigrateFrom18to19(String tenantArguments, String blackListTenantArguments) throws UserStoreException {
        TenantManager tenantManager = ServiceHolder.getRealmService().getTenantManager();

        if (tenantArguments != null) {  // Tenant arguments have been provided so need to load specific ones
            tenantArguments = tenantArguments.replaceAll("\\s", ""); // Remove spaces and tabs

            tenantsArray = new ArrayList();

            buildTenantList(tenantManager, tenantsArray, tenantArguments);
        } else if (blackListTenantArguments != null) {
            blackListTenantArguments = blackListTenantArguments.replaceAll("\\s", ""); // Remove spaces and tabs

            List<Tenant> blackListTenants = new ArrayList();
            buildTenantList(tenantManager, blackListTenants, blackListTenantArguments);

            List<Tenant> allTenants = new ArrayList(Arrays.asList(tenantManager.getAllTenants()));
            Tenant superTenant = new Tenant();
            superTenant.setDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            superTenant.setId(MultitenantConstants.SUPER_TENANT_ID);
            allTenants.add(superTenant);

            tenantsArray = new ArrayList();

            for (Tenant tenant : allTenants) {
                boolean isBlackListed = false;
                for (Tenant blackListTenant : blackListTenants) {
                    if (blackListTenant.getId() == tenant.getId()) {
                        isBlackListed = true;
                        break;
                    }
                }

                if (!isBlackListed) {
                    tenantsArray.add(tenant);
                }
            }

        } else {  // Load all tenants
            tenantsArray = new ArrayList(Arrays.asList(tenantManager.getAllTenants()));
            Tenant superTenant = new Tenant();
            superTenant.setDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            superTenant.setId(MultitenantConstants.SUPER_TENANT_ID);
            tenantsArray.add(superTenant);
        }
    }


    private void buildTenantList(TenantManager tenantManager, List<Tenant> tenantList, String tenantArguments) throws UserStoreException {
        if (tenantArguments.contains(",")) { // Multiple arguments specified
            String[] parts = tenantArguments.split(",");

            for (int i = 0; i < parts.length; ++i) {
                if (parts[i].length() > 0) {
                    populateTenants(tenantManager, tenantList, parts[i]);
                }
            }
        } else { // Only single argument provided
            populateTenants(tenantManager, tenantList, tenantArguments);
        }
    }


    private void populateTenants(TenantManager tenantManager, List<Tenant> tenantList, String argument) throws UserStoreException {
        log.debug("Argument provided : " + argument);

        if (argument.contains("@")) { // Username provided as argument
            int tenantID = tenantManager.getTenantId(argument);

            if (tenantID != -1) {
                tenantList.add(tenantManager.getTenant(tenantID));
            } else {
                log.error("Tenant does not exist for username " + argument);
            }
        } else { // Domain name provided as argument
            Tenant[] tenants = tenantManager.getAllTenantsForTenantDomainStr(argument);

            if (tenants.length > 0) {
                tenantList.addAll(Arrays.asList(tenants));
            } else {
                log.error("Tenant does not exist for domain " + argument);
            }
        }
    }

    /**
     * This method is used to migrate database tables
     * This executes the database queries according to the user's db type and alters the tables
     *
     * @param migrateVersion version to be migrated
     * @throws SQLException
     */
    @Override
    public void databaseMigration(String migrateVersion) throws SQLException {
        log.info("Database migration for API Manager " + Constants.VERSION_1_9 + " started");
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        BufferedReader bufferedReader = null;
        try {
            connection = APIMgtDBUtil.getConnection();
            String dbType = MigrationDBCreator.getDatabaseType(connection);
            String dbScript = ResourceUtil.pickQueryFromResources(migrateVersion, dbType);

            InputStream is = new FileInputStream(dbScript);
            bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF8"));
            String sqlQuery;
            while ((sqlQuery = bufferedReader.readLine()) != null) {
                if (Constants.DB_TYPE_ORACLE.equals(dbType)) {
                    sqlQuery = sqlQuery.replace(";", "");
                }
                sqlQuery = sqlQuery.trim();
                if (sqlQuery.startsWith("//") || sqlQuery.startsWith("--")) {
                    continue;
                }
                StringTokenizer stringTokenizer = new StringTokenizer(sqlQuery);
                if (stringTokenizer.hasMoreTokens()) {
                    String token = stringTokenizer.nextToken();
                    if ("REM".equalsIgnoreCase(token)) {
                        continue;
                    }
                }

                if (sqlQuery.contains("\\n")) {
                    sqlQuery = sqlQuery.replace("\\n", "");
                }

                if (sqlQuery.length() > 0) {
                    preparedStatement = connection.prepareStatement(sqlQuery.trim());
                    preparedStatement.execute();
                    connection.commit();
                }
            }

            //To drop the foreign key
            dropFKConstraint(migrateVersion, dbType);

            bufferedReader.close();

        } catch (IOException e) {
            //Errors logged to let user know the state of the db migration and continue other resource migrations
            log.error("Error occurred while migrating databases", e);
        } catch (Exception e) {
            /* MigrationDBCreator extends from org.wso2.carbon.utils.dbcreator.DatabaseCreator and in the super class
            method getDatabaseType throws generic Exception */
            log.error("Error occurred while migrating databases", e);
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        log.info("DB resource migration done for all the tenants");
    }

    /**
     * This method is used to remove the FK constraint which is unnamed
     * This finds the name of the constraint and build the query to delete the constraint and execute it
     *
     * @param migrateVersion version to be migrated
     * @param dbType         database type of the user
     * @throws SQLException
     * @throws IOException
     */
    public void dropFKConstraint(String migrateVersion, String dbType) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Statement statement = null;
        try {
            String queryToExecute = ResourceUtil.pickQueryFromResources(migrateVersion, Constants.CONSTRAINT).trim();
            String queryArray[] = queryToExecute.split(Constants.LINE_BREAK);

            connection = APIMgtDBUtil.getConnection();
            connection.setAutoCommit(false);
            statement = connection.createStatement();
            if (Constants.DB_TYPE_ORACLE.equals(dbType)) {
                queryArray[0] = queryArray[0].replace(Constants.DELIMITER, "");
            }
            resultSet = statement.executeQuery(queryArray[0]);
            String constraintName = null;

            while (resultSet.next()) {
                constraintName = resultSet.getString("constraint_name");
            }

            if (constraintName != null) {
                queryToExecute = queryArray[1].replace("<temp_key_name>", constraintName);
                if (Constants.DB_TYPE_ORACLE.equals(dbType)) {
                    queryToExecute = queryToExecute.replace(Constants.DELIMITER, "");
                }

                if (queryToExecute.contains("\\n")) {
                    queryToExecute = queryToExecute.replace("\\n", "");
                }
                preparedStatement = connection.prepareStatement(queryToExecute);
                preparedStatement.execute();
                connection.commit();
            }
        } catch (APIMigrationException e) {
            //Foreign key might be already deleted, log the error and let it continue
            log.error("Error occurred while deleting foreign key", e);
        } catch (IOException e) {
            //If user does not add the file migration will continue and migrate the db without deleting
            // the foreign key reference
            log.error("Error occurred while finding the foreign key deletion query for execution", e);
        } finally {
            if (statement != null) {
                statement.close();
            }

            APIMgtDBUtil.closeAllConnections(preparedStatement, connection, resultSet);
        }

    }

    /**
     * This method is used to migrate all registry resources
     * This migrates swagger resources and rxts
     *
     * @throws APIMigrationException
     */
    @Override
    public void registryResourceMigration() throws APIMigrationException {
        //copyNewRxtFileToRegistry();
        swaggerResourceMigration();
        rxtMigration();
        externalStoreMigration();
    }

    void externalStoreMigration() throws APIMigrationException {
        log.info("External API store migration for API Manager " + Constants.VERSION_1_9 + " started.");
        boolean isTenantFlowStarted = false;
        for (Tenant tenant : tenantsArray) {
            log.debug("Start API store migration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
            Registry registry = null;
            try {
                PrivilegedCarbonContext.startTenantFlow();
                isTenantFlowStarted = true;

                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenant.getId(), true);

                String adminName = ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId())
                        .getRealmConfiguration().getAdminUserName();
                ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
                registry = ServiceHolder.getRegistryService().getGovernanceUserRegistry(adminName, tenant
                        .getId());

                Resource externalStoreResource = registry.get(APIConstants.EXTERNAL_API_STORES_LOCATION);

                String modifiedConfig = modifyExternalStores(externalStoreResource);
                externalStoreResource.setContent(modifiedConfig);
                registry.put(APIConstants.EXTERNAL_API_STORES_LOCATION, externalStoreResource);


            } catch (RegistryException e) {
                ResourceUtil.handleException("Error occurred while accessing the registry", e);
                try {
                    if (registry != null) {
                        registry.rollbackTransaction();
                    }
                } catch (org.wso2.carbon.registry.core.exceptions.RegistryException ex) {
                    ResourceUtil.handleException("Error occurred while accessing the registry", ex);
                }
            } catch (UserStoreException e) {
                ResourceUtil.handleException("Error occurred while reading tenant information", e);
            } finally {
                if (isTenantFlowStarted) {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            }
            log.debug("End API store migration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
        }
    }

    String modifyExternalStores(Resource resource) throws APIMigrationException {
        String xmlContent;
        Writer stringWriter = new StringWriter();
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            xmlContent = new String((byte[]) resource.getContent(), "UTF8");
            Document doc = docBuilder.parse(new InputSource(new ByteArrayInputStream(xmlContent.getBytes())));

            NodeList externalAPIStores = doc.getElementsByTagName(Constants.EXTERNAL_API_STORE);

            for (int i = 0; i < externalAPIStores.getLength(); i++) {
                Element externalStoreElement = (Element) externalAPIStores.item(i);
                NamedNodeMap attributes =  externalStoreElement.getAttributes();
                boolean isAttribExists = false;

                for (int j = 0; j < attributes.getLength(); j++) {
                    if(attributes.item(j).getNodeName().equals(Constants.ATTRIBUTE_CLASSNAME)) {
                        isAttribExists = true;
                    }
                }
                if(!isAttribExists) {
                    externalStoreElement.setAttribute(Constants.ATTRIBUTE_CLASSNAME, Constants.API_PUBLISHER_CLASSNAME);
                }
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(doc), new StreamResult(stringWriter));

        } catch (SAXException e) {
            ResourceUtil.handleException("Error occurred while parsing the xml document", e);
        } catch (IOException e) {
            ResourceUtil.handleException("Error occurred while reading the xml document. " +
                    "Please check for external API config file in the registry", e);
        } catch (ParserConfigurationException e) {
            ResourceUtil.handleException("Error occurred while trying to build the xml document", e);
        } catch (TransformerException e) {
            ResourceUtil.handleException("Error occurred while saving modified the xml document", e);
        } catch (RegistryException e) {
            ResourceUtil.handleException("Error occurred while reading the content from registry resource", e);
        }

        return stringWriter.toString();
    }


    /**
     * This method is used to migrate rxt
     * This adds three new attributes to the api rxt
     *
     * @throws APIMigrationException
     */
    void rxtMigration() throws APIMigrationException {
        log.info("Rxt migration for API Manager " + Constants.VERSION_1_9 + " started.");
        boolean isTenantFlowStarted = false;
        for (Tenant tenant : tenantsArray) {
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
                            log.error("Cannot find corresponding api for registry artifact " + artifact.getAttribute("overview_name") + "-" + artifact.getAttribute("overview_version") + "-" + artifact.getAttribute("overview_provider") +
                                    " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ") in AM_DB");
                            continue;
                        }

                        APIIdentifier apiIdentifier = api.getId();
                        String apiVersion = apiIdentifier.getVersion();

                        if (!(api.getContext().endsWith(RegistryConstants.PATH_SEPARATOR + apiVersion))) {
                            artifact.setAttribute("overview_context", api.getContext() +
                                    RegistryConstants.PATH_SEPARATOR + apiVersion);
                        }

                        artifact.addAttribute("overview_contextTemplate", api.getContext() +
                                RegistryConstants.PATH_SEPARATOR + "{version}");
                        artifact.addAttribute("overview_environments", "");
                        artifact.addAttribute("overview_versionType", "");

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


    /**
     * This method is used to copy new rxt to the registry
     * This copies rxt from the file system to registry
     *
     * @throws APIMigrationException
     */
    void copyNewRxtFileToRegistry() throws APIMigrationException {
        boolean isTenantFlowStarted = false;
        try {
            String resourcePath = Constants.GOVERNANCE_COMPONENT_REGISTRY_LOCATION + "/types/api.rxt";
            File newRxtFile = new File(CarbonUtils.getCarbonHome() + Constants.RXT_PATH);
            String rxtContent = FileUtils.readFileToString(newRxtFile, "UTF-8");

            for (Tenant tenant : tenantsArray) {
                int tenantId = tenant.getId();
                isTenantFlowStarted = true;
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenant.getId(), true);

                String adminName = ServiceHolder.getRealmService().getTenantUserRealm(tenantId)
                        .getRealmConfiguration().getAdminUserName();
                ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenantId);
                Registry registry = ServiceHolder.getRegistryService().getGovernanceUserRegistry(adminName, tenantId);
                GovernanceUtils.loadGovernanceArtifacts((UserRegistry) registry);

                Resource resource;
                if (!registry.resourceExists(resourcePath)) {
                    resource = registry.newResource();
                } else {
                    resource = registry.get(resourcePath);
                }
                resource.setContent(rxtContent);
                resource.setMediaType("application/xml");
                registry.put(resourcePath, resource);

                ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).getAuthorizationManager()
                        .authorizeRole(APIConstants.ANONYMOUS_ROLE, resourcePath, ActionConstants.GET);

                if (isTenantFlowStarted) {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            }
            //_system/governance/repository/components/org.wso2.carbon.governance/types/api.rxt

        } catch (IOException e) {
            ResourceUtil.handleException("Error occurred while reading the rxt file from file system.  ", e);
        } catch (UserStoreException e) {
            ResourceUtil.handleException("Error occurred while searching for tenant admin. ", e);
        } catch (RegistryException e) {
            ResourceUtil.handleException("Error occurred while performing registry operation. ", e);
        } finally {
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
    }


    /**
     * This method is used to migrate swagger v1.2 resources to swagger v2.0 resource
     * This reads the swagger v1.2 doc from the registry and creates swagger v2.0 doc
     *
     * @throws APIMigrationException
     */
    void swaggerResourceMigration() throws APIMigrationException {
        log.info("Swagger migration for API Manager " + Constants.VERSION_1_9 + " started.");
        boolean isTenantFlowStarted = false;

        for (Tenant tenant : tenantsArray) {
            if (log.isDebugEnabled()) {
                log.debug("Start swaggerResourceMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
            }

            try {
                PrivilegedCarbonContext.startTenantFlow();
                isTenantFlowStarted = true;

                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain());
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenant.getId());

                Registry registry = getRegistry(tenant);

                if (registry != null) {
                    GenericArtifact[] artifacts = getGenericArtifacts(registry);

                    if (artifacts != null) {
                        updateSwaggerResources(artifacts, registry, tenant);
                    }
                }
            } finally {
                if (isTenantFlowStarted) {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            }

            log.debug("End swaggerResourceMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
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


    private GenericArtifact[] getGenericArtifacts(Registry registry) {
        log.debug("Calling getGenericArtifacts");
        GenericArtifact[] artifacts = null;

        try {
            if (GovernanceUtils.findGovernanceArtifactConfiguration(Constants.API, registry) != null) {
                GenericArtifactManager manager = new GenericArtifactManager(registry, Constants.API);
                GovernanceUtils.loadGovernanceArtifacts((UserRegistry) registry);
                artifacts = manager.getAllGenericArtifacts();

                log.debug("Total number of api artifacts : " + artifacts.length);
            } else {
                log.debug("API artifacts do not exist in registry");
            }

        } catch (RegistryException e) {
            log.error("Error occurred when getting GenericArtifacts from registry", e);
        }

        return artifacts;
    }

    private API getAPI(GenericArtifact artifact) {
        log.debug("Calling getAPI");
        API api = null;

        try {
            api = APIUtil.getAPI(artifact);
        } catch (APIManagementException e) {
            log.error("Error when getting api artifact " + artifact.getId() + " from registry", e);
        }

        return api;
    }

    private void updateSwaggerResources(GenericArtifact[] artifacts, Registry registry, Tenant tenant) throws APIMigrationException {
        log.debug("Calling updateSwaggerResources");
        for (GenericArtifact artifact : artifacts) {
            API api = getAPI(artifact);

            if (api != null) {
                APIIdentifier apiIdentifier = api.getId();
                String apiName = apiIdentifier.getApiName();
                String apiVersion = apiIdentifier.getVersion();
                String apiProviderName = apiIdentifier.getProviderName();
                try {
                    String swagger2location = ResourceUtil.getSwagger2ResourceLocation(apiName, apiVersion, apiProviderName);

                    // Create swagger 2.0 doc only if it does not exist
                    /*if (registry.resourceExists(swagger2location)) {
                        continue;
                    }*/

                    String swagger12location = ResourceUtil.getSwagger12ResourceLocation(apiName, apiVersion, apiProviderName);

                    String swagger2Document;

                    if (!registry.resourceExists(swagger12location)) {
                        log.debug("Creating swagger v2.0 resource from scratch for : " + apiName + "-" + apiVersion + "-" + apiProviderName);

                        APIDefinitionFromSwagger20 definitionFromSwagger20 = new APIDefinitionFromSwagger20();

                        swagger2Document = definitionFromSwagger20.generateAPIDefinition(api);
                    } else {
                        log.debug("Creating swagger v2.0 resource using v1.2 for : " + apiName + "-" + apiVersion + "-" + apiProviderName);
                        swagger2Document = getSwagger2docUsingSwagger12RegistryResources(registry, swagger12location, api);
                    }

                    Resource docContent = registry.newResource();
                    docContent.setContent(swagger2Document);
                    docContent.setMediaType("application/json");
                    registry.put(swagger2location, docContent);

                    ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).getAuthorizationManager()
                            .authorizeRole(APIConstants.ANONYMOUS_ROLE,
                                    "_system/governance" + swagger2location, ActionConstants.GET);
                    ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).getAuthorizationManager()
                            .authorizeRole(APIConstants.EVERYONE_ROLE,
                                    "_system/governance" + swagger2location, ActionConstants.GET);
                } catch (RegistryException e) {
                    log.error("Registry error encountered for api " + apiName + "-" + apiVersion + "-" + apiProviderName + " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
                } catch (ParseException e) {
                    log.error("Error occurred while parsing swagger v1.2 document for api " + apiName + "-" + apiVersion + "-" + apiProviderName + " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
                } catch (UserStoreException e) {
                    log.error("Error occurred while setting permissions of swagger v2.0 document for api " + apiName + "-" + apiVersion + "-" + apiProviderName + " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
                } catch (MalformedURLException e) {
                    log.error("Error occurred while creating swagger v2.0 document for api " + apiName + "-" + apiVersion + "-" + apiProviderName + " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
                } catch (APIManagementException e) {
                    log.error("Error occurred while creating swagger v2.0 document for api " + apiName + "-" + apiVersion + "-" + apiProviderName + " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
                }
            }
        }
    }


    /**
     * This method generates swagger v2 doc using swagger 1.2 doc
     *
     * @param registry          governance registry
     * @param swagger12location the location of swagger 1.2 doc
     * @return JSON string of swagger v2 doc
     * @throws java.net.MalformedURLException
     * @throws org.json.simple.parser.ParseException
     * @throws org.wso2.carbon.registry.core.exceptions.RegistryException
     */

    private String getSwagger2docUsingSwagger12RegistryResources(Registry registry, String swagger12location, API api)
            throws MalformedURLException, ParseException, RegistryException {
        log.debug("Calling getSwagger2docUsingSwagger12RegistryResources");
        JSONParser parser = new JSONParser();
        String swagger12BasePath = null;

        Resource swaggerRes = registry.get(swagger12location + APIConstants.API_DOC_1_2_RESOURCE_NAME);

        try {
            JSONObject swagger12doc = (JSONObject) parser.parse(new String((byte[]) swaggerRes.getContent(), "UTF8"));

            Map<String, JSONArray> apiDefPaths = new HashMap<String, JSONArray>();
            Resource swagger12Res = registry.get(swagger12location);

            //get all the resources inside the 1.2 resource location
            String[] apiDefinitions = (String[]) swagger12Res.getContent();

            //get each resource in the 1.2 folder except the api-doc resource
            for (String apiDefinition : apiDefinitions) {

                String resourceName = apiDefinition.substring(apiDefinition.lastIndexOf("/"));
                //skip if api-doc file
                if (resourceName.equals(APIConstants.API_DOC_1_2_RESOURCE_NAME)) {
                    continue;
                }

                Resource resource = registry.get(apiDefinition);

                String swaggerDocContent;

                if (resource.getContent() instanceof String[]) {
                    swaggerDocContent = Arrays.toString((String[]) resource.getContent());
                } else {
                    swaggerDocContent = new String((byte[]) resource.getContent(), "UTF8");
                }

                log.debug("swaggerDocContent : " + swaggerDocContent);

                JSONObject apiDef = (JSONObject) parser.parse(swaggerDocContent);
                //get the base path. this is same for all api definitions.
                swagger12BasePath = (String) apiDef.get("basePath");
                if (apiDef.containsKey("apis")) {
                    JSONArray apiArray = (JSONArray) apiDef.get("apis");
                    for (Object anApiArray : apiArray) {
                        JSONObject apiObject = (JSONObject) anApiArray;
                        String path = (String) apiObject.get("path");
                        JSONArray operations = (JSONArray) apiObject.get("operations");
                        //set the operations object inside each api definition and set it in a map against its resource path
                        apiDefPaths.put(path, operations);
                    }
                } else {
                    log.error("Cannot find resources in swagger v1.2 document");
                }
            }
            JSONObject swagger2Doc = generateSwagger2Document(swagger12doc, apiDefPaths, swagger12BasePath, api);
            return swagger2Doc.toJSONString();
        } catch (UnsupportedEncodingException e) {
            log.error("Error while reading swagger resource", e);
        }

        return null;
    }


    /**
     * Generate Swagger v2.0 document using Swagger v1.2 resources
     *
     * @param swagger12doc      Old Swagger Document
     * @param apiDefPaths       Paths in API definition
     * @param swagger12BasePath Location of swagger v1.2 document
     * @return Swagger v2.0 document as a JSON object
     * @throws ParseException
     * @throws MalformedURLException
     */

    private static JSONObject generateSwagger2Document(JSONObject swagger12doc,
                                                       Map<String, JSONArray> apiDefPaths, String swagger12BasePath, API api)
            throws ParseException, MalformedURLException {
        log.debug("Calling generateSwagger2Document");

        //create swagger 2.0 doc
        JSONObject swagger20doc = new JSONObject();

        //set swagger version
        swagger20doc.put(Constants.SWAGGER, Constants.SWAGGER_V2);

        //set the info object
        JSONObject info = generateInfoObject(swagger12doc, api);
        //update info object
        swagger20doc.put(Constants.SWAGGER_INFO, info);

        //set the paths object
        JSONObject pathObj = generatePathsObj(apiDefPaths);
        //JSONObject pathObj = generatePathsObj(api);
        swagger20doc.put(Constants.SWAGGER_PATHS, pathObj);

        //Base path and host is not needed for swagger v2.0
        /*if (swagger12BasePath != null) {
            URL url = new URL(swagger12BasePath);
            swagger20doc.put(Constants.SWAGGER_HOST, url.getHost());
            swagger20doc.put(Constants.SWAGGER_BASE_PATH, url.getPath());

            JSONArray schemes = new JSONArray();
            schemes.add(url.getProtocol());
            swagger20doc.put(Constants.SWAGGER_SCHEMES, schemes);
        }
        else {
            log.debug("swagger12BasePath is null");
        }*/

        //securityDefinitions
        if (swagger12doc.containsKey(Constants.SWAGGER_AUTHORIZATIONS)) {
            JSONObject securityDefinitions = generateSecurityDefinitionsObject(swagger12doc);
            swagger20doc.put(Constants.SWAGGER_X_WSO2_SECURITY, securityDefinitions);
        }

        //Add a sample definition
        JSONObject sampleDefinitionObject = new JSONObject();
        JSONObject sampleItemObject = new JSONObject();
        sampleDefinitionObject.put(Constants.SWAGGER_SAMPLE_DEFINITION, sampleItemObject);
        swagger20doc.put(Constants.SWAGGER_DEFINITIONS, sampleDefinitionObject);

        return swagger20doc;
    }

    /**
     * Generate swagger v2 security definition object
     * See <a href="https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#securityDefinitionsObject">
     * Swagger v2 definition object</a>
     *
     * @param swagger12doc Old Swagger Document
     * @return security definition object
     * @throws ParseException
     */
    private static JSONObject generateSecurityDefinitionsObject(JSONObject swagger12doc) throws ParseException {
        log.debug("Calling generateSecurityDefinitionsObject");
        JSONParser parser = new JSONParser();
        JSONObject securityDefinitionObject = new JSONObject();
        JSONObject securitySchemeObject = (JSONObject) parser.parse(Constants.DEFAULT_SECURITY_SCHEME);
        if(swagger12doc.containsKey(Constants.SWAGGER_AUTHORIZATIONS)) {
            JSONObject authorizations = (JSONObject) swagger12doc.get(Constants.SWAGGER_AUTHORIZATIONS);
            Set authTypes = authorizations.keySet();
            for (Object obj : authTypes) {
                JSONObject authObj = (JSONObject) authorizations.get(obj.toString());
                if (authObj.containsKey(Constants.SWAGGER_SCOPES)) {
                    //Put it to custom WSO2 scopes
                    if(authObj.containsKey(Constants.SWAGGER_SCOPES)) {
                        securitySchemeObject.put(Constants.SWAGGER_X_WSO2_SCOPES, authObj.get(Constants.SWAGGER_SCOPES));
                    }
                }
                securityDefinitionObject.put(Constants.SWAGGER_OBJECT_NAME_APIM, securitySchemeObject);
            }
        }
        return securityDefinitionObject;
    }

    /**
     * generate swagger v2 info object using swagger 1.2 doc.
     * See <a href="https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#infoObject">Swagger v2 info
     * object</a>
     *
     * @param swagger12doc Old Swagger Document
     * @return swagger v2 infoObject
     * @throws ParseException
     */
    private static JSONObject generateInfoObject(JSONObject swagger12doc, API api) throws ParseException {

        JSONObject infoObj;
        JSONParser parser = new JSONParser();
        JSONObject swagger2InfoObj = (JSONObject) parser.parse(Constants.DEFAULT_INFO);
        String title = api.getId().getApiName();
        String version = api.getId().getVersion();


        /*Check whether info object is available in swagger 1.2 or not, if not skip reading it and get title and version
        from API object */
        if (swagger12doc.containsKey("info")) {
            infoObj = (JSONObject) swagger12doc.get("info");
            if (infoObj.containsKey("title")) {
                title = (String) infoObj.get("title");
            }
            if (infoObj.containsKey("apiVersion")) {
                version = (String) swagger12doc.get("apiVersion");
            }
            if (infoObj.containsKey(Constants.SWAGGER_DESCRIPTION)) {
                swagger2InfoObj.put(Constants.SWAGGER_DESCRIPTION, infoObj.get("description"));
            }
            if (infoObj.containsKey(Constants.SWAGGER_TERMS_OF_SERVICE_URL)) {
                swagger2InfoObj.put(Constants.SWAGGER_TERMS_OF_SERVICE, infoObj.get(Constants.SWAGGER_TERMS_OF_SERVICE_URL));
            }

            //contact object
            if (infoObj.containsKey(Constants.SWAGGER_CONTACT)) {
                JSONObject contactsObj = new JSONObject();
                String contact = (String) infoObj.get(Constants.SWAGGER_CONTACT);
                if (contact.contains("http")) {
                    contactsObj.put(Constants.SWAGGER_URL, contact);
                } else if (contact.contains("@")) {
                    contactsObj.put(Constants.SWAGGER_EMAIL, contact);
                } else {
                    contactsObj.put(Constants.SWAGGER_NAME, contact);
                }
                swagger2InfoObj.put(Constants.SWAGGER_CONTACT, contactsObj);
            }

            //licence object
            JSONObject licenseObj = new JSONObject();
            if (infoObj.containsKey(Constants.SWAGGER_LICENCE)) {
                licenseObj.put(Constants.SWAGGER_NAME, infoObj.get(Constants.SWAGGER_LICENCE));
            }
            if (infoObj.containsKey(Constants.SWAGGER_LICENCE_URL)) {
                licenseObj.put(Constants.SWAGGER_URL, infoObj.get(Constants.SWAGGER_LICENCE_URL));
            }
            if (!licenseObj.isEmpty()) {
                swagger2InfoObj.put(Constants.SWAGGER_LICENCE, licenseObj);
            }
        }
        swagger2InfoObj.put(Constants.SWAGGER_TITLE, title);
        swagger2InfoObj.put(Constants.SWAGGER_VER, version);
        return swagger2InfoObj;
    }

    /**
     * Generate Swagger v2 paths object from swagger v1.2 document
     * See <a href="https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#paths-object">Swagger v2
     * paths object</a>
     *
     * @param apiDefinitionPaths API definition paths
     * @return swagger v2 paths object
     * @throws ParseException
     */
    private static JSONObject generatePathsObj(Map<String, JSONArray> apiDefinitionPaths) throws ParseException {
        JSONObject pathsObj = new JSONObject();

        String defaultType = "string";
        //add default response
        JSONObject responseObject = new JSONObject();
        JSONObject status200 = new JSONObject();
        status200.put(Constants.SWAGGER_DESCRIPTION, "OK");
        responseObject.put(Constants.SWAGGER_RESPONSE_200, status200);

        for (Map.Entry<String, JSONArray> entry : apiDefinitionPaths.entrySet()) {
            String key = entry.getKey();
            JSONArray operations = entry.getValue();
            JSONObject pathItemObj = new JSONObject();
            for (Object operation : operations) {
                JSONObject operationObject = (JSONObject) operation;
                if (operationObject.containsKey("method")) {
                    String method = (String) operationObject.get("method");
                    if (operationObject.containsKey("parameters")) {
                        JSONArray swagger2ParamObjects = (JSONArray) operationObject.get("parameters");
                        JSONObject swagger2OperationsObj = new JSONObject();
                        JSONArray newParameters = new JSONArray();
                        for (Object swagger2ParamObj : swagger2ParamObjects) {
                            JSONObject oldParam = (JSONObject) swagger2ParamObj;
                            JSONObject paramObj = new JSONObject();
                            paramObj.put(Constants.SWAGGER_NAME, oldParam.get(Constants.SWAGGER_NAME));

                            paramObj.put(Constants.SWAGGER_PARAM_TYPE_IN, oldParam.get("paramType"));
                            if (Constants.SWAGGER_PARAM_TYPE_BODY.equals(oldParam.get("paramType"))) {
                                JSONObject refObject = new JSONObject();
                                refObject.put(Constants.SWAGGER_REF, "#/definitions/sampleItem");
                                paramObj.put(Constants.SWAGGER_BODY_SCHEMA, refObject);
                            } else {
                                if (paramObj.containsKey(Constants.SWAGGER_REQUIRED_PARAM)) {
                                    paramObj.put(Constants.SWAGGER_PARAM_TYPE, oldParam.get("type"));
                                } else {
                                    paramObj.put(Constants.SWAGGER_PARAM_TYPE, defaultType);
                                }
                            }

                            if (paramObj.containsKey(Constants.SWAGGER_REQUIRED_PARAM)) {
                                paramObj.put(Constants.SWAGGER_REQUIRED_PARAM, oldParam.get(Constants.SWAGGER_REQUIRED_PARAM));
                            } else {
                                paramObj.put(Constants.SWAGGER_REQUIRED_PARAM, false);
                            }
                            if (paramObj.containsKey(Constants.SWAGGER_DESCRIPTION)) {
                                paramObj.put(Constants.SWAGGER_DESCRIPTION, oldParam.get(Constants.SWAGGER_DESCRIPTION));
                            } else {
                                paramObj.put(Constants.SWAGGER_DESCRIPTION, "");
                            }
                            newParameters.add(paramObj);
                        }

                        //generate the Operation object
                        // (https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#operationObject)
                        swagger2OperationsObj.put(Constants.SWAGGER_OPERATION_ID, operationObject.get("nickname"));
                        //setting operation level params
                        swagger2OperationsObj.put(Constants.SWAGGER_PARAMETERS, newParameters);

                        //Add auth_type and throttling_tier
                        if (operationObject.containsKey(Constants.SWAGGER_AUTH_TYPE)) {
                            swagger2OperationsObj.put(Constants.SWAGGER_X_AUTH_TYPE, operationObject.get(Constants.SWAGGER_AUTH_TYPE));
                        }
                        if (operationObject.containsKey(Constants.SWAGGER_THROTTLING_TIER)) {
                            swagger2OperationsObj.put(Constants.SWAGGER_X_THROTTLING_TIER, operationObject.get(Constants.SWAGGER_THROTTLING_TIER));
                        }

                        if (operationObject.containsKey("notes")) {
                            swagger2OperationsObj.put(Constants.SWAGGER_DESCRIPTION, operationObject.get("notes"));
                        }
                        if (operationObject.containsKey(Constants.SWAGGER_SUMMARY)) {
                            swagger2OperationsObj.put(Constants.SWAGGER_SUMMARY, operationObject.get(Constants.SWAGGER_SUMMARY));
                        }
                        //set pathItem object for the resource
                        //(https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#pathItemObject)
                        pathItemObj.put(method.toLowerCase(), swagger2OperationsObj);
                        //set the responseObject
                        //(https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#responsesObject)
                        if (operationObject.containsKey("responseMessages")) {
                            responseObject = new JSONObject();
                            JSONArray responseMessages = (JSONArray) operationObject.get("responseMessages");
                            for (Object responseMessage : responseMessages) {
                                JSONObject errorObj = (JSONObject) responseMessage;
                                responseObject.put(errorObj.get("code"), errorObj.get("message"));
                            }
                        }
                        swagger2OperationsObj.put(Constants.SWAGGER_RESPONSES, responseObject);
                    }
                } else {
                    log.error("Needed parameter method does not exists in swagger v1.2 doc");
                }
            }
            pathsObj.put(key, pathItemObj);
        }
        return pathsObj;
    }


    /**
     * This method is used to clean old registry resources.
     * This deletes the old swagger v1.2 resource from the registry
     *
     * @throws APIMigrationException
     */
    @Override
    public void cleanOldResources() throws APIMigrationException {
        log.info("Resource cleanup started for API Manager " + Constants.VERSION_1_9);
        try {
            for (Tenant tenant : tenantsArray) {
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain());
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenant.getId());

                String adminName = ServiceHolder.getRealmService().getTenantUserRealm(
                        tenant.getId()).getRealmConfiguration().getAdminUserName();
                ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
                Registry registry = ServiceHolder.getRegistryService().getGovernanceUserRegistry(adminName,
                        tenant.getId());
                GenericArtifactManager manager = new GenericArtifactManager(registry, "api");
                GovernanceUtils.loadGovernanceArtifacts((UserRegistry) registry);
                GenericArtifact[] artifacts = manager.getAllGenericArtifacts();

                for (GenericArtifact artifact : artifacts) {
                    API api;

                    api = APIUtil.getAPI(artifact, registry);

                    APIIdentifier apiIdentifier = api.getId();
                    String apiName = apiIdentifier.getApiName();
                    String apiVersion = apiIdentifier.getVersion();
                    String apiProviderName = apiIdentifier.getProviderName();

                    String swagger12location = ResourceUtil.getSwagger12ResourceLocation(apiName, apiVersion,
                            apiProviderName);

                    if (registry.resourceExists(swagger12location)) {
                        registry.delete(APIConstants.API_DOC_LOCATION);
                        log.info("Resource deleted from the registry.");

                    }

                }
            }
        } catch (APIManagementException e) {
            ResourceUtil.handleException("API Management Exception occurred while migrating rxt.", e);
        } catch (UserStoreException e) {
            ResourceUtil.handleException("Error occurred while reading tenant admin.", e);
        } catch (RegistryException e) {
            ResourceUtil.handleException("Error occurred while accessing the registry.", e);
        }
        if (log.isDebugEnabled()) {
            log.debug("old resources cleaned up.");
        }
    }

    /**
     * This method is used to migrate API stats database.
     * Database schema changes and data modifications as required will  be carried out.
     *
     * @throws APIMigrationException
     */
    @Override
    public void statsMigration() throws APIMigrationException {
        StatDBUtil.updateContext();
    }

    /**
     * This method is used to migrate all the file system components
     * such as sequences and synapse files
     *
     * @throws APIMigrationException
     */
    @Override
    public void fileSystemMigration() throws APIMigrationException {
        synapseAPIMigration();
        sequenceMigration();
    }

    /**
     * This method is used to migrate sequence files
     * This adds cors_request_handler_ to sequences
     *
     * @throws APIMigrationException
     */
    void sequenceMigration() {
        String repository = CarbonUtils.getCarbonRepository();
        String TenantRepo = CarbonUtils.getCarbonTenantsDirPath();
        for (Tenant tenant : tenantsArray) {
            log.debug("Start sequenceMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
            String SequenceFilePath;
            if (tenant.getId() != MultitenantConstants.SUPER_TENANT_ID) {
                SequenceFilePath = TenantRepo + "/" + tenant.getId() +
                        "/synapse-configs/default/sequences/";
            } else {
                SequenceFilePath = repository + "synapse-configs/default/sequences/";
            }

            File sequenceFolder = new File(SequenceFilePath);
            if (!sequenceFolder.exists()) {
                log.debug("No sequence folder exists for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
                continue;
            }

            try {
                FileUtils.copyInputStreamToFile(MigrateFrom18to19.class.getResourceAsStream(
                                "/18to19Migration/sequence-scripts/_cors_request_handler_.xml"),
                        new File(SequenceFilePath + "_cors_request_handler_.xml"));
                ResourceUtil.copyNewSequenceToExistingSequences(SequenceFilePath, "_auth_failure_handler_");
                ResourceUtil.copyNewSequenceToExistingSequences(SequenceFilePath, "_throttle_out_handler_");
                ResourceUtil.copyNewSequenceToExistingSequences(SequenceFilePath, "_token_fault_");
                ResourceUtil.copyNewSequenceToExistingSequences(SequenceFilePath, "fault");
            } catch (IOException e) {
                log.error("Error occurred while reading file to copy.", e);
            } catch (APIMigrationException e) {
                log.error("Copying sequences failed", e);
            }

            log.debug("End sequenceMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
        }
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
                        ResourceUtil.updateSynapseAPI(doc, synapseFile);
                    }


                } catch (ParserConfigurationException e) {
                    log.error("Parsing exception encountered for " + synapseFile.getAbsolutePath(), e);
                } catch (SAXException e) {
                    log.error("SAX exception encountered for " + synapseFile.getAbsolutePath(), e);
                } catch (IOException e) {
                    log.error("IO exception encountered for " + synapseFile.getAbsolutePath(), e);
                } catch (APIMigrationException e) {
                    log.error("Updating synapse file failed for " + synapseFile.getAbsolutePath(), e);
                }
            }

            log.debug("End synapseAPIMigration for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
        }
    }
}
