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

import org.apache.commons.codec.binary.Base64;
import org.wso2.am.integration.test.utils.bean.GenerateAppKeyRequest;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.validation.VerificationUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class APIStoreRestClient {
	private String backEndUrl;
	private Map<String, String> requestHeaders = new HashMap<String, String>();

	public APIStoreRestClient(String backEndUrl) {
		this.backEndUrl = backEndUrl;
		if (requestHeaders.get("Content-Type") == null) {
			this.requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
		}
	}

	/**
	 * login to API store
	 * @param userName
	 * @param password
	 * @return
	 * @throws Exception
	 */

	public HttpResponse login(String userName, String password)
			throws Exception {
		HttpResponse response = HttpRequestUtil
				.doPost(new URL(backEndUrl + "/store/site/blocks/user/login/ajax/login.jag")
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
	 * subscribe to API
	 * @param subscriptionRequest
	 * @return
	 * @throws Exception
	 */

	public HttpResponse subscribe(SubscriptionRequest subscriptionRequest)
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
		                                                       "/store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag")
				, subscriptionRequest.generateRequestParameters()
				, requestHeaders);
        if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("API Subscription failed> " + response.getData());
		}

	}

	/**
	 * generate token
	 * @param generateAppKeyRequest
	 * @return
	 * @throws Exception
	 */

	public HttpResponse generateApplicationKey(GenerateAppKeyRequest generateAppKeyRequest)
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
				"/store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag")
				, generateAppKeyRequest.generateRequestParameters()
				, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Generating Application Key failed> " + response.getData());
		}

	}

	/**
	 * get all API's which are published
	 * @param apiName
	 * @return
	 * @throws Exception
	 */

	public HttpResponse getAPI(String apiName)
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
				"/store/site/blocks/api/listing/ajax/list.jag?action=getAllPublishedAPIs")
				, ""
				, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

	/**
	 * set HTTP headers
	 * @param headerName
	 * @param value
	 */

	public void setHttpHeader(String headerName, String value) {
		this.requestHeaders.put(headerName, value);
	}

	/**
	 * get headers
	 * @param headerName
	 * @return
	 */
	public String getHttpHeader(String headerName) {
		return this.requestHeaders.get(headerName);
	}

	/**
	 * removeHttpHeader
	 * @param headerName
	 */

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
	 * check whether the user is logged in
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
	 * get access key
	 * @param consumeKey
	 * @param consumerSecret
	 * @param body
	 * @param tokenEndpointURL
	 * @return
	 * @throws Exception
	 */
	public HttpResponse generateUserAccessKey(String consumeKey, String consumerSecret, String body,
	                                          URL tokenEndpointURL)
			throws Exception {
		checkAuthentication();
		Map<String, String> authenticationRequestHeaders = new HashMap<String, String>();
		String basicAuthHeader = consumeKey + ":" + consumerSecret;
		byte[] encodedBytes = Base64.encodeBase64(basicAuthHeader.getBytes());
		authenticationRequestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
		authenticationRequestHeaders.put("Authorization", "Basic " + new String(encodedBytes));
		HttpResponse response = HttpRequestUtil.doPost(tokenEndpointURL
				, body
				, authenticationRequestHeaders);
		if (response.getResponseCode() == 200) {
			return response;
		} else {
			throw new Exception("Generating Application Key failed> " + response.getData());
		}
	}

	/**
	 * get all API's
	 * @return
	 * @throws Exception
	 */

	public HttpResponse getAllPublishedAPIs()
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doGet(backEndUrl +
				"/store/site/blocks/api/listing/ajax/list.jag?action=getAllPublishedAPIs"
				, requestHeaders);
		if (response.getResponseCode() == 200) {
			return response;
		} else {
			throw new Exception("Generating Application Key failed> " + response.getData());
		}
	}

	/**
	 * get all applications
	 * @return
	 * @throws Exception
	 */

	public HttpResponse getAllApplications()
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doGet(backEndUrl +
				"/store/site/blocks/application/application-list/ajax/application-list.jag?action=getApplications"
				, requestHeaders);
		if (response.getResponseCode() == 200) {
			return response;
		} else {
			throw new Exception("Generating Application Key failed> " + response.getData());
		}
	}

	/**
	 * getPublishedAPIsByApplication
	 * @param applicationName
	 * @return
	 * @throws Exception
	 */

	public HttpResponse getPublishedAPIsByApplication(String applicationName)
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doGet(backEndUrl +
				"/store/site/blocks/subscription/subscription-list/ajax/subscription-list.jag?action=getSubscriptionByApplication&app=" +
				applicationName
				, requestHeaders);
		if (response.getResponseCode() == 200) {
			return response;
		} else {
			throw new Exception("Generating Application Key failed> " + response.getData());
		}
	}

	/**
	 * addRatingToAPI
	 * @param apiName
	 * @param version
	 * @param provider
	 * @param rating
	 * @return
	 * @throws Exception
	 */
	public HttpResponse addRatingToAPI(String apiName, String version, String provider,
	                                   String rating)
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil
				.doGet(backEndUrl + "/store/site/blocks/api/api-info/ajax/api-info.jag?" +
						"action=addRating&name=" + apiName + "&version=" + version + "&provider=" +
						provider + "&rating=" + rating
						, requestHeaders);
		if (response.getResponseCode() == 200) {
			return response;
		} else {
			throw new Exception("Generating Application Key failed> " + response.getData());
		}
	}

	/**
	 * removeRatingFromAPI
	 * @param apiName
	 * @param version
	 * @param provider
	 * @return
	 * @throws Exception
	 */

	public HttpResponse removeRatingFromAPI(String apiName, String version, String provider)
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil
				.doGet(backEndUrl + "/store/site/blocks/api/api-info/ajax/api-info.jag?" +
						"action=removeRating&name=" + apiName + "&version=" + version +
						"&provider=" + provider
						, requestHeaders);
		if (response.getResponseCode() == 200) {
			return response;
		} else {
			throw new Exception("Generating Application Key failed> " + response.getData());
		}
	}

	/**
	 * isRatingActivated
	 * @return
	 * @throws Exception
	 */

	public HttpResponse isRatingActivated()
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil
				.doGet(backEndUrl + "/store/site/blocks/api/api-info/ajax/api-info.jag?" +
						"action=isRatingActivated"
						, requestHeaders);
		if (response.getResponseCode() == 200) {
			return response;
		} else {
			throw new Exception("Generating Application Key failed> " + response.getData());
		}
	}

	/**
	 * getAllDocumentationOfAPI
	 * @param apiName
	 * @param version
	 * @param provider
	 * @return
	 * @throws Exception
	 */

	public HttpResponse getAllDocumentationOfAPI(String apiName, String version, String provider)
			throws Exception {
		checkAuthentication();
		HttpResponse response =
				HttpRequestUtil.doGet(backEndUrl + "/store/site/blocks/api/listing/ajax/list.jag?" +
						"action=getAllDocumentationOfAPI&name=" + apiName +
						"&version=" + version + "&provider=" + provider
						, requestHeaders);
		if (response.getResponseCode() == 200) {
			return response;
		} else {
			throw new Exception("Generating Application Key failed> " + response.getData());
		}
	}

	/**
	 * getAllPaginatedPublishedAPIs
	 * @param tenant
	 * @param start
	 * @param end
	 * @return
	 * @throws Exception
	 */

	public HttpResponse getAllPaginatedPublishedAPIs(String tenant, String start, String end)
			throws Exception {
		checkAuthentication();
		HttpResponse response =
				HttpRequestUtil.doGet(backEndUrl + "/store/site/blocks/api/listing/ajax/list.jag?" +
						"action=getAllPaginatedPublishedAPIs&tenant=" + tenant +
						"&start=" + start + "&end=" + end
						, requestHeaders);
		if (response.getResponseCode() == 200) {
			return response;
		} else {
			throw new Exception("Generating Application Key failed> " + response.getData());
		}
	}

	/**
	 * getAllPublishedAPIs for tenant
	 * @param tenant
	 * @return
	 * @throws Exception
	 */

	public HttpResponse getAllPublishedAPIs(String tenant)
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
				"/store/site/blocks/api/listing/ajax/list.jag?action=getAllPublishedAPIs&tenant=" +
				tenant)
				, ""
				, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

	/**
	 * addApplication
	 * @param application
	 * @param tier
	 * @param callbackUrl
	 * @param description
	 * @return
	 * @throws Exception
	 */

	public HttpResponse addApplication(String application, String tier, String callbackUrl, String description)
			throws Exception {

		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
				"/store/site/blocks/application/application-add" +
				"/ajax/application-add.jag?action=addApplication&tier=" + tier + "&callbackUrl=" +
				callbackUrl + "&description=" + description + "&application=" + application), "", requestHeaders);

		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

	/**
	 * get applications
	 * @return
	 * @throws Exception
	 */
	public HttpResponse getApplications() throws Exception {

		checkAuthentication();

		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + "/store/site/blocks/application/" +
				"application-list/ajax/application-list.jag?action=getApplications"), "", requestHeaders);

		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

	/**
	 * delete application
	 * @param application
	 * @return
	 * @throws Exception
	 */
	public HttpResponse removeApplication(String application) throws Exception {
		checkAuthentication();

		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + "/store/site/blocks/application/" +
				"application-remove/ajax/application-remove.jag?action=removeApplication&application=" + application),
				"", requestHeaders);

		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

	/**
	 * updateApplication
	 * @param applicationOld
	 * @param applicationNew
	 * @param callbackUrlNew
	 * @param descriptionNew
	 * @param tier
	 * @return
	 * @throws Exception
	 */

	public HttpResponse updateApplication(String applicationOld, String applicationNew,
	                                      String callbackUrlNew, String descriptionNew, String tier) throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
		                                                       "/store/site/blocks/application/application-update/ajax/application-update.jag?" +
		                                                       "action=updateApplication&applicationOld=" +
		                                                       applicationOld + "&applicationNew=" +
		                                                       applicationNew + "&callbackUrlNew=" +
		                                                       callbackUrlNew +
		                                                       "&descriptionNew=" + descriptionNew +
		                                                       "&tier=" + tier), "", requestHeaders);

		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

	/**
	 * get all subscriptions
	 * @return
	 * @throws Exception
	 */

	public HttpResponse getAllSubscriptions()
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
		                                                       "/store/site/blocks/subscription/subscription-list/ajax/subscription-list.jag?" +
		                                                       "action=getAllSubscriptions"), "", requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

	/**
	 * unsubscribe from API
	 * @param API
	 * @param version
	 * @param provider
	 * @param applicationId
	 * @return
	 * @throws Exception
	 */
    public HttpResponse removeAPISubscription(String API, String version, String provider, String applicationId)
            throws Exception {
        checkAuthentication();

        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
				"/store/site/blocks/subscription/subscription-remove/ajax/subscription-remove.jag?action=removeSubscription&name=" +
				API + "&version=" + version + "&provider=" + provider + "&applicationId=" + applicationId), "", requestHeaders);

        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("Get Api Information failed> " + response.getData());
        }

    }

	/**
	 * get all tags of API
	 * @return
	 * @throws Exception
	 */
	public HttpResponse getAllTags() throws Exception {

		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doPost(new URL(
				backEndUrl + "/store/site/blocks/tag/tag-cloud/ajax/list.jag?action=getAllTags"), "", requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}


	/**
	 * add comment to api
	 * @param name
	 * @param version
	 * @param provider
	 * @param comment
	 * @return
	 * @throws Exception
	 */

	public HttpResponse addComment(String name, String version, String provider, String comment) throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil
				.doPost(new URL(backEndUrl + "/store/site/blocks/comment/comment-add/ajax/comment-add.jag?" +
				       "action=addComment&name=" + name + "&version=" + version + "&provider=" +
				       provider + "&comment=" + comment), "", requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

	/**
	 * isCommentActivated
	 * @return
	 * @throws Exception
	 */
	public HttpResponse isCommentActivated() throws Exception {

		checkAuthentication();

		HttpResponse response = HttpRequestUtil
				.doGet(backEndUrl + "/store/site/blocks/comment/comment-add/ajax/comment-add.jag?" +
						"action=isCommentActivated", requestHeaders);

		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

	/**
	 * getRecentlyAddedAPIs
	 * @param tenant
	 * @param limit
	 * @return
	 * @throws Exception
	 */

	public HttpResponse getRecentlyAddedAPIs(String tenant, String limit) throws Exception {

		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl + "/store/site/blocks/api/" +
				"recently-added/ajax/list.jag?action=getRecentlyAddedAPIs&tenant="
				+ tenant + "&limit=" + limit), "", requestHeaders);

		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

	/**
	 * Get the  web page with filtered API when  click the API Tag link
	 *
	 * @param apiTag - API tag the need ti filter the api.
	 * @return HttpResponse - Response  that contains the web page with filtered API when  click the API Tag link
	 * @throws Exception - Exception throws when  check the Authentication
	 */
	public HttpResponse getAPIPageFilteredWithTags(String apiTag)
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.sendGetRequest(backEndUrl + "/store/apis/list"
				, "tag=" + apiTag + "&tenant=carbon.super");
		return response;

	}

	/**
	 * Subscribe and API. This method return the response of the subscription server REST call.
	 *
	 * @param subscriptionRequest -SubscriptionRequest request instance  with API subscription information.
	 * @return HttpResponse - Response f the subscription server REST call
	 * @throws Exception- Exception throws when  check the Authentication
	 */
	public HttpResponse subscribeAPI(SubscriptionRequest subscriptionRequest)
			throws Exception {
		//This method  do the same functionality as subscribe(), except this method  always returns the response object
		//regardless of the response code. But subscribe() returns the response object only if  the response code is
		// 200 or else it will return an Exception.
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
				"/store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag")
				, subscriptionRequest.generateRequestParameters()
				, requestHeaders);

		return response;

	}
}
