# ApplicationTokensApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsApplicationIdKeysKeyTypeGenerateTokenPost**](ApplicationTokensApi.md#applicationsApplicationIdKeysKeyTypeGenerateTokenPost) | **POST** /applications/{applicationId}/keys/{keyType}/generate-token | Generate application token
[**applicationsApplicationIdOauthKeysKeyMappingIdGenerateTokenPost**](ApplicationTokensApi.md#applicationsApplicationIdOauthKeysKeyMappingIdGenerateTokenPost) | **POST** /applications/{applicationId}/oauth-keys/{keyMappingId}/generate-token | Generate application token


<a name="applicationsApplicationIdKeysKeyTypeGenerateTokenPost"></a>
# **applicationsApplicationIdKeysKeyTypeGenerateTokenPost**
> ApplicationTokenDTO applicationsApplicationIdKeysKeyTypeGenerateTokenPost(applicationId, keyType, body, ifMatch)

Generate application token

Generate an access token for application by client_credentials grant type 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationTokensApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationTokensApi apiInstance = new ApplicationTokensApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
ApplicationTokenGenerateRequestDTO body = new ApplicationTokenGenerateRequestDTO(); // ApplicationTokenGenerateRequestDTO | Application token generation request object 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    ApplicationTokenDTO result = apiInstance.applicationsApplicationIdKeysKeyTypeGenerateTokenPost(applicationId, keyType, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationTokensApi#applicationsApplicationIdKeysKeyTypeGenerateTokenPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **keyType** | **String**| **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  | [enum: PRODUCTION, SANDBOX]
 **body** | [**ApplicationTokenGenerateRequestDTO**](ApplicationTokenGenerateRequestDTO.md)| Application token generation request object  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**ApplicationTokenDTO**](ApplicationTokenDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdOauthKeysKeyMappingIdGenerateTokenPost"></a>
# **applicationsApplicationIdOauthKeysKeyMappingIdGenerateTokenPost**
> ApplicationTokenDTO applicationsApplicationIdOauthKeysKeyMappingIdGenerateTokenPost(applicationId, keyMappingId, body, ifMatch)

Generate application token

Generate an access token for application by client_credentials grant type 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationTokensApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationTokensApi apiInstance = new ApplicationTokensApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String keyMappingId = "keyMappingId_example"; // String | OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping. 
ApplicationTokenGenerateRequestDTO body = new ApplicationTokenGenerateRequestDTO(); // ApplicationTokenGenerateRequestDTO | Application token generation request object 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    ApplicationTokenDTO result = apiInstance.applicationsApplicationIdOauthKeysKeyMappingIdGenerateTokenPost(applicationId, keyMappingId, body, ifMatch);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationTokensApi#applicationsApplicationIdOauthKeysKeyMappingIdGenerateTokenPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **keyMappingId** | **String**| OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping.  |
 **body** | [**ApplicationTokenGenerateRequestDTO**](ApplicationTokenGenerateRequestDTO.md)| Application token generation request object  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**ApplicationTokenDTO**](ApplicationTokenDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

