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


import org.wso2.am.integration.clients.admin.api.dto.CreatePlatformGatewayRequestDTO;
import org.wso2.am.integration.clients.admin.api.dto.ErrorDTO;
import org.wso2.am.integration.clients.admin.api.dto.GatewayListDTO;
import org.wso2.am.integration.clients.admin.api.dto.GatewayResponseWithTokenDTO;
import org.wso2.am.integration.clients.admin.api.dto.PlatformGatewayResponseDTO;
import org.wso2.am.integration.clients.admin.api.dto.UpdatePlatformGatewayRequestDTO;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlatformGatewaysApi {
    private ApiClient localVarApiClient;

    public PlatformGatewaysApi() {
        this(Configuration.getDefaultApiClient());
    }

    public PlatformGatewaysApi(ApiClient apiClient) {
        this.localVarApiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return localVarApiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.localVarApiClient = apiClient;
    }

    /**
     * Build call for createPlatformGateway
     * @param createPlatformGatewayRequestDTO  (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 201 </td><td> Created. Gateway and registration token (returned once) in the response body.  </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 409 </td><td> Conflict. Specified resource already exists. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call createPlatformGatewayCall(CreatePlatformGatewayRequestDTO createPlatformGatewayRequestDTO, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = createPlatformGatewayRequestDTO;

        // create path and map variables
        String localVarPath = "/gateways";

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
    private okhttp3.Call createPlatformGatewayValidateBeforeCall(CreatePlatformGatewayRequestDTO createPlatformGatewayRequestDTO, final ApiCallback _callback) throws ApiException {
        
        // verify the required parameter 'createPlatformGatewayRequestDTO' is set
        if (createPlatformGatewayRequestDTO == null) {
            throw new ApiException("Missing the required parameter 'createPlatformGatewayRequestDTO' when calling createPlatformGateway(Async)");
        }
        

        okhttp3.Call localVarCall = createPlatformGatewayCall(createPlatformGatewayRequestDTO, _callback);
        return localVarCall;

    }

    /**
     * Register a platform gateway
     * Register a new platform gateway. A registration token is generated and returned once in the response; store it (e.g. as GATEWAY_CONTROL_PLANE_TOKEN in Docker Compose) for the gateway to connect to the control plane WebSocket. The token is stored hashed and cannot be retrieved later. 
     * @param createPlatformGatewayRequestDTO  (required)
     * @return GatewayResponseWithTokenDTO
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 201 </td><td> Created. Gateway and registration token (returned once) in the response body.  </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 409 </td><td> Conflict. Specified resource already exists. </td><td>  -  </td></tr>
     </table>
     */
    public GatewayResponseWithTokenDTO createPlatformGateway(CreatePlatformGatewayRequestDTO createPlatformGatewayRequestDTO) throws ApiException {
        ApiResponse<GatewayResponseWithTokenDTO> localVarResp = createPlatformGatewayWithHttpInfo(createPlatformGatewayRequestDTO);
        return localVarResp.getData();
    }

    /**
     * Register a platform gateway
     * Register a new platform gateway. A registration token is generated and returned once in the response; store it (e.g. as GATEWAY_CONTROL_PLANE_TOKEN in Docker Compose) for the gateway to connect to the control plane WebSocket. The token is stored hashed and cannot be retrieved later. 
     * @param createPlatformGatewayRequestDTO  (required)
     * @return ApiResponse&lt;GatewayResponseWithTokenDTO&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 201 </td><td> Created. Gateway and registration token (returned once) in the response body.  </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 409 </td><td> Conflict. Specified resource already exists. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<GatewayResponseWithTokenDTO> createPlatformGatewayWithHttpInfo(CreatePlatformGatewayRequestDTO createPlatformGatewayRequestDTO) throws ApiException {
        okhttp3.Call localVarCall = createPlatformGatewayValidateBeforeCall(createPlatformGatewayRequestDTO, null);
        Type localVarReturnType = new TypeToken<GatewayResponseWithTokenDTO>(){}.getType();
        return localVarApiClient.execute(localVarCall, localVarReturnType);
    }

    /**
     * Register a platform gateway (asynchronously)
     * Register a new platform gateway. A registration token is generated and returned once in the response; store it (e.g. as GATEWAY_CONTROL_PLANE_TOKEN in Docker Compose) for the gateway to connect to the control plane WebSocket. The token is stored hashed and cannot be retrieved later. 
     * @param createPlatformGatewayRequestDTO  (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 201 </td><td> Created. Gateway and registration token (returned once) in the response body.  </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 409 </td><td> Conflict. Specified resource already exists. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call createPlatformGatewayAsync(CreatePlatformGatewayRequestDTO createPlatformGatewayRequestDTO, final ApiCallback<GatewayResponseWithTokenDTO> _callback) throws ApiException {

        okhttp3.Call localVarCall = createPlatformGatewayValidateBeforeCall(createPlatformGatewayRequestDTO, _callback);
        Type localVarReturnType = new TypeToken<GatewayResponseWithTokenDTO>(){}.getType();
        localVarApiClient.executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }
    /**
     * Build call for deletePlatformGateway
     * @param gatewayId Gateway UUID (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Gateway and all references removed. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 409 </td><td> Conflict. Cannot delete gateway while API revisions are deployed to it. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call deletePlatformGatewayCall(String gatewayId, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/gateways/{gatewayId}"
            .replaceAll("\\{" + "gatewayId" + "\\}", localVarApiClient.escapeString(gatewayId.toString()));

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
    private okhttp3.Call deletePlatformGatewayValidateBeforeCall(String gatewayId, final ApiCallback _callback) throws ApiException {
        
        // verify the required parameter 'gatewayId' is set
        if (gatewayId == null) {
            throw new ApiException("Missing the required parameter 'gatewayId' when calling deletePlatformGateway(Async)");
        }
        

        okhttp3.Call localVarCall = deletePlatformGatewayCall(gatewayId, _callback);
        return localVarCall;

    }

    /**
     * Delete a platform gateway
     * Delete a platform gateway and all its references (tokens, instance mappings, revision deployment records, gateway environment, permissions). Fails with 409 if any API revisions are currently deployed to this gateway; undeploy all APIs from the gateway first. 
     * @param gatewayId Gateway UUID (required)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Gateway and all references removed. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 409 </td><td> Conflict. Cannot delete gateway while API revisions are deployed to it. </td><td>  -  </td></tr>
     </table>
     */
    public void deletePlatformGateway(String gatewayId) throws ApiException {
        deletePlatformGatewayWithHttpInfo(gatewayId);
    }

    /**
     * Delete a platform gateway
     * Delete a platform gateway and all its references (tokens, instance mappings, revision deployment records, gateway environment, permissions). Fails with 409 if any API revisions are currently deployed to this gateway; undeploy all APIs from the gateway first. 
     * @param gatewayId Gateway UUID (required)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Gateway and all references removed. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 409 </td><td> Conflict. Cannot delete gateway while API revisions are deployed to it. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<Void> deletePlatformGatewayWithHttpInfo(String gatewayId) throws ApiException {
        okhttp3.Call localVarCall = deletePlatformGatewayValidateBeforeCall(gatewayId, null);
        return localVarApiClient.execute(localVarCall);
    }

    /**
     * Delete a platform gateway (asynchronously)
     * Delete a platform gateway and all its references (tokens, instance mappings, revision deployment records, gateway environment, permissions). Fails with 409 if any API revisions are currently deployed to this gateway; undeploy all APIs from the gateway first. 
     * @param gatewayId Gateway UUID (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Gateway and all references removed. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 409 </td><td> Conflict. Cannot delete gateway while API revisions are deployed to it. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call deletePlatformGatewayAsync(String gatewayId, final ApiCallback<Void> _callback) throws ApiException {

        okhttp3.Call localVarCall = deletePlatformGatewayValidateBeforeCall(gatewayId, _callback);
        localVarApiClient.executeAsync(localVarCall, _callback);
        return localVarCall;
    }
    /**
     * Build call for getPlatformGateways
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. List of platform gateways returned (without registration tokens).  </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call getPlatformGatewaysCall(final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/gateways";

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
    private okhttp3.Call getPlatformGatewaysValidateBeforeCall(final ApiCallback _callback) throws ApiException {
        

        okhttp3.Call localVarCall = getPlatformGatewaysCall(_callback);
        return localVarCall;

    }

    /**
     * Get all platform gateways
     * Get all registered platform gateways for the organization. 
     * @return GatewayListDTO
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. List of platform gateways returned (without registration tokens).  </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public GatewayListDTO getPlatformGateways() throws ApiException {
        ApiResponse<GatewayListDTO> localVarResp = getPlatformGatewaysWithHttpInfo();
        return localVarResp.getData();
    }

    /**
     * Get all platform gateways
     * Get all registered platform gateways for the organization. 
     * @return ApiResponse&lt;GatewayListDTO&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. List of platform gateways returned (without registration tokens).  </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<GatewayListDTO> getPlatformGatewaysWithHttpInfo() throws ApiException {
        okhttp3.Call localVarCall = getPlatformGatewaysValidateBeforeCall(null);
        Type localVarReturnType = new TypeToken<GatewayListDTO>(){}.getType();
        return localVarApiClient.execute(localVarCall, localVarReturnType);
    }

    /**
     * Get all platform gateways (asynchronously)
     * Get all registered platform gateways for the organization. 
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. List of platform gateways returned (without registration tokens).  </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call getPlatformGatewaysAsync(final ApiCallback<GatewayListDTO> _callback) throws ApiException {

        okhttp3.Call localVarCall = getPlatformGatewaysValidateBeforeCall(_callback);
        Type localVarReturnType = new TypeToken<GatewayListDTO>(){}.getType();
        localVarApiClient.executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }
    /**
     * Build call for regeneratePlatformGatewayToken
     * @param gatewayId Gateway UUID (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Gateway and new registration token (returned once) in the response body.  </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call regeneratePlatformGatewayTokenCall(String gatewayId, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/gateways/{gatewayId}/regenerate-token"
            .replaceAll("\\{" + "gatewayId" + "\\}", localVarApiClient.escapeString(gatewayId.toString()));

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
        return localVarApiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
    }

    @SuppressWarnings("rawtypes")
    private okhttp3.Call regeneratePlatformGatewayTokenValidateBeforeCall(String gatewayId, final ApiCallback _callback) throws ApiException {
        
        // verify the required parameter 'gatewayId' is set
        if (gatewayId == null) {
            throw new ApiException("Missing the required parameter 'gatewayId' when calling regeneratePlatformGatewayToken(Async)");
        }
        

        okhttp3.Call localVarCall = regeneratePlatformGatewayTokenCall(gatewayId, _callback);
        return localVarCall;

    }

    /**
     * Regenerate registration token for a platform gateway
     * Regenerate the registration token for an existing platform gateway. The old token is revoked and a new one is generated. Store the new token (e.g. as GATEWAY_CONTROL_PLANE_TOKEN in Docker Compose) for the gateway to reconnect to the control plane WebSocket. The token is returned only once. 
     * @param gatewayId Gateway UUID (required)
     * @return GatewayResponseWithTokenDTO
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Gateway and new registration token (returned once) in the response body.  </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public GatewayResponseWithTokenDTO regeneratePlatformGatewayToken(String gatewayId) throws ApiException {
        ApiResponse<GatewayResponseWithTokenDTO> localVarResp = regeneratePlatformGatewayTokenWithHttpInfo(gatewayId);
        return localVarResp.getData();
    }

    /**
     * Regenerate registration token for a platform gateway
     * Regenerate the registration token for an existing platform gateway. The old token is revoked and a new one is generated. Store the new token (e.g. as GATEWAY_CONTROL_PLANE_TOKEN in Docker Compose) for the gateway to reconnect to the control plane WebSocket. The token is returned only once. 
     * @param gatewayId Gateway UUID (required)
     * @return ApiResponse&lt;GatewayResponseWithTokenDTO&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Gateway and new registration token (returned once) in the response body.  </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<GatewayResponseWithTokenDTO> regeneratePlatformGatewayTokenWithHttpInfo(String gatewayId) throws ApiException {
        okhttp3.Call localVarCall = regeneratePlatformGatewayTokenValidateBeforeCall(gatewayId, null);
        Type localVarReturnType = new TypeToken<GatewayResponseWithTokenDTO>(){}.getType();
        return localVarApiClient.execute(localVarCall, localVarReturnType);
    }

    /**
     * Regenerate registration token for a platform gateway (asynchronously)
     * Regenerate the registration token for an existing platform gateway. The old token is revoked and a new one is generated. Store the new token (e.g. as GATEWAY_CONTROL_PLANE_TOKEN in Docker Compose) for the gateway to reconnect to the control plane WebSocket. The token is returned only once. 
     * @param gatewayId Gateway UUID (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Gateway and new registration token (returned once) in the response body.  </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call regeneratePlatformGatewayTokenAsync(String gatewayId, final ApiCallback<GatewayResponseWithTokenDTO> _callback) throws ApiException {

        okhttp3.Call localVarCall = regeneratePlatformGatewayTokenValidateBeforeCall(gatewayId, _callback);
        Type localVarReturnType = new TypeToken<GatewayResponseWithTokenDTO>(){}.getType();
        localVarApiClient.executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }
    /**
     * Build call for updatePlatformGateway
     * @param gatewayId Gateway UUID (required)
     * @param updatePlatformGatewayRequestDTO  (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Updated platform gateway in the response body. </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call updatePlatformGatewayCall(String gatewayId, UpdatePlatformGatewayRequestDTO updatePlatformGatewayRequestDTO, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = updatePlatformGatewayRequestDTO;

        // create path and map variables
        String localVarPath = "/gateways/{gatewayId}"
            .replaceAll("\\{" + "gatewayId" + "\\}", localVarApiClient.escapeString(gatewayId.toString()));

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
    private okhttp3.Call updatePlatformGatewayValidateBeforeCall(String gatewayId, UpdatePlatformGatewayRequestDTO updatePlatformGatewayRequestDTO, final ApiCallback _callback) throws ApiException {
        
        // verify the required parameter 'gatewayId' is set
        if (gatewayId == null) {
            throw new ApiException("Missing the required parameter 'gatewayId' when calling updatePlatformGateway(Async)");
        }
        
        // verify the required parameter 'updatePlatformGatewayRequestDTO' is set
        if (updatePlatformGatewayRequestDTO == null) {
            throw new ApiException("Missing the required parameter 'updatePlatformGatewayRequestDTO' when calling updatePlatformGateway(Async)");
        }
        

        okhttp3.Call localVarCall = updatePlatformGatewayCall(gatewayId, updatePlatformGatewayRequestDTO, _callback);
        return localVarCall;

    }

    /**
     * Update a platform gateway
     * Update platform gateway metadata. Request body must include all updatable fields (displayName, description, properties, permissions). Name and vhost cannot be changed. UI should send the full resource representation to align with PUT semantics. 
     * @param gatewayId Gateway UUID (required)
     * @param updatePlatformGatewayRequestDTO  (required)
     * @return PlatformGatewayResponseDTO
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Updated platform gateway in the response body. </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public PlatformGatewayResponseDTO updatePlatformGateway(String gatewayId, UpdatePlatformGatewayRequestDTO updatePlatformGatewayRequestDTO) throws ApiException {
        ApiResponse<PlatformGatewayResponseDTO> localVarResp = updatePlatformGatewayWithHttpInfo(gatewayId, updatePlatformGatewayRequestDTO);
        return localVarResp.getData();
    }

    /**
     * Update a platform gateway
     * Update platform gateway metadata. Request body must include all updatable fields (displayName, description, properties, permissions). Name and vhost cannot be changed. UI should send the full resource representation to align with PUT semantics. 
     * @param gatewayId Gateway UUID (required)
     * @param updatePlatformGatewayRequestDTO  (required)
     * @return ApiResponse&lt;PlatformGatewayResponseDTO&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Updated platform gateway in the response body. </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<PlatformGatewayResponseDTO> updatePlatformGatewayWithHttpInfo(String gatewayId, UpdatePlatformGatewayRequestDTO updatePlatformGatewayRequestDTO) throws ApiException {
        okhttp3.Call localVarCall = updatePlatformGatewayValidateBeforeCall(gatewayId, updatePlatformGatewayRequestDTO, null);
        Type localVarReturnType = new TypeToken<PlatformGatewayResponseDTO>(){}.getType();
        return localVarApiClient.execute(localVarCall, localVarReturnType);
    }

    /**
     * Update a platform gateway (asynchronously)
     * Update platform gateway metadata. Request body must include all updatable fields (displayName, description, properties, permissions). Name and vhost cannot be changed. UI should send the full resource representation to align with PUT semantics. 
     * @param gatewayId Gateway UUID (required)
     * @param updatePlatformGatewayRequestDTO  (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Updated platform gateway in the response body. </td><td>  -  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call updatePlatformGatewayAsync(String gatewayId, UpdatePlatformGatewayRequestDTO updatePlatformGatewayRequestDTO, final ApiCallback<PlatformGatewayResponseDTO> _callback) throws ApiException {

        okhttp3.Call localVarCall = updatePlatformGatewayValidateBeforeCall(gatewayId, updatePlatformGatewayRequestDTO, _callback);
        Type localVarReturnType = new TypeToken<PlatformGatewayResponseDTO>(){}.getType();
        localVarApiClient.executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }
}
