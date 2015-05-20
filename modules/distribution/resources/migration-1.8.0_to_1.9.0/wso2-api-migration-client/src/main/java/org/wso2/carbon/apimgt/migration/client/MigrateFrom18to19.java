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
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
    public void databaseMigration(String migrateVersion) throws APIMigrationException, SQLException {
        log.info("Database migration for API Manager 1.8.0 started");
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            String queryToExecute = ResourceUtil.pickQueryFromResources(migrateVersion, Constants.ALTER).trim();
            connection = APIMgtDBUtil.getConnection();

            String queryArray[] = queryToExecute.split(Constants.LINE_BREAK);

            connection.setAutoCommit(false);

            for (String query : queryArray) {
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.execute();
                connection.commit();
                preparedStatement.close();
            }

            String dbType = MigrationDBCreator.getDatabaseType(connection);
            dropFKConstraint(migrateVersion, dbType);

            if (log.isDebugEnabled()) {
                log.debug("Query " + queryToExecute + " executed ");
            }

        } catch (IOException e) {
            //ResourceUtil.handleException("Error occurred while finding the query. Please check the file path.", e);
            log.error("Error occurred while migrating databases", e);
        } catch (Exception e) {
            //ResourceUtil.handleException("Error occurred while finding the query. Please check the file path.", e);
            log.error("Error occurred while migrating databases", e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
        log.info("DB resource migration done for all the tenants");
    }

    private void dropFKConstraint(String migrateVersion, String dbType) throws SQLException, IOException, APIMigrationException {
        String constraintName = null;
        Connection connection;
        String queryToExecute = ResourceUtil.pickQueryFromResources(migrateVersion, Constants.CONSTRAINT).trim();
        String queryArray[] = queryToExecute.split(Constants.LINE_BREAK);

        connection = APIMgtDBUtil.getConnection();
        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(queryArray[0]);
        while (resultSet.next()) {
            constraintName = resultSet.getString("constraint_name");
        }

        if (constraintName != null) {
            queryToExecute = queryArray[1].replace("<temp_key_name>", constraintName);
            PreparedStatement preparedStatement = connection.prepareStatement(queryToExecute);
            preparedStatement.execute();
            connection.commit();
            preparedStatement.close();
        }
        connection.close();
    }

    /**
     * This method is used to migrate all registry resources
     * This migrates swagger resources and rxts
     *
     * @throws APIMigrationException
     */
    @Override
    public void registryResourceMigration() throws APIMigrationException {
        swaggerResourceMigration();
        rxtMigration();
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
     * This method is used to migrate swagger v1.2 resources to swagger v2.0 resource
     * This reads the swagger v1.2 doc from the registry and creates swagger v2.0 doc
     *
     * @throws APIMigrationException
     */
    void swaggerResourceMigration() throws APIMigrationException {
        log.info("Swagger migration for API Manager 1.9.0 started.");
        try {
            for (Tenant tenant : tenantsArray) {
                if (log.isDebugEnabled()) {
                    log.debug("Swagger migration for tenant " + tenant.getDomain() + "[" + tenant.getId() + "]" + " ");
                }

                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain());
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenant.getId());

                String adminName = ServiceHolder.getRealmService().getTenantUserRealm(
                        tenant.getId()).getRealmConfiguration().getAdminUserName();
                ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
                Registry registry = ServiceHolder.getRegistryService().
                        getGovernanceUserRegistry(adminName, tenant.getId());
                GenericArtifactManager manager = new GenericArtifactManager(registry, "api");
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
        } finally {
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

    //@todo: check swagger v2 spec and add mandatory ones
    private static JSONObject generateSwagger2Document(JSONObject swagger12doc,
                                                       Map<String, JSONArray> apiDefPaths, String swagger12BasePath)
            throws ParseException, MalformedURLException {
        //create swagger 2.0 doc
        JSONObject swagger20doc = new JSONObject();

        //set swagger version
        swagger20doc.put("swagger", "2.0");

        //set the info object
        JSONObject info = generateInfoObject(swagger12doc);
        //update info object
        swagger20doc.put("info", info);

        //set the paths object
        JSONObject pathObj = generatePathsObj(apiDefPaths);
        swagger20doc.put("paths", pathObj);

        URL url = new URL(swagger12BasePath);
        swagger20doc.put("host", url.getHost());
        swagger20doc.put("basePath", url.getPath());

        JSONArray schemes = new JSONArray();
        schemes.add(url.getProtocol());
        swagger20doc.put("schemes", schemes);

        //securityDefinitions
        if (swagger12doc.containsKey("authorizations")) {
            JSONObject securityDefinitions = generateSecurityDefinitionsObject(swagger12doc);
            swagger20doc.put("securityDefinitions", securityDefinitions);
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

        JSONObject authorizations = (JSONObject) swagger12doc.get("authorizations");
        Set authTypes = authorizations.keySet();

        for (Object obj : authTypes) {
            JSONObject authObj = (JSONObject) authorizations.get(obj.toString());
            if (authObj.containsKey("scopes")) {
                //Put it to custom WSO2 scopes
                securitySchemeObject.put("x-wso2-scopes", authObj.get("scopes"));
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

        swagger2InfoObj.put("title", title);
        swagger2InfoObj.put("version", version);

        if (infoObj.containsKey("description")) {
            swagger2InfoObj.put("description", infoObj.get("description"));
        }
        if (infoObj.containsKey("termsOfServiceUrl")) {
            swagger2InfoObj.put("termsOfService", infoObj.get("termsOfServiceUrl"));
        }

        //contact object
        if (infoObj.containsKey("contact")) {
            JSONObject contactsObj = new JSONObject();
            String contact = (String) infoObj.get("contact");
            if (contact.contains("http")) {
                contactsObj.put("url", contact);
            } else if (contact.contains("@")) {
                contactsObj.put("email", contact);
            } else {
                contactsObj.put("name", contact);
            }
            swagger2InfoObj.put("contact", contactsObj);
        }

        //licence object
        JSONObject licenseObj = new JSONObject();
        if (infoObj.containsKey("license")) {
            licenseObj.put("name", infoObj.get("license"));
        }
        if (infoObj.containsKey("licenseUrl")) {
            licenseObj.put("url", infoObj.get("licenseUrl"));
        }
        if (!licenseObj.isEmpty()) {
            swagger2InfoObj.put("license", licenseObj);
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
                    paramObj.put("name", oldParam.get("name"));
                    paramObj.put("in", oldParam.get("paramType"));
                    paramObj.put("required", oldParam.get("required"));
                    if (paramObj.containsKey("description")) {
                        paramObj.put("description", oldParam.get("description"));
                    } else {
                        paramObj.put("description", "");
                    }
                    newParameters.add(paramObj);
                }

                //generate the Operation object
                // (https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#operationObject)
                swagger2OperationsObj.put("operationId", operationObject.get("nickname"));
                //setting operation level params
                swagger2OperationsObj.put("parameters", newParameters);
                if (operationObject.containsKey("notes")) {
                    swagger2OperationsObj.put("description", operationObject.get("notes"));
                }
                if (operationObject.containsKey("summary")) {
                    swagger2OperationsObj.put("summary", operationObject.get("summary"));
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
                swagger2OperationsObj.put("responses", responseObject);
            }
            pathsObj.put(key, pathItemObj);
        }
        return pathsObj;
    }


    /**
     * This method is used to migrate rxt
     * This adds three new attributes to the api rxt
     *
     * @throws APIMigrationException
     */
    //@todo : change the default api.rxt as well
    void rxtMigration() throws APIMigrationException {
        log.info("Rxt migration for API Manager 1.9.0 started.");
        try {
            for (Tenant tenant : tenantsArray) {
                String adminName = ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId())
                        .getRealmConfiguration().getAdminUserName();
                ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
                Registry registry = ServiceHolder.getRegistryService().getGovernanceUserRegistry(adminName, tenant
                        .getId());
                GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
                GenericArtifactManager manager = new GenericArtifactManager(registry, "api");
                GovernanceUtils.loadGovernanceArtifacts((UserRegistry) registry);
                GenericArtifact[] artifacts = manager.getAllGenericArtifacts();
                for (GenericArtifact artifact : artifacts) {
                    API api = APIUtil.getAPI(artifact, registry);
                    artifact.addAttribute("overview_contextTemplate", api.getContext() + "/{version}");
                    artifact.addAttribute("overview_environments", "");//@todo : assume published to all environments
                    artifact.addAttribute("overview_versionType", "");

                    artifactManager.updateGenericArtifact(artifact);
                }
            }
        } catch (APIManagementException e) {
            ResourceUtil.handleException("Error occurred while reading API from the artifact ", e);
        } catch (RegistryException e) {
            ResourceUtil.handleException("Error occurred while accessing the registry", e);
        } catch (UserStoreException e) {
            ResourceUtil.handleException("Error occurred while reading tenant information", e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Rxt resource migration done for all the tenants");
        }
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
     * This method is used to migrate sequence files
     * This adds cors_request_handler to sequences
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
                                "/18to19Migration/sequence-scripts/_cors_request_handler.xml"),
                        new File(SequenceFilePath + "_cors_request_handler.xml"));
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

    //@todo : read from fs
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
            for (File synapseFile : synapseFiles){
                if (tenant.getId() ==  MultitenantConstants.SUPER_TENANT_ID){
                    if (synapseFile.getName().matches("[\\w+][--][\\w+][__v]")){
                    ResourceUtil.updateSynapseAPI(synapseFile,"ENDPOINT");
                    }
                }else{
                    if (synapseFile.getName().matches("[\\w+][-AT-]"+tenant.getDomain()+"[--][\\w+]")){
                        ResourceUtil.updateSynapseAPI(synapseFile,"ENDPOINT");
                    }
                }

            }

        }
    }
}
