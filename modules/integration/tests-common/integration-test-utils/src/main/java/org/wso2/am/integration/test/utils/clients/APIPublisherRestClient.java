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
    private static final String URL_SUFFIX = "publisher/site/blocks";
    private Map<String, String> requestHeaders = new HashMap<String, String>();

    /**
     * construct of API rest client
     *
     * @param backendURL - backend URL of the publisher Jaggery app
     */
    public APIPublisherRestClient(String backendURL) {
        this.backendURL = backendURL;
        if (requestHeaders.get("Content-Type") == null) {
            this.requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        }
    }

    /**
     * login to publisher app
     *
     * @param userName - provided user name
     * @param password - password
     * @return HTTP response object
     * @throws APIManagerIntegrationTestException - Throws if user cannot login to the publisher
     */
    public HttpResponse login(String userName, String password)
            throws APIManagerIntegrationTestException {
        HttpResponse response;
        try {
            response =
                    HttpRequestUtil.doPost(
                            new URL(backendURL + URL_SUFFIX + "/user/login/ajax/login.jag"),
                            "action=login&username=" + userName + "&password=" + password + "",
                            requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to login to the publisher ", e);
        }

        String session = getSession(response.getHeaders());

        if (session == null) {
            throw new APIManagerIntegrationTestException("No session cookie found with response");
        }
        setSession(session);
        return response;
    }

    /**
     * log out from publisher
     *
     * @return http response object
     * @throws APIManagerIntegrationTestException - Throws if logout fails
     */
    public HttpResponse logout() throws APIManagerIntegrationTestException {
        try {
            return HttpRequestUtil.doGet(
                    backendURL + URL_SUFFIX + "/user/login/ajax/login.jag?action=logout",
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Failed to logout from publisher ", e);
        }
    }

    /**
     * Facilitate add API into publisher node
     *
     * @param apiRequest - Constructed API request object
     * @return http response object
     * @throws APIManagerIntegrationTestException - Throws if API addition fails
     */
    public HttpResponse addAPI(APIRequest apiRequest) throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + URL_SUFFIX + "/item-add/ajax/add.jag"),
                    apiRequest.generateRequestParameters(),
                    requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to add API ", e);
        }
    }

    /**
     * copy API from existing API
     *
     * @param provider         - name of the provider , ex: admin
     * @param apiName          - API name
     * @param oldVersion       - existing version
     * @param newVersion       - new version
     * @param isDefaultVersion - check default version
     * @return - http response object
     * @throws APIManagerIntegrationTestException - Throws if error occurred at API copy operation
     */
    public HttpResponse copyAPI(String provider, String apiName, String oldVersion,
                                String newVersion, String isDefaultVersion)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + URL_SUFFIX + "/overview/ajax/overview.jag"),
                    "action=createNewAPI&provider=" + provider + "&apiName=" + apiName + "&version="
                    + oldVersion + "&newVersion=" + newVersion + "&isDefaultVersion=" + isDefaultVersion,
                    requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable copy API - " + apiName, e);
        }
    }

    /**
     * Facilitate update API
     *
     * @param apiRequest - constructed API request object
     * @return http response object
     * @throws APIManagerIntegrationTestException - throws if update API fails
     */
    public HttpResponse updateAPI(APIRequest apiRequest)
            throws Exception {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(new URL(backendURL + URL_SUFFIX + "/item-add/ajax/add.jag"),
                                          apiRequest.generateRequestParameters("updateAPI"),
                                          requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to update API ", e);
        }

    }

    /**
     * change status of a created API
     *
     * @param updateRequest - APILifeCycleStateRequest object
     * @return http response object
     * @throws APIManagerIntegrationTestException - throws if change lifecycle state fails
     */
    public HttpResponse changeAPILifeCycleStatus(APILifeCycleStateRequest updateRequest)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "publisher/site/blocks/life-cycles/ajax/life-cycles.jag"),
                    updateRequest.generateRequestParameters(),
                    requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to update API ", e);
        }

    }

    /**
     * Method to get API information
     *
     * @param apiName  - API name
     * @param provider - name of the provider
     * @return http response object
     * @throws APIManagerIntegrationTestException - Throws if api information cannot be retrieved.
     */
    public HttpResponse getAPI(String apiName, String provider)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "publisher/site/blocks/listing/ajax/item-list.jag"),
                    "action=getAPI&name=" + apiName + "&version=1.0.0&provider=" + provider + "",
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to get API " + apiName, e);
        }

    }

    /**
     * delete API
     *
     * @param apiName  - API name
     * @param version  - API version
     * @param provider - name of the provider
     * @return http response object
     * @throws APIManagerIntegrationTestException - Throws if API delete fails
     */
    public HttpResponse deleteAPI(String apiName, String version, String provider)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "publisher/site/blocks/item-add/ajax/remove.jag"),
                    "action=removeAPI&name=" + apiName + "&version=" + version + "&provider=" + provider,
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to get API - " + apiName, e);
        }
    }

    private String getSession(Map<String, String> responseHeaders) {
        return responseHeaders.get("Set-Cookie");
    }

    private String setSession(String session) {
        return requestHeaders.put("Cookie", session);
    }

    /**
     * check whether user is authorized
     *
     * @throws APIManagerIntegrationTestException - Throws if no session cookie found.
     */
    private void checkAuthentication() throws APIManagerIntegrationTestException {
        if (requestHeaders.get("Cookie") == null) {
            throw new APIManagerIntegrationTestException("No Session Cookie found. Please login first");
        }
    }

    /**
     * Add document to API
     *
     * @param apiName     - API name
     * @param version     - API version
     * @param provider    - name of the provider
     * @param docName     - document name
     * @param docType     - type of document
     * @param sourceType  - source ( url,file)
     * @param docUrl      - URL of doc
     * @param summary     - summary of doc
     * @param docLocation - location
     * @return http response object
     * @throws APIManagerIntegrationTestException - throws if add document to API fails
     */
    public HttpResponse addDocument(String apiName, String version, String provider, String docName,
                                    String docType, String sourceType, String docUrl,
                                    String summary, String docLocation)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "publisher/site/blocks/documentation/ajax/docs.jag"),
                    "action=addDocumentation" + "&mode=''&provider=" + provider + "&apiName=" +
                    apiName + "&version=" + version + "&docName=" + docName + "&docType=" +
                    docType + "&sourceType=" + sourceType + "&docUrl=" + docUrl +
                    summary + "&docLocation=" + docLocation, requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to add document to API - " + apiName, e);
        }

    }

    /**
     * Remove document
     *
     * @param apiName  - API name
     * @param version  - API version
     * @param provider - name of the provider
     * @param docName  - document name
     * @param docType  - document type
     * @return http response object
     * @throws APIManagerIntegrationTestException - Throws if remove API document fails
     */
    public HttpResponse removeDocumentation(String apiName, String version, String provider,
                                            String docName, String docType)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "publisher/site/blocks/documentation/ajax/docs.jag"),
                    "action=removeDocumentation" + "&provider=" + provider + "&apiName=" +
                    apiName + "&version=" + version + "&docName=" + docName + "&docType=" +
                    docType, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to remove document from API - " + apiName, e);
        }
    }

    /**
     * revoke access token
     *
     * @param accessToken - access token already  received
     * @param consumerKey -  consumer key returned
     * @param authUser    - user name
     * @return http response object
     * @throws APIManagerIntegrationTestException - throws if access token revoke fails
     */
    public HttpResponse revokeAccessToken(String accessToken, String consumerKey, String authUser)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "publisher/site/blocks/tokens/ajax/revokeToken.jag"),
                    "action=revokeAccessToken" + "&accessToken=" + accessToken + "&authUser=" +
                    authUser + "&consumerKey=" + consumerKey, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to revoke access token", e);
        }
    }


    /**
     * update permissions to API access
     *
     * @param tierName       - name of api throttling tier
     * @param permissionType - permission type
     * @param roles          - roles of permission
     * @return http response object
     * @throws APIManagerIntegrationTestException - throws if permission update fails
     */
    public HttpResponse updatePermissions(String tierName, String permissionType, String roles)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "publisher/site/blocks/tiers/ajax/tiers.jag"),
                    "action=updatePermissions" + "&tierName=" + tierName + "&permissiontype=" +
                    permissionType + "&roles=" + roles, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to update permission ", e);
        }
    }

    /**
     * Update resources of API
     *
     * @param provider   - provider name of creator
     * @param apiName    - name of api
     * @param version    - api version
     * @param swaggerRes - swagger doc
     * @return http response object
     * @throws APIManagerIntegrationTestException - throws if API resource update fails
     */
    public HttpResponse updateResourceOfAPI(String provider, String apiName, String version,
                                            String swaggerRes)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            this.requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "publisher/site/blocks/item-design/ajax/add.jag"),
                    "action=manage" + "&provider=" + provider + "&name=" + apiName + "&version=" +
                    version + "&swagger=" + swaggerRes, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to update resource of API - " + apiName, e);
        }
    }


    /**
     * Get the API information for the given API Name, API Version and API Provider
     *
     * @param apiName  Name of the API
     * @param provider Provider Name of the API
     * @param version  Version of the API
     * @return http response object
     * @throws APIManagerIntegrationTestException - Unable to get API information
     */
    public HttpResponse getAPI(String apiName, String provider, String version)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "publisher/site/blocks/listing/ajax/item-list.jag"),
                    "action=getAPI&name=" + apiName + "&version=" + version + "&provider=" + provider + "",
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to retrieve API information - " + apiName, e);
        }
    }

    /**
     * Change the API Lifecycle status to Publish with the option of Re-subscription is required or not
     *
     * @param apiIdentifier           - Instance of APIIdentifier
     * @param isRequireReSubscription - true if Re-subscription is required else false.
     * @return HttpResponse           - Response of the API publish event
     * @throws APIManagerIntegrationTestException - Throws if lifecycle publish fails
     */
    public HttpResponse changeAPILifeCycleStatusToPublish(APIIdentifier apiIdentifier,
                                                          boolean isRequireReSubscription)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            APILifeCycleStateRequest publishUpdateRequest =
                    new APILifeCycleStateRequest(apiIdentifier.getApiName(),
                                                 apiIdentifier.getProviderName(),
                                                 APILifeCycleState.PUBLISHED);

            publishUpdateRequest.setVersion(apiIdentifier.getVersion());
            String requestParameters = publishUpdateRequest.generateRequestParameters();

            if (isRequireReSubscription) {
                requestParameters += "&requireResubscription=true";
            }
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "publisher/site/blocks/life-cycles/ajax/life-cycles.jag"),
                    requestParameters, requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to publish API - " +
                                                         apiIdentifier.getApiName(), e);
        }

    }

    /**
     * Get the API information for the given API Name,API Version and API Provider
     *
     * @param apiName  - Name of the API
     * @param provider - Provider Name of the API
     * @param version  - Version of the API
     * @return HttpResponse -  Response of the getAPI request
     * @throws APIManagerIntegrationTestException - Throws if get API fails
     */
    public HttpResponse getApi(String apiName, String provider, String version)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "publisher/site/blocks/listing/ajax/item-list.jag"),
                    "action=getAPI&name=" + apiName + "&version=" + version + "&provider=" +
                    provider + "", requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to get information for the API " +
                                                         apiName, e);
        }
    }

    /**
     * Check whether the Endpoint is valid
     *
     * @param endpointUrl url of the endpoint
     * @param type        type of Endpoint
     * @return HttpResponse -  Response of the getAPI request
     * @throws APIManagerIntegrationTestException - Check for valid endpoint fails.
     */
    public HttpResponse checkValidEndpoint(String type, String endpointUrl)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HttpRequestUtil.doPost(
                    new URL(backendURL + "publisher/site/blocks/item-add/ajax/add.jag"),
                    "action=isURLValid&" + "type=" + type + "&url=" + endpointUrl, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Check for valid endpoint fails for " +
                                                         endpointUrl, e);
        }
    }
}
