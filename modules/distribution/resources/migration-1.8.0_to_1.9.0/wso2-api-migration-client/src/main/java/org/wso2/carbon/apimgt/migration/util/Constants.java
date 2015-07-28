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

    public static final String VERSION_1_7 = "1.7";
    public static final String VERSION_1_8 = "1.8";
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


    //Swagger v2.0 constants
    public static final String SWAGGER_X_SCOPE = "x-scope";
    public static final String SWAGGER_X_AUTH_TYPE = "x-auth-type";
    public static final String SWAGGER_X_THROTTLING_TIER = "x-throttling-tier";
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


    //database types
    public static final String DB_TYPE_ORACLE = "oracle";
    public static final String DB_TYPE_DB2 = "db2";
    public static final String DB_TYPE_MYSQL= "mysql";
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
}
