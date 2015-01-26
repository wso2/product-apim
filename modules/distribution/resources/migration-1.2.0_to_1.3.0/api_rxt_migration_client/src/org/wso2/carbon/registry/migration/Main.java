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

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.wso2.carbon.apimgt.api.*;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;


import org.wso2.carbon.registry.migration.utils.APIDao;
import org.wso2.carbon.registry.migration.utils.DBUtils;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;
import org.wso2.carbon.registry.migration.utils.CommandHandler;
import org.wso2.carbon.apimgt.api.model.*;

import java.io.File;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;


public class Main {
    private static Registry registry;


    public static void main(String[] args) {
        CommandHandler.setInputs(args);
        try {
            String trustStore = System.getProperty("carbon.home") + File.separator + "repository" + File.separator + "resources" + File.separator + "security" + File.separator + "wso2carbon.jks";
            System.setProperty("javax.net.ssl.trustStore", trustStore);
            System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");
            System.setProperty("carbon.repo.write.mode", "true");

            ConfigurationContext cc = ConfigurationContextFactory.createConfigurationContextFromFileSystem(System.getProperty("carbon.home") + "repository/deployment/client", System.getProperty("carbon.home") + "repository/conf/axis2/axis2_client.xml");
            registry = new WSRegistryServiceClient(CommandHandler.getServiceURL(), CommandHandler.getUsername(), CommandHandler.getPassword(), cc);
            DBUtils.initializeDB();
            Main.migrate();
            System.out.println("Finished API data migration process.");
            System.exit(0);
        } catch (RegistryException e) {
            System.out.println("Error while connecting to registry." + e);
            return;
        } catch (Exception e) {
            System.out.println("Error while migrating data." + e);
            return;
        }

    }

    private static int migrate() throws RegistryException, SQLException, APIManagementException {

        Registry re = GovernanceUtils.getGovernanceUserRegistry(registry, CommandHandler.getUsername());
        GenericArtifactManager manager = new GenericArtifactManager(re, "api");
        GovernanceUtils.loadGovernanceArtifacts((UserRegistry) re);
        GenericArtifact[] artifacts = manager.getAllGenericArtifacts();
        for (GenericArtifact artifact : artifacts) {
            String[] attributes = artifact.getAttributes("uriTemplates_entry");
            if (attributes != null) {
                String apiName;
                artifact.removeAttribute("uriTemplates_entry");
                Set<URITemplate> uriTemplates = new LinkedHashSet<URITemplate>();
                for (int i = 0; i < attributes.length; i++) {
                    String attribute = attributes[i];
                    String[] atd = attribute.split(":");
                    String[] httpVerbs = atd[0].split("\\s");
                    if (httpVerbs.length > 0) {
                        for (int j = 0; j < httpVerbs.length; j++) {
                            URITemplate template = new URITemplate();
                            artifact.setAttribute("uriTemplates_urlPattern" + j, atd[1]);
                            template.setUriTemplate(atd[1]);
                            artifact.setAttribute("uriTemplates_httpVerb" + j, httpVerbs[j]);
                            template.setHTTPVerb(httpVerbs[j]);
                            artifact.setAttribute("uriTemplates_authType" + j, "Any");
                            template.setAuthType("Any");
                            uriTemplates.add(template);
                        }

                    } else {
                        URITemplate template = new URITemplate();
                        artifact.setAttribute("uriTemplates_urlPattern" + i, atd[1]);
                        template.setUriTemplate(atd[1]);
                        artifact.setAttribute("uriTemplates_httpVerb" + i, atd[0]);
                        template.setHTTPVerb(atd[0]);
                        artifact.setAttribute("uriTemplates_authType" + i, "Any");
                        template.setAuthType("Any");
                        uriTemplates.add(template);
                    }
                }
                manager.updateGenericArtifact(artifact);
                String providerName = artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER);
                apiName = artifact.getAttribute(APIConstants.API_OVERVIEW_NAME);
                String apiVersion = artifact.getAttribute(APIConstants.API_OVERVIEW_VERSION);
                APIIdentifier apiId = new APIIdentifier(providerName, apiName, apiVersion);
                int id = APIDao.getAPIID(apiId, DBUtils.getConnection());
                if (!APIDao.isURLMappingsExists(id, DBUtils.getConnection())) {
                    APIDao.addURLTemplates(id, uriTemplates, DBUtils.getConnection());
                }
                System.out.println("Successfully migrate the API resources data for the API :" + apiName);


            } else {
                System.out.println("Already the API resources data has been migrated.");

            }
        }
        return -1;

    }


}
