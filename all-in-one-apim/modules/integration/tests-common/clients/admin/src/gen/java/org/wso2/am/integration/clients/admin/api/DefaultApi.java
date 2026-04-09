/*
 * WSO2 API Manager - Admin
 * This document specifies a **RESTful API** for WSO2 **API Manager** - **Admin Portal**. Please see [full OpenAPI Specification](https://raw.githubusercontent.com/wso2/carbon-apimgt/master/components/apimgt/org.wso2.carbon.apimgt.rest.api.admin.v1/src/main/resources/admin-api.yaml) of the API which is written using [OAS 3.0](http://swagger.io/) specification.  # Authentication The Admin REST API is protected using OAuth2 and access control is achieved through scopes. Before you start invoking the the API you need to obtain an access token with the required scopes. This guide will walk you through the steps that you will need to follow to obtain an access token. First you need to obtain the consumer key/secret key pair by calling the dynamic client registration (DCR) endpoint. You can add your preferred grant types in the payload. A sample payload is shown below. ```   {   \"callbackUrl\":\"www.example.com\",   \"clientName\":\"rest_api_admin\",   \"owner\":\"admin\",   \"grantType\":\"client_credentials password refresh_token\",   \"saasApp\":true   } ``` Create a file (payload.json) with the above sample payload, and use the cURL shown bellow to invoke the DCR endpoint. Authorization header of this should contain the base64 encoded admin username and password. **Format of the request** ```   curl -X POST -H \"Authorization: Basic Base64(admin_username:admin_password)\" -H \"Content-Type: application/json\"   \\ -d @payload.json https://<host>:<servlet_port>/client-registration/v0.17/register ``` **Sample request** ```   curl -X POST -H \"Authorization: Basic YWRtaW46YWRtaW4=\" -H \"Content-Type: application/json\"   \\ -d @payload.json https://localhost:9443/client-registration/v0.17/register ``` Following is a sample response after invoking the above curl. ``` { \"clientId\": \"fOCi4vNJ59PpHucC2CAYfYuADdMa\", \"clientName\": \"rest_api_admin\", \"callBackURL\": \"www.example.com\", \"clientSecret\": \"a4FwHlq0iCIKVs2MPIIDnepZnYMa\", \"isSaasApplication\": true, \"appOwner\": \"admin\", \"jsonString\": \"{\\\"grant_types\\\":\\\"client_credentials password refresh_token\\\",\\\"redirect_uris\\\":\\\"www.example.com\\\",\\\"client_name\\\":\\\"rest_api_admin\\\"}\", \"jsonAppAttribute\": \"{}\", \"tokenType\": null } ``` Note that in a distributed deployment or IS as KM separated environment to invoke RESTful APIs (product APIs), users must generate tokens through API-M Control Plane's token endpoint. The tokens generated using third party key managers, are to manage end-user authentication when accessing APIs.  Next you must use the above client id and secret to obtain the access token. We will be using the password grant type for this, you can use any grant type you desire. You also need to add the proper **scope** when getting the access token. All possible scopes for Admin REST API can be viewed in **OAuth2 Security** section of this document and scope for each resource is given in **authorizations** section of resource documentation. Following is the format of the request if you are using the password grant type. ``` curl -k -d \"grant_type=password&username=<admin_username>&password=<admin_passowrd>&scope=<scopes seperated by space>\" \\ -H \"Authorization: Basic base64(cliet_id:client_secret)\" \\ https://<host>:<server_port>/oauth2/token ``` **Sample request** ``` curl https://localhost:9443/oauth2/token -k \\ -H \"Authorization: Basic Zk9DaTR2Tko1OVBwSHVjQzJDQVlmWXVBRGRNYTphNEZ3SGxxMGlDSUtWczJNUElJRG5lcFpuWU1h\" \\ -d \"grant_type=password&username=admin&password=admin&scope=apim:admin apim:tier_view\" ``` Shown below is a sample response to the above request. ``` { \"access_token\": \"e79bda48-3406-3178-acce-f6e4dbdcbb12\", \"refresh_token\": \"a757795d-e69f-38b8-bd85-9aded677a97c\", \"scope\": \"apim:admin apim:tier_view\", \"token_type\": \"Bearer\", \"expires_in\": 3600 } ``` Now you have a valid access token, which you can use to invoke an API. Navigate through the API descriptions to find the required API, obtain an access token as described above and invoke the API with the authentication header. If you use a different authentication mechanism, this process may change.  # Try out in Postman If you want to try-out the embedded postman collection with \"Run in Postman\" option, please follow the guidelines listed below. * All of the OAuth2 secured endpoints have been configured with an Authorization Bearer header with a parameterized access token. Before invoking any REST API resource make sure you run the `Register DCR Application` and `Generate Access Token` requests to fetch an access token with all required scopes. * Make sure you have an API Manager instance up and running. * Update the `basepath` parameter to match the hostname and port of the APIM instance.  [![Run in Postman](https://run.pstmn.io/button.svg)](https://app.getpostman.com/run-collection/32294946-71bea2bc-f808-4208-a4f6-861ede6f0434) 
 *
 * The version of the OpenAPI document: v4
 * Contact: architecture@wso2.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package org.wso2.am.integration.clients.admin.api;

import org.wso2.am.integration.clients.admin.ApiCallback;
import org.wso2.am.integration.clients.admin.ApiClient;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.Configuration;
import org.wso2.am.integration.clients.admin.Pair;
import org.wso2.am.integration.clients.admin.ProgressRequestBody;
import org.wso2.am.integration.clients.admin.ProgressResponseBody;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;


import org.wso2.am.integration.clients.admin.api.dto.ContentPublishStatusDTO;
import org.wso2.am.integration.clients.admin.api.dto.ContentPublishStatusResponseDTO;
import org.wso2.am.integration.clients.admin.api.dto.ErrorDTO;
import java.io.File;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultApi {
    private ApiClient localVarApiClient;

    public DefaultApi() {
        this(Configuration.getDefaultApiClient());
    }

    public DefaultApi(ApiClient apiClient) {
        this.localVarApiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return localVarApiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.localVarApiClient = apiClient;
    }

    /**
     * Build call for deleteOrgTheme
     * @param id  (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Successfully deleted </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call deleteOrgThemeCall(String id, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/org-themes/{id}"
            .replaceAll("\\{" + "id" + "\\}", localVarApiClient.escapeString(id.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();
        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        String[] localVarAuthNames = new String[] { "OAuth2Security" };
        return localVarApiClient.buildCall(localVarPath, "DELETE", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    @SuppressWarnings("rawtypes")
    private okhttp3.Call deleteOrgThemeValidateBeforeCall(String id, final ApiCallback _callback) throws ApiException {
        
        // verify the required parameter 'id' is set
        if (id == null) {
            throw new ApiException("Missing the required parameter 'id' when calling deleteOrgTheme(Async)");
        }
        

        okhttp3.Call localVarCall = deleteOrgThemeCall(id, _callback);
        return localVarCall;

    }

    /**
     * Delete an org theme
     * Deletes the org theme for the given ID.
     * @param id  (required)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Successfully deleted </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public void deleteOrgTheme(String id) throws ApiException {
        deleteOrgThemeWithHttpInfo(id);
    }

    /**
     * Delete an org theme
     * Deletes the org theme for the given ID.
     * @param id  (required)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Successfully deleted </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<Void> deleteOrgThemeWithHttpInfo(String id) throws ApiException {
        okhttp3.Call localVarCall = deleteOrgThemeValidateBeforeCall(id, null);
        return localVarApiClient.execute(localVarCall);
    }

    /**
     * Delete an org theme (asynchronously)
     * Deletes the org theme for the given ID.
     * @param id  (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Successfully deleted </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call deleteOrgThemeAsync(String id, final ApiCallback<Void> _callback) throws ApiException {

        okhttp3.Call localVarCall = deleteOrgThemeValidateBeforeCall(id, _callback);
        localVarApiClient.executeAsync(localVarCall, _callback);
        return localVarCall;
    }
    /**
     * Build call for getOrgThemeContent
     * @param id  (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Returns the org theme zip </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call getOrgThemeContentCall(String id, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/org-themes/{id}/content"
            .replaceAll("\\{" + "id" + "\\}", localVarApiClient.escapeString(id.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();
        final String[] localVarAccepts = {
            "application/zip", "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        String[] localVarAuthNames = new String[] { "OAuth2Security" };
        return localVarApiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    @SuppressWarnings("rawtypes")
    private okhttp3.Call getOrgThemeContentValidateBeforeCall(String id, final ApiCallback _callback) throws ApiException {
        
        // verify the required parameter 'id' is set
        if (id == null) {
            throw new ApiException("Missing the required parameter 'id' when calling getOrgThemeContent(Async)");
        }
        

        okhttp3.Call localVarCall = getOrgThemeContentCall(id, _callback);
        return localVarCall;

    }

    /**
     * Retrieve org theme as zip
     * Returns the org theme as a zip file for the given ID.
     * @param id  (required)
     * @return File
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Returns the org theme zip </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public File getOrgThemeContent(String id) throws ApiException {
        ApiResponse<File> localVarResp = getOrgThemeContentWithHttpInfo(id);
        return localVarResp.getData();
    }

    /**
     * Retrieve org theme as zip
     * Returns the org theme as a zip file for the given ID.
     * @param id  (required)
     * @return ApiResponse&lt;File&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Returns the org theme zip </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<File> getOrgThemeContentWithHttpInfo(String id) throws ApiException {
        okhttp3.Call localVarCall = getOrgThemeContentValidateBeforeCall(id, null);
        Type localVarReturnType = new TypeToken<File>(){}.getType();
        return localVarApiClient.execute(localVarCall, localVarReturnType);
    }

    /**
     * Retrieve org theme as zip (asynchronously)
     * Returns the org theme as a zip file for the given ID.
     * @param id  (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Returns the org theme zip </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call getOrgThemeContentAsync(String id, final ApiCallback<File> _callback) throws ApiException {

        okhttp3.Call localVarCall = getOrgThemeContentValidateBeforeCall(id, _callback);
        Type localVarReturnType = new TypeToken<File>(){}.getType();
        localVarApiClient.executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }
    /**
     * Build call for getOrgThemes
     * @param publish Filter themes based on published status (optional)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> List of org themes </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call getOrgThemesCall(Boolean publish, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/org-themes";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (publish != null) {
            localVarQueryParams.addAll(localVarApiClient.parameterToPair("publish", publish));
        }

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();
        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        String[] localVarAuthNames = new String[] { "OAuth2Security" };
        return localVarApiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    @SuppressWarnings("rawtypes")
    private okhttp3.Call getOrgThemesValidateBeforeCall(Boolean publish, final ApiCallback _callback) throws ApiException {
        

        okhttp3.Call localVarCall = getOrgThemesCall(publish, _callback);
        return localVarCall;

    }

    /**
     * Retrieve UUIDs of org-theme contents
     * Returns the UUIDs of org-theme contents and their publish status.
     * @param publish Filter themes based on published status (optional)
     * @return List&lt;ContentPublishStatusResponseDTO&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> List of org themes </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public List<ContentPublishStatusResponseDTO> getOrgThemes(Boolean publish) throws ApiException {
        ApiResponse<List<ContentPublishStatusResponseDTO>> localVarResp = getOrgThemesWithHttpInfo(publish);
        return localVarResp.getData();
    }

    /**
     * Retrieve UUIDs of org-theme contents
     * Returns the UUIDs of org-theme contents and their publish status.
     * @param publish Filter themes based on published status (optional)
     * @return ApiResponse&lt;List&lt;ContentPublishStatusResponseDTO&gt;&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> List of org themes </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<List<ContentPublishStatusResponseDTO>> getOrgThemesWithHttpInfo(Boolean publish) throws ApiException {
        okhttp3.Call localVarCall = getOrgThemesValidateBeforeCall(publish, null);
        Type localVarReturnType = new TypeToken<List<ContentPublishStatusResponseDTO>>(){}.getType();
        return localVarApiClient.execute(localVarCall, localVarReturnType);
    }

    /**
     * Retrieve UUIDs of org-theme contents (asynchronously)
     * Returns the UUIDs of org-theme contents and their publish status.
     * @param publish Filter themes based on published status (optional)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> List of org themes </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call getOrgThemesAsync(Boolean publish, final ApiCallback<List<ContentPublishStatusResponseDTO>> _callback) throws ApiException {

        okhttp3.Call localVarCall = getOrgThemesValidateBeforeCall(publish, _callback);
        Type localVarReturnType = new TypeToken<List<ContentPublishStatusResponseDTO>>(){}.getType();
        localVarApiClient.executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }
    /**
     * Build call for importOrgTheme
     * @param file The org-content.zip file  (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Successfully imported </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 413 </td><td> Payload Too Large. Request entity is larger than limits defined by server. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call importOrgThemeCall(File file, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/org-themes";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();
        if (file != null) {
            localVarFormParams.put("file", file);
        }

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "multipart/form-data"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        String[] localVarAuthNames = new String[] { "OAuth2Security" };
        return localVarApiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    @SuppressWarnings("rawtypes")
    private okhttp3.Call importOrgThemeValidateBeforeCall(File file, final ApiCallback _callback) throws ApiException {
        
        // verify the required parameter 'file' is set
        if (file == null) {
            throw new ApiException("Missing the required parameter 'file' when calling importOrgTheme(Async)");
        }
        

        okhttp3.Call localVarCall = importOrgThemeCall(file, _callback);
        return localVarCall;

    }

    /**
     * Import org theme
     * Imports a drafted zip of an org theme to APIM.
     * @param file The org-content.zip file  (required)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Successfully imported </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 413 </td><td> Payload Too Large. Request entity is larger than limits defined by server. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public void importOrgTheme(File file) throws ApiException {
        importOrgThemeWithHttpInfo(file);
    }

    /**
     * Import org theme
     * Imports a drafted zip of an org theme to APIM.
     * @param file The org-content.zip file  (required)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Successfully imported </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 413 </td><td> Payload Too Large. Request entity is larger than limits defined by server. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<Void> importOrgThemeWithHttpInfo(File file) throws ApiException {
        okhttp3.Call localVarCall = importOrgThemeValidateBeforeCall(file, null);
        return localVarApiClient.execute(localVarCall);
    }

    /**
     * Import org theme (asynchronously)
     * Imports a drafted zip of an org theme to APIM.
     * @param file The org-content.zip file  (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Successfully imported </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 413 </td><td> Payload Too Large. Request entity is larger than limits defined by server. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call importOrgThemeAsync(File file, final ApiCallback<Void> _callback) throws ApiException {

        okhttp3.Call localVarCall = importOrgThemeValidateBeforeCall(file, _callback);
        localVarApiClient.executeAsync(localVarCall, _callback);
        return localVarCall;
    }
    /**
     * Build call for updateOrgThemeStatus
     * @param id  (required)
     * @param contentPublishStatusDTO  (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Successfully updated status </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call updateOrgThemeStatusCall(String id, ContentPublishStatusDTO contentPublishStatusDTO, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = contentPublishStatusDTO;

        // create path and map variables
        String localVarPath = "/org-themes/{id}/status"
            .replaceAll("\\{" + "id" + "\\}", localVarApiClient.escapeString(id.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        Map<String, String> localVarCookieParams = new HashMap<String, String>();
        Map<String, Object> localVarFormParams = new HashMap<String, Object>();
        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = localVarApiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) {
            localVarHeaderParams.put("Accept", localVarAccept);
        }

        final String[] localVarContentTypes = {
            "application/json"
        };
        final String localVarContentType = localVarApiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        String[] localVarAuthNames = new String[] { "OAuth2Security" };
        return localVarApiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    @SuppressWarnings("rawtypes")
    private okhttp3.Call updateOrgThemeStatusValidateBeforeCall(String id, ContentPublishStatusDTO contentPublishStatusDTO, final ApiCallback _callback) throws ApiException {
        
        // verify the required parameter 'id' is set
        if (id == null) {
            throw new ApiException("Missing the required parameter 'id' when calling updateOrgThemeStatus(Async)");
        }
        
        // verify the required parameter 'contentPublishStatusDTO' is set
        if (contentPublishStatusDTO == null) {
            throw new ApiException("Missing the required parameter 'contentPublishStatusDTO' when calling updateOrgThemeStatus(Async)");
        }
        

        okhttp3.Call localVarCall = updateOrgThemeStatusCall(id, contentPublishStatusDTO, _callback);
        return localVarCall;

    }

    /**
     * Update publish status of an org theme
     * Publishes or unpublishes an org theme to the dev portal.
     * @param id  (required)
     * @param contentPublishStatusDTO  (required)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Successfully updated status </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public void updateOrgThemeStatus(String id, ContentPublishStatusDTO contentPublishStatusDTO) throws ApiException {
        updateOrgThemeStatusWithHttpInfo(id, contentPublishStatusDTO);
    }

    /**
     * Update publish status of an org theme
     * Publishes or unpublishes an org theme to the dev portal.
     * @param id  (required)
     * @param contentPublishStatusDTO  (required)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Successfully updated status </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<Void> updateOrgThemeStatusWithHttpInfo(String id, ContentPublishStatusDTO contentPublishStatusDTO) throws ApiException {
        okhttp3.Call localVarCall = updateOrgThemeStatusValidateBeforeCall(id, contentPublishStatusDTO, null);
        return localVarApiClient.execute(localVarCall);
    }

    /**
     * Update publish status of an org theme (asynchronously)
     * Publishes or unpublishes an org theme to the dev portal.
     * @param id  (required)
     * @param contentPublishStatusDTO  (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> Successfully updated status </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 403 </td><td> Forbidden. The request must be conditional but no condition has been specified. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 500 </td><td> Internal Server Error. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call updateOrgThemeStatusAsync(String id, ContentPublishStatusDTO contentPublishStatusDTO, final ApiCallback<Void> _callback) throws ApiException {

        okhttp3.Call localVarCall = updateOrgThemeStatusValidateBeforeCall(id, contentPublishStatusDTO, _callback);
        localVarApiClient.executeAsync(localVarCall, _callback);
        return localVarCall;
    }
}
