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
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
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
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.api.Resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;


/**
 * This class contains all the methods which is used to migrate APIs from APIManager 1.8.0 to APIManager 1.9.0.
 * The migration performs in database, registry and file system
 */

@SuppressWarnings("unchecked")
public class MigrateFrom18to19 implements MigrationClient {

    private static final Log log = LogFactory.getLog(MigrateFrom18to19.class);
    private List<Tenant> tenantsArray;

    public MigrateFrom18to19() throws UserStoreException {
        TenantManager tenantManager = ServiceHolder.getRealmService().getTenantManager();
        tenantsArray = new ArrayList(Arrays.asList(tenantManager.getAllTenants()));
        Tenant superTenant = new Tenant();
        superTenant.setDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        superTenant.setId(MultitenantConstants.SUPER_TENANT_ID);
        tenantsArray.add(superTenant);
    }

    /**
     * This method is used to migrate database tables
     * This executes the database queries according to the user's db type and alters the tables
     *
     * @param migrateVersion version to be migrated
     * @throws APIMigrationException
     * @throws SQLException
     */
    @Override
    public void databaseMigration(String migrateVersion) throws SQLException {
        log.info("Database migration for API Manager 1.8.0 started");
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = APIMgtDBUtil.getConnection();
            String dbType = MigrationDBCreator.getDatabaseType(connection);
            String dbScript = ResourceUtil.pickQueryFromResources(migrateVersion, dbType);
            BufferedReader bufferedReader;

            InputStream is = new FileInputStream(dbScript);
            bufferedReader = new BufferedReader(new InputStreamReader(is));
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

        } catch (IOException e) {
            //ResourceUtil.handleException("Error occurred while finding the query. Please check the file path.", e);
            log.error("Error occurred while migrating databases", e);
        } catch (Exception e) {
            //ResourceUtil.handleException("Error occurred while finding the query. Please check the file path.", e);
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
        String constraintName = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            String queryToExecute = ResourceUtil.pickQueryFromResources(migrateVersion, Constants.CONSTRAINT).trim();
            String queryArray[] = queryToExecute.split(Constants.LINE_BREAK);

            connection = APIMgtDBUtil.getConnection();
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            if (Constants.DB_TYPE_ORACLE.equals(dbType)) {
                queryArray[0] = queryArray[0].replace(Constants.DELIMITER, "");
            }
            ResultSet resultSet = statement.executeQuery(queryArray[0]);
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
            log.error("Error occurred while deleting foreign key", e);
        } catch (IOException e) {
            log.error("Error occurred while finding the query for execution", e);
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }

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
        try {
            for (Tenant tenant : tenantsArray) {
                PrivilegedCarbonContext.startTenantFlow();
                isTenantFlowStarted = true;

                /*Use the super tenant instead of tenant because tenants do not have access to master-datasources.xml
                If you use tenant details instead of super tenant, you will get javax.naming.NameNotFoundException:
                Name [jdbc/AM_API] is not bound in this Context. Unable to find [jdbc]*/

                PrivilegedCarbonContext.getThreadLocalCarbonContext().
                        setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(MultitenantConstants.SUPER_TENANT_ID);

                String adminName = ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId())
                        .getRealmConfiguration().getAdminUserName();
                ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
                Registry registry = ServiceHolder.getRegistryService().getGovernanceUserRegistry(adminName, tenant
                        .getId());
                GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
                //GenericArtifactManager manager = new GenericArtifactManager(registry, "api");
                GovernanceUtils.loadGovernanceArtifacts((UserRegistry) registry);
                GenericArtifact[] artifacts = artifactManager.getAllGenericArtifacts();
                for (GenericArtifact artifact : artifacts) {
                    API api = APIUtil.getAPI(artifact, registry);

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

            }
            if (isTenantFlowStarted) {
                PrivilegedCarbonContext.endTenantFlow();
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
        if (log.isDebugEnabled()) {
            log.debug("Rxt resource migration done for all the tenants");
        }
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
                PrivilegedCarbonContext.getThreadLocalCarbonContext()
                        .setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, true);
                PrivilegedCarbonContext.getThreadLocalCarbonContext()
                        .setTenantId(MultitenantConstants.SUPER_TENANT_ID);

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
        log.info("Swagger migration for API Manager 1.9.0 started.");
        boolean isTenantFlowStarted = false;
        try {
            for (Tenant tenant : tenantsArray) {
                if (log.isDebugEnabled()) {
                    log.debug("Swagger migration for tenant " + tenant.getDomain() + "[" + tenant.getId() + "]" + " ");
                }

                PrivilegedCarbonContext.startTenantFlow();
                isTenantFlowStarted = true;

                //Use the super tenant instead of tenant because tenants do not have access to master-datasources.xml
                PrivilegedCarbonContext.getThreadLocalCarbonContext().
                        setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(MultitenantConstants.SUPER_TENANT_ID);

                String adminName = ServiceHolder.getRealmService().getTenantUserRealm(
                        tenant.getId()).getRealmConfiguration().getAdminUserName();
                ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
                Registry registry = ServiceHolder.getRegistryService().
                        getGovernanceUserRegistry(adminName, tenant.getId());
                GenericArtifactManager manager = new GenericArtifactManager(registry, Constants.API);
                GovernanceUtils.loadGovernanceArtifacts((UserRegistry) registry);
                GenericArtifact[] artifacts = manager.getAllGenericArtifacts();

                for (GenericArtifact artifact : artifacts) {
                    API api = APIUtil.getAPI(artifact, registry);
                    APIIdentifier apiIdentifier = api.getId();
                    String apiName = apiIdentifier.getApiName();
                    String apiVersion = apiIdentifier.getVersion();
                    String apiProviderName = apiIdentifier.getProviderName();

                    String swagger12location = ResourceUtil.getSwagger12ResourceLocation(apiName,
                            apiVersion, apiProviderName);

                    if (!registry.resourceExists(swagger12location)) {
                        log.error("Swagger Resource migration has not happen yet for " +
                                apiName + "-" + apiVersion + "-"
                                + apiProviderName);

                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Creating swagger v2.0 resource for : " + apiName + "-" + apiVersion + "-"
                                    + apiProviderName);
                        }
                        //get swagger v2 doc
                        String swagger2doc = getSwagger2docUsingSwagger12RegistryResources(registry, swagger12location);

                        //create location in registry and add this
                        String swagger2location = ResourceUtil.getSwagger2ResourceLocation(apiName, apiVersion,
                                apiProviderName);

                        Resource docContent = registry.newResource();
                        docContent.setContent(swagger2doc);
                        docContent.setMediaType("application/json");
                        registry.put(swagger2location, docContent);

                        //Currently set to ANONYMOUS_ROLE, need to set to visible roles
                        ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).getAuthorizationManager()
                                .authorizeRole(APIConstants.ANONYMOUS_ROLE,
                                        "_system/governance" + swagger2location, ActionConstants.GET);
                    }
                }

