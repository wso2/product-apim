/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.test.utils.clients;

import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class APIPublisherRestClient {
	private String backendURL;
	private static final String URL_SURFIX = "publisher/site/blocks";
	private Map<String, String> requestHeaders = new HashMap<String, String>();

	/**
	 * construct API rest client
	 * @param backendURL - url of the publisher jaggery app
	 */
	public APIPublisherRestClient(String backendURL) {
		this.backendURL = backendURL;
		if (requestHeaders.get("Content-Type") == null) {
			this.requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
		}
	}

	/**
	 * login to publisher app
	 * @param userName - provided user name
	 * @param password - password
	 * @return http response object
	 * @throws Exception
	 */
	public HttpResponse login(String userName, String password)
			throws Exception {
		HttpResponse response = HttpRequestUtil
				.doPost(new URL(backendURL + URL_SURFIX + "/user/login/ajax/login.jag")
						, "action=login&username=" + userName + "&password=" + password + "",
						requestHeaders);

		String session = getSession(response.getHeaders());
		if (session == null) {
			throw new APIManagerIntegrationTestException("No session cookie found with response");
		}
		setSession(session);
		return response;


	}

	/**
	 * log out from publisher
	 * @return http response object
	 * @throws Exception
	 */
    public HttpResponse logout()
            throws Exception {

		return HttpRequestUtil
                .doGet(backendURL + URL_SURFIX + "/user/login/ajax/login.jag?action=logout",
                        requestHeaders);
    }

	/**
	 * add API to APIM
	 * @param apiRequest - Constructed API request object
	 * @return http response object
	 * @throws Exception
	 */
	public HttpResponse addAPI(APIRequest apiRequest)
			throws Exception {

		checkAuthentication();

		return HttpRequestUtil.doPost(new URL(backendURL + URL_SURFIX + "/item-add/ajax/add.jag")
						, apiRequest.generateRequestParameters()
						, requestHeaders);
	}

	/**
	 * copy API from existing API
	 * @param provider - name of the provider , ex: admin
	 * @param APIName - API name
	 * @param oldVersion - existing version
	 * @param newVersion - new version
	 * @param isDefaultVersion - check default version
	 * @return http response object
	 * @throws Exception
	 */
    public HttpResponse copyAPI(String provider, String APIName, String oldVersion,
                                String newVersion, String isDefaultVersion)
            throws Exception {

        checkAuthentication();

		return HttpRequestUtil.doPost(new URL(backendURL + URL_SURFIX + "/overview/ajax/overview.jag")
                        , "action=createNewAPI&provider=" + provider + "&apiName=" + APIName + "&version="
                            + oldVersion + "&newVersion=" + newVersion + "&isDefaultVersion=" + isDefaultVersion
                        , requestHeaders);
    }

	/**
	 * update created API
	 * @param apiRequest - constructed API request object
	 * @return http response object
	 * @throws Exception
	 */
	public HttpResponse updateAPI(APIRequest apiRequest)
			throws Exception {

		checkAuthentication();

		return	HttpRequestUtil.doPost(new URL(backendURL + URL_SURFIX + "/item-add/ajax/add.jag")
						, apiRequest.generateRequestParameters("updateAPI")
						, requestHeaders);
	}

	/**
	 * change status of a created API
	 * @param updateRequest - APILifeCycleStateRequest object
	 * @return http response object
	 * @throws Exception
	 */
	public HttpResponse changeAPILifeCycleStatus(APILifeCycleStateRequest updateRequest)
			throws Exception {

		checkAuthentication();

		return HttpRequestUtil.doPost(new URL(
				backendURL + "publisher/site/blocks/life-cycles/ajax/life-cycles.jag")
				, updateRequest.generateRequestParameters()
				, requestHeaders);

	}

	/**
	 * get API info
	 * @param apiName - API name
	 * @param provider - name of the provider
	 * @return http response object
	 * @throws Exception
	 */
	public HttpResponse getAPI(String apiName, String provider)
			throws Exception {

		checkAuthentication();

		return HttpRequestUtil
				.doPost(new URL(backendURL + "publisher/site/blocks/listing/ajax/item-list.jag")
						,
						"action=getAPI&name=" + apiName + "&version=1.0.0&provider=" + provider + ""
						, requestHeaders);

	}

	/**
	 * delete API
	 * @param name - API name
	 * @param version - API version
	 * @param provider - name of the provider
	 * @return http response object
	 * @throws Exception
	 */
	public HttpResponse deleteAPI(String name, String version, String provider)
			throws Exception {

		checkAuthentication();

		return HttpRequestUtil
				.doPost(new URL(backendURL + "publisher/site/blocks/item-add/ajax/remove.jag")
						, "action=removeAPI&name=" + name + "&version=" + version + "&provider=" +
						  provider
						, requestHeaders);
	}

	private String getSession(Map<String, String> responseHeaders) {
		return responseHeaders.get("Set-Cookie");
	}

	private String setSession(String session) {
		return requestHeaders.put("Cookie", session);
	}

	/**
	 * check whether user is authorized
	 * @throws Exception
	 */
	private void checkAuthentication() throws Exception {
		if (requestHeaders.get("Cookie") == null) {
			throw new APIManagerIntegrationTestException("No Session Cookie found. Please login first");
		}
	}

	/**
	 * add document to API
	 * @param apiName - API name
	 * @param version - API version
	 * @param provider - name of the provider
	 * @param docName - document name
	 * @param docType - type of document
	 * @param sourceType - source ( url,file)
	 * @param docUrl - URL of doc
	 * @param summary - summary of doc
	 * @param docLocation - location
	 * @return http response object
	 * @throws Exception
	 */
	public HttpResponse addDocument(String apiName, String version, String provider, String docName,
	                                String docType, String sourceType, String docUrl,
	                                String summary, String docLocation) throws Exception {

		/*curl -X POST -b cookies http://localhost:9763/publisher/site/blocks/documentation/ajax/docs.jag -d
		"action=addDocumentation&provider=admin&apiName=PhoneVerification&version=1.0.0
		&docName=testDoc&docType=how to&sourceType=inline&docUrl=&summary=testing&docLocation="*/

		checkAuthentication();

		return HttpRequestUtil
				.doPost(new URL(backendURL + "publisher/site/blocks/documentation/ajax/docs.jag")
						, "action=addDocumentation" + "&mode=''&provider=" + provider + "&apiName=" +
						  apiName + "&version=" + version + "&docName=" + docName + "&docType=" +
						  docType + "&sourceType=" + sourceType + "&docUrl=" + docUrl
						  + summary + "&docLocation=" + docLocation
						, requestHeaders);

	}

	/**
	 * remove document
	 * @param apiName - API name
	 * @param version - API version
	 * @param provider - name of the provider
	 * @param docName - document name
	 * @param docType - document type
	 * @return http response object
	 * @throws Exception
	 */
	public HttpResponse removeDocumentation(String apiName, String version, String provider,
	                                        String docName, String docType) throws Exception {
		checkAuthentication();

		return HttpRequestUtil
				.doPost(new URL(backendURL + "publisher/site/blocks/documentation/ajax/docs.jag")
						, "action=removeDocumentation" + "&provider=" + provider + "&apiName=" +
						  apiName + "&version=" + version + "&docName=" + docName + "&docType=" +
						  docType
						, requestHeaders);

	}

	/**
	 * revoke access token
	 * @param accessToken - access token already  received
	 * @param consumerKey -  consumer key returned
	 * @param authUser    - user name
	 * @return http response object
	 * @throws Exception
	 */
	public HttpResponse revokeAccessToken(String accessToken, String consumerKey, String authUser)
			throws Exception {

		checkAuthentication();

		return HttpRequestUtil
				.doPost(new URL(backendURL + "publisher/site/blocks/tokens/ajax/revokeToken.jag")
						,
						"action=revokeAccessToken" + "&accessToken=" + accessToken + "&authUser=" +
						authUser + "&consumerKey=" + consumerKey
						, requestHeaders
				);
	}


	/**
	 * update permissions to API access
	 * @param tierName 			- name of api throttling tier
	 * @param permissionType 	- permission type
	 * @param roles				- roles of permission
	 * @return http response object
	 * @throws Exception
	 */
	public HttpResponse updatePermissions(String tierName, String permissionType, String roles)
			throws Exception {

		checkAuthentication();

		return HttpRequestUtil
				.doPost(new URL(backendURL + "publisher/site/blocks/tiers/ajax/tiers.jag")
						,
						"action=updatePermissions" + "&tierName=" + tierName + "&permissiontype=" +
						permissionType + "&roles=" + roles
						, requestHeaders
				);
	}

	/**
	 * update resources of API
	 * @param provider - provider name of creator
	 * @param apiName  - name of api
	 * @param version  - api version
	 * @param swaggerRes - swagger doc
	 * @return http response object
	 * @throws Exception
	 */
    public HttpResponse updateResourceOfAPI(String provider, String apiName, String version, String swaggerRes)
            throws Exception {

        checkAuthentication();

        this.requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        return HttpRequestUtil.doPost(new URL(backendURL +
                "publisher/site/blocks/item-design/ajax/add.jag")
                , "action=manage" + "&provider=" + provider + "&name=" + apiName + "&version=" +
                version +"&swagger="+swaggerRes
                , requestHeaders);

    }


/**
     * Get the API information  for the given API Name,API Version and API Provider
     *
     * @param apiName  Name of the API
     * @param provider Provider Name of the API
     * @param version  Version of the API
     * @return http response object
     * @throws Exception
     */
    public HttpResponse getAPI(String apiName, String provider, String version)
            throws Exception {

        checkAuthentication();

        return HttpRequestUtil.doPost(new URL(backendURL +
                "publisher/site/blocks/listing/ajax/item-list.jag")
                , "action=getAPI&name=" + apiName + "&version=" + version + "&provider=" + provider + ""
                , requestHeaders);
    }

	/**
	 * Change the API Lifecycle status to Publish with the option of Re-subscription is required or not
	 *
	 * @param apiIdentifier           - Instance of APIIdentifier
	 * @param isRequireReSubscription - true if Re-subscription is required else false.
	 * @return HttpResponse - Response of the API publish event
	 * @throws Exception - Exception Throws in checkAuthentication() and when do the REST service calls to do the
	 *                   lifecycle change
	 */
	public HttpResponse changeAPILifeCycleStatusToPublish(APIIdentifier apiIdentifier, boolean isRequireReSubscription)
			throws Exception {

		checkAuthentication();

		APILifeCycleStateRequest publishUpdateRequest =
				new APILifeCycleStateRequest(apiIdentifier.getApiName(), apiIdentifier.getProviderName(), APILifeCycleState.PUBLISHED);
		publishUpdateRequest.setVersion(apiIdentifier.getVersion());
		String requestParameters = publishUpdateRequest.generateRequestParameters();
		if (isRequireReSubscription) {
			requestParameters += "&requireResubscription=true";
		}

		return HttpRequestUtil.doPost(new URL(backendURL + "publisher/site/blocks/life-cycles/ajax/life-cycles.jag")
				, requestParameters, requestHeaders);

	}

	/**
	 * Get the API information  for the given API Name,API Version and API Provider
	 *
	 * @param apiName  - Name of the API
	 * @param provider - Provider Name of the API
	 * @param version  - Version of the API
	 * @return HttpResponse -  Response of the getAPI request
	 * @throws Exception - Exception Throws in checkAuthentication() and when do the REST service calls to get the
	 *                   API information.
	 */
	public HttpResponse getApi(String apiName, String provider, String version)
			throws Exception {

		checkAuthentication();

		return HttpRequestUtil.doPost(new URL(backendURL + "publisher/site/blocks/listing/ajax/item-list.jag")
				, "action=getAPI&name=" + apiName + "&version=" + version + "&provider=" + provider + "", requestHeaders);

	}

	/**
	 * Check the Endpoint is valid
	 *
	 * @param endpointUrl url of the endpoint
	 * @param type        type of Endpoint
	 * @return HttpResponse -  Response of the getAPI request
	 * @throws Exception - Exception Throws in checkAuthentication() and when do the REST service calls to get the
	 *                   API information.
	 */
	public HttpResponse checkValidEndpoint(String type, String endpointUrl)
			throws Exception {

		checkAuthentication();

		return HttpRequestUtil.doPost(new URL(backendURL + "publisher/site/blocks/item-add/ajax/add.jag")
				, "action=isURLValid&" + "type=" + type + "&url=" + endpointUrl, requestHeaders);
	}
}
