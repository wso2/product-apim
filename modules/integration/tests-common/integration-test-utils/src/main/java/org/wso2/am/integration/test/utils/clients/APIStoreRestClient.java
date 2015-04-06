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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.am.integration.test.utils.bean.GenerateAppKeyRequest;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
     * login to API store
     * @param userName - username to login
     * @param password - password to login
     * @return - http response
     * @throws Exception
     */
    public HttpResponse login(String userName, String password)
            throws Exception {
        HttpResponse response = HttpRequestUtil
                .doPost(new URL(backendURL + "store/site/blocks/user/login/ajax/login.jag")
                        , "action=login&username=" + userName + "&password=" + password + "",
                        requestHeaders);

        String session = getSession(response.getHeaders());
        if (session == null) {
            throw new Exception("No session cookie found with response");
        }
        setSession(session);
        return response;

    }

    /**
     * subscribe to API
     * @param subscriptionRequest - subscribe api request
     * @return - http response
     * @throws Exception
     */
    public HttpResponse subscribe(SubscriptionRequest subscriptionRequest)
            throws Exception {

        checkAuthentication();

        return HttpRequestUtil.doPost(new URL(backendURL +
                "store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag")
                , subscriptionRequest.generateRequestParameters()
                , requestHeaders);
    }

    /**
     * generate token
     * @param generateAppKeyRequest - generate api key request
     * @return - http response
     * @throws Exception
     */
    public HttpResponse generateApplicationKey(GenerateAppKeyRequest generateAppKeyRequest)
            throws Exception {
        checkAuthentication();
        HttpResponse responseApp = getAllApplications();
        String appId = getApplicationId(responseApp.getData(), generateAppKeyRequest.getApplication());
        generateAppKeyRequest.setAppId(appId);

        return HttpRequestUtil.doPost(new URL(backendURL +
                "store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag")
                , generateAppKeyRequest.generateRequestParameters()
                , requestHeaders);

    }

    /**
     * get api which are published
     * @param apiName - name of API
     * @return - http response
     * @throws Exception
     */
    public HttpResponse getAPI(String apiName)
            throws Exception {
        checkAuthentication();
        return HttpRequestUtil.doPost(new URL(backendURL +
                "store/site/blocks/api/listing/ajax/list.jag?action=getAllPublishedAPIs")
                , ""
                , requestHeaders);

    }

    private String getSession(Map<String, String> responseHeaders) {
        return responseHeaders.get("Set-Cookie");
    }

    private String setSession(String session) {
        return requestHeaders.put("Cookie", session);
    }

    /**
     * check whether the user is logged in
     * @throws Exception
     */
    private void checkAuthentication() throws Exception {
        if (requestHeaders.get("Cookie") == null) {
            throw new Exception("No Session Cookie found. Please login first");
        }
    }

    /**
     * get access key
     * @param consumeKey - consumer  key of user
     * @param consumerSecret - consumer secret key
     * @param body - message body
     * @param tokenEndpointURL - token endpoint url
     * @return - http response
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

        return HttpRequestUtil.doPost(tokenEndpointURL
                , body
                , authenticationRequestHeaders);
    }

    /**
     * get all API's which are published
     * @return - http response
     * @throws Exception
     */
    public HttpResponse getAllPublishedAPIs()
            throws Exception {
        checkAuthentication();
        return HttpRequestUtil.doGet(backendURL +
                "store/site/blocks/api/listing/ajax/list.jag?action=getAllPublishedAPIs"
                , requestHeaders);
    }

    /**
     * get all the applications
     * @return - http response
     * @throws Exception
     */
    public HttpResponse getAllApplications()
            throws Exception {
        checkAuthentication();
        return HttpRequestUtil.doGet(backendURL +
                "store/site/blocks/application/application-list/ajax/application-list.jag?action=getApplications"
                , requestHeaders);
    }

    /**
     * getPublishedAPIsByApplication
     * @param applicationName - application name
     * @return - http response
     * @throws Exception
     */
    public HttpResponse getPublishedAPIsByApplication(String applicationName)
            throws Exception {
        checkAuthentication();
        return HttpRequestUtil.doGet(backendURL +
                "store/site/blocks/subscription/subscription-list/ajax/subscription-list.jag?action=getSubscriptionByApplication&app=" +
                applicationName
                , requestHeaders);
    }

    /**
     * addRatingToAPI
     * @param apiName - name of api
     * @param version - api version
     * @param provider - provider of api
     * @param rating   - api rating
     * @return - http response
     * @throws Exception
     */
    public HttpResponse addRatingToAPI(String apiName, String version, String provider,
                                       String rating)
            throws Exception {
        checkAuthentication();
        return HttpRequestUtil
                .doGet(backendURL + "store/site/blocks/api/api-info/ajax/api-info.jag?" +
                        "action=addRating&name=" + apiName + "&version=" + version + "&provider=" +
                        provider + "&rating=" + rating
                        , requestHeaders);
    }

    /**
     * removeRatingFromAPI
     * @param apiName - name of api
     * @param version - api version
     * @param provider - provider of api
     * @return - http response
     * @throws Exception
     */
    public HttpResponse removeRatingFromAPI(String apiName, String version, String provider)
            throws Exception {
        checkAuthentication();
        return HttpRequestUtil
                .doGet(backendURL + "store/site/blocks/api/api-info/ajax/api-info.jag?" +
                        "action=removeRating&name=" + apiName + "&version=" + version +
                        "&provider=" + provider
                        , requestHeaders);
    }

    /**
     * isRatingActivated
     * @return - http response
     * @throws Exception
     */
    public HttpResponse isRatingActivated()
            throws Exception {
        checkAuthentication();
        return HttpRequestUtil
                .doGet(backendURL + "store/site/blocks/api/api-info/ajax/api-info.jag?" +
                        "action=isRatingActivated"
                        , requestHeaders);
    }

    /**
     * getAllDocumentationOfAPI
     * @param apiName - name of api
     * @param version - api version
     * @param provider - provider of api
     * @return - http response
     * @throws Exception
     */
    public HttpResponse getAllDocumentationOfAPI(String apiName, String version, String provider)
            throws Exception {
        checkAuthentication();
        return HttpRequestUtil.doGet(backendURL + "store/site/blocks/api/listing/ajax/list.jag?" +
                        "action=getAllDocumentationOfAPI&name=" + apiName +
                        "&version=" + version + "&provider=" + provider
                        , requestHeaders);
    }

    /**
     * getAllPaginatedPublishedAPIs
     * @param tenant - tenant name
     * @param start - starting index
     * @param end - closing  index
     * @return - http response
     * @throws Exception
     */
    public HttpResponse getAllPaginatedPublishedAPIs(String tenant, String start, String end)
            throws Exception {
        checkAuthentication();
        return HttpRequestUtil.doGet(backendURL + "store/site/blocks/api/listing/ajax/list.jag?" +
                        "action=getAllPaginatedPublishedAPIs&tenant=" + tenant +
                        "&start=" + start + "&end=" + end
                        , requestHeaders);
    }

    /**
     * getAllPublishedAPIs for tenant
     * @param tenant - tenant name
     * @return - http response
     * @throws Exception
     */
    public HttpResponse getAllPublishedAPIs(String tenant)
            throws Exception {
        checkAuthentication();
        return HttpRequestUtil.doPost(new URL(backendURL +
                "store/site/blocks/api/listing/ajax/list.jag?action=getAllPublishedAPIs&tenant=" +
                tenant)
                , ""
                , requestHeaders);
    }

    /**
     * addApplication
     * @param application - application  name
     * @param tier - throttling tier
     * @param callbackUrl - callback url
     * @param description - description of app
     * @return - http response
     * @throws Exception
     */
    public HttpResponse addApplication(String application, String tier, String callbackUrl, String description)
            throws Exception {

        checkAuthentication();
        return HttpRequestUtil.doPost(new URL(backendURL +
                "store/site/blocks/application/application-add" +
                "/ajax/application-add.jag?action=addApplication&tier=" + tier + "&callbackUrl=" +
                callbackUrl + "&description=" + description + "&application=" + application), "", requestHeaders);

    }

    /**
     * get applications
     * @return - http response
     * @throws Exception
     */
    public HttpResponse getApplications() throws Exception {

        checkAuthentication();

        return HttpRequestUtil.doPost(new URL(backendURL + "store/site/blocks/application/" +
                "application-list/ajax/application-list.jag?action=getApplications"), "", requestHeaders);
    }

    /**
     * delete application
     * @param application - application name
     * @return - http response
     * @throws Exception
     */
    public HttpResponse removeApplication(String application) throws Exception {
        checkAuthentication();

        return HttpRequestUtil.doPost(new URL(backendURL + "store/site/blocks/application/" +
                "application-remove/ajax/application-remove.jag?action=removeApplication&application=" + application),
                "", requestHeaders);
    }

    /**
     * updateApplication
     * @param applicationOld - application name old
     * @param applicationNew - new  application name
     * @param callbackUrlNew - call back url
     * @param descriptionNew - updated description
     * @param tier - access tier
     * @return - http response
     * @throws Exception
     */
    public HttpResponse updateApplication(String applicationOld, String applicationNew,
                                          String callbackUrlNew, String descriptionNew, String tier) throws Exception {
        checkAuthentication();
        return HttpRequestUtil.doPost(new URL(backendURL +
                "store/site/blocks/application/application-update/ajax/application-update.jag?" +
                "action=updateApplication&applicationOld=" +
                applicationOld + "&applicationNew=" +
                applicationNew + "&callbackUrlNew=" +
                callbackUrlNew +
                "&descriptionNew=" + descriptionNew +
                "&tier=" + tier), "", requestHeaders);

    }

    /**
     * get all subscriptions
     * @return - http response
     * @throws Exception
     */
    public HttpResponse getAllSubscriptions()
            throws Exception {
        checkAuthentication();
        return HttpRequestUtil.doPost(new URL(backendURL +
                "store/site/blocks/subscription/subscription-list/ajax/subscription-list.jag?" +
                "action=getAllSubscriptions"), "", requestHeaders);

    }

    /**
     * unsubscribe from API
     * @param API - name of api
     * @param version - api version
     * @param provider - provider name
     * @param applicationId - application id
     * @return - http response
     * @throws Exception
     */
    public HttpResponse removeAPISubscription(String API, String version, String provider, String applicationId)
            throws Exception {
        checkAuthentication();

        return HttpRequestUtil.doPost(new URL(backendURL +
                "store/site/blocks/subscription/subscription-remove/ajax/subscription-remove.jag?action=removeSubscription&name=" +
                API + "&version=" + version + "&provider=" + provider + "&applicationId=" + applicationId), "", requestHeaders);

    }

    /**
     * get all tags of API
     * @return - http response
     * @throws Exception
     */
    public HttpResponse getAllTags() throws Exception {

        checkAuthentication();
        return  HttpRequestUtil.doPost(new URL(
                backendURL + "store/site/blocks/tag/tag-cloud/ajax/list.jag?action=getAllTags"), "", requestHeaders);

    }


    /**
     * add comment to api
     * @param name - name of api
     * @param version - api version
     * @param provider - provider name
     * @param comment - comment to  add
     * @return - http response
     * @throws Exception
     */
    public HttpResponse addComment(String name, String version, String provider, String comment) throws Exception {
        checkAuthentication();
        return HttpRequestUtil
                .doPost(new URL(backendURL + "store/site/blocks/comment/comment-add/ajax/comment-add.jag?" +
                        "action=addComment&name=" + name + "&version=" + version + "&provider=" +
                        provider + "&comment=" + comment), "", requestHeaders);

    }

    /**
     * check  comment  is enabled
     * @return - http response
     * @throws Exception
     */
    public HttpResponse isCommentActivated() throws Exception {

        checkAuthentication();

        return HttpRequestUtil
                .doGet(backendURL + "store/site/blocks/comment/comment-add/ajax/comment-add.jag?" +
                        "action=isCommentActivated", requestHeaders);
    }

    /**
     * get last added APi's
     * @param tenant - tenant name
     * @param limit - limit of result set
     * @return - http response
     * @throws Exception
     */
    public HttpResponse getRecentlyAddedAPIs(String tenant, String limit) throws Exception {

        checkAuthentication();
        return HttpRequestUtil.doPost(new URL(backendURL + "store/site/blocks/api/" +
                "recently-added/ajax/list.jag?action=getRecentlyAddedAPIs&tenant="
                + tenant + "&limit=" + limit), "", requestHeaders);
    }

    private String getApplicationId(String jsonStringOfApplications, String applicationName) throws Exception{
        String applicationId=null;
        JSONObject obj;
        try {
            obj = new JSONObject(jsonStringOfApplications);
            JSONArray arr = obj.getJSONArray("applications");
            for (int i = 0; i < arr.length(); i++)
            {
                String appName = arr.getJSONObject(i).getString("name");
                if(applicationName.equals(appName)){
                    applicationId = arr.getJSONObject(i).getString("id");
                }
            }
        } catch (JSONException e) {
            throw new  Exception("getting application Id failed ");
        }
        return applicationId;

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
        return HttpRequestUtil.sendGetRequest(backendURL + "store/apis/list"
                , "tag=" + apiTag + "&tenant=carbon.super");

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
        return HttpRequestUtil.doPost(new URL(backendURL +
                "store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag")
                , subscriptionRequest.generateRequestParameters()
                , requestHeaders);
    }


}
