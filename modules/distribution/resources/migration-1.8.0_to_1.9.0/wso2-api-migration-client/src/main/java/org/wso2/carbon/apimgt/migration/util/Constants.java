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

    public static final String VERSION_1_9 = "1.9";
    public static final String LINE_BREAK = "\\n";
    public static final String CONSTRAINT = "constraint";
    public static final String ALTER = "alter";
    public static final String DELIMITER = ";";
    public static final String API = "api";

    public static final String GOVERNANCE_COMPONENT_REGISTRY_LOCATION = "/repository/components/org.wso2.carbon" +
            ".governance";
    public static final String RXT_PATH = "/repository/resources/rxts/api.rxt";
    public static final String DATA_SOURCE_NAME = "DataSourceName";

    // Migration client argument property names
    public static final String ARG_MIGRATE_TO_VERSION = "migrateToVersion";
    public static final String ARG_MIGRATE_TENANTS = "tenants";
    public static final String ARG_MIGRATE_BLACKLIST_TENANTS = "blackListed";
    public static final String ARG_MIGRATE_ALL = "migrate";
    public static final String ARG_CLEANUP = "cleanup";
    public static final String ARG_MIGRATE_DB = "migrateDB";
    public static final String ARG_MIGRATE_REG = "migrateReg";
    public static final String ARG_MIGRATE_FILE_SYSTEM = "migrateFS";
    public static final String ARG_MIGRATE_STATS = "migrateStats";

    // Synapse configuration related
    public static final String SYNAPSE_API_ROOT_ELEMENT = "api";
    public static final String SYNAPSE_API_ATTRIBUTE_CONTEXT = "context";
    public static final String SYNAPSE_API_ATTRIBUTE_VERSION = "version";
    public static final String SYNAPSE_API_ATTRIBUTE_VERSION_TYPE = "version-type";
    public static final String SYNAPSE_API_ATTRIBUTE_SOURCE = "source";
    public static final String SYNAPSE_API_ATTRIBUTE_NAME = "name";
    public static final String SYNAPSE_API_ATTRIBUTE_EXPRESSION = "expression";
    public static final String SYNAPSE_API_ATTRIBUTE_CLASS = "class";
    public static final String SYNAPSE_API_ATTRIBUTE_VALUE = "value";
    public static final String SYNAPSE_API_ATTRIBUTE_XMLNS = "xmlns";
    public static final String SYNAPSE_API_ELEMENT_PROPERTY = "property";
    public static final String SYNAPSE_API_ELEMENT_FILTER = "filter";
    public static final String SYNAPSE_API_ELEMENT_CLASS = "class";
    public static final String SYNAPSE_API_ELEMENT_INSEQUENCE = "inSequence";
    public static final String SYNAPSE_API_ELEMENT_OUTSEQUENCE = "outSequence";
    public static final String SYNAPSE_API_ELEMENT_HANDLERS = "handlers";
    public static final String SYNAPSE_API_ELEMENT_HANDLER = "handler";
    public static final String SYNAPSE_API_ELEMENT_THEN = "then";
    public static final String SYNAPSE_API_ELEMENT_SEND = "send";
    public static final String SYNAPSE_API_VALUE_BACKEND_REQUEST_TIME = "api.ut.backendRequestTime";
    public static final String SYNAPSE_API_VALUE_AM_KEY_TYPE = "$ctx:AM_KEY_TYPE";
    public static final String SYNAPSE_API_VALUE_EXPRESSION = "get-property('SYSTEM_TIME')";
    public static final String SYNAPSE_API_VALUE_RESPONSE_HANDLER = "org.wso2.carbon.apimgt.usage.publisher.APIMgtResponseHandler";
    public static final String SYNAPSE_API_VALUE_CORS_HANDLER = "org.wso2.carbon.apimgt.gateway.handlers.security.CORSRequestHandler";
    public static final String SYNAPSE_API_VALUE_INLINE = "inline";
    public static final String SYNAPSE_API_VALUE_INLINE_UPPERCASE = "INLINE";
    public static final String SYNAPSE_API_VALUE_ENPOINT = "ENDPOINT";
    public static final String SYNAPSE_API_VALUE_VERSION_TYPE_URL = "url";
    public static final String SYNAPSE_API_VALUE_VERSION_TYPE_CONTEXT = "context";
    public static final String SYNAPSE_API_XMLNS = "http://ws.apache.org/ns/synapse";


    //Swagger v2.0 constants
    public static final String SWAGGER_X_SCOPE = "x-scope";
    public static final String SWAGGER_X_AUTH_TYPE = "x-auth-type";
    public static final String SWAGGER_X_THROTTLING_TIER = "x-throttling-tier";
    public static final String SWAGGER_AUTH_TYPE = "auth_type";
    public static final String SWAGGER_THROTTLING_TIER = "throttling_tier";
    public static final String SWAGGER_X_MEDIATION_SCRIPT = "x-mediation-script";
    public static final String SWAGGER_X_WSO2_SECURITY = "x-wso2-security";
    public static final String SWAGGER_X_WSO2_SCOPES = "x-wso2-scopes";
    public static final String SWAGGER_SCOPE_KEY = "key";
    public static final String SWAGGER_NAME = "name";
    public static final String SWAGGER_DESCRIPTION = "description";
    public static final String SWAGGER_ROLES = "roles";
    public static final String SWAGGER_TITLE = "title";
    public static final String SWAGGER_EMAIL = "email";
    public static final String SWAGGER_URL = "url";
    public static final String SWAGGER_CONTACT = "contact";
    public static final String SWAGGER_LICENCE = "license";
    public static final String SWAGGER_LICENCE_URL = "licenseUrl";
    public static final String SWAGGER_VER = "version";
    public static final String SWAGGER_OBJECT_NAME_APIM = "apim";
    public static final String SWAGGER_PATHS = "paths";
    public static final String SWAGGER_RESPONSES = "responses";
    public static final String SWAGGER = "swagger";
    public static final String SWAGGER_V2 = "2.0";
    public static final String SWAGGER_INFO = "info";
    public static final String SWAGGER_REQUIRED_PARAM = "required";
    public static final String SWAGGER_HOST = "host";
    public static final String SWAGGER_BASE_PATH = "basePath";
    public static final String SWAGGER_SCHEMES = "schemes";
    public static final String SWAGGER_AUTHORIZATIONS = "authorizations";
    public static final String SWAGGER_SECURITY_DEFINITIONS = "securityDefinitions";
    public static final String SWAGGER_SCOPES = "scopes";
    public static final String SWAGGER_TERMS_OF_SERVICE = "termsOfService";
    public static final String SWAGGER_TERMS_OF_SERVICE_URL = "termsOfServiceUrl";
    public static final String SWAGGER_PARAM_TYPE_IN = "in";
    public static final String SWAGGER_OPERATION_ID = "operationId";
    public static final String SWAGGER_PARAMETERS = "parameters";
    public static final String SWAGGER_SUMMARY = "summary";
    public static final String SWAGGER_RESPONSE_200 = "200";
    public static final String SWAGGER_PARAM_TYPE = "type";
    public static final String SWAGGER_PARAM_TYPE_BODY = "body";
    public static final String SWAGGER_BODY_SCHEMA = "schema";
    public static final String SWAGGER_DEFINITIONS = "definitions";
    public static final String SWAGGER_REF = "$ref";
    public static final String SWAGGER_SAMPLE_DEFINITION = "sampleItem";





    //database types
    public static final String DB_TYPE_ORACLE = "oracle";
    public static final String DB_TYPE_DB2 = "db2";
    public static final String DB_TYPE_MYSQL = "mysql";
    public static final String DB_TYPE_MSSQL = "mssql";
    public static final String DB_TYPE_POSTGRE = "postgre";
    public static final String DB_TYPE_OPENEDGE = "openedge";


    //default Swagger v2 response parameter
    public static final String DEFAULT_RESPONSE = "{ " +
            "\"200\": "
            + "{ " +
            "\"description\": \"No response was specified\"} "
            + "}";

    public static final String DEFAULT_SECURITY_SCHEME = "{" +
            "\"x-wso2-scopes\" : \"\", " +
            "\"type\" : \"\", " +
            "\"description\" : \"\", " +
            "\"name\" : \"\"," +
            " \"in\" : \"\", " +
            "\"flow\" : \"\", " +
            "\"authorizationUrl\" : \"\", " +
            "\"tokenUrl\" : \"\", " +
            "\"scopes\" : \"\"}";

    public static final String DEFAULT_INFO = "{" +
            "\"title\" : \"\", " +
            "\"version\" : \"\"" +
            "}";


    public static final String EXTERNAL_API_STORE = "ExternalAPIStore";
    public static final String ATTRIBUTE_CLASSNAME = "className";
    public static final String API_PUBLISHER_CLASSNAME = "org.wso2.carbon.apimgt.impl.publishers.WSO2APIPublisher";



}
