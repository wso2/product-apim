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
import java.io.StringWriter;
import java.sql.SQLException;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.json.simple.JSONObject;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.governance.api.common.dataobjects.GovernanceArtifact;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.migration.utils.CommandHandler;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;


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
            Main.migrate();
            System.out.println("Finished API data migration process.");
            System.exit(0);
        } catch (RegistryException e) {
            System.out.println("Error while connecting to registry." + e);
        } catch (Exception e) {
            System.out.println("Error while migrating data." + e);
        }

    }

    private static void migrate() throws RegistryException, APIManagementException, SQLException, IOException {
        Registry re = GovernanceUtils.getGovernanceUserRegistry(registry, CommandHandler.getUsername());
        GenericArtifactManager manager = new GenericArtifactManager(re, "api");
        GovernanceUtils.loadGovernanceArtifacts((UserRegistry) re);
        GenericArtifact[] artifacts = manager.getAllGenericArtifacts();

        for (GenericArtifact artifact : artifacts) {
        	API api = getAPI(artifact, re);
        	
        	JSONObject endpoint_config = new JSONObject();
        	
        	JSONObject production_config = new JSONObject();
        	production_config.put("url", api.getUrl());
        	
        	JSONObject sandbox_config = new JSONObject();
        	sandbox_config.put("url", api.getSandboxUrl());
        	
        	if (api.getUrl() != null && !api.getUrl().equals("")) {
        		endpoint_config.put("production_endpoints", production_config);
        	}
        	
        	if (api.getSandboxUrl() != null && !api.getSandboxUrl().equals("")) {
        		endpoint_config.put("sandbox_endpoints", sandbox_config);
        	}
        	endpoint_config.put("endpoint_type", "http");
        	
        	StringWriter out = new StringWriter();
        	endpoint_config.writeJSONString(out);
        	String endpoint_config_text = out.toString();
        	artifact.setAttribute("overview_endpointConfig", endpoint_config_text);
			manager.updateGenericArtifact(artifact);
		}
     
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
            

        } catch (GovernanceException e) {
            String msg = "Failed to get API fro artifact ";
            throw new APIManagementException(msg, e);
        } 
        return api;
    }
        
}

