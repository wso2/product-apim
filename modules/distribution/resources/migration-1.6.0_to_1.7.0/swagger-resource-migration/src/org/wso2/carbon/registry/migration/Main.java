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
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
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
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.migration.utils.CommandHandler;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceStub;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceResourceServiceExceptionException;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;


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
                    (System.getProperty("carbon.home") + File.separator + "repository" + File.separator + "deployment" + File.separator + "client", 
		     System.getProperty("carbon.home") + File.separator + "repository" + File.separator + "conf" + File.separator + "axis2" + File.separator + "axis2_client.xml");
            registry = new WSRegistryServiceClient(CommandHandler.getServiceURL(), CommandHandler.getUsername(),
                    CommandHandler.getPassword(), cc);
            Main.migrate();
            System.out.println("Finished API data migration process.");
            System.exit(0);
        } catch (RegistryException e) {
            System.out.println("Error while connecting to registry." + e);
        } catch (Exception e) {
            System.out.println("Error while migrating data." + e);
        }

    }

    private static void migrate() throws RegistryException, APIManagementException, LoginAuthenticationExceptionException, 
    										ResourceAdminServiceResourceServiceExceptionException, SQLException, IOException {
        Registry re = GovernanceUtils.getGovernanceUserRegistry(registry, CommandHandler.getUsername());
        GenericArtifactManager manager = new GenericArtifactManager(re, "api");
        GovernanceUtils.loadGovernanceArtifacts((UserRegistry) re);
        GenericArtifact[] artifacts = manager.getAllGenericArtifacts();

        for (GenericArtifact artifact : artifacts) {
        	API api = getAPI(artifact, re);
        	APIIdentifier apiIdentfier = api.getId();
        	
		artifact.setAttribute(APIConstants.PROTOTYPE_OVERVIEW_IMPLEMENTATION, APIConstants.IMPLEMENTATION_TYPE_ENDPOINT);
		manager.updateGenericArtifact(artifact);

        	String apiDefinitionFilePath = getAPIDefinitionFilePath(apiIdentfier.getApiName(), apiIdentfier.getVersion(), apiIdentfier.getProviderName());
            /*apiDefinitionFilePath = RegistryConstants.PATH_SEPARATOR + "registry"
                    + RegistryConstants.PATH_SEPARATOR + "resource"
                    + RegistryConstants.PATH_SEPARATOR + "_system"
                    + RegistryConstants.PATH_SEPARATOR + "governance"
                    + apiDefinitionFilePath;*/
            
            Resource resource = re.get(apiDefinitionFilePath);
            String text = new String ((byte[]) resource.getContent());
            
            String newContentPath = APIUtil.getAPIDefinitionFilePath(apiIdentfier.getApiName(), apiIdentfier.getVersion(),apiIdentfier.getProviderName());
        	
        	try {
                Resource docContent = re.newResource();
                docContent.setContent(text);
                docContent.setMediaType("text/plain");
                re.put(newContentPath, docContent);
                
                String visibleRolesList = api.getVisibleRoles();
                String[] visibleRoles = new String[0];
                if (visibleRolesList != null) {
                    visibleRoles = visibleRolesList.split(",");
                }
                String sessionCookie = login(CommandHandler.getUsername(),
                        CommandHandler.getPassword(), CommandHandler.getHost());
        		//String permissionString = "ra^false:rd^false:wa^false:wd^false:da^false:dd^false:aa^false:ad^false";
        		ResourceAdminServiceStub stub = new ResourceAdminServiceStub(CommandHandler.getServiceURL() + "ResourceAdminService");
        		
        		ServiceClient client = stub._getServiceClient();
        		Options option = client.getOptions();
                option.setManageSession(true);
                option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, sessionCookie);
        		stub.addRolePermission("_system/governance" + newContentPath, APIConstants.ANONYMOUS_ROLE, "2", "1");      
        	} catch (RegistryException e) {
                String msg = "Failed to add the API Definition content of : "
                             + APIConstants.API_DEFINITION_DOC_NAME + " of API :" + apiIdentfier.getApiName();
                throw new APIManagementException(msg, e);
            } 
        	
        	
		}
     
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
    
    private static String getAPIDefinitionFilePath(String apiName, String apiVersion,String apiProvider) {
    	String resourcePath = APIConstants.API_DOC_LOCATION + RegistryConstants.PATH_SEPARATOR + 
		apiName +"-"  + apiVersion + RegistryConstants.PATH_SEPARATOR + APIConstants.API_DOC_RESOURCE_NAME;

    	return resourcePath;
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
        
}
