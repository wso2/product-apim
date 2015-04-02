/*
*Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.ui.tests.util;

public class APIMTestConstants {

    public static final String API_ACTION = "action";
    public static final String API_ADD_ACTION = "addAPI";
    public static final String API_CHANGE_STATUS_ACTION = "updateStatus";
    public static final String API_LOGIN_ACTION = "login";
    public static final String ADD_SUBSCRIPTION_ACTION = "addAPISubscription";
    public static final String GENERATE_APPLICATION_KEY_ACTION = "generateApplicationKey";

    public static final String APISTORE_LOGIN_USERNAME = "username";
    public static final String APISTORE_LOGIN_PASSWORD = "password";
    public static final String APISTORE_LOGIN_URL = "/site/blocks/user/login/ajax/login.jag";
    public static final String APIPUBLISHER_PUBLISH_URL = "/site/blocks/life-cycles/ajax/life-cycles.jag";
    public static final String APIPUBLISHER_ADD_URL = "/site/blocks/item-add/ajax/add.jag";
    public static final String ADD_SUBSCRIPTION_URL = "/site/blocks/subscription/subscription-add/ajax/subscription-add.jag";

    public static final String PUBLISHED = "PUBLISHED";
    public static final String EMAIL_DOMAIN_SEPARATOR = "@";
    public static final String EMAIL_DOMAIN_SEPARATOR_REPLACEMENT = "-AT-";
    public static final String DEFAULT = "default";
    public static final String ASTERISK = "*";
    public static final String AUTHORIAZATION = "Authorization";
    public static final String BEARER = "Bearer";
    public static final String SPACE = " ";
    public static final String HYPHEN = "-";

    public static final String PUBLISHER_LOGIN_PAGE_URL_VERIFICATION = "login.jag?requestedPage=/publisher/";
    public static final String PUBLISHER_HOME_PAGE_URL_VERIFICATION = "publisher/site/pages/index.jag";
    public static final String STORE_HOME_PAGE_URL_VERIFICATION = "store/";
    public static final String STORE_REST_CLIENT_URL_VERIFICATION = "/rest-client.jag?";

    public static final String APIMANAGEMENTCONSOLE_LOGIN_USERNAME_ID="txtUserName";
    public static final String APIMANAGEMENTCONSOLE_LOGIN_PASSWORD_ID="txtPassword";
    public static final String APIMCONSOLE_ADDTENANT_DOMAIN_ID="domain";
    public static final String APIMCONSOLE_ADDTENANT_FIRSTNAME_ID="admin-firstname";
    public static final String APIMCONSOLE_ADDTENANT_LASTNAME_ID="admin-lastname";
    public static final String APIMCONSOLE_ADDTENANT_USERNAME_ID="admin";
    public static final String APIMCONSOLE_ADDTENANT_PASSW0RD_ID="admin-password";
    public static final String APIMCONSOLE_ADDTENANT_PASSWORDREPEAT_ID="admin-password-repeat";
    public static final String APIMCONSOLE_ADDTENANT_EMAIL_ID="admin-email";

    public static final String APIMANAGEMENTCONSOLE_LOGIN_USERNAME="admin";
    public static final String APIMANAGEMENTCONSOLE_LOGIN_PASSWORD="admin";
    public static final String APIMCONSOLE_DOMAIN="testDomainVisibilitytest.com";
    public static final String APIMCONSOLE_ADDTENANT_FIRSTNAME="testFirstNameVisibilitytest";
    public static final String APIMCONSOLE_ADDTENANT_LASTNAME="testLastNameVisibilitytest";
    public static final String APIMCONSOLE_ADDTENANT_USERNAME="testVisibility";
    public static final String APIMCONSOLE_ADDTENANT_PASSW0RD="testVisibility";
    public static final String APIMCONSOLE_ADDTENANT_PASSWORDREPEAT="testVisibility";
    public static final String APIMCONSOLE_ADDTENANT_EMAIL="testEmail@testDomainVisibilitytest.com";

    public static final String APIPUBLISHER_LOGIN_USERNAME_ID="username";
    public static final String APIPUBLISHER_LOGIN_PASSWORD_ID="pass";
    public static final String APIPUBLISHER_LOGIN_BUTTON_ID="loginButton";
    public static final String APIPUBLISHER_ADD_LINKTEXT="Add";
    public static final String APIPUBLISHER_ADD_APIVISIBILITY_ID="visibility";
    public static final String APIPUBLISHER_USERMENU_ID="userMenu";
    public static final String APIPUBLISHER_LOGOUT_BUTTONCSS="button.btn.btn-danger";
    public static final String APIPUBLISHER_ADD_APINAME_ID="name";
    public static final String APIPUBLISHER_ADD_APICONTEXT_ID="context";
    public static final String APIPUBLISHER_ADD_APIVERSION_ID="version";
    public static final String APIPUBLISHER_ADD_APIRESOURCEURL_ID="resource_url_pattern";
    public static final String APIPUBLISHER_ADD_HTTPVERB_CSS="input.http_verb_select";
    public static final String APIPUBLISHER_ADD_ADDRESOURCE_ID="add_resource";
    public static final String APIPUBLISHER_ADD_IMPLEMENT_ID="go_to_implement";
    public static final String APIPUBLISHER_LOGIN_USERNAME="admin";
    public static final String APIPUBLISHER_LOGIN_PASSWORD="admin";

    public static final String APIPUBLISHER_ADD_APIVISIBILITY="Public";
    public static final String APIPUBLISHER_ADD_APINAME_SUPERTENANT="TestSubcriptionsWithOnlySuperTenant";
    public static final String APIPUBLISHER_ADD_APICONTEXT_SUPERTENANT="TestSubcriptionsWithOnlySuperTenant";
    public static final String APIPUBLISHER_ADD_APINAME_MultipleTENANT="TestSubcriptionsWithMultipleTenants";
    public static final String APIPUBLISHER_ADD_APICONTEXT_MultipleTENANT="TestSubcriptionsWithOnlyMultipleTenant";
    public static final String APIPUBLISHER_ADD_APIVERSION="1.0.0";
    public static final String APIPUBLISHER_ADD_APIRESOURCEURL="resource_url";

    public static final long MAX_LOOP_WAIT_TIME_MILLISECONDS = 60000;
    public static final long WAIT_TIME_VISIBILITY_ELEMENT_SECONDS = 60;

}
