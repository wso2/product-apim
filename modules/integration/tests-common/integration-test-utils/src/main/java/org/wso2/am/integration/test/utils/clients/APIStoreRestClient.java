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

	private boolean checkAuthentication() throws Exception {
		if (requestHeaders.get("Cookie") == null) {
			throw new Exception("No Session Cookie found. Please login first");
		}
		return true;
	}

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

	/*
		apiData.name = request.getParameter("name");
	apiData.version = request.getParameter("version");
	apiData.provider = request.getParameter("provider")
	 */
	public HttpResponse getAllDocumentationOfApi(String apiName, String version, String provider)
			throws Exception {
		checkAuthentication();
		HttpResponse response =
				HttpRequestUtil.doGet(backEndUrl + "/store/site/blocks/api/listing/ajax/list.jag?" +
				                      "action=getAllDocumentationOfApi&name=" + apiName +
				                      "&version=" + version + "&provider=" + provider
						, requestHeaders);
		if (response.getResponseCode() == 200) {
			return response;
		} else {
			throw new Exception("Generating Application Key failed> " + response.getData());
		}
	}

	/*
				  tenant = request.getParameter("tenant");
			  var start=request.getParameter("start");
			  var end=request.getParameter("end");
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

	public HttpResponse addApplication(String application, String tier, String callbackUrl,
	                                   String description)
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
		                                                       "/store/site/blocks/application/application-add/ajax/application-add.jag?action=addApplication&tier=" +
		                                                       tier + "&callbackUrl=" +
		                                                       callbackUrl + "&description=" +
		                                                       description + "&application=" +
		                                                       application)
				, ""
				, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

	public HttpResponse getApplications()
			throws Exception {
		checkAuthentication();

		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
		                                                       "/store/site/blocks/application/application-list/ajax/application-list.jag?action=getApplications")
				, ""
				, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

	public HttpResponse removeApplication(String application)
			throws Exception {
		checkAuthentication();

		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
		                                                       "/store/site/blocks/application/application-remove/ajax/application-remove.jag?action=removeApplication&application=" +
		                                                       application)
				, ""
				, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

	public HttpResponse updateApplication(String applicationOld, String applicationNew,
	                                      String callbackUrlNew, String descriptionNew, String tier)
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
		                                                       "/store/site/blocks/application/application-update/ajax/application-update.jag?" +
		                                                       "action=updateApplication&applicationOld=" +
		                                                       applicationOld + "&applicationNew=" +
		                                                       applicationNew + "&callbackUrlNew=" +
		                                                       callbackUrlNew +
		                                                       "&descriptionNew=" + descriptionNew +
		                                                       "&tier=" + tier)
				, ""
				, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

	public HttpResponse getAllSubscriptions()
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
		                                                       "/store/site/blocks/subscription/subscription-list/ajax/subscription-list.jag?" +
		                                                       "action=getAllSubscriptions")
				, ""
				, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

    public HttpResponse removeAPISubscription(String API, String version, String provider, String applicationId)
            throws Exception {
        checkAuthentication();

        HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
                "/site/blocks/subscription/subscription-remove/ajax/subscription-remove.jag?action=removeSubscription&name=" +
                API + "&version=" + version + "&provider=" + provider + "&applicationId=" + applicationId)
                , ""
                , requestHeaders);
        if (response.getResponseCode() == 200) {
            VerificationUtil.checkErrors(response);
            return response;
        } else {
            throw new Exception("Get Api Information failed> " + response.getData());
        }

    }

	public HttpResponse getAllTags()
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doPost(new URL(
				backEndUrl + "/store/site/blocks/tag/tag-cloud/ajax/list.jag?action=getAllTags")
				, ""
				, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

	/*
			name = request.getParameter("name");
		version = request.getParameter("version");
		provider = request.getParameter("provider");
		comment = request.getParameter("comment");
		/home/sanjeewa/carbon/turing/components/apimgt/api-store-web/1.2.0/src/site/blocks/comment/comment-add/ajax/comment-add.jag
	 */
	public HttpResponse addComment(String name, String version, String provider, String comment)
			throws Exception {
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

	public HttpResponse isCommentActivated()
			throws Exception {
		checkAuthentication();
		HttpResponse response = HttpRequestUtil
				.doGet(backEndUrl + "/store/site/blocks/comment/comment-add/ajax/comment-add.jag?" +
				       "action=isCommentActivated"
						, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}

	public HttpResponse getRecentlyAddedAPIs(String tenant, String limit)
			throws Exception {
		///home/sanjeewa/carbon/turing/components/apimgt/api-store-web/1.2.0/src/site/blocks/api/recently-added/ajax/list.jag
		checkAuthentication();
		HttpResponse response = HttpRequestUtil.doPost(new URL(backEndUrl +
		                                                       "/store/site/blocks/api/recently-added/ajax/list.jag?action=getRecentlyAddedAPIs" +
		                                                       "&tenant=" + tenant + "&limit=" +
		                                                       limit)
				, ""
				, requestHeaders);
		if (response.getResponseCode() == 200) {
			VerificationUtil.checkErrors(response);
			return response;
		} else {
			throw new Exception("Get Api Information failed> " + response.getData());
		}

	}
}
