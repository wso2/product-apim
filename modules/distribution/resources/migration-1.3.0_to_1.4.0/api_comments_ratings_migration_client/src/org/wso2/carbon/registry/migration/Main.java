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
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.Subscriber;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.Comment;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.registry.migration.utils.*;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;

import java.io.File;
import java.sql.SQLException;
import java.util.*;


public class Main {
    private static Registry registry;


    public static void main(String[] args) {
        CommandHandler.setInputs(args);
        try {
            String trustStore = System.getProperty("carbon.home") + File.separator + "repository" + File.separator +
                    "resources" + File.separator + "security" + File.separator + "wso2carbon.jks";
            System.setProperty("javax.net.ssl.trustStore", trustStore);
            System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");
            System.setProperty("carbon.repo.write.mode", "true");

            ConfigurationContext cc = ConfigurationContextFactory.createConfigurationContextFromFileSystem
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

    private static void migrate() throws RegistryException, SQLException, APIManagementException {
        Registry re = GovernanceUtils.getGovernanceUserRegistry(registry, CommandHandler.getUsername());
        GenericArtifactManager manager = new GenericArtifactManager(re, "api");
        GovernanceUtils.loadGovernanceArtifacts((UserRegistry) re);
        GenericArtifact[] artifacts = manager.getAllGenericArtifacts();

        Set<Subscriber> subscribers = new HashSet<Subscriber>();
        List<APIComment> apiCommentList = new ArrayList<APIComment>();
        subscribers = ApiDAO.getAllSubscribers(DBUtils.getConnection());

        for (GenericArtifact artifact : artifacts) {
            String providerName = artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER);
            String apiName = artifact.getAttribute(APIConstants.API_OVERVIEW_NAME);
            String apiVersion = artifact.getAttribute(APIConstants.API_OVERVIEW_VERSION);
            APIIdentifier identifier = new APIIdentifier(providerName, apiName, apiVersion);

            String path = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                    identifier.getProviderName() + RegistryConstants.PATH_SEPARATOR +
                    identifier.getApiName() + RegistryConstants.PATH_SEPARATOR + identifier.getVersion() +
                    RegistryConstants.PATH_SEPARATOR + APIConstants.API_KEY;

            String resourcePath = RegistryUtils.getAbsolutePath(RegistryContext.getBaseInstance(),
                    RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + path);

            int apiId = ApiDAO.getAPIID(identifier, DBUtils.getConnection());
            Comment[] comments = registry.getComments(resourcePath);

            /*
            Comments cannot be added to database while we are iterating in the following loop. Because, we need to sort the
            comments from the comment_id first.
             */
            for (Comment comment : comments) {
                APIComment apiComment = new APIComment(apiId, comment.getCommentID(), comment);
                apiCommentList.add(apiComment);
            }

            /*
            APIs can be rated without subscribing to an API. Therefore, we need to get the all subscribers in Store.
            Taking the Subscribers of APIs would not enough.
             */
            for (Subscriber subscriber : subscribers) {
                int rating = registry.getRating(resourcePath, subscriber.getName());
                if (rating != 0) {
                    ApiDAO.addRating(apiId, DBUtils.getConnection(), rating, subscriber.getId());
                }
            }
        }

        //First sort the comments by comment_id and add to database
        Collections.sort(apiCommentList, new APICommentIdComparator());
        for (APIComment comment : apiCommentList) {
            ApiDAO.addComment(DBUtils.getConnection(), comment.getCommentId(), comment.getCommentText(), comment.getCommentedUser(),
                    comment.getCreatedDate(), comment.getApiId());
        }
    }

}
