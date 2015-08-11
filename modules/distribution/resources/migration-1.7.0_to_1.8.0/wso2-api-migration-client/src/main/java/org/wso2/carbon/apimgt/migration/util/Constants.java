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

package org.wso2.carbon.apimgt.migration.util;

public class Constants {
    public static final String APIM_PRODUCT_NAME = "WSO2 API Manager";
    public static final String PREVIOUS_VERSION = "1.7.0";
    public static final String VERSION_1_8 = "1.8.0";
    public static final String LINE_BREAK = "\\n";
    public static final String CONSTRAINT = "constraint";
    public static final String API = "api";
    public static final String MIGRATION_SCRIPTS_FOLDER = "migration-scripts";

    // Migration client argument property names
    public static final String ARG_MIGRATE_TO_VERSION = "migrateToVersion";
    public static final String ARG_MIGRATE_TENANTS = "tenants";
    public static final String ARG_MIGRATE_ALL = "migrate";

    // Synapse configuration related
    public static final String SYNAPSE_API_ROOT_ELEMENT = "api";
    public static final String SYNAPSE_API_ATTRIBUTE_VERSION = "version";

    /*
	 * Variables in api_doc.json version 1.1
	 */

    public static final String API_DOC_11_BASE_PATH = "basePath";
    public static final String API_DOC_11_RESOURCE_PATH = "resourcePath";
    public static final String API_DOC_11_API_VERSION = "apiVersion";
    public static final String API_DOC_11_APIS = "apis";
    public static final String API_DOC_11_PATH = "path";
    public static final String API_DOC_11_OPERATIONS = "operations";
    public static final String API_DOC_11_METHOD = "httpMethod";
    public static final String API_DOC_11_PARAMETERS = "parameters";

    /*
	 * Variables in api_doc.json version 1.2
	 */

    public static final String API_DOC_12_BASE_PATH = "basePath";
    public static final String API_DOC_12_APIS = "apis";
    public static final String API_DOC_12_PATH = "path";
    public static final String API_DOC_12_OPERATIONS = "operations";
    public static final String API_DOC_12_METHOD = "method";
    public static final String API_DOC_12_PARAMETERS = "parameters";
    public static final String API_DOC_12_NICKNAME = "nickname";
    //this resource contains all the resources. This is introduced to swagger 1.2 location to hold
    //all the resource during the migration of 1.6. AM 1.6 does not have resources seperated by name
    //so all resources are in one doc
    public static final String API_DOC_12_ALL_RESOURCES_DOC = "resources";

    /**
     * default parameter array
     */
    public static final String DEFAULT_PARAM_ARRAY = "[ " + "  { "
            + "    \"dataType\": \"String\", "
            + "    \"description\": \"Access Token\", "
            + "    \"name\": \"Authorization\", "
            + "    \"allowMultiple\": false, "
            + "    \"required\": true, "
            + "    \"paramType\": \"header\" " + "  }, "
            + "  { "
            + "    \"description\": \"Request Body\", "
            + "    \"name\": \"body\", "
            + "    \"allowMultiple\": false, "
            + "    \"type\": \"string\", "
            + "    \"required\": true, "
            + "    \"paramType\": \"body\" " + "  } "
            + "]";

    public static final String DEFAULT_PARAM_FOR_URL_TEMPLATE = " { "
            + "\"name\": \"\", "
            + "\"allowMultiple\": false, "
            + "\"required\": true, "
            + "\"type\": \"string\", "
            + "\"paramType\": \"path\" "
            + "}";


    //database types
    public static final String DB_TYPE_ORACLE = "oracle";
    public static final String DB_TYPE_DB2 = "db2";
    public static final String DB_TYPE_MYSQL= "mysql";
    public static final String DB_TYPE_MSSQL = "mssql";
    public static final String DB_TYPE_POSTGRE = "postgre";
    public static final String DB_TYPE_OPENEDGE = "openedge";
}
