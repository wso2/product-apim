/*
*Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.versioning;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIDesignBean;
import org.wso2.am.integration.test.utils.bean.APIImplementationBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.AddDocumentRequestBean;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APIPublisherRestClient {
    private static final Log log = LogFactory.getLog(APIPublisherRestClient.class);
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
        log.info("Login to Publisher " + backendURL + " as the user " + userName);

        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("action", "login"));
        urlParameters.add(new BasicNameValuePair("username", userName));
        urlParameters.add(new BasicNameValuePair("password", password));

        try {
            response = HttpClient.doPost(
                    backendURL + URL_SUFFIX + "/user/login/ajax/login.jag", requestHeaders, urlParameters);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to login to the publisher app ", e);
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
            return HTTPSClientUtils.doGet(
                    backendURL + URL_SUFFIX + "/user/login/ajax/login.jag?action=logout",
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Failed to logout from publisher."
                                                         + " Error: " + e.getMessage(), e);
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
            return HttpClient.doPost(
                    new URL(backendURL + URL_SUFFIX + "/item-add/ajax/add.jag"),
                    apiRequest.generateRequestParameters(),
                    requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to add API. Error: " + e.getMessage(), e);
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
            return HttpClient.doPost(
                    new URL(backendURL + URL_SUFFIX + "/overview/ajax/overview.jag"),
                    "action=createNewAPI&provider=" + provider + "&apiName=" + apiName + "&version="
                    + oldVersion + "&newVersion=" + newVersion + "&isDefaultVersion=" + isDefaultVersion,
                    requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable copy API - " + apiName + ". Error: " + e.getMessage()
                    , e);
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
            return HTTPSClientUtils.doPost(new URL(backendURL + URL_SUFFIX + "/item-add/ajax/add.jag"),
                                          apiRequest.generateRequestParameters("updateAPI"),
                                          requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to update API. Error: " + e.getMessage(), e);
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
            Thread.sleep(1000); // this is to make sure timestamps of current and next lifecycle states are different
            checkAuthentication();
            return HttpClient.doPost(
                    new URL(backendURL + "publisher/site/blocks/life-cycles/ajax/life-cycles.jag"),
                    updateRequest.generateRequestParameters(),
                    requestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to update API. Error: " + e.getMessage(), e);
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
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "publisher/site/blocks/listing/ajax/item-list.jag"),
                    "action=getAPI&name=" + apiName + "&version=1.0.0&provider=" + provider + "",
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to get API " + apiName
                                                         + ". Error: " + e.getMessage(), e);
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
            return HttpClient.doPost(
                    new URL(backendURL + "publisher/site/blocks/item-add/ajax/remove.jag"),
                    "action=removeAPI&name=" + apiName + "&version=" + version + "&provider=" + provider,
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to get API - " + apiName
                                                         + ". Error: " + e.getMessage(), e);
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
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "publisher/site/blocks/documentation/ajax/docs.jag"),
                    "action=removeDocumentation" + "&provider=" + provider + "&apiName=" +
                    apiName + "&version=" + version + "&docName=" + docName + "&docType=" +
                    docType, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to remove document from API - " + apiName
                                                         + ". Error: " + e.getMessage(), e);
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
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "publisher/site/blocks/tokens/ajax/revokeToken.jag"),
                    "action=revokeAccessToken" + "&accessToken=" + accessToken + "&authUser=" +
                    authUser + "&consumerKey=" + consumerKey, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to revoke access token"
                                                         + ". Error: " + e.getMessage(), e);
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
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "publisher/site/blocks/tiers/ajax/tiers.jag"),
                    "action=updatePermissions" + "&tierName=" + tierName + "&permissiontype=" +
                    permissionType + "&roles=" + roles, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to update permission."
                                                         + " Error: " + e.getMessage(), e);
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
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "publisher/site/blocks/item-design/ajax/add.jag"),
                    "action=manage" + "&provider=" + provider + "&name=" + apiName + "&version=" +
                    version + "&swagger=" + swaggerRes, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to update resource of API - " + apiName
                                                         + ". Error: " + e.getMessage(), e);
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
            return HttpClient.doPost(
                    new URL(backendURL + "publisher/site/blocks/listing/ajax/item-list.jag"),
                    "action=getAPI&name=" + apiName + "&version=" + version + "&provider=" + provider + "",
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to retrieve API information - " + apiName
                                                         + ". Error: " + e.getMessage(), e);
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
    public HttpResponse checkValidEndpoint(String type, String endpointUrl, String providerName, String apiName,
            String apiVersion) throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(new URL(backendURL + "publisher/site/blocks/item-add/ajax/add.jag"),
                    "action=isURLValid&" + "type=" + type + "&url=" + endpointUrl + "&providerName=" + providerName
                            + "&apiName=" + apiName + "&apiVersion=" + apiVersion, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Check for valid endpoint fails for " + endpointUrl
                                                         + ". Error: " + e.getMessage(), e);
        }
    }


    /**
     * Change the API Lifecycle status to Publish with the option of Re-subscription is required or not
     *
     * @param apiIdentifier           - Instance of APIIdentifier
     * @param isRequireReSubscription - true if Re-subscription is required else false.
     * @return HttpResponse - Response of the API publish event
     * @throws APIManagerIntegrationTestException - Exception Throws in checkAuthentication() and when do the REST
     *                                            service calls to do the lifecycle change.
     */
    public HttpResponse changeAPILifeCycleStatusToPublish(APIIdentifier apiIdentifier, boolean isRequireReSubscription)
            throws APIManagerIntegrationTestException {
        try {
            Thread.sleep(1000); // this is to make sure timestamps of current and next lifecycle states are different
            checkAuthentication();
            APILifeCycleStateRequest publishUpdateRequest =
                    new APILifeCycleStateRequest(apiIdentifier.getApiName(), apiIdentifier.getProviderName(),
                            APILifeCycleState.PUBLISHED);
            publishUpdateRequest.setVersion(apiIdentifier.getVersion());
            if (isRequireReSubscription) {
                publishUpdateRequest.setRequireResubscription("true");
            }
            String requestParameters = publishUpdateRequest.generateRequestParameters();
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "/publisher/site/blocks/life-cycles/ajax/life-cycles.jag"), requestParameters,
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when change he lifecycle to publish"
                                                         + ". Error: " + e.getMessage(), e);
        }
    }

    /**
     * Get the API information  for the given API Name,API Version and API Provider
     *
     * @param apiName  - Name of the API
     * @param provider - Provider Name of the API
     * @param version  - Version of the API
     * @return HttpResponse -  Response of the getAPI request
     * @throws APIManagerIntegrationTestException - Exception Throws in checkAuthentication() and when do the REST
     *                                            service calls to get the API information.
     */
    public HttpResponse getApi(String apiName, String provider, String version)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "/publisher/site/blocks/listing/ajax/item-list.jag"), "action=getAPI&name=" +
                            apiName + "&version=" + version + "&provider=" + provider + "", requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when retrieving a API"
                                                         + ". Error: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieve the Tier Permission Page
     *
     * @return HttpResponse - Response that contains the Tier Permission Page
     * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication() method and
     *                                            HTTPSClientUtils.doGet() method call
     */
    public HttpResponse getTierPermissionsPage() throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doGet(backendURL + "/publisher/site/pages/tiers.jag", requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when retrieving the Tier Permissions page"
                                                         + ". Error: " + e.getMessage(), e);
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
     *                                            HTTPSClientUtils.doGet() method call
     */
    public HttpResponse getAPIManagePage(String apiName, String provider, String version)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doGet(
                    backendURL + "/publisher/manage?name=" + apiName + "&version=" + version + "&provider=" + provider,
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when retrieving the API Manage page"
                                                         + ". Error: " + e.getMessage(), e);
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
     *                                            HTTPSClientUtils.doGet() method call
     */
    public HttpResponse getAPIInformationPage(String apiName, String provider, String version)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(new URL(backendURL + "/publisher/info"),
                    "name=" + apiName + "&version=" + version + "&provider=" + provider, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when retrieving the API Information page"
                                                         + ". Error: " + e.getMessage(), e);
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
     *                                            HTTPSClientUtils.doPost() method call
     */
    public HttpResponse addDocument(String apiName, String version, String provider, String docName, String docType,
                                    String sourceType, String docUrl, String summary, String docLocation,String mimeType, String newType)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "publisher/site/blocks/documentation/ajax/docs.jag"),
                    "action=addDocumentation&provider=" + provider + "&apiName=" + apiName + "&version=" + version +
                            "&docName=" + docName + "&docType=" + docType + "&sourceType=" + sourceType + "&docUrl=" + docUrl +
                            "&summary=" + summary + "&docLocation=" + docLocation + "&mimeType=" + mimeType+ "&newType="
                    + newType, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when Adding document to a API"
                                                         + ". Error: " + e.getMessage(), e);
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
     *                                            HTTPSClientUtils.doPost() method call
     */
    public HttpResponse addDocument(String apiName, String version, String provider, String docName, String docType,
                                    String sourceType, String docUrl, String summary, String docLocation)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "/publisher/site/blocks/documentation/ajax/docs.jag"),
                    "action=addDocumentation&provider=" + provider + "&apiName=" + apiName + "&version=" + version +
                            "&docName=" + docName + "&docType=" + docType + "&sourceType=" + sourceType + "&docUrl=" + docUrl +
                            "=&summary=" + summary + "&docLocation=" + docLocation, requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when Adding document to a API"
                                                         + ". Error: " + e.getMessage(), e);
        }
    }


    /**
     * Adding a Document to a API using AddDocumentRequestBean
     *
     * @param addDocRequestBean - Bean that contains all the values that needed to create a Document.
     * @return HttpResponse -  Response  with Document adding result.
     * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication() method and
     *                                            HTTPSClientUtils.doPost() method call
     */
    public HttpResponse addDocument(AddDocumentRequestBean addDocRequestBean) throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "/publisher/site/blocks/documentation/ajax/docs.jag"),
                    "action=addDocumentation&provider=" + addDocRequestBean.getApiProvider() + "&apiName=" +
                            addDocRequestBean.getApiName() + "&version=" + addDocRequestBean.getApiVersion() + "&docName=" +
                            addDocRequestBean.getDocName() + "&docType=" + addDocRequestBean.getDocType() + "&sourceType=" +
                            addDocRequestBean.getDocSourceType() + "&docUrl=" + addDocRequestBean.getDocUrl() + "=&summary=" +
                            addDocRequestBean.getDocSummary() + "&docLocation=" + addDocRequestBean.getDocLocation() +
                            "&mimeType=" + addDocRequestBean.getMimeType() +
                            "&optionsRadios=" + addDocRequestBean.getDocType() + "&optionsRadios1=" +
                    addDocRequestBean.getDocSourceType(), requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when Adding document to a API"
                                                         + ". Error: " + e.getMessage(), e);
        }
    }


    /**
     * Retrieve the All APIs available for the user in Publisher.
     *
     * @return HttpResponse - Response that contains all available APIs for the user
     * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication() method and
     *                                            HTTPSClientUtils.doGet() method call
     */
    public HttpResponse getAllAPIs() throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doGet(
                    backendURL + "/publisher/site/blocks/listing/ajax/item-list.jag?action=getAllAPIs", requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when Retrieve the All APIs available for " +
                                                         "the user in Publisher. Error: " + e.getMessage(), e);
        }
    }


    /**
     * Add a API using APICreationRequestBean object
     *
     * @param creationRequestBean - Instance of APICreationRequestBean object with all needed information to create the API.
     * @return HttpResponse - Response that contains the result of APi creation activity.
     * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication() method and
     *                                            HTTPSClientUtils.doPost() method call
     */
    public HttpResponse addAPI(APICreationRequestBean creationRequestBean) throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "/publisher/site/blocks/item-add/ajax/add.jag"),
                    creationRequestBean.generateRequestParameters(), requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when Adding the New API. "
                                                         + "Error: " + e.getMessage(), e);
        }
    }

    /**
     * Update a API using APICreationRequestBean object
     *
     * @param creationRequestBean - Instance of APICreationRequestBean object with all needed information to Update the API.
     * @return HttpResponse - Response that contains the result of APi creation activity.
     * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication() method and
     *                                            HTTPSClientUtils.doPost() method call
     */
    public HttpResponse updateAPI(APICreationRequestBean creationRequestBean) throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "/publisher/site/blocks/item-add/ajax/add.jag"),
                    creationRequestBean.generateRequestParameters("updateAPI"), requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when Retrieve the All APIs available " +
                    "for the user in Publisher. Error: " + e.getMessage(), e);
        }
    }


    /**
     * Design an API using APIDesignBean object
     *
     * @param designBean -Instance of APIDesignBean object with all needed information to create an API up to design level.
     * @return HttpResponse -Response that contains the results of API creation up to design level
     * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication() method and
     *                                            HTTPSClientUtils.doPost() method call
     */
    public HttpResponse designAPI(APIDesignBean designBean)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "/publisher/site/blocks/item-design/ajax/add.jag"),
                    designBean.generateRequestParameters("design"), requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to design API. Error: " + e.getMessage(), e);
        }
    }


    public HttpResponse implement(APIImplementationBean implementationBean)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(
                    new URL(backendURL + "/publisher/site/blocks/item-design/ajax/add.jag?"),
                    implementationBean.generateRequestParameters("implement"), requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to prototype API. Error: " + e.getMessage(), e);
        }
    }


    /**
     * Retrieve implementation page for the requested API
     *
     * @param apiName  - Name of the API
     * @param provider - Provider of the API
     * @param version  -Version of the API
     * @return - Response that contains the implementation page of the API
     * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication() method and
     *                                            HTTPSClientUtils.doGet() method call
     */
    public HttpResponse getAPIImplementPage(String apiName, String provider, String version)
            throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doGet(
                    backendURL + "/publisher/prototype?name=" + apiName + "&version=" + version + "&provider=" + provider,
                    requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when retrieving the API Implement page"
                                                         + ". Error: " + e.getMessage(), e);
        }
    }

    /**
     * Validate API Name when creating a new API
     * @param apiName - Name of the API
     * @return - Response that contains the API is valid or not..
     * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication()
     *                                           method and HTTPSClientUtils.doPost() method call
     */
    public HttpResponse checkValidAPIName(String apiName) throws APIManagerIntegrationTestException{
        try {
            checkAuthentication();
            return  HTTPSClientUtils.doPost( new URL(backendURL + "/publisher/site/blocks/item-add/ajax/add.jag"),
                    "action=isAPINameExist&apiName=" + apiName , requestHeaders);
        }catch (Exception e){
            throw new APIManagerIntegrationTestException("Exeption when adding a new API with existing API name"
                                                         + ". Error: " + e.getMessage(),e);

        }
    }

    /**
     * @return - Response that contains all the available tiers
     * @throws APIManagerIntegrationTestException
     */
    public HttpResponse getTiers() throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(new URL(backendURL + "/publisher/site/blocks/item-add/ajax/add.jag"),
                                           "action=getTiers" ,requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when retrieving the Tier Permissions page"
                                                         + ". Error: " + e.getMessage(), e);
        }
    }

    /**
     *
     * @param role role
     * @return HttpResponse
     * @throws APIManagerIntegrationTestException
     */
    public HttpResponse validateRoles(String role) throws APIManagerIntegrationTestException {
        try {
            checkAuthentication();
            return HTTPSClientUtils.doPost(new URL(backendURL + "/publisher/site/blocks/item-add/ajax/add.jag"),
                                           "action=validateRoles&roles=" + role ,requestHeaders);
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Exception when retrieving the Tier Permissions page"
                                                         + ". Error: " + e.getMessage(), e);
        }
    }


}
