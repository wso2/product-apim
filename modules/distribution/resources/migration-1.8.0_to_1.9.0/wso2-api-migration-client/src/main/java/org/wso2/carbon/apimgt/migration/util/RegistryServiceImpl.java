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
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.migration.client.internal.ServiceHolder;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;

import java.nio.charset.Charset;

public class RegistryServiceImpl implements RegistryService {
    private static final Log log = LogFactory.getLog(RegistryServiceImpl.class);

    @Override
    public GenericArtifact[] getGenericAPIArtifacts(Tenant tenant) {
        log.debug("Calling getGenericAPIArtifacts");
        GenericArtifact[] artifacts = null;

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);

            Registry registry = getGovernanceRegistry(tenant);
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);

            if (artifactManager != null) {
                artifacts = artifactManager.getAllGenericArtifacts();

                log.debug("Total number of api artifacts : " + artifacts.length);
            } else {
                log.debug("No api artifacts found in registry for tenant " + tenant.getId() + "(" + tenant.getDomain() + ")");
            }

        } catch (RegistryException e) {
            log.error("Error occurred when getting GenericArtifacts from registry", e);
        } catch (UserStoreException e) {
            log.error("Error occurred while reading tenant information of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
        } catch (APIManagementException e) {
            log.error("Failed to initialize GenericArtifactManager", e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

        return artifacts;
    }

    @Override
    public void updateGenericAPIArtifacts(Tenant tenant, GenericArtifact[] artifacts) {
        log.debug("Calling updateGenericAPIArtifacts");

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);

            Registry registry = getGovernanceRegistry(tenant);
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);

            for (GenericArtifact artifact : artifacts) {
                artifactManager.updateGenericArtifact(artifact);
            }

        } catch (UserStoreException e) {
            log.error("Error occurred while reading tenant information of tenant " + tenant.getId() + "(" + tenant.getDomain() + ")", e);
        } catch (RegistryException e) {
            log.error("Error occurred when updating GenericArtifacts in registry", e);
        } catch (APIManagementException e) {
            log.error("Failed to initialize GenericArtifactManager", e);
        }
        finally {
            PrivilegedCarbonContext.endTenantFlow();
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
    public boolean isConfigRegistryResourceExists(Tenant tenant, String registryLocation) throws UserStoreException, RegistryException {
        boolean isExists = false;
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);

            Registry registry = getConfigRegistry(tenant);
            isExists = registry.resourceExists(registryLocation);
        }
        finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        return isExists;
    }

    @Override
    public boolean isGovernanceRegistryResourceExists(Tenant tenant, String registryLocation) throws UserStoreException, RegistryException {
        boolean isExists = false;
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);

            Registry registry = getGovernanceRegistry(tenant);
            isExists = registry.resourceExists(registryLocation);
        }
        finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        return isExists;
    }

    @Override
    public Object getConfigRegistryResource(Tenant tenant, final String registryLocation) throws UserStoreException, RegistryException {
        Object content = null;

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);

            Registry registry = getConfigRegistry(tenant);

            if (registry.resourceExists(registryLocation)) {
                Resource resource = registry.get(registryLocation);
                content = resource.getContent();
            }
        }
        finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

        return content;
    }

    @Override
    public Object getGovernanceRegistryResource(Tenant tenant, final String registryLocation) throws UserStoreException, RegistryException {
        Object content = null;

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);

            Registry registry = getGovernanceRegistry(tenant);

            if (registry.resourceExists(registryLocation)) {
                Resource resource = registry.get(registryLocation);
                content = resource.getContent();
            }
        }
        finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

        return content;
    }

    @Override
    public void addConfigRegistryResource(Tenant tenant, final String registryLocation, final String content,
                                          final String mediaType) throws UserStoreException, RegistryException {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);

            Registry registry = getConfigRegistry(tenant);

            Resource resource = registry.newResource();
            resource.setContent(content);
            resource.setMediaType(mediaType);
            registry.put(content, resource);
        }
        finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    @Override
    public void addGovernanceRegistryResource(Tenant tenant, final String registryLocation, final String content,
                                              final String mediaType) throws UserStoreException, RegistryException {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);

            Registry registry = getGovernanceRegistry(tenant);

            Resource resource = registry.newResource();
            resource.setContent(content);
            resource.setMediaType(mediaType);
            registry.put(content, resource);
        }
        finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    @Override
    public void updateConfigRegistryResource(Tenant tenant, final String registryLocation, final String content)
                                                                        throws UserStoreException, RegistryException {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);

            Registry registry = getConfigRegistry(tenant);

            Resource resource = registry.get(registryLocation);
            resource.setContent(content);
            registry.put(registryLocation, resource);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }


    @Override
    public void updateGovernanceRegistryResource(Tenant tenant, final String registryLocation, final String content)
                                                                        throws UserStoreException, RegistryException {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenant.getDomain(), true);

            Registry registry = getGovernanceRegistry(tenant);

            Resource resource = registry.get(registryLocation);
            resource.setContent(content);
            registry.put(registryLocation, resource);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    private Registry getConfigRegistry(Tenant tenant) throws UserStoreException, RegistryException {
        String adminName = ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).
                getRealmConfiguration().getAdminUserName();
        log.debug("Tenant admin username : " + adminName);
        ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
        return ServiceHolder.getRegistryService().getConfigUserRegistry(adminName, tenant.getId());
    }


    private Registry getGovernanceRegistry(Tenant tenant) throws UserStoreException, RegistryException {
        String adminName = ServiceHolder.getRealmService().getTenantUserRealm(tenant.getId()).
                getRealmConfiguration().getAdminUserName();
        log.debug("Tenant admin username : " + adminName);
        ServiceHolder.getTenantRegLoader().loadTenantRegistry(tenant.getId());
        return ServiceHolder.getRegistryService().getGovernanceUserRegistry(adminName, tenant.getId());
    }
}
