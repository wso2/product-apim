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

package org.wso2.carbon.apimgt.migration.client;

import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.RegistryService;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;

import java.util.ArrayList;
import java.util.HashMap;

public final class MigrationClientFactory {
    private static HashMap<String, Integer> nextClientVersionLocator = new HashMap<>();
    private static HashMap<String, Integer> clientLocator = new HashMap<>();
    private static ArrayList<MigrationClient> migrationClients = new ArrayList<>();

    public static void initFactory(String tenants, String blackListTenants, RegistryService registryService,
            TenantManager tenantManager, boolean removeDecryptionFailedKeysFromDB) throws UserStoreException {
        String[] version_1_8_x = new String[]{Constants.VERSION_1_8};
        String[] version_1_9_x = new String[]{Constants.VERSION_1_9, Constants.VERSION_1_9_1};
        String[] version_1_10_x = new String[]{Constants.VERSION_1_10};
        String[] version_2_0_x = new String[]{Constants.VERSION_2_0_0};

        MigrationClient migrateFrom18to19 = new MigrateFrom18to19(tenants, blackListTenants,
                                                                        registryService, tenantManager);
        registerClient(version_1_8_x, migrateFrom18to19, version_1_9_x);

        MigrationClient migrateFrom19to110 = new MigrateFrom19to110(tenants, blackListTenants, registryService,
                tenantManager, removeDecryptionFailedKeysFromDB);
        registerClient(version_1_9_x, migrateFrom19to110, version_1_10_x);

        MigrationClient migrateFrom1100to200 = new MigrateFrom110to200(tenants, blackListTenants, registryService,
                tenantManager, removeDecryptionFailedKeysFromDB);
        registerClient(version_1_10_x, migrateFrom1100to200, version_2_0_x);
    }

    public static void clearFactory() {
        nextClientVersionLocator.clear();
        clientLocator.clear();
        migrationClients.clear();
    }

    private static void registerClient(String[] migrateFromVersions, MigrationClient migrationClient,
                                       String[] migrateToVersions) {
        migrationClients.add(migrationClient);
        Integer index = migrationClients.size() - 1;

        for (String fromVersion : migrateFromVersions) {
            nextClientVersionLocator.put(fromVersion, index);
        }

        for (String toVersion : migrateToVersions) {
            clientLocator.put(toVersion, index);
        }
    }


    public static MigrationClient[] getAllClients(String migratingFromVersion) {
        Integer index = nextClientVersionLocator.get(migratingFromVersion);
        ArrayList<MigrationClient> requiredClients = new ArrayList<>();

        if (index != null) {
            for (int i = index; i < migrationClients.size(); ++i) {
                requiredClients.add(migrationClients.get(i));
            }
        }

        MigrationClient[] clients = new MigrationClient[requiredClients.size()];
        return requiredClients.toArray(clients);
    }


    public static MigrationClient getClient(String migratingToVersion) {
        Integer index = clientLocator.get(migratingToVersion);

        if (index != null) {
            return migrationClients.get(index);
        }

        return null;
    }
}
