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
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.client._110Specific.dto.SwaggerInfoDTO;
import org.wso2.carbon.apimgt.migration.client.internal.ServiceHolder;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.RegistryService;
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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This class contains all the methods which is used to migrate APIs from APIManager 1.8.0 to APIManager 1.9.0.
 * The migration performs in database, registry and file system
 */

@SuppressWarnings("unchecked")
public class MigrateFrom18to19 extends MigrationClientBase implements MigrationClient {

    private static final Log log = LogFactory.getLog(MigrateFrom18to19.class);
    private RegistryService registryService;

    public MigrateFrom18to19(String tenantArguments, String blackListTenantArguments, RegistryService registryService,
                             TenantManager tenantManager) throws UserStoreException {
        super(tenantArguments, blackListTenantArguments, tenantManager);
        this.registryService = registryService;
    }

    /**
     * This method is used to migrate database tables
     * This executes the database queries according to the user's db type and alters the tables
     *
     * @throws SQLException
     */
    @Override
    public void databaseMigration() throws SQLException {
        final String productHome = CarbonUtils.getCarbonHome();
        String scriptPath = productHome + File.separator + "migration-scripts" + File.separator + "18-19-migration" +
                            File.separator;
        updateAPIManagerDatabase(scriptPath);

        String constraintsScriptPath = productHome + File.separator + "migration-scripts" + File.separator +
                                       "18-19-migration" + File.separator;

        //To drop the foreign key
        dropFKConstraint(constraintsScriptPath);
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
        externalStoreMigration();
    }

