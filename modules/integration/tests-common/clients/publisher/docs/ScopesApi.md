# ScopesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addSharedScope**](ScopesApi.md#addSharedScope) | **POST** /scopes | Add a new Shared Scope
[**deleteSharedScope**](ScopesApi.md#deleteSharedScope) | **DELETE** /scopes/{scopeId} | Delete a Shared Scope
[**getSharedScope**](ScopesApi.md#getSharedScope) | **GET** /scopes/{scopeId} | Get a Shared Scope by Scope Id
[**getSharedScopeUsages**](ScopesApi.md#getSharedScopeUsages) | **GET** /scopes/{scopeId}/usage | Get usages of a Shared Scope by Scope Id
[**getSharedScopes**](ScopesApi.md#getSharedScopes) | **GET** /scopes | Get all available Shared Scopes
[**updateSharedScope**](ScopesApi.md#updateSharedScope) | **PUT** /scopes/{scopeId} | Update a Shared Scope
[**validateScope**](ScopesApi.md#validateScope) | **HEAD** /scopes/{scopeId} | Check given scope name is already exist


<a name="addSharedScope"></a>
# **addSharedScope**
> ScopeDTO addSharedScope(body)

Add a new Shared Scope

This operation can be used to add a new Shared Scope. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ScopesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ScopesApi apiInstance = new ScopesApi();
ScopeDTO body = new ScopeDTO(); // ScopeDTO | Scope object that needs to be added 
try {
    ScopeDTO result = apiInstance.addSharedScope(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ScopesApi#addSharedScope");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**ScopeDTO**](ScopeDTO.md)| Scope object that needs to be added  |

### Return type

[**ScopeDTO**](ScopeDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="deleteSharedScope"></a>
# **deleteSharedScope**
> deleteSharedScope(scopeId)

Delete a Shared Scope

This operation can be used to delete a Shared Scope proving the Id of the scope. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ScopesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ScopesApi apiInstance = new ScopesApi();
String scopeId = "scopeId_example"; // String | Scope Id consisting the UUID of the shared scope 
try {
    apiInstance.deleteSharedScope(scopeId);
} catch (ApiException e) {
    System.err.println("Exception when calling ScopesApi#deleteSharedScope");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **scopeId** | **String**| Scope Id consisting the UUID of the shared scope  |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getSharedScope"></a>
# **getSharedScope**
> ScopeDTO getSharedScope(scopeId)

Get a Shared Scope by Scope Id

This operation can be used to retrieve details of a Shared Scope by a given scope Id. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ScopesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ScopesApi apiInstance = new ScopesApi();
String scopeId = "scopeId_example"; // String | Scope Id consisting the UUID of the shared scope 
try {
    ScopeDTO result = apiInstance.getSharedScope(scopeId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ScopesApi#getSharedScope");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **scopeId** | **String**| Scope Id consisting the UUID of the shared scope  |

### Return type

[**ScopeDTO**](ScopeDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getSharedScopeUsages"></a>
# **getSharedScopeUsages**
> SharedScopeUsageDTO getSharedScopeUsages(scopeId)

Get usages of a Shared Scope by Scope Id

This operation can be used to retrieve usages of a Shared Scope by a given scope Id. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ScopesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ScopesApi apiInstance = new ScopesApi();
String scopeId = "scopeId_example"; // String | Scope Id consisting the UUID of the shared scope 
try {
    SharedScopeUsageDTO result = apiInstance.getSharedScopeUsages(scopeId);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ScopesApi#getSharedScopeUsages");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **scopeId** | **String**| Scope Id consisting the UUID of the shared scope  |

### Return type

[**SharedScopeUsageDTO**](SharedScopeUsageDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getSharedScopes"></a>
# **getSharedScopes**
> ScopeListDTO getSharedScopes(limit, offset)

Get all available Shared Scopes

This operation can be used to get all the available Shared Scopes. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ScopesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ScopesApi apiInstance = new ScopesApi();
Integer limit = 25; // Integer | Maximum size of resource array to return. 
Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
try {
    ScopeListDTO result = apiInstance.getSharedScopes(limit, offset);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ScopesApi#getSharedScopes");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]

### Return type

[**ScopeListDTO**](ScopeListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="updateSharedScope"></a>
# **updateSharedScope**
> ScopeDTO updateSharedScope(scopeId, body)

Update a Shared Scope

This operation can be used to update a Shared Scope by a given scope Id. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ScopesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ScopesApi apiInstance = new ScopesApi();
String scopeId = "scopeId_example"; // String | Scope Id consisting the UUID of the shared scope 
ScopeDTO body = new ScopeDTO(); // ScopeDTO | Scope object that needs to be updated 
try {
    ScopeDTO result = apiInstance.updateSharedScope(scopeId, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ScopesApi#updateSharedScope");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **scopeId** | **String**| Scope Id consisting the UUID of the shared scope  |
 **body** | [**ScopeDTO**](ScopeDTO.md)| Scope object that needs to be updated  |

### Return type

[**ScopeDTO**](ScopeDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="validateScope"></a>
# **validateScope**
> validateScope(scopeId)

Check given scope name is already exist

Using this operation, user can check a given scope name exists or not. 

### Example
```java
// Import classes:
//import org.wso2.am.integration.clients.publisher.api.ApiClient;
//import org.wso2.am.integration.clients.publisher.api.ApiException;
//import org.wso2.am.integration.clients.publisher.api.Configuration;
//import org.wso2.am.integration.clients.publisher.api.auth.*;
//import org.wso2.am.integration.clients.publisher.api.v1.ScopesApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure OAuth2 access token for authorization: OAuth2Security
OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

ScopesApi apiInstance = new ScopesApi();
String scopeId = "scopeId_example"; // String | Scope name 
try {
    apiInstance.validateScope(scopeId);
} catch (ApiException e) {
    System.err.println("Exception when calling ScopesApi#validateScope");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **scopeId** | **String**| Scope name  |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

