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


import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.is.migration.client.MigrateFrom5to510;
import org.wso2.carbon.is.migration.util.Constants;
import org.wso2.carbon.user.api.UserStoreException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.SQLException;

public class MigrateFrom19to110 implements MigrationClient {

    private static final Log log = LogFactory.getLog(MigrateFrom19to110.class);

    @Override
    public void databaseMigration(String migrateVersion) throws APIMigrationException, SQLException {
        /*There are no changes in APIM tables, but there are changes in IDN tables, So use the database migration
        /method defined in IS Migration Client */
        MigrateFrom5to510 migrateFrom5to510;
        try {
            migrateFrom5to510 = new MigrateFrom5to510();
            migrateFrom5to510.databaseMigration(Constants.VERSION_5_1_0);
        } catch (UserStoreException e) {
            //Errors logged to let user know the state of the db migration and continue other resource migrations
            log.error("Error occurred while migrating identity databases from IS 5.0.0 to IS 5.1.0", e);
        } catch (Exception e) {
            //Errors logged to let user know the state of the db migration and continue other resource migrations
            log.error("Error occurred while migrating identity databases from IS 5.0.0 to IS 5.1.0", e);
        }
    }

    @Override
    public void registryResourceMigration() throws APIMigrationException {

    }

    @Override
    public void fileSystemMigration() throws APIMigrationException {

    }

    @Override
    public void cleanOldResources() throws APIMigrationException {

    }

    @Override
    public void statsMigration() throws APIMigrationException {

    }
}
