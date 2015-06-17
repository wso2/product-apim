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
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.util.ResourceUtil;
import org.wso2.carbon.utils.dbcreator.DatabaseCreator;

import javax.sql.DataSource;
import java.io.File;
import java.sql.SQLException;

public class MigrationDBCreator extends DatabaseCreator {

    private static final Log log = LogFactory.getLog(MigrationDBCreator.class);
    private DataSource dataSource;

    public MigrationDBCreator(DataSource dataSource) {
        super(dataSource);
        this.dataSource = dataSource;
    }

    @Override
    protected String getDbScriptLocation(String databaseType) {
        String scriptName = databaseType + ".sql";
        String resourcePath = "/18to19Migration/sql-scripts/";
        if (log.isDebugEnabled()) {
            log.debug("Loading database script :" + scriptName);
        }
        return resourcePath + scriptName;
    }

    @Override
    public void createRegistryDatabase() throws SQLException, APIMigrationException {
        String databaseType;
        try {
            databaseType = DatabaseCreator.getDatabaseType(this.dataSource.getConnection());

            String scriptPath = getDbScriptLocation(databaseType);
            File scriptFile = new File(scriptPath);
            if (scriptFile.exists()) {
                super.createRegistryDatabase();
            } else {
                log.error("API Migration client cannot find the database script.");
            }
        } catch (Exception e) {
            ResourceUtil.handleException("Error occurred while accessing the database connection", e);
        }
    }
}
