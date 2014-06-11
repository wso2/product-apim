/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.registry.migration;

import java.io.File;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.doc.model.APIDefinition;
import org.wso2.carbon.apimgt.api.doc.model.APIResource;
import org.wso2.carbon.apimgt.api.doc.model.Operation;
import org.wso2.carbon.apimgt.api.doc.model.Parameter;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.URITemplate;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.governance.api.common.dataobjects.GovernanceArtifact;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.Tag;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.migration.utils.ApiDAO;
import org.wso2.carbon.registry.migration.utils.CommandHandler;
import org.wso2.carbon.registry.migration.utils.DBUtils;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceResourceServiceExceptionException;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceStub;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;

import com.google.gson.Gson;


public class Main {
    private static Registry registry;
    static ConfigurationContext cc;


    public static void main(String[] args) {
        CommandHandler.setInputs(args);
        try {
            String trustStore = System.getProperty("carbon.home") + File.separator + "repository" + File.separator +
                    "resources" + File.separator + "security" + File.separator + "wso2carbon.jks";
            System.setProperty("javax.net.ssl.trustStore", trustStore);
            System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");
            System.setProperty("carbon.repo.write.mode", "true");

            cc = ConfigurationContextFactory.createConfigurationContextFromFileSystem
                    (System.getProperty("carbon.home") + "repository/deployment/client", System.getProperty("carbon.home") +
                            "repository/conf/axis2/axis2_client.xml");
            registry = new WSRegistryServiceClient(CommandHandler.getServiceURL(), CommandHandler.getUsername(),
                    CommandHandler.getPassword(), cc);
            DBUtils.initializeDB();
            Main.migrate();
            System.out.println("Finished API data migration process.");
            System.exit(0);
        } catch (RegistryException e) {
            System.out.println("Error while connecting to registry." + e);
        } catch (Exception e) {
            System.out.println("Error while migrating data." + e);
        }

    }

    private static void migrate() throws RegistryException, SQLException, APIManagementException, RemoteException, ResourceAdminServiceResourceServiceExceptionException, LoginAuthenticationExceptionException {
        Registry re = GovernanceUtils.getGovernanceUserRegistry(registry, CommandHandler.getUsername());
        GenericArtifactManager manager = new GenericArtifactManager(re, "api");
        GovernanceUtils.loadGovernanceArtifacts((UserRegistry) re);
        GenericArtifact[] artifacts = manager.getAllGenericArtifacts();

        for (GenericArtifact artifact : artifacts) {
        	API api = getAPI(artifact, re);
        	createUpdateAPIDefinition(api);
        }
     
    }
    
