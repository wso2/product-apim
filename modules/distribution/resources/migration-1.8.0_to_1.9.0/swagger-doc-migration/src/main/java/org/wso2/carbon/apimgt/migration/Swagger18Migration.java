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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.URITemplate;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
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
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Swagger18Migration {
    private static final Log log = LogFactory.getLog(Swagger18Migration.class);


    public void migrate() throws UserStoreException, InterruptedException {
        log.info("*** In migrate() of Swagger18Migration ***");

        TenantManager tenantManager = ServiceHolder.getRealmService().getTenantManager();
        Tenant[] tenantsArray = tenantManager.getAllTenants();

        // Add  super tenant to the tenant array
        Tenant[] allTenantsArray = Arrays.copyOf(tenantsArray, tenantsArray.length + 1);
        org.wso2.carbon.user.core.tenant.Tenant superTenant = new org.wso2.carbon.user.core.tenant.Tenant();
        superTenant.setId(MultitenantConstants.SUPER_TENANT_ID);
        superTenant.setDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        allTenantsArray[allTenantsArray.length - 1] = superTenant;

        for (Tenant tenant : allTenantsArray) {
            log.info("*** Swagger 1.8 migration for tenant " + tenant.getDomain() + "[" + tenant.getId() + "]" + " ***");
            try {
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
					try {
						api = getAPI(artifact, registry);
					
	                    //API api = APIUtil.getAPI(artifact);
	                    APIIdentifier apiIdentfier = api.getId();
	
	                    String apiDef11Path = APIUtil.getAPIDefinitionFilePath(apiIdentfier.getApiName(),
	                                                                           apiIdentfier.getVersion(),
	                                                                           apiIdentfier.getProviderName());
	
	                    Resource apiDef11 = registry.get(apiDef11Path);
	
	                    String apiDef11Json = new String((byte[]) apiDef11.getContent());
	
	                    String swagger12location = ResourceUtil.getSwagger12ResourceLocation(apiIdentfier.getApiName(),
	                                                                            apiIdentfier.getVersion(),
	                                                                            apiIdentfier.getProviderName());
	
	                    //log.info("swagger12location => " + swagger12location);                    
	                    
	                    if(!registry.resourceExists(swagger12location)) {
	                        log.error("Swagger Resource migration has not happen yet for " +
	                        		apiIdentfier.getApiName() + "-" + apiIdentfier.getVersion() + "-" + apiIdentfier.getProviderName() + 
	                        		". Please run -D" + Constants.VERSION_1_6 + " first");
	                        
	                    } else {
	                    	
	                    	//check whether there is a migration from 1.6 to 1.7 has happen. There will be
	                        //a resource file 'Constants.API_DOC_12_ALL_RESOURCES_DOC' will be there. 
	                        
	                        String allResourceFile = swagger12location + RegistryConstants.PATH_SEPARATOR 
	                        											+ Constants.API_DOC_12_ALL_RESOURCES_DOC;
	                        if(registry.resourceExists(allResourceFile)) {
	                        	//no need to update the swagger doc 1.2 with 1.1 doc because it has already happened
	                        	//remove paramaters to suit AM 1.8
	                        	log.info("Modify resources for AM 1.8 : " + apiIdentfier.getApiName() + "-" + apiIdentfier.getVersion() + "-" + apiIdentfier.getProviderName());
	                            ResourceUtil.updateSwagger12ResourcesForAM18(new String[]{allResourceFile}, registry);
	                        } else {
	                           Resource swagger12Res = registry.get(swagger12location);
	                           String[] resourcePaths = (String[]) swagger12Res.getContent();
	
	                           log.info("Updating resource for : " + apiIdentfier.getApiName() + "-" + apiIdentfier.getVersion() + "-" + apiIdentfier.getProviderName());
	                           ResourceUtil.updateAPISwaggerDocs(apiDef11Json, resourcePaths, registry);
	                           //remove paramaters to suit AM 1.8
	                           ResourceUtil.updateSwagger12ResourcesForAM18(resourcePaths, registry);
	                           log.info("Modify resources for AM 1.8 : " + apiIdentfier.getApiName() + "-" + apiIdentfier.getVersion() + "-" + apiIdentfier.getProviderName());
	                        }
	                    } 
	                    
	                    
					} catch (APIManagementException e) {
						log.error("APIManagementException while migrating api in " + tenant.getDomain() , e);
					} catch (RegistryException e) {
						log.error("RegistryException while getting api resource for " + tenant.getDomain() , e);
					} catch (ParseException e) {
						log.error("Error while parsing json resource for " + tenant.getDomain() , e);
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
            api.setContext(artifact.getAttribute(APIConstants.API_OVERVIEW_CONTEXT));
            api.setDescription(artifact.getAttribute(APIConstants.API_OVERVIEW_DESCRIPTION));

            ArrayList<URITemplate> urlPatternsList = ApiMgtDAO.getAllURITemplates(api.getContext(), api.getId().getVersion());
            Set<URITemplate> uriTemplates = new HashSet<URITemplate>(urlPatternsList);
            for (URITemplate uriTemplate : uriTemplates) {
                uriTemplate.setResourceURI(api.getUrl());
                uriTemplate.setResourceSandboxURI(api.getSandboxUrl());
            }
            api.setUriTemplates(uriTemplates);


        } catch (GovernanceException e) {
            String msg = "Failed to get API fro artifact ";
            throw new APIManagementException(msg, e);
        }
        return api;
    }

 
}