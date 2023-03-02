# ApplicationKeysApi

All URIs are relative to *https://apis.wso2.com/api/am/devportal/v3*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsApplicationIdGenerateKeysPost**](ApplicationKeysApi.md#applicationsApplicationIdGenerateKeysPost) | **POST** /applications/{applicationId}/generate-keys | Generate Application Keys
[**applicationsApplicationIdKeysGet**](ApplicationKeysApi.md#applicationsApplicationIdKeysGet) | **GET** /applications/{applicationId}/keys | Retrieve All Application Keys
[**applicationsApplicationIdKeysKeyTypeCleanUpPost**](ApplicationKeysApi.md#applicationsApplicationIdKeysKeyTypeCleanUpPost) | **POST** /applications/{applicationId}/keys/{keyType}/clean-up | Clean-Up Application Keys
[**applicationsApplicationIdKeysKeyTypeGet**](ApplicationKeysApi.md#applicationsApplicationIdKeysKeyTypeGet) | **GET** /applications/{applicationId}/keys/{keyType} | Get Key Details of a Given Type 
[**applicationsApplicationIdKeysKeyTypePut**](ApplicationKeysApi.md#applicationsApplicationIdKeysKeyTypePut) | **PUT** /applications/{applicationId}/keys/{keyType} | Update Grant Types and Callback Url of an Application 
[**applicationsApplicationIdKeysKeyTypeRegenerateSecretPost**](ApplicationKeysApi.md#applicationsApplicationIdKeysKeyTypeRegenerateSecretPost) | **POST** /applications/{applicationId}/keys/{keyType}/regenerate-secret | Re-Generate Consumer Secret 
[**applicationsApplicationIdMapKeysPost**](ApplicationKeysApi.md#applicationsApplicationIdMapKeysPost) | **POST** /applications/{applicationId}/map-keys | Map Application Keys
[**applicationsApplicationIdOauthKeysGet**](ApplicationKeysApi.md#applicationsApplicationIdOauthKeysGet) | **GET** /applications/{applicationId}/oauth-keys | Retrieve All Application Keys
[**applicationsApplicationIdOauthKeysKeyMappingIdCleanUpPost**](ApplicationKeysApi.md#applicationsApplicationIdOauthKeysKeyMappingIdCleanUpPost) | **POST** /applications/{applicationId}/oauth-keys/{keyMappingId}/clean-up | Clean-Up Application Keys
[**applicationsApplicationIdOauthKeysKeyMappingIdGet**](ApplicationKeysApi.md#applicationsApplicationIdOauthKeysKeyMappingIdGet) | **GET** /applications/{applicationId}/oauth-keys/{keyMappingId} | Get Key Details of a Given Type 
[**applicationsApplicationIdOauthKeysKeyMappingIdPut**](ApplicationKeysApi.md#applicationsApplicationIdOauthKeysKeyMappingIdPut) | **PUT** /applications/{applicationId}/oauth-keys/{keyMappingId} | Update Grant Types and Callback URL of an Application 
[**applicationsApplicationIdOauthKeysKeyMappingIdRegenerateSecretPost**](ApplicationKeysApi.md#applicationsApplicationIdOauthKeysKeyMappingIdRegenerateSecretPost) | **POST** /applications/{applicationId}/oauth-keys/{keyMappingId}/regenerate-secret | Re-Generate Consumer Secret 


<a name="applicationsApplicationIdGenerateKeysPost"></a>
# **applicationsApplicationIdGenerateKeysPost**
> ApplicationKeyDTO applicationsApplicationIdGenerateKeysPost(applicationId, applicationKeyGenerateRequestDTO, xWSO2Tenant)

Generate Application Keys

Generate keys (Consumer key/secret) for application 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationKeysApi apiInstance = new ApplicationKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    ApplicationKeyGenerateRequestDTO applicationKeyGenerateRequestDTO = new ApplicationKeyGenerateRequestDTO(); // ApplicationKeyGenerateRequestDTO | Application key generation request object 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    try {
      ApplicationKeyDTO result = apiInstance.applicationsApplicationIdGenerateKeysPost(applicationId, applicationKeyGenerateRequestDTO, xWSO2Tenant);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdGenerateKeysPost");
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
 **applicationKeyGenerateRequestDTO** | [**ApplicationKeyGenerateRequestDTO**](ApplicationKeyGenerateRequestDTO.md)| Application key generation request object  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]

### Return type

[**ApplicationKeyDTO**](ApplicationKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Keys are generated.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="applicationsApplicationIdKeysGet"></a>
# **applicationsApplicationIdKeysGet**
> ApplicationKeyListDTO applicationsApplicationIdKeysGet(applicationId)

Retrieve All Application Keys

Retrieve keys (Consumer key/secret) of application 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationKeysApi apiInstance = new ApplicationKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    try {
      ApplicationKeyListDTO result = apiInstance.applicationsApplicationIdKeysGet(applicationId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdKeysGet");
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

### Return type

[**ApplicationKeyListDTO**](ApplicationKeyListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Keys are returned.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="applicationsApplicationIdKeysKeyTypeCleanUpPost"></a>
# **applicationsApplicationIdKeysKeyTypeCleanUpPost**
> applicationsApplicationIdKeysKeyTypeCleanUpPost(applicationId, keyType, ifMatch)

Clean-Up Application Keys

Clean up keys after failed key generation of an application 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationKeysApi apiInstance = new ApplicationKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.applicationsApplicationIdKeysKeyTypeCleanUpPost(applicationId, keyType, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdKeysKeyTypeCleanUpPost");
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

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Clean up is performed  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="applicationsApplicationIdKeysKeyTypeGet"></a>
# **applicationsApplicationIdKeysKeyTypeGet**
> ApplicationKeyDTO applicationsApplicationIdKeysKeyTypeGet(applicationId, keyType, groupId)

Get Key Details of a Given Type 

This operation can be used to retrieve key details of an individual application specifying the key type in the URI. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationKeysApi apiInstance = new ApplicationKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
    String groupId = "groupId_example"; // String | Application Group Id 
    try {
      ApplicationKeyDTO result = apiInstance.applicationsApplicationIdKeysKeyTypeGet(applicationId, keyType, groupId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdKeysKeyTypeGet");
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
 **groupId** | **String**| Application Group Id  | [optional]

### Return type

[**ApplicationKeyDTO**](ApplicationKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Keys of given type are returned.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="applicationsApplicationIdKeysKeyTypePut"></a>
# **applicationsApplicationIdKeysKeyTypePut**
> ApplicationKeyDTO applicationsApplicationIdKeysKeyTypePut(applicationId, keyType, applicationKeyDTO)

Update Grant Types and Callback Url of an Application 

This operation can be used to update grant types and callback url of an application. (Consumer Key and Consumer Secret are ignored) Upon succesfull you will retrieve the updated key details as the response. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationKeysApi apiInstance = new ApplicationKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
    ApplicationKeyDTO applicationKeyDTO = new ApplicationKeyDTO(); // ApplicationKeyDTO | Grant types/Callback URL update request object 
    try {
      ApplicationKeyDTO result = apiInstance.applicationsApplicationIdKeysKeyTypePut(applicationId, keyType, applicationKeyDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdKeysKeyTypePut");
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
 **applicationKeyDTO** | [**ApplicationKeyDTO**](ApplicationKeyDTO.md)| Grant types/Callback URL update request object  |

### Return type

[**ApplicationKeyDTO**](ApplicationKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Ok. Grant types or/and callback url is/are updated.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="applicationsApplicationIdKeysKeyTypeRegenerateSecretPost"></a>
# **applicationsApplicationIdKeysKeyTypeRegenerateSecretPost**
> ApplicationKeyReGenerateResponseDTO applicationsApplicationIdKeysKeyTypeRegenerateSecretPost(applicationId, keyType)

Re-Generate Consumer Secret 

This operation can be used to re generate consumer secret for an application for the give key type 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationKeysApi apiInstance = new ApplicationKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
    try {
      ApplicationKeyReGenerateResponseDTO result = apiInstance.applicationsApplicationIdKeysKeyTypeRegenerateSecretPost(applicationId, keyType);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdKeysKeyTypeRegenerateSecretPost");
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

### Return type

[**ApplicationKeyReGenerateResponseDTO**](ApplicationKeyReGenerateResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Keys are re generated.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).‚  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="applicationsApplicationIdMapKeysPost"></a>
# **applicationsApplicationIdMapKeysPost**
> ApplicationKeyDTO applicationsApplicationIdMapKeysPost(applicationId, applicationKeyMappingRequestDTO, xWSO2Tenant)

Map Application Keys

Map keys (Consumer key/secret) to an application 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationKeysApi apiInstance = new ApplicationKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    ApplicationKeyMappingRequestDTO applicationKeyMappingRequestDTO = new ApplicationKeyMappingRequestDTO(); // ApplicationKeyMappingRequestDTO | Application key mapping request object 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    try {
      ApplicationKeyDTO result = apiInstance.applicationsApplicationIdMapKeysPost(applicationId, applicationKeyMappingRequestDTO, xWSO2Tenant);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdMapKeysPost");
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
 **applicationKeyMappingRequestDTO** | [**ApplicationKeyMappingRequestDTO**](ApplicationKeyMappingRequestDTO.md)| Application key mapping request object  |
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]

### Return type

[**ApplicationKeyDTO**](ApplicationKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Keys are mapped.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="applicationsApplicationIdOauthKeysGet"></a>
# **applicationsApplicationIdOauthKeysGet**
> ApplicationKeyListDTO applicationsApplicationIdOauthKeysGet(applicationId, xWSO2Tenant)

Retrieve All Application Keys

Retrieve keys (Consumer key/secret) of application 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationKeysApi apiInstance = new ApplicationKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String xWSO2Tenant = "xWSO2Tenant_example"; // String | For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from. 
    try {
      ApplicationKeyListDTO result = apiInstance.applicationsApplicationIdOauthKeysGet(applicationId, xWSO2Tenant);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdOauthKeysGet");
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
 **xWSO2Tenant** | **String**| For cross-tenant invocations, this is used to specify the tenant domain, where the resource need to be   retrieved from.  | [optional]

### Return type

[**ApplicationKeyListDTO**](ApplicationKeyListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Keys are returned.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="applicationsApplicationIdOauthKeysKeyMappingIdCleanUpPost"></a>
# **applicationsApplicationIdOauthKeysKeyMappingIdCleanUpPost**
> applicationsApplicationIdOauthKeysKeyMappingIdCleanUpPost(applicationId, keyMappingId, ifMatch)

Clean-Up Application Keys

Clean up keys after failed key generation of an application 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationKeysApi apiInstance = new ApplicationKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyMappingId = "keyMappingId_example"; // String | OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping. 
    String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
    try {
      apiInstance.applicationsApplicationIdOauthKeysKeyMappingIdCleanUpPost(applicationId, keyMappingId, ifMatch);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdOauthKeysKeyMappingIdCleanUpPost");
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
 **ifMatch** | **String**| Validator for conditional requests; based on ETag.  | [optional]

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Clean up is performed  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="applicationsApplicationIdOauthKeysKeyMappingIdGet"></a>
# **applicationsApplicationIdOauthKeysKeyMappingIdGet**
> ApplicationKeyDTO applicationsApplicationIdOauthKeysKeyMappingIdGet(applicationId, keyMappingId, groupId)

Get Key Details of a Given Type 

This operation can be used to retrieve key details of an individual application specifying the key type in the URI. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationKeysApi apiInstance = new ApplicationKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyMappingId = "keyMappingId_example"; // String | OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping. 
    String groupId = "groupId_example"; // String | Application Group Id 
    try {
      ApplicationKeyDTO result = apiInstance.applicationsApplicationIdOauthKeysKeyMappingIdGet(applicationId, keyMappingId, groupId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdOauthKeysKeyMappingIdGet");
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
 **groupId** | **String**| Application Group Id  | [optional]

### Return type

[**ApplicationKeyDTO**](ApplicationKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Keys of given type are returned.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="applicationsApplicationIdOauthKeysKeyMappingIdPut"></a>
# **applicationsApplicationIdOauthKeysKeyMappingIdPut**
> ApplicationKeyDTO applicationsApplicationIdOauthKeysKeyMappingIdPut(applicationId, keyMappingId, applicationKeyDTO)

Update Grant Types and Callback URL of an Application 

This operation can be used to update grant types and callback url of an application. (Consumer Key and Consumer Secret are ignored) Upon succesfull you will retrieve the updated key details as the response. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationKeysApi apiInstance = new ApplicationKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyMappingId = "keyMappingId_example"; // String | OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping. 
    ApplicationKeyDTO applicationKeyDTO = new ApplicationKeyDTO(); // ApplicationKeyDTO | Grant types/Callback URL update request object 
    try {
      ApplicationKeyDTO result = apiInstance.applicationsApplicationIdOauthKeysKeyMappingIdPut(applicationId, keyMappingId, applicationKeyDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdOauthKeysKeyMappingIdPut");
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
 **applicationKeyDTO** | [**ApplicationKeyDTO**](ApplicationKeyDTO.md)| Grant types/Callback URL update request object  |

### Return type

[**ApplicationKeyDTO**](ApplicationKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Ok. Grant types or/and callback url is/are updated.  |  -  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

<a name="applicationsApplicationIdOauthKeysKeyMappingIdRegenerateSecretPost"></a>
# **applicationsApplicationIdOauthKeysKeyMappingIdRegenerateSecretPost**
> ApplicationKeyReGenerateResponseDTO applicationsApplicationIdOauthKeysKeyMappingIdRegenerateSecretPost(applicationId, keyMappingId)

Re-Generate Consumer Secret 

This operation can be used to re generate consumer secret for an application for the give key type 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.Configuration;
import org.wso2.am.integration.clients.store.api.auth.*;
import org.wso2.am.integration.clients.store.api.models.*;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/devportal/v3");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ApplicationKeysApi apiInstance = new ApplicationKeysApi(defaultClient);
    String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
    String keyMappingId = "keyMappingId_example"; // String | OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping. 
    try {
      ApplicationKeyReGenerateResponseDTO result = apiInstance.applicationsApplicationIdOauthKeysKeyMappingIdRegenerateSecretPost(applicationId, keyMappingId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdOauthKeysKeyMappingIdRegenerateSecretPost");
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

### Return type

[**ApplicationKeyReGenerateResponseDTO**](ApplicationKeyReGenerateResponseDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Keys are re generated.  |  * ETag - Entity Tag of the response resource. Used by caches, or in conditional requests (Will be supported in future).  <br>  * Last-Modified - Date and time the resource has been modifed the last time. Used by caches, or in conditional requests (Will be supported in future).‚  <br>  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |
**412** | Precondition Failed. The request has not been performed because one of the preconditions is not met. |  -  |

