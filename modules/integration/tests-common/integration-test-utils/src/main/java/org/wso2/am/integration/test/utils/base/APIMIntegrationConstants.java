/*
*Copyright (c) 2015​, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.am.integration.test.utils.base;

/**
 * define constants that are used in APIM integration tests
 */
public class APIMIntegrationConstants {

    //instance names
    public static final String AM_PRODUCT_GROUP_NAME = "APIM";
    public static final String AM_STORE_INSTANCE = "store-old";
    public static final String AM_PUBLISHER_INSTANCE = "publisher-old";
    public static final String AM_GATEWAY_MGT_INSTANCE = "gateway-mgt";
    public static final String AM_GATEWAY_WRK_INSTANCE = "gateway-wrk";
    public static final String AM_KEY_MANAGER_INSTANCE = "keyManager";
    public static final String BACKEND_SERVER_INSTANCE = "backend-server";
    public static final String REST_API_VERSION = "v0.16";

    //Response element names
    public static final String API_RESPONSE_ELEMENT_NAME_ERROR = "error";
    public static final String API_RESPONSE_ELEMENT_NAME_SUBSCRIPTION = "subscriptions";
    public static final String API_RESPONSE_ELEMENT_NAME_APPLICATIONS = "applications";
    public static final String API_RESPONSE_ELEMENT_NAME_API_NAME = "name";
    public static final String API_RESPONSE_ELEMENT_NAME_API_VERSION = "version";
    public static final String API_RESPONSE_ELEMENT_NAME_API_PROVIDER = "provider";
    public static final String API_RESPONSE_ELEMENT_NAME_APIS = "apis";
    public static final String API_RESPONSE_ELEMENT_NAME_ID = "id";

    public static final String OAUTH_DEFAULT_APPLICATION_NAME = "DefaultApplication";

    public static final String IS_API_EXISTS = "\"isApiExists\":true";
    public static final String IS_WEB_APP_EXISTS = "\"isWebAppExists\":true";
    public static final String IS_API_BLOCKED = "API blocked";
    public static final String IS_API_NOT_EXISTS = "\"isApiExists\":false";
    public static final String JAXRS_BASIC_WEB_APP_NAME = "jaxrs_basic";
    public static final String AM_MONITORING_WEB_APP_NAME = "APIStatusMonitor";
    public static final String GRAPHQL_API_WEB_APP_NAME = "am-graphQL-sample";
    public static final String AUDIT_API_WEB_APP_NAME = "am-auditApi-sample";
    public static final String PRODEP1_WEB_APP_NAME = "name-checkOne";
    public static final String PRODEP2_WEB_APP_NAME = "name-checkTwo";
    public static final String PRODEP3_WEB_APP_NAME = "name-checkThree";
    public static final String SANDBOXEP1_WEB_APP_NAME = "name-check1_SB";
    public static final String SANDBOXEP2_WEB_APP_NAME = "name-check2_SB";
    public static final String SANDBOXEP3_WEB_APP_NAME = "name-check3_SB";
    public static final String BPMN_PROCESS_ENGINE_WEB_APP_NAME = "BPMNProcessServerApp-1.0.0";
    public static final String ETCD_WEB_APP_NAME = "etcdmock";
    public static final String WILDCARD_WEB_APP_NAME = "wildcard";

    public static final String RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER = "Application & Application User";
    public static final String RESOURCE_AUTH_TYPE_APPLICATION = "Application";
    public static final String RESOURCE_AUTH_TYPE_APPLICATION_USER = "Application User";
    public static final String RESOURCE_AUTH_TYPE_NONE = "None";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String HTTP_VERB_GET = "GET";
    public static final String HTTP_VERB_POST = "POST";

    public static final String API_DOCUMENT_TYPE_HOW_TO = "How To";
    public static final String API_DOCUMENT_SOURCE_INLINE = "Inline";

    public static final String APPLICATION_JSON_MEDIA_TYPE = "application/json";