    /**
     * Create API Definition in JSON and save in the registry
     *
     * @param api API
     * @throws org.wso2.carbon.apimgt.api.APIManagementException
     *          if failed to generate the content and save
     * @throws ResourceAdminServiceResourceServiceExceptionException 
     * @throws RemoteException 
     * @throws LoginAuthenticationExceptionException 
     */
    private static void createUpdateAPIDefinition(API api) throws APIManagementException, RemoteException, ResourceAdminServiceResourceServiceExceptionException, LoginAuthenticationExceptionException {
    	APIIdentifier identifier = api.getId(); 
    	
    	try{
    		String jsonText = createSwaggerJSONContent(api);
    		
    		String resourcePath = "_system/governance" + APIUtil.getAPIDefinitionFilePath(identifier.getApiName(), identifier.getVersion()); 
    		
    		Resource resource = registry.newResource();
    		    		
    		resource.setContent(jsonText);
    		resource.setMediaType("application/json");
    		registry.put(resourcePath, resource);
    		
    		String sessionCookie = login(CommandHandler.getUsername(),
                    CommandHandler.getPassword(), CommandHandler.getHost());
    		//String permissionString = "ra^false:rd^false:wa^false:wd^false:da^false:dd^false:aa^false:ad^false";
    		ResourceAdminServiceStub stub = new ResourceAdminServiceStub(CommandHandler.getServiceURL() + "ResourceAdminService");
    		
    		ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, sessionCookie);
    		stub.addRolePermission(resourcePath, APIConstants.ANONYMOUS_ROLE, "2", "1");
    		/*Set permissions to anonymous role */
    		//APIUtil.setResourcePermissions(api.getId().getProviderName(), null, null, resourcePath);
    			    
    	} catch (RegistryException e) {
    		System.out.println("Error while adding API Definition for " + identifier.getApiName() + "-" + identifier.getVersion());
		} catch (APIManagementException e) {
			System.out.println("Error while adding API Definition for " + identifier.getApiName() + "-" + identifier.getVersion());
		}
    }
    
    /**
     * Create API Definition in JSON
     *
     * @param api API
     * @throws org.wso2.carbon.apimgt.api.APIManagementException
     *          if failed to generate the content and save
     */
    private static String createSwaggerJSONContent(API api) throws APIManagementException {
    	APIIdentifier identifier = api.getId();    	

		/*APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        String endpoints = config.getFirstProperty(APIConstants.API_GATEWAY_API_ENDPOINT);*/
     
        
        String endpoint = CommandHandler.getGatewayURL();
        
        String apiContext = api.getContext();
        String version = identifier.getVersion();
        Set<URITemplate> uriTemplates = api.getUriTemplates();
        String description = api.getDescription();
        String urlPrefix = apiContext + "/" +version;
                        
        if (description == null || description.equals("")) {
    		description = "no-info";
    	}
    	
    	Map<String, List<Operation>> uriTemplateDefinitions = new HashMap<String, List<Operation>>();
    	List<APIResource> apis = new ArrayList<APIResource>();
    	for (URITemplate template : uriTemplates) {
    		List<Operation> ops;
    		List<Parameter> parameters = null;
    		String path = urlPrefix + 
    				APIUtil.removeAnySymbolFromUriTempate(template.getUriTemplate());
    		/* path exists in uriTemplateDefinitions */
    		if (uriTemplateDefinitions.get(path) != null) {
    			ops = uriTemplateDefinitions.get(path);
    			parameters = new ArrayList<Parameter>();
    			if (!(template.getAuthType().equals(APIConstants.AUTH_NO_AUTHENTICATION))) {
    				Parameter authParam = new Parameter(APIConstants.AuthParameter.AUTH_PARAM_NAME, 
    						APIConstants.AuthParameter.AUTH_PARAM_DESCRIPTION, APIConstants.AuthParameter.AUTH_PARAM_TYPE, true, false, "String");
    				parameters.add(authParam);
    			}
    			String httpVerb = template.getHTTPVerb();
    			/* For GET and DELETE Parameter name - Query Parameters*/
    			if (httpVerb.equals(Constants.Configuration.HTTP_METHOD_GET) ||
    					httpVerb.equals(Constants.Configuration.HTTP_METHOD_DELETE)) {
    				Parameter queryParam = new Parameter(APIConstants.AuthParameter.PAYLOAD_PARAM_NAME, 
    						APIConstants.AuthParameter.PAYLOAD_PARAM_DESCRIPTION, APIConstants.AuthParameter.PAYLOAD_PARAM_TYPE, false, false, "String");
    				parameters.add(queryParam);
    			} else {/* For POST and PUT Parameter name - Payload*/
    				Parameter payLoadParam = new Parameter(APIConstants.AuthParameter.PAYLOAD_PARAM_NAME, 
    						APIConstants.AuthParameter.PAYLOAD_PARAM_DESCRIPTION, APIConstants.AuthParameter.PAYLOAD_PARAM_TYPE, false, false, "String");
    				parameters.add(payLoadParam);
    			}
    			Operation op = new Operation(httpVerb, description, description, parameters);
    			ops.add(op);
    		} else {/* path not exists in uriTemplateDefinitions */
    			ops = new ArrayList<Operation>();
    			parameters = new ArrayList<Parameter>();
				if (!(template.getAuthType().equals(APIConstants.AUTH_NO_AUTHENTICATION))) {
    				Parameter authParam = new Parameter(APIConstants.AuthParameter.AUTH_PARAM_NAME, 
    						APIConstants.AuthParameter.AUTH_PARAM_DESCRIPTION, APIConstants.AuthParameter.AUTH_PARAM_TYPE, true, false, "String");
    				parameters.add(authParam);
    			}
				String httpVerb = template.getHTTPVerb();
				/* For GET and DELETE Parameter name - Query Parameters*/
    			if (httpVerb.equals(Constants.Configuration.HTTP_METHOD_GET) ||
    					httpVerb.equals(Constants.Configuration.HTTP_METHOD_DELETE)) {
    				Parameter queryParam = new Parameter(APIConstants.AuthParameter.PAYLOAD_PARAM_NAME, 
    						APIConstants.AuthParameter.PAYLOAD_PARAM_DESCRIPTION, APIConstants.AuthParameter.PAYLOAD_PARAM_TYPE, false, false, "String");
    				parameters.add(queryParam);
    			} else {/* For POST and PUT Parameter name - Payload*/
    				Parameter payLoadParam = new Parameter(APIConstants.AuthParameter.PAYLOAD_PARAM_NAME, 
    						APIConstants.AuthParameter.PAYLOAD_PARAM_DESCRIPTION, APIConstants.AuthParameter.PAYLOAD_PARAM_TYPE, false, false, "String");
    				parameters.add(payLoadParam);
    			}
    			Operation op = new Operation(httpVerb, description, description, parameters);
    			ops.add(op);
    			uriTemplateDefinitions.put(path, ops);
    		}
    	}
    	
    	Set<String> resPaths = uriTemplateDefinitions.keySet();
		
		for (String resPath: resPaths) {
			APIResource apiResource = new APIResource(resPath, description, uriTemplateDefinitions.get(resPath));
			apis.add(apiResource);
    	}
			
		APIDefinition apidefinition = new APIDefinition(version, APIConstants.SWAGGER_VERSION, endpoint, apiContext, apis);
    	    		    		
    	Gson gson = new Gson();
    	return gson.toJson(apidefinition); 
     }
    
    private static String login(String userName, String password, String host)
            throws LoginAuthenticationExceptionException, RemoteException {
        Boolean loginStatus;
        ServiceContext serviceContext;
        String sessionCookie;
        AuthenticationAdminStub authenticationAdminStub = new AuthenticationAdminStub(CommandHandler.getServiceURL() + "AuthenticationAdmin");
        loginStatus = authenticationAdminStub.login(userName, password, host);

        if (!loginStatus) {
            throw new LoginAuthenticationExceptionException("Login Unsuccessful. Return false as a login status by Server");
        }
        serviceContext = authenticationAdminStub._getServiceClient().getLastOperationContext().getServiceContext();
        sessionCookie = (String) serviceContext.getProperty(HTTPConstants.COOKIE_STRING);
        return sessionCookie;
    }
    
    public static API getAPI(GovernanceArtifact artifact, Registry registry)
            throws APIManagementException, SQLException {

        API api;
        try {
            String providerName = artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER);
            String apiName = artifact.getAttribute(APIConstants.API_OVERVIEW_NAME);
            String apiVersion = artifact.getAttribute(APIConstants.API_OVERVIEW_VERSION);
            APIIdentifier apiId=new APIIdentifier(providerName, apiName, apiVersion);
            api = new API(apiId);
            // set rating
            String artifactPath = GovernanceUtils.getArtifactPath(registry, artifact.getId());
           // BigDecimal bigDecimal = new BigDecimal(getAverageRating(apiId));
            //BigDecimal res = bigDecimal.setScale(1, RoundingMode.HALF_UP);
            
            //set description
            api.setDescription(artifact.getAttribute(APIConstants.API_OVERVIEW_DESCRIPTION));
            //set last access time
            api.setLastUpdated(registry.get(artifactPath).getLastModified());
            // set url
            api.setUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_ENDPOINT_URL));
            api.setSandboxUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_SANDBOX_URL));
            api.setThumbnailUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_THUMBNAIL_URL));
            api.setWsdlUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_WSDL));
            api.setWadlUrl(artifact.getAttribute(APIConstants.API_OVERVIEW_WADL));
            api.setTechnicalOwner(artifact.getAttribute(APIConstants.API_OVERVIEW_TEC_OWNER));
            api.setTechnicalOwnerEmail(artifact.getAttribute(APIConstants.API_OVERVIEW_TEC_OWNER_EMAIL));
            api.setBusinessOwner(artifact.getAttribute(APIConstants.API_OVERVIEW_BUSS_OWNER));
            api.setBusinessOwnerEmail(artifact.getAttribute(APIConstants.API_OVERVIEW_BUSS_OWNER_EMAIL));
            api.setVisibility(artifact.getAttribute(APIConstants.API_OVERVIEW_VISIBILITY));
            api.setVisibleRoles(artifact.getAttribute(APIConstants.API_OVERVIEW_VISIBLE_ROLES));
            api.setVisibleTenants(artifact.getAttribute(APIConstants.API_OVERVIEW_VISIBLE_TENANTS));
            api.setEndpointSecured(Boolean.parseBoolean(artifact.getAttribute(APIConstants.API_OVERVIEW_ENDPOINT_SECURED)));
            api.setEndpointUTUsername(artifact.getAttribute(APIConstants.API_OVERVIEW_ENDPOINT_USERNAME));
            api.setEndpointUTPassword(artifact.getAttribute(APIConstants.API_OVERVIEW_ENDPOINT_PASSWORD));

            
            api.setContext(artifact.getAttribute(APIConstants.API_OVERVIEW_CONTEXT));
            api.setLatest(Boolean.valueOf(artifact.getAttribute(APIConstants.API_OVERVIEW_IS_LATEST)));


            Set<URITemplate> uriTemplates = new LinkedHashSet<URITemplate>();
            List<String> uriTemplateNames = new ArrayList<String>();


            HashMap<String,String> urlPatternsSet;
            urlPatternsSet = ApiDAO.getURITemplatesPerAPIAsString(api.getId(), DBUtils.getConnection());
            Set<String> urlPatternsKeySet = urlPatternsSet.keySet();
            for (String urlPattern : urlPatternsKeySet) {
                    URITemplate uriTemplate = new URITemplate();
                    String uTemplate = urlPattern.split("::")[0];
                    String method = urlPattern.split("::")[1];
                    String authType = urlPattern.split("::")[2];

                    uriTemplate.setHTTPVerb(method);
                    uriTemplate.setAuthType(authType);
                    uriTemplate.setHttpVerbs(method);
                    uriTemplate.setAuthTypes(authType);
                    uriTemplate.setUriTemplate(uTemplate);
                    uriTemplate.setResourceURI(api.getUrl());
                    uriTemplate.setResourceSandboxURI(api.getSandboxUrl());

                    /*//Checking for duplicate uri template names
                    if (uriTemplateNames.contains(uTemplate)) {
                        for (URITemplate tmp : uriTemplates) {
                            if (uTemplate.equals(tmp.getUriTemplate())) {
                                tmp.setHttpVerbs(method);
                                tmp.setAuthTypes(authType);
                                break;
                            }
                        }

                    } else {
                        uriTemplates.add(uriTemplate);
                    }

                    uriTemplateNames.add(uTemplate);*/
                    uriTemplates.add(uriTemplate);


                }
            api.setUriTemplates(uriTemplates);


            Set<String> tags = new HashSet<String>();
            org.wso2.carbon.registry.core.Tag[] tag = registry.getTags(artifactPath);
            for (Tag tag1 : tag) {
                tags.add(tag1.getTagName());
            }
            api.addTags(tags);
            api.setLastUpdated(registry.get(artifactPath).getLastModified());

        } catch (GovernanceException e) {
            String msg = "Failed to get API fro artifact ";
            throw new APIManagementException(msg, e);
        } catch (RegistryException e) {
            String msg = "Failed to add Resource";
            throw new APIManagementException(msg, e);
        }
        return api;
    }

}

