/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.apimgt.migration;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.doc.model.Operation;
import org.wso2.carbon.apimgt.api.doc.model.Parameter;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.URITemplate;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.migration.internal.ServiceHolder;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.ResourceUtil;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.governance.api.common.dataobjects.GovernanceArtifact;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;


public class SwaggerResMigration {
    private static final Log log = LogFactory.getLog(SwaggerResMigration.class);

    public void migrate() throws UserStoreException{
        log.info("In migrate() of SwaggerResMigration");

        TenantManager tenantManager = ServiceHolder.getRealmService().getTenantManager();
        Tenant[] tenantsArray = tenantManager.getAllTenants();

        // Add  super tenant to the tenant array
        Tenant[] allTenantsArray = Arrays.copyOf(tenantsArray, tenantsArray.length + 1);
        org.wso2.carbon.user.core.tenant.Tenant superTenant = new org.wso2.carbon.user.core.tenant.Tenant();
        superTenant.setId(MultitenantConstants.SUPER_TENANT_ID);
        superTenant.setDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        allTenantsArray[allTenantsArray.length - 1] = superTenant;

        for (Tenant tenant : allTenantsArray) {
            log.info("Swagger resource migration for tenant " + tenant.getDomain() + "[" + tenant.getId() + "]");
            try {
                //Start a new tenant flow
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain());
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenant.getId());

                String adminName = ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).getRealmConfiguration().getAdminUserName();
                ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
                Registry registry = ServiceHolder.getRegistryService().getGovernanceUserRegistry(adminName, tenant.getId());
                GenericArtifactManager manager = new GenericArtifactManager(registry, "api");
                GovernanceUtils.loadGovernanceArtifacts((UserRegistry) registry);
                GenericArtifact[] artifacts = manager.getAllGenericArtifacts();

