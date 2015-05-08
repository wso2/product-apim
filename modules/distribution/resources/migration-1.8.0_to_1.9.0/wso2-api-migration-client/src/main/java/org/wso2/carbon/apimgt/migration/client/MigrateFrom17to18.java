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
import org.json.simple.parser.ParseException;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.migration.client.internal.ServiceHolder;
import org.wso2.carbon.apimgt.migration.client.util.Constants;
import org.wso2.carbon.apimgt.migration.client.util.ResourceUtil;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

public class MigrateFrom17to18 implements MigrationClient {

    private static final Log log = LogFactory.getLog(MigrateFrom17to18.class);

    @Override
    public void databaseMigration(String migrateVersion) throws APIManagementException {
        log.info("Database migration for API Manager 1.8.0 started");
        try {
            String queryToExecute = ResourceUtil.pickQueryFromResources(migrateVersion);

            Connection connection = APIMgtDBUtil.getConnection();
            connection.setAutoCommit(false);

            PreparedStatement preparedStatement = connection.prepareStatement(queryToExecute);
            boolean isUpdated = preparedStatement.execute();
            if (isUpdated) {
                connection.commit();
            } else {
                connection.rollback();
            }
            preparedStatement.close();

            if (log.isDebugEnabled()) {
                log.debug("Query " + queryToExecute + " executed ");
            }
            connection.close();

        } catch (SQLException e) {
            ResourceUtil.handleException(e.getMessage());
        } catch (IOException e) {
            ResourceUtil.handleException(e.getMessage());
        }
        log.info("DB resource migration done for all the tenants");
    }

    @Override
    public void registryResourceMigration() throws APIManagementException {
        swaggerResourceMigration();
    }

    @Override
    public void fileSystemMigration() {
        log.info("No files for 1.7.0 to 1.8.0 migration");
    }

    @Override
    public void cleanOldResources() {

    }


    public void swaggerResourceMigration() throws APIManagementException {
        log.info("Swagger migration for API Manager 1.8.0 started");

        TenantManager tenantManager = ServiceHolder.getRealmService().getTenantManager();
        try {
            Tenant[] tenantsArray = tenantManager.getAllTenants();
            if (log.isDebugEnabled()) {
                log.debug("Tenant array loaded successfully");
            }

            // Add  super tenant to the tenant array
            Tenant[] allTenantsArray = Arrays.copyOf(tenantsArray, tenantsArray.length + 1);
            org.wso2.carbon.user.core.tenant.Tenant superTenant = new org.wso2.carbon.user.core.tenant.Tenant();
            superTenant.setId(MultitenantConstants.SUPER_TENANT_ID);
            superTenant.setDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            allTenantsArray[allTenantsArray.length - 1] = superTenant;
            if (log.isDebugEnabled()) {
                log.debug("Super tenant added to the tenant array");
            }

            for (Tenant tenant : allTenantsArray) {
                log.info("Swagger migration for tenant " + tenant.getDomain() + "[" + tenant.getId() + "]" + " ");

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
                    API api;

                    api = APIUtil.getAPI(artifact, registry);

                    //API api = APIUtil.getAPI(artifact);
                    APIIdentifier apiIdentifier = api.getId();

                    String apiName = apiIdentifier.getApiName();
                    String apiVersion = apiIdentifier.getVersion();
                    String apiProviderName = apiIdentifier.getProviderName();

                    String apiDef11Path = APIUtil.getAPIDefinitionFilePath(apiName, apiVersion, apiProviderName);
                    Resource apiDef11 = registry.get(apiDef11Path);

                    String apiDef11Json = new String((byte[]) apiDef11.getContent());

                    String swagger12location = ResourceUtil.getSwagger12ResourceLocation(apiName, apiVersion, apiProviderName);

                    //Check for swagger v1.2 file
                    if (!registry.resourceExists(swagger12location)) {
                        log.error("Swagger Resource migration has not happen yet for " + apiName + "-" + apiVersion + "-" + apiProviderName +
                                ". Please run -Dmigrate=" + Constants.VERSION_1_8 + " first");

                    } else {
                        String allResourceFile = swagger12location + RegistryConstants.PATH_SEPARATOR + Constants.API_DOC_12_ALL_RESOURCES_DOC;
                        //Check whether swagger resource is available at swagger v1.2 location
                        if (registry.resourceExists(allResourceFile)) {
                            //no need to update the swagger doc 1.2 with 1.1 doc because it has already happened
                            //remove parameters to work with AM 1.8
                            log.info("Modifying resources to work with API Manage 1.8.0 : " + apiName + "-" + apiVersion + "-" + apiProviderName);
                            ResourceUtil.updateSwagger12ResourcesForAM18(new String[]{allResourceFile}, registry);
                        } else {
                            Resource swagger12Res = registry.get(swagger12location);
                            String[] resourcePaths = (String[]) swagger12Res.getContent();

                            log.info("Updating resource for : " + apiName + "-" + apiVersion + "-" + apiProviderName);
                            ResourceUtil.updateAPISwaggerDocs(apiDef11Json, resourcePaths, registry);
                            //remove parameters to work with AM 1.8
                            ResourceUtil.updateSwagger12ResourcesForAM18(resourcePaths, registry);
                            log.info("Modify resources for AM 1.8 : " + apiName + "-" + apiVersion + "-" + apiProviderName);
                        }


                    }
                }
            }
        } catch (ParseException e) {
            ResourceUtil.handleException(e.getMessage());
        } catch (GovernanceException e) {
            ResourceUtil.handleException(e.getMessage());
        } catch (UserStoreException e) {
            ResourceUtil.handleException(e.getMessage());
        } catch (RegistryException e) {
            ResourceUtil.handleException(e.getMessage());
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

    }
}
