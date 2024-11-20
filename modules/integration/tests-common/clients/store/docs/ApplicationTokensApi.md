# ApplicationTokensApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsApplicationIdKeysKeyTypeGenerateTokenPost**](ApplicationTokensApi.md#applicationsApplicationIdKeysKeyTypeGenerateTokenPost) | **POST** /applications/{applicationId}/keys/{keyType}/generate-token | Generate Application Token
[**applicationsApplicationIdOauthKeysKeyMappingIdGenerateTokenPost**](ApplicationTokensApi.md#applicationsApplicationIdOauthKeysKeyMappingIdGenerateTokenPost) | **POST** /applications/{applicationId}/oauth-keys/{keyMappingId}/generate-token | Generate Application Token


<a name="applicationsApplicationIdKeysKeyTypeGenerateTokenPost"></a>
# **applicationsApplicationIdKeysKeyTypeGenerateTokenPost**
> ApplicationTokenDTO applicationsApplicationIdKeysKeyTypeGenerateTokenPost(applicationId, keyType, applicationTokenGenerateRequestDTO, ifMatch)

Generate Application Token

Generate an access token for application by client_credentials grant type 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationTokensApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationTokensApi apiInstance = new ApplicationTokensApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
    ApplicationTokenGenerateRequestDTO applicationTokenGenerateRequestDTO = new ApplicationTokenGenerateRequestDTO(); // ApplicationTokenGenerateRequestDTO | Application token generation request object 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      ApplicationTokenDTO result = apiInstance.applicationsApplicationIdKeysKeyTypeGenerateTokenPost(applicationId, keyType, applicationTokenGenerateRequestDTO, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationTokensApi#applicationsApplicationIdKeysKeyTypeGenerateTokenPost");
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
 **applicationTokenGenerateRequestDTO** | [**ApplicationTokenGenerateRequestDTO**](ApplicationTokenGenerateRequestDTO.md)| Application token generation request object  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**ApplicationTokenDTO**](ApplicationTokenDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Token is generated.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="applicationsApplicationIdOauthKeysKeyMappingIdGenerateTokenPost"></a>
# **applicationsApplicationIdOauthKeysKeyMappingIdGenerateTokenPost**
> ApplicationTokenDTO applicationsApplicationIdOauthKeysKeyMappingIdGenerateTokenPost(applicationId, keyMappingId, applicationTokenGenerateRequestDTO, ifMatch)

Generate Application Token

Generate an access token for application by client_credentials grant type 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationTokensApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationTokensApi apiInstance = new ApplicationTokensApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyMappingId = "keyMappingId_example"; // String | OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping. 
    ApplicationTokenGenerateRequestDTO applicationTokenGenerateRequestDTO = new ApplicationTokenGenerateRequestDTO(); // ApplicationTokenGenerateRequestDTO | Application token generation request object 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      ApplicationTokenDTO result = apiInstance.applicationsApplicationIdOauthKeysKeyMappingIdGenerateTokenPost(applicationId, keyMappingId, applicationTokenGenerateRequestDTO, ifMatch);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationTokensApi#applicationsApplicationIdOauthKeysKeyMappingIdGenerateTokenPost");
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
 **keyMappingId** | **String**| OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping.  |
 **applicationTokenGenerateRequestDTO** | [**ApplicationTokenGenerateRequestDTO**](ApplicationTokenGenerateRequestDTO.md)| Application token generation request object  |
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

[**ApplicationTokenDTO**](ApplicationTokenDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Token is generated.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

