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


import org.wso2.am.integration.clients.admin.api.dto.CustomRuleDTO;
import org.wso2.am.integration.clients.admin.api.dto.ErrorDTO;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomRulesIndividualApi {
    private ApiClient localVarApiClient;

    public CustomRulesIndividualApi() {
        this(Configuration.getDefaultApiClient());
    }

    public CustomRulesIndividualApi(ApiClient apiClient) {
        this.localVarApiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return localVarApiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.localVarApiClient = apiClient;
    }

    /**
     * Build call for throttlingPoliciesCustomRuleIdDelete
     * @param ruleId Custom rule UUID  (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Resource successfully deleted.  </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call throttlingPoliciesCustomRuleIdDeleteCall(String ruleId, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/throttling/policies/custom/{ruleId}"
            .replaceAll("\\{" + "ruleId" + "\\}", localVarApiClient.escapeString(ruleId.toString()));

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
    private okhttp3.Call throttlingPoliciesCustomRuleIdDeleteValidateBeforeCall(String ruleId, final ApiCallback _callback) throws ApiException {
        
        // verify the required parameter 'ruleId' is set
        if (ruleId == null) {
            throw new ApiException("Missing the required parameter 'ruleId' when calling throttlingPoliciesCustomRuleIdDelete(Async)");
        }
        

        okhttp3.Call localVarCall = throttlingPoliciesCustomRuleIdDeleteCall(ruleId, _callback);
        return localVarCall;

    }

    /**
     * Delete a Custom Rule
     * Delete a custom rule. We need to provide the Id of the policy as a path parameter.  **NOTE:** * Only super tenant users are allowed for this operation. 
     * @param ruleId Custom rule UUID  (required)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Resource successfully deleted.  </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public void throttlingPoliciesCustomRuleIdDelete(String ruleId) throws ApiException {
        throttlingPoliciesCustomRuleIdDeleteWithHttpInfo(ruleId);
    }

    /**
     * Delete a Custom Rule
     * Delete a custom rule. We need to provide the Id of the policy as a path parameter.  **NOTE:** * Only super tenant users are allowed for this operation. 
     * @param ruleId Custom rule UUID  (required)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Resource successfully deleted.  </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<Void> throttlingPoliciesCustomRuleIdDeleteWithHttpInfo(String ruleId) throws ApiException {
        okhttp3.Call localVarCall = throttlingPoliciesCustomRuleIdDeleteValidateBeforeCall(ruleId, null);
        return localVarApiClient.execute(localVarCall);
    }

    /**
     * Delete a Custom Rule (asynchronously)
     * Delete a custom rule. We need to provide the Id of the policy as a path parameter.  **NOTE:** * Only super tenant users are allowed for this operation. 
     * @param ruleId Custom rule UUID  (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Resource successfully deleted.  </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call throttlingPoliciesCustomRuleIdDeleteAsync(String ruleId, final ApiCallback<Void> _callback) throws ApiException {

        okhttp3.Call localVarCall = throttlingPoliciesCustomRuleIdDeleteValidateBeforeCall(ruleId, _callback);
        localVarApiClient.executeAsync(localVarCall, _callback);
        return localVarCall;
    }
    /**
     * Build call for throttlingPoliciesCustomRuleIdGet
     * @param ruleId Custom rule UUID  (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Policy returned  </td><td>  * Content-Type - The content type of the body.  <br>  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 406 </td><td> Not Acceptable. The requested media type is not supported. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call throttlingPoliciesCustomRuleIdGetCall(String ruleId, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/throttling/policies/custom/{ruleId}"
            .replaceAll("\\{" + "ruleId" + "\\}", localVarApiClient.escapeString(ruleId.toString()));

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
    private okhttp3.Call throttlingPoliciesCustomRuleIdGetValidateBeforeCall(String ruleId, final ApiCallback _callback) throws ApiException {
        
        // verify the required parameter 'ruleId' is set
        if (ruleId == null) {
            throw new ApiException("Missing the required parameter 'ruleId' when calling throttlingPoliciesCustomRuleIdGet(Async)");
        }
        

        okhttp3.Call localVarCall = throttlingPoliciesCustomRuleIdGetCall(ruleId, _callback);
        return localVarCall;

    }

    /**
     * Get a Custom Rule
     * Retrieves a custom rule. We need to provide the policy Id as a path parameter.  **NOTE:** * Only super tenant users are allowed for this operation. 
     * @param ruleId Custom rule UUID  (required)
     * @return CustomRuleDTO
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Policy returned  </td><td>  * Content-Type - The content type of the body.  <br>  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 406 </td><td> Not Acceptable. The requested media type is not supported. </td><td>  -  </td></tr>
     </table>
     */
    public CustomRuleDTO throttlingPoliciesCustomRuleIdGet(String ruleId) throws ApiException {
        ApiResponse<CustomRuleDTO> localVarResp = throttlingPoliciesCustomRuleIdGetWithHttpInfo(ruleId);
        return localVarResp.getData();
    }

    /**
     * Get a Custom Rule
     * Retrieves a custom rule. We need to provide the policy Id as a path parameter.  **NOTE:** * Only super tenant users are allowed for this operation. 
     * @param ruleId Custom rule UUID  (required)
     * @return ApiResponse&lt;CustomRuleDTO&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Policy returned  </td><td>  * Content-Type - The content type of the body.  <br>  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 406 </td><td> Not Acceptable. The requested media type is not supported. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<CustomRuleDTO> throttlingPoliciesCustomRuleIdGetWithHttpInfo(String ruleId) throws ApiException {
        okhttp3.Call localVarCall = throttlingPoliciesCustomRuleIdGetValidateBeforeCall(ruleId, null);
        Type localVarReturnType = new TypeToken<CustomRuleDTO>(){}.getType();
        return localVarApiClient.execute(localVarCall, localVarReturnType);
    }

    /**
     * Get a Custom Rule (asynchronously)
     * Retrieves a custom rule. We need to provide the policy Id as a path parameter.  **NOTE:** * Only super tenant users are allowed for this operation. 
     * @param ruleId Custom rule UUID  (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Policy returned  </td><td>  * Content-Type - The content type of the body.  <br>  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
        <tr><td> 406 </td><td> Not Acceptable. The requested media type is not supported. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call throttlingPoliciesCustomRuleIdGetAsync(String ruleId, final ApiCallback<CustomRuleDTO> _callback) throws ApiException {

        okhttp3.Call localVarCall = throttlingPoliciesCustomRuleIdGetValidateBeforeCall(ruleId, _callback);
        Type localVarReturnType = new TypeToken<CustomRuleDTO>(){}.getType();
        localVarApiClient.executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }
    /**
     * Build call for throttlingPoliciesCustomRuleIdPut
     * @param ruleId Custom rule UUID  (required)
     * @param contentType Media type of the entity in the body. Default is application/json.  (required)
     * @param customRuleDTO Policy object that needs to be modified  (required)
     * @param _callback Callback for upload/download progress
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Policy updated.  </td><td>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call throttlingPoliciesCustomRuleIdPutCall(String ruleId, String contentType, CustomRuleDTO customRuleDTO, final ApiCallback _callback) throws ApiException {
        Object localVarPostBody = customRuleDTO;

        // create path and map variables
        String localVarPath = "/throttling/policies/custom/{ruleId}"
            .replaceAll("\\{" + "ruleId" + "\\}", localVarApiClient.escapeString(ruleId.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        Map<String, String> localVarHeaderParams = new HashMap<String, String>();
        if (contentType != null) {
            localVarHeaderParams.put("Content-Type", localVarApiClient.parameterToString(contentType));
        }

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
    private okhttp3.Call throttlingPoliciesCustomRuleIdPutValidateBeforeCall(String ruleId, String contentType, CustomRuleDTO customRuleDTO, final ApiCallback _callback) throws ApiException {
        
        // verify the required parameter 'ruleId' is set
        if (ruleId == null) {
            throw new ApiException("Missing the required parameter 'ruleId' when calling throttlingPoliciesCustomRuleIdPut(Async)");
        }
        
        // verify the required parameter 'contentType' is set
        if (contentType == null) {
            throw new ApiException("Missing the required parameter 'contentType' when calling throttlingPoliciesCustomRuleIdPut(Async)");
        }
        
        // verify the required parameter 'customRuleDTO' is set
        if (customRuleDTO == null) {
            throw new ApiException("Missing the required parameter 'customRuleDTO' when calling throttlingPoliciesCustomRuleIdPut(Async)");
        }
        

        okhttp3.Call localVarCall = throttlingPoliciesCustomRuleIdPutCall(ruleId, contentType, customRuleDTO, _callback);
        return localVarCall;

    }

    /**
     * Update a Custom Rule
     * Updates an existing custom rule.  **NOTE:** * Only super tenant users are allowed for this operation. 
     * @param ruleId Custom rule UUID  (required)
     * @param contentType Media type of the entity in the body. Default is application/json.  (required)
     * @param customRuleDTO Policy object that needs to be modified  (required)
     * @return CustomRuleDTO
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Policy updated.  </td><td>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public CustomRuleDTO throttlingPoliciesCustomRuleIdPut(String ruleId, String contentType, CustomRuleDTO customRuleDTO) throws ApiException {
        ApiResponse<CustomRuleDTO> localVarResp = throttlingPoliciesCustomRuleIdPutWithHttpInfo(ruleId, contentType, customRuleDTO);
        return localVarResp.getData();
    }

    /**
     * Update a Custom Rule
     * Updates an existing custom rule.  **NOTE:** * Only super tenant users are allowed for this operation. 
     * @param ruleId Custom rule UUID  (required)
     * @param contentType Media type of the entity in the body. Default is application/json.  (required)
     * @param customRuleDTO Policy object that needs to be modified  (required)
     * @return ApiResponse&lt;CustomRuleDTO&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Policy updated.  </td><td>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public ApiResponse<CustomRuleDTO> throttlingPoliciesCustomRuleIdPutWithHttpInfo(String ruleId, String contentType, CustomRuleDTO customRuleDTO) throws ApiException {
        okhttp3.Call localVarCall = throttlingPoliciesCustomRuleIdPutValidateBeforeCall(ruleId, contentType, customRuleDTO, null);
        Type localVarReturnType = new TypeToken<CustomRuleDTO>(){}.getType();
        return localVarApiClient.execute(localVarCall, localVarReturnType);
    }

    /**
     * Update a Custom Rule (asynchronously)
     * Updates an existing custom rule.  **NOTE:** * Only super tenant users are allowed for this operation. 
     * @param ruleId Custom rule UUID  (required)
     * @param contentType Media type of the entity in the body. Default is application/json.  (required)
     * @param customRuleDTO Policy object that needs to be modified  (required)
     * @param _callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     * @http.response.details
     <table summary="Response Details" border="1">
        <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
        <tr><td> 200 </td><td> OK. Policy updated.  </td><td>  * Location - The URL of the newly created resource.  <br>  * Content-Type - The content type of the body.  <br>  </td></tr>
        <tr><td> 400 </td><td> Bad Request. Invalid request or validation error. </td><td>  -  </td></tr>
        <tr><td> 404 </td><td> Not Found. The specified resource does not exist. </td><td>  -  </td></tr>
     </table>
     */
    public okhttp3.Call throttlingPoliciesCustomRuleIdPutAsync(String ruleId, String contentType, CustomRuleDTO customRuleDTO, final ApiCallback<CustomRuleDTO> _callback) throws ApiException {

        okhttp3.Call localVarCall = throttlingPoliciesCustomRuleIdPutValidateBeforeCall(ruleId, contentType, customRuleDTO, _callback);
        Type localVarReturnType = new TypeToken<CustomRuleDTO>(){}.getType();
        localVarApiClient.executeAsync(localVarCall, localVarReturnType, _callback);
        return localVarCall;
    }
}
