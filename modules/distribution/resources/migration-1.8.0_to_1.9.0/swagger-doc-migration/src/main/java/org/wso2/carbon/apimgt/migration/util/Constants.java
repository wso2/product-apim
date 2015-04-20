/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.migration.util;

public final class Constants {
 
	/*
	 * Variables in api_doc.json version 1.1
	 */
	
	public static final String API_DOC_11_BASE_PATH = "basePath";
	public static final String API_DOC_11_RESOURCE_PATH = "resourcePath";
	public static final String API_DOC_11_API_VERSION = "apiVersion";
	public static final String API_DOC_11_APIS = "apis";
	public static final String API_DOC_11_PATH = "path";
	public static final String API_DOC_11_OPERATIONS = "operations";
	public static final String API_DOC_11_METHOD = "httpMethod";
	public static final String API_DOC_11_PARAMETERS = "parameters";
	
	/*
	 * Variables in api_doc.json version 1.2
	 */
	
	public static final String API_DOC_12_BASE_PATH = "basePath";
	public static final String API_DOC_12_RESOURCE_PATH = "resourcePath";
	public static final String API_DOC_12_API_VERSION = "apiVersion";
	public static final String API_DOC_12_APIS = "apis";
	public static final String API_DOC_12_PATH = "path";
	public static final String API_DOC_12_OPERATIONS = "operations";
	public static final String API_DOC_12_METHOD = "method";
	public static final String API_DOC_12_PARAMETERS = "parameters";
	public static final String API_DOC_12_NICKNAME = "nickname";
	//this resource contains all the resources. This is introduced to swagger 1.2 location to hold 
	//all the resource during the migration of 1.6. AM 1.6 does not have resources separated by name
	//so all resources are in one doc
	public static final String API_DOC_12_ALL_RESOURCES_DOC = "resources";
	
	
	/**
	 * default parameter array
	 */
	public static final String DEFAULT_PARAM_ARRAY = "[ " + "  { "
			+ "    \"dataType\": \"String\", "
			+ "    \"description\": \"Access Token\", "
			+ "    \"name\": \"Authorization\", "
			+ "    \"allowMultiple\": false, "
			+ "    \"required\": true, "
			+ "    \"paramType\": \"header\" " + "  }, "
			+ "  { "
			+ "    \"description\": \"Request Body\", "
			+ "    \"name\": \"body\", "
			+ "    \"allowMultiple\": false, "
			+ "    \"type\": \"string\", "
			+ "    \"required\": true, "
			+ "    \"paramType\": \"body\" " + "  } "
			+ "]";
	
	public static final String DEFAULT_PARAM_FOR_URL_TEMPLATE = " { "
			+ "\"name\": \"\", "
			+ "\"allowMultiple\": false, "
			+ "\"required\": true, "
			+ "\"type\": \"string\", "
			+ "\"paramType\": \"path\" "
			+ "}";
	
	public static final String VERSION_1_6 = "1.6";
	public static final String VERSION_1_7 = "1.7";
	public static final String VERSION_1_8 = "1.8";


	//constants for swagger v2
	//default Swagger v2 response parameter
	public static final String DEFAULT_RESPONSE = "{ " +
            "\"200\": "
			+ "{ " +
            "\"description\": \"No response was specified\"} "
			+ "}";

    public static final String DEFAULT_SECURITY_SCHEME = "{" +
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
