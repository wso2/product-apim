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


    //constants for swagger v2

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
