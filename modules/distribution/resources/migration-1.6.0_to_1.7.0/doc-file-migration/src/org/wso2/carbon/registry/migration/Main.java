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
import org.wso2.carbon.registry.core.*;
import org.wso2.carbon.apimgt.api.model.*;


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
            String apiResourcePath = APIUtil.getAPIPath(apiIdentfier);
            try{
                Association[] docAssociations = re.getAssociations(apiResourcePath,
                        "document");               
                for (Association association : docAssociations) {
                    String docPath = association.getDestinationPath();

                    Resource docResource = re.get(docPath);
                    GenericArtifactManager docArtifactManager = new GenericArtifactManager(re,
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
                            if(filePath.split(RegistryConstants.PATH_SEPARATOR+"files"+RegistryConstants.PATH_SEPARATOR)[1].indexOf(".")==-1){                           
                            Resource resource=re.get(filePath);
                            resource.setMediaType("text/plain");
                            re.put(filePath,resource);
                            }else if(filePath.split(RegistryConstants.PATH_SEPARATOR+"files"+RegistryConstants.PATH_SEPARATOR)[1].indexOf(".wsdl")>-1){ 
                             String resourcePath="_system"+ RegistryConstants.PATH_SEPARATOR + "governance"+filePath;     
                            
                            if (registry.resourceExists(resourcePath)) {                                                 
                            Resource resource=re.get(filePath);
                            resource.setMediaType("application/api-wsdl");
                            re.put(filePath,resource);
                            }
                            }
                            
                            re.copy(filePath,filePath);
                            re.addAssociation(docArtifact.getPath(), filePath,
                                    APIConstants.DOCUMENTATION_FILE_ASSOCIATION);
                        }
                    }
                } }catch (RegistryException e) {
                String msg = "Failed to add the document file type association ";
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
