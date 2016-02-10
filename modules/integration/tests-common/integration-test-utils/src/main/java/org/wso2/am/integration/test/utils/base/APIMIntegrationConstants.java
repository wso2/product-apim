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
    public static final String AM_STORE_INSTANCE = "store";
    public static final String AM_PUBLISHER_INSTANCE = "publisher";
    public static final String AM_GATEWAY_MGT_INSTANCE = "gateway-mgt";
    public static final String AM_GATEWAY_WRK_INSTANCE = "gateway-wrk";
    public static final String AM_KEY_MANAGER_INSTANCE = "keyManager";
    public static final String BACKEND_SERVER_INSTANCE = "backend-server";

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
    public static final String PRODEP1_WEB_APP_NAME = "name-checkOne";
    public static final String PRODEP2_WEB_APP_NAME = "name-checkTwo";
    public static final String PRODEP3_WEB_APP_NAME = "name-checkThree";
    public static final String SANDBOXEP1_WEB_APP_NAME = "name-check1_SB";
    public static final String SANDBOXEP2_WEB_APP_NAME = "name-check2_SB";
    public static final String SANDBOXEP3_WEB_APP_NAME = "name-check3_SB";

    public static final String RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER = "Application & Application User";
    public static final String RESOURCE_AUTH_TYPE_APPLICATION = "Application";
    public static final String RESOURCE_AUTH_TYPE_APPLICATION_USER = "Application User";
    public static final String RESOURCE_AUTH_TYPE_NONE = "None";

    public static final String HTTP_VERB_GET = "GET";
    public static final String HTTP_VERB_POST = "POST";

    public static final class APPLICATION_TIER {
        public static final String UNLIMITED = "Unlimited";
        public static final String LARGE = "Large";
        public static final String MEDIUM = "Medium";
        public static final String SMALL = "Small";

        public static final int LARGE_LIMIT = 20;
        public static final int MEDIUM_LIMIT = 5;
        public static final int SMALL_LIMIT = 1;
    }

    public static class API_TIER {
        public static final String UNLIMITED = "Unlimited";
        public static final String GOLD = "Gold";
        public static final String SILVER = "Silver";
        public static final String BRONZE = "Bronze";

        public static final int GOLD_LIMIT = 20;
        public static final int SILVER_LIMIT = 5;
        public static final int BRONZE_LIMIT = 1;

    }

    public static class RESOURCE_TIER {
        public static final String UNLIMITED = "Unlimited";
        public static final String ULTIMATE = "Ultimate";
        public static final String PLUS = "Plus";
        public static final String BASIC = "Basic";

        public static final int ULTIMATE_LIMIT = 20;
        public static final int PLUS_LIMIT = 5;
        public static final int BASIC_LIMIT = 1;
    }
}
