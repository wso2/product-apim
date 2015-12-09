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
    public static final String AM_MONITORING_WEB_APP_NAME = "APIStatusMonitor";

    public static class APPLICATION_TIER {
        public static String LARGE = "Large";
        public static String MEDIUM = "Medium";
        public static String SMALL = "Small";
        public static int LARGE_LIMIT = 20;
        public static int MEDIUM_LIMIT = 5;
        public static int SMALL_LIMIT = 1;
    }

    public static class API_TIER {
        public static String GOLD = "Gold";
        public static String SILVER = "Silver";
        public static String BRONZE = "Bronze";
        public static int GOLD_LIMIT = 20;
        public static int SILVER_LIMIT = 5;
        public static int BRONZE_LIMIT = 1;

    }

    public static class RESOURCE_TIER {
        public static String ULTIMATE = "Ultimate";
        public static String PLUS = "Plus";
        public static String BASIC = "Basic";
        public static int ULTIMATE_LIMIT = 20;
        public static int PLUS_LIMIT = 5;
        public static int BASIC_LIMIT = 1;
    }
}
