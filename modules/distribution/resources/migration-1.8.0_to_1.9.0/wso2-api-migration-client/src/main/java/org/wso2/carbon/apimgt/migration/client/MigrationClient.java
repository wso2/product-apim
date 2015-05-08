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

import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.impl.workflow.WorkflowException;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.user.api.UserStoreException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Public interface for all migrations
 *
 */
@SuppressWarnings("unused")
public interface MigrationClient {

    /**
     * This method is used to migrate database resources
     *
     * @throws APIManagementException
     */
    public void databaseMigration(String migrateVersion) throws APIManagementException;

    /**
     * This method is used to migrate all the registry resources
     * Swagger, RXTs and all other registry resources will be migrated
     *
     * @throws APIManagementException
     */
    public void registryResourceMigration() throws APIManagementException;


    /**
     *
     * @throws APIManagementException
     */
    public void fileSystemMigration() throws APIManagementException;


    /**
     * This method is used to clean old resources from registry
     *
     * @throws APIManagementException
     */
    public void cleanOldResources() throws APIManagementException;

}
