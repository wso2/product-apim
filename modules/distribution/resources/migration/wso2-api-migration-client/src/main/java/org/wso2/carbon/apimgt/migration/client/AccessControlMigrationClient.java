/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.apimgt.migration.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.migration.APIMigrationException;
import org.wso2.carbon.registry.migration.util.RegistryService;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;

public class AccessControlMigrationClient extends MigrationClientBase implements MigrationClient {
    private static final Log log = LogFactory.getLog(AccessControlMigrationClient.class);
    private RegistryService registryService;

    public AccessControlMigrationClient(String tenantArguments, String blackListTenantArguments, String tenantRange,
                                        RegistryService registryService, TenantManager tenantManager) throws UserStoreException {
        super(tenantArguments, blackListTenantArguments, tenantRange, tenantManager);
        this.registryService = registryService;
    }

    /**
     * This method is used to migrate all registry resources
     *
     * @throws APIMigrationException
     */
    @Override
    public void registryResourceMigration() throws APIMigrationException {
        updateAPAIArtifactsForAccessControl();
    }

    /**
     * This method is used to update the API artifacts in the registry to Publisher Access Control feature.
     *
     * @throws APIMigrationException
     */
    private void updateAPAIArtifactsForAccessControl() throws APIMigrationException {
        log.info("Updating API artifacts for Publisher Access Control started.");

        for (Tenant tenant : getTenantsArray()) {
            try {
                registryService.startTenantFlow(tenant);

                log.debug("Updating APIs for tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');

                GenericArtifact[] artifacts = registryService.getGenericAPIArtifacts();
                for (GenericArtifact artifact : artifacts) {
                    String path = artifact.getPath();
                    if (registryService.isGovernanceRegistryResourceExists(path)) {
                        Object apiResource = registryService.getGovernanceRegistryResource(path);
                        if (apiResource == null) {
                            continue;
                        }
                        registryService.updateGenericAPIArtifactsForAccessControl(path, artifact);
                    }
                }
                log.info("End Updating API artifacts tenant " + tenant.getId() + '(' + tenant.getDomain() + ')');
            } catch (GovernanceException e) {
                log.error("Error when accessing API artifact in registry for tenant " + tenant.getId() + '(' +
                        tenant.getDomain() + ')', e);
            } catch (RegistryException e) {
                log.error("Error while updating API artifact in the registry for tenant " + tenant.getId() + '(' +
                        tenant.getDomain() + ')', e);
            } catch (UserStoreException e) {
                log.error("Error while updating the API artifact in the registry for tenant " + tenant.getId() + '(' +
                        tenant.getDomain() + ')', e);
            } finally {
                registryService.endTenantFlow();
            }
        }
        log.info("Updating API artifacts done for all the tenants for Publisher Access Control feature.");
    }
}
