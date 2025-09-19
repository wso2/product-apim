/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.test;

public class Constants {

    public static final String CHAR_AT = "@";

    public static final String MIGRATION_PROFILE = "migration";
    public static final String DEFAULT_PROFILE = "default";

    public static final String GATEWAY_ENVIRONMENT = "GATEWAY_ENVIRONMENT";

    public static final String SHARED_DATABASE_TYPE = "SHARED_DATABASE_TYPE";
    public static final String SHARED_DATABASE_DRIVER = "SHARED_DATABASE_DRIVER";
    public static final String SHARED_DATABASE_URL = "SHARED_DATABASE_URL";
    public static final String SHARED_DATABASE_USERNAME = "SHARED_DATABASE_USERNAME";
    public static final String SHARED_DATABASE_PASSWORD = "SHARED_DATABASE_PASSWORD";
    public static final String SHARED_DATABASE_VALIDATION_QUERY = "SHARED_DATABASE_VALIDATION_QUERY";

    public static final String API_MANAGER_DATABASE_TYPE = "API_MANAGER_DATABASE_TYPE";
    public static final String API_MANAGER_DATABASE_DRIVER = "API_MANAGER_DATABASE_DRIVER";
    public static final String API_MANAGER_DATABASE_URL = "API_MANAGER_DATABASE_URL";
    public static final String API_MANAGER_DATABASE_USERNAME = "API_MANAGER_DATABASE_USERNAME";
    public static final String API_MANAGER_DATABASE_PASSWORD = "API_MANAGER_DATABASE_PASSWORD";
    public static final String API_MANAGER_DATABASE_VALIDATION_QUERY = "API_MANAGER_DATABASE_VALIDATION_QUERY";


     public static final String APIM_CONTAINER_USER_HOME = "/home/wso2carbon";
    public static final String DEPLOYMENT_TOML_PATH = "/repository/conf/deployment.toml";

    public static final String DEFAULT_APIM_API_DEPLOYER = "api/am/publisher/v4/";
    public static final String DEFAULT_DEVPORTAL = "api/am/devportal/v3/";
    public static final String DEFAULT_APIM_TOKEN_EP = "oauth2/token";
    public static final String DEFAULT_DCR_EP = "client-registration/v0.17/register";

    public static class REQUEST_HEADERS {

        public static final String HOST = "Host";
        public static final String AUTHORIZATION = "Authorization";
        public static final String CONTENT_TYPE = "Content-Type";
    }

    public static class CONTENT_TYPES {

        public static final String APPLICATION_JSON = "application/json";
        public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
        public static final String MULTIPART_FORM_DATA = "multipart/form-data";
        public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
        public static final String APPLICATION_ZIP = "application/zip";
        public static final String TEXT_PLAIN = "text/plain";
        public static final String APPLICATION_CA_CERT = "application/x-x509-ca-cert";
    }
    }
