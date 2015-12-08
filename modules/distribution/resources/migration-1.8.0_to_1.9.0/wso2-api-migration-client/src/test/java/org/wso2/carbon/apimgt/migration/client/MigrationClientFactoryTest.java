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

import org.testng.Assert;
import org.testng.annotations.Test;
import org.mockito.Mockito;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.RegistryService;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;

public class MigrationClientFactoryTest {

    @Test
    public void testInitFactoryMigratingFrom18() throws Exception {
        RegistryService mockRegistryService = Mockito.mock(RegistryService.class);

        TenantManager mockTenantManager = Mockito.mock(TenantManager.class);
        Mockito.when(mockTenantManager.getAllTenants()).thenReturn(new Tenant[]{new Tenant()});

        MigrationClientFactory.initFactory(null, null, mockRegistryService, mockTenantManager);

        MigrationClient[] compatible18Clients = MigrationClientFactory.getAllClients(Constants.VERSION_1_8);

        Assert.assertEquals(compatible18Clients.length, 2);
        Assert.assertTrue(compatible18Clients[0] instanceof MigrateFrom18to19);
        Assert.assertTrue(compatible18Clients[1] instanceof MigrateFrom19to110);
    }


    @Test
    public void testInitFactoryMigratingFrom19() throws Exception {
        RegistryService mockRegistryService = Mockito.mock(RegistryService.class);

        TenantManager mockTenantManager = Mockito.mock(TenantManager.class);
        Mockito.when(mockTenantManager.getAllTenants()).thenReturn(new Tenant[]{new Tenant()});

        MigrationClientFactory.initFactory(null, null, mockRegistryService, mockTenantManager);

        MigrationClient[] compatible19Clients = MigrationClientFactory.getAllClients(Constants.VERSION_1_9);

        Assert.assertEquals(compatible19Clients.length, 1);
        Assert.assertTrue(compatible19Clients[0] instanceof MigrateFrom19to110);
    }


    @Test
    public void testInitFactoryMigratingFrom191() throws Exception {
        RegistryService mockRegistryService = Mockito.mock(RegistryService.class);

        TenantManager mockTenantManager = Mockito.mock(TenantManager.class);
        Mockito.when(mockTenantManager.getAllTenants()).thenReturn(new Tenant[]{new Tenant()});

        MigrationClientFactory.initFactory(null, null, mockRegistryService, mockTenantManager);

        MigrationClient[] compatible19Clients = MigrationClientFactory.getAllClients(Constants.VERSION_1_9_1);

        Assert.assertEquals(compatible19Clients.length, 1);
        Assert.assertTrue(compatible19Clients[0] instanceof MigrateFrom19to110);
    }


    @Test
    public void testInitFactoryInvalidVersion() throws Exception {
        RegistryService mockRegistryService = Mockito.mock(RegistryService.class);

        TenantManager mockTenantManager = Mockito.mock(TenantManager.class);
        Mockito.when(mockTenantManager.getAllTenants()).thenReturn(new Tenant[]{new Tenant()});

        MigrationClientFactory.initFactory(null, null, mockRegistryService, mockTenantManager);

        MigrationClient[] compatibleClients = MigrationClientFactory.getAllClients("1.0");

        Assert.assertEquals(compatibleClients.length, 0);
    }


    @Test
    public void testGetClient() throws Exception {
        RegistryService mockRegistryService = Mockito.mock(RegistryService.class);

        TenantManager mockTenantManager = Mockito.mock(TenantManager.class);
        Mockito.when(mockTenantManager.getAllTenants()).thenReturn(new Tenant[]{new Tenant()});

        MigrationClientFactory.initFactory(null, null, mockRegistryService, mockTenantManager);

        MigrationClient migrateTo18Client = MigrationClientFactory.getClient(Constants.VERSION_1_8);
        Assert.assertEquals(migrateTo18Client, null);

        MigrationClient migrateTo19Client = MigrationClientFactory.getClient(Constants.VERSION_1_9);
        Assert.assertTrue(migrateTo19Client instanceof MigrateFrom18to19);

        MigrationClient migrateTo191Client = MigrationClientFactory.getClient(Constants.VERSION_1_9_1);
        Assert.assertTrue(migrateTo191Client instanceof MigrateFrom18to19);

        MigrationClient migrateTo110Client = MigrationClientFactory.getClient(Constants.VERSION_1_10);
        Assert.assertTrue(migrateTo110Client instanceof MigrateFrom19to110);
    }
}