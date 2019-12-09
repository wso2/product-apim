# ApiKeysApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsApplicationIdApiKeysKeyTypeGeneratePost**](ApiKeysApi.md#applicationsApplicationIdApiKeysKeyTypeGeneratePost) | **POST** /applications/{applicationId}/api-keys/{keyType}/generate | Generate API Key
[**applicationsApplicationIdApiKeysKeyTypeRevokePost**](ApiKeysApi.md#applicationsApplicationIdApiKeysKeyTypeRevokePost) | **POST** /applications/{applicationId}/api-keys/{keyType}/revoke | Revoke API Key


<a name="applicationsApplicationIdApiKeysKeyTypeGeneratePost"></a>
# **applicationsApplicationIdApiKeysKeyTypeGeneratePost**
> APIKeyDTO applicationsApplicationIdApiKeysKeyTypeGeneratePost(applicationId, keyType, body, ifMatch)

Generate API Key

Generate a self contained API Key for the application 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiKeysApi apiInstance = new ApiKeysApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
APIKeyGenerateRequestDTO body = new APIKeyGenerateRequestDTO(); // APIKeyGenerateRequestDTO | API Key generation request object 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    APIKeyDTO result = apiInstance.applicationsApplicationIdApiKeysKeyTypeGeneratePost(applicationId, keyType, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiKeysApi#applicationsApplicationIdApiKeysKeyTypeGeneratePost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **keyType** | **String**| **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  | [enum: PRODUCTION, SANDBOX]
 **body** | [**APIKeyGenerateRequestDTO**](APIKeyGenerateRequestDTO.md)| API Key generation request object  | [optional]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**APIKeyDTO**](APIKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdApiKeysKeyTypeRevokePost"></a>
# **applicationsApplicationIdApiKeysKeyTypeRevokePost**
> applicationsApplicationIdApiKeysKeyTypeRevokePost(applicationId, keyType, body, ifMatch)

Revoke API Key

Revoke a self contained API Key for the application 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApiKeysApi apiInstance = new ApiKeysApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
APIKeyRevokeRequestDTO body = new APIKeyRevokeRequestDTO(); // APIKeyRevokeRequestDTO | API Key revoke request object 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.applicationsApplicationIdApiKeysKeyTypeRevokePost(applicationId, keyType, body, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApiKeysApi#applicationsApplicationIdApiKeysKeyTypeRevokePost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **keyType** | **String**| **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  | [enum: PRODUCTION, SANDBOX]
 **body** | [**APIKeyRevokeRequestDTO**](APIKeyRevokeRequestDTO.md)| API Key revoke request object  | [optional]
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

