# ApiKeysApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsApplicationIdApiKeysKeyTypeGeneratePost**](ApiKeysApi.md#applicationsApplicationIdApiKeysKeyTypeGeneratePost) | **POST** /applications/{applicationId}/api-keys/{keyType}/generate | Generate API Key
[**applicationsApplicationIdApiKeysKeyTypeRevokePost**](ApiKeysApi.md#applicationsApplicationIdApiKeysKeyTypeRevokePost) | **POST** /applications/{applicationId}/api-keys/{keyType}/revoke | Revoke API Key


<a name="applicationsApplicationIdApiKeysKeyTypeGeneratePost"></a>
# **applicationsApplicationIdApiKeysKeyTypeGeneratePost**
> APIKeyDTO applicationsApplicationIdApiKeysKeyTypeGeneratePost(applicationId, keyType, ifMatch, apIKeyGenerateRequestDTO)

Generate API Key

Generate a self contained API Key for the application 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiKeysApi apiInstance = new ApiKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    APIKeyGenerateRequestDTO apIKeyGenerateRequestDTO = new APIKeyGenerateRequestDTO(); // APIKeyGenerateRequestDTO | API Key generation request object 
    try {
      APIKeyDTO result = apiInstance.applicationsApplicationIdApiKeysKeyTypeGeneratePost(applicationId, keyType, ifMatch, apIKeyGenerateRequestDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiKeysApi#applicationsApplicationIdApiKeysKeyTypeGeneratePost");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **keyType** | **String**| **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  | [enum: PRODUCTION, SANDBOX]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]
 **apIKeyGenerateRequestDTO** | [**APIKeyGenerateRequestDTO**](APIKeyGenerateRequestDTO.md)| API Key generation request object  | [optional]

### Return type

[**APIKeyDTO**](APIKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. apikey generated.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="applicationsApplicationIdApiKeysKeyTypeRevokePost"></a>
# **applicationsApplicationIdApiKeysKeyTypeRevokePost**
> applicationsApplicationIdApiKeysKeyTypeRevokePost(applicationId, keyType, ifMatch, apIKeyRevokeRequestDTO)

Revoke API Key

Revoke a self contained API Key for the application 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApiKeysApi apiInstance = new ApiKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    APIKeyRevokeRequestDTO apIKeyRevokeRequestDTO = new APIKeyRevokeRequestDTO(); // APIKeyRevokeRequestDTO | API Key revoke request object 
    try {
      apiInstance.applicationsApplicationIdApiKeysKeyTypeRevokePost(applicationId, keyType, ifMatch, apIKeyRevokeRequestDTO);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApiKeysApi#applicationsApplicationIdApiKeysKeyTypeRevokePost");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **keyType** | **String**| **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  | [enum: PRODUCTION, SANDBOX]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]
 **apIKeyRevokeRequestDTO** | [**APIKeyRevokeRequestDTO**](APIKeyRevokeRequestDTO.md)| API Key revoke request object  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. apikey revoked successfully.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

