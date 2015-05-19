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

package org.wso2.am.integration.test.utils.clients;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides set of method to invoke publisher API
 */
public class APIStoreRestClient {
    private String backendURL;
    private Map<String, String> requestHeaders = new HashMap<String, String>();

    public APIStoreRestClient(String backendURL) {
        this.backendURL = backendURL;
        if (requestHeaders.get("Content-Type") == null) {
            this.requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        }
    }

    /**
     * Login to API store
     *
     * @param userName - username to login
     * @param password - password to login
     * @return - http response
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Throws if login to store fails
     */
    public HttpResponse login(String userName, String password)
            throws APIManagerIntegrationTestException {
        HttpResponse response;
        try {
            response = HttpRequestUtil.doPost(
                    new URL(backendURL + "store/site/blocks/user/login/ajax/login.jag"),
                    "action=login&username=" + userName + "&password=" + password + "",
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to login to the store app ", e);
        }

        String session = getSession(response.getHeaders());

        if (session == null) {
            throw new APIManagerIntegrationTestException("No session cookie found with response");
        }

        setSession(session);
        return response;
    }

    /**
     * Subscribe to API
     *
     * @param subscriptionRequest - subscribe api request
     * @return - http response
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if subscription fails
     */
    public HttpResponse subscribe(SubscriptionRequest subscriptionRequest)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag"),
                    subscriptionRequest.generateRequestParameters(), requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("subscript to api fails", e);
        }
    }

    /**
     * Generate token
     *
     * @param generateAppKeyRequest - generate api key request
     * @return - http response
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if application key generation fails.
     */
    public HttpResponse generateApplicationKey(APPKeyRequestGenerator generateAppKeyRequest)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            HttpResponse responseApp = getAllApplications();
            String appId = getApplicationId(responseApp.getData(), generateAppKeyRequest.getApplication());
            generateAppKeyRequest.setAppId(appId);

            return HttpRequestUtil.doPost(
                    new URL(backendURL + "store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag"),
                    generateAppKeyRequest.generateRequestParameters(), requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("subscript to api fails", e);
        }

    }

    /**
     * Get api which are published
     *
     * @return - http response of get API post request
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if API information retrieval fails.
     */
    public HttpResponse getAPI() throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "store/site/blocks/api/listing/ajax/list.jag?action=getAllPublishedAPIs"),
                    "", requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to retrieve API information ", e);
        }

    }

    private String getSession(Map<String, String> responseHeaders) {
        return responseHeaders.get("Set-Cookie");
    }

    private String setSession(String session) {
        return requestHeaders.put("Cookie", session);
    }

    /**
     * Check whether the user is logged in
     *
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - If session cookie not found in the request header
     */
    private void checkAuthentication() throws APIManagerIntegrationTestException {
        if (requestHeaders.get("Cookie") == null) {
            throw new APIManagerIntegrationTestException("No Session Cookie found. Please login first");
        }
    }

    /**
     * Generate user access key
     *
     * @param consumeKey       - consumer  key of user
     * @param consumerSecret   - consumer secret key
     * @param messageBody      - message body
     * @param tokenEndpointURL - token endpoint url
     * @return - http response of generate access token api call
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if generating APIM access token fails
     */
    public HttpResponse generateUserAccessKey(String consumeKey, String consumerSecret,
                                              String messageBody, URL tokenEndpointURL)
            throws APIManagerIntegrationTestException {

        try {
            checkAuthentication();
            Map<String, String> authenticationRequestHeaders = new HashMap<String, String>();
            String basicAuthHeader = consumeKey + ":" + consumerSecret;
            byte[] encodedBytes = Base64.encodeBase64(basicAuthHeader.getBytes("UTF-8"));

            authenticationRequestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
            authenticationRequestHeaders.put("Authorization", "Basic " + new String(encodedBytes, "UTF-8"));

            return HttpRequestUtil.doPost(tokenEndpointURL, messageBody, authenticationRequestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to generate API access token ", e);
        }
    }

    /**
     * Get all published apis
     *
     * @return - http response of get all published apis
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if getting publish APIs fails
     */
    public HttpResponse getAllPublishedAPIs() throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doGet(
                    backendURL + "store/site/blocks/api/listing/ajax/list.jag?action=getAllPublishedAPIs",
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to get retrieve all published APIs ", e);
        }
    }

    /**
     * Get all the applications
     *
     * @return - http response of get get all applications
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if get all application fails.
     */
    public HttpResponse getAllApplications() throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doGet(
                    backendURL + "store/site/blocks/application/application-list/ajax/" +
                    "application-list.jag?action=getApplications",
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to retrieve all applications ", e);
        }
    }

    /**
     * Get application details by given name
     *
     * @param applicationName - application name
     * @return - http response of get application request
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if get application by name fails
     */
    public HttpResponse getPublishedAPIsByApplication(String applicationName)
            throws APIManagerIntegrationTestException {
        try {

            checkAuthentication();
            return HttpRequestUtil.doGet(
                    backendURL + "store/site/blocks/subscription/subscription-list/ajax/" +
                    "subscription-list.jag?action=getSubscriptionByApplication&app=" +
                    applicationName, requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to retrieve the application -  " +
                                                         applicationName, e);
        }

    }

    /**
     * Add rating into api
     *
     * @param apiName  - name of api
     * @param version  - api version
     * @param provider - provider of api
     * @param rating   - api rating
     * @return - http response of add rating request
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if rating of api fails
     */
    public HttpResponse addRatingToAPI(String apiName, String version, String provider,
                                       String rating) throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doGet(
                    backendURL + "store/site/blocks/api/api-info/ajax/api-info.jag?" +
                    "action=addRating&name=" + apiName + "&version=" + version + "&provider=" +
                    provider + "&rating=" + rating, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to rate API -  " + apiName, e);
        }
    }

    /**
     * Remove rating of given API
     *
     * @param apiName  - name of api
     * @param version  - api version
     * @param provider - provider of api
     * @return - http response of remove rating request
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Throws if remove API rating fails
     */
    public HttpResponse removeRatingFromAPI(String apiName, String version, String provider)
            throws APIManagerIntegrationTestException {
        try {

            checkAuthentication();
            return HttpRequestUtil.doGet(
                    backendURL + "store/site/blocks/api/api-info/ajax/api-info.jag?" +
                    "action=removeRating&name=" + apiName + "&version=" + version +
                    "&provider=" + provider, requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to remove rating of API -  " +
                                                         apiName, e);
        }
    }

    /**
     * Check if API rating activated
     *
     * @return - http response of rating activated request
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Throws if rating status cannot be retrieved
     */
    public HttpResponse isRatingActivated() throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doGet(
                    backendURL + "store/site/blocks/api/api-info/ajax/api-info.jag?" +
                    "action=isRatingActivated", requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Rating status cannot be retrieved", e);
        }
    }

    /**
     * Method to retrieve all documents of given api
     *
     * @param apiName  - name of api
     * @param version  - api version
     * @param provider - provider of api
     * @return - http response of get all documentation of APIs
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if retrieval of API documentation fails
     */
    public HttpResponse getAllDocumentationOfAPI(String apiName, String version, String provider)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doGet(
                    backendURL + "store/site/blocks/api/listing/ajax/list.jag?" +
                    "action=getAllDocumentationOfAPI&name=" + apiName +
                    "&version=" + version + "&provider=" + provider, requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to retrieve documentation for - " +
                                                         apiName, e);

        }
    }

    /**
     * Get all paginated published API for a given tenant
     *
     * @param tenant - tenant name
     * @param start  - starting index
     * @param end    - closing  index
     * @return - http response of paginated published APIs
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if paginated apis cannot be retrieved.
     */
    public HttpResponse getAllPaginatedPublishedAPIs(String tenant, String start, String end)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doGet(backendURL + "store/site/blocks/api/listing/ajax/list.jag?" +
                                         "action=getAllPaginatedPublishedAPIs&tenant=" + tenant +
                                         "&start=" + start + "&end=" + end, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to retrieve paginated published " +
                                                         "APIs for tenant - " + tenant, e);
        }
    }

    /**
     * Get all published APIs for tenant
     *
     * @param tenant - tenant name
     * @return - http response of published API
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if published API retrieval fails.
     */
    public HttpResponse getAllPublishedAPIs(String tenant)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "store/site/blocks/api/listing/ajax/list.jag?action=getAllPublishedAPIs&tenant=" +
                            tenant), "", requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to retrieve published" +
                                                         "APIs for tenant - " + tenant, e);

        }
    }

    /**
     * Add application
     *
     * @param application - application  name
     * @param tier        - throttling tier
     * @param callbackUrl - callback url
     * @param description - description of app
     * @return - http response of add application
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - if fails to add application
     */
    public HttpResponse addApplication(String application, String tier, String callbackUrl,
                                       String description)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL +
                            "store/site/blocks/application/application-add" +
                            "/ajax/application-add.jag?action=addApplication&tier=" +
                            tier + "&callbackUrl=" + callbackUrl + "&description=" + description +
                            "&application=" + application), "", requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to add application - " + application, e);

        }
    }

    /**
     * Get application
     *
     * @return - http response of get applications
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if applications cannot be retrieved.
     */
    public HttpResponse getApplications() throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "store/site/blocks/application/application-list/ajax/" +
                            "application-list.jag?action=getApplications"), "", requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to get applications", e);

        }
    }

    /**
     * Delete application
     *
     * @param application - application name
     * @return - http response of remove application request
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if remove application fails
     */
    public HttpResponse removeApplication(String application)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "store/site/blocks/application/application-remove/ajax/application-remove.jag?" +
                            "action=removeApplication&application=" + application), "", requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to remove application - " + application, e);

        }
    }

    /**
     * Update given application
     *
     * @param applicationOld - application name old
     * @param applicationNew - new  application name
     * @param callbackUrlNew - call back url
     * @param descriptionNew - updated description
     * @param tier           - access tier
     * @return - http response of update application
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if update application fails
     */
    public HttpResponse updateApplication(String applicationOld, String applicationNew,
                                          String callbackUrlNew, String descriptionNew, String tier)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "store/site/blocks/application/application-update/ajax/application-update.jag?" +
                            "action=updateApplication&applicationOld=" + applicationOld + "&applicationNew=" +
                            applicationNew + "&callbackUrlNew=" + callbackUrlNew + "&descriptionNew=" +
                            descriptionNew + "&tier=" + tier), "", requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to update application - " + applicationOld, e);

        }

    }

    /**
     * Get all subscriptions
     *
     * @return - http response of get all subscription request
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if get all subscriptions fails
     */
    public HttpResponse getAllSubscriptions() throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "store/site/blocks/subscription/subscription-list/ajax/subscription-list.jag?" +
                            "action=getAllSubscriptions"), "", requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to get all subscriptions", e);

        }
    }

    /**
     * Unsubscribe from API
     *
     * @param API           - name of api
     * @param version       - api version
     * @param provider      - provider name
     * @param applicationId - application id
     * @return - http response of unsubscription request
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Throws if unsubscription fails
     */
    public HttpResponse removeAPISubscription(String API, String version, String provider,
                                              String applicationId)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "store/site/blocks/subscription/subscription-remove/ajax/subscription-remove.jag?" +
                            "action=removeSubscription&name=" + API + "&version=" + version + "&provider=" + provider +
                            "&applicationId=" + applicationId), "", requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to get all subscriptions", e);

        }
    }

    /**
     * Get all API tags
     *
     * @return - http response of get all api tags
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if get all tags fails
     */
    public HttpResponse getAllTags() throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();

            return HttpRequestUtil.doPost(
                    new URL(backendURL + "store/site/blocks/tag/tag-cloud/ajax/list.jag?action=getAllTags"),
                    "", requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to get all tags", e);
        }

    }


    /**
     * Add comment to given API
     *
     * @param apiName  - name of the api
     * @param version  - api version
     * @param provider - provider name
     * @param comment  - comment to  add
     * @return - http response of add comment
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if add comment fails
     */
    public HttpResponse addComment(String apiName, String version, String provider, String comment)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "store/site/blocks/comment/comment-add/ajax/comment-add.jag?" +
                            "action=addComment&name=" + apiName + "&version=" + version + "&provider=" +
                            provider + "&comment=" + comment), "", requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable add a comment in to API - " + apiName, e);
        }
    }

    /**
     * Check whether commenting is enabled
     *
     * @return - http response of comment status
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Throws if retrieving comment activation status fails.
     */
    public HttpResponse isCommentActivated() throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doGet(
                    backendURL + "store/site/blocks/comment/comment-add/ajax/comment-add.jag?" +
                    "action=isCommentActivated", requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Failed to get comment activation status", e);
        }
    }

    /**
     * Get recently added APIs by tenant
     *
     * @param tenant - tenant name
     * @param limit  - limit of result set
     * @return - http response of recently added API request
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if
     */
    public HttpResponse getRecentlyAddedAPIs(String tenant, String limit)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "store/site/blocks/api/" +
                            "recently-added/ajax/list.jag?action=getRecentlyAddedAPIs&tenant=" +
                            tenant + "&limit=" + limit), "", requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Failed to get recently added APIs from tenant - " +
                                                         tenant, e);
        }
    }

    private String getApplicationId(String jsonStringOfApplications, String applicationName)
            throws APIManagerIntegrationTestException {
        String applicationId = null;
        JSONObject obj;
        try {
            obj = new JSONObject(jsonStringOfApplications);
            JSONArray arr = obj.getJSONArray("applications");
            for (int i = 0; i < arr.length(); i++) {
                String appName = arr.getJSONObject(i).getString("name");
                if (applicationName.equals(appName)) {
                    applicationId = arr.getJSONObject(i).getString("id");
                }
            }
        } catch (JSONException e) {
            throw new APIManagerIntegrationTestException("getting application Id failed ", e);
        }
        return applicationId;
    }

    /**
     * Get the  web page with filtered API when  click the API Tag link
     *
     * @param apiTag - API tag the need ti filter the api.
     * @return HttpResponse - Response  that contains the web page with filtered API when  click the API Tag link
     * @throws APIManagerIntegrationTestException - Exception throws when check the Authentication and
     * HttpRequestUtil.sendGetRequest() method call
     */
    public HttpResponse getAPIPageFilteredWithTags(String apiTag) throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.sendGetRequest(backendURL + "/store/apis/list", "tag=" + apiTag + "&tenant=carbon.super");
        } catch (IOException ex) {
            throw new APIManagerIntegrationTestException("Exception when get APO page filtered by tag", ex);
        }
    }

    /**
     * Subscribe and API. This method return the response of the subscription server REST call.
     *
     * @param subscriptionRequest -SubscriptionRequest request instance  with API subscription information.
     * @return HttpResponse - Response f the subscription server REST call
     * @throws APIManagerIntegrationTestException - Exception throws when check the Authentication and
     *                                            HttpRequestUtil.doPost() method call.
     */
    public HttpResponse subscribeToAPI(SubscriptionRequest subscriptionRequest) throws APIManagerIntegrationTestException {
        //This method  do the same functionality as subscribe(), except this method  always returns the response object
        //regardless of the response code. But subscribe() returns the response object only if  the response code is
        // 200 or else it will return an Exception.
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(new URL(backendURL +
                    "/store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag")
                    , subscriptionRequest.generateRequestParameters(), requestHeaders);
        } catch (Exception ex) {
            throw new APIManagerIntegrationTestException("Exception when Subscribing to a API", ex);
        }
    }


    /**
     * Retrieve the API store page as anonymous user.
     *
     * @param storeTenantDomain - Tenant domain of store that need to  get the page.
     * @return HttpResponse - Response with API store page of the provided domain.
     * @throws APIManagerIntegrationTestException - IOException throws from HttpRequestUtil.doGet() method call
     */

    public HttpResponse getAPIStorePageAsAnonymousUser(String storeTenantDomain) throws APIManagerIntegrationTestException {
        try {
            return HttpRequestUtil.doGet(
                    backendURL + "store/?tenant=" + storeTenantDomain, requestHeaders);
        } catch (Exception ioE) {
            throw new APIManagerIntegrationTestException(
                    "Exception when retrieve the API store page as anonymous user", ioE);
        }
    }

}
