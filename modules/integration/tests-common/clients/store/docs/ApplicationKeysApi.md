# ApplicationKeysApi

All URIs are relative to *https://apis.wso2.com/api/am/store/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**applicationsApplicationIdGenerateKeysPost**](ApplicationKeysApi.md#applicationsApplicationIdGenerateKeysPost) | **POST** /applications/{applicationId}/generate-keys | Generate application keys
[**applicationsApplicationIdKeysGet**](ApplicationKeysApi.md#applicationsApplicationIdKeysGet) | **GET** /applications/{applicationId}/keys | Retrieve all application keys
[**applicationsApplicationIdKeysKeyTypeCleanUpPost**](ApplicationKeysApi.md#applicationsApplicationIdKeysKeyTypeCleanUpPost) | **POST** /applications/{applicationId}/keys/{keyType}/clean-up | Clean up application keys
[**applicationsApplicationIdKeysKeyTypeGet**](ApplicationKeysApi.md#applicationsApplicationIdKeysKeyTypeGet) | **GET** /applications/{applicationId}/keys/{keyType} | Get key details of a given type 
[**applicationsApplicationIdKeysKeyTypePut**](ApplicationKeysApi.md#applicationsApplicationIdKeysKeyTypePut) | **PUT** /applications/{applicationId}/keys/{keyType} | Update grant types and callback url of an application 
[**applicationsApplicationIdKeysKeyTypeRegenerateSecretPost**](ApplicationKeysApi.md#applicationsApplicationIdKeysKeyTypeRegenerateSecretPost) | **POST** /applications/{applicationId}/keys/{keyType}/regenerate-secret | Re-generate consumer secret 
[**applicationsApplicationIdMapKeysPost**](ApplicationKeysApi.md#applicationsApplicationIdMapKeysPost) | **POST** /applications/{applicationId}/map-keys | Map application keys
[**applicationsApplicationIdOauthKeysGet**](ApplicationKeysApi.md#applicationsApplicationIdOauthKeysGet) | **GET** /applications/{applicationId}/oauth-keys | Retrieve all application keys
[**applicationsApplicationIdOauthKeysKeyMappingIdCleanUpPost**](ApplicationKeysApi.md#applicationsApplicationIdOauthKeysKeyMappingIdCleanUpPost) | **POST** /applications/{applicationId}/oauth-keys/{keyMappingId}/clean-up | Clean up application keys
[**applicationsApplicationIdOauthKeysKeyMappingIdGet**](ApplicationKeysApi.md#applicationsApplicationIdOauthKeysKeyMappingIdGet) | **GET** /applications/{applicationId}/oauth-keys/{keyMappingId} | Get key details of a given type 
[**applicationsApplicationIdOauthKeysKeyMappingIdPut**](ApplicationKeysApi.md#applicationsApplicationIdOauthKeysKeyMappingIdPut) | **PUT** /applications/{applicationId}/oauth-keys/{keyMappingId} | Update grant types and callback url of an application 
[**applicationsApplicationIdOauthKeysKeyMappingIdRegenerateSecretPost**](ApplicationKeysApi.md#applicationsApplicationIdOauthKeysKeyMappingIdRegenerateSecretPost) | **POST** /applications/{applicationId}/oauth-keys/{keyMappingId}/regenerate-secret | Re-generate consumer secret 


<a name="applicationsApplicationIdGenerateKeysPost"></a>
# **applicationsApplicationIdGenerateKeysPost**
> ApplicationKeyDTO applicationsApplicationIdGenerateKeysPost(applicationId, body)

Generate application keys

Generate keys (Consumer key/secret) for application 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationKeysApi apiInstance = new ApplicationKeysApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
ApplicationKeyGenerateRequestDTO body = new ApplicationKeyGenerateRequestDTO(); // ApplicationKeyGenerateRequestDTO | Application key generation request object 
try {
    ApplicationKeyDTO result = apiInstance.applicationsApplicationIdGenerateKeysPost(applicationId, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdGenerateKeysPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **body** | [**ApplicationKeyGenerateRequestDTO**](ApplicationKeyGenerateRequestDTO.md)| Application key generation request object  |

### Return type

[**ApplicationKeyDTO**](ApplicationKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdKeysGet"></a>
# **applicationsApplicationIdKeysGet**
> ApplicationKeyListDTO applicationsApplicationIdKeysGet(applicationId)

Retrieve all application keys

Retrieve keys (Consumer key/secret) of application 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationKeysApi apiInstance = new ApplicationKeysApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
try {
    ApplicationKeyListDTO result = apiInstance.applicationsApplicationIdKeysGet(applicationId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdKeysGet");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdKeysKeyTypeCleanUpPost"></a>
# **applicationsApplicationIdKeysKeyTypeCleanUpPost**
> applicationsApplicationIdKeysKeyTypeCleanUpPost(applicationId, keyType, ifMatch)

Clean up application keys

Clean up keys after failed key generation of an application 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationKeysApi apiInstance = new ApplicationKeysApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.applicationsApplicationIdKeysKeyTypeCleanUpPost(applicationId, keyType, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdKeysKeyTypeCleanUpPost");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdKeysKeyTypeGet"></a>
# **applicationsApplicationIdKeysKeyTypeGet**
> ApplicationKeyDTO applicationsApplicationIdKeysKeyTypeGet(applicationId, keyType, groupId)

Get key details of a given type 

This operation can be used to retrieve key details of an individual application specifying the key type in the URI. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationKeysApi apiInstance = new ApplicationKeysApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
String groupId = "groupId_example"; // String | Application Group Id 
try {
    ApplicationKeyDTO result = apiInstance.applicationsApplicationIdKeysKeyTypeGet(applicationId, keyType, groupId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdKeysKeyTypeGet");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdKeysKeyTypePut"></a>
# **applicationsApplicationIdKeysKeyTypePut**
> ApplicationKeyDTO applicationsApplicationIdKeysKeyTypePut(applicationId, keyType, body)

Update grant types and callback url of an application 

This operation can be used to update grant types and callback url of an application. (Consumer Key and Consumer Secret are ignored) Upon succesfull you will retrieve the updated key details as the response. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationKeysApi apiInstance = new ApplicationKeysApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
ApplicationKeyDTO body = new ApplicationKeyDTO(); // ApplicationKeyDTO | Grant types/Callback URL update request object 
try {
    ApplicationKeyDTO result = apiInstance.applicationsApplicationIdKeysKeyTypePut(applicationId, keyType, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdKeysKeyTypePut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **keyType** | **String**| **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox).  | [enum: PRODUCTION, SANDBOX]
 **body** | [**ApplicationKeyDTO**](ApplicationKeyDTO.md)| Grant types/Callback URL update request object  |

### Return type

[**ApplicationKeyDTO**](ApplicationKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdKeysKeyTypeRegenerateSecretPost"></a>
# **applicationsApplicationIdKeysKeyTypeRegenerateSecretPost**
> ApplicationKeyReGenerateResponseDTO applicationsApplicationIdKeysKeyTypeRegenerateSecretPost(applicationId, keyType)

Re-generate consumer secret 

This operation can be used to re generate consumer secret for an application for the give key type 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationKeysApi apiInstance = new ApplicationKeysApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String keyType = "keyType_example"; // String | **Application Key Type** standing for the type of the keys (i.e. Production or Sandbox). 
try {
    ApplicationKeyReGenerateResponseDTO result = apiInstance.applicationsApplicationIdKeysKeyTypeRegenerateSecretPost(applicationId, keyType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdKeysKeyTypeRegenerateSecretPost");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdMapKeysPost"></a>
# **applicationsApplicationIdMapKeysPost**
> ApplicationKeyDTO applicationsApplicationIdMapKeysPost(applicationId, body)

Map application keys

Map keys (Consumer key/secret) to an application 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationKeysApi apiInstance = new ApplicationKeysApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
ApplicationKeyMappingRequestDTO body = new ApplicationKeyMappingRequestDTO(); // ApplicationKeyMappingRequestDTO | Application key mapping request object 
try {
    ApplicationKeyDTO result = apiInstance.applicationsApplicationIdMapKeysPost(applicationId, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdMapKeysPost");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **body** | [**ApplicationKeyMappingRequestDTO**](ApplicationKeyMappingRequestDTO.md)| Application key mapping request object  |

### Return type

[**ApplicationKeyDTO**](ApplicationKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdOauthKeysGet"></a>
# **applicationsApplicationIdOauthKeysGet**
> ApplicationKeyListDTO applicationsApplicationIdOauthKeysGet(applicationId)

Retrieve all application keys

Retrieve keys (Consumer key/secret) of application 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationKeysApi apiInstance = new ApplicationKeysApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
try {
    ApplicationKeyListDTO result = apiInstance.applicationsApplicationIdOauthKeysGet(applicationId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdOauthKeysGet");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdOauthKeysKeyMappingIdCleanUpPost"></a>
# **applicationsApplicationIdOauthKeysKeyMappingIdCleanUpPost**
> applicationsApplicationIdOauthKeysKeyMappingIdCleanUpPost(applicationId, keyMappingId, ifMatch)

Clean up application keys

Clean up keys after failed key generation of an application 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationKeysApi apiInstance = new ApplicationKeysApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String keyMappingId = "keyMappingId_example"; // String | OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping. 
String ifMatch = "ifMatch_example"; // String | Validator for conditional requests; based on ETag. 
try {
    apiInstance.applicationsApplicationIdOauthKeysKeyMappingIdCleanUpPost(applicationId, keyMappingId, ifMatch);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdOauthKeysKeyMappingIdCleanUpPost");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdOauthKeysKeyMappingIdGet"></a>
# **applicationsApplicationIdOauthKeysKeyMappingIdGet**
> ApplicationKeyDTO applicationsApplicationIdOauthKeysKeyMappingIdGet(applicationId, keyMappingId, groupId)

Get key details of a given type 

This operation can be used to retrieve key details of an individual application specifying the key type in the URI. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationKeysApi apiInstance = new ApplicationKeysApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String keyMappingId = "keyMappingId_example"; // String | OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping. 
String groupId = "groupId_example"; // String | Application Group Id 
try {
    ApplicationKeyDTO result = apiInstance.applicationsApplicationIdOauthKeysKeyMappingIdGet(applicationId, keyMappingId, groupId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdOauthKeysKeyMappingIdGet");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdOauthKeysKeyMappingIdPut"></a>
# **applicationsApplicationIdOauthKeysKeyMappingIdPut**
> ApplicationKeyDTO applicationsApplicationIdOauthKeysKeyMappingIdPut(applicationId, keyMappingId, body)

Update grant types and callback url of an application 

This operation can be used to update grant types and callback url of an application. (Consumer Key and Consumer Secret are ignored) Upon succesfull you will retrieve the updated key details as the response. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationKeysApi apiInstance = new ApplicationKeysApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String keyMappingId = "keyMappingId_example"; // String | OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping. 
ApplicationKeyDTO body = new ApplicationKeyDTO(); // ApplicationKeyDTO | Grant types/Callback URL update request object 
try {
    ApplicationKeyDTO result = apiInstance.applicationsApplicationIdOauthKeysKeyMappingIdPut(applicationId, keyMappingId, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdOauthKeysKeyMappingIdPut");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **applicationId** | **String**| Application Identifier consisting of the UUID of the Application.  |
 **keyMappingId** | **String**| OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping.  |
 **body** | [**ApplicationKeyDTO**](ApplicationKeyDTO.md)| Grant types/Callback URL update request object  |

### Return type

[**ApplicationKeyDTO**](ApplicationKeyDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="applicationsApplicationIdOauthKeysKeyMappingIdRegenerateSecretPost"></a>
# **applicationsApplicationIdOauthKeysKeyMappingIdRegenerateSecretPost**
> ApplicationKeyReGenerateResponseDTO applicationsApplicationIdOauthKeysKeyMappingIdRegenerateSecretPost(applicationId, keyMappingId)

Re-generate consumer secret 

This operation can be used to re generate consumer secret for an application for the give key type 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.store.api.ApiClient;
//import org.wso2.am.integration.clients.store.api.ApiException;
//import org.wso2.am.integration.clients.store.api.Configuration;
//import org.wso2.am.integration.clients.store.api.auth.*;
//import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ApplicationKeysApi apiInstance = new ApplicationKeysApi();
String applicationId = "applicationId_example"; // String | Application Identifier consisting of the UUID of the Application. 
String keyMappingId = "keyMappingId_example"; // String | OAuth Key Identifier consisting of the UUID of the Oauth Key Mapping. 
try {
    ApplicationKeyReGenerateResponseDTO result = apiInstance.applicationsApplicationIdOauthKeysKeyMappingIdRegenerateSecretPost(applicationId, keyMappingId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ApplicationKeysApi#applicationsApplicationIdOauthKeysKeyMappingIdRegenerateSecretPost");
    e.printStackTrace();
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

 - **Content-Type**: application/json
 - **Accept**: application/json

