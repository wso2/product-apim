/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.apimgt.migration.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.migration.client.internal.ServiceHolder;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.governance.lcm.util.CommonUtil;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.realm.RegistryAuthorizationManager;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;

import javax.xml.stream.XMLStreamException;

import java.io.FileNotFoundException;
import java.nio.charset.Charset;


public class RegistryServiceImpl implements RegistryService {
    private static final Log log = LogFactory.getLog(RegistryServiceImpl.class);
    private Tenant tenant = null;
    private APIProvider apiProvider = null;

    @Override
    public void startTenantFlow(Tenant tenant) {
        if (this.tenant != null) {
            log.error("Start tenant flow called without ending previous tenant flow");
            throw new IllegalStateException("Previous tenant flow has not been ended, " +
                                                "'RegistryService.endTenantFlow()' needs to be called");
        }
        else {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenant.getId(), true);
            this.tenant = tenant;
        }
    }

    @Override
    public void endTenantFlow() {
        if (this.tenant == null) {
            log.error("End tenant flow called even though tenant flow has already been ended or was not started");
            throw new IllegalStateException("Previous tenant flow has already been ended, " +
                    "unnecessary additional RegistryService.endTenantFlow()' call has been detected");
        }
        else {
            PrivilegedCarbonContext.endTenantFlow();
            this.tenant = null;
            this.apiProvider = null;
        }
    }

    @Override
    public void rollbackGovernanceRegistryTransaction() throws UserStoreException, RegistryException {
        getGovernanceRegistry().rollbackTransaction();
    }

    @Override
    public void rollbackConfigRegistryTransaction() throws UserStoreException, RegistryException {
        getConfigRegistry().rollbackTransaction();
    }

    @Override
    public void addDefaultLifecycles() throws RegistryException, UserStoreException, FileNotFoundException, XMLStreamException {
        CommonUtil.addDefaultLifecyclesIfNotAvailable(getConfigRegistry(), CommonUtil.getRootSystemRegistry(tenant.getId()));
    }

    @Override
    public GenericArtifact[] getGenericAPIArtifacts() {
        log.debug("Calling getGenericAPIArtifacts");
        GenericArtifact[] artifacts = {};

        try {
            Registry registry = getGovernanceRegistry();
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);

            if (artifactManager != null) {
                artifacts = artifactManager.getAllGenericArtifacts();

                log.debug("Total number of api artifacts : " + artifacts.length);
            } else {
                log.debug("No api artifacts found in registry for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            }

        } catch (RegistryException e) {
            log.error("Error occurred when getting GenericArtifacts from registry", e);
        } catch (UserStoreException e) {
            log.error("Error occurred while reading tenant information of tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        } catch (APIManagementException e) {
            log.error("Failed to initialize GenericArtifactManager", e);
        }

        return artifacts;
    }

    @Override
    public void updateGenericAPIArtifacts(GenericArtifact[] artifacts) {
        log.debug("Calling updateGenericAPIArtifacts");

        try {
            Registry registry = getGovernanceRegistry();
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);

            for (GenericArtifact artifact : artifacts) {
                artifactManager.updateGenericArtifact(artifact);
            }

        } catch (UserStoreException e) {
            log.error("Error occurred while reading tenant information of tenant " + tenant.getId() + '(' + tenant.getDomain() + ')', e);
        } catch (RegistryException e) {
            log.error("Error occurred when updating GenericArtifacts in registry", e);
        } catch (APIManagementException e) {
            log.error("Failed to initialize GenericArtifactManager", e);
        }
    }

    @Override
    public API getAPI(GenericArtifact artifact) {
        log.debug("Calling getAPI");
        API api = null;

        try {
            api = APIUtil.getAPI(artifact);
        } catch (APIManagementException e) {
            log.error("Error when getting api artifact " + artifact.getId() + " from registry", e);
        }

        return api;
    }

    @Override
    public String getGenericArtifactPath(GenericArtifact artifact) throws UserStoreException, RegistryException {
        return GovernanceUtils.getArtifactPath(getGovernanceRegistry(), artifact.getId());
    }

    @Override
    public boolean isConfigRegistryResourceExists(String registryLocation) throws UserStoreException, RegistryException {
        return getConfigRegistry().resourceExists(registryLocation);
    }

    @Override
    public boolean isGovernanceRegistryResourceExists(String registryLocation) throws UserStoreException, RegistryException {
        return getGovernanceRegistry().resourceExists(registryLocation);
    }

    @Override
    public Object getConfigRegistryResource(final String registryLocation) throws UserStoreException, RegistryException {
        Object content = null;

        Registry registry = getConfigRegistry();

        if (registry.resourceExists(registryLocation)) {
            Resource resource = registry.get(registryLocation);
            content = resource.getContent();
        }

        return content;
    }

    @Override
    public Object getGovernanceRegistryResource(final String registryLocation) throws UserStoreException, RegistryException {
        Object content = null;

        Registry registry = getGovernanceRegistry();

        if (registry.resourceExists(registryLocation)) {
            Resource resource = registry.get(registryLocation);
            content = resource.getContent();
        }

        return content;
    }

    @Override
    public void addConfigRegistryResource(final String registryLocation, final String content,
                                          final String mediaType) throws UserStoreException, RegistryException {
        Registry registry = getConfigRegistry();

        Resource resource = registry.newResource();
        resource.setContent(content);
        resource.setMediaType(mediaType);
        registry.put(registryLocation, resource);
    }

    @Override
    public void addGovernanceRegistryResource(final String registryLocation, final String content,
                                              final String mediaType) throws UserStoreException, RegistryException {
        Registry registry = getGovernanceRegistry();

        Resource resource = registry.newResource();
        resource.setContent(content);
        resource.setMediaType(mediaType);
        registry.put(registryLocation, resource);
    }

    @Override
    public void updateConfigRegistryResource(final String registryLocation, final String content)
                                                                        throws UserStoreException, RegistryException {
        Registry registry = getConfigRegistry();

        Resource resource = registry.get(registryLocation);
        resource.setContent(content);
        registry.put(registryLocation, resource);
    }


    @Override
    public void updateGovernanceRegistryResource(final String registryLocation, final String content)
                                                                        throws UserStoreException, RegistryException {
        Registry registry = getGovernanceRegistry();

        Resource resource = registry.get(registryLocation);
        resource.setContent(content);
        registry.put(registryLocation, resource);
    }

    @Override
    public void setGovernanceRegistryResourcePermissions(String visibility, String[] roles,
                                                                String resourcePath) throws APIManagementException {
        initAPIProvider();
        APIUtil.setResourcePermissions(tenant.getAdminName(), visibility, roles, resourcePath);
    }

    private void initAPIProvider() throws APIManagementException {
        if (apiProvider == null) {
            apiProvider = APIManagerFactory.getInstance().getAPIProvider(tenant.getAdminName());
        }
    }


    private Registry getConfigRegistry() throws UserStoreException, RegistryException {
        if (tenant == null) {
            throw new IllegalStateException("The tenant flow has not been started, " +
                        "'RegistryService.startTenantFlow(Tenant tenant)' needs to be called");
        }

        String adminName = ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).
                getRealmConfiguration().getAdminUserName();
        log.debug("Tenant admin username : " + adminName);
        ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
        return ServiceHolder.getRegistryService().getConfigUserRegistry(adminName, tenant.getId());
    }


    private Registry getGovernanceRegistry() throws UserStoreException, RegistryException {
        if (tenant == null) {
            throw new IllegalStateException("The tenant flow has not been started, " +
                    "'RegistryService.startTenantFlow(Tenant tenant)' needs to be called");
        }

        String adminName = ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).
                getRealmConfiguration().getAdminUserName();
        log.debug("Tenant admin username : " + adminName);
        ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
        return ServiceHolder.getRegistryService().getGovernanceUserRegistry(adminName, tenant.getId());
    }

    /* 
     * Update the RXT file in the registry 
     * 
     */
    @Override
    public void updateRXTResource(String rxtName, final String rxtPayload) throws UserStoreException, RegistryException {
        if (tenant == null) {
            throw new IllegalStateException("The tenant flow has not been started, "
                                            + "'RegistryService.startTenantFlow(Tenant tenant)' needs to be called");
        }
        ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());

        Registry registry = ServiceHolder.getRegistryService().getGovernanceSystemRegistry(tenant.getId());

        //Update RXT resource
        String resourcePath = Constants.RXT_REG_PATH + RegistryConstants.PATH_SEPARATOR + rxtName;       
        
        // This is "registry" is a governance registry instance, therefore
        // calculate the relative path to governance.
        String govRelativePath = RegistryUtils.getRelativePathToOriginal(resourcePath,
                                          APIUtil.getMountedPath(RegistryContext.getBaseInstance(),
                                                                     RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH));
        // calculate resource path
        RegistryAuthorizationManager authorizationManager = new RegistryAuthorizationManager(
                                                                                ServiceReferenceHolder.getUserRealm());
        resourcePath = authorizationManager.computePathOnMount(resourcePath);
        org.wso2.carbon.user.api.AuthorizationManager authManager = ServiceReferenceHolder.getInstance()
                                                                                          .getRealmService()
                                                                                          .getTenantUserRealm(tenant.getId())
                                                                                          .getAuthorizationManager();

        if (registry.resourceExists(govRelativePath)) {     
            Resource resource = registry.get(govRelativePath);
            resource.setContent(rxtPayload.getBytes(Charset.defaultCharset()));
            resource.setMediaType(APIConstants.RXT_MEDIA_TYPE);
            registry.put(govRelativePath, resource); 
            authManager.authorizeRole(APIConstants.ANONYMOUS_ROLE, resourcePath, ActionConstants.GET);
        }                
        
        //Update RXT UI Configuration
        Registry configRegistry = ServiceHolder.getRegistryService().getConfigSystemRegistry(tenant.getId());
        String rxtUIConfigPath = Constants.GOVERNANCE_ARTIFACT_CONFIGURATION_PATH + APIConstants.API_KEY;
        if(configRegistry.resourceExists(rxtUIConfigPath)) {
            Resource rxtUIResource = configRegistry.get(rxtUIConfigPath);
            rxtUIResource.setContent(ResourceUtil.getArtifactUIContentFromConfig(rxtPayload));
            configRegistry.put(rxtUIConfigPath, rxtUIResource);
            }

    }
    
    
}
