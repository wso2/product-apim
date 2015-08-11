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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.Documentation;
import org.wso2.carbon.apimgt.api.model.URITemplate;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.client.internal.ServiceHolder;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.ResourceUtil;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.core.*;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Created by uvindra on 8/8/15.
 */
public class RegistryMigration {
    private static final Log log = LogFactory.getLog(RegistryMigration.class);

    public static void updateDocResourceAssociation(GenericArtifact[] artifacts, Registry registry, Tenant tenant) {
        log.debug("Calling updateDocResourceAssociation");
        for (GenericArtifact artifact : artifacts) {
            API api = getAPI(artifact);

            if (api != null) {
                APIIdentifier apiIdentifier = api.getId();
                String apiName = apiIdentifier.getApiName();
                String apiVersion = apiIdentifier.getVersion();
                String apiProviderName = apiIdentifier.getProviderName();
                APIIdentifier apiIdentfier = api.getId();

                String apiResourcePath = APIUtil.getAPIPath(apiIdentfier);
                try{
                    Association[] docAssociations = registry.getAssociations(apiResourcePath, "document");
                    for (Association association : docAssociations) {
                        String docPath = association.getDestinationPath();

                        Resource docResource = registry.get(docPath);
                        GenericArtifactManager docArtifactManager = new GenericArtifactManager(registry,
                                APIConstants.DOCUMENTATION_KEY);
                        GenericArtifact docArtifact = docArtifactManager.getGenericArtifact(
                                docResource.getUUID());
                        String docFilePath = docArtifact.getAttribute(APIConstants.DOC_FILE_PATH);
                        Documentation doc = APIUtil.getDocumentation(docArtifact);
                        if (Documentation.DocumentSourceType.FILE.equals(doc.getSourceType())) {
                            if(docFilePath != null && !docFilePath.equals("")){
                                //The docFilePatch comes as /t/tenanatdoman/registry/resource/_system/governance/apimgt/applicationdata..
                                //We need to remove the /t/tenanatdoman/registry/resource/_system/governance section to set permissions.
                                int startIndex = docFilePath.indexOf("governance") + "governance".length();
                                String filePath = docFilePath.substring(startIndex, docFilePath.length());
                                if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".") == -1) {
                                    Resource resource = registry.get(filePath);
                                    resource.setMediaType("text/plain");
                                    registry.put(filePath, resource);
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".wsdl") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("application/api-wsdl");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".pdf") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("application/pdf");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".xl") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("application/vnd.ms-excel");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".ppt") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("application/vnd.ms-powerpoint");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".xml") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("application/xml");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".js") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("application/javascript");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".css") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("text/css");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".csv") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("text/csv");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".html") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("text/html");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".json") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("application/json");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".png") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("image/png");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".ttf") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("application/x-font-ttf");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".eot") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("application/vnd.ms-fontobject");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".woff") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("application/font-woff");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".otf") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("application/x-font-otf");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".zip") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("application/zip");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".xhtml") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("application/xhtml+xml");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".txt") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("text/plain");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".png") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("image/png");
                                        registry.put(filePath, resource);
                                    }
                                } else if (filePath.split(RegistryConstants.PATH_SEPARATOR + "files" + RegistryConstants.PATH_SEPARATOR)[1].indexOf(".jpeg") > -1) {
                                    String resourcePath = "_system" + RegistryConstants.PATH_SEPARATOR + "governance" + filePath;
                                    if (registry.resourceExists(resourcePath)) {
                                        Resource resource = registry.get(filePath);
                                        resource.setMediaType("image/jpeg");
                                        registry.put(filePath, resource);
                                    }
                                }

                                registry.copy(filePath, filePath);
                                registry.addAssociation(docArtifact.getPath(), filePath,
                                        APIConstants.DOCUMENTATION_FILE_ASSOCIATION);
                            }
                        }
                    }
                } catch (RegistryException e) {
                    log.error("Registry error encountered for api " + apiName + "-" + apiVersion + "-" + apiProviderName + " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
                } catch (APIManagementException e) {
                    log.error("Error occurred while creating swagger v2.0 document for api " + apiName + "-" + apiVersion + "-" + apiProviderName + " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
                }
            }
        }
    }

    public static void rxtMigration(GenericArtifactManager manager, GenericArtifact[] artifacts) {
        for (GenericArtifact artifact : artifacts) {
            try {
                artifact.setAttribute(APIConstants.PROTOTYPE_OVERVIEW_IMPLEMENTATION, APIConstants.IMPLEMENTATION_TYPE_ENDPOINT);

                manager.updateGenericArtifact(artifact);

            } catch (RegistryException e) {
                log.error("Registry error encountered when updating rxt", e);
            }
        }

    }

    public static void swagger12Migration(GenericArtifact[] artifacts, Registry registry, Tenant tenant) {
        for (GenericArtifact artifact : artifacts) {
            API api = getAPI(artifact);

            if (api != null) {
                APIIdentifier apiIdentifier = api.getId();
                try {
                    createSwagger12Resources(artifact, registry, api, tenant);

                    updateSwagger12ResourcesUsingSwagger11Doc(apiIdentifier, registry);
                } catch (UserStoreException e) {
                    log.error("UserStoreException thrown when migrating swagger for api "+ api.getId().getApiName() + "-" + api.getId().getVersion() + "-" + api.getId().getProviderName() + " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
                } catch (ParseException e) {
                    log.error("ParseException thrown when migrating swagger for api " + api.getId().getApiName() + "-" + api.getId().getVersion() + "-" + api.getId().getProviderName() + " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
                } catch (APIManagementException e) {
                    log.error("APIManagementException thrown when migrating swagger for api " + api.getId().getApiName() + "-" + api.getId().getVersion() + "-" + api.getId().getProviderName() + " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
                } catch (RegistryException e) {
                    log.error("RegistryException thrown when migrating swagger for api " + api.getId().getApiName() + "-" + api.getId().getVersion() + "-" + api.getId().getProviderName() + " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
                } catch (UnsupportedEncodingException e) {
                    log.error("UnsupportedEncodingException thrown when migrating swagger for api " + api.getId().getApiName() + "-" + api.getId().getVersion() + "-" + api.getId().getProviderName() + " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
                } catch (APIMigrationException e) {
                    log.error("APIMigrationException thrown when migrating swagger for api " + api.getId().getApiName() + "-" + api.getId().getVersion() + "-" + api.getId().getProviderName() + " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
                }
            }
        }
    }

    /**
     * create swagger 1.2 resources
     * @param artifact
     * @param registry
     * @param api
     * @throws APIManagementException
     * @throws RegistryException
     * @throws UserStoreException
     */
    public static void createSwagger12Resources(GenericArtifact artifact, Registry registry,
                                                API api, Tenant tenant) throws UserStoreException, APIManagementException, ParseException {

        JSONParser parser = new JSONParser();
        String pathJsonTemplate = "{\n    \"path\": \"\",\n    \"operations\": []\n}";
        String operationJsonTemplate = "{\n    \"method\": \"\",\n    \"parameters\": []\n}";

        // for apidoc
        String apiJsonTemplate = "{\n    \"apiVersion\": \"\",\n    \"swaggerVersion\": \"1.2\",\n    \"apis\": [],\n    \"info\": {\n        \"title\": \"\",\n        \"description\": \"\",\n        \"termsOfServiceUrl\": \"\",\n        \"contact\": \"\",\n        \"license\": \"\",\n        \"licenseUrl\": \"\"\n    },\n    \"authorizations\": {\n        \"oauth2\": {\n            \"type\": \"oauth2\",\n            \"scopes\": []\n        }\n    }\n}";

        // for each resource
        // String apiResourceJsontemplate =
        // "{\n    \"apiVersion\": \"\",\n    \"swaggerVersion\": \"1.2\",\n    \"resourcePath\":\"\",\n    \"apis\": [],\n    \"info\": {\n        \"title\": \"\",\n        \"description\": \"\",\n        \"termsOfServiceUrl\": \"\",\n        \"contact\": \"\",\n        \"license\": \"\",\n        \"licenseUrl\": \"\"\n    },\n    \"authorizations\": {\n        \"oauth2\": {\n            \"type\": \"oauth2\",\n            \"scopes\": []\n        }\n    }\n}";
        String apiResourceJsontemplate = "{\n    \"apiVersion\": \"\",\n    \"swaggerVersion\": \"1.2\",\n    \"resourcePath\":\"\",\n    \"apis\": [] \n}";

        //Auth Types
        HashMap<String,String> auth_types = new HashMap<String, String>();
        auth_types.put("None","None");
        auth_types.put("Application_User","Application User");
        auth_types.put("Application","Application");
        auth_types.put("Any","Application & Application User");

        JSONObject mainAPIJson;
        try {
            String apiVersion = artifact
                    .getAttribute(APIConstants.API_OVERVIEW_VERSION);

            Set<URITemplate> uriTemplates = api.getUriTemplates();
            Map<String, List<String>> resourceNamepaths = new HashMap<String, List<String>>();
            Map<String, List<JSONObject>> resourcePathJSONs = new HashMap<String, List<JSONObject>>();

            for (URITemplate template : uriTemplates) {

                String path = template.getUriTemplate();

                if (path == null) {
                    log.error("URI Template does not exist for api "+ api.getId().getApiName() + "-" + api.getId().getVersion() + "-" + api.getId().getProviderName() + " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
                    continue;
                }

                if (path.equals("/")) {
                    path = "/*";
                }
                List<String> resourcePaths;
                int resourceNameEndIndex = path.indexOf("/", 1);
                String resourceName = "/default";
                if(resourceNameEndIndex != -1) {
                    resourceName = path.substring(1, resourceNameEndIndex);
                }

                if(!resourceName.startsWith("/")) {
                    resourceName = "/" + resourceName;
                }

                if(resourceNamepaths.get(resourceName) != null) {
                    resourcePaths = resourceNamepaths.get(resourceName);
                    if (!resourcePaths.contains(path)) {
                        resourcePaths.add(path);
                    }
                    //verbs comes as a [POST, GET] type of a string
                    String[] httpVerbs = template.getMethodsAsString().split(" ");
                    String[] authtypes = template.getAuthTypeAsString().split(" ");
                    String[] tries = template.getThrottlingTiersAsString().split(" ");

                    for(int j = 0; j < httpVerbs.length ; j ++) {
                        final JSONObject operationJson = (JSONObject) parser.parse(operationJsonTemplate);
                        operationJson.put("method", httpVerbs[j]);
                        operationJson.put("auth_type", auth_types.get(authtypes[j]));
                        operationJson.put("throttling_tier", tries[j]);

                        if(resourcePathJSONs.get(path) != null) {
                            resourcePathJSONs.get(path).add(operationJson);

                        } else {
                            resourcePathJSONs.put(path, new ArrayList<JSONObject>() {{
                                add(operationJson);
                            }});
                        }
                    }
                    resourceNamepaths.put(resourceName, resourcePaths);
                } else {
                    resourcePaths = new ArrayList<String>();
                    resourcePaths.add(path);

                    //verbs comes as a [POST, GET] type of a string
                    String[] httpVerbs = template.getMethodsAsString().split(" ");
                    String[] authtypes = template.getAuthTypeAsString().split(" ");
                    String[] tries = template.getThrottlingTiersAsString().split(" ");

                    for(int j = 0; j < httpVerbs.length ; j ++) {
                        final JSONObject operationJson = (JSONObject) parser.parse(operationJsonTemplate);
                        operationJson.put("method", httpVerbs[j]);
                        operationJson.put("auth_type", auth_types.get(authtypes[j]));
                        operationJson.put("throttling_tier", tries[j]);

                        if(resourcePathJSONs.get(path) != null) {
                            resourcePathJSONs.get(path).add(operationJson);

                        } else {
                            resourcePathJSONs.put(path, new ArrayList<JSONObject>() {{
                                add(operationJson);
                            }});
                        }
                    }

                    resourceNamepaths.put(resourceName, resourcePaths);
                }
            }

            //list to store the api array objects
            List<JSONObject> apiArray = new ArrayList<JSONObject>();

            for (Map.Entry<String, List<JSONObject>> entry : resourcePathJSONs
                    .entrySet()) {
                String resourcePath = entry.getKey();
                JSONObject pathJson = (JSONObject) parser
                        .parse(pathJsonTemplate);
                pathJson.put("path", resourcePath);
                List<JSONObject> methodJsons = entry.getValue();
                for (JSONObject methodJson : methodJsons) {
                    JSONArray operations = (JSONArray) pathJson
                            .get("operations");
                    operations.add(methodJson);
                }

                apiArray.add(pathJson);
            }

            /**
             * create only one resource doc for all the resources. name it as 'resources'
             */
            // create resources in the registry
            APIIdentifier apiIdentfier = api.getId();
            String apiDefinitionFilePath = APIUtil.getSwagger12DefinitionFilePath(apiIdentfier.getApiName(),
                    apiIdentfier.getVersion(),apiIdentfier.getProviderName());

            String resourceName = Constants.API_DOC_12_ALL_RESOURCES_DOC;
            JSONObject resourcesObj = (JSONObject) parser.parse(apiResourceJsontemplate);
            resourcesObj.put("apiVersion", apiVersion);
            resourcesObj.put("resourcePath", "/" + resourceName);
            JSONArray apis = (JSONArray) resourcesObj.get("apis");
            //add all the apis to single one
            for(JSONObject arraObj : apiArray){
                apis.add(arraObj);
            }
            String registryRes = apiDefinitionFilePath
                    + RegistryConstants.PATH_SEPARATOR + resourceName;
            createResource(registry, resourcesObj.toJSONString(), registryRes, api,tenant);

            // create api-doc file in the 1.2 resource location

            mainAPIJson = (JSONObject) parser.parse(apiJsonTemplate);
            mainAPIJson.put("apiVersion", apiVersion);
            ((JSONObject)mainAPIJson.get("info")).put("description", "Available resources");
            ((JSONObject)mainAPIJson.get("info")).put("title", artifact.getAttribute(APIConstants.API_OVERVIEW_NAME));

            JSONArray apis1 = (JSONArray) mainAPIJson.get("apis");
            JSONObject pathjob = new JSONObject();
            pathjob.put("path", "/" + resourceName);
            pathjob.put("description", "All resources for the api");
            apis1.add(pathjob);


            createResource(registry, mainAPIJson.toJSONString(),
                    apiDefinitionFilePath
                            + APIConstants.API_DOC_1_2_RESOURCE_NAME, api, tenant);

        } catch (GovernanceException e) {
            String msg = "Failed to get API fro artifact ";
            throw new APIManagementException(msg, e);
        } catch (ParseException e) {
            throw new APIManagementException(
                    "Error while generating swagger 1.2 resource for api ", e);
        }

    }

    /**
     * save create resource at the given location and set permission
     * @param re
     * @param content
     * @param resourcePath
     * @param api
     * @throws UserStoreException
     * @throws RegistryException
     * @throws APIManagementException
     */
    private static void createResource(Registry re, String content, String resourcePath, API api, Tenant tenant) throws UserStoreException{
        try {
            Resource docContent = re.newResource();
            docContent.setContent(content);
            docContent.setMediaType("text/plain");
            re.put(resourcePath, docContent);

            ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).getAuthorizationManager().authorizeRole(APIConstants.ANONYMOUS_ROLE,
                    "_system/governance" + resourcePath,
                    ActionConstants.GET);
        } catch (RegistryException e) {
            String msg = "Failed to add the API Definition content of : "
                    + APIConstants.API_DEFINITION_DOC_NAME + " of API :" + api.getId().getApiName();
            log.error(msg);
        }
    }

    /**
     * Update the swagger 1.2 resources using swagger 1.1 doc
     * @param apiIdentfier
     * @param registry
     * @throws APIManagementException
     * @throws RegistryException
     */
    private static void updateSwagger12ResourcesUsingSwagger11Doc(APIIdentifier apiIdentfier, Registry registry)
            throws APIManagementException, RegistryException, UnsupportedEncodingException, APIMigrationException {

        String apiDef11Path =
                ResourceUtil.getSwagger11ResourceLocation(apiIdentfier.getApiName(),
                        apiIdentfier.getVersion());

        if (!registry.resourceExists(apiDef11Path)) {
            throw new APIMigrationException("Swagger 1.1 document at " + apiDef11Path + " does not exist");
        }

        Resource apiDef11 = registry.get(apiDef11Path);

        String apiDef11Json = new String((byte[]) apiDef11.getContent(), "UTF8");

        String swagger12location =
                ResourceUtil.getSwagger12ResourceLocation(apiIdentfier.getApiName(),
                        apiIdentfier.getVersion(),
                        apiIdentfier.getProviderName());

        if (!registry.resourceExists(swagger12location)) {
            throw new APIMigrationException("Swagger 1.2 document at " + swagger12location + " does not exist");
        }

        Resource swagger12Res = registry.get(swagger12location);
        String[] resourcePaths = (String[]) swagger12Res.getContent();

        try {
            ResourceUtil.updateAPISwaggerDocs(apiDef11Json, resourcePaths, registry);
        } catch (ParseException e) {
            throw new APIManagementException("Unable to parse registry resource", e);
        }

    }


    private static API getAPI(GenericArtifact artifact) {
        log.debug("Calling getAPI");
        API api = null;

        try {
            api = APIUtil.getAPI(artifact);
        } catch (APIManagementException e) {
            log.error("Error when getting api artifact " + artifact.getId() + " from registry", e);
        }

        return api;
    }
}
