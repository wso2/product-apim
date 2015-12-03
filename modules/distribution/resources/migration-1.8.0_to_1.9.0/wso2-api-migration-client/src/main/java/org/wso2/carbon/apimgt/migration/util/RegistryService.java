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

import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;

public interface RegistryService {
    GenericArtifact[] getGenericAPIArtifacts(Tenant tenant);

    void updateGenericAPIArtifacts(Tenant tenant, GenericArtifact[] artifacts);

    API getAPI(GenericArtifact artifact);

    boolean isConfigRegistryResourceExists(Tenant tenant, final String registryLocation)
            throws UserStoreException, RegistryException;

    boolean isGovernanceRegistryResourceExists(Tenant tenant, final String registryLocation)
            throws UserStoreException, RegistryException;

    Object getConfigRegistryResource(Tenant tenant, final String registryLocation)
            throws UserStoreException, RegistryException;

    Object getGovernanceRegistryResource(Tenant tenant, final String registryLocation)
            throws UserStoreException, RegistryException;

    void addConfigRegistryResource(Tenant tenant, final String registryLocation, final String content, final String mediaType)
            throws UserStoreException, RegistryException;

    void addGovernanceRegistryResource(Tenant tenant, final String registryLocation, final String content, final String mediaType)
            throws UserStoreException, RegistryException;

    void updateConfigRegistryResource(Tenant tenant, final String registryLocation, final String content)
            throws UserStoreException, RegistryException;

    void updateGovernanceRegistryResource(Tenant tenant, final String registryLocation, final String content)
            throws UserStoreException, RegistryException;
}


