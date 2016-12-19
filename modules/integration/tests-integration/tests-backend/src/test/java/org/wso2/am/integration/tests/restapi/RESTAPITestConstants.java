/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.restapi;


import java.io.File;

/**
 * This class represents the constants that are used for APIManager REST API implementation
 */
public final class RESTAPITestConstants {

    //substring of the path to the data file
    public static final String PATH_SUBSTRING = File.separator + "src" + File.separator + "test" + File.separator +
            "resources" + File.separator + "rest-api-test-data" + File.separator;

    //application json content type
    public static final String APPLICATION_JSON_CONTENT = "application/json";

    //http PUT method
    public static final String PUT_METHOD = "PUT";

    //http GET method
    public static final String GET_METHOD = "GET";

    //http POST method
    public static final String POST_METHOD = "POST";

    //http DELETE method
    public static final String DELETE_METHOD = "DELETE";

    //regular expression pattern for URL
    public static final String URL_REGEX = "\\{(.*?)\\}";

    //regular expression pattern for payload
    public static final String PAYLOAD_REGEX = "\\((.*?)\\)";

    //text to represent Authorization
    public static final String AUTHORIZATION_KEY = "Authorization";

    //test case root element in JSON file
    public static final String JSON_ROOT_ELEMENT = "testCase";

    //element in JSON file for the initialization
    public static final String INITIALIZATION_SECTION = "init";

    //scope element in initialization section in JSON file
    public static final String SCOPE_ELEMENT = "scope";

    //element in JSON file for the data-set
    public static final String DATA_SECTION = "data";

    //element in JSON file for the assert section
    public static final String ASSERT_SECTION = "asserts";

    //header-asserts under the assert section
    public static final String HEADER_ASSERTS = "header-asserts";

    //body-asserts under the assert section
    public static final String BODY_ASSERTS = "body-asserts";
    public static final String BODY_ASSERTS_JSONPATH = "jsonPath";
    public static final String BODY_ASSERTS_VALUE = "value";
    public static final String BODY_ASSERTS_REGEX = "regex";

    //method element in data section in JSON file
    public static final String METHOD_ELEMENT = "method";

    //URL element in data section in JSON file
    public static final String URL_ELEMENT = "url";

    //query parameter element in data section in JSON file
    public static final String QUERY_PARAMETERS = "query-parameters";

    //request header element in data section in JSON file
    public static final String REQUEST_HEADERS = "request-headers";

    //request payload element in data section in JSON file
    public static final String REQUEST_PAYLOAD = "request-payload";

    //response-headers element in data section in JSON file
    public static final String RESPONSE_HEADERS = "response-headers";

    //status code of the request
    public static final String STATUS_CODE = "status-code";

    //response-payload element in data section in JSON file
    public static final String RESPONSE_PAYLOAD = "response-payload";

    //text to represent consumer key
    public static final String CONSUMER_KEY = "consumerKey";

    //text to represent consumer secret
    public static final String CONSUMER_SECRET = "consumerSecret";

    //oauth message body
    public static final String OAUTH_MESSAGE_BODY = "grant_type=password&username=admin&password=admin&scope=";

    //text to represent access token
    public static final String ACCESS_TOKEN_TEXT = "access_token";

    //basic Auth Header
    public static final String BASIC_AUTH_HEADER = "admin:admin";

    //client-registration link
    public static final String CLIENT_REGISTRATION_URL = "client-registration/v0.11/register";

    //token endpoint suffix
    public static final String TOKEN_ENDPOINT_SUFFIX = "token";

    //text to represent content type
    public static final String CONTENT_TYPE = "Content-Type";

    //text to represent client id
    public static final String CLIENT_ID = "clientId";

    //text to represent client secret
    public static final String CLIENT_SECRET = "clientSecret";

    //text to represent preserve-list element
    public static final String PRESERVE_LIST = "preserve-list";

    //text to represent preserved attribute-name element
    public static final String PRESERVED_ATTRIBUTE_NAME = "attribute-name";

    //text to represent response-location element
    public static final String RESPONSE_LOCATION = "response-location";

}