                PrivilegedCarbonContext.endTenantFlow();
            }
        } catch (MalformedURLException e) {
            ResourceUtil.handleException("Error occurred while creating swagger v2.0 document ", e);
        } catch (APIManagementException e) {
            ResourceUtil.handleException("Error occurred while reading API from the artifact ", e);
        } catch (RegistryException e) {
            ResourceUtil.handleException("Error occurred while accessing the registry", e);
        } catch (ParseException e) {
            ResourceUtil.handleException("Error occurred while getting swagger v2.0 document", e);
        } catch (UserStoreException e) {
            ResourceUtil.handleException("Error occurred while reading tenant information", e);
        }
        if (isTenantFlowStarted) {
            PrivilegedCarbonContext.endTenantFlow();
        }

        if (log.isDebugEnabled()) {
            log.debug("Swagger resource migration done for all the tenants");
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

    private String getSwagger2docUsingSwagger12RegistryResources(Registry registry, String swagger12location)
            throws MalformedURLException, ParseException, RegistryException {

        JSONParser parser = new JSONParser();
        String swagger12BasePath = null;

        Resource swaggerRes = registry.get(swagger12location + APIConstants.API_DOC_1_2_RESOURCE_NAME);
        JSONObject swagger12doc = (JSONObject) parser.parse(new String((byte[]) swaggerRes.getContent()));

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
            JSONObject apiDef =
                    (JSONObject) parser.parse(new String((byte[]) resource.getContent()));
            //get the base path. this is same for all api definitions.
            swagger12BasePath = (String) apiDef.get("basePath");
            JSONArray apiArray = (JSONArray) apiDef.get("apis");
            for (Object anApiArray : apiArray) {
                JSONObject api = (JSONObject) anApiArray;
                String path = (String) api.get("path");
                JSONArray operations = (JSONArray) api.get("operations");
                //set the operations object inside each api definition and set it in a map against its resource path
                apiDefPaths.put(path, operations);
            }
        }
        JSONObject swagger2Doc = generateSwagger2Document(swagger12doc, apiDefPaths, swagger12BasePath);
        return swagger2Doc.toJSONString();
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
                                                       Map<String, JSONArray> apiDefPaths, String swagger12BasePath)
            throws ParseException, MalformedURLException {
        //create swagger 2.0 doc
        JSONObject swagger20doc = new JSONObject();

        //set swagger version
        swagger20doc.put(Constants.SWAGGER, Constants.SWAGGER_V2);

        //set the info object
        JSONObject info = generateInfoObject(swagger12doc);
        //update info object
        swagger20doc.put(Constants.SWAGGER_INFO, info);

        //set the paths object
        JSONObject pathObj = generatePathsObj(apiDefPaths);
        swagger20doc.put(Constants.SWAGGER_PATHS, pathObj);

        URL url = new URL(swagger12BasePath);
        swagger20doc.put(Constants.SWAGGER_HOST, url.getHost());
        swagger20doc.put(Constants.SWAGGER_BASE_PATH, url.getPath());

        JSONArray schemes = new JSONArray();
        schemes.add(url.getProtocol());
        swagger20doc.put(Constants.SWAGGER_SCHEMES, schemes);

        //securityDefinitions
        if (swagger12doc.containsKey(Constants.SWAGGER_AUTHORIZATIONS)) {
            JSONObject securityDefinitions = generateSecurityDefinitionsObject(swagger12doc);
            swagger20doc.put(Constants.SWAGGER_SECURITY_DEFINITIONS, securityDefinitions);
        }

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
        JSONParser parser = new JSONParser();
        JSONObject securityDefinitionObject = new JSONObject();
        JSONObject securitySchemeObject = (JSONObject) parser.parse(Constants.DEFAULT_SECURITY_SCHEME);

        JSONObject authorizations = (JSONObject) swagger12doc.get(Constants.SWAGGER_AUTHORIZATIONS);
        Set authTypes = authorizations.keySet();

        for (Object obj : authTypes) {
            JSONObject authObj = (JSONObject) authorizations.get(obj.toString());
            if (authObj.containsKey(Constants.SWAGGER_SCOPES)) {
                //Put it to custom WSO2 scopes
                securitySchemeObject.put(Constants.SWAGGER_X_WSO2_SCOPES, authObj.get(Constants.SWAGGER_SCOPES));
            }
            securityDefinitionObject.put(obj.toString(), securitySchemeObject);
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
    private static JSONObject generateInfoObject(JSONObject swagger12doc) throws ParseException {

        JSONObject infoObj = (JSONObject) swagger12doc.get("info");
        JSONParser parser = new JSONParser();
        JSONObject swagger2InfoObj = (JSONObject) parser.parse(Constants.DEFAULT_INFO);

        //set the required parameters first
        String title = (String) infoObj.get("title");
        String version = (String) swagger12doc.get("apiVersion");

        swagger2InfoObj.put(Constants.SWAGGER_TITLE, title);
        swagger2InfoObj.put(Constants.SWAGGER_VER, version);

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
        JSONParser jsonParser = new JSONParser();

        for (Map.Entry<String, JSONArray> entry : apiDefinitionPaths.entrySet()) {
            String key = entry.getKey();
            JSONArray operations = entry.getValue();
            JSONObject pathItemObj = new JSONObject();
            for (Object operation : operations) {
                JSONObject operationObject = (JSONObject) operation;
                String method = (String) operationObject.get("method");
                JSONArray swagger2ParamObjects = (JSONArray) operationObject.get("parameters");
                JSONObject swagger2OperationsObj = new JSONObject();
                JSONArray newParameters = new JSONArray();
                for (Object swagger2ParamObj : swagger2ParamObjects) {
                    JSONObject oldParam = (JSONObject) swagger2ParamObj;
                    JSONObject paramObj = new JSONObject();
                    paramObj.put(Constants.SWAGGER_NAME, oldParam.get(Constants.SWAGGER_NAME));
                    paramObj.put(Constants.SWAGGER_PARAM_TYPE_IN, oldParam.get("paramType"));
                    paramObj.put(Constants.SWAGGER_REQUIRED_PARAM, oldParam.get(Constants.SWAGGER_REQUIRED_PARAM));
                    if (paramObj.containsKey(Constants.SWAGGER_DESCRIPTION)) {
                        paramObj.put(Constants.SWAGGER_DESCRIPTION, oldParam.get(Constants.SWAGGER_DESCRIPTION));
                    } else {
                        paramObj.put(Constants.SWAGGER_DESCRIPTION, "");
                    }
                    //Skip body parameter of GET and DELETE methods
                    /*if (!("GET".equals(method)) && !("DELETE".equals(method))) {
                        newParameters.add(paramObj);
                    } else {
                        if (!("body".equals(oldParam.get("paramType")))) {
                            newParameters.add(paramObj);
                        }
                    }*/
                }

                //generate the Operation object
                // (https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#operationObject)
                swagger2OperationsObj.put(Constants.SWAGGER_OPERATION_ID, operationObject.get("nickname"));
                //setting operation level params
                swagger2OperationsObj.put(Constants.SWAGGER_PARAMETERS, newParameters);
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
                JSONObject responseObject = null;
                if (operationObject.containsKey("responseMessages")) {
                    responseObject = new JSONObject();
                    JSONArray responseMessages = (JSONArray) operationObject.get("responseMessages");
                    for (Object responseMessage : responseMessages) {
                        JSONObject errorObj = (JSONObject) responseMessage;
                        responseObject.put(errorObj.get("code"), errorObj.get("message"));
                    }
                }
                if (responseObject == null) {
                    //set a default response message since this is required field
                    responseObject = (JSONObject) jsonParser.parse(Constants.DEFAULT_RESPONSE);
                }
                swagger2OperationsObj.put(Constants.SWAGGER_RESPONSES, responseObject);
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
        log.info("Resource cleanup started for API Manager 1.9.0");
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
    void sequenceMigration() throws APIMigrationException {
        String repository = CarbonUtils.getCarbonRepository();
        String TenantRepo = CarbonUtils.getCarbonTenantsDirPath();
        for (Tenant tenant : tenantsArray) {
            String SequenceFilePath;
            if (tenant.getId() != MultitenantConstants.SUPER_TENANT_ID) {
                SequenceFilePath = TenantRepo + "/" + tenant.getId() +
                        "/synapse-configs/default/sequences/";
            } else {
                SequenceFilePath = repository + "synapse-configs/default/sequences/";
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
                ResourceUtil.handleException("Error occurred while reading file to copy.", e);
            }
        }
    }


    /**
     * This method is used to migrate synapse files
     * This changes the synapse api and add the new handlers
     *
     * @throws APIMigrationException
     */

    void synapseAPIMigration() throws APIMigrationException {
        String repository = CarbonUtils.getCarbonRepository();
        String tenantRepository = CarbonUtils.getCarbonTenantsDirPath();
        for (Tenant tenant : tenantsArray) {

            String SequenceFilePath;
            if (tenant.getId() != MultitenantConstants.SUPER_TENANT_ID) {
                SequenceFilePath = tenantRepository + "/" + tenant.getId() +
                        "/synapse-configs/default/api";
            } else {
                SequenceFilePath = repository + "synapse-configs/default/api";
            }
            File APIFiles = new File(SequenceFilePath);
            File[] synapseFiles = APIFiles.listFiles();
            for (File synapseFile : synapseFiles) {
                if (tenant.getId() == MultitenantConstants.SUPER_TENANT_ID) {
                    if (synapseFile.getName().matches("[\\w+][--][\\w+][__v]")) {
                        ResourceUtil.updateSynapseAPI(synapseFile, "ENDPOINT");
                    }
                } else {
                    if (synapseFile.getName().matches("[\\w+][-AT-]" + tenant.getDomain() + "[--][\\w+][--v]]")) {
                        ResourceUtil.updateSynapseAPI(synapseFile, "ENDPOINT");
                    }
                }

            }
        }
    }
}
