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


import org.wso2.am.integration.clients.admin.api.dto.EnvironmentDTO;
import org.wso2.am.integration.clients.admin.api.dto.EnvironmentListDTO;
import org.wso2.am.integration.clients.admin.api.dto.ErrorDTO;
import org.wso2.am.integration.clients.admin.api.dto.GatewayInstanceListDTO;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvironmentsApi {
    private ApiClient localVarApiClient;

    public EnvironmentsApi() {
        this(Configuration.getDefaultApiClient());
    }

    public EnvironmentsApi(ApiClient apiClient) {
        this.localVarApiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return localVarApiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.localVarApiClient = apiClient;
    }

    /**
     * Build call for environmentsEnvironmentIdDelete
     * @param environmentId Environment UUID (or Environment name defined in config), in case the ID contains special characters it should be base64 encoded  (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Environment successfully deleted.  </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call environmentsEnvironmentIdDeleteCall(String environmentId, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/environments/{environmentId}"
            .replaceAll("\\{" + "environmentId" + "\\}", localVarApiClient.escapeString(environmentId.toString()));

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
    private okhttp3.Call environmentsEnvironmentIdDeleteValidateBeforeCall(String environmentId, final ApiCallback _callback) throws ApiException {
        
        // verify the required parameter 'environmentId' is set
        if (environmentId == null) {
            throw new ApiException("Missing the required parameter 'environmentId' when calling environmentsEnvironmentIdDelete(Async)");
        }
        

        okhttp3.Call localVarCall = environmentsEnvironmentIdDeleteCall(environmentId, _callback);
        return localVarCall;

    }

    /**
     * Delete an Environment
     * Delete a Environment by Environment Id 
     * @param environmentId Environment UUID (or Environment name defined in config), in case the ID contains special characters it should be base64 encoded  (required)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Environment successfully deleted.  </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public void environmentsEnvironmentIdDelete(String environmentId) throws ApiException {
        environmentsEnvironmentIdDeleteWithHttpInfo(environmentId);
    }

    /**
     * Delete an Environment
     * Delete a Environment by Environment Id 
     * @param environmentId Environment UUID (or Environment name defined in config), in case the ID contains special characters it should be base64 encoded  (required)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Environment successfully deleted.  </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<Void> environmentsEnvironmentIdDeleteWithHttpInfo(String environmentId) throws ApiException {
        okhttp3.Call localVarCall = environmentsEnvironmentIdDeleteValidateBeforeCall(environmentId, null);
        return localVarApiClient.execute(localVarCall);
    }

    /**
     * Delete an Environment (asynchronously)
     * Delete a Environment by Environment Id 
     * @param environmentId Environment UUID (or Environment name defined in config), in case the ID contains special characters it should be base64 encoded  (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Environment successfully deleted.  </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call environmentsEnvironmentIdDeleteAsync(String environmentId, final ApiCallback<Void> _callback) throws ApiException {

        okhttp3.Call localVarCall = environmentsEnvironmentIdDeleteValidateBeforeCall(environmentId, _callback);
        localVarApiClient.executeAsync(localVarCall, _callback);
        return localVarCall;
    }
    /**
     * Build call for environmentsEnvironmentIdGatewaysGet
     * @param environmentId Environment UUID (or Environment name defined in config), in case the ID contains special characters it should be base64 encoded  (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. List of gateway Instances in the gateway environment returned  </td><td>  * Content-Type - The content type of the body.  <br>  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 406 </td><td> Not Acceptable. The requested media type is not supported. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call environmentsEnvironmentIdGatewaysGetCall(String environmentId, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/environments/{environmentId}/gateways"
            .replaceAll("\\{" + "environmentId" + "\\}", localVarApiClient.escapeString(environmentId.toString()));

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
        return localVarApiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    @SuppressWarnings("rawtypes")
    private okhttp3.Call environmentsEnvironmentIdGatewaysGetValidateBeforeCall(String environmentId, final ApiCallback _callback) throws ApiException {
        
        // verify the required parameter 'environmentId' is set
        if (environmentId == null) {
            throw new ApiException("Missing the required parameter 'environmentId' when calling environmentsEnvironmentIdGatewaysGet(Async)");
        }
        

        okhttp3.Call localVarCall = environmentsEnvironmentIdGatewaysGetCall(environmentId, _callback);
        return localVarCall;

    }

    /**
     * Get Gateway Instances in a Gateway Environment
     * Retrieve list of gateway Instances in the gateway environment. 
     * @param environmentId Environment UUID (or Environment name defined in config), in case the ID contains special characters it should be base64 encoded  (required)
     * @return GatewayInstanceListDTO
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. List of gateway Instances in the gateway environment returned  </td><td>  * Content-Type - The content type of the body.  <br>  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 406 </td><td> Not Acceptable. The requested media type is not supported. </td><td>  -  </td></tr>
     </table>
     */
    public GatewayInstanceListDTO environmentsEnvironmentIdGatewaysGet(String environmentId) throws ApiException {
        ApiResponse<GatewayInstanceListDTO> localVarResp = environmentsEnvironmentIdGatewaysGetWithHttpInfo(environmentId);
        return localVarResp.getData();
    }

    /**
     * Get Gateway Instances in a Gateway Environment
     * Retrieve list of gateway Instances in the gateway environment. 
     * @param environmentId Environment UUID (or Environment name defined in config), in case the ID contains special characters it should be base64 encoded  (required)
     * @return ApiResponse&lt;GatewayInstanceListDTO&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. List of gateway Instances in the gateway environment returned  </td><td>  * Content-Type - The content type of the body.  <br>  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 406 </td><td> Not Acceptable. The requested media type is not supported. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<GatewayInstanceListDTO> environmentsEnvironmentIdGatewaysGetWithHttpInfo(String environmentId) throws ApiException {
        okhttp3.Call localVarCall = environmentsEnvironmentIdGatewaysGetValidateBeforeCall(environmentId, null);
        Type localVarReturnType = new TypeToken<GatewayInstanceListDTO>(){}.getType();
        return localVarApiClient.execute(localVarCall, localVarReturnType);
    }

    /**
     * Get Gateway Instances in a Gateway Environment (asynchronously)
     * Retrieve list of gateway Instances in the gateway environment. 
     * @param environmentId Environment UUID (or Environment name defined in config), in case the ID contains special characters it should be base64 encoded  (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. List of gateway Instances in the gateway environment returned  </td><td>  * Content-Type - The content type of the body.  <br>  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 406 </td><td> Not Acceptable. The requested media type is not supported. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call environmentsEnvironmentIdGatewaysGetAsync(String environmentId, final ApiCallback<GatewayInstanceListDTO> _callback) throws ApiException {

        okhttp3.Call localVarCall = environmentsEnvironmentIdGatewaysGetValidateBeforeCall(environmentId, _callback);
        Type localVarReturnType = new TypeToken<GatewayInstanceListDTO>(){}.getType();
        localVarApiClient.executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }
    /**
     * Build call for environmentsEnvironmentIdGet
     * @param environmentId Environment UUID (or Environment name defined in config), in case the ID contains special characters it should be base64 encoded  (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Gateway Environment Configuration returned  </td><td>  * Content-Type - The content type of the body.  <br>  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 406 </td><td> Not Acceptable. The requested media type is not supported. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call environmentsEnvironmentIdGetCall(String environmentId, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/environments/{environmentId}"
            .replaceAll("\\{" + "environmentId" + "\\}", localVarApiClient.escapeString(environmentId.toString()));

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
        return localVarApiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    @SuppressWarnings("rawtypes")
    private okhttp3.Call environmentsEnvironmentIdGetValidateBeforeCall(String environmentId, final ApiCallback _callback) throws ApiException {
        
        // verify the required parameter 'environmentId' is set
        if (environmentId == null) {
            throw new ApiException("Missing the required parameter 'environmentId' when calling environmentsEnvironmentIdGet(Async)");
        }
        

        okhttp3.Call localVarCall = environmentsEnvironmentIdGetCall(environmentId, _callback);
        return localVarCall;

    }

    /**
     * Get a Gateway Environment Configuration
     * Retrieve a single Gateway Environment Configuration. We should provide the Id of the Environment as a path parameter. 
     * @param environmentId Environment UUID (or Environment name defined in config), in case the ID contains special characters it should be base64 encoded  (required)
     * @return EnvironmentDTO
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Gateway Environment Configuration returned  </td><td>  * Content-Type - The content type of the body.  <br>  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 406 </td><td> Not Acceptable. The requested media type is not supported. </td><td>  -  </td></tr>
     </table>
     */
    public EnvironmentDTO environmentsEnvironmentIdGet(String environmentId) throws ApiException {
        ApiResponse<EnvironmentDTO> localVarResp = environmentsEnvironmentIdGetWithHttpInfo(environmentId);
        return localVarResp.getData();
    }

    /**
     * Get a Gateway Environment Configuration
     * Retrieve a single Gateway Environment Configuration. We should provide the Id of the Environment as a path parameter. 
     * @param environmentId Environment UUID (or Environment name defined in config), in case the ID contains special characters it should be base64 encoded  (required)
     * @return ApiResponse&lt;EnvironmentDTO&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Gateway Environment Configuration returned  </td><td>  * Content-Type - The content type of the body.  <br>  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 406 </td><td> Not Acceptable. The requested media type is not supported. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<EnvironmentDTO> environmentsEnvironmentIdGetWithHttpInfo(String environmentId) throws ApiException {
        okhttp3.Call localVarCall = environmentsEnvironmentIdGetValidateBeforeCall(environmentId, null);
        Type localVarReturnType = new TypeToken<EnvironmentDTO>(){}.getType();
        return localVarApiClient.execute(localVarCall, localVarReturnType);
    }

    /**
     * Get a Gateway Environment Configuration (asynchronously)
     * Retrieve a single Gateway Environment Configuration. We should provide the Id of the Environment as a path parameter. 
     * @param environmentId Environment UUID (or Environment name defined in config), in case the ID contains special characters it should be base64 encoded  (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Gateway Environment Configuration returned  </td><td>  * Content-Type - The content type of the body.  <br>  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 406 </td><td> Not Acceptable. The requested media type is not supported. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call environmentsEnvironmentIdGetAsync(String environmentId, final ApiCallback<EnvironmentDTO> _callback) throws ApiException {

        okhttp3.Call localVarCall = environmentsEnvironmentIdGetValidateBeforeCall(environmentId, _callback);
        Type localVarReturnType = new TypeToken<EnvironmentDTO>(){}.getType();
        localVarApiClient.executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }
    /**
     * Build call for environmentsEnvironmentIdPut
     * @param environmentId Environment UUID (or Environment name defined in config), in case the ID contains special characters it should be base64 encoded  (required)
     * @param environmentDTO Environment object with updated information  (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Environment updated.  </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call environmentsEnvironmentIdPutCall(String environmentId, EnvironmentDTO environmentDTO, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = environmentDTO;

        // create path and map variables
        String localVarPath = "/environments/{environmentId}"
            .replaceAll("\\{" + "environmentId" + "\\}", localVarApiClient.escapeString(environmentId.toString()));

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
        return localVarApiClient.buildCall(localVarPath, "PUT", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    @SuppressWarnings("rawtypes")
    private okhttp3.Call environmentsEnvironmentIdPutValidateBeforeCall(String environmentId, EnvironmentDTO environmentDTO, final ApiCallback _callback) throws ApiException {
        
        // verify the required parameter 'environmentId' is set
        if (environmentId == null) {
            throw new ApiException("Missing the required parameter 'environmentId' when calling environmentsEnvironmentIdPut(Async)");
        }
        
        // verify the required parameter 'environmentDTO' is set
        if (environmentDTO == null) {
            throw new ApiException("Missing the required parameter 'environmentDTO' when calling environmentsEnvironmentIdPut(Async)");
        }
        

        okhttp3.Call localVarCall = environmentsEnvironmentIdPutCall(environmentId, environmentDTO, _callback);
        return localVarCall;

    }

    /**
     * Update an Environment
     * Update a gateway Environment by environment Id 
     * @param environmentId Environment UUID (or Environment name defined in config), in case the ID contains special characters it should be base64 encoded  (required)
     * @param environmentDTO Environment object with updated information  (required)
     * @return EnvironmentDTO
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Environment updated.  </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public EnvironmentDTO environmentsEnvironmentIdPut(String environmentId, EnvironmentDTO environmentDTO) throws ApiException {
        ApiResponse<EnvironmentDTO> localVarResp = environmentsEnvironmentIdPutWithHttpInfo(environmentId, environmentDTO);
        return localVarResp.getData();
    }

    /**
     * Update an Environment
     * Update a gateway Environment by environment Id 
     * @param environmentId Environment UUID (or Environment name defined in config), in case the ID contains special characters it should be base64 encoded  (required)
     * @param environmentDTO Environment object with updated information  (required)
     * @return ApiResponse&lt;EnvironmentDTO&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Environment updated.  </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<EnvironmentDTO> environmentsEnvironmentIdPutWithHttpInfo(String environmentId, EnvironmentDTO environmentDTO) throws ApiException {
        okhttp3.Call localVarCall = environmentsEnvironmentIdPutValidateBeforeCall(environmentId, environmentDTO, null);
        Type localVarReturnType = new TypeToken<EnvironmentDTO>(){}.getType();
        return localVarApiClient.execute(localVarCall, localVarReturnType);
    }

    /**
     * Update an Environment (asynchronously)
     * Update a gateway Environment by environment Id 
     * @param environmentId Environment UUID (or Environment name defined in config), in case the ID contains special characters it should be base64 encoded  (required)
     * @param environmentDTO Environment object with updated information  (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Environment updated.  </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call environmentsEnvironmentIdPutAsync(String environmentId, EnvironmentDTO environmentDTO, final ApiCallback<EnvironmentDTO> _callback) throws ApiException {

        okhttp3.Call localVarCall = environmentsEnvironmentIdPutValidateBeforeCall(environmentId, environmentDTO, _callback);
        Type localVarReturnType = new TypeToken<EnvironmentDTO>(){}.getType();
        localVarApiClient.executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }
    /**
     * Build call for environmentsGet
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Environments returned  </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call environmentsGetCall(final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/environments";

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
        return localVarApiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    @SuppressWarnings("rawtypes")
    private okhttp3.Call environmentsGetValidateBeforeCall(final ApiCallback _callback) throws ApiException {
        

        okhttp3.Call localVarCall = environmentsGetCall(_callback);
        return localVarCall;

    }

    /**
     * Get all registered Environments
     * Get all Registered Environments 
     * @return EnvironmentListDTO
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Environments returned  </td><td>  -  </td></tr>
     </table>
     */
    public EnvironmentListDTO environmentsGet() throws ApiException {
        ApiResponse<EnvironmentListDTO> localVarResp = environmentsGetWithHttpInfo();
        return localVarResp.getData();
    }

    /**
     * Get all registered Environments
     * Get all Registered Environments 
     * @return ApiResponse&lt;EnvironmentListDTO&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Environments returned  </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<EnvironmentListDTO> environmentsGetWithHttpInfo() throws ApiException {
        okhttp3.Call localVarCall = environmentsGetValidateBeforeCall(null);
        Type localVarReturnType = new TypeToken<EnvironmentListDTO>(){}.getType();
        return localVarApiClient.execute(localVarCall, localVarReturnType);
    }

    /**
     * Get all registered Environments (asynchronously)
     * Get all Registered Environments 
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Environments returned  </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call environmentsGetAsync(final ApiCallback<EnvironmentListDTO> _callback) throws ApiException {

        okhttp3.Call localVarCall = environmentsGetValidateBeforeCall(_callback);
        Type localVarReturnType = new TypeToken<EnvironmentListDTO>(){}.getType();
        localVarApiClient.executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }
    /**
     * Build call for environmentsPost
     * @param environmentDTO Environment object that should to be added  (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 201 </td><td> Created. Successful response with the newly created environment as entity in the body.  </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call environmentsPostCall(EnvironmentDTO environmentDTO, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = environmentDTO;

        // create path and map variables
        String localVarPath = "/environments";

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
    private okhttp3.Call environmentsPostValidateBeforeCall(EnvironmentDTO environmentDTO, final ApiCallback _callback) throws ApiException {
        
        // verify the required parameter 'environmentDTO' is set
        if (environmentDTO == null) {
            throw new ApiException("Missing the required parameter 'environmentDTO' when calling environmentsPost(Async)");
        }
        

        okhttp3.Call localVarCall = environmentsPostCall(environmentDTO, _callback);
        return localVarCall;

    }

    /**
     * Add an Environment
     * Add a new gateway environment 
     * @param environmentDTO Environment object that should to be added  (required)
     * @return EnvironmentDTO
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 201 </td><td> Created. Successful response with the newly created environment as entity in the body.  </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
     </table>
     */
    public EnvironmentDTO environmentsPost(EnvironmentDTO environmentDTO) throws ApiException {
        ApiResponse<EnvironmentDTO> localVarResp = environmentsPostWithHttpInfo(environmentDTO);
        return localVarResp.getData();
    }

    /**
     * Add an Environment
     * Add a new gateway environment 
     * @param environmentDTO Environment object that should to be added  (required)
     * @return ApiResponse&lt;EnvironmentDTO&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 201 </td><td> Created. Successful response with the newly created environment as entity in the body.  </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<EnvironmentDTO> environmentsPostWithHttpInfo(EnvironmentDTO environmentDTO) throws ApiException {
        okhttp3.Call localVarCall = environmentsPostValidateBeforeCall(environmentDTO, null);
        Type localVarReturnType = new TypeToken<EnvironmentDTO>(){}.getType();
        return localVarApiClient.execute(localVarCall, localVarReturnType);
    }

    /**
     * Add an Environment (asynchronously)
     * Add a new gateway environment 
     * @param environmentDTO Environment object that should to be added  (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 201 </td><td> Created. Successful response with the newly created environment as entity in the body.  </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call environmentsPostAsync(EnvironmentDTO environmentDTO, final ApiCallback<EnvironmentDTO> _callback) throws ApiException {

        okhttp3.Call localVarCall = environmentsPostValidateBeforeCall(environmentDTO, _callback);
        Type localVarReturnType = new TypeToken<EnvironmentDTO>(){}.getType();
        localVarApiClient.executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }
}
