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

public class RegistryMigration {
    private static final Log log = LogFactory.getLog(RegistryMigration.class);


    public static void swagger12Migration(GenericArtifact[] artifacts, Registry registry, Tenant tenant) {
        for (GenericArtifact artifact : artifacts) {
            API api = getAPI(artifact);

            if (api != null) {
                APIIdentifier apiIdentifier = api.getId();
                try {
                    String apiDef11Path = APIUtil.getAPIDefinitionFilePath(apiIdentifier.getApiName(),
                            apiIdentifier.getVersion(),
                            apiIdentifier.getProviderName());

                    if (!registry.resourceExists(apiDef11Path)) {
                        log.error("Swagger 1.1 document does not exist for api " +
                                apiIdentifier.getApiName() + "-" + apiIdentifier.getVersion() + "-" + apiIdentifier.getProviderName() +
                                " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
                        continue;
                    }

                    Resource apiDef11 = registry.get(apiDef11Path);

                    String apiDef11Json = new String((byte[]) apiDef11.getContent(), "UTF8");

                    String swagger12location = ResourceUtil.getSwagger12ResourceLocation(apiIdentifier.getApiName(),
                            apiIdentifier.getVersion(),
                            apiIdentifier.getProviderName());

                    if(!registry.resourceExists(swagger12location)) {
                        log.error("Swagger 1.2 document does not exist for api " +
                                apiIdentifier.getApiName() + "-" + apiIdentifier.getVersion() + "-" + apiIdentifier.getProviderName() +
                                " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
                    } else {
                        String allResourceFile = swagger12location + RegistryConstants.PATH_SEPARATOR
                                + Constants.API_DOC_12_ALL_RESOURCES_DOC;
                        if(registry.resourceExists(allResourceFile)) {
                            //no need to update the swagger doc 1.2 with 1.1 doc because it has already happened
                            //remove parameters to suit AM 1.8
                            log.debug("Remove unwanted parameters from swagger 1.2 doc for api : " + apiIdentifier.getApiName() + "-" + apiIdentifier.getVersion() + "-" + apiIdentifier.getProviderName() +
                                                                                                                    " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
                            ResourceUtil.updateSwagger12ResourcesForAM18(new String[]{allResourceFile}, registry);
                        } else {
                            Resource swagger12Res = registry.get(swagger12location);
                            String[] resourcePaths = (String[]) swagger12Res.getContent();

                            log.debug("Updating swagger 1.2 resource using 1.1 for api : " + apiIdentifier.getApiName() + "-" + apiIdentifier.getVersion() + "-" + apiIdentifier.getProviderName() +
                                                                                                                    " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
                            ResourceUtil.updateAPISwaggerDocs(apiDef11Json, resourcePaths, registry);
                            //remove paramaters to suit AM 1.8
                            ResourceUtil.updateSwagger12ResourcesForAM18(resourcePaths, registry);
                            log.debug("Remove unwanted parameters from swagger 1.2 doc for api : " + apiIdentifier.getApiName() + "-" + apiIdentifier.getVersion() + "-" + apiIdentifier.getProviderName() +
                                                                                                                    " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
                        }
                    }
                } catch (ParseException e) {
                    log.error("ParseException thrown when migrating swagger for api " + api.getId().getApiName() + "-" + api.getId().getVersion() + "-" + api.getId().getProviderName() + " of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
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
