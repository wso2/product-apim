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
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.validation.VerificationUtil;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class APIPublisherRestClient {
	private String backEndUrl;
	private static final String URL_SURFIX = "/publisher/site/blocks";
	private Map<String, String> requestHeaders = new HashMap<String, String>();

	/**
	 * construct API resr client
	 * @param backEndUrl
	 */
	public APIPublisherRestClient(String backEndUrl) {
		this.backEndUrl = backEndUrl;
		if (requestHeaders.get("Content-Type") == null) {
			this.requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
		}
	}

	/**
	 * login to publisher
	 * @param userName
	 * @param password
	 * @return
	 * @throws Exception
	 */

	public HttpResponse login(String userName, String password)
			throws Exception {
		HttpResponse response = HttpRequestUtil
				.doPost(new URL(backEndUrl + URL_SURFIX + "/user/login/ajax/login.jag")
						, "action=login&username=" + userName + "&password=" + password + "",
						requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			String session = getSession(response.getHeaders());
			if (session == null) {
				throw new Exception("No session cookie found with response");
			}
			setSession(session);
			return response;
		} else {
			throw new Exception("User Login failed> " + response.getData());
		}

	}

	/**
	 * log out from publisher
	 * @return
	 * @throws Exception
	 */

    public HttpResponse logout()
            throws Exception {
        HttpResponse response = HttpRequestUtil
                .doGet(backEndUrl + URL_SURFIX + "/user/login/ajax/login.jag?action=logout",
                        requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            String session = getSession(response.getHeaders());
            if (session == null) {
                throw new Exception("No session cookie found with response");
            }
            setSession(session);
            return response;
        } else {
            throw new Exception("User Login failed> " + response.getData());
        }

    }

	/**
	 * add API to APIM
	 * @param apiRequest
	 * @return
	 * @throws Exception
	 */

	public HttpResponse addAPI(APIRequest apiRequest)
			throws Exception {
		checkAuthentication();
		HttpResponse response =
				HttpRequestUtil.doPost(new URL(backEndUrl + URL_SURFIX + "/item-add/ajax/add.jag")
						, apiRequest.generateRequestParameters()
						, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("API Adding failed> " + response.getData());
		}
	}

	/**
	 * copy API
	 * @param provider
	 * @param APIName
	 * @param oldVersion
	 * @param newVersion
	 * @param isDefaultVersion
	 * @return
	 * @throws Exception
	 */
    public HttpResponse copyAPI(String provider, String APIName, String oldVersion,
                                String newVersion, String isDefaultVersion)
            throws Exception {
        checkAuthentication();
        HttpResponse response =
                HttpRequestUtil.doPost(new URL(backEndUrl + URL_SURFIX + "/overview/ajax/overview.jag")
                        , "action=createNewAPI&provider=" + provider + "&apiName=" + APIName + "&version="
                            + oldVersion + "&newVersion=" + newVersion + "&isDefaultVersion=" + isDefaultVersion
                        , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("API Copying failed> " + response.getData());
        }
    }

	/**
	 * update created API
	 * @param apiRequest
	 * @return
	 * @throws Exception
	 */

	public HttpResponse updateAPI(APIRequest apiRequest)
			throws Exception {
		checkAuthentication();
		HttpResponse response =
				HttpRequestUtil.doPost(new URL(backEndUrl + URL_SURFIX + "/item-add/ajax/add.jag")
						, apiRequest.generateRequestParameters("updateAPI")
						, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("API Adding failed> " + response.getData());
		}
	}

	/**
	 * change API status
	 * @param updateRequest
	 * @return
	 * @throws Exception
	 */

	public HttpResponse changeAPILifeCycleStatus(APILifeCycleStateRequest updateRequest)
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doPost(new URL(
				backEndUrl + "/publisher/site/blocks/life-cycles/ajax/life-cycles.jag")
				, updateRequest.generateRequestParameters()
				, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("API LifeCycle Updating failed> " + response.getData());
		}

	}

	/**
	 * get API info
	 * @param apiName
	 * @param provider
	 * @return
	 * @throws Exception
	 */

	public HttpResponse getAPI(String apiName, String provider)
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil
				.doPost(new URL(backEndUrl + "/publisher/site/blocks/listing/ajax/item-list.jag")
						,
						"action=getAPI&name=" + apiName + "&version=1.0.0&provider=" + provider + ""
						, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get API Information failed> " + response.getData());
		}

        /* name=YoutubeFeeds&version=1.0.0&provider=provider1&status=PUBLISHED&publishToGateway=true&action=updateStatus  */

	}

	/**
	 * delete API
	 * @param name
	 * @param version
	 * @param provider
	 * @return
	 * @throws Exception
	 */

	public HttpResponse deleteAPI(String name, String version, String provider)
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil
				.doPost(new URL(backEndUrl + "/publisher/site/blocks/item-add/ajax/remove.jag")
						, "action=removeAPI&name=" + name + "&version=" + version + "&provider=" +
						  provider
						, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("API Deletion failed : " + response.getData());
		}
	}

	public void setHttpHeader(String headerName, String value) {
		this.requestHeaders.put(headerName, value);
	}

	public String getHttpHeader(String headerName) {
		return this.requestHeaders.get(headerName);
	}

	public void removeHttpHeader(String headerName) {
		this.requestHeaders.remove(headerName);
	}

	private String getSession(Map<String, String> responseHeaders) {
		return responseHeaders.get("Set-Cookie");
	}

	private String setSession(String session) {
		return requestHeaders.put("Cookie", session);
	}

	/**
	 * check whether  user is logged in
	 * @return
	 * @throws Exception
	 */
	private boolean checkAuthentication() throws Exception {
		if (requestHeaders.get("Cookie") == null) {
			throw new Exception("No Session Cookie found. Please login first");
		}
		return true;
	}


	/**
	 * update document
	 * @param apiName
	 * @param version
	 * @param provider
	 * @param docName
	 * @param docType
	 * @param sourceType
	 * @param docUrl
	 * @param summary
	 * @param docLocation
	 * @return
	 * @throws Exception
	 */

    public HttpResponse updateDocument(String apiName, String version, String provider, String docName,
                                    String docType, String sourceType, String docUrl,
                                    String summary, String docLocation) throws Exception {
        checkAuthentication();
        HttpResponse response = HttpRequestUtil
                .doPost(new URL(backEndUrl + "/publisher/site/blocks/documentation/ajax/docs.jag")
                        , "action=addDocumentation" + "&mode=Update&provider=" + provider + "&apiName=" +
                        apiName + "&version=" + version + "&docName=" + docName + "&docType=" +
                        docType + "&sourceType=" + sourceType + "&docUrl=" + docUrl
                        + summary + "&docLocation=" + docLocation
                        , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("API Subscription failed : " + response.getData());
        }
    }

	/**
	 * add inline content
	 * @param apiName
	 * @param version
	 * @param provider
	 * @param docName
	 * @param content
	 * @param docDetails
	 * @return
	 * @throws Exception
	 */

	public HttpResponse inlineContent(String apiName, String version, String provider,
	                                  String docName, String content, String docDetails)
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil
				.doPost(new URL(backEndUrl + "/publisher/site/blocks/documentation/ajax/docs.jag")
						, "action=addInlineContent" + "&provider=" + provider + "&apiName=" +
						  apiName + "&version=" + version + "&docName=" + docName + "&content=" +
						  content + "&docDetails=" + docDetails
						, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("API Subscription failed : " + response.getData());
		}
	}

	/**
	 * remove document
	 * @param apiName
	 * @param version
	 * @param provider
	 * @param docName
	 * @param docType
	 * @return
	 * @throws Exception
	 */

	public HttpResponse removeDocumentation(String apiName, String version, String provider,
	                                        String docName, String docType) throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil
				.doPost(new URL(backEndUrl + "/publisher/site/blocks/documentation/ajax/docs.jag")
						, "action=removeDocumentation" + "&provider=" + provider + "&apiName=" +
						  apiName + "&version=" + version + "&docName=" + docName + "&docType=" +
						  docType
						, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("API Subscription failed : " + response.getData());
		}
	}

	/**
	 * get access token data
	 * @param accessToken
	 * @return
	 * @throws Exception
	 */

	public HttpResponse getAccessTokenData(String accessToken) throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil
				.doPost(new URL(backEndUrl + "/publisher/site/blocks/tokens/ajax/token.jag")
						, "action=getAccessTokenData" + "&accessToken=" + accessToken
						, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("API Subscription failed : " + response.getData());
		}
	}

	/**
	 * revoke access token
	 * @param accessToken
	 * @param consumerKey
	 * @param authUser
	 * @return
	 * @throws Exception
	 */
	public HttpResponse revokeAccessToken(String accessToken, String consumerKey, String authUser)
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil
				.doPost(new URL(backEndUrl + "/publisher/site/blocks/tokens/ajax/revokeToken.jag")
						,
						"action=revokeAccessToken" + "&accessToken=" + accessToken + "&authUser=" +
						authUser + "&consumerKey=" + consumerKey
						, requestHeaders
				);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("API Subscription failed : " + response.getData());
		}
	}

	/**
	 * revoke access token by subscriber
	 * @param subscriberName
	 * @return
	 * @throws Exception
	 */

	public HttpResponse revokeAccessTokenBySubscriber(String subscriberName) throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil
				.doPost(new URL(backEndUrl + "/publisher/site/blocks/tokens/ajax/revokeToken.jag")
						,
						"action=revokeAccessTokenBySubscriber" + "&subscriberName=" + subscriberName
						, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("API Subscription failed : " + response.getData());
		}
	}

	/**
	 * update permissions to API access
	 * @param tierName
	 * @param permissionType
	 * @param roles
	 * @return
	 * @throws Exception
	 */

	public HttpResponse updatePermissions(String tierName, String permissionType, String roles)
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil
				.doPost(new URL(backEndUrl + "/publisher/site/blocks/tiers/ajax/tiers.jag")
						,
						"action=updatePermissions" + "&tierName=" + tierName + "&permissiontype=" +
						permissionType + "&roles=" + roles
						, requestHeaders
				);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("API Subscription failed : " + response.getData());
		}
	}

	/**
	 * create new API
	 * @param provider
	 * @param apiName
	 * @param version
	 * @param newVersion
	 * @return
	 * @throws Exception
	 */
	public HttpResponse createNewAPI(String provider, String apiName, String version,
	                                 String newVersion) throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil
				.doPost(new URL(backEndUrl + "/publisher/site/blocks/overview/ajax/overview.jag")
						, "action=createNewAPI" + "&provider=" + provider + "&apiName=" + apiName +
						  "&version=" + version + "&newVersion=" + newVersion
						, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("API Subscription failed : " + response.getData());
		}
	}

	/**
	 * update resources of API
	 * @param provider
	 * @param apiName
	 * @param version
	 * @param swaggerRes
	 * @return
	 * @throws Exception
	 */

    public HttpResponse updateResourceOfAPI(String provider, String apiName, String version, String swaggerRes)
            throws Exception {
        checkAuthentication();
        this.requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
                "/publisher/site/blocks/item-design/ajax/add.jag")
                , "action=manage" + "&provider=" + provider + "&name=" + apiName + "&version=" +
                version +"&swagger="+swaggerRes
                , requestHeaders);
        if (response.getResponseCode() == 200) {
            //VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("API Resource update failed : " + response.getData());
        }
    }

	/**
	 * Change the API Lifecycle status to Publish with the option of Re-subscription is required or not
	 *
	 * @param apiIdentifier           - Instance of APIIdentifier
	 * @param isRequireReSubscription - true if Re-subscription is required else false.
	 * @return HttpResponse - Response of the API publish event
	 * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Exception Throws in checkAuthentication() and when do the REST service calls to do the
	 *                                            lifecycle change.
	 */
	public HttpResponse changeAPILifeCycleStatusToPublish(APIIdentifier apiIdentifier, boolean isRequireReSubscription)
			throws APIManagerIntegrationTestException {
		try {
			checkAuthentication();
			APILifeCycleStateRequest publishUpdateRequest =
					new APILifeCycleStateRequest(apiIdentifier.getApiName(), apiIdentifier.getProviderName(), APILifeCycleState.PUBLISHED);
			publishUpdateRequest.setVersion(apiIdentifier.getVersion());
			String requestParameters = publishUpdateRequest.generateRequestParameters();
			if (isRequireReSubscription) {
				requestParameters += "&requireResubscription=true";
			}
			return HttpRequestUtil.doPost(new URL(backEndUrl + "/publisher/site/blocks/life-cycles/ajax/life-cycles.jag")
					, requestParameters, requestHeaders);
		} catch (Exception e) {
			throw new APIManagerIntegrationTestException("Exception when change he lifecycle to publish", e);

		}

	}

	/**
	 * Get the API information  for the given API Name,API Version and API Provider
	 *
	 * @param apiName  - Name of the API
	 * @param provider - Provider Name of the API
	 * @param version  - Version of the API
	 * @return HttpResponse -  Response of the getAPI request
	 * @throws APIManagerIntegrationTestException - Exception Throws in checkAuthentication() and when do the REST service calls to get the
	 *                                            API information.
	 */
	public HttpResponse getApi(String apiName, String provider, String version)
			throws APIManagerIntegrationTestException {
		try {
			checkAuthentication();
			return HttpRequestUtil.doPost(new URL(backEndUrl + "/publisher/site/blocks/listing/ajax/item-list.jag")
					, "action=getAPI&name=" + apiName + "&version=" + version + "&provider=" + provider + "", requestHeaders);
		} catch (Exception e) {
			throw new APIManagerIntegrationTestException("Exception when retrieving a API", e);

		}

	}

	/**
	 * Retrieve the Tier Permission Page
	 *
	 * @return HttpResponse - Response that contains the Tier Permission Page
	 * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication() method and
	 *                                            HttpRequestUtil.doGet() method call
	 */
	public HttpResponse getTierPermissionsPage() throws APIManagerIntegrationTestException {

		try {
			checkAuthentication();
			return HttpRequestUtil.doGet(backEndUrl + "/publisher/site/pages/tiers.jag", requestHeaders);
		} catch (Exception e) {
			throw new APIManagerIntegrationTestException("Exception when retrieving the Tier Permissions page", e);
		}

	}

	/**
	 * Retrieve the API Manage Page
	 *
	 * @param apiName  - Name of the API.
	 * @param provider - Name of the API Provider.
	 * @param version  - Version of the API.
	 * @return HttpResponse - Response that contains the API Manage Page
	 * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication() method and
	 *                                            HttpRequestUtil.doGet() method call
	 */
	public HttpResponse getAPIManagePage(String apiName, String provider, String version) throws APIManagerIntegrationTestException {
		try {
			checkAuthentication();
			return HttpRequestUtil.doGet(backEndUrl + "/publisher/manage?name=" + apiName + "&version=" + version +
					"&provider=" + provider, requestHeaders);
		} catch (Exception e) {
			throw new APIManagerIntegrationTestException("Exception when retrieving the API Manage page", e);
		}

	}


	/**
	 * Retrieve the API Information Page
	 *
	 * @param apiName  - Name of the API.
	 * @param provider - Name of the API Provider.
	 * @param version  - Version of the API.
	 * @return HttpResponse - Response that contains the API Information Page
	 * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication() method and
	 *                                            HttpRequestUtil.doGet() method call
	 */
	public HttpResponse getAPIInformationPage(String apiName, String provider, String version)
			throws APIManagerIntegrationTestException {
		try {
			checkAuthentication();
			return HttpRequestUtil.doGet(backEndUrl + "/publisher/info?name=" + apiName + "&version=" + version +
					"&provider=" + provider, requestHeaders);
		} catch (Exception e) {
			throw new APIManagerIntegrationTestException("Exception when retrieving the API Information page", e);
		}

	}

	/**
	 * Adding a Document to a API
	 *
	 * @param apiName     - Name of the API.
	 * @param version     - Version of the API.
	 * @param provider    - Name of the API Provider.
	 * @param docName     - Name of the Document
	 * @param docType     - Document Type
	 * @param sourceType  - Source Type
	 * @param docUrl      - Document URL
	 * @param summary     - Document summary
	 * @param docLocation - Document Location
	 * @return HttpResponse - Response  with Document adding result.
	 * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication() method and
	 *                                            HttpRequestUtil.doPost() method call
	 */
	public HttpResponse addDocument(String apiName, String version, String provider, String docName, String docType,
									String sourceType, String docUrl, String summary, String docLocation)
			throws APIManagerIntegrationTestException {
		try {
			checkAuthentication();
			return HttpRequestUtil.doPost(new URL(backEndUrl + "/publisher/site/blocks/documentation/ajax/docs.jag")
					, "action=addDocumentation&provider=" + provider + "&apiName=" + apiName + "&version=" + version +
					"&docName=" + docName + "&docType=" + docType + "&sourceType=" + sourceType + "&docUrl" + docUrl +
					"=&summary=" + summary + "&docLocation=" + docLocation, requestHeaders);
		} catch (Exception e) {
			throw new APIManagerIntegrationTestException("Exception when Adding document to a API", e);
		}
	}

	/**
	 * Retrieve the All APIs available for the user in Publisher. this calls  the action=getAllAPIs in
	 * "/publisher/site/blocks/listing/ajax/item-list.jag"
	 *
	 * @return HttpResponse - Response that contains all available APIs for the user
	 * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication() method and
	 *                                            HttpRequestUtil.doGet() method call
	 */
	public HttpResponse getAllAPIs() throws APIManagerIntegrationTestException {
		try {
			checkAuthentication();
			return HttpRequestUtil.doGet(backEndUrl + "/publisher/site/blocks/listing/ajax/item-list.jag?action=getAllAPIs",
					requestHeaders);

		} catch (Exception e) {
			throw new APIManagerIntegrationTestException("Exception when Retrieve the All APIs available for the user in Publisher", e);
		}

	}


	/**
	 * Add a API using APICreationRequestBean object
	 *
	 * @param creationRequestBean - Instance of APICreationRequestBean object with all needed
	 *                            information to create the API.
	 * @return HttpResponse - Response that contains the result of APi creation activity.
	 * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication()
	 *                                            method and HttpRequestUtil.doPost() method call
	 */
	public HttpResponse addAPI(APICreationRequestBean creationRequestBean)
			throws APIManagerIntegrationTestException {
		try {
			checkAuthentication();
			return HttpRequestUtil.doPost(new URL(backEndUrl + URL_SURFIX + "/item-add/ajax/add.jag")
					, creationRequestBean.generateRequestParameters(), requestHeaders);
		} catch (Exception e) {
			throw new APIManagerIntegrationTestException("Exception when Retrieve the All APIs available " +
					"for the user in Publisher", e);
		}
	}

	/**
	 * Update a API using APICreationRequestBean object
	 *
	 * @param creationRequestBean - Instance of APICreationRequestBean object with all needed
	 *                            information to Update the API.
	 * @return HttpResponse - Response that contains the result of APi creation activity.
	 * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication()
	 *                                            method and HttpRequestUtil.doPost() method call
	 */
	public HttpResponse updateAPI(APICreationRequestBean creationRequestBean)
			throws APIManagerIntegrationTestException {
		try {
			checkAuthentication();
			return HttpRequestUtil.doPost(new URL(backEndUrl + URL_SURFIX + "/item-add/ajax/add.jag")
					, creationRequestBean.generateRequestParameters("updateAPI"), requestHeaders);
		} catch (Exception e) {
			throw new APIManagerIntegrationTestException("Exception when Retrieve the All APIs available " +
					"for the user in Publisher", e);
		}
	}


}