    private void externalStoreMigration() throws APIMigrationException {
        log.info("External API store migration for API Manager " + Constants.VERSION_1_9 + " started.");
        for (Tenant tenant : getTenantsArray()) {
            if (log.isDebugEnabled()) {
                log.debug("Start API store migration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            }

            try {
                registryService.startTenantFlow(tenant);
                if (!registryService.isGovernanceRegistryResourceExists(APIConstants.EXTERNAL_API_STORES_LOCATION)) {
                    continue;
                }

                //Resource externalStoreResource = registry.get(APIConstants.EXTERNAL_API_STORES_LOCATION);
                String externalStoreResource = ResourceUtil.getResourceContent(
                        registryService.getGovernanceRegistryResource(APIConstants.EXTERNAL_API_STORES_LOCATION));

                String modifiedConfig = modifyExternalStores(externalStoreResource);
                registryService.updateGovernanceRegistryResource(APIConstants.EXTERNAL_API_STORES_LOCATION,
                                                                 modifiedConfig);

            } catch (RegistryException e) {
                log.error("Error occurred while accessing the registry", e);
                try {
                    registryService.rollbackGovernanceRegistryTransaction();
                } catch (org.wso2.carbon.registry.core.exceptions.RegistryException ex) {
                    log.error("Error occurred while accessing the registry", ex);
                } catch (UserStoreException ex) {
                    log.error("Error occurred while reading tenant information", ex);
                }
            } catch (UserStoreException e) {
                log.error("Error occurred while reading tenant information", e);
            } finally {
                registryService.endTenantFlow();
            }
            if (log.isDebugEnabled()) {
                log.debug("End API store migration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            }
        }
    }

    private String modifyExternalStores(String xmlContent) throws APIMigrationException {
        Writer stringWriter = new StringWriter();
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new InputSource(new ByteArrayInputStream(xmlContent.getBytes("UTF8"))));

            NodeList externalAPIStores = doc.getElementsByTagName(Constants.EXTERNAL_API_STORE);

            for (int i = 0; i < externalAPIStores.getLength(); i++) {
                Element externalStoreElement = (Element) externalAPIStores.item(i);
                NamedNodeMap attributes = externalStoreElement.getAttributes();
                boolean isAttributeExists = false;

                for (int j = 0; j < attributes.getLength(); j++) {
                    if (Constants.ATTRIBUTE_CLASSNAME.equals(attributes.item(j).getNodeName())) {
                        isAttributeExists = true;
                        break;
                    }
                }
                if (!isAttributeExists) {
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
            ResourceUtil.handleException("Error occurred while reading the xml document. " + "Please check for " +
                                         "external API config file in the registry", e);
        } catch (ParserConfigurationException e) {
            ResourceUtil.handleException("Error occurred while trying to build the xml document", e);
        } catch (TransformerException e) {
            ResourceUtil.handleException("Error occurred while saving modified the xml document", e);
        }

        return stringWriter.toString();
    }


    /**
     * This method is used to migrate rxt
     * This adds three new attributes to the api rxt
     *
     * @throws APIMigrationException
     */
    private void rxtMigration() throws APIMigrationException {
        log.info("Rxt migration for API Manager " + Constants.VERSION_1_9 + " started.");

        boolean isTenantFlowStarted = false;
        for (Tenant tenant : getTenantsArray()) {
            if (log.isDebugEnabled()) {
                log.debug("Start rxtMigration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            }
            try {
                PrivilegedCarbonContext.startTenantFlow();
                isTenantFlowStarted = true;

                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenant.getId(), true);

                String adminName = ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId())
                        .getRealmConfiguration().getAdminUserName();

                if (log.isDebugEnabled()) {
                    log.debug("Tenant admin username : " + adminName);
                }

                ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
                Registry registry = ServiceHolder.getRegistryService().getGovernanceUserRegistry(adminName, tenant
                        .getId());
                GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);

                if (artifactManager != null) {
                    GovernanceUtils.loadGovernanceArtifacts((UserRegistry) registry);
                    GenericArtifact[] artifacts = artifactManager.getAllGenericArtifacts();
                    for (GenericArtifact artifact : artifacts) {
                        try {
                            API api = APIUtil.getAPI(artifact, registry);

                            if (api == null) {
                                log.error("Cannot find corresponding api for registry artifact " +
                                          artifact.getAttribute("overview_name") + '-' +
                                          artifact.getAttribute("overview_version") + '-' +
                                          artifact.getAttribute("overview_provider") +
                                          " of tenant " + tenant.getId() + '(' + tenant.getDomain() + ") in AM_DB");
                                continue;
                            }

                            if (log.isDebugEnabled()) {
                                log.debug("Doing the RXT migration for API : " +
                                          artifact.getAttribute("overview_name") + '-' +
                                          artifact.getAttribute("overview_version") + '-' +
                                          artifact.getAttribute("overview_provider") +
                                          " of tenant " + tenant.getId() + '(' + tenant.getDomain() + ")");
                            }

                            APIIdentifier apiIdentifier = api.getId();
                            String apiVersion = apiIdentifier.getVersion();

                            if (!(api.getContext().endsWith(RegistryConstants.PATH_SEPARATOR + apiVersion))) {
                                artifact.setAttribute("overview_context", api.getContext() +
                                                                          RegistryConstants.PATH_SEPARATOR +
                                                                          apiVersion);
                            }

                            artifact.setAttribute("overview_contextTemplate", api.getContext() +
                                                                              RegistryConstants.PATH_SEPARATOR +
                                                                              "{version}");
                            artifact.setAttribute("overview_environments", "");
                            artifact.setAttribute("overview_versionType", "context");
                            artifact.setAttribute("overview_endpointURL", null);

                            artifactManager.updateGenericArtifact(artifact);
                        } catch (Exception e) {
                            // we log the error and continue to the next resource.
                            log.error("Unable to migrate api metadata definition of API : " +
                                      artifact.getAttribute("overview_name") + '-' +
                                      artifact.getAttribute("overview_version") + '-' +
                                      artifact.getAttribute("overview_provider"), e);
                        }
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("No api artifacts found in registry for tenant " + tenant.getId() + '(' + tenant
                                .getDomain() + ')');
                    }
                }
            } catch (APIManagementException e) {
                log.error("Error occurred while reading API from the artifact ", e);
            } catch (RegistryException e) {
                log.error("Error occurred while accessing the registry", e);
            } catch (UserStoreException e) {
                log.error("Error occurred while reading tenant information", e);
            } finally {
                if (isTenantFlowStarted) {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("End rxtMigration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            }
        }
        log.info("Rxt resource migration done for all the tenants");
    }


    //TODO: cehck and remove
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

            for (Tenant tenant : getTenantsArray()) {
                try {
                    int tenantId = tenant.getId();

                    PrivilegedCarbonContext.startTenantFlow();
                    isTenantFlowStarted = true;
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenant.getId(), true);

                    String adminName = ServiceHolder.getRealmService().getTenantUserRealm(tenantId)
                            .getRealmConfiguration().getAdminUserName();
                    ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenantId);
                    Registry registry = ServiceHolder.getRegistryService().getGovernanceUserRegistry(adminName,
                                                                                                     tenantId);
                    GovernanceUtils.loadGovernanceArtifacts((UserRegistry) registry);

                    Resource resource;
                    if (!registry.resourceExists(resourcePath)) {
                        resource = registry.newResource();
                    } else {
                        resource = registry.get(resourcePath);
                    }
                    resource.setContent(rxtContent);
                    resource.setMediaType("application/xml");
                    // /_system/governance/repository/components/org.wso2.carbon.governance/types/api.rxt
                    registry.put(resourcePath, resource);

                    ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).getAuthorizationManager()
                            .authorizeRole(APIConstants.ANONYMOUS_ROLE, resourcePath, ActionConstants.GET);
                } catch (UserStoreException e) {
                    log.error("Error occurred while searching for tenant admin. ", e);
                } catch (RegistryException e) {
                    log.error("Error occurred while performing registry operation. ", e);
                } catch (Exception e) {
                    log.error("Unable to copy the new RXT for tenant : " + tenant.getDomain(), e);
                } finally {
                    if (isTenantFlowStarted) {
                        PrivilegedCarbonContext.endTenantFlow();
                    }
                }
            }
        } catch (IOException e) {
            ResourceUtil.handleException("Error occurred while reading the rxt file from file system.  ", e);
        }
    }


    /**
     * This method is used to migrate swagger v1.2 resources to swagger v2.0 resource
     * This reads the swagger v1.2 doc from the registry and creates swagger v2.0 doc
     *
     * @throws APIMigrationException
     */
    private void swaggerResourceMigration() throws APIMigrationException {
        log.info("Swagger migration for API Manager " + Constants.VERSION_1_9 + " started.");

        for (Tenant tenant : getTenantsArray()) {
            if (log.isDebugEnabled()) {
                log.debug("Start swaggerResourceMigration for tenant " + tenant.getId()
                          + '(' + tenant.getDomain() + ')');
            }

            try {
                registryService.startTenantFlow(tenant);

                GenericArtifact[] artifacts = registryService.getGenericAPIArtifacts();
                updateSwaggerResources(artifacts, tenant);
            } catch (Exception e) {
                // If any exception happen during a tenant data migration, we continue the other tenants
                log.error("Unable to migrate the swagger resources of tenant : " + tenant.getDomain());
            } finally {
                registryService.endTenantFlow();
            }

            if (log.isDebugEnabled()) {
                log.debug("End swaggerResourceMigration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            }
        }

        log.info("Swagger resource migration done for all the tenants.");
    }

    private void updateSwaggerResources(GenericArtifact[] artifacts, Tenant tenant) throws APIMigrationException {
        log.debug("Calling updateSwaggerResources");

        APIDefinitionFromSwagger20 definitionFromSwagger20 = new APIDefinitionFromSwagger20();
        for (GenericArtifact artifact : artifacts) {
            API api = registryService.getAPI(artifact);

            if (api != null) {
                APIIdentifier apiIdentifier = api.getId();
                String apiName = apiIdentifier.getApiName();
                String apiVersion = apiIdentifier.getVersion();
                String apiProviderName = apiIdentifier.getProviderName();
                try {
                    String swagger2Document;

                    String swagger2location = ResourceUtil.getSwagger2ResourceLocation(apiName, apiVersion,
                                                                                       apiProviderName);
                    String swagger12location = ResourceUtil.getSwagger12ResourceLocation(apiName,
                                                                                         apiVersion,
                                                                                         apiProviderName);

                    if (!registryService.isGovernanceRegistryResourceExists(swagger12location)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Creating swagger v2.0 resource from scratch for : " + apiName + '-' + apiVersion +
                                      '-' + apiProviderName);
                        }
                        swagger2Document = definitionFromSwagger20.generateAPIDefinition(api);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Creating swagger v2.0 resource using v1.2 for : " + apiName + '-' + apiVersion +
                                      '-' + apiProviderName);
                        }
                        swagger2Document = getSwagger2docUsingSwagger12RegistryResources(tenant, swagger12location,
                                                                                         api);
                    }

                    registryService.addGovernanceRegistryResource(swagger2location, swagger2Document,
                                                                  "application/json");
                    registryService.setGovernanceRegistryResourcePermissions(apiProviderName, null, null,
                                                                             swagger2location);
                } catch (RegistryException e) {
                    log.error("Registry error encountered for api " + apiName + '-' + apiVersion + '-' +
                              apiProviderName + " of tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
                } catch (ParseException e) {
                    log.error("Error occurred while parsing swagger v1.2 document for api " + apiName + '-' +
                              apiVersion + '-' + apiProviderName + " of tenant " + tenant.getId() + '(' + tenant
                                      .getDomain() + ')', e);
                } catch (UserStoreException e) {
                    log.error("Error occurred while setting permissions of swagger v2.0 document for api " + apiName
                              + '-' + apiVersion + '-' + apiProviderName + " of tenant " + tenant.getId() + '(' +
                              tenant.getDomain() + ')', e);
                } catch (MalformedURLException e) {
                    log.error("Error occurred while creating swagger v2.0 document for api " + apiName + '-' +
                              apiVersion + '-' + apiProviderName + " of tenant " + tenant.getId() + '(' + tenant
                                      .getDomain() + ')', e);
                } catch (APIManagementException e) {
                    log.error("Error occurred while creating swagger v2.0 document for api " + apiName + '-' +
                              apiVersion + '-' + apiProviderName + " of tenant " + tenant.getId() + '(' + tenant
                                      .getDomain() + ')', e);
                } catch (Exception e) {
                    log.error("Error occurred while creating swagger doc for api " + apiName + '-' + apiVersion + '-'
                              + apiProviderName + " of tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
                }
            }
        }
    }

    /**
     * This method generates swagger v2 doc using swagger 1.2 doc
     *
     * @param tenant            Tenant
     * @param swagger12location the location of swagger 1.2 doc
     * @return JSON string of swagger v2 doc
     * @throws java.net.MalformedURLException
     * @throws org.json.simple.parser.ParseException
     * @throws org.wso2.carbon.registry.core.exceptions.RegistryException
     */
    private String getSwagger2docUsingSwagger12RegistryResources(Tenant tenant, String swagger12location, API api)
            throws MalformedURLException, ParseException, RegistryException, UserStoreException {
        log.debug("Calling getSwagger2docUsingSwagger12RegistryResources");

        JSONParser parser = new JSONParser();
        Map<String, SwaggerInfoDTO> apiDefPaths = new HashMap<String, SwaggerInfoDTO>();

        Object rawResource = registryService.getGovernanceRegistryResource(swagger12location + APIConstants
                .API_DOC_1_2_RESOURCE_NAME);
        String swaggerRes = ResourceUtil.getResourceContent(rawResource);

        JSONObject swagger12doc = (JSONObject) parser.parse(swaggerRes);
        JSONArray pathConfigs = (JSONArray) swagger12doc.get(APIConstants.API_ARRAY_NAME);

        for (Object pathConfig : pathConfigs) {
            JSONObject jsonObjPathConfig = (JSONObject) pathConfig;
            String pathName = (String) jsonObjPathConfig.get(APIConstants.DOCUMENTATION_SEARCH_PATH_FIELD);
            pathName = pathName.startsWith("/") ? pathName : ("/" + pathName);

            Object pathResource = registryService.getGovernanceRegistryResource(swagger12location + pathName);
            String swaggerDocContent = ResourceUtil.getResourceContent(pathResource);

            if (log.isDebugEnabled()) {
                log.debug("swaggerDocContent : " + swaggerDocContent);
            }

            JSONObject apiDef = (JSONObject) parser.parse(swaggerDocContent);
            generateAPIDefinitionPaths(apiDefPaths, apiDef, pathName);
        }
        JSONObject swagger2Doc = generateSwagger2Document(swagger12doc, apiDefPaths, api);
        return swagger2Doc.toJSONString();
    }

    /**
     * This method populates the map of path and corresponding {@link SwaggerInfoDTO} by extracting information from
     * the swagger 1.2 document
     *
     * @param apiDefPaths      The map to be populated
     * @param apiDef           The API definition swagger
     * @param resourcePathName The resource path of the swagger doc
     */
    private void generateAPIDefinitionPaths(Map<String, SwaggerInfoDTO> apiDefPaths, JSONObject apiDef,
                                            String resourcePathName) {
        if (apiDef.containsKey("apis")) {
            JSONArray apiArray = (JSONArray) apiDef.get("apis");

            JSONArray producesMimeArray = extractProducesObject(apiDef);
            JSONArray consumesMimeArray = extractConsumesObject(apiDef);
            for (Object anApiArray : apiArray) {
                JSONObject apiObject = (JSONObject) anApiArray;
                String path = (String) apiObject.get("path");
                JSONArray operations = (JSONArray) apiObject.get("operations");

                /*
                 * This is done to fix the following scenario.
                 * We have came across swagger 1.2 docs where paths are defined as follows.
                 * "apis": [
                    {
                      "path": "/invoices",
                      "operations": [
                        {
                          ...
                          "method": "GET",
                          "parameters": [
                            ...
                          ],
                        },
                        {
                          ...
                          "method": "OPTIONS",
                          "parameters": [
                            ...
                          ]
                        }
                      ]
                    },
                    {
                      "path": "/invoices",
                      "operations": [
                        {
                          ...
                          "method": "POST",
                          "parameters": [
                            ...
                          ]
                        }
                      ]
                    }
                  ],
                */
                if (apiDefPaths.containsKey(path)) {
                    SwaggerInfoDTO swaggerInfoDTO = apiDefPaths.get(path);
                    swaggerInfoDTO.getOperationList().add(operations);
                } else {
                    SwaggerInfoDTO swaggerInfoDTO = new SwaggerInfoDTO();

                    //Adding the produces and consumes elements as they are there for all the resources
                    swaggerInfoDTO.setProducesList(producesMimeArray);
                    swaggerInfoDTO.setConsumeList(consumesMimeArray);

                    // Setting the tag name as the resource path
                    if(resourcePathName.startsWith("/")){
                        resourcePathName = resourcePathName.substring(1);
                    }
                    swaggerInfoDTO.setTagName(resourcePathName);

                    swaggerInfoDTO.getOperationList().add(operations);
                    apiDefPaths.put(path, swaggerInfoDTO);
                }
            }
        } else {
            log.error("Cannot find resources in swagger v1.2 document");
        }
    }

    /**
     * Generate Swagger v2.0 document using Swagger v1.2 resources
     *
     * @param swagger12doc Old Swagger Document
     * @param apiDefPaths  Paths in API definition
     * @return Swagger v2.0 document as a JSON object
     * @throws ParseException
     * @throws MalformedURLException
     */

    private JSONObject generateSwagger2Document(JSONObject swagger12doc, Map<String, SwaggerInfoDTO> apiDefPaths,
                                                API api)
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
        JSONObject pathObj = generatePathsObj(swagger12doc, apiDefPaths);

        //JSONObject pathObj = generatePathsObj(api);
        swagger20doc.put(Constants.SWAGGER_PATHS, pathObj);

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
        if (swagger12doc.containsKey(Constants.SWAGGER_AUTHORIZATIONS)) {
            JSONObject authorizations = (JSONObject) swagger12doc.get(Constants.SWAGGER_AUTHORIZATIONS);
            Set authTypes = authorizations.keySet();
            for (Object obj : authTypes) {
                JSONObject authObj = (JSONObject) authorizations.get(obj.toString());
                if (authObj.containsKey(Constants.SWAGGER_SCOPES)) {
                    //Put it to custom WSO2 scopes
                    if (authObj.containsKey(Constants.SWAGGER_SCOPES)) {
                        securitySchemeObject.put(Constants.SWAGGER_X_WSO2_SCOPES,
                                                 authObj.get(Constants.SWAGGER_SCOPES));
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
                swagger2InfoObj.put(Constants.SWAGGER_TERMS_OF_SERVICE,
                                    infoObj.get(Constants.SWAGGER_TERMS_OF_SERVICE_URL));
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
    private static JSONObject generatePathsObj(JSONObject swagger12doc, Map<String, SwaggerInfoDTO> apiDefinitionPaths)
            throws ParseException {
        JSONObject pathsObj = new JSONObject();

        // Extract produces element
        JSONArray producesMimeArray = extractProducesObject(swagger12doc);

        // Extract consumes element
        JSONArray consumesMimeArray = extractConsumesObject(swagger12doc);

        for (Map.Entry<String, SwaggerInfoDTO> entry : apiDefinitionPaths.entrySet()) {
            try {
                String key = entry.getKey();
                List<JSONArray> operationsList = entry.getValue().getOperationList();

                JSONObject pathItemObj = new JSONObject();
                String tagName = entry.getValue().getTagName();

                for (JSONArray operations : operationsList) {
                    for (Object operation : operations) {
                        JSONObject operationObject = (JSONObject) operation;
                        if (operationObject.containsKey("method")) {
                            String method = (String) operationObject.get("method");
                            if (operationObject.containsKey("parameters")) {
                                JSONObject swagger2OperationsObj = new JSONObject();
                                JSONArray newParameters;

                                Object swagger12Params = operationObject.get("parameters");
                                if (swagger12Params != null && (swagger12Params instanceof JSONArray)) {
                                    // There is no parameters defined. We can proceed with the rest
                                    JSONArray swagger12ParamObjects = (JSONArray) swagger12Params;
                                    newParameters = new JSONArray();

                                    // Process the method parameters and move them to swagger 2.0 format
                                    generateSwagger2OperationParameters(newParameters, swagger12ParamObjects);

                                    //setting operation level params
                                    swagger2OperationsObj.put(Constants.SWAGGER_PARAMETERS, newParameters);
                                }

                                //generate the Operation object
                                // (https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0
                                // .md#operationObject)
                                swagger2OperationsObj.put(Constants.SWAGGER_OPERATION_ID,
                                                          operationObject.get("nickname"));

                                //Add auth_type and throttling_tier
                                if (operationObject.containsKey(Constants.SWAGGER_AUTH_TYPE)) {
                                    swagger2OperationsObj.put(Constants.SWAGGER_X_AUTH_TYPE,
                                                              operationObject.get(Constants.SWAGGER_AUTH_TYPE));
                                }
                                if (operationObject.containsKey(Constants.SWAGGER_THROTTLING_TIER)) {
                                    swagger2OperationsObj.put(Constants.SWAGGER_X_THROTTLING_TIER,
                                                              operationObject.get(Constants.SWAGGER_THROTTLING_TIER));
                                }

                                if (operationObject.containsKey("notes")) {
                                    swagger2OperationsObj.put(Constants.SWAGGER_DESCRIPTION,
                                                              operationObject.get("notes"));
                                }
                                if (operationObject.containsKey(Constants.SWAGGER_SUMMARY)) {
                                    swagger2OperationsObj.put(Constants.SWAGGER_SUMMARY,
                                                              operationObject.get(Constants.SWAGGER_SUMMARY));
                                }

                                // Add the produces mime types
                                if(entry.getValue().getProducesList() != null){
                                    swagger2OperationsObj.put(Constants.SWAGGER_PRODUCES,
                                                              entry.getValue().getProducesList());
                                }else if(producesMimeArray != null){
                                    swagger2OperationsObj.put(Constants.SWAGGER_PRODUCES, producesMimeArray);
                                }

                                // Add the consumes mime types
                                if(entry.getValue().getConsumeList() != null){
                                    swagger2OperationsObj.put(Constants.SWAGGER_CONSUMES,
                                                              entry.getValue().getConsumeList());
                                }else if(consumesMimeArray != null){
                                    swagger2OperationsObj.put(Constants.SWAGGER_CONSUMES, consumesMimeArray);
                                }

                                //set the responseObject
                                buildResponseObject(operationObject, swagger2OperationsObj);

                                // Setting the tags based on the resource path. Otherwise all the resources would be
                                // shown as "default"
                                if (tagName != null) {
                                    JSONArray tagsArray = new JSONArray();
                                    tagsArray.add(tagName);
                                    swagger2OperationsObj.put("tags", tagsArray);
                                }

                                //set pathItem object for the resource
                                //(https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0
                                // .md#pathItemObject)
                                pathItemObj.put(method.toLowerCase(), swagger2OperationsObj);
                            }
                        } else {
                            log.error("Needed parameter method does not exists in swagger v1.2 doc");
                        }
                    }
                }

                // Adding the constructed paths element
                pathsObj.put(key, pathItemObj);
            } catch (Exception e) {
                log.error("Error occurred while doing the swagger paths migration. key : " + entry.getKey() + " value" +
                          " : " + entry.getValue().toString(), e);
            }
        }
        return pathsObj;
    }

    private static void buildResponseObject(JSONObject operationObject, JSONObject swagger2OperationsObj){
        JSONObject responseObject = new JSONObject();

        if (operationObject.containsKey("responseMessages")) {
            JSONArray responseMessages = (JSONArray) operationObject.get("responseMessages");
            for (Object responseMessage : responseMessages) {
                JSONObject response = new JSONObject();
                JSONObject messageObject = (JSONObject) responseMessage;

                // If there is a message object, we set that as the description
                if (messageObject.containsKey("message")) {
                    response.put(Constants.SWAGGER_DESCRIPTION, messageObject.get("message"));
                }else {
                    response.put(Constants.SWAGGER_DESCRIPTION, "Description was not specified");
                }

                // If there is a responseModel defined, we add that as the schema object
                if(messageObject.containsKey("responseModel")){
                    JSONObject schemaObject = new JSONObject();
                    schemaObject.put(Constants.SWAGGER_PARAM_TYPE, messageObject.get("responseModel"));

                    response.put(Constants.SWAGGER_BODY_SCHEMA, schemaObject);
                }
                responseObject.put(messageObject.get("code"), response);
            }
            swagger2OperationsObj.put(Constants.SWAGGER_RESPONSES, responseObject);
        }

        // We check whether 200 status is already defined. If not we add the default one.
        if(!responseObject.containsKey(Constants.SWAGGER_RESPONSE_200)){
            JSONObject status200 = new JSONObject();
            status200.put(Constants.SWAGGER_DESCRIPTION, "No response was specified");

            if(operationObject.containsKey(Constants.SWAGGER_PARAM_TYPE)){
                JSONObject schemaObject = new JSONObject();
                schemaObject.put(Constants.SWAGGER_PARAM_TYPE, operationObject.get(Constants.SWAGGER_PARAM_TYPE));
                status200.put(Constants.SWAGGER_BODY_SCHEMA, schemaObject);
            }
            responseObject.put(Constants.SWAGGER_RESPONSE_200, status200);
            swagger2OperationsObj.put(Constants.SWAGGER_RESPONSES, responseObject);
        }
    }

    /**
     * This method generates the swagger 2.0 parameter object
     *
     * @param newParameters         The parameter object to be populated
     * @param swagger12ParamObjects The parameter definition of the swagger 1.2 document
     */
    private static void generateSwagger2OperationParameters(JSONArray newParameters,
                                                            JSONArray swagger12ParamObjects) {
        for (Object swagger12ParamObj : swagger12ParamObjects) {
            JSONObject oldParam = (JSONObject) swagger12ParamObj;
            JSONObject paramObj = new JSONObject();

            paramObj.put(Constants.SWAGGER_NAME, oldParam.get(Constants.SWAGGER_NAME));
            paramObj.put(Constants.SWAGGER_PARAM_TYPE_IN, oldParam.get("paramType"));

            if (Constants.SWAGGER_PARAM_TYPE_BODY.equals(oldParam.get("paramType"))) {
                JSONObject refObject = new JSONObject();
                // refObject.put(Constants.SWAGGER_REF,
                // "#/definitions/sampleItem");
                if (oldParam.containsKey("type")) {
                    refObject.put(Constants.SWAGGER_PARAM_TYPE, oldParam.get("type"));
                } else {
                    refObject.put(Constants.SWAGGER_PARAM_TYPE, Constants.DEFAULT_DATA_TYPE);
                }
                paramObj.put(Constants.SWAGGER_BODY_SCHEMA, refObject);
            } else {
                if (oldParam.containsKey("type")) {
                    paramObj.put(Constants.SWAGGER_PARAM_TYPE, oldParam.get("type"));
                } else {
                    paramObj.put(Constants.SWAGGER_PARAM_TYPE, Constants.DEFAULT_DATA_TYPE);
                }
            }

            if (oldParam.containsKey(Constants.SWAGGER_REQUIRED_PARAM)) {
                String oldRequiredParam = oldParam.get(Constants.SWAGGER_REQUIRED_PARAM).toString();
                if (oldRequiredParam.equalsIgnoreCase("true")) {
                    paramObj.put(Constants.SWAGGER_REQUIRED_PARAM, Boolean.TRUE);
                } else {
                    paramObj.put(Constants.SWAGGER_REQUIRED_PARAM, Boolean.FALSE);
                }
            } else {
                paramObj.put(Constants.SWAGGER_REQUIRED_PARAM, Boolean.FALSE);
            }
            if (oldParam.containsKey(Constants.SWAGGER_DESCRIPTION)) {
                paramObj.put(Constants.SWAGGER_DESCRIPTION,
                             oldParam.get(Constants.SWAGGER_DESCRIPTION));
            } else {
                paramObj.put(Constants.SWAGGER_DESCRIPTION, "");
            }
            newParameters.add(paramObj);
        }
    }

    /**
     * The purpose of this method is to extract the produces element from the swagger 1.2 documentation.
     * We expect that the element to be an json array. If it is not, we return an array with the default mediatype -
     * application/json.
     *
     * */
    private static JSONArray extractProducesObject(JSONObject swagger12doc){

        if(swagger12doc.containsKey(Constants.SWAGGER_PRODUCES)){
            Object producesRawObject = swagger12doc.get(Constants.SWAGGER_PRODUCES);
            if(producesRawObject instanceof JSONArray){
                return (JSONArray) producesRawObject;
            }
        }else{
            log.debug("No produces object found in swagger doc.");
        }
        return null;
    }

    /**
     * The purpose of this method is to extract the consumes element from the swagger 1.2 documentation.
     * We expect that the element to be an json array. If it is not, we return an array with the default mediatype -
     * application/json.
     *
     * */
    private static JSONArray extractConsumesObject(JSONObject swagger12doc){
        if(swagger12doc.containsKey(Constants.SWAGGER_CONSUMES)){
            Object consumesRawObject = swagger12doc.get(Constants.SWAGGER_CONSUMES);
            if(consumesRawObject instanceof JSONArray){
                return (JSONArray) consumesRawObject;
            }
        }else{
            log.debug("No consumes object found in swagger doc." );
        }
        return null;
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

        for (Tenant tenant : getTenantsArray()) {
            try {
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain());
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenant.getId());

                String adminName = ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId())
                        .getRealmConfiguration().getAdminUserName();

                ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
                Registry registry = ServiceHolder.getRegistryService().getGovernanceUserRegistry(adminName, tenant
                        .getId());
                GenericArtifactManager manager = new GenericArtifactManager(registry, "api");
                GovernanceUtils.loadGovernanceArtifacts((UserRegistry) registry);
                GenericArtifact[] artifacts = manager.getAllGenericArtifacts();

                for (GenericArtifact artifact : artifacts) {
                    try {
                        API api = APIUtil.getAPI(artifact, registry);

                        APIIdentifier apiIdentifier = api.getId();
                        String apiName = apiIdentifier.getApiName();
                        String apiVersion = apiIdentifier.getVersion();
                        String apiProviderName = apiIdentifier.getProviderName();

                        String swagger12location = ResourceUtil.getSwagger12ResourceLocation(apiName, apiVersion,
                                                                                             apiProviderName);

                        if (registry.resourceExists(swagger12location)) {
                            registry.delete(APIConstants.API_DOC_LOCATION);
                            if (log.isDebugEnabled()) {
                                log.debug("Resource deleted from the registry from path : " + swagger12location);
                            }
                        }
                    } catch (Exception e) {
                        // We log any exception that occurs and move to the next artifact.
                        log.error("API Management Exception occurred while cleaning the swagger resources of tenant :" +
                                  tenant.getDomain() + " of api : " + artifact.getAttribute("overview_name") + '-' +
                                                                     artifact.getAttribute("overview_version") + '-' +
                                                                     artifact.getAttribute("overview_provider"), e);
                    }
                }
            } catch (UserStoreException e) {
                log.error("Error occurred while reading tenant admin of tenant : " + tenant.getDomain(), e);
            } catch (RegistryException e) {
                log.error("Error occurred while accessing the registry of tenant  : " + tenant.getDomain(), e);
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
       	}

        if (log.isDebugEnabled()) {
            log.debug("End of old resources cleaned up.");
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
     */
    private void sequenceMigration() {
        String repository = CarbonUtils.getCarbonRepository();
        String TenantRepo = CarbonUtils.getCarbonTenantsDirPath();
        for (Tenant tenant : getTenantsArray()) {
            if (log.isDebugEnabled()) {
                log.debug("Start sequenceMigration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            }

            String SequenceFilePath;
            if (tenant.getId() != MultitenantConstants.SUPER_TENANT_ID) {
                SequenceFilePath = TenantRepo + '/' + tenant.getId() + "/synapse-configs/default/sequences/";
            } else {
                SequenceFilePath = repository + "synapse-configs/default/sequences/";
            }

            File sequenceFolder = new File(SequenceFilePath);
            if (!sequenceFolder.exists()) {
                if (log.isDebugEnabled()) {
                    log.debug("No sequence folder exists for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
                }
                continue;
            }

            try {
                FileUtils.copyInputStreamToFile(
                        MigrateFrom18to19.class.getResourceAsStream(
                                "/18to19Migration/sequence-scripts/_cors_request_handler_.xml"),
                        new File(SequenceFilePath +"_cors_request_handler_.xml"));

                ResourceUtil.copyNewSequenceToExistingSequences(SequenceFilePath, "_auth_failure_handler_");
                ResourceUtil.copyNewSequenceToExistingSequences(SequenceFilePath, "_throttle_out_handler_");
                ResourceUtil.copyNewSequenceToExistingSequences(SequenceFilePath, "_token_fault_");
                ResourceUtil.copyNewSequenceToExistingSequences(SequenceFilePath, "fault");
            } catch (IOException e) {
                log.error("Error occurred while reading file to copy.", e);
            } catch (APIMigrationException e) {
                log.error("Copying sequences failed", e);
            }

            if (log.isDebugEnabled()) {
                log.debug("End sequenceMigration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            }
        }
    }

    /**
     * This method is used to migrate synapse files
     * This changes the synapse api and add the new handlers
     */
    private void synapseAPIMigration() {
        String repository = CarbonUtils.getCarbonRepository();
        String tenantRepository = CarbonUtils.getCarbonTenantsDirPath();
        for (Tenant tenant : getTenantsArray()) {
            if (log.isDebugEnabled()) {
                log.debug("Start synapseAPIMigration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            }

            String apiFilePath;
            if (tenant.getId() != MultitenantConstants.SUPER_TENANT_ID) {
                apiFilePath = tenantRepository + '/' + tenant.getId() + "/synapse-configs/default/api";
            } else {
                apiFilePath = repository + "synapse-configs/default/api";
            }
            File APIFiles = new File(apiFilePath);
            File[] synapseFiles = APIFiles.listFiles();

            if (synapseFiles == null) {
                if (log.isDebugEnabled()) {
                    log.debug("No api folder " + apiFilePath + " exists for tenant " + tenant.getId() + '(' + tenant
                            .getDomain() + ')');
                }
                continue;
            }

            for (File synapseFile : synapseFiles) {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                docFactory.setNamespaceAware(true);

                try {
                    docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                    Document doc = docBuilder.parse(synapseFile);

                    doc.getDocumentElement().normalize();
                    Element rootElement = doc.getDocumentElement();

                    if (Constants.SYNAPSE_API_ROOT_ELEMENT.equals(rootElement.getNodeName()) && rootElement
                            .hasAttribute(Constants.SYNAPSE_API_ATTRIBUTE_VERSION)) {
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

            if (log.isDebugEnabled()) {
                log.debug("End synapseAPIMigration for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            }
        }
    }
}