    public static final String STORE_APPLICATION_REST_URL = "store/site/pages/applications.jag";

    public static final String SECONDARY_USER_STORE = "secondary";
    public static final String REST_API_PUBLISHER_CONTEXT_FULL = "api/am/publisher/v2";
    public static final String REST_API_PUBLISHER_EXPORT_API_RESOURCE = "/apis/export";
    public static final String REST_API_PUBLISHER_IMPORT_API_RESOURCE = "/apis/import";

    public static final class APPLICATION_TIER {
        public static final String UNLIMITED = "Unlimited";
        public static final String LARGE = "Large";
        public static final String MEDIUM = "Medium";
        public static final String SMALL = "Small";
        public static final String TEN_PER_MIN = "10PerMin";

        public static final int LARGE_LIMIT = 20;
        public static final int MEDIUM_LIMIT = 5;
        public static final int SMALL_LIMIT = 1;

        public static final String DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN = "50PerMin";
    }

    public static class API_TIER {
        public static final String UNLIMITED = "Unlimited";
        public static final String GOLD = "Gold";
        public static final String SILVER = "Silver";
        public static final String BRONZE = "Bronze";
        public static final String ASYNC_UNLIMITED = "AsyncUnlimited";
        public static final String ASYNC_WH_UNLIMITED = "AsyncWHUnlimited";

        public static final int GOLD_LIMIT = 20;
        public static final int SILVER_LIMIT = 5;
        public static final int BRONZE_LIMIT = 1;

    }

    public static class GRANT_TYPE {
        public static final String PASSWORD = "password";
        public static final String CLIENT_CREDENTIAL = "client_credentials";
        public static final String AUTHORIZATION_CODE = "authorization_code";
        public static final String REFRESH_CODE = "refresh_token";
        public static final String SAML2 = "urn:ietf:params:oauth:grant-type:saml2-bearer";
        public static final String NTLM = "iwa:ntlm";
        public static final String IMPLICIT = "implicit";
        public static final String JWT = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    }

    public static class RESOURCE_TIER {
        public static final String UNLIMITED = "Unlimited";
        public static final String TENK_PER_MIN = "10KPerMin";
        public static final String TWENTYK_PER_MIN = "20KPerMin";
        public static final String FIFTYK_PER_MIN = "50KPerMin";

        public static final int ULTIMATE_LIMIT = 20;
        public static final int PLUS_LIMIT = 5;
        public static final int BASIC_LIMIT = 1;
    }

    public static final String REST_API_ADMIN_CONTEXT = "api/am/admin/";
    public static final String REST_API_ADMIN_VERSION = "v0.17";
    public static final String REST_API_ADMIN_CONTEXT_FULL_0 = REST_API_ADMIN_CONTEXT + REST_API_ADMIN_VERSION;
    public static final String REST_API_ADMIN_IMPORT_API_RESOURCE = "/import/api";
    public static final String REST_API_ADMIN_EXPORT_API_RESOURCE = "/export/api";
    public static final String REST_API_ADMIN_API_CATEGORIES_RESOURCE = "/api-categories";

    public enum ResourceAuthTypes {
        APPLICATION_USER("Application_User"), NONE("None"), APPLICATION("Application"),
        APPLICATION_AND_APPLICATION_USER("Any");

        private String authType;

        ResourceAuthTypes(String authType) {
            this.authType = authType;
        }

        public String getAuthType() {
            return authType;
        }
    }

    public static final String SUPER_TENANT_DOMAIN = "carbon.super";
    public static final String DEFAULT_TOKEN_VALIDITY_TIME = "3600";

    public static final class APIM_INTERNAL_ROLE {
        public static final String SUBSCRIBER = "Internal/subscriber";
        public static final String PUBLISHER = "Internal/publisher";
        public static final String CREATOR = "Internal/creator";
        public static final String EVERYONE = "Internal/everyone";
    }

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin";
    public static final String LOCAL_HOST_NAME = "localhost";

    public static final String WSO2_GATEWAY_VENDOR = "wso2";
}