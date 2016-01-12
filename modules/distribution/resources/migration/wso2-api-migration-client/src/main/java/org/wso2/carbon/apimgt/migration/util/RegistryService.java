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

import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;

import javax.xml.stream.XMLStreamException;

import java.io.FileNotFoundException;

public interface RegistryService {
    void startTenantFlow(Tenant tenant);

    void endTenantFlow();

    void rollbackGovernanceRegistryTransaction() throws UserStoreException, RegistryException;

    void rollbackConfigRegistryTransaction() throws UserStoreException, RegistryException;

    void addDefaultLifecycles() throws RegistryException, UserStoreException, FileNotFoundException, XMLStreamException;

    GenericArtifact[] getGenericAPIArtifacts();

    void updateGenericAPIArtifacts(GenericArtifact[] artifacts);

    API getAPI(GenericArtifact artifact);

    String getGenericArtifactPath(GenericArtifact artifact) throws UserStoreException, RegistryException;

    boolean isConfigRegistryResourceExists(final String registryLocation)
            throws UserStoreException, RegistryException;

    boolean isGovernanceRegistryResourceExists(final String registryLocation)
            throws UserStoreException, RegistryException;

    Object getConfigRegistryResource(final String registryLocation)
            throws UserStoreException, RegistryException;

    Object getGovernanceRegistryResource(final String registryLocation)
            throws UserStoreException, RegistryException;

    void addConfigRegistryResource(final String registryLocation, final String content, final String mediaType)
            throws UserStoreException, RegistryException;

    void addGovernanceRegistryResource(final String registryLocation, final String content, final String mediaType)
            throws UserStoreException, RegistryException;

    void updateConfigRegistryResource(final String registryLocation, final String content)
            throws UserStoreException, RegistryException;

    void updateGovernanceRegistryResource(final String registryLocation, final String content)
            throws UserStoreException, RegistryException;
    
    void updateRXTResource(final String rxtName, final String content) throws UserStoreException, RegistryException;

    void setGovernanceRegistryResourcePermissions(String visibility, String[] roles,
                                                  String resourcePath) throws APIManagementException;
}