                for (GenericArtifact artifact : artifacts) {
                	try {
	                    //API api = getAPI(artifact, registry);
	                    API api = APIUtil.getAPI(artifact, registry);
	                    APIIdentifier apiIdentfier = api.getId();
	                    artifact.setAttribute(APIConstants.PROTOTYPE_OVERVIEW_IMPLEMENTATION, APIConstants.IMPLEMENTATION_TYPE_ENDPOINT);
	                    manager.updateGenericArtifact(artifact);
	                    String apiDefinitionFilePath = getAPIDefinitionFilePath(apiIdentfier.getApiName(), apiIdentfier.getVersion(), apiIdentfier.getProviderName());
	                    Resource resource = registry.get(apiDefinitionFilePath);
	                    String text = new String((byte[]) resource.getContent());
	                    String newContentPath = APIUtil.getAPIDefinitionFilePath(apiIdentfier.getApiName(), apiIdentfier.getVersion(), apiIdentfier.getProviderName());
	                    Resource docContent = registry.newResource();
	                    docContent.setContent(text);
	                    docContent.setMediaType("text/plain");
	                    registry.put(newContentPath, docContent);
	                    String visibleRolesList = api.getVisibleRoles();
	                    String[] visibleRoles = new String[0];
	                    if (visibleRolesList != null) {
	                        visibleRoles = visibleRolesList.split(",");
	                    }
	                    ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).getAuthorizationManager().authorizeRole(APIConstants.ANONYMOUS_ROLE,
	                                                                                                                               "_system/governance" + newContentPath,
	                                                                                                                               ActionConstants.GET);



                    	//add swagger 1.2 resources
	                    log.info("Creating swagger 1.2 docs resources for : " + apiIdentfier.getApiName() + "-" +
	        					apiIdentfier.getVersion() + "-" + apiIdentfier.getProviderName());

						createSwagger12Resources(artifact, registry, api, tenant);

	                	//merge swagger 1.1 content with 1.2 resources
	                	log.info("Updating swagger 1.2 docs resource for : " + apiIdentfier.getApiName() + "-" +
	        					apiIdentfier.getVersion() + "-" + apiIdentfier.getProviderName());

						updateSwagger12ResourcesUsingSwagger11Doc(apiIdentfier, registry);

					} catch (RegistryException e) {
						log.error("RegistryException while migrating api in " + tenant.getDomain() , e);
					} catch (APIManagementException e) {
						log.error("APIManagementException while migrating api in " + tenant.getDomain() , e);
					} catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            } catch (RegistryException e) {
            	log.error("RegistryException while getting artifacts for  " + tenant.getDomain() , e);
			} catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }

    }

    private static String getAPIDefinitionFilePath(String apiName, String apiVersion, String apiProvider) {
       return APIConstants.API_DOC_LOCATION + RegistryConstants.PATH_SEPARATOR +
                              apiName + "-" + apiVersion + RegistryConstants.PATH_SEPARATOR + APIConstants.API_DOC_RESOURCE_NAME;
    }

    public static API getAPI(GovernanceArtifact artifact, Registry registry)
            throws APIManagementException {

        API api;
        try {
            String providerName = artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER);
            String apiName = artifact.getAttribute(APIConstants.API_OVERVIEW_NAME);
            String apiVersion = artifact.getAttribute(APIConstants.API_OVERVIEW_VERSION);
            APIIdentifier apiId = new APIIdentifier(providerName, apiName, apiVersion);
            api = new API(apiId);

            api.setUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_ENDPOINT_URL));
            api.setSandboxUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_SANDBOX_URL));
            api.setVisibility(artifact.getAttribute(APIConstants.API_OVERVIEW_VISIBILITY));
            api.setVisibleRoles(artifact.getAttribute(APIConstants.API_OVERVIEW_VISIBLE_ROLES));


        } catch (GovernanceException e) {
            String msg = "Failed to get API fro artifact ";
            throw new APIManagementException(msg, e);
        }
        return api;
    }


    /**
     * Update the swagger 1.2 resources using swagger 1.1 doc
     * @param adiIdentifier
     * @param registry
     * @throws APIManagementException
     * @throws RegistryException
     */
	private static void updateSwagger12ResourcesUsingSwagger11Doc(APIIdentifier adiIdentifier, Registry registry)
			throws APIManagementException, RegistryException {

		String apiDef11Path =
				APIUtil.getAPIDefinitionFilePath(adiIdentifier.getApiName(),
                        adiIdentifier.getVersion(),
                        adiIdentifier.getProviderName());
		Resource apiDef11 = registry.get(apiDef11Path);

		String apiDef11Json = new String((byte[]) apiDef11.getContent());

		String swagger12location =
				ResourceUtil.getSwagger12ResourceLocation(adiIdentifier.getApiName(),
				                                          adiIdentifier.getVersion(),
				                                          adiIdentifier.getProviderName());
		Resource swagger12Res = registry.get(swagger12location);
		String[] resourcePaths = (String[]) swagger12Res.getContent();

		try {

			ResourceUtil.updateAPISwaggerDocs(apiDef11Json, resourcePaths, registry);
		} catch (ParseException e) {
			throw new APIManagementException("Unable to parse registry resource", e);
		}

	}


    /**
     * save create resource at the given location and set permission
     * @param registry
     * @param content
     * @param resourcePath
     * @param api
     * @throws UserStoreException
     */
    private static void createResource(Registry registry, String content, String resourcePath, API api, Tenant tenant) throws UserStoreException{
    	try {
            Resource docContent = registry.newResource();
            docContent.setContent(content);
            docContent.setMediaType("text/plain");
            registry.put(resourcePath, docContent);

            String visibleRolesList = api.getVisibleRoles();
            String[] visibleRoles = new String[0];
            if (visibleRolesList != null) {
                visibleRoles = visibleRolesList.split(",");
            }
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
     * create swagger 1.2 resources
     * @param artifact
     * @param registry
     * @param api
     * @throws APIManagementException
     * @throws UserStoreException
     */
	public static void createSwagger12Resources(GovernanceArtifact artifact, Registry registry,
			API api, Tenant tenant) throws UserStoreException, APIManagementException  {

		JSONParser parser = new JSONParser();
		String pathJsonTemplate = "{\n    \"path\": \"\",\n    \"operations\": []\n}";
		String operationJsonTemplate = "{\n    \"method\": \"\",\n    \"parameters\": []\n}";

		// for apidoc
		String apiJsonTemplate = "{\n    \"apiVersion\": \"\",\n    \"swaggerVersion\": \"1.2\",\n    \"apis\": [],\n    \"info\": {\n        \"title\": \"\",\n        \"description\": \"\",\n        \"termsOfServiceUrl\": \"\",\n        \"contact\": \"\",\n        \"license\": \"\",\n        \"licenseUrl\": \"\"\n    },\n    \"authorizations\": {\n        \"oauth2\": {\n            \"type\": \"oauth2\",\n            \"scopes\": []\n        }\n    }\n}";

		// for each resource
		// String apiResourceTemplate =
		// "{\n    \"apiVersion\": \"\",\n    \"swaggerVersion\": \"1.2\",\n    \"resourcePath\":\"\",\n    \"apis\": [],\n    \"info\": {\n        \"title\": \"\",\n        \"description\": \"\",\n        \"termsOfServiceUrl\": \"\",\n        \"contact\": \"\",\n        \"license\": \"\",\n        \"licenseUrl\": \"\"\n    },\n    \"authorizations\": {\n        \"oauth2\": {\n            \"type\": \"oauth2\",\n            \"scopes\": []\n        }\n    }\n}";
		String apiResourceJsonTemplate = "{\n    \"apiVersion\": \"\",\n    \"swaggerVersion\": \"1.2\",\n    \"resourcePath\":\"\",\n    \"apis\": [] \n}";

        //Auth Types
        HashMap<String,String> auth_types = new HashMap<String, String>();
        auth_types.put("None","None");
        auth_types.put("Application_User","Application User");
        auth_types.put("Application","Application");
        auth_types.put("Any","Application & Application User");

		//List<String> uriTemplateNames = new ArrayList<String>();

		//new
		List<String> uriTemplatePathNames = new ArrayList<String>();

		Map<String, JSONObject> resourceNameJSONs = new HashMap<String, JSONObject>();


		// resousepath-operations list
		Map<String, List<JSONObject>> resourcePathOperations = new HashMap<String, List<JSONObject>>();

		Map<String, String> resourceNamePathNameMap = new HashMap<String, String>();
		JSONObject mainAPIJson = null;
		try {

			String apiVersion = artifact
					.getAttribute(APIConstants.API_OVERVIEW_VERSION);


			int i = 0;

			Set<URITemplate> uriTemplates = api.getUriTemplates();
			Map<String, List<String>> resourceNamepaths = new HashMap<String, List<String>>();
			Map<String, List<JSONObject>> resourcePathJSONs = new HashMap<String, List<JSONObject>>();

			for (URITemplate template : uriTemplates) {
	    		List<Operation> ops;
	    		List<Parameter> parameters = null;

	    		String path = template.getUriTemplate();

	    		if (path != null && (path.equals("/*") || (path.equals("/")))) {
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
	    			JSONObject resourcePathJson = (JSONObject) parser.parse(apiResourceJsonTemplate);

//	    			resourcePathJson.put("apiVersion", version);
//	    			resourcePathJson.put("resourcePath", resourceName);
//	    			resourceNameJSONs.put(resourceName, resourcePathJson);

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



			// store api object(which contains operations objects) against the resource path
			Map<String, JSONObject> pathNameApi = new HashMap<String, JSONObject>();

			//list to store the api array objects
			List<JSONObject> apiArray = new ArrayList<JSONObject>();

			for (Entry<String, List<JSONObject>> entry : resourcePathJSONs
					.entrySet()) {
				String resourcePath = entry.getKey();
				// JSONObject jsonOb = resourceNameJSONs.get(resourcePath);
				// List<JSONObject> pathItems = entry.getValue();
				// for (JSONObject pathItem : pathItems) {
				JSONObject pathJson = (JSONObject) parser
						.parse(pathJsonTemplate);
				pathJson.put("path", resourcePath);
				List<JSONObject> methodJsons = entry.getValue();
				for (JSONObject methodJson : methodJsons) {
					JSONArray operations = (JSONArray) pathJson
							.get("operations");
					operations.add(methodJson);
				}

				pathNameApi.put(resourcePath, pathJson);

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
			JSONObject resourcesObj = (JSONObject) parser.parse(apiResourceJsonTemplate);
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
}