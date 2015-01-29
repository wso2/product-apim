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
package org.wso2.am.integration.test.extensions.user.mgt;


import java.io.File;

public class ExtensionCommonConstants {
    public static final String SYSTEM_PROPERTY_SETTINGS_LOCATION = "automation.settings.location";
    public static final String SYSTEM_PROPERTY_BASEDIR_LOCATION = "basedir";
    public static final String SYSTEM_PROPERTY_OS_NAME = "os.name";
    public static final String SYSTEM_PROPERTY_CARBON_ZIP_LOCATION = "carbon.zip";
    public static final String SYSTEM_PROPERTY_SEC_VERIFIER_DIRECTORY = "sec.verifier.dir";
    public static final int DEFAULT_CARBON_PORT_OFFSET = 0;
    public static final String SERVICE_FILE_SEC_VERIFIER = "SecVerifier.aar";
    public static final String SEVER_STARTUP_SCRIPT_NAME = "wso2server";
    public static final String SERVER_STARTUP_PORT_OFFSET_COMMAND = "-DportOffset";
    public static final String SERVER_DEFAULT_HTTPS_PORT = "9443";
    public static final String SERVER_DEFAULT_HTTP_PORT = "9763";
    public static final String SUPER_TENANT_DOMAIN_NAME = "carbon.super";
    public static final String ADMIN_ROLE = "admin";
    public static final String ADMIN_USER = "admin";
    public static final String DEFAULT_KEY_STORE = "wso2";
    public static final String TENANT_USAGE_PLAN_DEMO = "demo";
    public static final String AUTOMATION_SCHEMA_NAME = "automationXMLSchema";
    public static final String AUTOMATION_SCHEMA_EXTENSION = ".xsd";
    public static final String LISTENER_INIT_METHOD = "initialize";
    public static final String LISTENER_EXECUTE_METHOD = "execution";
    public static final String DEFAULT_BACKEND_URL = "https://localhost:9443/services/";
    public static final String AUTHENTICATE_ADMIN_SERVICE_NAME = "AuthenticationAdmin";
    public static final String CONFIGURATION_FILE_NAME = "automation";
    public static final String CONFIGURATION_FILE_EXTENSION = ".xml";
    public static final String MAPPING_FILE_NAME = "automation_mapping";
    public static final String PORT_OFFSET_COMMAND = "-DportOffset";
    public static final String FIREFOX_BROWSER = "firefox";
    public static final String CHROME_BROWSER = "chrome";
    public static final String IE_BROWSER = "ie";
    public static final String HTML_UNIT_DRIVER = "htmlUnit";
    public static final String OPERA_BROWSER = "opera";
    public static final String DATE_FORMAT_YY_MM_DD_HH_MIN_SS = "yyyy_MM_dd_HH_mm_ss";
    public static final String UNDERSCORE = "_";
    public static final String SCREEN_SHOT_LOCATION = "capturedscreens" + File.separator + "failedtests";
    public static final String SCREEN_SHOT_EXTENSION = ".png";
}