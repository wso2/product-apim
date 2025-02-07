# ScopesApi

All URIs are relative to *https://apis.wso2.com/api/am/publisher/v4*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addSharedScope**](ScopesApi.md#addSharedScope) | **POST** /scopes | Add a New Shared Scope
[**deleteSharedScope**](ScopesApi.md#deleteSharedScope) | **DELETE** /scopes/{scopeId} | Delete a Shared Scope
[**getSharedScope**](ScopesApi.md#getSharedScope) | **GET** /scopes/{scopeId} | Get a Shared Scope by Scope Id
[**getSharedScopeUsages**](ScopesApi.md#getSharedScopeUsages) | **GET** /scopes/{scopeId}/usage | Get usages of a Shared Scope by Scope Id
[**getSharedScopes**](ScopesApi.md#getSharedScopes) | **GET** /scopes | Get All Available Shared Scopes
[**updateSharedScope**](ScopesApi.md#updateSharedScope) | **PUT** /scopes/{scopeId} | Update a Shared Scope
[**validateScope**](ScopesApi.md#validateScope) | **HEAD** /scopes/{scopeId} | Check Given Scope Name already Exists


<a name="addSharedScope"></a>
# **addSharedScope**
> ScopeDTO addSharedScope(scopeDTO)

Add a New Shared Scope

This operation can be used to add a new Shared Scope. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ScopesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ScopesApi apiInstance = new ScopesApi(defaultClient);
    ScopeDTO scopeDTO = new ScopeDTO(); // ScopeDTO | Scope object that needs to be added
    try {
      ScopeDTO result = apiInstance.addSharedScope(scopeDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ScopesApi#addSharedScope");
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
 **scopeDTO** | [**ScopeDTO**](ScopeDTO.md)| Scope object that needs to be added |

### Return type

[**ScopeDTO**](ScopeDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Created. Successful response with the newly created Scope object as an entity in the body.  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**415** | Unsupported Media Type. The entity of the request was not in a supported format. |  -  |

<a name="deleteSharedScope"></a>
# **deleteSharedScope**
> deleteSharedScope(scopeId)

Delete a Shared Scope

This operation can be used to delete a Shared Scope proving the Id of the scope. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ScopesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ScopesApi apiInstance = new ScopesApi(defaultClient);
    String scopeId = "scopeId_example"; // String | Scope Id consisting the UUID of the shared scope 
    try {
      apiInstance.deleteSharedScope(scopeId);
    } catch (ApiException e) {
      System.err.println("Exception when calling ScopesApi#deleteSharedScope");
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
 **scopeId** | **String**| Scope Id consisting the UUID of the shared scope  |

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
**200** | OK. Resource successfully deleted.  |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getSharedScope"></a>
# **getSharedScope**
> ScopeDTO getSharedScope(scopeId)

Get a Shared Scope by Scope Id

This operation can be used to retrieve details of a Shared Scope by a given scope Id. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ScopesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ScopesApi apiInstance = new ScopesApi(defaultClient);
    String scopeId = "scopeId_example"; // String | Scope Id consisting the UUID of the shared scope 
    try {
      ScopeDTO result = apiInstance.getSharedScope(scopeId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ScopesApi#getSharedScope");
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
 **scopeId** | **String**| Scope Id consisting the UUID of the shared scope  |

### Return type

[**ScopeDTO**](ScopeDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Requested Shared Scope is returned.  |  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getSharedScopeUsages"></a>
# **getSharedScopeUsages**
> SharedScopeUsageDTO getSharedScopeUsages(scopeId)

Get usages of a Shared Scope by Scope Id

This operation can be used to retrieve usages of a Shared Scope by a given scope Id. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ScopesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ScopesApi apiInstance = new ScopesApi(defaultClient);
    String scopeId = "scopeId_example"; // String | Scope Id consisting the UUID of the shared scope 
    try {
      SharedScopeUsageDTO result = apiInstance.getSharedScopeUsages(scopeId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ScopesApi#getSharedScopeUsages");
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
 **scopeId** | **String**| Scope Id consisting the UUID of the shared scope  |

### Return type

[**SharedScopeUsageDTO**](SharedScopeUsageDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Usages of the shared scope is returned.  |  * Content-Type - The content type of the body.  <br>  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="getSharedScopes"></a>
# **getSharedScopes**
> ScopeListDTO getSharedScopes(limit, offset)

Get All Available Shared Scopes

This operation can be used to get all the available Shared Scopes. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ScopesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ScopesApi apiInstance = new ScopesApi(defaultClient);
    Integer limit = 25; // Integer | Maximum size of resource array to return. 
    Integer offset = 0; // Integer | Starting point within the complete list of items qualified. 
    try {
      ScopeListDTO result = apiInstance.getSharedScopes(limit, offset);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ScopesApi#getSharedScopes");
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
 **limit** | **Integer**| Maximum size of resource array to return.  | [optional] [default to 25]
 **offset** | **Integer**| Starting point within the complete list of items qualified.  | [optional] [default to 0]

### Return type

[**ScopeListDTO**](ScopeListDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Shared Scope list is returned.  |  * Content-Type - The content type of the body.  <br>  |
**500** | Internal Server Error. |  -  |

<a name="updateSharedScope"></a>
# **updateSharedScope**
> ScopeDTO updateSharedScope(scopeId, scopeDTO)

Update a Shared Scope

This operation can be used to update a Shared Scope by a given scope Id. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ScopesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ScopesApi apiInstance = new ScopesApi(defaultClient);
    String scopeId = "scopeId_example"; // String | Scope Id consisting the UUID of the shared scope 
    ScopeDTO scopeDTO = new ScopeDTO(); // ScopeDTO | Scope object that needs to be updated
    try {
      ScopeDTO result = apiInstance.updateSharedScope(scopeId, scopeDTO);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ScopesApi#updateSharedScope");
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
 **scopeId** | **String**| Scope Id consisting the UUID of the shared scope  |
 **scopeDTO** | [**ScopeDTO**](ScopeDTO.md)| Scope object that needs to be updated |

### Return type

[**ScopeDTO**](ScopeDTO.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK. Successful response with updated Scope object  |  * Content-Type - The content type of the body.  <br>  |
**400** | Bad Request. Invalid request or validation error. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

<a name="validateScope"></a>
# **validateScope**
> validateScope(scopeId)

Check Given Scope Name already Exists

Using this operation, user can check a given scope name exists or not. 

### Example
```java
// Import classes:
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.Configuration;
import org.wso2.am.integration.clients.publisher.api.auth.*;
import org.wso2.am.integration.clients.publisher.api.models.*;
import org.wso2.am.integration.clients.publisher.api.v1.ScopesApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://apis.wso2.com/api/am/publisher/v4");
    
    // Configure OAuth2 access token for authorization: OAuth2Security
    OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
    OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

    ScopesApi apiInstance = new ScopesApi(defaultClient);
    String scopeId = "scopeId_example"; // String | Scope name 
    try {
      apiInstance.validateScope(scopeId);
    } catch (ApiException e) {
      System.err.println("Exception when calling ScopesApi#validateScope");
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
 **scopeId** | **String**| Scope name  |

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
**200** | OK. Requested scope name exists. |  -  |
**404** | Not Found. The specified resource does not exist. |  -  |

